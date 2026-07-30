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
		return revision.dispatch(new McpExchange(req, n -> null), config, ctx);
	}

	@Test
	void typedPrompt_nullArgs_passNull() {
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
	void adaptPrompt_nullArgumentsMap_propagatesNull() {
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
	void typedPrompt_argsBoundAndResult() {
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
