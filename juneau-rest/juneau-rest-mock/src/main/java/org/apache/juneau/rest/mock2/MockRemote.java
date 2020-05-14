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
import org.apache.juneau.msgpack.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.html.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;

/**
 * Creates a mocked interface against a REST resource class to use for creating test remote resource interfaces.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-mock.MockRemote}
 * </ul>
 *
 * @param <T> The interface class.
 */
public class MockRemote<T> {

	private MockRest.Builder mrb;
	private RestClientBuilder rcb = RestClient.create();
	private final Class<T> intf;

	/**
	 * Constructor.
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link Remote @Remote}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 */
	protected MockRemote(Class<T> intf, Object impl) {
		this.intf = intf;
		mrb = MockRest.create(impl);
	}

	/**
	 * Create a new builder using the specified remote resource interface and REST implementation bean or bean class.
	 *
	 * <p>
	 * No <c>Accept</c> or <c>Content-Type</c> headers are set on the request.
	 *
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link Remote @Remote}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new builder.
	 */
	public static <T> MockRemote<T> create(Class<T> intf, Object impl) {
		return new MockRemote<>(intf, impl);
	}

	/**
	 * Constructs a remote proxy interface based on the settings of this builder.
	 *
	 * @return A new remote proxy interface.
	 */
	public T build() {
		MockRest mr = mrb.build();
		return rcb.connectionManager(new MockHttpClientConnectionManager(mr)).rootUrl("http://localhost").headers(mr.getHeaders()).build().getRemote(intf);
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * No <c>Accept</c> or <c>Content-Type</c> headers are set on the request.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemote.<jsm>create</jsm>(intf, impl).build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link Remote @Remote}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link Rest @Rest}.
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
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"application/json"</js> unless explicitly set.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemote.<jsm>create</jsm>(intf, impl).json().build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link Remote @Remote}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new proxy interface.
	 */
	public static <T> T buildJson(Class<T> intf, Object impl) {
		return create(intf, impl).json().build();
	}

	/**
	 * Convenience method for getting a remote resource interface.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"application/json+simple"</js> unless explicitly set.
	 *
	 * <p>
	 * Equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	MockRemote.<jsm>create</jsm>(intf, impl).simpleJson().build();
	 * </p>
	 *
	 * @param intf
	 * 	The remote interface annotated with {@link Remote @Remote}.
	 * @param impl
	 * 	The REST implementation bean or bean class annotated with {@link Rest @Rest}.
	 * 	<br>If a class, it must have a no-arg constructor.
	 * @return A new proxy interface.
	 */
	public static <T> T buildSimpleJson(Class<T> intf, Object impl) {
		return create(intf, impl).simpleJson().build();
	}

	/**
	 * Enable debug mode.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> debug() {
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
	public MockRemote<T> header(String name, Object value) {
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
	public MockRemote<T> headers(Map<String,Object> value) {
		mrb.headers(value);
		rcb.headers(value);
		return this;
	}

	/**
	 * Adds an <c>Accept</c> header to every request.
	 *
	 * @param value The <c>Accept</c> header value.
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> accept(String value) {
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
	public MockRemote<T> contentType(String value) {
		mrb.contentType(value);
		rcb.contentType(value);
		return this;
	}

	/**
	 * Adds JSON support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"application/json"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> json() {
		return jsonSerializer().jsonParser();
	}

	/**
	 * Adds JSON support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"application/json"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> jsonSerializer() {
		serializer(JsonSerializer.class);
		return this;
	}

	/**
	 * Adds JSON support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"application/json"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> jsonParser() {
		parser(JsonParser.class);
		return this;
	}

	/**
	 * Adds Simplified JSON support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"application/json+simple"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> simpleJson() {
		return simpleJsonSerializer().jsonParser();
	}

	/**
	 * Adds Simplified JSON support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"application/json+simple"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> simpleJsonSerializer() {
		serializer(SimpleJsonSerializer.class);
		return this;
	}

	/**
	 * Adds XML support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"application/json+simple"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> simpleJsonParser() {
		parser(SimpleJsonParser.class);
		return this;
	}

	/**
	 * Adds XML support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"text/xml"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> xml() {
		return xmlSerializer().xmlParser();
	}

	/**
	 * Adds XML support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"text/xml"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> xmlSerializer() {
		serializer(XmlSerializer.class);
		return this;
	}

	/**
	 * Adds XML support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"text/xml"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> xmlParser() {
		parser(XmlParser.class);
		return this;
	}

	/**
	 * Adds HTML support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"text/html"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> html() {
		return htmlSerializer().htmlParser();
	}

	/**
	 * Adds HTML support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"text/html"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> htmlSerializer() {
		serializer(HtmlSerializer.class);
		return this;
	}

	/**
	 * Adds HTML support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"text/html"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> htmlParser() {
		parser(HtmlParser.class);
		return this;
	}

	/**
	 * Adds Plain-Text support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"text/plain"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> plainText() {
		return plainTextSerializer().plainTextParser();
	}

	/**
	 * Adds Plain-Text support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"text/plain"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> plainTextSerializer() {
		serializer(PlainTextSerializer.class);
		return this;
	}

	/**
	 * Adds Plain-Text support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"text/plain"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> plainTextParser() {
		parser(PlainTextParser.class);
		return this;
	}

	/**
	 * Adds MessagePack support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"octal/msgpack"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> msgPack() {
		return msgPackSerializer().msgPackParser();
	}

	/**
	 * Adds MessagePack support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"octal/msgpack"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> msgPackSerializer() {
		serializer(MsgPackSerializer.class);
		return this;
	}

	/**
	 * Adds MessagePack support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"octal/msgpack"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> msgPackParser() {
		parser(MsgPackParser.class);
		return this;
	}

	/**
	 * Adds UON support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"text/uon"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> uon() {
		return uonSerializer().uonParser();
	}

	/**
	 * Adds UON support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"text/uon"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> uonSerializer() {
		serializer(UonSerializer.class);
		return this;
	}

	/**
	 * Adds UON support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"text/uon"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> uonParser() {
		parser(UonParser.class);
		return this;
	}

	/**
	 * Adds URL-Encoding support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"application/x-www-form-urlencoded"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> urlEnc() {
		return urlEncSerializer().urlEncParser();
	}

	/**
	 * Adds URL-Encoding support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"application/x-www-form-urlencoded"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> urlEncSerializer() {
		serializer(UrlEncodingSerializer.class);
		return this;
	}

	/**
	 * Adds URL-Encoding support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"application/x-www-form-urlencoded"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> urlEncParser() {
		parser(UrlEncodingParser.class);
		return this;
	}

	/**
	 * Adds OpenAPI support for the request and response bodies.
	 *
	 * <p>
	 * <c>Accept</c> and <c>Content-Type</c> headers are set to <js>"text/openapi"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> openApi() {
		return openApiSerializer().openApiParser();
	}

	/**
	 * Adds OpenAPI support for the request body only.
	 *
	 * <p>
	 * <c>Content-Type</c> header is set to <js>"text/openapi"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> openApiSerializer() {
		serializer(OpenApiSerializer.class);
		return this;
	}

	/**
	 * Adds OpenAPI support for the response body only.
	 *
	 * <p>
	 * <c>Accept</c> header is set to <js>"text/openapi"</js> unless explicitly set.
	 *
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> openApiParser() {
		parser(OpenApiParser.class);
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
	public MockRemote<T> marshall(Marshall value) {
		if (value != null)
			serializer(value.getSerializer()).parser(value.getParser());
		return this;
	}

	/**
	 * Associates the specified {@link Serializer} with the HTTP client.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not specified, it will be set to the media type of this serializer.
	 * <br>Note that the serializer is not actually used during serialization.
	 *
	 * @param value
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> serializer(Serializer value) {
		rcb.serializer(value);
		return this;
	}

	/**
	 * Associates the specified {@link Serializer} with the HTTP client.
	 *
	 * <p>
	 * If the <c>Content-Type</c> header is not specified, it will be set to the media type of this serializer.
	 * <br>Note that the serializer is not actually used during serialization.
	 *
	 * @param value
	 * 	The serializer to use for serializing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> serializer(Class<? extends Serializer> value) {
		rcb.serializer(value);
		return this;
	}

	/**
	 * Associates the specified {@link Parser} with the HTTP client.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not specified, it will be set to the media type of this parser.
	 * <br>Note that the parser is not actually used during parsing.
	 *
	 * @param value
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> parser(Parser value) {
		rcb.parser(value);
		return this;
	}

	/**
	 * Associates the specified {@link Parser} with the HTTP client.
	 *
	 * <p>
	 * If the <c>Accept</c> header is not specified, it will be set to the media type of this parser.
	 * <br>Note that the parser is not actually used during parsing.
	 *
	 * @param value
	 * 	The parser to use for parsing HTTP bodies.
	 * 	<br>Can be <jk>null</jk>.
	 * @return This object (for method chaining).
	 */
	public MockRemote<T> parser(Class<? extends Parser> value) {
		rcb.parser(value);
		return this;
	}
}
