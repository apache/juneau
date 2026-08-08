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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.mcp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpTypedHandlers} adapters.
 */
class McpTypedHandlers_Test {

	public static class EchoArgs {
		private String message;
		private int repeat = 1;

		public String getMessage() { return message; }
		public EchoArgs setMessage(String message) { this.message = message; return this; }
		public int getRepeat() { return repeat; }
		public EchoArgs setRepeat(int repeat) { this.repeat = repeat; return this; }
	}

	private final BeanStore ctx = new BasicBeanStore();
	private final McpRevision revision = new McpRevision(null);

	private JsonRpcResponse dispatch(JsonRpcRequest req, McpServerConfig config) {
		var result = revision.dispatch(new McpExchange(req, n -> null), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	@Test
	void b01_typedPrompt_nullArgs_passNull() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : "non-null");
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var config = new McpServerConfig().addPrompt(raw);
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_GET).setParams(JsonMap.of("name", "p")), config);
		var pr = (GetPromptResult) resp.getResult();
		assertString("null", pr.getDescription());
	}

	@Test
	void c02_adaptPrompt_nullArgumentsMap_propagatesNull() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : "non-null");
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var pr = raw.get(null, ctx);
		assertString("null", pr.getDescription());
	}

	@Test
	void b03_typedPrompt_descriptorArgumentsAndAllRoleMessages_roundTrip() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() {
				return new Prompt().setName("m").setArguments(List.of(new PromptArgument().setName("who").setRequired(true)));
			}
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BeanStore ctx) {
				return new GetPromptResult().setMessages(List.of(
					new PromptMessage().setRole(Role.USER).setContent(new TextContent().setText("hi")),
					new PromptMessage().setRole(Role.ASSISTANT).setContent(new TextContent().setText("hello")),
					new PromptMessage().setRole(Role.SYSTEM).setContent(new TextContent().setText("sys")),
					new PromptMessage().setRole(Role.TOOL).setContent(new TextContent().setText("tool"))));
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var config = new McpServerConfig().addPrompt(raw);

		var list = (ListPromptsResult) dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_LIST).setParams(null), config).getResult();
		assertSize(1, list.getPrompts().get(0).getArguments());
		assertString("who", list.getPrompts().get(0).getArguments().get(0).getName());

		var pr = (GetPromptResult) dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_GET).setParams(JsonMap.of("name", "m")), config).getResult();
		assertSize(4, pr.getMessages());
		assertEquals(Role.USER, pr.getMessages().get(0).getRole());
		assertEquals(Role.ASSISTANT, pr.getMessages().get(1).getRole());
		assertEquals(Role.SYSTEM, pr.getMessages().get(2).getRole());
		assertEquals(Role.TOOL, pr.getMessages().get(3).getRole());
	}

	@Test
	void b02_typedPrompt_argsBoundAndResult() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : args.getMessage());
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var config = new McpServerConfig().addPrompt(raw);
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_GET)
			.setParams(JsonMap.of("name", "p", "arguments", JsonMap.of("message", "hello"))), config);
		var pr = (GetPromptResult) resp.getResult();
		assertString("hello", pr.getDescription());
	}
}
