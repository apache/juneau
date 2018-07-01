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

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","rawtypes","serial"})
public class RoundTripTransformBeansTest extends RoundTripTest {

	public RoundTripTransformBeansTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testSwapBeans1
	//====================================================================================================
	@Test
	public void testSwapBeans1() throws Exception {
		Class<?>[] f = {
			ByteArrayBase64Swap.class,
			CalendarSwap.ISO8601DTZ.class,
			DateSwap.ISO8601DTZ.class
		};
		pojoSwaps(f);
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
			flByte = new AList<byte[]>()
				.append(new byte[]{1,2,3})
				.append(new byte[]{4,5,6})
				.append(null)
			;
			fmByte = new AMap<String,byte[]>()
				.append("foo", new byte[]{1,2,3})
				.append("bar", new byte[]{4,5,6})
				.append("baz", null)
			;

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
			flDate = new AList<Date>()
				.append(new Date(4000))
				.append(null)
			;
			fmDate = new AMap<String,Date>()
				.append("foo", new Date(5000))
				.append("bar", null)
			;
			return this;
		}
	}


	//====================================================================================================
	// testSwapBeans2
	//====================================================================================================
	@Test
	public void testSwapBeans2() throws Exception {
		Class<?>[] f = {
			ByteArrayBase64Swap.class,
			CalendarSwap.DateMedium.class,
			DateSwap.RFC2822DT.class,
		};
		pojoSwaps(f);
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
	// testSwaps - Bean.pojoSwaps annotation
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

	//====================================================================================================
	// testXMLGregorianCalendar - Test XMLGregorianCalendarSwap class.
	//====================================================================================================
	@Test
	public void testXMLGregorianCalendar() throws Exception {

		if (isValidationOnly())
			return;

		GregorianCalendar gc = new GregorianCalendar();
		XMLGregorianCalendar c = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

		Serializer s = getSerializer().builder().pojoSwaps(XMLGregorianCalendarSwap.class).build();
		Parser p = getParser().builder().pojoSwaps(XMLGregorianCalendarSwap.class).build();

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


	@Bean(beanDictionary={C3.class})
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
		pojoSwaps(D2.class);

		JsonSerializer s = JsonSerializer.create().ssq().pojoSwaps(D2.class).build();
		JsonParser p = JsonParser.create().pojoSwaps(D2.class).build();
		Object r;
		D1 x = D1.create();

		r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, D1.class);
		assertEquals("f1", x.f1);

		r = getSerializer().serialize(x);
		assertTrue(TestUtils.toString(r).contains("f2"));

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
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		JsonParser p = JsonParser.DEFAULT;
		Object r;
		E1 x = E1.create();

		r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, E1.class);
		assertEquals("f1", x.f1);

		r = getSerializer().serialize(x);
		assertTrue(TestUtils.toString(r).contains("f2"));

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
}
