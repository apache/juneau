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

import static org.apache.juneau.internal.CollectionUtils.*;

import java.net.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.http.*;
import org.apache.http.client.config.*;
import org.apache.http.concurrent.*;
import org.apache.http.protocol.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * A subclass of {@link RestRequest} with additional features for mocked testing.
 *
 * <p>
 * Instances of this class are instantiated through methods on {@link MockRestClient} such as {@link MockRestClient#post(Object,Object)}
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
@FluentSetters(ignore="uriScheme")
public class MockRestRequest extends org.apache.juneau.rest.client.RestRequest {

	//------------------------------------------------------------------------------------------------------------------
	// Servlet request override values.
	//------------------------------------------------------------------------------------------------------------------
	private Map<String,Object> attributeMap = map();
	private Map<String,RequestDispatcher> requestDispatcherMap = map();
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
	 */
	@Override
	public MockRestRequest uriScheme(String value) {
		super.uriScheme(value);
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest accept(String value) throws RestCallException{
		super.accept(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest acceptCharset(String value) throws RestCallException{
		super.acceptCharset(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest cancellable(Cancellable cancellable) {
		super.cancellable(cancellable);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest config(RequestConfig value) {
		super.config(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest content(Object value) {
		super.content(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest content(Object input, HttpPartSchema schema) {
		super.content(input, schema);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest contentString(Object input) throws RestCallException{
		super.contentString(input);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest contentType(String value) throws RestCallException{
		super.contentType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest context(HttpContext context) {
		super.context(context);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest debug() throws RestCallException{
		super.debug();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest errorCodes(Predicate<Integer> value) {
		super.errorCodes(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest formData(NameValuePair...parts) {
		super.formData(parts);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest formData(String name, Object value) {
		super.formData(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest formDataBean(Object value) {
		super.formDataBean(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest formDataCustom(Object value) {
		super.formDataCustom(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest formDataPairs(String...pairs) throws RestCallException{
		super.formDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest header(Header part) {
		super.header(part);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest header(String name, Object value) {
		super.header(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest headerPairs(String...pairs) {
		super.headerPairs(pairs);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest headers(Header...parts) {
		super.headers(parts);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest headersBean(Object value) {
		super.headersBean(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest html() {
		super.html();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest htmlDoc() {
		super.htmlDoc();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest htmlStrippedDoc() {
		super.htmlStrippedDoc();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest ignoreErrors() {
		super.ignoreErrors();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest interceptors(RestCallInterceptor...interceptors) throws RestCallException{
		super.interceptors(interceptors);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest json() {
		super.json();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest mediaType(String value) throws RestCallException{
		super.mediaType(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest msgPack() {
		super.msgPack();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest noTrace() throws RestCallException{
		super.noTrace();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest openApi() {
		super.openApi();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest parser(Class<? extends org.apache.juneau.parser.Parser> parser) {
		super.parser(parser);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest parser(Parser parser) {
		super.parser(parser);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest pathData(NameValuePair...parts) {
		super.pathData(parts);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest pathData(String name, Object value) {
		super.pathData(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest pathDataBean(Object value) {
		super.pathDataBean(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest pathDataPairs(String...pairs) {
		super.pathDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest plainText() {
		super.plainText();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest protocolVersion(ProtocolVersion version) {
		super.protocolVersion(version);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest queryCustom(Object value) {
		super.queryCustom(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest queryData(NameValuePair...parts) {
		super.queryData(parts);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest queryData(String name, Object value) {
		super.queryData(name, value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest queryDataBean(Object value) {
		super.queryDataBean(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest queryDataPairs(String...pairs) throws RestCallException{
		super.queryDataPairs(pairs);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest rethrow(java.lang.Class<?>...values) {
		super.rethrow(values);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest serializer(Class<? extends org.apache.juneau.serializer.Serializer> serializer) {
		super.serializer(serializer);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest serializer(Serializer serializer) {
		super.serializer(serializer);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest json5() {
		super.json5();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest suppressLogging() {
		super.suppressLogging();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest target(HttpHost target) {
		super.target(target);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uon() {
		super.uon();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uri(Object uri) throws RestCallException{
		super.uri(uri);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uriFragment(String fragment) {
		super.uriFragment(fragment);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uriHost(String host) {
		super.uriHost(host);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uriPort(int port) {
		super.uriPort(port);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uriUserInfo(String userInfo) {
		super.uriUserInfo(userInfo);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest uriUserInfo(String username, String password) {
		super.uriUserInfo(username, password);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest urlEnc() {
		super.urlEnc();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.rest.client.RestRequest */
	public MockRestRequest xml() {
		super.xml();
		return this;
	}

	// </FluentSetters>
}
