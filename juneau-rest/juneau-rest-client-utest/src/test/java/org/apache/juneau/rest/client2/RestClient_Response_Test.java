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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.time.temporal.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.Header;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.http.params.*;
import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.header.BasicHeader;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestRequest;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.utils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Response_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	private static final Calendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR.set(2000,11,31,12,34,56);
	}

	@Rest
	public static class A extends BasicRest {
		@RestMethod(path="/bean")
		public ABean getBean() {
			return bean;
		}
	}

	public static class A1 extends MockRestClient {
		public A1(PropertyStore ps) {
			super(ps);
		}
		@Override
		protected MockRestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
			return new MockRestResponse(this, request, null, parser);
		}
	}

	@Test
	public void a01_getStatusLine() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusLine().getStatusCode());
		assertThrown(()->client().build(A1.class).get("/bean").run()).contains("caused response code '0, null'");
		assertEquals(0,client().ignoreErrors().build(A1.class).get("/bean").run().getStatusLine().getStatusCode());
	}

	@Test
	public void a02_getStatusLine_Mutable() throws RestCallException {
		Mutable<StatusLine> m = Mutable.create();
		client().build().get("/bean").run().getStatusLine(m);
		assertEquals(200,m.get().getStatusCode());
	}

	@Test
	public void a03_getStatusCode() throws RestCallException {
		assertEquals(200,client().build().get("/bean").run().getStatusCode());
	}

	@Test
	public void a04_getStatusCode_Mutable() throws RestCallException {
		Mutable<Integer> m = Mutable.create();
		client().build().get("/bean").run().getStatusCode(m);
		assertEquals(200,m.get().intValue());
	}

	@Test
	public void a05_getReasonPhrase() throws RestCallException {
		assertNull(client().build().get("/bean").run().getReasonPhrase());
	}

	@Test
	public void a06_getReasonPhrase_Mutable() throws RestCallException {
		Mutable<String> m = Mutable.create();
		client().build().get("/bean").run().getReasonPhrase(m);
		assertNull(m.get());
	}

	@Test
	public void a07_setStatusLine() throws RestCallException {
		StatusLine sl = new BasicStatusLine(new ProtocolVersion("http",9,8),299,"foo");
		RestResponse r = client().build().get("/bean").run();
		r.setStatusLine(sl);
		r
			.assertCode().is(299)
			.assertStatus().code().is(299)
			.assertStatus().protocol().is("http")
			.assertStatus().major().is(9)
			.assertStatus().minor().is(8)
			.assertStatus().reason().is("foo");

		r.setStatusCode(298);
		r.assertCode().is(298);
		r.setReasonPhrase("bar");
		r.setStatusLine(new ProtocolVersion("http",9,8),297);
		r.assertCode().is(297);
		r.setStatusLine(new ProtocolVersion("http",9,8),296,"foo");
		r.assertCode().is(296);

		assertEquals(9, r.getProtocolVersion().getMajor());
	}

	@Test
	public void a08_setLocale() throws RestCallException {
		RestResponse r = client().build().get("/bean").run();
		r.setLocale(Locale.JAPAN);
		assertEquals(Locale.JAPAN, r.getLocale());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response header methods.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C extends BasicRest {
		@RestMethod(path="/")
		public String getHeader(org.apache.juneau.rest.RestRequest req, org.apache.juneau.rest.RestResponse res) {
			String n = req.getHeader("Check");
			String v = req.getHeaders().getString(n);
			res.setHeader(n,v);
			return v;
		}
	}

	@Test
	public void c01_response_getStringHeader() throws Exception {
		RestResponse x = checkFooClient(C.class).build().get().json().header("Foo","bar").run();
		assertEquals("bar", x.getStringHeader("Foo"));
		assertEquals("bar", x.getStringHeader("Foo","baz"));
		assertEquals("baz", x.getStringHeader("Bar","baz"));
	}

	@Test
	public void c02_response_getCharacterEncoding() throws Exception {
		assertEquals("iso-8859-1", checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().getCharacterEncoding());
		assertEquals("utf-8", checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json").run().getCharacterEncoding());
	}

	@Test
	public void c03_response_headerAssertions() throws Exception {
		checkFooClient(C.class).build().get().json().header("Foo","123").run().assertIntHeader("Foo").is(123);
		checkFooClient(C.class).build().get().json().header("Foo","123").run().assertLongHeader("Foo").is(123l);
		checkFooClient(C.class).build().get().json().header(BasicDateHeader.of("Foo",CALENDAR)).run().assertDateHeader("Foo").equals(CALENDAR.getTime(), ChronoUnit.SECONDS);
		checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().assertCharset().is("iso-8859-1");
		checkClient(C.class,"Content-Type").build().get().json().header("Content-Type","application/json;charset=iso-8859-1").run().assertContentType().is("application/json;charset=iso-8859-1");
	}

	@Test
	public void c04_response_containsHeader() throws Exception {
		RestResponse r = checkFooClient(C.class).build().get().json().header("Foo","bar").run();
		assertTrue(r.containsHeader("Foo"));
		assertFalse(r.containsHeader("Bar"));
	}

	@Test
	public void c05_response_getHeaders() throws Exception {
		RestResponse r = checkFooClient(C.class).build().get().json().run();
		r.setHeader("Foo","bar");
		r.addHeader("Foo","baz");
		r.addHeader(BasicStringHeader.of("Foo","qux"));
		assertEquals(3, r.getHeaders("Foo").length);
		assertEquals(0, r.getHeaders("Bar").length);
		r.getFirstHeader("Foo").assertString().is("bar");
		assertNull(r.getFirstHeader("Bar"));
		r.getLastHeader("Foo").assertString().is("qux");
		assertNull(r.getLastHeader("Bar"));

		r.setHeaders(new Header[]{BasicHeader.of("Foo", "quux")});
		r.getFirstHeader("Foo").assertString().is("quux");
		r.getLastHeader("Foo").assertString().is("quux");

		r.removeHeader(BasicHeader.of("Foo","bar"));
		r.getFirstHeader("Foo").assertString().is("quux");
		r.getLastHeader("Foo").assertString().is("quux");

		HeaderIterator i = r.headerIterator();
		assertEquals("quux", i.nextHeader().getValue());

		i = r.headerIterator("Foo");
		assertEquals("quux", i.nextHeader().getValue());

		r.removeHeader(BasicHeader.of("Foo","quux"));
		assertNull(r.getFirstHeader("Foo"));

		r.setHeader(BasicHeader.of("Foo","quuux"));
		r.getHeader("Foo").assertString().is("quuux");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Response body methods.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D extends BasicRest {
		@RestMethod
		public ABean postBean(@Body ABean bean) {
			return bean;
		}
	}

	@Test
	public void d01_response_assertBody() throws Exception {
		client(D.class).build().post("/bean",bean).run().assertBody(ABean.class).json().is("{f:1}");
	}

	@Test
	public void d02_response_setEntity() throws Exception {
		RestResponse x = client(D.class).build().post("/bean",bean).run();
		x.setEntity(new StringEntity("{f:2}"));
		x.assertBody(ABean.class).json().is("{f:2}");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Test
	public void e01_response_getParams_setParams() throws Exception {
		RestResponse x = client(D.class).build().post("/bean",bean).run();
		HttpParams p = new BasicHttpParams();
		x.setParams(p);
		assertTrue(x.getParams() == p);
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

	private static RestClientBuilder checkFooClient(Class<?> c) {
		return MockRestClient.create(c).simpleJson().header("Check","Foo");
	}

	private static RestClientBuilder checkClient(Class<?> c, String headerToCheck) {
		return MockRestClient.create(c).simpleJson().header("Check",headerToCheck);
	}
}
