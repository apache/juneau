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

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Swagger_FormData_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@FormData(
			name="F",
			description= {"a","b"},
			type="string"
		)
		public static class A1 {
			public A1(String x) {}
		}
		@RestOp
		public void a(A1 f) {}

		@FormData(
			name="F",
			api={
				"description:'a\nb',",
				"type:'string'"
			}
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestOp
		public void b(A2 f) {}

		@FormData(
			name="F",
			api={
				"description:'b\nc',",
				"type:'string'"
			},
			description= {"a","b"},
			type="string"
		)
		public static class A3 {
			public A3(String x) {}
		}
		@RestOp
		public void c(A3 f) {}

		@FormData("F")
		public static class A4 {}
		@RestOp
		public void d(A4 f) {}
	}

	@Test
	public void a01_fromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","get","formData","F");
		assertEquals("F", x.getName());
	}

	@Rest
	public static class B {

		@FormData(name="F")
		public static class B1 {}
		@RestOp
		public void a(B1 f) {}

		@FormData("F")
		public static class B2 {
			public String f1;
		}
		@RestOp
		public void b(B2 f) {}

		@FormData("F")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestOp
		public void c(B3 f) {}

		@FormData("F")
		public static class B4 {}
		@RestOp
		public void d(B4 f) {}
	}

	@Test
	public void b01_schemaFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/b","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'string'}");
	}

	@Rest
	public static class C {

		@FormData(name="F", example={"{f1:'a'}"})
		public static class C1 {
			public String f1;
		}
		@RestOp
		public void a(C1 f) {}
	}

	@Test
	public void c01_exampleFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(C.class);

		ParameterInfo x = s.getParameterInfo("/a","get","formData","F");
		assertEquals("{f1:'a'}", x.getExample());
	}

	@Rest
	public static class D {

		@RestOp
		public void a(
			@FormData(
				name="F",
				description={"a","b"},
				type="string"
			) String f) {}

		@RestOp
		public void b(
			@FormData(
				name="F",
				api={
					"description:'a\nb',",
					"type:'string'",
				}
			) String f) {}

		@RestOp
		public void c(
			@FormData(
				name="F",
				api={
					"description:'b\nc',",
					"type:'string'",
				},
				description={"a","b"},
				type="string"
			) String f) {}

		@RestOp
		public void d(@FormData("F") String f) {}
	}

	@Test
	public void d01_fromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","get","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","get","formData","F");
		assertEquals("F", x.getName());
	}

	@Rest
	public static class E {

		@RestOp
		public void a(@FormData(name="F") String f) {}

		public static class E2 {
			public String f1;
		}
		@RestOp
		public void b(@FormData("F") E2 b) {}

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestOp
		public void c(@FormData("F") E3 b) {}

		public static class E4 {}
		@RestOp
		public void d(@FormData("F") E4 b) {}

		@RestOp
		public void e(@FormData("F") Integer b) {}

		@RestOp
		public void f(@FormData("F") Boolean b) {}
	}

	@Test
	public void e01_schemaFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(E.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/b","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/e","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'integer',format:'int32'}");

		x = s.getParameterInfo("/f","get","formData","F");
		assertObject(x).asJson().is("{'in':'formData',name:'F',type:'boolean'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestOp
		public void a(@FormData(name="F", example="{f1:'a'}") String f) {}
	}

	@Test
	public void f01_exampleFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(F.class);

		ParameterInfo x = s.getParameterInfo("/a","get","formData","F");
		assertEquals("{f1:'a'}", x.getExample());
	}
}
