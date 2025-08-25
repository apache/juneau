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
package org.apache.juneau;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class ParserGenericsTest extends SimpleTestBase {

	//====================================================================================================
	// Test generic maps
	//====================================================================================================
	@Test void testMap() throws Exception {
		ReaderParser p = JsonParser.DEFAULT;

		String t = "{foo:{bar:'baz'}}";
		Map<String,TreeMap<String,String>> r1 = p.parse(t, TestMap1.class);
		assertEquals(TestMap1.class, r1.getClass());
		assertEquals(TreeMap.class, r1.get("foo").getClass());

		t = "{foo:[1,2,3]}";
		Map<String,LinkedList<Integer>> r2 = p.parse(t, TestMap2.class);
		assertEquals(TestMap2.class, r2.getClass());
		assertEquals(LinkedList.class, r2.get("foo").getClass());
		assertEquals(Integer.class, r2.get("foo").get(0).getClass());
	}

	public static class TestMap1 extends LinkedHashMap<String,TreeMap<String,String>> {}
	public static class TestMap2 extends LinkedHashMap<String,LinkedList<Integer>> {}

	//====================================================================================================
	// Test generic maps
	//====================================================================================================
	@Test void testCollection() throws Exception {
		ReaderParser p = JsonParser.DEFAULT;

		String t = "[{foo:{bar:'baz'}}]";
		List<TestMap1> r1 = p.parse(t, TestCollection1.class);
		assertEquals(TestCollection1.class, r1.getClass());
		assertEquals(TestMap1.class, r1.get(0).getClass());
		assertEquals(TreeMap.class, r1.get(0).get("foo").getClass());

		t = "[{foo:[1,2,3]}]";
		List<TestMap2> r2 = p.parse(t, TestCollection2.class);
		assertEquals(TestCollection2.class, r2.getClass());
		assertEquals(TestMap2.class, r2.get(0).getClass());
		assertEquals(LinkedList.class, r2.get(0).get("foo").getClass());
		assertEquals(Integer.class, r2.get(0).get("foo").get(0).getClass());
	}

	public static class TestCollection1 extends LinkedList<TestMap1> {}
	public static class TestCollection2 extends LinkedList<TestMap2> {}
}