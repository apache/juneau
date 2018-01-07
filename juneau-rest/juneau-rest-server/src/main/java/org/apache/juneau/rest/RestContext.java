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
import org.apache.juneau.encoders.Encoder;
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
	 * Configuration property:  Allow body URL parameter.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowBodyParam.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js>
	 * URL parameter.
	 * <br>
	 * For example:  <js>"?body=(name='John%20Smith',age=45)"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property: 	{@link RestContext#REST_allowBodyParam}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#allowBodyParam()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowBodyParam(boolean)}
	 * 		</ul>
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging PUT and POST methods using only a browser.
	 *	</ul>
	 */
	public static final String REST_allowBodyParam = PREFIX + "allowBodyParam.b";
	
	/**
	 * Configuration property:  Allowed method parameters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowedMethodParams.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"HEAD,OPTIONS"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular
	 * GET request.
	 * <br>
	 * For example:  <js>"?method=OPTIONS"</js>
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowedMethodParams}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#allowedMethodParams()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowedMethodParams(String...)}
	 * 		</ul>
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
	 * Configuration property:  Allow header URL parameters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.allowHeaderParams.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query
	 * parameters.
	 * <br>
	 * For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_allowHeaderParams}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#allowHeaderParams()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#allowHeaderParams(boolean)}
	 * 		</ul>
	 * 	<li>Parameter name is case-insensitive.
	 * 	<li>Useful for debugging REST interface using only a browser.
	 *	</ul>
	 */
	public static final String REST_allowHeaderParams = PREFIX + "allowHeaderParams.b";
	
	/**
	 * Configuration property:  REST call handler.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.callHandler.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> RestCallHandler&gt; | RestCallHandler</code>
	 * 	<li><b>Default:</b>  {@link RestCallHandler}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * This class handles the basic lifecycle of an HTTP REST call.
	 * <br>Subclasses can be used to customize how these HTTP calls are handled.

	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_callHandler}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#callHandler()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#callHandler(Class)}
	 * 			<li>{@link RestContextBuilder#callHandler(RestCallHandler)} 
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_callHandler = PREFIX + "callHandler.o";

	/**
	 * Configuration property:  Children.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.children.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class | Object | RestChild&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Defines children of this resource.
	 *
	 * <p>
	 * A REST child resource is simply another servlet or object that is initialized as part of the parent resource and has a
	 * servlet path directly under the parent servlet path.
	 * <br>The main advantage to defining servlets as REST children is that you do not need to define them in the
	 * <code>web.xml</code> file of the web application.
	 * <br>This can cut down on the number of entries that show up in the <code>web.xml</code> file if you are defining
	 * large numbers of servlets.
	 *
	 * <p>
	 * Child resources must specify a value for {@link RestResource#path()} that identifies the subpath of the child resource
	 * relative to the parent path.
	 *
	 * <p>
	 * Child resources can be nested arbitrarily deep using this technique (i.e. children can also have children).
	 *
	 * <dl>
	 * 	<dt>Servlet initialization:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			A child resource will be initialized immediately after the parent servlet is initialized.
	 * 			The child resource receives the same servlet config as the parent resource.
	 * 			This allows configuration information such as servlet initialization parameters to filter to child
	 * 			resources.
	 * 		</p>
	 * 	</dd>
	 * 	<dt>Runtime behavior:</dt>
	 * 	<dd>
	 * 		<p>
	 * 			As a rule, methods defined on the <code>HttpServletRequest</code> object will behave as if the child
	 * 			servlet were deployed as a top-level resource under the child's servlet path.
	 * 			For example, the <code>getServletPath()</code> and <code>getPathInfo()</code> methods on the
	 * 			<code>HttpServletRequest</code> object will behave as if the child resource were deployed using the
	 * 			child's servlet path.
	 * 			Therefore, the runtime behavior should be equivalent to deploying the child servlet in the
	 * 			<code>web.xml</code> file of the web application.
	 * 		</p>
	 * 	</dd>
	 * </dl>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_children}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#children()}
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#child(String,Object)}
	 * 			<li>{@link RestContextBuilder#children(Class...)}
	 * 			<li>{@link RestContextBuilder#children(Object...)}
	 * 		</ul>
	 * 	<li>When defined as classes, instances are resolved using the registered {@link #REST_resourceResolver} which
	 * 		by default is {@link RestResourceResolverSimple} which requires the class have one of the following
	 * 		constructors:
	 * 		<ul>
	 * 			<li><code><jk>public</jk> T(RestContextBuilder)</code>
	 * 			<li><code><jk>public</jk> T()</code>
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_children = PREFIX + "children.lo";

	/**
	 * Configuration property:  Classpath resource finder. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.classpathResourceFinder.o"</js>
	 * 	<li><b>Data type:</b>  {@link ClasspathResourceFinder}
	 * 	<li><b>Default:</b>  {@link ClasspathResourceFinderBasic}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Used to retrieve localized files from the classpath.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_classpathResourceFinder}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#classpathResourceFinder()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(Class)}
	 * 			<li>{@link RestContextBuilder#classpathResourceFinder(ClasspathResourceFinder)}
	 * 		</ul>
	 * 	<li>
	 * 		The default value is {@link ClasspathResourceFinderBasic} which provides basic support for finding localized
	 * 		resources on the classpath and JVM working directory.
	 * 		<br>The {@link ClasspathResourceFinderRecursive} is another option that also recursively searches for resources
	 * 		up the parent class hierarchy.
	 * 		<br>Each of these classes can be extended to provide customized handling of resource retrieval.
	 * </ul>
	 */
	public static final String REST_classpathResourceFinder = PREFIX + "classpathResourceFinder.o";
	
	/**
	 * Configuration property:  Client version header.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.clientVersionHeader.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"X-Client-Version"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_clientVersionHeader}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#clientVersionHeader()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#clientVersionHeader(String)}
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_clientVersionHeader = PREFIX + "clientVersionHeader.s";

	/**
	 * Configuration property:  Resource context path. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.contextPath.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_contextPath}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#contextPath()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#contextPath(String)} 
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_contextPath = PREFIX + "contextPath.s";
	
	/**
	 * Configuration property:  Class-level response converters.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.converters.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;RestConverter | Class&lt;? <jk>extends</jk> RestConverter&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_converters}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#converters()}
	 * 			<li>{@link RestMethod#converters()}
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#converters(Class...)}
	 * 			<li>{@link RestContextBuilder#converters(RestConverter...)}
	 * 		</ul>
	 * 	<li>{@link RestConverter} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_converters = PREFIX + "converters.lo";

	/**
	 * Configuration property:  Default character encoding.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultCharset.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"utf-8"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultCharset}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultCharset()}
	 * 			<li>{@link RestMethod#defaultCharset()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultCharset(String)}
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_defaultCharset = PREFIX + "defaultCharset.s";
	
	/**
	 * Configuration property:  Default request headers.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultRequestHeaders.smo"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies default values for request headers.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultRequestHeaders}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultRequestHeaders()}
	 * 			<li>{@link RestMethod#defaultRequestHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultRequestHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultRequestHeaders(String...)}
	 * 		</ul>
	 * 	<li>Affects values returned by {@link RestRequest#getHeader(String)} when the header is not present on the request.
	 * 	<li>The most useful reason for this annotation is to provide a default <code>Accept</code> header when one is not
	 * 		specified so that a particular default {@link Serializer} is picked.
	 *	</ul>
	 */
	public static final String REST_defaultRequestHeaders = PREFIX + "defaultRequestHeaders.smo";

	/**
	 * Configuration property:  Default response headers.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.defaultResponseHeaders.omo"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  empty map
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 *	<p>
	 * Specifies default values for response headers.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_defaultResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#defaultResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#defaultResponseHeader(String,Object)}
	 * 			<li>{@link RestContextBuilder#defaultResponseHeaders(String...)}
	 * 		</ul>
	 * 	<li>This is equivalent to calling {@link RestResponse#setHeader(String, String)} programmatically in each of 
	 * 		the Java methods.
	 * 	<li>The header value will not be set if the header value has already been specified (hence the 'default' in the name).
	 *	</ul>
	 */
	public static final String REST_defaultResponseHeaders = PREFIX + "defaultResponseHeaders.omo";

	/**
	 * Configuration property:  Compression encoders. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.encoders.o"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class &lt;? <jk>extends</jk> Encoder&gt; | Encoder&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * These can be used to enable various kinds of compression (e.g. <js>"gzip"</js>) on requests and responses.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Servlet with automated support for GZIP compression</jc>
	 * 	<ja>@RestResource</ja>(encoders={GzipEncoder.<jk>class</jk>})
	 * 	<jk>public</jk> MyRestServlet <jk>extends</jk> RestServlet {
	 * 		...
	 * 	}
	 * </p>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_encoders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#encoders()} 
	 * 			<li>{@link RestMethod#encoders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#encoders(Class...)}
	 * 			<li>{@link RestContextBuilder#encoders(Encoder...)}
	 * 		</ul>
	 * 	<li>Instance classes must provide a public no-arg constructor, or a public constructor that takes in a
	 * 		{@link PropertyStore} object.
	 * 	<li>Instance class can be defined as an inner class of the REST resource class.
	 * </ul>
	 */
	public static final String REST_encoders = PREFIX + "encoders.lo";

	/**
	 * Configuration property:  Class-level guards.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.guards.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;RestGuard | Class&lt;? <jk>extends</jk> RestGuard&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_guards}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#guards()}
	 * 			<li>{@link RestMethod#guards()}
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#guards(Class...)}
	 * 			<li>{@link RestContextBuilder#guards(RestGuard...)}
	 * 		</ul>
	 * 	<li>{@link RestGuard} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_guards = PREFIX + "guards.lo";

	/**
	 * Configuration property:  REST info provider. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.infoProvider.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> RestInfoProvider&gt; | RestInfoProvider</code>
	 * 	<li><b>Default:</b>  {@link RestInfoProvider}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Class used to retrieve title/description/swagger information about a resource.
	 *
	 * <p>
	 * Subclasses can be used to customize the documentation on a resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_infoProvider}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#infoProvider()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#infoProvider(Class)}
	 * 			<li>{@link RestContextBuilder#infoProvider(RestInfoProvider)} 
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_infoProvider = PREFIX + "infoProvider.o";
	
	/**
	 * Configuration property:  REST logger.
	 * 
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.logger.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> RestLogger&gt; | RestLogger</code>
	 * 	<li><b>Default:</b>  {@link RestLogger.Normal}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the logger to use for logging.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_logger}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#logger()} 
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#logger(Class)}
	 * 			<li>{@link RestContextBuilder#logger(RestLogger)} 
	 * 		</ul>
	 * 	<li>The {@link RestLogger.Normal} logger can be used to provide basic error logging to the Java logger.
	 *	</ul>
	 */
	public static final String REST_logger = PREFIX + "logger.o";

	/**
	 * Configuration property:  The maximum allowed input size (in bytes) on HTTP requests.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.maxInput.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"100M"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Useful for alleviating DoS attacks by throwing an exception when too much input is received instead of resulting
	 * in out-of-memory errors which could affect system stability.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_maxInput}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#maxInput()}
	 * 			<li>{@link RestMethod#maxInput()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#maxInput(String)}
	 * 		</ul>
	 * 	<li>String value that gets resolved to a <jk>long</jk>.
	 * 	<li>Can be suffixed with any of the following representing kilobytes, megabytes, and gigabytes:  
	 * 		<js>'K'</js>, <js>'M'</js>, <js>'G'</js>.
	 * 	<li>A value of <js>"-1"</js> can be used to represent no limit.
	 *	</ul>
	 */
	public static final String REST_maxInput = PREFIX + "maxInput.s";
	
	/**
	 * Configuration property:  Messages. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.messages.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;MessageBundleLocation&gt;</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the location of the resource bundle for this class.
	 *
	 * <p>
	 * This annotation is used to provide localized messages for the following methods:
	 * <ul>
	 * 	<li>{@link RestRequest#getMessage(String, Object...)}
	 * 	<li>{@link RestContext#getMessages()}
	 * </ul>
	 *
	 * <p>
	 * Refer to the {@link MessageBundle} class for a description of the message key formats used in the properties file.
	 *
	 * <p>
	 * The value can be a relative path like <js>"nls/Messages"</js>, indicating to look for the resource bundle
	 * <js>"com.foo.sample.nls.Messages"</js> if the resource class is in <js>"com.foo.sample"</js>, or it can be an
	 * absolute path, like <js>"com.foo.sample.nls.Messages"</js>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_messages}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#messages()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#messages(String)},
	 * 			<li>{@link RestContextBuilder#messages(Class,String)}
	 * 			<li>{@link RestContextBuilder#messages(MessageBundleLocation)} 
	 * 		</ul>
	 * 	<li>Mappings are cumulative from parent to child.  
	 * </ul>
	 */
	public static final String REST_messages = PREFIX + "messages.lo";
	
	/**
	 * Configuration property:  MIME types. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.mimeTypes.ss"</js>
	 * 	<li><b>Data type:</b>  <code>Set&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Defines MIME-type file type mappings.
	 * 
	 * <p>
	 * Used for specifying the content type on file resources retrieved through the following methods:
	 * <ul>
	 * 	<li>{@link RestContext#resolveStaticFile(String)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String,boolean,MediaType)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String,boolean)}
	 * 	<li>{@link RestRequest#getClasspathReaderResource(String)}
	 * </ul>
	 * 
	 * <p>
	 * This list appends to the existing list provided by {@link ExtendedMimetypesFileTypeMap}.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_mimeTypes}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#mimeTypes()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#mimeTypes(String...)}
	 * 		</ul>
	 * 	<li>Values are .mime.types formatted entry string.
	 * 		<br>Example: <js>"image/svg+xml svg"</js>
	 * </ul>
	 */
	public static final String REST_mimeTypes = PREFIX + "mimeTypes.ss";

	/**
	 * Configuration property:  Java method parameter resolvers.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.paramResolvers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;RestParam | Class&lt;? <jk>extends</jk> RestParam&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_paramResolvers}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#paramResolvers()}
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#paramResolvers(Class...)}
	 * 			<li>{@link RestContextBuilder#paramResolvers(RestParam...)}
	 * 		</ul>
	 * 	<li>{@link RestParam} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_paramResolvers = PREFIX + "paramResolvers.lo";

	/**
	 * Configuration property:  Parsers. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.parsers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class &lt;? <jk>extends</jk> Parser&gt; | Parser&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level parsers to this resource.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_parsers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#parsers()} 
	 * 			<li>{@link RestMethod#parsers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#parsers(Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#parsers(Parser...)}
	 * 			<li>{@link RestContextBuilder#parsers(boolean,Parser...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_parsers = PREFIX + "parsers.lo";

	/**
	 * Configuration property:  HTTP part parser. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.partParser.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class &lt;? <jk>extends</jk> HttpPartParser&gt; | HttpPartParser</code>
	 * 	<li><b>Default:</b>  {@link UonPartParser}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartParser} to use for parsing headers, query/form parameters, and URI parts.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partParser}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partParser()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partParser(Class)}
	 * 			<li>{@link RestContextBuilder#partParser(HttpPartParser)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partParser = PREFIX + "partParser.o";

	/**
	 * Configuration property:  HTTP part serializer. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.partSerializer.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class &lt;? <jk>extends</jk> HttpPartSerializer&gt; | HttpPartSerializer</code>
	 * 	<li><b>Default:</b>  {@link SimpleUonPartSerializer}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the {@link HttpPartSerializer} to use for serializing headers, query/form parameters, and URI parts.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_partSerializer}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#partSerializer()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#partSerializer(Class)}
	 * 			<li>{@link RestContextBuilder#partSerializer(HttpPartSerializer)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_partSerializer = PREFIX + "partSerializer.o";

	/**
	 * Configuration property:  Resource path.   
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.path.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Identifies the URL subpath relative to the parent resource.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_path}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#path()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#path(String)} 
	 * 		</ul>
	 * 	<li>This annotation is ignored on top-level servlets (i.e. servlets defined in <code>web.xml</code> files).
	 * 		<br>Therefore, implementers can optionally specify a path value for documentation purposes.
	 * 	<li>Typically, this setting is only applicable to resources defined as children through the 
	 * 		{@link RestResource#children()} annotation.
	 * 		<br>However, it may be used in other ways (e.g. defining paths for top-level resources in microservices).
	 *	</ul>
	 */
	public static final String REST_path = PREFIX + "path.s";

	/**
	 * Configuration property:  Render response stack traces in responses.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.renderResponseStackTraces.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_renderResponseStackTraces}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#renderResponseStackTraces()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#renderResponseStackTraces(boolean)}
	 * 		</ul>
	 * 	<li>Useful for debugging, although allowing stack traces to be rendered may cause security concerns so use
	 * 		caution when enabling.
	 *	</ul>
	 */
	public static final String REST_renderResponseStackTraces = PREFIX + "renderResponseStackTraces.b";
	
	/**
	 * Configuration property:  REST resource resolver.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.resourceResolver.o"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? <jk>extends</jk> RestResourceResolver&gt; | RestResourceResolver</code>
	 * 	<li><b>Default:</b>  {@link RestResourceResolverSimple}
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 *	<p>
	 * The resolver used for resolving child resources.
	 * 
	 * <p>
	 * Can be used to provide customized resolution of REST resource class instances (e.g. resources retrieve from Spring).
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_resourceResolver}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#resourceResolver()} 
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#resourceResolver(Class)}
	 * 			<li>{@link RestContextBuilder#resourceResolver(RestResourceResolver)}
	 * 		</ul>
	 * 	<li>Unless overridden, resource resolvers are inherited from parent resources.
	 *	</ul>
	 */
	public static final String REST_resourceResolver = PREFIX + "resourceResolver.o";

	/**
	 * Configuration property:  Response handlers.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.responseHandlers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&lt;? <jk>extends</jk> ResponseHandler&gt;&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
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
	 * 	<li>Property:  {@link RestContext#REST_responseHandlers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#responseHandlers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#responseHandlers(Class...)}
					<li>{@link RestContextBuilder#responseHandlers(ResponseHandler...)}
				</ul>
	 * 	<li>{@link ResponseHandler} classes must have either a no-arg or {@link PropertyStore} argument constructors.
	 *	</ul>
	 */
	public static final String REST_responseHandlers = PREFIX + "responseHandlers.lo";

	/**
	 * Configuration property:  Serializers. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.serializers.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class &lt;? <jk>extends</jk> Serializer&gt; | Serializer&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Adds class-level serializers to this resource.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_serializers}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#serializers()} 
	 * 			<li>{@link RestMethod#serializers()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#serializers(Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Class...)}
	 * 			<li>{@link RestContextBuilder#serializers(Serializer...)}
	 * 			<li>{@link RestContextBuilder#serializers(boolean,Serializer...)}
	 * 		</ul>
	 * 	<li>When defined as a class, properties/transforms defined on the resource/method are inherited.
	 * 	<li>When defined as an instance, properties/transforms defined on the resource/method are NOT inherited.
	 * </ul>
	 */
	public static final String REST_serializers = PREFIX + "serializers.lo";

	/**
	 * Configuration property:  Static file response headers. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFileResponseHeaders.omo"</js>
	 * 	<li><b>Data type:</b>  <code>Map&lt;String,String&gt;</code>
	 * 	<li><b>Default:</b>  <code>{<js>'Cache-Control'</js>: <js>'max-age=86400, public</js>}</code>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Used to customize the headers on responses returned for statically-served files.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFileResponseHeaders}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFileResponseHeaders()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(boolean,Map)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeaders(String...)}
	 * 			<li>{@link RestContextBuilder#staticFileResponseHeader(String,String)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String REST_staticFileResponseHeaders = PREFIX + "staticFileResponseHeaders.omo";
	
	/**
	 * Configuration property:  Static file mappings. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.staticFiles.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;StaticFileMapping&gt;</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Used to define paths and locations of statically-served files such as images or HTML documents.
	 * 
	 * <p>
	 * Static files are found by calling {@link #getClasspathResource(String,Locale)} which uses the registered 
	 * {@link ClasspathResourceFinder} for locating files on the classpath (or other location).
	 * 
	 * <p>
	 * An example where this class is used is in the {@link RestResource#staticFiles} annotation:
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.foo.mypackage;
	 * 
	 * 	<ja>@RestResource</ja>(
	 * 		path=<js>"/myresource"</js>,
	 * 		staticFiles={<js>"htdocs:docs"</js>,<js>"styles:styles"</js>}
	 * 	)
	 * 	<jk>public class</jk> MyResource <jk>extends</jk> RestServletDefault {...}
	 * </p>
	 * 
	 * <p>
	 * In the example above, given a GET request to <l>/myresource/htdocs/foobar.html</l>, the servlet will attempt to find 
	 * the <l>foobar.html</l> file in the following ordered locations:
	 * <ol>
	 * 	<li><l>com.foo.mypackage.docs</l> package.
	 * 	<li><l>org.apache.juneau.rest.docs</l> package (since <l>RestServletDefault</l> is in <l>org.apache.juneau.rest</l>).
	 * 	<li><l>[working-dir]/docs</l> directory.
	 * </ol>
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_staticFiles}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#staticFiles()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#staticFiles(String)},
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(String,String)}
	 * 			<li>{@link RestContextBuilder#staticFiles(Class,String,String)} 
	 * 			<li>{@link RestContextBuilder#staticFiles(StaticFileMapping...)} 
	 * 		</ul>
	 * 	<li>Mappings are cumulative from parent to child.  
	 * 	<li>Child resources can override mappings made on parent resources.
	 * 	<li>The media type on the response is determined by the {@link org.apache.juneau.rest.RestContext#getMediaTypeForName(String)} method.
	 * 	<li>The resource finder is configured via the {@link #REST_classpathResourceFinder} setting, and can be
	 * 		overridden to provide customized handling of resource retrieval.
	 * 	<li>The {@link #REST_useClasspathResourceCaching} setting can be used to cache static files in memory
	 * 		to improve performance.
	 * </ul>
	 */
	public static final String REST_staticFiles = PREFIX + "staticFiles.lo";
	
	/**
	 * Configuration property:  Supported accept media types.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.supportedAcceptTypes.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the media types inferred from the serializers that identify what media types can be produced by the resource.
	 * 
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedAcceptTypes()} and the supported accept
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 *
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedAcceptTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedAcceptTypes()}
	 * 			<li>{@link RestMethod#supportedAcceptTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedAcceptTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_supportedAcceptTypes = PREFIX + "supportedAcceptTypes.ls";

	/**
	 * Configuration property:  Supported content media types.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.supportedContentTypes.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Overrides the media types inferred from the parsers that identify what media types can be consumed by the resource.
	 *
	 * <p>
	 * This affects the values returned by {@link RestRequest#getSupportedContentTypes()} and the supported content
	 * types shown in {@link RestInfoProvider#getSwagger(RestRequest)}.
	 * 
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_supportedContentTypes}
	 * 	<li>Annotations:
	 * 		<ul>
	 * 			<li>{@link RestResource#supportedContentTypes()}
	 * 			<li>{@link RestMethod#supportedContentTypes()}
	 * 		</ul> 
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,String...)}
	 * 			<li>{@link RestContextBuilder#supportedContentTypes(boolean,MediaType...)}
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_supportedContentTypes = PREFIX + "supportedContentTypes.ls";
	
	/**
	 * Configuration property:  Use classpath resource caching. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.useClasspathResourceCaching.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, resources retrieved via {@link RestContext#getClasspathResource(String, Locale)} (and related 
	 * methods) will be cached in memory to speed subsequent lookups.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useClasspathResourceCaching}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link RestResource#useClasspathResourceCaching()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useClasspathResourceCaching(boolean)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String REST_useClasspathResourceCaching = PREFIX + "useClasspathResourceCaching.b";

	/**
	 * Configuration property:  Use stack trace hashes.
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.useStackTraceHashes.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 *
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Property:  {@link RestContext#REST_useStackTraceHashes}
	 * 	<li>Annotations:  
	 * 		<ul>
	 * 			<li>{@link RestResource#useStackTraceHashes()}
	 * 		</ul>
	 * 	<li>Methods:  
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#useStackTraceHashes(boolean)}
	 * 		</ul>
	 *	</ul>
	 */
	public static final String REST_useStackTraceHashes = PREFIX + "useStackTraceHashes.b";
	
	/**
	 * Configuration property:  HTML Widgets. 
	 *
	 *	<h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RestContext.widgets.lo"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;Class&lt;? <jk>extends</jk> Widget&gt; | Widget&gt;</code>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * </ul>
	 * 
	 *	<h5 class='section'>Description:</h5>
	 * <p>
	 * Defines widgets that can be used in conjunction with string variables of the form <js>"$W{name}"</js>to quickly
	 * generate arbitrary replacement text.
	 * 
	 * Widgets resolve the following variables:
	 * <ul>
	 * 	<li><js>"$W{name}"</js> - Contents returned by {@link Widget#getHtml(RestRequest)}.
	 * 	<li><js>"$W{name.script}"</js> - Contents returned by {@link Widget#getScript(RestRequest)}.
	 * 		<br>The script contents are automatically inserted into the <xt>&lt;head/script&gt;</xt> section
	 * 			 in the HTML page.
	 * 	<li><js>"$W{name.style}"</js> - Contents returned by {@link Widget#getStyle(RestRequest)}.
	 * 		<br>The styles contents are automatically inserted into the <xt>&lt;head/style&gt;</xt> section
	 * 			 in the HTML page.
	 * </ul>
	 *
	 * <p>
	 * The following examples shows how to associate a widget with a REST method and then have it rendered in the links
	 * and aside section of the page:
	 *
	 * <p class='bcode'>
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
	 * 	<li>Property:  {@link RestContext#REST_widgets}
	 * 	<li>Annotations: 
	 * 		<ul>
	 * 			<li>{@link HtmlDoc#widgets()} 
	 * 		</ul>
	 * 	<li>Methods: 
	 * 		<ul>
	 * 			<li>{@link RestContextBuilder#widgets(Class...)}
	 * 			<li>{@link RestContextBuilder#widgets(Widget...)}
	 * 			<li>{@link RestContextBuilder#widgets(boolean,Widget...)}
	 * 		</ul>
	 * 	<li>Widgets are inherited from parent to child, but can be overridden by reusing the widget name.
	 * </ul>
	 */
	public static final String REST_widgets = PREFIX + "widgets.lo";
	
	
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
	private final Map<String,Object> 
		defaultRequestHeaders,
		defaultResponseHeaders,
		staticFileResponseHeaders;
	private final BeanContext beanContext;
	private final RestConverter[] converters;
	private final RestGuard[] guards;
	private final ResponseHandler[] responseHandlers;
	private final MimetypesFileTypeMap mimetypesFileTypeMap;
	private final StaticFileMapping[] staticFiles;
	private final String[] staticFilesPaths;
	private final MessageBundle msgs;
	private final ConfigFile configFile;
	private final VarResolver varResolver;
	private final Map<String,RestCallRouter> callRouters;
	private final Map<String,RestJavaMethod> callMethods;
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

	private final ClasspathResourceManager staticResourceManager;
	private final ConcurrentHashMap<Integer,AtomicInteger> stackTraceHashes = new ConcurrentHashMap<>();


	/**
	 * Constructor.
	 *
	 * @param builder The servlet configuration object.
	 * @throws Exception If any initialization problems were encountered.
	 */
	public RestContext(RestContextBuilder builder) throws Exception {
		super(builder.getPropertyStore());
		
		RestException _initException = null;
		
		try {
			ServletContext servletContext = builder.servletContext;

			this.resource = builder.resource;
			this.builder = builder;
			this.parentContext = builder.parentContext;
			
			PropertyStore ps = getPropertyStore().builder().add(builder.properties).build();
			Class<?> resourceClass = resource.getClass();

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
			
			Map<String,Object> _defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			_defaultRequestHeaders.putAll(getMapProperty(REST_defaultRequestHeaders, String.class));
			defaultRequestHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(_defaultRequestHeaders));
			
			defaultResponseHeaders = getMapProperty(REST_defaultResponseHeaders, Object.class);
			staticFileResponseHeaders = getMapProperty(REST_staticFileResponseHeaders, Object.class);	
			
			logger = getInstanceProperty(REST_logger, resource, RestLogger.class, RestLogger.NoOp.class, true, ps);

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
					UrlVar.class, 
					UrlEncodeVar.class, 
					WidgetVar.class
				)
				.build()
			;

			configFile = builder.configFile.getResolving(this.varResolver);
			
			properties = builder.properties;
			serializers = SerializerGroup.create().append(getInstanceArrayProperty(REST_serializers, Serializer.class, new Serializer[0], true, resource, ps)).build();
			parsers = ParserGroup.create().append(getInstanceArrayProperty(REST_parsers, Parser.class, new Parser[0], true, resource, ps)).build();
			partSerializer = getInstanceProperty(REST_partSerializer, HttpPartSerializer.class, SimpleUonPartSerializer.class, true, resource, ps);
			partParser = getInstanceProperty(REST_partSerializer, HttpPartParser.class, UonPartParser.class, true, resource, ps);
			encoders = new EncoderGroupBuilder().append(getInstanceArrayProperty(REST_encoders, Encoder.class, new Encoder[0], true, resource, ps)).build();
			beanContext = BeanContext.create().apply(ps).build();

			mimetypesFileTypeMap = new ExtendedMimetypesFileTypeMap();
			for (String mimeType : getArrayProperty(REST_mimeTypes, String.class))
				mimetypesFileTypeMap.addMimeTypes(mimeType);
			
			ClasspathResourceFinder rf = getInstanceProperty(REST_classpathResourceFinder, ClasspathResourceFinder.class, ClasspathResourceFinderBasic.class);
			boolean useClasspathResourceCaching = getProperty(REST_useClasspathResourceCaching, boolean.class, true);
			staticResourceManager = new ClasspathResourceManager(resourceClass, rf, useClasspathResourceCaching);

			supportedContentTypes = getListProperty(REST_supportedContentTypes, MediaType.class, serializers.getSupportedMediaTypes());
			supportedAcceptTypes = getListProperty(REST_supportedAcceptTypes, MediaType.class, parsers.getSupportedMediaTypes());
			
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
			
			fullPath = (builder.parentContext == null ? "" : (builder.parentContext.fullPath + '/')) + builder.path;
			
			this.childResources = Collections.synchronizedMap(new LinkedHashMap<String,RestContext>());  // Not unmodifiable on purpose so that children can be replaced.
			
			Map<String,Widget> _widgets = new LinkedHashMap<>();
			for (Widget w : getInstanceArrayProperty(REST_widgets, resource, Widget.class, new Widget[0], true, ps))
				_widgets.put(w.getName(), w);
			this.widgets = Collections.unmodifiableMap(_widgets);

			//----------------------------------------------------------------------------------------------------
			// Initialize the child resources.
			// Done after initializing fields above since we pass this object to the child resources.
			//----------------------------------------------------------------------------------------------------
			List<String> methodsFound = new LinkedList<>();   // Temporary to help debug transient duplicate method issue.
			Map<String,RestCallRouter.Builder> routers = new LinkedHashMap<>();
			Map<String,RestJavaMethod> _javaRestMethods = new LinkedHashMap<>();
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

			for (java.lang.reflect.Method method : resourceClass.getMethods()) {
				if (method.isAnnotationPresent(RestMethod.class)) {
					RestMethod a = method.getAnnotation(RestMethod.class);
					methodsFound.add(method.getName() + "," + a.name() + "," + a.path());
					try {
						if (! Modifier.isPublic(method.getModifiers()))
							throw new RestServletException("@RestMethod method {0}.{1} must be defined as public.", resourceClass.getName(), method.getName());

						RestJavaMethod sm = new RestJavaMethod(resource, method, this);
						String httpMethod = sm.getHttpMethod();

						// PROXY is a special case where a method returns an interface that we
						// can perform REST calls against.
						// We override the CallMethod.invoke() method to insert our logic.
						if ("PROXY".equals(httpMethod)) {

							final ClassMeta<?> interfaceClass = beanContext.getClassMeta(method.getGenericReturnType());
							final Map<String,Method> remoteableMethods = interfaceClass.getRemoteableMethods();
							if (remoteableMethods.isEmpty())
								throw new RestException(SC_INTERNAL_SERVER_ERROR, "Method {0} returns an interface {1} that doesn't define any remoteable methods.", getMethodSignature(method), interfaceClass.getReadableName());

							sm = new RestJavaMethod(resource, method, this) {

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
						throw new RestServletException("Problem occurred trying to serialize methods on class {0}, methods={1}", resourceClass.getName(), JsonSerializer.DEFAULT_LAX.serialize(methodsFound)).initCause(e);
					}
				}
			}

			for (Method m : ClassUtils.getAllMethods(resourceClass, true)) {
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

			Map<String,RestCallRouter> _callRouters = new LinkedHashMap<>();
			for (RestCallRouter.Builder crb : routers.values())
				_callRouters.put(crb.getHttpMethodName(), crb.build());
			this.callRouters = Collections.unmodifiableMap(_callRouters);

			// Initialize our child resources.
			resourceResolver = getInstanceProperty(REST_resourceResolver, resource, RestResourceResolver.class, parentContext == null ? RestResourceResolverSimple.class : parentContext.resourceResolver, true, this, ps);
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
					childBuilder = new RestContextBuilder(builder.inner, oc, this);
					r = resourceResolver.resolve(resource, oc, childBuilder);
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

			callHandler = getInstanceProperty(REST_callHandler, resource, RestCallHandler.class, RestCallHandler.class, true, this, ps);
			infoProvider = getInstanceProperty(REST_infoProvider, resource, RestInfoProvider.class, RestInfoProvider.class, true, this, ps);

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

	private static void addToRouter(Map<String, RestCallRouter.Builder> routers, String httpMethodName, RestJavaMethod cm) throws RestServletException {
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
	 * 	<li><code>$CO{arg1[,arg2...]}</code> - Coalesce variable. See {@link CoalesceVar}.
	 * 	<li><code>$CR{arg1[,arg2...]}</code> - Coalesce-and-recurse variable. See {@link CoalesceAndRecurseVar}.
	 * 	<li><code>$E{envVar[,defaultValue]}</code> - Environment variable. See {@link EnvVariablesVar}.
	 * 	<li><code>$F{path[,defaultValue]}</code> - File resource. See {@link FileVar}.
	 * 	<li><code>$I{name[,defaultValue]}</code> - Servlet init parameter. See {@link ServletInitParamVar}.
	 * 	<li><code>$IF{booleanArg,thenValue[,elseValue]}</code> - If/else variable. See {@link IfVar}.
	 * 	<li><code>$L{key[,args...]}</code> - Localized message. See {@link LocalizationVar}.
	 * 	<li><code>$RA{key1[,key2...]}</code> - Request attribute variable. See {@link RequestAttributeVar}.
	 * 	<li><code>$RF{key1[,key2...]}</code> - Request form-data variable. See {@link RequestFormDataVar}.
	 * 	<li><code>$RH{key1[,key2...]}</code> - Request header variable. See {@link RequestHeaderVar}.
	 * 	<li><code>$RP{key1[,key2...]}</code> - Request path variable. See {@link RequestPathVar}.
	 * 	<li><code>$RQ{key1[,key2...]}</code> - Request query parameter variable. See {@link RequestQueryVar}.
	 * 	<li><code>$R{key1[,key2...]}</code> - Request object variable. See {@link RequestVar}.
	 * 	<li><code>$S{systemProperty[,defaultValue]}</code> - System property. See {@link SystemPropertiesVar}.
	 * 	<li><code>$SA{contentType,key[,defaultValue]}</code> - Serialized request attribute. See {@link SerializedRequestAttrVar}.
	 * 	<li><code>$SW{stringArg(,pattern,thenValue)+[,elseValue]}</code> - Switch variable. See {@link SwitchVar}.
	 * 	<li><code>$U{uri}</code> - URI resolver. See {@link UrlVar}.
	 * 	<li><code>$UE{uriPart}</code> - URL-Encoder. See {@link UrlEncodeVar}.
	 * 	<li><code>$W{widgetName}</code> - HTML widget variable. See {@link WidgetVar}.
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
								staticFilesCache.put(pathInfo, new StreamResource(MediaType.forString(mediaType), responseHeaders, is));
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
	protected InputStream getClasspathResource(String name, Locale locale) throws IOException {
		return staticResourceManager.getStream(name, locale);
	}

	/**
	 * Same as {@link #getClasspathResource(String, Locale)}, but allows you to override the class used for looking
	 * up the classpath resource.
	 *
	 * @param baseClass 
	 * 	Overrides the default class to use for retrieving the classpath resource. 
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
	 * @param name The resource name.
	 * @param locale Optional locale.
	 * @return An input stream of the resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 */
	protected InputStream getClasspathResource(Class<?> baseClass, String name, Locale locale) throws IOException {
		return staticResourceManager.getStream(baseClass, name, locale);
	}

	/**
	 * Reads the input stream from {@link #getClasspathResource(String, Locale)} into a String.
	 *
	 * @param name The resource name.
	 * @param locale Optional locale.
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
	 * @param baseClass 
	 * 	Overrides the default class to use for retrieving the classpath resource. 
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
	 * @param name The resource name.
	 * @param locale Optional locale.
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
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale Optional locale.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getClasspathResource(Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		return getClasspathResource(null, c, mediaType, name, locale);
	}

	/**
	 * Same as {@link #getClasspathResource(Class, MediaType, String, Locale)}, except overrides the class used
	 * for retrieving the classpath resource.
	 * 
	 * @param baseClass 
	 * 	Overrides the default class to use for retrieving the classpath resource. 
	 * 	<br>If <jk>null<jk>, uses the REST resource class.
	 * @param c The class type of the POJO to create.
	 * @param mediaType The media type of the data in the stream (e.g. <js>"text/json"</js>)
	 * @param name The resource name (e.g. "htdocs/styles.css").
	 * @param locale Optional locale.
	 * @return The parsed resource, or <jk>null</jk> if the resource could not be found.
	 * @throws IOException
	 * @throws ServletException If the media type was unknown or the input could not be parsed into a POJO.
	 */
	public <T> T getClasspathResource(Class<?> baseClass, Class<T> c, MediaType mediaType, String name, Locale locale) throws IOException, ServletException {
		InputStream is = getClasspathResource(baseClass, name, locale);
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
	 * Defined by the {@link HtmlDoc#widgets()} annotation or {@link RestContextBuilder#widgets(Class...)} method.
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
	protected Map<String,Object> getDefaultRequestHeaders() {
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
	public Map<String,Object> getDefaultResponseHeaders() {
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
		return pathStartsWith(p, staticFilesPaths);
	}

	/**
	 * Returns the REST Java methods defined in this resource.
	 *
	 * <p>
	 * These are the methods annotated with the {@link RestMethod @RestMethod} annotation.
	 *
	 * @return A map of Java method names to call method objects.
	 */
	protected Map<String,RestJavaMethod> getCallMethods() {
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

	@Override /* BeanContextBuilder */
	public BeanSession createSession(BeanSessionArgs args) {
		throw new NoSuchMethodError();
	}

	@Override /* BeanContextBuilder */
	public BeanSessionArgs createDefaultSessionArgs() {
		throw new NoSuchMethodError();
	}
}
