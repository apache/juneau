/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.config;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.api.*;

class ConfigInterfaceTest extends TestBase {

	Config cf;
	ConfigInterface proxy;

	public ConfigInterfaceTest() {
		cf = Config.create().serializer(Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build()).build();
		proxy = cf.getSection("A").asInterface(ConfigInterface.class).orElse(null);
	}

	//====================================================================================================
	// getSectionAsInterface(String,Class)
	//====================================================================================================

	@Test void a01_string() {
		proxy.setString("foo");
		assertEquals("foo", proxy.getString());
		assertEquals("foo", cf.get("A/string").get());
	}

	@Test void a02_int() {
		proxy.setInt(1);
		assertEquals(1, proxy.getInt());
		assertEquals("1", cf.get("A/int").get());
	}

	@Test void a03_integer() {
		proxy.setInteger(2);
		assertEquals(2, proxy.getInteger().intValue());
		assertEquals("2", cf.get("A/integer").get());
		assertInstanceOf(Integer.class, proxy.getInteger());
	}

	@Test void a04_boolean() {
		proxy.setBoolean(true);
		assertEquals(true, proxy.isBoolean());
		assertEquals("true", cf.get("A/boolean").get());
	}

	@Test void a05_booleanObject() {
		proxy.setBooleanObject(true);
		assertEquals(true, proxy.getBooleanObject());
		assertEquals("true", cf.get("A/booleanObject").get());
		assertInstanceOf(Boolean.class, proxy.getBooleanObject());
	}

	@Test void a06_float() {
		proxy.setFloat(1f);
		assertEquals(1f, proxy.getFloat(), 0.1f);
		assertEquals("1.0", cf.get("A/float").get());
	}

	@Test void a07_floatObject() {
		proxy.setFloatObject(1f);
		assertEquals(1f, proxy.getFloatObject().floatValue(), 0.1f);
		assertEquals("1.0", cf.get("A/floatObject").get());
		assertInstanceOf(Float.class, proxy.getFloatObject());
	}

	@Test void a08_int3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1,2},null},null});
		assertEquals("[[[1,2],null],null]", cf.get("A/int3dArray").get());
		assertJson("[[[1,2],null],null]", proxy.getInt3dArray());
		assertInstanceOf(int[][][].class, proxy.getInt3dArray());
	}

	@Test void a09_integer3dArray() {
		proxy.setInteger3dArray(a(a(a(1,null),null),null));
		assertJson("[[[1,null],null],null]", proxy.getInteger3dArray());
		assertEquals("[[[1,null],null],null]", cf.get("A/integer3dArray").get());
		assertInstanceOf(Integer.class, proxy.getInteger3dArray()[0][0][0]);
	}

	@Test void a10_string3dArray() {
		proxy.setString3dArray(a(a(a("foo",null),null),null));
		assertJson("[[['foo',null],null],null]", proxy.getString3dArray());
		assertEquals("[[['foo',null],null],null]", cf.get("A/string3dArray").get());
	}

	@Test void a11_integerList() {
		proxy.setIntegerList(l(1,null));
		assertJson("[1,null]", proxy.getIntegerList());
		assertEquals("[1,null]", cf.get("A/integerList").get());
		assertInstanceOf(Integer.class, proxy.getIntegerList().get(0));
	}

	@Test void a12_integer3dList() {
		proxy.setInteger3dList(l(l(l(1,null),null),null));
		assertJson("[[[1,null],null],null]", proxy.getInteger3dList());
		assertEquals("[[[1,null],null],null]", cf.get("A/integer3dList").get());
		assertInstanceOf(Integer.class, proxy.getInteger3dList().get(0).get(0).get(0));
	}

	@Test void a13_integer1d3dList() {
		proxy.setInteger1d3dList(l(a(a(a(1,null),null),null),null));
		assertJson("[[[[1,null],null],null],null]", proxy.getInteger1d3dList());
		assertEquals("[[[[1,null],null],null],null]", cf.get("A/integer1d3dList").get());
		assertInstanceOf(Integer.class, proxy.getInteger1d3dList().get(0)[0][0][0]);
	}

	@Test void a14_int1d3dList() {
		proxy.setInt1d3dList(l(new int[][][]{{{1,2},null},null},null));
		assertJson("[[[[1,2],null],null],null]", proxy.getInt1d3dList());
		assertEquals("[[[[1,2],null],null],null]", cf.get("A/int1d3dList").get());
		assertInstanceOf(int[][][].class, proxy.getInt1d3dList().get(0));
	}

	@Test void a15_stringList() {
		proxy.setStringList(l("foo","bar",null));
		assertJson("['foo','bar',null]", proxy.getStringList());
		assertEquals("['foo','bar',null]", cf.get("A/stringList").get());
	}

	// Beans

	@Test void a16_bean() {
		proxy.setBean(ABean.get());
		assertJson("{a:1,b:'foo'}", proxy.getBean());
		assertEquals("{a:1,b:'foo'}", cf.get("A/bean").get());
		assertInstanceOf(ABean.class, proxy.getBean());
	}

	@Test void a17_bean3dArray() {
		proxy.setBean3dArray(a(a(a(ABean.get(),null),null),null));
		assertJson("[[[{a:1,b:'foo'},null],null],null]", proxy.getBean3dArray());
		assertEquals("[[[{a:1,b:'foo'},null],null],null]", cf.get("A/bean3dArray").get());
		assertInstanceOf(ABean.class, proxy.getBean3dArray()[0][0][0]);
	}

	@Test void a18_beanList() {
		proxy.setBeanList(l(ABean.get()));
		assertJson("[{a:1,b:'foo'}]", proxy.getBeanList());
		assertEquals("[{a:1,b:'foo'}]", cf.get("A/beanList").get());
		assertInstanceOf(ABean.class, proxy.getBeanList().get(0));
	}

	@Test void a19_bean1d3dList() {
		proxy.setBean1d3dList(l(a(a(a(ABean.get(),null),null),null),null));
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", proxy.getBean1d3dList());
		assertEquals("[[[[{a:1,b:'foo'},null],null],null],null]", cf.get("A/bean1d3dList").get());
		assertInstanceOf(ABean.class, proxy.getBean1d3dList().get(0)[0][0][0]);
	}

	@Test void a20_beanMap() {
		proxy.setBeanMap(map("foo",ABean.get()));
		assertJson("{foo:{a:1,b:'foo'}}", proxy.getBeanMap());
		assertEquals("{foo:{a:1,b:'foo'}}", cf.get("A/beanMap").get());
		assertInstanceOf(ABean.class, proxy.getBeanMap().get("foo"));
	}

	@Test void a21_beanListMap() {
		proxy.setBeanListMap(map("foo",l(ABean.get())));
		assertJson("{foo:[{a:1,b:'foo'}]}", proxy.getBeanListMap());
		assertEquals("{foo:[{a:1,b:'foo'}]}", cf.get("A/beanListMap").get());
		assertInstanceOf(ABean.class, proxy.getBeanListMap().get("foo").get(0));
	}

	@Test void a22_bean1d3dListMap() {
		proxy.setBean1d3dListMap(map("foo",l(a(a(a(ABean.get(),null),null),null),null)));
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", proxy.getBean1d3dListMap());
		assertEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", cf.get("A/bean1d3dListMap").get());
		assertInstanceOf(ABean.class, proxy.getBean1d3dListMap().get("foo").get(0)[0][0][0]);
	}

	@Test void a23_beanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(map(1,l(ABean.get())));
		assertJson("{'1':[{a:1,b:'foo'}]}", proxy.getBeanListMapIntegerKeys());
		assertEquals("{'1':[{a:1,b:'foo'}]}", cf.get("A/beanListMapIntegerKeys").get());
		assertInstanceOf(ABean.class, proxy.getBeanListMapIntegerKeys().get(1).get(0));
	}

	// Typed beans

	@Test void a24_typedBean() {
		proxy.setTypedBean(TypedBeanImpl.get());
		assertJson("{a:1,b:'foo'}", proxy.getTypedBean());
		assertEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", cf.get("A/typedBean").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBean());
	}

	@Test void a25_typedBean3dArray() {
		proxy.setTypedBean3dArray(a(a(a(TypedBeanImpl.get(),null),null),null));
		assertJson("[[[{a:1,b:'foo'},null],null],null]", proxy.getTypedBean3dArray());
		assertEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", cf.get("A/typedBean3dArray").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBean3dArray()[0][0][0]);
	}

	@Test void a26_typedBeanList() {
		proxy.setTypedBeanList(l((TypedBean)TypedBeanImpl.get()));
		assertJson("[{a:1,b:'foo'}]", proxy.getTypedBeanList());
		assertEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", cf.get("A/typedBeanList").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBeanList().get(0));
	}

	@Test void a27_typedBean1d3dList() {
		proxy.setTypedBean1d3dList(l(a(a(a(TypedBeanImpl.get(),null),null),null),null));
		assertJson("[[[[{a:1,b:'foo'},null],null],null],null]", proxy.getTypedBean1d3dList());
		assertEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", cf.get("A/typedBean1d3dList").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBean1d3dList().get(0)[0][0][0]);
	}

	@Test void a28_typedBeanMap() {
		proxy.setTypedBeanMap(map("foo",TypedBeanImpl.get()));
		assertJson("{foo:{a:1,b:'foo'}}", proxy.getTypedBeanMap());
		assertEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", cf.get("A/typedBeanMap").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBeanMap().get("foo"));
	}

	@Test void a29_typedBeanListMap() {
		proxy.setTypedBeanListMap(map("foo",l((TypedBean)TypedBeanImpl.get())));
		assertJson("{foo:[{a:1,b:'foo'}]}", proxy.getTypedBeanListMap());
		assertEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", cf.get("A/typedBeanListMap").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBeanListMap().get("foo").get(0));
	}

	@Test void a30_typedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(map("foo",l(a(a(a(TypedBeanImpl.get(),null),null),null),null)));
		assertJson("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", proxy.getTypedBean1d3dListMap());
		assertEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", cf.get("A/typedBean1d3dListMap").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBean1d3dListMap().get("foo").get(0)[0][0][0]);
	}

	@Test void a31_typedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(map(1,l((TypedBean)TypedBeanImpl.get())));
		assertJson("{'1':[{a:1,b:'foo'}]}", proxy.getTypedBeanListMapIntegerKeys());
		assertEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", cf.get("A/typedBeanListMapIntegerKeys").get());
		assertInstanceOf(TypedBeanImpl.class, proxy.getTypedBeanListMapIntegerKeys().get(1).get(0));
	}

	// Swapped POJOs

	@Test void a32_swappedObject() {
		proxy.setSwappedObject(new SwappedObject());
		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", proxy.getSwappedObject());
		assertEquals("swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/", cf.get("A/swappedObject").get());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObject());
	}

	@Test void a33_swappedObject3dArray() {
		proxy.setSwappedObject3dArray(a(a(a(new SwappedObject(),null),null),null));
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", proxy.getSwappedObject3dArray());
		assertEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", cf.get("A/swappedObject3dArray").get());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObject3dArray()[0][0][0]);
	}

	@Test void a34_swappedObjectMap() {
		proxy.setSwappedObjectMap(map(new SwappedObject(), new SwappedObject()));
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", proxy.getSwappedObjectMap());
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", cf.get("A/swappedObjectMap").get());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObjectMap().keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObjectMap().values().iterator().next());
	}

	@Test void a35_swappedObject3dMap() {
		proxy.setSwappedObject3dMap(map(new SwappedObject(), a(a(a(new SwappedObject(),null),null),null)));
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", proxy.getSwappedObject3dMap());
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", cf.get("A/swappedObject3dMap").get());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObject3dMap().keySet().iterator().next());
		assertInstanceOf(SwappedObject.class, proxy.getSwappedObject3dMap().values().iterator().next()[0][0][0]);
	}

	// Implicit swapped POJOs

	@Test void a36_implicitSwappedObject() {
		proxy.setImplicitSwappedObject(new ImplicitSwappedObject());
		assertJson("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'", proxy.getImplicitSwappedObject());
		assertEquals("swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/", cf.get("A/implicitSwappedObject").get());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObject());
	}

	@Test void a37_implicitSwappedObject3dArray() {
		proxy.setImplicitSwappedObject3dArray(a(a(a(new ImplicitSwappedObject(),null),null),null));
		assertJson("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", proxy.getImplicitSwappedObject3dArray());
		assertEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", cf.get("A/implicitSwappedObject3dArray").get());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObject3dArray()[0][0][0]);
	}

	@Test void a38_implicitSwappedObjectMap() {
		proxy.setImplicitSwappedObjectMap(map(new ImplicitSwappedObject(), new ImplicitSwappedObject()));
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", proxy.getImplicitSwappedObjectMap());
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", cf.get("A/implicitSwappedObjectMap").get());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObjectMap().keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObjectMap().values().iterator().next());
	}

	@Test void a39_implicitSwappedObject3dMap() {
		proxy.setImplicitSwappedObject3dMap(map(new ImplicitSwappedObject(), a(a(a(new ImplicitSwappedObject(),null),null),null)));
		assertJson("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", proxy.getImplicitSwappedObject3dMap());
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", cf.get("A/implicitSwappedObject3dMap").get());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObject3dMap().keySet().iterator().next());
		assertInstanceOf(ImplicitSwappedObject.class, proxy.getImplicitSwappedObject3dMap().values().iterator().next()[0][0][0]);
	}

	// Enums

	@Test void a40_enum() {
		proxy.setEnum(TestEnum.TWO);
		assertJson("'TWO'", proxy.getEnum());
		assertEquals("TWO", cf.get("A/enum").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum());
	}

	@Test void a41_enum3d() {
		proxy.setEnum3d(a(a(a(TestEnum.TWO,null),null),null));
		assertJson("[[['TWO',null],null],null]", proxy.getEnum3d());
		assertEquals("[[['TWO',null],null],null]", cf.get("A/enum3d").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum3d()[0][0][0]);
	}

	@Test void a42_enumList() {
		proxy.setEnumList(l(TestEnum.TWO,null));
		assertJson("['TWO',null]", proxy.getEnumList());
		assertEquals("['TWO',null]", cf.get("A/enumList").get());
		assertInstanceOf(TestEnum.class, proxy.getEnumList().get(0));
	}

	@Test void a43_enum3dList() {
		proxy.setEnum3dList(l(l(l(TestEnum.TWO,null),null),null));
		assertJson("[[['TWO',null],null],null]", proxy.getEnum3dList());
		assertEquals("[[['TWO',null],null],null]", cf.get("A/enum3dList").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum3dList().get(0).get(0).get(0));
	}

	@Test void a44_enum1d3dList() {
		proxy.setEnum1d3dList(l(a(a(a(TestEnum.TWO,null),null),null),null));
		assertJson("[[[['TWO',null],null],null],null]", proxy.getEnum1d3dList());
		assertEquals("[[[['TWO',null],null],null],null]", cf.get("A/enum1d3dList").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum1d3dList().get(0)[0][0][0]);
	}

	@Test void a45_enumMap() {
		proxy.setEnumMap(map(TestEnum.ONE,TestEnum.TWO));
		assertJson("{ONE:'TWO'}", proxy.getEnumMap());
		assertEquals("{ONE:'TWO'}", cf.get("A/enumMap").get());
		assertInstanceOf(TestEnum.class, proxy.getEnumMap().keySet().iterator().next());
		assertInstanceOf(TestEnum.class, proxy.getEnumMap().values().iterator().next());
	}

	@Test void a46_enum3dArrayMap() {
		proxy.setEnum3dArrayMap(map(TestEnum.ONE,a(a(a(TestEnum.TWO,null),null),null)));
		assertJson("{ONE:[[['TWO',null],null],null]}", proxy.getEnum3dArrayMap());
		assertEquals("{ONE:[[['TWO',null],null],null]}", cf.get("A/enum3dArrayMap").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum3dArrayMap().keySet().iterator().next());
		assertInstanceOf(TestEnum.class, proxy.getEnum3dArrayMap().values().iterator().next()[0][0][0]);
	}

	@Test void a47_enum1d3dListMap() {
		proxy.setEnum1d3dListMap(map(TestEnum.ONE,l(a(a(a(TestEnum.TWO,null),null),null),null)));
		assertJson("{ONE:[[[['TWO',null],null],null],null]}", proxy.getEnum1d3dListMap());
		assertEquals("{ONE:[[[['TWO',null],null],null],null]}", cf.get("A/enum1d3dListMap").get());
		assertInstanceOf(TestEnum.class, proxy.getEnum1d3dListMap().keySet().iterator().next());
		assertInstanceOf(TestEnum.class, proxy.getEnum1d3dListMap().values().iterator().next().get(0)[0][0][0]);
	}

	public interface ConfigInterface {

		// Various primitives

		String getString();
		void setString(String x);

		int getInt();
		void setInt(int x);

		Integer getInteger();
		void setInteger(Integer x);

		boolean isBoolean();
		void setBoolean(boolean x);

		Boolean getBooleanObject();
		void setBooleanObject(Boolean x);

		float getFloat();
		void setFloat(float x);

		Float getFloatObject();
		void setFloatObject(Float x);

		int[][][] getInt3dArray();
		void setInt3dArray(int[][][] x);

		Integer[][][] getInteger3dArray();
		void setInteger3dArray(Integer[][][] x);

		String[][][] getString3dArray();
		void setString3dArray(String[][][] x);

		List<Integer> getIntegerList();
		void setIntegerList(List<Integer> x);

		List<List<List<Integer>>> getInteger3dList();
		void setInteger3dList(List<List<List<Integer>>> x);

		List<Integer[][][]> getInteger1d3dList();
		void setInteger1d3dList(List<Integer[][][]> x);

		List<int[][][]> getInt1d3dList();
		void setInt1d3dList(List<int[][][]> x);

		List<String> getStringList();
		void setStringList(List<String> x);

		// Beans

		ABean getBean();
		void setBean(ABean x);

		ABean[][][] getBean3dArray();
		void setBean3dArray(ABean[][][] x);

		List<ABean> getBeanList();
		void setBeanList(List<ABean> x);

		List<ABean[][][]> getBean1d3dList();
		void setBean1d3dList(List<ABean[][][]> x);

		Map<String,ABean> getBeanMap();
		void setBeanMap(Map<String,ABean> x);

		Map<String,List<ABean>> getBeanListMap();
		void setBeanListMap(Map<String,List<ABean>> x);

		Map<String,List<ABean[][][]>> getBean1d3dListMap();
		void setBean1d3dListMap(Map<String,List<ABean[][][]>> x);

		Map<Integer,List<ABean>> getBeanListMapIntegerKeys();
		void setBeanListMapIntegerKeys(Map<Integer,List<ABean>> x);

		// Typed beans

		TypedBean getTypedBean();
		void setTypedBean(TypedBean x);

		TypedBean[][][] getTypedBean3dArray();
		void setTypedBean3dArray(TypedBean[][][] x);

		List<TypedBean> getTypedBeanList();
		void setTypedBeanList(List<TypedBean> x);

		List<TypedBean[][][]> getTypedBean1d3dList();
		void setTypedBean1d3dList(List<TypedBean[][][]> x);

		Map<String,TypedBean> getTypedBeanMap();
		void setTypedBeanMap(Map<String,TypedBean> x);

		Map<String,List<TypedBean>> getTypedBeanListMap();
		void setTypedBeanListMap(Map<String,List<TypedBean>> x);

		Map<String,List<TypedBean[][][]>> getTypedBean1d3dListMap();
		void setTypedBean1d3dListMap(Map<String,List<TypedBean[][][]>> x);

		Map<Integer,List<TypedBean>> getTypedBeanListMapIntegerKeys();
		void setTypedBeanListMapIntegerKeys(Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		SwappedObject getSwappedObject();
		void setSwappedObject(SwappedObject x);

		SwappedObject[][][] getSwappedObject3dArray();
		void setSwappedObject3dArray(SwappedObject[][][] x);

		Map<SwappedObject,SwappedObject> getSwappedObjectMap();
		void setSwappedObjectMap(Map<SwappedObject,SwappedObject> x);

		Map<SwappedObject,SwappedObject[][][]> getSwappedObject3dMap();
		void setSwappedObject3dMap(Map<SwappedObject,SwappedObject[][][]> x);

		// Implicit swapped POJOs

		ImplicitSwappedObject getImplicitSwappedObject();
		void setImplicitSwappedObject(ImplicitSwappedObject x);

		ImplicitSwappedObject[][][] getImplicitSwappedObject3dArray();
		void setImplicitSwappedObject3dArray(ImplicitSwappedObject[][][] x);

		Map<ImplicitSwappedObject,ImplicitSwappedObject> getImplicitSwappedObjectMap();
		void setImplicitSwappedObjectMap(Map<ImplicitSwappedObject,ImplicitSwappedObject> x);

		Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> getImplicitSwappedObject3dMap();
		void setImplicitSwappedObject3dMap(Map<ImplicitSwappedObject,ImplicitSwappedObject[][][]> x);

		// Enums

		TestEnum getEnum();
		void setEnum(TestEnum x);

		TestEnum[][][] getEnum3d();
		void setEnum3d(TestEnum[][][] x);

		List<TestEnum> getEnumList();
		void setEnumList(List<TestEnum> x);

		List<List<List<TestEnum>>> getEnum3dList();
		void setEnum3dList(List<List<List<TestEnum>>> x);

		List<TestEnum[][][]> getEnum1d3dList();
		void setEnum1d3dList(List<TestEnum[][][]> x);

		Map<TestEnum,TestEnum> getEnumMap();
		void setEnumMap(Map<TestEnum,TestEnum> x);

		Map<TestEnum,TestEnum[][][]> getEnum3dArrayMap();
		void setEnum3dArrayMap(Map<TestEnum,TestEnum[][][]> x);

		Map<TestEnum,List<TestEnum[][][]>> getEnum1d3dListMap();
		void setEnum1d3dListMap(Map<TestEnum,List<TestEnum[][][]>> x);
	}
}