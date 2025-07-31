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
package org.apache.juneau.config;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigInterfaceTest {

	Config cf;
	ConfigInterface proxy;

	public ConfigInterfaceTest() {
		cf = Config.create().serializer(Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build()).build();
		proxy = cf.getSection("A").asInterface(ConfigInterface.class).orElse(null);
	}


	//====================================================================================================
	// getSectionAsInterface(String,Class)
	//====================================================================================================

	@Test
	public void testString() {
		proxy.setString("foo");
		assertEquals("foo", proxy.getString());
		assertEquals("foo", cf.get("A/string").get());
	}

	@Test
	public void testInt() {
		proxy.setInt(1);
		assertEquals(1, proxy.getInt());
		assertEquals("1", cf.get("A/int").get());
	}

	@Test
	public void testInteger() {
		proxy.setInteger(2);
		assertEquals(2, proxy.getInteger().intValue());
		assertEquals("2", cf.get("A/integer").get());
		assertObject(proxy.getInteger()).isType(Integer.class);
	}

	@Test
	public void testBoolean() {
		proxy.setBoolean(true);
		assertEquals(true, proxy.isBoolean());
		assertEquals("true", cf.get("A/boolean").get());
	}

	@Test
	public void testBooleanObject() {
		proxy.setBooleanObject(true);
		assertEquals(true, proxy.getBooleanObject().booleanValue());
		assertEquals("true", cf.get("A/booleanObject").get());
		assertObject(proxy.getBooleanObject()).isType(Boolean.class);
	}

	@Test
	public void testFloat() {
		proxy.setFloat(1f);
		assertEquals(1f, proxy.getFloat(), 0.1f);
		assertEquals("1.0", cf.get("A/float").get());
	}

	@Test
	public void testFloatObject() {
		proxy.setFloatObject(1f);
		assertEquals(1f, proxy.getFloatObject().floatValue(), 0.1f);
		assertEquals("1.0", cf.get("A/floatObject").get());
		assertObject(proxy.getFloatObject()).isType(Float.class);
	}

	@Test
	public void testInt3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1,2},null},null});
		assertEquals("[[[1,2],null],null]", cf.get("A/int3dArray").get());
		assertObject(proxy.getInt3dArray()).asJson().is("[[[1,2],null],null]");
		assertObject(proxy.getInt3dArray()).isType(int[][][].class);
	}

	@Test
	public void testInteger3dArray() {
		proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null});
		assertObject(proxy.getInteger3dArray()).asJson().is("[[[1,null],null],null]");
		assertEquals("[[[1,null],null],null]", cf.get("A/integer3dArray").get());
		assertObject(proxy.getInteger3dArray()[0][0][0]).isType(Integer.class);
	}

	@Test
	public void testString3dArray() {
		proxy.setString3dArray(new String[][][]{{{"foo",null},null},null});
		assertObject(proxy.getString3dArray()).asJson().is("[[['foo',null],null],null]");
		assertEquals("[[['foo',null],null],null]", cf.get("A/string3dArray").get());
	}

	@Test
	public void testIntegerList() {
		proxy.setIntegerList(alist(1,null));
		assertObject(proxy.getIntegerList()).asJson().is("[1,null]");
		assertEquals("[1,null]", cf.get("A/integerList").get());
		assertObject(proxy.getIntegerList().get(0)).isType(Integer.class);
	}

	@Test
	public void testInteger3dList() {
		proxy.setInteger3dList(alist(alist(alist(1,null),null),null));
		assertObject(proxy.getInteger3dList()).asJson().is("[[[1,null],null],null]");
		assertEquals("[[[1,null],null],null]", cf.get("A/integer3dList").get());
		assertObject(proxy.getInteger3dList().get(0).get(0).get(0)).isType(Integer.class);
	}

	@Test
	public void testInteger1d3dList() {
		proxy.setInteger1d3dList(alist(new Integer[][][]{{{1,null},null},null},null));
		assertObject(proxy.getInteger1d3dList()).asJson().is("[[[[1,null],null],null],null]");
		assertEquals("[[[[1,null],null],null],null]", cf.get("A/integer1d3dList").get());
		assertObject(proxy.getInteger1d3dList().get(0)[0][0][0]).isType(Integer.class);
	}

	@Test
	public void testInt1d3dList() {
		proxy.setInt1d3dList(alist(new int[][][]{{{1,2},null},null},null));
		assertObject(proxy.getInt1d3dList()).asJson().is("[[[[1,2],null],null],null]");
		assertEquals("[[[[1,2],null],null],null]", cf.get("A/int1d3dList").get());
		assertObject(proxy.getInt1d3dList().get(0)).isType(int[][][].class);
	}

	@Test
	public void testStringList() {
		proxy.setStringList(Arrays.asList("foo","bar",null));
		assertObject(proxy.getStringList()).asJson().is("['foo','bar',null]");
		assertEquals("['foo','bar',null]", cf.get("A/stringList").get());
	}

	// Beans

	@Test
	public void testBean() {
		proxy.setBean(ABean.get());
		assertObject(proxy.getBean()).asJson().is("{a:1,b:'foo'}");
		assertEquals("{a:1,b:'foo'}", cf.get("A/bean").get());
		assertObject(proxy.getBean()).isType(ABean.class);
	}

	@Test
	public void testBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{ABean.get(),null},null},null});
		assertObject(proxy.getBean3dArray()).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertEquals("[[[{a:1,b:'foo'},null],null],null]", cf.get("A/bean3dArray").get());
		assertObject(proxy.getBean3dArray()[0][0][0]).isType(ABean.class);
	}

	@Test
	public void testBeanList() {
		proxy.setBeanList(Arrays.asList(ABean.get()));
		assertObject(proxy.getBeanList()).asJson().is("[{a:1,b:'foo'}]");
		assertEquals("[{a:1,b:'foo'}]", cf.get("A/beanList").get());
		assertObject(proxy.getBeanList().get(0)).isType(ABean.class);
	}

	@Test
	public void testBean1d3dList() {
		proxy.setBean1d3dList(alist(new ABean[][][]{{{ABean.get(),null},null},null},null));
		assertObject(proxy.getBean1d3dList()).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertEquals("[[[[{a:1,b:'foo'},null],null],null],null]", cf.get("A/bean1d3dList").get());
		assertObject(proxy.getBean1d3dList().get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void testBeanMap() {
		proxy.setBeanMap(map("foo",ABean.get()));
		assertObject(proxy.getBeanMap()).asJson().is("{foo:{a:1,b:'foo'}}");
		assertEquals("{foo:{a:1,b:'foo'}}", cf.get("A/beanMap").get());
		assertObject(proxy.getBeanMap().get("foo")).isType(ABean.class);
	}

	@Test
	public void testBeanListMap() {
		proxy.setBeanListMap(map("foo",Arrays.asList(ABean.get())));
		assertObject(proxy.getBeanListMap()).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertEquals("{foo:[{a:1,b:'foo'}]}", cf.get("A/beanListMap").get());
		assertObject(proxy.getBeanListMap().get("foo").get(0)).isType(ABean.class);
	}

	@Test
	public void testBean1d3dListMap() {
		proxy.setBean1d3dListMap(map("foo",alist(new ABean[][][]{{{ABean.get(),null},null},null},null)));
		assertObject(proxy.getBean1d3dListMap()).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", cf.get("A/bean1d3dListMap").get());
		assertObject(proxy.getBean1d3dListMap().get("foo").get(0)[0][0][0]).isType(ABean.class);
	}

	@Test
	public void testBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(map(1,Arrays.asList(ABean.get())));
		assertObject(proxy.getBeanListMapIntegerKeys()).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertEquals("{'1':[{a:1,b:'foo'}]}", cf.get("A/beanListMapIntegerKeys").get());
		assertObject(proxy.getBeanListMapIntegerKeys().get(1).get(0)).isType(ABean.class);
	}

	// Typed beans

	@Test
	public void testTypedBean() {
		proxy.setTypedBean(TypedBeanImpl.get());
		assertObject(proxy.getTypedBean()).asJson().is("{a:1,b:'foo'}");
		assertEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", cf.get("A/typedBean").get());
		assertObject(proxy.getTypedBean()).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null});
		assertObject(proxy.getTypedBean3dArray()).asJson().is("[[[{a:1,b:'foo'},null],null],null]");
		assertEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", cf.get("A/typedBean3dArray").get());
		assertObject(proxy.getTypedBean3dArray()[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)TypedBeanImpl.get()));
		assertObject(proxy.getTypedBeanList()).asJson().is("[{a:1,b:'foo'}]");
		assertEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", cf.get("A/typedBeanList").get());
		assertObject(proxy.getTypedBeanList().get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null));
		assertObject(proxy.getTypedBean1d3dList()).asJson().is("[[[[{a:1,b:'foo'},null],null],null],null]");
		assertEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", cf.get("A/typedBean1d3dList").get());
		assertObject(proxy.getTypedBean1d3dList().get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBeanMap() {
		proxy.setTypedBeanMap(map("foo",TypedBeanImpl.get()));
		assertObject(proxy.getTypedBeanMap()).asJson().is("{foo:{a:1,b:'foo'}}");
		assertEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", cf.get("A/typedBeanMap").get());
		assertObject(proxy.getTypedBeanMap().get("foo")).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBeanListMap() {
		proxy.setTypedBeanListMap(map("foo",Arrays.asList((TypedBean)TypedBeanImpl.get())));
		assertObject(proxy.getTypedBeanListMap()).asJson().is("{foo:[{a:1,b:'foo'}]}");
		assertEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", cf.get("A/typedBeanListMap").get());
		assertObject(proxy.getTypedBeanListMap().get("foo").get(0)).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(map("foo",alist(new TypedBean[][][]{{{TypedBeanImpl.get(),null},null},null},null)));
		assertObject(proxy.getTypedBean1d3dListMap()).asJson().is("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}");
		assertEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", cf.get("A/typedBean1d3dListMap").get());
		assertObject(proxy.getTypedBean1d3dListMap().get("foo").get(0)[0][0][0]).isType(TypedBeanImpl.class);
	}

	@Test
	public void testTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(map(1,Arrays.asList((TypedBean)TypedBeanImpl.get())));
		assertObject(proxy.getTypedBeanListMapIntegerKeys()).asJson().is("{'1':[{a:1,b:'foo'}]}");
		assertEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", cf.get("A/typedBeanListMapIntegerKeys").get());
		assertObject(proxy.getTypedBeanListMapIntegerKeys().get(1).get(0)).isType(TypedBeanImpl.class);
	}

	// Swapped POJOs

	@Test
	public void testSwappedObject() {
		proxy.setSwappedObject(new SwappedObject());
		assertObject(proxy.getSwappedObject()).asJson().is("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'");
		assertEquals("swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/", cf.get("A/swappedObject").get());
		assertObject(proxy.getSwappedObject()).isType(SwappedObject.class);
	}

	@Test
	public void testSwappedObject3dArray() {
		proxy.setSwappedObject3dArray(new SwappedObject[][][]{{{new SwappedObject(),null},null},null});
		assertObject(proxy.getSwappedObject3dArray()).asJson().is("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]");
		assertEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", cf.get("A/swappedObject3dArray").get());
		assertObject(proxy.getSwappedObject3dArray()[0][0][0]).isType(SwappedObject.class);
	}

	@Test
	public void testSwappedObjectMap() {
		proxy.setSwappedObjectMap(map(new SwappedObject(), new SwappedObject()));
		assertObject(proxy.getSwappedObjectMap()).asJson().is("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}");
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", cf.get("A/swappedObjectMap").get());
		assertObject(proxy.getSwappedObjectMap().keySet().iterator().next()).isType(SwappedObject.class);
		assertObject(proxy.getSwappedObjectMap().values().iterator().next()).isType(SwappedObject.class);
	}

	@Test
	public void testSwappedObject3dMap() {
		proxy.setSwappedObject3dMap(map(new SwappedObject(), new SwappedObject[][][]{{{new SwappedObject(),null},null},null}));
		assertObject(proxy.getSwappedObject3dMap()).asJson().is("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}");
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", cf.get("A/swappedObject3dMap").get());
		assertObject(proxy.getSwappedObject3dMap().keySet().iterator().next()).isType(SwappedObject.class);
		assertObject(proxy.getSwappedObject3dMap().values().iterator().next()[0][0][0]).isType(SwappedObject.class);
	}

	// Implicit swapped POJOs

	@Test
	public void testImplicitSwappedObject() {
		proxy.setImplicitSwappedObject(new ImplicitSwappedObject());
		assertObject(proxy.getImplicitSwappedObject()).asJson().is("'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'");
		assertEquals("swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/", cf.get("A/implicitSwappedObject").get());
		assertObject(proxy.getImplicitSwappedObject()).isType(ImplicitSwappedObject.class);
	}

	@Test
	public void testImplicitSwappedObject3dArray() {
		proxy.setImplicitSwappedObject3dArray(new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null});
		assertObject(proxy.getImplicitSwappedObject3dArray()).asJson().is("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]");
		assertEquals("[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]", cf.get("A/implicitSwappedObject3dArray").get());
		assertObject(proxy.getImplicitSwappedObject3dArray()[0][0][0]).isType(ImplicitSwappedObject.class);
	}

	@Test
	public void testImplicitSwappedObjectMap() {
		proxy.setImplicitSwappedObjectMap(map(new ImplicitSwappedObject(), new ImplicitSwappedObject()));
		assertObject(proxy.getImplicitSwappedObjectMap()).asJson().is("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}");
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/'}", cf.get("A/implicitSwappedObjectMap").get());
		assertObject(proxy.getImplicitSwappedObjectMap().keySet().iterator().next()).isType(ImplicitSwappedObject.class);
		assertObject(proxy.getImplicitSwappedObjectMap().values().iterator().next()).isType(ImplicitSwappedObject.class);
	}

	@Test
	public void testImplicitSwappedObject3dMap() {
		proxy.setImplicitSwappedObject3dMap(map(new ImplicitSwappedObject(), new ImplicitSwappedObject[][][]{{{new ImplicitSwappedObject(),null},null},null}));
		assertObject(proxy.getImplicitSwappedObject3dMap()).asJson().is("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}");
		assertEquals("{'swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/':[[['swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/',null],null],null]}", cf.get("A/implicitSwappedObject3dMap").get());
		assertObject(proxy.getImplicitSwappedObject3dMap().keySet().iterator().next()).isType(ImplicitSwappedObject.class);
		assertObject(proxy.getImplicitSwappedObject3dMap().values().iterator().next()[0][0][0]).isType(ImplicitSwappedObject.class);
	}

	// Enums

	@Test
	public void testEnum() {
		proxy.setEnum(TestEnum.TWO);
		assertObject(proxy.getEnum()).asJson().is("'TWO'");
		assertEquals("TWO", cf.get("A/enum").get());
		assertObject(proxy.getEnum()).isType(TestEnum.class);
	}

	@Test
	public void testEnum3d() {
		proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
		assertObject(proxy.getEnum3d()).asJson().is("[[['TWO',null],null],null]");
		assertEquals("[[['TWO',null],null],null]", cf.get("A/enum3d").get());
		assertObject(proxy.getEnum3d()[0][0][0]).isType(TestEnum.class);
	}

	@Test
	public void testEnumList() {
		proxy.setEnumList(alist(TestEnum.TWO,null));
		assertObject(proxy.getEnumList()).asJson().is("['TWO',null]");
		assertEquals("['TWO',null]", cf.get("A/enumList").get());
		assertObject(proxy.getEnumList().get(0)).isType(TestEnum.class);
	}

	@Test
	public void testEnum3dList() {
		proxy.setEnum3dList(alist(alist(alist(TestEnum.TWO,null),null),null));
		assertObject(proxy.getEnum3dList()).asJson().is("[[['TWO',null],null],null]");
		assertEquals("[[['TWO',null],null],null]", cf.get("A/enum3dList").get());
		assertObject(proxy.getEnum3dList().get(0).get(0).get(0)).isType(TestEnum.class);
	}

	@Test
	public void testEnum1d3dList() {
		proxy.setEnum1d3dList(alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null));
		assertObject(proxy.getEnum1d3dList()).asJson().is("[[[['TWO',null],null],null],null]");
		assertEquals("[[[['TWO',null],null],null],null]", cf.get("A/enum1d3dList").get());
		assertObject(proxy.getEnum1d3dList().get(0)[0][0][0]).isType(TestEnum.class);
	}

	@Test
	public void testEnumMap() {
		proxy.setEnumMap(map(TestEnum.ONE,TestEnum.TWO));
		assertObject(proxy.getEnumMap()).asJson().is("{ONE:'TWO'}");
		assertEquals("{ONE:'TWO'}", cf.get("A/enumMap").get());
		assertObject(proxy.getEnumMap().keySet().iterator().next()).isType(TestEnum.class);
		assertObject(proxy.getEnumMap().values().iterator().next()).isType(TestEnum.class);
	}

	@Test
	public void testEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(map(TestEnum.ONE,new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
		assertObject(proxy.getEnum3dArrayMap()).asJson().is("{ONE:[[['TWO',null],null],null]}");
		assertEquals("{ONE:[[['TWO',null],null],null]}", cf.get("A/enum3dArrayMap").get());
		assertObject(proxy.getEnum3dArrayMap().keySet().iterator().next()).isType(TestEnum.class);
		assertObject(proxy.getEnum3dArrayMap().values().iterator().next()[0][0][0]).isType(TestEnum.class);
	}

	@Test
	public void testEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(map(TestEnum.ONE,alist(new TestEnum[][][]{{{TestEnum.TWO,null},null},null},null)));
		assertObject(proxy.getEnum1d3dListMap()).asJson().is("{ONE:[[[['TWO',null],null],null],null]}");
		assertEquals("{ONE:[[[['TWO',null],null],null],null]}", cf.get("A/enum1d3dListMap").get());
		assertObject(proxy.getEnum1d3dListMap().keySet().iterator().next()).isType(TestEnum.class);
		assertObject(proxy.getEnum1d3dListMap().values().iterator().next().get(0)[0][0][0]).isType(TestEnum.class);
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