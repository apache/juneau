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

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;

/**
 * Specialized writer for serializing CSV.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>
 * 		This class is not intended for external use.
 * </ul>
 */
@SuppressWarnings("resource")
public class CsvWriter extends SerializerWriter {

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

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(char c) {
		super.append(c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(char[] value) {
		super.append(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(int indent, char c) {
		super.append(indent, c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(int indent, String text) {
		super.append(indent, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(Object text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter append(String text) {
		super.append(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter appendIf(boolean b, char c) {
		super.appendIf(b, c);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter appendIf(boolean b, String text) {
		super.appendIf(b, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter appendln(int indent, String text) {
		super.appendln(indent, text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter appendln(String text) {
		super.appendln(text);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter appendUri(Object value) {
		super.appendUri(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter cr(int depth) {
		super.cr(depth);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter cre(int depth) {
		super.cre(depth);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter i(int indent) {
		super.i(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter ie(int indent) {
		super.ie(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter nl(int indent) {
		super.nl(indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter nlIf(boolean flag, int indent) {
		super.nlIf(flag, indent);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter q() {
		super.q();
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter s() {
		super.s();
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter sIf(boolean flag) {
		super.sIf(flag);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter w(char value) {
		super.w(value);
		return this;
	}

	@Override /* Overridden from SerializerWriter */
	public CsvWriter w(String value) {
		super.w(value);
		return this;
	}

	/**
	 * Writes an entry to the writer.
	 *
	 * @param value The value to write.
	 */
	public void writeEntry(Object value) {
		if (value == null)
			w("null");
		else {
			var s = value.toString();
			var mustQuote = false;
			for (int i = 0; i < s.length() && ! mustQuote; i++) {
				var c = s.charAt(i);
				if (Character.isWhitespace(c) || c == ',')
					mustQuote = true;
			}
			if (mustQuote)
				w('"').w(s).w('"');
			else
				w(s);
		}
	}
}