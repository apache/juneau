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
package org.apache.juneau.http.classic.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.http.classic.header.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link StringEntity}.
 */
class StringEntity_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ctor_default() throws Exception {
		var x = new StringEntity();
		assertEquals("", x.asString());
	}

	@Test void a02_ctor_contentTypeAndContent() throws Exception {
		var x = new StringEntity(ContentType.of("text/plain"), "foo");
		assertEquals("foo", x.asString());
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a03_ctor_nullContent_treatedAsEmpty() throws Exception {
		var x = new StringEntity(ContentType.of("text/plain"), null);
		assertEquals("", x.asString());
	}

	@Test void a04_copy() throws Exception {
		var x = new StringEntity(ContentType.of("text/plain"), "foo");
		var y = x.copy();
		assertNotSame(x, y);
		assertEquals("foo", y.asString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// asBytes() / asString() / getContent()
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_asBytes_notCached() throws Exception {
		var x = new StringEntity(null, "foo");
		assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), x.asBytes());
	}

	@Test void b02_asBytes_cached_cachesOnFirstCall() throws Exception {
		var x = new StringEntity(null, "foo").setCached();
		assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), x.asBytes());
		// Second call reads from the byteCache field rather than re-encoding.
		assertArrayEquals("foo".getBytes(StandardCharsets.UTF_8), x.asBytes());
	}

	@SuppressWarnings({
		"resource" // getContent() returns a ByteArrayInputStream that requires no closing.
	})
	@Test void b03_getContent_cached() throws Exception {
		var x = new StringEntity(null, "foo").setCached();
		assertEquals("foo", new String(x.getContent().readAllBytes(), StandardCharsets.UTF_8));
	}

	@SuppressWarnings({
		"resource" // getContent() returns a ByteArrayInputStream that requires no closing.
	})
	@Test void b04_getContent_notCached() throws Exception {
		var x = new StringEntity(null, "foo");
		assertEquals("foo", new String(x.getContent().readAllBytes(), StandardCharsets.UTF_8));
	}

	//------------------------------------------------------------------------------------------------------------------
	// getContentLength()
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_getContentLength_cached_usesByteLength() throws Exception {
		var x = new StringEntity(null, "foo").setCached();
		assertEquals(3, x.getContentLength());
	}

	@Test void c02_getContentLength_explicitlySet_takesPrecedence() {
		var x = new StringEntity(null, "foo").setContentLength(99);
		assertEquals(99, x.getContentLength());
	}

	@Test void c03_getContentLength_supplied_returnsMinusOne() {
		var x = new StringEntity();
		x.setContent(() -> "foo");
		assertEquals(-1, x.getContentLength());
	}

	@Test void c04_getContentLength_utf8_asciiOnly_returnsStringLength() {
		var x = new StringEntity(null, "foo");
		assertEquals(3, x.getContentLength());
	}

	@Test void c05_getContentLength_utf8_nonAscii_returnsMinusOne() {
		// A char > 127 makes the UTF-8 byte length diverge from the char length, so the fast-path bails to -1.
		var x = new StringEntity(null, "f\u00e9o");
		assertEquals(-1, x.getContentLength());
	}

	@Test void c06_getContentLength_nonUtf8Charset_returnsCharLength() {
		// getCharset() != UTF8, so the byte-scanning loop is skipped entirely and the char length is returned as-is.
		var x = new StringEntity(null, "foo").setCharset(StandardCharsets.ISO_8859_1);
		assertEquals(3, x.getContentLength());
	}

	//------------------------------------------------------------------------------------------------------------------
	// isRepeatable() / isStreaming()
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_isRepeatable() {
		assertTrue(new StringEntity().isRepeatable());
	}

	@Test void d02_isStreaming() {
		assertFalse(new StringEntity().isStreaming());
	}

	//------------------------------------------------------------------------------------------------------------------
	// unmodifiable()
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_unmodifiable_createsSnapshot() {
		var x = new StringEntity(null, "foo");
		var y = x.unmodifiable();
		assertNotSame(x, y);
		assertInstanceOf(StringEntity.Unmodifiable.class, y);
	}

	@Test void e02_unmodifiable_idempotent() {
		var x = new StringEntity(null, "foo").unmodifiable();
		assertSame(x, x.unmodifiable());
	}

	@Test void e03_unmodifiable_modifyThrows() {
		var x = new StringEntity(null, "foo").unmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> x.setContentLength(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// writeTo()
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_writeTo_cached() throws Exception {
		var x = new StringEntity(null, "foo").setCached();
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("foo", baos.toString(StandardCharsets.UTF_8));
	}

	@Test void f02_writeTo_notCached() throws Exception {
		var x = new StringEntity(null, "foo");
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("foo", baos.toString(StandardCharsets.UTF_8));
	}

	@Test void f03_writeTo_nullOut_throws() {
		var x = new StringEntity(null, "foo");
		assertThrows(IllegalArgumentException.class, () -> x.writeTo(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// equals() / hashCode()
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_equals_sameContent_isEqual() {
		var a = new StringEntity(null, "foo");
		var b = new StringEntity(null, "foo");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test void g02_equals_differentContent_notEqual() {
		var a = new StringEntity(null, "foo");
		var b = new StringEntity(null, "bar");
		assertNotEquals(a, b);
	}

	@Test void g03_equals_differentLeafType_notEqual() {
		var a = new StringEntity(null, "foo");
		var b = new ByteArrayEntity(null, "foo".getBytes(StandardCharsets.UTF_8));
		assertNotEquals(a, b);
	}

	@Test void g04_equals_unmodifiableSnapshot_stillEqual() {
		var a = new StringEntity(null, "foo");
		var b = a.unmodifiable();
		assertEquals(a, b);
	}
}
