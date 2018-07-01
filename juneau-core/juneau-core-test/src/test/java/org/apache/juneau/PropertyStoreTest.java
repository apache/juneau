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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.utils.*;
import org.junit.*;


/**
 * Test the PropertyStore class.
 */
public class PropertyStoreTest {

	//====================================================================================================
	// testBasic()
	//====================================================================================================

	@Test
	public void testBasic() {
		PropertyStoreBuilder b = PropertyStore.create();

		b.set("B.f4.s", "4");
		b.set("B.f3.s", "3");
		b.set("A.f2.s", "2");
		b.set("A.f1.s", "1");

		assertObjectEquals("{A:{'f1.s':'1','f2.s':'2'},B:{'f3.s':'3','f4.s':'4'}}", b.build());
	}

	//====================================================================================================
	// testInvalidKeys()
	//====================================================================================================

	@Test
	public void testInvalidKeys() {
		PropertyStoreBuilder b = PropertyStore.create();
		testError(b, "A.f1/add", "foo", "Cannot add value 'foo' (String) to property 'f1' (String).");
		testError(b, "A.f1/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1' (String).");
		testError(b, "A.f1/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1' (String).");
		testError(b, "A.f1/remove.123", "foo", "Invalid key specified: 'A.f1/remove.123'");
		testError(b, "A.f1.s/addx", "foo", "Invalid key specified: 'A.f1.s/addx'");
		testError(b, "A.f1.s/removex", "foo", "Invalid key specified: 'A.f1.s/removex'");
	}

	//====================================================================================================
	// Property type tests
	//====================================================================================================

	@Test
	public void testString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.s", "1");
		b.set("A.f2.s", 2);
		b.set("A.f3.s", true);
		b.set("A.f4.s", new ObjectMap("{foo:'bar'}"));
		b.set("A.f5.s", new ObjectList("[1,2]"));
		b.set("A.f6.s", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.s':'1','f2.s':'2','f3.s':'true','f4.s':'{foo:\\'bar\\'}','f5.s':'[1,2]'}}", ps);
		assertInstanceOf(String.class, ps.getProperty("A.f1.s"));
		assertInstanceOf(String.class, ps.getProperty("A.f2.s"));
		assertInstanceOf(String.class, ps.getProperty("A.f3.s"));
		assertInstanceOf(String.class, ps.getProperty("A.f4.s"));
		assertInstanceOf(String.class, ps.getProperty("A.f5.s"));

		b.clear();
		b.set("A.f1", "1");
		b.set("A.f2", 2);
		b.set("A.f3", true);
		b.set("A.f4", new ObjectMap("{foo:'bar'}"));
		b.set("A.f5", new ObjectList("[1,2]"));
		b.set("A.f6", null);
		ps = b.build();
		assertObjectEquals("{A:{f1:'1',f2:'2',f3:'true',f4:'{foo:\\'bar\\'}',f5:'[1,2]'}}", ps);
		assertInstanceOf(String.class, ps.getProperty("A.f1"));
		assertInstanceOf(String.class, ps.getProperty("A.f2"));
		assertInstanceOf(String.class, ps.getProperty("A.f3"));
		assertInstanceOf(String.class, ps.getProperty("A.f4"));
		assertInstanceOf(String.class, ps.getProperty("A.f5"));

		b.set("A.f1", "x1");
		b.set("A.f2", null);
		b.set("A.f3", null);
		b.remove("A.f4");
		b.remove("A.f5");

		assertObjectEquals("{A:{f1:'x1'}}", b.build());

		testError(b, "A.f1/add", "foo", "Cannot add value 'foo' (String) to property 'f1' (String).");
		testError(b, "A.f1/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1' (String).");
		testError(b, "A.f1/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1' (String).");
	}

	@Test
	public void testBoolean() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.b", "true");
		b.set("A.f2.b", "false");
		b.set("A.f3.b", new StringBuilder("true"));
		b.set("A.f4.b", new StringBuilder("foo"));
		b.set("A.f5.b", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.b':true,'f2.b':false,'f3.b':true,'f4.b':false}}", ps);
		assertInstanceOf(Boolean.class, ps.getProperty("A.f1.b"));
		assertInstanceOf(Boolean.class, ps.getProperty("A.f2.b"));
		assertInstanceOf(Boolean.class, ps.getProperty("A.f3.b"));
		assertInstanceOf(Boolean.class, ps.getProperty("A.f4.b"));

		// Test nulls
		b.set("A.f2.b", null);
		b.set("A.f3.b", null);
		b.remove("A.f4.b");
		b.remove("A.f5.b");
		assertObjectEquals("{A:{'f1.b':true}}", b.build());

		testError(b, "A.f1.b/add", "foo", "Cannot add value 'foo' (String) to property 'f1.b' (Boolean).");
		testError(b, "A.f1.b/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.b' (Boolean).");
		testError(b, "A.f1.b/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.b' (Boolean).");
	}

	@Test
	public void testInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.i", 123);
		b.set("A.f2.i", "123");
		b.set("A.f3.i", new StringBuilder("123"));
		b.set("A.f4.i", new StringBuilder("-1"));
		b.set("A.f5.i", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.i':123,'f2.i':123,'f3.i':123,'f4.i':-1}}", ps);
		assertInstanceOf(Integer.class, ps.getProperty("A.f1.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f2.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f3.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f4.i"));

		// Test nulls
		b.set("A.f2.i", null);
		b.set("A.f3.i", null);
		b.remove("A.f4.i");
		b.remove("A.f5.i");
		assertObjectEquals("{A:{'f1.i':123}}", b.build());

		testError(b, "A.f1.i/add", "foo", "Cannot add value 'foo' (String) to property 'f1.i' (Integer).");
		testError(b, "A.f1.i/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.i' (Integer).");
		testError(b, "A.f1.i/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.i' (Integer).");
		testError(b, "A.f1.i", "foo", "Value 'foo' (String) cannot be converted to an Integer.");
	}

	@Test
	public void testClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.c", String.class);
		b.set("A.f2.c", Integer.class);
		b.set("A.f3.c", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.c':'java.lang.String','f2.c':'java.lang.Integer'}}", ps);
		assertInstanceOf(Class.class, ps.getProperty("A.f1.c"));

		// Test nulls
		b.set("A.f2.c", null);
		assertObjectEquals("{A:{'f1.c':'java.lang.String'}}", b.build());

		testError(b, "A.f1.c/add", "foo", "Cannot add value 'foo' (String) to property 'f1.c' (Class).");
		testError(b, "A.f1.c/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.c' (Class).");
		testError(b, "A.f1.c/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.c' (Class).");

		// Do not allow this for security reasons.
		testError(b, "A.f1.c", "java.lang.String", "Value 'java.lang.String' (String) cannot be converted to a Class.");
	}

	@Test
	public void testObject() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.o", 123);
		b.set("A.f2.o", true);
		b.set("A.f3.o", new StringBuilder("123"));
		b.set("A.f4.o", StringBuilder.class);
		b.set("A.f5.o", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.o':123,'f2.o':true,'f3.o':'123','f4.o':'java.lang.StringBuilder'}}", ps);
		assertInstanceOf(Integer.class, ps.getProperty("A.f1.o"));
		assertInstanceOf(Boolean.class, ps.getProperty("A.f2.o"));
		assertInstanceOf(StringBuilder.class, ps.getProperty("A.f3.o"));
		assertInstanceOf(Class.class, ps.getProperty("A.f4.o"));

		// Test nulls
		b.set("A.f2.o", null);
		b.set("A.f3.o", null);
		b.remove("A.f4.o");
		b.remove("A.f5.o");
		assertObjectEquals("{A:{'f1.o':123}}", b.build());

		testError(b, "A.f1.o/add", "foo", "Cannot add value 'foo' (String) to property 'f1.o' (Object).");
		testError(b, "A.f1.o/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.o' (Object).");
		testError(b, "A.f1.o/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.o' (Object).");
	}

	@Test
	public void testSetString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.ss", new AList<String>().appendAll("foo", "bar", "bar", null));
		b.set("A.f2.ss", new AList<>().appendAll(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ss", new AList<StringBuilder>().appendAll(new StringBuilder("foo"), null));
		b.set("A.f4.ss", "['foo',123,true]");
		b.set("A.f5.ss", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.ss':['bar','foo'],'f2.ss':['123','ONE','true'],'f3.ss':['foo'],'f4.ss':['123','foo','true']}}", ps);
		assertInstanceOf(Set.class, ps.getProperty("A.f1.ss"));
		assertInstanceOf(Set.class, ps.getProperty("A.f2.ss"));
		assertInstanceOf(Set.class, ps.getProperty("A.f3.ss"));
		assertInstanceOf(Set.class, ps.getProperty("A.f4.ss"));

		b.clear();
		b.set("A.f1.ss/add", "foo");
		assertObjectEquals("{A:{'f1.ss':['foo']}}", b.build());
		b.set("A.f1.ss/remove", "foo");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.ss/add", "['foo','bar','baz']");
		b.set("A.f1.ss/add", "qux");
		b.addTo("A.f1.ss","quux");
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}", b.build());
		b.set("A.f1.ss/remove", "['foo','bar']");
		b.set("A.f1.ss/remove", "qux");
		b.removeFrom("A.f1.ss", "quux");
		assertObjectEquals("{A:{'f1.ss':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ss/add", new AList<String>().appendAll("foo", "bar", "baz"));
		b.set("A.f1.ss/add", new AList<String>().appendAll("qux"));
		b.addTo("A.f1.ss", new AList<String>().appendAll("quux"));
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}", b.build());
		b.set("A.f1.ss/remove", new AList<String>().appendAll("foo", "bar"));
		b.set("A.f1.ss/remove", new AList<String>().appendAll("qux"));
		b.removeFrom("A.f1.ss", new AList<String>().appendAll("quux"));
		assertObjectEquals("{A:{'f1.ss':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ss/add", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ss/add", new String[]{"qux"});
		b.addTo("A.f1.ss", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}", b.build());
		b.set("A.f1.ss/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ss/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ss", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ss':['baz']}}", b.build());

		b.set("A.f1.ss", null);
		assertObjectEquals("{}", b.build());

		testError(b, "A.f1.ss/add.123", "foo", "Cannot use argument '123' on add command for property 'f1.ss' (Set<String>)");
		try {
			b.addTo("A.f1.ss", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot use argument 'foo' on add command for property 'f1.ss' (Set<String>)", e.getMessage());
		}
	}

	@Test
	public void testSetInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.si", new AList<Integer>().appendAll(3, 2, 1, null));
		b.set("A.f2.si", new AList<>().appendAll(123, "456", null));
		b.set("A.f3.si", new AList<StringBuilder>().appendAll(new StringBuilder("123"), null));
		b.set("A.f4.si", "[1,2,3]");
		b.set("A.f5.si", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.si':[1,2,3],'f2.si':[123,456],'f3.si':[123],'f4.si':[1,2,3]}}", ps);
		assertInstanceOf(Set.class, ps.getProperty("A.f1.si"));
		assertInstanceOf(Set.class, ps.getProperty("A.f2.si"));
		assertInstanceOf(Set.class, ps.getProperty("A.f3.si"));
		assertInstanceOf(Set.class, ps.getProperty("A.f4.si"));

		b.clear();
		b.set("A.f1.si/add", "123");
		assertObjectEquals("{A:{'f1.si':[123]}}", b.build());
		b.set("A.f1.si/remove", "123");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.si/add", "['3','2','1']");
		b.set("A.f1.si/add", "4");
		b.addTo("A.f1.si", "5");
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", "['1','2']");
		b.set("A.f1.si/remove", "3");
		b.removeFrom("A.f1.si", "4");
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", new AList<String>().appendAll("3", "2", "1"));
		b.set("A.f1.si/add", new AList<String>().appendAll("4"));
		b.addTo("A.f1.si", new AList<String>().appendAll("5"));
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", new AList<String>().appendAll("1", "2"));
		b.set("A.f1.si/remove", new AList<String>().appendAll("3"));
		b.removeFrom("A.f1.si", new AList<String>().appendAll("4"));
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", new AList<Integer>().appendAll(1, 2, 3));
		b.set("A.f1.si/add", new AList<Integer>().appendAll(4));
		b.addTo("A.f1.si", new AList<Integer>().appendAll(5));
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", new AList<Integer>().appendAll(1, 2));
		b.set("A.f1.si/remove", new AList<Integer>().appendAll(3));
		b.removeFrom("A.f1.si", new AList<Integer>().appendAll(4));
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", new String[]{"3", "2", "1"});
		b.set("A.f1.si/add", new String[]{"4"});
		b.addTo("A.f1.si", new String[]{"5"});
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", new String[]{"1", "2"});
		b.set("A.f1.si/remove", new String[]{"3"});
		b.removeFrom("A.f1.si", new String[]{"4"});
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", new Integer[]{3, 2, 1});
		b.set("A.f1.si/add", new Integer[]{4});
		b.addTo("A.f1.si", new Integer[]{5});
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", new Integer[]{1, 2});
		b.set("A.f1.si/remove", new Integer[]{3});
		b.removeFrom("A.f1.si", new Integer[]{4});
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", new int[]{3, 2, 1});
		b.set("A.f1.si/add", new int[]{4});
		b.addTo("A.f1.si", new int[]{5});
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", new int[]{1, 2});
		b.set("A.f1.si/remove", new int[]{3});
		b.removeFrom("A.f1.si", new int[]{4});
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.set("A.f1.si", null);
		assertObjectEquals("{}", b.build());

		testError(b, "A.f1.si/add.123", "foo", "Cannot use argument '123' on add command for property 'f1.si' (Set<Integer>)");
		try {
			b.addTo("A.f1.si", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot use argument 'foo' on add command for property 'f1.si' (Set<Integer>)", e.getMessage());
		}
	}

	@Test
	public void testSetClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.sc", new AList<Class<?>>().appendAll(String.class, Integer.class, null));
		b.set("A.f2.sc", new AList<>().appendAll(String.class, Integer.class, null));
		b.set("A.f3.sc", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String'],'f2.sc':['java.lang.Integer','java.lang.String']}}", ps);
		assertInstanceOf(Set.class, ps.getProperty("A.f1.sc"));
		assertInstanceOf(Set.class, ps.getProperty("A.f2.sc"));
		assertInstanceOf(Class.class, ((Set<?>)ps.getProperty("A.f1.sc")).iterator().next());

		b.clear();
		b.set("A.f1.sc/add", Integer.class);
		b.addTo("A.f1.sc", String.class);
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.sc/remove", Integer.class);
		assertObjectEquals("{A:{'f1.sc':['java.lang.String']}}", b.build());

		b.clear();
		testError(b, "A.f1.sc/add", "['java.lang.Integer']", "Cannot add value '[\\'java.lang.Integer\\']' (String) to property 'f1.sc' (Set<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");
		testError(b, "A.f1.sc/add", "java.lang.Integer", "Cannot add value 'java.lang.Integer' (String) to property 'f1.sc' (Set<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");

		b.clear();
		b.set("A.f1.sc/add", new AList<Class<?>>().appendAll(Integer.class, String.class));
		b.set("A.f1.sc/add", new AList<Class<?>>().appendAll(Map.class));
		b.addTo("A.f1.sc", new AList<Class<?>>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}", b.build());
		b.set("A.f1.sc/remove", new AList<Class<?>>().appendAll(Integer.class, String.class));
		b.removeFrom("A.f1.sc", new AList<Class<?>>().appendAll());
		b.removeFrom("A.f1.sc", new AList<Class<?>>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.sc/add", new AList<>().appendAll(Integer.class, String.class));
		b.set("A.f1.sc/add", new AList<>().appendAll(Map.class));
		b.addTo("A.f1.sc", new AList<>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}", b.build());
		b.set("A.f1.sc/remove", new AList<>().appendAll(Integer.class, String.class));
		b.set("A.f1.sc/remove", new AList<>().appendAll());
		b.removeFrom("A.f1.sc", new AList<>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.sc/add", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.sc/add", new Class<?>[]{Map.class});
		b.addTo("A.f1.sc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}", b.build());
		b.set("A.f1.sc/remove", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.sc/remove", new Class<?>[]{});
		b.removeFrom("A.f1.sc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.sc':['java.util.Map']}}", b.build());

		b.set("A.f1.sc", null);
		assertObjectEquals("{}", b.build());

		testError(b, "A.f1.sc/add.123", String.class, "Cannot use argument '123' on add command for property 'f1.sc' (Set<Class>)");
		try {
			b.addTo("A.f1.sc", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot use argument 'foo' on add command for property 'f1.sc' (Set<Class>)", e.getMessage());
		}
	}

	@Test
	public void testListString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.ls", new AList<String>().appendAll("foo", "bar", "bar", null));
		b.set("A.f2.ls", new AList<>().appendAll(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ls", new AList<StringBuilder>().appendAll(new StringBuilder("foo"), null));
		b.set("A.f4.ls", "['foo',123,true]");
		b.set("A.f5.ls", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.ls':['foo','bar'],'f2.ls':['123','true','ONE'],'f3.ls':['foo'],'f4.ls':['foo','123','true']}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f3.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f4.ls"));

		b.clear();
		b.set("A.f1.ls/add", "foo");
		assertObjectEquals("{A:{'f1.ls':['foo']}}", b.build());
		b.set("A.f1.ls/remove", "foo");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.ls/add", "['foo','bar','baz']");
		b.set("A.f1.ls/add", "qux");
		b.addTo("A.f1.ls","quux");
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());
		b.set("A.f1.ls/remove", "['foo','bar']");
		b.set("A.f1.ls/remove", "qux");
		b.removeFrom("A.f1.ls", "quux");
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ls/add", new AList<String>().appendAll("foo", "bar", "baz"));
		b.set("A.f1.ls/add", new AList<String>().appendAll("qux"));
		b.addTo("A.f1.ls", new AList<String>().appendAll("quux"));
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());
		b.set("A.f1.ls/remove", new AList<String>().appendAll("foo", "bar"));
		b.set("A.f1.ls/remove", new AList<String>().appendAll("qux"));
		b.removeFrom("A.f1.ls", new AList<String>().appendAll("quux"));
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ls/add", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ls/add", new String[]{"qux"});
		b.addTo("A.f1.ls", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());
		b.set("A.f1.ls/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ls/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ls", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.set("A.f1.ls", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.ls/add", "['foo','bar','baz']");
		b.set("A.f1.ls/add.10", "qux");
		b.set("A.f1.ls/add.1", "quux");
		b.set("A.f1.ls/add.0", "quuux");
		b.set("A.f1.ls/add.-10", "quuuux");
		assertObjectEquals("{A:{'f1.ls':['quuuux','quuux','foo','quux','bar','baz','qux']}}", b.build());
		b.set("A.f1.ls/add.1", "['1','2']");
		assertObjectEquals("{A:{'f1.ls':['quuuux','1','2','quuux','foo','quux','bar','baz','qux']}}", b.build());

		testError(b, "A.f1.ls/add.foo", "foo", "Invalid argument 'foo' on add command for property 'f1.ls' (List<String>)");
		try {
			b.addTo("A.f1.ls", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Invalid argument 'foo' on add command for property 'f1.ls' (List<String>)", e.getMessage());
		}
	}

	@Test
	public void testListInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.li", new AList<Integer>().appendAll(1, 2, 3, null));
		b.set("A.f2.li", new AList<>().appendAll(123, "456", null));
		b.set("A.f3.li", new AList<StringBuilder>().appendAll(new StringBuilder("123"), null));
		b.set("A.f4.li", "[1,2,3]");
		b.set("A.f5.li", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.li':[1,2,3],'f2.li':[123,456],'f3.li':[123],'f4.li':[1,2,3]}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f3.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f4.li"));

		b.clear();
		b.set("A.f1.li/add", "123");
		assertObjectEquals("{A:{'f1.li':[123]}}", b.build());
		b.set("A.f1.li/remove", "123");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.li/add", "['1','2','3']");
		b.set("A.f1.li/add", "4");
		b.addTo("A.f1.li", "5");
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", "['1','2']");
		b.set("A.f1.li/remove", "3");
		b.removeFrom("A.f1.li", "4");
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/add", new AList<String>().appendAll("1", "2", "3"));
		b.set("A.f1.li/add", new AList<String>().appendAll("4"));
		b.addTo("A.f1.li", new AList<String>().appendAll("5"));
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new AList<String>().appendAll("1", "2"));
		b.set("A.f1.li/remove", new AList<String>().appendAll("3"));
		b.removeFrom("A.f1.li", new AList<String>().appendAll("4"));
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/add", new AList<Integer>().appendAll(1, 2, 3));
		b.set("A.f1.li/add", new AList<Integer>().appendAll(4));
		b.addTo("A.f1.li", new AList<Integer>().appendAll(5));
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new AList<Integer>().appendAll(1, 2));
		b.set("A.f1.li/remove", new AList<Integer>().appendAll(3));
		b.removeFrom("A.f1.li", new AList<Integer>().appendAll(4));
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/add", new String[]{"1", "2", "3"});
		b.set("A.f1.li/add", new String[]{"4"});
		b.addTo("A.f1.li", new String[]{"5"});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new String[]{"1", "2"});
		b.set("A.f1.li/remove", new String[]{"3"});
		b.removeFrom("A.f1.li", new String[]{"4"});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/add", new Integer[]{1, 2, 3});
		b.set("A.f1.li/add", new Integer[]{4});
		b.addTo("A.f1.li", new Integer[]{5});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new Integer[]{1, 2});
		b.set("A.f1.li/remove", new Integer[]{3});
		b.removeFrom("A.f1.li", new Integer[]{4});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/add", new int[]{1, 2, 3});
		b.set("A.f1.li/add", new int[]{4});
		b.addTo("A.f1.li", new int[]{5});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new int[]{1, 2});
		b.set("A.f1.li/remove", new int[]{3});
		b.removeFrom("A.f1.li", new int[]{4});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.set("A.f1.li", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.ls/add", "['1','2','3']");
		b.set("A.f1.ls/add.10", "4");
		b.set("A.f1.ls/add.1", "5");
		b.set("A.f1.ls/add.0", "6");
		b.set("A.f1.ls/add.-10", "7");
		assertObjectEquals("{A:{'f1.ls':['7','6','1','5','2','3','4']}}", b.build());
		b.set("A.f1.ls/add.1", "['8','9']");
		assertObjectEquals("{A:{'f1.ls':['7','8','9','6','1','5','2','3','4']}}", b.build());

		testError(b, "A.f1.li/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.li' (List<Integer>).  Value 'foo' (String) cannot be converted to an Integer.");
		try {
			b.addTo("A.f1.li", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Invalid argument 'foo' on add command for property 'f1.li' (List<Integer>)", e.getMessage());
		}
	}

	@Test
	public void testListClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.lc", new AList<Class<?>>().appendAll(String.class, Integer.class, null));
		b.set("A.f2.lc", new AList<>().appendAll(String.class, Integer.class, null));
		b.set("A.f3.lc", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.lc':['java.lang.String','java.lang.Integer'],'f2.lc':['java.lang.String','java.lang.Integer']}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.lc"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.lc"));

		b.clear();
		b.set("A.f1.lc/add", Integer.class);
		b.addTo("A.f1.lc", String.class);
		assertObjectEquals("{A:{'f1.lc':['java.lang.String','java.lang.Integer']}}", b.build());
		b.set("A.f1.lc/remove", Integer.class);
		assertObjectEquals("{A:{'f1.lc':['java.lang.String']}}", b.build());

		b.clear();
		testError(b, "A.f1.lc/add", "['java.lang.Integer']", "Cannot add value '[\\'java.lang.Integer\\']' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");
		testError(b, "A.f1.lc/add", "java.lang.Integer", "Cannot add value 'java.lang.Integer' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");

		b.clear();
		b.set("A.f1.lc/add", AList.<Class<?>>create(Integer.class, String.class));
		b.set("A.f1.lc/add", new AList<Class<?>>().appendAll(Map.class));
		b.addTo("A.f1.lc", new AList<Class<?>>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", new AList<Class<?>>().appendAll(Integer.class, String.class));
		b.removeFrom("A.f1.lc", new AList<Class<?>>().appendAll());
		b.removeFrom("A.f1.lc", new AList<Class<?>>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.lc/add", new AList<>().appendAll(Integer.class, String.class));
		b.set("A.f1.lc/add", new AList<>().appendAll(Map.class));
		b.addTo("A.f1.lc", new AList<>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", new AList<>().appendAll(Integer.class, String.class));
		b.set("A.f1.lc/remove", new AList<>().appendAll());
		b.removeFrom("A.f1.lc", new AList<>().appendAll(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.lc/add", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/add", new Class<?>[]{Map.class});
		b.addTo("A.f1.lc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.set("A.f1.lc", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.lc/add", String.class);
		b.set("A.f1.lc/add.10", Integer.class);
		b.set("A.f1.lc/add.1", Map.class);
		b.set("A.f1.lc/add.0", List.class);
		b.set("A.f1.lc/add.-10", Object.class);
		assertObjectEquals("{A:{'f1.lc':['java.lang.Object','java.util.List','java.lang.String','java.util.Map','java.lang.Integer']}}", b.build());

		testError(b, "A.f1.lc/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.lc' (List<Class>).  Value 'foo' (String) cannot be converted to a Class.");
		try {
			b.addTo("A.f1.lc", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Invalid argument 'foo' on add command for property 'f1.lc' (List<Class>)", e.getMessage());
		}
	}

	@Test
	public void testListObject() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.lo", new AList<Class<?>>().appendAll(StringBuilder.class, null));
		b.set("A.f2.lo", new AList<>().appendAll(123, true, new StringBuilder(123), StringBuilder.class, null));
		b.set("A.f3.lo", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder'],'f2.lo':[123,true,'','java.lang.StringBuilder']}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.lo"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.lo"));
		assertInstanceOf(Class.class, ((List<?>)ps.getProperty("A.f1.lo")).get(0));
		assertInstanceOf(Integer.class, ((List<?>)ps.getProperty("A.f2.lo")).get(0));
		assertInstanceOf(Boolean.class, ((List<?>)ps.getProperty("A.f2.lo")).get(1));
		assertInstanceOf(StringBuilder.class, ((List<?>)ps.getProperty("A.f2.lo")).get(2));
		assertInstanceOf(Class.class, ((List<?>)ps.getProperty("A.f2.lo")).get(3));

		b.clear();
		b.set("A.f1.lo/add", 1);
		b.addTo("A.f1.lo", 2);
		assertObjectEquals("{A:{'f1.lo':[2,1]}}", b.build());
		b.set("A.f1.lo/remove", 1);
		assertObjectEquals("{A:{'f1.lo':[2]}}", b.build());

		b.clear();
		b.set("A.f1.lo/add", new AList<Class<?>>().appendAll(StringBuilder.class));
		b.set("A.f1.lo/add", new AList<Class<?>>().appendAll(HashMap.class));
		b.addTo("A.f1.lo", new AList<Class<?>>().appendAll(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", new AList<Class<?>>().appendAll(HashMap.class));
		b.removeFrom("A.f1.lo", new AList<Class<?>>().appendAll());
		b.removeFrom("A.f1.lo", new AList<Class<?>>().appendAll(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.clear();
		b.set("A.f1.lo/add", new AList<>().appendAll(StringBuilder.class));
		b.set("A.f1.lo/add", new AList<>().appendAll(HashMap.class));
		b.addTo("A.f1.lo", new AList<>().appendAll(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", new AList<>().appendAll(HashMap.class));
		b.set("A.f1.lo/remove", new AList<>().appendAll());
		b.removeFrom("A.f1.lo", new AList<>().appendAll(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.clear();
		b.set("A.f1.lo/add", new Class<?>[]{StringBuilder.class});
		b.set("A.f1.lo/add", new Class<?>[]{HashMap.class});
		b.addTo("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", new Class<?>[]{HashMap.class});
		b.set("A.f1.lo/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.set("A.f1.lo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.lo/add", StringBuilder.class);
		b.set("A.f1.lo/add.10", HashMap.class);
		b.set("A.f1.lo/add.1", LinkedList.class);
		b.set("A.f1.lo/add.0", TestEnum.ONE);
		b.set("A.f1.lo/add.-10", TestEnum.TWO);
		assertObjectEquals("{A:{'f1.lo':['TWO','ONE','java.lang.StringBuilder','java.util.LinkedList','java.util.HashMap']}}", b.build());

		try {
			b.addTo("A.f1.lo", "foo", "bar");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Invalid argument 'foo' on add command for property 'f1.lo' (List<Object>)", e.getMessage());
		}
	}

	@Test
	public void testMapString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.sms", new AMap<String,String>().append("foo", "bar").append("baz", "qux").append("quux", null).append(null, null));
		b.set("A.f2.sms", new AMap<String,Object>().append("foo", 123).append("bar", true).append("baz", TestEnum.ONE).append("qux", null));
		b.set("A.f3.sms", new AMap<String,StringBuilder>().append("foo", new StringBuilder("bar")).append("baz", null));
		b.set("A.f4.sms", "{foo:'bar',baz:123,qux:true}");
		b.set("A.f5.sms", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.sms':{baz:'qux',foo:'bar'},'f2.sms':{bar:'true',baz:'ONE',foo:'123'},'f3.sms':{foo:'bar'},'f4.sms':{baz:'123',foo:'bar',qux:'true'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.sms"));

		b.clear();
		b.set("A.f1.sms/add", "{foo:'bar'}");
		assertObjectEquals("{A:{'f1.sms':{foo:'bar'}}}", b.build());

		b.clear();
		b.set("A.f1.sms/add.foo", "bar");
		assertObjectEquals("{A:{'f1.sms':{foo:'bar'}}}", b.build());
		b.set("A.f1.sms/add.foo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.sms", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.sms", "{foo:'bar'}");
		testError(b, "A.f1.sms/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.sms' (Map<String,String>).");
		try {
			b.removeFrom("A.f1.sms", "foo");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot remove value 'foo' (String) from property 'f1.sms' (Map<String,String>).", e.getMessage());
		}
	}

	@Test
	public void testMapInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.smi", new AMap<String,String>().append("foo", "1").append("baz", "2").append("quux", null).append(null, null));
		b.set("A.f2.smi", new AMap<String,Object>().append("foo", 123).append("bar", "456").append("baz", null));
		b.set("A.f3.smi", new AMap<String,StringBuilder>().append("foo", new StringBuilder("123")).append("baz", null));
		b.set("A.f4.smi", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smi", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smi':{baz:2,foo:1},'f2.smi':{bar:456,foo:123},'f3.smi':{foo:123},'f4.smi':{baz:456,foo:123}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.smi"));

		b.clear();
		b.set("A.f1.smi/add", "{foo:'123'}");
		assertObjectEquals("{A:{'f1.smi':{foo:123}}}", b.build());

		b.clear();
		b.set("A.f1.smi/add.foo", "123");
		assertObjectEquals("{A:{'f1.smi':{foo:123}}}", b.build());
		b.set("A.f1.smi/add.foo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smi", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smi", "{foo:'123'}");
		testError(b, "A.f1.smi/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smi' (Map<String,Integer>).");
		try {
			b.removeFrom("A.f1.smi", "foo");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot remove value 'foo' (String) from property 'f1.smi' (Map<String,Integer>).", e.getMessage());
		}
	}

	@Test
	public void testMapClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.smc", new AMap<String,Class<?>>().append("foo", String.class).append("baz", Integer.class).append("quux", null).append(null, null));
		b.set("A.f2.smc", new AMap<String,Object>().append("foo", String.class).append("bar", Integer.class).append("baz", null));
		b.set("A.f3.smc", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smc':{baz:'java.lang.Integer',foo:'java.lang.String'},'f2.smc':{bar:'java.lang.Integer',foo:'java.lang.String'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smc"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smc"));
		assertInstanceOf(Class.class, ((Map<?,?>)ps.getProperty("A.f1.smc")).values().iterator().next());
		assertInstanceOf(Class.class, ((Map<?,?>)ps.getProperty("A.f2.smc")).values().iterator().next());

		b.clear();
		b.set("A.f1.smc/add.foo", String.class);
		assertObjectEquals("{A:{'f1.smc':{foo:'java.lang.String'}}}", b.build());
		b.set("A.f1.smc/add.foo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smc", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smc/add.foo", String.class);
		testError(b, "A.f1.smc/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smc' (Map<String,Class>).");
		try {
			b.removeFrom("A.f1.smc", "foo");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot remove value 'foo' (String) from property 'f1.smc' (Map<String,Class>).", e.getMessage());
		}
	}

	@Test
	public void testMapObject() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();

		PropertyStore ps = null;
		b.set("A.f1.smo", new AMap<String,String>().append("foo", "1").append("baz", "2").append("quux", null).append(null, null));
		b.set("A.f2.smo", new AMap<String,Object>().append("foo", 123).append("bar", StringBuilder.class).append("qux", null));
		b.set("A.f3.smo", new AMap<String,StringBuilder>().append("foo", new StringBuilder("123")).append("baz", null));
		b.set("A.f4.smo", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smo", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smo':{baz:'2',foo:'1'},'f2.smo':{bar:'java.lang.StringBuilder',foo:123},'f3.smo':{foo:'123'},'f4.smo':{baz:456,foo:'123'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.smo"));

		b.clear();
		b.set("A.f1.smo/add", "{foo:'123'}");
		assertObjectEquals("{A:{'f1.smo':{foo:'123'}}}", b.build());

		b.clear();
		b.set("A.f1.smo/add.foo", "123");
		assertObjectEquals("{A:{'f1.smo':{foo:'123'}}}", b.build());
		b.set("A.f1.smo/add.foo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smo", "{foo:'123'}");
		testError(b, "A.f1.smo/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smo' (Map<String,Object>).");
		try {
			b.removeFrom("A.f1.smo", "foo");
			fail("Exception expected.");
		} catch (Exception e) {
			assertEquals("Cannot remove value 'foo' (String) from property 'f1.smo' (Map<String,Object>).", e.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Hash tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testPropertyTypeStringHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1", "foo");
		b2.set("A.f1", new StringBuilder("foo"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1", "foo");
		b2.set("A.f1", new StringBuilder("foox"));
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1", "foo");
		assertTrue(ps == b1.build());

		b1.set("A.f1", "bar");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testPropertyTypeBooleanHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.b", true);
		b2.set("A.f1.b", new StringBuilder("true"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.b", true);
		b2.set("A.f1.b", new StringBuilder("false"));
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.b", true);
		assertTrue(ps == b1.build());

		b1.set("A.f1.b", false);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testPropertyTypeIntegerHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.i", 1);
		b2.set("A.f1.i", new StringBuilder("1"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.i", 1);
		b2.set("A.f1.i", new StringBuilder("2"));

		testNotEquals(b1, b2);
		assertTrue(b1.build() != b2.build());

		ps = b1.build();
		b1.set("A.f1.i", 1);
		assertTrue(ps == b1.build());

		b1.set("A.f1.i", 2);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testClassHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.c", String.class);
		b2.set("A.f1.c", String.class);
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.c", String.class);
		b2.set("A.f1.c", Integer.class);

		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.c", String.class);
		assertTrue(ps == b1.build());

		b1.set("A.f1.c", Integer.class);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testObjectHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.o", "foo");
		b2.set("A.f1.o", "foo");
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.o", TestEnum.ONE);
		b2.set("A.f1.o", TestEnum.TWO);
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.o", TestEnum.ONE);
		assertTrue(ps == b1.build());

		b1.set("A.f1.o", TestEnum.TWO);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetStringHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.ss", new AList<String>().appendAll("foo", "bar"));
		b2.set("A.f1.ss", new String[]{"foo","bar"});
		testEquals(b1, b2);

		b2.set("A.f1.ss", new AList<>().appendAll(new StringBuilder("bar"), new StringBuilder("foo")));
		testEquals(b1, b2);

		b2.set("A.f1.ss", new Object[]{new StringBuilder("bar"), new StringBuilder("foo")});
		testEquals(b1, b2);

		b1.set("A.f1.ss", new String[]{"foo"});
		b2.set("A.f1.ss", new String[]{"foox"});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.ss", "['foo']");
		assertTrue(ps == b1.build());

		b1.set("A.f1.ss", "['bar']");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetIntegerHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.si", new AList<String>().appendAll("1", "2"));
		b2.set("A.f1.si", new String[]{"1","2"});
		testEquals(b1, b2);

		b2.set("A.f1.si", new AList<>().appendAll(new StringBuilder("2"), 1));
		testEquals(b1, b2);

		b2.set("A.f1.si", new Object[]{new StringBuilder("2"), 1});
		testEquals(b1, b2);

		b1.set("A.f1.si", new String[]{"1"});
		b2.set("A.f1.si", new String[]{"2"});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.si", "['1']");
		assertTrue(ps == b1.build());

		b1.set("A.f1.si", "['2']");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetClassHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.sc", new AList<Class<?>>().appendAll(String.class, Integer.class));
		b2.set("A.f1.sc", new Class<?>[]{Integer.class,String.class});
		testEquals(b1, b2);

		b2.set("A.f1.sc", new AList<>().appendAll(Integer.class, String.class));
		testEquals(b1, b2);

		b2.set("A.f1.sc", new Object[]{String.class, Integer.class});
		testEquals(b1, b2);

		b1.set("A.f1.sc", new Class[]{String.class});
		b2.set("A.f1.sc", new Class[]{Integer.class});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.sc", String.class);
		assertTrue(ps == b1.build());

		b1.set("A.f1.sc", Map.class);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListStringHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.ls", new AList<String>().appendAll("foo", "bar"));
		b2.set("A.f1.ls", new String[]{"foo","bar"});
		testEquals(b1, b2);

		b2.set("A.f1.ls", new AList<>().appendAll(new StringBuilder("foo"), new StringBuilder("bar")));
		testEquals(b1, b2);

		b2.set("A.f1.ls", new AList<>().appendAll(new StringBuilder("bar"), new StringBuilder("foo")));
		testNotEquals(b1, b2);

		b2.set("A.f1.ls", new Object[]{new StringBuilder("foo"), new StringBuilder("bar")});
		testEquals(b1, b2);

		b2.set("A.f1.ls", new Object[]{new StringBuilder("foo"), new StringBuilder("bar")});
		testEquals(b1, b2);

		b1.set("A.f1.ls", new String[]{"foo"});
		b2.set("A.f1.ls", new String[]{"foox"});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.ls", "['foo']");
		assertTrue(ps == b1.build());

		b1.set("A.f1.ls", "['bar']");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListIntegerHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.li", new AList<String>().appendAll("1", "2"));
		b2.set("A.f1.li", new String[]{"1","2"});
		testEquals(b1, b2);

		b2.set("A.f1.li", new String[]{"2","1"});
		testNotEquals(b1, b2);

		b2.set("A.f1.li", new int[]{1,2});
		testEquals(b1, b2);

		b2.set("A.f1.li", new AList<>().appendAll(new StringBuilder("2"), 1));
		testNotEquals(b1, b2);

		b2.set("A.f1.li", new Object[]{new StringBuilder("1"), 2});
		testEquals(b1, b2);

		b1.set("A.f1.li", new String[]{"1"});
		b2.set("A.f1.li", new String[]{"2"});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.li", "['1']");
		assertTrue(ps == b1.build());

		b1.set("A.f1.li", "['2']");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListClassHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.lc", new AList<Class<?>>().appendAll(String.class, Integer.class));

		b2.set("A.f1.lc", new Class<?>[]{String.class,Integer.class});
		testEquals(b1, b2);

		b2.set("A.f1.lc", new Class<?>[]{Integer.class,String.class});
		testNotEquals(b1, b2);

		b2.set("A.f1.lc", new AList<>().appendAll(String.class, Integer.class));
		testEquals(b1, b2);

		b2.set("A.f1.lc", new Object[]{String.class, Integer.class});
		testEquals(b1, b2);

		b1.set("A.f1.lc", new Class[]{String.class});
		b2.set("A.f1.lc", new Class[]{Integer.class});
		testNotEquals(b1, b2);

		ps = b1.build();
		b1.set("A.f1.lc", String.class);
		assertTrue(ps == b1.build());

		b1.set("A.f1.lc", Map.class);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListObjectHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.lo", new AList<>().appendAll("foo", 123, true, TestEnum.ONE));

		b2.set("A.f1.lo", new AList<>().appendAll("foo", 123, true, TestEnum.ONE));
		testEquals(b1, b2);

		b2.set("A.f1.lo", new AList<>().appendAll(123, true, TestEnum.ONE, "foo"));
		testNotEquals(b1, b2);

		b2.set("A.f1.lo", new Object[]{"foo", 123, true, TestEnum.ONE});
		testEquals(b1, b2);

		b1.set("A.f1.lo", new Object[]{StringBuilder.class});
		b2.set("A.f1.lo", new Object[]{StringBuffer.class});
		testNotEquals(b1, b2);

		b1.set("A.f1.lo", "foo");
		ps = b1.build();
		b1.set("A.f1.lo", "foo");
		assertTrue(ps == b1.build());

		b1.set("A.f1.lo", "bar");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapStringHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.sms", new AMap<String,String>().append("foo", "123").append("bar", "true").append("baz", null).append(null, null));
		b2.set("A.f1.sms", new AMap<String,Object>().append("foo", 123).append("bar", true).append("baz", null).append(null, null));
		testEquals(b1, b2);

		b2.set("A.f1.sms", new AMap<String,Object>().append("foo", new StringBuilder("123")).append("bar", new StringBuilder("true")));
		testEquals(b1, b2);

		b2.set("A.f1.sms", new AMap<String,Object>().append("bar", new StringBuilder("true")).append("foo", new StringBuilder("123")));
		testEquals(b1, b2);

		b2.set("A.f1.sms", new AMap<String,Object>().append("bar", false).append("foo", new StringBuilder("123")));
		testNotEquals(b1, b2);

		b1.set("A.f1.sms", "{foo:'bar'}");
		ps = b1.build();
		b1.set("A.f1.sms", "{foo:'bar'}");
		assertTrue(ps == b1.build());

		b1.set("A.f1.sms", "{foo:'baz'}");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapIntegerHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.smi", new AMap<String,Integer>().append("foo", 123).append("bar", 456).append("baz", null).append(null, null));
		b2.set("A.f1.smi", new AMap<String,Object>().append("foo", 123).append("bar", "456").append("baz", null).append(null, null));
		testEquals(b1, b2);

		b2.set("A.f1.smi", new AMap<String,Object>().append("foo", new StringBuilder("123")).append("bar", new StringBuilder("456")));
		testEquals(b1, b2);

		b2.set("A.f1.smi", new AMap<String,Object>().append("bar", new StringBuilder("456")).append("foo", new StringBuilder("123")));
		testEquals(b1, b2);

		b2.set("A.f1.smi", new AMap<String,Object>().append("bar", "457").append("foo", new StringBuilder("123")));
		testNotEquals(b1, b2);

		b1.set("A.f1.smi", "{foo:'123'}");
		ps = b1.build();
		b1.set("A.f1.smi", "{foo:'123'}");
		assertTrue(ps == b1.build());

		b1.set("A.f1.smi", "{foo:'456'}");
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapClassHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.smc", new AMap<String,Class<?>>().append("foo", String.class).append("bar", Integer.class).append("baz", null).append(null, null));
		b2.set("A.f1.smc", new AMap<String,Object>().append("foo", String.class).append("bar", Integer.class).append("baz", null).append(null, null));
		testEquals(b1, b2);

		b2.set("A.f1.smc", new AMap<String,Object>().append("foo", String.class).append("bar", Integer.class));
		testEquals(b1, b2);

		b2.set("A.f1.smc", new AMap<String,Object>().append("bar", Integer.class).append("foo", String.class));
		testEquals(b1, b2);

		b2.set("A.f1.smc", new AMap<String,Object>().append("bar", Integer.class).append("foo", StringBuilder.class));
		testNotEquals(b1, b2);

		b1.clear();
		b1.set("A.f1.smc/add.foo", Integer.class);
		ps = b1.build();
		b1.set("A.f1.smc/add.foo", Integer.class);
		assertTrue(ps == b1.build());

		b1.set("A.f1.smc/add.foo", String.class);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapObjectHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.smo", new AMap<String,TestEnum>().append("foo", TestEnum.ONE).append("bar", TestEnum.TWO).append("baz", null).append(null, null));
		b2.set("A.f1.smo", new AMap<String,Object>().append("foo", TestEnum.ONE).append("bar", TestEnum.TWO).append("baz", null).append(null, null));
		testEquals(b1, b2);

		b2.set("A.f1.smo", new AMap<String,Object>().append("foo", TestEnum.ONE).append("bar", TestEnum.TWO));
		testEquals(b1, b2);

		b2.set("A.f1.smo", new AMap<String,Object>().append("bar", TestEnum.TWO).append("foo", TestEnum.ONE));
		testEquals(b1, b2);

		b2.set("A.f1.smo", new AMap<String,Object>().append("bar", TestEnum.ONE).append("foo", TestEnum.TWO));
		testNotEquals(b1, b2);

		b1.clear();
		b1.set("A.f1.smo/add.foo", TestEnum.ONE);
		ps = b1.build();
		b1.set("A.f1.smo/add.foo", TestEnum.ONE);
		assertTrue(ps == b1.build());

		b1.set("A.f1.smo/add.foo", TestEnum.TWO);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Test system property defaults
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testStringDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1", "foo");
		assertEquals("foo", ps.getProperty("A.f1"));
		System.clearProperty("A.f1");
	}

	@Test
	public void testIntegerDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.i", "1");
		assertEquals(1, ps.getProperty("A.f1.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f1.i"));
		System.clearProperty("A.f1.i");
	}

	@Test
	public void testObjectDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.o", "123");
		assertEquals("123", ps.getProperty("A.f1.o"));
		assertInstanceOf(String.class, ps.getProperty("A.f1.o"));
		System.clearProperty("A.f1.o");
	}

	@Test
	public void testSetStringDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.ss", "['foo','bar']");
		assertObjectEquals("['bar','foo']", ps.getProperty("A.f1.ss"));
		System.clearProperty("A.f1.ss");
	}

	@Test
	public void testSetIntegerDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.si", "['2','1']");
		assertObjectEquals("[1,2]", ps.getProperty("A.f1.si"));
		System.clearProperty("A.f1.si");
	}

	@Test
	public void testListStringDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.ls", "['foo','bar']");
		assertObjectEquals("['foo','bar']", ps.getProperty("A.f1.ls"));
		System.clearProperty("A.f1.ls");
	}

	@Test
	public void testListIntegerDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.li", "['2','1']");
		assertObjectEquals("[2,1]", ps.getProperty("A.f1.li"));
		System.clearProperty("A.f1.li");
	}

	@Test
	public void testMapStringDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.sms", "{foo:'bar',baz:null}");
		assertObjectEquals("{foo:'bar'}", ps.getProperty("A.f1.sms"));
		System.clearProperty("A.f1.sms");
	}

	@Test
	public void testMapIntegerDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.smi", "{foo:'123',baz:null}");
		assertObjectEquals("{foo:123}", ps.getProperty("A.f1.smi"));
		System.clearProperty("A.f1.smi");
	}

	@Test
	public void testMapObjectDefault() {
		PropertyStore ps = PropertyStore.create().build();

		System.setProperty("A.f1.smo", "{foo:123,bar:'baz',qux:true,quux:null}");
		assertObjectEquals("{bar:'baz',foo:123,qux:true}", ps.getProperty("A.f1.smo"));
		System.clearProperty("A.f1.smo");
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Other tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testBuilderFromStore() {
		PropertyStoreBuilder b = PropertyStore.create();

		b.set("A.foo", "bar");
		PropertyStore ps1 = b.build();
		b = ps1.builder();
		assertObjectEquals("{A:{foo:'bar'}}", b.build());
	}

	@Test
	public void testSet() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.set(new ObjectMap().append("A.foo", "bar"));
		b.set(new ObjectMap().append("A.baz", "qux"));
		b.add(null);
		assertObjectEquals("{A:{baz:'qux'}}", b.build());
	}

	@Test
	public void testAdd() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.add(new ObjectMap().append("A.foo", "bar"));
		b.add(new ObjectMap().append("A.baz", "qux"));
		b.add(new ObjectMap().append("A.quux", null));
		b.add(null);
		assertObjectEquals("{A:{baz:'qux',foo:'bar'}}", b.build());
	}

	@Test
	public void testRemoveNotExisting() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.removeFrom("A.foo.ls", "bar");
		assertObjectEquals("{}", b.build());
	}

	@Test
	public void testAddToNull() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.addTo("A.foo.ls", null);
		assertObjectEquals("{}", b.build());
	}

	@Test
	public void testRemoveNull() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.removeFrom("A.foo.ss", null);
		assertObjectEquals("{}", b.build());
		b.removeFrom("A.foo.ls", null);
		assertObjectEquals("{}", b.build());
	}

	@Test
	public void testRemoveFromInvalidObjectList() {
		PropertyStoreBuilder b = PropertyStore.create();
		try {
			b.removeFrom("A.foo.ss", "[xxx]");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertTrue(e.getMessage().startsWith("Cannot remove value '[xxx]' (String) from property 'foo.ss' (Set<String>)."));
		}
		try {
			b.removeFrom("A.foo.ls", "[xxx]");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertTrue(e.getMessage().startsWith("Cannot remove value '[xxx]' (String) from property 'foo.ls' (List<String>)."));
		}
	}

	@Test
	public void testAddToInvalidObjectMap() {
		PropertyStoreBuilder b = PropertyStore.create();
		try {
			b.addTo("A.foo.sms", "{xxx}");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertTrue(e.getMessage().startsWith("Cannot add '{xxx}' (String) to property 'foo.sms' (Map<String,String>)."));
		}
		try {
			b.addTo("A.foo.sms", "xxx");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Cannot add 'xxx' (String) to property 'foo.sms' (Map<String,String>).", e.getMessage());
		}
		try {
			b.addTo("A.foo.sms", new StringBuilder("foo"));
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Cannot add 'foo' (StringBuilder) to property 'foo.sms' (Map<String,String>).", e.getMessage());
		}
	}

	@Test
	public void testGetNonExistent() {
		PropertyStore b = PropertyStore.create().set("A.foo", "bar").build();
		assertNull(b.getProperty("A.baz"));
		assertNull(b.getProperty("B.foo"));
	}

	@Test
	public void testHashCodes() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.set("A.foo", "bar").set("B.foo", "bar");
		PropertyStore ps = b.build();

		assertEquals(ps.hashCode("A","B","C",null),ps.hashCode("A","B","C",null));
		assertNotEquals(ps.hashCode("A"),ps.hashCode("B"));
		assertNotEquals(ps.hashCode("A","B"),ps.hashCode("B","A"));
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEquals() {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = b.build();
		assertFalse(ps.equals("foo"));
	}

	@Test
	public void testEqualsByGroups() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.set("A.foo", "bar").set("B.foo", "bar").set("D.foo", "bar");
		PropertyStore ps1 = b.build();
		b.remove("A.foo").set("C.foo", "bar").set("D.foo", "baz");
		PropertyStore ps2 = b.build();

		assertTrue(ps1.equals(ps1, null, null));
		assertTrue(ps1.equals(ps2, null, null));

		assertTrue(ps1.equals(ps2, "B"));
		assertTrue(ps1.equals(ps2, "X"));
		assertFalse(ps1.equals(ps2, "A"));
		assertFalse(ps1.equals(ps2, "C"));
		assertFalse(ps1.equals(ps2, "D"));
		assertFalse(ps1.equals(ps2, "B", "D"));
	}

	@Test
	public void testKeySet() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.set("A.foo", "bar").set("B.foo", "bar").set("D.foo", "bar");
		PropertyStore ps = b.build();

		assertObjectEquals("[]", ps.getPropertyKeys(null));
		assertObjectEquals("['foo']", ps.getPropertyKeys("A"));
		assertObjectEquals("[]", ps.getPropertyKeys("C"));
	}

	@Test
	public void testToMutable() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.set("A.foo.s", "bar");
		b.set("A.foo.b", true);
		b.set("A.foo.i", 123);
		b.set("A.foo.c", String.class);
		b.set("A.foo.o", "bar");
		b.set("A.foo.ss", "['bar']");
		b.set("A.foo.si", "[123]");
		b.set("A.foo.sc/add", String.class);
		b.set("A.foo.ls", "['bar']");
		b.set("A.foo.li", "[123]");
		b.set("A.foo.lc/add", String.class);
		b.set("A.foo.lo/add", StringBuilder.class);
		b.set("A.foo.sms", "{foo:'bar'}");
		b.set("A.foo.smi", "{foo:123}");
		b.set("A.foo.smc/add.foo", String.class);
		b.set("A.foo.smo/add.foo", StringBuilder.class);
		PropertyStore ps = b.build();

		b = ps.builder();
		ps = b.build();

		assertObjectEquals("{A:{'foo.b':true,'foo.c':'java.lang.String','foo.i':123,'foo.lc':['java.lang.String'],'foo.li':[123],'foo.lo':['java.lang.StringBuilder'],'foo.ls':['bar'],'foo.o':'bar','foo.s':'bar','foo.sc':['java.lang.String'],'foo.si':[123],'foo.smc':{foo:'java.lang.String'},'foo.smi':{foo:123},'foo.smo':{foo:'java.lang.StringBuilder'},'foo.sms':{foo:'bar'},'foo.ss':['bar']}}", ps);
	}

	@Test
	public void testToString() {
		PropertyStore p = PropertyStore.create().build();
		assertEquals("{}", p.toString());
	}

	@Test
	public void testNoneOnList() {
		PropertyStoreBuilder psb = PropertyStore.create();

		psb.set("A.foo.ls", "['foo','bar']");
		psb.set("A.foo.ls", "NONE");
		assertEquals("{}", psb.build().toString());
	}

	@Test
	public void testNoneOnSet() {
		PropertyStoreBuilder psb = PropertyStore.create();

		psb.set("A.foo.ss", "['foo','bar']");
		psb.set("A.foo.ss", "NONE");
		assertEquals("{}", psb.build().toString());
	}

	@Test
	public void testInheritOnList() {
		PropertyStoreBuilder psb = PropertyStore.create();

		psb.set("A.foo.ls", "['foo','bar']");
		psb.set("A.foo.ls", "['baz','INHERIT','qux']");
		assertEquals("{A:{'foo.ls':['baz','foo','bar','qux']}}", psb.build().toString());
	}

	@Test
	public void testInheritOnSet() {
		PropertyStoreBuilder psb = PropertyStore.create();

		psb.set("A.foo.ls", "['foo','bar']");
		psb.set("A.foo.ls", "['baz','INHERIT','qux']");
		assertEquals("{A:{'foo.ls':['baz','foo','bar','qux']}}", psb.build().toString());
	}

	@Test
	public void testIndexedValuesOnList() {
		PropertyStoreBuilder psb = PropertyStore.create();

		psb.set("A.foo.ls", "['foo','bar']");
		psb.set("A.foo.ls", new String[]{"INHERIT", "[0]:baz"});
		assertEquals("{A:{'foo.ls':['baz','foo','bar']}}", psb.build().toString());
		psb.set("A.foo.ls", new String[]{"INHERIT", "[1]:qux"});
		assertEquals("{A:{'foo.ls':['baz','qux','foo','bar']}}", psb.build().toString());
		psb.set("A.foo.ls", new String[]{"INHERIT", "[10]:quux"});
		assertEquals("{A:{'foo.ls':['baz','qux','foo','bar','quux']}}", psb.build().toString());
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------

	private void testError(PropertyStoreBuilder b, String key, Object val, String msg) {
		try {
			b.set(key, val);
			fail("Exception expected.");
		} catch (ConfigException e) {
			if ("xxx".equals(msg))
				System.err.println(e.getLocalizedMessage());
			assertEquals(msg, e.getMessage());
		}
	}

	private void testEquals(PropertyStoreBuilder b1, PropertyStoreBuilder b2) {
		assertTrue(b1.build() == b2.build());
	}

	private void testNotEquals(PropertyStoreBuilder b1, PropertyStoreBuilder b2) {
		PropertyStore p1 = b1.build(), p2 = b2.build();
		assertTrue(p1 != p2);
		assertTrue(p1.hashCode() != p2.hashCode());
	}

	public static enum TestEnum {
		ONE, TWO, THREE;
	}
}