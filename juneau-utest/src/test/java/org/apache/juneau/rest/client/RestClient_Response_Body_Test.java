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

import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.testutils.StreamUtils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

import org.apache.http.*;
import org.apache.http.conn.*;
import org.apache.http.entity.*;
import org.apache.http.entity.ContentType;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Response_Body_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
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
		public void head() {
		}
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
			HttpResponse r = new BasicHttpResponse(new ProtocolVersion("http", 1,1),200,"");
			r.setEntity(responseEntity);
			for (Header h : headers)
				r.addHeader(h);
			return new MockRestResponse(this, request, r, parser);
		}
	}

	@Test
	public void a01_basic() throws Exception {
		client().build().post("/echo",bean).run().assertContent().as(ABean.class).asJson().is("{f:1}");
		client().build().post("/echo",bean).run().assertContent().asBytes().asString().is("{f:1}");
	}

	@Test
	public void a02_overrideParser() throws Exception {
		RestClient x = client().build();
		ABean b = x.post("/echo",bean).run().getContent().parser(JsonParser.DEFAULT).as(ABean.class);
		assertObject(b).asJson().is("{f:1}");
		assertThrown(()->x.post("/echo",bean).run().getContent().parser(XmlParser.DEFAULT).as(ABean.class)).asMessages().isAny(contains("ParseError at [row,col]:[1,1]"));
		assertThrown(()->x.post("/echo",bean).run().getContent().parser(XmlParser.DEFAULT).assertValue().as(ABean.class)).asMessages().isAny(contains("ParseError at [row,col]:[1,1]"));
	}

	@Test
	public void a03_asInputStream() throws Exception {
		RestResponse r1 = client().build().get("/bean").run();
		InputStream is = r1.getContent().asInputStream();
		assertBytes(is).asString().is("{f:1}");
		assertThrown(()->r1.getContent().asInputStream()).asMessage().isContains("Response has already been consumed.");

		// Non-repeatable entity.
		TestClient x = testClient().entity(inputStreamEntity("{f:2}"));
		RestResponse r2 = x.get("/bean").run();
		r2.getContent().asInputStream();
		assertThrown(()->r2.getContent().asInputStream()).asMessage().isContains("Response has already been consumed");

		// Repeatable entity.
		x.entity(new StringEntity("{f:2}"));
		RestResponse r3 = x.get("/bean").run();
		r3.getContent().asInputStream();
		is = r3.getContent().asInputStream();
		assertBytes(is).asString().is("{f:2}");
		is = x.get("/bean").run().getContent().asInputStream();
		((EofSensorInputStream)is).abortConnection();

		RestCallInterceptor rci = new BasicRestCallInterceptor() {
			@Override
			public void onClose(RestRequest req, RestResponse res) throws Exception {
				throw new NullPointerException("foo");
			}
		};

		TestClient x2 = client().interceptors(rci).build(TestClient.class).entity(new StringEntity("{f:2}"));
		assertThrown(()->x2.get("/bean").run().getContent().cache().asInputStream()).asMessage().is("foo");
		assertThrown(()->x2.get("/bean").run().getContent().asInputStream().close()).asMessage().is("foo");
		assertThrown(()->((EofSensorInputStream)x2.get("/bean").run().getContent().asInputStream()).abortConnection()).asMessage().is("foo");
	}

	@Test
	public void a04_asReader() throws Exception {
		TestClient x = testClient();
		x.entity(inputStreamEntity("{f:1}"));
		Reader r = x.get("/bean").run().getContent().asReader();
		assertReader(r).is("{f:1}");

		x.entity(inputStreamEntity("{f:1}"));
		r = x.get("/bean").run().getContent().asReader(UTF8);
		assertReader(r).is("{f:1}");

		x.entity(inputStreamEntity("{f:1}"));
		r = x.get("/bean").run().getContent().asReader(null);
		assertReader(r).is("{f:1}");
	}

	@Test
	public void a05_asBytes() throws Exception {
		byte[] x = client().build().get("/bean").run().getContent().asBytes();
		assertBytes(x).asString().is("{f:1}");

		x = client().build().get("/bean").run().assertContent().asBytes().asString().is("{f:1}").getContent().asBytes();
		assertBytes(x).asString().is("{f:1}");

		assertThrown(()->testClient().entity(new InputStreamEntity(badStream())).get().run().getContent().asBytes()).asMessages().isContains("foo");
	}

	@Test
	public void a06_pipeTo() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		client().build().get("/bean").run().getContent().pipeTo(baos);
		assertBytes(baos.toByteArray()).asString().is("{f:1}");

		StringWriter sw = new StringWriter();
		client().build().get("/bean").run().getContent().pipeTo(sw);
		assertString(sw.toString()).is("{f:1}");

		sw = new StringWriter();
		client().build().get("/bean").run().getContent().pipeTo(sw,UTF8);
		assertString(sw.toString()).is("{f:1}");
	}

	public static class A7a {
		String x;
		public static A7a fromReader(Reader r) throws IOException {
			A7a x = new A7a();
			x.x = read(r);
			return x;
		}
	}

	public static class A7b {
		String x;
		public static A7b fromInputStream(InputStream is) throws IOException {
			A7b x = new A7b();
			x.x = read(is);
			return x;
		}
	}

	public static class A7c {
		public A7c() {
			throw new RuntimeException("foo");
		}
	}

	@Test
	public void a07_asType() throws Exception {
		List<Integer> x1 = testClient().entity(stringEntity("[1,2]")).get().run().getContent().as(List.class,Integer.class);
		assertObject(x1).asJson().is("[1,2]");

		ABean x3 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(ABean.class);
		assertObject(x3).asJson().is("{f:1}");

		HttpEntity x5 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(ResponseContent.class);
		assertTrue(x5 instanceof ResponseContent);

		HttpEntity x6 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().as(HttpEntity.class);
		assertTrue(x6 instanceof ResponseContent);

		plainTestClient().entity(stringEntity("foo")).get().run().assertContent().as(A7a.class).is(x->x.x.equals("foo"));
		plainTestClient().entity(stringEntity("foo")).get().run().assertContent().as(A7b.class).is(x->x.x.equals("foo"));
		assertThrown(()->plainTestClient().entity(stringEntity("foo")).headers(header("Content-Type","foo")).get().run().getContent().as(A7c.class)).isExists().asMessages().isAny(contains("Unsupported media-type"));
		assertThrown(()->testClient().entity(stringEntity("")).get().run().getContent().as(A7c.class)).asMessages().isContains("foo");

		Future<ABean> x8 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asFuture(ABean.class);
		assertObject(x8.get()).asJson().is("{f:1}");

		Future<ABean> x10 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asFuture(cm(ABean.class));
		assertObject(x10.get()).asJson().is("{f:1}");

		Future<List<Integer>> x12 = testClient().entity(stringEntity("[1,2]")).get().run().getContent().asFuture(List.class,Integer.class);
		assertObject(x12.get()).asJson().is("[1,2]");

		String x14 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asString();
		assertString(x14).is("{f:1}");

		assertThrown(()->testClient().entity(new InputStreamEntity(badStream())).get().run().getContent().asString()).asMessages().isContains("foo");

		Future<String> x16 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asStringFuture();
		assertString(x16.get()).is("{f:1}");

		String x18 = testClient().entity(stringEntity("12345")).get().run().getContent().asAbbreviatedString(4);
		assertString(x18).is("1...");

		ObjectRest x20 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asObjectRest(ABean.class);
		assertString(x20.get("f")).is("1");

		ObjectRest x22 = testClient().entity(stringEntity("{f:1}")).get().run().getContent().asObjectRest();
		assertString(x22.get("f")).is("1");

		Matcher x24 = testClient().entity(stringEntity("foo=123")).get().run().getContent().asMatcher(Pattern.compile("foo=(.*)"));
		assertTrue(x24.matches());
		assertString(x24.group(1)).is("123");

		Matcher x26 = testClient().entity(stringEntity("foo=123")).get().run().getContent().asMatcher("foo=(.*)");
		assertTrue(x26.matches());
		assertString(x26.group(1)).is("123");
	}

	//------------------------------------------------------------------------------------------------------------------
	// HttpEntity passthrough methods.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Test
	public void b01_httpEntityMethods() throws Exception {
		ResponseContent x1 = testClient().entity(stringEntity("foo")).get().run().getContent();
		assertTrue(x1.isRepeatable());

		ResponseContent x2 = testClient().entity(inputStreamEntity("foo")).get().run().getContent();
		assertFalse(x2.isRepeatable());
		assertLong(x2.getContentLength()).is(-1l);
		x2.cache().asString();
		assertTrue(x2.isRepeatable());
		assertLong(x2.getContentLength()).is(3l);

		assertFalse(x2.isChunked());

		testClient().entity(inputStreamEntity("foo")).get().run().getContent().getContentEncoding().assertValue().isNull();

		InputStreamEntity x3 = inputStreamEntity("foo");
		x3.setContentType("text/foo");
		x3.setContentEncoding("identity");
		testClient().entity(x3).get().run().getContent().response()
			.getContent().getContentType().assertValue().is("text/foo").response()
			.getContent().getContentEncoding().assertValue().is("identity");

		InputStream x4 = testClient().entity(inputStreamEntity("foo")).get().run().getContent().asInputStream();
		assertBytes(x4).asString().is("foo");

		ByteArrayOutputStream x5 = new ByteArrayOutputStream();
		testClient().entity(inputStreamEntity("foo")).get().run().getContent().writeTo(x5);
		assertBytes(x5.toByteArray()).asString().is("foo");

		assertTrue(testClient().entity(inputStreamEntity("foo")).get().run().getContent().isStreaming());
		assertFalse(testClient().entity(inputStreamEntity("foo")).get().run().getContent().cache().isStreaming());
		assertFalse(testClient().entity(stringEntity("foo")).get().run().getContent().isStreaming());

		testClient().entity(inputStreamEntity("foo")).get().run().getContent().consumeContent();
	}

	@SuppressWarnings("deprecation")
	@Test
	public void b02_head() throws Exception {
		assertFalse(client().build().head("").run().getContent().isRepeatable());
		assertFalse(client().build().head("").run().getContent().isChunked());
		assertLong(client().build().head("").run().getContent().getContentLength()).is(-1l);
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
