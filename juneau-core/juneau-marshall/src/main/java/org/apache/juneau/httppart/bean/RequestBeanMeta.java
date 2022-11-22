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
import static org.apache.juneau.internal.CollectionUtils.*;
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
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
		if (mpi.hasNoAnnotation(Request.class))
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
		if (ci.hasNoAnnotation(Request.class))
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
		this.serializer = b.serializer.orElse(null);
		this.parser = b.parser.orElse(null);
		Map<String,RequestBeanPropertyMeta> properties = map();
		b.properties.forEach((k,v) -> properties.put(k, v.build(serializer, parser)));
		this.properties = unmodifiable(properties);
	}

	static class Builder {
		ClassMeta<?> cm;
		AnnotationWorkList annotations;
		BeanCreator<HttpPartSerializer> serializer = BeanCreator.of(HttpPartSerializer.class);
		BeanCreator<HttpPartParser> parser = BeanCreator.of(HttpPartParser.class);
		Map<String,RequestBeanPropertyMeta.Builder> properties = map();

		Builder(AnnotationWorkList annotations) {
			this.annotations = annotations;
		}

		Builder apply(ParamInfo mpi) {
			return apply(mpi.getParameterType().inner()).apply(mpi.getAnnotation(Request.class));
		}

		Builder apply(Class<?> c) {
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			apply(cm.getLastAnnotation(Request.class));
			cm.getInfo().forEachPublicMethod(x -> true, x -> {
				String n = x.getSimpleName();
				if (x.hasAnnotation(Header.class)) {
					assertNoArgs(x, Header.class);
					assertReturnNotVoid(x, Header.class);
					properties.put(n, RequestBeanPropertyMeta.create(HEADER, Header.class, x));
				} else if (x.hasAnnotation(Query.class)) {
					assertNoArgs(x, Query.class);
					assertReturnNotVoid(x, Query.class);
					properties.put(n, RequestBeanPropertyMeta.create(QUERY, Query.class, x));
				} else if (x.hasAnnotation(FormData.class)) {
					assertNoArgs(x, FormData.class);
					assertReturnNotVoid(x, FormData.class);
					properties.put(n, RequestBeanPropertyMeta.create(FORMDATA, FormData.class, x));
				} else if (x.hasAnnotation(Path.class)) {
					assertNoArgs(x, Path.class);
					assertReturnNotVoid(x, Path.class);
					properties.put(n, RequestBeanPropertyMeta.create(PATH, Path.class, x));
				} else if (x.hasAnnotation(Content.class)) {
					assertNoArgs(x, Content.class);
					assertReturnNotVoid(x, Content.class);
					properties.put(n, RequestBeanPropertyMeta.create(BODY, Content.class, x));
				}
			});
			return this;
		}

		Builder apply(Request a) {
			if (a != null) {
				if (isNotVoid(a.serializer()))
					serializer.type(a.serializer());
				if (isNotVoid(a.parser()))
					parser.type(a.parser());
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
