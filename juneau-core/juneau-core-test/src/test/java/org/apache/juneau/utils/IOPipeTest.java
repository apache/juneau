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

import static org.junit.Assert.*;
import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.utils.IOPipe.*;
import org.junit.*;

public class IOPipeTest {

	//====================================================================================================
	// IOPipe(Object input, Object output)
	//====================================================================================================
	@Test
	public void testConstructor() throws Exception {
		try { IOPipe.create(null, new StringWriter()); fail(); } catch (IllegalArgumentException e) {}
		try { IOPipe.create(new StringReader(""), null); fail(); } catch (IllegalArgumentException e) {}
		try { IOPipe.create(new Integer(1), new StringWriter()); fail(); } catch (IllegalArgumentException e) {}
		try { IOPipe.create("", new Integer(1)); fail(); } catch (IllegalArgumentException e) {}
	}

	//====================================================================================================
	// closeOut()
	// close(boolean in, boolean out)
	//====================================================================================================
	@Test
	public void testClose() throws Exception {
		TestReader in;
		TestWriter out;
		TestInputStream in2;
		TestOutputStream out2;

		in = new TestReader("foobar");
		out = new TestWriter();
		IOPipe.create(in, out).run();
		assertTrue(in.closed);
		assertFalse(out.closed);
		assertEquals("foobar", out.toString());

		in = new TestReader("foobar");
		out = new TestWriter();
		IOPipe.create(in, out).closeOut().run();
		assertTrue(in.closed);
		assertTrue(out.closed);
		assertEquals("foobar", out.toString());

		in = new TestReader("foobar");
		out = new TestWriter();
		IOPipe.create(in, out).close(false, true).run();
		assertFalse(in.closed);
		assertTrue(out.closed);
		assertEquals("foobar", out.toString());

		in2 = new TestInputStream("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in2, out2).run();
		assertTrue(in2.closed);
		assertFalse(out2.closed);
		assertEquals("foobar", out2.toString());

		in2 = new TestInputStream("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in2, out2).closeOut().run();
		assertTrue(in2.closed);
		assertTrue(out2.closed);
		assertEquals("foobar", out2.toString());

		in2 = new TestInputStream("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in2, out2).close(false, true).run();
		assertFalse(in2.closed);
		assertTrue(out2.closed);
		assertEquals("foobar", out2.toString());

		in = new TestReader("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in, out2).run();
		assertTrue(in.closed);
		assertFalse(out2.closed);
		assertEquals("foobar", out.toString());

		in = new TestReader("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in, out2).closeOut().run();
		assertTrue(in.closed);
		assertTrue(out2.closed);
		assertEquals("foobar", out.toString());

		in = new TestReader("foobar");
		out2 = new TestOutputStream();
		IOPipe.create(in, out2).close(false, true).run();
		assertFalse(in.closed);
		assertTrue(out2.closed);
		assertEquals("foobar", out.toString());

		in2 = new TestInputStream("foobar");
		out = new TestWriter();
		IOPipe.create(in2, out).run();
		assertTrue(in2.closed);
		assertFalse(out.closed);
		assertEquals("foobar", out2.toString());

		in2 = new TestInputStream("foobar");
		out = new TestWriter();
		IOPipe.create(in2, out).closeOut().run();
		assertTrue(in2.closed);
		assertTrue(out.closed);
		assertEquals("foobar", out2.toString());

		in2 = new TestInputStream("foobar");
		out = new TestWriter();
		IOPipe.create(in2, out).close(false, true).run();
		assertFalse(in2.closed);
		assertTrue(out.closed);
		assertEquals("foobar", out2.toString());
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
			super();
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
			super();
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

	//====================================================================================================
	// byLines()
	// byLines(boolean)
	//====================================================================================================
	@Test
	public void testByLines() throws Exception {
		TestReader in;
		TestWriter out;

		in = new TestReader("foo\nbar");
		out = new TestWriter() {
			@Override public void write(String s) {
				super.write("["+s+"]");
			}
		};
		IOPipe.create(in, out).byLines().run();
		assertEquals("[foo][][bar][]", out.toString().replaceAll("[\\r\\n]", ""));

		in = new TestReader("foo\nbar");
		out = new TestWriter() {
			@Override public void write(String s) {
				super.write("["+s+"]");
			}
		};
		IOPipe.create(in, out).byLines(true).run();
		assertEquals("[foo][][bar][]", out.toString().replaceAll("[\\r\\n]", ""));

		in = new TestReader("foo\nbar");
		out = new TestWriter() {
			@Override public void write(String s) {
				super.write("["+s+"]");
			}
		};
		IOPipe.create(in, out).byLines(false).run();
		assertEquals("foo\nbar", out.toString());
	}

	//====================================================================================================
	// lineProcessor()
	//====================================================================================================
	@Test
	public void testLineProcessor() throws Exception {
		TestReader in;
		TestWriter out;
		LineProcessor lp = new LineProcessor() {
			@Override /* LineProcessor */
			public String process(String line) {
				return "[" + line + "]";
			}
		};

		in = new TestReader("foo\nbar");
		out = new TestWriter();
		IOPipe.create(in, out).lineProcessor(lp).run();
		assertEquals("[foo][bar]", out.toString().replaceAll("[\\r\\n]", ""));

		LineProcessor lp2 = new LineProcessor() {
			@Override /* LineProcessor */
			public String process(String line) {
				return line.equals("foo") ? null : line;
			}
		};
		in = new TestReader("foo\nbar");
		out = new TestWriter();
		IOPipe.create(in, out).lineProcessor(lp2).run();
		assertEquals("bar", out.toString().replaceAll("[\\r\\n]", ""));

		TestInputStream in2;
		TestOutputStream out2;
		in2 = new TestInputStream("foo\nbar");
		out2 = new TestOutputStream();
		IOPipe.create(in2, out2).lineProcessor(lp).run();
		assertEquals("[foo][bar]", out2.toString().replaceAll("[\\r\\n]", ""));
	}

	//====================================================================================================
	// buffSize()
	//====================================================================================================
	@Test
	public void testBuffSize() throws Exception {
		TestReader in;
		TestWriter out;

		in = new TestReader("foobar");
		out = new TestWriter();
		IOPipe.create(in, out).buffSize(1).run();
		assertEquals("foobar", out.toString().replaceAll("[\\r\\n]", ""));

		try { IOPipe.create(in, out).buffSize(0); fail(); } catch (IllegalArgumentException e) {}
	}
}
