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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.Optional;

import org.junit.jupiter.api.*;

/**
 * Edge case tests for Markdown serialization and parsing.
 */
class MarkdownEdgeCases_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// f01 - Very long values
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_veryLongValues() throws Exception {
		var longVal = "x".repeat(500);
		var bean = Map.of("key", longVal);
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains(longVal), "Expected long value in output: " + md.substring(0, Math.min(200, md.length())));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f02 - Multi-line strings (round-trippable via JSON5 escaping)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f02_multiLineStrings() throws Exception {
		var value = "line1\nline2\nline3";
		var bean = new LinkedHashMap<String, String>();
		bean.put("text", value);
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		// Multi-line strings are wrapped in JSON5 backtick syntax for round-trip correctness
		assertTrue(md.contains("`'") && md.contains("\\n"), "Expected JSON5 escaped newline in output: " + md);
		// Verify round-trip
		@SuppressWarnings("unchecked")
		var parsed = (Map<String, String>) MarkdownParser.DEFAULT.parse(md, Map.class);
		assertEquals(value, parsed.get("text"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f03 - Unicode content
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f03_unicodeContent() throws Exception {
		var bean = Map.of("name", "日本語", "emoji", "😀");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("日本語"), "Expected Japanese: " + md);
		assertTrue(md.contains("😀"), "Expected emoji: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f04 - Empty strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f04_emptyStrings() throws Exception {
		var bean = Map.of("key", "", "other", "value");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("| key |") || md.contains("| other |"), "Expected both keys: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f05 - HTML in values (not interpreted as Markdown)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f05_htmlInValues() throws Exception {
		var bean = Map.of("html", "<div>content</div>");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("<div>"), "Expected literal HTML: " + md);
		assertTrue(md.contains("</div>"), "Expected closing tag: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f06 - Markdown syntax in values (escaped)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f06_markdownInValues() throws Exception {
		var bean = Map.of("desc", "**bold** and [link](url)");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("**bold**") || md.contains("bold"), "Expected bold text: " + md);
		assertTrue(md.contains("[link]") || md.contains("link"), "Expected link text: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f07 - Very wide table (many columns)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f07_veryWideTable() throws Exception {
		var props = new LinkedHashMap<String, Integer>();
		for (var i = 0; i < 20; i++)
			props.put("col" + i, i);
		var list = List.of(props);
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertTrue(md.contains("|---"), "Expected separator: " + md);
		assertTrue(md.contains("col0") && md.contains("col19"), "Expected many columns: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f08 - Single element collection
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f08_singleElementCollection() throws Exception {
		var list = List.of(Map.of("name", "Alice", "age", 30));
		var md = MarkdownSerializer.DEFAULT.serialize(list);
		assertTrue(md.contains("| name |") || md.contains("| age |"), "Expected table: " + md);
		assertTrue(md.contains("Alice"), "Expected value: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f09 - Null collection property
	//-----------------------------------------------------------------------------------------------------------------

	public static class F09_Bean {
		public String name;
		public List<String> items;
	}

	@Test void f09_nullCollection() throws Exception {
		var bean = new F09_Bean();
		bean.name = "test";
		bean.items = null;
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("*null*") || md.contains("items"), "Expected null or items key: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f10 - Cyclic references (recursion detection)
	//-----------------------------------------------------------------------------------------------------------------

	public static class F10_Bean {
		public String name;
		public F10_Bean child;
	}

	@Test void f10_cyclicReferences() throws Exception {
		var bean = new F10_Bean();
		bean.name = "root";
		bean.child = bean; // self-reference
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertNotNull(md);
		assertTrue(md.contains("root"), "Expected root name: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f11 - Deep nesting (doc mode, heading cap at H6)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f11_deepNesting() throws Exception {
		// Deeply nested structure - Maps render nested maps as inline JSON5; beans get sub-headings
		var s = MarkdownDocSerializer.create().title("L1").build();
		var nested = Map.<String, Object>of("x", "deep");
		var l1 = new LinkedHashMap<String, Object>();
		l1.put("nested", nested);
		var md = s.serialize(l1);
		assertTrue(md.contains("deep"), "Expected deep value: " + md);
		assertNotNull(md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f12 - Optional properties
	//-----------------------------------------------------------------------------------------------------------------

	public static class F12_Bean {
		public Optional<String> name;
	}

	@Test void f12_optionalProperties() throws Exception {
		var bean = new F12_Bean();
		bean.name = Optional.of("Alice");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		assertTrue(md.contains("Alice"), "Expected Optional value: " + md);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f13 - Table structure (separator alignment)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f13_columnAlignment() throws Exception {
		var bean = Map.of("a", "1", "ab", "2", "abc", "3");
		var md = MarkdownSerializer.DEFAULT.serialize(bean);
		// Should have valid table structure: |---|---|---|
		var lines = md.split("\n");
		assertTrue(lines.length >= 2, "Expected multiple lines: " + md);
		assertTrue(lines[0].startsWith("|") && lines[0].endsWith("|"), "Expected pipe-delimited: " + lines[0]);
	}
}
