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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.ext.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
		@RestMethod(path="/bean")
		public Bean postBean(@Body Bean b) {
			return b;
		}
		@RestMethod(path="/echo")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestMethod(path="/checkHeader")
		public String[] getHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
		@RestMethod(path="/checkQuery")
		public String[] getQuery(org.apache.juneau.rest.RestRequest req) {
			return req.getQuery().get(req.getHeader("Check"));
		}
		@RestMethod(path="/checkFormData")
		public String[] getFormData(org.apache.juneau.rest.RestRequest req) {
			return req.getFormData().get(req.getHeader("Check"));
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
		RestClient rc = MockRestClient.create(E.class).simpleJson()
			.basicAuth(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "user", "pw")
			.build();
		rc.get("/echo")
			.run()
			.getBody().assertContains("OK")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_basicHeader() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header("Foo","bar")
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f02_beanHeader() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header("Foo",bean)
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['(f=1)']");
	}

	@Test
	public void f03_nullHeaders() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header("Foo",null)
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("null");
	}

	@Test
	public void f04_header_Header() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new org.apache.http.message.BasicHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f05_header_NameValuePair() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new SimpleNameValuePair("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f06_header_HttpHeader() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new BasicStringHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f07_headers_Header() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(new org.apache.http.message.BasicHeader("Foo", "bar"),new org.apache.http.message.BasicHeader("Baz", "qux"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f08_headers_OMap() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(OMap.of("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f09_headers_Map() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(AMap.of("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f10_headers_NameValuePairs() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(NameValuePairs.of("Foo","bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f11_headers_NameValuePair() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(new SimpleNameValuePair("Foo","bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f12_headers_pairs() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headerPairs("Foo", "bar")
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f13_headers_HttpHeader() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.headers(new BasicStringHeader("Foo", "bar"))
			.header("Check", "Foo")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar']");
	}

	@Test
	public void f14_headers_accept() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.accept("text/plain")
			.header("Check", "Accept")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['text/plain']");
	}

	@Test
	public void f15_headers_acceptCharset() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.acceptCharset("UTF-8")
			.header("Check", "Accept-Charset")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['UTF-8']");
	}

	@Test
	public void f16_headers_acceptEncoding() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.acceptEncoding("identity")
			.header("Check", "Accept-Encoding")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['identity']");
	}

	@Test
	public void f17_headers_acceptLanguage() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.acceptLanguage("en")
			.header("Check", "Accept-Language")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['en']");
	}

	@Test
	public void f18_headers_authorization() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.authorization("foo")
			.header("Check", "Authorization")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f19_headers_cacheControl() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.cacheControl("none")
			.header("Check", "Cache-Control")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['none']");
	}

	@Test
	public void f20_headers_clientVersion() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.clientVersion("1")
			.header("Check", "X-Client-Version")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['1']");
	}

	@Test
	public void f21_headers_connection() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.connection("foo")
			.header("Check", "Connection")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f22_headers_contentLength() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.contentLength("123")
			.header("Check", "Content-Length")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['123']");
	}

	@Test
	public void f23_headers_contentType() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.contentType("foo")
			.header("Check", "Content-Type")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f24_headers_date() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.date("123")
			.header("Check", "Date")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['123']");
	}

	@Test
	public void f25_headers_expect() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.expect("foo")
			.header("Check", "Expect")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f26_headers_forwarded() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.forwarded("foo")
			.header("Check", "Forwarded")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f27_headers_from() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.from("foo")
			.header("Check", "From")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f28_headers_host() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.host("foo")
			.header("Check", "Host")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f29_headers_ifMatch() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.ifMatch("foo")
			.header("Check", "If-Match")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f30_headers_ifModifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.ifModifiedSince("foo")
			.header("Check", "If-Modified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f31_headers_ifNoneMatch() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.ifNoneMatch("foo")
			.header("Check", "If-None-Match")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f32_headers_ifRange() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.ifRange("foo")
			.header("Check", "If-Range")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f33_headers_ifUnmodifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.ifUnmodifiedSince("foo")
			.header("Check", "If-Unmodified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f34_headers_maxForwards() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.maxForwards("10")
			.header("Check", "Max-Forwards")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['10']");
	}

	@Test
	public void f35_headers_noTrace() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.noTrace()
			.header("Check", "No-Trace")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['true']");
	}

	@Test
	public void f36_headers_origin() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.origin("foo")
			.header("Check", "Origin")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f37_headers_pragma() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.pragma("foo")
			.header("Check", "Pragma")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f38_headers_proxyAuthorization() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.proxyAuthorization("foo")
			.header("Check", "Proxy-Authorization")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f39_headers_range() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.range("foo")
			.header("Check", "Range")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f40_headers_referer() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.referer("foo")
			.header("Check", "Referer")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f41_headers_te() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.te("foo")
			.header("Check", "TE")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f42_headers_userAgent() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.userAgent("foo")
			.header("Check", "User-Agent")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f43_headers_upgrade() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.upgrade("foo")
			.header("Check", "Upgrade")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f44_headers_via() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.via("foo")
			.header("Check", "Via")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void f45_headers_warning() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.warning("foo")
			.header("Check", "Warning")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Header Beans
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_headers_accept() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Accept("text/plain"))
			.header("Check", "Accept")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['text/plain']");
	}

	@Test
	public void g02_headers_acceptCharset() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new AcceptCharset("UTF-8"))
			.header("Check", "Accept-Charset")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['UTF-8']");
	}

	@Test
	public void g03_headers_acceptEncoding() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new AcceptEncoding("identity"))
			.header("Check", "Accept-Encoding")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['identity']");
	}

	@Test
	public void g04_headers_acceptLanguage() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new AcceptLanguage("en"))
			.header("Check", "Accept-Language")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['en']");
	}

	@Test
	public void g05_headers_authorization() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Authorization("foo"))
			.header("Check", "Authorization")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g06_headers_cacheControl() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new CacheControl("none"))
			.header("Check", "Cache-Control")
			.header("X-Expect", "none")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['none']");
	}

	@Test
	public void g07_headers_clientVersion() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new ClientVersion("1"))
			.header("Check", "X-Client-Version")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['1']");
	}

	@Test
	public void g08_headers_connection() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Connection("foo"))
			.header("Check", "Connection")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g09_headers_contentLength() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new ContentLength(123))
			.header("Check", "Content-Length")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['123']");
	}

	@Test
	public void g10_headers_contentType() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new ContentType("foo"))
			.header("Check", "Content-Type")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g11a_headers_date() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new org.apache.juneau.http.Date("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "Date")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g11b_headers_date() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new org.apache.juneau.http.Date(CALENDAR))
			.header("Check", "Date")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g12_headers_expect() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Expect("foo"))
			.header("Check", "Expect")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g13_headers_forwarded() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Forwarded("foo"))
			.header("Check", "Forwarded")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g14_headers_from() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new From("foo"))
			.header("Check", "From")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g15_headers_host() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Host("foo"))
			.header("Check", "Host")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g16_headers_ifMatch() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfMatch("foo"))
			.header("Check", "If-Match")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['\"foo\"']");
	}

	@Test
	public void g17a_headers_ifModifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfModifiedSince(CALENDAR))
			.header("Check", "If-Modified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g17b_headers_ifModifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfModifiedSince("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "If-Modified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g18_headers_ifNoneMatch() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfNoneMatch("foo"))
			.header("Check", "If-None-Match")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['\"foo\"']");
	}

	@Test
	public void g19_headers_ifRange() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfRange("foo"))
			.header("Check", "If-Range")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g20a_headers_ifUnmodifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfUnmodifiedSince(CALENDAR))
			.header("Check", "If-Unmodified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g20b_headers_ifUnmodifiedSince() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new IfUnmodifiedSince("Sun, 31 Dec 2000 12:34:56 GMT"))
			.header("Check", "If-Unmodified-Since")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['Sun, 31 Dec 2000 12:34:56 GMT']");
	}

	@Test
	public void g21_headers_maxForwards() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new MaxForwards(10))
			.header("Check", "Max-Forwards")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['10']");
	}

	@Test
	public void g22_headers_noTrace() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new NoTrace("true"))
			.header("Check", "No-Trace")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['true']");
	}

	@Test
	public void g23_headers_origin() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Origin("foo"))
			.header("Check", "Origin")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g24_headers_pragma() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Pragma("foo"))
			.header("Check", "Pragma")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g25_headers_proxyAuthorization() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new ProxyAuthorization("foo"))
			.header("Check", "Proxy-Authorization")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g26_headers_range() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Range("foo"))
			.header("Check", "Range")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g27_headers_referer() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Referer("foo"))
			.header("Check", "Referer")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g28_headers_te() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new TE("foo"))
			.header("Check", "TE")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g29_headers_userAgent() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new UserAgent("foo"))
			.header("Check", "User-Agent")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g30_headers_upgrade() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Upgrade("foo"))
			.header("Check", "Upgrade")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g31_headers_via() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Via("foo"))
			.header("Check", "Via")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	@Test
	public void g32_headers_warning() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson()
			.header(new Warning("foo"))
			.header("Check", "Warning")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['foo']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other Header tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_multipleHeaders() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.debug()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Foo")
			.headerPairs("Foo","bar","Foo","baz")
			.header("Foo","qux")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['bar','baz','qux']");
	}

	@Test
	public void h02_multipleHeaders_withRequest() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Foo")
			.headerPairs("Foo","bar","Foo","baz")
			.build();
		rc.get("/checkHeader").header("Foo","qux").run().getBody().assertValue("['bar','baz','qux']");
	}

	@Test
	public void h03_dontOverrideAccept() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Accept")
			.header("Accept", "text/plain")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['text/plain']");
	}

	@Test
	public void h04_dontOverrideAccept_withRequest() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Accept")
			.header("Accept", "text/foo")
			.build();
		rc.get("/checkHeader").header("Accept","text/plain").run().getBody().assertValue("['text/foo','text/plain']");
	}

	@Test
	public void h04b_dontOverrideAccept_withRequest() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Accept")
			.header("Accept", "text/foo")
			.build();
		RestRequest req = rc.get("/checkHeader");
		req.setHeader("Accept","text/plain");
		req.run().getBody().assertValue("['text/plain']");
	}

	@Test
	public void h05_dontOverrideContentType() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Content-Type")
			.header("Content-Type", "text/plain")
			.build();
		rc.get("/checkHeader").run().getBody().assertValue("['text/plain']");
	}

	@Test
	public void h06_dontOverrideAccept_withRequest() throws Exception {
		MockLogger ml = new MockLogger();
		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.logTo(Level.SEVERE, ml)
			.header("Check", "Content-Type")
			.header("Content-Type", "text/foo")
			.build();
		rc.get("/checkHeader").header("Content-Type", "text/plain").complete();
		ml.assertLevel(Level.SEVERE);
		ml.assertMessageContains("Content-Type: text/plain").assertMessageContains("Content-Type: text/plain");
	}

//	@Test
//	public void h07_header_HttpPartSerializer() throws Exception {
//	//	public RestClientBuilder header(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//		fail();
//	}
//
//	// TODO - Test Header[] on servlet side.
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Query
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void i01_query_StringObject() throws Exception { fail(); }
////	public RestClientBuilder query(String name, Object value) {
//
//	@Test
//	public void i02_query_NameValuePair() throws Exception { fail(); }
////	public RestClientBuilder query(NameValuePair param) {
//
//	@Test
//	public void i03_query_OMap() throws Exception { fail(); }
////	public RestClientBuilder query(OMap params) {
//
//	@Test
//	public void i04_query_Map() throws Exception { fail(); }
////	public RestClientBuilder query(Map<String,Object> params) {
//
//	@Test
//	public void i05_query_NameValuePairs() throws Exception { fail(); }
////	public RestClientBuilder query(NameValuePairs params) {
//
//	@Test
//	public void i06_query_NameValuePairArray() throws Exception { fail(); }
////	public RestClientBuilder query(NameValuePair...params) {
//
//	@Test
//	public void i07_query_Objects() throws Exception { fail(); }
////	public RestClientBuilder query(Object...pairs) {
//
//	@Test
//	public void i08_query_HttpPartSerializer() throws Exception { fail(); }
////	public RestClientBuilder query(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Form data
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void j01_formData_StringObject() throws Exception { fail(); }
////	public RestClientBuilder formData(String name, Object value) {
//
//	@Test
//	public void j02_formData_NameValuePair() throws Exception { fail(); }
////	public RestClientBuilder formData(NameValuePair param) {
//
//	@Test
//	public void j03_formData_OMap() throws Exception { fail(); }
////	public RestClientBuilder formData(OMap params) {
//
//	@Test
//	public void j04_formData_Map() throws Exception { fail(); }
////	public RestClientBuilder formData(Map<String,Object> params) {
//
//	@Test
//	public void j05_formData_NameValuePairs() throws Exception { fail(); }
////	public RestClientBuilder formData(NameValuePairs params) {
//
//	@Test
//	public void j06_formData_NameValuePairArray() throws Exception { fail(); }
////	public RestClientBuilder formData(NameValuePair...params) {
//
//	@Test
//	public void j07_formData_Objects() throws Exception { fail(); }
////	public RestClientBuilder formData(Object...pairs) {
//
//	@Test
//	public void j08_formData_HttpPartSerializer() throws Exception { fail(); }
////	public RestClientBuilder formData(String name, Object value, HttpPartSerializer serializer, HttpPartSchema schema) {
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// RestClient properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void k01_restClient_CallHandlerClass() throws Exception { fail(); }
////	public RestClientBuilder callHandler(Class<? extends RestCallHandler> value) {
//
//	@Test
//	public void k02_restClient_CallHandlerObject() throws Exception { fail(); }
////	public RestClientBuilder callHandler(RestCallHandler value) {
//
//	@Test
//	public void k03_restClient_errorCodes() throws Exception { fail(); }
////	public RestClientBuilder errorCodes(Predicate<Integer> value) {
//
//	@Test
//	public void k04_restClient_executorService() throws Exception { fail(); }
////	public RestClientBuilder executorService(ExecutorService executorService, boolean shutdownOnClose) {
//
//	@Test
//	public void k05_restClient_keepHttpClientOpenBoolean() throws Exception { fail(); }
////	public RestClientBuilder keepHttpClientOpen(boolean value) {
//
//	@Test
//	public void k06_restClient_keepHttpClientOpen() throws Exception { fail(); }
////	public RestClientBuilder keepHttpClientOpen() {
//
//	@Test
//	public void k07_restClient_interceptorsClasses() throws Exception { fail(); }
////	public RestClientBuilder interceptors(Class<? extends RestCallInterceptor>...values) {
//
//	@Test
//	public void k08_restClient_interceptorsObjects() throws Exception { fail(); }
////	public RestClientBuilder interceptors(RestCallInterceptor...value) {
//
//	@Test
//	public void k09_restClient_leakDetection() throws Exception { fail(); }
////	public RestClientBuilder leakDetection() {
//
//	@Test
//	public void k10_restClient_leakDetectionBoolean() throws Exception { fail(); }
////	public RestClientBuilder leakDetection(boolean value) {
//
//	@Test
//	public void k11_restClient_marshallObject() throws Exception { fail(); }
////	public RestClientBuilder marshall(Marshall value) {
//
//	@Test
//	public void k12_restClient_marshallsObjects() throws Exception { fail(); }
////	public RestClientBuilder marshalls(Marshall...value) {
//
//	@Test
//	public void k13_restClient_parserClass() throws Exception { fail(); }
////	public RestClientBuilder parser(Class<? extends Parser> value) {
//
//	@Test
//	public void k14_restClient_parserObject() throws Exception { fail(); }
////	public RestClientBuilder parser(Parser value) {
//
//	@Test
//	public void k15_restClient_parsersClasses() throws Exception { fail(); }
////	public RestClientBuilder parsers(Class<? extends Parser>...value) {
//
//	@Test
//	public void k16_restClient_parsersObjects() throws Exception { fail(); }
////	public RestClientBuilder parsers(Parser...value) {
//
//	@Test
//	public void k17_restClient_partParserClass() throws Exception { fail(); }
////	public RestClientBuilder partParser(Class<? extends HttpPartParser> value) {
//
//	@Test
//	public void k18_restClient_partParserObject() throws Exception { fail(); }
////	public RestClientBuilder partParser(HttpPartParser value) {
//
//	@Test
//	public void k19_restClient_partSerializerClass() throws Exception { fail(); }
////	public RestClientBuilder partSerializer(Class<? extends HttpPartSerializer> value) {
//
//	@Test
//	public void k20_restClient_partSerializerObject() throws Exception { fail(); }
////	public RestClientBuilder partSerializer(HttpPartSerializer value) {
//
//	@Test
//	public void k21_restClient_serializerClass() throws Exception { fail(); }
////	public RestClientBuilder serializer(Class<? extends Serializer> value) {
//
//	@Test
//	public void k22_restClient_serializerObject() throws Exception { fail(); }
////	public RestClientBuilder serializer(Serializer value) {
//
//	@Test
//	public void k23_restClient_serializersClasses() throws Exception { fail(); }
////	public RestClientBuilder serializers(Class<? extends Serializer>...value) {
//
//	@Test
//	public void k24_restClient_serializersObjects() throws Exception { fail(); }
////	public RestClientBuilder serializers(Serializer...value) {
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Serializer properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void l01_serializer_addBeanTypesBoolean() throws Exception { fail(); }
////	public RestClientBuilder addBeanTypes(boolean value) {
//
//	@Test
//	public void l02_serializer_addBeanTypes() throws Exception { fail(); }
////	public RestClientBuilder addBeanTypes() {
//
//	@Test
//	public void l03_serializer_addRootTypeBoolean() throws Exception { fail(); }
////	public RestClientBuilder addRootType(boolean value) {
//
//	@Test
//	public void l04_serializer_addRootType() throws Exception { fail(); }
////	public RestClientBuilder addRootType() {
//
//	@Test
//	public void l05_serializer_detectRecursionsBoolean() throws Exception { fail(); }
////	public RestClientBuilder detectRecursions(boolean value) {
//
//	@Test
//	public void l06_serializer_detectRecursions() throws Exception { fail(); }
////	public RestClientBuilder detectRecursions() {
//
//	@Test
//	public void l07_serializer_ignoreRecursionsBoolean() throws Exception { fail(); }
////	public RestClientBuilder ignoreRecursions(boolean value) {
//
//	@Test
//	public void l08_serializer_ignoreRecursions() throws Exception { fail(); }
////	public RestClientBuilder ignoreRecursions() {
//
//	@Test
//	public void l09_serializer_initialDepth() throws Exception { fail(); }
////	public RestClientBuilder initialDepth(int value) {
//
//	@Test
//	public void l10_serializer_listenerSClass() throws Exception { fail(); }
////	public RestClientBuilder listenerS(Class<? extends SerializerListener> value) {
//
//	@Test
//	public void l11_serializer_maxDepth() throws Exception { fail(); }
////	public RestClientBuilder maxDepth(int value) {
//
//	@Test
//	public void l12_serializer_sortCollectionsBoolean() throws Exception { fail(); }
////	public RestClientBuilder sortCollections(boolean value) {
//
//	@Test
//	public void l13_serializer_sortCollections() throws Exception { fail(); }
////	public RestClientBuilder sortCollections() {
//
//	@Test
//	public void l14_serializer_sortMapsBoolean() throws Exception { fail(); }
////	public RestClientBuilder sortMaps(boolean value) {
//
//	@Test
//	public void l15_serializer_sortMaps() throws Exception { fail(); }
////	public RestClientBuilder sortMaps() {
//
//	@Test
//	public void l16_serializer_trimEmptyCollectionsBoolean() throws Exception { fail(); }
////	public RestClientBuilder trimEmptyCollections(boolean value) {
//
//	@Test
//	public void l17_serializer_trimEmptyCollections() throws Exception { fail(); }
////	public RestClientBuilder trimEmptyCollections() {
//
//	@Test
//	public void l18_serializer_trimEmptyMapsBoolean() throws Exception { fail(); }
////	public RestClientBuilder trimEmptyMaps(boolean value) {
//
//	@Test
//	public void l19_serializer_trimEmptyMaps() throws Exception { fail(); }
////	public RestClientBuilder trimEmptyMaps() {
//
//	@Test
//	public void l20_serializer_trimNullPropertiesBoolean() throws Exception { fail(); }
////	public RestClientBuilder trimNullProperties(boolean value) {
//
//	@Test
//	public void l21_serializer_trimStringsSBoolean() throws Exception { fail(); }
////	public RestClientBuilder trimStringsS(boolean value) {
//
//	@Test
//	public void l22_serializer_trimStringsS() throws Exception { fail(); }
////	public RestClientBuilder trimStringsS() {
//
//	@Test
//	public void l23_serializer_uriContext() throws Exception { fail(); }
////	public RestClientBuilder uriContext(UriContext value) {
//
//	@Test
//	public void l24_serializer_uriRelativity() throws Exception { fail(); }
////	public RestClientBuilder uriRelativity(UriRelativity value) {
//
//	@Test
//	public void l25_serializer_uriResolution() throws Exception { fail(); }
////	public RestClientBuilder uriResolution(UriResolution value) {
//
//	@Test
//	public void l26_serializer_maxIndent() throws Exception { fail(); }
////	public RestClientBuilder maxIndent(int value) {
//
//	@Test
//	public void l27_serializer_quoteChar() throws Exception { fail(); }
////	public RestClientBuilder quoteChar(char value) {
//
//	@Test
//	public void l28_serializer_sq() throws Exception { fail(); }
////	public RestClientBuilder sq() {
//
//	@Test
//	public void l29_serializer_useWhitespaceBoolean() throws Exception { fail(); }
////	public RestClientBuilder useWhitespace(boolean value) {
//
//	@Test
//	public void l30_serializer_useWhitespace() throws Exception { fail(); }
////	public RestClientBuilder useWhitespace() {
//
//	@Test
//	public void l31_serializer_ws() throws Exception { fail(); }
////	public RestClientBuilder ws() {
//
//	@Test
//	public void l32_serializer_binaryOutputFormat() throws Exception { fail(); }
////	public RestClientBuilder binaryOutputFormat(BinaryFormat value) {
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Parser properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void m01_parser_autoCloseStreamsBoolean() throws Exception { fail(); }
////	public RestClientBuilder autoCloseStreams(boolean value) {
//
//	@Test
//	public void m02_parser_autoCloseStreams() throws Exception { fail(); }
////	public RestClientBuilder autoCloseStreams() {
//
//	@Test
//	public void m03_parser_debugOutputLines() throws Exception { fail(); }
////	public RestClientBuilder debugOutputLines(int value) {
//
//	@Test
//	public void m04_parser_listenerPClass() throws Exception { fail(); }
////	public RestClientBuilder listenerP(Class<? extends ParserListener> value) {
//
//	@Test
//	public void m05_parser_strictBoolean() throws Exception { fail(); }
////	public RestClientBuilder strict(boolean value) {
//
//	@Test
//	public void m06_parser_strict() throws Exception { fail(); }
////	public RestClientBuilder strict() {
//
//	@Test
//	public void m07_parser_trimStringsPBoolean() throws Exception { fail(); }
////	public RestClientBuilder trimStringsP(boolean value) {
//
//	@Test
//	public void m08_parser_trimStringsP() throws Exception { fail(); }
////	public RestClientBuilder trimStringsP() {
//
//	@Test
//	public void m09_parser_unbufferedBoolean() throws Exception { fail(); }
////	public RestClientBuilder unbuffered(boolean value) {
//
//	@Test
//	public void m10_parser_unbuffered() throws Exception { fail(); }
////	public RestClientBuilder unbuffered() {
//
//	@Test
//	public void m11_parser_fileCharset() throws Exception { fail(); }
////	public RestClientBuilder fileCharset(String value) {
//
//	@Test
//	public void m12_parser_inputStreamCharset() throws Exception { fail(); }
////	public RestClientBuilder inputStreamCharset(String value) {
//
//	@Test
//	public void m13_parser_binaryInputFormat() throws Exception { fail(); }
////	public RestClientBuilder binaryInputFormat(BinaryFormat value) {
//
//	@Test
//	public void m14_parser_paramFormat() throws Exception { fail(); }
////	public RestClientBuilder paramFormat(String value) {
//
//	@Test
//	public void m15_parser_paramFormatPlain() throws Exception { fail(); }
////	public RestClientBuilder paramFormatPlain() {
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// Context properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void n01_context_addMap() throws Exception { fail(); }
////	public RestClientBuilder add(Map<String,Object> properties) {
//
//	@Test
//	public void n02_context_addToStringObject() throws Exception { fail(); }
////	public RestClientBuilder addTo(String name, Object value) {
//
//	@Test
//	public void n03_context_appendToStringObject() throws Exception { fail(); }
////	public RestClientBuilder appendTo(String name, Object value) {
//
//	@Test
//	public void n04_context_prependToStringObject() throws Exception { fail(); }
////	public RestClientBuilder prependTo(String name, Object value) {
//
//	@Test
//	public void n05_context_addToStringStringObject() throws Exception { fail(); }
////	public RestClientBuilder addTo(String name, String key, Object value) {
//
//	@Test
//	public void n06_context_apply() throws Exception { fail(); }
////	public RestClientBuilder apply(PropertyStore copyFrom) {
//
//	@Test
//	public void n07_context_applyAnnotationsClasses() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(java.lang.Class<?>...fromClasses) {
//
//	@Test
//	public void n08_context_applyAnnotationsMethods() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(Method...fromMethods) {
//
//	@Test
//	public void n09_context_applyAnnotationsAnnotationList() throws Exception { fail(); }
////	public RestClientBuilder applyAnnotations(AnnotationList al, VarResolverSession r) {
//
//	@Test
//	public void n10_context_removeFrom() throws Exception { fail(); }
////	public RestClientBuilder removeFrom(String name, Object value) {
//
//	@Test
//	public void n11_context_setMap() throws Exception { fail(); }
////	public RestClientBuilder set(Map<String,Object> properties) {
//
//	@Test
//	public void n12_context_setStringObject() throws Exception { fail(); }
////	public RestClientBuilder set(String name, Object value) {
//
//	@Test
//	public void n13_context_annotations() throws Exception { fail(); }
////	public RestClientBuilder annotations(Annotation...values) {
//
//	//-----------------------------------------------------------------------------------------------------------------
//	// BeanContext properties
//	//-----------------------------------------------------------------------------------------------------------------
//
//	@Test
//	public void o001_beanContext_beanClassVisibility() throws Exception { fail(); }
////	public RestClientBuilder beanClassVisibility(Visibility value) {
//
//	@Test
//	public void o002_beanContext_beanConstructorVisibility() throws Exception { fail(); }
////	public RestClientBuilder beanConstructorVisibility(Visibility value) {
//
//	@Test
//	public void o003_beanContext_beanDictionaryClasses() throws Exception { fail(); }
////	public RestClientBuilder beanDictionary(java.lang.Class<?>...values) {
//
//	@Test
//	public void o004_beanContext_beanDictionaryObjects() throws Exception { fail(); }
////	public RestClientBuilder beanDictionary(Object...values) {
//
//	@Test
//	public void o005_beanContext_beanDictionaryRemoveClasses() throws Exception { fail(); }
////	public RestClientBuilder beanDictionaryRemove(java.lang.Class<?>...values) {
//
//	@Test
//	public void o006_beanContext_beanDictionaryRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder beanDictionaryRemove(Object...values) {
//
//	@Test
//	public void o007_beanContext_beanDictionaryReplaceClasses() throws Exception { fail(); }
////	public RestClientBuilder beanDictionaryReplace(java.lang.Class<?>...values) {
//
//	@Test
//	public void o008_beanContext_beanDictionaryReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder beanDictionaryReplace(Object...values) {
//
//	@Test
//	public void o009_beanContext_beanFieldVisibility() throws Exception { fail(); }
////	public RestClientBuilder beanFieldVisibility(Visibility value) {
//
//	@Test
//	public void o010_beanContext_beanFiltersClasses() throws Exception { fail(); }
////	public RestClientBuilder beanFilters(java.lang.Class<?>...values) {
//
//	@Test
//	public void o011_beanContext_beanFiltersObjects() throws Exception { fail(); }
////	public RestClientBuilder beanFilters(Object...values) {
//
//	@Test
//	public void o012_beanContext_beanFiltersRemoveClasses() throws Exception { fail(); }
////	public RestClientBuilder beanFiltersRemove(java.lang.Class<?>...values) {
//
//	@Test
//	public void o013_beanContext_beanFiltersRemoveObjects() throws Exception { fail(); }
////	public RestClientBuilder beanFiltersRemove(Object...values) {
//
//	@Test
//	public void o014_beanContext_beanFiltersReplaceClasses() throws Exception { fail(); }
////	public RestClientBuilder beanFiltersReplace(java.lang.Class<?>...values) {
//
//	@Test
//	public void o015_beanContext_beanFiltersReplaceObjects() throws Exception { fail(); }
////	public RestClientBuilder beanFiltersReplace(Object...values) {
//
//	@Test
//	public void o016_beanContext_beanMapPutReturnsOldValue() throws Exception { fail(); }
////	public RestClientBuilder beanMapPutReturnsOldValue() {
//
//	@Test
//	public void o017_beanContext_beanMapPutReturnsOldValueBoolean() throws Exception { fail(); }
////	public RestClientBuilder beanMapPutReturnsOldValue(boolean value) {
//
//	@Test
//	public void o018_beanContext_beanMethodVisibility() throws Exception { fail(); }
////	public RestClientBuilder beanMethodVisibility(Visibility value) {
//
//	@Test
//	public void o019_beanContext_beanTypePropertyName() throws Exception { fail(); }
////	public RestClientBuilder beanTypePropertyName(String value) {
//
//	@Test
//	public void o020_beanContext_beansDontRequireSomeProperties() throws Exception { fail(); }
////	public RestClientBuilder beansDontRequireSomeProperties() {
//
//	@Test
//	public void o021_beanContext_beansRequireDefaultConstructor() throws Exception { fail(); }
////	public RestClientBuilder beansRequireDefaultConstructor() {
//
//	@Test
//	public void o022_beanContext_beansRequireDefaultConstructorBoolean() throws Exception { fail(); }
////	public RestClientBuilder beansRequireDefaultConstructor(boolean value) {
//
//	@Test
//	public void o023_beanContext_beansRequireSerializable() throws Exception { fail(); }
////	public RestClientBuilder beansRequireSerializable() {
//
//	@Test
//	public void o024_beanContext_beansRequireSerializableBoolean() throws Exception { fail(); }
////	public RestClientBuilder beansRequireSerializable(boolean value) {
//
//	@Test
//	public void o025_beanContext_beansRequireSettersForGetters() throws Exception { fail(); }
////	public RestClientBuilder beansRequireSettersForGetters() {
//
//	@Test
//	public void o026_beanContext_beansRequireSettersForGettersBoolean() throws Exception { fail(); }
////	public RestClientBuilder beansRequireSettersForGetters(boolean value) {
//
//	@Test
//	public void o027_beanContext_beansRequireSomePropertiesBoolean() throws Exception { fail(); }
////	public RestClientBuilder beansRequireSomeProperties(boolean value) {
//
//	@Test
//	public void o028_beanContext_bpiMap() throws Exception { fail(); }
////	public RestClientBuilder bpi(Map<String,String> values) {
//
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
//	public void o055_beanContext_excludePropertiesMap() throws Exception { fail(); }
////	public RestClientBuilder excludeProperties(Map<String,String> values) {
//
//	@Test
//	public void o056_beanContext_excludePropertiesClassString() throws Exception { fail(); }
////	public RestClientBuilder excludeProperties(Class<?> beanClass, String properties) {
//
//	@Test
//	public void o057_beanContext_excludePropertiesStringString() throws Exception { fail(); }
////	public RestClientBuilder excludeProperties(String beanClassName, String value) {
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
//	public void o071_beanContext_includePropertiesMap() throws Exception { fail(); }
////	public RestClientBuilder includeProperties(Map<String,String> values) {
//
//	@Test
//	public void o072_beanContext_includePropertiesClassString() throws Exception { fail(); }
////	public RestClientBuilder includeProperties(Class<?> beanClass, String value) {
//
//	@Test
//	public void o073_beanContext_includePropertiesStringString() throws Exception { fail(); }
////	public RestClientBuilder includeProperties(String beanClassName, String value) {
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
}
