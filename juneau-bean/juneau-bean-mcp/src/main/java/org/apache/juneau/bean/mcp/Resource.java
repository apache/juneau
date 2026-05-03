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
 * MCP resource descriptor ({@code resources/list} entry).
 */
@Bean
public class Resource {

	private String uri;
	private String name;
	private String title;
	private String description;
	private String mimeType;
	private Long size;

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
	public Resource setUri(String value) {
		uri = value;
		return this;
	}

	/**
	 * Short resource name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the short name.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Resource setName(String value) {
		name = value;
		return this;
	}

	/**
	 * Human-readable title.
	 *
	 * @return The title, or {@code null} if not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Resource setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Resource description.
	 *
	 * @return The description, or {@code null} if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Resource setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * MIME type hint.
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
	public Resource setMimeType(String value) {
		mimeType = value;
		return this;
	}

	/**
	 * Optional size in bytes when known statically.
	 *
	 * @return The size, or {@code null} if not set.
	 */
	public Long getSize() {
		return size;
	}

	/**
	 * Sets the size hint.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public Resource setSize(Long value) {
		size = value;
		return this;
	}
}
