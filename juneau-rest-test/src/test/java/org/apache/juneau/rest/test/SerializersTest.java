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

import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class SerializersTest extends RestTestcase {

	private static String URL = "/testSerializers";
	private static boolean debug = false;
	private static RestClient client;

	@BeforeClass
	public static void beforeClass() {
		client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
	}

	@AfterClass
	public static void afterClass() {
		client.closeQuietly();
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@Test
	public void testSerializerOnClass() throws Exception {
		String url = URL + "/testSerializerOnClass";

		client.setAccept("text/a");
		String r = client.doGet(url).getResponseAsString();
		assertEquals("text/a - test1", r);

		try {
			client.setAccept("text/b");
			client.doGet(url + "?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/b'",
				"Supported media-types: [text/a, ");
		}

		client.setAccept("text/json");
		r = client.doGet(url).getResponseAsString();
		assertEquals("\"test1\"", r);
	}

	//====================================================================================================
	// Serializer defined on method.
	//====================================================================================================
	@Test
	public void testSerializerOnMethod() throws Exception {
		String url = URL + "/testSerializerOnMethod";

		try {
			client.setAccept("text/a");
			client.doGet(url + "?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/a'",
				"Supported media-types: [text/b]"
			);
		}

		try {
			client.setAccept("text/json");
			client.doGet(url + "?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/json'",
				"Supported media-types: [text/b]"
			);
		}
	}

	//====================================================================================================
	// Serializer overridden on method.
	//====================================================================================================
	@Test
	public void testSerializerOverriddenOnMethod() throws Exception {
		String url = URL + "/testSerializerOverriddenOnMethod";

		client.setAccept("text/a");
		String r = client.doGet(url).getResponseAsString();
		assertEquals("text/c - test3", r);

		client.setAccept("text/b");
		r = client.doGet(url).getResponseAsString();
		assertEquals("text/b - test3", r);

		client.setAccept("text/json");
		r = client.doGet(url).getResponseAsString();
		assertEquals("\"test3\"", r);
	}

	//====================================================================================================
	// Serializer with different Accept than Content-Type.
	//====================================================================================================
	@Test
	public void testSerializerWithDifferentMediaTypes() throws Exception {
		String url = URL + "/testSerializerWithDifferentMediaTypes";

		client.setAccept("text/a");
		String r = client.doGet(url).getResponseAsString();
		assertEquals("text/d - test4", r);

		client.setAccept("text/d");
		r = client.doGet(url).getResponseAsString();
		assertEquals("text/d - test4", r);

		client.setAccept("text/json");
		r = client.doGet(url).getResponseAsString();
		assertEquals("\"test4\"", r);
	}

	//====================================================================================================
	// Check for valid 406 error response.
	//====================================================================================================
	@Test
	public void test406() throws Exception {
		String url = URL + "/test406";

		try {
			client.setAccept("text/bad");
			client.doGet(url + "?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/bad'",
				"Supported media-types: [text/a");
		}
	}
}
