/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2016. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.*;

public class CT_ObjectList {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		assertEquals(
			"['A','B','C']",
			new ObjectList((Object[])new String[]{"A","B","C"}).toString()
		);

		assertEquals(
			"['A','B','C']",
			new ObjectList("A","B","C").toString()
		);

		assertEquals(
			"['A','B','C']",
			new ObjectList(Arrays.asList(new String[]{"A","B","C"})).toString()
		);
	}

	//====================================================================================================
	// testIterateAs
	//====================================================================================================
	@Test
	public void testIterateAs() throws Exception {

		// Iterate over a list of ObjectMaps.
		ObjectList l = new ObjectList("[{foo:'bar'},{baz:123}]");
		Iterator<ObjectMap> i1 = l.elements(ObjectMap.class).iterator();
		assertEquals("bar", i1.next().getString("foo"));
		assertEquals(123, (int)i1.next().getInt("baz"));

		// Iterate over a list of ints.
		l = new ObjectList("[1,2,3]");
		Iterator<Integer> i2 = l.elements(Integer.class).iterator();
		assertEquals(1, (int)i2.next());
		assertEquals(2, (int)i2.next());
		assertEquals(3, (int)i2.next());

		// Iterate over a list of beans.
		// Automatically converts to beans.
		l = new ObjectList("[{name:'John Smith',age:45}]");
		Iterator<Person> i3 = l.elements(Person.class).iterator();
		assertEquals("John Smith", i3.next().name);
	}

	public static class Person {
		public String name;
		public int age;
	}

	//====================================================================================================
	// testAtMethods
	//====================================================================================================
	@Test
	public void testAtMethods() throws Exception {
		ObjectList l = new ObjectList("[{foo:'bar'},{baz:123}]");
		String r;

		r = l.getAt(String.class, "0/foo");
		assertEquals("bar", r);

		l.putAt("0/foo", "bing");
		r = l.getAt(String.class, "0/foo");
		assertEquals("bing", r);

		l.postAt("", new ObjectMap("{a:'b'}"));
		r = l.getAt(String.class, "2/a");
		assertEquals("b", r);

		l.deleteAt("2");
		assertEquals("[{foo:'bing'},{baz:123}]", l.toString());
	}
}