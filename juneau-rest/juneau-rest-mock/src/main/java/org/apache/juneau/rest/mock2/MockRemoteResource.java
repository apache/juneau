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

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.serializer.*;

/**
 * Creates a mocked interface against a REST resource class to use for creating test remote resource interfaces.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRemoteResource}
 * </ul>
 *
 * @param <T> The interface class.
 */
public class MockRemoteResource<T> {

	private MockRest.Builder mrb;
	private RestClientBuilder rcb = RestClient.create().json();
	private final Class<T> intf;

	/**
	 * Constructor.
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 */
	protected MockRemoteResource(Class<T> intf, Object impl) {
		this.intf = intf;
		mrb = MockRest.create(impl);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation bean or bean class.
	 *
	 * <p>
	 * Uses {@link JsonSerializer#DEFAULT} and {@link JsonParser#DEFAULT} for serializing and parsing by default.
	 *
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static <T> MockRemoteResource<T> create(Class<T> intf, Object impl) {
		return new MockRemoteResource<>(intf, impl);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation bean or bean class.
	 *
	 * <p>
	 * Uses the serializer and parser defined on the specified marshall for serializing and parsing by default.
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for serializing and parsing the HTTP bodies.
	 * @return A new builder.
	 */
	public static <T> MockRemoteResource<T> create(Class<T> intf, Object impl, Marshall m) {
		return new MockRemoteResource<>(intf, impl).marshall(m);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation bean or bean class.
	 *
	 * <p>
	 * Uses the serializer and parser defined on the specified marshall for serializing and parsing by default.
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing request bodies.
	 * 	<br>Can be <jk>null</jk> to force no serializer to be used and no <c>Content-Type</c> header.
	 * @param p
	 * 	The parser to use for parsing response bodies.
	 * 	<br>Can be <jk>null</jk> to force no parser to be used and no <c>Accept</c> header.
	 * @return A new builder.
	 */
	public static <T> MockRemoteResource<T> create(Class<T> intf, Object impl, Serializer s, Parser p) {
		return new MockRemoteResource<>(intf, impl).serializer(s).parser(p);
	}

	/**
	 * Constructs a remote proxy interface based on the settings of this builder.
	 *
	 * @return A new remote proxy interface.
	 */
	public T build() {
		MockRest mr = mrb.build();
		return rcb.httpClientConnectionManager(new MockHttpClientConnectionManager(mr)).rootUrl("http://localhost").headers(mr.getHeaders()).build().getRemoteResource(intf);
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Uses {@link JsonSerializer#DEFAULT} and {@link JsonParser#DEFAULT} for serializing and parsing by default.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl) {
		return create(intf, impl).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Uses the serializer and parser defined on the specified marshall for serializing and parsing by default.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).marshall(m).build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for serializing request bodies and parsing response bodies.
	 * 	<br>Can be <jk>null</jk> to force no serializer or parser to be used and no <c>Accept</c> or <c>Content-Type</c> header.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl, Marshall m) {
		return create(intf, impl).marshall(m).build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * Uses the specified serializer and parser for serializing and parsing by default.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemoteResource.<jsf>create</jsf>(intf, impl).serializer(s).parser(p).build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link RemoteResource @RemoteResource}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing request bodies.
	 * 	<br>Can be <jk>null</jk> to force no serializer to be used and no <c>Content-Type</c> header.
	 * @param p
	 * 	The parser to use for parsing response bodies.
	 * 	<br>Can be <jk>null</jk> to force no parser to be used and no <c>Accept</c> header.
	 * @return A new proxy interface.
	 */
	public static <T> T build(Class<T> intf, Object impl, Serializer s, Parser p) {
		return create(intf, impl).serializer(s).parser(p).build();
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> debug() {
		mrb.debug();
		rcb.debug();
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
		rcb.header(name, value);
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
		rcb.headers(value);
		return this;
	}

	/**
	 * Adds an <c>Accept</c> header to every request.
	 *
	 * @param value The <code>Accept/code> header value.
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> accept(String value) {
		mrb.accept(value);
		rcb.accept(value);
		return this;
	}

	/**
	 * Adds a <c>Content-Type</c> header to every request.
	 *
	 * @param value The <c>Content-Type</c> header value.
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> contentType(String value) {
		mrb.contentType(value);
		rcb.contentType(value);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> json() {
		marshall(Json.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/json+simple"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> simpleJson() {
		marshall(SimpleJson.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> xml() {
		marshall(Xml.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> html() {
		marshall(Html.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> plainText() {
		marshall(PlainText.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> msgpack() {
		marshall(MsgPack.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> uon() {
		marshall(Uon.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemoteResource<T> urlEnc() {
		marshall(UrlEncoding.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <c>Accept</c> and <c>Content-Type</c> headers to <js>"text/openapi"</js>.
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
	 * This is shorthand for calling <c>serializer(x)</c> and <c>parser(x)</c> using the inner
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
		rcb.serializer(value);
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
		rcb.parser(value);
		accept(value == null ? null : value.getPrimaryMediaType().toString());
		return this;
	}
}
