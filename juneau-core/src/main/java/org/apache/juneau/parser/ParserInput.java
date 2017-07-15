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
 */
public class ParserInput {

	private Object input;
	private String inputString;
	private InputStream inputStream;
	private Reader reader, noCloseReader;
	private boolean debug, strict;
	private String fileCharset, inputStreamCharset;

	/**
	 * Constructor.
	 *
	 * @param input
	 * @param debug
	 * @param strict
	 * @param fileCharset
	 * @param inputStreamCharset
	 */
	public ParserInput(Object input, boolean debug, boolean strict, String fileCharset, String inputStreamCharset) {
		this.input = input;
		this.debug = debug;
		this.strict = strict;
		this.fileCharset = fileCharset;
		this.inputStreamCharset = inputStreamCharset;
	}

	/**
	 * Wraps the specified input object inside an input stream.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own input streams.
	 *
	 * @return The input object wrapped in an input stream, or <jk>null</jk> if the object is null.
	 * @throws ParseException If object could not be converted to an input stream.
	 */
	public InputStream getInputStream() throws ParseException {
		try {
			if (input == null)
				return null;
			if (input instanceof InputStream) {
				if (debug) {
					byte[] b = readBytes((InputStream)input, 1024);
					inputString = toHex(b);
					return new ByteArrayInputStream(b);
				}
				return (InputStream)input;
			}
			if (input instanceof byte[]) {
				if (debug)
					inputString = toHex((byte[])input);
				return new ByteArrayInputStream((byte[])input);
			}
			if (input instanceof String) {
				inputString = (String)input;
				return new ByteArrayInputStream(fromHex((String)input));
			}
			if (input instanceof File) {
				if (debug) {
					byte[] b = readBytes((File)input);
					inputString = toHex(b);
					return new ByteArrayInputStream(b);
				}
				inputStream = new FileInputStream((File)input);
				return inputStream;
			}
		} catch (IOException e) {
			throw new ParseException(e);
		}
		throw new ParseException("Cannot convert object of type {0} to an InputStream.", input.getClass().getName());
	}


	/**
	 * Wraps the specified input object inside a reader.
	 *
	 * <p>
	 * Subclasses can override this method to implement their own readers.
	 *
	 * @return The input object wrapped in a Reader, or <jk>null</jk> if the object is null.
	 * @throws Exception If object could not be converted to a reader.
	 */
	public Reader getReader() throws Exception {
		if (input == null)
			return null;
		if (input instanceof Reader) {
			if (debug) {
				inputString = read((Reader)input);
				return new StringReader(inputString);
			}
			return (Reader)input;
		}
		if (input instanceof CharSequence) {
			inputString = input.toString();
			if (reader == null)
				reader = new ParserReader((CharSequence)input);
			return reader;
		}
		if (input instanceof InputStream || input instanceof byte[]) {
			InputStream is = (
				input instanceof InputStream
				? (InputStream)input
				: new ByteArrayInputStream((byte[])input)
			);
			if (noCloseReader == null) {
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
				noCloseReader = new InputStreamReader(is, cd);
			}
			if (debug) {
				inputString = read(noCloseReader);
				return new StringReader(inputString);
			}
			return noCloseReader;
		}
		if (input instanceof File) {
			if (reader == null) {
				CharsetDecoder cd = (
					"default".equalsIgnoreCase(fileCharset)
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
			}
			if (debug) {
				inputString = read(reader);
				return new StringReader(inputString);
			}
			return reader;
		}
		throw new ParseException("Cannot convert object of type {0} to a Reader.", input.getClass().getName());
	}

	/**
	 * Returns the raw input object passed into this session.
	 *
	 * @return The raw input object passed into this session.
	 */
	protected Object getRawInput() {
		return input;
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
	 * Perform cleanup on this context object if necessary.
	 */
	public void close() {
		try {
			if (inputStream != null)
				inputStream.close();
			if (reader != null)
				reader.close();
		} catch (IOException e) {
			throw new BeanRuntimeException(e);
		}
	}
}