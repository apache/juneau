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
package org.apache.juneau.rest.client2;

import static org.junit.Assert.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.auth.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

public class RestClientBuilderTest {

	public static class Bean {
		public int f;

		public static Bean create() {
			Bean b = new Bean();
			b.f = 1;
			return b;
		}

		public void check() {
			assertEquals(f, 1);
		}

		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	public static Bean bean = Bean.create();

	@Rest
	public static class A extends BasicRest {
		@RestMethod
		public Bean postBean(@Body Bean b) {
			return b;
		}
		@RestMethod
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Override client and builder.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_overrideHttpClient() {
		HttpClientBuilder cb = HttpClientBuilder.create();
		CloseableHttpClient hc = HttpClientBuilder.create().build();
		RestClient.create().httpClientBuilder(cb).build();
		RestClient.create().httpClient(hc).build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging
	//------------------------------------------------------------------------------------------------------------------
	@Test
	public void b01_logToConsole() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson().logToConsole().build();
		rc.post("/bean", bean).complete();
	}

	@Test
	public void b02_logTo() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient.create(A.class).simpleJson().logTo(Level.SEVERE, ml).build();
		rc.post("/bean", bean).complete();
		ml.assertLevel(Level.SEVERE);
		ml.assertMessageContains(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json+simple",
			"---request entity---",
			"Content-Type: application/json+simple",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json",
			"---response content---",
			"{f:1}",
			"=== END ======================================================================="
		);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_interceptors() throws RestCallException {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.addInterceptorFirst(
				new HttpRequestInterceptor() {
					@Override public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
						request.setHeader("A1", "1");
					}
				}
			)
			.addInterceptorLast(
				new HttpRequestInterceptor() {
					@Override public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
						request.setHeader("A2", "2");
					}
				}
			)
			.addInterceptorFirst(
				new HttpResponseInterceptor() {
					@Override public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
						response.setHeader("B1", "1");
					}
				}
			)
			.addInterceptorLast(
				new HttpResponseInterceptor() {
					@Override public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
						response.setHeader("B2", "2");
					}
				}
			)
			.build();

		rc.get("/echo")
			.run()
			.getBody().assertContains("A1: 1", "A2: 2")
			.getHeader("B1").assertValue("1")
			.getHeader("B2").assertValue("2")
		;
	}

	@Test
	public void c02_httpProcessor() throws RestCallException {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.httpProcessor(new HttpProcessor() {
				@Override
				public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
					request.setHeader("A1", "1");
				}

				@Override
				public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
					response.setHeader("B1", "1");
				}
			})
			.build();

		rc.get("/echo")
			.run()
			.getBody().assertContains("A1: 1")
			.getHeader("B1").assertValue("1")
		;
	}

	@Test
	public void c03_requestExecutor() throws RestCallException {
		AtomicBoolean b1 = new AtomicBoolean();
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.requestExecutor(new HttpRequestExecutor() {
				@Override
				public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws HttpException, IOException {
					b1.set(true);
					return super.execute(request, conn, context);
				}
			})
			.build();

		rc.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo")
		;
		assertTrue(b1.get());
	}

	@Test
	public void c04_defaultHeaders() throws RestCallException {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.defaultHeaders(AList.of(new org.apache.http.message.BasicHeader("Foo", "bar")))
			.build();

		rc.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo","Foo: bar")
		;
	}

	@Test
	public void c05_miscellaneous() {
		RestClient.create()
			.disableRedirectHandling()
			.redirectStrategy(DefaultRedirectStrategy.INSTANCE)
			.defaultCookieSpecRegistry(null)
			.sslHostnameVerifier(null)
			.publicSuffixMatcher(null)
			.sslContext(null)
			.sslSocketFactory(null)
			.maxConnTotal(10)
			.maxConnPerRoute(10)
			.defaultSocketConfig(null)
			.defaultConnectionConfig(null)
			.connectionTimeToLive(100, TimeUnit.DAYS)
			.connectionManager(null)
			.connectionManagerShared(true)
			.connectionReuseStrategy(null)
			.keepAliveStrategy(null)
			.targetAuthenticationStrategy(null)
			.proxyAuthenticationStrategy(null)
			.userTokenHandler(null)
			.disableConnectionState()
			.schemePortResolver(null)
			.userAgent("foo")
			.disableCookieManagement()
			.disableContentCompression()
			.disableAuthCaching()
			.retryHandler(null)
			.disableAutomaticRetries()
			.proxy(null)
			.routePlanner(null)
			.connectionBackoffStrategy(null)
			.backoffManager(null)
			.serviceUnavailableRetryStrategy(null)
			.defaultCookieStore(null)
			.defaultCredentialsProvider(null)
			.defaultAuthSchemeRegistry(null)
			.contentDecoderRegistry(null)
			.defaultRequestConfig(null)
			.useSystemProperties()
			.evictExpiredConnections()
			.evictIdleConnections(1, TimeUnit.DAYS);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pooled connections
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_pooled() throws RestCallException {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.pooled()
			.build();

		rc.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo")
		;

	}

	//------------------------------------------------------------------------------------------------------------------
	// Authentication
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B extends BasicRest {
		@RestMethod
		public String getEcho(@org.apache.juneau.http.annotation.Header("Authorization") String auth, org.apache.juneau.rest.RestResponse res) throws IOException {
			if (auth == null) {
				throw new Unauthorized().header("WWW-Authenticate", "BASIC realm=\"foo\"");
			} else {
				if (! auth.equals("Basic dXNlcjpwdw==")) {
					throw new BadRequest("Wrong auth header: " + auth);
				}
				return "OK";
			}
		}
	}

	@Test
	public void e01_basicAuth() throws RestCallException {
		RestClient rc = MockRestClient.create(B.class).simpleJson()
			.basicAuth(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "user", "pw")
			.build();
		rc.get("/echo")
			.run()
			.getBody().assertContains("OK")
		;
	}

//	/**
//	 * Sets a header on all requests.
//	 *
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>, myPartSerializer, headerSchema);
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param name The header name.
//	 * @param value The header value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @param serializer The serializer to use for serializing the value to a string.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
//	 * 	</ul>
//	 * @param schema The schema object that defines the format of the output.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
//	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder header(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//		return addTo(RESTCLIENT_headers, name, SerializedNameValuePair.create().name(name).value(value).type(HEADER).serializer(serializer).schema(schema));
//	}
//
//	/**
//	 * Sets a header on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.header(<js>"Foo"</js>, <js>"bar"</js>);
//	 * 		.build();
//	 * </p>
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_headers}
//	 * </ul>
//	 *
//	 * @param name The header name.
//	 * @param value The header value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder header(String name, Object value) {
//		return header(name, value, null, null);
//	}
//
//	/**
//	 * Sets a header on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.header(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param header The header to set.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder header(Header header) {
//		return addTo(RESTCLIENT_headers, header.getName(), header);
//	}
//
//	/**
//	 * Sets a header on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.header(<jk>new</jk> NameValuePair(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param header The header to set.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder header(NameValuePair header) {
//		return addTo(RESTCLIENT_headers, header.getName(), header);
//	}
//
//	/**
//	 * Sets a header on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.header(<jk>new</jk> Accept(<js>"Content-Type"</js>, <js>"application/json"</js>)
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param header The header to set.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder header(HttpHeader header) {
//		return addTo(RESTCLIENT_headers, header.getName(), header);
//	}
//
//	/**
//	 * Sets multiple headers on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(<jk>new</jk> BasicHeader(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers The header to set.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(Header...headers) {
//		for (Header h : headers)
//			header(h);
//		return this;
//	}
//
//	/**
//	 * Sets multiple headers on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(<jk>new</jk> ObjectMap(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers The header pairs.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(ObjectMap headers) {
//		return headers((Map<String,Object>)headers);
//	}
//
//	/**
//	 * Sets multiple headers on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(AMap.<jsm>create</jsm>().append(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers The header pairs.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(Map<String,Object> headers) {
//		for (Map.Entry<String,Object> e : headers.entrySet())
//			header(e.getKey(), e.getValue(), null, null);
//		return this;
//	}
//
//	/**
//	 * Sets multiple headers on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(<jk>new</jk> NameValuePairs(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers The header pairs.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(NameValuePairs headers) {
//		for (NameValuePair p : headers)
//			header(p);
//		return this;
//	}
//
//	/**
//	 * Sets multiple headers on all requests.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(<jk>new</jk> NameValuePair(<js>"Foo"</js>, <js>"bar"</js>))
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers The header pairs.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(NameValuePair...headers) {
//		for (NameValuePair p : headers)
//			header(p);
//		return this;
//	}
//
//	/**
//	 * Sets multiple headers on all requests using freeform key/value pairs.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(<js>"Header1"</js>,<js>"val1"</js>,<js>"Header2"</js>,<js>"val2"</js>)
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param pairs The header key/value pairs.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(Object...pairs) {
//		if (pairs.length % 2 != 0)
//			throw new RuntimeException("Odd number of parameters passed into headers(Object...)");
//		for (int i = 0; i < pairs.length; i+=2)
//			header(stringify(pairs[i]), pairs[i+1]);
//		return this;
//	}
//
//	/**
//	 * Sets multiple headers on all requests using header beans.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	RestClient c = RestClient
//	 * 		.<jsm>create</jsm>()
//	 * 		.headers(
//	 * 			<jk>new</jk> AcceptEncoding(<js>"gzip"</js>),
//	 * 			<jk>new</jk> AcceptLanguage(<js>"da, en-gb;q=0.8, en;q=0.7"</js>)
//	 * 		)
//	 * 		.build();
//	 * </p>
//	 *
//	 * @param headers
//	 * 	The headers.
//	 * 	The header values are converted to strings using the configured {@link HttpPartSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder headers(HttpHeader...headers) {
//		for (HttpHeader h : headers)
//			header(h.getName(), h.getValue());
//		return this;
//	}
//
//	/**
//	 * Sets the value for the <c>Accept</c> request header.
//	 *
//	 * <p>
//	 * This overrides the media type specified on the parser, but is overridden by calling
//	 * <code>header(<js>"Accept"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder accept(Object value) {
//		return header("Accept", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Accept-Charset</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Accept-Charset"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder acceptCharset(Object value) {
//		return header("Accept-Charset", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Accept-Encoding</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Accept-Encoding"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder acceptEncoding(Object value) {
//		return header("Accept-Encoding", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Accept-Language</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Accept-Language"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder acceptLanguage(Object value) {
//		return header("Accept-Language", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Authorization</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Authorization"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder authorization(Object value) {
//		return header("Authorization", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Cache-Control</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Cache-Control"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder cacheControl(Object value) {
//		return header("Cache-Control", value);
//	}
//
//	/**
//	 * Sets the client version by setting the value for the <js>"X-Client-Version"</js> header.
//	 *
//	 * @param value The version string (e.g. <js>"1.2.3"</js>)
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder clientVersion(Object value) {
//		return header("X-Client-Version", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Connection</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Connection"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder connection(Object value) {
//		return header("Connection", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Content-Length</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Content-Length"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder contentLength(Object value) {
//		return header("Content-Length", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Content-Type</c> request header.
//	 *
//	 * <p>
//	 * This overrides the media type specified on the serializer, but is overridden by calling
//	 * <code>header(<js>"Content-Type"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder contentType(Object value) {
//		return header("Content-Type", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Date</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Date"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder date(Object value) {
//		return header("Date", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Expect</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Expect"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder expect(Object value) {
//		return header("Expect", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Forwarded</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Forwarded"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder forwarded(Object value) {
//		return header("Forwarded", value);
//	}
//
//	/**
//	 * Sets the value for the <c>From</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"From"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder from(Object value) {
//		return header("From", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Host</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Host"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder host(Object value) {
//		return header("Host", value);
//	}
//
//	/**
//	 * Sets the value for the <c>If-Match</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"If-Match"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ifMatch(Object value) {
//		return header("If-Match", value);
//	}
//
//	/**
//	 * Sets the value for the <c>If-Modified-Since</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"If-Modified-Since"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ifModifiedSince(Object value) {
//		return header("If-Modified-Since", value);
//	}
//
//	/**
//	 * Sets the value for the <c>If-None-Match</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"If-None-Match"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ifNoneMatch(Object value) {
//		return header("If-None-Match", value);
//	}
//
//	/**
//	 * Sets the value for the <c>If-Range</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"If-Range"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ifRange(Object value) {
//		return header("If-Range", value);
//	}
//
//	/**
//	 * Sets the value for the <c>If-Unmodified-Since</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"If-Unmodified-Since"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ifUnmodifiedSince(Object value) {
//		return header("If-Unmodified-Since", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Max-Forwards</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Max-Forwards"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder maxForwards(Object value) {
//		return header("If-Unmodified-Since", value);
//	}
//
//	/**
//	 * When called, <c>No-Trace: true</c> is added to requests.
//	 *
//	 * <p>
//	 * This gives the opportunity for the servlet to not log errors on invalid requests.
//	 * This is useful for testing purposes when you don't want your log file to show lots of errors that are simply the
//	 * results of testing.
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder noTrace() {
//		return header("No-Trace", true);
//	}
//
//	/**
//	 * Sets the value for the <c>Origin</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Origin"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder origin(Object value) {
//		return header("If-Unmodified-Since", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Pragma</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Pragma"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder pragma(Object value) {
//		return header("Pragma", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Proxy-Authorization</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Proxy-Authorization"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder proxyAuthorization(Object value) {
//		return header("Proxy-Authorization", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Range</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Range"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder range(Object value) {
//		return header("Range", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Referer</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Referer"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder referer(Object value) {
//		return header("Referer", value);
//	}
//
//	/**
//	 * Sets the value for the <c>TE</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"TE"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder te(Object value) {
//		return header("TE", value);
//	}
//
//	/**
//	 * Sets the value for the <c>User-Agent</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"User-Agent"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder userAgent(Object value) {
//		return header("User-Agent", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Upgrade</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Upgrade"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder upgrade(Object value) {
//		return header("Upgrade", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Via</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Via"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder via(Object value) {
//		return header("Via", value);
//	}
//
//	/**
//	 * Sets the value for the <c>Warning</c> request header.
//	 *
//	 * <p>
//	 * This is a shortcut for calling <code>header(<js>"Warning"</js>, value);</code>
//	 *
//	 * @param value The new header value.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder warning(Object value) {
//		return header("Warning", value);
//	}
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Query
//	//-----------------------------------------------------------------------------------------------------------------
//
//	/**
//	 * Adds a query parameter to the URI.
//	 *
//	 * @param name The parameter name.
//	 * @param value The parameter value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @param serializer The serializer to use for serializing the value to a string.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
//	 * 	</ul>
//	 * @param schema The schema object that defines the format of the output.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
//	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//		return addTo(RESTCLIENT_query, name, SerializedNameValuePair.create().name(name).value(value).type(QUERY).serializer(serializer).schema(schema));
//	}
//
//	/**
//	 * Adds a query parameter to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<js>"foo"</js>, <js>"bar"</js>)
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param name The parameter name.
//	 * @param value The parameter value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(String name, Object value) {
//		return query(name, value, null, null);
//	}
//
//	/**
//	 * Adds a query parameter to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param param The query parameter.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(NameValuePair param) {
//		return addTo(RESTCLIENT_query, param.getName(), param);
//	}
//
//	/**
//	 * Adds query parameters to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<jk>new</jk> ObjectMap(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The query parameters.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(ObjectMap params) {
//		return query((Map<String,Object>)params);
//	}
//
//	/**
//	 * Adds query parameters to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(AMap.<jsm>create</jsm>().append(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The query parameters.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(Map<String,Object> params) {
//		for (Map.Entry<String,Object> e : params.entrySet())
//			query(e.getKey(), e.getValue());
//		return this;
//	}
//
//	/**
//	 * Adds query parameters to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<jk>new</jk> NameValuePairs(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The query parameters.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(NameValuePairs params) {
//		for (NameValuePair p : params)
//			query(p);
//		return this;
//	}
//
//	/**
//	 * Adds query parameters to the URI.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The query parameters.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(NameValuePair...params) {
//		for (NameValuePair p : params)
//			query(p);
//		return this;
//	}
//
//	/**
//	 * Adds query parameters to the URI query using free-form key/value pairs.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.get(<jsf>URL</jsf>)
//	 * 		.query(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param pairs The query key/value pairs.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder query(Object...pairs) {
//		if (pairs.length % 2 != 0)
//			throw new RuntimeException("Odd number of parameters passed into query(Object...)");
//		for (int i = 0; i < pairs.length; i+=2)
//			query(stringify(pairs[i]), pairs[i+1]);
//		return this;
//	}
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Form data
//	//-----------------------------------------------------------------------------------------------------------------
//
//	/**
//	 * Adds a form-data parameter to all request bodies.
//	 *
//	 * @param name The parameter name.
//	 * @param value The parameter value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @param serializer The serializer to use for serializing the value to a string.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, then the {@link HttpPartSerializer} defined on the client is used ({@link OpenApiSerializer} by default).
//	 * 	</ul>
//	 * @param schema The schema object that defines the format of the output.
//	 * 	<ul>
//	 * 		<li>If <jk>null</jk>, defaults to {@link HttpPartSchema#DEFAULT}.
//	 * 		<li>Only used if serializer is schema-aware (e.g. {@link OpenApiSerializer}).
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//		return addTo(RESTCLIENT_formData, name, SerializedNameValuePair.create().name(name).value(value).type(FORMDATA).serializer(serializer).schema(schema));
//	}
//
//	/**
//	 * Adds a form-data parameter to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<js>"foo"</js>, <js>"bar"</js>)
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param name The parameter name.
//	 * @param value The parameter value.
//	 * 	<ul>
//	 * 		<li>Can be any POJO.
//	 * 		<li>Converted to a string using the specified part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(String name, Object value) {
//		return formData(name, value, null, null);
//	}
//
//	/**
//	 * Adds a form-data parameter to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param param The form-data parameter.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(NameValuePair param) {
//		return addTo(RESTCLIENT_formData, param.getName(), param);
//	}
//
//	/**
//	 * Adds form-data parameters to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<jk>new</jk> ObjectMap(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The form-data parameters.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(ObjectMap params) {
//		return formData((Map<String,Object>)params);
//	}
//
//	/**
//	 * Adds form-data parameters to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(AMap.<jsm>create</jsm>().append(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The form-data parameters.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(Map<String,Object> params) {
//		for (Map.Entry<String,Object> e : params.entrySet())
//			formData(e.getKey(), e.getValue());
//		return this;
//	}
//
//	/**
//	 * Adds form-data parameters to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<jk>new</jk> NameValuePairs(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The form-data parameters.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(NameValuePairs params) {
//		for (NameValuePair p : params)
//			formData(p);
//		return this;
//	}
//
//	/**
//	 * Adds form-data parameters to all request bodies.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<jk>new</jk> NameValuePair(<js>"foo"</js>, <js>"bar"</js>))
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param params The form-data parameters.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(NameValuePair...params) {
//		for (NameValuePair p : params)
//			formData(p);
//		return this;
//	}
//
//	/**
//	 * Adds form-data parameters to all request bodies using free-form key/value pairs.
//	 *
//	 * <h5 class='section'>Example:</h5>
//	 * <p class='bcode w800'>
//	 * 	client
//	 * 		.formPost(<jsf>URL</jsf>)
//	 * 		.formData(<js>"key1"</js>,<js>"val1"</js>,<js>"key2"</js>,<js>"val2"</js>)
//	 * 		.run();
//	 * </p>
//	 *
//	 * @param pairs The form-data key/value pairs.
//	 * 	<ul>
//	 * 		<li>Values can be any POJO.
//	 * 		<li>Values converted to a string using the configured part serializer.
//	 * 		<li>Values are converted to strings at runtime to allow them to be modified externally.
//	 * 	</ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder formData(Object...pairs) {
//		if (pairs.length % 2 != 0)
//			throw new RuntimeException("Odd number of parameters passed into formData(Object...)");
//		for (int i = 0; i < pairs.length; i+=2)
//			formData(stringify(pairs[i]), pairs[i+1]);
//		return this;
//	}
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	/**
//	 * Configuration property:  REST call handler.
//	 *
//	 * <p>
//	 * Allows you to provide a custom handler for making HTTP calls.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jic'>{@link RestCallHandler}
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is <jk>null</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder callHandler(Class<? extends RestCallHandler> value) {
//		return set(RESTCLIENT_callHandler, value);
//	}
//
//	/**
//	 * Configuration property:  REST call handler.
//	 *
//	 * <p>
//	 * Allows you to provide a custom handler for making HTTP calls.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jic'>{@link RestCallHandler}
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_callHandler}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is <jk>null</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder callHandler(RestCallHandler value) {
//		return set(RESTCLIENT_callHandler, value);
//	}
//
//	/**
//	 * Configuration property:  Errors codes predicate.
//	 *
//	 * <p>
//	 * Defines a predicate to test for error codes.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_errorCodes}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is <code>x -&gt; x &gt;= 400</code>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder errorCodes(Predicate<Integer> value) {
//		return set(RESTCLIENT_errorCodes, value);
//	}
//
//	/**
//	 * Configuration property:  Executor service.
//	 *
//	 * <p>
//	 * Defines the executor service to use when calling future methods on the {@link RestRequest} class.
//	 *
//	 * <p>
//	 * This executor service is used to create {@link Future} objects on the following methods:
//	 * <ul>
//	 * 	<li class='jm'>{@link RestRequest#runFuture()}
//	 * </ul>
//	 *
//	 * <p>
//	 * The default executor service is a single-threaded {@link ThreadPoolExecutor} with a 30 second timeout
//	 * and a queue size of 10.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorService}
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_executorServiceShutdownOnClose}
//	 * </ul>
//	 *
//	 * @param executorService The executor service.
//	 * @param shutdownOnClose Call {@link ExecutorService#shutdown()} when {@link RestClient#close()} is called.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
//		set(RESTCLIENT_executorService, executorService);
//		set(RESTCLIENT_executorServiceShutdownOnClose, shutdownOnClose);
//		return this;
//	}
//
//	/**
//	 * Configuration property:  Keep HttpClient open.
//	 *
//	 * <p>
//	 * Don't close this client when the {@link RestClient#close()} method is called.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder keepHttpClientOpen(boolean value) {
//		return set(RESTCLIENT_keepHttpClientOpen, value);
//	}
//
//	/**
//	 * Configuration property:  Keep HttpClient open.
//	 *
//	 * <p>
//	 * Don't close this client when the {@link RestClient#close()} method is called.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_keepHttpClientOpen}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder keepHttpClientOpen() {
//		return keepHttpClientOpen(true);
//	}
//
//	/**
//	 * Configuration property:  Call interceptors.
//	 *
//	 * <p>
//	 * Adds an interceptor that gets called immediately after a connection is made.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
//	 * </ul>
//	 *
//	 * @param values The values to add to this setting.
//	 * @return This object (for method chaining).
//	 */
//	@SuppressWarnings("unchecked")
//	@ConfigurationProperty
//	public RestClientBuilder interceptors(Class<? extends RestCallInterceptor>...values) {
//		return addTo(RESTCLIENT_interceptors, values);
//	}
//
//	/**
//	 * Configuration property:  Call interceptors.
//	 *
//	 * <p>
//	 * Adds an interceptor that gets called immediately after a connection is made.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_interceptors}
//	 * </ul>
//	 *
//	 * @param value The values to add to this setting.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder interceptors(RestCallInterceptor...value) {
//		return addTo(RESTCLIENT_interceptors, value);
//	}
//
//	/**
//	 * Configuration property:  Enable leak detection.
//	 *
//	 * <p>
//	 * Enable client and request/response leak detection.
//	 *
//	 * <p>
//	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
//	 * when the <c>finalize</c> methods are invoked.
//	 *
//	 * <p>
//	 * Automatically enabled with {@link RestClient#RESTCLIENT_debug}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_leakDetection}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder leakDetection() {
//		return leakDetection(true);
//	}
//
//	/**
//	 * Configuration property:  Enable leak detection.
//	 *
//	 * <p>
//	 * Enable client and request/response leak detection.
//	 *
//	 * <p>
//	 * Causes messages to be logged to the console if clients or request/response objects are not properly closed
//	 * when the <c>finalize</c> methods are invoked.
//	 *
//	 * <p>
//	 * Automatically enabled with {@link RestClient#RESTCLIENT_debug}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_leakDetection}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder leakDetection(boolean value) {
//		return set(RESTCLIENT_leakDetection, value);
//	}
//
//	/**
//	 * Configuration property:  Marshall
//	 *
//	 * <p>
//	 * Shortcut for specifying the {@link RestClient#RESTCLIENT_serializers} and {@link RestClient#RESTCLIENT_parsers}
//	 * using the serializer and parser defined in a marshall.
//	 *
//	 * @param value The values to add to this setting.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder marshall(Marshall value) {
//		if (value != null)
//			serializer(value.getSerializer()).parser(value.getParser());
//		return this;
//	}
//
//	/**
//	 * Configuration property:  Marshalls
//	 *
//	 * <p>
//	 * Shortcut for specifying the {@link RestClient#RESTCLIENT_serializers} and {@link RestClient#RESTCLIENT_parsers}
//	 * using the serializer and parser defined in a marshall.
//	 *
//	 * @param value The values to add to this setting.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder marshalls(Marshall...value) {
//		for (Marshall m : value) {
//			if (m != null)
//				serializer(m.getSerializer()).parser(m.getParser());
//		}
//		return this;
//	}
//
//	/**
//	 * Configuration property:  Parser.
//	 *
//	 * <p>
//	 * Shortcut for calling {@link #parsers(Class...)}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
//	 * @return This object (for method chaining).
//	 */
//	@SuppressWarnings("unchecked")
//	@ConfigurationProperty
//	public RestClientBuilder parser(Class<? extends Parser> value) {
//		return parsers(value);
//	}
//
//	/**
//	 * Configuration property:  Parser.
//	 *
//	 * <p>
//	 * Shortcut for calling {@link #parsers(Parser...)}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder parser(Parser value) {
//		return parsers(value);
//	}
//
//	/**
//	 * Configuration property:  Parsers.
//	 *
//	 * <p>
//	 * Associates the specified {@link Parser Parsers} with the HTTP client.
//	 *
//	 * <p>
//	 * The parser that best matches the <c>Accept</c> header will be used to parse the response body.
//	 * <br>If no <c>Accept</c> header is specified, the first parser in the list will be used.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
//	 * @return This object (for method chaining).
//	 */
//	@SuppressWarnings("unchecked")
//	@ConfigurationProperty
//	public RestClientBuilder parsers(Class<? extends Parser>...value) {
//		return addTo(RESTCLIENT_parsers, value);
//	}
//
//	/**
//	 * Configuration property:  Parsers.
//	 *
//	 * <p>
//	 * Same as {@link #parsers(Class...)} except takes in a parser instance.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_parsers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link JsonParser#DEFAULT}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder parsers(Parser...value) {
//		return addTo(RESTCLIENT_parsers, value);
//	}
//
//	/**
//	 * Configuration property:  Part parser.
//	 *
//	 * <p>
//	 * The parser to use for parsing POJOs from form data, query parameters, headers, and path variables.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link OpenApiParser}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
//		return set(RESTCLIENT_partParser, value);
//	}
//
//	/**
//	 * Configuration property:  Part parser.
//	 *
//	 * <p>
//	 * Same as {@link #partParser(Class)} but takes in a parser instance.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partParser}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link OpenApiParser}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder partParser(HttpPartParser value) {
//		return set(RESTCLIENT_partParser, value);
//	}
//
//	/**
//	 * Configuration property:  Part serializer.
//	 *
//	 * <p>
//	 * The serializer to use for serializing POJOs in form data, query parameters, headers, and path variables.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link OpenApiSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
//		return set(RESTCLIENT_partSerializer, value);
//	}
//
//	/**
//	 * Configuration property:  Part serializer.
//	 *
//	 * <p>
//	 * Same as {@link #partSerializer(Class)} but takes in a parser instance.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_partSerializer}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default value is {@link OpenApiSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder partSerializer(HttpPartSerializer value) {
//		return set(RESTCLIENT_partSerializer, value);
//	}
//
//	/**
//	 * Configuration property:  Root URI.
//	 *
//	 * <p>
//	 * When set, relative URL strings passed in through the various rest call methods (e.g. {@link RestClient#get(Object)}
//	 * will be prefixed with the specified root.
//	 * <br>This root URL is ignored on those methods if you pass in a {@link URL}, {@link URI}, or an absolute URL string.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_rootUri}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The root URL to prefix to relative URL strings.
//	 * 	<br>Trailing slashes are trimmed.
//	 * 	<br>Usually a <c>String</c> but you can also pass in <c>URI</c> and <c>URL</c> objects as well.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder rootUrl(Object value) {
//		return set(RESTCLIENT_rootUri, value);
//	}
//
//	/**
//	 * Configuration property:  Serializer.
//	 *
//	 * <p>
//	 * Shortcut for calling {@link #serializers(Class...)}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default is {@link JsonSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@SuppressWarnings("unchecked")
//	@ConfigurationProperty
//	public RestClientBuilder serializer(Class<? extends Serializer> value) {
//		return serializers(value);
//	}
//
//	/**
//	 * Configuration property:  Serializer.
//	 *
//	 * <p>
//	 * Shortcut for calling {@link #serializers(Serializer...)}.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default is {@link JsonSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder serializer(Serializer value) {
//		return serializers(value);
//	}
//
//	/**
//	 * Configuration property:  Serializers.
//	 *
//	 * <p>
//	 * Associates the specified {@link Serializer Serializers} with the HTTP client.
//	 *
//	 * <p>
//	 * The serializer that best matches the <c>Content-Type</c> header will be used to serialize the request body.
//	 * <br>If no <c>Content-Type</c> header is specified, the first serializer in the list will be used.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default is {@link JsonSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@SuppressWarnings("unchecked")
//	@ConfigurationProperty
//	public RestClientBuilder serializers(Class<? extends Serializer>...value) {
//		return addTo(RESTCLIENT_serializers, value);
//	}
//
//	/**
//	 * Configuration property:  Serializers.
//	 *
//	 * <p>
//	 * Same as {@link #serializers(Class...)} but takes in serializer instances.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link RestClient#RESTCLIENT_serializers}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this setting.
//	 * 	<br>The default is {@link JsonSerializer}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder serializers(Serializer...value) {
//		return addTo(RESTCLIENT_serializers, value);
//	}
//
//	/**
//	 * Configuration property:  Add <js>"_type"</js> properties when needed.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
//	 * through reflection.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder addBeanTypes(boolean value) {
//		return set(SERIALIZER_addBeanTypes, value);
//	}
//
//	/**
//	 * Configuration property:  Add <js>"_type"</js> properties when needed.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>addBeanTypes(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addBeanTypes}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder addBeanTypes() {
//		return set(SERIALIZER_addBeanTypes, true);
//	}
//
//	/**
//	 * Configuration property:  Add type attribute to root nodes.
//	 *
//	 * <p>
//	 * When disabled, it is assumed that the parser knows the exact Java POJO type being parsed, and therefore top-level
//	 * type information that might normally be included to determine the data type will not be serialized.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder addRootType(boolean value) {
//		return set(SERIALIZER_addRootType, value);
//	}
//
//	/**
//	 * Configuration property:  Add type attribute to root nodes.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>addRootType(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_addRootType}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder addRootType() {
//		return set(SERIALIZER_addRootType, true);
//	}
//
//	/**
//	 * Configuration property:  Automatically detect POJO recursions.
//	 *
//	 * <p>
//	 * Specifies that recursions should be checked for during serialization.
//	 *
//	 * <ul class='notes'>
//	 * 	<li>
//	 * 		Checking for recursion can cause a small performance penalty.
//	 * </ul>
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder detectRecursions(boolean value) {
//		return set(BEANTRAVERSE_detectRecursions, value);
//	}
//
//	/**
//	 * Configuration property:  Automatically detect POJO recursions.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>detectRecursions(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_detectRecursions}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder detectRecursions() {
//		return set(BEANTRAVERSE_detectRecursions, true);
//	}
//
//	/**
//	 * Configuration property:  Ignore recursion errors.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, when we encounter the same object when serializing a tree, we set the value to <jk>null</jk>.
//	 * Otherwise, an exception is thrown.
//	 *
//	 * <ul class='notes'>
//	 * 	<li>
//	 * 		Checking for recursion can cause a small performance penalty.
//	 * </ul>
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ignoreRecursions(boolean value) {
//		return set(BEANTRAVERSE_ignoreRecursions, value);
//	}
//
//	/**
//	 * Configuration property:  Ignore recursion errors.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>ignoreRecursions(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_ignoreRecursions}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ignoreRecursions() {
//		return set(BEANTRAVERSE_ignoreRecursions, true);
//	}
//
//	/**
//	 * Configuration property:  Initial depth.
//	 *
//	 * <p>
//	 * The initial indentation level at the root.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_initialDepth}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <c>0</c>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder initialDepth(int value) {
//		return set(BEANTRAVERSE_initialDepth, value);
//	}
//
//	/**
//	 * Configuration property:  Serializer listener.
//	 *
//	 * <p>
//	 * Class used to listen for errors and warnings that occur during serialization.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_listener}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder listenerS(Class<? extends SerializerListener> value) {
//		return set(SERIALIZER_listener, value);
//	}
//
//	/**
//	 * Configuration property:  Max serialization depth.
//	 *
//	 * <p>
//	 * Abort serialization if specified depth is reached in the POJO tree.
//	 * <br>If this depth is exceeded, an exception is thrown.
//	 * <br>This prevents stack overflows from occurring when trying to serialize models with recursive references.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link BeanTraverseContext#BEANTRAVERSE_maxDepth}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <c>100</c>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder maxDepth(int value) {
//		return set(BEANTRAVERSE_maxDepth, value);
//	}
//
//	/**
//	 * Configuration property:  Sort arrays and collections alphabetically.
//	 *
//	 * <p>
//	 * Copies and sorts the contents of arrays and collections before serializing them.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder sortCollections(boolean value) {
//		return set(SERIALIZER_sortCollections, value);
//	}
//
//	/**
//	 * Configuration property:  Sort arrays and collections alphabetically.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>sortCollections(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortCollections}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder sortCollections() {
//		return set(SERIALIZER_sortCollections, true);
//	}
//
//	/**
//	 * Sets the {@link Serializer#SERIALIZER_sortMaps} property on all serializers in this group.
//	 *
//	 * <p>
//	 * Copies and sorts the contents of maps before serializing them.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
//	 * </ul>
//	 *
//	 * @param value The new value for this property.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder sortMaps(boolean value) {
//		return set(SERIALIZER_sortMaps, value);
//	}
//
//	/**
//	 * Configuration property:  Sort maps alphabetically.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>sortMaps(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_sortMaps}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder sortMaps() {
//		return set(SERIALIZER_sortMaps, true);
//	}
//
//	/**
//	 * Configuration property:  Trim empty lists and arrays.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, empty list values will not be serialized to the output.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimEmptyCollections(boolean value) {
//		return set(SERIALIZER_trimEmptyCollections, value);
//	}
//
//	/**
//	 * Configuration property:  Trim empty lists and arrays.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>trimEmptyCollections(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyCollections}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimEmptyCollections() {
//		return set(SERIALIZER_trimEmptyCollections, true);
//	}
//
//	/**
//	 * Configuration property:  Trim empty maps.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, empty map values will not be serialized to the output.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimEmptyMaps(boolean value) {
//		return set(SERIALIZER_trimEmptyMaps, value);
//	}
//
//	/**
//	 * Configuration property:  Trim empty maps.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>trimEmptyMaps(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimEmptyMaps}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimEmptyMaps() {
//		return set(SERIALIZER_trimEmptyMaps, true);
//	}
//
//	/**
//	 * Configuration property:  Trim null bean property values.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, null bean values will not be serialized to the output.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimNullProperties}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>true</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimNullProperties(boolean value) {
//		return set(SERIALIZER_trimNullProperties, value);
//	}
//
//	/**
//	 * Configuration property:  Trim strings.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being serialized.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimStringsS(boolean value) {
//		return set(SERIALIZER_trimStrings, value);
//	}
//
//	/**
//	 * Configuration property:  Trim strings.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_trimStrings}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimStringsS() {
//		return set(SERIALIZER_trimStrings, true);
//	}
//
//	/**
//	 * Configuration property:  URI context bean.
//	 *
//	 * <p>
//	 * Bean used for resolution of URIs to absolute or root-relative form.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriContext}
//	 * </ul>
//	 *
//	 * @param value The new value for this property.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder uriContext(UriContext value) {
//		return set(SERIALIZER_uriContext, value);
//	}
//
//	/**
//	 * Configuration property:  URI relativity.
//	 *
//	 * <p>
//	 * Defines what relative URIs are relative to when serializing URI/URL objects.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriRelativity}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is {@link UriRelativity#RESOURCE}
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder uriRelativity(UriRelativity value) {
//		return set(SERIALIZER_uriRelativity, value);
//	}
//
//	/**
//	 * Configuration property:  URI resolution.
//	 *
//	 * <p>
//	 * Defines the resolution level for URIs when serializing URI/URL objects.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Serializer#SERIALIZER_uriResolution}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is {@link UriResolution#NONE}
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder uriResolution(UriResolution value) {
//		return set(SERIALIZER_uriResolution, value);
//	}
//
//	/**
//	 * Configuration property:  Maximum indentation.
//	 *
//	 * <p>
//	 * Specifies the maximum indentation level in the serialized document.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_maxIndent}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <c>100</c>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder maxIndent(int value) {
//		return set(WSERIALIZER_maxIndent, value);
//	}
//
//	/**
//	 * Configuration property:  Quote character.
//	 *
//	 * <p>
//	 * This is the character used for quoting attributes and values.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <js>'"'</js>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder quoteChar(char value) {
//		return set(WSERIALIZER_quoteChar, value);
//	}
//
//	/**
//	 * Configuration property:  Quote character.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>quoteChar(<js>'\''</js>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_quoteChar}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder sq() {
//		return set(WSERIALIZER_quoteChar, '\'');
//	}
//
//	/**
//	 * Configuration property:  Use whitespace.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, newlines and indentation and spaces are added to the output to improve readability.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder useWhitespace(boolean value) {
//		return set(WSERIALIZER_useWhitespace, value);
//	}
//
//	/**
//	 * Configuration property:  Use whitespace.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
//	 * </ul>
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder useWhitespace() {
//		return set(WSERIALIZER_useWhitespace, true);
//	}
//
//	/**
//	 * Configuration property:  Use whitespace.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>useWhitespace(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link WriterSerializer#WSERIALIZER_useWhitespace}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder ws() {
//		return set(WSERIALIZER_useWhitespace, true);
//	}
//
//	/**
//	 * Configuration property:  Binary string format.
//	 *
//	 * <p>
//	 * When using the {@link Serializer#serializeToString(Object)} method on stream-based serializers, this defines the format to use
//	 * when converting the resulting byte array to a string.
//	 *
//	 * <ul class='javatree'>
//	 * 	<li class='jf'>{@link OutputStreamSerializer#OSSERIALIZER_binaryFormat}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default is {@link BinaryFormat#HEX}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder binaryOutputFormat(BinaryFormat value) {
//		return set(OSSERIALIZER_binaryFormat, value);
//	}
//
//	/**
//	 * Configuration property:  Auto-close streams.
//	 *
//	 * If <jk>true</jk>, <l>InputStreams</l> and <l>Readers</l> passed into parsers will be closed
//	 * after parsing is complete.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder autoCloseStreams(boolean value) {
//		return set(PARSER_autoCloseStreams, value);
//	}
//
//	/**
//	 * Configuration property:  Auto-close streams.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>autoCloseStreams(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_autoCloseStreams}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder autoCloseStreams() {
//		return set(PARSER_autoCloseStreams, true);
//	}
//
//	/**
//	 * Configuration property:  Debug output lines.
//	 *
//	 * When parse errors occur, this specifies the number of lines of input before and after the
//	 * error location to be printed as part of the exception message.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_debugOutputLines}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <c>5</c>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder debugOutputLines(int value) {
//		set(PARSER_debugOutputLines, value);
//		return this;
//	}
//
//	/**
//	 * Configuration property:  Parser listener.
//	 *
//	 * <p>
//	 * Class used to listen for errors and warnings that occur during parsing.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_listener}
//	 * </ul>
//	 *
//	 * @param value The new value for this property.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder listenerP(Class<? extends ParserListener> value) {
//		return set(PARSER_listener, value);
//	}
//
//	/**
//	 * Configuration property:  Strict mode.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, strict mode for the parser is enabled.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_strict}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder strict(boolean value) {
//		return set(PARSER_strict, value);
//	}
//
//	/**
//	 * Configuration property:  Strict mode.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>strict(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_strict}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder strict() {
//		return set(PARSER_strict, true);
//	}
//
//	/**
//	 * Configuration property:  Trim parsed strings.
//	 *
//	 * <p>
//	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
//	 * the POJO.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimStringsP(boolean value) {
//		return set(PARSER_trimStrings, value);
//	}
//
//	/**
//	 * Configuration property:  Trim parsed strings.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>trimStrings(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_trimStrings}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder trimStringsP() {
//		return set(PARSER_trimStrings, true);
//	}
//
//	/**
//	 * Configuration property:  Unbuffered.
//	 *
//	 * If <jk>true</jk>, don't use internal buffering during parsing.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <jk>false</jk>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder unbuffered(boolean value) {
//		return set(PARSER_unbuffered, value);
//	}
//
//	/**
//	 * Configuration property:  Unbuffered.
//	 *
//	 * <p>
//	 * Shortcut for calling <code>unbuffered(<jk>true</jk>)</code>.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link Parser#PARSER_unbuffered}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder unbuffered() {
//		return set(PARSER_unbuffered, true);
//	}
//
//	/**
//	 * Configuration property:  File charset.
//	 *
//	 * <p>
//	 * The character set to use for reading <c>Files</c> from the file system.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link ReaderParser#RPARSER_fileCharset}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <js>"DEFAULT"</js> which causes the system default to be used.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder fileCharset(String value) {
//		return set(RPARSER_fileCharset, value);
//	}
//
//	/**
//	 * Configuration property:  Input stream charset.
//	 *
//	 * <p>
//	 * The character set to use for converting <c>InputStreams</c> and byte arrays to readers.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link ReaderParser#RPARSER_streamCharset}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is <js>"UTF-8"</js>.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder inputStreamCharset(String value) {
//		return set(RPARSER_streamCharset, value);
//	}
//
//	/**
//	 * Configuration property:  Binary input format.
//	 *
//	 * <p>
//	 * When using the {@link Parser#parse(Object,Class)} method on stream-based parsers and the input is a string, this defines the format to use
//	 * when converting the string into a byte array.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link InputStreamParser#ISPARSER_binaryFormat}
//	 * </ul>
//	 *
//	 * @param value
//	 * 	The new value for this property.
//	 * 	<br>The default value is {@link BinaryFormat#HEX}.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder binaryInputFormat(BinaryFormat value) {
//		return set(ISPARSER_binaryFormat, value);
//	}
//
//	/**
//	 * Configuration property:  Parameter format.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
//	 * </ul>
//	 *
//	 * @param value The new value for this property.
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder paramFormat(String value) {
//		return set(UON_paramFormat, value);
//	}
//
//	/**
//	 * Configuration property:  Parameter format.
//	 *
//	 * <ul class='seealso'>
//	 * 	<li class='jf'>{@link UonSerializer#UON_paramFormat}
//	 * </ul>
//	 *
//	 * @return This object (for method chaining).
//	 */
//	@ConfigurationProperty
//	public RestClientBuilder paramFormatPlain() {
//		return set(UON_paramFormat, "PLAINTEXT");
//	}
//
//	// <CONFIGURATION-PROPERTIES>
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder add(Map<String,Object> properties) {
//		super.add(properties);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder addTo(String name, Object value) {
//		super.addTo(name, value);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder addTo(String name, String key, Object value) {
//		super.addTo(name, key, value);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder apply(PropertyStore copyFrom) {
//		super.apply(copyFrom);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
//		super.applyAnnotations(fromClasses);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder applyAnnotations(Method...fromMethods) {
//		super.applyAnnotations(fromMethods);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
//		super.applyAnnotations(al, r);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder removeFrom(String name, Object value) {
//		super.removeFrom(name, value);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder set(Map<String,Object> properties) {
//		super.set(properties);
//		return this;
//	}
//
//	@Override /* GENERATED - ContextBuilder */
//	public RestClientBuilder set(String name, Object value) {
//		super.set(name, value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder annotations(Annotation...values) {
//		super.annotations(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanClassVisibility(Visibility value) {
//		super.beanClassVisibility(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanConstructorVisibility(Visibility value) {
//		super.beanConstructorVisibility(value);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionary(java.lang.Class<?>...values) {
//		super.beanDictionary(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionary(Object...values) {
//		super.beanDictionary(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionaryRemove(java.lang.Class<?>...values) {
//		super.beanDictionaryRemove(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionaryRemove(Object...values) {
//		super.beanDictionaryRemove(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionaryReplace(java.lang.Class<?>...values) {
//		super.beanDictionaryReplace(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanDictionaryReplace(Object...values) {
//		super.beanDictionaryReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFieldVisibility(Visibility value) {
//		super.beanFieldVisibility(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFilters(java.lang.Class<?>...values) {
//		super.beanFilters(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFilters(Object...values) {
//		super.beanFilters(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFiltersRemove(java.lang.Class<?>...values) {
//		super.beanFiltersRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFiltersRemove(Object...values) {
//		super.beanFiltersRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFiltersReplace(java.lang.Class<?>...values) {
//		super.beanFiltersReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanFiltersReplace(Object...values) {
//		super.beanFiltersReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanMapPutReturnsOldValue() {
//		super.beanMapPutReturnsOldValue();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanMapPutReturnsOldValue(boolean value) {
//		super.beanMapPutReturnsOldValue(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanMethodVisibility(Visibility value) {
//		super.beanMethodVisibility(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beanTypePropertyName(String value) {
//		super.beanTypePropertyName(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansDontRequireSomeProperties() {
//		super.beansDontRequireSomeProperties();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireDefaultConstructor() {
//		super.beansRequireDefaultConstructor();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireDefaultConstructor(boolean value) {
//		super.beansRequireDefaultConstructor(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireSerializable() {
//		super.beansRequireSerializable();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireSerializable(boolean value) {
//		super.beansRequireSerializable(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireSettersForGetters() {
//		super.beansRequireSettersForGetters();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireSettersForGetters(boolean value) {
//		super.beansRequireSettersForGetters(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder beansRequireSomeProperties(boolean value) {
//		super.beansRequireSomeProperties(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpi(Map<String,String> values) {
//		super.bpi(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpi(Class<?> beanClass, String properties) {
//		super.bpi(beanClass, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpi(String beanClassName, String properties) {
//		super.bpi(beanClassName, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpro(Map<String,String> values) {
//		super.bpro(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpro(Class<?> beanClass, String properties) {
//		super.bpro(beanClass, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpro(String beanClassName, String properties) {
//		super.bpro(beanClassName, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpwo(Map<String,String> values) {
//		super.bpwo(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpwo(Class<?> beanClass, String properties) {
//		super.bpwo(beanClass, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpwo(String beanClassName, String properties) {
//		super.bpwo(beanClassName, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpx(Map<String,String> values) {
//		super.bpx(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpx(Class<?> beanClass, String properties) {
//		super.bpx(beanClass, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder bpx(String beanClassName, String properties) {
//		super.bpx(beanClassName, properties);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder debug() {
//		super.debug();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder debug(boolean value) {
//		super.debug(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionary(java.lang.Class<?>...values) {
//		super.dictionary(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionary(Object...values) {
//		super.dictionary(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionaryRemove(java.lang.Class<?>...values) {
//		super.dictionaryRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionaryRemove(Object...values) {
//		super.dictionaryRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionaryReplace(java.lang.Class<?>...values) {
//		super.dictionaryReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dictionaryReplace(Object...values) {
//		super.dictionaryReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dontIgnorePropertiesWithoutSetters() {
//		super.dontIgnorePropertiesWithoutSetters();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dontIgnoreTransientFields() {
//		super.dontIgnoreTransientFields();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dontIgnoreUnknownNullBeanProperties() {
//		super.dontIgnoreUnknownNullBeanProperties();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder dontUseInterfaceProxies() {
//		super.dontUseInterfaceProxies();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public <T> RestClientBuilder example(Class<T> pojoClass, T o) {
//		super.example(pojoClass, o);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public <T> RestClientBuilder exampleJson(Class<T> pojoClass, String json) {
//		super.exampleJson(pojoClass, json);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder examples(String json) {
//		super.examples(json);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder excludeProperties(Map<String,String> values) {
//		super.excludeProperties(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder excludeProperties(Class<?> beanClass, String properties) {
//		super.excludeProperties(beanClass, properties);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder excludeProperties(String beanClassName, String value) {
//		super.excludeProperties(beanClassName, value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder fluentSetters() {
//		super.fluentSetters();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder fluentSetters(boolean value) {
//		super.fluentSetters(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreInvocationExceptionsOnGetters() {
//		super.ignoreInvocationExceptionsOnGetters();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
//		super.ignoreInvocationExceptionsOnGetters(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreInvocationExceptionsOnSetters() {
//		super.ignoreInvocationExceptionsOnSetters();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
//		super.ignoreInvocationExceptionsOnSetters(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignorePropertiesWithoutSetters(boolean value) {
//		super.ignorePropertiesWithoutSetters(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreTransientFields(boolean value) {
//		super.ignoreTransientFields(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreUnknownBeanProperties() {
//		super.ignoreUnknownBeanProperties();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreUnknownBeanProperties(boolean value) {
//		super.ignoreUnknownBeanProperties(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder ignoreUnknownNullBeanProperties(boolean value) {
//		super.ignoreUnknownNullBeanProperties(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
//		super.implClass(interfaceClass, implClass);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder implClasses(Map<String,Class<?>> values) {
//		super.implClasses(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder includeProperties(Map<String,String> values) {
//		super.includeProperties(values);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder includeProperties(Class<?> beanClass, String value) {
//		super.includeProperties(beanClass, value);
//		return this;
//	}
//
//	@Deprecated @Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder includeProperties(String beanClassName, String value) {
//		super.includeProperties(beanClassName, value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder locale(Locale value) {
//		super.locale(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder mediaType(MediaType value) {
//		super.mediaType(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClasses(java.lang.Class<?>...values) {
//		super.notBeanClasses(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClasses(Object...values) {
//		super.notBeanClasses(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClassesRemove(java.lang.Class<?>...values) {
//		super.notBeanClassesRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClassesRemove(Object...values) {
//		super.notBeanClassesRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClassesReplace(java.lang.Class<?>...values) {
//		super.notBeanClassesReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanClassesReplace(Object...values) {
//		super.notBeanClassesReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackages(Object...values) {
//		super.notBeanPackages(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackages(String...values) {
//		super.notBeanPackages(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackagesRemove(Object...values) {
//		super.notBeanPackagesRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackagesRemove(String...values) {
//		super.notBeanPackagesRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackagesReplace(Object...values) {
//		super.notBeanPackagesReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder notBeanPackagesReplace(String...values) {
//		super.notBeanPackagesReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwaps(java.lang.Class<?>...values) {
//		super.pojoSwaps(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwaps(Object...values) {
//		super.pojoSwaps(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwapsRemove(java.lang.Class<?>...values) {
//		super.pojoSwapsRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwapsRemove(Object...values) {
//		super.pojoSwapsRemove(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwapsReplace(java.lang.Class<?>...values) {
//		super.pojoSwapsReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder pojoSwapsReplace(Object...values) {
//		super.pojoSwapsReplace(values);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
//		super.propertyNamer(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder sortProperties() {
//		super.sortProperties();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder sortProperties(boolean value) {
//		super.sortProperties(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder timeZone(TimeZone value) {
//		super.timeZone(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder useEnumNames() {
//		super.useEnumNames();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder useEnumNames(boolean value) {
//		super.useEnumNames(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder useInterfaceProxies(boolean value) {
//		super.useInterfaceProxies(value);
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder useJavaBeanIntrospector() {
//		super.useJavaBeanIntrospector();
//		return this;
//	}
//
//	@Override /* GENERATED - BeanContextBuilder */
//	public RestClientBuilder useJavaBeanIntrospector(boolean value) {
//		super.useJavaBeanIntrospector(value);
//		return this;
//	}
//


}
