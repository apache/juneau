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

public class CT_TestPath {

	private static String URL = "/testPath";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r = null;

		r = client.doGet(URL).getResponse(String.class);
		assertEquals("/testPath", r);

		r = client.doGet(URL + "/testPath2").getResponse(String.class);
		assertEquals("/testPath/testPath2", r);

		r = client.doGet(URL + "/testPath2/testPath3").getResponse(String.class);
		assertEquals("/testPath/testPath2/testPath3", r);

		client.closeQuietly();
	}
}
