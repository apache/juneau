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

public class CT_TestBeanContextProperties {

	boolean debug = false;

	//====================================================================================================
	// Validate that filters defined on class filter to underlying bean context.
	//====================================================================================================
	@Test
	public void testClassFilters() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class, JsonParser.class);
		String r;
		r = client.doGet("/testBeanContextProperties/testClassFilters/2001-07-04T15:30:45Z?d2=2001-07-05T15:30:45Z").setHeader("X-D3", "2001-07-06T15:30:45Z").getResponseAsString();
		assertEquals("d1=2001-07-04T15:30:45Z,d2=2001-07-05T15:30:45Z,d3=2001-07-06T15:30:45Z", r);
		
		client.closeQuietly();
	}
}