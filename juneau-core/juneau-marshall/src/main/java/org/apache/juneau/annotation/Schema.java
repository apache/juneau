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
package org.apache.juneau.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;
import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.oapi.*;

/**
 * Swagger schema annotation.
 *
 * <p>
 * The Schema Object allows the definition of input and output data types.
 * These types can be objects, but also primitives and arrays.
 * This object is based on the JSON Schema Specification Draft 4 and uses a predefined subset of it.
 * On top of this subset, there are extensions provided by this specification to allow for more complete documentation.
 *
 * <p>
 * Used to populate the auto-generated Swagger documentation and UI for server-side <ja>@Rest</ja>-annotated classes.
 * <br>Also used to define OpenAPI schema information for POJOs serialized through {@link OpenApiSerializer} and parsed through {@link OpenApiParser}.
 *
 * <h5 class='section'>Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// A response object thats a hex-encoded string</jc>
 * 	<ja>@Response</ja>(
 * 		schema=<ja>@Schema</ja>(
 * 			type=<js>"string"</js>,
 * 			format=<js>"binary"</js>
 * 		)
 * 	)
 * </p>
 * <p class='bjava'>
 * 	<jc>// A request body consisting of an array of arrays, the internal
 * 	// array being of type integer, numbers must be between 0 and 63 (inclusive)</jc>
 * 	<ja>@Content</ja>(
 * 		schema=<ja>@Schema</ja>(
 * 			items=<ja>@Items</ja>(
 * 				type=<js>"array"</js>,
 * 				items=<ja>@SubItems</ja>(
 * 					type=<js>"integer"</js>,
 * 					minimum=<js>"0"</js>,
 * 					maximum=<js>"63"</js>
 * 				)
 *			)
 * 		)
 * 	)
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a>
 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,METHOD,TYPE,FIELD})
@Retention(RUNTIME)
@Repeatable(SchemaAnnotation.Array.class)
@ContextApply(SchemaAnnotation.Apply.class)
public @interface Schema {

	/**
	 * <mk>default</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 * <p class='bjava'>
	 * 	<jk>public</jk> Order placeOrder(
	 * 		<ja>@Header</ja>(<js>"X-PetId"</js>)
	 * 		<ja>@Schema</ja>(_default=<js>"100"</js>)
	 * 		<jk>long</jk> <jv>petId</jv>,
	 *
	 * 		<ja>@Header</ja>(<js>"X-AdditionalInfo"</js>)
	 * 		<ja>@Schema</ja>(format=<js>"uon"</js>, _default=<js>"(rushOrder=false)"</js>)
	 * 		AdditionalInfo <jv>additionalInfo</jv>,
	 *
	 * 		<ja>@Header</ja>(<js>"X-Flags"</js>)
	 * 		<ja>@Schema</ja>(collectionFormat=<js>"uon"</js>, _default=<js>"@(new-customer)"</js>)
	 * 		String[] <jv>flags</jv>
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
	 *
	 * @return The annotation value.
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 * The format is either individual values or a comma-delimited list.
	 * <br>Multiple lines are concatenated with newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Comma-delimited list</jc>
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Header</ja>(<js>"X-Status"</js>)
	 * 		<ja>@Schema</ja>(_enum=<js>"AVAILABLE,PENDING,SOLD"</js>)
	 * 		PetStatus <jv>status</jv>
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
	 *
	 * @return The annotation value.
	 */
	String[] _enum() default {};

	/**
	 * <mk>$ref</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * 	A JSON reference to the schema definition.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a href='https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03'>JSON Reference</a>.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String $ref() default "";

	/**
	 * <mk>additionalProperties</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] additionalProperties() default {};

	/**
	 * <mk>allOf</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] allOf() default {};

	/**
	 * Synonym for {@link #allowEmptyValue()}.
	 *
	 * @return The annotation value.
	 */
	boolean aev() default false;

	/**
	 * <mk>allowEmptyValue</mk> field of the <a class='doclink' href='https://swagger.io/specification/v2#parameterObject'>Swagger Parameter Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	boolean allowEmptyValue() default false;

	/**
	 * Synonym for {@link #collectionFormat()}.
	 *
	 * @return The annotation value.
	 */
	String cf() default "";

	/**
	 * <mk>collectionFormat</mk> field.
	 *
	 * <p>
	 * Note that this field isn't part of the Swagger 2.0 specification, but the specification does not specify how
	 * items are supposed to be represented.
	 *
	 * <p>
	 * Determines the format of the array if <c>type</c> <js>"array"</js> is used.
	 * <br>Can only be used if <c>type</c> is <js>"array"</js>.
	 *
	 * <p>
	 * Static strings are defined in {@link CollectionFormatType}.
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
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
	 *
	 * <ul class='values'>
	 * 	<li><js>"csv"</js> (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 	<li><js>"ssv"</js> - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 	<li><js>"tsv"</js> - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 	<li><js>"pipes</js> - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * 	<li><js>"multi"</js> - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
	 * 	<li><js>"uon"</js> - UON notation (e.g. <js>"@(foo,bar)"</js>).
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String collectionFormat() default "";

	/**
	 * Synonym for {@link #description()}.
	 *
	 * @return The annotation value.
	 */
	String[] d() default {};

	/**
	 * <mk>description</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * A brief description of the body. This could contain examples of use.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Content</ja> <ja>@Schema</ja>(description=<js>"Pet object to add to the store"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Content</ja> <ja>@Schema</ja>(description=<js>"Pet object to add to the store"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li class='note'>
	 * 		Supports <a class="doclink" href="../../../../index.html#jrs.SvlVariables">SVL Variables</a> (e.g. <js>"$L{my.localized.variable}"</js>) for the swagger generator.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] description() default {};

	/**
	 * Synonym for {@link #_default()}.
	 *
	 * @return The annotation value.
	 */
	String[] df() default {};

	/**
	 * <mk>discriminator</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String discriminator() default "";

	/**
	 * Synonym for {@link #_enum()}.
	 *
	 * @return The annotation value.
	 */
	String[] e() default {};

	/**
	 * Synonym for {@link #exclusiveMaximum()}.
	 *
	 * @return The annotation value.
	 */
	boolean emax() default false;

	/**
	 * Synonym for {@link #exclusiveMinimum()}.
	 *
	 * @return The annotation value.
	 */
	boolean emin() default false;

	/**
	 * <mk>exclusiveMaximum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * <mk>externalDocs</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	ExternalDocs externalDocs() default @ExternalDocs;

	/**
	 * Synonym for {@link #format()}.
	 *
	 * @return The annotation value.
	 */
	String f() default "";

	/**
	 * <mk>format</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * The extending format for the previously mentioned <a class="doclink" href="https://swagger.io/specification/v2#parameterType">parameter type</a>.
	 *
	 * <p>
	 * Static strings are defined in {@link FormatType}.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPut</ja>
	 * 	<jk>public void</jk> setAge(
	 * 		<ja>@Content</ja> <ja>@Schema</ja>(type=<js>"integer"</js>, format=<js>"int32"</js>) String <jv>input</jv>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String format() default "";

	/**
	 * Specifies that schema information for this part should not be shown in the generated Swagger documentation.
	 *
	 * @return The annotation value.
	 */
	boolean ignore() default false;

	/**
	 * <mk>items</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	Items items() default @Items;

	/**
	 * Synonym for {@link #maximum()}.
	 *
	 * @return The annotation value.
	 */
	String max() default "";

	/**
	 * Synonym for {@link #maxItems()}.
	 *
	 * @return The annotation value.
	 */
	long maxi() default -1;

	/**
	 * <mk>maximum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	String maximum() default "";

	/**
	 * <mk>maxItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	long maxItems() default -1;

	/**
	 * Synonym for {@link #maxLength()}.
	 *
	 * @return The annotation value.
	 */
	long maxl() default -1;

	/**
	 * <mk>maxLength</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	long maxLength() default -1;

	/**
	 * Synonym for {@link #maxProperties()}.
	 *
	 * @return The annotation value.
	 */
	long maxp() default -1;

	/**
	 * <mk>maxProperties</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long maxProperties() default -1;

	/**
	 * Synonym for {@link #minimum()}.
	 *
	 * @return The annotation value.
	 */
	String min() default "";

	/**
	 * Synonym for {@link #minItems()}.
	 *
	 * @return The annotation value.
	 */
	long mini() default -1;

	/**
	 * <mk>minimum</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	String minimum() default "";

	/**
	 * <mk>minItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	long minItems() default -1;

	/**
	 * Synonym for {@link #minLength()}.
	 *
	 * @return The annotation value.
	 */
	long minl() default -1;

	/**
	 * <mk>minLength</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	long minLength() default -1;

	/**
	 * Synonym for {@link #minProperties()}.
	 *
	 * @return The annotation value.
	 */
	long minp() default -1;

	/**
	 * <mk>minProperties</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	long minProperties() default -1;

	/**
	 * Synonym for {@link #multipleOf()}.
	 *
	 * @return The annotation value.
	 */
	String mo() default "";

	/**
	 * <mk>multipleOf</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	String multipleOf() default "";

	/**
	 * Dynamically apply this annotation to the specified classes/methods/fields.
	 *
	 * <p>
	 * Used in conjunction with {@link org.apache.juneau.BeanContext.Builder#applyAnnotations(Class...)} to dynamically apply an annotation to an existing class/method/field.
	 * It is ignored when the annotation is applied directly to classes/methods/fields.
	 *
	 * <h5 class='section'>Valid patterns:</h5>
	 * <ul class='spaced-list'>
	 *  <li>Classes:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass"</js>
	 * 				</ul>
	 * 			<li>Fully qualified inner class:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass$Inner1$Inner2"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass"</js>
	 * 				</ul>
	 * 			<li>Simple inner:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2"</js>
	 * 					<li><js>"Inner1$Inner2"</js>
	 * 					<li><js>"Inner2"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Methods:
	 * 		<ul>
	 * 			<li>Fully qualified with args:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"com.foo.MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple with args:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod(String,int)"</js>
	 * 					<li><js>"MyClass.myMethod(java.lang.String,int)"</js>
	 * 					<li><js>"MyClass.myMethod()"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myMethod"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner1$Inner2.myMethod"</js>
	 * 					<li><js>"Inner2.myMethod"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>Fields:
	 * 		<ul>
	 * 			<li>Fully qualified:
	 * 				<ul>
	 * 					<li><js>"com.foo.MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple:
	 * 				<ul>
	 * 					<li><js>"MyClass.myField"</js>
	 * 				</ul>
	 * 			<li>Simple inner class:
	 * 				<ul>
	 * 					<li><js>"MyClass$Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner1$Inner2.myField"</js>
	 * 					<li><js>"Inner2.myField"</js>
	 * 				</ul>
	 * 		</ul>
	 * 	<li>A comma-delimited list of anything on this list.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};

	/**
	 * Dynamically apply this annotation to the specified classes.
	 *
	 * <p>
	 * Identical to {@link #on()} except allows you to specify class objects instead of a strings.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	Class<?>[] onClass() default {};

	/**
	 * Synonym for {@link #pattern()}.
	 *
	 * @return The annotation value.
	 */
	String p() default "";

	/**
	 * <mk>pattern</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * A string input is valid if it matches the specified regular expression pattern.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@RestPut</ja>
	 * 	<jk>public void</jk> doPut(<ja>@Content</ja> <ja>@Schema</ja>(pattern=<js>"/\\w+\\.\\d+/"</js>) String <jv>input</jv>) {...}
	 * </p>
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
	 *
	 * @return The annotation value.
	 */
	String pattern() default "";

	/**
	 * <mk>properties</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] properties() default {};

	/**
	 * Synonym for {@link #required()}.
	 *
	 * @return The annotation value.
	 */
	boolean r() default false;

	/**
	 * <mk>readOnly</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	boolean readOnly() default false;

	/**
	 * <mk>required</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * Determines whether the parameter is mandatory.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 * <br>On the client-side, this gets converted to a <c>RestCallException</c> which is thrown before the connection is made.
	 * <br>On the server-side, this gets converted to a <c>BadRequest</c> (400).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Content</ja> <ja>@Schema</ja>(required=<jk>true</jk>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Content</ja>(required=<jk>true</jk>)
	 * 	<jk>public class</jk> Pet {...}
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
	 *
	 * @return The annotation value.
	 */
	boolean required() default false;

	/**
	 * Synonym for {@link #readOnly()}.
	 *
	 * @return The annotation value.
	 */
	boolean ro() default false;

	/**
	 * Synonym for {@link #skipIfEmpty()}.
	 *
	 * @return The annotation value.
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
	 *
	 * @return The annotation value.
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Synonym for {@link #type()}.
	 *
	 * @return The annotation value.
	 */
	String t() default "";

	/**
	 * <mk>title</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is plain text.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String title() default "";

	/**
	 * <mk>type</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <p>
	 * The type of the parameter.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Content</ja> <ja>@Schema</ja>(type=<js>"object"</js>) Pet <jv>input</jv>
	 * 	) {...}
	 * </p>
	 * <p class='bjava'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestPost</ja>
	 * 	<jk>public void</jk> addPet(Pet <jv>input</jv>) {...}
	 *
	 * 	<ja>@Content</ja> <ja>@Schema</ja>(type=<js>"object"</js>)
	 * 	<jk>public class</jk> Pet {...}
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
	 *
	 * <ul class='values spaced-list'>
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
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>Static strings are defined in {@link ParameterType}.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='extlink'><a class="doclink" href="https://swagger.io/specification#dataTypes">Swagger Data Types</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String type() default "";

	/**
	 * Synonym for {@link #uniqueItems()}.
	 *
	 * @return The annotation value.
	 */
	boolean ui() default false;

	/**
	 * <mk>uniqueItems</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
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
	 *
	 * @return The annotation value.
	 */
	boolean uniqueItems() default false;

	/**
	 * <mk>xml</mk> field of the <a class="doclink" href="https://swagger.io/specification/v2#schemaObject">Swagger Schema Object</a>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>
	 * 		The format is a <a class="doclink" href="../../../../index.html#jd.Swagger">Swagger</a> object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] xml() default {};
}
