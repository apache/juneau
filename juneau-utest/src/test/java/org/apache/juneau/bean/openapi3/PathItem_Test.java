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
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link PathItem}.
 */
class PathItem_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<PathItem> TESTER =
			testBean(
				bean()
					.setDelete(operation().setSummary("a"))
					.setGet(operation().setSummary("b"))
					.setHead(operation().setSummary("c"))
					.setOptions(operation().setSummary("d"))
					.setPatch(operation().setSummary("e"))
					.setPost(operation().setSummary("f"))
					.setPut(operation().setSummary("g"))
					.setTrace(operation().setSummary("h"))
			)
			.props("delete{summary},get{summary},head{summary},options{summary},patch{summary},post{summary},put{summary},trace{summary}")
			.vals("{a},{b},{c},{d},{e},{f},{g},{h}")
			.json("{'delete':{summary:'a'},get:{summary:'b'},head:{summary:'c'},options:{summary:'d'},patch:{summary:'e'},post:{summary:'f'},put:{summary:'g'},trace:{summary:'h'}}")
			.string("{'delete':{'summary':'a'},'get':{'summary':'b'},'head':{'summary':'c'},'options':{'summary':'d'},'patch':{'summary':'e'},'post':{'summary':'f'},'put':{'summary':'g'},'trace':{'summary':'h'}}".replace('\'','"'))
		;

		@Test void a01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void a02_copy() {
			TESTER.assertCopy();
		}

		@Test void a03_toJson() {
			TESTER.assertToJson();
		}

		@Test void a04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void a05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void a06_toString() {
			TESTER.assertToString();
		}

		@Test void a07_keySet() {
			assertList(TESTER.bean().keySet(), "delete", "get", "head", "options", "patch", "post", "put", "trace");
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<PathItem> TESTER =
			testBean(bean())
			.props("get,put,post,delete,options,head,patch,trace")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>,<null>")
			.json("{}")
			.string("{}")
		;

		@Test void b01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void b02_copy() {
			TESTER.assertCopy();
		}

		@Test void b03_toJson() {
			TESTER.assertToJson();
		}

		@Test void b04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void b05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void b06_toString() {
			TESTER.assertToString();
		}

		@Test void b07_keySet() {
			assertEmpty(TESTER.bean().keySet());
		}
	}

	@Nested class C_extraProperties extends TestBase {
		private static final BeanTester<PathItem> TESTER =
			testBean(
				bean()
					.set("delete", operation().setSummary("a"))
					.set("description", "b")
					.set("get", operation().setSummary("c"))
					.set("head", operation().setSummary("d"))
					.set("options", operation().setSummary("e"))
					.set("parameters", list(parameter("f1", "f2")))
					.set("patch", operation().setSummary("g"))
					.set("post", operation().setSummary("h"))
					.set("put", operation().setSummary("i"))
					.set("servers", list(server().setUrl(URI.create("j"))))
					.set("summary", "k")
					.set("trace", operation().setSummary("l"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("delete{summary},description,get{summary},head{summary},options{summary},parameters{0{in,name}},patch{summary},post{summary},put{summary},servers{0{url}},summary,trace{summary},x1,x2")
			.vals("{a},b,{c},{d},{e},{{f1,f2}},{g},{h},{i},{{j}},k,{l},x1a,<null>")
			.json("{'delete':{summary:'a'},description:'b',get:{summary:'c'},head:{summary:'d'},options:{summary:'e'},parameters:[{'in':'f1',name:'f2'}],patch:{summary:'g'},post:{summary:'h'},put:{summary:'i'},servers:[{url:'j'}],summary:'k',trace:{summary:'l'},x1:'x1a'}")
			.string("{'delete':{'summary':'a'},'description':'b','get':{'summary':'c'},'head':{'summary':'d'},'options':{'summary':'e'},'parameters':[{'in':'f1','name':'f2'}],'patch':{'summary':'g'},'post':{'summary':'h'},'put':{'summary':'i'},'servers':[{'url':'j'}],'summary':'k','trace':{'summary':'l'},'x1':'x1a'}".replace('\'', '"'))
		;

		@Test void c01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void c02_copy() {
			TESTER.assertCopy();
		}

		@Test void c03_toJson() {
			TESTER.assertToJson();
		}

		@Test void c04_fromJson() {
			TESTER.assertFromJson();
		}

		@Test void c05_roundTrip() {
			TESTER.assertRoundTrip();
		}

		@Test void c06_toString() {
			TESTER.assertToString();
		}

		@Test void c07_keySet() {
			assertList(TESTER.bean().keySet(), "delete", "description", "get", "head", "options", "parameters", "patch", "post", "put", "servers", "summary", "trace", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"delete{summary},description,get{summary},head{summary},options{summary},parameters{0{in,name}},patch{summary},post{summary},put{summary},servers{0{url}},summary,trace{summary},x1,x2",
				"{a},b,{c},{d},{e},{{f1,f2}},{g},{h},{i},{{j}},k,{l},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"delete,description,get,head,options,parameters,patch,post,put,servers,summary,trace,x1,x2",
				"Operation,String,Operation,Operation,Operation,ArrayList,Operation,Operation,Operation,ArrayList,String,Operation,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_additionalMethods extends TestBase {

		@Test void d01_asMap() {
			assertBean(
				bean()
					.setSummary("a")
					.set("x1", "x1a")
					.asMap(),
				"summary,x1",
				"a,x1a"
			);
		}

		@Test void d02_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}
	}

	@Nested class E_strictMode extends TestBase {

		@Test void e01_strictModeSetThrowsException() {
			var x = bean().strict();
			assertThrows(RuntimeException.class, () -> x.set("foo", "bar"));
		}

		@Test void e02_nonStrictModeAllowsSet() {
			var x = bean(); // not strict
			assertDoesNotThrow(() -> x.set("foo", "bar"));
		}

		@Test void e03_strictModeToggle() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
			x.strict(false);
			assertFalse(x.isStrict());
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static PathItem bean() {
		return pathItem();
	}
}