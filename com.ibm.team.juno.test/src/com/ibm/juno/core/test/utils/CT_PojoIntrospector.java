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

public class CT_PojoIntrospector {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		String in = null;
		Object r;

		r = new PojoIntrospector(in, null).invokeMethod("substring(int,int)", "[3,6]");
		assertNull(r);

		in = "foobar";
		r = new PojoIntrospector(in).invokeMethod("substring(int,int)", "[3,6]");
		assertEquals("bar", r);

		r = new PojoIntrospector(in).invokeMethod("toString", null);
		assertEquals("foobar", r);

		r = new PojoIntrospector(in).invokeMethod("toString", "");
		assertEquals("foobar", r);

		r = new PojoIntrospector(in).invokeMethod("toString", "[]");
		assertEquals("foobar", r);

		try { new PojoIntrospector(in).invokeMethod("noSuchMethod", "[3,6]"); fail(); } catch (NoSuchMethodException e) {}

		r = new PojoIntrospector(null).invokeMethod(String.class.getMethod("toString"), null);
		assertNull(r);

		r = new PojoIntrospector("foobar").invokeMethod(String.class.getMethod("toString"), null);
		assertEquals("foobar", r);
	}
}
