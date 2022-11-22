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
import org.apache.juneau.internal.*;

/**
 * Simple wrapper around a standard {@link Writer} with additional methods.
 *
 * <p>
 * Modeled after the Java ProcessBuilder class so that you can chain commands to reduce the need for string
 * concatenation for performance reasons.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>writer</jv>.append(<js>"foo"</js>).nl().i(5).append(<js>"bar"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.SerializersAndParsers">Serializers and Parsers</a> * </ul>
 */
@FluentSetters
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
	public SerializerWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, char quoteChar, UriResolver uriResolver) {
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
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter cr(int depth) {
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
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter cre(int depth) {
		if (useWhitespace && depth <= maxIndent-1)
			return nl(depth).i(depth);
		return this;
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text, followed by a newline
	 * (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param indent The number of tabs to indent.
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter appendln(int indent, String value) {
		return append(indent, true, value);
	}

	/**
	 * Writes the specified text followed by a newline (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter appendln(String value) {
		return append(0, true, value);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text.
	 *
	 * @param indent The number of tabs to indent.
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter append(int indent, String value) {
		return append(indent, false, value);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text.
	 *
	 * @param indent The number of tabs to indent.
	 * @param value The character to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter append(int indent, char value) {
		return i(indent).w(value);
	}

	/**
	 * Writes an indent (if the {@code useWhitespace} setting is enabled), followed by text, optionally followed by a
	 * newline (if the {@code useWhitespace} setting is enabled).
	 *
	 * @param indent The number of tabs to indent.
	 * @param newline If <jk>true</jk>, then a newline is written.
	 * @param value The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	private SerializerWriter append(int indent, boolean newline, String value) {

		if (value == null)
			return this;

		// If text contains newlines, we break it up into lines and indent them separately.
		if (value.indexOf('\n') != -1 && useWhitespace && indent <= maxIndent) {
			for (StringTokenizer st = new StringTokenizer(value, "\n"); st.hasMoreTokens();)
				i(indent).w(st.nextToken()).w("\n");
		} else {
			i(indent).w(value);
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
	 * The URI is resolved based on the {@link Serializer.Builder#uriRelativity(UriRelativity)} and
	 * {@link Serializer.Builder#uriResolution(UriResolution)} settings and the {@link UriContext} that's part of the
	 * session.
	 *
	 * @param value The URI to serialize.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter appendUri(Object value) {
		uriResolver.append(this, value);
		return this;
	}

	/**
	 * Appends the specified characters to this writer.
	 *
	 * @param value The characters to append to this writer.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter append(char[] value) {
		for (char c : value)
			w(c);
		return this;
	}

	/**
	 * Adds a whitespace character to the output if the {@code useWhitespace} setting is enabled.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter s() {
		if (useWhitespace)
			w(' ');
		return this;
	}

	/**
	 * Adds the quote character specified by the {@code quoteChar} setting to the output.
	 *
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter q() {
		w(quoteChar);
		return this;
	}

	/**
	 * Writes an indent to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The number of tabs to indent.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter i(int indent) {
		if (useWhitespace && indent <= maxIndent)
			for (int i = 0; i < indent; i++)
				w('\t');
		return this;
	}

	/**
	 * Writes an end-of-line indent to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The number of tabs to indent.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter ie(int indent) {
		if (useWhitespace && indent <= maxIndent-1)
			for (int i = 0; i < indent; i++)
				w('\t');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useWhitespace} setting is enabled.
	 *
	 * @param indent The current indentation level.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter nl(int indent) {
		if (useWhitespace && indent <= maxIndent)
			w('\n');
		return this;
	}

	/**
	 * Writes a space if the boolean expression is <jk>true</jk> and {@code useWhitespace} is false.
	 *
	 * <p>
	 * Intended for cases in XML where text should be separated by either a space or newline.
	 * This ensures the text is separated by a space if whitespace is disabled.
	 *
	 * @param flag The boolean flag.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter sIf(boolean flag) {
		if (flag && ! useWhitespace)
			w(' ');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useWhitespace} setting is enabled and the boolean flag is true.
	 *
	 * @param flag The boolean flag.
	 * @param indent The current indentation level.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter nlIf(boolean flag, int indent) {
		if (flag && useWhitespace && indent <= maxIndent)
			w('\n');
		return this;
	}

	/**
	 * Writes the specified text to the writer if it isn't <jk>null</jk>.
	 *
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter append(Object value) {
		w(value == null ? null : value.toString());
		return this;
	}

	/**
	 * Writes the specified text to the writer if it isn't <jk>null</jk>.
	 *
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter append(String value) {
		if (value != null)
			w(value);
		return this;
	}

	/**
	 * Writes the specified text to the writer if b is true.
	 *
	 * @param flag Boolean flag.
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter appendIf(boolean flag, String value) {
		if (flag)
			w(value);
		return this;
	}

	/**
	 * Writes the specified text to the writer if b is true.
	 *
	 * @param flag Boolean flag.
	 * @param value The text to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter appendIf(boolean flag, char value) {
		if (flag)
			w(value);
		return this;
	}

	/**
	 * Writes the specified character to the writer.
	 *
	 * @param value The character to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter w(char value) {
		try {
			out.write(value);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	/**
	 * Writes the specified string to the writer.
	 *
	 * @param value The string to write.
	 * @return This object.
	 */
	@FluentSetter
	public SerializerWriter w(String value) {
		try {
			out.write(value);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Writer */
	public SerializerWriter append(char c) {
		try {
			out.write(c);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
		return this;
	}

	@Override /* Writer */
	public void write(char[] cbuf, int off, int len) {
		try {
			out.write(cbuf, off, len);
		} catch (IOException e) {
			throw new SerializeException(e);
		}
	}

	@Override /* Writer */
	public void flush() throws IOException {
		out.flush();
	}

	@Override /* Writer */
	public void close() throws IOException {
		out.close();
	}

	// <FluentSetters>

	// </FluentSetters>
}
