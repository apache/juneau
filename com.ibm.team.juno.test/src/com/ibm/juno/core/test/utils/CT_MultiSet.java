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

import com.ibm.juno.core.utils.*;

public class CT_MultiSet {

	@Test
	@SuppressWarnings({ "unchecked" })
	public void doTest() throws Exception {
		List<String> l1, l2;
		MultiSet<String> ms;
		Iterator<String> i;

		l1 = Arrays.asList(new String[]{"1","2"});
		l2 = Arrays.asList(new String[]{"3","4"});
		ms = new MultiSet<String>(l1, l2);
		i = ms.iterator();
		assertTrue(i.hasNext());
		assertEquals("1", i.next());
		assertTrue(i.hasNext());
		assertEquals("2", i.next());
		assertTrue(i.hasNext());
		assertEquals("3", i.next());
		assertTrue(i.hasNext());
		assertEquals("4", i.next());
		assertFalse(i.hasNext());
		try {
			i.next();
			fail("Exception expected");
		} catch (NoSuchElementException e) {
		}

		l1 = Arrays.asList(new String[]{"1","2"});
		l2 = Arrays.asList(new String[]{});
		ms = new MultiSet<String>(l1, l2);
		i = ms.iterator();
		assertTrue(i.hasNext());
		assertEquals("1", i.next());
		assertTrue(i.hasNext());
		assertEquals("2", i.next());
		assertFalse(i.hasNext());
		try {
			i.next();
			fail("Exception expected");
		} catch (NoSuchElementException e) {
		}

		l1 = Arrays.asList(new String[]{});
		l2 = Arrays.asList(new String[]{"3","4"});
		ms = new MultiSet<String>(l1, l2);
		i = ms.iterator();
		assertTrue(i.hasNext());
		assertEquals("3", i.next());
		assertTrue(i.hasNext());
		assertEquals("4", i.next());
		assertFalse(i.hasNext());
		try {
			i.next();
			fail("Exception expected");
		} catch (NoSuchElementException e) {
		}

		l1 = Arrays.asList(new String[]{});
		l2 = Arrays.asList(new String[]{});
		ms = new MultiSet<String>(l1, l2);
		i = ms.iterator();
		assertFalse(i.hasNext());
		try {
			i.next();
			fail("Exception expected");
		} catch (NoSuchElementException e) {
		}

		l1 = Arrays.asList(new String[]{"1","2"});
		ms = new MultiSet<String>(l1);
		i = ms.iterator();
		assertTrue(i.hasNext());
		assertEquals("1", i.next());
		assertTrue(i.hasNext());
		assertEquals("2", i.next());
		assertFalse(i.hasNext());
		try {
			i.next();
			fail("Exception expected");
		} catch (NoSuchElementException e) {
		}

		l1 = new LinkedList<String>(Arrays.asList(new String[]{"1","2"}));
		l2 = new LinkedList<String>(Arrays.asList(new String[]{"3","4"}));
		ms = new MultiSet<String>(l1).append(l2);
		assertObjectEquals("['1','2','3','4']", ms);
		assertObjectEquals("['1','2','3','4']", ms.enumerator());
		assertEquals(4, ms.size());

		Iterator<String> t = ms.iterator();
		t.next();
		t.remove();
		assertObjectEquals("['2','3','4']", ms.enumerator());

		t = ms.iterator();
		t.next();
		t.remove();
		assertObjectEquals("['3','4']", ms.enumerator());

		t = ms.iterator();
		t.next();
		t.remove();
		assertObjectEquals("['4']", ms.enumerator());

		t = ms.iterator();
		t.next();
		t.remove();
		assertObjectEquals("[]", ms.enumerator());
		assertEquals(0, ms.size());

		ms = new MultiSet<String>();
		assertObjectEquals("[]", ms);
		assertEquals(0, ms.size());

		try { ms = new MultiSet<String>((Collection<String>)null); fail(); } catch (IllegalArgumentException e) {}
		try { new MultiSet<String>().iterator().next(); fail(); } catch (NoSuchElementException e) {}
		try { new MultiSet<String>().iterator().remove(); fail(); } catch (NoSuchElementException e) {}

	}
}
