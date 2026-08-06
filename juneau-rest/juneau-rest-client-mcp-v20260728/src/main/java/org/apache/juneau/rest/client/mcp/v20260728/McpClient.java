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
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.tracing.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.marshall.sse.*;
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
	private volatile ServerDiscoverResult discoveredServer;

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
	 * Builds a client for {@code endpoint} with default settings and performs its one mandatory handshake call,
	 * returning a ready-to-use client.
	 *
	 * <p>
	 * {@code 2026-07-28} has no {@code initialize} method - {@link #serverDiscover()} is this revision's
	 * handshake - so unlike {@code v20250618}'s {@code McpClient}, forgetting it is a real footgun: nothing in
	 * this class's type system forces a caller to call it before {@link #callTool}, {@link #listTools}, etc. This
	 * is the sanctioned one-expression path for the common case of connecting with default settings; use
	 * {@link #connect(Builder)} to customize the transport, interceptors, or other builder settings first.
	 *
	 * @param endpoint The absolute endpoint URL (e.g. {@code "http://localhost:8080/mcp"}). Must not be <jk>null</jk>.
	 * @return A new client that has already completed {@link #serverDiscover()} exactly once. Never <jk>null</jk>.
	 * @throws IOException If a transport-level or (de)serialization error occurs opening the connection.
	 * @throws McpException If the server returned a JSON-RPC error for {@value McpMethods#SERVER_DISCOVER}.
	 */
	public static McpClient connect(String endpoint) throws IOException {
		return connect(builder().endpoint(endpoint));
	}

	/**
	 * Builds a client from a caller-configured builder and performs its one mandatory handshake call, returning a
	 * ready-to-use client.
	 *
	 * <p>
	 * If {@link #serverDiscover()} fails, the just-built client is closed before the exception propagates - a
	 * caller that only ever sees an exception from {@code connect(...)} never ends up owning a client (and its
	 * underlying transport resources) it has no reference to.
	 *
	 * @param builder The configured builder to build from. Must not be <jk>null</jk>.
	 * @return A new client that has already completed {@link #serverDiscover()} exactly once. Never <jk>null</jk>.
	 * @throws IOException If a transport-level or (de)serialization error occurs opening the connection.
	 * @throws McpException If the server returned a JSON-RPC error for {@value McpMethods#SERVER_DISCOVER}.
	 */
	public static McpClient connect(Builder builder) throws IOException {
		assertArgNotNull("builder", builder);
		var client = builder.build();
		try {
			client.serverDiscover();
		} catch (Throwable e) {
			// Catches Throwable (not just IOException/RuntimeException): a client that only ever surfaces a
			// thrown Throwable from connect(...) must never end up leaking its transport resources just
			// because the handshake failed with something other than a checked/runtime exception (e.g. a
			// StackOverflowError decoding a pathological response). javac's "more precise rethrow" analysis
			// (JLS 11.2.2) sees that only IOException/RuntimeException/Error can reach this catch from the
			// try block, so rethrowing this effectively-final `e` still satisfies this method's declared
			// "throws IOException" without widening to "throws Throwable".
			closeQuietly(client, e);
			throw e;
		}
		return client;
	}

	/**
	 * Closes {@code client}, adding any close failure as a suppressed exception on {@code primary} rather than
	 * letting it mask the handshake failure that is the actual reason {@link #connect(Builder)} is failing.
	 */
	private static void closeQuietly(McpClient client, Throwable primary) {
		try {
			client.close();
		} catch (Throwable e) {
			// Catches Throwable, mirroring the outer catch in connect(Builder): a caller-supplied
			// HttpTransport.close() throwing an unchecked failure (RuntimeException/Error) must be recorded
			// as suppressed, not allowed to propagate from here and replace the handshake failure (primary)
			// this method exists to preserve.
			primary.addSuppressed(e);
		}
	}

	/**
	 * Sends {@value McpMethods#SERVER_DISCOVER}.
	 *
	 * <p>
	 * Caches its result, so a subsequent {@link #discoveredServer()} call (including one following the mandatory
	 * discovery {@link #connect(Builder)} already performs) does not need a repeat round trip.
	 *
	 * @return The server-discover result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public ServerDiscoverResult serverDiscover() throws IOException {
		var result = call(McpMethods.SERVER_DISCOVER, new RequestParamsOnly(), ServerDiscoverResult.class);
		discoveredServer = result;
		return result;
	}

	/**
	 * Returns the {@link ServerDiscoverResult} cached from the most recent {@link #serverDiscover()} call.
	 *
	 * <p>
	 * A client built via {@link #connect(String)}/{@link #connect(Builder)} already has this populated from the
	 * mandatory handshake call, so a caller that only needs the discovery info (server capabilities, supported
	 * versions, etc.) does not need to invoke {@link #serverDiscover()} again just to read it.
	 *
	 * @return The most recently discovered server info, or <jk>null</jk> if {@link #serverDiscover()} has not
	 * 	been called on this client, or the most recent call returned a <jk>null</jk> result.
	 */
	public ServerDiscoverResult discoveredServer() {
		return discoveredServer;
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
	 * Convenience for {@link #callTool} followed by {@link CallToolResult#firstText()}.
	 *
	 * <p>
	 * Kills the {@code ((TextContent)result.getContent().get(i)).getText()} cast-and-scan that a caller who
	 * only wants the tool's text result would otherwise repeat at every call site.
	 *
	 * @param name The tool name to invoke.
	 * @param arguments The tool arguments. Can be <jk>null</jk> (sent as an empty object).
	 * @return The first {@link TextContent} block's text found while scanning the result's content list in
	 * 	order, or <jk>null</jk> if the content list is empty or unset, it contains no {@link TextContent}
	 * 	block, or {@link #callTool} itself returned a <jk>null</jk> result.
	 * @throws IOException If a transport-level or (de)serialization error occurs.
	 * @throws McpException If the server returned a JSON-RPC error.
	 */
	public String callToolText(String name, Map<String,Object> arguments) throws IOException {
		var r = callTool(name, arguments);
		return r == null ? null : r.firstText();
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
	 * Performs a JSON-RPC call using a caller-supplied, fully-populated request bean, returning the raw result
	 * payload without deserializing into a specific {@link Result} subtype.
	 *
	 * <p>
	 * Every convenience method on this class ({@link #callTool}, {@link #getPrompt}, {@link #readResource}, etc.)
	 * both builds its own request bean internally and deserializes strictly into one known result type, so none of
	 * them can represent a paused {@code input_required} response (MCP {@code 2026-07-28} SEP-2322 Multi-Round-Trip
	 * Requests) or carry the {@code requestState}/{@code inputResponses} fields a resume call needs to set. This
	 * method exists specifically for MRTR-aware callers that must both populate those fields on the request (e.g.
	 * on a {@link CallToolRequest}) and branch on the response's
	 * {@code resultType} themselves, decoding into a typed result only once {@code resultType} reads
	 * {@code "complete"}.
	 *
	 * @param method The JSON-RPC method (e.g. {@link McpMethods#TOOLS_CALL}).
	 * @param params The fully-populated request bean.
	 * @return The raw result as a generic {@link Map}, exactly as received over the wire, before any
	 * 	subclass-specific deserialization. Never <jk>null</jk> on a successful JSON-RPC response.
	 * @throws IOException On a transport failure.
	 * @throws McpException If the server returns a JSON-RPC error.
	 */
	@SuppressWarnings({
		"unchecked" // Map.class.cast(...) below always yields a raw Map from JSON deserialization.
	})
	public Map<String,Object> callRaw(String method, RequestParams<?> params) throws IOException {
		return call(method, params, Map.class, false);
	}

	/**
	 * Default maximum number of {@code input_required} resume rounds the {@code *WithElicitation} helpers will
	 * drive before throwing {@link McpElicitationLimitException}.
	 */
	public static final int DEFAULT_MAX_ELICITATION_ROUNDS = 8;

	/**
	 * Invokes {@value McpMethods#TOOLS_CALL}, transparently answering any MCP {@code 2026-07-28} SEP-2322
	 * {@code input_required} elicitation pauses via {@code handler} until a terminal result is reached, using the
	 * {@link #DEFAULT_MAX_ELICITATION_ROUNDS default} max-rounds bound.
	 *
	 * <p>
	 * This is the ergonomic counterpart to hand-driving the resume loop with {@link #callRaw},
	 * {@link ElicitationRequests}, and {@link ElicitationResponses}: it detects each pause, decodes that round's
	 * requests, calls {@code handler} for the answers, echoes them back with the carried {@code requestState},
	 * and repeats until the server returns a non-{@code input_required} result, which is decoded into a typed
	 * {@link CallToolResult}. {@link #callRaw} remains available as the low-level escape hatch.
	 *
	 * @param name The tool name to invoke.
	 * @param arguments The tool arguments. Can be <jk>null</jk> (sent as an empty object).
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @return The terminal call-tool result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, or {@code handler} returns a
	 * 	<jk>null</jk> result or a result map containing a <jk>null</jk> value for a requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@link #DEFAULT_MAX_ELICITATION_ROUNDS}.
	 */
	public CallToolResult callToolWithElicitation(String name, Map<String,Object> arguments, McpElicitationHandler handler) throws IOException {
		return callToolWithElicitation(name, arguments, handler, DEFAULT_MAX_ELICITATION_ROUNDS);
	}

	/**
	 * Invokes {@value McpMethods#TOOLS_CALL}, transparently answering any {@code input_required} elicitation
	 * pauses via {@code handler} until a terminal result is reached, bounded by {@code maxRounds}.
	 *
	 * @param name The tool name to invoke.
	 * @param arguments The tool arguments. Can be <jk>null</jk> (sent as an empty object).
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @param maxRounds The maximum number of resume rounds before {@link McpElicitationLimitException} is thrown. Must be &ge; 1.
	 * @return The terminal call-tool result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, {@code maxRounds} is not &ge; 1, or
	 * 	{@code handler} returns a <jk>null</jk> result or a result map containing a <jk>null</jk> value for a
	 * 	requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@code maxRounds}.
	 */
	public CallToolResult callToolWithElicitation(String name, Map<String,Object> arguments, McpElicitationHandler handler, int maxRounds) throws IOException {
		var params = new CallToolRequest().setName(name).setArguments(arguments == null ? JsonMap.of() : new JsonMap(arguments));
		var raw = driveElicitation(McpMethods.TOOLS_CALL, params, (responses, state) -> params.setInputResponses(responses).setRequestState(state), handler, maxRounds);
		return decodeResult(raw, CallToolResult.class);
	}

	/**
	 * Invokes {@value McpMethods#PROMPTS_GET}, transparently answering any {@code input_required} elicitation
	 * pauses via {@code handler} until a terminal result is reached, using the
	 * {@link #DEFAULT_MAX_ELICITATION_ROUNDS default} max-rounds bound.
	 *
	 * @param name The prompt name to fetch.
	 * @param arguments The prompt argument values. Can be <jk>null</jk> (sent as an empty object).
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @return The terminal get-prompt result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, or {@code handler} returns a
	 * 	<jk>null</jk> result or a result map containing a <jk>null</jk> value for a requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@link #DEFAULT_MAX_ELICITATION_ROUNDS}.
	 */
	public GetPromptResult getPromptWithElicitation(String name, Map<String,Object> arguments, McpElicitationHandler handler) throws IOException {
		return getPromptWithElicitation(name, arguments, handler, DEFAULT_MAX_ELICITATION_ROUNDS);
	}

	/**
	 * Invokes {@value McpMethods#PROMPTS_GET}, transparently answering any {@code input_required} elicitation
	 * pauses via {@code handler} until a terminal result is reached, bounded by {@code maxRounds}.
	 *
	 * @param name The prompt name to fetch.
	 * @param arguments The prompt argument values. Can be <jk>null</jk> (sent as an empty object).
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @param maxRounds The maximum number of resume rounds before {@link McpElicitationLimitException} is thrown. Must be &ge; 1.
	 * @return The terminal get-prompt result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, {@code maxRounds} is not &ge; 1, or
	 * 	{@code handler} returns a <jk>null</jk> result or a result map containing a <jk>null</jk> value for a
	 * 	requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@code maxRounds}.
	 */
	public GetPromptResult getPromptWithElicitation(String name, Map<String,Object> arguments, McpElicitationHandler handler, int maxRounds) throws IOException {
		var params = new GetPromptRequest().setName(name).setArguments(arguments == null ? JsonMap.of() : new JsonMap(arguments));
		var raw = driveElicitation(McpMethods.PROMPTS_GET, params, (responses, state) -> params.setInputResponses(responses).setRequestState(state), handler, maxRounds);
		return decodeResult(raw, GetPromptResult.class);
	}

	/**
	 * Invokes {@value McpMethods#RESOURCES_READ}, transparently answering any {@code input_required} elicitation
	 * pauses via {@code handler} until a terminal result is reached, using the
	 * {@link #DEFAULT_MAX_ELICITATION_ROUNDS default} max-rounds bound.
	 *
	 * @param uri The resource URI to read.
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @return The terminal read-resource result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, or {@code handler} returns a
	 * 	<jk>null</jk> result or a result map containing a <jk>null</jk> value for a requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@link #DEFAULT_MAX_ELICITATION_ROUNDS}.
	 */
	public ReadResourceResult readResourceWithElicitation(String uri, McpElicitationHandler handler) throws IOException {
		return readResourceWithElicitation(uri, handler, DEFAULT_MAX_ELICITATION_ROUNDS);
	}

	/**
	 * Invokes {@value McpMethods#RESOURCES_READ}, transparently answering any {@code input_required} elicitation
	 * pauses via {@code handler} until a terminal result is reached, bounded by {@code maxRounds}.
	 *
	 * @param uri The resource URI to read.
	 * @param handler The elicitation answer callback, invoked once per pause. Must not be <jk>null</jk>.
	 * @param maxRounds The maximum number of resume rounds before {@link McpElicitationLimitException} is thrown. Must be &ge; 1.
	 * @return The terminal read-resource result. Never <jk>null</jk> on success unless the server returns a
	 * 	<jk>null</jk> result.
	 * @throws IllegalArgumentException If {@code handler} is <jk>null</jk>, {@code maxRounds} is not &ge; 1, or
	 * 	{@code handler} returns a <jk>null</jk> result or a result map containing a <jk>null</jk> value for a
	 * 	requested id.
	 * @throws IOException If a transport-level or (de)serialization error occurs, or {@code handler} throws it.
	 * @throws McpException If the server returned a JSON-RPC error.
	 * @throws McpElicitationLimitException If the server keeps pausing past {@code maxRounds}.
	 */
	public ReadResourceResult readResourceWithElicitation(String uri, McpElicitationHandler handler, int maxRounds) throws IOException {
		var params = new ReadResourceRequest().setUri(uri);
		var raw = driveElicitation(McpMethods.RESOURCES_READ, params, (responses, state) -> params.setInputResponses(responses).setRequestState(state), handler, maxRounds);
		return decodeResult(raw, ReadResourceResult.class);
	}

	/**
	 * Shared MRTR (SEP-2322) auto-resume loop backing every {@code *WithElicitation} method.
	 *
	 * <p>
	 * Issues the initial {@link #callRaw} for {@code params}, then while the raw result is an
	 * {@code input_required} pause: decodes the round's requests, invokes {@code handler} for the answers,
	 * applies them plus the pause's echoed {@code requestState} onto {@code params} via {@code applyResume} (each
	 * concrete request bean knows its own {@code setInputResponses}/{@code setRequestState}, which have no shared
	 * interface — see {@link ElicitationRequests}), and re-issues. A decline/cancel answer is not short-circuited
	 * locally: it is echoed back like any other answer, leaving the terminal outcome of a refused elicitation to
	 * the server. The loop is bounded by {@code maxRounds} so a server (or handler) that never converges surfaces
	 * as a typed {@link McpElicitationLimitException} rather than hanging.
	 */
	private Map<String,Object> driveElicitation(String method, RequestParams<?> params, ResumeApplier applyResume, McpElicitationHandler handler, int maxRounds) throws IOException {
		assertArgNotNull("handler", handler);
		assertArg(maxRounds >= 1, "maxRounds must be >= 1 (was %s).", maxRounds);
		var raw = callRaw(method, params);
		var rounds = 0;
		while (ElicitationRequests.isInputRequired(raw)) {
			if (++rounds > maxRounds)
				throw new McpElicitationLimitException(maxRounds);
			var requests = ElicitationRequests.requests(raw);
			var requestState = ElicitationRequests.requestState(raw);
			var answers = assertArgNotNull("handler result", handler.elicit(requests));
			applyResume.apply(ElicitationResponses.toInputResponses(answers), requestState);
			raw = callRaw(method, params);
		}
		return raw;
	}

	/**
	 * Decodes a terminal raw result {@link Map} into its typed result bean the same way {@link #call} decodes a
	 * live wire result, so a polymorphic field (e.g. a {@link CallToolResult}'s {@code content} entries) keeps
	 * the {@code type} discriminator it already carries in the raw tree.
	 */
	private static <T> T decodeResult(Map<String,Object> raw, Class<T> resultType) {
		return JsonMap.of("value", raw).get("value", resultType);
	}

	/**
	 * Applies one round's collected answers and carried continuation token onto the concrete resume request bean.
	 *
	 * <p>
	 * Exists (rather than a {@code BiConsumer}) so each {@code *WithElicitation} method can bind its own concrete
	 * bean's {@code setInputResponses}/{@code setRequestState} pair, which — unlike {@code RequestParams} — share
	 * no common interface across {@code CallToolRequest}/{@code GetPromptRequest}/{@code ReadResourceRequest}.
	 */
	@FunctionalInterface
	private interface ResumeApplier {
		void apply(Map<String,Object> inputResponses, String requestState);
	}

	/**
	 * Sends {@value McpMethods#SUBSCRIPTIONS_LISTEN}, opening a managed, held-open notification stream on a
	 * background thread and dispatching decoded frames to {@code listener} until the returned handle is closed,
	 * the server completes gracefully, or the connection drops.
	 *
	 * <p>
	 * Only the initial {@value McpMethods#SUBSCRIPTIONS_LISTEN} POST (opening the stream) is synchronous — a
	 * transport-level failure at that point surfaces here as a thrown {@link IOException}. Every frame after that
	 * (the mandatory {@code acknowledged} frame, zero or more change notifications, and the eventual terminal
	 * frame) is decoded on a dedicated background thread and delivered to {@code listener}; frame-processing
	 * failures surface via {@link McpSubscriptionListener#onError(Throwable)}, never as a thrown exception from
	 * this method. This does not reuse {@link #pumpNextServerMessage()} (single-shot, closes its stream after at
	 * most one event) or {@link McpDuplexDispatcher} (request/response-shaped, single registration) — {@code
	 * listen(...)} needs its own long-lived, multi-frame decode loop.
	 *
	 * @param filter The subscription filter (toolsListChanged/promptsListChanged/resourcesListChanged/resourceSubscriptions). Must not be <jk>null</jk>.
	 * @param listener The typed callback sink. Must not be <jk>null</jk>.
	 * @return A handle to cancel/close the subscription. Never <jk>null</jk>.
	 * @throws IOException If the initial listen request/stream-open fails.
	 */
	public McpSubscriptionHandle listen(SubscriptionFilter filter, McpSubscriptionListener listener) throws IOException {
		assertArgNotNull("filter", filter);
		assertArgNotNull("listener", listener);
		var params = new SubscriptionsListenRequest().setNotifications(filter);
		stampMeta(params.getMeta() == null ? params.setMeta(new RequestMeta()).getMeta() : params.getMeta());
		var wireParams = toWireParams(params);
		var req = new JsonRpcRequest()
			.setJsonrpc(McpProtocol.JSON_RPC_2_0)
			.setId(UUID.randomUUID().toString())
			.setMethod(McpMethods.SUBSCRIPTIONS_LISTEN)
			.setParams(wireParams);
		// SEP-2243 Mcp-Method/Mcp-Name headers, stamped exactly like every typed call(...) (see #call) - the v2
		// server's McpRevision.dispatch() runs validateHeaders(...) unconditionally before branching on the
		// method, so the opening subscriptions/listen POST is rejected with -32600 without these.
		var headers = Map.of("Mcp-Method", McpMethods.SUBSCRIPTIONS_LISTEN, "Mcp-Name", McpRoutingNames.routingName(McpMethods.SUBSCRIPTIONS_LISTEN, wireParams));
		var reader = openEventStream(req, headers);
		var pump = new SubscriptionPump(req.getId().toString(), reader, listener);
		try {
			pump.start();
		} catch (RuntimeException e) {
			// Thread.start() failing (e.g. IllegalThreadStateException, or a resource-exhaustion error from
			// the JVM/OS) must not leak the already-opened stream: nothing else owns it yet.
			pump.closeReaderQuietly();
			throw e;
		}
		return pump;
	}

	/**
	 * Background decode loop + closeable handle for one
	 * {@link #listen(SubscriptionFilter, McpSubscriptionListener)} subscription.
	 *
	 * <p>
	 * Every frame is decoded generically first (into a {@link JsonMap}) to tell a notification (has a
	 * {@code "method"} key) apart from the terminal JSON-RPC response (no {@code "method"} key) before committing
	 * to a strongly-typed re-parse — {@link JsonRpcRequest} and {@link JsonRpcResponse} share no common base type
	 * to parse into speculatively.
	 *
	 * <p>
	 * A named {@code "ping"} SSE event (the real transport's heartbeat, carrying no {@code data:} payload — see
	 * {@code SubscriptionsListenPublisher.HEARTBEAT_EVENT_NAME}) is recognized by event name and skipped
	 * <i>before</i> any attempt to inspect its (always <jk>null</jk>) data, so a heartbeat can never be mistaken
	 * for, or misreported as, a malformed JSON-RPC frame.
	 *
	 * <p>
	 * Package-private (rather than {@code private}) solely so tests in this package can observe the background
	 * thread's actual lifecycle (see {@code pumpThread}) — production code only ever sees this through the
	 * {@link McpSubscriptionHandle} interface {@link #listen} returns.
	 */
	static final class SubscriptionPump implements McpSubscriptionHandle, Runnable {
		private static final String EVENT_PING = "ping";
		private static final AtomicLong THREAD_SEQ = new AtomicLong();

		private final String id;
		private final SseEventReader reader;
		private final McpSubscriptionListener listener;
		final Thread pumpThread;
		private final AtomicBoolean closed = new AtomicBoolean(false);
		private volatile boolean open = true;

		SubscriptionPump(String id, SseEventReader reader, McpSubscriptionListener listener) {
			this.id = id;
			this.reader = reader;
			this.listener = listener;
			this.pumpThread = new Thread(this, "mcp-subscriptions-listen-" + THREAD_SEQ.incrementAndGet());
			this.pumpThread.setDaemon(true);
		}

		@Override
		public String id() {
			return id;
		}

		void start() {
			pumpThread.start();
		}

		@Override
		public void run() {
			var reachedTerminal = false;
			try {
				while (open && reader.hasNext()) {
					var event = reader.next();
					if (EVENT_PING.equals(event.getEvent()))
						continue;
					if (event.getData() == null || event.getData().isEmpty())
						continue;
					if (! dispatch(event.getData())) {
						reachedTerminal = true;
						break;
					}
				}
				// The stream ended (clean EOF, reader.hasNext() == false) without ever reaching a terminal
				// frame, and the caller did not initiate this close (open is still true): an abrupt drop the
				// listener must be told about, not silent - it is otherwise indistinguishable from a hang.
				//
				// open is flipped to false BEFORE invoking the listener (guarded on the pre-read value so a
				// concurrent cancel() can't cause a double-fire), not after in the finally block below: the
				// terminal callback is very often the exact synchronization point (a latch/future countdown,
				// or a direct isOpen() read from inside the callback itself) a caller relies on to learn the
				// pump is done, and CountDownLatch/Future's happens-before is one-directional (it orders the
				// signal before the waiter's wakeup, never the reverse) - so this volatile write must
				// happen-before that signal, not after it, or a woken caller can transiently observe open
				// still true.
				if (open && ! reachedTerminal) {
					open = false;
					invokeListener(() -> listener.onError(new EOFException(
						"Subscription stream closed before a terminal frame was received.")));
				}
			} catch (Exception e) {
				if (open) {
					open = false;
					invokeListener(() -> listener.onError(e));
				}
			} finally {
				// Idempotent safety net: open is already false on every path above that invoked a terminal
				// callback, and closeReaderQuietly() must run unconditionally regardless of which path (if
				// any) was taken - including a plain open cancel()/close() with no callback at all.
				open = false;
				closeReaderQuietly();
			}
		}

		/**
		 * Decodes one frame and invokes the matching listener callback.
		 *
		 * @return <jk>false</jk> if this was the terminal frame (the loop must stop); <jk>true</jk> to keep reading.
		 */
		private boolean dispatch(String data) {
			var tree = JsonParser.DEFAULT.read(data, JsonMap.class);
			if (tree.containsKey("method")) {
				dispatchNotification(JsonParser.DEFAULT.read(data, JsonRpcRequest.class));
				return true;
			}
			var res = JsonParser.DEFAULT.read(data, JsonRpcResponse.class);
			// open is flipped to false BEFORE invoking either terminal callback below, not after in run()'s
			// finally block - see the longer rationale on the abrupt-EOF branch in run().
			open = false;
			if (res.getError() != null) {
				var mcpException = McpException.fromJsonRpcError(res.getError());
				invokeListener(() -> listener.onError(mcpException));
			} else {
				invokeListener(listener::onComplete);
			}
			return false;
		}

		private void dispatchNotification(JsonRpcRequest notif) {
			var method = notif.getMethod();
			var rawParams = notif.getParams();
			if (McpMethods.NOTIFICATIONS_SUBSCRIPTIONS_ACKNOWLEDGED.equals(method)) {
				var decoded = Json.to(Json.of(rawParams), SubscriptionsAcknowledgedNotification.class);
				invokeListener(() -> listener.onAcknowledged(decoded.getNotifications()));
			} else if (McpMethods.NOTIFICATIONS_RESOURCES_UPDATED.equals(method)) {
				var decoded = Json.to(Json.of(rawParams), ResourceUpdatedNotification.class);
				invokeListener(() -> listener.onResourceUpdated(decoded.getUri()));
			} else if (McpMethods.NOTIFICATIONS_RESOURCES_LIST_CHANGED.equals(method)) {
				invokeListener(() -> listener.onListChanged(McpListChangedKind.RESOURCES));
			} else if (McpMethods.NOTIFICATIONS_TOOLS_LIST_CHANGED.equals(method)) {
				invokeListener(() -> listener.onListChanged(McpListChangedKind.TOOLS));
			} else if (McpMethods.NOTIFICATIONS_PROMPTS_LIST_CHANGED.equals(method)) {
				invokeListener(() -> listener.onListChanged(McpListChangedKind.PROMPTS));
			}
			// Unknown method: forward-compatible no-op, not an error - a future notification kind this
			// client version doesn't know about must not tear down an otherwise-healthy subscription.
		}

		/**
		 * Invokes one listener callback in isolation, so a bug in caller-supplied listener code can never be
		 * mistaken for (and re-routed through) a transport/decode failure, and can never itself crash the
		 * pump or escape this method - including a second throw from {@code onError} itself, which must not
		 * re-enter this same path.
		 *
		 * @param callback The single listener callback invocation to contain (e.g. {@code () -> listener.onComplete()}).
		 */
		private void invokeListener(Runnable callback) {
			try {
				callback.run();
			} catch (@SuppressWarnings("unused") RuntimeException e) {
				// Contained by design: this is the caller's own listener code, not this pump's transport/decode
				// path, so its exception must never surface as (or trigger) this pump's onError, and must never
				// tear down an otherwise-healthy subscription.
			}
		}

		@Override
		public void cancel() {
			if (closed.compareAndSet(false, true)) {
				open = false;
				closeReaderQuietly();
			}
		}

		@Override
		public void close() {
			cancel();
		}

		@Override
		public boolean isOpen() {
			return open;
		}

		private void closeReaderQuietly() {
			try {
				reader.close();
			} catch (@SuppressWarnings("unused") IOException e) {
				// Best-effort close: the stream may already be closed by the peer/EOF.
			}
		}
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

	// C5/C6 duplex return-channel header contract remains unsettled: Mcp-Name is always empty here because
	// DUPLEX_RETURN_METHOD has no routing-name mapping in McpRoutingNames. It is unclear whether the real
	// contract instead wants it to echo the correlated inbound request's tool/prompt/resource name; that
	// requires spec clarification and is deliberately left open rather than guessed at here.
	//
	// payload must be routed through toWireParams(...) exactly like every other outbound envelope (see #call):
	// AbstractMcpClient.send(...) posts through restClient, whose serializer is JsonSerializer.DEFAULT
	// (addBeanTypes=false) - handed the payload's typed beans directly, a polymorphic Content field (e.g.
	// CreateMessageResult.content holding a TextContent) would be serialized without its "type" discriminator,
	// since addBeanTypes is what drives whether SerializerSession writes bean-dictionary type names at all.
	// toWireParams(...) pre-flattens through REQUEST_SERIALIZER (addBeanTypes=true) into a generic Map/List
	// tree with the discriminator already baked in as literal data, so the final JsonSerializer.DEFAULT pass
	// re-emits it verbatim.
	private void postClientResult(Object id, JsonRpcResponse payload) throws IOException {
		var wire = toWireParams(payload);
		var headers = Map.of("Mcp-Method", DUPLEX_RETURN_METHOD, "Mcp-Name", McpRoutingNames.routingName(DUPLEX_RETURN_METHOD, wire));
		send(new JsonRpcRequest().setJsonrpc("2.0").setId(id).setMethod(DUPLEX_RETURN_METHOD).setParams(wire), headers);
	}

	private <T> T call(String method, RequestParams<?> params, Class<T> resultType) throws IOException {
		return call(method, params, resultType, true);
	}

	/**
	 * Shared wire implementation for every typed method and {@link #callRaw}.
	 *
	 * <p>
	 * {@code useCache=false} (used only by {@link #callRaw}) skips the cache READ so a {@code callRaw} call never
	 * retrieves a differently-typed cached result (e.g. a {@link CallToolResult} primed by {@link #callTool}) under
	 * the same cache key, which would otherwise throw a {@link ClassCastException} on the subsequent {@code cast}.
	 * The cache WRITE at the end is unaffected by this flag: it is already a no-op for {@code callRaw} because a
	 * plain {@link Map} result is never a {@link CacheableResult}.
	 */
	private <T> T call(String method, RequestParams<?> params, Class<T> resultType, boolean useCache) throws IOException {
		var cacheKey = cacheKey(method, params);
		if (useCache) {
			var cached = readCache(cacheKey);
			if (cached != null)
				return resultType.cast(cached);
		}
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
				default -> { /* Unknown trace-context key: no corresponding RequestMeta field to set. */ }
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
