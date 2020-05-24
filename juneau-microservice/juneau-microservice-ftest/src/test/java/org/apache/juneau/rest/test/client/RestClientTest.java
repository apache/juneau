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
package org.apache.juneau.rest.test.client;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.regex.*;

import org.apache.http.entity.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.test.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class RestClientTest extends RestTestcase {

	private static String URL = "/testRestClient";

	//====================================================================================================
	// successPattern()
	//====================================================================================================
	@Test
	public void testSuccessPattern() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;

		Mutable<Integer> rc = new Mutable<>();
		Mutable<String> r = new Mutable<>();
		c.post(URL, new StringEntity("xxxSUCCESSxxx"))
			.run()
			.getStatusCode(rc)
			.cacheBody()
			.assertBody().contains("SUCCESS")
			.getBody().asString(r);
		assertEquals("xxxSUCCESSxxx", r.get());
		assertEquals(200, rc.get().intValue());

		try {
			c.post(URL, new StringEntity("xxxFAILURExxx"))
				.run()
				.assertBody().contains("SUCCESS");
			fail();
		} catch (AssertionError e) {
			// OK
		}
	}

	//====================================================================================================
	// failurePattern()
	//====================================================================================================
	@Test
	public void testFailurePattern() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;

		Mutable<Integer> rc = new Mutable<>();
		Mutable<String> r = new Mutable<>();
		c.post(URL, new StringEntity("xxxSUCCESSxxx"))
			.run()
			.getStatusCode(rc)
			.cacheBody()
			.assertBody().passes(x -> ! x.contains("FAILURE"))
			.getBody().asString(r);
		assertEquals("xxxSUCCESSxxx", r.get());
		assertEquals(200, rc.get().intValue());

		try {
			c.post(URL, new StringEntity("xxxFAILURExxx"))
				.run()
				.assertBody().passes(x -> ! x.contains("FAILURE"));
			fail();
		} catch (AssertionError e) {
			// OK
		}
	}

	//====================================================================================================
	// captureResponse()/getCapturedResponse()
	//====================================================================================================
	@Test
	public void testCaptureResponse() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;
		RestResponse r = c
			.post(URL, new StringEntity("xxx"))
			.run()
			.cacheBody()
			.getBody().toResponse();

		assertEquals("xxx", r.getBody().asString());
		assertEquals("xxx", r.getBody().asString());

		r = c.post(URL, new StringEntity("xxx")).run();
		assertEquals("xxx", r.getBody().asString());

		try {
			r.getBody().asString();
			fail();
		} catch (IllegalStateException e) {
			assertEquals("Method cannot be called.  Response has already been consumed.  Consider using the RestResponse.cacheBody() method.", e.getLocalizedMessage());
		}
	}

	//====================================================================================================
	// addResponsePattern()
	//====================================================================================================
	@Test
	public void testAddResponsePattern() throws Exception {
		RestClient c = TestMicroservice.DEFAULT_CLIENT;
		String r;

		Mutable<Matcher> m = Mutable.create();
		r = c.post(URL, new StringEntity("x=1,y=2"))
			.run()
			.cacheBody()
			.getBody().asMatcher(m, "x=(\\d+),y=(\\S+)")
			.getBody().asString();
		assertEquals("x=1,y=2", r);
		assertTrue(m.get().matches());
		assertObjectEquals("['x=1,y=2','1','2']", m.get().toMatchResult());

		r = c.post(URL, new StringEntity("x=1,y=2\nx=3,y=4"))
			.run()
			.cacheBody()
			.getBody().asMatcher(m, "x=(\\d+),y=(\\S+)")
			.getBody().asString();
		assertEquals("x=1,y=2\nx=3,y=4", r);
		assertTrue(m.get().find());
		assertObjectEquals("['x=1,y=2','1','2']", m.get().toMatchResult());
		assertTrue(m.get().find());
		assertObjectEquals("['x=3,y=4','3','4']", m.get().toMatchResult());

		c.post(URL, new StringEntity("x=1")).run().getBody().asMatcher("x=(\\d+),y=(\\S+)");
		assertFalse(m.get().find());

		Mutable<Matcher> m2 = Mutable.create();
		c.post(URL, new StringEntity("x=1,y=2"))
			.run()
			.cacheBody()
			.getBody().asMatcher(m, "x=(\\d+),y=(\\S+)")
			.getBody().asMatcher(m2, "x=(\\d+),y=(\\S+)");
		assertTrue(m.get().matches());
		assertTrue(m2.get().matches());
		assertObjectEquals("['x=1,y=2','1','2']", m.get().toMatchResult());
		assertObjectEquals("['x=1,y=2','1','2']", m2.get().toMatchResult());
	}
}
