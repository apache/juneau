// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

public class ObjectListTest {

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

		r = l.getAt("0/foo", String.class);
		assertEquals("bar", r);

		l.putAt("0/foo", "bing");
		r = l.getAt("0/foo", String.class);
		assertEquals("bing", r);

		l.postAt("", new ObjectMap("{a:'b'}"));
		r = l.getAt("2/a", String.class);
		assertEquals("b", r);

		l.deleteAt("2");
		assertEquals("[{foo:'bing'},{baz:123}]", l.toString());
	}

	//====================================================================================================
	// ObjectList(Reader)
	//====================================================================================================
	@Test
	public void testFromReader() throws Exception {
		assertObjectEquals("[1,2,3]", new ObjectList(new StringReader("[1,2,3]")));
	}
	
	//====================================================================================================
	// testGetMap
	//====================================================================================================
	@Test
	public void testGetMap() throws Exception {
		ObjectList l = new ObjectList("[{1:'true',2:'false'}]");
		Map<Integer,Boolean> m2 = l.getMap(0, Integer.class, Boolean.class);
		assertObjectEquals("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());
	
		m2 = l.get(0, Map.class, Integer.class, Boolean.class);
		assertObjectEquals("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());
	}

	//====================================================================================================
	// testGetList
	//====================================================================================================
	@Test
	public void testGetList() throws Exception {
		ObjectList l = new ObjectList("[['123','456']]");
		List<Integer> l2 = l.getList(0, Integer.class);
		assertObjectEquals("[123,456]", l2);
		assertEquals(Integer.class, l2.iterator().next().getClass());
	
		l2 = l.get(0, List.class, Integer.class);
		assertObjectEquals("[123,456]", l2);
		assertEquals(Integer.class, l2.iterator().next().getClass());
	}
}