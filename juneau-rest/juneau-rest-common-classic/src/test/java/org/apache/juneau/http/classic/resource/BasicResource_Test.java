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
package org.apache.juneau.http.classic.resource;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.http.classic.HttpHeaders.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.http.classic.header.*;
import org.junit.jupiter.api.*;

// Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
@SuppressWarnings({
	"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class BasicResource_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Constructors
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_constructor_entity() throws Exception {
		var x = new StringResource(contentType("text/plain"), "foo");
		assertEquals("foo", toUtf8(x.getContent()));
		assertEquals("text/plain", x.getContentType().getValue());
		assertNotNull(x.getEntity());
		assertSame(x.getEntity(), x.getEntity());
	}

	@Test void a02_constructor_copy() throws Exception {
		var src = new StringResource(contentType("text/plain"), "foo")
			.setHeader("Foo", "bar");
		var x = new StringResource(src);
		assertNotSame(src, x);
		assertEquals("foo", toUtf8(x.getContent()));
		assertEquals("text/plain", x.getContentType().getValue());
		assertEquals("bar", x.getHeaders().getFirst("Foo").get().getValue());
	}

	@Test void a03_constructor_httpResponse() throws Exception {
		var resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 200, "OK");
		resp.setEntity(new org.apache.http.entity.StringEntity("hello"));
		resp.addHeader("X-Foo", "bar");
		resp.addHeader("Content-Type", "text/plain");
		resp.addHeader("Content-Encoding", "identity");
		resp.addHeader("Content-Length", "5");
		var x = new StreamResource(resp);
		assertEquals("hello", toUtf8(x.getContent()));
		assertEquals("bar", x.getHeaders().getFirst("X-Foo").get().getValue());
		assertEquals("text/plain", x.getContentType().getValue());
		assertEquals("identity", x.getContentEncoding().getValue());
		assertEquals(5L, x.getContentLength());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Headers
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_addHeader_basic() {
		var x = new StringResource(null, "foo")
			.addHeader("Foo", "bar")
			.addHeader("Foo", "baz")
			.setHeader("Qux", "quux");
		var headers = x.getHeaders();
		assertEquals("Foo: bar", headers.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", headers.getLast("Foo").get().toString());
		assertEquals("Qux: quux", headers.getFirst("Qux").get().toString());
	}

	@Test void b02_addHeader_nullSafe() {
		var x = new StringResource(null, "foo")
			.addHeader(null, "bar")
			.addHeader("Foo", null)
			.setHeader(null, "x")
			.setHeader("X", null);
		assertEquals(0, x.getHeaders().size());
	}

	@Test void b03_addHeaders_specialHeaders() {
		var x = new StringResource(null, "foo")
			.addHeaders(
				header("Content-Type", "text/plain"),
				header("Content-Encoding", "identity"),
				header("Content-Length", "3"),
				header("X-Foo", "bar"),
				null,                          // null header skipped
				new org.apache.http.message.BasicHeader("", "ignored") // empty-name header skipped
			);
		assertEquals("text/plain", x.getContentType().getValue());
		assertEquals("identity", x.getContentEncoding().getValue());
		assertEquals(3L, x.getContentLength());
		assertEquals("bar", x.getHeaders().getFirst("X-Foo").get().getValue());
	}

	@Test void b04_setHeaders_specialHeaders() {
		var x = new StringResource(null, "foo")
			.setHeaders(
				header("Content-Type", "text/plain"),
				header("Content-Encoding", "identity"),
				header("Content-Length", "3"),
				header("X-Foo", "bar"),
				null,                          // null header skipped
				new org.apache.http.message.BasicHeader("", "ignored") // empty-name header skipped
			);
		assertEquals("text/plain", x.getContentType().getValue());
		assertEquals("identity", x.getContentEncoding().getValue());
		assertEquals(3L, x.getContentLength());
		assertEquals("bar", x.getHeaders().getFirst("X-Foo").get().getValue());
	}

	@Test void b05_setHeaders_headerList() {
		var hl = HeaderList.create().append("Foo", "bar");
		var x = new StringResource(null, "foo")
			.setHeaders(hl);
		assertEquals("bar", x.getHeaders().getFirst("Foo").get().getValue());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Content/encoding/length/type setters
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_setContent_value() throws Exception {
		var x = new StringResource(null, "old")
			.setContent("new");
		assertEquals("new", toUtf8(x.getContent()));
	}

	@Test void c02_setContent_supplier() throws Exception {
		var x = new StringResource(null, null)
			.setContent(() -> "newish");
		assertEquals("newish", toUtf8(x.getContent()));
	}

	@Test void c03_setContentType_string_andContentType() {
		var x = new StringResource(null, "foo")
			.setContentType("text/plain");
		assertEquals("text/plain", x.getContentType().getValue());

		var x2 = new StringResource(null, "foo")
			.setContentType(contentType("text/html"));
		assertEquals("text/html", x2.getContentType().getValue());
	}

	@Test void c04_setContentEncoding_string_andContentEncoding() {
		var x = new StringResource(null, "foo")
			.setContentEncoding("identity");
		assertEquals("identity", x.getContentEncoding().getValue());

		var x2 = new StringResource(null, "foo")
			.setContentEncoding(contentEncoding("gzip"));
		assertEquals("gzip", x2.getContentEncoding().getValue());
	}

	@Test void c05_setContentLength() {
		var x = new StringResource(null, "foo")
			.setContentLength(10L);
		assertEquals(10L, x.getContentLength());
	}

	@Test void c06_chunked() {
		var x = new StringResource(null, "foo").setChunked();
		assertTrue(x.isChunked());
		var x2 = new StringResource(null, "foo").setChunked(false);
		assertFalse(x2.isChunked());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Caching, asBytes/asString, assertions, copy, writeTo, consumeContent
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_setCached_andRepeatable() throws Exception {
		var x = new StringResource(null, "foo").setCached();
		assertEquals("foo", toUtf8(x.getContent()));
		assertEquals("foo", toUtf8(x.getContent()));
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());
	}

	@Test void d02_asBytes_asString() throws Exception {
		var x = new StringResource(null, "hello");
		assertEquals("hello", new String(x.asBytes()));
		assertEquals("hello", x.asString());
	}

	@Test void d03_assertBytes_assertString() throws Exception {
		var x = new StringResource(null, "abc");
		assertNotNull(x.assertBytes());
		assertNotNull(x.assertString());
	}

	@Test void d04_copy_method() throws Exception {
		var x = new StringResource(contentType("text/plain"), "foo")
			.setHeader("Foo", "bar");
		var x2 = x.copy();
		assertNotSame(x, x2);
		assertEquals("foo", toUtf8(x2.getContent()));
		assertEquals("text/plain", x2.getContentType().getValue());
		assertEquals("bar", x2.getHeaders().getFirst("Foo").get().getValue());
	}

	@Test void d05_writeTo() throws Exception {
		var x = new StringResource(null, "writeMe");
		var out = new ByteArrayOutputStream();
		x.writeTo(out);
		assertEquals("writeMe", out.toString("UTF-8"));
	}

	@Test void d06_consumeContent() {
		var x = new StringResource(null, "foo");
		// consumeContent() is a no-op that must complete without throwing.
		assertDoesNotThrow(x::consumeContent);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Unmodifiable
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_isUnmodifiable_default() {
		var x = new StringResource(null, "foo");
		assertFalse(x.isUnmodifiable());
	}

	@Test void e02_unmodifiable_returnsFrozenSnapshot() {
		var x = new StringResource(null, "foo");
		var u = x.unmodifiable();
		assertFalse(x.isUnmodifiable());
		assertTrue(u.isUnmodifiable());
		// The snapshot's direct-field mutator (setHeaders(HeaderList)) throws through the modify() funnel...
		var newHeaders = HeaderList.create().append("X", "y");
		assertThrows(UnsupportedOperationException.class, () -> u.setHeaders(newHeaders));
		// ...and the sub-bean-delegating mutators throw through the frozen entity/headers.
		assertThrows(UnsupportedOperationException.class, () -> u.setContent("other"));
		assertThrows(UnsupportedOperationException.class, () -> u.addHeader("X", "y"));
		assertThrows(UnsupportedOperationException.class, () -> u.setHeader("X", "y"));
		assertThrows(UnsupportedOperationException.class, () -> u.setChunked(true));
		var frozenHeaders = u.getHeaders();
		assertThrows(UnsupportedOperationException.class, () -> frozenHeaders.append("X", "y"));
	}

	@Test void e03_unmodifiable_idempotent() {
		var u = new StringResource(null, "foo").unmodifiable();
		// D1 idempotency: unmodifiable() on an already-unmodifiable snapshot returns the same instance.
		assertSame(u, u.unmodifiable());
	}

	@Test void e04_unmodifiable_snapshotIndependence() {
		var x = new StringResource(null, "foo").addHeader("X-A", "a");
		var u = x.unmodifiable();
		var before = u.getHeaders().size();
		x.addHeader("X-B", "b"); // Mutate original after snapshotting.
		assertEquals(before, u.getHeaders().size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setter return-type checks
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_fluentReturns() {
		var x = new StringResource(null, "foo");
		assertSame(x, x.addHeader("X-A", "a"));
		assertSame(x, x.setHeader("X-B", "b"));
		assertSame(x, x.addHeaders(header("X-C", "c")));
		assertSame(x, x.setHeaders(header("X-D", "d")));
		assertSame(x, x.setHeaders(HeaderList.create().append("X-E", "e")));
		assertSame(x, x.setContent("new"));
		assertSame(x, x.setContent(() -> "supplied"));
		assertSame(x, x.setContentType("text/plain"));
		assertSame(x, x.setContentType(contentType("text/html")));
		assertSame(x, x.setContentEncoding("identity"));
		assertSame(x, x.setContentEncoding(contentEncoding("gzip")));
		assertSame(x, x.setContentLength(1L));
		assertSame(x, x.setChunked());
		assertSame(x, x.setChunked(true));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods/types
	//------------------------------------------------------------------------------------------------------------------

	private static Header header(String name, Object val) {
		return org.apache.juneau.http.classic.header.BasicHeader.of(name, val);
	}
}
