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
package org.apache.juneau.marshall.ini;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link IniParserSession} targeting branches not already exercised by
 * {@link IniParser_Test} / {@link IniRoundTrip_Test}:
 *  - {@code doRead}'s null-{@code Reader} short-circuit (real {@code r == null} arm, triggered by a null
 *    parse input).
 *  - {@code readIniContent}'s "reader is already a {@link BufferedReader}" reuse arm (only reachable by
 *    calling the {@code protected} method directly with a real {@link BufferedReader}, since the public
 *    read path always wraps input in a {@code ParserReader}).
 *  - {@code populateBean}/{@code buildMapFromSections}'s multi-level section recursion (grandchild sections,
 *    {@code cMeta.isMap()} nested-map-property arm, and silently-ignored unmatched nested sections -- which
 *    is asymmetric with the root-property loop's throw-on-unknown default).
 *  - {@code readValue}'s date/calendar/temporal/duration/period dispatch arms, and {@code '}-quoted-string
 *    edge cases (unterminated quote, bare single quote).
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to Map/bean in tests.
})
class IniParserSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - doRead: real null-Reader short-circuit.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_nullInputReturnsNull() throws Exception {
		// ParserPipe.getParserReader() returns null when the raw input object itself is null, which is the
		// actual (only) way doRead's "r == null" arm is reachable -- not empty-string input (readIniContent
		// always pre-seeds a "" default section, so a `sections.isEmpty()` check would never be true;
		// this was confirmed, along with the now-deleted dead guard that followed it in doRead).
		var result = IniParser.DEFAULT.read((Object) null, Map.class);
		assertNull(result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - readIniContent: reader-is-already-a-BufferedReader reuse arm. Only reachable via the protected
	// method directly (same package), since pipe.getParserReader() always yields a ParserReader, never a
	// literal BufferedReader, on the public read(...) path.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_readIniContent_bufferedReaderReused() throws Exception {
		var session = IniParserSession.create(IniParser.DEFAULT).build();
		var sections = session.readIniContent(new BufferedReader(new StringReader("a=1\n[sec]\nb=2")));
		assertEquals(2, sections.size());
		assertEquals("1", sections.get("").get("a"));
		assertEquals("2", sections.get("sec").get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - populateBean: grandchild-section recursion (non-empty sectionPath arm of isChild), nested Map
	// bean-property section, and silently-ignored unmatched nested sections.
	//------------------------------------------------------------------------------------------------------------------

	public static class C_Company {
		public String name;
		public String ticker;
	}

	public static class C_Employment {
		public String title;
		public C_Company company;
	}

	public static class C_Employee {
		public String name;
		public C_Employment employment;
		public Map<String,Object> extra;
	}

	@Test void c01_grandchildSectionRoundTrip() throws Exception {
		var e = new C_Employee();
		e.name = "John";
		e.employment = new C_Employment();
		e.employment.title = "Engineer";
		e.employment.company = new C_Company();
		e.employment.company.name = "Acme";
		e.employment.company.ticker = "ACME";
		var ini = IniSerializer.DEFAULT.write(e);
		var result = IniParser.DEFAULT.read(ini, C_Employee.class);
		assertEquals("John", result.name);
		assertEquals("Engineer", result.employment.title);
		assertEquals("Acme", result.employment.company.name);
		assertEquals("ACME", result.employment.company.ticker);
	}

	@Test void c02_nestedMapBeanPropertyRoundTrip() throws Exception {
		var e = new C_Employee();
		e.name = "Jane";
		var extra = new LinkedHashMap<String,Object>();
		extra.put("level", "senior");
		extra.put("years", 5);
		e.extra = extra;
		var ini = IniSerializer.DEFAULT.write(e);
		var result = IniParser.DEFAULT.read(ini, C_Employee.class);
		assertEquals("Jane", result.name);
		assertNotNull(result.extra);
		assertEquals("senior", result.extra.get("level"));
		// valueType here is Object.class (Map<String,Object>'s declared value type), and readValue only
		// special-cases numeric strings when the *declared* target type isNumber() -- an Object-typed map
		// value leaves numeric-looking strings as plain Strings (matches root-level Map<String,Object>
		// reads, e.g. IniParser_Test.a05_numbersAndBooleans, which round-trips via assertBean's
		// string-normalized comparison and so never surfaces this distinction).
		assertEquals("5", result.extra.get("years"));
	}

	public static class C_G_D {
		public String e;
	}

	public static class C_G_C {
		public C_G_D d;
	}

	public static class C_G_B {
		public C_G_C c;
	}

	public static class C_G_A {
		public C_G_B b;
	}

	@Test void c02b_intermediateSectionHeaderMissing_defaultSectionNull() throws Exception {
		// "[b]" exists as its own (empty) header, but "[b/c]" is skipped entirely -- only "[b/c/d]" is
		// present. When populateBean recurses into sectionPath "b/c" (derived by splitting "b/c/d"),
		// sections.get("b/c") is null (no such literal header), hitting defaultSection's null arm.
		var ini = "[b]\n\n[b/c/d]\ne = hi";
		var result = IniParser.DEFAULT.read(ini, C_G_A.class);
		assertNotNull(result.b);
		assertNotNull(result.b.c);
		assertNotNull(result.b.c.d);
		assertEquals("hi", result.b.c.d.e);
	}

	@Test void c03_unmatchedNestedSectionSilentlyIgnored() throws Exception {
		// Unlike the root-property loop (which throws ParseException for an unknown key -- see
		// IniParser_Test.b04_unknownBeanPropertyThrows), populateBean's nested-SECTION loop silently
		// continues past any section name that doesn't match a bean property, regardless of
		// ignoreUnknownBeanProperties(). Pin this asymmetry (flagged, not "fixed", per task scope).
		var ini = "name = John\n\n[bogus]\nx = 1";
		var result = IniParser.DEFAULT.read(ini, C_Employee.class);
		assertEquals("John", result.name);
	}

	@Test void c04_unmatchedNestedSection_ignoreUnknownBeanPropertiesTrue_alsoSilentlyIgnored() throws Exception {
		// Same unmatched-section scenario as c03, but with ignoreUnknownBeanProperties(true) explicitly
		// set -- hits the `pMeta == null && isIgnoreUnknownBeanProperties()` true/true combo directly
		// (c03 only exercises pMeta == null with the flag false).
		var p = IniParser.create().ignoreUnknownBeanProperties().build();
		var ini = "name = John\n\n[bogus]\nx = 1";
		var result = p.read(ini, C_Employee.class);
		assertEquals("John", result.name);
	}

	public static class C05_Bean {
		public String employment;
	}

	@Test void c05_sectionHeaderOnScalarProperty_cMetaIsMapFalse_throwsParseException() throws Exception {
		// FIXED: "employment" resolves to a real (non-null) pMeta, but its type is a plain String -- neither
		// isBean() nor isMap() -- so the isBean()/isMap() if/else-if chain used to fall through with no
		// assignment, silently dropping the section's data rather than erroring. Unlike an unmatched section
		// name (c03/c04), this is a genuine type mismatch on a KNOWN property, so it now throws instead.
		var ini = "\n[employment]\ntitle = Engineer";
		assertThrowsWithMessage(ParseException.class, "Cannot populate property 'employment'",
			() -> IniParser.DEFAULT.read(ini, C05_Bean.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - buildMapFromSections: hasNested recursion, dedup (containsKey) guard, and a section with no
	// matching header line (childSection == null, i.e. only reachable via a nested-only path with no direct
	// key/values of its own).
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_hasNestedRecursionIntoMapTarget() throws Exception {
		var ini = "name = John\n\n[employment]\ntitle = Engineer\n\n[employment/company]\nname = Acme\nticker = ACME";
		var result = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertEquals("John", result.get("name"));
		var employment = (Map<String,Object>) result.get("employment");
		assertEquals("Engineer", employment.get("title"));
		var company = (Map<String,Object>) employment.get("company");
		assertEquals("Acme", company.get("name"));
		assertEquals("ACME", company.get("ticker"));
	}

	@Test void d02_grandchildOnlySectionWithNoOwnHeaderIsRecovered() throws Exception {
		// FIXED: buildMapFromSections's isChild check used to only ever recognize a section as a "direct
		// child" of sectionPath by matching literal keys already present in the sections map. A grandchild
		// section like "employment/company" was never itself a direct child of "" (it contains
		// SECTION_PATH_DELIMITER), so if there's no separate literal "[employment]" header line,
		// "employment" was never generated as a childName candidate at the root level -- the whole
		// "employment/company" subtree was silently dropped from the result, even though the input
		// unambiguously specifies it. Any non-root section is a descendant of "" by definition, so the root
		// level now recognizes ALL sections as candidate children, deriving the immediate childName by
		// splitting on the path delimiter (matching the non-root arm's existing behavior).
		var ini = "name = John\n\n[employment/company]\nname = Acme";
		var result = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertEquals("John", result.get("name"));
		var employment = (Map<String,Object>) result.get("employment");
		assertNotNull(employment);
		var company = (Map<String,Object>) employment.get("company");
		assertEquals("Acme", company.get("name"));
	}

	@Test void d04_grandchildSectionHeaderMissing_defaultSectionNullAndDedup() throws Exception {
		// Three-level structure with the *middle* level's own header ("[a/b]") never literally present --
		// only "[a]" and two great-grandchild-ish entries "[a/b/x]" / "[a/b/y]" exist. When
		// buildMapFromSections recurses into the synthesized "a/b" path (derived from splitting
		// "a/b/x"/"a/b/y"), sections.get("a/b") is null (no such literal header), hitting defaultSection's
		// null arm. Then "a/b/y" resolves to the SAME childName ("b") as "a/b/x" did, already present in
		// result -- hitting the containsKey() dedup arm too.
		var ini = "[a]\n\n[a/b/x]\nval = 1\n\n[a/b/y]\nval = 2";
		var result = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		var a = (Map<String,Object>) result.get("a");
		var b = (Map<String,Object>) a.get("b");
		var x = (Map<String,Object>) b.get("x");
		var y = (Map<String,Object>) b.get("y");
		assertEquals("1", x.get("val"));
		assertEquals("2", y.get("val"));
	}

	@Test void d03_duplicateChildNameDeduped() throws Exception {
		// Two section headers that both resolve to the same immediate childName ("a") under the root --
		// e.g. "[a]" (processed first from its own key/values) and "[a/b]" (a grandchild under it). The
		// second time "a" is encountered as a candidate childName, result.containsKey("a") is already true,
		// so buildMapFromSections's dedup guard skips reprocessing it.
		var ini = "[a]\nx = 1\n\n[a/b]\ny = 2";
		var result = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		var a = (Map<String,Object>) result.get("a");
		assertEquals("1", a.get("x"));
		var b = (Map<String,Object>) a.get("b");
		assertEquals("2", b.get("y"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - Date / Calendar / Temporal / Duration / Period bean-property round trips.
	//
	// NOTE ON readValue's isDate()/isCalendar()/isTemporal()/isDuration()/isPeriod() arms: these look
	// reachable from populateBean's `readValue(rawValue, (ClassMeta<?>) pMeta.getBeanInfo())` call for a
	// Date/Calendar/Temporal/Duration/Period-typed bean property, but they are NOT -- confirmed by direct
	// reflection probing of BeanPropertyMeta -- because these types get a default post-processor-installed
	// swap, and BeanPropertyMeta#getBeanInfo() (backed by the swap-aware `typeMeta` field, distinct from the
	// unswapped `rawTypeMeta`) then reports the property as plain java.lang.Object rather than as
	// Date/Calendar/isTemporal()/etc-classified. (A plain numeric property like `int`/`double` has no such
	// swap, so isNumber() *is* reachable and already covered by IniParser_Test.a05.) The actual date-ish
	// conversion for these properties happens later via BeanMap#put()'s own type-aware coercion, which is
	// swap-aware -- NOT via these specific readValue lines. Since buildMapFromSections's two call sites
	// always pass object() too, these 10 lines are dead from every call site in this class; see the "// HTT"
	// markers on them. These round-trip tests are kept regardless since they're genuine regression coverage
	// of Date/Calendar/Temporal/Duration/Period bean properties end-to-end (exercising the real
	// swap-driven conversion path instead).
	//------------------------------------------------------------------------------------------------------------------

	public static class E01_DateBean {
		public Date date;
	}

	@Test void e01_dateRoundTrip() throws Exception {
		var b = new E01_DateBean();
		b.date = new Date(0);
		var ini = IniSerializer.DEFAULT.write(b);
		var result = IniParser.DEFAULT.read(ini, E01_DateBean.class);
		assertEquals(b.date, result.date);
	}

	public static class E02_CalendarBean {
		public Calendar cal;
	}

	@Test void e02_calendarRoundTrip() throws Exception {
		var b = new E02_CalendarBean();
		b.cal = new GregorianCalendar(2024, Calendar.MARCH, 15);
		var ini = IniSerializer.DEFAULT.write(b);
		var result = IniParser.DEFAULT.read(ini, E02_CalendarBean.class);
		assertNotNull(result.cal);
		assertEquals(b.cal.get(Calendar.YEAR), result.cal.get(Calendar.YEAR));
		assertEquals(b.cal.get(Calendar.MONTH), result.cal.get(Calendar.MONTH));
		assertEquals(b.cal.get(Calendar.DAY_OF_MONTH), result.cal.get(Calendar.DAY_OF_MONTH));
	}

	public static class E03_TemporalBean {
		public LocalDate localDate;
	}

	@Test void e03_temporalRoundTrip() throws Exception {
		var b = new E03_TemporalBean();
		b.localDate = LocalDate.of(2024, 3, 15);
		var ini = IniSerializer.DEFAULT.write(b);
		var result = IniParser.DEFAULT.read(ini, E03_TemporalBean.class);
		assertEquals(b.localDate, result.localDate);
	}

	public static class E04_DurationBean {
		public Duration dur;
	}

	@Test void e04_durationRoundTrip() throws Exception {
		var b = new E04_DurationBean();
		b.dur = Duration.ofMinutes(90);
		var ini = IniSerializer.DEFAULT.write(b);
		var result = IniParser.DEFAULT.read(ini, E04_DurationBean.class);
		assertEquals(b.dur, result.dur);
	}

	public static class E05_PeriodBean {
		public Period per;
	}

	@Test void e05_periodRoundTrip() throws Exception {
		var b = new E05_PeriodBean();
		b.per = Period.of(1, 2, 3);
		var ini = IniSerializer.DEFAULT.write(b);
		var result = IniParser.DEFAULT.read(ini, E05_PeriodBean.class);
		assertEquals(b.per, result.per);
	}

	public static class E06_NumBean {
		public int count;
	}

	@Test void e06_numberFormatExceptionFallsBackToConvertToMemberType() throws Exception {
		// "0x10" fails both Long.parseLong and Double.parseDouble (NumberFormatException), so readValue
		// falls back to convertToMemberType, which DOES understand the hex-literal convention.
		var result = IniParser.DEFAULT.read("count = 0x10", E06_NumBean.class);
		assertEquals(16, result.count);
	}

	public static class E07_DoubleBean {
		public double val;
	}

	@Test void e07_decimalPointTriggersDoubleParseDouble() throws Exception {
		// trimmed.contains(".") is true -> Double.parseDouble path (rather than Long.parseLong).
		var result = IniParser.DEFAULT.read("val = 3.5", E07_DoubleBean.class);
		assertEquals(3.5, result.val);
	}

	@Test void e08_scientificNotationTriggersDoubleParseDouble() throws Exception {
		// No "." present, but contains("e") (case-insensitive) is true -> Double.parseDouble path.
		var result = IniParser.DEFAULT.read("val = 1E3", E07_DoubleBean.class);
		assertEquals(1000.0, result.val);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x - readValue: '-quoted-string edge cases that fall through the "startsWith('\'') && endsWith('\'') &&
	// length >= 2" guard rather than satisfying it.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_bareApostropheNotTreatedAsQuoted() throws Exception {
		// startsWith("'") and endsWith("'") are both true (same single character satisfies both ends), but
		// length is 1, so the "length >= 2" conjunct is false and the quoted-string branch is skipped.
		var ini = "a = '";
		var m = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertEquals("'", m.get("a"));
	}

	@Test void f02_unterminatedQuoteNotTreatedAsQuoted() throws Exception {
		// startsWith("'") is true but endsWith("'") is false -- falls through to plain-string handling.
		var ini = "a = 'unterminated";
		var m = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertEquals("'unterminated", m.get("a"));
	}

	@Test void f03a_leadingSeparatorNotTreatedAsKeyValue() throws Exception {
		// first == '=' -> splitKeyValue short-circuits to null regardless of the configured kvSeparator
		// (the leading-char guard is hardcoded to '=', not `ctx.kvSeparator`) -- the line is dropped.
		var ini = "=noKeyHere";
		var m = (Map<String,Object>) IniParser.DEFAULT.read(ini, Map.class, String.class, Object.class);
		assertTrue(m.isEmpty(), m.toString());
	}

	@Test void f03_customKvSeparator_missingSeparatorNotTreatedAsKeyValue() throws Exception {
		// idx < 0 (separator char not found) && sep != '=' -> the `':'`-fallback (lastIndexOf(':')) is
		// skipped entirely, idx stays -1, and idx < 1 returns null -- the line is dropped, not an error.
		var p = IniParser.create().kvSeparator('|').build();
		var m = (Map<String,Object>) p.read("keyonly", Map.class, String.class, Object.class);
		assertTrue(m.isEmpty(), m.toString());
	}
}
