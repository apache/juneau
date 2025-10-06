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

class Swagger_Path_Test extends TestBase {

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
		public void a(A1 f) { /* no-op */ }

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
		public void b(A2 f) { /* no-op */ }

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
		public void c(A3 f) { /* no-op */ }


		@Path("P")
		public static class A4 {
			@Override
			public String toString() {
				return "d";
			}
		}
		@RestDelete(path="/d/{P}")
		public void d(A4 f) { /* no-op */ }

		@Path("P")
		@Schema(e="a,b")
		public static class A5 {
			@Override
			public String toString() {
				return "e";
			}
		}
		@RestOp(path="/e/{P}")
		public void e(A5 f) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		var s = getSwagger(A.class);
		var x = s.getParameterInfo("/a/{P}","get","path","P");

		assertBean(x, "in,name,type,description,required,enum", "path,P,string,a\nb,true,[a,b]");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertBean(x, "in,name,type,description,required,enum", "path,P,string,a\nb,true,[a,b]");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertBean(x, "in,name,type,description,required,enum", "path,P,string,b\nc,true,[b,c]");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertBean(x, "in,name,type,required", "path,P,string,true");

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertBean(x, "in,name,type,required,enum", "path,P,string,true,[a,b]");
	}

	@Rest
	public static class B {

		@Path(name="P")
		public static class B1 {}
		@RestGet(path="/a/{P}")
		public void a(B1 f) { /* no-op */ }

		@Path("P")
		public static class B2 {
			public String f1;
		}
		@RestPut(path="/b/{P}")
		public void b(B2 b) { /* no-op */ }

		@Path("P")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost(path="/c/{P}")
		public void c(B3 b) { /* no-op */ }

		@Path("P")
		public static class B4 {}
		@RestDelete(path="/d/{P}")
		public void d(B4 b) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		var s = getSwagger(B.class);
		var x = s.getParameterInfo("/a/{P}","get","path","P");

		assertBean(x, "in,name,type,required", "path,P,string,true");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertBean(x, "in,name,type,required,schema{properties{f1{type}}}", "path,P,object,true,{{{string}}}");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertBean(x, "in,name,type,required,items{type}", "path,P,array,true,{string}");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertBean(x, "in,name,type,required", "path,P,string,true");
	}

	@Rest
	public static class D {

		@RestGet(path="/a/{P}")
		public void a(
			@Path("P")
			@Schema(d="a", t="string")
			String h
		) { /* no-op */ }

		@RestPut(path="/b/{P}")
		public void b(
			@Path(
				name="P",
				schema=@Schema(description="a",type="string")
			)
			String h
		) { /* no-op */ }

		@RestPost(path="/c/{P}")
		public void c(
			@Path(
				name="P",
				schema=@Schema(description="b",type="string")
			)
			@Schema(d="a", t="string")
			String h
		) { /* no-op */ }

		@RestDelete(path="/d/{P}")
		public void d(@Path("P") String h) { /* no-op */ }

		@RestOp(path="/e/{P}")
		public void e(@Path("P") @Schema(e="a,b") String h) { /* no-op */ }
	}

	@Test void d01_fromParameter() {
		var s = getSwagger(D.class);
		var x = s.getParameterInfo("/a/{P}","get","path","P");

		assertBean(x, "description,type", "a,string");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertBean(x, "description,type", "a,string");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertBean(x, "description,type", "b,string");

		x = s.getParameterInfo("/d/{P}","delete","path","P");

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertList(x.getEnum(), "a", "b");
	}

	@Rest
	public static class E {

		@RestGet(path="/a/{P}")
		public void a(@Path("P") String h) { /* no-op */ }

		public static class E2 {
			public String f1;
		}
		@RestPut(path="/b/{P}")
		public void b(@Path("P") E2 b) { /* no-op */ }

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost(path="/c/{P}")
		public void c(@Path("P") E3 b) { /* no-op */ }

		public static class E4 {}
		@RestDelete(path="/d/{P}")
		public void d(@Path("P") E4 b) { /* no-op */ }

		@RestOp(path="/e/{P}")
		public void e(@Path("P") Integer b) { /* no-op */ }

		@RestGet(path="/f/{P}")
		public void f(@Path("P") Boolean b) { /* no-op */ }
	}

	@Test void d01_schemaFromParameter() {
		var s = getSwagger(E.class);
		var x = s.getParameterInfo("/a/{P}","get","path","P");

		assertBean(x, "in,name,type,required", "path,P,string,true");

		x = s.getParameterInfo("/b/{P}","put","path","P");
		assertBean(x, "in,name,type,required,schema{properties{f1{type}}}", "path,P,object,true,{{{string}}}");

		x = s.getParameterInfo("/c/{P}","post","path","P");
		assertBean(x, "in,name,type,required,items{type}", "path,P,array,true,{string}");

		x = s.getParameterInfo("/d/{P}","delete","path","P");
		assertBean(x, "in,name,type,required", "path,P,string,true");

		x = s.getParameterInfo("/e/{P}","get","path","P");
		assertBean(x, "in,name,type,required,format", "path,P,integer,true,int32");

		x = s.getParameterInfo("/f/{P}","get","path","P");
		assertBean(x, "in,name,type,required", "path,P,boolean,true");
	}
}