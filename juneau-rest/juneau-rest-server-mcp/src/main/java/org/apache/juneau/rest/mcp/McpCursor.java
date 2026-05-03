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

import org.apache.juneau.cp.*;

/**
 * Pagination strategy for MCP {@code list} dispatchers (tools / prompts / resources).
 *
 * <p>
 * Built-in strategies:
 * <ul>
 * 	<li>{@link #SINGLE_PAGE} - Returns everything in one page (default; ignores any incoming cursor).
 * 	<li>{@link #fixedSize(int)} - Slices a list into pages of a fixed size using opaque integer cursors.
 * </ul>
 */
@FunctionalInterface
public interface McpCursor {

	/**
	 * Slices a descriptor list into a page returned from {@code tools/list}, {@code prompts/list}, or
	 * {@code resources/list}.
	 *
	 * @param all The full list of descriptors. Never {@code null}.
	 * @param cursor Opaque cursor from the request, or {@code null} for the first page.
	 * @param ctx Per-request bean store (provided for cursor strategies that need access to the request).
	 * @param <T> Element type.
	 * @return A non-{@code null} page.
	 */
	<T> McpPage<T> page(List<T> all, String cursor, BasicBeanStore ctx);

	/**
	 * Returns everything in a single page; emits no {@code nextCursor}.
	 */
	McpCursor SINGLE_PAGE = new McpCursor() {
		@Override
		public <T> McpPage<T> page(List<T> all, String cursor, BasicBeanStore ctx) {
			return new McpPage<>(all == null ? List.of() : all, null);
		}
	};

	/**
	 * Returns a fixed-page-size cursor.
	 *
	 * <p>
	 * Cursor representation is an opaque integer offset (parsed leniently; non-numeric values restart at offset 0).
	 *
	 * @param pageSize Maximum items per page. Must be {@code > 0}.
	 * @return A new cursor strategy.
	 */
	static McpCursor fixedSize(int pageSize) {
		if (pageSize <= 0)
			throw new IllegalArgumentException("pageSize must be positive: " + pageSize);
		return new McpCursor() {
			@Override
			public <T> McpPage<T> page(List<T> all, String cursor, BasicBeanStore ctx) {
				var src = all == null ? List.<T>of() : all;
				var offset = parseOffset(cursor);
				if (offset >= src.size())
					return new McpPage<>(List.of(), null);
				var end = Math.min(offset + pageSize, src.size());
				var slice = src.subList(offset, end);
				var next = end < src.size() ? Integer.toString(end) : null;
				return new McpPage<>(slice, next);
			}
		};
	}

	/**
	 * Parses a cursor as a non-negative integer offset; returns {@code 0} for {@code null} or non-numeric inputs.
	 *
	 * @param cursor Cursor value (may be {@code null}).
	 * @return Parsed offset.
	 */
	static int parseOffset(String cursor) {
		if (cursor == null || cursor.isEmpty())
			return 0;
		try {
			var v = Integer.parseInt(cursor);
			return Math.max(v, 0);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
