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

import static org.junit.Assert.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of URL path parts.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ThreadLocalObjectsTest {

	//=================================================================================================================
	// Thread-locals on top-level resource.
	//=================================================================================================================

	@SuppressWarnings("serial")
	@RestResource(path="/a")
	public static class A extends BasicRestServlet {
		@RestMethod
		public void getA01() throws Exception {
			getResponse().getWriter().append(getRequest().getQuery("foo"));
		}

		@RestHook(HookEvent.END_CALL)
		public void assertThreadsNotSet() {
			assertNull(getRequest());
			assertNull(getResponse());
		}
	}
	static MockRest a = MockRest.create(A.class);

	@Test
	public void a01() throws Exception {
		a.get("/a01?foo=bar").execute()
			.assertBodyContains("bar")
		;
	}

	//=================================================================================================================
	// Thread-locals on child resource.
	//=================================================================================================================

	@SuppressWarnings("serial")
	@RestResource(
		children={
			A.class
		}
	)
	public static class B extends BasicRestServletGroup {
		@RestHook(HookEvent.END_CALL)
		public void assertThreadsNotSet2() {
			assertNull(getRequest());
			assertNull(getResponse());
		}
	}
	static MockRest b = MockRest.create(B.class);

	@Test
	public void b01() throws Exception {
		b.get("/a/a01?foo=bar").execute()
			.assertBodyContains("bar")
		;
	}
}