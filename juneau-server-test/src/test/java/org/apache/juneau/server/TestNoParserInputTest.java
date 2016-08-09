/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.server.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.apache.juneau.plaintext.*;
import org.junit.*;

public class TestNoParserInputTest {

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
				"'public java.lang.String org.apache.juneau.server.TestNoParserInput.testPushbackReader(java.io.PushbackReader) throws java.lang.Exception'");
		}

		client.closeQuietly();
	}
}
