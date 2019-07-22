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
package org.apache.juneau.internal;

import static org.apache.juneau.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Various I/O related utility methods.
 */
public final class IOUtils {

	/** UTF-8 charset */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * Reads the contents of a file into a string.
	 *
	 * @param path The path of the file to read using default character encoding.
	 * @return The contents of the reader as a string, or <jk>null</jk> if file does not exist.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String readFile(String path) throws IOException {
		return read(new File(path));
	}

	/**
	 * Reads the specified object to a <c>String</c>.
	 *
	 * <p>
	 * Can be any of the following object types:
	 * <ul>
	 * 	<li>{@link CharSequence}
	 * 	<li>{@link File}
	 * 	<li>{@link Reader}
	 * 	<li>{@link InputStream}
	 * 	<li><code><jk>byte</jk>[]</code>
	 * </ul>
	 *
	 * @param in The object to read.
	 * @return The object serialized to a string, or <jk>null</jk> if it wasn't a supported type.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static String read(Object in) throws IOException {
		if (in == null)
			return null;
		if (in instanceof CharSequence)
			return in.toString();
		if (in instanceof File)
			return read((File)in);
		if (in instanceof Reader)
			return read((Reader)in);
		if (in instanceof InputStream)
			return read((InputStream)in);
		if (in instanceof byte[])
			return read(new ByteArrayInputStream((byte[])in));
		throw new IOException("Cannot convert object of type '"+in.getClass().getName()+"' to a String.");
	}

	/**
	 * Same as {@link #read(Object)} but appends all the input into a single String.
	 *
	 * @param in The objects to read.
	 * @return The objects serialized to a string, never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static String readAll(Object...in) throws IOException {
		if (in.length == 1)
			return read(in[0]);
		StringWriter sw = new StringWriter();
		for (Object o : in)
			sw.write(emptyIfNull(read(o)));
		return sw.toString();
	}

	/**
	 * Reads the contents of a file into a string.
	 *
	 * @param in The file to read using default character encoding.
	 * @return The contents of the reader as a string, or <jk>null</jk> if file does not exist.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(File in) throws IOException {
		if (in == null || ! in.exists())
			return null;
		try (Reader r = FileReaderBuilder.create(in).build()) {
			return read(r, 0, 1024);
		}
	}

	/**
	 * Reads the contents of a reader into a string.
	 *
	 * @param in The input reader.
	 * @return The contents of the reader as a string.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(Reader in) throws IOException {
		return read(in, 0, 1024);
	}

	/**
	 * Reads the contents of an input stream into a string using the specified charset.
	 *
	 * @param in The input stream.
	 * @param cs The charset of the contents of the input stream.
	 * @return The contents of the reader as a string.  <jk>null</jk> if input stream was null.
	 * @throws IOException If a problem occurred trying to read from the input stream.
	 */
	public static String read(InputStream in, Charset cs) throws IOException {
		if (in == null)
			return null;
		return read(new InputStreamReader(in, cs));
	}

	/**
	 * Reads the contents of an input stream into a string using the system default charset.
	 *
	 * @param in The input stream.
	 * @return The contents of the reader as a string, or <jk>null</jk> if the input stream is null.
	 * @throws IOException If a problem occurred trying to read from the input stream.
	 */
	public static String read(InputStream in) throws IOException {
		if (in == null)
			return null;
		return read(new InputStreamReader(in, Charset.defaultCharset()));
	}

	/**
	 * Reads the specified input into a {@link String} until the end of the input is reached.
	 *
	 * <p>
	 * The {@code Reader} is automatically closed.
	 *
	 * <p>
	 * If the {@code Reader} is not an instance of a {@code BufferedReader}, then it gets wrapped in a
	 * {@code BufferedReader}.
	 *
	 * @param in The input reader.
	 * @param length Specify a positive number if the length of the input is known.
	 * @param bufferSize Specify the buffer size to use.
	 * @return The contents of the reader as a string.  <jk>null</jk> if reader was null.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(Reader in, int length, int bufferSize) throws IOException {
		if (in == null)
			return null;
		if (bufferSize == 0)
			bufferSize = 1024;
		length = (length <= 0 ? bufferSize : length);
		StringBuilder sb = new StringBuilder(length); // Assume they're ASCII characters.
		try {
			char[] buf = new char[Math.min(bufferSize, length)];
			int i = 0;
			while ((i = in.read(buf)) != -1)
				sb.append(buf, 0, i);
			return sb.toString();
		} finally {
			in.close();
		}
	}

	/**
	 * Read the specified object into a byte array.
	 *
	 * @param in
	 * 	The object to read into a byte array.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link InputStream}
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param buffSize
	 * 	The buffer size to use.
	 * @return The contents of the stream as a byte array.
	 * @throws IOException Thrown by underlying stream or if object is not a supported type.
	 */
	public static byte[] readBytes(Object in, int buffSize) throws IOException {
		if (in == null)
			return new byte[0];
		if (in instanceof byte[])
			return (byte[])in;
		if (in instanceof CharSequence)
			return in.toString().getBytes(UTF8);
		if (in instanceof InputStream)
			return readBytes((InputStream)in, buffSize);
		if (in instanceof Reader)
			return read((Reader)in, 0, buffSize).getBytes(UTF8);
		if (in instanceof File)
			return readBytes((File)in, buffSize);
		throw new IOException("Cannot convert object of type '"+in.getClass().getName()+"' to a byte array.");
	}

	/**
	 * Read the specified input stream into a byte array.
	 *
	 * @param in
	 * 	The stream to read into a byte array.
	 * @return The contents of the stream as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(InputStream in) throws IOException {
		return in == null ? null : readBytes(in, 1024);
	}

	/**
	 * Read the specified input stream into a byte array.
	 *
	 * @param in
	 * 	The stream to read into a byte array.
	 * @param buffSize
	 * 	The buffer size to use.
	 * @return The contents of the stream as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(InputStream in, int buffSize) throws IOException {
		if (buffSize == 0)
			buffSize = 1024;
        try (final ByteArrayOutputStream buff = new ByteArrayOutputStream(buffSize)) {
			int nRead;
			byte[] b = new byte[buffSize];
			while ((nRead = in.read(b, 0, b.length)) != -1)
				buff.write(b, 0, nRead);
			buff.flush();
			return buff.toByteArray();
        }
	}

	/**
	 * Read the specified file into a byte array.
	 *
	 * @param in
	 * 	The file to read into a byte array.
	 * @return The contents of the file as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(File in) throws IOException {
		return readBytes(in, 1024);
	}

	/**
	 * Read the specified file into a byte array.
	 *
	 * @param in
	 * 	The file to read into a byte array.
	 * @param buffSize
	 * 	The buffer size to use.
	 * @return The contents of the file as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(File in, int buffSize) throws IOException {
		if (buffSize == 0)
			buffSize = 1024;
		if (! (in.exists() && in.canRead()))
			return new byte[0];
		buffSize = Math.min((int)in.length(), buffSize);
		try (FileInputStream fis = new FileInputStream(in)) {
			return readBytes(fis, buffSize);
		}
	}

	/**
	 * Shortcut for calling <c>readBytes(in, 1024);</c>
	 *
	 * @param in
	 * 	The object to read into a byte array.
	 * 	<br>Can be any of the following types:
	 * 	<ul>
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link InputStream}
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link File}
	 * 	</ul>
	 * @return The contents of the stream as a byte array.
	 * @throws IOException Thrown by underlying stream or if object is not a supported type.
	 */
	public static byte[] readBytes(Object in) throws IOException {
		return readBytes(in, 1024);
	}

	/**
	 * Same as {@link #readBytes(Object)} but appends all the input into a single byte array.
	 *
	 * @param in The objects to read.
	 * @return The objects serialized to a byte array, never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(Object...in) throws IOException {
		if (in.length == 1)
			return readBytes(in[0]);
        try (final ByteArrayOutputStream buff = new ByteArrayOutputStream(1024)) {
			for (Object o : in) {
				byte[] bo = readBytes(o);
				if (bo != null)
					buff.write(bo);
			}
			buff.flush();
			return buff.toByteArray();
        }
	}

	/**
	 * Writes the contents of the specified <c>Reader</c> to the specified file.
	 *
	 * @param out The file to write the output to.
	 * @param in The reader to pipe from.
	 * @return The number of characters written to the file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static int write(File out, Reader in) throws IOException {
		assertFieldNotNull(out, "out");
		assertFieldNotNull(in, "in");
		try (Writer w = FileWriterBuilder.create(out).build()) {
			return IOPipe.create(in, w).run();
		}
	}

	/**
	 * Writes the contents of the specified <c>InputStream</c> to the specified file.
	 *
	 * @param out The file to write the output to.
	 * @param in The input stream to pipe from.
	 * @return The number of characters written to the file.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static int write(File out, InputStream in) throws IOException {
		assertFieldNotNull(out, "out");
		assertFieldNotNull(in, "in");
		try (OutputStream os = new FileOutputStream(out)) {
			return IOPipe.create(in, os).run();
		}
	}

	/**
	 * Pipes the contents of the specified object into the writer.
	 *
	 * <p>
	 * The reader is closed, the writer is not.
	 *
	 * @param in
	 * 	The input to pipe from.
	 * 	Can be any of the types defined by {@link #toReader(Object)}.
	 * @param out
	 * 	The writer to pipe to.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static void pipe(Object in, Writer out) throws IOException {
		IOPipe.create(in, out).run();
	}

	/**
	 * Pipes the contents of the specified object into the output stream.
	 *
	 * <p>
	 * The input stream is closed, the output stream is not.
	 *
	 * @param in
	 * 	The input to pipe from.
	 * 	Can be any of the types defined by {@link #toInputStream(Object)}.
	 * @param out
	 * 	The writer to pipe to.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static void pipe(Object in, OutputStream out) throws IOException {
		IOPipe.create(in, out).run();
	}

	/**
	 * Wraps the specified reader in a buffered reader.
	 *
	 * @param r The reader being wrapped.
	 * @return
	 * 	The reader wrapped in a {@link BufferedReader}, or the original {@link Reader} if it's already a buffered
	 * 	reader.
	 */
	public static Reader getBufferedReader(Reader r) {
		if (r == null || r instanceof BufferedReader || r instanceof StringReader)
			return r;
		return new BufferedReader(r);
	}

	/**
	 * Counts the number of bytes in the input stream and then closes the stream.
	 *
	 * @param is The input stream to read from.
	 * @return The number of bytes read.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long count(InputStream is) throws IOException {
		assertFieldNotNull(is, "is");
		long c = 0;
		long i;
		try {
			while ((i = is.skip(1024)) != 0)
				c += i;
		} finally {
			is.close();
		}
		return c;
	}

	/**
	 * Counts the number of characters in the reader and then closes the reader.
	 *
	 * @param r The reader to read from.
	 * @return The number of characters read.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long count(Reader r) throws IOException {
		assertFieldNotNull(r, "r");
		long c = 0;
		long i;
		try {
			while ((i = r.skip(1024)) != 0)
				c += i;
		} finally {
			r.close();
		}
		return c;
	}

	/**
	 * Given the specified <js>"Content-Length"</js> header value, return an appropriate buffer size.
	 *
	 * <p>
	 * The maximum buffer size is 1MB.
	 *
	 * @param contentLength The value of the <js>"Content-Length"</js> header.
	 * @return The appropriate buffer size.
	 */
	public static int getBufferSize(String contentLength) {
		try {
			if (isNotEmpty(contentLength)) {
				long l = Long.decode(contentLength);
				if (l > 1048576)
					return 1048576;
				if (l <= 0)
					return 8192;
				return (int)l;
			}
		} catch (Exception e) {
			return 8192;
		}
		return 8192;
	}

	/**
	 * Close input stream and ignore any exceptions.
	 *
	 * <p>
	 * No-op if input stream is <jk>null</jk>.
	 *
	 * @param is The input stream to close.
	 */
	public static void closeQuietly(InputStream is) {
		try {
			if (is != null)
				is.close();
		} catch (IOException e) {}
	}

	/**
	 * Close output stream and ignore any exceptions.
	 *
	 * <p>
	 * No-op if output stream is <jk>null</jk>.
	 *
	 * @param os The output stream to close.
	 */
	public static void closeQuietly(OutputStream os) {
		try {
			if (os != null)
				os.close();
		} catch (IOException e) {}
	}

	/**
	 * Close reader and ignore any exceptions.
	 *
	 * <p>
	 * No-op if reader is <jk>null</jk>.
	 *
	 * @param r The reader to close.
	 */
	public static void closeQuietly(Reader r) {
		try {
			if (r != null)
				r.close();
		} catch (IOException e) {}
	}

	/**
	 * Close writer and ignore any exceptions.
	 *
	 * <p>
	 * No-op if writer is <jk>null</jk>.
	 *
	 * @param w The writer to close.
	 */
	public static void closeQuietly(Writer w) {
		try {
			if (w != null)
				w.close();
		} catch (IOException e) {}
	}

	/**
	 * Quietly close all specified input streams, output streams, readers, and writers.
	 *
	 * @param o The list of all objects to quietly close.
	 */
	public static void closeQuietly(Object...o) {
		for (Object o2 : o) {
			if (o2 instanceof InputStream)
				closeQuietly((InputStream)o2);
			if (o2 instanceof OutputStream)
				closeQuietly((OutputStream)o2);
			if (o2 instanceof Reader)
				closeQuietly((Reader)o2);
			if (o2 instanceof Writer)
				closeQuietly((Writer)o2);
		}
	}

	/**
	 * Flushes multiple output streams and writers in a single call.
	 *
	 * @param o
	 * 	The objects to flush.
	 * 	<jk>null</jk> entries are ignored.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static void flush(Object...o) throws IOException {
		IOException ex = null;
		for (Object o2 : o) {
			try {
				if (o2 instanceof OutputStream)
					((OutputStream)o2).flush();
				if (o2 instanceof Writer)
					((Writer)o2).flush();
			} catch (IOException e) {
				ex = e;
			}
		}
		if (ex != null)
			throw ex;
	}

	/**
	 * Close all specified input streams, output streams, readers, and writers.
	 *
	 * @param o
	 * 	The list of all objects to close.
	 * 	<jk>null</jk> entries are ignored.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static void close(Object...o) throws IOException {
		IOException ex = null;
		for (Object o2 : o) {
			try {
				if (o2 instanceof InputStream)
					((InputStream)o2).close();
				if (o2 instanceof OutputStream)
					((OutputStream)o2).close();
				if (o2 instanceof Reader)
					((Reader)o2).close();
				if (o2 instanceof Writer)
					((Writer)o2).close();
			} catch (IOException e) {
				ex = e;
			}
		}
		if (ex != null)
			throw ex;
	}

	/**
	 * Converts an object to a <c>Reader</c>.
	 *
	 * @param o
	 * 	The object to convert to a reader.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link InputStream}
	 * 		<li>{@link Reader}
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li><code><jk>null</jk></code> - Returns <jk>null</jk>.
	 * 	</ul>
	 * @return The object converted to a reader.
	 * @throws IOException If file could not be read.
	 * @throws IllegalArgumentException If invalid object passed in.
	 */
	public static Reader toReader(Object o) throws IOException {
		if (o == null)
			return null;
		if (o instanceof CharSequence)
			return new StringReader(o.toString());
		if (o instanceof File)
			return new FileReader((File)o);
		if (o instanceof Reader)
			return (Reader)o;
		if (o instanceof InputStream)
			return new InputStreamReader((InputStream)o, "UTF-8");
		if (o instanceof byte[])
			return new InputStreamReader(new ByteArrayInputStream((byte[])o), "UTF-8");
		throw new FormattedIllegalArgumentException("Invalid object of type {0} passed to IOUtils.toReader(Object)", o.getClass());
	}

	/**
	 * Converts an object to an <c>InputStream</c>.
	 *
	 * @param o
	 * 	The object to convert to an input stream.
	 * 	Can be any of the following:
	 * 	<ul>
	 * 		<li>{@link InputStream}
	 * 		<li>{@link Reader}
	 * 		<li>{@link File}
	 * 		<li>{@link CharSequence} - Converted to UTF-8 stream.
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li><code><jk>null</jk></code> - Returns <jk>null</jk>.
	 * 	</ul>
	 * @return The object converted to an input stream.
	 * @throws IOException If file could not be read.
	 * @throws IllegalArgumentException If invalid object passed in.
	 */
	public static InputStream toInputStream(Object o) throws IOException {
		if (o == null)
			return null;
		if (o instanceof InputStream)
			return (InputStream)o;
		if (o instanceof File)
			return new FileInputStream((File)o);
		if (o instanceof byte[])
			return new ByteArrayInputStream((byte[])o);
		if (o instanceof CharSequence)
			return new ByteArrayInputStream(((CharSequence)o).toString().getBytes(UTF8));
		if (o instanceof Reader)
			return new ByteArrayInputStream(IOUtils.read((Reader)o).getBytes(UTF8));
		throw new FormattedIllegalArgumentException("Invalid object of type {0} passed to IOUtils.toInputStream(Object)", o.getClass());
	}

	/**
	 * Writes the specified string to the specified file.
	 *
	 * @param path The file path.
	 * @param contents The new file contents.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static void write(String path, String contents) throws IOException {
		write(new File(path), new StringReader(contents));
	}

	/**
	 * Loads a text file from either the file system or classpath.
	 *
	 * @param name The file name.
	 * @param paths The paths to search.
	 * @return The file contents, or <jk>null</jk> if not found.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static String loadSystemResourceAsString(String name, String...paths) throws IOException {
		for (String path : paths) {
			File p = new File(path);
			if (p.exists()) {
	 			File f = new File(p, name);
	 			if (f.exists() && f.canRead())
	 				return read(f);
			}
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		if (cl == null)
			cl = ClassLoader.getSystemClassLoader();
		for (String path : paths) {
			String n = ".".equals(path) ? name : path + '/' + name;
			try (InputStream is = cl.getResourceAsStream(n)) {
				if (is != null)
					return read(is);
			}
			try (InputStream is = ClassLoader.getSystemResourceAsStream(n)) {
				if (is != null)
					return read(is);
			}
		}
		return null;
	}
}
