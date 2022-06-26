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
public class Swagger_Path_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Path("P")
		@Schema(
			d={"a","b"},
			e="a,b",
			t="string"
		)
		public static class A1 {
			public A1(String x) {}
			@Override
			public String toString() {
				return "a";
			}
		}
		@RestGet(path="/a/{P}")
		public void a(A1 f) {}

		@Path(
			name="P",
			schema=@Schema(description="a\nb",type="string",_enum={"a","b"})
		)
		public static class A2 {
			public A2(String x) {}
			@Override
			public String toString() {
				return "b";
			}
		}
		@RestPut(path="/b/{P}")
		public void b(A2 f) {}

		@Path(
			name="P",
			schema=@Schema(description="b\nc",type="string",_enum={"b","c"})
		)
		@Schema(
			d={"a","b"},
			t="string",
			e="a,b"
		)
		public static class A3 {
			public A3(String x) {}
			@Override
			public String toString() {
				return "c";
			}
		}
		@RestPost(path="/c/{P}")
		public void c(A3 f) {}


		@Path("P")
		public static class A4 {
			@Override
			public String toString() {
				return "d";
			}
		}
		@RestDelete(path="/d/{P}")
		public void d(A4 f) {}

		@Path("P")
		@Schema(e="a,b")
		public static class A5 {
			@Override
			public String toString() {
				return "e";
			}
		}
		@RestOp(path="/e/{P}")
		public void e(A5 f) {}
	}

	@Test
	public void a01_fromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a/{P}","get","path","P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		assertObject(x.getEnum()).asJson().is("['a','b']");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',description:'a\\nb',required:true,'enum':['a','b']}");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertEquals("P", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		assertObject(x.getEnum()).asJson().is("['a','b']");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',description:'a\\nb',required:true,'enum':['a','b']}");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertEquals("P", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());
		assertObject(x.getEnum()).asJson().is("['b','c']");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',description:'b\\nc',required:true,'enum':['b','c']}");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertEquals("P", x.getName());
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true}");

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertObject(x.getEnum()).asJson().is("['a','b']");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true,'enum':['a','b']}");
	}

	@Rest
	public static class B {

		@Path(name="P")
		public static class B1 {}
		@RestGet(path="/a/{P}")
		public void a(B1 f) {}

		@Path("P")
		public static class B2 {
			public String f1;
		}
		@RestPut(path="/b/{P}")
		public void b(B2 b) {}

		@Path("P")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost(path="/c/{P}")
		public void c(B3 b) {}

		@Path("P")
		public static class B4 {}
		@RestDelete(path="/d/{P}")
		public void d(B4 b) {}
	}

	@Test
	public void b01_schemaFromPojo() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a/{P}","get","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true}");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'object',required:true,schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'array',required:true,items:{type:'string'}}");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true}");
	}

	@Rest
	public static class D {

		@RestGet(path="/a/{P}")
		public void a(
			@Path("P")
			@Schema(d="a", t="string")
			String h
		) {}

		@RestPut(path="/b/{P}")
		public void b(
			@Path(
				name="P",
				schema=@Schema(description="a",type="string")
			)
			String h
		) {}

		@RestPost(path="/c/{P}")
		public void c(
			@Path(
				name="P",
				schema=@Schema(description="b",type="string")
			)
			@Schema(d="a", t="string")
			String h
		) {}

		@RestDelete(path="/d/{P}")
		public void d(@Path("P") String h) {}

		@RestOp(path="/e/{P}")
		public void e(@Path("P") @Schema(e="a,b") String h) {}
	}

	@Test
	public void d01_fromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a/{P}","get","path","P");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertEquals("a", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertEquals("b", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertEquals("P", x.getName());

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertObject(x.getEnum()).asJson().is("['a','b']");
	}

	@Rest
	public static class E {

		@RestGet(path="/a/{P}")
		public void a(@Path("P") String h) {}

		public static class E2 {
			public String f1;
		}
		@RestPut(path="/b/{P}")
		public void b(@Path("P") E2 b) {}

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost(path="/c/{P}")
		public void c(@Path("P") E3 b) {}

		public static class E4 {}
		@RestDelete(path="/d/{P}")
		public void d(@Path("P") E4 b) {}

		@RestOp(path="/e/{P}")
		public void e(@Path("P") Integer b) {}

		@RestGet(path="/f/{P}")
		public void f(@Path("P") Boolean b) {}
	}

	@Test
	public void d01_schemaFromParameter() throws Exception {
		org.apache.juneau.dto.swagger.Swagger s = getSwagger(E.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a/{P}","get","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true}");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'object',required:true,schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'array',required:true,items:{type:'string'}}");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'string',required:true}");

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'integer',required:true,format:'int32'}");

		x = s.getParameterInfo("/f/{P}","get","path","P");
		assertObject(x).asJson().is("{'in':'path',name:'P',type:'boolean',required:true}");
	}
}