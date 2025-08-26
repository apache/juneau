// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.http;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class BasicHttpResource_Test extends SimpleTestBase {

	@Test void a01_basic() throws Exception {
		var f = Files.createTempFile("test","txt").toFile();

		var x = stringResource((String)null);
		assertNull(x.getContentType());
		assertEquals("", toUtf8(x.getContent()));
		assertNull(x.getContentEncoding());
		assertEquals(0, x.getHeaders().size());

		var x2 = stringResource("foo");
		assertEquals("foo", toUtf8(x2.getContent()));
		assertTrue(x2.isRepeatable());
		assertFalse(x2.isStreaming());

		var x3 = readerResource(reader("foo"));
		assertEquals("foo", toUtf8(x3.getContent()));
		assertFalse(x3.isRepeatable());
		assertTrue(x3.isStreaming());

		var x4 = byteArrayResource("foo".getBytes());
		assertEquals("foo", toUtf8(x4.getContent()));
		assertTrue(x4.isRepeatable());
		assertFalse(x4.isStreaming());

		var x5 = streamResource(inputStream("foo"));
		assertEquals("foo", toUtf8(x5.getContent()));
		assertFalse(x5.isRepeatable());
		assertTrue(x5.isStreaming());

		var x6 = fileResource(f);
		assertEquals("", toUtf8(x6.getContent()));
		assertTrue(x6.isRepeatable());
		assertFalse(x6.isStreaming());

		var x7 = stringResource("foo").setCached();
		assertEquals("foo", toUtf8(x7.getContent()));
		assertEquals("foo", toUtf8(x7.getContent()));
		assertTrue(x7.isRepeatable());

		var x8 = readerResource(reader("foo")).setCached();
		assertEquals("foo", toUtf8(x8.getContent()));
		assertEquals("foo", toUtf8(x8.getContent()));
		assertTrue(x8.isRepeatable());

		var x9 = byteArrayResource("foo".getBytes()).setCached();
		assertEquals("foo", toUtf8(x9.getContent()));
		assertEquals("foo", toUtf8(x9.getContent()));
		assertTrue(x9.isRepeatable());

		var x10 = streamResource(inputStream("foo")).setCached();
		assertEquals("foo", toUtf8(x10.getContent()));
		assertEquals("foo", toUtf8(x10.getContent()));
		assertTrue(x10.isRepeatable());

		var x11 = stringResource((String)null).setCached();
		assertEquals("", toUtf8(x11.getContent()));
		assertTrue(x11.isRepeatable());
		x11.writeTo(new ByteArrayOutputStream());

		var x12 = fileResource(f).setCached();
		assertEquals("", toUtf8(x12.getContent()));
		assertTrue(x12.isRepeatable());
		x12.writeTo(new ByteArrayOutputStream());

		assertEquals(3L, stringResource("foo").getContentLength());
		assertEquals(3L, byteArrayResource("foo".getBytes()).getContentLength());
		assertEquals(0L, fileResource(f).getContentLength());

		assertEquals(-1L, readerResource(reader("foo")).getContentLength());
		assertEquals(3L, readerResource(reader("foo")).setContentLength(3).getContentLength());

		var x13 = stringResource("foo", contentType("text/plain")).setContentEncoding("identity");
		assertEquals("text/plain", x13.getContentType().getValue());
		assertEquals("identity", x13.getContentEncoding().getValue());

		var x14 = stringResource("foo", null).setContentEncoding((String)null);
		assertNull(x14.getContentType());
		assertNull(x14.getContentEncoding());
	}

	@Test void a02_header_String_Object() {
		var x = stringResource("foo").addHeader("Foo","bar").addHeader("Foo","baz").addHeader(null,"bar").addHeader("foo",null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertEmpty(x.getFirst("Bar"));
		assertEmpty(x.getLast("Bar"));
		assertArray(x.getAll(), "Foo: bar,Foo: baz");
	}

	@Test void a03_header_Header() {
		var x = stringResource("foo").addHeaders(header("Foo","bar")).addHeaders(header("Foo","baz")).addHeaders(header("Bar",null)).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}

	@Test void a04_headers_List() {
		var x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}

	@Test void a05_headers_array() {
		var x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}

	@Test void a06_chunked() {
		var x1 = stringResource("foo").setChunked();
		assertTrue(x1.isChunked());
		var x2 = stringResource("foo");
		assertFalse(x2.isChunked());
	}

	@Test void a07_chunked_boolean() {
		var x1 = stringResource("foo").setChunked(true);
		assertTrue(x1.isChunked());
		var x2 = stringResource("foo").setChunked(false);
		assertFalse(x2.isChunked());
	}

	@Test void a08_contentType_String() {
		var x1 = stringResource("foo").setContentType("text/plain");
		assertEquals("text/plain", x1.getContentType().getValue());
		var x2 = stringResource("foo").setContentType((String)null);
		assertNull(x2.getContentType());
	}

	@Test void a09_contentEncoding_String() {
		var x1 = stringResource("foo").setContentEncoding("identity");
		assertEquals("identity", x1.getContentEncoding().getValue());
		var x2 = stringResource("foo").setContentEncoding((String)null);
		assertNull(x2.getContentEncoding());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return new BasicHeader(name, val);
	}
}