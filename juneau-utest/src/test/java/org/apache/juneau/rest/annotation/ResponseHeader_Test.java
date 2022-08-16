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

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ResponseHeader_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Header on method parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet
		public void a(Value<A1> h) {
			h.set(new A1());
		}
		@RestGet
		public void b(@Header(name="Foo") Value<String> h) {
			h.set("foo");
		}
		@RestGet
		public void c(@Header(name="Bar") Value<A1> h) {
			h.set(new A1());
		}
	}

	@Header(name="Foo")
	public static class A1 {
		@Override
		public String toString() {return "foo";}
	}

	@Test
	public void a01_methodParameters() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/a")
			.run()
			.assertStatus(200)
			.assertHeader("Foo").is("foo");
		a.get("/b")
			.run()
			.assertStatus(200)
			.assertHeader("Foo").is("foo");
		a.get("/c")
			.run()
			.assertStatus(200)
			.assertHeader("Bar").is("foo");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Header swagger on POJOs
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {

		@Header(
			name="H"
		)
		@Schema(
			description="a",
			type="string"
		)
		public static class B1 {}
		@RestGet
		public void a(Value<B1> h) {}

		@Header(
			name="H",
			schema=@Schema(description="a",type="string")
		)
		public static class B2 {}
		@RestGet
		public void b(Value<B2> h) {}

		@Header(
			name="H",
			schema=@Schema(description="b",type="number")
		)
		@Schema(
			description="a",
			type="string"
		)
		public static class B3 {}
		@RestGet
		public void c(Value<B3> h) {}

		@Header(name="H") @StatusCode(100)
		public static class B4 {}
		@RestGet
		public void d(Value<B4> h) {}

		@Header(name="H") @StatusCode({100,101})
		public static class B5 {}
		@RestGet
		public void e(Value<B5> h) {}

		@Header(name="H") @Schema(description="a")
		public static class B6 {}
		@RestGet
		public void f(Value<B6> h) {}

		@Header("H")
		public static class B7 {}
		@RestGet
		public void g(Value<B7> h) {}
	}

	@Test
	public void b01_swagger_onPojo() throws Exception {
		HeaderInfo x;
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(B.class);

		x = s.getResponseInfo("/a","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getResponseInfo("/b","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getResponseInfo("/c","get",200).getHeader("H");
		assertEquals("b", x.getDescription());
		assertEquals("number", x.getType());

		x = s.getResponseInfo("/d","get",100).getHeader("H");
		assertNotNull(x);

		Operation x2 = s.getOperation("/e","get");
		assertNotNull(x2.getResponse(100).getHeader("H"));
		assertNotNull(x2.getResponse(101).getHeader("H"));

		x = s.getResponseInfo("/f","get",200).getHeader("H");
		assertEquals("a", x.getDescription());

		x = s.getResponseInfo("/g","get",200).getHeader("H");
		assertNotNull(x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// @Header swagger on method parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {

		public static class C1 {}
		@RestGet
		public void a(
			@Header(
				name="H"
			)
			@Schema(
				description="a",
				type="string"
			)
			Value<C1> h) {}

		public static class C2 {}
		@RestGet
		public void b(
			@Header(
				name="H",
				schema=@Schema(description="a",type="string")
			)
			Value<C2> h) {}

		public static class C3 {}
		@RestGet
		public void c(
			@Header(
				name="H",
				schema=@Schema(description="b",type="number")
			)
			@Schema(
				description="a",
				type="string"
			)
			Value<C3> h) {}

		public static class C4 {}
		@RestGet
		public void d(@Header(name="H") @StatusCode(100) Value<C4> h) {}

		public static class C5 {}
		@RestGet
		public void e(@Header(name="H") @StatusCode({100,101}) Value<C5> h) {}

		public static class C6 {}
		@RestGet
		public void f(@Header(name="H") @Schema(description="a") Value<C6> h) {}

		public static class C7 {}
		@RestGet
		public void g(@Header("H") Value<C7> h) {}
	}

	@Test
	public void c01_swagger_onMethodParameters() throws Exception {
		HeaderInfo x;
		org.apache.juneau.dto.swagger.Swagger sc = getSwagger(C.class);

		x = sc.getResponseInfo("/a","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = sc.getResponseInfo("/b","get",200).getHeader("H");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = sc.getResponseInfo("/c","get",200).getHeader("H");
		assertEquals("b", x.getDescription());
		assertEquals("number", x.getType());

		x = sc.getResponseInfo("/d","get",100).getHeader("H");
		assertNotNull(x);

		Operation x2 = sc.getOperation("/e","get");
		assertNotNull(x2.getResponse(100).getHeader("H"));
		assertNotNull(x2.getResponse(101).getHeader("H"));

		x = sc.getResponseInfo("/f","get",200).getHeader("H");
		assertEquals("a", x.getDescription());

		x = sc.getResponseInfo("/g","get",200).getHeader("H");
		assertNotNull(x);
	}
}
