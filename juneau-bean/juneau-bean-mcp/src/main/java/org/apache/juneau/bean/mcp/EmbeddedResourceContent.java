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
 * MCP {@code resource} content block (inline resource body).
 *
 * <p>
 * The nested {@link #getResource() resource} value uses the same {@code text} / {@code blob} shapes as
 * {@link ReadResourceResult} entries, carried as {@link ResourceContents} implementations.
 */
@Bean(typeName = "resource")
public class EmbeddedResourceContent implements Content {

	private ResourceContents resource;

	/**
	 * Inline resource payload.
	 *
	 * @return The resource body, or {@code null} if not set.
	 */
	public ResourceContents getResource() {
		return resource;
	}

	/**
	 * Sets the inline resource payload.
	 *
	 * @param value The new value.
	 * @return This object (for method chaining).
	 */
	public EmbeddedResourceContent setResource(ResourceContents value) {
		resource = value;
		return this;
	}
}
