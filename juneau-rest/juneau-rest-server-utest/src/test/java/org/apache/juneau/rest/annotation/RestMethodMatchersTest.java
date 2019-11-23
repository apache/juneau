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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(matchers).
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestMethodMatchersTest {

	//=================================================================================================================
	// Overlapping matchers
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(name=GET, path="/one", matchers=M1.class)
		public String a01a() {
			return "OK-1a";
		}
		@RestMethod(name=GET, path="/one", matchers=M2.class)
		public String a01b() {
			return "OK-1b";
		}
		@RestMethod(name=GET, path="/one")
		public String a01c() {
			return "OK-1c";
		}
		@RestMethod(name=GET, path="/two")
		public String a02a() {
			return "OK-2a";
		}
		@RestMethod(name=GET, path="/two", matchers={M1.class, M2.class})
		public String a02b() {
			return "OK-2b";
		}

		public static class M1 extends RestMatcher {
			@Override /* RestMatcher */
			public boolean matches(RestRequest req) {
				return req.getQuery().getString("t1","").equals("1");
			}
		}
		public static class M2 extends RestMatcher {
			@Override /* RestMatcher */
			public boolean matches(RestRequest req) {
				return req.getQuery().getString("t2","").equals("2");
			}
		}
	}
	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01() throws Exception {
		a.get("/one?t1=1").execute().assertBody("OK-1a");
		a.get("/one?t2=2").execute().assertBody("OK-1b");
		a.get("/one").execute().assertBody("OK-1c");
	}
	@Test
	public void a02() throws Exception {
		a.get("/two?t1=1").execute().assertBody("OK-2b");
		a.get("/two?t2=2").execute().assertBody("OK-2b");
		a.get("/two?t1=1&t2=2").execute().assertBody("OK-2b");
		a.get("/two?tx=x").execute().assertBody("OK-2a");
	}
}
