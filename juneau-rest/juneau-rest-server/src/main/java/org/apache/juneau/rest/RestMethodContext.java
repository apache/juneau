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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.StringUtils.firstNonEmpty;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.httppart.HttpPartType.*;
import static org.apache.juneau.rest.RestContext.*;
import static org.apache.juneau.rest.util.RestUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.httppart.bean.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.internal.HttpUtils;
import org.apache.juneau.jsonschema.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.guards.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
@ConfigurableContext(nocache=true)
public class RestMethodContext extends BeanContext implements Comparable<RestMethodContext>  {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RestMethodContext";

	/**
	 * Configuration property:  Client version pattern matcher.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_clientVersion RESTMETHOD_clientVersion}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.clientVersion.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.clientVersion</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_CLIENTVERSION</c>
	 * 	<li><b>Default:</b>  empty string
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#clientVersion()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Specifies whether this method can be called based on the client version.
	 *
	 * <p>
	 * The client version is identified via the HTTP request header identified by
	 * {@link Rest#clientVersionHeader() @Rest(clientVersionHeader)} which by default is <js>"X-Client-Version"</js>.
	 *
	 * <p>
	 * This is a specialized kind of {@link RestMatcher} that allows you to invoke different Java methods for the same
	 * method/path based on the client version.
	 *
	 * <p>
	 * The format of the client version range is similar to that of OSGi versions.
	 *
	 * <p>
	 * In the following example, the Java methods are mapped to the same HTTP method and URL <js>"/foobar"</js>.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.
	 * 	// Note that this also matches 2.0.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> Object method1()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>)
	 * 	<jk>public</jk> Object method2()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is less than 1.1.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[0,1.1)"</js>)
	 * 	<jk>public</jk> Object method3()  {...}
	 * </p>
	 *
	 * <p>
	 * It's common to combine the client version with transforms that will convert new POJOs into older POJOs for
	 * backwards compatibility.
	 * <p class='bcode w800'>
	 * 	<jc>// Call this method if X-Client-Version is at least 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"2.0"</js>)
	 * 	<jk>public</jk> NewPojo newMethod()  {...}
	 *
	 * 	<jc>// Call this method if X-Client-Version is at least 1.1, but less than 2.0.</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/foobar"</js>, clientVersion=<js>"[1.1,2.0)"</js>, transforms={NewToOldPojoSwap.<jk>class</jk>})
	 * 	<jk>public</jk> NewPojo oldMethod() {
	 * 		<jk>return</jk> newMethod();
	 * 	}
	 *
	 * <p>
	 * Note that in the previous example, we're returning the exact same POJO, but using a transform to convert it into
	 * an older form.
	 * The old method could also just return back a completely different object.
	 * The range can be any of the following:
	 * <ul>
	 * 	<li><js>"[0,1.0)"</js> = Less than 1.0.  1.0 and 1.0.0 does not match.
	 * 	<li><js>"[0,1.0]"</js> = Less than or equal to 1.0.  Note that 1.0.1 will match.
	 * 	<li><js>"1.0"</js> = At least 1.0.  1.0 and 2.0 will match.
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_clientVersionHeader}
	 * </ul>
	 */
	public static final String RESTMETHOD_clientVersion = PREFIX + ".clientVersion.s";

	/**
	 * Configuration property:  Debug mode.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_debug RESTMETHOD_debug}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.debug.s"</js>
	 * 	<li><b>Data type:</b>  {@link org.apache.juneau.Enablement}
	 * 	<li><b>System property:</b>  <c>RestMethodContext.debug</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEBUG</c>
	 * 	<li><b>Default:</b>  {@link org.apache.juneau.Enablement#NEVER}
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#debug()}
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
	public static final String RESTMETHOD_debug = PREFIX + ".debug.s";

	/**
	 * Configuration property:  Default form data.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultFormData RESTMETHOD_defaultFormData}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultFormData.omo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultFormData</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTFORMDATA</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultFormData()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Specifies default values for form-data parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getFormData(String)} when the parameter is not present on the
	 * request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(method=<jsf>POST</jsf>, path=<js>"/*"</js>, defaultFormData={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@FormData</ja>(<js>"foo"</js>) String foo)  {...}
	 * </p>
	 */
	public static final String RESTMETHOD_defaultFormData = PREFIX + ".defaultFormData.omo";

	/**
	 * Configuration property:  Default query parameters.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_defaultQuery RESTMETHOD_defaultQuery}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.defaultQuery.omo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.defaultQuery</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_DEFAULTQUERY</c>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultQuery()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Specifies default values for query parameters.
	 *
	 * <p>
	 * Strings are of the format <js>"name=value"</js>.
	 *
	 * <p>
	 * Affects values returned by {@link RestRequest#getQuery(String)} when the parameter is not present on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, defaultQuery={<js>"foo=bar"</js>})
	 * 	<jk>public</jk> String doGet(<ja>@Query</ja>(<js>"foo"</js>) String foo)  {...}
	 * </p>
	 */
	public static final String RESTMETHOD_defaultQuery = PREFIX + ".defaultQuery.omo";

	/**
	 * Configuration property:  HTTP method name.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_httpMethod RESTMETHOD_httpMethod}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.httpMethod.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.httpMethod</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_HTTPMETHOD</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#method()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * REST method name.
	 *
	 * <p>
	 * Typically <js>"GET"</js>, <js>"PUT"</js>, <js>"POST"</js>, <js>"DELETE"</js>, or <js>"OPTIONS"</js>.
	 *
	 * <p>
	 * Method names are case-insensitive (always folded to upper-case).
	 *
	 * <p>
	 * Note that you can use {@link org.apache.juneau.http.HttpMethod} for constant values.
	 *
	 * <p>
	 * Besides the standard HTTP method names, the following can also be specified:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"*"</js>
	 * 		- Denotes any method.
	 * 		<br>Use this if you want to capture any HTTP methods in a single Java method.
	 * 		<br>The {@link org.apache.juneau.rest.annotation.Method @Method} annotation and/or {@link RestRequest#getMethod()} method can be used to
	 * 		distinguish the actual HTTP method name.
	 * 	<li>
	 * 		<js>""</js>
	 * 		- Auto-detect.
	 * 		<br>The method name is determined based on the Java method name.
	 * 		<br>For example, if the method is <c>doPost(...)</c>, then the method name is automatically detected
	 * 		as <js>"POST"</js>.
	 * 		<br>Otherwise, defaults to <js>"GET"</js>.
	 * 	<li>
	 * 		<js>"RRPC"</js>
	 * 		- Remote-proxy interface.
	 * 		<br>This denotes a Java method that returns an object (usually an interface, often annotated with the
	 * 		{@link Remote @Remote} annotation) to be used as a remote proxy using
	 * 		<c>RestClient.getRemoteInterface(Class&lt;T&gt; interfaceClass, String url)</c>.
	 * 		<br>This allows you to construct client-side interface proxies using REST as a transport medium.
	 * 		<br>Conceptually, this is simply a fancy <c>POST</c> against the url <js>"/{path}/{javaMethodName}"</js>
	 * 		where the arguments are marshalled from the client to the server as an HTTP body containing an array of
	 * 		objects, passed to the method as arguments, and then the resulting object is marshalled back to the client.
	 * 	<li>
	 * 		Anything else
	 * 		- Overloaded non-HTTP-standard names that are passed in through a <c>&amp;method=methodName</c> URL
	 * 		parameter.
	 * </ul>
	 */
	public static final String RESTMETHOD_httpMethod = PREFIX + ".httpMethod.s";

	/**
	 * Configuration property:  Method-level matchers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_matchers RESTMETHOD_matchers}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.matchers.lo"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.rest.RestMatcher}|Class&lt;{@link org.apache.juneau.rest.RestMatcher}&gt;&gt;</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#matchers()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Associates one or more {@link RestMatcher RestMatchers} with the specified method.
	 *
	 * <p>
	 * If multiple matchers are specified, <b>ONE</b> matcher must pass.
	 * <br>Note that this is different than guards where <b>ALL</b> guards needs to pass.
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
	 * 	<li class='link'>{@doc RestmMatchers}
	 * </ul>
	 */
	public static final String RESTMETHOD_matchers = PREFIX + ".matchers.lo";

	/**
	 * Configuration property:  Resource method paths.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_paths RESTMETHOD_paths}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.path.ls"</js>
	 * 	<li><b>Data type:</b>  <c>String[]</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.paths</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_PATHS</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#path()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#paths()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the URL subpath relative to the servlet class.
	 *
	 * <p>
	 * <ul class='notes'>
	 * 	<li>
	 * 		This method is only applicable for Java methods.
	 * 	<li>
	 * 		Slashes are trimmed from the path ends.
	 * 		<br>As a convention, you may want to start your path with <js>'/'</js> simple because it make it easier to read.
	 * </ul>
	 */
	public static final String RESTMETHOD_paths = PREFIX + ".paths.ls";

	/**
	 * Configuration property:  Priority.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_priority RESTMETHOD_priority}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.priority.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.priority</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_PRIORITY</c>
	 * 	<li><b>Default:</b>  <c>0</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#priority()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * URL path pattern priority.
	 *
	 * <p>
	 * To force path patterns to be checked before other path patterns, use a higher priority number.
	 *
	 * <p>
	 * By default, it's <c>0</c>, which means it will use an internal heuristic to determine a best match.
	 */
	public static final String RESTMETHOD_priority = PREFIX + ".priority.i";

	/**
	 * Configuration property:  Default request attributes.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_reqAttrs RESTMETHOD_reqAttrs}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.reqAttrs.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.reqAttrs</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_REQATTRS</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#reqAttrs()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Default request attributes.
	 *
	 * <p>
	 * Specifies default values for request attributes if they are not already set on the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, reqAttrs={<js>"Foo: bar"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqAttrs}
	 * </ul>
	 */
	public static final String RESTMETHOD_reqAttrs = PREFIX + ".reqAttrs.smo";

	/**
	 * Configuration property:  Default request headers.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.rest.RestMethodContext#RESTMETHOD_reqHeaders RESTMETHOD_reqHeaders}
	 * 	<li><b>Name:</b>  <js>"RestMethodContext.reqHeaders.smo"</js>
	 * 	<li><b>Data type:</b>  <c>Map&lt;String,Object&gt;</c>
	 * 	<li><b>System property:</b>  <c>RestMethodContext.reqHeaders</c>
	 * 	<li><b>Environment variable:</b>  <c>RESTMETHODCONTEXT_REQHEADERS</c>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#reqHeaders()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultAccept()}
	 * 			<li class='ja'>{@link org.apache.juneau.rest.annotation.RestMethod#defaultContentType()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * Default request headers.
	 *
	 * <p>
	 * Specifies default values for request headers if they're not passed in through the request.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Assume "text/json" Accept value when Accept not specified</jc>
	 * 	<ja>@RestMethod</ja>(method=<jsf>GET</jsf>, path=<js>"/*"</js>, reqHeaders={<js>"Accept: text/json"</js>})
	 * 	<jk>public</jk> String doGet()  {...}
	 * </p>
	 *
	 * <ul class='notes'>
	 * 	<li>
	 * 		Supports {@doc RestSvlVariables}
	 * 		(e.g. <js>"$S{mySystemProperty}"</js>).
	 * </ul>
	 *
	 * <ul class='seealso'>
	 * 	<li class='jf'>{@link RestContext#REST_reqHeaders}
	 * </ul>
	 */
	public static final String RESTMETHOD_reqHeaders = PREFIX + ".reqHeaders.smo";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String httpMethod;
	private final UrlPathMatcher[] pathMatchers;
	final RestMethodParam[] methodParams;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final Integer priority;
	private final RestContext context;
	private final BeanFactory beanFactory;
	final Method method;
	final MethodInvoker methodInvoker;
	final MethodInfo mi;
	final SerializerGroup serializers;
	final ParserGroup parsers;
	final EncoderGroup encoders;
	final HttpPartSerializer partSerializer;
	final HttpPartParser partParser;
	final JsonSchemaGenerator jsonSchemaGenerator;
	final Map<String,Object>
		reqHeaders,
		defaultQuery,
		defaultFormData;
	final OMap reqAttrs;
	final String defaultCharset;
	final long maxInput;
	final List<MediaType>
		supportedAcceptTypes,
		supportedContentTypes;
	final RestLogger callLogger;

	final Map<Class<?>,ResponseBeanMeta> responseBeanMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> headerPartMetas = new ConcurrentHashMap<>();
	final Map<Class<?>,ResponsePartMeta> bodyPartMetas = new ConcurrentHashMap<>();
	final ResponseBeanMeta responseMeta;

	final Map<Integer,AtomicInteger> statusCodes = new ConcurrentHashMap<>();

	final Enablement debug;
	final int hierarchyDepth;

	RestMethodContext(RestMethodContextBuilder b) throws ServletException {
		super(b.getPropertyStore());

		try {
			context = b.context;
			method = b.method;
			methodInvoker = new MethodInvoker(method, context.getMethodExecStats(method));
			mi = MethodInfo.of(method).accessible();
			PropertyStore ps = getPropertyStore();
			Object r = context.getResource();

			int _hierarchyDepth = 0;
			Class<?> sc = b.method.getDeclaringClass().getSuperclass();
			while (sc != null) {
				_hierarchyDepth++;
				sc = sc.getSuperclass();
			}
			hierarchyDepth = _hierarchyDepth;

			beanFactory = new BeanFactory(context.getBeanFactory(), r)
				.addBean(RestMethodContext.class, this)
				.addBean(Method.class, method);
			beanFactory.addBean(BeanFactory.class, beanFactory);

			serializers = createSerializers(r, beanFactory, ps);
			beanFactory.addBean(SerializerGroup.class, serializers);

			parsers = createParsers(r, beanFactory, ps);
			beanFactory.addBean(ParserGroup.class, parsers);

			String _httpMethod = getProperty(RESTMETHOD_httpMethod, String.class, null);
			if (_httpMethod == null)
				_httpMethod = HttpUtils.detectHttpMethod(method, true, "GET");
			if ("METHOD".equals(_httpMethod))
				_httpMethod = "*";
			httpMethod = _httpMethod.toUpperCase(Locale.ENGLISH);

			defaultCharset = getProperty(REST_defaultCharset, String.class, "utf-8");

			maxInput = StringUtils.parseLongWithSuffix(getProperty(REST_maxInput, String.class, "100M"));

			HttpPartParser hpp = context.getPartParser();
			if (hpp instanceof Parser) {
				Parser pp = (Parser)hpp;
				hpp = (HttpPartParser)pp.builder().apply(ps).build();
			}
			this.partParser = hpp;

			this.partSerializer = context.getPartSerializer();

			this.responseMeta = ResponseBeanMeta.create(mi, ps);

			boolean dotAll = b.dotAll;
			List<UrlPathMatcher> pathMatchers = new ArrayList<>();
			for (String p : getArrayProperty(RESTMETHOD_paths, String.class)) {
				if (dotAll && ! p.endsWith("/*"))
					p += "/*";
				pathMatchers.add(UrlPathMatcher.of(p));
			}
			if (pathMatchers.isEmpty()) {
				String p = HttpUtils.detectHttpPath(method, true);
				if (dotAll && ! p.endsWith("/*"))
					p += "/*";
				pathMatchers.add(UrlPathMatcher.of(p));
			}

			this.pathMatchers = pathMatchers.toArray(new UrlPathMatcher[pathMatchers.size()]);

			this.methodParams = context.findParams(mi, false, this.pathMatchers[this.pathMatchers.length-1]);

			this.converters = createConverters(r, beanFactory);

			AList<RestGuard> _guards = AList.of();
			_guards.a(createGuards(r, beanFactory));
			Set<String> rolesDeclared = getSetProperty(REST_rolesDeclared, String.class, null);
			Set<String> roleGuard = getSetProperty(REST_roleGuard, String.class, Collections.emptySet());

			for (String rg : roleGuard) {
				try {
					_guards.add(new RoleBasedRestGuard(rolesDeclared, rg));
				} catch (java.text.ParseException e1) {
					throw new ServletException(e1);
				}
			}
			this.guards = _guards.toArray(new RestGuard[_guards.size()]);

			List<RestMatcher> optionalMatchers = new LinkedList<>(), requiredMatchers = new LinkedList<>();
			for (RestMatcher matcher : createMatchers(r, beanFactory)) {
				if (matcher.mustMatch())
					requiredMatchers.add(matcher);
				else
					optionalMatchers.add(matcher);
			}
			String clientVersion = getProperty(RESTMETHOD_clientVersion, String.class, null);
			if (clientVersion != null)
				requiredMatchers.add(new ClientVersionMatcher(context.getClientVersionHeader(), mi));

			this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
			this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

			this.encoders = EncoderGroup
				.create()
				.append(IdentityEncoder.INSTANCE)
				.append(createEncoders(r, beanFactory))
				.build();

			this.jsonSchemaGenerator = JsonSchemaGenerator.create().apply(ps).build();

			Map<String,Object> _reqHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			_reqHeaders.putAll(getMapProperty(RESTMETHOD_reqHeaders, Object.class));

			OMap _reqAttrs = new OMap(context.getReqAttrs()).appendAll(getMapProperty(RESTMETHOD_reqAttrs, Object.class));

			Map<String,Object> _defaultQuery = new LinkedHashMap<>(getMapProperty(RESTMETHOD_defaultQuery, Object.class));

			Map<String,Object> _defaultFormData = new LinkedHashMap<>(getMapProperty(RESTMETHOD_defaultFormData, Object.class));

			Type[] pt = method.getGenericParameterTypes();
			Annotation[][] pa = method.getParameterAnnotations();
			for (int i = 0; i < pt.length; i++) {
				for (Annotation a : pa[i]) {
					if (a instanceof Header) {
						Header h = (Header)a;
						String def = joinnlFirstNonEmptyArray(h._default(), h.df());
						if (def != null) {
							try {
								_reqHeaders.put(firstNonEmpty(h.name(), h.n(), h.value()), parseAnything(def));
							} catch (ParseException e) {
								throw new ConfigException(e, "Malformed @Header annotation");
							}
						}
					} else if (a instanceof Query) {
						Query q = (Query)a;
						String def = joinnlFirstNonEmptyArray(q._default(), q.df());
						if (def != null) {
							try {
								_defaultQuery.put(firstNonEmpty(q.name(), q.n(), q.value()), parseAnything(def));
							} catch (ParseException e) {
								throw new ConfigException(e, "Malformed @Query annotation");
							}
						}
					} else if (a instanceof FormData) {
						FormData f = (FormData)a;
						String def = joinnlFirstNonEmptyArray(f._default(), f.df());
						if (def != null) {
							try {
								_defaultFormData.put(firstNonEmpty(f.name(), f.value(), f.n()), parseAnything(def));
							} catch (ParseException e) {
								throw new ConfigException(e, "Malformed @FormData annotation");
							}
						}
					}
				}
			}

			this.reqHeaders = Collections.unmodifiableMap(_reqHeaders);
			this.reqAttrs = _reqAttrs.unmodifiable();
			this.defaultQuery = Collections.unmodifiableMap(_defaultQuery);
			this.defaultFormData = Collections.unmodifiableMap(_defaultFormData);

			this.priority = getIntegerProperty(RESTMETHOD_priority, 0);

			this.supportedAcceptTypes = getListProperty(REST_produces, MediaType.class, serializers.getSupportedMediaTypes());
			this.supportedContentTypes = getListProperty(REST_consumes, MediaType.class, parsers.getSupportedMediaTypes());

			this.debug = context.getDebug(method);
			this.callLogger = context.getCallLogger();
		} catch (ServletException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private String joinnlFirstNonEmptyArray(String[]...s) {
		for (String[] ss : s)
			if (ss.length > 0)
				return joinnl(ss);
		return null;
	}

	ResponseBeanMeta getResponseBeanMeta(Object o) {
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

	/**
	 * Instantiates the result converters for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_converters} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}/{@link RestContextBuilder#converters(RestConverter...)}
	 * 			<li>{@link RestMethod#converters()}.
	 * 			<li>{@link Rest#converters()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createConverters()</> method that returns <c>{@link RestConverter}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestConverter[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The result converters for this REST resource method.
	 * @throws Exception If result converters could not be instantiated.
	 * @seealso #REST_converters
	 */
	protected RestConverter[] createConverters(Object resource, BeanFactory beanFactory) throws Exception {
		RestConverter[] x = getInstanceArrayProperty(REST_converters, RestConverter.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(RestConverter[].class, resource, "createConverters");
		if (x == null)
			x = beanFactory.getBean(RestConverter[].class).orElse(null);
		if (x == null)
			x = new RestConverter[0];
		return x;
	}

	/**
	 * Instantiates the guards for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_guards} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}/{@link RestContextBuilder#guards(RestGuard...)}
	 * 			<li>{@link RestMethod#guards()}.
	 * 			<li>{@link Rest#guards()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createGuards()</> method that returns <c>{@link RestGuard}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestGuard[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The guards for this REST resource method.
	 * @throws Exception If guards could not be instantiated.
	 * @seealso #REST_guards
	 */
	protected RestGuard[] createGuards(Object resource, BeanFactory beanFactory) throws Exception {
		RestGuard[] x = getInstanceArrayProperty(REST_guards, RestGuard.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(RestGuard[].class, resource, "createGuards");
		if (x == null)
			x = beanFactory.getBean(RestGuard[].class).orElse(null);
		if (x == null)
			x = new RestGuard[0];
		return x;
	}

	/**
	 * Instantiates the method matchers for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #RESTMETHOD_matchers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestMethod#matchers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createMatchers()</> method that returns <c>{@link RestMatcher}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link java.lang.reflect.Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>RestMatcher[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The method matchers for this REST resource method.
	 * @throws Exception If method matchers could not be instantiated.
	 * @seealso #RESTMETHOD_matchers
	 */
	protected RestMatcher[] createMatchers(Object resource, BeanFactory beanFactory) throws Exception {
		RestMatcher[] x = getInstanceArrayProperty(RESTMETHOD_matchers, RestMatcher.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(RestMatcher[].class, resource, "createMatchers", Method.class);
		if (x == null)
			x = beanFactory.getBean(RestMatcher[].class).orElse(null);
		if (x == null)
			x = new RestMatcher[0];
		return x;
	}

	/**
	 * Instantiates the encoders for this REST resource method.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_encoders} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}/{@link RestContextBuilder#encoders(Encoder...)}
	 * 			<li>{@link RestMethod#encoders()}.
	 * 			<li>{@link Rest#encoders()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createEncoders()</> method that returns <c>{@link Encoder}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link Method} - The Java method this context belongs to.
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Encoder[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The encoders for this REST resource method.
	 * @throws Exception If encoders could not be instantiated.
	 * @seealso #REST_encoders
	 */
	protected Encoder[] createEncoders(Object resource, BeanFactory beanFactory) throws Exception {
		Encoder[] x = getInstanceArrayProperty(REST_encoders, Encoder.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(Encoder[].class, resource, "createEncoders");
		if (x == null)
			x = beanFactory.getBean(Encoder[].class).orElse(null);
		if (x == null)
			x = new Encoder[0];
		return x;
	}

	/**
	 * Instantiates the serializers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_serializers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializers(Class...)}/{@link RestContextBuilder#serializers(Serializer...)}
	 * 			<li>{@link Rest#serializers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createSerializers()</> method that returns <c>{@link Serializer}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Serializer[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The serializers for this REST resource.
	 * @throws Exception If serializers could not be instantiated.
	 * @seealso #REST_serializers
	 */
	protected SerializerGroup createSerializers(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {
		Object x = getArrayProperty(REST_serializers, Object.class);
		if (x == null)
			x = beanFactory.createBeanViaMethod(Serializer[].class, resource, "createSerializers");
		if (x == null)
			x = beanFactory.createBeanViaMethod(Class[].class, resource, "createSerializers");
		if (x == null) {
			x = beanFactory.createBeanViaMethod(SerializerGroup.class, resource, "createSerializers");
			if (x != null)
				return (SerializerGroup)x;
		}
		if (x == null)
			x = beanFactory.getBean(Serializer[].class).orElse(null);
		if (x == null) {
			x = beanFactory.getBean(SerializerGroup.class).orElse(null);
			if (x != null)
				return (SerializerGroup)x;
		}
		if (x == null)
			x = new Serializer[0];
		return SerializerGroup
			.create()
			.append((Object[])x)
			.apply(ps)
			.build();
	}

	/**
	 * Instantiates the parsers for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Looks for {@link #REST_parsers} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parsers(Class...)}/{@link RestContextBuilder#parsers(Parser...)}
	 * 			<li>{@link Rest#parsers()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createParsers()</> method that returns <c>{@link Parser}[]</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates a <c>Parser[0]</c>.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @param ps The property store of this method.
	 * @return The parsers for this REST resource.
	 * @throws Exception If parsers could not be instantiated.
	 * @seealso #REST_parsers
	 */
	protected ParserGroup createParsers(Object resource, BeanFactory beanFactory, PropertyStore ps) throws Exception {
		Object x = getArrayProperty(REST_parsers, Object.class);
		if (x == null)
			x = beanFactory.createBeanViaMethod(Parser[].class, resource, "createParsers");
		if (x == null)
			x = beanFactory.createBeanViaMethod(Class[].class, resource, "createParsers");
		if (x == null) {
			x = beanFactory.createBeanViaMethod(ParserGroup.class, resource, "createParsers");
			if (x != null)
				return (ParserGroup)x;
		}
		if (x == null)
			x = beanFactory.getBean(Parser[].class).orElse(null);
		if (x == null) {
			x = beanFactory.getBean(ParserGroup.class).orElse(null);
			if (x != null)
				return (ParserGroup)x;
		}
		if (x == null)
			x = new Parser[0];
		return ParserGroup
			.create()
			.append((Object[])x)
			.apply(ps)
			.build();
	}

	/**
	 * Instantiates the HTTP part serializer for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartSerializer}.
	 * 	<li>Looks for {@link #REST_partSerializer} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}/{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 			<li>{@link Rest#partSerializer()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartSerializer()</> method that returns <c>{@link HttpPartSerializer}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The HTTP part serializer for this REST resource.
	 * @throws Exception If serializer could not be instantiated.
	 * @seealso #REST_partSerializer
	 */
	protected HttpPartSerializer createPartSerializer(Object resource, BeanFactory beanFactory) throws Exception {
		HttpPartSerializer x = null;
		if (resource instanceof HttpPartSerializer)
			x = (HttpPartSerializer)resource;
		if (x == null)
			x = getInstanceProperty(REST_partSerializer, HttpPartSerializer.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(HttpPartSerializer.class, resource, "createPartSerializer");
		if (x == null)
			x = beanFactory.getBean(HttpPartSerializer.class).orElse(null);
		if (x == null)
			x = new OpenApiSerializer(getPropertyStore());
		return x;
	}

	/**
	 * Instantiates the HTTP part parser for this REST resource.
	 *
	 * <p>
	 * Instantiates based on the following logic:
	 * <ul>
	 * 	<li>Returns the resource class itself is an instance of {@link HttpPartParser}.
	 * 	<li>Looks for {@link #REST_partParser} value set via any of the following:
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}/{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 			<li>{@link Rest#partParser()}.
	 * 		</ul>
	 * 	<li>Looks for a static or non-static <c>createPartParser()</> method that returns <c>{@link HttpPartParser}</c> on the
	 * 		resource class with any of the following arguments:
	 * 		<ul>
	 * 			<li>{@link RestContext}
	 * 			<li>{@link BeanFactory}
	 * 			<li>Any {@doc RestInjection injected beans}.
	 * 		</ul>
	 * 	<li>Resolves it via the bean factory registered in this context.
	 * 	<li>Instantiates an {@link OpenApiSerializer}.
	 * </ul>
	 *
	 * @param resource The REST resource object.
	 * @param beanFactory The bean factory to use for retrieving and creating beans.
	 * @return The HTTP part parser for this REST resource.
	 * @throws Exception If parser could not be instantiated.
	 * @seealso #REST_partParser
	 */
	protected HttpPartParser createPartParser(Object resource, BeanFactory beanFactory) throws Exception {
		HttpPartParser x = null;
		if (resource instanceof HttpPartParser)
			x = (HttpPartParser)resource;
		if (x == null)
			x = getInstanceProperty(REST_partParser, HttpPartParser.class, null, beanFactory);
		if (x == null)
			x = beanFactory.createBeanViaMethod(HttpPartParser.class, resource, "createPartParser");
		if (x == null)
			x = beanFactory.getBean(HttpPartParser.class).orElse(null);
		if (x == null)
			x = new OpenApiParser(getPropertyStore());
		return x;
	}

	ResponsePartMeta getResponseHeaderMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = headerPartMetas.get(c);
		if (pm == null) {
			ResponseHeader a = c.getAnnotation(ResponseHeader.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(HEADER, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			headerPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	ResponsePartMeta getResponseBodyMeta(Object o) {
		if (o == null)
			return null;
		Class<?> c = o.getClass();
		ResponsePartMeta pm = bodyPartMetas.get(c);
		if (pm == null) {
			ResponseBody a = c.getAnnotation(ResponseBody.class);
			if (a != null) {
				HttpPartSchema schema = HttpPartSchema.create(a);
				HttpPartSerializer serializer = createPartSerializer(schema.getSerializer(), serializers.getPropertyStore(), partSerializer);
				pm = new ResponsePartMeta(BODY, schema, serializer);
			}
			if (pm == null)
				pm = ResponsePartMeta.NULL;
			bodyPartMetas.put(c, pm);
		}
		if (pm == ResponsePartMeta.NULL)
			return null;
		return pm;
	}

	/**
	 * Returns <jk>true</jk> if this Java method has any guards or matchers.
	 */
	boolean hasGuardsOrMatchers() {
		return (guards.length != 0 || requiredMatchers.length != 0 || optionalMatchers.length != 0);
	}

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 */
	String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 */
	String getPathPattern() {
		return pathMatchers[0].toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified request object can call this method.
	 */
	boolean isRequestAllowed(RestRequest req) {
		for (RestGuard guard : guards) {
			req.setJavaMethod(method);
			if (! guard.isRequestAllowed(req))
				return false;
		}
		return true;
	}

	boolean matches(UrlPath urlPath) {
		for (UrlPathMatcher p : pathMatchers)
			if (p.match(urlPath) != null)
				return true;
		return false;
	}

	/**
	 * Identifies if this method can process the specified call.
	 *
	 * <p>
	 * To process the call, the following must be true:
	 * <ul>
	 * 	<li>Path pattern must match.
	 * 	<li>Matchers (if any) must match.
	 * </ul>
	 *
	 * @param call The call to check.
	 * @return
	 * 	One of the following values:
	 * 	<ul>
	 * 		<li><c>0</c> - Path doesn't match.
	 * 		<li><c>1</c> - Path matched but matchers did not.
	 * 		<li><c>2</c> - Matches.
	 * 	</ul>
	 */
	protected int match(RestCall call) {

		UrlPathMatch pm = matchPattern(call);

		if (pm == null)
			return 0;

		if (requiredMatchers.length == 0 && optionalMatchers.length == 0) {
			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		}

		try {
			RestRequest req = call.getRestRequest();
			RestResponse res = call.getRestResponse();

			req.init(this);
			res.init(this);

			// If the method implements matchers, test them.
			for (RestMatcher m : requiredMatchers)
				if (! m.matches(req))
					return 1;
			if (optionalMatchers.length > 0) {
				boolean matches = false;
				for (RestMatcher m : optionalMatchers)
					matches |= m.matches(req);
				if (! matches)
					return 1;
			}

			call.urlPathMatch(pm);  // Cache so we don't have to recalculate.
			return 2;
		} catch (Exception e) {
			throw new InternalServerError(e);
		}
	}

	private UrlPathMatch matchPattern(RestCall call) {
		UrlPathMatch pm = null;
		for (UrlPathMatcher pp : pathMatchers)
			if (pm == null)
				pm = pp.match(call.getUrlPath());
		return pm;
	}


	/**
	 * Workhorse method.
	 *
	 * @param pathInfo The value of {@link HttpServletRequest#getPathInfo()} (sorta)
	 */
	void invoke(RestCall call) throws Throwable {

		UrlPathMatch pm = call.getUrlPathMatch();
		if (pm == null)
			pm = matchPattern(call);

		if (pm == null)
			throw new NotFound();

		RestRequest req = call.getRestRequest();
		RestResponse res = call.getRestResponse();

		RequestPath rp = req.getPathMatch();
		for (Map.Entry<String,String> e : pm.getVars().entrySet())
			rp.put(e.getKey(), e.getValue());
		if (pm.getRemainder() != null)
			rp.remainder(pm.getRemainder());

		req.init(this);
		res.init(this);

		context.preCall(call);

		call.logger(callLogger);

		if (debug == ALWAYS) {
			call.debug(true);
		} else if (debug == NEVER) {
			call.debug(false);
		} else if (debug == CONDITIONAL) {
			boolean b = "true".equalsIgnoreCase(req.getHeader("X-Debug"));
			if (b) {
				call.debug(true);
			} else {
				call.debug(false);
			}
		}

		Object[] args = new Object[methodParams.length];
		for (int i = 0; i < methodParams.length; i++) {
			try {
				args[i] = methodParams[i].resolve(req, res);
			} catch (Exception e) {
				throw toHttpException(e, BadRequest.class, "Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.", methodParams[i].getParamType().name(), methodParams[i].getName(), methodParams[i].getType(), mi.getDeclaringClass().getFullName(), mi.getSimpleName());
			}
		}

		try {

			for (RestGuard guard : guards)
				if (! guard.guard(req, res))
					return;

			Object output;
			try {
				output = methodInvoker.invoke(context.getResource(), args);

				// Handle manual call to req.setDebug().
				Boolean debug = ObjectUtils.castOrNull(req.getAttribute("Debug"), Boolean.class);
				if (debug == Boolean.TRUE) {
					call.debug(true);
				} else if (debug == Boolean.FALSE) {
					call.debug(false);
				}

				if (res.getStatus() == 0)
					res.setStatus(200);
				if (! method.getReturnType().equals(Void.TYPE)) {
					if (output != null || ! res.getOutputStreamCalled())
						res.setOutput(output);
				}
			} catch (InvocationTargetException e) {
				Throwable e2 = e.getTargetException();		// Get the throwable thrown from the doX() method.
				res.setStatus(500);
				ResponsePartMeta rpm = getResponseBodyMeta(e2);
				ResponseBeanMeta rbm = getResponseBeanMeta(e2);
				if (rpm != null || rbm != null) {
					res.setOutput(e2);
					res.setResponseMeta(rbm);
				} else {
					throw e;
				}
			}

			context.postCall(call);

			if (res.hasOutput())
				for (RestConverter converter : converters)
					res.setOutput(converter.convert(req, res.getOutput()));

		} catch (IllegalArgumentException e) {
			throw new BadRequest(e,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				mi.toString(), mi.getFullName()
			);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestMethodContext o) {
		int c;

		c = priority.compareTo(o.priority);
		if (c != 0)
			return c;

		for (int i = 0; i < Math.min(pathMatchers.length, o.pathMatchers.length); i++) {
			c = pathMatchers[i].compareTo(o.pathMatchers[i]);
			if (c != 0)
				return c;
		}

		c = compare(o.hierarchyDepth, hierarchyDepth);
		if (c != 0)
			return c;

		c = compare(o.requiredMatchers.length, requiredMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.optionalMatchers.length, optionalMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.guards.length, guards.length);
		if (c != 0)
			return c;

		c = compare(method.getName(), o.method.getName());
		if (c != 0)
			return c;

		c = compare(method.getParameterCount(), o.method.getParameterCount());
		if (c != 0)
			return c;

		for (int i = 0; i < method.getParameterCount(); i++) {
			c = compare(method.getParameterTypes()[i].getName(), o.method.getParameterTypes()[i].getName());
			if (c != 0)
				return c;
		}

		c = compare(method.getReturnType().getName(), o.method.getReturnType().getName());
		if (c != 0)
			return c;

		return 0;
	}

	/**
	 * Bean property getter:  <property>serializers</property>.
	 *
	 * @return The value of the <property>serializers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public SerializerGroup getSerializers() {
		return serializers;
	}

	/**
	 * Bean property getter:  <property>parsers</property>.
	 *
	 * @return The value of the <property>parsers</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public ParserGroup getParsers() {
		return parsers;
	}

	/**
	 * Bean property getter:  <property>partSerializer</property>.
	 *
	 * @return The value of the <property>partSerializer</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartSerializer getPartSerializer() {
		return partSerializer;
	}

	/**
	 * Bean property getter:  <property>partParser</property>.
	 *
	 * @return The value of the <property>partParser</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public HttpPartParser getPartParser() {
		return partParser;
	}

	/**
	 * Returns the JSON-Schema generator applicable to this Java method.
	 *
	 * @return The JSON-Schema generator applicable to this Java method.
	 */
	public JsonSchemaGenerator getJsonSchemaGenerator() {
		return jsonSchemaGenerator;
	}

	Enablement getDebug() {
		return debug;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		return (o instanceof RestMethodContext) && eq(this, (RestMethodContext)o, (x,y)->x.method.equals(y.method));
	}

	@Override /* Object */
	public int hashCode() {
		return method.hashCode();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//-----------------------------------------------------------------------------------------------------------------

	static String[] resolveVars(VarResolver vr, String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++)
			out[i] = vr.resolve(in[i]);
		return out;
	}

	static HttpPartSerializer createPartSerializer(Class<? extends HttpPartSerializer> c, PropertyStore ps, HttpPartSerializer _default) {
		HttpPartSerializer hps = castOrCreate(HttpPartSerializer.class, c, true, ps);
		return hps == null ? _default : hps;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a("RestMethodContext", new DefaultFilteringOMap()
				.a("defaultFormData", defaultFormData)
				.a("defaultQuery", defaultQuery)
				.a("reqHeaders", reqHeaders)
				.a("httpMethod", httpMethod)
				.a("priority", priority)
			);
	}
}
