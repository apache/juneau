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
package org.apache.juneau.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.markdown.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Markdown} marshaller.
 */
class Markdown_Test extends TestBase {

	@Test void d01_of() throws Exception {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var md = Markdown.of(bean);
		assertTrue(md.contains("| Key | Value |") || md.contains("| Property | Value |"), "Expected table header: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name: " + md);
		assertTrue(md.contains("| age | 30 |"), "Expected age: " + md);
	}

	@Test void d02_write() throws Exception {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var md = Markdown.DEFAULT.write(bean);
		assertTrue(md.contains("| Key | Value |") || md.contains("| Property | Value |"), "Expected table header: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name: " + md);
	}

	@Test void d03_docMode() throws Exception {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var serializer = MarkdownDocSerializer.create().title("Person").build();
		var md = new MarkdownDoc(serializer, MarkdownDocParser.DEFAULT).write(bean);
		assertTrue(md.contains("# Person"), "Expected H1 title: " + md);
		assertTrue(md.contains("| Key | Value |") || md.contains("| Property | Value |"), "Expected table: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name: " + md);
	}

	@Test void d04_toOutput() throws Exception {
		var bean = JsonMap.of("name", "Alice", "age", 30);
		var sw = new StringWriter();
		Markdown.DEFAULT.write(bean, sw);
		var md = sw.toString();
		assertTrue(md.contains("| Key | Value |") || md.contains("| Property | Value |"), "Expected table header: " + md);
		assertTrue(md.contains("| name | Alice |"), "Expected name: " + md);
	}
}
