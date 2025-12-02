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
package org.apache.juneau.rest.client;

import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

class RestClient_Response_Test extends TestBase {

	public static class ABean {
		public int f;
		static ABean get() {
			var x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	private static final ZonedDateTime ZONEDDATETIME = ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Sat, 29 Oct 1994 19:43:31 GMT")).truncatedTo(SECONDS);

	@Rest
	public static class A extends BasicRestObject {
		@RestGet
		public ABean bean() {
			return bean;
		}
	}

	public static class A1 extends MockRestClient {
		public A1(MockRestClient.Builder b) {
			super(b);
		}
		@Override
		protected MockRestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
			return new MockRestResponse(this, request, null, parser);
		}
	}

	@Test void a01_getStatusLine() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusLine().getStatusCode());
		assertThrowsWithMessage(Exception.class, "caused response code '0, null'", ()->client().build(A1.class).get("/bean").run());
		assertEquals(0,client().ignoreErrors().build(A1.class).get("/bean").run().getStatusLine().getStatusCode());
	}

	@Test void a03_getStatusCode() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusCode());
	}

	@Test void a05_getReasonPhrase() throws RestCallException {
		assertNull(client().build().get("/bean").run().getReasonPhrase());
	}

	@Test void a07_setStatusLine() throws RestCallException {
		var sl = new BasicStatusLine(new ProtocolVersion("http",9,8),299,"foo");
		var r = client().build().get("/bean").run();
		r.setStatusLine(sl);
		r
			.assertStatus(299)
			.assertStatus().asCode().is(299)
			.assertStatus().asProtocol().is("http")
			.assertStatus().asMajor().is(9)
			.assertStatus().asMinor().is(8)
			.assertStatus().asReason().is("foo");

		r.setStatusCode(298);
		r.assertStatus(298);
		r.setReasonPhrase("bar");
		r.setStatusLine(new ProtocolVersion("http",9,8),297);
		r.assertStatus(297);
		r.setStatusLine(new ProtocolVersion("http",9,8),296,"foo");
		r.assertStatus(296);

		assertEquals(9, r.getProtocolVersion().getMajor());
	}

	@Test void a08_setLocale() throws RestCallException {
		var r = client().build().get("/bean").run();
		r.setLocale(Locale.JAPAN);
		assertEquals(Locale.JAPAN, r.getLocale());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response header methods.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends BasicRestObject {
		@RestGet(path="/")
		public String getHeader(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) {
			var n = req.getHeaderParam("Check").orElse(null);
			var v = req.getHeaderParam(n).orElse(null);
			res.setHeader(n,v);
			return v;
		}
	}

	@Test void c01_response_getStringHeader() throws Exception {
		var x = checkFooClient(C.class).build().get().json().header("Foo","bar").run();
		assertEquals("bar", x.getStringHeader("Foo").orElse(null));
		assertEquals("bar", x.getStringHeader("Foo").orElse("baz"));
		assertEquals("baz", x.getStringHeader("Bar").orElse("baz"));
	}

	@Test void c02_response_getCharacterEncoding() throws Exception {
		assertEquals("iso-8859-1", checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().getCharacterEncoding());
		assertEquals("utf-8", checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json").run().getCharacterEncoding());
	}

	@Test void c03_response_headerAssertions() throws Exception {
		checkFooClient(C.class).build().get().json().header("Foo","123").run().assertHeader("Foo").asInteger().is(123);
		checkFooClient(C.class).build().get().json().header("Foo","123").run().assertHeader("Foo").asLong().is(123L);
		checkFooClient(C.class).build().get().json().header(dateHeader("Foo",ZONEDDATETIME)).run().assertHeader("Foo").asZonedDateTime().is(ZONEDDATETIME);
		checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().assertCharset().is("iso-8859-1");
		checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().assertHeader("Content-Type").is("application/json;charset=iso-8859-1");
	}

	@Test void c04_response_containsHeader() throws Exception {
		var r = checkFooClient(C.class).build().get().json().header("Foo","bar").run();
		assertTrue(r.containsHeader("Foo"));
		assertFalse(r.containsHeader("Bar"));
	}

	@Test void c05_response_getHeaders() throws Exception {
		var r = checkFooClient(C.class).build().get().json().run();
		r.setHeader("Foo","bar");
		r.addHeader("Foo","baz");
		r.addHeader(stringHeader("Foo","qux"));
		assertEquals(3, r.getHeaders("Foo").length);
		assertEquals(0, r.getHeaders("Bar").length);
		r.getFirstHeader("Foo").assertValue().is("bar");
		assertFalse(r.getFirstHeader("Bar").isPresent());
		r.getHeader("Foo").assertValue().is("bar, baz, qux");
		assertFalse(r.getHeader("Bar").isPresent());

		r.setHeaders(a(basicHeader("Foo", "quux")));
		r.getFirstHeader("Foo").assertValue().is("quux");
		r.getLastHeader("Foo").assertValue().is("quux");

		r.removeHeader(basicHeader("Foo","bar"));
		r.getFirstHeader("Foo").assertValue().is("quux");
		r.getLastHeader("Foo").assertValue().is("quux");

		var i = r.headerIterator();
		assertEquals("quux", i.nextHeader().getValue());

		i = r.headerIterator("Foo");
		assertEquals("quux", i.nextHeader().getValue());

		r.removeHeader(basicHeader("Foo","quux"));
		assertFalse(r.getFirstHeader("Foo").isPresent());

		r.setHeader(basicHeader("Foo","quuux"));
		r.getHeader("Foo").assertValue().is("quuux");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response body methods.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRestObject {
		@RestPost
		public ABean bean(@Content ABean bean) {
			return bean;
		}
	}

	@Test void d01_response_assertBody() throws Exception {
		client(D.class).build().post("/bean",bean).run().assertContent().as(ABean.class).asJson().is("{f:1}");
	}

	@Test void d02_response_setEntity() throws Exception {
		var x = client(D.class).build().post("/bean",bean).run();
		x.setEntity(new StringEntity("{f:2}"));
		x.assertContent().as(ABean.class).asJson().is("{f:2}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Test void e01_response_getParams_setParams() throws Exception {
		var x = client(D.class).build().post("/bean",bean).run();
		var p = new BasicHttpParams();
		x.setParams(p);
		assertSame(x.getParams(), p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}

	private static RestClient.Builder client(Class<?> c) {
		return MockRestClient.create(c).json5();
	}

	private static RestClient.Builder checkFooClient(Class<?> c) {
		return MockRestClient.create(c).json5().header("Check","Foo");
	}

	private static RestClient.Builder checkClient(Class<?> c, String headerToCheck) {
		return MockRestClient.create(c).json5().header("Check",headerToCheck);
	}
}