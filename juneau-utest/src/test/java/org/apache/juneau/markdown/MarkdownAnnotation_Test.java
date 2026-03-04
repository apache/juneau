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

import org.apache.juneau.markdown.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Markdown @Markdown} annotation and metadata.
 */
class MarkdownAnnotation_Test {

	private static MarkdownSerializer serializer() {
		return MarkdownSerializer.DEFAULT;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e01 - format TABLE
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.TABLE)
	public static class E01_Bean {
		public String name;
	}

	@Test void e01_formatTable() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E01_Bean.class));
		assertEquals(MarkdownFormat.TABLE, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e02 - format LIST
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.LIST)
	public static class E02_Bean {
		public String name;
	}

	@Test void e02_formatList() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E02_Bean.class));
		assertEquals(MarkdownFormat.LIST, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e03 - format CODE
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.CODE)
	public static class E03_Bean {
		public String name;
	}

	@Test void e03_formatCode() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E03_Bean.class));
		assertEquals(MarkdownFormat.CODE, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e04 - format PLAIN_TEXT
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.PLAIN_TEXT)
	public static class E04_Bean {
		public String name;
	}

	@Test void e04_formatPlainText() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E04_Bean.class));
		assertEquals(MarkdownFormat.PLAIN_TEXT, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e05 - format JSON5
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.JSON5)
	public static class E05_Bean {
		public String name;
	}

	@Test void e05_formatJson5() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E05_Bean.class));
		assertEquals(MarkdownFormat.JSON5, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e06 - noTables
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(noTables = true)
	public static class E06_Bean {
		public String name;
	}

	@Test void e06_noTables() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E06_Bean.class));
		assertTrue(cm.isNoTables());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e07 - noHeaders
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(noHeaders = true)
	public static class E07_Bean {
		public String name;
	}

	@Test void e07_noHeaders() throws Exception {
		var cm = serializer().getMarkdownClassMeta(serializer().getBeanContext().getClassMeta(E07_Bean.class));
		assertTrue(cm.isNoHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e08 - custom heading (property-level)
	//-----------------------------------------------------------------------------------------------------------------

	public static class E08_Bean {
		@Markdown(heading = "Custom Name")
		public String name;
	}

	@Test void e08_customHeading() throws Exception {
		var bm = serializer().getBeanContext().getBeanMeta(E08_Bean.class);
		var pm = bm.getPropertyMeta("name");
		var mpm = serializer().getMarkdownBeanPropertyMeta(pm);
		assertEquals("Custom Name", mpm.getHeading());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e09 - code (property-level)
	//-----------------------------------------------------------------------------------------------------------------

	public static class E09_Bean {
		@Markdown(code = true)
		public String value;
	}

	@Test void e09_code() throws Exception {
		var bm = serializer().getBeanContext().getBeanMeta(E09_Bean.class);
		var pm = bm.getPropertyMeta("value");
		var mpm = serializer().getMarkdownBeanPropertyMeta(pm);
		assertTrue(mpm.isCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e10 - link (property-level)
	//-----------------------------------------------------------------------------------------------------------------

	public static class E10_Bean {
		@Markdown(link = "https://example.com/{value}")
		public String url;
	}

	@Test void e10_link() throws Exception {
		var bm = serializer().getBeanContext().getBeanMeta(E10_Bean.class);
		var pm = bm.getPropertyMeta("url");
		var mpm = serializer().getMarkdownBeanPropertyMeta(pm);
		assertEquals("https://example.com/{value}", mpm.getLink());
	}
}
