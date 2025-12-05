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
package org.apache.juneau.httppart.bean;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.ClassUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.httppart.bean.MethodInfoUtils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Request}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class RequestBeanMeta {

	private static final AnnotationProvider AP = AnnotationProvider.INSTANCE;

	static class Builder {
		ClassMeta<?> cm;
		AnnotationWorkList annotations;
		BeanCreator<HttpPartSerializer> serializer = BeanCreator.of(HttpPartSerializer.class);
		BeanCreator<HttpPartParser> parser = BeanCreator.of(HttpPartParser.class);
		Map<String,RequestBeanPropertyMeta.Builder> properties = map();

		Builder(AnnotationWorkList annotations) {
			this.annotations = annotations;
		}

		Builder apply(Class<?> c) {
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			apply(cm.getLastAnnotation(Request.class));
			cm.getPublicMethods().stream().forEach(x -> {
				var n = x.getSimpleName();
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

		Builder apply(ParameterInfo mpi) {
			return apply(mpi.getParameterType().inner()).apply(AP.find(Request.class, mpi).stream().findFirst().map(AnnotationInfo::inner).orElse(null));
		}

		Builder apply(Request a) {
			if (nn(a)) {
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
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link Request}.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(Class<?> c, AnnotationWorkList annotations) {
		var ci = info(c);
		if (! ci.hasAnnotation(Request.class))
			return null;
		return new RequestBeanMeta.Builder(annotations).apply(c).build();
	}

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param mpi The method parameter.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Request}.
	 */
	public static RequestBeanMeta create(ParameterInfo mpi, AnnotationWorkList annotations) {
		if (! AP.has(Request.class, mpi))
			return null;
		return new RequestBeanMeta.Builder(annotations).apply(mpi).build();
	}

	private final ClassMeta<?> cm;
	private final Map<String,RequestBeanPropertyMeta> properties;
	private final HttpPartSerializer serializer;

	private final HttpPartParser parser;

	RequestBeanMeta(Builder b) {
		cm = b.cm;
		serializer = b.serializer.orElse(null);
		parser = b.parser.orElse(null);
		Map<String,RequestBeanPropertyMeta> pm = map();
		b.properties.forEach((k, v) -> pm.put(k, v.build(serializer, parser)));
		properties = u(pm);
	}

	/**
	 * Returns metadata about the class.
	 *
	 * @return Metadata about the class.
	 */
	public ClassMeta<?> getClassMeta() { return cm; }

	/**
	 * Returns all the annotated methods on this bean.
	 *
	 * @return All the annotated methods on this bean.
	 */
	public Collection<RequestBeanPropertyMeta> getProperties() { return properties.values(); }

	/**
	 * Returns metadata about the bean property with the specified property name.
	 *
	 * @param name The bean property name.
	 * @return Metadata about the bean property, or <jk>null</jk> if none found.
	 */
	public RequestBeanPropertyMeta getProperty(String name) {
		return properties.get(name);
	}
}