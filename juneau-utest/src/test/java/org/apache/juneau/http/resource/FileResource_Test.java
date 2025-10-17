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
package org.apache.juneau.http.resource;

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class FileResource_Test extends TestBase {

	@Test void a01_basic() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();
		Files.write(f.toPath(), "foo".getBytes());

		FileResource x = new FileResource(null, f);
		assertEquals("foo", toUtf8(x.getContent()));
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		FileResource x2 = new FileResource(contentType("text/plain"), f);
		assertEquals("foo", toUtf8(x2.getContent()));
		assertEquals("text/plain", x2.getContentType().getValue());
		assertTrue(x2.isRepeatable());
		assertFalse(x2.isStreaming());

		f.delete();
	}

	@Test void a02_caching() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();
		Files.write(f.toPath(), "foo".getBytes());

		FileResource x = new FileResource(null, f).setCached();
		assertEquals("foo", toUtf8(x.getContent()));
		assertEquals("foo", toUtf8(x.getContent()));
		assertTrue(x.isRepeatable());

		f.delete();
	}

	@Test void a03_headers() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();

		FileResource x = new FileResource(null, f)
			.addHeader("Foo", "bar")
			.addHeader("Foo", "baz")
			.setHeader("Qux", "quux");

		HeaderList headers = x.getHeaders();
		assertEquals("Foo: bar", headers.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", headers.getLast("Foo").get().toString());
		assertEquals("Qux: quux", headers.getFirst("Qux").get().toString());

		f.delete();
	}

	@Test void a04_contentType() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();

		FileResource x = new FileResource(null, f)
			.setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());

		FileResource x2 = new FileResource(null, f)
			.setContentType(contentType("text/html"));
		assertEquals("text/html", x2.getContentType().getValue());

		f.delete();
	}

	@Test void a05_contentEncoding() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();

		FileResource x = new FileResource(null, f)
			.setContentEncoding("identity");
		assertEquals("identity", x.getContentEncoding().getValue());

		FileResource x2 = new FileResource(null, f)
			.setContentEncoding(contentEncoding("gzip"));
		assertEquals("gzip", x2.getContentEncoding().getValue());

		f.delete();
	}

	@Test void a06_chunked() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();

		FileResource x = new FileResource(null, f)
			.setChunked();
		assertTrue(x.isChunked());

		FileResource x2 = new FileResource(null, f)
			.setChunked(true);
		assertTrue(x2.isChunked());

		FileResource x3 = new FileResource(null, f)
			.setChunked(false);
		assertFalse(x3.isChunked());

		f.delete();
	}

	@Test void a07_fluentSetters() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();
		FileResource x = new FileResource(null, f);

		// Test setHeader returns correct type
		assertSame(x, x.setHeader("X-Test", "value"));
		assertEquals("value", x.getHeaders().getFirst("X-Test").get().getValue());

		// Test addHeader returns correct type
		assertSame(x, x.addHeader("X-Test2", "value2"));
		assertEquals("value2", x.getHeaders().getFirst("X-Test2").get().getValue());

		// Test setHeaders returns correct type
		assertSame(x, x.setHeaders(header("X-Test3", "value3")));
		assertEquals("value3", x.getHeaders().getFirst("X-Test3").get().getValue());

		// Test addHeaders returns correct type
		assertSame(x, x.addHeaders(header("X-Test4", "value4")));
		assertEquals("value4", x.getHeaders().getFirst("X-Test4").get().getValue());

		f.delete();
	}

	@Test void a08_copy() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();
		Files.write(f.toPath(), "foo".getBytes());

		FileResource x = new FileResource(contentType("text/plain"), f)
			.setHeader("Foo", "bar");

		FileResource x2 = x.copy();
		assertNotSame(x, x2);
		assertEquals("foo", toUtf8(x2.getContent()));
		assertEquals("text/plain", x2.getContentType().getValue());
		assertEquals("bar", x2.getHeaders().getFirst("Foo").get().getValue());

		f.delete();
	}

	@Test void a09_contentLength() throws Exception {
		File f = Files.createTempFile("test", "txt").toFile();
		Files.write(f.toPath(), "foo".getBytes());

		FileResource x = new FileResource(null, f);
		assertEquals(3L, x.getContentLength());

		// Content length is derived from actual file length for FileResource
		FileResource x2 = new FileResource(null, f).setContentLength(10);
		assertEquals(3L, x2.getContentLength());

		f.delete();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private static Header header(String name, Object val) {
		return BasicHeader.of(name, val);
	}
}