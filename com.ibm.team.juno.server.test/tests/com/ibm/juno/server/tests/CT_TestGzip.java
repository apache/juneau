/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static com.ibm.juno.server.tests.TestUtils.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.zip.*;

import org.apache.http.impl.client.*;
import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.utils.*;

/**
 * Test Accept-Encoding and Content-Encoding handling.
 *
 * Note:  WAS does automatic gzip decompression on http request messages, so we have to invent
 * 	our own 'mycoding' compression.
 */
public class CT_TestGzip {

	private static boolean debug = false;

	private static String testGzipOff = "/testGzipOff";
	private static String testGzipOn = "/testGzipOn";

	// Converts string into a GZipped input stream.
	private static InputStream compress(String contents) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(contents.length()>>1);
		GZIPOutputStream gos = new GZIPOutputStream(baos);
		gos.write(contents.getBytes());
		gos.finish();
		gos.close();
		return new ByteArrayInputStream(baos.toByteArray());
	}

	private static String decompress(InputStream is) throws Exception {
		return IOUtils.read(new GZIPInputStream(is));
	}

	//====================================================================================================
	// Test with no compression enabled.
	//====================================================================================================
	@Test
	public void testGzipOff() throws Exception {
		RestClient c = new TestRestClient().setAccept("text/plain").setContentType("text/plain");
		RestCall r;
		String url = testGzipOff;

		// *** GET ***

		r = c.doGet(url);
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).setHeader("Accept-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).setHeader("Accept-Encoding", "*");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).setHeader("Accept-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity.
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding");
		assertEquals("foo", r.getResponseAsString());

		// Shouldn't match.
		try {
			r = c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "mycoding,identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: [identity]"
			);
		}

		// Shouldn't match.
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "mycoding,*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: [identity]"
			);
		}

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "identity;q=0.8,mycoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding;q=0.8,identity;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding;q=0.8,*;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "*;q=0.8,myencoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: [identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "identity;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: [identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: [identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "*;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: [identity]"
			);
		}


		// *** PUT ***

		r = c.doPut(url, new StringReader("foo"));
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).setHeader("Content-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).setHeader("Content-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		try {
			c.doPut(url+"?noTrace=true", compress("foo")).setHeader("Content-Encoding", "mycoding").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported encoding in request header 'Content-Encoding': 'mycoding'",
				"Supported codings: [identity]"
			);
		}

		c.closeQuietly();
	}

	//====================================================================================================
	// Test with compression enabled.
	//====================================================================================================
	@Test
	public void testGzipOn() throws Exception {

		// Create a client that disables content compression support so that we can get the gzipped content directly.
		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(TestRestClient.getSSLSocketFactory()).disableContentCompression().build();

		RestClient c = new TestRestClient(httpClient).setAccept("text/plain").setContentType("text/plain");
		RestCall r;
		String url = testGzipOn;

		// *** GET ***

		r = c.doGet(url);
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).setHeader("Accept-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).setHeader("Accept-Encoding", "*");
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).setHeader("Accept-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity.
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding");
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding,identity;q=0").connect();
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding,*;q=0").connect();
		assertEquals("foo", decompress(r.getInputStream()));

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "identity;q=0.8,mycoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match mycoding
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding;q=0.8,identity;q=0.6");
		assertEquals("foo", decompress(r.getInputStream()));

		// Should match mycoding
		r = c.doGet(url).setHeader("Accept-Encoding", "mycoding;q=0.8,*;q=0.6");
		assertEquals("foo", decompress(r.getInputStream()));

		// Should match identity
		r = c.doGet(url).setHeader("Accept-Encoding", "*;q=0.8,myencoding;q=0.6");
		assertEquals("foo", decompress(r.getInputStream()));

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: [mycoding, identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "identity;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: [mycoding, identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: [mycoding, identity]"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").setHeader("Accept-Encoding", "*;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: [mycoding, identity]"
			);
		}


		// *** PUT ***

		r = c.doPut(url, new StringReader("foo"));
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).setHeader("Content-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).setHeader("Content-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, compress("foo")).setHeader("Content-Encoding", "mycoding");
		assertEquals("foo", r.getResponseAsString());

		c.closeQuietly();
	}

	//====================================================================================================
	// Test with compression enabled but with servlet using output stream directly.
	//====================================================================================================
	@Test
	public void testGzipOnDirect() throws Exception {
		// Create a client that disables content compression support so that we can get the gzipped content directly.
		CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLSocketFactory(TestRestClient.getSSLSocketFactory()).build();
		RestClient c = new TestRestClient(httpClient).setAccept("text/plain").setContentType("text/plain");
		RestCall r = null;
		String s = null;

		// res.getOutputStream() called....should bypass encoding.
		r = c.doGet(testGzipOn + "/direct").setHeader("Accept-Encoding", "mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertTrue(r.getResponse().getHeaders("Content-Type")[0].getValue().contains("text/direct")); // Should get header set manually.
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		// res.getWriter() called....should bypass encoding.
		r = c.doGet(testGzipOn + "/direct2").setHeader("Accept-Encoding", "mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		// res.getNegotiateWriter() called....should NOT bypass encoding.
		r = c.doGet(testGzipOn + "/direct3").setHeader("Accept-Encoding", "mycoding");
		try {
			assertEquals("mycoding", r.getResponse().getHeaders("content-encoding")[0].getValue());
		} catch (RestCallException e) {
			// OK - HttpClient doesn't know what mycoding is.
			// Newer versions of HttpClient ignore this condition.
		}

		// res.getNegotiateWriter() called but @RestMethod(encoders={})...should bypass encoding.
		r = c.doGet(testGzipOn + "/direct4").setHeader("Accept-Encoding", "mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		c.closeQuietly();
	}
}
