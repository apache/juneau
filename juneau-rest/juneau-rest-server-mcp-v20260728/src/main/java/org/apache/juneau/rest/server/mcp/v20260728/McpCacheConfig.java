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

import java.util.*;

/**
 * Binding-owned, immutable-after-construction cache configuration for a v2 {@link McpRevision}.
 *
 * <p>
 * Applications populate one {@link McpCacheConfig} and pass it to
 * {@link McpRevision#McpRevision(org.apache.juneau.bean.mcp.v20260728.ServerCapabilities, McpCacheConfig)}. Every
 * setter validates its {@link McpCacheHint} argument eagerly (including entries assigned via
 * {@link #setResourceReadOverrides(Map)}), so an invalid TTL fails at configuration time rather than at dispatch
 * time.
 *
 * <p>
 * This type holds no resolution logic; precedence between {@link #getDefaultHint()}, the per-method hints, and
 * {@link #getResourceReadOverrides()} is owned by {@link McpRevision}.
 */
public class McpCacheConfig {

	private McpCacheHint defaultHint;
	private McpCacheHint toolsList;
	private McpCacheHint promptsList;
	private McpCacheHint resourcesList;
	private McpCacheHint resourceTemplatesList;
	private McpCacheHint resourcesRead;
	private Map<String,McpCacheHint> resourceReadOverrides = new LinkedHashMap<>();

	private static McpCacheHint validate(McpCacheHint value) {
		if (value != null && value.getTtlMs() != null && value.getTtlMs() < 0)
			throw new IllegalArgumentException("ttlMs " + value.getTtlMs() + " is below minimum 0");
		return value;
	}

	/**
	 * The fallback hint applied when a method-specific hint is not configured.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getDefaultHint() {
		return defaultHint;
	}

	/**
	 * Sets the fallback hint.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setDefaultHint(McpCacheHint value) {
		defaultHint = validate(value);
		return this;
	}

	/**
	 * The hint applied to {@code tools/list}.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getToolsList() {
		return toolsList;
	}

	/**
	 * Sets the hint applied to {@code tools/list}.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setToolsList(McpCacheHint value) {
		toolsList = validate(value);
		return this;
	}

	/**
	 * The hint applied to {@code prompts/list}.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getPromptsList() {
		return promptsList;
	}

	/**
	 * Sets the hint applied to {@code prompts/list}.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setPromptsList(McpCacheHint value) {
		promptsList = validate(value);
		return this;
	}

	/**
	 * The hint applied to {@code resources/list}.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getResourcesList() {
		return resourcesList;
	}

	/**
	 * Sets the hint applied to {@code resources/list}.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setResourcesList(McpCacheHint value) {
		resourcesList = validate(value);
		return this;
	}

	/**
	 * The hint applied to {@code resources/templates/list}.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getResourceTemplatesList() {
		return resourceTemplatesList;
	}

	/**
	 * Sets the hint applied to {@code resources/templates/list}.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setResourceTemplatesList(McpCacheHint value) {
		resourceTemplatesList = validate(value);
		return this;
	}

	/**
	 * The hint applied to {@code resources/read} when no per-URI override matches.
	 *
	 * @return The hint, or {@code null} if not set.
	 */
	public McpCacheHint getResourcesRead() {
		return resourcesRead;
	}

	/**
	 * Sets the hint applied to {@code resources/read} when no per-URI override matches.
	 *
	 * @param value The new value. Can be {@code null} to unset the property.
	 * @return This object.
	 */
	public McpCacheConfig setResourcesRead(McpCacheHint value) {
		resourcesRead = validate(value);
		return this;
	}

	/**
	 * Per-URI hint overrides for {@code resources/read}, keyed by exact resource URI.
	 *
	 * <p>
	 * Returns an unmodifiable view (in insertion order) so callers cannot bypass validation via direct mutation.
	 *
	 * @return An unmodifiable map. Never {@code null}.
	 */
	public Map<String,McpCacheHint> getResourceReadOverrides() {
		return Collections.unmodifiableMap(resourceReadOverrides);
	}

	/**
	 * Replaces the per-URI hint overrides.
	 *
	 * @param value The new value (or {@code null} to clear). Every entry is validated; a {@code null} URI key is
	 * 	rejected.
	 * @return This object.
	 */
	public McpCacheConfig setResourceReadOverrides(Map<String,McpCacheHint> value) {
		var copy = new LinkedHashMap<String,McpCacheHint>();
		if (value != null)
			value.forEach((uri, hint) -> putValidated(copy, uri, hint));
		resourceReadOverrides = copy;
		return this;
	}

	/**
	 * Adds or replaces a single per-URI hint override.
	 *
	 * @param uri The exact resource URI. Must not be {@code null}.
	 * @param hint The hint. Can be {@code null}.
	 * @return This object.
	 */
	public McpCacheConfig addResourceReadOverride(String uri, McpCacheHint hint) {
		putValidated(resourceReadOverrides, uri, hint);
		return this;
	}

	private static void putValidated(Map<String,McpCacheHint> target, String uri, McpCacheHint hint) {
		if (uri == null)
			throw new IllegalArgumentException("resourceReadOverrides URI must not be null");
		target.put(uri, validate(hint));
	}
}
