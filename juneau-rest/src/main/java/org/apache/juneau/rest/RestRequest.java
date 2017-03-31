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

import static java.util.Collections.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.encoders.Encoder;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * Represents an HTTP request for a REST resource.
 * <p>
 * Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 * <p>
 * For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * 	following methods return the following values....
 * <table class='styled'>
 * 	<tr><th>Method</th><th>Value</th></tr>
 * 	<tr><td>{@code getContextPath()}</td><td>{@code /contextRoot}</td></tr>
 * 	<tr><td>{@code getPathInfo()}</td><td>{@code /foo}</td></tr>
 * 	<tr><td>{@code getPathTranslated()}</td><td>{@code path-to-deployed-war-on-filesystem/foo}</td></tr>
 * 	<tr><td>{@code getQueryString()}</td><td>{@code bar=baz}</td></tr>
 * 	<tr><td>{@code getRequestURI()}</td><td>{@code /contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getRequestURL()}</td><td>{@code http://localhost:9080/contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getServletPath()}</td><td>{@code /servletPath}</td></tr>
 * </table>
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#TOC">REST Servlet API</a> for information about using this class.
 */
@SuppressWarnings("unchecked")
public final class RestRequest extends HttpServletRequestWrapper {

	private final RestContext context;

	private final String method;
	private String pathRemainder;
	private byte[] body;
	private Method javaMethod;
	private ObjectMap properties;
	private SerializerGroup serializerGroup;
	private ParserGroup parserGroup;
	private EncoderGroup encoders;
	private Encoder encoder;
	private int contentLength;
	private final boolean debug;
	private UrlEncodingParser urlEncodingParser;   // The parser used to parse URL attributes and parameters (beanContext also used to parse headers)
	private BeanSession beanSession;
	private VarResolverSession varSession;
	private final Map<String,String[]> queryParams;
	private final Map<String,String> defaultServletHeaders;
	private Map<String,String> defaultMethodHeaders, overriddenHeaders, overriddenQueryParams, overriddenFormDataParams, pathParameters;
	private boolean isPost;
	private String servletURI, relativeServletURI;
	private String charset, defaultCharset;
	private ObjectMap headers;
	private ConfigFile cf;
	private Swagger swagger, fileSwagger;
	private String pageTitle, pageText, pageLinks;

	/**
	 * Constructor.
	 */
	RestRequest(RestContext context, HttpServletRequest req) throws ServletException {
		super(req);
		this.context = context;

		try {
			isPost = req.getMethod().equalsIgnoreCase("POST");

			// If this is a POST, we want to parse the query parameters ourselves to prevent
			// the servlet code from processing the HTTP body as URL-Encoded parameters.
			if (isPost)
				queryParams = context.getUrlEncodingParser().parseIntoSimpleMap(getQueryString());
			else {
				queryParams = req.getParameterMap();
			}

			// Get the HTTP method.
			// Can be overridden through a "method" GET attribute.
			String _method = super.getMethod();

			String m = getQueryParameter("method");
			if (context.allowMethodParam(m))
				_method = m;

			method = _method;

			if (context.isAllowBodyParam()) {
				String b = getQueryParameter("body");
				if (b != null) {
					setHeader("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
					this.body = b.getBytes(IOUtils.UTF8);
				}
			}

			defaultServletHeaders = context.getDefaultRequestHeaders();

			debug = "true".equals(getQueryParameter("debug", "false")) || "true".equals(getHeader("Debug", "false"));

		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/*
	 * Called from RestServlet after a match has been made but before the guard or method invocation.
	 */
	@SuppressWarnings("hiding")
	final void init(Method javaMethod, String pathRemainder, ObjectMap properties, Map<String,String> mDefaultRequestHeaders, String defaultCharset, SerializerGroup mSerializers, ParserGroup mParsers, UrlEncodingParser mUrlEncodingParser, EncoderGroup encoders, String pageTitle, String pageText, String pageLinks) {
		this.javaMethod = javaMethod;
		this.pathRemainder = pathRemainder;
		this.properties = properties;
		this.defaultMethodHeaders = mDefaultRequestHeaders;
		this.serializerGroup = mSerializers;
		this.parserGroup = mParsers;
		this.urlEncodingParser = mUrlEncodingParser;
		this.beanSession = urlEncodingParser.getBeanContext().createSession();
		this.defaultCharset = defaultCharset;
		this.encoders = encoders;
		this.pageTitle = pageTitle;
		this.pageText = pageText;
		this.pageLinks = pageLinks;

		if (debug) {
			String msg = ""
				+ "\n=== HTTP Request (incoming) ===================================================="
				+ toString()
				+ "\n=== END ========================================================================";
			context.getLogger().log(Level.WARNING, msg);
		}
	}

	/**
	 * Returns a string of the form <js>"HTTP method-name full-url"</js>
	 *
	 * @return A description of the request.
	 */
	public String getDescription() {
		String qs = getQueryString();
		return "HTTP " + getMethod() + " " + getRequestURI() + (qs == null ? "" : "?" + qs);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Servlet calls this method to initialize the properties.
	 */
	RestRequest setProperties(ObjectMap properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Retrieve the properties active for this request.
	 * <p>
	 * These properties can be modified by the request.
	 *
	 * @return The properties active for this request.
	 */
	public ObjectMap getProperties() {
		return this.properties;
	}


	//--------------------------------------------------------------------------------
	// Headers
	//--------------------------------------------------------------------------------

	/**
	 * Sets a request header value.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public void setHeader(String name, Object value) {
		if (overriddenHeaders == null)
			overriddenHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		overriddenHeaders.put(name, StringUtils.toString(value));
	}


	/**
	 * Returns the specified header value, or <jk>null</jk> if the header doesn't exist.
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getHeader(String name) {
		return getHeader(name, (String)null);
	}

	/**
	 * Returns the specified header value, or a default value if the header doesn't exist.
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &HeaderName=x} in the URL query string.
	 *
	 * @param name The HTTP header name.
	 * @param def The default value to return if the header value isn't found.
	 * @return The header value, or the default value if the header isn't present.
	 */
	public String getHeader(String name, String def) {
		String h = getOverriddenHeader(name);
		if (h != null)
			return h;
		h = super.getHeader(name);
		if (h != null && ! h.isEmpty())
			return h;
		if (defaultMethodHeaders != null) {
			h = defaultMethodHeaders.get(name);
			if (h != null)
				return h;
		}
		h = defaultServletHeaders.get(name);
		if (h != null)
			return h;
		return def;
	}

	/**
	 * Returns the specified header value converted to a POJO.
	 * <p>
	 * The type can be any POJO type convertable from a <code>String</code>
	 * (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertable From Strings</a>).
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myheader = req.getHeader(<js>"My-Header"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse a UUID.</jc>
	 * 	UUID myheader = req.getHeader(<js>"My-Header"</js>, UUID.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The HTTP header name.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, Class<T> type) {
		String h = getHeader(name);
		return beanSession.convertToType(h, type);
	}

	/**
	 * Same as {@link #getHeader(String, Class)} but returns a default value if not found.
	 *
	 * @param name The HTTP header name.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, T def, Class<T> type) {
		String h = getHeader(name);
		if (h == null)
			return def;
		return beanSession.convertToType(h, type);
	}

	/**
	 * Returns the specified header value converted to a POJO.
	 * <p>
	 * The type can be any POJO type convertable from a <code>String</code>
	 * (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertable From Strings</a>).
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myheader = req.getHeader(<js>"My-Header"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The HTTP header name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, Type type, Type...args) {
		String h = getHeader(name);
		return (T)beanSession.convertToType(null, h, beanSession.getClassMeta(type, args));
	}

	/**
	 * Returns all the request headers as an {@link ObjectMap}.
	 * <p>
	 * Altering entries in this map does not alter headers in the underlying request.
	 *
	 * @return The request headers.  Never <jk>null</jk>.
	 */
	public ObjectMap getHeaders() {
		if (headers == null) {
			headers = new ObjectMap();
			for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
				String key = e.nextElement();
				headers.put(key, getHeader(key));
			}
		}
		return headers;
	}

	@Override /* ServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String h = getOverriddenHeader(name);
		if (h != null)
			return enumeration(singleton(h));
		return super.getHeaders(name);
	}

	/**
	 * Returns the <code>Content-Type</code> header value on the request, stripped
	 * 	of any parameters such as <js>";charset=X"</js>.
	 * <p>
	 * Example: <js>"text/json"</js>.
	 * <p>
	 * If the content type is not specified, and the content is specified via a
	 * 	<code>&amp;body</code> query parameter, the content type is assumed to be
	 * 	<js>"text/uon"</js>.  Otherwise, the content type is assumed to be <js>"text/json"</js>.
	 *
	 * @return The <code>Content-Type</code> media-type header value on the request.
	 */
	public MediaType getMediaType() {
		String cm = getHeader("Content-Type");
		if (cm == null) {
			if (body != null)
				return MediaType.UON;
			return MediaType.JSON;
		}
		return MediaType.forString(cm);
	}

	/**
	 * Returns the <code>Time-Zone</code> header value on the request if there is one.
	 * <p>
	 * Example: <js>"GMT"</js>.
	 *
	 * @return The <code>Time-Zone</code> header value on the request, or <jk>null</jk> if not present.
	 */
	public TimeZone getTimeZone() {
		String tz = getHeader("Time-Zone");
		if (tz != null)
			return TimeZone.getTimeZone(tz);
		return null;
	}

	/**
	 * Returns the media types that are valid for <code>Content-Type</code> headers on the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<MediaType> getSupportedMediaTypes() {
		return parserGroup.getSupportedMediaTypes();
	}

	/**
	 * Sets the charset to expect on the request body.
	 */
	@Override /* ServletRequest */
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}

	/**
	 * Returns the charset specified on the <code>Content-Type</code> header, or <js>"UTF-8"</js> if not specified.
	 */
	@Override /* ServletRequest */
	public String getCharacterEncoding() {
		if (charset == null) {
			// Determine charset
			// NOTE:  Don't use super.getCharacterEncoding() because the spec is implemented inconsistently.
			// Jetty returns the default charset instead of null if the character is not specified on the request.
			String h = getHeader("Content-Type");
			if (h != null) {
				int i = h.indexOf(";charset=");
				if (i > 0)
					charset = h.substring(i+9).trim();
			}
			if (charset == null)
				charset = defaultCharset;
			if (! Charset.isSupported(charset))
				throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header ''Content-Type'': ''{0}''", h);
		}
		return charset;
	}

	@Override /* ServletRequest */
	public Locale getLocale() {
		String h = getOverriddenHeader("Accept-Language");
		if (h != null) {
			MediaRange[] mr = MediaRange.parse(h);
			if (mr.length > 0)
				return toLocale(mr[0].getMediaType().getType());
		}
		return super.getLocale();
	}

	@Override /* ServletRequest */
	public Enumeration<Locale> getLocales() {
		String h = getOverriddenHeader("Accept-Language");
		if (h != null) {
			MediaRange[] mr = MediaRange.parse(h);
			if (mr.length > 0) {
				List<Locale> l = new ArrayList<Locale>(mr.length);
				for (MediaRange r : mr)
					l.add(toLocale(r.getMediaType().getType()));
				return enumeration(l);
			}
		}
		return super.getLocales();
	}


	//--------------------------------------------------------------------------------
	// Query parameters
	//--------------------------------------------------------------------------------

	/**
	 * Sets a request query parameter value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void setQueryParameter(String name, Object value) {
		if (overriddenQueryParams == null)
			overriddenQueryParams = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		overriddenQueryParams.put(name, value == null ? null : value.toString());
	}

	/**
	 * Returns a query parameter value.
	 * <p>
	 * Same as {@link #getParameter(String)} except only looks in the URL string, not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 *
	 * @param name The URL parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter not specified or has no value (e.g. <js>"&amp;foo"</js>.
	 */
	public String getQueryParameter(String name) {
		String s = null;
		if (overriddenQueryParams != null)
			s = overriddenQueryParams.get(name);
		if (s != null)
			return s;
		String[] v = queryParams.get(name);
		if (v == null || v.length == 0)
			return null;
		if (v.length == 1 && v[0] != null && v[0].isEmpty()) {
			// Fix for behavior difference between Tomcat and WAS.
			// getParameter("foo") on "&foo" in Tomcat returns "".
			// getParameter("foo") on "&foo" in WAS returns null.
			if (queryParams.containsKey(name))
				return null;
		}
		return v[0];
	}

	/**
	 * Same as {@link #getQueryParameter(String)} but returns the specified default value if the query parameter was not specified.
	 *
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter not specified or has no value (e.g. <js>"&amp;foo"</js>.
	 */
	public String getQueryParameter(String name, String def) {
		String s = getQueryParameter(name);
		return s == null ? def : s;
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = req.getQueryParameter(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = req.getQueryParameter(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = req.getQueryParameter(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Class<T> type) throws ParseException {
		return getQueryParameter(name, beanSession.getClassMeta(type));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Class)} except returns a default value if not found.
	 *
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, T def, Class<T> type) throws ParseException {
		return getQueryParameter(name, def, beanSession.getClassMeta(type));
	}

	/**
	 * Returns the specified query parameter value converted to a POJO.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying servlet API to load and parse the request body.
	 * <p>
	 * Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	Listt&lt;String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	Listt&lt;List&lt;String&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getQueryParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Type type, Type...args) throws ParseException {
		return (T)getQueryParameter(name, beanSession.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Class)} except returns a default value if not found.
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Object def, Type type, Type...args) throws ParseException {
		return (T)getQueryParameter(name, def, getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Class)} except for use on multi-part parameters
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js> instead of <js>"&amp;key=(1,2,3)"</js>).
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The query parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameters(String name, Class<T> c) throws ParseException {
		return getQueryParameters(name, beanSession.getClassMeta(c));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Type, Type...)} except for use on multi-part parameters
	 * (e.g. <js>"&amp;key=1&amp;key=2&amp;key=3"</js> instead of <js>"&amp;key=(1,2,3)"</js>).
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The query parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameters(String name, Type type, Type...args) throws ParseException {
		return (T)getQueryParameters(name, getBeanSession().getClassMeta(type, args));
	}

	/**
	 * Returns the list of all query parameters with the specified name.
	 * <p>
	 * Same as {@link #getParameterValues(String)} except only looks in the URL string, not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve parameters without triggering the underlying servlet API to load and parse the request body.
	 *
	 * @param name
	 * @return the list of query parameters, or <jk>null</jk> if the parameter does not exist.
	 */
	public String[] getQueryParameters(String name) {
		return queryParams.get(name);
	}

	/**
	 * Returns <jk>true</jk> if the query parameters on this request contains the specified entry.
	 * <p>
	 * Note that this returns <jk>true</jk> even if the value is set to null (e.g. <js>"?key"</js>).
	 * <p>
	 * This method can be used to check the existence of a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The URL parameter name.
	 * @return <jk>true</jk> if the URL parameters on this request contains the specified entry.
	 */
	public boolean hasQueryParameter(String name) {
		return queryParams.containsKey(name);
	}

	/**
	 * Returns <jk>true</jk> if the request contains any of the specified query parameters.
	 *
	 * @param params The list of parameters to check for.
	 * @return <jk>true</jk> if the request contains any of the specified query parameters.
	 */
	public boolean hasAnyQueryParameters(String...params) {
		for (String p : params)
			if (hasQueryParameter(p))
				return true;
		return false;
	}

	/**
	 * Equivalent to {@link #getParameterMap()}, but only looks for query parameters in the URL, not form posts.
	 * <p>
	 * This method can be used to retrieve query parameters without triggering the underlying servlet API to load and parse the request body.
	 * <p>
	 * This object is modifiable.
	 *
	 * @return The query parameters as a modifiable map.
	 */
	public Map<String,String[]> getQueryParameterMap() {
		return queryParams;
	}

	/**
	 * Equivalent to {@link #getParameterNames()}, but only looks for query parameters in the URL, not form posts.
	 * <p>
	 * This method can be used to retrieve query parameters without triggering the underlying servlet API to load and parse the request body.
	 * <p>
	 * This object is modifiable.
	 *
	 * @return An iterator of query parameter names.
	 */
	public Iterator<String> getQueryParameterNames() {
		return queryParams.keySet().iterator();
	}

	/* Workhorse method */
	<T> T getQueryParameter(String name, T def, ClassMeta<T> cm) throws ParseException {
		String val = getQueryParameter(name);
		if (val == null)
			return def;
		return parseParameter(val, cm);
	}

	/* Workhorse method */
	<T> T getQueryParameter(String name, ClassMeta<T> cm) throws ParseException {
		String val = getQueryParameter(name);
		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseParameter(val, cm);
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	<T> T getQueryParameters(String name, ClassMeta<T> cm) throws ParseException {
		String[] p = getQueryParameters(name);
		if (p == null)
			return null;
		if (cm.isArray()) {
			List c = new ArrayList();
			for (int i = 0; i < p.length; i++)
				c.add(parseParameter(p[i], cm.getElementType()));
			return (T)ArrayUtils.toArray(c, cm.getElementType().getInnerClass());
		} else if (cm.isCollection()) {
			try {
				Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new ObjectList());
				for (int i = 0; i < p.length; i++)
					c.add(parseParameter(p[i], cm.getElementType()));
				return (T)c;
			} catch (ParseException e) {
				throw e;
			} catch (Exception e) {
				// Typically an instantiation exception.
				throw new ParseException(e);
			}
		}
		throw new ParseException("Invalid call to getQueryParameters(String, ClassMeta).  Class type must be a Collection or array.");
	}


	//--------------------------------------------------------------------------------
	// Form data parameters
	//--------------------------------------------------------------------------------

	/**
	 * Sets a request form data parameter value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void setFormDataParameter(String name, Object value) {
		if (overriddenFormDataParams == null)
			overriddenFormDataParams = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		overriddenFormDataParams.put(name, value == null ? null : value.toString());
	}

	/**
	 * Returns a form data parameter value.
	 * <p>
	 * Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the underlying servlet API.
	 * 	<li>This method returns the raw unparsed value, and differs from calling <code>getFormDataParameter(name, String.<jk>class</js>)</code>
	 * 		which will convert the value from UON notation:
	 * 		<ul>
	 * 			<li><js>"null"</js> =&gt; <jk>null</jk>
	 * 			<li><js>"'null'"</js> =&gt; <js>"null"</js>
	 * 			<li><js>"'foo bar'"</js> =&gt; <js>"foo bar"</js>
	 * 			<li><js>"foo~~bar"</js> =&gt; <js>"foo~bar"</js>
	 * 		</ul>
	 * </ul>
	 *
	 * @param name The form data parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter does not exist.
	 */
	public String getFormDataParameter(String name) {
		String s = null;
		if (overriddenFormDataParams != null)
			s = overriddenFormDataParams.get(name);
		if (s != null)
			return s;

		return super.getParameter(name);
	}

	/**
	 * Same as {@link #getFormDataParameter(String)} except returns a default value if <jk>null</jk> or empty.
	 *
	 * @param name The form data parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if <jk>null</jk> or empty.
	 */
	public String getFormDataParameter(String name, String def) {
		String val = getParameter(name);
		if (val == null || val.isEmpty())
			return def;
		return val;
	}

	/**
	 * Returns the specified form data parameter value converted to a POJO using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = req.getFormDataParameter(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = req.getFormDataParameter(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = req.getFormDataParameter(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the underlying servlet API.
	 * </ul>
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getFormDataParameter(String name, Class<T> type) throws ParseException {
		return getFormDataParameter(name, beanSession.getClassMeta(type));
	}

	/**
	 * Same as {@link #getFormDataParameter(String, Class)} except returns a default value if not specified.
	 *
	 * @param name The parameter name.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param type The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getFormDataParameter(String name, T def, Class<T> type) throws ParseException {
		return getFormDataParameter(name, def, beanSession.getClassMeta(type));
	}

	/**
	 * Same as {@link #getFormDataParameter(String, Class)} except for use on multi-part parameters
	 * 	(e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=(1,2,3)"</js>)
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param type The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getFormDataParameters(String name, Class<T> type) throws ParseException {
		return getFormDataParameters(name, beanSession.getClassMeta(type));
	}

	/**
	 * Returns the specified form data parameter value converted to a POJO using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by the underlying servlet API.
	 * 	<li>Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * </ul>
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getFormDataParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getFormDataParameter(String name, Type type, Type...args) throws ParseException {
		return (T)getFormDataParameter(name, beanSession.getClassMeta(type, args));
	}

	/**
	 * Same as {@link #getFormDataParameter(String, Type, Type...)} except for use on multi-part parameters
	 * 	(e.g. <js>"key=1&amp;key=2&amp;key=3"</js> instead of <js>"key=(1,2,3)"</js>)
	 * <p>
	 * This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getFormDataParameters(String name, Type type, Type...args) throws ParseException {
		return (T)getFormDataParameters(name, beanSession.getClassMeta(type, args));
	}

	/**
	 * Returns <jk>true</jk> if the form data parameters on this request contains the specified entry.
	 * <p>
	 * Note that this returns <jk>true</jk> even if the value is set to null (e.g. <js>"?key"</js>).
	 *
	 * @param name The URL parameter name.
	 * @return <jk>true</jk> if the URL parameters on this request contains the specified entry.
	 */
	public boolean hasFormDataParameter(String name) {
		return getParameterMap().containsKey(name);
	}

	/* Workhorse method */
	<T> T getFormDataParameter(String name, T def, ClassMeta<T> cm) throws ParseException {
		String val = getParameter(name);
		if (val == null)
			return def;
		return parseParameter(val, cm);
	}

	/* Workhorse method */
	<T> T getFormDataParameter(String name, ClassMeta<T> cm) throws ParseException {
		String val = getParameter(name);
		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseParameter(val, cm);
	}

	/* Workhorse method */
	@SuppressWarnings("rawtypes")
	<T> T getFormDataParameters(String name, ClassMeta<T> cm) throws ParseException {
		String[] p = getParameterValues(name);
		if (p == null)
			return null;
		if (cm.isArray()) {
			List c = new ArrayList();
			for (int i = 0; i < p.length; i++)
				c.add(parseParameter(p[i], cm.getElementType()));
			return (T)ArrayUtils.toArray(c, cm.getElementType().getInnerClass());
		} else if (cm.isCollection()) {
			try {
				Collection c = (Collection)(cm.canCreateNewInstance() ? cm.newInstance() : new ObjectList());
				for (int i = 0; i < p.length; i++)
					c.add(parseParameter(p[i], cm.getElementType()));
				return (T)c;
			} catch (ParseException e) {
				throw e;
			} catch (Exception e) {
				// Typically an instantiation exception.
				throw new ParseException(e);
			}
		}
		throw new ParseException("Invalid call to getParameters(String, ClassMeta).  Class type must be a Collection or array.");
	}


	//--------------------------------------------------------------------------------
	// Path parameters
	//--------------------------------------------------------------------------------

	/**
	 * Sets a path parameter value.
	 * <p>
	 * A path parameter is a variable in the path pattern such as <js>"/{foo}"</js>
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void setPathParameter(String name, String value) {
		if (pathParameters == null)
			pathParameters = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		pathParameters.put(name, value == null ? null : value.toString());
	}

	/**
	 * Returns a path parameter value.
	 * <p>
	 * A path parameter is a variable in the path pattern such as <js>"/{foo}"</js>
	 *
	 * @param name The parameter name.
	 * @return The paramter value, or <jk>null</jk> if path parameter not specified.
	 */
	public String getPathParameter(String name) {
		return (pathParameters == null ? null : pathParameters.get(name));
	}

	/**
	 * Returns the specified path parameter converted to a POJO.
	 * <p>
	 * The type can be any POJO type convertable from a <code>String</code> (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertable From Strings</a>).
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> myparam = req.getPathParameter(<js>"myparam"</js>, <jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] myparam = req.getPathParameter(<js>"myparam"</js>, <jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean myparam = req.getPathParameter(<js>"myparam"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List myparam = req.getPathParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map myparam = req.getPathParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The attribute name.
	 * @param type The class type to convert the attribute value to.
	 * @param <T> The class type to convert the attribute value to.
	 * @return The attribute value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getPathParameter(String name, Class<T> type) throws ParseException {
		return getPathParameter(name, beanSession.getClassMeta(type));
	}

	/**
	 * Returns the specified path parameter converted to a POJO.
	 * <p>
	 * The type can be any POJO type convertable from a <code>String</code> (See <a class="doclink" href="package-summary.html#PojosConvertableFromString">POJOs Convertable From Strings</a>).
	 * <p>
	 * Use this method if you want to parse into a parameterized <code>Map</code>/<code>Collection</code> object.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; myparam = req.getPathParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; myparam = req.getPathParameter(<js>"myparam"</js>, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; myparam = req.getPathParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; myparam = req.getPathParameter(<js>"myparam"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param name The attribute name.
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to convert the attribute value to.
	 * @return The attribute value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getPathParameter(String name, Type type, Type...args) throws ParseException {
		return (T)getPathParameter(name, beanSession.getClassMeta(type, args));
	}

	/* Workhorse method */
	<T> T getPathParameter(String name, ClassMeta<T> cm) throws ParseException {
		Object attr = getPathParameter(name);
		T t = null;
		if (attr != null)
			t = urlEncodingParser.parseParameter(attr.toString(), cm);
		if (t == null && cm.isPrimitive())
			return cm.getPrimitiveDefault();
		return t;
	}


	//--------------------------------------------------------------------------------
	// Body methods
	//--------------------------------------------------------------------------------

	/**
	 * Reads the input from the HTTP request as JSON, XML, or HTML and converts the input to a POJO.
	 * <p>
	 * If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks for {@code &body=xxx} in the URL query string.
	 * <p>
	 * If type is <jk>null</jk> or <code>Object.<jk>class</jk></code>, then the actual type will be determined automatically based on the following input:
	 * <table class='styled'>
	 * 	<tr><th>Type</th><th>JSON input</th><th>XML input</th><th>Return type</th></tr>
	 * 	<tr>
	 * 		<td>object</td>
	 * 		<td><js>"{...}"</js></td>
	 * 		<td><code><xt>&lt;object&gt;</xt>...<xt>&lt;/object&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'object'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link ObjectMap}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>array</td>
	 * 		<td><js>"[...]"</js></td>
	 * 		<td><code><xt>&lt;array&gt;</xt>...<xt>&lt;/array&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'array'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link ObjectList}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>string</td>
	 * 		<td><js>"'...'"</js></td>
	 * 		<td><code><xt>&lt;string&gt;</xt>...<xt>&lt;/string&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link String}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>number</td>
	 * 		<td><code>123</code></td>
	 * 		<td><code><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link Number}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>boolean</td>
	 * 		<td><jk>true</jk></td>
	 * 		<td><code><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 		<td>{@link Boolean}</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>null</td>
	 * 		<td><jk>null</jk> or blank</td>
	 * 		<td><code><xt>&lt;null/&gt;</xt></code> or blank<br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'null'</xs><xt>/&gt;</xt></code></td>
	 * 		<td><jk>null</jk></td>
	 * 	</tr>
	 * </table>
	 * <p>
	 * Refer to <a class="doclink" href="../../../../overview-summary.html#Core.PojoCategories">POJO Categories</a> for a complete definition of supported POJOs.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into an integer.</jc>
	 * 	<jk>int</jk> body = req.getBody(<jk>int</jk>.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into an int array.</jc>
	 * 	<jk>int</jk>[] body = req.getBody(<jk>int</jk>[].<jk>class</jk>);

	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean body = req.getBody(MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List body = req.getBody(LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map body = req.getBody(TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The class type to instantiate.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws ParseException If the input contains a syntax error or is malformed for the requested {@code Accept} header or is not valid for the specified type.
	 */
	public <T> T getBody(Class<T> type) throws IOException, ParseException {
		return getBody(beanSession.getClassMeta(type));
	}

	/**
	 * Reads the input from the HTTP request as JSON, XML, or HTML and converts the input to a POJO.
	 * <p>
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List&lt;String&gt; body = req.getBody(LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List&lt;List&lt;String&gt;&gt; body = req.getBody(LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map&lt;String,String&gt; body = req.getBody(TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map&lt;String,List&lt;MyBean&gt;&gt; body = req.getBody(TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The type of object to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 */
	public <T> T getBody(Type type, Type...args) {
		return (T)getBody(beanSession.getClassMeta(type, args));
	}

	/**
	 * Returns the HTTP body content as a plain string.
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query string.
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public String getBodyAsString() throws IOException {
		if (body == null)
			body = IOUtils.readBytes(getInputStream(), 1024);
		return new String(body, IOUtils.UTF8);
	}

	/**
	 * Returns the HTTP body content as a {@link Reader}.
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query string.
	 * <p>
	 * Automatically handles GZipped input streams.
	 */
	@Override /* ServletRequest */
	public BufferedReader getReader() throws IOException {
		Reader r = getUnbufferedReader();
		if (r instanceof BufferedReader)
			return (BufferedReader)r;
		int len = getContentLength();
		int buffSize = len <= 0 ? 8192 : Math.max(len, 8192);
		return new BufferedReader(r, buffSize);
	}

	/**
	 * Same as {@link #getReader()}, but doesn't encapsulate the result in a {@link BufferedReader};
	 *
	 * @return An unbuffered reader.
	 * @throws IOException
	 */
	protected Reader getUnbufferedReader() throws IOException {
		if (body != null)
			return new CharSequenceReader(new String(body, IOUtils.UTF8));
		return new InputStreamReader(getInputStream(), getCharacterEncoding());
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it
	 * 	in the GZIP wrapper.
	 */
	@Override /* ServletRequest */
	public ServletInputStream getInputStream() throws IOException {

		if (body != null)
			return new ServletInputStream2(body);

		Encoder enc = getEncoder();

		ServletInputStream is = super.getInputStream();
		if (enc != null) {
			final InputStream is2 = enc.getInputStream(is);
			return new ServletInputStream2(is2);
		}
		return is;
	}

	/* Workhorse method */
	<T> T getBody(ClassMeta<T> cm) throws RestException {

		try {
			if (cm.isReader())
				return (T)getReader();

			if (cm.isInputStream())
				return (T)getInputStream();

			TimeZone timeZone = getTimeZone();
			Locale locale = getLocale();
			ParserMatch pm = getParserMatch();

			if (pm != null) {
				Parser p = pm.getParser();
				MediaType mediaType = pm.getMediaType();
				try {
					properties.append("mediaType", mediaType).append("characterEncoding", getCharacterEncoding());
					if (! p.isReaderParser()) {
						InputStreamParser p2 = (InputStreamParser)p;
						ParserSession session = p2.createSession(getInputStream(), properties, getJavaMethod(), context.getResource(), locale, timeZone, mediaType);
						return p2.parseSession(session, cm);
					}
					ReaderParser p2 = (ReaderParser)p;
					ParserSession session = p2.createSession(getUnbufferedReader(), properties, getJavaMethod(), context.getResource(), locale, timeZone, mediaType);
					return p2.parseSession(session, cm);
				} catch (ParseException e) {
					throw new RestException(SC_BAD_REQUEST,
						"Could not convert request body content to class type ''{0}'' using parser ''{1}''.",
						cm, p.getClass().getName()
					).initCause(e);
				}
			}

			throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header ''Content-Type'': ''{0}''\n\tSupported media-types: {1}",
				getHeader("Content-Type"), parserGroup.getSupportedMediaTypes()
			);

		} catch (IOException e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR,
				"I/O exception occurred while attempting to handle request ''{0}''.",
				getDescription()
			).initCause(e);
		}
	}


	//--------------------------------------------------------------------------------
	// URI-related methods
	//--------------------------------------------------------------------------------

	/**
	 * Same as {@link HttpServletRequest#getPathInfo()} except returns the path undecoded.
	 *
	 * @return The undecoded portion of the URL after the resource URL path pattern match.
	 */
	public String getPathInfoUndecoded() {
		return RestUtils.getPathInfoUndecoded(this);
	}

	/**
	 * Returns the value {@link #getPathInfo()} split on the <js>'/'</js> character.
	 * <p>
	 * If path info is <jk>null</jk>, returns an empty list.
	 * <p>
	 * URL-encoded characters in segments are automatically decoded by this method.
	 *
	 * @return The decoded segments, or an empty list if path info is <jk>null</jk>.
	 */
	public String[] getPathInfoParts() {
		String s = getPathInfoUndecoded();
		if (s == null || s.isEmpty() || s.equals("/"))
			return new String[0];
		s = s.substring(1);
		if (s.endsWith("/"))
			s = s.substring(0, s.length()-1);
		boolean needsDecode = (s.indexOf('%') != -1 || s.indexOf('+') != -1);
		String[] l = s.split("/", Integer.MAX_VALUE);
		try {
			if (needsDecode)
				for (int i = 0; i < l.length; i++)
					l[i] = URLDecoder.decode(l[i], "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();  // Won't happen.
		}
		return l;
	}

	/**
	 * Returns a resolved URL.
	 * <p>
	 * <ul class='spaced-list'>
	 * 	<li>Fully-qualified absolute URLs (e.g. <js>"http://..."</js>, <js>"https://"</js>) are simply converted to a URL.
	 * 	<li>Absolute URLs (e.g. <js>"/foo/..."</js>) are interpreted as relative to the server hostname.
	 * 	<li>Relative URLs (e.g. <js>"foo/..."</js>) are interpreted as relative to this servlet path.
	 * </ul>
	 *
	 * @param path The URL path to resolve.
	 * @return The resolved URL.
	 * @throws MalformedURLException If path is not a valid URL component.
	 */
	public URL getURL(String path) throws MalformedURLException {
		if (path.startsWith("http://") || path.startsWith("https://"))
			return new URL(path);
		if (StringUtils.startsWith(path, '/'))
			return new URL(getScheme(), getLocalName(), getLocalPort(), path);
		return new URL(getScheme(), getLocalName(), getLocalPort(), getContextPath() + getServletPath() + (StringUtils.isEmpty(path) ? "" : ('/' + path)));
	}

	/**
	 * Returns the URI of the parent of this servlet.
	 *
	 * @return The URI of the parent of this servlet.
	 */
	public String getServletParentURI() {
		String s = getServletURI();
		return s.substring(0, s.lastIndexOf('/'));
	}

	/**
	 * Returns the decoded remainder of the URL following any path pattern matches.
	 * <p>
	 * The behavior of path remainder is shown below given the path pattern "/foo/*":
	 * <p>
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>URL</th>
	 * 		<th>Path Remainder</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo</code></td>
	 * 		<td><jk>null</jk></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/</code></td>
	 * 		<td><js>""</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo//</code></td>
	 * 		<td><js>"/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo///</code></td>
	 * 		<td><js>"//"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/a/b</code></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo//a/b/</code></td>
	 * 		<td><js>"/a/b/"</js></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/a%2Fb</code></td>
	 * 		<td><js>"a/b"</js></td>
	 * 	</tr>
	 * </table>
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// REST method</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>,path=<js>"/foo/{bar}/*"</js>)
	 * 	<jk>public</jk> doGetById(RestServlet res, RestResponse res, <jk>int</jk> bar) {
	 * 		System.<jsm>err</jsm>.println(res.getRemainder());
	 * 	}
	 *
	 * 	<jc>// Prints "path/remainder"</jc>
	 * 	<jk>new</jk> RestCall(servletPath + <js>"/foo/123/path/remainder"</js>).connect();
	 * </p>
	 *
	 * @return The path remainder string.
	 */
	public String getPathRemainder() {
		return RestUtils.decode(pathRemainder);
	}

	/**
	 * Same as {@link #getPathRemainder()} but doesn't decode characters.
	 *
	 * @return The undecoded path remainder.
	 */
	public String getPathRemainderUndecoded() {
		return pathRemainder;
	}

	/**
	 * Returns the URI of the parent resource.
	 * <p>
	 * Trailing slashes in the path are ignored by this method.
	 * <p>
	 * The behavior is shown below:
	 * <table class='styled'>
	 * 	<tr>
	 * 		<th>getRequestURI</th>
	 * 		<th>getRequestParentURI</th>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/bar</code></td>
	 * 		<td><code>/foo</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/bar?baz=bing</code></td>
	 * 		<td><code>/foo</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/bar/</code></td>
	 * 		<td><code>/foo</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo/bar//</code></td>
	 * 		<td><code>/foo</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo//bar//</code></td>
	 * 		<td><code>/foo/</code></td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td><code>/foo</code></td>
	 * 		<td>/</td>
	 * 	</tr>
	 * </table>
	 *
	 * @return The request parent URI.
	 */
	public String getRequestParentURI() {
		String uri = getRequestURI();
		while (StringUtils.endsWith(uri, '/'))
			uri = uri.substring(0, uri.length()-1);
		int i = uri.lastIndexOf('/');
		if (i <= 0)
			return "/";
		return uri.substring(0, i);
	}

	/**
	 * Same as {@link #getRequestURI()} but trims trailing slashes from the result.
	 *
	 * @return The trimmed request URI.
	 */
	public String getTrimmedRequestURI() {
		return RestUtils.trimTrailingSlashes(getRequestURI());
	}

	/**
	 * Same as {@link #getRequestURL()} but trims trailing slashes from the result.
	 *
	 * @return The trimmed request URL.
	 */
	public StringBuffer getTrimmedRequestURL() {
		return RestUtils.trimTrailingSlashes(getRequestURL());
	}

	/**
	 * Gets the URI of the servlet (e.g. <js>"https://localhost:9080/contextPath/servletPath"</js>).
	 *
	 * @return The servlet URI.
	 */
	public String getServletURI() {
		if (servletURI == null) {
			// Note that we can't use getPathInfo() to calculate this since it replaces
			// URL-encoded chars (e.g. %2F) which throws off the length calculation
			// because getRequestURL() does not replace those chars.
			servletURI = getServletURIBuilder().toString();
		}
		return servletURI;
	}

	/**
	 * Gets the path-absolute relative URI of the servlet (e.g. <js>"/contextPath/servletPath"</js>).
	 *
	 * @return The relative servlet URI.
	 */
	public String getRelativeServletURI() {
		if (relativeServletURI == null)
			relativeServletURI = getContextPath() + getServletPath();
		return relativeServletURI;
	}

	/**
	 * Returns a <code>StringBuffer</code> prefilled with the string <code><js>"/[contextPath]/[servletPath]"</js></code>.
	 *
	 * @return The servlet URI string builder.
	 */
	public StringBuffer getServletURIBuilder() {
		return RestUtils.trimPathInfo(getRequestURL(), getContextPath(), getServletPath());
	}


	//--------------------------------------------------------------------------------
	// Labels
	//--------------------------------------------------------------------------------

	/**
	 * Returns the localized servlet title.
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getTitle(RestRequest)} with this object.
	 *
	 * @return The localized servlet label.
	 */
	public String getServletTitle() {
		return context.getInfoProvider().getTitle(this);
	}

	/**
	 * Returns the localized servlet description.
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getDescription(RestRequest)} with this object.
	 *
	 * @return The localized servlet description.
	 */
	public String getServletDescription() {
		return context.getInfoProvider().getDescription(this);
	}

	/**
	 * Returns the localized method summary.
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getMethodSummary(String, RestRequest)} with this object.
	 *
	 * @return The localized method description.
	 */
	public String getMethodSummary() {
		return context.getInfoProvider().getMethodSummary(javaMethod.getName(), this);
	}

	/**
	 * Returns the localized method description.
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getMethodDescription(String, RestRequest)} with this object.
	 *
	 * @return The localized method description.
	 */
	public String getMethodDescription() {
		return context.getInfoProvider().getMethodDescription(javaMethod.getName(), this);
	}

	/**
	 * Returns the localized page title for HTML views.
	 *
	 * @return The localized page title for HTML views.
	 */
	protected String getPageTitle() {
		String s = pageTitle;
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), javaMethod.getName() + ".pageTitle");
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), "pageTitle");
		if (! StringUtils.isEmpty(s))
			return resolveVars(s);
		s = getServletTitle();
		return s;
	}

	/**
	 * Returns the localized page text for HTML views.
	 *
	 * @return The localized page text for HTML views.
	 */
	protected String getPageText() {
		String s = pageText;
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), javaMethod.getName() + ".pageText");
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), "pageText");
		if (! StringUtils.isEmpty(s))
			return resolveVars(s);
		s = getMethodSummary();
		if (StringUtils.isEmpty(s))
			s = getServletDescription();
		return s;
	}

	/**
	 * Returns the localized page links for HTML views.
	 *
	 * @return The localized page links for HTML views.
	 */
	protected String getPageLinks() {
		String s = pageLinks;
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), javaMethod.getName() + ".pageLinks");
		if (StringUtils.isEmpty(s))
			s = context.getMessages().findFirstString(getLocale(), "pageLinks");
		return resolveVars(s);
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the serializers associated with this request.
	 *
	 * @return The serializers associated with this request.
	 */
	public SerializerGroup getSerializerGroup() {
		return serializerGroup;
	}

	/**
	 * Returns the parsers associated with this request.
	 *
	 * @return The parsers associated with this request.
	 */
	public ParserGroup getParserGroup() {
		return parserGroup;
	}

	/**
	 * Returns the parser and media type matching the request <code>Content-Type</code> header.
	 *
	 * @return The parser matching the request <code>Content-Type</code> header, or <jk>null</jk>
	 * 	if no matching parser was found.
	 * Includes the matching media type.
	 */
	public ParserMatch getParserMatch() {
		MediaType mediaType = getMediaType();
		ParserMatch pm = parserGroup.getParserMatch(mediaType);

		// If no patching parser for URL-encoding, use the one defined on the servlet.
		if (pm == null && mediaType.equals(MediaType.URLENCODING))
			pm = new ParserMatch(MediaType.URLENCODING, urlEncodingParser);

		return pm;
	}

	/**
	 * Returns the parser matching the request <code>Content-Type</code> header.
	 *
	 * @return The parser matching the request <code>Content-Type</code> header, or <jk>null</jk>
	 * 	if no matching parser was found.
	 */
	public Parser getParser() {
		ParserMatch pm = getParserMatch();
		return (pm == null ? null : pm.getParser());
	}

	/**
	 * Returns the reader parser matching the request <code>Content-Type</code> header.
	 *
	 * @return The reader parser matching the request <code>Content-Type</code> header, or <jk>null</jk>
	 * 	if no matching reader parser was found, or the matching parser was an input stream parser.
	 */
	public ReaderParser getReaderParser() {
		Parser p = getParser();
		if (p != null && p.isReaderParser())
			return (ReaderParser)p;
		return null;
	}

	/**
	 * Returns the method of this request.
	 * <p>
	 * If <code>allowHeaderParams</code> init parameter is <jk>true</jk>, then first looks for <code>&amp;method=xxx</code> in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getMethod() {
		return method;
	}


	@Override /* ServletRequest */
	public int getContentLength() {
		return contentLength == 0 ? super.getContentLength() : contentLength;
	}

	/**
	 * Returns <jk>true</jk> if <code>&amp;plainText=true</code> was specified as a URL parameter.
	 * <p>
	 * This indicates that the <code>Content-Type</code> of the output should always be set to <js>"text/plain"</js>
	 * 	to make it easy to render in a browser.
	 * <p>
	 * This feature is useful for debugging.
	 *
	 * @return <jk>true</jk> if {@code &amp;plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(getQueryParameter("plainText", "false"));
	}

	/**
	 * Shortcut method for calling {@link MessageBundle#getString(Locale, String, Object...)} based on the request locale.
	 *
	 * @param key The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return The localized message.
	 */
	public String getMessage(String key, Object...args) {
		return context.getMessages().getString(getLocale(), key, args);
	}

	/**
	 * Returns the resource bundle for the request locale.
	 *
	 * @return The resource bundle.  Never <jk>null</jk>.
	 */
	public MessageBundle getResourceBundle() {
		return context.getMessages().getBundle(getLocale());
	}

	/**
	 * Returns the servlet handling the request.
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests,
	 * 	such as in calls to {@link RestGuard#guard(RestRequest, RestResponse)}..
	 *
	 * @return The servlet handling the request.
	 */
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns the java method handling the request.
	 * <p>
	 * Can be used to access the method name or method annotations during requests, such
	 * 	as in calls to {@link RestGuard#guard(RestRequest, RestResponse)}.
	 * <p>
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This returns null when evaluating servlet-level guards since the method has not been resolved at that point of execution.
	 * </ul>
	 *
	 * @return The Java method handling the request, or <code>null</code> if the method
	 * 	has not yet been resolved.
	 */
	public Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the {@link BeanSession} associated with this request.
	 *
	 * @return The request bean session.
	 */
	public BeanSession getBeanSession() {
		return beanSession;
	}

	/**
	 * Returns the variable resolver session for this request using session objects created by {@link RestCallHandler#getSessionObjects(RestRequest)}.
	 *
	 * @return The variable resolver for this request.
	 */
	public VarResolverSession getVarResolverSession() {
		if (varSession == null)
			varSession = context.getVarResolver().createSession(context.getCallHandler().getSessionObjects(this));
		return varSession;
	}

	/**
	 * Shortcut for calling <code>getVarResolverSession().resolve(input)</code>.
	 *
	 * @param input The input string to resolve variables in.
	 * @return The string with variables resolved, or <jk>null</jk> if input is null.
	 */
	public String resolveVars(String input) {
		return getVarResolverSession().resolve(input);
	}

	/**
	 * Returns an instance of a {@link ReaderResource} that represents the contents of a resource text file from the classpath.
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter} variables will be resolved by the variable resolver returned
	 * 	by {@link #getVarResolverSession()}.
	 * @param mediaType The value to set as the <js>"Content-Type"</js> header for this object.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name, boolean resolveVars, MediaType mediaType) throws IOException {
		String s = context.getResourceAsString(name, getLocale());
		if (s == null)
			return null;
		ReaderResource.Builder b = new ReaderResource.Builder().mediaType(mediaType).contents(s);
		if (resolveVars)
			b.varResolver(getVarResolverSession());
		return b.build();
	}

	/**
	 * Same as {@link #getReaderResource(String, boolean, MediaType)} except uses the resource mime-type map
	 * constructed using {@link RestConfig#addMimeTypes(String...)} to determine the media type.
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter} variables will be resolved by the variable resolver returned
	 * 	by {@link #getVarResolverSession()}.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name, boolean resolveVars) throws IOException {
		return getReaderResource(name, resolveVars, MediaType.forString(context.getMediaTypeForName(name)));
	}

	/**
	 * Same as {@link #getReaderResource(String, boolean)} with <code>resolveVars == <jk>false</jk></code>
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name) throws IOException {
		return getReaderResource(name, false, MediaType.forString(context.getMediaTypeForName(name)));
	}

	/**
	 * Returns the config file associated with the servlet.
	 *
	 * @return The config file associated with the servlet, or <jk>null</jk> if servlet does not have a config file associated with it.
	 */
	public ConfigFile getConfigFile() {
		if (cf == null)
			cf = context.getConfigFile().getResolving(getVarResolverSession());
		return cf;
	}

	/**
	 * Returns the localized swagger associated with the servlet.
	 *
	 * @return The swagger associated with the servlet.  Never <jk>null</jk>.
	 */
	public Swagger getSwagger() {
		if (swagger == null)
			swagger = context.getInfoProvider().getSwagger(this);
		return swagger;
	}

	/**
	 * Returns the localized Swagger from the file system.
	 * <p>
	 * Looks for a file called <js>"{ServletClass}_{locale}.json"</js> in the same package
	 * 	as this servlet and returns it as a parsed {@link Swagger} object.
	 * <p>
	 * Returned objects are cached for later quick-lookup.
	 *
	 * @return The parsed swagger object, or <jk>null</jk> if the swagger file could not be found.
	 */
	protected Swagger getSwaggerFromFile() {
		if (fileSwagger == null)
			fileSwagger = context.getInfoProvider().getSwaggerFromFile(this.getLocale());
		if (fileSwagger == null)
			fileSwagger = Swagger.NULL;
		return fileSwagger == Swagger.NULL ? null : fileSwagger;
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder("\n").append(getDescription()).append("\n");
		sb.append("---Headers---\n");
		for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
			String h = e.nextElement();
			sb.append("\t").append(h).append(": ").append(getHeader(h)).append("\n");
		}
		sb.append("---Default Servlet Headers---\n");
		for (Map.Entry<String,String> e : defaultServletHeaders.entrySet()) {
			sb.append("\t").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
		}
		if (javaMethod == null) {
			sb.append("***init() not called yet!***\n");
		} else if (method.equals("PUT") || method.equals("POST")) {
			sb.append("---Body---\n");
			try {
				sb.append(getBodyAsString()).append("\n");
			} catch (Exception e1) {
				sb.append(e1.getLocalizedMessage());
				context.getLogger().log(WARNING, e1, "Error occurred while trying to read debug input.");
			}
		}
		return sb.toString();
	}


	//--------------------------------------------------------------------------------
	// Utility methods
	//--------------------------------------------------------------------------------

	private <T> T parseParameter(String val, ClassMeta<T> c) throws ParseException {
		if (val == null)
			return null;

		// Shortcut - If we're returning a string and the value doesn't start with "'" or is "null", then
		// just return the string since it's a plain value.
		// This allows us to bypass the creation of a UonParserSession object.
		if (c.getInnerClass() == String.class && val.length() > 0) {
			char x = val.charAt(0);
			if (x != '\'' && x != 'n' && val.indexOf('~') == -1)
				return (T)val;
			if (x == 'n' && "null".equals(val))
				return null;
		}

		return urlEncodingParser.parseParameter(val, c);
	}

	/*
	 * Converts an Accept-Language value entry to a Locale.
	 */
	private static Locale toLocale(String lang) {
      String country = "";
      int i = lang.indexOf('-');
      if (i > -1) {
          country = lang.substring(i+1).trim();
          lang = lang.substring(0,i).trim();
      }
      return new Locale(lang, country);
	}

	private Encoder getEncoder() {
		if (encoder == null) {
			String ce = getHeader("content-encoding");
			if (! (ce == null || ce.isEmpty())) {
				ce = ce.trim();
				encoder = encoders.getEncoder(ce);
				if (encoder == null)
					throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE,
						"Unsupported encoding in request header ''Content-Encoding'': ''{0}''\n\tSupported codings: {1}",
						getHeader("content-encoding"), encoders.getSupportedEncodings()
					);
			}

			if (encoder != null)
				contentLength = -1;
		}
		// Note that if this is the identity encoder, we want to return null
		// so that we don't needlessly wrap the input stream.
		if (encoder == IdentityEncoder.INSTANCE)
			return null;
		return encoder;
	}

	/*
	 * Returns header value from URL-parameters or set via setHeader() meant
	 * to override actual header values on the request.
	 */
	private String getOverriddenHeader(String name) {
		String h = null;
		if (context.isAllowHeaderParams())
			h = getQueryParameter(name);
		if (h != null)
			return h;
		if (overriddenHeaders != null) {
			h = overriddenHeaders.get(name);
			if (h != null)
				return h;
		}
		return h;
	}

	void setJavaMethod(Method method) {
		this.javaMethod = method;
	}

	/**
	 * ServletInputStream wrapper around a normal input stream.
	 */
	private static class ServletInputStream2 extends ServletInputStream {

		private final InputStream is;

		private ServletInputStream2(InputStream is) {
			this.is = is;
		}

		private ServletInputStream2(byte[] b) {
			this(new ByteArrayInputStream(b));
		}

		@Override /* InputStream */
		public final int read() throws IOException {
			return is.read();
		}

		@Override /* InputStream */
		public final void close() throws IOException {
			is.close();
		}
	}
}