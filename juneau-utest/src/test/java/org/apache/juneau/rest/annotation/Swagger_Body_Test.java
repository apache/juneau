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
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

class Swagger_Body_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Content
		@Schema(
			d={"a","b"},
			type="string",
			r=true
		)
		public static class A1 {
			public A1(String x) {}
		}
		@RestGet
		public void a(A1 h) { /* no-op */ }

		@Content
		@Schema(
			description="a\nb",
			required=true,
			type="string"
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestPut
		public void b(A2 h) { /* no-op */ }

		@Content
		@Schema(
			description="a\nb",
			required=true,
			type="string"
		)
		public static class A3 {
			public A3(String x) {}
		}
		@RestPost
		public void c(A3 h) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		var s = getSwagger(A.class);
		var x = s.getParameterInfo("/a","get","body",null);

		assertBean(x, "description,required,schema{required,type}", "a\nb,true,{true,string}");

		x = s.getParameterInfo("/b","put","body",null);
		assertBean(x, "description,required,schema{required,type}", "a\nb,true,{true,string}");

		x = s.getParameterInfo("/c","post","body",null);
		assertBean(x, "description,required,schema{required,type}", "a\nb,true,{true,string}");
	}

	@Rest
	public static class B {

		@Content
		@Schema(type="object")
		public static class B1 {}
		@RestGet
		public void a(B1 h) { /* no-op */ }

		@Content
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 b) { /* no-op */ }

		@Content
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 b) { /* no-op */ }

		@Content
		public static class B4 {}
		@RestDelete
		public void d(B4 b) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		var s = getSwagger(B.class);
		var x = s.getParameterInfo("/a","get","body",null);

		assertJson("{type:'object'}", x.getSchema());

		x = s.getParameterInfo("/b","put","body",null);
		assertJson("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());

		x = s.getParameterInfo("/c","post","body",null);
		assertJson("{type:'array',items:{type:'string'}}", x.getSchema());

		x = s.getParameterInfo("/d","delete","body",null);
		assertJson("{type:'string'}", x.getSchema());
	}

	@Rest
	public static class D {

		public static class D1 {
			public D1(String x) {}
		}

		@RestGet
		public void a(
			@Content
			@Schema(
				d= {"a","b"},
				r=true,
				type="string"
			)
			D1 b) { /* no-op */ }

		public static class D2 {
			public D2(String x) {}
		}

		@RestPut
		public void b(
			@Content
			@Schema(
				description="a\nb",
				required=true,
				type="string"
			)
			D2 b) { /* no-op */ }

		public static class D3 {
			public D3(String x) {}
		}

		@RestPost
		public void c(
			@Content
			@Schema(
				d= {"b","c"},
				required=true,
				type="string"
			)
			D3 b) { /* no-op */ }
	}

	@Test void d01_fromParameter() {
		var s = getSwagger(D.class);
		var x = s.getParameterInfo("/a","get","body",null);

		assertBean(x, "description,required,schema{required,type}", "a\nb,true,{true,string}");

		x = s.getParameterInfo("/b","put","body",null);
		assertBean(x, "description,required,schema{required,type}", "a\nb,true,{true,string}");

		x = s.getParameterInfo("/c","post","body",null);
		assertBean(x, "description,required,schema{required,type}", "b\nc,true,{true,string}");
	}

	@Rest
	public static class E {

		public static class E1 {}
		@RestGet
		public void a(@Content @Schema(type="object") E1 b) { /* no-op */ }

		public static class E2 {
			public String f1;
		}
		@RestPut
		public void b(@Content E2 b) { /* no-op */ }

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(@Content E3 b) { /* no-op */ }

		public static class E4 {}
		@RestDelete
		public void d(@Content E4 b) { /* no-op */ }

		@RestOp
		public void e(@Content Integer b) { /* no-op */ }

		@RestGet
		public void f(@Content Boolean b) { /* no-op */ }
	}

	@Test void e01_schemaFromParameter() {
		var s = getSwagger(E.class);
		var x = s.getParameterInfo("/a","get","body",null);

		assertJson("{type:'object'}", x.getSchema());

		x = s.getParameterInfo("/b","put","body",null);
		assertJson("{type:'object',properties:{f1:{type:'string'}}}", x.getSchema());

		x = s.getParameterInfo("/c","post","body",null);
		assertJson("{type:'array',items:{type:'string'}}", x.getSchema());

		x = s.getParameterInfo("/d","delete","body",null);
		assertJson("{type:'string'}", x.getSchema());

		x = s.getParameterInfo("/e","get","body",null);
		assertJson("{format:'int32',type:'integer'}", x.getSchema());

		x = s.getParameterInfo("/f","get","body",null);
		assertJson("{type:'boolean'}", x.getSchema());
	}
}