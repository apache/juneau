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
package org.apache.juneau.commons.io;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CharSequenceReader_Test extends TestBase {

	//====================================================================================================
	// Constructor tests
	//====================================================================================================
	@Test void a01_constructorWithString() {
		var reader = new CharSequenceReader("test");
		var result = new StringBuilder();
		int c;
		while ((c = reader.read()) != -1) {
			result.append((char)c);
		}
		assertEquals("test", result.toString());
	}

	@Test void a02_constructorWithStringBuilder() {
		var cs = new StringBuilder("test");
		var reader = new CharSequenceReader(cs);
		var result = new StringBuilder();
		int c;
		while ((c = reader.read()) != -1) {
			result.append((char)c);
		}
		assertEquals("test", result.toString());
	}

	@Test void a03_constructorWithStringBuffer() {
		var cs = new StringBuffer("test");
		var reader = new CharSequenceReader(cs);
		var result = new StringBuilder();
		int c;
		while ((c = reader.read()) != -1) {
			result.append((char)c);
		}
		assertEquals("test", result.toString());
	}

	@Test void a04_constructorWithNull() {
		var reader = new CharSequenceReader(null);
		assertEquals(-1, reader.read());
		assertEquals("", reader.toString());
	}

	@Test void a05_constructorWithEmptyString() {
		var reader = new CharSequenceReader("");
		assertEquals(-1, reader.read());
		assertEquals("", reader.toString());
	}

	//====================================================================================================
	// read() tests
	//====================================================================================================
	@Test void b01_readSingleChar() {
		var reader = new CharSequenceReader("abc");
		assertEquals('a', reader.read());
		assertEquals('b', reader.read());
		assertEquals('c', reader.read());
		assertEquals(-1, reader.read());
	}

	@Test void b02_readEndOfStream() {
		var reader = new CharSequenceReader("a");
		assertEquals('a', reader.read());
		assertEquals(-1, reader.read());
		assertEquals(-1, reader.read()); // Should continue returning -1
	}

	//====================================================================================================
	// read(char[], int, int) tests
	//====================================================================================================
	@Test void c01_readCharArray() {
		var reader = new CharSequenceReader("hello");
		var buf = new char[5];
		var count = reader.read(buf, 0, 5);
		assertEquals(5, count);
		assertEquals("hello", new String(buf));
	}

	@Test void c02_readCharArray_partial() {
		var reader = new CharSequenceReader("hello");
		var buf = new char[10];
		var count = reader.read(buf, 0, 3);
		assertEquals(3, count);
		assertEquals("hel", new String(buf, 0, 3));
	}

	@Test void c03_readCharArray_multipleReads() {
		var reader = new CharSequenceReader("hello");
		var buf = new char[3];
		var count1 = reader.read(buf, 0, 3);
		assertEquals(3, count1);
		assertEquals("hel", new String(buf));
		var count2 = reader.read(buf, 0, 3);
		assertEquals(2, count2);
		assertEquals("lo", new String(buf, 0, 2));
	}

	@Test void c04_readCharArray_endOfStream() {
		var reader = new CharSequenceReader("a");
		var buf = new char[10];
		var count = reader.read(buf, 0, 10);
		assertEquals(1, count);
		assertEquals('a', buf[0]);
		var count2 = reader.read(buf, 0, 10);
		assertEquals(-1, count2);
	}

	@Test void c05_readCharArray_empty() {
		var reader = new CharSequenceReader("");
		var buf = new char[10];
		var count = reader.read(buf, 0, 10);
		assertEquals(-1, count);
	}

	@Test void c06_readCharArray_withOffset() {
		var reader = new CharSequenceReader("hello");
		var buf = new char[10];
		var count = reader.read(buf, 2, 3);
		assertEquals(3, count);
		assertEquals("hel", new String(buf, 2, 3));
	}

	//====================================================================================================
	// skip() tests
	//====================================================================================================
	@Test void d01_skip() {
		var reader = new CharSequenceReader("hello");
		var skipped = reader.skip(2);
		assertEquals(2, skipped);
		assertEquals('l', reader.read());
	}

	@Test void d02_skip_all() {
		var reader = new CharSequenceReader("hello");
		var skipped = reader.skip(10);
		assertEquals(5, skipped);
		assertEquals(-1, reader.read());
	}

	@Test void d03_skip_zero() {
		var reader = new CharSequenceReader("hello");
		var skipped = reader.skip(0);
		assertEquals(0, skipped);
		assertEquals('h', reader.read());
	}

	@Test void d04_skip_negative() {
		var reader = new CharSequenceReader("hello");
		var skipped = reader.skip(-5);
		assertEquals(0, skipped); // Should clamp to 0
		assertEquals('h', reader.read());
	}

	@Test void d05_skip_afterEnd() {
		var reader = new CharSequenceReader("a");
		reader.read(); // Read the 'a'
		var skipped = reader.skip(10);
		assertEquals(0, skipped);
	}

	//====================================================================================================
	// markSupported() tests
	//====================================================================================================
	@Test void e01_markSupported() {
		var reader = new CharSequenceReader("test");
		assertFalse(reader.markSupported());
	}

	//====================================================================================================
	// close() tests
	//====================================================================================================
	@Test void f01_close() {
		var reader = new CharSequenceReader("test");
		reader.close(); // Should not throw
		// Should still be able to read after close
		assertEquals('t', reader.read());
	}

	//====================================================================================================
	// toString() tests
	//====================================================================================================
	@Test void g01_toString() {
		var reader = new CharSequenceReader("test");
		assertEquals("test", reader.toString());
	}

	@Test void g02_toString_afterReading() {
		var reader = new CharSequenceReader("test");
		reader.read(); // Read one character
		assertEquals("test", reader.toString()); // Should still return full string
	}

	//====================================================================================================
	// Different CharSequence types
	//====================================================================================================
	@Test void h01_stringOptimization() {
		var reader = new CharSequenceReader("test");
		var buf = new char[4];
		reader.read(buf, 0, 4);
		assertEquals("test", new String(buf));
	}

	@Test void h02_stringBuilderOptimization() {
		var cs = new StringBuilder("test");
		var reader = new CharSequenceReader(cs);
		var buf = new char[4];
		reader.read(buf, 0, 4);
		assertEquals("test", new String(buf));
	}

	@Test void h03_stringBufferOptimization() {
		var cs = new StringBuffer("test");
		var reader = new CharSequenceReader(cs);
		var buf = new char[4];
		reader.read(buf, 0, 4);
		assertEquals("test", new String(buf));
	}

	@Test void h04_genericCharSequence() {
		// Using a custom CharSequence implementation
		CharSequence cs = new CharSequence() {
			@Override
			public int length() { return 4; }
			@Override
			public char charAt(int index) {
				return "test".charAt(index);
			}
			@Override
			public CharSequence subSequence(int start, int end) {
				return "test".subSequence(start, end);
			}
		};
		var reader = new CharSequenceReader(cs);
		var buf = new char[4];
		reader.read(buf, 0, 4);
		assertEquals("test", new String(buf));
	}
}

