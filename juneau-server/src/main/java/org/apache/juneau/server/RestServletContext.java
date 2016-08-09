/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;

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
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class RestServletContext extends Context {

	/**
	 * Allow header URL parameters ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query parameters.
	 * For example:  <js>"?Accept=text/json&Content-Type=text/json"</js>
	 * <p>
	 * Parameter names are case-insensitive.
	 * <p>
	 * Useful for debugging REST interface using only a browser.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowHeaderParams = "RestServlet.allowHeaderParams";

	/**
	 * Allow <js>"method"</js> URL parameter for specific HTTP methods (String, default=<js>""</js>, example=<js>"HEAD,OPTIONS"</js>).
	 * <p>
	 * When specified, the HTTP method can be overridden by passing in a <js>"method"</js> URL parameter on a regular GET request.
	 * For example:  <js>"?method=OPTIONS"</js>
	 * <p>
	 * Parameter name is case-insensitive.  Use "*" to represent all methods.  For backwards compatibility, "true" also means "*".
	 * <p>
	 * Note that per the <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">HTTP specification</a>, special care should
	 * 	be taken when allowing non-safe (POST, PUT, DELETE) methods to be invoked through GET requests.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowMethodParam = "RestServlet.allowMethodParam";

	/**
	 * Allow <js>"content"</js> URL parameter ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * When enabled, the HTTP body content on PUT and POST requests can be passed in as text using the <js>"content"</js> URL parameter.
	 * For example:  <js>"?content={name:'John%20Smith',age:45}"</js>
	 * <p>
	 * Parameter name is case-insensitive.
	 * <p>
	 * Useful for debugging PUT and POST methods using only a browser.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_allowContentParam = "RestServlet.allowContentParam";

	/**
	 * Render stack traces in HTTP response bodies when errors occur ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When enabled, Java stack traces will be rendered in the output response.
	 * Useful for debugging, although allowing stack traces to be rendered may cause security concerns.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_renderResponseStackTraces = "RestServlet.renderResponseStackTraces";

	/**
	 * Use stack trace hashes ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 * <p>
	 * Applicable to servlet class only.
	 */
	public static final String REST_useStackTraceHashes = "RestServlet.useStackTraceHashes";

	/**
	 * The default character encoding for the request and response if not specified on the request ({@link String}>, default=<js>"utf-8"</js>).
	 * <p>
	 * Applicable to servlet class and methods.
	 */
	public static final String REST_defaultCharset = "RestServlet.defaultCharset";

	/**
	 * The expected format of request parameters ({@link String}, default=<js>"UON"</js>).
	 * <p>
	 * Possible values:
	 * <ul class='spaced-list'>
	 * 	<li><js>"UON"</js> - URL-Encoded Object Notation.<br>
	 *			This notation allows for request parameters to contain arbitrarily complex POJOs.
	 * 	<li><js>"PLAIN"</js> - Plain text.<br>
	 *			This treats request parameters as plain text.<br>
	 *			Only POJOs directly convertable from <l>Strings</l> can be represented in parameters when using this mode.
	 * </ul>
	 * <p>
	 * Note that the parameter value <js>"(foo)"</js> is interpreted as <js>"(foo)"</js> when using plain mode, but
	 * 	<js>"foo"</js> when using UON mode.
	 * <p>
	 * The format can also be specified per-parameter using the {@link Param#format() @Param.format()} and {@link QParam#format() @QParam.format()}
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


	final boolean allowHeaderParams, allowContentParam, renderResponseStackTraces, useStackTraceHashes;
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
		allowContentParam = cf.getProperty(REST_allowContentParam, boolean.class, true);
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
