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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.commons.utils.StringUtils.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.commons.utils.*;
import org.apache.juneau.marshall.*;

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
 * 	<li><code><jk>null</jk></code>
 * </ul>
 *
 * <p>
 * For stream-based parsers, the input object can be any of the following:
 * <ul>
 * 	<li>{@link InputStream}
 * 	<li><code><jk>byte</jk>[]</code>
 * 	<li>{@link String} - Hex-encoded bytes.  (not BASE-64!)
 * 	<li><code><jk>null</jk></code>
 * </ul>
 *
 * <p>
 * Note that Readers and InputStreams will NOT be automatically closed when {@link #close()} is called
 * unless the {@code autoCloseStreams} setting is enabled.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // ParserPipe manages Closeable resources
})
public class ParserPipe implements Closeable {

	private final Object input;
	final boolean debug;
	final boolean strict;
	final boolean autoCloseStreams;
	final boolean unbuffered;
	private final Charset charset;

	private String inputString;
	private InputStream inputStream;
	private Reader reader;
	private boolean doClose;
	private BinaryFormat binaryFormat;
	private Positionable positionable;

	/**
	 * Shortcut constructor, typically for straight string input.
	 *
	 * <p>
	 * Equivalent to calling <code><jk>new</jk> ParserPipe(input, <jk>false</jk>, <jk>false</jk>, <jk>false</jk>, <jk>false</jk>, <jk>null</jk>);</code>
	 *
	 * @param input The input object.
	 * 	<br>Can be <jk>null</jk> (the pipe wraps no data; {@link #getReader()}/{@link #getInputStream()} return <jk>null</jk>).
	 */
	public ParserPipe(Object input) {
		this(input, false, false, false, false, null);
	}

	/**
	 * Constructor for stream-based parsers.
	 *
	 * @param input The parser input object.
	 * 	<br>Can be <jk>null</jk> (the pipe wraps no data; {@link #getReader()}/{@link #getInputStream()} return <jk>null</jk>).
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
	 * 	<br>Can be <jk>null</jk> (string input converts to an empty byte array).
	 */
	public ParserPipe(Object input, boolean debug, boolean autoCloseStreams, boolean unbuffered, BinaryFormat binaryFormat) {
		this.input = input;
		this.debug = debug;
		this.strict = false;
		this.autoCloseStreams = autoCloseStreams;
		this.unbuffered = unbuffered;
		this.charset = null;
		if (input instanceof CharSequence input2)
			this.inputString = input2.toString();
		this.binaryFormat = binaryFormat;
	}

	/**
	 * Constructor for reader-based parsers.
	 *
	 * @param input The parser input object.
	 * 	<br>Can be <jk>null</jk> (the pipe wraps no data; {@link #getReader()}/{@link #getInputStream()} return <jk>null</jk>).
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
	 * @param streamCharset
	 * 	The charset to expect when reading from {@link InputStream InputStreams}.
	 * 	<br>Can be <jk>null</jk> (defaults to UTF-8).
	 */
	public ParserPipe(Object input, boolean debug, boolean strict, boolean autoCloseStreams, boolean unbuffered, Charset streamCharset) {
		this.input = input;
		this.debug = debug;
		this.strict = strict;
		this.autoCloseStreams = autoCloseStreams;
		this.unbuffered = unbuffered;
		this.charset = streamCharset != null ? streamCharset : UTF8;
		if (input instanceof CharSequence cs2)
			this.inputString = cs2.toString();
		this.binaryFormat = null;
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

	@Override /* Overridden from Closeable */
	public void close() {
		try {
			if (doClose)
				IoUtils.close(reader, inputStream);
		} catch (IOException e) {
			throw brex(e);
		}
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
	public Reader getBufferedReader() throws IOException { return toBufferedReader(getReader()); }

	/**
	 * Returns the input to this parser as a plain string.
	 *
	 * <p>
	 * This method only returns a value if {@link org.apache.juneau.marshall.Context.Builder#debug()} is enabled.
	 *
	 * @return The input as a string, or <jk>null</jk> if debug mode not enabled.
	 */
	public String getInputAsString() { return inputString; }

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

		if (input instanceof InputStream input2) {
			if (debug) {
				var b = readBytes(input2);
				inputString = toHex(b);
				inputStream = new ByteArrayInputStream(b);
			} else {
				inputStream = input2;
				doClose = autoCloseStreams;
			}
		} else if (input instanceof byte[] input2) {
			if (debug)
				inputString = toHex(input2);
			inputStream = new ByteArrayInputStream(input2);
			doClose = false;
		} else if (input instanceof String input2) {
			inputString = input2;
			inputStream = new ByteArrayInputStream(convertFromString(input2));
			doClose = false;
		} else {
			throw ioex("Cannot convert object of type %s to an InputStream.", cn(input));
		}

		return inputStream;
	}

	/**
	 * Converts this pipe into a {@link ParserReader}.
	 *
	 * @return The converted pipe, or <jk>null</jk> if the input object is <jk>null</jk>.
	 * @throws IOException Thrown by underlying stream.
	 */
	public ParserReader getParserReader() throws IOException {
		if (input == null)
			return null;
		return input instanceof ParserReader input2 ? input2 : new ParserReader(this);
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
	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for reader initialization logic
	})
	public Reader getReader() throws IOException {
		if (input == null)
			return null;

		if (input instanceof Reader input2) {
			if (debug) {
				inputString = read(input2);
				reader = new StringReader(inputString);
			} else {
				reader = input2;
				doClose = autoCloseStreams;
			}
		} else if (input instanceof CharSequence input2) {
			inputString = input2.toString();
			reader = new ParserReader(this);
			doClose = false;
		} else if (input instanceof InputStream || input instanceof byte[]) {
			doClose = input instanceof InputStream && autoCloseStreams;
			InputStream is = (input instanceof InputStream input2 ? input2 : new ByteArrayInputStream((byte[])input));
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
		} else {
			throw ioex("Cannot convert object of type %s to an InputStream.", cn(input));
		}

		return reader;
	}

	/**
	 * Returns <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 *
	 * @return <jk>true</jk> if the contents passed into this pipe was a {@link CharSequence}.
	 */
	public boolean isString() { return nn(inputString); }

	/**
	 * Sets the ParserReader/ParserInputStream/XmlReader constructed from this pipe.
	 *
	 * <p>
	 * Used for gathering the failure position when {@link ParseException} is thrown.
	 *
	 * @param positionable The ParserReader/ParserInputStream/XmlReader constructed from this pipe.
	 * 	<br>Can be <jk>null</jk> (the failure position reported on {@link ParseException} is {@link Position#UNKNOWN}).
	 */
	public void setPositionable(Positionable positionable) { this.positionable = positionable; }

	private byte[] convertFromString(String in) {
		return binaryFormat == null ? new byte[0] : binaryFormat.parse(in);
	}

	Position getPosition() {
		if (positionable == null)
			return Position.UNKNOWN;
		Position p = positionable.getPosition();
		if (p == null)
			return Position.UNKNOWN;
		return p;
	}
}
