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
package org.apache.juneau.rest.server.mcp;

/**
 * Revision-neutral content block returned by tool calls and prompt messages.
 *
 * <p>
 * Carries all three variants supported today — text, image, and embedded resource — so a handler
 * cannot express less through the neutral model than it can through a wire bean. Supersedes the
 * wire-level {@code Content} dictionary and its three leaf types.
 */
public final class McpContentBlock {

	/** Which content variant is populated. */
	public enum Kind {
		/** {@link McpContentBlock#text()} is populated. */
		TEXT,
		/** {@link McpContentBlock#data()} and {@link McpContentBlock#mimeType()} are populated. */
		IMAGE,
		/** {@link McpContentBlock#resource()} is populated. */
		RESOURCE
	}

	private final Kind kind;
	private final String text;
	private final String data;
	private final String mimeType;
	private final McpResourceContents resource;

	private McpContentBlock(Kind kind, String text, String data, String mimeType, McpResourceContents resource) {
		this.kind = kind;
		this.text = text;
		this.data = data;
		this.mimeType = mimeType;
		this.resource = resource;
	}

	/**
	 * Creates a text content block.
	 *
	 * @param text The text. Can be <jk>null</jk>.
	 * @return A new block. Never <jk>null</jk>.
	 */
	public static McpContentBlock text(String text) {
		return new McpContentBlock(Kind.TEXT, text, null, null, null);
	}

	/**
	 * Creates an image content block.
	 *
	 * @param data Base64-encoded image bytes. Can be <jk>null</jk>.
	 * @param mimeType The image media type. Can be <jk>null</jk>.
	 * @return A new block. Never <jk>null</jk>.
	 */
	public static McpContentBlock image(String data, String mimeType) {
		return new McpContentBlock(Kind.IMAGE, null, data, mimeType, null);
	}

	/**
	 * Creates an embedded-resource content block.
	 *
	 * @param resource The embedded payload. Can be <jk>null</jk>.
	 * @return A new block. Never <jk>null</jk>.
	 */
	public static McpContentBlock resource(McpResourceContents resource) {
		return new McpContentBlock(Kind.RESOURCE, null, null, null, resource);
	}

	/**
	 * Which variant this block carries.
	 *
	 * @return The variant. Never <jk>null</jk>.
	 */
	public Kind kind() {
		return kind;
	}

	/**
	 * The text payload.
	 *
	 * @return The text, or <jk>null</jk> unless this is a {@link Kind#TEXT} block.
	 */
	public String text() {
		return text;
	}

	/**
	 * The base64 image payload.
	 *
	 * @return The data, or <jk>null</jk> unless this is a {@link Kind#IMAGE} block.
	 */
	public String data() {
		return data;
	}

	/**
	 * The image media type.
	 *
	 * @return The media type, or <jk>null</jk> unless this is a {@link Kind#IMAGE} block.
	 */
	public String mimeType() {
		return mimeType;
	}

	/**
	 * The embedded resource payload.
	 *
	 * @return The payload, or <jk>null</jk> unless this is a {@link Kind#RESOURCE} block.
	 */
	public McpResourceContents resource() {
		return resource;
	}
}
