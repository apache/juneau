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
 * Revision-neutral resource descriptor returned from {@code resources/list}.
 *
 * <p>
 * Supersedes the wire-level {@code Resource} bean.
 */
public class McpResourceSpec {

	private String uri;
	private String name;
	private String title;
	private String description;
	private String mimeType;
	private Long size;

	/**
	 * The resource URI.
	 *
	 * @return The URI, or <jk>null</jk> if not set.
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * Sets the resource URI.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setUri(String value) {
		uri = value;
		return this;
	}

	/**
	 * The resource name.
	 *
	 * @return The name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the resource name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setName(String value) {
		name = value;
		return this;
	}

	/**
	 * The human-readable resource title.
	 *
	 * @return The title, or <jk>null</jk> if not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the resource title.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * The human-readable resource description.
	 *
	 * @return The description, or <jk>null</jk> if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the resource description.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * The resource's media type.
	 *
	 * @return The media type, or <jk>null</jk> if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the resource's media type.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setMimeType(String value) {
		mimeType = value;
		return this;
	}

	/**
	 * The resource's size in bytes.
	 *
	 * @return The size, or <jk>null</jk> if not set.
	 */
	public Long getSize() {
		return size;
	}

	/**
	 * Sets the resource's size in bytes.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceSpec setSize(Long value) {
		size = value;
		return this;
	}
}
