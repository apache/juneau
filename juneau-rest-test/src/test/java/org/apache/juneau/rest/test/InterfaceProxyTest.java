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
		assertObjectEquals("[[[1,2]]]", getProxy().returnInt3dArray());
	}

	@Test
	public void returnInteger3dArray() {
		assertObjectEquals("[[[1,null]]]", getProxy().returnInteger3dArray());
	}

	@Test
	public void returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null]]]", getProxy().returnString3dArray());
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
		assertObjectEquals("[[[1,null]]]", x);
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
		assertObjectEquals("'[{(<swapped>)}]'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void returnSwappedPojo3dArray() {
		SwappedPojo[][][] x = getProxy().returnSwappedPojo3dArray();
		assertObjectEquals("[[['[{(<swapped>)}]',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void returnSwappedPojoMap() {
		Map<SwappedPojo,SwappedPojo> x = getProxy().returnSwappedPojoMap();
		assertObjectEquals("{'[{(<swapped>)}]':'[{(<swapped>)}]'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void returnSwappedPojo3dMap() {
		Map<SwappedPojo,SwappedPojo[][][]> x = getProxy().returnSwappedPojo3dMap();
		assertObjectEquals("{'[{(<swapped>)}]':[[['[{(<swapped>)}]',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs
	@Test
	public void returnImplicitSwappedPojo() {
		ImplicitSwappedPojo x = getProxy().returnImplicitSwappedPojo();
		assertObjectEquals("'[{(<swapped>)}]'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojo3dArray() {
		ImplicitSwappedPojo[][][] x = getProxy().returnImplicitSwappedPojo3dArray();
		assertObjectEquals("[[['[{(<swapped>)}]',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojoMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x = getProxy().returnImplicitSwappedPojoMap();
		assertObjectEquals("{'[{(<swapped>)}]':'[{(<swapped>)}]'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void returnImplicitSwappedPojo3dMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x = getProxy().returnImplicitSwappedPojo3dMap();
		assertObjectEquals("{'[{(<swapped>)}]':[[['[{(<swapped>)}]',null],null],null]}", x);
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
		getProxy().setInt3dArray(new int[][][]{{{1,2}}});
	}

	@Test
	public void setInteger3dArray() {
		getProxy().setInteger3dArray(new Integer[][][]{{{1,null}}});
	}

	@Test
	public void setString3dArray() {
		getProxy().setString3dArray(new String[][][]{{{"foo","bar",null}}});
	}

	@Test
	public void setIntegerList() {
		getProxy().setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void setInteger2dList() {
		getProxy().setInteger2dList(
			new AList<List<Integer>>()
			.append(new AList<Integer>().append(1).append(null))
		);
	}

	@Test
	public void setInteger3dList() {
		getProxy().setInteger3dList(
			new AList<List<List<Integer>>>()
			.append(
				new AList<List<Integer>>()
				.append(new AList<Integer>().append(1).append(null))
			)
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
}
