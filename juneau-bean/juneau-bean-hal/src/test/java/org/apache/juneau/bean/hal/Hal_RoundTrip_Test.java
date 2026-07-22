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
package org.apache.juneau.bean.hal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Round-trip tests for {@code juneau-bean-hal} wire beans.
 */
class Hal_RoundTrip_Test {

	private static final JsonSerializer HAL_JSON = JsonSerializer.DEFAULT;
	private static final JsonParser HAL_PARSER = JsonParser.DEFAULT;

	private static void assertJsonRoundTrip(Object bean, Class<?> type) {
		var j1 = HAL_JSON.write(bean);
		var copy = HAL_PARSER.read(j1, type);
		var j2 = HAL_JSON.write(copy);
		assertEquals(j1, j2, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + j1);
	}

	@Nested
	class A_HalLink {

		@Test
		void a01_allFieldsRoundTrip() {
			var link = new HalLink()
				.setHref("/orders/123")
				.setTemplated(false)
				.setType("application/hal+json")
				.setDeprecation("https://api.example.com/deprecations/links")
				.setName("acme")
				.setProfile("https://api.example.com/profiles/order")
				.setTitle("Order #123")
				.setHreflang("en");
			assertJsonRoundTrip(link, HalLink.class);
		}

		@Test
		void a02_hrefConstructorAndToStringWorks() {
			var link = new HalLink("/u");
			assertEquals("/u", link.getHref());
			assertNotNull(link.toString());
		}

		@Test
		void a03_emptyConstructorFieldsAreNull() {
			var link = new HalLink();
			assertNull(link.getHref());
			assertNull(link.getTemplated());
			assertNull(link.getType());
			assertNull(link.getDeprecation());
			assertNull(link.getName());
			assertNull(link.getProfile());
			assertNull(link.getTitle());
			assertNull(link.getHreflang());
		}
	}

	@Nested
	class B_HalResource_links {

		@Test
		void b01_singleLinkRoundTripsAsObject() {
			var r = new HalResource().addLink("self", new HalLink("/orders/123"));
			var j = HAL_JSON.write(r);
			// Single-link form ⇒ JSON object, not array.
			assertTrue(j.contains("\"self\":{"), () -> "Expected single-object form for self link: " + j);
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void b02_multiLinkRoundTripsAsArray() {
			var r = new HalResource()
				.addLinks("curies",
					new HalLink("https://acme.example/{rel}").setName("acme").setTemplated(true),
					new HalLink("https://acme.example/v2/{rel}").setName("acme2").setTemplated(true)
				);
			var j = HAL_JSON.write(r);
			assertTrue(j.contains("\"curies\":["), () -> "Expected multi-link array form for curies: " + j);
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void b03_addLinksPromotesSingleToArray() {
			var r = new HalResource()
				.addLink("alt", new HalLink("/a"))
				.addLinks("alt", new HalLink("/b"));
			var raw = r.getLinks().get("alt");
			assertInstanceOf(HalLinkArray.class, raw);
			assertEquals(2, ((HalLinkArray) raw).size());
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void b04_addLinksExtendsExistingArray() {
			var r = new HalResource()
				.addLinks("alt", new HalLink("/a"), new HalLink("/b"))
				.addLinks("alt", new HalLink("/c"));
			var raw = r.getLinks().get("alt");
			assertInstanceOf(HalLinkArray.class, raw);
			assertEquals(3, ((HalLinkArray) raw).size());
		}

		@Test
		void b05_setLinksReplacesContent() {
			var r = new HalResource()
				.addLink("self", new HalLink("/x"))
				.setLinks(new LinkedHashMap<>(Map.of("self", new HalLink("/y"))));
			assertEquals("/y", ((HalLink) r.getLinks().get("self")).getHref());
		}
	}

	@Nested
	class C_HalResource_embedded {

		@Test
		void c01_singleEmbeddedRoundTripsAsObject() {
			var inner = new HalResource()
				.addLink("self", new HalLink("/items/1"))
				.set("name", "widget");
			var r = new HalResource()
				.addLink("self", new HalLink("/orders/123"))
				.addEmbedded("item", inner);
			var j = HAL_JSON.write(r);
			assertTrue(j.contains("\"item\":{"), () -> "Expected single-embedded object form: " + j);
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void c02_multiEmbeddedRoundTripsAsArray() {
			var r = new HalResource()
				.addLink("self", new HalLink("/orders/123"))
				.addEmbedded("items",
					new HalResource().addLink("self", new HalLink("/items/1")).set("name", "a"),
					new HalResource().addLink("self", new HalLink("/items/2")).set("name", "b")
				);
			var j = HAL_JSON.write(r);
			assertTrue(j.contains("\"items\":["), () -> "Expected multi-embedded array form: " + j);
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void c03_addEmbeddedPromotesSingleToArray() {
			var r = new HalResource()
				.addEmbedded("item", new HalResource().set("k", "v1"))
				.addEmbedded("item", new HalResource[] { new HalResource().set("k", "v2") });
			var raw = r.getEmbedded().get("item");
			assertInstanceOf(HalResourceArray.class, raw);
			assertEquals(2, ((HalResourceArray) raw).size());
		}

		@Test
		void c04_addEmbeddedExtendsExistingArray() {
			var r = new HalResource()
				.addEmbedded("items", new HalResource[] { new HalResource().set("k", "a") })
				.addEmbedded("items", new HalResource[] { new HalResource().set("k", "b") });
			var raw = r.getEmbedded().get("items");
			assertInstanceOf(HalResourceArray.class, raw);
			assertEquals(2, ((HalResourceArray) raw).size());
		}

		@Test
		void c05_addEmbeddedSingleTwiceUnderDifferentRelationsKeepsBothInExistingMap() {
			var r = new HalResource()
				.addEmbedded("a", new HalResource().set("k", "va"))
				.addEmbedded("b", new HalResource().set("k", "vb"));
			// Deep-path content check (not just count): both relations must retain their own resource/value.
			assertEquals(2, r.getEmbedded().size());
			assertEquals("va", ((HalResource) r.getEmbedded().get("a")).get("k"));
			assertEquals("vb", ((HalResource) r.getEmbedded().get("b")).get("k"));
		}

		@Test
		void c06_setEmbeddedReplacesContent() {
			var r = new HalResource()
				.addEmbedded("a", new HalResource().set("k", "v"))
				.setEmbedded(new LinkedHashMap<>(Map.of("b", new HalResource().set("k", "w"))));
			assertNull(r.getEmbedded().get("a"));
			assertNotNull(r.getEmbedded().get("b"));
		}
	}

	@Nested
	class D_HalResource_extras {

		@Test
		void d01_payloadIsFlatNotNestedUnderProperties() {
			var r = new HalResource()
				.addLink("self", new HalLink("/orders/123"))
				.set("total", 99.50)
				.set("currency", "USD");
			var j = HAL_JSON.write(r);
			assertTrue(j.contains("\"total\":99.5"), () -> j);
			assertTrue(j.contains("\"currency\":\"USD\""), () -> j);
			assertJsonRoundTrip(r, HalResource.class);
		}

		@Test
		void d02_getOnUnknownReturnsNull() {
			var r = new HalResource();
			assertNull(r.get("nope"));
			assertTrue(r.extraKeys().isEmpty());
		}

		@Test
		void d03_getAfterSetReturnsValue() {
			var r = new HalResource().set("k", "v");
			assertEquals("v", r.get("k"));
			assertTrue(r.extraKeys().contains("k"));
		}
	}

	@Nested
	class E_TopLevelArrayBeans {

		@Test
		void e01_halLinkArrayConstructionAndAddAll() {
			var arr = new HalLinkArray(new HalLink("/a"));
			arr.addAll(new HalLink("/b"), new HalLink("/c"));
			// Deep-path content check (not just count): verify element order/identity survived the addAll.
			assertEquals(3, arr.size());
			assertEquals("/a", arr.get(0).getHref());
			assertEquals("/b", arr.get(1).getHref());
			assertEquals("/c", arr.get(2).getHref());
		}

		@Test
		void e02_halResourceArrayConstructionAndAddAll() {
			var arr = new HalResourceArray(new HalResource());
			arr.addAll(new HalResource(), new HalResource());
			// Count-is-property here: every element is an indistinguishable default-constructed HalResource
			// (no property is set on any of them), so there is no meaningful deep-path content to assert beyond size.
			assertEquals(3, arr.size());
		}

		@Test
		void e03_halLinkArraySerializesAsJsonArray() {
			var arr = new HalLinkArray(new HalLink("/x"), new HalLink("/y"));
			var j = HAL_JSON.write(arr);
			assertTrue(j.startsWith("["), j);
			assertJsonRoundTrip(arr, HalLinkArray.class);
		}
	}

	@Nested
	class F_Swaps_directInvocation {

		@Test
		void f01_linkOrArraySwapNullIsNull() {
			var s = new HalLinkOrArraySwap();
			assertNull(s.unswap(null, null, null));
		}

		@Test
		void f02_linkOrArraySwapSwapIsIdentity() {
			var s = new HalLinkOrArraySwap();
			var input = new Object();
			assertSame(input, s.swap(null, input));
		}

		@Test
		void f03_linkOrArraySwapNonMapPassesThrough() {
			var s = new HalLinkOrArraySwap();
			var input = "not-a-map";
			assertSame(input, s.unswap(null, input, null));
		}

		@Test
		void f04_linkOrArraySwapMapWithScalarPassesValueThrough() {
			// Round-trip via parser to exercise the "else" branch (scalar entries inside a links map).
			var json = "{\"_links\":{\"x\":\"a-string-not-a-link\"}}";
			var r = JsonParser.DEFAULT.read(json, HalResource.class);
			assertEquals("a-string-not-a-link", r.getLinks().get("x"));
		}

		@Test
		void f05_resourceOrArraySwapNullIsNull() {
			var s = new HalResourceOrArraySwap();
			assertNull(s.unswap(null, null, null));
		}

		@Test
		void f06_resourceOrArraySwapSwapIsIdentity() {
			var s = new HalResourceOrArraySwap();
			var input = new Object();
			assertSame(input, s.swap(null, input));
		}

		@Test
		void f07_resourceOrArraySwapNonMapPassesThrough() {
			var s = new HalResourceOrArraySwap();
			var input = "not-a-map";
			assertSame(input, s.unswap(null, input, null));
		}

		@Test
		void f08_resourceOrArraySwapMapWithScalarPassesValueThrough() {
			var json = "{\"_embedded\":{\"x\":\"a-string-not-a-resource\"}}";
			var r = JsonParser.DEFAULT.read(json, HalResource.class);
			assertEquals("a-string-not-a-resource", r.getEmbedded().get("x"));
		}
	}

	@Nested
	class G_Resource_toString {

		@Test
		void g01_toStringDoesNotThrow() {
			assertNotNull(new HalResource().addLink("self", new HalLink("/a")).toString());
		}

		@Test
		void g02_halLinkToStringDoesNotThrow() {
			assertNotNull(new HalLink("/a").toString());
		}
	}

	@Nested
	class H_Resource_CompoundExample {

		@Test
		void h01_compoundDocumentRoundTrip() {
			var order = new HalResource()
				.addLink("self", new HalLink("/orders/123"))
				.addLink("warehouse", new HalLink("/warehouse/56"))
				.addLink("invoice", new HalLink("/invoices/873"))
				.addLinks("curies", new HalLink("https://acme.example/{rel}").setName("acme").setTemplated(true))
				.set("currency", "USD")
				.set("status", "shipped")
				.set("total", 30.00)
				.addEmbedded("customer",
					new HalResource()
						.addLink("self", new HalLink("/customers/7"))
						.set("name", "Kelsey")
				);
			assertJsonRoundTrip(order, HalResource.class);
		}
	}
}
