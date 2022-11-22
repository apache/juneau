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
package org.apache.juneau.http.header;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.serializer.*;

/**
 * TODO
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 *
 * @serial exclude
 */
public class SerializedHeader extends BasicHeader {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final long serialVersionUID = 1L;

	/**
	 * Static creator.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The POJO to serialize as the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader of(String name, Object value) {
		return new SerializedHeader(name, value, null, null, false);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The supplier of the POJO to serialize as the header value.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader of(String name, Supplier<?> value) {
		return new SerializedHeader(name, value, null, null, false);
	}

	/**
	 * Static creator.
	 *
	 * @param name The HTTP header name name.
	 * @param value
	 * 	The POJO to serialize as the header value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader of(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return new SerializedHeader(name, value, serializer, schema, skipIfEmpty);
	}

	/**
	 * Static creator with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The HTTP header name name.
	 * @param value
	 * 	The supplier of the POJO to serialize as the header value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @return A new header bean, or <jk>null</jk> if the value is <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public static SerializedHeader of(String name, Supplier<?> value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		return new SerializedHeader(name, value, serializer, schema, skipIfEmpty);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Object value;
	private final Supplier<Object> supplier;
	private HttpPartSerializerSession serializer;
	private HttpPartSchema schema = HttpPartSchema.DEFAULT;
	private boolean skipIfEmpty;

	/**
	 * Constructor.
	 *
	 * @param name The HTTP header name name.
	 * @param value The POJO to serialize to the parameter value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	@SuppressWarnings("unchecked")
	public SerializedHeader(String name, Object value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		super(name, null);
		this.value = value instanceof Supplier ? null : value;
		this.supplier = value instanceof Supplier ? (Supplier<Object>)value : null;
		this.serializer = serializer;
		this.schema = schema;
		this.skipIfEmpty = skipIfEmpty;
	}

	/**
	 * Constructor with delayed value.
	 *
	 * <p>
	 * Header value is re-evaluated on each call to {@link #getValue()}.
	 *
	 * @param name The HTTP header name name.
	 * @param value The supplier of the POJO to serialize to the parameter value.
	 * @param serializer
	 * 	The serializer to use for serializing the value to a string value.
	 * @param schema
	 * 	The schema object that defines the format of the output.
	 * 	<br>If <jk>null</jk>, defaults to the schema defined on the serializer.
	 * 	<br>If that's also <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
	 * 	<br>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
	 * 	<br>Can also be a {@link Supplier}.
	 * @param skipIfEmpty If value is a blank string, the value should return as <jk>null</jk>.
	 * @throws IllegalArgumentException If name is <jk>null</jk> or empty.
	 */
	public SerializedHeader(String name, Supplier<Object> value, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		super(name, null);
		this.value = null;
		this.supplier = value;
		this.serializer = serializer;
		this.schema = schema;
		this.skipIfEmpty = skipIfEmpty;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected SerializedHeader(SerializedHeader copyFrom) {
		super(copyFrom);
		this.value = copyFrom.value;
		this.supplier = copyFrom.supplier;
		this.serializer = copyFrom.serializer == null ? serializer : copyFrom.serializer;
		this.schema = copyFrom.schema == null ? schema : copyFrom.schema;
		this.skipIfEmpty = copyFrom.skipIfEmpty;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A new copy of this object.
	 */
	public SerializedHeader copy() {
		return new SerializedHeader(this);
	}

	/**
	 * Sets the serializer to use for serializing the value to a string value.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedHeader serializer(HttpPartSerializer value) {
		if (value != null)
			return serializer(value.getPartSession());
		return this;
	}

	/**
	 * Sets the serializer to use for serializing the value to a string value.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedHeader serializer(HttpPartSerializerSession value) {
		serializer = value;
		return this;
	}

	/**
	 * Sets the schema object that defines the format of the output.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedHeader schema(HttpPartSchema value) {
		this.schema = value;
		return this;
	}

	/**
	 * Copies this bean and sets the serializer and schema on it.
	 *
	 * @param serializer The new serializer for the bean.  Can be <jk>null</jk>.
	 * @param schema The new schema for the bean.  Can be <jk>null</jk>.
	 * @return Either a new bean with the serializer set, or this bean if
	 * 	both values are <jk>null</jk> or the serializer and schema were already set.
	 */
	public SerializedHeader copyWith(HttpPartSerializerSession serializer, HttpPartSchema schema) {
		if ((this.serializer == null && serializer != null) || (this.schema == null && schema != null)) {
			SerializedHeader h = copy();
			if (serializer != null)
				h.serializer(serializer);
			if (schema != null)
				h.schema(schema);
			return h;
		}
		return this;
	}

	/**
	 * Don't serialize this header if the value is <jk>null</jk> or an empty string.
	 *
	 * @return This object.
	 */
	public SerializedHeader skipIfEmpty() {
		return skipIfEmpty(true);
	}

	/**
	 * Don't serialize this header if the value is <jk>null</jk> or an empty string.
	 *
	 * @param value The new value of this setting.
	 * @return This object.
	 */
	public SerializedHeader skipIfEmpty(boolean value) {
		this.skipIfEmpty = value;
		return this;
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			Object v = value;
			if (supplier != null)
				v = supplier.get();
			HttpPartSchema schema = this.schema == null ? HttpPartSchema.DEFAULT : this.schema;
			String def = schema.getDefault();
			if (v == null) {
				if ((def == null && ! schema.isRequired()) || (def == null && schema.isAllowEmptyValue()))
					return null;
			}
			if (isEmpty(stringify(v)) && skipIfEmpty && def == null)
				return null;
			return serializer == null ? stringify(v) : serializer.serialize(HttpPartType.HEADER, schema, v);
		} catch (SchemaValidationException e) {
			throw new BasicRuntimeException(e, "Validation error on request {0} parameter ''{1}''=''{2}''", HttpPartType.HEADER, getName(), value);
		} catch (SerializeException e) {
			throw new BasicRuntimeException(e, "Serialization error on request {0} parameter ''{1}''", HttpPartType.HEADER, getName());
		}
	}
}
