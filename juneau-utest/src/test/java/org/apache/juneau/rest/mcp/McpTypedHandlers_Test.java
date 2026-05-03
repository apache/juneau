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
package org.apache.juneau.rest.mcp;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.mcp.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
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

	public static class EchoResult {
		private String text;

		public String getText() { return text; }
		public EchoResult setText(String text) { this.text = text; return this; }
	}

	private final BasicBeanStore ctx = BasicBeanStore.create().build();
	private final McpDispatcher dispatcher = new McpDispatcher();

	@Test
	void typedTool_argsBound_andResultWrappedAsText() {
		var typed = new McpTypedToolHandler<EchoArgs, EchoResult>() {
			@Override
			public Tool descriptor() {
				return new Tool().setName("echo");
			}

			@Override
			public Class<EchoArgs> argumentType() {
				return EchoArgs.class;
			}

			@Override
			public EchoResult call(EchoArgs args, BasicBeanStore ctx) {
				return new EchoResult().setText(args.getMessage() + ":" + args.getRepeat());
			}
		};
		var raw = McpTypedHandlers.adaptTool(typed);
		var config = new McpServerConfig().addTool(raw);

		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(JsonMap.of("name", "echo", "arguments", JsonMap.of("message", "hi", "repeat", 3)));
		var resp = dispatcher.dispatch(req, config, ctx);
		var ctr = (CallToolResult) resp.getResult();
		var text = ((TextContent) ctr.getContent().get(0)).getText();
		assertContains("\"text\":\"hi:3\"", text);
	}

	@Test
	void typedTool_returningCallToolResult_passesThrough() {
		var ctr = new CallToolResult().setContent(List.of(new TextContent().setText("direct")));
		var typed = new McpTypedToolHandler<EchoArgs, CallToolResult>() {
			@Override
			public Tool descriptor() {
				return new Tool().setName("d");
			}

			@Override
			public Class<EchoArgs> argumentType() {
				return EchoArgs.class;
			}

			@Override
			public CallToolResult call(EchoArgs args, BasicBeanStore ctx) {
				return ctr;
			}
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(JsonMap.of("name", "d"));
		var resp = dispatcher.dispatch(req, config, ctx);
		assertSame(ctr, resp.getResult());
	}

	@Test
	void typedTool_returningString_wrapped() {
		var typed = new McpTypedToolHandler<EchoArgs, String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("s"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BasicBeanStore ctx) { return "hello"; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "s")), config, ctx);
		var ctr = (CallToolResult) resp.getResult();
		assertString("hello", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void typedTool_nullResult_wrappedAsEmpty() {
		var typed = new McpTypedToolHandler<EchoArgs, EchoResult>() {
			@Override
			public Tool descriptor() { return new Tool().setName("n"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public EchoResult call(EchoArgs args, BasicBeanStore ctx) { return null; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "n")), config, ctx);
		var ctr = (CallToolResult) resp.getResult();
		assertString("", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void typedTool_nullArgs_passNull() {
		var typed = new McpTypedToolHandler<EchoArgs, String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("z"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BasicBeanStore ctx) {
				return args == null ? "null" : "not-null";
			}
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "z")), config, ctx);
		var ctr = (CallToolResult) resp.getResult();
		assertString("null", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void typedTool_argBindingFailure_invalidParams() {
		var typed = new McpTypedToolHandler<EchoArgs, String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("x"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BasicBeanStore ctx) { return "ok"; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		// Bad: 'repeat' should be int, supply a non-numeric value to trigger parser failure.
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(JsonMap.of("name", "x", "arguments", JsonMap.of("repeat", "not-an-int"))), config, ctx);
		assertEquals(McpDispatcher.CODE_INVALID_PARAMS, resp.getError().getCode());
	}

	@Test
	void typedPrompt_nullArgs_passNull() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BasicBeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : "non-null");
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var config = new McpServerConfig().addPrompt(raw);
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_GET).setParams(JsonMap.of("name", "p")), config, ctx);
		var pr = (GetPromptResult) resp.getResult();
		assertString("null", pr.getDescription());
	}

	public static class Unserializable {
		public String getValue() {
			throw new RuntimeException("intentional serialize failure");
		}
	}

	@Test
	void adaptTool_nullArgumentsMap_propagatesNull() {
		var typed = new McpTypedToolHandler<EchoArgs, String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("z"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BasicBeanStore ctx) {
				return args == null ? "null" : "not-null";
			}
		};
		var raw = McpTypedHandlers.adaptTool(typed);
		var ctr = raw.call(null, ctx);
		assertString("null", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void adaptPrompt_nullArgumentsMap_propagatesNull() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BasicBeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : "non-null");
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var pr = raw.get(null, ctx);
		assertString("null", pr.getDescription());
	}

	@Test
	void typedTool_unserializableResult_internalError() {
		var typed = new McpTypedToolHandler<EchoArgs, Unserializable>() {
			@Override
			public Tool descriptor() { return new Tool().setName("u"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public Unserializable call(EchoArgs args, BasicBeanStore ctx) { return new Unserializable(); }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "u")), config, ctx);
		assertEquals(McpDispatcher.CODE_INTERNAL_ERROR, resp.getError().getCode());
	}

	@Test
	void typedPrompt_argsBoundAndResult() {
		var typed = new McpTypedPromptHandler<EchoArgs>() {
			@Override
			public Prompt descriptor() { return new Prompt().setName("p"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public GetPromptResult get(EchoArgs args, BasicBeanStore ctx) {
				return new GetPromptResult().setDescription(args == null ? "null" : args.getMessage());
			}
		};
		var raw = McpTypedHandlers.adaptPrompt(typed);
		var config = new McpServerConfig().addPrompt(raw);
		var resp = dispatcher.dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.PROMPTS_GET)
			.setParams(JsonMap.of("name", "p", "arguments", JsonMap.of("message", "hello"))), config, ctx);
		var pr = (GetPromptResult) resp.getResult();
		assertString("hello", pr.getDescription());
	}
}
