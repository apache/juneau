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
import static org.apache.juneau.httppart.HttpPartType.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Request}.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.HttpPartSerializersParsers}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class RequestBeanMeta {

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param mpi The method parameter.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(ParamInfo mpi, AnnotationWorkList annotations) {
		if (! mpi.hasAnnotation(Request.class))
			return null;
		return new RequestBeanMeta.Builder(annotations).apply(mpi).build();
	}

	/**
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link Request}.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(Class<?> c, AnnotationWorkList annotations) {
		ClassInfo ci = ClassInfo.of(c);
		if (! ci.hasAnnotation(Request.class))
			return null;
		return new RequestBeanMeta.Builder(annotations).apply(c).build();
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
		this.serializer = BeanStore.INSTANCE.createBean(HttpPartSerializer.class).type(b.serializer).run();
		this.parser = BeanStore.INSTANCE.createBean(HttpPartParser.class).type(b.parser).run();
		Map<String,RequestBeanPropertyMeta> properties = new LinkedHashMap<>();
		for (Map.Entry<String,RequestBeanPropertyMeta.Builder> e : b.properties.entrySet())
			properties.put(e.getKey(), e.getValue().build(serializer, parser));
		this.properties = Collections.unmodifiableMap(properties);
	}

	static class Builder {
		ClassMeta<?> cm;
		AnnotationWorkList annotations;
		Class<? extends HttpPartSerializer> serializer;
		Class<? extends HttpPartParser> parser;
		Map<String,RequestBeanPropertyMeta.Builder> properties = new LinkedHashMap<>();

		Builder(AnnotationWorkList annotations) {
			this.annotations = annotations;
		}

		Builder apply(ParamInfo mpi) {
			return apply(mpi.getParameterType().inner()).apply(mpi.getLastAnnotation(Request.class));
		}

		Builder apply(Class<?> c) {
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			apply(cm.getLastAnnotation(Request.class));
			for (MethodInfo m : cm.getInfo().getAllMethods()) {

				if (m.isPublic()) {
					String n = m.getSimpleName();
					if (m.hasAnnotation(Header.class)) {
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
					} else if (m.hasAnnotation(Body.class)) {
						assertNoArgs(m, Body.class);
						assertReturnNotVoid(m, Body.class);
						properties.put(n, RequestBeanPropertyMeta.create(BODY, Body.class, m));
					}
				}
			}
			return this;
		}

		Builder apply(Request a) {
			if (a != null) {
				if (a.serializer() != HttpPartSerializer.Null.class)
					serializer = a.serializer();
				if (a.parser() != HttpPartParser.Null.class)
					parser = a.parser();
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
