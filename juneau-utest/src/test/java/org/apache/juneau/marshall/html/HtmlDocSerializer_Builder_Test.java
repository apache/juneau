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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Targeted coverage for {@link HtmlDocSerializer.Builder} setters, lazy-list accessors,
 * the {@code merge}/{@code mergeNavLinks} helpers (NONE/INHERIT/indexed-link branches),
 * and the bean→builder copy path.
 */
class HtmlDocSerializer_Builder_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A. Lazy list accessors — exercise the "null → new list" branch.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_aside_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.aside);
		var l = b.aside();
		assertNotNull(l);
		assertSame(l, b.aside());
	}

	@Test void a02_footer_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.footer);
		var l = b.footer();
		assertNotNull(l);
		assertSame(l, b.footer());
	}

	@Test void a03_head_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.head);
		var l = b.head();
		assertNotNull(l);
		assertSame(l, b.head());
	}

	@Test void a04_header_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.header);
		var l = b.header();
		assertNotNull(l);
		assertSame(l, b.header());
	}

	@Test void a05_nav_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.nav);
		var l = b.nav();
		assertNotNull(l);
		assertSame(l, b.nav());
	}

	@Test void a06_navlinks_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.navlinks);
		var l = b.navlinks();
		assertNotNull(l);
		assertSame(l, b.navlinks());
	}

	@Test void a07_script_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.script);
		var l = b.script();
		assertNotNull(l);
		assertSame(l, b.script());
	}

	@Test void a08_style_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.style);
		var l = b.style();
		assertNotNull(l);
		assertSame(l, b.style());
	}

	@Test void a09_stylesheet_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.stylesheet);
		var l = b.stylesheet();
		assertNotNull(l);
		assertSame(l, b.stylesheet());
	}

	@Test void a10_widgets_lazyInit() {
		var b = HtmlDocSerializer.create();
		assertNull(b.widgets);
		var l = b.widgets();
		assertNotNull(l);
		assertSame(l, b.widgets());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. merge() — NONE / INHERIT / plain values.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_merge_plainValue_appends() {
		var b = HtmlDocSerializer.create().aside("a", "b");
		assertEquals(2, b.aside.size());
		assertEquals("a", b.aside.get(0));
		assertEquals("b", b.aside.get(1));
	}

	@Test void b02_merge_NONE_clearsExisting() {
		var b = HtmlDocSerializer.create().aside("a", "b").aside("NONE");
		// "NONE" clears the old list, no other tokens, so result is empty
		assertEquals(0, b.aside.size());
	}

	@Test void b03_merge_INHERIT_includesParent() {
		var b = HtmlDocSerializer.create().aside("a", "b").aside("INHERIT", "c");
		assertEquals(3, b.aside.size());
		assertEquals("a", b.aside.get(0));
		assertEquals("b", b.aside.get(1));
		assertEquals("c", b.aside.get(2));
	}

	@Test void b04_merge_INHERIT_withNullOld_skipsAddAll() {
		// First call has navlinks==null, INHERIT can't add anything. Then add a value.
		var b = HtmlDocSerializer.create().head("INHERIT", "x");
		assertEquals(1, b.head.size());
		assertEquals("x", b.head.get(0));
	}

	@Test void b05_merge_NONE_withNullOld_noOp() {
		var b = HtmlDocSerializer.create().style("NONE");
		assertEquals(0, b.style.size());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. mergeNavLinks() — NONE / INHERIT / indexed-link / plain.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_mergeNavLinks_plainEntry() {
		var b = HtmlDocSerializer.create().navlinks("a:link-a", "b:link-b");
		assertEquals(2, b.navlinks.size());
	}

	@Test void c02_mergeNavLinks_NONE_clears() {
		var b = HtmlDocSerializer.create().navlinks("a:link-a").navlinks("NONE");
		assertEquals(0, b.navlinks.size());
	}

	@Test void c03_mergeNavLinks_INHERIT_appendsParent() {
		var b = HtmlDocSerializer.create().navlinks("a:link-a").navlinks("INHERIT", "b:link-b");
		assertEquals(2, b.navlinks.size());
		assertEquals("a:link-a", b.navlinks.get(0));
		assertEquals("b:link-b", b.navlinks.get(1));
	}

	@Test void c04_mergeNavLinks_indexedLink_insertsAtIndex() {
		// `key[index]:remainder` — pattern requires a key, an index, then a remainder.
		var b = HtmlDocSerializer.create().navlinks("foo:link-a", "bar:link-b").navlinks("INHERIT", "baz[1]:link-c");
		// INHERIT brings in foo:link-a and bar:link-b; baz[1]:link-c inserts at index 1.
		assertEquals(3, b.navlinks.size());
		assertEquals("foo:link-a", b.navlinks.get(0));
		assertEquals("baz:link-c", b.navlinks.get(1));
		assertEquals("bar:link-b", b.navlinks.get(2));
	}

	@Test void c05_mergeNavLinks_indexedLink_emptyKey() {
		// "[0]:remainder" — empty key falls into x.add(index, remainder) path.
		var b = HtmlDocSerializer.create().navlinks("a:link-a").navlinks("INHERIT", "[0]:link-b");
		assertEquals(2, b.navlinks.size());
		assertEquals("link-b", b.navlinks.get(0));
		assertEquals("a:link-a", b.navlinks.get(1));
	}

	@Test void c06_mergeNavLinks_indexedLink_indexBeyondSize_clamps() {
		// Index 99 with only 1 entry pre-existing gets clamped to current size.
		var b = HtmlDocSerializer.create().navlinks("a:link-a").navlinks("INHERIT", "k[99]:r");
		assertEquals(2, b.navlinks.size());
		// "k:r" appended at the clamped end.
		assertEquals("k:r", b.navlinks.get(1));
	}

	@Test void c07_mergeNavLinks_NONE_withNullOld() {
		var b = HtmlDocSerializer.create().navlinks("NONE");
		assertEquals(0, b.navlinks.size());
	}

	@Test void c08_mergeNavLinks_INHERIT_withNullOld() {
		var b = HtmlDocSerializer.create().navlinks("INHERIT", "x:y");
		assertEquals(1, b.navlinks.size());
		assertEquals("x:y", b.navlinks.get(0));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. Setters: nowrap / resolveBodyVars no-arg, asideFloat, template, noResultsMessage.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_nowrap_noArg_setsTrue() {
		var b = HtmlDocSerializer.create().nowrap();
		assertTrue(b.nowrap);
	}

	@Test void d02_nowrap_explicitFalse_setsFalse() {
		var b = HtmlDocSerializer.create().nowrap(true).nowrap(false);
		assertFalse(b.nowrap);
	}

	@Test void d03_resolveBodyVars_noArg_setsTrue() {
		var b = HtmlDocSerializer.create().resolveBodyVars();
		assertTrue(b.resolveBodyVars);
	}

	@Test void d04_asideFloat_setter() {
		var b = HtmlDocSerializer.create().asideFloat(AsideFloat.LEFT);
		assertEquals(AsideFloat.LEFT, b.asideFloat);
	}

	@Test void d05_template_setter() {
		var b = HtmlDocSerializer.create().template(BasicHtmlDocTemplate.class);
		assertEquals(BasicHtmlDocTemplate.class, b.template);
	}

	@Test void d06_noResultsMessage_setter() {
		var b = HtmlDocSerializer.create().noResultsMessage("none here");
		assertEquals("none here", b.noResultsMessage);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. Bean→builder copy path (the static {@code copy(List)} / {@code copy(T[])} helpers and
	//    the {@code Builder(HtmlDocSerializer)} copy constructor).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_copy_fromBuiltSerializer_carriesValues() {
		var orig = HtmlDocSerializer.create()
			.aside("aside-1")
			.footer("footer-1")
			.head("head-1")
			.header("header-1")
			.nav("nav-1")
			.navlinks("foo:bar")
			.script("script-1")
			.style("style-1")
			.stylesheet("ss-1")
			.noResultsMessage("nrm")
			.nowrap(true)
			.resolveBodyVars(true)
			.asideFloat(AsideFloat.LEFT)
			.template(BasicHtmlDocTemplate.class)
			.build();

		var copy = orig.copy().build();

		assertArrayEquals(new String[] { "aside-1" }, copy.getAside());
		assertArrayEquals(new String[] { "footer-1" }, copy.getFooter());
		assertArrayEquals(new String[] { "head-1" }, copy.getHead());
		assertArrayEquals(new String[] { "header-1" }, copy.getHeader());
		assertArrayEquals(new String[] { "nav-1" }, copy.getNav());
		assertArrayEquals(new String[] { "foo:bar" }, copy.getNavlinks());
		assertArrayEquals(new String[] { "script-1" }, copy.getScript());
		assertArrayEquals(new String[] { "style-1" }, copy.getStyle());
		assertArrayEquals(new String[] { "ss-1" }, copy.getStylesheet());
		assertEquals("nrm", copy.getNoResultsMessage());
		assertTrue(copy.isNowrap());
		assertEquals(AsideFloat.LEFT, copy.getAsideFloat());
	}

	@Test void e02_copy_fromEmptyBuilder_keepsEmptyArrays() {
		// All list fields are null/empty in a default builder — the copy(List) helper must
		// return null for them (which the bean side surfaces as empty arrays).
		var orig = HtmlDocSerializer.create().build();
		var copy = orig.copy().build();
		assertEquals(0, copy.getAside().length);
		assertEquals(0, copy.getFooter().length);
		assertEquals(0, copy.getHead().length);
		assertEquals(0, copy.getHeader().length);
		assertEquals(0, copy.getNav().length);
		assertEquals(0, copy.getNavlinks().length);
		assertEquals(0, copy.getScript().length);
		assertEquals(0, copy.getStyle().length);
		assertEquals(0, copy.getStylesheet().length);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. Built-serializer accessors round-trip through Builder values.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_built_isNowrap() {
		assertTrue(HtmlDocSerializer.create().nowrap().build().isNowrap());
		assertFalse(HtmlDocSerializer.create().build().isNowrap());
	}

	@Test void f02_built_getNoResultsMessage_default() {
		assertEquals("<p>no results</p>", HtmlDocSerializer.create().build().getNoResultsMessage());
	}

	@Test void f03_built_getTemplate_isInstance() {
		var s = HtmlDocSerializer.create().build();
		assertNotNull(s.getTemplate());
	}
}
