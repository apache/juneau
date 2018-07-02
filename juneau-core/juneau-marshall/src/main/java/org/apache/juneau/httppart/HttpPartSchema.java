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

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
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

	/** Reusable instance of {@link OpenApiPartSerializer}, all default settings. */
	public static final HttpPartSchema DEFAULT = HttpPartSchema.create().allowEmptyValue(true).build();

	final String name;
	final Set<Integer> codes;
	final String _default;
	final Set<String> _enum;
	final Map<String,HttpPartSchema> properties;
	final Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
	final CollectionFormat collectionFormat;
	final Type type;
	final Format format;
	final Pattern pattern;
	final HttpPartSchema items, additionalProperties;
	final Number maximum, minimum, multipleOf;
	final Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
	final Class<? extends HttpPartParser> parser;
	final Class<? extends HttpPartSerializer> serializer;

	final ObjectMap api;

	/**
	 * Instantiates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Finds the schema information for the specified method parameter.
	 *
	 * <p>
	 * This method will gather all the schema information from the annotations at the following locations:
	 * <ul>
	 * 	<li>The method parameter.
	 * 	<li>The method parameter class.
	 * 	<li>The method parameter parent classes and interfaces.
	 * </ul>
	 *
	 * @param c
	 * 	The annotation to look for.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li>{@link Body}
	 * 		<li>{@link Header}
	 * 		<li>{@link Query}
	 * 		<li>{@link FormData}
	 * 		<li>{@link Path}
	 * 		<li>{@link Response}
	 * 		<li>{@link ResponseHeader}
	 * 		<li>{@link ResponseStatus}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param m
	 * 	The Java method containing the parameter.
	 * @param mi
	 * 	The index of the parameter on the method.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, Method m, int mi) {
		return create().apply(c, m, mi).build();
	}

	/**
	 * Finds the schema information for the specified class.
	 *
	 * <p>
	 * This method will gather all the schema information from the annotations on the class and all parent classes/interfaces.
	 *
	 * @param c
	 * 	The annotation to look for.
	 * 	<br>Valid values:
	 * 	<ul>
	 * 		<li>{@link Body}
	 * 		<li>{@link Header}
	 * 		<li>{@link Query}
	 * 		<li>{@link FormData}
	 * 		<li>{@link Path}
	 * 		<li>{@link Response}
	 * 		<li>{@link ResponseHeader}
	 * 		<li>{@link ResponseStatus}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param t
	 * 	The class containing the parameter.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, java.lang.reflect.Type t) {
		return create().apply(c, t).build();
	}

	/**
	 * Utility method for creating response maps from a schema.
	 *
	 * <p>
	 * Given the valid response codes for this particular schema (from the {@link #getCodes()} method, this will
	 * return a map with response-code keys and values that are the api of the passed-in schema.
	 * <br>
	 * The purpose of this method is to easily construct response sections in generated Swagger JSON documents.
	 *
	 * <p>
	 * Only valid for the following types of
	 * <ul>
	 * 		<li>{@link Response}
	 * 		<li>{@link ResponseHeader}
	 * 		<li>{@link ResponseStatus}
	 * </ul>
	 * For
	 *
	 * @param s
	 * 	The schema to create a map from.
	 * 	<br>Only valid for the following types of schemas:
	 * 	<ul>
	 * 		<li>{@link Response}
	 * 		<li>{@link ResponseHeader}
	 * 		<li>{@link ResponseStatus}
	 * 	</ul>
	 * @param def
	 * 	The default response code if no codes were specified in the schema.
	 * @return The schema response map.
	 */
	public static ObjectMap getApiCodeMap(HttpPartSchema s, Integer def) {
		ObjectMap om = new ObjectMap();
		for (Integer c : s.getCodes(def))
			om.getObjectMap(String.valueOf(c), true).appendAll(s.getApi());
		return om;
	}

	/**
	 * Utility method for creating response maps from multiple schemas.
	 *
	 * <p>
	 * Same as {@link #getApiCodeMap(HttpPartSchema, Integer)} except combines the maps from multiple schemas.
	 *
	 * @param ss
	 * 	The schemas to create a map from.
	 * 	<br>Only valid for the following types of schemas:
	 * 	<ul>
	 * 		<li>{@link Response}
	 * 		<li>{@link ResponseHeader}
	 * 		<li>{@link ResponseStatus}
	 * 	</ul>
	 * @param def
	 * 	The default response code if no codes were specified in the schema.
	 * @return The schema response map.
	 */
	public static ObjectMap getApiCodeMap(HttpPartSchema[] ss, Integer def) {
		ObjectMap om = new ObjectMap();
		for (HttpPartSchema s : ss)
			for (Integer c : s.getCodes(def))
				om.getObjectMap(String.valueOf(c), true).appendAll(s.getApi());
		return om;
	}


	/**
	 * Finds the schema information on the specified annotation.
	 *
	 * @param a
	 * 	The annotation to find the schema information on..
	 * @return The schema information found on the annotation.
	 */
	public static HttpPartSchema create(Annotation a) {
		return create().apply(a).build();
	}

	HttpPartSchema(Builder b) {
		this.name = b.name;
		this.codes = copy(b.codes);
		this._default = b._default;
		this._enum = copy(b._enum);
		this.properties = build(b.properties, b.noValidate);
		this.allowEmptyValue = b.allowEmptyValue;
		this.exclusiveMaximum = b.exclusiveMaximum;
		this.exclusiveMinimum = b.exclusiveMinimum;
		this.required = b.required;
		this.uniqueItems = b.uniqueItems;
		this.skipIfEmpty = b.skipIfEmpty;
		this.collectionFormat = b.collectionFormat;
		this.type = b.type;
		this.format = b.format;
		this.pattern = b.pattern;
		this.items = build(b.items, b.noValidate);
		this.additionalProperties = build(b.additionalProperties, b.noValidate);
		this.maximum = b.maximum;
		this.minimum = b.minimum;
		this.multipleOf = b.multipleOf;
		this.maxItems = b.maxItems;
		this.maxLength = b.maxLength;
		this.maxProperties = b.maxProperties;
		this.minItems = b.minItems;
		this.minLength = b.minLength;
		this.minProperties = b.minProperties;
		this.api = b.api.unmodifiable();
		this.parser = b.parser;
		this.serializer = b.serializer;

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
				invalidFormat = ! format.isOneOf(Format.BYTE, Format.BINARY, Format.BINARY_SPACED, Format.DATE, Format.DATE_TIME, Format.PASSWORD, Format.UON, Format.NONE);
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
		if (minLength != null && maxLength != null && maxLength < minLength)
			errors.add("maxLength cannot be less than minLength.");
		if (minimum != null && maximum != null && maximum.doubleValue() < minimum.doubleValue())
			errors.add("maximum cannot be less than minimum.");
		if (minItems != null && maxItems != null && maxItems < minItems)
			errors.add("maxItems cannot be less than minItems.");
		if (minProperties != null && maxProperties != null && maxProperties < minProperties)
			errors.add("maxProperties cannot be less than minProperties.");
		if (minLength != null && minLength < 0)
			errors.add("minLength cannot be less than zero.");
		if (maxLength != null && maxLength < 0)
			errors.add("maxLength cannot be less than zero.");
		if (minItems != null && minItems < 0)
			errors.add("minItems cannot be less than zero.");
		if (maxItems != null && maxItems < 0)
			errors.add("maxItems cannot be less than zero.");
		if (minProperties != null && minProperties < 0)
			errors.add("minProperties cannot be less than zero.");
		if (maxProperties != null && maxProperties < 0)
			errors.add("maxProperties cannot be less than zero.");

		if (! errors.isEmpty())
			throw new ContextRuntimeException("Schema specification errors: \n\t" + join(errors, "\n\t"));
	}

	/**
	 * The builder class for creating {@link HttpPartSchema} objects.
	 *
	 */
	public static class Builder {
		String name, _default;
		Set<Integer> codes;
		Set<String> _enum;
		Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
		CollectionFormat collectionFormat = CollectionFormat.NONE;
		Type type = Type.NONE;
		Format format = Format.NONE;
		Pattern pattern;
		Number maximum, minimum, multipleOf;
		Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
		Map<String,Builder> properties;
		HttpPartSchema.Builder items, additionalProperties;
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

		Builder apply(Class<? extends Annotation> c, Method m, int index) {
			for (Annotation a :  m.getParameterAnnotations()[index])
				if (c.isInstance(a))
					return apply(a);
			apply(c, m.getGenericParameterTypes()[index]);
			return this;
		}

		Builder apply(Class<? extends Annotation> c, java.lang.reflect.Type t) {
			if (t instanceof Class<?>)
				for (Annotation a : ReflectionUtils.findAnnotationsParentFirst(c, (Class<?>)t))
					apply(a);
			return this;
		}

		Builder apply(Annotation a) {
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

		Builder apply(Body a) {
			api = AnnotationUtils.merge(api, a);
			required(toBoolean(a.required()));
			allowEmptyValue(toBoolean(! a.required()));
			parser(a.parser());
			apply(a.schema());
			return this;
		}

		Builder apply(Header a) {
			api = AnnotationUtils.merge(api, a);
			name(a.value());
			name(a.name());
			required(toBoolean(a.required()));
			type(a.type());
			format(a.format());
			allowEmptyValue(toBoolean(a.allowEmptyValue()));
			items(a.items());
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			skipIfEmpty(toBoolean(a.skipIfEmpty()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(ResponseHeader a) {
			api = AnnotationUtils.merge(api, a);
			name(a.value());
			name(a.name());
			codes(a.code());
			type(a.type());
			format(a.format());
			items(a.items());
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			allowEmptyValue(false);
			serializer(a.serializer());
			return this;
		}

		Builder apply(ResponseStatus a) {
			api = AnnotationUtils.merge(api, a);
			code(a.value());
			code(a.code());
			return this;
		}

		Builder apply(FormData a) {
			api = AnnotationUtils.merge(api, a);
			name(a.value());
			name(a.name());
			required(toBoolean(a.required()));
			type(a.type());
			format(a.format());
			allowEmptyValue(toBoolean(a.allowEmptyValue()));
			items(a.items());
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			skipIfEmpty(toBoolean(a.skipIfEmpty()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(Query a) {
			api = AnnotationUtils.merge(api, a);
			name(a.value());
			name(a.name());
			required(toBoolean(a.required()));
			type(a.type());
			format(a.format());
			allowEmptyValue(toBoolean(a.allowEmptyValue()));
			items(a.items());
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			skipIfEmpty(toBoolean(a.skipIfEmpty()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(Path a) {
			api = AnnotationUtils.merge(api, a);
			name(a.value());
			name(a.name());
			type(a.type());
			format(a.format());
			items(a.items());
			collectionFormat(a.collectionFormat());
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(Response a) {
			api = AnnotationUtils.merge(api, a);
			codes(a.value());
			codes(a.code());
			required(false);
			allowEmptyValue(true);
			serializer(a.serializer());
			apply(a.schema());
			return this;
		}

		Builder apply(Items a) {
			api = AnnotationUtils.merge(api, a);
			type(a.type());
			format(a.format());
			items(a.items());
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			return this;
		}

		Builder apply(SubItems a) {
			api = AnnotationUtils.merge(api, a);
			type(a.type());
			format(a.format());
			items(toObjectMap(a.items()));
			collectionFormat(a.collectionFormat());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			return this;
		}

		Builder apply(Schema a) {
			type(a.type());
			format(a.format());
			items(a.items());
			_default(joinnl(a._default()));
			maximum(toNumber(a.maximum()));
			exclusiveMaximum(toBoolean(a.exclusiveMaximum()));
			minimum(toNumber(a.minimum()));
			exclusiveMinimum(toBoolean(a.exclusiveMinimum()));
			maxLength(toLong(a.maxLength()));
			minLength(toLong(a.minLength()));
			pattern(a.pattern());
			maxItems(toLong(a.maxItems()));
			minItems(toLong(a.minItems()));
			uniqueItems(toBoolean(a.uniqueItems()));
			_enum(toSet(a._enum()));
			multipleOf(toNumber(a.multipleOf()));
			maxProperties(toLong(a.maxProperties()));
			minProperties(toLong(a.minProperties()));
			properties(toObjectMap(a.properties()));
			additionalProperties(toObjectMap(a.additionalProperties()));
			return this;
		}

		Builder apply(HasQuery a) {
			name(a.value());
			name(a.name());
			return this;
		}

		Builder apply(HasFormData a) {
			name(a.value());
			name(a.name());
			return this;
		}

		Builder apply(ObjectMap m) {
			if (m != null && ! m.isEmpty()) {
				_default(m.getString("default"));
				_enum(toSet(m.getString("enum")));
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
		public Builder name(String value) {
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
		public Builder codes(int[] value) {
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
		public Builder code(int value) {
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
		public Builder required(Boolean value) {
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
		public Builder required() {
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
		public Builder type(String value) {
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
		public Builder format(String value) {
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
		public Builder allowEmptyValue(Boolean value) {
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
		public Builder allowEmptyValue() {
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
		public Builder items(ObjectMap value) {
			if (value != null && ! value.isEmpty()) {
				items = HttpPartSchema.create().apply(value);
				api.put("items", value);
			}
			return this;
		}

		Builder items(Items value) {
			if (! AnnotationUtils.empty(value)) {
				items = HttpPartSchema.create().apply(value);
				api.put("items", items.api);
			}
			return this;
		}

		Builder items(SubItems value) {
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
		public Builder _default(String value) {
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
		public Builder maximum(Number value) {
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
		public Builder exclusiveMaximum(Boolean value) {
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
		public Builder exclusiveMaximum() {
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
		public Builder minimum(Number value) {
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
		public Builder exclusiveMinimum(Boolean value) {
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
		public Builder exclusiveMinimum() {
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
		public Builder maxLength(Long value) {
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
		public Builder minLength(Long value) {
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
		public Builder maxItems(Long value) {
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
		public Builder minItems(Long value) {
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
		public Builder uniqueItems(Boolean value) {
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
		public Builder uniqueItems() {
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
		public Builder skipIfEmpty(Boolean value) {
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
		public Builder skipIfEmpty() {
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
		public Builder _enum(Set<String> value) {
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
		public Builder _enum(String...values) {
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
		public Builder multipleOf(Number value) {
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
		public Builder maxProperties(Long value) {
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
		public Builder minProperties(Long value) {
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
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder properties(ObjectMap value) {
			if (value != null && ! value.isEmpty())
				for (Map.Entry<String,Object> e : value.entrySet())
					properties.put(e.getKey(), HttpPartSchema.create().apply((ObjectMap)e.getValue()));
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
		public Builder additionalProperties(ObjectMap value) {
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
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
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
		public Builder parser(Class<? extends HttpPartParser> value) {
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
		public Builder noValidate(Boolean value) {
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
		public Builder noValidate() {
			return noValidate(true);
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
		 * Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
		 */
		BINARY_SPACED,

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
	 * Returns the name of the object described by this schema, for example the query or form parameter name.
	 *
	 * @return The name, or <jk>null</jk> if not specified.
	 * @see Builder#name(String)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the HTTP status code or codes defined on a schema.
	 *
	 * @return
	 * 	The list of HTTP status codes.
	 * 	<br>Never <jk>null</jk>.
	 * @see Builder#code(int)
	 * @see Builder#codes(int[])
	 */
	public Set<Integer> getCodes() {
		return codes;
	}

	/**
	 * Returns the HTTP status code or codes defined on a schema.
	 *
	 * @param def The default value if there are no codes defined.
	 * @return
	 * 	The list of HTTP status codes.
	 * 	<br>A singleton set containing the default value if the set is empty.
	 * 	<br>Never <jk>null</jk>.
	 * @see Builder#code(int)
	 * @see Builder#codes(int[])
	 */
	public Set<Integer> getCodes(Integer def) {
		return codes.isEmpty() ? Collections.singleton(def) : codes;
	}

	/**
	 * Returns the first HTTP status code on a schema.
	 *
	 * @param def The default value if there are no codes defined.
	 * @return
	 * 	The list of HTTP status codes.
	 * 	<br>A singleton set containing the default value if the set is empty.
	 * 	<br>Never <jk>null</jk>.
	 * @see Builder#code(int)
	 * @see Builder#codes(int[])
	 */
	public Integer getCode(Integer def) {
		return codes.isEmpty() ? def : codes.iterator().next();
	}

	/**
	 * Returns the <code>type</code> field of this schema.
	 *
	 * @return The <code>type</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#type(String)
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the <code>default</code> field of this schema.
	 *
	 * @return The default value for this schema, or <jk>null</jk> if not specified.
	 * @see Builder#_default(String)
	 */
	public String getDefault() {
		return _default;
	}

	/**
	 * Returns the <code>collectionFormat</code> field of this schema.
	 *
	 * @return The <code>collectionFormat</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#collectionFormat(String)
	 */
	public CollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the <code>type</code> field of this schema.
	 *
	 * @param cm
	 * 	The class meta of the object.
	 * 	<br>Used to auto-detect the type if the type was not specified.
	 * @return The format field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#format(String)
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
	 * @return The <code>format</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#format(String)
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * Returns the <code>maximum</code> field of this schema.
	 *
	 * @return The schema for child items of the object represented by this schema, or <jk>null</jk> if not defined.
	 * @see Builder#items(ObjectMap)
	 */
	public HttpPartSchema getItems() {
		return items;
	}

	/**
	 * Returns the <code>maximum</code> field of this schema.
	 *
	 * @return The <code>maximum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#maximum(Number)
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Returns the <code>minimum</code> field of this schema.
	 *
	 * @return The <code>minimum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#minimum(Number)
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#multipleOf(Number)
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#pattern(String)
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#maxLength(Long)
	 */
	public Long getMaxLength() {
		return maxLength;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#minLength(Long)
	 */
	public Long getMinLength() {
		return minLength;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#maxItems(Long)
	 */
	public Long getMaxItems() {
		return maxItems;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#minItems(Long)
	 */
	public Long getMinItems() {
		return minItems;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#maxProperties(Long)
	 */
	public Long getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#minProperties(Long)
	 */
	public Long getMinProperties() {
		return minProperties;
	}

	/**
	 * Returns the <code>exclusiveMaximum</code> field of this schema.
	 *
	 * @return The <code>exclusiveMaximum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#exclusiveMaximum(Boolean)
	 */
	public Boolean getExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Returns the <code>exclusiveMinimum</code> field of this schema.
	 *
	 * @return The <code>exclusiveMinimum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#exclusiveMinimum(Boolean)
	 */
	public Boolean getExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Returns the <code>uniqueItems</code> field of this schema.
	 *
	 * @return The <code>uniqueItems</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#uniqueItems(Boolean)
	 */
	public Boolean getUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Returns the <code>required</code> field of this schema.
	 *
	 * @return The <code>required</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#required(Boolean)
	 */
	public Boolean getRequired() {
		return required;
	}

	/**
	 * Returns the <code>skipIfEmpty</code> field of this schema.
	 *
	 * @return The <code>skipIfEmpty</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#skipIfEmpty(Boolean)
	 */
	public Boolean getSkipIfEmpty() {
		return skipIfEmpty;
	}

	/**
	 * Returns the <code>enum</code> field of this schema.
	 *
	 * @return The <code>enum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#_enum(Set)
	 */
	public Set<String> getEnum() {
		return _enum;
	}

	/**
	 * Returns the <code>parser</code> field of this schema.
	 *
	 * @return The <code>parser</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#parser(Class)
	 */
	public Class<? extends HttpPartParser> getParser() {
		return parser;
	}

	/**
	 * Returns the <code>serializer</code> field of this schema.
	 *
	 * @return The <code>serializer</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see Builder#serializer(Class)
	 */
	public Class<? extends HttpPartSerializer> getSerializer() {
		return serializer;
	}


	/**
	 * Returns the Swagger documentation for this schema.
	 *
	 * @return The Swagger documentation for this schema as an unmodifiable {@link ObjectMap}.
	 */
	public ObjectMap getApi() {
		return api;
	}

	/**
	 * Throws a {@link ParseException} if the specified pre-parsed input does not validate against this schema.
	 *
	 * @param in The input.
	 * @return The same object passed in.
	 * @throws SchemaValidationParseException if the specified pre-parsed input does not validate against this schema.
	 */
	public String validateInput(String in) throws SchemaValidationParseException {
		if (! isValidRequired(in))
			throw new SchemaValidationParseException("No value specified.");
		if (in != null) {
			if (! isValidAllowEmpty(in))
				throw new SchemaValidationParseException("Empty value not allowed.");
			if (! isValidPattern(in))
				throw new SchemaValidationParseException("Value does not match expected pattern.  Must match pattern: {0}", pattern.pattern());
			if (! isValidEnum(in))
				throw new SchemaValidationParseException("Value does not match one of the expected values.  Must be one of the following: {0}", _enum);
			if (! isValidMaxLength(in))
				throw new SchemaValidationParseException("Maximum length of value exceeded.");
			if (! isValidMinLength(in))
				throw new SchemaValidationParseException("Minimum length of value not met.");
		}
		return in;
	}

	/**
	 * Throws a {@link ParseException} if the specified parsed output does not validate against this schema.
	 *
	 * @param o The parsed output.
	 * @param bc The bean context used to detect POJO types.
	 * @return The same object passed in.
	 * @throws SchemaValidationParseException if the specified parsed output does not validate against this schema.
	 */
	@SuppressWarnings("rawtypes")
	public Object validateOutput(Object o, BeanContext bc) throws SchemaValidationParseException {
		if (o == null) {
			if (! isValidRequired(o))
				throw new SchemaValidationParseException("Required value not provided.");
			return o;
		}
		ClassMeta<?> cm = bc.getClassMetaForObject(o);
		switch (getType(cm)) {
			case ARRAY: {
				if (cm.isArray()) {
					if (! isValidMinItems(o))
						throw new SchemaValidationParseException("Minimum number of items not met.");
					if (! isValidMaxItems(o))
						throw new SchemaValidationParseException("Maximum number of items exceeded.");
					if (! isValidUniqueItems(o))
						throw new SchemaValidationParseException("Duplicate items not allowed.");
					HttpPartSchema items = getItems();
					if (items != null)
						for (int i = 0; i < Array.getLength(o); i++)
							items.validateOutput(Array.get(o, i), bc);
				} else if (cm.isCollection()) {
					Collection<?> c = (Collection<?>)o;
					if (! isValidMinItems(c))
						throw new SchemaValidationParseException("Minimum number of items not met.");
					if (! isValidMaxItems(c))
						throw new SchemaValidationParseException("Maximum number of items exceeded.");
					if (! isValidUniqueItems(c))
						throw new SchemaValidationParseException("Duplicate items not allowed.");
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
						throw new SchemaValidationParseException("Minimum value not met.");
					if (! isValidMaximum(n))
						throw new SchemaValidationParseException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new SchemaValidationParseException("Multiple-of not met.");
				}
				break;
			}
			case NUMBER: {
				if (cm.isNumber()) {
					Number n = (Number)o;
					if (! isValidMinimum(n))
						throw new SchemaValidationParseException("Minimum value not met.");
					if (! isValidMaximum(n))
						throw new SchemaValidationParseException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new SchemaValidationParseException("Multiple-of not met.");
				}
				break;
			}
			case OBJECT: {
				if (cm.isMapOrBean()) {
					Map<?,?> m = cm.isMap() ? (Map<?,?>)o : bc.createSession().toBeanMap(o);
					if (! isValidMinProperties(m))
						throw new SchemaValidationParseException("Minimum number of properties not met.");
					if (! isValidMaxProperties(m))
						throw new SchemaValidationParseException("Maximum number of properties exceeded.");
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
			case STRING:
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
		return (allowEmptyValue != null && allowEmptyValue) || isNotEmpty(x);
	}

	private boolean isValidPattern(String x) {
		return pattern == null || pattern.matcher(x).matches();
	}

	private boolean isValidEnum(String x) {
		return _enum.isEmpty() || _enum.contains(x);
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


	private static <T> Set<T> copy(Set<T> in) {
		return in == null ? Collections.EMPTY_SET : unmodifiableSet(new LinkedHashSet<>(in));
	}

	private static Map<String,HttpPartSchema> build(Map<String,Builder> in, boolean noValidate) {
		if (in == null)
			return null;
		Map<String,HttpPartSchema> m = new LinkedHashMap<>();
		for (Map.Entry<String,Builder> e : in.entrySet())
			m.put(e.getKey(), e.getValue().noValidate(noValidate).build());
		return unmodifiableMap(m);
	}

	private static HttpPartSchema build(Builder in, boolean noValidate) {
		return in == null ? null : in.noValidate(noValidate).build();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	final static Set<String> toSet(String[] s) {
		return toSet(joinnl(s));
	}

	final static Set<String> toSet(String s) {
		if (isEmpty(s))
			return null;
		Set<String> set = new ASet<>();
		try {
			for (Object o : StringUtils.parseListOrCdl(s))
				set.add(o.toString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
		return set;
	}

	final static Number toNumber(String s) {
		try {
			if (isNotEmpty(s))
				return parseNumber(s, Number.class);
			return null;
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

//	final static Integer toInteger(int i) {
//		return i == -1 ? null : i;
//	}

	final static Long toLong(long l) {
		return l == -1 ? null : l;
	}

	final static Boolean toBoolean(boolean b) {
		return b == false ? null : b;
	}

	final static ObjectMap toObjectMap(String[] ss) {
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isObjectMap(s, true))
			s = "{" + s + "}";
		try {
			return new ObjectMap(s);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
