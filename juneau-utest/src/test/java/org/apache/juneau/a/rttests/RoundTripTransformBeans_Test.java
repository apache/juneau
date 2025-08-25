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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.DateUtils.*;
import static org.junit.Assert.*;
import static org.apache.juneau.common.internal.Utils.*;
import static org.apache.juneau.TestUtils.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"rawtypes","serial"})
class RoundTripTransformBeans_Test extends SimpleTestBase {

	private static RoundTripTester[] TESTERS = {
		tester("Json - default")
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Json - lax")
			.serializer(JsonSerializer.create().json5().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Json - lax, readable")
			.serializer(JsonSerializer.create().json5().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester("Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester("Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester("Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester("Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester("Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester("UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester("UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester("UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester("MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester("Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
	};

	static Stream<Arguments> testers() {
		return Stream.of(TESTERS).map(x -> args(x));
	}

	protected static RoundTripTester.Builder tester(String label) {
		return RoundTripTester.create(label).annotatedClasses(BcConfig.class, E1cConfig.class, F1cConfig.class, F2acConfig.class).pojoSwaps(D2.class, ByteArraySwap.Base64.class);
	}

	//====================================================================================================
	// testSwapBeans1
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_swapBeans1(RoundTripTester t) throws Exception {
		var x = new A().init();
		x = t.roundTrip(x, A.class);

		// ByteArrayBase64Swap
		assertEquals(3, x.fByte[3]);
		assertNull(x.fnByte);
		assertEquals(5, x.faByte[2][1]);
		assertEquals(6, x.flByte.get(1)[2]);
		assertNull(x.flByte.get(2));
		assertEquals(6, x.fmByte.get("bar")[2]);
		assertNull(x.fmByte.get("baz"));

		// CalendarSwap
		x.fCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, x.fCalendar.get(Calendar.YEAR));
		assertEquals(01, x.fCalendar.get(Calendar.MONTH));
		assertEquals(02, x.fCalendar.get(Calendar.DATE));
		assertEquals(03, x.fCalendar.get(Calendar.HOUR));
		assertEquals(04, x.fCalendar.get(Calendar.MINUTE));
		assertEquals(05, x.fCalendar.get(Calendar.SECOND));

		x.faCalendar[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, x.faCalendar[0].get(Calendar.YEAR));
		assertEquals(01, x.faCalendar[0].get(Calendar.MONTH));
		assertEquals(02, x.faCalendar[0].get(Calendar.DATE));
		assertEquals(03, x.faCalendar[0].get(Calendar.HOUR));
		assertEquals(04, x.faCalendar[0].get(Calendar.MINUTE));
		assertEquals(05, x.faCalendar[0].get(Calendar.SECOND));
		assertNull(x.fnCalendar);
		assertNull(x.fn2Calendar);

		// DateSwap
		assertEquals(1000, x.fDate.getTime());
		assertNull(x.fnDate);
		assertEquals(3000, x.faDate[2].getTime());
		assertEquals(4000, x.flDate.get(0).getTime());
		assertNull(x.flDate.get(1));
		assertEquals(5000, x.fmDate.get("foo").getTime());
		assertNull(x.fmDate.get("bar"));
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
			flByte = alist(new byte[]{1,2,3},new byte[]{4,5,6},null);
			fmByte = map("foo",new byte[]{1,2,3},"bar",new byte[]{4,5,6},"baz",null);

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
			flDate = alist(new Date(4000),null);
			fmDate = map("foo",new Date(5000),"bar",null);
			return this;
		}
	}


	//====================================================================================================
	// testSwapBeans2
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_swapBeans2(RoundTripTester t) throws Exception {
		var x = new A().init();
		x = t.roundTrip(x, A.class);

		// ByteArrayBase64Swap
		assertEquals(3, x.fByte[3]);
		assertNull(x.fnByte);
		assertEquals(5, x.faByte[2][1]);
		assertEquals(6, x.flByte.get(1)[2]);
		assertNull(x.flByte.get(2));
		assertEquals(6, x.fmByte.get("bar")[2]);
		assertNull(x.fmByte.get("baz"));

		// CalendarSwap
		x.fCalendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, x.fCalendar.get(Calendar.YEAR));
		assertEquals(01, x.fCalendar.get(Calendar.MONTH));

		x.faCalendar[0].setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2001, x.faCalendar[0].get(Calendar.YEAR));
		assertEquals(01, x.faCalendar[0].get(Calendar.MONTH));
		assertNull(x.fnCalendar);
		assertNull(x.fn2Calendar);

		// DateSwap
		assertEquals(1000, x.fDate.getTime() % 3600000);
		assertNull(x.fnDate);
		assertEquals(3000, x.faDate[2].getTime() % 3600000);
		assertEquals(4000, x.flDate.get(0).getTime() % 3600000);
		assertNull(x.flDate.get(1));
		assertEquals(5000, x.fmDate.get("foo").getTime() % 3600000);
		assertNull(x.fmDate.get("bar"));
	}

	//====================================================================================================
	// swaps - Bean.swaps annotation
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_swaps(RoundTripTester t) throws Exception {
		var x = new B();
		x.f1 = "bar";
		x = t.roundTrip(x, B.class);

		assertEquals("bar", x.f1);
	}

	@Swap(BSwap.class)
	public static class B {
		public String f1;
	}

	public static class BSwap extends StringSwap<B> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, B o) throws SerializeException {
			return o.f1;
		}
		@Override /* ObjectSwap */
		public B unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			var b1 = new B();
			b1.f1 = f;
			return b1;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_Swaps_usingConfig(RoundTripTester t) throws Exception {
		var x = new Bc();
		x.f1 = "bar";
		x = t.roundTrip(x, Bc.class);

		assertEquals("bar", x.f1);
	}

	@Swap(on="Dummy1",value=BcSwap.class)
	@Swap(on="Bc",value=BcSwap.class)
	@Swap(on="Dummy2",value=BcSwap.class)
	private static class BcConfig {}

	public static class Bc {
		public String f1;
	}

	public static class BcSwap extends StringSwap<Bc> {
		@Override /* ObjectSwap */
		public String swap(BeanSession session, Bc o) throws SerializeException {
			return o.f1;
		}
		@Override /* ObjectSwap */
		public Bc unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			var b1 = new Bc();
			b1.f1 = f;
			return b1;
		}
	}

	//====================================================================================================
	// testXMLGregorianCalendar - Test XMLGregorianCalendarSwap class.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_xmlGregorianCalendar(RoundTripTester t) throws Exception {

		if (t.isValidationOnly())
			return;

		var gc = new GregorianCalendar();
		var c = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

		var s = t.getSerializer();
		var p = t.getParser();

		var r = s.serialize(c);
		var c2 = p.parse(r, XMLGregorianCalendar.class);
		assertEquals(c, c2);
	}

	//====================================================================================================
	// testSubTypeWithGenerics
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a06_subTypeWithGenerics(RoundTripTester t) throws Exception {
		var s = JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build();

		var x = C3.create();
		var r = s.serialize(x);
		assertEquals("{\"_type\":\"C3\",\"f1\":{\"f2\":\"f2\",\"f3\":3}}", r);
	}


	@Bean(dictionary={C3.class})
	public interface C1<T> extends Serializable {
		void setF1(T f1);
		T getF1();
	}

	public abstract static class C2<T> implements C1<T> {
		protected T f1;
		@Override /* C1 */ public T getF1() { return f1; }
		@Override /* C1 */ public void setF1(T v) { f1 = v; }
	}

	@Bean(typeName="C3")
	public static class C3<T> extends C2<T> {

		public static C3 create() {
			var c3 = new C3<>();
			var cdto = new CDTO();
			cdto.f2 = "f2";
			cdto.f3 = 3;
			c3.f1 = cdto;
			return c3;
		}

		@Override /* C1 */ public T getF1() { return f1; }
		@Override /* C1 */ public void setF1(T v) { f1 = v; }
	}

	public static class CDTO {
		public String f2;
		public int f3;
	}

	//====================================================================================================
	// Surrogate transforms
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a07_surrogates(RoundTripTester t) throws Exception {
		var s = JsonSerializer.create().json5().swaps(D2.class).build();
		var p = JsonParser.create().swaps(D2.class).build();
		var x = D1.create();

		Object r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, D1.class);
		assertEquals("f1", x.f1);

		r = t.getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		t.roundTrip(x, D1.class);
	}

	public static class D1 {
		public String f1;

		public static D1 create() {
			var x = new D1();
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
			var x = new D1();
			x.f1 = this.f2;
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a08_surrogatesThroughAnnotation(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;
		var x = E1.create();

		Object r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, E1.class);
		assertEquals("f1", x.f1);

		r = t.getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		t.roundTrip(x, E1.class);
	}

	@Swap(E2.class)
	public static class E1 {
		public String f1;

		public static E1 create() {
			var x = new E1();
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
			var x = new E1();
			x.f1 = this.f2;
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a09_surrogatesThroughAnnotation_usingConfig(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(E1cConfig.class).build();
		var p = JsonParser.DEFAULT.copy().applyAnnotations(E1cConfig.class).build();
		var x = E1c.create();

		Object r = s.serialize(x);
		assertEquals("{f2:'f1'}", r);

		x = p.parse(r, E1c.class);
		assertEquals("f1", x.f1);

		r = t.getSerializer().serialize(x);
		assertTrue(toString(r).contains("f2"));

		t.roundTrip(x, E1c.class);
	}

	@Swap(on="Dummy1",value=E2c.class)
	@Swap(on="E1c",value=E2c.class)
	@Swap(on="Dummy2",value=E2c.class)
	private static class E1cConfig {}

	public static class E1c {
		public String f1;

		public static E1c create() {
			var x = new E1c();
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
			var x = new E1c();
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
		public Calendar getC() { return c; }
		public void setC(Calendar v) { c = v; }

		public static F1 create() {
			var x = new F1();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a10_swapOnPrivateField(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;

		var x = F1.create();

		var r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F1.class);
		assertJson(x, "{c:'2018-12-12T05:12:00'}");

		t.roundTrip(x, F1.class);
	}

	@Swap(on="Dummy1.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="F1c.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="Dummy2.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	private static class F1cConfig {}

	public static class F1c {

		private Calendar c;
		public Calendar getC() { return c; }
		public void setC(Calendar v) { c = v; }

		public static F1c create() {
			var x = new F1c();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a11_swapOnPrivateField_usingConfig(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(F1cConfig.class).build();
		var p = JsonParser.DEFAULT.copy().applyAnnotations(F1cConfig.class).build();

		var x = F1c.create();

		var r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F1c.class);
		assertSerialized(x, s, "{c:'2018-12-12T05:12:00'}");

		t.roundTrip(x, F1c.class);
	}

	public static class F2a {
		@Swap(TemporalCalendarSwap.IsoLocalDateTime.class)
		protected Calendar c;
	}

	public static class F2 extends F2a {

		public Calendar getC() { return c; }
		public void setC(Calendar v) { c = v; }

		public static F2 create() {
			var x = new F2();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a12_swapOnPrivateField_Inherited(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;

		var x = F2.create();

		var r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F2.class);
		assertJson(x, "{c:'2018-12-12T05:12:00'}");

		t.roundTrip(x, F2.class);
	}

	@Swap(on="Dummy1.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="F2ac.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	@Swap(on="Dummy2.c", value=TemporalCalendarSwap.IsoLocalDateTime.class)
	private static class F2acConfig {}

	public static class F2ac {
		protected Calendar c;
	}

	public static class F2c extends F2ac {

		public void setC(Calendar v) { c = v; }
		public Calendar getC() { return c; }

		public static F2c create() {
			var x = new F2c();
			x.setC(parseISO8601Calendar("2018-12-12T05:12:00"));
			return x;
		}
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a13_swapOnPrivateField_Inherited_usingConfig(RoundTripTester t) throws Exception {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(F2ac.class).build();
		var p = JsonParser.DEFAULT.copy().applyAnnotations(F2ac.class).build();

		var x = F2.create();

		var r = s.serialize(x);
		assertEquals("{c:'2018-12-12T05:12:00'}", r);

		x = p.parse(r, F2.class);
		assertJson(x, "{c:'2018-12-12T05:12:00'}");

		t.roundTrip(x, F2.class);
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
			var f = new F();
			f.f1 = 1;
			f.f2 = 2;
			return f;
		}
	}

	/**
	 * The create() method and copy constructor should not be confused as the classes Builder class.
	 */

	@ParameterizedTest
	@MethodSource("testers")
	void a14_beanWithIncompleteCopyConstructor(RoundTripTester t) throws Exception {
		var x = F.create();
		x = t.roundTrip(x);
		assertJson(x, "{f1:1,f2:2}");
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