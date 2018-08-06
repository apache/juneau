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
		@RestMethod(guards=Test1Guard.class)
		public String a01() {
			return "OK1";
		}
		@RestMethod(guards={Test1Guard.class,Test2Guard.class})
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
		a.get("/a01?t1=1").execute().assertBody("OK1");
		a.get("/a01?noTrace=true").execute().assertStatus(403).assertBodyContains("Access denied by guard");
	}

	@Test
	public void a02_overlappingTwoGuards() throws Exception {
		a.get("/a02?noTrace=true").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.get("/a02?noTrace=true&t1=1").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.get("/a02?noTrace=true&t2=2").execute().assertStatus(403).assertBodyContains("Access denied by guard");
		a.get("/a02?t1=1&t2=2").execute().assertBody("OK2");
	}
}
