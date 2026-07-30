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

	public static class EchoResult {
		private String text;

		public String getText() { return text; }
		public EchoResult setText(String text) { this.text = text; return this; }
	}

	private final BeanStore ctx = new BasicBeanStore();
	private final McpRevision revision = new McpRevision(null);

	private JsonRpcResponse dispatch(JsonRpcRequest req, McpServerConfig config) {
		return revision.dispatch(new McpExchange(req, n -> null), config, ctx);
	}

	@Test
	void a01_typedTool_argsBound_andResultWrappedAsText() {
		var typed = new McpTypedToolHandler<EchoArgs,EchoResult>() {
			@Override
			public Tool descriptor() {
				return new Tool().setName("echo");
			}

			@Override
			public Class<EchoArgs> argumentType() {
				return EchoArgs.class;
			}

			@Override
			public EchoResult call(EchoArgs args, BeanStore ctx) {
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
		var resp = dispatch(req, config);
		var ctr = (CallToolResult) resp.getResult();
		var text = ((TextContent) ctr.getContent().get(0)).getText();
		assertContains("\"text\":\"hi:3\"", text);
	}

	@Test
	void a02_typedTool_returningCallToolResult_passesThrough() {
		var ctr = new CallToolResult().setContent(List.of(new TextContent().setText("direct")));
		var typed = new McpTypedToolHandler<EchoArgs,CallToolResult>() {
			@Override
			public Tool descriptor() {
				return new Tool().setName("d");
			}

			@Override
			public Class<EchoArgs> argumentType() {
				return EchoArgs.class;
			}

			@Override
			public CallToolResult call(EchoArgs args, BeanStore ctx) {
				return ctr;
			}
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(JsonMap.of("name", "d"));
		var resp = dispatch(req, config);
		// The neutral/wire boundary always remaps the result, so only value equality survives here.
		var result = (CallToolResult) resp.getResult();
		assertString("direct", ((TextContent) result.getContent().get(0)).getText());
	}

	@Test
	void a03_typedTool_returningString_wrapped() {
		var typed = new McpTypedToolHandler<EchoArgs,String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("s"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BeanStore ctx) { return "hello"; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "s")), config);
		var ctr = (CallToolResult) resp.getResult();
		assertString("hello", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void a04_typedTool_nullResult_wrappedAsEmpty() {
		var typed = new McpTypedToolHandler<EchoArgs,EchoResult>() {
			@Override
			public Tool descriptor() { return new Tool().setName("n"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public EchoResult call(EchoArgs args, BeanStore ctx) { return null; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "n")), config);
		var ctr = (CallToolResult) resp.getResult();
		assertString("", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void a05_typedTool_nullArgs_passNull() {
		var typed = new McpTypedToolHandler<EchoArgs,String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("z"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BeanStore ctx) {
				return args == null ? "null" : "not-null";
			}
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "z")), config);
		var ctr = (CallToolResult) resp.getResult();
		assertString("null", ((TextContent) ctr.getContent().get(0)).getText());
	}

	@Test
	void a06_typedTool_argBindingFailure_invalidParams() {
		var typed = new McpTypedToolHandler<EchoArgs,String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("x"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BeanStore ctx) { return "ok"; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		// Bad: 'repeat' should be int, supply a non-numeric value to trigger parser failure.
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL)
			.setParams(JsonMap.of("name", "x", "arguments", JsonMap.of("repeat", "not-an-int"))), config);
		assertEquals(McpRevision.CODE_INVALID_PARAMS, resp.getError().getCode());
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

	public static class Unserializable {
		public String getValue() {
			throw new RuntimeException("intentional serialize failure");
		}
	}

	@Test
	void c01_adaptTool_nullArgumentsMap_propagatesNull() {
		var typed = new McpTypedToolHandler<EchoArgs,String>() {
			@Override
			public Tool descriptor() { return new Tool().setName("z"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public String call(EchoArgs args, BeanStore ctx) {
				return args == null ? "null" : "not-null";
			}
		};
		var raw = McpTypedHandlers.adaptTool(typed);
		var ctr = raw.call(null, ctx);
		assertString("null", ctr.getContent().get(0).text());
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
	void a07_typedTool_unserializableResult_internalError() {
		var typed = new McpTypedToolHandler<EchoArgs,Unserializable>() {
			@Override
			public Tool descriptor() { return new Tool().setName("u"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public Unserializable call(EchoArgs args, BeanStore ctx) { return new Unserializable(); }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "u")), config);
		assertEquals(McpRevision.CODE_INTERNAL_ERROR, resp.getError().getCode());
	}

	@Test
	void a08_typedTool_callToolResultWithImageAndResourceContent_roundTrips() {
		var ctr = new CallToolResult().setContent(List.of(
			new ImageContent().setData("aW1n").setMimeType("image/png"),
			new EmbeddedResourceContent().setResource(new TextResourceContents().setUri("r://x").setMimeType("text/plain").setText("inline")),
			new EmbeddedResourceContent().setResource(new BlobResourceContents().setUri("r://y").setMimeType("application/octet-stream").setBlob("QUJD"))));
		var typed = new McpTypedToolHandler<EchoArgs,CallToolResult>() {
			@Override
			public Tool descriptor() { return new Tool().setName("m"); }
			@Override
			public Class<EchoArgs> argumentType() { return EchoArgs.class; }
			@Override
			public CallToolResult call(EchoArgs args, BeanStore ctx) { return ctr; }
		};
		var config = new McpServerConfig().addTool(McpTypedHandlers.adaptTool(typed));
		var resp = dispatch(new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod(McpMethods.TOOLS_CALL).setParams(JsonMap.of("name", "m")), config);
		var result = (CallToolResult) resp.getResult();
		assertSize(3, result.getContent());
		assertString("aW1n", ((ImageContent) result.getContent().get(0)).getData());
		var textResource = (EmbeddedResourceContent) result.getContent().get(1);
		assertString("inline", ((TextResourceContents) textResource.getResource()).getText());
		var blobResource = (EmbeddedResourceContent) result.getContent().get(2);
		assertString("QUJD", ((BlobResourceContents) blobResource.getResource()).getBlob());
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
