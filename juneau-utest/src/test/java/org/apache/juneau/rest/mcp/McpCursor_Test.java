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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.cp.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for {@link McpCursor} pagination strategies.
 */
class McpCursor_Test {

	private static final BasicBeanStore CTX = BasicBeanStore.create().build();

	@Test
	void singlePage_returnsAll_withNullCursor() {
		var page = McpCursor.SINGLE_PAGE.page(List.of("a", "b", "c"), null, CTX);
		assertList(page.items(), "a", "b", "c");
		assertNull(page.nextCursor());
	}

	@Test
	void singlePage_acceptsNullList() {
		var page = McpCursor.SINGLE_PAGE.page(null, "ignored", CTX);
		assertEmpty(page.items());
		assertNull(page.nextCursor());
	}

	@Test
	void singlePage_acceptsAnyCursor() {
		var page = McpCursor.SINGLE_PAGE.page(List.of("a", "b"), "ignored", CTX);
		assertSize(2, page.items());
		assertNull(page.nextCursor());
	}

	@Test
	void fixedSize_invalid_pageSize_throws() {
		assertThrows(IllegalArgumentException.class, () -> McpCursor.fixedSize(0));
		assertThrows(IllegalArgumentException.class, () -> McpCursor.fixedSize(-1));
	}

	@Test
	void fixedSize_first_page() {
		var c = McpCursor.fixedSize(2);
		var page = c.page(List.of("a", "b", "c", "d"), null, CTX);
		assertList(page.items(), "a", "b");
		assertString("2", page.nextCursor());
	}

	@Test
	void fixedSize_middle_page() {
		var c = McpCursor.fixedSize(2);
		var page = c.page(List.of("a", "b", "c", "d", "e"), "2", CTX);
		assertList(page.items(), "c", "d");
		assertString("4", page.nextCursor());
	}

	@Test
	void fixedSize_last_page_no_next() {
		var c = McpCursor.fixedSize(2);
		var page = c.page(List.of("a", "b", "c"), "2", CTX);
		assertList(page.items(), "c");
		assertNull(page.nextCursor());
	}

	@Test
	void fixedSize_offset_beyond_returnsEmpty() {
		var c = McpCursor.fixedSize(2);
		var page = c.page(List.of("a", "b"), "10", CTX);
		assertEmpty(page.items());
		assertNull(page.nextCursor());
	}

	@Test
	void fixedSize_acceptsNullList() {
		var c = McpCursor.fixedSize(3);
		var page = c.page(null, null, CTX);
		assertEmpty(page.items());
		assertNull(page.nextCursor());
	}

	@Test
	void parseOffset_handlesNullsAndJunk() {
		assertEquals(0, McpCursor.parseOffset(null));
		assertEquals(0, McpCursor.parseOffset(""));
		assertEquals(0, McpCursor.parseOffset("not-a-number"));
		assertEquals(0, McpCursor.parseOffset("-5"));
		assertEquals(7, McpCursor.parseOffset("7"));
	}

	@Test
	void mcpPage_nullItems_defaultsToEmpty() {
		var p = new McpPage<>(null, "next");
		assertEmpty(p.items());
		assertString("next", p.nextCursor());
	}
}
