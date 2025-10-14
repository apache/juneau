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

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.guard.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class RestOp_Guards_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Overlapping guards
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp(guards=A1.class)
		public String a1() {
			return "OK-a1";
		}
		@RestOp(guards={A1.class,A2.class})
		public String a2() {
			return "OK-a2";
		}
		@RestGet(guards=A1.class)
		public String b1() {
			return "OK-b1";
		}
		@RestGet(guards={A1.class,A2.class})
		public String b2() {
			return "OK-b2";
		}
		@RestPut(guards=A1.class)
		public String c1() {
			return "OK-c1";
		}
		@RestPut(guards={A1.class,A2.class})
		public String c2() {
			return "OK-c2";
		}
		@RestPost(guards=A1.class)
		public String d1() {
			return "OK-d1";
		}
		@RestPost(guards={A1.class,A2.class})
		public String d2() {
			return "OK-d2";
		}
		@RestDelete(guards=A1.class)
		public String e1() {
			return "OK-e1";
		}
		@RestDelete(guards={A1.class,A2.class})
		public String e2() {
			return "OK-e2";
		}
		public static class A1 extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQueryParam("t1").orElse("").equals("1");
			}
		}
		public static class A2 extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQueryParam("t2").orElse("").equals("2");
			}
		}
	}

	@Test void a01_basic() throws Exception {
		var a = MockRestClient.buildLax(A.class);

		a.get("/a1?t1=1")
			.run()
			.assertContent("OK-a1");
		a.get("/a1?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/a2?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/a2?noTrace=true&t1=1")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/a2?noTrace=true&t2=2")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/a2?t1=1&t2=2")
			.run()
			.assertContent("OK-a2");

		a.get("/b1?t1=1")
			.run()
			.assertContent("OK-b1");
		a.get("/b1?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/b2?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/b2?noTrace=true&t1=1")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/b2?noTrace=true&t2=2")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.get("/b2?t1=1&t2=2")
			.run()
			.assertContent("OK-b2");

		a.put("/c1?t1=1")
			.run()
			.assertContent("OK-c1");
		a.put("/c1?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.put("/c2?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.put("/c2?noTrace=true&t1=1")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.put("/c2?noTrace=true&t2=2")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.put("/c2?t1=1&t2=2")
			.run()
			.assertContent("OK-c2");

		a.post("/d1?t1=1")
			.run()
			.assertContent("OK-d1");
		a.post("/d1?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.post("/d2?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.post("/d2?noTrace=true&t1=1")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.post("/d2?noTrace=true&t2=2")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.post("/d2?t1=1&t2=2")
			.run()
			.assertContent("OK-d2");

		a.delete("/e1?t1=1")
			.run()
			.assertContent("OK-e1");
		a.delete("/e1?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.delete("/e2?noTrace=true")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.delete("/e2?noTrace=true&t1=1")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.delete("/e2?noTrace=true&t2=2")
			.run()
			.assertStatus(403)
			.assertContent().isContains("Access denied by guard");
		a.delete("/e2?t1=1&t2=2")
			.run()
			.assertContent("OK-e2");
	}
}