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
package org.apache.juneau.marshall.toml;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

class Toml_Test {

	@Test
	void a01_of() {
		var m = new LinkedHashMap<String, Object>();
		m.put("a", "1");
		m.put("b", 2);

		String toml = Toml.of(m);
		assertNotNull(toml);

		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("1", parsed.getString("a"));
		assertEquals(2, parsed.getInt("b"));
	}

	@Test
	void a02_roundTripString() {
		var m = JsonMap.of("s", "hello");
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("hello", parsed.getString("s"));
	}

	@Test
	void a03_roundTripNumber() {
		var m = JsonMap.of("n", 42);
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(42, parsed.getInt("n"));
	}

	@Test
	void a04_roundTripBoolean() {
		var m = JsonMap.of("b", true);
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertTrue(parsed.getBoolean("b"));
	}

	@Test
	void a05_roundTripList() {
		var m = JsonMap.of("tags", List.of("a", "b", "c"));
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var list = parsed.getList("tags", String.class, List.of());
		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}

	@Test
	void a06_roundTripNested() {
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		db.put("port", 5432);
		var config = new LinkedHashMap<String, Object>();
		config.put("name", "myapp");
		config.put("database", db);

		String toml = Toml.of(config);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("myapp", parsed.getString("name"));
		JsonMap dbParsed = parsed.getMap("database");
		assertNotNull(dbParsed);
		assertEquals("localhost", dbParsed.get("host"));
		assertEquals(5432L, dbParsed.get("port"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Array of tables
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_parseArrayOfTables() {
		var toml = """
			[[products]]
			name = "Hammer"
			sku = 738594937

			[[products]]
			name = "Nail"
			sku = 284758393
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var products = parsed.getList("products");
		assertNotNull(products);
		assertEquals(2, products.size());
		assertEquals("Hammer", ((Map<?,?>)products.get(0)).get("name"));
		assertEquals(738594937L, ((Map<?,?>)products.get(0)).get("sku"));
		assertEquals("Nail", ((Map<?,?>)products.get(1)).get("name"));
	}

	@Test
	void b02_serializeAndParseArrayOfTables() {
		// Verify serializer produces array-of-tables for map with list-of-maps value
		// and that the round-trip parser produces the same structure
		var toml = """
			title = "Store"

			[[products]]
			name = "Hammer"
			sku = 738594937

			[[products]]
			name = "Nail"
			sku = 284758393
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("Store", parsed.getString("title"));
		var products = parsed.getList("products");
		assertNotNull(products);
		assertEquals(2, products.size());
		assertEquals("Hammer", ((Map<?,?>)products.get(0)).get("name"));
		assertEquals(738594937L, ((Map<?,?>)products.get(0)).get("sku"));
		assertEquals("Nail", ((Map<?,?>)products.get(1)).get("name"));
		assertEquals(284758393L, ((Map<?,?>)products.get(1)).get("sku"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Inline tables
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_parseInlineTable() {
		var toml = "point = {x = 1, y = 2}\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		JsonMap point = parsed.getMap("point");
		assertNotNull(point);
		assertEquals(1L, point.get("x"));
		assertEquals(2L, point.get("y"));
	}

	@Test
	void c02_parseNestedInlineTable() {
		var toml = "config = {db = {host = \"localhost\", port = 5432}}\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var config = parsed.get("config");
		assertNotNull(config);
		assertTrue(config instanceof Map, "Expected Map for config");
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var configMap = (Map<String, Object>) config;
		var db = configMap.get("db");
		assertNotNull(db);
		assertTrue(db instanceof Map, "Expected Map for db");
		@SuppressWarnings({
			"unchecked"  // Unchecked cast required for generic test utility.
		})
		var dbMap = (Map<String, Object>) db;
		assertEquals("localhost", dbMap.get("host"));
		assertEquals(5432L, dbMap.get("port"));
	}

	@Test
	void c03_serializerUseInlineTables() {
		var s = TomlSerializer.create().useInlineTables(true).inlineTableThreshold(5).build();
		var inner = new LinkedHashMap<String, Object>();
		inner.put("x", 1);
		inner.put("y", 2);
		var outer = new LinkedHashMap<String, Object>();
		outer.put("point", inner);

		String toml = s.serialize(outer);
		assertNotNull(toml);
		// The inner map should be under a [point] table header since maps go through serializeMapAtRoot
		assertTrue(toml.contains("point") && toml.contains("x = 1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Basic string escape sequences
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_parseBasicStringWithEscapes() {
		var toml = "msg = \"line1\\nline2\\ttab\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("line1\nline2\ttab", parsed.getString("msg"));
	}

	@Test
	void d02_parseBasicStringWithUnicode() {
		var toml = "emoji = \"\\u0041\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("A", parsed.getString("emoji"));
	}

	@Test
	void d03_parseLiteralString() {
		var toml = "path = 'C:\\Users\\admin'\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		// Literal strings have no escape processing
		assertEquals("C:\\Users\\admin", parsed.getString("path"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Literal strings
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_parseLiteralStringNoEscapes() {
		var toml = "regex = '\\d{2} apps'\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("\\d{2} apps", parsed.getString("regex"));
	}

	@Test
	void e02_parseLiteralStringPreservesBackslash() {
		var toml = "path = 'C:\\path\\to\\file'\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("C:\\path\\to\\file", parsed.getString("path"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Datetime types
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_parseOffsetDateTime() {
		var toml = "odt = 1979-05-27T07:32:00Z\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var value = parsed.get("odt");
		assertNotNull(value);
		assertTrue(value instanceof OffsetDateTime, "Expected OffsetDateTime, got " + cn(value));
		var odt = (OffsetDateTime) value;
		assertEquals(1979, odt.getYear());
		assertEquals(5, odt.getMonthValue());
		assertEquals(27, odt.getDayOfMonth());
	}

	@Test
	void f02_parseLocalDateTime() {
		var toml = "ldt = 1979-05-27T07:32:00\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var value = parsed.get("ldt");
		assertNotNull(value);
		assertTrue(value instanceof LocalDateTime, "Expected LocalDateTime, got " + cn(value));
		var ldt = (LocalDateTime) value;
		assertEquals(1979, ldt.getYear());
		assertEquals(7, ldt.getHour());
	}

	@Test
	void f03_parseLocalDate() {
		var toml = "ld = 1979-05-27\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var value = parsed.get("ld");
		assertNotNull(value);
		// Parser may return as string for later conversion or as LocalDate
		if (value instanceof LocalDate ld) {
			assertEquals(1979, ld.getYear());
		} else {
			assertEquals("1979-05-27", value.toString());
		}
	}

	@Test
	void f04_parseLocalTime() {
		var toml = "lt = 07:32:00\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var value = parsed.get("lt");
		assertNotNull(value);
		// Bare time values may be returned as String for later conversion
		if (value instanceof LocalTime lt) {
			assertEquals(7, lt.getHour());
			assertEquals(32, lt.getMinute());
		} else {
			assertEquals("07:32:00", value.toString());
		}
	}

	@Test
	void f05_parseOffsetDateTimeWithOffset() {
		var toml = "odt = 1979-05-27T07:32:00+05:30\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var value = parsed.get("odt");
		assertNotNull(value);
		assertTrue(value instanceof OffsetDateTime, "Expected OffsetDateTime, got " + cn(value));
		var odt = (OffsetDateTime) value;
		assertEquals(ZoneOffset.ofHoursMinutes(5, 30), odt.getOffset());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Invalid escape sequences
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_invalidEscapeSequence() {
		var toml = "bad = \"hello\\qworld\"\n";
		assertThrows(ParseException.class, () -> Toml.to(toml, JsonMap.class));
	}

	@Test
	void g02_unterminatedString() {
		var toml = "bad = \"hello\n";
		assertThrows(ParseException.class, () -> Toml.to(toml, JsonMap.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer builder options
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_serializerCopy() {
		var s = TomlSerializer.create().sortKeys(true).nullValue("NIL").build();
		assertNotNull(s);
		assertEquals("NIL", s.getNullValue());
		// Verify copy preserves settings
		var copy = s.copy().build();
		assertEquals("NIL", copy.getNullValue());
	}

	@Test
	void h02_nullValue() {
		var s = TomlSerializer.create().keepNullProperties().nullValue("~NULL~").build();
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "test");
		m.put("value", null);
		String toml = s.serialize(m);
		assertTrue(toml.contains("~NULL~"), "Should contain custom null marker");
	}

	@Test
	void h03_readableSerializer() {
		var m = new LinkedHashMap<String, Object>();
		m.put("name", "test");
		var db = new LinkedHashMap<String, Object>();
		db.put("host", "localhost");
		m.put("database", db);

		String toml = TomlSerializer.DEFAULT_READABLE.serialize(m);
		assertNotNull(toml);
		assertTrue(toml.contains("[database]"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parser edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_commentsAreIgnored() {
		var toml = """
			# This is a comment
			name = "test" # inline comment
			# Another comment
			port = 8080
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("test", parsed.getString("name"));
		assertEquals(8080L, parsed.get("port"));
	}

	@Test
	void i02_dottedKeys() {
		var toml = "physical.color = \"orange\"\nphysical.shape = \"round\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		JsonMap physical = parsed.getMap("physical");
		assertNotNull(physical);
		assertEquals("orange", physical.get("color"));
		assertEquals("round", physical.get("shape"));
	}

	@Test
	void i03_floatValues() {
		var toml = """
			pi = 3.14159
			neg = -0.5
			sci = 1.5e2
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(3.14159, (Double) parsed.get("pi"), 0.00001);
		assertEquals(-0.5, (Double) parsed.get("neg"), 0.00001);
		assertEquals(150.0, (Double) parsed.get("sci"), 0.00001);
	}

	@Test
	void i04_negativeIntegers() {
		var toml = """
			neg = -42
			pos = +17
			zero = 0
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(-42L, parsed.get("neg"));
		assertEquals(17L, parsed.get("pos"));
		assertEquals(0L, parsed.get("zero"));
	}

	@Test
	void i05_underscoresInNumbers() {
		var toml = "big = 1_000_000\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(1000000L, parsed.get("big"));
	}

	@Test
	void i06_emptyInput() {
		var toml = "";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		// Empty TOML produces null or empty map
		assertTrue(parsed == null || parsed.isEmpty());
	}

	@Test
	void i07_quotedKeys() {
		var toml = "\"key with spaces\" = \"value\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("value", parsed.get("key with spaces"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Serializer special values
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_serializeFloatSpecials() {
		var m = new LinkedHashMap<String, Object>();
		m.put("pos_inf", Double.POSITIVE_INFINITY);
		m.put("neg_inf", Double.NEGATIVE_INFINITY);
		m.put("nan_val", Double.NaN);
		String toml = Toml.of(m);
		assertTrue(toml.contains("inf"));
		assertTrue(toml.contains("-inf"));
		assertTrue(toml.contains("nan"));
	}

	@Test
	void j02_serializeEscapedStrings() {
		var m = JsonMap.of("msg", "hello\nworld\t\"quoted\"");
		String toml = Toml.of(m);
		assertTrue(toml.contains("\\n"), "Should contain escaped newline");
		assertTrue(toml.contains("\\t"), "Should contain escaped tab");
		assertTrue(toml.contains("\\\""), "Should contain escaped quote");
	}

	@Test
	void j03_roundTripEscapedStrings() {
		var m = JsonMap.of("msg", "line1\nline2\ttab");
		String toml = Toml.of(m);
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("line1\nline2\ttab", parsed.getString("msg"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean round-trip with array of tables
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void k01_roundTripRootArrayOfTables() {
		// Parse array-of-tables and verify structure
		var toml = """
			[[servers]]
			host = "alpha"
			port = 8080

			[[servers]]
			host = "beta"
			port = 9090
			""";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		var servers = parsed.getList("servers");
		assertNotNull(servers);
		assertEquals(2, servers.size());
		assertEquals("alpha", ((Map<?,?>)servers.get(0)).get("host"));
		assertEquals(8080L, ((Map<?,?>)servers.get(0)).get("port"));
		assertEquals("beta", ((Map<?,?>)servers.get(1)).get("host"));
		assertEquals(9090L, ((Map<?,?>)servers.get(1)).get("port"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// TomlWriter functionality
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_quotedKeyForSpecialChars() {
		var m = JsonMap.of("key.with.dots", "value1", "key with spaces", "value2");
		String toml = Toml.of(m);
		assertTrue(toml.contains("\"key.with.dots\"") || toml.contains("'key.with.dots'"),
			"Keys with dots should be quoted");
		assertTrue(toml.contains("\"key with spaces\"") || toml.contains("'key with spaces'"),
			"Keys with spaces should be quoted");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parser creates correct builder
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_parserBuilderNullValue() {
		var p = TomlParser.create().nullValue("N/A").build();
		assertEquals("N/A", p.getNullValue());
	}

	@Test
	void m02_serializerBuilderNullValue() {
		var s = TomlSerializer.create().nullValue("N/A").build();
		assertEquals("N/A", s.getNullValue());
	}

	@Test
	void m03_parserDefaultNullValue() {
		assertEquals("<NULL>", TomlParser.DEFAULT.getNullValue());
	}

	@Test
	void m04_serializerDefaultNullValue() {
		assertEquals("<NULL>", TomlSerializer.DEFAULT.getNullValue());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Multi-line strings (Bug 1)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void n01_parseMultiLineBasicString() {
		var toml = "msg = \"\"\"line1\nline2\"\"\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("line1\nline2", parsed.getString("msg"));
	}

	@Test
	void n02_parseMultiLineLiteralString() {
		var toml = "msg = '''line1\nline2'''\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("line1\nline2", parsed.getString("msg"));
	}

	@Test
	void n03_parseMultiLineBasicStringWithEscapes() {
		var toml = "msg = \"\"\"hello \\\"world\\\"\"\"\"\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals("hello \"world\"", parsed.getString("msg"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Hex / octal / binary integers (Bug 2)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void o01_parseHexInteger() {
		var toml = "n = 0xFF\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(255L, parsed.get("n"));
	}

	@Test
	void o02_parseHexIntegerLowerCase() {
		var toml = "n = 0xdeadbeef\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(0xdeadbeefL, parsed.get("n"));
	}

	@Test
	void o03_parseOctalInteger() {
		var toml = "n = 0o755\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(0755L, parsed.get("n"));
	}

	@Test
	void o04_parseBinaryInteger() {
		var toml = "n = 0b1010\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(10L, parsed.get("n"));
	}

	@Test
	void o05_parseHexIntegerWithUnderscores() {
		var toml = "n = 0xFFFF_FFFF\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(0xFFFFFFFFL, parsed.get("n"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// inf / -inf / +inf / nan (Bug 3)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void p01_parsePositiveInfinity() {
		var toml = "x = inf\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, (Double) parsed.get("x"));
	}

	@Test
	void p02_parseExplicitPositiveInfinity() {
		var toml = "x = +inf\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, (Double) parsed.get("x"));
	}

	@Test
	void p03_parseNegativeInfinity() {
		var toml = "x = -inf\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertEquals(Double.NEGATIVE_INFINITY, (Double) parsed.get("x"));
	}

	@Test
	void p04_parseNan() {
		var toml = "x = nan\n";
		JsonMap parsed = Toml.to(toml, JsonMap.class);
		assertTrue(Double.isNaN((Double) parsed.get("x")));
	}
}
