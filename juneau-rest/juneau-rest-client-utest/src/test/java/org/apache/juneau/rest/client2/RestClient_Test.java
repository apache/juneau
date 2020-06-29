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
import static org.apache.juneau.rest.client2.RestClient.*;
import static org.apache.juneau.assertions.Assertions.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestRequest;
import org.apache.juneau.rest.mock2.*;
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
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestMethod(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Override client and builder.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_basic_overrideHttpClient() {
		HttpClientBuilder cb = HttpClientBuilder.create();
		CloseableHttpClient hc = HttpClientBuilder.create().build();
		RestClient.create().httpClientBuilder(cb).build().builder().build();
		RestClient.create().httpClient(hc).build().builder().build();
	}

	public static class A2 extends RestClientBuilder {}

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
		RestClient.create().set(RESTCLIENT_httpClient,null).keepHttpClientOpen().build().close();

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
		assertThrown(()->{rc.request(HttpMethod.GET,"/bean",null);}).contains("RestClient.close() has already been called");
	}

	@Test
	public void a05_request_whenClosed_withStackCreation() throws Exception {
		RestClient rc = client().debug().build();
		rc.closeQuietly();
		assertThrown(()->{rc.request(HttpMethod.GET,"/bean",null);}).contains("RestClient.close() has already been called");
	}


	//------------------------------------------------------------------------------------------------------------------
	// Logging
	//------------------------------------------------------------------------------------------------------------------

	public static class B4 extends RestClient {
		private static boolean METHOD_CALLED;
		public B4(PropertyStore ps) {
			super(ps);
		}
		@Override
		protected RestRequest createRequest(java.net.URI uri, String method, boolean hasBody) throws RestCallException {
			METHOD_CALLED = true;
			return super.createRequest(uri, method, hasBody);
		}
	}

	@Test
	public void b04_restClient_overrideCreateRequest() throws Exception {
		RestClient.create().simpleJson().build(B4.class).get("foo");
		assertTrue(B4.METHOD_CALLED);
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
			.build().get("/echo").run().assertBody().contains("A1: 1","A2: 2").assertHeader("B1").is("1").assertHeader("B2").is("2");
		client().interceptors(C01.class).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
		client().interceptors(new C01()).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
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
		client().httpProcessor(x).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
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
		client().requestExecutor(x).build().get("/echo").run().assertBody().contains("HTTP GET /echo");
		assertTrue(b1.get());
	}

	@Test
	public void c04_httpClient_defaultHeaders() throws RestCallException {
		client().defaultHeaders(AList.of(new org.apache.http.message.BasicHeader("Foo","bar"))).build().get("/echo").run().assertBody().contains("HTTP GET /echo","Foo: bar");
	}

	@Test
	public void c05_httpClient_httpClientBuilderMethods() {
		RestClient.create().disableRedirectHandling().redirectStrategy(DefaultRedirectStrategy.INSTANCE).defaultCookieSpecRegistry(null).sslHostnameVerifier(null).publicSuffixMatcher(null).sslContext(null).sslSocketFactory(null).maxConnTotal(10).maxConnPerRoute(10).defaultSocketConfig(null).defaultConnectionConfig(null).connectionTimeToLive(100,TimeUnit.DAYS).connectionManager(null).connectionManagerShared(true).connectionReuseStrategy(null).keepAliveStrategy(null).targetAuthenticationStrategy(null).proxyAuthenticationStrategy(null).userTokenHandler(null).disableConnectionState().schemePortResolver(null).userAgent("foo").disableCookieManagement().disableContentCompression().disableAuthCaching().retryHandler(null).disableAutomaticRetries().proxy(null).routePlanner(null).connectionBackoffStrategy(null).backoffManager(null).serviceUnavailableRetryStrategy(null).defaultCookieStore(null).defaultCredentialsProvider(null).defaultAuthSchemeRegistry(null).contentDecoderRegistry(null).defaultRequestConfig(null).useSystemProperties().evictExpiredConnections().evictIdleConnections(1,TimeUnit.DAYS);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void c06_httpClient_unusedHttpClientMethods() {
		RestClient x = RestClient.create().build();
		assertThrown(()->{x.getParams();}).isType(UnsupportedOperationException.class);
		assertNotNull(x.getConnectionManager());
	}

	@Test
	public void c07_httpClient_executeHttpUriRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(x);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c08_httpClient_executeHttpHostHttpRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		x.addHeader("Accept","text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target,x);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c09_httpClient_executeHttpHostHttpRequestHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		HttpContext context = new BasicHttpContext();
		x.addHeader("Accept","text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target,x,context);
		assertEquals("{f:1}",IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c10_httpClient_executeResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(x,new BasicResponseHandler());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c11_httpClient_executeHttpUriRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(x,new BasicResponseHandler(),new BasicHttpContext());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c12_httpClient_executeHttpHostHttpRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicResponseHandler(),new BasicHttpContext());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c13_httpClient_executeHttpHostHttpRequestResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept","text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"),x,new BasicResponseHandler());
		assertEquals("{f:1}",res);
	}

	@Test
	public void c14_httpClient_requestConfig() throws Exception {
		client().build().get("/bean").requestConfig(RequestConfig.custom().setMaxRedirects(1).build()).run().assertBody().is("{f:1}");
	}

	@Test
	public void c15_httpClient_pooled() throws Exception {
		RestClient x1 = RestClient.create().simpleJson().pooled().build();
		RestClient x2 = RestClient.create().simpleJson().build();
		RestClient x3 = client().pooled().build();
		assertEquals("PoolingHttpClientConnectionManager",ClassInfo.of(x1.httpClient).getDeclaredField("connManager").accessible().invoke(x1.httpClient).getClass().getSimpleName());
		assertEquals("BasicHttpClientConnectionManager",ClassInfo.of(x2.httpClient).getDeclaredField("connManager").accessible().invoke(x2.httpClient).getClass().getSimpleName());
		assertEquals("MockHttpClientConnectionManager",ClassInfo.of(x3.httpClient).getDeclaredField("connManager").accessible().invoke(x3.httpClient).getClass().getSimpleName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Authentication
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E extends BasicRest {
		@RestMethod
		public String getEcho(@org.apache.juneau.http.annotation.Header("Authorization") String auth, org.apache.juneau.rest.RestResponse res) throws IOException {
			if (auth == null) {
				throw new Unauthorized().header("WWW-Authenticate","BASIC realm=\"foo\"");
			} else {
				assertEquals("Basic dXNlcjpwdw==",auth);
				return "OK";
			}
		}
	}

	@Test
	public void d01_basicAuth() throws RestCallException {
		client(E.class).basicAuth(AuthScope.ANY_HOST,AuthScope.ANY_PORT,"user","pw").build().get("/echo").run().assertBody().contains("OK");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}

	private static RestClientBuilder client(Class<?> c) {
		return MockRestClient.create(c).simpleJson();
	}
}
