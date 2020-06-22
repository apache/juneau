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

import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.httppart.*;

/**
 * Builder for {@link SerializedNameValuePair} objects.
 */
public class SerializedNameValuePairBuilder implements NameValuePairable, Headerable {
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
	public SerializedNameValuePairBuilder name(String value) {
		this.name = value;
		return this;
	}

	/**
	 * Sets the POJO to serialize to the parameter value.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializedNameValuePairBuilder value(Object value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the POJO to serialize to the parameter value.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializedNameValuePairBuilder value(Supplier<Object> value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the HTTP part type.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializedNameValuePairBuilder type(HttpPartType value) {
		this.type = value;
		return this;
	}

	/**
	 * Sets the serializer to use for serializing the value to a string value.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public SerializedNameValuePairBuilder serializer(HttpPartSerializer value) {
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
	public SerializedNameValuePairBuilder serializer(HttpPartSerializerSession value) {
		return serializer(value, true);
	}

	/**
	 * Sets the serializer to use for serializing the value to a string value.
	 *
	 * @param value The new value for this property.
	 * @param overwrite If <jk>true</jk>, overwrites the existing value if the old value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public SerializedNameValuePairBuilder serializer(HttpPartSerializerSession value, boolean overwrite) {
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
	public SerializedNameValuePairBuilder schema(HttpPartSchema value) {
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

	@Override /* NameValuePairable */
	public NameValuePair asNameValuePair() {
		return build();
	}

	@Override /* Headerable */
	public Header asHeader() {
		return build().asHeader();
	}
}