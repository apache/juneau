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

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;

/**
 * Represents the metadata gathered from a getter method of a class annotated with {@link Response}.
 */
public class ResponseBeanPropertyMeta {

	static ResponseBeanPropertyMeta.Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final String partName;
	private final Method getter;
	private final HttpPartType partType;
	private final HttpPartSerializer serializer;
	private final HttpPartSchema schema;

	ResponseBeanPropertyMeta(Builder b, HttpPartSerializer serializer) {
		this.partType = b.partType;
		this.schema = b.schema.build();
		this.partName = StringUtils.firstNonEmpty(schema.getName(), b.name);
		this.getter = b.getter;
		this.serializer = schema.getSerializer() == null ? serializer : ClassUtils.newInstance(HttpPartSerializer.class, schema.getSerializer(), true, b.ps);
	}

	static class Builder {
		HttpPartType partType;
		HttpPartSchemaBuilder schema;
		String name;
		Method getter;
		PropertyStore ps = PropertyStore.DEFAULT;

		Builder name(String value) {
			name = value;
			return this;
		}

		Builder getter(Method value) {
			getter = value;
			return this;
		}

		Builder partType(HttpPartType value) {
			partType = value;
			return this;
		}

		Builder schema(HttpPartSchemaBuilder value) {
			schema = value;
			return this;
		}

		Builder apply(HttpPartSchemaBuilder s) {
			schema = s;
			return this;
		}

		ResponseBeanPropertyMeta build(HttpPartSerializer serializer) {
			return new ResponseBeanPropertyMeta(this, serializer);
		}
	}

	/**
	 * Returns the HTTP part name for this property (the query parameter name for example).
	 *
	 * @return The HTTP part name, or <jk>null</jk> if it doesn't have a part name.
	 */
	public String getPartName() {
		return partName;
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
	 * @return The serializer to use for serializing the bean property value.
	 */
	public HttpPartSerializer getSerializer() {
		return serializer;
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
