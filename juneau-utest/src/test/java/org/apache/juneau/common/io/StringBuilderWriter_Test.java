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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class StringBuilderWriter_Test extends TestBase {

	//====================================================================================================
	// Constructor tests
	//====================================================================================================
	@Test void a01_defaultConstructor() {
		var sbw = new StringBuilderWriter();
		assertEquals("", sbw.toString());
		sbw.write("test");
		assertEquals("test", sbw.toString());
	}

	@Test void a02_constructorWithInitialSize() {
		var sbw = new StringBuilderWriter(100);
		assertEquals("", sbw.toString());
		sbw.write("test");
		assertEquals("test", sbw.toString());
	}

	@Test void a03_constructorWithInitialSize_negative() {
		assertThrows(IllegalArgumentException.class, () -> {
			new StringBuilderWriter(-1);
		});
	}

	@Test void a04_constructorWithStringBuilder() {
		var sb = new StringBuilder("initial");
		var sbw = new StringBuilderWriter(sb);
		assertEquals("initial", sbw.toString());
		sbw.write("test");
		assertEquals("initialtest", sbw.toString());
		// Verify the original StringBuilder is modified
		assertEquals("initialtest", sb.toString());
	}

	@Test void a05_constructorWithStringBuilder_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			new StringBuilderWriter((StringBuilder)null);
		});
	}

	//====================================================================================================
	// write(String) tests
	//====================================================================================================
	@Test void b01_writeString() {
		var sbw = new StringBuilderWriter();
		sbw.write("abc");
		assertEquals("abc", sbw.toString());
		sbw.write("def");
		assertEquals("abcdef", sbw.toString());
	}

	@Test void b02_writeString_null() {
		var sbw = new StringBuilderWriter();
		assertThrows(IllegalArgumentException.class, () -> {
			sbw.write((String)null);
		});
	}

	@Test void b03_writeString_empty() {
		var sbw = new StringBuilderWriter();
		sbw.write("");
		assertEquals("", sbw.toString());
		sbw.write("test");
		assertEquals("test", sbw.toString());
	}

	//====================================================================================================
	// write(String, int, int) tests
	//====================================================================================================
	@Test void b04_writeStringWithOffset() {
		var sbw = new StringBuilderWriter();
		sbw.write("abc", 0, 3);
		assertEquals("abc", sbw.toString());
		sbw.write("def", 1, 2);
		assertEquals("abcef", sbw.toString());
	}

	@Test void b05_writeStringWithOffset_null() {
		var sbw = new StringBuilderWriter();
		assertThrows(IllegalArgumentException.class, () -> {
			sbw.write((String)null, 0, 4);
		});
	}

	@Test void b06_writeStringWithOffset_empty() {
		var sbw = new StringBuilderWriter();
		sbw.write("abc", 0, 0);
		assertEquals("", sbw.toString());
		sbw.write("abc", 1, 1);
		assertEquals("b", sbw.toString());
	}

	@Test void b07_writeStringWithOffset_fullLength() {
		var sbw = new StringBuilderWriter();
		sbw.write("hello", 0, 5);
		assertEquals("hello", sbw.toString());
	}

	//====================================================================================================
	// write(int) tests
	//====================================================================================================
	@Test void c01_writeInt() {
		var sbw = new StringBuilderWriter();
		sbw.write('a');
		assertEquals("a", sbw.toString());
		sbw.write(98); // 'b'
		assertEquals("ab", sbw.toString());
		sbw.write(0); // null character
		assertEquals("ab\u0000", sbw.toString());
	}

	@Test void c02_writeInt_unicode() {
		var sbw = new StringBuilderWriter();
		sbw.write(0x1F600); // 😀 emoji
		assertEquals("\uD83D\uDE00", sbw.toString());
	}

	//====================================================================================================
	// write(char[], int, int) tests
	//====================================================================================================
	@Test void d01_writeCharArray() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		sbw.write(buff, 0, buff.length);
		assertEquals("abc", sbw.toString());
	}

	@Test void d02_writeCharArray_partial() {
		var sbw = new StringBuilderWriter();
		var buff = "abcdef".toCharArray();
		sbw.write(buff, 1, 3);
		assertEquals("bcd", sbw.toString());
	}

	@Test void d03_writeCharArray_empty() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		sbw.write(buff, 0, 0);
		assertEquals("", sbw.toString());
	}

	@Test void d04_writeCharArray_null() {
		var sbw = new StringBuilderWriter();
		assertThrows(IllegalArgumentException.class, () -> {
			sbw.write((char[])null, 0, 0);
		});
	}

	@Test void d05_writeCharArray_indexOutOfBounds_negativeStart() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		assertThrows(IndexOutOfBoundsException.class, () -> {
			sbw.write(buff, -1, buff.length);
		});
	}

	@Test void d06_writeCharArray_indexOutOfBounds_startTooLarge() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		assertThrows(IndexOutOfBoundsException.class, () -> {
			sbw.write(buff, buff.length + 1, 0);
		});
	}

	@Test void d07_writeCharArray_indexOutOfBounds_lengthTooLarge() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		assertThrows(IndexOutOfBoundsException.class, () -> {
			sbw.write(buff, 0, buff.length + 1);
		});
	}

	@Test void d08_writeCharArray_indexOutOfBounds_negativeLength() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		assertThrows(IndexOutOfBoundsException.class, () -> {
			sbw.write(buff, 0, -1);
		});
	}

	@Test void d09_writeCharArray_indexOutOfBounds_startPlusLengthTooLarge() {
		var sbw = new StringBuilderWriter();
		var buff = "abc".toCharArray();
		assertThrows(IndexOutOfBoundsException.class, () -> {
			sbw.write(buff, buff.length - 1, 2);
		});
	}

	//====================================================================================================
	// append(char) tests
	//====================================================================================================
	@Test void e01_appendChar() {
		var sbw = new StringBuilderWriter();
		var result = sbw.append('a');
		assertSame(sbw, result); // Should return this
		assertEquals("a", sbw.toString());
		sbw.append('b');
		assertEquals("ab", sbw.toString());
	}

	@Test void e02_appendChar_unicode() {
		var sbw = new StringBuilderWriter();
		sbw.append('\u00E9'); // é
		assertEquals("é", sbw.toString());
	}

	//====================================================================================================
	// append(CharSequence) tests
	//====================================================================================================
	@Test void f01_appendCharSequence() {
		var sbw = new StringBuilderWriter();
		var result = sbw.append("abc");
		assertSame(sbw, result); // Should return this
		assertEquals("abc", sbw.toString());
		sbw.append("def");
		assertEquals("abcdef", sbw.toString());
	}

	@Test void f02_appendCharSequence_null() {
		var sbw = new StringBuilderWriter();
		sbw.append((CharSequence)null);
		assertEquals("null", sbw.toString());
	}

	@Test void f03_appendCharSequence_StringBuilder() {
		var sbw = new StringBuilderWriter();
		var cs = new StringBuilder("test");
		sbw.append(cs);
		assertEquals("test", sbw.toString());
	}

	@Test void f04_appendCharSequence_StringBuffer() {
		var sbw = new StringBuilderWriter();
		var cs = new StringBuffer("test");
		sbw.append(cs);
		assertEquals("test", sbw.toString());
	}

	//====================================================================================================
	// append(CharSequence, int, int) tests
	//====================================================================================================
	@Test void g01_appendCharSequenceWithRange() {
		var sbw = new StringBuilderWriter();
		var result = sbw.append("abc", 0, 3);
		assertSame(sbw, result); // Should return this
		assertEquals("abc", sbw.toString());
		sbw.append("def", 1, 3);
		assertEquals("abcef", sbw.toString());
	}

	@Test void g02_appendCharSequenceWithRange_null() {
		var sbw = new StringBuilderWriter();
		sbw.append((CharSequence)null, 0, 4);
		assertEquals("null", sbw.toString());
	}

	@Test void g03_appendCharSequenceWithRange_empty() {
		var sbw = new StringBuilderWriter();
		sbw.append("abc", 0, 0);
		assertEquals("", sbw.toString());
		sbw.append("abc", 1, 1);
		assertEquals("", sbw.toString());
	}

	@Test void g04_appendCharSequenceWithRange_fullLength() {
		var sbw = new StringBuilderWriter();
		sbw.append("hello", 0, 5);
		assertEquals("hello", sbw.toString());
	}

	@Test void g05_appendCharSequenceWithRange_StringBuilder() {
		var sbw = new StringBuilderWriter();
		var cs = new StringBuilder("test");
		sbw.append(cs, 1, 3);
		assertEquals("es", sbw.toString());
	}

	//====================================================================================================
	// flush() and close() tests
	//====================================================================================================
	@Test void h01_flush() throws Exception {
		var sbw = new StringBuilderWriter();
		sbw.write("test");
		sbw.flush(); // Should not throw
		assertEquals("test", sbw.toString());
		// Should be able to write after flush
		sbw.write("more");
		assertEquals("testmore", sbw.toString());
	}

	@Test void h02_close() throws Exception {
		var sbw = new StringBuilderWriter();
		sbw.write("test");
		sbw.close(); // Should not throw
		assertEquals("test", sbw.toString());
		// Should be able to write after close
		sbw.write("more");
		assertEquals("testmore", sbw.toString());
	}

	@Test void h03_flushAndClose() throws Exception {
		var sbw = new StringBuilderWriter();
		sbw.write("test");
		sbw.flush();
		sbw.close();
		sbw.flush(); // Should be safe to call multiple times
		sbw.close(); // Should be safe to call multiple times
		assertEquals("test", sbw.toString());
	}

	//====================================================================================================
	// toString() tests
	//====================================================================================================
	@Test void i01_toString() {
		var sbw = new StringBuilderWriter();
		assertEquals("", sbw.toString());
		sbw.write("test");
		assertEquals("test", sbw.toString());
		sbw.write("more");
		assertEquals("testmore", sbw.toString());
	}

	@Test void i02_toString_afterOperations() {
		var sbw = new StringBuilderWriter();
		sbw.write("a");
		sbw.append('b');
		sbw.append("c");
		sbw.append("def", 0, 2);  // Appends "de" (indices 0-1)
		sbw.write(new char[]{'g', 'h', 'i'}, 0, 2);  // Writes "gh" (2 chars from offset 0)
		assertEquals("abcdegh", sbw.toString());
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================
	@Test void j01_combinedOperations() {
		var sbw = new StringBuilderWriter();
		sbw.write("Hello");
		sbw.append(' ');
		sbw.append("World");
		sbw.write("!", 0, 1);
		assertEquals("Hello World!", sbw.toString());
	}

	@Test void j02_chaining() {
		var sbw = new StringBuilderWriter();
		sbw.append('a').append("bc").append("def", 0, 2);
		assertEquals("abcde", sbw.toString());
	}

	@Test void j03_largeContent() {
		var sbw = new StringBuilderWriter(1000);
		for (int i = 0; i < 100; i++) {
			sbw.write("test");
		}
		assertEquals(400, sbw.toString().length());
		assertTrue(sbw.toString().startsWith("test"));
		assertTrue(sbw.toString().endsWith("test"));
	}
}

