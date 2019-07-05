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

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;

/**
 * Simple wrapper around a standard {@link Writer} with additional methods.
 *
 * <p>
 * Modeled after the Java ProcessBuilder class so that you can chain commands to reduce the need for string
 * concatenation for performance reasons.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode w800'>
 * 	writer.append(<js>"foo"</js>).nl().i(5).append(<js>"bar"</js>);
 * </p>
 */
public class SerializerWriter extends Writer {

	/** The underlying writer. */
	protected final Writer out;

	/** Use-whitespace flag. */
	protected final boolean useWhitespace;

	/** Max indentation levels. */
	protected final int maxIndent;

	/** Trim strings flag. */
	protected final boolean trimStrings;

	/** The quote character being used by this writer. */
	protected final char quoteChar;

	/** The URI resolver of the request. */
	protected final UriResolver uriResolver;

	/**
	 * @param out The writer being wrapped.
	 * @param useWhitespace
	 * 	If <jk>true</jk>, calling {@link #cr(int)} will create an indentation and calling {@link #s()} will write a
	 * 	space character.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param quoteChar The character to write when {@link #q()} is called.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	public SerializerWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar,
			UriResolver uriResolver) {
		this.out = out;
		this.useWhitespace = useWhitespace;
		this.maxIndent = maxIndent;
		this.trimStrings = trimStrings;
		this.quoteChar = quoteChar;
		this.uriResolver = uriResolver;
	}

	/**
	 * Performs a carriage return.
	 *
	 * <p>
	 * Adds a newline and the specified number of tabs (if the {@code useWhitespace} setting is enabled) to the output.
	 *
	 * @param depth The indentation.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter cr(int depth) throws IOException {
		if (useWhitespace && depth <= maxIndent)
			return nl(depth).i(depth);
		return this;
	}

	/**
	 * Performs a carriage return at the end of a line.
	 *
	 * <p>
	 * Adds a newline and the specified number of tabs (if the {@code useWhitespace} setting is enabled) to the output.
	 *
	 * @param depth The indentation.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter cre(int depth) throws IOException {
		if (useWhitespace && depth <= maxIndent-1)
			return nl(depth).i(depth);
		return this;
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text, followed by a newline
	 * (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param indent The number of tabs to indent.
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	public SerializerWriter appendln(int indent, String text) throws IOException {
		return append(indent, true, text);
	}

	/**
	 * Writes the specified text followed by a newline (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	public SerializerWriter appendln(String text) throws IOException {
		return append(0, true, text);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text.
	 *
	 * @param indent The number of tabs to indent.
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	public SerializerWriter append(int indent, String text) throws IOException {
		return append(indent, false, text);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text.
	 *
	 * @param indent The number of tabs to indent.
	 * @param c The character to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	public SerializerWriter append(int indent, char c) throws IOException {
		return i(indent).append(c);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text, optionally followed by a
	 * newline (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param indent The number of tabs to indent.
	 * @param newline If <jk>true</jk>, then a newline is written.
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	private SerializerWriter append(int indent, boolean newline, String text) throws IOException {

		// If text contains newlines, we break it up into lines and indent them separately.
		if (text.indexOf('\n') != -1 && useWhitespace && indent <= maxIndent) {
			for (StringTokenizer st = new StringTokenizer(text, "\n"); st.hasMoreTokens();) {
				i(indent);
				out.write(st.nextToken());
				out.write("\n");
			}
		} else {
			i(indent);
			out.write(text);
		}
		if (newline)
			nl(indent);
		return this;
	}

	/**
	 * Appends the specified object as a URI.
	 *
	 * <p>
	 * Object is converted to a <c>String</c> using <c>toString()</c>, so this will work on {@link URL} or
	 * {@link URI} objects, or any other type that returns a URI via it's <c>toString()</c> method.
	 *
	 * <p>
	 * The URI is resolved based on the {@link Serializer#SERIALIZER_uriRelativity} and
	 * {@link Serializer#SERIALIZER_uriResolution} settings and the {@link UriContext} that's part of the
	 * session.
	 *
	 * @param uri The URI to serialize.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter appendUri(Object uri) throws IOException {
		uriResolver.append(this, uri);
		return this;
	}

	/**
	 * Appends the specified characters to this writer.
	 *
	 * @param characters The characters to append to this writer.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 */
	public SerializerWriter append(char[] characters) throws IOException {
		for (char c : characters)
			append(c);
		return this;
	}

	/**
	 * Adds a whitespace character to the output if the {@code useWhitespace} setting is enabled.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter s() throws IOException {
		if (useWhitespace)
			out.write(' ');
		return this;
	}

	/**
	 * Adds the quote character specified by the {@code quoteChar} setting to the output.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter q() throws IOException {
		out.write(quoteChar);
		return this;
	}

	/**
	 * Writes an indent to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The number of tabs to indent.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter i(int indent) throws IOException {
		if (useWhitespace && indent <= maxIndent)
			for (int i = 0; i < indent; i++)
				out.write('\t');
		return this;
	}

	/**
	 * Writes an end-of-line indent to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The number of tabs to indent.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter ie(int indent) throws IOException {
		if (useWhitespace && indent <= maxIndent-1)
			for (int i = 0; i < indent; i++)
				out.write('\t');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The current indentation level.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter nl(int indent) throws IOException {
		if (useWhitespace && indent <= maxIndent)
			out.write('\n');
		return this;
	}

	/**
	 * Writes a space if the boolean expression is <jk>true</jk> and {@code useWhitespace} is false.
	 *
	 * <p>
	 * Intended for cases in XML where text should be separated by either a space or newline.
	 * This ensures the text is separated by a space if whitespace is disabled.
	 *
	 * @param b The boolean flag.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter sIf(boolean b) throws IOException {
		if (b && ! useWhitespace)
			out.write(' ');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useWhitespace} setting is enabled and the boolean flag is true.
	 *
	 * @param b The boolean flag.
	 * @param indent The current indentation level.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter nlIf(boolean b, int indent) throws IOException {
		if (b && useWhitespace && indent <= maxIndent)
			out.write('\n');
		return this;
	}

	/**
	 * Writes the specified text to the writer.
	 *
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter append(Object text) throws IOException {
		out.append(text == null ? null : text.toString());
		return this;
	}

	/**
	 * Writes the specified text to the writer.
	 *
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter append(String text) throws IOException {
		if (text != null)
			out.append(text);
		return this;
	}

	/**
	 * Writes the specified text to the writer if b is true.
	 *
	 * @param b Boolean flag.
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter appendIf(boolean b, String text) throws IOException {
		if (b)
			out.write(text);
		return this;
	}

	/**
	 * Writes the specified text to the writer if b is true.
	 *
	 * @param b Boolean flag.
	 * @param c The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter appendIf(boolean b, char c) throws IOException {
		if (b)
			out.write(c);
		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Writer */
	public SerializerWriter append(char c) throws IOException {
		out.write(c);
		return this;
	}

	@Override /* Writer */
	public void write(char[] cbuf, int off, int len) throws IOException {
		out.write(cbuf, off, len);
	}

	@Override /* Writer */
	public void flush() throws IOException {
		out.flush();
	}

	@Override /* Writer */
	public void close() throws IOException {
		out.close();
	}
}
