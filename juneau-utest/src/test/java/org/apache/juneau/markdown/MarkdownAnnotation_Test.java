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

import org.apache.juneau.*;
import org.apache.juneau.marshall.markdown.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Markdown @Markdown} annotation and metadata.
 */
@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class MarkdownAnnotation_Test extends TestBase {

	private static MarkdownSerializer serializer() {
		return MarkdownSerializer.DEFAULT;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a* - MarkdownAnnotation default value
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_markdownAnnotationDefault() {
		assertNotNull(MarkdownAnnotation.DEFAULT);
	}

	@Test void a02_markdownAnnotationDefaultMethods() {
		var a = MarkdownAnnotation.DEFAULT;
		assertNotNull(a.description());
		assertEquals(0, a.description().length);
		assertEquals(MarkdownFormat.DEFAULT, a.format());
		assertEquals("", a.heading());
		assertFalse(a.noTables());
		assertFalse(a.noHeaders());
		assertFalse(a.code());
		assertEquals("", a.link());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d* - MarkdownApplyAnnotation tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class D02_Class {}
	public static class D04_Class {}

	@Test void d01_applyAnnotationDefault() {
		assertNotNull(MarkdownApplyAnnotation.DEFAULT);
	}

	@Test void d02_applyAnnotationEmpty() {
		assertTrue(MarkdownApplyAnnotation.empty(null));
		assertTrue(MarkdownApplyAnnotation.empty(MarkdownApplyAnnotation.DEFAULT));
		assertFalse(MarkdownApplyAnnotation.empty(MarkdownApplyAnnotation.create(D02_Class.class).build()));
	}

	@Test void d03_applyAnnotationCreate() {
		var a = MarkdownApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void d04_applyAnnotationCreateWithClass() {
		var a = MarkdownApplyAnnotation.create(D04_Class.class).build();
		assertNotNull(a);
	}

	@Test void d05_applyAnnotationCreateWithString() {
		var a = MarkdownApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void d06_applyAnnotationBuilderValue() {
		var a = MarkdownApplyAnnotation.create().value(MarkdownAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e01 - format TABLE
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.TABLE)
	public static class E01_Bean {
		public String name;
	}

	@Test void e01_formatTable() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E01_Bean.class));
		assertEquals(MarkdownFormat.TABLE, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e02 - format LIST
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.LIST)
	public static class E02_Bean {
		public String name;
	}

	@Test void e02_formatList() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E02_Bean.class));
		assertEquals(MarkdownFormat.LIST, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e03 - format CODE
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.CODE)
	public static class E03_Bean {
		public String name;
	}

	@Test void e03_formatCode() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E03_Bean.class));
		assertEquals(MarkdownFormat.CODE, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e04 - format PLAIN_TEXT
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.PLAIN_TEXT)
	public static class E04_Bean {
		public String name;
	}

	@Test void e04_formatPlainText() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E04_Bean.class));
		assertEquals(MarkdownFormat.PLAIN_TEXT, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e05 - format JSON5
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(format = MarkdownFormat.JSON5)
	public static class E05_Bean {
		public String name;
	}

	@Test void e05_formatJson5() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E05_Bean.class));
		assertEquals(MarkdownFormat.JSON5, cm.getFormat());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e06 - noTables
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(noTables = true)
	public static class E06_Bean {
		public String name;
	}

	@Test void e06_noTables() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E06_Bean.class));
		assertTrue(cm.isNoTables());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e07 - noHeaders
	//-----------------------------------------------------------------------------------------------------------------

	@Markdown(noHeaders = true)
	public static class E07_Bean {
		public String name;
	}

	@Test void e07_noHeaders() {
		var cm = serializer().getMarkdownClassMeta(serializer().getMarshallingContext().getClassMeta(E07_Bean.class));
		assertTrue(cm.isNoHeaders());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e08 - custom heading (property-level)
	//-----------------------------------------------------------------------------------------------------------------

	public static class E08_Bean {
		@Markdown(heading = "Custom Name")
		public String name;
	}

	@Test void e08_customHeading() {
		var bm = serializer().getMarshallingContext().getBeanMeta(E08_Bean.class);
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

	@Test void e09_code() {
		var bm = serializer().getMarshallingContext().getBeanMeta(E09_Bean.class);
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

	@Test void e10_link() {
		var bm = serializer().getMarshallingContext().getBeanMeta(E10_Bean.class);
		var pm = bm.getPropertyMeta("url");
		var mpm = serializer().getMarkdownBeanPropertyMeta(pm);
		assertEquals("https://example.com/{value}", mpm.getLink());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g* - MarkdownConfigAnnotation tests
	//-----------------------------------------------------------------------------------------------------------------

	@MarkdownConfig
	public static class G01_Bean {}

	@Test void g01_markdownConfigSerializerApply() {
		var s = MarkdownSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@MarkdownConfig(rank = 1)
	public static class G02_Bean {}

	@Test void g02_markdownConfigRank() {
		var s = MarkdownSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@MarkdownConfig
	public static class G03_Bean {}

	@Test void g03_markdownConfigParserApply() {
		var p = MarkdownParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}
}
