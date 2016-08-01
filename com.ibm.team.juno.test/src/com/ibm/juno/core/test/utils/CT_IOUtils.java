/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_IOUtils {

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
		IOPipe.create(in, out).run();
		assertTrue(in.closed);
		assertFalse(out.closed);
		assertEquals("foobar", out.toString());
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
			return new String(this.toByteArray(), IOUtils.UTF8);
		}
	}
}
