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
import static org.apache.juneau.testutils.TestUtils.*;

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
		public Bean getBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public Bean postBean(@Body Bean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public Bean putBean(@Body Bean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public Bean patchBean(@Body Bean b) {
			return b;
		}
		@RestMethod(path="/bean")
		public Bean deleteBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public Bean optionsBean() {
			return bean;
		}
		@RestMethod(path="/bean")
		public Bean headBean() {
			return bean;
		}
		@RestMethod(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestMethod(path="/echo")
		public String putEcho(org.apache.juneau.rest.RestRequest req) {
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
	public void a01_overrideHttpClient() {
		HttpClientBuilder cb = HttpClientBuilder.create();
		CloseableHttpClient hc = HttpClientBuilder.create().build();
		RestClient.create().httpClientBuilder(cb).build().builder().build();
		RestClient.create().httpClient(hc).build().builder().build();
	}

	public static class A2 extends RestClientBuilder {}

	@Test
	public void a02_useNoArgConstructor() {
		new A2().build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Closing
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a03_close_basic() throws IOException {
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
		RestClient rc = MockRestClient
			.create(A.class)
			.build();
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
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/bean")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get(url)
			.run()
			.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a07_basicCalls_put() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.put("/bean", bean)
			.run()
			.assertBody().is("{f:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.put("/bean")
			.body(bean)
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void a08_basicCalls_put_fromString() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.put("/bean", "{f:1}", "application/json")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.put(url, bean)
				.run()
				.assertBody().is("{f:1}");

			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.put(url, "{f:1}", "application/json")
				.run()
				.assertBody().is("{f:1}");

			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.put(url)
				.body(bean)
				.run()
				.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json")
				.build()
				.put("/bean", body)
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a11_basicCalls_post() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/bean", bean)
			.run()
			.assertBody().is("{f:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/bean")
			.body(bean)
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void a12_basicCalls_post_fromString() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.post("/bean", "{f:1}", "application/json")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post(url, bean)
				.run()
				.assertBody().is("{f:1}");

			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post(url, "{f:1}", "application/json")
				.run()
				.assertBody().is("{f:1}");

			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.post(url)
				.body(bean)
				.run()
				.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json")
				.build()
				.post("/bean", body)
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a15_basicCalls_delete() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.delete("/bean")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.delete(url)
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a17_basicCalls_options() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.options("/bean")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.options(url)
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a19_basicCalls_head() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.head("/bean")
			.run()
			.assertBody().is("");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.head(url)
				.run()
				.assertBody().is("");
		}
	}

	@Test
	public void a21_basicCalls_formPost() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.formPost("/bean", bean)
			.accept("application/json+simple")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.build()
				.formPost(url, bean)
				.accept("application/json+simple")
				.run()
				.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.header("Check", "Content-Type")
				.accept("application/json+simple")
				.build()
				.formPost("/checkHeader", bodies.get(i))
				.run()
				.assertBody().msg("Body {0} failed", i).matchesSimple("['application/x-www-form-urlencoded*']");

			MockRestClient
				.create(A.class)
				.build()
				.formPost("/bean", bodies.get(i))
				.accept("application/json+simple")
				.run()
				.assertBody().msg("Body {0} failed", "#"+i).is("{f:1}");
		}
	}

	@Test
	public void a24_basicCalls_formPostPairs() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.formPostPairs("/bean", new StringBuilder("f"), new StringBuilder("1"))
			.accept("application/json+simple")
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void a25_basicCalls_patch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.patch("/bean", bean)
			.run()
			.assertBody().is("{f:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.patch("/bean")
			.body(bean)
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void a26_basicCalls_patch_fromString() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.patch("/bean", "{f:1}", "application/json")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.patch("/bean", body)
				.contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json")
				.run()
				.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.build()
				.patch(url, bean)
				.accept("application/json+simple")
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a29_basicCalls_request_patch() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.request(HttpMethod.PATCH, "/bean", bean)
			.run()
			.assertBody().is("{f:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.request(HttpMethod.PATCH, "/bean")
			.body(bean)
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.request(HttpMethod.PATCH, "/bean", body)
				.contentType(body instanceof NameValuePairs ? "application/x-www-form-urlencoded" : "application/json")
				.run()
				.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.build()
				.request(HttpMethod.PATCH, url, bean)
				.accept("application/json+simple")
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a32_basicCalls_request_get() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.request(HttpMethod.GET, "/bean", null)
			.run()
			.assertBody().is("{f:1}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.request(HttpMethod.GET, "/bean")
			.run()
			.assertBody().is("{f:1}");
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
			MockRestClient
				.create(A.class)
				.build()
				.request(HttpMethod.GET, url)
				.accept("application/json+simple")
				.run()
				.assertBody().is("{f:1}");
		}
	}

	@Test
	public void a34_basicCalls_request_whenClosed() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.build();

		rc.closeQuietly();

		try {
			rc.request(HttpMethod.GET, "/bean", null);
			fail();
		} catch (RestCallException e) {
			assertTrue(e.getMessage().startsWith("RestClient.close() has already been called"));
		}
	}

	@Test
	public void a35_basicCalls_request_whenClosed_withStackCreation() throws Exception {
		RestClient rc = MockRestClient
			.create(A.class)
			.debug()
			.build();

		rc.closeQuietly();

		try {
			rc.request(HttpMethod.GET, "/bean", null);
			fail();
		} catch (RestCallException e) {
			assertTrue(e.getMessage().startsWith("RestClient.close() has already been called"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Logging
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_logToConsole() throws Exception {
		MockConsole c = MockConsole.create();
		MockLogger l = MockLogger.create();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logRequests(DetailLevel.NONE, Level.SEVERE)
			.logToConsole()
			.logger(l)
			.console(c)
			.build()
			.post("/bean", bean)
			.complete();

		c.assertContents().is("");

		c.reset();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logRequests(DetailLevel.SIMPLE, Level.SEVERE)
			.logToConsole()
			.logger(l)
			.console(c)
			.build()
			.post("/bean", bean)
			.complete();

		c.assertContents().is("HTTP POST http://localhost/bean, HTTP/1.1 200 \n");

		c.reset();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logRequests(DetailLevel.FULL, Level.SEVERE)
			.logToConsole()
			.logger(l)
			.console(c)
			.build()
			.post("/bean", bean)
			.complete();

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
	}

	@Test
	public void b02_logTo() throws Exception {
		MockLogger l = MockLogger.create();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logRequests(DetailLevel.NONE, Level.SEVERE)
			.logToConsole()
			.logger(l)
			.build()
			.post("/bean", bean)
			.complete();

		l.assertContents().is("");
		l.assertRecordCount().is(0);

		l.reset();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logger(l)
			.logRequests(DetailLevel.SIMPLE, Level.WARNING)
			.build()
			.post("/bean", bean)
			.complete();

		l.assertLastLevel(Level.WARNING);
		l.assertLastMessage().stderr().is("HTTP POST http://localhost/bean, HTTP/1.1 200 ");
		l.assertContents().is("WARNING: HTTP POST http://localhost/bean, HTTP/1.1 200 \n");

		l.reset();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logger(l)
			.logRequests(DetailLevel.FULL, Level.WARNING)
			.build()
			.post("/bean", bean)
			.complete();

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

	public static class B02a extends BasicRestCallInterceptor {
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
	public void b02a_loggingOther() throws Exception {
		MockLogger ml = MockLogger.create();
		MockConsole mc = MockConsole.create();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logger(ml)
			.interceptors(B02a.class)
			.build()
			.post("/bean", bean)
			.complete();

		ml.assertRecordCount().is(5);

		ml.reset();

		MockRestClient
			.create(A.class)
			.simpleJson()
			.logger(ml)
			.logToConsole()
			.console(mc)
			.interceptors(B02a.class)
			.build()
			.post("/bean", bean)
			.complete();

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

	public static class B03 extends RestClient {
		private static boolean METHOD_CALLED;
		public B03(PropertyStore ps) {
			super(ps);
		}

		@Override
		protected RestRequest createRequest(java.net.URI uri, String method, boolean hasBody) throws RestCallException {
			METHOD_CALLED = true;
			return super.createRequest(uri, method, hasBody);
		}
	}

	@Test
	public void b03_overrideCreateRequest() throws Exception {
		RestClient
			.create()
			.simpleJson()
			.build(B03.class)
			.get("foo");
		assertTrue(B03.METHOD_CALLED);
	}

	public static class B04a extends BasicRestCallInterceptor {
		@Override /* HttpRequestInterceptor */
		public void onInit(RestRequest req) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class B04d extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onConnect(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class B04e extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onClose(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}

	@Test
	public void b04_restCallInterceptor_exceptionHandling() throws Exception {
		try {
			MockRestClient
			.create(A.class)
			.simpleJson()
			.interceptors(B04a.class)
			.build()
			.post("/bean", bean)
			.complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
		try {
			MockRestClient
			.create(A.class)
			.simpleJson()
			.interceptors(B04d.class)
			.build()
			.post("/bean", bean)
			.complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
		try {
			MockRestClient
			.create(A.class)
			.simpleJson()
			.interceptors(B04e.class)
			.build()
			.post("/bean", bean)
			.complete();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
	}


	//------------------------------------------------------------------------------------------------------------------
	// Passthrough methods for HttpClientBuilder.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_miscellaneous_interceptors() throws RestCallException {
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
	public void c02_miscellaneous_httpProcessor() throws RestCallException {
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
	public void c03_miscellaneous_requestExecutor() throws RestCallException {
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
	public void c04_miscellaneous_defaultHeaders() throws RestCallException {
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
	public void c05_miscellaneous_httpClientBuilderMethods() {
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

	@SuppressWarnings("deprecation")
	@Test
	public void c06_miscellaneous_unusedHttpClientMethods() {
		RestClient rc = RestClient
			.create()
			.build();

		try {
			assertNotNull(rc.getParams());
		} catch (UnsupportedOperationException e) {}
		assertNotNull(rc.getConnectionManager());
	}

	@Test
	public void c07_miscellaneous_executeHttpUriRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient
			.create(A.class)
			.build()
			.execute(x);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c08_miscellaneous_executeHttpHostHttpRequest() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient
			.create(A.class)
			.build()
			.execute(target, x);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c09_miscellaneous_executeHttpHostHttpRequestHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		HttpHost target = new HttpHost("localhost");
		HttpContext context = new BasicHttpContext();
		x.addHeader("Accept", "text/json+simple");
		HttpResponse res = MockRestClient
			.create(A.class)
			.build()
			.execute(target, x, context);
		assertEquals("{f:1}", IOUtils.read(res.getEntity().getContent()));
	}

	@Test
	public void c10_miscellaneous_executeResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient
			.create(A.class)
			.build()
			.execute(x, new BasicResponseHandler());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c11_miscellaneous_executeHttpUriRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient
			.create(A.class)
			.build()
			.execute(x, new BasicResponseHandler(), new BasicHttpContext());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c12_miscellaneous_executeHttpHostHttpRequestResponseHandlerHttpContext() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient
			.create(A.class)
			.build()
			.execute(new HttpHost("localhost"), x, new BasicResponseHandler(), new BasicHttpContext());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c13_miscellaneous_executeHttpHostHttpRequestResponseHandler() throws Exception {
		HttpGet x = new HttpGet("http://localhost/bean");
		x.addHeader("Accept", "text/json+simple");
		String res = MockRestClient
			.create(A.class)
			.build()
			.execute(new HttpHost("localhost"), x, new BasicResponseHandler());
		assertEquals("{f:1}", res);
	}

	@Test
	public void c14_miscellaneous_requestConfig() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/bean")
			.requestConfig(RequestConfig.custom().setMaxRedirects(1).build())
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void c15_miscellaneous_RestClient_toString() throws Exception {
		String s = MockRestClient
			.create(A.class)
			.simpleJson()
			.rootUrl("foo")
			.build()
			.toString();
		assertTrue(s.contains("rootUri: 'foo'"));
	}

	@Test
	public void c15_miscellaneous_request_target() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/bean")
			.target(new HttpHost("localhost"))
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void c16_miscellaneous_request_context() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/bean")
			.context(new BasicHttpContext())
			.run()
			.assertBody().is("{f:1}");
	}

	@Test
	public void c18_miscellaneous_request_uriParts() throws Exception {
		java.net.URI uri = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get()
			.scheme("http")
			.host("localhost")
			.port(8080)
			.userInfo("foo:bar")
			.uri("/bean")
			.fragment("baz")
			.query("foo", "bar")
			.run()
			.assertBody().is("{f:1}")
			.getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get()
			.scheme("http")
			.host("localhost")
			.port(8080)
			.userInfo("foo","bar")
			.uri("/bean")
			.fragment("baz")
			.query("foo", "bar")
			.run()
			.assertBody().is("{f:1}")
			.getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get()
			.uri("http://localhost")
			.uri("http://foo:bar@localhost:8080/bean?foo=bar#baz")
			.run()
			.assertBody().is("{f:1}")
			.getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());

		uri = MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get()
			.uri(new java.net.URI(null, null, null, null))
			.uri(new java.net.URI("http://foo:bar@localhost:8080/bean?foo=bar#baz"))
			.run()
			.assertBody().is("{f:1}")
			.getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz", uri.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pooled connections
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_pooled() throws Exception {
		RestClient rc = RestClient
			.create()
			.simpleJson()
			.pooled()
			.build();
		Object hc = rc.httpClient;
		assertEquals("PoolingHttpClientConnectionManager", ClassInfo.of(hc).getDeclaredField("connManager").accessible().invoke(hc).getClass().getSimpleName());

		rc = RestClient
			.create()
			.simpleJson()
			.build();
		hc = rc.httpClient;
		assertEquals("BasicHttpClientConnectionManager", ClassInfo.of(hc).getDeclaredField("connManager").accessible().invoke(hc).getClass().getSimpleName());

		rc = MockRestClient
			.create(A.class)
			.pooled()
			.simpleJson()
			.build();
		hc = rc.httpClient;
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
	public void f02_supplierHeader() throws Exception {
		TestSupplier s = TestSupplier.of("foo");

		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.header("Foo", s)
			.build();

		rc.get("/checkHeader")
			.header("Foo", s)
			.run()
			.assertBody().is("['foo','foo']");

		s.set("bar");

		rc.get("/checkHeader")
			.header("Foo", s)
			.run()
			.assertBody().is("['bar','bar']");
	}

	@Test
	public void f03_beanHeader() throws Exception {
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
	public void f04_nullHeaders() throws Exception {
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
	public void f05_header_Header() throws Exception {
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
	public void f06_header_NameValuePair() throws Exception {
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
	public void f07_header_HttpHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header(BasicStringHeader.of("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.header(BasicStringHeader.of("Foo", "baz"))
			.run()
			.assertBody().is("['bar','baz']");
	}

	@Test
	public void f08_headers_Header() throws Exception {
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
	public void f09_headers_OMap() throws Exception {
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
	public void f10_headers_Map() throws Exception {
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
	public void f11_headers_NameValuePairs() throws Exception {
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
	public void f12_headers_NameValuePair() throws Exception {
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
	public void f13_headers_pairs() throws Exception {
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
	public void f14_headers_HttpHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(BasicStringHeader.of("Foo", "bar"))
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.headers(BasicStringHeader.of("Foo", "baz"))
			.run()
			.assertBody().is("['bar','baz']");
	}

	@Test
	public void f15_headers_accept() throws Exception {
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
	public void f16_headers_acceptCharset() throws Exception {
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
	public void f17_headers_acceptEncoding() throws Exception {
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
	public void f18_headers_acceptLanguage() throws Exception {
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
	public void f19_headers_authorization() throws Exception {
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
	public void f20_headers_cacheControl() throws Exception {
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
	public void f21_headers_clientVersion() throws Exception {
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
	public void f22_headers_connection() throws Exception {
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
	public void f23_headers_contentLength() throws Exception {
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
	public void f24_headers_contentType() throws Exception {
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
	public void f25_headers_contentEncoding() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.contentEncoding("identity")
			.header("Check", "Content-Encoding")
			.build()
			.get("/checkHeader")
			.run()
			.assertBody().is("['identity']");
	}

	@Test
	public void f26_headers_date() throws Exception {
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
	public void f27_headers_expect() throws Exception {
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
	public void f28_headers_forwarded() throws Exception {
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
	public void f29_headers_from() throws Exception {
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
	public void f30_headers_host() throws Exception {
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
	public void f31_headers_ifMatch() throws Exception {
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
	public void f32_headers_ifModifiedSince() throws Exception {
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
	public void f33_headers_ifNoneMatch() throws Exception {
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
	public void f34_headers_ifRange() throws Exception {
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
	public void f35_headers_ifUnmodifiedSince() throws Exception {
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
	public void f36_headers_maxForwards() throws Exception {
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
	public void f37_headers_noTrace() throws Exception {
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
	public void f38_headers_origin() throws Exception {
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
	public void f39_headers_pragma() throws Exception {
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
	public void f40_headers_proxyAuthorization() throws Exception {
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
	public void f41_headers_range() throws Exception {
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
	public void f42_headers_referer() throws Exception {
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
	public void f43_headers_te() throws Exception {
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
	public void f44_headers_userAgent() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.userAgent(new StringBuilder("foo"))
			.header("Check", "User-Agent")
			.build()
			.get("/checkHeader")
			.run()
			.assertBody().is("['foo']");
	}

	@Test
	public void f45_headers_upgrade() throws Exception {
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
	public void f46_headers_via() throws Exception {
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
	public void f47_headers_warning() throws Exception {
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

	@Test
	public void f48_headers_onRequest_debug() throws Exception {
		MockRestClient
		.create(A.class)
		.simpleJson()
		.header("Check", "Debug")
		.build()
		.get("/checkHeader")
		.debug()
		.run()
		.assertBody().is("['true']");
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
			.header(new org.apache.juneau.http.header.Date("Sun, 31 Dec 2000 12:34:56 GMT"))
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
			.header(new org.apache.juneau.http.header.Date(CALENDAR))
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
			.header("Foo", bean, null, new XPartSerializer())
			.build()
			.get("/checkHeader")
			.run()
			.assertBody().is("['x{f:1}']");
	}

	@Test
	public void h08_headers_withSchema() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo", AList.of("bar","baz"), HttpPartSchema.T_ARRAY_CSV)
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.run()
			.assertBody().is("['bar,baz']");
	}

	@Test
	public void h08a_headers_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});

		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Check", "Foo")
			.header("Foo", s, HttpPartSchema.T_ARRAY_PIPES)
			.build();

		rc.get("/checkHeader")
			.header("Foo", s, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().is("['foo|bar','foo|bar']");

		s.set(new String[]{"bar","baz"});

		rc.get("/checkHeader")
			.header("Foo", s, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().is("['bar|baz','bar|baz']");
	}

	@Test
	public void h09_headers_nullHeader() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.headers(BasicStringHeader.of("Foo", "bar"), null)
			.header("Check", "Foo")
			.build()
			.get("/checkHeader")
			.run()
			.assertBody().is("['bar']");
	}

	@Test
	public void h10_headers_invalidHeader() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.headers("Foo");
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertEquals("Invalid type passed to headers(Object...):  java.lang.String", e.getLocalizedMessage());
		}
	}

	@Test
	public void h10_headers_invalidHeaderPairs() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.headerPairs("Foo");
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertEquals("Odd number of parameters passed into headerPairs(Object...)", e.getLocalizedMessage());
		}
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
			.queries(BasicNameValuePair.of("Foo","f1"))
			.queries(OMap.of("Foo","f2"))
			.queries(AMap.of("Foo","f3"))
			.queries(NameValuePairs.of("Foo","f4","Foo","f5"))
			.queries(BasicNameValuePair.of("Foo","f6"), BasicNameValuePair.of("Foo","f7"))
			.build()
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
	}

	@Test
	public void i03_query_withSchema() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.query("Foo",AList.of("bar","baz"), HttpPartSchema.T_ARRAY_PIPES)
			.build()
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz")
		;
	}

	@Test
	public void i04_query_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));

		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.query("Foo", s, HttpPartSchema.T_ARRAY_PIPES)
			.build();

		rc
			.get("/checkQuery")
			.query("Bar", s, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().is("Foo=foo%7Cbar&Bar=foo%7Cbar")
			.assertBody().urlDecodedIs("Foo=foo|bar&Bar=foo|bar")
		;

		s.set(new String[]{"bar","baz"});

		rc
			.get("/checkQuery")
			.query("Bar", s, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().is("Foo=bar%7Cbaz&Bar=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz&Bar=bar|baz")
		;
	}

	@Test
	public void i05_query_withSchemaAndSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));

		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.query("Foo", s, HttpPartSchema.T_ARRAY_PIPES, new XPartSerializer())
			.build();

		rc
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D")
			.assertBody().urlDecodedIs("Foo=x['foo','bar']")
		;

		s.set(AList.of("bar","baz"));

		rc
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D")
			.assertBody().urlDecodedIs("Foo=x['bar','baz']")
		;
	}

	@Test
	public void i06_query_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));

		RestClient rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.query("Foo", s)
			.build();

		rc
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=foo%2Cbar")
			.assertBody().urlDecodedIs("Foo=foo,bar")
		;

		s.set(AList.of("bar","baz"));

		rc
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=bar%2Cbaz")
			.assertBody().urlDecodedIs("Foo=bar,baz")
		;
	}

	@Test
	public void i07_query_withNull() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.queries(BasicNameValuePair.of("Foo","bar"), null)
			.build()
			.get("/checkQuery")
			.run()
			.assertBody().is("Foo=bar");
	}

	@Test
	public void i08_query_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.queries(BasicNameValuePair.of("Foo","bar"), "Baz");
			fail();
		} catch (Exception e) {
			assertEquals("Invalid type passed to query(Object...):  java.lang.String", e.getMessage());
		}
	}

	@Test
	public void i09_queryPairs() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.queryPairs("foo","bar","baz","qux")
			.build()
			.get("/checkQuery")
			.run()
			.assertBody().is("foo=bar&baz=qux")
		;

		MockRestClient
			.create(A.class)
			.simpleJson()
			.queryPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2"))
			.build()
			.get("/checkQuery")
			.run()
			.assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2")
			.assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2")
		;
	}

	@Test
	public void i10_queryPairs_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.queryPairs("foo","bar","baz");
			fail();
		} catch (Exception e) {
			assertEquals("Odd number of parameters passed into queryPairs(Object...)", e.getMessage());
		}
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
			.formDatas(BasicNameValuePair.of("Foo","f1"))
			.formDatas(OMap.of("Foo","f2"))
			.formDatas(AMap.of("Foo","f3"))
			.formDatas(NameValuePairs.of("Foo","f4","Foo","f5"))
			.formDatas(BasicNameValuePair.of("Foo","f6"), BasicNameValuePair.of("Foo","f7"))
			.build()
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=f1&Foo=f2&Foo=f3&Foo=f4&Foo=f5&Foo=f6&Foo=f7");
	}

	@Test
	public void j03_formData_withSchema() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formData("Foo",AList.of("bar","baz"), HttpPartSchema.T_ARRAY_PIPES)
			.build()
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz")
		;
	}

	@Test
	public void j03a_formData_withSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));

		RestClient rc= MockRestClient
			.create(A.class)
			.simpleJson()
			.formData("Foo", s)
			.build();

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=foo%2Cbar")
			.assertBody().urlDecodedIs("Foo=foo,bar")
		;

		s.set(OList.of("bar","baz"));

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=bar%2Cbaz")
			.assertBody().urlDecodedIs("Foo=bar,baz")
		;
	}

	@Test
	public void j03a_formData_withSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(OList.of("foo","bar"));

		RestClient rc= MockRestClient
			.create(A.class)
			.simpleJson()
			.formData("Foo", s, HttpPartSchema.T_ARRAY_PIPES, new XPartSerializer())
			.build();

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=x%5B%27foo%27%2C%27bar%27%5D")
			.assertBody().urlDecodedIs("Foo=x['foo','bar']")
		;

		s.set(OList.of("bar","baz"));

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=x%5B%27bar%27%2C%27baz%27%5D")
			.assertBody().urlDecodedIs("Foo=x['bar','baz']")
		;
	}

	@Test
	public void j03a_formData_withSupplierAndSchema() throws Exception {
		TestSupplier s = TestSupplier.of(AList.of("foo","bar"));

		RestClient rc= MockRestClient
			.create(A.class)
			.simpleJson()
			.formData("Foo", s, HttpPartSchema.T_ARRAY_PIPES)
			.build();

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=foo%7Cbar")
			.assertBody().urlDecodedIs("Foo=foo|bar")
		;

		s.set(AList.of("bar","baz"));

		rc
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz")
		;
	}

	@Test
	public void j04_formData_withNull() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formDatas(BasicNameValuePair.of("Foo","bar"), null)
			.build()
			.post("/checkFormData")
			.run()
			.assertBody().is("Foo=bar");
	}

	@Test
	public void j05_formData_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.formDatas(BasicNameValuePair.of("Foo","bar"), "Baz");
			fail();
		} catch (Exception e) {
			assertEquals("Invalid type passed to formData(Object...):  java.lang.String", e.getMessage());
		}
	}

	@Test
	public void j06_formDataPairs() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formDataPairs("foo","bar","baz","qux")
			.build()
			.post("/checkFormData")
			.run()
			.assertBody().is("foo=bar&baz=qux")
		;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.formDataPairs("foo",AList.of("bar1","bar2"),"baz",AList.of("qux1","qux2"))
			.build()
			.post("/checkFormData")
			.run()
			.assertBody().is("foo=bar1%2Cbar2&baz=qux1%2Cqux2")
			.assertBody().urlDecodedIs("foo=bar1,bar2&baz=qux1,qux2")
		;
	}

	@Test
	public void j05_formDataPairs_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.formDataPairs("foo","bar","baz");
			fail();
		} catch (Exception e) {
			assertEquals("Odd number of parameters passed into formDataPairs(Object...)", e.getMessage());
		}
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
	public void k02_restClient_CallHandlerObject() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.callHandler(new RestCallHandler() {
				@Override
				public HttpResponse execute(HttpHost target, HttpEntityEnclosingRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
					return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, null));
				}
				@Override
				public HttpResponse execute(HttpHost target, HttpRequestBase request, HttpContext context) throws ClientProtocolException, IOException {
					return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, null));
				}
			})
			.header("Foo", "f1")
			.build()
			.get("/checkHeader")
			.header("Foo","f2")
			.run()
			.assertStatus().is(201);
	}

	@Test
	public void k03_restClient_errorCodes() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.errorCodes(x -> x == 200)
				.ignoreErrors(false)
				.build()
				.get("/echo")
				.run();
			fail("Exception expected.");
		} catch (RestCallException e) {
			assertEquals(200, e.getResponseCode());
		}
	}

	@Test
	public void k03a_restClient_errorCodes_perRequest() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.ignoreErrors(false)
				.build()
				.get("/echo")
				.errorCodes(x -> x == 200)
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
		assertEquals(es, rc.getExecutorService());
		rc
			.get("/echo")
			.runFuture()
			.get()
			.assertStatus().is(200)
			.assertBody().contains("HTTP GET /echo");

		es = null;
		rc = MockRestClient
			.create(A.class)
			.simpleJson()
			.executorService(es, true)
			.build();
		assertNotNull(rc.getExecutorService());
		rc
			.get("/echo")
			.runFuture()
			.get()
			.assertStatus().is(200)
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


	@Test
	public void k08a_restClient_interceptorsObjects_perRequest() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.header("Foo","f1")
			.build()
			.get("/checkHeader")
			.interceptors(new XRestCallInterceptor())
			.header("Check","foo")
			.header("Foo","f3")
			.run()
			.assertBody().is("['f1','f2','f3']")
			.assertHeader("Bar").is("b1");
		assertEquals(111, XRestCallInterceptor.x);
	}

	public static class K08a extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
			throw new RuntimeException("foo");
		}

		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
		}

		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
		}
	}

	public static class K08b extends BasicRestCallInterceptor {
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

	public static class K08c extends BasicRestCallInterceptor {
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
	public void k08_restClient_interceptorsClasses_exceptions() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(K08a.class)
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(K08b.class)
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(K08c.class)
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run()
				.close();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
	}

	@Test
	public void k08_restClient_interceptorsObjects_exceptions() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(new K08a())
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(new K08b())
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.interceptors(new K08c())
				.build()
				.get("/checkHeader")
				.header("Check","foo")
				.header("Foo","f3")
				.run()
				.close();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
	}

	@Test
	public void k08_restClient_interceptorsObjects_perRequest_exceptions() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.build()
				.get("/checkHeader")
				.interceptors(new K08a())
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.build()
				.get("/checkHeader")
				.interceptors(new K08b())
				.header("Check","foo")
				.header("Foo","f3")
				.run();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.header("Foo","f1")
				.build()
				.get("/checkHeader")
				.interceptors(new K08c())
				.header("Check","foo")
				.header("Foo","f3")
				.run()
				.close();
			fail();
		} catch (RestCallException e) {
			assertEquals("foo", e.getCause(RuntimeException.class).getMessage());
		}
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
	public void k14a_restClient_invalidSerializersAndParsers() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.prependTo(RESTCLIENT_serializers, String.class)
				.build();
			fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_serializers property had invalid class of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.prependTo(RESTCLIENT_serializers, "")
				.build();
			fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_serializers property had invalid object of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.prependTo(RESTCLIENT_parsers, String.class)
				.build();
			fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_parsers property had invalid class of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}

		try {
			MockRestClient
				.create(A.class)
				.prependTo(RESTCLIENT_parsers, "")
				.build();
			fail();
		} catch (ContextRuntimeException e) {
			assertEquals("RESTCLIENT_parsers property had invalid object of type 'java.lang.String'", e.getCause(ConfigException.class).getMessage());
		}
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
			.build();
		Bean b = rc
			.get("/")
			.header("Foo",bean)
			.run()
			.assertHeader("Foo").is("x{f:1}")
			.getHeader("Foo").as(Bean.class);
		assertEquals("{f:1}", b.toString());
		b = rc
			.get()
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
			.uriContext(UriContext.of("http://localhost:80", "/context", "/resource", "/path"))
			.build()
			.post("/echoBody", x)
			.run()
			.assertBody().is("{f:'http://localhost:80/context/resource/foo'}");

		MockRestClient
			.create(A.class)
			.simpleJson()
			.uriResolution(UriResolution.NONE)
			.uriRelativity(UriRelativity.RESOURCE)
			.uriContext(UriContext.of("http://localhost:80", "/context", "/resource", "/path"))
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

		MockRestClient
			.create(A.class)
			.simpleJson()
			.useWhitespace()
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
			.assertBody().is("Foo=%27bar+baz%27")
			.assertBody().urlDecodedIs("Foo='bar baz'")
		;
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
			.assertBody().is("Foo=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz")
		;

		rc.post("/checkFormData")
			.formData("Foo", new String[]{"bar","baz"})
			.run()
			.assertBody().is("Foo=bar%7Cbaz")
			.assertBody().urlDecodedIs("Foo=bar|baz")
		;

		rc.get("/checkHeader")
			.header("Check", "Foo")
			.header("Foo", new String[]{"bar","baz"})
			.accept("text/json+simple")
			.run()
			.assertBody().is("['bar|baz']")
		;
	}

	@Test
	public void n03_urlEnc_paramFormat() throws Exception {
		 OMap map = OMap.of(
			"foo", "bar",
			"baz", new String[]{"qux", "true", "123"}
		);

		 MockRestClient
			.create(A.class)
			.urlEnc()
			.paramFormat(ParamFormat.PLAINTEXT)
			.build()
			.post("/echoBody", map)
			.run()
			.assertBody().is("foo=bar&baz=qux,true,123");

		 MockRestClient
			.create(A.class)
			.urlEnc()
			.paramFormatPlain()
			.build()
			.post("/echoBody", map)
			.run()
			.assertBody().is("foo=bar&baz=qux,true,123");

		 MockRestClient
			.create(A.class)
			.urlEnc()
			.paramFormat(ParamFormat.UON)
			.build()
			.post("/echoBody", map)
			.run()
			.assertBody().is("foo=bar&baz=@(qux,'true','123')");
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
			.assertBody().is("'O1'")
		;
		rc2.post("/echoBody", new O1())
			.run()
			.assertBody().is("{f:1}")
		;

		rc1.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.assertBody().is("foo=O1")
		;
		rc2.get("/checkQuery")
			.query("foo", new O1())
			.run()
			.assertBody().is("foo=f%3D1")
			.assertBody().urlDecodedIs("foo=f=1")
		;

		rc1.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.assertBody().is("foo=O1")
		;
		rc2.formPost("/checkFormData")
			.formData("foo", new O1())
			.run()
			.assertBody().is("foo=f%3D1")
			.assertBody().urlDecodedIs("foo=f=1")
		;

		rc1.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.assertBody().is("['O1']")
		;
		rc2.get("/checkHeader")
			.header("foo", new O1())
			.header("Check", "foo")
			.run()
			.assertBody().is("['f=1']")
		;
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

	public static class O05a {}

	@Test
	public void o05a_beanContext_beansDontRequireSomeProperties() throws Exception {
		MockRestClient
			.create(A.class)
			.beansDontRequireSomeProperties()
			.simpleJson()
			.build()
			.post("/echoBody", new O05a())
			.run()
			.assertBody().is("{}")
		;
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

		x = MockRestClient
			.create(A.class)
			.simpleJson()
			.typeName(O35.class, "foo")
			.typePropertyName(O35.class, "X")
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

	public static class O38 {
		private int foo;
		public int bar;

		public int getFoo() {
			return foo;
		}

		public void setFoo(int foo) {
			this.foo = foo;
		}

		public O38 init() {
			this.foo = 1;
			this.bar = 2;
			return this;
		}
	}

	@Test
	public void o38_beanContext_useJavaIntrospector() throws Exception {
		O38 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.useJavaBeanIntrospector()
			.build()
			.post("/echoBody", new O38().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(O38.class)
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
		MockRestClient
			.create(A.class)
			.simpleJson()
			.addTo(BeanContext.BEAN_notBeanClasses,P2.class)
			.build()
			.post("/echoBody", P2.fromString("bar"))
			.run()
			.cacheBody()
			.assertBody().is("'bar'")
			.getBody().as(P2.class);
	}

	public static class P3 {
		public int foo;
		public P3 init() {
			foo = 1;
			return this;
		}
	}

	public static class P3s extends PojoSwap<P3,Integer> {
		@Override
		public Integer swap(BeanSession session, P3 o) { return o.foo; }
		@Override
		public P3 unswap(BeanSession session, Integer f, ClassMeta<?> hint) {return new P3().init(); }
	}

	@Test
	public void p03_context_appendToStringObject() throws Exception {
		P3 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.appendTo(BeanContext.BEAN_swaps,P3s.class)
			.build()
			.post("/echoBody", new P3().init())
			.run()
			.cacheBody()
			.assertBody().is("1")
			.getBody().as(P3.class);
		assertEquals(1, x.foo);
	}

	@Test
	public void p04_context_prependToStringObject() throws Exception {
		P3 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.prependTo(BeanContext.BEAN_swaps,P3s.class)
			.build()
			.post("/echoBody", new P3().init())
			.run()
			.cacheBody()
			.assertBody().is("1")
			.getBody().as(P3.class);
		assertEquals(1, x.foo);
	}

	public static class P6 {
		public int foo;
		public P6 init() {
			this.foo = 1;
			return this;
		}
	}

	@Test
	public void p06_context_apply() throws Exception {
		MockRestClient
			.create(A.class)
			.json()
			.apply(SimpleJsonSerializer.DEFAULT.getPropertyStore())
			.build()
			.post("/echoBody", new P6().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(P6.class);
	}

	public static class P7 {
		public int foo,bar,baz;
		public P7 init() {
			foo = 1;
			bar = 2;
			baz = 3;
			return this;
		}
	}

	@org.apache.juneau.annotation.Bean(sort=true, on="P7")
	public static class P7a {}

	@BeanConfig(sortProperties="true")
	public static class P7b {}

	@Test
	public void p07_context_applyAnnotationsClasses() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.applyAnnotations(P7a.class)
			.build()
			.post("/echoBody", new P7().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(P7.class);
		MockRestClient
			.create(A.class)
			.simpleJson()
			.applyAnnotations(P7b.class)
			.build()
			.post("/echoBody", new P7().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(P7.class);
	}

	public static class P8a {
		@BeanConfig(sortProperties="true")
		public void foo() {}
	}

	@Test
	public void p08_context_applyAnnotationsMethods() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.applyAnnotations(P8a.class.getMethod("foo"))
			.build()
			.post("/echoBody", new P7().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(P7.class);
	}

	@Test
	public void p09_context_applyAnnotationsAnnotationList() throws Exception {
		AnnotationList al = ClassInfo.of(P7b.class).getAnnotationList(ConfigAnnotationFilter.INSTANCE);
		VarResolverSession vr = VarResolver.DEFAULT.createSession();
		MockRestClient
			.create(A.class)
			.simpleJson()
			.applyAnnotations(al,vr)
			.build()
			.post("/echoBody", new P7().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(P7.class);
	}

	@Test
	public void p10_context_removeFrom() throws Exception {
		P3 x = MockRestClient
			.create(A.class)
			.simpleJson()
			.appendTo(BeanContext.BEAN_swaps,P3s.class)
			.removeFrom(BeanContext.BEAN_swaps,P3s.class)
			.build()
			.post("/echoBody", new P3().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(P3.class);
		assertEquals(1, x.foo);
	}

	@Test
	public void p12_context_setStringObject() throws Exception {
		MockRestClient
			.create(A.class)
			.json()
			.set(JsonSerializer.JSON_simpleMode,true)
			.build()
			.post("/echoBody", new P3().init())
			.run()
			.cacheBody()
			.assertBody().is("{foo:1}")
			.getBody().as(P3.class);
	}

	@Test
	public void p13_context_annotations() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.annotations(new BeanAnnotation(P7.class).sort(true))
			.build()
			.post("/echoBody", new P7().init())
			.run()
			.cacheBody()
			.assertBody().is("{bar:2,baz:3,foo:1}")
			.getBody().as(P7.class);
	}

	public static interface P14i {
		void setFoo(int foo);
		int getFoo();
	}

	public static class P14 implements P14i {
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
	public void p14_context_putAllTo() throws Exception {
		P14i x = MockRestClient
			.create(A.class)
			.simpleJson()
			.putAllTo(BeanContext.BEAN_implClasses, AMap.of(P14i.class.getName(), P14.class))
			.build()
			.post("/echoBody", new StringReader("{foo:1}"))
			.run()
			.getBody().as(P14i.class)
		;
		assertEquals(1, x.getFoo());
		assertTrue(x instanceof P14);
	}


	public static class P15 {
		public int foo = 1;
	}

	@Test
	public void p15_context_set() throws Exception {
		MockRestClient
			.create(null)
			.set(
				AMap.of(
					JsonSerializer.JSON_simpleMode, true,
					WriterSerializer.WSERIALIZER_quoteChar, "'",
					MockRestClient.MOCKRESTCLIENT_restBean, A.class
				)
			)
			.json()
			.build()
			.post("/echoBody", new P15())
			.run()
			.assertBody().is("{foo:1}")
		;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Path
	//-----------------------------------------------------------------------------------------------------------------

	public static class Q01 {
		public int foo;
		public Q01 init() {
			foo = 1;
			return this;
		}

		@Override
		public String toString() {
			return "xxx";
		}
	}

	@Test
	public void q01_request_path() throws Exception {
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/{x}")
			.path("x", new Q01().init())
			.run()
			.assertBody().contains("HTTP GET /echo/foo=1")
		;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/{x}")
			.path("x", ()->new Q01().init())
			.run()
			.assertBody().contains("HTTP GET /echo/foo=1")
		;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/*")
			.path("/*", new Q01().init())
			.run()
			.assertBody().contains("HTTP GET /echo/foo=1")
		;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/*")
			.path("/*", ()->new Q01().init())
			.run()
			.assertBody().contains("HTTP GET /echo/foo=1")
			;
	}

	@Test
	public void q03_request_path_withSchema() throws Exception {
		String[] a = new String[]{"foo","bar"};

		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/{x}")
			.path("x", a, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().contains("HTTP GET /echo/foo%7Cbar")
		;
		MockRestClient
			.create(A.class)
			.simpleJson()
			.build()
			.get("/echo/{x}")
			.path("x", ()->a, HttpPartSchema.T_ARRAY_PIPES)
			.run()
			.assertBody().contains("HTTP GET /echo/foo%7Cbar")
		;
	}

	@Test
	public void q04_request_path_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.get("/echo/{x}")
				.path("y", "foo")
				.run()
				.assertBody().contains("HTTP GET /echo/foo%7Cbar")
			;
			fail();
		} catch (RestCallException e) {
			assertEquals("Path variable {y} was not found in path.", e.getMessage());
		}
	}


	@Test
	public void q05_request_path_invalid() throws Exception {
		try {
			MockRestClient
				.create(A.class)
				.simpleJson()
				.build()
				.get("/echo/{x}")
				.path("y", "foo")
				.run()
				.assertBody().contains("HTTP GET /echo/foo%7Cbar")
			;
			fail();
		} catch (RestCallException e) {
			assertEquals("Path variable {y} was not found in path.", e.getMessage());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------
}
