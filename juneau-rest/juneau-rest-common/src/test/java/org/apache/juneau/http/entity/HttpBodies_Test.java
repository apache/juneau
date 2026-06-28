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
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests for the {@link org.apache.juneau.http.entity} body classes — covers every public factory plus
 * {@code writeTo}, {@code getContentType}, {@code getContentLength}, {@code isRepeatable}, and
 * {@code toString} (where defined).
 */
class HttpBodies_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// A. StringBody
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_stringBody_defaultContentType() throws Exception {
		var b = StringBody.of("hello");
		assertEquals("text/plain; charset=UTF-8", b.getContentType());
		assertEquals(5, b.getContentLength());
		assertTrue(b.isRepeatable());
		assertEquals("hello", b.toString());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertEquals("hello", out.toString());
	}

	@Test void a02_stringBody_explicitContentType() throws Exception {
		var b = StringBody.of("{}", "application/json");
		assertEquals("application/json", b.getContentType());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertEquals("{}", out.toString());
	}

	@Test void a03_stringBody_writeToTwice_isRepeatable() throws Exception {
		var b = StringBody.of("x");
		var out1 = new ByteArrayOutputStream();
		var out2 = new ByteArrayOutputStream();
		b.writeTo(out1);
		b.writeTo(out2);
		assertEquals(out1.toString(), out2.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// B. ByteArrayBody
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_byteArrayBody_defaultContentType() throws Exception {
		var data = new byte[]{1, 2, 3};
		var b = ByteArrayBody.of(data);
		assertEquals("application/octet-stream", b.getContentType());
		assertEquals(3, b.getContentLength());
		assertTrue(b.isRepeatable());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertArrayEquals(data, out.toByteArray());
	}

	@Test void b02_byteArrayBody_defensiveCopy() throws Exception {
		var data = new byte[]{1, 2, 3};
		var b = ByteArrayBody.of(data);
		data[0] = 99; // mutate after construction
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray(), "ByteArrayBody must defensively copy its input");
	}

	@Test void b03_byteArrayBody_explicitContentType() {
		var b = ByteArrayBody.of(new byte[]{0}, "image/png");
		assertEquals("image/png", b.getContentType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// C. FileBody
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_fileBody_defaultContentType(@TempDir Path dir) throws Exception {
		var f = dir.resolve("a.txt");
		Files.writeString(f, "abc");
		var b = FileBody.of(f.toFile());
		assertEquals("application/octet-stream", b.getContentType());
		assertEquals(3, b.getContentLength());
		assertTrue(b.isRepeatable());
		assertEquals(f.toFile(), b.getFile());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertEquals("abc", out.toString());
	}

	@Test void c02_fileBody_explicitContentType(@TempDir Path dir) throws Exception {
		var f = dir.resolve("a.pdf");
		Files.writeString(f, "x");
		var b = FileBody.of(f.toFile(), "application/pdf");
		assertEquals("application/pdf", b.getContentType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// D. StreamBody
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_streamBody_defaultContentType() throws Exception {
		var b = StreamBody.of(new ByteArrayInputStream("data".getBytes()));
		assertEquals("application/octet-stream", b.getContentType());
		assertFalse(b.isRepeatable());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertEquals("data", out.toString());
	}

	@Test void d02_streamBody_explicitContentType() {
		var b = StreamBody.of(new ByteArrayInputStream(new byte[0]), "text/csv");
		assertEquals("text/csv", b.getContentType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// E. HttpBodyBean
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_httpBodyBean_overridesContentType() throws Exception {
		var inner = StringBody.of("x");
		var b = HttpBodyBean.of(inner, "application/xml");
		assertEquals("application/xml", b.getContentType());
		assertEquals(1, b.getContentLength());
		assertTrue(b.isRepeatable());
		assertEquals(inner.toString(), b.toString());
		var out = new ByteArrayOutputStream();
		b.writeTo(out);
		assertEquals("x", out.toString());
	}

	@Test void e02_httpBodyBean_inheritsContentType() {
		var inner = StringBody.of("x", "application/json");
		var b = HttpBodyBean.of(inner);
		assertEquals("application/json", b.getContentType());
	}

	//------------------------------------------------------------------------------------------------------------------
	// F. MultipartBody
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_multipartBody_buildsAndStreams(@TempDir Path dir) throws Exception {
		var report = dir.resolve("report.pdf");
		Files.writeString(report, "PDF-CONTENT");
		var body = MultipartBody.builder()
			.boundary("boundary123")
			.field("title", "My Report")
			.file("attachment", report.toFile(), "application/pdf")
			.part(MultipartBody.MultipartPart.of("notes", null, "text/plain", StringBody.of("ok")))
			.build();
		assertEquals("multipart/form-data; boundary=boundary123", body.getContentType());
		assertEquals(-1, body.getContentLength());
		assertTrue(body.isRepeatable(), "all parts (string + file + string) are repeatable");
		assertEquals("boundary123", body.getBoundary());
		assertEquals(3, body.getParts().size());

		var out = new ByteArrayOutputStream();
		body.writeTo(out);
		var wire = out.toString();
		assertTrue(wire.contains("--boundary123"));
		assertTrue(wire.contains("Content-Disposition: form-data; name=\"title\""));
		assertTrue(wire.contains("My Report"));
		assertTrue(wire.contains("filename=\"report.pdf\""));
		assertTrue(wire.contains("Content-Type: application/pdf"));
		assertTrue(wire.contains("PDF-CONTENT"));
		assertTrue(wire.endsWith("--boundary123--\r\n"));
	}

	@Test void f02_multipartBody_notRepeatable_whenStreamPart() {
		var body = MultipartBody.builder()
			.part(MultipartBody.MultipartPart.of("upload", "x.bin", "application/octet-stream",
				StreamBody.of(new ByteArrayInputStream(new byte[]{1}))))
			.build();
		assertFalse(body.isRepeatable());
	}

	@Test void f03_multipartBody_partFactories() {
		var p1 = MultipartBody.MultipartPart.field("a", "b");
		assertEquals("a", p1.name());
		assertNull(p1.filename());

		var dir = Path.of(System.getProperty("java.io.tmpdir"));
		var f = dir.resolve("missing.txt").toFile();
		var p2 = MultipartBody.MultipartPart.file("upload", f, "text/plain");
		assertEquals("upload", p2.name());
		assertEquals("missing.txt", p2.filename());
		assertEquals("text/plain", p2.contentType());
	}
}
