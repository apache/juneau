// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.httppart.bean.Utils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.annotation.InvalidAnnotationException.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflection.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Request}.
 */
public class RequestBeanMeta {

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param mpi The method parameter.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(MethodParamInfo mpi, PropertyStore ps) {
		if (! mpi.hasAnnotation(Request.class))
			return null;
		return new RequestBeanMeta.Builder(ps).apply(mpi).build();
	}

	/**
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link Request}.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(Class<?> c, PropertyStore ps) {
		ClassInfo ci = getClassInfo(c);
		if (! ci.hasAnnotation(Request.class))
			return null;
		return new RequestBeanMeta.Builder(ps).apply(c).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ClassMeta<?> cm;
	private final Map<String,RequestBeanPropertyMeta> properties;
	private final HttpPartSerializer serializer;
	private final HttpPartParser parser;

	RequestBeanMeta(Builder b) {
		this.cm = b.cm;
		this.serializer = castOrCreate(HttpPartSerializer.class, b.serializer, true, b.ps);
		this.parser = castOrCreate(HttpPartParser.class, b.parser, true, b.ps);
		Map<String,RequestBeanPropertyMeta> properties = new LinkedHashMap<>();
		for (Map.Entry<String,RequestBeanPropertyMeta.Builder> e : b.properties.entrySet())
			properties.put(e.getKey(), e.getValue().build(serializer, parser));
		this.properties = Collections.unmodifiableMap(properties);
	}

	static class Builder {
		ClassMeta<?> cm;
		PropertyStore ps;
		Class<? extends HttpPartSerializer> serializer;
		Class<? extends HttpPartParser> parser;
		Map<String,RequestBeanPropertyMeta.Builder> properties = new LinkedHashMap<>();

		Builder(PropertyStore ps) {
			this.ps = ps;
		}

		Builder apply(MethodParamInfo mpi) {
			return apply(mpi.getParameterType()).apply(mpi.getAnnotation(Request.class));
		}

		Builder apply(Class<?> c) {
			ClassInfo ci = getClassInfo(c);
			apply(ci.getAnnotation(Request.class));
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			for (MethodInfo m : cm.getInfo().getAllMethods()) {

				if (m.isPublic()) {
					assertNoInvalidAnnotations(m, ResponseHeader.class, ResponseBody.class, ResponseStatus.class);
					String n = m.getSimpleName();
					if (m.hasAnnotation(Body.class)) {
						assertNoArgs(m, Body.class);
						assertReturnNotVoid(m, Body.class);
						properties.put(n, RequestBeanPropertyMeta.create(BODY, Body.class, m));
					} else if (m.hasAnnotation(Header.class)) {
						assertNoArgs(m, Header.class);
						assertReturnNotVoid(m, Header.class);
						properties.put(n, RequestBeanPropertyMeta.create(HEADER, Header.class, m));
					} else if (m.hasAnnotation(Query.class)) {
						assertNoArgs(m, Query.class);
						assertReturnNotVoid(m, Query.class);
						properties.put(n, RequestBeanPropertyMeta.create(QUERY, Query.class, m));
					} else if (m.hasAnnotation(FormData.class)) {
						assertNoArgs(m, FormData.class);
						assertReturnNotVoid(m, FormData.class);
						properties.put(n, RequestBeanPropertyMeta.create(FORMDATA, FormData.class, m));
					} else if (m.hasAnnotation(Path.class)) {
						assertNoArgs(m, Path.class);
						assertReturnNotVoid(m, Path.class);
						properties.put(n, RequestBeanPropertyMeta.create(PATH, Path.class, m));
					}
				}
			}
			return this;
		}

		Builder apply(Request a) {
			if (a != null) {
				if (a.partSerializer() != HttpPartSerializer.Null.class)
					serializer = a.partSerializer();
				if (a.partParser() != HttpPartParser.Null.class)
					parser = a.partParser();
			}
			return this;
		}

		RequestBeanMeta build() {
			return new RequestBeanMeta(this);
		}
	}

	/**
	 * Returns metadata about the class.
	 *
	 * @return Metadata about the class.
	 */
	public ClassMeta<?> getClassMeta() {
		return cm;
	}

	/**
	 * Returns metadata about the bean property with the specified property name.
	 *
	 * @param name The bean property name.
	 * @return Metadata about the bean property, or <jk>null</jk> if none found.
	 */
	public RequestBeanPropertyMeta getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Returns all the annotated methods on this bean.
	 *
	 * @return All the annotated methods on this bean.
	 */
	public Collection<RequestBeanPropertyMeta> getProperties() {
		return properties.values();
	}
}
