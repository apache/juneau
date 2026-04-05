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

import static org.apache.juneau.http.HttpEntities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

class SerializedEntity_Test extends TestBase {

	@Test void a01_basic_noSerializer() throws Exception {
		var x = new SerializedEntity().setContent("hello world");
		assertEquals("hello world", x.asString());
	}

	@Test void a02_basic_withSerializer() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT);
		assertEquals("\"hello\"", x.asString());
	}

	@Test void a03_constructor_full() throws Exception {
		var x = new SerializedEntity(ContentType.APPLICATION_JSON, "hello", JsonSerializer.DEFAULT, null);
		assertEquals("\"hello\"", x.asString());
		assertEquals("application/json", x.getContentType().getValue());
	}

	@Test void a04_copy() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT).setContentType("text/plain");
		var copy = x.copy();
		assertNotSame(x, copy);
		assertEquals("\"hello\"", copy.asString());
	}

	@Test void a05_copyWith_noChange() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT);
		var copy = x.copyWith(null, null);
		assertSame(x, copy);
	}

	@Test void a06_copyWith_newSerializer() throws Exception {
		var x = new SerializedEntity().setContent("hello");
		var copy = x.copyWith(JsonSerializer.DEFAULT, null);
		assertNotSame(x, copy);
		assertEquals("\"hello\"", copy.asString());
	}

	@Test void a07_getContent() {
		var x = serializedEntity("hello", null);
		var is = x.getContent();
		assertNotNull(is);
	}

	@Test void a08_getContentLength() {
		var x = serializedEntity("hello", null);
		assertEquals(-1L, x.getContentLength());
	}

	@Test void a09_getContentType_fromSerializer() {
		var x = new SerializedEntity().setContent("hello").setSerializer(JsonSerializer.DEFAULT);
		assertNotNull(x.getContentType());
	}

	@Test void a10_getContentType_explicit() {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT).setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a11_isRepeatable() {
		var x = serializedEntity("hello", null);
		assertTrue(x.isRepeatable());
	}

	@Test void a12_writeTo_noSerializer() throws Exception {
		var x = serializedEntity("hello", null);
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("hello", baos.toString(StandardCharsets.UTF_8));
	}

	@Test void a13_writeTo_withSerializer() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT);
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertEquals("\"hello\"", baos.toString(StandardCharsets.UTF_8));
	}

	@Test void a14_setCached() throws Exception {
		var x = serializedEntity("hello", null).setCached();
		assertTrue(x.isCached());
	}

	@Test void a15_setChunked() {
		var x = serializedEntity("hello", null).setChunked();
		assertTrue(x.isChunked());
	}

	@Test void a16_setChunked_bool() {
		var x = serializedEntity("hello", null).setChunked(true);
		assertTrue(x.isChunked());
		x.setChunked(false);
		assertFalse(x.isChunked());
	}

	@Test void a17_setCharset() throws Exception {
		var x = serializedEntity("hello", null).setCharset(StandardCharsets.UTF_8);
		assertEquals("hello", x.asString());
	}

	@Test void a18_setContentEncoding_string() {
		var x = serializedEntity("hello", null).setContentEncoding("gzip");
		assertEquals("gzip", x.getContentEncoding().getValue());
	}

	@Test void a19_setContentEncoding_object() {
		var x = serializedEntity("hello", null).setContentEncoding(ContentEncoding.of("identity"));
		assertEquals("identity", x.getContentEncoding().getValue());
	}

	@Test void a20_setContentLength() {
		// SerializedEntity overrides getContentLength() to always return -1
		var x = serializedEntity("hello", null).setContentLength(99L);
		assertEquals(-1L, x.getContentLength());
	}

	@Test void a21_setContentType_string() {
		var x = serializedEntity("hello", null).setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a22_setContentType_object() {
		var x = serializedEntity("hello", null).setContentType(ContentType.TEXT_HTML);
		assertEquals("text/html", x.getContentType().getValue());
	}

	@Test void a23_setMaxLength() {
		var x = serializedEntity("hello", null).setMaxLength(5);
		assertEquals(5, x.getMaxLength());
	}

	@Test void a24_setSerializer() throws Exception {
		var x = new SerializedEntity().setContent("hello").setSerializer(JsonSerializer.DEFAULT);
		assertEquals("\"hello\"", x.asString());
	}

	@Test void a25_setSchema_null() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT).setSchema(null);
		assertEquals("\"hello\"", x.asString());
	}

	@Test void a26_setUnmodifiable() {
		var x = serializedEntity("hello", null).setUnmodifiable();
		assertThrows(UnsupportedOperationException.class, () -> x.setChunked(true));
	}

	@Test void a27_setContent_supplier() throws Exception {
		var x = new SerializedEntity().setContent(() -> "hello");
		assertEquals("hello", x.asString());
	}

	@Test void a28_copyWith_alreadySet() throws Exception {
		var x = serializedEntity("hello", JsonSerializer.DEFAULT);
		// copyWith returns same instance when serializer is already set
		var copy = x.copyWith(JsonSerializer.DEFAULT, null);
		assertSame(x, copy);
	}
}
