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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@code juneau-bean-jsonapi} wire beans.
 */
class JsonApi_RoundTrip_Test {

	private static final JsonSerializer SER = JsonSerializer.DEFAULT;
	private static final JsonParser PAR = JsonParser.DEFAULT;

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = SER.write(bean);
		var copy = PAR.read(j1, type);
		var j2 = SER.write(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Nested
	class A_Link {

		@Test
		void a01_allFieldsRoundTrip() {
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
		void a02_hrefConstructorAndToString() {
			var link = new JsonApiLink("/u");
			assertEquals("/u", link.getHref());
			assertNotNull(link.toString());
		}

		@Test
		void a03_emptyConstructorAllFieldsNull() {
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
		void b01_roundTrip() {
			var v = new JsonApiVersion("1.1").setMeta(map("k", "v"));
			assertJsonRoundTrip(v, JsonApiVersion.class);
			assertEquals("1.1", v.getVersion());
		}

		@Test
		void b02_emptyConstructor() {
			var v = new JsonApiVersion();
			assertNull(v.getVersion());
			assertNull(v.getMeta());
			assertNotNull(v.toString());
		}
	}

	@Nested
	class C_ResourceIdentifier {

		@Test
		void c01_roundTrip() {
			var rid = new JsonApiResourceIdentifier("people", "9").setMeta(map("k", "v"));
			assertJsonRoundTrip(rid, JsonApiResourceIdentifier.class);
		}

		@Test
		void c02_settersAndDefaultConstructor() {
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
		void d01_singleIdentifierDataRoundTrip() {
			var rel = new JsonApiRelationship()
				.setData(new JsonApiResourceIdentifier("people", "9"))
				.setLinks(new LinkedHashMap<>(Map.of("self", "/articles/1/relationships/author")))
				.setMeta(map("k", "v"));
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
			assertNotNull(rel.getData());
		}

		@Test
		void d02_manyIdentifiersDataRoundTrip() {
			var rel = new JsonApiRelationship()
				.setData(list(
					new JsonApiResourceIdentifier("comments", "5"),
					new JsonApiResourceIdentifier("comments", "12")
				));
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
		}

		@Test
		void d03_linksAcceptStringOrLinkObject() {
			var rel = new JsonApiRelationship()
				.setLinks(new LinkedHashMap<>(Map.of(
					"self", "/articles/1/relationships/author",
					"related", new JsonApiLink("/articles/1/author").setTitle("Author")
				)));
			var j = SER.write(rel);
			// One value should be a string, the other a JSON object.
			assertTrue(j.contains("\"self\":\"/articles/1/relationships/author\""), () -> j);
			assertTrue(j.contains("\"related\":{"), () -> j);
			var back = PAR.read(j, JsonApiRelationship.class);
			assertInstanceOf(String.class, back.getLinks().get("self"));
			assertInstanceOf(JsonApiLink.class, back.getLinks().get("related"));
		}

		@Test
		void d04_nullDataIsPreserved() {
			var rel = new JsonApiRelationship().setData((Object) null).setMeta(map("k", "v"));
			assertNull(rel.getData());
			assertJsonRoundTrip(rel, JsonApiRelationship.class);
		}

		@Test
		void d05_toStringDoesNotThrow() {
			assertNotNull(new JsonApiRelationship().toString());
		}
	}

	@Nested
	class E_ErrorAndSource {

		@Test
		void e01_allFieldsRoundTrip() {
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
		void e02_sourceEmptyConstructorAndToString() {
			var s = new JsonApiErrorSource();
			assertNull(s.getPointer());
			assertNull(s.getParameter());
			assertNull(s.getHeader());
			assertNotNull(s.toString());
		}

		@Test
		void e03_errorEmptyConstructorAndToString() {
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
		void f01_allFieldsRoundTrip() {
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
		void f02_putAttributeSetsBackingMapIfNull() {
			var r = new JsonApiResource().putAttribute("k", "v");
			assertEquals("v", r.getAttributes().get("k"));
		}

		@Test
		void f03_putRelationshipSetsBackingMapIfNull() {
			var r = new JsonApiResource().putRelationship("author",
				new JsonApiRelationship().setData(new JsonApiResourceIdentifier("people", "9")));
			assertNotNull(r.getRelationships().get("author"));
		}

		@Test
		void f04_typeIsPlainStringNotDiscriminator() {
			// JSON:API ‘type’ is just data. Round-tripping a custom value must not require a Java dictionary entry.
			var r = new JsonApiResource("very-custom-resource-not-in-any-registry", "1");
			var j = SER.write(r);
			assertTrue(j.contains("\"type\":\"very-custom-resource-not-in-any-registry\""), () -> j);
			var back = PAR.read(j, JsonApiResource.class);
			assertEquals("very-custom-resource-not-in-any-registry", back.getType());
		}

		@Test
		void f05_settersAndDefaultConstructor() {
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
		void g01_singleResourceRoundTrip() {
			var doc = new JsonApiDocument()
				.setData(new JsonApiResource("articles", "1"))
				.setJsonapi(new JsonApiVersion("1.1"));
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void g02_manyResourcesRoundTrip() {
			var doc = new JsonApiDocument().setData(list(
				new JsonApiResource("articles", "1"),
				new JsonApiResource("articles", "2")
			));
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void g03_errorsDocumentRoundTrip() {
			var doc = new JsonApiDocument().addErrors(
				new JsonApiError().setStatus("422").setTitle("Bad")
			);
			assertJsonRoundTrip(doc, JsonApiDocument.class);
		}

		@Test
		void g04_compoundDocumentExampleRoundTrip() {
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
		void g05_setDataAllOverloads() {
			var doc1 = new JsonApiDocument().setData((Object) "x");
			assertEquals("x", doc1.getData());
			var doc2 = new JsonApiDocument().setData(new JsonApiResource("a", "1"));
			assertInstanceOf(JsonApiResource.class, doc2.getData());
			var doc3 = new JsonApiDocument().setData(list(new JsonApiResource("a", "1")));
			assertInstanceOf(List.class, doc3.getData());
		}

		@Test
		void g06_setIncludedAndErrorsReplacesBackingList() {
			var doc = new JsonApiDocument()
				.setIncluded(list(new JsonApiResource("a", "1")))
				.setErrors(list(new JsonApiError().setTitle("x")));
			assertEquals(1, doc.getIncluded().size());
			assertEquals("a", doc.getIncluded().get(0).getType());
			assertEquals("1", doc.getIncluded().get(0).getId());
			assertEquals(1, doc.getErrors().size());
			assertEquals("x", doc.getErrors().get(0).getTitle());
		}

		@Test
		void g07_validateThrowsWhenDataAndErrorsCoexist() {
			var doc = new JsonApiDocument()
				.setData(new JsonApiResource("a", "1"))
				.addErrors(new JsonApiError());
			assertThrows(IllegalStateException.class, doc::validate);
		}

		@Test
		void g08_validateThrowsWhenEmpty() {
			var doc = new JsonApiDocument();
			assertThrows(IllegalStateException.class, doc::validate);
		}

		@Test
		void g09_validateOkWithJustMeta() {
			var doc = new JsonApiDocument().setMeta(map("k", "v"));
			assertSame(doc, doc.validate());
		}

		@Test
		void g10_validateOkWithJustData() {
			var doc = new JsonApiDocument().setData(new JsonApiResource("a", "1"));
			assertSame(doc, doc.validate());
		}

		@Test
		void g11_validateOkWithJustErrors() {
			var doc = new JsonApiDocument().addErrors(new JsonApiError().setTitle("x"));
			assertSame(doc, doc.validate());
		}

		@Test
		void g12_addErrorsAndAddIncludedAppendToBackingLists() {
			var doc = new JsonApiDocument()
				.addErrors(new JsonApiError().setTitle("a"))
				.addErrors(new JsonApiError().setTitle("b"));
			assertEquals(2, doc.getErrors().size());
			assertEquals("a", doc.getErrors().get(0).getTitle());
			assertEquals("b", doc.getErrors().get(1).getTitle());
			doc.addIncluded(new JsonApiResource("a", "1")).addIncluded(new JsonApiResource("a", "2"));
			assertEquals(2, doc.getIncluded().size());
			assertEquals("1", doc.getIncluded().get(0).getId());
			assertEquals("2", doc.getIncluded().get(1).getId());
		}

		@Test
		void g13_linksEmptyConstructorTrip() {
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
		void h01_unswapNullIsNull() {
			var swap = new JsonApiLinkOrStringSwap();
			assertNull(swap.unswap(null, null, null));
		}

		@Test
		void h02_swapIsIdentity() {
			var swap = new JsonApiLinkOrStringSwap();
			var input = new Object();
			assertSame(input, swap.swap(null, input));
		}

		@Test
		void h03_unswapNonMapPassesThrough() {
			var swap = new JsonApiLinkOrStringSwap();
			var input = "raw-string";
			assertSame(input, swap.unswap(null, input, null));
		}

		@Test
		void h04_unswapMapWithScalarEntryPassesValueThrough() {
			// A nested map (object) that itself contains a scalar at the "x" key still parses to a JsonApiLink
			// (the per-entry rule); a more direct scalar-passthrough is hit via the JsonApiDocument case below.
			var json = "{\"links\":{\"self\":\"/x\"}}";
			var rel = JsonParser.DEFAULT.read(json, JsonApiRelationship.class);
			assertEquals("/x", rel.getLinks().get("self"));
		}
	}
}
