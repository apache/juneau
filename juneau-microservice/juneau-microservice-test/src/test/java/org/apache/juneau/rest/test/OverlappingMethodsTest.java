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
import static org.apache.juneau.microservice.testutils.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class OverlappingMethodsTest extends RestTestcase {

	private static String URL = "/testOverlappingMethods";
	private static boolean debug = false;

	//====================================================================================================
	// Overlapping guards
	//====================================================================================================
	@Test
	public void testOverlappingGuards1() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testOverlappingGuards1";

		r = client.doGet(url + "?t1=1").getResponseAsString();
		assertEquals("test1_doGet", r);

		try {
			client.doGet(url + "?noTrace=true").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_FORBIDDEN, "Access denied by guard");
		}

		client.closeQuietly();
	}

	//====================================================================================================
	// Overlapping guards
	//====================================================================================================
	@Test
	public void testOverlappingGuards2() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testOverlappingGuards2";
		try {
			client.doGet(url + "?noTrace=true").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_FORBIDDEN, "Access denied by guard");
		}

		try {
			client.doGet(url + "?t1=1&noTrace=true").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_FORBIDDEN, "Access denied by guard");
		}

		try {
			client.doGet(url + "?t2=2&noTrace=true").connect();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_FORBIDDEN, "Access denied by guard");
		}

		r = client.doGet(url + "?t1=1&t2=2").getResponseAsString();
		assertEquals("test2_doGet", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Overlapping matchers
	//====================================================================================================
	@Test
	public void testOverlappingMatchers1() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testOverlappingMatchers1";

		r = client.doGet(url + "?t1=1").getResponseAsString();
		assertEquals("test3a", r);

		r = client.doGet(url + "?t2=2").getResponseAsString();
		assertEquals("test3b", r);

		r = client.doGet(url).getResponseAsString();
		assertEquals("test3c", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Overlapping matchers
	//====================================================================================================
	@Test
	public void testOverlappingMatchers2() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testOverlappingMatchers2";

		r = client.doGet(url + "?t1=1").getResponseAsString();
		assertEquals("test4b", r);

		r = client.doGet(url + "?t2=2").getResponseAsString();
		assertEquals("test4b", r);

		r = client.doGet(url + "?t1=1&t2=2").getResponseAsString();
		assertEquals("test4b", r);

		r = client.doGet(url + "?tx=x").getResponseAsString();
		assertEquals("test4a", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Overlapping URL patterns
	//====================================================================================================
	@Test
	public void testOverlappingUrlPatterns() throws Exception {
		RestClient client = TestMicroservice.client().accept("text/plain").build();
		String r;
		String url = URL + "/testOverlappingUrlPatterns";

		// [/test5] = [test5a]
		// [/test5/*] = [test5b]   -- Cannot get called.
		// [/test5/foo] = [test5c]
		// [/test5/foo/*] = [test5d]
		// [/test5/{id}] = [test5e]
		// [/test5/{id}/*] = [test5f]
		// [/test5/{id}/foo] = [test5g]
		// [/test5/{id}/foo/*] = [test5h]

		r = client.doGet(url).getResponseAsString();
		assertEquals("test5a", r);

		r = client.doGet(url + "/foo").getResponseAsString();
		assertEquals("test5c", r);

		r = client.doGet(url + "/foo/x").getResponseAsString();
		assertEquals("test5d", r);

		r = client.doGet(url + "/x").getResponseAsString();
		assertEquals("test5e", r);

		r = client.doGet(url + "/x/x").getResponseAsString();
		assertEquals("test5f", r);

		r = client.doGet(url + "/x/foo").getResponseAsString();
		assertEquals("test5g", r);

		r = client.doGet(url + "/x/foo/x").getResponseAsString();
		assertEquals("test5h", r);

		client.closeQuietly();
	}
}
