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
import static org.junit.runners.MethodSorters.*;
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

@FixMethodOrder(NAME_ASCENDING)
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
			.assertBody().contains("A1: 1", "A2: 2")
			.assertHeader("B1").is("1")
			.assertHeader("B2").is("2")
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
			.assertBody().contains("A1: 1")
			.assertHeader("B1").is("1");
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
			.assertBody().contains("HTTP GET /echo");
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
			.assertBody().contains("HTTP GET /echo","Foo: bar");
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
			.assertBody().contains("HTTP GET /echo");
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
			.assertBody().contains("OK");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['f=1','f=1']");
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
			.assertBody().is("null");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['bar','baz']");
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
			.assertBody().is("['text/foo','text/plain']");
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
			.assertBody().is("['UTF-8']");
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
			.assertBody().is("['identity']");
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
			.assertBody().is("['en']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['none']");
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
			.assertBody().is("['1']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['123']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['123']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['10']");
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
			.assertBody().is("['true']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['text/foo','text/plain']");
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
			.assertBody().is("['UTF-8']");
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
			.assertBody().is("['identity']");
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
			.assertBody().is("['en']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['none']");
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
			.assertBody().is("['1']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['123']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['\"foo\"']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['\"foo\"']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
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
			.assertBody().is("['10']");
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
			.assertBody().is("['true']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['foo']");
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
			.assertBody().is("['bar','baz','qux','q1x','q2x']");
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
			.assertBody().is("['bar','baz','qux']");
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
			.assertBody().is("['text/plain']");
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
			.assertBody().is("['text/foo','text/plain']");
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
			.assertBody().is("['text/plain']");
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
			.assertBody().is("['text/plain']");
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
			.assertBody().is("['text/foo','text/plain']");
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
			.assertBody().is("['x{f:1}','x{f:1}']");
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
			.assertBody().is("Foo=bar&Foo=baz");
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
			.assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
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
			.assertBody().is("Foo=bar&Foo=baz");
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
			.assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
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
			.assertBody().is("['f1','f2','baz']");
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
			.assertStatusCode().is(200)
			.assertBody().contains("HTTP GET /echo");
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
			.assertBody().contains("OK");
	}

	@Test
	public void k06_restClient_keepHttpClientOpen() throws Exception {
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
			.assertBody().contains("OK");
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
			.assertBody().is("['f1','f2','f3']")
			.assertHeader("Bar").is("b1");
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
			.assertBody().is("['f1','f2','f3']")
			.assertHeader("Bar").is("b1");
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
			.leakDetection()
			.build(K09RestClient.class)
			.finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.", K09RestClient.lastMessage);
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
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
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
			.assertBody().is("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.cacheBody()
			.assertBody().is("{\"f\":1}")
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
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
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
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
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
			.assertBody().is("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.cacheBody()
			.assertBody().is("{\"f\":1}")
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
			.assertBody().is("{f:1}");

		Bean b = rc
			.post("/echoBody", bean)
			.accept("text/xml")
			.contentType("text/xml")
			.run()
			.cacheBody()
			.assertBody().is("<object><f>1</f></object>")
			.getBody().as(Bean.class);
		assertEqualObjects(b, bean);

		b = rc
			.post("/echoBody", bean)
			.accept("text/json")
			.contentType("text/json")
			.run()
			.cacheBody()
			.assertBody().is("{\"f\":1}")
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
			.assertHeader("Foo").is("x{f:1}")
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
			.assertHeader("Foo").is("x{f:1}")
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
			.addBeanTypes()
			.build()
			.post("/echoBody", l1)
			.run()
			.assertBody().is("{f1:{_type:'L',f2:1}}");
	}

	@Test
	public void l03_serializer_addRootType() throws Exception {
		L2 l2 = new L2().init();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.addBeanTypes()
			.addRootType()
			.build()
			.post("/echoBody", l2)
			.run()
			.assertBody().is("{_type:'L',f2:1}");
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
			assertTrue(e.getCause(SerializeException.class).getMessage().startsWith("Recursion occurred"));
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
			.assertBody().is("{}");
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
			.assertBody().is("\t\t{\n\t\t\tf: 1\n\t\t}");
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
			.assertBody().is("{}");
	}

	@Test
	public void l12_serializer_sortCollections() throws Exception {
		String[] x = new String[]{"c","a","b"};

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortCollections()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("['a','b','c']");
	}

	@Test
	public void l14_serializer_sortMapsBoolean() throws Exception {
		AMap<String,Integer> x = AMap.of("c", 3, "a", 1, "b", 2);

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sortMaps()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{a:1,b:2,c:3}");
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
			.trimEmptyCollections()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{}");
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
			.trimEmptyMaps()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{}");
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
			.assertBody().is("{f:null}");
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
			.assertBody().is("{f:'foo'}");
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
			.assertBody().is("{f:'http://localhost:80/context/resource/foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.uriResolution(UriResolution.NONE)
			.uriRelativity(UriRelativity.RESOURCE)
			.uriContext(new UriContext("http://localhost:80", "/context", "/resource", "/path"))
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{f:'foo'}");
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
			.assertBody().is("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}");
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
			.assertBody().is("{'f1':'foo'}");

		MockRestClient
			.create(A.class)
			.json()
			.quoteChar('|')
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{|f1|:|foo|}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.quoteChar('|')
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{f1:|foo|}");
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
			.assertBody().is("{'f1':'foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.sq()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{f1:'foo'}");
	}

	@Test
	public void l29_serializer_useWhitespace() throws Exception {
		L27 x = new L27();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.ws()
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{\n\tf1: 'foo'\n}");
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
			.assertBody().is("Foo=%27bar+baz%27");
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
			.assertBody().is("Foo=bar%7Cbaz");

		rc.post("/checkFormData")
			.formData("Foo", new String[]{"bar","baz"})
			.run()
			.assertBody().is("Foo=bar%7Cbaz");

		rc.get("/checkHeader")
			.header("Check", "Foo")
			.header("Foo", new String[]{"bar","baz"})
			.accept("text/json+simple")
			.run()
			.assertBody().is("['bar|baz']");
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
	public void o01_beanContext_beanClassVisibility() throws Exception {
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
			.assertBody().is("'O1'");
		rc2.post("/echoBody", new O1())
			.run()
			.assertBody().is("{f:1}");

		rc1.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.assertBody().is("foo=O1");
		rc2.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.assertBody().is("foo=f%3D1");

		rc1.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.assertBody().is("foo=O1");
		rc2.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.assertBody().is("foo=f%3D1");

		rc1.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.assertBody().is("['O1']");
		rc2.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.assertBody().is("['f=1']");
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
	public void o02_beanContext_beanConstructorVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanConstructorVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O2(1))
			.header("X", new O2(1))
			.run()
			.cacheBody()
			.assertBody().is("1")
			.assertHeader("X").is("1")
		;
		assertEquals(1, rr.getBody().as(O2.class).f);
		assertEquals(1, rr.getHeader("X").as(O2.class).f);
	}

	public static class O3 {
		public int f1;
		protected int f2;

		O3 init() {
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
	public void o03_beanContext_beanFieldVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanFieldVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O3().init())
			.header("X", new O3().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O3.class).f2);
		assertEquals(2, rr.getHeader("X").as(O3.class).f2);
	}

	public static interface O4I {
		int getF3();
		void setF3(int f3);
	}

	public static class O4 implements O4I {
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

		O4 init() {
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

	@Test
	public void o04_beanContext_beanFilters() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.bpi(O4.class, "f1")
			.simpleJson()
			.build()
			.post("/test", new O4().init())
			.header("X", new O4().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.bpi(O4.class, "f1")
			.simpleJson()
			.build()
			.post("/test", new O4().init())
			.header("X", new O4().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.bpi(O4.class, "f1")
			.simpleJson()
			.build()
			.post("/test", new O4().init())
			.header("X", new O4().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.bpi(O4.class, "f1")
			.simpleJson()
			.build()
			.post("/test", new O4().init())
			.header("X", new O4().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4.class).f2);

		rr = MockRestClient
			.create(O2R.class)
			.interfaces(O4I.class)
			.simpleJson()
			.build()
			.post("/test", new O4().init())
			.header("X", new O4().init())
			.run()
			.cacheBody()
			.assertBody().is("{f3:3}")
			.assertHeader("X").is("f3=3")
		;
		assertEquals(3, rr.getBody().as(O4.class).f3);
		assertEquals(3, rr.getHeader("X").as(O4.class).f3);
	}

	public static class O5  {
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

		O5 init() {
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
	public void o05_beanContext_beanMethodVisibility() throws Exception {
		RestResponse rr = MockRestClient
			.create(O2R.class)
			.beanMethodVisibility(Visibility.PROTECTED)
			.simpleJson()
			.build()
			.post("/test", new O5().init())
			.header("X", new O5().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O5.class).f2);
		assertEquals(2, rr.getHeader("X").as(O5.class).f2);
	}

	public static class O6  {
		public String f1;

		public O6(String i) {
			f1 = i;
		}

		@Override
		public String toString() {
			return f1;
		}
	}

	@Test
	public void o06_beanContext_beansRequireDefaultConstructor() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O6("1"))
			.header("X", new O6("1"))
			.run()
			.assertBody().is("{f1:'1'}")
			.assertHeader("X").is("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireDefaultConstructor()
			.build()
			.post("/test", new O6("1"))
			.header("X", new O6("1"))
			.run()
			.assertBody().is("'1'")
			.assertHeader("X").is("1")
		;
	}

	@Test
	public void o07_beanContext_beansRequireSerializable() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O6("1"))
			.header("X", new O6("1"))
			.run()
			.assertBody().is("{f1:'1'}")
			.assertHeader("X").is("f1=1")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSerializable()
			.build()
			.post("/test", new O6("1"))
			.header("X", new O6("1"))
			.run()
			.assertBody().is("'1'")
			.assertHeader("X").is("1")
		;
	}

	public static class O8  {
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

		O8 init() {
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
	public void o08_beanContext_beansRequireSettersForGetters() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.beansRequireSettersForGetters()
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
	}

	@Test
	public void o09_beanContext_bpi() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpi(OMap.of("O8", "f2"))
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpi(O8.class, "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpi("O8", "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpi(O8.class.getName(), "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
	}

	@Test
	public void o10_beanContext_bpro() throws Exception {
		RestResponse rr = null;

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpro(OMap.of("O25", "f2"))
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpro(O8.class, "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpro("O25", "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1,f2:2}")
			.assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());
	}

	@Test
	public void o11_beanContext_bpwo() throws Exception {
		RestResponse rr = null;

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpwo(OMap.of("O8", "f2"))
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpwo(O8.class, "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());

		rr = MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpwo("O8", "f2")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.cacheBody()
			.assertBody().is("{f1:1}")
			.assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O8.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O8.class).toString());
	}

	@Test
	public void o12_beanContext_bpx() throws Exception {
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpx(OMap.of("O8", "f1"))
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpx(O8.class, "f1")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpx("O8", "f1")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
		MockRestClient
			.create(O2R.class)
			.simpleJson()
			.bpx(O8.class.getName(), "f1")
			.build()
			.post("/test", new O8().init())
			.header("X", new O8().init())
			.run()
			.assertBody().is("{f2:2}")
			.assertHeader("X").is("f2=2")
		;
	}

	public static class O13 {
		public Object f;
	}

	@Test
	public void o13_beanContext_debug() throws Exception {
		O13 x = new O13();
		x.f = x;
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.debug()
				.build()
				.post("/echo", x)
				.run()
				.assertBody().contains("HTTP GET /echo","Foo: bar");
			;
		} catch (RestCallException e) {
			assertTrue(e.getCause(SerializeException.class).getMessage().startsWith("Recursion occurred"));
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="foo")
	public static class O14a {
		public String foo;

		public O14a init() {
			foo = "1";
			return this;
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="bar")
	public static class O14b {
		public String foo;

		public O14b init() {
			foo = "2";
			return this;
		}
	}

	public static class O14c {
		public Object foo;

		public O14c init() {
			foo = new O14a().init();
			return this;
		}
	}

	@Test
	public void o14_beanContext_dictionary() throws Exception {
		Object o = MockRestClient
			.create(A.class)
			.simpleJson()
			.dictionary(O14a.class,O14b.class)
			.addRootType()
			.addBeanTypes()
			.build()
			.post("/echoBody", new O14a().init())
			.run()
			.cacheBody()
			.assertBody().contains("{_type:'foo',foo:'1'}")
			.getBody().as(Object.class);
		;
		assertTrue(o instanceof O14a);

		OMap m = OMap.of("x", new O14a().init(), "y", new O14b().init());
		m = MockRestClient
			.create(A.class)
			.simpleJson()
			.dictionary(O14a.class,O14b.class)
			.addRootType()
			.addBeanTypes()
			.build()
			.post("/echoBody", m)
			.run()
			.cacheBody()
			.assertBody().is("{x:{_type:'foo',foo:'1'},y:{_type:'bar',foo:'2'}}")
			.getBody().as(OMap.class);
		;
		assertTrue(m.get("x") instanceof O14a);
		assertTrue(m.get("y") instanceof O14b);

		O14c o33c = MockRestClient
			.create(A.class)
			.simpleJson()
			.dictionaryOn(O14c.class,O14a.class,O14b.class)
			.addRootType()
			.addBeanTypes()
			.build()
			.post("/echoBody", new O14c().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:{_type:'foo',foo:'1'}}")
			.getBody().as(O14c.class);
		;
		assertTrue(o33c.foo instanceof O14a);
	}

	public static class O15 {
		private String foo;
		public String getFoo() {
			return foo;
		}
		public O15 init() {
			foo = "foo";
			return this;
		}
	}

	@Test
	public void o15_beanContext_dontIgnorePropertiesWithoutSetters() throws Exception {
		O15 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/echoBody", new O15().init())
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'foo'}")
			.getBody().as(O15.class);
		;
		assertNull(x.foo);

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.dontIgnorePropertiesWithoutSetters()
				.build()
				.post("/echoBody", new O15().init())
				.run()
				.cacheBody()
				.assertBody().contains("{foo:'foo'}")
				.getBody().as(O15.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(BeanRuntimeException.class).getMessage().contains("Setter or public field not defined"));
		}
	}

	public static class O16 {
		public String foo;
		public transient String bar;

		public O16 init() {
			foo = "1";
			bar = "2";
			return this;
		}
	}

	@Test
	public void o16_beanContext_dontIgnoreTransientFields() throws Exception {
		O16 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/echoBody", new O16().init())
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1'}")
			.getBody().as(O16.class);
		;
		assertNull(x.bar);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.dontIgnoreTransientFields()
			.build()
			.post("/echoBody", new O16().init())
			.run()
			.cacheBody()
			.assertBody().contains("{bar:'2',foo:'1'}")
			.getBody().as(O16.class);
		assertEquals("2", x.bar);
	}

	public static class O17 {
		public String foo;
	}

	@Test
	public void o17_beanContext_dontIgnoreUnknownNullBeanProperties() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/echoBody", new StringReader("{foo:'1',bar:null}"))
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1',bar:null}")
			.getBody().as(O17.class);
		;

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.dontIgnoreUnknownNullBeanProperties()
				.build()
				.post("/echoBody", new StringReader("{foo:'1',bar:null}"))
				.run()
				.cacheBody()
				.assertBody().contains("{foo:'1',bar:null}")
				.getBody().as(O17.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("Unknown property 'bar'"));
		}
	}

	public static interface O18 {
		public String getFoo();
		public void setFoo(String foo);
	}

	@Test
	public void o18_beanContext_dontUseInterfaceProxies() throws Exception {
		O18 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/echoBody", new StringReader("{foo:'1'}"))
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1'}")
			.getBody().as(O18.class);
		;
		assertEquals("1", x.getFoo());

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.dontUseInterfaceProxies()
				.build()
				.post("/echoBody", new StringReader("{foo:'1'}"))
				.run()
				.cacheBody()
				.assertBody().contains("{foo:'1'}")
				.getBody().as(O18.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("could not be instantiated"));
		}
	}

	public static class O19 {
		private String foo;
		public String getFoo() {
			return foo;
		}
		public O19 foo(String foo) {
			this.foo = foo;
			return this;
		}
	}

	@Test
	public void o19_beanContext_fluentSetters() throws Exception {
		O19 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.fluentSetters()
			.build()
			.post("/echoBody", new StringReader("{foo:'1'}"))
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1'}")
			.getBody().as(O19.class);
		;
		assertEquals("1", x.getFoo());

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.fluentSetters(O19.class)
			.build()
			.post("/echoBody", new StringReader("{foo:'1'}"))
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1'}")
			.getBody().as(O19.class);
		;
		assertEquals("1", x.getFoo());
	}

	public static class O20 {
		@SuppressWarnings("unused")
		private String foo,bar;
		public String getFoo() {
			return foo;
		}
		public void setFoo(String foo) {
			this.foo = foo;
		}
		public String getBar() {
			throw new RuntimeException("xxx");
		}
		public void setBar(String bar) {
			this.bar = bar;
		}
		public O20 init() {
			this.foo = "1";
			this.bar = "2";
			return this;
		}
	}

	@Test
	public void o20_beanContext_ignoreInvocationExceptionsOnGetters() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post("/echoBody", new O20().init())
				.run()
			;
			fail("Exception expected.");
		} catch (RestCallException e) {
			assertTrue(e.getCause(SerializeException.class).getMessage().contains("Could not call getValue() on property 'bar'"));
		}

		O20 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.ignoreInvocationExceptionsOnGetters()
			.build()
			.post("/echoBody", new O20().init())
			.run()
			.cacheBody()
			.assertBody().contains("{foo:'1'}")
			.getBody().as(O20.class);
		;
		assertEquals("1", x.getFoo());
	}

	public static class O21 {
		@SuppressWarnings("unused")
		private String foo,bar;
		public String getFoo() {
			return foo;
		}
		public void setFoo(String foo) {
			this.foo = foo;
		}
		public String getBar() {
			return bar;
		}
		public void setBar(String bar) {
			throw new RuntimeException("xxx");
		}
		public O21 init() {
			this.foo = "1";
			this.bar = "2";
			return this;
		}
	}

	@Test
	public void o21_beanContext_ignoreInvocationExceptionsOnSetters() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post("/echoBody", new O21().init())
				.run()
				.getBody().as(O21.class)
			;
			fail("Exception expected.");
		} catch (RestCallException e) {
			assertTrue(e.getCause(BeanRuntimeException.class).getMessage().contains("Error occurred trying to set property 'bar'"));
		}

		O21 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.ignoreInvocationExceptionsOnSetters()
			.build()
			.post("/echoBody", new O21().init())
			.run()
			.cacheBody()
			.getBody().as(O21.class);
		;
		assertEquals("1", x.getFoo());
	}

	public static class O22 {
		public String foo;
	}

	@Test
	public void o22_beanContext_ignoreUnknownBeanProperties() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post("/echoBody", new StringReader("{foo:'1',bar:'2'}"))
				.run()
				.getBody().as(O22.class)
			;
			fail("Exception expected.");
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("Unknown property 'bar' encountered"));
		}

		O22 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.ignoreUnknownBeanProperties()
			.build()
			.post("/echoBody", new StringReader("{foo:'1',bar:'2'}"))
			.run()
			.cacheBody()
			.getBody().as(O22.class);
		;
		assertEquals("1", x.foo);
	}

	public static interface O23i {
		void setFoo(int foo);
		int getFoo();
	}

	public static class O23 implements O23i {
		private int foo;
		@Override
		public int getFoo() {
			return foo;
		}
		@Override
		public void setFoo(int foo) {
			this.foo = foo;
		}
	}

	@Test
	public void o23_beanContext_implClass() throws Exception {
		O23i x = MockRestClient
			.create(A.class)
			.simpleJson()
			.implClass(O23i.class, O23.class)
			.build()
			.post("/echoBody", new StringReader("{foo:1}"))
			.run()
			.getBody().as(O23i.class)
		;
		assertEquals(1, x.getFoo());
		assertTrue(x instanceof O23);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.implClasses(AMap.of(O23i.class, O23.class))
			.build()
			.post("/echoBody", new StringReader("{foo:1}"))
			.run()
			.getBody().as(O23i.class)
		;
		assertEquals(1, x.getFoo());
		assertTrue(x instanceof O23);
	}

	public static interface O24i {
		void setFoo(int foo);
		int getFoo();
	}

	public static class O24 implements O24i {
		private int foo,bar;
		@Override
		public int getFoo() { return foo; }
		@Override
		public void setFoo(int foo) { this.foo = foo; }
		public int getBar() { return bar; }
		public void setBar(int bar) { this.bar = bar; }

		public O24 init() {
			foo = 1;
			bar = 2;
			return this;
		}
	}

	@Test
	public void o24_beanContext_interfaceClass() throws Exception {
		O24i x = MockRestClient
			.create(A.class)
			.simpleJson()
			.interfaceClass(O24.class, O24i.class)
			.build()
			.post("/echoBody", new O24().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(O24.class)
		;
		assertEquals(1, x.getFoo());

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.interfaces(O24i.class)
			.build()
			.post("/echoBody", new O24().init())
			.run()
			.assertBody().is("{foo:1}")
			.getBody().as(O24.class)
		;
		assertEquals(1, x.getFoo());
	}

	public static class O25 {
		public int foo;
		public O25 init() {
			foo = 1;
			return this;
		}
	}

	public static class O25s extends StringSwap<O25> {
		@Override
		public String swap(BeanSession session, O25 o) throws Exception {
			assertEquals(Locale.UK, session.getLocale());
			return super.swap(session, o);
		}

		@Override
		public O25 unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
			assertEquals(Locale.UK, session.getLocale());
			return super.unswap(session, f, hint);
		}
	}

	@Test
	public void o25_beanContext_locale() throws Exception {
		O25 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.locale(Locale.UK)
			.build()
			.post("/echoBody", new O25().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(O25.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O26s extends StringSwap<O25> {
		@Override
		public String swap(BeanSession session, O25 o) throws Exception {
			assertEquals(MediaType.JSON, session.getMediaType());
			return super.swap(session, o);
		}

		@Override
		public O25 unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
			assertEquals(MediaType.JSON, session.getMediaType());
			return super.unswap(session, f, hint);
		}
	}

	@Test
	public void o26_beanContext_mediaType() throws Exception {
		O25 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.mediaType(MediaType.JSON)
			.build()
			.post("/echoBody", new O25().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(O25.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O27 {
		public int foo;
		public O27 init() {
			foo = 1;
			return this;
		}
		@Override
		public String toString() {
			return String.valueOf(foo);
		}
		public static O27 fromString(String foo) throws ParseException {
			O27 x = new O27();
			x.foo = JsonParser.DEFAULT.parse(foo, int.class);
			return x;
		}
	}

	@Test
	public void o27_beanContext_notBeanClasses() throws Exception {
		O27 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.notBeanClasses(O27.class)
			.build()
			.post("/echoBody", new O27().init())
			.run()
			.cacheBody()
			.assertBody().is("'1'")
			.getBody().as(O27.class)
		;
		assertEquals(1, x.foo);
	}

	@Test
	public void o28_beanContext_notBeanPackages() throws Exception {
		O27 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.notBeanPackages(O27.class.getPackage())
			.build()
			.post("/echoBody", new O27().init())
			.run()
			.cacheBody()
			.assertBody().is("'1'")
			.getBody().as(O27.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O29 {
		private String foo;
		public String getFoo() { return foo; }
		public void setFoo(String foo) { this.foo = foo; }
		public O29 init() {
			foo = "foo";
			return this;
		}
	}

	public static class O29Interceptor extends BeanInterceptor<O29> {
		static boolean getterCalled, setterCalled;
		@Override
		public Object readProperty(O29 bean, String name, Object value) {
			getterCalled = true;
			return "x" + value;
		}
		@Override
		public Object writeProperty(O29 bean, String name, Object value) {
			setterCalled = true;
			return value.toString().substring(1);
		}
	}

	@Test
	public void o29_beanContext_beanInterceptor() throws Exception {
		O29 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.beanInterceptor(O29.class, O29Interceptor.class)
			.build()
			.post("/echoBody", new O29().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:'xfoo'}")
			.getBody().as(O29.class)
		;
		assertEquals("foo", x.foo);
		assertTrue(O29Interceptor.getterCalled);
		assertTrue(O29Interceptor.setterCalled);
	}

	public static class O30 {
		private String fooBar;
		public String getFooBar() { return fooBar; }
		public void setFooBar(String fooBar) { this.fooBar = fooBar; }
		public O30 init() {
			fooBar = "fooBar";
			return this;
		}
	}

	@Test
	public void o30_beanContext_propertyNamer() throws Exception {
		O30 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.propertyNamer(PropertyNamerDLC.class)
			.build()
			.post("/echoBody", new O30().init())
			.run()
			.cacheBody()
			.assertBody().is("{'foo-bar':'fooBar'}")
			.getBody().as(O30.class)
		;
		assertEquals("fooBar", x.fooBar);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.propertyNamer(O30.class, PropertyNamerDLC.class)
			.build()
			.post("/echoBody", new O30().init())
			.run()
			.cacheBody()
			.assertBody().is("{'foo-bar':'fooBar'}")
			.getBody().as(O30.class)
		;
		assertEquals("fooBar", x.fooBar);
	}

	public static class O31 {
		public int foo, bar, baz;
		public O31 init() {
			foo = 1;
			bar = 2;
			baz = 3;
			return this;
		}
	}

	@Test
	public void o31_beanContext_sortProperties() throws Exception {
		O31 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.sortProperties()
			.build()
			.post("/echoBody", new O31().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(O31.class)
		;
		assertEquals(1, x.foo);

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.sortProperties(O31.class)
			.build()
			.post("/echoBody", new O31().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(O31.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O32a {
		public int foo;
	}

	public static class O32b extends O32a {
		public int bar;
		public O32b init() {
			foo = 1;
			bar = 2;
			return this;
		}
	}

	@Test
	public void o32_beanContext_stopClass() throws Exception {
		O32b x = MockRestClient
			.create(A.class)
			.simpleJson()
			.stopClass(O32b.class, O32a.class)
			.build()
			.post("/echoBody", new O32b().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2}")
			.getBody().as(O32b.class)
		;
		assertEquals(0, x.foo);
		assertEquals(2, x.bar);
	}

	public static class O33 {
		public int foo;
		public O33 init() {
			this.foo = 1;
			return this;
		}
	}

	public static class O33s extends PojoSwap<O33,Integer> {
		@Override
		public Integer swap(BeanSession session, O33 o) { return o.foo; }
		@Override
		public O33 unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return new O33().init(); }
	}

	@Test
	public void o33_beanContext_swaps() throws Exception {
		O33 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.swaps(O33s.class)
			.build()
			.post("/echoBody", new O33().init())
			.run()
			.cacheBody()
			.assertBody().is("1")
			.getBody().as(O33.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O34 {
		public int foo;
		public O34 init() {
			foo = 1;
			return this;
		}
	}

	public static class O34s extends StringSwap<O34> {
		@Override
		public String swap(BeanSession session, O34 o) throws Exception {
			assertEquals(TimeZone.getTimeZone("Z"), session.getTimeZone());
			return super.swap(session, o);
		}

		@Override
		public O34 unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
			assertEquals(TimeZone.getTimeZone("Z"), session.getTimeZone());
			return super.unswap(session, f, hint);
		}
	}

	@Test
	public void o34_beanContext_timeZone() throws Exception {
		O34 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.timeZone(TimeZone.getTimeZone("Z"))
			.build()
			.post("/echoBody", new O34().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(O34.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O35 {
		public int foo;
		public O35 init() {
			this.foo = 1;
			return this;
		}
	}

	@Test
	public void o35_beanContext_typeName() throws Exception {
		O35 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.typeName(O35.class, "foo")
			.addRootType()
			.build()
			.post("/echoBody", new O35().init())
			.run()
			.cacheBody()
			.assertBody().is("{_type:'foo',foo:1}")
			.getBody().as(O35.class)
		;
		assertEquals(1, x.foo);
	}

	@Test
	public void o36_beanContext_typePropertyName() throws Exception {
		O35 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.typeName(O35.class, "foo")
			.typePropertyName("X")
			.addRootType()
			.build()
			.post("/echoBody", new O35().init())
			.run()
			.cacheBody()
			.assertBody().is("{X:'foo',foo:1}")
			.getBody().as(O35.class)
		;
		assertEquals(1, x.foo);
	}

	public static enum O37e {
		ONE(1),TWO(2);

		private int value;

		O37e(int value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	public static class O37 {
		public O37e foo;
		public O37 init() {
			this.foo = O37e.ONE;
			return this;
		}
	}

	@Test
	public void o37_beanContext_useEnumNames() throws Exception {
		O37 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.useEnumNames()
			.build()
			.post("/echoBody", new O37().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:'ONE'}")
			.getBody().as(O37.class)
		;
		assertEquals(O37e.ONE, x.foo);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class P1 {
		public String foo;
	}

	@Test
	public void p01_context_addMap() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.add(OMap.of(Serializer.SERIALIZER_keepNullProperties, true))
			.build()
			.post("/echoBody", new P1())
			.run()
			.cacheBody()
			.assertBody().is("{foo:null}")
			.getBody().as(P1.class);
	}
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

}
