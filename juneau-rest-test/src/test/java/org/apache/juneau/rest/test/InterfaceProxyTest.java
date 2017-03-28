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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.test.InterfaceProxy.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.test.InterfaceProxy.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@SuppressWarnings("unused")
@RunWith(Parameterized.class)
public class InterfaceProxyTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT, JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT, HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT, UonParser.DEFAULT },
			//{ /* 7 */ "RdfXml", RdfSerializer.DEFAULT_XMLABBREV, RdfParser.DEFAULT_XML },
		});
	}

	private Serializer serializer;
	private Parser parser;

	public InterfaceProxyTest(String label, Serializer serializer, Parser parser) {
		this.serializer = serializer;
		this.parser = parser;
	}

	private InterfaceProxy getProxy() {
		return getClient(serializer, parser).getRemoteableProxy(InterfaceProxy.class, "/testInterfaceProxyResource/proxy");
	}

	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void returnVoid() {
		getProxy().returnVoid();
	}

	@Test
	public void returnInteger() {
		assertEquals((Integer)1, getProxy().returnInteger());
	}

	@Test
	public void returnInt() {
		assertEquals(1, getProxy().returnInt());
	}

	@Test
	public void returnBoolean() {
		assertEquals(true, getProxy().returnBoolean());
	}

	@Test
	public void returnFloat() {
		assertTrue(1f == getProxy().returnFloat());
	}

	@Test
	public void returnFloatObject() {
		assertTrue(1f == getProxy().returnFloatObject());
	}

	@Test
	public void returnString() {
		assertEquals("foobar", getProxy().returnString());
	}

	@Test
	public void returnNullString() {
		assertNull(getProxy().returnNullString());
	}

	@Test
	public void returnInt3dArray() {
		assertObjectEquals("[[[1,2],null],null]", getProxy().returnInt3dArray());
	}

	@Test
	public void returnInteger3dArray() {
		assertObjectEquals("[[[1,null],null],null]", getProxy().returnInteger3dArray());
	}

	@Test
	public void returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null],null],null]", getProxy().returnString3dArray());
	}

	@Test
	public void returnIntegerList() {
		List<Integer> x = getProxy().returnIntegerList();
		assertObjectEquals("[1,null]", x);
		assertEquals(Integer.class, x.get(0).getClass());
	}

	@Test
	public void returnInteger3dList() {
		List<List<List<Integer>>> x = getProxy().returnInteger3dList();
		assertObjectEquals("[[[1,null],null],null]", x);
		assertEquals(Integer.class, x.get(0).get(0).get(0).getClass());
	}

	@Test
	public void returnInteger1d3dList() {
		List<Integer[][][]> x = getProxy().returnInteger1d3dList();
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertEquals(Integer.class, x.get(0)[0][0][0].getClass());
	}

	@Test
	public void returnInt1d3dList() {
		List<int[][][]> x = getProxy().returnInt1d3dList();
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertEquals(int[][][].class, x.get(0).getClass());
	}

	@Test
	public void returnStringList() {
		assertObjectEquals("['foo','bar',null]", getProxy().returnStringList());
	}

	// Beans
	@Test
	public void returnBean() {
		Bean x = getProxy().returnBean();
		assertObjectEquals("{a:1,b:'foo'}", x);
		assertEquals(InterfaceProxy.Bean.class, x.getClass());
	}

	@Test
	public void returnBean3dArray() {
		Bean[][][] x = getProxy().returnBean3dArray();
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
		assertEquals(InterfaceProxy.Bean.class, x[0][0][0].getClass());
	}

	@Test
	public void returnBeanList() {
		List<Bean> x = getProxy().returnBeanList();
		assertObjectEquals("[{a:1,b:'foo'}]", x);
		assertEquals(InterfaceProxy.Bean.class, x.get(0).getClass());
	}

	@Test
	public void returnBean1d3dList() {
		List<Bean[][][]> x = getProxy().returnBean1d3dList();
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
		assertEquals(InterfaceProxy.Bean.class, x.get(0)[0][0][0].getClass());
	}

	@Test
	public void returnBeanMap() {
		Map<String,Bean> x = getProxy().returnBeanMap();
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
		assertEquals(InterfaceProxy.Bean.class, x.get("foo").getClass());
	}

	@Test
	public void returnBeanListMap() {
		Map<String,List<Bean>> x = getProxy().returnBeanListMap();
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
		assertEquals(InterfaceProxy.Bean.class, x.get("foo").get(0).getClass());
	}

	@Test
	public void returnBean1d3dListMap() {
		Map<String,List<Bean[][][]>> x = getProxy().returnBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
		assertEquals(InterfaceProxy.Bean.class, x.get("foo").get(0)[0][0][0].getClass());
	}

	@Test
	public void returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<Bean>> x = getProxy().returnBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);
		assertEquals(Integer.class, x.keySet().iterator().next().getClass());
	}

	// Swapped POJOs
	@Test
	public void returnSwappedPojo() {
		SwappedPojo x = getProxy().returnSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void returnSwappedPojo3dArray() {
		SwappedPojo[][][] x = getProxy().returnSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void returnSwappedPojoMap() {
		Map<SwappedPojo,SwappedPojo> x = getProxy().returnSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void returnSwappedPojo3dMap() {
		Map<SwappedPojo,SwappedPojo[][][]> x = getProxy().returnSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs
	@Test
	public void returnImplicitSwappedPojo() {
		ImplicitSwappedPojo x = getProxy().returnImplicitSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojo3dArray() {
		ImplicitSwappedPojo[][][] x = getProxy().returnImplicitSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojoMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x = getProxy().returnImplicitSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojo3dMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x = getProxy().returnImplicitSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums
	@Test
	public void returnEnum() {
		TestEnum x = getProxy().returnEnum();
		assertObjectEquals("'TWO'", x);
	}

	@Test
	public void returnEnum3d() {
		TestEnum[][][] x = getProxy().returnEnum3d();
		assertObjectEquals("[[['TWO',null],null],null]", x);
		assertEquals(TestEnum.class, x[0][0][0].getClass());
	}

	@Test
	public void returnEnumList() {
		List<TestEnum> x = getProxy().returnEnumList();
		assertObjectEquals("['TWO',null]", x);
		assertEquals(TestEnum.class, x.get(0).getClass());
	}

	@Test
	public void returnEnum3dList() {
		List<List<List<TestEnum>>> x = getProxy().returnEnum3dList();
		assertObjectEquals("[[['TWO',null],null,null]]", x);
		assertEquals(TestEnum.class, x.get(0).get(0).get(0).getClass());
	}

	@Test
	public void returnEnum1d3dList() {
		List<TestEnum[][][]> x = getProxy().returnEnum1d3dList();
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertEquals(TestEnum[][][].class, x.get(0).getClass());
	}

	@Test
	public void returnEnumMap() {
		Map<TestEnum,TestEnum> x = getProxy().returnEnumMap();
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertEquals(TestEnum.class, e.getKey().getClass());
		assertEquals(TestEnum.class, e.getValue().getClass());
	}

	@Test
	public void returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = getProxy().returnEnum3dArrayMap();
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertEquals(TestEnum.class, e.getKey().getClass());
		assertEquals(TestEnum[][][].class, e.getValue().getClass());
	}

	@Test
	public void returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = getProxy().returnEnum1d3dListMap();
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		assertEquals(TestEnum[][][].class, x.get(TestEnum.ONE).get(0).getClass());
	}

	//--------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//--------------------------------------------------------------------------------

	@Test
	public void throwException1() {
		try {
			getProxy().throwException1();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException1 e) {
			assertEquals("foo", e.getMessage());
		}
	}

	@Test
	public void throwException2() {
		try {
			getProxy().throwException2();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException2 e) {
		}
	}

	//--------------------------------------------------------------------------------
	// Test parameters
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void setNothing() {
		getProxy().setNothing();
	}

	@Test
	public void setInt() {
		getProxy().setInt(1);
	}

	@Test
	public void setWrongInt() {
		try {
			getProxy().setInt(2);
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected:<1> but was:<2>", e.getMessage());
		}
	}

	@Test
	public void setInteger() {
		getProxy().setInteger(1);
	}

	@Test
	public void setBoolean() {
		getProxy().setBoolean(true);
	}

	@Test
	public void setFloat() {
		getProxy().setFloat(1f);
	}

	@Test
	public void setFloatObject() {
		getProxy().setFloatObject(1f);
	}

	@Test
	public void setString() {
		getProxy().setString("foo");
	}

	@Test
	public void setNullString() {
		getProxy().setNullString(null);
	}

	@Test
	public void setNullStringBad() {
		try {
			getProxy().setNullString("foo");
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected null, but was:<foo>", e.getLocalizedMessage());
		}
	}

	@Test
	public void setInt3dArray() {
		getProxy().setInt3dArray(new int[][][]{{{1,2},null},null});
	}

	@Test
	public void setInteger3dArray() {
		getProxy().setInteger3dArray(new Integer[][][]{{{1,null},null},null});
	}

	@Test
	public void setString3dArray() {
		getProxy().setString3dArray(new String[][][]{{{"foo",null},null},null});
	}

	@Test
	public void setIntegerList() {
		getProxy().setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void setInteger3dList() {
		getProxy().setInteger3dList(
			new AList<List<List<Integer>>>()
			.append(
				new AList<List<Integer>>()
				.append(new AList<Integer>().append(1).append(null))
				.append(null)
			)
			.append(null)
		);
	}

	@Test
	public void setInteger1d3dList() {
		getProxy().setInteger1d3dList(
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null)
		);
	}

	@Test
	public void setInt1d3dList() {
		getProxy().setInt1d3dList(
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null)
		);
	}

	@Test
	public void setStringList() {
		getProxy().setStringList(Arrays.asList("foo","bar",null));
	}

	// Beans
	@Test
	public void setBean() {
		getProxy().setBean(new Bean().init());
	}

	@Test
	public void setBeanList() {
		getProxy().setBeanList(Arrays.asList(new Bean().init()));
	}

	@Test
	public void setBeanMap() {
		getProxy().setBeanMap(new AMap<String,Bean>().append("foo",new Bean().init()));
	}

	@Test
	public void setBeanListMap() {
		getProxy().setBeanListMap(new AMap<String,List<Bean>>().append("foo",Arrays.asList(new Bean().init())));
	}

	@Test
	public void setBeanListMapIntegerKeys() {
		getProxy().setBeanListMapIntegerKeys(new AMap<Integer,List<Bean>>().append(1,Arrays.asList(new Bean().init())));
	}

	// Swapped POJOs
	@Test
	public void setSwappedPojo() {
		getProxy().setSwappedPojo(new SwappedPojo());
	}

	@Test
	public void setSwappedPojo3dArray() {
		getProxy().setSwappedPojo3dArray(new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	@Test
	public void setSwappedPojoMap() {
		getProxy().setSwappedPojoMap(new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()));
	}

	@Test
	public void setSwappedPojo3dMap() {
		getProxy().setSwappedPojo3dMap(new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void setImplicitSwappedPojo() {
		getProxy().setImplicitSwappedPojo(new ImplicitSwappedPojo());
	}

	@Test
	public void setImplicitSwappedPojo3dArray() {
		getProxy().setImplicitSwappedPojo3dArray(new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	@Test
	public void setImplicitSwappedPojoMap() {
		getProxy().setImplicitSwappedPojoMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()));
	}

	@Test
	public void setImplicitSwappedPojo3dMap() {
		getProxy().setImplicitSwappedPojo3dMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null}));
	}

	// Enums
	@Test
	public void setEnum() {
		getProxy().setEnum(TestEnum.TWO);
	}

	@Test
	public void setEnum3d() {
		getProxy().setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@Test
	public void setEnumList() {
		getProxy().setEnumList(new AList<TestEnum>().append(TestEnum.TWO).append(null));
	}

	@Test
	public void setEnum3dList() {
		getProxy().setEnum3dList(
			new AList<List<List<TestEnum>>>()
			.append(
				new AList<List<TestEnum>>()
				.append(
					new AList<TestEnum>().append(TestEnum.TWO).append(null)
				)
				.append(null)
			.append(null)
			)
		);
	}

	@Test
	public void setEnum1d3dList() {
		getProxy().setEnum1d3dList(new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
	}

	@Test
	public void setEnumMap() {
		getProxy().setEnumMap(new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void setEnum3dArrayMap() {
		getProxy().setEnum3dArrayMap(new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void setEnum1d3dListMap() {
		getProxy().setEnum1d3dListMap(new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null)));
	}

	//--------------------------------------------------------------------------------
	// Test multi-parameters
	//--------------------------------------------------------------------------------

	@Test
	public void setMultiParamsInts() {
		int x1 = 1;
		int[][][] x2 = new int[][][]{{{1,2},null},null};
		int[][][] x2n = null;
		List<int[][][]> x3 = new AList<int[][][]>().append(x2).append(null);
		List<int[][][]> x3n = null;
		getProxy().setMultiParamsInts(x1, x2, x2n, x3, x3n);
	}
	@Test
	public void setMultiParamsInteger() {
		Integer x1 = 1;
		Integer x1n = null;
		Integer[][][] x2 = new Integer[][][]{{{1,null},null},null};
		Integer[][][] x2n = null;
		List<Integer[][][]> x3 = new AList<Integer[][][]>().append(x2).append(null);
		List<Integer[][][]> x3n = null;
		getProxy().setMultiParamsInteger(x1, x1n, x2, x2n, x3, x3n);
	}
	@Test
	public void setMultiParamsFloat() {
		float x1 = 1;
		float[][][] x2 = new float[][][]{{{1,2},null},null};
		float[][][] x2n = null;
		List<float[][][]> x3 = new AList<float[][][]>().append(x2).append(null);
		List<float[][][]> x3n = null;
		getProxy().setMultiParamsFloat(x1, x2, x2n, x3, x3n);
	}
	@Test
	public void setMultiParamsFloatObject() {
		Float x1 = 1f;
		Float x1n = null;
		Float[][][] x2 = new Float[][][]{{{1f,null},null},null};
		Float[][][] x2n = null;
		List<Float[][][]> x3 = new AList<Float[][][]>().append(x2).append(null);
		List<Float[][][]> x3n = null;
		getProxy().setMultiParamsFloatObject(x1, x1n, x2, x2n, x3, x3n);
	}
	@Test
	public void setMultiParamsString() {
		String x1 = "foo";
		String[][][] x2 = new String[][][]{{{"foo",null},null},null};
		String[][][] x2n = null;
		List<String[][][]> x3 = new AList<String[][][]>().append(x2).append(null);
		List<String[][][]> x3n = null;
		getProxy().setMultiParamsString(x1, x2, x2n, x3, x3n);
	}
	@Test
	public void setMultiParamsBean() {
		Bean x1 = new Bean().init();
		Bean[][][] x2 = new Bean[][][]{{{new Bean().init(),null},null},null};
		Bean[][][] x2n = null;
		List<Bean[][][]> x3 = new AList<Bean[][][]>().append(x2).append(null);
		List<Bean[][][]> x3n = null;
		Map<String,Bean> x4 = new AMap<String,Bean>().append("foo",new Bean().init());
		Map<String,Bean> x4n = null;
		Map<String,List<Bean[][][]>> x5 = new AMap<String,List<Bean[][][]>>().append("foo", x3);
		Map<String,List<Bean[][][]>> x5n = null;
		getProxy().setMultiParamsBean(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
	@Test
	public void setMultiParamsSwappedPojo() {
		SwappedPojo x1 = new SwappedPojo();
		SwappedPojo[][][] x2 = new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
		SwappedPojo[][][] x2n = null;
		List<SwappedPojo[][][]> x3 = new AList<SwappedPojo[][][]>().append(x2).append(null);
		List<SwappedPojo[][][]> x3n = null;
		Map<SwappedPojo,SwappedPojo> x4 = new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo());
		Map<SwappedPojo,SwappedPojo> x4n = null;
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5 = new AMap<SwappedPojo,List<SwappedPojo[][][]>>().append(new SwappedPojo(), x3);
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5n = null;
		getProxy().setMultiParamsSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
	@Test
	public void setMultiParamsImplicitSwappedPojo() {
		ImplicitSwappedPojo x1 = new ImplicitSwappedPojo();
		ImplicitSwappedPojo[][][] x2 = new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
		ImplicitSwappedPojo[][][] x2n = null;
		List<ImplicitSwappedPojo[][][]> x3 = new AList<ImplicitSwappedPojo[][][]>().append(x2).append(null);
		List<ImplicitSwappedPojo[][][]> x3n = null;
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4 = new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo());
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n = null;
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5 = new AMap<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>>().append(new ImplicitSwappedPojo(), x3);
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n = null;
		getProxy().setMultiParamsImplicitSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
	@Test
	public void setMultiParamsEnum() {
		TestEnum x1 = TestEnum.TWO;
		TestEnum[][][] x2 = new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
		TestEnum[][][] x2n = null;
		List<TestEnum[][][]> x3 = new AList<TestEnum[][][]>().append(x2).append(null);
		List<TestEnum[][][]> x3n = null;
		Map<TestEnum,TestEnum> x4 = new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO);
		Map<TestEnum,TestEnum> x4n = null;
		Map<TestEnum,List<TestEnum[][][]>> x5 = new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, x3);
		Map<TestEnum,List<TestEnum[][][]>> x5n = null;
		getProxy().setMultiParamsEnum(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
}
