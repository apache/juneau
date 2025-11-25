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
package org.apache.juneau.common.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ReaderInputStream_Test extends TestBase {

	//====================================================================================================
	// Constructor tests
	//====================================================================================================
	@Test void a01_constructorWithReaderAndCharset() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a02_constructorWithReaderAndCharsetName() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, "UTF-8");
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a03_constructorWithReaderAndCharsetAndBufferSize() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8, 512);
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a04_constructorWithReaderAndCharsetNameAndBufferSize() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, "UTF-8", 512);
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a05_constructorWithReaderAndEncoder() throws IOException {
		var reader = new StringReader("test");
		var encoder = StandardCharsets.UTF_8.newEncoder();
		var is = new ReaderInputStream(reader, encoder);
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a06_constructorWithReaderAndEncoderAndBufferSize() throws IOException {
		var reader = new StringReader("test");
		var encoder = StandardCharsets.UTF_8.newEncoder();
		var is = new ReaderInputStream(reader, encoder, 512);
		var bytes = is.readAllBytes();
		assertEquals("test", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void a07_constructorWithReaderAndEncoderAndBufferSize_zero() {
		var reader = new StringReader("test");
		var encoder = StandardCharsets.UTF_8.newEncoder();
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, encoder, 0);
		});
	}

	@Test void a08_constructorWithReaderAndEncoderAndBufferSize_negative() {
		var reader = new StringReader("test");
		var encoder = StandardCharsets.UTF_8.newEncoder();
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, encoder, -1);
		});
	}

	@Test void a09_constructorWithReaderAndCharsetAndBufferSize_zero() {
		var reader = new StringReader("test");
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, StandardCharsets.UTF_8, 0);
		});
	}

	@Test void a10_constructorWithReaderAndCharsetAndBufferSize_negative() {
		var reader = new StringReader("test");
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, StandardCharsets.UTF_8, -1);
		});
	}

	@Test void a11_constructorWithReaderAndCharsetNameAndBufferSize_zero() {
		var reader = new StringReader("test");
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, "UTF-8", 0);
		});
	}

	@Test void a12_constructorWithReaderAndCharsetNameAndBufferSize_negative() {
		var reader = new StringReader("test");
		assertThrows(IllegalArgumentException.class, () -> {
			new ReaderInputStream(reader, "UTF-8", -1);
		});
	}

	//====================================================================================================
	// read() tests
	//====================================================================================================
	@Test void b01_readSingleByte() throws IOException {
		var reader = new StringReader("ABC");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		assertEquals('A', is.read());
		assertEquals('B', is.read());
		assertEquals('C', is.read());
		assertEquals(-1, is.read());
		is.close();
	}

	@Test void b02_readEndOfStream() throws IOException {
		var reader = new StringReader("");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		assertEquals(-1, is.read());
		assertEquals(-1, is.read()); // Should continue returning -1
		is.close();
	}

	//====================================================================================================
	// read(byte[]) tests
	//====================================================================================================
	@Test void c01_readByteArray() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[100];
		var count = is.read(buf);
		assertTrue(count > 0);
		assertEquals("test", new String(buf, 0, count, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void c02_readByteArray_empty() throws IOException {
		var reader = new StringReader("");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[100];
		var count = is.read(buf);
		assertEquals(-1, count);
		is.close();
	}

	//====================================================================================================
	// read(byte[], int, int) tests
	//====================================================================================================
	@Test void d01_readByteArrayWithOffset() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[100];
		var count = is.read(buf, 10, 50);
		assertTrue(count > 0);
		assertEquals("test", new String(buf, 10, count, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void d02_readByteArrayWithOffset_zeroLength() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[100];
		var count = is.read(buf, 0, 0);
		assertEquals(0, count);
		is.close();
	}

	@Test void d03_readByteArrayWithOffset_nullArray() {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		assertThrows(IllegalArgumentException.class, () -> {
			is.read(null, 0, 10);
		});
	}

	@Test void d04_readByteArrayWithOffset_invalidOffset() {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[10];
		assertThrows(IndexOutOfBoundsException.class, () -> {
			is.read(buf, -1, 5);
		});
	}

	@Test void d05_readByteArrayWithOffset_invalidLength() {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[10];
		assertThrows(IndexOutOfBoundsException.class, () -> {
			is.read(buf, 0, -1);
		});
	}

	@Test void d06_readByteArrayWithOffset_offsetPlusLengthTooLarge() {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf = new byte[10];
		assertThrows(IndexOutOfBoundsException.class, () -> {
			is.read(buf, 5, 10);
		});
	}

	//====================================================================================================
	// close() tests
	//====================================================================================================
	@Test void e01_close() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		is.close(); // Should close the underlying reader
		// Reader should be closed
		assertThrows(IOException.class, () -> {
			reader.read();
		});
	}

	//====================================================================================================
	// Charset encoding tests
	//====================================================================================================
	@Test void f01_utf8() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var bytes = is.readAllBytes();
		assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), bytes);
		is.close();
	}

	@Test void f02_iso8859_1() throws IOException {
		var reader = new StringReader("test");
		var is = new ReaderInputStream(reader, StandardCharsets.ISO_8859_1);
		var bytes = is.readAllBytes();
		assertArrayEquals("test".getBytes(StandardCharsets.ISO_8859_1), bytes);
		is.close();
	}

	@Test void f03_unicode() throws IOException {
		var reader = new StringReader("héllo");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var bytes = is.readAllBytes();
		assertEquals("héllo", new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	//====================================================================================================
	// Large content tests
	//====================================================================================================
	@Test void g01_largeContent() throws IOException {
		var sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			sb.append("test");
		}
		var reader = new StringReader(sb.toString());
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var bytes = is.readAllBytes();
		assertEquals(sb.toString(), new String(bytes, StandardCharsets.UTF_8));
		is.close();
	}

	@Test void g02_multipleReads() throws IOException {
		var reader = new StringReader("hello world");
		var is = new ReaderInputStream(reader, StandardCharsets.UTF_8);
		var buf1 = new byte[5];
		var count1 = is.read(buf1);
		var buf2 = new byte[100];
		var count2 = is.read(buf2);
		var result = new String(buf1, 0, count1, StandardCharsets.UTF_8) +
			new String(buf2, 0, count2, StandardCharsets.UTF_8);
		assertEquals("hello world", result);
		is.close();
	}
}

