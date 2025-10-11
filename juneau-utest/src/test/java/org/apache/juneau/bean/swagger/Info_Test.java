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
package org.apache.juneau.bean.swagger;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.bean.swagger.SwaggerBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Info}.
 */
class Info_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Info> TESTER =
			testBean(
				bean()
					.setContact(contact().setName("a"))
					.setDescription("b")
					.setLicense(license().setName("c"))
					.setSiteName("d")
					.setTermsOfService("e")
					.setTitle("f")
					.setVersion("g")
			)
			.props("contact{name},description,license{name},siteName,termsOfService,title,version")
			.vals("{a},b,{c},d,e,f,g")
			.json("{contact:{name:'a'},description:'b',license:{name:'c'},siteName:'d',termsOfService:'e',title:'f',version:'g'}")
			.string("{'contact':{'name':'a'},'description':'b','license':{'name':'c'},'siteName':'d','termsOfService':'e','title':'f','version':'g'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "contact", "description", "license", "siteName", "termsOfService", "title", "version");
		}

		@Test void a08_nullParameters() {
			var x = bean();
			assertThrows(IllegalArgumentException.class, () -> x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, () -> x.set(null, "value"));
		}

		@Test void a09_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.setTitle("b")
					.setVersion("c")
					.set("x1", "x1a")
					.asMap(),
				"description,title,version,x1",
				"a,b,c,x1a"
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
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Info> TESTER =
			testBean(bean())
			.props("description,title,version,termsOfService,contact,license")
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

		private static final BeanTester<Info> TESTER =
			testBean(
				bean()
					.set("contact", contact().setName("a"))
					.set("description", "b")
					.set("license", license().setName("c"))
					.set("siteName", "d")
					.set("termsOfService", "e")
					.set("title", "f")
					.set("version", "g")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("contact{name},description,license{name},siteName,termsOfService,title,version,x1,x2")
			.vals("{a},b,{c},d,e,f,g,x1a,<null>")
			.json("{contact:{name:'a'},description:'b',license:{name:'c'},siteName:'d',termsOfService:'e',title:'f',version:'g',x1:'x1a'}")
			.string("{'contact':{'name':'a'},'description':'b','license':{'name':'c'},'siteName':'d','termsOfService':'e','title':'f','version':'g','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "contact", "description", "license", "siteName", "termsOfService", "title", "version", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"contact{name},description,license{name},siteName,termsOfService,title,version,x1,x2",
				"{a},b,{c},d,e,f,g,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"contact,description,license,siteName,termsOfService,title,version,x1,x2",
				"Contact,String,License,String,String,String,String,String,<null>"
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

	private static Info bean() {
		return info();
	}
}