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

import static java.util.Collections.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.oapi.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.utils.*;

/**
 * Represents an OpenAPI schema definition.
 * 
 * <p>
 * The schema definition can be applied to any HTTP parts such as bodies, headers, query/form parameters, and URL path parts.
 * <br>The API is generic enough to apply to any path part although some attributes may only applicable for certain parts.
 * 
 * <p>
 * Schema objects are created via builders instantiated through the {@link #create()} method.
 * 
 * <p>
 * This class is thread safe and reusable.
 */
public class HttpPartSchema {
	
	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link OapiPartSerializer}, all default settings. */
	public static final HttpPartSchema DEFAULT = HttpPartSchema.create().build();

	final String _default;
	final Set<String> _enum;
	final Map<String,HttpPartSchema> properties;
	final Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems;
	final CollectionFormat collectionFormat;
	final Type type;
	final Format format;
	final Pattern pattern;
	final HttpPartSchema items, additionalProperties;
	final Number maximum, minimum, multipleOf;
	final Integer maxItems, maxLength, maxProperties, minItems, minLength, minProperties;
	
	/**
	 * Instantiates a new builder for this object.
	 * 
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}
	
	HttpPartSchema(Builder b) {
		this._default = b._default; 
		this._enum = copy(b._enum);
		this.properties = build(b.properties);
		this.allowEmptyValue = b.allowEmptyValue;
		this.exclusiveMaximum = b.exclusiveMaximum;
		this.exclusiveMinimum = b.exclusiveMinimum;
		this.required = b.required;
		this.uniqueItems = b.uniqueItems;
		this.collectionFormat = b.collectionFormat;
		this.type = b.type;
		this.format = b.format;
		this.pattern = b.pattern;
		this.items = build(b.items);
		this.additionalProperties = build(b.additionalProperties);
		this.maximum = b.maximum;
		this.minimum = b.minimum;
		this.multipleOf = b.multipleOf;
		this.maxItems = b.maxItems;
		this.maxLength = b.maxLength;
		this.maxProperties = b.maxProperties;
		this.minItems = b.minItems;
		this.minLength = b.minLength;
		this.minProperties = b.minProperties;
		
		if (b.noValidate)
			return;
		
		// Validation.
		List<String> errors = new ArrayList<>();
		AList<String> notAllowed = new AList<>();
		boolean invalidFormat = false;
		switch (type) {
			case STRING: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum != null, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum != null, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems != null, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NONE, "collectionFormat");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.BYTE, Format.BINARY, Format.DATE, Format.DATE_TIME, Format.PASSWORD, Format.UON, Format.NONE);
				break;
			}
			case ARRAY: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum != null, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum != null, "exclusiveMinimum");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NONE, Format.UON);
				break;
			}
			case BOOLEAN: {
				notAllowed.appendIf(_enum != null, "_enum");
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum != null, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum != null, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems != null, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NONE, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NONE);
				break;
			}
			case FILE: {
				break;
			}
			case INTEGER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems != null, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NONE, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NONE, Format.INT32, Format.INT64);
				break;
			}
			case NUMBER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems != null, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NONE, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NONE, Format.FLOAT, Format.DOUBLE);
				break;
			}
			case OBJECT: {
				notAllowed.appendIf(exclusiveMaximum != null, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum != null, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems != null, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NONE, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				invalidFormat = ! format.isOneOf(Format.NONE, Format.UON);
				break;
			}
			default:
				break;
		}

		if (! notAllowed.isEmpty())
			errors.add("Attributes not allow for type='"+type+"': " + StringUtils.join(notAllowed, ","));
		if (invalidFormat)
			errors.add("Invalid format for type='"+type+"': '"+format+"'");
		if (exclusiveMaximum != null && maximum == null)
			errors.add("Cannot specify exclusiveMaximum with maximum.");
		if (exclusiveMinimum != null && minimum == null)
			errors.add("Cannot specify exclusiveMinimum with minimum.");
		if (required != null && required && _default != null)
			errors.add("Cannot specify a default value on a required value.");
		
		if (! errors.isEmpty())
			throw new ContextRuntimeException("Schema specification errors: \n\t" + join(errors, "\n\t")); 
	}
	
	/**
	 * The builder class for creating {@link HttpPartSchema} objects.
	 * 
	 */
	public static class Builder {
		String _default;
		Set<String> _enum;
		Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems;
		CollectionFormat collectionFormat = CollectionFormat.NONE;
		Type type = Type.NONE;
		Format format = Format.NONE;
		Pattern pattern;
		Number maximum, minimum, multipleOf;
		Integer maxItems, maxLength, maxProperties, minItems, minLength, minProperties;
		Map<String,Builder> properties;
		HttpPartSchema.Builder items, additionalProperties;
		boolean noValidate;
		
		/**
		 * Instantiates a new {@link HttpPartSchema} object based on the configuration of this builder.
		 * 
		 * <p>
		 * This method can be called multiple times to produce new schema objects.
		 * 
		 * @return 
		 * 	A new {@link HttpPartSchema} object.
		 * 	<br>Never <jk>null</jk>.
		 */
		public HttpPartSchema build() {
			return new HttpPartSchema(this);
		}

		/**
		 * <mk>required</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Determines whether the parameter is mandatory.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder required(Boolean value) {
			if (value != null)
				this.required = value;
			return this;
		}
	
		/**
		 * <mk>type</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * The type of the parameter. 
		 * 
		 * <p> 
		 * The possible values are:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"string"</js>
		 * 		<br>Parameter must be a string or a POJO convertible from a string.
		 * 	<li>
		 * 		<js>"number"</js>
		 * 		<br>Parameter must be a number primitive or number object.
		 * 		<br>If parameter is <code>Object</code>, creates either a <code>Float</code> or <code>Double</code> depending on the size of the number.
		 * 	<li>
		 * 		<js>"integer"</js>
		 * 		<br>Parameter must be a integer/long primitive or integer/long object.
		 * 		<br>If parameter is <code>Object</code>, creates either a <code>Short</code>, <code>Integer</code>, or <code>Long</code> depending on the size of the number.
		 * 	<li>
		 * 		<js>"boolean"</js>
		 * 		<br>Parameter must be a boolean primitive or object.
		 * 	<li>
		 * 		<js>"array"</js>
		 * 		<br>Parameter must be an array or collection.
		 * 		<br>Elements must be strings or POJOs convertible from strings.
		 * 		<br>If parameter is <code>Object</code>, creates an {@link ObjectList}.
		 * 	<li>
		 * 		<js>"object"</js>
		 * 		<br>Parameter must be a map or bean.
		 * 		<br>If parameter is <code>Object</code>, creates an {@link ObjectMap}.
		 * 		<br>Note that this is an extension of the OpenAPI schema as Juneau allows for arbitrarily-complex POJOs to be serialized as HTTP parts.
		 * 	<li>
		 * 		<js>"file"</js>
		 * 		<br>This type is currently not supported.
		 * </ul>
		 * 
		 * <p>
		 * If the type is not specified, it will be auto-detected based on the parameter class type.
		 * 
		 * <h5 class='section'>See Also:</h5>
		 * <ul class='doctree'>
		 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
		 * </ul>
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder type(String value) {
			try {
				if (isNotEmpty(value))
					this.type = Type.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as type value.  Valid values: {1}", value, Type.values()); 
			}
			return this;
		}
	
		/**
		 * <mk>format</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * The extending format for the previously mentioned <a href='https://swagger.io/specification/v2/#parameterType'>type</a>. 
		 * 
		 * <p> 
		 * The possible values are:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"int32"</js> - Signed 32 bits.
		 * 		<br>Only valid with type <js>"integer"</js>.
		 * 	<li>
		 * 		<js>"int64"</js> - Signed 64 bits.
		 * 		<br>Only valid with type <js>"integer"</js>.
		 * 	<li>
		 * 		<js>"float"</js> - 32-bit floating point number.
		 * 		<br>Only valid with type <js>"number"</js>.
		 * 	<li>
		 * 		<js>"double"</js> - 64-bit floating point number.
		 * 		<br>Only valid with type <js>"number"</js>.
		 * 	<li>
		 * 		<js>"byte"</js> - BASE-64 encoded characters.
		 * 		<br>Only valid with type <js>"string"</js>.
		 * 		<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
		 * 	<li>
		 * 		<js>"binary"</js> - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 * 		<br>Only valid with type <js>"string"</js>.
		 * 		<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
		 * 	<li>
		 * 		<js>"date"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 * 		<br>Only valid with type <js>"string"</js>.
		 * 	<li>
		 * 		<js>"date-time"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 * 		<br>Only valid with type <js>"string"</js>.
		 * 	<li>
		 * 		<js>"password"</js> - Used to hint UIs the input needs to be obscured.
		 * 		<br>This format does not affect the serialization or parsing of the parameter.
		 * 	<li>
		 * 		<js>"uon"</js> - UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>). 
		 * 		<br>Only valid with type <js>"object"</js>.
		 * 		<br>If not specified, then the input is interpreted as plain-text and is converted to a POJO directly.
		 * </ul>
		 * 
		 * <h5 class='section'>See Also:</h5>
		 * <ul class='doctree'>
		 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/v2/#dataTypeFormat'>Swagger specification &gt; Data Type Formats</a>
		 * </ul>
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder format(String value) {
			try {
				if (isNotEmpty(value))
					this.format = Format.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as format value.  Valid values: {1}", value, Format.values()); 
			}
			return this;
		}
	
		/**
		 * <mk>allowEmptyValue</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Sets the ability to pass empty-valued parameters. 
		 * <br>This is valid only for either query or formData parameters and allows you to send a parameter with a name only or an empty value. 
		 * <br>The default value is <jk>false</jk>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder allowEmptyValue(Boolean value) {
			if (value != null)
				this.allowEmptyValue = value;
			return this;
		}
	
		/**
		 * <mk>items</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Describes the type of items in the array.
		 * <p>
		 * Required if <code>type</code> is <js>"array"</js>. 
		 * <br>Can only be used if <code>type</code> is <js>"array"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder items(Builder value) {
			if (value != null)
				items = value;
			return this;
		}

		/**
		 * <mk>collectionFormat</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Determines the format of the array if <code>type</code> <js>"array"</js> is used. 
		 * <br>Can only be used if <code>type</code> is <js>"array"</js>.
		 * 
		 * <br>Possible values are:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<js>"csv"</js> (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
		 * 	<li>
		 * 		<js>"ssv"</js> - Space-separated values (e.g. <js>"foo bar"</js>).
		 * 	<li>
		 * 		<js>"tsv"</js> - Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 * 	<li>
		 * 		<js>"pipes</js> - Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 * 	<li>
		 * 		<js>"multi"</js> - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>). 
		 * 	<li>
		 * 		<js>"uon"</js> - UON notation (e.g. <js>"@(foo,bar)"</js>). 
		 * 	<li>
		 * </ul>
		 * 
		 * <p>
		 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements. 
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder collectionFormat(String value) {
			try {
				if (isNotEmpty(value))
					this.collectionFormat = CollectionFormat.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as collectionFormat value.  Valid values: {1}", value, CollectionFormat.values()); 
			}
			return this;
		}
	
		/**
		 * <mk>default</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Declares the value of the parameter that the server will use if none is provided, for example a "count" to control the number of results per page might default to 100 if not supplied by the client in the request. 
		 * <br>(Note: "default" has no meaning for required parameters.) 
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _default(String value) {
			if (isNotEmpty(value))
				this._default = value;
			return this;
		}
	
		/**
		 * <mk>maximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Defines the maximum value for a parameter of numeric types.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maximum(Number value) {
			if (value != null)
				this.maximum = value;
			return this;
		}
	
		/**
		 * <mk>exclusiveMaximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Defines whether the maximum is matched exclusively.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * <br>If <jk>true</jk>, must be accompanied with <code>maximum</code>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMaximum(Boolean value) {
			if (value != null)
				this.exclusiveMaximum = value;
			return this;
		}
	
		/**
		 * <mk>minimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Defines the minimum value for a parameter of numeric types.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minimum(Number value) {
			if (value != null)
				this.minimum = value;
			return this;
		}
	
		/**
		 * <mk>exclusiveMinimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * Defines whether the minimum is matched exclusively.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * <br>If <jk>true</jk>, must be accompanied with <code>minimum</code>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder exclusiveMinimum(Boolean value) {
			if (value != null)
				this.exclusiveMinimum = value;
			return this;
		}
	
		/**
		 * <mk>maxLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword.
		 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"string"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxLength(Integer value) {
			if (value != null)
				this.maxLength = value;
			return this;
		}
	
		/**
		 * <mk>minLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword.
		 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"string"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minLength(Integer value) {
			if (value != null)
				this.minLength = value;
			return this;
		}
	
		/**
		 * <mk>pattern</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * A string input is valid if it matches the specified regular expression pattern.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"string"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder pattern(String value) {
			try {
				if (isNotEmpty(value))
					this.pattern = Pattern.compile(value);
			} catch (Exception e) {
				throw new ContextRuntimeException(e, "Invalid value {0} passed in as pattern value.  Must be a valid regular expression.", value); 
			}
			return this;
		}

		/**
		 * <mk>maxItems</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * An array or collection is valid if its size is less than, or equal to, the value of this keyword.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"array"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxItems(Integer value) {
			if (value != null)
				this.maxItems = value;
			return this;
		}
	
		/**
		 * <mk>minItems</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * An array or collection is valid if its size is greater than, or equal to, the value of this keyword.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"array"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minItems(Integer value) {
			if (value != null)
				this.minItems = value;
			return this;
		}
	
		/**
		 * <mk>uniqueItems</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * If <jk>true</jk>, the input validates successfully if all of its elements are unique.
		 * 
		 * <p>
		 * <br>If the parameter type is a subclass of {@link Set}, this validation is skipped (since a set can only contain unique items anyway).
		 * <br>Otherwise, the collection or array is checked for duplicate items.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"array"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uniqueItems(Boolean value) {
			if (value != null)
				this.uniqueItems = value;
			return this;
		}
	
		/**
		 * <mk>enum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * If specified, the input validates successfully if it is equal to one of the elements in this array.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder _enum(Set<String> value) {
			if (value != null)
				this._enum = value;
			return this;
		}
	
		/**
		 * <mk>multipleOf</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
		 * 
		 * <p>
		 * A numeric instance is valid if the result of the division of the instance by this keyword's value is an integer.
		 * 
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder multipleOf(Number value) {
			if (value != null)
				this.multipleOf = value;
			return this;
		}
	
		/**
		 * TODO
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder maxProperties(Integer value) {
			if (value != null)
				this.maxProperties = value;
			return this;
		}
	
		/**
		 * TODO
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder minProperties(Integer value) {
			if (value != null)
				this.minProperties = value;
			return this;
		}
	
		/**
		 * TODO
		 * 
		 * @param name The property name.
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder property(String name, Builder value) {
			if (isNotEmpty(name) && isNotEmpty(value))
				properties.put(name, value);
			return this;
		}
	
		/**
		 * TODO
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder additionalProperties(Builder value) {
			if (value != null)
				additionalProperties = value;
			return additionalProperties;
		}
		
		/**
		 * TODO
		 * 
		 * @param value 
		 * 	The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder noValidate(boolean value) {
			this.noValidate = value;
			return this;
		}
	}
	
	/**
	 * Valid values for the <code>collectionFormat</code> field.
	 */
	public static enum CollectionFormat {
		
		/**
		 * Comma-separated values (e.g. <js>"foo,bar"</js>).
		 */
		CSV, 
		
		/**
		 * Space-separated values (e.g. <js>"foo bar"</js>).
		 */
		SSV, 
		
		/**
		 * Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 */
		TSV, 
		
		/**
		 * Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 */
		PIPES, 
		
		/**
		 * Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>). 
		 */
		MULTI, 
		
		/**
		 * UON notation (e.g. <js>"@(foo,bar)"</js>). 
		 */
		UON,

		/**
		 * Not specified.
		 */
		NONE;
		
		static CollectionFormat fromString(String value) {
			
			return valueOf(value.toUpperCase());
		}
		
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}
	
	/**
	 * Valid values for the <code>type</code> field.
	 */
	public static enum Type {
		
		/**
		 * String.
		 */
		STRING, 
		
		/**
		 * Floating point number.
		 */
		NUMBER, 
		
		/**
		 * Decimal number.
		 */
		INTEGER, 
		
		/**
		 * Boolean.
		 */
		BOOLEAN, 
		
		/**
		 * Array or collection.
		 */
		ARRAY, 
		
		/**
		 * Map or bean.
		 */
		OBJECT, 
		
		/**
		 * File.
		 */
		FILE,
		
		/**
		 * Not specified.
		 */
		NONE;
		
		static Type fromString(String value) {
			return valueOf(value.toUpperCase());
		}
		
		@Override
		public String toString() {
			return name().toLowerCase();
		}
	}

	/**
	 * Valid values for the <code>format</code> field.
	 */
	public static enum Format {
		
		/**
		 * Signed 32 bits.
		 */
		INT32, 
		
		/**
		 * Signed 64 bits.
		 */
		INT64, 
		
		/**
		 * 32-bit floating point number.
		 */
		FLOAT, 
		
		/**
		 * 64-bit floating point number.
		 */
		DOUBLE, 
		
		/**
		 * BASE-64 encoded characters.
		 */
		BYTE, 
		
		/**
		 * Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 */
		BINARY, 
		
		/**
		 * An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 */
		DATE, 
		
		/**
		 *  An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 */
		DATE_TIME, 
		
		/**
		 * Used to hint UIs the input needs to be obscured.
		 */
		PASSWORD, 
		
		/**
		 * UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>). 
		 */
		UON, 

		/**
		 * Not specified.
		 */
		NONE;
		
		static Format fromString(String value) {
			value = value.toUpperCase().replace('-','_');
			return valueOf(value);
		}
		
		@Override
		public String toString() {
			String s = name().toLowerCase().replace('_','-');
			return s;
		}
		
		/**
		 * Returns <jk>true</jk> if this format is in the provided list.
		 * 
		 * @param list The list of formats to check against.
		 * @return <jk>true</jk> if this format is in the provided list.
		 */
		public boolean isOneOf(Format...list) {
			for (Format ff : list)
				if (this == ff)
					return true;
			return false;
		}
	}

	/**
	 * Returns the default value for this schema.
	 * 
	 * @return The default value for this schema, or <jk>null</jk> if not specified. 
	 */
	public String getDefault() {
		return _default;
	}

	/**
	 * Returns the <code>collectionFormat</code> field of this schema.
	 * 
	 * @return The <code>collectionFormat</code> field of this schema.
	 */
	public CollectionFormat getCollectionFormat() {
		return collectionFormat;
	}
	
	/**
	 * Returns the type field of this schema.
	 * 
	 * @param cm 
	 * 	The class meta of the object.
	 * 	<br>Used to auto-detect the type if the type was not specified. 
	 * @return The format field of this schema.
	 */
	public Type getType(ClassMeta<?> cm) {
		if (type != Type.NONE)
			return type;
		if (cm.isMapOrBean())
			return Type.OBJECT;
		if (cm.isCollectionOrArray())
			return Type.ARRAY;
		if (cm.isInteger())
			return Type.INTEGER;
		if (cm.isNumber())
			return Type.NUMBER;
		if (cm.isBoolean())
			return Type.BOOLEAN;
		return Type.STRING;
	}

	/**
	 * Returns the <code>format</code> field of this schema.
	 * 
	 * @return The <code>format</code> field of this schema.
	 */
	public Format getFormat() {
		return format;
	}
	
	/**
	 * Returns the schema for child items of the object represented by this schema.
	 * 
	 * @return The schema for child items of the object represented by this schema, or <jk>null</jk> if not defined.
	 */
	public HttpPartSchema getItems() {
		return items;
	}
	
	/**
	 * Throws a {@link ParseException} if the specified pre-parsed input does not validate against this schema.
	 * 
	 * @param in The input.
	 * @return The same object passed in.
	 * @throws ParseException if the specified pre-parsed input does not validate against this schema.
	 */
	public String validateInput(String in) throws ParseException {
		if (! isValidRequired(in))
			throw new ParseException("No value specified.");
		if (in != null) {
			if (! isValidAllowEmpty(in))
				throw new ParseException("Empty value not allowed.");
			if (! isValidPattern(in))
				throw new ParseException("Value does not match expected pattern.  Must match pattern: {0}", pattern.pattern());
			if (! isValidEnum(in))
				throw new ParseException("Value does not match one of the expected values.  Must be one of the following: {0}", _enum);
			if (! isValidMaxLength(in))
				throw new ParseException("Maximum length of value exceeded.");
			if (! isValidMinLength(in))
				throw new ParseException("Minimum length of value exceeded.");
		}
		return in;
	}
	
	/**
	 * Throws a {@link ParseException} if the specified parsed output does not validate against this schema.
	 * 
	 * @param o The parsed output.
	 * @param bc The bean context used to detect POJO types.
	 * @return The same object passed in.
	 * @throws ParseException if the specified parsed output does not validate against this schema.
	 */
	@SuppressWarnings("rawtypes")
	public Object validateOutput(Object o, BeanContext bc) throws ParseException {
		if (o == null) {
			if (! isValidRequired(o))
				throw new ParseException("Required value not provided.");
			return o;
		}
		ClassMeta<?> cm = bc.getClassMetaForObject(o);
		switch (getType(cm)) {
			case STRING: {
				if (cm.isString()) 
					return validateInput(o.toString());
				break;
			}
			case ARRAY: {
				if (cm.isArray()) {
					if (! isValidMinItems(o))
						throw new ParseException("Minimum items of value exceeded.");
					if (! isValidMaxItems(o))
						throw new ParseException("Maximum items of value exceeded.");
					if (! isValidUniqueItems(o)) 
						throw new ParseException("Duplicate items found.");
					HttpPartSchema items = getItems();
					if (items != null) 
						for (int i = 0; i < Array.getLength(o); i++)
							items.validateOutput(Array.get(o, i), bc);
				} else if (cm.isCollection()) {
					Collection<?> c = (Collection<?>)o;
					if (! isValidMinItems(c))
						throw new ParseException("Minimum items of value exceeded.");
					if (! isValidMaxItems(c))
						throw new ParseException("Maximum items of value exceeded.");
					if (! isValidUniqueItems(c))
						throw new ParseException("Duplicate items found.");
					HttpPartSchema items = getItems();
					if (items != null) 
						for (Object o2 : c)
							items.validateOutput(o2, bc);
				}
				break;
			}
			case INTEGER: {
				if (cm.isNumber()) {
					Number n = (Number)o;
					if (! isValidMinimum(n))
						throw new ParseException("Minimal value exceeded.");
					if (! isValidMaximum(n))
						throw new ParseException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new ParseException("Multiple-of not met.");
				}
				break;
			}
			case NUMBER: {
				if (cm.isNumber()) {
					Number n = (Number)o;
					if (! isValidMinimum(n))
						throw new ParseException("Minimal value exceeded.");
					if (! isValidMaximum(n))
						throw new ParseException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new ParseException("Multiple-of not met.");
				}
				break;
			}
			case OBJECT: {
				if (cm.isMapOrBean()) {
					Map<?,?> m = cm.isMap() ? (Map<?,?>)o : bc.createSession().toBeanMap(o);
					if (! isValidMinProperties(m))
						throw new ParseException("Minimum number properties of value not met.");
					if (! isValidMaxProperties(m))
						throw new ParseException("Maximum number of properties of value exceeded.");
					for (Map.Entry e : m.entrySet()) {
						String key = e.getKey().toString();
						HttpPartSchema s2 = getProperty(key);
						if (s2 != null) 
							s2.validateOutput(e.getValue(), bc);
					}
				} else if (cm.isBean()) {
					
				}
				break;
			}
			case BOOLEAN: 
			case FILE: 
			case NONE: 
				break;
		}		
		return o;
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------
	
	private boolean isValidRequired(Object x) {
		return x != null || required == null || ! required;
	}

	private boolean isValidMinProperties(Map<?,?> x) {
		return minProperties == null || x.size() >= minProperties;
	}

	private boolean isValidMaxProperties(Map<?,?> x) {
		return maxProperties == null || x.size() <= maxProperties;
	}
	
	private boolean isValidMinimum(Number x) {
		if (x instanceof Integer) 
			return minimum == null || x.intValue() > minimum.intValue() || (x.intValue() == minimum.intValue() && (exclusiveMinimum == null || ! exclusiveMinimum));
		if (x instanceof Short)
			return minimum == null || x.shortValue() > minimum.shortValue() || (x.intValue() == minimum.shortValue() && (exclusiveMinimum == null || ! exclusiveMinimum));
		if (x instanceof Long)
			return minimum == null || x.longValue() > minimum.longValue() || (x.intValue() == minimum.longValue() && (exclusiveMinimum == null || ! exclusiveMinimum));
		if (x instanceof Float)
			return minimum == null || x.floatValue() > minimum.floatValue() || (x.floatValue() == minimum.floatValue() && (exclusiveMinimum == null || ! exclusiveMinimum));
		if (x instanceof Double)
			return minimum == null || x.doubleValue() > minimum.doubleValue() || (x.doubleValue() == minimum.doubleValue() && (exclusiveMinimum == null || ! exclusiveMinimum));
		return true;
	}
	
	private boolean isValidMaximum(Number x) {
		if (x instanceof Integer) 
			return maximum == null || x.intValue() < maximum.intValue() || (x.intValue() == maximum.intValue() && (exclusiveMaximum == null || ! exclusiveMaximum));
		if (x instanceof Short)
			return maximum == null || x.shortValue() < maximum.shortValue() || (x.intValue() == maximum.shortValue() && (exclusiveMaximum == null || ! exclusiveMaximum));
		if (x instanceof Long)
			return maximum == null || x.longValue() < maximum.longValue() || (x.intValue() == maximum.longValue() && (exclusiveMaximum == null || ! exclusiveMaximum));
		if (x instanceof Float)
			return maximum == null || x.floatValue() < maximum.floatValue() || (x.floatValue() == maximum.floatValue() && (exclusiveMaximum == null || ! exclusiveMaximum));
		if (x instanceof Double)
			return maximum == null || x.doubleValue() < maximum.doubleValue() || (x.doubleValue() == maximum.doubleValue() && (exclusiveMaximum == null || ! exclusiveMaximum));
		return true;
	}
	
	private boolean isValidMultipleOf(Number x) {
		if (x instanceof Integer) 
			return multipleOf == null || x.intValue() % multipleOf.intValue() == 0;
		if (x instanceof Short)
			return multipleOf == null || x.shortValue() % multipleOf.shortValue() == 0;
		if (x instanceof Long)
			return multipleOf == null || x.longValue() % multipleOf.longValue() == 0;
		if (x instanceof Float)
			return multipleOf == null || x.floatValue() % multipleOf.floatValue() == 0;
		if (x instanceof Double)
			return multipleOf == null || x.doubleValue() % multipleOf.doubleValue() == 0;
		return true;
	}

	private boolean isValidAllowEmpty(String x) {
		return allowEmptyValue == null || allowEmptyValue || isNotEmpty(x);
	}
	
	private boolean isValidPattern(String x) {
		return pattern == null || pattern.matcher(x).matches();
	}
	
	private boolean isValidEnum(String x) {
		return _enum == null || _enum.contains(x);
	}
	
	private boolean isValidMinLength(String x) {
		return minLength == null || x.length() >= minLength;
	}

	private boolean isValidMaxLength(String x) {
		return maxLength == null || x.length() <= maxLength;
	}

	private boolean isValidMinItems(Object x) {
		return minItems == null || Array.getLength(x) >= minItems;
	}

	private boolean isValidMaxItems(Object x) {
		return maxItems == null || Array.getLength(x) <= maxItems;
	}

	private boolean isValidUniqueItems(Object x) {
		if (uniqueItems != null && uniqueItems) {
			Set<Object> s = new HashSet<>();
			for (int i = 0; i < Array.getLength(x); i++) {
				Object o = Array.get(x, i);
				if (! s.add(o))
					return false;
			}
		}
		return true;
	}

	private boolean isValidMinItems(Collection<?> x) {
		return minItems == null || x.size() >= minItems;
	}

	private boolean isValidMaxItems(Collection<?> x) {
		return maxItems == null || x.size() <= maxItems;
	}

	private boolean isValidUniqueItems(Collection<?> x) {
		if (uniqueItems != null && uniqueItems && ! (x instanceof Set)) {
			Set<Object> s = new HashSet<>();
			for (Object o : x) 
				if (! s.add(o))
					return false;
		}
		return true;
	}
	
	private HttpPartSchema getProperty(String name) {
		if (properties != null) {
			HttpPartSchema schema = properties.get(name);
			if (schema != null)
				return schema;
		}
		return additionalProperties;
	}

	
	private static Set<String> copy(Set<String> in) {
		return in == null ? null : unmodifiableSet(new LinkedHashSet<>(in));
	}
	
	private static Map<String,HttpPartSchema> build(Map<String,Builder> in) {
		if (in == null)
			return null;
		Map<String,HttpPartSchema> m = new LinkedHashMap<>();
		for (Map.Entry<String,Builder> e : in.entrySet()) 
			m.put(e.getKey(), e.getValue().build());
		return unmodifiableMap(m);
	}

	private static HttpPartSchema build(Builder in) {
		return in == null ? null : in.build();
	}
}
