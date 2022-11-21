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
package org.apache.juneau.http.part;

import static org.apache.juneau.common.internal.StringUtils.*;

import java.util.function.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.urlencoding.*;

/**
 * Subclass of {@link NameValuePair} for serializing POJOs as URL-encoded form post entries using the
 * {@link UrlEncodingSerializer class}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	NameValuePairs <jv>params</jv> = <jk>new</jk> NameValuePairs()
 * 		.append(<jk>new</jk> SerializedNameValuePair(<js>"myPojo"</js>, <jv>pojo</jv>, UrlEncodingSerializer.<jsf>DEFAULT_SIMPLE</jsf>))
 * 		.append(<jk>new</jk> BasicNameValuePair(<js>"someOtherParam"</js>, <js>"foobar"</js>));
 * 	<jv>request</jv>.setEntity(<jk>new</jk> UrlEncodedFormEntity(<jv>params</jv>));
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-common">juneau-rest-common</a>
 * </ul>
 */
public class SerializedPart extends BasicPart implements Headerable {
	private final Object value;
	private HttpPartType type;
	private HttpPartSerializerSession serializer;
	private HttpPartSchema schema = HttpPartSchema.DEFAULT;
	private boolean skipIfEmpty;

	/**
	 * Instantiates a new instance of this object.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value.
	 * 	<br>Can be any POJO.
	 * @return A new {@link SerializedPart} object, never <jk>null</jk>.
	 */
	public static SerializedPart of(String name, Object value) {
		return new SerializedPart(name, value, null, null, null, false);
	}

	/**
	 * Instantiates a new instance of this object.
	 *
	 * @param name The part name.
	 * @param value
	 * 	The part value supplier.
	 * 	<br>Can be a supplier of any POJO.
	 * @return A new {@link SerializedPart} object, never <jk>null</jk>.
	 */
	public static SerializedPart of(String name, Supplier<?> value) {
		return new SerializedPart(name, value, null, null, null, false);
	}

	/**
	 * Constructor.
	 *
	 * @param name The part name.
	 * @param value The POJO to serialize to The part value.
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
	public SerializedPart(String name, Object value, HttpPartType type, HttpPartSerializerSession serializer, HttpPartSchema schema, boolean skipIfEmpty) {
		super(name, value);
		this.value = value;
		this.type = type;
		this.serializer = serializer;
		this.schema = schema;
		this.skipIfEmpty = skipIfEmpty;
	}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The object to copy.
	 */
	protected SerializedPart(SerializedPart copyFrom) {
		super(copyFrom);
		this.value = copyFrom.value;
		this.type = copyFrom.type;
		this.serializer = copyFrom.serializer == null ? serializer : copyFrom.serializer;
		this.schema = copyFrom.schema == null ? schema : copyFrom.schema;
		this.skipIfEmpty = copyFrom.skipIfEmpty;
	}

	/**
	 * Creates a copy of this object.
	 *
	 * @return A new copy of this object.
	 */
	public SerializedPart copy() {
		return new SerializedPart(this);
	}

	/**
	 * Sets the HTTP part type.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedPart type(HttpPartType value) {
		type = value;
		return this;
	}

	/**
	 * Sets the serializer to use for serializing the value to a string value.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedPart serializer(HttpPartSerializer value) {
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
	public SerializedPart serializer(HttpPartSerializerSession value) {
		serializer = value;
		return this;
	}

	/**
	 * Sets the schema object that defines the format of the output.
	 *
	 * @param value The new value for this property.
	 * @return This object.
	 */
	public SerializedPart schema(HttpPartSchema value) {
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
	public SerializedPart copyWith(HttpPartSerializerSession serializer, HttpPartSchema schema) {
		if ((this.serializer == null && serializer != null) || (this.schema == null && schema != null)) {
			SerializedPart p = copy();
			if (serializer != null)
				p.serializer(serializer);
			if (schema != null)
				p.schema(schema);
			return p;
		}
		return this;
	}

	/**
	 * Don't serialize this pair if the value is <jk>null</jk> or an empty string.
	 *
	 * @return This object.
	 */
	public SerializedPart skipIfEmpty() {
		return skipIfEmpty(true);
	}

	/**
	 * Don't serialize this pair if the value is <jk>null</jk> or an empty string.
	 *
	 * @param value The new value of this setting.
	 * @return This object.
	 */
	public SerializedPart skipIfEmpty(boolean value) {
		this.skipIfEmpty = value;
		return this;
	}

	@Override /* Headerable */
	public SerializedHeader asHeader() {
		return new SerializedHeader(getName(), value, serializer, schema, skipIfEmpty);
	}

	@Override /* NameValuePair */
	public String getValue() {
		try {
			Object v = unwrap(value);
			HttpPartSchema schema = this.schema == null ? HttpPartSchema.DEFAULT : this.schema;
			String def = schema.getDefault();
			if (v == null) {
				if ((def == null && ! schema.isRequired()) || (def == null && schema.isAllowEmptyValue()))
					return null;
			}
			if (isEmpty(stringify(v)) && skipIfEmpty && def == null)
				return null;
			return serializer == null ? stringify(v) : serializer.serialize(type, schema, v);
		} catch (SchemaValidationException e) {
			throw new BasicRuntimeException(e, "Validation error on request {0} part ''{1}''=''{2}''", type, getName(), value);
		} catch (SerializeException e) {
			throw new BasicRuntimeException(e, "Serialization error on request {0} part ''{1}''", type, getName());
		}
	}

	private Object unwrap(Object o) {
		if (o instanceof Supplier)
			return ((Supplier<?>)o).get();
		return o;
	}
}
