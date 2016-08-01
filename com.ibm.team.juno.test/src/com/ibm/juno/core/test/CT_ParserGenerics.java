/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;

@SuppressWarnings("serial")
public class CT_ParserGenerics {

	//====================================================================================================
	// Test generic maps
	//====================================================================================================
	@Test
	public void testMap() throws Exception {
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
	@Test
	public void testCollection() throws Exception {
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
