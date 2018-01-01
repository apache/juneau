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
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.ReflectionUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.activation.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.rest.response.*;
import org.apache.juneau.rest.vars.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

/**
 * Contains all the configuration on a REST resource and the entry points for handling REST calls.
 *
 * <p>
 * See {@link PropertyStore} for more information about context properties.
 */
public final class RestContext extends BeanContext {
	
	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RestContext.";
	
	/**
	 * <b>Configuration property:</b>  Allow header URL parameters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.allowHeaderParams.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_allowHeaderParams}
	 * 	<li>Annotation:  {@link RestResource#allowHeaderParams()}
	 * 	<li>Method: {@link RestContextBuilder#allowHeaderParams(boolean)}
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging REST interface using only a browser.
	 *	</ul>
	 */
	public static final String REST_allowHeaderParams = PREFIX + "allowHeaderParams.b";
	
	/**
	 * <b>Configuration property:</b>  Allow body URL parameter.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.allowBodyParam.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_allowBodyParam}
	 * 	<li>Annotation:  {@link RestResource#allowBodyParam()}
	 * 	<li>Method: {@link RestContextBuilder#allowBodyParam(boolean)}
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging PUT and POST methods using only a browser.
	 *	</ul>
	 */
	public static final String REST_allowBodyParam = PREFIX + "allowBodyParam.b";
	
	/**
	 * <b>Configuration property:</b>  Allowed method parameters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.allowedMethodParams.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"HEAD,OPTIONS"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_allowedMethodParams}
	 * 	<li>Annotation:  {@link RestResource#allowedMethodParams()}
	 * 	<li>Method: {@link RestContextBuilder#allowedMethodParams(String...)}
	 * 	<li>Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Use "*" to represent all methods.
	 *	</ul>
	 *
	 * <p>
	 * Note that per the <a class="doclink"
	 * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 */
	public static final String REST_allowedMethodParams = PREFIX + "allowedMethodParams.s";

	/**
	 * <b>Configuration property:</b>  Render response stack traces in responses.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.renderResponseStackTraces.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_renderResponseStackTraces}
	 * 	<li>Annotation:  {@link RestResource#renderResponseStackTraces()}
	 * 	<li>Method: {@link RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 	<li>Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 *	</ul>
	 */
	public static final String REST_renderResponseStackTraces = PREFIX + "renderResponseStackTraces.b";
	
	/**
	 * <b>Configuration property:</b>  Use stack trace hashes.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.useStackTraceHashes.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_useStackTraceHashes}
	 * 	<li>Annotation:  {@link RestResource#useStackTraceHashes()}
	 * 	<li>Method: {@link RestContextBuilder#useStackTraceHashes(boolean)}
	 *	</ul>
	 */
	public static final String REST_useStackTraceHashes = PREFIX + "useStackTraceHashes.b";
	
	/**
	 * <b>Configuration property:</b>  Default character encoding.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.defaultCharset.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"utf-8"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_defaultCharset}
	 * 	<li>Annotation:  {@link RestResource#defaultCharset()} / {@link RestMethod#defaultCharset()}
	 * 	<li>Method: {@link RestContextBuilder#defaultCharset(String)}
	 *	</ul>
	 */
	public static final String REST_defaultCharset = PREFIX + "defaultCharset.s";
	
	/**
	 * <b>Configuration property:</b>  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.maxInput.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"100M"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_maxInput}
	 * 	<li>Annotation:  {@link RestResource#maxInput()} / {@link RestMethod#maxInput()}
	 * 	<li>Method: {@link RestContextBuilder#maxInput(String)}
	 * 	<li>String value that gets resolved to a <jk>long</jk>.
	 * 	<li>Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:  
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>A value of <js>"-1"</js> can be used to represent no limit.
	 *	</ul>
	 */
	public static final String REST_maxInput = PREFIX + "maxInput.s";
	
	/**
	 * <b>Configuration property:</b>  Java method parameter resolvers.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.paramResolvers.lo"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;RestParam | Class&lt;? <jk>extends</jk> RestParam&gt;&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * By default, the Juneau framework will automatically Java method parameters of various types (e.g.
	 * <code>RestRequest</code>, <code>Accept</code>, <code>Reader</code>).
	 * This annotation allows you to provide your own resolvers for your own class types that you want resolved.
	 *
	 * <p>
	 * For example, if you want to pass in instances of <code>MySpecialObject</code> to your Java method, define
	 * the following resolver:
	 * <p class='bcode'>
	 * 	<jk>public class</jk> MyRestParam <jk>extends</jk> RestParam {
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
	 * </p>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_paramResolvers}
	 * 	<li>Annotation:  {@link RestResource#paramResolvers()}
	 * 	<li>Method: {@link RestContextBuilder#paramResolvers(Class...)} / {@link RestContextBuilder#paramResolvers(RestParam...)}
	 * 	<li>{@link RestParam} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_paramResolvers = PREFIX + "paramResolvers.lo";

	/**
	 * <b>Configuration property:</b>  Class-level response converters.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.converters.lo"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;RestConverter | Class&lt;? <jk>extends</jk> RestConverter&gt;&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Associates one or more {@link RestConverter converters} with a resource class.
	 * These converters get called immediately after execution of the REST method in the same order specified in the
	 * annotation.
	 *
	 * <p>
	 * Can be used for performing post-processing on the response object before serialization.
	 *
	 * <p>
	 * Default converter implementations are provided in the <a class='doclink'
	 * href='../converters/package-summary.html#TOC'>org.apache.juneau.rest.converters</a> package.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_converters}
	 * 	<li>Annotation:  {@link RestResource#converters()} / {@link RestMethod#converters()}
	 * 	<li>Method: {@link RestContextBuilder#converters(Class...)} / {@link RestContextBuilder#converters(RestConverter...)}
	 * 	<li>{@link RestConverter} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_converters = PREFIX + "converters.lo";

	/**
	 * <b>Configuration property:</b>  Class-level guards.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.guards.lo"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;RestGuard | Class&lt;? <jk>extends</jk> RestGuard&gt;&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 *
	 * <p>
	 * Associates one or more {@link RestGuard RestGuards} with all REST methods defined in this class.
	 * These guards get called immediately before execution of any REST method in this class.
	 *
	 * <p>
	 * Typically, guards will be used for permissions checking on the user making the request, but it can also be used
	 * for other purposes like pre-call validation of a request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_guards}
	 * 	<li>Annotation:  {@link RestResource#guards()} / {@link RestMethod#guards()}
	 * 	<li>Method: {@link RestContextBuilder#guards(Class...)} / {@link RestContextBuilder#guards(RestGuard...)}
	 * 	<li>{@link RestGuard} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_guards = PREFIX + "guards.lo";

	/**
	 * <b>Configuration property:</b>  Response handlers.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.responseHandlers.lo"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;Class&lt;? <jk>extends</jk> ResponseHandler&gt;&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Specifies a list of {@link ResponseHandler} classes that know how to convert POJOs returned by REST methods or
	 * set via {@link RestResponse#setOutput(Object)} into appropriate HTTP responses.
	 *
	 * <p>
	 * By default, the following response handlers are provided out-of-the-box:
	 * <ul>
	 * 	<li>{@link StreamableHandler}
	 * 	<li>{@link WritableHandler}
	 * 	<li>{@link ReaderHandler}
	 * 	<li>{@link InputStreamHandler}
	 * 	<li>{@link RedirectHandler}
	 * 	<li>{@link DefaultHandler}
	 * </ul>

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_responseHandlers}
	 * 	<li>Annotation:  {@link RestResource#responseHandlers()} 
	 * 	<li>Method: {@link RestContextBuilder#responseHandlers(Class...)} / {@link RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 	<li>{@link ResponseHandler} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_responseHandlers = PREFIX + "responseHandlers.lo";

	/**
	 * <b>Configuration property:</b>  Default request headers.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.defaultRequestHeaders.sms"</js>
	 * 	<li><b>Data type:</b> <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b> empty map
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Specifies default values for request headers.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_defaultRequestHeaders}
	 * 	<li>Annotation:  {@link RestResource#defaultRequestHeaders()} / {@link RestMethod#defaultRequestHeaders()} 
	 * 	<li>Method: {@link RestContextBuilder#defaultRequestHeader(String,Object)} / {@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 	<li>Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 *	</ul>
	 */
	public static final String REST_defaultRequestHeaders = PREFIX + "defaultRequestHeaders.sms";

	/**
	 * <b>Configuration property:</b>  Default response headers.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.defaultResponseHeaders.oms"</js>
	 * 	<li><b>Data type:</b> <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b> empty map
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Specifies default values for response headers.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_defaultResponseHeaders}
	 * 	<li>Annotation:  {@link RestResource#defaultResponseHeaders()} 
	 * 	<li>Method: {@link RestContextBuilder#defaultResponseHeader(String,Object)} / {@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 	<li>This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of 
	 * 		the Java methods.
	 * 	<li>The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 *	</ul>
	 */
	public static final String REST_defaultResponseHeaders = PREFIX + "defaultResponseHeaders.oms";

	/**
	 * <b>Configuration property:</b>  Supported content media types.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.supportedContentTypes.ls"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_supportedContentTypes}
	 * 	<li>Annotation:  N/A 
	 * 	<li>Method: {@link RestContextBuilder#supportedContentTypes(boolean,String...)} / {@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}
	 *	</ul>
	 */
	public static final String REST_supportedContentTypes = PREFIX + "supportedContentTypes.ls";
	
	/**
	 * <b>Configuration property:</b>  Supported accept media types.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.supportedAcceptTypes.ls"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_supportedAcceptTypes}
	 * 	<li>Annotation:  N/A 
	 * 	<li>Method: {@link RestContextBuilder#supportedAcceptTypes(boolean,String...)} / {@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}
	 *	</ul>
	 */
	public static final String REST_supportedAcceptTypes = PREFIX + "supportedAcceptTypes.ls";

	/**
	 * <b>Configuration property:</b>  Client version header.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.clientVersionHeader.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"X-Client-Version"</js>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Specifies the name of the header used to denote the client version on HTTP requests.
	 *
	 * <p>
	 * The client version is used to support backwards compatibility for breaking REST interface changes.
	 * <br>Used in conjunction with {@link RestMethod#clientVersion()} annotation.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_clientVersionHeader}
	 * 	<li>Annotation:  {@link RestResource#clientVersionHeader()} 
	 * 	<li>Method: {@link RestContextBuilder#clientVersionHeader(String)}
	 *	</ul>
	 */
	public static final String REST_clientVersionHeader = PREFIX + "clientVersionHeader.s";

	/**
	 * <b>Configuration property:</b>  REST resource resolver.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.resourceResolver.o"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? <jk>extends</jk> RestResourceResolver&gt; | RestResourceResolver</code>
	 * 	<li><b>Default:</b> {@link RestResourceResolverSimple}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * The resolver used for resolving child resources.
	 * 
	 * <p>
	 * Can be used to provide customized resolution of REST resource class instances (e.g. resources retrieve from Spring).
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_resourceResolver}
	 * 	<li>Annotation:  {@link RestResource#resourceResolver()} 
	 * 	<li>Method: {@link RestContextBuilder#resourceResolver(Class)} / {@link RestContextBuilder#resourceResolver(RestResourceResolver)}
	 * 	<li>Unless overridden, resource resolvers are inherited from parent resources.
	 *	</ul>
	 */
	public static final String REST_resourceResolver = PREFIX + "resourceResolver.o";

	/**
	 * <b>Configuration property:</b>  REST logger.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.logger.o"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? <jk>extends</jk> RestLogger&gt; | RestLogger</code>
	 * 	<li><b>Default:</b> {@link RestLogger.Normal}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Specifies the logger to use for logging.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_logger}
	 * 	<li>Annotation:  {@link RestResource#logger()} 
	 * 	<li>Method: {@link RestContextBuilder#logger(Class)} / {@link RestContextBuilder#logger(RestLogger)} 
	 * 	<li>The {@link RestLogger.Normal} logger can be used to provide basic error logging to the Java logger.
	 *	</ul>
	 */
	public static final String REST_logger = PREFIX + "logger.o";

	/**
	 * <b>Configuration property:</b>  REST call handler.
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.callHandler.o"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? <jk>extends</jk> RestCallHandler&gt; | RestCallHandler</code>
	 * 	<li><b>Default:</b> {@link RestCallHandler}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_callHandler}
	 * 	<li>Annotation:  {@link RestResource#callHandler()} 
	 * 	<li>Method: {@link RestContextBuilder#callHandler(Class)} / {@link RestContextBuilder#callHandler(RestCallHandler)} 
	 *	</ul>
	 */
	public static final String REST_callHandler = PREFIX + "callHandler.o";

	/**
	 * <b>Configuration property:</b>  REST info provider. 
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.infoProvider.o"</js>
	 * 	<li><b>Data type:</b> <code>Class&lt;? <jk>extends</jk> RestInfoProvider&gt; | RestInfoProvider</code>
	 * 	<li><b>Default:</b> {@link RestInfoProvider}
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_infoProvider}
	 * 	<li>Annotation:  {@link RestResource#infoProvider()} 
	 * 	<li>Method: {@link RestContextBuilder#infoProvider(Class)} / {@link RestContextBuilder#infoProvider(RestInfoProvider)} 
	 *	</ul>
	 */
	public static final String REST_infoProvider = PREFIX + "infoProvider.o";
	
	/**
	 * <b>Configuration property:</b>  Resource path.   
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.path.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_path}
	 * 	<li>Annotation:  {@link RestResource#path()} 
	 * 	<li>Method: {@link RestContextBuilder#path(String)} 
	 * 	<li>This annotation is ignored on top-level servlets (i.e. servlets defined in <code>web.xml</code> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>Typically, this setting is only applicable to resources defined as children through the 
	 * 		{@link RestResource#children()} annotation.
	 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 *	</ul>
	 */
	public static final String REST_path = PREFIX + "path.s";

	/**
	 * <b>Configuration property:</b>  Resource context path. 
	 *
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestContext.contextPath.s"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <jk>null</jk>
	 * 	<li><b>Session-overridable:</b> <jk>false</jk>
	 * </ul>
	 * 
	 * <p>
	 * Overrides the context path value for this resource and any child resources.
	 *
	 * <p>
	 * This setting is useful if you want to use <js>"context:/child/path"</js> URLs in child resource POJOs but
	 * the context path is not actually specified on the servlet container.
	 * The net effect is that the {@link RestRequest#getContextPath()} and {@link RestRequest#getServletPath()} methods
	 * will return this value instead of the actual context path of the web app.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: {@link RestContext#REST_contextPath}
	 * 	<li>Annotation:  {@link RestResource#contextPath()} 
	 * 	<li>Method: {@link RestContextBuilder#contextPath(String)} 
	 *	</ul>
	 */
	public static final String REST_contextPath = PREFIX + "contextPath.s";
	
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Object resource;
	final RestContextBuilder builder;
	private final boolean
		allowHeaderParams,
		allowBodyParam,
		renderResponseStackTraces,
		useStackTraceHashes;
	private final String
		defaultCharset,
		clientVersionHeader,
		contextPath;
	private final long
		maxInput;
	
	final String fullPath;

	private final Map<String,Widget> widgets;

	private final Set<String> allowedMethodParams;

	private final ObjectMap properties;
	private final Map<Class<?>,RestParam> paramResolvers;
	private final SerializerGroup serializers;
	private final ParserGroup parsers;
	private final HttpPartSerializer partSerializer;
	private final HttpPartParser partParser;
	private final EncoderGroup encoders;
	private final List<MediaType>
		supportedContentTypes,
		supportedAcceptTypes;
	private final Map<String,String> defaultRequestHeaders, defaultResponseHeaders;
	private final BeanContext beanContext;
	private final RestConverter[] converters;
	private final RestGuard[] guards;
	private final ResponseHandler[] responseHandlers;
	private final MimetypesFileTypeMap mimetypesFileTypeMap;
	private final Map<String,String> staticFilesMap;
	private final String[] staticFilesPrefixes;
	private final MessageBundle msgs;
	private final ConfigFile configFile;
	private final VarResolver varResolver;
	private final Map<String,CallRouter> callRouters;
	private final Map<String,CallMethod> callMethods;
	private final Map<String,RestContext> childResources;
	private final RestLogger logger;
	private final RestCallHandler callHandler;
	private final RestInfoProvider infoProvider;
	private final RestException initException;
	private final RestContext parentContext;
	private final RestResourceResolver resourceResolver;

	// Lifecycle methods
	private final Method[]
		postInitMethods,
		postInitChildFirstMethods,
		preCallMethods,
		postCallMethods,
		startCallMethods,
		endCallMethods,
		destroyMethods;
	private final RestParam[][]
		preCallMethodParams,
		postCallMethodParams;
	private final Class<?>[][]
		postInitMethodParams,
		postInitChildFirstMethodParams,
		startCallMethodParams,
		endCallMethodParams,
		destroyMethodParams;

	// In-memory cache of images and stylesheets in the org.apache.juneau.rest.htdocs package.
	private final Map<String,StreamResource> staticFilesCache = new ConcurrentHashMap<>();

	private final ResourceFinder resourceFinder;
	private final ConcurrentHashMap<Integer,AtomicInteger> stackTraceHashes = new ConcurrentHashMap<>();


	/**
	 * Constructor.
	 *
	 * @param builder The servlet configuration object.
	 * @throws Exception If any initialization problems were encountered.
	 */
	@SuppressWarnings("unchecked")
	public RestContext(RestContextBuilder builder) throws Exception {
		super(builder.getPropertyStore());
		
		RestException _initException = null;
		ServletContext servletContext = builder.servletContext;

		this.resource = builder.resource;
		this.builder = builder;
		this.parentContext = builder.parentContext;
		
		PropertyStore ps = getPropertyStore();

		contextPath = nullIfEmpty(getProperty(REST_contextPath, String.class, null));
		allowHeaderParams = getProperty(REST_allowHeaderParams, boolean.class, true);
		allowBodyParam = getProperty(REST_allowBodyParam, boolean.class, true);
		allowedMethodParams = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(StringUtils.split(getProperty(REST_allowedMethodParams, String.class, "HEAD,OPTIONS")))));
		renderResponseStackTraces = getProperty(REST_renderResponseStackTraces, boolean.class, false);
		useStackTraceHashes = getProperty(REST_useStackTraceHashes, boolean.class, true);
		defaultCharset = getProperty(REST_defaultCharset, String.class, "utf-8");
		maxInput = getProperty(REST_maxInput, long.class, 100_000_000l);
		clientVersionHeader = getProperty(REST_clientVersionHeader, String.class, "X-Client-Version");

		converters = getInstanceArrayProperty(REST_converters, resource, RestConverter.class, new RestConverter[0], true, ps);
		guards = getInstanceArrayProperty(REST_guards, resource, RestGuard.class, new RestGuard[0], true, ps);
		responseHandlers = getInstanceArrayProperty(REST_responseHandlers, resource, ResponseHandler.class, new ResponseHandler[0], true, ps);

		Map<Class<?>,RestParam> _paramResolvers = new HashMap<>();
		for (RestParam rp : getInstanceArrayProperty(REST_paramResolvers, RestParam.class, new RestParam[0], true, ps)) 
			_paramResolvers.put(rp.forClass(), rp);
		paramResolvers = Collections.unmodifiableMap(_paramResolvers);
		
		Map<String,String> _defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		_defaultRequestHeaders.putAll(getMapProperty(REST_defaultRequestHeaders, String.class));
		defaultRequestHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(_defaultRequestHeaders));
		
		defaultResponseHeaders = getMapProperty(REST_defaultResponseHeaders, String.class);
		
		logger = getInstanceProperty(REST_logger, resource, RestLogger.class, RestLogger.NoOp.class, false);

		try {
			this.resourceFinder = new ResourceFinder(resource.getClass());

			Builder b = new Builder(builder, ps);
			this.varResolver = b.varResolver;
			this.configFile = b.configFile;
			this.properties = b.properties;
			this.serializers = b.serializers;
			this.parsers = b.parsers;
			this.partSerializer = b.partSerializer;
			this.partParser = b.partParser;
			this.encoders = b.encoders;
			this.beanContext = b.beanContext;
			this.mimetypesFileTypeMap = b.mimetypesFileTypeMap;
			this.staticFilesMap = Collections.unmodifiableMap(b.staticFilesMap);
			this.staticFilesPrefixes = b.staticFilesPrefixes;
			this.msgs = b.messageBundle;
			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.
			this.fullPath = b.fullPath;
			this.widgets = Collections.unmodifiableMap(b.widgets);

			supportedContentTypes = getListProperty(REST_supportedContentTypes, MediaType.class, serializers.getSupportedMediaTypes());
			supportedAcceptTypes = getListProperty(REST_supportedAcceptTypes, MediaType.class, parsers.getSupportedMediaTypes());

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<>();   // Temporary to help debug transient duplicate method issue.
			Map<String,CallRouter.Builder> routers = new LinkedHashMap<>();
			Map<String,CallMethod> _javaRestMethods = new LinkedHashMap<>();
			Map<String,Method>
				_startCallMethods = new LinkedHashMap<>(),
				_preCallMethods = new LinkedHashMap<>(),
				_postCallMethods = new LinkedHashMap<>(),
				_endCallMethods = new LinkedHashMap<>(),
				_postInitMethods = new LinkedHashMap<>(),
				_postInitChildFirstMethods = new LinkedHashMap<>(),
				_destroyMethods = new LinkedHashMap<>();
			List<RestParam[]>
				_preCallMethodParams = new ArrayList<>(),
				_postCallMethodParams = new ArrayList<>();
			List<Class<?>[]>
				_startCallMethodParams = new ArrayList<>(),
				_endCallMethodParams = new ArrayList<>(),
				_postInitMethodParams = new ArrayList<>(),
				_postInitChildFirstMethodParams = new ArrayList<>(),
				_destroyMethodParams = new ArrayList<>();

			for (java.lang.reflect.Method method : resource.getClass().getMethods()) {
				if (method.isAnnotationPresent(RestMethod.class)) {
					RestMethod a = method.getAnnotation(RestMethod.class);
					methodsFound.add(method.getName() + "," + a.name() + "," + a.path());
					try {
						if (! Modifier.isPublic(method.getModifiers()))
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", this.getClass().getName(), method.getName());

						CallMethod sm = new CallMethod(resource, method, this);
						String httpMethod = sm.getHttpMethod();

						// PROXY is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("PROXY".equals(httpMethod)) {

							final ClassMeta<?> interfaceClass = beanContext.getClassMeta(method.getGenericReturnType());
							final Map<String,Method> remoteableMethods = interfaceClass.getRemoteableMethods();
							if (remoteableMethods.isEmpty())
								throw new RestException(SC_INTERNAL_SERVER_ERROR, "Method {0} returns an interface {1} that doesn't define any remoteable methods.", getMethodSignature(method), interfaceClass.getReadableName());

							sm = new CallMethod(resource, method, this) {

								@Override
								int invoke(String pathInfo, RestRequest req, RestResponse res) throws RestException {

									int rc = super.invoke(pathInfo, req, res);
									if (rc != SC_OK)
										return rc;

									final Object o = res.getOutput();

									if ("GET".equals(req.getMethod())) {
										res.setOutput(getMethodInfo(remoteableMethods.values()));
										return SC_OK;

									} else if ("POST".equals(req.getMethod())) {
										if (pathInfo.indexOf('/') != -1)
											pathInfo = pathInfo.substring(pathInfo.lastIndexOf('/')+1);
										pathInfo = urlDecode(pathInfo);
										java.lang.reflect.Method m = remoteableMethods.get(pathInfo);
										if (m != null) {
											try {
												// Parse the args and invoke the method.
												Parser p = req.getBody().getParser();
												try (Closeable in = p.isReaderParser() ? req.getReader() : req.getInputStream()) {
													Object output = m.invoke(o, p.parseArgs(in, m.getGenericParameterTypes()));
													res.setOutput(output);
												}
												return SC_OK;
											} catch (Exception e) {
												throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
											}
										}
									}
									return SC_NOT_FOUND;
								}
							};

							_javaRestMethods.put(method.getName(), sm);
							addToRouter(routers, "GET", sm);
							addToRouter(routers, "POST", sm);

						} else {
							_javaRestMethods.put(method.getName(), sm);
							addToRouter(routers, httpMethod, sm);
						}
					} catch (RestServletException e) {
						throw new RestServletException("Problem occurred trying to serialize methods on class {0}, methods={1}", this.getClass().getName(), JsonSerializer.DEFAULT_LAX.serialize(methodsFound)).initCause(e);
					}
				}
			}

			for (Method m : ClassUtils.getAllMethods(resource.getClass(), true)) {
				if (ClassUtils.isPublic(m) && m.isAnnotationPresent(RestHook.class)) {
					HookEvent he = m.getAnnotation(RestHook.class).value();
					String sig = ClassUtils.getMethodSignature(m);
					switch(he) {
						case PRE_CALL: {
							if (! _preCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_preCallMethods.put(sig, m);
								_preCallMethodParams.add(findParams(m, null, true));
							}
							break;
						}
						case POST_CALL: {
							if (! _postCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postCallMethods.put(sig, m);
								_postCallMethodParams.add(findParams(m, null, true));
							}
							break;
						}
						case START_CALL: {
							if (! _startCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_startCallMethods.put(sig, m);
								_startCallMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case END_CALL: {
							if (! _endCallMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_endCallMethods.put(sig, m);
								_endCallMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, HttpServletRequest.class, HttpServletResponse.class);
							}
							break;
						}
						case POST_INIT: {
							if (! _postInitMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postInitMethods.put(sig, m);
								_postInitMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						case POST_INIT_CHILD_FIRST: {
							if (! _postInitChildFirstMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_postInitChildFirstMethods.put(sig, m);
								_postInitChildFirstMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						case DESTROY: {
							if (! _destroyMethods.containsKey(sig)) {
								Visibility.setAccessible(m);
								_destroyMethods.put(sig, m);
								_destroyMethodParams.add(m.getParameterTypes());
								ClassUtils.assertArgsOfType(m, RestContext.class);
							}
							break;
						}
						default: // Ignore INIT
					}
				}
			}

			this.callMethods = Collections.unmodifiableMap(_javaRestMethods);
			this.preCallMethods = _preCallMethods.values().toArray(new Method[_preCallMethods.size()]);
			this.postCallMethods = _postCallMethods.values().toArray(new Method[_postCallMethods.size()]);
			this.startCallMethods = _startCallMethods.values().toArray(new Method[_startCallMethods.size()]);
			this.endCallMethods = _endCallMethods.values().toArray(new Method[_endCallMethods.size()]);
			this.postInitMethods = _postInitMethods.values().toArray(new Method[_postInitMethods.size()]);
			this.postInitChildFirstMethods = _postInitChildFirstMethods.values().toArray(new Method[_postInitChildFirstMethods.size()]);
			this.destroyMethods = _destroyMethods.values().toArray(new Method[_destroyMethods.size()]);
			this.preCallMethodParams = _preCallMethodParams.toArray(new RestParam[_preCallMethodParams.size()][]);
			this.postCallMethodParams = _postCallMethodParams.toArray(new RestParam[_postCallMethodParams.size()][]);
			this.startCallMethodParams = _startCallMethodParams.toArray(new Class[_startCallMethodParams.size()][]);
			this.endCallMethodParams = _endCallMethodParams.toArray(new Class[_endCallMethodParams.size()][]);
			this.postInitMethodParams = _postInitMethodParams.toArray(new Class[_postInitMethodParams.size()][]);
			this.postInitChildFirstMethodParams = _postInitChildFirstMethodParams.toArray(new Class[_postInitChildFirstMethodParams.size()][]);
			this.destroyMethodParams = _destroyMethodParams.toArray(new Class[_destroyMethodParams.size()][]);

			Map<String,CallRouter> _callRouters = new LinkedHashMap<>();
			for (CallRouter.Builder crb : routers.values())
				_callRouters.put(crb.getHttpMethodName(), crb.build());
			this.callRouters = Collections.unmodifiableMap(_callRouters);

			// Initialize our child resources.
			resourceResolver = getInstanceProperty(REST_resourceResolver, resource, RestResourceResolver.class, parentContext == null ? RestResourceResolverSimple.class : parentContext.resourceResolver, true, this);
			for (Object o : builder.childResources) {
				String path = null;
				Object r = null;
				if (o instanceof Pair) {
					Pair<String,Object> p = (Pair<String,Object>)o;
					path = p.first();
					r = p.second();
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
					childBuilder = new RestContextBuilder(builder.inner, oc, this);
					r = resourceResolver.resolve(oc, childBuilder);
				} else {
					r = o;
					childBuilder = new RestContextBuilder(builder.inner, o.getClass(), this);
				}

				childBuilder.init(r);
				if (r instanceof RestServlet)
					((RestServlet)r).innerInit(childBuilder);
				childBuilder.servletContext(servletContext);
				RestContext rc2 = new RestContext(childBuilder);
				if (r instanceof RestServlet)
					((RestServlet)r).setContext(rc2);
				path = childBuilder.path;
				childResources.put(path, rc2);
			}

			callHandler = getInstanceProperty(REST_callHandler, resource, RestCallHandler.class, RestCallHandler.class, true, this);
			infoProvider = getInstanceProperty(REST_infoProvider, resource, RestInfoProvider.class, RestInfoProvider.class, true, this);

		} catch (RestException e) {
			_initException = e;
			throw e;
		} catch (Exception e) {
			_initException = new RestException(SC_INTERNAL_SERVER_ERROR, e);
			throw e;
		} finally {
			initException = _initException;
		}
	}

	private static void addToRouter(Map<String, CallRouter.Builder> routers, String httpMethodName, CallMethod cm) throws RestServletException {
		if (! routers.containsKey(httpMethodName))
			routers.put(httpMethodName, new CallRouter.Builder(httpMethodName));
		routers.get(httpMethodName).add(cm);
	}

	private static final class Builder {

		VarResolver varResolver;
		ConfigFile configFile;
		ObjectMap properties;
		SerializerGroup serializers;
		ParserGroup parsers;
		HttpPartSerializer partSerializer;
		HttpPartParser partParser;
		EncoderGroup encoders;

		BeanContext beanContext;
		MimetypesFileTypeMap mimetypesFileTypeMap;
		Map<String,String> staticFilesMap;
		String[] staticFilesPrefixes;
		MessageBundle messageBundle;
		String fullPath;
		Map<String,Widget> widgets;

		@SuppressWarnings("unchecked")
		Builder(RestContextBuilder rcb, PropertyStore ps) throws Exception {

			Object resource = rcb.resource;
			
			LinkedHashMap<Class<?>,RestResource> restResourceAnnotationsChildFirst = findAnnotationsMap(RestResource.class, resource.getClass());

			varResolver = rcb.varResolverBuilder
				.vars(FileVar.class, LocalizationVar.class, RequestVar.class, SerializedRequestAttrVar.class, ServletInitParamVar.class, UrlVar.class, UrlEncodeVar.class, WidgetVar.class)
				.build()
			;
			
			configFile = rcb.configFile.getResolving(this.varResolver);
			properties = rcb.properties;
			
			// Find resource resource bundle location.
			for (Map.Entry<Class<?>,RestResource> e : restResourceAnnotationsChildFirst.entrySet()) {
				Class<?> c = e.getKey();
				RestResource r = e.getValue();
				if (! r.messages().isEmpty()) {
					if (messageBundle == null)
						messageBundle = new MessageBundle(c, r.messages());
					else
						messageBundle.addSearchPath(c, r.messages());
				}
			}

			if (messageBundle == null)
				messageBundle = new MessageBundle(resource.getClass(), "");
			
			serializers = rcb.serializers.apply(ps).add(properties).build();
			parsers = rcb.parsers.apply(ps).add(properties).build();
			partSerializer = resolve(resource, HttpPartSerializer.class, rcb.partSerializer, serializers.getPropertyStore());
			partParser = resolve(resource, HttpPartParser.class, rcb.partParser, parsers.getPropertyStore());
			encoders = rcb.encoders.build();
			beanContext = BeanContext.create().apply(ps).add(properties).build();

			mimetypesFileTypeMap = rcb.mimeTypes;

			VarResolver vr = rcb.getVarResolverBuilder().build();

			staticFilesMap = new LinkedHashMap<>();
			if (rcb.staticFiles != null) {
				for (Object o : rcb.staticFiles) {
					if (o instanceof Pair) {
						Pair<Class<?>,String> p = (Pair<Class<?>,String>)o;
						// TODO - Currently doesn't take parent class location into account.
						staticFilesMap.putAll(JsonParser.DEFAULT.parse(vr.resolve(p.second()), LinkedHashMap.class));
					} else {
						throw new RuntimeException("TODO");
					}
				}
			}
			staticFilesPrefixes = staticFilesMap.keySet().toArray(new String[0]);


			fullPath = (rcb.parentContext == null ? "" : (rcb.parentContext.fullPath + '/')) + rcb.path;

			HtmlDocBuilder hdb = new HtmlDocBuilder(rcb.properties);

			this.widgets = new LinkedHashMap<>();

			for (Class<? extends Widget> wc : rcb.widgets) {
				Widget w = resolve(resource, Widget.class, wc);
				String n = w.getName();
				this.widgets.put(n, w);
				hdb.script("INHERIT", "$W{"+n+".script}");
				hdb.style("INHERIT", "$W{"+n+".style}");
			}
		}
	}

	static final boolean getBoolean(Object o, String systemProperty, boolean def) {
		if (o == null)
			o = SystemUtils.getFirstBoolean(def, systemProperty);
		return "true".equalsIgnoreCase(o.toString());
	}

	static final String getString(Object o, String systemProperty, String def) {
		if (o == null)
			o = SystemUtils.getFirstString(def, systemProperty);
		return o.toString();
	}

	static final long getLong(Object o, String systemProperty, long def) {
		String s = StringUtils.toString(o);
		if (s == null)
			s = System.getProperty(systemProperty);
		if (StringUtils.isEmpty(s))
			return def;
		return StringUtils.parseLongWithSuffix(s);
	}

	/**
	 * Returns the resource resolver associated with this context.
	 *
	 * <p>
	 * The resource resolver is used for instantiating child resource classes.
	 *
	 * <p>
	 * Unless overridden via the {@link RestResource#resourceResolver()} annotation or the {@link RestContextBuilder#resourceResolver(Class)}
	 * method, this value is always inherited from parent to child.
	 * This allows a single resource resolver to be passed in to the top-level servlet to handle instantiation of all
	 * child resources.
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
	 * <h6 class='figure'>Example:</h6>
	 * <p class='bcode'>
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
	 * 	<jk>public class</jk> MyRestResource <jk>extends</jk> RestServletDefault {
	 * </p>
	 *
	 * <p>
	 * A typical usage pattern involves using variables inside the {@link HtmlDoc} annotation:
	 * <p class='bcode'>
	 * 	<ja>@RestMethod</ja>(
	 * 		name=<jsf>GET</jsf>, path=<js>"/{name}/*"</js>,
	 * 		htmldoc=@HtmlDoc(
	 * 			navlinks={
	 * 				<js>"up: $R{requestParentURI}"</js>,
	 * 				<js>"options: servlet:/?method=OPTIONS"</js>,
	 * 				<js>"editLevel: servlet:/editLevel?logger=$A{attribute.name, OFF}"</js>
	 * 			}
	 * 			header={
	 * 				<js>"&lt;h1&gt;$L{MyLocalizedPageTitle}&lt;/h1&gt;"</js>
	 * 			},
	 * 			aside={
	 * 				<js>"$F{resources/AsideText.html}"</js>
	 * 			}
	 * 		)
	 * 	)
	 * 	<jk>public</jk> LoggerEntry getLogger(RestRequest req, <ja>@Path</ja> String name) <jk>throws</jk> Exception {
	 * </p>
	 *
	 * <p>
	 * The following is the default list of supported variables:
	 * <ul>
	 * 	<li><code>$C{key[,defaultValue]}</code> - Config file entry. See {@link ConfigFileVar}.
	 * 	<li><code>$E{envVar[,defaultValue]}</code> - Environment variable. See {@link EnvVariablesVar}.
	 * 	<li><code>$F{path[,defaultValue]}</code> - File resource. See {@link FileVar}.
	 * 	<li><code>$I{name[,defaultValue]}</code> - Servlet init parameter. See {@link ServletInitParamVar}.
	 * 	<li><code>$L{key[,args...]}</code> - Localized message. See {@link LocalizationVar}.
	 * 	<li><code>$RA{key1[,key2...]}</code> - Request attribute variable. See {@link RequestAttributeVar}.
	 * 	<li><code>$RF{key1[,key2...]}</code> - Request form-data variable. See {@link RequestFormDataVar}.
	 * 	<li><code>$RH{key1[,key2...]}</code> - Request header variable. See {@link RequestHeaderVar}.
	 * 	<li><code>$RP{key1[,key2...]}</code> - Request path variable. See {@link RequestPathVar}.
	 * 	<li><code>$RQ{key1[,key2...]}</code> - Request query parameter variable. See {@link RequestQueryVar}.
	 * 	<li><code>$R{key1[,key2...]}</code> - Request object variable. See {@link RequestVar}.
	 * 	<li><code>$S{systemProperty[,defaultValue]}</code> - System property. See {@link SystemPropertiesVar}.
	 * 	<li><code>$SA{contentType,key[,defaultValue]}</code> - Serialized request attribute. See {@link SerializedRequestAttrVar}.
	 * 	<li><code>$U{uri}</code> - URI resolver. See {@link UrlVar}.
	 * 	<li><code>$UE{uriPart}</code> - URL-Encoder. See {@link UrlEncodeVar}.
	 * 	<li><code>$W{widgetName}</code> - HTML widget variable. See {@link WidgetVar}.
	 * </ul>
	 *
	 * <p>
	 * The following syntax variables are also provided:
	 * <ul>
	 * 	<li><code>$CO{string1[,string2...]}</code> - Coalesce variable. See {@link CoalesceVar}.
	 * 	<li><code>$CR{string1[,string2...]}</code> - Coalesce-and-recurse variable. See {@link CoalesceAndRecurseVar}.
	 * 	<li><code>$IF{booleanArg,thenValue[,elseValue]}</code> - If/else variable. See {@link IfVar}.
	 * 	<li><code>$SW{stringArg(,pattern,thenValue)+[,elseValue]}</code> - Switch variable. See {@link SwitchVar}.
	 * </ul>
	 *
	 * <p>
	 * The list of variables can be extended using the {@link RestContextBuilder#vars(Class...)} method.
	 * For example, this is used to add support for the Args and Manifest-File variables in the microservice
	 * <code>Resource</code> class.
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
	 * 	<li>{@link RestResource#config() @RestResource.config()} annotation.
	 * 	<li>{@link RestContextBuilder#configFile(ConfigFile)} method.
	 * </ul>
	 *
	 * @return The resolving config file associated with this servlet.  Never <jk>null</jk>.
	 */
	public ConfigFile getConfigFile() {
		return configFile;
	}

	/**
	 * Resolve a static resource file.
	 *
	 * <p>
	 * The location of static resources are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#staticFiles() @RestResource.staticFiles()} annotation.
	 * 	<li>{@link RestContextBuilder#staticFiles(Class, String)} method.
	 * </ul>
	 *
	 * @param pathInfo The unencoded path info.
	 * @return The resource, or <jk>null</jk> if the resource could not be resolved.
	 * @throws IOException
	 */
	public StreamResource resolveStaticFile(String pathInfo) throws IOException {
		if (! staticFilesCache.containsKey(pathInfo)) {
			String p = urlDecode(trimSlashes(pathInfo));
			if (p.indexOf("..") != -1)
				throw new RestException(SC_NOT_FOUND, "Invalid path");
			for (Map.Entry<String,String> e : staticFilesMap.entrySet()) {
				String key = trimSlashes(e.getKey());
				if (p.startsWith(key)) {
					String remainder = (p.equals(key) ? "" : p.substring(key.length()));
					if (remainder.isEmpty() || remainder.startsWith("/")) {
						String p2 = trimSlashes(e.getValue()) + remainder;
						try (InputStream is = getResource(p2, null)) {
							if (is != null) {
								int i = p2.lastIndexOf('/');
								String name = (i == -1 ? p2 : p2.substring(i+1));
								String mediaType = mimetypesFileTypeMap.getContentType(name);
								ObjectMap headers = new ObjectMap().append("Cache-Control", "max-age=86400, public");
								staticFilesCache.put(pathInfo, new StreamResource(MediaType.forString(mediaType), headers, is));
								return staticFilesCache.get(pathInfo);
							}
						}
					}
				}
			}
		}
		return staticFilesCache.get(pathInfo);
	}

	/**
	 * Same as {@link Class#getResourceAsStream(String)} except if it doesn't find the resource on this class, searches
	 * up the parent hierarchy chain.
	 *
	 * <p>
	 * If the resource cannot be found in the classpath, then an attempt is made to look in the JVM working directory.
	 *
	 * <p>
	 * If the <code>locale</code> is specified, then we look for resources whose name matches that locale.
	 * For example, if looking for the resource <js>"MyResource.txt"</js> for the Japanese locale, we will look for
	 * files in the following order:
	 * <ol>
	 * 	<li><js>"MyResource_ja_JP.txt"</js>
	 * 	<li><js>"MyResource_ja.txt"</js>
	 * 	<li><js>"MyResource.txt"</js>
	 * </ol>
	 *
	 * @param name The resource name.
	 * @param locale Optional locale.
	 * @return An input stream of the resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected InputStream getResource(String name, Locale locale) throws IOException {
		return resourceFinder.getResourceAsStream(name, locale);
	}

	/**
	 * Reads the input stream from {@link #getResource(String, Locale)} into a String.
	 *
	 * @param name The resource name.
	 * @param locale Optional locale.
	 * @return The contents of the stream as a string, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException If resource could not be found.
	 */
	public String getResourceAsString(String name, Locale locale) throws IOException {
		return resourceFinder.getResourceAsString(name, locale);
	}

	/**
	 * Reads the input stream from {@link #getResource(String, Locale)} and parses it into a POJO using the parser
	 * matched by the specified media type.
	 *
	 * <p>
	 * Useful if you want to load predefined POJOs from JSON files in your classpath.
	 *
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale Optional locale.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getResource(Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		InputStream is = getResource(name, locale);
		if (is == null)
			return null;
		try {
			Parser p = parsers.getParser(mediaType);
			if (p != null) {
				try {
					try (Closeable in = p.isReaderParser() ? new InputStreamReader(is, UTF8) : is) {
						return p.parse(in, c);
					}
				} catch (ParseException e) {
					throw new ServletException("Could not parse resource '' as media type '"+mediaType+"'.");
				}
			}
			throw new ServletException("Unknown media type '"+mediaType+"'");
		} catch (Exception e) {
			throw new ServletException("Could not parse resource with name '"+name+"'", e);
		}
	}

	/**
	 * Returns the path for this resource as defined by the {@link RestResource#path()} annotation or
	 * {@link RestContextBuilder#path(String)} method concatenated with those on all parent classes.
	 *
	 * <p>
	 * If path is not specified, returns <js>"/"</js>.
	 *
	 * <p>
	 * Path always starts with <js>"/"</js>.
	 *
	 * @return The servlet path.
	 */
	public String getPath() {
		return fullPath;
	}

	/**
	 * The widgets used for resolving <js>"$W{...}"<js> variables.
	 *
	 * <p>
	 * Defined by the {@link HtmlDoc#widgets()} annotation or {@link RestContextBuilder#widget(Class)} method.
	 *
	 * @return The var resolver widgets as a map with keys being the name returned by {@link Widget#getName()}.
	 */
	public Map<String,Widget> getWidgets() {
		return widgets;
	}

	/**
	 * Returns the logger to use for this resource.
	 *
	 * <p>
	 * The logger for a resource is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#logger() @RestResource.logger()} annotation.
	 * 	<li>{@link RestContextBuilder#logger(Class)}/{@link RestContextBuilder#logger(RestLogger)} methods.
	 * </ul>
	 *
	 * @return The logger to use for this resource.  Never <jk>null</jk>.
	 */
	public RestLogger getLogger() {
		return logger;
	}

	/**
	 * Returns the resource bundle used by this resource.
	 *
	 * <p>
	 * The resource bundle is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#messages() @RestResource.messages()} annotation.
	 * </ul>
	 *
	 * @return The resource bundle for this resource.  Never <jk>null</jk>.
	 */
	public MessageBundle getMessages() {
		return msgs;
	}

	/**
	 * Returns the REST information provider used by this resource.
	 *
	 * <p>
	 * The information provider is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#infoProvider() @RestResource.infoProvider()} annotation.
	 * 	<li>{@link RestContextBuilder#infoProvider(Class)}/{@link RestContextBuilder#infoProvider(RestInfoProvider)} methods.
	 * </ul>
	 *
	 * @return The information provider for this resource.  Never <jk>null</jk>.
	 */
	public RestInfoProvider getInfoProvider() {
		return infoProvider;
	}

	/**
	 * Returns the REST call handler used by this resource.
	 *
	 * <p>
	 * The call handler is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#callHandler() @RestResource.callHandler()} annotation.
	 * 	<li>{@link RestContextBuilder#callHandler(Class)}/{@link RestContextBuilder#callHandler(RestCallHandler)} methods.
	 * </ul>
	 *
	 * @return The call handler for this resource.  Never <jk>null</jk>.
	 */
	protected RestCallHandler getCallHandler() {
		return callHandler;
	}

	/**
	 * Returns a map of HTTP method names to call routers.
	 *
	 * @return A map with HTTP method names upper-cased as the keys, and call routers as the values.
	 */
	protected Map<String,CallRouter> getCallRouters() {
		return callRouters;
	}

	/**
	 * Returns the resource object.
	 *
	 * <p>
	 * This is the instance of the class annotated with the {@link RestResource @RestResource} annotation, usually
	 * an instance of {@link RestServlet}.
	 *
	 * @return The resource object.  Never <jk>null</jk>.
	 */
	public Object getResource() {
		return resource;
	}

	/**
	 * Returns the resource object as a {@link RestServlet}.
	 *
	 * @return
	 * 	The resource object cast to {@link RestServlet}, or <jk>null</jk> if the resource doesn't subclass from
	 * 	{@link RestServlet}
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
	 * Returns the {@link BeanContext} object used for parsing path variables and header values.
	 *
	 * @return The bean context used for parsing path variables and header values.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the class-level properties associated with this servlet.
	 *
	 * <p>
	 * Properties at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#properties() @RestResource.properties()} annotation.
	 * 	<li>{@link RestContextBuilder#setProperty(String, Object)}/{@link RestContextBuilder#setProperties(Map)} methods.
	 * </ul>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>The returned {@code Map} is mutable.  Therefore, subclasses are free to override
	 * 	or set additional initialization parameters in their {@code init()} method.
	 * </ul>
	 *
	 * @return The resource properties as an {@link ObjectMap}.
	 */
	public ObjectMap getProperties() {
		return properties;
	}

	/**
	 * Returns the serializers registered with this resource.
	 *
	 * <p>
	 * Serializers at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#serializers() @RestResource.serializers()} annotation.
	 * 	<li>{@link RestContextBuilder#serializers(Class...)}/{@link RestContextBuilder#serializers(Serializer...)} methods.
	 * </ul>
	 *
	 * @return The serializers registered with this resource.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Returns the parsers registered with this resource.
	 *
	 * <p>
	 * Parsers at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#parsers() @RestResource.parsers()} annotation.
	 * 	<li>{@link RestContextBuilder#parsers(Class...)}/{@link RestContextBuilder#parsers(Parser...)} methods.
	 * </ul>
	 *
	 * @return The parsers registered with this resource.
	 */
	public ParserGroup getParsers() {
		return parsers;
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
	 * 	Keys are the {@link RestResource#path() @RestResource.path()} annotation defined on the child resource.
	 */
	public Map<String,RestContext> getChildResources() {
		return Collections.unmodifiableMap(childResources);
	}

	/**
	 * Returns the number of times this exception was thrown based on a hash of its stacktrace.
	 *
	 * @param e The exception to check.
	 * @return
	 * 	The number of times this exception was thrown, or <code>0</code> if <code>stackTraceHashes</code>
	 * 	setting is not enabled.
	 */
	protected int getStackTraceOccurrence(Throwable e) {
		if (! useStackTraceHashes)
			return 0;
		int h = e.hashCode();
		stackTraceHashes.putIfAbsent(h, new AtomicInteger());
		return stackTraceHashes.get(h).incrementAndGet();
	}

	/**
	 * Returns the value of the {@link RestResource#renderResponseStackTraces()} setting.
	 *
	 * @return The value of the {@link RestResource#renderResponseStackTraces()} setting.
	 */
	protected boolean isRenderResponseStackTraces() {
		return renderResponseStackTraces;
	}

	/**
	 * Returns the value of the {@link RestResource#allowHeaderParams()} setting.
	 *
	 * @return The value of the {@link RestResource#allowHeaderParams()} setting.
	 */
	protected boolean isAllowHeaderParams() {
		return allowHeaderParams;
	}

	/**
	 * Returns the value of the {@link RestResource#allowBodyParam()} setting.
	 *
	 * @return The value of the {@link RestResource#allowBodyParam()} setting.
	 */
	protected boolean isAllowBodyParam() {
		return allowBodyParam;
	}

	/**
	 * Returns the value of the {@link RestResource#defaultCharset()} setting.
	 *
	 * @return The value of the {@link RestResource#defaultCharset()} setting.
	 */
	protected String getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Returns the value of the {@link RestResource#maxInput()} setting.
	 *
	 * @return The value of the {@link RestResource#maxInput()} setting.
	 */
	protected long getMaxInput() {
		return maxInput;
	}

	/**
	 * Returns the name of the client version header name used by this resource.
	 *
	 * <p>
	 * The client version header is the name of the HTTP header on requests that identify a client version.
	 *
	 * <p>
	 * The client version header is defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#clientVersionHeader() @RestResource.clientVersion()} annotation.
	 * </ul>
	 *
	 * @return The name of the client version header used by this resource.  Never <jk>null</jk>.
	 */
	protected String getClientVersionHeader() {
		return clientVersionHeader;
	}

	/**
	 * Returns <jk>true</jk> if the specified <code>Method</code> GET parameter value can be used to override
	 * the method name in the HTTP header.
	 *
	 * @param m The method name, upper-cased.
	 * @return <jk>true</jk> if this resource allows the specified method to be overridden.
	 */
	protected boolean allowMethodParam(String m) {
		return (! isEmpty(m) && (allowedMethodParams.contains(m) || allowedMethodParams.contains("*")));
	}

	/**
	 * Finds the {@link RestParam} instances to handle resolving objects on the calls to the specified Java method.
	 *
	 * @param method The Java method being called.
	 * @param pathPattern The parsed URL path pattern.
	 * @param isPreOrPost Whether this is a <ja>@RestMethodPre</ja> or <ja>@RestMethodPost</ja>.
	 * @return The array of resolvers.
	 * @throws ServletException If an annotation usage error was detected.
	 */
	protected RestParam[] findParams(Method method, UrlPathPattern pathPattern, boolean isPreOrPost) throws ServletException {

		Type[] pt = method.getGenericParameterTypes();
		Annotation[][] pa = method.getParameterAnnotations();
		RestParam[] rp = new RestParam[pt.length];
		int attrIndex = 0;
		PropertyStore ps = getPropertyStore();

		for (int i = 0; i < pt.length; i++) {

			Type t = pt[i];
			if (t instanceof Class) {
				Class<?> c = (Class<?>)t;
				rp[i] = paramResolvers.get(c);
				if (rp[i] == null)
					rp[i] = RestParamDefaults.STANDARD_RESOLVERS.get(c);
			}

			if (rp[i] == null) {
				for (Annotation a : pa[i]) {
					if (a instanceof Header)
						rp[i] = new RestParamDefaults.HeaderObject((Header)a, t, ps);
					else if (a instanceof FormData)
						rp[i] = new RestParamDefaults.FormDataObject(method, (FormData)a, t, ps);
					else if (a instanceof Query)
						rp[i] = new RestParamDefaults.QueryObject(method, (Query)a, t, ps);
					else if (a instanceof HasFormData)
						rp[i] = new RestParamDefaults.HasFormDataObject(method, (HasFormData)a, t);
					else if (a instanceof HasQuery)
						rp[i] = new RestParamDefaults.HasQueryObject(method, (HasQuery)a, t);
					else if (a instanceof Body)
						rp[i] = new RestParamDefaults.BodyObject(t);
					else if (a instanceof org.apache.juneau.rest.annotation.Method)
						rp[i] = new RestParamDefaults.MethodObject(method, t);
					else if (a instanceof PathRemainder)
						rp[i] = new RestParamDefaults.PathRemainderObject(method, t);
					else if (a instanceof Properties)
						rp[i] = new RestParamDefaults.PropsObject(method, t);
					else if (a instanceof Messages)
						rp[i] = new RestParamDefaults.MessageBundleObject();
				}
			}

			if (rp[i] == null) {

				if (isPreOrPost)
					throw new RestServletException("Invalid parameter specified for method ''{0}'' at index position {1}", method, i);

				Path p = null;
				for (Annotation a : pa[i])
					if (a instanceof Path)
						p = (Path)a;

				String name = (p == null ? "" : firstNonEmpty(p.name(), p.value()));

				if (isEmpty(name)) {
					int idx = attrIndex++;
					String[] vars = pathPattern.getVars();
					if (vars.length <= idx)
						throw new RestServletException("Number of attribute parameters in method ''{0}'' exceeds the number of URL pattern variables.", method);

					// Check for {#} variables.
					String idxs = String.valueOf(idx);
					for (int j = 0; j < vars.length; j++)
						if (isNumeric(vars[j]) && vars[j].equals(idxs))
							name = vars[j];

					if (isEmpty(name))
						name = pathPattern.getVars()[idx];
				}
				rp[i] = new RestParamDefaults.PathParameterObject(name, t);
			}
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

	private static void preOrPost(Object resource, Method m, RestParam[] mp, RestRequest req, RestResponse res) throws RestException {
		if (m != null) {
			Object[] args = new Object[mp.length];
			for (int i = 0; i < mp.length; i++) {
				try {
					args[i] = mp[i].resolve(req, res);
				} catch (RestException e) {
					throw e;
				} catch (Exception e) {
					throw new RestException(SC_BAD_REQUEST,
						"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
						mp[i].getParamType().name(), mp[i].getName(), mp[i].getType(), m.getDeclaringClass().getName(), m.getName()
					).initCause(e);
				}
			}
			try {
				m.invoke(resource, args);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
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

	private static void startOrFinish(Object resource, Method m, Class<?>[] p, HttpServletRequest req, HttpServletResponse res) {
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
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
			}
		}
	}

	/*
	 * Calls all @RestHook(POST_INIT) methods.
	 */
	void postInit() throws ServletException {
		for (int i = 0; i < postInitMethods.length; i++)
			postInitOrDestroy(resource, postInitMethods[i], postInitMethodParams[i]);
		for (RestContext childContext : this.childResources.values())
			childContext.postInit();
	}

	/*
	 * Calls all @RestHook(POST_INIT_CHILD_FIRST) methods.
	 */
	void postInitChildFirst() throws ServletException {
		for (RestContext childContext : this.childResources.values())
			childContext.postInitChildFirst();
		for (int i = 0; i < postInitChildFirstMethods.length; i++)
			postInitOrDestroy(resource, postInitChildFirstMethods[i], postInitChildFirstMethodParams[i]);
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
				throw new RestException(SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage()).initCause(e);
			}
		}
	}


	/**
	 * Returns the HTTP-part parser associated with this resource.
	 *
	 * @return The HTTP-part parser associated with this resource.  Never <jk>null</jk>.
	 */
	protected HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the HTTP-part serializer associated with this resource.
	 *
	 * @return The HTTP-part serializer associated with this resource.  Never <jk>null</jk>.
	 */
	protected HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Returns the encoders associated with this resource.
	 *
	 * <p>
	 * Encoders are used to provide various types of encoding such as <code>gzip</code> encoding.
	 *
	 * <p>
	 * Encoders at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#encoders() @RestResource.encoders()} annotation.
	 * 	<li>{@link RestContextBuilder#encoders(Class...)}/{@link RestContextBuilder#encoders(org.apache.juneau.encoders.Encoder...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The encoders associated with this resource.  Never <jk>null</jk>.
	 */
	protected EncoderGroup getEncoders() {
		return encoders;
	}

	/**
	 * Returns the explicit list of supported accept types for this resource.
	 *
	 * <p>
	 * By default, this is simply the list of accept types supported by the registered parsers, but
	 * can be overridden via the {@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}/{@link RestContextBuilder#supportedAcceptTypes(boolean,String...)}
	 * methods.
	 *
	 * @return The supported <code>Accept</code> header values for this resource.  Never <jk>null</jk>.
	 */
	protected List<MediaType> getSupportedAcceptTypes() {
		return supportedAcceptTypes;
	}

	/**
	 * Returns the explicit list of supported content types for this resource.
	 *
	 * <p>
	 * By default, this is simply the list of content types supported by the registered serializers, but can be
	 * overridden via the {@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}/{@link RestContextBuilder#supportedContentTypes(boolean,String...)}
	 * methods.
	 *
	 * @return The supported <code>Content-Type</code> header values for this resource.  Never <jk>null</jk>.
	 */
	protected List<MediaType> getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/**
	 * Returns the default request headers for this resource.
	 *
	 * <p>
	 * These are headers automatically added to requests if not present.
	 *
	 * <p>
	 * Default request headers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#defaultRequestHeaders() @RestResource.defaultRequestHeaders()} annotation.
	 * 	<li>{@link RestContextBuilder#defaultRequestHeader(String, Object)}/{@link RestContextBuilder#defaultRequestHeaders(String...)} methods.
	 * </ul>
	 *
	 * @return The default request headers for this resource.  Never <jk>null</jk>.
	 */
	protected Map<String,String> getDefaultRequestHeaders() {
		return defaultRequestHeaders;
	}

	/**
	 * Returns the default response headers for this resource.
	 *
	 * <p>
	 * These are headers automatically added to responses if not otherwise specified during the request.
	 *
	 * <p>
	 * Default response headers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#defaultResponseHeaders() @RestResource.defaultResponseHeaders()} annotation.
	 * 	<li>{@link RestContextBuilder#defaultResponseHeader(String, Object)}/{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The default response headers for this resource.  Never <jk>null</jk>.
	 */
	public Map<String,String> getDefaultResponseHeaders() {
		return defaultResponseHeaders;
	}

	/**
	 * Returns the converters associated with this resource at the class level.
	 *
	 * <p>
	 * Converters are used to 'convert' POJOs from one form to another before being passed of to the response handlers.
	 *
	 * <p>
	 * Converters at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#converters() @RestResource.converters()} annotation.
	 * 	<li>{@link RestContextBuilder#converters(Class...)}/{@link RestContextBuilder#converters(RestConverter...)} methods.
	 * </ul>
	 *
	 * @return The converters associated with this resource.  Never <jk>null</jk>.
	 */
	protected RestConverter[] getConverters() {
		return converters;
	}

	/**
	 * Returns the guards associated with this resource at the class level.
	 *
	 * <p>
	 * Guards are used to restrict access to resources.
	 *
	 * <p>
	 * Guards at the class level are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#guards() @RestResource.guards()} annotation.
	 * 	<li>{@link RestContextBuilder#guards(Class...)}/{@link RestContextBuilder#guards(RestGuard...)} methods.
	 * </ul>
	 *
	 * @return The guards associated with this resource.  Never <jk>null</jk>.
	 */
	protected RestGuard[] getGuards() {
		return guards;
	}

	/**
	 * Returns the response handlers associated with this resource.
	 *
	 * <p>
	 * Response handlers are used to convert POJOs returned by REST Java methods into actual HTTP responses.
	 *
	 * <p>
	 * Response handlers are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#responseHandlers() @RestResource.responseHandlers()} annotation.
	 * 	<li>{@link RestContextBuilder#responseHandlers(Class...)}/{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
	 * 		methods.
	 * </ul>
	 *
	 * @return The response handlers associated with this resource.  Never <jk>null</jk>.
	 */
	protected ResponseHandler[] getResponseHandlers() {
		return responseHandlers;
	}

	/**
	 * Returns the media type for the specified file name.
	 *
	 * <p>
	 * The list of MIME-type mappings can be augmented through the {@link RestContextBuilder#mimeTypes(String...)} method.
	 * See that method for a description of predefined MIME-type mappings.
	 *
	 * @param name The file name.
	 * @return The MIME-type, or <jk>null</jk> if it could not be determined.
	 */
	protected String getMediaTypeForName(String name) {
		return mimetypesFileTypeMap.getContentType(name);
	}

	/**
	 * Returns <jk>true</jk> if the specified path refers to a static file.
	 *
	 * <p>
	 * Static files are files pulled from the classpath and served up directly to the browser.
	 *
	 * <p>
	 * Static files are defined via one of the following:
	 * <ul>
	 * 	<li>{@link RestResource#staticFiles() @RestResource.staticFiles()} annotation.
	 * 	<li>{@link RestContextBuilder#staticFiles(Class, String)} method.
	 * </ul>
	 *
	 * @param p The URL path remainder after the servlet match.
	 * @return <jk>true</jk> if the specified path refers to a static file.
	 */
	protected boolean isStaticFile(String p) {
		return pathStartsWith(p, staticFilesPrefixes);
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestMethod @RestMethod} annotation.
	 *
	 * @return A map of Java method names to call method objects.
	 */
	protected Map<String,CallMethod> getCallMethods() {
		return callMethods;
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
	 * Returns <jk>true</jk> if this resource has any child resources associated with it.
	 *
	 * @return <jk>true</jk> if this resource has any child resources associated with it.
	 */
	protected boolean hasChildResources() {
		return ! childResources.isEmpty();
	}

	/**
	 * Returns the context of the child resource associated with the specified path.
	 *
	 * @param path The path of the child resource to resolve.
	 * @return The resolved context, or <jk>null</jk> if it could not be resolved.
	 */
	protected RestContext getChildResource(String path) {
		return childResources.get(path);
	}

	/**
	 * Returns the context path of the resource if it's specified via the {@link RestResource#contextPath()} setting
	 * on this or a parent resource.
	 *
	 * @return The {@link RestResource#contextPath()} setting value, or <jk>null</jk> if it's not specified.
	 */
	protected String getContextPath() {
		if (contextPath != null)
			return contextPath;
		if (parentContext != null)
			return parentContext.getContextPath();
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	// Utility methods
	//----------------------------------------------------------------------------------------------------

	/**
	 * Takes in an object of type T or a Class<T> and either casts or constructs a T.
	 */
	static final <T> T resolve(Object outer, Class<T> c, Object o, Object...cArgs) throws RestServletException {
		try {
			return ClassUtils.newInstanceFromOuter(outer, c, o, true, cArgs);
		} catch (Exception e) {
			throw new RestServletException("Exception occurred while constructing class ''{0}''", c).initCause(e);
		}
	}

	@Override /* BeanContextBuilder */
	public BeanSession createSession(BeanSessionArgs args) {
		throw new NoSuchMethodError();
	}

	@Override /* BeanContextBuilder */
	public BeanSessionArgs createDefaultSessionArgs() {
		throw new NoSuchMethodError();
	}
}
