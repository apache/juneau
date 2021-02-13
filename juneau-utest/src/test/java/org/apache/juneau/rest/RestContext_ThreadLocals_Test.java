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
package org.apache.juneau.rest;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestContext_ThreadLocals_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Thread-locals on top-level resource.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("serial")
	@Rest(path="/a")
	public static class A extends BasicRestServlet {
		@RestGet
		public void a() throws Exception {
			getResponse().getWriter().append(getRequest().getQuery("foo"));
		}

		@RestHook(HookEvent.END_CALL)
		public void assertThreadsNotSet() {
			assertThrown(()->getRequest()).contains("No active request on current thread.");
			assertThrown(()->getResponse()).contains("No active request on current thread.");
		}
	}
	static MockRestClient a = MockRestClient.build(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/a?foo=bar").run()
			.assertBody().contains("bar")
		;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Thread-locals on child resource.
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings("serial")
	@Rest(
		children={
			A.class
		}
	)
	public static class B extends BasicRestServletGroup {
		@RestHook(HookEvent.END_CALL)
		public void assertThreadsNotSet2() {
			assertThrown(()->getRequest()).contains("No active request on current thread.");
			assertThrown(()->getResponse()).contains("No active request on current thread.");
		}
	}
	static MockRestClient b = MockRestClient.build(B.class);

	@Test
	public void b01() throws Exception {
		b.get("/a/a?foo=bar").run()
			.assertBody().contains("bar")
		;
	}
}