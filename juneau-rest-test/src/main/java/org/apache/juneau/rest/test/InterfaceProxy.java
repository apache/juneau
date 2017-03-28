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

/**
 * Interface proxy exposed in InterfaceProxyResource and tested in InterfaceProxyTest.
 */
public interface InterfaceProxy {

	void returnVoid();
	int returnInt();
	Integer returnInteger();
	boolean returnBoolean();
	float returnFloat();
	Float returnFloatObject();
	String returnString();
	String returnNullString();
	int[] returnIntArray();
	int[][] returnInt2dArray();
	int[][][] returnInt3dArray();
	Integer[] returnIntegerArray();
	Integer[][] returnInteger2dArray();
	Integer[][][] returnInteger3dArray();
	String[] returnStringArray();
	String[][] returnString2dArray();
	String[][][] returnString3dArray();
	List<Integer> returnIntegerList();
	List<List<Integer>> returnInteger2dList();
	List<List<List<Integer>>> returnInteger3dList();
	List<Integer[]> returnInteger1d1dList();
	List<Integer[][]> returnInteger1d2dList();
	List<Integer[][][]> returnInteger1d3dList();
	List<int[]> returnInt1d1dList();
	List<int[][]> returnInt1d2dList();
	List<int[][][]> returnInt1d3dList();
	List<String> returnStringList();
	Bean returnBean();
	Bean[] returnBeanArray();
	List<Bean> returnBeanList();
	Map<String,Bean> returnBeanMap();
	Map<String,List<Bean>> returnBeanListMap();
	Map<Integer,List<Bean>> returnBeanListMapIntegerKeys();

	void throwException1() throws InterfaceProxyException1;
	void throwException2() throws InterfaceProxyException2;

	void setNothing();
	void setInt(int x);
	void setInteger(Integer x);
	void setBoolean(boolean x);
	void setFloat(float x);
	void setFloatObject(Float x);
	void setString(String x);
	void setNullString(String x);
	void setIntArray(int[] x);
	void setInt2dArray(int[][] x);
	void setInt3dArray(int[][][] x);
	void setIntegerArray(Integer[] x);
	void setInteger2dArray(Integer[][] x);
	void setInteger3dArray(Integer[][][] x);
	void setStringArray(String[] x);
	void setString2dArray(String[][] x);
	void setString3dArray(String[][][] x);
	void setIntegerList(List<Integer> x);
	void setInteger2dList(List<List<Integer>> x);
	void setInteger3dList(List<List<List<Integer>>> x);
	void setInteger1d1dList(List<Integer[]> x);
	void setInteger1d2dList(List<Integer[][]> x);
	void setInteger1d3dList(List<Integer[][][]> x);
	void setInt1d1dList(List<int[]> x);
	void setInt1d2dList(List<int[][]> x);
	void setInt1d3dList(List<int[][][]> x);
	void setStringList(List<String> x);
	void setBean(Bean x);
	void setBeanArray(Bean[] x);
	void setBeanList(List<Bean> x);
	void setBeanMap(Map<String,Bean> x);
	void setBeanListMap(Map<String,List<Bean>> x);
	void setBeanListMapIntegerKeys(Map<Integer,List<Bean>> x);

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
}
