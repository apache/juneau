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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Tests the <code>@RestMethod.path()</code> annotation.
 */
public class PathVariableTest extends RestTestcase {

	private static String URL = "/testPathVariables";
	RestClient client = TestMicroservice.DEFAULT_CLIENT;

	@Test
	public void test1() throws Exception {
		String r = client.doGet(URL + "/test1/xxx/foo/123/bar/true").getResponseAsString();
		assertEquals("x=xxx,y=123,z=true", r);
	}

	@Test
	public void test2() throws Exception {
		String r = client.doGet(URL + "/test2/true/foo/123/bar/xxx").getResponseAsString();
		assertEquals("x=xxx,y=123,z=true", r);
	}

	@Test
	public void test3() throws Exception {
		String r = client.doGet(URL + "/test3/xxx/foo/123/bar/true").getResponseAsString();
		assertEquals("x=xxx,y=123,z=true", r);
	}

	@Test
	public void test4() throws Exception {
		String r = client.doGet(URL + "/test4/true/foo/123/bar/xxx").getResponseAsString();
		assertEquals("x=xxx,y=123,z=true", r);
	}
}
