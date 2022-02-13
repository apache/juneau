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
package org.apache.juneau.http.remote;

import static java.util.Arrays.*;
import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.testutils.Constants.*;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.logging.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.servlet.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.testutils.pojos.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(NAME_ASCENDING)
@RunWith(Parameterized.class)
public class RrpcInterfaceTest {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfXmlAbbrevSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), RdfXmlParser.DEFAULT },
		});
	}

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
		SwappedObject returnSwappedObject();
		SwappedObject[][][] returnSwappedObject3dArray();
		Map<SwappedObject,SwappedObject> returnSwappedObjectMap();
		Map<SwappedObject,SwappedObject[][][]> returnSwappedObject3dMap();

		// Implicit swapped POJOs
		ImplicitSwappedObject returnImplicitSwappedObject();
		ImplicitSwappedObject[][][] returnImplicitSwappedObject3dArray();
		Map<ImplicitSwappedObject,ImplicitSwappedObject> returnImplicitSwappedObjectMap();
		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> returnImplicitSwappedObject3dMap();

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
		void setInt(int x) throws AssertionError;
		void setInteger(Integer x);
		void setBoolean(boolean x);
		void setFloat(float x);
		void setFloatObject(Float x);
		void setString(String x);
		void setNullString(String x) throws AssertionError;
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
		void setSwappedObject(SwappedObject x);
		void setSwappedObject3dArray(SwappedObject[][][] x);
		void setSwappedObjectMap(Map<SwappedObject,SwappedObject> x);
		void setSwappedObject3dMap(Map<SwappedObject,SwappedObject[][][]> x);

		// Implicit swapped POJOs
		void setImplicitSwappedObject(ImplicitSwappedObject x);
		void setImplicitSwappedObject3dArray(ImplicitSwappedObject[][][] x);
		void setImplicitSwappedObjectMap(Map<ImplicitSwappedObject,ImplicitSwappedObject> x);
		void setImplicitSwappedObject3dMap(Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x);

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

		void setMultiParamsInts(int x1,int[][][] x2,int[][][] x2n,List<int[][][]> x3,List<int[][][]> x3n);
		void setMultiParamsInteger(Integer x1,Integer x1n,Integer[][][] x2,Integer[][][] x2n,List<Integer[][][]> x3,List<Integer[][][]> x3n);
		void setMultiParamsFloat(float x1,float[][][] x2,float[][][] x2n,List<float[][][]> x3,List<float[][][]> x3n);
		void setMultiParamsFloatObject(Float x1,Float x1n,Float[][][] x2,Float[][][] x2n,List<Float[][][]> x3,List<Float[][][]> x3n);
		void setMultiParamsString(String x1,String[][][] x2,String[][][] x2n,List<String[][][]> x3,List<String[][][]> x3n);
		void setMultiParamsBean(ABean x1,ABean[][][] x2,ABean[][][] x2n,List<ABean[][][]> x3,List<ABean[][][]> x3n,Map<String,ABean> x4,Map<String,ABean> x4n,Map<String,List<ABean[][][]>> x5,Map<String,List<ABean[][][]>> x5n);
		void setMultiParamsSwappedObject(SwappedObject x1,SwappedObject[][][] x2,SwappedObject[][][] x2n,List<SwappedObject[][][]> x3,List<SwappedObject[][][]> x3n,Map<SwappedObject,SwappedObject> x4,Map<SwappedObject,SwappedObject> x4n,Map<SwappedObject,List<SwappedObject[][][]>> x5,Map<SwappedObject,List<SwappedObject[][][]>> x5n);
		void setMultiParamsImplicitSwappedObject(ImplicitSwappedObject x1,ImplicitSwappedObject[][][] x2,ImplicitSwappedObject[][][] x2n,List<ImplicitSwappedObject[][][]> x3,List<ImplicitSwappedObject[][][]> x3n,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n);
		void setMultiParamsEnum(TestEnum x1,TestEnum[][][] x2,TestEnum[][][] x2n,List<TestEnum[][][]> x3,List<TestEnum[][][]> x3n,Map<TestEnum,TestEnum> x4,Map<TestEnum,TestEnum> x4n,Map<TestEnum,List<TestEnum[][][]>> x5,Map<TestEnum,List<TestEnum[][][]>> x5n);

		//-------------------------------------------------------------------------------------------------------------
		// Helper classes
		//-------------------------------------------------------------------------------------------------------------

		@SuppressWarnings("serial")
		public static class InterfaceProxyException1 extends Exception {
			public InterfaceProxyException1(String msg) {
				super(msg);
			}
		}

		@SuppressWarnings("serial")
		public static class InterfaceProxyException2 extends Exception {
		}
	}

	@Rest(callLogger=BasicDisabledRestLogger.class)
	@SerializerConfig(addRootType="true",addBeanTypes="true")
	public static class InterfaceProxyResource extends BasicRestServlet implements BasicUniversalJenaConfig {
		private static final long serialVersionUID = 1L;

		//-----------------------------------------------------------------------------------------------------------------
		// Test that Q-values are being resolved correctly.
		//-----------------------------------------------------------------------------------------------------------------
		@RestOp(method=RRPC,path="/proxy/*")
		public InterfaceProxy proxy() {
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
					return ABean.get();
				}

				@Override
				public ABean[][][] returnBean3dArray() {
					return new ABean[][][]{{{ABean.get(),null},null},null};
				}

				@Override
				public List<ABean> returnBeanList() {
					return asList(ABean.get());
				}

				@Override
				public List<ABean[][][]> returnBean1d3dList() {
					return AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null);
				}

				@Override
				public Map<String,ABean> returnBeanMap() {
					return AMap.of("foo",ABean.get());
				}

				@Override
				public Map<String,List<ABean>> returnBeanListMap() {
					return AMap.of("foo",asList(ABean.get()));
				}

				@Override
				public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
					return AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
					return AMap.of(1,asList(ABean.get()));
				}

				// Typed beans

				@Override
				public TypedBean returnTypedBean() {
					return TypedBeanImpl.get();
				}

				@Override
				public TypedBean[][][] returnTypedBean3dArray() {
					return new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null};
				}

				@Override
				public List<TypedBean> returnTypedBeanList() {
					return asList((TypedBean)TypedBeanImpl.get());
				}

				@Override
				public List<TypedBean[][][]> returnTypedBean1d3dList() {
					return AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null);
				}

				@Override
				public Map<String,TypedBean> returnTypedBeanMap() {
					return AMap.of("foo",TypedBeanImpl.get());
				}

				@Override
				public Map<String,List<TypedBean>> returnTypedBeanListMap() {
					return AMap.of("foo",asList((TypedBean)TypedBeanImpl.get()));
				}

				@Override
				public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
					return AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
					return AMap.of(1,asList((TypedBean)TypedBeanImpl.get()));
				}

				// Swapped POJOs

				@Override
				public SwappedObject returnSwappedObject() {
					return new SwappedObject();
				}

				@Override
				public SwappedObject[][][] returnSwappedObject3dArray() {
					return new SwappedObject[][][]{{{new SwappedObject(),null},null},null};
				}

				@Override
				public Map<SwappedObject,SwappedObject> returnSwappedObjectMap() {
					return AMap.of(new SwappedObject(),new SwappedObject());
				}

				@Override
				public Map<SwappedObject,SwappedObject[][][]> returnSwappedObject3dMap() {
					return AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
				}

				// Implicit swapped POJOs

				@Override
				public ImplicitSwappedObject returnImplicitSwappedObject() {
					return new ImplicitSwappedObject();
				}

				@Override
				public ImplicitSwappedObject[][][] returnImplicitSwappedObject3dArray() {
					return new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null};
				}

				@Override
				public Map<ImplicitSwappedObject,ImplicitSwappedObject> returnImplicitSwappedObjectMap() {
					return AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject());
				}

				@Override
				public Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> returnImplicitSwappedObject3dMap() {
					return AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
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
					return AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
				}

				@Override
				public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
					return AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
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
					assertEquals(1,x);
				}

				@Override
				public void setInteger(Integer x) {
					assertEquals((Integer)1,x);
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
					assertEquals("foo",x);
				}

				@Override
				public void setNullString(String x) {
					assertNull(x);
				}

				@Override
				public void setInt3dArray(int[][][] x) {
					assertObject(x).asJson().is("[[[1,2],null],null]");
				}

				@Override
				public void setInteger3dArray(Integer[][][] x) {
					assertObject(x).asJson().is("[[[1,null],null],null]");
				}

				@Override
				public void setString3dArray(String[][][] x) {
					assertObject(x).asJson().is("[[['foo',null],null],null]");
				}

				@Override
				public void setIntegerList(List<Integer> x) {
					assertObject(x).asJson().is("[1,null]");
					assertObject(x.get(0)).isType(Integer.class);
				}

				@Override
				public void setInteger3dList(List<List<List<Integer>>> x) {
					assertObject(x).asJson().is("[[[1,null],null],null]");
					assertObject(x.get(0).get(0).get(0)).isType(Integer.class);
				}

				@Override
				public void setInteger1d3dList(List<Integer[][][]> x) {
					assertObject(x).asJson().is("[[[[1,null],null],null],null]");
					assertObject(x.get(0)).isType(Integer[][][].class);
					assertObject(x.get(0)[0][0][0]).isType(Integer.class);
				}

				@Override
				public void setInt1d3dList(List<int[][][]> x) {
					assertObject(x).asJson().is("[[[[1,2],null],null],null]");
					assertObject(x.get(0)).isType(int[][][].class);
				}

				@Override
				public void setStringList(List<String> x) {
					assertObject(x).asJson().is("['foo','bar',null]");
				}

				// Beans

				@Override
				public void setBean(ABean x) {
					assertObject(x).asJson().is("{a:1,b:'foo'}");
				}

				@Override
				public void setBean3dArray(ABean[][][] x) {
					assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
				}

				@Override
				public void setBeanList(List<ABean> x) {
					assertObject(x).asJson().is("[{a:1,b:'foo'}]");
				}

				@Override
				public void setBean1d3dList(List<ABean[][][]> x) {
					assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
				}

				@Override
				public void setBeanMap(Map<String,ABean> x) {
					assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
				}

				@Override
				public void setBeanListMap(Map<String,List<ABean>> x) {
					assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
				}

				@Override
				public void setBean1d3dListMap(Map<String,List<ABean[][][]>> x) {
					assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
				}

				@Override
				public void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> x) {
					assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");  // Note: JsonSerializer serializes key as string.
					assertObject(x.keySet().iterator().next()).isType(Integer.class);
				}

				// Typed beans

				@Override
				public void setTypedBean(TypedBean x) {
					assertObject(x).asJson().is("{a:1,b:'foo'}");
					assertObject(x).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBean3dArray(TypedBean[][][] x) {
					assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
					assertObject(x[0][0][0]).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBeanList(List<TypedBean> x) {
					assertObject(x).asJson().is("[{a:1,b:'foo'}]");
					assertObject(x.get(0)).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBean1d3dList(List<TypedBean[][][]> x) {
					assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
					assertObject(x.get(0)[0][0][0]).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBeanMap(Map<String,TypedBean> x) {
					assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
					assertObject(x.get("foo")).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBeanListMap(Map<String,List<TypedBean>> x) {
					assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
					assertObject(x.get("foo").get(0)).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> x) {
					assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
					assertObject(x.get("foo").get(0)[0][0][0]).isType(TypedBeanImpl.class);
				}

				@Override
				public void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> x) {
					assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");  // Note: JsonSerializer serializes key as string.
					assertObject(x.get(1).get(0)).isType(TypedBeanImpl.class);
				}

				// Swapped POJOs

				@Override
				public void setSwappedObject(SwappedObject x) {
					assertTrue(x.wasUnswapped);
				}

				@Override
				public void setSwappedObject3dArray(SwappedObject[][][] x) {
					assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setSwappedObjectMap(Map<SwappedObject,SwappedObject> x) {
					assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
					Map.Entry<SwappedObject,SwappedObject> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setSwappedObject3dMap(Map<SwappedObject,SwappedObject[][][]> x) {
					assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
					Map.Entry<SwappedObject,SwappedObject[][][]> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue()[0][0][0].wasUnswapped);
				}

				// Implicit swapped POJOs

				@Override
				public void setImplicitSwappedObject(ImplicitSwappedObject x) {
					assertTrue(x.wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObject3dArray(ImplicitSwappedObject[][][] x) {
					assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObjectMap(Map<ImplicitSwappedObject,ImplicitSwappedObject> x) {
					assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
					Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObject3dMap(Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x) {
					assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
					Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject[][][]> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue()[0][0][0].wasUnswapped);
				}

				// Enums

				@Override
				public void setEnum(TestEnum x) {
					assertEquals(TestEnum.TWO,x);
				}

				@Override
				public void setEnum3d(TestEnum[][][] x) {
					assertObject(x).asJson().is("[[['TWO',null],null],null]");
				}

				@Override
				public void setEnumList(List<TestEnum> x) {
					assertObject(x).asJson().is("['TWO',null]");
					assertObject(x.get(0)).isType(TestEnum.class);
				}

				@Override
				public void setEnum3dList(List<List<List<TestEnum>>> x) {
					assertObject(x).asJson().is("[[['TWO',null],null],null]");
					assertObject(x.get(0).get(0).get(0)).isType(TestEnum.class);
				}

				@Override
				public void setEnum1d3dList(List<TestEnum[][][]> x) {
					assertObject(x).asJson().is("[[[['TWO',null],null],null],null]");
					assertObject(x.get(0)).isType(TestEnum[][][].class);
				}

				@Override
				public void setEnumMap(Map<TestEnum,TestEnum> x) {
					assertObject(x).asJson().is("{ONE:'TWO'}");
					Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
					assertObject(e.getKey()).isType(TestEnum.class);
					assertObject(e.getValue()).isType(TestEnum.class);
				}

				@Override
				public void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x) {
					assertObject(x).asJson().is("{ONE:[[['TWO',null],null],null]}");
					Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
					assertObject(e.getKey()).isType(TestEnum.class);
					assertObject(e.getValue()).isType(TestEnum[][][].class);
				}

				@Override
				public void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x) {
					assertObject(x).asJson().is("{ONE:[[[['TWO',null],null],null],null]}");
					Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
					assertObject(e.getKey()).isType(TestEnum.class);
					assertObject(e.getValue().get(0)).isType(TestEnum[][][].class);
				}

				//--------------------------------------------------------------------------------
				// Test multi-parameters
				//--------------------------------------------------------------------------------

				@Override
				public void setMultiParamsInts(int x1,int[][][] x2,int[][][] x2n,List<int[][][]> x3,List<int[][][]> x3n) {
					assertObject((Object) x1).asJson().is("1");
					assertObject(x2).asJson().is("[[[1,2],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[[1,2],null],null],null]");
					assertObject(x3.get(0)).isType(int[][][].class);
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsInteger(Integer x1,Integer x1n,Integer[][][] x2,Integer[][][] x2n,List<Integer[][][]> x3,List<Integer[][][]> x3n) {
					assertObject(x1).asJson().is("1");
					assertObject(x2).asJson().is("[[[1,null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[[1,null],null],null],null]");
					assertObject(x3.get(0)).isType(Integer[][][].class);
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloat(float x1,float[][][] x2,float[][][] x2n,List<float[][][]> x3,List<float[][][]> x3n) {
					assertObject((Object) x1).asJson().is("1.0");
					assertObject(x2).asJson().is("[[[1.0,2.0],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[[1.0,2.0],null],null],null]");
					assertObject(x3.get(0)).isType(float[][][].class);
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloatObject(Float x1,Float x1n,Float[][][] x2,Float[][][] x2n,List<Float[][][]> x3,List<Float[][][]> x3n) {
					assertObject(x1).asJson().is("1.0");
					assertObject(x2).asJson().is("[[[1.0,null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[[1.0,null],null],null],null]");
					assertObject(x3.get(0)).isType(Float[][][].class);
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsString(String x1,String[][][] x2,String[][][] x2n,List<String[][][]> x3,List<String[][][]> x3n) {
					assertObject(x1).asJson().is("'foo'");
					assertObject(x2).asJson().is("[[['foo',null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[['foo',null],null],null],null]");
					assertObject(x3.get(0)).isType(String[][][].class);
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsBean(ABean x1,ABean[][][] x2,ABean[][][] x2n,List<ABean[][][]> x3,List<ABean[][][]> x3n,Map<String,ABean> x4,Map<String,ABean> x4n,Map<String,List<ABean[][][]>> x5,Map<String,List<ABean[][][]>> x5n) {
					assertObject(x1).asJson().is("{a:1,b:'foo'}");
					assertObject(x2).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
					assertObject(x3.get(0)).isType(ABean[][][].class);
					assertNull(x3n);
					assertObject(x4).asJson().is("{foo:{a:1,b:'foo'}}");
					assertNull(x4n);
					assertObject(x5).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsSwappedObject(SwappedObject x1,SwappedObject[][][] x2,SwappedObject[][][] x2n,List<SwappedObject[][][]> x3,List<SwappedObject[][][]> x3n,Map<SwappedObject,SwappedObject> x4,Map<SwappedObject,SwappedObject> x4n,Map<SwappedObject,List<SwappedObject[][][]>> x5,Map<SwappedObject,List<SwappedObject[][][]>> x5n) {
					assertObject(x1).asJson().is("'"+SWAP+"'");
					assertObject(x2).asJson().is("[[['"+SWAP+"',null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[['"+SWAP+"',null],null],null],null]");
					assertObject(x3.get(0)).isType(SwappedObject[][][].class);
					assertNull(x3n);
					assertObject(x4).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
					assertNull(x4n);
					assertObject(x5).asJson().is("{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsImplicitSwappedObject(ImplicitSwappedObject x1,ImplicitSwappedObject[][][] x2,ImplicitSwappedObject[][][] x2n,List<ImplicitSwappedObject[][][]> x3,List<ImplicitSwappedObject[][][]> x3n,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n) {
					assertObject(x1).asJson().is("'"+SWAP+"'");
					assertObject(x2).asJson().is("[[['"+SWAP+"',null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[['"+SWAP+"',null],null],null],null]");
					assertObject(x3.get(0)).isType(ImplicitSwappedObject[][][].class);
					assertNull(x3n);
					assertObject(x4).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
					assertNull(x4n);
					assertObject(x5).asJson().is("{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsEnum(TestEnum x1,TestEnum[][][] x2,TestEnum[][][] x2n,List<TestEnum[][][]> x3,List<TestEnum[][][]> x3n,Map<TestEnum,TestEnum> x4,Map<TestEnum,TestEnum> x4n,Map<TestEnum,List<TestEnum[][][]>> x5,Map<TestEnum,List<TestEnum[][][]>> x5n) {
					assertObject(x1).asJson().is("'TWO'");
					assertObject(x2).asJson().is("[[['TWO',null],null],null]");
					assertNull(x2n);
					assertObject(x3).asJson().is("[[[['TWO',null],null],null],null]");
					assertObject(x3.get(0)).isType(TestEnum[][][].class);
					assertNull(x3n);
					assertObject(x4).asJson().is("{ONE:'TWO'}");
					assertNull(x4n);
					assertObject(x5).asJson().is("{ONE:[[[['TWO',null],null],null],null]}");
					assertNull(x5n);
				}
			};
		}
	}

	private static Map<String,InterfaceProxy> cache = new LinkedHashMap<>();

	private InterfaceProxy proxy;

	public RrpcInterfaceTest(String label, Serializer serializer, Parser parser) {
		proxy = cache.get(label);
		if (proxy == null) {
			proxy = MockRestClient.create(InterfaceProxyResource.class).serializer(serializer).parser(parser).noTrace().build().getRrpcInterface(InterfaceProxy.class,"/proxy");
			cache.put(label,proxy);
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
		assertEquals((Integer)1,proxy.returnInteger());
	}

	@Test
	public void a03_returnInt() {
		assertEquals(1,proxy.returnInt());
	}

	@Test
	public void a04_returnBoolean() {
		assertEquals(true,proxy.returnBoolean());
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
		assertEquals("foobar",proxy.returnString());
	}

	@Test
	public void a08_returnNullString() {
		assertNull(proxy.returnNullString());
	}

	@Test
	public void a09_returnInt3dArray() {
		assertObject(proxy.returnInt3dArray()).asJson().is("[[[1,2],null],null]");
	}

	@Test
	public void a10_returnInteger3dArray() {
		assertObject(proxy.returnInteger3dArray()).asJson().is("[[[1,null],null],null]");
	}

	@Test
	public void a11_returnString3dArray() {
		assertObject(proxy.returnString3dArray()).asJson().is("[[['foo','bar',null],null],null]");
	}

	@Test
	public void a12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertObject(x).asJson().is("[1,null]");
		assertObject(x.get(0)).isType(Integer.class);
	}

	@Test
	public void a13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertObject(x).asJson().is("[[[1,null],null],null]");
		assertObject(x.get(0).get(0).get(0)).isType(Integer.class);
	}

	@Test
	public void a14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertObject(x).asJson().is("[[[[1,null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(Integer.class);
	}

	@Test
	public void a15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertObject(x).asJson().is("[[[[1,2],null],null],null]");
		assertObject(x.get(0)).isType(int[][][].class);
	}

	@Test
	public void a16_returnStringList() {
		assertObject(proxy.returnStringList()).asJson().is("['foo','bar',null]");
	}

	// Beans

	@Test
	public void b01_returnBean() {
		ABean x = proxy.returnBean();
		assertObject(x).asJson().is("{a:1,b:'foo'}");
		assertObject(x).isType(ABean.class);
	}

	@Test
	public void b02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertObject(x[0][0][0]).isType(ABean.class);
	}

	@Test
	public void b03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertObject(x).asJson().is("[{a:1,b:'foo'}]");
		assertObject(x.get(0)).isType(ABean.class);
	}

	@Test
	public void b04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void b05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
		assertObject(x.get("foo")).isType(ABean.class);
	}

	@Test
	public void b06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertObject(x.get("foo").get(0)).isType(ABean.class);
	}

	@Test
	public void b07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertObject(x.get("foo").get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void b08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertObject(x.keySet().iterator().next()).isType(Integer.class);
	}

	// Typed beans

	@Test
	public void c01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertObject(x).asJson().is("{a:1,b:'foo'}");
		assertObject(x).isType(TypedBeanImpl.class);
	}

	@Test
	public void c02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertObject(x).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertObject(x[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void c03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertObject(x).asJson().is("[{a:1,b:'foo'}]");
		assertObject(x.get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void c04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertObject(x).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertObject(x.get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void c05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertObject(x).asJson().is("{foo:{a:1,b:'foo'}}");
		assertObject(x.get("foo")).isType(TypedBeanImpl.class);
	}

	@Test
	public void c06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertObject(x).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertObject(x.get("foo").get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void c07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertObject(x).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertObject(x.get("foo").get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void c08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertObject(x).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertObject(x.get(1).get(0)).isType(TypedBeanImpl.class);
	}

	// Swapped POJOs

	@Test
	public void d01_returnSwappedObject() {
		SwappedObject x = proxy.returnSwappedObject();
		assertObject(x).asJson().is("'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void d02_returnSwappedObject3dArray() {
		SwappedObject[][][] x = proxy.returnSwappedObject3dArray();
		assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void d03_returnSwappedObjectMap() {
		Map<SwappedObject,SwappedObject> x = proxy.returnSwappedObjectMap();
		assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<SwappedObject,SwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void d04_returnSwappedObject3dMap() {
		Map<SwappedObject,SwappedObject[][][]> x = proxy.returnSwappedObject3dMap();
		assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<SwappedObject,SwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void e01_returnImplicitSwappedObject() {
		ImplicitSwappedObject x = proxy.returnImplicitSwappedObject();
		assertObject(x).asJson().is("'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void e02_returnImplicitSwappedObject3dArray() {
		ImplicitSwappedObject[][][] x = proxy.returnImplicitSwappedObject3dArray();
		assertObject(x).asJson().is("[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void e03_returnImplicitSwappedObjectMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x = proxy.returnImplicitSwappedObjectMap();
		assertObject(x).asJson().is("{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void e04_returnImplicitSwappedObject3dMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x = proxy.returnImplicitSwappedObject3dMap();
		assertObject(x).asJson().is("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void f01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertObject(x).asJson().is("'TWO'");
	}

	@Test
	public void f02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertObject(x).asJson().is("[[['TWO',null],null],null]");
		assertObject(x[0][0][0]).isType(TestEnum.class);
	}

	@Test
	public void f03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertObject(x).asJson().is("['TWO',null]");
		assertObject(x.get(0)).isType(TestEnum.class);
	}

	@Test
	public void f04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertObject(x).asJson().is("[[['TWO',null],null],null]");
		assertObject(x.get(0).get(0).get(0)).isType(TestEnum.class);
	}

	@Test
	public void f05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertObject(x).asJson().is("[[[['TWO',null],null],null],null]");
		assertObject(x.get(0)).isType(TestEnum[][][].class);
	}

	@Test
	public void f06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertObject(x).asJson().is("{ONE:'TWO'}");
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertObject(e.getKey()).isType(TestEnum.class);
		assertObject(e.getValue()).isType(TestEnum.class);
	}

	@Test
	public void f07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertObject(x).asJson().is("{ONE:[[['TWO',null],null],null]}");
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertObject(e.getKey()).isType(TestEnum.class);
		assertObject(e.getValue()).isType(TestEnum[][][].class);
	}

	@Test
	public void f08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertObject(x).asJson().is("{ONE:[[[['TWO',null],null],null],null]}");
		assertObject(x.get(TestEnum.ONE).get(0)).isType(TestEnum[][][].class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_throwException1() {
		try {
			proxy.throwException1();
			fail();
		} catch (InterfaceProxy.InterfaceProxyException1 e) {
			assertEquals("foo",e.getMessage());
		}
	}

	@Test
	public void g02_throwException2() {
		try {
			proxy.throwException2();
			fail();
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
		assertThrown(()->proxy.setInt(2)).message().is("expected:<1> but was:<2>");
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
		assertThrown(()->proxy.setNullString("foo")).message().is("expected null, but was:<foo>");
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
		proxy.setBean(ABean.get());
	}

	@Test
	public void h20_setBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null});
	}

	@Test
	public void h21_setBeanList() {
		proxy.setBeanList(Arrays.asList(ABean.get()));
	}

	@Test
	public void h22_setBean1d3dList() {
		proxy.setBean1d3dList(AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null));
	}

	@Test
	public void h23_setBeanMap() {
		proxy.setBeanMap(AMap.of("foo",ABean.get()));
	}

	@Test
	public void h24_setBeanListMap() {
		proxy.setBeanListMap(AMap.of("foo",Arrays.asList(ABean.get())));
	}

	@Test
	public void h25_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(AMap.of("foo",AList.of(new ABean[][][]{{{ABean.get(),null},null},null},null)));
	}

	@Test
	public void h26_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(AMap.of(1,Arrays.asList(ABean.get())));
	}

	// Typed beans

	@Test
	public void i01_setTypedBean() {
		proxy.setTypedBean(TypedBeanImpl.get());
	}

	@Test
	public void i02_setTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null});
	}

	@Test
	public void i03_setTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)TypedBeanImpl.get()));
	}

	@Test
	public void i04_setTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
	}

	@Test
	public void i05_setTypedBeanMap() {
		proxy.setTypedBeanMap(AMap.of("foo",TypedBeanImpl.get()));
	}

	@Test
	public void i06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(AMap.of("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	@Test
	public void i07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(AMap.of("foo",AList.of(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
	}

	@Test
	public void i08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(AMap.of(1,Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	// Swapped POJOs

	@Test
	public void j01_setSwappedObject() {
		proxy.setSwappedObject(new SwappedObject());
	}

	@Test
	public void j02_setSwappedObject3dArray() {
		proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
	}

	@Test
	public void j03_setSwappedObjectMap() {
		proxy.setSwappedObjectMap(AMap.of(new SwappedObject(),new SwappedObject()));
	}

	@Test
	public void j04_setSwappedObject3dMap() {
		proxy.setSwappedObject3dMap(AMap.of(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void k01_setImplicitSwappedObject() {
		proxy.setImplicitSwappedObject(new ImplicitSwappedObject());
	}

	@Test
	public void k02_setImplicitSwappedObject3dArray() {
		proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
	}

	@Test
	public void k03_setImplicitSwappedObjectMap() {
		proxy.setImplicitSwappedObjectMap(AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject()));
	}

	@Test
	public void k04_setImplicitSwappedObject3dMap() {
		proxy.setImplicitSwappedObject3dMap(AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
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
		proxy.setEnum3dArrayMap(AMap.of(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void l08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(AMap.of(TestEnum.ONE,AList.of(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
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
		proxy.setMultiParamsInts(x1,x2,x2n,x3,x3n);
	}

	@Test
	public void m02_setMultiParamsInteger() {
		Integer x1 = 1;
		Integer x1n = null;
		Integer[][][] x2 = new Integer[][][]{{{1,null},null},null};
		Integer[][][] x2n = null;
		List<Integer[][][]> x3 = AList.of(x2,null);
		List<Integer[][][]> x3n = null;
		proxy.setMultiParamsInteger(x1,x1n,x2,x2n,x3,x3n);
	}

	@Test
	public void m03_setMultiParamsFloat() {
		float x1 = 1;
		float[][][] x2 = new float[][][]{{{1,2},null},null};
		float[][][] x2n = null;
		List<float[][][]> x3 = AList.of(x2,null);
		List<float[][][]> x3n = null;
		proxy.setMultiParamsFloat(x1,x2,x2n,x3,x3n);
	}

	@Test
	public void m04_setMultiParamsFloatObject() {
		Float x1 = 1f;
		Float x1n = null;
		Float[][][] x2 = new Float[][][]{{{1f,null},null},null};
		Float[][][] x2n = null;
		List<Float[][][]> x3 = AList.of(x2,null);
		List<Float[][][]> x3n = null;
		proxy.setMultiParamsFloatObject(x1,x1n,x2,x2n,x3,x3n);
	}

	@Test
	public void m05_setMultiParamsString() {
		String x1 = "foo";
		String[][][] x2 = new String[][][]{{{"foo",null},null},null};
		String[][][] x2n = null;
		List<String[][][]> x3 = AList.of(x2,null);
		List<String[][][]> x3n = null;
		proxy.setMultiParamsString(x1,x2,x2n,x3,x3n);
	}

	@Test
	public void m06_setMultiParamsBean() {
		ABean x1 = ABean.get();
		ABean[][][] x2 = new ABean[][][]{{{ABean.get(),null},null},null};
		ABean[][][] x2n = null;
		List<ABean[][][]> x3 = AList.of(x2,null);
		List<ABean[][][]> x3n = null;
		Map<String,ABean> x4 = AMap.of("foo",ABean.get());
		Map<String,ABean> x4n = null;
		Map<String,List<ABean[][][]>> x5 = AMap.of("foo",x3);
		Map<String,List<ABean[][][]>> x5n = null;
		proxy.setMultiParamsBean(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n);
	}

	@Test
	public void m07_setMultiParamsSwappedObject() {
		SwappedObject x1 = new SwappedObject();
		SwappedObject[][][] x2 = new SwappedObject[][][]{{{new SwappedObject(),null},null},null};
		SwappedObject[][][] x2n = null;
		List<SwappedObject[][][]> x3 = AList.of(x2,null);
		List<SwappedObject[][][]> x3n = null;
		Map<SwappedObject,SwappedObject> x4 = AMap.of(new SwappedObject(),new SwappedObject());
		Map<SwappedObject,SwappedObject> x4n = null;
		Map<SwappedObject,List<SwappedObject[][][]>> x5 = AMap.of(new SwappedObject(),x3);
		Map<SwappedObject,List<SwappedObject[][][]>> x5n = null;
		proxy.setMultiParamsSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n);
	}

	@Test
	public void m08_setMultiParamsImplicitSwappedObject() {
		ImplicitSwappedObject x1 = new ImplicitSwappedObject();
		ImplicitSwappedObject[][][] x2 = new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null};
		ImplicitSwappedObject[][][] x2n = null;
		List<ImplicitSwappedObject[][][]> x3 = AList.of(x2,null);
		List<ImplicitSwappedObject[][][]> x3n = null;
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x4 = AMap.of(new ImplicitSwappedObject(),new ImplicitSwappedObject());
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n = null;
		Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5 = AMap.of(new ImplicitSwappedObject(),x3);
		Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n = null;
		proxy.setMultiParamsImplicitSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n);
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
		proxy.setMultiParamsEnum(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n);
	}
}
