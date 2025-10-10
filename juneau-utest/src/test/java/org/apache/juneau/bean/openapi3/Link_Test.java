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
 * Testcase for {@link Link}.
 */
class Link_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Link> TESTER =
			testBean(
				bean()
					.setDescription("a")
					.setOperationId("b")
					.setOperationRef("c")
					.setParameters(map("d1", "d2"))
					.setRequestBody("e")
					.setServer(server().setUrl(URI.create("f")))
			)
			.props("description,operationId,operationRef,parameters{d1},requestBody,server{url}")
			.vals("a,b,c,{d2},e,{f}")
			.json("{description:'a',operationId:'b',operationRef:'c',parameters:{d1:'d2'},requestBody:'e',server:{url:'f'}}")
			.string("{'description':'a','operationId':'b','operationRef':'c','parameters':{'d1':'d2'},'requestBody':'e','server':{'url':'f'}}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "description", "operationId", "operationRef", "parameters", "requestBody", "server");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_addMethods() {
			assertBean(
				bean()
					.addParameter("a1", "a2"),
				"parameters{a1}",
				"{a2}"
			);
		}

		@Test void a10_asMap() {
			assertBean(
				bean()
					.setOperationId("a")
					.set("x1", "x1a")
					.asMap(),
				"operationId,x1",
				"a,x1a"
			);
		}

		@Test void a11_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a12_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Link> TESTER =
			testBean(bean())
			.props("operationRef,operationId,description,requestBody,server,parameters")
			.vals("<null>,<null>,<null>,<null>,<null>,<null>")
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
		private static final BeanTester<Link> TESTER =
			testBean(
				bean()
					.set("description", "a")
					.set("operationId", "b")
					.set("operationRef", "c")
					.set("parameters", map("d1", "d2"))
					.set("requestBody", "e")
					.set("server", server().setUrl(URI.create("f")))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("description,operationId,operationRef,parameters{d1},requestBody,server{url},x1,x2")
			.vals("a,b,c,{d2},e,{f},x1a,<null>")
			.json("{description:'a',operationId:'b',operationRef:'c',parameters:{d1:'d2'},requestBody:'e',server:{url:'f'},x1:'x1a'}")
			.string("{'description':'a','operationId':'b','operationRef':'c','parameters':{'d1':'d2'},'requestBody':'e','server':{'url':'f'},'x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "description", "operationId", "operationRef", "parameters", "requestBody", "server", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"description,operationId,operationRef,parameters{d1},requestBody,server{url},x1,x2",
				"a,b,c,{d2},e,{f},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"description,operationId,operationRef,parameters,requestBody,server,x1,x2",
				"String,String,String,LinkedHashMap,String,Server,String,<null>"
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

	private static Link bean() {
		return link();
	}
}
