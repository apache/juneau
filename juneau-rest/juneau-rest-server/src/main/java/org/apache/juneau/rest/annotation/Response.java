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

import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;

/**
 * Annotation that can be applied to exceptions and return types that identify the HTTP status they trigger and a description about the exception.
 * 
 * <p>
 * When applied to exception classes, this annotation defines Swagger information on non-200 return types.
 * 
 * <p>
 * The following example shows the <ja>@Response</ja> annotation used to define a subclass of {@link Unauthorized} for an invalid login attempt.
 * <br>Note that the annotation can be used on super and subclasses.
 * 
 * <p class='bcode'>
 * 	<jc>// Our REST method that throws an annotated exception.</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/user/login"</js>)
 * 	<jk>public</jk> Ok login(String username, String password) <jk>throws</jk> InvalidLogin {...}
 * 
 * 	<jc>// Our annotated exception.</jc>
 * 	<ja>@Response</ja>(description=<js>"Invalid username or password provided"</js>)
 * 	<jk>public class</jk> InvalidLogin <jk>extends</jk> Unauthorized {
 * 		<jk>public</jk> InvalidLogin() {
 * 			<jk>super</jk>(<js>"Invalid username or password."</js>);  <jc>// Message sent in response</jc>
 * 		}
 * 	}
 * 
 * 	<jc>// Parent exception class.</jc>
 * 	<jc>// Note that the default description is overridden above.</jc>
 * 	<ja>@Response</ja>(code=401, description=<js>"Unauthorized"</js>)
 * 	<jk>public class</jk> Unauthorized <jk>extends</jk> RestException { ... }
 * </p>
 * 
 * <p>
 * The attributes on this annotation are used to populate the generated Swagger for the method.
 * <br>In this case, the Swagger is populated with the following:
 * 
 * <p class='bcode'>
 * 	<js>'/user/login'</js>: {
 * 		get: {
 * 			responses: {
 * 				401: {
 * 					description: <js>'Invalid username or password provided'</js>
 * 				}
 * 			}
 * 		}
 * 	}
 * </p>
 * 
 * <p>
 * When applied to return type classes, this annotation defines Swagger information on the body of responses.
 * 
 * <p>
 * In the example above, we're using the {@link Ok} class which is defined like so:
 * 
 * <p class='bcode'>
 * 	<ja>@Response</ja>(code=200, example=<js>"'OK'"</js>)
 * 	<jk>public class</jk> Ok { ... }
 * </p>
 * 
 * <p>
 * Another example is {@link Redirect} which is defined like so:
 * 
 * <p class='bcode'>
 * 	<ja>@Response<ja>(
 * 		code=302, 
 * 		description=<js>"Redirect"</js>, 
 * 		headers={
 * 			<ja>@ResponseHeader</ja>(
 * 				name=<js>"Location"</js>,
 * 				type=<js>"string"</js>,
 * 				format=<js>"uri"</js>
 * 			)
 * 		}
 * 	)
 * 	<jk>public class</jk> Redirect { ... }
 * </p>
 * 
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://swagger.io/specification/v2/#responseObject">Swagger Specification &gt; Response Object</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,TYPE})
@Retention(RUNTIME)
@Inherited
public @interface Response {
	
	/**
	 * The HTTP response code.
	 * 
	 * The default value is <code>500</code> for exceptions and <code>200</code> for return types.
	 */
	int code() default 0;
	
	int value() default 0;
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/description</code>.
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
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/schema</code>.
	 */
	Schema schema() default @Schema;
	
	/**
	 * Defines the swagger field <code>/paths/{path}/{method}/responses/{status-code}/headers</code>.
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
	 * 			<li><code>headers=<js>"{Location:{description:'Redirect URI', type:'string', format:'uri'}}"</js></code>
	 * 			<li><code>headers=<js>"Location:{description:'Redirect URI', type:'string', format:'uri'}"</js></code>
	 * 		<ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	ResponseHeader[] headers() default {};
	
	/**
	 * Used for populating the swagger field <code>/paths/{path}/{method}/responses/{status-code}/x-examples</code>.
	 * 
	 * <p>
	 * The format of the example should be a JSON representation of the POJO being serialized.
	 * <br>This value is parsed from JSON into a POJO using the JSON parser, then serialized to the various supported
	 * media types for the method using the registered serializers to produce examples for all supported types.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is any JSON.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<ul>
	 * 			<li><code>example=<js>"{foo:'bar',baz:123}"</js></code>
	 * 			<li><code>example=<js>"foo:'bar',baz:123"</js></code>
	 * 		<ul>
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] example() default {};
	
	/**
	 * Used for populating the swagger field <code>/paths/{path}/{method}/responses/{status-code}/examples</code>.
	 * 
	 * <p>
	 * The format is a JSON object with keys as media types and values as string representations of the body response.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The format is a JSON object.
	 * 		<br>Multiple lines are concatenated with newlines.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 	<li>
	 * 		Supports <a class="doclink" href="../../../../../overview-summary.html#DefaultRestSvlVariables">initialization-time and request-time variables</a> 
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 */
	String[] examples() default {};
	
	/**
	 * Free-form value for the swagger field <code>/paths/{path}/{method}/responses/{response}</code>
	 * 
	 * <p>
	 * This is a JSON object that makes up the swagger information for this field.
	 * 
	 * <p>
	 * The following are completely equivalent ways of defining the swagger description of the Response object:
	 * <p class='bcode w800'>
	 * 	<jc>// Normal</jc>
	 * 	<ja>@Response</ja>(
	 * 		code=302, 
	 * 		description=<js>"Redirect"</js>, 
	 * 		headers={
	 * 			<ja>@ResponseHeader</ja>(
	 * 				name=<js>"Location"</js>,
	 * 				type=<js>"string"</js>,
	 * 				format=<js>"uri"</js>
	 * 			)
	 * 		}
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form</jc>
	 * 	<ja>@Response</ja>(
	 * 		code=302,
	 * 		api={
	 * 			<js>"description: 'Redirect',"</js>, 
	 * 			<js>"headers: {"</js>,
	 * 				<js>"Location: {"</js>,
	 * 					<js>"type: 'string',"</js>,
	 * 					<js>"format: 'uri'"</js>,
	 * 				<js>"}"</js>,
	 * 			<js>"}"</js>
	 * 		} 
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Free-form using variables</jc>
	 * 	<ja>@Response</ja>(
	 * 		code=302,
	 * 		api=<js>"$L{redirectSwagger}"</js>
	 * 	)
	 * </p>
	 * <p class='bcode w800'>
	 * 	<mc>// Contents of MyResource.properties</mc>
	 * 	<mk>redirectSwagger</mk> = <mv>{ description: "Redirect", headers: { Location: { type: "string", format: "uri" } } }</mv>
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
	 * 		Note that the only swagger field you can't specify using this value is <js>"code"</js> whose value needs to be known during servlet initialization.
	 * 	<li>
	 * 		The format is a Simplified JSON object.
	 * 	<li>
	 * 		The leading/trailing <code>{ }</code> characters are optional.
	 * 		<br>The following two example are considered equivalent:
	 * 		<p class='bcode w800'>
	 * 	<ja>@Response</ja>(api=<js>"{description: 'Redirect'}"</js>)
	 * 		</p>
	 * 		<p class='bcode w800'>
	 * 	<ja>@Response</ja>(api=<js>"description: 'Redirect''"</js>)
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
