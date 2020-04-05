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
package org.apache.juneau.http;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries using the
 * {@link UrlEncodingSerializer class}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	NameValuePairs params = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> SerializedNameValuePair(<js>"myPojo"</js>, pojo, UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"someOtherParam"</js>, <js>"foobar"</js>));
 * 	request.setEntity(<jk>new</jk> UrlEncodedFormEntity(params));
 * </p>
 */
public class SerializedNameValuePair implements NameValuePair {
	private String name;
	private Object value;
	private HttpPartType type;
	private HttpPartSerializerSession serializer;
	private HttpPartSchema schema;
	private boolean skipIfEmpty;

	/**
	 * Instantiates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param name The parameter name.
	 * @param value The POJO to serialize to the parameter value.
	 * @param type The HTTP part type.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 */
	public SerializedNameValuePair(String name, Object value, HttpPartType type, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		this.name = name;
		this.value = value;
		this.type = type;
		this.serializer = serializer;
		this.schema = schema == null ? HttpPartSchema.DEFAULT : schema;
		this.skipIfEmpty = skipIfEmpty;
	}

	SerializedNameValuePair(Builder b) {
		this.name = b.name;
		this.value = b.value;
		this.type = b.type;
		this.serializer = b.serializer;
		this.schema = b.schema == null ? HttpPartSchema.DEFAULT : b.schema;
	}

	/**
	 * Builder for {@link SerializedNameValuePair} objects.
	 */
	public static class Builder {
		String name;
		Object value;
		HttpPartType type;
		HttpPartSerializerSession serializer;
		HttpPartSchema schema;

		/**
		 * Sets the parameter name.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the POJO to serialize to the parameter value.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder value(Object value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the HTTP part type.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder type(HttpPartType value) {
			this.type = value;
			return this;
		}

		/**
		 * Sets the serializer to use for serializing the value to a string value.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(HttpPartSerializer value) {
			if (value != null)
				return serializer(value.createPartSession(null));
			return this;
		}

		/**
		 * Sets the serializer to use for serializing the value to a string value.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(HttpPartSerializerSession value) {
			return serializer(value, true);
		}

		/**
		 * Sets the serializer to use for serializing the value to a string value.
		 *
		 * @param value The new value for this property.
		 * @param overwrite If <jk>true</jk>, overwrites the existing value if the old value is <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder serializer(HttpPartSerializerSession value, boolean overwrite) {
			if (overwrite || serializer == null)
				this.serializer = value;
			return this;
		}

		/**
		 * Sets the schema object that defines the format of the output.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder schema(HttpPartSchema value) {
			this.schema = value;
			return this;
		}

		/**
		 * Creates the new {@link SerializedNameValuePair}
		 *
		 * @return The new {@link SerializedNameValuePair}
		 */
		public SerializedNameValuePair build() {
			return new SerializedNameValuePair(this);
		}
	}

	@Override /* NameValuePair */
	public String getName() {
		return name;
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			if (value == null) {
				if (schema == null)
					return null;
				if (schema.getDefault() == null && ! schema.isRequired())
					return null;
			}
			if (isEmpty(value) && skipIfEmpty && schema.getDefault() == null)
				return null;
			return serializer.serialize(type, schema, value);
		} catch (SchemaValidationException e) {
			throw new BasicRuntimeException(e, "Validation error on request {0} parameter ''{1}''=''{2}''", type, name, value);
		} catch (SerializeException e) {
			throw new BasicRuntimeException(e, "Serialization error on request {0} parameter ''{1}''", type, name);
		}
	}

	@Override /* Object */
	public String toString() {
		return name + "=" + getValue();
	}
}
