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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class NoCloseWriter_Test extends TestBase {

	//====================================================================================================
	// Basic write tests
	//====================================================================================================
	@Test void a01_writeInt() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write(65); // 'A'
		wrapper.write(66); // 'B'
		assertEquals("AB", sw.toString());
	}

	@Test void a02_writeString() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("test");
		assertEquals("test", sw.toString());
	}

	@Test void a03_writeStringWithOffset() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("hello", 1, 3);
		assertEquals("ell", sw.toString());
	}

	@Test void a04_writeCharArray() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("test".toCharArray());
		assertEquals("test", sw.toString());
	}

	@Test void a05_writeCharArrayWithOffset() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("hello".toCharArray(), 1, 3);
		assertEquals("ell", sw.toString());
	}

	//====================================================================================================
	// append() tests
	//====================================================================================================
	@Test void b01_appendChar() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		var result = wrapper.append('a');
		assertSame(sw, result); // Returns the underlying writer
		wrapper.append('b');
		assertEquals("ab", sw.toString());
	}

	@Test void b02_appendCharSequence() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		var result = wrapper.append("test");
		assertSame(sw, result); // Returns the underlying writer
		assertEquals("test", sw.toString());
	}

	@Test void b03_appendCharSequenceWithRange() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		var result = wrapper.append("hello", 1, 4);
		assertSame(sw, result); // Returns the underlying writer
		assertEquals("ell", sw.toString());
	}

	@Test void b04_appendNull() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.append((CharSequence)null);
		assertEquals("null", sw.toString());
	}

	//====================================================================================================
	// flush() tests
	//====================================================================================================
	@Test void c01_flush() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("test");
		wrapper.flush(); // Should not throw
		assertEquals("test", sw.toString());
	}

	@Test void c02_flush_multipleTimes() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.flush();
		wrapper.flush();
		wrapper.flush();
		// Should not throw
	}

	//====================================================================================================
	// close() tests - should not actually close
	//====================================================================================================
	@Test void d01_close_doesNotClose() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("test");
		wrapper.close(); // Should flush but not close
		// Should still be able to write after close
		wrapper.write("more");
		assertEquals("testmore", sw.toString());
	}

	@Test void d02_close_flushes() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write("test");
		wrapper.close();
		// Content should be flushed
		assertEquals("test", sw.toString());
	}

	@Test void d03_close_multipleTimes() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.close();
		wrapper.close();
		wrapper.close();
		// Should not throw
	}

	//====================================================================================================
	// toString() tests
	//====================================================================================================
	@Test void e01_toString() {
		var sw = new StringWriter();
		sw.write("test");
		var wrapper = new NoCloseWriter(sw);
		assertEquals("test", wrapper.toString());
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================
	@Test void f01_combinedOperations() throws IOException {
		var sw = new StringWriter();
		var wrapper = new NoCloseWriter(sw);
		wrapper.write('H');
		wrapper.write("ello");
		wrapper.append(' ');
		wrapper.append("World");
		wrapper.write("!", 0, 1);
		wrapper.flush();
		wrapper.close();
		wrapper.write('!');
		assertEquals("Hello World!!", sw.toString());
	}
}

