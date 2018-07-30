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
package org.apache.juneau.httppart;

import static org.apache.juneau.internal.ClassFlags.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;

/**
 * Represents the metadata gathered from a parameter or class annotated with {@link Response}.
 */
public class ResponseMeta {

	/**
	 * Represents a non-existent meta object.
	 */
	public static ResponseMeta NULL = new ResponseMeta(new Builder(PropertyStore.DEFAULT));

	/**
	 * Create metadata from specified parameter.
	 *
	 * @param m The method containing the parameter or parameter type annotated with {@link Response}.
	 * @param i The parameter index.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Response}.
	 */
	public static ResponseMeta create(Method m, int i, PropertyStore ps) {
		if (! hasAnnotation(Response.class, m, i))
			return null;
		return new ResponseMeta.Builder(ps).apply(m, i).build();
	}

	/**
	 * Create metadata from specified method return.
	 *
	 * @param m The method annotated with {@link Response}.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the parameter, or <jk>null</jk> if parameter or parameter type not annotated with {@link Response}.
	 */
	public static ResponseMeta create(Method m, PropertyStore ps) {
		if (! hasAnnotation(Response.class, m))
			return null;
		return new ResponseMeta.Builder(ps).apply(m).build();
	}

	/**
	 * Create metadata from specified class.
	 *
	 * @param c The class annotated with {@link Response}.
	 * @param ps
	 * 	Configuration information used to instantiate part serializers and part parsers.
	 * 	<br>Can be <jk>null</jk>.
	 * @return Metadata about the class, or <jk>null</jk> if class not annotated with {@link Response}.
	 */
	public static ResponseMeta create(Class<?> c, PropertyStore ps) {
		if (! hasAnnotation(Response.class, c))
			return null;
		return new ResponseMeta.Builder(ps).apply(c).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final ClassMeta<?> cm;
	private final int code;
	private final Map<String,ResponsePropertyMeta> headers;
	private final HttpPartSerializer partSerializer;
	private final HttpPartSchema schema;
	private final boolean usePartSerializer;

	ResponseMeta(Builder b) {
		this.cm = b.cm;
		this.code = b.code;
		this.partSerializer = ClassUtils.newInstance(HttpPartSerializer.class, b.partSerializer, true, b.ps);
		this.schema = b.schema.build();
		this.usePartSerializer = b.usePartSerializer || partSerializer != null;
		Map<String,ResponsePropertyMeta> headers = new LinkedHashMap<>();
		for (Map.Entry<String,ResponsePropertyMeta.Builder> e : b.headers.entrySet()) {
			ResponsePropertyMeta pm = e.getValue().build(partSerializer);
			headers.put(e.getKey(), pm);

		}
		this.headers = Collections.unmodifiableMap(headers);
	}

	static class Builder {
		ClassMeta<?> cm;
		int code;
		PropertyStore ps;
		boolean usePartSerializer;
		Class<? extends HttpPartSerializer> partSerializer;
		HttpPartSchemaBuilder schema = HttpPartSchema.create();
		Map<String,ResponsePropertyMeta.Builder> headers = new LinkedHashMap<>();

		Builder(PropertyStore ps) {
			this.ps = ps;
		}

		Builder apply(Method m, int i) {
			return apply(m.getParameterTypes()[i]).apply(getAnnotation(Response.class, m, i));
		}

		Builder apply(Method m) {
			return apply(m.getReturnType()).apply(getAnnotation(Response.class, m));
		}

		Builder apply(Class<?> c) {
			apply(getAnnotation(Response.class, c));
			this.cm = BeanContext.DEFAULT.getClassMeta(c);
			for (Method m : ClassUtils.getAllMethods(c, false)) {
				if (isAll(m, PUBLIC, HAS_NO_ARGS)) {
					Header h = m.getAnnotation(Header.class);
					if (h != null) {
						String n = h.name();
						if (n.isEmpty())
							n = m.getName();
						HttpPartSchemaBuilder s = HttpPartSchema.create().apply(h);
						getProperty(n, HttpPartType.HEADER).apply(s).getter(m);
					}
				}
			}
			return this;
		}

		Builder apply(Response rb) {
			if (rb != null) {
				if (rb.partSerializer() != HttpPartSerializer.Null.class)
					partSerializer = rb.partSerializer();
				if (rb.usePartSerializer())
					usePartSerializer = true;
				if (rb.value().length > 0)
					code = rb.value()[0];
				if (rb.code().length > 0)
					code = rb.code()[0];
				schema.apply(rb.schema());
			}
			return this;
		}

		ResponseMeta build() {
			return new ResponseMeta(this);
		}

		private ResponsePropertyMeta.Builder getProperty(String name, HttpPartType partType) {
			ResponsePropertyMeta.Builder b = headers.get(name);
			if (b == null) {
				b = ResponsePropertyMeta.create().name(name).partType(partType);
				headers.put(name, b);
			}
			return b;
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
	 * Returns metadata about headers defined on the response object.
	 *
	 * @return Metadata about headers defined on the response object.
	 */
	public Collection<ResponsePropertyMeta> getHeaderMetas() {
		return headers.values();
	}

	/**
	 * Returns the flag for whether the part serializer should be used to serialize this response.
	 *
	 * @return The flag for whether the part serializer should be used to serialize this response.
	 */
	public boolean isUsePartSerializer() {
		return usePartSerializer;
	}

	/**
	 * Returns the part serializer to use to serialize this response.
	 *
	 * @param _default The default serializer to use if it's not defined on this metadata.
	 * @return The part serializer to use to serialize this response.
	 */
	public HttpPartSerializer getPartSerializer(HttpPartSerializer _default) {
		return partSerializer == null ? _default : partSerializer;
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
