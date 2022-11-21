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
package org.apache.juneau.http.remote;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.annotation.Response;
import org.apache.juneau.marshaller.*;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Remote_ResponseAnnotation_Test {

	public static class ABean {
		public int f;
		static ABean get() {
			ABean x = new ABean();
			x.f = 1;
			return x;
		}
		@Override
		public String toString() {
			return Json5.of(this);
		}
	}

	private static ABean bean = ABean.get();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp
		public String get(RestResponse res) {
			res.setHeader("X","x");
			res.setStatus(201);
			return "foo";
		}
	}

	@Response
	public interface A1a {
		@Content Reader getContent();
		@Header("X") String getHeader();
		@StatusCode int getStatus();
	}

	@Remote
	public static interface A1b {
		@RemoteOp A1a get();
	}

	@Test
	public void a01_basic() throws Exception {
		A1a x = remote(A.class,A1b.class).get();
		assertEquals("foo",read(x.getContent()));
		assertEquals("x",x.getHeader());
		assertEquals(201,x.getStatus());
	}

	@Response
	public interface A2a {
		@Content Reader getContent();
	}

	@Remote
	public static interface A2b {
		@RemoteOp A2a get();
	}

	@Test
	public void a02_unannotatedMethod() throws Exception {
		A2a x = remote(A.class,A2b.class).get();
		assertEquals("foo",read(x.getContent()));
	}

	@Rest
	public static class A3 implements BasicJsonConfig {
		@RestOp
		public ABean get() {
			return bean;
		}
	}

	@Response
	public interface A3a {
		@Content ABean getContent();
	}

	@Remote
	public static interface A3b {
		@RemoteOp A3a get();
	}

	@Test
	public void a03_beanBody() throws Exception {
		A3a x = client(A3.class).json().build().getRemote(A3b.class).get();
		assertEquals("{f:1}",x.getContent().toString());

		A3a x2 = client(A3.class).build().getRemote(A3b.class).get();
		assertThrown(()->x2.getContent()).asMessages().isContains("Unsupported media-type in request header 'Content-Type': 'application/json'");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static <T> T remote(Class<?> rest, Class<T> t) {
		return MockRestClient.create(rest).build().getRemote(t);
	}

	private static <T> MockRestClient.Builder client(Class<?> rest) {
		return MockRestClient.create(rest);
	}
}
