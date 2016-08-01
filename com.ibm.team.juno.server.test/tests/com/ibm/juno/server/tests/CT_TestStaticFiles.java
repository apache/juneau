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

public class CT_TestStaticFiles {

	private static String URL = "/testStaticFiles";

	//====================================================================================================
	// Tests the @RestResource(staticFiles) annotation.
	//====================================================================================================
	@Test
	public void testXdocs() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r;
		String url = URL + "/xdocs";

		r = client.doGet(url + "/test.txt").getResponseAsString();
		assertEquals("OK-1", r);
		r = client.doGet(url + "/xdocs/test.txt").getResponseAsString();
		assertEquals("OK-2", r);

		// For security reasons, paths containing ".." should always return 404.
		try {
			client.doGet(url + "/xdocs/../test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}

		try {
			client.doGet(url + "/xdocs/%2E%2E/test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}

		client.closeQuietly();
	}
}
