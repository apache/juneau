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

public class CT_TestProperties {

	private static String URL = "/testProperties";

	//====================================================================================================
	// Properties defined on method.
	//====================================================================================================
	@Test
	public void testPropertiesDefinedOnMethod() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r = client.doGet(URL + "/testPropertiesDefinedOnMethod").getResponseAsString();
		assertTrue(r.matches("A1=a1,A2=c,B1=b1,B2=c,C=c,R1a=.*/testProperties/testPropertiesDefinedOnMethod,R1b=.*/testProperties,R2=bar,R3=baz,R4=a1,R5=c,R6=c"));

		client.closeQuietly();
	}

	//====================================================================================================
	// Make sure attributes/parameters/headers are available through ctx.getProperties().
	//====================================================================================================
	@Test
	public void testProperties() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r = client.doGet(URL + "/testProperties/a1?P=p1").setHeader("H", "h1").getResponseAsString();
		assertEquals("A=a1,P=p1,H=h1", r);

		client.closeQuietly();
	}
}
