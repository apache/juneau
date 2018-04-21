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
package org.apache.juneau.rest.util;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;

/**
 * An implementation of {@link HttpServletRequest} for testing purposes.
 */
public class MockHttpServletRequest implements HttpServletRequest {
	
	private String method = "GET";
	private Map<String,String[]> parameterMap = new LinkedHashMap<>();
	private Map<String,String[]> headerMap = new LinkedHashMap<>();	
	private Map<String,Object> attributeMap = new LinkedHashMap<>();
	private String characterEncoding = "UTF-8";
	private byte[] body;
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
	private String contextPath;
	private String queryString;
	private String remoteUser;
	private Principal userPrincipal;
	private String requestedSessionId;
	private String requestURI;
	private String servletPath;
	private HttpSession httpSession;
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The method name for this request.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest method(String value) {
		this.method = value;
		return this;
	}

	/**
	 * Fluent setter.
	 * 
	 * @param name Query parameter name. 
	 * @param value Query parameter values.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest param(String name, String[] value) {
		this.parameterMap.put(name, value);
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param name Query parameter name. 
	 * @param value Query parameter value.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest param(String name, String value) {
		this.parameterMap.put(name, new String[] {value});
		return this;
	}

	/**
	 * Fluent setter.
	 * 
	 * @param name Header name. 
	 * @param value Header value.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest header(String name, String value) {
		this.headerMap.put(name, new String[] {value});
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param name Request attribute name. 
	 * @param value Request attribute value.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest attribute(String name, Object value) {
		this.attributeMap.put(name, value);
		return this;
	}

	/**
	 * Fluent setter.
	 * 
	 * @param value The body of the request.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest body(Object value) {
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
	 * Fluent setter.
	 * 
	 * @param value The character encoding.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest characterEncoding(String value) {
		this.characterEncoding = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The protocol.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest protocol(String value) {
		this.protocol = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The scheme.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest scheme(String value) {
		this.scheme = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The server name.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest serverName(String value) {
		this.serverName = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The server port.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest serverPort(int value) {
		this.serverPort = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The remote address.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest remoteAddr(String value) {
		this.remoteAddr = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The remote port.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest remoteHost(String value) {
		this.remoteHost = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The locale.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest locale(Locale value) {
		this.locale = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The real path.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest realPath(String value) {
		this.realPath = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The remote port.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest remotePort(int value) {
		this.remotePort = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The local name.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest localName(String value) {
		this.localName = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The local address.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest localAddr(String value) {
		this.localAddr = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The local port.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest localPort(int value) {
		this.localPort = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The request dispatcher.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest requestDispatcher(RequestDispatcher value) {
		this.requestDispatcher = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The servlet context.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest servletContext(ServletContext value) {
		this.servletContext = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The dispatcher type.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest dispatcherType(DispatcherType value) {
		this.dispatcherType = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The auth type.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest authType(String value) {
		this.authType = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The cookies.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest cookies(Cookie[] value) {
		this.cookies = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The path info.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest pathInfo(String value) {
		this.pathInfo = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The path translated.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest pathTranslated(String value) {
		this.pathTranslated = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest contextPath(String value) {
		this.contextPath = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The query string.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest queryString(String value) {
		this.queryString = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The remote user.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest remoteUser(String value) {
		this.remoteUser = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The user principal.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest userPrincipal(Principal value) {
		this.userPrincipal = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The requested session ID.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest requestedSessionId(String value) {
		this.requestedSessionId = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The request URI.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest requestURI(String value) {
		this.requestURI = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The servlet path.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest servletPath(String value) {
		this.servletPath = value;
		return this;
	}
	
	/**
	 * Fluent setter.
	 * 
	 * @param value The HTTP session.
	 * @return This object (for method chaining).
	 */
	public MockHttpServletRequest httpSession(HttpSession value) {
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
		return new BoundedServletInputStream(new ByteArrayInputStream(body), Integer.MAX_VALUE);
	}

	@Override /* HttpServletRequest */
	public String getParameter(String name) {
		String[] s = parameterMap.get(name);
		return s == null || s.length == 0 ? null : s[0];
	}

	@Override /* HttpServletRequest */
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(new ArrayList<>(parameterMap.keySet()));
	}

	@Override /* HttpServletRequest */
	public String[] getParameterValues(String name) {
		return parameterMap.get(name);
	}

	@Override /* HttpServletRequest */
	public Map<String,String[]> getParameterMap() {
		return parameterMap;
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
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(body), characterEncoding));
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
		return pathInfo;
	}

	@Override /* HttpServletRequest */
	public String getPathTranslated() {
		return pathTranslated;
	}

	@Override /* HttpServletRequest */
	public String getContextPath() {
		return contextPath;
	}

	@Override /* HttpServletRequest */
	public String getQueryString() {
		return queryString;
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
		return requestURI;
	}

	@Override /* HttpServletRequest */
	public StringBuffer getRequestURL() {
		return new StringBuffer(requestURI);
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
}
