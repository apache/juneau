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
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Testcase for {@link Response}.
 */
class Response_Test extends TestBase {

	@Nested class A_basicTests extends TestBase {

		private static final BeanTester<Response> TESTER =
			testBean(
				bean()
					.setContent(map("a1", mediaType().setSchema(schemaInfo().setType("a2"))))
					.setDescription("b")
					.setHeaders(map("c1", headerInfo(schemaInfo().setType("c2"))))
					.setLinks(map("d1", link().setOperationId("d2")))
			)
			.props("content{a1{schema{type}}},description,headers{c1{schema{type}}},links{d1{operationId}}")
			.vals("{{{a2}}},b,{{{c2}}},{{d2}}")
			.json("{content:{a1:{schema:{type:'a2'}}},description:'b',headers:{c1:{schema:{type:'c2'}}},links:{d1:{operationId:'d2'}}}")
			.string("{'content':{'a1':{'schema':{'type':'a2'}}},'description':'b','headers':{'c1':{'schema':{'type':'c2'}}},'links':{'d1':{'operationId':'d2'}}}".replace('\'','"'))
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
			assertList(TESTER.bean().keySet(), "content", "description", "headers", "links");
		}

		@Test void a08_otherGettersAndSetters() {
			// No Collection variants for Response setters

			// Test special getters
			var x = bean()
				.setHeaders(map(
					"a1", headerInfo(schemaInfo("a2")),
					"a3", headerInfo(schemaInfo("a4"))
				))
				.setContent(map(
					"b1", mediaType().setSchema(schemaInfo("b2")),
					"b3", mediaType().setSchema(schemaInfo("b4"))
				))
				.setLinks(map(
					"c1", link().setOperationId("c2"),
					"c3", link().setOperationId("c4")
				));

			assertBean(x.getHeader("a1"), "schema{type}", "{a2}");
			assertBean(x.getContent("b1"), "schema{type}", "{b2}");
			assertBean(x.getLink("c1"), "operationId", "c2");
		}

		@Test void a09_nullParameters() {
			var x = bean();

			assertThrows(IllegalArgumentException.class, ()->x.getHeader(null));
			assertThrows(IllegalArgumentException.class, ()->x.getContent(null));
			assertThrows(IllegalArgumentException.class, ()->x.getLink(null));
			assertThrows(IllegalArgumentException.class, ()->x.get(null, String.class));
			assertThrows(IllegalArgumentException.class, ()->x.set(null, "value"));
			assertThrows(IllegalArgumentException.class, ()->x.addHeader(null, headerInfo()));
			assertThrows(IllegalArgumentException.class, ()->x.addHeader("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addContent(null, mediaType()));
			assertThrows(IllegalArgumentException.class, ()->x.addContent("a", null));
			assertThrows(IllegalArgumentException.class, ()->x.addLink(null, link()));
			assertThrows(IllegalArgumentException.class, ()->x.addLink("a", null));
		}

		@Test void a10_addMethods() {
			assertBean(
				bean()
					.addHeader("a1", headerInfo(schemaInfo("a2")))
					.addContent("b1", mediaType().setSchema(schemaInfo("b2")))
					.addLink("c1", link().setOperationId("c2")),
				"headers{a1{schema{type}}},content{b1{schema{type}}},links{c1{operationId}}",
				"{{{a2}}},{{{b2}}},{{c2}}"
			);
		}

		@Test void a11_asMap() {
			assertBean(
				bean()
					.setDescription("a")
					.set("x1", "x1a")
					.asMap(),
				"description,x1",
				"a,x1a"
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

		@Test void a14_getHeader() {
			// Test getHeader with null headers map (covers the null check branch)
			var y = bean();
			assertNull(y.getHeader("nonexistent"));

			// Test with headers set
			var x = bean()
				.setHeaders(map("header1", headerInfo().setDescription("Test header")));

			assertNotNull(x.getHeader("header1"));
			assertEquals("Test header", x.getHeader("header1").getDescription());
			assertNull(x.getHeader("nonexistent"));

			// Test null name parameter
			assertThrows(IllegalArgumentException.class, () -> x.getHeader(null));
		}

		@Test void a15_getContent() {
			// Test getContent with null content map (covers the null check branch)
			var y = bean();
			assertNull(y.getContent("application/json"));

			// Test with content set
			var x = bean()
				.setContent(map("application/json", mediaType().setSchema(schemaInfo().setType("object"))));

			assertNotNull(x.getContent("application/json"));
			assertEquals("object", x.getContent("application/json").getSchema().getType());
			assertNull(x.getContent("application/xml"));

			// Test null mediaType parameter
			assertThrows(IllegalArgumentException.class, () -> x.getContent(null));
		}

		@Test void a16_getLink() {
			// Test getLink with null links map (covers the null check branch)
			var y = bean();
			assertNull(y.getLink("nonexistent"));

			// Test with links set
			var x = bean()
				.setLinks(map("link1", link().setOperationId("getUser")));

			assertNotNull(x.getLink("link1"));
			assertEquals("getUser", x.getLink("link1").getOperationId());
			assertNull(x.getLink("nonexistent"));

			// Test null name parameter
			assertThrows(IllegalArgumentException.class, () -> x.getLink(null));
		}
	}

	@Nested class B_emptyTests extends TestBase {

		private static final BeanTester<Response> TESTER =
			testBean(bean())
			.props("description,headers,content,links")
			.vals("<null>,<null>,<null>,<null>")
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
		private static final BeanTester<Response> TESTER =
			testBean(
				bean()
					.set("content", m("a1", mediaType().setSchema(schemaInfo("a2"))))
					.set("description", "b")
					.set("headers", m("c1", headerInfo(schemaInfo("c2"))))
					.set("links", m("d1", link().setOperationId("d2")))
					.set("x1", "x1a")
					.set("x2", null)
			)
			.props("content{a1{schema{type}}},description,headers{c1{schema{type}}},links{d1{operationId}},x1,x2")
			.vals("{{{a2}}},b,{{{c2}}},{{d2}},x1a,<null>")
			.json("{content:{a1:{schema:{type:'a2'}}},description:'b',headers:{c1:{schema:{type:'c2'}}},links:{d1:{operationId:'d2'}},x1:'x1a'}")
			.string("{'content':{'a1':{'schema':{'type':'a2'}}},'description':'b','headers':{'c1':{'schema':{'type':'c2'}}},'links':{'d1':{'operationId':'d2'}},'x1':'x1a'}".replace('\'', '"'))
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
			assertList(TESTER.bean().keySet(), "content", "description", "headers", "links", "x1", "x2");
		}

		@Test void c08_get() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> obj.get(prop, Object.class),
				"content{a1{schema{type}}},description,headers{c1{schema{type}}},links{d1{operationId}},x1,x2",
				"{{{a2}}},b,{{{c2}}},{{d2}},x1a,<null>"
			);
		}

		@Test void c09_getTypes() {
			assertMapped(
				TESTER.bean(), (obj,prop) -> cns(obj.get(prop, Object.class)),
				"content,description,headers,links,x1,x2",
				"LinkedHashMap,String,LinkedHashMap,LinkedHashMap,String,<null>"
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

	private static Response bean() {
		return response();
	}
}