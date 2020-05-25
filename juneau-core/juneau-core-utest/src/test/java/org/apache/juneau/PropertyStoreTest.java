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
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.json.annotation.*;
import org.junit.*;


/**
 * Test the PropertyStore class.
 */
@FixMethodOrder(NAME_ASCENDING)
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
		testError(b, "A.f1/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
		testError(b, "A.f1/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
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
		b.set("A.f4.s", OMap.ofJson("{foo:'bar'}"));
		b.set("A.f5.s", OList.ofJson("[1,2]"));
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
		b.set("A.f4", OMap.ofJson("{foo:'bar'}"));
		b.set("A.f5", OList.ofJson("[1,2]"));
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

		testError(b, "A.f1/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
		testError(b, "A.f1/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
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

		testError(b, "A.f1.b/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.b'.");
		testError(b, "A.f1.b/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.b'.");
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

		testError(b, "A.f1.i/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.i'.");
		testError(b, "A.f1.i/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.i'.");
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

		testError(b, "A.f1.c/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.c'.");
		testError(b, "A.f1.c/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.c'.");
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

		testError(b, "A.f1.o/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.o'.");
		testError(b, "A.f1.o/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.o'.");
		testError(b, "A.f1.o/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.o' (Object).");
	}

	@Test
	public void testSetString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.ss", AList.of("foo", "bar", "bar", null));
		b.set("A.f2.ss", AList.of(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ss", AList.of(new StringBuilder("foo"), null));
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
		b.set("A.f1.ss/add", AList.of("foo", "bar", "baz"));
		b.set("A.f1.ss/add", AList.of("qux"));
		b.addTo("A.f1.ss", AList.of("quux"));
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}", b.build());
		b.set("A.f1.ss/remove", AList.of("foo", "bar"));
		b.set("A.f1.ss/remove", AList.of("qux"));
		b.removeFrom("A.f1.ss", AList.of("quux"));
		assertObjectEquals("{A:{'f1.ss':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ss/add", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ss/add", new String[]{"qux"});
		b.addTo("A.f1.ss", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}", b.build());

		b.addTo("A.f1.ss", new String[]{"quuux"});
		b.addTo("A.f1.ss", new String[]{"quuuux"});
		assertObjectEquals("{A:{'f1.ss':['bar','baz','foo','quuuux','quuux','quux','qux']}}", b.build());
		b.set("A.f1.ss/remove", new String[]{"quuux", "quuuux"});

		b.set("A.f1.ss/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ss/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ss", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ss':['baz']}}", b.build());

		b.set("A.f1.ss", null);
		assertObjectEquals("{}", b.build());
	}

	@Test
	public void testSetInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.si", AList.of(3, 2, 1, null));
		b.set("A.f2.si", AList.of(123, "456", null));
		b.set("A.f3.si", AList.of(new StringBuilder("123"), null));
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
		b.set("A.f1.si/add", AList.of("3", "2", "1"));
		b.set("A.f1.si/add", AList.of("4"));
		b.addTo("A.f1.si", AList.of("5"));
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", AList.of("1", "2"));
		b.set("A.f1.si/remove", AList.of("3"));
		b.removeFrom("A.f1.si", AList.of("4"));
		assertObjectEquals("{A:{'f1.si':[5]}}", b.build());

		b.clear();
		b.set("A.f1.si/add", AList.of(1, 2, 3));
		b.set("A.f1.si/add", AList.of(4));
		b.addTo("A.f1.si", AList.of(5));
		assertObjectEquals("{A:{'f1.si':[1,2,3,4,5]}}", b.build());
		b.set("A.f1.si/remove", AList.of(1, 2));
		b.set("A.f1.si/remove", AList.of(3));
		b.removeFrom("A.f1.si", AList.of(4));
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
	}

	@Test
	public void testSetClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.sc", AList.of(String.class, Integer.class, null));
		b.set("A.f2.sc", AList.of(String.class, Integer.class, null));
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
		b.set("A.f1.sc/add", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/add", AList.of(Map.class));
		b.addTo("A.f1.sc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}", b.build());
		b.set("A.f1.sc/remove", AList.of(Integer.class, String.class));
		b.removeFrom("A.f1.sc", AList.of());
		b.removeFrom("A.f1.sc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.sc/add", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/add", AList.of(Map.class));
		b.addTo("A.f1.sc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}", b.build());
		b.set("A.f1.sc/remove", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/remove", AList.of());
		b.removeFrom("A.f1.sc", AList.of(List.class));
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
	}

	@Test
	public void testListString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.ls", AList.of("foo", "bar", "bar", null));
		b.set("A.f2.ls", AList.of(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ls", AList.of(new StringBuilder("foo"), null));
		b.set("A.f4.ls", "['foo',123,true]");
		b.set("A.f5.ls", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.ls':['foo','bar'],'f2.ls':['123','true','ONE'],'f3.ls':['foo'],'f4.ls':['foo','123','true']}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f3.ls"));
		assertInstanceOf(List.class, ps.getProperty("A.f4.ls"));

		b.clear();
		b.set("A.f1.ls/prepend", "foo");
		assertObjectEquals("{A:{'f1.ls':['foo']}}", b.build());
		b.set("A.f1.ls/remove", "foo");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.ls/prepend", "['foo','bar','baz']");
		b.set("A.f1.ls/prepend", "qux");
		b.prependTo("A.f1.ls","quux");
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());
		b.set("A.f1.ls/remove", "['foo','bar']");
		b.set("A.f1.ls/remove", "qux");
		b.removeFrom("A.f1.ls", "quux");
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ls/prepend", AList.of("foo", "bar", "baz"));
		b.set("A.f1.ls/prepend", AList.of("qux"));
		b.prependTo("A.f1.ls", AList.of("quux"));
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());
		b.set("A.f1.ls/remove", AList.of("foo", "bar"));
		b.set("A.f1.ls/remove", AList.of("qux"));
		b.removeFrom("A.f1.ls", AList.of("quux"));
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.clear();
		b.set("A.f1.ls/prepend", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ls/prepend", new String[]{"qux"});
		b.prependTo("A.f1.ls", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}", b.build());

		b.appendTo("A.f1.ls", new String[]{"q1x", "q2x"});
		b.appendTo("A.f1.ls", "q3x");
		b.prependTo("A.f1.ls", new String[]{"q4x", "q5x"});
		b.prependTo("A.f1.ls", "q6x");
		assertObjectEquals("{A:{'f1.ls':['q6x','q4x','q5x','quux','qux','foo','bar','baz','q1x','q2x','q3x']}}", b.build());
		b.set("A.f1.ls/remove", new String[]{"q1x","q2x","q3x","q4x","q5x","q6x"});

		b.set("A.f1.ls/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ls/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ls", new String[]{"quux"});
		assertObjectEquals("{A:{'f1.ls':['baz']}}", b.build());

		b.set("A.f1.ls", null);
		assertObjectEquals("{}", b.build());

//		b.clear();
//		b.set("A.f1.ls/add", "['foo','bar','baz']");
//		b.set("A.f1.ls/add.10", "qux");
//		b.set("A.f1.ls/add.1", "quux");
//		b.set("A.f1.ls/add.0", "quuux");
//		b.set("A.f1.ls/add.-10", "quuuux");
//		assertObjectEquals("{A:{'f1.ls':['quuuux','quuux','foo','quux','bar','baz','qux']}}", b.build());
//		b.set("A.f1.ls/add.1", "['1','2']");
//		assertObjectEquals("{A:{'f1.ls':['quuuux','1','2','quuux','foo','quux','bar','baz','qux']}}", b.build());
//
//		testError(b, "A.f1.ls/add.foo", "foo", "Invalid argument 'foo' on add command for property 'f1.ls' (List<String>)");
//		try {
//			b.addTo("A.f1.ls", "foo", "bar");
//			fail("Exception expected.");
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.ls' (List<String>)", e.getMessage());
//		}
	}

	@Test
	public void testListInteger() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.li", AList.of(1, 2, 3, null));
		b.set("A.f2.li", AList.of(123, "456", null));
		b.set("A.f3.li", AList.of(new StringBuilder("123"), null));
		b.set("A.f4.li", "[1,2,3]");
		b.set("A.f5.li", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.li':[1,2,3],'f2.li':[123,456],'f3.li':[123],'f4.li':[1,2,3]}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f3.li"));
		assertInstanceOf(List.class, ps.getProperty("A.f4.li"));

		b.clear();
		b.set("A.f1.li/prepend", "123");
		assertObjectEquals("{A:{'f1.li':[123]}}", b.build());
		b.set("A.f1.li/remove", "123");
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", "['1','2','3']");
		b.set("A.f1.li/prepend", "4");
		b.prependTo("A.f1.li", "5");
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", "['1','2']");
		b.set("A.f1.li/remove", "3");
		b.removeFrom("A.f1.li", "4");
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", AList.of("1", "2", "3"));
		b.set("A.f1.li/prepend", AList.of("4"));
		b.prependTo("A.f1.li", AList.of("5"));
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", AList.of("1", "2"));
		b.set("A.f1.li/remove", AList.of("3"));
		b.removeFrom("A.f1.li", AList.of("4"));
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", AList.of(1, 2, 3));
		b.set("A.f1.li/prepend", AList.of(4));
		b.prependTo("A.f1.li", AList.of(5));
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", AList.of(1, 2));
		b.set("A.f1.li/remove", AList.of(3));
		b.removeFrom("A.f1.li", AList.of(4));
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", new String[]{"1", "2", "3"});
		b.set("A.f1.li/prepend", new String[]{"4"});
		b.prependTo("A.f1.li", new String[]{"5"});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new String[]{"1", "2"});
		b.set("A.f1.li/remove", new String[]{"3"});
		b.removeFrom("A.f1.li", new String[]{"4"});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", new Integer[]{1, 2, 3});
		b.set("A.f1.li/prepend", new Integer[]{4});
		b.prependTo("A.f1.li", new Integer[]{5});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new Integer[]{1, 2});
		b.set("A.f1.li/remove", new Integer[]{3});
		b.removeFrom("A.f1.li", new Integer[]{4});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.clear();
		b.set("A.f1.li/prepend", new int[]{1, 2, 3});
		b.set("A.f1.li/prepend", new int[]{4});
		b.prependTo("A.f1.li", new int[]{5});
		assertObjectEquals("{A:{'f1.li':[5,4,1,2,3]}}", b.build());
		b.set("A.f1.li/remove", new int[]{1, 2});
		b.set("A.f1.li/remove", new int[]{3});
		b.removeFrom("A.f1.li", new int[]{4});
		assertObjectEquals("{A:{'f1.li':[5]}}", b.build());

		b.set("A.f1.li", null);
		assertObjectEquals("{}", b.build());

//		b.clear();
//		b.set("A.f1.ls/add", "['1','2','3']");
//		b.set("A.f1.ls/add.10", "4");
//		b.set("A.f1.ls/add.1", "5");
//		b.set("A.f1.ls/add.0", "6");
//		b.set("A.f1.ls/add.-10", "7");
//		assertObjectEquals("{A:{'f1.ls':['7','6','1','5','2','3','4']}}", b.build());
//		b.set("A.f1.ls/add.1", "['8','9']");
//		assertObjectEquals("{A:{'f1.ls':['7','8','9','6','1','5','2','3','4']}}", b.build());
//
//		testError(b, "A.f1.li/add.123", "foo", "Cannot add value 'foo' (String) to property 'f1.li' (List<Integer>).  Value 'foo' (String) cannot be converted to an Integer.");
//		try {
//			b.addTo("A.f1.li", "foo", "bar");
//			fail("Exception expected.");
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.li' (List<Integer>)", e.getMessage());
//		}
	}

	@Test
	public void testListClass() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.lc", AList.of(String.class, Integer.class, null));
		b.set("A.f2.lc", AList.of(String.class, Integer.class, null));
		b.set("A.f3.lc", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.lc':['java.lang.String','java.lang.Integer'],'f2.lc':['java.lang.String','java.lang.Integer']}}", ps);
		assertInstanceOf(List.class, ps.getProperty("A.f1.lc"));
		assertInstanceOf(List.class, ps.getProperty("A.f2.lc"));

		b.clear();
		b.set("A.f1.lc/prepend", Integer.class);
		b.prependTo("A.f1.lc", String.class);
		assertObjectEquals("{A:{'f1.lc':['java.lang.String','java.lang.Integer']}}", b.build());
		b.set("A.f1.lc/remove", Integer.class);
		assertObjectEquals("{A:{'f1.lc':['java.lang.String']}}", b.build());

		b.clear();
		testError(b, "A.f1.lc/prepend", "['java.lang.Integer']", "Cannot prepend value '[\\'java.lang.Integer\\']' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");
		testError(b, "A.f1.lc/prepend", "java.lang.Integer", "Cannot prepend value 'java.lang.Integer' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");

		b.clear();
		b.set("A.f1.lc/prepend", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/prepend", AList.of(Map.class));
		b.prependTo("A.f1.lc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", AList.of(Integer.class, String.class));
		b.removeFrom("A.f1.lc", AList.of());
		b.removeFrom("A.f1.lc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.lc/prepend", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/prepend", AList.of(Map.class));
		b.prependTo("A.f1.lc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/remove", AList.of());
		b.removeFrom("A.f1.lc", AList.of(List.class));
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.clear();
		b.set("A.f1.lc/prepend", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/prepend", new Class<?>[]{Map.class});
		b.prependTo("A.f1.lc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}", b.build());
		b.set("A.f1.lc/remove", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lc", new Class<?>[]{List.class});
		assertObjectEquals("{A:{'f1.lc':['java.util.Map']}}", b.build());

		b.set("A.f1.lc", null);
		assertObjectEquals("{}", b.build());

		b.clear();
//		b.set("A.f1.lc/add", String.class);
//		b.set("A.f1.lc/add.10", Integer.class);
//		b.set("A.f1.lc/add.1", Map.class);
//		b.set("A.f1.lc/add.0", List.class);
//		b.set("A.f1.lc/add.-10", Object.class);
//		assertObjectEquals("{A:{'f1.lc':['java.lang.Object','java.util.List','java.lang.String','java.util.Map','java.lang.Integer']}}", b.build());

		testError(b, "A.f1.lc/prepend.123", "foo", "Cannot prepend value 'foo' (String) to property 'f1.lc' (List<Class>).  Value 'foo' (String) cannot be converted to a Class.");
//		try {
//			b.prependTo("A.f1.lc", "foo", "bar");
//			fail("Exception expected.");
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.lc' (List<Class>)", e.getMessage());
//		}
	}

	@Test
	public void testListObject() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.lo", AList.of(StringBuilder.class, null));
		b.set("A.f2.lo", AList.of(123, true, new StringBuilder(123), StringBuilder.class, null));
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
		b.set("A.f1.lo/prepend", 1);
		b.prependTo("A.f1.lo", 2);
		assertObjectEquals("{A:{'f1.lo':[2,1]}}", b.build());
		b.set("A.f1.lo/remove", 1);
		assertObjectEquals("{A:{'f1.lo':[2]}}", b.build());

		b.clear();
		b.set("A.f1.lo/prepend", AList.of(StringBuilder.class));
		b.set("A.f1.lo/prepend", AList.of(HashMap.class));
		b.prependTo("A.f1.lo", AList.of(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", AList.of(HashMap.class));
		b.removeFrom("A.f1.lo", AList.of());
		b.removeFrom("A.f1.lo", AList.of(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.clear();
		b.set("A.f1.lo/prepend", AList.of(StringBuilder.class));
		b.set("A.f1.lo/prepend", AList.of(HashMap.class));
		b.prependTo("A.f1.lo", AList.of(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", AList.of(HashMap.class));
		b.set("A.f1.lo/remove", AList.of());
		b.removeFrom("A.f1.lo", AList.of(LinkedList.class));
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.clear();
		b.set("A.f1.lo/prepend", new Class<?>[]{StringBuilder.class});
		b.set("A.f1.lo/prepend", new Class<?>[]{HashMap.class});
		b.prependTo("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObjectEquals("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}", b.build());
		b.set("A.f1.lo/remove", new Class<?>[]{HashMap.class});
		b.set("A.f1.lo/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObjectEquals("{A:{'f1.lo':['java.lang.StringBuilder']}}", b.build());

		b.set("A.f1.lo", null);
		assertObjectEquals("{}", b.build());

//		b.clear();
//		b.set("A.f1.lo/add", StringBuilder.class);
//		b.set("A.f1.lo/add.10", HashMap.class);
//		b.set("A.f1.lo/add.1", LinkedList.class);
//		b.set("A.f1.lo/add.0", TestEnum.ONE);
//		b.set("A.f1.lo/add.-10", TestEnum.TWO);
//		assertObjectEquals("{A:{'f1.lo':['TWO','ONE','java.lang.StringBuilder','java.util.LinkedList','java.util.HashMap']}}", b.build());
//
//		try {
//			b.addTo("A.f1.lo", "foo", "bar");
//			fail("Exception expected.");
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.lo' (List<Object>)", e.getMessage());
//		}
	}

	@Test
	public void testMapString() throws Exception {
		PropertyStoreBuilder b = PropertyStore.create();
		PropertyStore ps = null;
		b.set("A.f1.sms", AMap.of("foo","bar","baz","qux","quux",null,null,null));
		b.set("A.f2.sms", AMap.of("foo",(Object)123,"bar",true,"baz",TestEnum.ONE,"qux",null));
		b.set("A.f3.sms", AMap.of("foo",new StringBuilder("bar"),"baz",null));
		b.set("A.f4.sms", "{foo:'bar',baz:123,qux:true}");
		b.set("A.f5.sms", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.sms':{baz:'qux',foo:'bar'},'f2.sms':{bar:'true',baz:'ONE',foo:'123'},'f3.sms':{foo:'bar'},'f4.sms':{baz:'123',foo:'bar',qux:'true'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.sms"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.sms"));

		b.clear();
		b.set("A.f1.sms/put", "{foo:'bar'}");
		assertObjectEquals("{A:{'f1.sms':{foo:'bar'}}}", b.build());

		b.clear();
		b.set("A.f1.sms/put.foo", "bar");
		assertObjectEquals("{A:{'f1.sms':{foo:'bar'}}}", b.build());
		b.set("A.f1.sms/put.foo", null);
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
		b.set("A.f1.smi", AMap.of("foo","1","baz","2","quux",null,null,null));
		b.set("A.f2.smi", AMap.of("foo",123,"bar","456","baz",null));
		b.set("A.f3.smi", AMap.of("foo",new StringBuilder("123"),"baz",null));
		b.set("A.f4.smi", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smi", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smi':{baz:2,foo:1},'f2.smi':{bar:456,foo:123},'f3.smi':{foo:123},'f4.smi':{baz:456,foo:123}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.smi"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.smi"));

		b.clear();
		b.set("A.f1.smi/put", "{foo:'123'}");
		assertObjectEquals("{A:{'f1.smi':{foo:123}}}", b.build());

		b.clear();
		b.set("A.f1.smi/put.foo", "123");
		assertObjectEquals("{A:{'f1.smi':{foo:123}}}", b.build());
		b.set("A.f1.smi/put.foo", null);
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
		b.set("A.f1.smc", AMap.of("foo",String.class,"baz",Integer.class,"quux",null,null,null));
		b.set("A.f2.smc", AMap.of("foo",String.class,"bar",Integer.class,"baz",null));
		b.set("A.f3.smc", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smc':{baz:'java.lang.Integer',foo:'java.lang.String'},'f2.smc':{bar:'java.lang.Integer',foo:'java.lang.String'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smc"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smc"));
		assertInstanceOf(Class.class, ((Map<?,?>)ps.getProperty("A.f1.smc")).values().iterator().next());
		assertInstanceOf(Class.class, ((Map<?,?>)ps.getProperty("A.f2.smc")).values().iterator().next());

		b.clear();
		b.set("A.f1.smc/put.foo", String.class);
		assertObjectEquals("{A:{'f1.smc':{foo:'java.lang.String'}}}", b.build());
		b.set("A.f1.smc/put.foo", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smc", null);
		assertObjectEquals("{}", b.build());

		b.clear();
		b.set("A.f1.smc/put.foo", String.class);
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
		b.set("A.f1.smo", AMap.of("foo","1","baz","2","quux",null,null,null));
		b.set("A.f2.smo", AMap.of("foo",123,"bar",StringBuilder.class,"qux",null));
		b.set("A.f3.smo", AMap.of("foo",new StringBuilder("123"),"baz",null));
		b.set("A.f4.smo", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smo", null);
		ps = b.build();
		assertObjectEquals("{A:{'f1.smo':{baz:'2',foo:'1'},'f2.smo':{bar:'java.lang.StringBuilder',foo:123},'f3.smo':{foo:'123'},'f4.smo':{baz:456,foo:'123'}}}", ps);
		assertInstanceOf(Map.class, ps.getProperty("A.f1.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f2.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f3.smo"));
		assertInstanceOf(Map.class, ps.getProperty("A.f4.smo"));

		b.clear();
		b.set("A.f1.smo/put", "{foo:'123'}");
		assertObjectEquals("{A:{'f1.smo':{foo:'123'}}}", b.build());

		b.clear();
		b.set("A.f1.smo/put.foo", "123");
		assertObjectEquals("{A:{'f1.smo':{foo:'123'}}}", b.build());
		b.set("A.f1.smo/put.foo", null);
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

		b1.set("A.f1.ss", AList.of("foo", "bar"));
		b2.set("A.f1.ss", new String[]{"foo","bar"});
		testEquals(b1, b2);

		b2.set("A.f1.ss", AList.of(new StringBuilder("bar"), new StringBuilder("foo")));
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

		b1.set("A.f1.si", AList.of("1", "2"));
		b2.set("A.f1.si", new String[]{"1","2"});
		testEquals(b1, b2);

		b2.set("A.f1.si", AList.of(new StringBuilder("2"), 1));
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

		b1.set("A.f1.sc", AList.of(String.class, Integer.class));
		b2.set("A.f1.sc", new Class<?>[]{Integer.class,String.class});
		testEquals(b1, b2);

		b2.set("A.f1.sc", AList.of(Integer.class, String.class));
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

		b1.set("A.f1.ls", AList.of("foo", "bar"));
		b2.set("A.f1.ls", new String[]{"foo","bar"});
		testEquals(b1, b2);

		b2.set("A.f1.ls", AList.of(new StringBuilder("foo"), new StringBuilder("bar")));
		testEquals(b1, b2);

		b2.set("A.f1.ls", AList.of(new StringBuilder("bar"), new StringBuilder("foo")));
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

		b1.set("A.f1.li", AList.of("1", "2"));
		b2.set("A.f1.li", new String[]{"1","2"});
		testEquals(b1, b2);

		b2.set("A.f1.li", new String[]{"2","1"});
		testNotEquals(b1, b2);

		b2.set("A.f1.li", new int[]{1,2});
		testEquals(b1, b2);

		b2.set("A.f1.li", AList.of(new StringBuilder("2"), 1));
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

		b1.set("A.f1.lc", AList.of(String.class, Integer.class));

		b2.set("A.f1.lc", new Class<?>[]{String.class,Integer.class});
		testEquals(b1, b2);

		b2.set("A.f1.lc", new Class<?>[]{Integer.class,String.class});
		testNotEquals(b1, b2);

		b2.set("A.f1.lc", AList.of(String.class, Integer.class));
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

		b1.set("A.f1.lo", AList.of("foo", 123, true, TestEnum.ONE));

		b2.set("A.f1.lo", AList.of("foo", 123, true, TestEnum.ONE));
		testEquals(b1, b2);

		b2.set("A.f1.lo", AList.of(123, true, TestEnum.ONE, "foo"));
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

		b1.set("A.f1.sms", AMap.of("foo","123","bar","true","baz",null,null,null));
		b2.set("A.f1.sms", AMap.of("foo",123,"bar",true,"baz",null,null,null));
		testEquals(b1, b2);

		b2.set("A.f1.sms", AMap.of("foo",new StringBuilder("123"),"bar",new StringBuilder("true")));
		testEquals(b1, b2);

		b2.set("A.f1.sms", AMap.of("bar",new StringBuilder("true"),"foo",new StringBuilder("123")));
		testEquals(b1, b2);

		b2.set("A.f1.sms", AMap.of("bar",false,"foo",new StringBuilder("123")));
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

		b1.set("A.f1.smi", AMap.of("foo",123,"bar",456,"baz",null,null,null));
		b2.set("A.f1.smi", AMap.of("foo",123,"bar","456","baz",null,null,null));
		testEquals(b1, b2);

		b2.set("A.f1.smi", AMap.of("foo",new StringBuilder("123"),"bar",new StringBuilder("456")));
		testEquals(b1, b2);

		b2.set("A.f1.smi", AMap.of("bar",new StringBuilder("456"),"foo",new StringBuilder("123")));
		testEquals(b1, b2);

		b2.set("A.f1.smi", AMap.of("bar","457","foo",new StringBuilder("123")));
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

		b1.set("A.f1.smc", AMap.of("foo",String.class,"bar",Integer.class,"baz",null,null,null));
		b2.set("A.f1.smc", AMap.of("foo",String.class,"bar",Integer.class,"baz",null,null,null));
		testEquals(b1, b2);

		b2.set("A.f1.smc", AMap.of("foo",String.class,"bar",Integer.class));
		testEquals(b1, b2);

		b2.set("A.f1.smc", AMap.of("bar",Integer.class,"foo",String.class));
		testEquals(b1, b2);

		b2.set("A.f1.smc", AMap.of("bar",Integer.class,"foo",StringBuilder.class));
		testNotEquals(b1, b2);

		b1.clear();
		b1.set("A.f1.smc/put.foo", Integer.class);
		ps = b1.build();
		b1.set("A.f1.smc/put.foo", Integer.class);
		assertTrue(ps == b1.build());

		b1.set("A.f1.smc/put.foo", String.class);
		assertTrue(ps != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapObjectHash() throws Exception {
		PropertyStoreBuilder b1 = PropertyStore.create(), b2 = PropertyStore.create();
		PropertyStore ps = null;

		b1.set("A.f1.smo", AMap.of("foo",TestEnum.ONE,"bar",TestEnum.TWO,"baz",null,null,null));
		b2.set("A.f1.smo", AMap.of("foo",TestEnum.ONE,"bar",TestEnum.TWO,"baz",null,null,null));
		testEquals(b1, b2);

		b2.set("A.f1.smo", AMap.of("foo",TestEnum.ONE,"bar",TestEnum.TWO));
		testEquals(b1, b2);

		b2.set("A.f1.smo", AMap.of("bar",TestEnum.TWO,"foo",TestEnum.ONE));
		testEquals(b1, b2);

		b2.set("A.f1.smo", AMap.of("bar",TestEnum.ONE,"foo",TestEnum.TWO));
		testNotEquals(b1, b2);

		b1.clear();
		b1.set("A.f1.smo/put.foo", TestEnum.ONE);
		ps = b1.build();
		b1.set("A.f1.smo/put.foo", TestEnum.ONE);
		assertTrue(ps == b1.build());

		b1.set("A.f1.smo/put.foo", TestEnum.TWO);
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

		System.setProperty("A.f1", "foo");
		PropertyStore ps = PropertyStore.create().build();
		assertEquals("foo", ps.getProperty("A.f1"));

		System.clearProperty("A.f1");
		ps = PropertyStore.create().build();
		assertNull(ps.getProperty("A.f1"));
	}

	@Test
	public void testIntegerDefault() {

		System.setProperty("A.f1.i", "1");
		PropertyStore ps = PropertyStore.create().build();
		assertEquals(1, ps.getProperty("A.f1.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f1.i"));

		System.clearProperty("A.f1.i");
		System.setProperty("A.f1", "1");
		ps = PropertyStore.create().build();
		assertEquals(1, ps.getProperty("A.f1.i"));
		assertInstanceOf(Integer.class, ps.getProperty("A.f1.i"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.i"));
	}

	@Test
	public void testObjectDefault() {

		System.setProperty("A.f1.o", "123");
		PropertyStore ps = PropertyStore.create().build();
		assertEquals("123", ps.getProperty("A.f1.o"));
		assertInstanceOf(String.class, ps.getProperty("A.f1.o"));

		System.clearProperty("A.f1.o");
		System.setProperty("A.f1", "123");
		assertEquals("123", ps.getProperty("A.f1.o"));
		assertInstanceOf(String.class, ps.getProperty("A.f1.o"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.o"));
	}

	@Test
	public void testSetStringDefault() {

		System.setProperty("A.f1.ss", "['foo','bar']");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("['bar','foo']", ps.getProperty("A.f1.ss"));

		System.clearProperty("A.f1.ss");
		System.setProperty("A.f1", "['foo','bar']");
		ps = PropertyStore.create().build();
		assertObjectEquals("['bar','foo']", ps.getProperty("A.f1.ss"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.ss"));
	}

	@Test
	public void testSetIntegerDefault() {

		System.setProperty("A.f1.si", "['2','1']");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("[1,2]", ps.getProperty("A.f1.si"));

		System.clearProperty("A.f1.si");
		System.setProperty("A.f1", "['2','1']");
		ps = PropertyStore.create().build();
		assertObjectEquals("[1,2]", ps.getProperty("A.f1.si"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.si"));
	}

	@Test
	public void testListStringDefault() {

		System.setProperty("A.f1.ls", "['foo','bar']");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("['foo','bar']", ps.getProperty("A.f1.ls"));

		System.clearProperty("A.f1.ls");
		System.setProperty("A.f1", "['foo','bar']");
		ps = PropertyStore.create().build();
		assertObjectEquals("['foo','bar']", ps.getProperty("A.f1.ls"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.ls"));
	}

	@Test
	public void testListIntegerDefault() {

		System.setProperty("A.f1.li", "['2','1']");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("[2,1]", ps.getProperty("A.f1.li"));

		System.clearProperty("A.f1.li");
		System.setProperty("A.f1", "['2','1']");
		ps = PropertyStore.create().build();
		assertObjectEquals("[2,1]", ps.getProperty("A.f1.li"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.li"));
	}

	@Test
	public void testMapStringDefault() {

		System.setProperty("A.f1.sms", "{foo:'bar',baz:null}");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("{foo:'bar'}", ps.getProperty("A.f1.sms"));

		System.clearProperty("A.f1.sms");
		System.setProperty("A.f1", "{foo:'bar',baz:null}");
		ps = PropertyStore.create().build();
		assertObjectEquals("{foo:'bar'}", ps.getProperty("A.f1.sms"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.sms"));
	}

	@Test
	public void testMapIntegerDefault() {

		System.setProperty("A.f1.smi", "{foo:'123',baz:null}");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("{foo:123}", ps.getProperty("A.f1.smi"));

		System.clearProperty("A.f1.smi");
		System.setProperty("A.f1", "{foo:'123',baz:null}");
		ps = PropertyStore.create().build();
		assertObjectEquals("{foo:123}", ps.getProperty("A.f1.smi"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.smi"));
	}

	@Test
	public void testMapObjectDefault() {

		System.setProperty("A.f1.smo", "{foo:123,bar:'baz',qux:true,quux:null}");
		PropertyStore ps = PropertyStore.create().build();
		assertObjectEquals("{bar:'baz',foo:123,qux:true}", ps.getProperty("A.f1.smo"));

		System.clearProperty("A.f1.smo");
		System.setProperty("A.f1", "{foo:123,bar:'baz',qux:true,quux:null}");
		ps = PropertyStore.create().build();
		assertObjectEquals("{bar:'baz',foo:123,qux:true}", ps.getProperty("A.f1.smo"));

		System.clearProperty("A.f1");
		assertNull(ps.getProperty("A.f1.smo"));
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
		b.set(OMap.of("A.foo", "bar"));
		b.set(OMap.of("A.baz", "qux"));
		b.add(null);
		assertObjectEquals("{A:{baz:'qux'}}", b.build());
	}

	@Test
	public void testAdd() {
		PropertyStoreBuilder b = PropertyStore.create();
		b.add(OMap.of("A.foo", "bar"));
		b.add(OMap.of("A.baz", "qux"));
		b.add(OMap.of("A.quux", null));
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
		b.appendTo("A.foo.ls", null);
		b.prependTo("A.foo.ls", null);
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
	public void testRemoveFromInvalidListOfObjects() {
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
	public void testAddToInvalidMapOfObjects() {
		PropertyStoreBuilder b = PropertyStore.create();
		try {
			b.putAllTo("A.foo.sms", "{xxx}");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertTrue(e.getMessage().startsWith("Cannot put '{xxx}' (String) to property 'foo.sms' (Map<String,String>)."));
		}
		try {
			b.putAllTo("A.foo.sms", "xxx");
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Cannot put 'xxx' (String) to property 'foo.sms' (Map<String,String>).", e.getMessage());
		}
		try {
			b.putAllTo("A.foo.sms", new StringBuilder("foo"));
			fail("Exception expected.");
		} catch (ConfigException e) {
			assertEquals("Cannot put 'foo' (StringBuilder) to property 'foo.sms' (Map<String,String>).", e.getMessage());
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
		b.set("A.foo.lc/prepend", String.class);
		b.set("A.foo.lo/prepend", StringBuilder.class);
		b.set("A.foo.sms", "{foo:'bar'}");
		b.set("A.foo.smi", "{foo:123}");
		b.set("A.foo.smc/put.foo", String.class);
		b.set("A.foo.smo/put.foo", StringBuilder.class);
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

	@Test
	public void testListOfAnnotations() {

		// Per the Annotation spec, two annotations have the same hashcode and are equivalent if their individual
		// values are equal.  Therefore, the annotations on A1 and A2 are two different but equivalent instances.

		Html html1 = A1.class.getAnnotation(Html.class);
		Html html1a = A1.class.getAnnotation(Html.class);
		Html html2 = A2.class.getAnnotation(Html.class);
		Html html3 = A3.class.getAnnotation(Html.class);
		Json json4 = A4.class.getAnnotation(Json.class);

		PropertyStore
			ps1 = PropertyStore.create().set("xxx", AList.of(html1)).build(),
			ps1a = PropertyStore.create().set("xxx", AList.of(html1a)).build(),
			ps2 = PropertyStore.create().set("xxx", AList.of(html2)).build(),
			ps3 = PropertyStore.create().set("xxx", AList.of(html3)).build(),
			ps4 = PropertyStore.create().set("xxx", AList.of(json4)).build();

		assertTrue(ps1.equals(ps1a));
		assertTrue(ps1.equals(ps2));
		assertFalse(ps1.equals(ps3));
		assertFalse(ps1.equals(ps4));
	}

	@Html(on="foo")
	public static class A1 {}

	@Html(on="foo")
	public static class A2 {}

	@Html(on="bar")
	public static class A3 {}

	@Json(on="foo")
	public static class A4 {}

	@Test
	public void testEqualsWithAnnotations() {
		HtmlSerializer
			s1 = HtmlSerializer.create().build(),
			s2 = HtmlSerializer.create().applyAnnotations(B1.class).build(),
			s3 = HtmlSerializer.create().applyAnnotations(B1.class).build(),
			s4 = HtmlSerializer.create().applyAnnotations(B2.class).build();
		assertFalse(s1.getPropertyStore().equals(s2.getPropertyStore()));
		assertFalse(s1.getPropertyStore().equals(s4.getPropertyStore()));
		assertTrue(s2.getPropertyStore().equals(s3.getPropertyStore()));
	}

	@HtmlConfig(applyHtml={@Html(on="B1", format=HtmlFormat.XML)})
	public static class B1 {}
	@HtmlConfig(applyHtml={@Html(on="B2", format=HtmlFormat.HTML)})
	public static class B2 {}

	@Test
	public void testSetDefault() {
		PropertyStoreBuilder psb = PropertyStore.create();
		psb.setDefault("Foo", "1");
		psb.setDefault("Foo", "2");
		assertEquals("1", psb.peek("Foo"));
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
				System.err.println(e.getLocalizedMessage());  // NOT DEBUG
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