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
package org.apache.juneau.rest.client.mcp.v20250618;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.client.mcp.*;

/**
 * Typed <c>2025-06-18</c> Model Context Protocol (MCP) client facade.
 *
 * <p>
 * Extends {@link AbstractMcpClient}, adding one typed method per <c>2025-06-18</c> JSON-RPC method
 * (see {@link McpMethods}). Every method builds the corresponding v1 request bean, wraps it in a
 * {@link JsonRpcRequest}, delegates to the inherited {@link #send(JsonRpcRequest)}, and then either returns the
 * <c>result</c> re-marshaled into its typed v1 result bean, or throws an {@link McpException} built from the
 * response's <c>error</c>.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This class builds directly on {@code org.apache.juneau.rest.client.mcp},
 * which is itself Beta.
 *
 * @since 10.0.0
 */
public class McpClient extends AbstractMcpClient {

	private static final JsonParser RESULT_PARSER = JsonParser.create()
		.typePropertyName(Content.class, "type")
		.typePropertyName(ResourceContents.class, "type")
		.build();

	private static final JsonSerializer REQUEST_SERIALIZER = JsonSerializer.create()
		.addBeanTypes()
		.typePropertyName(CompletionReference.class, "type")
		.build();

	private final AtomicLong nextId = new AtomicLong(1);

	/**
	 * Constructor.
	 *
	 * @param builder The builder supplying this client's configuration. Must not be <jk>null</jk>.
	 */
	protected McpClient(Builder builder) {
		super(builder);
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Sends {@value McpMethods#INITIALIZE}.
	 *
	 * @return The initialize result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public InitializeResult initialize() throws IOException {
		var params = new InitializeRequest()
			.setProtocolVersion(McpProtocol.VERSION_2025_06_18)
			.setCapabilities(new ClientCapabilities())
			.setClientInfo(new Implementation().setName("juneau-rest-client-mcp-v20250618").setVersion(McpProtocol.VERSION_2025_06_18));
		return call(McpMethods.INITIALIZE, params, InitializeResult.class);
	}

	/**
	 * Sends {@value McpMethods#PING} to verify server liveness.
	 *
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public void ping() throws IOException {
		call(McpMethods.PING, null, JsonMap.class);
	}

	/**
	 * Sends {@value McpMethods#TOOLS_LIST}.
	 *
	 * @return The tools-list result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListToolsResult listTools() throws IOException {
		return call(McpMethods.TOOLS_LIST, null, ListToolsResult.class);
	}

	/**
	 * Sends {@value McpMethods#TOOLS_CALL}.
	 *
	 * @param name The tool name to invoke.
	 * @param arguments The tool arguments. Can be <jk>null</jk>.
	 * @return The call-tool result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public CallToolResult callTool(String name, Map<String,Object> arguments) throws IOException {
		var params = new CallToolRequest().setName(name).setArguments(arguments);
		return call(McpMethods.TOOLS_CALL, params, CallToolResult.class);
	}

	/**
	 * Sends {@value McpMethods#PROMPTS_LIST}.
	 *
	 * @return The prompts-list result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListPromptsResult listPrompts() throws IOException {
		return call(McpMethods.PROMPTS_LIST, null, ListPromptsResult.class);
	}

	/**
	 * Sends {@value McpMethods#PROMPTS_GET}.
	 *
	 * @param name The prompt name to fetch.
	 * @param arguments The prompt argument values. Can be <jk>null</jk>.
	 * @return The get-prompt result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public GetPromptResult getPrompt(String name, Map<String,Object> arguments) throws IOException {
		var params = new GetPromptRequest().setName(name).setArguments(arguments);
		return call(McpMethods.PROMPTS_GET, params, GetPromptResult.class);
	}

	/**
	 * Sends {@value McpMethods#RESOURCES_LIST}.
	 *
	 * @return The resources-list result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListResourcesResult listResources() throws IOException {
		return call(McpMethods.RESOURCES_LIST, null, ListResourcesResult.class);
	}

	/**
	 * Sends {@value McpMethods#RESOURCES_READ}.
	 *
	 * @param uri The resource URI to read.
	 * @return The read-resource result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ReadResourceResult readResource(String uri) throws IOException {
		var params = new ReadResourceRequest().setUri(uri);
		return call(McpMethods.RESOURCES_READ, params, ReadResourceResult.class);
	}

	/**
	 * Sends {@value McpMethods#RESOURCES_TEMPLATES_LIST}.
	 *
	 * @return The resource-templates-list result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListResourceTemplatesResult listResourceTemplates() throws IOException {
		return call(McpMethods.RESOURCES_TEMPLATES_LIST, null, ListResourceTemplatesResult.class);
	}

	/**
	 * Sends {@value McpMethods#COMPLETION_COMPLETE}.
	 *
	 * @param ref The completion target (a declared prompt or resource template reference).
	 * @param argument The prompt argument or resource-template variable being completed, and its current partial
	 * 	value.
	 * @return The completion result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public CompleteResult complete(CompletionReference ref, CompletionArgument argument) throws IOException {
		var params = new CompleteRequest().setRef(ref).setArgument(argument);
		return call(McpMethods.COMPLETION_COMPLETE, params, CompleteResult.class);
	}

	private static Object toWireParams(Object params) {
		return JsonParser.DEFAULT.read(REQUEST_SERIALIZER.write(params), Object.class);
	}

	/**
	 * Builds and sends a JSON-RPC request for {@code method}, mapping a JSON-RPC {@code error} to a thrown
	 * {@link McpException} and a non-<jk>null</jk> {@code result} to {@code resultType} via {@link #RESULT_PARSER}.
	 *
	 * <p>
	 * {@code params}, if non-<jk>null</jk>, is uniformly pre-flattened through {@link #toWireParams(Object)}
	 * before being placed on the request - the same shape the v2 ({@code 2026-07-28}) adapter's {@code call(...)}
	 * applies unconditionally, so a param bean with a polymorphic field (e.g. {@link CompletionReference}) is
	 * never accidentally sent through {@link org.apache.juneau.rest.client.RestClient}'s
	 * {@code addBeanTypes=false} default serializer without its type discriminator, regardless of which typed
	 * method call reaches this shared path.
	 *
	 * @param <T> The typed v1 result bean type.
	 * @param method The JSON-RPC method name (see {@link McpMethods}).
	 * @param params The method params bean, or <jk>null</jk> for methods that take none.
	 * @param resultType The typed v1 result bean class to re-marshal a non-<jk>null</jk> result into.
	 * @return The typed result, or <jk>null</jk> if the response's {@code result} was <jk>null</jk>.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	private <T> T call(String method, Object params, Class<T> resultType) throws IOException {
		var request = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(nextId.getAndIncrement())
			.setMethod(method)
			.setParams(params == null ? null : toWireParams(params));
		var response = send(request);
		var error = response.getError();
		if (error != null)
			throw McpException.fromJsonRpcError(error);
		var result = response.getResult();
		if (result == null)
			return null;
		return RESULT_PARSER.read(JsonSerializer.DEFAULT.toString(result), resultType);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link McpClient}.
	 *
	 * @since 10.0.0
	 */
	public static class Builder extends AbstractMcpClient.Builder<Builder> {

		/**
		 * Builds the client.
		 *
		 * @return A new {@link McpClient}. Never <jk>null</jk>.
		 */
		@SuppressWarnings("resource") // factory hands the built client to the caller, who must close it (McpClient implements Closeable).
		public McpClient build() {
			return new McpClient(this);
		}
	}
}
