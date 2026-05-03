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
package org.apache.juneau.rest.mcp;

import java.util.*;

/**
 * Slice of MCP descriptors returned by {@link McpCursor#page(List, String, org.apache.juneau.cp.BasicBeanStore)}.
 *
 * @param <T> Descriptor element type ({@code Tool}, {@code Prompt}, or {@code Resource}).
 * @param items Items to return on the current page. Never {@code null}.
 * @param nextCursor Opaque cursor for the next page, or {@code null} to indicate end-of-results.
 */
public record McpPage<T>(List<T> items, String nextCursor) {

	/**
	 * Convenience constructor that defends against a {@code null} {@code items} value.
	 *
	 * @param items Items.
	 * @param nextCursor Next cursor.
	 */
	public McpPage {
		items = items == null ? List.of() : items;
	}
}
