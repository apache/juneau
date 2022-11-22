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
package org.apache.juneau.serializer;

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.internal.*;

/**
 * A wrapper around an object that a serializer sends its output to.
 *
 * <p>
 * For character-based serializers, the output object can be any of the following:
 * <ul>
 * 	<li>{@link Writer}
 * 	<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
 * 	<li>{@link File} - Output will be written as system-default encoded stream.
 * 	<li>{@link StringBuilder}
 * </ul>
 *
 * <p>
 * For stream-based serializers, the output object can be any of the following:
 * <ul>
 * 	<li>{@link OutputStream}
 * 	<li>{@link File}
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public final class SerializerPipe implements Closeable {

	private final Object output;
	private final boolean autoClose;

	private OutputStream outputStream;
	private Writer writer;
	private Charset charset;

	/**
	 * Writer-based constructor.
	 *
	 * @param output The object to pipe the serializer output to.
	 */
	SerializerPipe(Object output, Charset streamCharset, Charset fileCharset) {
		boolean isFile = (output instanceof File);
		this.output = output;
		this.autoClose = isFile;
		Charset cs = isFile ? fileCharset : streamCharset;
		if (cs == null)
			cs = isFile ? Charset.defaultCharset() : UTF8;
		this.charset = cs;
	}

	/**
	 * Stream-based constructor.
	 *
	 * @param output The object to pipe the serializer output to.
	 */
	SerializerPipe(Object output) {
		this.output = output;
		this.autoClose = false;
		this.charset = null;
	}

	/**
	 * Wraps the specified output object inside an output stream.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own specialized output streams.
	 *
	 * <p>
	 * This method can be used if the output object is any of the following class types:
	 * <ul>
	 * 	<li>{@link OutputStream}
	 * 	<li>{@link File}
	 * </ul>
	 *
	 * @return
	 * 	The output object wrapped in an output stream.
	 * 	Calling {@link OutputStream#close()} on the returned object simply flushes the response and does not close
	 * 	the underlying stream.
	 * @throws IOException If object could not be converted to an output stream.
	 */
	public OutputStream getOutputStream() throws IOException {
		if (output == null)
			throw new IOException("Output cannot be null.");

		if (output instanceof OutputStream)
			outputStream = (OutputStream)output;
		else if (output instanceof File)
			outputStream = new BufferedOutputStream(new FileOutputStream((File)output));
		else
			throw new IOException("Cannot convert object of type "+className(output)+" to an OutputStream.");

		return new NoCloseOutputStream(outputStream);
	}


	/**
	 * Wraps the specified output object inside a writer.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own specialized writers.
	 *
	 * <p>
	 * This method can be used if the output object is any of the following class types:
	 * <ul>
	 * 	<li>{@link Writer}
	 * 	<li>{@link OutputStream} - Output will be written as UTF-8 encoded stream.
	 * 	<li>{@link File} - Output will be written as system-default encoded stream.
	 * </ul>
	 *
	 * @return
	 * 	The output object wrapped in a writer.
	 * 	Calling {@link Writer#close()} on the returned object simply flushes the response and does not close
	 * 	the underlying writer.
	 * @throws SerializeException If object could not be converted to a writer.
	 */
	public Writer getWriter() throws SerializeException {
		if (output == null)
			throw new SerializeException("Output cannot be null.");

		try {
			if (output instanceof Writer)
				writer = (Writer)output;
			else if (output instanceof OutputStream)
				writer = new OutputStreamWriter((OutputStream)output, charset);
			else if (output instanceof File)
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream((File)output)));
			else if (output instanceof StringBuilder)
				writer = new StringBuilderWriter((StringBuilder)output);
			else
				throw new SerializeException("Cannot convert object of type "+className(output)+" to a Writer.");
		} catch (FileNotFoundException e) {
			throw cast(SerializeException.class, e);
		}

		return new NoCloseWriter(writer);
	}

	/**
	 * Overwrites the writer in this pipe.
	 *
	 * <p>
	 * Used when wrapping the writer returned by {@link #getWriter()} so that the wrapped writer will be flushed
	 * and closed when {@link #close()} is called.
	 *
	 * @param writer The wrapped writer.
	 */
	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	/**
	 * Overwrites the output stream in this pipe.
	 *
	 * <p>
	 * Used when wrapping the stream returned by {@link #getOutputStream()} so that the wrapped stream will be flushed
	 * when {@link #close()} is called.
	 *
	 * @param outputStream The wrapped stream.
	 */
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * Returns the raw output object passed into this session.
	 *
	 * @return The raw output object passed into this session.
	 */
	public Object getRawOutput() {
		return output;
	}

	/**
	 * Closes the output pipe.
	 */
	@Override /* Closeable */
	public void close() {
		try {
			IOUtils.flush(writer, outputStream);
			if (autoClose)
				IOUtils.close(writer, outputStream);
		} catch (IOException e) {
			throw new BeanRuntimeException(e);
		}
	}
}
