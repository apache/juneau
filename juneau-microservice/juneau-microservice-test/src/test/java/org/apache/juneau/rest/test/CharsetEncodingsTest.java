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


public class CharsetEncodingsTest extends RestTestcase {

	private static boolean debug = false;

	/**
	 * Basic tests to ensure that the correct charsets are found and used
	 * under a variety of scenarios.
	 */
	@Test
	public void test() throws Exception {
		String url = "/testCharsetEncodings";
		RestClientBuilder cb = TestMicroservice.client().accept("text/s").contentType("text/p");
		RestClient client = cb.build();
		InputStream is;
		String r;

		r = client.doPut(url, new StringReader("foo")).getResponseAsString();
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		is = client.doPut(url, new StringReader("foo")).getInputStream();
		r = read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.closeQuietly();

		client = cb.acceptCharset("utf-8").contentType("text/p;charset=utf-8").build();
		is = client.doPut(url, new StringReader("foo")).acceptCharset("utf-8").contentType("text/p;charset=utf-8").getInputStream();
		r = read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.closeQuietly();

		client = cb.acceptCharset("Shift_JIS").contentType("text/p;charset=shift_jis").build();
		is = client.doPut(url, new StringReader("foo")).getInputStream();
		r = read(new InputStreamReader(is, "Shift_JIS"));
		if (debug) System.err.println(r);
		assertEquals("shift_jis/foo/shift_jis", r);

		client.closeQuietly();

		try {
			client = cb.acceptCharset("BAD").contentType("text/p;charset=sjis").build();
			is = client.doPut(url + "?noTrace=true", new StringReader("foo")).getInputStream();
			r = read(new InputStreamReader(is, "sjis"));
			if (debug) System.err.println(r);
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE, "No supported charsets in header 'Accept-Charset': 'BAD'");
		}

		client.closeQuietly();

		client = cb.accept("text/s").acceptCharset("utf-8").contentType("text/p").build();
		is = client.doPut(url+"?Content-Type=text/p", new StringReader("foo")).getInputStream();
		r = read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.closeQuietly();

		client = cb.accept("text/s").contentType("text/bad").acceptCharset("utf-8").build();
		is = client.doPut(url+"?Content-Type=text/p;charset=utf-8", new StringReader("foo")).getInputStream();
		r = read(new InputStreamReader(is, "utf-8"));
		if (debug) System.err.println(r);
		assertEquals("utf-8/foo/utf-8", r);

		client.closeQuietly();

		try {
			client = cb.accept("text/s").contentType("text/p").acceptCharset("utf-8").build();
			is = client.doPut(url+"?Content-Type=text/p;charset=BAD&noTrace=true", new StringReader("foo")).getInputStream();
			r = read(new InputStreamReader(is, "utf-8"));
			if (debug) System.err.println(r);
			assertEquals("utf-8/foo/utf-8", r);
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header 'Content-Type': 'text/p;charset=BAD'");
		}

		client.closeQuietly();
	}
}
