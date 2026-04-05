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
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class FileEntity_Test extends TestBase {

	private static File tempFile;

	@BeforeAll
	static void setupTempFile() throws Exception {
		tempFile = File.createTempFile("FileEntity_Test", ".txt");
		tempFile.deleteOnExit();
		Files.writeString(tempFile.toPath(), "hello world");
	}

	@Test void a01_basic() throws Exception {
		var x = new FileEntity().setContent(tempFile);
		assertEquals("hello world", x.asString());
	}

	@Test void a02_constructor_contentType() throws Exception {
		var x = new FileEntity(ContentType.TEXT_PLAIN, tempFile);
		assertEquals("hello world", x.asString());
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a03_asBytes_uncached() throws Exception {
		var x = new FileEntity().setContent(tempFile);
		assertArrayEquals("hello world".getBytes(), x.asBytes());
	}

	@Test void a04_asBytes_cached() throws Exception {
		var x = new FileEntity().setContent(tempFile).setCached();
		assertTrue(x.isCached());
		// Call twice to verify cached path
		assertArrayEquals("hello world".getBytes(), x.asBytes());
		assertArrayEquals("hello world".getBytes(), x.asBytes());
	}

	@Test void a05_asString_uncached() throws Exception {
		var x = new FileEntity().setContent(tempFile);
		assertEquals("hello world", x.asString());
	}

	@Test void a06_asString_cached() throws Exception {
		var x = new FileEntity().setContent(tempFile).setCached();
		// Call twice to verify cached path
		assertEquals("hello world", x.asString());
		assertEquals("hello world", x.asString());
	}

	@Test void a07_getContent_uncached() throws Exception {
		var x = new FileEntity().setContent(tempFile);
		var content = x.getContent();
		assertEquals("hello world", new String(content.readAllBytes()));
	}

	@Test void a08_getContent_cached() throws Exception {
		var x = new FileEntity().setContent(tempFile).setCached();
		// Call twice - cached returns ByteArrayInputStream each time
		var c1 = x.getContent();
		assertEquals("hello world", new String(c1.readAllBytes()));
		var c2 = x.getContent();
		assertEquals("hello world", new String(c2.readAllBytes()));
	}

	@Test void a09_getContentLength() {
		var x = new FileEntity().setContent(tempFile);
		assertEquals(11L, x.getContentLength());
	}

	@Test void a10_isRepeatable() {
		var x = new FileEntity().setContent(tempFile);
		assertTrue(x.isRepeatable());
	}

	@Test void a11_writeTo_uncached() throws Exception {
		var x = new FileEntity().setContent(tempFile);
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("hello world", baos.toString());
	}

	@Test void a12_writeTo_cached() throws Exception {
		var x = new FileEntity().setContent(tempFile).setCached();
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("hello world", baos.toString());
	}

	@Test void a13_copy() throws Exception {
		var x = new FileEntity().setContent(tempFile).setContentType("text/plain");
		var copy = x.copy();
		assertNotSame(x, copy);
		assertEquals("hello world", copy.asString());
		assertEquals("text/plain", copy.getContentType().getValue());
	}

	@Test void a14_setChunked() {
		var x = new FileEntity().setContent(tempFile).setChunked();
		assertTrue(x.isChunked());
	}

	@Test void a15_setChunked_bool() {
		var x = new FileEntity().setContent(tempFile).setChunked(true);
		assertTrue(x.isChunked());
		x.setChunked(false);
		assertFalse(x.isChunked());
	}

	@Test void a16_setContentType_string() throws Exception {
		var x = new FileEntity().setContent(tempFile).setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a17_setCharset() throws Exception {
		var x = new FileEntity().setContent(tempFile).setCharset(StandardCharsets.UTF_8);
		assertEquals("hello world", x.asString());
	}

	@Test void a18_setMaxLength() throws Exception {
		var x = new FileEntity().setContent(tempFile).setMaxLength(5);
		assertEquals(5, x.getMaxLength());
		// maxLength is a buffer size hint, content is still fully readable
		assertEquals("hello world", x.asString());
	}

	@Test void a19_setContentEncoding_string() {
		var x = new FileEntity().setContent(tempFile).setContentEncoding("gzip");
		assertEquals("gzip", x.getContentEncoding().getValue());
	}

	@Test void a20_setContentEncoding_object() {
		var x = new FileEntity().setContent(tempFile).setContentEncoding(ContentEncoding.of("identity"));
		assertEquals("identity", x.getContentEncoding().getValue());
	}

	@Test void a21_setContentLength() {
		// FileEntity overrides getContentLength() to return actual file size
		var x = new FileEntity().setContent(tempFile).setContentLength(99L);
		assertEquals(11L, x.getContentLength());
	}

	@Test void a22_setContentType_object() throws Exception {
		var x = new FileEntity().setContent(tempFile).setContentType(ContentType.TEXT_HTML);
		assertEquals("text/html", x.getContentType().getValue());
	}

	@Test void a23_setUnmodifiable() {
		var x = new FileEntity().setContent(tempFile).setUnmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> x.setChunked(true));
	}

	@Test void a24_setContent_supplier() throws Exception {
		var x = new FileEntity().setContent(() -> tempFile);
		assertEquals("hello world", x.asString());
	}

	@Test void a25_asString_maxLength_cached() throws Exception {
		var x = new FileEntity().setContent(tempFile).setMaxLength(5).setCached();
		// maxLength is a buffer size hint; full content is still readable
		assertEquals("hello world", x.asString());
		// Second call uses cache
		assertEquals("hello world", x.asString());
	}
}
