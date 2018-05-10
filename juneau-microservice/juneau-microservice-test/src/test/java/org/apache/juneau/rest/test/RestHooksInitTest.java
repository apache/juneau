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

import static org.apache.juneau.rest.testutils.TestUtils.*;

import java.util.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Validates the behavior of the @RestHook(INIT/POST_INIT/POST_INIT_CHILD_FIRST) annotations.
 */
public class RestHooksInitTest extends RestTestcase {

	private static String URL = "/testRestHooksInit";

	//====================================================================================================
	// @RestHook(INIT)
	//====================================================================================================
	@Test
	public void testInit() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/super/init").getResponse(List.class);
		e = "['super-1a','super-1b','super-1c','super-2a']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/init").getResponse(List.class);
		e = "['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/child/init").getResponse(List.class);
		e = "['super-1a','super-1b','child-1c','super-2a','child-2b']";
		assertObjectEquals(e, r);
	}

	//====================================================================================================
	// @RestHook(POST_INIT)
	//====================================================================================================
	@Test
	public void testPostInit() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/super/postInit").getResponse(List.class);
		e = "['super-1a','super-1b','super-1c','super-2a']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/postInit").getResponse(List.class);
		e = "['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/child/postInit").getResponse(List.class);
		e = "['super-1a','super-1b','child-1c','super-2a','child-2b']";
		assertObjectEquals(e, r);
	}

	//====================================================================================================
	// @RestHook(POST_INIT_CHILD_FIRST)
	//====================================================================================================
	@Test
	public void testPostInitChildFirst() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/super/postInitChildFirst").getResponse(List.class);
		e = "['super-1a','super-1b','super-1c','super-2a']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/postInitChildFirst").getResponse(List.class);
		e = "['sub-1a','sub-1b','sub-1c','super-2a','sub-2b']";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/child/postInitChildFirst").getResponse(List.class);
		e = "['super-1a','super-1b','child-1c','super-2a','child-2b']";
		assertObjectEquals(e, r);
	}

	//====================================================================================================
	// @RestHook(POST_INIT/POST_INIT_CHILD_FIRST) orders
	//====================================================================================================
	@Test
	public void testPostInitChildFirstOrder() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String e;
		Object r;

		r = client.doGet(URL + "/sub/postInitOrder").getResponse(String.class);
		e = "'CHILD'";
		assertObjectEquals(e, r);

		r = client.doGet(URL + "/sub/postInitChildFirstOrder").getResponse(String.class);
		e = "'PARENT'";
		assertObjectEquals(e, r);
	}
}
