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

import static java.util.Arrays.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.jena.*;
import org.apache.juneau.utils.*;
import org.junit.*;

/**
 * Tests inteface proxies exposed through <code>@RestMethod(name="PROXY")</code>
 */
@RestResource(
	path="/testInterfaceProxyResource")
public class InterfaceProxyResource extends RestServletJenaDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Test that Q-values are being resolved correctly.
	//====================================================================================================
	@RestMethod(name="PROXY", path="/proxy/*")
	public InterfaceProxy getProxy() {
		return new InterfaceProxy() {

			//--------------------------------------------------------------------------------
			// Test return types.
			//--------------------------------------------------------------------------------

			// Various primitives
			@Override
			public void returnVoid() {
			}
			@Override
			public Integer returnInteger() {
				return 1;
			}
			@Override
			public int returnInt() {
				return 1;
			}
			@Override
			public boolean returnBoolean() {
				return true;
			}
			@Override
			public float returnFloat() {
				return 1f;
			}
			@Override
			public Float returnFloatObject() {
				return 1f;
			}
			@Override
			public String returnString() {
				return "foobar";
			}
			@Override
			public String returnNullString() {
				return null;
			}
			@Override
			public int[][][] returnInt3dArray() {
				return new int[][][]{{{1,2}}};
			}
			@Override
			public Integer[][][] returnInteger3dArray() {
				return new Integer[][][]{{{1,null}}};
			}
			@Override
			public String[][][] returnString3dArray() {
				return new String[][][]{{{"foo","bar",null}}};
			}
			@Override
			public List<Integer> returnIntegerList() {
				return asList(new Integer[]{1,null});
			}
			@Override
			public List<List<List<Integer>>> returnInteger3dList() {
				return new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(
						new AList<Integer>().append(1).append(null)
					)
				);
			}
			@Override
			public List<Integer[][][]> returnInteger1d3dList() {
				return new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null);
			}
			@Override
			public List<int[][][]> returnInt1d3dList() {
				return new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null);
			}
			@Override
			public List<String> returnStringList() {
				return asList(new String[]{"foo","bar",null});
			}

			// Beans
			@Override
			public Bean returnBean() {
				return new Bean().init();
			}
			@Override
			public Bean[][][] returnBean3dArray() {
				return new Bean[][][]{{{new Bean().init(),null},null},null};
			}
			@Override
			public List<Bean> returnBeanList() {
				return asList(new Bean().init());
			}
			@Override
			public List<Bean[][][]> returnBean1d3dList() {
				return new AList<Bean[][][]>().append(new Bean[][][]{{{new Bean().init(),null},null},null}).append(null);
			}
			@Override
			public Map<String,Bean> returnBeanMap() {
				return new AMap<String,Bean>().append("foo",new Bean().init());
			}
			@Override
			public Map<String,List<Bean>> returnBeanListMap() {
				return new AMap<String,List<Bean>>().append("foo",asList(new Bean().init()));
			}
			@Override
			public Map<String,List<Bean[][][]>> returnBean1d3dListMap() {
				return new AMap<String,List<Bean[][][]>>().append("foo", new AList<Bean[][][]>().append(new Bean[][][]{{{new Bean().init(),null},null},null}).append(null));
			}
			@Override
			public Map<Integer,List<Bean>> returnBeanListMapIntegerKeys() {
				return new AMap<Integer,List<Bean>>().append(1,asList(new Bean().init()));
			}

			// Swapped POJOs
			@Override
			public SwappedPojo returnSwappedPojo() {
				return new SwappedPojo();
			}
			@Override
			public SwappedPojo[][][] returnSwappedPojo3dArray() {
				return new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
			}
			@Override
			public Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap() {
				return new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo());
			}
			@Override
			public Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap() {
				return new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
			}

			// Implicit swapped POJOs
			@Override
			public ImplicitSwappedPojo returnImplicitSwappedPojo() {
				return new ImplicitSwappedPojo();
			}
			@Override
			public ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray() {
				return new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
			}
			@Override
			public Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap() {
				return new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo());
			}
			@Override
			public Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap() {
				return new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
			}

			// Enums
			@Override
			public TestEnum returnEnum() {
				return TestEnum.TWO;
			}
			@Override
			public TestEnum[][][] returnEnum3d() {
				return new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
			}
			@Override
			public List<TestEnum> returnEnumList() {
				return new AList<TestEnum>().append(TestEnum.TWO).append(null);
			}
			@Override
			public List<List<List<TestEnum>>> returnEnum3dList() {
				return new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
					.append(
						new AList<TestEnum>().append(TestEnum.TWO).append(null)
					)
					.append(null)
				.append(null)
				);
			}
			@Override
			public List<TestEnum[][][]> returnEnum1d3dList() {
				return new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null);
			}
			@Override
			public Map<TestEnum,TestEnum> returnEnumMap() {
				return new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO);
			}
			@Override
			public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
				return new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
			}
			@Override
			public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
				return new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
			}

			//--------------------------------------------------------------------------------
			// Test server-side exception serialization.
			//--------------------------------------------------------------------------------

			@Override
			public void throwException1() throws InterfaceProxy.InterfaceProxyException1 {
				throw new InterfaceProxy.InterfaceProxyException1("foo");
			}
			@Override
			public void throwException2() throws InterfaceProxy.InterfaceProxyException2 {
				throw new InterfaceProxy.InterfaceProxyException2();
			}

			//--------------------------------------------------------------------------------
			// Test parameters
			//--------------------------------------------------------------------------------

			// Various primitives
			@Override
			public void setNothing() {
			}
			@Override
			public void setInt(int x) {
				assertEquals(1, x);
			}
			@Override
			public void setInteger(Integer x) {
				assertEquals((Integer)1, x);
			}
			@Override
			public void setBoolean(boolean x) {
				assertTrue(x);
			}
			@Override
			public void setFloat(float x) {
				assertTrue(1f == x);
			}
			@Override
			public void setFloatObject(Float x) {
				assertTrue(1f == x);
			}
			@Override
			public void setString(String x) {
				assertEquals("foo", x);
			}
			@Override
			public void setNullString(String x) {
				assertNull(x);
			}
			@Override
			public void setInt3dArray(int[][][] x) {
				assertObjectEquals("[[[1,2]]]", x);
			}
			@Override
			public void setInteger3dArray(Integer[][][] x) {
				assertObjectEquals("[[[1,null]]]", x);
			}
			@Override
			public void setString3dArray(String[][][] x) {
				assertObjectEquals("[[['foo','bar',null]]]", x);
			}
			@Override
			public void setIntegerList(List<Integer> x) {
				assertObjectEquals("[1,null]", x);
				assertEquals(Integer.class, x.get(0).getClass());
			}
			@Override
			public void setInteger2dList(List<List<Integer>> x) {
				assertObjectEquals("[[1,null]]", x);
				assertEquals(Integer.class, x.get(0).get(0).getClass());
			}
			@Override
			public void setInteger3dList(List<List<List<Integer>>> x) {
				assertObjectEquals("[[[1,null]]]", x);
				assertEquals(Integer.class, x.get(0).get(0).get(0).getClass());
			}
			@Override
			public void setInteger1d3dList(List<Integer[][][]> x) {
				assertObjectEquals("[[[[1,null],null],null],null]", x);
				assertEquals(Integer[][][].class, x.get(0).getClass());
				assertEquals(Integer.class, x.get(0)[0][0][0].getClass());
			}
			@Override
			public void setInt1d3dList(List<int[][][]> x) {
				assertObjectEquals("[[[[1,2],null],null],null]", x);
				assertEquals(int[][][].class, x.get(0).getClass());
			}
			@Override
			public void setStringList(List<String> x) {
				assertObjectEquals("['foo','bar',null]", x);
			}

			// Beans
			@Override
			public void setBean(Bean x) {
				assertObjectEquals("{a:1,b:'foo'}", x);
			}
			@Override
			public void setBean3dArray(Bean[][][] x) {
				assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
			}
			@Override
			public void setBeanList(List<Bean> x) {
				assertObjectEquals("[{a:1,b:'foo'}]", x);
			}
			@Override
			public void setBean1d3dList(List<Bean[][][]> x) {
				assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
			}
			@Override
			public void setBeanMap(Map<String,Bean> x) {
				assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
			}
			@Override
			public void setBeanListMap(Map<String,List<Bean>> x) {
				assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
			}
			@Override
			public void setBean1d3dListMap(Map<String,List<Bean[][][]>> x) {
				assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
			}
			@Override
			public void setBeanListMapIntegerKeys(Map<Integer,List<Bean>> x) {
				assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
				assertEquals(Integer.class, x.keySet().iterator().next().getClass());
			}

			// Swapped POJOs
			@Override
			public void setSwappedPojo(SwappedPojo x) {
				assertTrue(x.wasUnswapped);
			}
			@Override
			public void setSwappedPojo3dArray(SwappedPojo[][][] x) {
				assertObjectEquals("[[['[{(<swapped>)}]',null],null],null]", x);
				assertTrue(x[0][0][0].wasUnswapped);
			}
			@Override
			public void setSwappedPojoMap(Map<SwappedPojo,SwappedPojo> x) {
				assertObjectEquals("{'[{(<swapped>)}]':'[{(<swapped>)}]'}", x);
				Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
				assertTrue(e.getKey().wasUnswapped);
				assertTrue(e.getValue().wasUnswapped);
			}
			@Override
			public void setSwappedPojo3dMap(Map<SwappedPojo,SwappedPojo[][][]> x) {
				assertObjectEquals("{'[{(<swapped>)}]':[[['[{(<swapped>)}]',null],null],null]}", x);
				Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
				assertTrue(e.getKey().wasUnswapped);
				assertTrue(e.getValue()[0][0][0].wasUnswapped);
			}

			// Implicit swapped POJOs
			@Override
			public void setImplicitSwappedPojo(ImplicitSwappedPojo x) {
				assertTrue(x.wasUnswapped);
			}
			@Override
			public void setImplicitSwappedPojo3dArray(ImplicitSwappedPojo[][][] x) {
				assertObjectEquals("[[['[{(<swapped>)}]',null],null],null]", x);
				assertTrue(x[0][0][0].wasUnswapped);
			}
			@Override
			public void setImplicitSwappedPojoMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x) {
				assertObjectEquals("{'[{(<swapped>)}]':'[{(<swapped>)}]'}", x);
				Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
				assertTrue(e.getKey().wasUnswapped);
				assertTrue(e.getValue().wasUnswapped);
			}
			@Override
			public void setImplicitSwappedPojo3dMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x) {
				assertObjectEquals("{'[{(<swapped>)}]':[[['[{(<swapped>)}]',null],null],null]}", x);
				Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
				assertTrue(e.getKey().wasUnswapped);
				assertTrue(e.getValue()[0][0][0].wasUnswapped);
			}

			// Enums
			@Override
			public void setEnum(TestEnum x) {
				assertEquals(TestEnum.TWO, x);
			}
			@Override
			public void setEnum3d(TestEnum[][][] x) {
				assertObjectEquals("[[['TWO',null],null],null]", x);
			}
			@Override
			public void setEnumList(List<TestEnum> x) {
				assertObjectEquals("['TWO',null]", x);
				assertEquals(TestEnum.class, x.get(0).getClass());
			}
			@Override
			public void setEnum3dList(List<List<List<TestEnum>>> x) {
				assertObjectEquals("[[['TWO',null],null,null]]", x);
				assertEquals(TestEnum.class, x.get(0).get(0).get(0).getClass());
			}
			@Override
			public void setEnum1d3dList(List<TestEnum[][][]> x) {
				assertObjectEquals("[[[['TWO',null],null],null],null]", x);
				assertEquals(TestEnum[][][].class, x.get(0).getClass());
			}
			@Override
			public void setEnumMap(Map<TestEnum,TestEnum> x) {
				assertObjectEquals("{ONE:'TWO'}", x);
				Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
				assertEquals(TestEnum.class, e.getKey().getClass());
				assertEquals(TestEnum.class, e.getValue().getClass());
			}
			@Override
			public void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x) {
				assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
				Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
				assertEquals(TestEnum.class, e.getKey().getClass());
				assertEquals(TestEnum[][][].class, e.getValue().getClass());
			}
			@Override
			public void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x) {
				assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
				Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
				assertEquals(TestEnum.class, e.getKey().getClass());
				assertEquals(TestEnum[][][].class, e.getValue().get(0).getClass());
			}
		};
	}

	private static void assertObjectEquals(String e, Object o) {
		Assert.assertEquals(e, JsonSerializer.DEFAULT_LAX.toString(o));
	}
}
