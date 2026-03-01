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
package org.apache.juneau.csv;

import java.io.*;
import java.util.*;

import org.apache.juneau.parser.*;

/**
 * Specialized reader for parsing CSV input.
 *
 * <p>
 * Parses CSV data according to RFC 4180 with support for:
 * <ul>
 *   <li>Quoted fields (configurable quote character)
 *   <li>Embedded delimiter characters within quoted fields
 *   <li>Embedded newlines within quoted fields
 *   <li>Embedded quote characters escaped by doubling (RFC 4180 style)
 *   <li>Both CRLF and LF line endings
 *   <li>Optional whitespace trimming
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is not intended for external use.
 * </ul>
 */
@SuppressWarnings({
	"resource" // CsvReader owns ParserReader; caller must close CsvReader
})
public class CsvReader implements Closeable {

	private final ParserReader r;
	private final char delimiter;
	private final char quoteChar;
	private final boolean trimStrings;
	private boolean eof = false;

	/**
	 * Constructor.
	 *
	 * @param r The parser reader to wrap.
	 * @param delimiter The field delimiter character (typically <js>','</js>).
	 * @param quoteChar The quote character (typically <js>'"'</js>).
	 * @param trimStrings If <jk>true</jk>, field values are trimmed of surrounding whitespace.
	 */
	public CsvReader(ParserReader r, char delimiter, char quoteChar, boolean trimStrings) {
		this.r = r;
		this.delimiter = delimiter;
		this.quoteChar = quoteChar;
		this.trimStrings = trimStrings;
	}

	/**
	 * Creates a {@link CsvReader} from a {@link ParserPipe}.
	 *
	 * @param pipe The parser pipe.
	 * @param delimiter The delimiter character.
	 * @param quoteChar The quote character.
	 * @param trimStrings Whether to trim strings.
	 * @return A new {@link CsvReader}, or <jk>null</jk> if the pipe has no input.
	 * @throws IOException Thrown by underlying stream.
	 */
	public static CsvReader from(ParserPipe pipe, char delimiter, char quoteChar, boolean trimStrings) throws IOException {
		var pr = pipe.getParserReader();
		if (pr == null)
			return null;
		return new CsvReader(pr, delimiter, quoteChar, trimStrings);
	}

	/**
	 * Reads a single row of CSV data, returning <jk>null</jk> at end of input.
	 *
	 * <p>
	 * Each call advances past one record (terminated by LF, CRLF, or end-of-file).
	 * Quoted fields may span multiple lines.
	 *
	 * @return A list of field values for the row, or <jk>null</jk> if end of input has been reached.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If the CSV is malformed (e.g. an unclosed quoted field).
	 */
	@SuppressWarnings({
		"java:S1168", // null = end of input (distinct from empty row)
		"java:S3776", // Cognitive complexity acceptable for CSV row parsing state machine
		"java:S6541" // Brain method acceptable for CSV row parsing state machine
	})
	public List<String> readRow() throws IOException, ParseException {
		if (eof)
			return null;

		// Skip blank lines between records
		int c = r.read();
		while (c == '\r' || c == '\n') {
			if (c == '\r') {
				int next = r.read();
				if (next != '\n' && next != -1)
					r.unread();
			}
			c = r.read();
		}

		if (c == -1) {
			eof = true;
			return null;
		}

		r.unread();

		var fields = new ArrayList<String>();
		var field = new StringBuilder();

		while (true) {
			c = r.read();

			if (c == -1) {
				eof = true;
				fields.add(finishField(field));
				return fields;
			}

			if (c == quoteChar) {
				parseQuotedField(field);
				// After a quoted field, read the next char: delimiter, newline, or EOF
				int next = r.read();
				if (next == -1) {
					eof = true;
					fields.add(finishField(field));
					return fields;
				} else if (next == delimiter) {
					fields.add(finishField(field));
					field = new StringBuilder();
				} else if (next == '\r') {
					int peek = r.read();
					if (peek != '\n' && peek != -1)
						r.unread();
					fields.add(finishField(field));
					return fields;
				} else if (next == '\n') {
					fields.add(finishField(field));
					return fields;
				} else {
					// Lenient: treat extra content after closing quote as unquoted
					field.append((char) next);
				}
			} else if (c == delimiter) {
				fields.add(finishField(field));
				field = new StringBuilder();
			} else if (c == '\r') {
				int next = r.read();
				if (next != '\n' && next != -1)
					r.unread();
				fields.add(finishField(field));
				return fields;
			} else if (c == '\n') {
				fields.add(finishField(field));
				return fields;
			} else {
				field.append((char) c);
			}
		}
	}

	/**
	 * Parses the contents of a quoted field.
	 *
	 * <p>
	 * The opening quote character has already been consumed.
	 * Handles RFC 4180 doubled-quote escaping: two consecutive quote characters inside a quoted field
	 * produce a single literal quote character.
	 *
	 * @param field The string builder to append field content to.
	 * @throws IOException Thrown by underlying stream.
	 * @throws ParseException If end of input is reached before the closing quote.
	 */
	private void parseQuotedField(StringBuilder field) throws IOException, ParseException {
		while (true) {
			int c = r.read();
			if (c == -1)
				throw new ParseException("Unterminated quoted field in CSV input.");
			if (c == quoteChar) {
				int next = r.read();
				if (next == quoteChar) {
					// Doubled quote → literal quote
					field.append(quoteChar);
				} else {
					// Closing quote; push back the following character
					if (next != -1)
						r.unread();
					return;
				}
			} else {
				field.append((char) c);
			}
		}
	}

	private String finishField(StringBuilder sb) {
		var s = sb.toString();
		return trimStrings ? s.trim() : s;
	}

	/**
	 * Returns whether the end of input has been reached.
	 *
	 * @return <jk>true</jk> if end of input has been reached.
	 */
	public boolean isEof() {
		return eof;
	}

	@Override
	public void close() throws IOException {
		r.close();
	}
}
