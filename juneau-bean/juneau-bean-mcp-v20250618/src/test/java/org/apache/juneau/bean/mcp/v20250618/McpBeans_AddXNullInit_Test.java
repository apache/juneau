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
package org.apache.juneau.bean.mcp.v20250618;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Covers the {@code addX(Collection/varargs)}/{@code putX(...)} null-init branch
 * (<code>if (field == null) field = list()/map();</code>) across the module's collection/map-backed beans.
 *
 * <p>
 * The module's round-trip suite ({@link McpBeans_RoundTrip_Test}) only ever calls the {@code setX(...)} form
 * first, pre-populating the backing collection, so the null-init branch of every {@code addX()}/{@code putX()}
 * method was never exercised (module was 0% branch). Each test below calls the adder/putter twice on a fresh
 * (null-backed) instance: the first call exercises the {@code true} (null-init) branch, the second exercises the
 * {@code false} (already-materialized) branch, closing both sides of the conditional.
 */
class McpBeans_AddXNullInit_Test {

	@Test void a01_callToolRequest_putArgument_nullInit() {
		var x = new CallToolRequest().putArgument("a", 1).putArgument("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), x.getArguments());
	}

	@Test void a02_callToolResult_addContent_varargs_nullInit() {
		var x = new CallToolResult().addContent(new TextContent().setText("a")).addContent(new TextContent().setText("b"));
		assertEquals(2, x.getContent().size());
	}

	@Test void a03_callToolResult_addContent_collection_nullInit() {
		var x = new CallToolResult()
			.addContent(List.of(new TextContent().setText("a")))
			.addContent(List.of(new TextContent().setText("b")));
		assertEquals(2, x.getContent().size());
	}

	@Test void a04_clientCapabilities_putSampling_nullInit() {
		var x = new ClientCapabilities().putSampling("a", 1).putSampling("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), x.getSampling());
	}

	@Test void a05_clientCapabilities_putExperimental_nullInit() {
		var x = new ClientCapabilities().putExperimental("a", 1).putExperimental("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), x.getExperimental());
	}

	@Test void a06_getPromptRequest_putArgument_nullInit() {
		var x = new GetPromptRequest().putArgument("a", 1).putArgument("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), x.getArguments());
	}

	@Test void a07_getPromptResult_addMessages_varargs_nullInit() {
		var m1 = new PromptMessage().setRole(Role.USER);
		var m2 = new PromptMessage().setRole(Role.ASSISTANT);
		var x = new GetPromptResult().addMessages(m1).addMessages(m2);
		assertEquals(2, x.getMessages().size());
	}

	@Test void a08_getPromptResult_addMessages_collection_nullInit() {
		var m1 = new PromptMessage().setRole(Role.USER);
		var m2 = new PromptMessage().setRole(Role.ASSISTANT);
		var x = new GetPromptResult().addMessages(List.of(m1)).addMessages(List.of(m2));
		assertEquals(2, x.getMessages().size());
	}

	@Test void a09_jsonSchema_addProperty_nullInit() {
		var x = new JsonSchema().addProperty("a", new JsonSchema().setType("string")).addProperty("b", new JsonSchema().setType("number"));
		assertEquals(2, x.getProperties().size());
	}

	@Test void a10_jsonSchema_addRequired_varargs_nullInit() {
		var x = new JsonSchema().addRequired("a").addRequired("b");
		assertEquals(List.of("a", "b"), x.getRequired());
	}

	@Test void a11_jsonSchema_addRequired_collection_nullInit() {
		var x = new JsonSchema().addRequired(List.of("a")).addRequired(List.of("b"));
		assertEquals(List.of("a", "b"), x.getRequired());
	}

	@Test void a12_jsonSchema_addDef_nullInit() {
		var x = new JsonSchema().addDef("a", new JsonSchema().setType("string")).addDef("b", new JsonSchema().setType("number"));
		assertEquals(2, x.getDefs().size());
	}

	@Test void a13_listPromptsResult_addPrompts_varargs_nullInit() {
		var x = new ListPromptsResult().addPrompts(new Prompt().setName("a")).addPrompts(new Prompt().setName("b"));
		assertEquals(2, x.getPrompts().size());
	}

	@Test void a14_listPromptsResult_addPrompts_collection_nullInit() {
		var x = new ListPromptsResult().addPrompts(List.of(new Prompt().setName("a"))).addPrompts(List.of(new Prompt().setName("b")));
		assertEquals(2, x.getPrompts().size());
	}

	@Test void a15_listResourcesResult_addResources_varargs_nullInit() {
		var x = new ListResourcesResult().addResources(new Resource().setUri("a")).addResources(new Resource().setUri("b"));
		assertEquals(2, x.getResources().size());
	}

	@Test void a16_listResourcesResult_addResources_collection_nullInit() {
		var x = new ListResourcesResult()
			.addResources(List.of(new Resource().setUri("a")))
			.addResources(List.of(new Resource().setUri("b")));
		assertEquals(2, x.getResources().size());
	}

	@Test void a17_listToolsResult_addTools_varargs_nullInit() {
		var x = new ListToolsResult().addTools(new Tool().setName("a")).addTools(new Tool().setName("b"));
		assertEquals(2, x.getTools().size());
	}

	@Test void a18_listToolsResult_addTools_collection_nullInit() {
		var x = new ListToolsResult().addTools(List.of(new Tool().setName("a"))).addTools(List.of(new Tool().setName("b")));
		assertEquals(2, x.getTools().size());
	}

	@Test void a19_prompt_addArguments_varargs_nullInit() {
		var x = new Prompt().addArguments(new PromptArgument().setName("a")).addArguments(new PromptArgument().setName("b"));
		assertEquals(2, x.getArguments().size());
	}

	@Test void a20_prompt_addArguments_collection_nullInit() {
		var x = new Prompt()
			.addArguments(List.of(new PromptArgument().setName("a")))
			.addArguments(List.of(new PromptArgument().setName("b")));
		assertEquals(2, x.getArguments().size());
	}

	@Test void a21_readResourceResult_addContents_varargs_nullInit() {
		var x = new ReadResourceResult()
			.addContents(new TextResourceContents().setUri("a"))
			.addContents(new TextResourceContents().setUri("b"));
		assertEquals(2, x.getContents().size());
	}

	@Test void a22_readResourceResult_addContents_collection_nullInit() {
		var x = new ReadResourceResult()
			.addContents(List.of(new TextResourceContents().setUri("a")))
			.addContents(List.of(new TextResourceContents().setUri("b")));
		assertEquals(2, x.getContents().size());
	}

	@Test void a23_serverCapabilities_putExperimental_nullInit() {
		var x = new ServerCapabilities().putExperimental("a", 1).putExperimental("b", 2);
		assertEquals(Map.of("a", 1, "b", 2), x.getExperimental());
	}

	@Test void a24_listResourceTemplatesResult_addResourceTemplates_varargs_nullInit() {
		var x = new ListResourceTemplatesResult()
			.addResourceTemplates(new ResourceTemplate().setUriTemplate("a"))
			.addResourceTemplates(new ResourceTemplate().setUriTemplate("b"));
		assertEquals(2, x.getResourceTemplates().size());
	}

	@Test void a25_listResourceTemplatesResult_addResourceTemplates_collection_nullInit() {
		var x = new ListResourceTemplatesResult()
			.addResourceTemplates(List.of(new ResourceTemplate().setUriTemplate("a")))
			.addResourceTemplates(List.of(new ResourceTemplate().setUriTemplate("b")));
		assertEquals(2, x.getResourceTemplates().size());
	}

	@Test void a26_listResourceTemplatesResult_getResourceTemplates_isUnmodifiableView() {
		var x = new ListResourceTemplatesResult().setResourceTemplates(new ResourceTemplate().setUriTemplate("a"));
		assertThrows(UnsupportedOperationException.class,
			() -> x.getResourceTemplates().add(new ResourceTemplate().setUriTemplate("b")));
	}

	@Test void a27_completionContext_putArgument_nullInit() {
		var x = new CompletionContext().putArgument("a", "1").putArgument("b", "2");
		assertEquals(Map.of("a", "1", "b", "2"), x.getArguments());
	}

	@Test void a28_completion_addValues_varargs_nullInit() {
		var x = new Completion().addValues("a").addValues("b");
		assertEquals(List.of("a", "b"), x.getValues());
	}

	@Test void a29_completion_addValues_collection_nullInit() {
		var x = new Completion().addValues(List.of("a")).addValues(List.of("b"));
		assertEquals(List.of("a", "b"), x.getValues());
	}
}
