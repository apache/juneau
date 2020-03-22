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

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.rest.util.RestUtils;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;

/**
 * An implementation of {@link HttpServletRequest} for mocking purposes.
 *
 * <div class='warn'>
 * 	<b>Deprecated</b> - Use <c>org.apache.juneau.restmock2</c>
 * </div>
 */
@Deprecated
public class MockServletRequest implements HttpServletRequest, MockHttpRequest {

	private String method = "GET";
	private Map<String,String[]> queryData;
	private Map<String,String[]> formDataMap;
	private Map<String,String[]> headerMap = new LinkedHashMap<>();
	private Map<String,Object> attributeMap = new LinkedHashMap<>();
	private String characterEncoding = "UTF-8";
	private byte[] body = new byte[0];
	private String protocol = "HTTP/1.1";
	private String scheme = "http";
	private String serverName = "localhost";
	private int serverPort = 8080;
	private String remoteAddr = "";
	private String remoteHost = "";
	private Locale locale = Locale.ENGLISH;
	private String realPath;
	private int remotePort;
	private String localName;
	private String localAddr;
	private int localPort;
	private RequestDispatcher requestDispatcher;
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
	private RestContext restContext;
	private String uri = "";
	private boolean debug = false;

	/**
	 * Creates a new servlet request.
	 *
	 * Initialized with the following:
	 * <ul>
	 * 	<li><c>"Accept: text/json+simple"</c>
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
	 * 	<li><c>"Accept: text/json+simple"</c>
	 * 	<li><c>"Content-Type: text/json"</c>
	 * </ul>
	 *
	 * @param method The HTTP method  name.
	 * @param path The request path.
	 * @param pathArgs Optional path arguments.
	 *
	 * @return A new request.
	 */
	public static MockServletRequest create(String method, String path, Object...pathArgs) {
		return create()
			.method(method)
			.uri(StringUtils.format(path, pathArgs));
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest json() {
		return header("Accept", "application/json").header("Content-Type", "application/json");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest xml() {
		return header("Accept", "text/xml").header("Content-Type", "text/xml");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest html() {
		return header("Accept", "text/html").header("Content-Type", "text/html");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest plainText() {
		return header("Accept", "text/plain").header("Content-Type", "text/plain");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest msgpack() {
		return header("Accept", "octal/msgpack").header("Content-Type", "octal/msgpack");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest uon() {
		return header("Accept", "text/uon").header("Content-Type", "text/uon");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest urlEnc() {
		return header("Accept", "application/x-www-form-urlencoded").header("Content-Type", "application/x-www-form-urlencoded");
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/yaml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest yaml() {
		return header("Accept", "text/yaml").header("Content-Type", "text/yaml");
	}

	/**
	 * Fluent setter.
	 *
	 * @param uri The URI of the request.
	 * @return This object (for method chaining).
	 */
	@Override /* MockHttpRequest */
	public MockServletRequest uri(String uri) {
		this.uri = emptyIfNull(uri);
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param restContext The rest context.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest restContext(RestContext restContext) {
		this.restContext = restContext;
		return this;
	}

	/**
	 * Executes this request and returns the response object.
	 *
	 * @return The response object.
	 * @throws Exception Error occurred.
	 */
	@Override /* MockHttpRequest */
	public MockServletResponse execute() throws Exception {
		MockServletResponse res = MockServletResponse.create();
		restContext.getCallHandler().execute(this, res);

		// If the status isn't set, something's broken.
		if (res.getStatus() == 0)
			throw new RuntimeException("Response status was 0.");

		if (debug)
			log(this, res);

		return res;
	}

	private void log(MockServletRequest req, MockServletResponse res) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n=== HTTP Call =================================================================");

		sb.append("\n=== REQUEST ===");
		sb.append("\nTODO");
		sb.append("\n=== RESPONSE ===");
		sb.append("\nStatus: ").append(res.getStatus());
		sb.append("\n---response headers---");
		for (Map.Entry<String,String[]> h : res.getHeaders().entrySet())
			for (String h2 : h.getValue())
				sb.append("\n").append(h.getKey()).append(": ").append(h2);
		sb.append("\n---response content---\n");
		sb.append(res.getBodyAsString());
		sb.append("\n=== END ========================================================================");

		System.err.println(sb);  // NOT DEBUG
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The method name for this request.
	 * @return This object (for method chaining).
	 */
	@Override /* MockHttpRequest */
	public MockServletRequest method(String value) {
		this.method = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The character encoding.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest characterEncoding(String value) {
		this.characterEncoding = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The protocol.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest protocol(String value) {
		this.protocol = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The scheme.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest scheme(String value) {
		this.scheme = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The server name.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest serverName(String value) {
		this.serverName = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The server port.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest serverPort(int value) {
		this.serverPort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote address.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest remoteAddr(String value) {
		this.remoteAddr = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote port.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest remoteHost(String value) {
		this.remoteHost = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The locale.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest locale(Locale value) {
		this.locale = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The real path.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest realPath(String value) {
		this.realPath = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote port.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest remotePort(int value) {
		this.remotePort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local name.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest localName(String value) {
		this.localName = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local address.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest localAddr(String value) {
		this.localAddr = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The local port.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest localPort(int value) {
		this.localPort = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The request dispatcher.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest requestDispatcher(RequestDispatcher value) {
		this.requestDispatcher = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The servlet context.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest servletContext(ServletContext value) {
		this.servletContext = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The dispatcher type.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest dispatcherType(DispatcherType value) {
		this.dispatcherType = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The auth type.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest authType(String value) {
		this.authType = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The cookies.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest cookies(Cookie[] value) {
		this.cookies = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The path info.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest pathInfo(String value) {
		this.pathInfo = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The path translated.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest pathTranslated(String value) {
		this.pathTranslated = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest contextPath(String value) {
		this.contextPath = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The query string.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest queryString(String value) {
		this.queryString = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The remote user.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest remoteUser(String value) {
		this.remoteUser = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The user principal.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest userPrincipal(Principal value) {
		this.userPrincipal = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The requested session ID.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest requestedSessionId(String value) {
		this.requestedSessionId = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The request URI.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest requestURI(String value) {
		this.requestURI = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The servlet path.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest servletPath(String value) {
		this.servletPath = value;
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param value The HTTP session.
	 * @return This object (for method chaining).
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
		return body == null ? 0 : body.length;
	}

	@Override /* HttpServletRequest */
	public long getContentLengthLong() {
		return body == null ? 0 : body.length;
	}

	@Override /* HttpServletRequest */
	public String getContentType() {
		return getHeader("Content-Type");
	}

	@Override /* HttpServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		if (formDataMap != null)
			body = UrlEncodingSerializer.DEFAULT.toString(formDataMap).getBytes();
		return new BoundedServletInputStream(new ByteArrayInputStream(body), Integer.MAX_VALUE);
	}

	@Override /* HttpServletRequest */
	public String getParameter(String name) {
		String[] s = getParameterMap().get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(new ArrayList<>(getParameterMap().keySet()));
	}

	@Override /* HttpServletRequest */
	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override /* HttpServletRequest */
	public Map<String,String[]> getParameterMap() {
		if (queryData == null) {
			try {
				if ("POST".equalsIgnoreCase(method)) {
					if (formDataMap != null)
						queryData = formDataMap;
					else
						queryData = RestUtils.parseQuery(IOUtils.read(body));
				} else {
					queryData = RestUtils.parseQuery(getQueryString());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return queryData;
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
		return Collections.enumeration(Arrays.asList(locale));
	}

	@Override /* HttpServletRequest */
	public boolean isSecure() {
		return false;
	}

	@Override /* HttpServletRequest */
	public RequestDispatcher getRequestDispatcher(String path) {
		return requestDispatcher;
	}

	@Override /* HttpServletRequest */
	public String getRealPath(String path) {
		return realPath;
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
		return s == null ? 0 : org.apache.juneau.http.Date.forString(s).asDate().getTime();
	}

	@Override /* HttpServletRequest */
	public String getHeader(String name) {
		String[] s = headerMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] s = headerMap.get(name);
		return Collections.enumeration(Arrays.asList(s == null ? new String[0] : s));
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
			queryString = "";
			if (uri.indexOf('?') != -1) {
				queryString = uri.substring(uri.indexOf('?') + 1);
			if (queryString.indexOf('#') != -1)
				queryString = queryString.substring(0, queryString.indexOf('#'));
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
		return false;
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
	 * @return This object (for method chaining).
	 */
	@Override /* MockHttpRequest */
	public MockServletRequest header(String name, Object value) {
		this.headerMap.put(name, new String[] {stringify(value)});
		return this;
	}

	/**
	 * Fluent setter.
	 *
	 * @param name Request attribute name.
	 * @param value Request attribute value.
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
	 */
	@Override /* MockHttpRequest */
	public MockServletRequest body(Object value) {
		try {
			if (value instanceof byte[])
				this.body = (byte[])value;
			if (value instanceof Reader)
				this.body = IOUtils.read((Reader)value).getBytes();
			if (value instanceof InputStream)
				this.body = IOUtils.readBytes((InputStream)value, 1024);
			if (value instanceof CharSequence)
				this.body = ((CharSequence)value).toString().getBytes();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * Adds a form data entry to this request.
	 *
	 * @param key The form data key.
	 * @param value The form data value.
	 * 	<br>The value is converted to a simple string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest formData(String key, Object value) {
		if (formDataMap == null)
			formDataMap = new LinkedHashMap<>();
		String s = stringify(value);
		String[] existing = formDataMap.get(key);
		if (existing == null)
			existing = new String[]{s};
		else
			existing = AList.of(existing).append(s).asArrayOf(String.class);
		formDataMap.put(key, existing);
		return this;
	}

	/**
	 * Adds a query data entry to this request.
	 *
	 * @param key The query key.
	 * @param value The query value.
	 * 	<br>The value is converted to a simple string using {@link Object#toString()}.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest query(String key, Object value) {
		if (queryData == null)
			queryData = new LinkedHashMap<>();
		String s = stringify(value);
		String[] existing = queryData.get(key);
		if (existing == null)
			existing = new String[]{s};
		else
			existing = AList.of(existing).append(s).asArrayOf(String.class);
		queryData.put(key, existing);
		return this;
	}

	//=================================================================================================================
	// Convenience methods - headers
	//=================================================================================================================

	/**
	 * Specifies the <c>Accept</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest accept(Object value) {
		return header("Accept", value);
	}

	/**
	 * Specifies the <c>Accept-Charset</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest acceptCharset(Object value) {
		return header("Accept-Charset", value);
	}

	/**
	 * Specifies the <c>Accept-Encoding</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest acceptEncoding(Object value) {
		return header("Accept-Encoding", value);
	}

	/**
	 * Specifies the <c>Accept-Language</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest acceptLanguage(Object value) {
		return header("Accept-Language", value);
	}

	/**
	 * Specifies the <c>Authorization</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest authorization(Object value) {
		return header("Authorization", value);
	}

	/**
	 * Specifies the <c>Cache-Control</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest cacheControl(Object value) {
		return header("Cache-Control", value);
	}

	/**
	 * Specifies the <c>X-Client-Version</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest clientVersion(Object value) {
		return header("X-Client-Version", value);
	}

	/**
	 * Specifies the <c>Connection</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest connection(Object value) {
		return header("Connection", value);
	}

	/**
	 * Specifies the <c>Content-Encoding</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest contentEncoding(Object value) {
		return header("Content-Encoding", value);
	}

	/**
	 * Specifies the <c>Content-Length</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest contentLength(Object value) {
		return header("Content-Length", value);
	}

	/**
	 * Specifies the <c>Content-Type</c> header value on the request.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest contentType(Object value) {
		return header("Content-Type", value);
	}

	/**
	 * Specifies the <c>Date</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest date(Object value) {
		return header("Date", value);
	}

	/**
	 * Specifies the <c>Expect</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest expect(Object value) {
		return header("Expect", value);
	}

	/**
	 * Specifies the <c>From</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest from(Object value) {
		return header("From", value);
	}

	/**
	 * Specifies the <c>Host</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest host(Object value) {
		return header("Host", value);
	}

	/**
	 * Specifies the <c>If-Match</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest ifMatch(Object value) {
		return header("If-Match", value);
	}

	/**
	 * Specifies the <c>If-Modified-Since</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest ifModifiedSince(Object value) {
		return header("If-Modified-Since", value);
	}

	/**
	 * Specifies the <c>If-None-Match</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest ifNoneMatch(Object value) {
		return header("If-None-Match", value);
	}

	/**
	 * Specifies the <c>If-Range</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest ifRange(Object value) {
		return header("If-Range", value);
	}

	/**
	 * Specifies the <c>If-Unmodified-Since</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest ifUnmodifiedSince(Object value) {
		return header("If-Unmodified-Since", value);
	}

	/**
	 * Specifies the <c>Max-Forwards</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest maxForwards(Object value) {
		return header("Max-Forwards", value);
	}

	/**
	 * Specifies the <c>Pragma</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest pragma(Object value) {
		return header("Pragma", value);
	}

	/**
	 * Specifies the <c>Proxy-Authorization</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest proxyAuthorization(Object value) {
		return header("Proxy-Authorization", value);
	}

	/**
	 * Specifies the <c>Range</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest range(Object value) {
		return header("Range", value);
	}

	/**
	 * Specifies the <c>Referer</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest referer(Object value) {
		return header("Referer", value);
	}

	/**
	 * Specifies the <c>TE</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest te(Object value) {
		return header("TE", value);
	}

	/**
	 * Specifies the <c>Upgrade</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest upgrade(Object value) {
		return header("Upgrade", value);
	}

	/**
	 * Specifies the <c>User-Agent</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest userAgent(Object value) {
		return header("User-Agent", value);
	}

	/**
	 * Specifies the <c>Warning</c> header value on the request.
	 *
	 * @param value The new value for the header.
	 * @return This object (for method chaining).
	 */
	public MockServletRequest warning(Object value) {
		return header("Warning", value);
	}

	/**
	 * Enabled debug mode on this request.
	 *
	 * <p>
	 * Causes information about the request execution to be sent to STDERR.
	 *
	 * @return This object (for method chaining).
	 */
	public MockServletRequest debug() {
		this.debug = true;
		return this;
	}
}
