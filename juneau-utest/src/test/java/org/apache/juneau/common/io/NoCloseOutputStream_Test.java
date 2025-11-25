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

class NoCloseOutputStream_Test extends TestBase {

	//====================================================================================================
	// Basic write tests
	//====================================================================================================
	@Test void a01_writeInt() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write(65); // 'A'
		wrapper.write(66); // 'B'
		assertEquals("AB", baos.toString());
	}

	@Test void a02_writeByteArray() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write("test".getBytes());
		assertEquals("test", baos.toString());
	}

	@Test void a03_writeByteArrayWithOffset() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		var bytes = "hello".getBytes();
		wrapper.write(bytes, 1, 3);
		assertEquals("ell", baos.toString());
	}

	//====================================================================================================
	// flush() tests
	//====================================================================================================
	@Test void b01_flush() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write("test".getBytes());
		wrapper.flush(); // Should not throw
		assertEquals("test", baos.toString());
	}

	@Test void b02_flush_multipleTimes() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.flush();
		wrapper.flush();
		wrapper.flush();
		// Should not throw
	}

	//====================================================================================================
	// close() tests - should not actually close
	//====================================================================================================
	@Test void c01_close_doesNotClose() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write("test".getBytes());
		wrapper.close(); // Should flush but not close
		// Should still be able to write after close
		wrapper.write("more".getBytes());
		assertEquals("testmore", baos.toString());
	}

	@Test void c02_close_flushes() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write("test".getBytes());
		wrapper.close();
		// Content should be flushed
		assertEquals("test", baos.toString());
	}

	@Test void c03_close_multipleTimes() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.close();
		wrapper.close();
		wrapper.close();
		// Should not throw
	}

	//====================================================================================================
	// Integration tests
	//====================================================================================================
	@Test void d01_combinedOperations() throws IOException {
		var baos = new ByteArrayOutputStream();
		var wrapper = new NoCloseOutputStream(baos);
		wrapper.write(72); // 'H'
		wrapper.write("ello".getBytes());
		wrapper.write(" World".getBytes(), 0, 6);
		wrapper.flush();
		wrapper.close();
		wrapper.write(33); // '!'
		assertEquals("Hello World!", baos.toString());
	}
}

