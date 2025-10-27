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
package org.apache.juneau.transforms;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.chrono.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
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

	private static void test1(String expected, Object o) throws Exception {
		assertEquals(expected, SERIALIZER.serialize(o));
	}

	private static void test2(String expected, Object o, Class<?> configClass) throws Exception {
		assertEquals(expected, SERIALIZER.copy().applyAnnotations(configClass).build().serialize(o));
	}

	private static void test3(String expected, Object o, Class<?> swap) throws Exception {
		assertEquals(expected, SERIALIZER.copy().swaps(swap).build().serializeToString(o));
	}

	private static void test4(String expected, Object o, Class<?> swap, Class<?> configClass) throws Exception {
		assertEquals(expected, SERIALIZER.copy().swaps(swap).applyAnnotations(configClass).build().serializeToString(o));
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Enumeration.class, new EnumerationSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static Vector<String> A = new Vector<>();
	static {
		A.add("foo");
		A.add("bar");
	}

	public static class ASwap extends StringSwap<Enumeration<?>> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Enumeration<?> o) throws Exception {
			return "FOO";
		}
	}

	public static class ABean {
		public Enumeration<String> f1 = A.elements();
		@Swap(ASwap.class)
		public Enumeration<String> f2 = A.elements();
	}

	@Test void a01_Enumeration() throws Exception {
		test1("['foo','bar']", A.elements());
	}

	@Test void a02_Enumeration_overrideSwap() throws Exception {
		test3("'FOO'", A.elements(), ASwap.class);
	}

	@Test void a03_Enumeration_overrideAnnotation() throws Exception {
		test1("{f1:['foo','bar'],f2:'FOO'}", new ABean());
	}

	private static Vector<String> Ac = new Vector<>();
	static {
		Ac.add("foo");
		Ac.add("bar");
	}

	public static class AcSwap extends StringSwap<Enumeration<?>> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Enumeration<?> o) throws Exception {
			return "FOO";
		}
	}

	@Swap(on="Dummy1.f2", value=AcSwap.class)
	@Swap(on="AcBean.f2", value=AcSwap.class)
	@Swap(on="Dummy2.f2", value=AcSwap.class)
	private static class AcBeanConfig {}

	public static class AcBean {
		public Enumeration<String> f1 = A.elements();

		public Enumeration<String> f2 = A.elements();
	}

	@Test void a01c_Enumeration_usingConfig() throws Exception {
		test2("['foo','bar']", Ac.elements(), AcBeanConfig.class);
	}

	@Test void a02c_Enumeration_overrideSwap_usingConfig() throws Exception {
		test4("'FOO'", Ac.elements(), AcSwap.class, AcBeanConfig.class);
	}

	@Test void a03c_Enumeration_overrideAnnotation_usingConfig() throws Exception {
		test2("{f1:['foo','bar'],f2:'FOO'}", new AcBean(), AcBeanConfig.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Iterator.class, new IteratorSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static List<String> B = list("foo","bar");

	public static class BSwap extends StringSwap<Iterator<?>> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Iterator<?> o) throws Exception {
			return "FOO";
		}
	}

	public static class BBean {
		public Iterator<?> f1 = B.iterator();
		@Swap(BSwap.class)
		public Iterator<?> f2 = B.iterator();
	}

	@Test void b01_Iterator() throws Exception {
		test1("['foo','bar']", B.iterator());
	}

	@Test void b02_Iterator_overrideSwap() throws Exception {
		test3("'FOO'", B.iterator(), BSwap.class);
	}

	@Test void b03_Iterator_overrideAnnotation() throws Exception {
		test1("{f1:['foo','bar'],f2:'FOO'}", new BBean());
	}

	private static List<String> Bc = list("foo","bar");

	public static class BcSwap extends StringSwap<Iterator<?>> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Iterator<?> o) throws Exception {
			return "FOO";
		}
	}

	@Swap(on="Dummy1.f2", value=BcSwap.class)
	@Swap(on="BcBean.f2", value=BcSwap.class)
	@Swap(on="Dummy2.f2", value=BcSwap.class)
	private static class BcBeanConfig {}

	public static class BcBean {
		public Iterator<?> f1 = B.iterator();
		public Iterator<?> f2 = B.iterator();
	}

	@Test void b01c_Iterator_usingConfig() throws Exception {
		test2("['foo','bar']", Bc.iterator(), BcBeanConfig.class);
	}

	@Test void b02c_Iterator_overrideSwap_usingConfig() throws Exception {
		test4("'FOO'", Bc.iterator(), BcSwap.class, BcBeanConfig.class);
	}

	@Test void b03c_Iterator_overrideAnnotation_usingConfig() throws Exception {
		test2("{f1:['foo','bar'],f2:'FOO'}", new BcBean(), BcBeanConfig.class);
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Locale.class, new LocaleSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static Locale C = Locale.JAPAN;

	public static class CSwap extends StringSwap<Locale> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Locale o) throws Exception {
			return "FOO";
		}
	}

	public static class CBean {
		public Locale f1 = C;
		@Swap(CSwap.class)
		public Locale f2 = C;
	}

	@Test void c01_Locale() throws Exception {
		test1("'ja-JP'", C);
	}

	@Test void c02_Locale_overrideSwap() throws Exception {
		test3("'FOO'", C, CSwap.class);
	}

	@Test void c03_Locale_overrideAnnotation() throws Exception {
		test1("{f1:'ja-JP',f2:'FOO'}", new CBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Calendar.class, new TemporalCalendarSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static GregorianCalendar D = GregorianCalendar.from(ZonedDateTime.from(DateTimeFormatter.ISO_ZONED_DATE_TIME.parse("2012-12-21T12:34:56Z")));

	public static class DSwap extends StringSwap<Calendar> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Calendar o) throws Exception {
			return "FOO";
		}
	}

	public static class DBean {
		public GregorianCalendar f1 = D;
		@Swap(DSwap.class)
		public GregorianCalendar f2 = D;
	}

	@Test void d01_Calendar() throws Exception {
		test1("'2012-12-21T12:34:56Z'", D);
	}

	@Test void d02_Calendar_overrideSwap() throws Exception {
		test3("'FOO'", D, DSwap.class);
	}

	@Test void d03_Calendar_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new DBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Date.class, new TemporalDateSwap.IsoLocalDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static Date E = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2012-12-21T12:34:56Z")));

	public static class ESwap extends StringSwap<Date> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Date o) throws Exception {
			return "FOO";
		}
	}

	public static class EBean {
		public Date f1 = E;
		@Swap(ESwap.class)
		public Date f2 = E;
	}

	@Test void e01_Date() throws Exception {
		test1("'2012-12-21T07:34:56'", E);
	}

	@Test void e02_Date_overrideSwap() throws Exception {
		test3("'FOO'", E, ESwap.class);
	}

	@Test void e03_Date_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T07:34:56',f2:'FOO'}", new EBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Instant.class, new TemporalSwap.IsoInstant())
	//------------------------------------------------------------------------------------------------------------------
	private static Instant FA = Instant.parse("2012-12-21T12:34:56Z");

	public static class FASwap extends StringSwap<Instant> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Instant o) throws Exception {
			return "FOO";
		}
	}

	public static class FABean {
		public Instant f1 = FA;
		@Swap(FASwap.class)
		public Instant f2 = FA;
	}

	@Test void fa01_Instant() throws Exception {
		test1("'2012-12-21T12:34:56Z'", FA);
	}

	@Test void fa02_Instant_overrideSwap() throws Exception {
		test3("'FOO'", FA, FASwap.class);
	}

	@Test void fa03_Instant_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new FABean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(ZonedDateTime.class, new TemporalSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static ZonedDateTime FB = ZonedDateTime.parse("2012-12-21T12:34:56Z");

	public static class FBSwap extends StringSwap<ZonedDateTime> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, ZonedDateTime o) throws Exception {
			return "FOO";
		}
	}

	public static class FBBean {
		public ZonedDateTime f1 = FB;
		@Swap(FBSwap.class)
		public ZonedDateTime f2 = FB;
	}

	@Test void fb01_ZonedDateTime() throws Exception {
		test1("'2012-12-21T12:34:56Z'", FB);
	}

	@Test void fb02_ZonedDateTime_overrideSwap() throws Exception {
		test3("'FOO'", FB, FBSwap.class);
	}

	@Test void fb03_ZonedDateTime_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56Z',f2:'FOO'}", new FBBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalDate.class, new TemporalSwap.IsoLocalDate())
	//------------------------------------------------------------------------------------------------------------------
	private static LocalDate FC = LocalDate.parse("2012-12-21");

	public static class FCSwap extends StringSwap<LocalDate> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, LocalDate o) throws Exception {
			return "FOO";
		}
	}

	public static class FCBean {
		public LocalDate f1 = FC;
		@Swap(FCSwap.class)
		public LocalDate f2 = FC;
	}

	@Test void fc01_LocalDate() throws Exception {
		test1("'2012-12-21'", FC);
	}

	@Test void fc02_LocalDate_overrideSwap() throws Exception {
		test3("'FOO'", FC, FCSwap.class);
	}

	@Test void fc03_LocalDate_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21',f2:'FOO'}", new FCBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalDateTime.class, new TemporalSwap.IsoLocalDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static LocalDateTime FD = LocalDateTime.parse("2012-12-21T12:34:56");

	public static class FDSwap extends StringSwap<LocalDateTime> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, LocalDateTime o) throws Exception {
			return "FOO";
		}
	}

	public static class FDBean {
		public LocalDateTime f1 = FD;
		@Swap(FDSwap.class)
		public LocalDateTime f2 = FD;
	}

	@Test void fd01_LocalDateTime() throws Exception {
		test1("'2012-12-21T12:34:56'", FD);
	}

	@Test void fd02_LocalDateTime_overrideSwap() throws Exception {
		test3("'FOO'", FD, FDSwap.class);
	}

	@Test void fd03_LocalDateTime_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56',f2:'FOO'}", new FDBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(LocalTime.class, new TemporalSwap.IsoLocalTime())
	//------------------------------------------------------------------------------------------------------------------
	private static LocalTime FE = LocalTime.parse("12:34:56");

	public static class FESwap extends StringSwap<LocalTime> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, LocalTime o) throws Exception {
			return "FOO";
		}
	}

	public static class FEBean {
		public LocalTime f1 = FE;
		@Swap(FESwap.class)
		public LocalTime f2 = FE;
	}

	@Test void fe01_LocalTime() throws Exception {
		test1("'12:34:56'", FE);
	}

	@Test void fe02_LocalTime_overrideSwap() throws Exception {
		test3("'FOO'", FE, FESwap.class);
	}

	@Test void fe03_LocalTime_overrideAnnotation() throws Exception {
		test1("{f1:'12:34:56',f2:'FOO'}", new FEBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(OffsetDateTime.class, new TemporalSwap.IsoOffsetDateTime())
	//------------------------------------------------------------------------------------------------------------------
	private static OffsetDateTime FF = OffsetDateTime.parse("2012-12-21T12:34:56-05:00");

	public static class FFSwap extends StringSwap<OffsetDateTime> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, OffsetDateTime o) throws Exception {
			return "FOO";
		}
	}

	public static class FFBean {
		public OffsetDateTime f1 = FF;
		@Swap(FFSwap.class)
		public OffsetDateTime f2 = FF;
	}

	@Test void ff01_OffsetDateTime() throws Exception {
		test1("'2012-12-21T12:34:56-05:00'", FF);
	}

	@Test void ff02_OffsetDateTime_overrideSwap() throws Exception {
		test3("'FOO'", FF, FFSwap.class);
	}

	@Test void ff03_OffsetDateTime_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56-05:00',f2:'FOO'}", new FFBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(OffsetTime.class, new TemporalSwap.IsoOffsetTime())
	//------------------------------------------------------------------------------------------------------------------
	private static OffsetTime FG = OffsetTime.parse("12:34:56-05:00");

	public static class FGSwap extends StringSwap<OffsetTime> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, OffsetTime o) throws Exception {
			return "FOO";
		}
	}

	public static class FGBean {
		public OffsetTime f1 = FG;
		@Swap(FGSwap.class)
		public OffsetTime f2 = FG;
	}

	@Test void fg01_OffsetTime() throws Exception {
		test1("'12:34:56-05:00'", FG);
	}

	@Test void fg02_OffsetTime_overrideSwap() throws Exception {
		test3("'FOO'", FG, FGSwap.class);
	}

	@Test void fg03_OffsetTime_overrideAnnotation() throws Exception {
		test1("{f1:'12:34:56-05:00',f2:'FOO'}", new FGBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Year.class, new TemporalSwap.IsoYear())
	//------------------------------------------------------------------------------------------------------------------
	private static Year FH = Year.parse("2012");

	public static class FHSwap extends StringSwap<Year> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Year o) throws Exception {
			return "FOO";
		}
	}

	public static class FHBean {
		public Year f1 = FH;
		@Swap(FHSwap.class)
		public Year f2 = FH;
	}

	@Test void fh01_Year() throws Exception {
		test1("'2012'", FH);
	}

	@Test void fh02_Year_overrideSwap() throws Exception {
		test3("'FOO'", FH, FHSwap.class);
	}

	@Test void fh03_Year_overrideAnnotation() throws Exception {
		test1("{f1:'2012',f2:'FOO'}", new FHBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(YearMonth.class, new TemporalSwap.IsoYearMonth())
	//------------------------------------------------------------------------------------------------------------------
	private static YearMonth FI = YearMonth.parse("2012-12");

	public static class FISwap extends StringSwap<YearMonth> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, YearMonth o) throws Exception {
			return "FOO";
		}
	}

	public static class FIBean {
		public YearMonth f1 = FI;
		@Swap(FISwap.class)
		public YearMonth f2 = FI;
	}

	@Test void fi01_YearMonth() throws Exception {
		test1("'2012-12'", FI);
	}

	@Test void fi02_YearMonth_overrideSwap() throws Exception {
		test3("'FOO'", FI, FISwap.class);
	}

	@Test void fi03_YearMonth_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12',f2:'FOO'}", new FIBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(Temporal.class, new TemporalSwap.IsoInstant())
	//------------------------------------------------------------------------------------------------------------------
	private static Temporal FJ = HijrahDate.from(FB);

	public static class FJSwap extends StringSwap<Temporal> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Temporal o) throws Exception {
			return "FOO";
		}
	}

	public static class FJBean {
		public Temporal f1 = FJ;
		@Swap(FJSwap.class)
		public Temporal f2 = FJ;
	}

	@Test void fj01_Temporal() throws Exception {
		test1("'2012-12-21T05:00:00Z'", FJ);
	}

	@Test void fj02_Temporal_overrideSwap() throws Exception {
		test3("'FOO'", FJ, FJSwap.class);
	}

	@Test void fj03_Temporal_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T05:00:00Z',f2:'FOO'}", new FJBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(TimeZone.class, new TimeZoneSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static TimeZone G = TimeZone.getTimeZone("Z");

	public static class GSwap extends StringSwap<TimeZone> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, TimeZone o) throws Exception {
			return "FOO";
		}
	}

	public static class GBean {
		public TimeZone f1 = G;
		@Swap(GSwap.class)
		public TimeZone f2 = G;
	}

	@Test void g01_TimeZone() throws Exception {
		test1("'GMT'", G);
	}

	@Test void g02_TimeZone_overrideSwap() throws Exception {
		test3("'FOO'", G, GSwap.class);
	}

	@Test void g03_TimeZone_overrideAnnotation() throws Exception {
		test1("{f1:'GMT',f2:'FOO'}", new GBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(XMLGregorianCalendar.class, new XMLGregorianCalendarSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static XMLGregorianCalendar H;
	static {
		try {
			H = DatatypeFactory.newInstance().newXMLGregorianCalendar("2012-12-21T12:34:56.789Z");
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}

	public static class HSwap extends StringSwap<XMLGregorianCalendar> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, XMLGregorianCalendar o) throws Exception {
			return "FOO";
		}
	}

	public static class HBean {
		public XMLGregorianCalendar f1 = H;
		@Swap(HSwap.class)
		public XMLGregorianCalendar f2 = H;
	}

	@Test void h01_XMLGregorianCalendar() throws Exception {
		test1("'2012-12-21T12:34:56.789Z'", H);
	}

	@Test void h02_XMLGregorianCalendar_overrideSwap() throws Exception {
		test3("'FOO'", H, HSwap.class);
	}

	@Test void h03_XMLGregorianCalendar_overrideAnnotation() throws Exception {
		test1("{f1:'2012-12-21T12:34:56.789Z',f2:'FOO'}", new HBean());
	}

	//------------------------------------------------------------------------------------------------------------------
	//	POJO_SWAPS.put(ZoneId.class, new ZoneIdSwap())
	//------------------------------------------------------------------------------------------------------------------
	private static ZoneId I = ZoneId.of("Z");

	public static class ISwap extends StringSwap<ZoneId> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, ZoneId o) throws Exception {
			return "FOO";
		}
	}

	public static class IBean {
		public ZoneId f1 = I;
		@Swap(ISwap.class)
		public ZoneId f2 = I;
	}

	@Test void i01_ZoneId() throws Exception {
		test1("'Z'", I);
	}

	@Test void i02_ZoneId_overrideSwap() throws Exception {
		test3("'FOO'", I, ISwap.class);
	}

	@Test void i03_ZoneId_overrideAnnotation() throws Exception {
		test1("{f1:'Z',f2:'FOO'}", new IBean());
	}
}