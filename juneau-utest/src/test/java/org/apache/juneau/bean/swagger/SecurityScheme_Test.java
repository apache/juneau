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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SecurityScheme}.
 */
class SecurityScheme_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<SecurityScheme> TESTER =
			testBean(
				bean()
					.setAuthorizationUrl("a")
					.setDescription("b")
					.setFlow("c")
					.setIn("d")
					.setName("e")
					.setScopes(map("x1", "x2"))
					.setTokenUrl("f")
					.setType("g")
			)
			.props("authorizationUrl,description,flow,in,name,scopes{x1},tokenUrl,type")
			.vals("a,b,c,d,e,{x2},f,g")
			.json("{authorizationUrl:'a',description:'b',flow:'c','in':'d',name:'e',scopes:{x1:'x2'},tokenUrl:'f',type:'g'}")
			.string("{'authorizationUrl':'a','description':'b','flow':'c','in':'d','name':'e','scopes':{'x1':'x2'},'tokenUrl':'f','type':'g'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "authorizationUrl", "description", "flow", "in", "name", "scopes", "tokenUrl", "type");
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<SecurityScheme> TESTER =
			testBean(bean())
			.props("description,type,name,in,flow,authorizationUrl,tokenUrl,scopes")
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

		@Test void b08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}
	}

	@Nested class C_extraProperties extends TestBase {

		private static final BeanTester<SecurityScheme> TESTER =
			testBean(
				bean()
					.set("authorizationUrl", "a")
					.set("description", "b")
					.set("flow", "c")
					.set("in", "d")
					.set("name", "e")
					.set("scopes", map("x1", "x2"))
					.set("tokenUrl", "f")
					.set("type", "g")
					.set("x3", "x3a")
					.set("x4", null)
			)
			.props("authorizationUrl,description,flow,in,name,scopes{x1},tokenUrl,type,x3,x4")
			.vals("a,b,c,d,e,{x2},f,g,x3a,<null>")
			.json("{authorizationUrl:'a',description:'b',flow:'c','in':'d',name:'e',scopes:{x1:'x2'},tokenUrl:'f',type:'g',x3:'x3a'}")
			.string("{'authorizationUrl':'a','description':'b','flow':'c','in':'d','name':'e','scopes':{'x1':'x2'},'tokenUrl':'f','type':'g','x3':'x3a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "authorizationUrl", "description", "flow", "in", "name", "scopes", "tokenUrl", "type", "x3", "x4");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"authorizationUrl,description,flow,in,name,scopes{x1},tokenUrl,type,x3,x4",
				"a,b,c,d,e,{x2},f,g,x3a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"authorizationUrl,description,flow,in,name,scopes,tokenUrl,type,x3,x4",
				"String,String,String,String,String,LinkedHashMap,String,String,String,<null>"
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
			var x = bean()
				.setScopes(map("a1", "a2"))
				.addScope("b1", "b2");

			assertBean(x, "scopes{a1,b1}", "{a2,b2}");
		}

		@Test void d02_addMethodsWithNullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.addScope(null, "a"));
			assertThrows(IllegalArgumentException.class, () -> x.addScope("a", null));
		}

		@Test void d03_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setType("b")
					.set("x1", "x1a")
					.asMap(),
				"description,type,x1",
				"a,b,x1a"
			);
		}

		@Test void d04_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void d05_strict() {
			var x = bean();
			assertFalse(x.isStrict());
			x.strict();
			assertTrue(x.isStrict());
		}
	}

	@Nested class E_strictMode extends TestBase {

		@Test void e01_strictModeSetThrowsException() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Cannot set property 'foo' in strict mode", () -> x.set("foo", "bar"));
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

		@Test void e04_strictModeSetType() {
			var x = bean().strict();
			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=['basic','apiKey','oauth2']", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("basic"));
			assertDoesNotThrow(() -> x.setType("apiKey"));
			assertDoesNotThrow(() -> x.setType("oauth2"));
			var y = bean(); // not strict
			assertDoesNotThrow(() -> y.setType("invalid"));
		}
	}


	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static SecurityScheme bean() {
		return securityScheme();
	}

}