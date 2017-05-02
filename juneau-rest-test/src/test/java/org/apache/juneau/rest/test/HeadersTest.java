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

public class HeadersTest extends RestTestcase {

	private static String URL = "/testHeaders";

	//====================================================================================================
	// Basic tests
	//====================================================================================================

	@Test
	public void testAccept() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;

		assertEquals("text/foo", client.doGet(URL + "/accept").accept("text/foo").getResponseAsString());
		assertEquals("text/foo+bar", client.doGet(URL + "/accept").accept("text/foo+bar").getResponseAsString());
		assertEquals("text/*", client.doGet(URL + "/accept").accept("text/*").getResponseAsString());
		assertEquals("*/foo", client.doGet(URL + "/accept").accept("*/foo").getResponseAsString());

		assertEquals("text/foo", client.doGet(URL + "/accept").accept("text/foo;q=1.0").getResponseAsString());
		assertEquals("text/foo;q=0.9", client.doGet(URL + "/accept").accept("text/foo;q=0.9").getResponseAsString());
		assertEquals("text/foo;x=X;q=0.9;y=Y", client.doGet(URL + "/accept").accept("text/foo;x=X;q=0.9;y=Y").getResponseAsString());
	}

	@Test
	public void testAcceptEncoding() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;

		assertEquals("foo", client.doGet(URL + "/acceptEncoding").accept("text/plain").acceptEncoding("foo").getResponseAsString());
		assertEquals("*", client.doGet(URL + "/acceptEncoding").accept("text/plain").acceptEncoding("*").getResponseAsString());
	}

	@Test
	public void testContentType() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;

		assertEquals("text/foo", client.doGet(URL + "/contentType").accept("text/plain").contentType("text/foo").getResponseAsString());
	}
}
