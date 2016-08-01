/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import javax.xml.datatype.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.annotation.Filter;
import com.ibm.juno.core.filter.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","rawtypes","hiding","serial"})
public class CT_RoundTripFilterBeans extends RoundTripTest {

	public CT_RoundTripFilterBeans(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testFilterBeans1
	//====================================================================================================
	@Test
	public void testFilterBeans1() throws Exception {
		Class<?>[] f = {
			ByteArrayBase64Filter.class,
			CalendarFilter.ISO8601DTZ.class,
			DateFilter.ISO8601DTZ.class
		};
		s.addFilters(f);
		if (p != null)
			p.addFilters(f);
		A t = new A().init();
		t = roundTrip(t, A.class);

		// ByteArrayBase64Filter
		assertEquals(3, t.fByte[3]);
		assertNull(t.fnByte);
		assertEquals(5, t.faByte[2][1]);
		assertEquals(6, t.flByte.get(1)[2]);
		assertNull(t.flByte.get(2));
		assertEquals(6, t.fmByte.get("bar")[2]);
		assertNull(t.fmByte.get("baz"));

		// CalendarFilter
		t.fCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.fCalendar.get(Calendar.YEAR));
		assertEquals(01, t.fCalendar.get(Calendar.MONTH));
		assertEquals(02, t.fCalendar.get(Calendar.DATE));
		assertEquals(03, t.fCalendar.get(Calendar.HOUR));
		assertEquals(04, t.fCalendar.get(Calendar.MINUTE));
		assertEquals(05, t.fCalendar.get(Calendar.SECOND));

		t.faCalendar[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.faCalendar[0].get(Calendar.YEAR));
		assertEquals(01, t.faCalendar[0].get(Calendar.MONTH));
		assertEquals(02, t.faCalendar[0].get(Calendar.DATE));
		assertEquals(03, t.faCalendar[0].get(Calendar.HOUR));
		assertEquals(04, t.faCalendar[0].get(Calendar.MINUTE));
		assertEquals(05, t.faCalendar[0].get(Calendar.SECOND));
		assertNull(t.fnCalendar);
		assertNull(t.fn2Calendar);

		// DateFilter
		assertEquals(1000, t.fDate.getTime());
		assertNull(t.fnDate);
		assertEquals(3000, t.faDate[2].getTime());
		assertEquals(4000, t.flDate.get(0).getTime());
		assertNull(t.flDate.get(1));
		assertEquals(5000, t.fmDate.get("foo").getTime());
		assertNull(t.fmDate.get("bar"));
	}

	public static class A {

		// Test ByteArrayBase64Filter
		public byte[] fByte;
		public byte[] fnByte;
		public byte[][] faByte;
		public List<byte[]> flByte;
		public Map<String,byte[]> fmByte;

		public GregorianCalendar fCalendar;
		public GregorianCalendar fnCalendar;
		public Calendar fn2Calendar;
		public GregorianCalendar[] faCalendar;

		public Date fDate;
		public Date fnDate;
		public Date[] faDate;
		public List<Date> flDate;
		public Map<String,Date> fmDate;

		public A init() {
			fByte = new byte[]{0,1,2,3};
			fnByte = null;
			faByte = new byte[][]{{0,1},{2,3},{4,5}};
			flByte = new ArrayList<byte[]>() {{
				add(new byte[]{1,2,3});
				add(new byte[]{4,5,6});
				add(null);
			}};
			fmByte = new LinkedHashMap<String,byte[]>() {{
				put("foo", new byte[]{1,2,3});
				put("bar", new byte[]{4,5,6});
				put("baz", null);
			}};

			fCalendar = new GregorianCalendar() {{
				set(2001, 01, 02, 03, 04, 05);
				setTimeZone(TimeZone.getTimeZone("GMT"));
			}};
			fnCalendar = null;
			fn2Calendar = null;
			faCalendar = new GregorianCalendar[]{
				new GregorianCalendar() {{
					set(2001, 01, 02, 03, 04, 05);
					setTimeZone(TimeZone.getTimeZone("GMT"));
				}}
			};

			fDate = new Date(1000);
			fnDate = null;
			faDate = new Date[]{
				new Date(1000), new Date(2000), new Date(3000)
			};
			flDate = new ArrayList<Date>() {{
				add(new Date(4000));
				add(null);
			}};
			fmDate = new LinkedHashMap<String,Date>() {{
				put("foo", new Date(5000));
				put("bar", null);
			}};
			return this;
		}
	}


	//====================================================================================================
	// testFilterBeans2
	//====================================================================================================
	@Test
	public void testFilterBeans2() throws Exception {
		Class<?>[] f = {
			ByteArrayBase64Filter.class,
			CalendarFilter.Medium.class,
			DateFilter.RFC2822DT.class,
		};
		s.addFilters(f);
		if (p != null)
			p.addFilters(f);
		A t = new A().init();
		t = roundTrip(t, A.class);

		// ByteArrayBase64Filter
		assertEquals(3, t.fByte[3]);
		assertNull(t.fnByte);
		assertEquals(5, t.faByte[2][1]);
		assertEquals(6, t.flByte.get(1)[2]);
		assertNull(t.flByte.get(2));
		assertEquals(6, t.fmByte.get("bar")[2]);
		assertNull(t.fmByte.get("baz"));

		// CalendarFilter
		t.fCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.fCalendar.get(Calendar.YEAR));
		assertEquals(01, t.fCalendar.get(Calendar.MONTH));
		// Note: We lose precision on the following because of the filter type.
		//assertEquals(02, b.fCalendar.get(Calendar.DATE));
		//assertEquals(03, b.fCalendar.get(Calendar.HOUR));
		//assertEquals(04, b.fCalendar.get(Calendar.MINUTE));
		//assertEquals(05, b.fCalendar.get(Calendar.SECOND));

		t.faCalendar[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.faCalendar[0].get(Calendar.YEAR));
		assertEquals(01, t.faCalendar[0].get(Calendar.MONTH));
		// Note: We lose precision on the following because of the filter type.
		//assertEquals(02, b.faCalendar[0].get(Calendar.DATE));
		//assertEquals(03, b.faCalendar[0].get(Calendar.HOUR));
		//assertEquals(04, b.faCalendar[0].get(Calendar.MINUTE));
		//assertEquals(05, b.faCalendar[0].get(Calendar.SECOND));
		assertNull(t.fnCalendar);
		assertNull(t.fn2Calendar);

		// DateFilter
		assertEquals(1000, t.fDate.getTime());
		assertNull(t.fnDate);
		assertEquals(3000, t.faDate[2].getTime());
		assertEquals(4000, t.flDate.get(0).getTime());
		assertNull(t.flDate.get(1));
		assertEquals(5000, t.fmDate.get("foo").getTime());
		assertNull(t.fmDate.get("bar"));
	}

	//====================================================================================================
	// testFilter - Bean.filter annotation
	//====================================================================================================
	@Test
	public void testFilter() throws Exception {
		B t = new B();
		t.f1 = "bar";
		t = roundTrip(t, B.class);

		assertEquals("bar", t.f1);
	}

	@Filter(BFilter.class)
	public static class B {
		public String f1;
	}

	public static class BFilter extends PojoFilter<B,String> {
		@Override /* PojoFilter */
		public String filter(B o) throws SerializeException {
			return o.f1;
		}
		@Override /* PojoFilter */
		public B unfilter(String f, ClassMeta<?> hint) throws ParseException {
			B b1 = new B();
			b1.f1 = f;
			return b1;
		}
	}

	//====================================================================================================
	// testXMLGregorianCalendar - Test XMLGregorianCalendarFilter class.
	//====================================================================================================
	@Test
	public void testXMLGregorianCalendar() throws Exception {

		if (isValidationOnly())
			return;

		GregorianCalendar gc = new GregorianCalendar();
		XMLGregorianCalendar c = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

		WriterSerializer s = getSerializer().clone().addFilters(XMLGregorianCalendarFilter.class);
		ReaderParser p = getParser().clone().addFilters(XMLGregorianCalendarFilter.class);

		String r = s.serialize(c);
		XMLGregorianCalendar c2 = p.parse(r, XMLGregorianCalendar.class);
		assertEquals(c, c2);
	}

	//====================================================================================================
	// testSubTypeWithGenerics
	//====================================================================================================
	@Test
	public void testSubTypeWithGenerics() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT;

		C1 c1 = C3.create();
		String r = s.serialize(c1);
		assertEquals("{\"type\":\"C3\",\"f1\":{\"f2\":\"f2\",\"f3\":3}}", r);
	}


	@Bean(
		subTypeProperty="type",
		subTypes={
			@BeanSubType(id="C3", type=C3.class)
		}
	)
	public static interface C1<T> extends Serializable {
		void setF1(T f1);
		T getF1();
	}

	public abstract static class C2<T> implements C1<T> {
		protected T f1;

		@Override /* C1 */
		public void setF1(T f1) {
			this.f1 = f1;
		}

		@Override /* C1 */
		public T getF1() {
			return f1;
		}
	}

	public static class C3<T> extends C2<T> {

		public static C3 create() {
			C3 c3 = new C3<Object>();
			CDTO cdto = new CDTO();
			cdto.f2 = "f2";
			cdto.f3 = 3;
			c3.f1 = cdto;
			return c3;
		}

		@Override /* C1 */
		public void setF1(T f1) {
			this.f1 = f1;
		}

		@Override /* C1 */
		public T getF1() {
			return f1;
		}
	}

	public static class CDTO {
		public String f2;
		public int f3;
	}


	//====================================================================================================
	// Surrogate filters
	//====================================================================================================
	@Test
	public void testSurrogateFilter() throws Exception {
		addFilters(D2.class);

		JsonSerializer s = new JsonSerializer.Simple().addFilters(D2.class);
		JsonParser p = new JsonParser().addFilters(D2.class);
		String r;
		D1 d1 = D1.create();

		r = s.serialize(d1);
		assertEquals("{f2:'f1'}", r);

		d1 = p.parse(r, D1.class);
		assertEquals("f1", d1.f1);

		r = getSerializer().serialize(d1);
		assertTrue(r.contains("f2"));

		d1 = roundTrip(d1, D1.class);
	}

	public static class D1 {
		public String f1;

		public static D1 create() {
			D1 d1 = new D1();
			d1.f1 = "f1";
			return d1;
		}
	}

	public static class D2 {
		public String f2;
		public D2(D1 d1) {
			f2 = d1.f1;
		}
		public D2() {
		}
		public static D1 valueOf(D2 d2) {
			D1 d1 = new D1();
			d1.f1 = d2.f2;
			return d1;
		}
	}
}
