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

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class AcceptCharsetTest extends RestTestcase {

	boolean debug = false;

	//====================================================================================================
	// Test that Q-values are being resolved correctly.
	//====================================================================================================
	@Test
	public void testQValues() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;

		check1(client, "utf-8", "utf-8");
		check1(client, "iso-8859-1", "iso-8859-1");
		check1(client, "bad,utf-8", "utf-8");
		check1(client, "utf-8,bad", "utf-8");
		check1(client, "bad;q=0.9,utf-8;q=0.1", "utf-8");
		check1(client, "bad;q=0.1,utf-8;q=0.9", "utf-8");
//		check1(client, "utf-8,iso-8859-1", "utf-8");
//		check1(client, "iso-8859-1,utf-8", "utf-8");
		check1(client, "utf-8;q=0.9,iso-8859-1;q=0.1", "utf-8");
		check1(client, "utf-8;q=0.1,iso-8859-1;q=0.9", "iso-8859-1");
		check1(client, "*", "utf-8");
		check1(client, "bad,iso-8859-1;q=0.5,*;q=0.1", "iso-8859-1");
		check1(client, "bad,iso-8859-1;q=0.1,*;q=0.5", "utf-8");
	}

	private void check1(RestClient client, String requestCharset, String responseCharset) throws Exception {
		RestCall r;
		debug=true;
		InputStream is;
		String url = "/testAcceptCharset/testQValues";
		r = client.doGet(url).acceptCharset(requestCharset).connect();

		assertTrue(r.getResponse().getFirstHeader("Content-Type").getValue().toLowerCase().contains(responseCharset));
		is = r.getInputStream();
		assertEquals("foo", read(new InputStreamReader(is, responseCharset)));
	}

	//====================================================================================================
	// Validate various Accept-Charset variations.
	//====================================================================================================
	@Test
	public void testCharsetOnResponse() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = "/testAcceptCharset/testCharsetOnResponse";
		String r;

		r = client.doPut(url, new StringReader("")).getResponseAsString();
		assertEquals("utf-8/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).acceptCharset("Shift_JIS").getResponseAsString();
		assertEquals("utf-8/shift_jis", r.toLowerCase());

		try {
			r = client.doPut(url+"?noTrace=true", new StringReader("")).acceptCharset("BAD").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE, "No supported charsets in header 'Accept-Charset': 'BAD'");
		}

		r = client.doPut(url, new StringReader("")).acceptCharset("UTF-8").getResponseAsString();
		assertEquals("utf-8/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).acceptCharset("bad,iso-8859-1").getResponseAsString();
		assertEquals("utf-8/iso-8859-1", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).acceptCharset("bad;q=0.9,iso-8859-1;q=0.1").getResponseAsString();
		assertEquals("utf-8/iso-8859-1", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).acceptCharset("bad;q=0.1,iso-8859-1;q=0.9").getResponseAsString();
		assertEquals("utf-8/iso-8859-1", r.toLowerCase());

		client = TestMicroservice.client().accept("text/plain").contentType("text/plain").acceptCharset("utf-8").build();

		r = client.doPut(url, new StringReader("")).contentType("text/plain").getResponseAsString();
		assertEquals("utf-8/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).contentType("text/plain;charset=utf-8").getResponseAsString();
		assertEquals("utf-8/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).contentType("text/plain;charset=UTF-8").getResponseAsString();
		assertEquals("utf-8/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).contentType("text/plain;charset=iso-8859-1").getResponseAsString();
		assertEquals("iso-8859-1/utf-8", r.toLowerCase());

		r = client.doPut(url, new StringReader("")).contentType("text/plain;charset=Shift_JIS").getResponseAsString();
		assertEquals("shift_jis/utf-8", r.toLowerCase());

		try {
			r = client.doPut(url + "?noTrace=true&Content-Type=text/plain;charset=BAD", new StringReader("")).getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header 'Content-Type': 'text/plain;charset=BAD'");
		}

		client.closeQuietly();
	}
}