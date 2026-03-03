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
package org.apache.juneau.markdown;

import java.io.*;

import org.apache.juneau.serializer.*;

/**
 * Low-level Markdown writer providing helper methods for producing Markdown output.
 *
 * <p>
 * Extends {@link SerializerWriter} to provide Markdown-specific output helpers for headings,
 * tables, lists, and inline formatting.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='warn'>This class is not thread safe and is typically discarded after one use.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S110",
	"resource", // Writer lifecycle managed by caller
})
public class MarkdownWriter extends SerializerWriter {

	/**
	 * Constructor.
	 *
	 * @param out The underlying writer.
	 * @param useWhitespace Whether to use whitespace formatting.
	 * @param maxIndent The maximum indentation level.
	 * @param trimStrings Whether to trim strings.
	 * @param uriResolver The URI resolver for resolving relative URIs.
	 */
	public MarkdownWriter(Writer out, boolean useWhitespace, int maxIndent, boolean trimStrings, org.apache.juneau.UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, '\'', uriResolver);
	}

	/**
	 * Writes a Markdown heading at the specified level.
	 *
	 * @param level The heading level (1-6).
	 * @param text The heading text.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter heading(int level, String text) throws IOException {
		var l = Math.min(6, Math.max(1, level));
		for (var i = 0; i < l; i++)
			append('#');
		append(' ');
		append(escapeText(text));
		append('\n');
		return this;
	}

	/**
	 * Writes a Markdown table header row.
	 *
	 * @param columns The column names.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter tableHeader(String... columns) throws IOException {
		append('|');
		for (var col : columns) {
			append(' ');
			append(escapeCell(col));
			append(" |");
		}
		append('\n');
		return this;
	}

	/**
	 * Writes a Markdown table separator row.
	 *
	 * @param numColumns The number of columns.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter tableSeparator(int numColumns) throws IOException {
		append('|');
		for (var i = 0; i < numColumns; i++)
			append("---|");
		append('\n');
		return this;
	}

	/**
	 * Writes a Markdown table data row.
	 *
	 * @param values The cell values.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter tableRow(String... values) throws IOException {
		append('|');
		for (var val : values) {
			append(' ');
			append(val == null ? "" : val);
			append(" |");
		}
		append('\n');
		return this;
	}

	/**
	 * Writes a Markdown bulleted list item.
	 *
	 * @param depth The indentation depth (0 = no indent).
	 * @param text The item text.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter bulletItem(int depth, String text) throws IOException {
		for (var i = 0; i < depth; i++)
			append("  ");
		append("- ");
		append(text == null ? "" : text);
		append('\n');
		return this;
	}

	/**
	 * Writes a Markdown horizontal rule.
	 *
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter horizontalRule() throws IOException {
		append("---");
		append('\n');
		return this;
	}

	/**
	 * Writes a blank line.
	 *
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter blankLine() throws IOException {
		append('\n');
		return this;
	}

	/**
	 * Writes plain text with Markdown escaping.
	 *
	 * @param text The text to write.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter text(String text) throws IOException {
		append(escapeText(text));
		return this;
	}

	/**
	 * Writes text wrapped in backticks (inline code).
	 *
	 * @param text The text to render as code.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter code(String text) throws IOException {
		append('`');
		append(text == null ? "" : text);
		append('`');
		return this;
	}

	/**
	 * Writes bold text.
	 *
	 * @param text The text to render bold.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter bold(String text) throws IOException {
		append("**");
		append(escapeText(text));
		append("**");
		return this;
	}

	/**
	 * Writes italic text.
	 *
	 * @param text The text to render italic.
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public MarkdownWriter italic(String text) throws IOException {
		append('*');
		append(escapeText(text));
		append('*');
		return this;
	}

	/**
	 * Escapes a string for use inside a Markdown table cell.
	 *
	 * <p>
	 * Escapes pipe characters and backslashes. Replaces newlines with {@code <br>}.
	 *
	 * @param text The raw text.
	 * @return The escaped text suitable for a table cell.
	 */
	public static String escapeCell(String text) {
		if (text == null)
			return "";
		var sb = new StringBuilder(text.length() + 4);
		for (var i = 0; i < text.length(); i++) {
			var c = text.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '|' -> sb.append("\\|");
				case '\n' -> sb.append("<br>");
				case '\r' -> { /* skip carriage return */ }
				default -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Escapes a string for use in general Markdown text.
	 *
	 * <p>
	 * Escapes backslashes and leading Markdown special characters.
	 *
	 * @param text The raw text.
	 * @return The escaped text.
	 */
	public static String escapeText(String text) {
		if (text == null)
			return "";
		var sb = new StringBuilder(text.length() + 4);
		for (var i = 0; i < text.length(); i++) {
			var c = text.charAt(i);
			if (c == '\\') {
				sb.append("\\\\");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
