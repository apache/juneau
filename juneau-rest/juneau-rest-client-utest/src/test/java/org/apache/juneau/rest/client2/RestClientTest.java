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
import static org.apache.juneau.testutils.TestUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestRequest;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestClientTest {

	public static class Bean {
		public int f;

		public Bean init() {
			f = 1;
			return this;
		}

		public void check() {
			assertEquals(f, 1);
		}

		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	public static Bean bean = new Bean().init();

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public Bean postBean(@Body Bean b) {
			return b;
		}
		@RestMethod(path="/echo")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestMethod(path="/echo")
		public String postEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestMethod(path="/echoBody")
		public Reader postEchoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getBody().getReader();
		}
		@RestMethod(path="/ok")
		public Ok getOk() {
			return Ok.OK;
		}
		@RestMethod(path="/checkHeader")
		public String[] getHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
		@RestMethod(path="/checkQuery")
		public Reader getQuery(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getQuery().asQueryString());
		}
		@RestMethod(path="/checkFormData")
		public Reader postFormData(org.apache.juneau.rest.RestRequest req) {
			return new StringReader(req.getFormData().asQueryString());
		}
	}

	private static final Calendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR.set(2000, 11, 31, 12, 34, 56);
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
		MockRestClient
			.create(A.class)
			.simpleJson()
			.logToConsole()
			.build()
			.post("/bean", bean)
			.complete();
	}

	@Test
	public void b02_logTo() throws Exception {
		MockLogger ml = new MockLogger();
		MockRestClient
			.create(A.class)
			.simpleJson()
			.logger(ml)
			.logRequests(DetailLevel.FULL, Level.SEVERE)
			.build()
			.post("/bean", bean)
			.complete();
		ml.assertLevel(Level.SEVERE);
		ml.assertMessageContains(
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json+simple",
			"---request entity---",
			"application/json+simple",
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
		MockRestClient
			.create(A.class)
			.simpleJson()
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
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("A1: 1", "A2: 2")
			.getHeader("B1").assertValue("1")
			.getHeader("B2").assertValue("2")
		;
	}

	@Test
	public void c02_httpProcessor() throws RestCallException {
		MockRestClient
			.create(A.class)
			.simpleJson()
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
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("A1: 1")
			.getHeader("B1").assertValue("1");
	}

	@Test
	public void c03_requestExecutor() throws RestCallException {
		AtomicBoolean b1 = new AtomicBoolean();
		MockRestClient
			.create(A.class)
			.simpleJson()
			.requestExecutor(new HttpRequestExecutor() {
				@Override
				public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws HttpException, IOException {
					b1.set(true);
					return super.execute(request, conn, context);
				}
			})
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo");
		assertTrue(b1.get());
	}

	@Test
	public void c04_defaultHeaders() throws RestCallException {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.defaultHeaders(AList.of(new org.apache.http.message.BasicHeader("Foo", "bar")))
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo","Foo: bar");
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
		MockRestClient
			.create(A.class)
			.simpleJson()
			.pooled()
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("HTTP GET /echo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Authentication
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E extends BasicRest {
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
		MockRestClient
			.create(E.class)
			.simpleJson()
			.basicAuth(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "user", "pw")
			.build()
			.get("/echo")
			.run()
			.getBody().assertContains("OK");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_basicHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.header("Foo","bar")
			.build()
			.get("/checkHeader")
			.header("Foo","baz")
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f02_beanHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo", bean)
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header("Foo", bean)
			.run()
			.getBody().assertValue("['f=1','f=1']");
	}

	@Test
	public void f03_nullHeaders() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo", null)
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header("Foo", null)
			.run()
			.getBody().assertValue("null");
	}

	@Test
	public void f04_header_Header() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new org.apache.http.message.BasicHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header(new org.apache.http.message.BasicHeader("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f05_header_NameValuePair() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new BasicNameValuePair("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header(new BasicNameValuePair("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f06_header_HttpHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new BasicObjectHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header(new BasicObjectHeader("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f07_headers_Header() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(new org.apache.http.message.BasicHeader("Foo", "bar"),new org.apache.http.message.BasicHeader("Baz", "baz"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(new org.apache.http.message.BasicHeader("Foo", "baz"),new org.apache.http.message.BasicHeader("Baz", "quux"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f08_headers_OMap() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(OMap.of("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(OMap.of("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f09_headers_Map() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(AMap.of("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(AMap.of("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f10_headers_NameValuePairs() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(NameValuePairs.of("Foo","bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(NameValuePairs.of("Foo","baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f11_headers_NameValuePair() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(new BasicNameValuePair("Foo","bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(new BasicNameValuePair("Foo","baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f12_headers_pairs() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headerPairs("Foo", "bar")
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headerPairs("Foo", "baz")
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f13_headers_HttpHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(new BasicObjectHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(new BasicObjectHeader("Foo", "baz"))
			.run()
			.getBody().assertValue("['bar','baz']");
	}

	@Test
	public void f14_headers_accept() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.accept("text/foo")
			.header("Check", "Accept")
			.build()
			.get("/checkHeader")
			.accept("text/plain")
			.run()
			.getBody().assertValue("['text/foo','text/plain']");
	}

	@Test
	public void f15_headers_acceptCharset() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.acceptCharset("UTF-8")
			.header("Check", "Accept-Charset")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['UTF-8']");
	}

	@Test
	public void f16_headers_acceptEncoding() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.acceptEncoding("identity")
			.header("Check", "Accept-Encoding")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['identity']");
	}

	@Test
	public void f17_headers_acceptLanguage() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.acceptLanguage("en")
			.header("Check", "Accept-Language")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['en']");
	}

	@Test
	public void f18_headers_authorization() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.authorization("foo")
			.header("Check", "Authorization")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f19_headers_cacheControl() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.cacheControl("none")
			.header("Check", "Cache-Control")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['none']");
	}

	@Test
	public void f20_headers_clientVersion() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.clientVersion("1")
			.header("Check", "X-Client-Version")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['1']");
	}

	@Test
	public void f21_headers_connection() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.connection("foo")
			.header("Check", "Connection")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f22_headers_contentLength() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.contentLength("123")
			.header("Check", "Content-Length")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['123']");
	}

	@Test
	public void f23_headers_contentType() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.contentType("foo")
			.header("Check", "Content-Type")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f24_headers_date() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.date("123")
			.header("Check", "Date")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['123']");
	}

	@Test
	public void f25_headers_expect() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.expect("foo")
			.header("Check", "Expect")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f26_headers_forwarded() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.forwarded("foo")
			.header("Check", "Forwarded")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f27_headers_from() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.from("foo")
			.header("Check", "From")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f28_headers_host() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.host("foo")
			.header("Check", "Host")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f29_headers_ifMatch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.ifMatch("foo")
			.header("Check", "If-Match")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f30_headers_ifModifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.ifModifiedSince("foo")
			.header("Check", "If-Modified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f31_headers_ifNoneMatch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.ifNoneMatch("foo")
			.header("Check", "If-None-Match")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f32_headers_ifRange() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.ifRange("foo")
			.header("Check", "If-Range")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f33_headers_ifUnmodifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.ifUnmodifiedSince("foo")
			.header("Check", "If-Unmodified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f34_headers_maxForwards() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.maxForwards("10")
			.header("Check", "Max-Forwards")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['10']");
	}

	@Test
	public void f35_headers_noTrace() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.noTrace()
			.header("Check", "No-Trace")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['true']");
	}

	@Test
	public void f36_headers_origin() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.origin("foo")
			.header("Check", "Origin")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f37_headers_pragma() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.pragma("foo")
			.header("Check", "Pragma")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f38_headers_proxyAuthorization() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.proxyAuthorization("foo")
			.header("Check", "Proxy-Authorization")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f39_headers_range() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.range("foo")
			.header("Check", "Range")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f40_headers_referer() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.referer("foo")
			.header("Check", "Referer")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f41_headers_te() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.te("foo")
			.header("Check", "TE")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f42_headers_userAgent() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.userAgent("foo")
			.header("Check", "User-Agent")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f43_headers_upgrade() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.upgrade("foo")
			.header("Check", "Upgrade")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f44_headers_via() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.via("foo")
			.header("Check", "Via")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void f45_headers_warning() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.warning("foo")
			.header("Check", "Warning")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header Beans
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_headers_accept() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Accept("text/foo"))
			.header("Check", "Accept")
			.build()
			.get("/checkHeader")
			.header(new Accept("text/plain"))
			.run()
			.getBody().assertValue("['text/foo','text/plain']");
	}

	@Test
	public void g02_headers_acceptCharset() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new AcceptCharset("UTF-8"))
			.header("Check", "Accept-Charset")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['UTF-8']");
	}

	@Test
	public void g03_headers_acceptEncoding() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new AcceptEncoding("identity"))
			.header("Check", "Accept-Encoding")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['identity']");
	}

	@Test
	public void g04_headers_acceptLanguage() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new AcceptLanguage("en"))
			.header("Check", "Accept-Language")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['en']");
	}

	@Test
	public void g05_headers_authorization() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Authorization("foo"))
			.header("Check", "Authorization")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g06_headers_cacheControl() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new CacheControl("none"))
			.header("Check", "Cache-Control")
			.header("X-Expect", "none")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['none']");
	}

	@Test
	public void g07_headers_clientVersion() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new ClientVersion("1"))
			.header("Check", "X-Client-Version")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['1']");
	}

	@Test
	public void g08_headers_connection() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Connection("foo"))
			.header("Check", "Connection")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g09_headers_contentLength() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new ContentLength(123))
			.header("Check", "Content-Length")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['123']");
	}

	@Test
	public void g10_headers_contentType() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new ContentType("foo"))
			.header("Check", "Content-Type")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g11a_headers_date() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new org.apache.juneau.http.Date("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "Date")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g11b_headers_date() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new org.apache.juneau.http.Date(CALENDAR))
			.header("Check", "Date")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g12_headers_expect() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Expect("foo"))
			.header("Check", "Expect")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g13_headers_forwarded() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Forwarded("foo"))
			.header("Check", "Forwarded")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g14_headers_from() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new From("foo"))
			.header("Check", "From")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g15_headers_host() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Host("foo"))
			.header("Check", "Host")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g16_headers_ifMatch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfMatch("\"foo\""))
			.header("Check", "If-Match")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['\"foo\"']");
	}

	@Test
	public void g17a_headers_ifModifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfModifiedSince(CALENDAR))
			.header("Check", "If-Modified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g17b_headers_ifModifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfModifiedSince("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "If-Modified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g18_headers_ifNoneMatch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfNoneMatch("\"foo\""))
			.header("Check", "If-None-Match")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['\"foo\"']");
	}

	@Test
	public void g19_headers_ifRange() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfRange("foo"))
			.header("Check", "If-Range")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g20a_headers_ifUnmodifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfUnmodifiedSince(CALENDAR))
			.header("Check", "If-Unmodified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g20b_headers_ifUnmodifiedSince() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new IfUnmodifiedSince("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "If-Unmodified-Since")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g21_headers_maxForwards() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new MaxForwards(10))
			.header("Check", "Max-Forwards")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['10']");
	}

	@Test
	public void g22_headers_noTrace() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new NoTrace("true"))
			.header("Check", "No-Trace")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['true']");
	}

	@Test
	public void g23_headers_origin() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Origin("foo"))
			.header("Check", "Origin")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g24_headers_pragma() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Pragma("foo"))
			.header("Check", "Pragma")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g25_headers_proxyAuthorization() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new ProxyAuthorization("foo"))
			.header("Check", "Proxy-Authorization")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g26_headers_range() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Range("foo"))
			.header("Check", "Range")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g27_headers_referer() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Referer("foo"))
			.header("Check", "Referer")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g28_headers_te() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new TE("foo"))
			.header("Check", "TE")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g29_headers_userAgent() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new UserAgent("foo"))
			.header("Check", "User-Agent")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g30_headers_upgrade() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Upgrade("foo"))
			.header("Check", "Upgrade")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g31_headers_via() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Via("foo"))
			.header("Check", "Via")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	@Test
	public void g32_headers_warning() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(new Warning("foo"))
			.header("Check", "Warning")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other Header tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_multipleHeaders() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.headerPairs("Foo","bar","Foo","baz")
			.header("Foo","qux")
			.build()
			.get("/checkHeader")
			.headerPairs("Foo","q1x","Foo","q2x")
			.run()
			.getBody().assertValue("['bar','baz','qux','q1x','q2x']");
	}

	@Test
	public void h02_multipleHeaders_withRequest() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.headerPairs("Foo","bar","Foo","baz")
			.build()
			.get("/checkHeader")
			.header("Foo","qux")
			.run()
			.getBody().assertValue("['bar','baz','qux']");
	}

	@Test
	public void h03_dontOverrideAccept() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Accept")
			.header("Accept", "text/plain")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['text/plain']");
	}

	@Test
	public void h04_dontOverrideAccept_withRequest() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Accept")
			.header("Accept", "text/foo")
			.build()
			.get("/checkHeader")
			.header("Accept","text/plain")
			.run()
			.getBody().assertValue("['text/foo','text/plain']");
	}

	@Test
	public void h04b_dontOverrideAccept_withRequest() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Accept")
			.header("Accept", "text/foo")
			.build();
		RestRequest req = rc.get("/checkHeader");
		req.setHeader("Accept","text/plain");
		req
			.run()
			.getBody().assertValue("['text/plain']");
	}

	@Test
	public void h05_dontOverrideContentType() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Content-Type")
			.header("Content-Type", "text/plain")
			.build()
			.get("/checkHeader")
			.run()
			.getBody().assertValue("['text/plain']");
	}

	@Test
	public void h06_dontOverrideAccept_withRequest() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Content-Type")
			.header("Content-Type", "text/foo")
			.build()
			.get("/checkHeader")
			.header("Content-Type", "text/plain")
			.run()
			.getBody().assertValue("['text/foo','text/plain']");
	}

	@Test
	public void h07_header_HttpPartSerializer() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.header("Foo", bean, new XPartSerializer(), null)
			.build()
			.get("/checkHeader")
			.header(AddFlag.DEFAULT_FLAGS,"Foo",bean,new XPartSerializer().createPartSession(null),null)
			.run()
			.getBody().assertValue("['x{f:1}','x{f:1}']");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_query_basic() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.query("Foo","bar")
			.query("Foo",new StringBuilder("baz"))
			.build()
			.get("/checkQuery")
			.run()
			.getBody().assertValue("Foo=bar&Foo=baz");
	}

	@Test
	public void i02_query_objects() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.query(new BasicNameValuePair("Foo","f1"))
			.query(OMap.of("Foo","f2"))
			.query(AMap.of("Foo","f3"))
			.query(NameValuePairs.of("Foo","f4","Foo","f5"))
			.query(new BasicNameValuePair("Foo","f6"), new BasicNameValuePair("Foo","f7"))
			.build()
			.get("/checkQuery")
			.run()
			.getBody().assertValue("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Form data
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void j01_formData_basic() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formData("Foo","bar")
			.formData("Foo",new StringBuilder("baz"))
			.build()
			.post("/checkFormData")
			.run()
			.getBody().assertValue("Foo=bar&Foo=baz");
	}

	@Test
	public void j02_formData_objects() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formData(new BasicNameValuePair("Foo","f1"))
			.formData(OMap.of("Foo","f2"))
			.formData(AMap.of("Foo","f3"))
			.formData(NameValuePairs.of("Foo","f4","Foo","f5"))
			.formData(new BasicNameValuePair("Foo","f6"), new BasicNameValuePair("Foo","f7"))
			.build()
			.post("/checkFormData")
			.run()
			.getBody().assertValue("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RestClient properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class XCallHandler extends BasicRestCallHandler {

		public XCallHandler(RestClient client) {
			super(client);
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
			request.addHeader("Check", "Foo");
			request.addHeader("Foo", "bar");
			return super.execute(target, request, context);
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
			request.addHeader("Check", "Foo");
			request.addHeader("Foo", "baz");
			return super.execute(target, request, context);
		}
	}
	@Test
	public void k01_restClient_CallHandlerClass() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.callHandler(XCallHandler.class)
			.header("Foo", "f1")
			.build()
			.get("/checkHeader")
			.header("Foo","f2")
			.run()
			.getBody().assertValue("['f1','f2','baz']");
	}

	@Test
	public void k03_restClient_errorCodes() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.errorCodes(x -> x == 200)
				.build()
				.get("/echo")
				.run();
			fail("Exception expected.");
		} catch (RestCallException e) {
			assertEquals(200, e.getResponseCode());
		}
	}

	@Test
	public void k04_restClient_executorService() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.executorService(es, true)
			.build();
		assertEquals(es, rc.getExecutorService(false));
		rc
			.get("/echo")
			.runFuture()
			.get()
			.assertStatusCode(200)
			.getBody().assertContains("HTTP GET /echo");
	}

	@Test
	public void k05_restClient_keepHttpClientOpenBoolean() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.keepHttpClientOpen()
			.build();
		CloseableHttpClient c = rc.httpClient;
		rc.close();
		MockRestClient
			.create(A.class)
			.simpleJson()
			.httpClient(c)
			.build()
			.get("/ok")
			.runFuture()
			.get()
			.getBody().assertContains("OK");
	}

	@Test
	public void k06_restClient_keepHttpClientOpen() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.keepHttpClientOpen(true)
			.build();
		CloseableHttpClient c = rc.httpClient;
		rc.close();
		MockRestClient
			.create(A.class)
			.simpleJson()
			.httpClient(c)
			.build()
			.get("/ok")
			.runFuture()
			.get()
			.getBody().assertContains("OK");
	}

	public static class XRestCallInterceptor extends BasicRestCallInterceptor {
		public static int x;
		@Override
		public void onInit(RestRequest req) throws Exception {
			x = 1;
			req.header("Foo", "f2");
		}

		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			x += 10;
			res.addHeader("Bar", "b1");
		}

		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			x += 100;
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void k07_restClient_interceptorsClasses() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo","f1")
			.interceptors(XRestCallInterceptor.class)
			.build()
			.get("/checkHeader")
			.header("Check","foo")
			.header("Foo","f3")
			.run()
			.getBody().assertValue("['f1','f2','f3']")
			.getHeader("Bar").assertValue("b1");
		assertEquals(111, XRestCallInterceptor.x);
	}

	@Test
	public void k08_restClient_interceptorsObjects() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo","f1")
			.interceptors(new XRestCallInterceptor())
			.build()
			.get("/checkHeader")
			.header("Check","foo")
			.header("Foo","f3")
			.run()
			.getBody().assertValue("['f1','f2','f3']")
			.getHeader("Bar").assertValue("b1");
		assertEquals(111, XRestCallInterceptor.x);
	}

	public static class K09RestClient extends RestClient {
		private static String lastMessage;

		public K09RestClient(PropertyStore ps) {
			super(ps);
		}

		@Override
		public void log(Level level, String msg, Object...args) {
			lastMessage = msg;
		}
	}

	@Test
	public void k09a_restClient_leakDetection() throws Throwable {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.leakDetection()
			.build(K09RestClient.class)
			.finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.", K09RestClient.lastMessage);
	}

	@Test
	public void k09b_restClient_leakDetection_withThreadCreationStack() throws Throwable {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.debug()
			.build(K09RestClient.class)
			.finalize();
		assertTrue(K09RestClient.lastMessage.startsWith("WARNING:  RestClient garbage collected before it was finalized.\nCreation Stack:\n\t"));
	}

	@Test
	public void k10_restClient_leakDetectionBoolean() throws Throwable {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.leakDetection(true)
			.build(K09RestClient.class)
			.finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.", K09RestClient.lastMessage);

		K09RestClient.lastMessage = null;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.leakDetection(false)
			.build(K09RestClient.class)
			.finalize();
		assertNull(K09RestClient.lastMessage);
	}

	@Test
	public void k11_restClient_marshallObject() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.marshall(Xml.DEFAULT)
			.build();

		Bean b = rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache()
			.assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);

		assertEqualObjects(b, bean);
	}

	@Test
	public void k12_restClient_marshallsObjects() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.marshalls(Xml.DEFAULT,Json.DEFAULT)
			.build();

		rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache().assertValue("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.getBody().cache()
			.assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.getBody().cache().assertValue("{\"f\":1}")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);
	}

	@Test
	public void k13_restClient_serializerClass_parserClass() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.serializer(XmlSerializer.class)
			.parser(XmlParser.class)
			.build();

		Bean b = rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache().assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);

		assertEqualObjects(b, bean);
	}

	@Test
	public void k14_restClient_serializerObject_parserObject() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.serializer(XmlSerializer.DEFAULT)
			.parser(XmlParser.DEFAULT)
			.build();

		Bean b = rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache().assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);

		assertEqualObjects(b, bean);
	}

	@Test
	public void k15_restClient_serializersClasses_parsersClasses() throws Exception {
		@SuppressWarnings("unchecked")
		RestClient rc = MockRestClient
			.create(A.class)
			.serializers(XmlSerializer.class,JsonSerializer.class)
			.parsers(XmlParser.class,JsonParser.class)
			.build();

		rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache().assertValue("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.getBody().cache().assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.getBody().cache().assertValue("{\"f\":1}")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);
	}

	@Test
	public void k16_restClient_serializersObjects_parsersObjects() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.serializers(XmlSerializer.DEFAULT,JsonSerializer.DEFAULT)
			.parsers(XmlParser.DEFAULT,JsonParser.DEFAULT)
			.build();

		rc
			.post("/echoBody", bean)
			.run()
			.getBody().cache().assertValue("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.getBody().cache().assertValue("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.getBody().cache().assertValue("{\"f\":1}")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);
	}

	public static class XPartSerializer extends SimplePartSerializer {
		@Override
		public SimplePartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new SimplePartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) {
					if (value instanceof Bean)
						return "x" + SimpleJson.DEFAULT.toString(value);
					return "x" + super.serialize(type, schema, value);
				}
			};
		}
	}

	public static class XPartParser extends SimplePartParser {
		@Override
		public SimplePartParserSession createPartSession(ParserSessionArgs args) {
			return new SimplePartParserSession() {
				@Override
				public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
					if (toType.isInstanceOf(Bean.class))
						return SimpleJson.DEFAULT.read(in.substring(1), toType);
					return super.parse(null, schema, in, toType);
				}
			};
		}
	}

	@Rest(partSerializer=XPartSerializer.class, partParser=XPartParser.class)
	public static class K extends BasicRest {
		@RestMethod(path="/")
		public Ok get(@Header(name="Foo",multi=true) Bean[] foo, org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws Exception {
			assertEquals(2, foo.length);
			assertObjectEquals("['x{f:1}','x{f:1}']", req.getHeaders().getAll("Foo", String[].class));
			assertEquals("{f:1}", foo[0].toString());
			assertEquals("{f:1}", foo[1].toString());
			res.header("Foo", bean);
			return Ok.OK;
		}
	}

	@Test
	public void k17_restClient_partSerializer_partParser_Class() throws Exception {
		RestClient rc = MockRestClient
			.create(K.class)
			.simpleJson()
			.header("Foo",bean)
			.partSerializer(XPartSerializer.class)
			.partParser(XPartParser.class)
			.header("")
			.build();
		Bean b = rc
			.get("/")
			.header("Foo",bean)
			.run()
			.getHeader("Foo").assertValue("x{f:1}")
			.getHeader("Foo").as(Bean.class);
		assertEquals("{f:1}", b.toString());
	}

	@Test
	public void k18_restClient_partSerializer_partParser_Object() throws Exception {
		RestClient rc = MockRestClient
			.create(K.class)
			.simpleJson()
			.header("Foo",bean)
			.partSerializer(new XPartSerializer())
			.partParser(new XPartParser())
			.header("")
			.build();
		Bean b = rc
			.get("/")
			.header("Foo",bean)
			.run()
			.getHeader("Foo").assertValue("x{f:1}")
			.getHeader("Foo").as(Bean.class);
		assertEquals("{f:1}", b.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class L1 {
		public Object f1;

		public L1 init() {
			f1 = new L2().init();
			return this;
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="L")
	public static class L2 {
		public int f2;

		public L2 init() {
			f2 = 1;
			return this;
		}
	}

	@Test
	public void l01a_serializer_addBeanTypes() throws Exception {
		L1 l1 = new L1().init();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes(true)
			.build()
			.post("/echoBody", l1)
			.run()
			.getBody().assertValue("{f1:{_type:'L',f2:1}}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes(false)
			.build()
			.post("/echoBody", l1)
			.run()
			.getBody().assertValue("{f1:{f2:1}}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes()
			.build()
			.post("/echoBody", l1)
			.run()
			.getBody().assertValue("{f1:{_type:'L',f2:1}}");
	}

	@Test
	public void l03_serializer_addRootType() throws Exception {
		L2 l2 = new L2().init();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addRootType(true)
			.build()
			.post("/echoBody", l2)
			.run()
			.getBody().assertValue("{f2:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes()
			.addRootType(false)
			.build()
			.post("/echoBody", l2)
			.run()
			.getBody().assertValue("{f2:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes()
			.addRootType(true)
			.build()
			.post("/echoBody", l2)
			.run()
			.getBody().assertValue("{_type:'L',f2:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes()
			.addRootType()
			.build()
			.post("/echoBody", l2)
			.run()
			.getBody().assertValue("{_type:'L',f2:1}");
	}

	@Test
	public void l05_serializer_detectRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.detectRecursions()
				.build()
				.post("/echoBody", l1)
				.run();
		} catch (RestCallException e) {
			assertTrue(e.getCause().getCause().getMessage().startsWith("Recursion occurred"));
		}

		try {
			 MockRestClient
				.create(A.class)
				.simpleJson()
				.detectRecursions(true)
				.build()
			 	.post("/echoBody", l1)
				.run();
		} catch (RestCallException e) {
			assertTrue(e.getCause().getCause().getMessage().startsWith("Recursion occurred"));
		}
	}

	@Test
	public void l07_serializer_ignoreRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;

		MockRestClient
			.create(A.class)
			.simpleJson()
			.ignoreRecursions()
			.build()
			.post("/echoBody", l1)
			.run()
			.getBody().assertValue("{}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.ignoreRecursions(true)
			.build()
			.post("/echoBody", l1)
			.run()
			.getBody().assertValue("{}");
	}

	@Test
	public void l09_serializer_initialDepth() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.initialDepth(2)
			.ws()
			.build()
			.post("/echoBody", bean)
			.run()
			.getBody().assertValue("\t\t{\n\t\t\tf: 1\n\t\t}");
	}

	public static class L10 extends SerializerListener {
		public static volatile Throwable T;
		public static volatile String MSG;

		@Override
		public void onError(SerializerSession session, Throwable t, String msg) {
			T = t;
			MSG = msg;
		}
	}

	public static class L11 {
		public Bean f;

		public L11 init() {
			f = bean;
			return this;
		}
	}

	@Test
	public void l11_serializer_maxDepth() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.maxDepth(1)
			.build()
			.post("/echoBody", new L11().init())
			.run()
			.getBody().assertValue("{}");
	}

	@Test
	public void l12_serializer_sortCollections() throws Exception {
		String[] x = new String[]{"c","a","b"};

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortCollections(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("['a','b','c']");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortCollections()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("['a','b','c']");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortCollections(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("['c','a','b']");
	}

	@Test
	public void l14_serializer_sortMapsBoolean() throws Exception {
		AMap<String,Integer> x = AMap.of("c", 3, "a", 1, "b", 2);

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortMaps(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{a:1,b:2,c:3}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortMaps()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{a:1,b:2,c:3}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortMaps(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{c:3,a:1,b:2}");
	}

	public static class L16 {
		public List<String> f1 = AList.of();
		public String[] f2 = new String[0];
	}

	@Test
	public void l16_serializer_trimEmptyCollections() throws Exception {
		L16 x = new L16();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyCollections(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyCollections()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyCollections(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f1:[],f2:[]}");
	}

	public static class L18 {
		public Map<String,String> f1 = AMap.of();
		public OMap f2 = OMap.of();
	}

	@Test
	public void l18_serializer_trimEmptyMaps() throws Exception {
		L18 x = new L18();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyMaps(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyMaps()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimEmptyMaps(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f1:{},f2:{}}");

	}

	public static class L20 {
		public String f;
	}

	@Test
	public void l20_serializer_trimNullPropertiesBoolean() throws Exception {
		L20 x = new L20();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.keepNullProperties()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:null}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.keepNullProperties(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:null}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.keepNullProperties(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{}");
	}

	public static class L21 {
		public String f = " foo ";
	}

	@Test
	public void l21_serializer_trimStringsOnWrite() throws Exception {
		L21 x = new L21();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnWrite()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:'foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnWrite(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:'foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnWrite(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:' foo '}");
	}

	public static class L23 {
		@URI
		public String f = "foo";
	}

	@Test
	public void l23_serializer_uriContext_uriResolution_uriRelativity() throws Exception {
		L23 x = new L23();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.uriResolution(UriResolution.ABSOLUTE)
			.uriRelativity(UriRelativity.PATH_INFO)
			.uriContext(new UriContext("http://localhost:80", "/context", "/resource", "/path"))
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:'http://localhost:80/context/resource/foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.uriResolution(UriResolution.NONE)
			.uriRelativity(UriRelativity.RESOURCE)
			.uriContext(new UriContext("http://localhost:80", "/context", "/resource", "/path"))
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f:'foo'}");
	}

	public static class L26 {
		public int f1;
		public L26 f2;

		public L26 init() {
			L26 x2 = new L26(), x3 = new L26();
			this.f1 = 1;
			x2.f1 = 2;
			x3.f1 = 3;
			this.f2 = x2;
			x2.f2 = x3;
			return this;
		}
	}

	@Test
	public void l26_serializer_maxIndent() throws Exception {
		L26 x = new L26().init();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.maxIndent(2)
			.ws()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}");
	}

	public static class L27 {
		public String f1 = "foo";
	}

	@Test
	public void l27_serializer_quoteChar() throws Exception {
		L27 x = new L27();

		MockRestClient
			.create(A.class)
			.json()
			.quoteChar('\'')
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{'f1':'foo'}");

		MockRestClient
			.create(A.class)
			.json()
			.quoteChar('|')
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{|f1|:|foo|}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.quoteChar('|')
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f1:|foo|}");
	}

	@Test
	public void l28_serializer_sq() throws Exception {
		L27 x = new L27();

		MockRestClient
			.create(A.class)
			.json()
			.sq()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{'f1':'foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sq()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f1:'foo'}");
	}

	@Test
	public void l29_serializer_useWhitespace() throws Exception {
		L27 x = new L27();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.useWhitespace(true)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{\n\tf1: 'foo'\n}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.ws()
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{\n\tf1: 'foo'\n}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.useWhitespace(false)
			.build()
			.post("/echoBody", x)
			.run()
			.getBody().assertValue("{f1:'foo'}");

	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parser properties
	//-----------------------------------------------------------------------------------------------------------------


	@Test
	public void m03_parser_debugOutputLines() throws Exception {
		RestClient rc = null;

		rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.debugOutputLines(10)
			.build();

		assertEquals(10, rc.parsers.getParser("application/json").toMap().getMap("Parser").getInt("debugOutputLines").intValue());
	}

	public static class M4L extends ParserListener {
		public static Throwable T;
		public static String MSG;

		@Override
		public void onError(ParserSession session, Throwable t, String msg) {
			T = t;
			MSG = msg;
		}
	}

	public static class M5 {
		public int f;
	}

	@Test
	public void m05_parser_strict() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.json()
				.strict()
				.build()
				.post("/echoBody", new StringReader("{f:1}"))
				.run()
				.getBody().as(M5.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Unquoted attribute detected."));
		}

		try {
			MockRestClient
				.create(A.class)
				.json()
				.strict(true)
				.build()
				.post("/echoBody", new StringReader("{f:1}"))
				.run()
				.getBody().as(M5.class);
			fail("Exception expected");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Unquoted attribute detected."));
		}

		MockRestClient
			.create(A.class)
			.json()
			.strict(false)
			.build()
			.post("/echoBody", new StringReader("{f:1}"))
			.run()
			.getBody().as(M5.class);
	}

	public static class M7 {
		public String f;
	}

	@Test
	public void m07_parser_trimStringsOnRead() throws Exception {
		M7 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnRead()
			.build()
			.post("/echoBody", new StringReader("{f:' 1 '}"))
			.run()
			.getBody().as(M7.class);
		assertEquals("1", x.f);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnRead(true)
			.build()
			.post("/echoBody", new StringReader("{f:' 1 '}"))
			.run()
			.getBody().as(M7.class);
		assertEquals("1", x.f);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.trimStringsOnRead(false)
			.build()
			.post("/echoBody", new StringReader("{f:' 1 '}"))
			.run()
			.getBody().as(M7.class);
		assertEquals(" 1 ", x.f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OpenApi properties
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void n01_openApi_oapiFormat() throws Exception {
		MockRestClient
			.create(A.class)
			.oapiFormat(HttpPartFormat.UON)
			.build()
			.get("/checkQuery")
			.query("Foo", "bar baz")
			.run()
			.getBody().assertValue("Foo=%27bar+baz%27");
	}

	@Test
	public void n02_openApi_oapiCollectionFormat() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.oapiCollectionFormat(HttpPartCollectionFormat.PIPES)
			.build();

		rc.get("/checkQuery")
			.query("Foo", new String[]{"bar","baz"})
			.run()
			.getBody().assertValue("Foo=bar%7Cbaz");

		rc.post("/checkFormData")
			.formData("Foo", new String[]{"bar","baz"})
			.run()
			.getBody().assertValue("Foo=bar%7Cbaz");

		rc.get("/checkHeader")
			.header("Check", "Foo")
			.header("Foo", new String[]{"bar","baz"})
			.accept("text/json+simple")
			.run()
			.getBody().assertValue("['bar|baz']");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanContext properties
	//-----------------------------------------------------------------------------------------------------------------

	protected static class O1 {
		public int f = 1;

		@Override
		public String toString() {
			return "O1";
		}
	}

	@Test
	public void o001_beanContext_beanClassVisibility() throws Exception {
		RestClient rc1 = MockRestClient
			.create(A.class)
			.simpleJson()
			.build();

		RestClient rc2 = MockRestClient
			.create(A.class)
			.beanClassVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build();

		rc1.post("/echoBody", new O1())
			.run()
			.getBody().assertValue("'O1'");
		rc2.post("/echoBody", new O1())
			.run()
			.getBody().assertValue("{f:1}");

		rc1.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.getBody().assertValue("foo=O1");
		rc2.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.getBody().assertValue("foo=f%3D1");

		rc1.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.getBody().assertValue("foo=O1");
		rc2.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.getBody().assertValue("foo=f%3D1");

		rc1.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.getBody().assertValue("['O1']");
		rc2.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.getBody().assertValue("['f=1']");
	}

	public static class O2 {
		private int f;

		protected O2(int f) {
			this.f = f;
		}

		public int toInt() {
			return f;
		}
	}

	@Rest
	public static class O2R extends BasicRest {
		@RestMethod
		public Reader postTest(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws IOException {
			res.setHeader("X", req.getHeaders().getString("X"));
			return req.getBody().getReader();
		}
	}

	@Test
	public void o002_beanContext_beanConstructorVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanConstructorVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O2(1))
			.header("X", new O2(1))
			.run()
			.getBody().cache().assertValue("1")
			.getHeader("X").assertValue("1")
		;
		assertEquals(1, rr.getBody().as(O2.class).f);
		assertEquals(1, rr.getHeader("X").as(O2.class).f);
	}


	public static class O9 {
		public int f1;
		protected int f2;

		O9 init() {
			f1 = 1;
			f2 = 2;
			return this;
		}

		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o009_beanContext_beanFieldVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanFieldVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O9().init())
			.header("X", new O9().init())
			.run()
			.getBody().cache().assertValue("{f1:1,f2:2}")
			.getHeader("X").assertValue("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O9.class).f2);
		assertEquals(2, rr.getHeader("X").as(O9.class).f2);
	}

	public static interface O10I {
		int getF3();
		void setF3(int f3);
	}

	public static class O10 implements O10I {
		public int f1, f2;
		private int f3;

		@Override
		public int getF3() {
			return f3;
		}

		@Override
		public void setF3(int f3) {
			this.f3 = f3;
		}

		O10 init() {
			f1 = 1;
			f2 = 2;
			f3 = 3;
			return this;
		}

		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	public static class O10Filter extends org.apache.juneau.transform.BeanFilterBuilder<O10> {
		public O10Filter() {
			bpi("f1");
		}
	}

	@Test
	public void o010_beanContext_beanFilters() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanFilters(O10Filter.class)
			.simpleJson()
			.build()
			.post("/test", new O10().init())
			.header("X", new O10().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
		assertEquals(0, rr.getBody().as(O10.class).f2);
		assertEquals(0, rr.getHeader("X").as(O10.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.beanFilters(new O10Filter())
			.simpleJson()
			.build()
			.post("/test", new O10().init())
			.header("X", new O10().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
		assertEquals(0, rr.getBody().as(O10.class).f2);
		assertEquals(0, rr.getHeader("X").as(O10.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.beanFilters(BeanFilter.create(O10.class).bpi("f1").build())
			.simpleJson()
			.build()
			.post("/test", new O10().init())
			.header("X", new O10().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
		assertEquals(0, rr.getBody().as(O10.class).f2);
		assertEquals(0, rr.getHeader("X").as(O10.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.beanFilters(BeanFilter.create(O10.class).bpi("f1"))
			.simpleJson()
			.build()
			.post("/test", new O10().init())
			.header("X", new O10().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
		assertEquals(0, rr.getBody().as(O10.class).f2);
		assertEquals(0, rr.getHeader("X").as(O10.class).f2);


		rr = MockRestClient
			.create(O2R.class)
			.beanFilters(O10I.class)
			.simpleJson()
			.build()
			.post("/test", new O10().init())
			.header("X", new O10().init())
			.run()
			.getBody().cache().assertValue("{f3:3}")
			.getHeader("X").assertValue("f3=3")
		;
		assertEquals(3, rr.getBody().as(O10.class).f3);
		assertEquals(3, rr.getHeader("X").as(O10.class).f3);
	}

	public static class O18  {
		private int f1, f2;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		protected int getF2() {
			return f2;
		}
		protected void setF2(int f2) {
			this.f2 = f2;
		}

		O18 init() {
			f1 = 1;
			f2 = 2;
			return this;
		}

		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o018_beanContext_beanMethodVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanMethodVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O18().init())
			.header("X", new O18().init())
			.run()
			.getBody().cache().assertValue("{f1:1,f2:2}")
			.getHeader("X").assertValue("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O18.class).f2);
		assertEquals(2, rr.getHeader("X").as(O18.class).f2);
	}

	public static class O21  {
		public String f1;

		public O21(String i) {
			f1 = i;
		}

		@Override
		public String toString() {
			return f1;
		}
	}

	@Test
	public void o021_beanContext_beansRequireDefaultConstructor() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("{f1:'1'}")
			.getHeader("X").assertValue("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireDefaultConstructor(false)
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("{f1:'1'}")
			.getHeader("X").assertValue("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireDefaultConstructor()
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("'1'")
			.getHeader("X").assertValue("1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireDefaultConstructor(true)
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("'1'")
			.getHeader("X").assertValue("1")
		;
	}

	@Test
	public void o022_beanContext_beansRequireSerializable() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("{f1:'1'}")
			.getHeader("X").assertValue("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSerializable(false)
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("{f1:'1'}")
			.getHeader("X").assertValue("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSerializable()
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("'1'")
			.getHeader("X").assertValue("1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSerializable(true)
			.build()
			.post("/test", new O21("1"))
			.header("X", new O21("1"))
			.run()
			.getBody().cache().assertValue("'1'")
			.getHeader("X").assertValue("1")
		;
	}

	public static class O25  {
		private int f1, f2;

		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF2() {
			return f2;
		}

		O25 init() {
			f1 = 1;
			f2 = 2;
			return this;
		}

		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o025_beanContext_beansRequireSettersForGetters() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O25().init())
			.header("X", new O25().init())
			.run()
			.getBody().cache().assertValue("{f1:1,f2:2}")
			.getHeader("X").assertValue("f1=1,f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSettersForGetters(false)
			.build()
			.post("/test", new O25().init())
			.header("X", new O25().init())
			.run()
			.getBody().cache().assertValue("{f1:1,f2:2}")
			.getHeader("X").assertValue("f1=1,f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSettersForGetters()
			.build()
			.post("/test", new O25().init())
			.header("X", new O25().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSettersForGetters(true)
			.build()
			.post("/test", new O25().init())
			.header("X", new O25().init())
			.run()
			.getBody().cache().assertValue("{f1:1}")
			.getHeader("X").assertValue("f1=1")
		;
	}

	@Test
	public void o028_beanContext_bpiMap() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpi(OMap.of("O25", "f2"))
			.build()
			.post("/test", new O25().init())
			.header("X", new O25().init())
			.run()
			.getBody().cache().assertValue("{f2:2}")
			.getHeader("X").assertValue("f2=2")
		;
	}
//	public RestClientBuilder bpi(Map<String,String> values) {

//	@Test
//	public void o029_beanContext_bpiClassString() throws Exception { fail(); }
////	public RestClientBuilder bpi(Class<?> beanClass, String properties) {
//
//	@Test
//	public void o030_beanContext_bpiStringString() throws Exception { fail(); }
////	public RestClientBuilder bpi(String beanClassName, String properties) {
//
//	@Test
//	public void o031_beanContext_bproMap() throws Exception { fail(); }
////	public RestClientBuilder bpro(Map<String,String> values) {
//
//	@Test
//	public void o032_beanContext_bproClassString() throws Exception { fail(); }
////	public RestClientBuilder bpro(Class<?> beanClass, String properties) {
//
//	@Test
//	public void o033_beanContext_bproStringString() throws Exception { fail(); }
////	public RestClientBuilder bpro(String beanClassName, String properties) {
//
//	@Test
//	public void o034_beanContext_bpwoMap() throws Exception { fail(); }
////	public RestClientBuilder bpwo(Map<String,String> values) {
//
//	@Test
//	public void o035_beanContext_bpwoClassString() throws Exception { fail(); }
////	public RestClientBuilder bpwo(Class<?> beanClass, String properties) {
//
//	@Test
//	public void o036_beanContext_bpwoStringString() throws Exception { fail(); }
////	public RestClientBuilder bpwo(String beanClassName, String properties) {
//
//	@Test
//	public void o037_beanContext_bpxMap() throws Exception { fail(); }
////	public RestClientBuilder bpx(Map<String,String> values) {
//
//	@Test
//	public void o038_beanContext_bpxClassString() throws Exception { fail(); }
////	public RestClientBuilder bpx(Class<?> beanClass, String properties) {
//
//	@Test
//	public void o039_beanContext_bpxStringString() throws Exception { fail(); }
////	public RestClientBuilder bpx(String beanClassName, String properties) {
//
//	@Test
//	public void o040_beanContext_debug() throws Exception { fail(); }
////	public RestClientBuilder debug() {
//
//	@Test
//	public void o041_beanContext_debugBoolean() throws Exception { fail(); }
////	public RestClientBuilder debug(boolean value) {
//
//	@Test
//	public void o042_beanContext_dictionaryClasses() throws Exception { fail(); }
////	public RestClientBuilder dictionary(java.lang.Class<?>...values) {
//
//	@Test
//	public void o043_beanContext_dictionaryObjects() throws Exception { fail(); }
////	public RestClientBuilder dictionary(Object...values) {
//
//	@Test
//	public void o044_beanContext_dictionaryRemoveClasses() throws Exception { fail(); }
////	public RestClientBuilder dictionaryRemove(java.lang.Class<?>...values) {
//
//	@Test
//	public void o045_beanContext_dictionaryRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder dictionaryRemove(Object...values) {
//
//	@Test
//	public void o046_beanContext_dictionaryReplaceClasses() throws Exception { fail(); }
////	public RestClientBuilder dictionaryReplace(java.lang.Class<?>...values) {
//
//	@Test
//	public void o047_beanContext_dictionaryReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder dictionaryReplace(Object...values) {
//
//	@Test
//	public void o048_beanContext_dontIgnorePropertiesWithoutSetters() throws Exception { fail(); }
////	public RestClientBuilder dontIgnorePropertiesWithoutSetters() {
//
//	@Test
//	public void o049_beanContext_dontIgnoreTransientFields() throws Exception { fail(); }
////	public RestClientBuilder dontIgnoreTransientFields() {
//
//	@Test
//	public void o050_beanContext_dontIgnoreUnknownNullBeanProperties() throws Exception { fail(); }
////	public RestClientBuilder dontIgnoreUnknownNullBeanProperties() {
//
//	@Test
//	public void o051_beanContext_dontUseInterfaceProxies() throws Exception { fail(); }
////	public RestClientBuilder dontUseInterfaceProxies() {
//
//	@Test
//	public void o052_beanContext_example() throws Exception { fail(); }
////	public <T> RestClientBuilder example(Class<T> pojoClass, T o) {
//
//	@Test
//	public void o053_beanContext_exampleJson() throws Exception { fail(); }
////	public <T> RestClientBuilder exampleJson(Class<T> pojoClass, String json) {
//
//	@Test
//	public void o054_beanContext_examples() throws Exception { fail(); }
////	public RestClientBuilder examples(String json) {
//
//	@Test
//	public void o058_beanContext_fluentSetters() throws Exception { fail(); }
////	public RestClientBuilder fluentSetters() {
//
//	@Test
//	public void o059_beanContext_fluentSettersBoolean() throws Exception { fail(); }
////	public RestClientBuilder fluentSetters(boolean value) {
//
//	@Test
//	public void o060_beanContext_ignoreInvocationExceptionsOnGetters() throws Exception { fail(); }
////	public RestClientBuilder ignoreInvocationExceptionsOnGetters() {
//
//	@Test
//	public void o061_beanContext_ignoreInvocationExceptionsOnGettersBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
//
//	@Test
//	public void o062_beanContext_ignoreInvocationExceptionsOnSetters() throws Exception { fail(); }
////	public RestClientBuilder ignoreInvocationExceptionsOnSetters() {
//
//	@Test
//	public void o063_beanContext_ignoreInvocationExceptionsOnSettersBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
//
//	@Test
//	public void o064_beanContext_ignorePropertiesWithoutSettersBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignorePropertiesWithoutSetters(boolean value) {
//
//	@Test
//	public void o065_beanContext_ignoreTransientFields() throws Exception { fail(); }
////	public RestClientBuilder ignoreTransientFields(boolean value) {
//
//	@Test
//	public void o066_beanContext_ignoreUnknownBeanProperties() throws Exception { fail(); }
////	public RestClientBuilder ignoreUnknownBeanProperties() {
//
//	@Test
//	public void o067_beanContext_ignoreUnknownBeanPropertiesBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignoreUnknownBeanProperties(boolean value) {
//
//	@Test
//	public void o068_beanContext_ignoreUnknownNullBeanPropertiesBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignoreUnknownNullBeanProperties(boolean value) {
//
//	@Test
//	public void o069_beanContext_implClass() throws Exception { fail(); }
////	public RestClientBuilder implClass(Class<?> interfaceClass, Class<?> implClass) {
//
//	@Test
//	public void o070_beanContext_implClasses() throws Exception { fail(); }
////	public RestClientBuilder implClasses(Map<String,Class<?>> values) {
//
//	@Test
//	public void o074_beanContext_locale() throws Exception { fail(); }
////	public RestClientBuilder locale(Locale value) {
//
//	@Test
//	public void o075_beanContext_mediaType() throws Exception { fail(); }
////	public RestClientBuilder mediaType(MediaType value) {
//
//	@Test
//	public void o076_beanContext_notBeanClassesClasses() throws Exception { fail(); }
////	public RestClientBuilder notBeanClasses(java.lang.Class<?>...values) {
//
//	@Test
//	public void o077_beanContext_notBeanClassesObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanClasses(Object...values) {
//
//	@Test
//	public void o078_beanContext_notBeanClassesRemoveClasses() throws Exception { fail(); }
////	public RestClientBuilder notBeanClassesRemove(java.lang.Class<?>...values) {
//
//	@Test
//	public void o079_beanContext_notBeanClassesRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanClassesRemove(Object...values) {
//
//	@Test
//	public void o080_beanContext_notBeanClassesReplaceClasses() throws Exception { fail(); }
////	public RestClientBuilder notBeanClassesReplace(java.lang.Class<?>...values) {
//
//	@Test
//	public void o081_beanContext_notBeanClassesReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanClassesReplace(Object...values) {
//
//	@Test
//	public void o082_beanContext_notBeanPackagesObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackages(Object...values) {
//
//	@Test
//	public void o083_beanContext_notBeanPackagesStrings() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackages(String...values) {
//
//	@Test
//	public void o084_beanContext_notBeanPackagesRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackagesRemove(Object...values) {
//
//	@Test
//	public void o085_beanContext_notBeanPackagesRemoveStrings() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackagesRemove(String...values) {
//
//	@Test
//	public void o086_beanContext_notBeanPackagesReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackagesReplace(Object...values) {
//
//	@Test
//	public void o087_beanContext_notBeanPackagesReplaceStrings() throws Exception { fail(); }
////	public RestClientBuilder notBeanPackagesReplace(String...values) {
//
//	@Test
//	public void o088_beanContext_pojoSwapsClasses() throws Exception { fail(); }
////	public RestClientBuilder pojoSwaps(java.lang.Class<?>...values) {
//
//	@Test
//	public void o089_beanContext_pojoSwapsObjects() throws Exception { fail(); }
////	public RestClientBuilder pojoSwaps(Object...values) {
//
//	@Test
//	public void o090_beanContext_pojoSwapsRemoveClasses() throws Exception { fail(); }
////	public RestClientBuilder pojoSwapsRemove(java.lang.Class<?>...values) {
//
//	@Test
//	public void o091_beanContext_pojoSwapsRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder pojoSwapsRemove(Object...values) {
//
//	@Test
//	public void o092_beanContext_pojoSwapsReplaceClasses() throws Exception { fail(); }
////	public RestClientBuilder pojoSwapsReplace(java.lang.Class<?>...values) {
//
//	@Test
//	public void o093_beanContextpojoSwapsReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder pojoSwapsReplace(Object...values) {
//
//	@Test
//	public void o094_beanContext_propertyNamer() throws Exception { fail(); }
////	public RestClientBuilder propertyNamer(Class<? extends org.apache.juneau.PropertyNamer> value) {
//
//	@Test
//	public void o095_beanContext_sortProperties() throws Exception { fail(); }
////	public RestClientBuilder sortProperties() {
//
//	@Test
//	public void o096_beanContext_sortPropertiesBoolean() throws Exception { fail(); }
////	public RestClientBuilder sortProperties(boolean value) {
//
//	@Test
//	public void o097_beanContext_timeZone() throws Exception { fail(); }
////	public RestClientBuilder timeZone(TimeZone value) {
//
//	@Test
//	public void o098_beanContext_useEnumNames() throws Exception { fail(); }
////	public RestClientBuilder useEnumNames() {
//
//	@Test
//	public void o099_beanContext_useEnumNamesBoolean() throws Exception { fail(); }
////	public RestClientBuilder useEnumNames(boolean value) {
//
//	@Test
//	public void o100_beanContext_useInterfaceProxies() throws Exception { fail(); }
////	public RestClientBuilder useInterfaceProxies(boolean value) {
//
//	@Test
//	public void o101_beanContext_useJavaBeanIntrospector() throws Exception { fail(); }
////	public RestClientBuilder useJavaBeanIntrospector() {
//
//	@Test
//	public void o102_beanContext_useJavaBeanIntrospectorBoolean() throws Exception { fail(); }
////	public RestClientBuilder useJavaBeanIntrospector(boolean value) {


//	//-----------------------------------------------------------------------------------------------------------------
//	// Context properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void p01_context_addMap() throws Exception { fail(); }
////	public RestClientBuilder add(Map<String,Object> properties) {
//
//	@Test
//	public void p02_context_addToStringObject() throws Exception { fail(); }
////	public RestClientBuilder addTo(String name, Object value) {
//
//	@Test
//	public void p03_context_appendToStringObject() throws Exception { fail(); }
////	public RestClientBuilder appendTo(String name, Object value) {
//
//	@Test
//	public void p04_context_prependToStringObject() throws Exception { fail(); }
////	public RestClientBuilder prependTo(String name, Object value) {
//
//	@Test
//	public void p05_context_addToStringStringObject() throws Exception { fail(); }
////	public RestClientBuilder addTo(String name, String key, Object value) {
//
//	@Test
//	public void p06_context_apply() throws Exception { fail(); }
////	public RestClientBuilder apply(PropertyStore copyFrom) {
//
//	@Test
//	public void p07_context_applyAnnotationsClasses() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
//
//	@Test
//	public void p08_context_applyAnnotationsMethods() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(Method...fromMethods) {
//
//	@Test
//	public void p09_context_applyAnnotationsAnnotationList() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
//
//	@Test
//	public void p10_context_removeFrom() throws Exception { fail(); }
////	public RestClientBuilder removeFrom(String name, Object value) {
//
//	@Test
//	public void p11_context_setMap() throws Exception { fail(); }
////	public RestClientBuilder set(Map<String,Object> properties) {
//
//	@Test
//	public void p12_context_setStringObject() throws Exception { fail(); }
////	public RestClientBuilder set(String name, Object value) {
//
//	@Test
//	public void p13_context_annotations() throws Exception { fail(); }
////	public RestClientBuilder annotations(Annotation...values) {
//
}
