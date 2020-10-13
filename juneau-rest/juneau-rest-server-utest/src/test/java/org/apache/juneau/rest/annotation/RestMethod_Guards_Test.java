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

import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestMethod_Guards_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Overlapping guards
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestMethod(guards=A1.class)
		public String a() {
			return "OK1";
		}
		@RestMethod(guards={A1.class,A2.class})
		public String b() {
			return "OK2";
		}
		public static class A1 extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQuery().getString("t1","").equals("1");
			}
		}
		public static class A2 extends RestGuard {
			@Override /* RestGuard */
			public boolean isRequestAllowed(RestRequest req) {
				return req.getQuery().getString("t2","").equals("2");
			}
		}
	}

	@Test
	public void a01_basic() throws Exception {
		RestClient a = MockRestClient.buildLax(A.class);
		a.get("/a?t1=1")
			.run()
			.assertBody().is("OK1");
		a.get("/a?noTrace=true")
			.run()
			.assertCode().is(403)
			.assertBody().contains("Access denied by guard");
		a.get("/b?noTrace=true")
			.run()
			.assertCode().is(403)
			.assertBody().contains("Access denied by guard");
		a.get("/b?noTrace=true&t1=1")
			.run()
			.assertCode().is(403)
			.assertBody().contains("Access denied by guard");
		a.get("/b?noTrace=true&t2=2")
			.run()
			.assertCode().is(403)
			.assertBody().contains("Access denied by guard");
		a.get("/b?t1=1&t2=2")
			.run()
			.assertBody().is("OK2");
	}
}
