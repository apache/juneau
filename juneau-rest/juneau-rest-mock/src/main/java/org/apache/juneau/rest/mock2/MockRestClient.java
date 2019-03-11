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

import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.serializer.*;

/**
 * Mocked {@link RestClient}.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-mock}
 * </ul>
 */
public class MockRestClient extends RestClientBuilder {

	private MockRest.Builder mrb;

	/**
	 * Constructor.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 */
	protected MockRestClient(Object impl) {
		super(null, null);
		mrb = MockRest.create(impl);
		rootUrl("http://localhost");
	}

	/**
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl) {
		return new MockRestClient(impl);
	}

	/**
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for serializing and parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer/parser).
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl, Marshall m) {
		return create(impl).marshall(m);
	}

	/**
	 * Creates a new RestClient builder configured with the specified REST implementation bean or bean class.
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer).
	 * @param p
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing parser).
	 * @return A new builder.
	 */
	public static MockRestClient create(Object impl, Serializer s, Parser p) {
		return create(impl).serializer(s).parser(p);
	}

	/**
	 * Convenience method for creating a Restclient over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRestClient.create(impl, m).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param m
	 * 	The marshall to use for specifying the <code>Accept</code> and <code>Content-Type</code> headers.
	 * 	<br>If <jk>null</jk>, headers will be reset.
	 * @return A new {@link MockRest} object.
	 */
	public static RestClient build(Object impl, Marshall m) {
		return create(impl, m).build();
	}

	/**
	 * Convenience method for creating a Restclient over the specified REST implementation bean or bean class.
	 *
	 * <p>
	 * Equivalent to calling:
	 * <p class='bpcode w800'>
	 * 	MockRestClient.create(impl, s, p).build();
	 * </p>
	 *
	 * @param impl
	 * 	The REST bean or bean class annotated with {@link RestResource @RestResource}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @param s
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing serializer).
	 * @param p
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk> (will remove the existing parser).
	 * @return A new {@link MockRest} object.
	 */
	public static RestClient build(Object impl, Serializer s, Parser p) {
		return create(impl, s, p).build();
	}

	@Override
	public RestClient build() {
		httpClientConnectionManager(new MockHttpClientConnectionManager(mrb.build()));
		return super.build();
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient debug() {
		mrb.debug();
		debug();
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient json() {
		marshall(Json.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/json+simple"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient simpleJson() {
		marshall(SimpleJson.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/xml"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient xml() {
		marshall(Xml.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/html"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient html() {
		marshall(Html.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/plain"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient plainText() {
		marshall(PlainText.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"octal/msgpack"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient msgpack() {
		marshall(MsgPack.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/uon"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient uon() {
		marshall(Uon.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"application/x-www-form-urlencoded"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient urlEnc() {
		marshall(UrlEncoding.DEFAULT);
		return this;
	}

	/**
	 * Convenience method for setting <code>Accept</code> and <code>Content-Type</code> headers to <js>"text/openapi"</js>.
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public MockRestClient openapi() {
		marshall(OpenApi.DEFAULT);
		return this;
	}

	@Override
	public MockRestClient marshall(Marshall value) {
		super.marshall(value);
		mrb.marshall(value);
		return this;
	}

	@Override
	public MockRestClient serializer(Serializer value) {
		super.serializer(value);
		mrb.serializer(value);
		return this;
	}

	@Override
	public MockRestClient parser(Parser value) {
		super.parser(value);
		mrb.parser(value);
		return this;
	}
}
