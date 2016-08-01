/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_IdentityList {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		List<String> l = new IdentityList<String>();
		String a = "a";
		l.add(a);
		l.add(a);
		l.add("b");
		assertEquals(2, l.size());
		assertTrue(l.contains("a"));
		assertFalse(l.contains("c"));
	}
}
