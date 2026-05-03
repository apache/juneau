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
package org.apache.juneau.bean.mcp;

import org.apache.juneau.annotation.*;

/**
 * Base64 blob body for {@code resources/read} results or embedded resource content.
 *
 * <p>
 * Discriminator {@code type} value is {@code resourceBlob} (distinct from generic {@code blob} tokens elsewhere).
 */
@Bean(typeName = "resourceBlob")
public class BlobResourceContents implements ResourceContents {

	private String uri;
	private String mimeType;
	private String blob;

	/**
	 * Resource URI.
	 *
	 * @return The URI, or {@code null} if not set.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the resource URI.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public BlobResourceContents setUri(String value) {
		uri = value;
		return this;
	}

	/**
	 * Optional MIME type.
	 *
	 * @return The MIME type, or {@code null} if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the MIME type.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public BlobResourceContents setMimeType(String value) {
		mimeType = value;
		return this;
	}

	/**
	 * Base64-encoded bytes.
	 *
	 * @return The blob, or {@code null} if not set.
	 */
	public String getBlob() {
		return blob;
	}

	/**
	 * Sets the base64 payload.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public BlobResourceContents setBlob(String value) {
		blob = value;
		return this;
	}
}
