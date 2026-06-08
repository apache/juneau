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

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link MarkdownParserSession} branches not exercised by the higher-level
 * {@link MarkdownParser_Test} suite (TODO-155 Tier E3).
 */
class MarkdownParserSession_Test extends TestBase {

	//====================================================================================================
	// a - Bullet list marker variants and edge cases
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a01_bulletListAsterisk() throws Exception {
		var md = "* alpha\n* beta\n* gamma";
		var r = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(List.of("alpha", "beta", "gamma"), r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a02_bulletListPlus() throws Exception {
		var md = "+ one\n+ two";
		var r = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(List.of("one", "two"), r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a03_bulletListEmptyMarkers() throws Exception {
		// Bare "-", "*", "+" markers should produce empty-string entries.
		var md = "- a\n-\n- b";
		var r = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("a", r.get(0));
		assertEquals("", r.get(1));
		assertEquals("b", r.get(2));
	}

	@Test void a04_bulletListToArray() throws Exception {
		var md = "* x\n* y";
		var r = MarkdownParser.DEFAULT.parse(md, String[].class);
		assertArrayEquals(new String[]{"x", "y"}, r);
	}

	@Test void a05_bulletListToCustomCollection() throws Exception {
		// Exercise the canCreateNewInstance(outer) branch in parseBulletList for a concrete List type.
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (LinkedList<String>) MarkdownParser.DEFAULT.parse("- a\n- b", LinkedList.class, String.class);
		assertEquals(2, r.size());
		assertEquals("a", r.getFirst());
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a06_bulletListNullValues() throws Exception {
		var md = "- foo\n- *null*\n- bar";
		var r = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("foo", r.get(0));
		assertNull(r.get(1));
		assertEquals("bar", r.get(2));
	}

	//====================================================================================================
	// b - Empty / blank input handling
	//====================================================================================================

	@Test void b01_blankLinesOnly() throws Exception {
		var r = MarkdownParser.DEFAULT.parse("\n   \n\t\n", Map.class);
		assertNull(r);
	}

	@Test void b02_emptyToString() throws Exception {
		// The "" target requires String.class — non-bean, isString().
		var r = MarkdownParser.DEFAULT.parse("", String.class);
		assertEquals("", r);
	}

	@Test void b03_blankToString() throws Exception {
		var r = MarkdownParser.DEFAULT.parse("   \n  ", String.class);
		assertEquals("", r);
	}

	@Test void b04_plainTextValue() throws Exception {
		// First non-blank line not starting with | or list marker → treated as a simple cell value.
		var r = MarkdownParser.DEFAULT.parse("hello", String.class);
		assertEquals("hello", r);
	}

	@Test void b05_plainTextToInteger() throws Exception {
		var r = MarkdownParser.DEFAULT.parse("42", Integer.class);
		assertEquals(42, r);
	}

	@Test void b06_plainNullToBean() throws Exception {
		// "*null*" line treated as plain value parsed to type — non-string type returns null.
		var r = MarkdownParser.DEFAULT.parse("*null*", MarkdownParser_Test.A.class);
		assertNull(r);
	}

	//====================================================================================================
	// c - Table edge cases (empty data rows, separator filtering)
	//====================================================================================================

	@Test void c01_tableNoDataRows() throws Exception {
		// Header + separator only → empty bean (all defaults).
		var md = "| Property | Value |\n|---|---|";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertNull(r.name);
		assertEquals(0, r.age);
	}

	@Test void c02_tableNoTableLines() throws Exception {
		// Only a separator with no |-prefixed lines → null result for tables.
		// Use plain text that does not start with | - Markdown parser should handle it as text.
		var md = "  ---\n";
		var r = MarkdownParser.DEFAULT.parse(md, String.class);
		assertEquals("---", r);
	}

	@Test void c03_keyHeaderVariant() throws Exception {
		// "Key"/"Value" headers should also be detected as a key/value table.
		var md = "| Key | Value |\n|---|---|\n| name | Bob |\n| age | 25 |";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.A.class);
		assertEquals("Bob", r.name);
		assertEquals(25, r.age);
	}

	@Test void c04_tableEmptyHeaderCells() throws Exception {
		// A row with empty headers should produce an empty result.
		var md = "|  |  |\n|---|---|\n| a | b |";
		var r = MarkdownParser.DEFAULT.parse(md, Map.class);
		// With empty headers we still get a map keyed by "".
		assertNotNull(r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c05_multiColumnAllNullRow() throws Exception {
		// A row of all *null*/empty cells should yield a null entry in the list.
		var md = "| name | age |\n|---|---|\n| *null* | *null* |\n| Alice | 30 |";
		var r = (List<MarkdownParser_Test.B>) MarkdownParser.DEFAULT.parse(md, List.class, MarkdownParser_Test.B.class);
		assertEquals(2, r.size());
		assertNull(r.get(0));
		assertEquals("Alice", r.get(1).name);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c06_multiColumnToObject() throws Exception {
		// Multi-column with isObject() target → ArrayList of MarshalledMaps.
		var md = "| name | age |\n|---|---|\n| Alice | 30 |";
		var r = (List<Map<String,Object>>) MarkdownParser.DEFAULT.parse(md, Object.class);
		assertEquals(1, r.size());
		assertEquals("Alice", r.get(0).get("name"));
		assertEquals(30, r.get(0).get("age"));
	}

	@Test void c07_multiColumnSingleBean() throws Exception {
		// Multi-column with a non-list/array bean target → parses just the first row.
		var md = "| name | age |\n|---|---|\n| Alice | 30 |\n| Bob | 25 |";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.B.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void c08_multiColumnSingleBeanNoRows() throws Exception {
		// Single-bean target with no data rows → null.
		var md = "| name | age |\n|---|---|";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.B.class);
		assertNull(r);
	}

	//====================================================================================================
	// d - Cell-value coercion paths (numbers, booleans, plain text)
	//====================================================================================================

	@Test void d01_autoDetectLong() throws Exception {
		var md = "| Property | Value |\n|---|---|\n| big | 9999999999 |";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Object.class);
		assertEquals(9999999999L, r.get("big"));
		assertInstanceOf(Long.class, r.get("big"));
	}

	@Test void d02_autoDetectDouble() throws Exception {
		var md = "| Property | Value |\n|---|---|\n| pi | 3.14 |";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Object.class);
		assertEquals(3.14, r.get("pi"));
		assertInstanceOf(Double.class, r.get("pi"));
	}

	@Test void d03_autoDetectFalse() throws Exception {
		var md = "| Property | Value |\n|---|---|\n| flag | false |";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Object.class);
		assertEquals(Boolean.FALSE, r.get("flag"));
	}

	@Test void d04_emptyCellPreservedForString() throws Exception {
		// Empty cell to a String-typed property → empty string.
		var md = "| name | age |\n|---|---|\n|  | 1 |";
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (List<MarkdownParser_Test.B>) MarkdownParser.DEFAULT.parse(md, List.class, MarkdownParser_Test.B.class);
		assertEquals(1, r.size());
		assertEquals("", r.get(0).name);
	}

	@Test void d05_invalidJson5InBackticks() throws Exception {
		var md = "| Property | Value |\n|---|---|\n| addr | `{this is not json5` |";
		assertThrows(ParseException.class, () -> MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.D.class));
	}

	@Test void d06_invalidConvertCellValue() throws Exception {
		// "abc" cannot convert to int → ParseException.
		var md = "| Property | Value |\n|---|---|\n| age | abc |";
		assertThrows(ParseException.class, () -> MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.A.class));
	}

	//====================================================================================================
	// e - Optional element handling (isOptional branch)
	//====================================================================================================

	@Test void e01_parseOptionalString() throws Exception {
		var md = "hello";
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (Optional<String>) MarkdownParser.DEFAULT.parse(md, Optional.class, String.class);
		assertNotNull(r);
		assertTrue(r.isPresent());
		assertEquals("hello", r.get());
	}

	//====================================================================================================
	// f - Map target with key/value table
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void f01_keyValueToTreeMap() throws Exception {
		// Concrete Map type that canCreateNewInstance — exercises eType.newInstance() branch.
		var md = "| Property | Value |\n|---|---|\n| b | 2 |\n| a | 1 |";
		var r = (TreeMap<String,Integer>) MarkdownParser.DEFAULT.parse(md, TreeMap.class, String.class, Integer.class);
		assertEquals(2, r.size());
		assertEquals(1, r.get("a"));
		assertEquals(2, r.get("b"));
		// TreeMap is sorted — first key should be "a".
		assertEquals("a", r.firstKey());
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void f02_multiColumnToTreeMap() throws Exception {
		// Single-row multi-column → Map element. Exercises parseRow's isMap() branch.
		var md = "| k1 | k2 |\n|---|---|\n| v1 | v2 |";
		var r = (List<Map<String,String>>) MarkdownParser.DEFAULT.parse(md, List.class, TreeMap.class, String.class, String.class);
		assertEquals(1, r.size());
		assertEquals("v1", r.get(0).get("k1"));
		assertEquals("v2", r.get(0).get("k2"));
		assertInstanceOf(TreeMap.class, r.get(0));
	}

	//====================================================================================================
	// g - Pipe and backslash escaping
	//====================================================================================================

	@Test void g01_escapedBackslashInCell() throws Exception {
		// "\\\\" (two backslashes) should unescape to a single backslash.
		var md = "| Property | Value |\n|---|---|\n| path | a\\\\b |";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Map.class);
		assertEquals("a\\b", r.get("path"));
	}

	@Test void g02_unescapedBackslashLeftIntact() throws Exception {
		// A backslash followed by a non-special character is left as-is.
		var md = "| Property | Value |\n|---|---|\n| str | foo\\nbar |";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Map.class);
		assertEquals("foo\\nbar", r.get("str"));
	}

	@Test void g03_pipeAtRowEndEscaped() throws Exception {
		// Trailing "\\|" should remain part of the value rather than being trimmed as a row terminator.
		var md = "| Property | Value |\n|---|---|\n| s | abc\\|";
		var r = (Map<?,?>) MarkdownParser.DEFAULT.parse(md, Map.class);
		assertEquals("abc|", r.get("s"));
	}

	//====================================================================================================
	// h - Bean dictionary / _type column resolution
	//====================================================================================================

	@Marshalled(typeName="ha")
	public static class HA {
		public String name;
		public int age;
	}

	@Marshalled(typeName="hb")
	public static class HB {
		public String name;
		public boolean flag;
	}

	@Test
	void h01_keyValueTypeResolution() throws Exception {
		// _type row in a 2-column key/value table should resolve via bean dictionary.
		var p = MarkdownParser.create().beanDictionary(HA.class, HB.class).build();
		var md = "| Property | Value |\n|---|---|\n| _type | ha |\n| name | Alice |\n| age | 30 |";
		var r = p.parse(md, Object.class);
		assertInstanceOf(HA.class, r);
		assertEquals("Alice", ((HA)r).name);
		assertEquals(30, ((HA)r).age);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void h02_multiColumnTypeColumn() throws Exception {
		// _type column in multi-column table is recognized and skipped from output map keys.
		var p = MarkdownParser.create().beanDictionary(HA.class, HB.class).build();
		var md = "| _type | name | age |\n|---|---|---|\n| ha | Alice | 30 |";
		var r = (List<Map<String,Object>>) p.parse(md, List.class, Object.class);
		assertEquals(1, r.size());
		// _type column should be filtered out of the resulting map keys.
		assertFalse(r.get(0).containsKey("_type"));
		assertEquals("Alice", r.get(0).get("name"));
		assertEquals(30, r.get(0).get("age"));
	}

	//====================================================================================================
	// i - ObjectSwap path (needsJson5Path)
	//====================================================================================================

	public static class UpperCaseSwap extends ObjectSwap<UpperWrapped,String> {
		@Override
		public String swap(MarshallingSession session, UpperWrapped o) { return o == null ? null : o.value; }
		@Override
		public UpperWrapped unswap(MarshallingSession session, String s, ClassMeta<?> hint, String attrName) {
			var w = new UpperWrapped();
			w.value = s == null ? null : s.toUpperCase();
			return w;
		}
	}

	public static class UpperWrapped {
		public String value;
	}

	public static class IBean {
		public String name;
		public UpperWrapped wrapped;
	}

	@Test void i01_keyValueWithSwap() throws Exception {
		// Swap forces the JSON5 path through keyValueTableToJson5/cellToJson5.
		var p = MarkdownParser.create().swaps(UpperCaseSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| wrapped | hello |";
		var r = p.parse(md, IBean.class);
		assertEquals("Alice", r.name);
		assertNotNull(r.wrapped);
		assertEquals("HELLO", r.wrapped.value);
	}

	// Swap targeting Map<String,Object> - exercises top-level keyValueTableToJson5 / needsJson5Path branch.
	public static class IBeanMapSwap extends ObjectSwap<IBeanWithSwap, Map<String,Object>> {
		@Override
		public Map<String,Object> swap(MarshallingSession session, IBeanWithSwap o) {
			var m = new LinkedHashMap<String,Object>();
			if (o != null) { m.put("name", o.name); m.put("count", o.count); }
			return m;
		}
		@Override
		public IBeanWithSwap unswap(MarshallingSession session, Map<String,Object> in, ClassMeta<?> hint, String attrName) {
			var b = new IBeanWithSwap();
			b.name = (String) in.get("name");
			b.count = ((Number) in.get("count")).intValue();
			return b;
		}
	}

	@Swap(IBeanMapSwap.class)
	public static class IBeanWithSwap {
		public String name;
		public int count;
	}

	@Test void i02_topLevelSwapKeyValueTable() throws Exception {
		// Top-level type has a swap → needsJson5Path returns true → keyValueTableToJson5 path is hit.
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| count | 7 |";
		var r = MarkdownParser.DEFAULT.parse(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertEquals(7, r.count);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void i03_topLevelSwapMultiColumnTable() throws Exception {
		// Same swap exercised through parseRow's needsJson5Path branch (rowToJson5).
		var md = "| name | count |\n|---|---|\n| Alice | 3 |\n| Bob | 5 |";
		var r = (List<IBeanWithSwap>) MarkdownParser.DEFAULT.parse(md, List.class, IBeanWithSwap.class);
		assertEquals(2, r.size());
		assertEquals("Alice", r.get(0).name);
		assertEquals(3, r.get(0).count);
		assertEquals("Bob", r.get(1).name);
		assertEquals(5, r.get(1).count);
	}

	@Test void i04_cellToJson5_inlineJson5Unwrapped() throws Exception {
		// Backtick-wrapped cell value embedded into the JSON5 object via cellToJson5.
		// Property value uses inline JSON5; outer type has a swap so the JSON5 path is taken.
		var md = "| Property | Value |\n|---|---|\n| name | `'Alice'` |\n| count | 12 |";
		var r = MarkdownParser.DEFAULT.parse(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertEquals(12, r.count);
	}

	@Test void i05_cellToJson5_nullAndEmpty() throws Exception {
		// "*null*" cell becomes JSON5 null; empty cell becomes "''" via cellToJson5.
		var md = "| Property | Value |\n|---|---|\n| name | *null* |\n| count | 0 |";
		var r = MarkdownParser.DEFAULT.parse(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertNull(r.name);
		assertEquals(0, r.count);
	}

	@Test void i06_cellToJson5_booleanLiteral() throws Exception {
		// Bare "true"/"false" cell in JSON5 path is written without quotes.
		var p = MarkdownParser.create().swaps(IBeanMapSwapBool.class).build();
		var md = "| Property | Value |\n|---|---|\n| flag | true |\n| name | x |";
		var r = p.parse(md, IBeanBool.class);
		assertNotNull(r);
		assertTrue(r.flag);
		assertEquals("x", r.name);
	}

	public static class IBeanMapSwapBool extends ObjectSwap<IBeanBool, Map<String,Object>> {
		@Override
		public Map<String,Object> swap(MarshallingSession session, IBeanBool o) {
			var m = new LinkedHashMap<String,Object>();
			if (o != null) { m.put("flag", o.flag); m.put("name", o.name); }
			return m;
		}
		@Override
		public IBeanBool unswap(MarshallingSession session, Map<String,Object> in, ClassMeta<?> hint, String attrName) {
			var b = new IBeanBool();
			b.flag = (Boolean) in.get("flag");
			b.name = (String) in.get("name");
			return b;
		}
	}

	public static class IBeanBool {
		public boolean flag;
		public String name;
	}

	//====================================================================================================
	// j - Custom null marker propagation through builder
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void j01_customNullInBulletList() throws Exception {
		var p = MarkdownParser.create().nullValue("NIL").build();
		var md = "- a\n- NIL\n- b";
		var r = (List<String>) p.parse(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("a", r.get(0));
		assertNull(r.get(1));
		assertEquals("b", r.get(2));
	}

	//====================================================================================================
	// k - Whitespace / boundary table parsing
	//====================================================================================================

	@Test void k01_separatorWithSpacesAndColons() throws Exception {
		// "| :--- | :---: |" should still be detected as a separator row.
		var md = "| Property | Value |\n| :--- | :---: |\n| name | Alice |";
		var r = MarkdownParser.DEFAULT.parse(md, Map.class);
		assertEquals("Alice", r.get("name"));
	}

	@Test void k02_lineWithoutPipeIgnored() throws Exception {
		// Lines not starting with | should be filtered out of the table-line collection.
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\nthis is not a row\n| age | 30 |";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// l - Multi-column simple element type (parseRow simple type fall-through)
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void l01_multiColumnToStringList() throws Exception {
		// List<String> as multi-column target → parseRow's "simple type" fall-through is hit.
		var md = "| col1 | col2 |\n|---|---|\n| value-a | ignored |\n| value-b | ignored |";
		var r = (List<String>) MarkdownParser.DEFAULT.parse(md, List.class, String.class);
		assertEquals(2, r.size());
		assertEquals("value-a", r.get(0));
		assertEquals("value-b", r.get(1));
	}

	//====================================================================================================
	// m - Inline JSON5 with parse error
	//====================================================================================================

	@Test void m01_emptyBackticks() throws Exception {
		// "``" — length 2 with both backticks — backtick branch is taken with empty inner.
		// The inline JSON5 parser parses empty string (with name optional) and yields a default-valued bean.
		var md = "| Property | Value |\n|---|---|\n| name | `` |\n| age | 0 |";
		var r = MarkdownParser.DEFAULT.parse(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals(0, r.age);
	}
}
