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

import java.util.*;

import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.testutils.*;

/**
 * Interface proxy exposed in InterfaceProxyResource and tested in InterfaceProxyTest.
 */
@Remoteable
public interface InterfaceProxy {

	public static final String SWAP = "swap-~!@#$%^&*()_+`-={}[]|:;\"<,>.?/";

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

	//--------------------------------------------------------------------------------
	// Test multi-parameters
	//--------------------------------------------------------------------------------

	void setMultiParamsInts(int x1, int[][][] x2, int[][][] x2n, List<int[][][]> x3, List<int[][][]> x3n);
	void setMultiParamsInteger(Integer x1, Integer x1n, Integer[][][] x2, Integer[][][] x2n, List<Integer[][][]> x3, List<Integer[][][]> x3n);
	void setMultiParamsFloat(float x1, float[][][] x2, float[][][] x2n, List<float[][][]> x3, List<float[][][]> x3n);
	void setMultiParamsFloatObject(Float x1, Float x1n, Float[][][] x2, Float[][][] x2n, List<Float[][][]> x3, List<Float[][][]> x3n);
	void setMultiParamsString(String x1, String[][][] x2, String[][][] x2n, List<String[][][]> x3, List<String[][][]> x3n);
	void setMultiParamsBean(ABean x1, ABean[][][] x2, ABean[][][] x2n, List<ABean[][][]> x3, List<ABean[][][]> x3n, Map<String,ABean> x4, Map<String,ABean> x4n, Map<String,List<ABean[][][]>> x5, Map<String,List<ABean[][][]>> x5n);
	void setMultiParamsSwappedPojo(SwappedPojo x1, SwappedPojo[][][] x2, SwappedPojo[][][] x2n, List<SwappedPojo[][][]> x3, List<SwappedPojo[][][]> x3n, Map<SwappedPojo,SwappedPojo> x4, Map<SwappedPojo,SwappedPojo> x4n, Map<SwappedPojo,List<SwappedPojo[][][]>> x5, Map<SwappedPojo,List<SwappedPojo[][][]>> x5n);
	void setMultiParamsImplicitSwappedPojo(ImplicitSwappedPojo x1, ImplicitSwappedPojo[][][] x2, ImplicitSwappedPojo[][][] x2n, List<ImplicitSwappedPojo[][][]> x3, List<ImplicitSwappedPojo[][][]> x3n, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4, Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x4n, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5, Map<ImplicitSwappedPojo,List<ImplicitSwappedPojo[][][]>> x5n);
	void setMultiParamsEnum(TestEnum x1, TestEnum[][][] x2, TestEnum[][][] x2n, List<TestEnum[][][]> x3, List<TestEnum[][][]> x3n, Map<TestEnum,TestEnum> x4, Map<TestEnum,TestEnum> x4n, Map<TestEnum,List<TestEnum[][][]>> x5, Map<TestEnum,List<TestEnum[][][]>> x5n);

	//--------------------------------------------------------------------------------
	// Helper classes
	//--------------------------------------------------------------------------------

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
