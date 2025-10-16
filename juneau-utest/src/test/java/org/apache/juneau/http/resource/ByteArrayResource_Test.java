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

import org.apache.http.*;
import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class ByteArrayResource_Test extends TestBase {

	@Test void a01_basic() throws Exception {
		ByteArrayResource x = new ByteArrayResource();
		assertEquals("", toUtf8(x.getContent()));
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		ByteArrayResource x2 = new ByteArrayResource(contentType("text/plain"), "foo".getBytes());
		assertEquals("foo", toUtf8(x2.getContent()));
		assertEquals("text/plain", x2.getContentType().getValue());
		assertTrue(x2.isRepeatable());
		assertFalse(x2.isStreaming());
	}

	@Test void a02_caching() throws Exception {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes()).setCached();
		assertEquals("foo", toUtf8(x.getContent()));
		assertEquals("foo", toUtf8(x.getContent()));
		assertTrue(x.isRepeatable());
	}

	@Test void a03_headers() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes())
			.addHeader("Foo", "bar")
			.addHeader("Foo", "baz")
			.setHeader("Qux", "quux");

		HeaderList headers = x.getHeaders();
		assertEquals("Foo: bar", headers.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", headers.getLast("Foo").get().toString());
		assertEquals("Qux: quux", headers.getFirst("Qux").get().toString());
	}

	@Test void a04_contentType() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes())
			.setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());

		ByteArrayResource x2 = new ByteArrayResource(null, "foo".getBytes())
			.setContentType(contentType("text/html"));
		assertEquals("text/html", x2.getContentType().getValue());
	}

	@Test void a05_contentEncoding() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes())
			.setContentEncoding("identity");
		assertEquals("identity", x.getContentEncoding().getValue());

		ByteArrayResource x2 = new ByteArrayResource(null, "foo".getBytes())
			.setContentEncoding(contentEncoding("gzip"));
		assertEquals("gzip", x2.getContentEncoding().getValue());
	}

	@Test void a06_chunked() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes())
			.setChunked();
		assertTrue(x.isChunked());

		ByteArrayResource x2 = new ByteArrayResource(null, "foo".getBytes())
			.setChunked(true);
		assertTrue(x2.isChunked());

		ByteArrayResource x3 = new ByteArrayResource(null, "foo".getBytes())
			.setChunked(false);
		assertFalse(x3.isChunked());
	}

	@Test void a07_fluentSetters() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes());

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
	}

	@Test void a08_copy() throws Exception {
		ByteArrayResource x = new ByteArrayResource(contentType("text/plain"), "foo".getBytes())
			.setHeader("Foo", "bar");

		ByteArrayResource x2 = x.copy();
		assertNotSame(x, x2);
		assertEquals("foo", toUtf8(x2.getContent()));
		assertEquals("text/plain", x2.getContentType().getValue());
		assertEquals("bar", x2.getHeaders().getFirst("Foo").get().getValue());
	}

	@Test void a09_contentLength() {
		ByteArrayResource x = new ByteArrayResource(null, "foo".getBytes());
		assertEquals(3L, x.getContentLength());

		// Content length is derived from actual content for ByteArrayResource
		ByteArrayResource x2 = new ByteArrayResource(null, "foo".getBytes()).setContentLength(10);
		assertEquals(3L, x2.getContentLength());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private Header header(String name, Object val) {
		return BasicHeader.of(name, val);
	}
}