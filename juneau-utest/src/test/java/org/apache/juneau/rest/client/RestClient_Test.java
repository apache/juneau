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
package org.apache.juneau.rest.client;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResponses.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.concurrent.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.http.protocol.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestGet
		public ABean bean() {
			return bean;
		}
		@RestGet(path="/echo/*")
		public String echo(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Override client and builder.
	//------------------------------------------------------------------------------------------------------------------

	public static class A2 extends RestClient.Builder {
		public A2() {
			super();
		}
	}

	@Test
	public void a02_basic_useNoArgConstructor() {
		new A2().build();
	}

	@Test
	public void a03_basic_close() throws IOException {
		RestClient.create().build().close();
		RestClient.create().build().closeQuietly();
		RestClient.create().keepHttpClientOpen().build().close();
		RestClient.create().keepHttpClientOpen().build().closeQuietly();
		RestClient.create().httpClient(null).keepHttpClientOpen().build().close();

		ExecutorService es = new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(10));
		RestClient.create().executorService(es,true).build().close();
		RestClient.create().executorService(es,true).build().closeQuietly();
		RestClient.create().executorService(es,false).build().close();
		RestClient.create().executorService(es,false).build().closeQuietly();

		RestClient.create().debug().build().close();
		RestClient.create().debug().build().closeQuietly();
	}


	@Test
	public void a04_request_whenClosed() throws Exception {
		RestClient rc = client().build();
		rc.closeQuietly();
		assertThrown(()->rc.request("get","/bean",null)).asMessage().isContains("RestClient.close() has already been called");
	}

	@Test
	public void a05_request_whenClosed_withStackCreation() throws Exception {
		RestClient rc = client().debug().build();
		rc.closeQuietly();
		assertThrown(()->rc.request("get","/bean",null)).asMessage().isContains("RestClient.close() has already been called");
	}

	@Test
	public void a06_request_runCalledTwice() throws Exception {
		assertThrown(()->{RestRequest r = client().build().get("/echo"); r.run(); r.run();}).asMessage().is("run() already called.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//------------------------------------------------------------------------------------------------------------------

	public static class B4 extends RestClient {
		private static boolean CREATE_REQUEST_CALLED, CREATE_RESPONSE_CALLED;
		public B4(RestClient.Builder b) {
			super(b);
		}
		@Override
		protected RestRequest createRequest(java.net.URI uri, String method, boolean hasBody) throws RestCallException {
			CREATE_REQUEST_CALLED = true;
			return super.createRequest(uri, method, hasBody);
		}
		@Override
		protected RestResponse createResponse(RestRequest req, HttpResponse httpResponse, Parser parser) throws RestCallException {
			CREATE_RESPONSE_CALLED = true;
			return super.createResponse(req, httpResponse, parser);
		}
		@Override /* HttpClient */
		public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
			return new BasicHttpResponse(new ProtocolVersion("http",1,1),200,null);
		}
	}

	@Test
	public void b04_restClient_overrideCreateRequest() throws Exception {
		RestClient.create().json5().build(B4.class).get("foo").run();
		assertTrue(B4.CREATE_REQUEST_CALLED);
		assertTrue(B4.CREATE_RESPONSE_CALLED);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 implements HttpRequestInterceptor, HttpResponseInterceptor {
		@Override
		public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
			request.setHeader("A1","1");
		}
		@Override
		public void process(HttpResponse response, HttpContext context) throws HttpException,IOException {
			response.setHeader("B1","1");
		}
	}

	@Test
	public void c01_httpClient_interceptors() throws Exception {
		HttpRequestInterceptor x1 = new HttpRequestInterceptor() {
			@Override public void process(HttpRequest request, HttpContext context) throws HttpException,IOException {
				request.setHeader("A1","1");
			}
		};
		HttpResponseInterceptor x2 = new HttpResponseInterceptor() {
			@Override public void process(HttpResponse response, HttpContext context) throws HttpException,IOException {
				response.setHeader("B1","1");
			}
		};
		HttpRequestInterceptor x3 = new HttpRequestInterceptor() {
			@Override public void process(HttpRequest request, HttpContext context) throws HttpException,IOException {
				request.setHeader("A2","2");
			}
		};
		HttpResponseInterceptor x4 = new HttpResponseInterceptor() {
			@Override public void process(HttpResponse response, HttpContext context) throws HttpException,IOException {
				response.setHeader("B2","2");
			}
		};

		client().addInterceptorFirst(x1).addInterceptorLast(x2).addInterceptorFirst(x3).addInterceptorLast(x4)
			.build().get("/echo").run().assertContent().isContains("A1: 1","A2: 2").assertHeader("B1").is("1").assertHeader("B2").is("2");
		client().interceptors(C01.class).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
		client().interceptors(new C01()).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
	}

	@Test
	public void c02_httpClient_httpProcessor() throws RestCallException {
		HttpProcessor x = new HttpProcessor() {
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				request.setHeader("A1","1");
			}
			@Override
			public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				response.setHeader("B1","1");
			}
		};
		client().httpProcessor(x).build().get("/echo").run().assertContent().isContains("A1: 1").assertHeader("B1").is("1");
	}

	@Test
	public void c03_httpClient_requestExecutor() throws RestCallException {
		AtomicBoolean b1 = new AtomicBoolean();
		HttpRequestExecutor x = new HttpRequestExecutor() {
			@Override
			public HttpResponse execute(HttpRequest request, HttpClientConnection conn, HttpContext context) throws HttpException, IOException {
				b1.set(true);
				return super.execute(request, conn, context);
			}
		};
		client().requestExecutor(x).build().get("/echo").run().assertContent().isContains("GET /echo HTTP/1.1");
		assertTrue(b1.get());
	}

	@Test
	public void c04_httpClient_defaultHeaders() throws RestCallException {
		client().headersDefault(stringHeader("Foo","bar")).build().get("/echo").run().assertContent().isContains("GET /echo HTTP/1.1","Foo: bar");
	}

	@Test
	public void c05_httpClient_httpClientBuilderMethods() {
		RestClient.create().disableRedirectHandling().redirectStrategy(DefaultRedirectStrategy.INSTANCE).defaultCookieSpecRegistry(null).sslHostnameVerifier(null).publicSuffixMatcher(null).sslContext(null).sslSocketFactory(null).maxConnTotal(10).maxConnPerRoute(10).defaultSocketConfig(null).defaultConnectionConfig(null).connectionTimeToLive(100,TimeUnit.DAYS).connectionManager(null).connectionManagerShared(true).connectionReuseStrategy(null).keepAliveStrategy(null).targetAuthenticationStrategy(null).proxyAuthenticationStrategy(null).userTokenHandler(null).disableConnectionState().schemePortResolver(null).disableCookieManagement().disableContentCompression().disableAuthCaching().retryHandler(null).disableAutomaticRetries().proxy(null).routePlanner(null).connectionBackoffStrategy(null).backoffManager(null).serviceUnavailableRetryStrategy(null).defaultCookieStore(null).defaultCredentialsProvider(null).defaultAuthSchemeRegistry(null).contentDecoderRegistry(null).defaultRequestConfig(null).useSystemProperties().evictExpiredConnections().evictIdleConnections(1,TimeUnit.DAYS);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void c06_httpClient_unusedHttpClientMethods() {
		RestClient x = RestClient.create().build();
		assertThrown(()->x.getParams()).isType(UnsupportedOperationException.class);
		assertNotNull(x.getConnectionManager());
	}

	@Test
	public void c07_httpClient_executeHttpUriRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json5");
		HttpResponse res = MockRestClient.create(A.class).build().execute(x);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c08_httpClient_executeHttpHostHttpRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		x.addHeader("Accept","text/json5");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target,x);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c09_httpClient_executeHttpHostHttpRequestHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		HttpContext context = new BasicHttpContext();
		x.addHeader("Accept","text/json5");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target,x,context);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c10_httpClient_executeResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json5");
		String res = MockRestClient.create(A.class).build().execute(x,new BasicResponseHandler());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c11_httpClient_executeHttpUriRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json5");
		String res = MockRestClient.create(A.class).build().execute(x,new BasicResponseHandler(),new BasicHttpContext());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c12_httpClient_executeHttpHostHttpRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json5");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicResponseHandler(),new BasicHttpContext());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c13_httpClient_executeHttpHostHttpRequestResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json5");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicResponseHandler());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c14_httpClient_requestConfig() throws Exception {
		RestRequest req = client().build().get("/bean").config(RequestConfig.custom().setMaxRedirects(1).build());
		req.run().assertContent("{f:1}");
		assertEquals(1, req.getConfig().getMaxRedirects());
	}

	@Test
	public void c15_httpClient_pooled() throws Exception {
		RestClient x1 = RestClient.create().json5().pooled().build();
		RestClient x2 = RestClient.create().json5().build();
		RestClient x3 = client().pooled().build();
		assertEquals("PoolingHttpClientConnectionManager",ClassInfo.of(x1.httpClient).getDeclaredField(x -> x.hasName("connManager")).accessible().get(x1.httpClient).getClass().getSimpleName());
		assertEquals("BasicHttpClientConnectionManager",ClassInfo.of(x2.httpClient).getDeclaredField(x -> x.hasName("connManager")).accessible().get(x2.httpClient).getClass().getSimpleName());
		assertEquals("MockHttpClientConnectionManager",ClassInfo.of(x3.httpClient).getDeclaredField(x -> x.hasName("connManager")).accessible().get(x3.httpClient).getClass().getSimpleName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Authentication
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestObject {
		@RestGet
		public String echo(@org.apache.juneau.http.annotation.Header("Authorization") String auth, org.apache.juneau.rest.RestResponse res) throws IOException {
			if (auth == null) {
				throw unauthorized().setHeader2("WWW-Authenticate","BASIC realm=\"foo\"");
			} else {
				assertEquals("Basic dXNlcjpwdw==",auth);
				return "OK";
			}
		}
	}

	@Test
	public void d01_basicAuth() throws RestCallException {
		client(D.class).basicAuth(AuthScope.ANY_HOST,AuthScope.ANY_PORT,"user","pw").build().get("/echo").run().assertContent().isContains("OK");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_other_completeFuture() throws Exception {
		client().build().get("/bean").completeFuture().get().assertStatus(200);
	}

	public static class E2 implements Cancellable {
		@Override
		public boolean cancel() {
			return false;
		}
	}

	@Test
	public void e02_httpRequestBase_setCancellable() throws Exception {
		client().build().get("/bean").cancellable(new E2()).run().assertStatus(200);
	}

	@Test
	public void e03_httpRequestBase_protocolVersion() throws Exception {
		client().build().get("/bean").protocolVersion(new ProtocolVersion("http", 2, 0)).run().assertStatus(200);
		ProtocolVersion x = client().build().get("/bean").protocolVersion(new ProtocolVersion("http", 2, 0)).getProtocolVersion();
		assertEquals(2,x.getMajor());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void e04_httpRequestBase_completed() throws Exception {
		client().build().get("/bean").completed().run().assertStatus(200);
	}

	@Test
	public void e05_httpUriRequest_abort() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.abort();
		assertTrue(x.isAborted());
	}

	@Test
	public void e06_httpMessage_getRequestLine() throws Exception {
		RestRequest x = client().build().get("/bean");
		assertEquals("GET",x.getRequestLine().getMethod());
	}

	@Test
	public void e07_httpMessage_containsHeader() throws Exception {
		RestRequest x = client().build().get("/bean").header("Foo", "bar");
		assertTrue(x.containsHeader("Foo"));
	}

	@Test
	public void e08_httpMessage_getFirstHeader_getLastHeader() throws Exception {
		RestRequest x = client().build().get("/bean").header("Foo","bar").header("Foo","baz");
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
	}

	@Test
	public void e09_httpMessage_addHeader() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.addHeader(header("Foo","bar"));
		x.addHeader("Foo","baz");
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
	}

	@Test
	public void e10_httpMessage_setHeader() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.setHeader(header("Foo","bar"));
		x.setHeader(header("Foo","baz"));
		assertEquals("baz",x.getFirstHeader("Foo").getValue());
		assertEquals("baz",x.getLastHeader("Foo").getValue());
		x.setHeader("Foo","qux");
		assertEquals("qux",x.getFirstHeader("Foo").getValue());
		assertEquals("qux",x.getLastHeader("Foo").getValue());
	}

	@Test
	public void e11_httpMessage_setHeaders() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.setHeaders(new Header[]{header("Foo","bar")});
		assertEquals("bar",x.getFirstHeader("Foo").getValue());
	}

	@Test
	public void e12_httpMessage_removeHeaders() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.setHeaders(new Header[]{header("Foo","bar")});
		x.removeHeaders("Foo");
		assertNull(x.getFirstHeader("Foo"));
	}

	@Test
	public void e13_httpMessage_removeHeader() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.setHeaders(new Header[]{header("Foo","bar")});
		x.removeHeader(header("Foo","bar"));
		//assertNull(x.getFirstHeader("Foo"));  // Bug in HttpClient API?
	}

	@Test
	public void e14_httpMessage_headerIterator() throws Exception {
		RestRequest x = client().build().get("/bean");
		x.setHeaders(new Header[]{header("Foo","bar")});
		assertEquals("Foo: bar", x.headerIterator().next().toString());
		assertEquals("Foo: bar", x.headerIterator("Foo").next().toString());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void e15_httpMessage_getParams() throws Exception {
		HttpParams p = new BasicHttpParams();
		RestRequest x = client().build().get("/bean");
		x.setParams(p);
		assertEquals(p, x.getParams());
	}

	@Test
	public void e16_toMap() throws Exception {
		assertNotNull(client().build().toString());
		assertNotNull(client().build().get("/bean").toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}

	private static RestClient.Builder client(Class<?> c) {
		return MockRestClient.create(c).noTrace().json5();
	}

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}
}
