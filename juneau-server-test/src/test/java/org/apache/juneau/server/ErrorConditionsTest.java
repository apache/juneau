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
package org.apache.juneau.server;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.server.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.junit.*;


public class ErrorConditionsTest {

	private static String URL = "/testErrorConditions";
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
	// Test non-existent properties
	//====================================================================================================
	@Test
	public void testNonExistentBeanProperties() throws Exception {
		String url = URL + "/testNonExistentBeanProperties";

		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f2:'foo'}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert request body content to class type 'org.apache.juneau.server.ErrorConditionsResource$Test1' using parser 'org.apache.juneau.json.JsonParser'",
				"Unknown property 'f2' encountered while trying to parse into class 'org.apache.juneau.server.ErrorConditionsResource$Test1'");
		}

		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:'foo', f2:'foo'}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert request body content to class type 'org.apache.juneau.server.ErrorConditionsResource$Test1' using parser 'org.apache.juneau.json.JsonParser'",
				"Unknown property 'f2' encountered while trying to parse into class 'org.apache.juneau.server.ErrorConditionsResource$Test1'");
		}
	}

	//====================================================================================================
	// Test trying to set properties to wrong data type
	//====================================================================================================
	@Test
	public void testWrongDataType() throws Exception {
		String url = URL + "/testWrongDataType";
		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:'foo'}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert request body content to class type 'org.apache.juneau.server.ErrorConditionsResource$Test2' using parser 'org.apache.juneau.json.JsonParser'.",
				"Could not convert string 'foo' to class 'int'");
		}
	}

	//====================================================================================================
	// Test trying to parse into class with non-public no-arg constructor.
	//====================================================================================================
	@Test
	public void testParseIntoNonConstructableBean() throws Exception {
		String url = URL + "/testParseIntoNonConstructableBean";
		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:1}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Class 'org.apache.juneau.server.ErrorConditionsResource$Test3a' could not be instantiated.");
		}
	}

	//====================================================================================================
	// Test trying to parse into non-static inner class
	//====================================================================================================
	@Test
	public void testParseIntoNonStaticInnerClass() throws Exception {
		String url = URL + "/testParseIntoNonStaticInnerClass";
		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:1}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Class 'org.apache.juneau.server.ErrorConditionsResource$Test3b' could not be instantiated.  Reason: 'No properties detected on bean class'");
		}
	}

	//====================================================================================================
	// Test trying to parse into non-public inner class
	//====================================================================================================
	@Test
	public void testParseIntoNonPublicInnerClass() throws Exception {
		String url = URL + "/testParseIntoNonPublicInnerClass";
		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:1}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Class 'org.apache.juneau.server.ErrorConditionsResource$Test3b1' could not be instantiated",
				"Class is not public");
		}
	}

	//====================================================================================================
	// Test exception thrown during bean construction.
	//====================================================================================================
	@Test
	public void testThrownConstructorException() throws Exception {
		String url = URL + "/testThrownConstructorException";
		try {
			client.doPut(url + "?noTrace=true", "'foo'").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert request body content to class type 'org.apache.juneau.server.ErrorConditionsResource$Test3c' using parser 'org.apache.juneau.json.JsonParser'.",
				"Caused by (RuntimeException): Test error");
		}
	}

	//====================================================================================================
	// Test trying to set parameters to invalid types.
	//====================================================================================================
	@Test
	public void testSetParameterToInvalidTypes() throws Exception {
		String url = URL + "/testSetParameterToInvalidTypes";
		try {
			client.doPut(url + "/1?noTrace=true&p1=foo", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert PARAM 'p1' to type 'int' on method 'org.apache.juneau.server.ErrorConditionsResource.testSetParameterToInvalidTypes'");
		}

		try {
			client.doPut(url + "/foo?noTrace=true&p1=1", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert ATTR 'a1' to type 'int' on method 'org.apache.juneau.server.ErrorConditionsResource.testSetParameterToInvalidTypes'");
		}

		try {
			client.doPut(url + "/1?noTrace=true&p1=1", "").setHeader("h1", "foo").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert HEADER 'h1' to type 'int' on method 'org.apache.juneau.server.ErrorConditionsResource.testSetParameterToInvalidTypes'");
		}
	}

	//====================================================================================================
	// Test SC_NOT_FOUND & SC_METHOD_NOT_ALLOWED
	//====================================================================================================
	@Test
	public void test404and405() throws Exception {
		String url = URL + "/test404and405";
		try {
			client.doGet(URL + "/testNonExistent?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_FOUND,
				"Method 'GET' not found on resource with matching pattern on path '/testNonExistent'");
		}

		try {
			client.doPut(url + "?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_FOUND,
				"Method 'PUT' not found on resource with matching pattern on path '/test404and405'");
		}

		try {
			client.doPost(url + "?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_METHOD_NOT_ALLOWED,
				"Method 'POST' not found on resource.");
		}
	}

	//====================================================================================================
	// Test SC_PRECONDITION_FAILED
	//====================================================================================================
	@Test
	public void test412() throws Exception {
		String url = URL + "/test412";
		try {
			client.doGet(url + "?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_PRECONDITION_FAILED,
				"Method 'GET' not found on resource on path '/test412' with matching matcher.");
		}
	}
}
