/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static com.ibm.juno.core.test.TestUtils.*;
import static com.ibm.juno.core.utils.ArrayUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class CT_ArrayUtils {

	//====================================================================================================
	// iterator(T[])
	//====================================================================================================
	@Test
	public void testArrayIterator() throws Exception {
		assertObjectEquals("[1,2,3]", iterator(new Object[]{1,2,3}));
		assertObjectEquals("[1,2,3]", iterator(new int[]{1,2,3}));
		assertObjectEquals("[]", iterator(null));
	}

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test
	public void testAppendArrayToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, "a", "b");
		assertObjectEquals("['a','b']", s);

		s = append(s, "c");
		assertObjectEquals("['a','b','c']", s);

		s = append(s);
		assertObjectEquals("['a','b','c']", s);

		try {
			append((Object[])null);
			fail();
		} catch (IllegalArgumentException e) {}
	}

	//====================================================================================================
	// append(T[], Collection)
	//====================================================================================================
	@Test
	public void testAppendCollectionToArray() throws Exception {
		String[] s = new String[0];

		s = append(s, Arrays.asList(new String[]{"a","b"}));
		assertObjectEquals("['a','b']", s);

		s = append(s, Arrays.asList(new String[]{"c"}));
		assertObjectEquals("['a','b','c']", s);

		s = append(s, Arrays.asList(new String[0]));
		assertObjectEquals("['a','b','c']", s);

		try {
			append((Object[])null, Collections.emptyList());
			fail();
		} catch (IllegalArgumentException e) {}
	}

	//====================================================================================================
	// reverse(T[] array)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		String[] s = new String[0];

		s = new String[]{"a","b"};
		assertObjectEquals("['b','a']", reverse(s));

		try {
			reverse((Object[])null);
			fail();
		} catch (IllegalArgumentException e) {}
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test
	public void testAsSet() throws Exception {
		String[] s = null;

		try {
			asSet(s);
			fail();
		} catch (IllegalArgumentException e) {}

		s = new String[]{"a"};
		Iterator<String> i = asSet(s).iterator();
		assertEquals("a", i.next());

		try {
			i.remove();
			fail();
		} catch (UnsupportedOperationException e) {}

		try {
			i.next();
			fail();
		} catch (NoSuchElementException e) {}
	}

	//====================================================================================================
	// iterator(T[])
	//====================================================================================================
	@Test
	public void testIterator() throws Exception {
		String[] s = null;

		s = new String[]{"a"};
		Iterator<Object> i = iterator(s);
		assertEquals("a", i.next());

		try {
			i.remove();
			fail();
		} catch (UnsupportedOperationException e) {}

		try {
			i.next();
			fail();
		} catch (NoSuchElementException e) {}
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test
	public void testCombine() throws Exception {
		String[] s1 = new String[]{"a"}, s2 = new String[]{"b"};

		assertObjectEquals("['a','b']", combine(s1, s2));
		assertObjectEquals("['a']", combine(s1));
		assertObjectEquals("['b']", combine(s2));
		assertObjectEquals("['a']", combine(s1,null));
		assertObjectEquals("['b']", combine(null,s2));
		assertNull(combine(null,null));
		assertNull(combine());
	}
}
