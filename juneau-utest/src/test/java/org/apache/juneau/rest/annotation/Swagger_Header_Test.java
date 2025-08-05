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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

public class Swagger_Header_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Header(
			name="H"
		)
		@Schema(description={"a","b"}, type="string")
		public static class A1 {
			public A1(String x) {}
		}
		@RestGet
		public void a(A1 h) { /* no-op */ }

		@Header(
			name="H",
			schema=@Schema(description="a\nb",type="string")
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestPut
		public void b(A2 h) { /* no-op */ }

		@Header(
			name="H",
			schema=@Schema(description="b\nc",type="string")
		)
		@Schema(description={"a","b"}, type="string")
		public static class A3 {
			public A3(String x) {}
		}
		@RestPost
		public void c(A3 h) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","header","H");
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getType()).asJson().is("'string'");

		x = s.getParameterInfo("/b","put","header","H");
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getType()).asJson().is("'string'");

		x = s.getParameterInfo("/c","post","header","H");
		assertEquals("b\nc", x.getDescription());
		assertObject(x.getType()).asJson().is("'string'");
	}

	@Rest
	public static class B {

		@Header(name="H")
		public static class B1 {}
		@RestGet
		public void a(B1 h) { /* no-op */ }

		@Header(name="H")
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 b) { /* no-op */ }

		@Header(name="H")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 b) { /* no-op */ }

		@Header(name="H")
		public static class B4 {}
		@RestDelete
		public void d(B4 b) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'string'}");

		x = s.getParameterInfo("/b","put","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","post","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","delete","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'string'}");
	}

	@Rest
	public static class D {

		@RestGet
		public void a(
			@Header(
				name="H"
			)
			@Schema(description={"a","b"}, type="string")
			String h) { /* no-op */ }

		@RestPut
		public void b(
			@Header(
				name="H",
				schema=@Schema(description="a\nb",type="string")
			) String h) { /* no-op */ }

		@RestPost
		public void c(
			@Header(
				name="H",
				schema=@Schema(description="b\nc",type="string")
			)
			@Schema(description={"a","b"}, type="string")
			String h) { /* no-op */ }

		@RestDelete
		public void d(@Header("H") String h) { /* no-op */ }
	}

	@Test void d01_fromParameter() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","header","H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","put","header","H");
		assertEquals("H", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","post","header","H");
		assertEquals("H", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","delete","header","H");
		assertEquals("H", x.getName());
	}

	@Rest
	public static class E {

		@RestGet
		public void a(@Header(name="H") String h) { /* no-op */ }

		public static class E2 {
			public String f1;
		}
		@RestPut
		public void b(@Header("H") E2 b) { /* no-op */ }

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(@Header("H") E3 b) { /* no-op */ }

		public static class E4 {}
		@RestDelete
		public void d(@Header("H") E4 b) { /* no-op */ }

		@RestOp
		public void e(@Header("H") Integer b) { /* no-op */ }

		@RestGet
		public void f(@Header("H") Boolean b) { /* no-op */ }
	}

	@Test void e01_schemaFromParameter() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(E.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'string'}");

		x = s.getParameterInfo("/b","put","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","post","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","delete","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'string'}");

		x = s.getParameterInfo("/e","get","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'integer',format:'int32'}");

		x = s.getParameterInfo("/f","get","header","H");
		assertObject(x).asJson().is("{'in':'header',name:'H',type:'boolean'}");
	}
}