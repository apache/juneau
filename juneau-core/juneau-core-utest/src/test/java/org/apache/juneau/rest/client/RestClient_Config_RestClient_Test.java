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
import static org.apache.juneau.rest.client.RestClient.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.impl.client.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.response.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.json.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestClient_Config_RestClient_Test {

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

	private static ABean bean = ABean.get();

	@Rest
	public static class A extends BasicRestObject {
		@RestOp(path="/bean")
		public ABean getBean() {
			return bean;
		}
		@RestOp(path="/bean")
		public ABean postBean(@Body ABean b) {
			return b;
		}
		@RestOp(path="/echo/*")
		public String getEcho(org.apache.juneau.rest.RestRequest req) {
			return req.toString();
		}
		@RestOp(path="/echoBody")
		public Reader postEchoBody(org.apache.juneau.rest.RestRequest req) throws IOException {
			return req.getBody().getReader();
		}
		@RestOp(path="/ok")
		public Ok getOk() {
			return Ok.OK;
		}
		@RestOp(path="/checkHeader")
		public String[] getHeader(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().get(req.getHeader("Check"));
		}
	}

	public static class A1 extends BasicRestCallHandler {
		public A1(RestClient client) {
			super(client);
		}
		@Override
		public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws ClientProtocolException, IOException {
			request.addHeader("Check","Foo");
			request.addHeader("Foo","baz");
			return super.run(target,request,context);
		}
	}

	@Test
	public void a01_callHandler() throws Exception {
		RestCallHandler x = new RestCallHandler() {
			@Override
			public HttpResponse run(HttpHost target, HttpRequest request, HttpContext context) throws ClientProtocolException, IOException {
				return new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http",1,1),201,null));
			}
		};
		client().callHandler(A1.class).header("Foo","f1").build().get("/checkHeader").header("Foo","f2").run().assertBody().is("['f1','f2','baz']");
		client().callHandler(x).header("Foo","f1").build().get("/checkHeader").header("Foo","f2").run().assertCode().is(201);
	}

	@Test
	public void a02_errorCodes() throws Exception {
		RestClient x1 = client().errorCodes(x -> x == 200).ignoreErrors(false).build();
		RestClient x2 = client().ignoreErrors(false).build();
		assertThrown(()->x1.get("/echo").run()).passes(x -> ((RestCallException)x).getResponseCode() == 200);
		assertThrown(()->x2.get("/echo").errorCodes(x -> x == 200).run()).passes(x -> ((RestCallException)x).getResponseCode() == 200);
	}

	@Test
	public void a03_executorService() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(1,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(10));
		RestClient x1 = client().executorService(es,true).build();

		assertEquals(es,x1.getExecutorService());
		x1.get("/echo").runFuture().get().assertCode().is(200).assertBody().contains("HTTP GET /echo");

		es = null;
		RestClient x2 = client().executorService(es,true).build();
		assertNotNull(x2.getExecutorService());
		x2.get("/echo").runFuture().get().assertCode().is(200).assertBody().contains("HTTP GET /echo");
	}

	@Test
	public void a04_keepHttpClientOpen() throws Exception {
		RestClient x = client().keepHttpClientOpen().build();

		CloseableHttpClient c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");

		x = client().keepHttpClientOpen().build();
		c = x.httpClient;
		x.close();
		client().httpClient(c).build().get("/ok").runFuture().get().assertBody().contains("OK");
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
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new RuntimeException("foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
		}
	}

	public static class A5c extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
		}
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
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new RestCallException(null,null,"foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
		}
	}

	public static class A5f extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new RestCallException(null,null,"foo");
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
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
			throw new IOException("foo");
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
		}
	}

	public static class A5i extends BasicRestCallInterceptor {
		@Override
		public void onInit(RestRequest req) throws Exception {
		}
		@Override
		public void onConnect(RestRequest req, RestResponse res) throws Exception {
		}
		@Override
		public void onClose(RestRequest req, RestResponse res) throws Exception {
			throw new IOException("foo");
		}
	}

	@Test
	public void a05_interceptors() throws Exception {
		client().header("Foo","f1").interceptors(A5.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertStringHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		client().header("Foo","f1").interceptors(new A5()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertStringHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5()).header("Check","foo").header("Foo","f3").run().assertBody().is("['f1','f2','f3']").assertStringHeader("Bar").is("b1");
		assertEquals(111,A5.x);

		assertThrown(()->client().header("Foo","f1").interceptors(A5a.class).build().get("/checkHeader")).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5b.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5c.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5a()).build().get("/checkHeader")).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5b()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5c()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5a())).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5b()).header("Check","foo").header("Foo","f3").run()).isType(RuntimeException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5c()).header("Check","foo").header("Foo","f3").run().close()).isType(RuntimeException.class).is("foo");

		assertThrown(()->client().header("Foo","f1").interceptors(A5d.class).build().get("/checkHeader")).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5e.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5f.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5d()).build().get("/checkHeader")).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5e()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5f()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5d())).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5e()).header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).is("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5f()).header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).is("foo");

		assertThrown(()->client().header("Foo","f1").interceptors(A5g.class).build().get("/checkHeader")).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5h.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(A5i.class).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5g()).build().get("/checkHeader")).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5h()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").interceptors(new A5i()).build().get("/checkHeader").header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5g())).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5h()).header("Check","foo").header("Foo","f3").run()).isType(RestCallException.class).contains("foo");
		assertThrown(()->client().header("Foo","f1").build().get("/checkHeader").interceptors(new A5i()).header("Check","foo").header("Foo","f3").run().close()).isType(RestCallException.class).contains("foo");

		assertThrown(()->client().interceptors(String.class)).is("Invalid class of type 'java.lang.String' passed to interceptors().");
		assertThrown(()->client().interceptors("")).is("Invalid object of type 'java.lang.String' passed to interceptors().");
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

	@Test
	public void a06_interceptors_exceptionHandling() throws Exception {
		assertThrown(()->client().interceptors(A6a.class).build().post("/bean",bean).complete()).is("foo");
		assertThrown(()->client().interceptors(A6b.class).build().post("/bean",bean).complete()).is("foo");
		assertThrown(()->client().interceptors(A6c.class).build().post("/bean",bean).complete()).is("foo");
	}

	public static class A7 extends RestClient {
		private static String lastMessage;
		public A7(PropertyStore ps) {
			super(ps);
		}
		@Override
		public void log(Level level,String msg,Object...args) {
			lastMessage = msg;
		}
	}

	@Test
	public void a07_leakDetection() throws Throwable {
		client().leakDetection().build(A7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.",A7.lastMessage);

		client().debug().build(A7.class).finalize();
		assertTrue(A7.lastMessage.startsWith("WARNING:  RestClient garbage collected before it was finalized.\nCreation Stack:\n\t"));

		client().leakDetection().build(A7.class).finalize();
		assertEquals("WARNING:  RestClient garbage collected before it was finalized.",A7.lastMessage);
	}

	@Test
	public void a08_marshall() throws Exception {
		RestClient rc = MockRestClient.create(A.class).marshall(Xml.DEFAULT).build();
		ABean b = rc.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);
	}

	@Test
	public void a09_marshalls() throws Exception {
		final RestClient x = MockRestClient.create(A.class).marshalls(Xml.DEFAULT,Json.DEFAULT).build();

		assertThrown(()->x.post("/echoBody",bean).run()).contains("Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");

		assertThrown(()->x.post("/echoBody",bean).contentType("text/json").run().getBody().as(ABean.class)).contains("Content-Type not specified in response header.  Cannot find appropriate parser.");

		ABean b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).mediaType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).mediaType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);
	}

	@Test
	public void a10_serializer_parser() throws Exception {
		RestClient x = MockRestClient.create(A.class).serializer(XmlSerializer.class).parser(XmlParser.class).build();

		ABean b = x.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		x = MockRestClient.create(A.class).serializer(XmlSerializer.DEFAULT).parser(XmlParser.DEFAULT).build();
		b = x.post("/echoBody",bean).run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);
		assertThrown(()->MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers,String.class).build()).contains("RESTCLIENT_serializers property had invalid class of type 'java.lang.String'");
		assertThrown(()->MockRestClient.create(A.class).prependTo(RESTCLIENT_serializers,"").build()).contains("RESTCLIENT_serializers property had invalid object of type 'java.lang.String'");
		assertThrown(()->MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers,String.class).build()).contains("RESTCLIENT_parsers property had invalid class of type 'java.lang.String'");
		assertThrown(()->MockRestClient.create(A.class).prependTo(RESTCLIENT_parsers,"").build()).contains("RESTCLIENT_parsers property had invalid object of type 'java.lang.String'");
	}

	@Test
	public void a11_serializers_parsers() throws Exception {
		@SuppressWarnings("unchecked")
		final RestClient x = MockRestClient.create(A.class).serializers(XmlSerializer.class,JsonSerializer.class).parsers(XmlParser.class,JsonParser.class).build();

		assertThrown(()->x.post("/echoBody",bean).run()).contains("Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");

		assertThrown(()->x.post("/echoBody",bean).contentType("text/json").run().getBody().as(ABean.class)).contains("Content-Type not specified in response header.  Cannot find appropriate parser.");

		ABean b = x.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).mediaType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x.post("/echoBody",bean).mediaType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		final RestClient x2 = MockRestClient.create(A.class).serializers(XmlSerializer.DEFAULT,JsonSerializer.DEFAULT).parsers(XmlParser.DEFAULT,JsonParser.DEFAULT).build();

		assertThrown(()->x2.post("/echoBody",bean).run()).contains("Content-Type not specified on request.  Cannot match correct serializer.  Use contentType(String) or mediaType(String) to specify transport language.");

		assertThrown(()->x2.post("/echoBody",bean).contentType("text/json").run().getBody().as(ABean.class)).contains("Content-Type not specified in response header.  Cannot find appropriate parser.");

		b = x2.post("/echoBody",bean).accept("text/xml").contentType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x2.post("/echoBody",bean).mediaType("text/xml").run().cacheBody().assertBody().is("<object><f>1</f></object>").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x2.post("/echoBody",bean).accept("text/json").contentType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);

		b = x2.post("/echoBody",bean).mediaType("text/json").run().cacheBody().assertBody().is("{\"f\":1}").getBody().as(ABean.class);
		assertObject(b).isSameJsonAs(bean);
	}

	@Rest(partSerializer=A12a.class,partParser=A12b.class)
	public static class A12 extends BasicRestObject {
		@RestOp(path="/")
		public Ok get(@Header(name="Foo",multi=true) ABean[] foo,org.apache.juneau.rest.RestRequest req,org.apache.juneau.rest.RestResponse res) throws Exception {
			assertEquals(2,foo.length);
			assertObject(req.getHeaders().getAll("Foo",String[].class)).asJson().is("['x{f:1}','x{f:1}']");
			assertEquals("{f:1}",foo[0].toString());
			assertEquals("{f:1}",foo[1].toString());
			res.header("Foo",bean);
			return Ok.OK;
		}
	}

	public static class A12a extends SimplePartSerializer {
		@Override
		public SimplePartSerializerSession createPartSession(SerializerSessionArgs args) {
			return new SimplePartSerializerSession() {
				@Override
				public String serialize(HttpPartType type, HttpPartSchema schema, Object value) {
					return "x" + SimpleJson.DEFAULT.toString(value);
				}
			};
		}
	}

	public static class A12b extends SimplePartParser {
		@Override
		public SimplePartParserSession createPartSession(ParserSessionArgs args) {
			return new SimplePartParserSession() {
				@Override
				public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
					if (toType.isInstanceOf(ABean.class))
						return SimpleJson.DEFAULT.read(in.substring(1),toType);
					return super.parse(null,schema,in,toType);
				}
			};
		}
	}

	@Test
	public void a12_partSerializer_partParser() throws Exception {
		RestClient x = client(A12.class).header("Foo",bean).partSerializer(A12a.class).partParser(A12b.class).build();
		ABean b = x.get("/").header("Foo",bean).run().assertStringHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());
		b = x.get().header("Foo",bean).run().assertStringHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());

		x = client(A12.class).header("Foo",bean).partSerializer(new A12a()).partParser(new A12b()).build();
		b = x.get("/").header("Foo",bean).run().assertStringHeader("Foo").is("x{f:1}").getHeader("Foo").as(ABean.class);
		assertEquals("{f:1}",b.toString());
	}


	@Test
	public void a13_toString() throws Exception {
		String s = client().rootUri("foo").build().toString();
		assertTrue(s.contains("rootUri: 'foo'"));
	}

	@Test
	public void a14_request_target() throws Exception {
		client().build().get("/bean").target(new HttpHost("localhost")).run().assertBody().is("{f:1}");
	}

	@Test
	public void a15_request_context() throws Exception {
		client().build().get("/bean").context(new BasicHttpContext()).run().assertBody().is("{f:1}");
	}

	@Test
	public void a16_request_uriParts() throws Exception {
		java.net.URI uri = client().build().get().scheme("http").host("localhost").port(8080).userInfo("foo:bar").uri("/bean").fragment("baz").query("foo","bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().scheme("http").host("localhost").port(8080).userInfo("foo","bar").uri("/bean").fragment("baz").query("foo","bar").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri("http://localhost").uri("http://foo:bar@localhost:8080/bean?foo=bar#baz").run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());

		uri = client().build().get().uri(new java.net.URI(null,null,null,null)).uri(new java.net.URI("http://foo:bar@localhost:8080/bean?foo=bar#baz")).run().assertBody().is("{f:1}").getRequest().getURI();
		assertEquals("http://foo:bar@localhost:8080/bean?foo=bar#baz",uri.toString());
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
}
