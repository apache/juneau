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
package org.apache.juneau.rest.mock2;

import java.net.*;
import java.security.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client2.*;

/**
 * A subclass of {@link RestRequest} with additional features for mocked testing.
 *
 * <p>
 * Instances of this class are instantiated through methods on {@link MockRestClient} such as {@link MockRestClient#post(Object,Object)}
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRest}
 * </ul>
 */
public class MockRestRequest extends org.apache.juneau.rest.client2.RestRequest {

	//------------------------------------------------------------------------------------------------------------------
	// Servlet request override values.
	//------------------------------------------------------------------------------------------------------------------
	private Map<String,Object> attributeMap = new LinkedHashMap<>();
	private Map<String,RequestDispatcher> requestDispatcherMap = new LinkedHashMap<>();
	private String characterEncoding, protocol, scheme, serverName, remoteAddr, remoteHost, localName, localAddr,
		pathInfo, pathTranslated, contextPath, queryString, remoteUser, requestedSessionId, requestURI, servletPath, authType;
	private Integer serverPort, remotePort, localPort;
	private Locale locale;
	private ServletContext servletContext;
	private DispatcherType dispatcherType;
	private Cookie[] cookies;
	private Principal userPrincipal;
	private HttpSession httpSession;
	private String[] roles;

	/**
	 * Constructs a REST call with the specified method name.
	 *
	 * @param client The client that created this request.
	 * @param uri The target URI.
	 * @param method The HTTP method name (uppercase).
	 * @param hasBody Whether this method has a body.
	 * @throws RestCallException If an exception or non-200 response code occurred during the connection attempt.
	 */
	protected MockRestRequest(RestClient client, URI uri, String method, boolean hasBody) throws RestCallException {
		super(client, uri, method, hasBody);
	}

	@Override /* RestClient */
	protected MockRestResponse createResponse(RestClient client, HttpResponse httpResponse, Parser parser) throws RestCallException {
		return new MockRestResponse(client, this, httpResponse, parser);
	}

	//------------------------------------------------------------------------------------------------------------------
	// MockServletRequest passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Adds an attribute to the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param name The servlet request attribute name.
	 * @param value The servlet request attribute value.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest attribute(String name, Object value) {
		this.attributeMap.put(name, value);
		return this;
	}

	/**
	 * Replaces the attributes on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new servlet attribute values.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest attributes(Map<String,Object> value) {
		this.attributeMap.clear();
		this.attributeMap.putAll(value);
		return this;
	}

	/**
	 * Returns the attributes to add to the underlying {@link HttpServletRequest} object.
	 *
	 * @return The attributes to add to the underlying {@link HttpServletRequest} object.
	 */
	public Map<String,Object> getAttributeMap() {
		return attributeMap;
	}

	/**
	 * Specifies the user roles on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#isUserInRole(String)}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param roles The roles to add to this request (e.g. <js>"ROLE_ADMIN"</js>).
	 * @return This object (for method chaining).
	 */
	public MockRestRequest roles(String...roles) {
		this.roles = roles;
		return this;
	}

	/**
	 * Returns the user roles to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The user roles to set on the underlying {@link HttpServletRequest} object.
	 */
	public String[] getRoles() {
		return roles;
	}

	/**
	 * Specifies the value for the security roles on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#isUserInRole(String)}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param role The role to add to this request (e.g. <js>"ROLE_ADMIN"</js>).
	 * @return This object (for method chaining).
	 */
	public MockRestRequest role(String role) {
		this.roles = new String[]{role};
		return this;
	}

	/**
	 * Overrides the character encoding value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getCharacterEncoding()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest characterEncoding(String value) {
		this.characterEncoding = value;
		return this;
	}

	/**
	 * Returns the value to set for the return value on the underlying {@link HttpServletRequest#getCharacterEncoding()} method.
	 *
	 * @return The value to set for the return value on the underlying {@link HttpServletRequest#getCharacterEncoding()} method.
	 */
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	/**
	 * Overrides the HTTP protocol value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getProtocol()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest protocol(String value) {
		this.protocol = value;
		return this;
	}

	/**
	 * Returns the HTTP protocol value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The HTTP protocol value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Overrides the HTTP schema value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getScheme()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestRequest scheme(String value) {
		super.scheme(value);
		this.scheme = value;
		return this;
	}

	/**
	 * Returns the HTTP schema value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The HTTP schema value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getScheme() {
		return scheme;
	}

	/**
	 * Overrides the server name value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getServerName()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest serverName(String value) {
		this.serverName = value;
		return this;
	}

	/**
	 * Returns the server name value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The server name value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Overrides the server port value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getServerPort()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest serverPort(int value) {
		this.serverPort = value;
		return this;
	}

	/**
	 * Returns the server port value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The server port value to set on the underlying {@link HttpServletRequest} object.
	 */
	public Integer getServerPort() {
		return serverPort;
	}

	/**
	 * Overrides the remote address value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRemoteAddr()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest remoteAddr(String value) {
		this.remoteAddr = value;
		return this;
	}

	/**
	 * Returns the remote address value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The remote address value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getRemoteAddr() {
		return remoteAddr;
	}

	/**
	 * Overrides the remote host value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRemoteHost()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest remoteHost(String value) {
		this.remoteHost = value;
		return this;
	}

	/**
	 * Returns the remote host value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The remote host value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getRemoteHost() {
		return remoteHost;
	}

	/**
	 * Overrides the locale on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getLocale()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest locale(Locale value) {
		this.locale = value;
		return this;
	}

	/**
	 * Returns the locale to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The locale to set on the underlying {@link HttpServletRequest} object.
	 */
	@Override
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Overrides the remote port value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRemotePort()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest remotePort(int value) {
		this.remotePort = value;
		return this;
	}

	/**
	 * Returns the remote port value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The remote port value to set on the underlying {@link HttpServletRequest} object.
	 */
	public Integer getRemotePort() {
		return remotePort;
	}

	/**
	 * Overrides the local name value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getLocalName()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest localName(String value) {
		this.localName = value;
		return this;
	}

	/**
	 * Returns the local name value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The local name value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getLocalName() {
		return localName;
	}

	/**
	 * Overrides the local address value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getLocalAddr()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest localAddr(String value) {
		this.localAddr = value;
		return this;
	}

	/**
	 * Returns the local address value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The local address value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getLocalAddr() {
		return localAddr;
	}

	/**
	 * Overrides the local port value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getLocalPort()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest localPort(int value) {
		this.localPort = value;
		return this;
	}

	/**
	 * Returns the local port value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The local port value to set on the underlying {@link HttpServletRequest} object.
	 */
	public Integer getLocalPort() {
		return localPort;
	}

	/**
	 * Overrides the request dispatcher on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRequestDispatcher(String)}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param path The path to the resource being resolved.
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest requestDispatcher(String path, RequestDispatcher value) {
		this.requestDispatcherMap.put(path, value);
		return this;
	}

	/**
	 * Returns the request dispatcher to set on the underlying {@link HttpServletRequest} obhject.
	 *
	 * @return The value of the <property>requestDispatcherMap</property> property on this bean, or <jk>null</jk> if it is not set.
	 */
	public Map<String,RequestDispatcher> getRequestDispatcherMap() {
		return requestDispatcherMap;
	}

	/**
	 * Overrides the servlet context on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getServletContext()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest servletContext(ServletContext value) {
		this.servletContext = value;
		return this;
	}

	/**
	 * Returns the servlet context to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The servlet context to set on the underlying {@link HttpServletRequest} object.
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}

	/**
	 * Overrides the dispatcher type value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getDispatcherType()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest dispatcherType(DispatcherType value) {
		this.dispatcherType = value;
		return this;
	}

	/**
	 * Returns the dispatcher type value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The dispatcher type value to set on the underlying {@link HttpServletRequest} object.
	 */
	public DispatcherType getDispatcherType() {
		return dispatcherType;
	}

	/**
	 * Overrides the authorization type value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getAuthType()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest authType(String value) {
		this.authType = value;
		return this;
	}

	/**
	 * Returns the authorization type value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The authorization type value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getAuthType() {
		return authType;
	}

	/**
	 * Overrides the cookies on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getCookies()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest cookies(Cookie[] value) {
		this.cookies = value;
		return this;
	}

	/**
	 * Returns the cookies to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The cookies to set on the underlying {@link HttpServletRequest} object.
	 */
	public Cookie[] getCookies() {
		return cookies;
	}

	/**
	 * Overrides the path-info value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getPathInfo()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest pathInfo(String value) {
		this.pathInfo = value;
		return this;
	}

	/**
	 * Returns the path-info value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The path-info value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getPathInfo() {
		return pathInfo;
	}

	/**
	 * Overrides the path-translated value on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getPathTranslated()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest pathTranslated(String value) {
		this.pathTranslated = value;
		return this;
	}

	/**
	 * Returns the path-translated value to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The path-translated value to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getPathTranslated() {
		return pathTranslated;
	}

	/**
	 * Overrides the context path on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getContextPath()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest contextPath(String value) {
		this.contextPath = value;
		return this;
	}

	/**
	 * Returns the context path to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The context path to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getContextPath() {
		return contextPath;
	}

	/**
	 * Overrides the query string on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getQueryString()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest queryString(String value) {
		this.queryString = value;
		return this;
	}

	/**
	 * Returns the query string to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The query string to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Overrides the remote user on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRemoteUser()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest remoteUser(String value) {
		this.remoteUser = value;
		return this;
	}

	/**
	 * Returns the remote user to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The remote user to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getRemoteUser() {
		return remoteUser;
	}

	/**
	 * Overrides the user principal on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getUserPrincipal()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest userPrincipal(Principal value) {
		this.userPrincipal = value;
		return this;
	}

	/**
	 * Returns the user principal to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The user principal to set on the underlying {@link HttpServletRequest} object.
	 */
	public Principal getUserPrincipal() {
		return userPrincipal;
	}

	/**
	 * Overrides the requested session ID on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRequestedSessionId()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest requestedSessionId(String value) {
		this.requestedSessionId = value;
		return this;
	}

	/**
	 * Returns the requested session ID to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The requested session ID to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getRequestedSessionId() {
		return requestedSessionId;
	}

	/**
	 * Overrides the request URI on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getRequestURI()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest requestURI(String value) {
		this.requestURI = value;
		return this;
	}

	/**
	 * Returns the request URI to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The request URI to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getRequestURI() {
		return requestURI;
	}

	/**
	 * Overrides the servlet path on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getServletPath()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest servletPath(String value) {
		this.servletPath = value;
		return this;
	}

	/**
	 * Returns the servlet path to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The servlet path to set on the underlying {@link HttpServletRequest} object.
	 */
	public String getServletPath() {
		return servletPath;
	}

	/**
	 * Overrides the HTTP session on the underlying {@link HttpServletRequest} object.
	 *
	 * <p>
	 * Affects the results of calling {@link HttpServletRequest#getSession()}.
	 *
	 * <p>
	 * This value gets copied to the servlet request after the call to {@link HttpClientConnection#sendRequestHeader(HttpRequest)}
	 * and right before {@link HttpClientConnection#sendRequestEntity(HttpEntityEnclosingRequest)}.
	 *
	 * @param value The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public MockRestRequest httpSession(HttpSession value) {
		this.httpSession = value;
		return this;
	}

	/**
	 * Returns the HTTP session to set on the underlying {@link HttpServletRequest} object.
	 *
	 * @return The HTTP session to set on the underlying {@link HttpServletRequest} object.
	 */
	public HttpSession getHttpSession() {
		return httpSession;
	}
}
