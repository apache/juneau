/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static com.ibm.juno.server.tests.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.regex.*;

import org.apache.http.entity.*;
import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;

public class CT_TestRestClient {

	private static String URL = "/testRestClient";

	//====================================================================================================
	// successPattern()
	//====================================================================================================
	@Test
	public void testSuccessPattern() throws Exception {
		RestClient c = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		int rc;

		r = c.doPost(URL, new StringEntity("xxxSUCCESSxxx")).successPattern("SUCCESS").getResponseAsString();
		assertEquals("xxxSUCCESSxxx", r);
		rc = c.doPost(URL, new StringEntity("xxxSUCCESSxxx")).successPattern("SUCCESS").run();
		assertEquals(200, rc);

		try {
			r = c.doPost(URL, new StringEntity("xxxFAILURExxx")).successPattern("SUCCESS").getResponseAsString();
			fail();
		} catch (RestCallException e) {
			assertEquals("Success pattern not detected.", e.getLocalizedMessage());
		}

		c.closeQuietly();
	}

	//====================================================================================================
	// failurePattern()
	//====================================================================================================
	@Test
	public void testFailurePattern() throws Exception {
		RestClient c = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		int rc;

		r = c.doPost(URL, new StringEntity("xxxSUCCESSxxx")).failurePattern("FAILURE").getResponseAsString();
		assertEquals("xxxSUCCESSxxx", r);
		rc = c.doPost(URL, new StringEntity("xxxSUCCESSxxx")).failurePattern("FAILURE").run();
		assertEquals(200, rc);

		try {
			r = c.doPost(URL, new StringEntity("xxxFAILURExxx")).failurePattern("FAILURE").getResponseAsString();
			fail();
		} catch (RestCallException e) {
			assertEquals("Failure pattern detected.", e.getLocalizedMessage());
		}

		try {
			r = c.doPost(URL, new StringEntity("xxxERRORxxx")).failurePattern("FAILURE|ERROR").getResponseAsString();
			fail();
		} catch (RestCallException e) {
			assertEquals("Failure pattern detected.", e.getLocalizedMessage());
		}

		c.closeQuietly();
	}

	//====================================================================================================
	// captureResponse()/getCapturedResponse()
	//====================================================================================================
	@Test
	public void testCaptureResponse() throws Exception {
		RestClient c = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall rc = c.doPost(URL, new StringEntity("xxx")).captureResponse();

		try {
			rc.getCapturedResponse();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("This method cannot be called until the response has been consumed.", e.getLocalizedMessage());
		}
		rc.run();
		assertEquals("xxx", rc.getCapturedResponse());
		assertEquals("xxx", rc.getCapturedResponse());

		rc = c.doPost(URL, new StringEntity("xxx")).captureResponse();
		assertEquals("xxx", rc.getResponseAsString());
		assertEquals("xxx", rc.getCapturedResponse());
		assertEquals("xxx", rc.getCapturedResponse());

		try {
			rc.getResponseAsString();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Method cannot be called.  Response has already been consumed.", e.getLocalizedMessage());
		}

		c.closeQuietly();
	}

	//====================================================================================================
	// addResponsePattern()
	//====================================================================================================
	@Test
	public void testAddResponsePattern() throws Exception {
		RestClient c = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		final List<String> l = new ArrayList<String>();
		ResponsePattern p = new ResponsePattern("x=(\\d+),y=(\\S+)") {
			@Override
			public void onMatch(RestCall restCall, Matcher m) throws RestCallException {
				l.add(m.group(1)+'/'+m.group(2));
			}
			@Override
			public void onNoMatch(RestCall restCall) throws RestCallException {
				throw new RestCallException("Pattern not found!");
			}
		};

		r = c.doPost(URL, new StringEntity("x=1,y=2")).addResponsePattern(p).getResponseAsString();
		assertEquals("x=1,y=2", r);
		assertObjectEquals("['1/2']", l);

		l.clear();

		r = c.doPost(URL, new StringEntity("x=1,y=2\nx=3,y=4")).addResponsePattern(p).getResponseAsString();
		assertEquals("x=1,y=2\nx=3,y=4", r);
		assertObjectEquals("['1/2','3/4']", l);

		try {
			c.doPost(URL, new StringEntity("x=1")).addResponsePattern(p).run();
			fail();
		} catch (RestCallException e) {
			assertEquals("Pattern not found!", e.getLocalizedMessage());
			assertEquals(0, e.getResponseCode());
		}

		// Two patterns!
		ResponsePattern p1 = new ResponsePattern("x=(\\d+)") {
			@Override
			public void onMatch(RestCall restCall, Matcher m) throws RestCallException {
				l.add("x="+m.group(1));
			}
			@Override
			public void onNoMatch(RestCall restCall) throws RestCallException {
				throw new RestCallException("Pattern x not found!");
			}
		};
		ResponsePattern p2 = new ResponsePattern("y=(\\S+)") {
			@Override
			public void onMatch(RestCall restCall, Matcher m) throws RestCallException {
				l.add("y="+m.group(1));
			}
			@Override
			public void onNoMatch(RestCall restCall) throws RestCallException {
				throw new RestCallException("Pattern y not found!");
			}
		};

		l.clear();
		r = c.doPost(URL, new StringEntity("x=1,y=2\nx=3,y=4")).addResponsePattern(p1).addResponsePattern(p2).getResponseAsString();
		assertEquals("x=1,y=2\nx=3,y=4", r);
		assertObjectEquals("['x=1','x=3','y=2','y=4']", l);

		try {
			c.doPost(URL, new StringEntity("x=1\nx=3")).addResponsePattern(p1).addResponsePattern(p2).getResponseAsString();
		} catch (RestCallException e) {
			assertEquals("Pattern y not found!", e.getLocalizedMessage());
			assertEquals(0, e.getResponseCode());
		}

		try {
			c.doPost(URL, new StringEntity("y=1\ny=3")).addResponsePattern(p1).addResponsePattern(p2).getResponseAsString();
		} catch (RestCallException e) {
			assertEquals("Pattern x not found!", e.getLocalizedMessage());
			assertEquals(0, e.getResponseCode());
		}

		c.closeQuietly();
	}
}
