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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class PathRemainder_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Simple tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A  {
		@RestMethod(name=GET, path="/*")
		public String a(@Path("/*") String remainder) {
			return remainder;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.get("/").run().assertBody().is("");
		a.get("/foo").run().assertBody().is("foo");
		a.get("/foo/bar").run().assertBody().is("foo/bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional path remainder parameter.
	//------------------------------------------------------------------------------------------------------------------

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

	@Test
	public void b01_optionalParam() throws Exception {
		RestClient b = MockRestClient.buildJson(B.class);
		b.get("/a/123")
			.run()
			.assertCode().is(200)
			.assertBody().is("123");
		b.get("/b/a=1,b=foo")
			.run()
			.assertCode().is(200)
			.assertBody().is("{a:1,b:'foo'}");
		b.get("/c/@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
		b.get("/d/@((a=1,b=foo))")
			.run()
			.assertCode().is(200)
			.assertBody().is("[{a:1,b:'foo'}]");
	}
}
