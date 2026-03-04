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
package org.apache.juneau.markdown;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for Markdown media type configuration.
 */
class MarkdownMediaType_Test {

	@Test void g01_producesCorrectMediaType() throws Exception {
		var ct = MarkdownSerializer.DEFAULT.getResponseContentType();
		assertEquals("text", ct.getType());
		assertEquals("markdown", ct.getSubType());
	}

	@Test void g02_acceptsMediaTypes() throws Exception {
		var types = new java.util.ArrayList<String>();
		MarkdownSerializer.DEFAULT.forEachAcceptMediaType(mt -> types.add(mt.getType() + "/" + mt.getSubType()));
		assertTrue(types.stream().anyMatch(t -> t.contains("markdown")), "Expected text/markdown or text/x-markdown: " + types);
	}

	@Test void g03_parserAcceptsMediaTypes() throws Exception {
		var types = MarkdownParser.DEFAULT.getMediaTypes().stream()
			.map(mt -> mt.getType() + "/" + mt.getSubType())
			.toList();
		assertTrue(types.stream().anyMatch(t -> t.contains("markdown")), "Expected markdown types: " + types);
	}
}
