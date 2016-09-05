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
package org.apache.juneau.server;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.server.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;

import org.apache.juneau.client.*;
import org.apache.juneau.internal.*;
import org.junit.*;


public class CharsetEncodingsTest {

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
