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
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link SecurityRequirement}.
 */
class SecurityRequirement_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<SecurityRequirement> TESTER =
			testBean(
				bean()
					.setRequirements(map("a1", l("a2")))
			)
			.props("requirements{a1}")
			.vals("{[a2]}")
			.json("{requirements:{a1:['a2']}}")
			.string("{'requirements':{'a1':['a2']}}".replace('\'','"'))
		;

		@Test void a01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void a02_copy() {
			var t = new SecurityRequirement(TESTER.bean());
			assertNotSame(TESTER.bean(), t);
			assertBean(t, TESTER.props(), TESTER.vals());
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
			assertList(TESTER.bean().keySet(), "requirements");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_addMethods() {
			assertBean(
				bean()
					.addRequirement("a1", "a2", "a3")
					.setApiKeyAuth("b"),
				"requirements{a1,b}",
				"{[a2,a3],[]}"
			);
		}

		@Test void a10_getAndSetUsingPropertyName() {
			var x = bean()
				.set("requirements", m("a1", l("a2")))
				.set("b1", l("b2"));

			assertBean(x, "requirements{a1},b1", "{[a2]},[b2]");
			assertBean(x.get("requirements", Map.class), "a1", "[a2]");
			assertList((List<?>)x.get("b1", List.class), "b2");
		}

		@Test void a11_asMap() {
			assertBean(
				bean()
					.set("a1", l("a2"))
					.set("x1", "x1a")
					.asMap(),
				"a1,x1",
				"[a2],x1a"
			);
		}

		@Test void a12_extraKeys() {
			var x = bean().set("x1", "x1a").set("x2", "x2a");
			assertList(x.extraKeys(), "x1", "x2");
			assertEmpty(bean().extraKeys());
		}

		@Test void a13_strictMode() {
			assertThrows(RuntimeException.class, () -> bean().strict().set("foo", "bar"));
			assertDoesNotThrow(() -> bean().set("foo", "bar"));

			assertFalse(bean().isStrict());
			assertTrue(bean().strict().isStrict());
			assertFalse(bean().strict(false).isStrict());
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<SecurityRequirement> TESTER =
			testBean(bean())
			.props("requirements")
			.vals("<null>")
			.json("{}")
			.string("{}")
		;

		@Test void b01_gettersAndSetters() {
			TESTER.assertGettersAndSetters();
		}

		@Test void b02_copy() {
			var t = new SecurityRequirement(TESTER.bean());
			assertNotSame(TESTER.bean(), t);
			assertBean(t, TESTER.props(), TESTER.vals());
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
		private static final BeanTester<SecurityRequirement> TESTER =
			testBean(
				bean()
					.set("a1", l("a2", "a3"))
					.set("b1", l("b2"))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("a1{#{toString}},b1{#{toString}},x1,x2")
			.vals("{[{a2},{a3}]},{[{b2}]},x1a,<null>")
			.json("{a1:['a2','a3'],b1:['b2'],x1:'x1a'}")
			.string("{'a1':['a2','a3'],'b1':['b2'],'x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "a1", "b1", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"a1{#{toString}},b1{#{toString}},x1,x2",
				"{[{a2},{a3}]},{[{b2}]},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> scn(obj.get(prop, Object.class)),
				"a1,b1,x1,x2",
				"ArrayList,ArrayList,String,<null>"
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

	private static SecurityRequirement bean() {
		return securityRequirement();
	}
}