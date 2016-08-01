/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_Args {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {	
		Args a;
		
		// Empty args
		a = new Args(new String[]{});
		assertNull(a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));
		
		a = new Args(new String[]{"foo"});
		assertEquals("foo", a.getArg(0));
		assertNull(a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertFalse(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar", "-foo"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertNull(a.getArg("foo"));
		assertEquals(0, a.getArgs("foo").size());
		assertTrue(a.containsKey("foo"));

		a = new Args(new String[]{"foo", "bar bar", "-foo", "bar bar"});
		assertEquals("foo", a.getArg(0));
		assertEquals("bar bar", a.getArg(1));
		assertNull(a.getArg(-1));
		assertEquals("bar bar", a.getArg("foo"));
		assertEquals(1, a.getArgs("foo").size());
		assertEquals("bar bar", a.getArgs("foo").get(0));
		assertTrue(a.containsKey("foo"));
	}
}
