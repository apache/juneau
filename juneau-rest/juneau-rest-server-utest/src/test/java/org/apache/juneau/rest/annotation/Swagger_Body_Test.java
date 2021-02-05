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
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.jsonschema.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Swagger_Body_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Body(
			d={"a","b"},
			r=true,
			schema=@Schema(type="string"),
			ex=" 'a' ",
			exs="{foo:'bar'}"
		)
		public static class A1 {
			public A1(String x) {}
		}
		@RestOp
		public void a(A1 h) {}

		@Body({
			"description:'a\nb',",
			"required:true,",
			"schema:{type:'string'},",
			"example:'\\'a\\'',",
			"examples:{foo:'bar'}"
		})
		public static class A2 {
			public A2(String x) {}
		}
		@RestOp
		public void b(A2 h) {}

		@Body(
			value={
				"description:'a\nb',",
				"required:true,",
				"schema:{type:'string'},",
				"example:'\\'a\\'',",
				"examples:{foo:'bar'}"
			},
			d={"b","c"},
			schema=@Schema(type="string"),
			ex="'b'",
			exs="{foo:'baz'}"
		)
		public static class A3 {
			public A3(String x) {}
		}
		@RestOp
		public void c(A3 h) {}
	}

	@Test
	public void a01_fromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("'a'", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");

		x = s.getParameterInfo("/b","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("'a'", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");

		x = s.getParameterInfo("/c","get","body",null);
		assertEquals("b\nc", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("'b'", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'baz'}");
	}

	@Rest
	public static class B {

		@Body(schema=@Schema(" type:'b' "))
		public static class B1 {}
		@RestOp
		public void a(B1 h) {}

		@Body
		public static class B2 {
			public String f1;
		}
		@RestOp
		public void b(B2 b) {}

		@Body
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestOp
		public void c(B3 b) {}

		@Body
		public static class B4 {}
		@RestOp
		public void d(B4 b) {}
	}

	@Test
	public void b01_schemaFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'b'}");

		x = s.getParameterInfo("/b","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'object',properties:{f1:{type:'string'}}}");

		x = s.getParameterInfo("/c","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
	}

	@Rest
	public static class C {
		@Body(ex=" {f1:'b'} ")
		public static class C1 {
			public String f1;
		}
		@RestOp
		public void a(C1 h) {}

		@Body(exs={" foo:'bar' "})
		public static class C2 {}
		@RestOp
		public void b(C2 h) {}
	}

	@Test
	public void c01_exampleFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(C.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertEquals("{f1:'b'}", x.getExample());

		x = s.getParameterInfo("/b","get","body",null);
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");
	}

	@Rest
	public static class D {

		public static class D1 {
			public D1(String x) {}
		}

		@RestOp
		public void a(
			@Body(
				d= {"a","b"},
				r=true,
				schema=@Schema(type="string"),
				ex="a",
				exs=" {foo:'bar'} "
			) D1 b) {}

		public static class D2 {
			public D2(String x) {}
		}

		@RestOp
		public void b(
			@Body({
				"description:'a\nb',",
				"required:true,",
				"schema:{type:'string'},",
				"example:'a',",
				"examples:{foo:'bar'}"
			}) D2 b) {}

		public static class D3 {
			public D3(String x) {}
		}

		@RestOp
		public void c(
			@Body(
				value= {
					"description:'a\nb',",
					"required:true,",
					"schema:{type:'string'},",
					"example:'a',",
					"examples:{foo:'bar'}"
				},
				d= {"b","c"},
				schema=@Schema(type="string"),
				ex="b",
				exs=" {foo:'baz'} "
			) D3 b) {}
	}

	@Test
	public void d01_fromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("a", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");

		x = s.getParameterInfo("/b","get","body",null);
		assertEquals("a\nb", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("a", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");

		x = s.getParameterInfo("/c","get","body",null);
		assertEquals("b\nc", x.getDescription());
		assertObject(x.getRequired()).asJson().is("true");
		assertObject(x.getSchema()).asJson().is("{type:'string'}");
		assertEquals("b", x.getExample());
		assertObject(x.getExamples()).asJson().is("{foo:'baz'}");
	}

	@Rest
	public static class E {

		public static class E1 {}
		@RestOp
		public void a(@Body(schema=@Schema(" { type:'b' } ")) E1 b) {}

		public static class E2 {
			public String f1;
		}
		@RestOp
		public void b(@Body E2 b) {}

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestOp
		public void c(@Body E3 b) {}

		public static class E4 {}
		@RestOp
		public void d(@Body E4 b) {}

		@RestOp
		public void e(@Body Integer b) {}

		@RestOp
		public void f(@Body Boolean b) {}
	}

	@Test
	public void e01_schemaFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(E.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'b'}");

		x = s.getParameterInfo("/b","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'object',properties:{f1:{type:'string'}}}");

		x = s.getParameterInfo("/c","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'string'}");

		x = s.getParameterInfo("/e","get","body",null);
		assertObject(x.getSchema()).asJson().is("{format:'int32',type:'integer'}");

		x = s.getParameterInfo("/f","get","body",null);
		assertObject(x.getSchema()).asJson().is("{type:'boolean'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {

		public static class F1 {
			public String f1;
		}
		@RestOp
		public void a(@Body(ex="{f1:'b'}") F1 b) {}

		public static class F2 {}
		@RestOp
		public void b(@Body(exs={" foo:'bar' "}) F2 b) {}
	}

	@Test
	public void f01_exampleFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(F.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","body",null);
		assertEquals("{f1:'b'}", x.getExample());

		x = s.getParameterInfo("/b","get","body",null);
		assertObject(x.getExamples()).asJson().is("{foo:'bar'}");
	}
}
