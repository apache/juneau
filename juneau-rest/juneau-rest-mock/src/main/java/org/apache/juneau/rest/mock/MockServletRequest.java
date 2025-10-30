/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.mock;

import static java.util.Collections.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.io.*;
import java.security.*;
import java.util.*;

import org.apache.http.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.urlencoding.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

/**
 * A mutable implementation of {@link HttpServletRequest} for mocking purposes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauRestMockBasics">juneau-rest-mock Basics</a>
 * </ul>
 */
public class MockServletRequest implements HttpServletRequest {

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
		return create().method(method).uri(StringUtils.format(uri, pathArgs));
	}

	private String method = "GET";
	private Map<String,String[]> queryDataMap = map();
	private Map<String,String[]> formDataMap;
	private Map<String,String[]> headerMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String,Object> attributeMap = map();
	private String characterEncoding = "UTF-8";
	private byte[] content = {};
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
	 * If the specified request is a {@link MockRestRequest}, applies any of the override values to this servlet request.
	 *
	 * @param req The request to copy overrides from.
	 * @return This object.
	 */
	public MockServletRequest applyOverrides(HttpRequest req) {

		if (req instanceof MockRestRequest mreq) {
			mreq.getAttributeMap().forEach(this::attribute);
			mreq.getRequestDispatcherMap().forEach(this::requestDispatcher);
			if (nn(mreq.getCharacterEncoding()))
				characterEncoding(mreq.getCharacterEncoding());
			if (nn(mreq.getProtocol()))
				protocol(mreq.getProtocol());
			if (nn(mreq.getScheme()))
				scheme(mreq.getScheme());
			if (nn(mreq.getServerName()))
				serverName(mreq.getServerName());
			if (nn(mreq.getRemoteAddr()))
				remoteAddr(mreq.getRemoteAddr());
			if (nn(mreq.getRemoteHost()))
				remoteHost(mreq.getRemoteHost());
			if (nn(mreq.getLocalName()))
				localName(mreq.getLocalName());
			if (nn(mreq.getLocalAddr()))
				localAddr(mreq.getLocalAddr());
			if (nn(mreq.getPathInfo()))
				pathInfo(mreq.getPathInfo());
			if (nn(mreq.getPathTranslated()))
				pathTranslated(mreq.getPathTranslated());
			if (nn(mreq.getContextPath()))
				contextPath(mreq.getContextPath());
			if (nn(mreq.getQueryString()))
				queryString(mreq.getQueryString());
			if (nn(mreq.getRemoteUser()))
				remoteUser(mreq.getRemoteUser());
			if (nn(mreq.getRequestedSessionId()))
				requestedSessionId(mreq.getRequestedSessionId());
			if (nn(mreq.getRequestURI()))
				requestURI(mreq.getRequestURI());
			if (nn(mreq.getServletPath()))
				servletPath(mreq.getServletPath());
			if (nn(mreq.getAuthType()))
				authType(mreq.getAuthType());
			if (nn(mreq.getServerPort()))
				serverPort(mreq.getServerPort());
			if (nn(mreq.getRemotePort()))
				remotePort(mreq.getRemotePort());
			if (nn(mreq.getLocalPort()))
				localPort(mreq.getLocalPort());
			if (nn(mreq.getLocale()))
				locale(mreq.getLocale());
			if (nn(mreq.getServletContext()))
				servletContext(mreq.getServletContext());
			if (nn(mreq.getDispatcherType()))
				dispatcherType(mreq.getDispatcherType());
			if (nn(mreq.getCookies()))
				cookies(mreq.getCookies());
			if (nn(mreq.getUserPrincipal()))
				userPrincipal(mreq.getUserPrincipal());
			if (nn(mreq.getHttpSession()))
				httpSession(mreq.getHttpSession());
			if (nn(mreq.getRoles()))
				roles(mreq.getRoles());
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

	@Override /* Overridden from HttpServletRequest */
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		return false;
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

	@Override /* Overridden from HttpServletRequest */
	public String changeSessionId() {
		return null;
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
			else if (nn(value))
				this.content = value.toString().getBytes();
		} catch (IOException e) {
			throw toRuntimeException(e);
		}
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
	 * @param value The dispatcher type.
	 * @return This object.
	 */
	public MockServletRequest dispatcherType(DispatcherType value) {
		this.dispatcherType = value;
		return this;
	}

	@Override /* Overridden from HttpServletRequest */
	public AsyncContext getAsyncContext() { return null; }

	@Override /* Overridden from HttpServletRequest */
	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	@Override /* Overridden from HttpServletRequest */
	public Enumeration<String> getAttributeNames() { return Collections.enumeration(attributeMap.keySet()); }

	@Override /* Overridden from HttpServletRequest */
	public String getAuthType() { return authType; }

	@Override /* Overridden from HttpServletRequest */
	public String getCharacterEncoding() { return characterEncoding; }

	@Override /* Overridden from HttpServletRequest */
	public int getContentLength() { return content == null ? 0 : content.length; }

	@Override /* Overridden from HttpServletRequest */
	public long getContentLengthLong() { return content == null ? 0 : content.length; }

	@Override /* Overridden from HttpServletRequest */
	public String getContentType() { return getHeader("Content-Type"); }

	@Override /* Overridden from HttpServletRequest */
	public String getContextPath() { return contextPath; }

	@Override /* Overridden from HttpServletRequest */
	public Cookie[] getCookies() { return cookies; }

	@Override /* Overridden from HttpServletRequest */
	public long getDateHeader(String name) {
		String s = getHeader(name);
		return s == null ? 0 : date(s).asZonedDateTime().get().toInstant().toEpochMilli();
	}

	@Override /* Overridden from HttpServletRequest */
	public DispatcherType getDispatcherType() { return dispatcherType; }

	@Override /* Overridden from HttpServletRequest */
	public String getHeader(String name) {
		String[] s = headerMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* Overridden from HttpServletRequest */
	public Enumeration<String> getHeaderNames() { return Collections.enumeration(headerMap.keySet()); }

	@Override /* Overridden from HttpServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] s = headerMap.get(name);
		return Collections.enumeration(l(s == null ? new String[0] : s));
	}

	@Override /* Overridden from HttpServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		if (nn(formDataMap))
			content = UrlEncodingSerializer.DEFAULT.toString(formDataMap).getBytes();
		return new BoundedServletInputStream(new ByteArrayInputStream(content), Integer.MAX_VALUE);
	}

	@Override /* Overridden from HttpServletRequest */
	public int getIntHeader(String name) {
		String s = getHeader(name);
		return s == null || s.isEmpty() ? 0 : Integer.parseInt(s);
	}

	@Override /* Overridden from HttpServletRequest */
	public String getLocalAddr() { return localAddr; }

	@Override /* Overridden from HttpServletRequest */
	public Locale getLocale() { return locale; }

	@Override /* Overridden from HttpServletRequest */
	public Enumeration<Locale> getLocales() { return Collections.enumeration(l(locale)); }

	@Override /* Overridden from HttpServletRequest */
	public String getLocalName() { return localName; }

	@Override /* Overridden from HttpServletRequest */
	public int getLocalPort() { return localPort; }

	@Override /* Overridden from HttpServletRequest */
	public String getMethod() { return method; }

	@Override /* Overridden from HttpServletRequest */
	public String getParameter(String name) {
		String[] s = getParameterMap().get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* Overridden from HttpServletRequest */
	public Map<String,String[]> getParameterMap() {
		if ("POST".equalsIgnoreCase(method)) {
			if (formDataMap == null)
				formDataMap = RestUtils.parseQuery(read(content));
			return formDataMap;
		}
		return queryDataMap;
	}

	@Override /* Overridden from HttpServletRequest */
	public Enumeration<String> getParameterNames() { return enumeration(toList(getParameterMap().keySet())); }

	@Override /* Overridden from HttpServletRequest */
	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override /* Overridden from HttpServletRequest */
	public Part getPart(String name) throws IOException, ServletException {
		return null;
	}

	@Override /* Overridden from HttpServletRequest */
	public Collection<Part> getParts() throws IOException, ServletException { return null; }

	@Override /* Overridden from HttpServletRequest */
	public String getPathInfo() {
		if (pathInfo == null) {
			pathInfo = getRequestURI();
			if (isNotEmpty(contextPath))
				pathInfo = pathInfo.substring(contextPath.length());
			if (isNotEmpty(servletPath))
				pathInfo = pathInfo.substring(servletPath.length());
		}
		return StringUtils.nullIfEmpty(StringUtils.urlDecode(pathInfo));
	}

	@Override /* Overridden from HttpServletRequest */
	public String getPathTranslated() {
		if (pathTranslated == null)
			pathTranslated = "/mock-path" + getPathInfo();
		return pathTranslated;
	}

	@Override /* Overridden from HttpServletRequest */
	public String getProtocol() { return protocol; }

	@Override
	public String getProtocolRequestId() { return null; }

	@Override /* Overridden from HttpServletRequest */
	public String getQueryString() {
		if (queryString == null) {
			if (queryDataMap.isEmpty())
				queryString = "";
			else {
				StringBuilder sb = new StringBuilder();
				queryDataMap.forEach((k, v) -> {
					if (v == null)
						sb.append(sb.length() == 0 ? "" : "&").append(StringUtils.urlEncode(k));
					else
						for (String v2 : v)
							sb.append(sb.length() == 0 ? "" : "&").append(StringUtils.urlEncode(k)).append('=').append(StringUtils.urlEncode(v2));
				});
				queryString = sb.toString();
			}
		}
		return isEmpty(queryString) ? null : queryString;
	}

	@Override /* Overridden from HttpServletRequest */
	public BufferedReader getReader() throws IOException { return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding)); }

	@Override /* Overridden from HttpServletRequest */
	public String getRemoteAddr() { return remoteAddr; }

	@Override /* Overridden from HttpServletRequest */
	public String getRemoteHost() { return remoteHost; }

	@Override /* Overridden from HttpServletRequest */
	public int getRemotePort() { return remotePort; }

	@Override /* Overridden from HttpServletRequest */
	public String getRemoteUser() { return remoteUser; }

	@Override /* Overridden from HttpServletRequest */
	public RequestDispatcher getRequestDispatcher(String path) {
		return requestDispatcher.get(path);
	}

	@Override /* Overridden from HttpServletRequest */
	public String getRequestedSessionId() { return requestedSessionId; }

	@Override
	public String getRequestId() { return null; }

	@Override /* Overridden from HttpServletRequest */
	public String getRequestURI() {
		if (requestURI == null) {
			requestURI = uri;
			requestURI = requestURI.replaceAll("^\\w+\\:\\/\\/[^\\/]+", "").replaceAll("\\?.*$", "");
		}
		return requestURI;
	}

	@Override /* Overridden from HttpServletRequest */
	public StringBuffer getRequestURL() { return new StringBuffer(uri.replaceAll("\\?.*$", "")); }

	@Override /* Overridden from HttpServletRequest */
	public String getScheme() { return scheme; }

	@Override /* Overridden from HttpServletRequest */
	public String getServerName() { return serverName; }

	@Override /* Overridden from HttpServletRequest */
	public int getServerPort() { return serverPort; }

	@Override
	public ServletConnection getServletConnection() { return null; }

	@Override /* Overridden from HttpServletRequest */
	public ServletContext getServletContext() { return servletContext; }

	@Override /* Overridden from HttpServletRequest */
	public String getServletPath() { return servletPath; }

	@Override /* Overridden from HttpServletRequest */
	public HttpSession getSession() { return httpSession; }

	@Override /* Overridden from HttpServletRequest */
	public HttpSession getSession(boolean create) {
		return httpSession;
	}

	@Override /* Overridden from HttpServletRequest */
	public Principal getUserPrincipal() { return userPrincipal; }

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
		if (nn(value)) {
			String[] v1 = (value instanceof String[]) ? (String[])value : a(value.toString());
			String[] v2 = headerMap.get(name);
			String[] v3 = combine(v2, v1);
			headerMap.put(name, v3);
		}
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

	@Override /* Overridden from HttpServletRequest */
	public boolean isAsyncStarted() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isAsyncSupported() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isRequestedSessionIdFromCookie() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isRequestedSessionIdFromURL() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isRequestedSessionIdValid() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isSecure() { return false; }

	@Override /* Overridden from HttpServletRequest */
	public boolean isUserInRole(String role) {
		return roles.contains(role);
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
	 * @param value The local port.
	 * @return This object.
	 */
	public MockServletRequest localPort(int value) {
		this.localPort = value;
		return this;
	}

	@Override /* Overridden from HttpServletRequest */
	public void login(String username, String password) throws ServletException {}

	@Override /* Overridden from HttpServletRequest */
	public void logout() throws ServletException {}

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
		if (nn(pathVars))
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
		return pathVars(mapb(String.class, String.class).addPairs((Object[])pairs).build());
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
	 * @param value The remote user.
	 * @return This object.
	 */
	public MockServletRequest remoteUser(String value) {
		this.remoteUser = value;
		return this;
	}

	@Override /* Overridden from HttpServletRequest */
	public void removeAttribute(String name) {
		this.attributeMap.remove(name);
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
	 * @param value The servlet path.
	 * @return This object.
	 */
	public MockServletRequest servletPath(String value) {
		this.servletPath = value;
		return this;
	}

	@Override /* Overridden from HttpServletRequest */
	public void setAttribute(String name, Object o) {
		this.attributeMap.put(name, o);
	}

	@Override /* Overridden from HttpServletRequest */
	public void setCharacterEncoding(String characterEncoding) throws UnsupportedEncodingException { this.characterEncoding = characterEncoding; }

	@Override /* Overridden from HttpServletRequest */
	public AsyncContext startAsync() throws IllegalStateException {
		return null;
	}

	@Override /* Overridden from HttpServletRequest */
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		return null;
	}

	@Override /* Overridden from HttpServletRequest */
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
		return null;
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
}