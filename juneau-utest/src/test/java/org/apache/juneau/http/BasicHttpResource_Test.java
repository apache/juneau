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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpHeaders.*;
import static org.apache.juneau.http.HttpResources.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.junit.jupiter.api.*;

class BasicHttpResource_Test extends SimpleTestBase {

	@Test void a01_basic() throws Exception {
		File f = Files.createTempFile("test","txt").toFile();

		HttpResource x = stringResource((String)null);

		assertNull(x.getContentType());
		assertBytes(x.getContent()).isNotNull().asString().isEmpty();
		assertNull(x.getContentEncoding());
		assertEquals(0, x.getHeaders().size());

		x = stringResource("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = readerResource(reader("foo"));
		assertBytes(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = byteArrayResource("foo".getBytes());
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = streamResource(inputStream("foo"));
		assertBytes(x.getContent()).asString().is("foo");
		assertFalse(x.isRepeatable());
		assertTrue(x.isStreaming());

		x = fileResource(f);
		assertBytes(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		assertFalse(x.isStreaming());

		x = stringResource("foo").setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = readerResource(reader("foo")).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = byteArrayResource("foo".getBytes()).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = streamResource(inputStream("foo")).setCached();
		assertBytes(x.getContent()).asString().is("foo");
		assertBytes(x.getContent()).asString().is("foo");
		assertTrue(x.isRepeatable());

		x = stringResource((String)null).setCached();
		assertBytes(x.getContent()).isExists().asString().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		x = fileResource(f).setCached();
		assertBytes(x.getContent()).asString().isEmpty();
		assertTrue(x.isRepeatable());
		x.writeTo(new ByteArrayOutputStream());

		assertLong(stringResource("foo").getContentLength()).is(3L);
		assertLong(byteArrayResource("foo".getBytes()).getContentLength()).is(3L);
		assertLong(fileResource(f).getContentLength()).is(0L);

		assertLong(readerResource(reader("foo")).getContentLength()).is(-1L);
		assertLong(readerResource(reader("foo")).setContentLength(3).getContentLength()).is(3L);

		x = stringResource("foo", contentType("text/plain")).setContentEncoding("identity");
		assertEquals("text/plain", x.getContentType().getValue());
		assertEquals("identity", x.getContentEncoding().getValue());

		x = stringResource("foo", null).setContentEncoding((String)null);
		assertNull(x.getContentType());
		assertNull(x.getContentEncoding());
	}

	@Test void a02_header_String_Object() {
		HeaderList x = stringResource("foo").addHeader("Foo","bar").addHeader("Foo","baz").addHeader(null,"bar").addHeader("foo",null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertEmpty(x.getFirst("Bar"));
		assertEmpty(x.getLast("Bar"));
		assertArray(x.getAll(), "Foo: bar,Foo: baz");
	}

	@Test void a03_header_Header() {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar")).addHeaders(header("Foo","baz")).addHeaders(header("Bar",null)).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}

	@Test void a04_headers_List() {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}

	@Test void a05_headers_array() {
		HeaderList x = stringResource("foo").addHeaders(header("Foo","bar"),header("Foo","baz"),header("Bar",null),null).getHeaders();
		assertEquals("Foo: bar", x.getFirst("Foo").get().toString());
		assertEquals("Foo: baz", x.getLast("Foo").get().toString());
		assertNull(x.getFirst("Bar").get().getValue());
		assertNull(x.getLast("Bar").get().getValue());
		assertArray(x.getAll(), "Foo: bar,Foo: baz,Bar: null");
	}


	@Test void a06_chunked() {
		StringResource x1 = stringResource("foo").setChunked();
		assertTrue(x1.isChunked());
		StringResource x2 = stringResource("foo");
		assertFalse(x2.isChunked());
	}

	@Test void a07_chunked_boolean() {
		StringResource x1 = stringResource("foo").setChunked(true);
		assertTrue(x1.isChunked());
		StringResource x2 = stringResource("foo").setChunked(false);
		assertFalse(x2.isChunked());
	}

	@Test void a08_contentType_String() {
		StringResource x1 = stringResource("foo").setContentType("text/plain");
		assertEquals("text/plain", x1.getContentType().getValue());
		StringResource x2 = stringResource("foo").setContentType((String)null);
		assertNull(x2.getContentType());
	}

	@Test void a09_contentEncoding_String() {
		StringResource x1 = stringResource("foo").setContentEncoding("identity");
		assertEquals("identity", x1.getContentEncoding().getValue());
		StringResource x2 = stringResource("foo").setContentEncoding((String)null);
		assertNull(x2.getContentEncoding());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//------------------------------------------------------------------------------------------------------------------

	private BasicHeader header(String name, Object val) {
		return new BasicHeader(name, val);
	}
}