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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static java.util.Collections.*;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.util.RestUtils;
import org.apache.juneau.urlencoding.*;

/**
 * A mutable implementation of {@link HttpServletRequest} for mocking purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
public class MockServletRequest implements HttpServletRequest {

	private String method = "GET";
	private Map<String,String[]> queryDataMap = map();
	private Map<String,String[]> formDataMap;
	private Map<String,String[]> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String,Object> attributeMap = map();
	private String characterEncoding = "UTF-8";
	private byte[] content = new byte[0];
	private String protocol = "HTTP/1.1";
	private String scheme = "http";
	private String serverName = "localhost";
	private int serverPort = 8080;
	private String remoteAddr = "";
	private String remoteHost = "";
	private Locale locale = Locale.ENGLISH;
	private int remotePort;
	private String localName;
	private String localAddr;
	private int localPort;
	private Map<String,RequestDispatcher> requestDispatcher = map();
	private ServletContext servletContext;
	private DispatcherType dispatcherType;
	private String authType;
	private Cookie[] cookies;
	private String pathInfo;
	private String pathTranslated;
	private String contextPath = "";
	private String queryString;
	private String remoteUser;
	private Principal userPrincipal;
	private String requestedSessionId;
	private String requestURI;
	private String servletPath = "";
	private HttpSession httpSession = MockHttpSession.create();
	private String uri = "";
	private Set<String> roles = set();

	/**
	 * Creates a new servlet request.
	 *
	 * Initialized with the following:
	 * <ul>
	 * 	<li><c>"Accept: text/json5"</c>
	 * 	<li><c>"Content-Type: text/json"</c>
	 * </ul>
	 *
	 * @return A new request.
	 */
	public static MockServletRequest create() {
		MockServletRequest r = new MockServletRequest();
		return r;
	}

	/**
	 * Creates a new servlet request with the specified method name and request path.
	 *
	 * Initialized with the following:
	 * <ul>
	 * 	<li><c>"Accept: text/json5"</c>
	 * 	<li><c>"Content-Type: text/json"</c>
	 * </ul>
	 *
	 * @param method The HTTP method  name.
	 * @param uri The request path.
	 * @param pathArgs Optional path arguments.
	 *
	 * @return A new request.
	 */
	public static MockServletRequest create(String method, String uri, Object...pathArgs) {
		return create()
			.method(method)
			.uri(StringUtils.format(uri, pathArgs));
	}

	/**
	 * Fluent setter.
	 *
	 * @param uri The URI of the request.
	 * @return This object.
	 */
	public MockServletRequest uri(String uri) {
		uri = emptyIfNull(uri);
		this.uri = uri;

		if (uri.indexOf('?') != -1) {
			String qs = uri.substring(uri.indexOf('?') + 1);
			if (qs.indexOf('#') != -1)
				qs = qs.substring(0, qs.indexOf('#'));
			queryString = qs;
			queryDataMap.putAll(RestUtils.parseQuery(qs));
		}

		return this;
	}

	/**
	 * Adds the specified roles on this request.
	 *
	 * @param roles The roles to add to this request (e.g. <js>"ROLE_ADMIN"</js>).
	 * @return This object.
	 */
	public MockServletRequest roles(String...roles) {
		this.roles = set(roles);
		return this;
	}

	/**
	 * Adds the specified parent path variables to this servlet request.
	 *
	 * <p>
	 * See {@link MockRestClient.Builder#pathVars(Map)} for an example.
	 *
	 * @param pathVars The
	 * @return This object.
	 * @see MockRestClient.Builder#pathVars(Map)
	 */
	public MockServletRequest pathVars(Map<String,String> pathVars) {
		if (pathVars != null)
			this.attributeMap.put("juneau.pathVars", new TreeMap<>(pathVars));
		return this;
	}

	/**
	 * Add resolved path variables to this client.
	 *
	 * <p>
	 * Identical to {@link #pathVars(Map)} but allows you to specify as a list of key/value pairs.
	 *
	 * @param pairs The key/value pairs.  Must be an even number of parameters.
	 * @return This object.
	 */
	public MockServletRequest pathVars(String...pairs) {
		return pathVars(mapBuilder(String.class,String.class).addPairs((Object[])pairs).build());
	}

	/**
	 * Adds the specified role on this request.
	 *
	 * @param role The role to add to this request (e.g. <js>"ROLE_ADMIN"</js>).
	 * @return This object.
	 */
	public MockServletRequest role(String role) {
		this.roles = set(role);
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The method name for this request.
	 * @return This object.
	 */
	public MockServletRequest method(String value) {
		this.method = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The character encoding.
	 * @return This object.
	 */
	public MockServletRequest characterEncoding(String value) {
		this.characterEncoding = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The protocol.
	 * @return This object.
	 */
	public MockServletRequest protocol(String value) {
		this.protocol = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The scheme.
	 * @return This object.
	 */
	public MockServletRequest scheme(String value) {
		this.scheme = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The server name.
	 * @return This object.
	 */
	public MockServletRequest serverName(String value) {
		this.serverName = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The server port.
	 * @return This object.
	 */
	public MockServletRequest serverPort(int value) {
		this.serverPort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote address.
	 * @return This object.
	 */
	public MockServletRequest remoteAddr(String value) {
		this.remoteAddr = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote port.
	 * @return This object.
	 */
	public MockServletRequest remoteHost(String value) {
		this.remoteHost = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The locale.
	 * @return This object.
	 */
	public MockServletRequest locale(Locale value) {
		this.locale = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote port.
	 * @return This object.
	 */
	public MockServletRequest remotePort(int value) {
		this.remotePort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local name.
	 * @return This object.
	 */
	public MockServletRequest localName(String value) {
		this.localName = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local address.
	 * @return This object.
	 */
	public MockServletRequest localAddr(String value) {
		this.localAddr = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local port.
	 * @return This object.
	 */
	public MockServletRequest localPort(int value) {
		this.localPort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param name The path to resolve.
	 * @param value The request dispatcher.
	 * @return This object.
	 */
	public MockServletRequest requestDispatcher(String name, RequestDispatcher value) {
		this.requestDispatcher.put(name, value);
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The servlet context.
	 * @return This object.
	 */
	public MockServletRequest servletContext(ServletContext value) {
		this.servletContext = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The dispatcher type.
	 * @return This object.
	 */
	public MockServletRequest dispatcherType(DispatcherType value) {
		this.dispatcherType = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The auth type.
	 * @return This object.
	 */
	public MockServletRequest authType(String value) {
		this.authType = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The cookies.
	 * @return This object.
	 */
	public MockServletRequest cookies(Cookie[] value) {
		this.cookies = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The path info.
	 * @return This object.
	 */
	public MockServletRequest pathInfo(String value) {
		this.pathInfo = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The path translated.
	 * @return This object.
	 */
	public MockServletRequest pathTranslated(String value) {
		this.pathTranslated = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The context path.
	 * @return This object.
	 */
	public MockServletRequest contextPath(String value) {
		this.contextPath = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The query string.
	 * @return This object.
	 */
	public MockServletRequest queryString(String value) {
		this.queryString = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote user.
	 * @return This object.
	 */
	public MockServletRequest remoteUser(String value) {
		this.remoteUser = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The user principal.
	 * @return This object.
	 */
	public MockServletRequest userPrincipal(Principal value) {
		this.userPrincipal = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The requested session ID.
	 * @return This object.
	 */
	public MockServletRequest requestedSessionId(String value) {
		this.requestedSessionId = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The request URI.
	 * @return This object.
	 */
	public MockServletRequest requestURI(String value) {
		this.requestURI = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The servlet path.
	 * @return This object.
	 */
	public MockServletRequest servletPath(String value) {
		this.servletPath = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The HTTP session.
	 * @return This object.
	 */
	public MockServletRequest httpSession(HttpSession value) {
		this.httpSession = value;
		return this;
	}

	@Override /* HttpServletRequest */
	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributeMap.keySet());
	}

	@Override /* HttpServletRequest */
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override /* HttpServletRequest */
	public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException {
		this.characterEncoding = characterEncoding;
	}

	@Override /* HttpServletRequest */
	public int getContentLength() {
		return content == null ? 0 : content.length;
	}

	@Override /* HttpServletRequest */
	public long getContentLengthLong() {
		return content == null ? 0 : content.length;
	}

	@Override /* HttpServletRequest */
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override /* HttpServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		if (formDataMap != null)
			content = UrlEncodingSerializer.DEFAULT.toString(formDataMap).getBytes();
		return new BoundedServletInputStream(new ByteArrayInputStream(content), Integer.MAX_VALUE);
	}

	@Override /* HttpServletRequest */
	public String getParameter(String name) {
		String[] s = getParameterMap().get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getParameterNames() {
		return enumeration(listFrom(getParameterMap().keySet()));
	}

	@Override /* HttpServletRequest */
	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override /* HttpServletRequest */
	public Map<String,String[]> getParameterMap() {
		if ("POST".equalsIgnoreCase(method)) {
			if (formDataMap == null)
				formDataMap = RestUtils.parseQuery(read(content));
			return formDataMap;
		}
		return queryDataMap;
	}

	@Override /* HttpServletRequest */
	public String getProtocol() {
		return protocol;
	}

	@Override /* HttpServletRequest */
	public String getScheme() {
		return scheme;
	}

	@Override /* HttpServletRequest */
	public String getServerName() {
		return serverName;
	}

	@Override /* HttpServletRequest */
	public int getServerPort() {
		return serverPort;
	}

	@Override /* HttpServletRequest */
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
	}

	@Override /* HttpServletRequest */
	public String getRemoteAddr() {
		return remoteAddr;
	}

	@Override /* HttpServletRequest */
	public String getRemoteHost() {
		return remoteHost;
	}

	@Override /* HttpServletRequest */
	public void setAttribute(String name, Object o) {
		this.attributeMap.put(name, o);
	}

	@Override /* HttpServletRequest */
	public void removeAttribute(String name) {
		this.attributeMap.remove(name);
	}

	@Override /* HttpServletRequest */
	public Locale getLocale() {
		return locale;
	}

	@Override /* HttpServletRequest */
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(alist(locale));
	}

	@Override /* HttpServletRequest */
	public boolean isSecure() {
		return false;
	}

	@Override /* HttpServletRequest */
	public RequestDispatcher getRequestDispatcher(String path) {
		return requestDispatcher.get(path);
	}

	@Override /* HttpServletRequest */
	public String getRealPath(String path) {
		return path;
	}

	@Override /* HttpServletRequest */
	public int getRemotePort() {
		return remotePort;
	}

	@Override /* HttpServletRequest */
	public String getLocalName() {
		return localName;
	}

	@Override /* HttpServletRequest */
	public String getLocalAddr() {
		return localAddr;
	}

	@Override /* HttpServletRequest */
	public int getLocalPort() {
		return localPort;
	}

	@Override /* HttpServletRequest */
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override /* HttpServletRequest */
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override /* HttpServletRequest */
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		return null;
	}

	@Override /* HttpServletRequest */
	public boolean isAsyncStarted() {
		return false;
	}

	@Override /* HttpServletRequest */
	public boolean isAsyncSupported() {
		return false;
	}

	@Override /* HttpServletRequest */
	public AsyncContext getAsyncContext() {
		return null;
	}

	@Override /* HttpServletRequest */
	public DispatcherType getDispatcherType() {
		return dispatcherType;
	}

	@Override /* HttpServletRequest */
	public String getAuthType() {
		return authType;
	}

	@Override /* HttpServletRequest */
	public Cookie[] getCookies() {
		return cookies;
	}

	@Override /* HttpServletRequest */
	public long getDateHeader(String name) {
		String s = getHeader(name);
		return s == null ? 0 : date(s).asZonedDateTime().get().toInstant().toEpochMilli();
	}

	@Override /* HttpServletRequest */
	public String getHeader(String name) {
		String[] s = headerMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] s = headerMap.get(name);
		return Collections.enumeration(alist(s == null ? new String[0] : s));
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(headerMap.keySet());
	}

	@Override /* HttpServletRequest */
	public int getIntHeader(String name) {
		String s = getHeader(name);
		return s == null || s.isEmpty() ? 0 : Integer.parseInt(s);
	}

	@Override /* HttpServletRequest */
	public String getMethod() {
		return method;
	}

	@Override /* HttpServletRequest */
	public String getPathInfo() {
		if (pathInfo == null) {
			pathInfo = getRequestURI();
			if (isNotEmpty(contextPath))
				pathInfo = pathInfo.substring(contextPath.length());
			if (isNotEmpty(servletPath))
				pathInfo = pathInfo.substring(servletPath.length());
		}
		return nullIfEmpty(urlDecode(pathInfo));
	}

	@Override /* HttpServletRequest */
	public String getPathTranslated() {
		if (pathTranslated == null)
			pathTranslated = "/mock-path" + getPathInfo();
		return pathTranslated;
	}

	@Override /* HttpServletRequest */
	public String getContextPath() {
		return contextPath;
	}

	@Override /* HttpServletRequest */
	public String getQueryString() {
		if (queryString == null) {
			if (queryDataMap.isEmpty())
				queryString = "";
			else {
				StringBuilder sb = new StringBuilder();
				queryDataMap.forEach((k,v) -> {
					if (v == null)
						sb.append(sb.length() == 0 ? "" : "&").append(urlEncode(k));
					else for (String v2 : v)
						sb.append(sb.length() == 0 ? "" : "&").append(urlEncode(k)).append('=').append(urlEncode(v2));
				});
				queryString = sb.toString();
			}
		}
		return isEmpty(queryString) ? null : queryString;
	}

	@Override /* HttpServletRequest */
	public String getRemoteUser() {
		return remoteUser;
	}

	@Override /* HttpServletRequest */
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

	@Override /* HttpServletRequest */
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	@Override /* HttpServletRequest */
	public String getRequestedSessionId() {
		return requestedSessionId;
	}

	@Override /* HttpServletRequest */
	public String getRequestURI() {
		if (requestURI == null) {
			requestURI = uri;
			requestURI = requestURI.replaceAll("^\\w+\\:\\/\\/[^\\/]+", "").replaceAll("\\?.*$", "");
		}
		return requestURI;
	}

	@Override /* HttpServletRequest */
	public StringBuffer getRequestURL() {
		return new StringBuffer(uri.replaceAll("\\?.*$", ""));
	}

	@Override /* HttpServletRequest */
	public String getServletPath() {
		return servletPath;
	}

	@Override /* HttpServletRequest */
	public HttpSession getSession(boolean create) {
		return httpSession;
	}

	@Override /* HttpServletRequest */
	public HttpSession getSession() {
		return httpSession;
	}

	@Override /* HttpServletRequest */
	public String changeSessionId() {
		return null;
	}

	@Override /* HttpServletRequest */
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override /* HttpServletRequest */
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override /* HttpServletRequest */
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override /* HttpServletRequest */
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	@Override /* HttpServletRequest */
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		return false;
	}

	@Override /* HttpServletRequest */
	public void login(String username, String password) throws ServletException {
	}

	@Override /* HttpServletRequest */
	public void logout() throws ServletException {
	}

	@Override /* HttpServletRequest */
	public Collection<Part> getParts() throws IOException, ServletException {
		return null;
	}

	@Override /* HttpServletRequest */
	public Part getPart(String name) throws IOException, ServletException {
		return null;
	}

	@Override /* HttpServletRequest */
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		return null;
	}

	//=================================================================================================================
	// Convenience methods
	//=================================================================================================================

	/**
	 * Fluent setter.
	 *
	 * @param name Header name.
	 * @param value
	 * 	Header value.
	 * 	<br>The value is converted to a simple string using {@link Object#toString()}.
	 * @return This object.
	 */
	public MockServletRequest header(String name, Object value) {
		if (value != null) {
			String[] v1 = (value instanceof String[]) ? (String[])value : new String[]{value.toString()};
			String[] v2 = headerMap.get(name);
			String[] v3 = ArrayUtils.combine(v2, v1);
			headerMap.put(name, v3);
		}
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param name Request attribute name.
	 * @param value Request attribute value.
	 * @return This object.
	 */
	public MockServletRequest attribute(String name, Object value) {
		this.attributeMap.put(name, value);
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value
	 * 	The body of the request.
	 * 	<br>Can be any of the following data types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link Reader}
	 * 		<li>{@link InputStream}
	 * 		<li>{@link CharSequence}
	 * 	</ul>
	 * 	Any other types are converted to a string using the <c>toString()</c> method.
	 * @return This object.
	 */
	public MockServletRequest content(Object value) {
		try {
			if (value instanceof byte[])
				this.content = (byte[])value;
			else if (value instanceof Reader)
				this.content = readBytes((Reader)value);
			else if (value instanceof InputStream)
				this.content = readBytes((InputStream)value);
			else if (value instanceof CharSequence)
				this.content = ((CharSequence)value).toString().getBytes();
			else if (value != null)
				this.content = value.toString().getBytes();
		} catch (IOException e) {
			throw asRuntimeException(e);
		}
		return this;
	}

	//=================================================================================================================
	// Convenience methods - headers
	//=================================================================================================================

	/**
	 * Enabled debug mode on this request.
	 *
	 * <p>
	 * Causes information about the request execution to be sent to STDERR.
	 *
	 * @param value The enable flag value.
	 * @return This object.
	 */
	protected MockServletRequest debug(boolean value) {
		if (value)
			header("Debug", "true");
		return this;
	}

	/**
	 * Enabled debug mode on this request.
	 *
	 * <p>
	 * Prevents errors from being logged on the server side if no-trace per-request is enabled.
	 *
	 * @param value The enable flag value.
	 * @return This object.
	 */
	public MockServletRequest noTrace(boolean value) {
		if (value)
			header("No-Trace", "true");
		return this;
	}

	/**
	 * If the specified request is a {@link MockRestRequest}, applies any of the override values to this servlet request.
	 *
	 * @param req The request to copy overrides from.
	 * @return This object.
	 */
	public MockServletRequest applyOverrides(HttpRequest req) {

		if (req instanceof MockRestRequest) {
			MockRestRequest mreq = (MockRestRequest)req;
			mreq.getAttributeMap().forEach((k,v) -> attribute(k, v));
			mreq.getRequestDispatcherMap().forEach((k,v) -> requestDispatcher(k, v));
			if (mreq.getCharacterEncoding() != null)
				characterEncoding(mreq.getCharacterEncoding());
			if (mreq.getProtocol() != null)
				protocol(mreq.getProtocol());
			if (mreq.getScheme() != null)
				scheme(mreq.getScheme());
			if (mreq.getServerName() != null)
				serverName(mreq.getServerName());
			if (mreq.getRemoteAddr() != null)
				remoteAddr(mreq.getRemoteAddr());
			if (mreq.getRemoteHost() != null)
				remoteHost(mreq.getRemoteHost());
			if (mreq.getLocalName() != null)
				localName(mreq.getLocalName());
			if (mreq.getLocalAddr() != null)
				localAddr(mreq.getLocalAddr());
			if (mreq.getPathInfo() != null)
				pathInfo(mreq.getPathInfo());
			if (mreq.getPathTranslated() != null)
				pathTranslated(mreq.getPathTranslated());
			if (mreq.getContextPath() != null)
				contextPath(mreq.getContextPath());
			if (mreq.getQueryString() != null)
				queryString(mreq.getQueryString());
			if (mreq.getRemoteUser() != null)
				remoteUser(mreq.getRemoteUser());
			if (mreq.getRequestedSessionId() != null)
				requestedSessionId(mreq.getRequestedSessionId());
			if (mreq.getRequestURI() != null)
				requestURI(mreq.getRequestURI());
			if (mreq.getServletPath() != null)
				servletPath(mreq.getServletPath());
			if (mreq.getAuthType() != null)
				authType(mreq.getAuthType());
			if (mreq.getServerPort() != null)
				serverPort(mreq.getServerPort());
			if (mreq.getRemotePort() != null)
				remotePort(mreq.getRemotePort());
			if (mreq.getLocalPort() != null)
				localPort(mreq.getLocalPort());
			if (mreq.getLocale() != null)
				locale(mreq.getLocale());
			if (mreq.getServletContext() != null)
				servletContext(mreq.getServletContext());
			if (mreq.getDispatcherType() != null)
				dispatcherType(mreq.getDispatcherType());
			if (mreq.getCookies() != null)
				cookies(mreq.getCookies());
			if (mreq.getUserPrincipal() != null)
				userPrincipal(mreq.getUserPrincipal());
			if (mreq.getHttpSession() != null)
				httpSession(mreq.getHttpSession());
			if (mreq.getRoles() != null)
				roles(mreq.getRoles());
		}

		return this;
	}
}
