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


public class DefaultContentTypesTest extends RestTestcase {

	private static String URL = "/testDefaultContentTypes";
	private static boolean debug = false;

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up.
	//====================================================================================================
	@Test
	public void testDefaultHeadersOnServletAnnotation() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r;

		String url = URL + "/testDefaultHeadersOnServletAnnotation";

		r = client.doPut(url, "").accept("").contentType("").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s1").contentType("").getResponseAsString();
		assertEquals("s1/p2", r);

		r = client.doPut(url, "").accept("").contentType("text/p1").getResponseAsString();
		assertEquals("s2/p1", r);

		r = client.doPut(url, "").accept("text/s1").contentType("text/p1").getResponseAsString();
		assertEquals("s1/p1", r);

		r = client.doPut(url, "").accept("text/s2").contentType("").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("").contentType("text/p2").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s2").contentType("text/p2").getResponseAsString();
		assertEquals("s2/p2", r);

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s3").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s3'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p3").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: ['text/p1','text/p2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s3").contentType("text/p3").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p3'",
				"Supported media-types: ['text/p1','text/p2']"
			);
		}
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodParsersSerializers() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r;

		String url = URL + "/testRestMethodParsersSerializers";

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s1").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s1").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s2").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p2").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s2").contentType("text/p2").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s3").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p3").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: ['text/s3']"
			);
		}

		r = client.doPut(url, "").accept("text/s3").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodAddParsersSerializers() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r;

		String url = URL + "/testRestMethodAddParsersSerializers";

		r = client.doPut(url, "").accept("").contentType("").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s1").contentType("").getResponseAsString();
		assertEquals("s1/p2", r);

		r = client.doPut(url, "").accept("").contentType("text/p1").getResponseAsString();
		assertEquals("s2/p1", r);

		r = client.doPut(url, "").accept("text/s1").contentType("text/p1").getResponseAsString();
		assertEquals("s1/p1", r);

		r = client.doPut(url, "").accept("text/s2").contentType("").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("").contentType("text/p2").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s2").contentType("text/p2").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s3").contentType("").getResponseAsString();
		assertEquals("s3/p2", r);

		r = client.doPut(url, "").accept("").contentType("text/p3").getResponseAsString();
		assertEquals("s2/p3", r);

		r = client.doPut(url, "").accept("text/s3").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p4").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			// Note that parsers defined on method are listed before parsers defined on class.
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p4'",
				"Supported media-types: ['text/p3','text/p1','text/p2']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s4").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			// Note that serializers defined on method are listed before serializers defined on class.
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s4'",
				"Supported media-types: ['text/s3','text/s1','text/s2']"
			);
		}
	}

	//====================================================================================================
	// Various Accept incantations.
	//====================================================================================================
	@Test
	public void testAccept() throws Exception {
		RestClient client = TestMicroservice.client().contentType("text/p1").build();
		String r;

		String url = URL + "/testAccept";

		// "*/*" should match the first serializer, not the default serializer.
		r = client.doPut(url, "").accept("*/*").getResponseAsString();
		assertEquals("s1/p1", r);

		// "text/*" should match the first serializer, not the default serializer.
		r = client.doPut(url, "").accept("text/*").getResponseAsString();
		assertEquals("s1/p1", r);

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("bad/*").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'bad/*'",
				"Supported media-types: ['text/s1','text/s2']"
			);
		}

		r = client.doPut(url, "").accept("bad/*,text/*").getResponseAsString();
		assertEquals("s1/p1", r);

		r = client.doPut(url, "").accept("text/*,bad/*").getResponseAsString();
		assertEquals("s1/p1", r);

		r = client.doPut(url, "").accept("text/s1;q=0.5,text/s2").getResponseAsString();
		assertEquals("s2/p1", r);

		r = client.doPut(url, "").accept("text/s1,text/s2;q=0.5").getResponseAsString();
		assertEquals("s1/p1", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodParserSerializerAnnotations() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r;

		String url = URL + "/testRestMethodParserSerializerAnnotations";

		r = client.doPut(url, "").accept("").contentType("").getResponseAsString();
		assertEquals("s3/p3", r);

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s1").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s1'",
				"Supported media-types: ['text/s3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s1").contentType("text/p1").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p1'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s2").contentType("").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_NOT_ACCEPTABLE,
				"Unsupported media-type in request header 'Accept': 'text/s2'",
				"Supported media-types: ['text/s3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("").contentType("text/p2").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		try {
			r = client.doPut(url+"?noTrace=true", "").accept("text/s2").contentType("text/p2").getResponseAsString();
			fail("Exception expected");
		} catch (RestCallException e) {
			checkErrorResponse(debug, e, SC_UNSUPPORTED_MEDIA_TYPE,
				"Unsupported media-type in request header 'Content-Type': 'text/p2'",
				"Supported media-types: ['text/p3']"
			);
		}

		r = client.doPut(url, "").accept("text/s3").contentType("").getResponseAsString();
		assertEquals("s3/p3", r);

		r = client.doPut(url, "").accept("").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);

		r = client.doPut(url, "").accept("text/s3").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@Test
	public void testRestMethodAddParsersSerializersAnnotations() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r;

		String url = URL + "/testRestMethodAddParsersSerializersAnnotations";

		r = client.doPut(url, "").accept("").contentType("").getResponseAsString();
		assertEquals("s3/p3", r);

		r = client.doPut(url, "").accept("text/s1").contentType("").getResponseAsString();
		assertEquals("s1/p3", r);

		r = client.doPut(url, "").accept("").contentType("text/p1").getResponseAsString();
		assertEquals("s3/p1", r);

		r = client.doPut(url, "").accept("text/s1").contentType("text/p1").getResponseAsString();
		assertEquals("s1/p1", r);

		r = client.doPut(url, "").accept("text/s2").contentType("").getResponseAsString();
		assertEquals("s2/p3", r);

		r = client.doPut(url, "").accept("").contentType("text/p2").getResponseAsString();
		assertEquals("s3/p2", r);

		r = client.doPut(url, "").accept("text/s2").contentType("text/p2").getResponseAsString();
		assertEquals("s2/p2", r);

		r = client.doPut(url, "").accept("text/s3").contentType("").getResponseAsString();
		assertEquals("s3/p3", r);

		r = client.doPut(url, "").accept("").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);

		r = client.doPut(url, "").accept("text/s3").contentType("text/p3").getResponseAsString();
		assertEquals("s3/p3", r);
	}
}
