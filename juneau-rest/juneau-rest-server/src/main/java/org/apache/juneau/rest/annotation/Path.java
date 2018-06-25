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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.oapi.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.exception.*;

/**
 * Annotation that can be applied to a parameter of a {@link RestMethod @RestMethod} annotated method to identify it as a variable
 * in a URL path pattern converted to a POJO.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/myurl/{foo}/{bar}/{baz}/*"</js>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res,
 * 			<ja>@Path</ja>(<js>"foo"</js>) String foo, <ja>@Path</ja>(<js>"bar"</js>) <jk>int</jk> bar, <ja>@Path</ja>(<js>"baz"</js>) UUID baz) {
 * 		...
 * 	}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.MethodParameters">Overview &gt; juneau-rest-server &gt; Method Parameters</a>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Swagger Specification &gt; Parameter Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Path {

	/**
	 * Specifies the {@link HttpPartParser} class used for parsing values from strings.
	 * 
	 * <p>
	 * The default value for this parser is inherited from the servlet/method which defaults to {@link OapiPartParser}.
	 * <br>You can use {@link SimplePartParser} to parse POJOs that are directly convertible from <code>Strings</code>.
	 */
	Class<? extends HttpPartParser> parser() default HttpPartParser.Null.class;

	//=================================================================================================================
	// Attributes common to all Swagger Parameter objects
	//=================================================================================================================

	/**
	 * URL path variable name.
	 * 
	 * <p>
	 * The name field MUST correspond to the associated <a href='https://swagger.io/specification/v2/#pathsPath'>path</a> segment from the path field in the <a href='https://swagger.io/specification/v2/#pathsObject'>Paths Object</a>. 
	 * See <a href='https://swagger.io/specification/v2/#pathTemplating'>Path Templating</a> for further information.
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
	 * The following are completely equivalent ways of defining a path entry:
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, 
	 * 		path=<js>"/pet/{petId}"</js>
	 * 	)
	 * 	<jk>public</jk> Pet getPet(<ja>@Path</ja>(name=<js>"petId"</js>) <jk>long</jk> petId) { ... }
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<js>"GET"</js>, 
	 * 		path=<js>"/pet/{petId}"</js>
	 * 	)
	 * 	<jk>public</jk> Pet getPet(<ja>@Path</ja>(<js>"petId"</js>) <jk>long</jk> petId) { ... }
	 * </p>
	 */
	String value() default "";
	
	/**
	 * <mk>description</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A brief description of the parameter. This could contain examples of use. 
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 		<br>TODO - Future support for <a href='https://guides.github.com/features/mastering-markdown/#GitHub-flavored-markdown'>MarkDown</a>.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};
	
	//=================================================================================================================
	// Attributes specific to parameters other than body
	//=================================================================================================================

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
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
	 */
	String type() default "";
	
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
	 */
	String format() default "";
	
	/**
	 * <mk>items</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Describes the type of items in the array.
	 * <p>
	 * Required if <code>type</code> is <js>"array"</js>. 
	 * <br>Can only be used if <code>type</code> is <js>"array"</js>.
	 */
	Items items() default @Items;	
	
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
	 */
	String collectionFormat() default "";

	/**
	 * <mk>maximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Defines the maximum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 */
	String maximum() default "";
	
	/**
	 * <mk>exclusiveMaximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Defines whether the maximum is matched exclusively.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, must be accompanied with <code>maximum</code>.
	 */
	boolean exclusiveMaximum() default false;

	/**
	 * <mk>minimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Defines the minimum value for a parameter of numeric types.
	 * <br>The value must be a valid JSON number.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 */
	String minimum() default "";
	
	/**
	 * <mk>exclusiveMinimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Defines whether the minimum is matched exclusively.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 * <br>If <jk>true</jk>, Must be accompanied with <code>minimum</code>.
	 */
	boolean exclusiveMinimum() default false;

	/**
	 * <mk>maxLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A string instance is valid against this keyword if its length is less than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <code>-1</code> is always ignored.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 */
	long maxLength() default -1;
	
	/**
	 * <mk>minLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A string instance is valid against this keyword if its length is greater than, or equal to, the value of this keyword.
	 * <br>The length of a string instance is defined as the number of its characters as defined by <a href='https://tools.ietf.org/html/rfc4627'>RFC 4627</a>.
	 * <br>The value <code>-1</code> is always ignored.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 */
	long minLength() default -1;
	
	/**
	 * <mk>pattern</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A string input is valid if it matches the specified regular expression pattern.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"string"</js>.
	 */
	String pattern() default "";
	
	/**
	 * <mk>enum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * If specified, the input validates successfully if it is equal to one of the elements in this array.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * The format is a {@link JsonSerializer#DEFAULT_LAX Simple-JSON} array or comma-delimited list.
	 * <br>Multiple lines are concatenated with newlines.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/pet/findByStatus/{status}"</js>)
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Path</ja>(
	 * 			name=<js>"status"</js>, 
	 * 			_enum=<js>"AVAILABLE,PENDING,SOLD"</js>,
	 * 		) PetStatus status
	 * 	) {...}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/pet/findByStatus/{status}"</js>)
	 * 	<jk>public</jk> Collection&lt;Pet&gt; findPetsByStatus(
	 * 		<ja>@Path</ja>(
	 * 			name=<js>"status"</js>, 
	 * 			_enum=<js>"['AVAILABLE','PENDING','SOLD']"</js>,
	 * 		) PetStatus status
	 * 	) {...}
	 * </p>
	 */
	String[] _enum() default {};

	/**
	 * <mk>multipleOf</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * A numeric instance is valid if the result of the division of the instance by this keyword's value is an integer.
	 * <br>The value must be a valid JSON number.
	 * 
	 * <p>
	 * If validation is not met, the method call will throw a {@link BadRequest}.
	 * 
	 * <p>
	 * Only allowed for the following types: <js>"integer"</js>, <js>"number"</js>.
	 */
	String multipleOf() default "";
	
	//=================================================================================================================
	// Other
	//=================================================================================================================

	/**
	 * A serialized example of the parameter.
	 * 
	 * <p>
	 * This attribute defines a JSON representation of the value that is used by {@link BasicRestInfoProvider} to construct
	 * an example of the path.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a {@link JsonSerializer#DEFAULT_LAX Simple-JSON} object or plain text string.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};
	
	/**
	 * Free-form value for the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * This is a {@link JsonSerializer#DEFAULT_LAX Simple-JSON} object that makes up the swagger information for this field.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the Path object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Path</ja>(
	 * 		name=<js>"orderId"</js>, 
	 * 		description=<js>"ID of order to fetch"</js>, 
	 * 		maximum=<js>"1000"</js>, 
	 * 		minimum=<js>"101"</js>, 
	 * 		example=<js>"123"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Path</ja>({
	 * 		name=<js>"orderId"</js>,
	 * 		api={ 
	 * 			<js>"description: 'ID of order to fetch',"</js>, 
	 * 			<js>"maximum: 1000,"</js>, 
	 * 			<js>"minimum: 101,"</js>, 
	 * 			<js>"example: 123"</js>
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@Path</ja>({
	 * 		name=<js>"orderId"</js>,
	 * 		api=<js>"$L{orderIdSwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>orderIdSwagger</mk> = <mv>{ description: "ID of order to fetch", maximum: 1000, minimum: 101, example: 123 }</mv>
	 * </p>
	 * 
	 * <p>
	 * 	The reasons why you may want to use this field include:
	 * <ul>
	 * 	<li>You want to pull in the entire Swagger JSON definition for this field from an external source such as a properties file.
	 * 	<li>You want to add extra fields to the Swagger documentation that are not officially part of the Swagger specification.
	 * </ul>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Note that the only swagger field you can't specify using this value is <js>"name"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a {@link JsonSerializer#DEFAULT_LAX Simple-JSON} object.
	 * 	<li>
	 * 		Automatic validation is NOT performed on input based on attributes in this value.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Path</ja>(api=<js>"{description: 'ID of order to fetch'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Path</ja>(api=<js>"description: 'ID of order to fetch''"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Multiple lines are concatenated with newlines so that you can format the value to be readable.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
