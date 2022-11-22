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
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.matcher.*;

/**
 * Identifies a method that gets called immediately before the <ja>@RestOp</ja> annotated method gets called.
 *
 * <p>
 * At this point, the {@link RestRequest} object has been fully initialized, and all {@link RestGuard} and
 * {@link RestMatcher} objects have been called.
 *
 * <p>
 * The list of valid parameter types are as follows:
 * <ul>
 * 	<li>Servlet request/response objects:
 * 		<ul class='javatreec'>
 * 			<li>{@link HttpServletRequest}
 * 			<li>{@link HttpServletResponse}
 * 		</ul>
 * 	<li>Extended request/response objects:
 * 		<ul class='javatreec'>
 * 			<li>{@link RestRequest}
 * 			<li>{@link RestResponse}
 * 		</ul>
 * 	<li>Header objects:
 * 		<ul class='javatreec'>
 * 			<li>{@link Accept}
 * 			<li>{@link AcceptCharset}
 * 			<li>{@link AcceptEncoding}
 * 			<li>{@link AcceptLanguage}
 * 			<li>{@link Authorization}
 * 			<li>{@link CacheControl}
 * 			<li>{@link Connection}
 * 			<li>{@link ContentLength}
 * 			<li>{@link ContentType}
 * 			<li>{@link org.apache.juneau.http.header.Date}
 * 			<li>{@link Expect}
 * 			<li>{@link From}
 * 			<li>{@link Host}
 * 			<li>{@link IfMatch}
 * 			<li>{@link IfModifiedSince}
 * 			<li>{@link IfNoneMatch}
 * 			<li>{@link IfRange}
 * 			<li>{@link IfUnmodifiedSince}
 * 			<li>{@link MaxForwards}
 * 			<li>{@link Pragma}
 * 			<li>{@link ProxyAuthorization}
 * 			<li>{@link Range}
 * 			<li>{@link Referer}
 * 			<li>{@link TE}
 * 			<li>{@link UserAgent}
 * 			<li>{@link Upgrade}
 * 			<li>{@link Via}
 * 			<li>{@link Warning}
 * 			<li>{@link TimeZone}
 * 		</ul>
 * 	<li>Other objects:
 * 		<ul class='javatreec'>
 * 			<li>{@link ResourceBundle}
 * 			<li>{@link Messages}
 * 			<li>{@link InputStream}
 * 			<li>{@link ServletInputStream}
 * 			<li>{@link Reader}
 * 			<li>{@link OutputStream}
 * 			<li>{@link ServletOutputStream}
 * 			<li>{@link Writer}
 * 			<li>{@link RequestHeaders}
 * 			<li>{@link RequestQueryParams}
 * 			<li>{@link RequestFormParams}
 * 			<li>{@link RequestPathParams}
 * 			<li>{@link Logger}
 * 			<li>{@link RestContext}
 * 			<li>{@link org.apache.juneau.parser.Parser}
 * 			<li>{@link Locale}
 * 			<li>{@link Swagger}
 * 			<li>{@link RequestContent}
 * 			<li>{@link Config}
 * 			<li>{@link UriContext}
 * 			<li>{@link UriResolver}
 * 		</ul>
 * </ul>
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@Rest</ja>(...)
 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
 *
 * 		<jc>// Log the incoming request.</jc>
 * 		<ja>@RestPreCall</ja>
 * 		<jk>public void</jk> onPreCall(Accept <jv>accept</jv>, Logger <jv>logger</jv>) {
 * 			<jv>logger</jv>.fine(<js>"Accept {0} header found."</js>, <jv>accept</jv>);
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
 * 	<li class='note'>
 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
 * 	<li class='note'>
 * 		Static methods can be used.
 * 	<li class='note'>
 * 		Multiple pre-call methods can be defined on a class.
 * 		<br>Pre-call methods on parent classes are invoked before pre-call methods on child classes.
 * 		<br>The order of pre-call method invocations within a class is alphabetical, then by parameter count, then by parameter types.
 * 	<li class='note'>
 * 		The method can throw any exception.
 * 		<br>{@link BasicHttpException BasicHttpExceptions} can be thrown to cause a particular HTTP error status code.
 * 		<br>All other exceptions cause an HTTP 500 error status code.
 * 	<li class='note'>
 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
 * 		overridden by the child class.
 * 	<li class='note'>
 * 		It's advisable not to mess around with the HTTP content itself since you may end up consuming the content
 * 		before the actual REST method has a chance to use it.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LifecycleHooks">Lifecycle Hooks</a>
 * </ul>
 */
@Target({METHOD,TYPE})
@Retention(RUNTIME)
@Inherited
@Repeatable(RestPreCallAnnotation.Array.class)
public @interface RestPreCall {

	/**
	 * Dynamically apply this annotation to the specified methods.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jm.DynamicallyAppliedAnnotations">Dynamically Applied Annotations</a>
	 * </ul>
	 *
	 * @return The annotation value.
	 */
	String[] on() default {};
}
