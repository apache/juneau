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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Swagger_Header_Test {

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
		public void a(A1 h) {}

		@Header(
			name="H",
			schema=@Schema(description="a\nb",type="string")
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestPut
		public void b(A2 h) {}

		@Header(
			name="H",
			schema=@Schema(description="b\nc",type="string")
		)
		@Schema(description={"a","b"}, type="string")
		public static class A3 {
			public A3(String x) {}
		}
		@RestPost
		public void c(A3 h) {}
	}

	@Test
	public void a01_fromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(A.class);
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
		public void a(B1 h) {}

		@Header(name="H")
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 b) {}

		@Header(name="H")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 b) {}

		@Header(name="H")
		public static class B4 {}
		@RestDelete
		public void d(B4 b) {}
	}

	@Test
	public void b01_schemaFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(B.class);
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
			String h) {}

		@RestPut
		public void b(
			@Header(
				name="H",
				schema=@Schema(description="a\nb",type="string")
			) String h) {}

		@RestPost
		public void c(
			@Header(
				name="H",
				schema=@Schema(description="b\nc",type="string")
			)
			@Schema(description={"a","b"}, type="string")
			String h) {}

		@RestDelete
		public void d(@Header("H") String h) {}
	}

	@Test
	public void d01_fromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(D.class);
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
		public void a(@Header(name="H") String h) {}

		public static class E2 {
			public String f1;
		}
		@RestPut
		public void b(@Header("H") E2 b) {}

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(@Header("H") E3 b) {}

		public static class E4 {}
		@RestDelete
		public void d(@Header("H") E4 b) {}

		@RestOp
		public void e(@Header("H") Integer b) {}

		@RestGet
		public void f(@Header("H") Boolean b) {}
	}

	@Test
	public void e01_schemaFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(E.class);
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
