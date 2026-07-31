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
package org.apache.juneau.bean.mcp.v20250618;

import org.apache.juneau.marshall.*;

/**
 * MCP resource-template descriptor ({@code resources/templates/list} entry).
 */
@Marshalled
public class ResourceTemplate {

	private String uriTemplate;
	private String name;
	private String title;
	private String description;
	private String mimeType;

	/**
	 * URI template (RFC 6570) describing matching resource URIs.
	 *
	 * @return The URI template, or {@code null} if not set.
	 */
	public String getUriTemplate() {
		return uriTemplate;
	}

	/**
	 * Sets the URI template.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplate setUriTemplate(String value) {
		uriTemplate = value;
		return this;
	}

	/**
	 * Short template name.
	 *
	 * @return The name, or {@code null} if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the short name.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplate setName(String value) {
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
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplate setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Template description.
	 *
	 * @return The description, or {@code null} if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the description.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplate setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * MIME type hint for matching resources.
	 *
	 * @return The MIME type, or {@code null} if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the MIME type.
	 *
	 * @param value The new value.  Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public ResourceTemplate setMimeType(String value) {
		mimeType = value;
		return this;
	}
}
