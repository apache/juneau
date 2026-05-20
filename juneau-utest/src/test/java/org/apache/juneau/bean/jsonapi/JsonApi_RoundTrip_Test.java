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
package org.apache.juneau.bean.jsonapi;

import static org.apache.juneau.commons.utils.CollectionUtils.list;
import static org.apache.juneau.commons.utils.CollectionUtils.map;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@code juneau-bean-jsonapi} wire beans.
 */
class JsonApi_RoundTrip_Test {

	private static final JsonSerializer SER = JsonSerializer.DEFAULT;
	private static final JsonParser PAR = JsonParser.DEFAULT;

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = SER.serialize(bean);
		var copy = PAR.parse(j1, type);
		var j2 = SER.serialize(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Nested
	class A_Link {

		@Test
		void allFields_roundTrip() {
			var link = new JsonApiLink()
				.setHref("https://example.com/articles/1")
				.setRel("canonical")
				.setDescribedby("https://example.com/schemas/article")
				.setTitle("Article 1")
				.setType("application/vnd.api+json")
				.setHreflang("en")
				.setMeta(map("k", "v"));
			assertJsonRoundTrip(link, JsonApiLink.class);
		}

		@Test
		void hrefConstructor_andToString() {
			var link = new JsonApiLink("/u");
			assertEquals("/u", link.getHref());
			assertNotNull(link.toString());
		}

		@Test
		void emptyConstructor_allFieldsNull() {
			var l = new JsonApiLink();
			assertNull(l.getHref());
			assertNull(l.getRel());
			assertNull(l.getDescribedby());
			assertNull(l.getTitle());
			assertNull(l.getType());
			assertNull(l.getHreflang());
			assertNull(l.getMeta());
		}
	}

	@Nested
	class B_Version {

		@Test
		void roundTrip() {
			var v = new JsonApiVersion("1.1").setMeta(map("k", "v"));
			assertJsonRoundTrip(v, JsonApiVersion.class);
			assertEquals("1.1", v.getVersion());
		}

		@Test
		void emptyConstructor() {
			var v = new JsonApiVersion();
			assertNull(v.getVersion());
			assertNull(v.getMeta());
			assertNotNull(v.toString());
		}
	}

	@Nested
	class C_ResourceIdentifier {

		@Test
		void roundTrip() {
			var rid = new JsonApiResourceIdentifier("people", "9").setMeta(map("k", "v"));
			assertJsonRoundTrip(rid, JsonApiResourceIdentifier.class);
		}

		@Test
		void settersAndDefaultConstructor() {
			var rid = new JsonApiResourceIdentifier()
				.setType("articles")
				.setId("1")
				.setMeta(map("m", 1));
			assertEquals("articles", rid.getType());
			assertEquals("1", rid.getId());
			assertNotNull(rid.getMeta());
			assertNotNull(rid.toString());
		}
	}

	@Nested
	class D_Relationship {

		@Test
		void singleIdentifierData_roundTrip() {
			var rel = new JsonApiRelationship()
				.setData(new JsonApiResourceIdentifier("people", "9"))
				.setLinks(new LinkedHashMap<>(Map.of("self", "/articles/1/relationships/author")))
				.setMeta(map("k", "v"));
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
			assertNotNull(rel.getData());
		}

		@Test
		void manyIdentifiersData_roundTrip() {
			var rel = new JsonApiRelationship()
				.setData(list(
					new JsonApiResourceIdentifier("comments", "5"),
					new JsonApiResourceIdentifier("comments", "12")
				));
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
		}

		@Test
		void linksAcceptStringOrLinkObject() {
			var rel = new JsonApiRelationship()
				.setLinks(new LinkedHashMap<>(Map.of(
					"self", "/articles/1/relationships/author",
					"related", new JsonApiLink("/articles/1/author").setTitle("Author")
				)));
			var j = SER.serialize(rel);
			// One value should be a string, the other a JSON object.
			assertTrue(j.contains("\"self\":\"/articles/1/relationships/author\""), () -> j);
			assertTrue(j.contains("\"related\":{"), () -> j);
			var back = PAR.parse(j, JsonApiRelationship.class);
			assertInstanceOf(String.class, back.getLinks().get("self"));
			assertInstanceOf(JsonApiLink.class, back.getLinks().get("related"));
		}

		@Test
		void nullData_isPreserved() {
			var rel = new JsonApiRelationship().setData((Object) null).setMeta(map("k", "v"));
			assertNull(rel.getData());
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
		}

		@Test
		void toString_doesNotThrow() {
			assertNotNull(new JsonApiRelationship().toString());
		}
	}

	@Nested
	class E_ErrorAndSource {

		@Test
		void allFields_roundTrip() {
			var src = new JsonApiErrorSource()
				.setPointer("/data/attributes/title")
				.setParameter("filter[title]")
				.setHeader("If-Match");
			var err = new JsonApiError()
				.setId("err-1")
				.setLinks(new LinkedHashMap<>(Map.of("about", "/errors/err-1")))
				.setStatus("422")
				.setCode("E422")
				.setTitle("Invalid Attribute")
				.setDetail("Title must contain at least three characters.")
				.setSource(src)
				.setMeta(map("k", "v"));
			assertJsonRoundTrip(err, JsonApiError.class);
		}

		@Test
		void source_emptyConstructorAndToString() {
			var s = new JsonApiErrorSource();
			assertNull(s.getPointer());
			assertNull(s.getParameter());
			assertNull(s.getHeader());
			assertNotNull(s.toString());
		}

		@Test
		void error_emptyConstructorAndToString() {
			var e = new JsonApiError();
			assertNull(e.getId());
			assertNull(e.getLinks());
			assertNull(e.getStatus());
			assertNull(e.getCode());
			assertNull(e.getTitle());
			assertNull(e.getDetail());
			assertNull(e.getSource());
			assertNull(e.getMeta());
			assertNotNull(e.toString());
		}
	}

	@Nested
	class F_Resource {

		@Test
		void allFields_roundTrip() {
			var res = new JsonApiResource("articles", "1")
				.setAttributes(JsonMap.of("title", "JSON:API paints my bikeshed!"))
				.putAttribute("body", "lorem")
				.putRelationship("author",
					new JsonApiRelationship().setData(new JsonApiResourceIdentifier("people", "9"))
				)
				.setLinks(new LinkedHashMap<>(Map.of("self", "/articles/1")))
				.setMeta(map("rev", "abc"));
			assertJsonRoundTrip(res, JsonApiResource.class);
		}

		@Test
		void putAttribute_setsBackingMapIfNull() {
			var r = new JsonApiResource().putAttribute("k", "v");
			assertEquals("v", r.getAttributes().get("k"));
		}

		@Test
		void putRelationship_setsBackingMapIfNull() {
			var r = new JsonApiResource().putRelationship("author",
				new JsonApiRelationship().setData(new JsonApiResourceIdentifier("people", "9")));
			assertNotNull(r.getRelationships().get("author"));
		}

		@Test
		void typeIsPlainString_notDiscriminator() {
			// JSON:API ‘type’ is just data. Round-tripping a custom value must not require a Java dictionary entry.
			var r = new JsonApiResource("very-custom-resource-not-in-any-registry", "1");
			var j = SER.serialize(r);
			assertTrue(j.contains("\"type\":\"very-custom-resource-not-in-any-registry\""), () -> j);
			var back = PAR.parse(j, JsonApiResource.class);
			assertEquals("very-custom-resource-not-in-any-registry", back.getType());
		}

		@Test
		void settersAndDefaultConstructor() {
			var r = new JsonApiResource()
				.setType("a")
				.setId("1")
				.setAttributes(map("k", "v"))
				.setRelationships(map("r", new JsonApiRelationship()))
				.setLinks(new LinkedHashMap<>(Map.of("self", "/a/1")))
				.setMeta(map("m", 1));
			assertEquals("a", r.getType());
			assertEquals("1", r.getId());
			assertNotNull(r.getAttributes());
			assertNotNull(r.getRelationships());
			assertNotNull(r.getLinks());
			assertNotNull(r.getMeta());
			assertNotNull(r.toString());
		}
	}

	@Nested
	class G_Document {

		@Test
		void singleResource_roundTrip() {
			var doc = new JsonApiDocument()
				.setData(new JsonApiResource("articles", "1"))
				.setJsonapi(new JsonApiVersion("1.1"));
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void manyResources_roundTrip() {
			var doc = new JsonApiDocument().setData(list(
				new JsonApiResource("articles", "1"),
				new JsonApiResource("articles", "2")
			));
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void errorsDocument_roundTrip() {
			var doc = new JsonApiDocument().addErrors(
				new JsonApiError().setStatus("422").setTitle("Bad")
			);
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void compoundDocumentExample_roundTrip() {
			// Spec example: top-level data + included.
			var author = new JsonApiResource("people", "9")
				.setAttributes(JsonMap.of("firstName", "Dan", "lastName", "Gebhardt"))
				.setLinks(new LinkedHashMap<>(Map.of("self", "/people/9")));
			var comment1 = new JsonApiResource("comments", "5")
				.setAttributes(JsonMap.of("body", "First!"));
			var comment2 = new JsonApiResource("comments", "12")
				.setAttributes(JsonMap.of("body", "I like XML better"));
			var article = new JsonApiResource("articles", "1")
				.setAttributes(JsonMap.of("title", "JSON:API paints my bikeshed!"))
				.putRelationship("author",
					new JsonApiRelationship()
						.setData(new JsonApiResourceIdentifier("people", "9"))
						.setLinks(new LinkedHashMap<>(Map.of(
							"self", "/articles/1/relationships/author",
							"related", "/articles/1/author"
						)))
				)
				.putRelationship("comments",
					new JsonApiRelationship().setData(list(
						new JsonApiResourceIdentifier("comments", "5"),
						new JsonApiResourceIdentifier("comments", "12")
					))
				)
				.setLinks(new LinkedHashMap<>(Map.of("self", "/articles/1")));
			var doc = new JsonApiDocument()
				.setLinks(new LinkedHashMap<>(Map.of("self", "/articles")))
				.setJsonapi(new JsonApiVersion("1.1"))
				.setData(article)
				.addIncluded(author, comment1, comment2)
				.setMeta(map("count", 1));
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void setData_allOverloads() {
			var doc1 = new JsonApiDocument().setData((Object) "x");
			assertEquals("x", doc1.getData());
			var doc2 = new JsonApiDocument().setData(new JsonApiResource("a", "1"));
			assertInstanceOf(JsonApiResource.class, doc2.getData());
			var doc3 = new JsonApiDocument().setData(list(new JsonApiResource("a", "1")));
			assertInstanceOf(List.class, doc3.getData());
		}

		@Test
		void setIncludedAndErrors_replacesBackingList() {
			var doc = new JsonApiDocument()
				.setIncluded(list(new JsonApiResource("a", "1")))
				.setErrors(list(new JsonApiError().setTitle("x")));
			assertEquals(1, doc.getIncluded().size());
			assertEquals(1, doc.getErrors().size());
		}

		@Test
		void validate_throwsWhenDataAndErrorsCoexist() {
			var doc = new JsonApiDocument()
				.setData(new JsonApiResource("a", "1"))
				.addErrors(new JsonApiError());
			assertThrows(IllegalStateException.class, doc::validate);
		}

		@Test
		void validate_throwsWhenEmpty() {
			var doc = new JsonApiDocument();
			assertThrows(IllegalStateException.class, doc::validate);
		}

		@Test
		void validate_okWithJustMeta() {
			var doc = new JsonApiDocument().setMeta(map("k", "v"));
			assertSame(doc, doc.validate());
		}

		@Test
		void validate_okWithJustData() {
			var doc = new JsonApiDocument().setData(new JsonApiResource("a", "1"));
			assertSame(doc, doc.validate());
		}

		@Test
		void validate_okWithJustErrors() {
			var doc = new JsonApiDocument().addErrors(new JsonApiError().setTitle("x"));
			assertSame(doc, doc.validate());
		}

		@Test
		void addErrors_andAddIncluded_appendToBackingLists() {
			var doc = new JsonApiDocument()
				.addErrors(new JsonApiError().setTitle("a"))
				.addErrors(new JsonApiError().setTitle("b"));
			assertEquals(2, doc.getErrors().size());
			doc.addIncluded(new JsonApiResource("a", "1")).addIncluded(new JsonApiResource("a", "2"));
			assertEquals(2, doc.getIncluded().size());
		}

		@Test
		void links_emptyConstructorTrip() {
			var d = new JsonApiDocument();
			assertNull(d.getLinks());
			assertNull(d.getMeta());
			assertNull(d.getJsonapi());
			assertNull(d.getData());
			assertNull(d.getErrors());
			assertNull(d.getIncluded());
			assertNotNull(d.toString());
		}
	}

	@Nested
	class H_LinkSwap_directInvocation {

		@Test
		void unswap_nullIsNull() {
			var swap = new JsonApiLinkOrStringSwap();
			assertNull(swap.unswap(null, null, null));
		}

		@Test
		void swap_isIdentity() {
			var swap = new JsonApiLinkOrStringSwap();
			var input = new Object();
			assertSame(input, swap.swap(null, input));
		}

		@Test
		void unswap_nonMapPassesThrough() {
			var swap = new JsonApiLinkOrStringSwap();
			var input = "raw-string";
			assertSame(input, swap.unswap(null, input, null));
		}

		@Test
		void unswap_mapWithScalarEntry_passesValueThrough() {
			// A nested map (object) that itself contains a scalar at the "x" key still parses to a JsonApiLink
			// (the per-entry rule); a more direct scalar-passthrough is hit via the JsonApiDocument case below.
			var json = "{\"links\":{\"self\":\"/x\"}}";
			var rel = JsonParser.DEFAULT.parse(json, JsonApiRelationship.class);
			assertEquals("/x", rel.getLinks().get("self"));
		}
	}
}
