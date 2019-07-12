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
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.FormattedIllegalArgumentException.*;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.activation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.config.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.StreamResource;
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
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.remote.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.reshandlers.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.soap.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xmlschema.XmlSchemaDocSerializer;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.RestContext}
 * </ul>
 */
@ConfigurableContext(nocache=true)
public final class RestContext extends BeanContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestContext";

	/**
	 * Configuration property:  Allow body URL parameter.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowBodyParam.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#allowBodyParam()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#allowBodyParam(boolean)}
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
	 * 	<ja>@RestResource</ja>(allowBodyParam=<js>"$C{REST/allowBodyParam,false}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.allowBodyParam(<jk>false</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_allowBodyParam</jsf>, <jk>false</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.allowBodyParam(<jk>false</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>'body'</js> parameter name is case-insensitive.
	 * 	<li>
	 * 		Useful for debugging PUT and POST methods using only a browser.
	 * </ul>
	 */
	public static final String REST_allowBodyParam = PREFIX + ".allowBodyParam.b";

	/**
	 * Configuration property:  Allowed header URL parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedHeaderParams.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited list)
	 * 	<li><b>Default:</b>  <js>"Accept,Content-Type"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#allowedHeaderParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#allowedHeaderParams(String)}
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
	 * 	<ja>@RestResource</ja>(allowedHeaderParams=<js>"Accept,Content-Type"</js>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedMethodHeaders.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited list)
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#allowedMethodHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#allowedMethodHeaders(String)}
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
	 * 	<ja>@RestResource</ja>(allowedMethodHeaders=<js>"PATCH"</js>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedMethodParams.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c> (comma-delimited list)
	 * 	<li><b>Default:</b>  <js>"HEAD,OPTIONS"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#allowedMethodParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#allowedMethodParams(String)}
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
	 * Note that per the {@doc RFC2616.section9 HTTP specification}, special care should
	 * be taken when allowing non-safe (<c>POST</c>, <c>PUT</c>, <c>DELETE</c>) methods to be invoked through GET requests.
	 *
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@RestResource</ja>(allowedMethodParams=<js>"HEAD,OPTIONS,PUT"</js>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * Configuration property:  Allow header URL parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowHeaderParams.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#allowHeaderParams()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#allowHeaderParams(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:
	 * <p class='bcode w800'>
	 *  ?Accept=text/json&amp;Content-Type=text/json
	 * </p>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(allowMethodParams=<js>"$C{REST/allowHeaderParams,false}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.allowHeaderParams(<jk>false</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_allowHeaderParams</jsf>, <jk>false</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.allowHeaderParams(<jk>false</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Header names are case-insensitive.
	 * 	<li>
	 * 		Useful for debugging REST interface using only a browser.
	 * </ul>
	 * @deprecated Use {@link #REST_allowedHeaderParams}
	 */
	@Deprecated
	public static final String REST_allowHeaderParams = PREFIX + ".allowHeaderParams.b";

	/**
	 * Configuration property:  REST call handler.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.callHandler.o"</js>
	 * 	<li><b>Data type:</b>  {@link RestCallHandler} | <code>Class&lt;? <jk>extends</jk> {@link RestCallHandler}&gt;</code>
	 * 	<li><b>Default:</b>  {@link BasicRestCallHandler}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#callHandler()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#callHandler(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#callHandler(RestCallHandler)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our customized call handler.</jc>
	 * 	<jk>public class</jk> MyRestCallHandler <jk>extends</jk> BasicRestCallHandler {
	 *
	 * 		<jc>// Must provide this constructor!</jc>
	 * 		<jk>public</jk> MyRestCallHandler(RestContext context) {
	 * 			<jk>super</jk>(context);
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> RestRequest createRequest(HttpServletRequest req) <jk>throws</jk> ServletException {
	 * 			<jc>// Low-level handling of requests.</jc>
	 * 			...
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> handleResponse(RestRequest req, RestResponse res, Object output) <jk>throws</jk> IOException, RestException {
	 * 			<jc>// Low-level handling of responses.</jc>
	 * 			...
	 * 		}
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> handleNotFound(int rc, RestRequest req, RestResponse res) <jk>throws</jk> Exception {
	 * 			<jc>// Low-level handling of various error conditions.</jc>
	 * 			...
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(callHandler=MyRestCallHandler.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.callHandler(MyRestCallHandler.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_callHandler</jsf>, MyRestCallHandler.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.callHandler(MyRestCallHandler.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_callHandler = PREFIX + ".callHandler.o";

	/**
	 * Configuration property:  REST call logger.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.callLogger.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link RestCallLogger} | Class&lt;? <jk>extends</jk> {@link RestCallLogger}&gt;</code>
	 * 	<li><b>Default:</b>  {@link BasicRestCallLogger}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#callLogger()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#callLogger(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#callLogger(RestCallLogger)}
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
	 * 	<ja>@RestResource</ja>(callLogger=MyLogger.<jk>class</jk>)
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public static final String REST_callLogger = PREFIX + ".callLogger.o";

	/**
	 * Configuration property:  REST call logging rules.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.callLoggerConfig.o"</js>
	 * 	<li><b>Data type:</b>  <c>RestCallLoggerConfig</c>
	 * 	<li><b>Default:</b>  {@link RestCallLoggerConfig#DEFAULT}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#logging()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#callLoggerConfig(RestCallLoggerConfig)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies rules on how to handle logging of HTTP requests/responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		logging=<ja>@Logging</ja>(
	 * 			level=<js>"INFO"</js>,
	 * 			rules={
	 * 				<ja>@LoggingRule</ja>(codes=<js>"400-499"</js>, level=<js>"WARNING"</js>, req=<js>"SHORT"</js>, res=<js>"MEDIUM"</js>),
	 * 				<ja>@LoggingRule</ja>(codes=<js>">=500"</js>, level=<js>"SEVERE"</js>, req=<js>"LONG"</js>, res=<js>"LONG"</js>)
	 * 			}
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.callLoggerConfig(
	 * 				RestCallLoggerConfig
	 * 					.<jsm>create</jsm>()
	 * 					.level(Level.<jsf>INFO</jsf>)
	 * 					.rules(
	 * 						RestCallLoggingRule
	 * 							.<jsm>create</jsm>()
	 * 							.codes(<js>"400-499"</js>)
	 * 							.level(<jsf>WARNING</jsf>)
	 * 							.req(<jsf>SHORT</jsf>)
	 * 							.res(<jsf>MEDIUM</jsf>)
	 * 							.build(),
	 * 						RestCallLoggingRule
	 * 							.<jsm>create</jsm>()
	 * 							.codes(<js>">=500"</js>)
	 * 							.level(<jsf>SEVERE</jsf>)
	 * 							.req(<jsf>LONG</jsf>)
	 * 							.res(<jsf>LONG</jsf>)
	 * 							.build()
	 * 					)
	 * 					.build()
	 * 			);
	 *
	 * 			<jc>// Same, but using property with JSON value.</jc>
	 * 			builder.set(<jsf>REST_callLoggerConfig</jsf>, <js>"{level:'INFO',rules:[{codes:'400-499',level:'WARNING',...},...]}"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 */
	public static final String REST_callLoggerConfig = PREFIX + ".callLoggerConfig.o";

	/**
	 * Configuration property:  Children.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.children.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;Class | Object | {@link RestChild}&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#children()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#child(String,Object)}
	 * 			<li class='jm'>{@link RestContextBuilder#children(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#children(Object...)}
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
	 * Child resources must specify a value for {@link RestResource#path() @RestResource(path)} that identifies the subpath of the child resource
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
	 * 	<ja>@RestResource</ja>(path=<js>"/child"</js>)
	 * 	<jk>public class</jk> MyChildResource {...}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@RestResource</ja>(children={MyChildResource.<jk>class</jk>})
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
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.Children}
	 * </ul>
	 */
	public static final String REST_children = PREFIX + ".children.lo";

	/**
	 * Configuration property:  Classpath resource finder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.classpathResourceFinder.o"</js>
	 * 	<li><b>Data type:</b>  {@link ClasspathResourceFinder}
	 * 	<li><b>Default:</b>  {@link ClasspathResourceFinderBasic}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#classpathResourceFinder()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#classpathResourceFinder(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#classpathResourceFinder(ClasspathResourceFinder)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 *
	 * <p>
	 * Used by the following methods:
	 * <ul>
	 * 	<li class='jc'>{@link RestContext}
	 * 	<ul>
	 * 		<li class='jm'>{@link #getClasspathResource(String,Locale) getClasspathResource(String,Locale)}
	 * 		<li class='jm'>{@link #getClasspathResource(Class,String,Locale) getClasspathResource(Class,String,Locale)}
	 * 		<li class='jm'>{@link #getClasspathResource(Class,MediaType,String,Locale) getClasspathResource(Class,MediaType,String,Locale)}
	 * 		<li class='jm'>{@link #getClasspathResource(Class,Class,MediaType,String,Locale) getClasspathResource(Class,Class,MediaType,String,Locale)}
	 * 		<li class='jm'>{@link #getClasspathResourceAsString(String,Locale) getClasspathResourceAsString(String,Locale)}
	 * 		<li class='jm'>{@link #getClasspathResourceAsString(Class,String,Locale) getClasspathResourceAsString(Class,String,Locale)}
	 * 		<li class='jm'>{@link #resolveStaticFile(String) resolveStaticFile(String)}
	 * 	</ul>
	 * 	<li class='jc'>{@link RestRequest}
	 * 	<ul>
	 * 		<li class='jm'>{@link RestRequest#getClasspathReaderResource(String) getClasspathReaderResource(String)}
	 * 		<li class='jm'>{@link RestRequest#getClasspathReaderResource(String,boolean) getClasspathReaderResource(String,boolean)}
	 * 		<li class='jm'>{@link RestRequest#getClasspathReaderResource(String,boolean,MediaType,boolean) getClasspathReaderResource(String,boolean,MediaType,boolean)}
	 * 	</ul>
	 * </ul>
	 *
	 * <p>
	 * It also affects the behavior of the {@link #REST_staticFiles} property.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our customized classpath resource finder.</jc>
	 * 	<jk>public class</jk> MyClasspathResourceFinder <jk>extends</jk> ClasspathResourceFinderBasic {
	 * 		<ja>@Override</ja>
	 * 		<jk>public</jk> InputStream findResource(Class&lt;?&gt; baseClass, String name, Locale locale) <jk>throws</jk> IOException {
	 * 			<jc>// Do your own resolution.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation.</jc>
	 * 	<ja>@RestResource</ja>(classpathResourceFinder=MyClasspathResourceFinder.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.classpathResourceFinder(MyClasspathResourceFinder.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_classpathResourceFinder</jsf>, MyClasspathResourceFinder.<jk>class</jk>));
	 *
	 * 			<jc>// Use a pre-instantiated object instead.</jc>
	 * 			builder.classpathResourceFinder(<jk>new</jk> MyClasspathResourceFinder());
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.classpathResourceFinder(MyClasspathResourceFinder.<jk>class</jk>);
	 * 		}
	 * 	}
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The default value is {@link ClasspathResourceFinderBasic} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * 		<br>The {@link ClasspathResourceFinderRecursive} is another option that also recursively searches for resources
	 * 		up the class-hierarchy.
	 * 		<br>Each of these classes can be extended to provide customized handling of resource retrieval.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_classpathResourceFinder = PREFIX + ".classpathResourceFinder.o";

	/**
	 * Configuration property:  Client version header.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.clientVersionHeader.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"X-Client-Version"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#clientVersionHeader()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#clientVersionHeader(String)}
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
	 * 	<ja>@RestResource</ja>(clientVersionHeader=<js>"$C{REST/clientVersionHeader,Client-Version}"</js>)
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
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2() {
	 * 		...
	 * 	}
	 *
	 * 	<jc>// Call this method if Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(name=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.converters.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link RestConverter} | Class&lt;? <jk>extends</jk> {@link RestConverter}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#converters()}
	 * 			<li class='ja'>{@link RestMethod#converters()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#converters(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#converters(RestConverter...)}
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
	 * 	<ja>@RestResource</ja>(converters={MyConverter.<jk>class</jk>})
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jc'>{@link Traversable} - Allows URL additional path info to address individual elements in a POJO tree.
	 * 	<li class='jc'>{@link Queryable} - Allows query/view/sort functions to be performed on POJOs.
	 * 	<li class='jc'>{@link Introspectable} - Allows Java public methods to be invoked on the returned POJOs.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Converters}
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link Enablement}
	 * 	<li><b>Default:</b>  {@link Enablement#FALSE}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#debug()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#debug(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Enables the following:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		HTTP request/response bodies are cached in memory for logging purposes.
	 * </ul>
	 */
	public static final String REST_debug = PREFIX + ".debug.s";

	/**
	 * Configuration property:  Default character encoding.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"utf-8"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#defaultCharset()}
	 * 			<li class='ja'>{@link RestMethod#defaultCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#defaultCharset(String)}
	 * 			<li class='jm'>{@link RestContextBuilder#defaultCharset(Charset)}
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
	 * 	<ja>@RestResource</ja>(defaultCharset=<js>"$C{REST/defaultCharset,US-ASCII}"</js>)
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
	 * Configuration property:  Default request attributes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.attrs.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#attrs()}
	 * 			<li class='ja'>{@link RestMethod#attrs()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#attrs(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#attr(String,Object)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for request attributes if they're not already set on the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * 	<ja>@RestResource</ja>(defaultRequestAttributes={<js>"Foo: bar"</js>, <js>"Baz: $C{REST/myAttributeValue}"</js>})
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
	 * 			builder.addTo(<jsf>REST_attrs</jsf>, <js>"Foo"</js>, <js>"bar"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.attr(<js>"Foo"</js>, <js>"bar"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(attrs={<js>"Foo: bar"</js>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 */
	public static final String REST_attrs = PREFIX + ".attrs.smo";

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultRequestHeaders.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#defaultRequestHeaders()}
	 * 			<li class='ja'>{@link RestMethod#defaultRequestHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li class='jm'>{@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * 	<ja>@RestResource</ja>(defaultRequestHeaders={<js>"Accept: application/json"</js>, <js>"My-Header: $C{REST/myHeaderValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.defaultRequestHeader(<js>"Accept"</js>, <js>"application/json"</js>);
	 * 				.defaultRequestHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_defaultRequestHeaders</jsf>, <js>"Accept"</js>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.defaultRequestHeader(<js>"Accept"</js>, <js>"application/json"</js>);
	 * 		}
	 *
	 * 		<jc>// Override at the method level.</jc>
	 * 		<ja>@RestMethod</ja>(defaultRequestHeaders={<js>"Accept: text/xml"</js>})
	 * 		public Object myMethod() {...}
	 * 	}
	 * </p>
	 */
	public static final String REST_defaultRequestHeaders = PREFIX + ".defaultRequestHeaders.smo";

	/**
	 * Configuration property:  Default response headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultResponseHeaders.omo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#defaultResponseHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li class='jm'>{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for response headers if they're not set after the Java REST method is called.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * 	<ja>@RestResource</ja>(defaultResponseHeaders={<js>"Content-Type: $C{REST/defaultContentType,text/plain}"</js>,<js>"My-Header: $C{REST/myHeaderValue}"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.defaultResponseHeader(<js>"Content-Type"</js>, <js>"text/plain"</js>);
	 * 				.defaultResponseHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder
	 * 				.addTo(<jsf>REST_defaultRequestHeaders</jsf>, <js>"Accept"</js>, <js>"application/json"</js>);
	 * 				.addTo(<jsf>REST_defaultRequestHeaders</jsf>, <js>"My-Header"</js>, <js>"foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.defaultResponseHeader(<js>"Content-Type"</js>, <js>"text/plain"</js>);
	 * 		}
	 * 	}
	 * </p>
	 */
	public static final String REST_defaultResponseHeaders = PREFIX + ".defaultResponseHeaders.omo";

	/**
	 * Configuration property:  Compression encoders.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.encoders.o"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link Encoder} | Class&lt;? <jk>extends</jk> {@link Encoder}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#encoders()}
	 * 			<li class='ja'>{@link RestMethod#encoders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#encoders(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#encoders(Encoder...)}
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
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Encoders}
	 * </ul>
	 */
	public static final String REST_encoders = PREFIX + ".encoders.lo";

	/**
	 * Configuration property:  Class-level guards.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.guards.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link RestGuard} | Class&lt;? <jk>extends</jk> {@link RestGuard}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#guards()}
	 * 			<li class='ja'>{@link RestMethod#guards()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#guards(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#guards(RestGuard...)}
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
	 * 	<ja>@RestResource</ja>(guards={BillyGuard.<jk>class</jk>})
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Guards}
	 * </ul>
	 */
	public static final String REST_guards = PREFIX + ".guards.lo";

	/**
	 * Configuration property:  REST info provider.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.infoProvider.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link RestInfoProvider} | Class&lt;? <jk>extends</jk> {@link RestInfoProvider}&gt;</code>
	 * 	<li><b>Default:</b>  {@link BasicRestInfoProvider}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#infoProvider()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#infoProvider(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#infoProvider(RestInfoProvider)}
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
	 * 	<ja>@RestResource</ja>(infoProvider=MyRestInfoProvider.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 */
	public static final String REST_infoProvider = PREFIX + ".infoProvider.o";

	/**
	 * Configuration property:  REST logger.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.logger.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link RestLogger} | Class&lt;? <jk>extends</jk> {@link RestLogger}&gt;</code>
	 * 	<li><b>Default:</b>  {@link BasicRestLogger}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#logger()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#logger(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#logger(RestLogger)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the logger to use for logging.
	 *
	 * <p>
	 * Two implementations are provided by default:
	 * <ul>
	 * 	<li class='jc'>{@link BasicRestLogger} - Default logging.
	 * 	<li class='jc'>{@link NoOpRestLogger} - Logging disabled.
	 * </ul>
	 *
	 * <p>
	 * Loggers are accessible through the following:
	 * <ul>
	 * 	<li class='jm'>{@link RestContext#getLogger() RestContext.getLogger()}
	 * 	<li class='jm'>{@link RestRequest#getLogger() RestRequest.getLogger()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Our customized logger.</jc>
	 * 	<jk>public class</jk> MyRestLogger <jk>extends</jk> BasicRestLogger {
	 *
	 * 		<ja>@Override</ja>
	 * 		<jk>public void</jk> log(Level level, Throwable cause, String msg, Object...args) {
	 * 			<jc>// Handle logging ourselves.</jc>
	 * 		}
	 * 	}
	 *
	 * 	<jc>// Option #1 - Registered via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(logger=MyRestLogger.<jk>class</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Registered via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.logger(MyRestLogger.<jk>class</jk>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_logger</jsf>, MyRestLogger.<jk>class</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Registered via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.logger(MyRestLogger.<jk>class</jk>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
	 * </ul>
	 * @deprecated Use {@link #REST_callLogger}
	 */
	@Deprecated
	public static final String REST_logger = PREFIX + ".logger.o";

	/**
	 * Configuration property:  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.maxInput.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"100M"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#maxInput()}
	 * 			<li class='ja'>{@link RestMethod#maxInput()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#maxInput(String)}
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
	 * 	<ja>@RestResource</ja>(maxInput=<js>"$C{REST/maxInput,10M}"</js>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.messages.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link MessageBundleLocation}&gt;</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#messages()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#messages(String)},
	 * 			<li class='jm'>{@link RestContextBuilder#messages(Class,String)}
	 * 			<li class='jm'>{@link RestContextBuilder#messages(MessageBundleLocation...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * This annotation is used to provide localized messages for the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li class='jm'>{@link RestContext#getMessages() RestContext.getMessages()}
	 * </ul>
	 *
	 * <p>
	 * Messages are also available by passing either of the following parameter types into your Java method:
	 * <ul>
	 * 	<li class='jc'>{@link ResourceBundle} - Basic Java resource bundle.
	 * 	<li class='jc'>{@link MessageBundle} - Extended resource bundle with several convenience methods.
	 * </ul>
	 *
	 * <p>
	 * Messages passed into Java methods already have their locale set to that of the incoming request.
	 *
	 * <p>
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
	 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
	 * absolute path like <js>"com.foo.sample.nls.Messages"</js>
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> org.apache.foo;
	 *
	 * 	<jc>// Resolve messages to org/apache/foo/nls/MyMessages.properties</jc>
	 * 	<ja>@RestResource</ja>(messages=<js>"nls/MyMessages"</js>)
	 * 	<jk>public class</jk> MyResource {...}
	 *
	 * 		<ja>@RestMethod</ja>(name=<js>"GET"</js>, path=<js>"/hello/{you}"</js>)
	 * 		<jk>public</jk> Object helloYou(RestRequest req, MessageBundle messages, <ja>@Path</ja>(<js>"name"</js>) String you)) {
	 * 			String s;
	 *
	 * 			<jc>// Get it from the RestRequest object.</jc>
	 * 			s = req.getMessage(<js>"HelloMessage"</js>, you);
	 *
	 * 			<jc>// Or get it from the method parameter.</jc>
	 * 			s = messages.getString(<js>"HelloMessage"</js>, you);
	 *
	 * 			<jc>// Or get the message in a locale different from the request.</jc>
	 * 			s = messages.getString(Locale.<jsf>UK</jsf>, <js>"HelloMessage"</js>, you);
	 *
	 * 			<jk>return</jk> s;
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li
	 * 		>Mappings are cumulative from super classes.
	 * 		<br>Therefore, you can find and retrieve messages up the class-hierarchy chain.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Messages}
	 * </ul>
	 */
	public static final String REST_messages = PREFIX + ".messages.lo";

	/**
	 * Configuration property:  MIME types.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.mimeTypes.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#mimeTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#mimeTypes(String...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines MIME-type file type mappings.
	 *
	 * <p>
	 * Used for specifying the content type on file resources retrieved through the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestContext#resolveStaticFile(String) RestContext.resolveStaticFile(String)}
	 * 	<li class='jm'>{@link RestRequest#getClasspathReaderResource(String,boolean,MediaType,boolean)}
	 * 	<li class='jm'>{@link RestRequest#getClasspathReaderResource(String,boolean)}
	 * 	<li class='jm'>{@link RestRequest#getClasspathReaderResource(String)}
	 * </ul>
	 *
	 * <p>
	 * This list appends to the existing list provided by {@link ExtendedMimetypesFileTypeMap}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@RestResource</ja>(mimeTypes={<js>"text/plain txt text TXT"</js>})
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.mimeTypes(<js>"text/plain txt text TXT"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.addTo(<jsf>REST_mimeTypes</jsf>, <js>"text/plain txt text TXT"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.mimeTypes(<js>"text/plain txt text TXT"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Values are .mime.types formatted entry string.
	 * 		<br>Example: <js>"image/svg+xml svg"</js>
	 * </ul>
	 */
	public static final String REST_mimeTypes = PREFIX + ".mimeTypes.ss";

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.paramResolvers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link RestMethodParam} | Class&lt;? <jk>extends</jk> {@link RestMethodParam}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#paramResolvers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#paramResolvers(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#paramResolvers(RestMethodParam...)}
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
	 * 	<ja>@RestResource</ja>(paramResolvers=MyRestParam.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(BeanContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.parsers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link Parser} | Class&lt;? <jk>extends</jk> {@link Parser}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#parsers()}
	 * 			<li class='ja'>{@link RestMethod#parsers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#parsers(Object...)}
	 * 			<li class='jm'>{@link RestContextBuilder#parsers(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#parsersReplace(Object...)}
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
	 * <ul>
	 * 	<li class='jm'>{@link Parser#getMediaTypes()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@RestResource</ja>(parsers={JsonParser.<jk>class</jk>, XmlParser.<jk>class</jk>})
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Parsers}
	 * </ul>
	 */
	public static final String REST_parsers = PREFIX + ".parsers.lo";

	/**
	 * Configuration property:  HTTP part parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.partParser.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link HttpPartParser} | Class&lt;? <jk>extends</jk> {@link HttpPartParser}&gt;</code>
	 * 	<li><b>Default:</b>  {@link OpenApiParser}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#partParser()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#partParser(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#partParser(HttpPartParser)}
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
	 * 	<ja>@RestResource</ja>(partParser=SimplePartParser.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link HttpPartSerializer} | Class&lt;? <jk>extends</jk> {@link HttpPartSerializer}&gt;</code>
	 * 	<li><b>Default:</b>  {@link OpenApiSerializer}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#partSerializer()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#partSerializer(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
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
	 * 	<ja>@RestResource</ja>(partSerializer=SimplePartSerializer.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.path.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#path()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#path(String)}
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
	 * 	<ja>@RestResource</ja>(path=<js>"/myResource"</js>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		This annotation is ignored on top-level servlets (i.e. servlets defined in <c>web.xml</c> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>
	 * 		Typically, this setting is only applicable to resources defined as children through the
	 * 		{@link RestResource#children() @RestResource(children)} annotation.
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.renderResponseStackTraces.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 			<li class='jm'>{@link RestContextBuilder#renderResponseStackTraces()}
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
	 * 	<ja>@RestResource</ja>(renderResponseStackTraces=<jk>true</jk>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.renderResponseStackTraces();
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_renderResponseStackTraces</jsf>, <jk>true</jk>);
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 * 	<li>
	 * 		This setting is available through the following method:
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContext#isRenderResponseStackTraces() RestContext.isRenderResponseStackTraces()}
	 * 		</ul>
	 * 		That method is used by {@link BasicRestCallHandler#handleError(HttpServletRequest, HttpServletResponse, Throwable)}.
	 * </ul>
	 */
	public static final String REST_renderResponseStackTraces = PREFIX + ".renderResponseStackTraces.b";

	/**
	 * Configuration property:  REST resource resolver.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.resourceResolver.o"</js>
	 * 	<li><b>Data type:</b>  <code>{@link RestResourceResolver} | Class&lt;? <jk>extends</jk> {@link RestResourceResolver}&gt;</code>
	 * 	<li><b>Default:</b>  {@link BasicRestResourceResolver}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#resourceResolver()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#resourceResolver(Class)}
	 * 			<li class='jm'>{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
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
	 * 	<ja>@RestResource</ja>(resourceResolver=MyResourceResolver.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Unless overridden, resource resolvers are inherited from ascendant resources.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 * 	<li>
	 * 		Inner classes of the REST resource class are allowed.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Instantiation.ResourceResolvers}
	 * 	<li class='link'>{@doc juneau-rest-server.Injection}
	 * </ul>
	 */
	public static final String REST_resourceResolver = PREFIX + ".resourceResolver.o";

	/**
	 * Configuration property:  Response handlers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.responseHandlers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link ResponseHandler} | Class&lt;? <jk>extends</jk> {@link ResponseHandler}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#responseHandlers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#responseHandlers(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
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
	 * 	<ja>@RestResource</ja>(responseHandlers=MyResponseHandler.<jk>class</jk>)
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Response handlers resolvers are always inherited from ascendant resources.
	 * 	<li>
	 * 		When defined as a class, the implementation must have one of the following constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContext)</code>
	 * 			<li><code><jk>public</jk> T()</code>
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.rolesDeclared.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#rolesDeclared()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#rolesDeclared(String...)}
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
	 * 	<ja>@RestResource</ja>(
	 * 		rolesDeclared=<js>"ROLE_ADMIN,ROLE_READ_WRITE,ROLE_READ_ONLY,ROLE_SPECIAL"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE && ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_rolesDeclared}
	 * </ul>
	 */
	public static final String REST_rolesDeclared = PREFIX + ".rolesDeclared.ss";

	/**
	 * Configuration property:  Role guard.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.roleGuard.ss"</js>
	 * 	<li><b>Data type:</b>  <c>Set&lt;String&gt;</c>
	 * 	<li><b>Default:</b>  empty set
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#roleGuard()}
	 * 			<li class='ja'>{@link RestMethod#roleGuard()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#roleGuard(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * An expression defining if a user with the specified roles are allowed to access methods on this class.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/foo"</js>,
	 * 		roleGuard=<js>"ROLE_ADMIN || (ROLE_READ_WRITE && ROLE_SPECIAL)"</js>
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports any of the following expression constructs:
	 * 		<ul>
	 * 			<li><js>"foo"</js> - Single arguments.
	 * 			<li><js>"foo,bar,baz"</js> - Multiple OR'ed arguments.
	 * 			<li><js>"foo | bar | bqz"</js> - Multiple OR'ed arguments, pipe syntax.
	 * 			<li><js>"foo || bar || bqz"</js> - Multiple OR'ed arguments, Java-OR syntax.
	 * 			<li><js>"fo*"</js> - Patterns including <js>'*'</js> and <js>'?'</js>.
	 * 			<li><js>"fo* & *oo"</js> - Multiple AND'ed arguments, ampersand syntax.
	 * 			<li><js>"fo* && *oo"</js> - Multiple AND'ed arguments, Java-AND syntax.
	 * 			<li><js>"fo* || (*oo || bar)"</js> - Parenthesis.
	 * 		</ul>
	 * 	<li>
	 * 		AND operations take precedence over OR operations (as expected).
	 * 	<li>
	 * 		Whitespace is ignored.
	 * 	<li>
	 * 		<jk>null</jk> or empty expressions always match as <jk>false</jk>.
	 * 	<li>
	 * 		If patterns are used, you must specify the list of declared roles using {@link RestResource#rolesDeclared()} or {@link RestContext#REST_rolesDeclared}.
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * 	<li>
	 * 		Role guards defined at both the class and method level must both pass.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_roleGuard}
	 * </ul>
	 */
	public static final String REST_roleGuard = PREFIX + ".roleGuard.ss";

	/**
	 * Configuration property:  Serializers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.serializers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link Serializer} | Class&lt;? <jk>extends</jk> {@link Serializer}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#serializers()}
	 * 			<li class='ja'>{@link RestMethod#serializers()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#serializers(Object...)}
	 * 			<li class='jm'>{@link RestContextBuilder#serializers(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#serializersReplace(Object...)}
	 * 			<li class='jm'>{@link RestContextBuilder#serializersReplace(Class...)}
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
	 * <ul>
	 * 	<li class='jm'>{@link Serializer#getMediaTypeRanges()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation.</jc>
	 * 	<ja>@RestResource</ja>(serializers={JsonSerializer.<jk>class</jk>, XmlSerializer.<jk>class</jk>})
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
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
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
	 * 			<li class='jc'>{@link XmlSchemaDocSerializer}
	 * 			<li class='jc'>{@link UonSerializer}
	 * 			<li class='jc'>{@link UrlEncodingSerializer}
	 * 			<li class='jc'>{@link MsgPackSerializer}
	 * 			<li class='jc'>{@link SoapXmlSerializer}
	 * 			<li class='jc'>{@link PlainTextSerializer}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.Serializers}
	 * </ul>
	 * <p>
	 */
	public static final String REST_serializers = PREFIX + ".serializers.lo";

	/**
	 * Configuration property:  Static file response headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFileResponseHeaders.omo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>Default:</b>  <code>{<js>'Cache-Control'</js>: <js>'max-age=86400, public</js>}</code>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#staticFileResponseHeaders()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#staticFileResponseHeaders(Map)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFileResponseHeadersReplace(Map)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(
	 * 		staticFileResponseHeaders={
	 * 			<js>"Cache-Control: $C{REST/cacheControl,nocache}"</js>,
	 * 			<js>"My-Header: $C{REST/myHeaderValue}"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder
	 * 				.staticFileResponseHeader(<js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 				.staticFileResponseHeaders(<js>"My-Header: foo"</js>);
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder
	 * 				.addTo(<jsf>REST_staticFileResponseHeaders</jsf>, <js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 				.addTo(<jsf>REST_staticFileResponseHeaders</jsf>, <js>"My-Header"</js>, <js>"foo"</js>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.staticFileResponseHeader(<js>"Cache-Control"</js>, <js>"nocache"</js>);
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_staticFiles} for information about statically-served files.
	 * </ul>
	 */
	public static final String REST_staticFileResponseHeaders = PREFIX + ".staticFileResponseHeaders.omo";

	/**
	 * Configuration property:  Static file mappings.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFiles.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;StaticFileMapping&gt;</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#staticFiles()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#staticFiles(String)},
	 * 			<li class='jm'>{@link RestContextBuilder#staticFiles(Class,String)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFiles(String,String)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFiles(Class,String,String)}
	 * 			<li class='jm'>{@link RestContextBuilder#staticFiles(StaticFileMapping...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents
	 * from the classpath or file system.
	 *
	 * <p>
	 * An example where this class is used is in the {@link RestResource#staticFiles} annotation:
	 * <p class='bcode w800'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 *
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles={
	 * 			<js>"htdocs:docs"</js>,
	 * 			<js>"styles:styles"</js>
	 * 		}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> BasicRestServlet {...}
	 * </p>
	 *
	 * <p>
	 * In the example above, given a GET request to the following URL...
	 * <p class='bcode w800'>
	 *  	/myresource/htdocs/foobar.html
	 * </p>
	 * <br>...the servlet will attempt to find the <c>foobar.html</c> file in the following ordered locations:
	 * <ol class='spaced-list'>
	 * 	<li><c>com.foo.mypackage.docs</c> package.
	 * 	<li><c>[working-dir]/docs</c> directory.
	 * </ol>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder} for configuring how classpath resources are located and retrieved.
	 * 	<li class='jf'>{@link #REST_mimeTypes} for configuring the media types based on file extension.
	 * 	<li class='jf'>{@link #REST_staticFileResponseHeaders} for configuring response headers on statically served files.
	 * 	<li class='jf'>{@link #REST_useClasspathResourceCaching} for configuring static file caching.
	 * 	<li class='jm'>{@link RestContext#getClasspathResource(String,Locale)} for retrieving static files.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Mappings are cumulative from super classes.
	 * 	<li>
	 * 		Child resources can override mappings made on parent class resources.
	 * </ul>
	 */
	public static final String REST_staticFiles = PREFIX + ".staticFiles.lo";

	/**
	 * Configuration property:  Supported accept media types.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.produces.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#produces()}
	 * 			<li class='ja'>{@link RestMethod#produces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#produces(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#produces(MediaType...)}
	 * 			<li class='jm'>{@link RestContextBuilder#producesReplace(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#producesReplace(MediaType...)}
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
	 * 	<ja>@RestResource</ja>(produces={<js>"$C{REST/supportedProduces,application/json}"</js>})
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
	 * <ul>
	 * 	<li class='jm'>{@link RestContext#getProduces() RestContext.getProduces()}
	 * 	<li class='jm'>{@link RestRequest#getProduces()}
	 * 	<li class='jm'>{@link RestInfoProvider#getSwagger(RestRequest)} - Affects produces field.
	 * </ul>
	 */
	public static final String REST_produces = PREFIX + ".produces.ls";

	/**
	 * Configuration property:  Properties.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.properties.sms"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,String&gt;</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#properties()}
	 * 			<li class='ja'>{@link RestResource#flags()}
	 * 			<li class='ja'>{@link RestMethod#properties()}
	 * 			<li class='ja'>{@link RestMethod#flags()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#property(String,Object)}
	 * 			<li class='jm'>{@link RestContextBuilder#properties(Map)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Shortcut to add properties to the bean contexts of all serializers and parsers on all methods in the class.
	 *
	 * <p>
	 * Any of the properties defined on {@link RestContext} or any of the serializers and parsers can be specified.
	 *
	 * <p>
	 * Property values will be converted to the appropriate type.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Supports {@doc DefaultRestSvlVariables}
	 * 		(e.g. <js>"$L{my.localized.variable}"</js>).
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link RestContextBuilder#set(String,Object)}
	 * 	<li class='jm'>{@link RestContextBuilder#set(java.util.Map)}
	 * </ul>
	 */
	public static final String REST_properties = PREFIX + ".properties.sms";

	/**
	 * Configuration property:  Supported content media types.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.consumes.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;String&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#consumes()}
	 * 			<li class='ja'>{@link RestMethod#consumes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#consumes(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#consumes(MediaType...)}
	 * 			<li class='jm'>{@link RestContextBuilder#consumesReplace(String...)}
	 * 			<li class='jm'>{@link RestContextBuilder#consumesReplace(MediaType...)}
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
	 * 	<ja>@RestResource</ja>(consumes={<js>"$C{REST/supportedConsumes,application/json}"</js>})
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
	 * <ul>
	 * 	<li class='jm'>{@link RestContext#getConsumes() RestContext.getConsumes()}
	 * 	<li class='jm'>{@link RestRequest#getConsumes()}
	 * 	<li class='jm'>{@link RestInfoProvider#getSwagger(RestRequest)} - Affects consumes field.
	 * </ul>
	 */
	public static final String REST_consumes = PREFIX + ".consumes.ls";

	/**
	 * Configuration property:  Use classpath resource caching.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.useClasspathResourceCaching.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#useClasspathResourceCaching()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#useClasspathResourceCaching(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, resources retrieved via {@link RestContext#getClasspathResource(String, Locale)} (and related
	 * methods) will be cached in memory to speed subsequent lookups.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(useClasspathResourceCaching=<js>"$C{REST/useClasspathResourceCaching,false}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.useClasspathResourceCaching(<jk>false</jk>)
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_useClasspathResourceCaching</jsf>, <jk>false</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.useClasspathResourceCaching(<jk>false</jk>)
	 * 		}
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_staticFiles} for information about static files.
	 * </ul>
	 */
	public static final String REST_useClasspathResourceCaching = PREFIX + ".useClasspathResourceCaching.b";

	/**
	 * Configuration property:  Use stack trace hashes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.useStackTraceHashes.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#useStackTraceHashes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#useStackTraceHashes(boolean)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, the number of times an exception has occurred will be tracked based on stack trace hashsums.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul>
	 * 	<li class='jm'>{@link RestContext#getStackTraceOccurrence(Throwable) RestContext.getStackTraceOccurrance(Throwable)}
	 * 	<li class='jm'>{@link RestCallHandler#handleError(HttpServletRequest, HttpServletResponse, Throwable)}
	 * 	<li class='jm'>{@link RestException#getOccurrence()} - Returns the number of times this exception occurred.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(useStackTraceHashes=<js>"$C{REST/useStackTraceHashes,false}"</js>)
	 * 	<jk>public class</jk> MyResource {
	 *
	 * 		<jc>// Option #2 - Defined via builder passed in through resource constructor.</jc>
	 * 		<jk>public</jk> MyResource(RestContextBuilder builder) <jk>throws</jk> Exception {
	 *
	 * 			<jc>// Using method on builder.</jc>
	 * 			builder.useStackTraceHashes(<jk>false</jk>)
	 *
	 * 			<jc>// Same, but using property.</jc>
	 * 			builder.set(<jsf>REST_useStackTraceHashes</jsf>, <jk>false</jk>);
	 * 		}
	 *
	 * 		<jc>// Option #3 - Defined via builder passed in through init method.</jc>
	 * 		<ja>@RestHook</ja>(<jsf>INIT</jsf>)
	 * 		<jk>public void</jk> init(RestContextBuilder builder) <jk>throws</jk> Exception {
	 * 			builder.useStackTraceHashes(<jk>false</jk>)
	 * 		}
	 * 	}
	 * </p>
	 *
	 * @deprecated Use {@link Logging#useStackTraceHashing}
	 */
	@Deprecated
	public static final String REST_useStackTraceHashes = PREFIX + ".useStackTraceHashes.b";

	/**
	 * Configuration property:  Resource URI authority path.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.uriAuthority.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#uriAuthority()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#uriAuthority(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the authority path value for this resource and any child resources.
	 *
	 * <p>
	 * Affects the following methods:
	 * <ul>
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
	 * 	<ja>@RestResource</ja>(
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.uriContext.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#uriContext()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#uriContext(String)}
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
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getContextPath()} - Returns the overridden context path for the resource.
	 * 	<li class='jm'>{@link RestRequest#getServletPath()} - Includes the overridden context path for the resource.
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.uriRelativity.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"RESOURCE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#uriRelativity()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#uriRelativity(String)}
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
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(
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
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.uriResolution.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"ROOT_RELATIVE"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link RestResource#uriResolution()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#uriResolution(String)}
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
	 * <ul>
	 * 	<li class='jm'>{@link RestRequest#getUriResolver()}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Option #1 - Defined via annotation resolving to a config file setting with default value.</jc>
	 * 	<ja>@RestResource</ja>(
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

	/**
	 * Configuration property:  HTML Widgets.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.widgets.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;{@link Widget} | Class&lt;? <jk>extends</jk> {@link Widget}&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link HtmlDoc#widgets()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RestContextBuilder#widgets(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#widgets(Widget...)}
	 * 			<li class='jm'>{@link RestContextBuilder#widgetsReplace(Class...)}
	 * 			<li class='jm'>{@link RestContextBuilder#widgetsReplace(Widget...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 *
	 * Widgets resolve the following variables:
	 * <ul class='spaced-list'>
	 * 	<li><js>"$W{name}"</js> - Contents returned by {@link Widget#getHtml(RestRequest,RestResponse)}.
	 * 	<li><js>"$W{name.script}"</js> - Contents returned by {@link Widget#getScript(RestRequest,RestResponse)}.
	 * 		<br>The script contents are automatically inserted into the <xt>&lt;head/script&gt;</xt> section
	 * 			 in the HTML page.
	 * 	<li><js>"$W{name.style}"</js> - Contents returned by {@link Widget#getStyle(RestRequest,RestResponse)}.
	 * 		<br>The styles contents are automatically inserted into the <xt>&lt;head/style&gt;</xt> section
	 * 			 in the HTML page.
	 * </ul>
	 *
	 * <p>
	 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
	 * and aside section of the page:
	 *
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(
	 * 		widgets={
	 * 			MyWidget.<jk>class</jk>
	 * 		}
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			navlinks={
	 * 				<js>"$W{MyWidget}"</js>
	 * 			},
	 * 			aside={
	 * 				<js>"Check out this widget:  $W{MyWidget}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Widgets are inherited from super classes, but can be overridden by reusing the widget name.
	 * </ul>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='link'>{@doc juneau-rest-server.HtmlDocAnnotation.Widgets}
	 * </ul>
	 *
	 * @deprecated Use {@link HtmlDocSerializer#HTMLDOC_widgets}
	 */
	@Deprecated
	public static final String REST_widgets = PREFIX + ".widgets.lo";


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Object resource;
	final RestContextBuilder builder;
	private final boolean
		allowBodyParam,
		renderResponseStackTraces,
		useClasspathResourceCaching;
	private final Enablement debug;
	@Deprecated private final boolean
		useStackTraceHashes;
	private final String
		clientVersionHeader,
		uriAuthority,
		uriContext;
	final String fullPath;
	final UrlPathPattern pathPattern;

	private final Set<String> allowedMethodParams, allowedHeaderParams, allowedMethodHeaders;

	private final RestContextProperties properties;
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
		defaultRequestHeaders,
		defaultResponseHeaders,
		staticFileResponseHeaders;
	private final ObjectMap defaultRequestAttributes;
	private final ResponseHandler[] responseHandlers;
	private final MimetypesFileTypeMap mimetypesFileTypeMap;
	private final StaticFileMapping[] staticFiles;
	private final String[] staticFilesPaths;
	private final MessageBundle msgs;
	private final Config config;
	private final VarResolver varResolver;
	private final Map<String,RestCallRouter> callRouters;
	private final Map<String,RestMethodContext> callMethods;
	private final Map<String,RestContext> childResources;
	@SuppressWarnings("deprecation") private final RestLogger logger;
	private final RestCallLogger callLogger;
	private final RestCallLoggerConfig callLoggerConfig;
	private final RestCallHandler callHandler;
	private final RestInfoProvider infoProvider;
	private final RestException initException;
	private final RestContext parentContext;
	private final RestResourceResolver resourceResolver;
	private final UriResolution uriResolution;
	private final UriRelativity uriRelativity;

	// Lifecycle methods
	private final Method[]
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

	// In-memory cache of images and stylesheets in the org.apache.juneau.rest.htdocs package.
	private final Map<String,StaticFile> staticFilesCache = new ConcurrentHashMap<>();

	private final ClasspathResourceManager staticResourceManager;
	@Deprecated private final ConcurrentHashMap<Integer,AtomicInteger> stackTraceHashes = new ConcurrentHashMap<>();

	private final ThreadLocal<RestRequest> req = new ThreadLocal<>();
	private final ThreadLocal<RestResponse> res = new ThreadLocal<>();

	/**
	 * Constructor.
	 *
	 * @param resource The resource annotated with <ja>@RestResource</ja>.
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
	 * @param resourceClass The class annotated with <ja>@RestResource</ja>.
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
	@SuppressWarnings("deprecation")
	RestContext(RestContextBuilder builder) throws Exception {
		super(builder.getPropertyStore());

		RestException _initException = null;

		try {
			ServletContext servletContext = builder.servletContext;

			this.resource = builder.resource;
			this.builder = builder;
			this.parentContext = builder.parentContext;
			resourceResolver = getInstanceProperty(REST_resourceResolver, resource, RestResourceResolver.class, parentContext == null ? BasicRestResourceResolver.class : parentContext.resourceResolver, ResourceResolver.FUZZY, this);

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
					WidgetVar.class
				)
				.build()
			;

			VarResolverSession vrs = this.varResolver.createSession();
			config = builder.config.resolving(vrs);

			Class<?> resourceClass = resource.getClass();
			ClassInfo rci = getClassInfo(resourceClass);
			PropertyStore ps = getPropertyStore();

			uriContext = nullIfEmpty(getStringProperty(REST_uriContext, null));
			uriAuthority = nullIfEmpty(getStringProperty(REST_uriAuthority, null));
			uriResolution = getProperty(REST_uriResolution, UriResolution.class, UriResolution.ROOT_RELATIVE);
			uriRelativity = getProperty(REST_uriRelativity, UriRelativity.class, UriRelativity.RESOURCE);

			allowBodyParam = getBooleanProperty(REST_allowBodyParam, true);
			allowedHeaderParams = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedHeaderParams, "Accept,Content-Type"));
			allowedMethodParams = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedMethodParams, "HEAD,OPTIONS"));
			allowedMethodHeaders = newUnmodifiableSortedCaseInsensitiveSet(getStringPropertyWithNone(REST_allowedMethodHeaders, ""));
			renderResponseStackTraces = getBooleanProperty(REST_renderResponseStackTraces, false);
			useStackTraceHashes = getBooleanProperty(REST_useStackTraceHashes, true);
			debug = getInstanceProperty(REST_debug, Enablement.class, Enablement.FALSE);
			clientVersionHeader = getStringProperty(REST_clientVersionHeader, "X-Client-Version");

			responseHandlers = getInstanceArrayProperty(REST_responseHandlers, resource, ResponseHandler.class, new ResponseHandler[0], resourceResolver, this);

			Map<Class<?>,RestMethodParam> _paramResolvers = new HashMap<>();
			for (RestMethodParam rp : getInstanceArrayProperty(REST_paramResolvers, RestMethodParam.class, new RestMethodParam[0], resourceResolver, this))
				_paramResolvers.put(rp.forClass(), rp);
			paramResolvers = unmodifiableMap(_paramResolvers);

			Map<String,Object> _defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			_defaultRequestHeaders.putAll(getMapProperty(REST_defaultRequestHeaders, String.class));
			defaultRequestHeaders = unmodifiableMap(new LinkedHashMap<>(_defaultRequestHeaders));

			defaultRequestAttributes = new ObjectMap(getMapProperty(REST_attrs, Object.class)).unmodifiable();
			defaultResponseHeaders = getMapProperty(REST_defaultResponseHeaders, Object.class);
			staticFileResponseHeaders = getMapProperty(REST_staticFileResponseHeaders, Object.class);

			logger = getInstanceProperty(REST_logger, resource, RestLogger.class, NoOpRestLogger.class, resourceResolver, this);
			callLogger = getInstanceProperty(REST_callLogger, resource, RestCallLogger.class, BasicRestCallLogger.class, resourceResolver, this);

			Object clc = getProperty(REST_callLoggerConfig);
			if (clc instanceof RestCallLoggerConfig)
				this.callLoggerConfig = (RestCallLoggerConfig)clc;
			else if (clc instanceof ObjectMap)
				this.callLoggerConfig = RestCallLoggerConfig.create().apply((ObjectMap)clc).build();
			else
				this.callLoggerConfig = RestCallLoggerConfig.DEFAULT;

			properties = builder.properties;
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
			partSerializer =
				(HttpPartSerializer)
				SerializerGroup
				.create()
				.append(getInstanceProperty(REST_partSerializer, HttpPartSerializer.class, OpenApiSerializer.class, resourceResolver, resource, ps))
				.build()
				.getSerializers()
				.get(0);
			partParser =
				(HttpPartParser)
				ParserGroup
				.create()
				.append(getInstanceProperty(REST_partParser, HttpPartParser.class, OpenApiParser.class, resourceResolver, resource, ps))
				.build()
				.getParsers()
				.get(0);
			jsonSchemaGenerator =
				JsonSchemaGenerator
				.create()
				.apply(ps)
				.build();

			mimetypesFileTypeMap = new ExtendedMimetypesFileTypeMap();
			for (String mimeType : getArrayProperty(REST_mimeTypes, String.class))
				mimetypesFileTypeMap.addMimeTypes(mimeType);

			ClasspathResourceFinder rf = getInstanceProperty(REST_classpathResourceFinder, ClasspathResourceFinder.class, ClasspathResourceFinderBasic.class, resourceResolver, this);
			useClasspathResourceCaching = getProperty(REST_useClasspathResourceCaching, boolean.class, true);
			staticResourceManager = new ClasspathResourceManager(resourceClass, rf, useClasspathResourceCaching);

			consumes = getListProperty(REST_consumes, MediaType.class, parsers.getSupportedMediaTypes());
			produces = getListProperty(REST_produces, MediaType.class, serializers.getSupportedMediaTypes());

			staticFiles = ArrayUtils.reverse(getArrayProperty(REST_staticFiles, StaticFileMapping.class));
			Set<String> s = new TreeSet<>();
			for (StaticFileMapping sfm : staticFiles)
				s.add(sfm.path);
			staticFilesPaths = s.toArray(new String[s.size()]);

			MessageBundleLocation[] mbl = getInstanceArrayProperty(REST_messages, MessageBundleLocation.class, new MessageBundleLocation[0]);
			if (mbl.length == 0)
				msgs = new MessageBundle(resourceClass, "");
			else {
				msgs = new MessageBundle(mbl[0] != null ? mbl[0].baseClass : resourceClass, mbl[0].bundlePath);
				for (int i = 1; i < mbl.length; i++)
					msgs.addSearchPath(mbl[i] != null ? mbl[i].baseClass : resourceClass, mbl[i].bundlePath);
			}

			this.fullPath = (builder.parentContext == null ? "" : (builder.parentContext.fullPath + '/')) + builder.getPath();

			String p = builder.getPath();
			if (! p.endsWith("/*"))
				p += "/*";
			this.pathPattern = new UrlPathPattern(p);

			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<>();   // Temporary to help debug transient duplicate method issue.
			Map<String,RestCallRouter.Builder> routers = new LinkedHashMap<>();
			Map<String,RestMethodContext> _javaRestMethods = new LinkedHashMap<>();
			Map<String,Method>
				_startCallMethods = new LinkedHashMap<>(),
				_preCallMethods = new LinkedHashMap<>(),
				_postCallMethods = new LinkedHashMap<>(),
				_endCallMethods = new LinkedHashMap<>(),
				_postInitMethods = new LinkedHashMap<>(),
				_postInitChildFirstMethods = new LinkedHashMap<>(),
				_destroyMethods = new LinkedHashMap<>();
			List<RestMethodParam[]>
				_preCallMethodParams = new ArrayList<>(),
				_postCallMethodParams = new ArrayList<>();
			List<Class<?>[]>
				_startCallMethodParams = new ArrayList<>(),
				_endCallMethodParams = new ArrayList<>(),
				_postInitMethodParams = new ArrayList<>(),
				_postInitChildFirstMethodParams = new ArrayList<>(),
				_destroyMethodParams = new ArrayList<>();

			for (MethodInfo mi : rci.getPublicMethods()) {
				RestMethod a = mi.getAnnotation(RestMethod.class);
				if (a != null) {
					methodsFound.add(mi.getSimpleName() + "," + emptyIfNull(firstNonEmpty(a.name(), a.method())) + "," + fixMethodPath(a.path()));
					try {
						if (mi.isNotPublic())
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", resourceClass.getName(), mi.getSimpleName());

						RestMethodContextBuilder rmcb = new RestMethodContextBuilder(resource, mi.inner(), this);
						RestMethodContext sm = new RestMethodContext(rmcb);
						String httpMethod = sm.getHttpMethod();

						// RRPC is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("RRPC".equals(httpMethod)) {

							final ClassMeta<?> interfaceClass = getClassMeta(mi.inner().getGenericReturnType());
							final RemoteInterfaceMeta rim = new RemoteInterfaceMeta(interfaceClass.getInnerClass(), null);
							if (rim.getMethodsByPath().isEmpty())
								throw new RestException(SC_INTERNAL_SERVER_ERROR, "Method {0} returns an interface {1} that doesn't define any remote methods.", mi.getSignature(), interfaceClass.getFullName());

							RestMethodContextBuilder smb = new RestMethodContextBuilder(resource, mi.inner(), this);
							sm = new RestMethodContext(smb) {

								@Override
								int invoke(UrlPathInfo pathInfo, RestRequest req, RestResponse res) throws Throwable {

									int rc = super.invoke(pathInfo, req, res);
									if (rc != SC_OK)
										return rc;

									final Object o = res.getOutput();

									if ("GET".equals(req.getMethod())) {
										res.setOutput(rim.getMethodsByPath().keySet());
										return SC_OK;

									} else if ("POST".equals(req.getMethod())) {
										String pip = pathInfo.getPath();
										if (pip.indexOf('/') != -1)
											pip = pip.substring(pip.lastIndexOf('/')+1);
										pip = urlDecode(pip);
										RemoteInterfaceMethod rmm = rim.getMethodMetaByPath(pip);
										if (rmm != null) {
											Method m = rmm.getJavaMethod();
											try {
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
												res.setOutput(output);
												return SC_OK;
											} catch (Exception e) {
												throw new InternalServerError(e);
											}
										}
									}
									return SC_NOT_FOUND;
								}
							};

							_javaRestMethods.put(mi.getSimpleName(), sm);
							addToRouter(routers, "GET", sm);
							addToRouter(routers, "POST", sm);

						} else {
							_javaRestMethods.put(mi.getSimpleName(), sm);
							addToRouter(routers, httpMethod, sm);
						}
					} catch (Throwable e) {
						throw new RestServletException("Problem occurred trying to serialize methods on class {0}, methods={1}", resourceClass.getName(), SimpleJsonSerializer.DEFAULT.serialize(methodsFound)).initCause(e);
					}
				}
			}

			for (MethodInfo m : rci.getAllMethodsParentFirst()) {
				if (m.isPublic() && m.hasAnnotation(RestHook.class)) {
					HookEvent he = m.getAnnotation(RestHook.class).value();
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
								_startCallMethodParams.add(m.getRawParamTypes());
								assertArgsOnlyOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case END_CALL: {
							if (! _endCallMethods.containsKey(sig)) {
								m.setAccessible();
								_endCallMethods.put(sig, m.inner());
								_endCallMethodParams.add(m.getRawParamTypes());
								assertArgsOnlyOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case POST_INIT: {
							if (! _postInitMethods.containsKey(sig)) {
								m.setAccessible();
								_postInitMethods.put(sig, m.inner());
								_postInitMethodParams.add(m.getRawParamTypes());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						case POST_INIT_CHILD_FIRST: {
							if (! _postInitChildFirstMethods.containsKey(sig)) {
								m.setAccessible();
								_postInitChildFirstMethods.put(sig, m.inner());
								_postInitChildFirstMethodParams.add(m.getRawParamTypes());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						case DESTROY: {
							if (! _destroyMethods.containsKey(sig)) {
								m.setAccessible();
								_destroyMethods.put(sig, m.inner());
								_destroyMethodParams.add(m.getRawParamTypes());
								assertArgsOnlyOfType(m, RestContext.class);
							}
							break;
						}
						default: // Ignore INIT
					}
				}
			}

			this.callMethods = unmodifiableMap(_javaRestMethods);
			this.preCallMethods = _preCallMethods.values().toArray(new Method[_preCallMethods.size()]);
			this.postCallMethods = _postCallMethods.values().toArray(new Method[_postCallMethods.size()]);
			this.startCallMethods = _startCallMethods.values().toArray(new Method[_startCallMethods.size()]);
			this.endCallMethods = _endCallMethods.values().toArray(new Method[_endCallMethods.size()]);
			this.postInitMethods = _postInitMethods.values().toArray(new Method[_postInitMethods.size()]);
			this.postInitChildFirstMethods = _postInitChildFirstMethods.values().toArray(new Method[_postInitChildFirstMethods.size()]);
			this.destroyMethods = _destroyMethods.values().toArray(new Method[_destroyMethods.size()]);
			this.preCallMethodParams = _preCallMethodParams.toArray(new RestMethodParam[_preCallMethodParams.size()][]);
			this.postCallMethodParams = _postCallMethodParams.toArray(new RestMethodParam[_postCallMethodParams.size()][]);
			this.startCallMethodParams = _startCallMethodParams.toArray(new Class[_startCallMethodParams.size()][]);
			this.endCallMethodParams = _endCallMethodParams.toArray(new Class[_endCallMethodParams.size()][]);
			this.postInitMethodParams = _postInitMethodParams.toArray(new Class[_postInitMethodParams.size()][]);
			this.postInitChildFirstMethodParams = _postInitChildFirstMethodParams.toArray(new Class[_postInitChildFirstMethodParams.size()][]);
			this.destroyMethodParams = _destroyMethodParams.toArray(new Class[_destroyMethodParams.size()][]);

			Map<String,RestCallRouter> _callRouters = new LinkedHashMap<>();
			for (RestCallRouter.Builder crb : routers.values())
				_callRouters.put(crb.getHttpMethodName(), crb.build());
			this.callRouters = unmodifiableMap(_callRouters);

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

			callHandler = getInstanceProperty(REST_callHandler, resource, RestCallHandler.class, BasicRestCallHandler.class, resourceResolver, this);
			infoProvider = getInstanceProperty(REST_infoProvider, resource, RestInfoProvider.class, BasicRestInfoProvider.class, resourceResolver, this);

		} catch (RestException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new RestException(e, SC_INTERNAL_SERVER_ERROR);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	private static void addToRouter(Map<String, RestCallRouter.Builder> routers, String httpMethodName, RestMethodContext cm) throws RestServletException {
		if (! routers.containsKey(httpMethodName))
			routers.put(httpMethodName, new RestCallRouter.Builder(httpMethodName));
		routers.get(httpMethodName).add(cm);
	}

	/**
	 * Returns the resource resolver associated with this context.
	 *
	 * <p>
	 * The resource resolver is used for instantiating child resource classes.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_resourceResolver}
	 * </ul>
	 *
	 * @return The resource resolver associated with this context.
	 */
	protected RestResourceResolver getResourceResolver() {
		return resourceResolver;
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
	 * 	<ja>@RestResource</ja>(
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
	 * 		name=<jsf>GET</jsf>, path=<js>"/{name}/*"</js>
	 * 	)
	 * 	<ja>@HtmlDocConfig</ja>(
	 * 		navlinks={
	 * 			<js>"up: $R{requestParentURI}"</js>,
	 * 			<js>"options: servlet:/?method=OPTIONS"</js>,
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContextBuilder#vars(Class...)} - For adding custom vars.
	 * 	<li class='link'>{@doc juneau-rest-server.SvlVariables}
	 * 	<li class='link'>{@doc DefaultRestSvlVariables}
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
	 * <ul>
	 * 	<li class='ja'>{@link RestResource#config()}
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
	 * Resolve a static resource file.
	 *
	 * <p>
	 * The location of static resources are defined via:
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles RestContext.REST_staticFiles}
	 * </ul>
	 *
	 * @param pathInfo The unencoded path info.
	 * @return The wrapped resource, never <jk>null</jk>.
	 * @throws NotFound Invalid path.
	 * @throws IOException Thrown by underlying stream.
	 */
	protected StaticFile resolveStaticFile(String pathInfo) throws NotFound, IOException {
		if (! staticFilesCache.containsKey(pathInfo)) {
			String p = urlDecode(trimSlashes(pathInfo));
			if (p.indexOf("..") != -1)
				throw new NotFound("Invalid path");
			StreamResource sr = null;
			for (StaticFileMapping sfm : staticFiles) {
				String path = sfm.path;
				if (p.startsWith(path)) {
					String remainder = (p.equals(path) ? "" : p.substring(path.length()));
					if (remainder.isEmpty() || remainder.startsWith("/")) {
						String p2 = sfm.location + remainder;
						try (InputStream is = getClasspathResource(sfm.resourceClass, p2, null)) {
							if (is != null) {
								int i = p2.lastIndexOf('/');
								String name = (i == -1 ? p2 : p2.substring(i+1));
								String mediaType = mimetypesFileTypeMap.getContentType(name);
								Map<String,Object> responseHeaders = sfm.responseHeaders != null ? sfm.responseHeaders : staticFileResponseHeaders;
								sr = new StreamResource(MediaType.forString(mediaType), responseHeaders, true, is);
								break;
							}
						}
					}
				}
			}
			StaticFile sf = new StaticFile(sr);
			if (useClasspathResourceCaching) {
				if (staticFilesCache.size() > 100)
					staticFilesCache.clear();
				staticFilesCache.put(pathInfo, sf);
			}
			return sf;
		}
		return staticFilesCache.get(pathInfo);
	}

	/**
	 * A cached static file instance.
	 */
	class StaticFile {
		StreamResource resource;
		ResponseBeanMeta meta;

		/**
		 * Constructor.
		 *
		 * @param resource The inner resource.
		 */
		StaticFile(StreamResource resource) {
			this.resource = resource;
			this.meta = resource == null ? null : ResponseBeanMeta.create(resource.getClass(), getPropertyStore());
		}
	}

	/**
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
	 * up the parent hierarchy chain.
	 *
	 * <p>
	 * If the resource cannot be found in the classpath, then an attempt is made to look in the JVM working directory.
	 *
	 * <p>
	 * If the <c>locale</c> is specified, then we look for resources whose name matches that locale.
	 * <br>For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
	 * files in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> Object myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResource(file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param name The resource name.
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return An input stream of the resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getClasspathResource(String name, Locale locale) throws IOException {
		return staticResourceManager.getStream(name, locale);
	}

	/**
	 * Same as {@link #getClasspathResource(String, Locale)}, but allows you to override the class used for looking
	 * up the classpath resource.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> Object myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResource(SomeOtherClass.<jk>class</jk>, file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null</jk>, uses the REST resource class.
	 * @param name The resource name.
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return An input stream of the resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public InputStream getClasspathResource(Class<?> baseClass, String name, Locale locale) throws IOException {
		return staticResourceManager.getStream(baseClass, name, locale);
	}

	/**
	 * Reads the input stream from {@link #getClasspathResource(String, Locale)} into a String.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> String myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResourceAsString(file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param name The resource name.
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The contents of the stream as a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException If resource could not be found.
	 */
	public String getClasspathResourceAsString(String name, Locale locale) throws IOException {
		return staticResourceManager.getString(name, locale);
	}

	/**
	 * Same as {@link #getClasspathResourceAsString(String, Locale)}, but allows you to override the class used for looking
	 * up the classpath resource.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> String myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResourceAsString(SomeOtherClass.<jk>class</jk>, file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null</jk>, uses the REST resource class.
	 * @param name The resource name.
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The contents of the stream as a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException If resource could not be found.
	 */
	public String getClasspathResourceAsString(Class<?> baseClass, String name, Locale locale) throws IOException {
		return staticResourceManager.getString(baseClass, name, locale);
	}

	/**
	 * Reads the input stream from {@link #getClasspathResource(String, Locale)} and parses it into a POJO using the parser
	 * matched by the specified media type.
	 *
	 * <p>
	 * Useful if you want to load predefined POJOs from JSON files in your classpath.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath parsed as an array of beans.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> MyBean[] myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResource(MyBean[].<jk>class</jk>, <jsf>JSON</jsf>, file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getClasspathResource(Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		return getClasspathResource(null, c, mediaType, name, locale);
	}

	/**
	 * Same as {@link #getClasspathResource(Class, MediaType, String, Locale)}, except overrides the class used
	 * for retrieving the classpath resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_classpathResourceFinder}
	 * </ul>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// A rest method that (unsafely!) returns the contents of a localized file </jc>
	 *	<jc>// from the classpath parsed as an array of beans.</jc>
	 * 	<ja>@RestMethod</ja>(path=<js>"/foo"</js>)
	 * 	<jk>public</jk> MyBean[] myMethod(RestRequest req, <ja>@Query</ja>(<js>"file"</js>) String file) {
	 * 		<jk>return</jk> getContext().getClasspathResource(SomeOtherClass.<jk>class</jk>, MyBean[].<jk>class</jk>, <jsf>JSON</jsf>, file, req.getLocale());
	 * 	}
	 * </p>
	 *
	 * @param baseClass
	 * 	Overrides the default class to use for retrieving the classpath resource.
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale
	 * 	Optional locale.
	 * 	<br>If <jk>null</jk>, won't look for localized file names.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getClasspathResource(Class<?> baseClass, Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		InputStream is = getClasspathResource(baseClass, name, locale);
		if (is == null)
			return null;
		try {
			Parser p = parsers.getParser(mediaType);
			if (p == null) {
				if (mediaType == MediaType.JSON)
					p = JsonParser.DEFAULT;
				if (mediaType == MediaType.XML)
					p = XmlParser.DEFAULT;
				if (mediaType == MediaType.HTML)
					p = HtmlParser.DEFAULT;
				if (mediaType == MediaType.UON)
					p = UonParser.DEFAULT;
				if (mediaType == MediaType.URLENCODING)
					p = UrlEncodingParser.DEFAULT;
				if (mediaType == MediaType.MSGPACK)
					p = MsgPackParser.DEFAULT;
			}
			if (p != null) {
				try {
					try (Closeable in = p.isReaderParser() ? new InputStreamReader(is, UTF8) : is) {
						return p.parse(in, c);
					}
				} catch (ParseException e) {
					throw new ServletException("Could not parse resource '"+name+" as media type '"+mediaType+"'.", e);
				}
			}
			throw new ServletException("Unknown media type '"+mediaType+"'");
		} catch (Exception e) {
			throw new ServletException("Could not parse resource with name '"+name+"'", e);
		}
	}

	/**
	 * Returns the path for this resource as defined by the {@link RestResource#path() @RestResource(path)} annotation or
	 * {@link RestContextBuilder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>""</js>.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_path}
	 * </ul>
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return fullPath;
	}

	/**
	 * Returns the logger to use for this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_logger}
	 * </ul>
	 *
	 * @return
	 * 	The logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 * @deprecated Use {@link #getCallLogger()}
	 */
	@Deprecated
	public RestLogger getLogger() {
		return logger;
	}

	/**
	 * Returns the call logger to use for this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_callLogger}
	 * </ul>
	 *
	 * @return
	 * 	The call logger to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestCallLogger getCallLogger() {
		return callLogger;
	}

	/**
	 * Returns the call logger config to use for this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_callLoggerConfig}
	 * </ul>
	 *
	 * @return
	 * 	The call logger config to use for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestCallLoggerConfig getCallLoggerConfig() {
		return callLoggerConfig;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link #REST_messages}
	 * </ul>
	 *
	 * @return
	 * 	The resource bundle for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public MessageBundle getMessages() {
		return msgs;
	}

	/**
	 * Returns the REST information provider used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * Returns the REST call handler used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_callHandler}
	 * </ul>
	 *
	 * @return
	 * 	The call handler for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public RestCallHandler getCallHandler() {
		return callHandler;
	}

	/**
	 * Returns a map of HTTP method names to call routers.
	 *
	 * @return A map with HTTP method names upper-cased as the keys, and call routers as the values.
	 */
	protected Map<String,RestCallRouter> getCallRouters() {
		return callRouters;
	}

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link RestResource @RestResource} annotation, usually
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
	 * Throws a {@link RestException} if an exception occurred in the constructor of this object.
	 *
	 * @throws RestException The initialization exception wrapped in a {@link RestException}.
	 */
	protected void checkForInitException() throws RestException {
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
	 * Returns the class-level properties associated with this servlet.
	 *
	 * <p>
	 * Properties at the class level are defined via the following:
	 * <ul>
	 * 	<li class='ja'>{@link RestResource#properties()}
	 * 	<li class='jm'>{@link RestContextBuilder#set(String, Object)}
	 * 	<li class='jm'>{@link RestContextBuilder#set(Map)}
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The returned {@code Map} is mutable.
	 * 		<br>Therefore, subclasses are free to override or set additional initialization parameters in their {@code init()} method.
	 * </ul>
	 *
	 * @return The resource properties as a {@link RestContextProperties}.
	 */
	public RestContextProperties getProperties() {
		return properties;
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
	 * 	Keys are the {@link RestResource#path() @RestResource(path)} annotation defined on the child resource.
	 */
	public Map<String,RestContext> getChildResources() {
		return Collections.unmodifiableMap(childResources);
	}

	/**
	 * Returns the number of times this exception was thrown based on a hash of its stacktrace.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_useStackTraceHashes}
	 * </ul>
	 *
	 * @param e The exception to check.
	 * @return
	 * 	The number of times this exception was thrown, or <c>0</c> if {@link #REST_useStackTraceHashes}
	 * 	setting is not enabled.
	 * @deprecated Not used by new logging API.
	 */
	@Deprecated
	public int getStackTraceOccurrence(Throwable e) {
		if (! useStackTraceHashes)
			return 0;
		int h = e.hashCode();
		stackTraceHashes.putIfAbsent(h, new AtomicInteger());
		return stackTraceHashes.get(h).incrementAndGet();
	}

	/**
	 * Returns whether it's safe to render stack traces in HTTP responses.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_useStackTraceHashes}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 */
	public boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns whether it's safe to pass the HTTP body as a <js>"body"</js> GET parameter.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_allowBodyParam}
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * Returns <jk>true</jk> if debug mode is enabled on this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_debug}
	 * </ul>
	 *
	 * @return <jk>true</jk> if setting is enabled.
	 * @deprecated Use {@link #getDebug()}.
	 */
	@Deprecated
	@Override
	public boolean isDebug() {
		return debug == Enablement.TRUE;
	}

	/**
	 * Returns the debug setting on this context.
	 *
	 * @return The debug setting on this context.
	 */
	public Enablement getDebug() {
		return debug;
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_defaultRequestHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The default request headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public ObjectMap getDefaultRequestAttributes() {
		return defaultRequestAttributes;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_defaultResponseHeaders}
	 * </ul>
	 *
	 * @return
	 * 	The default response headers for this resource.
	 * 	<br>Never <jk>null</jk>.
	 */
	public Map<String,Object> getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the response handlers associated with this resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * Returns <jk>true</jk> if this resource has any child resources associated with it.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 *
	 * @return <jk>true</jk> if this resource has any child resources associated with it.
	 */
	public boolean hasChildResources() {
		return ! childResources.isEmpty();
	}

	/**
	 * Returns the context of the child resource associated with the specified path.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_children}
	 * </ul>
	 *
	 * @param path The path of the child resource to resolve.
	 * @return The resolved context, or <jk>null</jk> if it could not be resolved.
	 */
	public RestContext getChildResource(String path) {
		return childResources.get(path);
	}

	/**
	 * Returns the authority path of the resource.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
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
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_uriRelativity}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution relativity setting value.
	 * 	<br>Never <jk>null<jk>.
	 */
	public UriRelativity getUriRelativity() {
		return uriRelativity;
	}

	/**
	 * Returns the setting on how relative URIs should be resolved.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_uriResolution}
	 * </ul>
	 *
	 * @return
	 * 	The URI-resolution setting value.
	 * 	<br>Never <jk>null<jk>.
	 */
	public UriResolution getUriResolution() {
		return uriResolution;
	}

	/**
	 * Returns the parameters defined on the specified Java method.
	 *
	 * @param method The Java method to check.
	 * @return The parameters defined on the Java method.
	 */
	public RestMethodParam[] getRestMethodParams(Method method) {
		return callMethods.get(method.getName()).methodParams;
	}

	/**
	 * Returns the media type for the specified file name.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_mimeTypes}
	 * </ul>
	 *
	 * @param name The file name.
	 * @return The MIME-type, or <jk>null</jk> if it could not be determined.
	 */
	public String getMediaTypeForName(String name) {
		return mimetypesFileTypeMap.getContentType(name);
	}

	/**
	 * Returns <jk>true</jk> if the specified path refers to a static file.
	 *
	 * <p>
	 * Static files are files pulled from the classpath and served up directly to the browser.
	 *
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link RestContext#REST_staticFiles}
	 * </ul>
	 *
	 * @param p The URL path remainder after the servlet match.
	 * @return <jk>true</jk> if the specified path refers to a static file.
	 */
	public boolean isStaticFile(String p) {
		return pathStartsWith(p, staticFilesPaths);
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
	public Map<String,RestMethodContext> getCallMethods() {
		return callMethods;
	}

	/**
	 * Finds the {@link RestMethodParam} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param mi The Java method being called.
	 * @param isPreOrPost Whether this is a {@link HookEvent#PRE_CALL} or {@link HookEvent#POST_CALL}.
	 * @param pathPattern The path pattern to match against.
	 * @return The array of resolvers.
	 * @throws ServletException If an annotation usage error was detected.
	 */
	protected RestMethodParam[] findParams(MethodInfo mi, boolean isPreOrPost, UrlPathPattern pathPattern) throws ServletException {

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
				rp[i] = new RestParamDefaults.PathObject(mpi, ps, pathPattern);
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
				rp[i] = new RestParamDefaults.MethodObject(mi, t);
			}

			if (rp[i] == null && ! isPreOrPost)
				throw new RestServletException("Invalid parameter specified for method ''{0}'' at index position {1}", mi.inner(), i);
		}

		return rp;
	}

	/*
	 * Calls all @RestHook(PRE) methods.
	 */
	void preCall(RestRequest req, RestResponse res) throws RestException {
		for (int i = 0; i < preCallMethods.length; i++)
			preOrPost(resource, preCallMethods[i], preCallMethodParams[i], req, res);
	}

	/*
	 * Calls all @RestHook(POST) methods.
	 */
	void postCall(RestRequest req, RestResponse res) throws RestException {
		for (int i = 0; i < postCallMethods.length; i++)
			preOrPost(resource, postCallMethods[i], postCallMethodParams[i], req, res);
	}

	private static void preOrPost(Object resource, Method m, RestMethodParam[] mp, RestRequest req, RestResponse res) throws RestException {
		if (m != null) {
			Object[] args = new Object[mp.length];
			for (int i = 0; i < mp.length; i++) {
				try {
					args[i] = mp[i].resolve(req, res);
				} catch (RestException e) {
					throw e;
				} catch (Exception e) {
					throw new BadRequest(e,
						"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
						mp[i].getParamType().name(), mp[i].getName(), mp[i].getType(), m.getDeclaringClass().getName(), m.getName()
					);
				}
			}
			try {
				m.invoke(resource, args);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}
	}

	/*
	 * Calls all @RestHook(START) methods.
	 */
	void startCall(HttpServletRequest req, HttpServletResponse res) {
		for (int i = 0; i < startCallMethods.length; i++)
			startOrFinish(resource, startCallMethods[i], startCallMethodParams[i], req, res);
	}

	/*
	 * Calls all @RestHook(FINISH) methods.
	 */
	void finishCall(HttpServletRequest req, HttpServletResponse res) {
		for (int i = 0; i < endCallMethods.length; i++)
			startOrFinish(resource, endCallMethods[i], endCallMethodParams[i], req, res);
	}

	private static void startOrFinish(Object resource, Method m, Class<?>[] p, HttpServletRequest req, HttpServletResponse res) throws RestException, InternalServerError {
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
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}
	}

	/**
	 * Calls all @RestHook(POST_INIT) methods in parent-to-child order.
	 *
	 * @return This object (for method chaining).
	 * @throws ServletException Error occurred.
	 */
	public RestContext postInit() throws ServletException {
		for (int i = 0; i < postInitMethods.length; i++)
			postInitOrDestroy(resource, postInitMethods[i], postInitMethodParams[i]);
		for (RestContext childContext : this.childResources.values())
			childContext.postInit();
		return this;
	}

	/**
	 * Calls all @RestHook(POST_INIT_CHILD_FIRST) methods in child-to-parent order.
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

	private void postInitOrDestroy(Object r, Method m, Class<?>[] p) {
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
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new InternalServerError(e);
			}
		}
	}

	/**
	 * Calls {@link Servlet#destroy()} on any child resources defined on this resource.
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
		return req.get();
	}

	void setRequest(RestRequest req) {
		// Must be careful not to bleed thread-locals.
		if (this.req.get() != null)
			System.err.println("WARNING:  Thread-local request object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");
		this.req.set(req);
	}

	/**
	 * Returns the HTTP response object for the current request.
	 *
	 * @return The HTTP response object, or <jk>null</jk> if it hasn't been created.
	 */
	public RestResponse getResponse() {
		return res.get();
	}

	void setResponse(RestResponse res) {
		// Must be careful not to bleed thread-locals.
		if (this.res.get() != null)
			System.err.println("WARNING:  Thread-local response object was not cleaned up from previous request.  " + this + ", thread=["+Thread.currentThread().getId()+"]");
		this.res.set(res);
	}

	/**
	 * Clear any request state information on this context.
	 * This should always be called in a finally block in the RestServlet.
	 */
	void clearState() {
		req.remove();
		res.remove();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public ObjectMap toMap() {
		return super.toMap()
			.append("RestContext", new DefaultFilteringObjectMap()
				.append("allowBodyParam", allowBodyParam)
				.append("allowedMethodHeader", allowedMethodHeaders)
				.append("allowedMethodParams", allowedMethodParams)
				.append("allowedHeaderParams", allowedHeaderParams)
				.append("callHandler", callHandler)
				.append("clientVersionHeader", clientVersionHeader)
				.append("consumes", consumes)
				.append("defaultRequestHeaders", defaultRequestHeaders)
				.append("defaultResponseHeaders", defaultResponseHeaders)
				.append("infoProvider", infoProvider)
				.append("logger", logger)
				.append("paramResolvers", paramResolvers)
				.append("parsers", parsers)
				.append("partParser", partParser)
				.append("partSerializer", partSerializer)
				.append("produces", produces)
				.append("properties", properties)
				.append("renderResponseStackTraces", renderResponseStackTraces)
				.append("resourceResolver", resourceResolver)
				.append("responseHandlers", responseHandlers)
				.append("serializers", serializers)
				.append("staticFileResponseHeaders", staticFileResponseHeaders)
				.append("staticFiles", staticFiles)
				.append("uriAuthority", uriAuthority)
				.append("uriContext", uriContext)
				.append("uriRelativity", uriRelativity)
				.append("uriResolution", uriResolution)
				.append("useClasspathResourceCaching", useClasspathResourceCaching)
			);
	}
}
