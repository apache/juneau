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
 * Tests that validate the behavior of @RestResource(path).
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourcePathTest {

	//=================================================================================================================
	// Nested children.
	//=================================================================================================================

	@RestResource(path="/p0", children={A01.class})
	public static class A  {
		@RestMethod(name=GET, path="/")
		public String doGet(RestContext c) {
			return "A-" + c.getPath();
		}
	}
	@RestResource(path="/p1", children={A02.class})
	public static class A01 {
		@RestMethod(name=GET, path="/")
		public String doGet(RestContext c) {
			return "A01-" + c.getPath();
		}
	}
	public static class A02a  {
		@RestMethod(name=GET, path="/")
		public String doGet(RestContext c) {
			return "A02a-" + c.getPath();
		}
	}
	@RestResource(path="/p2")
	public static class A02 extends A02a {}

	static MockRest a = MockRest.build(A.class, null);

	@Test
	public void a01_nestedChildren() throws Exception {
		// Since we're not running from a servlet container, we access A directly with no path.
		// However, the path is still reflected in RestContext.getPath().
		a.get("/").execute().assertBody("A-p0");
		a.get("/p1").execute().assertBody("A01-p0/p1");
		a.get("/p1/p2").execute().assertBody("A02a-p0/p1/p2");
	}
}
