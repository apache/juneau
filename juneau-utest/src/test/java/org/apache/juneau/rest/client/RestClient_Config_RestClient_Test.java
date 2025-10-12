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
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResponses.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.httppart.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.utest.utils.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class RestClient_Config_RestClient_Test extends TestBase {

	public static class ABean {
		public int f;
		static ABean get() {
			var x = new ABean();
			x.f = 1;
			return x;
		}
		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestOp(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestOp(path="/bean")
		public ABean postBean(@Content ABean b) {
			return b;
		}
		@RestOp(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestOp(path="/echoBody")
		public Reader postEchoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getContent().getReader();
		}
		@RestOp(path="/ok")
		public Ok getOk() {
			return OK;
		}
		@RestOp(path="/checkHeader")
		public String[] getHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeaderParam("Check").orElse(null)).stream().map(RequestHeader::getValue).toArray(String[]::new);
		}
	}

	public static class A1 extends BasicRestCallHandler {
		public A1(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws IOException {
			request.addHeader("Check","Foo");
			request.addHeader("Foo","baz");
			return super.run(target,request,context);
		}
	}

	@Test void a01_callHandler() throws Exception {
		client().callHandler(A1.class).header("Foo","f1").build().get("/checkHeader").header("Foo","f2").run().assertContent("['f1','f2','baz']");
	}

	@Test void a02_errorCodes() {
		var x1 = client().errorCodes(x -> x == 200).build();
		var x2 = client().build();
		assertEquals(200, ((RestCallException)assertThrows(Throwable.class, ()->x1.get("/echo").run())).getResponseCode());
		assertEquals(200, ((RestCallException)assertThrows(Throwable.class, ()->x2.get("/echo").errorCodes(x -> x == 200).run())).getResponseCode());
	}

	@Test void a03_executorService() throws Exception {
		var es = new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(10));
		var x1 = client().executorService(es,true).build();

		assertEquals(es,x1.getExecutorService());
		x1.get("/echo").runFuture().get().assertStatus(200).assertContent().isContains("GET /echo HTTP/1.1");

		es = null;
		var x2 = client().executorService(es,true).build();
		assertNotNull(x2.getExecutorService());
		x2.get("/echo").runFuture().get().assertStatus(200).assertContent().isContains("GET /echo HTTP/1.1");
	}

	@Test void a04_keepHttpClientOpen() throws Exception {
		var x = client().keepHttpClientOpen().build();

		var c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertContent().isContains("OK");

		x = client().keepHttpClientOpen().build();
		c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertContent().isContains("OK");
	}

	public static class A5 extends BasicRestCallInterceptor {
		public static int x;
		@Override
		public void onInit(RestRequest req) throws Exception {
			x = 1;
			req.header("Foo","f2");
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			x += 10;
			res.addHeader("Bar","b1");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			x += 100;
		}
	}

	public static class A5a extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	public static class A5b extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
	}

	public static class A5c extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	public static class A5d extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
			throw new RestCallException(null,null,"foo");
		}
	}

	public static class A5e extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new RestCallException(null,null,"foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
	}

	public static class A5f extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	public static class A5g extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
			throw new IOException("foo");
		}
	}

	public static class A5h extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new IOException("foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
	}

	public static class A5i extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {}  // NOSONAR
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {}  // NOSONAR
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
	}

	@Test void a05_interceptors() throws Exception {
		client().header("Foo","f1").interceptors(A5.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertContent("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		client().header("Foo","f1").interceptors(new A5()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertContent("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5()).header("Check","foo").header("Foo","f3").run().assertContent("['f1','f2','f3']").assertHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(A5a.class).build().get("/checkHeader"));
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(A5b.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(A5c.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5a()).build().get("/checkHeader"));
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5b()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5c()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5a()));
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5b()).header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5c()).header("Check","foo").header("Foo","f3").run().close());

		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(A5d.class).build().get("/checkHeader"));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(A5e.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(A5f.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5d()).build().get("/checkHeader"));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5e()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5f()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5d()));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5e()).header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5f()).header("Check","foo").header("Foo","f3").run().close());

		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(A5g.class).build().get("/checkHeader"));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(A5h.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(A5i.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5g()).build().get("/checkHeader"));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5h()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").interceptors(new A5i()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close());
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5g()));
		assertThrowsWithMessage(RestCallException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5h()).header("Check","foo").header("Foo","f3").run());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5i()).header("Check","foo").header("Foo","f3").run().close());

		assertThrowsWithMessage(ConfigException.class, "Invalid class of type 'java.lang.String' passed to interceptors().", ()->client().interceptors(String.class));
		assertThrowsWithMessage(ConfigException.class, "Invalid object of type 'java.lang.String' passed to interceptors().", ()->client().interceptors(""));
		client().interceptors((Object)null).header("Foo","f1").build().get("/checkHeader");
		client().interceptors((Class<?>)null).header("Foo","f1").build().get("/checkHeader");
	}

	public static class A6a extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onInit(RestRequest req) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class A6b extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onConnect(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}
	public static class A6c extends BasicRestCallInterceptor {
		@Override /* RestCallInterceptor */
		public void onClose(RestRequest req, RestResponse res) throws Exception { throw new RuntimeException("foo"); }
	}

	@Test void a06_interceptors_exceptionHandling() {
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().interceptors(A6a.class).build().post("/bean",bean).complete());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().interceptors(A6b.class).build().post("/bean",bean).complete());
		assertThrowsWithMessage(RuntimeException.class, "foo", ()->client().interceptors(A6c.class).build().post("/bean",bean).complete());
	}

	public static class A7 extends RestClient {
		private static String lastMessage;
		public A7(RestClient.Builder builder) {
			super(builder);
		}
		@Override
		public void log(Level level,String msg,Object...args) {
			lastMessage = msg;
		}
	}

	@Test void a07_leakDetection() throws Throwable {
		client().detectLeaks().build(A7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.",A7.lastMessage);
	}

	@Test void a08_marshaller() throws Exception {
		var rc = MockRestClient.create(A.class).marshaller(Xml.DEFAULT).build();
		var b = rc.post("/echoBody",bean).run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));
	}

	@Test void a09_marshalls() throws Exception {
		var x = MockRestClient.create(A.class).marshallers(Xml.DEFAULT,Json.DEFAULT).build();

		assertThrowsWithMessage(RestCallException.class, "Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.", ()->x.post("/echoBody",bean).run());

		assertThrowsWithMessage(Exception.class, "Content-Type not specified in response header.  Cannot find appropriate parser.", ()->x.post("/echoBody",bean).contentType("text/json").run().getContent().as(ABean.class));

		var b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).mediaType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).mediaType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));
	}

	@Test void a10_serializer_parser() throws Exception {
		var x = MockRestClient.create(A.class).serializer(XmlSerializer.class).parser(XmlParser.class).build();

		var b = x.post("/echoBody",bean).run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		x = MockRestClient.create(A.class).serializer(XmlSerializer.DEFAULT).parser(XmlParser.DEFAULT).build();
		b = x.post("/echoBody",bean).run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));
	}

	@Test void a11_serializers_parsers() throws Exception {
		var x = MockRestClient.create(A.class).serializers(a(XmlSerializer.class,JsonSerializer.class)).parsers(a(XmlParser.class,JsonParser.class)).build();

		assertThrowsWithMessage(RestCallException.class, "Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.", ()->x.post("/echoBody",bean).run());

		assertThrowsWithMessage(Exception.class, "Content-Type not specified in response header.  Cannot find appropriate parser.", ()->x.post("/echoBody",bean).contentType("text/json").run().getContent().as(ABean.class));

		var b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).mediaType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x.post("/echoBody",bean).mediaType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		var x2 = MockRestClient.create(A.class).serializers(XmlSerializer.DEFAULT,JsonSerializer.DEFAULT).parsers(XmlParser.DEFAULT,JsonParser.DEFAULT).build();

		assertThrowsWithMessage(RestCallException.class, "Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.", ()->x2.post("/echoBody",bean).run());

		assertThrowsWithMessage(Exception.class, "Content-Type not specified in response header.  Cannot find appropriate parser.", ()->x2.post("/echoBody",bean).contentType("text/json").run().getContent().as(ABean.class));

		b = x2.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x2.post("/echoBody",bean).mediaType("text/xml").run().cacheContent().assertContent("<object><f>1</f></object>").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x2.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));

		b = x2.post("/echoBody",bean).mediaType("text/json").run().cacheContent().assertContent("{\"f\":1}").getContent().as(ABean.class);
		assertEquals(json(bean), json(b));
	}

	@Rest(partSerializer=A12a.class,partParser=A12b.class)
	public static class A12 extends BasicRestObject {
		@RestGet(path="/")
		public Ok get(@Header(name="Foo") @Schema(cf="multi") ABean[] foo,org.apache.juneau.rest.RestRequest req,org.apache.juneau.rest.RestResponse res) throws Exception {
			assertEquals(2,foo.length);
			assertList(req.getHeaders().getAll("Foo").stream().map(RequestHeader::getValue).toList(), "x{f:1}", "x{f:1}");
			assertEquals("{f:1}",foo[0].toString());
			assertEquals("{f:1}",foo[1].toString());
			res.setHeader("Foo",bean);
			return OK;
		}
	}

	public static class A12a extends FakeWriterSerializer {
		public A12a(Builder builder) {
			super(builder.partFunction((t,s,o)->"x" + Json5.of(o)));
		}
	}

	public static class A12b extends FakeReaderParser {
		public A12b(Builder builder) {
			super(builder.partFunction(A12b::in));
		}

		private static Object in(HttpPartType type, HttpPartSchema schema, String in, ClassMeta<?> c) {
			try {
				if (c.isInstanceOf(ABean.class))
					return Json5.DEFAULT.read(in.substring(1),c);
				return SimplePartParser.DEFAULT.parse(type,schema,in,c);
			} catch (Exception e) {
				throw asRuntimeException(e);
			}
		}
	}

	@Test void a12_partSerializer_partParser() throws Exception {
		var x = client(A12.class).headers(serializedHeader("Foo", bean)).partSerializer(A12a.class).partParser(A12b.class).build();
		var b = x.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class).get();
		assertEquals("{f:1}",b.toString());
		b = x.get().header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class).get();
		assertEquals("{f:1}",b.toString());

		x = client(A12.class).headers(serializedHeader("Foo", bean)).partSerializer(new A12a(FakeWriterSerializer.create())).partParser(new A12b(FakeReaderParser.create())).build();
		b = x.get("/").header("Foo",bean).run().assertHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class).get();
		assertEquals("{f:1}",b.toString());
	}


	@Test void a13_toString() {
		var s = client().rootUrl("https://foo").build().toString();
		assertTrue(s.contains("rootUrl: 'https://foo'"));
	}

	@Test void a14_request_target() throws Exception {
		client().build().get("/bean").target(new HttpHost("localhost")).run().assertContent("{f:1}");
	}

	@Test void a15_request_context() throws Exception {
		client().build().get("/bean").context(new BasicHttpContext()).run().assertContent("{f:1}");
	}

	@Test void a16_request_uriParts() throws Exception {
		var uri = client().build().get().uriScheme("http").uriHost("localhost").uriPort(8080).uriUserInfo("foo:bar").uri("/bean").uriFragment("baz").queryData("foo","bar").run().assertContent("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uriScheme("http").uriHost("localhost").uriPort(8080).uriUserInfo("foo","bar").uri("/bean").uriFragment("baz").queryData("foo","bar").run().assertContent("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri("http://localhost").uri("http://foo:bar@localhost:8080/bean?foo=bar#baz").run().assertContent("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri(new java.net.URI(null,null,null,null)).uri(new java.net.URI("http://foo:bar@localhost:8080/bean?foo=bar#baz")).run().assertContent("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());
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
}