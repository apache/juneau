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
import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.http.HttpMethod.*;
import static org.apache.juneau.utest.utils.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
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
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.opentest4j.*;

class RrpcInterface_Test extends SimpleTestBase {

	private static final Input[] INPUT = {
		input("Json", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), JsonParser.DEFAULT),
		input("Xml", XmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT),
		input("Mixed", JsonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), XmlParser.DEFAULT),
		input("Html", HtmlSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), HtmlParser.DEFAULT),
		input("MessagePack", MsgPackSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), MsgPackParser.DEFAULT),
		input("UrlEncoding", UrlEncodingSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UrlEncodingParser.DEFAULT),
		input("Uon", UonSerializer.DEFAULT.copy().addBeanTypes().addRootType().build(), UonParser.DEFAULT)
	};

	private static Input input(String label, Serializer serializer, Parser parser) {
		return new Input(label, serializer, parser);
	}

	private static class Input {
		InterfaceProxy proxy;

		public Input(String label, Serializer serializer, Parser parser) {
			proxy = MockRestClient.create(InterfaceProxyResource.class).serializer(serializer).parser(parser).noTrace().build().getRrpcInterface(InterfaceProxy.class,"/proxy");
		}
	}

	static Input[] input() {
		return INPUT;
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
		void setInt(int x) throws AssertionFailedError;
		void setInteger(Integer x);
		void setBoolean(boolean x);
		void setFloat(float x);
		void setFloatObject(Float x);
		void setString(String x);
		void setNullString(String x) throws AssertionFailedError;
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
				public void setInt(int v) {
					assertEquals(1,v);
				}

				@Override
				public void setInteger(Integer v) {
					assertEquals((Integer)1,v);
				}

				@Override
				public void setBoolean(boolean v) {
					assertTrue(v);
				}

				@Override
				public void setFloat(float v) {
					assertEquals(1f, v, 0.1f);
				}

				@Override
				public void setFloatObject(Float v) {
					assertEquals(1f, v, 0.1f);
				}

				@Override
				public void setString(String v) {
					assertEquals("foo",v);
				}

				@Override
				public void setNullString(String v) {
					assertNull(v);
				}

				@Override
				public void setInt3dArray(int[][][] v) {
					assertArray(v, "[[1,2],null]", null);
				}

				@Override
				public void setInteger3dArray(Integer[][][] v) {
					assertArray(v, "[[1,null],null]", null);
				}

				@Override
				public void setString3dArray(String[][][] v) {
					assertArray(v, "[[foo,null],null]", null);
				}

				@Override
				public void setIntegerList(List<Integer> v) {
					assertList(v, "1", null);
					assertType(Integer.class, v.get(0));
				}

				@Override
				public void setInteger3dList(List<List<List<Integer>>> v) {
					assertList(v, "[[1,null],null]", null);
					assertType(Integer.class, v.get(0).get(0).get(0));
				}

				@Override
				public void setInteger1d3dList(List<Integer[][][]> v) {
					assertList(v, "[[[1,null],null],null]", null);
					assertType(Integer[][][].class, v.get(0));
					assertType(Integer.class, v.get(0)[0][0][0]);
				}

				@Override
				public void setInt1d3dList(List<int[][][]> v) {
					assertList(v, "[[[1,2],null],null]", null);
					assertType(int[][][].class, v.get(0));
				}

				@Override
				public void setStringList(List<String> v) {
					assertList(v, "foo", "bar", null);
				}

				// Beans

				@Override
				public void setBean(ABean v) {
					assertBean(v, "a,b", "1,foo");
				}

				@Override
				public void setBean3dArray(ABean[][][] v) {
					assertArray(v, "[[{a:1,b:'foo'},null],null]",null);
				}

				@Override
				public void setBeanList(List<ABean> v) {
					assertList(v, "{a:1,b:'foo'}");
				}

				@Override
				public void setBean1d3dList(List<ABean[][][]> v) {
					assertList(v, "[[[{a:1,b:'foo'},null],null],null]", null);
				}

				@Override
				public void setBeanMap(Map<String,ABean> v) {
					assertMap(v, "foo", "{a:1,b:'foo'}");
				}

				@Override
				public void setBeanListMap(Map<String,List<ABean>> v) {
					assertMap(v, "foo", "[{a:1,b:'foo'}]");
				}

				@Override
				public void setBean1d3dListMap(Map<String,List<ABean[][][]>> v) {
					assertMap(v, "foo", "[[[[{a:1,b:'foo'},null],null],null],null]");
				}

				@Override
				public void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> v) {
					assertMapPairs(v, "1=[{a:1,b:'foo'}]");
					assertType(Integer.class, v.keySet().iterator().next());
				}

				// Typed beans

				@Override
				public void setTypedBean(TypedBean v) {
					assertBean(v, "a,b", "1,foo");
					assertType(TypedBeanImpl.class, v);
				}

				@Override
				public void setTypedBean3dArray(TypedBean[][][] v) {
					assertArray(v, "[[a:1;b:foo,null],null]" ,null);  // Testing serialization here.
					assertType(TypedBeanImpl.class, v[0][0][0]);
				}

				@Override
				public void setTypedBeanList(List<TypedBean> v) {
					assertList(v, "a:1;b:foo");
					assertType(TypedBeanImpl.class, v.get(0));
				}

				@Override
				public void setTypedBean1d3dList(List<TypedBean[][][]> v) {
					assertList(v, "[[[a:1;b:foo,null],null],null]", null);
					assertType(TypedBeanImpl.class, v.get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanMap(Map<String,TypedBean> v) {
					assertMap(v, "foo", "a:1;b:foo");
					assertType(TypedBeanImpl.class, v.get("foo"));
				}

				@Override
				public void setTypedBeanListMap(Map<String,List<TypedBean>> v) {
					assertMap(v, "foo", "[a:1;b:foo]");
					assertType(TypedBeanImpl.class, v.get("foo").get(0));
				}

				@Override
				public void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> v) {
					assertMap(v, "foo", "[[[[a:1;b:foo,null],null],null],null]");
					assertType(TypedBeanImpl.class, v.get("foo").get(0)[0][0][0]);
				}

				@Override
				public void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> v) {
					assertMapPairs(v, "1=[a:1;b:foo]");
					assertType(TypedBeanImpl.class, v.get(1).get(0));
				}

				// Swapped POJOs

				@Override
				public void setSwappedObject(SwappedObject v) {
					assertTrue(v.wasUnswapped);
				}

				@Override
				public void setSwappedObject3dArray(SwappedObject[][][] v) {
					assertArray(v, "[[wasUnswapped:true,null],null]" ,null);
					assertTrue(v[0][0][0].wasUnswapped);
				}

				@Override
				public void setSwappedObjectMap(Map<SwappedObject,SwappedObject> v) {
					assertMapPairs(v, "wasUnswapped:true=wasUnswapped:true");
					var e = v.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setSwappedObject3dMap(Map<SwappedObject,SwappedObject[][][]> v) {
					assertMapPairs(v, "wasUnswapped:true=[[[wasUnswapped:true,null],null],null]");
					var e = v.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue()[0][0][0].wasUnswapped);
				}

				// Implicit swapped POJOs

				@Override
				public void setImplicitSwappedObject(ImplicitSwappedObject v) {
					assertTrue(v.wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObject3dArray(ImplicitSwappedObject[][][] v) {
					assertArray(v, "[["+SWAP+",null],null]", null);
					assertTrue(v[0][0][0].wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObjectMap(Map<ImplicitSwappedObject,ImplicitSwappedObject> v) {
					assertMapPairs(v, SWAP+"="+SWAP);
					var e = v.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue().wasUnswapped);
				}

				@Override
				public void setImplicitSwappedObject3dMap(Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> v) {
					assertMapPairs(v, SWAP+"=[[["+SWAP+",null],null],null]");
					var e = v.entrySet().iterator().next();
					assertTrue(e.getKey().wasUnswapped);
					assertTrue(e.getValue()[0][0][0].wasUnswapped);
				}

				// Enums

				@Override
				public void setEnum(TestEnum v) {
					assertEquals(TestEnum.TWO,v);
				}

				@Override
				public void setEnum3d(TestEnum[][][] v) {
					assertArray(v, "[[TWO,null],null]", null);
				}

				@Override
				public void setEnumList(List<TestEnum> v) {
					assertList(v, "TWO", null);
					assertType(TestEnum.class, v.get(0));
				}

				@Override
				public void setEnum3dList(List<List<List<TestEnum>>> v) {
					assertList(v, "[[TWO,null],null]", null);
					assertType(TestEnum.class, v.get(0).get(0).get(0));
				}

				@Override
				public void setEnum1d3dList(List<TestEnum[][][]> v) {
					assertList(v, "[[[TWO,null],null],null]", null);
					assertType(TestEnum[][][].class, v.get(0));
				}

				@Override
				public void setEnumMap(Map<TestEnum,TestEnum> v) {
					assertMapPairs(v, "ONE=TWO");
					var e = v.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum.class, e.getValue());
				}

				@Override
				public void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> v) {
					assertMapPairs(v, "ONE=[[[TWO,null],null],null]");
					var e = v.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum[][][].class, e.getValue());
				}

				@Override
				public void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> v) {
					assertMapPairs(v, "ONE=[[[[TWO,null],null],null],null]");
					Map.Entry<TestEnum,List<TestEnum[][][]>> e = v.entrySet().iterator().next();
					assertType(TestEnum.class, e.getKey());
					assertType(TestEnum[][][].class, e.getValue().get(0));
				}

				//--------------------------------------------------------------------------------
				// Test multi-parameters
				//--------------------------------------------------------------------------------

				@Override
				public void setMultiParamsInts(int x1,int[][][] x2,int[][][] x2n,List<int[][][]> x3,List<int[][][]> x3n) {
					assertEquals(1, x1);
					assertArray(x2, "[[1,2],null]" ,null);
					assertNull(x2n);
					assertList(x3, "[[[1,2],null],null]", null);
					assertType(int[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsInteger(Integer x1,Integer x1n,Integer[][][] x2,Integer[][][] x2n,List<Integer[][][]> x3,List<Integer[][][]> x3n) {
					assertEquals((Integer)1, x1);
					assertArray(x2, "[[1,null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[1,null],null],null]", null);
					assertType(Integer[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloat(float x1,float[][][] x2,float[][][] x2n,List<float[][][]> x3,List<float[][][]> x3n) {
					assertEquals(1.0f, x1, 0.1f);
					assertArray(x2, "[[1.0,2.0],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[1.0,2.0],null],null]", null);
					assertType(float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsFloatObject(Float x1,Float x1n,Float[][][] x2,Float[][][] x2n,List<Float[][][]> x3,List<Float[][][]> x3n) {
					assertEquals(1.0f, x1, 0.1f);
					assertArray(x2, "[[1.0,null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[1.0,null],null],null]", null);
					assertType(Float[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsString(String x1,String[][][] x2,String[][][] x2n,List<String[][][]> x3,List<String[][][]> x3n) {
					assertEquals("foo", x1);
					assertArray(x2, "[[foo,null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[foo,null],null],null]", null);
					assertType(String[][][].class, x3.get(0));
					assertNull(x3n);
				}

				@Override
				public void setMultiParamsBean(ABean x1,ABean[][][] x2,ABean[][][] x2n,List<ABean[][][]> x3,List<ABean[][][]> x3n,Map<String,ABean> x4,Map<String,ABean> x4n,Map<String,List<ABean[][][]>> x5,Map<String,List<ABean[][][]>> x5n) {
					assertBean(x1, "a,b", "1,foo");
					assertArray(x2, "[[{a:1,b:'foo'},null],null]", null);  // ABean toString converts it to JSON.
					assertNull(x2n);
					assertList(x3, "[[[{a:1,b:'foo'},null],null],null]", null);
					assertType(ABean[][][].class, x3.get(0));
					assertNull(x3n);
					assertMapPairs(x4, "foo={a:1,b:'foo'}");
					assertNull(x4n);
					assertMapPairs(x5, "foo=[[[[{a:1,b:'foo'},null],null],null],null]");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsSwappedObject(SwappedObject x1,SwappedObject[][][] x2,SwappedObject[][][] x2n,List<SwappedObject[][][]> x3,List<SwappedObject[][][]> x3n,Map<SwappedObject,SwappedObject> x4,Map<SwappedObject,SwappedObject> x4n,Map<SwappedObject,List<SwappedObject[][][]>> x5,Map<SwappedObject,List<SwappedObject[][][]>> x5n) {
					assertTrue(x1.wasUnswapped);
					assertArray(x2, "[[wasUnswapped:true,null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[wasUnswapped:true,null],null],null]", null);
					assertType(SwappedObject[][][].class, x3.get(0));
					assertNull(x3n);
					assertMapPairs(x4, "wasUnswapped:true=wasUnswapped:true");
					assertNull(x4n);
					assertMapPairs(x5, "wasUnswapped:true=[[[[wasUnswapped:true,null],null],null],null]");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsImplicitSwappedObject(ImplicitSwappedObject x1,ImplicitSwappedObject[][][] x2,ImplicitSwappedObject[][][] x2n,List<ImplicitSwappedObject[][][]> x3,List<ImplicitSwappedObject[][][]> x3n,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4,Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5,Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n) {
					assertTrue(x1.wasUnswapped);
					assertArray(x2, "[["+SWAP+",null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[["+SWAP+",null],null],null]", null);
					assertType(ImplicitSwappedObject[][][].class, x3.get(0));
					assertNull(x3n);
					assertMapPairs(x4, SWAP+"="+SWAP);
					assertNull(x4n);
					assertMapPairs(x5, SWAP+"=[[[["+SWAP+",null],null],null],null]");
					assertNull(x5n);
				}

				@Override
				public void setMultiParamsEnum(TestEnum x1,TestEnum[][][] x2,TestEnum[][][] x2n,List<TestEnum[][][]> x3,List<TestEnum[][][]> x3n,Map<TestEnum,TestEnum> x4,Map<TestEnum,TestEnum> x4n,Map<TestEnum,List<TestEnum[][][]>> x5,Map<TestEnum,List<TestEnum[][][]>> x5n) {
					assertEquals(TestEnum.TWO, x1);
					assertArray(x2, "[[TWO,null],null]", null);
					assertNull(x2n);
					assertList(x3, "[[[TWO,null],null],null]", null);
					assertType(TestEnum[][][].class, x3.get(0));
					assertNull(x3n);
					assertMapPairs(x4, "ONE=TWO");
					assertNull(x4n);
					assertMapPairs(x5, "ONE=[[[[TWO,null],null],null],null]");
					assertNull(x5n);
				}
			};
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test return types.
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives
	@ParameterizedTest
	@MethodSource("input")
	void a01_returnVoid(Input input) {
		assertDoesNotThrow(()->input.proxy.returnVoid());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a02_returnInteger(Input input) {
		assertEquals((Integer)1, input.proxy.returnInteger());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a03_returnInt(Input input) {
		assertEquals(1, input.proxy.returnInt());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a04_returnBoolean(Input input) {
		assertEquals(true, input.proxy.returnBoolean());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a05_returnFloat(Input input) {
		assertEquals(1f, input.proxy.returnFloat(), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a06_returnFloatObject(Input input) {
		assertEquals(1f, input.proxy.returnFloatObject(), 0.1f);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a07_returnString(Input input) {
		assertEquals("foobar",input.proxy.returnString());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a08_returnNullString(Input input) {
		assertNull(input.proxy.returnNullString());
	}

	@ParameterizedTest
	@MethodSource("input")
	void a09_returnInt3dArray(Input input) {
		assertArray(input.proxy.returnInt3dArray(), "[[1,2],null]", null);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a10_returnInteger3dArray(Input input) {
		assertArray(input.proxy.returnInteger3dArray(), "[[1,null],null]", null);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a11_returnString3dArray(Input input) {
		assertArray(input.proxy.returnString3dArray(), "[[foo,bar,null],null]", null);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a12_returnIntegerList(Input input) {
		var x = input.proxy.returnIntegerList();
		assertList(x, "1", null);
		assertType(Integer.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void a13_returnInteger3dList(Input input) {
		var x = input.proxy.returnInteger3dList();
		assertList(x, "[[1,null],null]", null);
		assertType(Integer.class, x.get(0).get(0).get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void a14_returnInteger1d3dList(Input input) {
		var x = input.proxy.returnInteger1d3dList();
		assertList(x, "[[[1,null],null],null]", null);
		assertType(Integer.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void a15_returnInt1d3dList(Input input) {
		var x = input.proxy.returnInt1d3dList();
		assertList(x, "[[[1,2],null],null]", null);
		assertType(int[][][].class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void a16_returnStringList(Input input) {
		assertList(input.proxy.returnStringList(), "foo", "bar", null);
	}

	// Beans

	@ParameterizedTest
	@MethodSource("input")
	void b01_returnBean(Input input) {
		var x = input.proxy.returnBean();
		assertBean(x, "a,b", "1,foo");
		assertType(ABean.class, x);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b02_returnBean3dArray(Input input) {
		var x = input.proxy.returnBean3dArray();
		assertArray(x, "[[{a:1,b:'foo'},null],null]", null);
		assertType(ABean.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b03_returnBeanList(Input input) {
		var x = input.proxy.returnBeanList();
		assertList(x, "{a:1,b:'foo'}");
		assertType(ABean.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void b04_returnBean1d3dList(Input input) {
		var x = input.proxy.returnBean1d3dList();
		assertList(x, "[[[{a:1,b:'foo'},null],null],null]", null);
		assertType(ABean.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b05_returnBeanMap(Input input) {
		var x = input.proxy.returnBeanMap();
		assertMap(x, "foo", "{a:1,b:'foo'}");
		assertType(ABean.class, x.get("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void b06_returnBeanListMap(Input input) {
		var x = input.proxy.returnBeanListMap();
		assertMap(x, "foo", "[{a:1,b:'foo'}]");
		assertType(ABean.class, x.get("foo").get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void b07_returnBean1d3dListMap(Input input) {
		var x = input.proxy.returnBean1d3dListMap();
		assertMap(x, "foo", "[[[[{a:1,b:'foo'},null],null],null],null]");
		assertType(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void b08_returnBeanListMapIntegerKeys(Input input) {
		// Note: JsonSerializer serializes key as string.
		var x = input.proxy.returnBeanListMapIntegerKeys();
		assertMapPairs(x, "1=[{a:1,b:'foo'}]");
		assertType(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@ParameterizedTest
	@MethodSource("input")
	void c01_returnTypedBean(Input input) {
		var x = input.proxy.returnTypedBean();
		assertBean(x, "a,b", "1,foo");
		assertType(TypedBeanImpl.class, x);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c02_returnTypedBean3dArray(Input input) {
		var x = input.proxy.returnTypedBean3dArray();
		assertArray(x, "[[a:1;b:foo,null],null]", null);
		assertType(TypedBeanImpl.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c03_returnTypedBeanList(Input input) {
		var x = input.proxy.returnTypedBeanList();
		assertList(x, "a:1;b:foo");
		assertType(TypedBeanImpl.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void c04_returnTypedBean1d3dList(Input input) {
		var x = input.proxy.returnTypedBean1d3dList();
		assertList(x, "[[[a:1;b:foo,null],null],null]", null);
		assertType(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c05_returnTypedBeanMap(Input input) {
		var x = input.proxy.returnTypedBeanMap();
		assertMap(x, "foo", "a:1;b:foo");
		assertType(TypedBeanImpl.class, x.get("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void c06_returnTypedBeanListMap(Input input) {
		var x = input.proxy.returnTypedBeanListMap();
		assertMap(x, "foo", "[a:1;b:foo]");
		assertType(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void c07_returnTypedBean1d3dListMap(Input input) {
		var x = input.proxy.returnTypedBean1d3dListMap();
		assertMap(x, "foo", "[[[[a:1;b:foo,null],null],null],null]");
		assertType(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void c08_returnTypedBeanListMapIntegerKeys(Input input) {
		// Note: JsonSerializer serializes key as string.
		var x = input.proxy.returnTypedBeanListMapIntegerKeys();
		assertMapPairs(x, "1=[a:1;b:foo]");
		assertType(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void d01_returnSwappedObject(Input input) {
		var x = input.proxy.returnSwappedObject();
		assertTrue(x.wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void d02_returnSwappedObject3dArray(Input input) {
		var x = input.proxy.returnSwappedObject3dArray();
		assertArray(x, "[[wasUnswapped:true,null],null]", null);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void d03_returnSwappedObjectMap(Input input) {
		var x = input.proxy.returnSwappedObjectMap();
		assertMapPairs(x, "wasUnswapped:true=wasUnswapped:true");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void d04_returnSwappedObject3dMap(Input input) {
		var x = input.proxy.returnSwappedObject3dMap();
		assertMapPairs(x, "wasUnswapped:true=[[[wasUnswapped:true,null],null],null]");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void e01_returnImplicitSwappedObject(Input input) {
		var x = input.proxy.returnImplicitSwappedObject();
		assertTrue(x.wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void e02_returnImplicitSwappedObject3dArray(Input input) {
		var x = input.proxy.returnImplicitSwappedObject3dArray();
		assertArray(x, "[["+SWAP+",null],null]", null);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void e03_returnImplicitSwappedObjectMap(Input input) {
		var x = input.proxy.returnImplicitSwappedObjectMap();
		assertMapPairs(x, SWAP+"="+SWAP);
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@ParameterizedTest
	@MethodSource("input")
	void e04_returnImplicitSwappedObject3dMap(Input input) {
		var x = input.proxy.returnImplicitSwappedObject3dMap();
		assertMapPairs(x, SWAP+"=[[["+SWAP+",null],null],null]");
		var e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@ParameterizedTest
	@MethodSource("input")
	void f01_returnEnum(Input input) {
		var x = input.proxy.returnEnum();
		assertEquals(TestEnum.TWO, x);
	}

	@ParameterizedTest
	@MethodSource("input")
	void f02_returnEnum3d(Input input) {
		var x = input.proxy.returnEnum3d();
		assertArray(x, "[[TWO,null],null]", null);
		assertType(TestEnum.class, x[0][0][0]);
	}

	@ParameterizedTest
	@MethodSource("input")
	void f03_returnEnumList(Input input) {
		var x = input.proxy.returnEnumList();
		assertList(x, "TWO", null);
		assertType(TestEnum.class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void f04_returnEnum3dList(Input input) {
		var x = input.proxy.returnEnum3dList();
		assertList(x, "[[TWO,null],null]", null);
		assertType(TestEnum.class, x.get(0).get(0).get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void f05_returnEnum1d3dList(Input input) {
		var x = input.proxy.returnEnum1d3dList();
		assertList(x, "[[[TWO,null],null],null]", null);
		assertType(TestEnum[][][].class, x.get(0));
	}

	@ParameterizedTest
	@MethodSource("input")
	void f06_returnEnumMap(Input input) {
		var x = input.proxy.returnEnumMap();
		assertMapPairs(x, "ONE=TWO");
		var e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum.class, e.getValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void f07_returnEnum3dArrayMap(Input input) {
		var x = input.proxy.returnEnum3dArrayMap();
		assertMapPairs(x, "ONE=[[[TWO,null],null],null]");
		var e = x.entrySet().iterator().next();
		assertType(TestEnum.class, e.getKey());
		assertType(TestEnum[][][].class, e.getValue());
	}

	@ParameterizedTest
	@MethodSource("input")
	void f08_returnEnum1d3dListMap(Input input) {
		var x = input.proxy.returnEnum1d3dListMap();
		assertMapPairs(x, "ONE=[[[[TWO,null],null],null],null]");
		assertType(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void g01_throwException1(Input input) {
		try {
			input.proxy.throwException1();
			fail();
		} catch (InterfaceProxy.InterfaceProxyException1 e) {
			assertEquals("foo",e.getMessage());
		}
	}

	@SuppressWarnings("unused")
	@ParameterizedTest
	@MethodSource("input")
	void g02_throwException2(Input input) {
		try {
			input.proxy.throwException2();
			fail();
		} catch (InterfaceProxy.InterfaceProxyException2 e) {/*no-op*/}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test parameters
	//-----------------------------------------------------------------------------------------------------------------

	// Various primitives
	@ParameterizedTest
	@MethodSource("input")
	void h01_setNothing(Input input) {
		assertDoesNotThrow(()->input.proxy.setNothing());
	}

	@ParameterizedTest
	@MethodSource("input")
	void h02_setInt(Input input) {
		assertDoesNotThrow(()->input.proxy.setInt(1));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h03_setWrongInt(Input input) {
		assertThrows(AssertionError.class, ()->input.proxy.setInt(2), "expected: <1> but was: <2>");
	}

	@ParameterizedTest
	@MethodSource("input")
	void h04_setInteger(Input input) {
		assertDoesNotThrow(()->input.proxy.setInteger(1));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h05_setBoolean(Input input) {
		assertDoesNotThrow(()->input.proxy.setBoolean(true));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h06_setFloat(Input input) {
		assertDoesNotThrow(()->input.proxy.setFloat(1f));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h07_setFloatObject(Input input) {
		assertDoesNotThrow(()->input.proxy.setFloatObject(1f));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h08_setString(Input input) {
		assertDoesNotThrow(()->input.proxy.setString("foo"));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h09_setNullString(Input input) {
		assertDoesNotThrow(()->input.proxy.setNullString(null));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h10_setNullStringBad(Input input) {
		assertThrows(AssertionError.class, ()->input.proxy.setNullString("foo"), "expected: <null> but was: <foo>");
	}

	@ParameterizedTest
	@MethodSource("input")
	void h11_setInt3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setInt3dArray(new int[][][]{{{1,2},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h12_setInteger3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h13_setString3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setString3dArray(new String[][][]{{{"foo",null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h14_setIntegerList(Input input) {
		assertDoesNotThrow(()->input.proxy.setIntegerList(alist(1,null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h15_setInteger3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setInteger3dList(alist(alist(alist(1,null),null),null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h16_setInteger1d3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setInteger1d3dList(alist(new Integer[][][]{{{1,null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h17_setInt1d3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setInt1d3dList(alist(new int[][][]{{{1,2},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h18_setStringList(Input input) {
		assertDoesNotThrow(()->input.proxy.setStringList(Arrays.asList("foo","bar",null)));
	}

	// Beans
	@ParameterizedTest
	@MethodSource("input")
	void h19_setBean(Input input) {
		assertDoesNotThrow(()->input.proxy.setBean(ABean.get()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h20_setBean3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h21_setBeanList(Input input) {
		assertDoesNotThrow(()->input.proxy.setBeanList(Arrays.asList(ABean.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h22_setBean1d3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setBean1d3dList(alist(new ABean[][][]{{{ABean.get(),null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h23_setBeanMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setBeanMap(map("foo",ABean.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h24_setBeanListMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setBeanListMap(map("foo",Arrays.asList(ABean.get()))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h25_setBean1d3dListMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setBean1d3dListMap(map("foo",alist(new ABean[][][]{{{ABean.get(),null},null},null},null))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void h26_setBeanListMapIntegerKeys(Input input) {
		assertDoesNotThrow(()->input.proxy.setBeanListMapIntegerKeys(map(1,Arrays.asList(ABean.get()))));
	}

	// Typed beans

	@ParameterizedTest
	@MethodSource("input")
	void i01_setTypedBean(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBean(TypedBeanImpl.get()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i02_setTypedBean3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i03_setTypedBeanList(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBeanList(Arrays.asList((TypedBean)TypedBeanImpl.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i04_setTypedBean1d3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBean1d3dList(alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i05_setTypedBeanMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBeanMap(map("foo",TypedBeanImpl.get())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i06_setTypedBeanListMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBeanListMap(map("foo",Arrays.asList((TypedBean)TypedBeanImpl.get()))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i07_setTypedBean1d3dListMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBean1d3dListMap(map("foo",alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null))));
	}

	@ParameterizedTest
	@MethodSource("input")
	void i08_setTypedBeanListMapIntegerKeys(Input input) {
		assertDoesNotThrow(()->input.proxy.setTypedBeanListMapIntegerKeys(map(1,Arrays.asList((TypedBean)TypedBeanImpl.get()))));
	}

	// Swapped POJOs

	@ParameterizedTest
	@MethodSource("input")
	void j01_setSwappedObject(Input input) {
		assertDoesNotThrow(()->input.proxy.setSwappedObject(new SwappedObject()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void j02_setSwappedObject3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void j03_setSwappedObjectMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setSwappedObjectMap(map(new SwappedObject(),new SwappedObject())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void j04_setSwappedObject3dMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setSwappedObject3dMap(map(new SwappedObject(),new SwappedObject[][][]{{{new SwappedObject(),null},null},null})));
	}

	// Implicit swapped POJOs
	@ParameterizedTest
	@MethodSource("input")
	void k01_setImplicitSwappedObject(Input input) {
		assertDoesNotThrow(()->input.proxy.setImplicitSwappedObject(new ImplicitSwappedObject()));
	}

	@ParameterizedTest
	@MethodSource("input")
	void k02_setImplicitSwappedObject3dArray(Input input) {
		assertDoesNotThrow(()->input.proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void k03_setImplicitSwappedObjectMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setImplicitSwappedObjectMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject())));
	}

	@ParameterizedTest
	@MethodSource("input")
	void k04_setImplicitSwappedObject3dMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setImplicitSwappedObject3dMap(map(new ImplicitSwappedObject(),new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null})));
	}

	// Enums

	@ParameterizedTest
	@MethodSource("input")
	void l01_setEnum(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum(TestEnum.TWO));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l02_setEnum3d(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l03_setEnumList(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnumList(alist(TestEnum.TWO,null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l04_setEnum3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum3dList(alist(alist(alist(TestEnum.TWO,null),null),null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l05_setEnum1d3dList(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum1d3dList(alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l06_setEnumMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnumMap(map(TestEnum.ONE,TestEnum.TWO)));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l07_setEnum3dArrayMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum3dArrayMap(map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null})));
	}

	@ParameterizedTest
	@MethodSource("input")
	void l08_setEnum1d3dListMap(Input input) {
		assertDoesNotThrow(()->input.proxy.setEnum1d3dListMap(map(TestEnum.ONE,alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null))));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test multi-parameters
	//-----------------------------------------------------------------------------------------------------------------

	@ParameterizedTest
	@MethodSource("input")
	void m01_setMultiParamsInts(Input input) {
		var x1 = 1;
		var x2 = new int[][][]{{{1,2},null},null};
		int[][][] x2n = null;
		var x3 = alist(x2,null);
		List<int[][][]> x3n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsInts(x1,x2,x2n,x3,x3n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m02_setMultiParamsInteger(Input input) {
		var x1 = Integer.valueOf(1);
		Integer x1n = null;
		var x2 = new Integer[][][]{{{1,null},null},null};
		Integer[][][] x2n = null;
		var x3 = alist(x2,null);
		List<Integer[][][]> x3n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsInteger(x1,x1n,x2,x2n,x3,x3n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m03_setMultiParamsFloat(Input input) {
		var x1 = 1f;
		var x2 = new float[][][]{{{1,2},null},null};
		float[][][] x2n = null;
		var x3 = alist(x2,null);
		List<float[][][]> x3n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsFloat(x1,x2,x2n,x3,x3n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m04_setMultiParamsFloatObject(Input input) {
		var x1 = 1f;
		Float x1n = null;
		var x2 = new Float[][][]{{{1f,null},null},null};
		Float[][][] x2n = null;
		var x3 = alist(x2,null);
		List<Float[][][]> x3n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsFloatObject(x1,x1n,x2,x2n,x3,x3n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m05_setMultiParamsString(Input input) {
		var x1 = "foo";
		var x2 = new String[][][]{{{"foo",null},null},null};
		String[][][] x2n = null;
		var x3 = alist(x2,null);
		List<String[][][]> x3n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsString(x1,x2,x2n,x3,x3n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m06_setMultiParamsBean(Input input) {
		var x1 = ABean.get();
		var x2 = new ABean[][][]{{{ABean.get(),null},null},null};
		ABean[][][] x2n = null;
		var x3 = alist(x2,null);
		List<ABean[][][]> x3n = null;
		var x4 = CollectionUtils.map("foo",ABean.get());
		Map<String,ABean> x4n = null;
		var x5 = CollectionUtils.map("foo",x3);
		Map<String,List<ABean[][][]>> x5n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsBean(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m07_setMultiParamsSwappedObject(Input input) {
		var x1 = new SwappedObject();
		var x2 = new SwappedObject[][][]{{{new SwappedObject(),null},null},null};
		SwappedObject[][][] x2n = null;
		var x3 = alist(x2,null);
		List<SwappedObject[][][]> x3n = null;
		var x4 = CollectionUtils.map(new SwappedObject(),new SwappedObject());
		Map<SwappedObject,SwappedObject> x4n = null;
		var x5 = CollectionUtils.map(new SwappedObject(),x3);
		Map<SwappedObject,List<SwappedObject[][][]>> x5n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m08_setMultiParamsImplicitSwappedObject(Input input) {
		var x1 = new ImplicitSwappedObject();
		var x2 = new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null};
		ImplicitSwappedObject[][][] x2n = null;
		var x3 = alist(x2,null);
		List<ImplicitSwappedObject[][][]> x3n = null;
		var x4 = CollectionUtils.map(new ImplicitSwappedObject(),new ImplicitSwappedObject());
		Map<ImplicitSwappedObject,ImplicitSwappedObject> x4n = null;
		var x5 = CollectionUtils.map(new ImplicitSwappedObject(),x3);
		Map<ImplicitSwappedObject,List<ImplicitSwappedObject[][][]>> x5n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsImplicitSwappedObject(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}

	@ParameterizedTest
	@MethodSource("input")
	void m09_setMultiParamsEnum(Input input) {
		var x1 = TestEnum.TWO;
		var x2 = new TestEnum[][][]{{{TestEnum.TWO,null},null},null};
		TestEnum[][][] x2n = null;
		var x3 = alist(x2,null);
		List<TestEnum[][][]> x3n = null;
		var x4 = CollectionUtils.map(TestEnum.ONE,TestEnum.TWO);
		Map<TestEnum,TestEnum> x4n = null;
		var x5 = CollectionUtils.map(TestEnum.ONE,x3);
		Map<TestEnum,List<TestEnum[][][]>> x5n = null;
		assertDoesNotThrow(()->input.proxy.setMultiParamsEnum(x1,x2,x2n,x3,x3n,x4,x4n,x5,x5n));
	}
}