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

import java.io.*;
import java.nio.charset.*;

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
	 * Reads the contents of a file into a string.
	 *
	 * @param in The file to read using default character encoding.
	 * @return The contents of the reader as a string, or <jk>null</jk> if file does not exist.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(File in) throws IOException {
		if (in == null || ! in.exists())
			return null;
		Reader r = new InputStreamReader(new FileInputStream(in), Charset.defaultCharset());
		return read(r, 0, 1024);
	}

	/**
	 * Reads the specified object to a <code>String</code>.
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
	 * @param o The object to read.
	 * @return The object serialized to a string, or <jk>null</jk> if it wasn't a supported type.
	 * @throws IOException
	 */
	public static String read(Object o) throws IOException {
		if (o instanceof CharSequence)
			return o.toString();
		if (o instanceof File)
			return read((File)o);
		if (o instanceof Reader)
			return read((Reader)o);
		if (o instanceof InputStream)
			return read((InputStream)o);
		if (o instanceof byte[])
			return read(new ByteArrayInputStream((byte[])o));
		return null;
	}

	/**
	 * Writes the contents of the specified <code>Reader</code> to the specified file.
	 *
	 * @param out The file to write the output to.
	 * @param in The reader to pipe from.
	 * @return The number of characters written to the file.
	 * @throws IOException
	 */
	public static int write(File out, Reader in) throws IOException {
		assertFieldNotNull(out, "out");
		assertFieldNotNull(in, "in");
		Writer w = new OutputStreamWriter(new FileOutputStream(out), Charset.defaultCharset());
		try {
			return IOPipe.create(in, w).closeOut().run();
		} finally {
			w.close();
		}
	}

	/**
	 * Writes the contents of the specified <code>InputStream</code> to the specified file.
	 *
	 * @param out The file to write the output to.
	 * @param in The input stream to pipe from.
	 * @return The number of characters written to the file.
	 * @throws IOException
	 */
	public static int write(File out, InputStream in) throws IOException {
		assertFieldNotNull(out, "out");
		assertFieldNotNull(in, "in");
		OutputStream os = new FileOutputStream(out);
		try {
			return IOPipe.create(in, os).closeOut().run();
		} finally {
			os.close();
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
	 * Read the specified input stream into a byte array and closes the stream.
	 *
	 * @param in The input stream.
	 * @param bufferSize The expected size of the buffer.
	 * @return The contents of the stream as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(InputStream in, int bufferSize) throws IOException {
		if (in == null)
			return null;
		ByteArrayOutputStream buff = new ByteArrayOutputStream(bufferSize);
		int nRead;
		byte[] b = new byte[Math.min(bufferSize, 8192)];

		try {
			while ((nRead = in.read(b, 0, b.length)) != -1)
				buff.write(b, 0, nRead);
			buff.flush();

			return buff.toByteArray();
		} finally {
			in.close();
		}
	}

	/**
	 * Reads a raw stream of bytes from the specified file.
	 *
	 * @param f The file to read.
	 * @return A byte array containing the contents of the file.
	 * @throws IOException
	 */
	public static byte[] readBytes(File f) throws IOException {
		if (f == null || ! (f.exists() && f.canRead()))
			return null;

		FileInputStream fis = new FileInputStream(f);
		try {
			return readBytes(fis, (int)f.length());
		} finally {
			fis.close();
		}
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
	 * Pipes the contents of the specified reader into the writer.
	 *
	 * <p>
	 * The reader is closed, the writer is not.
	 *
	 * @param in The reader to pipe from.
	 * @param out The writer to pipe to.
	 * @throws IOException
	 */
	public static void pipe(Reader in, Writer out) throws IOException {
		assertFieldNotNull(out, "out");
		assertFieldNotNull(in, "in");
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
		if (r instanceof BufferedReader || r instanceof StringReader)
			return r;
		return new BufferedReader(r);
	}

	/**
	 * Counts the number of bytes in the input stream and then closes the stream.
	 *
	 * @param is The input stream to read from.
	 * @return The number of bytes read.
	 * @throws IOException
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
	 * @throws IOException
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
			if (! StringUtils.isEmpty(contentLength)) {
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
	 * Converts an object to an <code>InputStream</code>.
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
	 * 		<li><code><jk>null</jk></code> - Returns null.
	 * 	</ul>
	 * @return The object converted to an input stream.
	 * @throws IOException If invalid object passed in or file could not be read.
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
		throw new IOException("Invalid object type passed to IOUtils.toInputStream(Object): " + o.getClass().getName());
	}
}
