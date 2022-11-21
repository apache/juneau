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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.apache.juneau.config.*;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.arg.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestOp_Params_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Various parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest(messages="RestParamsTest")
	public static class A {
		@RestGet
		public String a(ResourceBundle t) {
			return t == null ? null : t.getString("foo");
		}
		@RestGet
		public String b(Messages t) {
			return t == null ? null : t.getString("foo");
		}
		@RestPost
		public String c(InputStream t) throws IOException {
			return read(t);
		}
		@RestPost
		public String d(ServletInputStream t) throws IOException {
			return read(t);
		}
		@RestPost
		public String e(Reader t) throws IOException {
			return read(t);
		}
		@RestGet
		public void f(OutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestGet
		public void g(ServletOutputStream t) throws IOException {
			t.write("OK".getBytes());
		}
		@RestGet
		public void h(Writer t) throws IOException {
			t.write("OK");
		}
		@RestGet
		public boolean i(RequestHeaders t) {
			return t != null;
		}
		@RestGet
		public boolean j(RequestQueryParams t) {
			return t != null;
		}
		@RestGet
		public boolean k(RequestFormParams t) {
			return t != null;
		}
		@RestGet
		public String l(@Method String t) {
			return t;
		}
		@RestGet
		public boolean n(RestContext t) {
			return t != null;
		}
		@RestOp(method=GET,parsers={JsonParser.class})
		public String o(Parser t) {
			return t.getClass().getName();
		}
		@RestGet
		public String p(Locale t) {
			return t.toString();
		}
		@RestGet
		public boolean q(org.apache.juneau.dto.swagger.Swagger t) {
			return t != null;
		}
		@RestGet
		public boolean r(RequestPathParams t) {
			return t != null;
		}
		@RestGet
		public boolean s(RequestContent t) {
			return t != null;
		}
		@RestGet
		public boolean t(Config t) {
			return t != null;
		}
	}

	@Test
	public void a01_params() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.post("/c", "foo").run().assertContent("foo");
		a.post("/d", "foo").run().assertContent("foo");
		a.post("/e", "foo").run().assertContent("foo");
		a.get("/f").run().assertContent("OK");
		a.get("/g").run().assertContent("OK");
		a.get("/h").run().assertContent("OK");
		a.get("/i").run().assertContent("true");
		a.get("/j").run().assertContent("true");
		a.get("/k").run().assertContent("true");
		a.get("/l").run().assertContent("GET");
		a.get("/n").run().assertContent("true");
		a.get("/o").contentType("application/json").run().assertContent("org.apache.juneau.json.JsonParser");
		a.get("/q").run().assertContent("true");
		a.get("/r").run().assertContent("true");
		a.get("/s").run().assertContent("true");
		a.get("/t").run().assertContent("true");
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

		@RestGet
		public String accept(Accept accept) {
			return accept.getValue();
		}
		@RestGet
		public String acceptCharset(AcceptCharset acceptCharset) {
			return acceptCharset.getValue();
		}
		@RestGet
		public String acceptEncoding(AcceptEncoding acceptEncoding) {
			return acceptEncoding.getValue();
		}
		@RestGet
		public String acceptLanguage(AcceptLanguage acceptLanguage) {
			return acceptLanguage.getValue();
		}
		@RestGet
		public String authorization(Authorization authorization) {
			return authorization.getValue();
		}
		@RestGet
		public String cacheControl(CacheControl cacheControl) {
			return cacheControl.getValue();
		}
		@RestGet
		public String connection(Connection connection) {
			return connection.getValue();
		}
		@RestGet
		public String contentLength(ContentLength contentLength) {
			return contentLength.getValue();
		}
		@RestGet
		public String contentType(ContentType contentType) {
			return contentType.getValue();
		}
		@RestGet
		public String date(org.apache.juneau.http.header.Date date) {
			return date.getValue();
		}
		@RestGet
		public String expect(Expect expect) {
			return expect.getValue();
		}
		@RestGet
		public String from(From from) {
			return from.getValue();
		}
		@RestGet
		public String host(Host host) {
			return host.getValue();
		}
		@RestGet
		public String ifMatch(IfMatch ifMatch) {
			return ifMatch.getValue();
		}
		@RestGet
		public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
			return ifModifiedSince.getValue();
		}
		@RestGet
		public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
			return ifNoneMatch.getValue();
		}
		@RestGet
		public String ifRange(IfRange ifRange) {
			return ifRange.getValue();
		}
		@RestGet
		public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
			return ifUnmodifiedSince.getValue();
		}
		@RestGet
		public String maxForwards(MaxForwards maxForwards) {
			return maxForwards.getValue();
		}
		@RestGet
		public String pragma(Pragma pragma) {
			return pragma.getValue();
		}
		@RestGet
		public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
			return proxyAuthorization.getValue();
		}
		@RestGet
		public String range(Range range) {
			return range.getValue();
		}
		@RestGet
		public String referer(Referer referer) {
			return referer.getValue();
		}
		@RestGet
		public String te(TE te) {
			return te.getValue();
		}
		@RestGet
		public String upgrade(Upgrade upgrade) {
			return upgrade.getValue();
		}
		@RestGet
		public String userAgent(UserAgent userAgent) {
			return userAgent.getValue();
		}
		@RestGet
		public String warning(Warning warning) {
			return warning.getValue();
		}
	}

	public static class B1a extends PlainTextSerializer {
		public B1a(PlainTextSerializer.Builder b) {
			super(b.accept("*/*"));
		}
	}

	public static class B1b extends PlainTextParser {
		public B1b(PlainTextParser.Builder b) {
			super(b.consumes("*/*"));
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

		b.get("/accept").accept("text/foo").run().assertContent("text/foo");
		b.get("/accept").accept("text/foo+bar").run().assertContent("text/foo+bar");
		b.get("/accept").accept("text/*").run().assertContent("text/*");
		b.get("/accept").accept("*/foo").run().assertContent("*/foo");
		b.get("/accept").accept("text/foo;q=1.0").run().assertContent("text/foo;q=1.0");
		b.get("/accept").accept("text/foo;q=0.9").run().assertContent("text/foo;q=0.9");
		b.get("/accept").accept("text/foo;x=X;q=0.9;y=Y").run().assertContent("text/foo;x=X;q=0.9;y=Y");
		b.get("/accept?Accept=text/foo").run().assertContent("text/foo");
		b.get("/acceptCharset").acceptCharset("UTF-8").run().assertContent("UTF-8");
		b.get("/acceptCharset?Accept-Charset=UTF-8").run().assertContent("UTF-8");
		b.get("/acceptEncoding?Accept-Encoding=*").run().assertContent("*");
		b.get("/authorization?Authorization=foo").run().assertContent("foo");
		b.get("/cacheControl?Cache-Control=foo").run().assertContent("foo");
		b.get("/connection?Connection=foo").run().assertContent("foo");
		b.get("/contentLength?Content-Length=0").run().assertContent("0");
		b.get("/contentType").contentType("text/foo").run().assertContent("text/foo");
		b.get("/contentType?Content-Type=text/foo").run().assertContent("text/foo");
		b.get("/date?Date=Mon, 3 Dec 2007 10:15:30 GMT").run().assertContent("Mon, 3 Dec 2007 10:15:30 GMT");
		b.get("/expect?Expect=100-continue").run().assertContent("100-continue");
		b.get("/from?From=foo").run().assertContent("foo");
		b.get("/host").uriHost("localhost").run().assertContent("localhost");
		b.get("/host?Host=localhost").run().assertContent("localhost");
		b.get("/ifMatch?If-Match=\"foo\"").run().assertContent("\"foo\"");
		b.get("/ifModifiedSince?If-Modified-Since=Mon, 3 Dec 2007 10:15:30 GMT").run().assertContent("Mon, 3 Dec 2007 10:15:30 GMT");
		b.get("/ifNoneMatch?If-None-Match=\"foo\"").run().assertContent("\"foo\"");
		b.get("/ifRange?If-Range=\"foo\"").run().assertContent("\"foo\"");
		b.get("/ifUnmodifiedSince?If-Unmodified-Since=Mon, 3 Dec 2007 10:15:30 GMT").run().assertContent("Mon, 3 Dec 2007 10:15:30 GMT");
		b.get("/maxForwards?Max-Forwards=123").run().assertContent("123");
		b.get("/pragma?Pragma=foo").run().assertContent("foo");
		b.get("/proxyAuthorization?Proxy-Authorization=foo").run().assertContent("foo");
		b.get("/range?Range=foo").run().assertContent("foo");
		b.get("/referer?Referer=foo").run().assertContent("foo");
		b.get("/te?TE=foo").run().assertContent("foo");
		b.get("/upgrade?Upgrade=foo").run().assertContent("foo");
		b.get("/userAgent?User-Agent=foo").run().assertContent("foo");
		b.get("/warning?Warning=foo").run().assertContent("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Custom header.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(
		restOpArgs=B2a.class,
		allowedHeaderParams="Custom"
	)
	public static class B2 {
		@RestGet
		public String a(B2b customHeader) {
			return customHeader.toString();
		}
	}

	public static class B2a implements RestOpArg {

		public static B2a create(ParamInfo pi) {
			if (pi.isType(B2b.class))
				return new B2a();
			return null;
		}

		@Override
		public Object resolve(RestOpSession opSession) throws Exception {
			return new B2b(opSession.getRequest().getHeaderParam("Custom").orElse(null));
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
		b.get("/a").header("Custom", "foo").run().assertContent("foo");
		b.get("/a?Custom=foo").run().assertContent("foo");
	}
}
