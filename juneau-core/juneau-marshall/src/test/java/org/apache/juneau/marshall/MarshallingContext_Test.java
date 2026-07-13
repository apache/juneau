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
package org.apache.juneau.marshall;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.conversion.*;
import org.apache.juneau.commons.http.MediaType;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.swap.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class BeanContext_Test extends TestBase {

	MarshallingContext bc = MarshallingContext.DEFAULT;
	MarshallingSession bs = MarshallingContext.DEFAULT_SESSION;

	@AfterEach
	void tearDown() {
		Settings.get().clearLocal();
	}

	public interface A1 {
		int getF1();
		void setF1(int f1);
	}

	@Test void a01_normalCachableBean() throws ExecutableException {
		var cm1 = bc.getClassMeta(A1.class);
		var cm2 = bc.getClassMeta(A1.class);
		assertSame(cm1, cm2);
	}

	interface A2 {
		void foo(int x);
	}

	@Test void a02_lambdaExpressionsNotCached() throws ExecutableException {
		var bc2 = MarshallingContext.DEFAULT;
		var fi = (A2)System.out::println;
		var cm1 = bc2.getClassMeta(fi.getClass());
		var cm2 = bc2.getClassMeta(fi.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void a03_proxiesNotCached() throws ExecutableException {
		var a1 = bs.getBeanMeta(A1.class).newBean(null);
		var cm1 = bc.getClassMeta(a1.getClass());
		var cm2 = bc.getClassMeta(a1.getClass());
		assertNotSame(cm1, cm2);
	}

	@Test void b01_ignoreUnknownEnumValues() {
		var p1 = Json5Parser.DEFAULT;
		assertThrowsWithMessage(Exception.class, "Could not resolve enum value 'UNKNOWN' on class 'org.apache.juneau.testutils.pojos.TestEnum'", () -> p1.parse("'UNKNOWN'", TestEnum.class));

		var p2 = Json5Parser.create().ignoreUnknownEnumValues().build();
		assertNull(p2.parse("'UNKNOWN'", TestEnum.class));
	}

	//====================================================================================================
	// MarshallingContext.Builder<?> locale, mediaType, and timeZone from Settings
	//====================================================================================================

	@Test void c01_locale_fromSettings() {
		Settings.get().setLocal("MarshallingContext.locale", "fr-CA");
		try {
			var bc2 = MarshallingContext.create().build();
			assertEquals(Locale.forLanguageTag("fr-CA"), bc2.getLocale());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c02_locale_defaultWhenNotSet() {
		var bc2 = MarshallingContext.create().build();
		assertEquals(Locale.getDefault(), bc2.getLocale());
	}

	@Test void c03_mediaType_fromSettings() {
		Settings.get().setLocal("MarshallingContext.mediaType", "application/json");
		try {
			var bc2 = MarshallingContext.create().build();
			assertEquals(MediaType.of("application/json"), bc2.getMediaType());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c04_mediaType_nullWhenNotSet() {
		var bc2 = MarshallingContext.create().build();
		assertNull(bc2.getMediaType());
	}

	@Test void c05_timeZone_fromSettings() {
		Settings.get().setLocal("MarshallingContext.timeZone", "America/New_York");
		try {
			var bc2 = MarshallingContext.create().build();
			assertEquals(TimeZone.getTimeZone("America/New_York"), bc2.getTimeZone());
		} finally {
			Settings.get().clearLocal();
		}
	}

	@Test void c06_timeZone_nullWhenNotSet() {
		var bc2 = MarshallingContext.create().build();
		assertNull(bc2.getTimeZone());
	}

	//====================================================================================================
	// MarshallingContext.copy() - Copy constructor coverage
	//====================================================================================================

	@Test void d01_copy() {
		// Create a MarshallingContext with some properties set to exercise the copy constructor
		var original = MarshallingContext.create()
			.unsortedProperties()
			.locale(Locale.CANADA)
			.mediaType(MediaType.JSON)
			.timeZone(TimeZone.getTimeZone("America/New_York"))
			.build();

		// Call copy() which uses the copy constructor Builder(MarshallingContext copyFrom)
		// This exercises lines 273-277 which convert boolean fields from MarshallingContext to Builder disable flags
		var builder = original.copy();

		// Build a new context from the copied builder
		var copied = builder.build();

		// Verify the copied context has the same values
		assertEquals(original.getBeanClassVisibility(), copied.getBeanClassVisibility());
		assertEquals(original.getBeanConstructorVisibility(), copied.getBeanConstructorVisibility());
		assertEquals(original.getBeanFieldVisibility(), copied.getBeanFieldVisibility());
		assertEquals(original.getBeanMethodVisibility(), copied.getBeanMethodVisibility());
		assertEquals(original.getBeanDictionary(), copied.getBeanDictionary());
		assertEquals(original.getLocale(), copied.getLocale());
		assertEquals(original.getMediaType(), copied.getMediaType());
		assertEquals(original.getTimeZone(), copied.getTimeZone());
	}

	//====================================================================================================
	// MarshallingContext.Builder.impl() - Line 2372 coverage
	//====================================================================================================

	@Test
	void e01_impl() {
		var impl = MarshallingContext.create()
			.unsortedProperties()
			.locale(Locale.CANADA)
			.build();

		var builder = MarshallingContext.create()
			.impl(impl);

		// Build should return the pre-instantiated context
		var result = builder.build();
		assertSame(impl, result);
	}

	//====================================================================================================
	// Builder copy-constructor null-check negations (lines 381-385, 421)
	// When source context has disable* flags set, the negation branch in the copy constructor fires.
	//====================================================================================================

	@Test void f01_copyConstructor_disabledFlags() {
		var original = MarshallingContext.create()
			.disableBeansRequireSomeProperties()
			.disableIgnoreMissingSetters()
			.disableIgnoreTransientFields()
			.disableIgnoreUnknownNullBeanProperties()
			.disableInterfaceProxies()
			.disableDefaultViewInclusion()
			.build();

		var copied = original.copy().build();

		assertFalse(copied.isBeansRequireSomeProperties());
		assertFalse(copied.isIgnoreMissingSetters());
		assertFalse(copied.isIgnoreTransientFields());
		assertFalse(copied.isIgnoreUnknownNullBeanProperties());
		assertFalse(copied.isUseInterfaceProxies());
		assertFalse(copied.isDefaultViewInclusion());
	}

	//====================================================================================================
	// unsortedProperties(Class<?>...on) for-loop (line 3251)
	// Passing two classes exercises the loop body more than once.
	//====================================================================================================

	@Test void g01_unsortedProperties_multipleClasses() {
		var bc2 = MarshallingContext.create().unsortedProperties(String.class, Integer.class).build();
		assertNotNull(bc2);
	}

	//====================================================================================================
	// Format setter null-guard branches (lines 3532-3736)
	// Each setter has `value == null ? DEFAULT : value`; calling with null covers the null branch.
	//====================================================================================================

	@Test void h01_formatSetters_nullDefaultsPreserved() {
		var bc2 = MarshallingContext.create()
			.durationFormat(null)
			.periodFormat(null)
			.calendarFormat(null)
			.dateFormat(null)
			.temporalFormat(null)
			.timeZoneFormat(null)
			.localeFormat(null)
			.binaryFormat(null)
			.enumFormat(null)
			.uuidFormat(null)
			.bitSetFormat(null)
			.bigNumberFormat(null)
			.booleanFormat(null)
			.floatFormat(null)
			.currencyFormat(null)
			.classFormat(null)
			.build();

		assertEquals(DurationFormat.ISO_8601_WITH_DAYS, bc2.getDurationFormat());
		assertEquals(PeriodFormat.ISO_8601, bc2.getPeriodFormat());
		assertEquals(CalendarFormat.ISO_OFFSET_DATE_TIME, bc2.getCalendarFormat());
		assertEquals(DateFormat.ISO_LOCAL_DATE_TIME, bc2.getDateFormat());
		assertEquals(TemporalFormat.DEFAULT, bc2.getTemporalFormat());
		assertEquals(TimeZoneFormat.ID, bc2.getTimeZoneFormat());
		assertEquals(LocaleFormat.BCP_47, bc2.getLocaleFormat());
		assertEquals(BinaryFormat.NOT_SET, bc2.getBinaryFormat());
		assertEquals(EnumFormat.TO_STRING, bc2.getEnumFormat());
		assertEquals(UuidFormat.STANDARD, bc2.getUuidFormat());
		assertEquals(BitSetFormat.INDICES, bc2.getBitSetFormat());
		assertEquals(BigNumberFormat.NUMBER, bc2.getBigNumberFormat());
		assertEquals(BooleanFormat.TRUE_FALSE, bc2.getBooleanFormat());
		assertEquals(FloatFormat.NaN_AS_NULL, bc2.getFloatFormat());
		assertEquals(CurrencyFormat.ISO_CODE, bc2.getCurrencyFormat());
		assertEquals(ClassFormat.FQCN, bc2.getClassFormat());
	}

	//====================================================================================================
	// findConversion() paths (lines 4351-4721)
	//====================================================================================================

	public static class MyBean {
		public String name;
		public MyBean() {}
		public MyBean(String name) { this.name = name; }
	}

	@Test void i01_findConversion_charSequenceToDuration() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, Duration.class));
	}

	@Test void i02_findConversion_durationToCharSequence() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Duration.class, String.class));
	}

	@Test void i03_findConversion_numberToCalendar() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Long.class, Calendar.class));
	}

	@Test void i04_findConversion_byteArrayToString() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(byte[].class, String.class));
	}

	@Test void i05_findConversion_collectionToString() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(List.class, String.class));
	}

	@Test void i06_findConversion_classToString() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Class.class, String.class));
	}

	@Test void i07_findConversion_charSequenceToArray() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, int[].class));
	}

	@Test void i08_findConversion_charSequenceToMap() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, Map.class));
	}

	@Test void i09_findConversion_collectionToCollection() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(List.class, Collection.class));
	}

	@Test void i10_findConversion_mapToCollection() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Map.class, Collection.class));
	}

	@Test void i11_findConversion_charSequenceToCollection() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, ArrayList.class));
	}

	@Test void i12_findConversion_mapToBean() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Map.class, MyBean.class));
	}

	@Test void i13_findConversion_charSequenceToUrl() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, URL.class));
	}

	@Test void i14_findConversion_charSequenceToBean() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, MyBean.class));
	}

	@Test void i15_findConversion_calendarToCalendar() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Calendar.class, Calendar.class));
	}

	@Test void i16_findConversion_dateToCalendar() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(java.util.Date.class, Calendar.class));
	}

	@Test void i17_findConversion_calendarToDate() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(Calendar.class, java.util.Date.class));
	}

	@Test void i18_findConversion_byteArrayToInputStream() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(byte[].class, InputStream.class));
	}

	@Test void i19_findConversion_charSequenceToInputStream() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, InputStream.class));
	}

	@Test void i20_findConversion_byteArrayToReader() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(byte[].class, Reader.class));
	}

	@Test void i21_findConversion_charSequenceToReader() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(String.class, Reader.class));
	}

	@Test void i22_findConversion_enumToInteger() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(TestEnum.class, Integer.class));
	}

	@Test void i23_findConversion_objectToBoolean() {
		assertNotNull(MarshallingContext.DEFAULT.findConversion(MyBean.class, Boolean.class));
	}

	@Test void i24_findConversion_noMatchReturnsNull() {
		assertNull(MarshallingContext.DEFAULT.findConversion(InputStream.class, OutputStream.class));
	}

	//====================================================================================================
	// ObjectSwap findConversion paths (lines 4351-4413)
	//====================================================================================================

	public static class DummyNormal {
		public String value;
		public DummyNormal() {}
		public DummyNormal(String v) { this.value = v; }
	}

	public static class DummySwap extends ObjectSwap<DummyNormal, String> {
		@Override
		public String swap(MarshallingSession session, DummyNormal o) throws Exception {
			return o.value;
		}
		@Override
		public DummyNormal unswap(MarshallingSession session, String f, ClassMeta<?> hint) throws Exception {
			return new DummyNormal(f);
		}
	}

	@Test void j01_findConversion_objectSwap_normalToSwap() {
		var bc2 = MarshallingContext.create().swaps(DummySwap.class).build();
		assertNotNull(bc2.findConversion(DummyNormal.class, String.class));
	}

	@Test void j02_findConversion_objectSwap_swapToNormal() {
		var bc2 = MarshallingContext.create().swaps(DummySwap.class).build();
		assertNotNull(bc2.findConversion(String.class, DummyNormal.class));
	}

	//====================================================================================================
	// Invoke conversion lambdas to cover internal branches
	//====================================================================================================

	@SuppressWarnings("unchecked")
	@Test void k01_invokeConversion_charSequenceToDuration() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, Duration.class);
		assertNotNull(conv);
		var result = conv.to("PT1H", null, null);
		assertEquals(Duration.ofHours(1), result);
	}

	@SuppressWarnings("unchecked")
	@Test void k02_invokeConversion_durationToString() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Duration.class, String.class);
		assertNotNull(conv);
		assertNotNull(conv.to(Duration.ofHours(1), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k03_invokeConversion_numberToCalendar() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Long.class, Calendar.class);
		assertNotNull(conv);
		assertNotNull(conv.to(0L, null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k04_invokeConversion_byteArrayToString() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(byte[].class, String.class);
		assertNotNull(conv);
		assertEquals("hello", conv.to("hello".getBytes(), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k05_invokeConversion_collectionToString() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, String.class);
		assertNotNull(conv);
		assertNotNull(conv.to(List.of("a", "b"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k06_invokeConversion_classToString() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Class.class, String.class);
		assertNotNull(conv);
		assertEquals("java.lang.String", conv.to(String.class, null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k07_invokeConversion_charSequenceToArray() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, int[].class);
		assertNotNull(conv);
		assertNotNull(conv.to("[1,2,3]", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k08_invokeConversion_charSequenceToMap() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, Map.class);
		assertNotNull(conv);
		assertNotNull(conv.to("{a:'b'}", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k09_invokeConversion_listToCollection() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, Collection.class);
		assertNotNull(conv);
		assertNotNull(conv.to(List.of("x"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k10_invokeConversion_mapToCollection() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Map.class, Collection.class);
		assertNotNull(conv);
		assertNotNull(conv.to(Map.of("k", "v"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k11_invokeConversion_stringToArrayList() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, ArrayList.class);
		assertNotNull(conv);
		assertNotNull(conv.to("['a','b']", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k12_invokeConversion_mapToBean() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Map.class, MyBean.class);
		assertNotNull(conv);
		assertNotNull(conv.to(Map.of("name", "test"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k13_invokeConversion_stringToUrl() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, URL.class);
		assertNotNull(conv);
		assertNotNull(conv.to("http://example.com", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k14_invokeConversion_stringToBean() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, MyBean.class);
		assertNotNull(conv);
		assertNotNull(conv.to("{name:'test'}", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k15_invokeConversion_calendarToCalendar() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Calendar.class, Calendar.class);
		assertNotNull(conv);
		assertNotNull(conv.to(new GregorianCalendar(2024, 0, 1), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k16_invokeConversion_dateToCalendar() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(java.util.Date.class, Calendar.class);
		assertNotNull(conv);
		assertNotNull(conv.to(new java.util.Date(0), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k17_invokeConversion_calendarToDate() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Calendar.class, java.util.Date.class);
		assertNotNull(conv);
		assertNotNull(conv.to(new GregorianCalendar(2024, 0, 1), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k18_invokeConversion_byteArrayToInputStream() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(byte[].class, InputStream.class);
		assertNotNull(conv);
		assertNotNull(conv.to("test".getBytes(), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k19_invokeConversion_stringToInputStream() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, InputStream.class);
		assertNotNull(conv);
		assertNotNull(conv.to("test", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k20_invokeConversion_byteArrayToReader() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(byte[].class, Reader.class);
		assertNotNull(conv);
		assertNotNull(conv.to("test".getBytes(), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k21_invokeConversion_stringToReader() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, Reader.class);
		assertNotNull(conv);
		assertNotNull(conv.to("test", null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k22_invokeConversion_enumToInteger() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(TestEnum.class, Integer.class);
		assertNotNull(conv);
		// enum.toString() → Integer; non-numeric string throws — the branch (enum→toString→outType) is still covered.
		assertThrows(Exception.class, () -> conv.to(TestEnum.ONE, null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k23_invokeConversion_objectToBoolean() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(MyBean.class, Boolean.class);
		assertNotNull(conv);
		assertNotNull(conv.to(new MyBean("true"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k24_invokeConversion_objectSwap_normalToSwap() {
		var bc2 = MarshallingContext.create().swaps(DummySwap.class).build();
		var conv = (Conversion<Object,Object>) bc2.findConversion(DummyNormal.class, String.class);
		assertNotNull(conv);
		assertEquals("hello", conv.to(new DummyNormal("hello"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void k25_invokeConversion_objectSwap_swapToNormal() {
		var bc2 = MarshallingContext.create().swaps(DummySwap.class).build();
		var conv = (Conversion<Object,Object>) bc2.findConversion(String.class, DummyNormal.class);
		assertNotNull(conv);
		assertEquals("hello", ((DummyNormal) conv.to("hello", null, null)).value);
	}

	//====================================================================================================
	// newCollection() branches — exercise Set, SortedSet, and concrete-class paths
	//====================================================================================================

	@SuppressWarnings("unchecked")
	@Test void l01_invokeConversion_listToSet() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, Set.class);
		assertNotNull(conv);
		assertNotNull(conv.to(List.of("a"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void l02_invokeConversion_listToSortedSet() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, SortedSet.class);
		assertNotNull(conv);
		assertNotNull(conv.to(List.of("a"), null, null));
	}

	@SuppressWarnings("unchecked")
	@Test void l03_invokeConversion_listToLinkedList() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, LinkedList.class);
		assertNotNull(conv);
		assertNotNull(conv.to(List.of("a"), null, null));
	}

	//====================================================================================================
	// isNotABean() branches — requires notBeanPackages / notBeanClasses on builder
	//====================================================================================================

	@Test void m01_isNotABean_array() {
		var cm = bc.getClassMeta(String[].class);
		assertNotNull(cm);
	}

	@Test void m02_isNotABean_primitive() {
		var cm = bc.getClassMeta(int.class);
		assertNotNull(cm);
	}

	@Test void m03_isNotABean_enum() {
		var cm = bc.getClassMeta(TestEnum.class);
		assertNotNull(cm);
	}

	@Test void m04_isNotABean_matchingPackageName() {
		var bc2 = MarshallingContext.create().notBeanPackages("org.apache.juneau.testutils.pojos").build();
		var cm = bc2.getClassMeta(ABean.class);
		assertNotNull(cm);
	}

	@Test void m05_isNotABean_matchingPackagePrefix() {
		var bc2 = MarshallingContext.create().notBeanPackages("org.apache.juneau.*").build();
		var cm = bc2.getClassMeta(ABean.class);
		assertNotNull(cm);
	}

	@Test void m06_isNotABean_matchingNotBeanClass() {
		var bc2 = MarshallingContext.create().notBeanClasses(ABean.class).build();
		var cm = bc2.getClassMeta(ABean.class);
		assertNotNull(cm);
	}

	//====================================================================================================
	// findConversion — collection with element-type arg (line 4535/4536)
	//====================================================================================================

	@SuppressWarnings("unchecked")
	@Test void n01_invokeConversion_mapToCollection_withElementType() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(Map.class, Collection.class);
		assertNotNull(conv);
		// Pass element-type arg to exercise the elemType != null branch
		var result = conv.to(Map.of("k", "v"), null, null, String.class);
		assertNotNull(result);
	}

	@SuppressWarnings("unchecked")
	@Test void n02_invokeConversion_listToCollection_withElementType() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, Collection.class);
		assertNotNull(conv);
		// Pass element-type arg to exercise elemType != null path
		var result = conv.to(List.of("a", "b"), null, null, String.class);
		assertNotNull(result);
	}

	@SuppressWarnings("unchecked")
	@Test void n03_invokeConversion_listToCollection_alreadyAssignable() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(List.class, Collection.class);
		assertNotNull(conv);
		// No element type arg → if outType is assignable from in.getClass(), the early-return path fires
		var list = new ArrayList<>(List.of("a"));
		var result = conv.to(list, null, null);
		assertNotNull(result);
	}

	//====================================================================================================
	// findConversion — collection→string when no serializer (ws == null path, line 4452)
	// Use a minimal context with no serializer registered
	//====================================================================================================

	@SuppressWarnings("unchecked")
	@Test void n04_invokeConversion_arrayToString_noSerializer() {
		var bc2 = MarshallingContext.create().build();
		var conv = (Conversion<Object,Object>) bc2.findConversion(String[].class, String.class);
		assertNotNull(conv);
		// DEFAULT has a serializer; minimal context may not — just ensure the conversion runs
		var result = conv.to(new String[]{"a", "b"}, null, null);
		assertNotNull(result);
	}

	//====================================================================================================
	// findConversion — CharSequence→Collection with elemType arg in lambda (line 4552)
	//====================================================================================================

	@SuppressWarnings("unchecked")
	@Test void n05_invokeConversion_stringToArrayList_withElementType() {
		var conv = (Conversion<Object,Object>) MarshallingContext.DEFAULT.findConversion(String.class, ArrayList.class);
		assertNotNull(conv);
		var result = conv.to("['a','b']", null, null, String.class);
		assertNotNull(result);
	}

	//====================================================================================================
	// getClassMeta(Type, Type...) branches (line 4860)
	//====================================================================================================

	@Test void o01_getClassMeta_withTypeArgs() {
		var cm = bc.getClassMeta(Map.class, String.class, Integer.class);
		assertNotNull(cm);
	}

	@Test void o02_getClassMeta_withCollectionTypeArg() {
		var cm = bc.getClassMeta(List.class, String.class);
		assertNotNull(cm);
	}
}