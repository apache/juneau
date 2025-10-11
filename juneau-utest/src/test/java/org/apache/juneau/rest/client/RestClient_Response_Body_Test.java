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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.TestUtils.assertBean;
import static org.apache.juneau.TestUtils.assertList;
import static org.apache.juneau.TestUtils.assertString;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class RestClient_Response_Body_Test extends TestBase {

	public static class ABean {
		public int f;
		static ABean get() {
			var x = new ABean();
			x.f = 1;
			return x;
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestPost
		public InputStream echo(InputStream is) {
			return is;
		}
		@RestGet
		public ABean bean() {
			return bean;
		}
		@RestOp
		public void head() {}  // NOSONAR
	}

	public static class TestClient extends MockRestClient {
		public HttpEntity responseEntity;
		public Header[] headers = {};
		public TestClient entity(HttpEntity entity) {
			this.responseEntity = entity;
			return this;
		}
		public TestClient headers(Header...o) {
			this.headers = o;
			return this;
		}
		public TestClient(MockRestClient.Builder builder) {
			super(builder);
		}
		@Override
		protected MockRestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
			var r = new BasicHttpResponse(new ProtocolVersion("http", 1,1),200,"");
			r.setEntity(responseEntity);
			for (var h : headers)
				r.addHeader(h);
			return new MockRestResponse(this, request, r, parser);
		}
	}

	@Test void a01_basic() throws Exception {
		client().build().post("/echo",bean).run().assertContent().as(ABean.class).asJson().is("{f:1}");
		client().build().post("/echo",bean).run().assertContent().asBytes().asString().is("{f:1}");
	}

	@Test void a02_overrideParser() throws Exception {
		var x = client().build();
		var b = x.post("/echo",bean).run().getContent().parser(JsonParser.DEFAULT).as(ABean.class);
		assertBean(b, "f", "1");
		assertThrowsWithMessage(Exception.class, "ParseError at [row,col]:[1,1]", ()->x.post("/echo",bean).run().getContent().parser(XmlParser.DEFAULT).as(ABean.class));
		assertThrowsWithMessage(Exception.class, "ParseError at [row,col]:[1,1]", ()->x.post("/echo",bean).run().getContent().parser(XmlParser.DEFAULT).assertValue().as(ABean.class));
	}

	@Test void a03_asInputStream() throws Exception {
		var r1 = client().build().get("/bean").run();
		var is = r1.getContent().asInputStream();
		assertEquals("{f:1}", StringUtils.toUtf8(is));
		assertThrowsWithMessage(Exception.class, "Response has already been consumed.", ()->r1.getContent().asInputStream());

		// Non-repeatable entity.
		var x = testClient().entity(inputStreamEntity("{f:2}"));
		var r2 = x.get("/bean").run();
		r2.getContent().asInputStream();
		assertThrowsWithMessage(Exception.class, "Response has already been consumed", ()->r2.getContent().asInputStream());

		// Repeatable entity.
		x.entity(new StringEntity("{f:2}"));
		var r3 = x.get("/bean").run();
		r3.getContent().asInputStream();
		is = r3.getContent().asInputStream();
		assertEquals("{f:2}", StringUtils.toUtf8(is));
		is = x.get("/bean").run().getContent().asInputStream();
		((EofSensorInputStream)is).abortConnection();

		var rci = new BasicRestCallInterceptor() {
			@Override
			public void onClose(RestRequest req, RestResponse res) throws Exception {
				throw new NullPointerException("foo");
			}
		};

		var x2 = client().interceptors(rci).build(TestClient.class).entity(new StringEntity("{f:2}"));
		assertThrowsWithMessage(NullPointerException.class, "foo", ()->x2.get("/bean").run().getContent().cache().asInputStream());
		assertThrowsWithMessage(NullPointerException.class, "foo", ()->x2.get("/bean").run().getContent().asInputStream().close());
		assertThrowsWithMessage(NullPointerException.class, "foo", ()->((EofSensorInputStream)x2.get("/bean").run().getContent().asInputStream()).abortConnection());  // NOSONAR
	}

	@Test void a04_asReader() throws Exception {
		var x = testClient();
		x.entity(inputStreamEntity("{f:1}"));
		var r = x.get("/bean").run().getContent().asReader();
		assertString("{f:1}", r);

		x.entity(inputStreamEntity("{f:1}"));
		r = x.get("/bean").run().getContent().asReader(UTF8);
		assertString("{f:1}", r);

		x.entity(inputStreamEntity("{f:1}"));
		r = x.get("/bean").run().getContent().asReader(null);
		assertString("{f:1}", r);
	}

	@Test void a05_asBytes() throws Exception {
		var x = client().build().get("/bean").run().getContent().asBytes();
		assertEquals("{f:1}", StringUtils.toUtf8(x));

		x = client().build().get("/bean").run().assertContent().asBytes().asString().is("{f:1}").getContent().asBytes();
		assertEquals("{f:1}", StringUtils.toUtf8(x));

		assertThrowsWithMessage(Exception.class, "foo", ()->testClient().entity(new InputStreamEntity(badStream())).get().run().getContent().asBytes());
	}

	@Test void a06_pipeTo() throws Exception {
		var baos = new ByteArrayOutputStream();
		client().build().get("/bean").run().getContent().pipeTo(baos);
		assertEquals("{f:1}", StringUtils.toUtf8(baos.toByteArray()));

		var sw = new StringWriter();
		client().build().get("/bean").run().getContent().pipeTo(sw);
		assertEquals("{f:1}", sw.toString());

		sw = new StringWriter();
		client().build().get("/bean").run().getContent().pipeTo(sw,UTF8);
		assertEquals("{f:1}", sw.toString());
	}

	public static class A7a {
		String x;
		public static A7a fromReader(Reader r) throws IOException {
			var x = new A7a();
			x.x = read(r);
			return x;
		}
	}

	public static class A7b {
		String x;
		public static A7b fromInputStream(InputStream is) throws IOException {
			var x = new A7b();
			x.x = read(is);
			return x;
		}
	}

	public static class A7c {
		public A7c() {
			throw new RuntimeException("foo");
		}
	}

	@Test void a07_asType() throws Exception {
		var x1 = testClient().entity(stringEntity("[1,2]")).get().run().getContent().as(List.class,Integer.class);
		assertList(x1, "1", "2");

		var x3 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(ABean.class);
		assertBean(x3, "f", "1");

		testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(ResponseContent.class);

		var x6 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(HttpEntity.class);
		assertTrue(x6 instanceof ResponseContent);

		plainTestClient().entity(stringEntity("foo")).get().run().assertContent().as(A7a.class).is(x->x.x.equals("foo"));
		plainTestClient().entity(stringEntity("foo")).get().run().assertContent().as(A7b.class).is(x->x.x.equals("foo"));
		assertThrowsWithMessage(Exception.class, "Unsupported media-type", ()->plainTestClient().entity(stringEntity("foo")).headers(header("Content-Type","foo")).get().run().getContent().as(A7c.class));
		assertThrowsWithMessage(Exception.class, "foo", ()->testClient().entity(stringEntity("")).get().run().getContent().as(A7c.class));

		var x8 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asFuture(ABean.class);
		assertBean(x8.get(), "f", "1");

		var x10 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asFuture(cm(ABean.class));
		assertBean(x10.get(), "f", "1");

		var x12 = testClient().entity(stringEntity("[1,2]")).get().run().getContent().asFuture(List.class,Integer.class);
		assertList(x12.get(), "1", "2");

		var x14 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asString();
		assertEquals("{f:1}", x14);

		assertThrowsWithMessage(Exception.class, "foo", ()->testClient().entity(new InputStreamEntity(badStream())).get().run().getContent().asString());

		var x16 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asStringFuture();
		assertEquals("{f:1}", x16.get());

		var x18 = testClient().entity(stringEntity("12345")).get().run().getContent().asAbbreviatedString(4);
		assertEquals("1...", x18);

		var x20 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asObjectRest(ABean.class);
		assertString("1", x20.get("f"));

		var x22 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asObjectRest();
		assertString("1", x22.get("f"));

		var x24 = testClient().entity(stringEntity("foo=123")).get().run().getContent().asMatcher(Pattern.compile("foo=(.*)"));
		assertTrue(x24.matches());
		assertEquals("123", x24.group(1));

		var x26 = testClient().entity(stringEntity("foo=123")).get().run().getContent().asMatcher("foo=(.*)");
		assertTrue(x26.matches());
		assertEquals("123", x26.group(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpEntity passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Test void b01_httpEntityMethods() throws Exception {
		var x1 = testClient().entity(stringEntity("foo")).get().run().getContent();
		assertTrue(x1.isRepeatable());

		var x2 = testClient().entity(inputStreamEntity("foo")).get().run().getContent();
		assertFalse(x2.isRepeatable());
		assertEquals(-1L, x2.getContentLength());
		x2.cache().asString();
		assertTrue(x2.isRepeatable());
		assertEquals(3L, x2.getContentLength());

		assertFalse(x2.isChunked());

		testClient().entity(inputStreamEntity("foo")).get().run().getContent().getContentEncoding().assertValue().isNull();

		var x3 = inputStreamEntity("foo");
		x3.setContentType("text/foo");
		x3.setContentEncoding("identity");
		testClient().entity(x3).get().run().getContent().response()
			.getContent().getContentType().assertValue().is("text/foo").response()
			.getContent().getContentEncoding().assertValue().is("identity");

		var x4 = testClient().entity(inputStreamEntity("foo")).get().run().getContent().asInputStream();
		assertBytes(x4).asString().is("foo");

		var x5 = new ByteArrayOutputStream();
		testClient().entity(inputStreamEntity("foo")).get().run().getContent().writeTo(x5);
		assertBytes(x5.toByteArray()).asString().is("foo");

		assertTrue(testClient().entity(inputStreamEntity("foo")).get().run().getContent().isStreaming());
		assertFalse(testClient().entity(inputStreamEntity("foo")).get().run().getContent().cache().isStreaming());
		assertFalse(testClient().entity(stringEntity("foo")).get().run().getContent().isStreaming());

		testClient().entity(inputStreamEntity("foo")).get().run().getContent().consumeContent();
	}

	@SuppressWarnings("deprecation")
	@Test void b02_head() throws Exception {
		assertFalse(client().build().head("").run().getContent().isRepeatable());
		assertFalse(client().build().head("").run().getContent().isChunked());
		assertEquals(-1L, client().build().head("").run().getContent().getContentLength());
		client().build().head("").run().getContent().getContentType().assertValue().isNull();
		client().build().head("").run().getContent().getContentEncoding().assertValue().isNull();
		client().build().head("").run().getContent().writeTo(new ByteArrayOutputStream());
		assertFalse(client().build().head("").run().getContent().isStreaming());
		client().build().head("").run().getContent().consumeContent();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class).json5();
	}

	private static TestClient plainTestClient() {
		return MockRestClient.create(A.class).noTrace().build(TestClient.class);
	}

	private static TestClient testClient() {
		return MockRestClient.create(A.class).json5().noTrace().build(TestClient.class);
	}

	private static StringEntity stringEntity(String in) {
		return new StringEntity(in, (ContentType)null);
	}

	private static Header header(String name, Object val) {
		return basicHeader(name, val);
	}

	private static InputStreamEntity inputStreamEntity(String in) {
		return new InputStreamEntity(inputStream(in));
	}

	private static <T> ClassMeta<T> cm(Class<T> t) {
		 return BeanContext.DEFAULT.getClassMeta(t);
	}

	private static InputStream badStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("foo");
			}
		};
	}
}