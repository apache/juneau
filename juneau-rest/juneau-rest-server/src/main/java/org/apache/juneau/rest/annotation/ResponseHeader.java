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
import org.apache.juneau.utils.*;

/**
 * Annotation that can be applied to parameters and types to denote them as an HTTP response headers.
 * 
 * <p>
 * This annotation can only be applied to subclasses of type {@link Value}.
 * 
 * <p>
 * The following examples show 3 different ways of accomplishing the same task of setting an HTTP header
 * on a response:
 * 
 * <p class='bcode'>
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
 * 			<ja>@ResponseHeader</ja>(name=<js>"X-Rate-Limit"</js>, type=<js>"integer"</js>, format=<js>"int32"</js>, description=<js>"Calls per hour allowed by the user."</js>, example=<js>"123"</js>) 
 * 			Value&lt;Integer&gt; rateLimit
 *		) {
 *		rateLimit.set(1000);
 *		...
 * 	}
 *
 *	<jc>// Example #3 - Use on type.</jc>
 * 	<ja>@RestMethod</ja>(...)
 * 	<jk>public void</jk> login(RateLimit rateLimit) {
 * 		rateLimit.set(1000);
 * 		...
 * 	} 
 * 
 * 	<ja>@ResponseHeader</ja>(name=<js>"X-Rate-Limit"</js>, type=<js>"integer"</js>, format=<js>"int32"</js>, description=<js>"Calls per hour allowed by the user."</js>, example=<js>"123"</js>)
 * 	<jk>public static class</jk> RateLimit <jk>extends</jk> Value&lt;Integer&gt; {}
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#headerObject">Swagger Specification &gt; Header Object</a>
 * </ul>
*/
@Documented
@Target({PARAMETER,TYPE})
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
	int code() default 0;
	
	/**
	 * The HTTP status (or statuses) of the response.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a comma-delimited list of HTTP status codes that this header applies to.
	 * 	<li>
	 * 		The default value is <js>"200"</js>.
	 * </ul>
	 */
	int[] codes() default {};

	/**
	 * The HTTP header name.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		At least one of {@link #name()} or {@link #value()} must be specified}.
	 * </ul>
	 */
	String name() default "";
	String value() default "";
	
//	/**
//	 * A synonym to {@link #name()}.
//	 * 
//	 * <p>
//	 * Useful if you only want to specify a header name.
//	 * 
//	 * <p class='bcode'>
//	 * 	<ja>@RestMethod</ja>(...)
//	 * 	<jk>public void</jk> login(<ja>@ResponseHeader</ja>(<js>"X-Rate-Limit"</js>) Value&lt;Integer&gt; rateLimit) {
//	 *		rateLimit.set(1000);
//	 *		...
//	 * 	}
//	 * </p>
//	 * 
//	 * <h5 class='section'>Notes:</h5>
//	 * <ul class='spaced-list'>
//	 * 	<li>
//	 * 		At least one of {@link #name()} or {@link #value()} must be specified}.
//	 * </ul>
//	 */
//	String value() default "";

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values.
	 * 
	 * <p>
	 * The default value for this parser is inherited from the servlet/method which defaults to {@link SimpleUonPartSerializer}.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;

	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/description</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] description() default {};
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/type</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		The possible values are:
	 * 		<ul>
	 * 			<li><js>"string"</js>
	 * 			<li><js>"number"</js>
	 * 			<li><js>"integer"</js>
	 * 			<li><js>"boolean"</js>
	 * 			<li><js>"array"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
	 */
	String type() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/format</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text:
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://swagger.io/specification/#dataTypes'>Swagger specification &gt; Data Types</a>
	 * </ul>
	 */
	String format() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/collectionFormat</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text:
	 * 	<li>
	 * 		The possible value are:
	 * 		<ul>
	 * 			<li><js>"csv"</js>
	 * 			<li><js>"ssv"</js>
	 * 			<li><js>"tsv"</js>
	 * 			<li><js>"pipes"</js>
	 * 			<li><js>"multi"</js>
	 * 		</ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String collectionFormat() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/$ref</code>.
	 * 
	 * <p>
	 * Denotes a reference to a definition object.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String $ref() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/maximum</code>.
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
	String maximum() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/minimum</code>.
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
	String minimum() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/multipleOf</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/maxLength</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/minLength</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/maxItems</code>.
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
	String maxItems() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/minItems</code>.
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
	String minItems() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/exclusiveMaximum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/exclusiveMinimum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/uniqueItems</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is boolean.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String uniqueItems() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/items</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	Items items() default @Items;

	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/default</code>.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is any JSON.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] _default() default {};
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/enum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers/{header-name}/x-example</code>.
	 * 
	 * <p>
	 * This attribute defines a JSON representation of the body value that is used by {@link BasicRestInfoProvider} to construct
	 * media-type-based examples of the header value.
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
	 * Free-form value for the swagger field <code>/paths/{path}/{method}/responses/{response}/headers/{header}</code>
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this Header object.
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
	 * 	<ja>@ResponseHeader</ja>(<js>"{description:'The new location of this resource'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@ResponseHeader</ja>(<js>"description:'The new location of this resource'"</js>)
	 * 		</p>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Values defined in this field supersede values pulled from the Swagger JSON file and are superseded by individual values defined on this annotation.
	 * </ul>
	 */
	String[] api() default {};
}
