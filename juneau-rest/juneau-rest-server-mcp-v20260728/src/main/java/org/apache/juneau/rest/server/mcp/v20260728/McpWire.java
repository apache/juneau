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
package org.apache.juneau.rest.server.mcp.v20260728;

import java.net.*;
import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.jsonschema.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpCompletionResult;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpPromptArgument;
import org.apache.juneau.rest.server.mcp.McpPromptMessage;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceContents;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpResourceTemplateSpec;
import org.apache.juneau.rest.server.mcp.McpRole;
import org.apache.juneau.rest.server.mcp.McpSchema;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;

/**
 * Mapping between the revision-neutral model and the {@code 2026-07-28} wire beans.
 *
 * <p>
 * This revision maps in one direction only: neutral-to-wire. What a neutral handler returns becomes
 * what a client receives. Unlike the {@code 2025-06-18} adapter, this revision defines no
 * typed-handler API, so no wire-to-neutral adaptation is needed.
 *
 * <p>
 * {@link #requestMeta(Object)} is the single opaque-to-typed conversion: every v2 request carries its
 * negotiation/trace metadata nested under {@code params._meta} (never beside the JSON-RPC envelope), and this
 * class parses that opaque {@code _meta} member into the v2 {@link RequestMeta} bean at the boundary.
 *
 * <p>
 * <b>{@code null} is preserved as {@code null} throughout.</b> Juneau omits null bean properties, so
 * normalizing a null collection to an empty one would add a property to the wire output. An empty
 * {@code CallToolResult} must keep serializing as <c>{}</c>.
 */
final class McpWire {

	private McpWire() {}

	// --- opaque params._meta -> typed ------------------------------------------------------

	/**
	 * Parses the {@code _meta} member nested under an opaque {@code params} object into a typed
	 * {@link RequestMeta}.
	 *
	 * <p>
	 * Requires an object {@code params} containing an object {@code _meta}; the {@code 2026-07-28} schema puts
	 * every request's negotiation/trace metadata under {@code params._meta}, never beside the JSON-RPC
	 * envelope.
	 *
	 * @param params The opaque request params (the JSON-RPC {@code params} member). Can be <jk>null</jk>.
	 * @return The parsed metadata. Never <jk>null</jk>.
	 * @throws McpException If {@code params} or {@code params._meta} is absent or not an object.
	 */
	static RequestMeta requestMeta(Object params) {
		if (! (params instanceof Map<?,?> p))
			throw new McpException(McpRevision.CODE_INVALID_REQUEST, "Request params must be an object");
		var meta = p.get("_meta");
		if (! (meta instanceof Map<?,?>))
			throw new McpException(McpRevision.CODE_INVALID_REQUEST, "Request params._meta must be an object");
		return Json.to(Json.of(meta), RequestMeta.class);
	}

	/**
	 * Returns the opaque {@code params._meta} member as a raw, string-keyed map, or an empty map if
	 * {@code params} or {@code params._meta} is absent or malformed.
	 *
	 * <p>
	 * Unlike {@link #requestMeta(Object)}, this never throws. A {@code TraceContextExtractor} runs
	 * before {@link McpRevision#validateMeta(Object)} enforces the per-request {@code _meta} contract
	 * (extraction happens once per operation invocation, valid or not), so it needs a defensive read
	 * rather than one that raises {@link McpException} on a shape violation.
	 *
	 * @param params The opaque request params (the JSON-RPC {@code params} member). Can be <jk>null</jk>.
	 * @return An unmodifiable, insertion-ordered map keyed by {@code String}. Never <jk>null</jk>;
	 * 	empty when {@code params._meta} is absent or not an object.
	 */
	static Map<String,Object> metaMapOrEmpty(Object params) {
		if (params instanceof Map<?,?> p && p.get("_meta") instanceof Map<?,?> m) {
			var result = new LinkedHashMap<String,Object>();
			m.forEach((k, v) -> result.put(String.valueOf(k), v));
			return Collections.unmodifiableMap(result);
		}
		return Collections.emptyMap();
	}

	// --- neutral -> wire -------------------------------------------------------------------

	static Tool toWire(McpToolSpec value) {
		if (value == null)
			return null;
		return new Tool()
			.setName(value.getName())
			.setDescription(value.getDescription())
			.setInputSchema(toWire(value.getInputSchema()))
			.setOutputSchema(toWire(value.getOutputSchema()));
	}

	@SuppressWarnings({
		"java:S1452" // Intentional: WireJsonSchema is a private CRTP-typed inner class; JsonSchema<?> is the only return type that can expose the shared bean type without leaking the internal subtype.
	})
	static JsonSchema<?> toWire(McpSchema value) {
		return value == null ? null : Json.to(Json.of(value.toJsonMap()), WireJsonSchema.class);
	}

	/**
	 * {@link JsonSchema} subclass used only for neutral-to-wire schema conversion.
	 *
	 * <p>
	 * {@link JsonSchema#getDefinitions()} and {@link JsonSchema#getId()} are legacy Draft-04 getters
	 * retained on the shared bean for backward compatibility; each falls back to reading the Draft
	 * 2020-12 {@code $defs}/{@code $id} field when its own field is unset "to avoid double
	 * serialization" in the single-field-set case the shared bean's authors anticipated. But because
	 * both legacy and current getters are still independently non-null once only {@code $defs}/{@code
	 * $id} is populated, plain bean introspection serializes both names. This adapter only ever
	 * populates the Draft 2020-12 fields (never the legacy ones — see the five keys asserted by
	 * {@code McpSchemaSafety}), so the legacy getters are pinned to {@code null} here to keep the wire
	 * output to the Draft 2020-12 property names alone.
	 */
	static final class WireJsonSchema extends JsonSchema<WireJsonSchema> {
		@SuppressWarnings({
			"java:S1168" // null (not {}) is required here: this pins the legacy Draft-04 getter to absent so Juneau's null-omitting serializer never emits "definitions" alongside the Draft 2020-12 "$defs" this adapter actually populates (see class Javadoc).
		})
		@Override public Map<String,JsonSchema<?>> getDefinitions() { return null; }

		@SuppressWarnings({
			"java:S5738" // Intentional legacy-schema mapping: pins the deprecated-for-removal Draft-04 getter to absent for the same reason as getDefinitions() above (see class Javadoc).
		})
		@Override
		public URI getId() { return null; }
	}

	static CallToolResult toWire(McpToolOutcome value) {
		if (value == null)
			return null;
		var result = new CallToolResult().setIsError(value.getError()).setStructuredContent(value.getStructuredContent());
		if (value.getContent() != null)
			result.setContent(value.getContent().stream().map(McpWire::toWire).toList());
		return result;
	}

	static Content toWire(McpContentBlock x) {
		if (x == null)
			return null;
		return switch (x.kind()) {
			case TEXT -> new TextContent().setText(x.text());
			case IMAGE -> new ImageContent().setData(x.data()).setMimeType(x.mimeType());
			case RESOURCE -> new EmbeddedResourceContent().setResource(toWire(x.resource()));
		};
	}

	static ResourceContents toWire(McpResourceContents x) {
		if (x == null)
			return null;
		return switch (x.kind()) {
			case TEXT -> new TextResourceContents().setUri(x.uri()).setMimeType(x.mimeType()).setText(x.text());
			case BLOB -> new BlobResourceContents().setUri(x.uri()).setMimeType(x.mimeType()).setBlob(x.blob());
		};
	}

	static Prompt toWire(McpPromptSpec x) {
		if (x == null)
			return null;
		var r = new Prompt().setName(x.getName()).setDescription(x.getDescription());
		if (x.getArguments() != null)
			r.setArguments(x.getArguments().stream().map(McpWire::toWire).toList());
		return r;
	}

	static PromptArgument toWire(McpPromptArgument x) {
		if (x == null)
			return null;
		return new PromptArgument().setName(x.getName()).setDescription(x.getDescription()).setRequired(x.getRequired());
	}

	static GetPromptResult toWire(McpPromptOutcome x) {
		if (x == null)
			return null;
		var r = new GetPromptResult().setDescription(x.getDescription());
		if (x.getMessages() != null)
			r.setMessages(x.getMessages().stream().map(McpWire::toWire).toList());
		return r;
	}

	static PromptMessage toWire(McpPromptMessage x) {
		if (x == null)
			return null;
		return new PromptMessage().setRole(toWire(x.getRole())).setContent(toWire(x.getContent()));
	}

	static Role toWire(McpRole x) {
		if (x == null)
			return null;
		return switch (x) {
			case USER -> Role.USER;
			case ASSISTANT -> Role.ASSISTANT;
			case SYSTEM -> Role.SYSTEM;
			case TOOL -> Role.TOOL;
		};
	}

	static Resource toWire(McpResourceSpec x) {
		if (x == null)
			return null;
		return new Resource()
			.setUri(x.getUri())
			.setName(x.getName())
			.setTitle(x.getTitle())
			.setDescription(x.getDescription())
			.setMimeType(x.getMimeType())
			.setSize(x.getSize());
	}

	static ReadResourceResult toWire(McpResourceOutcome x) {
		if (x == null)
			return null;
		var r = new ReadResourceResult();
		if (x.getContents() != null)
			r.setContents(x.getContents().stream().map(McpWire::toWire).toList());
		return r;
	}

	static ResourceTemplate toWire(McpResourceTemplateSpec value) {
		if (value == null)
			return null;
		return new ResourceTemplate()
			.setUriTemplate(value.getUriTemplate())
			.setName(value.getName())
			.setTitle(value.getTitle())
			.setDescription(value.getDescription())
			.setMimeType(value.getMimeType());
	}

	static McpResourceTemplateSpec toNeutral(ResourceTemplate value) {
		if (value == null)
			return null;
		return new McpResourceTemplateSpec()
			.setUriTemplate(value.getUriTemplate())
			.setName(value.getName())
			.setTitle(value.getTitle())
			.setDescription(value.getDescription())
			.setMimeType(value.getMimeType());
	}

	static CompleteResult toWire(McpCompletionResult.Normalized x) {
		return new CompleteResult().setCompletion(new Completion()
			.setValues(x.values())
			.setTotal(x.total())
			.setHasMore(x.hasMore()));
	}

	static Implementation serverInfo(McpServerConfig config) {
		if (config.getName() == null && config.getVersion() == null)
			return new Implementation()
				.setName(McpRevision.DEFAULT_SERVER_NAME)
				.setVersion("unknown");
		return new Implementation().setName(config.getName()).setVersion(config.getVersion());
	}

	/**
	 * Builds the {@code server/discover} result.
	 *
	 * <p>
	 * Server identity is <b>not</b> set here: every successful result (including this one) receives its
	 * {@link ResultMeta#getServerInfo() serverInfo} uniformly from {@link McpRevision}'s common result
	 * finalization, so discovery does not special-case identity placement.
	 *
	 * @param capabilities The top-level capability advertisement. Can be <jk>null</jk>.
	 * @param protocolVersion The single supported protocol version to advertise. Must not be <jk>null</jk>.
	 * @param instructions Optional free-form usage instructions. Can be <jk>null</jk>.
	 * @return A new discovery result. Never <jk>null</jk>.
	 */
	static ServerDiscoverResult discover(ServerCapabilities capabilities, String protocolVersion, String instructions) {
		return new ServerDiscoverResult()
			.setSupportedVersions(protocolVersion)
			.setCapabilities(capabilities)
			.setInstructions(instructions);
	}
}
