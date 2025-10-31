/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.header;

import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.lang.reflect.*;
import java.util.concurrent.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.common.reflect.*;

/**
 * Holds metadata about header beans (POJOs that get serialized as HTTP headers).
 *
 * <p>
 * Header beans are typically annotated with {@link org.apache.juneau.http.annotation.Header @Header} although it's not an
 * absolute requirement.
 *
 * <p>
 * Header beans must have one of the following public constructors:
 * <ul>
 * 	<li><c><jk>public</jk> X(String <jv>headerValue</jv>)</c>
 * 	<li><c><jk>public</jk> X(Object <jv>headerValue</jv>)</c>
 * 	<li><c><jk>public</jk> X(String <jv>headerName</jv>, String <jv>headerValue</jv>)</c>
 * 	<li><c><jk>public</jk> X(String <jv>headerName</jv>, Object <jv>headerValue</jv>)</c>
 * </ul>
 *
 * <p>
 * <h5 class='figure'>Example</h5>
 * <p class='bjava'>
 * 	<jc>// Our header bean.</jc>
 * 	<ja>@Header</ja>(<js>"Foo"</js>)
 * 	<jk>public class</jk> FooHeader <jk>extends</jk> BasicStringHeader {
 *
 * 		<jk>public</jk> FooHeader(String <jv>headerValue</jv>) {
 * 			<jk>super</jk>(<js>"Foo"</js>, <jv>headerValue</jv>);
 * 		}
 *  }
 *
 *  <jc>// Code to retrieve a header bean from a header list in a request.</jc>
 *  HeaderList <jv>headers</jv> = <jv>httpRequest</jv>.getHeaders();
 *  FooHeader <jv>foo</jv> = <jv>headers</jv>.get(FooHeader.<jk>class</jk>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 *
 * @param <T> The header bean type.
 */
public class HeaderBeanMeta<T> {

	private static final ConcurrentHashMap<Class<?>,HeaderBeanMeta<?>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Finds the header bean meta for the specified type.
	 *
	 * @param <T> The header bean type.
	 * @param type The header bean type.
	 * @return The metadata, or <jk>null</jk> if a valid constructor could not be found.
	 */
	@SuppressWarnings("unchecked")
	public static <T> HeaderBeanMeta<T> of(Class<T> type) {
		HeaderBeanMeta<?> m = CACHE.get(type);
		if (m == null) {
			m = new HeaderBeanMeta<>(type);
			CACHE.put(type, m);
		}
		return (HeaderBeanMeta<T>)m;
	}

	private final Class<T> type;
	private final Constructor<T> constructor;

	private final HttpPartSchema schema;

	private HeaderBeanMeta(Class<T> type) {
		this.type = type;

		var ci = ClassInfo.of(type);

		ConstructorInfo cci = ci.getPublicConstructor(x -> x.hasParamTypes(String.class));
		if (cci == null)
			cci = ci.getPublicConstructor(x -> x.hasParamTypes(Object.class));
		if (cci == null)
			cci = ci.getPublicConstructor(x -> x.hasParamTypes(String.class, String.class));
		if (cci == null)
			cci = ci.getPublicConstructor(x -> x.hasParamTypes(String.class, Object.class));
		constructor = cci == null ? null : cci.inner();

		this.schema = HttpPartSchema.create(org.apache.juneau.http.annotation.Header.class, type);
	}

	/**
	 * Constructs a header bean with the specified name or value.
	 *
	 * <p>
	 * Can only be used on beans where the header name is known.
	 *
	 * @param value
	 * 	The header value.
	 * @return A newly constructed bean.
	 */
	public T construct(Object value) {
		return construct(null, value);
	}

	/**
	 * Constructs a header bean with the specified name or value.
	 *
	 * @param name
	 * 	The header name.
	 * 	<br>If <jk>null</jk>, uses the value pulled from the {@link org.apache.juneau.http.annotation.Header#name() @Header(name)} or
	 * 	{@link org.apache.juneau.http.annotation.Header#value() @Header(value)} annotations.
	 * @param value
	 * 	The header value.
	 * @return A newly constructed bean.
	 * @throws UnsupportedOperationException If bean could not be constructed (e.g. couldn't find a constructor).
	 */
	public T construct(String name, Object value) {

		if (constructor == null)
			throw unsupportedOp("Constructor for type {0} could not be found.", cn(type));

		if (name == null)
			name = schema.getName();

		Class<?>[] pt = constructor.getParameterTypes();
		Object[] args = new Object[pt.length];
		if (pt.length == 1) {
			args[0] = pt[0] == String.class ? s(value) : value;
		} else {
			if (name == null)
				throw unsupportedOp("Constructor for type {0} requires a name as the first argument.", cn(type));
			args[0] = name;
			args[1] = pt[1] == String.class ? s(value) : value;
		}

		try {
			return constructor.newInstance(args);
		} catch (Exception e) {
			throw toRuntimeException(e);
		}
	}

	/**
	 * Returns schema information about this header.
	 *
	 * <p>
	 * This is information pulled from {@link org.apache.juneau.http.annotation.Header @Header} annotation on the class.
	 *
	 * @return The schema information.
	 */
	public HttpPartSchema getSchema() { return schema; }
}