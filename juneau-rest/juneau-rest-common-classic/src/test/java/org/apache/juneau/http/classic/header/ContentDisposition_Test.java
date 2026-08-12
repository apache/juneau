/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.http.classic.header;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.http.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link ContentDisposition}.
 */
class ContentDisposition_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// attachment()
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_attachment_basic() {
		var h = ContentDisposition.attachment("report.pdf");
		assertEquals("attachment; filename=\"report.pdf\"", h.getValue());
	}

	@Test void a02_attachment_escapesQuotesAndBackslashes() {
		var h = ContentDisposition.attachment("a\"b\\c");
		assertEquals("attachment; filename=\"a\\\"b\\\\c\"", h.getValue());
	}

	@Test void a03_attachment_null_throws() {
		assertThrows(IllegalArgumentException.class, () -> ContentDisposition.attachment(null));
	}

	@Test void a04_attachment_blank_throws() {
		assertThrows(IllegalArgumentException.class, () -> ContentDisposition.attachment("   "));
	}

	@Test void a05_attachment_containsCr_throws() {
		assertThrows(IllegalArgumentException.class, () -> ContentDisposition.attachment("foo\rbar"));
	}

	@Test void a06_attachment_containsLf_throws() {
		assertThrows(IllegalArgumentException.class, () -> ContentDisposition.attachment("foo\nbar"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Static creators
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_of_wireString() {
		var h = ContentDisposition.of("attachment; filename=\"foo.txt\"");
		assertEquals("Content-Disposition", h.getName());
		assertEquals("attachment; filename=\"foo.txt\"", h.getValue());
	}

	@Test void b02_of_wireString_null_returnsNull() {
		assertNull(ContentDisposition.of((String)null));
	}

	@Test void b03_of_wireString_cached_returnsSameInstanceForSameValue() {
		var h1 = ContentDisposition.of("attachment; filename=\"same.txt\"");
		var h2 = ContentDisposition.of("attachment; filename=\"same.txt\"");
		assertSame(h1, h2);
	}

	@Test void b04_of_typedValue() {
		var ranges = StringRanges.of("attachment");
		var h = ContentDisposition.of(ranges);
		assertEquals("attachment", h.getValue());
	}

	@Test void b05_of_typedValue_null_returnsNull() {
		assertNull(ContentDisposition.of((StringRanges)null));
	}

	@Test void b06_of_supplier_delaysEvaluation() {
		var calls = new int[1];
		var h = ContentDisposition.of((Supplier<StringRanges>) () -> { calls[0]++; return StringRanges.of("attachment"); });
		assertEquals(0, calls[0]);
		assertEquals("attachment", h.getValue());
		assertEquals(1, calls[0]);
	}

	@Test void b07_of_supplier_null_returnsNull() {
		assertNull(ContentDisposition.of((Supplier<StringRanges>)null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_ctor_wireString() {
		assertEquals("attachment", new ContentDisposition("attachment").getValue());
	}

	@Test void c02_ctor_typedValue() {
		assertEquals("attachment", new ContentDisposition(StringRanges.of("attachment")).getValue());
	}
}
