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
package org.apache.juneau.commons.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/** Tests for {@link Destinations}. */
class Destinations_Test extends TestBase {

	private static String r(Destination d, String t, Map<String,?> scope) {
		var tr = TemplateResolver.create().destination(d).build();
		return tr.render(tr.parse(t), scope);
	}

	@Test void a01_htmlEscapesValuesNotTemplate() {
		assertEquals("<p>&lt;b&gt; &amp; &quot;q&quot;</p>", r(Destinations.HTML, "<p>${a}</p>", Map.of("a", "<b> & \"q\"")));
	}

	@Test void a02_htmlList() {
		assertEquals("<ul><li>&lt;a&gt;</li><li>b</li></ul>", r(Destinations.HTML, "#{ul(xs)}", Map.of("xs", List.of("<a>", "b"))));
	}

	@Test void a03_htmlTable() {
		var rows = List.of(Map.of("id", "<1>", "n", "x"));
		assertEquals("<table><thead><tr><th>ID</th><th>N</th></tr></thead><tbody><tr><td>&lt;1&gt;</td><td>x</td></tr></tbody></table>",
			r(Destinations.HTML, "#{table(list('ID','N'), rows.![list(id, n)])}", Map.of("rows", rows)));
	}

	@Test void a04_markdownEscapesAndFlattens() {
		assertEquals("\\*a\\* \\[b\\]\\(c\\)", r(Destinations.MARKDOWN, "${a}", Map.of("a", "*a*\n[b](c)")));
	}

	@Test void a05_markdownLeadingBlockMarkers() {
		assertEquals("\\# h", r(Destinations.MARKDOWN, "${a}", Map.of("a", "# h")));
		assertEquals("1\\. x", r(Destinations.MARKDOWN, "${a}", Map.of("a", "1. x")));
		assertEquals("1\\) x", r(Destinations.MARKDOWN, "${a}", Map.of("a", "1) x")));
		assertEquals("\\- x", r(Destinations.MARKDOWN, "${a}", Map.of("a", "- x")));
	}

	@Test void a06_markdownPipeEscapedInTable() {
		var out = r(Destinations.MARKDOWN, "#{table(list('A'), list(list(v)))}", Map.of("v", "a|b"));
		assertEquals("| A |\n| --- |\n| a\\|b |", out);
	}

	@Test void a07_plainTextDropsControlChars() {
		assertEquals("ab\nc", r(Destinations.PLAIN_TEXT, "${a}", Map.of("a", "a\u0000b\nc\u0007")));
	}

	@Test void a08_plainTextListAndTable() {
		assertEquals("- a\n- b", r(Destinations.PLAIN_TEXT, "#{ul(xs)}", Map.of("xs", List.of("a", "b"))));
		assertEquals("1. a\n2. b", r(Destinations.PLAIN_TEXT, "#{ol(xs)}", Map.of("xs", List.of("a", "b"))));
	}

	@Test void a09_jsonString() {
		assertEquals("a\\\"b\\\\c\\nd\\u0001", r(Destinations.JSON_STRING, "${a}", Map.of("a", "a\"b\\c\nd\u0001")));
	}

	@Test void a10_urlComponent() {
		assertEquals("a%20b%26c%2Fd%2B", r(Destinations.URL_COMPONENT, "${a}", Map.of("a", "a b&c/d+")));
	}

	@Test void a11_templateTextUntouched() {
		assertEquals("a&b <i>", r(Destinations.HTML, "a&b <i>", Map.of()));
	}

	@Test void a12_markdownCorpusNeverStartsBlockOrInjectsLinks() {
		var corpus = List.of("# h", "> q", "+ x", "= x", "1) x", "    code", "```", "<script>", "[a](http://x)", "https://evil.example/x", "{.raw}", "!img", "{javascript}", "a\n\n# b", "***", "~~x~~", "&amp;");
		for (var s : corpus) {
			var out = r(Destinations.MARKDOWN, "${a}", Map.of("a", s));
			assertFalse(out.contains("\n"), out);
			assertFalse(out.matches("^\\s*[#>+=].*"), out);
			assertFalse(out.matches(".*(?<!\\\\)[\\[\\]<>`*~{}!].*"), out);
			assertFalse(out.matches("(?s).*(?<!\\\\)://.*"), out);
		}
	}

	private static final List<Destination> ALL = List.of(Destinations.HTML, Destinations.MARKDOWN, Destinations.PLAIN_TEXT, Destinations.JSON_STRING, Destinations.URL_COMPONENT);

	@Test void b01_dataIsNeverReparsedOnAnyPreset() {
		for (var d : ALL) {
			assertFalse(r(d, "${a}", Map.of("a", "${S:user.home}")).contains(System.getProperty("user.home")), d.name());
			assertFalse(r(d, "${a}", Map.of("a", "#{1+1}")).contains("2") && ! r(d, "${a}", Map.of("a", "#{1+1}")).contains("1"), d.name());
		}
		assertEquals("#{1+1}", r(Destinations.PLAIN_TEXT, "${a}", Map.of("a", "#{1+1}")));
		assertEquals("${x}", r(Destinations.PLAIN_TEXT, "${a}", Map.of("a", "${x}")));
	}

	@Test void b02_htmlQuoteAndTagBreakout() {
		assertEquals("&#39;&quot;&lt;/td&gt;&lt;script&gt;", r(Destinations.HTML, "${a}", Map.of("a", "'\"</td><script>")));
	}

	@Test void b03_htmlNestedStructure() {
		var inner = "#{ul(ul('<a>'))}";
		assertEquals("<ul><li><ul><li>&lt;a&gt;</li></ul></li></ul>", r(Destinations.HTML, inner, Map.of()));
	}

	@Test void b04_markdownNestedStructureAndPipeNewline() {
		assertEquals("- - a\\|b c", r(Destinations.MARKDOWN, "#{ul(ul(v))}", Map.of("v", "a|b\nc")));
	}

	@Test void b05_markdownJavascriptLinkInert() {
		var out = r(Destinations.MARKDOWN, "${a}", Map.of("a", "[x](javascript:alert(1))"));
		assertEquals("\\[x\\]\\(javascript:alert\\(1\\)\\)", out);
	}

	@Test void b06_jsonSeparators() {
		assertEquals("a\\u2028b\\u2029c", r(Destinations.JSON_STRING, "${a}", Map.of("a", "a\u2028b\u2029c")));
		assertEquals("it's", r(Destinations.JSON_STRING, "${a}", Map.of("a", "it's")));
	}

	@Test void b07_customDestination() {
		var d = new Destination("upper", String::toUpperCase, (s, t) -> "STRUCT");
		assertEquals("A-STRUCT", r(d, "${a}-#{ul(xs)}", Map.of("a", "a", "xs", List.of(1))));
	}

	@Test void b08_destinationRejectsNulls() {
		assertThrows(NullPointerException.class, () -> new Destination(null, s -> s, (s, t) -> ""));
		assertThrows(NullPointerException.class, () -> new Destination("x", null, (s, t) -> ""));
		assertThrows(NullPointerException.class, () -> new Destination("x", s -> s, null));
	}

	@Test void b09_tableArityMismatchThrows() {
		assertThrows(RuntimeException.class, () -> r(Destinations.HTML, "#{table(list('A','B'), list(list(1)))}", Map.of()));
	}

	@Test void b10_deniedFunctionsDoNotRunOnAnyPreset() {
		for (var d : ALL) {
			var tr = TemplateResolver.create().destination(d).build();
			for (var f : List.of("jsonPath('x','y')", "get('x','y')", "match('a','a')", "replaceRegex('a','a','b')", "htmlUnescape('&lt;')", "repeat('a', 3)")) {
				var t = "#{" + f + "}";
				var out = tr.render(tr.parse(t), Map.of());
				assertFalse(out.contains("<") || out.equals("b") || out.equals("aaa"), d.name() + " " + f + " -> " + out);
			}
		}
	}

	@Test void b11_urlComponentEncodesStructureItems() {
		assertEquals("a%2Fb", r(Destinations.URL_COMPONENT, "#{ul(list('a/b'))}", Map.of()).replace("-%20", ""));
	}
}
