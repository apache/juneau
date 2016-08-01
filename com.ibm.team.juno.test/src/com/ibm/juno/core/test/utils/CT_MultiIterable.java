/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.utils.*;

public class CT_MultiIterable {

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test() throws Exception {
		List
			l1 = new ObjectList(),
			l2 = new ObjectList("['a','b']"),
			l3 = new ObjectList("['c']");

		MultiIterable mi = new MultiIterable(l1.iterator(),l2.iterator());
		mi.append(l3.iterator());

		assertObjectEquals("['a','b','c']", mi.iterator());

		mi = new MultiIterable(l1.iterator());
		assertObjectEquals("[]", mi.iterator());

		mi = new MultiIterable(l2.iterator(), l1.iterator());
		assertObjectEquals("['a','b']", mi.iterator());

		mi = new MultiIterable(l2.iterator(), l1.iterator(), l3.iterator());
		assertObjectEquals("['a','b','c']", mi.iterator());

		mi = new MultiIterable();
		assertObjectEquals("[]", mi.iterator());

		try { mi.append(null); fail(); } catch (IllegalArgumentException e) {}

		mi = new MultiIterable(l1.iterator());
		try { mi.iterator().next(); fail(); } catch (NoSuchElementException e) {}

		mi = new MultiIterable(l1.iterator());
		Iterator i = mi.iterator();
		assertFalse(i.hasNext());
		try { i.remove(); fail(); } catch (NoSuchElementException e) {}

		mi = new MultiIterable(l2.iterator());
		i = mi.iterator();
		assertTrue(i.hasNext());
		assertEquals("a", i.next());
		i.remove();
	}
}
