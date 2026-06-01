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
package org.apache.juneau.rest.ops;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link RouteIndexMixin} mounted as a mixin via {@code @Rest(mixins=...)} on a
 * vanilla {@link RestServlet}.
 *
 * <p>
 * Cases:
 * <ul>
 * 	<li>{@code GET /options} returns the JSON list, ordered by path (single SVL-configurable
 * 		default mount).
 * 	<li>Every host {@code @RestOp}-annotated method appears in the listing with method, path,
 * 		and {@code summary} fields populated.
 * 	<li>The route-index endpoint omits itself from the listing.
 * 	<li>Operations marked {@link OpSwagger#ignore() @OpSwagger(ignore=true)} are excluded
 * 		(consistent with their absence from the OpenAPI spec).
 * 	<li>{@link Deprecated @Deprecated} methods surface {@code "deprecated": true}.
 * </ul>
 *
 * <p>
 * Per the FINISHED-101 multi-path collapse, the historical {@code /routes} mount alias
 * (formerly a dual default on a single op) is now reached via
 * {@code -Djuneau.routeindex.path=routes}; that behavior is covered by
 * {@code RouteIndexMixin_SvlPathOverride_Test}.
 *
 * @since 9.5.0
 */
class RouteIndexMixin_AsMixin_Test extends TestBase {

	@Rest(mixins=RouteIndexMixin.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/items", summary="List items") public String items() { return "items"; }
		@RestGet(path="/items/{id}", summary="Get item") public String item() { return "item"; }
		@RestPost(path="/items", summary="Create item") public String create() { return "created"; }
		@RestDelete(path="/items/{id}", summary="Delete item") public String delete() { return "ok"; }

		@RestGet(path="/internal", swagger=@OpSwagger(ignore=true))
		public String internal() { return "internal"; }

		@Deprecated
		@RestGet(path="/legacy", summary="Legacy endpoint")
		public String legacy() { return "legacy"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_optionsReturnsJsonList() throws Exception {
		var body = ca.get("/options")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, List.class);
		Assertions.assertFalse(parsed.isEmpty(), "route index should not be empty");
	}

	@Test void a02_legacyRoutesAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /routes is no longer a multi-path default. It only mounts when the
		// deployer overrides juneau.routeindex.path=routes. Default-build hosts get 404 here.
		ca.get("/routes").run().assertStatus(404);
	}

	@Test void a03_listsAllVisibleHostEndpoints() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var paths = pathsOf(entries);
		Assertions.assertTrue(paths.contains("/items"), "GET /items should appear");
		Assertions.assertTrue(paths.contains("/items/{id}"), "GET /items/{id} should appear");
		Assertions.assertTrue(paths.contains("/legacy"), "Legacy endpoint should appear");
	}

	@Test void a04_excludesItself() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var paths = pathsOf(entries);
		Assertions.assertFalse(paths.contains("/options"),
			"Route index must not echo itself; got: " + paths);
	}

	@Test void a05_excludesOpSwaggerIgnoreEndpoints() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var paths = pathsOf(entries);
		Assertions.assertFalse(paths.contains("/internal"),
			"@OpSwagger(ignore=true) endpoints must be excluded; got: " + paths);
	}

	@Test void a06_summaryFieldPopulated() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var byPath = byPath(entries);
		Assertions.assertEquals("List items", byPath.get("/items GET").get("summary"));
		Assertions.assertEquals("Get item", byPath.get("/items/{id} GET").get("summary"));
	}

	@Test void a07_methodsMapsToRequestMethod() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var byPath = byPath(entries);
		Assertions.assertEquals(List.of("GET"), byPath.get("/items GET").get("methods"));
		Assertions.assertEquals(List.of("POST"), byPath.get("/items POST").get("methods"));
		Assertions.assertEquals(List.of("DELETE"), byPath.get("/items/{id} DELETE").get("methods"));
	}

	@Test void a08_deprecatedFlagPropagates() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var byPath = byPath(entries);
		var legacy = byPath.get("/legacy GET");
		Assertions.assertNotNull(legacy, "legacy entry must be present");
		Assertions.assertEquals(Boolean.TRUE, legacy.get("deprecated"));
		var items = byPath.get("/items GET");
		Assertions.assertEquals(Boolean.FALSE, items.get("deprecated"));
	}

	@Test void a09_orderedByPathAscending() throws Exception {
		var entries = parseEntries(ca.get("/options").run().assertStatus(200).getContent().asString());
		var paths = pathsOf(entries);
		var sorted = new ArrayList<>(paths);
		Collections.sort(sorted);
		Assertions.assertEquals(sorted, paths,
			"Entries should already be sorted by path ascending; got: " + paths);
	}

	// -----------------------------------------------------------------------------------------
	// Class-level @Deprecated propagates to every endpoint declared on that class (covers the
	// second branch of m.isAnnotationPresent(Deprecated.class) || m.getDeclaringClass()...).
	// Description as a multi-line String[] (covers the array path of readDescription).
	// -----------------------------------------------------------------------------------------

	@Deprecated
	@Rest(mixins=RouteIndexMixin.class)
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/c-item", summary="C item",
			description={"line one", "line two"})
		public String cItem() { return "c"; }
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_classLevelDeprecatedPropagates() throws Exception {
		var entries = parseEntries(cc.get("/options").run().assertStatus(200).getContent().asString());
		var byPath = byPath(entries);
		var entry = byPath.get("/c-item GET");
		Assertions.assertNotNull(entry, "class-level @Deprecated entry must be present");
		Assertions.assertEquals(Boolean.TRUE, entry.get("deprecated"),
			"class-level @Deprecated should make every endpoint deprecated; entry: " + entry);
	}

	@Test void c02_multilineDescriptionJoined() throws Exception {
		var entries = parseEntries(cc.get("/options").run().assertStatus(200).getContent().asString());
		var byPath = byPath(entries);
		var entry = byPath.get("/c-item GET");
		Assertions.assertEquals("line one line two", entry.get("description"),
			"String[] description should be space-joined; entry: " + entry);
	}

	// -----------------------------------------------------------------------------------------
	// Mounting on a host with no extra ops should still serve a non-empty list (the host's own
	// ops, e.g. /items) and never expose the route-index endpoint itself.
	// -----------------------------------------------------------------------------------------

	@Rest(mixins=RouteIndexMixin.class)
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/only") public String only() { return "only"; }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_minimalHostListsOneEntry() throws Exception {
		var entries = parseEntries(cb.get("/options").run().assertStatus(200).getContent().asString());
		var paths = pathsOf(entries);
		Assertions.assertEquals(List.of("/only"), paths,
			"Only the host's own /only endpoint should appear; got: " + paths);
	}

	// -----------------------------------------------------------------------------------------
	// Helpers.
	// -----------------------------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> parseEntries(String body) throws Exception {
		return (List<Map<String,Object>>) JsonParser.DEFAULT.parse(body, List.class);
	}

	private static List<String> pathsOf(List<Map<String,Object>> entries) {
		return entries.stream().map(e -> String.valueOf(e.get("path"))).toList();
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Map<String,Object>> byPath(List<Map<String,Object>> entries) {
		var out = new LinkedHashMap<String,Map<String,Object>>();
		for (var e : entries) {
			var path = String.valueOf(e.get("path"));
			var methods = (List<String>) e.get("methods");
			out.put(path + " " + methods.get(0), e);
		}
		return out;
	}
}
