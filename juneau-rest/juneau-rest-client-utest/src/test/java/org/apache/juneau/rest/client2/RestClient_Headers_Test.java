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
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.AddFlag.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.oapi.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.RestClient_Test.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

public class RestClient_Headers_Test {

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
		@RestMethod
		public String[] getHeaders(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
	}

	private static final Calendar CALENDAR = new GregorianCalendar(TimeZone.getTimeZone("Z"));
	static {
		CALENDAR.set(2000,11,31,12,34,56);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_header_String_Object() throws Exception {
		checkFooClient().header("Foo","bar").build().get("/headers").run().assertBody().is("['bar']");
		checkFooClient().build().get("/headers").header("Foo","baz").run().assertBody().is("['baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").header("Foo","baz").run().assertBody().is("['bar','baz']");
		checkFooClient().header("Foo",bean).build().get("/headers").header("Foo",bean).run().assertBody().is("['f=1','f=1']");
		checkFooClient().header("Foo",null).build().get("/headers").header("Foo",null).run().assertBody().is("null");

		checkClient("null").header(null,"bar").build().get("/headers").header(null,"Foo").run().assertBody().is("null");
		checkClient("null").header(null,null).build().get("/headers").header(null,null).run().assertBody().is("null");
	}

	@Test
	public void a02_header_String_Object_Schema() throws Exception {
		List<String> l1 = AList.of("bar","baz"), l2 = AList.of("qux","quux");
		checkFooClient().header("Foo",l1,T_ARRAY_PIPES).build().get("/headers").header("Foo",l2,T_ARRAY_PIPES).run().assertBody().is("['bar|baz','qux|quux']");
	}

	@Test
	public void a03_header_Header() throws Exception {
		checkFooClient().header(header("Foo","bar")).build().get("/headers").header(header("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().header(BasicStringHeader.of("Foo","bar")).build().get("/headers").header(BasicStringHeader.of("Foo","baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a04_header_NameValuePair() throws Exception {
		checkFooClient().header(pair("Foo","bar")).build().get("/headers").header(pair("Foo","baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a05_headerPairs_Objects() throws Exception {
		checkFooClient().headerPairs("Foo","bar").build().get("/headers").headerPairs("Foo","baz").run().assertBody().is("['bar','baz']");
		checkFooClient().headerPairs("Foo","bar","Foo","baz").header("Foo","qux").build().get("/headers").headerPairs("Foo","q1x","Foo","q2x").run().assertBody().is("['bar','baz','qux','q1x','q2x']");
		assertThrown(()->{client().headerPairs("Foo");}).contains("Odd number of parameters");
		assertThrown(()->{client().build().get("").headerPairs("Foo");}).contains("Odd number of parameters");
	}

	@Test
	public void a06_headers_Objects() throws Exception {
		checkFooClient().headers((Header)null).build().get("/headers").headers((Header)null).run().assertBody().is("null");
		checkFooClient().headers(header("Foo","bar"),header("Baz","baz")).build().get("/headers").headers(header("Foo","baz"),header("Baz","quux")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers(OMap.of("Foo","bar")).build().get("/headers").headers(OMap.of("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers(AMap.of("Foo","bar")).build().get("/headers").headers(AMap.of("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers(pairs("Foo","bar")).build().get("/headers").headers(pairs("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers((Object)new NameValuePair[]{pair("Foo","bar")}).build().get("/headers").headers(pairs("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers(pair("Foo","bar")).build().get("/headers").headers(pair("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().headers(SerializedNameValuePair.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/headers").headers(SerializedNameValuePair.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
		checkFooClient().headers(SerializedHeader.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/headers").headers(SerializedHeader.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
		checkFooClient().headers((Object)new Header[]{header("Foo","bar")}).build().get("/headers").headers((Object)new Header[]{header("Foo","baz")}).debug().run().assertBody().is("['bar','baz']");
		checkClient("f").build().get("/headers").headers(bean).debug().run().assertBody().is("['1']");
		checkClient("f").build().get("/headers").headers((Object)null).debug().run().assertBody().is("null");
		assertThrown(()->{client().headers("Foo");}).contains("Invalid type");
		assertThrown(()->{client().build().get("").headers("Foo");}).contains("Invalid type");
	}

	@Test
	public void a07_header_AddFlag_String_Object() throws Exception {
		checkFooClient().header("Foo","bar").build().get("/headers").header(APPEND,"Foo","baz").run().assertBody().is("['bar','baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").header(REPLACE,"Foo","baz").run().assertBody().is("['baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").header(PREPEND,"Foo","baz").run().assertBody().is("['baz','bar']");
	}

	@Test
	public void a07_header_AddFlag_String_Object_Schema() throws Exception {
		List<String> l = AList.of("baz","qux");
		checkFooClient().header("Foo","bar").build().get("/headers").header(APPEND,"Foo",l,T_ARRAY_PIPES).run().assertBody().is("['bar','baz|qux']");
		checkFooClient().header("Foo","bar").build().get("/headers").header(REPLACE,"Foo",l,T_ARRAY_PIPES).run().assertBody().is("['baz|qux']");
		checkFooClient().header("Foo","bar").build().get("/headers").header(PREPEND,"Foo",l,T_ARRAY_PIPES).run().assertBody().is("['baz|qux','bar']");
	}

	@Test
	public void a07_headers_AddFlag_Objects() throws Exception {
		checkFooClient().header("Foo","bar").build().get("/headers").headers(APPEND,header("Foo","baz")).run().assertBody().is("['bar','baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").headers(REPLACE,header("Foo","baz")).run().assertBody().is("['baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").headers(PREPEND,header("Foo","baz")).run().assertBody().is("['baz','bar']");
	}

	@Test
	public void a08_header_String_Supplier() throws Exception {
		TestSupplier s = TestSupplier.of("foo");
		RestClient x = checkFooClient().header("Foo",s).build();
		x.get("/headers").header("Foo",s).run().assertBody().is("['foo','foo']");
		s.set("bar");
		x.get("/headers").header("Foo",s).run().assertBody().is("['bar','bar']");
	}

	@Test
	public void a09_headers_String_Object_Schema_Serializer() throws Exception {
		checkFooClient().header("Foo",bean,null,new K12a()).build().get("/headers").run().assertBody().is("['x{f:1}']");
	}

	@Test
	public void a10_headers_String_Supplier_Schema() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		RestClient x = checkFooClient().header("Foo",s,T_ARRAY_PIPES).build();
		x.get("/headers").header("Foo",s,T_ARRAY_PIPES).run().assertBody().is("['foo|bar','foo|bar']");
		s.set(new String[]{"bar","baz"});
		x.get("/headers").header("Foo",s,T_ARRAY_PIPES).run().assertBody().is("['bar|baz','bar|baz']");
	}

	@Test
	public void a11_headers_String_Supplier_Schema_Serializer() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		checkFooClient().header("Foo",s,T_ARRAY_PIPES,UonSerializer.DEFAULT).build().get("/headers").run().assertBody().is("['@(foo,bar)']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_standardHeaders() throws Exception {
		checkClient("Accept").accept("text/foo").build().get("/headers").accept("text/plain").run().assertBody().is("['text/foo','text/plain']");
		checkClient("Accept-Charset").acceptCharset("UTF-8").build().get("/headers").run().assertBody().is("['UTF-8']");
		checkClient("Accept-Encoding").acceptEncoding("identity").build().get("/headers").run().assertBody().is("['identity']");
		checkClient("Accept-Language").acceptLanguage("en").build().get("/headers").run().assertBody().is("['en']");
		checkClient("Authorization").authorization("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Cache-Control").cacheControl("none").build().get("/headers").run().assertBody().is("['none']");
		checkClient("X-Client-Version").clientVersion("1").build().get("/headers").run().assertBody().is("['1']");
		checkClient("Connection").connection("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Content-Length").contentLength("123").build().get("/headers").run().assertBody().is("['123']");
		checkClient("Content-Type").contentType("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Content-Encoding").contentEncoding("identity").build().get("/headers").run().assertBody().is("['identity']");
		checkClient("Date").date("123").build().get("/headers").run().assertBody().is("['123']");
		checkClient("Expect").expect("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Forwarded").forwarded("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("From").from("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Host").host("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Match").ifMatch("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Modified-Since").ifModifiedSince("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-None-Match").ifNoneMatch("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Range").ifRange("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Unmodified-Since").ifUnmodifiedSince("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Max-Forwards").maxForwards("10").build().get("/headers").run().assertBody().is("['10']");
		checkClient("No-Trace").noTrace().build().get("/headers").run().assertBody().is("['true']");
		checkClient("Origin").origin("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Pragma").pragma("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Proxy-Authorization").proxyAuthorization("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Range").range("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Referer").referer("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("TE").te("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("User-Agent").userAgent(new StringBuilder("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Upgrade").upgrade("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Via").via("foo").build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Warning").warning("foo").build().get("/headers").run().assertBody().is("['foo']");
	}

	@Test
	public void b02_headerBeans() throws Exception {
		checkClient("Accept").header(new Accept("text/foo")).build().get("/headers").header(new Accept("text/plain")).run().assertBody().is("['text/foo','text/plain']");
		checkClient("Accept-Charset").header(new AcceptCharset("UTF-8")).build().get("/headers").run().assertBody().is("['UTF-8']");
		checkClient("Accept-Encoding").header(new AcceptEncoding("identity")).build().get("/headers").run().assertBody().is("['identity']");
		checkClient("Accept-Language").header(new AcceptLanguage("en")).build().get("/headers").run().assertBody().is("['en']");
		checkClient("Authorization").header(new Authorization("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Cache-Control").header(new CacheControl("none")).header("X-Expect","none").build().get("/headers").run().assertBody().is("['none']");
		checkClient("X-Client-Version").header(new ClientVersion("1")).build().get("/headers").run().assertBody().is("['1']");
		checkClient("Connection").header(new Connection("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Content-Length").header(new ContentLength(123)).build().get("/headers").run().assertBody().is("['123']");
		checkClient("Content-Type").header(new ContentType("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Date").header(new org.apache.juneau.http.header.Date("Sun, 31 Dec 2000 12:34:56 GMT")).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("Date").header(new org.apache.juneau.http.header.Date(CALENDAR)).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("Expect").header(new Expect("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Forwarded").header(new Forwarded("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("From").header(new From("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Host").header(new Host("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Match").header(new IfMatch("\"foo\"")).build().get("/headers").run().assertBody().is("['\"foo\"']");
		checkClient("If-Modified-Since").header(new IfModifiedSince(CALENDAR)).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("If-Modified-Since").header(new IfModifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("If-None-Match").header(new IfNoneMatch("\"foo\"")).build().get("/headers").run().assertBody().is("['\"foo\"']");
		checkClient("If-Range").header(new IfRange("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("If-Unmodified-Since").header(new IfUnmodifiedSince(CALENDAR)).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("If-Unmodified-Since").header(new IfUnmodifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		checkClient("Max-Forwards").header(new MaxForwards(10)).build().get("/headers").run().assertBody().is("['10']");
		checkClient("No-Trace").header(new NoTrace("true")).build().get("/headers").run().assertBody().is("['true']");
		checkClient("Origin").header(new Origin("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Pragma").header(new Pragma("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Proxy-Authorization").header(new ProxyAuthorization("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Range").header(new Range("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Referer").header(new Referer("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("TE").header(new TE("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("User-Agent").header(new UserAgent("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Upgrade").header(new Upgrade("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Via").header(new Via("foo")).build().get("/headers").run().assertBody().is("['foo']");
		checkClient("Warning").header(new Warning("foo")).build().get("/headers").run().assertBody().is("['foo']");
	}

	@Test
	public void b03_debugHeader() throws Exception {
		checkClient("Debug").build().get("/headers").debug().run().assertBody().is("['true']");
	}

	@Test
	public void b04_dontOverrideAccept() throws Exception {
		checkClient("Accept").header("Accept","text/plain").build().get("/headers").run().assertBody().is("['text/plain']");
		checkClient("Accept").header("Accept","text/foo").build().get("/headers").header("Accept","text/plain").run().assertBody().is("['text/foo','text/plain']");
		RestClient rc = checkClient("Accept").header("Accept","text/foo").build();
		RestRequest req = rc.get("/headers");
		req.setHeader("Accept","text/plain");
		req.run().assertBody().is("['text/plain']");
	}

	@Test
	public void b05_dontOverrideContentType() throws Exception {
		checkClient("Content-Type").header("Content-Type","text/plain").build().get("/headers").run().assertBody().is("['text/plain']");
		checkClient("Content-Type").header("Content-Type","text/foo").build().get("/headers").header("Content-Type","text/plain").run().assertBody().is("['text/foo','text/plain']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static org.apache.http.Header header(String name, String val) {
		return new BasicHeader(name, val);
	}

	private static NameValuePair pair(String name, Object val) {
		return BasicNameValuePair.of(name, val);
	}

	private static NameValuePairs pairs(Object...pairs) {
		return NameValuePairs.of(pairs);
	}

	private static RestClientBuilder client() {
		return MockRestClient.create(A.class).simpleJson();
	}

	private static RestClientBuilder checkFooClient() {
		return MockRestClient.create(A.class).simpleJson().header("Check","Foo");
	}

	private static RestClientBuilder checkClient(String headerToCheck) {
		return MockRestClient.create(A.class).simpleJson().header("Check",headerToCheck);
	}
}
