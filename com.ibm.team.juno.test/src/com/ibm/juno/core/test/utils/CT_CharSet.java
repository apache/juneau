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

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_CharSet {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		AsciiSet cs = new AsciiSet("abc\u1234");
		assertTrue(cs.contains('a'));
		assertFalse(cs.contains('d'));
		assertFalse(cs.contains('\u1234'));
		assertFalse(cs.contains((char)-1));
		assertFalse(cs.contains((char)128));
	}
}
