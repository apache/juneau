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
package org.apache.juneau.rest.test.client;

import static java.util.Arrays.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.testutils.Constants.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remote.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.apache.juneau.rest.testutils.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class InterfaceProxyTest {

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
			{ /* 7 */ "RdfXml", RdfXmlAbbrevSerializer.DEFAULT.builder().addBeanTypes().addRootType().build(), RdfXmlParser.DEFAULT },
		});
	}

	@RemoteInterface
	public interface InterfaceProxy {

		public static final String SWAP = "swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/";

		//-------------------------------------------------------------------------------------------------------------
		// Test return types.
		//-------------------------------------------------------------------------------------------------------------

		// Various primitives
		void returnVoid();
		int returnInt();
		Integer returnInteger();
		boolean returnBoolean();
		float returnFloat();
		Float returnFloatObject();
		String returnString();
		String returnNullString();
		int[][][] returnInt3dArray();
		Integer[][][] returnInteger3dArray();
		String[][][] returnString3dArray();
		List<Integer> returnIntegerList();
		List<List<List<Integer>>> returnInteger3dList();
		List<Integer[][][]> returnInteger1d3dList();
		List<int[][][]> returnInt1d3dList();
		List<String> returnStringList();

		// Beans
		ABean returnBean();
		ABean[][][] returnBean3dArray();
		List<ABean> returnBeanList();
		List<ABean[][][]> returnBean1d3dList();
		Map<String,ABean> returnBeanMap();
		Map<String,List<ABean>> returnBeanListMap();
		Map<String,List<ABean[][][]>> returnBean1d3dListMap();
		Map<Integer,List<ABean>> returnBeanListMapIntegerKeys();

		// Typed beans
		TypedBean returnTypedBean();
		TypedBean[][][] returnTypedBean3dArray();
		List<TypedBean> returnTypedBeanList();
		List<TypedBean[][][]> returnTypedBean1d3dList();
		Map<String,TypedBean> returnTypedBeanMap();
		Map<String,List<TypedBean>> returnTypedBeanListMap();
		Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap();
		Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys();

		// Swapped POJOs
		SwappedPojo returnSwappedPojo();
		SwappedPojo[][][] returnSwappedPojo3dArray();
		Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap();
		Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap();

		// Implicit swapped POJOs
		ImplicitSwappedPojo returnImplicitSwappedPojo();
		ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray();
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap();
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap();

		// Enums
		TestEnum returnEnum();
		TestEnum[][][] returnEnum3d();
		List<TestEnum> returnEnumList();
		List<List<List<TestEnum>>> returnEnum3dList();
		List<TestEnum[][][]> returnEnum1d3dList();
		Map<TestEnum,TestEnum> returnEnumMap();
		Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap();
		Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap();

		//-------------------------------------------------------------------------------------------------------------
		// Test server-side exception serialization.
		//-------------------------------------------------------------------------------------------------------------

		void throwException1() throws InterfaceProxyException1;
		void throwException2() throws InterfaceProxyException2;

		//-------------------------------------------------------------------------------------------------------------
		// Test parameters
		//-------------------------------------------------------------------------------------------------------------

		// Various primitives
		void setNothing();
		void setInt(int x);
		void setInteger(Integer x);
		void setBoolean(boolean x);
		void setFloat(float x);
		void setFloatObject(Float x);
		void setString(String x);
		void setNullString(String x);
		void setInt3dArray(int[][][] x);
		void setInteger3dArray(Integer[][][] x);
		void setString3dArray(String[][][] x);
		void setIntegerList(List<Integer> x);
		void setInteger3dList(List<List<List<Integer>>> x);
		void setInteger1d3dList(List<Integer[][][]> x);
		void setInt1d3dList(List<int[][][]> x);
		void setStringList(List<String> x);

		// Beans
		void setBean(ABean x);
		void setBean3dArray(ABean[][][] x);
		void setBeanList(List<ABean> x);
		void setBean1d3dList(List<ABean[][][]> x);
		void setBeanMap(Map<String,ABean> x);
		void setBeanListMap(Map<String,List<ABean>> x);
		void setBean1d3dListMap(Map<String,List<ABean[][][]>> x);
		void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> x);

		// Typed beans
		void setTypedBean(TypedBean x);
		void setTypedBean3dArray(TypedBean[][][] x);
		void setTypedBeanList(List<TypedBean> x);
		void setTypedBean1d3dList(List<TypedBean[][][]> x);
		void setTypedBeanMap(Map<String,TypedBean> x);
		void setTypedBeanListMap(Map<String,List<TypedBean>> x);
		void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> x);
		void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> x);

		// Swapped POJOs
		void setSwappedPojo(SwappedPojo x);
		void setSwappedPojo3dArray(SwappedPojo[][][] x);
		void setSwappedPojoMap(Map<SwappedPojo,SwappedPojo> x);
		void setSwappedPojo3dMap(Map<SwappedPojo,SwappedPojo[][][]> x);

		// Implicit swapped POJOs
		void setImplicitSwappedPojo(ImplicitSwappedPojo x);
		void setImplicitSwappedPojo3dArray(ImplicitSwappedPojo[][][] x);
		void setImplicitSwappedPojoMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x);
		void setImplicitSwappedPojo3dMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x);

		// Enums
		void setEnum(TestEnum x);
		void setEnum3d(TestEnum[][][] x);
		void setEnumList(List<TestEnum> x);
		void setEnum3dList(List<List<List<TestEnum>>> x);
		void setEnum1d3dList(List<TestEnum[][][]> x);
		void setEnumMap(Map<TestEnum,TestEnum> x);
		void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x);
		void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x);

		//-------------------------------------------------------------------------------------------------------------
		// Test multi-parameters
		//-------------------------------------------------------------------------------------------------------------

		void setMultiParamsInts(int x1, int[][][] x2, int[][][] x2n, List<int[][][]> x3, List<int[][][]> x3n);
		void setMultiParamsInteger(Integer x1, Integer x1n, Integer[][][] x2, Integer[][][] x2n, List<Integer[][][]> x3, List<Integer[][][]> x3n);
		void setMultiParamsFloat(float x1, float[][][] x2, float[][][] x2n, List<float[][][]> x3, List<float[][][]> x3n);
		void setMultiParamsFloatObject(Float x1, Float x1n, Float[][][] x2, Float[][][] x2n, List<Float[][][]> x3, List<Float[][][]> x3n);
		void setMultiParamsString(String x1, String[][][] x2, String[][][] x2n, List<String[][][]> x3, List<String[][][]> x3n);
		void setMultiParamsBean(ABean x1, ABean[][][] x2, ABean[][][] x2n, List<ABean[][][]> x3, List<ABean[][][]> x3n, Map<String,ABean> x4, Map<String,ABean> x4n, Map<String,List<ABean[][][]>> x5, Map<String,List<ABean[][][]>> x5n);
		void setMultiParamsSwappedPojo(SwappedPojo x1, SwappedPojo[][][] x2, SwappedPojo[][][] x2n, List<SwappedPojo[][][]> x3, List<SwappedPojo[][][]> x3n, Map<SwappedPojo,SwappedPojo> x4, Map<SwappedPojo,SwappedPojo> x4n, Map<SwappedPojo,List<SwappedPojo[][][]>> x5, Map<SwappedPojo,List<SwappedPojo[][][]>> x5n);
		void setMultiParamsImplicitSwappedPojo(ImplicitSwappedPojo x1, ImplicitSwappedPojo[][][] x2, ImplicitSwappedPojo[][][] x2n, List<ImplicitSwappedPojo[][][]> x3, List<ImplicitSwappedPojo[][][]> x3n, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n);
		void setMultiParamsEnum(TestEnum x1, TestEnum[][][] x2, TestEnum[][][] x2n, List<TestEnum[][][]> x3, List<TestEnum[][][]> x3n, Map<TestEnum,TestEnum> x4, Map<TestEnum,TestEnum> x4n, Map<TestEnum,List<TestEnum[][][]>> x5, Map<TestEnum,List<TestEnum[][][]>> x5n);

		//-------------------------------------------------------------------------------------------------------------
		// Helper classes
		//-------------------------------------------------------------------------------------------------------------

		@SuppressWarnings("serial")
		public static class InterfaceProxyException1 extends Throwable {
			public InterfaceProxyException1(String msg) {
				super(msg);
			}
		}

		@SuppressWarnings("serial")
		public static class InterfaceProxyException2 extends Throwable {
		}
	}

	@Rest(
		logging=@Logging(
			disabled="true"
		)
	)
	@SerializerConfig(addRootType="true",addBeanTypes="true")
	public static class InterfaceProxyResource extends BasicRestServletJena {
		private static final long serialVersionUID = 1L;

		//====================================================================================================
		// Test that Q-values are being resolved correctly.
		//====================================================================================================
		@RestMethod(name=RRPC, path="/proxy/*")
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
					return new int[][][]{{{1,2},null},null};
				}

				@Override
				public Integer[][][] returnInteger3dArray() {
					return new Integer[][][]{{{1,null},null},null};
				}

				@Override
				public String[][][] returnString3dArray() {
					return new String[][][]{{{"foo","bar",null},null},null};
				}

				@Override
				public List<Integer> returnIntegerList() {
					return asList(new Integer[]{1,null});
				}

				@Override
				public List<List<List<Integer>>> returnInteger3dList() {
					return AList.of(AList.of(AList.of(1,null),null),null);
				}

				@Override
				public List<Integer[][][]> returnInteger1d3dList() {
					return AList.of(new Integer[][][]{{{1,null},null},null},null);
				}

				@Override
				public List<int[][][]> returnInt1d3dList() {
					return AList.of(new int[][][]{{{1,2},null},null},null);
				}

				@Override
				public List<String> returnStringList() {
					return asList(new String[]{"foo","bar",null});
				}

				// Beans

				@Override
				public ABean returnBean() {
					return new ABean().init();
				}

				@Override
				public ABean[][][] returnBean3dArray() {
					return new ABean[][][]{{{new ABean().init(),null},null},null};
				}

				@Override
				public List<ABean> returnBeanList() {
					return asList(new ABean().init());
				}

				@Override
				public List<ABean[][][]> returnBean1d3dList() {
					return AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null);
				}

				@Override
				public Map<String,ABean> returnBeanMap() {
					return AMap.of("foo",new ABean().init());
				}

				@Override
				public Map<String,List<ABean>> returnBeanListMap() {
					return AMap.of("foo",asList(new ABean().init()));
				}

				@Override
				public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
					return AMap.of("foo", AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
					return AMap.of(1,asList(new ABean().init()));
				}

				// Typed beans

				@Override
				public TypedBean returnTypedBean() {
					return new TypedBeanImpl().init();
				}

				@Override
				public TypedBean[][][] returnTypedBean3dArray() {
					return new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null};
				}

				@Override
				public List<TypedBean> returnTypedBeanList() {
					return asList((TypedBean)new TypedBeanImpl().init());
				}

				@Override
				public List<TypedBean[][][]> returnTypedBean1d3dList() {
					return AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null);
				}

				@Override
				public Map<String,TypedBean> returnTypedBeanMap() {
					return AMap.of("foo",new TypedBeanImpl().init());
				}

				@Override
				public Map<String,List<TypedBean>> returnTypedBeanListMap() {
					return AMap.of("foo",asList((TypedBean)new TypedBeanImpl().init()));
				}

				@Override
				public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
					return AMap.of("foo", AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
					return AMap.of(1,asList((TypedBean)new TypedBeanImpl().init()));
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
					return AMap.of(new SwappedPojo(), new SwappedPojo());
				}

				@Override
				public Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap() {
					return AMap.of(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
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
					return AMap.of(new ImplicitSwappedPojo(), new ImplicitSwappedPojo());
				}

				@Override
				public Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap() {
					return AMap.of(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
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
					return AList.of(TestEnum.TWO,null);
				}

				@Override
				public List<List<List<TestEnum>>> returnEnum3dList() {
					return AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null);
				}

				@Override
				public List<TestEnum[][][]> returnEnum1d3dList() {
					return AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null);
				}

				@Override
				public Map<TestEnum,TestEnum> returnEnumMap() {
					return AMap.of(TestEnum.ONE,TestEnum.TWO);
				}

				@Override
				public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
					return AMap.of(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
				}

				@Override
				public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
					return AMap.of(TestEnum.ONE, AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
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
					assertObjectEquals("[[[1,2],null],null]", x);
				}

				@Override
				public void setInteger3dArray(Integer[][][] x) {
					assertObjectEquals("[[[1,null],null],null]", x);
				}

				@Override
				public void setString3dArray(String[][][] x) {
					assertObjectEquals("[[['foo',null],null],null]", x);
				}

				@Override
				public void setIntegerList(List<Integer> x) {
					assertObjectEquals("[1,null]", x);
					assertClass(Integer.class, x.get(0));
				}

				@Override
				public void setInteger3dList(List<List<List<Integer>>> x) {
					assertObjectEquals("[[[1,null],null],null]", x);
					assertClass(Integer.class, x.get(0).get(0).get(0));
				}

				@Override
				public void setInteger1d3dList(List<Integer[][][]> x) {
					assertObjectEquals("[[[[1,null],null],null],null]", x);
					assertClass(Integer[][][].class, x.get(0));
					assertClass(Integer.class, x.get(0)[0][0][0]);
				}

				@Override
				public void setInt1d3dList(List<int[][][]> x) {
					assertObjectEquals("[[[[1,2],null],null],null]", x);
					assertClass(int[][][].class, x.get(0));
				}

				@Override
				public void setStringList(List<String> x) {
					assertObjectEquals("['foo','bar',null]", x);
				}

				// Beans

				@Override
				public void setBean(ABean x) {
					assertObjectEquals("{a:1,b:'foo'}", x);
				}

				@Override
				public void setBean3dArray(ABean[][][] x) {
					assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
				}

				@Override
				public void setBeanList(List<ABean> x) {
					assertObjectEquals("[{a:1,b:'foo'}]", x);
				}

				@Override
				public void setBean1d3dList(List<ABean[][][]> x) {
					assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
				}

				@Override
				public void setBeanMap(Map<String,ABean> x) {
					assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
				}

				@Override
				public void setBeanListMap(Map<String,List<ABean>> x) {
					assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
				}

				@Override
				public void setBean1d3dListMap(Map<String,List<ABean[][][]>> x) {
					assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
				}

				@Override
				public void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> x) {
					assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
					assertClass(Integer.class, x.keySet().iterator().next());
				}

				// Typed beans

				@Override
				public void setTypedBean(TypedBean x) {
					assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
					assertClass(TypedBeanImpl.class, x);
				}

				@Override
				public void setTypedBean3dArray(TypedBean[][][] x) {
					assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
					assertClass(TypedBeanImpl.class, x[0][0][0]);
				}

				@Override
				public void setTypedBeanList(List<TypedBean> x) {
					assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
					assertClass(TypedBeanImpl.class, x.get(0));
				}

				@Override
				public void setTypedBean1d3dList(List<TypedBean[][][]> x) {
					assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
					assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanMap(Map<String,TypedBean> x) {
					assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
					assertClass(TypedBeanImpl.class, x.get("foo"));
				}

				@Override
				public void setTypedBeanListMap(Map<String,List<TypedBean>> x) {
					assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
					assertClass(TypedBeanImpl.class, x.get("foo").get(0));
				}

				@Override
				public void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> x) {
					assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
					assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> x) {
					assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);  // Note: JsonSerializer serializes key as string.
					assertClass(TypedBeanImpl.class, x.get(1).get(0));
				}

				// Swapped POJOs

				@Override
				public void setSwappedPojo(SwappedPojo x) {
					assertTrue(x.wasUnswapped);
				}

				@Override
				public void setSwappedPojo3dArray(SwappedPojo[][][] x) {
					assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setSwappedPojoMap(Map<SwappedPojo,SwappedPojo> x) {
					assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
					Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setSwappedPojo3dMap(Map<SwappedPojo,SwappedPojo[][][]> x) {
					assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
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
					assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setImplicitSwappedPojoMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x) {
					assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
					Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setImplicitSwappedPojo3dMap(Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x) {
					assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
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
					assertClass(TestEnum.class, x.get(0));
				}

				@Override
				public void setEnum3dList(List<List<List<TestEnum>>> x) {
					assertObjectEquals("[[['TWO',null],null],null]", x);
					assertClass(TestEnum.class, x.get(0).get(0).get(0));
				}

				@Override
				public void setEnum1d3dList(List<TestEnum[][][]> x) {
					assertObjectEquals("[[[['TWO',null],null],null],null]", x);
					assertClass(TestEnum[][][].class, x.get(0));
				}

				@Override
				public void setEnumMap(Map<TestEnum,TestEnum> x) {
					assertObjectEquals("{ONE:'TWO'}", x);
					Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
					assertClass(TestEnum.class, e.getKey());
					assertClass(TestEnum.class, e.getValue());
				}

				@Override
				public void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x) {
					assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
					Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
					assertClass(TestEnum.class, e.getKey());
					assertClass(TestEnum[][][].class, e.getValue());
				}

				@Override
				public void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x) {
					assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
					Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
					assertClass(TestEnum.class, e.getKey());
					assertClass(TestEnum[][][].class, e.getValue().get(0));
				}

				//--------------------------------------------------------------------------------
				// Test multi-parameters
				//--------------------------------------------------------------------------------

				@Override
				public void setMultiParamsInts(int x1, int[][][] x2, int[][][] x2n, List<int[][][]> x3, List<int[][][]> x3n) {
					assertObjectEquals("1", x1);
					assertObjectEquals("[[[1,2],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[[1,2],null],null],null]", x3);
					assertClass(int[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsInteger(Integer x1, Integer x1n, Integer[][][] x2, Integer[][][] x2n, List<Integer[][][]> x3, List<Integer[][][]> x3n) {
					assertObjectEquals("1", x1);
					assertObjectEquals("[[[1,null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[[1,null],null],null],null]", x3);
					assertClass(Integer[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloat(float x1, float[][][] x2, float[][][] x2n, List<float[][][]> x3, List<float[][][]> x3n) {
					assertObjectEquals("1.0", x1);
					assertObjectEquals("[[[1.0,2.0],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[[1.0,2.0],null],null],null]", x3);
					assertClass(float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloatObject(Float x1, Float x1n, Float[][][] x2, Float[][][] x2n, List<Float[][][]> x3, List<Float[][][]> x3n) {
					assertObjectEquals("1.0", x1);
					assertObjectEquals("[[[1.0,null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[[1.0,null],null],null],null]", x3);
					assertClass(Float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsString(String x1, String[][][] x2, String[][][] x2n, List<String[][][]> x3, List<String[][][]> x3n) {
					assertObjectEquals("'foo'", x1);
					assertObjectEquals("[[['foo',null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[['foo',null],null],null],null]", x3);
					assertClass(String[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsBean(ABean x1, ABean[][][] x2, ABean[][][] x2n, List<ABean[][][]> x3, List<ABean[][][]> x3n, Map<String,ABean> x4, Map<String,ABean> x4n, Map<String,List<ABean[][][]>> x5, Map<String,List<ABean[][][]>> x5n) {
					assertObjectEquals("{a:1,b:'foo'}", x1);
					assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x3);
					assertClass(ABean[][][].class, x3.get(0));
					assertNull(x3n);
					assertObjectEquals("{foo:{a:1,b:'foo'}}", x4);
					assertNull(x4n);
					assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x5);
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsSwappedPojo(SwappedPojo x1, SwappedPojo[][][] x2, SwappedPojo[][][] x2n, List<SwappedPojo[][][]> x3, List<SwappedPojo[][][]> x3n, Map<SwappedPojo,SwappedPojo> x4, Map<SwappedPojo,SwappedPojo> x4n, Map<SwappedPojo,List<SwappedPojo[][][]>> x5, Map<SwappedPojo,List<SwappedPojo[][][]>> x5n) {
					assertObjectEquals("'"+SWAP+"'", x1);
					assertObjectEquals("[[['"+SWAP+"',null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[['"+SWAP+"',null],null],null],null]", x3);
					assertClass(SwappedPojo[][][].class, x3.get(0));
					assertNull(x3n);
					assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x4);
					assertNull(x4n);
					assertObjectEquals("{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}", x5);
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsImplicitSwappedPojo(ImplicitSwappedPojo x1, ImplicitSwappedPojo[][][] x2, ImplicitSwappedPojo[][][] x2n, List<ImplicitSwappedPojo[][][]> x3, List<ImplicitSwappedPojo[][][]> x3n, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n) {
					assertObjectEquals("'"+SWAP+"'", x1);
					assertObjectEquals("[[['"+SWAP+"',null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[['"+SWAP+"',null],null],null],null]", x3);
					assertClass(ImplicitSwappedPojo[][][].class, x3.get(0));
					assertNull(x3n);
					assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x4);
					assertNull(x4n);
					assertObjectEquals("{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}", x5);
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsEnum(TestEnum x1, TestEnum[][][] x2, TestEnum[][][] x2n, List<TestEnum[][][]> x3, List<TestEnum[][][]> x3n, Map<TestEnum,TestEnum> x4, Map<TestEnum,TestEnum> x4n, Map<TestEnum,List<TestEnum[][][]>> x5, Map<TestEnum,List<TestEnum[][][]>> x5n) {
					assertObjectEquals("'TWO'", x1);
					assertObjectEquals("[[['TWO',null],null],null]", x2);
					assertNull(x2n);
					assertObjectEquals("[[[['TWO',null],null],null],null]", x3);
					assertClass(TestEnum[][][].class, x3.get(0));
					assertNull(x3n);
					assertObjectEquals("{ONE:'TWO'}", x4);
					assertNull(x4n);
					assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x5);
					assertNull(x5n);
				}
			};
		}
	}
	static MockRest interfaceProxyResource = MockRest.build(InterfaceProxyResource.class);

	private static Map<String,InterfaceProxy> cache = new LinkedHashMap<>();

	private InterfaceProxy proxy;

	public InterfaceProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = cache.get(label);
		if (proxy == null) {
			proxy = MockRestClient.create(InterfaceProxyResource.class).serializer(serializer).parser(parser).build().getRrpcInterface(InterfaceProxy.class, "/proxy");
			cache.put(label, proxy);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

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
		assertObjectEquals("[[['TWO',null],null],null]", x);
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

	//-----------------------------------------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//-----------------------------------------------------------------------------------------------------------------

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

	//-----------------------------------------------------------------------------------------------------------------
	// Test parameters
	//-----------------------------------------------------------------------------------------------------------------

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
		proxy.setIntegerList(AList.of(1,null));
	}

	@Test
	public void h15_setInteger3dList() {
		proxy.setInteger3dList(
			AList.of(AList.of(AList.of(1,null),null),null));
	}

	@Test
	public void h16_setInteger1d3dList() {
		proxy.setInteger1d3dList(
			AList.of(new Integer[][][]{{{1,null},null},null},null)
		);
	}

	@Test
	public void h17_setInt1d3dList() {
		proxy.setInt1d3dList(
			AList.of(new int[][][]{{{1,2},null},null},null)
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
		proxy.setBean1d3dList(AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null));
	}

	@Test
	public void h23_setBeanMap() {
		proxy.setBeanMap(AMap.of("foo",new ABean().init()));
	}

	@Test
	public void h24_setBeanListMap() {
		proxy.setBeanListMap(AMap.of("foo",Arrays.asList(new ABean().init())));
	}

	@Test
	public void h25_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(AMap.of("foo",AList.of(new ABean[][][]{{{new ABean().init(),null},null},null},null)));
	}

	@Test
	public void h26_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(AMap.of(1,Arrays.asList(new ABean().init())));
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
		proxy.setTypedBean1d3dList(AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null));
	}

	@Test
	public void i05_setTypedBeanMap() {
		proxy.setTypedBeanMap(AMap.of("foo",new TypedBeanImpl().init()));
	}

	@Test
	public void i06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(AMap.of("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	@Test
	public void i07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(AMap.of("foo",AList.of(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},null)));
	}

	@Test
	public void i08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(AMap.of(1,Arrays.asList((TypedBean)new TypedBeanImpl().init())));
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
		proxy.setSwappedPojoMap(AMap.of(new SwappedPojo(),new SwappedPojo()));
	}

	@Test
	public void j04_setSwappedPojo3dMap() {
		proxy.setSwappedPojo3dMap(AMap.of(new SwappedPojo(),new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null}));
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
		proxy.setImplicitSwappedPojoMap(AMap.of(new ImplicitSwappedPojo(),new ImplicitSwappedPojo()));
	}

	@Test
	public void k04_setImplicitSwappedPojo3dMap() {
		proxy.setImplicitSwappedPojo3dMap(AMap.of(new ImplicitSwappedPojo(),new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null}));
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
		proxy.setEnumList(AList.of(TestEnum.TWO,null));
	}

	@Test
	public void l04_setEnum3dList() {
		proxy.setEnum3dList(AList.of(AList.of(AList.of(TestEnum.TWO,null),null),null));
	}

	@Test
	public void l05_setEnum1d3dList() {
		proxy.setEnum1d3dList(AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
	}

	@Test
	public void l06_setEnumMap() {
		proxy.setEnumMap(AMap.of(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void l07_setEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(AMap.of(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void l08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(AMap.of(TestEnum.ONE, AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test multi-parameters
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void m01_setMultiParamsInts() {
		int x1 = 1;
		int[][][] x2 = new int[][][]{{{1,2},null},null};
		int[][][] x2n = null;
		List<int[][][]> x3 = AList.of(x2,null);
		List<int[][][]> x3n = null;
		proxy.setMultiParamsInts(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m02_setMultiParamsInteger() {
		Integer x1 = 1;
		Integer x1n = null;
		Integer[][][] x2 = new Integer[][][]{{{1,null},null},null};
		Integer[][][] x2n = null;
		List<Integer[][][]> x3 = AList.of(x2,null);
		List<Integer[][][]> x3n = null;
		proxy.setMultiParamsInteger(x1, x1n, x2, x2n, x3, x3n);
	}

	@Test
	public void m03_setMultiParamsFloat() {
		float x1 = 1;
		float[][][] x2 = new float[][][]{{{1,2},null},null};
		float[][][] x2n = null;
		List<float[][][]> x3 = AList.of(x2,null);
		List<float[][][]> x3n = null;
		proxy.setMultiParamsFloat(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m04_setMultiParamsFloatObject() {
		Float x1 = 1f;
		Float x1n = null;
		Float[][][] x2 = new Float[][][]{{{1f,null},null},null};
		Float[][][] x2n = null;
		List<Float[][][]> x3 = AList.of(x2,null);
		List<Float[][][]> x3n = null;
		proxy.setMultiParamsFloatObject(x1, x1n, x2, x2n, x3, x3n);
	}

	@Test
	public void m05_setMultiParamsString() {
		String x1 = "foo";
		String[][][] x2 = new String[][][]{{{"foo",null},null},null};
		String[][][] x2n = null;
		List<String[][][]> x3 = AList.of(x2,null);
		List<String[][][]> x3n = null;
		proxy.setMultiParamsString(x1, x2, x2n, x3, x3n);
	}

	@Test
	public void m06_setMultiParamsBean() {
		ABean x1 = new ABean().init();
		ABean[][][] x2 = new ABean[][][]{{{new ABean().init(),null},null},null};
		ABean[][][] x2n = null;
		List<ABean[][][]> x3 = AList.of(x2,null);
		List<ABean[][][]> x3n = null;
		Map<String,ABean> x4 = AMap.of("foo",new ABean().init());
		Map<String,ABean> x4n = null;
		Map<String,List<ABean[][][]>> x5 = AMap.of("foo",x3);
		Map<String,List<ABean[][][]>> x5n = null;
		proxy.setMultiParamsBean(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m07_setMultiParamsSwappedPojo() {
		SwappedPojo x1 = new SwappedPojo();
		SwappedPojo[][][] x2 = new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null};
		SwappedPojo[][][] x2n = null;
		List<SwappedPojo[][][]> x3 = AList.of(x2,null);
		List<SwappedPojo[][][]> x3n = null;
		Map<SwappedPojo,SwappedPojo> x4 = AMap.of(new SwappedPojo(),new SwappedPojo());
		Map<SwappedPojo,SwappedPojo> x4n = null;
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5 = AMap.of(new SwappedPojo(),x3);
		Map<SwappedPojo,List<SwappedPojo[][][]>> x5n = null;
		proxy.setMultiParamsSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m08_setMultiParamsImplicitSwappedPojo() {
		ImplicitSwappedPojo x1 = new ImplicitSwappedPojo();
		ImplicitSwappedPojo[][][] x2 = new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null};
		ImplicitSwappedPojo[][][] x2n = null;
		List<ImplicitSwappedPojo[][][]> x3 = AList.of(x2,null);
		List<ImplicitSwappedPojo[][][]> x3n = null;
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4 = AMap.of(new ImplicitSwappedPojo(),new ImplicitSwappedPojo());
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n = null;
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5 = AMap.of(new ImplicitSwappedPojo(),x3);
		Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n = null;
		proxy.setMultiParamsImplicitSwappedPojo(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}

	@Test
	public void m09_setMultiParamsEnum() {
		TestEnum x1 = TestEnum.TWO;
		TestEnum[][][] x2 = new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
		TestEnum[][][] x2n = null;
		List<TestEnum[][][]> x3 = AList.of(x2,null);
		List<TestEnum[][][]> x3n = null;
		Map<TestEnum,TestEnum> x4 = AMap.of(TestEnum.ONE,TestEnum.TWO);
		Map<TestEnum,TestEnum> x4n = null;
		Map<TestEnum,List<TestEnum[][][]>> x5 = AMap.of(TestEnum.ONE,x3);
		Map<TestEnum,List<TestEnum[][][]>> x5n = null;
		proxy.setMultiParamsEnum(x1, x2, x2n, x3, x3n, x4, x4n, x5, x5n);
	}
}
