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
package org.apache.juneau.rest;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;
import static org.apache.juneau.BasicIllegalArgumentException.*;
import static org.apache.juneau.Enablement.*;

import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.FormData;
import org.apache.juneau.http.annotation.HasFormData;
import org.apache.juneau.http.annotation.HasQuery;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.reshandlers.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc RestContext}
 * </ul>
 */
@ConfigurableContext(nocache=true)
public class RestContext extends BeanContext {

	/** Represents a null value for the {@link Rest#context()} annotation.*/
	@SuppressWarnings("javadoc")
	public static final class Null extends RestContext {
		public Null(RestContextBuilder builder) throws Exception {
			super(builder);
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestContext";

	/**
	 * Configuration property:  Disable allow body URL parameter.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_disableAllowBodyParam REST_disableAllowBodyParam}
	 * 	<li><b>Name:</b>  <js>"RestContext.disableAllowBodyParam.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestContext.disableAllowBodyParam</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_DISABLEALLOWBODYPARAM</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#disableAllowBodyParam()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#disableAllowBodyParam()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?body=(name='John%20Smith',age=45)
	 * </p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(disableAllowBodyParam=<js>"$C{REST/disableAllowBodyParam,true}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.disableAllowBodyParam();
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_disableAllowBodyParam</jsf>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.disableAllowBodyParam();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		<js>'body'</js> parameter name is case-insensitive.
	 * 	<li>
	 * 		Useful for debugging PUT and POST methods using only a browser.
	 * </ul>
	 */
	public static final String REST_disableAllowBodyParam = PREFIX + ".disableAllowBodyParam.b";

	/**
	 * Configuration property:  Allowed header URL parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_allowedHeaderParams REST_allowedHeaderParams}
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedHeaderParams.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited)
	 * 	<li><b>System property:</b>  <c>RestContext.allowedHeaderParams</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_ALLOWHEADERPARAMS</c>
	 * 	<li><b>Default:</b>  <js>"Accept,Content-Type"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#allowedHeaderParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#allowedHeaderParams(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, allows headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?Accept=text/json&amp;Content-Type=text/json
	 * </p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(allowedHeaderParams=<js>"Accept,Content-Type"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.allowedHeaderParams(<js>"Accept,Content-Type"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_allowedHeaderParams</jsf>, <js>"Accept,Content-Type"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.allowedHeaderParams(<js>"Accept,Content-Type"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Useful for debugging REST interface using only a browser so that you can quickly simulate header values
	 * 		in the URL bar.
	 * 	<li>
	 * 		Header names are case-insensitive.
	 * 	<li>
	 * 		Use <js>"*"</js> to allow any headers to be specified as URL parameters.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 */
	public static final String REST_allowedHeaderParams = PREFIX + ".allowedHeaderParams.s";

	/**
	 * Configuration property:  Allowed method headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_allowedMethodHeaders REST_allowedMethodHeaders}
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedMethodHeaders.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited)
	 * 	<li><b>System property:</b>  <c>RestContext.allowedMethodHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_ALLOWEDMETHODHEADERS</c>
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#allowedMethodHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#allowedMethodHeaders(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * A comma-delimited list of HTTP method names that are allowed to be passed as values in an <c>X-Method</c> HTTP header
	 * to override the real HTTP method name.
	 * <p>
	 * Allows you to override the actual HTTP method with a simulated method.
	 * <br>For example, if an HTTP Client API doesn't support <c>PATCH</c> but does support <c>POST</c> (because
	 * <c>PATCH</c> is not part of the original HTTP spec), you can add a <c>X-Method: PATCH</c> header on a normal
	 * <c>HTTP POST /foo</c> request call which will make the HTTP call look like a <c>PATCH</c> request in any of the REST APIs.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(allowedMethodHeaders=<js>"PATCH"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.allowedMethodHeaders(<js>"PATCH"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_allowedMethodHeaders</jsf>, <js>"PATCH"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.allowedMethodHeaders(<js>"PATCH"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Method names are case-insensitive.
	 * 	<li>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 */
	public static final String REST_allowedMethodHeaders = PREFIX + ".allowedMethodHeaders.s";

	/**
	 * Configuration property:  Allowed method URL parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_allowedMethodParams REST_allowedMethodParams}
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedMethodParams.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited)
	 * 	<li><b>System property:</b>  <c>RestContext.allowedMethodParams</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_ALLOWEDMETHODPARAMS</c>
	 * 	<li><b>Default:</b>  <js>"HEAD,OPTIONS"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#allowedMethodParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#allowedMethodParams(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> (case-insensitive) URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  /myservlet/myendpoint?method=OPTIONS
	 * </p>
	 * <p>
	 * 	Useful in cases where you want to simulate a non-GET request in a browser by simply adding a parameter.
	 * 	<br>Also useful if you want to construct hyperlinks to non-GET REST endpoints such as links to <c>OPTIONS</c>
	 * pages.
	 *
	 * <p>
	 * Note that per the {@doc ExtRFC2616.section9 HTTP specification}, special care should
	 * be taken when allowing non-safe (<c>POST</c>, <c>PUT</c>, <c>DELETE</c>) methods to be invoked through GET requests.
	 *
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(allowedMethodParams=<js>"HEAD,OPTIONS,PUT"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.allowedMethodParams(<js>"HEAD,OPTIONS,PUT"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_allowedMethodParams</jsf>, <js>"HEAD,OPTIONS,PUT"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.allowedMethodParams(<js>"HEAD,OPTIONS,PUT"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * 	<li>
	 * 		<js>'method'</js> parameter name is case-insensitive.
	 * 	<li>
	 * 		Use <js>"*"</js> to represent all methods.
	 * 	<li>
	 * 		Use <js>"NONE"</js> (case insensitive) to suppress inheriting a value from a parent class.
	 * </ul>
	 */
	public static final String REST_allowedMethodParams = PREFIX + ".allowedMethodParams.s";

	/**
	 * Configuration property:  REST call logger.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_callLogger REST_callLogger}
	 * 	<li><b>Name:</b>  <js>"RestContext.callLogger.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.rest.logging.RestLogger}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.rest.logging.RestLogger}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link #REST_callLoggerDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#callLogger()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#callLogger(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#callLogger(RestLogger)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the logger to use for logging of HTTP requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our customized logger.</jc>
	 * 	<jk>public class</jk> MyLogger <jk>extends</jk> BasicRestCallLogger {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {
	 * 			<jc>// Handle logging ourselves.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(callLogger=MyLogger.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.callLogger(MyLogger.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_callLogger</jsf>, MyLogger.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.callLogger(MyLogger.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The default call logger if not specified is {@link BasicRestLogger} unless overwritten by {@link #REST_callLoggerDefault}.
	 * 	<li>
	 * 		The resource class itself will be used if it implements the {@link RestLogger} interface and not
	 * 		explicitly overridden via this annotation.
	 * 	<li>
	 * 		The {@link RestServlet} and {@link BasicRest} classes implement the {@link RestLogger} interface with the same
	 * 		that gets used if not overridden by this annotation.
	 * 		<br>Subclasses can also alter the behavior by overriding these methods.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * </ul>
	 */
	public static final String REST_callLogger = PREFIX + ".callLogger.o";

	/**
	 * Configuration property:  Default REST call logger.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_callLoggerDefault REST_callLoggerDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.callLoggerDefault.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.rest.logging.RestLogger}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.rest.logging.RestLogger}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.logging.BasicRestLogger}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#callLoggerDefault(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#callLoggerDefault(RestLogger)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default logger to use if one is not specified.
	 * <p>
	 * This setting is inherited from the parent context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestLoggingAndDebugging}
	 * </ul>
	 */
	public static final String REST_callLoggerDefault = PREFIX + ".callLoggerDefault.o";

	/**
	 * Configuration property:  Children.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_children REST_children}
	 * 	<li><b>Name:</b>  <js>"RestContext.children.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Class|Object|{@link org.apache.juneau.rest.RestChild}&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#children()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#child(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#children(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#children(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines children of this resource.
	 *
	 * <p>
	 * A REST child resource is simply another servlet or object that is initialized as part of the ascendant resource and has a
	 * servlet path directly under the ascendant resource object path.
	 * <br>The main advantage to defining servlets as REST children is that you do not need to define them in the
	 * <c>web.xml</c> file of the web application.
	 * <br>This can cut down on the number of entries that show up in the <c>web.xml</c> file if you are defining
	 * large numbers of servlets.
	 *
	 * <p>
	 * Child resources must specify a value for {@link Rest#path() @Rest(path)} that identifies the subpath of the child resource
	 * relative to the ascendant path UNLESS you use the {@link RestContextBuilder#child(String, Object)} method to register it.
	 *
	 * <p>
	 * Child resources can be nested arbitrarily deep using this technique (i.e. children can also have children).
	 *
	 * <dl>
	 * 	<dt>Servlet initialization:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			A child resource will be initialized immediately after the ascendant servlet/resource is initialized.
	 * 			<br>The child resource receives the same servlet config as the ascendant servlet/resource.
	 * 			<br>This allows configuration information such as servlet initialization parameters to filter to child
	 * 			resources.
	 * 		</p>
	 * 	</dd>
	 * 	<dt>Runtime behavior:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			As a rule, methods defined on the <c>HttpServletRequest</c> object will behave as if the child
	 * 			servlet were deployed as a top-level resource under the child's servlet path.
	 * 			<br>For example, the <c>getServletPath()</c> and <c>getPathInfo()</c> methods on the
	 * 			<c>HttpServletRequest</c> object will behave as if the child resource were deployed using the
	 * 			child's servlet path.
	 * 			<br>Therefore, the runtime behavior should be equivalent to deploying the child servlet in the
	 * 			<c>web.xml</c> file of the web application.
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our child resource.</jc>
	 * 	<ja>@Rest</ja>(path=<js>"/child"</js>)
	 * 	<jk>public class</jk> MyChildResource {...}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(children={MyChildResource.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.children(MyChildResource.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_children</jsf>, MyChildResource.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			builder.child(<js>"/child"</js>, <jk>new</jk> MyChildResource());
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.children(MyChildResource.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as classes, instances are resolved using the registered {@link #REST_resourceResolver} which
	 * 		by default is {@link BasicRestResourceResolver} which requires the class have one of the following
	 * 		constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContextBuilder)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestChildren}
	 * </ul>
	 */
	public static final String REST_children = PREFIX + ".children.lo";

	/**
	 * Configuration property:  Client version header.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_clientVersionHeader REST_clientVersionHeader}
	 * 	<li><b>Name:</b>  <js>"RestContext.clientVersionHeader.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.clientVersionHeader</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_CLIENTVERSIONHEADER</c>
	 * 	<li><b>Default:</b>  <js>"X-Client-Version"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#clientVersionHeader()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#clientVersionHeader(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestMethod#clientVersion() @RestMethod(clientVersion)} annotation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(clientVersionHeader=<js>"$C{REST/clientVersionHeader,Client-Version}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.clientVersionHeader(<js>"Client-Version"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_clientVersionHeader</jsf>, <js>"Client-Version"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.clientVersionHeader(<js>"Client-Version"</js>);
	 * 		}
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3() {
	 * 		...
	 * 	}
	 * </p>
	 */
	public static final String REST_clientVersionHeader = PREFIX + ".clientVersionHeader.s";

	/**
	 * Configuration property:  Class-level response converters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_converters REST_converters}
	 * 	<li><b>Name:</b>  <js>"RestContext.converters.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestConverter}|Class&lt;{@link org.apache.juneau.rest.RestConverter}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#converters()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#converters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#converters(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#converters(RestConverter...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * <br>These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 * <br>The object passed into this converter is the object returned from the Java method or passed into
	 * the {@link RestResponse#setOutput(Object)} method.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * 	When multiple converters are specified, they're executed in the order they're specified in the annotation
	 * 	(e.g. first the results will be traversed, then the resulting node will be searched/sorted).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our converter.</jc>
	 * 	<jk>public class</jk> MyConverter <jk>implements</jk> RestConverter {
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Object convert(RestRequest req, Object o) {
	 * 			<jc>// Do something with object and return another object.</jc>
	 * 			<jc>// Or just return the same object for a no-op.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(converters={MyConverter.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.converters(MyConverter.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_converters</jsf>, MyConverter.<jk>class</jk>);
	 *
	 * 			<jc>// Pass in an instance instead.</jc>
	 * 			builder.converters(<jk>new</jk> MyConverter());
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.converters(MyConverter.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link Traversable} - Allows URL additional path info to address individual elements in a POJO tree.
	 * 	<li class='jc'>{@link Queryable} - Allows query/view/sort functions to be performed on POJOs.
	 * 	<li class='jc'>{@link Introspectable} - Allows Java public methods to be invoked on the returned POJOs.
	 * 	<li class='link'>{@doc RestConverters}
	 * </ul>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_converters = PREFIX + ".converters.lo";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_debug REST_debug}
	 * 	<li><b>Name:</b>  <js>"RestContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Enablement}
	 * 	<li><b>System property:</b>  <c>RestContext.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  {@link #REST_debugDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#debug()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * 	<li>
	 * 		Request/response messages are automatically logged always or per request.
	 * 	<li>
	 * 		The default can be overwritten by {@link #REST_debugDefault}.
	 * </ul>
	 */
	public static final String REST_debug = PREFIX + ".debug.s";

	/**
	 * Configuration property:  Default debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_debugDefault REST_debugDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Enablement}
	 * 	<li><b>System property:</b>  <c>RestContext.debugDefault</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_DEBUGDEFAULT</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Enablement#NEVER}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#debugDefault(Enablement)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default value for the {@link #REST_debug} setting.
	 * <p>
	 * This setting is inherited from parent contexts.
	 */
	public static final String REST_debugDefault = PREFIX + ".debugDefault.s";

	/**
	 * Configuration property:  Debug mode on specified classes/methods.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_debugOn REST_debugOn}
	 * 	<li><b>Name:</b>  <js>"RestContext.debugOn.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited)
	 * 	<li><b>System property:</b>  <c>RestContext.debugOn</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_DEBUGON</c>
	 * 	<li><b>Default:</b>  Empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#debugOn()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#debugOn(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * 	<li>
	 * 		Request/response messages are automatically logged always or per request.
	 * </ul>
	 */
	public static final String REST_debugOn = PREFIX + ".debugOn.s";

	/**
	 * Configuration property:  Default character encoding.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_defaultCharset REST_defaultCharset}
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.defaultCharset</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_DEFAULTCHARSET</c>
	 * 	<li><b>Default:</b>  <js>"utf-8"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#defaultCharset()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#defaultCharset(String)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#defaultCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(defaultCharset=<js>"$C{REST/defaultCharset,US-ASCII}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.defaultCharset(<js>"US-ASCII"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_defaultCharset</jsf>, <js>"US-ASCII"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.defaultCharset(<js>"US-ASCII"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(defaultCharset=<js>"UTF-16"</js>)
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 */
	public static final String REST_defaultCharset = PREFIX + ".defaultCharset.s";

	/**
	 * Configuration property:  Compression encoders.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_encoders REST_encoders}
	 * 	<li><b>Name:</b>  <js>"RestContext.encoders.o"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.encoders.Encoder}|Class&lt;{@link org.apache.juneau.encoders.Encoder}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#encoders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#encoders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#encoders(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.encoders(GzipEncoder.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_encoders</jsf>, GzipEncoder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.encoders(GzipEncoder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(encoders={MySpecialEncoder.<jk>class</jk>}, inherit={<js>"ENCODERS"</js>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestEncoders}
	 * </ul>
	 */
	public static final String REST_encoders = PREFIX + ".encoders.lo";

	/**
	 * Configuration property:  File finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_fileFinder REST_fileFinder}
	 * 	<li><b>Name:</b>  <js>"RestContext.fileFinder.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.cp.FileFinder}
	 * 	<li><b>Default:</b>  {@link #REST_fileFinderDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#fileFinder()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinder(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinder(FileFinder)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContext#createFileFinder()}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.BasicRest#createFileFinder()}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.BasicRestServlet#createFileFinder()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files from the classpath for a variety of purposes including:
	 * <ul>
	 * 	<li>Resolution of {@link FileVar $F} variable contents.
	 * </ul>
	 *
	 * <p>
	 * The file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getFileFinder()}
	 * 	<li class='jm'>{@link RestRequest#getFileFinder()}
	 * </ul>
	 *
	 * <p>
	 * The file finder is instantiated via the {@link RestContext#createFileFinder()} method which in turn instantiates
	 * based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself if it's an instance of {@link FileFinder}.
	 * 	<li>Looks for {@link #REST_fileFinder} setting.
	 * 	<li>Looks for a public <c>createFileFinder()</> method on the resource class with an optional {@link RestContext} argument.
	 * 		<br>Note that the {@link BasicRest#createFileFinder()} and {@link BasicRestServlet#createFileFinder()} methods are implemented
	 * 		to automatically look for injected beans of type {@link FileFinder} allowing preconfigured file finders to be
	 * 		defined in a Spring configuration class.
	 * 	<li>Instantiates the default file finder as specified via {@link #REST_fileFinderDefault}.
	 * 	<li>Instantiates a {@link BasicFileFinder} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a file finder that looks for files in the /files working subdirectory, but overrides the find()
	 * 	// method for special handling of special cases.</jc>
	 * 	<jk>public class</jk> MyFileFinder <jk>extends</jk> FileFinder {
	 *
	 * 		<jk>public</jk> MyFileFinder() {
	 * 			<jk>super</jk>(
	 * 				<jk>new</jk> FileFinderBuilder()
	 * 					.dir(<js>"/files"</js>)
	 *			);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// FileFinder</jc>
	 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
	 * 			<jc>// Do special handling or just call super.find().</jc>
	 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(fileFinder=MyFileFinder.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Created via createFileFinder() method.</jc>
	 * 		<jk>public</jk> FileFinder createFileFinder(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MyFileFinder();
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_fileFinder</jsf>, MyFileFinder.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			<jv>builder</jv>.fileFinder(<jk>new</jk> MyFileFinder());
	 * 		}
	 *
	 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.fileFinder(MyFileFinder.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Create a REST method that uses the file finder.</jc>
	 * 		<ja>@RestMethod</ja>
	 * 		<jk>public</jk> InputStream getFoo(RestRequest <jv>req</jv>) {
	 * 			<jk>return</jk> <jv>req</jv>.getFileFinder().getStream(<js>"foo.json"</js>).orElseThrow(NotFound::<jk>new</jk>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_fileFinder = PREFIX + ".fileFinder.o";

	/**
	 * Configuration property:  Default file finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_fileFinderDefault REST_fileFinderDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.fileFinderDefault.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.cp.FileFinder}
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.BasicFileFinder}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinderDefault(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#fileFinderDefault(FileFinder)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default file finder to use if not specified.
	 * <p>
	 * This setting is inherited from the parent context.
	 */
	public static final String REST_fileFinderDefault = PREFIX + ".fileFinderDefault.o";

	/**
	 * Configuration property:  Class-level guards.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_guards REST_guards}
	 * 	<li><b>Name:</b>  <js>"RestContext.guards.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestGuard}|Class&lt;{@link org.apache.juneau.rest.RestGuard}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#guards()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#guards()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#guards(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#guards(RestGuard...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * <br>These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * If multiple guards are specified, <b>ALL</b> guards must pass.
	 * <br>Note that this is different than matchers where only ONE matcher needs to pass.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Define a guard that only lets Billy make a request.</jc>
	 * 	<jk>public</jk> BillyGuard <jk>extends</jk> RestGuard {
	 * 		<ja>@Override</ja>
	 * 		<jk>public boolean</jk> isRequestAllowed(RestRequest req) {
	 * 			<jk>return</jk> req.getUserPrincipal().getName().equals(<js>"Billy"</js>);
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(guards={BillyGuard.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.guards(BillyGuard.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_guards</jsf>, BillyGuard.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.guards(BillyGuard.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(guards={SomeOtherGuard.<jk>class</jk>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestGuards}
	 * </ul>
	 */
	public static final String REST_guards = PREFIX + ".guards.lo";

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_infoProvider REST_infoProvider}
	 * 	<li><b>Name:</b>  <js>"RestContext.infoProvider.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.rest.RestInfoProvider}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.rest.RestInfoProvider}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.BasicRestInfoProvider}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#infoProvider()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#infoProvider(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#infoProvider(RestInfoProvider)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our customized info provider.</jc>
	 * 	<jc>// Extend from the default implementation and selectively override values.</jc>
	 * 	<jk>public class</jk> MyRestInfoProvider <jk>extends</jk> BasicRestInfoProvider {
	 *
	 * 		<jc>// Must provide this constructor!</jc>
	 * 		<jk>public</jk> MyRestInfoProvider(RestContext context) {
	 * 			<jk>super</jk>(context);
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Swagger getSwaggerFromFile(RestRequest req) <jk>throws</jk> RestException {
	 * 			<jc>// Provide our own method of retrieving swagger from file system.</jc>
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Swagger getSwagger(RestRequest req) <jk>throws</jk> RestException {
	 * 			Swagger s = <jk>super</jk>.getSwagger(req);
	 * 			<jc>// Made inline modifications to generated swagger.</jc>
	 * 			<jk>return</jk> s;
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> String getSiteName(RestRequest req) {
	 * 			<jc>// Override the site name.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(infoProvider=MyRestInfoProvider.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.infoProvider(MyRestInfoProvider.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_infoProvider</jsf>, MyRestInfoProvider.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.infoProvider(MyRestInfoProvider.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		The default info provider if not specified is {@link BasicRestInfoProvider}.
	 * 	<li>
	 * 		The resource class itself will be used if it implements the {@link RestInfoProvider} interface and not
	 * 		explicitly overridden via this annotation.
	 * 	<li>
	 * 		The{@link RestServlet} and {@link BasicRest} classes implement the {@link RestInfoProvider} interface with the same
	 * 		functionality as {@link BasicRestInfoProvider} that gets used if not overridden by this annotation.
	 * 		<br>Subclasses can also alter the behavior by overriding these methods.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_infoProvider = PREFIX + ".infoProvider.o";

	/**
	 * Configuration property:  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_maxInput REST_maxInput}
	 * 	<li><b>Name:</b>  <js>"RestContext.maxInput.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.maxInput</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_MAXINPUT</c>
	 * 	<li><b>Default:</b>  <js>"100M"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#maxInput()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#maxInput()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#maxInput(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(maxInput=<js>"$C{REST/maxInput,10M}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.maxInput(<js>"10M"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_maxInput</jsf>, <js>"10M"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.maxInput(<js>"10M"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(maxInput=<js>"10M"</js>)
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		String value that gets resolved to a <jk>long</jk>.
	 * 	<li>
	 * 		Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>
	 * 		A value of <js>"-1"</js> can be used to represent no limit.
	 * </ul>
	 */
	public static final String REST_maxInput = PREFIX + ".maxInput.s";

	/**
	 * Configuration property:  Messages.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_messages REST_messages}
	 * 	<li><b>Name:</b>  <js>"RestContext.messages.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.utils.Tuple2}&lt;Class,String&gt;&gt;</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#messages()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#messages(String)},
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the location of the resource bundle for this class if it's different from the class name.
	 *
	 * <p>
	 * By default, the resource bundle name is assumed to match the class name.  For example, given the class
	 * <c>MyClass.java</c>, the resource bundle is assumed to be <c>MyClass.properties</c>.  This property
	 * allows you to override this setting to specify a different location such as <c>MyMessages.properties</c> by
	 * specifying a value of <js>"MyMessages"</js>.
	 *
	 * <p>
	 * 	Resource bundles are searched using the following base name patterns:
	 * 	<ul>
	 * 		<li><js>"{package}.{name}"</js>
	 * 		<li><js>"{package}.i18n.{name}"</js>
	 * 		<li><js>"{package}.nls.{name}"</js>
	 * 		<li><js>"{package}.messages.{name}"</js>
	 * 	</ul>
	 *
	 * <p>
	 * This annotation is used to provide request-localized (based on <c>Accept-Language</c>) messages for the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li class='jm'>{@link RestContext#getMessages() RestContext.getMessages()}
	 * </ul>
	 *
	 * <p>
	 * Request-localized messages are also available by passing either of the following parameter types into your Java method:
	 * <ul class='javatree'>
	 * 	<li class='jc'>{@link ResourceBundle} - Basic Java resource bundle.
	 * 	<li class='jc'>{@link Messages} - Extended resource bundle with several convenience methods.
	 * </ul>
	 *
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
	 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
	 * absolute path like <js>"com.foo.sample.nls.Messages"</js>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<cc># Contents of org/apache/foo/nls/MyMessages.properties</cc>
	 *
	 * 	<ck>HelloMessage</ck> = <cv>Hello {0}!</cv>
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Contents of org/apache/foo/MyResource.java</jc>
	 *
	 * 	<ja>@Rest</ja>(messages=<js>"nls/MyMessages"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 		<ja>@RestMethod</ja>(method=<js>"GET"</js>, path=<js>"/hello/{you}"</js>)
	 * 		<jk>public</jk> Object helloYou(RestRequest <jv>req</jv>, Messages <jv>messages</jv>, <ja>@Path</ja>(<js>"name"</js>) String <jv>you</jv>) {
	 * 			String <jv>s</jv>;
	 *
	 * 			<jc>// Get it from the RestRequest object.</jc>
	 * 			<jv>s</jv> = <jv>req</jv>.getMessage(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jc>// Or get it from the method parameter.</jc>
	 * 			<jv>s</jv> = <jv>messages</jv>.getString(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jc>// Or get the message in a locale different from the request.</jc>
	 * 			<jv>s</jv> = <jv>messages</jv>.forLocale(Locale.<jsf>UK</jsf>).getString(<js>"HelloMessage"</js>, <jv>you</jv>);
	 *
	 * 			<jk>return</jk> <jv>s</jv>;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>Mappings are cumulative from super classes.
	 * 		<br>Therefore, you can find and retrieve messages up the class-hierarchy chain.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jc'>{@link Messages}
	 * 	<li class='link'>{@doc RestMessages}
	 * </ul>
	 */
	public static final String REST_messages = PREFIX + ".messages.lo";

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_paramResolvers REST_paramResolvers}
	 * 	<li><b>Name:</b>  <js>"RestContext.paramResolvers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestMethodParam}|Class&lt;{@link org.apache.juneau.rest.RestMethodParam}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#paramResolvers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#paramResolvers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#paramResolvers(RestMethodParam...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <c>RestRequest</c>, <c>Accept</c>, <c>Reader</c>).
	 * This setting allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * For example, if you want to pass in instances of <c>MySpecialObject</c> to your Java method, define
	 * the following resolver:
	 * <p class='bcode w800'>
	 * 	<jc>// Define a parameter resolver for resolving MySpecialObject objects.</jc>
	 * 	<jk>public class</jk> MyRestParam <jk>extends</jk> RestMethodParam {
	 *
	 * 		<jc>// Must have no-arg constructor!</jc>
	 * 		<jk>public</jk> MyRestParam() {
	 * 			<jc>// First two parameters help with Swagger doc generation.</jc>
	 * 			<jk>super</jk>(<jsf>QUERY</jsf>, <js>"myparam"</js>, MySpecialObject.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// The method that creates our object.
	 * 		// In this case, we're taking in a query parameter and converting it to our object.</jc>
	 * 		<jk>public</jk> Object resolve(RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MySpecialObject(req.getQuery().get(<js>"myparam"</js>));
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(paramResolvers=MyRestParam.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.paramResolvers(MyRestParam.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_paramResolver</jsf>, MyRestParam.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.paramResolvers(MyRestParam.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Now pass it into your method.</jc>
	 * 		<ja>@RestMethod</ja>(...)
	 * 		<jk>public</jk> Object doMyMethod(MySpecialObject mySpeciaObject) {
	 * 			<jc>// Do something with it.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * 	<li>
	 * 		Refer to {@link RestMethodParam} for the list of predefined parameter resolvers.
	 * </ul>
	 */
	public static final String REST_paramResolvers = PREFIX + ".paramResolvers.lo";

	/**
	 * Configuration property:  Parsers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_parsers REST_parsers}
	 * 	<li><b>Name:</b>  <js>"RestContext.parsers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.parser.Parser}|Class&lt;{@link org.apache.juneau.parser.Parser}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#parsers()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#parsers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsers(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#parsersReplace(Object...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level parsers to this resource.
	 *
	 * <p>
	 * Parsers are used to convert the body of HTTP requests into POJOs.
	 * <br>Any of the Juneau framework parsers can be used in this setting.
	 * <br>The parser selected is based on the request <c>Content-Type</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Parser#getMediaTypes()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(parsers={JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.parsers(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but use pre-instantiated parsers.</jc>
	 * 			builder.parsers(JsonParser.<jsf>DEFAULT</jsf>, XmlParser.<jsf>DEFAULT</jsf>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_parsers</jsf>, JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.parsers(JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(parsers={HtmlParser.<jk>class</jk>})
	 * 		<jk>public</jk> Object myMethod(<ja>@Body</ja> MyPojo myPojo) {
	 * 			<jc>// Do something with your parsed POJO.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>
	 * 		Typically, you'll want your resource to extend directly from {@link BasicRestServlet} which comes
	 * 		preconfigured with the following parsers:
	 * 		<ul>
	 * 			<li class='jc'>{@link JsonParser}
	 * 			<li class='jc'>{@link XmlParser}
	 * 			<li class='jc'>{@link HtmlParser}
	 * 			<li class='jc'>{@link UonParser}
	 * 			<li class='jc'>{@link UrlEncodingParser}
	 * 			<li class='jc'>{@link MsgPackParser}
	 * 			<li class='jc'>{@link PlainTextParser}
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestParsers}
	 * </ul>
	 */
	public static final String REST_parsers = PREFIX + ".parsers.lo";

	/**
	 * Configuration property:  HTTP part parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_partParser REST_partParser}
	 * 	<li><b>Name:</b>  <js>"RestContext.partParser.o"</js>
	 * 	<li><b>Data type:</b>  <c>{@link org.apache.juneau.httppart.HttpPartParser}|Class&lt;{@link org.apache.juneau.httppart.HttpPartParser}&gt;</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiParser}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#partParser()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partParser(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The default value is {@link OpenApiParser} which allows for both plain-text and URL-Encoded-Object-Notation values.
	 * <br>If your parts contain text that can be confused with UON (e.g. <js>"(foo)"</js>), you can switch to
	 * {@link SimplePartParser} which treats everything as plain text.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(partParser=SimplePartParser.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.partParser(SimplePartParser.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_partParser</jsf>, SimplePartParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.partParser(SimplePartParser.<jk>class</jk>);
	 * 		}
	 *
	 * 		<ja>@RestMethod</ja>(...)
	 * 		<jk>public</jk> Object myMethod(<ja>@Header</ja>(<js>"My-Header"</js>) MyParsedHeader h, <ja>@Query</ja>(<js>"myquery"</js>) MyParsedQuery q) {
	 * 			<jc>// Do something with your parsed parts.</jc>
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partParser = PREFIX + ".partParser.o";

	/**
	 * Configuration property:  HTTP part serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_partSerializer REST_partSerializer}
	 * 	<li><b>Name:</b>  <js>"RestContext.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.httppart.HttpPartSerializer}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.httppart.HttpPartSerializer}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.oapi.OpenApiSerializer}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#partSerializer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partSerializer(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <p>
	 * The default value is {@link OpenApiSerializer} which serializes based on OpenAPI rules, but defaults to UON notation for beans and maps, and
	 * plain text for everything else.
	 * <br>Other options include:
	 * <ul>
	 * 	<li class='jc'>{@link SimplePartSerializer} - Always serializes to plain text.
	 * 	<li class='jc'>{@link UonSerializer} - Always serializers to UON.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(partSerializer=SimplePartSerializer.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.partSerializer(SimplePartSerializer.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_partSerializer</jsf>, SimplePartSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.partSerializer(SimplePartSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<ja>@RestMethod</ja>(...)
	 * 		<jk>public</jk> Object myMethod(RestResponse res) {
	 * 			<jc>// Set a header to a POJO.</jc>
	 * 			res.setHeader(<js>"My-Header"</js>, <jk>new</jk> MyPojo());
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partSerializer = PREFIX + ".partSerializer.o";

	/**
	 * Configuration property:  Resource path.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_path REST_path}
	 * 	<li><b>Name:</b>  <js>"RestContext.path.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.path.</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_PATH</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#path()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#path(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the URL subpath relative to the ascendant resource.
	 *
	 * <p>
	 * This setting is critical for the routing of HTTP requests from ascendant to child resources.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(path=<js>"/myResource"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.path(<js>"/myResource"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_path</jsf>, <js>"/myResource"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.path(<js>"/myResource"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * <ul class='notes'>
	 * 	<li>
	 * 		This annotation is ignored on top-level servlets (i.e. servlets defined in <c>web.xml</c> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>
	 * 		Typically, this setting is only applicable to resources defined as children through the
	 * 		{@link Rest#children() @Rest(children)} annotation.
	 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 * 	<li>
	 * 		Slashes are trimmed from the path ends.
	 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
	 * 	<li>
	 * 		This path is available through the following method:
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContext#getPath() RestContext.getPath()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String REST_path = PREFIX + ".path.s";

	/**
	 * Configuration property:  Render response stack traces in responses.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_renderResponseStackTraces REST_renderResponseStackTraces}
	 * 	<li><b>Name:</b>  <js>"RestContext.renderResponseStackTraces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RestContext.renderResponseStackTraces</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_RENDERRESPONSESTACKTRACES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#renderResponseStackTraces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(renderResponseStackTraces=<jk>true</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.renderResponseStackTraces();
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_renderResponseStackTraces</jsf>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.renderResponseStackTraces();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 * 	<li>
	 * 		This setting is available through the following method:
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContext#isRenderResponseStackTraces() RestContext.isRenderResponseStackTraces()}
	 * 		</ul>
	 * 		That method is used by {@link #handleError(RestCall, Throwable)}.
	 * </ul>
	 */
	public static final String REST_renderResponseStackTraces = PREFIX + ".renderResponseStackTraces.b";

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_reqAttrs REST_reqAttrs}
	 * 	<li><b>Name:</b>  <js>"RestContext.reqAttrs.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.reqAttrs</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_REQATTRS</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#reqAttrs()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#reqAttrs()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#reqAttrs(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#reqAttr(String,Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Strings are in the format <js>"Name: value"</js>.
	 * 	<li>
	 * 		Affects values returned by the following methods:
	 * 		<ul>
	 * 			<li class='jm'>{@link RestRequest#getAttribute(String)}.
	 * 			<li class='jm'>{@link RestRequest#getAttributes()}.
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(reqAttrs={<js>"Foo: bar"</js>, <js>"Baz: $C{REST/myAttributeValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.attr(<js>"Foo"</js>, <js>"bar"</js>);
	 * 				.attr(<js>"Baz: true"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_reqAttrs</jsf>, <js>"Foo"</js>, <js>"bar"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.reqAttr(<js>"Foo"</js>, <js>"bar"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(reqAttrs={<js>"Foo: bar"</js>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 */
	public static final String REST_reqAttrs = PREFIX + ".reqAttrs.smo";

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_reqHeaders REST_reqHeaders}
	 * 	<li><b>Name:</b>  <js>"RestContext.reqHeaders.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.reqHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_REQHEADERS</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#reqHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#reqHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#reqHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#reqHeaders(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Strings are in the format <js>"Header-Name: header-value"</js>.
	 * 	<li>
	 * 		Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>
	 * 		The most useful reason for this annotation is to provide a default <c>Accept</c> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(reqHeaders={<js>"Accept: application/json"</js>, <js>"My-Header: $C{REST/myHeaderValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.reqHeader(<js>"Accept"</js>, <js>"application/json"</js>);
	 * 				.reqHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_reqHeaders</jsf>, <js>"Accept"</js>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.reqHeader(<js>"Accept"</js>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(reqHeaders={<js>"Accept: text/xml"</js>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 */
	public static final String REST_reqHeaders = PREFIX + ".reqHeaders.smo";

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_resHeaders REST_resHeaders}
	 * 	<li><b>Name:</b>  <js>"RestContext.resHeaders.omo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.resHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_RESHEADERS</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#resHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#resHeader(String,Object)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#resHeaders(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Strings are in the format <js>"Header-Name: header-value"</js>.
	 * 	<li>
	 * 		This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of
	 * 		the Java methods.
	 * 	<li>
	 * 		The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(resHeaders={<js>"Content-Type: $C{REST/defaultContentType,text/plain}"</js>,<js>"My-Header: $C{REST/myHeaderValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.resHeader(<js>"Content-Type"</js>, <js>"text/plain"</js>);
	 * 				.resHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder
	 * 				.addTo(<jsf>REST_resHeaders</jsf>, <js>"Accept"</js>, <js>"application/json"</js>);
	 * 				.addTo(<jsf>REST_resHeaders</jsf>, <js>"My-Header"</js>, <js>"foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.resHeader(<js>"Content-Type"</js>, <js>"text/plain"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_resHeaders = PREFIX + ".resHeaders.omo";

	/**
	 * Configuration property:  REST resource resolver.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_resourceResolver REST_resourceResolver}
	 * 	<li><b>Name:</b>  <js>"RestContext.resourceResolver.o"</js>
	 * 	<li><b>Data type:</b>
	 * 		<ul>
	 * 			<li>{@link org.apache.juneau.rest.RestResourceResolver}
	 * 			<li><c>Class&lt;{@link org.apache.juneau.rest.RestResourceResolver}&gt;</c>
	 * 		</ul>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.BasicRestResourceResolver}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#resourceResolver()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#resourceResolver(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#resourceResolver(RestResourceResolver)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The resolver used for resolving instances of child resources.
	 *
	 * <p>
	 * Can be used to provide customized resolution of REST resource class instances (e.g. resources retrieve from Spring).
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our custom resource resolver. </jc>
	 * 	<jk>public class</jk> MyResourceResolver <jk>extends</jk> RestResourceResolverSimple {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> Object resolve(Class&lt;?&gt; resourceType, RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			Object resource = <jsm>findOurResourceSomehow</jsm>(resourceType);
	 *
	 * 			<jc>// If we can't resolve it, use default resolution.</jc>
	 * 			<jk>if</jk> (resource == <jk>null</jk>)
	 * 				resource = <jk>super</jk>.resolve(resourceType, builder);
	 *
	 * 			<jk>return</jk> resource;
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(resourceResolver=MyResourceResolver.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.resourceResolver(MyResourceResolver.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_resourceResolver</jsf>, MyResourceResolver.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.resourceResolver(MyResourceResolver.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Unless overridden, resource resolvers are inherited from ascendant resources.
	 * 	<li>
	 * 		The resource class itself will be used if it implements the {@link RestResourceResolver} interface and not
	 * 		explicitly overridden via this annotation.
	 * 	<li>
	 * 		The {@link RestServlet} and {@link BasicRest} classes implement the {@link RestResourceResolver} interface with the same
	 * 		functionality as {@link BasicRestResourceResolver} that gets used if not overridden by this annotation.
	 * 		<br>Subclasses can also alter the behavior by overriding these methods.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestResourceResolvers}
	 * 	<li class='link'>{@doc RestInjection}
	 * </ul>
	 */
	public static final String REST_resourceResolver = PREFIX + ".resourceResolver.o";

	/**
	 * Configuration property:  Response handlers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_responseHandlers REST_responseHandlers}
	 * 	<li><b>Name:</b>  <js>"RestContext.responseHandlers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.ResponseHandler}|Class&lt;{@link org.apache.juneau.rest.ResponseHandler}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#responseHandlers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#responseHandlers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * By default, the following response handlers are provided out-of-the-box:
	 * <ul>
	 * 	<li class='jc'>{@link ReaderHandler} - {@link Reader} objects.
	 * 	<li class='jc'>{@link InputStreamHandler} - {@link InputStream} objects.
	 * 	<li class='jc'>{@link DefaultHandler} - All other POJOs.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our custom response handler for MySpecialObject objects. </jc>
	 * 	<jk>public class</jk> MyResponseHandler <jk>implements</jk> ResponseHandler {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public boolean</jk> handle(RestRequest req, RestResponse res, Object output) <jk>throws</jk> IOException, RestException {
	 * 			<jk>if</jk> (output <jk>instanceof</jk> MySpecialObject) {
	 * 				<jk>try</jk> (Writer w = res.getNegotiatedWriter()) {
	 * 					<jc>//Pipe it to the writer ourselves.</jc>
	 * 				}
	 * 				<jk>return true</jk>;  <jc>// We handled it.</jc>
	 * 			}
	 * 			<jk>return false</jk>; <jc>// We didn't handle it.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(responseHandlers=MyResponseHandler.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.responseHandlers(MyResponseHandler.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_responseHandlers</jsf>, MyResponseHandler.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.responseHandlers(MyResponseHandler.<jk>class</jk>);
	 * 		}
	 *
	 * 		<ja>@RestMethod</ja>(...)
	 * 		<jk>public</jk> Object myMethod() {
	 * 			<jc>// Return a special object for our handler.</jc>
	 * 			<jk>return new</jk> MySpecialObject();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Response handlers resolvers are always inherited from ascendant resources.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>(RestContext)</code>
	 * 			<li><code><jk>public static</jk> T <jsm>create</jsm>()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_responseHandlers = PREFIX + ".responseHandlers.lo";

	/**
	 * Configuration property:  Declared roles.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_rolesDeclared REST_rolesDeclared}
	 * 	<li><b>Name:</b>  <js>"RestContext.rolesDeclared.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.rolesDeclared</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_ROLESDECLARED</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#rolesDeclared()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#rolesDeclared(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * A comma-delimited list of all possible user roles.
	 *
	 * <p>
	 * Used in conjunction with {@link RestContextBuilder#roleGuard(String)} is used with patterns.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_rolesDeclared}
	 * </ul>
	 */
	public static final String REST_rolesDeclared = PREFIX + ".rolesDeclared.ss";

	/**
	 * Configuration property:  Role guard.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_roleGuard REST_roleGuard}
	 * 	<li><b>Name:</b>  <js>"RestContext.roleGuard.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.roleGuard</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_ROLEGUARD</c>
	 * 	<li><b>Default:</b>  empty set
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#roleGuard()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#roleGuard()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#roleGuard(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE &amp;&amp; ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* &amp; *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* &amp;&amp; *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link Rest#rolesDeclared()} or {@link RestContext#REST_rolesDeclared}.
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Role guards defined at both the class and method level must both pass.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_roleGuard}
	 * </ul>
	 */
	public static final String REST_roleGuard = PREFIX + ".roleGuard.ss";

	/**
	 * Configuration property:  Serializers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_serializers REST_serializers}
	 * 	<li><b>Name:</b>  <js>"RestContext.serializers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.serializer.Serializer}|Class&lt;{@link org.apache.juneau.serializer.Serializer}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#serializers()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#serializers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializers(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializers(Class...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializersReplace(Object...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#serializersReplace(Class...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level serializers to this resource.
	 *
	 * <p>
	 * Serializer are used to convert POJOs to HTTP response bodies.
	 * <br>Any of the Juneau framework serializers can be used in this setting.
	 * <br>The serializer selected is based on the request <c>Accept</c> header matched against the values returned by the following method
	 * using a best-match algorithm:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(serializers={JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but use pre-instantiated parsers.</jc>
	 * 			builder.serializers(JsonSerializer.<jsf>DEFAULT</jsf>, XmlSerializer.<jsf>DEFAULT</jsf>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_serializers</jsf>, JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.serializers(JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(serializers={HtmlSerializer.<jk>class</jk>})
	 * 		<jk>public</jk> MyPojo myMethod() {
	 * 			<jc>// Return a POJO to be serialized.</jc>
	 * 			<jk>return new</jk> MyPojo();
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>
	 * 		When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * 	<li>
	 * 		Typically, you'll want your resource to extend directly from {@link BasicRestServlet} which comes
	 * 		preconfigured with the following serializers:
	 * 		<ul>
	 * 			<li class='jc'>{@link HtmlDocSerializer}
	 * 			<li class='jc'>{@link HtmlStrippedDocSerializer}
	 * 			<li class='jc'>{@link HtmlSchemaDocSerializer}
	 * 			<li class='jc'>{@link JsonSerializer}
	 * 			<li class='jc'>{@link SimpleJsonSerializer}
	 * 			<li class='jc'>{@link JsonSchemaSerializer}
	 * 			<li class='jc'>{@link XmlDocSerializer}
	 * 			<li class='jc'>{@link UonSerializer}
	 * 			<li class='jc'>{@link UrlEncodingSerializer}
	 * 			<li class='jc'>{@link MsgPackSerializer}
	 * 			<li class='jc'>{@link SoapXmlSerializer}
	 * 			<li class='jc'>{@link PlainTextSerializer}
	 * 		</ul>
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='link'>{@doc RestSerializers}
	 * </ul>
	 * <p>
	 */
	public static final String REST_serializers = PREFIX + ".serializers.lo";

	/**
	 * Configuration property:  Static file finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_staticFiles REST_staticFiles}
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFiles.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.rest.StaticFiles}
	 * 	<li><b>Default:</b>  {@link #REST_staticFilesDefault}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#staticFiles()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFiles(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFiles(StaticFiles)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContext#createStaticFiles()}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.BasicRest#createStaticFiles()}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.BasicRestServlet#createStaticFiles()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files to be served up as static files through the REST API via the following
	 * predefined methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link BasicRest#getHtdoc(String, Locale)}.
	 * 	<li class='jm'>{@link BasicRestServlet#getHtdoc(String, Locale)}.
	 * </ul>
	 *
	 * <p>
	 * The static file finder can be accessed through the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getStaticFiles()}
	 * 	<li class='jm'>{@link RestRequest#getStaticFiles()}
	 * </ul>
	 *
	 * <p>
	 * The static file finder is instantiated via the {@link RestContext#createStaticFiles()} method which in turn instantiates
	 * based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link StaticFiles}.
	 * 	<li>Looks in {@link #REST_staticFiles} setting.
	 * 	<li>Looks for a public <c>createStaticFiles()</> method on the resource class with an optional {@link RestContext} argument.
	 * 		<br>Note that the {@link BasicRest#createStaticFiles()} and {@link BasicRestServlet#createStaticFiles()} methhods are implemented
	 * 		to automatically look for injected beans of type {@link StaticFiles} allowing preconfigured static file finders to be
	 * 		defined in a Spring configuration class.
	 * 	<li>Instantiates a {@link BasicStaticFiles} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory..
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Create a static file finder that looks for files in the /files working subdirectory, but overrides the find()
	 * 	// and resolve methods for special handling of special cases and adds a Foo header to all requests.</jc>
	 * 	<jk>public class</jk> MyStaticFiles <jk>extends</jk> StaticFiles {
	 *
	 * 		<jk>public</jk> MyStaticFiles() {
	 * 			<jk>super</jk>(
	 * 				<jk>new</jk> StaticFilesBuilder()
	 * 					.dir(<js>"/files"</js>)
	 * 					.headers(BasicStringHeader.<jsm>of</jsm>(<js>"Foo"</js>, <js>"bar"</js>))
	 * 			);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// FileFinder</jc>
	 * 		<jk>protected</jk> Optional&lt;InputStream&gt; find(String <jv>name</jv>, Locale <jv>locale</jv>) <jk>throws</jk> IOException {
	 * 			<jc>// Do special handling or just call super.find().</jc>
	 * 			<jk>return super</jk>.find(<jv>name</jv>, <jv>locale</jv>);
	 * 		}
	 *
	 *		<ja>@Override</ja> <jc>// staticFiles</jc>
	 * 		<jk>public</jk> Optional&lt;BasicHttpResource&gt; resolve(String <jv>path</jv>, Locale <jv>locale</jv>) {
	 * 			<jc>// Do special handling or just call super.resolve().</jc>
	 * 			<jk>return super</jk>.resolve(<jv>path</jv>, <jv>locale</jv>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@Rest</ja>(staticFiles=MyStaticFiles.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Created via createStaticFiles() method.</jc>
	 * 		<jk>public</jk> StaticFiles createStaticFiles(RestContext <jv>context</jv>) <jk>throws</jk> Exception {
	 * 			<jk>return new</jk> MyStaticFiles();
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			<jv>builder</jv>.set(<jsf>REST_staticFiles</jsf>, MyStaticFiles.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			<jv>builder</jv>.staticFiles(<jk>new</jk> MyStaticFiles());
	 * 		}
	 *
	 * 		<jc>// Option #4 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.staticFiles(MyStaticFiles.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Create a REST method that uses the static files finder.</jc>
	 * 		<ja>@RestMethod<ja>(
	 * 			method=<jsf>GET</jsf>,
	 * 			path=<js>"/htdocs/*"</js>
	 * 		)
	 * 		<jk>public</jk> HttpResource getHtdoc(RestRequest <jv>req</jv>, <ja>@Path</ja>("/*") String <jv>path</jv>, Locale <jv>locale</jv>) <jk>throws</jk> NotFound {
	 * 			<jk>return</jk> <jv>req</jv>.getStaticFiles().resolve(<jv>path</jv>, <jv>locale</jv>).orElseThrow(NotFound::<jk>new</jk>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_staticFiles = PREFIX + ".staticFiles.o";

	/**
	 * Configuration property:  Static file finder default.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_staticFilesDefault REST_staticFilesDefault}
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFilesDefault.o"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.rest.StaticFiles}
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.BasicStaticFiles}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFilesDefault(Class)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#staticFilesDefault(StaticFiles)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default static file finder.
	 * <p>
	 * This setting is inherited from the parent context.
	 */
	public static final String REST_staticFilesDefault = PREFIX + ".staticFilesDefault.o";

	/**
	 * Configuration property:  Supported accept media types.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_produces REST_produces}
	 * 	<li><b>Name:</b>  <js>"RestContext.produces.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.produces</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_PRODUCES</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#produces()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#produces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#produces(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#produces(MediaType...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#producesReplace(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#producesReplace(MediaType...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * <br>An example where this might be useful if you have serializers registered that handle media types that you
	 * don't want exposed in the Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(produces={<js>"$C{REST/supportedProduces,application/json}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.produces(<jk>false</jk>, <js>"application/json"</js>)
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_produces</jsf>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.produces(<jk>false</jk>, <js>"application/json"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * This affects the returned values from the following:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getProduces() RestContext.getProduces()}
	 * 	<li class='jm'>{@link RestRequest#getProduces()}
	 * 	<li class='jm'>{@link RestInfoProvider#getSwagger(RestRequest)} - Affects produces field.
	 * </ul>
	 */
	public static final String REST_produces = PREFIX + ".produces.ls";

	/**
	 * Configuration property:  Supported content media types.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_consumes REST_consumes}
	 * 	<li><b>Name:</b>  <js>"RestContext.consumes.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestContext.consumes</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_CONSUMES</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#consumes()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#consumes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#consumes(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#consumes(MediaType...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#consumesReplace(String...)}
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#consumesReplace(MediaType...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 * <br>An example where this might be useful if you have parsers registered that handle media types that you
	 * don't want exposed in the Swagger documentation.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(consumes={<js>"$C{REST/supportedConsumes,application/json}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.consumes(<jk>false</jk>, <js>"application/json"</js>)
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_consumes</jsf>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.consumes(<jk>false</jk>, <js>"application/json"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <p>
	 * This affects the returned values from the following:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestContext#getConsumes() RestContext.getConsumes()}
	 * 	<li class='jm'>{@link RestRequest#getConsumes()}
	 * 	<li class='jm'>{@link RestInfoProvider#getSwagger(RestRequest)} - Affects consumes field.
	 * </ul>
	 */
	public static final String REST_consumes = PREFIX + ".consumes.ls";

	/**
	 * Configuration property:  REST context class.
	 *
	 * <review>NEEDS REVIEW</review>
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_context REST_context}
	 * 	<li><b>Name:</b>  <js>"RestContext.context.c"</js>
	 * 	<li><b>Data type:</b>  <c>Class&lt;? extends {@link org.apache.juneau.rest.RestContext}&gt;</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.rest.RestContext}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#context()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#context(Class)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Allows you to extend the {@link RestContext} class to modify how any of the methods are implemented.
	 *
	 * <p>
	 * The subclass must provide the following:
	 * <ul>
	 * 	<li>A public constructor that takes in one parameter that should be passed to the super constructor:  {@link RestContextBuilder}.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our extended context class</jc>
	 * 	<jk>public</jk> MyRestContext <jk>extends</jk> RestContext {
	 * 		<jk>public</jk> MyRestContext(RestContextBuilder <jv>builder</jv>) {
	 * 			<jk>super</jk>(<jv>builder</jv>);
	 * 		}
	 *
	 * 		<jc>// Override any methods.</jc>
	 * 	}
	 * </p>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@Rest</ja>(context=MyRestContext.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 * 		...
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder <jv>builder</jv>) <jk>throws</jk> Exception {
	 * 			<jv>builder</jv>.context(MyRestContext.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_context = PREFIX + ".context.c";

	/**
	 * Configuration property:  Resource URI authority path.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_uriAuthority REST_uriAuthority}
	 * 	<li><b>Name:</b>  <js>"RestContext.uriAuthority.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.uriAuthority</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_URIAUTHORITY</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#uriAuthority()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#uriAuthority(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the authority path value for this resource and any child resources.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getAuthorityPath()}
	 * </ul>
	 *
	 * <p>
	 * If you do not specify the authority, it is automatically calculated via the following:
	 *
	 * <p class='bcode w800'>
	 * 	String scheme = request.getScheme();
	 * 	<jk>int</jk> port = request.getServerPort();
	 * 	StringBuilder sb = <jk>new</jk> StringBuilder(request.getScheme()).append(<js>"://"</js>).append(request.getServerName());
	 * 	<jk>if</jk> (! (port == 80 &amp;&amp; <js>"http"</js>.equals(scheme) || port == 443 &amp;&amp; <js>"https"</js>.equals(scheme)))
	 * 		sb.append(<js>':'</js>).append(port);
	 * 	authorityPath = sb.toString();
	 * </p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/servlet"</js>,
	 * 		uriAuthority=<js>"$C{REST/authorityPathOverride,http://localhost:10000}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.uriAuthority(<js>"http://localhost:10000"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_uriAuthority</jsf>, <js>"http://localhost:10000"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.uriAuthority(<js>"http://localhost:10000"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_uriAuthority = PREFIX + ".uriAuthority.s";

	/**
	 * Configuration property:  Resource URI context path.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_uriContext REST_uriContext}
	 * 	<li><b>Name:</b>  <js>"RestContext.uriContext.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.uriContext</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_URICONTEXT</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#uriContext()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#uriContext(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <p>
	 * This setting is useful if you want to use <js>"context:/child/path"</js> URLs in child resource POJOs but
	 * the context path is not actually specified on the servlet container.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getContextPath()} - Returns the overridden context path for the resource.
	 * 	<li class='jm'>{@link RestRequest#getServletPath()} - Includes the overridden context path for the resource.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/servlet"</js>,
	 * 		uriContext=<js>"$C{REST/contextPathOverride,/foo}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.uriContext(<js>"/foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_uriContext</jsf>, <js>"/foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.uriContext(<js>"/foo"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_uriContext = PREFIX + ".uriContext.s";

	/**
	 * Configuration property:  URI resolution relativity.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_uriRelativity REST_uriRelativity}
	 * 	<li><b>Name:</b>  <js>"RestContext.uriRelativity.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.uriRelativity</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_URIRELATIVITY</c>
	 * 	<li><b>Default:</b>  <js>"RESOURCE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#uriRelativity()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#uriRelativity(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/servlet"</js>,
	 * 		uriRelativity=<js>"$C{REST/uriRelativity,PATH_INFO}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.uriRelativity(<js>"PATH_INFO"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_uriRelativity</jsf>, <js>"PATH_INFO"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.uriRelativity(<js>"PATH_INFO"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_uriRelativity = PREFIX + ".uriRelativity.s";

	/**
	 * Configuration property:  URI resolution.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestContext#REST_uriResolution REST_uriResolution}
	 * 	<li><b>Name:</b>  <js>"RestContext.uriResolution.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestContext.uriResolution</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTCONTEXT_URIRESOLUTION</c>
	 * 	<li><b>Default:</b>  <js>"ROOT_RELATIVE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.Rest#uriResolution()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#uriResolution(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies how relative URIs should be interpreted by serializers.
	 *
	 * <p>
	 * See {@link UriResolution} for possible values.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@Rest</ja>(
	 * 		path=<js>"/servlet"</js>,
	 * 		uriResolution=<js>"$C{REST/uriResolution,ABSOLUTE}"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.uriResolution(<js>"ABSOLUTE"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_uriResolution</jsf>, <js>"ABSOLUTE"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.uriResolution(<js>"ABSOLUTE"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_uriResolution = PREFIX + ".uriResolution.s";

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	private static final Map<Class<?>, RestContext> REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Returns a registry of all created {@link RestContext} objects.
	 *
	 * @return An unmodifiable map of resource classes to {@link RestContext} objects.
	 */
	public static final Map<Class<?>, RestContext> getGlobalRegistry() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Object resource;
	final RestContextBuilder builder;
	private final boolean
		allowBodyParam,
		renderResponseStackTraces;
	private final Enablement debug;
	private final String
		clientVersionHeader,
		uriAuthority,
		uriContext;
	final String fullPath;
	final UrlPathMatcher pathMatcher;

	private final Set<String> allowedMethodParams, allowedHeaderParams, allowedMethodHeaders;

	private final Map<Class<?>,RestMethodParam> paramResolvers;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final JsonSchemaGenerator jsonSchemaGenerator;
	private final List<MediaType>
		consumes,
		produces;
	private final Map<String,Object>
		reqHeaders,
		resHeaders;
	private final OMap reqAttrs;
	private final ResponseHandler[] responseHandlers;
	private final Messages msgs;
	private final Config config;
	private final VarResolver varResolver;
	private final Map<String,List<RestMethodContext>> methodMap;
	private final List<RestMethodContext> methods;
	private final Map<String,RestContext> childResources;
	private final StackTraceStore stackTraceDatabase;
	private final Logger logger;
	private final RestInfoProvider infoProvider;
	private final HttpException initException;
	private final RestContext parentContext;
	private final RestResourceResolver resourceResolver;
	private final UriResolution uriResolution;
	private final UriRelativity uriRelativity;
	private final ConcurrentHashMap<String,MethodExecStats> methodExecStats = new ConcurrentHashMap<>();
	private final Instant startTime;
	private final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();

	// Lifecycle methods
	private final MethodInvoker[]
		postInitMethods,
		postInitChildFirstMethods,
		preCallMethods,
		postCallMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;
	private final RestMethodParam[][]
		preCallMethodParams,
		postCallMethodParams;
	private final Class<?>[][]
		postInitMethodParams,
		postInitChildFirstMethodParams,
		startCallMethodParams,
		endCallMethodParams,
		destroyMethodParams;

	private final FileFinder fileFinder;
	private final StaticFiles staticFiles;
	private final RestLogger callLogger;

	private final ThreadLocal<RestCall> call = new ThreadLocal<>();

	private final ReflectionMap<Enablement> debugEnablement;

	/**
	 * Constructor.
	 *
	 * @param resource The resource annotated with <ja>@Rest</ja>.
	 * @return A new builder object.
	 * @throws ServletException Something bad happened.
	 */
	public static RestContextBuilder create(Object resource) throws ServletException {
		return new RestContextBuilder(null, resource.getClass(), null).init(resource);
	}

	/**
	 * Constructor.
	 *
	 * @param servletConfig The servlet config passed into the servlet by the servlet container.
	 * @param resourceClass The class annotated with <ja>@Rest</ja>.
	 * @param parentContext The parent context, or <jk>null</jk> if there is no parent context.
	 * @return A new builder object.
	 * @throws ServletException Something bad happened.
	 */
	static RestContextBuilder create(ServletConfig servletConfig, Class<?> resourceClass, RestContext parentContext) throws ServletException {
		return new RestContextBuilder(servletConfig, resourceClass, parentContext);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The servlet configuration object.
	 * @throws Exception If any initialization problems were encountered.
	 */
	public RestContext(RestContextBuilder builder) throws Exception {
		super(builder.getPropertyStore());

		startTime = Instant.now();

		REGISTRY.put(builder.resourceClass, this);

		HttpException _initException = null;

		try {
			ServletContext servletContext = builder.servletContext;

			this.resource = builder.resource;
			this.builder = builder;
			this.parentContext = builder.parentContext;
			this.logger = createLogger();
			this.stackTraceDatabase = createStackTraceDatabase();

			Object defaultResourceResolver = parentContext == null ? (resource instanceof RestResourceResolver ? resource : BasicRestResourceResolver.class) : parentContext.resourceResolver;
			resourceResolver = getInstanceProperty(REST_resourceResolver, resource, RestResourceResolver.class, defaultResourceResolver, ResourceResolver.FUZZY, this);

			varResolver = builder.varResolverBuilder
				.vars(
					FileVar.class,
					LocalizationVar.class,
					RequestAttributeVar.class,
					RequestFormDataVar.class,
					RequestHeaderVar.class,
					RequestPathVar.class,
					RequestQueryVar.class,
					RequestVar.class,
					RestInfoVar.class,
					SerializedRequestAttrVar.class,
					ServletInitParamVar.class,
					SwaggerVar.class,
					UrlVar.class,
					UrlEncodeVar.class,
					HtmlWidgetVar.class
				)
				.build()
			;

			VarResolverSession vrs = this.varResolver.createSession();
			config = builder.config.resolving(vrs);

			ClassInfo rci = ClassInfo.of(resource).resolved();

			PropertyStore ps = getPropertyStore();

			uriContext = nullIfEmpty(getStringProperty(REST_uriContext));
			uriAuthority = nullIfEmpty(getStringProperty(REST_uriAuthority));
			uriResolution = getProperty(REST_uriResolution, UriResolution.class, UriResolution.ROOT_RELATIVE);
			uriRelativity = getProperty(REST_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);

			allowBodyParam = ! getBooleanProperty(REST_disableAllowBodyParam);
			allowedHeaderParams = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedHeaderParams, "Accept,Content-Type"));
			allowedMethodParams = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedMethodParams, "HEAD,OPTIONS"));
			allowedMethodHeaders = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedMethodHeaders, ""));
			renderResponseStackTraces = getBooleanProperty(REST_renderResponseStackTraces);
			clientVersionHeader = getStringProperty(REST_clientVersionHeader, "X-Client-Version");

			ReflectionMap.Builder<Enablement> deb = ReflectionMap.create(Enablement.class);
			for (String s : split(getStringProperty(REST_debugOn, ""))) {
				s = s.trim();
				if (! s.isEmpty()) {
					int i = s.indexOf('=');
					if (i == -1)
						deb.append(s.trim(), Enablement.ALWAYS);
					else
						deb.append(s.substring(0, i).trim(), Enablement.fromString(s.substring(i+1).trim()));
				}
			}

			Enablement defaultDebug = getInstanceProperty(REST_debugDefault, Enablement.class, null);
			if (defaultDebug == null)
				defaultDebug = isDebug() ? Enablement.ALWAYS : Enablement.NEVER;

			Enablement de = getInstanceProperty(REST_debug, Enablement.class, defaultDebug);
			if (de != null)
				deb.append(rci.getFullName(), de);
			for (MethodInfo mi : rci.getPublicMethods())
				for (RestMethod a : mi.getAnnotations(RestMethod.class))
					if (a != null && ! a.debug().isEmpty())
						deb.append(mi.getFullName(), Enablement.fromString(a.debug()));

			this.debugEnablement = deb.build();

			this.debug = debugEnablement.find(rci.inner(), Enablement.class).orElse(Enablement.NEVER);

			responseHandlers = getInstanceArrayProperty(REST_responseHandlers, resource, ResponseHandler.class, new ResponseHandler[0], resourceResolver, this);

			AMap<Class<?>,RestMethodParam> _paramResolvers = AMap.of();
			for (RestMethodParam rp : getInstanceArrayProperty(REST_paramResolvers, RestMethodParam.class, new RestMethodParam[0], resourceResolver, this))
				_paramResolvers.put(rp.forClass(), rp);
			paramResolvers = _paramResolvers.unmodifiable();

			Map<String,Object> _reqHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			_reqHeaders.putAll(getMapProperty(REST_reqHeaders, String.class));
			reqHeaders = AMap.unmodifiable(_reqHeaders);

			reqAttrs = new OMap(getMapProperty(REST_reqAttrs, Object.class)).unmodifiable();
			resHeaders = getMapProperty(REST_resHeaders, Object.class);

			callLogger = createCallLogger();

			serializers =
				SerializerGroup
				.create()
				.append(getInstanceArrayProperty(REST_serializers, Serializer.class, new Serializer[0], resourceResolver, resource, ps))
				.build();
			parsers =
				ParserGroup
				.create()
				.append(getInstanceArrayProperty(REST_parsers, Parser.class, new Parser[0], resourceResolver, resource, ps))
				.build();
			partSerializer = getInstanceProperty(REST_partSerializer, HttpPartSerializer.class, OpenApiSerializer.class, resourceResolver, resource, ps);
			partParser = getInstanceProperty(REST_partParser, HttpPartParser.class, OpenApiParser.class, resourceResolver, resource, ps);
			jsonSchemaGenerator =
				JsonSchemaGenerator
				.create()
				.apply(ps)
				.build();

			this.fileFinder = createFileFinder();
			this.staticFiles = createStaticFiles();

			consumes = getListProperty(REST_consumes, MediaType.class, parsers.getSupportedMediaTypes());
			produces = getListProperty(REST_produces, MediaType.class, serializers.getSupportedMediaTypes());

			Tuple2<Class<?>,String>[] mbl = getInstanceArrayProperty(REST_messages, Tuple2.class);
			Messages msgs = null;
			for (int i = mbl.length-1; i >= 0; i--)
				msgs = Messages.create(firstNonNull(mbl[i].getA(), rci.inner())).name(mbl[i].getB()).parent(msgs).build();
			this.msgs = msgs;

			this.fullPath = (builder.parentContext == null ? "" : (builder.parentContext.fullPath + '/')) + builder.getPath();

			String p = builder.getPath();
			if (! p.endsWith("/*"))
				p += "/*";
			this.pathMatcher = UrlPathMatcher.of(p);

			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<>();   // Temporary to help debug transient duplicate method issue.
			MethodMapBuilder methodMapBuilder = new MethodMapBuilder();
			AMap<String,Method>
				_startCallMethods = AMap.of(),
				_preCallMethods = AMap.of(),
				_postCallMethods = AMap.of(),
				_endCallMethods = AMap.of(),
				_postInitMethods = AMap.of(),
				_postInitChildFirstMethods = AMap.of(),
				_destroyMethods = AMap.of();
			AList<RestMethodParam[]>
				_preCallMethodParams = AList.of(),
				_postCallMethodParams = AList.of();
			AList<Class<?>[]>
				_startCallMethodParams = AList.of(),
				_endCallMethodParams = AList.of(),
				_postInitMethodParams = AList.of(),
				_postInitChildFirstMethodParams = AList.of(),
				_destroyMethodParams = AList.of();

			for (MethodInfo mi : rci.getPublicMethods()) {
				RestMethod a = mi.getLastAnnotation(RestMethod.class);

				// Also include methods on @Rest-annotated interfaces.
				if (a == null) {
					for (Method mi2 : mi.getMatching()) {
						Class<?> ci2 = mi2.getDeclaringClass();
						if (ci2.isInterface() && ci2.getAnnotation(Rest.class) != null) {
							a = RestMethodAnnotation.DEFAULT;
						}
					}
				}
				if (a != null) {
					methodsFound.add(mi.getSimpleName() + "," + emptyIfNull(a.method()) + "," + fixMethodPath(a.path()));
					try {
						if (mi.isNotPublic())
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", rci.inner().getName(), mi.getSimpleName());

						RestMethodContextBuilder rmcb = new RestMethodContextBuilder(resource, mi.inner(), this);
						RestMethodContext sm = new RestMethodContext(rmcb);
						String httpMethod = sm.getHttpMethod();

						// RRPC is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("RRPC".equals(httpMethod)) {

							final ClassMeta<?> interfaceClass = getClassMeta(mi.inner().getGenericReturnType());
							final RrpcInterfaceMeta rim = new RrpcInterfaceMeta(interfaceClass.getInnerClass(), null);
							if (rim.getMethodsByPath().isEmpty())
								throw new InternalServerError("Method {0} returns an interface {1} that doesn't define any remote methods.", mi.getSignature(), interfaceClass.getFullName());

							RestMethodContextBuilder smb = new RestMethodContextBuilder(resource, mi.inner(), this);
							smb.dotAll();
							sm = new RestMethodContext(smb) {

								@Override
								void invoke(RestCall call) throws Throwable {

									super.invoke(call);

									final Object o = call.getOutput();

									if ("GET".equals(call.getMethod())) {
										call.output(rim.getMethodsByPath().keySet());
										return;

									} else if ("POST".equals(call.getMethod())) {
										String pip = call.getUrlPath().getPath();
										if (pip.indexOf('/') != -1)
											pip = pip.substring(pip.lastIndexOf('/')+1);
										pip = urlDecode(pip);
										RrpcInterfaceMethodMeta rmm = rim.getMethodMetaByPath(pip);
										if (rmm != null) {
											Method m = rmm.getJavaMethod();
											try {
												RestRequest req = call.getRestRequest();
												// Parse the args and invoke the method.
												Parser p = req.getBody().getParser();
												Object[] args = null;
												if (m.getGenericParameterTypes().length == 0)
													args = new Object[0];
												else {
													try (Closeable in = p.isReaderParser() ? req.getReader() : req.getInputStream()) {
														args = p.parseArgs(in, m.getGenericParameterTypes());
													}
												}
												Object output = m.invoke(o, args);
												call.output(output);
												return;
											} catch (Exception e) {
												throw toHttpException(e, InternalServerError.class);
											}
										}
									}
									throw new NotFound();
								}
							};

							methodMapBuilder.add("GET", sm).add("POST", sm);

						} else {
							methodMapBuilder.add(httpMethod, sm);
						}
					} catch (Throwable e) {
						throw new RestServletException(e, "Problem occurred trying to initialize methods on class {0}, methods={1}", rci.inner().getName(), SimpleJsonSerializer.DEFAULT.serialize(methodsFound));
					}
				}
			}

			for (MethodInfo m : rci.getAllMethodsParentFirst()) {
				if (m.isPublic() && m.hasAnnotation(RestHook.class)) {
					HookEvent he = m.getLastAnnotation(RestHook.class).value();
					String sig = m.getSignature();
					switch(he) {
						case PRE_CALL: {
							if (! _preCallMethods.containsKey(sig)) {
								m.setAccessible();
								_preCallMethods.put(sig, m.inner());
								_preCallMethodParams.add(findParams(m, true, null));
							}
							break;
						}
						case POST_CALL: {
							if (! _postCallMethods.containsKey(sig)) {
								m.setAccessible();
								_postCallMethods.put(sig, m.inner());
								_postCallMethodParams.add(findParams(m, true, null));
							}
							break;
						}
						case START_CALL: {
							if (! _startCallMethods.containsKey(sig)) {
								m.setAccessible();
								_startCallMethods.put(sig, m.inner());
								_startCallMethodParams.add((Class<?>[])m.getRawParamTypes().toArray());
								assertArgsOnlyOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case END_CALL: {
							if (! _endCallMethods.containsKey(sig)) {
								m.setAccessible();
								_endCallMethods.put(sig, m.inner());
								_endCallMethodParams.add((Class<?>[])m.getRawParamTypes().toArray());
								assertArgsOnlyOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case POST_INIT: {
							if (! _postInitMethods.containsKey(sig)) {
								m.setAccessible();
								_postInitMethods.put(sig, m.inner());
								_postInitMethodParams.add((Class<?>[])m.getRawParamTypes().toArray());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						case POST_INIT_CHILD_FIRST: {
							if (! _postInitChildFirstMethods.containsKey(sig)) {
								m.setAccessible();
								_postInitChildFirstMethods.put(sig, m.inner());
								_postInitChildFirstMethodParams.add((Class<?>[])m.getRawParamTypes().toArray());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						case DESTROY: {
							if (! _destroyMethods.containsKey(sig)) {
								m.setAccessible();
								_destroyMethods.put(sig, m.inner());
								_destroyMethodParams.add((Class<?>[])m.getRawParamTypes().toArray());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						default: // Ignore INIT
					}
				}
			}

			this.preCallMethods = _preCallMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_preCallMethods.size()]);
			this.postCallMethods = _postCallMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_postCallMethods.size()]);
			this.startCallMethods = _startCallMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_startCallMethods.size()]);
			this.endCallMethods = _endCallMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_endCallMethods.size()]);
			this.postInitMethods = _postInitMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_postInitMethods.size()]);
			this.postInitChildFirstMethods = _postInitChildFirstMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_postInitChildFirstMethods.size()]);
			this.destroyMethods = _destroyMethods.values().stream().map(x->new MethodInvoker(x, getMethodExecStats(x))).collect(Collectors.toList()).toArray(new MethodInvoker[_destroyMethods.size()]);
			this.preCallMethodParams = _preCallMethodParams.toArray(new RestMethodParam[_preCallMethodParams.size()][]);
			this.postCallMethodParams = _postCallMethodParams.toArray(new RestMethodParam[_postCallMethodParams.size()][]);
			this.startCallMethodParams = _startCallMethodParams.toArray(new Class[_startCallMethodParams.size()][]);
			this.endCallMethodParams = _endCallMethodParams.toArray(new Class[_endCallMethodParams.size()][]);
			this.postInitMethodParams = _postInitMethodParams.toArray(new Class[_postInitMethodParams.size()][]);
			this.postInitChildFirstMethodParams = _postInitChildFirstMethodParams.toArray(new Class[_postInitChildFirstMethodParams.size()][]);
			this.destroyMethodParams = _destroyMethodParams.toArray(new Class[_destroyMethodParams.size()][]);

			this.methodMap = methodMapBuilder.getMap();
			this.methods = methodMapBuilder.getList();

			// Initialize our child resources.
			for (Object o : getArrayProperty(REST_children, Object.class)) {
				String path = null;
				Object r = null;
				if (o instanceof RestChild) {
					RestChild rc = (RestChild)o;
					path = rc.path;
					r = rc.resource;
				} else if (o instanceof Class<?>) {
					Class<?> c = (Class<?>)o;
					// Don't allow specifying yourself as a child.  Causes an infinite loop.
					if (c == builder.resourceClass)
						continue;
					r = c;
				} else {
					r = o;
				}

				RestContextBuilder childBuilder = null;

				if (o instanceof Class) {
					Class<?> oc = (Class<?>)o;
					childBuilder = RestContext.create(builder.inner, oc, this);
					r = resourceResolver.resolve(resource, oc, childBuilder);
				} else {
					r = o;
					childBuilder = RestContext.create(builder.inner, o.getClass(), this);
				}

				childBuilder.init(r);
				if (r instanceof RestServlet)
					((RestServlet)r).innerInit(childBuilder);
				childBuilder.servletContext(servletContext);
				RestContext rc2 = childBuilder.build();
				if (r instanceof RestServlet)
					((RestServlet)r).setContext(rc2);
				path = childBuilder.getPath();
				childResources.put(path, rc2);
			}

			Object defaultRestInfoProvider = resource instanceof RestInfoProvider ? resource : BasicRestInfoProvider.class;
			infoProvider = getInstanceProperty(REST_infoProvider, resource, RestInfoProvider.class, defaultRestInfoProvider, resourceResolver, this);

		} catch (HttpException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new InternalServerError(e);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	/**
	 * Instantiates the file finder for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link FileFinder}.
	 * 	<li>Looks for value in {@link #REST_fileFinder} setting.
	 * 	<li>Looks for a <c>createFileFinder()</> method on the resource class with an optional {@link RestContext} argument.
	 * 	<li>Looks for value in {@link #REST_fileFinderDefault} setting.
	 * 	<li>Instantiates a {@link BasicFileFinder}.
	 * </ul>
	 *
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 * @seealso #REST_fileFinder
	 */
	protected FileFinder createFileFinder() throws Exception {
		FileFinder x = null;
		if (resource instanceof FileFinder)
			x = (FileFinder)resource;
		if (x == null)
			x = getInstanceProperty(REST_fileFinder, FileFinder.class, null, resourceResolver, this);
		if (x == null) {
			MethodInfo mi = ClassInfo.of(resource).getPublicMethodFuzzy2("createFileFinder", FileFinder.class, this);
			if (mi != null)
				x = (FileFinder)mi.invokeFuzzy(resource, this, resourceResolver);
		}
		if (x == null)
			x = getInstanceProperty(REST_fileFinderDefault, FileFinder.class, null, resourceResolver, this);
		if (x == null)
			x = new BasicFileFinder(this);
		return x;
	}

	/**
	 * Instantiates the static files finder for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of FileFinder.
	 * 	<li>Looks for value in {@link #REST_staticFiles} setting.
	 * 	<li>Looks for a <c>createStaticFiles()</> method on the resource class with an optional {@link RestContext} argument.
	 * 	<li>Looks for value in {@link #REST_staticFilesDefault} setting.
	 * 	<li>Instantiates a {@link BasicStaticFiles}.
	 * </ul>
	 *
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 * @seealso #REST_staticFiles
	 */
	protected StaticFiles createStaticFiles() throws Exception {
		StaticFiles x = null;
		if (resource instanceof StaticFiles)
			x = (StaticFiles)resource;
		if (x == null)
			x = getInstanceProperty(REST_staticFiles, StaticFiles.class, null, resourceResolver, this);
		if (x == null) {
			MethodInfo mi = ClassInfo.of(resource).getPublicMethodFuzzy2("createStaticFiles", StaticFiles.class, this);
			if (mi != null)
				x = (StaticFiles)mi.invokeFuzzy(resource, this, resourceResolver);
		}
		if (x == null)
			x = getInstanceProperty(REST_staticFilesDefault, StaticFiles.class, null, resourceResolver, this);
		if (x == null)
			x = new BasicStaticFiles(this);
		return x;
	}

	/**
	 * Instantiates the call logger this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of RestLogger.
	 * 	<li>Looks for value in {@link #REST_callLogger} setting.
	 * 	<li>Looks for a <c>createCallLogger()</> method on the resource class with an optional {@link RestContext} argument.
	 * 	<li>Looks for value in {@link #REST_callLoggerDefault} setting.
	 * 	<li>Instantiates a {@link BasicFileFinder}.
	 * </ul>
	 *
	 * @return The file finder for this REST resource.
	 * @throws Exception If file finder could not be instantiated.
	 * @seealso #REST_callLogger
	 */
	protected RestLogger createCallLogger() throws Exception {
		RestLogger x = null;
		if (resource instanceof RestLogger)
			x = (RestLogger)resource;
		if (x == null)
			x = getInstanceProperty(REST_callLogger, RestLogger.class, null, resourceResolver, this);
		if (x == null) {
			MethodInfo mi = ClassInfo.of(resource).getPublicMethodFuzzy2("createCallLogger", RestLogger.class, this);
			if (mi != null)
				x = (RestLogger)mi.invokeFuzzy(resource, this, resourceResolver);
		}
		if (x == null)
			x = getInstanceProperty(REST_callLoggerDefault, RestLogger.class, null, resourceResolver, this);
		if (x == null)
			x = new BasicRestLogger(this);
		return x;
	}

	/**
	 * Instantiates the Java logger to use for this REST context.
	 *
	 * @return The Java logger to use for this REST context.
	 */
	protected Logger createLogger() {
		return Logger.getLogger(resource.getClass().getName());
	}

	/**
	 * Instantiates the stack trace database to use for this REST context.
	 *
	 * @return The stack trace database to use for this REST context.
	 */
	protected StackTraceStore createStackTraceDatabase() {
		return StackTraceStore.GLOBAL;
	}

	/**
	 * Returns the resource resolver associated with this context.
	 *
	 * <p>
	 * The resource resolver is used for instantiating child resource classes.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_resourceResolver}
	 * </ul>
	 *
	 * @return The resource resolver associated with this context.
	 */
	protected RestResourceResolver getResourceResolver() {
		return resourceResolver;
	}

	/**
	 * Returns the time statistics gatherer for the specified method.
	 *
	 * @param m The method to get statistics for.
	 * @return The cached time-stats object.
	 */
	protected MethodExecStats getMethodExecStats(Method m) {
		String n = MethodInfo.of(m).getSimpleName();
		MethodExecStats ts = methodExecStats.get(n);
		if (ts == null) {
			methodExecStats.putIfAbsent(n, new MethodExecStats(m));
			ts = methodExecStats.get(n);
		}
		return ts;
	}

	/**
	 * Returns the variable resolver for this servlet.
	 *
	 * <p>
	 * Variable resolvers are used to replace variables in property values.
	 * They can be nested arbitrarily deep.
	 * They can also return values that themselves contain other variables.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@Rest</ja>(
	 * 		messages=<js>"nls/Messages"</js>,
	 * 		properties={
	 * 			<ja>@Property</ja>(name=<js>"title"</js>,value=<js>"$L{title}"</js>),  <jc>// Localized variable in Messages.properties</jc>
	 * 			<ja>@Property</ja>(name=<js>"javaVendor"</js>,value=<js>"$S{java.vendor,Oracle}"</js>),  <jc>// System property with default value</jc>
	 * 			<ja>@Property</ja>(name=<js>"foo"</js>,value=<js>"bar"</js>),
	 * 			<ja>@Property</ja>(name=<js>"bar"</js>,value=<js>"baz"</js>),
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 			<ja>@Property</ja>(name=<js>"v1"</js>,value=<js>"$R{foo,bar}"</js>),  <jc>// Request variable.  value="bar"</jc>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> BasicRestServlet {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern involves using variables inside the {@link HtmlDocConfig @HtmlDocConfig} annotation:
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(
	 * 		method=<jsf>GET</jsf>, path=<js>"/{name}/*"</js>
	 * 	)
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"up: $R{requestParentURI}"</js>,
	 * 			<js>"options: servlet:/?method=OPTIONS"</js>,
	 * 			<js>"stats: servlet:/stats"</js>,
	 * 			<js>"editLevel: servlet:/editLevel?logger=$A{attribute.name, OFF}"</js>
	 * 		}
	 * 		header={
	 * 			<js>"&lt;h1&gt;$L{MyLocalizedPageTitle}&lt;/h1&gt;"</js>
	 * 		},
	 * 		aside={
	 * 			<js>"$F{resources/AsideText.html}"</js>
	 * 		}
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest req, <ja>@Path</ja> String name) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#vars(Class...)} - For adding custom vars.
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * 	<li class='link'>{@doc RestSvlVariables}
	 * </ul>
	 *
	 * @return The var resolver in use by this resource.
	 */
	public VarResolver getVarResolver() {
		return varResolver;
	}

	/**
	 * Returns the config file associated with this servlet.
	 *
	 * <p>
	 * The config file is identified via one of the following:
	 * <ul class='javatree'>
	 * 	<li class='ja'>{@link Rest#config()}
	 * 	<li class='jm'>{@link RestContextBuilder#config(Config)}
	 * </ul>
	 *
	 * @return
	 * 	The resolving config file associated with this servlet.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Config getConfig() {
		return config;
	}


	/**
	 * Returns the path for this resource as defined by the {@link Rest#path() @Rest(path)} annotation or
	 * {@link RestContextBuilder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_path}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return fullPath;
	}

	/**
	 * Returns the call logger to use for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_callLogger}
	 * </ul>
	 *
	 * @return
	 * 	The call logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestLogger getCallLogger() {
		return callLogger;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link #REST_messages}
	 * </ul>
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Messages getMessages() {
		return msgs;
	}

	/**
	 * Returns the REST information provider used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_infoProvider}
	 * </ul>
	 *
	 * @return
	 * 	The information provider for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestInfoProvider getInfoProvider() {
		return infoProvider;
	}

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link Rest @Rest} annotation, usually
	 * an instance of {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Object getResource() {
		return resource;
	}

	/**
	 * Returns the resource object as a {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object cast to {@link RestServlet}, or <jk>null</jk> if the resource doesn't subclass from
	 * 	{@link RestServlet}.
	 */
	public RestServlet getRestServlet() {
		return resource instanceof RestServlet ? (RestServlet)resource : null;
	}

	/**
	 * Throws a {@link HttpException} if an exception occurred in the constructor of this object.
	 *
	 * @throws HttpException The initialization exception wrapped in a {@link HttpException}.
	 */
	protected void checkForInitException() throws HttpException {
		if (initException != null)
			throw initException;
	}

	/**
	 * Returns the parent resource context (if this resource was initialized from a parent).
	 *
	 * <p>
	 * From this object, you can get access to the parent resource class itself using {@link #getResource()} or
	 * {@link #getRestServlet()}
	 *
	 * @return The parent resource context, or <jk>null</jk> if there is no parent context.
	 */
	public RestContext getParentContext() {
		return parentContext;
	}

	/**
	 * Returns the servlet init parameter returned by {@link ServletConfig#getInitParameter(String)}.
	 *
	 * @param name The init parameter name.
	 * @return The servlet init parameter, or <jk>null</jk> if not found.
	 */
	public String getServletInitParameter(String name) {
		return builder.getInitParameter(name);
	}

	/**
	 * Returns the child resources associated with this servlet.
	 *
	 * @return
	 * 	An unmodifiable map of child resources.
	 * 	Keys are the {@link Rest#path() @Rest(path)} annotation defined on the child resource.
	 */
	public Map<String,RestContext> getChildResources() {
		return Collections.unmodifiableMap(childResources);
	}

	/**
	 * Returns whether it's safe to render stack traces in HTTP responses.
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns whether it's safe to pass the HTTP body as a <js>"body"</js> GET parameter.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_disableAllowBodyParam}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Allowed header URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedHeaderParams}
	 * </ul>
	 *
	 * @return
	 * 	The header names allowed to be passed as URL parameters.
	 * 	<br>The set is case-insensitive ordered.
	 */
	public Set<String> getAllowedHeaderParams() {
		return allowedHeaderParams;
	}

	/**
	 * Allowed method headers.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedMethodHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>X-Method</c> headers.
	 * 	<br>The set is case-insensitive ordered.
	 */
	public Set<String> getAllowedMethodHeaders() {
		return allowedMethodHeaders;
	}

	/**
	 * Allowed method URL parameters.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_allowedMethodParams}
	 * </ul>
	 *
	 * @return
	 * 	The method names allowed to be passed as <c>method</c> URL parameters.
	 * 	<br>The set is case-insensitive ordered.
	 */
	public Set<String> getAllowedMethodParams() {
		return allowedMethodParams;
	}

	/**
	 * Returns the debug setting on this context for the specified method.
	 *
	 * @param method The java method.
	 * @return The debug setting on this context or the debug value of the servlet context if not specified for this method.
	 */
	public Enablement getDebug(Method method) {
		if (method == null)
			return null;
		return debugEnablement.find(method).orElse(debug);
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 *
	 * @return
	 * 	The name of the client version header used by this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Returns the file finder associated with this context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_fileFinder}
	 * </ul>
	 *
	 * @return
	 * 	The file finder for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public FileFinder getFileFinder() {
		return fileFinder;
	}

	/**
	 * Returns the static files associated with this context.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @return
	 * 	The static files for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public StaticFiles getStaticFiles() {
		return staticFiles;
	}

	/**
	 * Returns the logger associated with this context.
	 *
	 * @return
	 * 	The logger for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the stack trace database associated with this context.
	 *
	 * @return
	 * 	The stack trace database for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public StackTraceStore getStackTraceStore() {
		return stackTraceDatabase;
	}

	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partParser}
	 * </ul>
	 *
	 * @return
	 * 	The HTTP-part parser associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_partSerializer}
	 * </ul>
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the JSON-Schema generator associated with this resource.
	 *
	 * @return
	 * 	The HTTP-part serializer associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_serializers}
	 * 	<li class='jf'>{@link RestContext#REST_produces}
	 * </ul>
	 *
	 * @return
	 * 	The supported <c>Accept</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getProduces() {
		return produces;
	}

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_parsers}
	 * 	<li class='jf'>{@link RestContext#REST_consumes}
	 * </ul>
	 *
	 * @return
	 * 	The supported <c>Content-Type</c> header values for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public List<MediaType> getConsumes() {
		return consumes;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getReqHeaders() {
		return reqHeaders;
	}

	/**
	 * Returns the default request attributes for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqAttrs}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public OMap getReqAttrs() {
		return reqAttrs;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_resHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The default response headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getResHeaders() {
		return resHeaders;
	}

	/**
	 * Returns the response handlers associated with this resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_responseHandlers}
	 * </ul>
	 *
	 * @return
	 * 	The response handlers associated with this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	protected ResponseHandler[] getResponseHandlers() {
		return responseHandlers;
	}

	/**
	 * Returns the authority path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriAuthority}
	 * </ul>
	 *
	 * @return
	 * 	The authority path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriAuthority() {
		if (uriAuthority != null)
			return uriAuthority;
		if (parentContext != null)
			return parentContext.getUriAuthority();
		return null;
	}

	/**
	 * Returns the context path of the resource.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriContext}
	 * </ul>
	 *
	 * @return
	 * 	The context path of this resource.
	 * 	<br>If not specified, returns the context path of the ascendant resource.
	 */
	public String getUriContext() {
		if (uriContext != null)
			return uriContext;
		if (parentContext != null)
			return parentContext.getUriContext();
		return null;
	}

	/**
	 * Returns the setting on how relative URIs should be interpreted as relative to.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriRelativity}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution relativity setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * Returns the setting on how relative URIs should be resolved.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_uriResolution}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution setting value.
	 * 	<br>Never <jk>null</jk>.
	 */
	public UriResolution getUriResolution() {
		return uriResolution;
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestMethod @RestMethod} annotation.
	 *
	 * @return
	 * 	An unmodifiable map of Java method names to call method objects.
	 */
	public List<RestMethodContext> getMethodContexts() {
		return methods;
	}

	/**
	 * Returns timing information on all method executions on this class.
	 *
	 * <p>
	 * Timing information is maintained for any <ja>@RestResource</ja>-annotated and hook methods.
	 *
	 * @return A list of timing statistics ordered by average execution time descending.
	 */
	public List<MethodExecStats> getMethodExecStats() {
		return methodExecStats.values().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * Gives access to the internal stack trace database.
	 *
	 * @return The stack trace database.
	 */
	public RestContextStats getStats() {
		return new RestContextStats(startTime, getMethodExecStats());
	}

	/**
	 * Returns the timing information returned by {@link #getMethodExecStats()} in a readable format.
	 *
	 * @return A report of all method execution times ordered by .
	 */
	public String getMethodExecStatsReport() {
		StringBuilder sb = new StringBuilder()
			.append(" Method                         Runs      Running   Errors   Avg          Total     \n")
			.append("------------------------------ --------- --------- -------- ------------ -----------\n");
		getMethodExecStats()
			.stream()
			.sorted(Comparator.comparingDouble(MethodExecStats::getTotalTime).reversed())
			.forEach(x -> sb.append(String.format("%30s %9d %9d %9d %10dms %10dms\n", x.getMethod(), x.getRuns(), x.getRunning(), x.getErrors(), x.getAvgTime(), x.getTotalTime())));
		return sb.toString();
	}

	/**
	 * Finds the {@link RestMethodParam} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param mi The Java method being called.
	 * @param isPreOrPost Whether this is a {@link HookEvent#PRE_CALL} or {@link HookEvent#POST_CALL}.
	 * @param urlPathMatcher The path pattern to match against.
	 * @return The array of resolvers.
	 * @throws ServletException If an annotation usage error was detected.
	 */
	protected RestMethodParam[] findParams(MethodInfo mi, boolean isPreOrPost, UrlPathMatcher urlPathMatcher) throws ServletException {

		List<ClassInfo> pt = mi.getParamTypes();
		RestMethodParam[] rp = new RestMethodParam[pt.size()];
		PropertyStore ps = getPropertyStore();

		for (int i = 0; i < pt.size(); i++) {

			ClassInfo t = pt.get(i);
			if (t.inner() != null) {
				Class<?> c = t.inner();
				rp[i] = paramResolvers.get(c);
				if (rp[i] == null)
					rp[i] = RestParamDefaults.STANDARD_RESOLVERS.get(c);
			}

			ParamInfo mpi = mi.getParam(i);

			if (mpi.hasAnnotation(Header.class)) {
				rp[i] = new RestParamDefaults.HeaderObject(mpi, ps);
			} else if (mpi.hasAnnotation(Attr.class)) {
				rp[i] = new RestParamDefaults.AttributeObject(mpi, ps);
			} else if (mpi.hasAnnotation(Query.class)) {
				rp[i] = new RestParamDefaults.QueryObject(mpi, ps);
			} else if (mpi.hasAnnotation(FormData.class)) {
				rp[i] = new RestParamDefaults.FormDataObject(mpi, ps);
			} else if (mpi.hasAnnotation(Path.class)) {
				rp[i] = new RestParamDefaults.PathObject(mpi, ps, urlPathMatcher);
			} else if (mpi.hasAnnotation(Body.class)) {
				rp[i] = new RestParamDefaults.BodyObject(mpi, ps);
			} else if (mpi.hasAnnotation(Request.class)) {
				rp[i] = new RestParamDefaults.RequestObject(mpi, ps);
			} else if (mpi.hasAnnotation(Response.class)) {
				rp[i] = new RestParamDefaults.ResponseObject(mpi, ps);
			} else if (mpi.hasAnnotation(ResponseHeader.class)) {
				rp[i] = new RestParamDefaults.ResponseHeaderObject(mpi, ps);
			} else if (mpi.hasAnnotation(ResponseStatus.class)) {
				rp[i] = new RestParamDefaults.ResponseStatusObject(t);
			} else if (mpi.hasAnnotation(HasFormData.class)) {
				rp[i] = new RestParamDefaults.HasFormDataObject(mpi);
			} else if (mpi.hasAnnotation(HasQuery.class)) {
				rp[i] = new RestParamDefaults.HasQueryObject(mpi);
			} else if (mpi.hasAnnotation(org.apache.juneau.rest.annotation.Method.class)) {
				rp[i] = new RestParamDefaults.MethodObject(mi, t, mpi);
			}

			if (rp[i] == null && ! isPreOrPost)
				throw new RestServletException("Invalid parameter specified for method ''{0}'' at index position {1}", mi.inner(), i);
		}

		return rp;
	}


	//------------------------------------------------------------------------------------------------------------------
	// Call handling
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Wraps an incoming servlet request/response pair into a single {@link RestCall} object.
	 *
	 * <p>
	 * This is the first method called by {@link #execute(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param req The rest request.
	 * @param res The rest response.
	 * @return The wrapped request/response pair.
	 */
	protected RestCall createCall(HttpServletRequest req, HttpServletResponse res) {
		return new RestCall(this, req, res).logger(getCallLogger());
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 *
	 * <p>
	 * This method is called immediately after {@link #startCall(RestCall)} has been called.
	 *
	 * @param call The current REST call.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	public RestRequest createRequest(RestCall call) throws ServletException {
		return new RestRequest(call);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * and the request returned by {@link #createRequest(RestCall)}.
	 *
	 * @param call The current REST call.
	 * @return The wrapped response object.
	 * @throws ServletException If any errors occur trying to interpret the request or response.
	 */
	public RestResponse createResponse(RestCall call) throws ServletException {
		return new RestResponse(call);
	}

	/**
	 * The main service method.
	 *
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException General servlet exception.
	 * @throws IOException Thrown by underlying stream.
	 */
	public void execute(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		RestCall call = createCall(r1, r2);

		// Must be careful not to bleed thread-locals.
		if (this.call.get() != null)
			System.err.println("WARNING:  Thread-local call object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");
		this.call.set(call);

		try {
			checkForInitException();

			// If the resource path contains variables (e.g. @Rest(path="/f/{a}/{b}"), then we want to resolve
			// those variables and push the servletPath to include the resolved variables.  The new pathInfo will be
			// the remainder after the new servletPath.
			// Only do this for the top-level resource because the logic for child resources are processed next.
			if (pathMatcher.hasVars() && getParentContext() == null) {
				String sp = call.getServletPath();
				String pi = call.getPathInfoUndecoded();
				UrlPath upi2 = UrlPath.of(pi == null ? sp : sp + pi);
				UrlPathMatch uppm = pathMatcher.match(upi2);
				if (uppm != null && ! uppm.hasEmptyVars()) {
					call.addPathVars(uppm.getVars());
					call.request(
						new OverrideableHttpServletRequest(call.getRequest())
							.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
							.servletPath(uppm.getPrefix())
					);
				} else {
					call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
					return;
				}
			}

			// If this resource has child resources, try to recursively call them.
			String pi = call.getPathInfoUndecoded();
			if ((! childResources.isEmpty()) && pi != null && ! pi.equals("/")) {
				for (RestContext rc : getChildResources().values()) {
					UrlPathMatcher upp = rc.pathMatcher;
					UrlPathMatch uppm = upp.match(call.getUrlPath());
					if (uppm != null) {
						if (! uppm.hasEmptyVars()) {
							call.addPathVars(uppm.getVars());
							HttpServletRequest childRequest = new OverrideableHttpServletRequest(call.getRequest())
								.pathInfo(nullIfEmpty(urlDecode(uppm.getSuffix())))
								.servletPath(call.getServletPath() + uppm.getPrefix());
							rc.execute(childRequest, call.getResponse());
						} else {
							call.debug(isDebug(call)).status(SC_NOT_FOUND).finish();
						}
						return;
					}
				}
			}

			if (isDebug(call))
				call.debug(true);

			startCall(call);

			createRequest(call);
			createResponse(call);

			// If the specified method has been defined in a subclass, invoke it.
			try {
				findMethod(call).invoke(call);
			} catch (NotFound e) {
				if (call.getStatus() == 0)
					call.status(404);
				call.exception(e);
				handleNotFound(call);
			}

			if (call.hasOutput()) {
				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				handleResponse(call);
			}


		} catch (Throwable e) {
			handleError(call, convertThrowable(e));
		} finally {
			clearState();
		}

		call.finish();
		finishCall(call);
	}

	private RestMethodContext findMethod(RestCall call) throws Throwable {
		String m = call.getMethod();

		int rc = 0;
		if (methodMap.containsKey(m)) {
			for (RestMethodContext mc : methodMap.get(m)) {
				int mrc = mc.match(call);
				if (mrc == 2)
					return mc;
				rc = Math.max(rc, mrc);
			}
		}

		if (methodMap.containsKey("*")) {
			for (RestMethodContext mc : methodMap.get("*")) {
				int mrc = mc.match(call);
				if (mrc == 2)
					return mc;
				rc = Math.max(rc, mrc);
			}
		}

		// If no paths matched, see if the path matches any other methods.
		// Note that we don't want to match against "/*" patterns such as getOptions().
		if (rc == 0) {
			for (RestMethodContext mc : methods) {
				if (! mc.getPathPattern().endsWith("/*")) {
					int mrc = mc.match(call);
					if (mrc == 2)
						throw new MethodNotAllowed();
				}
			}
		}

		if (rc == 1)
			throw new PreconditionFailed("Method ''{0}'' not found on resource on path ''{1}'' with matching matcher.", m, call.getPathInfo());

		throw new NotFound("Java method matching path ''{0}'' not found on resource ''{1}''.", call.getPathInfo(), resource.getClass().getName());
	}

	private boolean isDebug(RestCall call) {
		Enablement e = null;
		RestMethodContext mc = call.getRestMethodContext();
		if (mc != null)
			e = mc.getDebug();
		if (e == null)
			e = getDebug();
		if (e == ALWAYS)
			return true;
		if (e == NEVER)
			return false;
		if (e == CONDITIONAL)
			return "true".equalsIgnoreCase(call.getRequest().getHeader("X-Debug"));
		return false;
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or
	 * returned by the Java method.
	 *
	 * <p>
	 * Subclasses may override this method if they wish to modify the way the output is rendered or support other output
	 * formats.
	 *
	 * <p>
	 * The default implementation simply iterates through the response handlers on this resource
	 * looking for the first one whose {@link ResponseHandler#handle(RestRequest,RestResponse)} method returns
	 * <jk>true</jk>.
	 *
	 * @param call The HTTP call.
	 * @throws IOException Thrown by underlying stream.
	 * @throws HttpException Non-200 response.
	 * @throws NotImplemented No registered response handlers could handle the call.
	 */
	public void handleResponse(RestCall call) throws IOException, HttpException, NotImplemented {

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		// Loop until we find the correct handler for the POJO.
		for (ResponseHandler h : getResponseHandlers())
			if (h.handle(req, res))
				return;

		Object output = res.getOutput();
		throw new NotImplemented("No response handlers found to process output of type '"+(output == null ? null : output.getClass().getName())+"'");
	}

	/**
	 * Method that can be subclassed to allow uncaught throwables to be treated as other types of throwables.
	 *
	 * <p>
	 * The default implementation looks at the throwable class name to determine whether it can be converted to another type:
	 *
	 * <ul>
	 * 	<li><js>"*AccessDenied*"</js> - Converted to {@link Unauthorized}.
	 * 	<li><js>"*Empty*"</js>,<js>"*NotFound*"</js> - Converted to {@link NotFound}.
	 * </ul>
	 *
	 * @param t The thrown object.
	 * @return The converted thrown object.
	 */
	public Throwable convertThrowable(Throwable t) {

		ClassInfo ci = ClassInfo.ofc(t);
		if (ci.is(InvocationTargetException.class)) {
			t = ((InvocationTargetException)t).getTargetException();
			ci = ClassInfo.ofc(t);
		}

		if (ci.is(HttpRuntimeException.class)) {
			t = ((HttpRuntimeException)t).getInner();
			ci = ClassInfo.ofc(t);
		}

		if (ci.hasAnnotation(Response.class))
			return t;

		if (t instanceof ParseException || t instanceof InvalidDataConversionException)
			return new BadRequest(t);

		String n = t.getClass().getName();

		if (n.contains("AccessDenied") || n.contains("Unauthorized"))
			return new Unauthorized(t);

		if (n.contains("Empty") || n.contains("NotFound"))
			return new NotFound(t);

		return t;
	}

	/**
	 * Handle the case where a matching method was not found.
	 *
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param call The HTTP call.
	 * @throws Exception Any exception can be thrown.
	 */
	public void handleNotFound(RestCall call) throws Exception {
		String pathInfo = call.getPathInfo();
		String methodUC = call.getMethod();
		int rc = call.getStatus();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new NotFound("Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new PreconditionFailed("Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new MethodNotAllowed("Method ''{0}'' not found on resource{1}.", methodUC, onPath);
		else
			throw new ServletException("Invalid method response: " + rc, call.getException());
	}

	/**
	 * Method for handling response errors.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param call The rest call.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	public synchronized void handleError(RestCall call, Throwable e) throws IOException {

		call.exception(e);

		if (call.isDebug())
			e.printStackTrace();

		int code = 500;

		ClassInfo ci = ClassInfo.ofc(e);
		Response r = ci.getLastAnnotation(Response.class);
		if (r != null)
			if (r.code().length > 0)
				code = r.code()[0];

		HttpException e2 = (e instanceof HttpException ? (HttpException)e : new HttpException(e, code));

		HttpServletRequest req = call.getRequest();
		HttpServletResponse res = call.getResponse();

		Throwable t = null;
		if (e instanceof HttpRuntimeException)
			t = ((HttpRuntimeException)e).getInner();
		if (t == null)
			t = e2.getRootCause();
		if (t != null) {
			res.setHeader("Exception-Name", stripInvalidHttpHeaderChars(t.getClass().getName()));
			res.setHeader("Exception-Message", stripInvalidHttpHeaderChars(t.getMessage()));
		}

		try {
			res.setContentType("text/plain");
			res.setHeader("Content-Encoding", "identity");
			res.setStatus(e2.getStatus());

			PrintWriter w = null;
			try {
				w = res.getWriter();
			} catch (IllegalStateException x) {
				w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), UTF8));
			}

			try (PrintWriter w2 = w) {
				String httpMessage = RestUtils.getHttpResponseText(e2.getStatus());
				if (httpMessage != null)
					w2.append("HTTP ").append(String.valueOf(e2.getStatus())).append(": ").append(httpMessage).append("\n\n");
				if (isRenderResponseStackTraces())
					e.printStackTrace(w2);
				else
					w2.append(e2.getFullStackMessage(true));
			}

		} catch (Exception e1) {
			req.setAttribute("Exception", e1);
		}
	}

	/**
	 * Returns the session objects for the specified request.
	 *
	 * <p>
	 * The default implementation simply returns a single map containing <c>{'req':req,'res',res}</c>.
	 *
	 * @param call The current REST call.
	 * @return The session objects for that request.
	 */
	public Map<String,Object> getSessionObjects(RestCall call) {
		Map<String,Object> m = new HashMap<>();
		m.put("req", call.getRequest());
		m.put("res", call.getResponse());
		return m;
	}

	/**
	 * Called at the start of a request to invoke all {@link HookEvent#START_CALL} methods.
	 *
	 * @param call The current request.
	 */
	protected void startCall(RestCall call) {
		for (int i = 0; i < startCallMethods.length; i++)
			startOrFinish(resource, startCallMethods[i], startCallMethodParams[i], call.getRequest(), call.getResponse());
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#PRE_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws HttpException If thrown from call methods.
	 */
	protected void preCall(RestCall call) throws HttpException {
		for (int i = 0; i < preCallMethods.length; i++)
			preOrPost(resource, preCallMethods[i], preCallMethodParams[i], call);
	}

	/**
	 * Called during a request to invoke all {@link HookEvent#POST_CALL} methods.
	 *
	 * @param call The current request.
	 * @throws HttpException If thrown from call methods.
	 */
	protected void postCall(RestCall call) throws HttpException {
		for (int i = 0; i < postCallMethods.length; i++)
			preOrPost(resource, postCallMethods[i], postCallMethodParams[i], call);
	}

	private static void preOrPost(Object resource, MethodInvoker m, RestMethodParam[] mp, RestCall call) throws HttpException {
		if (m != null) {
			Object[] args = new Object[mp.length];
			for (int i = 0; i < mp.length; i++) {
				try {
					args[i] = mp[i].resolve(call.getRestRequest(), call.getRestResponse());
				} catch (Exception e) {
					throw toHttpException(e, BadRequest.class, "Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.", mp[i].getParamType().name(), mp[i].getName(), mp[i].getType(), m.getDeclaringClass().getName(), m.getName());
				}
			}
			try {
				m.invoke(resource, args);
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}
	}

	/**
	 * Called at the end of a request to invoke all {@link HookEvent#END_CALL} methods.
	 *
	 * <p>
	 * This is the very last method called in {@link #execute(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param call The current request.
	 */
	protected void finishCall(RestCall call) {
		for (int i = 0; i < endCallMethods.length; i++)
			startOrFinish(resource, endCallMethods[i], endCallMethodParams[i], call.getRequest(), call.getResponse());
	}

	private static void startOrFinish(Object resource, MethodInvoker m, Class<?>[] p, HttpServletRequest req, HttpServletResponse res) throws HttpException, InternalServerError {
		if (m != null) {
			Object[] args = new Object[p.length];
			for (int i = 0; i < p.length; i++) {
				if (p[i] == HttpServletRequest.class)
					args[i] = req;
				else if (p[i] == HttpServletResponse.class)
					args[i] = res;
			}
			try {
				m.invoke(resource, args);
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT} methods.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public synchronized RestContext postInit() throws ServletException {
		for (int i = 0; i < postInitMethods.length; i++)
			postInitOrDestroy(resource, postInitMethods[i], postInitMethodParams[i]);
		for (RestContext childContext : this.childResources.values())
			childContext.postInit();
		return this;
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#POST_INIT_CHILD_FIRST} methods.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInitChildFirst() throws ServletException {
		for (RestContext childContext : this.childResources.values())
			childContext.postInitChildFirst();
		for (int i = 0; i < postInitChildFirstMethods.length; i++)
			postInitOrDestroy(resource, postInitChildFirstMethods[i], postInitChildFirstMethodParams[i]);
		return this;
	}

	private void postInitOrDestroy(Object r, MethodInvoker m, Class<?>[] p) {
		if (m != null) {
			Object[] args = new Object[p.length];
			for (int i = 0; i < p.length; i++) {
				if (p[i] == RestContext.class)
					args[i] = this;
				else if (p[i] == RestContextBuilder.class)
					args[i] = this.builder;
				else if (p[i] == ServletConfig.class)
					args[i] = this.builder.inner;
			}
			try {
				m.invoke(r, args);
			} catch (Exception e) {
				if (e instanceof RuntimeException && ClassInfo.of(e).hasAnnotation(Response.class))
					throw (RuntimeException)e;
				throw new InternalServerError(e);
			}
		}
	}

	/**
	 * Called during servlet initialization to invoke all {@link HookEvent#DESTROY} methods.
	 */
	protected void destroy() {
		for (int i = 0; i < destroyMethods.length; i++) {
			try {
				postInitOrDestroy(resource, destroyMethods[i], destroyMethodParams[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (RestContext r : childResources.values()) {
			r.destroy();
			if (r.resource instanceof Servlet)
				((Servlet)r.resource).destroy();
		}
	}

	/**
	 * Returns the HTTP request object for the current request.
	 *
	 * @return The HTTP request object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestRequest getRequest() {
		RestCall rc = call.get();
		return rc == null ? null : rc.getRestRequest();
	}

	/**
	 * Returns the HTTP response object for the current request.
	 *
	 * @return The HTTP response object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestResponse getResponse() {
		RestCall rc = call.get();
		return rc == null ? null : rc.getRestResponse();
	}

	/**
	 * If the specified object is annotated with {@link Response}, this returns the response metadata about that object.
	 *
	 * @param o The object to check.
	 * @return The response metadata, or <jk>null</jk> if it wasn't annotated with {@link Response}.
	 */
	public ResponseBeanMeta getResponseBeanMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponseBeanMeta rbm = responseBeanMetas.get(c);
		if (rbm == null) {
			rbm = ResponseBeanMeta.create(c, serializers.getPropertyStore());
			if (rbm == null)
				rbm = ResponseBeanMeta.NULL;
			responseBeanMetas.put(c, rbm);
		}
		if (rbm == ResponseBeanMeta.NULL)
			return null;
		return rbm;
	}

	Enablement getDebug() {
		return debug;
	}

	/**
	 * Clear any request state information on this context.
	 * This should always be called in a finally block in the RestServlet.
	 */
	void clearState() {
		call.remove();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("RestContext", new DefaultFilteringOMap()
				.a("allowBodyParam", allowBodyParam)
				.a("allowedMethodHeader", allowedMethodHeaders)
				.a("allowedMethodParams", allowedMethodParams)
				.a("allowedHeaderParams", allowedHeaderParams)
				.a("clientVersionHeader", clientVersionHeader)
				.a("consumes", consumes)
				.a("fileFinder", fileFinder)
				.a("infoProvider", infoProvider)
				.a("paramResolvers", paramResolvers)
				.a("parsers", parsers)
				.a("partParser", partParser)
				.a("partSerializer", partSerializer)
				.a("produces", produces)
				.a("renderResponseStackTraces", renderResponseStackTraces)
				.a("reqHeaders", reqHeaders)
				.a("resHeaders", resHeaders)
				.a("resourceResolver", resourceResolver)
				.a("responseHandlers", responseHandlers)
				.a("serializers", serializers)
				.a("staticFiles", staticFiles)
				.a("uriAuthority", uriAuthority)
				.a("uriContext", uriContext)
				.a("uriRelativity", uriRelativity)
				.a("uriResolution", uriResolution)
			);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helpers.
	//-----------------------------------------------------------------------------------------------------------------

	static class MethodMapBuilder  {
		TreeMap<String,TreeSet<RestMethodContext>> map = new TreeMap<>();
		Set<RestMethodContext> set = ASet.of();


		MethodMapBuilder add(String httpMethodName, RestMethodContext mc) {
			httpMethodName = httpMethodName.toUpperCase();
			if (! map.containsKey(httpMethodName))
				map.put(httpMethodName, new TreeSet<>());
			map.get(httpMethodName).add(mc);
			set.add(mc);
			return this;
		}

		Map<String,List<RestMethodContext>> getMap() {
			AMap<String,List<RestMethodContext>> m = AMap.of();
			for (Map.Entry<String,TreeSet<RestMethodContext>> e : map.entrySet())
				m.put(e.getKey(), AList.of(e.getValue()));
			return m.unmodifiable();
		}

		List<RestMethodContext> getList() {
			return AList.of(set).unmodifiable();
		}
	}
}
