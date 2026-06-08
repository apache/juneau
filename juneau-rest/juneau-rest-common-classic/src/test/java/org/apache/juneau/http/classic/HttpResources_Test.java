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

import static org.apache.juneau.http.classic.HttpResources.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.classic.header.*;
import org.apache.juneau.http.classic.resource.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link HttpResources}.  Each factory method
 * (and each overload) is exercised, including null-input and blank-input
 * variants.
 */
class HttpResources_Test extends TestBase {

	private static final byte[] BYTES = "hello".getBytes();
	private static final ContentType CT_JSON = ContentType.APPLICATION_JSON;

	// ------------------------------------------------------------------------------------------------------------------
	// byteArrayResource — 4 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void a01_byteArrayResource_bytes() throws Exception {
		var r = byteArrayResource(BYTES);
		assertInstanceOf(ByteArrayResource.class, r);
		assertArrayEquals(BYTES, r.asBytes());
		assertNull(r.getContentType());
	}

	@Test void a02_byteArrayResource_bytes_null() {
		var r = byteArrayResource((byte[])null);
		assertNotNull(r);
	}

	@Test void a03_byteArrayResource_bytes_withContentType() throws Exception {
		var r = byteArrayResource(BYTES, CT_JSON);
		assertArrayEquals(BYTES, r.asBytes());
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void a04_byteArrayResource_bytes_withContentType_null() {
		var r = byteArrayResource(BYTES, null);
		assertNull(r.getContentType());
	}

	@Test void a05_byteArrayResource_supplier() throws Exception {
		Supplier<byte[]> s = () -> BYTES;
		var r = byteArrayResource(s);
		assertArrayEquals(BYTES, r.asBytes());
	}

	@Test void a06_byteArrayResource_supplier_null() {
		var r = byteArrayResource((Supplier<byte[]>)null);
		assertNotNull(r);
	}

	@Test void a07_byteArrayResource_supplier_withContentType() throws Exception {
		Supplier<byte[]> s = () -> BYTES;
		var r = byteArrayResource(s, CT_JSON);
		assertArrayEquals(BYTES, r.asBytes());
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void a08_byteArrayResource_supplier_withContentType_null() {
		Supplier<byte[]> s = () -> BYTES;
		var r = byteArrayResource(s, null);
		assertNull(r.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// fileResource — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void b01_fileResource_file() {
		var f = new File("/tmp/nonexistent-juneau-test-file.txt");
		var r = fileResource(f);
		assertInstanceOf(FileResource.class, r);
		assertNull(r.getContentType());
	}

	@Test void b02_fileResource_file_null() {
		var r = fileResource((File)null);
		assertNotNull(r);
	}

	@Test void b03_fileResource_file_withContentType() {
		var f = new File("/tmp/nonexistent-juneau-test-file.txt");
		var r = fileResource(f, CT_JSON);
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void b04_fileResource_file_withContentType_null() {
		var r = fileResource(new File("/tmp/x"), null);
		assertNull(r.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// readerResource — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void c01_readerResource_reader() throws Exception {
		var rd = new StringReader("hello");
		var r = readerResource(rd);
		assertInstanceOf(ReaderResource.class, r);
		assertEquals("hello", r.asString());
	}

	@Test void c02_readerResource_reader_null() {
		var r = readerResource((Reader)null);
		assertNotNull(r);
	}

	@Test void c03_readerResource_reader_withContentType() throws Exception {
		var rd = new StringReader("hello");
		var r = readerResource(rd, CT_JSON);
		assertEquals("application/json", r.getContentType().getValue());
		assertEquals("hello", r.asString());
	}

	@Test void c04_readerResource_reader_withContentType_null() {
		var r = readerResource(new StringReader(""), null);
		assertNull(r.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// streamResource — 2 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void d01_streamResource_inputStream() {
		var in = new ByteArrayInputStream(BYTES);
		var r = streamResource(in);
		assertInstanceOf(StreamResource.class, r);
	}

	@Test void d02_streamResource_inputStream_null() {
		var r = streamResource((InputStream)null);
		assertNotNull(r);
	}

	@Test void d03_streamResource_inputStream_lengthAndContentType() {
		var in = new ByteArrayInputStream(BYTES);
		var r = streamResource(in, BYTES.length, CT_JSON);
		assertEquals(BYTES.length, r.getContentLength());
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void d04_streamResource_inputStream_unknownLength_nullCT() {
		var in = new ByteArrayInputStream(BYTES);
		var r = streamResource(in, -1L, null);
		assertEquals(-1L, r.getContentLength());
		assertNull(r.getContentType());
	}

	// ------------------------------------------------------------------------------------------------------------------
	// stringResource — 4 overloads
	// ------------------------------------------------------------------------------------------------------------------

	@Test void e01_stringResource_string() throws Exception {
		var r = stringResource("hello");
		assertInstanceOf(StringResource.class, r);
		assertEquals("hello", r.asString());
	}

	@Test void e02_stringResource_string_null() {
		var r = stringResource((String)null);
		assertNotNull(r);
	}

	@Test void e03_stringResource_string_blank() throws Exception {
		var r = stringResource("");
		assertEquals("", r.asString());
	}

	@Test void e04_stringResource_string_withContentType() throws Exception {
		var r = stringResource("hello", CT_JSON);
		assertEquals("hello", r.asString());
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void e05_stringResource_string_withContentType_null() {
		var r = stringResource("hello", null);
		assertNull(r.getContentType());
	}

	@Test void e06_stringResource_supplier() throws Exception {
		Supplier<String> s = () -> "lazy";
		var r = stringResource(s);
		assertEquals("lazy", r.asString());
	}

	@Test void e07_stringResource_supplier_null() {
		var r = stringResource((Supplier<String>)null);
		assertNotNull(r);
	}

	@Test void e08_stringResource_supplier_withContentType() throws Exception {
		Supplier<String> s = () -> "lazy";
		var r = stringResource(s, CT_JSON);
		assertEquals("lazy", r.asString());
		assertEquals("application/json", r.getContentType().getValue());
	}

	@Test void e09_stringResource_supplier_withContentType_null() {
		Supplier<String> s = () -> "lazy";
		var r = stringResource(s, null);
		assertNull(r.getContentType());
	}
}
