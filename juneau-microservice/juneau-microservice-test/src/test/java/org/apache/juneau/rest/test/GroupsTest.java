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

import java.io.*;

import org.apache.juneau.rest.client.*;
import org.junit.*;


public class GroupsTest extends RestTestcase {

	private static String URL = "/testGroups";
	private static boolean debug = false;

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@Test
	public void testSerializerDefinedOnClass() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String url = URL + "/testSerializerDefinedOnClass";
		String r;

		try {
			r = client.doGet(url+"?noTrace=true").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'application/json'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		r = client.doGet(url).accept("text/s1").contentType("").getResponseAsString();
		assertEquals("text/s,GET", r);

		r = client.doGet(url).accept("text/s2").contentType("").getResponseAsString();
		assertEquals("text/s,GET", r);

		try {
			r = client.doGet(url+"?noTrace=true").accept("text/s3").contentType("").getResponseAsString();
			assertEquals("text/s,GET", r);
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).accept("text/json").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/json'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).accept("text/s1").contentType("text/json").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/json'",
				"Supported media-types: ['text/p1','text/p2']"
			);
		}

		r = client.doPut(url, new StringReader("foo")).contentType("text/p1").accept("text/s1").getResponseAsString();
		assertEquals("text/s,foo", r);

		r = client.doPut(url, new StringReader("foo")).contentType("text/p2").accept("text/s2").getResponseAsString();
		assertEquals("text/s,foo", r);

		try {
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).contentType("text/p1").accept("text/s3").getResponseAsString();
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", new StringReader("foo")).contentType("text/p3").accept("text/s1").getResponseAsString();
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: ['text/p1','text/p2']"
			);
		}
	}
}
