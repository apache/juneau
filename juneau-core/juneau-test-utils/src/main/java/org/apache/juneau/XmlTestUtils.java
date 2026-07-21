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
package org.apache.juneau;

import static org.apache.juneau.commons.utils.IoUtils.*;

import java.util.regex.*;

/**
 * Shared test-support utilities for validating XML output.
 */
public final class XmlTestUtils {

	// Whitespace-validation patterns.  The lookbehind on the trailing '>' distinguishes start tags ('(?<!/)')
	// from self-closing tags ('(?<=/)') in a single linear pass, avoiding the super-linear backtracking of the
	// previous '.*'/nested-quantifier forms (java:S8786).  Hoisted to static final fields since they're
	// stateless and otherwise get recompiled on every checkXmlWhitespace() call/recursion.
	private static final Pattern START_TAG = Pattern.compile("^(\\s*)<[^/>][^>]*(?<!/)>$");
	private static final Pattern END_TAG = Pattern.compile("^(\\s*)</[^>]+>$");
	private static final Pattern COMBINED_TAG = Pattern.compile("^(\\s*)<[^/>][^>]*(?<=/)>$");
	private static final Pattern CONTENT_ONLY = Pattern.compile("^(\\s*)[^\\s\\<]+$");

	private XmlTestUtils() {}

	/**
	 * Validates that the whitespace is correct in the specified XML.
	 *
	 * @param out The XML to validate.
	 * @throws Exception If the whitespace/indentation in the XML is incorrect.
	 */
	@SuppressWarnings({
		"java:S112"  // Generic exception throw required; checked exception wrapping would obscure test intent.
	})
	public static void checkXmlWhitespace(String out) throws Exception {
		if (out.indexOf('\u0000') != -1) {
			for (var s : out.split("\u0000"))
				checkXmlWhitespace(s);
			return;
		}

		var lines = out.split("\n");
		var indent = -1;
		try {
			for (var i = 0; i < lines.length; i++)
				indent = checkLineIndent(lines[i], i + 1, indent);
			if (indent != -1)
				throw new Exception("Possible unmatched tag.  indent=''" + indent + "''");
		} catch (Exception e) {
			printLines(lines);
			throw e;
		}
	}

	/**
	 * Validates the indentation of a single line of XML and returns the indent depth after processing it.
	 *
	 * @param line The line to validate.
	 * @param lineNum The 1-based line number (for error messages).
	 * @param indent The indent depth before processing this line.
	 * @return The indent depth after processing this line.
	 * @throws Exception If the line's indentation is wrong, or the line doesn't match any recognized form.
	 */
	private static int checkLineIndent(String line, int lineNum, int indent) throws Exception {
		var m = START_TAG.matcher(line);
		if (m.matches()) {
			checkIndent(indent + 1, m.group(1).length(), lineNum, "start tag");
			return indent + 1;
		}

		m = END_TAG.matcher(line);
		if (m.matches()) {
			checkIndent(indent, m.group(1).length(), lineNum, "end tag");
			return indent - 1;
		}

		m = COMBINED_TAG.matcher(line);
		if (m.matches()) {
			checkIndent(indent + 1, m.group(1).length(), lineNum, "combined tag");
			return indent;
		}

		m = CONTENT_ONLY.matcher(line);
		if (m.matches()) {
			checkIndent(indent + 1, m.group(1).length(), lineNum, "content-only");
			return indent;
		}

		var twc = tagWithContentIndent(line);
		if (twc != -1) {
			checkIndent(indent + 1, twc, lineNum, "tag-with-content");
			return indent;
		}

		throw new Exception("Unmatched whitespace line at line number ''" + lineNum + "''");
	}

	/**
	 * Verifies that an observed indent width matches the expected one, throwing a descriptive exception otherwise.
	 *
	 * @param expected The expected indent width.
	 * @param actual The observed indent width.
	 * @param lineNum The 1-based line number (for error messages).
	 * @param lineType A human-readable description of the line kind being checked (for error messages).
	 * @throws Exception If <c>actual != expected</c>.
	 */
	private static void checkIndent(int expected, int actual, int lineNum, String lineType) throws Exception {
		if (actual != expected)
			throw new Exception("Wrong indentation detected on " + lineType + " line ''" + lineNum + "''");
	}

	/**
	 * Returns the leading-whitespace indent width of a line that is a single tag-with-content line.
	 *
	 * <p>
	 * A tag-with-content line is one that — after any optional leading whitespace — starts with an opening tag and
	 * ends with a closing tag, such as <c>&lt;foo&gt;bar&lt;/foo&gt;</c>.  This is a linear-time replacement for the
	 * <c>^(\s*)&lt;[^&gt;]+&gt;.*&lt;/[^&gt;]+&gt;$</c> regular expression that avoids its super-linear backtracking.
	 *
	 * @param line The line to test.
	 * @return
	 * 	The number of leading whitespace characters on the line, or <c>-1</c> if the line is not a single
	 * 	tag-with-content line.
	 */
	private static int tagWithContentIndent(String line) {
		var n = line.length();
		var w = 0;
		while (w < n) {
			var c = line.charAt(w);
			if (c != ' ' && c != '\t' && c != '\n' && c != '\u000B' && c != '\f' && c != '\r')
				break;
			w++;
		}
		if (n - w < 2 || line.charAt(w) != '<' || line.charAt(n - 1) != '>')
			return -1;
		var open = line.indexOf('>', w);          // End of the opening tag.
		if (open - w < 2)
			return -1;
		var close = line.lastIndexOf("</");        // Start of the final closing tag.
		if (close <= open || close + 3 > n - 1)
			return -1;
		for (var j = close + 2; j < n - 1; j++)    // Closing tag name must not contain '>'.
			if (line.charAt(j) == '>')
				return -1;
		return w;
	}
}
