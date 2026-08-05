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
package org.apache.juneau.rest.server.mcp.v20250618;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for template-backed {@code resources/read} dispatch and {@code resources/templates/list} on the
 * {@code 2025-06-18} adapter: exact-before-template precedence, the deterministic literal/variable/registration
 * specificity ranking, the {@code -32002} resource-not-found mapping, handler-exception mapping,
 * malformed-escape/non-matchable-template boundaries, and list wire mapping/capability derivation.
 */
class McpResourceTemplate_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- fixtures ---------

	private static McpResourceHandler resource(String uri, Function<String,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res"); }
			@Override public McpResourceOutcome read(String u, BeanStore ctx) { return fn.apply(u); }
		};
	}

	private static McpResourceTemplateHandler template(String uriTemplate, BiFunction<String,Map<String,String>,McpResourceOutcome> fn) {
		return new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() {
				return new McpResourceTemplateSpec().setUriTemplate(uriTemplate).setName("t:" + uriTemplate);
			}
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore ctx) {
				return fn.apply(uri, variables);
			}
		};
	}

	private static McpResourceOutcome text(String body) {
		return new McpResourceOutcome().setContents(List.of(McpResourceContents.text("u", null, body)));
	}

	private static JsonRpcRequest req(Object id, String method, Object params) {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(params);
	}

	private JsonRpcResponse send(McpServerConfig config, JsonRpcRequest r) {
		return new McpRevision(null).dispatch(new McpExchange(r, n -> null), config, ctx);
	}

	private JsonRpcResponse readResponse(McpServerConfig config, String uri) {
		return send(config, req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", uri)));
	}

	private ReadResourceResult read(McpServerConfig config, String uri) {
		var resp = readResponse(config, uri);
		assertNull(resp.getError(), () -> "unexpected error: " + (resp.getError() == null ? null : resp.getError().getMessage()));
		return (ReadResourceResult) resp.getResult();
	}

	private static String bodyOf(ReadResourceResult result) {
		return ((TextResourceContents) result.getContents().get(0)).getText();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// A: exact-before-template precedence
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_exactResourceBeatsMatchingTemplate() {
		var config = new McpServerConfig()
			.addResource(resource("file:///a", u -> text("exact")))
			.addResourceTemplate(template("file:///{x}", (u, v) -> text("template")));
		assertEquals("exact", bodyOf(read(config, "file:///a")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: deterministic specificity ranking
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_mostLiteralTemplateWins() {
		var config = new McpServerConfig()
			.addResourceTemplate(template("file:///{+full}", (u, v) -> text("generic:" + v.get("full"))))
			.addResourceTemplate(template("file:///a/{x}", (u, v) -> text("specific:" + v.get("x"))));
		assertEquals("specific:one", bodyOf(read(config, "file:///a/one")));
	}

	@Test void b02_fewerVariablesBreaksEqualLiteralTie() {
		// Both templates have 5 literal octets ("res/" + "/" for the two-var form; "res/" + "!" for the
		// reserved one-var form) and both match "res/one/two!"; the one-variable form must win.
		var config = new McpServerConfig()
			.addResourceTemplate(template("res/{a}/{b}", (u, v) -> text("two-vars")))
			.addResourceTemplate(template("res/{+c}!", (u, v) -> text("one-var:" + v.get("c"))));
		assertEquals("one-var:one/two", bodyOf(read(config, "res/one/two!")));
	}

	@Test void b03_registrationOrderBreaksCompleteTies() {
		var config = new McpServerConfig()
			.addResourceTemplate(template("file:///{p}", (u, v) -> text("first")))
			.addResourceTemplate(template("file:///{q}", (u, v) -> text("second")));
		assertEquals("first", bodyOf(read(config, "file:///x")));
	}

	@Test void b04_winnerReceivesOriginalUriDecodedInsertionOrderedVariablesAndBeanStore() {
		var captured = new HashMap<String,Object>();
		var config = new McpServerConfig().addResourceTemplate(template("file:///{b}/{a}", (u, v) -> {
			captured.put("uri", u);
			captured.put("variables", v);
			return text("ok");
		}));
		var marker = new BasicBeanStore();
		var resp = new McpRevision(null).dispatch(
			new McpExchange(req(1, McpMethods.RESOURCES_READ, JsonMap.of("uri", "file:///Caf%C3%A9/two")), n -> null),
			config, marker);
		assertNull(resp.getError());
		assertEquals("file:///Caf%C3%A9/two", captured.get("uri"));
		@SuppressWarnings("unchecked")
		var variables = (Map<String,String>) captured.get("variables");
		assertEquals("Café", variables.get("b"));
		assertEquals("two", variables.get("a"));
		assertThrows(UnsupportedOperationException.class, () -> variables.put("x", "y"));
		assertEquals(List.of("b", "a"), new ArrayList<>(variables.keySet()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: not-found / no-fallthrough / handler exceptions
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_noCandidateIsResourceNotFound() {
		var resp = readResponse(new McpServerConfig(), "file:///ghost");
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test void c02_listingOnlyWinnerIsNotFound() {
		var config = new McpServerConfig().addResourceTemplate(
			new McpResourceTemplateSpec().setUriTemplate("file:///{x}").setName("listing-only"));
		var resp = readResponse(config, "file:///a");
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test void c03_nullOutcomeIsNotFoundWithNoFallthrough() {
		// The more-specific template wins selection and returns null; a less-specific template that would
		// also match must never be tried as a fallback.
		var config = new McpServerConfig()
			.addResourceTemplate(template("file:///{+full}", (u, v) -> text("fallback-should-not-run")))
			.addResourceTemplate(template("file:///a/{x}", (u, v) -> null));
		var resp = readResponse(config, "file:///a/one");
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test void c04_handlerExceptionIsInternalError() {
		var config = new McpServerConfig().addResourceTemplate(template("file:///{x}", (u, v) -> {
			throw new RuntimeException("boom");
		}));
		var resp = readResponse(config, "file:///a");
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
		assertEquals("boom", resp.getError().getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: malformed percent escape and non-matchable templates
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_malformedEscapeStillAllowsExactLookup() {
		var config = new McpServerConfig().addResource(resource("file:///a%zzb", u -> text("exact")));
		assertEquals("exact", bodyOf(read(config, "file:///a%zzb")));
	}

	@Test void d02_malformedEscapeNeverTemplateMatches() {
		var config = new McpServerConfig().addResourceTemplate(template("file:///{x}", (u, v) -> text("template")));
		var resp = readResponse(config, "file:///a%zzb");
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	@Test void d03_validNonMatchableTemplateListsButNeverReadMatches() {
		var config = new McpServerConfig().addResourceTemplate(template("file:///{x,y}", (u, v) -> text("should-not-run")));
		var list = (ListResourceTemplatesResult) send(config, req(1, McpMethods.RESOURCES_TEMPLATES_LIST, null)).getResult();
		assertEquals(1, list.getResourceTemplates().size());
		var resp = readResponse(config, "file:///a,b");
		assertEquals(McpRevision.CODE_RESOURCE_NOT_FOUND, resp.getError().getCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: resources/templates/list wire mapping, pagination, and capability
	//-----------------------------------------------------------------------------------------------------------------

	private static McpResourceTemplateSpec spec(String name) {
		return new McpResourceTemplateSpec().setUriTemplate("file:///" + name + "/{x}").setName(name)
			.setTitle(name + "-title").setDescription(name + "-desc").setMimeType("text/plain");
	}

	@Test void e01_emptyRegistrySucceedsWithEmptyList() {
		var list = (ListResourceTemplatesResult) send(new McpServerConfig(), req(1, McpMethods.RESOURCES_TEMPLATES_LIST, null)).getResult();
		assertNotNull(list.getResourceTemplates());
		assertTrue(list.getResourceTemplates().isEmpty());
	}

	@Test void e02_listMapsAllFiveFieldsInHandlerOrder() {
		var config = new McpServerConfig().addResourceTemplate(spec("a"), spec("b"));
		var list = (ListResourceTemplatesResult) send(config, req(1, McpMethods.RESOURCES_TEMPLATES_LIST, null)).getResult();
		assertEquals(2, list.getResourceTemplates().size());
		var first = list.getResourceTemplates().get(0);
		assertEquals("file:///a/{x}", first.getUriTemplate());
		assertEquals("a", first.getName());
		assertEquals("a-title", first.getTitle());
		assertEquals("a-desc", first.getDescription());
		assertEquals("text/plain", first.getMimeType());
		assertEquals("b", list.getResourceTemplates().get(1).getName());
	}

	@Test void e03_listIsPagedInRegistrationOrder() {
		var config = new McpServerConfig().setCursor(McpCursor.fixedSize(1)).addResourceTemplate(spec("a"), spec("b"));
		var first = (ListResourceTemplatesResult) send(config, req(1, McpMethods.RESOURCES_TEMPLATES_LIST, null)).getResult();
		assertEquals("a", first.getResourceTemplates().get(0).getName());
		assertEquals("1", first.getNextCursor());
		var second = (ListResourceTemplatesResult) send(config,
			req(2, McpMethods.RESOURCES_TEMPLATES_LIST, JsonMap.of("cursor", "1"))).getResult();
		assertEquals("b", second.getResourceTemplates().get(0).getName());
		assertNull(second.getNextCursor());
	}

	@Test void e04_templateOnlyRegistrationDerivesResourcesCapability() {
		var config = new McpServerConfig().addResourceTemplate(spec("a"));
		var result = (InitializeResult) send(config, req(1, McpMethods.INITIALIZE, null)).getResult();
		assertNotNull(result.getCapabilities().getResources());
	}

	@Test void e05_explicitCapabilitiesRemainAuthoritativeWithTemplates() {
		var revision = new McpRevision(new ServerCapabilities().setPrompts(new PromptCapability()));
		var config = new McpServerConfig().addResourceTemplate(spec("a"));
		var resp = revision.dispatch(new McpExchange(req(1, McpMethods.INITIALIZE, null), n -> null), config, ctx);
		var result = (InitializeResult) resp.getResult();
		assertNotNull(result.getCapabilities().getPrompts());
		assertNull(result.getCapabilities().getResources());
	}
}
