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

import java.util.*;

import org.apache.juneau.internal.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class RoundTripBeanProperties_Test extends BasicRoundTripTest {

	//------------------------------------------------------------------------------------------------------------------
	// Combo arrays/lists
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 {
		public List<Long>[] f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a01_arrayOfListOfLongs(RoundTripTester t) throws Exception {
		var o = new A01();
		o.f1 = new List[1];
		o.f1[0] = alist(123L);
		o = t.roundTrip(o);
		assertEquals(123, o.f1[0].get(0).intValue());
	}

	public static class A02 {
		public List<Long[]> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_listOfArrayOfLongs(RoundTripTester t) throws Exception {
		var o = new A02();
		o.f1 = CollectionUtils.<Long[]>alist(new Long[]{123L});
		o = t.roundTrip(o);
		assertEquals(123, o.f1.get(0)[0].intValue());
	}

	public static class A03 {
		public List<Long>[][] f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_2dArrayOfListOfLongs(RoundTripTester t) throws Exception {
		var o = new A03();
		o.f1 = new List[1][1];
		o.f1[0] = new List[]{alist(123L)};
		o = t.roundTrip(o);
		assertEquals(123, o.f1[0][0].get(0).intValue());
	}

	public static class A04 {
		public List<Long[][]> f1;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_listOf2dArrayOfLongs(RoundTripTester t) throws Exception {
		var o = new A04();
		o.f1 = CollectionUtils.<Long[][]>alist(new Long[][]{new Long[]{123L}});
		o = t.roundTrip(o);
		assertEquals(123, o.f1.get(0)[0][0].intValue());
	}
}