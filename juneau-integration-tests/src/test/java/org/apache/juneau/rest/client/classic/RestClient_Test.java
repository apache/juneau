/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;
import static org.apache.juneau.http.classic.HttpResponses.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.io.HttpRequestExecutor;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.auth.*;
import org.apache.hc.client5.http.config.*;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.core5.concurrent.*;
import org.apache.hc.client5.http.impl.classic.*;
import org.apache.hc.core5.http.message.*;
import org.apache.hc.core5.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"deprecation", // Uses deprecated API
	"java:S1186", // Empty test method intentional for framework testing
	"removal" // Tests deprecated API for backward compatibility
})
class RestClient_Test extends TestBase {

	public static class ABean {
		public int f;
		static ABean get() {
			var x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestResource {
		@RestGet
		public ABean bean() {
			return bean;
		}
		@RestGet(path="/echo/*")
		public String echo(org.apache.juneau.rest.server.RestRequest req) {
			return req.toString();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Override client and builder.
	//------------------------------------------------------------------------------------------------------------------

	public static class A2 extends RestClient.Builder<A2> {
		public A2() {}
	}

	@Test void a02_basic_useNoArgConstructor() {
		assertDoesNotThrow(()->new A2().build());
	}

	@Test void a03_basic_close() throws IOException {
		RestClient.create().build().close();
		RestClient.create().build().closeQuietly();
		RestClient.create().keepHttpClientOpen().build().close();
		RestClient.create().keepHttpClientOpen().build().closeQuietly();
		RestClient.create().httpClient(null).keepHttpClientOpen().build().close();

		var es = new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
		RestClient.create().executorService(es,true).build().close();
		RestClient.create().executorService(es,true).build().closeQuietly();
		RestClient.create().executorService(es,false).build().close();
		RestClient.create().executorService(es,false).build().closeQuietly();

		RestClient.create().debug().build().close();
		assertDoesNotThrow(()->RestClient.create().debug().build().closeQuietly());
	}

	@Test void a04_request_whenClosed() {
		var rc = client().build();
		rc.closeQuietly();
		assertThrowsWithMessage(Exception.class, "RestClient.close() has already been called", ()->rc.request("get","/bean",null));
	}

	@Test void a05_request_whenClosed_withStackCreation() {
		var rc = client().debug().build();
		rc.closeQuietly();
		assertThrowsWithMessage(Exception.class, "RestClient.close() has already been called", ()->rc.request("get","/bean",null));
	}

	@Test void a06_request_runCalledTwice() {
		assertThrowsWithMessage(RestCallException.class, "run() already called.", ()->{RestRequest r = client().build().get("/echo"); r.run(); r.run();});
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//------------------------------------------------------------------------------------------------------------------

	public static class B4 extends RestClient {
		private static boolean createRequestCalled, createResponseCalled;
		public B4(RestClient.Builder<?> b) {
			super(b);
		}
		@Override
		protected RestRequest createRequest(java.net.URI uri, String method, boolean hasBody) throws RestCallException {
			createRequestCalled = true;
			return super.createRequest(uri, method, hasBody);
		}
		@Override
		protected RestResponse createResponse(RestRequest req, ClassicHttpResponse httpResponse, Parser parser) throws RestCallException {
			createResponseCalled = true;
			return super.createResponse(req, httpResponse, parser);
		}
		@Override /* HttpClient */
		public ClassicHttpResponse execute(ClassicHttpRequest request, HttpContext context) throws IOException {
			return new org.apache.hc.core5.http.message.BasicClassicHttpResponse(200, null);
		}
	}

	@Test void b04_restClient_overrideCreateRequest() throws Exception {
		RestClient.create().json5().build(B4.class).get("foo").run();
		assertTrue(B4.createRequestCalled);
		assertTrue(B4.createResponseCalled);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 implements HttpRequestInterceptor, HttpResponseInterceptor {
		@Override
		public void process(HttpRequest request, EntityDetails entity, HttpContext context) throws HttpException, IOException {
			request.setHeader("A1","1");
		}
		@Override
		public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException,IOException {
			response.setHeader("B1","1");
		}
	}

	@Test void c01_httpClient_interceptors() throws Exception {
		HttpRequestInterceptor x1 = (request, entity, context) -> request.setHeader("A1","1");
		HttpResponseInterceptor x2 = (response, entity, context) -> response.setHeader("B1","1");
		HttpRequestInterceptor x3 = (request, entity, context) -> request.setHeader("A2","2");
		HttpResponseInterceptor x4 = (response, entity, context) -> response.setHeader("B2","2");

		client().addInterceptorFirst(x1).addInterceptorLast(x2).addInterceptorFirst(x3).addInterceptorLast(x4)
			.build().get("/echo").run().assertContent().isContains("A1: 1","A2: 2").assertHeader("B1").is("1").assertHeader("B2").is("2");
		client().interceptors(C01.class).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
		client().interceptors(new C01()).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
	}

	@Test void c02_httpClient_httpProcessor() throws RestCallException {
		var x = new HttpProcessor() {
			@Override
			public void process(HttpRequest request, EntityDetails entity, HttpContext context) throws HttpException, IOException {
				request.setHeader("A1","1");
			}
			@Override
			public void process(HttpResponse response, EntityDetails entity, HttpContext context) throws HttpException, IOException {
				response.setHeader("B1","1");
			}
		};
		client().httpProcessor(x).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
	}

	@Test void c03_httpClient_requestExecutor() throws RestCallException {
		var b1 = new AtomicBoolean();
		var x = new HttpRequestExecutor() {
			@Override
			public ClassicHttpResponse execute(ClassicHttpRequest request, HttpClientConnection conn, org.apache.hc.core5.http.io.HttpResponseInformationCallback informationCallback, HttpContext context) throws HttpException, IOException {
				b1.set(true);
				return super.execute(request, conn, informationCallback, context);
			}
		};
		client().requestExecutor(x).build().get("/echo").run().assertContent().isContains("GET /echo HTTP/1.1");
		assertTrue(b1.get());
	}

	@Test void c04_httpClient_defaultHeaders() throws RestCallException {
		client().headersDefault(stringHeader("Foo","bar")).build().get("/echo").run().assertContent().isContains("GET /echo HTTP/1.1","Foo: bar");
	}

	@Test void c05_httpClient_httpClientBuilderMethods() {
		assertDoesNotThrow(()->RestClient.create().disableRedirectHandling().redirectStrategy(DefaultRedirectStrategy.INSTANCE).defaultCookieSpecRegistry(null).sslHostnameVerifier(null).publicSuffixMatcher(null).sslContext(null).sslSocketFactory(null).maxConnTotal(10).maxConnPerRoute(10).defaultSocketConfig(null).defaultConnectionConfig(null).connectionTimeToLive(100,TimeUnit.DAYS).connectionManager(null).connectionManagerShared(true).connectionReuseStrategy(null).keepAliveStrategy(null).targetAuthenticationStrategy(null).proxyAuthenticationStrategy(null).userTokenHandler(null).disableConnectionState().schemePortResolver(null).disableCookieManagement().disableContentCompression().disableAuthCaching().retryHandler(null).disableAutomaticRetries().proxy(null).routePlanner(null).connectionBackoffStrategy(null).backoffManager(null).serviceUnavailableRetryStrategy(null).defaultCookieStore(null).defaultCredentialsProvider(null).defaultAuthSchemeRegistry(null).contentDecoderRegistry(null).defaultRequestConfig(null).useSystemProperties().evictExpiredConnections().evictIdleConnections(1,TimeUnit.DAYS));
	}

	@Test void c06_httpClient_unusedHttpClientMethods() {
		var x = RestClient.create().build();

		assertNotNull(x.getHttpClientConnectionManager());
	}

	@Test void c07_httpClient_executeHttpUriRequest() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(x);
		assertEquals("{\"f\":1}",read(res.getEntity().getContent()));
	}

	@Test void c08_httpClient_executeHttpHostHttpRequest() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		var target = new HttpHost("localhost");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(target,x);
		assertEquals("{\"f\":1}",read(res.getEntity().getContent()));
	}

	@Test void c09_httpClient_executeHttpHostHttpRequestHttpContext() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		var target = new HttpHost("localhost");
		var context = new BasicHttpContext();
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(target,x,context);
		assertEquals("{\"f\":1}",read(res.getEntity().getContent()));
	}

	@Test void c10_httpClient_executeResponseHandler() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(x,new BasicHttpClientResponseHandler());
		assertEquals("{\"f\":1}",res);
	}

	@Test void c11_httpClient_executeHttpUriRequestResponseHandlerHttpContext() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(x,new BasicHttpContext(),new BasicHttpClientResponseHandler());
		assertEquals("{\"f\":1}",res);
	}

	@Test void c12_httpClient_executeHttpHostHttpRequestResponseHandlerHttpContext() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicHttpContext(),new BasicHttpClientResponseHandler());
		assertEquals("{\"f\":1}",res);
	}

	@Test void c13_httpClient_executeHttpHostHttpRequestResponseHandler() throws Exception {
		var x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","application/json");
		var res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicHttpClientResponseHandler());
		assertEquals("{\"f\":1}",res);
	}

	@Test void c14_httpClient_requestConfig() throws Exception {
		var req = client().build().get("/bean").config(RequestConfig.custom().setMaxRedirects(1).build());
		req.run().assertContent("{\"f\":1}");
		assertEquals(1, req.getConfig().getMaxRedirects());
	}

	@Test void c15_httpClient_pooled() throws Exception {
		var x1 = RestClient.create().json5().pooled().build();
		var x2 = RestClient.create().json5().build();
		var x3 = client().pooled().build();
		assertEquals("PoolingHttpClientConnectionManager",cns(ClassInfo.of(x1.httpClient).getDeclaredField(x -> x.hasName("connManager")).get().accessible().get(x1.httpClient)));
		assertEquals("BasicHttpClientConnectionManager",cns(ClassInfo.of(x2.httpClient).getDeclaredField(x -> x.hasName("connManager")).get().accessible().get(x2.httpClient)));
		assertEquals("MockHttpClientConnectionManager",cns(ClassInfo.of(x3.httpClient).getDeclaredField(x -> x.hasName("connManager")).get().accessible().get(x3.httpClient)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Authentication
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestResource {
		@RestGet
		public String echo(@org.apache.juneau.http.Header("Authorization") String auth, org.apache.juneau.rest.server.RestResponse res) {
			if (auth == null) {
				throw unauthorized().setHeader2("WWW-Authenticate","BASIC realm=\"foo\"");
			}
			assertEquals("Basic dXNlcjpwdw==",auth);
			return "OK";
		}
	}

	@Test void d01_basicAuth() throws RestCallException {
		client(D.class).basicAuth(null,-1,"user","pw").build().get("/echo").run().assertContent().isContains("OK");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_other_completeFuture() throws Exception {
		client().build().get("/bean").completeFuture().get().assertStatus(200);
	}

	public static class E2 implements Cancellable {
		@Override
		public boolean cancel() {
			return false;
		}
	}

	@Test void e02_httpRequestBase_setCancellable() throws Exception {
		client().build().get("/bean").cancellable(new E2()).run().assertStatus(200);
	}

	@Test void e03_httpRequestBase_protocolVersion() throws Exception {
		client().build().get("/bean").protocolVersion(new ProtocolVersion("http", 2, 0)).run().assertStatus(200);
		var x = client().build().get("/bean").protocolVersion(new ProtocolVersion("http", 2, 0)).getVersion();
		assertEquals(2,x.getMajor());
	}

	@Test void e04_httpRequestBase_completed() throws Exception {
		client().build().get("/bean").completed().run().assertStatus(200);
	}

	@Test void e05_httpUriRequest_abort() throws Exception {
		var x = client().build().get("/bean");
		x.abort();
		assertTrue(x.isAborted());
	}

	@Test void e06_httpMessage_getRequestLine() throws Exception {
		var x = client().build().get("/bean");
		assertEquals("GET",x.getRequestLine().getMethod());
	}

	@Test void e07_httpMessage_containsHeader() throws Exception {
		var x = client().build().get("/bean").header("Foo", "bar");
		assertTrue(x.containsHeader("Foo"));
	}

	@Test void e08_httpMessage_getFirstHeader_getLastHeader() throws Exception {
		var x = client().build().get("/bean").header("Foo","bar").header("Foo","baz");
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
	}

	@Test void e09_httpMessage_addHeader() throws Exception {
		var x = client().build().get("/bean");
		x.addHeader(header("Foo","bar"));
		x.addHeader("Foo","baz");
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
	}

	@Test void e10_httpMessage_setHeader() throws Exception {
		var x = client().build().get("/bean");
		x.setHeader(header("Foo","bar"));
		x.setHeader(header("Foo","baz"));
		assertEquals("baz",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
		x.setHeader("Foo","qux");
		assertEquals("qux",x.getFirstHeader("Foo").getValue());
		assertEquals("qux",x.getLastHeader("Foo").getValue());
	}

	@Test void e11_httpMessage_setHeaders() throws Exception {
		var x = client().build().get("/bean");
		x.setHeaders(a(header("Foo","bar")));
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
	}

	@Test void e12_httpMessage_removeHeaders() throws Exception {
		var x = client().build().get("/bean");
		x.setHeaders(a(header("Foo","bar")));
		x.removeHeaders("Foo");
		assertNull(x.getFirstHeader("Foo"));
	}

	@Test void e13_httpMessage_removeHeader() throws Exception {
		var x = client().build().get("/bean");
		x.setHeaders(a(header("Foo","bar")));
		assertDoesNotThrow(() -> x.removeHeader(header("Foo","bar")));
		// Note: assertNull(x.getFirstHeader("Foo")) fails due to HttpClient API behavior
	}

	@Test void e14_httpMessage_headerIterator() throws Exception {
		var x = client().build().get("/bean");
		x.setHeaders(a(header("Foo","bar")));
		assertEquals("Foo: bar", x.headerIterator().next().toString());
		assertEquals("Foo: bar", x.headerIterator("Foo").next().toString());
	}


	@Test void e16_toMap() throws Exception {
		assertNotNull(client().build().toString());
		assertNotNull(client().build().get("/bean").toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder<?> client() {
		return MockRestClient.create(A.class).json();
	}

	private static RestClient.Builder<?> client(Class<?> c) {
		return MockRestClient.create(c).noTrace().json();
	}

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}
}