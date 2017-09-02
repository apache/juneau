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

import static org.apache.juneau.a.rttests.RoundTripTest.Flags.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings("javadoc")
public class RoundTripAddClassAttrsTest extends RoundTripTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getPairs() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */
				"JsonSerializer.DEFAULT/JsonParser.DEFAULT",
				new JsonSerializerBuilder().addBeanTypeProperties(true),
				new JsonParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 1 */
				"JsonSerializer.DEFAULT_LAX/JsonParser.DEFAULT",
				new JsonSerializerBuilder().simple().addBeanTypeProperties(true),
				new JsonParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 2 */
				"JsonSerializer.DEFAULT_SQ/JsonParser.DEFAULT",
				new JsonSerializerBuilder().simple().addBeanTypeProperties(true),
				new JsonParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 3 */
				"XmlSerializer.DEFAULT/XmlParser.DEFAULT",
				new XmlSerializerBuilder().addBeanTypeProperties(true),
				new XmlParserBuilder().useInterfaceProxies(false),
				CHECK_XML_WHITESPACE | VALIDATE_XML
			},
			{ /* 4 */
				"HtmlSerializer.DEFAULT/HtmlParser.DEFAULT",
				new HtmlSerializerBuilder().addBeanTypeProperties(true),
				new HtmlParserBuilder().useInterfaceProxies(false),
				CHECK_XML_WHITESPACE
			},
			{ /* 5 */
				"UonSerializer.DEFAULT_ENCODING/UonParser.DEFAULT_DECODING",
				new UonSerializerBuilder().encoding().addBeanTypeProperties(true),
				new UonParserBuilder().decoding().useInterfaceProxies(false),
				0
			},
			{ /* 6 */
				"UonSerializer.DEFAULT/UonParser.DEFAULT",
				new UonSerializerBuilder().addBeanTypeProperties(true),
				new UonParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 7 */
				"UrlEncodingSerializer.DEFAULT/UrlEncodingParser.DEFAULT",
				new UrlEncodingSerializerBuilder().addBeanTypeProperties(true),
				new UrlEncodingParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 8 */
				"RdfSerializer.Xml/RdfParser.Xml",
				new RdfSerializerBuilder().addBeanTypeProperties(true),
				new RdfParserBuilder().useInterfaceProxies(false),
				0
			},
			{ /* 9 */
				"MsgPackSerializer.DEFAULT/MsgPackParser.DEFAULT",
				new MsgPackSerializerBuilder().addBeanTypeProperties(true),
				new MsgPackParserBuilder().useInterfaceProxies(false),
				0
			}
		});
	}

	public RoundTripAddClassAttrsTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	@Override
	public Class<?>[] getDictionary() {
		return new Class<?>[]{A.class, B.class, C.class, D.class, E.class, F.class};
	}

	//====================================================================================================
	// testBean
	//====================================================================================================
	@Test
	public void testBean() throws Exception {
		A t = new A("foo");
		AA ta;
		IA ti;

		t = roundTrip(t, A.class);
		assertEquals("foo", t.getF1());

		ta = roundTrip(t, AA.class);
		assertEquals("foo", ta.getF1());

		ti = roundTrip(t, IA.class);
		assertEquals("foo", ti.getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.getF1());
	}

	public static interface IA {
		public String getF1();
		public void setF1(String f1);
	}

	public static abstract class AA implements IA {
	}

	@Bean(typeName="A")
	public static class A extends AA {
		private String f1;

		@Override /* AA */
		public String getF1() {
			return f1;
		}
		@Override /* AA */
		public void setF1(String f1) {
			this.f1 = f1;
		}

		public A() {}
		public A(String f1) {
			this.f1 = f1;
		}
	}

	//====================================================================================================
	// testBeanArray
	//====================================================================================================
	@Test
	public void testBeanArray() throws Exception {
		A[] t = {new A("foo")};
		AA[] ta;
		IA[] ti;

		t = roundTrip(t, A[].class);
		assertEquals("foo", t[0].getF1());

		ta = roundTrip(t, AA[].class);
		assertEquals("foo", ta[0].getF1());

		ti = roundTrip(t, IA[].class);
		assertEquals("foo", ti[0].getF1());
	}

	//====================================================================================================
	// testBeanWithBeanProps
	//====================================================================================================
	@Test
	public void testBeanWithBeanProps() throws Exception {
		B t = new B("foo");
		t = roundTrip(t, B.class);
		assertEquals("foo", t.f2a.getF1());
		assertEquals("foo", t.f2b.getF1());
		assertEquals("foo", t.f2c.getF1());
		assertEquals("foo", ((A)t.f2d).getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.f2a.getF1());
		assertEquals("foo", t.f2b.getF1());
		assertEquals("foo", t.f2c.getF1());
		assertEquals("foo", ((A)t.f2d).getF1());
	}

	@Bean(typeName="B")
	public static class B {
		public A f2a;
		public AA f2b;
		public IA f2c;
		public Object f2d;
		public B() {}
		public B(String f1) {
			f2d = f2c = f2b = f2a = new A(f1);
		}
	}

	//====================================================================================================
	// testMapsWithTypeParams - Maps with type parameters should not have class attributes on entries.
	//====================================================================================================
	@Test
	public void testMapsWithTypeParams() throws Exception {
		C t = new C("foo");
		t = roundTrip(t, C.class);
		assertEquals("foo", t.f3a.get("foo").getF1());
		assertEquals("foo", t.f3b.get("foo").getF1());
		assertEquals("foo", t.f3c.get("foo").getF1());
		assertEquals("foo", t.f3d.get("foo").getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.f3a.get("foo").getF1());
		assertEquals("foo", t.f3b.get("foo").getF1());
		assertEquals("foo", t.f3c.get("foo").getF1());
		assertEquals("foo", t.f3d.get("foo").getF1());
	}

	@Bean(typeName="C")
	public static class C {
		public Map<String,A> f3a = new HashMap<String,A>();
		public Map<String,A> f3b = new HashMap<String,A>();
		public Map<String,A> f3c = new HashMap<String,A>();
		public Map<String,A> f3d = new HashMap<String,A>();

		public C(){}
		public C(String f1) {
			A b = new A(f1);
			f3a.put("foo", b);
			f3b.put("foo", b);
			f3c.put("foo", b);
			f3d.put("foo", b);
		}
	}

	//====================================================================================================
	// testMapsWithoutTypeParams - Maps without type parameters should have class attributes on entries.
	//====================================================================================================
	@Test
	public void testMapsWithoutTypeParams() throws Exception {
		D t = new D("foo");
		t = roundTrip(t, D.class);
		assertEquals("foo", t.f4a[0].getF1());
		assertEquals("foo", t.f4b[0].getF1());
		assertEquals("foo", t.f4c[0].getF1());
		assertEquals("foo", ((A)t.f4d[0]).getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.f4a[0].getF1());
		assertEquals("foo", t.f4b[0].getF1());
		assertEquals("foo", t.f4c[0].getF1());
		assertEquals("foo", ((A)t.f4d[0]).getF1());
	}

	@Bean(typeName="D")
	public static class D {
		public A[] f4a;
		public AA[] f4b;
		public IA[] f4c;
		public Object[] f4d;

		public D(){}
		public D(String f1) {
			A b = new A(f1);
			f4a = new A[]{b};
			f4b = new AA[]{b};
			f4c = new IA[]{b};
			f4d = new Object[]{b};
		}
	}

	//====================================================================================================
	// testBeanWithListProps
	//====================================================================================================
	@Test
	public void testBeanWithListProps() throws Exception {
		E t = new E("foo");
		t = roundTrip(t, E.class);
		assertEquals("foo", t.f5a.get(0).getF1());
		assertEquals("foo", t.f5b.get(0).getF1());
		assertEquals("foo", t.f5c.get(0).getF1());
		assertEquals("foo", ((A)t.f5d.get(0)).getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.f5a.get(0).getF1());
		assertEquals("foo", t.f5b.get(0).getF1());
		assertEquals("foo", t.f5c.get(0).getF1());
		assertEquals("foo", ((A)t.f5d.get(0)).getF1());
	}

	@Bean(typeName="E")
	public static class E {
		public List<A> f5a = new LinkedList<A>();
		public List<AA> f5b = new LinkedList<AA>();
		public List<IA> f5c = new LinkedList<IA>();
		public List<Object> f5d = new LinkedList<Object>();

		public E(){}
		public E(String f1) {
			A b = new A(f1);
			f5a.add(b);
			f5b.add(b);
			f5c.add(b);
			f5d.add(b);
		}
	}

	//====================================================================================================
	// testBeanWithListOfArraysProps
	//====================================================================================================
	@Test
	public void testBeanWithListOfArraysProps() throws Exception {
		F t = new F("foo");
		t = roundTrip(t, F.class);
		assertEquals("foo", t.f6a.get(0)[0].getF1());
		assertEquals("foo", t.f6b.get(0)[0].getF1());
		assertEquals("foo", t.f6c.get(0)[0].getF1());
		assertEquals("foo", ((A)t.f6d.get(0)[0]).getF1());

		t = roundTrip(t, Object.class);
		assertEquals("foo", t.f6a.get(0)[0].getF1());
		assertEquals("foo", t.f6b.get(0)[0].getF1());
		assertEquals("foo", t.f6c.get(0)[0].getF1());
		assertEquals("foo", ((A)t.f6d.get(0)[0]).getF1());
	}

	@Bean(typeName="F")
	public static class F {
		public List<A[]> f6a = new LinkedList<A[]>();
		public List<AA[]> f6b = new LinkedList<AA[]>();
		public List<IA[]> f6c = new LinkedList<IA[]>();
		public List<Object[]> f6d = new LinkedList<Object[]>();

		public F(){}
		public F(String f1) {
			A[] b = {new A(f1)};
			f6a.add(b);
			f6b.add(b);
			f6c.add(b);
			f6d.add(b);
		}
	}
}
