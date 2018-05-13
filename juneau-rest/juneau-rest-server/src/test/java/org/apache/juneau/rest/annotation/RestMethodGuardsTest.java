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
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestMethod(guards).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestMethodGuardsTest {
	
	//=================================================================================================================
	// Overlapping guards
	//=================================================================================================================

	@RestResource
	public static class A {
		@RestMethod(name=GET, path="/overlappingOneGuard", guards=Test1Guard.class)
		public String a01() {
			return "OK1";
		}
		@RestMethod(name=GET, path="/overlappingTwoGuards", guards={Test1Guard.class,Test2Guard.class})
		public String a02() {
			return "OK2";
		}
		public static class Test1Guard extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQuery().getString("t1","").equals("1");
			}
		}
		public static class Test2Guard extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQuery().getString("t2","").equals("2");
			}
		}
	}
	static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01_overlappingOneGuard() throws Exception {
		a.request("GET", "/overlappingOneGuard?t1=1").execute().assertBody("OK1");
		a.request("GET", "/overlappingOneGuard?noTrace=true").execute().assertStatus(403).assertBodyContains("Access denied by guard");
	}

	@Test
	public void a01_overlappingTwoGuards() throws Exception {
		a.request("GET", "/overlappingTwoGuards?noTrace=true").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.request("GET", "/overlappingTwoGuards?noTrace=true&t1=1").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.request("GET", "/overlappingTwoGuards?noTrace=true&t2=2").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.request("GET", "/overlappingTwoGuards?t1=1&t2=2").execute().assertBody("OK2");
	}
}
