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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class Swagger_Response_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Response(
			schema=@Schema(description={"a","b"},type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class A1 {
			public A1(String x){}
		}
		@RestGet
		public void a(Value<A1> r) { /* no-op */ }
		@RestPut
		public A1 b() {return null;}

		@Response(
			schema=@Schema(description="a\nb",type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class A2 {
			public A2(String x){}
		}
		@RestPost
		public void c(Value<A2> r) { /* no-op */ }
		@RestDelete
		public A2 d() {return null;}

		@Response(
			schema=@Schema(description={"a","b"},type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class A3 {
			public A3(String x){}
		}
		@RestOp
		public void e(Value<A3> r) { /* no-op */ }
		@RestOp
		public A3 f() {return null;}

		@Response @StatusCode(100)
		public static class A4 {}
		@RestOp
		public void g(Value<A4> r) { /* no-op */ }
		@RestOp
		public A4 h() {return null;}

		@Response @StatusCode(100)
		public static class A5 {}
		@RestOp
		public void i(Value<A5> r) { /* no-op */ }
		@RestOp
		public A5 j() {return null;}

		@Response(headers=@Header(name="foo",schema=@Schema(type="object")))
		public static class A6 {}
		@RestOp
		public void k(Value<A6> r) { /* no-op */ }
		@RestOp
		public A6 l() {return null;}
	}

	@Test void a01_fromPojo() {
		var s = getSwagger(A.class);
		var x = s.getResponseInfo("/a","get",200);

		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/b","put",200);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/c","post",200);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/d","delete",200);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/e","get",200);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/f","get",200);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/g","get",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/h","get",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/i","get",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/j","get",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/k","get",200);
		assertJson(x.getHeaders(), "{foo:{type:'object'}}");

		x = s.getResponseInfo("/l","get",200);
		assertJson(x.getHeaders(), "{foo:{type:'object'}}");
	}

	@Rest
	public static class B {

		@Response(schema=@Schema(type="number"))
		public static class B1 {}
		@RestGet
		public void a(Value<B1> r) { /* no-op */ }
		@RestPut
		public B1 b() {return null;}

		@Response
		public static class B2 {
			public String f1;
		}
		@RestPost
		public void c(Value<B2> b) { /* no-op */ }
		@RestDelete
		public B2 d() {return null;}

		@Response
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestOp
		public void e(Value<B3> b) { /* no-op */ }
		@RestOp
		public B3 f() {return null;}

		@Response
		public static class B4 {}
		@RestOp
		public void g(Value<B4> b) { /* no-op */ }
		@RestOp
		public B4 h() {return null;}
	}

	@Test void b01_schemaFromPojo() {
		var s = getSwagger(B.class);
		var x = s.getResponseInfo("/a","get",200);

		assertJson(x.getSchema(), "{type:'number'}");

		x = s.getResponseInfo("/b","put",200);
		assertJson(x.getSchema(), "{type:'number'}");

		x = s.getResponseInfo("/c","post",200);
		assertJson(x.getSchema(), "{type:'object',properties:{f1:{type:'string'}}}");

		x = s.getResponseInfo("/d","delete",200);
		assertJson(x.getSchema(), "{type:'object',properties:{f1:{type:'string'}}}");

		x = s.getResponseInfo("/e","get",200);
		assertJson(x.getSchema(), "{type:'array',items:{type:'string'}}");

		x = s.getResponseInfo("/f","get",200);
		assertJson(x.getSchema(), "{type:'array',items:{type:'string'}}");

		x = s.getResponseInfo("/g","get",200);
		assertJson(x.getSchema(), "{type:'string'}");

		x = s.getResponseInfo("/h","get",200);
		assertJson(x.getSchema(), "{type:'string'}");
	}

	@Rest
	public static class C {

		public static class C1 {
			public String f1;
		}
		@RestGet
		public void a(Value<C1> r) { /* no-op */ }
		@RestPut
		public C1 b() {return null;}

		@Response(examples={" foo:'b' "})
		public static class C2 {
			public C2(String x){}
		}
		@RestPost
		public void c(Value<C2> r) { /* no-op */ }
		@RestDelete
		public C2 d() {return null;}
	}

	@Test void c01_exampleFromPojo() {
		var sc = getSwagger(C.class);
		var x = sc.getResponseInfo("/c","post",200);

		sc.getResponseInfo("/a","get",200);

		sc.getResponseInfo("/b","put",200);

		assertJson(x.getExamples(), "{foo:'b'}");

		x = sc.getResponseInfo("/d","delete",200);
		assertJson(x.getExamples(), "{foo:'b'}");
	}

	@Rest
	@SuppressWarnings("unused")
	public static class D {

		@Response(
			schema=@Schema(description={"a","b"},type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class D1 extends Throwable {}
		@RestGet
		public void a() throws D1 { /* no-op */ }

		@Response(
			schema=@Schema(description={"a","b"},type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class D2 extends Throwable {}
		@RestPut
		public void b() throws D2 { /* no-op */ }

		@Response(
			schema=@Schema(description={"a","b"},type="string"),
			headers=@Header(name="foo",schema=@Schema(type="string")),
			examples=" {foo:'a'} "
		)
		public static class D3 extends Throwable {}
		@RestPost
		public void c() throws D3 { /* no-op */ }

		@Response @StatusCode(100)
		public static class D4 extends Throwable {}
		@RestDelete
		public void d() throws D4 { /* no-op */ }

		@Response @StatusCode(100)
		public static class D5 extends Throwable {}
		@RestOp
		public void e() throws D5 { /* no-op */ }

		@Response(headers=@Header(name="foo", schema=@Schema(type="number")))
		public static class D6 extends Throwable {}
		@RestOp
		public void f() throws D6 { /* no-op */ }
	}

	@Test void d01_fromThrowable() {
		var s = getSwagger(D.class);
		var x = s.getResponseInfo("/a","get",500);

		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/b","put",500);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/c","post",500);
		assertEquals("a\nb", x.getDescription());
		assertJson(x.getSchema(), "{type:'string'}");
		assertJson(x.getHeaders(), "{foo:{type:'string'}}");
		assertJson(x.getExamples(), "{foo:'a'}");

		x = s.getResponseInfo("/d","delete",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/e","get",100);
		assertEquals("Continue", x.getDescription());

		x = s.getResponseInfo("/f","get",500);
		assertJson(x.getHeaders(), "{foo:{type:'number'}}");
	}

	@Rest
	@SuppressWarnings("unused")
	public static class E {

		@Response(schema=@Schema(type="number"))
		public static class E1 extends Throwable {}
		@RestGet
		public void a() throws E1 { /* no-op */ }
	}

	@Test void e01_schemaFromThrowable() {
		var s = getSwagger(E.class);

		var x = s.getResponseInfo("/a","get",500);
		assertJson(x.getSchema(), "{type:'number'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	@SuppressWarnings("unused")
	public static class F {

		@Response(examples={" foo:'b' "})
		public static class F2 extends Throwable {}
		@RestPut
		public void b() throws F2 { /* no-op */ }
	}

	@Test void f01_exampeFromThrowable() {
		var s = getSwagger(F.class);
		var x = s.getResponseInfo("/b","put",500);

		assertJson(x.getExamples(), "{foo:'b'}");
	}
}