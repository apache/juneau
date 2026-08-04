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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpSchemaSafety}: no-fetch handling of external {@code $ref}s and bounded
 * (depth / node / wall-clock) schema validation (Resolution B2).
 */
class McpSchemaSafety_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpToolHandler tool(String name, McpSchema schema) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name).setInputSchema(schema); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text("ok"); }
		};
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		var p = params instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(p);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		m.put("Mcp-Method", method);
		m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r, Map<String,String> headers) {
		return new McpRevision(null).dispatch(new McpExchange(r, headers::get), config, ctx);
	}

	private static Map<String,Object> nest(int depth) {
		Map<String,Object> node = new LinkedHashMap<>();
		for (var i = 1; i < depth; i++) {
			var parent = new LinkedHashMap<String,Object>();
			parent.put("child", node);
			node = parent;
		}
		return node;
	}

	private static Map<String,Object> flat(int nodeCount) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < nodeCount - 1; i++)  // root map (1) + (nodeCount-1) scalar entries == nodeCount nodes
			m.put("k" + i, i);
		return m;
	}

	// -------- no JsonSchemaMap / no external fetch ---------

	@Test
	void a01_noAdapterClassExtendsJsonSchemaMap() {
		for (var c : List.of(McpSchemaSafety.class, McpWire.class, McpRevision.class, McpRestServlet.class, McpEndpoint.class))
			assertFalse(JsonSchemaMap.class.isAssignableFrom(c), () -> c.getName() + " must not extend JsonSchemaMap");
	}

	@Test
	void a02_externalRef_serializesListsAndDispatchesWithoutFetch() {
		// An unresolvable host: if any code path tried to dereference this $ref, DNS resolution would throw.
		var schema = McpSchema.of(JsonMap.of("$ref", "https://never.invalid/schema.json"));
		var config = new McpServerConfig().addTool(tool("refTool", schema));

		// serialize: the $ref survives verbatim as opaque data.
		var wireTool = McpWire.toWire(new McpToolSpec().setName("refTool").setInputSchema(schema));
		assertContains("https://never.invalid/schema.json", Json.of(wireTool));

		// list: dispatch tools/list and confirm the $ref round-trips.
		var listed = (ListToolsResult) send(config, req(1, "tools/list", null), hdrs("tools/list", "")).getResult();
		assertContains("https://never.invalid/schema.json", Json.of(listed));

		// dispatch: tools/call runs input validation against the $ref schema and completes without a fetch.
		var params = JsonMap.of("name", "refTool", "arguments", JsonMap.of("anything", 1));
		var resp = send(config, req(2, "tools/call", params), hdrs("tools/call", "refTool"));
		assertNull(resp.getError());
		assertInstanceOf(CallToolResult.class, resp.getResult());
	}

	// -------- bounded depth ---------

	@Test
	void b01_depth64_passes() {
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), nest(McpSchemaSafety.MAX_DEPTH)));
	}

	@Test
	void b02_depth65_fails() {
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), nest(McpSchemaSafety.MAX_DEPTH + 1)));
		assertEquals(-32602, e.getCode());
		assertContains("nesting depth", e.getMessage());
	}

	// -------- bounded node count ---------

	@Test
	void c01_nodes10000_passes() {
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), flat(McpSchemaSafety.MAX_NODES)));
	}

	@Test
	void c02_nodes10001_fails() {
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), flat(McpSchemaSafety.MAX_NODES + 1)));
		assertEquals(-32602, e.getCode());
		assertContains("node count", e.getMessage());
	}

	// -------- bounded wall-clock ---------

	@Test
	void d01_adversarialValidation_terminatesWithinDeadline() {
		// A catastrophically-backtracking pattern applied to a non-matching input hangs the validator.
		var schema = McpSchema.of(JsonMap.of(
			"type", "object",
			"properties", JsonMap.of("s", JsonMap.of("type", "string", "pattern", "^(.*a){25}$"))));
		var args = new LinkedHashMap<String,Object>();
		args.put("s", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!");  // 40 'a's then a non-matching char

		var start = System.nanoTime();
		var e = assertThrows(McpException.class, () -> McpSchemaSafety.validateInput(schema, args));
		var elapsedMs = (System.nanoTime() - start) / 1_000_000;

		assertEquals(-32602, e.getCode());
		assertContains("exceeded " + McpSchemaSafety.MAX_VALIDATION_MILLIS + " ms", e.getMessage());
		assertTrue(elapsedMs < McpSchemaSafety.MAX_VALIDATION_MILLIS + 5000, () -> "validation did not terminate promptly: elapsed=" + elapsedMs + "ms");
	}

	@Test
	void d02_schedulingLatency_notCountedAgainstComputeBudget() throws Exception {
		// Exercises McpSchemaSafety.awaitBounded() directly (rather than saturating the shared
		// VALIDATION_POOL, which other tests in this class can leave with a permanently-stuck thread since a
		// catastrophically-backtracking regex match is not interruptible) with a task-local executor that
		// deliberately delays counting down `started` well past MAX_VALIDATION_MILLIS before doing its
		// (instantaneous) "work". If scheduling latency were - the regression this guards against - counted
		// against the compute budget, awaitBounded() would throw a timeout error despite the task's own
		// work costing nothing; with the root-cause fix, only compute time (measured from `started`) counts.
		var schedulingDelayMillis = 3 * McpSchemaSafety.MAX_VALIDATION_MILLIS;
		assertTrue(schedulingDelayMillis < McpSchemaSafety.MAX_SCHEDULING_MILLIS, "test fixture assumption");

		var started = new CountDownLatch(1);
		var startedAtNanos = new AtomicLong();
		var executor = Executors.newSingleThreadExecutor();
		try {
			var future = executor.submit(() -> {
				Thread.sleep(schedulingDelayMillis);  // simulates thread-pool queueing/scheduling delay
				startedAtNanos.set(System.nanoTime());
				started.countDown();
				return null;  // the "validation" work itself is instantaneous
			});

			assertDoesNotThrow(() -> McpSchemaSafety.awaitBounded(
				future, started, startedAtNanos, TimeUnit.MILLISECONDS.toNanos(McpSchemaSafety.MAX_VALIDATION_MILLIS)));
		} finally {
			executor.shutdownNow();
		}
	}

	// -------- shared JsonValueSafety delegation now supports arrays ---------

	@Test
	void f01_argumentContainingList_passesUnderPermissiveSchema() {
		var args = JsonMap.of("arr", JsonList.of(1, 2, 3));
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), args));
	}

	@Test
	void f02_argumentContainingPrimitiveArray_isWalkedWithoutError() {
		// The old local checkBounds() only recursed into Map/Collection, never arrays; delegating to
		// JsonValueSafety (which also walks java.lang.reflect.Array elements) must not regress this.
		var args = new LinkedHashMap<String,Object>();
		args.put("arr", new int[]{1, 2, 3});
		assertDoesNotThrow(() -> McpSchemaSafety.validateInput(McpSchema.of(new JsonMap()), args));
	}

	// -------- v1 finiteness anchor is untouched ---------

	@Test
	void e01_v1FinitenessTest_hasZeroDiff() throws Exception {
		var relPath = "juneau-bean/juneau-bean-mcp-v20250618/src/test/java/org/apache/juneau/bean/mcp/v20250618/JsonSchema_Finiteness_Test.java";
		var root = repoRoot();
		Assumptions.assumeTrue(root != null, "git repository root not resolvable");
		var diff = new ProcessBuilder("git", "diff", "--exit-code", "--", relPath);
		diff.directory(root);
		diff.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		diff.redirectError(ProcessBuilder.Redirect.DISCARD);
		assertEquals(0, diff.start().waitFor(), "v1 JsonSchema_Finiteness_Test.java must have zero diff");
	}

	private static File repoRoot() {
		try {
			var pb = new ProcessBuilder("git", "rev-parse", "--show-toplevel");
			pb.directory(new File(System.getProperty("user.dir")));
			var p = pb.start();
			String out;
			try (var r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				out = r.readLine();
			}
			return p.waitFor() == 0 && out != null ? new File(out.trim()) : null;
		} catch (IOException | InterruptedException e) {
			return null;
		}
	}
}
