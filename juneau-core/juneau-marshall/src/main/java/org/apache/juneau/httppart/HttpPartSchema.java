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
import static org.apache.juneau.httppart.HttpPartSchema.Type.*;
import static org.apache.juneau.httppart.HttpPartSchema.Format.*;

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
	final boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
	final CollectionFormat collectionFormat;
	final Type type;
	final Format format;
	final Pattern pattern;
	final HttpPartSchema items, additionalProperties;
	final Number maximum, minimum, multipleOf;
	final Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
	final Class<? extends HttpPartParser> parser;
	final Class<? extends HttpPartSerializer> serializer;
	final ClassMeta<?> parsedType;

	final ObjectMap api;

	/**
	 * Instantiates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder create() {
		return new HttpPartSchemaBuilder();
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
	 * Finds the schema information for the specified method return.
	 *
	 * <p>
	 * This method will gather all the schema information from the annotations at the following locations:
	 * <ul>
	 * 	<li>The method.
	 * 	<li>The method return class.
	 * 	<li>The method return parent classes and interfaces.
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
	 * 	The Java method with the return type being checked.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, Method m) {
		return create().apply(c, m).build();
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
	 * Shortcut for calling <code>create().type(type);</code>
	 *
	 * @param type The schema type value.
	 * @return A new builder.
	 */
	public static HttpPartSchemaBuilder create(String type) {
		return create().type(type);
	}

	/**
	 * Shortcut for calling <code>create().type(type).format(format);</code>
	 *
	 * @param type The schema type value.
	 * @param format The schema format value.
	 * @return A new builder.
	 */
	public static HttpPartSchemaBuilder create(String type, String format) {
		return create().type(type).format(format);
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

	HttpPartSchema(HttpPartSchemaBuilder b) {
		this.name = b.name;
		this.codes = copy(b.codes);
		this._default = b._default;
		this._enum = copy(b._enum);
		this.properties = build(b.properties, b.noValidate);
		this.allowEmptyValue = resolve(b.allowEmptyValue);
		this.exclusiveMaximum = resolve(b.exclusiveMaximum);
		this.exclusiveMinimum = resolve(b.exclusiveMinimum);
		this.required = resolve(b.required);
		this.uniqueItems = resolve(b.uniqueItems);
		this.skipIfEmpty = resolve(b.skipIfEmpty);
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

		// Calculate parse type
		Class<?> parsedType = Object.class;
		if (type == ARRAY) {
			if (items != null)
				parsedType = Array.newInstance(items.parsedType.getInnerClass(), 0).getClass();
		} else if (type == BOOLEAN) {
			parsedType = Boolean.class;
		} else if (type == INTEGER) {
			if (format == INT64)
				parsedType = Long.class;
			else
				parsedType = Integer.class;
		} else if (type == NUMBER) {
			if (format == DOUBLE)
				parsedType = Double.class;
			else
				parsedType = Float.class;
		} else if (type == STRING) {
			if (format == BYTE || format == BINARY || format == BINARY_SPACED)
				parsedType = byte[].class;
			else if (format == DATE || format == DATE_TIME)
				parsedType = Calendar.class;
			else
				parsedType = String.class;
		}
		this.parsedType = BeanContext.DEFAULT.getClassMeta(parsedType);

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
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.BYTE, Format.BINARY, Format.BINARY_SPACED, Format.DATE, Format.DATE_TIME, Format.PASSWORD, Format.UON, Format.NO_FORMAT);
				break;
			}
			case ARRAY: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NO_FORMAT, Format.UON);
				break;
			}
			case BOOLEAN: {
				notAllowed.appendIf(! _enum.isEmpty(), "_enum");
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
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
				invalidFormat = ! format.isOneOf(Format.NO_FORMAT, Format.UON);
				break;
			}
			case FILE: {
				break;
			}
			case INTEGER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NO_FORMAT, Format.UON, Format.INT32, Format.INT64);
				break;
			}
			case NUMBER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(Format.NO_FORMAT, Format.UON, Format.FLOAT, Format.DOUBLE);
				break;
			}
			case OBJECT: {
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != CollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				invalidFormat = ! format.isOneOf(Format.NO_FORMAT, Format.UON);
				break;
			}
			default:
				break;
		}

		if (! notAllowed.isEmpty())
			errors.add("Attributes not allow for type='"+type+"': " + StringUtils.join(notAllowed, ","));
		if (invalidFormat)
			errors.add("Invalid format for type='"+type+"': '"+format+"'");
		if (exclusiveMaximum && maximum == null)
			errors.add("Cannot specify exclusiveMaximum with maximum.");
		if (exclusiveMinimum && minimum == null)
			errors.add("Cannot specify exclusiveMinimum with minimum.");
		if (required && _default != null)
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
		if (type == ARRAY && items != null && items.getType() == OBJECT && (format != UON && format != Format.NO_FORMAT))
			errors.add("Cannot define an array of objects unless array format is 'uon'.");

		if (! errors.isEmpty())
			throw new ContextRuntimeException("Schema specification errors: \n\t" + join(errors, "\n\t"));
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
		NO_COLLECTION_FORMAT;

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
		NO_TYPE;

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
		NO_FORMAT;

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
	 * Returns the default parsed type for this schema.
	 *
	 * @return The default parsed type for this schema.  Never <jk>null</jk>.
	 */
	public ClassMeta<?> getParsedType() {
		return parsedType;
	}

	/**
	 * Returns the name of the object described by this schema, for example the query or form parameter name.
	 *
	 * @return The name, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#name(String)
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
	 * @see HttpPartSchemaBuilder#code(int)
	 * @see HttpPartSchemaBuilder#codes(int[])
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
	 * @see HttpPartSchemaBuilder#code(int)
	 * @see HttpPartSchemaBuilder#codes(int[])
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
	 * @see HttpPartSchemaBuilder#code(int)
	 * @see HttpPartSchemaBuilder#codes(int[])
	 */
	public Integer getCode(Integer def) {
		return codes.isEmpty() ? def : codes.iterator().next();
	}

	/**
	 * Returns the <code>type</code> field of this schema.
	 *
	 * @return The <code>type</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#type(String)
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Returns the <code>default</code> field of this schema.
	 *
	 * @return The default value for this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#_default(String)
	 */
	public String getDefault() {
		return _default;
	}

	/**
	 * Returns the <code>collectionFormat</code> field of this schema.
	 *
	 * @return The <code>collectionFormat</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#collectionFormat(String)
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
	 * @see HttpPartSchemaBuilder#format(String)
	 */
	public Type getType(ClassMeta<?> cm) {
		if (type != Type.NO_TYPE)
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
	 * @see HttpPartSchemaBuilder#format(String)
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * Returns the <code>maximum</code> field of this schema.
	 *
	 * @return The schema for child items of the object represented by this schema, or <jk>null</jk> if not defined.
	 * @see HttpPartSchemaBuilder#items(HttpPartSchemaBuilder)
	 */
	public HttpPartSchema getItems() {
		return items;
	}

	/**
	 * Returns the <code>maximum</code> field of this schema.
	 *
	 * @return The <code>maximum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maximum(Number)
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Returns the <code>minimum</code> field of this schema.
	 *
	 * @return The <code>minimum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minimum(Number)
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#multipleOf(Number)
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#pattern(String)
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxLength(Long)
	 */
	public Long getMaxLength() {
		return maxLength;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minLength(Long)
	 */
	public Long getMinLength() {
		return minLength;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxItems(Long)
	 */
	public Long getMaxItems() {
		return maxItems;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minItems(Long)
	 */
	public Long getMinItems() {
		return minItems;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxProperties(Long)
	 */
	public Long getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Returns the <code>xxx</code> field of this schema.
	 *
	 * @return The <code>xxx</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minProperties(Long)
	 */
	public Long getMinProperties() {
		return minProperties;
	}

	/**
	 * Returns the <code>exclusiveMaximum</code> field of this schema.
	 *
	 * @return The <code>exclusiveMaximum</code> field of this schema.
	 * @see HttpPartSchemaBuilder#exclusiveMaximum(Boolean)
	 */
	public boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Returns the <code>exclusiveMinimum</code> field of this schema.
	 *
	 * @return The <code>exclusiveMinimum</code> field of this schema.
	 * @see HttpPartSchemaBuilder#exclusiveMinimum(Boolean)
	 */
	public boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Returns the <code>uniqueItems</code> field of this schema.
	 *
	 * @return The <code>uniqueItems</code> field of this schema.
	 * @see HttpPartSchemaBuilder#uniqueItems(Boolean)
	 */
	public boolean isUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Returns the <code>required</code> field of this schema.
	 *
	 * @return The <code>required</code> field of this schema.
	 * @see HttpPartSchemaBuilder#required(Boolean)
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Returns the <code>skipIfEmpty</code> field of this schema.
	 *
	 * @return The <code>skipIfEmpty</code> field of this schema.
	 * @see HttpPartSchemaBuilder#skipIfEmpty(Boolean)
	 */
	public boolean isSkipIfEmpty() {
		return skipIfEmpty;
	}

	/**
	 * Returns the <code>allowEmptyValue</code> field of this schema.
	 *
	 * @return The <code>skipIfEmpty</code> field of this schema.
	 * @see HttpPartSchemaBuilder#skipIfEmpty(Boolean)
	 */
	public boolean isAllowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Returns the <code>enum</code> field of this schema.
	 *
	 * @return The <code>enum</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#_enum(Set)
	 */
	public Set<String> getEnum() {
		return _enum;
	}

	/**
	 * Returns the <code>parser</code> field of this schema.
	 *
	 * @return The <code>parser</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#parser(Class)
	 */
	public Class<? extends HttpPartParser> getParser() {
		return parser;
	}

	/**
	 * Returns the <code>serializer</code> field of this schema.
	 *
	 * @return The <code>serializer</code> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#serializer(Class)
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
	 * @throws SchemaValidationException if the specified pre-parsed input does not validate against this schema.
	 */
	public String validateInput(String in) throws SchemaValidationException {
		if (! isValidRequired(in))
			throw new SchemaValidationException("No value specified.");
		if (in != null) {
			if (! isValidAllowEmpty(in))
				throw new SchemaValidationException("Empty value not allowed.");
			if (! isValidPattern(in))
				throw new SchemaValidationException("Value does not match expected pattern.  Must match pattern: {0}", pattern.pattern());
			if (! isValidEnum(in))
				throw new SchemaValidationException("Value does not match one of the expected values.  Must be one of the following: {0}", _enum);
			if (! isValidMaxLength(in))
				throw new SchemaValidationException("Maximum length of value exceeded.");
			if (! isValidMinLength(in))
				throw new SchemaValidationException("Minimum length of value not met.");
		}
		return in;
	}

	/**
	 * Throws a {@link ParseException} if the specified parsed output does not validate against this schema.
	 *
	 * @param o The parsed output.
	 * @param bc The bean context used to detect POJO types.
	 * @return The same object passed in.
	 * @throws SchemaValidationException if the specified parsed output does not validate against this schema.
	 */
	@SuppressWarnings("rawtypes")
	public <T> T validateOutput(T o, BeanContext bc) throws SchemaValidationException {
		if (o == null) {
			if (! isValidRequired(o))
				throw new SchemaValidationException("Required value not provided.");
			return o;
		}
		ClassMeta<?> cm = bc.getClassMetaForObject(o);
		switch (getType(cm)) {
			case ARRAY: {
				if (cm.isArray()) {
					if (! isValidMinItems(o))
						throw new SchemaValidationException("Minimum number of items not met.");
					if (! isValidMaxItems(o))
						throw new SchemaValidationException("Maximum number of items exceeded.");
					if (! isValidUniqueItems(o))
						throw new SchemaValidationException("Duplicate items not allowed.");
					HttpPartSchema items = getItems();
					if (items != null)
						for (int i = 0; i < Array.getLength(o); i++)
							items.validateOutput(Array.get(o, i), bc);
				} else if (cm.isCollection()) {
					Collection<?> c = (Collection<?>)o;
					if (! isValidMinItems(c))
						throw new SchemaValidationException("Minimum number of items not met.");
					if (! isValidMaxItems(c))
						throw new SchemaValidationException("Maximum number of items exceeded.");
					if (! isValidUniqueItems(c))
						throw new SchemaValidationException("Duplicate items not allowed.");
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
						throw new SchemaValidationException("Minimum value not met.");
					if (! isValidMaximum(n))
						throw new SchemaValidationException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new SchemaValidationException("Multiple-of not met.");
				}
				break;
			}
			case NUMBER: {
				if (cm.isNumber()) {
					Number n = (Number)o;
					if (! isValidMinimum(n))
						throw new SchemaValidationException("Minimum value not met.");
					if (! isValidMaximum(n))
						throw new SchemaValidationException("Maximum value exceeded.");
					if (! isValidMultipleOf(n))
						throw new SchemaValidationException("Multiple-of not met.");
				}
				break;
			}
			case OBJECT: {
				if (cm.isMapOrBean()) {
					Map<?,?> m = cm.isMap() ? (Map<?,?>)o : bc.createSession().toBeanMap(o);
					if (! isValidMinProperties(m))
						throw new SchemaValidationException("Minimum number of properties not met.");
					if (! isValidMaxProperties(m))
						throw new SchemaValidationException("Maximum number of properties exceeded.");
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
			case NO_TYPE:
				break;
		}
		return o;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private boolean isValidRequired(Object x) {
		return x != null || ! required;
	}

	private boolean isValidMinProperties(Map<?,?> x) {
		return minProperties == null || x.size() >= minProperties;
	}

	private boolean isValidMaxProperties(Map<?,?> x) {
		return maxProperties == null || x.size() <= maxProperties;
	}

	private boolean isValidMinimum(Number x) {
		if (x instanceof Integer)
			return minimum == null || x.intValue() > minimum.intValue() || (x.intValue() == minimum.intValue() && (! exclusiveMinimum));
		if (x instanceof Short)
			return minimum == null || x.shortValue() > minimum.shortValue() || (x.intValue() == minimum.shortValue() && (! exclusiveMinimum));
		if (x instanceof Long)
			return minimum == null || x.longValue() > minimum.longValue() || (x.intValue() == minimum.longValue() && (! exclusiveMinimum));
		if (x instanceof Float)
			return minimum == null || x.floatValue() > minimum.floatValue() || (x.floatValue() == minimum.floatValue() && (! exclusiveMinimum));
		if (x instanceof Double)
			return minimum == null || x.doubleValue() > minimum.doubleValue() || (x.doubleValue() == minimum.doubleValue() && (! exclusiveMinimum));
		return true;
	}

	private boolean isValidMaximum(Number x) {
		if (x instanceof Integer)
			return maximum == null || x.intValue() < maximum.intValue() || (x.intValue() == maximum.intValue() && (! exclusiveMaximum));
		if (x instanceof Short)
			return maximum == null || x.shortValue() < maximum.shortValue() || (x.intValue() == maximum.shortValue() && (! exclusiveMaximum));
		if (x instanceof Long)
			return maximum == null || x.longValue() < maximum.longValue() || (x.intValue() == maximum.longValue() && (! exclusiveMaximum));
		if (x instanceof Float)
			return maximum == null || x.floatValue() < maximum.floatValue() || (x.floatValue() == maximum.floatValue() && (! exclusiveMaximum));
		if (x instanceof Double)
			return maximum == null || x.doubleValue() < maximum.doubleValue() || (x.doubleValue() == maximum.doubleValue() && (! exclusiveMaximum));
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
		return allowEmptyValue || isNotEmpty(x);
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
		if (uniqueItems) {
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
		if (uniqueItems && ! (x instanceof Set)) {
			Set<Object> s = new HashSet<>();
			for (Object o : x)
				if (! s.add(o))
					return false;
		}
		return true;
	}

	/**
	 * Returns the schema information for the specified property.
	 *
	 * @param name The property name.
	 * @return The schema information for the specified property, or <jk>null</jk> if properties are not defined on this schema.
	 */
	public HttpPartSchema getProperty(String name) {
		if (properties != null) {
			HttpPartSchema schema = properties.get(name);
			if (schema != null)
				return schema;
		}
		return additionalProperties;
	}

	/**
	 * Returns <jk>true</jk> if this schema has properties associated with it.
	 *
	 * @return <jk>true</jk> if this schema has properties associated with it.
	 */
	public boolean hasProperties() {
		return properties != null || additionalProperties != null;
	}

	private static <T> Set<T> copy(Set<T> in) {
		return in == null ? Collections.EMPTY_SET : unmodifiableSet(new LinkedHashSet<>(in));
	}

	private static Map<String,HttpPartSchema> build(Map<String,HttpPartSchemaBuilder> in, boolean noValidate) {
		if (in == null)
			return null;
		Map<String,HttpPartSchema> m = new LinkedHashMap<>();
		for (Map.Entry<String,HttpPartSchemaBuilder> e : in.entrySet())
			m.put(e.getKey(), e.getValue().noValidate(noValidate).build());
		return unmodifiableMap(m);
	}

	private static HttpPartSchema build(HttpPartSchemaBuilder in, boolean noValidate) {
		return in == null ? null : in.noValidate(noValidate).build();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private boolean resolve(Boolean b) {
		return b == null ? false : b;
	}

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
