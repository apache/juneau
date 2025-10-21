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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.utils.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class JsonMaps_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// Class with X(JsonMap) constructor and toJsonMap() method.
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_basic(RoundTrip_Tester t) throws Exception {
		var x1 = new A(JsonMap.ofJson("{f1:'a',f2:2}"));
		x1 = t.roundTrip(x1, A.class);
		assertBean(x1, "f1,f2", "a,2");

		var x2 = new A[]{x1};
		x2 = t.roundTrip(x2, A[].class);
		assertBean(x2, "length,#{f1,f2}", "1,[{a,2}]");

		var x3 = alist(new A(JsonMap.ofJson("{f1:'a',f2:2}")));
		x3 = t.roundTrip(x3, List.class, A.class);
		assertBean(x3, "size,0{f1,f2}", "1,{a,2}");

		var x4 = CollectionUtils.map("a",new A(JsonMap.ofJson("{f1:'a',f2:2}")));
		x4 = t.roundTrip(x4, Map.class, String.class, A.class);
		assertMap(x4, "size,a{f1,f2}", "1,{a,2}");
	}

	public static class A {
		private String f1;
		private int f2;
		public A(JsonMap m) {
			this.f1 = m.getString("f1");
			this.f2 = m.getInt("f2");
		}
		public JsonMap swap(BeanSession session) {
			return JsonMap.of("f1",f1,"f2",f2);
		}
	}
}