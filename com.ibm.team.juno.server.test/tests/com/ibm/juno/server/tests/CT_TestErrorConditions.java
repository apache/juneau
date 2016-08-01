/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static com.ibm.juno.server.tests.TestUtils.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.json.*;


public class CT_TestErrorConditions {

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
				"Could not convert request body content to class type 'com.ibm.juno.server.test.TestErrorConditions$Test1' using parser 'com.ibm.juno.core.json.JsonParser'",
				"Unknown property 'f2' encountered while trying to parse into class 'com.ibm.juno.server.test.TestErrorConditions$Test1'");
		}

		try {
			client.doPut(url + "?noTrace=true", new ObjectMap("{f1:'foo', f2:'foo'}")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert request body content to class type 'com.ibm.juno.server.test.TestErrorConditions$Test1' using parser 'com.ibm.juno.core.json.JsonParser'",
				"Unknown property 'f2' encountered while trying to parse into class 'com.ibm.juno.server.test.TestErrorConditions$Test1'");
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
				"Could not convert request body content to class type 'com.ibm.juno.server.test.TestErrorConditions$Test2' using parser 'com.ibm.juno.core.json.JsonParser'.",
				"Error occurred trying to parse value for bean property 'f1' on class 'com.ibm.juno.server.test.TestErrorConditions$Test2'",
				"Could not convert string");
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
				"Error occurred trying to parse into class 'com.ibm.juno.server.test.TestErrorConditions$Test3a'");
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
				"Error occurred trying to parse into class 'OTHER-com.ibm.juno.server.test.TestErrorConditions$Test3b,notABeanReason=No properties detected on bean class'");
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
				"Class 'com.ibm.juno.server.test.TestErrorConditions$Test3b1' could not be instantiated",
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
				"Could not convert request body content to class type 'com.ibm.juno.server.test.TestErrorConditions$Test3c' using parser 'com.ibm.juno.core.json.JsonParser'.",
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
				"Could not convert PARAM 'p1' to type 'int' on method 'com.ibm.juno.server.test.TestErrorConditions.testSetParameterToInvalidTypes'");
		}

		try {
			client.doPut(url + "/foo?noTrace=true&p1=1", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert ATTR 'a1' to type 'int' on method 'com.ibm.juno.server.test.TestErrorConditions.testSetParameterToInvalidTypes'");
		}

		try {
			client.doPut(url + "/1?noTrace=true&p1=1", "").setHeader("h1", "foo").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Could not convert HEADER 'h1' to type 'int' on method 'com.ibm.juno.server.test.TestErrorConditions.testSetParameterToInvalidTypes'");
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
