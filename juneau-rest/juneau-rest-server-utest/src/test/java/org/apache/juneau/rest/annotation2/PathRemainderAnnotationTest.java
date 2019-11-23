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
package org.apache.juneau.rest.annotation2;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests related to @PathREmainder annotation.
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PathRemainderAnnotationTest {

	//=================================================================================================================
	// Simple tests
	//=================================================================================================================

	@Rest
	public static class A  {
		@RestMethod(name=GET, path="/*")
		public String b(@Path("/*") String remainder) {
			return remainder;
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_withoutRemainder() throws Exception {
		a.get("/").execute().assertBody("");
	}
	@Test
	public void a02_withRemainder() throws Exception {
		a.get("/foo").execute().assertBody("foo");
	}
	@Test
	public void a03_withRemainder2() throws Exception {
		a.get("/foo/bar").execute().assertBody("foo/bar");
	}

	//=================================================================================================================
	// Optional path remainer parameter.
	//=================================================================================================================

	@Rest(serializers=SimpleJsonSerializer.class)
	public static class B {
		@RestMethod(name=GET,path="/a/*")
		public Object a(@Path("/*") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/b/*")
		public Object b(@Path("/*") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/c/*")
		public Object c(@Path("/*") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestMethod(name=GET,path="/d/*")
		public Object d(@Path("/*") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}
	static MockRest b = MockRest.create(B.class).json().build();

	@Test
	public void b01_optionalParam_integer() throws Exception {
		b.get("/a/123").execute().assertStatus(200).assertBody("123");
	}

	@Test
	public void b02_optionalParam_bean() throws Exception {
		b.get("/b/(a=1,b=foo)").execute().assertStatus(200).assertBody("{a:1,b:'foo'}");
	}

	@Test
	public void b03_optionalParam_listOfBeans() throws Exception {
		b.get("/c/@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
	}

	@Test
	public void b04_optionalParam_listOfOptionals() throws Exception {
		b.get("/d/@((a=1,b=foo))").execute().assertStatus(200).assertBody("[{a:1,b:'foo'}]");
	}
}
