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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

/**
 * Interface proxy exposed in InterfaceProxyResource and tested in InterfaceProxyTest.
 */
public interface InterfaceProxy {

	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

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
	Bean returnBean();
	Bean[][][] returnBean3dArray();
	List<Bean> returnBeanList();
	List<Bean[][][]> returnBean1d3dList();
	Map<String,Bean> returnBeanMap();
	Map<String,List<Bean>> returnBeanListMap();
	Map<String,List<Bean[][][]>> returnBean1d3dListMap();
	Map<Integer,List<Bean>> returnBeanListMapIntegerKeys();

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

	//--------------------------------------------------------------------------------
	// Test server-side exception serialization.
	//--------------------------------------------------------------------------------

	void throwException1() throws InterfaceProxyException1;
	void throwException2() throws InterfaceProxyException2;

	//--------------------------------------------------------------------------------
	// Test parameters
	//--------------------------------------------------------------------------------

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
	void setBean(Bean x);
	void setBean3dArray(Bean[][][] x);
	void setBeanList(List<Bean> x);
	void setBean1d3dList(List<Bean[][][]> x);
	void setBeanMap(Map<String,Bean> x);
	void setBeanListMap(Map<String,List<Bean>> x);
	void setBean1d3dListMap(Map<String,List<Bean[][][]>> x);
	void setBeanListMapIntegerKeys(Map<Integer,List<Bean>> x);

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

	//--------------------------------------------------------------------------------
	// Test multi-parameters
	//--------------------------------------------------------------------------------

	void setMultiParamsInts(int x1, int[][][] x2, int[][][] x2n, List<int[][][]> x3, List<int[][][]> x3n);
	void setMultiParamsInteger(Integer x1, Integer x1n, Integer[][][] x2, Integer[][][] x2n, List<Integer[][][]> x3, List<Integer[][][]> x3n);
	void setMultiParamsFloat(float x1, float[][][] x2, float[][][] x2n, List<float[][][]> x3, List<float[][][]> x3n);
	void setMultiParamsFloatObject(Float x1, Float x1n, Float[][][] x2, Float[][][] x2n, List<Float[][][]> x3, List<Float[][][]> x3n);
	void setMultiParamsString(String x1, String[][][] x2, String[][][] x2n, List<String[][][]> x3, List<String[][][]> x3n);
	void setMultiParamsBean(Bean x1, Bean[][][] x2, Bean[][][] x2n, List<Bean[][][]> x3, List<Bean[][][]> x3n, Map<String,Bean> x4, Map<String,Bean> x4n, Map<String,List<Bean[][][]>> x5, Map<String,List<Bean[][][]>> x5n);
	void setMultiParamsSwappedPojo(SwappedPojo x1, SwappedPojo[][][] x2, SwappedPojo[][][] x2n, List<SwappedPojo[][][]> x3, List<SwappedPojo[][][]> x3n, Map<SwappedPojo,SwappedPojo> x4, Map<SwappedPojo,SwappedPojo> x4n, Map<SwappedPojo,List<SwappedPojo[][][]>> x5, Map<SwappedPojo,List<SwappedPojo[][][]>> x5n);
	void setMultiParamsImplicitSwappedPojo(ImplicitSwappedPojo x1, ImplicitSwappedPojo[][][] x2, ImplicitSwappedPojo[][][] x2n, List<ImplicitSwappedPojo[][][]> x3, List<ImplicitSwappedPojo[][][]> x3n, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n);
	void setMultiParamsEnum(TestEnum x1, TestEnum[][][] x2, TestEnum[][][] x2n, List<TestEnum[][][]> x3, List<TestEnum[][][]> x3n, Map<TestEnum,TestEnum> x4, Map<TestEnum,TestEnum> x4n, Map<TestEnum,List<TestEnum[][][]>> x5, Map<TestEnum,List<TestEnum[][][]>> x5n);

	//--------------------------------------------------------------------------------
	// Helper classes
	//--------------------------------------------------------------------------------

	public static class Bean {
		public int a;
		public String b;

		Bean init() {
			this.a = 1;
			this.b = "foo";
			return this;
		}
	}

	@SuppressWarnings("serial")
	public static class InterfaceProxyException1 extends Throwable {
		public InterfaceProxyException1(String msg) {
			super(msg);
		}
	}

	@SuppressWarnings("serial")
	public static class InterfaceProxyException2 extends Throwable {
	}

	@Pojo(swap=SwappedPojoSwap.class)
	public static class SwappedPojo {
		public boolean wasUnswapped;
	}

	public static class SwappedPojoSwap extends PojoSwap<SwappedPojo,String> {
		@Override
		public String swap(BeanSession session, SwappedPojo c) throws SerializeException {
			return "[{(<swapped>)}]";  // Use special characters.
		}

		@Override
		public SwappedPojo unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			SwappedPojo c = new SwappedPojo();
			if (f.equals("[{(<swapped>)}]"))
				c.wasUnswapped = true;
			return c;
		}
	}

	@BeanIgnore
	public static class ImplicitSwappedPojo {
		public boolean wasUnswapped;
		@Override
		public String toString() {
			return "[{(<swapped>)}]";
		}
		public ImplicitSwappedPojo() {
		}
		public ImplicitSwappedPojo(String fromString) {
			if (fromString.equals("[{(<swapped>)}]"))
				wasUnswapped = true;
		}
	}

	public static enum TestEnum {
		ONE,TWO,THREE
	}
}
