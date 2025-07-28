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
package org.apache.juneau.utils;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class IOUtilsTest {

	//====================================================================================================
	// read(File)
	// read(InputStream, Charset)
	// read(InputStream)
	// read(Reader, int, int)
	//====================================================================================================
	@Test
	public void testRead() throws Exception {

		TestReader in;
		TestWriter out;

		in = new TestReader("foobar");
		out = new TestWriter();
		pipe(in, out);
		assertTrue(in.closed);
		assertFalse(out.closed);
		assertEquals("foobar", out.toString());
	}

	@Test
	public void testLoadSystemResourceAsString() throws Exception {
		assertNotNull(loadSystemResourceAsString("test1.txt", "."));
		assertNull(loadSystemResourceAsString("test2.txt", "."));
		assertNull(loadSystemResourceAsString("test3.txt", "sub"));
		assertNull(loadSystemResourceAsString("test3.txt", "sub2"));
		assertNotNull(loadSystemResourceAsString("test3.txt", "."));
		assertNotNull(loadSystemResourceAsString("test4.txt", ".", "sub"));
		assertNotNull(loadSystemResourceAsString("test4.txt", "sub"));
	}

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

		public TestWriter() {
		}

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

		public TestOutputStream() {
		}

		@Override /* OutputStream */
		public void close() throws IOException {
			super.close();
			closed = true;
		}

		@Override /* Object */
		public String toString() {
			return new String(this.toByteArray(), UTF8);
		}
	}
}