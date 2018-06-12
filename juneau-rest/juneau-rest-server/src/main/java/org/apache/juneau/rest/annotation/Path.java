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

import org.apache.juneau.httppart.*;
import org.apache.juneau.rest.*;

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
	 * The default value for this parser is inherited from the servlet/method which defaults to {@link UonPartParser}.
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
	 * Note that per the Swagger specification, this value is required and must be a simple type (e.g. <js>"string"</js>, <js>"number"</js>...).
	 * <br>However, Juneau allows for arbitrarily-complex POJOs such as beans in form parameters using UON notation, this attribute can be left off in lieu of a {@link #schema()} 
	 * attribute.
	 * 
	 * <p> 
	 * The possible values are:
	 * <ul>
	 * 	<li><js>"string"</js>
	 * 	<li><js>"number"</js>
	 * 	<li><js>"integer"</js>
	 * 	<li><js>"boolean"</js>
	 * 	<li><js>"array"</js>
	 * 	<li><js>"file"</js>
	 * </ul>
	 * 
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String type() default "";
	
	/**
	 * <mk>format</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * The extending format for the previously mentioned <a href='https://swagger.io/specification/v2/#parameterType'>type</a>. 
	 * See <a href='https://swagger.io/specification/v2/#dataTypeFormat'>Data Type Formats</a> for further details.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain-text.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String format() default "";
	
	/**
	 * <mk>pattern</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a regular expression according to the ECMA 262 regular expression dialect.
	 * 	<li>
	 * 		This string SHOULD be a valid regular expression.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * <mk>collectionFormat</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <p>
	 * Determines the format of the array if type array is used. 
	 * <br>Possible values are:
	 * <ul>
	 * 	<li><js>"csv"</js> (default) - Comma-separated values (e.g. <js>"foo,bar"</js>).
	 * 	<li><js>"ssv"</js> - Space-separated values (e.g. <js>"foo bar"</js>).
	 * 	<li><js>"tsv"</js> - Tab-separated values (e.g. <js>"foo\tbar"</js>).
	 * 	<li><js>"pipes</js> - Pipe-separated values (e.g. <js>"foo|bar"</js>).
	 * </ul>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String collectionFormat() default "";

	/**
	 * <mk>maximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON number.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String maximum() default "";
	
	/**
	 * <mk>minimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON number.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String minimum() default "";

	/**
	 * <mk>multipleOf</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String multipleOf() default "";
	
	/**
	 * <mk>maxLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String maxLength() default "";
	
	/**
	 * <mk>minLength</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String minLength() default "";
	
	/**
	 * <mk>exclusiveMaximum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String exclusiveMaximum() default "";

	/**
	 * <mk>exclusiveMinimum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is numeric.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String exclusiveMinimum() default "";
	
	/**
	 * <mk>schema</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<ul>
	 * 			<li><code>schema=<js>"{type:'string',format:'binary'}"</js></code>
	 * 			<li><code>schema=<js>"type:'string',format:'binary'"</js></code>
	 * 		<ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	Schema schema() default @Schema;
	
	/**
	 * <mk>enum</mk> field of the Swagger <a class="doclink" href="https://swagger.io/specification/v2/#parameterObject">Parameter</a> object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON array or comma-delimited list.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] _enum() default {};
	
	/**
	 * TODO
	 * 
	 * <p>
	 * This attribute defines a JSON representation of the value that is used by {@link BasicRestInfoProvider} to construct
	 * an example of the path.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object or plain text string.
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
	 * This is a JSON object that makes up the swagger information for this field.
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
	 * 		The format is a Simplified JSON object.
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
