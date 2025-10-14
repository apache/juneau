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

import static org.apache.juneau.common.utils.Utils.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.matcher.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

class RestOp_Matchers_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Overlapping matchers
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestOp(path="/one", matchers=A1.class)
		public String a() {
			return "OK-1a";
		}
		@RestGet(path="/one", matchers=A2.class)
		public String b() {
			return "OK-1b";
		}
		@RestGet(path="/one")
		public String c() {
			return "OK-1c";
		}
		@RestOp(path="/two")
		public String d() {
			return "OK-2a";
		}
		@RestGet(path="/two", matchers={A1.class, A2.class})
		public String e() {
			return "OK-2b";
		}

		public static class A1 extends RestMatcher {
			@Override /* RestMatcher */
			public boolean matches(HttpServletRequest req) {
				return emptyIfNull(req.getQueryString()).contains("t1=1");
			}
		}
		public static class A2 extends RestMatcher {
			@Override /* RestMatcher */
			public boolean matches(HttpServletRequest req) {
				return emptyIfNull(req.getQueryString()).contains("t2=2");
			}
		}
	}

	@Test void a01_overlapping() throws Exception {
		var a = MockRestClient.build(A.class);
		a.get("/one?t1=1").run().assertContent("OK-1a");
		a.get("/one?t2=2").run().assertContent("OK-1b");
		a.get("/one").run().assertContent("OK-1c");
		a.get("/two?t1=1").run().assertContent("OK-2b");
		a.get("/two?t2=2").run().assertContent("OK-2b");
		a.get("/two?t1=1&t2=2").run().assertContent("OK-2b");
		a.get("/two?tx=x").run().assertContent("OK-2a");
	}
}