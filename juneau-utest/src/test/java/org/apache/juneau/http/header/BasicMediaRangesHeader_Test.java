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

class BasicMediaRangesHeader_Test extends SimpleTestBase {

	private static final String HEADER = "Foo";
	private static final String VALUE = "foo/bar;x=1";
	private static final MediaRanges PARSED = MediaRanges.of("foo/bar;x=1");

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
		c.get().header(mediaRangesHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaRangesHeader(HEADER,VALUE)).run().assertContent(VALUE);
		c.get().header(mediaRangesHeader(HEADER,PARSED)).run().assertContent(VALUE);
		c.get().header(mediaRangesHeader(HEADER,()->PARSED)).run().assertContent(VALUE);

		// Invalid usage.
		c.get().header(mediaRangesHeader(HEADER,(Supplier<MediaRanges>)null)).run().assertContent().isEmpty();
		c.get().header(mediaRangesHeader(HEADER,()->null)).run().assertContent().isEmpty();
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader("", VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader(null, VALUE));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader("", PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader(null, PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader("", ()->PARSED));
		assertThrowsWithMessage(IllegalArgumentException.class, "Name cannot be empty on header.", ()->mediaRangesHeader(null, ()->PARSED));
	}

	@Test void a02_match() {
		assertEquals(0, accept("text/foo").match(alist(MediaType.of("text/foo"))));
		assertEquals(-1, accept("text/foo").match(alist(MediaType.of("text/bar"))));
		assertEquals(-1, new Accept((String)null).match(alist(MediaType.of("text/bar"))));
		assertEquals(-1, accept("text/foo").match(alist(MediaType.of(null))));
		assertEquals(-1, accept("text/foo").match(null));
	}

	@Test void a03_getRange() {
		assertEquals("text/foo", s(accept("text/foo").getRange(0)));
		assertNull(accept("text/foo").getRange(1));
		assertNull(accept("text/foo").getRange(-1));
		assertNull(new Accept((String)null).getRange(0));
	}

	@Test void a04_hasSubtypePart() {
		assertTrue(accept("text/foo").hasSubtypePart("foo"));
		assertFalse(accept("text/foo").hasSubtypePart("bar"));
		assertFalse(accept("text/foo").hasSubtypePart(null));
		assertFalse(new Accept((String)null).hasSubtypePart("foo"));
	}

	@Test void a05_getRanges() {
		assertList(accept("text/foo,text/bar").toMediaRanges().toList(), "text/foo" ,"text/bar");
		assertNull(new Accept((String)null).toMediaRanges());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper methods.
	//------------------------------------------------------------------------------------------------------------------

	private static RestClient.Builder client() {
		return MockRestClient.create(A.class);
	}
}