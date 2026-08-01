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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@code completion/complete} dispatch on the {@code 2026-07-28} adapter: strict params
 * validation, neutral ref/lookup reuse, per-target completer invocation, empty-miss behavior, internal-error
 * mapping, result cap/order/total/hasMore mapping, and this revision's nameless SEP-2243 header/{@code _meta}
 * agreement.
 */
@SuppressWarnings({
	"java:S5976" // Each aNN/bNN/... test pins a distinct named dispatch scenario as its own discoverable, individually-runnable test (per project SSLLC convention); collapsing similar-shaped groups (e.g. the b0x invalid-params cases) into @ParameterizedTest would trade per-scenario failure clarity for a marginal LOC reduction.
})
class McpCompletion_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpPromptHandler promptWithCompleter(String name, String argumentName, McpCompleter completer) {
		var descriptor = new McpPromptSpec().setName(name)
			.setArguments(List.of(new McpPromptArgument().setName(argumentName).setCompleter(completer)));
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return descriptor; }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore beanStore) { return new McpPromptOutcome(); }
		};
	}

	private static McpResourceTemplateHandler templateWithCompleter(String uriTemplate, String variableName, McpCompleter completer) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate(uriTemplate).setName("t:" + uriTemplate);
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore beanStore) { return null; }
			@Override public McpCompleter completer(String v) { return variableName.equals(v) ? completer : null; }
		};
	}

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	/**
	 * Builds a {@code completion/complete} request, nesting {@code meta} under {@code params._meta} when both
	 * {@code params} is an object and {@code meta} is non-<jk>null</jk>. A non-object {@code params} (used by the
	 * strict-params-validation scenarios below) is passed through as-is: it can carry no nested {@code _meta} and
	 * is expected to fail generic {@code params} shape validation before reaching completion-specific logic.
	 */
	private static JsonRpcRequest req(Object id, Object params, Object meta) {
		Object merged = params;
		if (meta != null && params instanceof Map<?,?> m) {
			var p = new JsonMap(m);
			p.put("_meta", meta);
			merged = p;
		}
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(McpMethods.COMPLETION_COMPLETE).setParams(merged);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		if (method != null)
			m.put("Mcp-Method", method);
		if (name != null)
			m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpServerConfig config, Object params) {
		return new McpRevision(null).dispatch(new McpExchange(req(1, params, validMeta()), hdrs(McpMethods.COMPLETION_COMPLETE, "")::get), config, ctx);
	}

	private JsonRpcResponse sendNotification(McpServerConfig config, Object params) {
		return new McpRevision(null).dispatch(new McpExchange(req(null, params, validMeta()), hdrs(McpMethods.COMPLETION_COMPLETE, "")::get), config, ctx);
	}

	private static JsonMap promptRef(String name) {
		return JsonMap.of("type", "ref/prompt", "name", name);
	}

	private static JsonMap resourceRef(String uriTemplate) {
		return JsonMap.of("type", "ref/resource", "uri", uriTemplate);
	}

	private static JsonMap argument(String name, Object value) {
		return JsonMap.of("name", name, "value", value);
	}

	private Completion complete(McpServerConfig config, Object params) {
		var resp = send(config, params);
		assertNull(resp.getError(), () -> "unexpected error: " + (resp.getError() == null ? null : resp.getError().getMessage()));
		return ((CompleteResult) resp.getResult()).getCompletion();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A: prompt/resource references route to the correct target through the neutral lookup
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_promptCompletion_routesToDeclaredArgumentCompleter() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(request, beanStore) -> new McpCompletionResult().setValues(List.of("formal", "casual"))));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "f")));
		assertEquals(List.of("formal", "casual"), completion.getValues());
	}

	@Test void a02_resourceCompletion_routesToDeclaredVariableCompleter() {
		var config = new McpServerConfig().addResourceTemplate(templateWithCompleter("file:///{name}", "name",
			(request, beanStore) -> new McpCompletionResult().setValues(List.of("alice", "albert"))));
		var completion = complete(config, JsonMap.of("ref", resourceRef("file:///{name}"), "argument", argument("name", "al")));
		assertEquals(List.of("alice", "albert"), completion.getValues());
	}

	@Test void a03_completerReceivesValueContextRefKindAndBeanStore() {
		var captured = new AtomicReference<McpCompletionRequest>();
		var marker = new BasicBeanStore();
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (request, beanStore) -> {
			captured.set(request);
			assertSame(marker, beanStore);
			return McpCompletionResult.empty();
		}));
		var params = JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "partial"),
			"context", JsonMap.of("arguments", JsonMap.of("name", "Alice")));
		var resp = new McpRevision(null).dispatch(
			new McpExchange(req(1, params, validMeta()), hdrs(McpMethods.COMPLETION_COMPLETE, "")::get), config, marker);
		assertNull(resp.getError());
		var request = captured.get();
		assertEquals(McpCompletionRef.Kind.PROMPT, request.getRef().getKind());
		assertEquals("greet", request.getRef().getTarget());
		assertEquals("style", request.getArgumentName());
		assertEquals("partial", request.getValue());
		assertEquals(Map.of("name", "Alice"), request.getContextArguments());
	}

	@Test void a04_omittedContext_normalizesToEmptyMap() {
		var captured = new AtomicReference<McpCompletionRequest>();
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (request, beanStore) -> {
			captured.set(request);
			return McpCompletionResult.empty();
		}));
		complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "x")));
		assertTrue(captured.get().getContextArguments().isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: strict params validation -> -32602
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_paramsNotObject_invalidRequest() {
		// A non-object params can carry no nested `_meta`, so the generic params-shape check now rejects it
		// (-32600) before completion-specific params validation (-32602) is ever reached.
		var resp = send(new McpServerConfig(), "not-a-map");
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
	}

	@Test void b02_refNotObject_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", "not-a-map", "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b03_refMissingType_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", JsonMap.of(), "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b04_refUnrecognizedType_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", JsonMap.of("type", "ref/bogus"), "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b05_refPromptMissingName_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", JsonMap.of("type", "ref/prompt"), "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b06_refPromptEmptyName_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef(""), "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b07_refResourceMissingUri_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", JsonMap.of("type", "ref/resource"), "argument", argument("x", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b08_argumentNotObject_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", "not-a-map"));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b09_argumentMissingName_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", JsonMap.of("value", "v")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b10_argumentMissingValue_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", JsonMap.of("name", "style")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b11_argumentNumericValue_rejectedWithoutToStringCoercion() {
		var invoked = new AtomicInteger();
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (request, beanStore) -> {
			invoked.incrementAndGet();
			return McpCompletionResult.empty();
		}));
		var resp = send(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", 7)));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
		assertEquals(0, invoked.get());
	}

	@Test void b12_argumentBooleanValue_rejectedWithoutToStringCoercion() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", argument("style", true)));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b13_contextNotObject_invalidParams() {
		var resp = send(new McpServerConfig(),
			JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"), "context", "not-a-map"));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b14_contextArgumentsNotObject_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"),
			"context", JsonMap.of("arguments", "not-a-map")));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test void b15_contextArgumentsValueNotString_invalidParams() {
		var resp = send(new McpServerConfig(), JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"),
			"context", JsonMap.of("arguments", JsonMap.of("k", 7))));
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: exact template string required; unknown/uncompletable targets return successful empty completion
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_concreteExpandedUri_returnsEmptyCompletionWithoutInvokingCompleter() {
		var invoked = new AtomicInteger();
		var config = new McpServerConfig().addResourceTemplate(templateWithCompleter("file:///{name}", "name",
			(request, beanStore) -> { invoked.incrementAndGet(); return new McpCompletionResult().setValues(List.of("x")); }));
		var completion = complete(config, JsonMap.of("ref", resourceRef("file:///alice"), "argument", argument("name", "a")));
		assertTrue(completion.getValues().isEmpty());
		assertEquals(0, invoked.get());
	}

	@Test void c02_unknownPrompt_returnsEmptyCompletion() {
		var completion = complete(new McpServerConfig(), JsonMap.of("ref", promptRef("ghost"), "argument", argument("x", "v")));
		assertEquals(List.of(), completion.getValues());
	}

	@Test void c03_unknownArgument_returnsEmptyCompletion() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> new McpCompletionResult().setValues(List.of("x"))));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("other", "v")));
		assertEquals(List.of(), completion.getValues());
	}

	@Test void c04_argumentWithoutCompleter_returnsEmptyCompletion() {
		var config = new McpServerConfig().addPrompt(new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() {
				return new McpPromptSpec().setName("greet").setArguments(List.of(new McpPromptArgument().setName("style")));
			}
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore beanStore) { return new McpPromptOutcome(); }
		});
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(List.of(), completion.getValues());
	}

	@Test void c05_unknownTemplate_returnsEmptyCompletion() {
		var completion = complete(new McpServerConfig(), JsonMap.of("ref", resourceRef("file:///{x}"), "argument", argument("x", "v")));
		assertEquals(List.of(), completion.getValues());
	}

	@Test void c06_undeclaredVariable_returnsEmptyCompletion() {
		var config = new McpServerConfig().addResourceTemplate(templateWithCompleter("file:///{name}", "name",
			(r, c) -> new McpCompletionResult().setValues(List.of("x"))));
		var completion = complete(config, JsonMap.of("ref", resourceRef("file:///{name}"), "argument", argument("other", "v")));
		assertEquals(List.of(), completion.getValues());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: completer failure -> internal error
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_completerThrows_internalError() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> {
			throw new RuntimeException("boom");
		}));
		var resp = send(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertEquals("boom", resp.getError().getMessage());
	}

	@Test void d02_completerReturnsNull_internalError() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> null));
		var resp = send(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
	}

	@Test void d03_completerResultWithNullValueElement_internalError() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(r, c) -> new McpCompletionResult().setValues(Arrays.asList("a", null))));
		var resp = send(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
	}

	@Test void d04_completerResultWithNegativeTotal_internalError() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(r, c) -> new McpCompletionResult().setValues(List.of()).setTotal(-1)));
		var resp = send(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: cap/order/duplicates/total/hasMore map exactly
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_orderAndDuplicatesPreserved() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(r, c) -> new McpCompletionResult().setValues(List.of("b", "a", "b"))));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(List.of("b", "a", "b"), completion.getValues());
	}

	@Test void e02_totalAndHasMorePreservedWithoutTruncation() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(r, c) -> new McpCompletionResult().setValues(List.of("a")).setTotal(5).setHasMore(false)));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(5, completion.getTotal());
		assertEquals(Boolean.FALSE, completion.getHasMore());
	}

	@Test void e03_over100Values_truncatesAndForcesHasMore() {
		var values = new ArrayList<String>();
		for (var i = 0; i < 150; i++)
			values.add("v" + i);
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style",
			(r, c) -> new McpCompletionResult().setValues(values).setHasMore(false)));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(100, completion.getValues().size());
		assertEquals("v0", completion.getValues().get(0));
		assertEquals("v99", completion.getValues().get(99));
		assertEquals(Boolean.TRUE, completion.getHasMore());
	}

	@Test void e04_emptyCompletion_serializesToExactShape() {
		var completion = complete(new McpServerConfig(), JsonMap.of("ref", promptRef("ghost"), "argument", argument("x", "v")));
		assertEquals(List.of(), completion.getValues());
		assertNull(completion.getTotal());
		assertNull(completion.getHasMore());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: notifications emit no response
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_notification_returnsNullResponse() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> new McpCompletionResult().setValues(List.of("x"))));
		assertNull(sendNotification(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"))));
	}

	@Test void f02_notification_invalidParams_stillReturnsNull() {
		assertNull(sendNotification(new McpServerConfig(), "not-a-map"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: 2026-07-28-specific - nameless SEP-2243 header agreement, normal per-request _meta validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_missingMeta_invalidRequest_beforeAnyCompleterInvocation() {
		var invoked = new AtomicInteger();
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> { invoked.incrementAndGet(); return McpCompletionResult.empty(); }));
		var params = JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"));
		var resp = new McpRevision(null).dispatch(
			new McpExchange(req(1, params, null), hdrs(McpMethods.COMPLETION_COMPLETE, "")::get), config, ctx);
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
		assertEquals(0, invoked.get());
	}

	@Test void g02_nonEmptyMcpName_headerMismatch_invalidRequest() {
		var params = JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"));
		var resp = new McpRevision(null).dispatch(
			new McpExchange(req(1, params, validMeta()), hdrs(McpMethods.COMPLETION_COMPLETE, "greet")::get), new McpServerConfig(), ctx);
		assertEquals(McpRevision.CODE_INVALID_REQUEST, resp.getError().getCode());
		assertEquals("Mcp-Name header 'greet' does not match request name ''", resp.getError().getMessage());
	}

	@Test void g03_emptyMcpName_dispatchesNormally() {
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> new McpCompletionResult().setValues(List.of("x"))));
		var completion = complete(config, JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v")));
		assertEquals(List.of("x"), completion.getValues());
	}

	@Test void g04_refTargetNeverBecomesRoutingName_evenWhenItMatchesMcpMethodConvention() {
		// params.ref.name looks like it could be mistaken for a routing name (as tools/call's params.name is);
		// this proves it never is one for completion/complete, whose Mcp-Name is always the empty string.
		var config = new McpServerConfig().addPrompt(promptWithCompleter("greet", "style", (r, c) -> new McpCompletionResult().setValues(List.of("x"))));
		var params = JsonMap.of("ref", promptRef("greet"), "argument", argument("style", "v"));
		var resp = new McpRevision(null).dispatch(
			new McpExchange(req(1, params, validMeta()), hdrs(McpMethods.COMPLETION_COMPLETE, "")::get), config, ctx);
		assertNull(resp.getError());
	}
}
