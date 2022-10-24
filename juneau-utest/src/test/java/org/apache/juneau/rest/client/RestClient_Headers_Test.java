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
package org.apache.juneau.rest.client;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.httppart.HttpPartSchema.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.runners.MethodSorters.*;
import static java.time.format.DateTimeFormatter.*;
import static java.time.temporal.ChronoUnit.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.uon.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Headers_Test {

	public static final CaptureLogger LOGGER = new CaptureLogger();

	public static class CaptureLogger extends BasicTestCaptureCallLogger {
		public static CaptureLogger getInstance() {
			return LOGGER;
		}
	}

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	private static ABean bean = ABean.get();

	@Rest(callLogger=CaptureLogger.class)
	public static class A extends BasicRestObject {
		@RestGet
		public String[] headers(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeaderParam("Check").orElse(null)).stream().map(x -> x.getValue()).toArray(String[]::new);
		}
	}

	private static final ZonedDateTime ZONEDDATETIME = ZonedDateTime.from(RFC_1123_DATE_TIME.parse("Mon, 3 Dec 2007 10:15:30 GMT")).truncatedTo(SECONDS);
	private static final String PARSEDZONEDDATETIME = "Mon, 3 Dec 2007 10:15:30 GMT";

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_header_String_Object() throws Exception {
		checkFooClient().header("Foo","bar").build().get("/headers").run().assertContent("['bar']");
		checkFooClient().build().get("/headers").header("Foo","baz").run().assertContent("['baz']");
		checkFooClient().header("Foo","bar").build().get("/headers").header("Foo","baz").run().assertContent("['bar','baz']");
		checkFooClient().headers(header("Foo",bean,null)).build().get("/headers").header("Foo",bean).run().assertContent("['f=1','f=1']");
		checkFooClient().headers(header("Foo",null,null)).build().get("/headers").header("Foo",null).run().assertContent("[]");
	}

	@Test
	public void a02_header_String_Object_Schema() throws Exception {
		List<String> l1 = list("bar","baz"), l2 = list("qux","quux");
		checkFooClient().headers(header("Foo",l1,T_ARRAY_PIPES)).build().get("/headers").header(header("Foo",l2,T_ARRAY_PIPES)).run().assertContent("['bar|baz','qux|quux']");
	}

	@Test
	public void a03_header_Header() throws Exception {
		checkFooClient().headers(header("Foo","bar")).build().get("/headers").header(header("Foo","baz")).run().assertContent("['bar','baz']");
		checkFooClient().headers(stringHeader("Foo","bar")).build().get("/headers").header(stringHeader("Foo","baz")).run().assertContent("['bar','baz']");
	}

	@Test
	public void a08_header_String_Supplier() throws Exception {
		TestSupplier s = TestSupplier.of("foo");
		RestClient x = checkFooClient().headers(header("Foo",s,null)).build();
		x.get("/headers").header("Foo",s).run().assertContent("['foo','foo']");
		s.set("bar");
		x.get("/headers").header("Foo",s).run().assertContent("['bar','bar']");
	}

	@Test
	public void a09_headers_String_Object_Schema_Serializer() throws Exception {
		checkFooClient().headers(header("Foo",bean,null).serializer(MockWriterSerializer.X)).build().get("/headers").run().assertContent("['x{f:1}x']");
	}

	@Test
	public void a10_headers_String_Supplier_Schema() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		RestClient x = checkFooClient().headers(header("Foo",s,T_ARRAY_PIPES)).build();
		x.get("/headers").header(header("Foo",s,T_ARRAY_PIPES)).run().assertContent("['foo|bar','foo|bar']");
		s.set(new String[]{"bar","baz"});
		x.get("/headers").header(header("Foo",s,T_ARRAY_PIPES)).run().assertContent("['bar|baz','bar|baz']");
	}

	@Test
	public void a11_headers_String_Supplier_Schema_Serializer() throws Exception {
		TestSupplier s = TestSupplier.of(new String[]{"foo","bar"});
		checkFooClient().headers(header("Foo",s,T_ARRAY_PIPES).serializer(UonSerializer.DEFAULT)).build().get("/headers").run().assertContent("['@(foo,bar)']");
	}

	public static class A12 implements HttpPartSerializer {
		@Override
		public HttpPartSerializerSession getPartSession() {
			return new HttpPartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) throws SerializeException, SchemaValidationException {
					throw new SerializeException("bad");
				}
			};
		}
	}

	@Test
	public void a12_badSerialization() throws Exception {
		assertThrown(()->checkFooClient().headers(header("Foo","bar",null).serializer(new A12())).build().get().run()).asMessages().isContains("bad");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other tests
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_standardHeaders() throws Exception {
		checkClient("Accept").accept("text/plain").build().get("/headers").run().assertContent("['text/plain']");
		checkClient("Accept-Charset").acceptCharset("UTF-8").build().get("/headers").run().assertContent("['UTF-8']");
		checkClient("Client-Version").clientVersion("1").build().get("/headers").run().assertContent("['1']");
		checkClient("Content-Type").contentType("foo").build().get("/headers").run().assertContent("['foo']");
		checkClient("No-Trace").noTrace().build().get("/headers").run().assertContent("['true','true']");

		checkClient("Accept").build().get("/headers").accept("text/plain").run().assertContent("['text/plain']");
		checkClient("Accept-Charset").build().get("/headers").acceptCharset("UTF-8").run().assertContent("['UTF-8']");
		checkClient("Content-Type").build().get("/headers").contentType("foo").run().assertContent("['foo']");
		checkClient("No-Trace").build().get("/headers").noTrace().run().assertContent("['true','true']");
	}

	@Test
	public void b02_headerBeans() throws Exception {
		checkClient("Accept").headers(new Accept("text/plain")).build().get("/headers").run().assertContent("['text/plain']");
		checkClient("Accept-Charset").headers(new AcceptCharset("UTF-8")).build().get("/headers").run().assertContent("['UTF-8']");
		checkClient("Accept-Encoding").headers(new AcceptEncoding("identity")).build().get("/headers").run().assertContent("['identity']");
		checkClient("Accept-Language").headers(new AcceptLanguage("en")).build().get("/headers").run().assertContent("['en']");
		checkClient("Authorization").headers(new Authorization("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Cache-Control").headers(new CacheControl("none")).header("X-Expect","none").build().get("/headers").run().assertContent("['none']");
		checkClient("Client-Version").headers(new ClientVersion("1")).build().get("/headers").run().assertContent("['1']");
		checkClient("Connection").headers(new Connection("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Content-Length").headers(new ContentLength(123l)).build().get("/headers").run().assertContent("['123']");
		checkClient("Content-Type").headers(new ContentType("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Date").headers(new org.apache.juneau.http.header.Date(PARSEDZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("Date").headers(new org.apache.juneau.http.header.Date(ZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("Expect").headers(new Expect("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Forwarded").headers(new Forwarded("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("From").headers(new From("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Host").headers(new Host("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("If-Match").headers(new IfMatch("\"foo\"")).build().get("/headers").run().assertContent("['\"foo\"']");
		checkClient("If-Modified-Since").headers(new IfModifiedSince(ZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("If-Modified-Since").headers(new IfModifiedSince(PARSEDZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("If-None-Match").headers(new IfNoneMatch("\"foo\"")).build().get("/headers").run().assertContent("['\"foo\"']");
		checkClient("If-Range").headers(new IfRange("\"foo\"")).build().get("/headers").run().assertContent("['\"foo\"']");
		checkClient("If-Unmodified-Since").headers(new IfUnmodifiedSince(ZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("If-Unmodified-Since").headers(new IfUnmodifiedSince(PARSEDZONEDDATETIME)).build().get("/headers").run().assertContent("['"+PARSEDZONEDDATETIME+"']");
		checkClient("Max-Forwards").headers(new MaxForwards(10)).build().get("/headers").run().assertContent("['10']");
		checkClient("No-Trace").headers(new NoTrace("true")).build().get("/headers").run().assertContent("['true','true']");
		checkClient("Origin").headers(new Origin("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Pragma").headers(new Pragma("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Proxy-Authorization").headers(new ProxyAuthorization("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Range").headers(new Range("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Referer").headers(new Referer("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("TE").headers(new TE("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("User-Agent").headers(new UserAgent("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Upgrade").headers(new Upgrade("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Via").headers(new Via("foo")).build().get("/headers").run().assertContent("['foo']");
		checkClient("Warning").headers(new Warning("foo")).build().get("/headers").run().assertContent("['foo']");
	}

	@Test
	public void b03_debugHeader() throws Exception {
		checkClient("Debug").build().get("/headers").debug().suppressLogging().run().assertContent("['true']");
	}

	@Test
	public void b04_dontOverrideAccept() throws Exception {
		checkClient("Accept").header("Accept","text/plain").build().get("/headers").run().assertContent("['text/plain']");
		checkClient("Accept").header("Accept","text/foo").build().get("/headers").header("Accept","text/plain").run().assertContent("['text/foo','text/plain']");
		RestClient rc = checkClient("Accept").header("Accept","text/foo").build();
		RestRequest req = rc.get("/headers");
		req.setHeader("Accept","text/plain");
		req.run().assertContent("['text/plain']");
	}

	@Test
	public void b05_dontOverrideContentType() throws Exception {
		checkClient("Content-Type").header("Content-Type","text/plain").build().get("/headers").run().assertContent("['text/plain']");
		checkClient("Content-Type").header("Content-Type","text/foo").build().get("/headers").header("Content-Type","text/plain").run().assertContent("['text/foo','text/plain']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static org.apache.http.Header header(String name, String val) {
		return basicHeader(name, val);
	}

	private static SerializedHeader header(String name, Object val, HttpPartSchema schema) {
		return serializedHeader(name, val).schema(schema);
	}

	private static RestClient.Builder checkFooClient() {
		return MockRestClient.create(A.class).json5().header("Check","Foo");
	}

	private static RestClient.Builder checkClient(String headerToCheck) {
		return MockRestClient.create(A.class).json5().header("Check",headerToCheck).noTrace();
	}
}
