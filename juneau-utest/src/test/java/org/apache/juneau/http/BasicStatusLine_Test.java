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
package org.apache.juneau.http;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BasicStatusLine}.
 */
class BasicStatusLine_Test extends TestBase {

	@Test void a01_create_noArgs() {
		var sl = BasicStatusLine.create();
		assertNotNull(sl);
		assertEquals(0, sl.getStatusCode());
	}

	@Test void a02_create_withStatusCodeAndReasonPhrase() {
		var sl = BasicStatusLine.create(200, "OK");
		assertEquals(200, sl.getStatusCode());
		assertEquals("OK", sl.getReasonPhrase());
	}

	@Test void a03_defaultProtocolVersion() {
		var sl = BasicStatusLine.create();
		var pv = sl.getProtocolVersion();
		assertNotNull(pv);
		assertEquals("HTTP", pv.getProtocol());
		assertEquals(1, pv.getMajor());
		assertEquals(1, pv.getMinor());
	}

	@Test void a04_setProtocolVersion() {
		var sl = BasicStatusLine.create();
		var pv = new ProtocolVersion("HTTP", 2, 0);
		sl.setProtocolVersion(pv);
		assertEquals(pv, sl.getProtocolVersion());
	}

	@Test void a05_setStatusCode() {
		var sl = BasicStatusLine.create().setStatusCode(404);
		assertEquals(404, sl.getStatusCode());
	}

	@Test void a06_setReasonPhrase() {
		var sl = BasicStatusLine.create().setReasonPhrase("Not Found");
		assertEquals("Not Found", sl.getReasonPhrase());
	}

	@Test void a07_setLocale() {
		var sl = BasicStatusLine.create().setLocale(Locale.FRENCH);
		assertEquals(Locale.FRENCH, sl.getLocale());
	}

	@Test void a08_setReasonPhraseCatalog() {
		var sl = BasicStatusLine.create()
			.setStatusCode(200)
			.setReasonPhraseCatalog(org.apache.http.impl.EnglishReasonPhraseCatalog.INSTANCE);
		// With a custom catalog, setReasonPhrase is null so it uses the catalog
		assertNotNull(sl.getReasonPhrase());
	}

	@Test void a09_getReasonPhrase_fromCatalog_whenNoReasonPhrase() {
		// No reason phrase set → uses default catalog
		var sl = BasicStatusLine.create().setStatusCode(200);
		assertEquals("OK", sl.getReasonPhrase());
	}

	@Test void a10_getReasonPhrase_fromExplicitPhrase() {
		var sl = BasicStatusLine.create().setStatusCode(200).setReasonPhrase("Custom");
		assertEquals("Custom", sl.getReasonPhrase());
	}

	@Test void a11_copy() {
		var sl = BasicStatusLine.create(404, "Not Found").setLocale(Locale.ENGLISH);
		var copy = sl.copy();
		assertNotSame(sl, copy);
		assertEquals(404, copy.getStatusCode());
		assertEquals("Not Found", copy.getReasonPhrase());
	}

	@Test void a12_setUnmodifiable_blocksModification() {
		var sl = BasicStatusLine.create().setStatusCode(200).setUnmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> sl.setStatusCode(404));
		assertThrows(UnsupportedOperationException.class, () -> sl.setReasonPhrase("Foo"));
		assertThrows(UnsupportedOperationException.class, () -> sl.setProtocolVersion(null));
		assertThrows(UnsupportedOperationException.class, () -> sl.setLocale(Locale.FRENCH));
		assertThrows(UnsupportedOperationException.class, () -> sl.setReasonPhraseCatalog(null));
	}

	@Test void a13_toString() {
		var sl = BasicStatusLine.create(200, "OK");
		var s = sl.toString();
		assertTrue(s.contains("200"));
		assertTrue(s.contains("OK"));
	}

	@Test void a14_equals_sameValues() {
		var sl1 = BasicStatusLine.create(200, "OK");
		var sl2 = BasicStatusLine.create(200, "OK");
		assertEquals(sl1, sl2);
	}

	@Test void a15_equals_differentStatusCode() {
		var sl1 = BasicStatusLine.create(200, "OK");
		var sl2 = BasicStatusLine.create(404, "Not Found");
		assertNotEquals(sl1, sl2);
	}

	@Test void a16_equals_differentReasonPhrase() {
		var sl1 = BasicStatusLine.create(200, "OK");
		var sl2 = BasicStatusLine.create(200, "Custom");
		assertNotEquals(sl1, sl2);
	}

	@Test void a17_equals_differentProtocolVersion() {
		var sl1 = BasicStatusLine.create(200, "OK");
		var sl2 = BasicStatusLine.create(200, "OK").setProtocolVersion(new ProtocolVersion("HTTP", 2, 0));
		assertNotEquals(sl1, sl2);
	}

	@Test void a18_equals_differentLocale() {
		var sl1 = BasicStatusLine.create(200, "OK").setLocale(Locale.ENGLISH);
		var sl2 = BasicStatusLine.create(200, "OK").setLocale(Locale.FRENCH);
		assertNotEquals(sl1, sl2);
	}

	@Test void a19_equals_nonStatusLine() {
		var sl = BasicStatusLine.create(200, "OK");
		assertNotEquals(sl, "not a status line");
		assertNotEquals(sl, null);
	}

	@Test void a20_hashCode_consistency() {
		var sl1 = BasicStatusLine.create(200, "OK");
		var sl2 = BasicStatusLine.create(200, "OK");
		assertEquals(sl1.hashCode(), sl2.hashCode());
	}
}
