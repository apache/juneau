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
 * Revision-neutral resource payload, either inline text or a base64 blob.
 *
 * <p>
 * Supersedes the wire-level {@code ResourceContents} dictionary and its two leaf types. Used both
 * by {@link McpContentBlock#resource(McpResourceContents)} and by {@link McpResourceOutcome}.
 */
public final class McpResourceContents {

	/** Which payload variant is populated. */
	public enum Kind {
		/** {@link McpResourceContents#text()} is populated. */
		TEXT,
		/** {@link McpResourceContents#blob()} is populated. */
		BLOB
	}

	private final Kind kind;
	private final String uri;
	private final String mimeType;
	private final String text;
	private final String blob;

	private McpResourceContents(Kind kind, String uri, String mimeType, String text, String blob) {
		this.kind = kind;
		this.uri = uri;
		this.mimeType = mimeType;
		this.text = text;
		this.blob = blob;
	}

	/**
	 * Creates an inline-text resource payload.
	 *
	 * @param uri The resource URI. Can be <jk>null</jk>.
	 * @param mimeType The media type. Can be <jk>null</jk>.
	 * @param text The text payload. Can be <jk>null</jk>.
	 * @return A new payload. Never <jk>null</jk>.
	 */
	public static McpResourceContents text(String uri, String mimeType, String text) {
		return new McpResourceContents(Kind.TEXT, uri, mimeType, text, null);
	}

	/**
	 * Creates a base64-blob resource payload.
	 *
	 * @param uri The resource URI. Can be <jk>null</jk>.
	 * @param mimeType The media type. Can be <jk>null</jk>.
	 * @param blob The base64-encoded payload. Can be <jk>null</jk>.
	 * @return A new payload. Never <jk>null</jk>.
	 */
	public static McpResourceContents blob(String uri, String mimeType, String blob) {
		return new McpResourceContents(Kind.BLOB, uri, mimeType, null, blob);
	}

	/**
	 * Which variant this payload carries.
	 *
	 * @return The variant. Never <jk>null</jk>.
	 */
	public Kind kind() {
		return kind;
	}

	/**
	 * The resource URI.
	 *
	 * @return The URI, or <jk>null</jk> if not set.
	 */
	public String uri() {
		return uri;
	}

	/**
	 * The media type.
	 *
	 * @return The media type, or <jk>null</jk> if not set.
	 */
	public String mimeType() {
		return mimeType;
	}

	/**
	 * The inline text payload.
	 *
	 * @return The text, or <jk>null</jk> for a {@link Kind#BLOB} payload.
	 */
	public String text() {
		return text;
	}

	/**
	 * The base64-encoded payload.
	 *
	 * @return The blob, or <jk>null</jk> for a {@link Kind#TEXT} payload.
	 */
	public String blob() {
		return blob;
	}
}
