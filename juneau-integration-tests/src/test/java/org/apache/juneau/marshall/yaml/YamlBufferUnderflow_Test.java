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
package org.apache.juneau.marshall.yaml;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.openapi3.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Focused, parser-only regression coverage for the YAML buffer-underflow fix: the YAML
 * parser used to throw {@code IOException: Buffer underflow} when round-tripping documents
 * that crossed the underlying {@code ParserReader}'s 1024-char buffer boundary while a single
 * block-mapping line had more than ~10 chars of leading indent.
 *
 * <p>Root cause: {@code ParserReader.readFromBuff()} retained only 10 chars of unread
 * lookback when refilling the buffer without an active mark, but {@code YamlParserSession}
 * routinely reads N indent spaces (where N can be 12+ for deeply nested OpenAPI 3.1 docs)
 * and then unreads all of them via {@code unreadSpaces(r, N)}. Once the read crossed a
 * buffer boundary, only 10 of those unreads were honored — the rest threw underflow. The
 * fix widens the unmarked-refill lookback window (see
 * {@code ParserReader.UNMARKED_LOOKBACK_CHARS}).
 *
 * <p>Symptom (pre-fix): the live OpenAPI 3.1 doc emitted by {@code BasicRestServlet} with
 * the full api-docs mixin pack (six api-docs URLs + components.schemas) reliably tripped
 * the underflow on YAML round-trip; this test reproduces the underflow at the parser layer
 * with no REST stack, so a future regression of the buffer-management bug surfaces here
 * before it reaches the live-doc test.
 */
class YamlBufferUnderflow_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Synthesized minimal repro: many top-level keys whose values are deeply-nested maps,
	// emitted with 2-space indents. Total document size deliberately exceeds the 1024-char
	// ParserReader buffer; nested indent depth exceeds the 10-char unread lookback.
	//------------------------------------------------------------------------------------------------------------------

	private static Map<String,Object> nestedMap(int depth) {
		Map<String,Object> head = new LinkedHashMap<>();
		Map<String,Object> cur = head;
		for (int i = 0; i < depth; i++) {
			Map<String,Object> next = new LinkedHashMap<>();
			next.put("leaf", "value-" + i);
			cur.put("level" + i, next);
			cur = next;
		}
		return head;
	}

	@Test void a01_largeNestedMap_jsonYamlJsonRoundTrip() throws Exception {
		// Build a document that's > 1024 chars when serialized to YAML and has nested
		// indent depth > 10 spaces (to trip the unread-lookback underflow).
		var top = new LinkedHashMap<String,Object>();
		for (int i = 0; i < 30; i++)
			top.put("entry" + i, nestedMap(8));

		String yaml = YamlSerializer.DEFAULT_READABLE.toString(top);
		assertTrue(yaml.length() > 1024, () -> "YAML too small for test (got " + yaml.length() + " chars)");
		assertTrue(yaml.contains("                "), () -> "YAML must contain >10-space indent line for repro");

		Map<?,?> parsed = YamlParser.DEFAULT.read(yaml, Map.class);

		assertEquals(top.size(), parsed.size(), "Top-level entry count must match");
		assertEquals(top.keySet(), parsed.keySet(), "Top-level keys must match");
	}

	@Test void a02_jsonToYamlToJson_structuralEquality() throws Exception {
		// Same shape but driven through JSON to YAML to JSON to verify the round-trip
		// path the OpenApiYamlRoundTrip_Test#c01 workaround was avoiding.
		var top = new LinkedHashMap<String,Object>();
		for (int i = 0; i < 20; i++)
			top.put("k" + i, nestedMap(7));

		String json = JsonSerializer.DEFAULT.toString(top);
		Map<?,?> fromJson = JsonParser.DEFAULT.read(json, Map.class);
		String yaml = YamlSerializer.DEFAULT_READABLE.toString(fromJson);
		assertTrue(yaml.length() > 1024);

		// This is the line that used to throw IOException: Buffer underflow.
		Map<?,?> fromYaml = YamlParser.DEFAULT.read(yaml, Map.class);
		String json2 = JsonSerializer.DEFAULT.toString(fromYaml);

		// Sanity: the second JSON has all the top-level keys.
		Map<?,?> reparsed = JsonParser.DEFAULT.read(json2, Map.class);
		assertEquals(top.keySet(), reparsed.keySet());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pathological cases that exercise the same code paths but with fewer moving parts.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_deepIndentAcrossBufferBoundary() throws Exception {
		// Construct a YAML doc whose first 1000+ chars are a single long string value,
		// followed by a nested block-mapping that requires >10-char indent unreads at
		// the boundary.
		var top = new LinkedHashMap<String,Object>();
		var bigStr = new StringBuilder();
		for (int i = 0; i < 500; i++)
			bigStr.append('x');
		top.put("filler", bigStr.toString());
		top.put("nested", nestedMap(10));

		String yaml = YamlSerializer.DEFAULT_READABLE.toString(top);
		Map<?,?> parsed = YamlParser.DEFAULT.read(yaml, Map.class);

		assertEquals(top.keySet(), parsed.keySet());
		assertEquals(bigStr.toString(), parsed.get("filler"));
	}

	@Test void b02_manyTopLevelKeysWithDeepIndent() throws Exception {
		// Many shallow keys whose values are a deep nested map, so that the sequence of
		// {top-level-key newline + indent + key}-pairs accumulates past the buffer
		// boundary and the indent length flips between 0 (top-level) and >10 (nested).
		var top = new LinkedHashMap<String,Object>();
		for (int i = 0; i < 40; i++)
			top.put("key_" + i, nestedMap(6));

		String yaml = YamlSerializer.DEFAULT_READABLE.toString(top);
		Map<?,?> parsed = YamlParser.DEFAULT.read(yaml, Map.class);

		assertEquals(40, parsed.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Live-resource repro: the FULL BasicRestServlet OpenAPI doc — six api-docs URLs and the
	// full components.schemas set — is the document shape that originally tripped the
	// underflow. We re-use the same RestContext-driven path that OpenApiYamlRoundTrip_Test
	// uses.
	//------------------------------------------------------------------------------------------------------------------

	public static class Pet {
		public int id;
		public String name;
	}

	@Rest
	public static class FullSurface extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/pet") public Pet getPet() { return new Pet(); }
	}

	public void testMethod() { /* no-op */ }

	private OpenApi getOpenApi(Object resource) throws Exception {
		var rc = new RestContext(new RestContext.Args(resource.getClass(), null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT));
		var roc = new RestOpContext(YamlBufferUnderflow_Test.class.getMethod("testMethod"), rc);
		var call = RestSession.create(rc).resource(resource).req(new MockServletRequest()).res(new MockServletResponse()).build();
		var req = roc.createRequest(call);
		return rc.getOpenApiProvider().getOpenApi(rc, req.getLocale());
	}

	@Test void c01_basicRestServletFullMixinSurface_yamlRoundTrip() throws Exception {
		var doc = getOpenApi(new FullSurface());
		String yaml = YamlSerializer.DEFAULT_READABLE.toString(doc);
		// The full mixin surface produces a multi-KB YAML doc that crosses the
		// ParserReader buffer boundary; deep schema nesting puts indent depth well past the
		// reader's no-mark unread-lookback limit. Pre-fix this throws IOException: Buffer
		// underflow.
		var parsed = YamlParser.DEFAULT.read(yaml, OpenApi.class);
		assertEquals(doc.getOpenapi(), parsed.getOpenapi());
		assertNotNull(parsed.getPaths());
	}
}
