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
package org.apache.juneau.server;

import java.util.*;

/**
 * A large POJO object.
 */
@SuppressWarnings("serial")
public class LargePojo {
	public A1Map a1Map;
	public A1List a1List;
	public A1[] a1Array;

	public static LargePojo create() {
		LargePojo a = new LargePojo();
		a.a1Map = new A1Map();
		a.a1List = new A1List();
		for (int i = 0; i < 20000; i++) {
			a.a1Map.put(String.valueOf(i), new A1());
			a.a1List.add(new A1());
		}
		a.a1Array = a.a1List.toArray(new A1[0]);
		return a;
	}

	public static class A1 {
		public String f1 = "a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789";
	}

	public static class A1Map extends LinkedHashMap<String,A1> {}

	public static class A1List extends LinkedList<A1> {}
}
