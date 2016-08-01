/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server;

import static java.util.Collections.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.encoders.*;
import com.ibm.juno.core.encoders.Encoder;
import com.ibm.juno.core.ini.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.parser.ParseException;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.labels.*;

/**
 * Represents an HTTP request for a REST resource.
 * <p>
 * 	Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 * </p>
 * <p>
 * 	For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * 	following methods return the following values....
 * </p>
 * 	<table class='styled'>
 * 		<tr><th>Method</th><th>Value</th></tr>
 * 		<tr><td>{@code getContextPath()}</td><td>{@code /contextRoot}</td></tr>
 * 		<tr><td>{@code getPathInfo()}</td><td>{@code /foo}</td></tr>
 * 		<tr><td>{@code getPathTranslated()}</td><td>{@code path-to-deployed-war-on-filesystem/foo}</td></tr>
 * 		<tr><td>{@code getQueryString()}</td><td>{@code bar=baz}</td></tr>
 * 		<tr><td>{@code getRequestURI()}</td><td>{@code /contextRoot/servletPath/foo}</td></tr>
 * 		<tr><td>{@code getRequestURL()}</td><td>{@code http://localhost:9080/contextRoot/servletPath/foo}</td></tr>
 * 		<tr><td>{@code getServletPath()}</td><td>{@code /servletPath}</td></tr>
 * 	</table>
 * <p>
 * 	Refer to <a class='doclink' href='package-summary.html#TOC'>REST Servlet API</a> for information about using this class.
 * </p>
 *
 * @author jbognar
 */
@SuppressWarnings("unchecked")
public final class RestRequest extends HttpServletRequestWrapper {

	private final RestServlet servlet;
	private String method, pathRemainder, content;
	Method javaMethod;
	private ObjectMap properties;
	private ParserGroup parserGroup;
	private Encoder encoder;
	private int contentLength;
	private final boolean debug;
	private UrlEncodingParser urlEncodingParser;   // The parser used to parse URL attributes and parameters (beanContext also used to parse headers)
	private BeanContext beanContext;
	private StringVarResolver varResolver;
	private Map<String,String[]> queryParams;
	private Map<String,String> defaultServletHeaders, defaultMethodHeaders, overriddenHeaders, overriddenParams;
	private boolean isPost;
	private String servletURI, relativeServletURI;
	private String charset, defaultCharset;
	private ObjectMap headers;
	private ConfigFile cf;

	/**
	 * Constructor.
	 */
	RestRequest(RestServlet servlet, HttpServletRequest req) throws ServletException {
		super(req);

		try {
			this.servlet = servlet;
			isPost = req.getMethod().equalsIgnoreCase("POST");

			// If this is a POST, we want to parse the query parameters ourselves to prevent
			// the servlet code from processing the HTTP body as URL-Encoded parameters.
			if (isPost)
				queryParams = servlet.getUrlEncodingParser().parseIntoSimpleMap(getQueryString());
			else {
				queryParams = req.getParameterMap();
			}

			// Get the HTTP method.
			// Can be overridden through a "method" GET attribute.
			method = super.getMethod();

			String m = getQueryParameter("method");
			if (! StringUtils.isEmpty(m) && (servlet.allowMethodParams.contains(m) || servlet.allowMethodParams.contains("*")))
				method = m;

			if (servlet.allowContentParam)
				content = getQueryParameter("content");

			defaultServletHeaders = servlet.getDefaultRequestHeaders();

			debug = "true".equals(getQueryParameter("debug", "false"));

			if (debug) {
				servlet.log(Level.INFO, toString());
			}

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
	final void init(Method javaMethod, String pathRemainder, ObjectMap properties, Map<String,String> mDefaultRequestHeaders, String defaultCharset, ParserGroup mParsers, UrlEncodingParser mUrlEncodingParser) {
		this.javaMethod = javaMethod;
		this.pathRemainder = pathRemainder;
		this.properties = properties;
		this.defaultMethodHeaders = mDefaultRequestHeaders;
		this.parserGroup = mParsers;
		this.urlEncodingParser = mUrlEncodingParser;
		this.beanContext = urlEncodingParser.getBeanContext();
		this.defaultCharset = defaultCharset;
	}

	/**
	 * Returns <jk>true</jk> if the request contains any of the specified parameters.
	 *
	 * @param params The list of parameters to check for.
	 * @return <jk>true</jk> if the request contains any of the specified parameters.
	 */
	public boolean hasAnyQueryParameters(String...params) {
		for (String p : params)
			if (hasQueryParameter(p))
				return true;
		return false;
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

	/**
	 * Servlet calls this method to initialize the properties.
	 */
	RestRequest setProperties(ObjectMap properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Retrieve the properties active for this request.
	 * These properties can be modified by the request.
	 *
	 * @return The properties active for this request.
	 */
	public ObjectMap getProperties() {
		return this.properties;
	}

	/**
	 * Returns the <code>Content-Type</code> header value on the request, stripped
	 * 	of any parameters such as <js>";charset=X"</js>.
	 * <p>
	 * Example: <js>"text/json"</js>.
	 * <p>
	 * If the content type is not specified, and the content is specified via a
	 * 	<code>&content</code> query parameter, the content type is assumed to be
	 * 	<js>"text/uon"</js>.  Otherwise, the
	 * 	content type is assumed to be <js>"text/json"</js>.
	 *
	 * @return The <code>Accept</code> media-type header values on the request.
	 */
	public String getMediaType() {
		String cm = getHeader("Content-Type");
		if (cm == null) {
			if (content != null)
				return "text/uon";
			return "text/json";
		}
		int j = cm.indexOf(';');
		if (j != -1)
			cm = cm.substring(0, j);
		return cm;
	}

	/**
	 * Returns the media types that are valid for <code>Content-Type</code> headers on the request.
	 *
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<String> getSupportedMediaTypes() {
		return parserGroup.getSupportedMediaTypes();
	}

	/**
	 * Returns the charset specified on the <code>Content-Type</code> header, or
	 * <js>"UTF-8"</js> if not specified.
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
			if (! RestServlet.availableCharsets.containsKey(charset))
				throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header ''Content-Type'': ''{0}''", h);
		}
		return charset;
	}

	/**
	 * Sets the charset to expect on the request body.
	 */
	@Override /* ServletRequest */
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}

	/**
	 * Returns the specified header value, or <jk>null</jk> if the header value isn't present.
	 * <p>
	 * 	If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks
	 * 	for {@code &HeaderName=x} in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getHeader(String name) {
		return getHeader(name, (String)null);
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

	/**
	 * Set the request header to the specified value.
	 *
	 * @param name The header name.
	 * @param value The header value.
	 */
	public void setHeader(String name, String value) {
		if (overriddenHeaders == null)
			overriddenHeaders = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		overriddenHeaders.put(name, value);
	}

	/**
	 * Set the request parameter to the specified value.
	 *
	 * @param name The parameter name.
	 * @param value The parameter value.
	 */
	public void setParameter(String name, Object value) {
		if (overriddenParams == null)
			overriddenParams = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
		overriddenParams.put(name, value == null ? null : value.toString());
	}

	/**
	 * Returns the specified header value, or the specified default value if the
	 * 	header value isn't present.
	 * <p>
	 * 	If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks
	 * 	for {@code &HeaderName=x} in the URL query string.
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

	@Override /* ServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String h = getOverriddenHeader(name);
		if (h != null)
			return enumeration(singleton(h));
		return super.getHeaders(name);
	}

	/*
	 * Returns header value from URL-parameters or set via setHeader() meant
	 * to override actual header values on the request.
	 */
	private String getOverriddenHeader(String name) {
		String h = null;
		if (servlet.allowHeaderParams)
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

	@Override /* ServletRequest */
	public Locale getLocale() {
		String h = getOverriddenHeader("Accept-Language");
		if (h != null) {
			MediaRange[] mr = MediaRange.parse(h);
			if (mr.length > 0)
				return toLocale(mr[0].getType());
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
					l.add(toLocale(r.getType()));
				return enumeration(l);
			}
		}
		return super.getLocales();
	}

	/**
	 * Converts an Accept-Header value entry to a Locale.
	 */
	private Locale toLocale(String lang) {
      String country = "";
      int i = lang.indexOf('-');
      if (i > -1) {
          country = lang.substring(i+1).trim();
          lang = lang.substring(0,i).trim();
      }
      return new Locale(lang, country);
	}

	/**
	 * Returns the method of this request.
	 * <p>
	 * 	If <code>allowHeaderParams</code> init parameter is <jk>true</jk>, then first looks
	 * 	for <code>&method=xxx</code> in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the parameter with the specified name.
	 * <p>
	 * Returns <jk>null</jk> for parameters with no value (e.g. <js>"&foo"</js>).
	 * This is consistent with WAS, but differs from Tomcat behavior.
	 * The presence of parameter <js>"&foo"</js> in this case can be determined using {@link #hasParameter(String)}.
	 * <p>
	 * Parameter lookup is case-insensitive (consistent with WAS, but differs from Tomcat).
	 * <p>
	 * <i>Note:</i> Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by
	 * 	the underlying servlet API.
	 * <p>
	 * <i>Note:</i> This method returns the raw unparsed value, and differs from calling <code>getParameter(name, String.<jk>class</js>)</code>
	 * 	which will convert the value from UON notation:
	 * <ul>
	 * 	<li><js>"\u0000"</js> =&gt; <jk>null</jk>
	 * 	<li><js>"$s(foo)"</js> =&gt; <js>"foo"</js>
	 * 	<li><js>"(foo)"</js> =&gt; <js>"foo"</js>
	 * </ul>
	 */
	@Override /* ServletRequest */
	public String getParameter(String name) {
		String s = null;
		if (overriddenParams != null)
			s = overriddenParams.get(name);
		if (s != null)
			return s;

		String val = super.getParameter(name);

		// Fix for behavior difference between Tomcat and WAS.
		// getParameter("foo") on "&foo" in Tomcat returns "".
		// getParameter("foo") on "&foo" in WAS returns null.
		if (val != null && val.isEmpty())
			if (queryParams.containsKey(name))
				val = null;

		return val;
	}

	/**
	 * Same as {@link #getParameter(String)} except returns the default value
	 * 	if <jk>null</jk> or empty.
	 *
	 * @param name The query parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if <jk>null</jk> or empty.
	 */
	public String getParameter(String name, String def) {
		String val = getParameter(name);
		if (val == null || val.isEmpty())
			return def;
		return val;
	}

	/**
	 * Returns the specified URL parameter value parsed to the specified class type using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <i>Note:</i> Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by
	 * 	the underlying servlet API.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameter(String name, Class<T> c, T def) throws ParseException {
		return getParameter(name, beanContext.getClassMeta(c), def);
	}

	/**
	 * Returns the specified URL parameter value parsed to the specified class type using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <i>Note:</i> Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by
	 * 	the underlying servlet API.
	 * <p>
	 * Unlike {@link #getParameter(String, Class, Object)}, this method can be used to parse parameters
	 * 	of complex types involving JCF classes.
	 * <p class='bcode'>
	 * 	ClassMeta&ltMap&lt;String,Integer&gt;&gt; cm = request.getBeanContext().getMapClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, Integer.<jk>class</jk>);
	 * 	Map&lt;String,Integer&gt; m = request.getParameter(<js>"myParameter"</js>, cm, <jk>new</jk> TreeMap&lt;String,Integer&gt;());
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameter(String name, ClassMeta<T> cm, T def) throws ParseException {
		String val = getParameter(name);
		if (val == null)
			return def;
		return parseParameter(val, cm);
	}

	/**
	 * Returns the specified URL parameter value parsed to the specified class type using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <i>Note:</i> Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by
	 * 	the underlying servlet API.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameter(String name, Class<T> c) throws ParseException {
		return getParameter(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getParameter(String, Class)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>)
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameters(String name, Class<T> c) throws ParseException {
		return getParameters(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getParameter(String, Class)} except works on parameterized
	 * types such as those returned by {@link Method#getGenericParameterTypes()}
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameter(String name, Type c) throws ParseException {
		return (T)getParameter(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getParameter(String, Class)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>)
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameters(String name, Type c) throws ParseException {
		return (T)getParameters(name, beanContext.getClassMeta(c));
	}

	/**
	 * Returns the specified URL parameter value parsed to the specified class type using the
	 * 	{@link UrlEncodingParser} registered with this servlet.
	 * <p>
	 * <i>Note:</i> Calling this method on URL-Encoded FORM posts causes the body content to be loaded and parsed by
	 * 	the underlying servlet API.
	 * <p>
	 * Unlike {@link #getParameter(String, Class)}, this method can be used to parse parameters
	 * 	of complex types involving JCF classes.
	 * <p class='bcode'>
	 * 	ClassMeta&lt;Map&lt;String,Integer&gt;&gt; cm = request.getBeanContext().getMapClassMeta(TreeMap.<jk>class</jk>, String.<jk>class</jk>, Integer.<jk>class</jk>);
	 * 	Map&lt;String,Integer&gt; m = request.getParameter(<js>"myParameter"</js>, cm);
	 * </p>
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getParameter(String name, ClassMeta<T> cm) throws ParseException {

		String val = getParameter(name);

		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();

		return parseParameter(val, cm);
	}

	/**
	 * Same as {@link #getParameter(String, ClassMeta)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>)
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	@SuppressWarnings("rawtypes")
	public <T> T getParameters(String name, ClassMeta<T> cm) throws ParseException {
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

	/**
	 * Returns <jk>true</jk> if the URL parameters on this request contains the specified entry.
	 * <p>
	 * Note that this returns <jk>true</jk> even if the value is set to null (e.g. <js>"?key"</js>).
	 *
	 * @param name The URL parameter name.
	 * @return <jk>true</jk> if the URL parameters on this request contains the specified entry.
	 */
	public boolean hasParameter(String name) {
		return getParameterMap().containsKey(name);
	}

	/**
	 * Same as {@link #getParameter(String)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The URL parameter name.
	 * @return The parameter value, or <jk>null</jk> if parameter not specified or has no value (e.g. <js>"&foo"</js>.
	 */
	public String getQueryParameter(String name) {
		String s = null;
		if (overriddenParams != null)
			s = overriddenParams.get(name);
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
	 * Same as {@link #getQueryParameter(String)} but returns the specified default
	 * 	value if the query parameter was not specified.
	 *
	 * @param name The URL parameter name.
	 * @param def The default value.
	 * @return The parameter value, or the default value if parameter not specified or has no value (e.g. <js>"&foo"</js>.
	 */
	public String getQueryParameter(String name, String def) {
		String s = getQueryParameter(name);
		return s == null ? def : s;
	}

	/**
	 * Same as {@link #getParameter(String, Class, Object)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Class<T> c, T def) throws ParseException {
		return getQueryParameter(name, beanContext.getClassMeta(c), def);
	}

	/**
	 * Same as {@link #getParameter(String, ClassMeta, Object)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param def The default value if the parameter was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, ClassMeta<T> cm, T def) throws ParseException {
		String val = getQueryParameter(name);
		if (val == null)
			return def;
		return parseParameter(val, cm);
	}

	/**
	 * Same as {@link #getParameter(String, ClassMeta, Object)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Class<T> c) throws ParseException {
		return getQueryParameter(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Class)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>).
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The query parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameters(String name, Class<T> c) throws ParseException {
		return getQueryParameters(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Class)} except works on parameterized
	 * types such as those returned by {@link Method#getGenericParameterTypes()}
	 *
	 * @param name The query parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, Type c) throws ParseException {
		return (T)getQueryParameter(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getQueryParameter(String, Type)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>).
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The query parameter name.
	 * @param c The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The query parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameters(String name, Type c) throws ParseException {
		return (T)getQueryParameters(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getParameter(String, ClassMeta)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve a parameter without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getQueryParameter(String name, ClassMeta<T> cm) throws ParseException {

		String val = getQueryParameter(name);

		if (cm.isPrimitive() && (val == null || val.isEmpty()))
			return cm.getPrimitiveDefault();
		return parseParameter(val, cm);
	}

	/**
	 * Same as {@link #getQueryParameter(String, ClassMeta)} except for use on multi-part parameters
	 * 	(e.g. <js>"&key=1&key=2&key=3"</js> instead of <js>"&key=(1,2,3)"</js>).
	 * <p>
	 * 	This method must only be called when parsing into classes of type Collection or array.
	 *
	 * @param name The parameter name.
	 * @param cm The class type to convert the parameter value to.
	 * @param <T> The class type to convert the parameter value to.
	 * @return The parameter value converted to the specified class type.
	 * @throws ParseException
	 */
	@SuppressWarnings("rawtypes")
	public <T> T getQueryParameters(String name, ClassMeta<T> cm) throws ParseException {
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

	/**
	 * Returns the list of all query parameters with the specified name.
	 * Same as {@link #getParameterValues(String)} except only looks in the URL string,
	 * 	not parameters from URL-Encoded FORM posts.
	 * <p>
	 * This method can be used to retrieve parameters without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 *
	 * @param name
	 * @return the list of query parameters, or <jk>null</jk> if the parameter does not exist.
	 */
	public String[] getQueryParameters(String name) {
		return queryParams.get(name);
	}

	/**
	 * Returns <jk>true</jk> if the URL parameters on this request contains the specified entry.
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
	 * Equivalent to {@link #getParameterMap()}, but only looks for query parameters in the URL, not form posts.
	 * <p>
	 * This method can be used to retrieve query parameters without triggering the underlying
	 * 	servlet API to load and parse the request body.
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
	 * This method can be used to retrieve query parameters without triggering the underlying
	 * 	servlet API to load and parse the request body.
	 * <p>
	 * This object is modifiable.
	 *
	 * @return An iterator of query parameter names.
	 */
	public Iterator<String> getQueryParameterNames() {
		return queryParams.keySet().iterator();
	}

	private <T> T parseParameter(String val, ClassMeta<T> c) throws ParseException {
		if (val == null)
			return null;
		// Shortcut - If we're returning a string and the value doesn't start with '$' or '(', then
		// just return the string since it's a plain value.
		if (c.getInnerClass() == String.class && val.length() > 0) {
			char x = val.charAt(0);
			if (x != '(' && x != '$' && x != '\u0000' && val.indexOf('~') == -1)
				return (T)val;
		}
		return urlEncodingParser.parseParameter(val, c);
	}

	/**
	 * Shortcut for calling <code>getHeaders().get(c, name, def);</code>
	 * <p>
	 * 	The type can be any POJO type convertable from a <code>String</code> (See <a class='doclink' href='package-summary.html#PojosConvertableFromString'>POJOs Convertable From Strings</a>).
	 *
	 * @param name The HTTP header name.
	 * @param c The class type to convert the header value to.
	 * @param def The default value if the header was not specified or is <jk>null</jk>.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, Class<T> c, T def) {
		String h = getHeader(name);
		if (h == null)
			return def;
		return beanContext.convertToType(h, c);
	}

	/**
	 * Shortcut for calling <code>getHeaders().get(c, name);</code>
	 * <p>
	 * 	The type can be any POJO type convertable from a <code>String</code> (See <a class='doclink' href='package-summary.html#PojosConvertableFromString'>POJOs Convertable From Strings</a>).
	 *
	 * @param name The HTTP header name.
	 * @param c The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, Class<T> c) {
		String h = getHeader(name);
		return beanContext.convertToType(h, c);
	}

	/**
	 * Same as {@link #getHeader(String, Class)} except works on parameterized
	 * types such as those returned by {@link Method#getGenericParameterTypes()}
	 *
	 * @param name The HTTP header name.
	 * @param c The class type to convert the header value to.
	 * @param <T> The class type to convert the header value to.
	 * @return The parameter value converted to the specified class type.
	 */
	public <T> T getHeader(String name, Type c) {
		String h = getHeader(name);
		return (T)beanContext.convertToType(null, h, beanContext.getClassMeta(c));
	}

	/**
	 * Returns the specified request attribute converted to the specified class type.
	 * <p>
	 * 	The type can be any POJO type convertable from a <code>String</code> (See <a class='doclink' href='package-summary.html#PojosConvertableFromString'>POJOs Convertable From Strings</a>).
	 *
	 * @param name The attribute name.
	 * @param c The class type to convert the attribute value to.
	 * @param <T> The class type to convert the attribute value to.
	 * @return The attribute value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAttribute(String name, Class<T> c) throws ParseException {
		return getAttribute(name, beanContext.getClassMeta(c));
	}

	/**
	 * Same as {@link #getAttribute(String, Class)} except works on parameterized
	 * types such as those returned by {@link Method#getGenericParameterTypes()}
	 *
	 * @param name The attribute name.
	 * @param c The class type to convert the attribute value to.
	 * @param <T> The class type to convert the attribute value to.
	 * @return The attribute value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAttribute(String name, Type c) throws ParseException {
		return (T)getAttribute(name, beanContext.getClassMeta(c));
	}

	/**
	 * Returns the specified request attribute converted to the specified class type.
	 * <p>
	 * 	The type can be any POJO type convertable from a <code>String</code> (See <a class='doclink' href='package-summary.html#PojosConvertableFromString'>POJOs Convertable From Strings</a>).
	 *
	 * @param name The attribute name.
	 * @param cm The class type to convert the attribute value to.
	 * @param <T> The class type to convert the attribute value to.
	 * @return The attribute value converted to the specified class type.
	 * @throws ParseException
	 */
	public <T> T getAttribute(String name, ClassMeta<T> cm) throws ParseException {
		Object attr = getAttribute(name);
		T t = null;
		if (attr != null)
			t = urlEncodingParser.parseParameter(attr.toString(), cm);
		if (t == null && cm.isPrimitive())
			return cm.getPrimitiveDefault();
		return t;
	}

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
	 * Same as {@link #getInput(ClassMeta)}, except a shortcut for passing in regular {@link Class} objects
	 * 	instead of having to look up {@link ClassMeta} objects.
	 *
	 * @param type The class type to instantiate.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 * @throws ParseException If the input contains a syntax error or is malformed for the requested {@code Accept} header or is not valid for the specified type.
	 */
	public <T> T getInput(Class<T> type) throws IOException, ParseException {
		return getInput(beanContext.getClassMeta(type));
	}

	/**
	 * Same as {@link #getInput(Class)} except works on parameterized
	 * types such as those returned by {@link Method#getGenericParameterTypes()}
	 *
	 * @param type The class type to instantiate.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 */
	public <T> T getInput(Type type) {
		return (T)getInput(beanContext.getClassMeta(type));
	}

	/**
	 * Reads the input from the HTTP request as JSON, XML, or HTML and converts the input to the specified class type.
	 * <p>
	 * 	If {@code allowHeaderParams} init parameter is <jk>true</jk>, then first looks
	 * 	for {@code &content=xxx} in the URL query string.
	 * <p>
	 * 	If type is <jk>null</jk> or <code>Object.<jk>class</jk></code>, then the actual type will be determined automatically based on the
	 * 	following input:
	 * 	<table class='styled'>
	 * 		<tr><th>Type</th><th>JSON input</th><th>XML input</th><th>Return type</th></tr>
	 * 		<tr>
	 * 			<td>object</td>
	 * 			<td><js>"{...}"</js></td>
	 * 			<td><code><xt>&lt;object&gt;</xt>...<xt>&lt;/object&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'object'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 			<td>{@link ObjectMap}</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>array</td>
	 * 			<td><js>"[...]"</js></td>
	 * 			<td><code><xt>&lt;array&gt;</xt>...<xt>&lt;/array&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'array'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 			<td>{@link ObjectList}</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>string</td>
	 * 			<td><js>"'...'"</js></td>
	 * 			<td><code><xt>&lt;string&gt;</xt>...<xt>&lt;/string&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 			<td>{@link String}</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>number</td>
	 * 			<td><code>123</code></td>
	 * 			<td><code><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 			<td>{@link Number}</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>boolean</td>
	 * 			<td><jk>true</jk></td>
	 * 			<td><code><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></code><br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'boolean'</xs><xt>&gt;</xt>...<xt>&lt;/x&gt;</xt></code></td>
	 * 			<td>{@link Boolean}</td>
	 * 		</tr>
	 * 		<tr>
	 * 			<td>null</td>
	 * 			<td><jk>null</jk> or blank</td>
	 * 			<td><code><xt>&lt;null/&gt;</xt></code> or blank<br><code><xt>&lt;x</xt> <xa>type</xa>=<xs>'null'</xs><xt>/&gt;</xt></code></td>
	 * 			<td><jk>null</jk></td>
	 * 		</tr>
	 * 	</table>
	 * <p>
	 * 	Refer to <a href='../core/package-summary.html#PojoCategories' class='doclink'>POJO Categories</a> for a complete definition of supported POJOs.
	 *
	 * @param type The class type to instantiate.
	 * @param <T> The class type to instantiate.
	 * @return The input parsed to a POJO.
	 * @throws RestException If a problem occurred trying to read the input.
	 */
	public <T> T getInput(ClassMeta<T> type) throws RestException {

		try {
			if (type.isReader())
				return (T)getReader();

			if (type.isInputStream())
				return (T)getInputStream();

			String mediaType = getMediaType();
			Parser<?> p = getParser();

			if (p != null) {
				try {
					properties.append("mediaType", mediaType).append("characterEncoding", getCharacterEncoding());
					if (! p.isReaderParser()) {
						InputStreamParser p2 = (InputStreamParser)p;
						ParserContext ctx = p2.createContext(properties, getJavaMethod(), getServlet());
						return p2.parse(getInputStream(), getContentLength(), type, ctx);
					}
					ReaderParser p2 = (ReaderParser)p;
					ParserContext ctx = p2.createContext(properties, getJavaMethod(), getServlet());
					Reader r = getUnbufferedReader();
					return p2.parse(r, getContentLength(), type, ctx);
				} catch (ParseException e) {
					throw new RestException(SC_BAD_REQUEST,
						"Could not convert request body content to class type ''{0}'' using parser ''{1}''.",
						type, p.getClass().getName()
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

	/**
	 * Returns the parser matching the request <code>Accept</code> header.
	 *
	 * @return The parser matching the request <code>Accept</code> header, or <jk>null</jk>
	 * if no matching parser was found.
	 */
	public Parser<?> getParser() {
		String mediaType = getMediaType();
		Parser<?> p = parserGroup.getParser(mediaType);

		// If no patching parser for URL-encoding, use the one defined on the servlet.
		if (p == null && mediaType.equals("application/x-www-form-urlencoded"))
			p = urlEncodingParser;

		return p;
	}

	/**
	 * Returns the reader parser matching the request <code>Accept</code> header.
	 *
	 * @return The reader parser matching the request <code>Accept</code> header, or <jk>null</jk>
	 * if no matching reader parser was found, or the matching parser was an input stream parser.
	 */
	public ReaderParser getReaderParser() {
		Parser<?> p = getParser();
		if (p.isReaderParser())
			return (ReaderParser)p;
		return null;
	}

	/**
	 * Returns the HTTP body content as a plain string.
	 * <p>
	 * 	If {@code allowHeaderParams} init parameter is true, then first looks
	 * 	for {@code &content=xxx} in the URL query string.
	 *
	 * @return The incoming input from the connection as a plain string.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public String getInputAsString() throws IOException {
		if (content != null)
			return content;
		content = IOUtils.read(getReader()).toString();
		return content;
	}

	/**
	 * Returns a resolved URL.
	 * <p>
	 * <ul>
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
	 * Returns the HTTP body content as a {@link Reader}.
	 * <p>
	 * 	If {@code allowHeaderParams} init parameter is true, then first looks
	 * 	for {@code &content=xxx} in the URL query string.
	 * <p>
	 * 	Automatically handles GZipped input streams.
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
		if (content != null)
			return new CharSequenceReader(content);
		return new InputStreamReader(getInputStream(), getCharacterEncoding());
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 * <p>
	 * 	Automatically handles GZipped input streams.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it
	 * 	in the GZIP wrapper.
	 */
	@Override /* ServletRequest */
	public ServletInputStream getInputStream() throws IOException {

		Encoder enc = getEncoder();

		ServletInputStream is = super.getInputStream();
		if (enc != null) {
			final InputStream is2 = enc.getInputStream(is);
			return new ServletInputStream() {
				@Override /* InputStream */
				public final int read() throws IOException {
					return is2.read();
				}
				@Override /* InputStream */
				public final void close() throws IOException {
					is2.close();
				}
			};
		}
		return is;
	}

	private Encoder getEncoder() throws IOException {
		if (encoder == null) {
			String ce = getHeader("content-encoding");
			if (! (ce == null || ce.isEmpty())) {
				try {
					ce = ce.trim();
					encoder = servlet.getEncoders().getEncoder(ce);
					if (encoder == null)
						throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE,
							"Unsupported encoding in request header ''Content-Encoding'': ''{0}''\n\tSupported codings: {1}",
							getHeader("content-encoding"), servlet.getEncoders().getSupportedEncodings()
						);
				} catch (RestServletException e) {
					throw new IOException(e);
				}
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

	@Override /* ServletRequest */
	public int getContentLength() {
		return contentLength == 0 ? super.getContentLength() : contentLength;
	}

	/**
	 * Returns <jk>true</jk> if <code>&plainText=true</code> was specified as a URL parameter.
	 * <p>
	 * 	This indicates that the <code>Content-Type</code> of the output should always be set to <js>"text/plain"</js>
	 * 	to make it easy to render in a browser.
	 * <p>
	 * 	This feature is useful for debugging.
	 *
	 * @return <jk>true</jk> if {@code &plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(getQueryParameter("plainText", "false"));
	}

	/**
	 * Returns the decoded remainder of the URL following any path pattern matches.
	 * <p>
	 * The behavior of path remainder is shown below given the path pattern "/foo/*":
	 * <p>
	 * 	<table class='styled'>
	 * 		<tr>
	 * 			<th>URL</th>
	 * 			<th>Path Remainder</th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo</code></th>
	 * 			<th><jk>null</jk></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/</code></th>
	 * 			<th><js>""</js></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo//</code></th>
	 * 			<th><js>"/"</js></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo///</code></th>
	 * 			<th><js>"//"</js></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/a/b</code></th>
	 * 			<th><js>"a/b"</js></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo//a/b/</code></th>
	 * 			<th><js>"/a/b/"</js></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/a%2Fb</code></th>
	 * 			<th><js>"a/b"</js></th>
	 * 		</tr>
	 * 	</table>
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * 		<p class='bcode'>
	 * 	<jc>// REST method</jc>
	 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>,path=<js>"/foo/{bar}/*"</js>)
	 * 	<jk>public</jk> doGetById(RestServlet res, RestResponse res, <jk>int</jk> bar) {
	 * 		System.<jsm>err</jsm>.println(res.getRemainder());
	 * 	}
	 *
	 * 	<jc>// Prints "path/remainder"</jc>
	 * 	<jk>new</jk> RestCall(servletPath + <js>"/foo/123/path/remainder"</js>).connect();
	 * 		</p>
	 * 	</dd>
	 * </dl>
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
	 * Shortcut method for calling {@link RestServlet#getMessage(Locale, String, Object...)} based
	 * 	on the request locale.
	 *
	 * @param key The message key.
	 * @param args Optional {@link MessageFormat} variable values in the value.
	 * @return The localized message.
	 */
	public String getMessage(String key, Object...args) {
		return servlet.getMessage(getLocale(), key, args);
	}

	/**
	 * Shortcut method for calling {@link RestServlet#getMethodDescriptions(RestRequest)} based
	 * 	on the request locale.
	 *
	 * @return The localized method descriptions.
	 * @throws RestServletException
	 */
	public Collection<MethodDescription> getMethodDescriptions() throws RestServletException {
		return servlet.getMethodDescriptions(this);
	}

	/**
	 * Returns the resource bundle for the request locale.
	 *
	 * @return The resource bundle.  Never <jk>null</jk>.
	 */
	public SafeResourceBundle getResourceBundle() {
		return servlet.getResourceBundle(getLocale());
	}

	/**
	 * Returns the servlet handling the request.
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests,
	 * 	such as in calls to {@link RestGuard#guard(RestRequest, RestResponse)}..
	 *
	 * @return The servlet handling the request.
	 */
	public RestServlet getServlet() {
		return servlet;
	}

	/**
	 * Returns the java method handling the request.
	 * <p>
	 * Can be used to access the method name or method annotations during requests, such
	 * 	as in calls to {@link RestGuard#guard(RestRequest, RestResponse)}.
	 * <p>
	 * Note:  This returns null when evaluating servlet-level guards since the method
	 * 	has not been resolved at that point of execution.
	 *
	 * @return The Java method handling the request, or <code>null</code> if the method
	 * 	has not yet been resolved.
	 */
	public Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the URI of the parent resource.
	 * <p>
	 * Trailing slashes in the path are ignored by this method.
	 * <p>
	 * The behavior is shown below:
	 * 	<table class='styled'>
	 * 		<tr>
	 * 			<th>getRequestURI</th>
	 * 			<th>getRequestParentURI</th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/bar</code></th>
	 * 			<th><code>/foo</code></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/bar?baz=bing</code></th>
	 * 			<th><code>/foo</code></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/bar/</code></th>
	 * 			<th><code>/foo</code></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo/bar//</code></th>
	 * 			<th><code>/foo</code></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo//bar//</code></th>
	 * 			<th><code>/foo/</code></th>
	 * 		</tr>
	 * 		<tr>
	 * 			<th><code>/foo</code></th>
	 * 			<th>/</th>
	 * 		</tr>
	 * 	</table>
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

	/**
	 * Returns the {@link BeanContext} associated with this request.
	 *
	 * @return The request bean context.
	 */
	public BeanContext getBeanContext() {
		return beanContext;
	}

	/**
	 * Returns the localized servlet label.
	 * Equivalent to calling {@link RestServlet#getLabel(RestRequest)} with this object.
	 *
	 * @return The localized servlet label.
	 */
	public String getServletLabel() {
		return servlet.getLabel(this);
	}

	/**
	 * Returns the localized servlet description.
	 * Equivalent to calling {@link RestServlet#getDescription(RestRequest)} with this object.
	 *
	 * @return The localized servlet description.
	 */
	public String getServletDescription() {
		return servlet.getDescription(this);
	}

	/**
	 * Returns the localized method description.
	 * Equivalent to calling {@link RestServlet#getMethodDescription(String, RestRequest)} with this object.
	 *
	 * @return The localized method description.
	 */
	public String getMethodDescription() {
		return servlet.getMethodDescription(javaMethod.getName(), this);
	}

	/**
	 * Returns the variable resolver for this request.
	 *
	 * @return The variable resolver for this request.
	 * @see RestServlet#createRequestVarResolver(RestRequest)
	 */
	public StringVarResolver getVarResolver() {
		if (varResolver == null)
			varResolver = servlet.createRequestVarResolver(this);
		return varResolver;
	}

	/**
	 * Resolves an <code>$X{}</code> variables in the specified input using
	 * the variable resolver returned by {@link #getVarResolver()}.
	 *
	 * @param input The input string to resolve variables in.
	 * @return The string with variables resolved, or <jk>null</jk> if input is null.
	 */
	public String resolveVars(String input) {
		return getVarResolver().resolve(input);
	}

	/**
	 * Returns an instance of a {@link ReaderResource} that represents the contents of a resource text file from the classpath.
	 * <p>
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars If <jk>true</jk>, any {@link StringVar} variables will be resolved by the variable resolver returned
	 * 	by {@link #getVarResolver()}.
	 * @param contentType The value to set as the <js>"Content-Type"</js> header for this object.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name, boolean resolveVars, String contentType) throws IOException {
		String s = servlet.getResourceAsString(name);
		if (s == null)
			return null;
		ReaderResource rr = new ReaderResource(s, contentType);
		if (resolveVars)
			rr.setVarResolver(getVarResolver());
		return rr;
	}

	/**
	 * Same as {@link #getReaderResource(String, boolean, String)} except uses {@link RestServlet#getMimetypesFileTypeMap()}
	 * to determine the media type.
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars If <jk>true</jk>, any {@link StringVar} variables will be resolved by the variable resolver returned
	 * 	by {@link #getVarResolver()}.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name, boolean resolveVars) throws IOException {
		return getReaderResource(name, resolveVars, servlet.getMimetypesFileTypeMap().getContentType(name));
	}

	/**
	 * Same as {@link #getReaderResource(String, boolean)} with <code>resolveVars == <jk>false</jk></code>
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getReaderResource(String name) throws IOException {
		return getReaderResource(name, false, servlet.getMimetypesFileTypeMap().getContentType(name));
	}

	/**
	 * Returns the config file associated with the servlet.
	 *
	 * @return The config file associated with the servlet, or <jk>null</jk> if servlet does not have a config file associated with it.
	 */
	public ConfigFile getConfig() {
		if (cf == null)
			cf = servlet.getConfig().getResolving(getVarResolver());
		return cf;
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
		if (method.equals("PUT") || method.equals("POST")) {
			sb.append("---Content---\n");
			try {
				sb.append(getInputAsString()).append("\n");
			} catch (Exception e1) {
				sb.append(e1.getLocalizedMessage());
				servlet.log(WARNING, e1, "Error occurred while trying to read debug input.");
			}
		}
		return sb.toString();
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
}