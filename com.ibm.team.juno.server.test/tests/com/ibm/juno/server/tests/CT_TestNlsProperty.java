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
import com.ibm.juno.core.plaintext.*;

public class CT_TestNlsProperty {

	private static String URL = "/testNlsProperty";

	//====================================================================================================
	// Test getting an NLS property defined on a class.
	//====================================================================================================
	@Test
	public void testInheritedFromClass() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doGet(URL + "/testInheritedFromClass").getResponseAsString();
		assertEquals("value1", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test getting an NLS property defined on a method.
	//====================================================================================================
	@Test
	public void testInheritedFromMethod() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doGet(URL + "/testInheritedFromMethod").getResponseAsString();
		assertEquals("value2", r);

		client.closeQuietly();
	}
}
