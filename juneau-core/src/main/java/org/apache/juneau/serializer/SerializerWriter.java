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

import org.apache.juneau.internal.*;

/**
 * Simple wrapper around a standard {@link Writer} with additional methods.
 * <p>
 * Modeled after the Java ProcessBuilder class so that you can chain commands to reduce
 * 	the need for string concatenation for performance reasons.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	writer.append(<js>"foo"</js>).nl().i(5).append(<js>"bar"</js>);
 * </p>
 */
public class SerializerWriter extends Writer {

	/** The underlying writer. */
	protected final Writer out;

	/** Use-indentation flag. */
	protected final boolean useIndentation;

	/** Use-whitespace flag. */
	protected final boolean useWhitespace;

	/** Use-whitespace flag. */
	protected final boolean trimStrings;

	/** The quote character being used by this writer. */
	protected final char quoteChar;

	/** The base (e.g. <js>https://localhost:9443/contextPath"</js>) for relative URIs (e.g. <js>"my/path"</js>). */
	protected final String relativeUriBase;

	/** The base (e.g. <js>https://localhost:9443"</js>) for relative URIs with absolute paths (e.g. <js>"/contextPath/my/path"</js>). */
	protected final String absolutePathUriBase;

	/**
	 * @param out The writer being wrapped.
	 * @param useIndentation If <jk>true</jk>, calling {@link #cr(int)} will create an indentation.
	 * @param useWhitespace If <jk>true</jk>, calling {@link #s()} will write a space character.
	 * @param trimStrings If <jk>true</jk>, strings should be trimmed before they're serialized.
	 * @param quoteChar The character to write when {@link #q()} is called.
	 * @param relativeUriBase The base (e.g. <js>https://localhost:9443/contextPath"</js>) for relative URIs (e.g. <js>"my/path"</js>).
	 * @param absolutePathUriBase The base (e.g. <js>https://localhost:9443"</js>) for relative URIs with absolute paths (e.g. <js>"/contextPath/my/path"</js>).
	 */
	public SerializerWriter(Writer out, boolean useIndentation, boolean useWhitespace, boolean trimStrings, char quoteChar, String relativeUriBase, String absolutePathUriBase) {
		this.out = out;
		this.useIndentation = useIndentation;
		this.useWhitespace = useWhitespace;
		this.trimStrings = trimStrings;
		this.quoteChar = quoteChar;
		this.relativeUriBase = relativeUriBase;
		this.absolutePathUriBase = absolutePathUriBase;
	}

	/**
	 * Performs a carriage return.
	 * <p>
	 * 	Adds a newline and the specified number of tabs (if the {@code useIndentation} setting is enabled) to the output.
	 *
	 * @param depth The indentation.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter cr(int depth) throws IOException {
		if (useIndentation)
			return nl().i(depth);
		return this;
	}

	/**
	 * Writes an indent (if the {@code useIndentation} setting is enabled), followed by text,
	 * 	followed by a newline (if the {@code useIndentation} setting is enabled).
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
	 * Writes the specified text followed by a newline (if the {@code useIndentation} setting is enabled).
	 *
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object.
	 */
	public SerializerWriter appendln(String text) throws IOException {
		return append(0, true, text);
	}

	/**
	 * Writes an indent (if the {@code useIndentation} setting is enabled), followed by text.
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
	 * Writes an indent (if the {@code useIndentation} setting is enabled), followed by text.
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
	 * Writes an indent (if the {@code useIndentation} setting is enabled), followed by text,
	 * 	optionally followed by a newline (if the {@code useIndentation} setting is enabled).
	 *
	 * @param indent The number of tabs to indent.
	 * @param newline If <jk>true</jk>, then a newline is written.
	 * @param text The text to write.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	private SerializerWriter append(int indent, boolean newline, String text) throws IOException {
		i(indent);
		out.write(text);
		if (newline)
			nl();
		return this;
	}

	/**
	 * Appends the specified object as a URI.
	 * <p>
	 * Object is converted to a <code>String</code> using <code>toString()</code>, so this will work on {@link URL} or {@link URI} objects,
	 * or any other type that returns a URI via it's <code>toString()</code> method.
	 * <p>
	 * If the URI is relative (i.e. without a schema and not prepended with <js>'/'</js>) the URI
	 * will be prepended with {@link #absolutePathUriBase} and {@link #relativeUriBase}.
	 * <p>
	 * If the URI is context-absolute (i.e. without a schema, but prepended with <js>'/'</js>)
	 * the URI will be prepended with {@link #absolutePathUriBase}.
	 *
	 * @param uri The URI to serialize.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter appendUri(Object uri) throws IOException {
		String s = uri.toString();
		if (s.indexOf("://") == -1) {
			if (StringUtils.startsWith(s, '/')) {
				if (absolutePathUriBase != null)
					append(absolutePathUriBase);
			} else {
				if (relativeUriBase != null) {
					append(relativeUriBase);
					if (! relativeUriBase.equals("/"))
						append("/");

				}
			}
		}
		return append(s);
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
	 * Writes an indent to the writer if the {@code useIndentation} setting is enabled.
	 *
	 * @param indent The number of tabs to indent.
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter i(int indent) throws IOException {
		if (useIndentation)
			for (int i = 0; i < indent; i++)
				out.write('\t');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useIndentation} setting is enabled.
	 *
	 * @throws IOException If a problem occurred trying to write to the writer.
	 * @return This object (for method chaining).
	 */
	public SerializerWriter nl() throws IOException {
		if (useIndentation)
			out.write('\n');
		return this;
	}

	/**
	 * Writes a newline to the writer if the {@code useIndentation} setting is enabled and the boolean flag is true.
	 *
	 * @param b The boolean flag.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to write to the writer.
	 */
	public SerializerWriter nlIf(boolean b) throws IOException {
		if (b && useIndentation)
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


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

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
