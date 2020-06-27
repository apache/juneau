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
		CALENDAR.set(2000, 11, 31, 12, 34, 56);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_header_basicHeaders() throws Exception {
		client().header("Check", "Foo").header("Foo","bar").build().get("/headers").run().assertBody().is("['bar']");
		client().header("Check", "Foo").build().get("/headers").header("Foo","baz").run().assertBody().is("['baz']");
		client().header("Check", "Foo").header("Foo","bar").build().get("/headers").header("Foo","baz").run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a02_headers_fromSupplier() throws Exception {
		TestSupplier s = TestSupplier.of("foo");
		RestClient x = client().header("Check", "Foo").header("Foo", s).build();
		x.get("/headers").header("Foo", s).run().assertBody().is("['foo','foo']");
		s.set("bar");
		x.get("/headers").header("Foo", s).run().assertBody().is("['bar','bar']");
	}

	@Test
	public void a03_headers_beanValue() throws Exception {
		client().header("Foo", bean).header("Check", "Foo").build().get("/headers").header("Foo", bean).run().assertBody().is("['f=1','f=1']");
	}

	@Test
	public void a04_headers_nullValue() throws Exception {
		client().header("Foo", null).header("Check", "Foo").build().get("/headers").header("Foo", null).run().assertBody().is("null");
		client().header(null, "bar").header("Check", "null").build().get("/headers").header(null, "Foo").run().assertBody().is("null");
		client().header(null, null).header("Check", "null").build().get("/headers").header(null, null).run().assertBody().is("null");
	}
	@Test
	public void a04_headers_nullHeader() throws Exception {
		client().headers((Header)null).header("Check", "Foo").build().get("/headers").headers((Header)null).run().assertBody().is("null");
	}

	@Test
	public void a05_headers_Header() throws Exception {
		client().header(header("Foo", "bar")).header("Check", "Foo").build().get("/headers").header(header("Foo", "baz")).run().assertBody().is("['bar','baz']");
		client().headers(header("Foo", "bar"),header("Baz", "baz")).header("Check", "Foo").build().get("/headers").headers(header("Foo", "baz"),header("Baz", "quux")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a06_headers_NameValuePair() throws Exception {
		client().header(pair("Foo", "bar")).header("Check", "Foo").build().get("/headers").header(pair("Foo", "baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a07_headers_HttpHeader() throws Exception {
		client().header(BasicStringHeader.of("Foo", "bar")).header("Check", "Foo").build().get("/headers").header(BasicStringHeader.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a08_headers_objects() throws Exception {
		client().headers(OMap.of("Foo", "bar")).header("Check", "Foo").build().get("/headers").headers(OMap.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
		client().headers(AMap.of("Foo", "bar")).header("Check", "Foo").build().get("/headers").headers(AMap.of("Foo", "baz")).run().assertBody().is("['bar','baz']");
		client().headers(pairs("Foo","bar")).header("Check", "Foo").build().get("/headers").headers(pairs("Foo","baz")).run().assertBody().is("['bar','baz']");
		client().headers((Object)new NameValuePair[]{pair("Foo","bar")}).header("Check", "Foo").build().get("/headers").headers(pairs("Foo","baz")).run().assertBody().is("['bar','baz']");
		client().headers(pair("Foo","bar")).header("Check", "Foo").build().get("/headers").headers(pair("Foo","baz")).run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a12_headers_headerPairs() throws Exception {
		client().headerPairs("Foo", "bar").header("Check", "Foo").build().get("/headers").headerPairs("Foo", "baz").run().assertBody().is("['bar','baz']");
	}

	@Test
	public void a13_headers_standardHeaders() throws Exception {
		client().accept("text/foo").header("Check", "Accept").build().get("/headers").accept("text/plain").run().assertBody().is("['text/foo','text/plain']");
		client().acceptCharset("UTF-8").header("Check", "Accept-Charset").build().get("/headers").run().assertBody().is("['UTF-8']");
		client().acceptEncoding("identity").header("Check", "Accept-Encoding").build().get("/headers").run().assertBody().is("['identity']");
		client().acceptLanguage("en").header("Check", "Accept-Language").build().get("/headers").run().assertBody().is("['en']");
		client().authorization("foo").header("Check", "Authorization").build().get("/headers").run().assertBody().is("['foo']");
		client().cacheControl("none").header("Check", "Cache-Control").build().get("/headers").run().assertBody().is("['none']");
		client().clientVersion("1").header("Check", "X-Client-Version").build().get("/headers").run().assertBody().is("['1']");
		client().connection("foo").header("Check", "Connection").build().get("/headers").run().assertBody().is("['foo']");
		client().contentLength("123").header("Check", "Content-Length").build().get("/headers").run().assertBody().is("['123']");
		client().contentType("foo").header("Check", "Content-Type").build().get("/headers").run().assertBody().is("['foo']");
		client().contentEncoding("identity").header("Check", "Content-Encoding").build().get("/headers").run().assertBody().is("['identity']");
		client().date("123").header("Check", "Date").build().get("/headers").run().assertBody().is("['123']");
		client().expect("foo").header("Check", "Expect").build().get("/headers").run().assertBody().is("['foo']");
		client().forwarded("foo").header("Check", "Forwarded").build().get("/headers").run().assertBody().is("['foo']");
		client().from("foo").header("Check", "From").build().get("/headers").run().assertBody().is("['foo']");
		client().host("foo").header("Check", "Host").build().get("/headers").run().assertBody().is("['foo']");
		client().ifMatch("foo").header("Check", "If-Match").build().get("/headers").run().assertBody().is("['foo']");
		client().ifModifiedSince("foo").header("Check", "If-Modified-Since").build().get("/headers").run().assertBody().is("['foo']");
		client().ifNoneMatch("foo").header("Check", "If-None-Match").build().get("/headers").run().assertBody().is("['foo']");
		client().ifRange("foo").header("Check", "If-Range").build().get("/headers").run().assertBody().is("['foo']");
		client().ifUnmodifiedSince("foo").header("Check", "If-Unmodified-Since").build().get("/headers").run().assertBody().is("['foo']");
		client().maxForwards("10").header("Check", "Max-Forwards").build().get("/headers").run().assertBody().is("['10']");
		client().noTrace().header("Check", "No-Trace").build().get("/headers").run().assertBody().is("['true']");
		client().origin("foo").header("Check", "Origin").build().get("/headers").run().assertBody().is("['foo']");
		client().pragma("foo").header("Check", "Pragma").build().get("/headers").run().assertBody().is("['foo']");
		client().proxyAuthorization("foo").header("Check", "Proxy-Authorization").build().get("/headers").run().assertBody().is("['foo']");
		client().range("foo").header("Check", "Range").build().get("/headers").run().assertBody().is("['foo']");
		client().referer("foo").header("Check", "Referer").build().get("/headers").run().assertBody().is("['foo']");
		client().te("foo").header("Check", "TE").build().get("/headers").run().assertBody().is("['foo']");
		client().userAgent(new StringBuilder("foo")).header("Check", "User-Agent").build().get("/headers").run().assertBody().is("['foo']");
		client().upgrade("foo").header("Check", "Upgrade").build().get("/headers").run().assertBody().is("['foo']");
		client().via("foo").header("Check", "Via").build().get("/headers").run().assertBody().is("['foo']");
		client().warning("foo").header("Check", "Warning").build().get("/headers").run().assertBody().is("['foo']");
	}

	@Test
	public void a14_headers_debug_onRequest() throws Exception {
		client().header("Check", "Debug").build().get("/headers").debug().run().assertBody().is("['true']");
	}

	@Test
	public void a15_headers_SerializedNameValuePairBuilder() throws Exception {
		client().header("Check", "Foo").headers(SerializedNameValuePair.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/headers").headers(SerializedNameValuePair.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
	}

	@Test
	public void a16_headers_SerializedHeaderBuilder() throws Exception {
		client().header("Check", "Foo").headers(SerializedHeader.create().name("Foo").value("Bar").serializer(OpenApiSerializer.DEFAULT)).build().get("/headers").headers(SerializedHeader.create().name("Foo").value("Baz").serializer(OpenApiSerializer.DEFAULT)).debug().run().assertBody().is("['Bar','Baz']");
	}

	@Test
	public void a17_headers_headerBeans() throws Exception {
		client().header(new Accept("text/foo")).header("Check", "Accept").build().get("/headers").header(new Accept("text/plain")).run().assertBody().is("['text/foo','text/plain']");
		client().header(new AcceptCharset("UTF-8")).header("Check", "Accept-Charset").build().get("/headers").run().assertBody().is("['UTF-8']");
		client().header(new AcceptEncoding("identity")).header("Check", "Accept-Encoding").build().get("/headers").run().assertBody().is("['identity']");
		client().header(new AcceptLanguage("en")).header("Check", "Accept-Language").build().get("/headers").run().assertBody().is("['en']");
		client().header(new Authorization("foo")).header("Check", "Authorization").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new CacheControl("none")).header("Check", "Cache-Control").header("X-Expect", "none").build().get("/headers").run().assertBody().is("['none']");
		client().header(new ClientVersion("1")).header("Check", "X-Client-Version").build().get("/headers").run().assertBody().is("['1']");
		client().header(new Connection("foo")).header("Check", "Connection").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new ContentLength(123)).header("Check", "Content-Length").build().get("/headers").run().assertBody().is("['123']");
		client().header(new ContentType("foo")).header("Check", "Content-Type").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new org.apache.juneau.http.header.Date("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "Date").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new org.apache.juneau.http.header.Date(CALENDAR)).header("Check", "Date").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new Expect("foo")).header("Check", "Expect").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Forwarded("foo")).header("Check", "Forwarded").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new From("foo")).header("Check", "From").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Host("foo")).header("Check", "Host").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new IfMatch("\"foo\"")).header("Check", "If-Match").build().get("/headers").run().assertBody().is("['\"foo\"']");
		client().header(new IfModifiedSince(CALENDAR)).header("Check", "If-Modified-Since").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new IfModifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "If-Modified-Since").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new IfNoneMatch("\"foo\"")).header("Check", "If-None-Match").build().get("/headers").run().assertBody().is("['\"foo\"']");
		client().header(new IfRange("foo")).header("Check", "If-Range").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new IfUnmodifiedSince(CALENDAR)).header("Check", "If-Unmodified-Since").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new IfUnmodifiedSince("Sun, 31 Dec 2000 12:34:56 GMT")).header("Check", "If-Unmodified-Since").build().get("/headers").run().assertBody().is("['Sun, 31 Dec 2000 12:34:56 GMT']");
		client().header(new MaxForwards(10)).header("Check", "Max-Forwards").build().get("/headers").run().assertBody().is("['10']");
		client().header(new NoTrace("true")).header("Check", "No-Trace").build().get("/headers").run().assertBody().is("['true']");
		client().header(new Origin("foo")).header("Check", "Origin").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Pragma("foo")).header("Check", "Pragma").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new ProxyAuthorization("foo")).header("Check", "Proxy-Authorization").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Range("foo")).header("Check", "Range").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Referer("foo")).header("Check", "Referer").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new TE("foo")).header("Check", "TE").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new UserAgent("foo")).header("Check", "User-Agent").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Upgrade("foo")).header("Check", "Upgrade").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Via("foo")).header("Check", "Via").build().get("/headers").run().assertBody().is("['foo']");
		client().header(new Warning("foo")).header("Check", "Warning").build().get("/headers").run().assertBody().is("['foo']");
	}

	@Test
	public void a18_headers_headerPairs() throws Exception {
		client().header("Check", "Foo").headerPairs("Foo","bar","Foo","baz").header("Foo","qux").build().get("/headers").headerPairs("Foo","q1x","Foo","q2x").run().assertBody().is("['bar','baz','qux','q1x','q2x']");
	}

	@Test
	public void a19_headers_dontOverrideAccept() throws Exception {
		client().header("Check", "Accept").header("Accept", "text/plain").build().get("/headers").run().assertBody().is("['text/plain']");
		client().header("Check", "Accept").header("Accept", "text/foo").build().get("/headers").header("Accept","text/plain").run().assertBody().is("['text/foo','text/plain']");
		RestClient rc = client().header("Check", "Accept").header("Accept", "text/foo").build();
		RestRequest req = rc.get("/headers");
		req.setHeader("Accept","text/plain");
		req.run().assertBody().is("['text/plain']");
	}

	@Test
	public void a20_headers_dontOverrideContentType() throws Exception {
		client().header("Check", "Content-Type").header("Content-Type", "text/plain").build().get("/headers").run().assertBody().is("['text/plain']");
		client().header("Check", "Content-Type").header("Content-Type", "text/foo").build().get("/headers").header("Content-Type", "text/plain").run().assertBody().is("['text/foo','text/plain']");
	}

	@Test
	public void a21_headers_HttpPartSerializer() throws Exception {
		client().header("Check", "Foo").header("Foo", bean, null, new K12a()).build().get("/headers").run().assertBody().is("['x{f:1}']");
	}

	@Test
	public void a22_headers_withSchema() throws Exception {
		client().header("Foo", AList.of("bar","baz"), T_ARRAY_CSV).header("Check", "Foo").build().get("/headers").run().assertBody().is("['bar,baz']");
	}

	@Test
	public void a23_headers_withSchemaAndSupplier() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		RestClient x = client().header("Check", "Foo").header("Foo", s, T_ARRAY_PIPES).build();
		x.get("/headers").header("Foo", s, T_ARRAY_PIPES).run().assertBody().is("['foo|bar','foo|bar']");
		s.set(new String[]{"bar","baz"});
		x.get("/headers").header("Foo", s, T_ARRAY_PIPES).run().assertBody().is("['bar|baz','bar|baz']");
	}

	@Test
	public void a24_headers_withSchemaAndSupplierAndSerializer() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		client().header("Check", "Foo").header("Foo", s, T_ARRAY_PIPES, UonSerializer.DEFAULT).build().get("/headers").run().assertBody().is("['@(foo,bar)']");
	}

	@Test
	public void a25_headers_invalidHeader() throws Exception {
		assertThrown(()->{client().headers("Foo");}).contains("Invalid type");
	}

	@Test
	public void a26_headers_invalidHeaderPairs() throws Exception {
		assertThrown(()->{client().headerPairs("Foo");}).contains("Odd number of parameters");
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
}
