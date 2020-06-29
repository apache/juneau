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
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.annotation.URI;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.BasicNameValuePair;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestRequest;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;
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
		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	public static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean postBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean putBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean patchBean(@Body ABean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public ABean deleteBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean optionsBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public ABean headBean() {
			return bean;
		}
		@RestMethod(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
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
		@RestMethod(path="/checkHeader")
		public String[] postHeader(org.apache.juneau.rest.RestRequest req) {
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
		@RestMethod(path="/",name="*")
		public Reader echoMethod(@Method String method) {
			return new StringReader(method);
		}
	}

	private static final Calendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR.set(2000,11,31,12,34,56);
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
	public void a04_basicCalls() throws Exception {
		RestClient x = client().build();
		x.get().run().assertBody().is("GET");
		x.get("/").run().assertBody().is("GET");
		x.get("").run().assertBody().is("GET");
		x.put("/",null).run().assertBody().is("PUT");
		x.post("/",null).run().assertBody().is("POST");
		x.delete("/").run().assertBody().is("DELETE");
		x.formPost("/").run().assertBody().is("POST");
	}

	@Test
	public void a05_basicCalls_get() throws Exception {
		client().build().get("/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a06_basicCalls_get_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().get(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a07_basicCalls_put() throws Exception {
		client().build().put("/bean",bean).run().assertBody().is("{f:1}");
		client().build().put("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a08_basicCalls_put_fromString() throws Exception {
		client().build().put("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
		client().build().put("/bean").bodyString("{f:1}").simpleJson().run().assertBody().is("{f:1}");
		client().build().put("/bean").bodyString("").simpleJson().run().assertBody().is("{f:0}");
		client().build().put("/bean").bodyString(null).simpleJson().run().assertBody().is("null");
 	}

	@Test
	public void a09_basicCalls_put_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().put(url,bean).run().assertBody().is("{f:1}");
			client().build().put(url,"{f:1}","application/json").run().assertBody().is("{f:1}");
			client().build().put(url).body(bean).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a10_basicCalls_put_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().contents("{f:1}").build(),
			StreamResource.create().contents("{f:1}").build(),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").build().put("/bean",body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a11_basicCalls_post() throws Exception {
		client().build().post("/bean",bean).run().assertBody().is("{f:1}");
		client().build().post("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a12_basicCalls_post_fromString() throws Exception {
		client().build().post("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
	}

	@Test
	public void a13_basicCalls_post_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().post(url,bean).run().assertBody().is("{f:1}");
			client().build().post(url,"{f:1}","application/json").run().assertBody().is("{f:1}");
			client().build().post(url).body(bean).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a14_basicCalls_post_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().contents("{f:1}").build(),
			StreamResource.create().contents("{f:1}").build(),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").build().post("/bean",body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a15_basicCalls_delete() throws Exception {
		client().build().delete("/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a16_basicCalls_delete_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().delete(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a17_basicCalls_options() throws Exception {
		client().build().options("/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a18_basicCalls_options_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().options(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a19_basicCalls_head() throws Exception {
		client().build().head("/bean").run().assertBody().is("");
	}

	@Test
	public void a20_basicCalls_head_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client().build().head(url).run().assertBody().is("");
		}
	}

	@Test
	public void a21_basicCalls_formPost() throws Exception {
		client().build().formPost("/bean",bean).accept("application/json+simple").run().assertBody().is("{f:1}");
	}

	@Test
	public void a22_basicCalls_formPost_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			client(A.class).build().formPost(url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a23_basicCalls_formPost_exhaustiveBodyTypes() throws Exception {
		Supplier<Object>
			s1 = () -> new StringReader("f=1"),
			s2 = () -> new ByteArrayInputStream("f=1".getBytes());
		List<Object> bodies = AList.of(
			/*[ 0]*/ bean,
			/*[ 1]*/ pairs("f","1"),
			/*[ 2]*/ new NameValuePair[]{pair("f","1")},
			/*[ 3]*/ new StringEntity("f=1",org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED),
			/*[ 4]*/ new StringEntity("f=1",(org.apache.http.entity.ContentType)null),
			/*[ 5]*/ pair("f","1"),
			/*[ 6]*/ ReaderResource.create().contents("f=1").build(),
			/*[ 7]*/ ReaderResource.create().contents("f=1"),
			/*[ 8]*/ ReaderResource.create().contents("f=1").mediaType("application/x-www-form-urlencoded").build(),
			/*[ 9]*/ ReaderResource.create().contents("f=1").mediaType("application/x-www-form-urlencoded"),
			/*[10]*/ StreamResource.create().contents("f=1").build(),
			/*[11]*/ StreamResource.create().contents("f=1"),
			/*[12]*/ StreamResource.create().contents("f=1").mediaType("application/x-www-form-urlencoded").build(),
			/*[13]*/ StreamResource.create().contents("f=1").mediaType("application/x-www-form-urlencoded"),
			/*[14]*/ s1,
			/*[15]*/ s2
		);
		for (int i = 0; i < bodies.size(); i++) {
			MockRestClient.create(A.class).header("Check","Content-Type").accept("application/json+simple").build().formPost("/checkHeader",bodies.get(i)).run().assertBody().msg("Body {0} failed",i).matchesSimple("['application/x-www-form-urlencoded*']");
			MockRestClient.create(A.class).build().formPost("/bean",bodies.get(i)).accept("application/json+simple").run().assertBody().msg("Body {0} failed","#"+i).is("{f:1}");
		}
	}

	@Test
	public void a24_basicCalls_formPostPairs() throws Exception {
		MockRestClient.create(A.class).build().formPostPairs("/bean",new StringBuilder("f"),new StringBuilder("1")).accept("application/json+simple").run().assertBody().is("{f:1}");
	}

	@Test
	public void a25_basicCalls_patch() throws Exception {
		client().build().patch("/bean",bean).run().assertBody().is("{f:1}");
		client().build().patch("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a26_basicCalls_patch_fromString() throws Exception {
		client().build().patch("/bean","{f:1}","application/json").run().assertBody().is("{f:1}");
	}

	@Test
	public void a27_basicCalls_patch_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().contents("{f:1}").build(),
			StreamResource.create().contents("{f:1}").build(),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().build().patch("/bean",body).contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a28_basicCalls_patch_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			MockRestClient.create(A.class).build().patch(url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a29_basicCalls_request_patch() throws Exception {
		client().build().request(HttpMethod.PATCH,"/bean",bean).run().assertBody().is("{f:1}");
		client().build().request(HttpMethod.PATCH,"/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a30_basicCalls_request_patch_exhaustiveBodyTypes() throws Exception {
		List<Object> bodies = AList.<Object>of(
			new StringReader("{f:1}"),
			new ByteArrayInputStream("{f:1}".getBytes()),
			ReaderResource.create().contents("{f:1}").build(),
			StreamResource.create().contents("{f:1}").build(),
			bean,
			new StringEntity("{f:1}"),
			pairs("f",1)
		);
		for (Object body : bodies) {
			client().build().request(HttpMethod.PATCH,"/bean",body).contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a31_basicCalls_request_patch_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			MockRestClient.create(A.class).build().request(HttpMethod.PATCH,url,bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a32_basicCalls_request_get() throws Exception {
		client().build().request(HttpMethod.GET,"/bean",null).run().assertBody().is("{f:1}");
		client().build().request(HttpMethod.GET,"/bean").run().assertBody().is("{f:1}");
	}

	@Test
	public void a33_basicCalls_request_get_exhaustiveUrls() throws Exception {
		List<Object> urls = AList.<Object>of(
			new URIBuilder("http://localhost/bean"),
			java.net.URI.create("http://localhost/bean"),
			new URL("http://localhost/bean"),
			"/bean",
			new StringBuilder("/bean")
		);
		for (Object url : urls) {
			MockRestClient.create(A.class).build().request(HttpMethod.GET,url).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a34_basicCalls_request_whenClosed() throws Exception {
		RestClient rc = client().build();
		rc.closeQuietly();
		assertThrown(()->{rc.request(HttpMethod.GET,"/bean",null);}).contains("RestClient.close() has already been called");
	}

	@Test
	public void a35_basicCalls_request_whenClosed_withStackCreation() throws Exception {
		RestClient rc = client().debug().build();
		rc.closeQuietly();
		assertThrown(()->{rc.request(HttpMethod.GET,"/bean",null);}).contains("RestClient.close() has already been called");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_logging_logToConsole() throws Exception {
		MockConsole c = MockConsole.create();
		MockLogger l = MockLogger.create();

		client().logRequests(DetailLevel.NONE,Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().is("");
		c.reset();

		client().logRequests(DetailLevel.SIMPLE,Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().is("HTTP POST http://localhost/bean, HTTP/1.1 200 \n");
		c.reset();

		client().logRequests(DetailLevel.FULL,Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean",bean).complete();
		c.assertContents().is(
			"",
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json+simple",
			"---request entity---",
			"	Content-Type: application/json+simple",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json",
			"---response content---",
			"{f:1}",
			"=== END =======================================================================",
			""
		);

		client().logRequests(DetailLevel.NONE,Level.SEVERE).logToConsole().logger(l).console(MockConsole.class).build().post("/bean",bean).complete();
	}

	@Test
	public void b02_logging_logTo() throws Exception {
		MockLogger l = MockLogger.create();

		client().logRequests(DetailLevel.NONE,Level.SEVERE).logToConsole().logger(l).build().post("/bean",bean).complete();
		l.assertContents().is("");
		l.assertRecordCount().is(0);
		l.reset();

		client().logger(l).logRequests(DetailLevel.SIMPLE,Level.WARNING).build().post("/bean",bean).complete();
		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().stderr().is("HTTP POST http://localhost/bean, HTTP/1.1 200 ");
		l.assertContents().is("WARNING: HTTP POST http://localhost/bean, HTTP/1.1 200 \n");
		l.reset();

		client().logger(l).logRequests(DetailLevel.FULL,Level.WARNING).build().post("/bean",bean).complete();
		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().is(
			"",
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json+simple",
			"---request entity---",
			"	Content-Type: application/json+simple",
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
		l.assertContents().stderr().javaStrings().is(
			"WARNING: ",
			"=== HTTP Call (outgoing) ======================================================",
			"=== REQUEST ===",
			"POST http://localhost/bean",
			"---request headers---",
			"	Accept: application/json+simple",
			"---request entity---",
			"	Content-Type: application/json+simple",
			"---request content---",
			"{f:1}",
			"=== RESPONSE ===",
			"HTTP/1.1 200 ",
			"---response headers---",
			"	Content-Type: application/json",
			"---response content---",
			"{f:1}",
			"=== END =======================================================================",
			""
		);
	}

	public static class B3 extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			super.onConnect(req,res);
			req.log(Level.WARNING,"Foo");
			req.log(Level.WARNING,new RuntimeException(),"Bar");
			res.log(Level.WARNING,"Baz");
			res.log(Level.WARNING,new RuntimeException(),"Qux");
			req.log(Level.WARNING,(Throwable)null,"Quux");
		}
	}

	@Test
	public void b03_logging_other() throws Exception {
		MockLogger ml = MockLogger.create();
		MockConsole mc = MockConsole.create();
		client().logger(ml).interceptors(B3.class).build().post("/bean",bean).complete();
		ml.assertRecordCount().is(5);
		ml.reset();
		client().logger(ml).logToConsole().console(mc).interceptors(B3.class).build().post("/bean",bean).complete();
		ml.assertRecordCount().is(5);
		ml.assertContents().contains(
			"WARNING: Foo",
			"WARNING: Bar",
			"WARNING: Baz",
			"WARNING: Qux",
			"WARNING: Quux",
			"at org.apache.juneau"
		);
	}

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
	public void e01_basicAuth() throws RestCallException {
		client(E.class).basicAuth(AuthScope.ANY_HOST,AuthScope.ANY_PORT,"user","pw").build().get("/echo").run().assertBody().contains("OK");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RestClient properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class K1 extends BasicRestCallHandler {
		public K1(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
			request.addHeader("Check","Foo");
			request.addHeader("Foo","baz");
			return super.execute(target,request,context);
		}
	}

	@Test
	public void k01_restClient_CallHandler() throws Exception {
		RestCallHandler x = new RestCallHandler() {
			@Override
			public HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
				return null;
			}
			@Override
			public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
				return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http",1,1),201,null));
			}
		};
		client().callHandler(K1.class).header("Foo","f1").build().get("/checkHeader").header("Foo","f2").run().assertBody().is("['f1','f2','baz']");
		client().callHandler(x).header("Foo","f1").build().get("/checkHeader").header("Foo","f2").run().assertStatus().is(201);
	}

	@Test
	public void k02_restClient_errorCodes() throws Exception {
		RestClient x1 = client().errorCodes(x -> x == 200).ignoreErrors(false).build();
		RestClient x2 = client().ignoreErrors(false).build();
		assertThrown(()->{x1.get("/echo").run();}).passes(x -> ((RestCallException)x).getResponseCode() == 200);
		assertThrown(()->{x2.get("/echo").errorCodes(x -> x == 200).run();}).passes(x -> ((RestCallException)x).getResponseCode() == 200);
	}

	@Test
	public void k03_restClient_executorService() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(10));
		RestClient x1 = client().executorService(es,true).build();

		assertEquals(es,x1.getExecutorService());
		x1.get("/echo").runFuture().get().assertStatus().is(200).assertBody().contains("HTTP GET /echo");

		es = null;
		RestClient x2 = client().executorService(es,true).build();
		assertNotNull(x2.getExecutorService());
		x2.get("/echo").runFuture().get().assertStatus().is(200).assertBody().contains("HTTP GET /echo");
	}

	@Test
	public void k04_restClient_keepHttpClientOpen() throws Exception {
		RestClient x = client().keepHttpClientOpen().build();

		CloseableHttpClient c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");

		x = client().keepHttpClientOpen().build();
		c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");
	}

	public static class K5 extends BasicRestCallInterceptor {
		public static int x;
		@Override
		public void onInit(RestRequest req) throws Exception {
			x = 1;
			req.header("Foo","f2");
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			x += 10;
			res.addHeader("Bar","b1");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			x += 100;
		}
	}

	public static class K5a extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	public static class K5b extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
		}
	}

	public static class K5c extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	@Test
	public void k05_restClient_interceptors() throws Exception {
		client().header("Foo","f1").interceptors(K5.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,K5.x);

		client().header("Foo","f1").interceptors(new K5()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,K5.x);

		client().header("Foo","f1").build().get("/checkHeader").interceptors(new K5()).header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,K5.x);
		assertThrown(()->{client().header("Foo","f1").interceptors(K5a.class).build().get("/checkHeader");}).is("foo");
		assertThrown(()->{client().header("Foo","f1").interceptors(K5b.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run();}).is("foo");
		assertThrown(()->{client().header("Foo","f1").interceptors(K5c.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close();}).is("foo");
		assertThrown(()->{client().header("Foo","f1").interceptors(new K5a()).build().get("/checkHeader");}).is("foo");
		assertThrown(()->{client().header("Foo","f1").interceptors(new K5b()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run();}).is("foo");
		assertThrown(()->{client().header("Foo","f1").interceptors(new K5c()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close();}).is("foo");
		assertThrown(()->{client().header("Foo","f1").build().get("/checkHeader").interceptors(new K5a());}).is("foo");
		assertThrown(()->{client().header("Foo","f1").build().get("/checkHeader").interceptors(new K5b()).header("Check","foo").header("Foo","f3").run();}).is("foo");
		assertThrown(()->{client().header("Foo","f1").build().get("/checkHeader").interceptors(new K5c()).header("Check","foo").header("Foo","f3").run().close();}).is("foo");
		assertThrown(()->{client().interceptors(String.class);}).is("Invalid class of type 'java.lang.String' passed to interceptors().");
		assertThrown(()->{client().interceptors("");}).is("Invalid object of type 'java.lang.String' passed to interceptors().");
		client().interceptors((Object)null).header("Foo","f1").build().get("/checkHeader");
		client().interceptors((Class<?>)null).header("Foo","f1").build().get("/checkHeader");
	}

	public static class K6a extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onInit(RestRequest req) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class K6b extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onConnect(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class K6c extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onClose(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}

	@Test
	public void k06_restClient_interceptors_exceptionHandling() throws Exception {
		assertThrown(()->{client().interceptors(K6a.class).build().post("/bean",bean).complete();}).is("foo");
		assertThrown(()->{client().interceptors(K6b.class).build().post("/bean",bean).complete();}).is("foo");
		assertThrown(()->{client().interceptors(K6c.class).build().post("/bean",bean).complete();}).is("foo");
	}

	public static class K7 extends RestClient {
		private static String lastMessage;
		public K7(PropertyStore ps) {
			super(ps);
		}
		@Override
		public void log(Level level,String msg,Object...args) {
			lastMessage = msg;
		}
	}

	@Test
	public void k07_restClient_leakDetection() throws Throwable {
		client().leakDetection().build(K7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.",K7.lastMessage);

		client().debug().build(K7.class).finalize();
		assertTrue(K7.lastMessage.startsWith("WARNING:  RestClient garbage collected before it was finalized.\nCreation Stack:\n\t"));

		client().leakDetection().build(K7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.",K7.lastMessage);
	}

	@Test
	public void k08_restClient_marshall() throws Exception {
		RestClient rc = MockRestClient.create(A.class).marshall(Xml.DEFAULT).build();
		ABean b = rc.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
	}

	@Test
	public void k09_restClient_marshalls() throws Exception {
		RestClient x = MockRestClient.create(A.class).marshalls(Xml.DEFAULT,Json.DEFAULT).build();

		x.post("/echoBody",bean).run().assertBody().is("{f:1}");

		ABean b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
	}

	@Test
	public void k10_restClient_serializer_parser() throws Exception {
		RestClient x = MockRestClient.create(A.class).serializer(XmlSerializer.class).parser(XmlParser.class).build();

		ABean b = x.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		x = MockRestClient.create(A.class).serializer(XmlSerializer.DEFAULT).parser(XmlParser.DEFAULT).build();
		b = x.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
		assertThrown(()->{MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers,String.class).build();}).contains("RESTCLIENT_serializers property had invalid class of type 'java.lang.String'");
		assertThrown(()->{MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers,"").build();}).contains("RESTCLIENT_serializers property had invalid object of type 'java.lang.String'");
		assertThrown(()->{MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers,String.class).build();}).contains("RESTCLIENT_parsers property had invalid class of type 'java.lang.String'");
		assertThrown(()->{MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers,"").build();}).contains("RESTCLIENT_parsers property had invalid object of type 'java.lang.String'");
	}

	@Test
	public void k11_restClient_serializers_parsers() throws Exception {
		@SuppressWarnings("unchecked")
		RestClient x = MockRestClient.create(A.class).serializers(XmlSerializer.class,JsonSerializer.class).parsers(XmlParser.class,JsonParser.class).build();

		x.post("/echoBody",bean).run().assertBody().is("{f:1}");
		ABean b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		x = MockRestClient.create(A.class).serializers(XmlSerializer.DEFAULT,JsonSerializer.DEFAULT).parsers(XmlParser.DEFAULT,JsonParser.DEFAULT).build();
		x.post("/echoBody",bean).run().assertBody().is("{f:1}");
		b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
	}

	@Rest(partSerializer=K12a.class,partParser=K12b.class)
	public static class K12 extends BasicRest {
		@RestMethod(path="/")
		public Ok get(@Header(name="Foo",multi=true) ABean[] foo,org.apache.juneau.rest.RestRequest req,org.apache.juneau.rest.RestResponse res) throws Exception {
			assertEquals(2,foo.length);
			assertObject(req.getHeaders().getAll("Foo",String[].class)).json().is("['x{f:1}','x{f:1}']");
			assertEquals("{f:1}",foo[0].toString());
			assertEquals("{f:1}",foo[1].toString());
			res.header("Foo",bean);
			return Ok.OK;
		}
	}

	public static class K12a extends SimplePartSerializer {
		@Override
		public SimplePartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new SimplePartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) {
					if (value instanceof ABean)
						return "x" + SimpleJson.DEFAULT.toString(value);
					return "x" + super.serialize(type,schema,value);
				}
			};
		}
	}

	public static class K12b extends SimplePartParser {
		@Override
		public SimplePartParserSession createPartSession(ParserSessionArgs args) {
			return new SimplePartParserSession() {
				@Override
				public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
					if (toType.isInstanceOf(ABean.class))
						return SimpleJson.DEFAULT.read(in.substring(1),toType);
					return super.parse(null,schema,in,toType);
				}
			};
		}
	}

	@Test
	public void k12_restClient_partSerializer_partParser() throws Exception {
		RestClient x = client(K12.class).header("Foo",bean).partSerializer(K12a.class).partParser(K12b.class).build();
		ABean b = x.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());
		b = x.get().header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());

		x = client(K12.class).header("Foo",bean).partSerializer(new K12a()).partParser(new K12b()).build();
		b = x.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());
	}


	@Test
	public void k13_restClient_toString() throws Exception {
		String s = client().rootUrl("foo").build().toString();
		assertTrue(s.contains("rootUri: 'foo'"));
	}

	@Test
	public void k14_restClient_request_target() throws Exception {
		client().build().get("/bean").target(new HttpHost("localhost")).run().assertBody().is("{f:1}");
	}

	@Test
	public void k15_restClient_request_context() throws Exception {
		client().build().get("/bean").context(new BasicHttpContext()).run().assertBody().is("{f:1}");
	}

	@Test
	public void k16_restClient_request_uriParts() throws Exception {
		java.net.URI uri = client().build().get().scheme("http").host("localhost").port(8080).userInfo("foo:bar").uri("/bean").fragment("baz").query("foo","bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().scheme("http").host("localhost").port(8080).userInfo("foo","bar").uri("/bean").fragment("baz").query("foo","bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri("http://localhost").uri("http://foo:bar@localhost:8080/bean?foo=bar#baz").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri(new java.net.URI(null,null,null,null)).uri(new java.net.URI("http://foo:bar@localhost:8080/bean?foo=bar#baz")).run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class L1 {
		public Object f1;
		static L1 get() {
			L1 x = new L1();
			x.f1 = L2.get();
			return x;
		}
	}

	@Test
	public void l01_serializer_addBeanTypes() throws Exception {
		L1 l1 = L1.get();
		client().addBeanTypes().build().post("/echoBody",l1).run().assertBody().is("{f1:{_type:'L',f2:1}}");
	}

	@org.apache.juneau.annotation.Bean(typeName="L")
	public static class L2 {
		public int f2;
		static L2 get() {
			L2 x = new L2();
			x.f2 = 1;
			return x;
		}
	}

	@Test
	public void l02_serializer_addRootType() throws Exception {
		L2 l2 = L2.get();
		client().addBeanTypes().addRootType().build().post("/echoBody",l2).run().assertBody().is("{_type:'L',f2:1}");
	}

	@Test
	public void l03_serializer_detectRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;
		assertThrown(()->{client().detectRecursions().build().post("/echoBody",l1).run();}).contains("Recursion occurred");
	}

	@Test
	public void l04_serializer_ignoreRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;
		client().ignoreRecursions().build().post("/echoBody",l1).run().assertBody().is("{}");
	}

	@Test
	public void l05_serializer_initialDepth() throws Exception {
		client().initialDepth(2).ws().build().post("/echoBody",bean).run().assertBody().is("\t\t{\n\t\t\tf: 1\n\t\t}");
	}

	public static class L6 {
		public ABean f;
		static L6 get() {
			L6 x = new L6();
			x.f = bean;
			return x;
		}
	}

	@Test
	public void l06_serializer_maxDepth() throws Exception {
		client().maxDepth(1).build().post("/echoBody",L6.get()).run().assertBody().is("{}");
	}

	@Test
	public void l07_serializer_sortCollections() throws Exception {
		String[] x = new String[]{"c","a","b"};
		client().sortCollections().build().post("/echoBody",x).run().assertBody().is("['a','b','c']");
	}

	@Test
	public void l08_serializer_sortMapsBoolean() throws Exception {
		AMap<String,Integer> x = AMap.of("c",3,"a",1,"b",2);
		client().sortMaps().build().post("/echoBody",x).run().assertBody().is("{a:1,b:2,c:3}");
	}

	public static class L9 {
		public List<String> f1 = AList.of();
		public String[] f2 = new String[0];
	}

	@Test
	public void l09_serializer_trimEmptyCollections() throws Exception {
		L9 x = new L9();
		client().trimEmptyCollections().build().post("/echoBody",x).run().assertBody().is("{}");
	}

	public static class L10 {
		public Map<String,String> f1 = AMap.of();
		public OMap f2 = OMap.of();
	}

	@Test
	public void l10_serializer_trimEmptyMaps() throws Exception {
		L10 x = new L10();
		client().trimEmptyMaps().build().post("/echoBody",x).run().assertBody().is("{}");
	}

	public static class L11 {
		public String f;
	}

	@Test
	public void l11_serializer_trimNullPropertiesBoolean() throws Exception {
		L11 x = new L11();
		client().keepNullProperties().build().post("/echoBody",x).run().assertBody().is("{f:null}");
	}

	public static class L12 {
		public String f = " foo ";
	}

	@Test
	public void l12_serializer_trimStringsOnWrite() throws Exception {
		L12 x = new L12();
		client().trimStringsOnWrite().build().post("/echoBody",x).run().assertBody().is("{f:'foo'}");
	}

	public static class L13 {
		@URI
		public String f = "foo";
	}

	@Test
	public void l13_serializer_uriContext_uriResolution_uriRelativity() throws Exception {
		L13 x = new L13();
		client().uriResolution(UriResolution.ABSOLUTE).uriRelativity(UriRelativity.PATH_INFO).uriContext(UriContext.of("http://localhost:80","/context","/resource","/path")).build().post("/echoBody",x).run().assertBody().is("{f:'http://localhost:80/context/resource/foo'}");
		client().uriResolution(UriResolution.NONE).uriRelativity(UriRelativity.RESOURCE).uriContext(UriContext.of("http://localhost:80","/context","/resource","/path")).build().post("/echoBody",x).run().assertBody().is("{f:'foo'}");
	}

	public static class L14 {
		public int f1;
		public L14 f2;

		static L14 get() {
			L14 x = new L14();
			L14 x2 = new L14(),x3 = new L14();
			x.f1 = 1;
			x2.f1 = 2;
			x3.f1 = 3;
			x.f2 = x2;
			x2.f2 = x3;
			return x;
		}
	}

	@Test
	public void l14_serializer_maxIndent() throws Exception {
		L14 x = L14.get();
		client().maxIndent(2).ws().build().post("/echoBody",x).run().assertBody().is("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}");
	}

	public static class L15 {
		public String f1 = "foo";
	}

	@Test
	public void l15_serializer_quoteChar() throws Exception {
		L15 x = new L15();
		MockRestClient.create(A.class).json().quoteChar('\'').build().post("/echoBody",x).run().assertBody().is("{'f1':'foo'}");
		MockRestClient.create(A.class).json().quoteChar('|').build().post("/echoBody",x).run().assertBody().is("{|f1|:|foo|}");
		client().quoteChar('|').build().post("/echoBody",x).run().assertBody().is("{f1:|foo|}");
	}

	@Test
	public void l16_serializer_sq() throws Exception {
		L15 x = new L15();
		MockRestClient.create(A.class).json().sq().build().post("/echoBody",x).run().assertBody().is("{'f1':'foo'}");
		client().sq().build().post("/echoBody",x).run().assertBody().is("{f1:'foo'}");
	}

	@Test
	public void l17_serializer_useWhitespace() throws Exception {
		L15 x = new L15();
		client().ws().build().post("/echoBody",x).run().assertBody().is("{\n\tf1: 'foo'\n}");
		client().useWhitespace().build().post("/echoBody",x).run().assertBody().is("{\n\tf1: 'foo'\n}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parser properties
	//-----------------------------------------------------------------------------------------------------------------


	@Test
	public void m01_parser_debugOutputLines() throws Exception {
		RestClient rc = client().debugOutputLines(10).build();
		assertEquals(10,rc.parsers.getParser("application/json").toMap().getMap("Parser").getInt("debugOutputLines").intValue());
	}

	public static class M2 {
		public int f;
	}

	@Test
	public void m02_parser_strict() throws Exception {
		assertThrown(()->{MockRestClient.create(A.class).json().strict().build().post("/echoBody",new StringReader("{f:1}")).run().getBody().as(M2.class);}).contains("Unquoted attribute detected.");
	}

	public static class M3 {
		public String f;
	}

	@Test
	public void m03_parser_trimStringsOnRead() throws Exception {
		M3 x = client().trimStringsOnRead().build().post("/echoBody",new StringReader("{f:' 1 '}")).run().getBody().as(M3.class);
		assertEquals("1",x.f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OpenApi properties
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void n01_openApi_oapiFormat() throws Exception {
		MockRestClient.create(A.class).oapiFormat(HttpPartFormat.UON).build().get("/checkQuery").query("Foo","bar baz").run().assertBody().is("Foo=%27bar+baz%27").assertBody().urlDecode().is("Foo='bar baz'");
	}

	@Test
	public void n02_openApi_oapiCollectionFormat() throws Exception {
		RestClient x = MockRestClient.create(A.class).oapiCollectionFormat(HttpPartCollectionFormat.PIPES).build();
		x.get("/checkQuery").query("Foo",new String[]{"bar","baz"}).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecode().is("Foo=bar|baz");
		x.post("/checkFormData").formData("Foo",new String[]{"bar","baz"}).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecode().is("Foo=bar|baz");
		x.get("/checkHeader").header("Check","Foo").header("Foo",new String[]{"bar","baz"}).accept("text/json+simple").run().assertBody().is("['bar|baz']");
	}

	@Test
	public void n03_urlEnc_paramFormat() throws Exception {
		 OMap map = OMap.of(
			"foo","bar",
			"baz",new String[]{"qux","true","123"}
		);
		MockRestClient.create(A.class).urlEnc().paramFormat(ParamFormat.PLAINTEXT).build().post("/echoBody",map).run().assertBody().is("foo=bar&baz=qux,true,123");
		MockRestClient.create(A.class).urlEnc().paramFormatPlain().build().post("/echoBody",map).run().assertBody().is("foo=bar&baz=qux,true,123");
		MockRestClient.create(A.class).urlEnc().paramFormat(ParamFormat.UON).build().post("/echoBody",map).run().assertBody().is("foo=bar&baz=@(qux,'true','123')");
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
		RestClient x1 = client().build();
		RestClient x2 = client(A.class).beanClassVisibility(Visibility.PROTECTED).build();
		x1.post("/echoBody",new O1()).run().assertBody().is("'O1'");
		x2.post("/echoBody",new O1()).run().assertBody().is("{f:1}");
		x1.get("/checkQuery").query("foo",new O1()).run().assertBody().is("foo=O1");
		x2.get("/checkQuery").query("foo",new O1()).run().assertBody().is("foo=f%3D1").assertBody().urlDecode().is("foo=f=1");
		x1.formPost("/checkFormData").formData("foo",new O1()).run().assertBody().is("foo=O1");
		x2.formPost("/checkFormData").formData("foo",new O1()).run().assertBody().is("foo=f%3D1").assertBody().urlDecode().is("foo=f=1");
		x1.get("/checkHeader").header("foo",new O1()).header("Check","foo").run().assertBody().is("['O1']");
		x2.get("/checkHeader").header("foo",new O1()).header("Check","foo").run().assertBody().is("['f=1']");
	}

	public static class O2a {
		private int f;
		protected O2a(int f) {
			this.f = f;
		}
		public int toInt() {
			return f;
		}
	}

	@Rest
	public static class O2b extends BasicRest {
		@RestMethod
		public Reader postTest(org.apache.juneau.rest.RestRequest req,org.apache.juneau.rest.RestResponse res) throws IOException {
			res.setHeader("X",req.getHeaders().getString("X"));
			return req.getBody().getReader();
		}
	}

	@Test
	public void o02_beanContext_beanConstructorVisibility() throws Exception {
		RestResponse x = client(O2b.class).beanConstructorVisibility(Visibility.PROTECTED).build().post("/test",new O2a(1)).header("X",new O2a(1)).run().cacheBody().assertBody().is("1").assertHeader("X").is("1");
		assertEquals(1,x.getBody().as(O2a.class).f);
		assertEquals(1,x.getHeader("X").as(O2a.class).f);
	}

	public static class O3 {
		public int f1;
		protected int f2;
		static O3 get() {
			O3 x = new O3();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o03_beanContext_beanFieldVisibility() throws Exception {
		RestResponse x = client(O2b.class).beanFieldVisibility(Visibility.PROTECTED).build().post("/test",O3.get()).header("X",O3.get()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		assertEquals(2,x.getBody().as(O3.class).f2);
		assertEquals(2,x.getHeader("X").as(O3.class).f2);
	}

	public static interface O4a {
		int getF3();
		void setF3(int f3);
	}

	public static class O4b implements O4a {
		public int f1,f2;
		private int f3;
		@Override
		public int getF3() {
			return f3;
		}
		@Override
		public void setF3(int f3) {
			this.f3 = f3;
		}
		static O4b get() {
			O4b x = new O4b();
			x.f1 = 1;
			x.f2 = 2;
			x.f3 = 3;
			return x;
		}
		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o04_beanContext_beanFilters() throws Exception {
		RestResponse x = client(O2b.class).bpi(O4b.class,"f1").build().post("/test",O4b.get()).header("X",O4b.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals(0,x.getBody().as(O4b.class).f2);
		assertEquals(0,x.getHeader("X").as(O4b.class).f2);

		x = client(O2b.class).bpi(O4b.class,"f1").build().post("/test",O4b.get()).header("X",O4b.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals(0,x.getBody().as(O4b.class).f2);
		assertEquals(0,x.getHeader("X").as(O4b.class).f2);

		x = client(O2b.class).bpi(O4b.class,"f1").build().post("/test",O4b.get()).header("X",O4b.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals(0,x.getBody().as(O4b.class).f2);
		assertEquals(0,x.getHeader("X").as(O4b.class).f2);

		x = client(O2b.class).bpi(O4b.class,"f1").build().post("/test",O4b.get()).header("X",O4b.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals(0,x.getBody().as(O4b.class).f2);
		assertEquals(0,x.getHeader("X").as(O4b.class).f2);

		x = client(O2b.class).interfaces(O4a.class).build().post("/test",O4b.get()).header("X",O4b.get()).run().cacheBody().assertBody().is("{f3:3}").assertHeader("X").is("f3=3");
		assertEquals(3,x.getBody().as(O4b.class).f3);
		assertEquals(3,x.getHeader("X").as(O4b.class).f3);
	}

	public static class O5  {
		private int f1,f2;
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
		static O5 get() {
			O5 x = new O5();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o05_beanContext_beanMethodVisibility() throws Exception {
		RestResponse x = client(O2b.class).beanMethodVisibility(Visibility.PROTECTED).build().post("/test",O5.get()).header("X",O5.get()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		assertEquals(2,x.getBody().as(O5.class).f2);
		assertEquals(2,x.getHeader("X").as(O5.class).f2);
	}

	public static class O6 {}

	@Test
	public void o06_beanContext_beansDontRequireSomeProperties() throws Exception {
		client().beansDontRequireSomeProperties().build().post("/echoBody",new O6()).run().assertBody().is("{}");
	}

	public static class O7  {
		public String f1;
		public O7(String i) {
			f1 = i;
		}
		@Override
		public String toString() {
			return f1;
		}
	}

	@Test
	public void o07_beanContext_beansRequireDefaultConstructor() throws Exception {
		client(O2b.class).build().post("/test",new O7("1")).header("X",new O7("1")).run().assertBody().is("{f1:'1'}").assertHeader("X").is("f1=1");
		client(O2b.class).beansRequireDefaultConstructor().build().post("/test",new O7("1")).header("X",new O7("1")).run().assertBody().is("'1'").assertHeader("X").is("1");
	}

	@Test
	public void o08_beanContext_beansRequireSerializable() throws Exception {
		client(O2b.class).build().post("/test",new O7("1")).header("X",new O7("1")).run().assertBody().is("{f1:'1'}").assertHeader("X").is("f1=1");
		client(O2b.class).beansRequireSerializable().build().post("/test",new O7("1")).header("X",new O7("1")).run().assertBody().is("'1'").assertHeader("X").is("1");
	}

	public static class O9 {
		private int f1,f2;
		public int getF1() {
			return f1;
		}
		public void setF1(int f1) {
			this.f1 = f1;
		}
		public int getF2() {
			return f2;
		}
		static O9 get() {
			O9 x = new O9();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}
		@Override
		public String toString() {
			return f1 + "/" + f2;
		}
	}

	@Test
	public void o09_beanContext_beansRequireSettersForGetters() throws Exception {
		client(O2b.class).build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		client(O2b.class).beansRequireSettersForGetters().build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
	}

	@Test
	public void o10_beanContext_bpi() throws Exception {
		client(O2b.class).bpi(OMap.of("O9","f2")).build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpi(O9.class,"f2").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpi("O9","f2").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpi(O9.class.getName(),"f2").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
	}

	@Test
	public void o11_beanContext_bpro() throws Exception {
		RestResponse x = null;

		x = client(O2b.class).bpro(OMap.of("09","f2")).build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());

		x = client(O2b.class).bpro(O9.class,"f2").build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());

		x = client(O2b.class).bpro("O9","f2").build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());
	}

	@Test
	public void o12_beanContext_bpwo() throws Exception {
		RestResponse x = null;

		x = client(O2b.class).bpwo(OMap.of("O9","f2")).build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());

		x = client(O2b.class).bpwo(O9.class,"f2").build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());

		x = client(O2b.class).bpwo("O9","f2").build().post("/test",O9.get()).header("X",O9.get()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1");
		assertEquals("1/0",x.getBody().as(O9.class).toString());
		assertEquals("1/0",x.getHeader("X").as(O9.class).toString());
	}

	@Test
	public void o13_beanContext_bpx() throws Exception {
		client(O2b.class).bpx(OMap.of("O9","f1")).build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpx(O9.class,"f1").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpx("O9","f1").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
		client(O2b.class).bpx(O9.class.getName(),"f1").build().post("/test",O9.get()).header("X",O9.get()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2");
	}

	public static class O14 {
		public Object f;
	}

	@Test
	public void o14_beanContext_debug() throws Exception {
		O14 x = new O14();
		x.f = x;
		assertThrown(()->{client().debug().build().post("/echo",x).run();}).contains("Recursion occurred");
	}

	@org.apache.juneau.annotation.Bean(typeName="foo")
	public static class O15a {
		public String foo;
		static O15a get() {
			O15a x = new O15a();
			x.foo = "1";
			return x;
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="bar")
	public static class O15b {
		public String foo;
		static O15b get() {
			O15b x = new O15b();
			x.foo = "2";
			return x;
		}
	}

	public static class O15c {
		public Object foo;
		static O15c get() {
			O15c x = new O15c();
			x.foo = O15a.get();
			return x;
		}
	}

	@Test
	public void o15_beanContext_dictionary() throws Exception {
		Object o = client().dictionary(O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody",O15a.get()).run().cacheBody().assertBody().contains("{_type:'foo',foo:'1'}").getBody().as(Object.class);;
		assertTrue(o instanceof O15a);

		OMap m = OMap.of("x",O15a.get(),"y",O15b.get());
		m = client().dictionary(O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody",m).run().cacheBody().assertBody().is("{x:{_type:'foo',foo:'1'},y:{_type:'bar',foo:'2'}}").getBody().as(OMap.class);;
		assertTrue(m.get("x") instanceof O15a);
		assertTrue(m.get("y") instanceof O15b);

		O15c x = client().dictionaryOn(O15c.class,O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody",O15c.get()).run().cacheBody().assertBody().is("{foo:{_type:'foo',foo:'1'}}").getBody().as(O15c.class);;
		assertTrue(x.foo instanceof O15a);
	}

	public static class O16 {
		private String foo;
		public String getFoo() {
			return foo;
		}
		static O16 get() {
			O16 x = new O16();
			x.foo = "foo";
			return x;
		}
	}

	@Test
	public void o16_beanContext_dontIgnorePropertiesWithoutSetters() throws Exception {
		O16 x = client().build().post("/echoBody",O16.get()).run().cacheBody().assertBody().contains("{foo:'foo'}").getBody().as(O16.class);
		assertNull(x.foo);
		assertThrown(()->{client().dontIgnorePropertiesWithoutSetters().build().post("/echoBody",O16.get()).run().cacheBody().assertBody().contains("{foo:'foo'}").getBody().as(O16.class);}).contains("Setter or public field not defined");
	}

	public static class O17 {
		public String foo;
		public transient String bar;
		static O17 get() {
			O17 x = new O17();
			x.foo = "1";
			x.bar = "2";
			return x;
		}
	}

	@Test
	public void o17_beanContext_dontIgnoreTransientFields() throws Exception {
		O17 x = client().build().post("/echoBody",O17.get()).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O17.class);;
		assertNull(x.bar);
		x = client().dontIgnoreTransientFields().build().post("/echoBody",O17.get()).run().cacheBody().assertBody().contains("{bar:'2',foo:'1'}").getBody().as(O17.class);
		assertEquals("2",x.bar);
	}

	public static class O18 {
		public String foo;
	}

	@Test
	public void o18_beanContext_dontIgnoreUnknownNullBeanProperties() throws Exception {
		client().build().post("/echoBody",new StringReader("{foo:'1',bar:null}")).run().cacheBody().assertBody().contains("{foo:'1',bar:null}").getBody().as(O18.class);;
		assertThrown(()->{client().dontIgnoreUnknownNullBeanProperties().build().post("/echoBody",new StringReader("{foo:'1',bar:null}")).run().cacheBody().assertBody().contains("{foo:'1',bar:null}").getBody().as(O18.class);}).contains("Unknown property 'bar'");
	}

	public static interface O19 {
		public String getFoo();
		public void setFoo(String foo);
	}

	@Test
	public void o19_beanContext_dontUseInterfaceProxies() throws Exception {
		O19 x = client().build().post("/echoBody",new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O19.class);;
		assertEquals("1",x.getFoo());
		assertThrown(()->{client().dontUseInterfaceProxies().build().post("/echoBody",new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O19.class);}).contains("could not be instantiated");
	}

	public static class O20 {
		private String foo;
		public String getFoo() {
			return foo;
		}
		public O20 foo(String foo) {
			this.foo = foo;
			return this;
		}
	}

	@Test
	public void o20_beanContext_fluentSetters() throws Exception {
		O20 x = client().fluentSetters().build().post("/echoBody",new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O20.class);;
		assertEquals("1",x.getFoo());
		x = client().fluentSetters(O20.class).build().post("/echoBody",new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O20.class);;
		assertEquals("1",x.getFoo());
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
			throw new RuntimeException("xxx");
		}
		static O21 get() {
			O21 x = new O21();
			x.foo = "1";
			x.bar = "2";
			return x;
		}
	}

	@Test
	public void o21_beanContext_ignoreInvocationExceptionsOnGetters() throws Exception {
		assertThrown(()->{client().build().post("/echoBody",O21.get()).run();}).contains("Could not call getValue() on property 'bar'");
		O21 x = client().ignoreInvocationExceptionsOnGetters().build().post("/echoBody",O21.get()).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O21.class);;
		assertEquals("1",x.getFoo());
	}

	public static class O22 {
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
		static O22 get() {
			O22 x = new O22();
			x.foo = "1";
			x.bar = "2";
			return x;
		}
	}

	@Test
	public void o22_beanContext_ignoreInvocationExceptionsOnSetters() throws Exception {
		assertThrown(()->{client().build().post("/echoBody",O22.get()).run().getBody().as(O22.class);}).contains("Error occurred trying to set property 'bar'");
		O22 x = client().ignoreInvocationExceptionsOnSetters().build().post("/echoBody",O22.get()).run().cacheBody().getBody().as(O22.class);;
		assertEquals("1",x.getFoo());
	}

	public static class O23 {
		public String foo;
	}

	@Test
	public void o23_beanContext_ignoreUnknownBeanProperties() throws Exception {
		assertThrown(()->{client().build().post("/echoBody",new StringReader("{foo:'1',bar:'2'}")).run().getBody().as(O23.class);}).contains("Unknown property 'bar' encountered");
		O23 x = client().ignoreUnknownBeanProperties().build().post("/echoBody",new StringReader("{foo:'1',bar:'2'}")).run().cacheBody().getBody().as(O23.class);;
		assertEquals("1",x.foo);
	}

	public static interface O24a {
		void setFoo(int foo);
		int getFoo();
	}

	public static class O24b implements O24a {
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
	public void o24_beanContext_implClass() throws Exception {
		O24a x = client().implClass(O24a.class,O24b.class).build().post("/echoBody",new StringReader("{foo:1}")).run().getBody().as(O24a.class);
		assertEquals(1,x.getFoo());
		assertTrue(x instanceof O24b);

		x = client().implClasses(AMap.of(O24a.class,O24b.class)).build().post("/echoBody",new StringReader("{foo:1}")).run().getBody().as(O24a.class);
		assertEquals(1,x.getFoo());
		assertTrue(x instanceof O24b);
	}

	public static interface O25a {
		void setFoo(int foo);
		int getFoo();
	}

	public static class O25b implements O25a {
		private int foo,bar;
		@Override
		public int getFoo() { return foo; }
		@Override
		public void setFoo(int foo) { this.foo = foo; }
		public int getBar() { return bar; }  // Not executed
		public void setBar(int bar) { this.bar = bar; }  // Not executed

		static O25b get() {
			O25b x = new O25b();
			x.foo = 1;
			x.bar = 2;
			return x;
		}
	}

	@Test
	public void o25_beanContext_interfaceClass() throws Exception {
		O25a x = client().interfaceClass(O25b.class,O25a.class).build().post("/echoBody",O25b.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O25b.class);
		assertEquals(1,x.getFoo());
		x = client().interfaces(O25a.class).build().post("/echoBody",O25b.get()).run().assertBody().is("{foo:1}").getBody().as(O25b.class);
		assertEquals(1,x.getFoo());
	}

	public static class O26a {
		public int foo;
		static O26a get() {
			O26a x = new O26a();
			x.foo = 1;
			return x;
		}
	}

	@Test
	public void o26_beanContext_locale() throws Exception {
		O26a x = client().locale(Locale.UK).build().post("/echoBody",O26a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O26a.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void o27_beanContext_mediaType() throws Exception {
		O26a x = client().mediaType(MediaType.JSON).build().post("/echoBody",O26a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O26a.class);
		assertEquals(1,x.foo);
	}

	public static class O28 {
		public int foo;
		static O28 get() {
			O28 x = new O28();
			x.foo = 1;
			return x;
		}
		@Override
		public String toString() {
			return String.valueOf(foo);
		}
		public static O28 fromString(String foo) throws ParseException {
			O28 x = new O28();
			x.foo = JsonParser.DEFAULT.parse(foo,int.class);
			return x;
		}
	}

	@Test
	public void o28_beanContext_notBeanClasses() throws Exception {
		O28 x = client().notBeanClasses(O28.class).build().post("/echoBody",O28.get()).run().cacheBody().assertBody().is("'1'").getBody().as(O28.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void o29_beanContext_notBeanPackages() throws Exception {
		O28 x = client().notBeanPackages(O28.class.getPackage()).build().post("/echoBody",O28.get()).run().cacheBody().assertBody().is("'1'").getBody().as(O28.class);
		assertEquals(1,x.foo);
	}

	public static class O30a {
		private String foo;
		public String getFoo() { return foo; }
		public void setFoo(String foo) { this.foo = foo; }
		static O30a get() {
			O30a x = new O30a();
			x.foo = "foo";
			return x;
		}
	}

	public static class O30b extends BeanInterceptor<O30a> {
		static boolean getterCalled,setterCalled;
		@Override
		public Object readProperty(O30a bean,String name,Object value) {
			getterCalled = true;
			return "x" + value;
		}
		@Override
		public Object writeProperty(O30a bean,String name,Object value) {
			setterCalled = true;
			return value.toString().substring(1);
		}
	}

	@Test
	public void o30_beanContext_beanInterceptor() throws Exception {
		O30a x = client().beanInterceptor(O30a.class,O30b.class).build().post("/echoBody",O30a.get()).run().cacheBody().assertBody().is("{foo:'xfoo'}").getBody().as(O30a.class);
		assertEquals("foo",x.foo);
		assertTrue(O30b.getterCalled);
		assertTrue(O30b.setterCalled);
	}

	public static class O31 {
		private String fooBar;
		public String getFooBar() { return fooBar; }
		public void setFooBar(String fooBar) { this.fooBar = fooBar; }
		static O31 get() {
			O31 x = new O31();
			x.fooBar = "fooBar";
			return x;
		}
	}

	@Test
	public void o31_beanContext_propertyNamer() throws Exception {
		O31 x = client().propertyNamer(PropertyNamerDLC.class).build().post("/echoBody",O31.get()).run().cacheBody().assertBody().is("{'foo-bar':'fooBar'}").getBody().as(O31.class);
		assertEquals("fooBar",x.fooBar);
		x = client().propertyNamer(O31.class,PropertyNamerDLC.class).build().post("/echoBody",O31.get()).run().cacheBody().assertBody().is("{'foo-bar':'fooBar'}").getBody().as(O31.class);
		assertEquals("fooBar",x.fooBar);
	}

	public static class O32 {
		public int foo,bar,baz;
		static O32 get() {
			O32 x = new O32();
			x.foo = 1;
			x.bar = 2;
			x.baz = 3;
			return x;
		}
	}

	@Test
	public void o32_beanContext_sortProperties() throws Exception {
		O32 x = client().sortProperties().build().post("/echoBody",O32.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(O32.class);
		assertEquals(1,x.foo);
		x = client().sortProperties(O32.class).build().post("/echoBody",O32.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(O32.class);
		assertEquals(1,x.foo);
	}

	public static class O33a {
		public int foo;
	}

	public static class O33b extends O33a {
		public int bar;
		static O33b get() {
			O33b x = new O33b();
			x.foo = 1;
			x.bar = 2;
			return x;
		}
	}

	@Test
	public void o33_beanContext_stopClass() throws Exception {
		O33b x = client().stopClass(O33b.class,O33a.class).build().post("/echoBody",O33b.get()).run().cacheBody().assertBody().is("{bar:2}").getBody().as(O33b.class);
		assertEquals(0,x.foo);
		assertEquals(2,x.bar);
	}

	public static class O34a {
		public int foo;
		static O34a get() {
			O34a x = new O34a();
			x.foo = 1;
			return x;
		}
	}

	public static class O34b extends PojoSwap<O34a,Integer> {
		@Override
		public Integer swap(BeanSession session,O34a o) { return o.foo; }
		@Override
		public O34a unswap(BeanSession session,Integer f,ClassMeta<?> hint) {return O34a.get(); }
	}

	@Test
	public void o34_beanContext_swaps() throws Exception {
		O34a x = client().swaps(O34b.class).build().post("/echoBody",O34a.get()).run().cacheBody().assertBody().is("1").getBody().as(O34a.class);
		assertEquals(1,x.foo);
	}

	public static class O35a {
		public int foo;
		static O35a get() {
			O35a x = new O35a();
			x.foo = 1;
			return x;
		}
	}

	@Test
	public void o35_beanContext_timeZone() throws Exception {
		O35a x = client().timeZone(TimeZone.getTimeZone("Z")).build().post("/echoBody",O35a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O35a.class);
		assertEquals(1,x.foo);
	}

	public static class O36 {
		public int foo;
		static O36 get() {
			O36 x = new O36();
			x.foo = 1;
			return x;
		}
	}

	@Test
	public void o36_beanContext_typeName() throws Exception {
		O36 x = client().typeName(O36.class,"foo").addRootType().build().post("/echoBody",O36.get()).run().cacheBody().assertBody().is("{_type:'foo',foo:1}").getBody().as(O36.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void o37_beanContext_typePropertyName() throws Exception {
		O36 x = client().typeName(O36.class,"foo").typePropertyName("X").addRootType().build().post("/echoBody",O36.get()).run().cacheBody().assertBody().is("{X:'foo',foo:1}").getBody().as(O36.class);
		assertEquals(1,x.foo);
		x = client().typeName(O36.class,"foo").typePropertyName(O36.class,"X").addRootType().build().post("/echoBody",O36.get()).run().cacheBody().assertBody().is("{X:'foo',foo:1}").getBody().as(O36.class);
		assertEquals(1,x.foo);
	}

	public static enum O38a {
		ONE(1),TWO(2);
		private int value;
		O38a(int value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return String.valueOf(value);  // Not executed
		}
	}

	public static class O38b {
		public O38a foo;
		static O38b get() {
			O38b x = new O38b();
			x.foo = O38a.ONE;
			return x;
		}
	}

	@Test
	public void o38_beanContext_useEnumNames() throws Exception {
		O38b x = client().useEnumNames().build().post("/echoBody",O38b.get()).run().cacheBody().assertBody().is("{foo:'ONE'}").getBody().as(O38b.class);
		assertEquals(O38a.ONE,x.foo);
	}

	public static class O39 {
		private int foo;
		public int bar;
		public int getFoo() {
			return foo;
		}
		public void setFoo(int foo) {
			this.foo = foo;
		}
		static O39 get() {
			O39 x = new O39();
			x.foo = 1;
			x.bar = 2;
			return x;
		}
	}

	@Test
	public void o39_beanContext_useJavaIntrospector() throws Exception {
		O39 x = client().useJavaBeanIntrospector().build().post("/echoBody",O39.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O39.class);
		assertEquals(1,x.foo);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class P1 {
		public String foo;
	}

	@Test
	public void p01_context_addMap() throws Exception {
		client().add(OMap.of(Serializer.SERIALIZER_keepNullProperties,true)).build().post("/echoBody",new P1()).run().cacheBody().assertBody().is("{foo:null}").getBody().as(P1.class);
	}

	public static class P2 {
		public String foo;
		@Override
		public String toString() {
			return foo;
		}
		public static P2 fromString(String s) {
			P2 p2 = new P2();
			p2.foo = s;
			return p2;
		}
	}

	@Test
	public void p02_context_addToStringObject() throws Exception {
		client().addTo(BeanContext.BEAN_notBeanClasses,P2.class).build().post("/echoBody",P2.fromString("bar")).run().cacheBody().assertBody().is("'bar'").getBody().as(P2.class);
	}

	public static class P3a {
		public int foo;
		static P3a get() {
			P3a x = new P3a();
			x.foo = 1;
			return x;
		}
	}

	public static class P3b extends PojoSwap<P3a,Integer> {
		@Override
		public Integer swap(BeanSession session, P3a o) { return o.foo; }
		@Override
		public P3a unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return P3a.get(); }
	}

	@Test
	public void p03_context_appendToStringObject() throws Exception {
		P3a x = client().appendTo(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody",P3a.get()).run().cacheBody().assertBody().is("1").getBody().as(P3a.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void p04_context_prependToStringObject() throws Exception {
		P3a x = client().prependTo(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody",P3a.get()).run().cacheBody().assertBody().is("1").getBody().as(P3a.class);
		assertEquals(1,x.foo);
	}

	public static class P5 {
		public int foo;
		static P5 get() {
			P5 x = new P5();
			x.foo = 1;
			return x;
		}
	}

	@Test
	public void p05_context_apply() throws Exception {
		MockRestClient.create(A.class).json().apply(SimpleJsonSerializer.DEFAULT.getPropertyStore()).build().post("/echoBody",P5.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P5.class);
	}

	public static class P6a {
		public int foo,bar,baz;
		static P6a get() {
			P6a x = new P6a();
			x.foo = 1;
			x.bar = 2;
			x.baz = 3;
			return x;
		}
	}

	@org.apache.juneau.annotation.Bean(sort=true,on="P6a")
	public static class P6b {}

	@BeanConfig(sortProperties="true")
	public static class P6c {}

	public static class P6d {
		@BeanConfig(sortProperties="true")
		public void foo() {}
	}

	@Test
	public void p06_context_applyAnnotations() throws Exception {
		new P6b();
		new P6c();
		new P6d().foo();
		client().applyAnnotations(P6b.class).build().post("/echoBody",P6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		client().applyAnnotations(P6c.class).build().post("/echoBody",P6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		client().applyAnnotations(P6d.class.getMethod("foo")).build().post("/echoBody",P6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		AnnotationList al = ClassInfo.of(P6c.class).getAnnotationList(ConfigAnnotationFilter.INSTANCE);
		VarResolverSession vr = VarResolver.DEFAULT.createSession();
		client().applyAnnotations(al,vr).build().post("/echoBody",P6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
	}

	@Test
	public void p07_context_removeFrom() throws Exception {
		P3a x = client().appendTo(BeanContext.BEAN_swaps,P3b.class).removeFrom(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody",P3a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P3a.class);
		assertEquals(1,x.foo);
	}

	@Test
	public void p08_context_setStringObject() throws Exception {
		MockRestClient.create(A.class).json().set(JsonSerializer.JSON_simpleMode,true).build().post("/echoBody",P3a.get()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P3a.class);
	}

	@Test
	public void p09_context_annotations() throws Exception {
		client().annotations(new BeanAnnotation(P6a.class).sort(true)).build().post("/echoBody",P6a.get()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
	}

	public static interface P10a {
		void setFoo(int foo);
		int getFoo();
	}

	public static class P10b implements P10a {
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
	public void p10_context_putAllTo() throws Exception {
		P10a x = client().putAllTo(BeanContext.BEAN_implClasses,AMap.of(P10a.class.getName(),P10b.class)).build().post("/echoBody",new StringReader("{foo:1}")).run().getBody().as(P10a.class);
		assertEquals(1,x.getFoo());
		assertTrue(x instanceof P10b);
	}

	public static class P11 {
		public int foo = 1;
	}

	@Test
	public void p11_context_set() throws Exception {
		MockRestClient.create(null).set(
				AMap.of(
					JsonSerializer.JSON_simpleMode,true,
					WriterSerializer.WSERIALIZER_quoteChar,"'",
					MockRestClient.MOCKRESTCLIENT_restBean,A.class
				)
			).json().build().post("/echoBody",new P11()).run().assertBody().is("{foo:1}")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairs pairs(Object...pairs) {
		return NameValuePairs.of(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}

	private static RestClientBuilder client(Class<?> c) {
		return MockRestClient.create(c).simpleJson();
	}
}
