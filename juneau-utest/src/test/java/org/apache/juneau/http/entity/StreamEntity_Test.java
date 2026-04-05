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
package org.apache.juneau.http.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class StreamEntity_Test extends TestBase {

	private static byte[] CONTENT = "hello world".getBytes(StandardCharsets.UTF_8);

	private static InputStream stream() {
		return new ByteArrayInputStream(CONTENT);
	}

	@Test void a01_basic() throws Exception {
		var x = new StreamEntity().setContent(stream());
		assertEquals("hello world", x.asString());
	}

	@Test void a02_constructor_contentType() throws Exception {
		var x = new StreamEntity(ContentType.TEXT_PLAIN, stream());
		assertEquals("hello world", x.asString());
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a03_asBytes_uncached() throws Exception {
		var x = new StreamEntity().setContent(stream());
		assertArrayEquals(CONTENT, x.asBytes());
	}

	@Test void a04_asBytes_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		assertTrue(x.isCached());
		// Call twice to verify cached path
		assertArrayEquals(CONTENT, x.asBytes());
		assertArrayEquals(CONTENT, x.asBytes());
	}

	@Test void a05_asString_uncached() throws Exception {
		var x = new StreamEntity().setContent(stream());
		assertEquals("hello world", x.asString());
	}

	@Test void a06_asString_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		// Call twice to verify cached path
		assertEquals("hello world", x.asString());
		assertEquals("hello world", x.asString());
	}

	@Test void a07_getContent_uncached() throws Exception {
		var x = new StreamEntity().setContent(stream());
		var content = x.getContent();
		assertEquals("hello world", new String(content.readAllBytes()));
	}

	@Test void a08_getContent_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		// Cached returns ByteArrayInputStream each time
		var c1 = x.getContent();
		assertEquals("hello world", new String(c1.readAllBytes()));
		var c2 = x.getContent();
		assertEquals("hello world", new String(c2.readAllBytes()));
	}

	@Test void a09_getContentLength_uncached() {
		var x = new StreamEntity().setContent(stream());
		assertEquals(-1L, x.getContentLength());
	}

	@Test void a10_getContentLength_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		assertEquals(11L, x.getContentLength());
	}

	@Test void a11_isRepeatable_uncached() {
		var x = new StreamEntity().setContent(stream());
		assertFalse(x.isRepeatable());
	}

	@Test void a12_isRepeatable_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		assertTrue(x.isRepeatable());
	}

	@Test void a13_isStreaming_uncached() {
		var x = new StreamEntity().setContent(stream());
		assertTrue(x.isStreaming());
	}

	@Test void a14_isStreaming_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		assertFalse(x.isStreaming());
	}

	@Test void a15_writeTo_uncached() throws Exception {
		var x = new StreamEntity().setContent(stream());
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("hello world", baos.toString());
	}

	@Test void a16_writeTo_cached() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached();
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("hello world", baos.toString());
	}

	@Test void a17_copy() throws Exception {
		var x = new StreamEntity().setContent(stream()).setContentType("text/plain").setCached();
		var copy = x.copy();
		assertNotSame(x, copy);
		assertEquals("hello world", copy.asString());
		assertEquals("text/plain", copy.getContentType().getValue());
	}

	@Test void a18_setChunked() {
		var x = new StreamEntity().setContent(stream()).setChunked();
		assertTrue(x.isChunked());
	}

	@Test void a19_setChunked_bool() {
		var x = new StreamEntity().setContent(stream()).setChunked(true);
		assertTrue(x.isChunked());
		x.setChunked(false);
		assertFalse(x.isChunked());
	}

	@Test void a20_setContentType_string() {
		var x = new StreamEntity().setContent(stream()).setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a21_setCharset() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCached().setCharset(StandardCharsets.UTF_8);
		assertEquals("hello world", x.asString());
	}

	@Test void a22_setMaxLength() throws Exception {
		var x = new StreamEntity().setContent(stream()).setMaxLength(5);
		assertEquals(5, x.getMaxLength());
		// maxLength is a buffer size hint, content is still fully readable
		assertArrayEquals(CONTENT, x.asBytes());
	}

	@Test void a23_setContentEncoding_string() {
		var x = new StreamEntity().setContent(stream()).setContentEncoding("gzip");
		assertEquals("gzip", x.getContentEncoding().getValue());
	}

	@Test void a24_setContentEncoding_object() {
		var x = new StreamEntity().setContent(stream()).setContentEncoding(ContentEncoding.of("identity"));
		assertEquals("identity", x.getContentEncoding().getValue());
	}

	@Test void a25_setContentLength() {
		var x = new StreamEntity().setContent(stream()).setContentLength(99L);
		assertEquals(99L, x.getContentLength());
	}

	@Test void a26_setContentType_object() {
		var x = new StreamEntity().setContent(stream()).setContentType(ContentType.TEXT_HTML);
		assertEquals("text/html", x.getContentType().getValue());
	}

	@Test void a27_setUnmodifiable() {
		var x = new StreamEntity().setContent(stream()).setUnmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> x.setChunked(true));
	}

	@Test void a28_setContent_supplier() throws Exception {
		var x = new StreamEntity().setContent(() -> stream());
		assertEquals("hello world", x.asString());
	}

	@Test void a29_asString_cached_charset() throws Exception {
		var x = new StreamEntity().setContent(stream()).setCharset(StandardCharsets.UTF_8).setCached();
		assertEquals("hello world", x.asString());
		// Second call uses cache
		assertEquals("hello world", x.asString());
	}
}
