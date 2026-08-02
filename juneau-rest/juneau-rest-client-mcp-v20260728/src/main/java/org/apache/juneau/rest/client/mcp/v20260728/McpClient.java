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
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.tracing.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.client.mcp.*;

/**
 * Typed <c>2026-07-28</c> Model Context Protocol (MCP) client facade.
 *
 * <p>
 * Extends {@link AbstractMcpClient}, adding one typed method per <c>2026-07-28</c> JSON-RPC method
 * (see {@link McpMethods}). Every method stamps the SEP-2243 {@code params._meta} envelope (protocol
 * version, client capabilities, client info, and any supplied trace-context fields), builds the
 * corresponding v2 request bean, wraps it in a {@link JsonRpcRequest} with the required
 * {@code Mcp-Method}/{@code Mcp-Name} headers, delegates to the inherited {@link #send(JsonRpcRequest, Map)},
 * and then either returns the {@code result} re-marshaled into its typed v2 result bean, or throws an
 * {@link McpException} built from the response's {@code error}.
 *
 * <p>
 * An optional {@link McpResponseCache} can be supplied via {@link Builder#responseCache(McpResponseCache)}
 * to serve repeat identical calls from cache instead of the wire, keyed off the method and business params
 * (never the per-call {@code _meta}, so a per-request trace-context supplier does not defeat the cache).
 *
 * <p>
 * A server-initiated request delivered over the duplex event-stream channel is routed through the
 * {@link McpDuplexDispatcher} registered via {@link #setServerRequestHandler(McpServerRequestHandler)}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This class builds directly on {@code org.apache.juneau.rest.client.mcp},
 * which is itself Beta.
 *
 * @since 10.0.0
 */
public final class McpClient extends AbstractMcpClient {

	private static final JsonSerializer REQUEST_SERIALIZER = JsonSerializer.create()
		.addBeanTypes()
		.typePropertyName(CompletionReference.class, "type")
		.build();

	/** JSON-RPC method name used for the client-to-server duplex return-channel POST. */
	private static final String DUPLEX_RETURN_METHOD = "mcp/clientResult";

	private final ClientCapabilities clientCapabilities;
	private final Implementation clientInfo;
	private final Supplier<Map<String,String>> traceFieldsSupplier;
	private final McpResponseCache responseCache;
	private final String privateScopePartitionPrefix;
	private final McpDuplexDispatcher duplexDispatcher = new McpDuplexDispatcher();

	private McpClient(Builder builder) {
		super(builder);
		this.clientCapabilities = builder.clientCapabilities;
		this.clientInfo = builder.clientInfo;
		this.traceFieldsSupplier = builder.traceFieldsSupplier;
		this.responseCache = builder.responseCache;
		this.privateScopePartitionPrefix = "private:" + UUID.randomUUID() + ":";
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
	 * Sends {@value McpMethods#SERVER_DISCOVER}.
	 *
	 * @return The server-discover result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ServerDiscoverResult serverDiscover() throws IOException {
		return call(McpMethods.SERVER_DISCOVER, new RequestParamsOnly(), ServerDiscoverResult.class);
	}

	/**
	 * Sends {@value McpMethods#PING} to verify server liveness.
	 *
	 * @return The ping result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public PingResult ping() throws IOException {
		return call(McpMethods.PING, new RequestParamsOnly(), PingResult.class);
	}

	/**
	 * Sends {@value McpMethods#TOOLS_LIST}.
	 *
	 * @return The tools-list result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListToolsResult listTools() throws IOException {
		return call(McpMethods.TOOLS_LIST, new RequestParamsOnly(), ListToolsResult.class);
	}

	/**
	 * Sends {@value McpMethods#TOOLS_CALL}.
	 *
	 * @param name The tool name to invoke.
	 * @param arguments The tool arguments. Can be <jk>null</jk> (sent as an empty object).
	 * @return The call-tool result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public CallToolResult callTool(String name, Map<String,Object> arguments) throws IOException {
		var params = new CallToolRequest().setName(name).setArguments(arguments == null ? JsonMap.of() : new JsonMap(arguments));
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
		return call(McpMethods.PROMPTS_LIST, new RequestParamsOnly(), ListPromptsResult.class);
	}

	/**
	 * Sends {@value McpMethods#PROMPTS_GET}.
	 *
	 * @param name The prompt name to fetch.
	 * @param arguments The prompt argument values. Can be <jk>null</jk> (sent as an empty object).
	 * @return The get-prompt result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public GetPromptResult getPrompt(String name, Map<String,Object> arguments) throws IOException {
		var params = new GetPromptRequest().setName(name).setArguments(arguments == null ? JsonMap.of() : new JsonMap(arguments));
		return call(McpMethods.PROMPTS_GET, params, GetPromptResult.class);
	}

	/**
	 * Sends {@value McpMethods#RESOURCES_LIST}.
	 *
	 * @return The resources-list result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ListResourcesResult listResources() throws IOException {
		return call(McpMethods.RESOURCES_LIST, new RequestParamsOnly(), ListResourcesResult.class);
	}

	/**
	 * Sends {@value McpMethods#RESOURCES_READ}.
	 *
	 * @param uri The resource URI to read.
	 * @return The read-resource result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ReadResourceResult readResource(String uri) throws IOException {
		return call(McpMethods.RESOURCES_READ, new ReadResourceRequest().setUri(uri), ReadResourceResult.class);
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
		return call(McpMethods.RESOURCES_TEMPLATES_LIST, new RequestParamsOnly(), ListResourceTemplatesResult.class);
	}

	/**
	 * Sends {@value McpMethods#COMPLETION_COMPLETE}.
	 *
	 * @param ref The completion target (a declared prompt or resource template reference).
	 * @param argumentName The prompt argument or resource-template variable being completed.
	 * @param argumentValue The current partial value of {@code argumentName}.
	 * @param contextArguments Previously-resolved argument values for context, or <jk>null</jk> to omit the
	 * 	{@code context} object entirely.
	 * @return The completion result. Never <jk>null</jk> on success unless the server returns a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public CompleteResult complete(CompletionReference ref, String argumentName, String argumentValue, Map<String,String> contextArguments) throws IOException {
		var argument = new CompletionArgument().setName(argumentName).setValue(argumentValue);
		var params = new CompleteRequest().setRef(ref).setArgument(argument);
		if (contextArguments != null)
			params.setContext(new CompletionContext().setArguments(contextArguments));
		return call(McpMethods.COMPLETION_COMPLETE, params, CompleteResult.class);
	}

	/**
	 * Returns this instance's cache-partition prefix used to namespace {@link McpResponseCache#SCOPE_PRIVATE}
	 * entries so two client instances sharing the same {@link McpResponseCache} never see each other's
	 * private-scope results.
	 *
	 * @return The per-instance private-scope partition prefix. Never <jk>null</jk>.
	 */
	String privateScopePartitionPrefix() {
		return privateScopePartitionPrefix;
	}

	/**
	 * Registers the handler invoked for server-initiated requests delivered over the duplex event-stream
	 * channel (see {@link McpDuplexDispatcher}).
	 *
	 * @param value The handler to register. Passing <jk>null</jk> de-registers any previously-registered handler.
	 * @return This object.
	 */
	public McpClient setServerRequestHandler(McpServerRequestHandler value) {
		duplexDispatcher.register(value);
		return this;
	}

	/**
	 * Opens the duplex event-stream channel, reads at most one server-initiated request, dispatches it to the
	 * registered {@link McpServerRequestHandler}, and posts the result (or a JSON-RPC error) back to the server
	 * via {@value #DUPLEX_RETURN_METHOD}.
	 *
	 * <p>
	 * Package-private test/plumbing entry point: production duplex pumping is driven by the caller repeatedly
	 * invoking this method (or an equivalent loop) for as long as the channel should stay open.
	 *
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 */
	void pumpNextServerMessage() throws IOException {
		try (var r = openEventStream()) {
			if (! r.hasNext())
				return;
			var event = r.next();
			if (event.getData() == null || event.getData().isEmpty())
				return;
			var req = JsonParser.DEFAULT.read(event.getData(), JsonRpcRequest.class);
			try {
				var result = duplexDispatcher.dispatch(req, BasicBeanStore.INSTANCE);
				if (JsonRpcResponse.notification(req.getId()))
					return;
				postClientResult(req.getId(), JsonRpcResponse.ok(req.getId(), result));
			} catch (McpException e) {
				postClientResult(req.getId(), JsonRpcResponse.errorResponse(req.getId(), e.getCode(), e.getMessage()));
			}
		}
	}

	// TODO C5/C6: finalize duplex return-channel header contract. Mcp-Name is always empty here because
	// DUPLEX_RETURN_METHOD has no routing-name mapping in McpRoutingNames; it is unclear whether the real
	// contract instead wants it to echo the correlated inbound request's tool/prompt/resource name.
	private void postClientResult(Object id, JsonRpcResponse payload) throws IOException {
		var headers = Map.of("Mcp-Method", DUPLEX_RETURN_METHOD, "Mcp-Name", McpRoutingNames.routingName(DUPLEX_RETURN_METHOD, payload));
		send(new JsonRpcRequest().setJsonrpc("2.0").setId(id).setMethod(DUPLEX_RETURN_METHOD).setParams(payload), headers);
	}

	private <T> T call(String method, RequestParams<?> params, Class<T> resultType) throws IOException {
		var cacheKey = cacheKey(method, params);
		var cached = readCache(cacheKey);
		if (cached != null)
			return resultType.cast(cached);
		stampMeta(params.getMeta() == null ? params.setMeta(new RequestMeta()).getMeta() : params.getMeta());
		var wireParams = toWireParams(params);
		var headers = Map.of("Mcp-Method", method, "Mcp-Name", McpRoutingNames.routingName(method, wireParams));
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(UUID.randomUUID().toString())
			.setMethod(method)
			.setParams(wireParams);
		var res = send(req, headers);
		if (res.getError() != null)
			throw McpException.fromJsonRpcError(res.getError());
		var result = JsonMap.of("value", res.getResult()).get("value", resultType);
		writeCache(cacheKey, result);
		return result;
	}

	private static Object toWireParams(Object params) {
		return JsonParser.DEFAULT.read(REQUEST_SERIALIZER.write(params), Object.class);
	}

	private void stampMeta(RequestMeta meta) {
		meta.setProtocolVersion(McpProtocol.VERSION_2026_07_28);
		meta.setClientCapabilities(clientCapabilities);
		if (clientInfo != null)
			meta.setClientInfo(clientInfo);
		if (traceFieldsSupplier != null) {
			var fields = traceFieldsSupplier.get();
			if (fields != null) {
				var carrier = new RequestMetaCarrier(meta);
				putIfPresent(carrier, RequestMeta.KEY_TRACEPARENT, fields.get(RequestMeta.KEY_TRACEPARENT));
				putIfPresent(carrier, RequestMeta.KEY_TRACESTATE, fields.get(RequestMeta.KEY_TRACESTATE));
				putIfPresent(carrier, RequestMeta.KEY_BAGGAGE, fields.get(RequestMeta.KEY_BAGGAGE));
			}
		}
	}

	private void putIfPresent(TraceContextCarrier carrier, String key, String value) {
		if (value != null)
			carrier.set(key, value);
	}

	/**
	 * Computes the cache key from {@code method} and the caller-supplied business {@code params} only.
	 *
	 * <p>
	 * Called before {@link #stampMeta(RequestMeta)} ever touches {@code params}, so the key never reflects
	 * per-call {@code _meta} content (protocol version, capabilities, or trace-context fields) — otherwise a
	 * real per-span {@link #traceFieldsSupplier} would make every key unique and defeat caching entirely.
	 */
	private String cacheKey(String method, Object params) {
		return method + "|" + JsonMap.of("p", params).toString();
	}

	private Object readCache(String key) {
		if (responseCache == null)
			return null;
		return responseCache.get(privateScopePartitionPrefix + McpResponseCache.SCOPE_PRIVATE, key)
			.or(() -> responseCache.get(McpResponseCache.SCOPE_PUBLIC, key))
			.orElse(null);
	}

	private void writeCache(String key, Object result) {
		if (responseCache == null)
			return;
		if (result instanceof CacheableResult<?> c && c.getTtlMs() != null) {
			var scope = c.getCacheScope() == McpCacheScope.PRIVATE
				? privateScopePartitionPrefix + McpResponseCache.SCOPE_PRIVATE
				: McpResponseCache.SCOPE_PUBLIC;
			responseCache.put(scope, key, result, c.getTtlMs());
		}
	}

	private static final class RequestMetaCarrier implements TraceContextCarrier {
		private final RequestMeta meta;
		RequestMetaCarrier(RequestMeta meta) { this.meta = meta; }
		@Override public String get(String key) {
			return switch (key) {
				case RequestMeta.KEY_TRACEPARENT -> meta.getTraceparent();
				case RequestMeta.KEY_TRACESTATE -> meta.getTracestate();
				case RequestMeta.KEY_BAGGAGE -> meta.getBaggage();
				default -> null;
			};
		}
		@Override public Iterable<String> keys() { return List.of(RequestMeta.KEY_TRACEPARENT, RequestMeta.KEY_TRACESTATE, RequestMeta.KEY_BAGGAGE); }
		@Override public void set(String key, String value) {
			switch (key) {
				case RequestMeta.KEY_TRACEPARENT -> meta.setTraceparent(value);
				case RequestMeta.KEY_TRACESTATE -> meta.setTracestate(value);
				case RequestMeta.KEY_BAGGAGE -> meta.setBaggage(value);
				default -> { }
			}
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link McpClient}.
	 *
	 * @since 10.0.0
	 */
	public static final class Builder extends AbstractMcpClient.Builder<Builder> {
		private ClientCapabilities clientCapabilities = new ClientCapabilities();
		private Implementation clientInfo;
		private Supplier<Map<String,String>> traceFieldsSupplier;
		private McpResponseCache responseCache;

		/**
		 * Sets the client capabilities stamped into every request's {@code params._meta.clientCapabilities}.
		 *
		 * @param value The client capabilities. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder clientCapabilities(ClientCapabilities value) {
			this.clientCapabilities = assertArgNotNull("clientCapabilities", value);
			return this;
		}

		/**
		 * Sets the client identity stamped into every request's {@code params._meta.clientInfo}.
		 *
		 * @param value The client identity. Can be <jk>null</jk> to omit {@code clientInfo} from every request.
		 * @return This object.
		 */
		public Builder clientInfo(Implementation value) {
			this.clientInfo = value;
			return this;
		}

		/**
		 * Sets the per-call supplier of W3C trace-context fields ({@code traceparent}/{@code tracestate}/
		 * {@code baggage}) stamped into every request's {@code params._meta}.
		 *
		 * @param value The supplier, invoked once per call. Can be <jk>null</jk> (no trace-context fields are
		 * 	stamped). A supplier that returns <jk>null</jk> for a given call is treated the same as returning an
		 * 	empty map for that call.
		 * @return This object.
		 */
		public Builder traceFieldsSupplier(Supplier<Map<String,String>> value) {
			this.traceFieldsSupplier = value;
			return this;
		}

		/**
		 * Sets the opt-in response cache used to serve repeat identical calls without a round trip.
		 *
		 * <p>
		 * A cached result instance may be handed back as-is (not re-marshaled) to more than one caller when the
		 * same cache key hits more than once; callers must not mutate a returned result if they cannot guarantee
		 * exclusive ownership of it.
		 *
		 * @param value The response cache, or <jk>null</jk> (the default) to disable caching entirely.
		 * @return This object.
		 */
		public Builder responseCache(McpResponseCache value) {
			this.responseCache = value;
			return this;
		}

		/**
		 * Builds the client.
		 *
		 * @return A new {@link McpClient}. Never <jk>null</jk>.
		 */
		public McpClient build() {
			return new McpClient(this);
		}
	}
}
