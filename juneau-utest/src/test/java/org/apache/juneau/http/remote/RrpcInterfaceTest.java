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
import static org.apache.juneau.AssertionHelpers.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.utest.utils.Constants.*;
import static org.apache.juneau.utest.utils.Utils2.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.logger.*;
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
			{ /* 6 */ "Uon", UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UonParser.DEFAULT }
		});
	}

	public interface InterfaceProxy {

		String SWAP = "swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/";

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

	@Rest(callLogger=BasicDisabledCallLogger.class)
	@SerializerConfig(addRootType="true",addBeanTypes="true")
	public static class InterfaceProxyResource extends BasicRestServlet {
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
				public void returnVoid() {}  // NOSONAR

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
					return alist(alist(alist(1,null),null),null);
				}

				@Override
				public List<Integer[][][]> returnInteger1d3dList() {
					return alist(new Integer[][][]{{{1,null},null},null},null);
				}

				@Override
				public List<int[][][]> returnInt1d3dList() {
					return alist(new int[][][]{{{1,2},null},null},null);
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
					return alist(new ABean[][][]{{{ABean.get(),null},null},null},null);
				}

				@Override
				public Map<String,ABean> returnBeanMap() {
					return map("foo",ABean.get());
				}

				@Override
				public Map<String,List<ABean>> returnBeanListMap() {
					return map("foo",asList(ABean.get()));
				}

				@Override
				public Map<String,List<ABean[][][]>> returnBean1d3dListMap() {
					return map("foo",alist(new ABean[][][]{{{ABean.get(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<ABean>> returnBeanListMapIntegerKeys() {
					return map(1,asList(ABean.get()));
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
					return alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null);
				}

				@Override
				public Map<String,TypedBean> returnTypedBeanMap() {
					return map("foo",TypedBeanImpl.get());
				}

				@Override
				public Map<String,List<TypedBean>> returnTypedBeanListMap() {
					return map("foo",asList((TypedBean)TypedBeanImpl.get()));
				}

				@Override
				public Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap() {
					return map("foo",alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
				}

				@Override
				public Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys() {
					return map(1,asList((TypedBean)TypedBeanImpl.get()));
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
					return map(new SwappedObject(),new SwappedObject());
				}

				@Override
				public Map<SwappedObject,SwappedObject[][][]> returnSwappedObject3dMap() {
					return map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
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
					return map(new ImplicitSwappedObject(),new ImplicitSwappedObject());
				}

				@Override
				public Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> returnImplicitSwappedObject3dMap() {
					return map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
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
					return alist(TestEnum.TWO,null);
				}

				@Override
				public List<List<List<TestEnum>>> returnEnum3dList() {
					return alist(alist(alist(TestEnum.TWO,null),null),null);
				}

				@Override
				public List<TestEnum[][][]> returnEnum1d3dList() {
					return alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null);
				}

				@Override
				public Map<TestEnum,TestEnum> returnEnumMap() {
					return map(TestEnum.ONE,TestEnum.TWO);
				}

				@Override
				public Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap() {
					return map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
				}

				@Override
				public Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap() {
					return map(TestEnum.ONE,alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
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
				public void setNothing() {}  // NOSONAR

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
					assertEquals(1f, x, 0.1f);
				}

				@Override
				public void setFloatObject(Float x) {
					assertEquals(1f, x, 0.1f);
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
					assertJson(x, "[[[1,2],null],null]");
				}

				@Override
				public void setInteger3dArray(Integer[][][] x) {
					assertJson(x, "[[[1,null],null],null]");
				}

				@Override
				public void setString3dArray(String[][][] x) {
					assertJson(x, "[[['foo',null],null],null]");
				}

				@Override
				public void setIntegerList(List<Integer> x) {
					assertJson(x, "[1,null]");
					assertType(Integer.class, x.get(0));
				}

				@Override
				public void setInteger3dList(List<List<List<Integer>>> x) {
					assertJson(x, "[[[1,null],null],null]");
					assertType(Integer.class, x.get(0).get(0).get(0));
				}

				@Override
				public void setInteger1d3dList(List<Integer[][][]> x) {
					assertJson(x, "[[[[1,null],null],null],null]");
					assertType(Integer[][][].class, x.get(0));
					assertType(Integer.class, x.get(0)[0][0][0]);
				}

				@Override
				public void setInt1d3dList(List<int[][][]> x) {
					assertJson(x, "[[[[1,2],null],null],null]");
					assertType(int[][][].class, x.get(0));
				}

				@Override
				public void setStringList(List<String> x) {
					assertJson(x, "['foo','bar',null]");
				}

				// Beans

				@Override
				public void setBean(ABean x) {
					assertJson(x, "{a:1,b:'foo'}");
				}

				@Override
				public void setBean3dArray(ABean[][][] x) {
					assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
				}

				@Override
				public void setBeanList(List<ABean> x) {
					assertJson(x, "[{a:1,b:'foo'}]");
				}

				@Override
				public void setBean1d3dList(List<ABean[][][]> x) {
					assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
				}

				@Override
				public void setBeanMap(Map<String,ABean> x) {
					assertJson(x, "{foo:{a:1,b:'foo'}}");
				}

				@Override
				public void setBeanListMap(Map<String,List<ABean>> x) {
					assertJson(x, "{foo:[{a:1,b:'foo'}]}");
				}

				@Override
				public void setBean1d3dListMap(Map<String,List<ABean[][][]>> x) {
					assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
				}

				@Override
				public void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> x) {
					assertJson(x, "{'1':[{a:1,b:'foo'}]}");  // Note: JsonSerializer serializes key as string.
					assertType(Integer.class, x.keySet().iterator().next());
				}

				// Typed beans

				@Override
				public void setTypedBean(TypedBean x) {
					assertJson(x, "{a:1,b:'foo'}");
					assertType(TypedBeanImpl.class, x);
				}

				@Override
				public void setTypedBean3dArray(TypedBean[][][] x) {
					assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
					assertType(TypedBeanImpl.class, x[0][0][0]);
				}

				@Override
				public void setTypedBeanList(List<TypedBean> x) {
					assertJson(x, "[{a:1,b:'foo'}]");
					assertType(TypedBeanImpl.class, x.get(0));
				}

				@Override
				public void setTypedBean1d3dList(List<TypedBean[][][]> x) {
					assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
					assertType(TypedBeanImpl.class, x.get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanMap(Map<String,TypedBean> x) {
					assertJson(x, "{foo:{a:1,b:'foo'}}");
					assertType(TypedBeanImpl.class, x.get("foo"));
				}

				@Override
				public void setTypedBeanListMap(Map<String,List<TypedBean>> x) {
					assertJson(x, "{foo:[{a:1,b:'foo'}]}");
					assertType(TypedBeanImpl.class, x.get("foo").get(0));
				}

				@Override
				public void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> x) {
					assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
					assertType(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> x) {
					assertJson(x, "{'1':[{a:1,b:'foo'}]}");  // Note: JsonSerializer serializes key as string.
					assertType(TypedBeanImpl.class, x.get(1).get(0));
				}

				// Swapped POJOs

				@Override
				public void setSwappedObject(SwappedObject x) {
					assertTrue(x.wasUnswapped);
				}

				@Override
				public void setSwappedObject3dArray(SwappedObject[][][] x) {
					assertJson(x, "[[['"+SWAP+"',null],null],null]");
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setSwappedObjectMap(Map<SwappedObject,SwappedObject> x) {
					assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
					Map.Entry<SwappedObject,SwappedObject> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setSwappedObject3dMap(Map<SwappedObject,SwappedObject[][][]> x) {
					assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
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
					assertJson(x, "[[['"+SWAP+"',null],null],null]");
					assertTrue(x[0][0][0].wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObjectMap(Map<ImplicitSwappedObject,ImplicitSwappedObject> x) {
					assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
					Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject> e = x.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObject3dMap(Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x) {
					assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
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
					assertJson(x, "[[['TWO',null],null],null]");
				}

				@Override
				public void setEnumList(List<TestEnum> x) {
					assertJson(x, "['TWO',null]");
					assertType(TestEnum.class, x.get(0));
				}

				@Override
				public void setEnum3dList(List<List<List<TestEnum>>> x) {
					assertJson(x, "[[['TWO',null],null],null]");
					assertType(TestEnum.class, x.get(0).get(0).get(0));
				}

				@Override
				public void setEnum1d3dList(List<TestEnum[][][]> x) {
					assertJson(x, "[[[['TWO',null],null],null],null]");
					assertType(TestEnum[][][].class, x.get(0));
				}

				@Override
				public void setEnumMap(Map<TestEnum,TestEnum> x) {
					assertJson(x, "{ONE:'TWO'}");
					Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum.class, e.getValue());
				}

				@Override
				public void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x) {
					assertJson(x, "{ONE:[[['TWO',null],null],null]}");
					Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum[][][].class, e.getValue());
				}

				@Override
				public void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x) {
					assertJson(x, "{ONE:[[[['TWO',null],null],null],null]}");
					Map.Entry<TestEnum,List<TestEnum[][][]>> e = x.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum[][][].class, e.getValue().get(0));
				}

				//--------------------------------------------------------------------------------
				// Test multi-parameters
				//--------------------------------------------------------------------------------

				@Override
				public void setMultiParamsInts(int x1,int[][][] x2,int[][][] x2n,List<int[][][]> x3,List<int[][][]> x3n) {
					assertJson(x1, "1");
					assertJson(x2, "[[[1,2],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[[1,2],null],null],null]");
					assertType(int[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsInteger(Integer x1,Integer x1n,Integer[][][] x2,Integer[][][] x2n,List<Integer[][][]> x3,List<Integer[][][]> x3n) {
					assertJson(x1, "1");
					assertJson(x2, "[[[1,null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[[1,null],null],null],null]");
					assertType(Integer[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloat(float x1,float[][][] x2,float[][][] x2n,List<float[][][]> x3,List<float[][][]> x3n) {
					assertJson(x1, "1.0");
					assertJson(x2, "[[[1.0,2.0],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[[1.0,2.0],null],null],null]");
					assertType(float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloatObject(Float x1,Float x1n,Float[][][] x2,Float[][][] x2n,List<Float[][][]> x3,List<Float[][][]> x3n) {
					assertJson(x1, "1.0");
					assertJson(x2, "[[[1.0,null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[[1.0,null],null],null],null]");
					assertType(Float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsString(String x1,String[][][] x2,String[][][] x2n,List<String[][][]> x3,List<String[][][]> x3n) {
					assertJson(x1, "'foo'");
					assertJson(x2, "[[['foo',null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[['foo',null],null],null],null]");
					assertType(String[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsBean(ABean x1,ABean[][][] x2,ABean[][][] x2n,List<ABean[][][]> x3,List<ABean[][][]> x3n,Map<String,ABean> x4,Map<String,ABean> x4n,Map<String,List<ABean[][][]>> x5,Map<String,List<ABean[][][]>> x5n) {
					assertJson(x1, "{a:1,b:'foo'}");
					assertJson(x2, "[[[{a:1,b:'foo'},null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[[{a:1,b:'foo'},null],null],null],null]");
					assertType(ABean[][][].class, x3.get(0));
					assertNull(x3n);
					assertJson(x4, "{foo:{a:1,b:'foo'}}");
					assertNull(x4n);
					assertJson(x5, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsSwappedObject(SwappedObject x1,SwappedObject[][][] x2,SwappedObject[][][] x2n,List<SwappedObject[][][]> x3,List<SwappedObject[][][]> x3n,Map<SwappedObject,SwappedObject> x4,Map<SwappedObject,SwappedObject> x4n,Map<SwappedObject,List<SwappedObject[][][]>> x5,Map<SwappedObject,List<SwappedObject[][][]>> x5n) {
					assertJson(x1, "'"+SWAP+"'");
					assertJson(x2, "[[['"+SWAP+"',null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[['"+SWAP+"',null],null],null],null]");
					assertType(SwappedObject[][][].class, x3.get(0));
					assertNull(x3n);
					assertJson(x4, "{'"+SWAP+"':'"+SWAP+"'}");
					assertNull(x4n);
					assertJson(x5, "{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsImplicitSwappedObject(ImplicitSwappedObject x1,ImplicitSwappedObject[][][] x2,ImplicitSwappedObject[][][] x2n,List<ImplicitSwappedObject[][][]> x3,List<ImplicitSwappedObject[][][]> x3n,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n) {
					assertJson(x1, "'"+SWAP+"'");
					assertJson(x2, "[[['"+SWAP+"',null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[['"+SWAP+"',null],null],null],null]");
					assertType(ImplicitSwappedObject[][][].class, x3.get(0));
					assertNull(x3n);
					assertJson(x4, "{'"+SWAP+"':'"+SWAP+"'}");
					assertNull(x4n);
					assertJson(x5, "{'"+SWAP+"':[[[['"+SWAP+"',null],null],null],null]}");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsEnum(TestEnum x1,TestEnum[][][] x2,TestEnum[][][] x2n,List<TestEnum[][][]> x3,List<TestEnum[][][]> x3n,Map<TestEnum,TestEnum> x4,Map<TestEnum,TestEnum> x4n,Map<TestEnum,List<TestEnum[][][]>> x5,Map<TestEnum,List<TestEnum[][][]>> x5n) {
					assertJson(x1, "'TWO'");
					assertJson(x2, "[[['TWO',null],null],null]");
					assertNull(x2n);
					assertJson(x3, "[[[['TWO',null],null],null],null]");
					assertType(TestEnum[][][].class, x3.get(0));
					assertNull(x3n);
					assertJson(x4, "{ONE:'TWO'}");
					assertNull(x4n);
					assertJson(x5, "{ONE:[[[['TWO',null],null],null],null]}");
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
		assertNotThrown(()->proxy.returnVoid());
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
		assertEquals(1f, proxy.returnFloat(), 0.1f);
	}

	@Test
	public void a06_returnFloatObject() {
		assertEquals(1f, proxy.returnFloatObject(), 0.1f);
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
		assertJson(proxy.returnInt3dArray(), "[[[1,2],null],null]");
	}

	@Test
	public void a10_returnInteger3dArray() {
		assertJson(proxy.returnInteger3dArray(), "[[[1,null],null],null]");
	}

	@Test
	public void a11_returnString3dArray() {
		assertJson(proxy.returnString3dArray(), "[[['foo','bar',null],null],null]");
	}

	@Test
	public void a12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertJson(x, "[1,null]");
		assertType(Integer.class, x.get(0));
	}

	@Test
	public void a13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertJson(x, "[[[1,null],null],null]");
		assertType(Integer.class, x.get(0).get(0).get(0));
	}

	@Test
	public void a14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertJson(x, "[[[[1,null],null],null],null]");
		assertType(Integer.class, x.get(0)[0][0][0]);
	}

	@Test
	public void a15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertJson(x, "[[[[1,2],null],null],null]");
		assertType(int[][][].class, x.get(0));
	}

	@Test
	public void a16_returnStringList() {
		assertJson(proxy.returnStringList(), "['foo','bar',null]");
	}

	// Beans

	@Test
	public void b01_returnBean() {
		ABean x = proxy.returnBean();
		assertJson(x, "{a:1,b:'foo'}");
		assertType(ABean.class, x);
	}

	@Test
	public void b02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
		assertType(ABean.class, x[0][0][0]);
	}

	@Test
	public void b03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertJson(x, "[{a:1,b:'foo'}]");
		assertType(ABean.class, x.get(0));
	}

	@Test
	public void b04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
		assertType(ABean.class, x.get(0)[0][0][0]);
	}

	@Test
	public void b05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertJson(x, "{foo:{a:1,b:'foo'}}");
		assertType(ABean.class, x.get("foo"));
	}

	@Test
	public void b06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertJson(x, "{foo:[{a:1,b:'foo'}]}");
		assertType(ABean.class, x.get("foo").get(0));
	}

	@Test
	public void b07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertType(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void b08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertJson(x, "{'1':[{a:1,b:'foo'}]}");
		assertType(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@Test
	public void c01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertJson(x, "{a:1,b:'foo'}");
		assertType(TypedBeanImpl.class, x);
	}

	@Test
	public void c02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertJson(x, "[[[{a:1,b:'foo'},null],null],null]");
		assertType(TypedBeanImpl.class, x[0][0][0]);
	}

	@Test
	public void c03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertJson(x, "[{a:1,b:'foo'}]");
		assertType(TypedBeanImpl.class, x.get(0));
	}

	@Test
	public void c04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertJson(x, "[[[[{a:1,b:'foo'},null],null],null],null]");
		assertType(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@Test
	public void c05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertJson(x, "{foo:{a:1,b:'foo'}}");
		assertType(TypedBeanImpl.class, x.get("foo"));
	}

	@Test
	public void c06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertJson(x, "{foo:[{a:1,b:'foo'}]}");
		assertType(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@Test
	public void c07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertJson(x, "{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertType(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void c08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertJson(x, "{'1':[{a:1,b:'foo'}]}");
		assertType(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@Test
	public void d01_returnSwappedObject() {
		SwappedObject x = proxy.returnSwappedObject();
		assertJson(x, "'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void d02_returnSwappedObject3dArray() {
		SwappedObject[][][] x = proxy.returnSwappedObject3dArray();
		assertJson(x, "[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void d03_returnSwappedObjectMap() {
		Map<SwappedObject,SwappedObject> x = proxy.returnSwappedObjectMap();
		assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<SwappedObject,SwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void d04_returnSwappedObject3dMap() {
		Map<SwappedObject,SwappedObject[][][]> x = proxy.returnSwappedObject3dMap();
		assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<SwappedObject,SwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void e01_returnImplicitSwappedObject() {
		ImplicitSwappedObject x = proxy.returnImplicitSwappedObject();
		assertJson(x, "'"+SWAP+"'");
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void e02_returnImplicitSwappedObject3dArray() {
		ImplicitSwappedObject[][][] x = proxy.returnImplicitSwappedObject3dArray();
		assertJson(x, "[[['"+SWAP+"',null],null],null]");
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void e03_returnImplicitSwappedObjectMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x = proxy.returnImplicitSwappedObjectMap();
		assertJson(x, "{'"+SWAP+"':'"+SWAP+"'}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void e04_returnImplicitSwappedObject3dMap() {
		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x = proxy.returnImplicitSwappedObject3dMap();
		assertJson(x, "{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}");
		Map.Entry<ImplicitSwappedObject,ImplicitSwappedObject[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void f01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertJson(x, "'TWO'");
	}

	@Test
	public void f02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertJson(x, "[[['TWO',null],null],null]");
		assertType(TestEnum.class, x[0][0][0]);
	}

	@Test
	public void f03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertJson(x, "['TWO',null]");
		assertType(TestEnum.class, x.get(0));
	}

	@Test
	public void f04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertJson(x, "[[['TWO',null],null],null]");
		assertType(TestEnum.class, x.get(0).get(0).get(0));
	}

	@Test
	public void f05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertJson(x, "[[[['TWO',null],null],null],null]");
		assertType(TestEnum[][][].class, x.get(0));
	}

	@Test
	public void f06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertJson(x, "{ONE:'TWO'}");
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum.class, e.getValue());
	}

	@Test
	public void f07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertJson(x, "{ONE:[[['TWO',null],null],null]}");
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum[][][].class, e.getValue());
	}

	@Test
	public void f08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertJson(x, "{ONE:[[[['TWO',null],null],null],null]}");
		assertType(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
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
		} catch (InterfaceProxy.InterfaceProxyException2 e) {/*no-op*/}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test parameters
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void h01_setNothing() {
		assertNotThrown(()->proxy.setNothing());
	}

	@Test
	public void h02_setInt() {
		assertNotThrown(()->proxy.setInt(1));
	}

	@Test
	public void h03_setWrongInt() {
		assertThrows(AssertionError.class, ()->proxy.setInt(2), "expected:<1> but was:<2>");
	}

	@Test
	public void h04_setInteger() {
		assertNotThrown(()->proxy.setInteger(1));
	}

	@Test
	public void h05_setBoolean() {
		assertNotThrown(()->proxy.setBoolean(true));
	}

	@Test
	public void h06_setFloat() {
		assertNotThrown(()->proxy.setFloat(1f));
	}

	@Test
	public void h07_setFloatObject() {
		assertNotThrown(()->proxy.setFloatObject(1f));
	}

	@Test
	public void h08_setString() {
		assertNotThrown(()->proxy.setString("foo"));
	}

	@Test
	public void h09_setNullString() {
		assertNotThrown(()->proxy.setNullString(null));
	}

	@Test
	public void h10_setNullStringBad() {
		assertThrows(AssertionError.class, ()->proxy.setNullString("foo"), "expected null, but was:<foo>");
	}

	@Test
	public void h11_setInt3dArray() {
		assertNotThrown(()->proxy.setInt3dArray(new int[][][]{{{1,2},null},null}));
	}

	@Test
	public void h12_setInteger3dArray() {
		assertNotThrown(()->proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null}));
	}

	@Test
	public void h13_setString3dArray() {
		assertNotThrown(()->proxy.setString3dArray(new String[][][]{{{"foo",null},null},null}));
	}

	@Test
	public void h14_setIntegerList() {
		assertNotThrown(()->proxy.setIntegerList(alist(1,null)));
	}

	@Test
	public void h15_setInteger3dList() {
		assertNotThrown(()->proxy.setInteger3dList(alist(alist(alist(1,null),null),null)));
	}

	@Test
	public void h16_setInteger1d3dList() {
		assertNotThrown(()->proxy.setInteger1d3dList(alist(new Integer[][][]{{{1,null},null},null},null)));
	}

	@Test
	public void h17_setInt1d3dList() {
		assertNotThrown(()->proxy.setInt1d3dList(alist(new int[][][]{{{1,2},null},null},null)));
	}

	@Test
	public void h18_setStringList() {
		assertNotThrown(()->proxy.setStringList(Arrays.asList("foo","bar",null)));
	}

	// Beans
	@Test
	public void h19_setBean() {
		assertNotThrown(()->proxy.setBean(ABean.get()));
	}

	@Test
	public void h20_setBean3dArray() {
		assertNotThrown(()->proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null}));
	}

	@Test
	public void h21_setBeanList() {
		assertNotThrown(()->proxy.setBeanList(Arrays.asList(ABean.get())));
	}

	@Test
	public void h22_setBean1d3dList() {
		assertNotThrown(()->proxy.setBean1d3dList(alist(new ABean[][][]{{{ABean.get(),null},null},null},null)));
	}

	@Test
	public void h23_setBeanMap() {
		assertNotThrown(()->proxy.setBeanMap(map("foo",ABean.get())));
	}

	@Test
	public void h24_setBeanListMap() {
		assertNotThrown(()->proxy.setBeanListMap(map("foo",Arrays.asList(ABean.get()))));
	}

	@Test
	public void h25_setBean1d3dListMap() {
		assertNotThrown(()->proxy.setBean1d3dListMap(map("foo",alist(new ABean[][][]{{{ABean.get(),null},null},null},null))));
	}

	@Test
	public void h26_setBeanListMapIntegerKeys() {
		assertNotThrown(()->proxy.setBeanListMapIntegerKeys(map(1,Arrays.asList(ABean.get()))));
	}

	// Typed beans

	@Test
	public void i01_setTypedBean() {
		assertNotThrown(()->proxy.setTypedBean(TypedBeanImpl.get()));
	}

	@Test
	public void i02_setTypedBean3dArray() {
		assertNotThrown(()->proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null}));
	}

	@Test
	public void i03_setTypedBeanList() {
		assertNotThrown(()->proxy.setTypedBeanList(Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	@Test
	public void i04_setTypedBean1d3dList() {
		assertNotThrown(()->proxy.setTypedBean1d3dList(alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
	}

	@Test
	public void i05_setTypedBeanMap() {
		assertNotThrown(()->proxy.setTypedBeanMap(map("foo",TypedBeanImpl.get())));
	}

	@Test
	public void i06_setTypedBeanListMap() {
		assertNotThrown(()->proxy.setTypedBeanListMap(map("foo",Arrays.asList((TypedBean)TypedBeanImpl.get()))));
	}

	@Test
	public void i07_setTypedBean1d3dListMap() {
		assertNotThrown(()->proxy.setTypedBean1d3dListMap(map("foo",alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null))));
	}

	@Test
	public void i08_setTypedBeanListMapIntegerKeys() {
		assertNotThrown(()->proxy.setTypedBeanListMapIntegerKeys(map(1,Arrays.asList((TypedBean)TypedBeanImpl.get()))));
	}

	// Swapped POJOs

	@Test
	public void j01_setSwappedObject() {
		assertNotThrown(()->proxy.setSwappedObject(new SwappedObject()));
	}

	@Test
	public void j02_setSwappedObject3dArray() {
		assertNotThrown(()->proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
	}

	@Test
	public void j03_setSwappedObjectMap() {
		assertNotThrown(()->proxy.setSwappedObjectMap(map(new SwappedObject(),new SwappedObject())));
	}

	@Test
	public void j04_setSwappedObject3dMap() {
		assertNotThrown(()->proxy.setSwappedObject3dMap(map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})));
	}

	// Implicit swapped POJOs
	@Test
	public void k01_setImplicitSwappedObject() {
		assertNotThrown(()->proxy.setImplicitSwappedObject(new ImplicitSwappedObject()));
	}

	@Test
	public void k02_setImplicitSwappedObject3dArray() {
		assertNotThrown(()->proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
	}

	@Test
	public void k03_setImplicitSwappedObjectMap() {
		assertNotThrown(()->proxy.setImplicitSwappedObjectMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject())));
	}

	@Test
	public void k04_setImplicitSwappedObject3dMap() {
		assertNotThrown(()->proxy.setImplicitSwappedObject3dMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})));
	}

	// Enums

	@Test
	public void l01_setEnum() {
		assertNotThrown(()->proxy.setEnum(TestEnum.TWO));
	}

	@Test
	public void l02_setEnum3d() {
		assertNotThrown(()->proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void l03_setEnumList() {
		assertNotThrown(()->proxy.setEnumList(alist(TestEnum.TWO,null)));
	}

	@Test
	public void l04_setEnum3dList() {
		assertNotThrown(()->proxy.setEnum3dList(alist(alist(alist(TestEnum.TWO,null),null),null)));
	}

	@Test
	public void l05_setEnum1d3dList() {
		assertNotThrown(()->proxy.setEnum1d3dList(alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
	}

	@Test
	public void l06_setEnumMap() {
		assertNotThrown(()->proxy.setEnumMap(map(TestEnum.ONE,TestEnum.TWO)));
	}

	@Test
	public void l07_setEnum3dArrayMap() {
		assertNotThrown(()->proxy.setEnum3dArrayMap(map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null})));
	}

	@Test
	public void l08_setEnum1d3dListMap() {
		assertNotThrown(()->proxy.setEnum1d3dListMap(map(TestEnum.ONE,alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test multi-parameters
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void m01_setMultiParamsInts() {
		int x1 = 1;
		int[][][] x2 = {{{1,2},null},null};
		int[][][] x2n = null;
		List<int[][][]> x3 = alist(x2,null);
		List<int[][][]> x3n = null;
		assertNotThrown(()->proxy.setMultiParamsInts(x1,x2,x2n,x3,x3n));
	}

	@Test
	public void m02_setMultiParamsInteger() {
		Integer x1 = 1;
		Integer x1n = null;
		Integer[][][] x2 = {{{1,null},null},null};
		Integer[][][] x2n = null;
		List<Integer[][][]> x3 = alist(x2,null);
		List<Integer[][][]> x3n = null;
		assertNotThrown(()->proxy.setMultiParamsInteger(x1,x1n,x2,x2n,x3,x3n));
	}

	@Test
	public void m03_setMultiParamsFloat() {
		float x1 = 1;
		float[][][] x2 = {{{1,2},null},null};
		float[][][] x2n = null;
		List<float[][][]> x3 = alist(x2,null);
		List<float[][][]> x3n = null;
		assertNotThrown(()->proxy.setMultiParamsFloat(x1,x2,x2n,x3,x3n));
	}

	@Test
	public void m04_setMultiParamsFloatObject() {
		Float x1 = 1f;
		Float x1n = null;
		Float[][][] x2 = {{{1f,null},null},null};
		Float[][][] x2n = null;
		List<Float[][][]> x3 = alist(x2,null);
		List<Float[][][]> x3n = null;
		assertNotThrown(()->proxy.setMultiParamsFloatObject(x1,x1n,x2,x2n,x3,x3n));
	}

	@Test
	public void m05_setMultiParamsString() {
		String x1 = "foo";
		String[][][] x2 = {{{"foo",null},null},null};
		String[][][] x2n = null;
		List<String[][][]> x3 = alist(x2,null);
		List<String[][][]> x3n = null;
		assertNotThrown(()->proxy.setMultiParamsString(x1,x2,x2n,x3,x3n));
	}

	@Test
	public void m06_setMultiParamsBean() {
		ABean x1 = ABean.get();
		ABean[][][] x2 = {{{ABean.get(),null},null},null};
		ABean[][][] x2n = null;
		List<ABean[][][]> x3 = alist(x2,null);
		List<ABean[][][]> x3n = null;
		Map<String,ABean> x4 = map("foo",ABean.get());
		Map<String,ABean> x4n = null;
		Map<String,List<ABean[][][]>> x5 = map("foo",x3);
		Map<String,List<ABean[][][]>> x5n = null;
		assertNotThrown(()->proxy.setMultiParamsBean(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@Test
	public void m07_setMultiParamsSwappedObject() {
		SwappedObject x1 = new SwappedObject();
		SwappedObject[][][] x2 = {{{new SwappedObject(),null},null},null};
		SwappedObject[][][] x2n = null;
		List<SwappedObject[][][]> x3 = alist(x2,null);
		List<SwappedObject[][][]> x3n = null;
		Map<SwappedObject,SwappedObject> x4 = map(new SwappedObject(),new SwappedObject());
		Map<SwappedObject,SwappedObject> x4n = null;
		Map<SwappedObject,List<SwappedObject[][][]>> x5 = map(new SwappedObject(),x3);
		Map<SwappedObject,List<SwappedObject[][][]>> x5n = null;
		assertNotThrown(()->proxy.setMultiParamsSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@Test
	public void m08_setMultiParamsImplicitSwappedObject() {
		ImplicitSwappedObject x1 = new ImplicitSwappedObject();
		ImplicitSwappedObject[][][] x2 = {{{new ImplicitSwappedObject(),null},null},null};
		ImplicitSwappedObject[][][] x2n = null;
		List<ImplicitSwappedObject[][][]> x3 = alist(x2,null);
		List<ImplicitSwappedObject[][][]> x3n = null;
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x4 = map(new ImplicitSwappedObject(),new ImplicitSwappedObject());
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n = null;
		Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5 = map(new ImplicitSwappedObject(),x3);
		Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n = null;
		assertNotThrown(()->proxy.setMultiParamsImplicitSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@Test
	public void m09_setMultiParamsEnum() {
		TestEnum x1 = TestEnum.TWO;
		TestEnum[][][] x2 = {{{TestEnum.TWO,null},null},null};
		TestEnum[][][] x2n = null;
		List<TestEnum[][][]> x3 = alist(x2,null);
		List<TestEnum[][][]> x3n = null;
		Map<TestEnum,TestEnum> x4 = map(TestEnum.ONE,TestEnum.TWO);
		Map<TestEnum,TestEnum> x4n = null;
		Map<TestEnum,List<TestEnum[][][]>> x5 = map(TestEnum.ONE,x3);
		Map<TestEnum,List<TestEnum[][][]>> x5n = null;
		assertNotThrown(()->proxy.setMultiParamsEnum(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}
}