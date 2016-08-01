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
import com.ibm.juno.core.utils.*;


public class CT_TestCharsetEncodings {

	private static boolean debug = false;

	/**
	 * Basic tests to ensure that the correct charsets are found and used
	 * under a variety of scenarios.
	 */
	@Test
	public void test() throws Exception {
		String url = "/testCharsetEncodings";
		RestClient client = new TestRestClient().setAccept("text/s").setContentType("text/p");
		InputStream is;
		String r;

		r = client.doPut(url, new StringReader("foo")).getResponseAsString();
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		is = client.doPut(url, new StringReader("foo")).getInputStream();
		r = IOUtils.read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.setHeader("Accept-Charset", "utf-8").setContentType("text/p;charset=utf-8");
		is = client.doPut(url, new StringReader("foo")).getInputStream();
		r = IOUtils.read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.setHeader("Accept-Charset", "Shift_JIS").setContentType("text/p;charset=shift_jis");
		is = client.doPut(url, new StringReader("foo")).getInputStream();
		r = IOUtils.read(new InputStreamReader(is, "Shift_JIS"));
		if (debug) System.err.println(r);
		assertEquals("shift_jis/foo/shift_jis", r);

		try {
			client.setHeader("Accept-Charset", "BAD").setContentType("text/p;charset=sjis");
			is = client.doPut(url + "?noTrace=true", new StringReader("foo")).getInputStream();
			r = IOUtils.read(new InputStreamReader(is, "sjis"));
			if (debug) System.err.println(r);
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE, "No supported charsets in header 'Accept-Charset': 'BAD'");
		}

		client.setAccept("text/s").setHeader("Accept-Charset", "utf-8").setContentType("text/p");
		is = client.doPut(url+"?Content-Type=text/p", new StringReader("foo")).getInputStream();
		r = IOUtils.read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.setAccept("text/s").setContentType("text/bad").setHeader("Accept-Charset", "utf-8");
		is = client.doPut(url+"?Content-Type=text/p;charset=utf-8", new StringReader("foo")).getInputStream();
		r = IOUtils.read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		try {
			client.setAccept("text/s").setContentType("text/p").setHeader("Accept-Charset", "utf-8");
			is = client.doPut(url+"?Content-Type=text/p;charset=BAD&noTrace=true", new StringReader("foo")).getInputStream();
			r = IOUtils.read(new InputStreamReader(is, "utf-8"));
			if (debug) System.err.println(r);
			assertEquals("utf-8/foo/utf-8", r);
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header 'Content-Type': 'text/p;charset=BAD'");
		}
		client.closeQuietly();
	}
}
