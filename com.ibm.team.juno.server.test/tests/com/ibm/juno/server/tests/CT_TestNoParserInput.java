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

public class CT_TestNoParserInput {

	private static String URL = "/testNoParserInput";
	private static boolean debug = false;

	//====================================================================================================
	// @Content annotated InputStream.
	//====================================================================================================
	@Test
	public void testInputStream() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doPut(URL + "/testInputStream", "foo").getResponseAsString();
		assertEquals("foo", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @Content annotated Reader.
	//====================================================================================================
	@Test
	public void testReader() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doPut(URL + "/testReader", "foo").getResponseAsString();
		assertEquals("foo", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// @Content annotated PushbackReader.
	// This should always fail since the servlet reader is not a pushback reader.
	//====================================================================================================
	@Test
	public void testPushbackReader() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		try {
			client.doPut(URL + "/testPushbackReader?noTrace=true", "foo").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_BAD_REQUEST,
				"Invalid argument type passed to the following method:",
				"'public java.lang.String com.ibm.juno.server.test.TestNoParserInput.testPushbackReader(java.io.PushbackReader) throws java.lang.Exception'");
		}

		client.closeQuietly();
	}
}
