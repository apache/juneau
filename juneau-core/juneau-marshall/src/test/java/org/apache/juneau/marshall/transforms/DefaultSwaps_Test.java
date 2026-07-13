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
package org.apache.juneau.marshall.transforms;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

class DefaultSwaps_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Setup
	//------------------------------------------------------------------------------------------------------------------

	@BeforeAll static void beforeClass() {
		setTimeZone("GMT-5");
	}

	@AfterAll static void afterClass() {
		unsetTimeZone();
	}

	private static final WriterSerializer SERIALIZER = Json5Serializer.DEFAULT;

	private static void test1(String expected, Object o) {
		assertEquals(expected, SERIALIZER.serialize(o));
	}

	private static void test2(String expected, Object o, Class<?> configClass) {
		assertEquals(expected, SERIALIZER.copy().applyAnnotations(configClass).build().serialize(o));
	}

	private static void test3(String expected, Object o, Class<?> swap) {
		assertEquals(expected, SERIALIZER.copy().swaps(swap).build().serializeToString(o));
	}

	private static void test4(String expected, Object o, Class<?> swap, Class<?> configClass) {
		assertEquals(expected, SERIALIZER.copy().swaps(swap).applyAnnotations(configClass).build().serializeToString(o));
	}

	//------------------------------------------------------------------------------------------------------------------
	//	Enumeration - natively serialized as array
	//------------------------------------------------------------------------------------------------------------------
	private static final Vector<String> A = new Vector<>();
	static {
		A.add("foo");
		A.add("bar");
	}

	public static class ASwap extends StringSwap<Enumeration<?>> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Enumeration<?> o) {
			return "FOO";
		}
	}

	public static class ABean {
		public Enumeration<String> f1 = A.elements();
		@Swap(ASwap.class)
		public Enumeration<String> f2 = A.elements();
	}

	@Test void a01_Enumeration() {
		test1("['foo','bar']", A.elements());
	}

	@Test void a02_Enumeration_overrideSwap() {
		test3("'FOO'", A.elements(), ASwap.class);
	}

	@Test void a03_Enumeration_overrideAnnotation() {
		test1("{f1:['foo','bar'],f2:'FOO'}", new ABean());
	}

	private static final Vector<String> A_C = new Vector<>();
	static {
		A_C.add("foo");
		A_C.add("bar");
	}

	public static class AcSwap extends StringSwap<Enumeration<?>> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Enumeration<?> o) {
			return "FOO";
		}
	}

	@SwapApply(on="Dummy1.f2", value=@Swap(AcSwap.class))
	@SwapApply(on="AcBean.f2", value=@Swap(AcSwap.class))
	@SwapApply(on="Dummy2.f2", value=@Swap(AcSwap.class))
	private static class AcMarshalledConfig {}

	public static class AcBean {
		public Enumeration<String> f1 = A.elements();

		public Enumeration<String> f2 = A.elements();
	}

	@Test void a01c_Enumeration_usingConfig() {
		test2("['foo','bar']", A_C.elements(), AcMarshalledConfig.class);
	}

	@Test void a02c_Enumeration_overrideSwap_usingConfig() {
		test4("'FOO'", A_C.elements(), AcSwap.class, AcMarshalledConfig.class);
	}

	@Test void a03c_Enumeration_overrideAnnotation_usingConfig() {
		test2("{f1:['foo','bar'],f2:'FOO'}", new AcBean(), AcMarshalledConfig.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	//	Iterator - natively serialized as array
	//------------------------------------------------------------------------------------------------------------------
	private static final List<String> B = l("foo","bar");

	public static class BSwap extends StringSwap<Iterator<?>> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Iterator<?> o) {
			return "FOO";
		}
	}

	public static class BBean {
		public Iterator<?> f1 = B.iterator();
		@Swap(BSwap.class)
		public Iterator<?> f2 = B.iterator();
	}

	@Test void b01_Iterator() {
		test1("['foo','bar']", B.iterator());
	}

	@Test void b02_Iterator_overrideSwap() {
		test3("'FOO'", B.iterator(), BSwap.class);
	}

	@Test void b03_Iterator_overrideAnnotation() {
		test1("{f1:['foo','bar'],f2:'FOO'}", new BBean());
	}

	private static final List<String> B_C = l("foo","bar");

	public static class BcSwap extends StringSwap<Iterator<?>> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Iterator<?> o) {
			return "FOO";
		}
	}

	@SwapApply(on="Dummy1.f2", value=@Swap(BcSwap.class))
	@SwapApply(on="BcBean.f2", value=@Swap(BcSwap.class))
	@SwapApply(on="Dummy2.f2", value=@Swap(BcSwap.class))
	private static class BcMarshalledConfig {}

	public static class BcBean {
		public Iterator<?> f1 = B.iterator();
		public Iterator<?> f2 = B.iterator();
	}

	@Test void b01c_Iterator_usingConfig() {
		test2("['foo','bar']", B_C.iterator(), BcMarshalledConfig.class);
	}

	@Test void b02c_Iterator_overrideSwap_usingConfig() {
		test4("'FOO'", B_C.iterator(), BcSwap.class, BcMarshalledConfig.class);
	}

	@Test void b03c_Iterator_overrideAnnotation_usingConfig() {
		test2("{f1:['foo','bar'],f2:'FOO'}", new BcBean(), BcMarshalledConfig.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Locale.class, ...) - inline anonymous StringSwap<Locale> in DefaultSwaps
	//------------------------------------------------------------------------------------------------------------------
	private static final Locale C = Locale.JAPAN;

	public static class CSwap extends StringSwap<Locale> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Locale o) {
			return "FOO";
		}
	}

	public static class CBean {
		public Locale f1 = C;
		@Swap(CSwap.class)
		public Locale f2 = C;
	}

	@Test void c01_Locale() {
		test1("'ja-JP'", C);
	}

	@Test void c02_Locale_overrideSwap() {
		test3("'FOO'", C, CSwap.class);
	}

	@Test void c03_Locale_overrideAnnotation() {
		test1("{f1:'ja-JP',f2:'FOO'}", new CBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Calendar.class, new TemporalCalendarSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final GregorianCalendar D = GregorianCalendar.from(ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2012-12-21T12:34:56Z")));

	public static class DSwap extends StringSwap<Calendar> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Calendar o) {
			return "FOO";
		}
	}

	public static class DBean {
		public GregorianCalendar f1 = D;
		@Swap(DSwap.class)
		public GregorianCalendar f2 = D;
	}

	@Test void d01_Calendar() {
		test1("'2012-12-21T12:34:56Z'", D);
	}

	@Test void d02_Calendar_overrideSwap() {
		test3("'FOO'", D, DSwap.class);
	}

	@Test void d03_Calendar_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new DBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Date.class, new TemporalDateSwap.IsoLocalDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final Date E = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2012-12-21T12:34:56Z")));

	public static class ESwap extends StringSwap<Date> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Date o) {
			return "FOO";
		}
	}

	public static class EBean {
		public Date f1 = E;
		@Swap(ESwap.class)
		public Date f2 = E;
	}

	@Test void e01_Date() {
		test1("'2012-12-21T07:34:56'", E);
	}

	@Test void e02_Date_overrideSwap() {
		test3("'FOO'", E, ESwap.class);
	}

	@Test void e03_Date_overrideAnnotation() {
		test1("{f1:'2012-12-21T07:34:56',f2:'FOO'}", new EBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Instant.class, new TemporalSwap.IsoInstant())
	//------------------------------------------------------------------------------------------------------------------
	private static final Instant FA = Instant.parse("2012-12-21T12:34:56Z");

	public static class FASwap extends StringSwap<Instant> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Instant o) {
			return "FOO";
		}
	}

	public static class FABean {
		public Instant f1 = FA;
		@Swap(FASwap.class)
		public Instant f2 = FA;
	}

	@Test void fa01_Instant() {
		test1("'2012-12-21T12:34:56Z'", FA);
	}

	@Test void fa02_Instant_overrideSwap() {
		test3("'FOO'", FA, FASwap.class);
	}

	@Test void fa03_Instant_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new FABean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(ZonedDateTime.class, new TemporalSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final ZonedDateTime FB = ZonedDateTime.parse("2012-12-21T12:34:56Z");

	public static class FBSwap extends StringSwap<ZonedDateTime> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, ZonedDateTime o) {
			return "FOO";
		}
	}

	public static class FBBean {
		public ZonedDateTime f1 = FB;
		@Swap(FBSwap.class)
		public ZonedDateTime f2 = FB;
	}

	@Test void fb01_ZonedDateTime() {
		test1("'2012-12-21T12:34:56Z'", FB);
	}

	@Test void fb02_ZonedDateTime_overrideSwap() {
		test3("'FOO'", FB, FBSwap.class);
	}

	@Test void fb03_ZonedDateTime_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new FBBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalDate.class, new TemporalSwap.IsoLocalDate())
	//------------------------------------------------------------------------------------------------------------------
	private static final LocalDate FC = LocalDate.parse("2012-12-21");

	public static class FCSwap extends StringSwap<LocalDate> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, LocalDate o) {
			return "FOO";
		}
	}

	public static class FCBean {
		public LocalDate f1 = FC;
		@Swap(FCSwap.class)
		public LocalDate f2 = FC;
	}

	@Test void fc01_LocalDate() {
		test1("'2012-12-21'", FC);
	}

	@Test void fc02_LocalDate_overrideSwap() {
		test3("'FOO'", FC, FCSwap.class);
	}

	@Test void fc03_LocalDate_overrideAnnotation() {
		test1("{f1:'2012-12-21',f2:'FOO'}", new FCBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalDateTime.class, new TemporalSwap.IsoLocalDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final LocalDateTime FD = LocalDateTime.parse("2012-12-21T12:34:56");

	public static class FDSwap extends StringSwap<LocalDateTime> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, LocalDateTime o) {
			return "FOO";
		}
	}

	public static class FDBean {
		public LocalDateTime f1 = FD;
		@Swap(FDSwap.class)
		public LocalDateTime f2 = FD;
	}

	@Test void fd01_LocalDateTime() {
		test1("'2012-12-21T12:34:56'", FD);
	}

	@Test void fd02_LocalDateTime_overrideSwap() {
		test3("'FOO'", FD, FDSwap.class);
	}

	@Test void fd03_LocalDateTime_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56',f2:'FOO'}", new FDBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalTime.class, new TemporalSwap.IsoLocalTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final LocalTime FE = LocalTime.parse("12:34:56");

	public static class FESwap extends StringSwap<LocalTime> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, LocalTime o) {
			return "FOO";
		}
	}

	public static class FEBean {
		public LocalTime f1 = FE;
		@Swap(FESwap.class)
		public LocalTime f2 = FE;
	}

	@Test void fe01_LocalTime() {
		test1("'12:34:56'", FE);
	}

	@Test void fe02_LocalTime_overrideSwap() {
		test3("'FOO'", FE, FESwap.class);
	}

	@Test void fe03_LocalTime_overrideAnnotation() {
		test1("{f1:'12:34:56',f2:'FOO'}", new FEBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(OffsetDateTime.class, new TemporalSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final OffsetDateTime FF = OffsetDateTime.parse("2012-12-21T12:34:56-05:00");

	public static class FFSwap extends StringSwap<OffsetDateTime> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, OffsetDateTime o) {
			return "FOO";
		}
	}

	public static class FFBean {
		public OffsetDateTime f1 = FF;
		@Swap(FFSwap.class)
		public OffsetDateTime f2 = FF;
	}

	@Test void ff01_OffsetDateTime() {
		test1("'2012-12-21T12:34:56-05:00'", FF);
	}

	@Test void ff02_OffsetDateTime_overrideSwap() {
		test3("'FOO'", FF, FFSwap.class);
	}

	@Test void ff03_OffsetDateTime_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56-05:00',f2:'FOO'}", new FFBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(OffsetTime.class, new TemporalSwap.IsoOffsetTime())
	//------------------------------------------------------------------------------------------------------------------
	private static final OffsetTime FG = OffsetTime.parse("12:34:56-05:00");

	public static class FGSwap extends StringSwap<OffsetTime> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, OffsetTime o) {
			return "FOO";
		}
	}

	public static class FGBean {
		public OffsetTime f1 = FG;
		@Swap(FGSwap.class)
		public OffsetTime f2 = FG;
	}

	@Test void fg01_OffsetTime() {
		test1("'12:34:56-05:00'", FG);
	}

	@Test void fg02_OffsetTime_overrideSwap() {
		test3("'FOO'", FG, FGSwap.class);
	}

	@Test void fg03_OffsetTime_overrideAnnotation() {
		test1("{f1:'12:34:56-05:00',f2:'FOO'}", new FGBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Year.class, new TemporalSwap.IsoYear())
	//------------------------------------------------------------------------------------------------------------------
	private static final Year FH = Year.parse("2012");

	public static class FHSwap extends StringSwap<Year> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Year o) {
			return "FOO";
		}
	}

	public static class FHBean {
		public Year f1 = FH;
		@Swap(FHSwap.class)
		public Year f2 = FH;
	}

	@Test void fh01_Year() {
		test1("'2012'", FH);
	}

	@Test void fh02_Year_overrideSwap() {
		test3("'FOO'", FH, FHSwap.class);
	}

	@Test void fh03_Year_overrideAnnotation() {
		test1("{f1:'2012',f2:'FOO'}", new FHBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(YearMonth.class, new TemporalSwap.IsoYearMonth())
	//------------------------------------------------------------------------------------------------------------------
	private static final YearMonth FI = YearMonth.parse("2012-12");

	public static class FISwap extends StringSwap<YearMonth> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, YearMonth o) {
			return "FOO";
		}
	}

	public static class FIBean {
		public YearMonth f1 = FI;
		@Swap(FISwap.class)
		public YearMonth f2 = FI;
	}

	@Test void fi01_YearMonth() {
		test1("'2012-12'", FI);
	}

	@Test void fi02_YearMonth_overrideSwap() {
		test3("'FOO'", FI, FISwap.class);
	}

	@Test void fi03_YearMonth_overrideAnnotation() {
		test1("{f1:'2012-12',f2:'FOO'}", new FIBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Temporal.class, new TemporalSwap.IsoInstant())
	//------------------------------------------------------------------------------------------------------------------
	private static final Temporal FJ = HijrahDate.from(FB);

	public static class FJSwap extends StringSwap<Temporal> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, Temporal o) {
			return "FOO";
		}
	}

	public static class FJBean {
		public Temporal f1 = FJ;
		@Swap(FJSwap.class)
		public Temporal f2 = FJ;
	}

	@Test void fj01_Temporal() {
		test1("'2012-12-21T05:00:00Z'", FJ);
	}

	@Test void fj02_Temporal_overrideSwap() {
		test3("'FOO'", FJ, FJSwap.class);
	}

	@Test void fj03_Temporal_overrideAnnotation() {
		test1("{f1:'2012-12-21T05:00:00Z',f2:'FOO'}", new FJBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(TimeZone.class, ...) - inline anonymous StringSwap<TimeZone> in DefaultSwaps
	//------------------------------------------------------------------------------------------------------------------
	private static final TimeZone G = TimeZone.getTimeZone("Z");

	public static class GSwap extends StringSwap<TimeZone> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, TimeZone o) {
			return "FOO";
		}
	}

	public static class GBean {
		public TimeZone f1 = G;
		@Swap(GSwap.class)
		public TimeZone f2 = G;
	}

	@Test void g01_TimeZone() {
		test1("'GMT'", G);
	}

	@Test void g02_TimeZone_overrideSwap() {
		test3("'FOO'", G, GSwap.class);
	}

	@Test void g03_TimeZone_overrideAnnotation() {
		test1("{f1:'GMT',f2:'FOO'}", new GBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(XMLGregorianCalendar.class, new XMLGregorianCalendarSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static final XMLGregorianCalendar H = safe(()->DatatypeFactory.newInstance().newXMLGregorianCalendar("2012-12-21T12:34:56.789Z"));

	public static class HSwap extends StringSwap<XMLGregorianCalendar> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, XMLGregorianCalendar o) {
			return "FOO";
		}
	}

	public static class HBean {
		public XMLGregorianCalendar f1 = H;
		@Swap(HSwap.class)
		public XMLGregorianCalendar f2 = H;
	}

	@Test void h01_XMLGregorianCalendar() {
		test1("'2012-12-21T12:34:56.789Z'", H);
	}

	@Test void h02_XMLGregorianCalendar_overrideSwap() {
		test3("'FOO'", H, HSwap.class);
	}

	@Test void h03_XMLGregorianCalendar_overrideAnnotation() {
		test1("{f1:'2012-12-21T12:34:56.789Z',f2:'FOO'}", new HBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(ZoneId.class, ...) - inline anonymous StringSwap<ZoneId> in DefaultSwaps
	//------------------------------------------------------------------------------------------------------------------
	private static final ZoneId I = ZoneId.of("Z");

	public static class ISwap extends StringSwap<ZoneId> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, ZoneId o) {
			return "FOO";
		}
	}

	public static class IBean {
		public ZoneId f1 = I;
		@Swap(ISwap.class)
		public ZoneId f2 = I;
	}

	@Test void i01_ZoneId() {
		test1("'Z'", I);
	}

	@Test void i02_ZoneId_overrideSwap() {
		test3("'FOO'", I, ISwap.class);
	}

	@Test void i03_ZoneId_overrideAnnotation() {
		test1("{f1:'Z',f2:'FOO'}", new IBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	Duration - built-in first-class support
	//------------------------------------------------------------------------------------------------------------------
	private static final java.time.Duration J = java.time.Duration.ofHours(1).plusMinutes(30);

	public static class JSwap extends StringSwap<java.time.Duration> {
		@Override /* ObjectSwap */
		public String swap(MarshallingSession session, java.time.Duration o) {
			return "FOO";
		}
	}

	public static class JBean {
		public java.time.Duration f1 = J;
		@Swap(JSwap.class)
		public java.time.Duration f2 = J;
	}

	@Test void j01_Duration() {
		test1("'PT1H30M'", J);
	}

	@Test void j02_Duration_overrideSwap() {
		test3("'FOO'", J, JSwap.class);
	}

	@Test void j03_Duration_overrideAnnotation() {
		test1("{f1:'PT1H30M',f2:'FOO'}", new JBean());
	}

	public static class J2Bean {
		@MarshalledProp(durationFormat=DurationFormat.MILLIS)
		public java.time.Duration f1 = java.time.Duration.ofSeconds(5);
		@MarshalledProp(durationFormat=DurationFormat.SECONDS)
		public java.time.Duration f2 = java.time.Duration.ofSeconds(5);
	}

	@Test void j04_Duration_propertyFormatOverride() {
		test1("{f1:5000,f2:5.0}", new J2Bean());
	}

	public static class J3Bean {
		@MarshalledProp(periodFormat=PeriodFormat.DAYS)
		public Period f1 = Period.ofDays(9);
		@MarshalledProp(periodFormat=PeriodFormat.DAYS)
		public Period f2 = Period.ofDays(2);
	}

	@Test void j05_Period_propertyFormatOverride() {
		test1("{f1:9,f2:2}", new J3Bean());
	}
}