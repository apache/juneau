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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked"})
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripBeanPropertiesTest extends RoundTripTest {

	public RoundTripBeanPropertiesTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Combo arrays/lists
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 {
		public List<Long>[] f1;
	}

	@Test
	public void a01_arrayOfListOfLongs() throws Exception {
		A01 o = new A01();
		o.f1 = new List[1];
		o.f1[0] = AList.of(123l);
		o = roundTrip(o);
		assertEquals(123, o.f1[0].get(0).intValue());
		assertTrue(o.f1[0].get(0) instanceof Long);
	}

	public static class A02 {
		public List<Long[]> f1;
	}

	@Test
	public void a02_ListOfArrayOfLongs() throws Exception {
		A02 o = new A02();
		o.f1 = AList.<Long[]>of(new Long[]{123l});
		o = roundTrip(o);
		assertEquals(123, o.f1.get(0)[0].intValue());
		assertTrue(o.f1.get(0)[0] instanceof Long);
	}

	public static class A03 {
		public List<Long>[][] f1;
	}

	@Test
	public void a03_2dArrayOfListOfLongs() throws Exception {
		A03 o = new A03();
		o.f1 = new List[1][1];
		o.f1[0] = new List[]{AList.of(123l)};
		o = roundTrip(o);
		assertEquals(123, o.f1[0][0].get(0).intValue());
		assertTrue(o.f1[0][0].get(0) instanceof Long);
	}

	public static class A04 {
		public List<Long[][]> f1;
	}

	@Test
	public void a04_ListOf2dArrayOfLongs() throws Exception {
		A04 o = new A04();
		o.f1 = AList.<Long[][]>of(new Long[][]{new Long[]{123l}});
		o = roundTrip(o);
		assertEquals(123, o.f1.get(0)[0][0].intValue());
		assertTrue(o.f1.get(0)[0][0] instanceof Long);
	}
}
