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

public class CT_KeywordStore {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		KeywordSet ks = new KeywordSet("aaa", "zzz");
		assertTrue(ks.contains("aaa"));
		assertTrue(ks.contains("zzz"));
		assertFalse(ks.contains("xxx"));
		assertFalse(ks.contains("aaaa"));
		assertFalse(ks.contains("zzzz"));
		assertFalse(ks.contains("\u0000\u1000"));
		assertFalse(ks.contains("z"));
		assertFalse(ks.contains(null));
		assertFalse(ks.contains("a|"));
		assertFalse(ks.contains("|a"));
		assertFalse(ks.contains("Aa"));
		assertFalse(ks.contains("aA"));

		for (String s : new String[]{"a","aA","Aa","a|","|a"}) {
			try { ks = new KeywordSet(s); fail(); } catch (IllegalArgumentException e) {}
		}
	}
}
