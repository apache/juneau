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

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.*;

/**
 * Identifies servlet and REST call lifecycle events which cause {@link RestHook @RestHook}-annotated Java methods
 * to be called.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestLifecycleHooks}
 * </ul>
 */
public enum HookEvent {

	/**
	 * Identifies a method that is called immediately after the <c>HttpServlet.service(HttpServletRequest, HttpServletResponse)</c>
	 * method is called.
	 *
	 * <p>
	 * Note that you only have access to the raw request and response objects at this point.
	 *
	 * <p>
	 * The list of valid parameter types are as follows:
	 * <ul>
	 * 	<li>Servlet request/response objects:
	 * 		<ul>
	 * 			<li>{@link HttpServletRequest}
	 * 			<li>{@link HttpServletResponse}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<jc>// Add a request attribute to all incoming requests.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>START_CALL</jsf>)
	 * 		<jk>public void</jk> onStartCall(HttpServletRequest req) {
	 * 			req.setAttribute(<js>"foobar"</js>, <jk>new</jk> FooBar());
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple START_CALL methods can be defined on a class.
	 * 		<br>START_CALL methods on parent classes are invoked before START_CALL methods on child classes.
	 * 		<br>The order of START_CALL method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception.
	 * 		<br>{@link HttpException HttpExceptions} can be thrown to cause a particular HTTP error status code.
	 * 		<br>All other exceptions cause an HTTP 500 error status code.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	START_CALL,

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
	 * 		<ul>
	 * 			<li>{@link HttpServletRequest}
	 * 			<li>{@link HttpServletResponse}
	 * 		</ul>
	 * 	<li>Extended request/response objects:
	 * 		<ul>
	 * 			<li>{@link RestRequest}
	 * 			<li>{@link RestResponse}
	 * 		</ul>
	 * 	<li>Header objects:
	 * 		<ul>
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
	 * 		<ul>
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
	 * 			<li>{@link RequestBody}
	 * 			<li>{@link Config}
	 * 			<li>{@link UriContext}
	 * 			<li>{@link UriResolver}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<jc>// Log the incoming request.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>PRE_CALL</jsf>)
	 * 		<jk>public void</jk> onPreCall(Accept accept, Logger logger) {
	 * 			logger.fine(<js>"Accept {0} header found."</js>, accept);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple PRE_CALL methods can be defined on a class.
	 * 		<br>PRE_CALL methods on parent classes are invoked before PRE_CALL methods on child classes.
	 * 		<br>The order of PRE_CALL method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception.
	 * 		<br>{@link HttpException HttpExceptions} can be thrown to cause a particular HTTP error status code.
	 * 		<br>All other exceptions cause an HTTP 500 error status code.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * 	<li>
	 * 		It's advisable not to mess around with the HTTP body itself since you may end up consuming the body
	 * 		before the actual REST method has a chance to use it.
	 * </ul>
	 */
	PRE_CALL,

	/**
	 * Identifies a method that gets called immediately after the <ja>@RestOp</ja> annotated method gets called.
	 *
	 * <p>
	 * At this point, the output object returned by the method call has been set on the response, but
	 * {@link RestConverter RestConverters} have not yet been executed and the response has not yet been written.
	 *
	 * <p>
	 * The list of valid parameter types are the same as {@link #PRE_CALL}.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<jc>// Log the result of the request.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>POST_CALL</jsf>)
	 * 		<jk>public void</jk> onPostCall(RestResponse res, Logger logger) {
	 * 			logger.fine(<js>Output {0} was set on the response."</js>, res.getOutput());
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple POST_CALL methods can be defined on a class.
	 * 		<br>POST_CALL methods on parent classes are invoked before POST_CALL methods on child classes.
	 * 		<br>The order of POST_CALL method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception, although at this point it is too late to set an HTTP error status code.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	POST_CALL,

	/**
	 * Identifies a method that gets called right before we exit the servlet service method.
	 *
	 * <p>
	 * At this point, the output has been written and flushed.
	 *
	 * <p>
	 * The list of valid parameter types are as follows:
	 * <ul>
	 * 	<li>Servlet request/response objects:
	 * 		<ul>
	 * 			<li>{@link HttpServletRequest}
	 * 			<li>{@link HttpServletResponse}
	 * 		</ul>
	 * </ul>
	 *
	 * <p>
	 * The following attributes are set on the {@link HttpServletRequest} object that can be useful for logging purposes:
	 * <ul>
	 * 	<li><js>"Exception"</js> - Any exceptions thrown during the request.
	 * 	<li><js>"ExecTime"</js> - Execution time of the request.
	 * </ul>
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {
	 *
	 * 		<jc>// Log the time it took to execute the request.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>END_CALL</jsf>)
	 * 		<jk>public void</jk> onEndCall(HttpServletRequest req, Logger logger) {
	 * 			Exception e = (Exception)req.getAttribute(<js>"Exception"</js>);
	 * 			Long execTime = (Long)req.getAttribute(<js>"ExecTime"</js>);
	 * 			<jk>if</jk> (e != <jk>null</jk>)
	 * 				logger.warn(e, <js>"Request failed in {0}ms."</js>, execTime);
	 * 			<jk>else</jk>
	 * 				logger.fine(<js>"Request finished in {0}ms."</js>, execTime);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple END_CALL methods can be defined on a class.
	 * 		<br>END_CALL methods on parent classes are invoked before END_CALL methods on child classes.
	 * 		<br>The order of END_CALL method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception, although at this point it is too late to set an HTTP error status code.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	END_CALL,

	/**
	 * Identifies a method that gets called during servlet initialization.
	 *
	 * <p>
	 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the {@link RestContextBuilder}
	 * object has been created and initialized with the annotations defined on the class, but before the
	 * {@link RestContext} object has been created.
	 *
	 * <p>
	 * The only valid parameter type for this method is {@link RestContextBuilder} which can be used to configure the servlet.
	 *
	 * <p>
	 * An example of this is the <c>PetStoreResource</c> class that uses an init method to perform initialization
	 * of an internal data structure.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> PetStoreResource <jk>extends</jk> ResourceJena {
	 *
	 * 		<jc>// Our database.</jc>
	 * 		<jk>private</jk> Map&lt;Integer,Pet&gt; <jf>petDB</jf>;
	 *
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> onInit(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			<jc>// Load our database from a local JSON file.</jc>
	 * 			<jf>petDB</jf> = JsonParser.<jsf>DEFAULT</jsf>.parse(getClass().getResourceAsStream(<js>"PetStore.json"</js>), LinkedHashMap.<jk>class</jk>, Integer.<jk>class</jk>, Pet.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple INIT methods can be defined on a class.
	 * 		<br>INIT methods on parent classes are invoked before INIT methods on child classes.
	 * 		<br>The order of INIT method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception causing initialization of the servlet to fail.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	INIT,

	/**
	 * Identifies a method that gets called immediately after servlet initialization.
	 *
	 * <p>
	 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the {@link RestContext}
	 * object has been created.
	 *
	 * <p>
	 * The only valid parameter type for this method is {@link RestContext} which can be used to retrieve information
	 * about the servlet.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple POST_INIT methods can be defined on a class.
	 * 		<br>POST_INIT methods on parent classes are invoked before POST_INIT methods on child classes.
	 * 		<br>The order of POST_INIT method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception causing initialization of the servlet to fail.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	POST_INIT,

	/**
	 * Identical to {@link #POST_INIT} except the order of execution is child-resources first.
	 *
	 * <p>
	 * Use this annotation if you need to perform any kind of initialization on child resources before the parent resource.
	 *
	 * <p>
	 * This method is called from within the {@link Servlet#init(ServletConfig)} method after the {@link RestContext}
	 * object has been created and after the {@link #POST_INIT} methods have been called.
	 *
	 * <p>
	 * The only valid parameter type for this method is {@link RestContext} which can be used to retrieve information
	 * about the servlet.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple POST_INIT_CHILD_FIRST methods can be defined on a class.
	 * 		<br>POST_INIT_CHILD_FIRST methods on parent classes are invoked before POST_INIT_CHILD_FIRST methods on child classes.
	 * 		<br>The order of POST_INIT_CHILD_FIRST method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		The method can throw any exception causing initialization of the servlet to fail.
	 * </ul>
	 */
	POST_INIT_CHILD_FIRST,

	/**
	 * Identifies a method that gets called during servlet destroy.
	 *
	 * <p>
	 * This method is called from within the {@link Servlet#destroy()}.
	 *
	 * <p>
	 * The only valid parameter type for this method is {@link RestContext}, although typically no arguments will
	 * be specified.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(...)
	 * 	<jk>public class</jk> PetStoreResource <jk>extends</jk> ResourceJena {
	 *
	 * 		<jc>// Our database.</jc>
	 * 		<jk>private</jk> Map&lt;Integer,Pet&gt; <jf>petDB</jf>;
	 *
	 * 		<ja>@RestHook</ja>(<jsf>DESTROY</jsf>)
	 * 		<jk>public void</jk> onDestroy() {
	 * 			<jf>petDB</jf> = <jk>null</jk>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The method should return <jk>void</jk> although if it does return any value, the value will be ignored.
	 * 	<li>
	 * 		The method should be <jk>public</jk> although other visibilities are valid if the security manager allows it.
	 * 	<li>
	 * 		Static methods can be used.
	 * 	<li>
	 * 		Multiple DESTROY methods can be defined on a class.
	 * 		<br>DESTROY methods on child classes are invoked before DESTROY methods on parent classes.
	 * 		<br>The order of DESTROY method invocations within a class is alphabetical, then by parameter count, then by parameter types.
	 * 	<li>
	 * 		In general, destroy methods should not throw any exceptions, although if any are thrown, the stack trace will be
	 * 		printed to <c>System.err</c>.
	 * 	<li>
	 * 		Note that if you override a parent method, you probably need to call <code><jk>super</jk>.parentMethod(...)</code>.
	 * 		<br>The method is still considered part of the parent class for ordering purposes even though it's
	 * 		overridden by the child class.
	 * </ul>
	 */
	DESTROY
}
