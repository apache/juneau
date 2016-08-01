/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.io.*;
import java.nio.charset.*;

/**
 * Various I/O related utility methods.
 *
 * @author jbognar
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
	 * Reads the specified input into a {@link String} until the end of the input is reached.
	 * <p>
	 * 	The {@code Reader} is automatically closed.
	 * <p>
	 * 	If the {@code Reader} is not an instance of a {@code BufferedReader}, then it gets wrapped in a {@code BufferedReader}.
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
	 * <p>
	 * Returns the original reader if it's already one of the following:
	 * <ul>
	 * 	<li>{@link BufferedReader}
	 * 	<li>{@link StringReader}
	 * 	<li>{@link CharSequenceReader}
	 * </ul>
	 *
	 * @param r The reader being wrapped.
	 * @param buffSize The expected size of the input.
	 * @param minBuffSize The minimum buffer size to use if buffSize is too small.
	 * @param maxBuffSize The maximum buffer size to use if buffSize is too large.
	 * @return The wrapped reader.
	 */
	public static Reader getBufferedReader(Reader r, int buffSize, int minBuffSize, int maxBuffSize) {
		assertFieldNotNull(r, "r");
		if (r instanceof BufferedReader || r instanceof StringReader)
			return r;
		if (buffSize <= 0)
			buffSize = 1024;
		if (buffSize < minBuffSize)
			buffSize = minBuffSize;
		else if (buffSize > maxBuffSize)
			buffSize = maxBuffSize;
		return new BufferedReader(r, buffSize);
	}

	/**
	 * Shortcut for {@code getBufferedReader(r, buffSize, 128, 8096)}
	 *
	 * @param r The reader being wrapped.
	 * @param buffSize The expected size of the input.
	 * @return The reader wrapped in a {@link BufferedReader}, or the original {@link Reader} if it's already
	 * 	a buffered reader.
	 */
	public static Reader getBufferedReader(Reader r, int buffSize) {
		return getBufferedReader(r, buffSize, 128, 8096);
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
}
