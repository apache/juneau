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
package org.apache.juneau.parser;

import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;

/**
 * A wrapper around an object that a parser reads its input from.
 *
 * <p>
 * For character-based parsers, the input object can be any of the following:
 * <ul>
 * 	<li>{@link Reader}
 * 	<li>{@link CharSequence}
 * 	<li>{@link InputStream}
 * 	<li><code><jk>byte</jk>[]</code>
 * 	<li>{@link File}
 * 	<li><code><jk>null</jk></code>
 * </ul>
 *
 * <p>
 * For stream-based parsers, the input object can be any of the following:
 * <ul>
 * 	<li>{@link InputStream}
 * 	<li><code><jk>byte</jk>[]</code>
 * 	<li>{@link File}
 * 	<li>{@link String} - Hex-encoded bytes.  (not BASE-64!)
 * 	<li><code><jk>null</jk></code>
 * </ul>
 *
 * <p>
 * Note that Readers and InputStreams will NOT be automatically closed when {@link #close()} is called, but
 * streams and readers created from other types (e.g. Files) WILL be automatically closed.
 */
public final class ParserPipe implements Closeable {

	private final Object input;
	private final boolean debug, strict;
	private final String fileCharset, inputStreamCharset;

	private String inputString;
	private InputStream inputStream;
	private Reader reader;

	/**
	 * Constructor.
	 *
	 * @param input The parser input object.
	 * @param debug
	 * 	If <jk>true</jk>, the input contents will be copied locally and accessible via the {@link #getInputAsString()}
	 * 	method.
	 * 	This allows the contents of the pipe to be accessed when a problem occurs.
	 * @param strict
	 * 	If <jk>true</jk>, sets {@link CodingErrorAction#REPORT} on {@link CharsetDecoder#onMalformedInput(CodingErrorAction)}
	 * 	and {@link CharsetDecoder#onUnmappableCharacter(CodingErrorAction)}.
	 * 	Otherwise, sets them to {@link CodingErrorAction#REPLACE}.
	 * @param fileCharset
	 * 	The charset to expect when reading from {@link File Files}.
	 * 	Use <js>"default"</js> to specify {@link Charset#defaultCharset()}.
	 * @param inputStreamCharset
	 * 	The charset to expect when reading from {@link InputStream InputStreams}.
	 * 	Use <js>"default"</js> to specify {@link Charset#defaultCharset()}.
	 */
	public ParserPipe(Object input, boolean debug, boolean strict, String fileCharset, String inputStreamCharset) {
		this.input = input;
		this.debug = debug;
		this.strict = strict;
		this.fileCharset = fileCharset;
		this.inputStreamCharset = inputStreamCharset;
		if (input instanceof CharSequence)
			this.inputString = input.toString();
	}

	/**
	 * Shortcut constructor, typically for straight string input.
	 *
	 * <p>
	 * Equivalent to calling <code><jk>new</jk> ParserPipe(input, <jk>false</jk>, <jk>false</jk>, <jk>null</jk>, <jk>null</jk>);</code>
	 *
	 * @param input The input object.
	 */
	public ParserPipe(Object input) {
		this(input, false, false, null, null);
	}

	/**
	 * Wraps the specified input object inside an input stream.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own input streams.
	 *
	 * @return The input object wrapped in an input stream, or <jk>null</jk> if the object is null.
	 * @throws IOException If object could not be converted to an input stream.
	 */
	public InputStream getInputStream() throws IOException {
		if (input == null)
			return null;

		if (input instanceof InputStream) {
			if (debug) {
				byte[] b = readBytes((InputStream)input, 1024);
				inputString = toHex(b);
				inputStream = new ByteArrayInputStream(b);
			} else {
				inputStream = (InputStream)input;
			}
		} else if (input instanceof byte[]) {
			if (debug)
				inputString = toHex((byte[])input);
			inputStream = new ByteArrayInputStream((byte[])input);
		} else if (input instanceof String) {
			inputString = (String)input;
			inputStream = new ByteArrayInputStream(fromHex((String)input));
		} else if (input instanceof File) {
			if (debug) {
				byte[] b = readBytes((File)input);
				inputString = toHex(b);
				inputStream = new ByteArrayInputStream(b);
			} else {
				inputStream = new FileInputStream((File)input);
			}
		} else {
			throw new IOException("Cannot convert object of type "+input.getClass().getName()+" to an InputStream.");
		}

		return inputStream;
	}


	/**
	 * Wraps the specified input object inside a reader.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own readers.
	 *
	 * @return The input object wrapped in a Reader, or <jk>null</jk> if the object is null.
	 * @throws IOException If object could not be converted to a reader.
	 */
	public Reader getReader() throws IOException {
		if (input == null)
			return null;

		if (input instanceof Reader) {
			if (debug) {
				inputString = read((Reader)input);
				reader = new StringReader(inputString);
			} else {
				reader = (Reader)input;
			}
		} else if (input instanceof CharSequence) {
			inputString = input.toString();
			reader = new ParserReader(this);
		} else if (input instanceof InputStream || input instanceof byte[]) {
			InputStream is = (
				input instanceof InputStream
				? (InputStream)input
				: new ByteArrayInputStream((byte[])input)
			);
			CharsetDecoder cd = (
				"default".equalsIgnoreCase(inputStreamCharset)
				? Charset.defaultCharset()
				: Charset.forName(inputStreamCharset)
			).newDecoder();
			if (strict) {
				cd.onMalformedInput(CodingErrorAction.REPORT);
				cd.onUnmappableCharacter(CodingErrorAction.REPORT);
			} else {
				cd.onMalformedInput(CodingErrorAction.REPLACE);
				cd.onUnmappableCharacter(CodingErrorAction.REPLACE);
			}
			reader = new InputStreamReader(is, cd);
			if (debug) {
				inputString = read(reader);
				reader = new StringReader(inputString);
			}
		} else if (input instanceof File) {
			CharsetDecoder cd = (
				"DEFAULT".equalsIgnoreCase(fileCharset)
				? Charset.defaultCharset()
				: Charset.forName(fileCharset)
			).newDecoder();
			if (strict) {
				cd.onMalformedInput(CodingErrorAction.REPORT);
				cd.onUnmappableCharacter(CodingErrorAction.REPORT);
			} else {
				cd.onMalformedInput(CodingErrorAction.REPLACE);
				cd.onUnmappableCharacter(CodingErrorAction.REPLACE);
			}
			reader = new InputStreamReader(new FileInputStream((File)input), cd);
			if (debug) {
				inputString = read(reader);
				reader = new StringReader(inputString);
			}
		} else {
			throw new IOException("Cannot convert object of type "+input.getClass().getName()+" to a Reader.");
		}

		return reader;
	}

	/**
	 * Returns the contents of this pipe as a buffered reader.
	 *
	 * <p>
	 * If the reader passed into this pipe is already a buffered reader, that reader will be returned.
	 *
	 * @return The contents of this pipe as a buffered reader.
	 * @throws Exception
	 */
	public Reader getBufferedReader() throws Exception {
		return IOUtils.getBufferedReader(getReader());
	}

	/**
	 * Returns the input to this parser as a plain string.
	 *
	 * <p>
	 * This method only returns a value if {@link BeanContext#BEAN_debug} is enabled.
	 *
	 * @return The input as a string, or <jk>null</jk> if debug mode not enabled.
	 */
	public String getInputAsString() {
		return inputString;
	}

	/**
	 * Converts this pipe into a {@link ParserReader}.
	 *
	 * @return The converted pipe.
	 * @throws Exception
	 */
	public ParserReader getParserReader() throws Exception {
		if (input == null)
			return null;
		if (input instanceof ParserReader)
			reader = (ParserReader)input;
		else
			reader = new ParserReader(this);
		return (ParserReader)reader;
	}

	/**
	 * Returns <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 */
	public boolean isString() {
		return inputString != null;
	}

	@Override /* Closeable */
	public void close() {
		try {
			IOUtils.close(reader, inputStream);
		} catch (IOException e) {
			throw new BeanRuntimeException(e);
		}
	}
}