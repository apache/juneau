/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;

public class CT_TestFilters {

	private static String URL = "/testFilters";

	//====================================================================================================
	// test1 - Test class filter overrides parent class filter
	// Should return "A2-1".
	//====================================================================================================
	@Test
	public void testClassFilterOverridesParentClassFilter() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		String url = URL + "/testClassFilterOverridesParentClassFilter";

		r = client.doGet(url).getResponse(String.class);
		assertEquals("A2-0", r);

		r = client.doPut(url, "A2-1").getResponse(String.class);
		assertEquals("A2-1", r);

		r = client.doPut(url + "/A2-2", "").getResponse(String.class);
		assertEquals("A2-2", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test method filter overrides class filter
	// Should return "A3-1".
	//====================================================================================================
	@Test
	public void testMethodFilterOverridesClassFilter() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		String url = URL + "/testMethodFilterOverridesClassFilter";

		r = client.doGet(url).getResponse(String.class);
		assertEquals("A3-0", r);

		r = client.doPut(url, "A3-1").getResponse(String.class);
		assertEquals("A3-1", r);

		r = client.doPut(url + "/A3-2", "").getResponse(String.class);
		assertEquals("A3-2", r);

		client.closeQuietly();
	}
}
