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
package org.apache.juneau.http.header;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class BasicMediaTypeHeader_Test extends TestBase {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo/bar";
	private static final MediaType PARSED = MediaType.of("foo/bar");

	@Rest
	public static class A {
		@RestOp
		public StringReader get(@Header(name=HEADER) @Schema(cf="multi") String[] h) {
			return reader(h == null ? "null" : Utils.join(h, '|'));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Method tests
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_basic() throws Exception {
		var c = client().build();

		// Normal usage.
		c.get().header(mediaTypeHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(mediaTypeHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(mediaTypeHeader(HEADER,(Supplier<MediaType>)null)).run().assertContent().isEmpty();
		c.get().header(mediaTypeHeader(HEADER,()->null)).run().assertContent().isEmpty();
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader("", VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader(null, VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader("", PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader(null, PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader("", ()->PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaTypeHeader(null, ()->PARSED));
	}

	@Test void a02_getType() {
		assertEquals("text", contentType("text/foo").getType());
		assertEquals("", new ContentType((String)null).getType());
	}

	@Test void a03_getSubType() {
		assertEquals("foo", contentType("text/foo").getSubType());
		assertEquals("*", new ContentType((String)null).getSubType());
	}

	@Test void a04_hasSubType() {
		assertTrue(contentType("text/foo+bar").hasSubType("bar"));
		assertFalse(contentType("text/foo+bar").hasSubType("baz"));
		assertFalse(contentType("text/foo+bar").hasSubType(null));
		assertFalse(new ContentType((String)null).hasSubType("bar"));
	}

	@Test void a05_getSubTypes() {
		assertList(contentType("text/foo+bar").getSubTypes(), "foo", "bar");
		assertList(new ContentType((String)null).getSubTypes(), "*");
	}

	@Test void a06_isMeta() {
		assertFalse(contentType("text/foo+bar").isMetaSubtype());
		assertTrue(contentType("text/*").isMetaSubtype());
		assertTrue(new ContentType((String)null).isMetaSubtype());
	}

	@Test void a07_match() {
		assertEquals(100000, contentType("text/foo").match(MediaType.of("text/foo"),true));
		assertEquals(0, new ContentType((String)null).match(MediaType.of("text/foo"),true));
	}

	@Test void a08_getParameters() {
		assertList(contentType("text/foo;x=1;y=2").getParameters(), "x=1", "y=2");
		assertEmpty(new ContentType((String)null).getParameters());
	}

	@Test void a09_getParameter() {
		assertEquals("1", contentType("text/foo;x=1;y=2").getParameter("x"));
		assertNull(contentType("text/foo;x=1;y=2").getParameter("z"));
		assertNull(contentType("text/foo;x=1;y=2").getParameter(null));
		assertNull(new ContentType((String)null).getParameter("x"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}