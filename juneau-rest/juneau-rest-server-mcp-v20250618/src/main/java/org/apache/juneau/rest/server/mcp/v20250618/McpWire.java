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

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20250618.*;
import org.apache.juneau.marshall.collections.*;
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
 * Mapping between the revision-neutral model and the {@code 2025-06-18} wire beans.
 *
 * <p>
 * The mapping runs in both directions. Neutral-to-wire is the request path: what a handler returns
 * becomes what a client receives. Wire-to-neutral is needed because {@link McpTypedHandlers} accepts
 * user handlers written against this revision's wire beans and must adapt them to the neutral
 * handler interfaces the core registry holds.
 *
 * <p>
 * <b>{@code null} is preserved as {@code null} throughout.</b> Juneau omits null bean properties, so
 * normalizing a null collection to an empty one would add a property to the wire output. An empty
 * {@code CallToolResult} must keep serializing as <c>{}</c>.
 */
final class McpWire {

	private McpWire() {}

	// --- neutral -> wire -------------------------------------------------------------------

	static Tool toWire(McpToolSpec x) {
		if (x == null)
			return null;
		return new Tool()
			.setName(x.getName())
			.setDescription(x.getDescription())
			.setInputSchema(toWire(x.getInputSchema()))
			.setOutputSchema(toWire(x.getOutputSchema()));
	}

	static JsonSchema toWire(McpSchema x) {
		return x == null ? null : Json.to(Json.of(x.toJsonMap()), JsonSchema.class);
	}

	static CallToolResult toWire(McpToolOutcome x) {
		if (x == null)
			return null;
		var r = new CallToolResult().setIsError(x.getError()).setStructuredContent(x.getStructuredContent());
		if (x.getContent() != null)
			r.setContent(x.getContent().stream().map(McpWire::toWire).toList());
		return r;
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

	static ResourceTemplate toWire(McpResourceTemplateSpec x) {
		if (x == null)
			return null;
		return new ResourceTemplate()
			.setUriTemplate(x.getUriTemplate())
			.setName(x.getName())
			.setTitle(x.getTitle())
			.setDescription(x.getDescription())
			.setMimeType(x.getMimeType());
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

	// --- wire -> neutral -------------------------------------------------------------------

	static McpToolSpec toNeutral(Tool x) {
		if (x == null)
			return null;
		return new McpToolSpec()
			.setName(x.getName())
			.setDescription(x.getDescription())
			.setInputSchema(toNeutral(x.getInputSchema()))
			.setOutputSchema(toNeutral(x.getOutputSchema()));
	}

	static McpSchema toNeutral(JsonSchema x) {
		return x == null ? null : McpSchema.of(Json.to(Json.of(x), JsonMap.class));
	}

	static McpToolOutcome toNeutral(CallToolResult x) {
		if (x == null)
			return null;
		var r = new McpToolOutcome().setError(x.getIsError()).setStructuredContent(x.getStructuredContent());
		if (x.getContent() != null)
			r.setContent(x.getContent().stream().map(McpWire::toNeutral).toList());
		return r;
	}

	static McpContentBlock toNeutral(Content x) {
		if (x == null)
			return null;
		if (x instanceof TextContent x2)
			return McpContentBlock.text(x2.getText());
		if (x instanceof ImageContent x2)
			return McpContentBlock.image(x2.getData(), x2.getMimeType());
		if (x instanceof EmbeddedResourceContent x2)
			return McpContentBlock.resource(toNeutral(x2.getResource()));
		throw new McpException(McpRevision.CODE_INTERNAL_ERROR, "Unsupported content type: " + x.getClass().getName()); // HTT: the Content dictionary is closed to Text/Image/EmbeddedResource; a 4th implementation cannot occur through the public API.
	}

	static McpResourceContents toNeutral(ResourceContents x) {
		if (x == null)
			return null;
		if (x instanceof TextResourceContents x2)
			return McpResourceContents.text(x2.getUri(), x2.getMimeType(), x2.getText());
		if (x instanceof BlobResourceContents x2)
			return McpResourceContents.blob(x2.getUri(), x2.getMimeType(), x2.getBlob());
		throw new McpException(McpRevision.CODE_INTERNAL_ERROR, "Unsupported resource contents type: " + x.getClass().getName()); // HTT: the ResourceContents dictionary is closed to Text/Blob; a 3rd implementation cannot occur through the public API.
	}

	static McpResourceTemplateSpec toNeutral(ResourceTemplate x) {
		if (x == null)
			return null;
		return new McpResourceTemplateSpec()
			.setUriTemplate(x.getUriTemplate())
			.setName(x.getName())
			.setTitle(x.getTitle())
			.setDescription(x.getDescription())
			.setMimeType(x.getMimeType());
	}

	static McpPromptSpec toNeutral(Prompt x) {
		if (x == null)
			return null;
		var r = new McpPromptSpec().setName(x.getName()).setDescription(x.getDescription());
		if (x.getArguments() != null)
			r.setArguments(x.getArguments().stream().map(McpWire::toNeutral).toList());
		return r;
	}

	static McpPromptArgument toNeutral(PromptArgument x) {
		if (x == null)
			return null;
		return new McpPromptArgument().setName(x.getName()).setDescription(x.getDescription()).setRequired(x.getRequired());
	}

	static McpPromptOutcome toNeutral(GetPromptResult x) {
		if (x == null)
			return null;
		var r = new McpPromptOutcome().setDescription(x.getDescription());
		if (x.getMessages() != null)
			r.setMessages(x.getMessages().stream().map(McpWire::toNeutral).toList());
		return r;
	}

	static McpPromptMessage toNeutral(PromptMessage x) {
		if (x == null)
			return null;
		return new McpPromptMessage().setRole(toNeutral(x.getRole())).setContent(toNeutral(x.getContent()));
	}

	static McpRole toNeutral(Role x) {
		if (x == null)
			return null;
		return switch (x) {
			case USER -> McpRole.USER;
			case ASSISTANT -> McpRole.ASSISTANT;
			case SYSTEM -> McpRole.SYSTEM;
			case TOOL -> McpRole.TOOL;
		};
	}
}
