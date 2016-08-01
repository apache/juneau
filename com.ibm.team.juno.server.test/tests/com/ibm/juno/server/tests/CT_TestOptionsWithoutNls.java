/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.server.labels.*;

public class CT_TestOptionsWithoutNls {

	private static String URL = "/testOptionsWithoutNls";

	//====================================================================================================
	// Should get to the options page without errors
	//====================================================================================================
	@Test
	public void testOptions() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall r = client.doOptions(URL + "/testOptions");
		ResourceOptions o = r.getResponse(ResourceOptions.class);
		assertEquals("", o.getDescription());

		client.closeQuietly();
	}

	//====================================================================================================
	// Missing resource bundle should cause {!!x} string.
	//====================================================================================================
	@Test
	public void testMissingResourceBundle() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall r = client.doGet(URL + "/testMissingResourceBundle");
		String o = r.getResponse(String.class);
		assertEquals("{!!bad}", o);

		client.closeQuietly();
	}
}
