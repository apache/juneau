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
import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link IniSerializerSession} targeting branches not already exercised by
 * {@link IniSerializer_Test} / {@link IniRoundTrip_Test}:
 *  - {@code doWrite}'s null-root short-circuit and typeName resolution (registered dictionary name vs. simple-name
 *    fallback).
 *  - {@code getIniWriter}'s "raw output is already an {@link IniWriter}" reuse arm.
 *  - {@code writeBean}'s {@code @Ini(section=...)}/{@code @Ini(json5Encoding=true)} overrides, null section-property
 *    values (with/without {@code keepNullProperties}), and nested-section whitespace/comment combinations.
 *  - {@code writeMapAtRoot}/{@code writeMapSection}'s "mixed" (non-simple) map routing and whitespace before nested
 *    sections.
 *  - {@code formatSimpleValue}'s URI/Calendar/Duration/Period arms and {@code needsQuoting}'s edge cases.
 *  - {@code isSimpleMap}'s non-{@link CharSequence} key and bean/map/collection value rejection arms.
 */
class IniSerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a0x - doWrite: null root short-circuit, addRootType()-only combo, and typeName resolution.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_nullRootShortCircuits() {
		var ini = IniSerializer.DEFAULT.write(null);
		assertEquals("", ini);
	}

	@Test void a02_addRootTypeOnly_noAddBeanTypes() {
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "test");
		var s = IniSerializer.create().addRootType().build();
		var ini = s.write(m);
		assertTrue(ini.contains("name") && ini.contains("test"));
	}

	@Marshalled(typeName="A01")
	public static class A03_Bean {
		public String name;
	}

	@Test void a03_typeNameResolvedFromDictionary() {
		var b = new A03_Bean();
		b.name = "x";
		var s = IniSerializer.create().addBeanTypes().addRootType().beanDictionary(A03_Bean.class).build();
		var ini = s.write(b);
		assertTrue(ini.contains("A01"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - getIniWriter: raw output already an IniWriter -> reused directly (mirrors Json5SerializerSession's
	// analogous branch).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_getIniWriter_alreadyIniWriter() throws Exception {
		var sw = new StringWriter();
		try (var iw = new IniWriter(sw, false, 0, false, '=', false, null)) {
			var m = new LinkedHashMap<String,Object>();
			m.put("a", "1");
			IniSerializer.DEFAULT.write(m, iw);
			assertTrue(sw.toString().contains("a='1'"));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - @Ini(section=...) custom section name override (bean property AND root-map entry).
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_Outer {
		@Ini(section="custom")
		public C01_Inner db;
	}

	public static class C01_Inner {
		public String host;
	}

	@Test void c01_customSectionName_beanProperty() {
		var o = new C01_Outer();
		o.db = new C01_Inner();
		o.db.host = "localhost";
		var ini = IniSerializer.DEFAULT.write(o);
		assertTrue(ini.contains("[custom]"), ini);
		assertFalse(ini.contains("[db]"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - @Ini(json5Encoding=true) forces inline JSON5 encoding for a value that would otherwise become its own
	// [section] (a nested bean).
	//------------------------------------------------------------------------------------------------------------------

	public static class D01_Outer {
		@Ini(json5Encoding=true)
		public D01_Inner inline;
	}

	public static class D01_Inner {
		public String host;
	}

	@Test void d01_json5EncodingForcesInline() {
		var o = new D01_Outer();
		o.inline = new D01_Inner();
		o.inline.host = "localhost";
		var ini = IniSerializer.DEFAULT.write(o);
		assertFalse(ini.contains("["), ini);
		assertTrue(ini.contains("host"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - null-valued section-typed bean properties: dropped by default, emitted (with comment) when
	// keepNullProperties() is set.
	//------------------------------------------------------------------------------------------------------------------

	public static class E01_Outer {
		@Ini(comment="A nested section.")
		public E01_Inner db;
	}

	public static class E01_Inner {
		public String host;
	}

	@Test void e01_nullSectionProperty_droppedByDefault() {
		var o = new E01_Outer();
		var ini = IniSerializer.DEFAULT.write(o);
		assertFalse(ini.contains("db"), ini);
	}

	@Test void e02_nullSectionProperty_keptWithComment() {
		var o = new E01_Outer();
		var s = IniSerializer.create().keepNullProperties().useComments().build();
		var ini = s.write(o);
		assertTrue(ini.contains("db"), ini);
		assertTrue(ini.contains("# A nested section."), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x - useWhitespace() blank line before a nested (non-root) [section] header.
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Root {
		public String name;
		public F01_Mid mid;
	}

	public static class F01_Mid {
		public String label;
		public F01_Leaf leaf;
	}

	public static class F01_Leaf {
		public String value;
	}

	@Test void f01_whitespaceBeforeNestedSection() {
		var r = new F01_Root();
		r.name = "root";
		r.mid = new F01_Mid();
		r.mid.label = "mid";
		r.mid.leaf = new F01_Leaf();
		r.mid.leaf.value = "deep";
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(r);
		assertTrue(ini.contains("[mid]"), ini);
		assertTrue(ini.contains("[mid/leaf]"), ini);
		// A blank line must precede the nested (non-root) [mid/leaf] section header.
		assertTrue(ini.contains("\n\n[mid/leaf]"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g0x - writeMapAtRoot / writeMapSection: "mixed" (non-simple) map value falls back to an inline JSON5-encoded
	// key-value pair instead of a [section], both at root and nested one level down.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_mixedMapAtRoot_inlineFallback() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		var nested = new LinkedHashMap<String,Object>();
		nested.put("db", inner);
		var m = new LinkedHashMap<String,Object>();
		m.put("config", nested);
		var ini = IniSerializer.DEFAULT.write(m);
		assertFalse(ini.contains("[config]"), ini);
		assertTrue(ini.contains("config"), ini);
	}

	public static class G02_Outer {
		public String name;
		public Map<String,Object> settings;
	}

	@Test void g02_mixedMapAsSectionProperty_inlineFallbackWithComment() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		var mixed = new LinkedHashMap<String,Object>();
		mixed.put("nested", inner);
		var o = new G02_Outer();
		o.name = "x";
		o.settings = mixed;
		var s = IniSerializer.create().useComments().build();
		var ini = s.write(o);
		assertFalse(ini.contains("[settings]"), ini);
		assertTrue(ini.contains("settings"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h0x - formatSimpleValue: URI, Calendar, Duration, and Period value arms (Date/enum/Temporal already covered by
	// IniSerializer_Test.b01/b02).
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_uriValue() {
		var m = new LinkedHashMap<String,Object>();
		m.put("link", URI.create("http://example.com"));
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("http://example.com"), ini);
	}

	@Test void h02_calendarValue() {
		var m = new LinkedHashMap<String,Object>();
		var cal = new GregorianCalendar(2024, Calendar.MARCH, 15);
		m.put("cal", cal);
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("2024"), ini);
	}

	@Test void h03_durationValue() {
		var m = new LinkedHashMap<String,Object>();
		m.put("d", Duration.ofMinutes(90));
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("PT1H30M") || ini.contains("PT90M"), ini);
	}

	@Test void h04_periodValue() {
		var m = new LinkedHashMap<String,Object>();
		m.put("p", Period.of(1, 2, 3));
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("P1Y2M3D"), ini);
	}

	@Test void h05_utilDateValue() {
		var m = new LinkedHashMap<String,Object>();
		m.put("d", new Date(0));
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("d"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i0x - needsQuoting edge cases: leading/trailing-whitespace strings and structural characters.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_needsQuoting_leadingTrailingWhitespace() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "  padded  ");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'  padded  '"), ini);
	}

	@Test void i02_needsQuoting_structuralChars() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "a=b");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'a=b'"), ini);
	}

	@Test void i03_needsQuoting_bracketChars() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "a[b]");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'a[b]'"), ini);
	}

	@Test void i03b_needsQuoting_closeBracketOnly() {
		// "]" without a preceding "[" -- the "[" check short-circuits false, so this is the only way to
		// exercise the "]" contains() check's true branch on its own.
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "a]b");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'a]b'"), ini);
	}

	@Test void i04_needsQuoting_hashChar() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "a#b");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'a#b'"), ini);
	}

	@Test void i05_needsQuoting_emptyString() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("''"), ini);
	}

	@Test void i06_needsQuoting_literalNullTrueFalseStrings() {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", "null");
		m.put("b", "true");
		m.put("c", "FALSE");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'null'"), ini);
		assertTrue(ini.contains("'true'"), ini);
		assertTrue(ini.contains("'FALSE'"), ini);
	}

	@Test void i07_needsQuoting_embeddedNewline() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "line1\nline2");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'line1\nline2'"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// j0x - isSimpleMap: non-CharSequence key, and a bean/map/collection-typed value, both force the "mixed" (not a
	// [section]) inline-fallback path.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_nonCharSequenceKey_forcesInlineFallback() {
		var m = new LinkedHashMap<Object,Object>();
		m.put(1, "one");
		var outer = new LinkedHashMap<String,Object>();
		outer.put("nums", m);
		var ini = IniSerializer.DEFAULT.write(outer);
		assertFalse(ini.contains("[nums]"), ini);
		assertTrue(ini.contains("nums"), ini);
	}

	@Test void j02_beanValuedMapEntry_forcesInlineFallback() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("bean", new C01_Inner());
		var outer = new LinkedHashMap<String,Object>();
		outer.put("wrap", inner);
		var ini = IniSerializer.DEFAULT.write(outer);
		assertFalse(ini.contains("[wrap]"), ini);
		assertTrue(ini.contains("wrap"), ini);
	}

	@Test void j03_collectionValuedMapEntry_forcesInlineFallback() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("list", List.of("a", "b"));
		var outer = new LinkedHashMap<String,Object>();
		outer.put("wrap", inner);
		var ini = IniSerializer.DEFAULT.write(outer);
		assertFalse(ini.contains("[wrap]"), ini);
		assertTrue(ini.contains("wrap"), ini);
	}

	public static class J04_Bean {
		public Map<Integer,String> byId;
	}

	@Test void j04_typedNonCharSequenceKeyType_forcesInlineFallback() {
		var b = new J04_Bean();
		var m = new LinkedHashMap<Integer,String>();
		m.put(1, "one");
		b.byId = m;
		var ini = IniSerializer.DEFAULT.write(b);
		assertFalse(ini.contains("[byId]"), ini);
		assertTrue(ini.contains("byId"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// k0x - writeMapAtRoot: root-level bean value (own [section]) and simple nested-map value, both with
	// useWhitespace() to exercise the pre-section blank-line branches.
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_rootMapWithBeanValue_whitespace() {
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", new C01_Inner());
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(outer);
		assertTrue(ini.contains("[db]"), ini);
	}

	@Test void k02_rootMapWithSimpleNestedMap_whitespace() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", inner);
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(outer);
		assertTrue(ini.contains("[db]"), ini);
		assertTrue(ini.contains("host"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l0x - writeBean's "sections" pass: a non-null, non-bean/map/collection-or-array section-typed property value
	// (e.g. a streamable Reader/InputStream) used to fall through every isBean()/isMap()/isCollection()-or-isArray()
	// arm with no else, silently dropping it -- FIXED by adding a fallback else arm that writes it as an inline
	// key-value, same as the (unreachable) collection/array arm already did.
	//------------------------------------------------------------------------------------------------------------------

	public static class L01_Bean {
		public String name;
		public java.util.stream.Stream<String> notes;
	}

	@Test void l01_streamableSectionProperty_writtenAsInlineKeyValue() {
		// A Stream property is neither bean, map, nor collection/array, so it's classified into "sections"
		// (not "simple", since isSimpleOrJson5Inline() returns false for isStreamable() types) but now hits the
		// new fallback else arm in the sections pass, which writes it out as an inline JSON5-encoded key-value
		// rather than silently dropping it.
		var b = new L01_Bean();
		b.name = "x";
		b.notes = java.util.stream.Stream.of("a", "b");
		var ini = IniSerializer.DEFAULT.write(b);
		assertTrue(ini.contains("name"), ini);
		assertTrue(ini.contains("notes"), ini);
		assertTrue(ini.contains("'a'") && ini.contains("'b'"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l1x - writeBean's mixed-map "else" arm with a comment annotation AND useComments() both true.
	//------------------------------------------------------------------------------------------------------------------

	public static class L02_Outer {
		@Ini(comment="Mixed settings.")
		public Map<String,Object> settings;
	}

	@Test void l02_mixedMapSectionProperty_commentAndUseCommentsBothTrue() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		var mixed = new LinkedHashMap<String,Object>();
		mixed.put("nested", inner);
		var o = new L02_Outer();
		o.settings = mixed;
		var s = IniSerializer.create().useComments().build();
		var ini = s.write(o);
		assertTrue(ini.contains("# Mixed settings."), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l2x - writeBean's null-section-property-with-keepNullProperties useComments/comment combinations not covered
	// by e02 (which has both useComments() and a comment set).
	//------------------------------------------------------------------------------------------------------------------

	@Test void l03_nullSectionProperty_keptWithoutUseComments() {
		var o = new E01_Outer();
		var s = IniSerializer.create().keepNullProperties().build();
		var ini = s.write(o);
		assertTrue(ini.contains("db"), ini);
		assertFalse(ini.contains("#"), ini);
	}

	public static class L04_Outer {
		public E01_Inner db;
	}

	@Test void l04_nullSectionProperty_useCommentsButNoCommentAnnotation() {
		var o = new L04_Outer();
		var s = IniSerializer.create().keepNullProperties().useComments().build();
		var ini = s.write(o);
		assertTrue(ini.contains("db"), ini);
		assertFalse(ini.contains("#"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l3x - writeMapAtRoot: null-valued root map entries, dropped by default and kept with keepNullProperties().
	//------------------------------------------------------------------------------------------------------------------

	@Test void l05_rootMapNullValue_droppedByDefault() {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", null);
		var ini = IniSerializer.DEFAULT.write(m);
		assertFalse(ini.contains("a"), ini);
	}

	@Test void l06_rootMapNullValue_keptWithKeepNullProperties() {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", null);
		var s = IniSerializer.create().keepNullProperties().build();
		var ini = s.write(m);
		assertTrue(ini.contains("a"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l4x - writeMapAtRoot: root-level bean value WITHOUT useWhitespace() (no blank line branch).
	//------------------------------------------------------------------------------------------------------------------

	@Test void l07_rootMapWithBeanValue_noWhitespace() {
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", new C01_Inner());
		var ini = IniSerializer.DEFAULT.write(outer);
		assertTrue(ini.contains("[db]"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l5x - writeMapSection: null-valued nested-section-map entries, dropped by default and kept with
	// keepNullProperties().
	//------------------------------------------------------------------------------------------------------------------

	@Test void l08_nestedSectionMapNullValue_droppedByDefault() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		inner.put("port", null);
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", inner);
		var ini = IniSerializer.DEFAULT.write(outer);
		assertTrue(ini.contains("host"), ini);
		assertFalse(ini.contains("port"), ini);
	}

	@Test void l09_nestedSectionMapNullValue_keptWithKeepNullProperties() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("host", "localhost");
		inner.put("port", null);
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", inner);
		var s = IniSerializer.create().keepNullProperties().build();
		var ini = s.write(outer);
		assertTrue(ini.contains("port"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l6x - writeKeyValue: registered ObjectSwap whose swap-class-meta resolves to Object -> re-resolved via
	// getClassMetaForObject(value).
	//------------------------------------------------------------------------------------------------------------------

	// UUID is not bean-shaped (isBean() == false, handled via toString() fallback by default), so a swap
	// registered against it is actually consulted by writeKeyValue -- unlike a swap on a bean-shaped class,
	// where isBean() intercepts before the swap is ever considered (see writeMapAtRoot/writeBean's isBean()
	// checks, which run ahead of any swap logic).

	public static class L10_Swap extends org.apache.juneau.marshall.swap.ObjectSwap<UUID,Object> {
		@Override
		public Object swap(MarshallingSession session, UUID o) {
			return o == null ? null : o.toString();
		}
	}

	@Test void l10_swapToObjectClassMeta_reResolvedFromRuntimeValue() {
		// Swap-class-meta is Object -> isObject() true -> re-resolved from the runtime (String) value.
		var m = new LinkedHashMap<String,Object>();
		m.put("k", UUID.fromString("00000000-0000-0000-0000-000000000001"));
		var s = IniSerializer.create().swaps(L10_Swap.class).build();
		var ini = s.write(m);
		assertTrue(ini.contains("00000000-0000-0000-0000-000000000001"), ini);
	}

	public static class L10b_Swap extends org.apache.juneau.marshall.swap.ObjectSwap<UUID,String> {
		@Override
		public String swap(MarshallingSession session, UUID o) {
			return o == null ? null : o.toString();
		}
	}

	@Test void l10b_swapToConcreteNonObjectType_noReResolve() {
		// Swap-class-meta is String (concrete), not Object -> isObject() false, skipping the re-resolve branch.
		var m = new LinkedHashMap<String,Object>();
		m.put("k", UUID.fromString("00000000-0000-0000-0000-000000000002"));
		var s = IniSerializer.create().swaps(L10b_Swap.class).build();
		var ini = s.write(m);
		assertTrue(ini.contains("00000000-0000-0000-0000-000000000002"), ini);
	}

	public static class L10c_Swap extends org.apache.juneau.marshall.swap.ObjectSwap<UUID,String> {
		@Override
		public String swap(MarshallingSession session, UUID o) {
			return null;
		}
	}

	@Test void l10c_swapReturningNull_droppedWithoutKeepNullProperties() {
		// Swap converts a non-null input to null; downstream `value == null && !isKeepNullProperties()`
		// short-circuits the property entirely (default settings, no keepNullProperties()).
		var m = new LinkedHashMap<String,Object>();
		m.put("k", UUID.fromString("00000000-0000-0000-0000-000000000003"));
		var s = IniSerializer.create().swaps(L10c_Swap.class).build();
		var ini = s.write(m);
		assertFalse(ini.contains("k"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l7x - needsQuoting: a value that is purely numeric (hits the matchNumberPrefix branch directly, distinct from
	// the structural-character and whitespace checks already covered).
	//------------------------------------------------------------------------------------------------------------------

	@Test void l11_needsQuoting_numericLookingString() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", "12345");
		var ini = IniSerializer.DEFAULT.write(m);
		assertTrue(ini.contains("'12345'"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l8x - isSimpleMap: a null value within an otherwise-simple map is tolerated (still "simple").
	// Note: a literal null *key* is NOT exercised here -- isSimpleMap() itself tolerates it (line 352's
	// `k != null && ...` guard), but writeKeyValue()/IniWriter.keyValue() downstream do not handle a null key
	// (NoCloseWriter.write() rejects a null string), so map.put(null, ...) at this level throws. Flagging as a
	// separate latent bug rather than fixing it here.
	//------------------------------------------------------------------------------------------------------------------

	@Test void l12_simpleMapWithNullValue_stillSimple() {
		var inner = new LinkedHashMap<String,Object>();
		inner.put("k1", "v1");
		inner.put("k2", null);
		var outer = new LinkedHashMap<String,Object>();
		outer.put("db", inner);
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(outer);
		assertTrue(ini.contains("[db]"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l9x - isSimpleMap: raw (untyped) map key type and an explicitly Object-typed key -- both take the
	// runtime-key-check path instead of the CharSequence-key-type short-circuit.
	//------------------------------------------------------------------------------------------------------------------

	public static class L13_Bean {
		public Map<Object,String> byObj;
	}

	@Test void l13_objectTypedMapKey_simpleMapRuntimeCheck() {
		var b = new L13_Bean();
		var m = new LinkedHashMap<Object,String>();
		m.put("k", "v");
		b.byObj = m;
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(b);
		assertTrue(ini.contains("[byObj]"), ini);
	}

	public static class L16_Bean {
		public Map<String,String> byStr;
	}

	@Test void l16_charSequenceTypedMapKey_shortCircuitsKeyTypeCheck() {
		// keyType.isCharSequence() true -> the isSimpleMap() key-type guard short-circuits to false
		// (proceeds to the runtime per-key check) instead of returning false immediately.
		var b = new L16_Bean();
		var m = new LinkedHashMap<String,String>();
		m.put("k", "v");
		b.byStr = m;
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(b);
		assertTrue(ini.contains("[byStr]"), ini);
	}

	// getClassMetaForObject(value, cMeta) resolves purely from value.getClass() (ignoring the
	// declared field's generics), so isSimpleMap()'s mapType.getKeyType() only ever reflects a
	// non-Object/non-CharSequence type when the *runtime* map class itself bakes in a concrete key
	// type via its class declaration (e.g. `class Foo extends LinkedHashMap<Integer,String>`).
	public static class IntKeyMap extends LinkedHashMap<Integer,String> {
		private static final long serialVersionUID = 1L;
	}

	public static class L18_Bean {
		public Map<Integer,String> byInt;
	}

	@Test void l18_nonCharSequenceNonObjectTypedMapKey_notSimple() {
		// Runtime map class (IntKeyMap) bakes in Integer as its key type via its own class
		// declaration -> keyType is non-null, not CharSequence, not Object -> isSimpleMap() returns
		// false immediately without inspecting individual keys/values.
		var b = new L18_Bean();
		var m = new IntKeyMap();
		m.put(1, "v");
		b.byInt = m;
		var s = IniSerializer.create().useWhitespace().build();
		var ini = s.write(b);
		assertFalse(ini.contains("[byInt]"), ini);
		assertTrue(ini.contains("byInt"), ini);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l10x - writeBean's onBeanGetterException call (a throwing getter surfaces as a SerializeException), and
	// writeMapAtRoot's try/catch re-wrap of that same SerializeException when the bean is a root-map value.
	//------------------------------------------------------------------------------------------------------------------

	public static class L14_Bean {
		public String getX() { throw new RuntimeException("boom"); }
	}

	@Test void l14_beanGetterException_atSectionRoot() {
		assertThrowsWithMessage(SerializeException.class, "boom", () -> IniSerializer.DEFAULT.write(new L14_Bean()));
	}

	@Test void l15_beanGetterException_asRootMapValue_rewrappedByCatch() {
		var m = new LinkedHashMap<String,Object>();
		m.put("k", new L14_Bean());
		assertThrowsWithMessage(SerializeException.class, "boom", () -> IniSerializer.DEFAULT.write(m));
	}
}
