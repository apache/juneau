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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;

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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
public final class ParserPipe implements Closeable {

	private final Object input;
	final boolean debug, strict, autoCloseStreams, unbuffered;
	private final Charset charset;

	private String inputString;
	private InputStream inputStream;
	private Reader reader;
	private ParserReader parserReader;
	private boolean doClose;
	private BinaryFormat binaryFormat;
	private Positionable positionable;

	/**
	 * Constructor for reader-based parsers.
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
	 * @param autoCloseStreams
	 * 	Automatically close {@link InputStream InputStreams} and {@link Reader Readers} when passed in as input.
	 * @param unbuffered
	 * 	If <jk>true</jk>, we read one character at a time from underlying readers when the readers are expected to be parsed
	 * 	multiple times.
	 * 	<br>Otherwise, we read character data into a reusable buffer.
	 * @param fileCharset
	 * 	The charset to expect when reading from {@link File Files}.
	 * @param streamCharset
	 * 	The charset to expect when reading from {@link InputStream InputStreams}.
	 */
	public ParserPipe(Object input, boolean debug, boolean strict, boolean autoCloseStreams, boolean unbuffered, Charset streamCharset, Charset fileCharset) {
		boolean isFile = input instanceof File;
		this.input = input;
		this.debug = debug;
		this.strict = strict;
		this.autoCloseStreams = autoCloseStreams;
		this.unbuffered = unbuffered;
		Charset cs = isFile ? fileCharset : streamCharset;
		if (cs == null)
			cs = (isFile ? Charset.defaultCharset() : UTF8);
		this.charset = cs;
		if (input instanceof CharSequence)
			this.inputString = input.toString();
		this.binaryFormat = null;
	}

	/**
	 * Constructor for stream-based parsers.
	 *
	 * @param input The parser input object.
	 * @param debug
	 * 	If <jk>true</jk>, the input contents will be copied locally and accessible via the {@link #getInputAsString()}
	 * 	method.
	 * 	This allows the contents of the pipe to be accessed when a problem occurs.
	 * @param autoCloseStreams
	 * 	Automatically close {@link InputStream InputStreams} and {@link Reader Readers} when passed in as input.
	 * @param unbuffered
	 * 	If <jk>true</jk>, we read one character at a time from underlying readers when the readers are expected to be parsed
	 * 	multiple times.
	 * 	<br>Otherwise, we read character data into a reusable buffer.
	 * @param binaryFormat The binary format of input strings when converted to bytes.
	 */
	public ParserPipe(Object input, boolean debug, boolean autoCloseStreams, boolean unbuffered, BinaryFormat binaryFormat) {
		this.input = input;
		this.debug = debug;
		this.strict = false;
		this.autoCloseStreams = autoCloseStreams;
		this.unbuffered = unbuffered;
		this.charset = null;
		if (input instanceof CharSequence)
			this.inputString = input.toString();
		this.binaryFormat = binaryFormat;
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
		this(input, false, false, false, false, null, null);
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
				byte[] b = readBytes((InputStream)input);
				inputString = toHex(b);
				inputStream = new ByteArrayInputStream(b);
			} else {
				inputStream = (InputStream)input;
				doClose = autoCloseStreams;
			}
		} else if (input instanceof byte[]) {
			if (debug)
				inputString = toHex((byte[])input);
			inputStream = new ByteArrayInputStream((byte[])input);
			doClose = false;
		} else if (input instanceof String) {
			inputString = (String)input;
			inputStream = new ByteArrayInputStream(convertFromString((String)input));
			doClose = false;
		} else if (input instanceof File) {
			if (debug) {
				byte[] b = readBytes((File)input);
				inputString = toHex(b);
				inputStream = new ByteArrayInputStream(b);
			} else {
				inputStream = new FileInputStream((File)input);
				doClose = true;
			}
		} else {
			throw new IOException("Cannot convert object of type "+className(input)+" to an InputStream.");
		}

		return inputStream;
	}

	private byte[] convertFromString(String in) {
		switch(binaryFormat) {
			case BASE64: return base64Decode(in);
			case HEX: return fromHex(in);
			case SPACED_HEX: return fromSpacedHex(in);
			default:	return new byte[0];
		}
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
				doClose = autoCloseStreams;
			}
		} else if (input instanceof CharSequence) {
			inputString = input.toString();
			reader = new ParserReader(this);
			doClose = false;
		} else if (input instanceof InputStream || input instanceof byte[]) {
			doClose = input instanceof InputStream && autoCloseStreams;
			InputStream is = (
				input instanceof InputStream
				? (InputStream)input
				: new ByteArrayInputStream((byte[])input)
			);
			CharsetDecoder cd = charset.newDecoder();
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
			CharsetDecoder cd = charset.newDecoder();
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
			doClose = true;
		} else {
			throw new IOException("Cannot convert object of type "+className(input)+" to an InputStream.");
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
	 * @throws IOException Thrown by underlying stream.
	 */
	public Reader getBufferedReader() throws IOException {
		return toBufferedReader(getReader());
	}

	/**
	 * Returns the input to this parser as a plain string.
	 *
	 * <p>
	 * This method only returns a value if {@link org.apache.juneau.Context.Builder#debug()} is enabled.
	 *
	 * @return The input as a string, or <jk>null</jk> if debug mode not enabled.
	 */
	public String getInputAsString() {
		return inputString;
	}

	/**
	 * Returns the contents of this pipe as a string.
	 *
	 * @return The contents of this pipe as a string.
	 * @throws IOException If thrown from inner reader.
	 */
	public String asString() throws IOException {
		if (inputString == null)
			inputString = read(getReader());
		return inputString;
	}

	/**
	 * Converts this pipe into a {@link ParserReader}.
	 *
	 * @return The converted pipe.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ParserReader getParserReader() throws IOException {
		if (input == null)
			return null;
		if (input instanceof ParserReader)
			parserReader = (ParserReader)input;
		else
			parserReader = new ParserReader(this);
		return parserReader;
	}

	/**
	 * Returns <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 */
	public boolean isString() {
		return inputString != null;
	}

	/**
	 * Sets the ParserReader/ParserInputStream/XmlReader constructed from this pipe.
	 *
	 * <p>
	 * Used for gathering the failure position when {@link ParseException} is thrown.
	 *
	 * @param positionable The ParserReader/ParserInputStream/XmlReader constructed from this pipe.
	 */
	public void setPositionable(Positionable positionable) {
		this.positionable = positionable;
	}

	Position getPosition() {
		if (positionable == null)
			return Position.UNKNOWN;
		Position p = positionable.getPosition();
		if (p == null)
			return Position.UNKNOWN;
		return p;
	}

	@Override /* Closeable */
	public void close() {
		try {
			if (doClose)
				IOUtils.close(reader, inputStream);
		} catch (IOException e) {
			throw new BeanRuntimeException(e);
		}
	}
}