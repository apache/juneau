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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Configurable properties on the {@link RestServlet} class.
 * <p>
 * Properties can be set on the {@link RestServlet} class using the {@link RestResource#properties} or {@link RestMethod#properties} annotations.
 * <p>
 * These properties can also be passed in as servlet init parameters or system properties.
 * <p>
 * Some of these properties are only applicable on the servlet class, and others can be specified on the servlet class or method.<br>
 * These distinctions are noted below.
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 */
public final class RestServletContext extends Context {

	/**
	 * <b>Configuration property:</b>  Enable header URL parameters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowHeaderParams"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query parameters.
	 * For example:  <js>"?Accept=text/json&amp;Content-Type=text/json"</js>
	 * <p>
	 * Parameter names are case-insensitive.
	 * <p>
	 * Useful for debugging REST interface using only a browser.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowHeaderParams = "RestServlet.allowHeaderParams";

	/**
	 * <b>Configuration property:</b>  Enable <js>"method"</js> URL parameter for specific HTTP methods.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowMethodParam"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>""</js>
	 * </ul>
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular GET request.
	 * For example:  <js>"?method=OPTIONS"</js>
	 * <p>
	 * Format is a comma-delimited list of HTTP method names that can be passed in as a method parameter.
	 * Parameter name is case-insensitive.
	 * Use "*" to represent all methods.
	 * For backwards compatibility, "true" also means "*".
	 * <p>
	 * Note that per the <a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * 	be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 * <p>
	 * Applicable to servlet class only.
	 * <p>
	 * Example: <js>"HEAD,OPTIONS"</js>
	 */
	public static final String REST_allowMethodParam = "RestServlet.allowMethodParam";

	/**
	 * <b>Configuration property:</b>  Enable <js>"body"</js> URL parameter.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.allowBodyParam"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"body"</js> URL parameter.
	 * For example:  <js>"?body={name:'John%20Smith',age:45}"</js>
	 * <p>
	 * Parameter name is case-insensitive.
	 * <p>
	 * Useful for debugging PUT and POST methods using only a browser.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowBodyParam = "RestServlet.allowBodyParam";

	/**
	 * <b>Configuration property:</b>  Render stack traces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.renderResponseStackTraces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * Render stack traces in HTTP response bodies when errors occur.
	 * <p>
	 * When enabled, Java stack traces will be rendered in the output response.
	 * Useful for debugging, although allowing stack traces to be rendered may cause security concerns.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_renderResponseStackTraces = "RestServlet.renderResponseStackTraces";

	/**
	 * <b>Configuration property:</b>  Use stack trace hashes.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.useStackTraceHashes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * 	made available through the {@link RestException#getOccurrence()} method.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_useStackTraceHashes = "RestServlet.useStackTraceHashes";

	/**
	 * <b>Configuration property:</b>  Default character encoding.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.defaultCharset"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"utf-8"</js>
	 * </ul>
	 * <p>
	 * The default character encoding for the request and response if not specified on the request.
	 * <p>
	 * Applicable to servlet class and methods.
	 */
	public static final String REST_defaultCharset = "RestServlet.defaultCharset";

	/**
	 * <b>Configuration property:</b>  Expected format of request parameters.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RestServlet.paramFormat"</js>
	 * 	<li><b>Data type:</b> <code>String</code>
	 * 	<li><b>Default:</b> <js>"UON"</js>
	 * </ul>
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"UON"</js> - URL-Encoded Object Notation.<br>
	 * 		This notation allows for request parameters to contain arbitrarily complex POJOs.
	 * 	<li><js>"PLAIN"</js> - Plain text.<br>
	 * 		This treats request parameters as plain text.<br>
	 * 		Only POJOs directly convertable from <l>Strings</l> can be represented in parameters when using this mode.
	 * </ul>
	 * <p>
	 * Note that the parameter value <js>"(foo)"</js> is interpreted as <js>"(foo)"</js> when using plain mode, but
	 * 	<js>"foo"</js> when using UON mode.
	 * <p>
	 * The format can also be specified per-parameter using the {@link FormData#format() @FormData.format()} and {@link Query#format() @Query.format()}
	 * 	annotations.
	 * <p>
	 * Applicable to servlet class and methods.
	 */
	public static final String REST_paramFormat = "RestServlet.paramFormat";

	//--------------------------------------------------------------------------------
	// Automatically added properties.
	//--------------------------------------------------------------------------------

	/**
	 * The request servlet path.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletPath()}
	 */
	public static final String REST_servletPath = "RestServlet.servletPath";

	/**
	 * The request servlet URI.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletURI()}
	 */
	public static final String REST_servletURI = "RestServlet.servletURI";

	/**
	 * The request servlet URI.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getRelativeServletURI()}
	 */
	public static final String REST_relativeServletURI = "RestServlet.relativeServletURI";

	/**
	 * The request URI path info.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getPathInfo()}
	 */
	public static final String REST_pathInfo = "RestServlet.pathInfo";

	/**
	 * The request URI.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getRequestURI()}
	 */
	public static final String REST_requestURI = "RestServlet.requestURI";

	/**
	 * The request method.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethod()}
	 */
	public static final String REST_method = "RestServlet.method";

	/**
	 * The localized servlet title.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletTitle()}
	 */
	public static final String REST_servletTitle = "RestServlet.servletTitle";

	/**
	 * The localized servlet description.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getServletDescription()}
	 */
	public static final String REST_servletDescription = "RestServlet.servletDescription";

	/**
	 * The localized method summary.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethodSummary()}
	 */
	public static final String REST_methodSummary = "RestServlet.methodSummary";

	/**
	 * The localized method description.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(org.apache.juneau.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerSession#getProperties()} and {@link ParserSession#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethodDescription()}
	 */
	public static final String REST_methodDescription = "RestServlet.methodDescription";

	final boolean allowHeaderParams, allowBodyParam, renderResponseStackTraces, useStackTraceHashes;
	final String defaultCharset, paramFormat;
	final Set<String> allowMethodParams;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public RestServletContext(ContextFactory cf) {
		super(cf);
		allowHeaderParams = cf.getProperty(REST_allowHeaderParams, boolean.class, true);
		allowBodyParam = cf.getProperty(REST_allowBodyParam, boolean.class, true);
		renderResponseStackTraces = cf.getProperty(REST_renderResponseStackTraces, boolean.class, false);
		useStackTraceHashes = cf.getProperty(REST_useStackTraceHashes, boolean.class, true);
		defaultCharset = cf.getProperty(REST_defaultCharset, String.class, "utf-8");
		paramFormat = cf.getProperty(REST_paramFormat, String.class, "");

		Set<String> s = new LinkedHashSet<String>();
		for (String m : StringUtils.split(cf.getProperty(REST_allowMethodParam, String.class, ""), ','))
			if (m.equals("true"))  // For backwards compatibility when this was a boolean field.
				s.add("*");
			else
				s.add(m.toUpperCase());
		allowMethodParams = Collections.unmodifiableSet(s);
	}
}
