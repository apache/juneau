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
import static org.apache.juneau.rest.test.pojos.Constants.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.test.pojos.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class InterfaceProxyTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfSerializer.DEFAULT_XMLABBREV.builder().addBeanTypes().addRootType().build(), RdfParser.DEFAULT_XML },
		});
	}

	private InterfaceProxy proxy;

	public InterfaceProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = getCached(label, InterfaceProxy.class);
		if (proxy == null) {
			proxy = getClient(label, serializer, parser).getRemoteableProxy(InterfaceProxy.class, "/testInterfaceProxyResource/proxy");
			cache(label, proxy);
		}
	}

	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void a01_returnVoid() {
		proxy.returnVoid();
	}

	@Test
	public void a02_returnInteger() {
		assertEquals((Integer)1, proxy.returnInteger());
	}

	@Test
	public void a03_returnInt() {
		assertEquals(1, proxy.returnInt());
	}

	@Test
	public void a04_returnBoolean() {
		assertEquals(true, proxy.returnBoolean());
	}

	@Test
	public void a05_returnFloat() {
		assertTrue(1f == proxy.returnFloat());
	}

	@Test
	public void a06_returnFloatObject() {
		assertTrue(1f == proxy.returnFloatObject());
	}

	@Test
	public void a07_returnString() {
		assertEquals("foobar", proxy.returnString());
	}

	@Test
	public void a08_returnNullString() {
		assertNull(proxy.returnNullString());
	}

	@Test
	public void a09_returnInt3dArray() {
		assertObjectEquals("[[[1,2],null],null]", proxy.returnInt3dArray());
	}

	@Test
	public void a10_returnInteger3dArray() {
		assertObjectEquals("[[[1,null],null],null]", proxy.returnInteger3dArray());
	}

	@Test
	public void a11_returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null],null],null]", proxy.returnString3dArray());
	}

	@Test
	public void a12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertObjectEquals("[1,null]", x);
		assertClass(Integer.class, x.get(0));
	}

	@Test
	public void a13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertObjectEquals("[[[1,null],null],null]", x);
		assertClass(Integer.class, x.get(0).get(0).get(0));
	}

	@Test
	public void a14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertClass(Integer.class, x.get(0)[0][0][0]);
	}

	@Test
	public void a15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertClass(int[][][].class, x.get(0));
	}

	@Test
	public void a16_returnStringList() {
		assertObjectEquals("['foo','bar',null]", proxy.returnStringList());
	}

	// Beans

	@Test
	public void b01_returnBean() {
		ABean x = proxy.returnBean();
		assertObjectEquals("{a:1,b:'foo'}", x);
		assertClass(ABean.class, x);
	}

	@Test
	public void b02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
		assertClass(ABean.class, x[0][0][0]);
	}

	@Test
	public void b03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertObjectEquals("[{a:1,b:'foo'}]", x);
		assertClass(ABean.class, x.get(0));
	}

	@Test
	public void b04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
		assertClass(ABean.class, x.get(0)[0][0][0]);
	}

	@Test
	public void b05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
		assertClass(ABean.class, x.get("foo"));
	}

	@Test
	public void b06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
		assertClass(ABean.class, x.get("foo").get(0));
	}

	@Test
	public void b07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void b08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);
		assertClass(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@Test
	public void c01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
		assertClass(TypedBeanImpl.class, x);
	}

	@Test
	public void c02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
		assertClass(TypedBeanImpl.class, x[0][0][0]);
	}

	@Test
	public void c03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
		assertClass(TypedBeanImpl.class, x.get(0));
	}

	@Test
	public void c04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
		assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@Test
	public void c05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
		assertClass(TypedBeanImpl.class, x.get("foo"));
	}

	@Test
	public void c06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@Test
	public void c07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void c08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@Test
	public void d01_returnSwappedPojo() {
		SwappedPojo x = proxy.returnSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void d02_returnSwappedPojo3dArray() {
		SwappedPojo[][][] x = proxy.returnSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void d03_returnSwappedPojoMap() {
		Map<SwappedPojo,SwappedPojo> x = proxy.returnSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void d04_returnSwappedPojo3dMap() {
		Map<SwappedPojo,SwappedPojo[][][]> x = proxy.returnSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void e01_returnImplicitSwappedPojo() {
		ImplicitSwappedPojo x = proxy.returnImplicitSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void e02_returnImplicitSwappedPojo3dArray() {
		ImplicitSwappedPojo[][][] x = proxy.returnImplicitSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void e03_returnImplicitSwappedPojoMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x = proxy.returnImplicitSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void e04_returnImplicitSwappedPojo3dMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x = proxy.returnImplicitSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void f01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertObjectEquals("'TWO'", x);
	}

	@Test
	public void f02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertObjectEquals("[[['TWO',null],null],null]", x);
		assertClass(TestEnum.class, x[0][0][0]);
	}

	@Test
	public void f03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertObjectEquals("['TWO',null]", x);
		assertClass(TestEnum.class, x.get(0));
	}

	@Test
	public void f04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertObjectEquals("[[['TWO',null],null,null]]", x);
		assertClass(TestEnum.class, x.get(0).get(0).get(0));
	}

	@Test
	public void f05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertClass(TestEnum[][][].class, x.get(0));
	}

	@Test
	public void f06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum.class, e.getValue());
	}

	@Test
	public void f07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue());
	}

	@Test
	public void f08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		assertClass(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
	}

	//--------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//--------------------------------------------------------------------------------

	@Test
	public void g01_throwException1() {
		try {
			proxy.throwException1();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException1 e) {
			assertEquals("foo", e.getMessage());
		}
	}

	@Test
	public void g02_throwException2() {
		try {
			proxy.throwException2();
			fail("Exception expected");
		} catch (InterfaceProxy.InterfaceProxyException2 e) {
		}
	}

	//--------------------------------------------------------------------------------
	// Test parameters
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void h01_setNothing() {
		proxy.setNothing();
	}

	@Test
	public void h02_setInt() {
		proxy.setInt(1);
	}

	@Test
	public void h03_setWrongInt() {
		try {
			proxy.setInt(2);
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected:<1> but was:<2>", e.getMessage());
		}
	}

	@Test
	public void h04_setInteger() {
		proxy.setInteger(1);
	}

	@Test
	public void h05_setBoolean() {
		proxy.setBoolean(true);
	}

	@Test
	public void h06_setFloat() {
		proxy.setFloat(1f);
	}

	@Test
	public void h07_setFloatObject() {
		proxy.setFloatObject(1f);
	}

	@Test
	public void h08_setString() {
		proxy.setString("foo");
	}

	@Test
	public void h09_setNullString() {
		proxy.setNullString(null);
	}

	@Test
	public void h10_setNullStringBad() {
		try {
			proxy.setNullString("foo");
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected null, but was:<foo>", e.getLocalizedMessage());
		}
	}

	@Test
	public void h11_setInt3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1,2},null},null});
	}

	@Test
	public void h12_setInteger3dArray() {
		proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null});
	}

	@Test
	public void h13_setString3dArray() {
		proxy.setString3dArray(new String[][][]{{{"foo",null},null},null});
	}

	@Test
	public void h14_setIntegerList() {
		proxy.setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void h15_setInteger3dList() {
		proxy.setInteger3dList(
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
	public void h16_setInteger1d3dList() {
		proxy.setInteger1d3dList(
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null)
		);
	}

	@Test
	public void h17_setInt1d3dList() {
		proxy.setInt1d3dList(
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null)
		);
	}

	@Test
	public void h18_setStringList() {
		proxy.setStringList(Arrays.asList("foo","bar",null));
	}

	// Beans
	@Test
	public void h19_setBean() {
		proxy.setBean(new ABean().init());
	}

	@Test
	public void h20_setBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{new ABean().init(),null},null},null});
	}

	@Test
	public void h21_setBeanList() {
		proxy.setBeanList(Arrays.asList(new ABean().init()));
	}

	@Test
	public void h22_setBean1d3dList() {
		proxy.setBean1d3dList(new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null));
	}

	@Test
	public void h23_setBeanMap() {
		proxy.setBeanMap(new AMap<String,ABean>().append("foo",new ABean().init()));
	}

	@Test
	public void h24_setBeanListMap() {
		proxy.setBeanListMap(new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())));
	}

	@Test
	public void h25_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)));
	}

	@Test
	public void h26_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init())));
	}

	// Typed beans

	@Test
	public void i01_setTypedBean() {
		proxy.setTypedBean(new TypedBeanImpl().init());
	}

	@Test
	public void i02_setTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null});
	}

	@Test
	public void i03_setTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)new TypedBeanImpl().init()));
	}

	@Test
	public void i04_setTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null));
	}

	@Test
	public void i05_setTypedBeanMap() {
		proxy.setTypedBeanMap(new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()));
	}

	@Test
	public void i06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	@Test
	public void i07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)));
	}

	@Test
	public void i08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	// Swapped POJOs

	@Test
	public void j01_setSwappedPojo() {
		proxy.setSwappedPojo(new SwappedPojo());
	}

	@Test
	public void j02_setSwappedPojo3dArray() {
		proxy.setSwappedPojo3dArray(new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	@Test
	public void j03_setSwappedPojoMap() {
		proxy.setSwappedPojoMap(new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()));
	}

	@Test
	public void j04_setSwappedPojo3dMap() {
		proxy.setSwappedPojo3dMap(new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void k01_setImplicitSwappedPojo() {
		proxy.setImplicitSwappedPojo(new ImplicitSwappedPojo());
	}

	@Test
	public void k02_setImplicitSwappedPojo3dArray() {
		proxy.setImplicitSwappedPojo3dArray(new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	@Test
	public void k03_setImplicitSwappedPojoMap() {
		proxy.setImplicitSwappedPojoMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()));
	}

	@Test
	public void k04_setImplicitSwappedPojo3dMap() {
		proxy.setImplicitSwappedPojo3dMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null}));
	}

	// Enums

	@Test
	public void l01_setEnum() {
		proxy.setEnum(TestEnum.TWO);
	}

	@Test
	public void l02_setEnum3d() {
		proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@Test
	public void l03_setEnumList() {
		proxy.setEnumList(new AList<TestEnum>().append(TestEnum.TWO).append(null));
	}

	@Test
	public void l04_setEnum3dList() {
		proxy.setEnum3dList(
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
	public void l05_setEnum1d3dList() {
		proxy.setEnum1d3dList(new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
	}

	@Test
	public void l06_setEnumMap() {
		proxy.setEnumMap(new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void l07_setEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void l08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null)));
	}

	//--------------------------------------------------------------------------------
	// Test multi-parameters
	//--------------------------------------------------------------------------------

	@Test
	public void m01_setMultiParamsInts() {
		int x1 = 1;
		int[][][] x2 = new int[][][]{{{1,2},null},null};
		int[][][] x2n = null;
		List<int[][][]> x3 = new AList<int[][][]>().append(x2).append(null);
		List<int[][][]> x3n = null;
		proxy.setMultiParamsInts(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m02_setMultiParamsInteger() {
		Integer x1 = 1;
		Integer x1n = null;
		Integer[][][] x2 = new Integer[][][]{{{1,null},null},null};
		Integer[][][] x2n = null;
		List<Integer[][][]> x3 = new AList<Integer[][][]>().append(x2).append(null);
		List<Integer[][][]> x3n = null;
		proxy.setMultiParamsInteger(x1, x1n, x2, x2n, x3, x3n);
	}

	@Test
	public void m03_setMultiParamsFloat() {
		float x1 = 1;
		float[][][] x2 = new float[][][]{{{1,2},null},null};
		float[][][] x2n = null;
		List<float[][][]> x3 = new AList<float[][][]>().append(x2).append(null);
		List<float[][][]> x3n = null;
		proxy.setMultiParamsFloat(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m04_setMultiParamsFloatObject() {
		Float x1 = 1f;
		Float x1n = null;
		Float[][][] x2 = new Float[][][]{{{1f,null},null},null};
		Float[][][] x2n = null;
		List<Float[][][]> x3 = new AList<Float[][][]>().append(x2).append(null);
		List<Float[][][]> x3n = null;
		proxy.setMultiParamsFloatObject(x1, x1n, x2, x2n, x3, x3n);
	}

	@Test
	public void m05_setMultiParamsString() {
		String x1 = "foo";
		String[][][] x2 = new String[][][]{{{"foo",null},null},null};
		String[][][] x2n = null;
		List<String[][][]> x3 = new AList<String[][][]>().append(x2).append(null);
		List<String[][][]> x3n = null;
		proxy.setMultiParamsString(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m06_setMultiParamsBean() {
		ABean x1 = new ABean().init();
		ABean[][][] x2 = new ABean[][][]{{{new ABean().init(),null},null},null};
		ABean[][][] x2n = null;
		List<ABean[][][]> x3 = new AList<ABean[][][]>().append(x2).append(null);
		List<ABean[][][]> x3n = null;
		Map<String,ABean> x4 = new AMap<String,ABean>().append("foo",new ABean().init());
		Map<String,ABean> x4n = null;
		Map<String,List<ABean[][][]>> x5 = new AMap<String,List<ABean[][][]>>().append("foo", x3);
		Map<String,List<ABean[][][]>> x5n = null;
		proxy.setMultiParamsBean(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m07_setMultiParamsSwappedPojo() {
		SwappedPojo x1 = new SwappedPojo();
		SwappedPojo[][][] x2 = new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
		SwappedPojo[][][] x2n = null;
		List<SwappedPojo[][][]> x3 = new AList<SwappedPojo[][][]>().append(x2).append(null);
		List<SwappedPojo[][][]> x3n = null;
		Map<SwappedPojo,SwappedPojo> x4 = new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo());
		Map<SwappedPojo,SwappedPojo> x4n = null;
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5 = new AMap<SwappedPojo,List<SwappedPojo[][][]>>().append(new SwappedPojo(), x3);
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5n = null;
		proxy.setMultiParamsSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m08_setMultiParamsImplicitSwappedPojo() {
		ImplicitSwappedPojo x1 = new ImplicitSwappedPojo();
		ImplicitSwappedPojo[][][] x2 = new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
		ImplicitSwappedPojo[][][] x2n = null;
		List<ImplicitSwappedPojo[][][]> x3 = new AList<ImplicitSwappedPojo[][][]>().append(x2).append(null);
		List<ImplicitSwappedPojo[][][]> x3n = null;
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4 = new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo());
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n = null;
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5 = new AMap<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>>().append(new ImplicitSwappedPojo(), x3);
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n = null;
		proxy.setMultiParamsImplicitSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m09_setMultiParamsEnum() {
		TestEnum x1 = TestEnum.TWO;
		TestEnum[][][] x2 = new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
		TestEnum[][][] x2n = null;
		List<TestEnum[][][]> x3 = new AList<TestEnum[][][]>().append(x2).append(null);
		List<TestEnum[][][]> x3n = null;
		Map<TestEnum,TestEnum> x4 = new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO);
		Map<TestEnum,TestEnum> x4n = null;
		Map<TestEnum,List<TestEnum[][][]>> x5 = new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, x3);
		Map<TestEnum,List<TestEnum[][][]>> x5n = null;
		proxy.setMultiParamsEnum(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
}
