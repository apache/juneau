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
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

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
import org.apache.juneau.html.*;
import org.apache.juneau.http.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * Represents an HTTP request for a REST resource.
 *
 * <p>
 * Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 *
 * <p>
 * For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * following methods return the following values....
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
 *
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#TOC">REST Servlet API</a> for information about using this
 * class.
 */
@SuppressWarnings("unchecked")
public final class RestRequest extends HttpServletRequestWrapper {

	private final RestContext context;

	private final String method, stylesheet;
	private RequestBody body;
	private Method javaMethod;
	private ObjectMap properties;
	private SerializerGroup serializerGroup;
	private ParserGroup parserGroup;
	private final boolean debug;
	private UrlEncodingParser urlEncodingParser;   // The parser used to parse URL attributes and parameters (beanContext also used to parse headers)
	private BeanSession beanSession;
	private VarResolverSession varSession;
	private final RequestQuery queryParams;
	private RequestFormData formData;
	private Map<String,String> defFormData;
	private RequestPathMatch pathParams;
	private boolean isPost;
	private UriContext uriContext;
	private String charset, defaultCharset;
	private RequestHeaders headers;
	private ConfigFile cf;
	private Swagger swagger, fileSwagger;
	private Map<String,Widget> widgets;

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
			queryParams = new RequestQuery();
			if (isPost)
				context.getUrlEncodingParser().parseIntoSimpleMap(getQueryString(), queryParams);
			else
				queryParams.putAll(req.getParameterMap());


			// Get the HTTP method.
			// Can be overridden through a "method" GET attribute.
			String _method = super.getMethod();

			String m = getQuery().getString("method");
			if (context.allowMethodParam(m))
				_method = m;

			method = _method;

			String _stylesheet = getQuery().getString("stylesheet");
			if (_stylesheet != null)
				req.getSession().setAttribute("stylesheet", _stylesheet);
			stylesheet = (String)req.getSession().getAttribute("stylesheet");

			headers = new RequestHeaders();
			for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
				String name = e.nextElement();
				headers.put(name, super.getHeaders(name));
			}

			body = new RequestBody(this);

			if (context.isAllowBodyParam()) {
				String b = getQuery().getString("body");
				if (b != null) {
					headers.put("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
					body.load(b.getBytes(UTF8));
				}
			}

			if (context.isAllowHeaderParams())
				headers.setQueryParams(queryParams);

			debug = "true".equals(getQuery().getString("debug", "false")) || "true".equals(getHeaders().getString("Debug", "false"));

			this.pathParams = new RequestPathMatch();

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
	final void init(Method javaMethod, ObjectMap properties, Map<String,String> defHeader,
			Map<String,String> defQuery, Map<String,String> defFormData, String defaultCharset,
			SerializerGroup mSerializers, ParserGroup mParsers, UrlEncodingParser mUrlEncodingParser,
			BeanContext beanContext, EncoderGroup encoders, Map<String,Widget> widgets) {
		this.javaMethod = javaMethod;
		this.properties = properties;
		this.urlEncodingParser = mUrlEncodingParser;
		this.beanSession = beanContext.createSession();
		this.pathParams
			.setParser(urlEncodingParser)
			.setBeanSession(beanSession);
		this.queryParams
			.addDefault(defQuery)
			.setParser(urlEncodingParser)
			.setBeanSession(beanSession);
		this.headers
			.addDefault(defHeader)
			.addDefault(context.getDefaultRequestHeaders())
			.setParser(urlEncodingParser)
			.setBeanSession(beanSession);
		this.body
			.setEncoders(encoders)
			.setParsers(mParsers)
			.setHeaders(headers)
			.setBeanSession(beanSession)
			.setUrlEncodingParser(mUrlEncodingParser);
		this.serializerGroup = mSerializers;
		this.parserGroup = mParsers;
		this.defaultCharset = defaultCharset;
		this.defFormData = defFormData;
		this.widgets = widgets;

		if (debug) {
			String msg = ""
				+ "\n=== HTTP Request (incoming) ===================================================="
				+ toString()
				+ "\n=== END ========================================================================";
			context.getLogger().log(Level.WARNING, msg);
		}

		if (isPlainText())
			this.properties.put(SerializerContext.SERIALIZER_useWhitespace, true);
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
	 * Same as {@link #getAttribute(String)} but returns a default value if not found.
	 *
	 * @param name The request attribute name.
	 * @param def The default value if the attribute doesn't exist.
	 * @return The request attribute value.
	 */
	public Object getAttribute(String name, Object def) {
		Object o = super.getAttribute(name);
		return (o == null ? def : o);
	}

	/**
	 * Shorthand method for calling {@link #setAttribute(String, Object)} fluently.
	 *
	 * @param name The request attribute name.
	 * @param value The request attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest attr(String name, Object value) {
		setAttribute(name, value);
		return this;
	}


	/**
	 * Resolves the specified property.
	 *
	 * @param cm
	 * 	The <code>CallMethod</code> object where the <code>HtmlDocSerializer</code> settings are defined.
	 * 	Optional value.  If not specified, then won't resolve <code>HtmlDocSerializer</code> properties.
	 * @param category
	 * 	The property category.
	 * 	The possible values are:
	 * 	<ul>
	 * 		<li>
	 * 			<js>"Attribute"</js> - Value returned by {@link HttpServletRequest#getAttribute(String)}.
	 * 		<li>
	 * 			<js>"FormData"</js> - Value returned by {@link RestRequest#getFormData(String)}.
	 * 		<li>
	 * 			<js>"Header"</js> - Value returned by {@link RestRequest#getHeader(String)}.
	 * 		<li>
	 * 			<js>"HtmlDocSerializer"</js>
	 * 			<br>Valid names:
	 * 			<ul>
	 * 				<li><js>"aside"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_aside}
	 * 				<li><js>"footer"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_footer}
	 * 				<li><js>"header"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_header}
	 * 				<li><js>"navlinks.list"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_navlinks}
	 * 				<li><js>"nav"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_nav}
	 * 				<li><js>"noResultsMessage"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_noResultsMessage}
	 * 				<li><js>"nowrap"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_nowrap}
	 * 				<li><js>"script.list"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_script}
	 * 				<li><js>"style.list"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_style}
	 * 				<li><js>"stylesheet"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_stylesheet}
	 * 				<li><js>"template"</js> - See {@link HtmlDocSerializerContext#HTMLDOC_template}
	 * 			</ul>
	 * 		<li>
	 * 			<js>"Path"</js> - Value returned by {@link RestRequest#getPath(String)}.
	 * 		<li>
	 * 			<js>"Query"</js> = Value returned by {@link RestRequest#getQuery(String)}.
	 * 		<li>
	 * 			<js>"Request"</js>
	 * 			<br>Valid names:
	 * 			<ul>
	 * 				<li><js>"contextPath"</js> - Value returned by {@link RestRequest#getContextPath()}
	 * 				<li><js>"method"</js> - Value returned by {@link RestRequest#getMethod()}
	 * 				<li><js>"methodDescription"</js> - Value returned by {@link RestRequest#getMethodDescription()}
	 * 				<li><js>"methodSummary"</js> - Value returned by {@link RestRequest#getMethodSummary()}
	 * 				<li><js>"pathInfo"</js> - Value returned by {@link RestRequest#getPathInfo()}
	 * 				<li><js>"requestParentURI"</js> - Value returned by {@link UriContext#getRootRelativePathInfoParent()}
	 * 				<li><js>"requestURI"</js> - Value returned by {@link RestRequest#getRequestURI()}
	 * 				<li><js>"servletClass"</js> - The class name of the servlet
	 * 				<li><js>"servletClassSimple"</js> - The simple class name of the servlet.
	 * 				<li><js>"servletDescription"</js> - Value returned by {@link RestRequest#getServletDescription()}
	 * 				<li><js>"servletParentURI"</js> - Value returned by {@link UriContext#getRootRelativeServletPathParent()}
	 * 				<li><js>"servletPath"</js> - See {@link RestRequest#getServletPath()}
	 * 				<li><js>"servletTitle"</js> - See {@link RestRequest#getServletTitle()}
	 * 				<li><js>"servletURI"</js> - See {@link UriContext#getRootRelativeServletPath()}
	 * 				<li><js>"siteName"</js> - See {@link RestRequest#getSiteName()}
	 * 			</ul>
	 * 	</ul>
	 * @param name The property name.
	 * @return The resolve property, or <jk>null</jk> if it wasn't found.
	 */
	public Object resolveProperty(CallMethod cm, String category, String name) {
		char c = category.charAt(0);
		if (c == 'A') {
			if ("Attribute".equals(category))
				return getAttribute(name);
		} else if (c == 'F') {
			if ("FormData".equals(category))
				return getFormData(name);
		} else if (c == 'H') {
			if ("Header".equals(category))
				return getHeader(name);
			if ("HtmlDocSerializer".equals(category) && cm != null) {
				char c2 = StringUtils.charAt(name, 0);
				if (c2 == 'a') {
					if ("aside".equals(name))
						return cm.htmlAside == null ? null : resolveVars(cm.htmlAside);
				} else if (c2 == 'f') {
					if ("footer".equals(name))
						return cm.htmlFooter == null ? null : resolveVars(cm.htmlFooter);
				} else if (c2 == 'h') {
					if ("header".equals(name))
						return cm.htmlHeader == null ? null : resolveVars(cm.htmlHeader);
				} else if (c2 == 'n') {
					if ("nav".equals(name))
						return cm.htmlNav == null ? null : resolveVars(cm.htmlNav);
					if ("navlinks.list".equals(name)) {
						if (cm.htmlNavLinks == null || cm.htmlNavLinks.length == 0)
							return null;
						try {
							List<String> la = new ArrayList<String>();
							for (String l : cm.htmlNavLinks) {
								// Temporary backwards compatibility with JSON object format.
								if (l.startsWith("{")) {
									ObjectMap m = new ObjectMap(l);
									for (Map.Entry<String,Object> e : m.entrySet())
										la.add(resolveVars(e.getKey()) + ":" + resolveVars(StringUtils.toString(e.getValue())));
								} else {
									la.add(resolveVars(l));
								}
							}
							return la;
						} catch (ParseException e) {
							throw new RuntimeException(e);
						}
					}
					if ("noResultsMessage".equals(name))
						return cm.htmlNoResultsMessage == null ? null : resolveVars(cm.htmlNoResultsMessage);
					if ("nowrap".equals(name))
						return cm.htmlNoWrap;
				} else if (c2 == 's') {
					if ("script.list".equals(name)) {
						Set<String> l = new LinkedHashSet<String>();
						if (cm.htmlScript != null)
							l.add(resolveVars(cm.htmlScript));
						for (Widget w : getWidgets().values()) {
							String script;
							try {
								script = w.getScript(this);
							} catch (Exception e) {
								script = e.getLocalizedMessage();
							}
							if (script != null)
								l.add(resolveVars(script));
						}
						return l;
					}
					if ("style.list".equals(name)) {
						Set<String> l = new LinkedHashSet<String>();
						if (cm.htmlStyle != null)
							l.add(resolveVars(cm.htmlStyle));
						for (Widget w : getWidgets().values()) {
							String style;
							try {
								style = w.getStyle(this);
							} catch (Exception e) {
								style = e.getLocalizedMessage();
							}
							if (style != null)
								l.add(resolveVars(style));
						}
						return l;
					}
					if ("stylesheet".equals(name)) {
						String s = getStylesheet();
						// Exclude absolute URIs to stylesheets for security reasons.
						if (s == null || isAbsoluteUri(s))
							s = cm.htmlStylesheet;
						return s == null ? null : resolveVars(s);
					}
				} else if (c2 == 't') {
					if ("template".equals(name))
						return cm.htmlTemplate;
				}
			}
		} else if (c == 'P') {
			if ("Path".equals(category))
				return getPath(name);
		} else if (c == 'Q') {
			if ("Query".equals(category))
				return getQuery(name);
		} else if (c == 'R') {
			if ("Request".equals(category)) {
				char c2 = StringUtils.charAt(name, 0);
				if (c2 == 'c') {
					if ("contextPath".equals(name))
						return getContextPath();
				} else if (c2 == 'm') {
					if ("method".equals(name))
						return getMethod();
					if ("methodDescription".equals(name))
						return getMethodDescription();
					if ("methodSummary".equals(name))
						return getMethodSummary();
				} else if (c2 == 'p') {
					if ("pathInfo".equals(name))
						return getPathInfo();
				} else if (c2 == 'r') {
					if ("requestParentURI".equals(name))
						return getUriContext().getRootRelativePathInfoParent();
					if ("requestURI".equals(name))
						return getRequestURI();
				} else if (c2 == 's') {
					if ("servletClass".equals(name))
						return getContext().getResource().getClass().getName();
					if ("servletClassSimple".equals(name))
						return getContext().getResource().getClass().getSimpleName();
					if ("servletDescription".equals(name))
						return getServletDescription();
					if ("servletParentURI".equals(name))
						return getUriContext().getRootRelativeServletPathParent();
					if ("servletPath".equals(name))
						return getServletPath();
					if ("servletTitle".equals(name))
						return getServletTitle();
					if ("servletURI".equals(name))
						return getUriContext().getRootRelativeServletPath();
					if ("siteName".equals(name))
						return getSiteName();
				}
			}
		}
		return null;
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
	 *
	 * <p>
	 * These properties can be modified by the request.
	 *
	 * @return The properties active for this request.
	 */
	public ObjectMap getProperties() {
		return this.properties;
	}

	/**
	 * Shortcut for calling <code>getProperties().append(name, value);</code> fluently.
	 *
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public RestRequest prop(String name, Object value) {
		this.properties.append(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Headers
	//--------------------------------------------------------------------------------

	/**
	 * Returns the headers on this request.
	 *
	 * @return The headers on this request.  Never <jk>null</jk>.
	 */
	public RequestHeaders getHeaders() {
		return headers;
	}

	@Override /* ServletRequest */
	public String getHeader(String name) {
		return getHeaders().getString(name);
	}

	@Override /* ServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] v = headers.get(name);
		if (v == null || v.length == 0)
			return Collections.enumeration(Collections.EMPTY_LIST);
		return Collections.enumeration(Arrays.asList(v));
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
		String h = headers.getString("Accept-Language");
		if (h != null) {
			MediaTypeRange[] mr = MediaTypeRange.parse(h);
			if (mr.length > 0)
				return toLocale(mr[0].getMediaType().getType());
		}
		return super.getLocale();
	}

	@Override /* ServletRequest */
	public Enumeration<Locale> getLocales() {
		String h = headers.getString("Accept-Language");
		if (h != null) {
			MediaTypeRange[] mr = MediaTypeRange.parse(h);
			if (mr.length > 0) {
				List<Locale> l = new ArrayList<Locale>(mr.length);
				for (MediaTypeRange r : mr)
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
	 * Equivalent to {@link #getParameterMap()}, but only looks for query parameters in the URL, not form posts.
	 *
	 * <p>
	 * This method can be used to retrieve query parameters without triggering the underlying servlet API to load and
	 * parse the request body.
	 *
	 * <p>
	 * This object is modifiable.
	 *
	 * @return The query parameters as a modifiable map.
	 */
	public RequestQuery getQuery() {
		return queryParams;
	}

	/**
	 * Shortcut for calling <code>getQuery().getString(name)</code>.
	 *
	 * @param name The query parameter name.
	 * @return The query parameter value, or <jk>null<jk> if not found.
	 */
	public String getQuery(String name) {
		return getQuery().getString(name);
	}


	//--------------------------------------------------------------------------------
	// Form data parameters
	//--------------------------------------------------------------------------------

	/**
	 * Retrieves the URL-encoded form data from the request if the body has already been cached locally.
	 *
	 * @return The URL-encoded form data from the request.
	 */
	public RequestFormData getFormData() {
		try {
			if (formData == null) {
				formData = new RequestFormData();
				formData.setParser(urlEncodingParser).setBeanSession(beanSession);
				if (! body.isLoaded()) {
					formData.putAll(getParameterMap());
				} else {
					Map<String,String> m = urlEncodingParser.parse(body.getReader(), Map.class, String.class, String.class);
					for (Map.Entry<String,String> e : m.entrySet()) {
						formData.put(e.getKey(), e.getValue());
					}
				}
			}
			formData.addDefault(defFormData);
			return formData;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Shortcut for calling <code>getFormData().getString(name)</code>.
	 *
	 * @param name The form data parameter name.
	 * @return The form data parameter value, or <jk>null<jk> if not found.
	 */
	public String getFormData(String name) {
		return getFormData().getString(name);
	}


	//--------------------------------------------------------------------------------
	// Path parameters
	//--------------------------------------------------------------------------------

	/**
	 * Retrieves the URL-encoded form data from the request if the body has already been cached locally.
	 *
	 * @return The URL-encoded form data from the request.
	 */
	public RequestPathMatch getPathMatch() {
		return pathParams;
	}

	/**
	 * Shortcut for calling <code>getPathMatch().get(name)</code>.
	 *
	 * @param name The path variable name.
	 * @return The path variable value, or <jk>null<jk> if not found.
	 */
	public String getPath(String name) {
		return getPathMatch().get(name);
	}

	//--------------------------------------------------------------------------------
	// Body methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the body of this HTTP request.
	 *
	 * @return The body of this HTTP request.
	 */
	public RequestBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP body content as a {@link Reader}.
	 *
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query
	 * string.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 */
	@Override /* ServletRequest */
	public BufferedReader getReader() throws IOException {
		return getBody().getReader();
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 *
	 * <p>
	 * Automatically handles GZipped input streams.
	 *
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it in the GZIP wrapper.
	 */
	@Override /* ServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		return getBody().getInputStream();
	}

	ServletInputStream getRawInputStream() throws IOException {
		return super.getInputStream();
	}


	//--------------------------------------------------------------------------------
	// URI-related methods
	//--------------------------------------------------------------------------------

	@Override /* HttpServletRequest */
	public String getContextPath() {
		String cp = context.getContextPath();
		return cp == null ? super.getContextPath() : cp;
	}

	@Override /* HttpServletRequest */
	public String getServletPath() {
		String cp = context.getContextPath();
		String sp = super.getServletPath();
		return cp == null || ! sp.startsWith(cp) ? sp : sp.substring(cp.length());
	}

	/**
	 * Returns the URI context of the request.
	 *
	 * <p>
	 * The URI context contains all the information about the URI of the request, such as the servlet URI, context
	 * path, etc...
	 *
	 * @return The URI context of the request.
	 */
	public UriContext getUriContext() {
		if (uriContext == null) {
			String scheme = getScheme();
			int port = getServerPort();
			StringBuilder authority = new StringBuilder(getScheme()).append("://").append(getServerName());
			if (! (port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)))
				authority.append(':').append(port);
			uriContext = new UriContext(authority.toString(), getContextPath(), getServletPath(), super.getPathInfo());
		}
		return uriContext;
	}

	/**
	 * Returns a URI resolver that can be used to convert URIs to absolute or root-relative form.
	 *
	 * @param resolution The URI resolution rule.
	 * @param relativity The relative URI relativity rule.
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver(UriResolution resolution, UriRelativity relativity) {
		return new UriResolver(resolution, relativity, getUriContext());
	}

	/**
	 * Shortcut for calling {@link #getUriResolver()} using {@link UriResolution#ROOT_RELATIVE} and
	 * {@link UriRelativity#RESOURCE}
	 *
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver() {
		return new UriResolver(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, getUriContext());
	}

	/**
	 * Returns the URI for this request.
	 *
	 * <p>
	 * Similar to {@link #getRequestURI()} but returns the value as a {@link URI}.
	 * It also gives you the capability to override the query parameters (e.g. add new query parameters to the existing
	 * URI).
	 *
	 * @param includeQuery If <jk>true</jk> include the query parameters on the request.
	 * @param addQueryParams Augment the request URI with the specified query parameters.
	 * @return A new URI.
	 */
	public URI getUri(boolean includeQuery, Map<String,?> addQueryParams) {
		String uri = getRequestURI();
		if (includeQuery || addQueryParams != null) {
			StringBuilder sb = new StringBuilder(uri);
			RequestQuery rq = this.queryParams.copy();
			if (addQueryParams != null)
				for (Map.Entry<String,?> e : addQueryParams.entrySet())
					rq.put(e.getKey(), e.getValue());
			if (! rq.isEmpty())
				sb.append('?').append(rq.toQueryString());
			uri = sb.toString();
		}
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			// Shouldn't happen.
			throw new RuntimeException(e);
		}
	}

	//--------------------------------------------------------------------------------
	// Labels
	//--------------------------------------------------------------------------------

	/**
	 * Returns the localized site name.
	 *
	 * <p>
	 * The site name is intended to be a title that can be applied to the entire site.
	 *
	 * <p>
	 * One possible use is if you want to add the same title to the top of all pages by defining a header on a
	 * common parent class like so:
	 * <p class='bcode'>
	 * 	htmldoc=<ja>@HtmlDoc</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$R{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$R{servletTitle}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 *
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getSiteName(RestRequest)} with this object.
	 *
	 * @return The localized servlet label.
	 */
	public String getSiteName() {
		return context.getInfoProvider().getSiteName(this);
	}

	/**
	 * Returns the localized servlet title.
	 *
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
	 *
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
	 *
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
	 *
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getMethodDescription(String, RestRequest)} with this object.
	 *
	 * @return The localized method description.
	 */
	public String getMethodDescription() {
		return context.getInfoProvider().getMethodDescription(javaMethod.getName(), this);
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the value of the <jk>"stylesheet"</js> parameter.
	 *
	 * @return The value of the <jk>"stylesheet"</js> parameter, or <jk>null</jk> if it wasn't specified.
	 */
	protected String getStylesheet() {
		return stylesheet;
	}

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
	 * Returns the method of this request.
	 *
	 * <p>
	 * If <code>allowHeaderParams</code> init parameter is <jk>true</jk>, then first looks for
	 * <code>&amp;method=xxx</code> in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the HTTP 1.1 method name of the request as an enum.
	 *
	 * <p>
	 * Note that non-RFC2616 method names resolve as {@link HttpMethod#OTHER}.
	 *
	 * @return The HTTP method.
	 */
	public HttpMethod getHttpMethod() {
		return HttpMethod.forString(method);
	}

	@Override /* ServletRequest */
	public int getContentLength() {
		return getBody().getContentLength();
	}

	int getRawContentLength() {
		return super.getContentLength();
	}

	/**
	 * Returns <jk>true</jk> if <code>&amp;plainText=true</code> was specified as a URL parameter.
	 *
	 * <p>
	 * This indicates that the <code>Content-Type</code> of the output should always be set to <js>"text/plain"</js>
	 * to make it easy to render in a browser.
	 *
	 * <p>
	 * This feature is useful for debugging.
	 *
	 * @return <jk>true</jk> if {@code &amp;plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(getQuery().getString("plainText", "false"));
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
	 *
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}..
	 *
	 * @return The servlet handling the request.
	 */
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns the java method handling the request.
	 *
	 * <p>
	 * Can be used to access the method name or method annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This returns null when evaluating servlet-level guards since the method has not been resolved at that
	 * 		point of execution.
	 * </ul>
	 *
	 * @return The Java method handling the request, or <code>null</code> if the method has not yet been resolved.
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
	 * Returns the variable resolver session for this request using session objects created by
	 * {@link RestCallHandler#getSessionObjects(RestRequest)}.
	 *
	 * <p>
	 * See {@link RestContext#getVarResolver()} for the list of supported variables.
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
	 * Returns an instance of a {@link ReaderResource} that represents the contents of a resource text file from the
	 * classpath.
	 *
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars
	 * 	If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter} variables will be
	 * 	resolved by the variable resolver returned by {@link #getVarResolverSession()}.
	 * 	<br>See {@link RestContext#getVarResolver()} for the list of supported variables.
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
	 * @param resolveVars
	 * 	If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter} variables will be
	 * 	resolved by the variable resolver returned by {@link #getVarResolverSession()}.
	 * 	<br>See {@link RestContext#getVarResolver()} for the list of supported variables.
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
	 * @return
	 * 	The config file associated with the servlet, or <jk>null</jk> if servlet does not have a config file
	 * 	associated with it.
	 */
	public ConfigFile getConfigFile() {
		if (cf == null)
			cf = context.getConfigFile().getResolving(getVarResolverSession());
		return cf;
	}

	/**
	 * Returns the localized swagger associated with the servlet.
	 *
	 * @return
	 * 	The swagger associated with the servlet.
	 * 	Never <jk>null</jk>.
	 */
	public Swagger getSwagger() {
		if (swagger == null)
			swagger = context.getInfoProvider().getSwagger(this);
		return swagger;
	}

	/**
	 * Returns the widgets used for resolving <js>"$W{...}"</js> string variables.
	 *
	 * @return
	 * 	The widgets used for resolving <js>"$W{...}"</js> string variables.
	 * 	Never <jk>null</jk>.
	 */
	public Map<String,Widget> getWidgets() {
		return widgets;
	}

	/**
	 * Returns the localized Swagger from the file system.
	 *
	 * <p>
	 * Looks for a file called <js>"{ServletClass}_{locale}.json"</js> in the same package as this servlet and returns
	 * it as a parsed {@link Swagger} object.
	 *
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
		for (Map.Entry<String,String> e : context.getDefaultRequestHeaders().entrySet()) {
			sb.append("\t").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
		}
		if (javaMethod == null) {
			sb.append("***init() not called yet!***\n");
		} else if (method.equals("PUT") || method.equals("POST")) {
			try {
				sb.append("---Body UTF-8---\n");
				sb.append(body.asString()).append("\n");
				sb.append("---Body Hex---\n");
				sb.append(body.asHex()).append("\n");
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


	void setJavaMethod(Method method) {
		this.javaMethod = method;
	}
}