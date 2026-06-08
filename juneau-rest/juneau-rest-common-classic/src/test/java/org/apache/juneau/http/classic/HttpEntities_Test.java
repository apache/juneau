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
package org.apache.juneau.http.classic;

import static org.apache.juneau.http.classic.HttpEntities.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.classic.entity.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.marshall.httppart.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link HttpEntities}.  Each factory method
 * (and each overload) is exercised at least once; null and supplier inputs
 * are checked where the contract permits {@code null}.
 */
class HttpEntities_Test extends TestBase {

	private static final byte[] BYTES = "hello".getBytes();
	private static final ContentType CT_JSON = ContentType.APPLICATION_JSON;

	// ------------------------------------------------------------------------------------------------------------------
	// byteArrayEntity — 4 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a01_byteArrayEntity_bytes() throws Exception {
		var e = byteArrayEntity(BYTES);
		assertInstanceOf(ByteArrayEntity.class, e);
		assertArrayEquals(BYTES, e.asBytes());
		assertNull(e.getContentType());
	}

	@Test void a02_byteArrayEntity_bytes_null() {
		var e = byteArrayEntity((byte[])null);
		assertNotNull(e);
	}

	@Test void a03_byteArrayEntity_bytes_withContentType() throws Exception {
		var e = byteArrayEntity(BYTES, CT_JSON);
		assertArrayEquals(BYTES, e.asBytes());
		assertEquals("application/json", e.getContentType().getValue());
	}

	@Test void a04_byteArrayEntity_bytes_withContentType_null() {
		var e = byteArrayEntity(BYTES, null);
		assertNull(e.getContentType());
	}

	@Test void a05_byteArrayEntity_supplier() throws Exception {
		Supplier<byte[]> s = () -> BYTES;
		var e = byteArrayEntity(s);
		assertArrayEquals(BYTES, e.asBytes());
	}

	@Test void a06_byteArrayEntity_supplier_null() {
		var e = byteArrayEntity((Supplier<byte[]>)null);
		assertNotNull(e);
	}

	@Test void a07_byteArrayEntity_supplier_withContentType() throws Exception {
		Supplier<byte[]> s = () -> BYTES;
		var e = byteArrayEntity(s, CT_JSON);
		assertArrayEquals(BYTES, e.asBytes());
		assertEquals("application/json", e.getContentType().getValue());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// fileEntity — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void b01_fileEntity_file() {
		var f = new File("/tmp/nonexistent-juneau-test-file.txt");
		var e = fileEntity(f);
		assertInstanceOf(FileEntity.class, e);
		assertNull(e.getContentType());
	}

	@Test void b02_fileEntity_file_null() {
		var e = fileEntity((File)null);
		assertNotNull(e);
	}

	@Test void b03_fileEntity_file_withContentType() {
		var f = new File("/tmp/nonexistent-juneau-test-file.txt");
		var e = fileEntity(f, CT_JSON);
		assertEquals("application/json", e.getContentType().getValue());
	}

	@Test void b04_fileEntity_file_withContentType_null() {
		var e = fileEntity(new File("/tmp/x"), null);
		assertNull(e.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// readerEntity — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void c01_readerEntity_reader() throws Exception {
		var r = new StringReader("hello");
		var e = readerEntity(r);
		assertInstanceOf(ReaderEntity.class, e);
		assertEquals("hello", e.asString());
	}

	@Test void c02_readerEntity_reader_null() {
		var e = readerEntity((Reader)null);
		assertNotNull(e);
	}

	@Test void c03_readerEntity_reader_withContentType() throws Exception {
		var r = new StringReader("hello");
		var e = readerEntity(r, CT_JSON);
		assertEquals("application/json", e.getContentType().getValue());
		assertEquals("hello", e.asString());
	}

	@Test void c04_readerEntity_reader_withContentType_null() {
		var e = readerEntity(new StringReader(""), null);
		assertNull(e.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// serializedEntity — 4 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void d01_serializedEntity_object_serializer() {
		var e = serializedEntity("hello", JsonSerializer.DEFAULT);
		assertInstanceOf(SerializedEntity.class, e);
	}

	@Test void d02_serializedEntity_object_nullSerializer() {
		// Null serializer is permitted — entity falls back to toString().
		var e = serializedEntity("hello", null);
		assertNotNull(e);
	}

	@Test void d03_serializedEntity_object_serializer_schema() {
		var schema = HttpPartSchema.create().build();
		var e = serializedEntity("hello", JsonSerializer.DEFAULT, schema);
		assertNotNull(e);
	}

	@Test void d04_serializedEntity_object_serializer_nullSchema() {
		var e = serializedEntity("hello", JsonSerializer.DEFAULT, null);
		assertNotNull(e);
	}

	@Test void d05_serializedEntity_supplier_serializer() {
		Supplier<?> s = () -> "lazy";
		var e = serializedEntity(s, JsonSerializer.DEFAULT);
		assertNotNull(e);
	}

	@Test void d06_serializedEntity_supplier_serializer_schema() {
		Supplier<?> s = () -> "lazy";
		var schema = HttpPartSchema.create().build();
		var e = serializedEntity(s, JsonSerializer.DEFAULT, schema);
		assertNotNull(e);
	}

	@Test void d07_serializedEntity_supplier_nullSerializer_nullSchema() {
		Supplier<?> s = () -> "lazy";
		var e = serializedEntity(s, null, null);
		assertNotNull(e);
	}

	@Test void d08_serializedEntity_nullContent() {
		var e = serializedEntity((Object)null, JsonSerializer.DEFAULT);
		assertNotNull(e);
	}

	// ------------------------------------------------------------------------------------------------------------------
	// streamEntity — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void e01_streamEntity_inputStream() {
		var in = new ByteArrayInputStream(BYTES);
		var e = streamEntity(in);
		assertInstanceOf(StreamEntity.class, e);
	}

	@Test void e02_streamEntity_inputStream_null() {
		var e = streamEntity((InputStream)null);
		assertNotNull(e);
	}

	@Test void e03_streamEntity_inputStream_lengthAndContentType() {
		var in = new ByteArrayInputStream(BYTES);
		var e = streamEntity(in, BYTES.length, CT_JSON);
		assertEquals(BYTES.length, e.getContentLength());
		assertEquals("application/json", e.getContentType().getValue());
	}

	@Test void e04_streamEntity_inputStream_unknownLength() {
		var in = new ByteArrayInputStream(BYTES);
		var e = streamEntity(in, -1L, null);
		assertEquals(-1L, e.getContentLength());
		assertNull(e.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// stringEntity — 4 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void f01_stringEntity_string() throws Exception {
		var e = stringEntity("hello");
		assertInstanceOf(StringEntity.class, e);
		assertEquals("hello", e.asString());
	}

	@Test void f02_stringEntity_string_null() {
		var e = stringEntity((String)null);
		assertNotNull(e);
	}

	@Test void f03_stringEntity_string_blank() throws Exception {
		var e = stringEntity("");
		assertEquals("", e.asString());
	}

	@Test void f04_stringEntity_string_withContentType() throws Exception {
		var e = stringEntity("hello", CT_JSON);
		assertEquals("hello", e.asString());
		assertEquals("application/json", e.getContentType().getValue());
	}

	@Test void f05_stringEntity_string_withContentType_null() {
		var e = stringEntity("hello", null);
		assertNull(e.getContentType());
	}

	@Test void f06_stringEntity_supplier() throws Exception {
		Supplier<String> s = () -> "lazy";
		var e = stringEntity(s);
		assertEquals("lazy", e.asString());
	}

	@Test void f07_stringEntity_supplier_null() {
		var e = stringEntity((Supplier<String>)null);
		assertNotNull(e);
	}

	@Test void f08_stringEntity_supplier_withContentType() throws Exception {
		Supplier<String> s = () -> "lazy";
		var e = stringEntity(s, CT_JSON);
		assertEquals("lazy", e.asString());
		assertEquals("application/json", e.getContentType().getValue());
	}

	@Test void f09_stringEntity_supplier_withContentType_null() {
		Supplier<String> s = () -> "lazy";
		var e = stringEntity(s, null);
		assertNull(e.getContentType());
	}
}
