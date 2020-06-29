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

import java.io.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_RequestAnnotation_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body",IOUtils.read(r),
				"header",h,
				"query",q,
				"path",p
			).toString();
		}
	}

	@Request
	public static class A1 {
		@Body
		public String getBody() {
			return "foo";
		}
		@Header("X")
		public String getHeader() {
			return "x";
		}
		@Query("x")
		public String getQuery() {
			return "x";
		}
		@Path("x")
		public String getPath() {
			return "x";
		}
	}

	@Remote
	public static interface A2 {
		@RemoteMethod(path="/{x}") String post(A1 req);
	}

	@Test
	public void a01_basic() throws Exception {
		A2 x = remote(A.class,A2.class);
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}",x.post(new A1()));
		assertEquals("{body:'',header:null,query:null,path:'{x}'}",x.post(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation on parent
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body",IOUtils.read(r),
				"header",h,
				"query",q,
				"path",p
			).toString();
		}
	}

	@Request
	public abstract static class B1 {
		@Body public abstract String getBody();
		@Header("X") public abstract String getHeader();
		@Query("x") public abstract String getQuery();
		@Path("x") public abstract String getPath();
	}

	public static class B2 extends B1 {
		@Override
		public String getBody() {
			return "foo";
		}
		@Override
		public String getHeader() {
			return "x";
		}
		@Override
		public String getQuery() {
			return "x";
		}
		@Override
		public String getPath() {
			return "x";
		}
	}

	@Remote
	public static interface B3 {
		@RemoteMethod(path="/{x}") String post(B1 req);
	}

	@Test
	public void b01_annotationOnParent() throws Exception {
		B3 x = remote(B.class,B3.class);
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}",x.post(new B2()));
		assertEquals("{body:'',header:null,query:null,path:'{x}'}",x.post(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation on interface
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body",IOUtils.read(r),
				"header",h,
				"query",q,
				"path",p
			).toString();
		}
	}

	@Request
	public interface C1 {
		@Body String getBody();
		@Header("X") String getHeader();
		@Query("x") String getQuery();
		@Path("x") String getPath();
	}

	public static class C2 implements C1 {
		@Override
		public String getBody() {
			return "foo";
		}
		@Override
		public String getHeader() {
			return "x";
		}
		@Override
		public String getQuery() {
			return "x";
		}
		@Override
		public String getPath() {
			return "x";
		}
	}

	@Remote
	public static interface C3 {
		@RemoteMethod(path="/{x}") String post(C1 req);
	}

	@Test
	public void c01_annotationOnInterface() throws Exception {
		C3 x = remote(C.class,C3.class);
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}",x.post(new C2()));
		assertEquals("{body:'',header:null,query:null,path:'{x}'}",x.post(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation on parameter
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body",IOUtils.read(r),
				"header",h,
				"query",q,
				"path",p
			).toString();
		}
	}

	public static class D1 {
		@Body
		public String getBody() {
			return "foo";
		}
		@Header("X")
		public String getHeader() {
			return "x";
		}
		@Query("x")
		public String getQuery() {
			return "x";
		}
		@Path("x")
		public String getPath() {
			return "x";
		}
	}

	@Remote
	public static interface D2 {
		@RemoteMethod(path="/{x}") String post(@Request D1 req);
	}

	@Test
	public void d01_annotationOnParameter() throws Exception {
		D2 x = remote(D.class,D2.class);
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}",x.post(new D1()));
		assertEquals("{body:'',header:null,query:null,path:'{x}'}",x.post(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// @Request(partSerializer)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body",IOUtils.read(r),
				"header",h,
				"query",q,
				"path",p
			).toString();
		}
	}

	@Request(partSerializer=XPartSerializer.class)
	public static class E1 {
		@Body
		public String getBody() {
			return "foo";
		}
		@Header("X")
		public String getHeader() {
			return "x";
		}
		@Query("x")
		public String getQuery() {
			return "x";
		}
		@Path("x")
		public String getPath() {
			return "x";
		}
	}

	@Remote
	public static interface E2 {
		@RemoteMethod(path="/{x}") String post(E1 req);
	}

	@Test
	public void e01_partSerializer() throws Exception {
		E2 x = remote(E.class,E2.class);
		assertEquals("{body:'foo',header:'xxx',query:'xxx',path:'xxx'}",x.post(new E1()));
		assertEquals("{body:'',header:null,query:null,path:'{x}'}",x.post(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static <T> T remote(Class<?> rest, Class<T> t) {
		return MockRestClient.build(rest).getRemote(t);
	}
}
