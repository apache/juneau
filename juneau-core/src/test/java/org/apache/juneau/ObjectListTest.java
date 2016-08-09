/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

@SuppressWarnings("javadoc")
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