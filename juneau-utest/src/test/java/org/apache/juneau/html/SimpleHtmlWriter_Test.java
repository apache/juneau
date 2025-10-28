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
package org.apache.juneau.html;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for SimpleHtmlWriter fluent setter overrides.
 */
class SimpleHtmlWriter_Test extends TestBase {

	@Test void a01_fluentChaining_tagMethods() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test that fluent methods return SimpleHtmlWriter (not HtmlWriter)
		SimpleHtmlWriter result;

		result = w.sTag("div");
		assertSame(w, result);
		assertInstanceOf(SimpleHtmlWriter.class, result);

		result = w.attr("class", "test");
		assertSame(w, result);

		result = w.cTag();
		assertSame(w, result);

		result = w.text("content");
		assertSame(w, result);

		result = w.eTag("div");
		assertSame(w, result);
	}

	@Test void a02_fluentChaining_appendMethods() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test append methods
		SimpleHtmlWriter result;

		result = w.append("test");
		assertSame(w, result);
		assertInstanceOf(SimpleHtmlWriter.class, result);

		result = w.append('x');
		assertSame(w, result);

		result = w.append((Object)"obj");
		assertSame(w, result);

		result = w.append(chars('a','b','c'));
		assertSame(w, result);

		result = w.appendIf(true, "conditional");
		assertSame(w, result);

		result = w.appendIf(true, 'y');
		assertSame(w, result);
	}

	@Test void a03_fluentChaining_formattingMethods() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test formatting methods
		SimpleHtmlWriter result;

		result = w.cr(0);
		assertSame(w, result);
		assertInstanceOf(SimpleHtmlWriter.class, result);

		result = w.cre(0);
		assertSame(w, result);

		result = w.i(1);
		assertSame(w, result);

		result = w.ie(1);
		assertSame(w, result);

		result = w.nl(1);
		assertSame(w, result);

		result = w.s();
		assertSame(w, result);

		result = w.q();
		assertSame(w, result);

		result = w.sIf(true);
		assertSame(w, result);

		result = w.nlIf(true, 1);
		assertSame(w, result);

		result = w.w('z');
		assertSame(w, result);

		result = w.w("write");
		assertSame(w, result);
	}

	@Test void a04_fluentChaining_complex() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test chaining multiple fluent calls
		SimpleHtmlWriter result = w
			.sTag("table")
			.sTag("tr")
			.sTag("td")
			.append("hello")
			.eTag("td")
			.eTag("tr")
			.eTag("table");

		assertSame(w, result);
		assertInstanceOf(SimpleHtmlWriter.class, result);
	}

	@Test void a05_output_simpleTable() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		String result = w
			.sTag("table")
			.sTag("tr")
			.sTag("td")
			.append("hello")
			.eTag("td")
			.eTag("tr")
			.eTag("table")
			.toString();

		assertTrue(result.contains("<table"));
		assertTrue(result.contains("<tr"));
		assertTrue(result.contains("<td"));
		assertTrue(result.contains("hello"));
		assertTrue(result.contains("</td>"));
		assertTrue(result.contains("</tr>"));
		assertTrue(result.contains("</table>"));
	}

	@Test void a06_output_withAttributes() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		String result = w
			.sTag("div")
			.attr("class", "container")
			.attr("id", "main")
			.cTag()
			.text("content")
			.eTag("div")
			.toString();

		assertTrue(result.contains("class"));
		assertTrue(result.contains("container"));
		assertTrue(result.contains("id"));
		assertTrue(result.contains("main"));
		assertTrue(result.contains("content"));
		assertTrue(result.contains("</div>"));
	}

	@Test void a08_tagVariations() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test different tag method variations
		SimpleHtmlWriter result;

		result = w.oTag("div");
		assertSame(w, result);

		result = w.tag("span");
		assertSame(w, result);

		result = w.sTag(1, "p");
		assertSame(w, result);

		result = w.eTag(1, "p");
		assertSame(w, result);

		result = w.oTag(null, "div");
		assertSame(w, result);

		result = w.sTag(null, "span");
		assertSame(w, result);

		result = w.eTag(null, "span");
		assertSame(w, result);
	}

	@Test void a09_attrVariations() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test different attr method variations
		SimpleHtmlWriter result;

		result = w.attr("id", "test", true);
		assertSame(w, result);

		result = w.attr("class", "test");
		assertSame(w, result);
	}

	@Test void a10_appendVariations() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test different append method variations
		SimpleHtmlWriter result;

		result = w.appendln("line");
		assertSame(w, result);

		result = w.appendln(1, "indented");
		assertSame(w, result);

		result = w.append(1, "indented text");
		assertSame(w, result);

		result = w.append(2, 'c');
		assertSame(w, result);
	}

	@Test void a11_textVariations() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		// Test different text method variations
		SimpleHtmlWriter result;

		result = w.text("normal text");
		assertSame(w, result);

		result = w.text("preserved", true);
		assertSame(w, result);
	}

	@Test void a12_ceTag() {
		SimpleHtmlWriter w = new SimpleHtmlWriter();

		String result = w
			.sTag("br")
			.ceTag()
			.toString();

		assertTrue(result.contains("<br"));
		assertTrue(result.contains("/>"));
	}
}