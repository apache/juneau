/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.bean.openapi3;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.openapi3.OpenApiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SecuritySchemeInfo}.
 */
class SecuritySchemeInfo_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<SecuritySchemeInfo> TESTER =
			testBean(
				bean()
					.setBearerFormat("a")
					.setDescription("b")
					.setFlows(oAuthFlow().setAuthorizationUrl("c"))
					.setIn("d")
					.setName("e")
					.setScheme("f")
					.setType("g")
			)
			.props("bearerFormat,description,flows{authorizationUrl},in,name,scheme,type")
			.vals("a,b,{c},d,e,f,g")
			.json("{bearerFormat:'a',description:'b',flows:{authorizationUrl:'c'},'in':'d',name:'e',scheme:'f',type:'g'}")
			.string("{'bearerFormat':'a','description':'b','flows':{'authorizationUrl':'c'},'in':'d','name':'e','scheme':'f','type':'g'}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "bearerFormat", "description", "flows", "in", "name", "scheme", "type");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_asMap() {
			assertBean(
				bean()
					.setType("a")
					.set("x1", "x1a")
					.asMap(),
				"type,x1",
				"a,x1a"
			);
		}

		@Test void a10_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a11_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());

			var x = bean().strict();
			var y = bean(); // not strict

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setIn(String).  Value='invalid', valid values=[query, header, cookie]", () -> x.setIn("invalid"));
			assertDoesNotThrow(() -> x.setIn("query"));
			assertDoesNotThrow(() -> x.setIn("header"));
			assertDoesNotThrow(() -> x.setIn("cookie"));
			assertDoesNotThrow(() -> y.setIn("invalid"));

			assertThrowsWithMessage(RuntimeException.class, "Invalid value passed in to setType(String).  Value='invalid', valid values=[apiKey, http, oauth2, openIdConnect]", () -> x.setType("invalid"));
			assertDoesNotThrow(() -> x.setType("apiKey"));
			assertDoesNotThrow(() -> x.setType("http"));
			assertDoesNotThrow(() -> x.setType("oauth2"));
			assertDoesNotThrow(() -> x.setType("openIdConnect"));
			assertDoesNotThrow(() -> y.setType("invalid"));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<SecuritySchemeInfo> TESTER =
			testBean(bean())
			.props("type,description,name,in,scheme,bearerFormat,flows")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<SecuritySchemeInfo> TESTER =
			testBean(
				bean()
					.set("bearerFormat", "a")
					.set("description", "b")
					.set("flows", oAuthFlows().setImplicit(oAuthFlow().setAuthorizationUrl("c")))
					.set("in", "d")
					.set("name", "e")
					.set("openIdConnectUrl", "f")
					.set("scheme", "g")
					.set("type", "h")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("bearerFormat,description,flows{implicit{authorizationUrl}},in,name,openIdConnectUrl,scheme,type,x1,x2")
			.vals("a,b,{{c}},d,e,f,g,h,x1a,<null>")
			.json("{bearerFormat:'a',description:'b',flows:{implicit:{authorizationUrl:'c'}},'in':'d',name:'e',openIdConnectUrl:'f',scheme:'g',type:'h',x1:'x1a'}")
			.string("{'bearerFormat':'a','description':'b','flows':{'implicit':{'authorizationUrl':'c'}},'in':'d','name':'e','openIdConnectUrl':'f','scheme':'g','type':'h','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "bearerFormat", "description", "flows", "in", "name", "openIdConnectUrl", "scheme", "type", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"bearerFormat,description,flows{implicit{authorizationUrl}},in,name,openIdConnectUrl,scheme,type,x1,x2",
				"a,b,{{c}},d,e,f,g,h,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"bearerFormat,description,flows,in,name,openIdConnectUrl,scheme,type,x1,x2",
				"String,String,OAuthFlow,String,String,String,String,String,String,<null>"
			);
		}

		@Test void c10_nullPropertyValue() {
			assertThrows(IllegalArgumentException.class, ()->bean().get(null));
			assertThrows(IllegalArgumentException.class, ()->bean().get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->bean().set(null, "a"));
		}
	}

	//---------------------------------------------------------------------------------------------
	// Helper methods
	//---------------------------------------------------------------------------------------------

	private static SecuritySchemeInfo bean() {
		return securitySchemeInfo();
	}
}