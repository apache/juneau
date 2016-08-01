/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static com.ibm.juno.server.tests.TestUtils.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;

/**
 * Validates that resource bundles can be defined on both parent and child classes.
 */
public class CT_TestMessages {

	//====================================================================================================
	// Return contents of resource bundle.
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void test() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class,JsonParser.class);

		// Parent resource should just pick up values from its bundle.
		TreeMap r = client.doGet("/testMessages/test").getResponse(TreeMap.class);
		assertObjectEquals("{key1:'value1a',key2:'value2a'}", r);

		// Child resource should pick up values from both parent and child,
		// ordered child before parent.
		r = client.doGet("/testMessages2/test").getResponse(TreeMap.class);
		assertObjectEquals("{key1:'value1a',key2:'value2b',key3:'value3b'}", r);

		client.closeQuietly();
	}
}
