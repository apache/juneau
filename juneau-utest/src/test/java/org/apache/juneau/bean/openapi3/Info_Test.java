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
 * Testcase for {@link Info}.
 */
class Info_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Info> TESTER =
			testBean(
				bean()
					.setContact(contact().setEmail("a1").setName("a2").setUrl(URI.create("a3")))
					.setDescription("b")
					.setLicense(license().setName("c1").setUrl(URI.create("c2")))
					.setTermsOfService("d")
					.setTitle("e")
					.setVersion("f")
			)
			.props("contact{email,name,url},description,license{name,url},termsOfService,title,version")
			.vals("{a1,a2,a3},b,{c1,c2},d,e,f")
			.json("{contact:{email:'a1',name:'a2',url:'a3'},description:'b',license:{name:'c1',url:'c2'},termsOfService:'d',title:'e',version:'f'}")
			.string("{'contact':{'email':'a1','name':'a2','url':'a3'},'description':'b','license':{'name':'c1','url':'c2'},'termsOfService':'d','title':'e','version':'f'}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "contact", "description", "license", "termsOfService", "title", "version");
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Info> TESTER =
			testBean(bean())
			.props("title,version,description,termsOfService,contact,license")
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
					.set("termsOfService", "d")
					.set("title", "e")
					.set("version", "f")
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("contact{name},description,license{name},termsOfService,title,version,x1,x2")
			.vals("{a},b,{c},d,e,f,x1a,<null>")
			.json("{contact:{name:'a'},description:'b',license:{name:'c'},termsOfService:'d',title:'e',version:'f',x1:'x1a'}")
			.string("{'contact':{'name':'a'},'description':'b','license':{'name':'c'},'termsOfService':'d','title':'e','version':'f','x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "contact", "description", "license", "termsOfService", "title", "version", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"contact{name},description,license{name},termsOfService,title,version,x1,x2",
				"{a},b,{c},d,e,f,x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> simpleClassNameOf(obj.get(prop, Object.class)),
				"contact,description,license,termsOfService,title,version,x1,x2",
				"Contact,String,License,String,String,String,String,<null>"
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
					.setTitle("a")
					.set("x1", "x1a")
					.asMap(),
				"title,x1",
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

	private static Info bean() {
		return info();
	}
}
