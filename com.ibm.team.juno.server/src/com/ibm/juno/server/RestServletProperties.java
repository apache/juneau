/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.server.annotation.*;

/**
 * Configurable properties for the {@link RestServlet} class.
 * <p>
 * Properties can be set on the {@link RestServlet} class using the {@link RestResource#properties} or {@link RestMethod#properties} annotations.
 * <p>
 * These properties can also be passed in as servlet init parameters.
 * <p>
 * These properties are only valid at the class level, not the method level.  Setting them on {@link RestMethod#properties()} has no effect.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RestServletProperties {

	/**
	 * Allow header URL parameters ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * When enabled, headers such as <js>"Accept"</js> and <js>"Content-Type"</js> to be passed in as URL query parameters.
	 * For example:  <js>"?Accept=text/json&Content-Type=text/json"</js>
	 * <p>
	 * Parameter names are case-insensitive.
	 * <p>
	 * Useful for debugging REST interface using only a browser.
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
	 */
	public static final String REST_allowContentParam = "RestServlet.allowContentParam";

	/**
	 * Render stack traces in HTTP response bodies when errors occur ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When enabled, Java stack traces will be rendered in the output response.
	 * Useful for debugging, although allowing stack traces to be rendered may cause security concerns.
	 */
	public static final String REST_renderResponseStackTraces = "RestServlet.renderResponseStackTraces";

	/**
	 * Use stack trace hashes ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * When enabled, the number of times an exception has occurred will be determined based on stack trace hashsums,
	 * made available through the {@link RestException#getOccurrence()} method.
	 */
	public static final String REST_useStackTraceHashes = "RestServlet.useStackTraceHashes";

	/**
	 * The default character encoding for the request and response if not specified on the request ({@link String}>, default=<js>"utf-8"</js>).
	 */
	public static final String REST_defaultCharset = "RestServlet.defaultCharset";


	//--------------------------------------------------------------------------------
	// Automatically added properties.
	//--------------------------------------------------------------------------------

	/**
	 * The request servlet path.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(com.ibm.juno.core.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerContext#getProperties()} and {@link ParserContext#getProperties()}.
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
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(com.ibm.juno.core.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerContext#getProperties()} and {@link ParserContext#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getPathInfo()}
	 */
	public static final String REST_pathInfo = "RestServlet.pathInfo";

	/**
	 * The request URI.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(com.ibm.juno.core.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerContext#getProperties()} and {@link ParserContext#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getRequestURI()}
	 */
	public static final String REST_requestURI = "RestServlet.requestURI";

	/**
	 * The request method.
	 * <p>
	 * Automatically added to properties return by {@link RestServlet#createRequestProperties(com.ibm.juno.core.ObjectMap, RestRequest)}
	 * 	and are therefore available through {@link SerializerContext#getProperties()} and {@link ParserContext#getProperties()}.
	 * <p>
	 * Equivalent to the value returned by {@link RestRequest#getMethod()}
	 */
	public static final String REST_method = "RestServlet.method";
}
