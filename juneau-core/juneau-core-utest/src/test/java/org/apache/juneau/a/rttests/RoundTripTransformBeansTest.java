// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.a.rttests;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.DateUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","rawtypes","serial"})
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripTransformBeansTest extends RoundTripTest {

	public RoundTripTransformBeansTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s == null ? null : s.applyAnnotations(BcConfig.class, E1cConfig.class, F1cConfig.class, F2acConfig.class), p == null ? null : p.applyAnnotations(BcConfig.class, E1cConfig.class, F1cConfig.class, F2acConfig.class), flags);

	}

	//====================================================================================================
	// testSwapBeans1
	//====================================================================================================
	@Test
	public void testSwapBeans1() throws Exception {
		Object[] f = {
			ByteArraySwap.Base64.class
		};
		swaps(f);
		A t = new A().init();
		t = roundTrip(t, A.class);

		// ByteArrayBase64Swap
		assertEquals(3, t.fByte[3]);
		assertNull(t.fnByte);
		assertEquals(5, t.faByte[2][1]);
		assertEquals(6, t.flByte.get(1)[2]);
		assertNull(t.flByte.get(2));
		assertEquals(6, t.fmByte.get("bar")[2]);
		assertNull(t.fmByte.get("baz"));

		// CalendarSwap
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

		// DateSwap
		assertEquals(1000, t.fDate.getTime());
		assertNull(t.fnDate);
		assertEquals(3000, t.faDate[2].getTime());
		assertEquals(4000, t.flDate.get(0).getTime());
		assertNull(t.flDate.get(1));
		assertEquals(5000, t.fmDate.get("foo").getTime());
		assertNull(t.fmDate.get("bar"));
	}

	public static class A {

		// Test ByteArrayBase64Swap
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
			flByte = AList.of(new byte[]{1,2,3},new byte[]{4,5,6},null);
			fmByte = AMap.of("foo",new byte[]{1,2,3},"bar",new byte[]{4,5,6},"baz",null);

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
			flDate = AList.of(new Date(4000),null);
			fmDate = AMap.of("foo",new Date(5000),"bar",null);
			return this;
		}
	}


	//====================================================================================================
	// testSwapBeans2
	//====================================================================================================
	@Test
	public void testSwapBeans2() throws Exception {
		Object[] f = {
			ByteArraySwap.Base64.class
		};
		swaps(f);
		A t = new A().init();
		t = roundTrip(t, A.class);

		// ByteArrayBase64Swap
		assertEquals(3, t.fByte[3]);
		assertNull(t.fnByte);
		assertEquals(5, t.faByte[2][1]);
		assertEquals(6, t.flByte.get(1)[2]);
		assertNull(t.flByte.get(2));
		assertEquals(6, t.fmByte.get("bar")[2]);
		assertNull(t.fmByte.get("baz"));

		// CalendarSwap
		t.fCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.fCalendar.get(Calendar.YEAR));
		assertEquals(01, t.fCalendar.get(Calendar.MONTH));
		// Note: We lose precision on the following because of the transform type.
		//assertEquals(02, b.fCalendar.get(Calendar.DATE));
		//assertEquals(03, b.fCalendar.get(Calendar.HOUR));
		//assertEquals(04, b.fCalendar.get(Calendar.MINUTE));
		//assertEquals(05, b.fCalendar.get(Calendar.SECOND));

		t.faCalendar[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, t.faCalendar[0].get(Calendar.YEAR));
		assertEquals(01, t.faCalendar[0].get(Calendar.MONTH));
		// Note: We lose precision on the following because of the transform type.
		//assertEquals(02, b.faCalendar[0].get(Calendar.DATE));
		//assertEquals(03, b.faCalendar[0].get(Calendar.HOUR));
		//assertEquals(04, b.faCalendar[0].get(Calendar.MINUTE));
		//assertEquals(05, b.faCalendar[0].get(Calendar.SECOND));
		assertNull(t.fnCalendar);
		assertNull(t.fn2Calendar);

		// DateSwap
		assertEquals(1000, t.fDate.getTime() % 3600000);
		assertNull(t.fnDate);
		assertEquals(3000, t.faDate[2].getTime() % 3600000);
		assertEquals(4000, t.flDate.get(0).getTime() % 3600000);
		assertNull(t.flDate.get(1));
		assertEquals(5000, t.fmDate.get("foo").getTime() % 3600000);
		assertNull(t.fmDate.get("bar"));
	}

	//====================================================================================================
	// swaps - Bean.swaps annotation
	//====================================================================================================
	@Test
	public void testSwaps() throws Exception {
		B t = new B();
		t.f1 = "bar";
		t = roundTrip(t, B.class);

		assertEquals("bar", t.f1);
	}

	@Swap(BSwap.class)
	public static class B {
		public String f1;
	}

	public static class BSwap extends StringSwap<B> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, B o) throws SerializeException {
			return o.f1;
		}
		@Override /* PojoSwap */
		public B unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			B b1 = new B();
			b1.f1 = f;
			return b1;
		}
	}

	@Test
	public void testSwaps_usingConfig() throws Exception {
		Bc t = new Bc();
		t.f1 = "bar";
		t = roundTrip(t, Bc.class);

		assertEquals("bar", t.f1);
	}

	@Swap(on="Dummy1",value=BcSwap.class)
	@Swap(on="Bc",value=BcSwap.class)
	@Swap(on="Dummy2",value=BcSwap.class)
	private static class BcConfig {}

	public static class Bc {
		public String f1;
	}

	public static class BcSwap extends StringSwap<Bc> {
		@Override /* PojoSwap */
		public String swap(BeanSession session, Bc o) throws SerializeException {
			return o.f1;
		}
		@Override /* PojoSwap */
		public Bc unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			Bc b1 = new Bc();
			b1.f1 = f;
			return b1;
		}
	}

	//====================================================================================================
	// testXMLGregorianCalendar - Test XMLGregorianCalendarSwap class.
	//====================================================================================================
	@Test
	public void testXMLGregorianCalendar() throws Exception {

		if (isValidationOnly())
			return;

		GregorianCalendar gc = new GregorianCalendar();
		XMLGregorianCalendar c = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

		Serializer s = getSerializer();
		Parser p = getParser();

		Object r = s.serialize(c);
		XMLGregorianCalendar c2 = p.parse(r, XMLGregorianCalendar.class);
		assertEquals(c, c2);
	}

	//====================================================================================================
	// testSubTypeWithGenerics
	//====================================================================================================
	@Test
	public void testSubTypeWithGenerics() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build();

		C1 c1 = C3.create();
		String r = s.serialize(c1);
		assertEquals("{\"_type\":\"C3\",\"f1\":{\"f2\":\"f2\",\"f3\":3}}", r);
	}


	@Bean(dictionary={C3.class})
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

	@Bean(typeName="C3")
	public static class C3<T> extends C2<T> {

		public static C3 create() {
			C3 c3 = new C3<>();
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
	// Surrogate transforms
	//====================================================================================================
	@Test
	public void testSurrogates() throws Exception {
		swaps(D2.class);

		JsonSerializer s = JsonSerializer.create().ssq().swaps(D2.class).build();
		JsonParser p = JsonParser.create().swaps(D2.class).build();
		Object r;
		D1 x = D1.create();

		r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, D1.class);
		assertEquals("f1", x.f1);

		r = getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		x = roundTrip(x, D1.class);
	}

	public static class D1 {
		public String f1;

		public static D1 create() {
			D1 x = new D1();
			x.f1 = "f1";
			return x;
		}
	}

	public static class D2 implements Surrogate {
		public String f2;
		public D2(D1 x) {
			f2 = x.f1;
		}
		public D2() {}
		public D1 create() {
			D1 x = new D1();
			x.f1 = this.f2;
			return x;
		}
	}

	@Test
	public void testSurrogatesThroughAnnotation() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT;
		JsonParser p = JsonParser.DEFAULT;
		Object r;
		E1 x = E1.create();

		r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, E1.class);
		assertEquals("f1", x.f1);

		r = getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		x = roundTrip(x, E1.class);
	}

	@Swap(E2.class)
	public static class E1 {
		public String f1;

		public static E1 create() {
			E1 x = new E1();
			x.f1 = "f1";
			return x;
		}
	}

	public static class E2 implements Surrogate {
		public String f2;
		public E2(E1 x) {
			f2 = x.f1;
		}
		public E2() {}
		public E1 create() {
			E1 x = new E1();
			x.f1 = this.f2;
			return x;
		}
	}

	@Test
	public void testSurrogatesThroughAnnotation_usingConfig() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(E1cConfig.class).build();
		JsonParser p = JsonParser.DEFAULT.builder().applyAnnotations(E1cConfig.class).build();
		Object r;
		E1c x = E1c.create();

		r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, E1c.class);
		assertEquals("f1", x.f1);

		r = getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		x = roundTrip(x, E1c.class);
	}

	@Swap(on="Dummy1",value=E2c.class)
	@Swap(on="E1c",value=E2c.class)
	@Swap(on="Dummy2",value=E2c.class)
	private static class E1cConfig {}

	public static class E1c {
		public String f1;

		public static E1c create() {
			E1c x = new E1c();
			x.f1 = "f1";
			return x;
		}
	}

	public static class E2c implements Surrogate {
		public String f2;
		public E2c(E1c x) {
			f2 = x.f1;
		}
		public E2c() {}
		public E1c create() {
			E1c x = new E1c();
			x.f1 = this.f2;
			return x;
		}
	}

	//====================================================================================================
	// Transforms on private fields.
	//====================================================================================================

	public static class F1 {

		@Swap(TemporalCalendarSwap.IsoLocalDateTime.class)
		private Calendar c;

		public void setC(Calendar c) {
			this.c = c;
		}

		public Calendar getC() {
			return c;
		}

		public static F1 create() {
			F1 x = new F1();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@Test
	public void testSwapOnPrivateField() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT;
		JsonParser p = JsonParser.DEFAULT;

		F1 x = F1.create();
		String r = null;

		r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F1.class);
		assertObject(x).json().is("{c:'2018-12-12T05:12:00'}");

		x = roundTrip(x, F1.class);
	}

	@Swap(on="Dummy1.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="F1c.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="Dummy2.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	private static class F1cConfig {}

	public static class F1c {

		private Calendar c;

		public void setC(Calendar c) {
			this.c = c;
		}

		public Calendar getC() {
			return c;
		}

		public static F1c create() {
			F1c x = new F1c();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@Test
	public void testSwapOnPrivateField_usingConfig() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(F1cConfig.class).build();
		JsonParser p = JsonParser.DEFAULT.builder().applyAnnotations(F1cConfig.class).build();

		F1c x = F1c.create();
		String r = null;

		r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F1c.class);
		assertObject(x).serialized(s).is("{c:'2018-12-12T05:12:00'}");

		x = roundTrip(x, F1c.class);
	}

	public static class F2a {

		@Swap(TemporalCalendarSwap.IsoLocalDateTime.class)
		protected Calendar c;

	}

	public static class F2 extends F2a {

		public void setC(Calendar c) {
			this.c = c;
		}

		public Calendar getC() {
			return c;
		}

		public static F2 create() {
			F2 x = new F2();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@Test
	public void testSwapOnPrivateField_Inherited() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT;
		JsonParser p = JsonParser.DEFAULT;

		F2 x = F2.create();
		String r = null;

		r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F2.class);
		assertObject(x).json().is("{c:'2018-12-12T05:12:00'}");

		x = roundTrip(x, F2.class);
	}

	@Swap(on="Dummy1.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="F2ac.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="Dummy2.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	private static class F2acConfig {}

	public static class F2ac {
		protected Calendar c;
	}

	public static class F2c extends F2ac {

		public void setC(Calendar c) {
			this.c = c;
		}

		public Calendar getC() {
			return c;
		}

		public static F2c create() {
			F2c x = new F2c();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@Test
	public void testSwapOnPrivateField_Inherited_usingConfig() throws Exception {
		JsonSerializer s = SimpleJsonSerializer.DEFAULT.builder().applyAnnotations(F2ac.class).build();
		JsonParser p = JsonParser.DEFAULT.builder().applyAnnotations(F2ac.class).build();

		F2 x = F2.create();
		String r = null;

		r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F2.class);
		assertObject(x).serialized(s).is("{c:'2018-12-12T05:12:00'}");

		x = roundTrip(x, F2.class);
	}

	//==================================================================================================================
	// testBeanWithIncompleteCopyConstructor
	//==================================================================================================================

	public static class F {
		public int f1, f2;

		public F() {}

		public F(F c) {
			this.f1 = c.f1;
		}

		public static F create() {
			F f = new F();
			f.f1 = 1;
			f.f2 = 2;
			return f;
		}
	}

	/**
	 * The create() method and copy constructor should not be confused as the classes Builder class.
	 */
	@Test
	public void testBeanWithIncompleteCopyConstructor() throws Exception {
		F f = F.create();
		f = roundTrip(f);
		assertObject(f).json().is("{f1:1,f2:2}");
	}


	//------------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//------------------------------------------------------------------------------------------------------------------

	private static final String toString(Object o) {
		if (o == null)
			return null;
		if (o instanceof String)
			return (String)o;
		if (o instanceof byte[])
			return new String((byte[])o, UTF8);
		return o.toString();
	}
}
