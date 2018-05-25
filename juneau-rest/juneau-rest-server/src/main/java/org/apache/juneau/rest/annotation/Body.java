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

import java.io.*;
import java.lang.annotation.*;
import java.nio.charset.*;
import java.util.logging.*;

import org.apache.juneau.rest.*;

/**
 * REST request body annotation.
 * 
 * <p>
 * Identifies a POJO to be used as the body of an HTTP request.
 * 
 * <h5 class='section'>Examples:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Used on parameter</jc>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(<ja>@Body</ja> Pet pet) {...}
 * </p>
 * <p class='bcode w800'>
 * 	<jc>// Used on class</jc>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(Pet pet) {...}
 * 
 * 	<ja>@Body</ja>
 * 	<jk>public class</jk> Pet {...}
 * </p>
 * 
 * <p>
 * This is functionally equivalent to the following code...
 * <p class='bcode w800'>
 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
 * 	<jk>public void</jk> addPet(RestRequest req) {
 * 		Pet pet = req.getBody().asType(Pet.<jk>class</jk>);
 * 		...
 * 	}
 * </p>
 * 
 * <p>
 * This annotation can be applied to the following:
 * <ul class='spaced-list'>
 * 	<li>
 * 		Parameters on a  {@link RestMethod @RestMethod}-annotated method.
 * 	<li>
 * 		POJO classes.
 * </ul>
 * 
 * <p>
 * Any of the following types can be used (matched in the specified order):
 * <ol class='spaced-list'>
 * 	<li>
 * 		{@link Reader}
 * 		<br><ja>@Body</ja> annotation is optional (it's inferred from the class type).
 * 		<br><code>Content-Type</code> is always ignored.
 * 	<li>
 * 		{@link InputStream} 
 * 		<br><ja>@Body</ja> annotation is optional (it's inferred from the class type).
 * 		<br><code>Content-Type</code> is always ignored.
 * 	<li>
 * 		Any <a class='doclink' href='../../../../../overview-summary.html#juneau-marshall.PojoCategories'>parsable</a> POJO type.
 * 		<br><code>Content-Type</code> is required to identify correct parser.
 * 	<li>
 * 		Objects convertible from {@link Reader} by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(Reader in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(Reader in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromReader</jsm>(Reader in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * 	<li>
 * 		Objects convertible from {@link InputStream} by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(InputStream in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(InputStream in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromInputStream</jsm>(InputStream in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * 	<li>
 * 		Objects convertible from {@link String} (including <code>String</code> itself) by having one of the following non-deprecated methods:
 * 		<ul>
 * 			<li><code><jk>public</jk> T(String in) {...}</code> (e.g. {@link Integer}, {@link Boolean})
 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(String in) {...}</code> 
 * 			<li><code><jk>public static</jk> T <jsm>fromString</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>fromValue</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>valueOf</jsm>(String in) {...}</code> (e.g. enums)
 * 			<li><code><jk>public static</jk> T <jsm>parse</jsm>(String in) {...}</code> (e.g. {@link Level})
 * 			<li><code><jk>public static</jk> T <jsm>parseString</jsm>(String in) {...}</code>
 * 			<li><code><jk>public static</jk> T <jsm>forName</jsm>(String in) {...}</code> (e.g. {@link Class}, {@link Charset})
 * 			<li><code><jk>public static</jk> T <jsm>forString</jsm>(String in) {...}</code>
 * 		</ul>
 * 		<br><code>Content-Type</code> must not be present or match an existing parser so that it's not parsed as a POJO.
 * </ol>
 * 
 * 
 * <h5 class='section'>Notes:</h5>
 * <ul class='spaced-list'>
 * 	<li>
 * 		Annotation values are coalesced from multiple sources in the following order of precedence:
 * 		<ol>
 * 			<li><ja>@Body</ja> annotation on parameter.
 * 			<li><ja>@Body</ja> annotation on class.
 * 			<li><ja>@Body</ja> annotation on parent classes and interfaces.
 * 		</ol>
 * </ul>
 * 
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../../overview-summary.html#juneau-rest-server.Body">Overview &gt; juneau-rest-server &gt; @Body</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Body {
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/description</code>.
	 * 
	 * <p>
	 * A brief description of the body. This could contain examples of use.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(Pet input) {...}
	 * 
	 * 	<ja>@Body</ja>(description=<js>"Pet object to add to the store"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		TODO - Future support for MarkDown.
	 * </ul>
	 */
	String[] description() default {};
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/required</code>.
	 * 
	 * <p>
	 * 	Determines whether this parameter is mandatory. 
	 *  <br>The property MAY be included and its default value is false.
	 *  
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(required=<js>"true"</js>) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(Pet input) {...}
	 * 
	 * 	<ja>@Body</ja>(required=<js>"true"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
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
	String required() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/schema</code>.
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
	String[] schema() default {};
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/schema/$ref</code>.
	 * 
	 * <p>
	 * 	A JSON reference to the schema definition.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a <a href='https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03'>JSON Reference</a>.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String $ref() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/schema/format</code>.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>)
	 * 	<jk>public void</jk> setAge(
	 * 		<ja>@Body</ja>(type=<js>"integer"</js>, format=<js>"int32"</js>) String input
	 * 	) {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul class='doctree'>
	 * 	<li class='link'><a class='doclink' href='https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#dataTypeFormat'>Swagger specification &gt; Data Type Formats</a>
	 * </ul>
	 */
	String format() default "";
	
	
	
	
	
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/type</code>.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Used on parameter</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(
	 * 		<ja>@Body</ja>(type=<js>"object"</js>) Pet input
	 * 	) {...}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Used on class</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>POST</jsf>)
	 * 	<jk>public void</jk> addPet(Pet input) {...}
	 * 
	 * 	<ja>@Body</ja>(type=<js>"object"</js>)
	 * 	<jk>public class</jk> Pet {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		The possible values are:
	 * 		<ul>
	 * 			<li><js>"object"</js>
	 * 			<li><js>"string"</js>
	 * 			<li><js>"number"</js>
	 * 			<li><js>"integer"</js>
	 * 			<li><js>"boolean"</js>
	 * 			<li><js>"array"</js>
	 * 			<li><js>"file"</js>
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
	 * 
	 */
	String type() default "";
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/pattern</code>.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>)
	 * 	<jk>public void</jk> doPut(<ja>@Body</ja>(format=<js>"/\\w+\\.\\d+/"</js>) String input) {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is plain text.
	 * 	<li>
	 * 		This string SHOULD be a valid regular expression.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String pattern() default "";

	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/collectionFormat</code>.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(name=<jsf>PUT</jsf>)
	 * 	<jk>public void</jk> doPut(<ja>@Body</ja>(type=<js>"array"</js>,collectionFormat=<js>"csv"</js>) String input) {...}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/maximum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/minimum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/multipleOf</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/maxLength</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/minLength</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/maxItems</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/minItems</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/allowEmptyVals</code>.
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
	String allowEmptyValue() default "";

	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/exclusiveMaximum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/exclusiveMinimum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/uniqueItems</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/default</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/enum</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/items</code>.
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
	String[] items() default {};	
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/x-example</code>.
	 * 
	 * <p>
	 * This attribute defines a JSON representation of the body value that is used by {@link BasicRestInfoProvider} to construct
	 * media-type-based examples of the body of the request.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/parameters(in=body)/#/x-examples</code>.
	 * 
	 * <p>
	 * This is a JSON object whose keys are media types and values are string representations of that value.
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
	String[] examples() default {};
}
