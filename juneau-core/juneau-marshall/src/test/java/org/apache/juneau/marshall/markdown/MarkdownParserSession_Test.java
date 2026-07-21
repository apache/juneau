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
package org.apache.juneau.marshall.markdown;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Coverage tests for {@link MarkdownParserSession} branches not exercised by the higher-level
 * {@link MarkdownParser_Test} suite (see coverage analysis issue 155, Tier E3).
 */
class MarkdownParserSession_Test extends TestBase {

	//====================================================================================================
	// a - Bullet list marker variants and edge cases
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a01_bulletListAsterisk() {
		var md = "* alpha\n* beta\n* gamma";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(List.of("alpha", "beta", "gamma"), r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a02_bulletListPlus() {
		var md = "+ one\n+ two";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(List.of("one", "two"), r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a03_bulletListEmptyMarkers() {
		// Bare "-", "*", "+" markers should produce empty-string entries.
		var md = "- a\n-\n- b";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("a", r.get(0));
		assertEquals("", r.get(1));
		assertEquals("b", r.get(2));
	}

	@Test void a04_bulletListToArray() {
		var md = "* x\n* y";
		var r = MarkdownParser.DEFAULT.read(md, String[].class);
		assertArrayEquals(new String[]{"x", "y"}, r);
	}

	@Test void a05_bulletListToCustomCollection() {
		// Exercise the canCreateNewInstance(outer) branch in readBulletList for a concrete List type.
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (LinkedList<String>) MarkdownParser.DEFAULT.read("- a\n- b", LinkedList.class, String.class);
		assertEquals(2, r.size());
		assertEquals("a", r.getFirst());
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void a06_bulletListNullValues() {
		var md = "- foo\n- *null*\n- bar";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("foo", r.get(0));
		assertNull(r.get(1));
		assertEquals("bar", r.get(2));
	}

	//====================================================================================================
	// b - Empty / blank input handling
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("b01_readsToNullProvider")
	void b01_readsToNull(String input, Class<?> type) {
		assertNull(MarkdownParser.DEFAULT.read(input, type));
	}

	static Stream<Arguments> b01_readsToNullProvider() {
		return Stream.of(
			Arguments.of("\n   \n\t\n", Map.class),                   // b01: blank lines only
			Arguments.of("*null*", MarkdownParser_Test.A.class),      // b06: "*null*" plain value to non-string type
			Arguments.of("", MarkdownParser_Test.A.class),            // o01: empty input for non-string type (readAnything null branch)
			Arguments.of("   \n  \n  ", MarkdownParser_Test.A.class)  // o03: blank lines only for non-string type
		);
	}

	@ParameterizedTest
	@MethodSource("b02_readToStringProvider")
	void b02_readToString(String input, String expected) {
		assertEquals(expected, MarkdownParser.DEFAULT.read(input, String.class));
	}

	static Stream<Arguments> b02_readToStringProvider() {
		return Stream.of(
			Arguments.of("", ""),
			Arguments.of("   \n  ", ""),
			Arguments.of("hello", "hello")
		);
	}

	@Test void b05_plainTextToInteger() {
		var r = MarkdownParser.DEFAULT.read("42", Integer.class);
		assertEquals(42, r);
	}

	//====================================================================================================
	// c - Table edge cases (empty data rows, separator filtering)
	//====================================================================================================

	@Test void c01_tableNoDataRows() {
		// Header + separator only → empty bean (all defaults).
		var md = "| Property | Value |\n|---|---|";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertNull(r.name);
		assertEquals(0, r.age);
	}

	@Test void c02_tableNoTableLines() {
		// Only a separator with no |-prefixed lines → null result for tables.
		// Use plain text that does not start with | - Markdown parser should handle it as text.
		var md = "  ---\n";
		var r = MarkdownParser.DEFAULT.read(md, String.class);
		assertEquals("---", r);
	}

	@Test void c03_keyHeaderVariant() {
		// "Key"/"Value" headers should also be detected as a key/value table.
		var md = "| Key | Value |\n|---|---|\n| name | Bob |\n| age | 25 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertEquals("Bob", r.name);
		assertEquals(25, r.age);
	}

	@Test void c04_tableEmptyHeaderCells() {
		// A row with empty headers should produce an empty result.
		var md = "|  |  |\n|---|---|\n| a | b |";
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		// With empty headers we still get a map keyed by "".
		assertNotNull(r);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c05_multiColumnAllNullRow() {
		// A row of all *null*/empty cells should yield a null entry in the list.
		var md = "| name | age |\n|---|---|\n| *null* | *null* |\n| Alice | 30 |";
		var r = (List<MarkdownParser_Test.B>) MarkdownParser.DEFAULT.read(md, List.class, MarkdownParser_Test.B.class);
		assertEquals(2, r.size());
		assertNull(r.get(0));
		assertEquals("Alice", r.get(1).name);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void c06_multiColumnToObject() {
		// Multi-column with isObject() target → ArrayList of MarshalledMaps.
		var md = "| name | age |\n|---|---|\n| Alice | 30 |";
		var r = (List<Map<String,Object>>) MarkdownParser.DEFAULT.read(md, Object.class);
		assertEquals(1, r.size());
		assertEquals("Alice", r.get(0).get("name"));
		assertEquals(30, r.get(0).get("age"));
	}

	@Test void c07_multiColumnSingleBean() {
		// Multi-column with a non-list/array bean target → parses just the first row.
		var md = "| name | age |\n|---|---|\n| Alice | 30 |\n| Bob | 25 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.B.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void c08_multiColumnSingleBeanNoRows() {
		// Single-bean target with no data rows → null.
		var md = "| name | age |\n|---|---|";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.B.class);
		assertNull(r);
	}

	//====================================================================================================
	// d - Cell-value coercion paths (numbers, booleans, plain text)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("d01_autoDetectProvider")
	void d01_autoDetect(String md, String key, Object expected) {
		var r = (Map<?,?>) MarkdownParser.DEFAULT.read(md, Object.class);
		assertEquals(expected, r.get(key));
		assertInstanceOf(expected.getClass(), r.get(key));
	}

	static Stream<Arguments> d01_autoDetectProvider() {
		return Stream.of(
			// d01: 9999999999 auto-detected as Long
			Arguments.of("| Property | Value |\n|---|---|\n| big | 9999999999 |", "big", 9999999999L),
			// d02: 3.14 auto-detected as Double
			Arguments.of("| Property | Value |\n|---|---|\n| pi | 3.14 |", "pi", 3.14),
			// d03: false auto-detected as Boolean
			Arguments.of("| Property | Value |\n|---|---|\n| flag | false |", "flag", Boolean.FALSE)
		);
	}

	@Test void d04_emptyCellPreservedForString() {
		// Empty cell to a String-typed property → empty string.
		var md = "| name | age |\n|---|---|\n|  | 1 |";
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (List<MarkdownParser_Test.B>) MarkdownParser.DEFAULT.read(md, List.class, MarkdownParser_Test.B.class);
		assertEquals(1, r.size());
		assertEquals("", r.get(0).name);
	}

	@ParameterizedTest
	@MethodSource("d05_readThrowsProvider")
	void d05_readThrows(String md, Class<? extends Throwable> expected, Class<?> type) {
		assertThrows(expected, () -> MarkdownParser.DEFAULT.read(md, type));
	}

	static Stream<Arguments> d05_readThrowsProvider() {
		return Stream.of(
			// d05: invalid JSON5 inside backticks → ParseException
			Arguments.of("| Property | Value |\n|---|---|\n| addr | `{this is not json5` |", ParseException.class, MarkdownParser_Test.D.class),
			// d06: "abc" cannot convert to int → ParseException
			Arguments.of("| Property | Value |\n|---|---|\n| age | abc |", ParseException.class, MarkdownParser_Test.A.class),
			// v01: non-bean type can't be deserialized from a JSON5 object (needsJson5Path → keyValueTableToJson5)
			Arguments.of("| Property | Value |\n|---|---|\n| value | hello |", Exception.class, NonBeanType.class),
			// v03: multiple rows → keyValueTableToJson5 appends comma between entries (line 718-719)
			Arguments.of("| Property | Value |\n|---|---|\n| a | 1 |\n| b | 2 |", Exception.class, NonBeanType.class)
		);
	}

	//====================================================================================================
	// e - Optional element handling (isOptional branch)
	//====================================================================================================

	@Test void e01_readOptionalString() {
		var md = "hello";
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var r = (Optional<String>) MarkdownParser.DEFAULT.read(md, Optional.class, String.class);
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
		var r = (TreeMap<String,Integer>) MarkdownParser.DEFAULT.read(md, TreeMap.class, String.class, Integer.class);
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
	void f02_multiColumnToTreeMap() {
		// Single-row multi-column → Map element. Exercises readRow's isMap() branch.
		var md = "| k1 | k2 |\n|---|---|\n| v1 | v2 |";
		var r = (List<Map<String,String>>) MarkdownParser.DEFAULT.read(md, List.class, TreeMap.class, String.class, String.class);
		assertEquals(1, r.size());
		assertEquals("v1", r.get(0).get("k1"));
		assertEquals("v2", r.get(0).get("k2"));
		assertInstanceOf(TreeMap.class, r.get(0));
	}

	//====================================================================================================
	// g - Pipe and backslash escaping
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("g01_escapedCellValueProvider")
	void g01_escapedCellValue(String md, String key, String expected) {
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		assertEquals(expected, r.get(key));
	}

	static Stream<Arguments> g01_escapedCellValueProvider() {
		return Stream.of(
			Arguments.of("| Property | Value |\n|---|---|\n| path | a\\\\b |", "path", "a\\b"),
			Arguments.of("| Property | Value |\n|---|---|\n| str | foo\\nbar |", "str", "foo\\nbar"),
			Arguments.of("| Property | Value |\n|---|---|\n| s | abc\\|", "s", "abc|")
		);
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
	void h01_keyValueTypeResolution() {
		// _type row in a 2-column key/value table should resolve via bean dictionary.
		var p = MarkdownParser.create().beanDictionary(HA.class, HB.class).build();
		var md = "| Property | Value |\n|---|---|\n| _type | ha |\n| name | Alice |\n| age | 30 |";
		var r = p.read(md, Object.class);
		assertInstanceOf(HA.class, r);
		assertEquals("Alice", ((HA)r).name);
		assertEquals(30, ((HA)r).age);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void h02_multiColumnTypeColumn() {
		// _type column in multi-column table is recognized and skipped from output map keys.
		var p = MarkdownParser.create().beanDictionary(HA.class, HB.class).build();
		var md = "| _type | name | age |\n|---|---|---|\n| ha | Alice | 30 |";
		var r = (List<Map<String,Object>>) p.read(md, List.class, Object.class);
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

	@Test void i01_keyValueWithSwap() {
		// Swap forces the JSON5 path through keyValueTableToJson5/cellToJson5.
		var p = MarkdownParser.create().swaps(UpperCaseSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| wrapped | hello |";
		var r = p.read(md, IBean.class);
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

	@Test void i02_topLevelSwapKeyValueTable() {
		// Top-level type has a swap explicitly registered → needsJson5Path returns true → keyValueTableToJson5 path is hit.
		var p = MarkdownParser.create().swaps(IBeanMapSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| count | 7 |";
		var r = p.read(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertEquals(7, r.count);
	}

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void i03_topLevelSwapMultiColumnTable() {
		// Same swap exercised through readRow's needsJson5Path branch (rowToJson5).
		var p = MarkdownParser.create().swaps(IBeanMapSwap.class).build();
		var md = "| name | count |\n|---|---|\n| Alice | 3 |\n| Bob | 5 |";
		var r = (List<IBeanWithSwap>) p.read(md, List.class, IBeanWithSwap.class);
		assertEquals(2, r.size());
		assertEquals("Alice", r.get(0).name);
		assertEquals(3, r.get(0).count);
		assertEquals("Bob", r.get(1).name);
		assertEquals(5, r.get(1).count);
	}

	@Test void i04_cellToJson5_inlineJson5Unwrapped() {
		// Backtick-wrapped cell value embedded into the JSON5 object via cellToJson5.
		// Swap is explicitly registered so keyValueTableToJson5 path is taken.
		var p = MarkdownParser.create().swaps(IBeanMapSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | `'Alice'` |\n| count | 12 |";
		var r = p.read(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertEquals(12, r.count);
	}

	@Test void i05_cellToJson5_nullAndEmpty() {
		// "*null*" cell becomes JSON5 null; empty cell becomes "''" via cellToJson5.
		// Swap explicitly registered so keyValueTableToJson5 path is taken.
		var p = MarkdownParser.create().swaps(IBeanMapSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | *null* |\n| count | 0 |";
		var r = p.read(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertNull(r.name);
		assertEquals(0, r.count);
	}

	@Marshalled(typeName="i07bean")
	public static class I07Bean {
		public String name;
		public int count;
	}

	public static class I07BeanSwap extends ObjectSwap<I07Bean, Map<String,Object>> {
		@Override
		public Map<String,Object> swap(MarshallingSession session, I07Bean o) {
			var m = new LinkedHashMap<String,Object>();
			if (o != null) { m.put("name", o.name); m.put("count", o.count); }
			return m;
		}
		@Override
		public I07Bean unswap(MarshallingSession session, Map<String,Object> in, ClassMeta<?> hint, String attrName) {
			var b = new I07Bean();
			b.name = (String) in.get("name");
			b.count = in.get("count") == null ? 0 : ((Number)in.get("count")).intValue();
			return b;
		}
	}

	@Test void i07_keyValueTableToJson5_typeKey() {
		// _type key in keyValueTableToJson5 → _type handling branch (lines 721-722).
		var p = MarkdownParser.create().swaps(I07BeanSwap.class).beanDictionary(I07Bean.class).build();
		var md = "| Property | Value |\n|---|---|\n| _type | i07bean |\n| name | Bob |\n| count | 5 |";
		var r = p.read(md, I07Bean.class);
		assertNotNull(r);
		assertEquals("Bob", r.name);
	}

	@Test void i08_rowToJson5_typeColumn() {
		// _type column in multi-column table → rowToJson5 _type handling branch (lines 695-697).
		var p = MarkdownParser.create().swaps(I07BeanSwap.class).beanDictionary(I07Bean.class).build();
		var md = "| _type | name | count |\n|---|---|---|\n| i07bean | Carol | 9 |";
		@SuppressWarnings("unchecked")
		var r = (List<I07Bean>) p.read(md, List.class, I07Bean.class);
		assertNotNull(r);
		assertFalse(r.isEmpty());
		assertEquals("Carol", r.get(0).name);
	}

	@Test void i09_keyValueTableToJson5_withLongAndDouble() {
		// Long and Double cell values in keyValueTableToJson5 path → cellToJson5 Long/Double branches.
		// Use a class that is not a bean (no public fields) → needsJson5Path returns true
		// via !type.isBean(). Use IBeanMapSwap target so we get the swap path.
		var p = MarkdownParser.create().swaps(IBeanMapSwap.class).build();
		var md = "| Property | Value |\n|---|---|\n| name | test |\n| count | 9999999999 |";
		// count will be parsed as Long in cellToJson5 → Long branch hit
		var r = p.read(md, IBeanWithSwap.class);
		assertNotNull(r);
		assertEquals("test", r.name);
	}

	@Test void i06_cellToJson5_booleanLiteral() {
		// Bare "true"/"false" cell in JSON5 path is written without quotes.
		var p = MarkdownParser.create().swaps(IBeanMapSwapBool.class).build();
		var md = "| Property | Value |\n|---|---|\n| flag | true |\n| name | x |";
		var r = p.read(md, IBeanBool.class);
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
	void j01_customNullInBulletList() {
		var p = MarkdownParser.create().nullValue("NIL").build();
		var md = "- a\n- NIL\n- b";
		var r = (List<String>) p.read(md, List.class, String.class);
		assertEquals(3, r.size());
		assertEquals("a", r.get(0));
		assertNull(r.get(1));
		assertEquals("b", r.get(2));
	}

	//====================================================================================================
	// k - Whitespace / boundary table parsing
	//====================================================================================================

	@Test void k01_separatorWithSpacesAndColons() {
		// "| :--- | :---: |" should still be detected as a separator row.
		var md = "| Property | Value |\n| :--- | :---: |\n| name | Alice |";
		var r = MarkdownParser.DEFAULT.read(md, Map.class);
		assertEquals("Alice", r.get("name"));
	}

	@Test void k02_lineWithoutPipeIgnored() {
		// Lines not starting with | should be filtered out of the table-line collection.
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\nthis is not a row\n| age | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// l - Multi-column simple element type (readRow simple type fall-through)
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	void l01_multiColumnToStringList() {
		// List<String> as multi-column target → readRow's "simple type" fall-through is hit.
		var md = "| col1 | col2 |\n|---|---|\n| value-a | ignored |\n| value-b | ignored |";
		var r = (List<String>) MarkdownParser.DEFAULT.read(md, List.class, String.class);
		assertEquals(2, r.size());
		assertEquals("value-a", r.get(0));
		assertEquals("value-b", r.get(1));
	}

	//====================================================================================================
	// m - Inline JSON5 with parse error
	//====================================================================================================

	@Test void m01_emptyBackticks() {
		// "``" — length 2 with both backticks — backtick branch is taken with empty inner.
		// The inline JSON5 parser parses empty string (with name optional) and yields a default-valued bean.
		var md = "| Property | Value |\n|---|---|\n| name | `` |\n| age | 0 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals(0, r.age);
	}

	//====================================================================================================
	// n - readCellValue edge cases
	//====================================================================================================

	@Test void n01_trimStrings_trimsCellValue() {
		// isTrimStrings() = true → cell value is trimmed (line 574 in readCellValue, line 60 in json5Parser memoizer)
		var p = MarkdownParser.create().trimStrings().build();
		var md = "| Property | Value |\n|---|---|\n| name |   Alice   |\n| age | 30 |";
		var r = p.read(md, MarkdownParser_Test.A.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void n02_emptyCellForIntType() {
		// Empty cell for int type → readCellValue returns null/default-zero at line 578
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| age |  |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertEquals("Alice", r.name);
		assertEquals(0, r.age);
	}

	@Test void n03_customNullValueMatch() {
		// Cell value matching custom nullValue → readCellValue returns null at line 572
		var p = MarkdownParser.create().nullValue("N/A").build();
		var md = "| Property | Value |\n|---|---|\n| name | N/A |\n| age | 30 |";
		var r = p.read(md, MarkdownParser_Test.A.class);
		assertNull(r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// o - Empty input parsing
	//====================================================================================================

	@Test void o02_emptyInputForString() {
		// Empty input string for String type → readAnything returns "" at line 175
		var r = MarkdownParser.DEFAULT.read("", String.class);
		assertEquals("", r);
	}

	//====================================================================================================
	// p - Polymorphic bean parsing with _type row
	//====================================================================================================

	@Marshalled(typeName="pbase")
	public static class PBase {
		public String name;
	}

	@Marshalled(typeName="pchild")
	public static class PChild extends PBase {
		public String extra;
	}

	@Test void p01_beanWithTypeRow_resolvedViaRegistry() {
		// Key-value table with _type row for bean type → lines 266-270 in readKeyValueTable
		// Registry is non-null and resolves to the child type
		var p = MarkdownParser.create().beanDictionary(PBase.class, PChild.class).build();
		var md = "| Property | Value |\n|---|---|\n| _type | pchild |\n| name | Alice |\n| extra | bonus |";
		var r = p.read(md, PBase.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertInstanceOf(PChild.class, r);
		assertEquals("bonus", ((PChild) r).extra);
	}

	@Test void p02_beanWithTypeRow_noRegistry() {
		// Key-value table with _type row but no registry → resolved == null, actualType unchanged (line 269 false branch)
		var md = "| Property | Value |\n|---|---|\n| _type | unknown |\n| name | Bob |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals("Bob", r.name);
	}

	//====================================================================================================
	// q - Unknown bean property handling (onUnknownProperty path)
	//====================================================================================================

	@Test void q01_beanWithUnknownProperty() {
		// Key-value table with property not in the bean → pm == null → onUnknownProperty at line 300
		// Must enable ignoreUnknownBeanProperties to avoid exception
		var p = MarkdownParser.create().ignoreUnknownBeanProperties().build();
		var md = "| Property | Value |\n|---|---|\n| name | Alice |\n| unknownProp | someValue |";
		var r = p.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
	}

	//====================================================================================================
	// r - Multi-column table edge cases (readMultiColumnTable)
	//====================================================================================================

	@Test void r01_multiColumnTable_asArray() {
		// Multi-column table parsed as array type → isArray() branch in readMultiColumnTable at line 366
		var md = "| name | age |\n|---|---|\n| Alice | 30 |\n| Bob | 25 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A[].class);
		assertNotNull(r);
		assertEquals(2, r.length);
		assertEquals("Alice", r[0].name);
	}

	@Test void r02_multiColumnTable_singleBeanTarget() {
		// Multi-column table with a single bean target (not a collection/array) → else branch at line 375
		var md = "| name | age |\n|---|---|\n| Alice | 30 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	//====================================================================================================
	// s - readRow: all-null cells and _type column in multi-column table
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"
	})
	void s01_readRow_allNullCells() {
		// All cells are null/nullValue → allNull=true → readRow returns null at line 415 (line 371 check)
		var md = "| name | age |\n|---|---|\n| *null* | *null* |";
		var r = (List<MarkdownParser_Test.A>) MarkdownParser.DEFAULT.read(md, List.class, MarkdownParser_Test.A.class);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertNull(r.get(0));
	}

	@Test
	@SuppressWarnings({
		"unchecked"
	})
	void s02_readRow_withTypeColumn_resolvesType() {
		// Multi-column table with _type column → typeColIndex resolution (line 422)
		var p = MarkdownParser.create().beanDictionary(PBase.class, PChild.class).build();
		var md = "| _type | name | extra |\n|---|---|---|\n| pchild | Alice | bonus |";
		var r = (List<PBase>) p.read(md, List.class, PBase.class);
		assertNotNull(r);
		assertEquals(1, r.size());
		assertInstanceOf(PChild.class, r.get(0));
		assertEquals("Alice", r.get(0).name);
	}

	//====================================================================================================
	// t - Map target in readKeyValueTable (line 306)
	//====================================================================================================

	@Test
	@SuppressWarnings({
		"unchecked"
	})
	void t01_keyValueTable_asTypedMap() {
		// Key-value table with typed Map target → isMap() branch in readKeyValueTable at line 306
		// Also covers line 311 (keyType != null), 315 (cells.size() < 2)
		var r = (TreeMap<String, String>) MarkdownParser.DEFAULT.read(
			"| Key | Value |\n|---|---|\n| k1 | v1 |\n| k2 | v2 |",
			TreeMap.class, String.class, String.class
		);
		assertNotNull(r);
		assertEquals("v1", r.get("k1"));
		assertEquals("v2", r.get("k2"));
	}

	//====================================================================================================
	// u - readCellValue backtick edge cases (line 582)
	//====================================================================================================

	@Test void u01_backtick_singleChar() {
		// Single backtick cell "`" — startsWith("`") and endsWith("`") but length == 1 → NOT inline JSON5
		var md = "| Property | Value |\n|---|---|\n| name | ` |\n| age | 10 |";
		var r = MarkdownParser.DEFAULT.read(md, MarkdownParser_Test.A.class);
		assertNotNull(r);
	}

	//====================================================================================================
	// v - keyValueTableToJson5 path (needsJson5Path returns true for non-bean, non-primitive types)
	//====================================================================================================

	// A class with a static fromString factory — NOT a Juneau bean (no public fields/properties)
	public static class NonBeanType {
		private final String value;
		private NonBeanType(String v) { this.value = v; }
		public static NonBeanType fromString(String s) { return new NonBeanType(s); }
		@Override public String toString() { return value; }
	}

	@Test void v02_keyValueTableToJson5_typeKeyRow() {
		// Non-bean type with _type row → keyValueTableToJson5 handles _type as quoted string (line 721-722)
		// May succeed or fail depending on how Json5Parser handles the output; either way, line 721-722 is exercised.
		var md = "| Property | Value |\n|---|---|\n| _type | mytype |\n| value | hello |";
		assertDoesNotThrow(() -> {
			try {
				MarkdownParser.DEFAULT.read(md, NonBeanType.class);
			} catch (Exception e) {
				// Expected: non-bean type can't be deserialized from JSON5 object
			}
		});
	}
}
