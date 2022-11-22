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
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.httppart.HttpPartDataType.*;
import static org.apache.juneau.httppart.HttpPartFormat.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
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
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.OpenApiDetails">OpenAPI Details</a>
 * </ul>
 */
public class HttpPartSchema {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
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

	/**
	 * Instantiates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>BOOLEAN</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tBoolean() {
		return create().tBoolean();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>FILE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tFile() {
		return create().tFile();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tInteger() {
		return create().tInteger();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>).format(HttpPartFormat.<jsf>INT32</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tInt32() {
		return create().tInteger().fInt32();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>INTEGER</jsf>).format(HttpPartFormat.<jsf>INT64</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tInt64() {
		return create().tInteger().fInt64();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NONE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tNone() {
		return create().tNone();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tNumber() {
		return create().tNumber();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>).format(HttpPartFormat.<jsf>FLOAT</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tFloat() {
		return create().tNumber().fFloat();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>NUMBER</jsf>).format(HttpPartFormat.<jsf>DOUBLE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tDouble() {
		return create().tNumber().fDouble();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tString() {
		return create().tString();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BYTE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tByte() {
		return create().tString().fByte();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BINARY</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tBinary() {
		return create().tString().fBinary();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>BINARY_SPACED</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tBinarySpaced() {
		return create().tString().fBinarySpaced();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>DATE</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tDate() {
		return create().tString().fDate();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>DATE_TIME</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tDateTime() {
		return create().tString().fDateTime();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>STRING</jsf>).format(HttpPartFormat.<jsf>UON</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tUon() {
		return create().tString().fUon();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArray() {
		return create().tArray();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArray(Builder items) {
		return create().tArray().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArrayCsv() {
		return create().tArray().cfCsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArrayCsv(Builder items) {
		return create().tArray().cfCsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArrayPipes() {
		return create().tArray().cfPipes();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArrayPipes(Builder items) {
		return create().tArray().cfPipes().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArraySsv() {
		return create().tArray().cfSsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArraySsv(Builder items) {
		return create().tArray().cfSsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArrayTsv() {
		return create().tArray().cfTsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArrayTsv(Builder items) {
		return create().tArray().cfTsv().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UONC</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArrayUon() {
		return create().tArray().cfUon();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UONC</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArrayUon(Builder items) {
		return create().tArray().cfUon().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>MULTI</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tArrayMulti() {
		return create().tArray().cfMulti();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>ARRAY</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>MULTI</jsf>).items(items)</c>.
	 *
	 * @param items The schema of the array items.
	 * @return A new builder for this object.
	 */
	public static Builder tArrayMulti(Builder items) {
		return create().tArray().cfMulti().items(items);
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObject() {
		return create().tObject();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>CSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObjectCsv() {
		return create().tObject().cfCsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>PIPES</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObjectPipes() {
		return create().tObject().cfPipes();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>SSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObjectSsv() {
		return create().tObject().cfSsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>TSV</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObjectTsv() {
		return create().tObject().cfTsv();
	}

	/**
	 * Shortcut for <c><jsm>create</jsm>().type(HttpPartDataType.<jsf>OBJECT</jsf>).collectionFormat(HttpPartCollectionFormat.<jsf>UON</jsf>)</c>.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder tObjectUon() {
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
	 * 		<li>{@link Content}
	 * 		<li>{@link Header}
	 * 		<li>{@link Query}
	 * 		<li>{@link FormData}
	 * 		<li>{@link Path}
	 * 		<li>{@link Response}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param mpi The Java method parameter.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, ParamInfo mpi) {
		return create().applyAll(c, mpi).build();
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
	 * 		<li>{@link Content}
	 * 		<li>{@link Header}
	 * 		<li>{@link Query}
	 * 		<li>{@link FormData}
	 * 		<li>{@link Path}
	 * 		<li>{@link Response}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param m
	 * 	The Java method with the return type being checked.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, Method m) {
		return create().applyAll(c, m).build();
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
	 * 		<li>{@link Content}
	 * 		<li>{@link Header}
	 * 		<li>{@link Query}
	 * 		<li>{@link FormData}
	 * 		<li>{@link Path}
	 * 		<li>{@link Response}
	 * 		<li>{@link HasQuery}
	 * 		<li>{@link HasFormData}
	 * 	</ul>
	 * @param t
	 * 	The class containing the parameter.
	 * @return The schema information about the parameter.
	 */
	public static HttpPartSchema create(Class<? extends Annotation> c, java.lang.reflect.Type t) {
		return create().applyAll(c, t).build();
	}

	/**
	 * Shortcut for calling <c>create().type(type);</c>
	 *
	 * @param type The schema type value.
	 * @return A new builder.
	 */
	public static Builder create(String type) {
		return create().type(type);
	}

	/**
	 * Shortcut for calling <c>create().type(type).format(format);</c>
	 *
	 * @param type The schema type value.
	 * @param format The schema format value.
	 * @return A new builder.
	 */
	public static Builder create(String type, String format) {
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

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {
		String name, _default;
		Set<Integer> codes;
		Set<String> _enum;
		Boolean allowEmptyValue, exclusiveMaximum, exclusiveMinimum, required, uniqueItems, skipIfEmpty;
		HttpPartCollectionFormat collectionFormat = HttpPartCollectionFormat.NO_COLLECTION_FORMAT;
		HttpPartDataType type = HttpPartDataType.NO_TYPE;
		HttpPartFormat format = HttpPartFormat.NO_FORMAT;
		Pattern pattern;
		Number maximum, minimum, multipleOf;
		Long maxLength, minLength, maxItems, minItems, maxProperties, minProperties;
		Map<String,Object> properties;
		Object items, additionalProperties;
		boolean noValidate;
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

		Builder apply(Class<? extends Annotation> c, ParamInfo mpi) {
			apply(c, mpi.getParameterType().innerType());
			mpi.forEachDeclaredAnnotation(c, x -> true, x -> apply(x));
			return this;
		}

		Builder applyAll(Class<? extends Annotation> c, ParamInfo mpi) {
			return apply(Schema.class, mpi).apply(c, mpi);
		}

		Builder apply(Class<? extends Annotation> c, Method m) {
			apply(c, m.getGenericReturnType());
			Annotation a = m.getAnnotation(c);
			if (a != null)
				return apply(a);
			return this;
		}

		Builder applyAll(Class<? extends Annotation> c, Method m) {
			return apply(Schema.class, m).apply(c, m);
		}

		Builder apply(Class<? extends Annotation> c, java.lang.reflect.Type t) {
			if (t instanceof Class<?>) {
				ClassInfo.of((Class<?>)t).forEachAnnotation(c, x -> true, x -> apply(x));
			} else if (Value.isType(t)) {
				apply(c, Value.getParameterType(t));
			}
			return this;
		}

		Builder applyAll(Class<? extends Annotation> c, java.lang.reflect.Type t) {
			return apply(Schema.class, t).apply(c, t);
		}

		/**
		 * Apply the specified annotation to this schema.
		 *
		 * @param a The annotation to apply.
		 * @return This object.
		 */
		public Builder apply(Annotation a) {
			if (a instanceof Content)
				apply((Content)a);
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
			else if (a instanceof StatusCode)
				apply((StatusCode)a);
			else if (a instanceof HasQuery)
				apply((HasQuery)a);
			else if (a instanceof HasFormData)
				apply((HasFormData)a);
			else if (a instanceof Schema)
				apply((Schema)a);
			else
				throw new BasicRuntimeException("Builder.apply(@{0}) not defined", className(a));
			return this;
		}

		Builder apply(Content a) {
			if (! SchemaAnnotation.empty(a.schema()))
				apply(a.schema());
			return this;
		}

		Builder apply(Header a) {
			if (! SchemaAnnotation.empty(a.schema()))
				apply(a.schema());
			name(firstNonEmpty(a.name(), a.value()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(FormData a) {
			if (! SchemaAnnotation.empty(a.schema()))
				apply(a.schema());
			name(firstNonEmpty(a.name(), a.value()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(Query a) {
			if (! SchemaAnnotation.empty(a.schema()))
				apply(a.schema());
			name(firstNonEmpty(a.name(), a.value()));
			parser(a.parser());
			serializer(a.serializer());
			return this;
		}

		Builder apply(Path a) {
			if (! SchemaAnnotation.empty(a.schema()))
				apply(a.schema());
			name(firstNonEmpty(a.name(), a.value()));
			parser(a.parser());
			serializer(a.serializer());

			// Path remainder always allows empty value.
			if (startsWith(name, '/')) {
				allowEmptyValue();
				required(false);
			} else if (required == null) {
				required(true);
			}

			return this;
		}

		Builder apply(Response a) {
			allowEmptyValue(true);
			apply(a.schema());
			parser(a.parser());
			required(false);
			serializer(a.serializer());
			return this;
		}

		Builder apply(StatusCode a) {
			codes(a.value());
			return this;
		}

		Builder apply(Items a) {
			_default(joinnlOrNull(a._default(), a.df()));
			_enum(toSet(a._enum(), a.e()));
			collectionFormat(firstNonEmpty(a.collectionFormat(), a.cf()));
			exclusiveMaximum(a.exclusiveMaximum() || a.emax());
			exclusiveMinimum(a.exclusiveMinimum() || a.emin());
			format(firstNonEmpty(a.format(), a.f()));
			items(a.items());
			maximum(toNumber(a.maximum(), a.max()));
			maxItems(firstNmo(a.maxItems(), a.maxi()));
			maxLength(firstNmo(a.maxLength(), a.maxl()));
			minimum(toNumber(a.minimum(), a.min()));
			minItems(firstNmo(a.minItems(), a.mini()));
			minLength(firstNmo(a.minLength(), a.minl()));
			multipleOf(toNumber(a.multipleOf(), a.mo()));
			pattern(firstNonEmpty(a.pattern(), a.p()));
			type(firstNonEmpty(a.type(), a.t()));
			uniqueItems(a.uniqueItems() || a.ui());			return this;
		}

		Builder apply(SubItems a) {
			_default(joinnlOrNull(a._default(), a.df()));
			_enum(toSet(a._enum(), a.e()));
			collectionFormat(firstNonEmpty(a.collectionFormat(), a.cf()));
			exclusiveMaximum(a.exclusiveMaximum() || a.emax());
			exclusiveMinimum(a.exclusiveMinimum() || a.emin());
			format(firstNonEmpty(a.format(), a.f()));
			items(HttpPartSchema.toJsonMap(a.items()));
			maximum(toNumber(a.maximum(), a.max()));
			maxItems(firstNmo(a.maxItems(), a.maxi()));
			maxLength(firstNmo(a.maxLength(), a.maxl()));
			minimum(toNumber(a.minimum(), a.min()));
			minItems(firstNmo(a.minItems(), a.mini()));
			minLength(firstNmo(a.minLength(), a.minl()));
			multipleOf(toNumber(a.multipleOf(), a.mo()));
			pattern(firstNonEmpty(a.pattern(), a.p()));
			type(firstNonEmpty(a.type(), a.t()));
			uniqueItems(a.uniqueItems() || a.ui());			return this;
		}

		Builder apply(Schema a) {
			_default(joinnlOrNull(a._default(), a.df()));
			_enum(toSet(a._enum(), a.e()));
			additionalProperties(HttpPartSchema.toJsonMap(a.additionalProperties()));
			allowEmptyValue(a.allowEmptyValue() || a.aev());
			collectionFormat(firstNonEmpty(a.collectionFormat(), a.cf()));
			exclusiveMaximum(a.exclusiveMaximum() || a.emax());
			exclusiveMinimum(a.exclusiveMinimum() || a.emin());
			format(firstNonEmpty(a.format(), a.f()));
			items(a.items());
			maximum(toNumber(a.maximum(), a.max()));
			maxItems(firstNmo(a.maxItems(), a.maxi()));
			maxLength(firstNmo(a.maxLength(), a.maxl()));
			maxProperties(firstNmo(a.maxProperties(), a.maxp()));
			minimum(toNumber(a.minimum(), a.min()));
			minItems(firstNmo(a.minItems(), a.mini()));
			minLength(firstNmo(a.minLength(), a.minl()));
			minProperties(firstNmo(a.minProperties(), a.minp()));
			multipleOf(toNumber(a.multipleOf(), a.mo()));
			pattern(firstNonEmpty(a.pattern(), a.p()));
			properties(HttpPartSchema.toJsonMap(a.properties()));
			required(a.required() || a.r());
			skipIfEmpty(a.skipIfEmpty() || a.sie());
			type(firstNonEmpty(a.type(), a.t()));
			uniqueItems(a.uniqueItems() || a.ui());
			return this;
		}

		Builder apply(HasQuery a) {
			name(firstNonEmpty(a.name(), a.value()));
			return this;
		}

		Builder apply(HasFormData a) {
			name(firstNonEmpty(a.name(), a.value()));
			return this;
		}

		Builder apply(JsonMap m) {
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

				items(m.getMap("items"));
				properties(m.getMap("properties"));
				additionalProperties(m.getMap("additionalProperties"));

				apply(m.getMap("schema", null));
			}
			return this;
		}

		/**
		 * <mk>name</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			if (isNotEmpty(value))
				name = value;
			return this;
		}

		/**
		 * Synonym for {@link #name(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder n(String value) {
			return name(value);
		}

		/**
		 * <mk>httpStatusCode</mk> key.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#responsesObject">Responses</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if <jk>null</jk> or an empty array.
		 * @return This object.
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#responsesObject">Responses</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <c>0</c>.
		 * @return This object.
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder required(Boolean value) {
			required = resolve(value, required);
			return this;
		}

		/**
		 * Synonym for {@link #required(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder r(Boolean value) {
			return required(value);
		}

		/**
		 * <mk>required</mk> field.
		 *
		 * <p>
		 * Determines whether the parameter is mandatory.
		 *
		 * <p>
		 * Same as {@link #required(Boolean)} but takes in a boolean value as a string.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder required(String value) {
			required = resolve(value, required);
			return this;
		}

		/**
		 * Synonym for {@link #required(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder r(String value) {
			return required(value);
		}

		/**
		 * <mk>required</mk> field.
		 *
		 * <p>
		 * Shortcut for calling <code>required(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder required() {
			return required(true);
		}

		/**
		 * Synonym for {@link #required()}.
		 *
		 * @return This object.
		 */
		public Builder r() {
			return required();
		}

		/**
		 * <mk>type</mk> field.
		 *
		 * <p>
		 * The type of the parameter.
		 *
		 * <p>
		 * If the type is not specified, it will be auto-detected based on the parameter class type.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#securitySchemeObject">SecurityScheme</a>
		 * </ul>
		 *
		 * <ul class='values'>
		 * 	<li>
		 * 		<js>"string"</js>
		 * 		<br>Parameter must be a string or a POJO convertible from a string.
		 * 	<li>
		 * 		<js>"number"</js>
		 * 		<br>Parameter must be a number primitive or number object.
		 * 		<br>If parameter is <c>Object</c>, creates either a <c>Float</c> or <c>Double</c> depending on the size of the number.
		 * 	<li>
		 * 		<js>"integer"</js>
		 * 		<br>Parameter must be a integer/long primitive or integer/long object.
		 * 		<br>If parameter is <c>Object</c>, creates either a <c>Short</c>, <c>Integer</c>, or <c>Long</c> depending on the size of the number.
		 * 	<li>
		 * 		<js>"boolean"</js>
		 * 		<br>Parameter must be a boolean primitive or object.
		 * 	<li>
		 * 		<js>"array"</js>
		 * 		<br>Parameter must be an array or collection.
		 * 		<br>Elements must be strings or POJOs convertible from strings.
		 * 		<br>If parameter is <c>Object</c>, creates an {@link JsonList}.
		 * 	<li>
		 * 		<js>"object"</js>
		 * 		<br>Parameter must be a map or bean.
		 * 		<br>If parameter is <c>Object</c>, creates an {@link JsonMap}.
		 * 		<br>Note that this is an extension of the OpenAPI schema as Juneau allows for arbitrarily-complex POJOs to be serialized as HTTP parts.
		 * 	<li>
		 * 		<js>"file"</js>
		 * 		<br>This type is currently not supported.
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification#dataTypes">Swagger Data Types</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder type(String value) {
			try {
				if (isNotEmpty(value))
					type = HttpPartDataType.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as type value.  Valid values: {1}", value, HttpPartDataType.values());
			}
			return this;
		}

		/**
		 * Synonym for {@link #type(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder t(String value) {
			return type(value);
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.STRING)</c>.
		 *
		 * @return This object.
		 */
		public Builder tString() {
			type = HttpPartDataType.STRING;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.NUMBER)</c>.
		 *
		 * @return This object.
		 */
		public Builder tNumber() {
			type = HttpPartDataType.NUMBER;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.INTEGER)</c>.
		 *
		 * @return This object.
		 */
		public Builder tInteger() {
			type = HttpPartDataType.INTEGER;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.BOOLEAN)</c>.
		 *
		 * @return This object.
		 */
		public Builder tBoolean() {
			type = HttpPartDataType.BOOLEAN;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.ARRAY)</c>.
		 *
		 * @return This object.
		 */
		public Builder tArray() {
			type = HttpPartDataType.ARRAY;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.OBJECT)</c>.
		 *
		 * @return This object.
		 */
		public Builder tObject() {
			type = HttpPartDataType.OBJECT;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.FILE)</c>.
		 *
		 * @return This object.
		 */
		public Builder tFile() {
			type = HttpPartDataType.FILE;
			return this;
		}

		/**
		 * Shortcut for <c>type(HttpPartDataType.NO_TYPE)</c>.
		 *
		 * @return This object.
		 */
		public Builder tNone() {
			type = HttpPartDataType.NO_TYPE;
			return this;
		}

		/**
		 * <mk>type</mk> field.
		 *
		 * <p>
		 * The type of the parameter.
		 *
		 * <p>
		 * If the type is not specified, it will be auto-detected based on the parameter class type.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#securitySchemeObject">SecurityScheme</a>
		 * </ul>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link HttpPartDataType}
		 * 	<ul>
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#STRING STRING}
		 * 			<br>Parameter must be a string or a POJO convertible from a string.
		 * 			<li>
		 * 			{@link HttpPartDataType#NUMBER NUMBER}
		 * 			<br>Parameter must be a number primitive or number object.
		 * 			<br>If parameter is <c>Object</c>, creates either a <c>Float</c> or <c>Double</c> depending on the size of the number.
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#INTEGER INTEGER}
		 * 			<br>Parameter must be a integer/long primitive or integer/long object.
		 * 			<br>If parameter is <c>Object</c>, creates either a <c>Short</c>, <c>Integer</c>, or <c>Long</c> depending on the size of the number.
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#BOOLEAN BOOLEAN}
		 * 			<br>Parameter must be a boolean primitive or object.
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#ARRAY ARRAY}
		 * 			<br>Parameter must be an array or collection.
		 * 			<br>Elements must be strings or POJOs convertible from strings.
		 * 			<br>If parameter is <c>Object</c>, creates an {@link JsonList}.
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#OBJECT OBJECT}
		 * 			<br>Parameter must be a map or bean.
		 * 			<br>If parameter is <c>Object</c>, creates an {@link JsonMap}.
		 * 			<br>Note that this is an extension of the OpenAPI schema as Juneau allows for arbitrarily-complex POJOs to be serialized as HTTP parts.
		 * 		<li class='jf'>
		 * 			{@link HttpPartDataType#FILE FILE}
		 * 			<br>This type is currently not supported.
		 * 	</ul>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification#dataTypes">Swagger Data Types</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder type(HttpPartDataType value) {
			this.type = value;
			return this;
		}

		/**
		 * Synonym for {@link #type(HttpPartDataType)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder t(HttpPartDataType value) {
			return type(value);
		}

		/**
		 * <mk>format</mk> field.
		 *
		 * <p>
		 * The extending format for the previously mentioned <a class="doclink" href="https://swagger.io/specification/v2#parameterType">parameter type</a>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * <ul class='values'>
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
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or an empty string.
		 * @return This object.
		 */
		public Builder format(String value) {
			try {
				if (isNotEmpty(value))
					format = HttpPartFormat.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as format value.  Valid values: {1}", value, HttpPartFormat.values());
			}
			return this;
		}

		/**
		 * Synonym for {@link #format(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder f(String value) {
			return format(value);
		}

		/**
		 * <mk>format</mk> field.
		 *
		 * <p>
		 * The extending format for the previously mentioned <a class="doclink" href="https://swagger.io/specification/v2#parameterType">parameter type</a>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * <ul class='values javatree'>
		 * 	<ul class='jc'>{@link HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#INT32 INT32} - Signed 32 bits.
		 * 			<br>Only valid with type <js>"integer"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#INT64 INT64} - Signed 64 bits.
		 * 			<br>Only valid with type <js>"integer"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
		 * 			<br>Only valid with type <js>"number"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
		 * 			<br>Only valid with type <js>"number"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
		 * 			<br>Only valid with type <js>"string"</js>.
		 * 			<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 * 			<br>Only valid with type <js>"string"</js>.
		 * 			<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Hexadecimal encoded octets, spaced (e.g. <js>"00 FF"</js>).
		 * 			<br>Only valid with type <js>"string"</js>.
		 * 			<br>Parameters of type POJO convertible from string are converted after the string has been decoded.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 * 			<br>Only valid with type <js>"string"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 * 			<br>Only valid with type <js>"string"</js>.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
		 * 			<br>This format does not affect the serialization or parsing of the parameter.
		 * 		<li class='jf'>
		 * 			{@link HttpPartFormat#UON UON} - UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>).
		 * 			<br>Only valid with type <js>"object"</js>.
		 * 			<br>If not specified, then the input is interpreted as plain-text and is converted to a POJO directly.
		 * 	</ul>
		 * </ul>
		 *
		 * <h5 class='section'>See Also:</h5><ul>
		 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder format(HttpPartFormat value) {
			format = value;
			return this;
		}

		/**
		 * Synonym for {@link #format(HttpPartFormat)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder f(HttpPartFormat value) {
			return format(value);
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.INT32)</c>.
		 *
		 * @return This object.
		 */
		public Builder fInt32() {
			format = HttpPartFormat.INT32;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.INT64)</c>.
		 *
		 * @return This object.
		 */
		public Builder fInt64() {
			format = HttpPartFormat.INT64;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.FLOAT)</c>.
		 *
		 * @return This object.
		 */
		public Builder fFloat() {
			format = HttpPartFormat.FLOAT;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.DOUBLE)</c>.
		 *
		 * @return This object.
		 */
		public Builder fDouble() {
			format = HttpPartFormat.DOUBLE;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.BYTE)</c>.
		 *
		 * @return This object.
		 */
		public Builder fByte() {
			format = HttpPartFormat.BYTE;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.BINARY)</c>.
		 *
		 * @return This object.
		 */
		public Builder fBinary() {
			format = HttpPartFormat.BINARY;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.BINARY_SPACED)</c>.
		 *
		 * @return This object.
		 */
		public Builder fBinarySpaced() {
			format = HttpPartFormat.BINARY_SPACED;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.DATE)</c>.
		 *
		 * @return This object.
		 */
		public Builder fDate() {
			format = HttpPartFormat.DATE;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.DATE_TIME)</c>.
		 *
		 * @return This object.
		 */
		public Builder fDateTime() {
			format = HttpPartFormat.DATE_TIME;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.PASSWORD)</c>.
		 *
		 * @return This object.
		 */
		public Builder fPassword() {
			format = HttpPartFormat.PASSWORD;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.UON)</c>.
		 *
		 * @return This object.
		 */
		public Builder fUon() {
			format = HttpPartFormat.UON;
			return this;
		}

		/**
		 * Shortcut for <c>format(HttpPartFormat.NO_FORMAT)</c>.
		 *
		 * @return This object.
		 */
		public Builder fNone() {
			format = HttpPartFormat.NO_FORMAT;
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder allowEmptyValue(Boolean value) {
			allowEmptyValue = resolve(value, allowEmptyValue);
			return this;
		}

		/**
		 * Synonym for {@link #allowEmptyValue(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder aev(Boolean value) {
			return allowEmptyValue(value);
		}

		/**
		 * <mk>allowEmptyValue</mk> field.
		 *
		 * <p>
		 * Same as {@link #allowEmptyValue(Boolean)} but takes in a string boolean value.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder allowEmptyValue(String value) {
			allowEmptyValue = resolve(value, allowEmptyValue);
			return this;
		}

		/**
		 * Synonym for {@link #allowEmptyValue(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder aev(String value) {
			return allowEmptyValue(value);
		}

		/**
		 * <mk>allowEmptyValue</mk> field.
		 *
		 * <p>
		 * Shortcut for calling <code>allowEmptyValue(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder allowEmptyValue() {
			return allowEmptyValue(true);
		}

		/**
		 * Synonym for {@link #allowEmptyValue()}.
		 *
		 * @return This object.
		 */
		public Builder aev() {
			return allowEmptyValue(true);
		}

		/**
		 * <mk>items</mk> field.
		 *
		 * <p>
		 * Describes the type of items in the array.
		 * <p>
		 * Required if <c>type</c> is <js>"array"</js>.
		 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder items(Builder value) {
			if (value != null)
				this.items = value;
			return this;
		}

		/**
		 * Synonym for {@link #items(HttpPartSchema.Builder)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder i(Builder value) {
			return items(value);
		}

		/**
		 * <mk>items</mk> field.
		 *
		 * <p>
		 * Describes the type of items in the array.
		 * <p>
		 * Required if <c>type</c> is <js>"array"</js>.
		 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder items(HttpPartSchema value) {
			if (value != null)
				this.items = value;
			return this;
		}

		/**
		 * Synonym for {@link #items(HttpPartSchema)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder i(HttpPartSchema value) {
			return items(value);
		}

		Builder items(JsonMap value) {
			if (value != null && ! value.isEmpty())
				items = HttpPartSchema.create().apply(value);
			return this;
		}

		Builder items(Items value) {
			if (! ItemsAnnotation.empty(value))
				items = HttpPartSchema.create().apply(value);
			return this;
		}

		Builder items(SubItems value) {
			if (! SubItemsAnnotation.empty(value))
				items = HttpPartSchema.create().apply(value);
			return this;
		}


		/**
		 * <mk>collectionFormat</mk> field.
		 *
		 * <p>
		 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
		 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * <p>
		 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
		 *
		 * <ul class='values'>
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
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder collectionFormat(String value) {
			try {
				if (isNotEmpty(value))
					this.collectionFormat = HttpPartCollectionFormat.fromString(value);
			} catch (Exception e) {
				throw new ContextRuntimeException("Invalid value ''{0}'' passed in as collectionFormat value.  Valid values: {1}", value, HttpPartCollectionFormat.values());
			}
			return this;
		}

		/**
		 * Synonym for {@link #collectionFormat(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder cf(String value) {
			return collectionFormat(value);
		}

		/**
		 * <mk>collectionFormat</mk> field.
		 *
		 * <p>
		 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
		 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * <p>
		 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
		 *
		 * <ul class='values javatree'>
		 * 	<ul class='jc'>{@link HttpPartCollectionFormat}
		 * 	<ul>
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#CSV CSV} (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
		 * 		<li>
		 * 			{@link HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
		 * 	</ul>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(HttpPartCollectionFormat value) {
			collectionFormat = value;
			return this;
		}

		/**
		 * Synonym for {@link #collectionFormat(HttpPartCollectionFormat)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder cf(HttpPartCollectionFormat value) {
			return collectionFormat(value);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.CSV)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfCsv() {
			return collectionFormat(HttpPartCollectionFormat.CSV);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.SSV)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfSsv() {
			return collectionFormat(HttpPartCollectionFormat.SSV);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.TSV)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfTsv() {
			return collectionFormat(HttpPartCollectionFormat.TSV);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.PIPES)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfPipes() {
			return collectionFormat(HttpPartCollectionFormat.PIPES);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.MULTI)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfMulti() {
			return collectionFormat(HttpPartCollectionFormat.MULTI);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.UONC)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfUon() {
			return collectionFormat(HttpPartCollectionFormat.UONC);
		}

		/**
		 * Shortcut for <c>collectionFormat(HttpPartCollectionFormat.NO_COLLECTION_FORMAT)</c>.
		 *
		 * @return This object.
		 */
		public Builder cfNone() {
			return collectionFormat(HttpPartCollectionFormat.NO_COLLECTION_FORMAT);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder _default(String value) {
			if (value != null)
				this._default = value;
			return this;
		}

		/**
		 * Synonym for {@link #_default(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder df(String value) {
			return _default(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder maximum(Number value) {
			if (value != null)
				this.maximum = value;
			return this;
		}

		/**
		 * Synonym for {@link #maximum(Number)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder max(Number value) {
			return maximum(value);
		}

		/**
		 * <mk>exclusiveMaximum</mk> field.
		 *
		 * <p>
		 * Defines whether the maximum is matched exclusively.
		 *
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * <br>If <jk>true</jk>, must be accompanied with <c>maximum</c>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(Boolean value) {
			exclusiveMaximum = resolve(value, exclusiveMaximum);
			return this;
		}

		/**
		 * Synonym for {@link #exclusiveMaximum(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder emax(Boolean value) {
			return exclusiveMaximum(value);
		}

		/**
		 * <mk>exclusiveMaximum</mk> field.
		 *
		 * <p>
		 * Same as {@link #exclusiveMaximum(Boolean)} but takes in a string boolean value.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder exclusiveMaximum(String value) {
			exclusiveMaximum = resolve(value, exclusiveMaximum);
			return this;
		}

		/**
		 * Synonym for {@link #exclusiveMaximum(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder emax(String value) {
			return exclusiveMaximum(value);
		}

		/**
		 * <mk>exclusiveMaximum</mk> field.
		 *
		 * <p>
		 * Shortcut for calling <code>exclusiveMaximum(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder exclusiveMaximum() {
			return exclusiveMaximum(true);
		}

		/**
		 * Synonym for {@link #exclusiveMaximum()}.
		 *
		 * @return This object.
		 */
		public Builder emax() {
			return exclusiveMaximum();
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder minimum(Number value) {
			if (value != null)
				this.minimum = value;
			return this;
		}

		/**
		 * Synonym for {@link #minimum(Number)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder min(Number value) {
			return minimum(value);
		}

		/**
		 * <mk>exclusiveMinimum</mk> field.
		 *
		 * <p>
		 * Defines whether the minimum is matched exclusively.
		 *
		 * <p>
		 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
		 * <br>If <jk>true</jk>, must be accompanied with <c>minimum</c>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(Boolean value) {
			exclusiveMinimum = resolve(value, exclusiveMinimum);
			return this;
		}

		/**
		 * Synonym for {@link #exclusiveMinimum(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder emin(Boolean value) {
			return exclusiveMinimum(value);
		}

		/**
		 * <mk>exclusiveMinimum</mk> field.
		 *
		 * <p>
		 * Same as {@link #exclusiveMinimum(Boolean)} but takes in a string boolean value.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder exclusiveMinimum(String value) {
			exclusiveMinimum = resolve(value, exclusiveMinimum);
			return this;
		}

		/**
		 * Synonym for {@link #exclusiveMinimum(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder emin(String value) {
			return exclusiveMinimum(value);
		}

		/**
		 * <mk>exclusiveMinimum</mk> field.
		 *
		 * <p>
		 * Shortcut for calling <code>exclusiveMinimum(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder exclusiveMinimum() {
			return exclusiveMinimum(true);
		}

		/**
		 * Synonym for {@link #exclusiveMinimum()}.
		 *
		 * @return This object.
		 */
		public Builder emin() {
			return exclusiveMinimum();
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or <c>-1</c>.
		 * @return This object.
		 */
		public Builder maxLength(Long value) {
			maxLength = resolve(value, maxLength);
			return this;
		}

		/**
		 * Synonym for {@link #maxLength(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(Long value) {
			return maxLength(value);
		}

		/**
		 * <mk>maxLength</mk> field.
		 *
		 * <p>
		 * Same as {@link #maxLength(Long)} but takes in a string number.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder maxLength(String value) {
			maxLength = resolve(value, maxLength);
			return this;
		}

		/**
		 * Synonym for {@link #maxLength(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxl(String value) {
			return maxLength(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or <c>-1</c>.
		 * @return This object.
		 */
		public Builder minLength(Long value) {
			minLength = resolve(value, minLength);
			return this;
		}

		/**
		 * Synonym for {@link #minLength(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder minl(Long value) {
			return minLength(value);
		}

		/**
		 * <mk>minLength</mk> field.
		 *
		 * <p>
		 * Same as {@link #minLength(Long)} but takes in a string number.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder minLength(String value) {
			minLength = resolve(value, minLength);
			return this;
		}

		/**
		 * Synonym for {@link #minLength(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder minl(String value) {
			return minLength(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
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
		 * Synonym for {@link #pattern(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder p(String value) {
			return pattern(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or <c>-1</c>.
		 * @return This object.
		 */
		public Builder maxItems(Long value) {
			maxItems = resolve(value, maxItems);
			return this;
		}

		/**
		 * Synonym for {@link #maxItems(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(Long value) {
			return maxItems(value);
		}

		/**
		 * <mk>maxItems</mk> field.
		 *
		 * <p>
		 * Same as {@link #maxItems(Long)} but takes in a string number.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder maxItems(String value) {
			maxItems = resolve(value, maxItems);
			return this;
		}

		/**
		 * Synonym for {@link #maxItems(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxi(String value) {
			return maxItems(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or <c>-1</c>.
		 * @return This object.
		 */
		public Builder minItems(Long value) {
			minItems = resolve(value, minItems);
			return this;
		}

		/**
		 * Synonym for {@link #minItems(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder mini(Long value) {
			return minItems(value);
		}

		/**
		 * <mk>minItems</mk> field.
		 *
		 * <p>
		 * Same as {@link #minItems(Long)} but takes in a string number.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder minItems(String value) {
			minItems = resolve(value, minItems);
			return this;
		}

		/**
		 * Synonym for {@link #minItems(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder mini(String value) {
			return minItems(value);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder uniqueItems(Boolean value) {
			uniqueItems = resolve(value, uniqueItems);
			return this;
		}

		/**
		 * Synonym for {@link #uniqueItems(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder ui(Boolean value) {
			return uniqueItems(value);
		}

		/**
		 * <mk>uniqueItems</mk> field.
		 *
		 * <p>
		 * Same as {@link #uniqueItems(Boolean)} but takes in a string boolean.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty..
		 * @return This object.
		 */
		public Builder uniqueItems(String value) {
			uniqueItems = resolve(value, uniqueItems);
			return this;
		}

		/**
		 * Synonym for {@link #uniqueItems(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder ui(String value) {
			return uniqueItems(value);
		}

		/**
		 * <mk>uniqueItems</mk> field.
		 *
		 * <p>
		 * Shortcut for calling <code>uniqueItems(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder uniqueItems() {
			return uniqueItems(true);
		}

		/**
		 * Synonym for {@link #uniqueItems()}.
		 *
		 * @return This object.
		 */
		public Builder ui() {
			return uniqueItems();
		}

		/**
		 * <mk>skipIfEmpty</mk> field.
		 *
		 * <p>
		 * Identifies whether an item should be skipped during serialization if it's empty.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder skipIfEmpty(Boolean value) {
			skipIfEmpty = resolve(value, skipIfEmpty);
			return this;
		}

		/**
		 * Synonym for {@link #skipIfEmpty(Boolean)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder sie(Boolean value) {
			return skipIfEmpty(value);
		}

		/**
		 * <mk>skipIfEmpty</mk> field.
		 *
		 * <p>
		 * Same as {@link #skipIfEmpty(Boolean)} but takes in a string boolean.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder skipIfEmpty(String value) {
			skipIfEmpty = resolve(value, skipIfEmpty);
			return this;
		}

		/**
		 * Synonym for {@link #skipIfEmpty(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder sie(String value) {
			return skipIfEmpty(value);
		}

		/**
		 * Identifies whether an item should be skipped if it's empty.
		 *
		 * <p>
		 * Shortcut for calling <code>skipIfEmpty(<jk>true</jk>);</code>.
		 *
		 * @return This object.
		 */
		public Builder skipIfEmpty() {
			return skipIfEmpty(true);
		}

		/**
		 * Synonym for {@link #skipIfEmpty()}.
		 *
		 * @return This object.
		 */
		public Builder sie() {
			return skipIfEmpty();
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or an empty set.
		 * @return This object.
		 */
		public Builder _enum(Set<String> value) {
			if (value != null && ! value.isEmpty())
				this._enum = value;
			return this;
		}

		/**
		 * Synonym for {@link #_enum(Set)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder e(Set<String> value) {
			return _enum(value);
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
		 * @return This object.
		 */
		public Builder _enum(String...values) {
			return _enum(set(values));
		}

		/**
		 * Synonym for {@link #_enum(String...)}.
		 *
		 * @param values
		 * 	The new values for this property.
		 * @return This object.
		 */
		public Builder e(String...values) {
			return _enum(values);
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
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#parameterObject">Parameter</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#itemsObject">Items</a>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#headerObject">Header</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder multipleOf(Number value) {
			if (value != null)
				this.multipleOf = value;
			return this;
		}

		/**
		 * Synonym for {@link #multipleOf(Number)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder mo(Number value) {
			return multipleOf(value);
		}

		/**
		 * <mk>mapProperties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or <c>-1</c>.
		 * @return This object.
		 */
		public Builder maxProperties(Long value) {
			maxProperties = resolve(value, maxProperties);
			return this;
		}

		/**
		 * Synonym for {@link #maxProperties(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxp(Long value) {
			return maxProperties(value);
		}

		/**
		 * <mk>mapProperties</mk> field.
		 *
		 * <p>
		 * Same as {@link #maxProperties(Long)} but takes in a string number.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder maxProperties(String value) {
			maxProperties = resolve(value, maxProperties);
			return this;
		}

		/**
		 * Synonym for {@link #maxProperties(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder maxp(String value) {
			return maxProperties(value);
		}

		/**
		 * <mk>minProperties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder minProperties(Long value) {
			minProperties = resolve(value, minProperties);
			return this;
		}

		/**
		 * Synonym for {@link #minProperties(Long)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder minp(Long value) {
			return minProperties(value);
		}

		/**
		 * <mk>minProperties</mk> field.
		 *
		 * <p>
		 * Same as {@link #minProperties(Long)} but takes in a string boolean.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder minProperties(String value) {
			minProperties = resolve(value, minProperties);
			return this;
		}

		/**
		 * Synonym for {@link #minProperties(String)}.
		 *
		 * @param value
		 * 	The new value for this property.
		 * @return This object.
		 */
		public Builder minp(String value) {
			return minProperties(value);
		}

		/**
		 * <mk>properties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param key
		 *	The property name.
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder property(String key, Builder value) {
			if ( key != null && value != null) {
				if (properties == null)
					properties = map();
				properties.put(key, value);
			}
			return this;
		}

		/**
		 * <mk>properties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param key
		 *	The property name.
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder property(String key, HttpPartSchema value) {
			if ( key != null && value != null) {
				if (properties == null)
					properties = map();
				properties.put(key, value);
			}
			return this;
		}

		/**
		 * Shortcut for <c>property(key, value)</c>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param key
		 *	The property name.
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder p(String key, Builder value) {
			return property(key, value);
		}

		/**
		 * Shortcut for <c>property(key, value)</c>.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param key
		 *	The property name.
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk>.
		 * @return This object.
		 */
		public Builder p(String key, HttpPartSchema value) {
			return property(key, value);
		}

		private Builder properties(JsonMap value) {
			if (value != null)
				value.forEach((k,v) -> property(k, HttpPartSchema.create().apply((JsonMap)v)));
			return this;
		}

		/**
		 * <mk>additionalProperties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder additionalProperties(Builder value) {
			if (value != null)
				additionalProperties = value;
			return this;
		}

		/**
		 * <mk>additionalProperties</mk> field.
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder additionalProperties(HttpPartSchema value) {
			if (value != null)
				additionalProperties = value;
			return this;
		}

		/**
		 * Shortcut for <c>additionalProperties(value)</c>
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder ap(Builder value) {
			return additionalProperties(value);
		}

		/**
		 * Shortcut for <c>additionalProperties(value)</c>
		 *
		 * <p>
		 * Applicable to the following Swagger schema objects:
		 * <ul>
		 * 	<li><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Schema</a>
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or empty.
		 * @return This object.
		 */
		public Builder ap(HttpPartSchema value) {
			return additionalProperties(value);
		}

		private Builder additionalProperties(JsonMap value) {
			if (value != null && ! value.isEmpty())
				additionalProperties = HttpPartSchema.create().apply(value);
			return this;
		}

		/**
		 * Identifies the part serializer to use for serializing this part.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or {@link HttpPartSerializer.Void}.
		 * @return This object.
		 */
		public Builder serializer(Class<? extends HttpPartSerializer> value) {
			if (isNotVoid(value))
				serializer = value;
			return this;
		}

		/**
		 * Identifies the part parser to use for parsing this part.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>Ignored if value is <jk>null</jk> or {@link HttpPartParser.Void}.
		 * @return This object.
		 */
		public Builder parser(Class<? extends HttpPartParser> value) {
			if (isNotVoid(value))
				parser = value;
			return this;
		}

		/**
		 * Disables Swagger schema usage validation checking.
		 *
		 * @param value Specify <jk>true</jk> to prevent {@link ContextRuntimeException} from being thrown if invalid Swagger usage was detected.
		 * @return This object.
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
		 * @return This object.
		 */
		public Builder noValidate() {
			return noValidate(true);
		}

		private Boolean resolve(String newValue, Boolean oldValue) {
			return isEmpty(newValue) ? oldValue : Boolean.valueOf(newValue);
		}

		private Boolean resolve(Boolean newValue, Boolean oldValue) {
			return newValue == null ? oldValue : newValue;
		}

		private Long resolve(String newValue, Long oldValue) {
			return isEmpty(newValue) ? oldValue : Long.parseLong(newValue);
		}

		private Long resolve(Long newValue, Long oldValue) {
			return (newValue == null || newValue == -1) ? oldValue : newValue;
		}

		private Set<String> toSet(String[]...s) {
			return HttpPartSchema.toSet(s);
		}

		private Number toNumber(String...s) {
			return HttpPartSchema.toNumber(s);
		}

		private Long firstNmo(Long...l) {
			for (Long ll : l)
				if (ll != null && ll != -1)
					return ll;
			return null;
		}

		private String joinnlOrNull(String[]...s) {
			for (String[] ss : s)
				if (ss.length > 0)
					return joinnl(ss);
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

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

	HttpPartSchema(Builder b) {
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
		List<String> errors = list();
		ListBuilder<String> notAllowed = listBuilder(String.class);
		boolean invalidFormat = false;
		switch (type) {
			case STRING: {
				notAllowed
					.addIf(properties != null, "properties")
					.addIf(additionalProperties != null, "additionalProperties")
					.addIf(exclusiveMaximum, "exclusiveMaximum")
					.addIf(exclusiveMinimum, "exclusiveMinimum")
					.addIf(uniqueItems, "uniqueItems")
					.addIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat")
					.addIf(items != null, "items")
					.addIf(maximum != null, "maximum")
					.addIf(minimum != null, "minimum")
					.addIf(multipleOf != null, "multipleOf")
					.addIf(maxItems != null, "maxItems")
					.addIf(minItems != null, "minItems")
					.addIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.BYTE, HttpPartFormat.BINARY, HttpPartFormat.BINARY_SPACED, HttpPartFormat.DATE, HttpPartFormat.DATE_TIME, HttpPartFormat.PASSWORD, HttpPartFormat.UON, HttpPartFormat.NO_FORMAT);
				break;
			}
			case ARRAY: {
				notAllowed.addIf(properties != null, "properties")
					.addIf(additionalProperties != null, "additionalProperties")
					.addIf(exclusiveMaximum, "exclusiveMaximum")
					.addIf(exclusiveMinimum, "exclusiveMinimum")
					.addIf(pattern != null, "pattern")
					.addIf(maximum != null, "maximum")
					.addIf(minimum != null, "minimum")
					.addIf(multipleOf != null, "multipleOf")
					.addIf(maxLength != null, "maxLength")
					.addIf(minLength != null, "minLength")
					.addIf(maxProperties != null, "maxProperties")
					.addIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON);
				break;
			}
			case BOOLEAN: {
				notAllowed.addIf(! _enum.isEmpty(), "_enum")
					.addIf(properties != null, "properties")
					.addIf(additionalProperties != null, "additionalProperties")
					.addIf(exclusiveMaximum, "exclusiveMaximum")
					.addIf(exclusiveMinimum, "exclusiveMinimum")
					.addIf(uniqueItems, "uniqueItems")
					.addIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat")
					.addIf(pattern != null, "pattern")
					.addIf(items != null, "items")
					.addIf(maximum != null, "maximum")
					.addIf(minimum != null, "minimum")
					.addIf(multipleOf != null, "multipleOf")
					.addIf(maxItems != null, "maxItems")
					.addIf(maxLength != null, "maxLength")
					.addIf(maxProperties != null, "maxProperties")
					.addIf(minItems != null, "minItems")
					.addIf(minLength != null, "minLength")
					.addIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON);
				break;
			}
			case FILE: {
				break;
			}
			case INTEGER: {
				notAllowed.addIf(properties != null, "properties")
					.addIf(additionalProperties != null, "additionalProperties")
					.addIf(uniqueItems, "uniqueItems")
					.addIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat")
					.addIf(pattern != null, "pattern")
					.addIf(items != null, "items")
					.addIf(maxItems != null, "maxItems")
					.addIf(maxLength != null, "maxLength")
					.addIf(maxProperties != null, "maxProperties")
					.addIf(minItems != null, "minItems")
					.addIf(minLength != null, "minLength")
					.addIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON, HttpPartFormat.INT32, HttpPartFormat.INT64);
				break;
			}
			case NUMBER: {
				notAllowed.addIf(properties != null, "properties")
					.addIf(additionalProperties != null, "additionalProperties")
					.addIf(uniqueItems, "uniqueItems")
					.addIf(collectionFormat != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat")
					.addIf(pattern != null, "pattern")
					.addIf(items != null, "items")
					.addIf(maxItems != null, "maxItems")
					.addIf(maxLength != null, "maxLength")
					.addIf(maxProperties != null, "maxProperties")
					.addIf(minItems != null, "minItems")
					.addIf(minLength != null, "minLength")
					.addIf(minProperties != null, "minProperties");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT, HttpPartFormat.UON, HttpPartFormat.FLOAT, HttpPartFormat.DOUBLE);
				break;
			}
			case OBJECT: {
				notAllowed.addIf(exclusiveMaximum, "exclusiveMaximum")
					.addIf(exclusiveMinimum, "exclusiveMinimum")
					.addIf(uniqueItems, "uniqueItems")
					.addIf(pattern != null, "pattern")
					.addIf(items != null, "items")
					.addIf(maximum != null, "maximum")
					.addIf(minimum != null, "minimum")
					.addIf(multipleOf != null, "multipleOf")
					.addIf(maxItems != null, "maxItems")
					.addIf(maxLength != null, "maxLength")
					.addIf(minItems != null, "minItems")
					.addIf(minLength != null, "minLength");
				invalidFormat = ! format.isOneOf(HttpPartFormat.NO_FORMAT);
				break;
			}
			default:
				break;
		}

		List<String> notAllowed2 = notAllowed.build();
		if (! notAllowed2.isEmpty())
			errors.add("Attributes not allow for type='"+type+"': " + StringUtils.join(notAllowed2, ","));
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
	 * @see HttpPartSchema.Builder#name(String)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the <c>type</c> field of this schema.
	 *
	 * @return The <c>type</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#type(String)
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
	 * @see HttpPartSchema.Builder#format(String)
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
	 * @see HttpPartSchema.Builder#_default(String)
	 */
	public String getDefault() {
		return _default;
	}

	/**
	 * Returns the <c>collectionFormat</c> field of this schema.
	 *
	 * @return The <c>collectionFormat</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#collectionFormat(String)
	 */
	public HttpPartCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the <c>format</c> field of this schema.
	 *
	 * @see HttpPartSchema.Builder#format(String)
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
	 * @see HttpPartSchema.Builder#format(String)
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
	 * @see HttpPartSchema.Builder#items(HttpPartSchema.Builder)
	 */
	public HttpPartSchema getItems() {
		return items;
	}

	/**
	 * Returns the <c>maximum</c> field of this schema.
	 *
	 * @return The <c>maximum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#maximum(Number)
	 */
	public Number getMaximum() {
		return maximum;
	}

	/**
	 * Returns the <c>minimum</c> field of this schema.
	 *
	 * @return The <c>minimum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#minimum(Number)
	 */
	public Number getMinimum() {
		return minimum;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#multipleOf(Number)
	 */
	public Number getMultipleOf() {
		return multipleOf;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#pattern(String)
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#maxLength(Long)
	 */
	public Long getMaxLength() {
		return maxLength;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#minLength(Long)
	 */
	public Long getMinLength() {
		return minLength;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#maxItems(Long)
	 */
	public Long getMaxItems() {
		return maxItems;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#minItems(Long)
	 */
	public Long getMinItems() {
		return minItems;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#maxProperties(Long)
	 */
	public Long getMaxProperties() {
		return maxProperties;
	}

	/**
	 * Returns the <c>xxx</c> field of this schema.
	 *
	 * @return The <c>xxx</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#minProperties(Long)
	 */
	public Long getMinProperties() {
		return minProperties;
	}

	/**
	 * Returns the <c>exclusiveMaximum</c> field of this schema.
	 *
	 * @return The <c>exclusiveMaximum</c> field of this schema.
	 * @see HttpPartSchema.Builder#exclusiveMaximum(Boolean)
	 */
	public boolean isExclusiveMaximum() {
		return exclusiveMaximum;
	}

	/**
	 * Returns the <c>exclusiveMinimum</c> field of this schema.
	 *
	 * @return The <c>exclusiveMinimum</c> field of this schema.
	 * @see HttpPartSchema.Builder#exclusiveMinimum(Boolean)
	 */
	public boolean isExclusiveMinimum() {
		return exclusiveMinimum;
	}

	/**
	 * Returns the <c>uniqueItems</c> field of this schema.
	 *
	 * @return The <c>uniqueItems</c> field of this schema.
	 * @see HttpPartSchema.Builder#uniqueItems(Boolean)
	 */
	public boolean isUniqueItems() {
		return uniqueItems;
	}

	/**
	 * Returns the <c>required</c> field of this schema.
	 *
	 * @return The <c>required</c> field of this schema.
	 * @see HttpPartSchema.Builder#required(Boolean)
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * Returns the <c>skipIfEmpty</c> field of this schema.
	 *
	 * @return The <c>skipIfEmpty</c> field of this schema.
	 * @see HttpPartSchema.Builder#skipIfEmpty(Boolean)
	 */
	public boolean isSkipIfEmpty() {
		return skipIfEmpty;
	}

	/**
	 * Returns the <c>allowEmptyValue</c> field of this schema.
	 *
	 * @return The <c>skipIfEmpty</c> field of this schema.
	 * @see HttpPartSchema.Builder#skipIfEmpty(Boolean)
	 */
	public boolean isAllowEmptyValue() {
		return allowEmptyValue;
	}

	/**
	 * Returns the <c>enum</c> field of this schema.
	 *
	 * @return The <c>enum</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#_enum(Set)
	 */
	public Set<String> getEnum() {
		return _enum;
	}

	/**
	 * Returns the <c>parser</c> field of this schema.
	 *
	 * @return The <c>parser</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#parser(Class)
	 */
	public Class<? extends HttpPartParser> getParser() {
		return parser;
	}

	/**
	 * Returns the <c>serializer</c> field of this schema.
	 *
	 * @return The <c>serializer</c> field of this schema, or <jk>null</jk> if not specified.
	 * @see HttpPartSchema.Builder#serializer(Class)
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
	 * @param <T> The return type.
	 * @param o The parsed output.
	 * @param bc The bean context used to detect POJO types.
	 * @return The same object passed in.
	 * @throws SchemaValidationException if the specified parsed output does not validate against this schema.
	 */
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
						c.forEach(x -> items.validateOutput(x, bc));
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
					Map<?,?> m = cm.isMap() ? (Map<?,?>)o : bc.toBeanMap(o);
					if (! isValidMinProperties(m))
						throw new SchemaValidationException("Minimum number of properties not met.");
					if (! isValidMaxProperties(m))
						throw new SchemaValidationException("Maximum number of properties exceeded.");
					m.forEach((k,v) -> {
						String key = k.toString();
						HttpPartSchema s2 = getProperty(key);
						if (s2 != null)
							s2.validateOutput(v, bc);
					});
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
		return in == null ? emptySet() : unmodifiable(copyOf(in));
	}

	private static Map<String,HttpPartSchema> build(Map<String,Object> in, boolean noValidate) {
		if (in == null)
			return null;
		Map<String,HttpPartSchema> m = map();
		in.forEach((k,v) -> m.put(k, build(v, noValidate)));
		return unmodifiable(m);
	}

	private static HttpPartSchema build(Object in, boolean noValidate) {
		if (in == null)
			return null;
		if (in instanceof HttpPartSchema)
			return (HttpPartSchema)in;
		return ((Builder)in).noValidate(noValidate).build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private boolean resolve(Boolean b) {
		return b == null ? false : b;
	}

	final static Set<String> toSet(String[]...s) {
		boolean isNotEmpty = false;
		for (String[] ss : s)
			isNotEmpty |= ss.length > 0;
		if (! isNotEmpty)
			return null;
		Set<String> set = set();
		for (String[] ss : s)
			if (ss != null)
				for (String ss2 : ss)
					split(ss2, x -> set.add(x));
		return set.isEmpty() ? null : set;
	}

	final static Set<String> toSet(String s) {
		if (isEmpty(s))
			return null;
		Set<String> set = set();
		try {
			JsonList.ofJsonOrCdl(s).forEach(x -> set.add(x.toString()));
		} catch (ParseException e) {
			throw asRuntimeException(e);
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
			throw asRuntimeException(e);
		}
	}

	final static JsonMap toJsonMap(String[] ss) {
		String s = joinnl(ss);
		if (s.isEmpty())
			return null;
		if (! isJsonObject(s, true))
			s = "{" + s + "}";
		try {
			return JsonMap.ofJson(s);
		} catch (ParseException e) {
			throw asRuntimeException(e);
		}
	}

	@Override
	public String toString() {
		try {
			Predicate<Object> ne = x -> isNotEmpty(stringify(x));
			Predicate<Boolean> nf = ObjectUtils::isTrue;
			Predicate<Number> nm1 = ObjectUtils::isNotMinusOne;
			Predicate<Object> nn = ObjectUtils::isNotNull;
			JsonMap m = new JsonMap()
				.appendIf(ne, "name", name)
				.appendIf(ne, "type", type)
				.appendIf(ne, "format", format)
				.appendIf(ne, "default", _default)
				.appendIf(ne, "enum", _enum)
				.appendIf(ne, "properties", properties)
				.appendIf(nf, "allowEmptyValue", allowEmptyValue)
				.appendIf(nf, "exclusiveMaximum", exclusiveMaximum)
				.appendIf(nf, "exclusiveMinimum", exclusiveMinimum)
				.appendIf(nf, "required", required)
				.appendIf(nf, "uniqueItems", uniqueItems)
				.appendIf(nf, "skipIfEmpty", skipIfEmpty)
				.appendIf(x -> x != HttpPartCollectionFormat.NO_COLLECTION_FORMAT, "collectionFormat", collectionFormat)
				.appendIf(ne, "pattern", pattern)
				.appendIf(nn, "items", items)
				.appendIf(nn, "additionalProperties", additionalProperties)
				.appendIf(nm1, "maximum", maximum)
				.appendIf(nm1, "minimum", minimum)
				.appendIf(nm1, "multipleOf", multipleOf)
				.appendIf(nm1, "maxLength", maxLength)
				.appendIf(nm1, "minLength", minLength)
				.appendIf(nm1, "maxItems", maxItems)
				.appendIf(nm1, "minItems", minItems)
				.appendIf(nm1, "maxProperties", maxProperties)
				.appendIf(nm1, "minProperties", minProperties)
				.append("parsedType", parsedType)
			;
			return m.toString();
		} catch (Exception e) {
			return "";
		}
	}
}
