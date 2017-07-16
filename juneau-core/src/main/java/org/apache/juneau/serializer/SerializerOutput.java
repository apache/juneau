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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.*;
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
 */
public class SerializerOutput {

	private final Object output;
	private final boolean autoClose;
	private OutputStream outputStream;
	private Writer writer, flushOnlyWriter;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Equivalent to calling <code>SerializerOutput(output, <jk>true</jk>);</code>.
	 *
	 * @param output The object to pipe the serializer output to.
	 */
	public SerializerOutput(Object output) {
		this(output, true);
	}

	/**
	 * Constructor.
	 *
	 * @param output The object to pipe the serializer output to.
	 * @param autoClose Close the stream or writer at the end of the session.
	 */
	public SerializerOutput(Object output, boolean autoClose) {
		this.output = output;
		this.autoClose = autoClose;
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
	 * @return The output object wrapped in an output stream.
	 * @throws Exception If object could not be converted to an output stream.
	 */
	public OutputStream getOutputStream() throws Exception {
		if (output == null)
			throw new SerializeException("Output cannot be null.");
		if (output instanceof OutputStream)
			return (OutputStream)output;
		if (output instanceof File) {
			if (outputStream == null)
				outputStream = new BufferedOutputStream(new FileOutputStream((File)output));
			return outputStream;
		}
		throw new SerializeException("Cannot convert object of type {0} to an OutputStream.", output.getClass().getName());
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
	 * @return The output object wrapped in a Writer.
	 * @throws Exception If object could not be converted to a writer.
	 */
	public Writer getWriter() throws Exception {
		if (output == null)
			throw new SerializeException("Output cannot be null.");
		if (output instanceof Writer)
			return (Writer)output;
		if (output instanceof OutputStream) {
			if (flushOnlyWriter == null)
				flushOnlyWriter = new OutputStreamWriter((OutputStream)output, UTF8);
			return flushOnlyWriter;
		}
		if (output instanceof File) {
			if (writer == null)
				writer = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream((File)output)));
			return writer;
		}
		if (output instanceof StringBuilder) {
			if (writer == null)
				writer = new StringBuilderWriter((StringBuilder)output);
			return writer;
		}
		throw new SerializeException("Cannot convert object of type {0} to a Writer.", output.getClass().getName());
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
	public void close() {
		try {
			if (! autoClose) {
				if (outputStream != null)
					outputStream.flush();
				if (flushOnlyWriter != null)
					flushOnlyWriter.flush();
				if (writer != null)
					writer.flush();
			} else {
				if (outputStream != null)
					outputStream.close();
				if (flushOnlyWriter != null)
					flushOnlyWriter.flush();
				if (writer != null)
					writer.close();
			}
		} catch (IOException e) {
			throw new BeanRuntimeException(e);
		}
	}
}
