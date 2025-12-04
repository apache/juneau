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

package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.IOUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.concurrent.atomic.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link IOUtils}.
 */
class IOUtils_Test extends TestBase {

	//====================================================================================================
	// Constructor (line 35)
	//====================================================================================================
	@Test
	void a00_constructor() {
		// Test line 35: class instantiation
		// IOUtils has an implicit public no-arg constructor
		var instance = new IOUtils();
		assertNotNull(instance);
	}

	//====================================================================================================
	// close(Object...)
	//====================================================================================================
	@Test
	void a001_close() throws IOException {
		var is = new TestInputStream("test");
		var os = new TestOutputStream();
		var r = new TestReader("test");
		var w = new TestWriter();

		close(is, os, r, w);

		assertTrue(is.closed);
		assertTrue(os.closed);
		assertTrue(r.closed);
		assertTrue(w.closed);

		// Test with null entries
		close((Object)null, is, null, os);
		// Should not throw

		// Test with empty array
		close();
	}

	//====================================================================================================
	// closeQuietly(InputStream)
	//====================================================================================================
	@Test
	void a002_closeQuietly_InputStream() {
		var is = new TestInputStream("test");
		closeQuietly(is);
		assertTrue(is.closed);

		// Test with null
		closeQuietly((InputStream)null);
		// Should not throw
	}

	//====================================================================================================
	// closeQuietly(Object...)
	//====================================================================================================
	@Test
	void a003_closeQuietly_Object() {
		var is = new TestInputStream("test");
		var os = new TestOutputStream();
		var r = new TestReader("test");
		var w = new TestWriter();

		closeQuietly(is, os, r, w);

		assertTrue(is.closed);
		assertTrue(os.closed);
		assertTrue(r.closed);
		assertTrue(w.closed);

		// Test with null entries
		closeQuietly((Object)null, is, null, os);
		// Should not throw

		// Test with empty array
		closeQuietly();
	}

	//====================================================================================================
	// closeQuietly(OutputStream)
	//====================================================================================================
	@Test
	void a004_closeQuietly_OutputStream() {
		var os = new TestOutputStream();
		closeQuietly(os);
		assertTrue(os.closed);

		// Test with null
		closeQuietly((OutputStream)null);
		// Should not throw
	}

	//====================================================================================================
	// closeQuietly(Reader)
	//====================================================================================================
	@Test
	void a005_closeQuietly_Reader() {
		var r = new TestReader("test");
		closeQuietly(r);
		assertTrue(r.closed);

		// Test with null
		closeQuietly((Reader)null);
		// Should not throw
	}

	//====================================================================================================
	// closeQuietly(Writer)
	//====================================================================================================
	@Test
	void a006_closeQuietly_Writer() {
		var w = new TestWriter();
		closeQuietly(w);
		assertTrue(w.closed);

		// Test with null
		closeQuietly((Writer)null);
		// Should not throw
	}

	//====================================================================================================
	// count(InputStream)
	//====================================================================================================
	@Test
	void a007_count_InputStream() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var count = count(is);
		assertEquals(data.length(), count);
		assertTrue(is.available() == 0 || is.read() == -1); // Stream should be closed/consumed

		// Test with null
		assertEquals(0, count((InputStream)null));
	}

	//====================================================================================================
	// count(Reader)
	//====================================================================================================
	@Test
	void a008_count_Reader() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var count = count(r);
		assertEquals(data.length(), count);

		// Test with null
		assertEquals(0, count((Reader)null));
	}

	//====================================================================================================
	// flush(Object...)
	//====================================================================================================
	@Test
	void a009_flush() throws IOException {
		var os = new ByteArrayOutputStream();
		var w = new StringWriter();

		os.write("test".getBytes());
		w.write("test");

		flush(os, w);

		// Test with null entries
		flush((Object)null, os, null, w);
		// Should not throw

		// Test with empty array
		flush();
	}

	//====================================================================================================
	// loadSystemResourceAsString(String, String...)
	//====================================================================================================
	@Test
	void a010_loadSystemResourceAsString() throws Exception {
		assertNotNull(loadSystemResourceAsString("test1.txt", "."));
		assertNull(loadSystemResourceAsString("test2.txt", "."));
		assertNull(loadSystemResourceAsString("test3.txt", "sub"));
		assertNull(loadSystemResourceAsString("test3.txt", "sub2"));
		assertNotNull(loadSystemResourceAsString("test3.txt", "."));
		assertNotNull(loadSystemResourceAsString("test4.txt", ".", "sub"));
		assertNotNull(loadSystemResourceAsString("test4.txt", "sub"));
	}

	//====================================================================================================
	// pipe(byte[], OutputStream, int)
	//====================================================================================================
	@Test
	void a011_pipe_byteArray_OutputStream_int() throws IOException {
		var data = "Hello World".getBytes();
		var os = new ByteArrayOutputStream();
		var count = pipe(data, os, -1);
		assertEquals(data.length, count);
		assertArrayEquals(data, os.toByteArray());

		// Test with maxBytes
		os = new ByteArrayOutputStream();
		count = pipe(data, os, 5);
		assertEquals(5, count);
		assertEquals(5, os.toByteArray().length);

		// Test with null
		assertEquals(0, pipe((byte[])null, os, -1));
		assertEquals(0, pipe(data, null, -1));
	}

	//====================================================================================================
	// pipe(InputStream, OutputStream)
	//====================================================================================================
	@Test
	void a012_pipe_InputStream_OutputStream() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var os = new ByteArrayOutputStream();
		var count = pipe(is, os);
		assertEquals(data.length(), count);
		assertEquals(data, os.toString());
		assertTrue(is.available() == 0 || is.read() == -1); // Stream should be closed

		// Test with null
		assertEquals(0, pipe((InputStream)null, os));
		assertEquals(0, pipe(is, (OutputStream)null));
	}

	//====================================================================================================
	// pipe(InputStream, OutputStream, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a013_pipe_InputStream_OutputStream_Consumer() {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var os = new ByteArrayOutputStream();
		var exceptionCaught = new AtomicBoolean(false);
		var count = pipe(is, os, e -> exceptionCaught.set(true));
		assertEquals(data.length(), count);
		assertEquals(data, os.toString());
		assertFalse(exceptionCaught.get());
	}

	//====================================================================================================
	// pipe(InputStream, OutputStream, long)
	//====================================================================================================
	@Test
	void a014_pipe_InputStream_OutputStream_long() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var os = new ByteArrayOutputStream();
		var count = pipe(is, os, -1);
		assertEquals(data.length(), count);
		assertEquals(data, os.toString());

		// Test with maxBytes
		is = new ByteArrayInputStream(data.getBytes());
		os = new ByteArrayOutputStream();
		count = pipe(is, os, 5);
		assertEquals(5, count);
		assertEquals(5, os.toString().length());

		// Test with null
		assertEquals(0, pipe((InputStream)null, os, -1));
		assertEquals(0, pipe(is, null, -1));
	}

	//====================================================================================================
	// pipe(InputStream, Writer)
	//====================================================================================================
	@Test
	void a015_pipe_InputStream_Writer() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var w = new StringWriter();
		var count = pipe(is, w);
		assertEquals(data.length(), count);
		assertEquals(data, w.toString());

		// Test with null
		assertEquals(0, pipe((InputStream)null, w));
		assertEquals(0, pipe(is, (Writer)null));
	}

	//====================================================================================================
	// pipe(InputStream, Writer, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a016_pipe_InputStream_Writer_Consumer() {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var w = new StringWriter();
		var exceptionCaught = new AtomicBoolean(false);
		var count = pipe(is, w, e -> exceptionCaught.set(true));
		assertEquals(data.length(), count);
		assertEquals(data, w.toString());
		assertFalse(exceptionCaught.get());
	}

	//====================================================================================================
	// pipe(Reader, File)
	//====================================================================================================
	@Test
	void a017_pipe_Reader_File() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var file = File.createTempFile("test", ".txt");
		try {
			var count = pipe(r, file);
			assertEquals(data.length(), count);
			var content = read(file);
			assertEquals(data, content);
		} finally {
			file.delete();
		}

		// Test with null
		assertEquals(0, pipe((Reader)null, file));
		assertEquals(0, pipe(r, (File)null));
	}

	//====================================================================================================
	// pipe(Reader, OutputStream)
	//====================================================================================================
	@Test
	void a018_pipe_Reader_OutputStream() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var os = new ByteArrayOutputStream();
		var count = pipe(r, os);
		assertEquals(data.length(), count);
		assertEquals(data, os.toString());

		// Test with null
		assertEquals(0, pipe((Reader)null, os));
		assertEquals(0, pipe(r, (OutputStream)null));
	}

	//====================================================================================================
	// pipe(Reader, OutputStream, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a019_pipe_Reader_OutputStream_Consumer() {
		var data = "Hello World";
		var r = new StringReader(data);
		var os = new ByteArrayOutputStream();
		var exceptionCaught = new AtomicBoolean(false);
		var count = pipe(r, os, e -> exceptionCaught.set(true));
		assertEquals(data.length(), count);
		assertEquals(data, os.toString());
		assertFalse(exceptionCaught.get());
	}

	//====================================================================================================
	// pipe(Reader, Writer)
	//====================================================================================================
	@Test
	void a020_pipe_Reader_Writer() throws IOException {
		var data = "foobar";
		var in = new TestReader(data);
		var out = new TestWriter();

		var count = pipe(in, out);
		assertTrue(in.closed);
		assertFalse(out.closed);
		assertEquals(data, out.toString());
		assertEquals(data.length(), count);

		// Test with null
		assertEquals(0, pipe((Reader)null, out));
		assertEquals(0, pipe(in, (Writer)null));
	}

	//====================================================================================================
	// pipe(Reader, Writer, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a021_pipe_Reader_Writer_Consumer() {
		var data = "Hello World";
		var r = new StringReader(data);
		var w = new StringWriter();
		var exceptionCaught = new AtomicBoolean(false);
		var count = pipe(r, w, e -> exceptionCaught.set(true));
		assertEquals(data.length(), count);
		assertEquals(data, w.toString());
		assertFalse(exceptionCaught.get());
	}

	//====================================================================================================
	// pipeLines(Reader, Writer)
	//====================================================================================================
	@Test
	void a022_pipeLines() throws IOException {
		var data = "Line 1\nLine 2\nLine 3";
		var r = new StringReader(data);
		var w = new StringWriter();
		var count = pipeLines(r, w);
		assertEquals(data.length() + 1, count); // +1 for final newline
		assertTrue(w.toString().contains("Line 1"));
		assertTrue(w.toString().contains("Line 2"));
		assertTrue(w.toString().contains("Line 3"));

		// Test with null
		assertEquals(0, pipeLines((Reader)null, w));
		assertEquals(0, pipeLines(r, null));
	}

	//====================================================================================================
	// read(byte[])
	//====================================================================================================
	@Test
	void a023_read_byteArray() {
		var data = "Hello World";
		var bytes = data.getBytes();
		var result = read(bytes);
		assertEquals(data, result);

		// Test with null
		assertNull(read((byte[])null));
	}

	//====================================================================================================
	// read(byte[], Charset)
	//====================================================================================================
	@Test
	void a024_read_byteArray_Charset() {
		var data = "Hello World";
		var bytes = data.getBytes(StandardCharsets.UTF_8);
		var result = read(bytes, StandardCharsets.UTF_8);
		assertEquals(data, result);

		// Test with different charset
		bytes = data.getBytes(StandardCharsets.ISO_8859_1);
		result = read(bytes, StandardCharsets.ISO_8859_1);
		assertEquals(data, result);

		// Test with null
		assertNull(read((byte[])null, StandardCharsets.UTF_8));
	}

	//====================================================================================================
	// read(File)
	//====================================================================================================
	@Test
	void a025_read_File() throws IOException {
		var p = new Properties();
		p.load(new StringReader(read(Paths.get("src/test/resources/files/Test3.properties").toFile())));
		assertEquals("files/Test3.properties", p.get("file"));

		// Test with null
		assertNull(read((File)null));

		// Test with non-existent file
		var nonExistent = new File("nonexistent.txt");
		assertNull(read(nonExistent));
	}

	//====================================================================================================
	// read(InputStream)
	//====================================================================================================
	@Test
	void a026_read_InputStream() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var result = read(is);
		assertEquals(data, result);

		// Test with null
		assertNull(read((InputStream)null));
	}

	//====================================================================================================
	// read(InputStream, Charset)
	//====================================================================================================
	@Test
	void a027_read_InputStream_Charset() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		var result = read(is, StandardCharsets.UTF_8);
		assertEquals(data, result);

		// Test with null
		assertNull(read((InputStream)null, StandardCharsets.UTF_8));
	}

	//====================================================================================================
	// read(InputStream, Charset, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a028_read_InputStream_Charset_Consumer() {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var exceptionCaught = new AtomicBoolean(false);
		var result = read(is, StandardCharsets.UTF_8, e -> exceptionCaught.set(true));
		assertEquals(data, result);
		assertFalse(exceptionCaught.get());

		// Test with null
		assertNull(read((InputStream)null, StandardCharsets.UTF_8, e -> exceptionCaught.set(true)));
	}

	//====================================================================================================
	// read(InputStream, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a029_read_InputStream_Consumer() {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var exceptionCaught = new AtomicBoolean(false);
		var result = read(is, e -> exceptionCaught.set(true));
		assertEquals(data, result);
		assertFalse(exceptionCaught.get());

		// Test with null
		assertNull(read((InputStream)null, e -> exceptionCaught.set(true)));
	}

	//====================================================================================================
	// read(InputStream, int)
	//====================================================================================================
	@Test
	void a030_read_InputStream_int() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		// Note: maxBytes is used for buffer sizing, not limiting the read
		var result = read(is, 5);
		assertEquals(data, result); // Reads all bytes

		// Test with -1 (read all)
		is = new ByteArrayInputStream(data.getBytes());
		result = read(is, -1);
		assertEquals(data, result);

		// Test with null
		assertNull(read((InputStream)null, 10));
	}

	//====================================================================================================
	// read(InputStream, int, Charset)
	//====================================================================================================
	@Test
	void a031_read_InputStream_int_Charset() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
		// Note: maxBytes is used for buffer sizing, not limiting the read
		var result = read(is, 5, StandardCharsets.UTF_8);
		assertEquals(data, result); // Reads all bytes

		// Test with null
		assertNull(read((InputStream)null, 10, StandardCharsets.UTF_8));
	}

	//====================================================================================================
	// read(InputStream, long)
	//====================================================================================================
	@Test
	void a032_read_InputStream_long() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		// Note: maxBytes is used for buffer sizing, not limiting the read
		var result = read(is, 5L);
		assertEquals(data, result); // Reads all bytes

		// Test with null
		assertNull(read((InputStream)null, 10L));
	}

	//====================================================================================================
	// read(Object)
	//====================================================================================================
	@Test
	void a033_read_Object() throws IOException {
		var data = "Hello World";

		// Test with Reader
		var r = new StringReader(data);
		assertEquals(data, read(r));

		// Test with InputStream
		var is = new ByteArrayInputStream(data.getBytes());
		assertEquals(data, read(is));

		// Test with File
		var file = File.createTempFile("test", ".txt");
		try {
			try (var w = new FileWriter(file)) {
				w.write(data);
			}
			assertEquals(data, read(file));
		} finally {
			file.delete();
		}

		// Test with byte[]
		var bytes = data.getBytes();
		assertEquals(data, read(bytes));

		// Test with null
		assertNull(read((Object)null));

		// Test with invalid type
		assertThrows(IllegalArgumentException.class, () -> read(new Object()));
	}

	//====================================================================================================
	// read(Path)
	//====================================================================================================
	@Test
	void a034_read_Path() throws IOException {
		var p = new Properties();
		p.load(new StringReader(read(Paths.get("src/test/resources/files/Test3.properties"))));
		assertEquals("files/Test3.properties", p.get("file"));

		// Test with null
		assertNull(read((Path)null));

		// Test with non-existent path
		var nonExistent = Paths.get("nonexistent.txt");
		assertNull(read(nonExistent));
	}

	//====================================================================================================
	// read(Reader)
	//====================================================================================================
	@Test
	void a035_read_Reader() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var result = read(r);
		assertEquals(data, result);

		// Test with null
		assertNull(read((Reader)null));
	}

	//====================================================================================================
	// read(Reader, Consumer<IOException>)
	//====================================================================================================
	@Test
	void a036_read_Reader_Consumer() {
		var data = "Hello World";
		var r = new StringReader(data);
		var exceptionCaught = new AtomicBoolean(false);
		var result = read(r, e -> exceptionCaught.set(true));
		assertEquals(data, result);
		assertFalse(exceptionCaught.get());

		// Test with null
		assertNull(read((Reader)null, e -> exceptionCaught.set(true)));
	}

	//====================================================================================================
	// read(Reader, long)
	//====================================================================================================
	@Test
	void a037_read_Reader_long() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var result = read(r, data.length());
		assertEquals(data, result);

		// Test with -1 (unknown length)
		r = new StringReader(data);
		result = read(r, -1);
		assertEquals(data, result);

		// Test with null
		assertNull(read((Reader)null, 10L));
	}

	//====================================================================================================
	// readBytes(File)
	//====================================================================================================
	@Test
	void a038_readBytes_File() throws IOException {
		var data = "Hello World";
		var file = File.createTempFile("test", ".txt");
		try {
			try (var w = new FileWriter(file)) {
				w.write(data);
			}
			var bytes = readBytes(file);
			assertArrayEquals(data.getBytes(), bytes);
		} finally {
			file.delete();
		}

		// Test with null
		var bytes = readBytes((File)null);
		assertArrayEquals(new byte[0], bytes);

		// Test with non-existent file
		var nonExistent = new File("nonexistent.txt");
		bytes = readBytes(nonExistent);
		assertArrayEquals(new byte[0], bytes);
	}

	//====================================================================================================
	// readBytes(File, int)
	//====================================================================================================
	@Test
	void a039_readBytes_File_int() throws IOException {
		var data = "Hello World";
		var file = File.createTempFile("test", ".txt");
		try {
			try (var w = new FileWriter(file)) {
				w.write(data);
			}
			// Note: maxBytes is used for buffer sizing, not limiting the read
			var bytes = readBytes(file, 5);
			assertArrayEquals(data.getBytes(), bytes); // Reads all bytes
		} finally {
			file.delete();
		}

		// Test with null
		var bytes = readBytes((File)null, 10);
		assertArrayEquals(new byte[0], bytes);
	}

	//====================================================================================================
	// readBytes(InputStream)
	//====================================================================================================
	@Test
	void a040_readBytes_InputStream() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		var bytes = readBytes(is);
		assertArrayEquals(data.getBytes(), bytes);

		// Test with null
		var bytes2 = readBytes((InputStream)null);
		assertArrayEquals(new byte[0], bytes2);
	}

	//====================================================================================================
	// readBytes(InputStream, int)
	//====================================================================================================
	@Test
	void a041_readBytes_InputStream_int() throws IOException {
		var data = "Hello World";
		var is = new ByteArrayInputStream(data.getBytes());
		// Note: maxBytes is used for buffer sizing, not limiting the read
		var bytes = readBytes(is, 5);
		assertArrayEquals(data.getBytes(), bytes); // Reads all bytes

		// Test with -1 (read all)
		is = new ByteArrayInputStream(data.getBytes());
		bytes = readBytes(is, -1);
		assertArrayEquals(data.getBytes(), bytes);

		// Test with null
		var bytes2 = readBytes((InputStream)null, 10);
		assertArrayEquals(new byte[0], bytes2);
	}

	//====================================================================================================
	// readBytes(Reader)
	//====================================================================================================
	@Test
	void a042_readBytes_Reader() throws IOException {
		var data = "Hello World";
		var r = new StringReader(data);
		var bytes = readBytes(r);
		assertArrayEquals(data.getBytes(), bytes);

		// Test with null
		var bytes2 = readBytes((Reader)null);
		assertArrayEquals(new byte[0], bytes2);
	}

	//====================================================================================================
	// toBufferedReader(Reader)
	//====================================================================================================
	@Test
	void a043_toBufferedReader() {
		var r = new StringReader("test");
		var br = toBufferedReader(r);
		assertSame(r, br); // StringReader should be returned as-is

		// Test with BufferedReader
		var br2 = new BufferedReader(new StringReader("test"));
		var br3 = toBufferedReader(br2);
		assertSame(br2, br3);

		// Test with other Reader
		var fr = new StringReader("test");
		var br4 = toBufferedReader(fr);
		assertSame(fr, br4); // StringReader should be returned as-is

		// Test with FileReader (not StringReader or BufferedReader)
		// We can't easily test this without creating a file, but we can test the logic
		// Test with null
		assertNull(toBufferedReader(null));
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

