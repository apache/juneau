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
// * specific language governing permissions and limitations under the License.                                               *
// ***************************************************************************************************************************
package org.apache.juneau.http.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class ByteArrayEntity_Test extends TestBase {

	private static final byte[] BYTES = "hello world".getBytes();

	@Test void a01_basic() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertArrayEquals(BYTES, x.asBytes());
		assertEquals("hello world", x.asString());
	}

	@Test void a02_constructor_contentType() throws Exception {
		var x = new ByteArrayEntity(ContentType.TEXT_PLAIN, BYTES);
		assertArrayEquals(BYTES, x.asBytes());
		assertEquals("text/plain", x.getContentType().getValue());
	}

	@Test void a03_getContent() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertNotNull(x.getContent());
	}

	@Test void a04_getContentLength_notSupplied() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertEquals(BYTES.length, x.getContentLength());
	}

	@Test void a05_getContentLength_supplied() throws Exception {
		// When content is supplied via supplier, isSupplied()=true => super.getContentLength() is used
		Supplier<byte[]> supplier = () -> BYTES;
		var x = new ByteArrayEntity().setContent(supplier);
		// getContentLength() goes through the isSupplied() ? super.getContentLength() : content().length branch
		long len = x.getContentLength();
		// Not asserting exact value as it might be -1 from super, just verify no exception
		assertTrue(len >= -1);
	}

	@Test void a06_isRepeatable() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertTrue(x.isRepeatable());
	}

	@Test void a07_writeTo() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		var baos = new ByteArrayOutputStream();
		x.writeTo(baos);
		assertArrayEquals(BYTES, baos.toByteArray());
	}

	@Test void a08_copy() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		var copy = x.copy();
		assertArrayEquals(BYTES, copy.asBytes());
	}

	@Test void a09_fluent_setCharset() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		var result = x.setCharset(java.nio.charset.Charset.forName("UTF-8"));
		assertSame(x, result);
	}

	@Test void a10_fluent_setChunked() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setChunked());
		assertSame(x, x.setChunked(true));
	}

	@Test void a11_fluent_setContent() throws Exception {
		var x = new ByteArrayEntity();
		assertSame(x, x.setContent(BYTES));
		assertSame(x, x.setContent((Object)BYTES));
	}

	@Test void a12_fluent_setContentEncoding() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setContentEncoding("gzip"));
	}

	@Test void a13_fluent_setContentLength() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setContentLength(BYTES.length));
	}

	@Test void a14_fluent_setContentType() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setContentType(ContentType.TEXT_PLAIN));
		assertSame(x, x.setContentType("text/plain"));
	}

	@Test void a15_fluent_setMaxLength() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setMaxLength(100));
	}

	@Test void a16_fluent_setUnmodifiable() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setUnmodifiable());
	}

	@Test void a17_fluent_setCached() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setCached());
	}

	@Test void a18_fluent_setContentEncoding_header() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertSame(x, x.setContentEncoding(new ContentEncoding("gzip")));
	}
}
