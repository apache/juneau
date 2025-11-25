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

package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.IOUtils.*;
import static org.apache.juneau.common.utils.IOUtils.UTF8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link IOUtils}.
 */
class IOUtils_Test extends TestBase {

	//====================================================================================================
	// read(Path)
	//====================================================================================================
	@Test void a01_readPath() throws IOException {
		var p = new Properties();
		p.load(new StringReader(read(Paths.get("src/test/resources/files/Test3.properties"))));
		assertEquals("files/Test3.properties", p.get("file"));
	}

	//====================================================================================================
	// pipe(Reader, Writer)
	//====================================================================================================
	@Test void b01_pipe() throws Exception {
		var out = new TestWriter();
		var in = new TestReader("foobar");

		pipe(in, out);
		assertTrue(in.closed);
		assertFalse(out.closed);
		assertEquals("foobar", out.toString());
	}

	//====================================================================================================
	// loadSystemResourceAsString(String, String...)
	//====================================================================================================
	@Test void c01_loadSystemResourceAsString() throws Exception {
		assertNotNull(loadSystemResourceAsString("test1.txt", "."));
		assertNull(loadSystemResourceAsString("test2.txt", "."));
		assertNull(loadSystemResourceAsString("test3.txt", "sub"));
		assertNull(loadSystemResourceAsString("test3.txt", "sub2"));
		assertNotNull(loadSystemResourceAsString("test3.txt", "."));
		assertNotNull(loadSystemResourceAsString("test4.txt", ".", "sub"));
		assertNotNull(loadSystemResourceAsString("test4.txt", "sub"));
	}

	//====================================================================================================
	// Test helper classes
	//====================================================================================================
	public static class TestReader extends StringReader {
		boolean closed;

		public TestReader(String s) {
			super(s);
		}

		@Override /* Reader */
		public void close() {
			closed = true;
		}
	}

	public static class TestWriter extends StringWriter {
		boolean closed;

		public TestWriter() { /* no-op */ }

		@Override /* Writer */
		public void close() {
			closed = true;
		}
	}

	public static class TestInputStream extends ByteArrayInputStream {
		boolean closed;

		public TestInputStream(String s) {
			super(s.getBytes());
		}

		@Override /* InputStream */
		public void close() throws IOException {
			super.close();
			closed = true;
		}
	}

	public static class TestOutputStream extends ByteArrayOutputStream {
		boolean closed;

		public TestOutputStream() { /* no-op */ }

		@Override /* OutputStream */
		public void close() throws IOException {
			super.close();
			closed = true;
		}

		@Override /* Overridden from Object */
		public synchronized String toString() {
			return new String(this.toByteArray(), UTF8);
		}
	}
}