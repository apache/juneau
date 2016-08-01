/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.utils;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.core.utils.*;

public class CT_SimpleMap {

	@Test
	public void doTest() throws Exception {
		String[] keys = {"a","b"};
		Object[] vals = {"A","B"};
		SimpleMap m = new SimpleMap(keys, vals);
		assertEquals(2, m.size());
		assertEquals("A", m.get("a"));
		assertEquals("B", m.get("b"));
		assertObjectEquals("{a:'A',b:'B'}", m);
		assertObjectEquals("['a','b']", m.keySet());
		m.put("a", "1");
		assertObjectEquals("{a:'1',b:'B'}", m);
		m.entrySet().iterator().next().setValue("2");
		assertObjectEquals("{a:'2',b:'B'}", m);
		try { m.put("c", "1"); fail(); } catch (IllegalArgumentException e) {}

		assertNull(m.get("c"));

		try { m = new SimpleMap(null, vals); fail(); } catch (IllegalArgumentException e) {}
		try { m = new SimpleMap(keys, null); fail(); } catch (IllegalArgumentException e) {}
		try { m = new SimpleMap(keys, new Object[0]); fail(); } catch (IllegalArgumentException e) {}

		keys[0] = null;
		try { m = new SimpleMap(keys, vals); fail(); } catch (IllegalArgumentException e) {}
	}
}
