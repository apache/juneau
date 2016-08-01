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

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.plaintext.*;

public class CT_TestParsers {

	private static String URL = "/testParsers";
	private static boolean debug = false;

	//====================================================================================================
	// Parser defined on class.
	//====================================================================================================
	@Test
	public void testParserOnClass() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/testParserOnClass";

		client.setContentType("text/a");
		String r = client.doPut(url, "test1").getResponseAsString();
		assertEquals("text/a - test1", r);

		try {
			client.setContentType("text/b");
			client.doPut(url + "?noTrace=true", "test1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/b'",
				"Supported media-types: [text/a"
			);
		}

		client.setContentType("text/json").setAccept("text/json");
		r = client.doPut(url, "'test1'").getResponseAsString();
		assertEquals("\"test1\"", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Parser defined on method.
	//====================================================================================================
	@Test
	public void testParserOnMethod() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/testParserOnMethod";

		client.setContentType("text/b");
		String r = client.doPut(url, "test2").getResponseAsString();
		assertEquals("text/b - test2", r);

		try {
			client.setContentType("text/a");
			client.doPut(url + "?noTrace=true", "test2").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/a'",
				"Supported media-types: [text/b]"
			);
		}

		try {
			client.setContentType("text/json");
			r = client.doPut(url + "?noTrace=true", "'test2'").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/json'",
				"Supported media-types: [text/b]"
			);
		}

		client.closeQuietly();
	}

	//====================================================================================================
	// Parser overridden on method.
	//====================================================================================================
	@Test
	public void testParserOverriddenOnMethod() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/testParserOverriddenOnMethod";

		client.setContentType("text/a");
		String r = client.doPut(url, "test3").getResponseAsString();
		assertEquals("text/a - test3", r);

		client.setContentType("text/b");
		r = client.doPut(url, "test3").getResponseAsString();
		assertEquals("text/b - test3", r);

		client.setContentType("text/json");
		r = client.doPut(url, "'test3'").getResponseAsString();
		assertEquals("test3", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Parser with different Accept than Content-Type.
	//====================================================================================================
	@Test
	public void testParserWithDifferentMediaTypes() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/testParserWithDifferentMediaTypes";

		client.setContentType("text/a");
		String r = client.doPut(url, "test4").getResponseAsString();
		assertEquals("text/d - test4", r);

		client.setContentType("text/d");
		r = client.doPut(url, "test4").getResponseAsString();
		assertEquals("text/d - test4", r);

		client.setContentType("text/json");
		r = client.doPut(url, "'test4'").getResponseAsString();
		assertEquals("test4", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Check for valid error response.
	//====================================================================================================
	@Test
	public void testValidErrorResponse() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/testValidErrorResponse";

		try {
			client.setContentType("text/bad");
			client.doPut(url + "?noTrace=true", "test1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/bad'",
				"Supported media-types: [text/a"
			);
		}

		client.closeQuietly();
	}
}
