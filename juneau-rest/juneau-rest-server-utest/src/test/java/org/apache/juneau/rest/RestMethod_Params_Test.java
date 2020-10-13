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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestMethod_Params_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Various parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest(messages="RestParamsTest")
	public static class A {
		@RestMethod
		public String a(ResourceBundle t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod
		public String b(Messages t) {
			return t == null ? null : t.getString("foo");
		}
		@RestMethod(name=POST)
		public String c(InputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST)
		public String d(ServletInputStream t) throws IOException {
			return read(t);
		}
		@RestMethod(name=POST)
		public String e(Reader t) throws IOException {
			return read(t);
		}
		@RestMethod
		public void f(OutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod
		public void g(ServletOutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestMethod
		public void h(Writer t) throws IOException {
			t.write("OK");
		}
		@RestMethod
		public boolean i(RequestHeaders t) {
			return t != null;
		}
		@RestMethod
		public boolean j(RequestQuery t) {
			return t != null;
		}
		@RestMethod
		public boolean k(RequestFormData t) {
			return t != null;
		}
		@RestMethod
		public String l(@Method String t) {
			return t;
		}
		@SuppressWarnings("deprecation")
		@RestMethod
		public boolean m(RestLogger t) {
			return t != null;
		}
		@RestMethod
		public boolean n(RestContext t) {
			return t != null;
		}
		@RestMethod(parsers={JsonParser.class})
		public String o(Parser t) {
			return t.getClass().getName();
		}
		@RestMethod
		public String p(Locale t) {
			return t.toString();
		}
		@RestMethod
		public boolean q(Swagger t) {
			return t != null;
		}
		@RestMethod
		public boolean r(RequestPath t) {
			return t != null;
		}
		@RestMethod
		public boolean s(RequestBody t) {
			return t != null;
		}
		@RestMethod
		public boolean t(Config t) {
			return t != null;
		}
	}

	@Test
	public void a01_params() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a").acceptLanguage("en-US").run().assertBody().is("bar");
		a.get("/a").acceptLanguage("ja-JP").run().assertBody().is("baz");
		a.get("/b").acceptLanguage("en-US").run().assertBody().is("bar");
		a.get("/b").acceptLanguage("ja-JP").run().assertBody().is("baz");
		a.post("/c", "foo").run().assertBody().is("foo");
		a.post("/d", "foo").run().assertBody().is("foo");
		a.post("/e", "foo").run().assertBody().is("foo");
		a.get("/f").run().assertBody().is("OK");
		a.get("/g").run().assertBody().is("OK");
		a.get("/h").run().assertBody().is("OK");
		a.get("/i").run().assertBody().is("true");
		a.get("/j").run().assertBody().is("true");
		a.get("/k").run().assertBody().is("true");
		a.get("/l").run().assertBody().is("GET");
		a.get("/m").run().assertBody().is("true");
		a.get("/n").run().assertBody().is("true");
		a.get("/o").contentType("application/json").run().assertBody().is("org.apache.juneau.json.JsonParser");
		a.get("/p").acceptLanguage("en-US").run().assertBody().is("en_US");
		a.get("/p").acceptLanguage("ja-JP").run().assertBody().is("ja_JP");
		a.get("/q").run().assertBody().is("true");
		a.get("/r").run().assertBody().is("true");
		a.get("/s").run().assertBody().is("true");
		a.get("/t").run().assertBody().is("true");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		serializers=B1a.class,
		parsers=B1b.class,
		encoders=B1c.class,
		allowedHeaderParams="*"
	)
	public static class B1 {

		@RestMethod
		public String accept(Accept accept) {
			return accept.getValue();
		}
		@RestMethod
		public String acceptCharset(AcceptCharset acceptCharset) {
			return acceptCharset.getValue();
		}
		@RestMethod
		public String acceptEncoding(AcceptEncoding acceptEncoding) {
			return acceptEncoding.getValue();
		}
		@RestMethod
		public String acceptLanguage(AcceptLanguage acceptLanguage) {
			return acceptLanguage.getValue();
		}
		@RestMethod
		public String authorization(Authorization authorization) {
			return authorization.getValue();
		}
		@RestMethod
		public String cacheControl(CacheControl cacheControl) {
			return cacheControl.getValue();
		}
		@RestMethod
		public String connection(Connection connection) {
			return connection.getValue();
		}
		@RestMethod
		public String contentLength(ContentLength contentLength) {
			return contentLength.getValue();
		}
		@RestMethod
		public String contentType(ContentType contentType) {
			return contentType.getValue();
		}
		@RestMethod
		public String date(org.apache.juneau.http.header.Date date) {
			return date.getValue();
		}
		@RestMethod
		public String expect(Expect expect) {
			return expect.getValue();
		}
		@RestMethod
		public String from(From from) {
			return from.getValue();
		}
		@RestMethod
		public String host(Host host) {
			return host.getValue();
		}
		@RestMethod
		public String ifMatch(IfMatch ifMatch) {
			return ifMatch.getValue();
		}
		@RestMethod
		public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
			return ifModifiedSince.getValue();
		}
		@RestMethod
		public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
			return ifNoneMatch.getValue();
		}
		@RestMethod
		public String ifRange(IfRange ifRange) {
			return ifRange.getValue();
		}
		@RestMethod
		public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
			return ifUnmodifiedSince.getValue();
		}
		@RestMethod
		public String maxForwards(MaxForwards maxForwards) {
			return maxForwards.getValue();
		}
		@RestMethod
		public String pragma(Pragma pragma) {
			return pragma.getValue();
		}
		@RestMethod
		public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
			return proxyAuthorization.getValue();
		}
		@RestMethod
		public String range(Range range) {
			return range.getValue();
		}
		@RestMethod
		public String referer(Referer referer) {
			return referer.getValue();
		}
		@RestMethod
		public String te(TE te) {
			return te.getValue();
		}
		@RestMethod
		public String upgrade(Upgrade upgrade) {
			return upgrade.getValue();
		}
		@RestMethod
		public String userAgent(UserAgent userAgent) {
			return userAgent.getValue();
		}
		@RestMethod
		public String warning(Warning warning) {
			return warning.getValue();
		}
	}

	public static class B1a extends PlainTextSerializer {
		public B1a(PropertyStore ps) {
			super(ps, "text/plain", "*/*");
		}
	}

	public static class B1b extends PlainTextParser {
		public B1b(PropertyStore ps) {
			super(ps, "*/*");
		}
	}

	public static class B1c extends IdentityEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"*"};
		}
	}

	@Test
	public void b01_headers() throws Exception {
		RestClient b = MockRestClient.build(B1.class);

		b.get("/accept").accept("text/foo").run().assertBody().is("text/foo");
		b.get("/accept").accept("text/foo+bar").run().assertBody().is("text/foo+bar");
		b.get("/accept").accept("text/*").run().assertBody().is("text/*");
		b.get("/accept").accept("*/foo").run().assertBody().is("*/foo");
		b.get("/accept").accept("text/foo;q=1.0").run().assertBody().is("text/foo");
		b.get("/accept").accept("text/foo;q=0.9").run().assertBody().is("text/foo;q=0.9");
		b.get("/accept").accept("text/foo;x=X;q=0.9;y=Y").run().assertBody().is("text/foo;x=X;q=0.9;y=Y");
		b.get("/accept?Accept=text/foo").run().assertBody().is("text/foo");
		b.get("/acceptCharset").acceptCharset("UTF-8").run().assertBody().is("UTF-8");
		b.get("/acceptCharset?Accept-Charset=UTF-8").run().assertBody().is("UTF-8");
		b.get("/acceptEncoding").acceptEncoding("foo").run().assertBody().is("foo");
		b.get("/acceptEncoding").acceptEncoding("*").run().assertBody().is("*");
		b.get("/acceptEncoding?Accept-Encoding=*").run().assertBody().is("*");
		b.get("/acceptLanguage").acceptLanguage("foo").run().assertBody().is("foo");
		b.get("/acceptLanguage?Accept-Language=foo").acceptLanguage("foo").run().assertBody().is("foo");
		b.get("/authorization").authorization("foo").run().assertBody().is("foo");
		b.get("/authorization?Authorization=foo").run().assertBody().is("foo");
		b.get("/cacheControl").cacheControl("foo").run().assertBody().is("foo");
		b.get("/cacheControl?Cache-Control=foo").run().assertBody().is("foo");
		b.get("/connection").connection("foo").run().assertBody().is("foo");
		b.get("/connection?Connection=foo").run().assertBody().is("foo");
		b.get("/contentLength").contentLength(0).run().assertBody().is("0");
		b.get("/contentLength?Content-Length=0").run().assertBody().is("0");
		b.get("/contentType").contentType("text/foo").run().assertBody().is("text/foo");
		b.get("/contentType?Content-Type=text/foo").run().assertBody().is("text/foo");
		b.get("/date").date("Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/date?Date=Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/expect").expect("100-continue").run().assertBody().is("100-continue");
		b.get("/expect?Expect=100-continue").run().assertBody().is("100-continue");
		b.get("/from").from("foo").run().assertBody().is("foo");
		b.get("/from?From=foo").run().assertBody().is("foo");
		b.get("/host").host("localhost").run().assertBody().is("localhost");
		b.get("/host?Host=localhost").run().assertBody().is("localhost");
		b.get("/ifMatch").ifMatch("\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifMatch").ifMatch("W/\"foo\"").run().assertBody().is("W/\"foo\"");
		b.get("/ifMatch").ifMatch("W/\"foo\",\"bar\"").run().assertBody().is("W/\"foo\",\"bar\"");
		b.get("/ifMatch?If-Match=\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifModifiedSince").ifModifiedSince("Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/ifModifiedSince?If-Modified-Since=Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/ifNoneMatch").ifNoneMatch("\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifNoneMatch").ifNoneMatch("W/\"foo\"").run().assertBody().is("W/\"foo\"");
		b.get("/ifNoneMatch").ifNoneMatch("W/\"foo\",\"bar\"").run().assertBody().is("W/\"foo\",\"bar\"");
		b.get("/ifNoneMatch?If-None-Match=\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifRange").ifRange("\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifRange?If-Range=\"foo\"").run().assertBody().is("\"foo\"");
		b.get("/ifUnmodifiedSince").ifUnmodifiedSince("Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/ifUnmodifiedSince?If-Unmodified-Since=Wed, 21 Oct 2015 07:28:00 GMT").run().assertBody().is("Wed, 21 Oct 2015 07:28:00 GMT");
		b.get("/maxForwards").maxForwards(123).run().assertBody().is("123");
		b.get("/maxForwards?Max-Forwards=123").run().assertBody().is("123");
		b.get("/pragma").pragma("foo").run().assertBody().is("foo");
		b.get("/pragma?Pragma=foo").run().assertBody().is("foo");
		b.get("/proxyAuthorization").proxyAuthorization("foo").run().assertBody().is("foo");
		b.get("/proxyAuthorization?Proxy-Authorization=foo").run().assertBody().is("foo");
		b.get("/range").range("foo").run().assertBody().is("foo");
		b.get("/range?Range=foo").run().assertBody().is("foo");
		b.get("/referer").referer("foo").run().assertBody().is("foo");
		b.get("/referer?Referer=foo").run().assertBody().is("foo");
		b.get("/te").te("foo").run().assertBody().is("foo");
		b.get("/te?TE=foo").run().assertBody().is("foo");
		b.get("/upgrade").upgrade("foo").run().assertBody().is("foo");
		b.get("/upgrade?Upgrade=foo").run().assertBody().is("foo");
		b.get("/userAgent").userAgent("foo").run().assertBody().is("foo");
		b.get("/userAgent?User-Agent=foo").run().assertBody().is("foo");
		b.get("/warning").warning("foo").run().assertBody().is("foo");
		b.get("/warning?Warning=foo").run().assertBody().is("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom header.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		paramResolvers=B2a.class,
		allowedHeaderParams="Custom"
	)
	public static class B2 {
		@RestMethod
		public String a(B2b customHeader) {
			return customHeader.toString();
		}
	}

	public static class B2a extends RestMethodParam {
		public B2a() {
			super(RestParamType.HEADER, "Custom", B2b.class);
		}
		@Override
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return new B2b(req.getHeader("Custom"));
		}
	}

	public static class B2b {
		public String value;
		public B2b(String value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return value;
		}
	}

	@Test
	public void b02_customHeader() throws Exception {
		RestClient b = MockRestClient.build(B2.class);
		b.get("/a").header("Custom", "foo").run().assertBody().is("foo");
		b.get("/a?Custom=foo").run().assertBody().is("foo");
	}
}
