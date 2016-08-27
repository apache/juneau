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

import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.apache.juneau.plaintext.*;
import org.junit.*;

public class ClientVersionTest {

	private static String URL = "/testClientVersion";

	//====================================================================================================
	// Basic tests - default X-Client-Version header.
	//====================================================================================================
	@Test
	public void testDefaultHeader() throws Exception {
		RestClient c = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/defaultHeader";

		assertEquals("no-version", c.doGet(url).getResponseAsString());

//		for (String s : "0, 0.0, 0.1, .1, .9, .99".split("\\s*,\\s*")) {
//			c.setClientVersion(s);
//			assertEquals(s, "[0.0,1.0)", c.doGet(url).getResponseAsString());
//		}

		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			c.setClientVersion(s);
			assertEquals(s, "[1.0,1.0]", c.doGet(url).getResponseAsString());
		}

		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			c.setClientVersion(s);
			assertEquals(s, "[1.1,2)", c.doGet(url).getResponseAsString());
		}

		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			c.setClientVersion(s);
			assertEquals(s, "2", c.doGet(url).getResponseAsString());
		}

		c.closeQuietly();
	}

	//====================================================================================================
	// Basic tests - Custom-Client-Version header.
	//====================================================================================================
	@Test
	public void testCustomHeader() throws Exception {
		RestClient c = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String url = URL + "/customHeader";

		assertEquals("no-version", c.doGet(url).getResponseAsString());

		for (String s : "0, 0.0, 0.1, .1, .9, .99".split("\\s*,\\s*")) {
			c.setHeader("Custom-Client-Version", s);
			assertEquals("[0.0,1.0)", c.doGet(url).getResponseAsString());
		}

		for (String s : "1, 1.0, 1.0.0, 1.0.1".split("\\s*,\\s*")) {
			c.setHeader("Custom-Client-Version", s);
			assertEquals("[1.0,1.0]", c.doGet(url).getResponseAsString());
		}

		for (String s : "1.1, 1.1.1, 1.2, 1.9.9".split("\\s*,\\s*")) {
			c.setHeader("Custom-Client-Version", s);
			assertEquals("[1.1,2)", c.doGet(url).getResponseAsString());
		}

		for (String s : "2, 2.0, 2.1, 9, 9.9".split("\\s*,\\s*")) {
			c.setHeader("Custom-Client-Version", s);
			assertEquals("2", c.doGet(url).getResponseAsString());
		}

		c.closeQuietly();
	}
}
