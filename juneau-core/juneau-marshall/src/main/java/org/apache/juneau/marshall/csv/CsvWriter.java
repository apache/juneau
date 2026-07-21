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
package org.apache.juneau.marshall.csv;

import java.io.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;

/**
 * Specialized writer for serializing CSV.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 */
@SuppressWarnings({
	"resource",  // Writer resource managed by calling code
	"java:S3776" // Cognitive complexity acceptable for CSV cell quoting/escaping logic
})
public class CsvWriter extends SerializerWriter<CsvWriter> {

	/**
	 * Constructor.
	 *
	 * @param out The writer being wrapped.
	 * @param useWhitespace If <jk>true</jk>, tabs and spaces will be used in output.
	 * @param maxIndent The maximum indentation level.
	 * @param quoteChar The quote character to use (i.e. <js>'\''</js> or <js>'"'</js>)
	 * @param trimStrings If <jk>true</jk>, strings will be trimmed before being serialized.
	 * @param uriResolver The URI resolver for resolving URIs to absolute or root-relative form.
	 */
	protected CsvWriter(Writer out, boolean useWhitespace, int maxIndent, char quoteChar, boolean trimStrings, UriResolver uriResolver) {
		super(out, useWhitespace, maxIndent, trimStrings, quoteChar, uriResolver);
	}

	/**
	 * Writes an entry to the writer.
	 *
	 * <p>
	 * Follows RFC 4180 quoting rules:
	 * <ul>
	 *   <li>Values containing commas, double-quote characters, or newlines are enclosed in double quotes.
	 *   <li>Double-quote characters within a quoted field are escaped by preceding them with another double-quote.
	 *   <li>The literal string {@code null} is written unquoted; the string value {@code "null"} is quoted.
	 * </ul>
	 *
	 * @param value The value to write.
	 * 	<br>Can be <jk>null</jk> (written as the literal <js>"null"</js>).
	 */
	public void writeEntry(Object value) {
		if (value == null) {
			w("null");
		} else {
			var s = value.toString();
			var mustQuote = false;
			for (var i = 0; i < s.length() && !mustQuote; i++) {
				var c = s.charAt(i);
				if (c == ',' || c == '"' || c == '\r' || c == '\n')
					mustQuote = true;
			}
			if (mustQuote) {
				w('"');
				for (var i = 0; i < s.length(); i++) {
					var c = s.charAt(i);
					if (c == '"')
						w('"');  // RFC 4180: escape embedded quote by doubling it
					w(c);
				}
				w('"');
			} else {
				w(s);
			}
		}
	}
}