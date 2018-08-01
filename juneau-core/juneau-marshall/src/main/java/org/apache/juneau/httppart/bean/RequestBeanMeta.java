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

import static org.apache.juneau.internal.ReflectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link RequestBean}.
 */
public class RequestBeanMeta {

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param m The method containing the parameter or parameter type annotated with {@link RequestBean}.
	 * @param i The parameter index.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link RequestBean}.
	 */
	public static RequestBeanMeta create(Method m, int i, PropertyStore ps) {
		if (! hasAnnotation(RequestBean.class, m, i))
			return null;
		return new RequestBeanMeta.Builder(ps).apply(m, i).build();
	}

	/**
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link RequestBean}.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link RequestBean}.
	 */
	public static RequestBeanMeta create(Class<?> c, PropertyStore ps) {
		if (! hasAnnotation(RequestBean.class, c))
			return null;
		return new RequestBeanMeta.Builder(ps).apply(c).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ClassMeta<?> cm;
	private final Map<String,RequestBeanPropertyMeta> properties, propertiesByGetter;
	private final HttpPartSerializer serializer;
	private final HttpPartParser parser;

	RequestBeanMeta(Builder b) {
		this.cm = b.cm;
		this.serializer = ClassUtils.newInstance(HttpPartSerializer.class, b.serializer, true, b.ps);
		this.parser = ClassUtils.newInstance(HttpPartParser.class, b.parser, true, b.ps);
		Map<String,RequestBeanPropertyMeta> properties = new LinkedHashMap<>(), propertiesByGetter = new LinkedHashMap<>();
		for (Map.Entry<String,RequestBeanPropertyMeta.Builder> e : b.properties.entrySet()) {
			RequestBeanPropertyMeta pm = e.getValue().build(serializer, parser);
			properties.put(e.getKey(), pm);
			propertiesByGetter.put(pm.getGetter(), pm);

		}
		this.properties = Collections.unmodifiableMap(properties);
		this.propertiesByGetter = Collections.unmodifiableMap(propertiesByGetter);
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
			return apply(m.getParameterTypes()[i]).apply(getAnnotation(RequestBean.class, m, i));
		}

		Builder apply(Class<?> c) {
			apply(getAnnotation(RequestBean.class, c));
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			BeanMeta<?> bm = BeanContext.DEFAULT.getBeanMeta(c);
			for (BeanPropertyMeta bp : bm.getPropertyMetas()) {
				String n = bp.getName();
				Annotation a = bp.getAnnotation(Path.class);
				String g = bp.getGetter().getName();
				if (a != null) {
					HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
					getProperty(n, HttpPartType.PATH).apply(s).getter(g);
				}
				a = bp.getAnnotation(Header.class);
				if (a != null) {
					HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
					getProperty(n, HttpPartType.HEADER).apply(s).getter(g);
				}
				a = bp.getAnnotation(Query.class);
				if (a != null) {
					HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
					getProperty(n, HttpPartType.QUERY).apply(s).getter(g);
				}
				a = bp.getAnnotation(FormData.class);
				if (a != null) {
					HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
					getProperty(n, HttpPartType.FORMDATA).apply(s).getter(g);
				}
				a = bp.getAnnotation(Body.class);
				if (a != null) {
					HttpPartSchemaBuilder s = HttpPartSchema.create().apply(a);
					getProperty(n, HttpPartType.BODY).apply(s).getter(g);
				}
			}
			return this;
		}

		Builder apply(RequestBean rb) {
			if (rb != null) {
				if (rb.serializer() != HttpPartSerializer.Null.class)
					serializer = rb.serializer();
				if (rb.parser() != HttpPartParser.Null.class)
					parser = rb.parser();
			}
			return this;
		}

		RequestBeanMeta build() {
			return new RequestBeanMeta(this);
		}

		private RequestBeanPropertyMeta.Builder getProperty(String name, HttpPartType partType) {
			RequestBeanPropertyMeta.Builder b = properties.get(name);
			if (b == null) {
				b = RequestBeanPropertyMeta.create().name(name).partType(partType);
				properties.put(name, b);
			}
			return b;
		}
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
	 * Returns metadata about the bean property with the specified method getter name.
	 *
	 * @param name The bean method getter name.
	 * @return Metadata about the bean property, or <jk>null</jk> if none found.
	 */
	public RequestBeanPropertyMeta getPropertyByGetter(String name) {
		return propertiesByGetter.get(name);
	}

	/**
	 * Returns metadata about the class.
	 *
	 * @return Metadata about the class.
	 */
	public ClassMeta<?> getClassMeta() {
		return cm;
	}
}
