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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.assertions.Assertions.*;
import java.util.concurrent.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.remote.RemoteMethod;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_Test {

	//=================================================================================================================
	// @Remote(path), relative paths
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod
		public String x1() {
			return "foo";
		}
		@RestMethod(path="/A/x2")
		public String x2() {
			return "foo";
		}
		@RestMethod(path="/A/A/x3")
		public String x3() {
			return "foo";
		}
	}

	@Remote
	public static interface A1 {
		String x1();
		@RemoteMethod(path="x1") String x1a();
		@RemoteMethod(path="/x1/") String x1b();
	}

	@Test
	public void a01_noPath() throws Exception {
		A1 x = MockRestClient.build(A.class).getRemote(A1.class);
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote(path="A")
	public static interface A2 {
		String x2();
		@RemoteMethod(path="x2") String x2a();
		@RemoteMethod(path="/x2/") String x2b();
	}

	@Test
	public void a02_normalPath() throws Exception {
		A2 x = MockRestClient.build(A.class).getRemote(A2.class);
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote(path="/A/")
	public static interface A3 {
		String x2();
		@RemoteMethod(path="x2") String x2a();
		@RemoteMethod(path="/x2/") String x2b();
	}

	@Test
	public void a03_normalPathWithSlashes() throws Exception {
		A3 x = MockRestClient.build(A.class).getRemote(A3.class);
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote
	public static interface A4 {
		String x2();
		@RemoteMethod(path="x2") String x2a();
		@RemoteMethod(path="/x2/") String x2b();
	}

	@Test
	public void a04_pathOnClient() throws Exception {
		A4 x = MockRestClient.create(A.class).rootUrl("http://localhost/A").build().getRemote(A4.class);
		assertEquals("foo",x.x2());
		assertEquals("foo",x.x2a());
		assertEquals("foo",x.x2b());
	}

	@Remote(path="A/A")
	public static interface A5 {
		String x3();
		@RemoteMethod(path="x3") String x3a();
		@RemoteMethod(path="/x3/") String x3b();
	}

	@Test
	public void a05_normalPath() throws Exception {
		A5 x = MockRestClient.build(A.class).getRemote(A5.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="/A/A/")
	public static interface A6 {
		String x3();
		@RemoteMethod(path="x3") String x3a();
		@RemoteMethod(path="/x3/") String x3b();
	}

	@Test
	public void a06_normalPathWithSlashes() throws Exception {
		A6 x = MockRestClient.build(A.class).getRemote(A6.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="A")
	public static interface A7 {
		String x3();
		@RemoteMethod(path="x3") String x3a();
		@RemoteMethod(path="/x3/") String x3b();
	}

	@Test
	public void a07_partialPath() throws Exception {
		A7 x =  MockRestClient.create(A.class).rootUrl("http://localhost/A").build().getRemote(A7.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	@Remote(path="/A/")
	public static interface A8 {
		String x3();
		@RemoteMethod(path="x3") String x3a();
		@RemoteMethod(path="/x3/") String x3b();
	}

	@Test
	public void a08_partialPathExtraSlashes() throws Exception {
		A8 x = MockRestClient.create(A.class).rootUrl("http://localhost/A/").build().getRemote(A8.class);
		assertEquals("foo",x.x3());
		assertEquals("foo",x.x3a());
		assertEquals("foo",x.x3b());
	}

	//=================================================================================================================
	// @RemoteResource(path), absolute paths
	//=================================================================================================================

	@Rest
	public static class B {
		@RestMethod(path="B/x1")
		public String x1() {
			return "foo";
		}
	}

	@Remote
	public static interface B1 {
		String x1();
		@RemoteMethod(path="x1") String x1a();
		@RemoteMethod(path="/x1/") String x1b();
	}

	@Test
	public void b01_noPath() throws Exception {
		B1 x = MockRestClient.create(B.class).rootUrl("http://localhost/B").build().getRemote(B1.class);
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote(path="http://localhost/B")
	public static interface B2 {
		String x1();
		@RemoteMethod(path="x1") String x1a();
		@RemoteMethod(path="/x1/") String x1b();
	}

	@Test
	public void b02_absolutePathOnClass() throws Exception {
		B2 x = MockRestClient.create(B.class).rootUrl("http://localhost/B").build().getRemote(B2.class);
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	@Remote
	public static interface B3 {
		String x1();
		@RemoteMethod(path="http://localhost/B/x1") String x1a();
		@RemoteMethod(path="http://localhost/B/x1/") String x1b();
	}

	@Test
	public void b03_absolutePathsOnMethods() throws Exception {
		B3 x = MockRestClient.create(B.class).rootUrl("http://localhost/B").build().getRemote(B3.class);
		assertEquals("foo",x.x1());
		assertEquals("foo",x.x1a());
		assertEquals("foo",x.x1b());
	}

	//=================================================================================================================
	// Other tests
	//=================================================================================================================

	@Rest(path="/C1")
	public static class C implements BasicSimpleJsonRest {
		@RestMethod
		public String x1() {
			return "foo";
		}
	}

	@Remote(path="/")
	public static interface C1 {
		String x1();
	}

	@Test
	public void c01_overriddenRootUrl() throws Exception {
		C1 x = MockRestClient.create(C.class).json().build().getRemote(C1.class,"http://localhost/C1");
		assertEquals("foo",x.x1());
	}

	@Test
	public void c02_rootUriNotSpecified() throws Exception {
		C1 x = MockRestClient.create(C.class).json().rootUrl("").build().getRemote(C1.class);
		assertThrown(()->{x.x1();}).contains("Root URI has not been specified.");
	}


	@Rest(path="/C3")
	public static class C3a implements BasicSimpleJsonRest {
		@RestMethod
		public String x1() {
			return "bar";
		}
		@RestMethod
		public String getX2() {
			return "baz";
		}
	}

	@Remote(path="/")
	public static interface C3b {
		String x1();
		String getX2();
	}

	@Test
	public void c03_methodNotAnnotated() throws Exception {
		C3b x = MockRestClient.create(C3a.class).json().build().getRemote(C3b.class);
		assertEquals("bar",x.x1());
		assertEquals("baz",x.getX2());
	}

	@Rest(path="/C4")
	public static class C4a implements BasicSimpleJsonRest {
		@RestMethod
		public String x1() throws C4c {
			throw new C4c("foo");
		}
		@RestMethod
		public String x2() throws C4c {
			throw new RuntimeException("foo");
		}
		@RestMethod
		public String x3() {
			throw new AssertionError("foo");
		}
	}

	@Remote
	public static interface C4b {
		String x1() throws C4c;
		@RemoteMethod(path="x1") Future<String> x1a() throws C4c;
		@RemoteMethod(path="x1") CompletableFuture<String> x1b() throws C4c;
		String x2() throws C4c;
		Future<String> x3() throws AssertionError;
	}

	@SuppressWarnings("serial")
	public static class C4c extends Exception {
		public C4c(String msg) {
			super(msg);
		}
	}

	@Test
	public void c04_rethrownExceptions() throws Exception {
		C4b x = MockRestClient.create(C4a.class).json().build().getRemote(C4b.class);
		assertThrown(()->{x.x1();}).is("foo");
		assertThrown(()->{x.x1a().get();}).contains("foo");
		assertThrown(()->{x.x1b().get();}).contains("foo");
		assertThrown(()->{x.x2();}).contains("foo");
		assertThrown(()->{x.x3().get();}).contains("foo");
	}

	//=================================================================================================================
	// Status return type
	//=================================================================================================================

	@Rest
	public static class D1 implements BasicSimpleJsonRest {
		@RestMethod
		public void getR202(org.apache.juneau.rest.RestResponse res) {
			res.setStatus(202);
		}
		@RestMethod
		public void getR400(org.apache.juneau.rest.RestResponse res) {
			res.setStatus(400);
		}
	}

	@Remote
	public static interface D1a {
		@RemoteMethod(path="/r202",returns=RemoteReturn.STATUS) int x1() throws AssertionError;
		@RemoteMethod(path="/r202",returns=RemoteReturn.STATUS) Integer x2() throws AssertionError;
		@RemoteMethod(path="/r202",returns=RemoteReturn.STATUS) boolean x3() throws AssertionError;
		@RemoteMethod(path="/r202",returns=RemoteReturn.STATUS) Boolean x4() throws AssertionError;
		@RemoteMethod(path="/r202",returns=RemoteReturn.STATUS) String x5() throws AssertionError;
		@RemoteMethod(path="/r400",returns=RemoteReturn.STATUS) public int x6() throws AssertionError;
		@RemoteMethod(path="/r400",returns=RemoteReturn.STATUS) public Integer x7() throws AssertionError;
		@RemoteMethod(path="/r400",returns=RemoteReturn.STATUS) public boolean x8() throws AssertionError;
		@RemoteMethod(path="/r400",returns=RemoteReturn.STATUS) public Boolean x9() throws AssertionError;
		@RemoteMethod(path="/r400",returns=RemoteReturn.STATUS) public String x10() throws AssertionError;
	}

	@Test
	public void d01_statusReturnType() throws Exception {
		D1a x = MockRestClient.create(D1.class).json().ignoreErrors().build().getRemote(D1a.class);
		assertEquals(202,x.x1());
		assertEquals(202,x.x2().intValue());
		assertEquals(true,x.x3());
		assertEquals(true,x.x4());
		assertEquals(400,x.x6());
		assertEquals(400,x.x7().intValue());
		assertEquals(false,x.x8());
		assertEquals(false,x.x9());
		assertThrown(()->{x.x5();}).contains("Only integer and booleans types are valid.");
		assertThrown(()->{x.x10();}).contains("Only integer and booleans types are valid.");
	}

	@Rest
	public static class D2 implements BasicSimpleJsonRest {
		@RestMethod
		public Integer getX1() {
			return null;
		}
		@RestMethod
		public Integer getX2() {
			return 1;
		}
	}

	@Remote
	public static interface D2a {
		int x1() throws AssertionError;
		int x2() throws AssertionError;
		@RemoteMethod(path="x1") Integer x1a() throws AssertionError;
	}

	@Test
	public void d02_primitiveReturns() throws Exception {
		D2a x = MockRestClient.create(D2.class).json().ignoreErrors().build().getRemote(D2a.class);
		assertEquals(0,x.x1());
		assertEquals(1,x.x2());
		assertNull(x.x1a());
	}

	//=================================================================================================================
	// RRPC interfaces
	//=================================================================================================================

	@Rest
	public static class E implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E1 getProxy() {
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
		E1 x = MockRestClient.create(E.class).rootUrl("http://localhost/proxy").json().build().getRrpcInterface(E1.class);

		assertEquals("foo",x.echo("foo"));
	}

	@Test
	public void e02_rrpc_noRootPath() throws Exception {
		RestClient x = MockRestClient.create(E.class).rootUrl("").json().build();
		assertThrown(()->{x.getRrpcInterface(E1.class);}).contains("Root URI has not been specified.");
	}

	@Remote(path="/proxy")
	public interface E3 {
		String echo(String body);
	}

	@Test
	public void e03_rrpc_noRestUrl() throws Exception {
		E3 x = MockRestClient.create(E.class).rootUrl("http://localhost").json().build().getRrpcInterface(E3.class);
		assertEquals("foo",x.echo("foo"));
	}

	@Remote(path="http://localhost/proxy")
	public interface E4 {
		String echo(String body);
	}

	@Test
	public void e04_rrpc_fullPathOnRemotePath() throws Exception {
		E4 x = MockRestClient.create(E.class).rootUrl("").json().build().getRrpcInterface(E4.class);
		assertEquals("foo",x.echo("foo"));
	}

	@Rest
	public static class E5 implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E5b getProxy() {
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
		RestClient x = MockRestClient.create(E5.class).json().build();
		assertThrown(()->{x.getRrpcInterface(E5b.class,"/proxy").echo("foo");}).is("foobar");
	}

	@Rest
	public static class E6 implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E5b getProxy() {
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
		RestClient x = MockRestClient.create(E6.class).json().build();
		assertThrown(()->{x.getRrpcInterface(E5b.class,"/proxy").echo("foo");}).contains("foobar");
	}
}
