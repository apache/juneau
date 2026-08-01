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
package org.apache.juneau.rest.server.tracing;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;

/**
 * Immutable, low-cardinality description of the operation a {@code TracerHook} bridge should name a
 * span after, supplied to {@link TracerHook#startSpan(org.apache.juneau.rest.server.RestRequest, TraceContextCarrier, TraceOperation)}
 * alongside the extracted {@link TraceContextCarrier}.
 *
 * <p>
 * {@code juneau-rest-server} has no notion of MCP, JSON-RPC, or OpenTelemetry &mdash; this type exists
 * so a neutral {@code TraceContextExtractor} (for example the {@code juneau-rest-server-mcp-v20260728}
 * binding, added in a later task) can hand a bridge (for example the OpenTelemetry bridge in
 * {@code juneau-rest-server-tracing-otel}) a low-cardinality span name plus a small set of attributes
 * derived from resolved request arguments, without either side depending on the other's types.
 *
 * <h5 class='topic'>Default operation</h5>
 *
 * <p>
 * {@link #DEFAULT} carries no span name and no attributes. It is the value
 * {@link TraceContextExtractor#operation} returns when an extractor implementation supplies only
 * {@link TraceContextExtractor#extract extract(...)} &mdash; a bridge receiving {@link #DEFAULT} keeps
 * whatever span name / attributes it would otherwise derive from the {@link org.apache.juneau.rest.server.RestRequest}
 * itself (HTTP method, route, etc.); it is not required to name every span after a non-HTTP operation.
 *
 * <h5 class='topic'>Attribute-name constants</h5>
 *
 * <p>
 * The attribute-name constants declared here are the exact, pinned MCP/GenAI span-attribute keys that
 * a later OpenTelemetry-bridge task and MCP-binding task both need to agree on. They are declared on
 * this neutral type &mdash; rather than duplicated as private literals in each of those two modules
 * &mdash; specifically so {@code juneau-rest-server} stays free of any OpenTelemetry or MCP import
 * while still being the single source of truth for the wire-level attribute name. Attribute
 * <i>values</i> are supplied per-operation via {@link #getAttributes()}; these constants are only the
 * keys.
 *
 * <p>
 * Pinned to the OpenTelemetry GenAI semantic-conventions snapshot at commit
 * {@code c739977ae690961f36e435504e5c1febaef1f7f3} plus core OpenTelemetry semantic conventions
 * v1.43.0. All of these attributes are <b>experimental/development</b> &mdash; names and requirement
 * levels may change in a future semantic-conventions release; this pin must be re-checked before any
 * OpenTelemetry or semantic-conventions dependency upgrade touches MCP tracing.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TraceContextCarrier}
 * 	<li class='jc'>{@link TraceContextExtractor}
 * 	<li class='jc'>{@link TracerHook}
 * 	<li class='link'><a class="doclink" href="https://github.com/open-telemetry/semantic-conventions-genai/tree/c739977ae690961f36e435504e5c1febaef1f7f3">OpenTelemetry GenAI semantic conventions (pinned snapshot)</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public final class TraceOperation {

	/** Experimental span-attribute key: the exact MCP method name (e.g. {@code "tools/call"}). */
	public static final String ATTR_MCP_METHOD_NAME = "mcp.method.name";

	/** Experimental span-attribute key: the negotiated MCP protocol version (e.g. {@code "2026-07-28"}). */
	public static final String ATTR_MCP_PROTOCOL_VERSION = "mcp.protocol.version";

	/** Experimental span-attribute key: the JSON-RPC request id, in string form. */
	public static final String ATTR_JSONRPC_REQUEST_ID = "jsonrpc.request.id";

	/** Experimental span-attribute key: the {@code tools/call} target tool name. */
	public static final String ATTR_GEN_AI_TOOL_NAME = "gen_ai.tool.name";

	/** Experimental span-attribute key: the {@code prompts/get} target prompt name. */
	public static final String ATTR_GEN_AI_PROMPT_NAME = "gen_ai.prompt.name";

	/** Experimental span-attribute key: the {@code resources/read} target resource URI. */
	public static final String ATTR_MCP_RESOURCE_URI = "mcp.resource.uri";

	/** Experimental span-attribute key: the GenAI operation name (e.g. {@code "execute_tool"}). */
	public static final String ATTR_GEN_AI_OPERATION_NAME = "gen_ai.operation.name";

	/** Experimental span-attribute key: the JSON-RPC error response's numeric code, in string form. */
	public static final String ATTR_RPC_RESPONSE_STATUS_CODE = "rpc.response.status_code";

	/** Experimental span-attribute key: the JSON-RPC error category or handler exception type. */
	public static final String ATTR_ERROR_TYPE = "error.type";

	/**
	 * The default operation: no span name override, no attributes.
	 *
	 * <p>
	 * Returned by {@link TraceContextExtractor#operation} when an extractor supplies no override.
	 */
	public static final TraceOperation DEFAULT = new TraceOperation(null, Collections.emptyMap());

	private final String spanName;
	private final Map<String,String> attributes;

	private TraceOperation(String spanName, Map<String,String> attributes) {
		this.spanName = spanName;
		this.attributes = attributes;
	}

	/**
	 * Creates an operation with a span name and no attributes.
	 *
	 * @param spanName The low-cardinality span name (e.g. {@code "tools/call echo"}). Must not be
	 * 	<jk>null</jk> or blank.
	 * @return A new {@link TraceOperation}. Never <jk>null</jk>.
	 */
	public static TraceOperation of(String spanName) {
		return of(spanName, Collections.emptyMap());
	}

	/**
	 * Creates an operation with a span name and attributes.
	 *
	 * @param spanName The low-cardinality span name (e.g. {@code "tools/call echo"}). Must not be
	 * 	<jk>null</jk> or blank.
	 * @param attributes The span attributes, keyed by attribute name (typically one of the
	 * 	{@code ATTR_*} constants declared on this class). Iteration order is preserved. Must not be
	 * 	<jk>null</jk>; may be empty.
	 * @return A new {@link TraceOperation}. Never <jk>null</jk>.
	 */
	public static TraceOperation of(String spanName, Map<String,String> attributes) {
		assertArgNotNullOrBlank("spanName", spanName);
		assertArgNotNull("attributes", attributes);
		return new TraceOperation(spanName, Collections.unmodifiableMap(new LinkedHashMap<>(attributes)));
	}

	/**
	 * Returns the low-cardinality span name.
	 *
	 * @return The span name, or <jk>null</jk> for {@link #DEFAULT} (no override &mdash; the bridge
	 * 	keeps its own default naming).
	 */
	public String getSpanName() {
		return spanName;
	}

	/**
	 * Returns the span attributes.
	 *
	 * @return An unmodifiable, insertion-ordered map of span attributes. Never <jk>null</jk>; empty
	 * 	for {@link #DEFAULT}.
	 */
	public Map<String,String> getAttributes() {
		return attributes;
	}
}
