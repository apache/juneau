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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.json.annotation.*;
import org.junit.*;


/**
 * Test the ContextProperties class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ContextPropertiesTest {

	//====================================================================================================
	// testBasic()
	//====================================================================================================

	@Test
	public void testBasic() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("B.f4.s", "4");
		b.set("B.f3.s", "3");
		b.set("A.f2.s", "2");
		b.set("A.f1.s", "1");

		assertObject(b.build()).asJson().is("{A:{'f1.s':'1','f2.s':'2'},B:{'f3.s':'3','f4.s':'4'}}");
	}

	//====================================================================================================
	// testInvalidKeys()
	//====================================================================================================

	@Test
	public void testInvalidKeys() {
		ContextPropertiesBuilder b = ContextProperties.create();
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
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.s", "1");
		b.set("A.f2.s", 2);
		b.set("A.f3.s", true);
		b.set("A.f4.s", OMap.ofJson("{foo:'bar'}"));
		b.set("A.f5.s", OList.ofJson("[1,2]"));
		b.set("A.f6.s", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.s':'1','f2.s':'2','f3.s':'true','f4.s':'{foo:\\'bar\\'}','f5.s':'[1,2]'}}");
		assertOptional(cp.get("A.f1.s")).isType(String.class);
		assertOptional(cp.get("A.f2.s")).isType(String.class);
		assertOptional(cp.get("A.f3.s")).isType(String.class);
		assertOptional(cp.get("A.f4.s")).isType(String.class);
		assertOptional(cp.get("A.f5.s")).isType(String.class);

		b.clear();
		b.set("A.f1", "1");
		b.set("A.f2", 2);
		b.set("A.f3", true);
		b.set("A.f4", OMap.ofJson("{foo:'bar'}"));
		b.set("A.f5", OList.ofJson("[1,2]"));
		b.set("A.f6", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{f1:'1',f2:'2',f3:'true',f4:'{foo:\\'bar\\'}',f5:'[1,2]'}}");
		assertOptional(cp.get("A.f1")).isType(String.class);
		assertOptional(cp.get("A.f2")).isType(String.class);
		assertOptional(cp.get("A.f3")).isType(String.class);
		assertOptional(cp.get("A.f4")).isType(String.class);
		assertOptional(cp.get("A.f5")).isType(String.class);

		b.set("A.f1", "x1");
		b.set("A.f2", null);
		b.set("A.f3", null);
		b.remove("A.f4");
		b.remove("A.f5");

		assertObject(b.build()).asJson().is("{A:{f1:'x1'}}");

		testError(b, "A.f1/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
		testError(b, "A.f1/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1'.");
		testError(b, "A.f1/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1' (String).");
	}

	@Test
	public void testBoolean() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.b", "true");
		b.set("A.f2.b", "false");
		b.set("A.f3.b", new StringBuilder("true"));
		b.set("A.f4.b", new StringBuilder("foo"));
		b.set("A.f5.b", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.b':true,'f2.b':false,'f3.b':true,'f4.b':false}}");
		assertOptional(cp.get("A.f1.b")).isType(Boolean.class);
		assertOptional(cp.get("A.f2.b")).isType(Boolean.class);
		assertOptional(cp.get("A.f3.b")).isType(Boolean.class);
		assertOptional(cp.get("A.f4.b")).isType(Boolean.class);

		// Test nulls
		b.set("A.f2.b", null);
		b.set("A.f3.b", null);
		b.remove("A.f4.b");
		b.remove("A.f5.b");
		assertObject(b.build()).asJson().is("{A:{'f1.b':true}}");

		testError(b, "A.f1.b/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.b'.");
		testError(b, "A.f1.b/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.b'.");
		testError(b, "A.f1.b/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.b' (Boolean).");
	}

	@Test
	public void testInteger() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.i", 123);
		b.set("A.f2.i", "123");
		b.set("A.f3.i", new StringBuilder("123"));
		b.set("A.f4.i", new StringBuilder("-1"));
		b.set("A.f5.i", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.i':123,'f2.i':123,'f3.i':123,'f4.i':-1}}");
		assertOptional(cp.get("A.f1.i")).isType(Integer.class);
		assertOptional(cp.get("A.f2.i")).isType(Integer.class);
		assertOptional(cp.get("A.f3.i")).isType(Integer.class);
		assertOptional(cp.get("A.f4.i")).isType(Integer.class);

		// Test nulls
		b.set("A.f2.i", null);
		b.set("A.f3.i", null);
		b.remove("A.f4.i");
		b.remove("A.f5.i");
		assertObject(b.build()).asJson().is("{A:{'f1.i':123}}");

		testError(b, "A.f1.i/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.i'.");
		testError(b, "A.f1.i/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.i'.");
		testError(b, "A.f1.i/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.i' (Integer).");
		testError(b, "A.f1.i", "foo", "Value 'foo' (String) cannot be converted to an Integer.");
	}

	@Test
	public void testClass() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.c", String.class);
		b.set("A.f2.c", Integer.class);
		b.set("A.f3.c", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.c':'java.lang.String','f2.c':'java.lang.Integer'}}");
		assertOptional(cp.get("A.f1.c")).isType(Class.class);

		// Test nulls
		b.set("A.f2.c", null);
		assertObject(b.build()).asJson().is("{A:{'f1.c':'java.lang.String'}}");

		testError(b, "A.f1.c/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.c'.");
		testError(b, "A.f1.c/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.c'.");
		testError(b, "A.f1.c/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.c' (Class).");

		// Do not allow this for security reasons.
		testError(b, "A.f1.c", "java.lang.String", "Value 'java.lang.String' (String) cannot be converted to a Class.");
	}

	@Test
	public void testObject() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.o", 123);
		b.set("A.f2.o", true);
		b.set("A.f3.o", new StringBuilder("123"));
		b.set("A.f4.o", StringBuilder.class);
		b.set("A.f5.o", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.o':123,'f2.o':true,'f3.o':'123','f4.o':'java.lang.StringBuilder'}}");
		assertOptional(cp.get("A.f1.o")).isType(Integer.class);
		assertOptional(cp.get("A.f2.o")).isType(Boolean.class);
		assertOptional(cp.get("A.f3.o")).isType(StringBuilder.class);
		assertOptional(cp.get("A.f4.o")).isType(Class.class);

		// Test nulls
		b.set("A.f2.o", null);
		b.set("A.f3.o", null);
		b.remove("A.f4.o");
		b.remove("A.f5.o");
		assertObject(b.build()).asJson().is("{A:{'f1.o':123}}");

		testError(b, "A.f1.o/add", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.o'.");
		testError(b, "A.f1.o/add.123", "foo", "addTo() can only be used on properties of type Set on property 'A.f1.o'.");
		testError(b, "A.f1.o/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.o' (Object).");
	}

	@Test
	public void testSetString() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.ss", AList.of("foo", "bar", "bar", null));
		b.set("A.f2.ss", AList.of(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ss", AList.of(new StringBuilder("foo"), null));
		b.set("A.f4.ss", "['foo',123,true]");
		b.set("A.f5.ss", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.ss':['bar','foo'],'f2.ss':['123','ONE','true'],'f3.ss':['foo'],'f4.ss':['123','foo','true']}}");
		assertOptional(cp.get("A.f1.ss")).isType(Set.class);
		assertOptional(cp.get("A.f2.ss")).isType(Set.class);
		assertOptional(cp.get("A.f3.ss")).isType(Set.class);
		assertOptional(cp.get("A.f4.ss")).isType(Set.class);

		b.clear();
		b.set("A.f1.ss/add", "foo");
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['foo']}}");
		b.set("A.f1.ss/remove", "foo");
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.ss/add", "['foo','bar','baz']");
		b.set("A.f1.ss/add", "qux");
		b.addTo("A.f1.ss","quux");
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}");
		b.set("A.f1.ss/remove", "['foo','bar']");
		b.set("A.f1.ss/remove", "qux");
		b.removeFrom("A.f1.ss", "quux");
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['baz']}}");

		b.clear();
		b.set("A.f1.ss/add", AList.of("foo", "bar", "baz"));
		b.set("A.f1.ss/add", AList.of("qux"));
		b.addTo("A.f1.ss", AList.of("quux"));
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}");
		b.set("A.f1.ss/remove", AList.of("foo", "bar"));
		b.set("A.f1.ss/remove", AList.of("qux"));
		b.removeFrom("A.f1.ss", AList.of("quux"));
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['baz']}}");

		b.clear();
		b.set("A.f1.ss/add", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ss/add", new String[]{"qux"});
		b.addTo("A.f1.ss", new String[]{"quux"});
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['bar','baz','foo','quux','qux']}}");

		b.addTo("A.f1.ss", new String[]{"quuux"});
		b.addTo("A.f1.ss", new String[]{"quuuux"});
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['bar','baz','foo','quuuux','quuux','quux','qux']}}");
		b.set("A.f1.ss/remove", new String[]{"quuux", "quuuux"});

		b.set("A.f1.ss/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ss/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ss", new String[]{"quux"});
		assertObject(b.build()).asJson().is("{A:{'f1.ss':['baz']}}");

		b.set("A.f1.ss", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testSetInteger() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.si", AList.of(3, 2, 1, null));
		b.set("A.f2.si", AList.of(123, "456", null));
		b.set("A.f3.si", AList.of(new StringBuilder("123"), null));
		b.set("A.f4.si", "[1,2,3]");
		b.set("A.f5.si", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.si':[1,2,3],'f2.si':[123,456],'f3.si':[123],'f4.si':[1,2,3]}}");
		assertOptional(cp.get("A.f1.si")).isType(Set.class);
		assertOptional(cp.get("A.f2.si")).isType(Set.class);
		assertOptional(cp.get("A.f3.si")).isType(Set.class);
		assertOptional(cp.get("A.f4.si")).isType(Set.class);

		b.clear();
		b.set("A.f1.si/add", "123");
		assertObject(b.build()).asJson().is("{A:{'f1.si':[123]}}");
		b.set("A.f1.si/remove", "123");
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.si/add", "['3','2','1']");
		b.set("A.f1.si/add", "4");
		b.addTo("A.f1.si", "5");
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", "['1','2']");
		b.set("A.f1.si/remove", "3");
		b.removeFrom("A.f1.si", "4");
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.clear();
		b.set("A.f1.si/add", AList.of("3", "2", "1"));
		b.set("A.f1.si/add", AList.of("4"));
		b.addTo("A.f1.si", AList.of("5"));
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", AList.of("1", "2"));
		b.set("A.f1.si/remove", AList.of("3"));
		b.removeFrom("A.f1.si", AList.of("4"));
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.clear();
		b.set("A.f1.si/add", AList.of(1, 2, 3));
		b.set("A.f1.si/add", AList.of(4));
		b.addTo("A.f1.si", AList.of(5));
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", AList.of(1, 2));
		b.set("A.f1.si/remove", AList.of(3));
		b.removeFrom("A.f1.si", AList.of(4));
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.clear();
		b.set("A.f1.si/add", new String[]{"3", "2", "1"});
		b.set("A.f1.si/add", new String[]{"4"});
		b.addTo("A.f1.si", new String[]{"5"});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", new String[]{"1", "2"});
		b.set("A.f1.si/remove", new String[]{"3"});
		b.removeFrom("A.f1.si", new String[]{"4"});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.clear();
		b.set("A.f1.si/add", new Integer[]{3, 2, 1});
		b.set("A.f1.si/add", new Integer[]{4});
		b.addTo("A.f1.si", new Integer[]{5});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", new Integer[]{1, 2});
		b.set("A.f1.si/remove", new Integer[]{3});
		b.removeFrom("A.f1.si", new Integer[]{4});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.clear();
		b.set("A.f1.si/add", new int[]{3, 2, 1});
		b.set("A.f1.si/add", new int[]{4});
		b.addTo("A.f1.si", new int[]{5});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[1,2,3,4,5]}}");
		b.set("A.f1.si/remove", new int[]{1, 2});
		b.set("A.f1.si/remove", new int[]{3});
		b.removeFrom("A.f1.si", new int[]{4});
		assertObject(b.build()).asJson().is("{A:{'f1.si':[5]}}");

		b.set("A.f1.si", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testSetClass() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.sc", AList.of(String.class, Integer.class, null));
		b.set("A.f2.sc", AList.of(String.class, Integer.class, null));
		b.set("A.f3.sc", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.sc':['java.lang.Integer','java.lang.String'],'f2.sc':['java.lang.Integer','java.lang.String']}}");
		assertOptional(cp.get("A.f1.sc")).isType(Set.class);
		assertOptional(cp.get("A.f2.sc")).isType(Set.class);
		assertObject(((Set<?>)cp.get("A.f1.sc").get()).iterator().next()).isType(Class.class);

		b.clear();
		b.set("A.f1.sc/add", Integer.class);
		b.addTo("A.f1.sc", String.class);
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.lang.Integer','java.lang.String']}}");
		b.set("A.f1.sc/remove", Integer.class);
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.lang.String']}}");

		b.clear();
		testError(b, "A.f1.sc/add", "['java.lang.Integer']", "Cannot add value '[\\'java.lang.Integer\\']' (String) to property 'f1.sc' (Set<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");
		testError(b, "A.f1.sc/add", "java.lang.Integer", "Cannot add value 'java.lang.Integer' (String) to property 'f1.sc' (Set<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");

		b.clear();
		b.set("A.f1.sc/add", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/add", AList.of(Map.class));
		b.addTo("A.f1.sc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}");
		b.set("A.f1.sc/remove", AList.of(Integer.class, String.class));
		b.removeFrom("A.f1.sc", AList.create());
		b.removeFrom("A.f1.sc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.util.Map']}}");

		b.clear();
		b.set("A.f1.sc/add", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/add", AList.of(Map.class));
		b.addTo("A.f1.sc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}");
		b.set("A.f1.sc/remove", AList.of(Integer.class, String.class));
		b.set("A.f1.sc/remove", AList.create());
		b.removeFrom("A.f1.sc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.util.Map']}}");

		b.clear();
		b.set("A.f1.sc/add", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.sc/add", new Class<?>[]{Map.class});
		b.addTo("A.f1.sc", new Class<?>[]{List.class});
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.lang.Integer','java.lang.String','java.util.List','java.util.Map']}}");
		b.set("A.f1.sc/remove", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.sc/remove", new Class<?>[]{});
		b.removeFrom("A.f1.sc", new Class<?>[]{List.class});
		assertObject(b.build()).asJson().is("{A:{'f1.sc':['java.util.Map']}}");

		b.set("A.f1.sc", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testListString() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.ls", AList.of("foo", "bar", "bar", null));
		b.set("A.f2.ls", AList.of(123, true, TestEnum.ONE, TestEnum.ONE, null));
		b.set("A.f3.ls", AList.of(new StringBuilder("foo"), null));
		b.set("A.f4.ls", "['foo',123,true]");
		b.set("A.f5.ls", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.ls':['foo','bar'],'f2.ls':['123','true','ONE'],'f3.ls':['foo'],'f4.ls':['foo','123','true']}}");
		assertOptional(cp.get("A.f1.ls")).isType(List.class);
		assertOptional(cp.get("A.f2.ls")).isType(List.class);
		assertOptional(cp.get("A.f3.ls")).isType(List.class);
		assertOptional(cp.get("A.f4.ls")).isType(List.class);

		b.clear();
		b.set("A.f1.ls/prepend", "foo");
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['foo']}}");
		b.set("A.f1.ls/remove", "foo");
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.ls/prepend", "['foo','bar','baz']");
		b.set("A.f1.ls/prepend", "qux");
		b.prependTo("A.f1.ls","quux");
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}");
		b.set("A.f1.ls/remove", "['foo','bar']");
		b.set("A.f1.ls/remove", "qux");
		b.removeFrom("A.f1.ls", "quux");
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['baz']}}");

		b.clear();
		b.set("A.f1.ls/prepend", AList.of("foo", "bar", "baz"));
		b.set("A.f1.ls/prepend", AList.of("qux"));
		b.prependTo("A.f1.ls", AList.of("quux"));
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}");
		b.set("A.f1.ls/remove", AList.of("foo", "bar"));
		b.set("A.f1.ls/remove", AList.of("qux"));
		b.removeFrom("A.f1.ls", AList.of("quux"));
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['baz']}}");

		b.clear();
		b.set("A.f1.ls/prepend", new String[]{"foo", "bar", "baz"});
		b.set("A.f1.ls/prepend", new String[]{"qux"});
		b.prependTo("A.f1.ls", new String[]{"quux"});
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['quux','qux','foo','bar','baz']}}");

		b.appendTo("A.f1.ls", new String[]{"q1x", "q2x"});
		b.appendTo("A.f1.ls", "q3x");
		b.prependTo("A.f1.ls", new String[]{"q4x", "q5x"});
		b.prependTo("A.f1.ls", "q6x");
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['q6x','q4x','q5x','quux','qux','foo','bar','baz','q1x','q2x','q3x']}}");
		b.set("A.f1.ls/remove", new String[]{"q1x","q2x","q3x","q4x","q5x","q6x"});

		b.set("A.f1.ls/remove", new String[]{"foo", "bar"});
		b.set("A.f1.ls/remove", new String[]{"qux"});
		b.removeFrom("A.f1.ls", new String[]{"quux"});
		assertObject(b.build()).asJson().is("{A:{'f1.ls':['baz']}}");

		b.set("A.f1.ls", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testListInteger() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.li", AList.of(1, 2, 3, null));
		b.set("A.f2.li", AList.of(123, "456", null));
		b.set("A.f3.li", AList.of(new StringBuilder("123"), null));
		b.set("A.f4.li", "[1,2,3]");
		b.set("A.f5.li", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.li':[1,2,3],'f2.li':[123,456],'f3.li':[123],'f4.li':[1,2,3]}}");
		assertOptional(cp.get("A.f1.li")).isType(List.class);
		assertOptional(cp.get("A.f2.li")).isType(List.class);
		assertOptional(cp.get("A.f3.li")).isType(List.class);
		assertOptional(cp.get("A.f4.li")).isType(List.class);

		b.clear();
		b.set("A.f1.li/prepend", "123");
		assertObject(b.build()).asJson().is("{A:{'f1.li':[123]}}");
		b.set("A.f1.li/remove", "123");
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.li/prepend", "['1','2','3']");
		b.set("A.f1.li/prepend", "4");
		b.prependTo("A.f1.li", "5");
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", "['1','2']");
		b.set("A.f1.li/remove", "3");
		b.removeFrom("A.f1.li", "4");
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.clear();
		b.set("A.f1.li/prepend", AList.of("1", "2", "3"));
		b.set("A.f1.li/prepend", AList.of("4"));
		b.prependTo("A.f1.li", AList.of("5"));
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", AList.of("1", "2"));
		b.set("A.f1.li/remove", AList.of("3"));
		b.removeFrom("A.f1.li", AList.of("4"));
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.clear();
		b.set("A.f1.li/prepend", AList.of(1, 2, 3));
		b.set("A.f1.li/prepend", AList.of(4));
		b.prependTo("A.f1.li", AList.of(5));
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", AList.of(1, 2));
		b.set("A.f1.li/remove", AList.of(3));
		b.removeFrom("A.f1.li", AList.of(4));
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.clear();
		b.set("A.f1.li/prepend", new String[]{"1", "2", "3"});
		b.set("A.f1.li/prepend", new String[]{"4"});
		b.prependTo("A.f1.li", new String[]{"5"});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", new String[]{"1", "2"});
		b.set("A.f1.li/remove", new String[]{"3"});
		b.removeFrom("A.f1.li", new String[]{"4"});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.clear();
		b.set("A.f1.li/prepend", new Integer[]{1, 2, 3});
		b.set("A.f1.li/prepend", new Integer[]{4});
		b.prependTo("A.f1.li", new Integer[]{5});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", new Integer[]{1, 2});
		b.set("A.f1.li/remove", new Integer[]{3});
		b.removeFrom("A.f1.li", new Integer[]{4});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.clear();
		b.set("A.f1.li/prepend", new int[]{1, 2, 3});
		b.set("A.f1.li/prepend", new int[]{4});
		b.prependTo("A.f1.li", new int[]{5});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5,4,1,2,3]}}");
		b.set("A.f1.li/remove", new int[]{1, 2});
		b.set("A.f1.li/remove", new int[]{3});
		b.removeFrom("A.f1.li", new int[]{4});
		assertObject(b.build()).asJson().is("{A:{'f1.li':[5]}}");

		b.set("A.f1.li", null);
		assertObject(b.build()).asJson().is("{}");

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
//			fail();
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.li' (List<Integer>)", e.getMessage());
//		}
	}

	@Test
	public void testListClass() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.lc", AList.of(String.class, Integer.class, null));
		b.set("A.f2.lc", AList.of(String.class, Integer.class, null));
		b.set("A.f3.lc", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.lc':['java.lang.String','java.lang.Integer'],'f2.lc':['java.lang.String','java.lang.Integer']}}");
		assertOptional(cp.get("A.f1.lc")).isType(List.class);
		assertOptional(cp.get("A.f2.lc")).isType(List.class);

		b.clear();
		b.set("A.f1.lc/prepend", Integer.class);
		b.prependTo("A.f1.lc", String.class);
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.lang.String','java.lang.Integer']}}");
		b.set("A.f1.lc/remove", Integer.class);
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.lang.String']}}");

		b.clear();
		testError(b, "A.f1.lc/prepend", "['java.lang.Integer']", "Cannot prepend value '[\\'java.lang.Integer\\']' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");
		testError(b, "A.f1.lc/prepend", "java.lang.Integer", "Cannot prepend value 'java.lang.Integer' (String) to property 'f1.lc' (List<Class>).  Value 'java.lang.Integer' (String) cannot be converted to a Class.");

		b.clear();
		b.set("A.f1.lc/prepend", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/prepend", AList.of(Map.class));
		b.prependTo("A.f1.lc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}");
		b.set("A.f1.lc/remove", AList.of(Integer.class, String.class));
		b.removeFrom("A.f1.lc", AList.create());
		b.removeFrom("A.f1.lc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.Map']}}");

		b.clear();
		b.set("A.f1.lc/prepend", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/prepend", AList.of(Map.class));
		b.prependTo("A.f1.lc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}");
		b.set("A.f1.lc/remove", AList.of(Integer.class, String.class));
		b.set("A.f1.lc/remove", AList.create());
		b.removeFrom("A.f1.lc", AList.of(List.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.Map']}}");

		b.clear();
		b.set("A.f1.lc/prepend", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/prepend", new Class<?>[]{Map.class});
		b.prependTo("A.f1.lc", new Class<?>[]{List.class});
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.List','java.util.Map','java.lang.Integer','java.lang.String']}}");
		b.set("A.f1.lc/remove", new Class<?>[]{Integer.class, String.class});
		b.set("A.f1.lc/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lc", new Class<?>[]{List.class});
		assertObject(b.build()).asJson().is("{A:{'f1.lc':['java.util.Map']}}");

		b.set("A.f1.lc", null);
		assertObject(b.build()).asJson().is("{}");

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
//			fail();
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.lc' (List<Class>)", e.getMessage());
//		}
	}

	@Test
	public void testListObject() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.lo", AList.of(StringBuilder.class, null));
		b.set("A.f2.lo", AList.of(123, true, new StringBuilder(123), StringBuilder.class, null));
		b.set("A.f3.lo", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.lo':['java.lang.StringBuilder'],'f2.lo':[123,true,'','java.lang.StringBuilder']}}");
		assertOptional(cp.get("A.f1.lo")).isType(List.class);
		assertOptional(cp.get("A.f2.lo")).isType(List.class);
		assertObject(((List<?>)cp.get("A.f1.lo").get()).get(0)).isType(Class.class);
		assertObject(((List<?>)cp.get("A.f2.lo").get()).get(0)).isType(Integer.class);
		assertObject(((List<?>)cp.get("A.f2.lo").get()).get(1)).isType(Boolean.class);
		assertObject(((List<?>)cp.get("A.f2.lo").get()).get(2)).isType(StringBuilder.class);
		assertObject(((List<?>)cp.get("A.f2.lo").get()).get(3)).isType(Class.class);

		b.clear();
		b.set("A.f1.lo/prepend", 1);
		b.prependTo("A.f1.lo", 2);
		assertObject(b.build()).asJson().is("{A:{'f1.lo':[2,1]}}");
		b.set("A.f1.lo/remove", 1);
		assertObject(b.build()).asJson().is("{A:{'f1.lo':[2]}}");

		b.clear();
		b.set("A.f1.lo/prepend", AList.of(StringBuilder.class));
		b.set("A.f1.lo/prepend", AList.of(HashMap.class));
		b.prependTo("A.f1.lo", AList.of(LinkedList.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}");
		b.set("A.f1.lo/remove", AList.of(HashMap.class));
		b.removeFrom("A.f1.lo", AList.create());
		b.removeFrom("A.f1.lo", AList.of(LinkedList.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.lang.StringBuilder']}}");

		b.clear();
		b.set("A.f1.lo/prepend", AList.of(StringBuilder.class));
		b.set("A.f1.lo/prepend", AList.of(HashMap.class));
		b.prependTo("A.f1.lo", AList.of(LinkedList.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}");
		b.set("A.f1.lo/remove", AList.of(HashMap.class));
		b.set("A.f1.lo/remove", AList.create());
		b.removeFrom("A.f1.lo", AList.of(LinkedList.class));
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.lang.StringBuilder']}}");

		b.clear();
		b.set("A.f1.lo/prepend", new Class<?>[]{StringBuilder.class});
		b.set("A.f1.lo/prepend", new Class<?>[]{HashMap.class});
		b.prependTo("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.util.LinkedList','java.util.HashMap','java.lang.StringBuilder']}}");
		b.set("A.f1.lo/remove", new Class<?>[]{HashMap.class});
		b.set("A.f1.lo/remove", new Class<?>[]{});
		b.removeFrom("A.f1.lo", new Class<?>[]{LinkedList.class});
		assertObject(b.build()).asJson().is("{A:{'f1.lo':['java.lang.StringBuilder']}}");

		b.set("A.f1.lo", null);
		assertObject(b.build()).asJson().is("{}");

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
//			fail();
//		} catch (Exception e) {
//			assertEquals("Invalid argument 'foo' on add command for property 'f1.lo' (List<Object>)", e.getMessage());
//		}
	}

	@Test
	public void testMapString() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.sms", AMap.of("foo","bar","baz","qux","quux",null,null,null));
		b.set("A.f2.sms", AMap.of("foo",(Object)123,"bar",true,"baz",TestEnum.ONE,"qux",null));
		b.set("A.f3.sms", AMap.of("foo",new StringBuilder("bar"),"baz",null));
		b.set("A.f4.sms", "{foo:'bar',baz:123,qux:true}");
		b.set("A.f5.sms", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.sms':{baz:'qux',foo:'bar'},'f2.sms':{bar:'true',baz:'ONE',foo:'123'},'f3.sms':{foo:'bar'},'f4.sms':{baz:'123',foo:'bar',qux:'true'}}}");
		assertOptional(cp.get("A.f1.sms")).isType(Map.class);
		assertOptional(cp.get("A.f2.sms")).isType(Map.class);
		assertOptional(cp.get("A.f3.sms")).isType(Map.class);
		assertOptional(cp.get("A.f4.sms")).isType(Map.class);

		b.clear();
		b.set("A.f1.sms/put", "{foo:'bar'}");
		assertObject(b.build()).asJson().is("{A:{'f1.sms':{foo:'bar'}}}");

		b.clear();
		b.set("A.f1.sms/put.foo", "bar");
		assertObject(b.build()).asJson().is("{A:{'f1.sms':{foo:'bar'}}}");
		b.set("A.f1.sms/put.foo", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.sms", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.sms", "{foo:'bar'}");
		testError(b, "A.f1.sms/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.sms' (Map<String,String>).");
		assertThrown(()->b.removeFrom("A.f1.sms", "foo")).message().is("Cannot remove value 'foo' (String) from property 'f1.sms' (Map<String,String>).");
	}

	@Test
	public void testMapInteger() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.smi", AMap.of("foo","1","baz","2","quux",null,null,null));
		b.set("A.f2.smi", AMap.of("foo",123,"bar","456","baz",null));
		b.set("A.f3.smi", AMap.of("foo",new StringBuilder("123"),"baz",null));
		b.set("A.f4.smi", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smi", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.smi':{baz:2,foo:1},'f2.smi':{bar:456,foo:123},'f3.smi':{foo:123},'f4.smi':{baz:456,foo:123}}}");
		assertOptional(cp.get("A.f1.smi")).isType(Map.class);
		assertOptional(cp.get("A.f2.smi")).isType(Map.class);
		assertOptional(cp.get("A.f3.smi")).isType(Map.class);
		assertOptional(cp.get("A.f4.smi")).isType(Map.class);

		b.clear();
		b.set("A.f1.smi/put", "{foo:'123'}");
		assertObject(b.build()).asJson().is("{A:{'f1.smi':{foo:123}}}");

		b.clear();
		b.set("A.f1.smi/put.foo", "123");
		assertObject(b.build()).asJson().is("{A:{'f1.smi':{foo:123}}}");
		b.set("A.f1.smi/put.foo", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smi", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smi", "{foo:'123'}");
		testError(b, "A.f1.smi/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smi' (Map<String,Integer>).");
		assertThrown(()->b.removeFrom("A.f1.smi", "foo")).message().is("Cannot remove value 'foo' (String) from property 'f1.smi' (Map<String,Integer>).");
	}

	@Test
	public void testMapClass() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = null;
		b.set("A.f1.smc", AMap.of("foo",String.class,"baz",Integer.class,"quux",null,null,null));
		b.set("A.f2.smc", AMap.of("foo",String.class,"bar",Integer.class,"baz",null));
		b.set("A.f3.smc", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.smc':{baz:'java.lang.Integer',foo:'java.lang.String'},'f2.smc':{bar:'java.lang.Integer',foo:'java.lang.String'}}}");
		assertOptional(cp.get("A.f1.smc")).isType(Map.class);
		assertOptional(cp.get("A.f2.smc")).isType(Map.class);
		assertObject(((Map<?,?>)cp.get("A.f1.smc").get()).values().iterator().next()).isType(Class.class);
		assertObject(((Map<?,?>)cp.get("A.f2.smc").get()).values().iterator().next()).isType(Class.class);

		b.clear();
		b.set("A.f1.smc/put.foo", String.class);
		assertObject(b.build()).asJson().is("{A:{'f1.smc':{foo:'java.lang.String'}}}");
		b.set("A.f1.smc/put.foo", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smc", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smc/put.foo", String.class);
		testError(b, "A.f1.smc/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smc' (Map<String,Class>).");
		assertThrown(()->b.removeFrom("A.f1.smc", "foo")).message().is("Cannot remove value 'foo' (String) from property 'f1.smc' (Map<String,Class>).");
	}

	@Test
	public void testMapObject() throws Exception {
		ContextPropertiesBuilder b = ContextProperties.create();

		ContextProperties cp = null;
		b.set("A.f1.smo", AMap.of("foo","1","baz","2","quux",null,null,null));
		b.set("A.f2.smo", AMap.of("foo",123,"bar",StringBuilder.class,"qux",null));
		b.set("A.f3.smo", AMap.of("foo",new StringBuilder("123"),"baz",null));
		b.set("A.f4.smo", "{foo:'123',baz:456,qux:null}");
		b.set("A.f5.smo", null);
		cp = b.build();
		assertObject(cp).asJson().is("{A:{'f1.smo':{baz:'2',foo:'1'},'f2.smo':{bar:'java.lang.StringBuilder',foo:123},'f3.smo':{foo:'123'},'f4.smo':{baz:456,foo:'123'}}}");
		assertOptional(cp.get("A.f1.smo")).isType(Map.class);
		assertOptional(cp.get("A.f2.smo")).isType(Map.class);
		assertOptional(cp.get("A.f3.smo")).isType(Map.class);
		assertOptional(cp.get("A.f4.smo")).isType(Map.class);

		b.clear();
		b.set("A.f1.smo/put", "{foo:'123'}");
		assertObject(b.build()).asJson().is("{A:{'f1.smo':{foo:'123'}}}");

		b.clear();
		b.set("A.f1.smo/put.foo", "123");
		assertObject(b.build()).asJson().is("{A:{'f1.smo':{foo:'123'}}}");
		b.set("A.f1.smo/put.foo", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smo", null);
		assertObject(b.build()).asJson().is("{}");

		b.clear();
		b.set("A.f1.smo", "{foo:'123'}");
		testError(b, "A.f1.smo/remove", "foo", "Cannot remove value 'foo' (String) from property 'f1.smo' (Map<String,Object>).");
		assertThrown(()->b.removeFrom("A.f1.smo", "foo")).message().is("Cannot remove value 'foo' (String) from property 'f1.smo' (Map<String,Object>).");
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Hash tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testPropertyTypeStringHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

		b1.set("A.f1", "foo");
		b2.set("A.f1", new StringBuilder("foo"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1", "foo");
		b2.set("A.f1", new StringBuilder("foox"));
		testNotEquals(b1, b2);

		cp = b1.build();
		b1.set("A.f1", "foo");
		assertTrue(cp == b1.build());

		b1.set("A.f1", "bar");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testPropertyTypeBooleanHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

		b1.set("A.f1.b");
		b2.set("A.f1.b", new StringBuilder("true"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.b");
		b2.set("A.f1.b", new StringBuilder("false"));
		testNotEquals(b1, b2);

		cp = b1.build();
		b1.set("A.f1.b");
		assertTrue(cp == b1.build());

		b1.set("A.f1.b", false);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testPropertyTypeIntegerHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

		b1.set("A.f1.i", 1);
		b2.set("A.f1.i", new StringBuilder("1"));
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.i", 1);
		b2.set("A.f1.i", new StringBuilder("2"));

		testNotEquals(b1, b2);
		assertTrue(b1.build() != b2.build());

		cp = b1.build();
		b1.set("A.f1.i", 1);
		assertTrue(cp == b1.build());

		b1.set("A.f1.i", 2);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testClassHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

		b1.set("A.f1.c", String.class);
		b2.set("A.f1.c", String.class);
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.c", String.class);
		b2.set("A.f1.c", Integer.class);

		testNotEquals(b1, b2);

		cp = b1.build();
		b1.set("A.f1.c", String.class);
		assertTrue(cp == b1.build());

		b1.set("A.f1.c", Integer.class);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testObjectHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

		b1.set("A.f1.o", "foo");
		b2.set("A.f1.o", "foo");
		testEquals(b1, b2);

		testEquals(b1, b1);

		b1.set("A.f1.o", TestEnum.ONE);
		b2.set("A.f1.o", TestEnum.TWO);
		testNotEquals(b1, b2);

		cp = b1.build();
		b1.set("A.f1.o", TestEnum.ONE);
		assertTrue(cp == b1.build());

		b1.set("A.f1.o", TestEnum.TWO);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetStringHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.ss", "['foo']");
		assertTrue(cp == b1.build());

		b1.set("A.f1.ss", "['bar']");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetIntegerHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.si", "['1']");
		assertTrue(cp == b1.build());

		b1.set("A.f1.si", "['2']");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testSetClassHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.sc", String.class);
		assertTrue(cp == b1.build());

		b1.set("A.f1.sc", Map.class);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListStringHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.ls", "['foo']");
		assertTrue(cp == b1.build());

		b1.set("A.f1.ls", "['bar']");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListIntegerHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.li", "['1']");
		assertTrue(cp == b1.build());

		b1.set("A.f1.li", "['2']");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListClassHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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

		cp = b1.build();
		b1.set("A.f1.lc", String.class);
		assertTrue(cp == b1.build());

		b1.set("A.f1.lc", Map.class);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testListObjectHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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
		cp = b1.build();
		b1.set("A.f1.lo", "foo");
		assertTrue(cp == b1.build());

		b1.set("A.f1.lo", "bar");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapStringHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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
		cp = b1.build();
		b1.set("A.f1.sms", "{foo:'bar'}");
		assertTrue(cp == b1.build());

		b1.set("A.f1.sms", "{foo:'baz'}");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapIntegerHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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
		cp = b1.build();
		b1.set("A.f1.smi", "{foo:'123'}");
		assertTrue(cp == b1.build());

		b1.set("A.f1.smi", "{foo:'456'}");
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	public void testMapClassHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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
		cp = b1.build();
		b1.set("A.f1.smc/put.foo", Integer.class);
		assertTrue(cp == b1.build());

		b1.set("A.f1.smc/put.foo", String.class);
		assertTrue(cp != b1.build());

		b1.clear();
		b2.clear();
		testEquals(b1, b2);
	}

	@Test
	@Ignore // TODO - Fix me.
	public void testMapObjectHash() throws Exception {
		ContextPropertiesBuilder b1 = ContextProperties.create(), b2 = ContextProperties.create();
		ContextProperties cp = null;

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
		cp = b1.build();
		b1.set("A.f1.smo/put.foo", TestEnum.ONE);
		assertTrue(cp == b1.build());

		b1.set("A.f1.smo/put.foo", TestEnum.TWO);
		assertTrue(cp != b1.build());

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
		ContextProperties cp = ContextProperties.create().build();
		assertString(cp.get("A.f1")).is("foo");

		System.clearProperty("A.f1");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1")).isNull();
	}

	@Test
	public void testIntegerDefault() {

		System.setProperty("A.f1.i", "1");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.i")).isType(Integer.class).is(1);

		System.clearProperty("A.f1.i");
		System.setProperty("A.f1", "1");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.i")).isType(Integer.class).is(1);

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.i")).isNull();
	}

	@Test
	public void testObjectDefault() {

		System.setProperty("A.f1.o", "123");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.o")).isType(String.class).is("123");

		System.clearProperty("A.f1.o");
		System.setProperty("A.f1", "123");
		assertOptional(cp.get("A.f1.o")).isType(String.class).is("123");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.o")).isNull();
	}

	@Test
	public void testSetStringDefault() {

		System.setProperty("A.f1.ss", "['foo','bar']");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.ss")).asJson().is("['bar','foo']");

		System.clearProperty("A.f1.ss");
		System.setProperty("A.f1", "['foo','bar']");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.ss")).asJson().is("['bar','foo']");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.ss")).isNull();
	}

	@Test
	public void testSetIntegerDefault() {

		System.setProperty("A.f1.si", "['2','1']");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.si")).asJson().is("[1,2]");

		System.clearProperty("A.f1.si");
		System.setProperty("A.f1", "['2','1']");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.si")).asJson().is("[1,2]");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.si")).isNull();
	}

	@Test
	public void testListStringDefault() {

		System.setProperty("A.f1.ls", "['foo','bar']");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.ls")).asJson().is("['foo','bar']");

		System.clearProperty("A.f1.ls");
		System.setProperty("A.f1", "['foo','bar']");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.ls")).asJson().is("['foo','bar']");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.ls")).isNull();
	}

	@Test
	public void testListIntegerDefault() {

		System.setProperty("A.f1.li", "['2','1']");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.li")).asJson().is("[2,1]");

		System.clearProperty("A.f1.li");
		System.setProperty("A.f1", "['2','1']");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.li")).asJson().is("[2,1]");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.li")).isNull();
	}

	@Test
	public void testMapStringDefault() {

		System.setProperty("A.f1.sms", "{foo:'bar',baz:null}");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.sms")).asJson().is("{foo:'bar'}");

		System.clearProperty("A.f1.sms");
		System.setProperty("A.f1", "{foo:'bar',baz:null}");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.sms")).asJson().is("{foo:'bar'}");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.sms")).isNull();
	}

	@Test
	public void testMapIntegerDefault() {

		System.setProperty("A.f1.smi", "{foo:'123',baz:null}");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.smi")).asJson().is("{foo:123}");

		System.clearProperty("A.f1.smi");
		System.setProperty("A.f1", "{foo:'123',baz:null}");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.smi")).asJson().is("{foo:123}");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.smi")).isNull();
	}

	@Test
	public void testMapObjectDefault() {

		System.setProperty("A.f1.smo", "{foo:123,bar:'baz',qux:true,quux:null}");
		ContextProperties cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.smo")).asJson().is("{bar:'baz',foo:123,qux:true}");

		System.clearProperty("A.f1.smo");
		System.setProperty("A.f1", "{foo:123,bar:'baz',qux:true,quux:null}");
		cp = ContextProperties.create().build();
		assertOptional(cp.get("A.f1.smo")).asJson().is("{bar:'baz',foo:123,qux:true}");

		System.clearProperty("A.f1");
		assertOptional(cp.get("A.f1.smo")).isNull();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Other tests
	//-------------------------------------------------------------------------------------------------------------------

	@Test
	public void testBuilderFromStore() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo", "bar");
		ContextProperties cp = b.build();
		b = cp.copy();
		assertObject(b.build()).asJson().is("{A:{foo:'bar'}}");
	}

	@Test
	public void testSet() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.set(OMap.of("A.foo", "bar"));
		b.clear();
		b.set(OMap.of("A.baz", "qux"));
		b.add(null);
		assertObject(b.build()).asJson().is("{A:{baz:'qux'}}");
	}

	@Test
	public void testAdd() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.add(OMap.of("A.foo", "bar"));
		b.add(OMap.of("A.baz", "qux"));
		b.add(OMap.of("A.quux", null));
		b.add(null);
		assertObject(b.build()).asJson().is("{A:{baz:'qux',foo:'bar'}}");
	}

	@Test
	public void testRemoveNotExisting() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.removeFrom("A.foo.ls", "bar");
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testAddToNull() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.appendTo("A.foo.ls", null);
		b.prependTo("A.foo.ls", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testRemoveNull() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.removeFrom("A.foo.ss", null);
		assertObject(b.build()).asJson().is("{}");
		b.removeFrom("A.foo.ls", null);
		assertObject(b.build()).asJson().is("{}");
	}

	@Test
	public void testRemoveFromInvalidListOfObjects() {
		ContextPropertiesBuilder b = ContextProperties.create();
		assertThrown(()->b.removeFrom("A.foo.ss", "[xxx]")).message().contains("Cannot remove value '[xxx]' (String) from property 'foo.ss' (Set<String>).");
		assertThrown(()->b.removeFrom("A.foo.ls", "[xxx]")).message().contains("Cannot remove value '[xxx]' (String) from property 'foo.ls' (List<String>).");
	}

	@Test
	public void testAddToInvalidMapOfObjects() {
		ContextPropertiesBuilder b = ContextProperties.create();
		assertThrown(()->b.putAllTo("A.foo.sms", "{xxx}")).message().contains("Cannot put '{xxx}' (String) to property 'foo.sms' (Map<String,String>).");
		assertThrown(()->b.putAllTo("A.foo.sms", "xxx")).message().is("Cannot put 'xxx' (String) to property 'foo.sms' (Map<String,String>).");
		assertThrown(()->b.putAllTo("A.foo.sms", new StringBuilder("foo"))).message().is("Cannot put 'foo' (StringBuilder) to property 'foo.sms' (Map<String,String>).");
	}

	@Test
	public void testGetNonExistent() {
		ContextProperties b = ContextProperties.create().set("A.foo", "bar").build();
		assertOptional(b.get("A.baz")).isNull();
		assertOptional(b.get("B.foo")).isNull();
	}

	@Test
	public void testHashCodes() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.set("A.foo", "bar").set("B.foo", "bar");
		ContextProperties cp = b.build();

		assertEquals(cp.hashCode("A","B","C",null),cp.hashCode("A","B","C",null));
		assertNotEquals(cp.hashCode("A"),cp.hashCode("B"));
		assertNotEquals(cp.hashCode("A","B"),cp.hashCode("B","A"));
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEquals() {
		ContextPropertiesBuilder b = ContextProperties.create();
		ContextProperties cp = b.build();
		assertFalse(cp.equals("foo"));
	}

	@Test
	public void testEqualsByGroups() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.set("A.foo", "bar").set("B.foo", "bar").set("D.foo", "bar");
		ContextProperties cp1 = b.build();
		b.remove("A.foo").set("C.foo", "bar").set("D.foo", "baz");
		ContextProperties cp2 = b.build();

		assertTrue(cp1.equals(cp1, null, null));
		assertTrue(cp1.equals(cp2, null, null));

		assertTrue(cp1.equals(cp2, "B"));
		assertTrue(cp1.equals(cp2, "X"));
		assertFalse(cp1.equals(cp2, "A"));
		assertFalse(cp1.equals(cp2, "C"));
		assertFalse(cp1.equals(cp2, "D"));
		assertFalse(cp1.equals(cp2, "B", "D"));
	}

	@Test
	public void testKeySet() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.set("A.foo", "bar").set("B.foo", "bar").set("D.foo", "bar");
		ContextProperties cp = b.build();

		assertObject(cp.getKeys(null)).asJson().is("[]");
		assertObject(cp.getKeys("A")).asJson().is("['foo']");
		assertObject(cp.getKeys("C")).asJson().is("[]");
	}

	@Test
	public void testToMutable() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.set("A.foo.s", "bar");
		b.set("A.foo.b");
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
		ContextProperties cp = b.build();

		b = cp.copy();
		cp = b.build();

		assertObject(cp).asJson().is("{A:{'foo.b':true,'foo.c':'java.lang.String','foo.i':123,'foo.lc':['java.lang.String'],'foo.li':[123],'foo.lo':['java.lang.StringBuilder'],'foo.ls':['bar'],'foo.o':'bar','foo.s':'bar','foo.sc':['java.lang.String'],'foo.si':[123],'foo.smc':{foo:'java.lang.String'},'foo.smi':{foo:123},'foo.smo':{foo:'java.lang.StringBuilder'},'foo.sms':{foo:'bar'},'foo.ss':['bar']}}");
	}

	@Test
	public void testToString() {
		ContextProperties p = ContextProperties.create().build();
		assertEquals("{}", p.toString());
	}

	@Test
	public void testNoneOnList() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo.ls", "['foo','bar']");
		b.set("A.foo.ls", "NONE");
		assertEquals("{}", b.build().toString());
	}

	@Test
	public void testNoneOnSet() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo.ss", "['foo','bar']");
		b.set("A.foo.ss", "NONE");
		assertEquals("{}", b.build().toString());
	}

	@Test
	public void testInheritOnList() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo.ls", "['foo','bar']");
		b.set("A.foo.ls", "['baz','INHERIT','qux']");
		assertEquals("{A:{'foo.ls':['baz','foo','bar','qux']}}", b.build().toString());
	}

	@Test
	public void testInheritOnSet() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo.ls", "['foo','bar']");
		b.set("A.foo.ls", "['baz','INHERIT','qux']");
		assertEquals("{A:{'foo.ls':['baz','foo','bar','qux']}}", b.build().toString());
	}

	@Test
	public void testIndexedValuesOnList() {
		ContextPropertiesBuilder b = ContextProperties.create();

		b.set("A.foo.ls", "['foo','bar']");
		b.set("A.foo.ls", new String[]{"INHERIT", "[0]:baz"});
		assertEquals("{A:{'foo.ls':['baz','foo','bar']}}", b.build().toString());
		b.set("A.foo.ls", new String[]{"INHERIT", "[1]:qux"});
		assertEquals("{A:{'foo.ls':['baz','qux','foo','bar']}}", b.build().toString());
		b.set("A.foo.ls", new String[]{"INHERIT", "[10]:quux"});
		assertEquals("{A:{'foo.ls':['baz','qux','foo','bar','quux']}}", b.build().toString());
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

		ContextProperties
			cp1 = ContextProperties.create().set("xxx", AList.of(html1)).build(),
			cp1a = ContextProperties.create().set("xxx", AList.of(html1a)).build(),
			cp2 = ContextProperties.create().set("xxx", AList.of(html2)).build(),
			cp3 = ContextProperties.create().set("xxx", AList.of(html3)).build(),
			cp4 = ContextProperties.create().set("xxx", AList.of(json4)).build();

		assertTrue(cp1.equals(cp1a));
		assertTrue(cp1.equals(cp2));
		assertFalse(cp1.equals(cp3));
		assertFalse(cp1.equals(cp4));
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
		HtmlSerializerBuilder
			s1 = HtmlSerializer.create(),
			s2 = HtmlSerializer.create().applyAnnotations(B1Config.class),
			s3 = HtmlSerializer.create().applyAnnotations(B1Config.class),
			s4 = HtmlSerializer.create().applyAnnotations(B2Config.class);
		assertFalse(s1.hashKey().equals(s2.hashKey()));
		assertFalse(s1.hashKey().equals(s4.hashKey()));
		assertTrue(s2.hashKey().equals(s3.hashKey()));
	}

	@Html(on="B1", format=HtmlFormat.XML)
	private static class B1Config {}

	public static class B1 {}

	@Html(on="B2", format=HtmlFormat.HTML)
	private static class B2Config {}

	public static class B2 {}

	@Test
	public void testSetDefault() {
		ContextPropertiesBuilder b = ContextProperties.create();
		b.setDefault("Foo", "1");
		b.setDefault("Foo", "2");
		assertEquals("1", b.peek("Foo"));
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-------------------------------------------------------------------------------------------------------------------

	private void testError(ContextPropertiesBuilder b, String key, Object val, String msg) {
		assertThrown(()->b.set(key, val)).message().is(msg);
	}

	private void testEquals(ContextPropertiesBuilder b1, ContextPropertiesBuilder b2) {
		assertTrue(b1.build() == b2.build());
	}

	private void testNotEquals(ContextPropertiesBuilder b1, ContextPropertiesBuilder b2) {
		ContextProperties cp1 = b1.build(), cp2 = b2.build();
		assertTrue(cp1 != cp2);
		assertTrue(cp1.hashCode() != cp2.hashCode());
	}

	public static enum TestEnum {
		ONE, TWO, THREE;
	}
}