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

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;

/**
 * REST response header annotation.
 *
 * <p>
 * Annotation used to denote an HTTP response header.
 *
 * <p>
 * Can be used in the following locations:
 * <ul>
 * 	<li>Arguments of server-side <ja>@RestMethod</ja>-annotated methods.
 * 	<li>Methods and return types of server-side and client-side <ja>@Response</ja>-annotated interfaces.
 * </ul>
 *
 * <h5 class='topic'>Arguments of server-side <ja>@RestMethod</ja>-annotated methods</h5>
 *
 * <p>
 * On server-side REST, this annotation can be applied to method parameters to identify them as an HTTP response header.
 * <br>In this case, the annotation can only be applied to subclasses of type {@link Value}.
 *
 * <p>
 * The following examples show 3 different ways of accomplishing the same task of setting an HTTP header
 * on a response:
 *
 * <p class='bcode w800'>
 * 	<jc>// Example #1 - Setting header directly on RestResponse object.</jc>
 * 	<ja>@RestMethod</ja>(...)
 * 	<jk>public void</jk> login(RestResponse res) {
 * 		res.setHeader(<js>"X-Rate-Limit"</js>, 1000);
 * 		...
 * 	}
 *
 *	<jc>// Example #2 - Use on parameter.</jc>
 * 	<ja>@RestMethod</ja>(...)
 * 	<jk>public void</jk> login(
 * 			<ja>@ResponseHeader</ja>(
 * 				name=<js>"X-Rate-Limit"</js>,
 * 				type=<js>"integer"</js>,
 * 				format=<js>"int32"</js>,
 * 				description=<js>"Calls per hour allowed by the user."</js>,
 * 				example=<js>"123"</js>
 * 			)
 * 			Value&lt;Integer&gt; rateLimit
 *		) {
 *		rateLimit.set(1000);
 *		...
 * 	}
 *
 *	<jc>// Example #3 - Use on type.</jc>
 * 	<ja>@RestMethod</ja>(...)
 * 	<jk>public void</jk> login(Value&lt;RateLimit&gt; rateLimit) {
 * 		rateLimit.set(new RateLimit(1000));
 * 		...
 * 	}
 *
 * 	<ja>@ResponseHeader</ja>(
 * 		name=<js>"X-Rate-Limit"</js>,
 * 		type=<js>"integer"</js>,
 * 		format=<js>"int32"</js>,
 * 		description=<js>"Calls per hour allowed by the user."</js>,
 * 		example=<js>"123"</js>
 * 	)
 * 	<jk>public class</jk> RateLimit {
 * 		<jc>// OpenApiPartSerializer knows to look for this method based on format/type.</jc>
 * 		<jk>public</jk> Integer toInteger() {
 * 			<jk>return</jk> 1000;
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='topic'>Public methods of @Response-annotated types</h5>
 *
 * <p>
 * On server-side REST, this annotation can also be applied to public methods of {@link Response}-annotated methods.
 *
 * <p class='bcode w800'>
 * 	<ja>@Response</ja>
 * 	<jk>public class</jk> AddPetSuccess {
 *
 * 		<ja>@ResponseHeader</ja>(
 * 			name=<js>"X-PetId"</js>,
 * 			type=<js>"integer"</js>,
 * 			format=<js>"int32"</js>,
 * 			description=<js>"ID of added pet."</js>,
 * 			example=<js>"123"</js>
 * 		)
 * 		<jk>public int</jk> getPetId() {...}
 * 	}
 * </p>
 *
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.ResponseHeader}
 * 	<li class='link'>{@doc juneau-rest-server.Swagger}
 * 	<li class='extlink'>{@doc SwaggerHeaderObject}
 * </ul>
 *
 * <h5 class='topic'>Methods and return types of server-side and client-side @Response-annotated interfaces</h5>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-rest-server.HttpPartAnnotations.Response}
 * 	<li class='link'>{@doc juneau-rest-client.RestProxies.Response}
 * </ul>
*/
@Documented
@Target({PARAMETER,METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface ResponseHeader {

	/**
	 * The HTTP status (or statuses) of the response.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The is a comma-delimited list of HTTP status codes that this header applies to.
	 * 	<li>
	 * 		The default value is <js>"200"</js>.
	 * </ul>
	 */
	int[] code() default {};

	/**
	 * The HTTP header name.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain-text.
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 *
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining a response header:
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(name=<js>"X-Rate-Limit"</js>) Value&lt;Integer&gt; rateLimit)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(<js>"X-Rate-Limit"</js>) Value&lt;Integer&gt; rateLimit)
	 * </p>
	 */
	String value() default "";

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 *
	 * <p>
	 * Overrides for this part the part serializer defined on the REST resource which by default is {@link OpenApiPartSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * <mk>description</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};

	/**
	 * <mk>type</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <p>
	 * The type of the parameter.
	 *
	 * <p>
	 * The possible values are:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"string"</js>
	 * 	<li>
	 * 		<js>"number"</js>
	 * 	<li>
	 * 		<js>"integer"</js>
	 * 	<li>
	 * 		<js>"boolean"</js>
	 * 	<li>
	 * 		<js>"array"</js>
	 * 	<li>
	 * 		<js>"object"</js>
	 * 	<li>
	 * 		<js>"file"</js>
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link ParameterType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypes}
	 * </ul>
	 */
	String type() default "";

	/**
	 * <mk>format</mk> field of the {@doc SwaggerHeaderObject}.
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
	 * 	<li>
	 * 		<js>"binary"</js> - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 	<li>
	 * 		<js>"date"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 	<li>
	 * 		<js>"date-time"</js> - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
	 * 		<br>Only valid with type <js>"string"</js>.
	 * 	<li>
	 * 		<js>"password"</js> - Used to hint UIs the input needs to be obscured.
	 * 	<li>
	 * 		<js>"uon"</js> - UON notation (e.g. <js>"(foo=bar,baz=@(qux,123))"</js>).
	 * 		<br>Only valid with type <js>"object"</js>.
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link FormatType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='extlink'>{@doc SwaggerDataTypeFormats}
	 * </ul>
	 */
	String format() default "";

	/**
	 * <mk>collectionFormat</mk> field of the {@doc SwaggerHeaderObject}.
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
	 * </ul>
	 *
	 * <p>
	 * Static strings are defined in {@link CollectionFormatType}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <p>
	 * Note that for collections/arrays parameters with POJO element types, the input is broken into a string array before being converted into POJO elements.
	 */
	String collectionFormat() default "";

	/**
	 * <mk>$ref</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <p>
	 * Denotes a reference to a definition object.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String $ref() default "";

	/**
	 * <mk>maximum</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String maximum() default "";

	/**
	 * <mk>minimum</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>multipleOf</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String multipleOf() default "";

	/**
	 * <mk>maxLength</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	long maxLength() default -1;

	/**
	 * <mk>minLength</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	long minLength() default -1;

	/**
	 * <mk>pattern</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <p>
	 * A string value is valid if it matches the specified regular expression pattern.
	 *
	 * <p>
	 * If validation fails during serialization or parsing, the part serializer/parser will throw a {@link SchemaValidationException}.
	 *
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * <mk>maxItems</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	long maxItems() default -1;

	/**
	 * <mk>minItems</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	long minItems() default -1;

	/**
	 * <mk>exclusiveMaximum</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>exclusiveMinimum</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * <mk>uniqueItems</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	boolean uniqueItems() default false;

	/**
	 * <mk>items</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	Items items() default @Items;

	/**
	 * <mk>default</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String[] _default() default {};

	/**
	 * <mk>enum</mk> field of the {@doc SwaggerHeaderObject}.
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String[] _enum() default {};

	/**
	 * A serialized example of the parameter.
	 *
	 * <p>
	 * This attribute defines a representation of the value that is used by <code>BasicRestInfoProvider</code> to construct
	 * an example of parameter.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(
	 * 		name=<js>"Status"</js>,
	 * 		type=<js>"array"</js>,
	 * 		collectionFormat=<js>"csv"</js>,
	 * 		example=<js>"AVALIABLE,PENDING"</js>
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 */
	String[] example() default {};

	/**
	 * Free-form value for the {@doc SwaggerHeaderObject}.
	 *
	 * <p>
	 * This is a {@doc juneau-marshall.JsonDetails.SimplifiedJson} object that makes up the swagger information for this field.
	 *
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the Header object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@ResponseHeader</ja>(
	 * 		name=<js>"Location"</js>,
	 * 		description=<js>"The new location of this resource"</js>,
	 * 		type=<js>"string"</js>,
	 * 		format=<js>"uri"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@ResponseHeader</ja>(
	 * 		name=<js>"Location"</js>,
	 * 		api={
	 * 			<js>"description: 'The new location of this resource',"</js>,
	 * 			<js>"type: 'string',"</js>,
	 * 			<js>"format: 'uri'"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@ResponseHeader</ja>(
	 * 		name=<js>"Location"</js>,
	 * 		api=<js>"$L{locationSwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>locationSwagger</mk> = <mv>{ description: "The new location of this resource", type: "string", format: "uri" }</mv>
	 * </p>
	 *
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this body from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 *
	 * <h5 class='section'>Used for:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Server-side generated Swagger documentation.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a {@doc juneau-marshall.JsonDetails.SimplifiedJson} object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(<js>"{description:'The new location of this resource'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(<js>"description:'The new location of this resource'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
