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

import java.util.concurrent.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.http.remote.RemoteMethod;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RemotesTest {

	//=================================================================================================================
	// @RemoteResource(path), relative paths
	//=================================================================================================================

	@Rest
	public static class A {

		@RestMethod
		public String a01() {
			return "foo";
		}

		@RestMethod(path="/A/a02")
		public String a02() {
			return "foo";
		}

		@RestMethod(path="/A/A/a03")
		public String a03() {
			return "foo";
		}
	}

	@Remote
	public static interface A01a {
		@RemoteMethod
		public String a01();
		@RemoteMethod(path="a01")
		public String a01a();
		@RemoteMethod(path="/a01/")
		public String a01b();
	}

	@Test
	public void a01_noPath() throws Exception {
		A01a t = MockRestClient.build(A.class).getRemote(A01a.class);
		assertEquals("foo", t.a01());
		assertEquals("foo", t.a01a());
		assertEquals("foo", t.a01b());
	}

	@Remote(path="A")
	public static interface A02a {
		@RemoteMethod
		public String a02();
		@RemoteMethod(path="a02")
		public String a02a();
		@RemoteMethod(path="/a02/")
		public String a02b();
	}

	@Test
	public void a02a_normalPath() throws Exception {
		A02a t = MockRestClient.build(A.class).getRemote(A02a.class);
		assertEquals("foo", t.a02());
		assertEquals("foo", t.a02a());
		assertEquals("foo", t.a02b());
	}

	@Remote(path="/A/")
	public static interface A02b {
		@RemoteMethod
		public String a02();
		@RemoteMethod(path="a02")
		public String a02a();
		@RemoteMethod(path="/a02/")
		public String a02b();
	}

	@Test
	public void a02b_normalPathWithSlashes() throws Exception {
		A02b t = MockRestClient.build(A.class).getRemote(A02b.class);
		assertEquals("foo", t.a02());
		assertEquals("foo", t.a02a());
		assertEquals("foo", t.a02b());
	}

	@Remote
	public static interface A02c {
		@RemoteMethod
		public String a02();
		@RemoteMethod(path="a02")
		public String a02a();
		@RemoteMethod(path="/a02/")
		public String a02b();
	}

	@Test
	public void a02c_pathOnClient() throws Exception {
		try (RestClient rc = MockRestClient.create(A.class).rootUrl("http://localhost/A").build()) {
			A02c t = rc.getRemote(A02c.class);
			assertEquals("foo", t.a02());
			assertEquals("foo", t.a02a());
			assertEquals("foo", t.a02b());
		}
	}

	@Remote(path="A/A")
	public static interface A03a {
		@RemoteMethod
		public String a03();
		@RemoteMethod(path="a03")
		public String a03a();
		@RemoteMethod(path="/a03/")
		public String a03b();
	}

	@Test
	public void a03a_normalPath() throws Exception {
		A03a t = MockRestClient.build(A.class).getRemote(A03a.class);
		assertEquals("foo", t.a03());
		assertEquals("foo", t.a03a());
		assertEquals("foo", t.a03b());
	}

	@Remote(path="/A/A/")
	public static interface A03b {
		@RemoteMethod
		public String a03();
		@RemoteMethod(path="a03")
		public String a03a();
		@RemoteMethod(path="/a03/")
		public String a03b();
	}

	@Test
	public void a03b_normalPathWithSlashes() throws Exception {
		A03b t = MockRestClient.build(A.class).getRemote(A03b.class);
		assertEquals("foo", t.a03());
		assertEquals("foo", t.a03a());
		assertEquals("foo", t.a03b());
	}

	@Remote(path="A")
	public static interface A03c {
		@RemoteMethod
		public String a03();
		@RemoteMethod(path="a03")
		public String a03a();
		@RemoteMethod(path="/a03/")
		public String a03b();
	}

	@Test
	public void a03c_partialPath() throws Exception {
		try (RestClient rc = MockRestClient.create(A.class).rootUrl("http://localhost/A").build()) {
			A03c t = rc.getRemote(A03c.class);
			assertEquals("foo", t.a03());
			assertEquals("foo", t.a03a());
			assertEquals("foo", t.a03b());
		}
	}

	@Remote(path="/A/")
	public static interface A03d {
		@RemoteMethod
		public String a03();
		@RemoteMethod(path="a03")
		public String a03a();
		@RemoteMethod(path="/a03/")
		public String a03b();
	}

	@Test
	public void a03d_partialPathExtraSlashes() throws Exception {
		try (RestClient rc = MockRestClient.create(A.class).rootUrl("http://localhost/A/").build()) {
			A03d t = rc.getRemote(A03d.class);
			assertEquals("foo", t.a03());
			assertEquals("foo", t.a03a());
			assertEquals("foo", t.a03b());
		}
	}

	//=================================================================================================================
	// @RemoteResource(path), absolute paths
	//=================================================================================================================

	@Rest
	public static class B {

		@RestMethod(path="B/b01")
		public String b01() {
			return "foo";
		}

		@RestMethod(path="/B/b02")
		public String b02() {
			return "foo";
		}

		@RestMethod(path="/B/b03")
		public String b03() {
			return "foo";
		}
	}
	private static RestClient rb = MockRestClient.create(B.class).rootUrl("http://localhost/B").build();

	@Remote
	public static interface B01 {
		@RemoteMethod
		public String b01();
		@RemoteMethod(path="b01")
		public String b01a();
		@RemoteMethod(path="/b01/")
		public String b01b();
	}

	@Test
	public void b01_noPath() throws Exception {
		B01 t = rb.getRemote(B01.class);
		assertEquals("foo", t.b01());
		assertEquals("foo", t.b01a());
		assertEquals("foo", t.b01b());
	}

	@Remote(path="http://localhost/B")
	public static interface B02 {
		@RemoteMethod
		public String b01();
		@RemoteMethod(path="b01")
		public String b01a();
		@RemoteMethod(path="/b01/")
		public String b01b();
	}

	@Test
	public void b02_absolutePathOnClass() throws Exception {
		B02 t = rb.getRemote(B02.class);
		assertEquals("foo", t.b01());
		assertEquals("foo", t.b01a());
		assertEquals("foo", t.b01b());
	}

	@Remote
	public static interface B03 {
		@RemoteMethod
		public String b01();
		@RemoteMethod(path="http://localhost/B/b01")
		public String b01a();
		@RemoteMethod(path="http://localhost/B/b01/")
		public String b01b();
	}

	@Test
	public void b03_absolutePathsOnMethods() throws Exception {
		B03 t = rb.getRemote(B03.class);
		assertEquals("foo", t.b01());
		assertEquals("foo", t.b01a());
		assertEquals("foo", t.b01b());
	}

	//=================================================================================================================
	// Other tests
	//=================================================================================================================

	@SuppressWarnings("serial")
	public static class CException extends Exception {
		public CException(String msg) {
			super(msg);
		}
	}

	@Rest(path="/C01")
	public static class C01 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() {
			return "foo";
		}
	}

	@Remote(path="/")
	public static interface C01i {
		@RemoteMethod
		public String a();
	}

	@Test
	public void c01_overriddenRootUrl() throws Exception {
		C01i x = MockRestClient
			.create(C01.class)
			.json()
			.build()
			.getRemote(C01i.class, "http://localhost/C01");

		assertEquals("foo", x.a());
	}

	@Test
	public void c02_rootUriNotSpecified() throws Exception {
		C01i x = MockRestClient
			.create(C01.class)
			.json()
			.rootUrl("")
			.build()
			.getRemote(C01i.class);

		try {
			x.a();
			fail();
		} catch (RemoteMetadataException e) {
			assertEquals("Invalid remote definition found on class org.apache.juneau.rest.client2.RemotesTest$C01i. Root URI has not been specified.  Cannot construct absolute path to remote resource.", e.getLocalizedMessage());
		}
	}


	@Rest(path="/C03")
	public static class C03 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() {
			return "bar";
		}
		@RestMethod
		public String getB() {
			return "baz";
		}
	}

	@Remote(path="/")
	public static interface C03i {
		public String a();
		public String getB();
	}

	@Test
	public void c03_methodNotAnnotated() throws Exception {
		C03i x = MockRestClient
			.create(C03.class)
			.json()
			.build()
			.getRemote(C03i.class);

		assertEquals("bar", x.a());
		assertEquals("baz", x.getB());
	}

	@Rest(path="/C04")
	public static class C04 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() throws CException {
			throw new CException("foo");
		}
	}

	@Remote
	public static interface C04i {
		public String a() throws CException;
	}

	@Test
	public void c04_rethrownException() throws Exception {
		C04i x = MockRestClient
			.create(C04.class)
			.json()
			.build()
			.getRemote(C04i.class);

		try {
			x.a();
			fail();
		} catch (CException e) {
			assertEquals("foo", e.getLocalizedMessage());
		}
	}

	@Rest
	public static class C05 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() throws CException {
			throw new RuntimeException("foo");
		}
	}

	@Remote
	public static interface C05i {
		public String a() throws CException;
	}

	@Test
	public void c05_rethrownUndefinedException() throws Exception {
		C05i x = MockRestClient
			.create(C05.class)
			.json()
			.build()
			.getRemote(C05i.class);

		try {
			x.a();
			fail();
		} catch (RuntimeException e) {
			assertTrue(e.getLocalizedMessage().contains("foo"));
		}
	}

	public static class C06 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() throws CException {
			throw new CException("foo");
		}
	}

	@Remote
	public static interface C06i {
		public Future<String> a() throws CException;
	}

	@Test
	public void c06_rethrownExceptionOnFuture() throws Exception {
		C06i x = MockRestClient
			.create(C06.class)
			.json()
			.build()
			.getRemote(C06i.class);

		try {
			x.a().get();
			fail();
		} catch (ExecutionException e) {
			assertEquals("foo", e.getCause().getLocalizedMessage());
		}
	}

	@Remote
	public static interface C07i {
		public CompletableFuture<String> a() throws CException;
	}

	@Test
	public void c07_rethrownExceptionOnCompletableFuture() throws Exception {
		C07i x = MockRestClient
			.create(C06.class)
			.json()
			.build()
			.getRemote(C07i.class);

		try {
			x.a().get();
			fail();
		} catch (ExecutionException e) {
			assertEquals("foo", e.getCause().getLocalizedMessage());
		}
	}

	@Rest
	public static class C08 implements BasicSimpleJsonRest {
		@RestMethod
		public String a() {
			throw new AssertionError("foo");
		}
	}

	@Remote
	public static interface C08i {
		public Future<String> a() throws AssertionError;
	}

	@Test
	public void c08_rethrownThrowableOnFuture() throws Exception {
		C08i x = MockRestClient
			.create(C08.class)
			.json()
			.build()
			.getRemote(C08i.class);

		try {
			x.a().get();
			fail();
		} catch (ExecutionException e) {
			assertEquals("foo", e.getCause().getCause().getLocalizedMessage());
		}
	}

	//=================================================================================================================
	// Status return type
	//=================================================================================================================

	@Rest
	public static class DA implements BasicSimpleJsonRest {
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
	public static interface DAi {
		@RemoteMethod(path="/r202", returns=RemoteReturn.STATUS)
		public int a() throws AssertionError;
		@RemoteMethod(path="/r202", returns=RemoteReturn.STATUS)
		public Integer b() throws AssertionError;
		@RemoteMethod(path="/r202", returns=RemoteReturn.STATUS)
		public boolean c() throws AssertionError;
		@RemoteMethod(path="/r202", returns=RemoteReturn.STATUS)
		public Boolean d() throws AssertionError;
		@RemoteMethod(path="/r202", returns=RemoteReturn.STATUS)
		public String e() throws AssertionError;

		@RemoteMethod(path="/r400", returns=RemoteReturn.STATUS)
		public int f() throws AssertionError;
		@RemoteMethod(path="/r400", returns=RemoteReturn.STATUS)
		public Integer g() throws AssertionError;
		@RemoteMethod(path="/r400", returns=RemoteReturn.STATUS)
		public boolean h() throws AssertionError;
		@RemoteMethod(path="/r400", returns=RemoteReturn.STATUS)
		public Boolean i() throws AssertionError;
		@RemoteMethod(path="/r400", returns=RemoteReturn.STATUS)
		public String j() throws AssertionError;
	}

	@Test
	public void d01_statusReturnType() throws Exception {
		DAi x = MockRestClient
			.create(DA.class)
			.json()
			.ignoreErrors()
			.build()
			.getRemote(DAi.class);

		assertEquals(202, x.a());
		assertEquals(202, x.b().intValue());
		assertEquals(true, x.c());
		assertEquals(true, x.d());
		assertEquals(400, x.f());
		assertEquals(400, x.g().intValue());
		assertEquals(false, x.h());
		assertEquals(false, x.i());

		try {
			x.e();
			fail();
		} catch (Exception e) {
			assertEquals("Invalid return type on method annotated with @RemoteMethod(returns=RemoteReturn.STATUS).  Only integer and booleans types are valid.", e.getCause().getLocalizedMessage());
		}

		try {
			x.j();
			fail();
		} catch (Exception e) {
			assertEquals("Invalid return type on method annotated with @RemoteMethod(returns=RemoteReturn.STATUS).  Only integer and booleans types are valid.", e.getCause().getLocalizedMessage());
		}
	}

	@Rest
	public static class DB implements BasicSimpleJsonRest {
		@RestMethod
		public Integer getA() {
			return null;
		}
	}

	@Remote
	public static interface DBi {
		@RemoteMethod
		public int a() throws AssertionError;
	}

	@Test
	public void d02_nullPrimitiveReturn() throws Exception {
		DBi x = MockRestClient
			.create(DB.class)
			.json()
			.ignoreErrors()
			.build()
			.getRemote(DBi.class);

		assertEquals(0, x.a());
	}

	@Rest
	public static class DC implements BasicSimpleJsonRest {
		@RestMethod
		public Integer getA() {
			return 1;
		}
	}

	@Remote
	public static interface DCi {
		@RemoteMethod
		public int a() throws AssertionError;
	}

	@Test
	public void d03_primitiveReturn() throws Exception {
		DCi x = MockRestClient
			.create(DC.class)
			.json()
			.ignoreErrors()
			.build()
			.getRemote(DCi.class);

		assertEquals(1, x.a());
	}

	@Rest
	public static class DD implements BasicSimpleJsonRest {
		@RestMethod
		public Integer getA() {
			return null;
		}
	}

	@Remote
	public static interface DDi {
		@RemoteMethod
		public Integer a() throws AssertionError;
	}

	@Test
	public void d04_nullNonPrimitive() throws Exception {
		DDi x = MockRestClient
			.create(DD.class)
			.json()
			.ignoreErrors()
			.build()
			.getRemote(DDi.class);

		assertNull(x.a());
	}

	//=================================================================================================================
	// RRPC interfaces
	//=================================================================================================================

	public interface E1i {
		String echo(String body);
	}

	@Rest
	public static class E1 implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E1i getProxy() {
			return new E1i() {
				@Override
				public String echo(String body) {
					return body;
				}
			};
		}
	}

	@Test
	public void e01_rrpcBasic() throws Exception {
		E1i x = MockRestClient
			.create(E1.class)
			.rootUrl("http://localhost/proxy")
			.json()
			.build()
			.getRrpcInterface(E1i.class);

		assertEquals("foo", x.echo("foo"));
	}

	@Test
	public void e02_rrpc_noRootPath() throws Exception {
		try {
			MockRestClient
				.create(E1.class)
				.rootUrl("")
				.json()
				.build()
				.getRrpcInterface(E1i.class);
		} catch (RemoteMetadataException e) {
			assertEquals("Invalid remote definition found on class org.apache.juneau.rest.client2.RemotesTest$E1i. Root URI has not been specified.  Cannot construct absolute path to remote interface.", e.getMessage());
		}
	}

	@Remote(path="/proxy")
	public interface E3i {
		String echo(String body);
	}

	@Test
	public void e03_rrpc_noRestUrl() throws Exception {
		E3i x = MockRestClient
			.create(E1.class)
			.rootUrl("http://localhost")
			.json()
			.build()
			.getRrpcInterface(E3i.class);

		assertEquals("foo", x.echo("foo"));
	}

	@Remote(path="http://localhost/proxy")
	public interface E4i {
		String echo(String body);
	}

	@Test
	public void e04_rrpc_fullPathOnRemotePath() throws Exception {
		E4i x = MockRestClient
			.create(E1.class)
			.rootUrl("")
			.json()
			.build()
			.getRrpcInterface(E4i.class);

		assertEquals("foo", x.echo("foo"));
	}

	public interface E5i {
		String echo(String body) throws EException;
	}

	@SuppressWarnings("serial")
	public static class EException extends Exception {
		public EException(String msg) {
			super(msg);
		}
	}

	@Rest
	public static class E5 implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E5i getProxy() {
			return new E5i() {
				@Override
				public String echo(String body) throws EException {
					throw new EException("foobar");
				}
			};
		}
	}

	@Test
	public void e05_rrpc_rethrownCheckedException() throws Exception {
		try {
			E5i x = MockRestClient
				.create(E5.class)
				.json()
				.build()
				.getRrpcInterface(E5i.class, "/proxy");

			x.echo("foo");
		} catch (EException e) {
			assertEquals("foobar", e.getMessage());
		}
	}

	@Rest
	public static class E6 implements BasicSimpleJsonRest {
		@RestMethod(name=HttpMethodName.RRPC)
		public E5i getProxy() {
			return new E5i() {
				@Override
				public String echo(String body) throws EException {
					throw new AssertionError("foobar");
				}
			};
		}
	}

	@Test
	public void e06_rrpc_rethrownUncheckedException() throws Exception {
		try {
			E5i x = MockRestClient
				.create(E6.class)
				.json()
				.build()
				.getRrpcInterface(E5i.class, "/proxy");

			x.echo("foo");
		} catch (RuntimeException e) {
			assertEquals(RestCallException.class, e.getCause().getClass());
			assertTrue(e.getCause().getMessage().contains("foobar"));
		}
	}

}
