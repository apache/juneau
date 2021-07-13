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
import static org.apache.juneau.httppart.HttpPartDataType.*;
import static org.apache.juneau.httppart.HttpPartFormat.*;
import static org.apache.juneau.internal.ExceptionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;

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
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc OpenApiDetails}
 * </ul>
 */
public class HttpPartSchema {

	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Reusable instance of this object, all default settings. */
	public static final HttpPartSchema DEFAULT = HttpPartSchema.create().allowEmptyValue(true).build();

	/** Boolean type */
	public static final HttpPartSchema T_BOOLEAN = HttpPartSchema.tBoolean().build();

	/** File type */
	public static final HttpPartSchema T_FILE = HttpPartSchema.tFile().build();

	/** Integer type */
	public static final HttpPartSchema T_INTEGER = HttpPartSchema.tInteger().build();

	/** Int32 type */
	public static final HttpPartSchema T_INT32 = HttpPartSchema.tInt32().build();

	/** Int64 type */
	public static final HttpPartSchema T_INT64 = HttpPartSchema.tInt64().build();

	/** No type */
	public static final HttpPartSchema T_NONE = HttpPartSchema.tNone().build();

	/** Number type */
	public static final HttpPartSchema T_NUMBER = HttpPartSchema.tNumber().build();

	/** Float type */
	public static final HttpPartSchema T_FLOAT = HttpPartSchema.tFloat().build();

	/** Double type */
	public static final HttpPartSchema T_DOUBLE = HttpPartSchema.tDouble().build();

	/** String type */
	public static final HttpPartSchema T_STRING = HttpPartSchema.tString().build();

	/** Byte type */
	public static final HttpPartSchema T_BYTE = HttpPartSchema.tByte().build();

	/** Binary type */
	public static final HttpPartSchema T_BINARY = HttpPartSchema.tBinary().build();

	/** Spaced binary type */
	public static final HttpPartSchema T_BINARY_SPACED = HttpPartSchema.tBinarySpaced().build();

	/** Date type */
	public static final HttpPartSchema T_DATE = HttpPartSchema.tDate().build();

	/** Date-time type */
	public static final HttpPartSchema T_DATETIME = HttpPartSchema.tDateTime().build();

	/** UON-formated simple type */
	public static final HttpPartSchema T_UON = HttpPartSchema.tUon().build();

	/** Array type */
	public static final HttpPartSchema T_ARRAY = HttpPartSchema.tArray().build();

	/** Comma-delimited array type */
	public static final HttpPartSchema T_ARRAY_CSV = HttpPartSchema.tArrayCsv().build();

	/** Pipe-delimited array type */
	public static final HttpPartSchema T_ARRAY_PIPES = HttpPartSchema.tArrayPipes().build();

	/** Space-delimited array type */
	public static final HttpPartSchema T_ARRAY_SSV = HttpPartSchema.tArraySsv().build();

	/** Tab-delimited array type */
	public static final HttpPartSchema T_ARRAY_TSV = HttpPartSchema.tArrayTsv().build();

	/** UON-formatted array type */
	public static final HttpPartSchema T_ARRAY_UON = HttpPartSchema.tArrayUon().build();

	/** Multi-part array type */
	public static final HttpPartSchema T_ARRAY_MULTI = HttpPartSchema.tArrayMulti().build();

	/** Object type */
	public static final HttpPartSchema T_OBJECT = HttpPartSchema.tObject().build();

	/** Comma-delimited object type */
	public static final HttpPartSchema T_OBJECT_CSV = HttpPartSchema.tObjectCsv().build();

	/** Pipe-delimited object type */
	public static final HttpPartSchema T_OBJECT_PIPES = HttpPartSchema.tObjectPipes().build();

	/** Space-delimited object type */
	public static final HttpPartSchema T_OBJECT_SSV = HttpPartSchema.tObjectSsv().build();

	/** Tab-delimited object type */
	public static final HttpPartSchema T_OBJECT_TSV = HttpPartSchema.tObjectTsv().build();

	/** UON-formated object type */
	public static final HttpPartSchema T_OBJECT_UON = HttpPartSchema.tObjectUon().build();


	final String name;
	final String _default;
	final Set<String> _enum;
	final Map<String,HttpPartSchema> properties;
	final boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
	final HttpPartCollectionFormat collectionFormat;
	final HttpPartDataType type;
	final HttpPartFormat format;
	final Pattern pattern;
	final HttpPartSchema items, additionalProperties;
	final Number maximum, minimum, multipleOf;
	final Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
	final Class<? extends HttpPartParser> parser;
	final Class<? extends HttpPartSerializer> serializer;
	final ClassMeta<?> parsedType;

	/**
	 * Instantiates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder create() {
		return new HttpPartSchemaBuilder();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>BOOLEAN</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tBoolean() {
		return create().tBoolean();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>FILE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tFile() {
		return create().tFile();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tInteger() {
		return create().tInteger();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>).format(HttpPartFormat.<jsf>INT32</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tInt32() {
		return create().tInteger().fInt32();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>).format(HttpPartFormat.<jsf>INT64</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tInt64() {
		return create().tInteger().fInt64();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NONE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tNone() {
		return create().tNone();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tNumber() {
		return create().tNumber();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>).format(HttpPartFormat.<jsf>FLOAT</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tFloat() {
		return create().tNumber().fFloat();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>).format(HttpPartFormat.<jsf>DOUBLE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tDouble() {
		return create().tNumber().fDouble();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tString() {
		return create().tString();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BYTE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tByte() {
		return create().tString().fByte();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BINARY</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tBinary() {
		return create().tString().fBinary();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BINARY_SPACED</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tBinarySpaced() {
		return create().tString().fBinarySpaced();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>DATE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tDate() {
		return create().tString().fDate();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>DATE_TIME</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tDateTime() {
		return create().tString().fDateTime();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>UON</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tUon() {
		return create().tString().fUon();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArray() {
		return create().tArray();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArray(HttpPartSchemaBuilder items) {
		return create().tArray().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayCsv() {
		return create().tArray().cfCsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayCsv(HttpPartSchemaBuilder items) {
		return create().tArray().cfCsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayPipes() {
		return create().tArray().cfPipes();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayPipes(HttpPartSchemaBuilder items) {
		return create().tArray().cfPipes().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArraySsv() {
		return create().tArray().cfSsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArraySsv(HttpPartSchemaBuilder items) {
		return create().tArray().cfSsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayTsv() {
		return create().tArray().cfTsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayTsv(HttpPartSchemaBuilder items) {
		return create().tArray().cfTsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UONC</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayUon() {
		return create().tArray().cfUon();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UONC</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayUon(HttpPartSchemaBuilder items) {
		return create().tArray().cfUon().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>MULTI</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayMulti() {
		return create().tArray().cfMulti();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>MULTI</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tArrayMulti(HttpPartSchemaBuilder items) {
		return create().tArray().cfMulti().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObject() {
		return create().tObject();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObjectCsv() {
		return create().tObject().cfCsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObjectPipes() {
		return create().tObject().cfPipes();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObjectSsv() {
		return create().tObject().cfSsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObjectTsv() {
		return create().tObject().cfTsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UON</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static HttpPartSchemaBuilder tObjectUon() {
		return create().tObject().cfUon();
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
	 * 		<li>{@link ResponseBody}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param mpi The Java method parameter.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, ParamInfo mpi) {
		return create().apply(c, mpi).build();
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
	 * Shortcut for calling <c>create().type(type);</c>
	 *
	 * @param type The schema type value.
	 * @return A new builder.
	 */
	public static HttpPartSchemaBuilder create(String type) {
		return create().type(type);
	}

	/**
	 * Shortcut for calling <c>create().type(type).format(format);</c>
	 *
	 * @param type The schema type value.
	 * @param format The schema format value.
	 * @return A new builder.
	 */
	public static HttpPartSchemaBuilder create(String type, String format) {
		return create().type(type).format(format);
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

	/**
	 * Finds the schema information on the specified annotation.
	 *
	 * @param a
	 * 	The annotation to find the schema information on..
	 * @param defaultName The default part name if not specified on the annotation.
	 * @return The schema information found on the annotation.
	 */
	public static HttpPartSchema create(Annotation a, String defaultName) {
		return create().name(defaultName).apply(a).build();
	}

	HttpPartSchema(HttpPartSchemaBuilder b) {
		this.name = b.name;
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
		AList<String> notAllowed = AList.create();
		boolean invalidFormat = false;
		switch (type) {
			case STRING: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.BYTE, HttpPartFormat.BINARY, HttpPartFormat.BINARY_SPACED, HttpPartFormat.DATE, HttpPartFormat.DATE_TIME, HttpPartFormat.PASSWORD, HttpPartFormat.UON, HttpPartFormat.NO_FORMAT);
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
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON);
				break;
			}
			case BOOLEAN: {
				notAllowed.appendIf(! _enum.isEmpty(), "_enum");
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
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
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON);
				break;
			}
			case FILE: {
				break;
			}
			case INTEGER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON, HttpPartFormat.INT32, HttpPartFormat.INT64);
				break;
			}
			case NUMBER: {
				notAllowed.appendIf(properties != null, "properties");
				notAllowed.appendIf(additionalProperties != null, "additionalProperties");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(maxProperties != null, "maxProperties");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				notAllowed.appendIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON, HttpPartFormat.FLOAT, HttpPartFormat.DOUBLE);
				break;
			}
			case OBJECT: {
				notAllowed.appendIf(exclusiveMaximum, "exclusiveMaximum");
				notAllowed.appendIf(exclusiveMinimum, "exclusiveMinimum");
				notAllowed.appendIf(uniqueItems, "uniqueItems");
				notAllowed.appendIf(pattern != null, "pattern");
				notAllowed.appendIf(items != null, "items");
				notAllowed.appendIf(maximum != null, "maximum");
				notAllowed.appendIf(minimum != null, "minimum");
				notAllowed.appendIf(multipleOf != null, "multipleOf");
				notAllowed.appendIf(maxItems != null, "maxItems");
				notAllowed.appendIf(maxLength != null, "maxLength");
				notAllowed.appendIf(minItems != null, "minItems");
				notAllowed.appendIf(minLength != null, "minLength");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT);
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
		if (type == ARRAY && items != null && items.getType() == OBJECT && (format != UON && format != HttpPartFormat.NO_FORMAT))
			errors.add("Cannot define an array of objects unless array format is 'uon'.");

		if (! errors.isEmpty())
			throw new ContextRuntimeException("Schema specification errors: \n\t" + join(errors, "\n\t"), new Object[0]);
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
	 * Returns the <c>type</c> field of this schema.
	 *
	 * @return The <c>type</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#type(String)
	 */
	public HttpPartDataType getType() {
		return type;
	}

	/**
	 * Returns the <c>type</c> field of this schema.
	 *
	 * @param cm
	 * 	The class meta of the object.
	 * 	<br>Used to auto-detect the type if the type was not specified.
	 * @return The format field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#format(String)
	 */
	public HttpPartDataType getType(ClassMeta<?> cm) {
		if (type != HttpPartDataType.NO_TYPE)
			return type;
		if (cm.isTemporal() || cm.isDateOrCalendar())
			return HttpPartDataType.STRING;
		if (cm.isNumber()) {
			if (cm.isDecimal())
				return HttpPartDataType.NUMBER;
			return HttpPartDataType.INTEGER;
		}
		if (cm.isBoolean())
			return HttpPartDataType.BOOLEAN;
		if (cm.isMapOrBean())
			return HttpPartDataType.OBJECT;
		if (cm.isCollectionOrArray())
			return HttpPartDataType.ARRAY;
		return HttpPartDataType.STRING;
	}

	/**
	 * Returns the <c>default</c> field of this schema.
	 *
	 * @return The default value for this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#_default(String)
	 */
	public String getDefault() {
		return _default;
	}

	/**
	 * Returns the <c>collectionFormat</c> field of this schema.
	 *
	 * @return The <c>collectionFormat</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#collectionFormat(String)
	 */
	public HttpPartCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the <c>format</c> field of this schema.
	 *
	 * @see HttpPartSchemaBuilder#format(String)
	 * @return The <c>format</c> field of this schema, or <jk>null</jk> if not specified.
	 */
	public HttpPartFormat getFormat() {
		return format;
	}

	/**
	 * Returns the <c>format</c> field of this schema.
	 *
	 * @param cm
	 * 	The class meta of the object.
	 * 	<br>Used to auto-detect the format if the format was not specified.
	 * @return The <c>format</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#format(String)
	 */
	public HttpPartFormat getFormat(ClassMeta<?> cm) {
		if (format != HttpPartFormat.NO_FORMAT)
			return format;
		if (cm.isNumber()) {
			if (cm.isDecimal()) {
				if (cm.isDouble())
					return HttpPartFormat.DOUBLE;
				return HttpPartFormat.FLOAT;
			}
			if (cm.isLong())
				return HttpPartFormat.INT64;
			return HttpPartFormat.INT32;
		}
		return format;
	}

	/**
	 * Returns the <c>maximum</c> field of this schema.
	 *
	 * @return The schema for child items of the object represented by this schema, or <jk>null</jk> if not defined.
	 * @see HttpPartSchemaBuilder#items(HttpPartSchemaBuilder)
	 */
	public HttpPartSchema getItems() {
		return items;
	}

	/**
	 * Returns the <c>maximum</c> field of this schema.
	 *
	 * @return The <c>maximum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maximum(Number)
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Returns the <c>minimum</c> field of this schema.
	 *
	 * @return The <c>minimum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minimum(Number)
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#multipleOf(Number)
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#pattern(String)
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxLength(Long)
	 */
	public Long getMaxLength() {
		return maxLength;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minLength(Long)
	 */
	public Long getMinLength() {
		return minLength;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxItems(Long)
	 */
	public Long getMaxItems() {
		return maxItems;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minItems(Long)
	 */
	public Long getMinItems() {
		return minItems;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#maxProperties(Long)
	 */
	public Long getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#minProperties(Long)
	 */
	public Long getMinProperties() {
		return minProperties;
	}

	/**
	 * Returns the <c>exclusiveMaximum</c> field of this schema.
	 *
	 * @return The <c>exclusiveMaximum</c> field of this schema.
	 * @see HttpPartSchemaBuilder#exclusiveMaximum(Boolean)
	 */
	public boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Returns the <c>exclusiveMinimum</c> field of this schema.
	 *
	 * @return The <c>exclusiveMinimum</c> field of this schema.
	 * @see HttpPartSchemaBuilder#exclusiveMinimum(Boolean)
	 */
	public boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Returns the <c>uniqueItems</c> field of this schema.
	 *
	 * @return The <c>uniqueItems</c> field of this schema.
	 * @see HttpPartSchemaBuilder#uniqueItems(Boolean)
	 */
	public boolean isUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Returns the <c>required</c> field of this schema.
	 *
	 * @return The <c>required</c> field of this schema.
	 * @see HttpPartSchemaBuilder#required(Boolean)
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Returns the <c>skipIfEmpty</c> field of this schema.
	 *
	 * @return The <c>skipIfEmpty</c> field of this schema.
	 * @see HttpPartSchemaBuilder#skipIfEmpty(Boolean)
	 */
	public boolean isSkipIfEmpty() {
		return skipIfEmpty;
	}

	/**
	 * Returns the <c>allowEmptyValue</c> field of this schema.
	 *
	 * @return The <c>skipIfEmpty</c> field of this schema.
	 * @see HttpPartSchemaBuilder#skipIfEmpty(Boolean)
	 */
	public boolean isAllowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Returns the <c>enum</c> field of this schema.
	 *
	 * @return The <c>enum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#_enum(Set)
	 */
	public Set<String> getEnum() {
		return _enum;
	}

	/**
	 * Returns the <c>parser</c> field of this schema.
	 *
	 * @return The <c>parser</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#parser(Class)
	 */
	public Class<? extends HttpPartParser> getParser() {
		return parser;
	}

	/**
	 * Returns the <c>serializer</c> field of this schema.
	 *
	 * @return The <c>serializer</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchemaBuilder#serializer(Class)
	 */
	public Class<? extends HttpPartSerializer> getSerializer() {
		return serializer;
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
				throw new SchemaValidationException("Value does not match one of the expected values.  Must be one of the following:  {0}", cdl(_enum));
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
		if (x instanceof Integer || x instanceof AtomicInteger)
			return minimum == null || x.intValue() > minimum.intValue() || (x.intValue() == minimum.intValue() && (! exclusiveMinimum));
		if (x instanceof Short || x instanceof Byte)
			return minimum == null || x.shortValue() > minimum.shortValue() || (x.intValue() == minimum.shortValue() && (! exclusiveMinimum));
		if (x instanceof Long || x instanceof AtomicLong || x instanceof BigInteger)
			return minimum == null || x.longValue() > minimum.longValue() || (x.intValue() == minimum.longValue() && (! exclusiveMinimum));
		if (x instanceof Float)
			return minimum == null || x.floatValue() > minimum.floatValue() || (x.floatValue() == minimum.floatValue() && (! exclusiveMinimum));
		if (x instanceof Double || x instanceof BigDecimal)
			return minimum == null || x.doubleValue() > minimum.doubleValue() || (x.doubleValue() == minimum.doubleValue() && (! exclusiveMinimum));
		return true;
	}

	private boolean isValidMaximum(Number x) {
		if (x instanceof Integer || x instanceof AtomicInteger)
			return maximum == null || x.intValue() < maximum.intValue() || (x.intValue() == maximum.intValue() && (! exclusiveMaximum));
		if (x instanceof Short || x instanceof Byte)
			return maximum == null || x.shortValue() < maximum.shortValue() || (x.intValue() == maximum.shortValue() && (! exclusiveMaximum));
		if (x instanceof Long || x instanceof AtomicLong || x instanceof BigInteger)
			return maximum == null || x.longValue() < maximum.longValue() || (x.intValue() == maximum.longValue() && (! exclusiveMaximum));
		if (x instanceof Float)
			return maximum == null || x.floatValue() < maximum.floatValue() || (x.floatValue() == maximum.floatValue() && (! exclusiveMaximum));
		if (x instanceof Double || x instanceof BigDecimal)
			return maximum == null || x.doubleValue() < maximum.doubleValue() || (x.doubleValue() == maximum.doubleValue() && (! exclusiveMaximum));
		return true;
	}

	private boolean isValidMultipleOf(Number x) {
		if (x instanceof Integer || x instanceof AtomicInteger)
			return multipleOf == null || x.intValue() % multipleOf.intValue() == 0;
		if (x instanceof Short || x instanceof Byte)
			return multipleOf == null || x.shortValue() % multipleOf.shortValue() == 0;
		if (x instanceof Long || x instanceof AtomicLong || x instanceof BigInteger)
			return multipleOf == null || x.longValue() % multipleOf.longValue() == 0;
		if (x instanceof Float)
			return multipleOf == null || x.floatValue() % multipleOf.floatValue() == 0;
		if (x instanceof Double || x instanceof BigDecimal)
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
		return in == null ? Collections.emptySet() : unmodifiableSet(new LinkedHashSet<>(in));
	}

	private static Map<String,HttpPartSchema> build(Map<String,Object> in, boolean noValidate) {
		if (in == null)
			return null;
		Map<String,HttpPartSchema> m = new LinkedHashMap<>();
		for (Map.Entry<String,Object> e : in.entrySet()) {
			Object v = e.getValue();
			m.put(e.getKey(), build(v, noValidate));
		}
		return unmodifiableMap(m);
	}

	private static HttpPartSchema build(Object in, boolean noValidate) {
		if (in == null)
			return null;
		if (in instanceof HttpPartSchema)
			return (HttpPartSchema)in;
		return ((HttpPartSchemaBuilder)in).noValidate(noValidate).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private boolean resolve(Boolean b) {
		return b == null ? false : b;
	}

	final static Set<String> toSet(String[]...s) {
		for (String[] ss : s)
			if (ss != null && ss.length > 0)
				return toSet(joinnl(ss));
		return null;
	}

	final static Set<String> toSet(String s) {
		if (isEmpty(s))
			return null;
		Set<String> set = ASet.of();
		try {
			for (Object o : StringUtils.parseListOrCdl(s))
				set.add(o.toString());
		} catch (ParseException e) {
			throw runtimeException(e);
		}
		return set;
	}

	final static Number toNumber(String...s) {
		try {
			for (String ss : s)
				if (isNotEmpty(ss))
					return parseNumber(ss, Number.class);
			return null;
		} catch (ParseException e) {
			throw runtimeException(e);
		}
	}

	final static OMap toOMap(String[] ss) {
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isJsonObject(s, true))
			s = "{" + s + "}";
		try {
			return OMap.ofJson(s);
		} catch (ParseException e) {
			throw runtimeException(e);
		}
	}

	@Override
	public String toString() {
		try {
			OMap m = new OMap()
				.appendSkipEmpty("name", name)
				.appendSkipEmpty("type", type)
				.appendSkipEmpty("format", format)
				.appendSkipEmpty("default", _default)
				.appendSkipEmpty("enum", _enum)
				.appendSkipEmpty("properties", properties)
				.appendSkipFalse("allowEmptyValue", allowEmptyValue)
				.appendSkipFalse("exclusiveMaximum", exclusiveMaximum)
				.appendSkipFalse("exclusiveMinimum", exclusiveMinimum)
				.appendSkipFalse("required", required)
				.appendSkipFalse("uniqueItems", uniqueItems)
				.appendSkipFalse("skipIfEmpty", skipIfEmpty)
				.appendIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat", collectionFormat)
				.appendSkipEmpty("pattern", pattern)
				.appendSkipNull("items", items)
				.appendSkipNull("additionalProperties", additionalProperties)
				.appendSkipMinusOne("maximum", maximum)
				.appendSkipMinusOne("minimum", minimum)
				.appendSkipMinusOne("multipleOf", multipleOf)
				.appendSkipMinusOne("maxLength", maxLength)
				.appendSkipMinusOne("minLength", minLength)
				.appendSkipMinusOne("maxItems", maxItems)
				.appendSkipMinusOne("minItems", minItems)
				.appendSkipMinusOne("maxProperties", maxProperties)
				.appendSkipMinusOne("minProperties", minProperties)
				.append("parsedType", parsedType)
			;
			return m.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
