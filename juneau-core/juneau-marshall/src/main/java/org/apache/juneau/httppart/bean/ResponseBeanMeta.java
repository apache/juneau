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
import static org.apache.juneau.annotation.InvalidAnnotationException.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Response}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class ResponseBeanMeta {

	/**
	 * Represents a non-existent meta object.
	 */
	public static ResponseBeanMeta NULL = new ResponseBeanMeta(new Builder(AnnotationWorkList.create()));

	/**
	 * Create metadata from specified class.
	 *
	 * @param t The class annotated with {@link Response}.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Response}.
	 */
	public static ResponseBeanMeta create(Type t, AnnotationWorkList annotations) {
		ClassInfo ci = ClassInfo.of(t).unwrap(Value.class, Optional.class);
		if (ci.hasNoAnnotation(Response.class))
			return null;
		Builder b = new Builder(annotations);
		b.apply(ci.innerType());
		ci.forEachAnnotation(Response.class, x -> true, x -> b.apply(x));
		ci.forEachAnnotation(StatusCode.class, x -> true, x -> b.apply(x));
		return b.build();
	}

	/**
	 * Create metadata from specified method return.
	 *
	 * @param m The method annotated with {@link Response}.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Response}.
	 */
	public static ResponseBeanMeta create(MethodInfo m, AnnotationWorkList annotations) {
		if (! (m.hasAnnotation(Response.class) || m.getReturnType().unwrap(Value.class,Optional.class).hasAnnotation(Response.class)))
			return null;
		Builder b = new Builder(annotations);
		b.apply(m.getReturnType().unwrap(Value.class, Optional.class).innerType());
		m.forEachAnnotation(Response.class, x -> true, x -> b.apply(x));
		m.forEachAnnotation(StatusCode.class, x -> true, x -> b.apply(x));
		return b.build();
	}

	/**
	 * Create metadata from specified method parameter.
	 *
	 * @param mpi The method parameter.
	 * @param annotations The annotations to apply to any new part serializers or parsers.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Response}.
	 */
	public static ResponseBeanMeta create(ParamInfo mpi, AnnotationWorkList annotations) {
		if (mpi.hasNoAnnotation(Response.class))
			return null;
		Builder b = new Builder(annotations);
		b.apply(mpi.getParameterType().unwrap(Value.class, Optional.class).innerType());
		mpi.forEachAnnotation(Response.class, x-> true, x -> b.apply(x));
		mpi.forEachAnnotation(StatusCode.class, x -> true, x -> b.apply(x));
		return b.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ClassMeta<?> cm;
	private final Map<String,ResponseBeanPropertyMeta> properties;
	private final int code;
	private final Map<String,ResponseBeanPropertyMeta> headerMethods;
	private final ResponseBeanPropertyMeta statusMethod, contentMethod;
	private final Optional<HttpPartSerializer> partSerializer;
	private final Optional<HttpPartParser> partParser;
	private final HttpPartSchema schema;

	ResponseBeanMeta(Builder b) {
		this.cm = b.cm;
		this.code = b.code;
		this.partSerializer = optional(b.partSerializer).map(x -> HttpPartSerializer.creator().type(x).apply(b.annotations).create());
		this.partParser = optional(b.partParser).map(x -> HttpPartParser.creator().type(x).apply(b.annotations).create());
		this.schema = b.schema.build();

		Map<String,ResponseBeanPropertyMeta> properties = map();

		Map<String,ResponseBeanPropertyMeta> hm = map();
		b.headerMethods.forEach((k,v) -> {
			ResponseBeanPropertyMeta pm = v.build(partSerializer, partParser);
			hm.put(k, pm);
			properties.put(pm.getGetter().getName(), pm);
		});
		this.headerMethods = unmodifiable(hm);

		this.contentMethod = b.contentMethod == null ? null : b.contentMethod.schema(schema).build(partSerializer, partParser);
		this.statusMethod = b.statusMethod == null ? null : b.statusMethod.build(empty(), empty());

		if (contentMethod != null)
			properties.put(contentMethod.getGetter().getName(), contentMethod);
		if (statusMethod != null)
			properties.put(statusMethod.getGetter().getName(), statusMethod);

		this.properties = unmodifiable(properties);
	}

	static class Builder {
		ClassMeta<?> cm;
		int code;
		AnnotationWorkList annotations;
		Class<? extends HttpPartSerializer> partSerializer;
		Class<? extends HttpPartParser> partParser;
		HttpPartSchema.Builder schema = HttpPartSchema.create();

		Map<String,ResponseBeanPropertyMeta.Builder> headerMethods = map();
		ResponseBeanPropertyMeta.Builder contentMethod;
		ResponseBeanPropertyMeta.Builder statusMethod;

		Builder(AnnotationWorkList annotations) {
			this.annotations = annotations;
		}

		Builder apply(Type t) {
			Class<?> c = ClassUtils.toClass(t);
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			ClassInfo ci = cm.getInfo();
			ci.forEachPublicMethod(x -> true, x -> {
				assertNoInvalidAnnotations(x, Query.class, FormData.class);
				if (x.hasAnnotation(Header.class)) {
					assertNoArgs(x, Header.class);
					assertReturnNotVoid(x, Header.class);
					HttpPartSchema s = HttpPartSchema.create(x.getAnnotation(Header.class), x.getPropertyName());
					headerMethods.put(s.getName(), ResponseBeanPropertyMeta.create(RESPONSE_HEADER, s, x));
				} else if (x.hasAnnotation(StatusCode.class)) {
					assertNoArgs(x, Header.class);
					assertReturnType(x, Header.class, int.class, Integer.class);
					statusMethod = ResponseBeanPropertyMeta.create(RESPONSE_STATUS, x);
				} else if (x.hasAnnotation(Content.class)) {
					if (x.hasNoParams())
						assertReturnNotVoid(x, Header.class);
					else
						assertArgType(x, Header.class, OutputStream.class, Writer.class);
					contentMethod = ResponseBeanPropertyMeta.create(RESPONSE_BODY, x);
				}
			});
			return this;
		}

		Builder apply(Response a) {
			if (a != null) {
				if (isNotVoid(a.serializer()))
					partSerializer = a.serializer();
				if (isNotVoid(a.parser()))
					partParser = a.parser();
				schema.apply(a.schema());
			}
			return this;
		}

		Builder apply(StatusCode a) {
			if (a != null) {
				if (a.value().length > 0)
					code = a.value()[0];
			}
			return this;
		}

		ResponseBeanMeta build() {
			return new ResponseBeanMeta(this);
		}
	}

	/**
	 * Returns the HTTP status code.
	 *
	 * @return The HTTP status code.
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the schema information about the response object.
	 *
	 * @return The schema information about the response object.
	 */
	public HttpPartSchema getSchema() {
		return schema;
	}

	/**
	 * Returns metadata about the <ja>@Header</ja>-annotated methods.
	 *
	 * @return Metadata about the <ja>@Header</ja>-annotated methods, or an empty collection if none exist.
	 */
	public Collection<ResponseBeanPropertyMeta> getHeaderMethods() {
		return headerMethods.values();
	}

	/**
	 * Returns the <ja>@Content</ja>-annotated method.
	 *
	 * @return The <ja>@Content</ja>-annotated method, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseBeanPropertyMeta getContentMethod() {
		return contentMethod;
	}

	/**
	 * Returns the <ja>@StatusCode</ja>-annotated method.
	 *
	 * @return The <ja>@StatusCode</ja>-annotated method, or <jk>null</jk> if it doesn't exist.
	 */
	public ResponseBeanPropertyMeta getStatusMethod() {
		return statusMethod;
	}

	/**
	 * Returns the part serializer to use to serialize this response.
	 *
	 * @return The part serializer to use to serialize this response.
	 */
	public Optional<HttpPartSerializer> getPartSerializer() {
		return partSerializer;
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
	 * Returns metadata about the bean property with the specified method getter name.
	 *
	 * @param name The bean method getter name.
	 * @return Metadata about the bean property, or <jk>null</jk> if none found.
	 */
	public ResponseBeanPropertyMeta getProperty(String name) {
		return properties.get(name);
	}

	/**
	 * Returns all the annotated methods on this bean.
	 *
	 * @return All the annotated methods on this bean.
	 */
	public Collection<ResponseBeanPropertyMeta> getProperties() {
		return properties.values();
	}
}
