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
package org.apache.juneau.bean.mcp.v20260728;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonschema.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the plain accessors and the lazily-initialised {@code put}/{@code add} helpers on the MCP
 * 2026-07-28 request, result, capability, and content beans.
 *
 * <p>Each helper is called twice so that both the "create the collection" and "reuse the collection" paths run.</p>
 */
class McpBeanAccessors_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Request beans: map helpers.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_callToolRequest_mapHelpers() {
		var x = new CallToolRequest().putArgument("a", 1).putArgument("b", 2)
			.putInputResponse("i1", "x").putInputResponse("i2", "y");
		assertBean(x, "arguments{a,b},inputResponses{i1,i2}", "{1,2},{x,y}");
	}

	@Test void a02_getPromptRequest_accessorsAndMapHelpers() {
		var x = new GetPromptRequest().setName("p").putArgument("a", 1).putArgument("b", 2)
			.putInputResponse("i1", "x").putInputResponse("i2", "y");
		assertBean(x, "name,arguments{a,b},inputResponses{i1,i2}", "p,{1,2},{x,y}");
		x.setArguments(new LinkedHashMap<>(Map.of("c", 3)));
		assertBean(x, "arguments{c}", "{3}");
	}

	@Test void a03_readResourceRequest_accessorsAndMapHelpers() {
		var x = new ReadResourceRequest().setUri("file:///a").putInputResponse("i1", "x").putInputResponse("i2", "y");
		assertBean(x, "uri,inputResponses{i1,i2}", "file:///a,{x,y}");
	}

	@Test void a04_completionContext_putArgumentTwice() {
		var x = new CompletionContext().putArgument("a", "1").putArgument("b", "2");
		assertBean(x, "arguments{a,b}", "{1,2}");
	}

	@Test void a05_capabilities_experimentalAndExtensionsHelpers() {
		var c = new ClientCapabilities().putExperimental("a", 1).putExperimental("b", 2)
			.putExtensions("e1", 1).putExtensions("e2", 2);
		assertBean(c, "experimental{a,b},extensions{e1,e2}", "{1,2},{1,2}");
		var s = new ServerCapabilities().putExperimental("a", 1).putExperimental("b", 2)
			.putExtensions("e1", 1).putExtensions("e2", 2);
		assertBean(s, "experimental{a,b},extensions{e1,e2}", "{1,2},{1,2}");
	}

	@Test void a06_serverCapabilities_resourceCapability() {
		var r = new ResourceCapability().setListChanged(true).setSubscribe(true);
		assertBean(r, "listChanged,subscribe", "true,true");
		var s = new ServerCapabilities().setResources(r);
		assertBean(s, "resources{listChanged,subscribe}", "{true,true}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Result beans: list helpers (varargs, collection, and replace).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_callToolResult_contentHelpers() {
		var a = new CallToolResult().addContent(new TextContent().setText("a")).addContent(new TextContent().setText("b"));
		assertBean(a, "content{#{text}}", "{[{a},{b}]}");
		var b = new CallToolResult().addContent(List.of(new TextContent().setText("a"))).addContent(List.of(new TextContent().setText("b")));
		assertBean(b, "content{#{text}}", "{[{a},{b}]}");
		var c = new CallToolResult().setContent(new TextContent().setText("c"));
		assertBean(c, "content{#{text}}", "{[{c}]}");
	}

	@Test void b02_completion_valuesHelpers() {
		assertList(new Completion().setValues("a", "b").getValues(), "a", "b");
		assertList(new Completion().addValues("a").addValues("b").getValues(), "a", "b");
		assertList(new Completion().addValues(List.of("a")).addValues(List.of("b")).getValues(), "a", "b");
	}

	@Test void b03_getPromptResult_messagesHelpers() {
		var m = new PromptMessage().setRole(Role.USER).setContent(new TextContent().setText("t"));
		assertBean(new GetPromptResult().setDescription("d").setMessages(List.of(m)),
			"description,messages{#{role,content{text}}}", "d,{[{USER,{t}}]}");
		assertBean(new GetPromptResult().setMessages(m), "messages{length}", "{1}");
		assertBean(new GetPromptResult().addMessages(m).addMessages(m), "messages{length}", "{2}");
		assertBean(new GetPromptResult().addMessages(List.of(m)).addMessages(List.of(m)), "messages{length}", "{2}");
	}

	@Test void b04_listPromptsResult_promptsHelpers() {
		var p = new Prompt().setName("p");
		assertBean(new ListPromptsResult().addPrompts(p).addPrompts(p).setNextCursor("c"), "prompts{length},nextCursor", "{2},c");
		assertBean(new ListPromptsResult().addPrompts(List.of(p)).addPrompts(List.of(p)), "prompts{length}", "{2}");
		assertBean(new ListPromptsResult().setPrompts(p), "prompts{length}", "{1}");
	}

	@Test void b05_listResourcesResult_resourcesHelpers() {
		var r = new Resource().setUri("u");
		assertBean(new ListResourcesResult().addResources(r).addResources(r).setNextCursor("c"), "resources{length},nextCursor", "{2},c");
		assertBean(new ListResourcesResult().addResources(List.of(r)).addResources(List.of(r)), "resources{length}", "{2}");
		assertBean(new ListResourcesResult().setResources(r), "resources{length}", "{1}");
	}

	@Test void b06_listResourceTemplatesResult_templatesHelpers() {
		var t = new ResourceTemplate().setUriTemplate("u/{x}");
		assertBean(new ListResourceTemplatesResult().addResourceTemplates(t).addResourceTemplates(t).setNextCursor("c"),
			"resourceTemplates{length},nextCursor", "{2},c");
		assertBean(new ListResourceTemplatesResult().addResourceTemplates(List.of(t)).addResourceTemplates(List.of(t)),
			"resourceTemplates{length}", "{2}");
		assertBean(new ListResourceTemplatesResult().setResourceTemplates(t), "resourceTemplates{length}", "{1}");
	}

	@Test void b07_listToolsResult_toolsHelpers() {
		var t = new Tool().setName("t");
		assertBean(new ListToolsResult().addTools(t).addTools(t), "tools{length}", "{2}");
		assertBean(new ListToolsResult().addTools(List.of(t)).addTools(List.of(t)), "tools{length}", "{2}");
	}

	@Test void b08_readResourceResult_contentsHelpers() {
		var c = new TextResourceContents().setUri("u");
		assertBean(new ReadResourceResult().setContents(List.of(c)), "contents{length}", "{1}");
		assertBean(new ReadResourceResult().setContents(c, c), "contents{length}", "{2}");
		assertBean(new ReadResourceResult().addContents(c).addContents(c), "contents{length}", "{2}");
		assertBean(new ReadResourceResult().addContents(List.of(c)).addContents(List.of(c)), "contents{length}", "{2}");
	}

	@Test void b09_serverDiscoverResult_supportedVersionsHelpers() {
		assertList(new ServerDiscoverResult().addSupportedVersions("a").addSupportedVersions("b").getSupportedVersions(), "a", "b");
		assertList(new ServerDiscoverResult().addSupportedVersions(List.of("a")).addSupportedVersions(List.of("b")).getSupportedVersions(), "a", "b");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Metadata beans: icons, implementation, prompt.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_icon_sizesHelpersAndThemeWireToken() {
		assertList(new Icon().addSizes("a").addSizes("b").getSizes(), "a", "b");
		assertList(new Icon().addSizes(List.of("a")).addSizes(List.of("b")).getSizes(), "a", "b");
		assertList(new Icon().setSizes("a", "b").getSizes(), "a", "b");
		assertList(new Icon().setSizes(List.of("a")).getSizes(), "a");
		assertEquals("light", Icon.Theme.LIGHT.toWire());
		assertEquals("dark", Icon.Theme.DARK.toString());
	}

	@Test void c02_implementation_iconsHelpers() {
		var i = new Icon().setSrc("s");
		assertBean(new Implementation().addIcons(i).addIcons(i), "icons{length}", "{2}");
		assertBean(new Implementation().addIcons(List.of(i)).addIcons(List.of(i)), "icons{length}", "{2}");
		assertBean(new Implementation().setIcons(i), "icons{length}", "{1}");
	}

	@Test void c03_prompt_accessorsAndArgumentHelpers() {
		var a = new PromptArgument().setName("n").setDescription("d").setRequired(true);
		assertBean(a, "name,description,required", "n,d,true");
		assertBean(new Prompt().setName("p").setDescription("d").addArguments(a).addArguments(a),
			"name,description,arguments{length}", "p,d,{2}");
		assertBean(new Prompt().addArguments(List.of(a)).addArguments(List.of(a)), "arguments{length}", "{2}");
		assertBean(new Prompt().setArguments(a), "arguments{length}", "{1}");
		assertBean(new Prompt().setArguments(List.of(a)), "arguments{length}", "{1}");
	}

	@Test void c04_promptMessageAndImageContent_getters() {
		var t = new TextContent().setText("t");
		assertBean(new PromptMessage().setRole(Role.ASSISTANT).setContent(t), "role,content{text}", "ASSISTANT,{t}");
		assertBean(new ImageContent().setData("d").setMimeType("image/png"), "data,mimeType", "d,image/png");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Resource contents and enums.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_blobAndTextResourceContents_accessors() {
		assertBean(new BlobResourceContents().setUri("u").setMimeType("m").setBlob("b"), "uri,mimeType,blob", "u,m,b");
		assertBean(new TextResourceContents().setUri("u").setMimeType("m").setText("t"), "uri,mimeType,text", "u,m,t");
	}

	@Test void d02_embeddedResourceContent_resource() {
		var r = new TextResourceContents().setUri("u");
		assertBean(new EmbeddedResourceContent().setResource(r), "resource{uri}", "{u}");
	}

	@Test void d03_resource_accessors() {
		assertBean(new Resource().setUri("u").setName("n").setTitle("t").setDescription("d").setMimeType("m").setSize(5L),
			"uri,name,title,description,mimeType,size", "u,n,t,d,m,5");
	}

	@Test void d04_tool_schemasAndEnumWireTokens() {
		var in = new JsonSchema<>().setType(JsonType.OBJECT);
		var out = new JsonSchema<>().setType(JsonType.STRING);
		assertBean(new Tool().setInputSchema(in).setOutputSchema(out), "inputSchema{type},outputSchema{type}", "{OBJECT},{STRING}");
		assertEquals("private", McpCacheScope.PRIVATE.toWire());
		assertEquals("public", McpCacheScope.PUBLIC.toString());
		assertEquals("tool", Role.TOOL.toWire());
		assertEquals("system", Role.SYSTEM.toString());
	}
}
