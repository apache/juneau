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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@SuppressWarnings({
	"resource", // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
	"java:S8694" // Test data uses literal month ints for date construction; Month enum constants add noise without value.
})
class TomlWriter_Test extends TestBase {

	@FunctionalInterface
	private interface ThrowingConsumer<T> {
		void accept(T t) throws Exception;
	}

	private static TomlWriter newWriter(StringWriter sw, boolean trimStrings) {
		return new TomlWriter(sw, false, 10, trimStrings,
			UriResolver.of(UriResolution.NONE, UriRelativity.RESOURCE, UriContext.DEFAULT));
	}

	private static String run(ThrowingConsumer<TomlWriter> body) throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		body.accept(w);
		w.flush();
		return sw.toString();
	}

	private static String runTrim(ThrowingConsumer<TomlWriter> body) throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, true);
		body.accept(w);
		w.flush();
		return sw.toString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// tableHeader / arrayOfTablesHeader
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_tableHeader() throws Exception {
		assertEquals("[database]\n", run(w -> w.tableHeader("database")));
	}

	@Test
	void a02_arrayOfTablesHeader() throws Exception {
		assertEquals("[[products]]\n", run(w -> w.arrayOfTablesHeader("products")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// keyValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_keyValueBareKey() throws Exception {
		assertEquals("foo = \"bar\"\n", run(w -> w.keyValue("foo", "bar")));
	}

	@Test
	void b02_keyValueQuotedKey() throws Exception {
		assertEquals("\"foo bar\" = \"baz\"\n", run(w -> w.keyValue("foo bar", "baz")));
	}

	@Test
	void b03_keyValueDottedKeyQuoted() throws Exception {
		// Dots are not bare-key chars
		assertEquals("\"a.b\" = \"v\"\n", run(w -> w.keyValue("a.b", "v")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// bareKey / quotedKey
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_bareKey() throws Exception {
		assertEquals("simple", run(w -> w.bareKey("simple")));
	}

	@Test
	void c02_quotedKey() throws Exception {
		assertEquals("\"key\"", run(w -> w.quotedKey("key")));
	}

	@Test
	void c03_quotedKeyEscapesQuotes() throws Exception {
		assertEquals("\"a\\\"b\"", run(w -> w.quotedKey("a\"b")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// stringValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_stringValueSimple() throws Exception {
		assertEquals("\"hello\"", run(w -> w.stringValue("hello")));
	}

	@Test
	void d02_stringValueNullBecomesEmpty() throws Exception {
		assertEquals("\"\"", run(w -> w.stringValue(null)));
	}

	@Test
	void d03_stringValueWithEscapes() throws Exception {
		assertEquals("\"a\\nb\\tc\"", run(w -> w.stringValue("a\nb\tc")));
	}

	@Test
	void d04_stringValueTrimsWhenEnabled() throws Exception {
		assertEquals("\"hello\"", runTrim(w -> w.stringValue("  hello  ")));
	}

	@Test
	void d05_stringValueDoesNotTrimWhenDisabled() throws Exception {
		assertEquals("\"  hello  \"", run(w -> w.stringValue("  hello  ")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// literalString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_literalStringSimple() throws Exception {
		assertEquals("'C:\\path\\to\\file'", run(w -> w.literalString("C:\\path\\to\\file")));
	}

	@Test
	void e02_literalStringNullBecomesEmpty() throws Exception {
		assertEquals("''", run(w -> w.literalString(null)));
	}

	@Test
	void e03_literalStringEscapesEmbeddedSingleQuote() throws Exception {
		// TOML literal strings cannot contain ' characters; doubling is the escape used by this writer.
		assertEquals("'a''b'", run(w -> w.literalString("a'b")));
	}

	@Test
	void e04_literalStringTrimsWhenEnabled() throws Exception {
		assertEquals("'hello'", runTrim(w -> w.literalString("  hello  ")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// multiLineString
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_multiLineStringSimple() throws Exception {
		assertEquals("\"\"\"\nfoo\nbar\"\"\"", run(w -> w.multiLineString("foo\nbar")));
	}

	@Test
	void f02_multiLineStringNullBecomesEmpty() throws Exception {
		assertEquals("\"\"\"\n\"\"\"", run(w -> w.multiLineString(null)));
	}

	@Test
	void f03_multiLineStringTrimsWhenEnabled() throws Exception {
		assertEquals("\"\"\"\nhello\"\"\"", runTrim(w -> w.multiLineString("  hello  ")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// integerValue / floatValue / booleanValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_integerValuePositive() throws Exception {
		assertEquals("42", run(w -> w.integerValue(42)));
	}

	@Test
	void g02_integerValueNegative() throws Exception {
		assertEquals("-42", run(w -> w.integerValue(-42)));
	}

	@Test
	void g03_integerValueLong() throws Exception {
		assertEquals("9999999999", run(w -> w.integerValue(9_999_999_999L)));
	}

	@Test
	void g04_floatValue() throws Exception {
		assertEquals("3.14", run(w -> w.floatValue(3.14)));
	}

	@Test
	void g05_floatValueWholeNumberStripsTrailing() throws Exception {
		// Double.toString(3.0) → "3.0"; writer trims trailing ".0"
		assertEquals("3", run(w -> w.floatValue(3.0)));
	}

	@Test
	void g06_floatValueNan() throws Exception {
		assertEquals("nan", run(w -> w.floatValue(Double.NaN)));
	}

	@Test
	void g07_floatValuePosInf() throws Exception {
		assertEquals("inf", run(w -> w.floatValue(Double.POSITIVE_INFINITY)));
	}

	@Test
	void g08_floatValueNegInf() throws Exception {
		assertEquals("-inf", run(w -> w.floatValue(Double.NEGATIVE_INFINITY)));
	}

	@Test
	void g09_floatValueScientific() throws Exception {
		assertEquals("1.5E-10", run(w -> w.floatValue(1.5e-10)));
	}

	@Test
	void g10_booleanTrue() throws Exception {
		assertEquals("true", run(w -> w.booleanValue(true)));
	}

	@Test
	void g11_booleanFalse() throws Exception {
		assertEquals("false", run(w -> w.booleanValue(false)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// dateTimeValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_dateTimeOffsetDateTime() throws Exception {
		var odt = OffsetDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.UTC);
		var out = run(w -> w.dateTimeValue(odt));
		assertEquals("1979-05-27T07:32:00Z", out);
	}

	@Test
	void h02_dateTimeInstant() throws Exception {
		var i = Instant.parse("1979-05-27T07:32:00Z");
		var out = run(w -> w.dateTimeValue(i));
		assertEquals("1979-05-27T07:32:00Z", out);
	}

	@Test
	void h03_dateTimeZonedDateTime() throws Exception {
		var zdt = ZonedDateTime.of(1979, 5, 27, 7, 32, 0, 0, ZoneOffset.ofHours(2));
		var out = run(w -> w.dateTimeValue(zdt));
		assertEquals("1979-05-27T07:32:00+02:00", out);
	}

	@Test
	void h04_dateTimeLocalDateTime() throws Exception {
		var ldt = LocalDateTime.of(1979, 5, 27, 7, 32, 0);
		var out = run(w -> w.dateTimeValue(ldt));
		assertEquals("1979-05-27T07:32:00", out);
	}

	@Test
	void h05_dateTimeLocalDate() throws Exception {
		var ld = LocalDate.of(1979, 5, 27);
		var out = run(w -> w.dateTimeValue(ld));
		assertEquals("1979-05-27", out);
	}

	@Test
	void h06_dateTimeLocalTime() throws Exception {
		var lt = LocalTime.of(7, 32, 0);
		var out = run(w -> w.dateTimeValue(lt));
		assertEquals("07:32:00", out);
	}

	@Test
	void h07_dateTimeOtherTemporalAccessor() throws Exception {
		// Year is a TemporalAccessor that doesn't match any specific branch — falls through to value.toString()
		var y = Year.of(2024);
		var out = run(w -> w.dateTimeValue(y));
		assertEquals("2024", out);
	}

	@Test
	void h08_dateTimeNonTemporal() throws Exception {
		var out = run(w -> w.dateTimeValue("2024-01-01"));
		assertEquals("2024-01-01", out);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// localDateTimeValue / localDateValue / localTimeValue
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_localDateTimeValueWithLDT() throws Exception {
		var ldt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
		assertEquals("2024-06-01T12:00:00", run(w -> w.localDateTimeValue(ldt)));
	}

	@Test
	void i02_localDateTimeValueWithNonLDT() throws Exception {
		assertEquals("hello", run(w -> w.localDateTimeValue("hello")));
	}

	@Test
	void i03_localDateValueWithLD() throws Exception {
		var ld = LocalDate.of(2024, 6, 1);
		assertEquals("2024-06-01", run(w -> w.localDateValue(ld)));
	}

	@Test
	void i04_localDateValueWithNonLD() throws Exception {
		assertEquals("hello", run(w -> w.localDateValue("hello")));
	}

	@Test
	void i05_localTimeValueWithLT() throws Exception {
		var lt = LocalTime.of(12, 0, 0);
		assertEquals("12:00:00", run(w -> w.localTimeValue(lt)));
	}

	@Test
	void i06_localTimeValueWithNonLT() throws Exception {
		assertEquals("hello", run(w -> w.localTimeValue("hello")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Structural tokens
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_arrayStartEnd() throws Exception {
		assertEquals("[]", run(w -> w.arrayStart().arrayEnd()));
	}

	@Test
	void j02_inlineTableStartEnd() throws Exception {
		assertEquals("{}", run(w -> w.inlineTableStart().inlineTableEnd()));
	}

	@Test
	void j03_comment() throws Exception {
		assertEquals("# hello\n", run(w -> w.comment("hello")));
	}

	@Test
	void j04_blankLine() throws Exception {
		assertEquals("\n", run(w -> w.blankLine()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// escapeBasicString
	//-----------------------------------------------------------------------------------------------------------------

	static Stream<Arguments> k01_escapeBasicStringCases() {
		return Stream.of(
			Arguments.of(null, ""),                                              // null input -> empty string
			Arguments.of("hello", "hello"),                                      // plain text passes through unchanged
			Arguments.of("\\\"\b\t\n\f\r", "\\\\\\\"\\b\\t\\n\\f\\r")             // all escapable control chars
		);
	}

	@ParameterizedTest
	@MethodSource("k01_escapeBasicStringCases")
	void k01_escapeBasicString(String input, String expected) throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertEquals(expected, w.escapeBasicString(input));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// isBareKey
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void l01_isBareKeyValid() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertTrue(w.isBareKey("simple"));
		assertTrue(w.isBareKey("with_underscore"));
		assertTrue(w.isBareKey("with-dash"));
		assertTrue(w.isBareKey("WithCaps"));
		assertTrue(w.isBareKey("with123digits"));
	}

	@Test
	void l02_isBareKeyInvalid() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertFalse(w.isBareKey(null));
		assertFalse(w.isBareKey(""));
		assertFalse(w.isBareKey("with space"));
		assertFalse(w.isBareKey("with.dot"));
		assertFalse(w.isBareKey("with$"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// SerializerWriter chain-override methods (return-type widening)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void m01_chainAppendChar() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append('x'));
		w.flush();
		assertEquals("x", sw.toString());
	}

	@Test
	void m02_chainAppendCharArray() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append(new char[]{'h','i'}));
		w.flush();
		assertEquals("hi", sw.toString());
	}

	@Test
	void m03_chainAppendIndentChar() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append(0, 'x'));
		w.flush();
		assertEquals("x", sw.toString());
	}

	@Test
	void m04_chainAppendIndentString() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append(0, "foo"));
		w.flush();
		assertEquals("foo", sw.toString());
	}

	@Test
	void m05_chainAppendObject() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append((Object) "obj"));
		w.flush();
		assertEquals("obj", sw.toString());
	}

	@Test
	void m06_chainAppendString() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.append("foo"));
		w.flush();
		assertEquals("foo", sw.toString());
	}

	@Test
	void m07_chainAppendIfChar() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.appendIf(true, 'x'));
		assertSame(w, w.appendIf(false, 'y'));
		w.flush();
		assertEquals("x", sw.toString());
	}

	@Test
	void m08_chainAppendIfString() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.appendIf(true, "yes"));
		assertSame(w, w.appendIf(false, "no"));
		w.flush();
		assertEquals("yes", sw.toString());
	}

	@Test
	void m09_chainAppendlnIndent() throws Exception {
		// Without useWhitespace=true, the trailing \n suppressed; just verify chain return.
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.appendln(0, "line"));
		w.flush();
		assertEquals("line", sw.toString());
	}

	@Test
	void m10_chainAppendln() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.appendln("line"));
		w.flush();
		assertEquals("line", sw.toString());
	}

	@Test
	void m23_chainAppendlnWithWhitespace() throws Exception {
		// With useWhitespace=true, the trailing \n is emitted.
		var sw = new StringWriter();
		var w = new TomlWriter(sw, true, 10, false,
			UriResolver.of(UriResolution.NONE, UriRelativity.RESOURCE, UriContext.DEFAULT));
		assertSame(w, w.appendln("line"));
		w.flush();
		assertEquals("line\n", sw.toString());
	}

	@Test
	void m11_chainAppendUri() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.appendUri("http://example.com"));
		w.flush();
		assertNotNull(sw.toString());
	}

	@Test
	void m12_chainCr() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.cr(2));
	}

	@Test
	void m13_chainCre() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.cre(2));
	}

	@Test
	void m14_chainI() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.i(2));
	}

	@Test
	void m15_chainIe() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.ie(2));
	}

	@Test
	void m16_chainNl() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.nl(0));
	}

	@Test
	void m17_chainNlIf() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.nlIf(true, 0));
		assertSame(w, w.nlIf(false, 0));
	}

	@Test
	void m18_chainQ() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.q());
	}

	@Test
	void m19_chainS() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.s());
	}

	@Test
	void m20_chainSIf() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.sIf(true));
		assertSame(w, w.sIf(false));
	}

	@Test
	void m21_chainWChar() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.w('x'));
		w.flush();
		assertEquals("x", sw.toString());
	}

	@Test
	void m22_chainWString() throws Exception {
		var sw = new StringWriter();
		var w = newWriter(sw, false);
		assertSame(w, w.w("foo"));
		w.flush();
		assertEquals("foo", sw.toString());
	}
}
