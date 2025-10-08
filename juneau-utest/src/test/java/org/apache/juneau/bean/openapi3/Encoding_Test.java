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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Encoding}.
 */
class Encoding_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Encoding> TESTER =
			testBean(
				bean()
					.setAllowReserved(true)
					.setContentType("a")
					.setExplode(true)
					.setHeaders(map("b1", headerInfo(schemaInfo().setType("b2"))))
					.setStyle("c")
			)
			.props("allowReserved,contentType,explode,headers{b1{schema{type}}},style")
			.vals("true,a,true,{{{b2}}},c")
			.json("{allowReserved:true,contentType:'a',explode:true,headers:{b1:{schema:{type:'b2'}}},style:'c'}")
			.string("{'allowReserved':true,'contentType':'a','explode':true,'headers':{'b1':{'schema':{'type':'b2'}}},'style':'c'}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "allowReserved", "contentType", "explode", "headers", "style");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Encoding> TESTER =
			testBean(bean())
			.props("contentType,style,explode,allowReserved,headers")
			.vals("<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<Encoding> TESTER =
			testBean(
				bean()
					.set("allowReserved", true)
					.set("contentType", "a")
					.set("explode", true)
					.set("headers", map("b1", headerInfo(schemaInfo("b2"))))
					.set("style", "c")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("allowReserved,contentType,explode,headers{b1{schema{type}}},style,x1,x2")
			.vals("true,a,true,{{{b2}}},c,x1a,<null>")
			.json("{allowReserved:true,contentType:'a',explode:true,headers:{b1:{schema:{type:'b2'}}},style:'c',x1:'x1a'}")
			.string("{'allowReserved':true,'contentType':'a','explode':true,'headers':{'b1':{'schema':{'type':'b2'}}},'style':'c','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "allowReserved", "contentType", "explode", "headers", "style", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"allowReserved,contentType,explode,headers{b1{schema{type}}},style,x1,x2",
				"true,a,true,{{{b2}}},c,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"allowReserved,contentType,explode,headers,style,x1,x2",
				"Boolean,String,Boolean,LinkedHashMap,String,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	@Nested class D_additionalMethods extends TestBase {

		@Test void d01_addMethods() {
			assertBean(
				bean()
					.addHeader("a1", headerInfo(schemaInfo("a2"))),
				"headers{a1{schema{type}}}",
				"{{{a2}}}"
			);
		}

		@Test void d02_asMap() {
			assertBean(
				bean()
					.setContentType("a")
					.set("x1", "x1a")
					.asMap(),
				"contentType,x1",
				"a,x1a"
			);
		}

		@Test void d03_extraKeys() {
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

	private static Encoding bean() {
		return encoding();
	}
}
