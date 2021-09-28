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
package org.apache.juneau.http.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;

/**
 * REST request header annotation.
 *
 * <p>
 * Identifies a POJO to be used as the header of an HTTP request.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments and argument-types of server-side <ja>@RestOp</ja>-annotated methods.
 * 	<li>Arguments and argument-types of client-side <ja>@RemoteResource</ja>-annotated interfaces.
 * 	<li>Methods and return types of server-side and client-side <ja>@Request</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of server-side @RestOp-annotated methods</h5>
 *
 * Annotation that can be applied to a parameter of a <ja>@RestOp</ja>-annotated method to identify it as a HTTP
 * request header.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(<ja>@Header</ja>(<js>"ETag"</js>) UUID <jv>etag</jv>) {...}
 * </p>
 *
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestGet</ja>
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		UUID <jv>etag</jv> = <jv>req</jv>.getHeader(UUID.<jk>class</jk>, <js>"ETag"</js>);
 * 		...
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestHeaderAnnotation}
 * 	<li class='link'>{@doc RestSwagger}
 * 	<li class='extlink'>{@doc ExtSwaggerParameterObject}
 * </ul>
 *
 * <h5 class='topic'>Arguments and argument-types of client-side @RemoteResource-annotated interfaces</h5>
 *
 * Annotation applied to Java method arguments of interface proxies to denote that they are serialized as an HTTP
 * header value.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestcHeader}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Request-annotated interfaces</h5>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestRequestAnnotation}
 * 	<li class='link'>{@doc RestcRequest}
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Inherited
@Repeatable(HeaderAnnotation.Array.class)
@ContextApply(HeaderAnnotation.Applier.class)
public @interface Header {

	/**
	 * <mk>default</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Declares the value of the parameter that the server will use if none is provided, for example a "count" to control the number of results per page might default to 100 if not supplied by the client in the request.
	 * <br>(Note: "default" has no meaning for required parameters.)
	 *
	 * <p>
	 * Additionally, this value is used to create instances of POJOs that are then serialized as language-specific examples in the generated Swagger documentation
	 * if the examples are not defined in some other way.
	 *
	 * <p>
	 * The format of this value is a string.
	 * <br>Multiple lines are concatenated with newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(
	 * 		<ja>@Header</ja>(name=<js>"X-PetId"</js>, _default=<js>"100"</js>) <jk>long</jk> <jv>petId</jv>,
	 * 		<ja>@Header</ja>(name=<js>"X-AdditionalInfo"</js>, format=<js>"uon"</js>, _default=<js>"(rushOrder=false)"</js>) AdditionalInfo <jv>additionalInfo</jv>,
	 * 		<ja>@Header</ja>(name=<js>"X-Flags"</js>, collectionFormat=<js>"uon"</js>, _default=<js>"@(new-customer)"</js>) String[] <jv>flags</jv>
	 * 	) {...}
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * If specified, the input validates successfully if it is equal to one of the elements in this array.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * The format is a {@doc SimplifiedJson} array or comma-delimited list.
	 * <br>Multiple lines are concatenated with newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Comma-delimited list</jc>
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Header</ja>(
	 * 			name=<js>"X-Status"</js>,
	 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
	 * 		) PetStatus <jv>status</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// JSON array</jc>
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Header</ja>(
	 * 			name=<js>"X-Status"</js>,
	 * 			_enum=<js>"['AVAILABLE','PENDING','SOLD']"</js>,
	 * 		) PetStatus <jv>status</jv>
	 * 	) {...}
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String[] _enum() default {};

	/**
	 * Synonym for {@link #allowEmptyValue()}.
	 */
	boolean aev() default false;

	/**
	 * <mk>allowEmptyValue</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Sets the ability to pass empty-valued heaver values.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 *
	 * <p>
	 * <b>Note:</b>  This is technically only valid for either query or formData parameters, but support is provided anyway for backwards compatability.
	 */
	boolean allowEmptyValue() default false;

	/**
	 * Free-form value for the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the request header:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Header</ja>(
	 * 		name=<js>"api_key"</js>,
	 * 		description=<js>"Security API key"</js>,
	 * 		required=<jk>true</jk>,
	 * 		example=<js>"foobar"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Header</ja>(
	 * 		name=<js>"api_key"</js>,
	 * 		api={
	 * 			<js>"description: 'Security API key',"</js>,
	 * 			<js>"required: true,"</js>,
	 * 			<js>"example: 'foobar'"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form with variables</jc>
	 * 	<ja>@Header</ja>(
	 * 		name=<js>"api_key"</js>,
	 * 		api=<js>"$L{apiKeySwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>apiKeySwagger</mk> = <mv>{ description: "Security API key", required: true, example: "foobar" }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this field from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object.
	 * 	<li>
	 * 		Automatic validation is NOT performed on input based on attributes in this value.
	 * 	<li>
	 * 		The leading/trailing <c>{ }</c> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Header</ja>(api=<js>"{required: true}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Header</ja>(api=<js>"required: true"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};

	//=================================================================================================================
	// Attributes common to all Swagger Parameter objects
	//=================================================================================================================

	/**
	 * Synonym for {@link #collectionFormat()}.
	 */
	String cf() default "";

	/**
	 * <mk>collectionFormat</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
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
	 * 		<js>"uon"</js> - UON notation (e.g. <js>"@(foo,bar)"</js>).
	 * 	<li>
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link CollectionFormatType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
	 */
	String collectionFormat() default "";

	/**
	 * Synonym for {@link #description()}.
	 */
	String[] d() default {};

	/**
	 * <mk>description</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A brief description of the parameter. This could contain examples of use.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * Synonym for {@link #_default()}.
	 */
	String[] df() default {};

	/**
	 * Synonym for {@link #_enum()}.
	 */
	String[] e() default {};

	/**
	 * Synonym for {@link #exclusiveMaximum()}.
	 */
	boolean emax() default false;

	//=================================================================================================================
	// Attributes specific to parameters other than body
	//=================================================================================================================

	/**
	 * Synonym for {@link #exclusiveMinimum()}.
	 */
	boolean emin() default false;

	/**
	 * Synonym for {@link #example()}.
	 */
	String[] ex() default {};

	/**
	 * A serialized example of the parameter.
	 *
	 * <p>
	 * This attribute defines a representation of the value that is used by <c>BasicRestInfoProvider</c> to construct
	 * an example of parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Header</ja>(
	 * 		name=<js>"Status"</js>,
	 * 		type=<js>"array"</js>,
	 * 		collectionFormat=<js>"csv"</js>,
	 * 		example=<js>"AVAILIABLE,PENDING"</js>
	 * 	)
	 * 	PetStatus[] <jv>status</jv>
	 * </p>
	 *
	 * <p>
	 * If not specified, Juneau will automatically create examples from sample POJOs serialized using the registered {@link HttpPartSerializer}.
	 * <br>
	 *
	 * </p>
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='ja'>{@link Example}
	 * 	<li class='ja'>{@link Marshalled#example() Marshalled(example)}
	 * 	<li class='jc'>{@link JsonSchemaGeneratorBuilder}
	 * 	<ul>
	 * 		<li class='jm'>{@link JsonSchemaGeneratorBuilder#addExamplesTo(TypeCategory...) addExamplesTo(TypeCategory...)}
	 * 		<li class='jm'>{@link JsonSchemaGeneratorBuilder#allowNestedExamples() allowNestedExamples()}
	 * 	</ul>
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The format is a {@doc SimplifiedJson} object or plain text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * <mk>exclusiveMaximum</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Defines whether the maximum is matched exclusively.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <c>maximum</c>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Defines whether the minimum is matched exclusively.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <c>minimum</c>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * Synonym for {@link #format()}.
	 */
	String f() default "";

	/**
	 * <mk>format</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * The extending format for the previously mentioned {@doc ExtSwaggerParameterTypes parameter type}.
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
	 * <p>
	 * Static strings are defined in {@link FormatType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc ExtSwaggerDataTypeFormats}
	 * </ul>
	 */
	String format() default "";

	/**
	 * <mk>items</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Describes the type of items in the array.
	 *
	 * <p>
	 * Required if <c>type</c> is <js>"array"</js>.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing and parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing and serializing validation.
	 * </ul>
	 */
	Items items() default @Items;

	/**
	 * Synonym for {@link #maximum()}.
	 */
	String max() default "";

	/**
	 * Synonym for {@link #maxItems()}.
	 */
	long maxi() default -1;

	/**
	 * <mk>maximum</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Defines the maximum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String maximum() default "";

	/**
	 * <mk>maxItems</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * An array or collection is valid if its size is less than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long maxItems() default -1;

	/**
	 * Synonym for {@link #maxLength()}.
	 */
	long maxl() default -1;

	/**
	 * <mk>maxLength</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <c>-1</c> is always ignored.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long maxLength() default -1;

	/**
	 * Synonym for {@link #minimum()}.
	 */
	String min() default "";

	/**
	 * Synonym for {@link #minItems()}.
	 */
	long mini() default -1;

	/**
	 * <mk>minimum</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Defines the minimum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>minItems</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * An array or collection is valid if its size is greater than, or equal to, the value of this keyword.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long minItems() default -1;

	/**
	 * Synonym for {@link #minLength()}.
	 */
	long minl() default -1;

	/**
	 * <mk>minLength</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <c>-1</c> is always ignored.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	long minLength() default -1;

	/**
	 * Synonym for {@link #multipleOf()}.
	 */
	String mo() default "";

	/**
	 * Denotes a multi-part parameter (e.g. multiple entries with the same name).
	 *
	 * <h5 class='figure'>Example</h5>
	 * 	<jk>public void</jk> doPost(
	 * 		<ja>@Header</ja>(name=<js>"Beans"</js>, multi=<jk>true</jk>) MyBean[] <jv>beans</jv>
	 * 	) {
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Meant to be used on multi-part parameters (e.g. <js>"Header: h1"</js>, <js>"Header: h2"</js> instead of <js>"Header: @(h1,h2)"</js>)
	 * 	<li>
	 * 		The data type must be a collection or array type.
	 * </ul>
	 */
	boolean multi() default false;

	/**
	 * <mk>multipleOf</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A numeric instance is valid if the result of the division of the instance by this keyword's value is an integer.
	 * <br>The value must be a valid JSON number.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String multipleOf() default "";

	/**
	 * Synonym for {@link #name()}.
	 */
	String n() default "";

	/**
	 * HTTP header name.
	 * <p>
	 * A blank value (the default) indicates to reuse the bean property name when used on a request bean property.
	 *
	 * <p>
	 * The value should be either a valid HTTP header name, or <js>"*"</js> to represent multiple name/value pairs
	 *
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <c>NameValuePairs</c>, <c>Map</c>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 *
	 * 		<h5 class='figure'>Examples:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a REST method</jc>
	 * 	<ja>@RestPost</ja>(<js>"/addPet"</js>)
	 * 	<jk>public void</jk> addPet(<ja>@Header</ja> OMap <jv>allHeaderParameters</jv>) {...}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@RemoteResource</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@RemoteGet</ja>(<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Header</ja> Map&lt;String,Object&gt; <jv>allHeaderParameters</jv>);
	 * 	}
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@Header</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * 	<li>
	 * 		If used on a request bean method, uses the bean property name.
	 *
	 * 		<h5 class='figure'>Example:</h5>
	 * 		<p class='bcode w800'>
	 * 	<jk>public interface</jk> MyRequest {
	 *
	 * 		<jc>// Equivalent to @Header("Foo")</jc>
	 * 		<ja>@Header</ja>
	 * 		<ja>@Beanp</ja>(<js>"Foo"</js>)
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 */
	String name() default "";

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc DynamicallyAppliedAnnotations}
	 * </ul>
	 */
	Class<?>[] onClass() default {};

	/**
	 * Synonym for {@link #pattern()}.
	 */
	String p() default "";

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing strings to values.
	 *
	 * <p>
	 * Overrides for this part the part parser defined on the REST resource which by default is {@link OpenApiParser}.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;

	/**
	 * <mk>pattern</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * A string input is valid if it matches the specified regular expression pattern.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * Synonym for {@link #required()}.
	 */
	boolean r() default false;

	/**
	 * <mk>required</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * Determines whether the parameter is mandatory.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean required() default false;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST client which by default is {@link OpenApiSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * Synonym for {@link #skipIfEmpty()}.
	 */
	boolean sie() default false;

	/**
	 * Skips this value during serialization if it's an empty string or empty collection/array.
	 *
	 * <p>
	 * Note that <jk>null</jk> values are already ignored.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Synonym for {@link #type()}.
	 */
	String t() default "";

	/**
	 * <mk>type</mk> field of the {@doc ExtSwaggerParameterObject}.
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
	 * 		<br>If parameter is <c>Object</c>, creates an {@link OList}.
	 * 	<li>
	 * 		<js>"object"</js>
	 * 		<br>Parameter must be a map or bean.
	 * 		<br>If parameter is <c>Object</c>, creates an {@link OMap}.
	 * 		<br>Note that this is an extension of the OpenAPI schema as Juneau allows for arbitrarily-complex POJOs to be serialized as HTTP parts.
	 * 	<li>
	 * 		<js>"file"</js>
	 * 		<br>This type is currently not supported.
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link ParameterType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='extlink'>{@doc ExtSwaggerDataTypes}
	 * </ul>
	 */
	String type() default "";

	//=================================================================================================================
	// Other
	//=================================================================================================================

	/**
	 * Synonym for {@link #uniqueItems()}.
	 */
	boolean ui() default false;

	/**
	 * <mk>uniqueItems</mk> field of the {@doc ExtSwaggerParameterObject}.
	 *
	 * <p>
	 * If <jk>true</jk> the input validates successfully if all of its elements are unique.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <p>
	 * If the parameter type is a subclass of {@link Set}, this validation is skipped (since a set can only contain unique items anyway).
	 * <br>Otherwise, the collection or array is checked for duplicate items.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"array"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side schema-based parsing validation.
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * 	<li>
	 * 		Client-side schema-based serializing validation.
	 * </ul>
	 */
	boolean uniqueItems() default false;

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a header entry:
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Header</ja>(name=<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jk>public</jk> Order placeOrder(<ja>@Header</ja>(<js>"api_key"</js>) String <jv>apiKey</jv>) {...}
	 * </p>
	 */
	String value() default "";
}
