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

import static org.junit.Assert.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class StaticFilesTest extends RestTestcase {

	private static String URL = "/testStaticFiles";

	//====================================================================================================
	// Tests the @RestResource(staticFiles) annotation.
	//====================================================================================================
	@Test
	public void testXdocs() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String r;
		String url = URL + "/xdocs";

		r = client.doGet(url + "/test.txt").getResponseAsString();
		assertTrue(r.endsWith("OK-1"));
		r = client.doGet(url + "/xsubdocs/test.txt").getResponseAsString();
		assertTrue(r.endsWith("OK-2"));

		// For security reasons, paths containing ".." should always return 404.
		try {
			client.doGet(url + "/xsubdocs/../test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}

		try {
			client.doGet(url + "/xsubdocs/%2E%2E/test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}
	}

	//====================================================================================================
	// Tests the @RestResource(staticFiles) annotation.
	//====================================================================================================
	@Test
	public void testXdocsPreventPathTraversal() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String url = URL + "/xdocs";

		// For security reasons, paths containing ".." should always return 404.
		try {
			client.doGet(url + "/xsubdocs/../test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}

		try {
			client.doGet(url + "/xsubdocs/%2E%2E/test.txt?noTrace=true").connect();
			fail("404 exception expected");
		} catch (RestCallException e) {
			assertEquals(404, e.getResponseCode());
		}
	}

	//====================================================================================================
	// Tests the @RestResource(staticFiles) annotation.
	//====================================================================================================
	@Test
	public void testXdocsWithHeader() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;
		String r;
		String url = URL + "/xdocs2";

		r = client.doGet(url + "/test.txt").getResponseAsString();
		assertTrue(r.endsWith("OK-3"));
		
		r = client.doGet(url + "/test.txt").getResponseHeader("Foo");
		assertEquals("Bar", r);
	}
}
