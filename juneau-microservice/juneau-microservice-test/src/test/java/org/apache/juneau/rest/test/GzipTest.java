// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.test;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.zip.*;

import org.apache.http.impl.client.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

/**
 * Test Accept-Encoding and Content-Encoding handling.
 * 
 * Note:  WAS does automatic gzip decompression on http request messages, so we have to invent
 * 	our own 'mycoding' compression.
 */
public class GzipTest extends RestTestcase {

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
		return read(new GZIPInputStream(is));
	}

	//====================================================================================================
	// Test with no compression enabled.
	//====================================================================================================
	@Test
	public void testGzipOff() throws Exception {
		RestClient c = TestMicroservice.client().accept("text/plain").contentType("text/plain").build();
		RestCall r;
		String url = testGzipOff;

		// *** GET ***

		r = c.doGet(url);
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).acceptEncoding("");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).acceptEncoding("*");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).acceptEncoding("identity");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity.
		r = c.doGet(url).acceptEncoding("mycoding");
		assertEquals("foo", r.getResponseAsString());

		// Shouldn't match.
		try {
			r = c.doGet(url+"?noTrace=true").acceptEncoding("mycoding,identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,identity;q=0'",
				"Supported codings: ['identity']"
			);
		}

		// Shouldn't match.
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("mycoding,*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'mycoding,*;q=0'",
				"Supported codings: ['identity']"
			);
		}

		// Should match identity
		r = c.doGet(url).acceptEncoding("identity;q=0.8,mycoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).acceptEncoding("mycoding;q=0.8,identity;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).acceptEncoding("mycoding;q=0.8,*;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity
		r = c.doGet(url).acceptEncoding("*;q=0.8,myencoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("identity;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("*;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['identity']"
			);
		}


		// *** PUT ***

		r = c.doPut(url, new StringReader("foo"));
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).header("Content-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).header("Content-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		try {
			c.doPut(url+"?noTrace=true", compress("foo")).header("Content-Encoding", "mycoding").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported encoding in request header 'Content-Encoding': 'mycoding'",
				"Supported codings: ['identity']"
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
		CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(TestMicroservice.getSSLSocketFactory()).disableContentCompression().build();

		RestClient c = TestMicroservice.client().httpClient(httpClient, false).accept("text/plain").contentType("text/plain").build();
		RestCall r;
		String url = testGzipOn;

		// *** GET ***

		r = c.doGet(url);
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).acceptEncoding("");
		assertEquals("foo", r.getResponseAsString());

		r = c.doGet(url).acceptEncoding("*");
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).acceptEncoding("identity");
		assertEquals("foo", r.getResponseAsString());

		// Should match identity.
		r = c.doGet(url).acceptEncoding("mycoding");
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).acceptEncoding("mycoding,identity;q=0").connect();
		assertEquals("foo", decompress(r.getInputStream()));

		r = c.doGet(url).acceptEncoding("mycoding,*;q=0").connect();
		assertEquals("foo", decompress(r.getInputStream()));

		// Should match identity
		r = c.doGet(url).acceptEncoding("identity;q=0.8,mycoding;q=0.6");
		assertEquals("foo", r.getResponseAsString());

		// Should match mycoding
		r = c.doGet(url).acceptEncoding("mycoding;q=0.8,identity;q=0.6");
		assertEquals("foo", decompress(r.getInputStream()));

		// Should match mycoding
		r = c.doGet(url).acceptEncoding("mycoding;q=0.8,*;q=0.6");
		assertEquals("foo", decompress(r.getInputStream()));

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("identity;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("identity;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': 'identity;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("*;q=0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0'",
				"Supported codings: ['mycoding','identity']"
			);
		}

		// Shouldn't match
		try {
			c.doGet(url+"?noTrace=true").acceptEncoding("*;q=0.0").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported encoding in request header 'Accept-Encoding': '*;q=0.0'",
				"Supported codings: ['mycoding','identity']"
			);
		}


		// *** PUT ***

		r = c.doPut(url, new StringReader("foo"));
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).header("Content-Encoding", "");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, new StringReader("foo")).header("Content-Encoding", "identity");
		assertEquals("foo", r.getResponseAsString());

		r = c.doPut(url, compress("foo")).header("Content-Encoding", "mycoding");
		assertEquals("foo", r.getResponseAsString());

		c.closeQuietly(); // We want to close our client because we created the HttpClient in this method.
	}

	//====================================================================================================
	// Test with compression enabled but with servlet using output stream directly.
	//====================================================================================================
	@Test
	public void testGzipOnDirect() throws Exception {
		// Create a client that disables content compression support so that we can get the gzipped content directly.
		CloseableHttpClient httpClient = HttpClientBuilder.create().setSSLSocketFactory(TestMicroservice.getSSLSocketFactory()).build();
		RestClient c = TestMicroservice.client().httpClient(httpClient, false).accept("text/plain").contentType("text/plain").build();
		RestCall r = null;
		String s = null;

		// res.getOutputStream() called....should bypass encoding.
		r = c.doGet(testGzipOn + "/direct").acceptEncoding("mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertTrue(r.getResponse().getHeaders("Content-Type")[0].getValue().contains("text/direct")); // Should get header set manually.
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		// res.getWriter() called....should bypass encoding.
		r = c.doGet(testGzipOn + "/direct2").acceptEncoding("mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		// res.getNegotiateWriter() called....should NOT bypass encoding.
		r = c.doGet(testGzipOn + "/direct3").acceptEncoding("mycoding");
		try {
			assertEquals("mycoding", r.getResponse().getHeaders("content-encoding")[0].getValue());
		} catch (RestCallException e) {
			// OK - HttpClient doesn't know what mycoding is.
			// Newer versions of HttpClient ignore this condition.
		}

		// res.getNegotiateWriter() called but @RestMethod(encoders={})...should bypass encoding.
		r = c.doGet(testGzipOn + "/direct4").acceptEncoding("mycoding");
		s = r.getResponseAsString();
		assertEquals("test", s);
		assertEquals(0, r.getResponse().getHeaders("Content-Encoding").length);                // Should not be set.

		c.closeQuietly(); // We want to close our client because we created the HttpClient in this method.
	}
}
