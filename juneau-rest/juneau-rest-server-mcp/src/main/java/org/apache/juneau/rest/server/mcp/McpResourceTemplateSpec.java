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
 * Revision-neutral resource-template descriptor returned from {@code resources/templates/list}.
 *
 * <p>
 * This is a static descriptor: it carries no read callback, template matcher, cache hint, or dated-revision
 * wire import. It is paired with a read callback and optional per-variable completers by
 * {@link McpResourceTemplateHandler#descriptor()}, or registered on its own as a listing-only template via
 * {@link McpServerConfig#addResourceTemplate(McpResourceTemplateSpec...)}.
 *
 * <p>
 * Supersedes the wire-level {@code ResourceTemplate} bean.
 */
public class McpResourceTemplateSpec {

	private String uriTemplate;
	private String name;
	private String title;
	private String description;
	private String mimeType;

	/**
	 * The URI template (RFC 6570) describing matching resource URIs.
	 *
	 * @return The URI template, or <jk>null</jk> if not set.
	 */
	public String getUriTemplate() {
		return uriTemplate;
	}

	/**
	 * Sets the URI template.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceTemplateSpec setUriTemplate(String value) {
		uriTemplate = value;
		return this;
	}

	/**
	 * The template name.
	 *
	 * @return The name, or <jk>null</jk> if not set.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the template name.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceTemplateSpec setName(String value) {
		name = value;
		return this;
	}

	/**
	 * The human-readable template title.
	 *
	 * @return The title, or <jk>null</jk> if not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the template title.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceTemplateSpec setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * The human-readable template description.
	 *
	 * @return The description, or <jk>null</jk> if not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the template description.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceTemplateSpec setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * The media type of resources matching this template.
	 *
	 * @return The media type, or <jk>null</jk> if not set.
	 */
	public String getMimeType() {
		return mimeType;
	}

	/**
	 * Sets the media type of resources matching this template.
	 *
	 * @param value The new value. Can be <jk>null</jk> to unset the property.
	 * @return This object.
	 */
	public McpResourceTemplateSpec setMimeType(String value) {
		mimeType = value;
		return this;
	}
}
