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

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MarkdownWriter}.
 */
class MarkdownWriter_Test {

	private static String run(ThrowingConsumer<MarkdownWriter> body) throws Exception {
		var sw = new StringWriter();
		var w = new MarkdownWriter(sw, false, 10, false,
			UriResolver.of(UriResolution.NONE, UriRelativity.RESOURCE, UriContext.DEFAULT));
		body.accept(w);
		w.flush();
		return sw.toString();
	}

	@FunctionalInterface
	private interface ThrowingConsumer<T> {
		void accept(T t) throws Exception;
	}

	@Test void c01_heading() throws Exception {
		var out = run(w -> w.heading(1, "Title"));
		assertEquals("# Title\n", out);
	}

	@Test void c02_headingLevels() throws Exception {
		assertEquals("# H1\n", run(w -> w.heading(1, "H1")));
		assertEquals("## H2\n", run(w -> w.heading(2, "H2")));
		assertEquals("### H3\n", run(w -> w.heading(3, "H3")));
		assertEquals("#### H4\n", run(w -> w.heading(4, "H4")));
		assertEquals("##### H5\n", run(w -> w.heading(5, "H5")));
		assertEquals("###### H6\n", run(w -> w.heading(6, "H6")));
	}

	@Test void c03_tableHeader() throws Exception {
		var out = run(w -> w.tableHeader("col1", "col2"));
		assertEquals("| col1 | col2 |\n", out);
	}

	@Test void c04_tableSeparator() throws Exception {
		var out = run(w -> w.tableSeparator(2));
		assertEquals("|---|---|\n", out);
	}

	@Test void c05_tableRow() throws Exception {
		var out = run(w -> w.tableRow("val1", "val2"));
		assertEquals("| val1 | val2 |\n", out);
	}

	@Test void c06_bulletItem() throws Exception {
		var out = run(w -> w.bulletItem(0, "text"));
		assertEquals("- text\n", out);
	}

	@Test void c07_nestedBulletItem() throws Exception {
		var out = run(w -> w.bulletItem(2, "text"));
		assertEquals("    - text\n", out);
	}

	@Test void c08_bold() throws Exception {
		var out = run(w -> w.bold("text"));
		assertEquals("**text**", out);
	}

	@Test void c09_italic() throws Exception {
		var out = run(w -> w.italic("text"));
		assertEquals("*text*", out);
	}

	@Test void c10_code() throws Exception {
		var out = run(w -> w.code("text"));
		assertEquals("`text`", out);
	}

	@Test void c11_horizontalRule() throws Exception {
		var out = run(w -> w.horizontalRule());
		assertEquals("---\n", out);
	}

	@Test void c12_escapePipe() throws Exception {
		var out = MarkdownWriter.escapeCell("hello | world");
		assertEquals("hello \\| world", out);
	}

	@Test void c13_escapeBackslash() throws Exception {
		var out = MarkdownWriter.escapeCell("path\\to\\file");
		assertEquals("path\\\\to\\\\file", out);
	}

	@Test void c14_blankLine() throws Exception {
		var out = run(w -> w.blankLine());
		assertEquals("\n", out);
	}

	@Test void c15_fullTable() throws Exception {
		var out = run(w -> {
			w.tableHeader("A", "B");
			w.tableSeparator(2);
			w.tableRow("1", "2");
		});
		assertTrue(out.contains("| A | B |"), out);
		assertTrue(out.contains("|---|---|"), out);
		assertTrue(out.contains("| 1 | 2 |"), out);
	}
}
