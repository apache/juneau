/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class PathRemainder_Test extends TestBase {

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

	@Test void a01_basic() throws Exception {
		var a = MockRestClient.build(A.class);

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
		public Object a(@Path("/*") Optional<Integer> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestPut(path="/b/*")
		public Object b(@Path("/*") Optional<ABean> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestPost(path="/c/*")
		public Object c(@Path("/*") Optional<List<ABean>> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestDelete(path="/d/*")
		public Object d(@Path("/*") List<Optional<ABean>> f1) {
			return f1;
		}
	}

	@Test void b01_optionalParam() throws Exception {
		var b = MockRestClient.buildJson(B.class);
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

	//------------------------------------------------------------------------------------------------------------------
	// @PathRemainder annotation tests
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestOp(path="/a/*")
		public String a(@PathRemainder String remainder) {
			return ""+remainder;
		}
		@RestGet(path="/b/*")
		public String b(@PathRemainder String remainder) {
			return ""+remainder;
		}
		@RestPut(path="/c/*")
		public String c(@PathRemainder String remainder) {
			return ""+remainder;
		}
		@RestPost(path="/d/*")
		public String d(@PathRemainder String remainder) {
			return ""+remainder;
		}
		@RestDelete(path="/e/*")
		public String e(@PathRemainder String remainder) {
			return ""+remainder;
		}
	}

	@Test void c01_pathRemainderAnnotation() throws Exception {
		var c = MockRestClient.build(C.class);

		// Test that @PathRemainder works identically to @Path("/*")
		c.get("/a").run().assertContent("null");
		c.get("/a/").run().assertContent("");
		c.get("/a/foo").run().assertContent("foo");
		c.get("/a/foo/bar").run().assertContent("foo/bar");

		c.get("/b").run().assertContent("null");
		c.get("/b/").run().assertContent("");
		c.get("/b/foo").run().assertContent("foo");
		c.get("/b/foo/bar").run().assertContent("foo/bar");

		c.put("/c").run().assertContent("null");
		c.put("/c/").run().assertContent("");
		c.put("/c/foo").run().assertContent("foo");
		c.put("/c/foo/bar").run().assertContent("foo/bar");

		c.post("/d").run().assertContent("null");
		c.post("/d/").run().assertContent("");
		c.post("/d/foo").run().assertContent("foo");
		c.post("/d/foo/bar").run().assertContent("foo/bar");

		c.delete("/e").run().assertContent("null");
		c.delete("/e/").run().assertContent("");
		c.delete("/e/foo").run().assertContent("foo");
		c.delete("/e/foo/bar").run().assertContent("foo/bar");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @PathRemainder with Optional and complex types
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=Json5Serializer.class)
	public static class D {
		@RestGet(path="/a/*")
		public Object a(@PathRemainder Optional<Integer> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestPut(path="/b/*")
		public Object b(@PathRemainder Optional<ABean> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestPost(path="/c/*")
		public Object c(@PathRemainder Optional<List<ABean>> f1) {
			assertNotNull(f1);
			return f1;
		}
		@RestDelete(path="/d/*")
		public Object d(@PathRemainder List<Optional<ABean>> f1) {
			return f1;
		}
	}

	@Test void d01_pathRemainderWithOptional() throws Exception {
		var d = MockRestClient.buildJson(D.class);
		d.get("/a/123")
			.run()
			.assertStatus(200)
			.assertContent("123");
		d.put("/b/a=1,b=foo")
			.run()
			.assertStatus(200)
			.assertContent("{a:1,b:'foo'}");
		d.post("/c/@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
		d.delete("/d/@((a=1,b=foo))")
			.run()
			.assertStatus(200)
			.assertContent("[{a:1,b:'foo'}]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @PathRemainder with mixed path parameters
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {
		@RestGet(path="/a/{foo}/{bar}/*")
		public String a(@Path("foo") String foo, @Path("bar") int bar, @PathRemainder String remainder) {
			return "foo="+foo+",bar="+bar+",remainder="+remainder;
		}
		@RestPost(path="/b/{id}/*")
		public String b(@Path("id") String id, @PathRemainder String remainder) {
			return "id="+id+",remainder="+remainder;
		}
	}

	@Test void e01_pathRemainderWithOtherPathParams() throws Exception {
		var e = MockRestClient.build(E.class);
		e.get("/a/x/123/extra/path")
			.run()
			.assertContent("foo=x,bar=123,remainder=extra/path");
		e.get("/a/hello/456")
			.run()
			.assertContent("foo=hello,bar=456,remainder=null");
		e.post("/b/myId/more/stuff")
			.run()
			.assertContent("id=myId,remainder=more/stuff");
	}

	//------------------------------------------------------------------------------------------------------------------
	// @PathRemainder with default values
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet(path="/a/*")
		public String a(@PathRemainder(def="defaultValue") String remainder) {
			return ""+remainder;
		}
	}

	@Test void f01_pathRemainderWithDefault() throws Exception {
		var f = MockRestClient.build(F.class);
		f.get("/a").run().assertContent("defaultValue");
		f.get("/a/").run().assertContent("");
		f.get("/a/custom").run().assertContent("custom");
	}

}