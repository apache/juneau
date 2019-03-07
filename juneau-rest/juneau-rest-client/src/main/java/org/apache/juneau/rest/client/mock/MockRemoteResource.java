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
package org.apache.juneau.rest.client.mock;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;

/**
 * Creates a mocked interface against a REST resource class to use for creating test remote resource interfaces.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.UnitTesting}
 * 	<li class='link'>{@doc juneau-rest-client.UnitTesting}
 * </ul>
 *
 * @param <T> The interface class.
 */
public class MockRemoteResource<T> {

	private MockRest.Builder mrb = MockRest.create();
	private final Class<T> intf;
	private Serializer s = JsonSerializer.DEFAULT;
	private Parser p = JsonParser.DEFAULT;
	private boolean debug;

	/**
	 * Constructor.
	 *
	 * @param intf The remote interface.
	 * @param impl The REST implementation class or bean.
	 */
	protected MockRemoteResource(Class<T> intf, Object impl) {
		this.intf = intf;
		mrb.impl(impl);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation class.
	 *
	 * @param intf The remote interface.
	 * @param impl The REST implementation class.
	 * @return A new builder.
	 */
	public static <T> MockRemoteResource<T> create(Class<T> intf, Class<?> impl) {
		return new MockRemoteResource<>(intf, impl);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation bean.
	 *
	 * @param intf The remote interface.
	 * @param impl The REST implementation bean.
	 * @return A new builder.
	 */
	public static <T> MockRemoteResource<T> create(Class<T> intf, Object impl) {
		return new MockRemoteResource<>(intf, impl);
	}

	/**
	 * Constructs a remote proxy interface based on the settings of this builder.
	 *
	 * @return A new remote proxy interface.
	 */
	public T build() {
		MockRest mr = mrb.build();
		return RestClient.create(s, p).debug(debug).mockHttpConnection(mr).headers(mr.getHeaders()).build().getRemoteResource(intf);
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).build();
	 * </p>
	 *
	 * <p>
	 * Uses JSON serialization and parsing.
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation class.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Class<?> impl) {
		return create(intf, impl).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).serializer(s).parser(p).build();
	 * </p>
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation class.
	 * @param s
	 * 	The serializer to use for serializing request bodies.
	 * 	<br>Can be <jk>null</jk> to force no serializer to be used and no <code>Content-Type</code> header.
	 * @param p
	 * 	The parser to use for parsing response bodies.
	 * 	<br>Can be <jk>null</jk> to force no parser to be used and no <code>Accept</code> header.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Class<?> impl, Serializer s, Parser p) {
		return create(intf, impl).serializer(s).parser(p).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).marshall(m).build();
	 * </p>
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation class.
	 * @param m
	 * 	The marshall to use for serializing request bodies and parsing response bodies.
	 * 	<br>Can be <jk>null</jk> to force no serializer or parser to be used and no <code>Accept</code> or <code>Content-Type</code> header.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Class<?> impl, Marshall m) {
		return create(intf, impl).marshall(m).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).build();
	 * </p>
	 *
	 * <p>
	 * Uses JSON serialization and parsing.
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation bean.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl) {
		return create(intf, impl).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).serializer(s).parser(p).build();
	 * </p>
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation object.
	 * @param s The serializer to use for serializing request bodies.
	 * @param p The parser to use for parsing response bodies.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl, Serializer s, Parser p) {
		return create(intf, impl).serializer(s).parser(p).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).marshall(m).build();
	 * </p>
	 *
	 * @param intf The remote proxy interface class.
	 * @param impl The REST implementation object.
	 * @param m The marshall to use for serializing request bodies and parsing response bodies.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl, Marshall m) {
		return create(intf, impl).marshall(m).build();
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> debug() {
		mrb.debug();
		this.debug = true;
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
	public MockRemoteResource<T> header(String name, Object value) {
		mrb.header(name, value);
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
	public MockRemoteResource<T> headers(Map<String,Object> value) {
		mrb.headers(value);
		return this;
	}

	/**
	 * Adds an <code>Accept</code> header to every request.
	 *
	 * @param value The <code>Accept/code> header value.
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> accept(String value) {
		mrb.accept(value);
		return this;
	}

	/**
	 * Adds a <code>Content-Type</code> header to every request.
	 *
	 * @param value The <code>Content-Type</code> header value.
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> contentType(String value) {
		mrb.contentType(value);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> json() {
		marshall(Json.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json+simple"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> simpleJson() {
		marshall(SimpleJson.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> xml() {
		marshall(Xml.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> html() {
		marshall(Html.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> plainText() {
		marshall(PlainText.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> msgpack() {
		marshall(MsgPack.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> uon() {
		marshall(Uon.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> urlEnc() {
		marshall(UrlEncoding.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/openapi"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> openapi() {
		marshall(OpenApi.DEFAULT);
		return this;
	}

	/**
	 * Associates the specified {@link Marshall} with this client.
	 *
	 * <p>
	 * This is shorthand for calling <code>serializer(x)</code> and <code>parser(x)</code> using the inner
	 * serializer and parser of the marshall object.
	 *
	 * @param value
	 * 	The marshall to use for serializing and parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remote the existing serializer/parser).
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> marshall(Marshall value) {
		if (value != null)
			serializer(value.getSerializer()).parser(value.getParser());
		else
			serializer(null).parser(null);
		return this;
	}

	/**
	 * Associates the specified {@link Serializer} with this client.
	 *
	 * @param value
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remote the existing serializer).
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> serializer(Serializer value) {
		this.s = value;
		contentType(value == null ? null : value.getPrimaryMediaType().toString());
		return this;
	}

	/**
	 * Associates the specified {@link Parser} with this client.
	 *
	 * @param value
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remote the existing parser).
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> parser(Parser value) {
		this.p = value;
		accept(value == null ? null : value.getPrimaryMediaType().toString());
		return this;
	}
}
