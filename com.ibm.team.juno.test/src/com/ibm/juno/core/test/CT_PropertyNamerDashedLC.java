/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.*;

public class CT_PropertyNamerDashedLC {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		PropertyNamer n = new PropertyNamerDashedLC();
		
		assertEquals("abc", n.getPropertyName("ABC"));
		assertEquals("abc", n.getPropertyName("abc"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBaz"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("foo-bar-baz", n.getPropertyName("fooBarBAZ"));
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
		assertEquals("a", n.getPropertyName("A"));
		assertEquals("a", n.getPropertyName("A"));
		
	}
}