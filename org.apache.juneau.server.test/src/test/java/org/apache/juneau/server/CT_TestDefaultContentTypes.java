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
import org.apache.juneau.json.*;
import org.junit.*;


public class CT_TestDefaultContentTypes {

	private static String URL = "/testDefaultContentTypes";
	private static boolean debug = false;

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up.
	//====================================================================================================
	@Test
	public void testDefaultHeadersOnServletAnnotation() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		String url = URL + "/testDefaultHeadersOnServletAnnotation";

		client.setAccept("").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s1").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p2", r);

		client.setAccept("").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p1", r);

		client.setAccept("text/s1").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.setAccept("text/s2").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s2").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		try {
			client.setAccept("text/s3").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p3");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: [text/p1, text/p2]"
			);
		}

		try {
			client.setAccept("text/s3").setContentType("text/p3");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: [text/p1, text/p2]"
			);
		}

		client.closeQuietly();
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodParsersSerializers() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		String url = URL + "/testRestMethodParsersSerializers";

		try {
			client.setAccept("").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s1").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p1");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s1").setContentType("text/p1");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s2").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p2");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s2").setContentType("text/p2");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s3").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p3");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: [text/s3]"
			);
		}

		client.setAccept("text/s3").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodAddParsersSerializers() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		String url = URL + "/testRestMethodAddParsersSerializers";

		client.setAccept("").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s1").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p2", r);

		client.setAccept("").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p1", r);

		client.setAccept("text/s1").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.setAccept("text/s2").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s2").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s3").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p2", r);

		client.setAccept("").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p3", r);

		client.setAccept("text/s3").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		try {
			client.setAccept("").setContentType("text/p4");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			// Note that parsers defined on method are listed before parsers defined on class.
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: [text/p3, text/p1, text/p2]"
			);
		}

		try {
			client.setAccept("text/s4").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			// Note that serializers defined on method are listed before serializers defined on class.
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s4'",
				"Supported media-types: [text/s3, text/s1, text/s2]"
			);
		}

		client.closeQuietly();
	}

	//====================================================================================================
	// Various Accept incantations.
	//====================================================================================================
	@Test
	public void testAccept() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT).setContentType("text/p1");
		String r;

		String url = URL + "/testAccept";

		// "*/*" should match the first serializer, not the default serializer.
		client.setAccept("*/*");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		// "text/*" should match the first serializer, not the default serializer.
		client.setAccept("text/*");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		try {
			client.setAccept("bad/*");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'bad/*'",
				"Supported media-types: [text/s1, text/s2]"
			);
		}

		client.setAccept("bad/*,text/*");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.setAccept("text/*,bad/*");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.setAccept("text/s1;q=0.5,text/s2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p1", r);

		client.setAccept("text/s1,text/s2;q=0.5");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodParserSerializerAnnotations() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		String url = URL + "/testRestMethodParserSerializerAnnotations";

		client.setAccept("").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		try {
			client.setAccept("text/s1").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s1'",
				"Supported media-types: [text/s3]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p1");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s1").setContentType("text/p1");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s2").setContentType("");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: [text/s3]"
			);
		}

		try {
			client.setAccept("").setContentType("text/p2");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		try {
			client.setAccept("text/s2").setContentType("text/p2");
			r = client.doPut(url+"?noTrace=true", "").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: [text/p3]"
			);
		}

		client.setAccept("text/s3").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.setAccept("").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.setAccept("text/s3").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;

		String url = URL + "/testRestMethodAddParsersSerializersAnnotations";

		client.setAccept("").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.setAccept("text/s1").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p3", r);

		client.setAccept("").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p1", r);

		client.setAccept("text/s1").setContentType("text/p1");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s1/p1", r);

		client.setAccept("text/s2").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p3", r);

		client.setAccept("").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p2", r);

		client.setAccept("text/s2").setContentType("text/p2");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s2/p2", r);

		client.setAccept("text/s3").setContentType("");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.setAccept("").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.setAccept("text/s3").setContentType("text/p3");
		r = client.doPut(url, "").getResponseAsString();
		assertEquals("s3/p3", r);

		client.closeQuietly();
	}
}
