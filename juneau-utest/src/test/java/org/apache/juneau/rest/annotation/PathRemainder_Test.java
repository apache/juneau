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
		@RestOp(path="/a/*")
		public String a(@Path("/*") String remainder) {
			return ""+remainder;
		}
		@RestGet(path="/b/*")
		public String b(@Path("/*") String remainder) {
			return ""+remainder;
		}
		@RestPut(path="/c/*")
		public String c(@Path("/*") String remainder) {
			return ""+remainder;
		}
		@RestPost(path="/d/*")
		public String d(@Path("/*") String remainder) {
			return ""+remainder;
		}
		@RestDelete(path="/e/*")
		public String e(@Path("/*") String remainder) {
			return ""+remainder;
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.build(A.class);

		a.get("/a").run().assertContent("null");
		a.get("/a/").run().assertContent("");
		a.get("/a/foo").run().assertContent("foo");
		a.get("/a/foo/bar").run().assertContent("foo/bar");

		a.get("/b").run().assertContent("null");
		a.get("/b/").run().assertContent("");
		a.get("/b/foo").run().assertContent("foo");
		a.get("/b/foo/bar").run().assertContent("foo/bar");

		a.put("/c").run().assertContent("null");
		a.put("/c/").run().assertContent("");
		a.put("/c/foo").run().assertContent("foo");
		a.put("/c/foo/bar").run().assertContent("foo/bar");

		a.post("/d").run().assertContent("null");
		a.post("/d/").run().assertContent("");
		a.post("/d/foo").run().assertContent("foo");
		a.post("/d/foo/bar").run().assertContent("foo/bar");

		a.delete("/e").run().assertContent("null");
		a.delete("/e/").run().assertContent("");
		a.delete("/e/foo").run().assertContent("foo");
		a.delete("/e/foo/bar").run().assertContent("foo/bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional path remainder parameter.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class B {
		@RestGet(path="/a/*")
		public Object a(@Path("/*") Optional<Integer> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPut(path="/b/*")
		public Object b(@Path("/*") Optional<ABean> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestPost(path="/c/*")
		public Object c(@Path("/*") Optional<List<ABean>> f1) throws Exception {
			assertNotNull(f1);
			return f1;
		}
		@RestDelete(path="/d/*")
		public Object d(@Path("/*") List<Optional<ABean>> f1) throws Exception {
			return f1;
		}
	}

	@Test
	public void b01_optionalParam() throws Exception {
		RestClient b = MockRestClient.buildJson(B.class);
		b.get("/a/123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		b.put("/b/a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		b.post("/c/@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		b.delete("/d/@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
	}
}
