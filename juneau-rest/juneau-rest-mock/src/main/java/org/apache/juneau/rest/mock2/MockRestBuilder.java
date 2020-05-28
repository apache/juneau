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

import static org.apache.juneau.rest.util.RestUtils.*;

import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Builder class for {@link MockRest} objects.
 */
public class MockRestBuilder {

	Object impl;
	boolean debug;
	Map<String,Object> headers = new LinkedHashMap<>();
	String contextPath = "", servletPath = "";

	MockRestBuilder(Object impl) {
		this.impl = impl;
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder debug() {
		this.debug = true;
		header("X-Debug", true);
		return this;
	}

	/**
	 * Enable no-trace mode.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder noTrace() {
		header("X-NoTrace", true);
		return this;
	}

	/**
	 * Adds a header to every request.
	 *
	 * @param name The header name.
	 * @param value
	 * 	The header value.
	 * 	<br>Can be <jk>null</jk> (will be skipped).
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder header(String name, Object value) {
		this.headers.put(name, value);
		return this;
	}

	/**
	 * Adds the specified headers to every request.
	 *
	 * @param value
	 * 	The header values.
	 * 	<br>Can be <jk>null</jk> (existing values will be cleared).
	 * 	<br><jk>null</jk> null map values will be ignored.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder headers(Map<String,Object> value) {
		if (value != null)
			this.headers.putAll(value);
		else
			this.headers.clear();
		return this;
	}

	/**
	 * Specifies the <c>Accept</c> header to every request.
	 *
	 * @param value The <c>Accept</c> header value.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder accept(String value) {
		return header("Accept", value);
	}

	/**
	 * Specifies the  <c>Content-Type</c> header to every request.
	 *
	 * @param value The <c>Content-Type</c> header value.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder contentType(String value) {
		return header("Content-Type", value);
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder json() {
		return accept("application/json").contentType("application/json");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json+simple"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder simpleJson() {
		return accept("application/json+simple").contentType("application/json+simple");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder xml() {
		return accept("text/xml").contentType("text/xml");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder html() {
		return accept("text/html").contentType("text/html");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder plainText() {
		return accept("text/plain").contentType("text/plain");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder msgpack() {
		return accept("octal/msgpack").contentType("octal/msgpack");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder uon() {
		return accept("text/uon").contentType("text/uon");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder urlEnc() {
		return accept("application/x-www-form-urlencoded").contentType("application/x-www-form-urlencoded");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/yaml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder yaml() {
		return accept("text/yaml").contentType("text/yaml");
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/openapi"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder openapi() {
		return accept("text/openapi").contentType("text/openapi");
	}

	/**
	 * Convenience method for setting the <c>Content-Type</c> header to the primary media type on the specified serializer.
	 *
	 * @param value
	 * 	The serializer to get the media type from.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder serializer(Serializer value) {
		return contentType(value == null ? null : value.getPrimaryMediaType().toString());
	}

	/**
	 * Convenience method for setting the <c>Accept</c> header to the primary media type on the specified parser.
	 *
	 * @param value
	 * 	The parser to get the media type from.
	 * 	<br>If <jk>null</jk>, header will be reset.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder parser(Parser value) {
		return accept(value == null ? null : value.getPrimaryMediaType().toString());
	}

	/**
	 * Convenience method for setting the <c>Accept</c> and <c>Content-Type</c> headers to the primary media types on the specified marshall.
	 *
	 * @param value
	 * 	The marshall to get the media types from.
	 * 	<br>If <jk>null</jk>, headers will be reset.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder marshall(Marshall value) {
		contentType(value == null ? null : value.getSerializer().getPrimaryMediaType().toString());
		accept(value == null ? null : value.getParser().getPrimaryMediaType().toString());
		return this;
	}

	/**
	 * Identifies the context path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder contextPath(String value) {
		this.contextPath = toValidContextPath(value);
		return this;
	}

	/**
	 * Identifies the servlet path for the REST resource.
	 *
	 * <p>
	 * 	This value is used to deconstruct the request URL and set the appropriate URL getters on the {@link HttpServletRequest}
	 * 	object correctly.
	 *
	 * <p>
	 * 	Should either be a value such as <js>"/foo"</js> or an empty string.
	 *
	 * <p>
	 * 	The following fixes are applied to non-conforming strings.
	 * <ul>
	 * 	<li><jk>nulls</jk> and <js>"/"</js> are converted to empty strings.
	 * 	<li>Trailing slashes are trimmed.
	 * 	<li>Leading slash is added if needed.
	 * </ul>
	 *
	 * @param value The context path.
	 * @return This object (for method chaining).
	 */
	public MockRestBuilder servletPath(String value) {
		this.servletPath = toValidContextPath(value);
		return this;
	}

	/**
	 * Create a new {@link MockRest} object based on the settings on this builder.
	 *
	 * @return A new {@link MockRest} object.
	 */
	public MockRest build() {
		return new MockRest(this);
	}
}