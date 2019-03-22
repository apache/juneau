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

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.reflection.*;

/**
 * Represents the metadata gathered from a getter method of a class annotated with {@link Request}.
 */
public class RequestBeanPropertyMeta {

	static RequestBeanPropertyMeta.Builder create(HttpPartType partType, Class<? extends Annotation> c, MethodInfo m) {
		HttpPartSchemaBuilder sb = HttpPartSchema.create().name(m.getPropertyName());
		for (Annotation a : m.getAnnotations(c, true))
			sb.apply(a);
		return new Builder().partType(partType).schema(sb.build()).getter(m.getInner());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Method getter;
	private final HttpPartType partType;
	private final HttpPartSerializer serializer;
	private final HttpPartParser parser;
	private final HttpPartSchema schema;

	RequestBeanPropertyMeta(Builder b, HttpPartSerializer serializer, HttpPartParser parser) {
		this.partType = b.partType;
		this.schema = b.schema;
		this.getter = b.getter;
		this.serializer = schema.getSerializer() == null ? serializer : castOrCreate(HttpPartSerializer.class, schema.getSerializer(), true, b.ps);
		this.parser = schema.getParser() == null ? parser : castOrCreate(HttpPartParser.class, schema.getParser(), true, b.ps);
	}

	static class Builder {
		HttpPartType partType;
		HttpPartSchema schema;
		Method getter;
		PropertyStore ps = PropertyStore.DEFAULT;

		Builder getter(Method value) {
			getter = value;
			return this;
		}

		Builder partType(HttpPartType value) {
			partType = value;
			return this;
		}

		Builder schema(HttpPartSchema value) {
			schema = value;
			return this;
		}

		RequestBeanPropertyMeta build(HttpPartSerializer serializer, HttpPartParser parser) {
			return new RequestBeanPropertyMeta(this, serializer, parser);
		}
	}

	/**
	 * Returns the HTTP part name for this property (query parameter name for example).
	 *
	 * @return The HTTP part name, or <jk>null</jk> if it doesn't have a part name.
	 */
	public String getPartName() {
		return schema == null ? null : schema.getName();
	}

	/**
	 * Returns the name of the Java method getter that defines this property.
	 *
	 * @return
	 * 	The name of the Java method getter that defines this property.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Method getGetter() {
		return getter;
	}

	/**
	 * Returns the HTTP part type for this property (query parameter for example).
	 *
	 * @return
	 * 	The HTTP part type for this property.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartType getPartType() {
		return partType;
	}

	/**
	 * Returns the serializer to use for serializing the bean property value.
	 *
	 * @param _default The default serializer to use if not defined on the annotation.
	 * @return The serializer to use for serializing the bean property value.
	 */
	public HttpPartSerializer getSerializer(HttpPartSerializer _default) {
		return serializer == null ? _default : serializer;
	}

	/**
	 * Returns the parser to use for parsing the bean property value.
	 *
	 * @param _default The default parsing to use if not defined on the annotation.
	 * @return The parsing to use for serializing the bean property value.
	 */
	public HttpPartParser getParser(HttpPartParser _default) {
		return parser == null ? _default : parser;
	}

	/**
	 * Returns the schema information gathered from annotations on the method and return type.
	 *
	 * @return
	 * 	The schema information gathered from annotations on the method and return type.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSchema getSchema() {
		return schema;
	}
}
