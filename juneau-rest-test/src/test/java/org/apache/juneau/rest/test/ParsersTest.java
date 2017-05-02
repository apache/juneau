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
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class ParsersTest extends RestTestcase {

	private static String URL = "/testParsers";
	private static boolean debug = false;

	//====================================================================================================
	// Parser defined on class.
	//====================================================================================================
	@Test
	public void testParserOnClass() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/testParserOnClass";

		String r = client.doPut(url, "test1").contentType("text/a").getResponseAsString();
		assertEquals("text/a - test1", r);

		try {
			client.doPut(url + "?noTrace=true", "test1").contentType("text/b").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/b'",
				"Supported media-types: [text/a"
			);
		}

		r = client.doPut(url, "'test1'").contentType("text/json").accept("text/json").getResponseAsString();
		assertEquals("\"test1\"", r);
	}

	//====================================================================================================
	// Parser defined on method.
	//====================================================================================================
	@Test
	public void testParserOnMethod() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/testParserOnMethod";

		String r = client.doPut(url, "test2").contentType("text/b").getResponseAsString();
		assertEquals("text/b - test2", r);

		try {
			client.doPut(url + "?noTrace=true", "test2").contentType("text/a").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/a'",
				"Supported media-types: [text/b]"
			);
		}

		try {
			r = client.doPut(url + "?noTrace=true", "'test2'").contentType("text/json").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/json'",
				"Supported media-types: [text/b]"
			);
		}
	}

	//====================================================================================================
	// Parser overridden on method.
	//====================================================================================================
	@Test
	public void testParserOverriddenOnMethod() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/testParserOverriddenOnMethod";

		String r = client.doPut(url, "test3").contentType("text/a").getResponseAsString();
		assertEquals("text/a - test3", r);

		r = client.doPut(url, "test3").contentType("text/b").getResponseAsString();
		assertEquals("text/b - test3", r);

		r = client.doPut(url, "'test3'").contentType("text/json").getResponseAsString();
		assertEquals("test3", r);
	}

	//====================================================================================================
	// Parser with different Accept than Content-Type.
	//====================================================================================================
	@Test
	public void testParserWithDifferentMediaTypes() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/testParserWithDifferentMediaTypes";

		String r = client.doPut(url, "test4").contentType("text/a").getResponseAsString();
		assertEquals("text/a - test4", r);

		r = client.doPut(url, "test4").contentType("text/d").getResponseAsString();
		assertEquals("text/d - test4", r);

		r = client.doPut(url, "'test4'").contentType("text/json").getResponseAsString();
		assertEquals("test4", r);
	}

	//====================================================================================================
	// Check for valid error response.
	//====================================================================================================
	@Test
	public void testValidErrorResponse() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/testValidErrorResponse";

		try {
			client.doPut(url + "?noTrace=true", "test1").contentType("text/bad").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/bad'",
				"Supported media-types: [text/a"
			);
		}
	}
}
