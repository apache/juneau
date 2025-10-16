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
package org.apache.juneau.http.part;

import static org.apache.juneau.internal.ClassUtils.*;

import java.util.*;
import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.assertions.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.reflect.*;

/**
 * Implementation of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries.
 *
 * Provides the following features:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Values from {@link Supplier Suppliers}.
 * 	<li>
 * 		Caching.
 * 	<li>
 * 		Fluent setters.
 * 	<li>
 * 		Fluent assertions.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestCommonBasics">juneau-rest-common Basics</a>
 * </ul>
 */
@BeanIgnore
public class BasicPart implements NameValuePair, Headerable {
	/**
	 * Returns <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 *
	 * @param o The object to check.
	 * @return <jk>true</jk> if the {@link #cast(Object)} method can be used on the specified object.
	 */
	public static boolean canCast(Object o) {
		ClassInfo ci = ClassInfo.of(o);
		return ci != null && ci.isChildOfAny(Headerable.class, NameValuePair.class, NameValuePairable.class, Map.Entry.class);
	}
	/**
	 * Utility method for converting an arbitrary object to a {@link NameValuePair}.
	 *
	 * @param o
	 * 	The object to cast or convert to a {@link NameValuePair}.
	 * @return Either the same object cast as a {@link NameValuePair} or converted to a {@link NameValuePair}.
	 */
	@SuppressWarnings("rawtypes")
	public static NameValuePair cast(Object o) {
		if (o instanceof NameValuePair)
			return (NameValuePair)o;
		if (o instanceof NameValuePairable)
			return ((NameValuePairable)o).asNameValuePair();
		if (o instanceof NameValuePair) {
			NameValuePair p = (NameValuePair)o;
			return BasicPart.of(p.getName(), p.getValue());
		}
		if (o instanceof Headerable) {
			Header x = ((Headerable)o).asHeader();
			return BasicPart.of(x.getName(), x.getValue());
		}
		if (o instanceof Map.Entry) {
			Map.Entry e = (Map.Entry)o;
			return BasicPart.of(Utils.s(e.getKey()), e.getValue());
		}
		throw new BasicRuntimeException("Object of type {0} could not be converted to a Part.", className(o));
	}

	/**
	 * Static creator.
	 *
	 * @param name The part name.
	 * @param value The part value.
	 * @return A new {@link BasicPart} object.
	 */
	public static BasicPart of(String name, Object value) {
		return new BasicPart(name, value);
	}

	/**
	 * Creates a {@link NameValuePair} from a name/value pair string (e.g. <js>"Foo: bar"</js>)
	 *
	 * @param pair The pair string.
	 * @return A new {@link NameValuePair} object.
	 */
	public static BasicPart ofPair(String pair) {
		if (pair == null)
			return null;
		int i = pair.indexOf(':');
		if (i == -1)
			i = pair.indexOf('=');
		if (i == -1)
			return of(pair, "");
		return of(pair.substring(0,i).trim(), pair.substring(i+1).trim());
	}

	private final String name;

	private final Object value;

	/**
	 * Constructor.
	 *
	 * @param name The part name.
	 * @param value The POJO to serialize to The part value.
	 */
	public BasicPart(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected BasicPart(BasicPart copyFrom) {
		Utils.assertArgNotNull("copyFrom", copyFrom);
		this.name = copyFrom.name;
		this.value = copyFrom.value;
	}

	@Override /* Overridden from Headerable */
	public BasicHeader asHeader() {
		return BasicHeader.of(name, Utils.s(value));
	}

	/**
	 * Provides an object for performing assertions against the name of this pair.
	 *
	 * @return An object for performing assertions against the name of this pair.
	 */
	public FluentStringAssertion<BasicPart> assertName() {
		return new FluentStringAssertion<>(getName(), this);
	}

	/**
	 * Provides an object for performing assertions against the value of this pair.
	 *
	 * @return An object for performing assertions against the value of this pair.
	 */
	public FluentStringAssertion<BasicPart> assertValue() {
		return new FluentStringAssertion<>(getValue(), this);
	}

	@Override /* Overridden from NameValuePair */
	public String getName() {
		return name;
	}

	/**
	 * Returns the raw value of the part.
	 *
	 * @return The raw value of the part.
	 */
	public Object getRawValue() {
		return unwrap(value);
	}

	@Override /* Overridden from NameValuePair */
	public String getValue() {
		return Utils.s(unwrap(value));
	}

	@Override /* Overridden from Object */
	public String toString() {
		return getName() + "=" + getValue();
	}

	private Object unwrap(Object o) {
		while (o instanceof Supplier)
			o = ((Supplier<?>)o).get();
		return o;
	}
}