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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class SerializersTest extends RestTestcase {

	private static String URL = "/testSerializers";
	private static boolean debug = false;
	private RestClient client = TestMicroservice.DEFAULT_CLIENT;


	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@Test
	public void testSerializerOnClass() throws Exception {
		String url = URL + "/testSerializerOnClass";

		String r = client.doGet(url).accept("text/a").getResponseAsString();
		assertEquals("text/a - test1", r);

		try {
			client.doGet(url + "?noTrace=true").accept("text/b").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/b'",
				"Supported media-types: ['text/a',");
		}

		r = client.doGet(url).accept("text/json").getResponseAsString();
		assertEquals("\"test1\"", r);
	}

	//====================================================================================================
	// Serializer defined on method.
	//====================================================================================================
	@Test
	public void testSerializerOnMethod() throws Exception {
		String url = URL + "/testSerializerOnMethod";

		try {
			client.doGet(url + "?noTrace=true").accept("text/a").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/a'",
				"Supported media-types: ['text/b']"
			);
		}

		try {
			client.doGet(url + "?noTrace=true").accept("text/json").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/json'",
				"Supported media-types: ['text/b']"
			);
		}
	}

	//====================================================================================================
	// Serializer overridden on method.
	//====================================================================================================
	@Test
	public void testSerializerOverriddenOnMethod() throws Exception {
		String url = URL + "/testSerializerOverriddenOnMethod";

		String r = client.doGet(url).accept("text/a").getResponseAsString();
		assertEquals("text/c - test3", r);

		r = client.doGet(url).accept("text/b").getResponseAsString();
		assertEquals("text/b - test3", r);

		r = client.doGet(url).accept("text/json").getResponseAsString();
		assertEquals("\"test3\"", r);
	}

	//====================================================================================================
	// Serializer with different Accept than Content-Type.
	//====================================================================================================
	@Test
	public void testSerializerWithDifferentMediaTypes() throws Exception {
		String url = URL + "/testSerializerWithDifferentMediaTypes";

		String r = client.doGet(url).accept("text/a").getResponseAsString();
		assertEquals("text/d - test4", r);

		r = client.doGet(url).accept("text/d").getResponseAsString();
		assertEquals("text/d - test4", r);

		r = client.doGet(url).accept("text/json").getResponseAsString();
		assertEquals("\"test4\"", r);
	}

	//====================================================================================================
	// Check for valid 406 error response.
	//====================================================================================================
	@Test
	public void test406() throws Exception {
		String url = URL + "/test406";

		try {
			client.doGet(url + "?noTrace=true").accept("text/bad").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/bad'",
				"Supported media-types: ['text/a");
		}
	}
}
