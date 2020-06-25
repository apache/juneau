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
import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.assertions.ThrowableAssertion.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;

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
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.ContentType;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
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
import org.apache.juneau.testutils.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Test {

	public static class ABean {
		public int f;

		public ABean init() {
			f = 1;
			return this;
		}

		@Override
		public String toString() {
			return SimpleJson.DEFAULT.toString(this);
		}
	}

	public static ABean bean = new ABean().init();

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
		@RestMethod(path="/", name="*")
		public Reader echoMethod(@Method String method) {
			return new StringReader(method);
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
		RestClient.create().set(RESTCLIENT_httpClient, null).keepHttpClientOpen().build().close();

		ExecutorService es = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
		RestClient.create().executorService(es, true).build().close();
		RestClient.create().executorService(es, true).build().closeQuietly();
		RestClient.create().executorService(es, false).build().close();
		RestClient.create().executorService(es, false).build().closeQuietly();

		RestClient.create().debug().build().close();
		RestClient.create().debug().build().closeQuietly();
	}

	@Test
	public void a04_basicCalls() throws Exception {
		RestClient rc = MockRestClient.create(A.class).build();
		rc.get().run().assertBody().is("GET");
		rc.get("/").run().assertBody().is("GET");
		rc.get("").run().assertBody().is("GET");
		rc.put("/", null).run().assertBody().is("PUT");
		rc.post("/", null).run().assertBody().is("POST");
		rc.delete("/").run().assertBody().is("DELETE");
		rc.formPost("/").run().assertBody().is("POST");
	}

	@Test
	public void a05_basicCalls_get() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/bean").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).simpleJson().build().get(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a07_basicCalls_put() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().put("/bean", bean).run().assertBody().is("{f:1}");
		MockRestClient.create(A.class).simpleJson().build().put("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a08_basicCalls_put_fromString() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().put("/bean", "{f:1}", "application/json").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).simpleJson().build().put(url, bean).run().assertBody().is("{f:1}");
			MockRestClient.create(A.class).simpleJson().build().put(url, "{f:1}", "application/json").run().assertBody().is("{f:1}");
			MockRestClient.create(A.class).simpleJson().build().put(url).body(bean).run().assertBody().is("{f:1}");
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
			NameValuePairs.of("f", 1)
		);
		for (Object body : bodies) {
			MockRestClient.create(A.class).simpleJson().contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").build().put("/bean", body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a11_basicCalls_post() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().post("/bean", bean).run().assertBody().is("{f:1}");
		MockRestClient.create(A.class).simpleJson().build().post("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a12_basicCalls_post_fromString() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().post("/bean", "{f:1}", "application/json").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).simpleJson().build().post(url, bean).run().assertBody().is("{f:1}");
			MockRestClient.create(A.class).simpleJson().build().post(url, "{f:1}", "application/json").run().assertBody().is("{f:1}");
			MockRestClient.create(A.class).simpleJson().build().post(url).body(bean).run().assertBody().is("{f:1}");
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
			NameValuePairs.of("f", 1)
		);
		for (Object body : bodies) {
			MockRestClient.create(A.class).simpleJson().contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").build().post("/bean", body).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a15_basicCalls_delete() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().delete("/bean").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).simpleJson().build().delete(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a17_basicCalls_options() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().options("/bean").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).simpleJson().build().options(url).run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a19_basicCalls_head() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().head("/bean").run().assertBody().is("");
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
			MockRestClient.create(A.class).simpleJson().build().head(url).run().assertBody().is("");
		}
	}

	@Test
	public void a21_basicCalls_formPost() throws Exception {
		MockRestClient.create(A.class).build().formPost("/bean", bean).accept("application/json+simple").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).build().formPost(url, bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a23_basicCalls_formPost_exhaustiveBodyTypes() throws Exception {
		Supplier<Object>
			s1 = () -> new StringReader("f=1"),
			s2 = () -> new ByteArrayInputStream("f=1".getBytes());
		List<Object> bodies = AList.of(
			/*[ 0]*/ bean,
			/*[ 1]*/ NameValuePairs.of("f","1"),
			/*[ 2]*/ new NameValuePair[]{BasicNameValuePair.of("f","1")},
			/*[ 3]*/ new StringEntity("f=1", org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED),
			/*[ 4]*/ new StringEntity("f=1", (org.apache.http.entity.ContentType)null),
			/*[ 5]*/ BasicNameValuePair.of("f","1"),
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
			MockRestClient.create(A.class).header("Check", "Content-Type").accept("application/json+simple").build().formPost("/checkHeader", bodies.get(i)).run().assertBody().msg("Body {0} failed", i).matchesSimple("['application/x-www-form-urlencoded*']");
			MockRestClient.create(A.class).build().formPost("/bean", bodies.get(i)).accept("application/json+simple").run().assertBody().msg("Body {0} failed", "#"+i).is("{f:1}");
		}
	}

	@Test
	public void a24_basicCalls_formPostPairs() throws Exception {
		MockRestClient.create(A.class).build().formPostPairs("/bean", new StringBuilder("f"), new StringBuilder("1")).accept("application/json+simple").run().assertBody().is("{f:1}");
	}

	@Test
	public void a25_basicCalls_patch() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().patch("/bean", bean).run().assertBody().is("{f:1}");
		MockRestClient.create(A.class).simpleJson().build().patch("/bean").body(bean).run().assertBody().is("{f:1}");
	}

	@Test
	public void a26_basicCalls_patch_fromString() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().patch("/bean", "{f:1}", "application/json").run().assertBody().is("{f:1}");
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
			NameValuePairs.of("f", 1)
		);
		for (Object body : bodies) {
			MockRestClient.create(A.class).simpleJson().build().patch("/bean", body).contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).build().patch(url, bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a29_basicCalls_request_patch() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().request(HttpMethod.PATCH, "/bean", bean).run().assertBody().is("{f:1}");
		MockRestClient.create(A.class).simpleJson().build().request(HttpMethod.PATCH, "/bean").body(bean).run().assertBody().is("{f:1}");
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
			NameValuePairs.of("f", 1)
		);
		for (Object body : bodies) {
			MockRestClient.create(A.class).simpleJson().build().request(HttpMethod.PATCH, "/bean", body).contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).build().request(HttpMethod.PATCH, url, bean).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a32_basicCalls_request_get() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().request(HttpMethod.GET, "/bean", null).run().assertBody().is("{f:1}");
		MockRestClient.create(A.class).simpleJson().build().request(HttpMethod.GET, "/bean").run().assertBody().is("{f:1}");
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
			MockRestClient.create(A.class).build().request(HttpMethod.GET, url).accept("application/json+simple").run().assertBody().is("{f:1}");
		}
	}

	@Test
	public void a34_basicCalls_request_whenClosed() throws Exception {
		RestClient rc = MockRestClient.create(A.class).build();
		rc.closeQuietly();
		try {
			rc.request(HttpMethod.GET, "/bean", null); fail();
		} catch (RestCallException e) {
			assertThrowable(e).contains("RestClient.close() has already been called");
		}
	}

	@Test
	public void a35_basicCalls_request_whenClosed_withStackCreation() throws Exception {
		RestClient rc = MockRestClient.create(A.class).debug().build();
		rc.closeQuietly();
		try {
			rc.request(HttpMethod.GET, "/bean", null); fail();
		} catch (RestCallException e) {
			assertThrowable(e).contains("RestClient.close() has already been called");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_logging_logToConsole() throws Exception {
		MockConsole c = MockConsole.create();
		MockLogger l = MockLogger.create();

		MockRestClient.create(A.class).simpleJson().logRequests(DetailLevel.NONE, Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean", bean).complete();
		c.assertContents().is("");
		c.reset();

		MockRestClient.create(A.class).simpleJson().logRequests(DetailLevel.SIMPLE, Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean", bean).complete();
		c.assertContents().is("HTTP POST http://localhost/bean, HTTP/1.1 200 \n");
		c.reset();

		MockRestClient.create(A.class).simpleJson().logRequests(DetailLevel.FULL, Level.SEVERE).logToConsole().logger(l).console(c).build().post("/bean", bean).complete();
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

		MockRestClient.create(A.class).simpleJson().logRequests(DetailLevel.NONE, Level.SEVERE).logToConsole().logger(l).console(MockConsole.class).build().post("/bean", bean).complete();
	}

	@Test
	public void b02_logging_logTo() throws Exception {
		MockLogger l = MockLogger.create();

		MockRestClient.create(A.class).simpleJson().logRequests(DetailLevel.NONE, Level.SEVERE).logToConsole().logger(l).build().post("/bean", bean).complete();
		l.assertContents().is("");
		l.assertRecordCount().is(0);
		l.reset();

		MockRestClient.create(A.class).simpleJson().logger(l).logRequests(DetailLevel.SIMPLE, Level.WARNING).build().post("/bean", bean).complete();
		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().stderr().is("HTTP POST http://localhost/bean, HTTP/1.1 200 ");
		l.assertContents().is("WARNING: HTTP POST http://localhost/bean, HTTP/1.1 200 \n");
		l.reset();

		MockRestClient.create(A.class).simpleJson().logger(l).logRequests(DetailLevel.FULL, Level.WARNING).build().post("/bean", bean).complete();
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
			super.onConnect(req, res);
			req.log(Level.WARNING, "Foo");
			req.log(Level.WARNING, new RuntimeException(), "Bar");
			res.log(Level.WARNING, "Baz");
			res.log(Level.WARNING, new RuntimeException(), "Qux");
			req.log(Level.WARNING, (Throwable)null, "Quux");
		}
	}

	@Test
	public void b03_logging_other() throws Exception {
		MockLogger ml = MockLogger.create();
		MockConsole mc = MockConsole.create();
		MockRestClient.create(A.class).simpleJson().logger(ml).interceptors(B3.class).build().post("/bean", bean).complete();
		ml.assertRecordCount().is(5);
		ml.reset();
		MockRestClient.create(A.class).simpleJson().logger(ml).logToConsole().console(mc).interceptors(B3.class).build().post("/bean", bean).complete();
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
			request.setHeader("A1", "1");
		}
		@Override
		public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
			response.setHeader("B1", "1");
		}
	}

	@Test
	public void c01_httpClient_interceptors() throws Exception {
		HttpRequestInterceptor x1 = new HttpRequestInterceptor() {
			@Override public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				request.setHeader("A1", "1");
			}
		};
		HttpResponseInterceptor x2 = new HttpResponseInterceptor() {
			@Override public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				response.setHeader("B1", "1");
			}
		};
		HttpRequestInterceptor x3 = new HttpRequestInterceptor() {
			@Override public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				request.setHeader("A2", "2");
			}
		};
		HttpResponseInterceptor x4 = new HttpResponseInterceptor() {
			@Override public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				response.setHeader("B2", "2");
			}
		};

		MockRestClient.create(A.class).simpleJson().addInterceptorFirst(x1).addInterceptorLast(x2).addInterceptorFirst(x3).addInterceptorLast(x4)
			.build().get("/echo").run().assertBody().contains("A1: 1", "A2: 2").assertHeader("B1").is("1").assertHeader("B2").is("2");
		MockRestClient.create(A.class).simpleJson().interceptors(C01.class).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
		MockRestClient.create(A.class).simpleJson().interceptors(new C01()).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
	}

	@Test
	public void c02_httpClient_httpProcessor() throws RestCallException {
		HttpProcessor x = new HttpProcessor() {
			@Override
			public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
				request.setHeader("A1", "1");
			}

			@Override
			public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
				response.setHeader("B1", "1");
			}
		};
		MockRestClient.create(A.class).simpleJson().httpProcessor(x).build().get("/echo").run().assertBody().contains("A1: 1").assertHeader("B1").is("1");
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
		MockRestClient.create(A.class).simpleJson().requestExecutor(x).build().get("/echo").run().assertBody().contains("HTTP GET /echo");
		assertTrue(b1.get());
	}

	@Test
	public void c04_httpClient_defaultHeaders() throws RestCallException {
		MockRestClient.create(A.class).simpleJson().defaultHeaders(AList.of(new org.apache.http.message.BasicHeader("Foo", "bar"))).build().get("/echo").run().assertBody().contains("HTTP GET /echo","Foo: bar");
	}

	@Test
	public void c05_httpClient_httpClientBuilderMethods() {
		RestClient.create().disableRedirectHandling().redirectStrategy(DefaultRedirectStrategy.INSTANCE).defaultCookieSpecRegistry(null).sslHostnameVerifier(null).publicSuffixMatcher(null).sslContext(null).sslSocketFactory(null).maxConnTotal(10).maxConnPerRoute(10).defaultSocketConfig(null).defaultConnectionConfig(null).connectionTimeToLive(100, TimeUnit.DAYS).connectionManager(null).connectionManagerShared(true).connectionReuseStrategy(null).keepAliveStrategy(null).targetAuthenticationStrategy(null).proxyAuthenticationStrategy(null).userTokenHandler(null).disableConnectionState().schemePortResolver(null).userAgent("foo").disableCookieManagement().disableContentCompression().disableAuthCaching().retryHandler(null).disableAutomaticRetries().proxy(null).routePlanner(null).connectionBackoffStrategy(null).backoffManager(null).serviceUnavailableRetryStrategy(null).defaultCookieStore(null).defaultCredentialsProvider(null).defaultAuthSchemeRegistry(null).contentDecoderRegistry(null).defaultRequestConfig(null).useSystemProperties().evictExpiredConnections().evictIdleConnections(1, TimeUnit.DAYS);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void c06_httpClient_unusedHttpClientMethods() {
		RestClient x = RestClient.create().build();
		try {
			x.getParams();
		} catch (UnsupportedOperationException e) {}
		assertNotNull(x.getConnectionManager());
	}

	@Test
	public void c07_httpClient_executeHttpUriRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(x);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c08_httpClient_executeHttpHostHttpRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target, x);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c09_httpClient_executeHttpHostHttpRequestHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		HttpContext context = new BasicHttpContext();
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient.create(A.class).build().execute(target, x, context);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c10_httpClient_executeResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(x, new BasicResponseHandler());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c11_httpClient_executeHttpUriRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(x, new BasicResponseHandler(), new BasicHttpContext());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c12_httpClient_executeHttpHostHttpRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"), x, new BasicResponseHandler(), new BasicHttpContext());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c13_httpClient_executeHttpHostHttpRequestResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient.create(A.class).build().execute(new HttpHost("localhost"), x, new BasicResponseHandler());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c14_httpClient_requestConfig() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/bean").requestConfig(RequestConfig.custom().setMaxRedirects(1).build()).run().assertBody().is("{f:1}");
	}

	@Test
	public void c15_httpClient_pooled() throws Exception {
		RestClient x = RestClient.create().simpleJson().pooled().build();
		Object hc = x.httpClient;
		assertEquals("PoolingHttpClientConnectionManager", ClassInfo.of(hc).getDeclaredField("connManager").accessible().invoke(hc).getClass().getSimpleName());

		x = RestClient.create().simpleJson().build();
		hc = x.httpClient;
		assertEquals("BasicHttpClientConnectionManager", ClassInfo.of(hc).getDeclaredField("connManager").accessible().invoke(hc).getClass().getSimpleName());

		x = MockRestClient.create(A.class).pooled().simpleJson().build();
		hc = x.httpClient;
		assertEquals("MockHttpClientConnectionManager", ClassInfo.of(hc).getDeclaredField("connManager").accessible().invoke(hc).getClass().getSimpleName());
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
				assertEquals("Basic dXNlcjpwdw==", auth);
				return "OK";
			}
		}
	}

	@Test
	public void e01_basicAuth() throws RestCallException {
		MockRestClient.create(E.class).simpleJson().basicAuth(AuthScope.ANY_HOST, AuthScope.ANY_PORT, "user", "pw").build().get("/echo").run().assertBody().contains("OK");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_headers_basicHeader() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").header("Foo","bar").build().get("/checkHeader").header("Foo","baz").run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f02_headers_fromSupplier() throws Exception {
		TestSupplier s = TestSupplier.of("foo");
		RestClient x = MockRestClient.create(A.class).simpleJson().header("Check", "Foo").header("Foo", s).build();
		x.get("/checkHeader").header("Foo", s).run().assertBody().is("['foo','foo']");
		s.set("bar");
		x.get("/checkHeader").header("Foo", s).run().assertBody().is("['bar','bar']");
	}

	@Test
	public void f03_headers_beanValue() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Foo", bean).header("Check", "Foo").build().get("/checkHeader").header("Foo", bean).run().assertBody().is("['f=1','f=1']");
	}

	@Test
	public void f04_headers_nullValue() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Foo", null).header("Check", "Foo").build().get("/checkHeader").header("Foo", null).run().assertBody().is("null");
	}
	@Test
	public void f04_headers_nullHeader() throws Exception {
		MockRestClient.create(A.class).simpleJson().headers(BasicStringHeader.of("Foo", "bar"), null).header("Check", "Foo").build().get("/checkHeader").run().assertBody().is("['bar']");
	}

	@Test
	public void f05_headers_Header() throws Exception {
		MockRestClient.create(A.class).simpleJson().header(new org.apache.http.message.BasicHeader("Foo", "bar")).header("Check", "Foo").build().get("/checkHeader").header(new org.apache.http.message.BasicHeader("Foo", "baz")).run().assertBody().is("['bar','baz']");
		MockRestClient.create(A.class).simpleJson().headers(new org.apache.http.message.BasicHeader("Foo", "bar"),new org.apache.http.message.BasicHeader("Baz", "baz")).header("Check", "Foo").build().get("/checkHeader").headers(new org.apache.http.message.BasicHeader("Foo", "baz"),new org.apache.http.message.BasicHeader("Baz", "quux")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f06_headers_NameValuePair() throws Exception {
		MockRestClient.create(A.class).simpleJson().header(new BasicNameValuePair("Foo", "bar")).header("Check", "Foo").build().get("/checkHeader").header(new BasicNameValuePair("Foo", "baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f07_headers_HttpHeader() throws Exception {
		MockRestClient.create(A.class).simpleJson().header(BasicStringHeader.of("Foo", "bar")).header("Check", "Foo").build().get("/checkHeader").header(BasicStringHeader.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f08_headers_objects() throws Exception {
		MockRestClient.create(A.class).simpleJson().headers(OMap.of("Foo", "bar")).header("Check", "Foo").build().get("/checkHeader").headers(OMap.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
		MockRestClient.create(A.class).simpleJson().headers(AMap.of("Foo", "bar")).header("Check", "Foo").build().get("/checkHeader").headers(AMap.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
		MockRestClient.create(A.class).simpleJson().headers(NameValuePairs.of("Foo","bar")).header("Check", "Foo").build().get("/checkHeader").headers(NameValuePairs.of("Foo","baz")).run().assertBody().is("['bar','baz']");
		MockRestClient.create(A.class).simpleJson().headers((Object)new NameValuePair[]{BasicNameValuePair.of("Foo","bar")}).header("Check", "Foo").build().get("/checkHeader").headers(NameValuePairs.of("Foo","baz")).run().assertBody().is("['bar','baz']");
		MockRestClient.create(A.class).simpleJson().headers(new BasicNameValuePair("Foo","bar")).header("Check", "Foo").build().get("/checkHeader").headers(new BasicNameValuePair("Foo","baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f12_headers_headerPairs() throws Exception {
		MockRestClient.create(A.class).simpleJson().headerPairs("Foo", "bar").header("Check", "Foo").build().get("/checkHeader").headerPairs("Foo", "baz").run().assertBody().is("['bar','baz']");
	}

	@Test
	public void f13_headers_standardHeaders() throws Exception {
		MockRestClient.create(A.class).simpleJson().accept("text/foo").header("Check", "Accept").build().get("/checkHeader").accept("text/plain").run().assertBody().is("['text/foo','text/plain']");
		MockRestClient.create(A.class).simpleJson().acceptCharset("UTF-8").header("Check", "Accept-Charset").build().get("/checkHeader").run().assertBody().is("['UTF-8']");
		MockRestClient.create(A.class).simpleJson().acceptEncoding("identity").header("Check", "Accept-Encoding").build().get("/checkHeader").run().assertBody().is("['identity']");
		MockRestClient.create(A.class).simpleJson().acceptLanguage("en").header("Check", "Accept-Language").build().get("/checkHeader").run().assertBody().is("['en']");
		MockRestClient.create(A.class).simpleJson().authorization("foo").header("Check", "Authorization").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().cacheControl("none").header("Check", "Cache-Control").build().get("/checkHeader").run().assertBody().is("['none']");
		MockRestClient.create(A.class).simpleJson().clientVersion("1").header("Check", "X-Client-Version").build().get("/checkHeader").run().assertBody().is("['1']");
		MockRestClient.create(A.class).simpleJson().connection("foo").header("Check", "Connection").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().contentLength("123").header("Check", "Content-Length").build().get("/checkHeader").run().assertBody().is("['123']");
		MockRestClient.create(A.class).simpleJson().contentType("foo").header("Check", "Content-Type").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().contentEncoding("identity").header("Check", "Content-Encoding").build().get("/checkHeader").run().assertBody().is("['identity']");
		MockRestClient.create(A.class).simpleJson().date("123").header("Check", "Date").build().get("/checkHeader").run().assertBody().is("['123']");
		MockRestClient.create(A.class).simpleJson().expect("foo").header("Check", "Expect").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().forwarded("foo").header("Check", "Forwarded").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().from("foo").header("Check", "From").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().host("foo").header("Check", "Host").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().ifMatch("foo").header("Check", "If-Match").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().ifModifiedSince("foo").header("Check", "If-Modified-Since").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().ifNoneMatch("foo").header("Check", "If-None-Match").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().ifRange("foo").header("Check", "If-Range").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().ifUnmodifiedSince("foo").header("Check", "If-Unmodified-Since").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().maxForwards("10").header("Check", "Max-Forwards").build().get("/checkHeader").run().assertBody().is("['10']");
		MockRestClient.create(A.class).simpleJson().noTrace().header("Check", "No-Trace").build().get("/checkHeader").run().assertBody().is("['true']");
		MockRestClient.create(A.class).simpleJson().origin("foo").header("Check", "Origin").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().pragma("foo").header("Check", "Pragma").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().proxyAuthorization("foo").header("Check", "Proxy-Authorization").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().range("foo").header("Check", "Range").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().referer("foo").header("Check", "Referer").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().te("foo").header("Check", "TE").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().userAgent(new StringBuilder("foo")).header("Check", "User-Agent").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().upgrade("foo").header("Check", "Upgrade").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().via("foo").header("Check", "Via").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().warning("foo").header("Check", "Warning").build().get("/checkHeader").run().assertBody().is("['foo']");
	}

	@Test
	public void f14_headers_debug_onRequest() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Debug").build().get("/checkHeader").debug().run().assertBody().is("['true']");
	}

	@Test
	public void f15_headers_SerializedNameValuePairBuilder() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").headers(SerializedNameValuePair.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/checkHeader").headers(SerializedNameValuePair.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
	}

	@Test
	public void f16_headers_SerializedHeaderBuilder() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").headers(SerializedHeader.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/checkHeader").headers(SerializedHeader.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
	}

	@Test
	public void f17_headers_headerBeans() throws Exception {
		MockRestClient.create(A.class).simpleJson().header(new Accept("text/foo")).header("Check", "Accept").build().get("/checkHeader").header(new Accept("text/plain")).run().assertBody().is("['text/foo','text/plain']");
		MockRestClient.create(A.class).simpleJson().header(new AcceptCharset("UTF-8")).header("Check", "Accept-Charset").build().get("/checkHeader").run().assertBody().is("['UTF-8']");
		MockRestClient.create(A.class).simpleJson().header(new AcceptEncoding("identity")).header("Check", "Accept-Encoding").build().get("/checkHeader").run().assertBody().is("['identity']");
		MockRestClient.create(A.class).simpleJson().header(new AcceptLanguage("en")).header("Check", "Accept-Language").build().get("/checkHeader").run().assertBody().is("['en']");
		MockRestClient.create(A.class).simpleJson().header(new Authorization("foo")).header("Check", "Authorization").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new CacheControl("none")).header("Check", "Cache-Control").header("X-Expect", "none").build().get("/checkHeader").run().assertBody().is("['none']");
		MockRestClient.create(A.class).simpleJson().header(new ClientVersion("1")).header("Check", "X-Client-Version").build().get("/checkHeader").run().assertBody().is("['1']");
		MockRestClient.create(A.class).simpleJson().header(new Connection("foo")).header("Check", "Connection").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new ContentLength(123)).header("Check", "Content-Length").build().get("/checkHeader").run().assertBody().is("['123']");
		MockRestClient.create(A.class).simpleJson().header(new ContentType("foo")).header("Check", "Content-Type").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new org.apache.juneau.http.header.Date("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "Date").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new org.apache.juneau.http.header.Date(CALENDAR)).header("Check", "Date").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new Expect("foo")).header("Check", "Expect").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Forwarded("foo")).header("Check", "Forwarded").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new From("foo")).header("Check", "From").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Host("foo")).header("Check", "Host").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new IfMatch("\"foo\"")).header("Check", "If-Match").build().get("/checkHeader").run().assertBody().is("['\"foo\"']");
		MockRestClient.create(A.class).simpleJson().header(new IfModifiedSince(CALENDAR)).header("Check", "If-Modified-Since").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new IfModifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "If-Modified-Since").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new IfNoneMatch("\"foo\"")).header("Check", "If-None-Match").build().get("/checkHeader").run().assertBody().is("['\"foo\"']");
		MockRestClient.create(A.class).simpleJson().header(new IfRange("foo")).header("Check", "If-Range").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new IfUnmodifiedSince(CALENDAR)).header("Check", "If-Unmodified-Since").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new IfUnmodifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "If-Unmodified-Since").build().get("/checkHeader").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		MockRestClient.create(A.class).simpleJson().header(new MaxForwards(10)).header("Check", "Max-Forwards").build().get("/checkHeader").run().assertBody().is("['10']");
		MockRestClient.create(A.class).simpleJson().header(new NoTrace("true")).header("Check", "No-Trace").build().get("/checkHeader").run().assertBody().is("['true']");
		MockRestClient.create(A.class).simpleJson().header(new Origin("foo")).header("Check", "Origin").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Pragma("foo")).header("Check", "Pragma").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new ProxyAuthorization("foo")).header("Check", "Proxy-Authorization").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Range("foo")).header("Check", "Range").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Referer("foo")).header("Check", "Referer").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new TE("foo")).header("Check", "TE").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new UserAgent("foo")).header("Check", "User-Agent").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Upgrade("foo")).header("Check", "Upgrade").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Via("foo")).header("Check", "Via").build().get("/checkHeader").run().assertBody().is("['foo']");
		MockRestClient.create(A.class).simpleJson().header(new Warning("foo")).header("Check", "Warning").build().get("/checkHeader").run().assertBody().is("['foo']");
	}

	@Test
	public void f18_headers_headerPairs() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").headerPairs("Foo","bar","Foo","baz").header("Foo","qux").build().get("/checkHeader").headerPairs("Foo","q1x","Foo","q2x").run().assertBody().is("['bar','baz','qux','q1x','q2x']");
	}

	@Test
	public void f19_headers_dontOverrideAccept() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Accept").header("Accept", "text/plain").build().get("/checkHeader").run().assertBody().is("['text/plain']");
		MockRestClient.create(A.class).simpleJson().header("Check", "Accept").header("Accept", "text/foo").build().get("/checkHeader").header("Accept","text/plain").run().assertBody().is("['text/foo','text/plain']");
		RestClient rc = MockRestClient.create(A.class).simpleJson().header("Check", "Accept").header("Accept", "text/foo").build();
		RestRequest req = rc.get("/checkHeader");
		req.setHeader("Accept","text/plain");
		req.run().assertBody().is("['text/plain']");
	}

	@Test
	public void f20_headers_dontOverrideContentType() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Content-Type").header("Content-Type", "text/plain").build().get("/checkHeader").run().assertBody().is("['text/plain']");
		MockRestClient.create(A.class).simpleJson().header("Check", "Content-Type").header("Content-Type", "text/foo").build().get("/checkHeader").header("Content-Type", "text/plain").run().assertBody().is("['text/foo','text/plain']");
	}

	@Test
	public void f21_headers_HttpPartSerializer() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").header("Foo", bean, null, new K12a()).build().get("/checkHeader").run().assertBody().is("['x{f:1}']");
	}

	@Test
	public void f22_headers_withSchema() throws Exception {
		MockRestClient.create(A.class).simpleJson().header("Foo", AList.of("bar","baz"), T_ARRAY_CSV).header("Check", "Foo").build().get("/checkHeader").run().assertBody().is("['bar,baz']");
	}

	@Test
	public void f23_headers_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		RestClient x = MockRestClient.create(A.class).simpleJson().header("Check", "Foo").header("Foo", s, T_ARRAY_PIPES).build();
		x.get("/checkHeader").header("Foo", s, T_ARRAY_PIPES).run().assertBody().is("['foo|bar','foo|bar']");
		s.set(new String[]{"bar","baz"});
		x.get("/checkHeader").header("Foo", s, T_ARRAY_PIPES).run().assertBody().is("['bar|baz','bar|baz']");
	}

	@Test
	public void f24_headers_withSchemaAndSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		MockRestClient.create(A.class).simpleJson().header("Check", "Foo").header("Foo", s, T_ARRAY_PIPES, UonSerializer.DEFAULT).build().get("/checkHeader").run().assertBody().is("['@(foo,bar)']");
	}

	@Test
	public void f25_headers_invalidHeader() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().headers("Foo"); fail();
		} catch (RuntimeException e) {
			assertThrowable(e).contains("Invalid type");
		}
	}

	@Test
	public void f26_headers_invalidHeaderPairs() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().headerPairs("Foo"); fail();
		} catch (RuntimeException e) {
			assertThrowable(e).contains("Odd number of parameters");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Query
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_query_basic() throws Exception {
		MockRestClient.create(A.class).simpleJson().query("Foo","bar").query("Foo",new StringBuilder("baz")).build().get("/checkQuery").run().assertBody().is("Foo=bar&Foo=baz");
	}

	@Test
	public void i02_query_objects() throws Exception {
		MockRestClient.create(A.class).simpleJson().queries(BasicNameValuePair.of("Foo","f1")).queries(OMap.of("Foo","f2")).queries(AMap.of("Foo","f3")).queries(NameValuePairs.of("Foo","f4","Foo","f5")).queries(BasicNameValuePair.of("Foo","f6"), BasicNameValuePair.of("Foo","f7")).queries((Object)new NameValuePair[]{BasicNameValuePair.of("Foo","f8")}).build().get("/checkQuery").run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
		MockRestClient.create(A.class).simpleJson().build().get("/checkQuery").queries(BasicNameValuePair.of("Foo","f1")).queries(OMap.of("Foo","f2")).queries(AMap.of("Foo","f3")).queries(NameValuePairs.of("Foo","f4","Foo","f5")).queries(BasicNameValuePair.of("Foo","f6"), BasicNameValuePair.of("Foo","f7")).queries((Object)new NameValuePair[]{BasicNameValuePair.of("Foo","f8")}).run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
		try {
			MockRestClient.create(A.class).simpleJson().build().get("/checkQuery").queries((Object)null); fail();
		} catch (Exception e) {
			assertThrowable(e).contains("Invalid type");
		}
		try {
			MockRestClient.create(A.class).simpleJson().queries("Baz"); fail();
		} catch (Exception e) {
			assertThrowable(e).contains("Invalid type");
		}
	}

	@Test
	public void i03_query_withSchema() throws Exception {
		MockRestClient.create(A.class).simpleJson().query("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().get("/checkQuery").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz");
	}

	@Test
	public void i04_query_withSchemaAndSerializer() throws Exception {
		MockRestClient.create(A.class).simpleJson().query("Foo",AList.of("bar","baz"), T_ARRAY_PIPES, UonSerializer.DEFAULT).build().get("/checkQuery").run().assertBody().is("Foo=%40%28bar%2Cbaz%29").assertBody().urlDecodedIs("Foo=@(bar,baz)");
	}

	@Test
	public void i05_query_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = MockRestClient.create(A.class).simpleJson().query("Foo", s, T_ARRAY_PIPES).build();
		x.get("/checkQuery").query("Bar", s, T_ARRAY_PIPES).run().assertBody().is("Foo=foo%7Cbar&Bar=foo%7Cbar").assertBody().urlDecodedIs("Foo=foo|bar&Bar=foo|bar");
		s.set(new String[]{"bar","baz"});
		x.get("/checkQuery").query("Bar", s, T_ARRAY_PIPES).run().assertBody().is("Foo=bar%7Cbaz&Bar=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz&Bar=bar|baz");
	}

	@Test
	public void i06_query_withSchemaAndSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = MockRestClient.create(A.class).simpleJson().query("Foo", s, T_ARRAY_PIPES, new K12a()).build();
		x.get("/checkQuery").run().assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D").assertBody().urlDecodedIs("Foo=x['foo','bar']");
		s.set(AList.of("bar","baz"));
		x.get("/checkQuery").run().assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D").assertBody().urlDecodedIs("Foo=x['bar','baz']");
	}

	@Test
	public void i07_query_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));
		RestClient x = MockRestClient.create(A.class).simpleJson().query("Foo", s).build();
		x.get("/checkQuery").run().assertBody().is("Foo=foo%2Cbar").assertBody().urlDecodedIs("Foo=foo,bar");
		s.set(AList.of("bar","baz"));
		x.get("/checkQuery").run().assertBody().is("Foo=bar%2Cbaz").assertBody().urlDecodedIs("Foo=bar,baz");
	}

	@Test
	public void i08_query_withNull() throws Exception {
		MockRestClient.create(A.class).simpleJson().queries(BasicNameValuePair.of("Foo","bar"), null).build().get("/checkQuery").run().assertBody().is("Foo=bar");
	}

	@Test
	public void i09_queryPairs() throws Exception {
		MockRestClient.create(A.class).simpleJson().queryPairs("foo","bar","baz","qux").build().get("/checkQuery").run().assertBody().is("foo=bar&baz=qux");
		MockRestClient.create(A.class).simpleJson().build().get("/checkQuery").queryPairs("foo","bar","baz","qux").run().assertBody().is("foo=bar&baz=qux");
		MockRestClient.create(A.class).simpleJson().queryPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).build().get("/checkQuery").run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2");
		MockRestClient.create(A.class).simpleJson().build().get("/checkQuery").queryPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2");
		try {
			MockRestClient.create(A.class).simpleJson().queryPairs("foo","bar","baz"); fail();
		} catch (Exception e) {
			assertThrowable(e).contains("Odd number of parameters");
		}
		try {
			MockRestClient.create(A.class).simpleJson().build().get().queryPairs("foo","bar","baz"); fail();
		} catch (Exception e) {
			assertThrowable(e).contains("Odd number of parameters");
		}
	}

	public static class I11 {
		public String foo;

		I11 init() {
			this.foo = "baz";
			return this;
		}
	}

	@Test
	public void i11_query_request() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/echo").query("foo", "bar").run().assertBody().contains("GET /echo?foo=bar");
		MockRestClient.create(A.class).simpleJson().build().get("/echo").query("foo", AList.of("bar","baz"), T_ARRAY_PIPES).run().assertBody().contains("GET /echo?foo=bar%7Cbaz");
		MockRestClient.create(A.class).simpleJson().query("foo","bar").build().get("/echo").query(EnumSet.of(AddFlag.PREPEND), "foo", "baz").run().assertBody().contains("GET /echo?foo=baz&foo=bar");
		MockRestClient.create(A.class).simpleJson().query("foo","bar").build().get("/echo").query(EnumSet.of(AddFlag.PREPEND), "foo", AList.of("baz","qux"), T_ARRAY_PIPES).run().assertBody().contains("GET /echo?foo=baz%7Cqux&foo=bar");
		MockRestClient.create(A.class).simpleJson().query("foo","bar").build().get("/echo").queries(BasicNameValuePair.of("foo","baz"), NameValuePairs.of("foo","qux"), OMap.of("foo","quux")).run().assertBody().contains("GET /echo?foo=bar&foo=baz&foo=qux&foo=quux");
		MockRestClient.create(A.class).simpleJson().query("foo","bar").build().get("/echo").queries(new I11().init()).run().assertBody().contains("GET /echo?foo=bar&foo=baz");
		try {
			MockRestClient.create(A.class).simpleJson().build().get("/echo").queries("foo=baz"); fail();
		} catch (RestCallException e) {
			assertEquals("Invalid type passed to queries(): java.lang.String", e.getMessage());
		}
	}

	@Test
	public void i12_query_NameValuePair() throws Exception {
		MockRestClient.create(A.class).simpleJson().query(BasicNameValuePair.of("foo", "bar")).build().get("/echo").query(BasicNameValuePair.of("foo", "baz")).run().assertBody().contains("GET /echo?foo=bar&foo=baz");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Form data
	//-----------------------------------------------------------------------------------------------------------------
	@Test
	public void j01_formData_basic() throws Exception {
		MockRestClient.create(A.class).simpleJson().formData("Foo","bar").formData("Foo",new StringBuilder("baz")).build().post("/checkFormData").run().assertBody().is("Foo=bar&Foo=baz");
	}

	@Test
	public void j02_formData_objects() throws Exception {
		MockRestClient.create(A.class).simpleJson().formDatas(BasicNameValuePair.of("Foo","f1")).formDatas(OMap.of("Foo","f2")).formDatas(AMap.of("Foo","f3")).formDatas(NameValuePairs.of("Foo","f4","Foo","f5")).formDatas(BasicNameValuePair.of("Foo","f6"), BasicNameValuePair.of("Foo","f7")).formDatas((Object)new NameValuePair[]{BasicNameValuePair.of("Foo","f8")}).build().post("/checkFormData").run().assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7&Foo=f8");
	}

	@Test
	public void j03_formData_withSchema() throws Exception {
		MockRestClient.create(A.class).simpleJson().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES).build().post("/checkFormData").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz")
		;
	}

	@Test
	public void j03_formData_withSchemaAndSerializer() throws Exception {
		MockRestClient.create(A.class).simpleJson().formData("Foo",AList.of("bar","baz"), T_ARRAY_PIPES, UonSerializer.DEFAULT).build().post("/checkFormData").run().assertBody().urlDecodedIs("Foo=@(bar,baz)")
		;
	}

	@Test
	public void j04_formData_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));

		RestClient rc= MockRestClient.create(A.class).simpleJson().formData("Foo", s).build();

		rc.post("/checkFormData").run().assertBody().is("Foo=foo%2Cbar").assertBody().urlDecodedIs("Foo=foo,bar")
		;

		s.set(OList.of("bar","baz"));

		rc.post("/checkFormData").run().assertBody().is("Foo=bar%2Cbaz").assertBody().urlDecodedIs("Foo=bar,baz")
		;
	}

	@Test
	public void j05_formData_withSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));

		RestClient rc= MockRestClient.create(A.class).simpleJson().formData("Foo", s, T_ARRAY_PIPES, new K12a()).build();

		rc.post("/checkFormData").run().assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D").assertBody().urlDecodedIs("Foo=x['foo','bar']")
		;

		s.set(OList.of("bar","baz"));

		rc.post("/checkFormData").run().assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D").assertBody().urlDecodedIs("Foo=x['bar','baz']")
		;
	}

	@Test
	public void j06_formData_withSupplierAndSchema() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));

		RestClient rc= MockRestClient.create(A.class).simpleJson().formData("Foo", s, T_ARRAY_PIPES).build();

		rc.post("/checkFormData").run().assertBody().is("Foo=foo%7Cbar").assertBody().urlDecodedIs("Foo=foo|bar")
		;

		s.set(AList.of("bar","baz"));

		rc.post("/checkFormData").run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz")
		;
	}

	@Test
	public void j07_formData_withNull() throws Exception {
		MockRestClient.create(A.class).simpleJson().formDatas(BasicNameValuePair.of("Foo","bar"), null).build().post("/checkFormData").run().assertBody().is("Foo=bar");
	}

	@Test
	public void j08_formData_invalid() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().formDatas(BasicNameValuePair.of("Foo","bar"), "Baz"); fail();
		} catch (Exception e) {
			assertEquals("Invalid type passed to formData():  java.lang.String", e.getMessage());
		}
	}

	@Test
	public void j09_formDataPairs() throws Exception {
		MockRestClient.create(A.class).simpleJson().formDataPairs("foo","bar","baz","qux").build().post("/checkFormData").run().assertBody().is("foo=bar&baz=qux")
		;
		MockRestClient.create(A.class).simpleJson().formDataPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2")).build().post("/checkFormData").run().assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2").assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2")
		;
	}

	@Test
	public void j10_formDataPairs_invalid() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().formDataPairs("foo","bar","baz"); fail();
		} catch (Exception e) {
			assertEquals("Odd number of parameters passed into formDataPairs(Object...)", e.getMessage());
		}
	}

	@Test
	public void j11_formData_serializedHeaderBuilder() throws Exception {
		MockRestClient.create(A.class).simpleJson().formDatas(SerializedHeader.create().name("foo").value("bar")).build().post("/checkFormData").run().assertBody().is("foo=bar");
	}

	@Test
	public void j12_formData_NameValuePair() throws Exception {
		MockRestClient.create(A.class).simpleJson().formData(BasicNameValuePair.of("foo", "bar")).build().post("/checkFormData").formData(BasicNameValuePair.of("foo", "baz")).run().assertBody().is("foo=bar&foo=baz");
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
			request.addHeader("Check", "Foo");
			request.addHeader("Foo", "baz");
			return super.execute(target, request, context);
		}
	}

	@Test
	public void k01_restClient_CallHandler() throws Exception {
		MockRestClient.create(A.class).simpleJson().callHandler(K1.class).header("Foo", "f1").build().get("/checkHeader").header("Foo","f2").run().assertBody().is("['f1','f2','baz']");
		MockRestClient.create(A.class).simpleJson().callHandler(new RestCallHandler() {
				@Override
				public HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
					return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, null));
				}
				@Override
				public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
					return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, null));
				}
			}).header("Foo", "f1").build().get("/checkHeader").header("Foo","f2").run().assertStatus().is(201);
	}

	@Test
	public void k02_restClient_errorCodes() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().errorCodes(x -> x == 200).ignoreErrors(false).build().get("/echo").run(); fail();
		} catch (RestCallException e) {
			assertEquals(200, e.getResponseCode());
		}
		try {
			MockRestClient.create(A.class).simpleJson().ignoreErrors(false).build().get("/echo").errorCodes(x -> x == 200).run(); fail();
		} catch (RestCallException e) {
			assertEquals(200, e.getResponseCode());
		}
	}

	@Test
	public void k03_restClient_executorService() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
		RestClient rc = MockRestClient.create(A.class).simpleJson().executorService(es, true).build();
		assertEquals(es, rc.getExecutorService());
		rc.get("/echo").runFuture().get().assertStatus().is(200).assertBody().contains("HTTP GET /echo");

		es = null;
		rc = MockRestClient.create(A.class).simpleJson().executorService(es, true).build();
		assertNotNull(rc.getExecutorService());
		rc.get("/echo").runFuture().get().assertStatus().is(200).assertBody().contains("HTTP GET /echo");
	}

	@Test
	public void k04_restClient_keepHttpClientOpen() throws Exception {
		RestClient rc = MockRestClient.create(A.class).simpleJson().keepHttpClientOpen().build();
		CloseableHttpClient c = rc.httpClient;
		rc.close();
		MockRestClient.create(A.class).simpleJson().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");
		rc = MockRestClient.create(A.class).simpleJson().keepHttpClientOpen().build();
		c = rc.httpClient;
		rc.close();
		MockRestClient.create(A.class).simpleJson().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");
	}

	public static class K5 extends BasicRestCallInterceptor {
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
		MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(K5.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111, K5.x);

		MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(new K5()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111, K5.x);

		MockRestClient.create(A.class).simpleJson().header("Foo","f1").build().get("/checkHeader").interceptors(new K5()).header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111, K5.x);
		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(K5a.class).build().get("/checkHeader"); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(K5b.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(K5c.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(new K5a()).build().get("/checkHeader"); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(new K5b()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").interceptors(new K5c()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").build().get("/checkHeader").interceptors(new K5a()); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").build().get("/checkHeader").interceptors(new K5b()).header("Check","foo").header("Foo","f3").run(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().header("Foo","f1").build().get("/checkHeader").interceptors(new K5c()).header("Check","foo").header("Foo","f3").run().close(); fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().interceptors(String.class); fail();
		} catch (ConfigException e) {
			assertEquals("Invalid class of type 'java.lang.String' passed to interceptors().", e.getMessage());
		}
		try {
			MockRestClient.create(A.class).simpleJson().interceptors(""); fail();
		} catch (ConfigException e) {
			assertEquals("Invalid object of type 'java.lang.String' passed to interceptors().", e.getMessage());
		}

		MockRestClient.create(A.class).simpleJson().interceptors((Object)null).header("Foo","f1").build().get("/checkHeader");

		MockRestClient.create(A.class).simpleJson().interceptors((Class<?>)null).header("Foo","f1").build().get("/checkHeader");
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
		try {
			MockRestClient.create(A.class).simpleJson().interceptors(K6a.class).build().post("/bean", bean).complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
		try {
			MockRestClient.create(A.class).simpleJson().interceptors(K6b.class).build().post("/bean", bean).complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
		try {
			MockRestClient.create(A.class).simpleJson().interceptors(K6c.class).build().post("/bean", bean).complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
	}

	public static class K7 extends RestClient {
		private static String lastMessage;

		public K7(PropertyStore ps) {
			super(ps);
		}

		@Override
		public void log(Level level, String msg, Object...args) {
			lastMessage = msg;
		}
	}

	@Test
	public void k07_restClient_leakDetection() throws Throwable {
		MockRestClient.create(A.class).simpleJson().leakDetection().build(K7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.", K7.lastMessage);

		MockRestClient.create(A.class).simpleJson().debug().build(K7.class).finalize();
		assertTrue(K7.lastMessage.startsWith("WARNING:  RestClient garbage collected before it was finalized.\nCreation Stack:\n\t"));

		MockRestClient.create(A.class).simpleJson().leakDetection().build(K7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.", K7.lastMessage);
	}

	@Test
	public void k08_restClient_marshall() throws Exception {
		RestClient rc = MockRestClient.create(A.class).marshall(Xml.DEFAULT).build();

		ABean b = rc.post("/echoBody", bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);

		assertObject(b).sameAs(bean);
	}

	@Test
	public void k09_restClient_marshalls() throws Exception {
		RestClient rc = MockRestClient.create(A.class).marshalls(Xml.DEFAULT,Json.DEFAULT).build();

		rc.post("/echoBody", bean).run().assertBody().is("{f:1}");

		ABean b = rc.post("/echoBody", bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		b = rc.post("/echoBody", bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
	}

	@Test
	public void k10_restClient_serializer_parser() throws Exception {
		RestClient rc = MockRestClient.create(A.class).serializer(XmlSerializer.class).parser(XmlParser.class).build();

		ABean b = rc.post("/echoBody", bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);

		assertObject(b).sameAs(bean);

		rc = MockRestClient.create(A.class).serializer(XmlSerializer.DEFAULT).parser(XmlParser.DEFAULT).build();

		b = rc.post("/echoBody", bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);

		assertObject(b).sameAs(bean);

		try {
			MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers, String.class).build(); fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_serializers property had invalid class of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers, "").build(); fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_serializers property had invalid object of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers, String.class).build(); fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_parsers property had invalid class of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers, "").build(); fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_parsers property had invalid object of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}
	}

	@Test
	public void k11_restClient_serializers_parsers() throws Exception {
		@SuppressWarnings("unchecked")
		RestClient rc = MockRestClient.create(A.class).serializers(XmlSerializer.class,JsonSerializer.class).parsers(XmlParser.class,JsonParser.class).build();

		rc.post("/echoBody", bean).run().assertBody().is("{f:1}");

		ABean b = rc.post("/echoBody", bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		b = rc.post("/echoBody", bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		rc = MockRestClient.create(A.class).serializers(XmlSerializer.DEFAULT,JsonSerializer.DEFAULT).parsers(XmlParser.DEFAULT,JsonParser.DEFAULT).build();

		rc.post("/echoBody", bean).run().assertBody().is("{f:1}");

		b = rc.post("/echoBody", bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);

		b = rc.post("/echoBody", bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).sameAs(bean);
	}

	@Rest(partSerializer=K12a.class, partParser=K12b.class)
	public static class K12 extends BasicRest {
		@RestMethod(path="/")
		public Ok get(@Header(name="Foo",multi=true) ABean[] foo, org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws Exception {
			assertEquals(2, foo.length);
			assertObject(req.getHeaders().getAll("Foo", String[].class)).json().is("['x{f:1}','x{f:1}']");
			assertEquals("{f:1}", foo[0].toString());
			assertEquals("{f:1}", foo[1].toString());
			res.header("Foo", bean);
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
					return "x" + super.serialize(type, schema, value);
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
						return SimpleJson.DEFAULT.read(in.substring(1), toType);
					return super.parse(null, schema, in, toType);
				}
			};
		}
	}

	@Test
	public void k12_restClient_partSerializer_partParser() throws Exception {
		RestClient rc = MockRestClient.create(K12.class).simpleJson().header("Foo",bean).partSerializer(K12a.class).partParser(K12b.class).build();
		ABean b = rc.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}", b.toString());
		b = rc.get().header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}", b.toString());

		rc = MockRestClient.create(K12.class).simpleJson().header("Foo",bean).partSerializer(new K12a()).partParser(new K12b()).build();
		b = rc.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}", b.toString());
	}


	@Test
	public void k13_restClient_toString() throws Exception {
		String s = MockRestClient.create(A.class).simpleJson().rootUrl("foo").build().toString();
		assertTrue(s.contains("rootUri: 'foo'"));
	}

	@Test
	public void k14_restClient_request_target() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/bean").target(new HttpHost("localhost")).run().assertBody().is("{f:1}");
	}

	@Test
	public void k15_restClient_request_context() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/bean").context(new BasicHttpContext()).run().assertBody().is("{f:1}");
	}

	@Test
	public void k16_restClient_request_uriParts() throws Exception {
		java.net.URI uri = MockRestClient.create(A.class).simpleJson().build().get().scheme("http").host("localhost").port(8080).userInfo("foo:bar").uri("/bean").fragment("baz").query("foo", "bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient.create(A.class).simpleJson().build().get().scheme("http").host("localhost").port(8080).userInfo("foo","bar").uri("/bean").fragment("baz").query("foo", "bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient.create(A.class).simpleJson().build().get().uri("http://localhost").uri("http://foo:bar@localhost:8080/bean?foo=bar#baz").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient.create(A.class).simpleJson().build().get().uri(new java.net.URI(null, null, null, null)).uri(new java.net.URI("http://foo:bar@localhost:8080/bean?foo=bar#baz")).run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());
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

	@Test
	public void l01_serializer_addBeanTypes() throws Exception {
		L1 l1 = new L1().init();

		MockRestClient.create(A.class).simpleJson().addBeanTypes().build().post("/echoBody", l1).run().assertBody().is("{f1:{_type:'L',f2:1}}");
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
	public void l02_serializer_addRootType() throws Exception {
		L2 l2 = new L2().init();

		MockRestClient.create(A.class).simpleJson().addBeanTypes().addRootType().build().post("/echoBody", l2).run().assertBody().is("{_type:'L',f2:1}");
	}

	@Test
	public void l03_serializer_detectRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;

		try {
			MockRestClient.create(A.class).simpleJson().detectRecursions().build().post("/echoBody", l1).run(); fail();
		} catch (RestCallException e) {
			assertTrue(e.getCause(SerializeException.class).getMessage().startsWith("Recursion occurred"));
		}
	}

	@Test
	public void l04_serializer_ignoreRecursions() throws Exception {
		L1 l1 = new L1();
		l1.f1 = l1;

		MockRestClient.create(A.class).simpleJson().ignoreRecursions().build().post("/echoBody", l1).run().assertBody().is("{}");
	}

	@Test
	public void l05_serializer_initialDepth() throws Exception {
		MockRestClient.create(A.class).simpleJson().initialDepth(2).ws().build().post("/echoBody", bean).run().assertBody().is("\t\t{\n\t\t\tf: 1\n\t\t}");
	}

	public static class L6 {
		public ABean f;

		public L6 init() {
			f = bean;
			return this;
		}
	}

	@Test
	public void l06_serializer_maxDepth() throws Exception {
		MockRestClient.create(A.class).simpleJson().maxDepth(1).build().post("/echoBody", new L6().init()).run().assertBody().is("{}");
	}

	@Test
	public void l07_serializer_sortCollections() throws Exception {
		String[] x = new String[]{"c","a","b"};

		MockRestClient.create(A.class).simpleJson().sortCollections().build().post("/echoBody", x).run().assertBody().is("['a','b','c']");
	}

	@Test
	public void l08_serializer_sortMapsBoolean() throws Exception {
		AMap<String,Integer> x = AMap.of("c", 3, "a", 1, "b", 2);

		MockRestClient.create(A.class).simpleJson().sortMaps().build().post("/echoBody", x).run().assertBody().is("{a:1,b:2,c:3}");
	}

	public static class L9 {
		public List<String> f1 = AList.of();
		public String[] f2 = new String[0];
	}

	@Test
	public void l09_serializer_trimEmptyCollections() throws Exception {
		L9 x = new L9();

		MockRestClient.create(A.class).simpleJson().trimEmptyCollections().build().post("/echoBody", x).run().assertBody().is("{}");
	}

	public static class L10 {
		public Map<String,String> f1 = AMap.of();
		public OMap f2 = OMap.of();
	}

	@Test
	public void l10_serializer_trimEmptyMaps() throws Exception {
		L10 x = new L10();

		MockRestClient.create(A.class).simpleJson().trimEmptyMaps().build().post("/echoBody", x).run().assertBody().is("{}");
	}

	public static class L11 {
		public String f;
	}

	@Test
	public void l11_serializer_trimNullPropertiesBoolean() throws Exception {
		L11 x = new L11();

		MockRestClient.create(A.class).simpleJson().keepNullProperties().build().post("/echoBody", x).run().assertBody().is("{f:null}");
	}

	public static class L12 {
		public String f = " foo ";
	}

	@Test
	public void l12_serializer_trimStringsOnWrite() throws Exception {
		L12 x = new L12();

		MockRestClient.create(A.class).simpleJson().trimStringsOnWrite().build().post("/echoBody", x).run().assertBody().is("{f:'foo'}");
	}

	public static class L13 {
		@URI
		public String f = "foo";
	}

	@Test
	public void l13_serializer_uriContext_uriResolution_uriRelativity() throws Exception {
		L13 x = new L13();

		MockRestClient.create(A.class).simpleJson().uriResolution(UriResolution.ABSOLUTE).uriRelativity(UriRelativity.PATH_INFO).uriContext(UriContext.of("http://localhost:80", "/context", "/resource", "/path")).build().post("/echoBody", x).run().assertBody().is("{f:'http://localhost:80/context/resource/foo'}");

		MockRestClient.create(A.class).simpleJson().uriResolution(UriResolution.NONE).uriRelativity(UriRelativity.RESOURCE).uriContext(UriContext.of("http://localhost:80", "/context", "/resource", "/path")).build().post("/echoBody", x).run().assertBody().is("{f:'foo'}");
	}

	public static class L14 {
		public int f1;
		public L14 f2;

		public L14 init() {
			L14 x2 = new L14(), x3 = new L14();
			this.f1 = 1;
			x2.f1 = 2;
			x3.f1 = 3;
			this.f2 = x2;
			x2.f2 = x3;
			return this;
		}
	}

	@Test
	public void l14_serializer_maxIndent() throws Exception {
		L14 x = new L14().init();

		MockRestClient.create(A.class).simpleJson().maxIndent(2).ws().build().post("/echoBody", x).run().assertBody().is("{\n\tf1: 1,\n\tf2: {\n\t\tf1: 2,\n\t\tf2: {f1:3}\n\t}\n}");
	}

	public static class L15 {
		public String f1 = "foo";
	}

	@Test
	public void l15_serializer_quoteChar() throws Exception {
		L15 x = new L15();

		MockRestClient.create(A.class).json().quoteChar('\'').build().post("/echoBody", x).run().assertBody().is("{'f1':'foo'}");

		MockRestClient.create(A.class).json().quoteChar('|').build().post("/echoBody", x).run().assertBody().is("{|f1|:|foo|}");

		MockRestClient.create(A.class).simpleJson().quoteChar('|').build().post("/echoBody", x).run().assertBody().is("{f1:|foo|}");
	}

	@Test
	public void l16_serializer_sq() throws Exception {
		L15 x = new L15();

		MockRestClient.create(A.class).json().sq().build().post("/echoBody", x).run().assertBody().is("{'f1':'foo'}");

		MockRestClient.create(A.class).simpleJson().sq().build().post("/echoBody", x).run().assertBody().is("{f1:'foo'}");
	}

	@Test
	public void l17_serializer_useWhitespace() throws Exception {
		L15 x = new L15();

		MockRestClient.create(A.class).simpleJson().ws().build().post("/echoBody", x).run().assertBody().is("{\n\tf1: 'foo'\n}");

		MockRestClient.create(A.class).simpleJson().useWhitespace().build().post("/echoBody", x).run().assertBody().is("{\n\tf1: 'foo'\n}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parser properties
	//-----------------------------------------------------------------------------------------------------------------


	@Test
	public void m01_parser_debugOutputLines() throws Exception {
		RestClient rc = null;

		rc = MockRestClient.create(A.class).simpleJson().debugOutputLines(10).build();

		assertEquals(10, rc.parsers.getParser("application/json").toMap().getMap("Parser").getInt("debugOutputLines").intValue());
	}

	public static class M2 {
		public int f;
	}

	@Test
	public void m02_parser_strict() throws Exception {
		try {
			MockRestClient.create(A.class).json().strict().build().post("/echoBody", new StringReader("{f:1}")).run().getBody().as(M2.class); fail();
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Unquoted attribute detected."));
		}
	}

	public static class M3 {
		public String f;
	}

	@Test
	public void m03_parser_trimStringsOnRead() throws Exception {
		M3 x = MockRestClient.create(A.class).simpleJson().trimStringsOnRead().build().post("/echoBody", new StringReader("{f:' 1 '}")).run().getBody().as(M3.class);
		assertEquals("1", x.f);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OpenApi properties
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void n01_openApi_oapiFormat() throws Exception {
		MockRestClient.create(A.class).oapiFormat(HttpPartFormat.UON).build().get("/checkQuery").query("Foo", "bar baz").run().assertBody().is("Foo=%27bar+baz%27").assertBody().urlDecodedIs("Foo='bar baz'")
		;
	}

	@Test
	public void n02_openApi_oapiCollectionFormat() throws Exception {
		RestClient rc = MockRestClient.create(A.class).oapiCollectionFormat(HttpPartCollectionFormat.PIPES).build();

		rc.get("/checkQuery").query("Foo", new String[]{"bar","baz"}).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz")
		;

		rc.post("/checkFormData").formData("Foo", new String[]{"bar","baz"}).run().assertBody().is("Foo=bar%7Cbaz").assertBody().urlDecodedIs("Foo=bar|baz")
		;

		rc.get("/checkHeader").header("Check", "Foo").header("Foo", new String[]{"bar","baz"}).accept("text/json+simple").run().assertBody().is("['bar|baz']")
		;
	}

	@Test
	public void n03_urlEnc_paramFormat() throws Exception {
		 OMap map = OMap.of(
			"foo", "bar",
			"baz", new String[]{"qux", "true", "123"}
		);

		 MockRestClient.create(A.class).urlEnc().paramFormat(ParamFormat.PLAINTEXT).build().post("/echoBody", map).run().assertBody().is("foo=bar&baz=qux,true,123");

		 MockRestClient.create(A.class).urlEnc().paramFormatPlain().build().post("/echoBody", map).run().assertBody().is("foo=bar&baz=qux,true,123");

		 MockRestClient.create(A.class).urlEnc().paramFormat(ParamFormat.UON).build().post("/echoBody", map).run().assertBody().is("foo=bar&baz=@(qux,'true','123')");
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
		RestClient rc1 = MockRestClient.create(A.class).simpleJson().build();

		RestClient rc2 = MockRestClient.create(A.class).beanClassVisibility(Visibility.PROTECTED).simpleJson().build();

		rc1.post("/echoBody", new O1()).run().assertBody().is("'O1'")
		;
		rc2.post("/echoBody", new O1()).run().assertBody().is("{f:1}")
		;

		rc1.get("/checkQuery").query("foo", new O1()).run().assertBody().is("foo=O1")
		;
		rc2.get("/checkQuery").query("foo", new O1()).run().assertBody().is("foo=f%3D1").assertBody().urlDecodedIs("foo=f=1")
		;

		rc1.formPost("/checkFormData").formData("foo", new O1()).run().assertBody().is("foo=O1")
		;
		rc2.formPost("/checkFormData").formData("foo", new O1()).run().assertBody().is("foo=f%3D1").assertBody().urlDecodedIs("foo=f=1")
		;

		rc1.get("/checkHeader").header("foo", new O1()).header("Check", "foo").run().assertBody().is("['O1']")
		;
		rc2.get("/checkHeader").header("foo", new O1()).header("Check", "foo").run().assertBody().is("['f=1']")
		;
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
		public Reader postTest(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) throws IOException {
			res.setHeader("X", req.getHeaders().getString("X"));
			return req.getBody().getReader();
		}
	}

	@Test
	public void o02_beanContext_beanConstructorVisibility() throws Exception {
		RestResponse rr = MockRestClient.create(O2b.class).beanConstructorVisibility(Visibility.PROTECTED).simpleJson().build().post("/test", new O2a(1)).header("X", new O2a(1)).run().cacheBody().assertBody().is("1").assertHeader("X").is("1")
		;
		assertEquals(1, rr.getBody().as(O2a.class).f);
		assertEquals(1, rr.getHeader("X").as(O2a.class).f);
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
		RestResponse rr = MockRestClient.create(O2b.class).beanFieldVisibility(Visibility.PROTECTED).simpleJson().build().post("/test", new O3().init()).header("X", new O3().init()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O3.class).f2);
		assertEquals(2, rr.getHeader("X").as(O3.class).f2);
	}

	public static interface O4a {
		int getF3();
		void setF3(int f3);
	}

	public static class O4b implements O4a {
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

		O4b init() {
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
		RestResponse rr = MockRestClient.create(O2b.class).bpi(O4b.class, "f1").simpleJson().build().post("/test", new O4b().init()).header("X", new O4b().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4b.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4b.class).f2);

		rr = MockRestClient.create(O2b.class).bpi(O4b.class, "f1").simpleJson().build().post("/test", new O4b().init()).header("X", new O4b().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4b.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4b.class).f2);

		rr = MockRestClient.create(O2b.class).bpi(O4b.class, "f1").simpleJson().build().post("/test", new O4b().init()).header("X", new O4b().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4b.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4b.class).f2);

		rr = MockRestClient.create(O2b.class).bpi(O4b.class, "f1").simpleJson().build().post("/test", new O4b().init()).header("X", new O4b().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals(0, rr.getBody().as(O4b.class).f2);
		assertEquals(0, rr.getHeader("X").as(O4b.class).f2);

		rr = MockRestClient.create(O2b.class).interfaces(O4a.class).simpleJson().build().post("/test", new O4b().init()).header("X", new O4b().init()).run().cacheBody().assertBody().is("{f3:3}").assertHeader("X").is("f3=3")
		;
		assertEquals(3, rr.getBody().as(O4b.class).f3);
		assertEquals(3, rr.getHeader("X").as(O4b.class).f3);
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
		RestResponse rr = MockRestClient.create(O2b.class).beanMethodVisibility(Visibility.PROTECTED).simpleJson().build().post("/test", new O5().init()).header("X", new O5().init()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals(2, rr.getBody().as(O5.class).f2);
		assertEquals(2, rr.getHeader("X").as(O5.class).f2);
	}

	public static class O6 {}

	@Test
	public void o06_beanContext_beansDontRequireSomeProperties() throws Exception {
		MockRestClient.create(A.class).beansDontRequireSomeProperties().simpleJson().build().post("/echoBody", new O6()).run().assertBody().is("{}")
		;
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
		MockRestClient.create(O2b.class).simpleJson().build().post("/test", new O7("1")).header("X", new O7("1")).run().assertBody().is("{f1:'1'}").assertHeader("X").is("f1=1")
		;
		MockRestClient.create(O2b.class).simpleJson().beansRequireDefaultConstructor().build().post("/test", new O7("1")).header("X", new O7("1")).run().assertBody().is("'1'").assertHeader("X").is("1")
		;
	}

	@Test
	public void o08_beanContext_beansRequireSerializable() throws Exception {
		MockRestClient.create(O2b.class).simpleJson().build().post("/test", new O7("1")).header("X", new O7("1")).run().assertBody().is("{f1:'1'}").assertHeader("X").is("f1=1")
		;
		MockRestClient.create(O2b.class).simpleJson().beansRequireSerializable().build().post("/test", new O7("1")).header("X", new O7("1")).run().assertBody().is("'1'").assertHeader("X").is("1")
		;
	}

	public static class O9 {
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
	public void o09_beanContext_beansRequireSettersForGetters() throws Exception {
		MockRestClient.create(O2b.class).simpleJson().build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().beansRequireSettersForGetters().build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
	}

	@Test
	public void o10_beanContext_bpi() throws Exception {
		MockRestClient.create(O2b.class).simpleJson().bpi(OMap.of("O9", "f2")).build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpi(O9.class, "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpi("O9", "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpi(O9.class.getName(), "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
	}

	@Test
	public void o11_beanContext_bpro() throws Exception {
		RestResponse rr = null;

		rr = MockRestClient.create(O2b.class).simpleJson().bpro(OMap.of("09", "f2")).build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());

		rr = MockRestClient.create(O2b.class).simpleJson().bpro(O9.class, "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());

		rr = MockRestClient.create(O2b.class).simpleJson().bpro("O9", "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1,f2:2}").assertHeader("X").is("f1=1,f2=2")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());
	}

	@Test
	public void o12_beanContext_bpwo() throws Exception {
		RestResponse rr = null;

		rr = MockRestClient.create(O2b.class).simpleJson().bpwo(OMap.of("O9", "f2")).build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());

		rr = MockRestClient.create(O2b.class).simpleJson().bpwo(O9.class, "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());

		rr = MockRestClient.create(O2b.class).simpleJson().bpwo("O9", "f2").build().post("/test", new O9().init()).header("X", new O9().init()).run().cacheBody().assertBody().is("{f1:1}").assertHeader("X").is("f1=1")
		;
		assertEquals("1/0", rr.getBody().as(O9.class).toString());
		assertEquals("1/0", rr.getHeader("X").as(O9.class).toString());
	}

	@Test
	public void o13_beanContext_bpx() throws Exception {
		MockRestClient.create(O2b.class).simpleJson().bpx(OMap.of("O9", "f1")).build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpx(O9.class, "f1").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpx("O9", "f1").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
		MockRestClient.create(O2b.class).simpleJson().bpx(O9.class.getName(), "f1").build().post("/test", new O9().init()).header("X", new O9().init()).run().assertBody().is("{f2:2}").assertHeader("X").is("f2=2")
		;
	}

	public static class O14 {
		public Object f;
	}

	@Test
	public void o14_beanContext_debug() throws Exception {
		O14 x = new O14();
		x.f = x;
		try {
			MockRestClient.create(A.class).simpleJson().debug().build().post("/echo", x).run(); fail();
		} catch (RestCallException e) {
			assertTrue(e.getCause(SerializeException.class).getMessage().startsWith("Recursion occurred"));
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="foo")
	public static class O15a {
		public String foo;

		public O15a init() {
			foo = "1";
			return this;
		}
	}

	@org.apache.juneau.annotation.Bean(typeName="bar")
	public static class O15b {
		public String foo;

		public O15b init() {
			foo = "2";
			return this;
		}
	}

	public static class O15c {
		public Object foo;

		public O15c init() {
			foo = new O15a().init();
			return this;
		}
	}

	@Test
	public void o15_beanContext_dictionary() throws Exception {
		Object o = MockRestClient.create(A.class).simpleJson().dictionary(O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody", new O15a().init()).run().cacheBody().assertBody().contains("{_type:'foo',foo:'1'}").getBody().as(Object.class);
		;
		assertTrue(o instanceof O15a);

		OMap m = OMap.of("x", new O15a().init(), "y", new O15b().init());
		m = MockRestClient.create(A.class).simpleJson().dictionary(O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody", m).run().cacheBody().assertBody().is("{x:{_type:'foo',foo:'1'},y:{_type:'bar',foo:'2'}}").getBody().as(OMap.class);
		;
		assertTrue(m.get("x") instanceof O15a);
		assertTrue(m.get("y") instanceof O15b);

		O15c x = MockRestClient.create(A.class).simpleJson().dictionaryOn(O15c.class,O15a.class,O15b.class).addRootType().addBeanTypes().build().post("/echoBody", new O15c().init()).run().cacheBody().assertBody().is("{foo:{_type:'foo',foo:'1'}}").getBody().as(O15c.class);
		;
		assertTrue(x.foo instanceof O15a);
	}

	public static class O16 {
		private String foo;
		public String getFoo() {
			return foo;
		}
		public O16 init() {
			foo = "foo";
			return this;
		}
	}

	@Test
	public void o16_beanContext_dontIgnorePropertiesWithoutSetters() throws Exception {
		O16 x = MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new O16().init()).run().cacheBody().assertBody().contains("{foo:'foo'}").getBody().as(O16.class);
		;
		assertNull(x.foo);

		try {
			MockRestClient.create(A.class).simpleJson().dontIgnorePropertiesWithoutSetters().build().post("/echoBody", new O16().init()).run().cacheBody().assertBody().contains("{foo:'foo'}").getBody().as(O16.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(BeanRuntimeException.class).getMessage().contains("Setter or public field not defined"));
		}
	}

	public static class O17 {
		public String foo;
		public transient String bar;

		public O17 init() {
			foo = "1";
			bar = "2";
			return this;
		}
	}

	@Test
	public void o17_beanContext_dontIgnoreTransientFields() throws Exception {
		O17 x = MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new O17().init()).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O17.class);
		;
		assertNull(x.bar);

		x = MockRestClient.create(A.class).simpleJson().dontIgnoreTransientFields().build().post("/echoBody", new O17().init()).run().cacheBody().assertBody().contains("{bar:'2',foo:'1'}").getBody().as(O17.class);
		assertEquals("2", x.bar);
	}

	public static class O18 {
		public String foo;
	}

	@Test
	public void o18_beanContext_dontIgnoreUnknownNullBeanProperties() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new StringReader("{foo:'1',bar:null}")).run().cacheBody().assertBody().contains("{foo:'1',bar:null}").getBody().as(O18.class);
		;

		try {
			MockRestClient.create(A.class).simpleJson().dontIgnoreUnknownNullBeanProperties().build().post("/echoBody", new StringReader("{foo:'1',bar:null}")).run().cacheBody().assertBody().contains("{foo:'1',bar:null}").getBody().as(O18.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("Unknown property 'bar'"));
		}
	}

	public static interface O19 {
		public String getFoo();
		public void setFoo(String foo);
	}

	@Test
	public void o19_beanContext_dontUseInterfaceProxies() throws Exception {
		O19 x = MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O19.class);
		;
		assertEquals("1", x.getFoo());

		try {
			MockRestClient.create(A.class).simpleJson().dontUseInterfaceProxies().build().post("/echoBody", new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O19.class);
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("could not be instantiated"));
		}
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
		O20 x = MockRestClient.create(A.class).simpleJson().fluentSetters().build().post("/echoBody", new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O20.class);
		;
		assertEquals("1", x.getFoo());

		x = MockRestClient.create(A.class).simpleJson().fluentSetters(O20.class).build().post("/echoBody", new StringReader("{foo:'1'}")).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O20.class);
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
			throw new RuntimeException("xxx");
		}
		public O21 init() {
			this.foo = "1";
			this.bar = "2";
			return this;
		}
	}

	@Test
	public void o21_beanContext_ignoreInvocationExceptionsOnGetters() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new O21().init()).run(); fail();
		} catch (RestCallException e) {
			assertTrue(e.getCause(SerializeException.class).getMessage().contains("Could not call getValue() on property 'bar'"));
		}

		O21 x = MockRestClient.create(A.class).simpleJson().ignoreInvocationExceptionsOnGetters().build().post("/echoBody", new O21().init()).run().cacheBody().assertBody().contains("{foo:'1'}").getBody().as(O21.class);
		;
		assertEquals("1", x.getFoo());
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
		public O22 init() {
			this.foo = "1";
			this.bar = "2";
			return this;
		}
	}

	@Test
	public void o22_beanContext_ignoreInvocationExceptionsOnSetters() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new O22().init()).run().getBody().as(O22.class); fail();
		} catch (RestCallException e) {
			assertTrue(e.getCause(BeanRuntimeException.class).getMessage().contains("Error occurred trying to set property 'bar'"));
		}

		O22 x = MockRestClient.create(A.class).simpleJson().ignoreInvocationExceptionsOnSetters().build().post("/echoBody", new O22().init()).run().cacheBody().getBody().as(O22.class);
		;
		assertEquals("1", x.getFoo());
	}

	public static class O23 {
		public String foo;
	}

	@Test
	public void o23_beanContext_ignoreUnknownBeanProperties() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().build().post("/echoBody", new StringReader("{foo:'1',bar:'2'}")).run().getBody().as(O23.class); fail();
		} catch (RestCallException e) {
			assertTrue(e.getCause(ParseException.class).getMessage().contains("Unknown property 'bar' encountered"));
		}

		O23 x = MockRestClient.create(A.class).simpleJson().ignoreUnknownBeanProperties().build().post("/echoBody", new StringReader("{foo:'1',bar:'2'}")).run().cacheBody().getBody().as(O23.class);
		;
		assertEquals("1", x.foo);
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
		O24a x = MockRestClient.create(A.class).simpleJson().implClass(O24a.class, O24b.class).build().post("/echoBody", new StringReader("{foo:1}")).run().getBody().as(O24a.class)
		;
		assertEquals(1, x.getFoo());
		assertTrue(x instanceof O24b);

		x = MockRestClient.create(A.class).simpleJson().implClasses(AMap.of(O24a.class, O24b.class)).build().post("/echoBody", new StringReader("{foo:1}")).run().getBody().as(O24a.class)
		;
		assertEquals(1, x.getFoo());
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

		public O25b init() {
			foo = 1;
			bar = 2;
			return this;
		}
	}

	@Test
	public void o25_beanContext_interfaceClass() throws Exception {
		O25a x = MockRestClient.create(A.class).simpleJson().interfaceClass(O25b.class, O25a.class).build().post("/echoBody", new O25b().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O25b.class)
		;
		assertEquals(1, x.getFoo());

		x = MockRestClient.create(A.class).simpleJson().interfaces(O25a.class).build().post("/echoBody", new O25b().init()).run().assertBody().is("{foo:1}").getBody().as(O25b.class)
		;
		assertEquals(1, x.getFoo());
	}

	public static class O26a {
		public int foo;
		public O26a init() {
			foo = 1;
			return this;
		}
	}

	public static class O26b extends StringSwap<O26a> {
		@Override
		public String swap(BeanSession session, O26a o) throws Exception {
			assertEquals(Locale.UK, session.getLocale());
			return super.swap(session, o);
		}

		@Override
		public O26a unswap(BeanSession session, String f, ClassMeta<?> hint) throws Exception {
			assertEquals(Locale.UK, session.getLocale());
			return super.unswap(session, f, hint);
		}
	}

	@Test
	public void o26_beanContext_locale() throws Exception {
		O26a x = MockRestClient.create(A.class).simpleJson().locale(Locale.UK).build().post("/echoBody", new O26a().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O26a.class)
		;
		assertEquals(1, x.foo);
	}

	@Test
	public void o27_beanContext_mediaType() throws Exception {
		O26a x = MockRestClient.create(A.class).simpleJson().mediaType(MediaType.JSON).build().post("/echoBody", new O26a().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O26a.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O28 {
		public int foo;
		public O28 init() {
			foo = 1;
			return this;
		}
		@Override
		public String toString() {
			return String.valueOf(foo);
		}
		public static O28 fromString(String foo) throws ParseException {
			O28 x = new O28();
			x.foo = JsonParser.DEFAULT.parse(foo, int.class);
			return x;
		}
	}

	@Test
	public void o28_beanContext_notBeanClasses() throws Exception {
		O28 x = MockRestClient.create(A.class).simpleJson().notBeanClasses(O28.class).build().post("/echoBody", new O28().init()).run().cacheBody().assertBody().is("'1'").getBody().as(O28.class)
		;
		assertEquals(1, x.foo);
	}

	@Test
	public void o29_beanContext_notBeanPackages() throws Exception {
		O28 x = MockRestClient.create(A.class).simpleJson().notBeanPackages(O28.class.getPackage()).build().post("/echoBody", new O28().init()).run().cacheBody().assertBody().is("'1'").getBody().as(O28.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O30a {
		private String foo;
		public String getFoo() { return foo; }
		public void setFoo(String foo) { this.foo = foo; }
		public O30a init() {
			foo = "foo";
			return this;
		}
	}

	public static class O30b extends BeanInterceptor<O30a> {
		static boolean getterCalled, setterCalled;
		@Override
		public Object readProperty(O30a bean, String name, Object value) {
			getterCalled = true;
			return "x" + value;
		}
		@Override
		public Object writeProperty(O30a bean, String name, Object value) {
			setterCalled = true;
			return value.toString().substring(1);
		}
	}

	@Test
	public void o30_beanContext_beanInterceptor() throws Exception {
		O30a x = MockRestClient.create(A.class).simpleJson().beanInterceptor(O30a.class, O30b.class).build().post("/echoBody", new O30a().init()).run().cacheBody().assertBody().is("{foo:'xfoo'}").getBody().as(O30a.class)
		;
		assertEquals("foo", x.foo);
		assertTrue(O30b.getterCalled);
		assertTrue(O30b.setterCalled);
	}

	public static class O31 {
		private String fooBar;
		public String getFooBar() { return fooBar; }
		public void setFooBar(String fooBar) { this.fooBar = fooBar; }
		public O31 init() {
			fooBar = "fooBar";
			return this;
		}
	}

	@Test
	public void o31_beanContext_propertyNamer() throws Exception {
		O31 x = MockRestClient.create(A.class).simpleJson().propertyNamer(PropertyNamerDLC.class).build().post("/echoBody", new O31().init()).run().cacheBody().assertBody().is("{'foo-bar':'fooBar'}").getBody().as(O31.class)
		;
		assertEquals("fooBar", x.fooBar);

		x = MockRestClient.create(A.class).simpleJson().propertyNamer(O31.class, PropertyNamerDLC.class).build().post("/echoBody", new O31().init()).run().cacheBody().assertBody().is("{'foo-bar':'fooBar'}").getBody().as(O31.class)
		;
		assertEquals("fooBar", x.fooBar);
	}

	public static class O32 {
		public int foo, bar, baz;
		public O32 init() {
			foo = 1;
			bar = 2;
			baz = 3;
			return this;
		}
	}

	@Test
	public void o32_beanContext_sortProperties() throws Exception {
		O32 x = MockRestClient.create(A.class).simpleJson().sortProperties().build().post("/echoBody", new O32().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(O32.class)
		;
		assertEquals(1, x.foo);

		x = MockRestClient.create(A.class).simpleJson().sortProperties(O32.class).build().post("/echoBody", new O32().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(O32.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O33a {
		public int foo;
	}

	public static class O33b extends O33a {
		public int bar;
		public O33b init() {
			foo = 1;
			bar = 2;
			return this;
		}
	}

	@Test
	public void o33_beanContext_stopClass() throws Exception {
		O33b x = MockRestClient.create(A.class).simpleJson().stopClass(O33b.class, O33a.class).build().post("/echoBody", new O33b().init()).run().cacheBody().assertBody().is("{bar:2}").getBody().as(O33b.class)
		;
		assertEquals(0, x.foo);
		assertEquals(2, x.bar);
	}

	public static class O34a {
		public int foo;
		public O34a init() {
			this.foo = 1;
			return this;
		}
	}

	public static class O34b extends PojoSwap<O34a,Integer> {
		@Override
		public Integer swap(BeanSession session, O34a o) { return o.foo; }
		@Override
		public O34a unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return new O34a().init(); }
	}

	@Test
	public void o34_beanContext_swaps() throws Exception {
		O34a x = MockRestClient.create(A.class).simpleJson().swaps(O34b.class).build().post("/echoBody", new O34a().init()).run().cacheBody().assertBody().is("1").getBody().as(O34a.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O35a {
		public int foo;
		public O35a init() {
			foo = 1;
			return this;
		}
	}

	@Test
	public void o35_beanContext_timeZone() throws Exception {
		O35a x = MockRestClient.create(A.class).simpleJson().timeZone(TimeZone.getTimeZone("Z")).build().post("/echoBody", new O35a().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O35a.class)
		;
		assertEquals(1, x.foo);
	}

	public static class O36 {
		public int foo;
		public O36 init() {
			this.foo = 1;
			return this;
		}
	}

	@Test
	public void o36_beanContext_typeName() throws Exception {
		O36 x = MockRestClient.create(A.class).simpleJson().typeName(O36.class, "foo").addRootType().build().post("/echoBody", new O36().init()).run().cacheBody().assertBody().is("{_type:'foo',foo:1}").getBody().as(O36.class)
		;
		assertEquals(1, x.foo);
	}

	@Test
	public void o37_beanContext_typePropertyName() throws Exception {
		O36 x = MockRestClient.create(A.class).simpleJson().typeName(O36.class, "foo").typePropertyName("X").addRootType().build().post("/echoBody", new O36().init()).run().cacheBody().assertBody().is("{X:'foo',foo:1}").getBody().as(O36.class)
		;
		assertEquals(1, x.foo);

		x = MockRestClient.create(A.class).simpleJson().typeName(O36.class, "foo").typePropertyName(O36.class, "X").addRootType().build().post("/echoBody", new O36().init()).run().cacheBody().assertBody().is("{X:'foo',foo:1}").getBody().as(O36.class)
		;
		assertEquals(1, x.foo);
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
		public O38b init() {
			this.foo = O38a.ONE;
			return this;
		}
	}

	@Test
	public void o38_beanContext_useEnumNames() throws Exception {
		O38b x = MockRestClient.create(A.class).simpleJson().useEnumNames().build().post("/echoBody", new O38b().init()).run().cacheBody().assertBody().is("{foo:'ONE'}").getBody().as(O38b.class)
		;
		assertEquals(O38a.ONE, x.foo);
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

		public O39 init() {
			this.foo = 1;
			this.bar = 2;
			return this;
		}
	}

	@Test
	public void o39_beanContext_useJavaIntrospector() throws Exception {
		O39 x = MockRestClient.create(A.class).simpleJson().useJavaBeanIntrospector().build().post("/echoBody", new O39().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(O39.class)
		;
		assertEquals(1, x.foo);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Context properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class P1 {
		public String foo;
	}

	@Test
	public void p01_context_addMap() throws Exception {
		MockRestClient.create(A.class).simpleJson().add(OMap.of(Serializer.SERIALIZER_keepNullProperties, true)).build().post("/echoBody", new P1()).run().cacheBody().assertBody().is("{foo:null}").getBody().as(P1.class);
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
		MockRestClient.create(A.class).simpleJson().addTo(BeanContext.BEAN_notBeanClasses,P2.class).build().post("/echoBody", P2.fromString("bar")).run().cacheBody().assertBody().is("'bar'").getBody().as(P2.class);
	}

	public static class P3a {
		public int foo;
		public P3a init() {
			foo = 1;
			return this;
		}
	}

	public static class P3b extends PojoSwap<P3a,Integer> {
		@Override
		public Integer swap(BeanSession session, P3a o) { return o.foo; }
		@Override
		public P3a unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return new P3a().init(); }
	}

	@Test
	public void p03_context_appendToStringObject() throws Exception {
		P3a x = MockRestClient.create(A.class).simpleJson().appendTo(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody", new P3a().init()).run().cacheBody().assertBody().is("1").getBody().as(P3a.class);
		assertEquals(1, x.foo);
	}

	@Test
	public void p04_context_prependToStringObject() throws Exception {
		P3a x = MockRestClient.create(A.class).simpleJson().prependTo(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody", new P3a().init()).run().cacheBody().assertBody().is("1").getBody().as(P3a.class);
		assertEquals(1, x.foo);
	}

	public static class P5 {
		public int foo;
		public P5 init() {
			this.foo = 1;
			return this;
		}
	}

	@Test
	public void p05_context_apply() throws Exception {
		MockRestClient.create(A.class).json().apply(SimpleJsonSerializer.DEFAULT.getPropertyStore()).build().post("/echoBody", new P5().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P5.class);
	}

	public static class P6a {
		public int foo,bar,baz;
		public P6a init() {
			foo = 1;
			bar = 2;
			baz = 3;
			return this;
		}
	}

	@org.apache.juneau.annotation.Bean(sort=true, on="P6a")
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

		MockRestClient.create(A.class).simpleJson().applyAnnotations(P6b.class).build().post("/echoBody", new P6a().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		MockRestClient.create(A.class).simpleJson().applyAnnotations(P6c.class).build().post("/echoBody", new P6a().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		MockRestClient.create(A.class).simpleJson().applyAnnotations(P6d.class.getMethod("foo")).build().post("/echoBody", new P6a().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
		AnnotationList al = ClassInfo.of(P6c.class).getAnnotationList(ConfigAnnotationFilter.INSTANCE);
		VarResolverSession vr = VarResolver.DEFAULT.createSession();
		MockRestClient.create(A.class).simpleJson().applyAnnotations(al,vr).build().post("/echoBody", new P6a().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
	}

	@Test
	public void p07_context_removeFrom() throws Exception {
		P3a x = MockRestClient.create(A.class).simpleJson().appendTo(BeanContext.BEAN_swaps,P3b.class).removeFrom(BeanContext.BEAN_swaps,P3b.class).build().post("/echoBody", new P3a().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P3a.class);
		assertEquals(1, x.foo);
	}

	@Test
	public void p08_context_setStringObject() throws Exception {
		MockRestClient.create(A.class).json().set(JsonSerializer.JSON_simpleMode,true).build().post("/echoBody", new P3a().init()).run().cacheBody().assertBody().is("{foo:1}").getBody().as(P3a.class);
	}

	@Test
	public void p09_context_annotations() throws Exception {
		MockRestClient.create(A.class).simpleJson().annotations(new BeanAnnotation(P6a.class).sort(true)).build().post("/echoBody", new P6a().init()).run().cacheBody().assertBody().is("{bar:2,baz:3,foo:1}").getBody().as(P6a.class);
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
		P10a x = MockRestClient.create(A.class).simpleJson().putAllTo(BeanContext.BEAN_implClasses, AMap.of(P10a.class.getName(), P10b.class)).build().post("/echoBody", new StringReader("{foo:1}")).run().getBody().as(P10a.class)
		;
		assertEquals(1, x.getFoo());
		assertTrue(x instanceof P10b);
	}

	public static class P11 {
		public int foo = 1;
	}

	@Test
	public void p11_context_set() throws Exception {
		MockRestClient.create(null).set(
				AMap.of(
					JsonSerializer.JSON_simpleMode, true,
					WriterSerializer.WSERIALIZER_quoteChar, "'",
					MockRestClient.MOCKRESTCLIENT_restBean, A.class
				)
			).json().build().post("/echoBody", new P11()).run().assertBody().is("{foo:1}")
		;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path
	//-----------------------------------------------------------------------------------------------------------------

	public static class Q1 {
		public int x;
		public Q1 init() {
			x = 1;
			return this;
		}

		@Override
		public String toString() {
			return "xxx";
		}
	}

	@Test
	public void q01_paths() throws Exception {
		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").path("x", new Q1().init()).run().assertBody().contains("HTTP GET /echo/x=1");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").path(BasicNameValuePair.of("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths(BasicNameValuePair.of("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths(NameValuePairs.of("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths(OMap.of("x","foo")).run().assertBody().contains("HTTP GET /echo/foo");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths((Object)new NameValuePair[]{BasicNameValuePair.of("x","foo")}).run().assertBody().contains("HTTP GET /echo/foo");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths(new Q1().init()).run().assertBody().contains("HTTP GET /echo/1");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").pathPairs("x", 1).run().assertBody().contains("HTTP GET /echo/1");

		MockRestClient.create(A.class).simpleJson().build().get("/echo/*").path("/*", new Q1().init()).run().assertBody().contains("HTTP GET /echo/x=1");

		try {
			MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths("x"); fail();
		} catch (RestCallException e) {
			assertEquals("Invalid type passed to paths(): java.lang.String", e.getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").pathPairs("x"); fail();
		} catch (RestCallException e) {
			assertEquals("Odd number of parameters passed into pathPairs()", e.getMessage());
		}

		try {
			MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").paths((Object)null); fail();
		} catch (RestCallException e) {
			assertEquals("Invalid type passed to paths(): null", e.getMessage());
		}
	}

	@Test
	public void q02_paths_withSchema() throws Exception {
		String[] a = new String[]{"foo","bar"};

		MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").path("x", a, T_ARRAY_PIPES).run().assertBody().contains("HTTP GET /echo/foo%7Cbar")
		;
	}

	@Test
	public void q03_paths_invalid() throws Exception {
		try {
			MockRestClient.create(A.class).simpleJson().build().get("/echo/{x}").path("y", "foo"); fail();
		} catch (RestCallException e) {
			assertEquals("Path variable {y} was not found in path.", e.getMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------
}
