/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import static com.ibm.juno.core.utils.ThrowableUtils.*;

import java.io.*;
import java.util.*;

/**
 * A utility class for piping input streams and readers to output streams and writers.
 * <p>
 * 	A typical usage is as follows...
 * <p class='bcode'>
 * 	InputStream in = getInputStream();
 * 	Writer out = getWriter();
 * 	IOPipe.create(in, out).closeOut().run();
 * </p>
 * <p>
 * 	By default, the input stream is closed and the output stream is not.
 * 	This can be changed by calling {@link #closeOut()} and {@link #close(boolean, boolean)}.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("hiding")
public class IOPipe {

	private Object input, output;
	private boolean byLines;
	private boolean closeIn = true, closeOut;
	private int buffSize = 1024;
	private LineProcessor lineProcessor;

	private IOPipe(Object input, Object output) {
		assertFieldNotNull(input, "input");
		assertFieldNotNull(output, "output");

		if (input instanceof CharSequence)
			this.input = new StringReader(input.toString());
		else if (input instanceof InputStream || input instanceof Reader)
			this.input = input;
		else
			illegalArg("Invalid input class type.  Must be one of the following:  InputStream, Reader, CharSequence");

		if (output instanceof OutputStream || output instanceof Writer)
			this.output = output;
		else
			illegalArg("Invalid output class type.  Must be one of the following:  OutputStream, Writer");
	}

	/**
	 * Creates a new pipe with the specified input and output.
	 *
	 * @param input The input.  Must be one of the following types:  Reader, InputStream, CharSequence.
	 * @param output The output.  Must be one of the following types:  Writer, OutputStream.
	 * @return This object (for method chaining).
	 */
	public static IOPipe create(Object input, Object output) {
		return new IOPipe(input, output);
	}

	/**
	 * Close output after piping.
	 *
	 * @return This object (for method chaining).
	 */
	public IOPipe closeOut() {
		this.closeOut = true;
		return this;
	}

	/**
	 * Specifies whether to close the input and output after piping.
	 *
	 * @param in Close input stream.  Default is <jk>true</jk>.
	 * @param out Close output stream.  Default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public IOPipe close(boolean in, boolean out) {
		this.closeIn = in;
		this.closeOut = out;
		return this;
	}

	/**
	 * Specifies the temporary buffer size.
	 *
	 * @param buffSize The buffer size.  Default is <code>1024</code>.
	 * @return This object (for method chaining).
	 */
	public IOPipe buffSize(int buffSize) {
		assertFieldPositive(buffSize, "buffSize");
		this.buffSize = buffSize;
		return this;
	}

	/**
	 * Specifies whether the content should be piped line-by-line.
	 * This can be useful if you're trying to pipe console-based input.
	 *
	 * @param byLines Pipe content line-by-line.  Default is <jk>false</jk>.
	 * @return This object (for method chaining).
	 */
	public IOPipe byLines(boolean byLines) {
		this.byLines = byLines;
		return this;
	}

	/**
	 * Sames as calling {@link #byLines()} with <jk>true</jk>.
	 *
	 * @return This object (for method chaining).
	 */
	public IOPipe byLines() {
		this.byLines = true;
		return this;
	}

	/**
	 * Specifies a line processor that can be used to process lines before they're piped to the output.
	 *
	 * @param lineProcessor The line processor.
	 * @return This object (for method chaining).
	 */
	public IOPipe lineProcessor(LineProcessor lineProcessor) {
		this.lineProcessor = lineProcessor;
		return this;
	}

	/**
	 * Interface to implement for the {@link #lineProcessor(LineProcessor)} method.
	 */
	public interface LineProcessor {
		/**
		 * Process the specified line.
		 *
		 * @param line The line to process.
		 * @return The processed line.
		 */
		public String process(String line);
	}

	/**
	 * Performs the piping of the input to the output.
	 *
	 * @return The number of bytes (if streams) or characters (if readers/writers) piped.
	 * @throws IOException
	 */
	public int run() throws IOException {

		int c = 0;

		try {
		if (input instanceof InputStream && output instanceof OutputStream && lineProcessor == null) {
			InputStream in = (InputStream)input;
			OutputStream out = (OutputStream)output;
			byte[] b = new byte[buffSize];
			int i;
				while ((i = in.read(b)) > 0) {
					c += i;
					out.write(b, 0, i);
				}
		} else {
				Reader in = (input instanceof Reader ? (Reader)input : new InputStreamReader((InputStream)input, IOUtils.UTF8));
				Writer out = (output instanceof Writer ? (Writer)output : new OutputStreamWriter((OutputStream)output, IOUtils.UTF8));
				output = out;
				input = in;
				if (byLines || lineProcessor != null) {
					Scanner s = new Scanner(in);
					while (s.hasNextLine()) {
						String l = s.nextLine();
						if (lineProcessor != null)
							l = lineProcessor.process(l);
							if (l != null) {
						out.write(l);
						out.write("\n");
						out.flush();
						c += l.length() + 1;
					}
						}
				} else {
					int i;
					char[] b = new char[buffSize];
					while ((i = in.read(b)) > 0) {
						c += i;
						out.write(b, 0, i);
					}
				}
			}
			} finally {
			closeQuietly(input, output);
		}
		return c;
	}

	private void closeQuietly(Object input, Object output) {
					if (closeIn)
			IOUtils.closeQuietly(input);
					if (closeOut)
			IOUtils.closeQuietly(output);
	}
}
