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
	String[] returnStringArray();
	List<Integer> returnIntegerList();
	List<String> returnStringList();
	Bean returnBean();
	Bean[] returnBeanArray();
	List<Bean> returnBeanList();
	Map<String,Bean> returnBeanMap();
	Map<String,List<Bean>> returnBeanListMap();
	Map<Integer,List<Bean>> returnBeanListMapIntegerKeys();

	void setNothing();
	void setInt(int x);
	void setInteger(Integer x);
	void setBoolean(boolean x);
	void setFloat(float x);
	void setFloatObject(Float x);
	void setString(String x);
	void setNullString(String x);
	void setIntArray(int[] x);
	void setStringArray(String[] x);
	void setIntegerList(List<Integer> x);
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
}
