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
import static javax.servlet.http.HttpServletResponse.*;
import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;


public class CT_TestGroups {

	private static String URL = "/testGroups";
	private static boolean debug = false;

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@Test
	public void testSerializerDefinedOnClass() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String url = URL + "/testSerializerDefinedOnClass";
		String r;

		try {
			client.setContentType("text/p1");
			r = client.doGet(url+"?noTrace=true").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'application/json'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		client.setAccept("text/s1").setContentType("");
		r = client.doGet(url).getResponseAsString();
		assertEquals("text/s,GET", r);

		client.setAccept("text/s2").setContentType("");
		r = client.doGet(url).getResponseAsString();
		assertEquals("text/s,GET", r);

		try {
			client.setAccept("text/s3").setContentType("");
			r = client.doGet(url+"?noTrace=true").getResponseAsString();
			assertEquals("text/s,GET", r);
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		try {
			client.setAccept("text/json").setContentType("text/p1");
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/json'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		try {
			client.setAccept("text/s1").setContentType("text/json");
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/json'",
				"Supported media-types: [text/p1, text/p2]"
			);
		}

		client.setContentType("text/p1").setAccept("text/s1");
		r = client.doPut(url, new StringReader("foo")).getResponseAsString();
		assertEquals("text/s,foo", r);

		client.setContentType("text/p2").setAccept("text/s2");
		r = client.doPut(url, new StringReader("foo")).getResponseAsString();
		assertEquals("text/s,foo", r);

		try {
			client.setContentType("text/p1").setAccept("text/s3");
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).getResponseAsString();
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		try {
			client.setContentType("text/p3").setAccept("text/s1");
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).getResponseAsString();
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: [text/p1, text/p2]"
			);
		}

		client.closeQuietly();
	}
}
