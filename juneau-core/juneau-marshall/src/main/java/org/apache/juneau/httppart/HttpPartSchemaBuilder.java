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

import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.HttpPartSchema.*;
import org.apache.juneau.httppart.HttpPartSchema.Type;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * The builder class for creating {@link HttpPartSchema} objects.
 *
 */
public class HttpPartSchemaBuilder {
	String name, _default;
	Set<Integer> codes;
	Set<String> _enum;
	Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
	CollectionFormat collectionFormat = CollectionFormat.NO_COLLECTION_FORMAT;
	Type type = Type.NO_TYPE;
	Format format = Format.NO_FORMAT;
	Pattern pattern;
	Number maximum, minimum, multipleOf;
	Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
	Map<String,HttpPartSchemaBuilder> properties;
	HttpPartSchemaBuilder items, additionalProperties;
	boolean noValidate;
	ObjectMap api = new ObjectMap();
	Class<? extends HttpPartParser> parser;
	Class<? extends HttpPartSerializer> serializer;

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

	HttpPartSchemaBuilder apply(Class<? extends Annotation> c, Method m, int index) {
		for (Annotation a :  m.getParameterAnnotations()[index])
			if (c.isInstance(a))
				return apply(a);
		apply(c, m.getGenericParameterTypes()[index]);
		return this;
	}

	HttpPartSchemaBuilder apply(Class<? extends Annotation> c, java.lang.reflect.Type t) {
		if (t instanceof Class<?>)
			for (Annotation a : ReflectionUtils.findAnnotationsParentFirst(c, (Class<?>)t))
				apply(a);
		return this;
	}

	HttpPartSchemaBuilder apply(Annotation a) {
		if (a instanceof Body)
			apply((Body)a);
		else if (a instanceof Header)
			apply((Header)a);
		else if (a instanceof FormData)
			apply((FormData)a);
		else if (a instanceof Query)
			apply((Query)a);
		else if (a instanceof Path)
			apply((Path)a);
		else if (a instanceof Response)
			apply((Response)a);
		else if (a instanceof ResponseHeader)
			apply((ResponseHeader)a);
		else if (a instanceof ResponseStatus)
			apply((ResponseStatus)a);
		else if (a instanceof HasQuery)
			apply((HasQuery)a);
		else if (a instanceof HasFormData)
			apply((HasFormData)a);
		return this;
	}

	HttpPartSchemaBuilder apply(Body a) {
		api = AnnotationUtils.merge(api, a);
		required(HttpPartSchema.toBoolean(a.required()));
		allowEmptyValue(HttpPartSchema.toBoolean(! a.required()));
		parser(a.parser());
		apply(a.schema());
		return this;
	}

	HttpPartSchemaBuilder apply(Header a) {
		api = AnnotationUtils.merge(api, a);
		name(a.value());
		name(a.name());
		required(HttpPartSchema.toBoolean(a.required()));
		type(a.type());
		format(a.format());
		allowEmptyValue(HttpPartSchema.toBoolean(a.allowEmptyValue()));
		items(a.items());
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		skipIfEmpty(HttpPartSchema.toBoolean(a.skipIfEmpty()));
		parser(a.parser());
		serializer(a.serializer());
		return this;
	}

	HttpPartSchemaBuilder apply(ResponseHeader a) {
		api = AnnotationUtils.merge(api, a);
		name(a.value());
		name(a.name());
		codes(a.code());
		type(a.type());
		format(a.format());
		items(a.items());
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		allowEmptyValue(false);
		serializer(a.serializer());
		return this;
	}

	HttpPartSchemaBuilder apply(ResponseStatus a) {
		api = AnnotationUtils.merge(api, a);
		code(a.value());
		code(a.code());
		return this;
	}

	HttpPartSchemaBuilder apply(FormData a) {
		api = AnnotationUtils.merge(api, a);
		name(a.value());
		name(a.name());
		required(HttpPartSchema.toBoolean(a.required()));
		type(a.type());
		format(a.format());
		allowEmptyValue(HttpPartSchema.toBoolean(a.allowEmptyValue()));
		items(a.items());
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		skipIfEmpty(HttpPartSchema.toBoolean(a.skipIfEmpty()));
		parser(a.parser());
		serializer(a.serializer());
		return this;
	}

	HttpPartSchemaBuilder apply(Query a) {
		api = AnnotationUtils.merge(api, a);
		name(a.value());
		name(a.name());
		required(HttpPartSchema.toBoolean(a.required()));
		type(a.type());
		format(a.format());
		allowEmptyValue(HttpPartSchema.toBoolean(a.allowEmptyValue()));
		items(a.items());
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		skipIfEmpty(HttpPartSchema.toBoolean(a.skipIfEmpty()));
		parser(a.parser());
		serializer(a.serializer());
		return this;
	}

	HttpPartSchemaBuilder apply(Path a) {
		api = AnnotationUtils.merge(api, a);
		name(a.value());
		name(a.name());
		type(a.type());
		format(a.format());
		items(a.items());
		collectionFormat(a.collectionFormat());
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		parser(a.parser());
		serializer(a.serializer());
		return this;
	}

	HttpPartSchemaBuilder apply(Response a) {
		api = AnnotationUtils.merge(api, a);
		codes(a.value());
		codes(a.code());
		required(false);
		allowEmptyValue(true);
		serializer(a.serializer());
		apply(a.schema());
		return this;
	}

	HttpPartSchemaBuilder apply(Items a) {
		api = AnnotationUtils.merge(api, a);
		type(a.type());
		format(a.format());
		items(a.items());
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		return this;
	}

	HttpPartSchemaBuilder apply(SubItems a) {
		api = AnnotationUtils.merge(api, a);
		type(a.type());
		format(a.format());
		items(HttpPartSchema.toObjectMap(a.items()));
		collectionFormat(a.collectionFormat());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		return this;
	}

	HttpPartSchemaBuilder apply(Schema a) {
		type(a.type());
		format(a.format());
		items(a.items());
		_default(joinnl(a._default()));
		maximum(HttpPartSchema.toNumber(a.maximum()));
		exclusiveMaximum(HttpPartSchema.toBoolean(a.exclusiveMaximum()));
		minimum(HttpPartSchema.toNumber(a.minimum()));
		exclusiveMinimum(HttpPartSchema.toBoolean(a.exclusiveMinimum()));
		maxLength(HttpPartSchema.toLong(a.maxLength()));
		minLength(HttpPartSchema.toLong(a.minLength()));
		pattern(a.pattern());
		maxItems(HttpPartSchema.toLong(a.maxItems()));
		minItems(HttpPartSchema.toLong(a.minItems()));
		uniqueItems(HttpPartSchema.toBoolean(a.uniqueItems()));
		_enum(HttpPartSchema.toSet(a._enum()));
		multipleOf(HttpPartSchema.toNumber(a.multipleOf()));
		maxProperties(HttpPartSchema.toLong(a.maxProperties()));
		minProperties(HttpPartSchema.toLong(a.minProperties()));
		properties(HttpPartSchema.toObjectMap(a.properties()));
		additionalProperties(HttpPartSchema.toObjectMap(a.additionalProperties()));
		return this;
	}

	HttpPartSchemaBuilder apply(HasQuery a) {
		name(a.value());
		name(a.name());
		return this;
	}

	HttpPartSchemaBuilder apply(HasFormData a) {
		name(a.value());
		name(a.name());
		return this;
	}

	HttpPartSchemaBuilder apply(ObjectMap m) {
		if (m != null && ! m.isEmpty()) {
			_default(m.getString("default"));
			_enum(HttpPartSchema.toSet(m.getString("enum")));
			allowEmptyValue(m.getBoolean("allowEmptyValue"));
			exclusiveMaximum(m.getBoolean("exclusiveMaximum"));
			exclusiveMinimum(m.getBoolean("exclusiveMinimum"));
			required(m.getBoolean("required"));
			uniqueItems(m.getBoolean("uniqueItems"));
			collectionFormat(m.getString("collectionFormat"));
			type(m.getString("type"));
			format(m.getString("format"));
			pattern(m.getString("pattern"));
			maximum(m.get("maximum", Number.class));
			minimum(m.get("minimum", Number.class));
			multipleOf(m.get("multipleOf", Number.class));
			maxItems(m.get("maxItems", Long.class));
			maxLength(m.get("maxLength", Long.class));
			maxProperties(m.get("maxProperties", Long.class));
			minItems(m.get("minItems", Long.class));
			minLength(m.get("minLength", Long.class));
			minProperties(m.get("minProperties", Long.class));

			items(m.getObjectMap("items"));
			properties(m.getObjectMap("properties"));
			additionalProperties(m.getObjectMap("additionalProperties"));

			apply(m.getObjectMap("schema", null));
		}
		return this;
	}

	/**
	 * <mk>name</mk> field.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder name(String value) {
		if (isNotEmpty(value))
			name = value;
		return this;
	}

	/**
	 * <mk>httpStatusCode</mk> key.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#responsesObject">Responses</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if <jk>null</jk> or an empty array.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder codes(int[] value) {
		if (value != null && value.length != 0)
			for (int v : value)
				code(v);
		return this;
	}

	/**
	 * <mk>httpStatusCode</mk> key.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#responsesObject">Responses</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <code>0</code>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder code(int value) {
		if (value != 0) {
			if (codes == null)
				codes = new TreeSet<>();
			codes.add(value);
		}
		return this;
	}

	/**
	 * <mk>required</mk> field.
	 *
	 * <p>
	 * Determines whether the parameter is mandatory.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder required(Boolean value) {
		if (value != null)
			required = value;
		return this;
	}

	/**
	 * <mk>required</mk> field.
	 *
	 * <p>
	 * Shortcut for calling <code>required(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder required() {
		return required(true);
	}

	/**
	 * <mk>type</mk> field.
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
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#securitySchemeObject">Security Scheme</a>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder type(String value) {
		try {
			if (isNotEmpty(value))
				type = Type.fromString(value);
		} catch (Exception e) {
			throw new ContextRuntimeException("Invalid value ''{0}'' passed in as type value.  Valid values: {1}", value, Type.values());
		}
		return this;
	}

	/**
	 * <mk>format</mk> field.
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
	 * 		<js>"binary-spaced"</js> - Hexadecimal encoded octets, spaced (e.g. <js>"00 FF"</js>).
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
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/v2/#dataTypeFormat'>Swagger specification &gt; Data Type Formats</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder format(String value) {
		try {
			if (isNotEmpty(value))
				format = Format.fromString(value);
		} catch (Exception e) {
			throw new ContextRuntimeException("Invalid value ''{0}'' passed in as format value.  Valid values: {1}", value, Format.values());
		}
		return this;
	}

	/**
	 * <mk>allowEmptyValue</mk> field.
	 *
	 * <p>
	 * Sets the ability to pass empty-valued parameters.
	 * <br>This is valid only for either query or formData parameters and allows you to send a parameter with a name only or an empty value.
	 * <br>The default value is <jk>false</jk>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder allowEmptyValue(Boolean value) {
		if (value != null)
			allowEmptyValue = value;
		return this;
	}

	/**
	 * <mk>allowEmptyValue</mk> field.
	 *
	 * <p>
	 * Shortcut for calling <code>allowEmptyValue(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder allowEmptyValue() {
		return allowEmptyValue(true);
	}

	/**
	 * <mk>items</mk> field.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>.
	 * <br>Can only be used if <code>type</code> is <js>"array"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder items(HttpPartSchemaBuilder value) {
		if (value != null)
			this.items = value;
		return this;
	}

	HttpPartSchemaBuilder items(ObjectMap value) {
		if (value != null && ! value.isEmpty()) {
			items = HttpPartSchema.create().apply(value);
			api.put("items", value);
		}
		return this;
	}

	HttpPartSchemaBuilder items(Items value) {
		if (! AnnotationUtils.empty(value)) {
			items = HttpPartSchema.create().apply(value);
			api.put("items", items.api);
		}
		return this;
	}

	HttpPartSchemaBuilder items(SubItems value) {
		if (! AnnotationUtils.empty(value)) {
			items = HttpPartSchema.create().apply(value);
			api.put("items", items.api);
		}
		return this;
	}


	/**
	 * <mk>collectionFormat</mk> field.
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
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder collectionFormat(String value) {
		try {
			if (isNotEmpty(value))
				this.collectionFormat = CollectionFormat.fromString(value);
		} catch (Exception e) {
			throw new ContextRuntimeException("Invalid value ''{0}'' passed in as collectionFormat value.  Valid values: {1}", value, CollectionFormat.values());
		}
		return this;
	}

	/**
	 * <mk>default</mk> field.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a "count" to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * <br>(Note: "default" has no meaning for required parameters.)
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder _default(String value) {
		if (isNotEmpty(value))
			this._default = value;
		return this;
	}

	/**
	 * <mk>maximum</mk> field.
	 *
	 * <p>
	 * Defines the maximum value for a parameter of numeric types.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder maximum(Number value) {
		if (value != null)
			this.maximum = value;
		return this;
	}

	/**
	 * <mk>exclusiveMaximum</mk> field.
	 *
	 * <p>
	 * Defines whether the maximum is matched exclusively.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <code>maximum</code>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder exclusiveMaximum(Boolean value) {
		if (value != null)
			this.exclusiveMaximum = value;
		return this;
	}

	/**
	 * <mk>exclusiveMaximum</mk> field.
	 *
	 * <p>
	 * Shortcut for calling <code>exclusiveMaximum(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder exclusiveMaximum() {
		return exclusiveMaximum(true);
	}

	/**
	 * <mk>minimum</mk> field.
	 *
	 * <p>
	 * Defines the minimum value for a parameter of numeric types.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder minimum(Number value) {
		if (value != null)
			this.minimum = value;
		return this;
	}

	/**
	 * <mk>exclusiveMinimum</mk> field.
	 *
	 * <p>
	 * Defines whether the minimum is matched exclusively.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <code>minimum</code>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder exclusiveMinimum(Boolean value) {
		if (value != null)
			this.exclusiveMinimum = value;
		return this;
	}

	/**
	 * <mk>exclusiveMinimum</mk> field.
	 *
	 * <p>
	 * Shortcut for calling <code>exclusiveMinimum(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder exclusiveMinimum() {
		return exclusiveMinimum(true);
	}

	/**
	 * <mk>maxLength</mk> field.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder maxLength(Long value) {
		if (value != null)
			this.maxLength = value;
		return this;
	}

	/**
	 * <mk>minLength</mk> field.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder minLength(Long value) {
		if (value != null)
			this.minLength = value;
		return this;
	}

	/**
	 * <mk>pattern</mk> field.
	 *
	 * <p>
	 * A string input is valid if it matches the specified regular expression pattern.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder pattern(String value) {
		try {
			if (isNotEmpty(value))
				this.pattern = Pattern.compile(value);
		} catch (Exception e) {
			throw new ContextRuntimeException(e, "Invalid value {0} passed in as pattern value.  Must be a valid regular expression.", value);
		}
		return this;
	}

	/**
	 * <mk>maxItems</mk> field.
	 *
	 * <p>
	 * An array or collection is valid if its size is less than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder maxItems(Long value) {
		if (value != null)
			this.maxItems = value;
		return this;
	}

	/**
	 * <mk>minItems</mk> field.
	 *
	 * <p>
	 * An array or collection is valid if its size is greater than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder minItems(Long value) {
		if (value != null)
			this.minItems = value;
		return this;
	}

	/**
	 * <mk>uniqueItems</mk> field.
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
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder uniqueItems(Boolean value) {
		if (value != null)
			this.uniqueItems = value;
		return this;
	}

	/**
	 * <mk>uniqueItems</mk> field.
	 *
	 * <p>
	 * Shortcut for calling <code>uniqueItems(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder uniqueItems() {
		return uniqueItems(true);
	}

	/**
	 * Identifies whether an item should be skipped if it's empty.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder skipIfEmpty(Boolean value) {
		if (value != null)
			this.skipIfEmpty = value;
		return this;
	}

	/**
	 * Identifies whether an item should be skipped if it's empty.
	 *
	 * <p>
	 * Shortcut for calling <code>skipIfEmpty(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder skipIfEmpty() {
		return skipIfEmpty(true);
	}

	/**
	 * <mk>enum</mk> field.
	 *
	 * <p>
	 * If specified, the input validates successfully if it is equal to one of the elements in this array.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or an empty set.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder _enum(Set<String> value) {
		if (value != null && ! value.isEmpty())
			this._enum = value;
		return this;
	}

	/**
	 * <mk>_enum</mk> field.
	 *
	 * <p>
	 * Same as {@link #_enum(Set)} but takes in a var-args array.
	 *
	 * @param values
	 * 	The new values for this property.
	 * 	<br>Ignored if value is empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder _enum(String...values) {
		return _enum(new ASet<String>().appendAll(values));
	}

	/**
	 * <mk>multipleOf</mk> field.
	 *
	 * <p>
	 * A numeric instance is valid if the result of the division of the instance by this keyword's value is an integer.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#itemsObject">Items</a>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Header</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder multipleOf(Number value) {
		if (value != null)
			this.multipleOf = value;
		return this;
	}

	/**
	 * <mk>mapProperties</mk> field.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder maxProperties(Long value) {
		if (value != null && value != -1)
			this.maxProperties = value;
		return this;
	}

	/**
	 * <mk>minProperties</mk> field.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder minProperties(Long value) {
		if (value != null && value != -1)
			this.minProperties = value;
		return this;
	}

	/**
	 * <mk>properties</mk> field.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * </ul>
	 *
	 * @param key
	 *	The property name.
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder property(String key, HttpPartSchemaBuilder value) {
		if ( key != null && value != null) {
			if (properties == null)
				properties = new LinkedHashMap<>();
			properties.put(key, value);
		}
		return this;
	}

	private HttpPartSchemaBuilder properties(ObjectMap value) {
		if (value != null && ! value.isEmpty())
		for (Map.Entry<String,Object> e : value.entrySet())
			property(e.getKey(), HttpPartSchema.create().apply((ObjectMap)e.getValue()));
		return this;
	}

	/**
	 * <mk>additionalProperties</mk> field.
	 *
	 * <p>
	 * Applicable to the following Swagger schema objects:
	 * <ul>
	 * 	<li><a class="doclink" href="https://swagger.io/specification/v2/#schemaObject">Schema</a>
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or empty.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder additionalProperties(HttpPartSchemaBuilder value) {
		if (value != null)
			additionalProperties = value;
		return this;
	}

	private HttpPartSchemaBuilder additionalProperties(ObjectMap value) {
		if (value != null && ! value.isEmpty())
			additionalProperties = HttpPartSchema.create().apply(value);
		return this;
	}

	/**
	 * Identifies the part serializer to use for serializing this part.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or {@link HttpPartSerializer.Null}.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder serializer(Class<? extends HttpPartSerializer> value) {
		if (serializer != null && serializer != HttpPartSerializer.Null.class)
			serializer = value;
		return this;
	}

	/**
	 * Identifies the part parser to use for parsing this part.
	 *
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Ignored if value is <jk>null</jk> or {@link HttpPartParser.Null}.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder parser(Class<? extends HttpPartParser> value) {
		if (parser != null && parser != HttpPartParser.Null.class)
			parser = value;
		return this;
	}

	/**
	 * Disables Swagger schema usage validation checking.
	 *
	 * @param value Specify <jk>true</jk> to prevent {@link ContextRuntimeException} from being thrown if invalid Swagger usage was detected.
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder noValidate(Boolean value) {
		if (value != null)
			this.noValidate = value;
		return this;
	}

	/**
	 * Disables Swagger schema usage validation checking.
	 *
	 * <p>
	 * Shortcut for calling <code>noValidate(<jk>true</jk>);</code>.
	 *
	 * @return This object (for method chaining).
	 */
	public HttpPartSchemaBuilder noValidate() {
		return noValidate(true);
	}
}