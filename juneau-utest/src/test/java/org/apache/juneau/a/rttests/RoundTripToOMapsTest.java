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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripToOMapsTest extends RoundTripTest {

	public RoundTripToOMapsTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// Class with X(OMap) constructor and toOMap() method.
	//====================================================================================================
	@Test
	public void test() throws Exception {
		A a = new A(OMap.ofJson("{f1:'a',f2:2}"));
		a = roundTrip(a, A.class);
		assertEquals("a", a.f1);
		assertEquals(2, a.f2);

		A[] aa = new A[]{a};
		aa = roundTrip(aa, A[].class);
		assertEquals(1, aa.length);
		assertEquals("a", aa[0].f1);
		assertEquals(2, aa[0].f2);

		List<A> a2 = alist(new A(OMap.ofJson("{f1:'a',f2:2}")));
		a2 = roundTrip(a2, List.class, A.class);
		assertEquals(1, a2.size());
		assertEquals("a", a2.get(0).f1);
		assertEquals(2, a2.get(0).f2);

		Map<String,A> a3 = map("a",new A(OMap.ofJson("{f1:'a',f2:2}")));
		a3 = roundTrip(a3, Map.class, String.class, A.class);
		assertEquals(1, a3.size());
		assertEquals("a", a3.get("a").f1);
		assertEquals(2, a3.get("a").f2);
	}

	public static class A {
		private String f1;
		private int f2;
		public A(OMap m) {
			this.f1 = m.getString("f1");
			this.f2 = m.getInt("f2");
		}
		public OMap swap(BeanSession session) {
			return OMap.of("f1",f1,"f2",f2);
		}
	}

}
