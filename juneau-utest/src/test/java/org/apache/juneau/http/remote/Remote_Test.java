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
package org.apache.juneau.http.remote;

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.AssertionPredicates.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;

import java.util.concurrent.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.marshaller.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// @Remote(path), relative paths
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public String x1() {
			return "foo";
		}
		@RestGet(path="/A/x2")
		public String x2() {
			return "foo";
		}
		@RestGet(path="/A/A/x3")
		public String x3() {
			return "foo";
		}
	}

	@Remote
	public static interface A1 {
		String x1();
		@RemoteOp(path="x1") String x1a();
		@RemoteOp(path="/x1/") String x1b();
	}

	@Test
	public void a01_noPath() throws Exception {
		A1 x = plainRemote(A.class,A1.class);
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote(path="A")
	public static interface A2 {
		String x2();
		@RemoteOp(path="x2") String x2a();
		@RemoteOp(path="/x2/") String x2b();
	}

	@Test
	public void a02_normalPath() throws Exception {
		A2 x = plainRemote(A.class,A2.class);
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote(path="/A/")
	public static interface A3 {
		String x2();
		@RemoteOp(path="x2") String x2a();
		@RemoteOp(path="/x2/") String x2b();
	}

	@Test
	public void a03_normalPathWithSlashes() throws Exception {
		A3 x = plainRemote(A.class,A3.class);
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote
	public static interface A4 {
		String x2();
		@RemoteOp(path="x2") String x2a();
		@RemoteOp(path="/x2/") String x2b();
	}

	@Test
	public void a04_pathOnClient() throws Exception {
		A4 x = plainRemote(A.class,A4.class,"http://localhost/A");
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote(path="A/A")
	public static interface A5 {
		String x3();
		@RemoteOp(path="x3") String x3a();
		@RemoteOp(path="/x3/") String x3b();
	}

	@Test
	public void a05_normalPath() throws Exception {
		A5 x = plainRemote(A.class,A5.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="/A/A/")
	public static interface A6 {
		String x3();
		@RemoteOp(path="x3") String x3a();
		@RemoteOp(path="/x3/") String x3b();
	}

	@Test
	public void a06_normalPathWithSlashes() throws Exception {
		A6 x = plainRemote(A.class,A6.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="A")
	public static interface A7 {
		String x3();
		@RemoteOp(path="x3") String x3a();
		@RemoteOp(path="/x3/") String x3b();
	}

	@Test
	public void a07_partialPath() throws Exception {
		A7 x = plainRemote(A.class,A7.class,"http://localhost/A");
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="/A/")
	public static interface A8 {
		String x3();
		@RemoteOp(path="x3") String x3a();
		@RemoteOp(path="/x3/") String x3b();
	}

	@Test
	public void a08_partialPathExtraSlashes() throws Exception {
		A8 x = plainRemote(A.class,A8.class,"http://localhost/A/");
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @RemoteResource(path), absolute paths
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="B/x1")
		public String x1() {
			return "foo";
		}
	}

	@Remote
	public static interface B1 {
		String x1();
		@RemoteOp(path="x1") String x1a();
		@RemoteOp(path="/x1/") String x1b();
	}

	@Test
	public void b01_noPath() throws Exception {
		B1 x = plainRemote(B.class,B1.class,"http://localhost/B");
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote(path="http://localhost/B")
	public static interface B2 {
		String x1();
		@RemoteOp(path="x1") String x1a();
		@RemoteOp(path="/x1/") String x1b();
	}

	@Test
	public void b02_absolutePathOnClass() throws Exception {
		B2 x = plainRemote(B.class,B2.class,"http://localhost/B");
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote
	public static interface B3 {
		String x1();
		@RemoteOp(path="http://localhost/B/x1") String x1a();
		@RemoteOp(path="http://localhost/B/x1/") String x1b();
	}

	@Test
	public void b03_absolutePathsOnMethods() throws Exception {
		B3 x = plainRemote(B.class,B3.class,"http://localhost/B");
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(path="/C1")
	public static class C implements BasicJson5Config {
		@RestOp
		public String x1() {
			return "foo";
		}
		@RestOp("GET")
		public String x2() {
			return "bar";
		}
		@RestOp("GET /x3")
		public String x3x() {
			return "baz";
		}
		@RestGet
		public String x4() {
			return "qux";
		}
		@RestGet("/x5")
		public String x5x() {
			return "quux";
		}
	}

	@Remote(path="/")
	public static interface C1 {
		String x1();
		@RemoteOp("GET") String x2();
		@RemoteOp("GET /x3") String x3x();
		@RemoteOp("GET /x4") String x4();
		@RemoteOp("GET /x5") String x5x();
	}

	@Test
	public void c01_overriddenRootUrl() throws Exception {
		C1 x = client(C.class).build().getRemote(C1.class,"http://localhost/C1");
		assertEquals("foo",x.x1());
		assertEquals("bar",x.x2());
		assertEquals("baz",x.x3x());
		assertEquals("qux",x.x4());
		assertEquals("quux",x.x5x());
	}

	@Rest(path="/C3")
	public static class C3a implements BasicJson5Config {
		@RestOp
		public String x1() {
			return "bar";
		}
		@RestOp
		public String getX2() {
			return "baz";
		}
		@RestGet
		public String x3() {
			return "baz";
		}
		@RestGet
		public String getX4() {
			return "qux";
		}
	}

	@Remote(path="/")
	public static interface C3b {
		String x1();
		String getX2();
		String x3();
		String getX4();
	}

	@Test
	public void c03_methodNotAnnotated() throws Exception {
		C3b x = remote(C3a.class,C3b.class);
		assertEquals("bar",x.x1());
		assertEquals("baz",x.getX2());
		assertEquals("baz",x.x3());
		assertEquals("qux",x.getX4());
	}

	@Rest(path="/C4")
	public static class C4a implements BasicJson5Config {
		@RestOp
		public String x1() throws C4c {
			throw new C4c("foo");
		}
		@RestOp
		public String x2() throws C4c {
			throw new RuntimeException("foo");
		}
		@RestOp
		public String x3() {
			throw new AssertionError("foo");
		}
		@RestGet
		public String x4() throws C4c {
			throw new C4c("foo");
		}
		@RestGet
		public String x5() throws C4c {
			throw new RuntimeException("foo");
		}
		@RestGet
		public String x6() {
			throw new AssertionError("foo");
		}
	}

	@Remote
	public static interface C4b {
		String x1() throws C4c;
		@RemoteOp(path="x1") Future<String> x1a() throws C4c;
		@RemoteOp(path="x1") CompletableFuture<String> x1b() throws C4c;
		String x2() throws C4c;
		Future<String> x3() throws AssertionError;
		String x4() throws C4c;
		@RemoteOp(path="x1") Future<String> x4a() throws C4c;
		@RemoteOp(path="x1") CompletableFuture<String> x4b() throws C4c;
		String x5() throws C4c;
		Future<String> x6() throws AssertionError;
	}

	@SuppressWarnings("serial")
	public static class C4c extends Exception {
		public C4c(String msg) {
			super(msg);
		}
	}

	@Test
	public void c04_rethrownExceptions() throws Exception {
		C4b x = remote(C4a.class,C4b.class);
		assertThrown(()->x.x1()).asMessage().is("foo");
		assertThrown(()->x.x1a().get()).asMessages().isContains("foo");
		assertThrown(()->x.x1b().get()).asMessages().isContains("foo");
		assertThrown(()->x.x2()).asMessage().is("foo");
		assertThrown(()->x.x3().get()).asMessages().isContains("foo");
		assertThrown(()->x.x4()).asMessage().is("foo");
		assertThrown(()->x.x4a().get()).asMessages().isContains("foo");
		assertThrown(()->x.x4b().get()).asMessages().isContains("foo");
		assertThrown(()->x.x5()).asMessage().is("foo");
		assertThrown(()->x.x6().get()).asMessages().isContains("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Status return type
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D1 implements BasicJson5Config {
		@RestGet
		public void r202(org.apache.juneau.rest.RestResponse res) {
			res.setStatus(202);
		}
		@RestGet
		public void r400(org.apache.juneau.rest.RestResponse res) {
			res.setStatus(400);
		}
	}

	@Remote
	public static interface D1a {
		@RemoteOp(path="/r202",returns=RemoteReturn.STATUS) int x1() throws AssertionError;
		@RemoteOp(path="/r202",returns=RemoteReturn.STATUS) Integer x2() throws AssertionError;
		@RemoteOp(path="/r202",returns=RemoteReturn.STATUS) boolean x3() throws AssertionError;
		@RemoteOp(path="/r202",returns=RemoteReturn.STATUS) Boolean x4() throws AssertionError;
		@RemoteOp(path="/r202",returns=RemoteReturn.STATUS) String x5() throws AssertionError;
		@RemoteOp(path="/r400",returns=RemoteReturn.STATUS) public int x6() throws AssertionError;
		@RemoteOp(path="/r400",returns=RemoteReturn.STATUS) public Integer x7() throws AssertionError;
		@RemoteOp(path="/r400",returns=RemoteReturn.STATUS) public boolean x8() throws AssertionError;
		@RemoteOp(path="/r400",returns=RemoteReturn.STATUS) public Boolean x9() throws AssertionError;
		@RemoteOp(path="/r400",returns=RemoteReturn.STATUS) public String x10() throws AssertionError;
	}

	@Test
	public void d01_statusReturnType() throws Exception {
		D1a x = client(D1.class).ignoreErrors().build().getRemote(D1a.class);
		assertEquals(202,x.x1());
		assertEquals(202,x.x2().intValue());
		assertEquals(true,x.x3());
		assertEquals(true,x.x4());
		assertEquals(400,x.x6());
		assertEquals(400,x.x7().intValue());
		assertEquals(false,x.x8());
		assertEquals(false,x.x9());
		assertThrown(()->x.x5()).asMessages().isAny(contains("Only integer and booleans types are valid."));
		assertThrown(()->x.x10()).asMessages().isAny(contains("Only integer and booleans types are valid."));
	}

	@Rest
	public static class D2 implements BasicJson5Config {
		@RestGet
		public Integer x1() {
			return null;
		}
		@RestGet
		public Integer x2() {
			return 1;
		}
	}

	@Remote
	public static interface D2a {
		int x1() throws AssertionError;
		int x2() throws AssertionError;
		@RemoteOp(path="x1") Integer x1a() throws AssertionError;
	}

	@Test
	public void d02_primitiveReturns() throws Exception {
		D2a x = client(D2.class).ignoreErrors().build().getRemote(D2a.class);
		assertEquals(0,x.x1());
		assertEquals(1,x.x2());
		assertNull(x.x1a());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// RRPC interfaces
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E implements BasicJson5Config {
		@RestOp(method=HttpMethod.RRPC)
		public E1 proxy() {
			return new E1() {
				@Override
				public String echo(String body) {
					return body;
				}
			};
		}
	}

	public interface E1 {
		String echo(String body);
	}

	@Test
	public void e01_rrpcBasic() throws Exception {
		E1 x = client(E.class).rootUrl("http://localhost/proxy").build().getRrpcInterface(E1.class);

		assertEquals("foo",x.echo("foo"));
	}

	@Remote(path="/proxy")
	public interface E3 {
		String echo(String body);
	}

	@Test
	public void e03_rrpc_noRestUrl() throws Exception {
		E3 x = client(E.class).rootUrl("http://localhost").build().getRrpcInterface(E3.class);
		assertEquals("foo",x.echo("foo"));
	}

	@Remote(path="http://localhost/proxy")
	public interface E4 {
		String echo(String body);
	}

	@Test
	public void e04_rrpc_fullPathOnRemotePath() throws Exception {
		E4 x = client(E.class).rootUrl("").build().getRrpcInterface(E4.class);
		assertEquals("foo",x.echo("foo"));
	}

	@Rest
	public static class E5 implements BasicJson5Config {
		@RestOp(method=HttpMethod.RRPC)
		public E5b proxy() {
			return new E5b() {
				@Override
				public String echo(String body) throws E5a {
					throw new E5a("foobar");
				}
			};
		}
	}

	@SuppressWarnings("serial")
	public static class E5a extends Exception {
		public E5a(String msg) {
			super(msg);
		}
	}

	public interface E5b {
		String echo(String body) throws E5a;
	}

	@Test
	public void e05_rrpc_rethrownCheckedException() throws Exception {
		RestClient x = client(E5.class).build();
		assertThrown(()->x.getRrpcInterface(E5b.class,"/proxy").echo("foo")).asMessage().is("foobar");
	}

	@Rest
	public static class E6 implements BasicJson5Config {
		@RestOp(method=HttpMethod.RRPC)
		public E5b proxy() {
			return new E5b() {
				@Override
				public String echo(String body) throws E5a {
					throw new AssertionError("foobar");
				}
			};
		}
	}

	@Test
	public void e06_rrpc_rethrownUncheckedException() throws Exception {
		RestClient x = client(E6.class).build();
		assertThrown(()->x.getRrpcInterface(E5b.class,"/proxy").echo("foo")).asMessages().isAny(contains("foobar"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Remote headers
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F extends BasicRestObject {
		@RestGet
		public String[] headers(org.apache.juneau.rest.RestRequest req) {
			return req.getHeaders().getAll(req.getHeaderParam("Check").orElse(null)).stream().map(x -> x.getValue()).toArray(String[]::new);
		}
	}

	@Remote(headers="Foo:bar",headerList=F1b.class,version="1.2.3")
	public static interface F1a {
		String[] getHeaders();
	}

	@SuppressWarnings("serial")
	public static class F1b extends HeaderList {
		public F1b() {
			super(
				create()
				.append(basicHeader("Foo","baz"))
				.append(HeaderList.create().append(basicHeader("Foo",()->"qux")).getAll())
			);
		}
	}

	@Test
	public void f01_headers() throws Exception {
		F1a x = client(F.class).header("Check","Foo").build().getRemote(F1a.class);
		assertEquals("['bar','baz','qux']",Json5.of(x.getHeaders()));
		x = client(F.class).header("Check","Client-Version").build().getRemote(F1a.class);
		assertEquals("['1.2.3']",Json5.of(x.getHeaders()));
	}

	@Remote(headerList=F2b.class)
	public static interface F2a {
		String[] getHeaders();
	}

	@SuppressWarnings("serial")
	public static class F2b extends HeaderList {
		public F2b() {
			super();
			throw new NullPointerException("foo");
		}
	}

	@Test
	public void f02_headers_badSupplier() throws Exception {
		assertThrown(()->client(F.class).build().getRemote(F2a.class)).asMessages().isContains("foo");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G extends BasicRestObject {}

	@Remote
	public static interface G1 {
		@RemoteOp(method="FOO")
		String[] getHeaders();
	}

	@Test
	public void g01_badMethodName() throws Exception {
		assertThrown(()->client(G.class).header("Check","Foo").build().getRemote(G1.class)).isType(RemoteMetadataException.class).asMessage().isContains("Invalid value");
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Method detection
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H extends BasicRestObject {
		@RestOp(method="*", path="/*")
		public String echoMethod(@Method String method, @Path("/*") String path) {
			return method + " " + path;
		}
	}

	@Remote
	public static interface H1 {
		@RemoteOp(method="get") String a1();
		@RemoteOp(method="put") String a2();
		@RemoteOp(method="post") String a3();
		@RemoteOp(method="patch") String a4();
		@RemoteOp(method="delete") String a5();
		@RemoteOp(method="options") String a6();
		@RemoteGet String a11();
		@RemotePut String a12();
		@RemotePost String a13();
		@RemotePatch String a14();
		@RemoteDelete String a15();
		@RemoteOp String getA21();
		@RemoteOp String putA22();
		@RemoteOp String postA23();
		@RemoteOp String patchA24();
		@RemoteOp String deleteA25();
		@RemoteOp String optionsA26();
		@RemoteGet("/a31x") String a31();
		@RemotePut("/a32x") String a32();
		@RemotePost("/a33x") String a33();
		@RemotePatch("/a34x") String a34();
		@RemoteDelete("/a35x") String a35();
		@RemoteOp("GET /a41x") String a41();
		@RemoteOp("PUT /a42x") String a42();
		@RemoteOp("POST /a43x") String a43();
		@RemoteOp("PATCH /a44x") String a44();
		@RemoteOp("DELETE /a45x") String a45();
		@RemoteOp("OPTIONS /a46x") String a46();
		@RemoteGet("a51x") String a51();
		@RemotePut("a52x") String a52();
		@RemotePost("a53x") String a53();
		@RemotePatch("a54x") String a54();
		@RemoteDelete("a55x") String a55();
		@RemoteOp("GET a61x") String a61();
		@RemoteOp("PUT a62x") String a62();
		@RemoteOp("POST a63x") String a63();
		@RemoteOp("PATCH a64x") String a64();
		@RemoteOp("DELETE a65x") String a65();
		@RemoteOp("OPTIONS a66x") String a66();
	}


	@Test
	public void h01_methodDetection() throws Exception {

		H1 x = client(H.class).build().getRemote(H1.class);
		assertEquals("GET a1", x.a1());
		assertEquals("PUT a2", x.a2());
		assertEquals("POST a3", x.a3());
		assertEquals("PATCH a4", x.a4());
		assertEquals("DELETE a5", x.a5());
		assertEquals("OPTIONS a6", x.a6());
		assertEquals("GET a11", x.a11());
		assertEquals("PUT a12", x.a12());
		assertEquals("POST a13", x.a13());
		assertEquals("PATCH a14", x.a14());
		assertEquals("DELETE a15", x.a15());
		assertEquals("GET a21", x.getA21());
		assertEquals("PUT a22", x.putA22());
		assertEquals("POST a23", x.postA23());
		assertEquals("PATCH a24", x.patchA24());
		assertEquals("DELETE a25", x.deleteA25());
		assertEquals("OPTIONS a26", x.optionsA26());
		assertEquals("GET a31x", x.a31());
		assertEquals("PUT a32x", x.a32());
		assertEquals("POST a33x", x.a33());
		assertEquals("PATCH a34x", x.a34());
		assertEquals("DELETE a35x", x.a35());
		assertEquals("GET a41x", x.a41());
		assertEquals("PUT a42x", x.a42());
		assertEquals("POST a43x", x.a43());
		assertEquals("PATCH a44x", x.a44());
		assertEquals("DELETE a45x", x.a45());
		assertEquals("OPTIONS a46x", x.a46());
		assertEquals("GET a51x", x.a51());
		assertEquals("PUT a52x", x.a52());
		assertEquals("POST a53x", x.a53());
		assertEquals("PATCH a54x", x.a54());
		assertEquals("DELETE a55x", x.a55());
		assertEquals("GET a61x", x.a61());
		assertEquals("PUT a62x", x.a62());
		assertEquals("POST a63x", x.a63());
		assertEquals("PATCH a64x", x.a64());
		assertEquals("DELETE a65x", x.a65());
		assertEquals("OPTIONS a66x", x.a66());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client(Class<?> c) {
		return MockRestClient.create(c).noTrace().json5();
	}

	private static <T> T remote(Class<?> c, Class<T> r) {
		return MockRestClient.create(c).noTrace().json5().build().getRemote(r);
	}

	private static <T> T plainRemote(Class<?> c, Class<T> r) {
		return MockRestClient.create(c).build().getRemote(r);
	}

	private static <T> T plainRemote(Class<?> c, Class<T> r, String rootUrl) {
		return MockRestClient.create(c).rootUrl(rootUrl).build().getRemote(r);
	}
}
