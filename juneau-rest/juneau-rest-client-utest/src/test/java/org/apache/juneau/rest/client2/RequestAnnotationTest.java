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
import org.junit.runners.*;

/**
 * Tests the @Request annotation.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RequestAnnotationTest {

	//=================================================================================================================
	// Basic tests
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body", IOUtils.read(r),
				"header", h,
				"query", q,
				"path", p
			).toString();
		}
	}
	@Request
	public static class ARequest {
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
	public static interface AR {
		@RemoteMethod(path="/{x}") String post(ARequest req);
	}

	private static AR ar = MockRemote.create(AR.class, A.class).build();

	@Test
	public void a01_basic() throws Exception {
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}", ar.post(new ARequest()));
	}
	@Test
	public void a02_basic_nullValue() throws Exception {
		assertEquals("{body:'',header:null,query:null,path:'{x}'}", ar.post(null));
	}

	//=================================================================================================================
	// Annotation on parent
	//=================================================================================================================

	@Rest
	public static class B {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body", IOUtils.read(r),
				"header", h,
				"query", q,
				"path", p
			).toString();
		}
	}

	@Request
	public abstract static class BRequest {
		@Body
		public abstract String getBody();
		@Header("X")
		public abstract String getHeader();
		@Query("x")
		public abstract String getQuery();
		@Path("x")
		public abstract String getPath();
	}

	public static class BRequestImpl extends BRequest {
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
	public static interface BR {
		@RemoteMethod(path="/{x}") String post(BRequest req);
	}

	private static BR br = MockRemote.build(BR.class, B.class);

	@Test
	public void b01_annotationOnParent() throws Exception {
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}", br.post(new BRequestImpl()));
	}
	@Test
	public void b02_annotationOnParent_nullValue() throws Exception {
		assertEquals("{body:'',header:null,query:null,path:'{x}'}", br.post(null));
	}

	//=================================================================================================================
	// Annotation on interface
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body", IOUtils.read(r),
				"header", h,
				"query", q,
				"path", p
			).toString();
		}
	}

	@Request
	public interface CRequest {
		@Body
		String getBody();
		@Header("X")
		String getHeader();
		@Query("x")
		String getQuery();
		@Path("x")
		String getPath();
	}

	public static class CRequestImpl implements CRequest {
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
	public static interface CR {
		@RemoteMethod(path="/{x}") String post(CRequest req);
	}

	private static CR cr = MockRemote.build(CR.class, C.class);

	@Test
	public void c01_annotationOnInterface() throws Exception {
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}", cr.post(new CRequestImpl()));
	}
	@Test
	public void c02_annotationOnInterface_nullValue() throws Exception {
		assertEquals("{body:'',header:null,query:null,path:'{x}'}", cr.post(null));
	}

	//=================================================================================================================
	// Annotation on parameter
	//=================================================================================================================

	@Rest
	public static class D {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body", IOUtils.read(r),
				"header", h,
				"query", q,
				"path", p
			).toString();
		}
	}

	public static class DRequest {
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
	public static interface DR {
		@RemoteMethod(path="/{x}") String post(@Request DRequest req);
	}

	private static DR dr = MockRemote.build(DR.class, D.class);

	@Test
	public void d01_annotationOnParameter() throws Exception {
		assertEquals("{body:'foo',header:'x',query:'x',path:'x'}", dr.post(new DRequest()));
	}
	@Test
	public void d02_annotationOnParameter_nullValue() throws Exception {
		assertEquals("{body:'',header:null,query:null,path:'{x}'}", dr.post(null));
	}

	//=================================================================================================================
	// @Request(partSerializer)
	//=================================================================================================================

	@Rest
	public static class E {
		@RestMethod(path="/{x}")
		public String post(@Body Reader r, @Header("X") String h, @Query("x") String q, @Path("x") String p) throws Exception {
			return OMap.of(
				"body", IOUtils.read(r),
				"header", h,
				"query", q,
				"path", p
			).toString();
		}
	}

	@Request(partSerializer=XPartSerializer.class)
	public static class ERequest {
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
	public static interface ER {
		@RemoteMethod(path="/{x}") String post(ERequest req);
	}

	private static ER er = MockRemote.build(ER.class, E.class);

	@Test
	public void e01_partSerializer() throws Exception {
		assertEquals("{body:'foo',header:'xxx',query:'xxx',path:'xxx'}", er.post(new ERequest()));
	}
	@Test
	public void a02_partSerializer_nullValue() throws Exception {
		assertEquals("{body:'',header:null,query:null,path:'{x}'}", er.post(null));
	}

}
