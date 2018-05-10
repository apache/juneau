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

import static org.apache.juneau.microservice.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Validates the behavior of the @RestHook(START/PRE/POST) annotations.
 */
public class RestHooksTest extends RestTestcase {

	private static String URL = "/testRestHooks";

	//====================================================================================================
	// @RestHook(START)
	//====================================================================================================
	@Test
	public void testStart() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/start").getResponse(Map.class);
		e = "{'1':'true','2':'true','3':'true','4':'true'}";
		assertObjectEquals(e, r);
	}

	//====================================================================================================
	// @RestHook(START)
	//====================================================================================================
	@Test
	public void testPre() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/pre").getResponse(Map.class);
		e = "{'1':'true','2':'true','3':'true','4':'true'}";
		assertObjectEquals(e, r);
	}

	//====================================================================================================
	// @RestHook(POST)
	//====================================================================================================
	@Test
	public void testPost() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;

		HttpResponse res = client.doGet(URL + "/post").getResponse();
		assertEquals("true", res.getFirstHeader("post1-called").getValue());
		assertEquals("true", res.getFirstHeader("post2-called").getValue());
		assertEquals("true", res.getFirstHeader("post3-called").getValue());
		assertEquals("true", res.getFirstHeader("post4-called").getValue());
	}
}
