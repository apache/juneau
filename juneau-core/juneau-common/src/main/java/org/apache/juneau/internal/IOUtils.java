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

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Various I/O related utility methods.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public final class IOUtils {

	/** UTF-8 charset */
	public static final Charset UTF8 = Charset.forName("UTF-8");

	/** Reusable empty input stream. */
	public static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
		@Override
		public int read() {
			return -1;  // end of stream
		}
	};

	private static final int BUFF_SIZE = 1024;
	private static final ThreadLocal<byte[]> BYTE_BUFFER_CACHE = (Boolean.getBoolean("juneau.disableIoBufferReuse") ? null : new ThreadLocal<>());
	private static final ThreadLocal<char[]> CHAR_BUFFER_CACHE = (Boolean.getBoolean("juneau.disableIoBufferReuse") ? null : new ThreadLocal<>());

	static final AtomicInteger BYTE_BUFFER_CACHE_HITS = new AtomicInteger();
	static final AtomicInteger BYTE_BUFFER_CACHE_MISSES = new AtomicInteger();
	static final AtomicInteger CHAR_BUFFER_CACHE_HITS = new AtomicInteger();
	static final AtomicInteger CHAR_BUFFER_CACHE_MISSES = new AtomicInteger();

	static {
		SystemUtils.shutdownMessage(()->"Byte buffer cache:  hits="+BYTE_BUFFER_CACHE_HITS.get()+", misses=" + BYTE_BUFFER_CACHE_MISSES);
		SystemUtils.shutdownMessage(()->"Char buffer cache:  hits="+CHAR_BUFFER_CACHE_HITS.get()+", misses=" + CHAR_BUFFER_CACHE_MISSES);
	}

	/** Reusable empty reader. */
	public static final Reader EMPTY_READER = new Reader() {
		@Override
		public int read() {
			return -1;  // end of stream
		}
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			return -1;  // end of stream
		}
		@Override
		public void close() throws IOException {}
	};

	//-----------------------------------------------------------------------------------------------------------------
	// Piping utilities.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Pipes the contents of the specified <c>Reader</c> to the specified file.
	 *
	 * @param in
	 * 	The reader to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Reader is automatically closed.
	 * @param out
	 * 	The file to write the output to.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	The number of characters piped.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long pipe(Reader in, File out) throws IOException {
		if (out == null || in == null)
			return 0;
		try (Writer w = FileWriterBuilder.create(out).buffered().build()) {
			return pipe(in, w);
		}
	}

	/**
	 * Pipes the contents of the specified <c>Reader</c> to the specified <c>Writer</c>.
	 *
	 * @param in
	 * 	The reader to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Reader is automatically closed.
	 * @param out
	 * 	The file to write the output to.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Writer is flushed but not automatically closed.
	 * @return
	 * 	The number of characters piped.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long pipe(Reader in, Writer out) throws IOException {
		if (out == null || in == null)
			return 0;
		long total = 0;
		try (Reader in2 = in) {
			char[] buffer = charBuffer(-1);
			int readLen;
			while ((readLen = in.read(buffer)) != -1) {
				out.write(buffer, 0, readLen);
				total += readLen;
			}
		}
		out.flush();
		return total;
	}

	/**
	 * Pipes the contents of the specified <c>Reader</c> to the specified <c>Writer</c>.
	 *
	 * @param in
	 * 	The reader to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Reader is automatically closed.
	 * @param out
	 * 	The file to write the output to.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Writer is flushed but not automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return
	 * 	The number of characters piped.
	 */
	public static long pipe(Reader in, Writer out, Consumer<IOException> onException) {
		try {
			return pipe(in, out);
		} catch (IOException e) {
			onException.accept(e);
			return -1;
		}
	}

	/**
	 * Pipes the contents of the specified <c>Reader</c> to the specified <c>Writer</c> a line at a time.
	 *
	 * <p>
	 * Writer is flushed after every line.  Typically useful when writing to consoles.
	 *
	 * @param in
	 * 	The reader to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Reader is automatically closed.
	 * @param out
	 * 	The file to write the output to.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Writer is flushed but not automatically closed.
	 * @return
	 * 	The number of characters piped.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long pipeLines(Reader in, Writer out) throws IOException {
		if (in == null || out == null)
			return 0;
		long total = 0;
		try (Reader in2 = in) {
			try (Scanner s = new Scanner(in2)) {
				while (s.hasNextLine()) {
					String l = s.nextLine();
					if (l != null) {
						out.write(l);
						out.write("\n");
						out.flush();
						total += l.length() + 1;
					}
				}
			}
		}
		return total;
	}

	/**
	 * Pipes the contents of the specified input stream to the writer.
	 *
	 * @param in
	 * 	The stream to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Streams is automatically closed.
	 * @param out
	 * 	The writer to pipe to.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @return
	 * 	The number of bytes written.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static long pipe(InputStream in, Writer out) throws IOException {
		if (in == null || out == null)
			return 0;
		return pipe(new InputStreamReader(in, UTF8), out);
	}

	/**
	 * Pipes the contents of the specified input stream to the writer.
	 *
	 * @param in
	 * 	The stream to pipe from.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Streams is automatically closed.
	 * @param out
	 * 	The writer to pipe to.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return
	 * 	The number of bytes written.
	 */
	public static long pipe(InputStream in, Writer out, Consumer<IOException> onException) {
		try {
			if (in == null || out == null)
				return 0;
			return pipe(new InputStreamReader(in, UTF8), out);
		} catch (IOException e) {
			onException.accept(e);
			return -2;
		}
	}

	/**
	 * Pipes the specified input stream to the specified output stream.
	 *
	 * <p>
	 * Either stream is not automatically closed.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @return The number of bytes written.
	 * @throws IOException If thrown from either stream.
	 */
	public static long pipe(InputStream in, OutputStream out) throws IOException {
		try (InputStream in2 = in) {
			return pipe(in, out, -1);
		}
	}

	/**
	 * Pipes the specified input stream to the specified output stream.
	 *
	 * <p>
	 * Either stream is not automatically closed.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return The number of bytes written.
	 */
	public static long pipe(InputStream in, OutputStream out, Consumer<IOException> onException) {
		try {
			try (InputStream in2 = in) {
				return pipe(in, out, -1);
			}
		} catch (IOException e) {
			onException.accept(e);
			return -1;
		}
	}

	/**
	 * Pipes the specified input stream to the specified output stream.
	 *
	 * <p>
	 * Either stream is not automatically closed.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param maxBytes
	 * 	The maximum number of bytes or <c>-1</c> to read the entire input stream.
	 * @return The number of bytes written.
	 * @throws IOException If thrown from either stream.
	 */
	public static long pipe(InputStream in, OutputStream out, long maxBytes) throws IOException {
		if (in == null || out == null)
			return 0;
		byte[] buffer = byteBuffer((int)maxBytes);
		int readLen;
		long total = 0;
		if (maxBytes < 0) {
			while ((readLen = in.read(buffer)) != -1) {
				out.write(buffer, 0, readLen);
				total += readLen;
			}
		} else {
			long remaining = maxBytes;
			while (remaining > 0) {
				readLen = in.read(buffer, 0, buffSize(remaining));
				if (readLen == -1)
					break;
				out.write(buffer, 0, readLen);
				total += readLen;
				remaining -= readLen;
			}
		}
		out.flush();
		return total;
	}

	/**
	 * Pipes the specified reader to the specified output stream.
	 *
	 * @param in
	 * 	The input reader.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @return The number of bytes written.
	 * @throws IOException If thrown from output stream.
	 */
	public static long pipe(Reader in, OutputStream out) throws IOException {
		if (in == null || out == null)
			return 0;
		long total = 0;
		try (Reader in2 = in) {
			OutputStreamWriter osw = new OutputStreamWriter(out, UTF8);
			int i;
			char[] b = charBuffer(-1);
			while ((i = in.read(b)) > 0) {
				total += i;
				osw.write(b, 0, i);
			}
			osw.flush();
		}
		return total;
	}

	/**
	 * Pipes the specified reader to the specified output stream.
	 *
	 * @param in
	 * 	The input reader.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return The number of bytes written.
	 */
	public static long pipe(Reader in, OutputStream out, Consumer<IOException> onException) {
		try {
			return pipe(in, out);
		} catch (IOException e) {
			onException.accept(e);
			return -1;
		}
	}

	/**
	 * Pipes the specified byte array to the specified output stream.
	 *
	 * @param in
	 * 	The input byte array.
	 * 	<br>Can be <jk>null</jk>.
	 * @param out
	 * 	The output stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param maxBytes
	 * 	The maximum number of bytes or <c>-1</c> to read the entire byte array.
	 * @return The number of bytes written.
	 * @throws IOException If thrown from output stream.
	 */
	public static final long pipe(byte[] in, OutputStream out, int maxBytes) throws IOException {
		if (in == null || out == null)
			return 0;
		int length = (maxBytes < 0 || maxBytes > in.length ) ? in.length : maxBytes;
		out.write(in, 0, length);
		return length;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Reading utilities.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Reads the specified byte array containing UTF-8 into a string.
	 *
	 * @param in
	 * 	The input.
	 * 	<br>Can be <jk>null</jk>.
	 * @return The new string, or <jk>null</jk> if the input was null.
	 */
	public static String read(byte[] in) {
		return read(in, UTF8);
	}

	/**
	 * Reads the specified byte array into a string.
	 *
	 * @param in
	 * 	The input.
	 * 	<br>Can be <jk>null</jk>.
	 * @param charset The character set to use for decoding.
	 * @return The new string, or <jk>null</jk> if the input was null.
	 */
	public static String read(byte[] in, Charset charset) {
		if (in == null)
			return null;
		return new String(in, charset);
	}

	/**
	 * Reads the contents of a file into a string.
	 *
	 * <p>
	 * Assumes default character encoding.
	 *
	 * @param in
	 * 	The file to read.
	 * 	<br>Can be <jk>null</jk>.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if file does not exist.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(File in) throws IOException {
		if (in == null || ! in.exists())
			return null;
		try (Reader r = FileReaderBuilder.create(in).build()) {
			return read(r, in.length());
		}
	}

	/**
	 * Reads the contents of a reader into a string.
	 *
	 * @param in
	 * 	The input reader.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if the reader was <jk>null</jk>.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(Reader in) throws IOException {
		try (Reader in2 = in) {
			return read(in, -1);
		}
	}

	/**
	 * Reads the contents of a reader into a string.
	 *
	 * @param in
	 * 	The input reader.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if the reader was <jk>null</jk>.
	 */
	public static String read(Reader in, Consumer<IOException> onException) {
		try (Reader in2 = in) {
			return read(in, -1);
		} catch (IOException e) {
			onException.accept(e);
			return null;
		}
	}

	/**
	 * Reads the specified input into a {@link String} until the end of the input is reached.
	 *
	 * @param in
	 * 	The input reader.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>String is automatically closed.
	 * @param expectedLength
	 * 	Specify a positive number if the length of the input is known, or <c>-1</c> if unknown.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if the reader was <jk>null</jk>.
	 * @throws IOException If a problem occurred trying to read from the reader.
	 */
	public static String read(Reader in, long expectedLength) throws IOException {
		if (in == null)
			return null;
		try (Reader in2 = in) {
			StringBuilder sb = new StringBuilder(buffSize(expectedLength)); // Assume they're ASCII characters.
			char[] buf = charBuffer((int)expectedLength);
			int i = 0;
			while ((i = in2.read(buf)) != -1)
				sb.append(buf, 0, i);
			return sb.toString();
		}
	}

	/**
	 * Reads the contents of an input stream into a string.
	 *
	 * <p>
	 * Assumes UTF-8 encoding.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if the input stream was <jk>null</jk>.
	 * @throws IOException If a problem occurred trying to read from the input stream.
	 */
	public static String read(InputStream in) throws IOException {
		return read(in, UTF8);
	}

	/**
	 * Reads the contents of an input stream into a string.
	 *
	 * <p>
	 * Assumes UTF-8 encoding.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return
	 * 	The contents of the reader as a string, or <jk>null</jk> if the input stream was <jk>null</jk>.
	 */
	public static String read(InputStream in, Consumer<IOException> onException) {
		return read(in, UTF8, onException);
	}

	/**
	 * Reads the contents of an input stream into a string using the specified charset.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param cs
	 * 	The charset of the contents of the input stream.
	 * @return
	 * 	The contents of the reader as a string or <jk>null</jk> if input stream was <jk>null</jk>.
	 * @throws IOException If a problem occurred trying to read from the input stream.
	 */
	public static String read(InputStream in, Charset cs) throws IOException {
		if (in == null)
			return null;
		try (InputStreamReader isr = new InputStreamReader(in, cs)) {
			return read(isr);
		}
	}

	/**
	 * Reads the contents of an input stream into a string using the specified charset.
	 *
	 * @param in
	 * 	The input stream.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @param cs
	 * 	The charset of the contents of the input stream.
	 * @param onException Consumer of any {@link IOException I/O exceptions}.
	 * @return
	 * 	The contents of the reader as a string or <jk>null</jk> if input stream was <jk>null</jk>.
	 */
	public static String read(InputStream in, Charset cs, Consumer<IOException> onException) {
		if (in == null)
			return null;
		try (InputStreamReader isr = new InputStreamReader(in, cs)) {
			return read(isr);
		} catch (IOException e) {
			onException.accept(e);
			return null;
		}
	}

	/**
	 * Reads the specified input stream into the specified byte array.
	 *
	 * @param in
	 * 	The input stream to read.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return A byte array containing the contents.  Never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(InputStream in) throws IOException {
		try (InputStream in2 = in) {
			return readBytes(in2, -1);
		}
	}

	/**
	 * Reads the specified input stream into the specified byte array.
	 *
	 * @param in
	 * 	The input stream to read.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is not automatically closed.
	 * @param maxBytes
	 * 	The maximum number of bytes or <c>-1</c> to read the entire stream.
	 * @return A byte array containing the contents.  Never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(InputStream in, int maxBytes) throws IOException {
		if (in == null)
			return new byte[0];
		ByteArrayOutputStream buff = new ByteArrayOutputStream(buffSize(maxBytes));
		int nRead;
		byte[] b = byteBuffer(maxBytes);
		while ((nRead = in.read(b, 0, b.length)) != -1)
			buff.write(b, 0, nRead);
		buff.flush();
		return buff.toByteArray();
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
		return readBytes(in, -1);
	}

	/**
	 * Read the specified file into a byte array.
	 *
	 * @param in
	 * 	The file to read into a byte array.
	 * @param maxBytes
	 * 	The maximum number of bytes to read, or <jk>-1</jk> to read all bytes.
	 * @return The contents of the file as a byte array.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(File in, int maxBytes) throws IOException {
		if (in == null || ! (in.exists() && in.canRead()))
			return new byte[0];
		try (FileInputStream is = new FileInputStream(in)) {
			return readBytes(is, maxBytes);
		}
	}

	/**
	 * Reads the specified input stream into the specified byte array.
	 *
	 * @param in
	 * 	The input stream to read.
	 * 	<br>Can be <jk>null</jk>.
	 * 	<br>Stream is automatically closed.
	 * @return A byte array containing the contents.  Never <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static byte[] readBytes(Reader in) throws IOException {
		if (in == null)
			return new byte[0];
		try (Reader in2 = in) {
			return read(in2, -1).getBytes();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other utilities.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Wraps the specified reader in a buffered reader.
	 *
	 * @param r The reader being wrapped.
	 * @return
	 * 	The reader wrapped in a {@link BufferedReader}, or the original {@link Reader} if it's already a buffered
	 * 	reader.
	 */
	public static Reader toBufferedReader(Reader r) {
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
		if (is == null)
			return 0;
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
		if (r == null)
			return 0;
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

	private static final byte[] byteBuffer(int maxBytes) {
		if (BYTE_BUFFER_CACHE != null) {
			byte[] x = BYTE_BUFFER_CACHE.get();
			if (x == null) {
				x = new byte[BUFF_SIZE];
				BYTE_BUFFER_CACHE.set(x);
				BYTE_BUFFER_CACHE_MISSES.incrementAndGet();
			} else {
				BYTE_BUFFER_CACHE_HITS.incrementAndGet();
			}
			return x;
		}
		return new byte[buffSize(maxBytes)];
	}

	private static final char[] charBuffer(int maxChars) {
		if (CHAR_BUFFER_CACHE != null) {
			char[] x = CHAR_BUFFER_CACHE.get();
			if (x == null) {
				x = new char[BUFF_SIZE];
				CHAR_BUFFER_CACHE.set(x);
				CHAR_BUFFER_CACHE_MISSES.incrementAndGet();
			} else {
				CHAR_BUFFER_CACHE_HITS.incrementAndGet();
			}
			return x;
		}
		return new char[buffSize(maxChars)];
	}

	private static final int buffSize(long max) {
		return (max > 0 && max < BUFF_SIZE) ? (int)max : BUFF_SIZE;
	}
}
