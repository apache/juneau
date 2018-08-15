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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.httppart.bean.Utils.*;
import static org.apache.juneau.httppart.HttpPartType.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Request}.
 */
public class RequestBeanMeta {

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param m The method containing the parameter or parameter type annotated with {@link Request}.
	 * @param i The parameter index.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(Method m, int i, PropertyStore ps) {
		if (! hasAnnotation(Request.class, m, i))
			return null;
		return new RequestBeanMeta.Builder(ps).apply(m, i).build();
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
		if (! hasAnnotation(Request.class, c))
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
		this.serializer = ClassUtils.newInstance(HttpPartSerializer.class, b.serializer, true, b.ps);
		this.parser = ClassUtils.newInstance(HttpPartParser.class, b.parser, true, b.ps);
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

		Builder apply(Method m, int i) {
			return apply(m.getParameterTypes()[i]).apply(getAnnotation(Request.class, m, i));
		}

		Builder apply(Class<?> c) {
			apply(getAnnotation(Request.class, c));
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			for (Method m : ClassUtils.getAllMethods(c, false)) {
				if (isPublic(m)) {
					assertNoAnnotations(m, Request.class, ResponseHeader.class, ResponseBody.class, ResponseStatus.class);
					String n = m.getName();
					if (hasAnnotation(Body.class, m)) {
						assertNoArgs(m, Body.class);
						assertReturnNotVoid(m, Body.class);
						properties.put(n, RequestBeanPropertyMeta.create(BODY, Body.class, m));
					} else if (hasAnnotation(Header.class, m)) {
						assertNoArgs(m, Header.class);
						assertReturnNotVoid(m, Header.class);
						properties.put(n, RequestBeanPropertyMeta.create(HEADER, Header.class, m));
					} else if (hasAnnotation(Query.class, m)) {
						assertNoArgs(m, Query.class);
						assertReturnNotVoid(m, Query.class);
						properties.put(n, RequestBeanPropertyMeta.create(QUERY, Query.class, m));
					} else if (hasAnnotation(FormData.class, m)) {
						assertNoArgs(m, FormData.class);
						assertReturnNotVoid(m, FormData.class);
						properties.put(n, RequestBeanPropertyMeta.create(FORMDATA, FormData.class, m));
					} else if (hasAnnotation(Path.class, m)) {
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
