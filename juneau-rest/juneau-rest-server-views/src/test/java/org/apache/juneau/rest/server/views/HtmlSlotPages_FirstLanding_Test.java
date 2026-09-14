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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * HTML-slot pages first-landing pins after the page-only Java types were deleted: those types are gone, dual-use
 * methods stay unannotated, and views CSS/JS introduce no {@code slds-*} or Salesforce Sans.
 */
@SuppressWarnings({
	"java:S5961" // Contract test is intentionally dense; splitting would hide landing-page pins.
})
class HtmlSlotPages_FirstLanding_Test extends TestBase {

	private static final List<String> GONE = List.of(
		"org.apache.juneau.rest.server.views.PageDef",
		"org.apache.juneau.rest.server.views.Tab",
		"org.apache.juneau.rest.server.views.Subtab",
		"org.apache.juneau.rest.server.views.PageTable",
		"org.apache.juneau.rest.server.views.ViewCardBody",
		"org.apache.juneau.rest.server.views.CardGridTable",
		"org.apache.juneau.rest.server.widgets.Card",
		"org.apache.juneau.rest.server.widgets.CardBody",
		"org.apache.juneau.rest.server.widgets.CardContent",
		"org.apache.juneau.rest.server.widgets.CardField",
		"org.apache.juneau.rest.server.widgets.CardFieldList",
		"org.apache.juneau.rest.server.widgets.CardGrid"
	);

	@Test void a01_pageOnlyTypesAreGone() {
		for (var name : GONE)
			assertThrows(ClassNotFoundException.class, () -> Class.forName(name), name);
		assertThrows(NoSuchFieldException.class, () -> ViewsMixin.class.getDeclaredField("PAGES_JS_PATH"));
		assertThrows(NoSuchFieldException.class, () -> ViewsMixin.class.getDeclaredField("CARDS_JS_PATH"));
		assertThrows(NoSuchMethodException.class, () -> ViewsMixin.class.getDeclaredMethod("getPagesScript"));
		assertNull(ViewsMixin.class.getResource("/org/apache/juneau/views/juneau-pages.js"));
		assertNull(ViewsMixin.class.getResource("/org/apache/juneau/widgets/juneau-cards.js"));
	}

	@Test void a02_dualUseMethodsAreNotDeprecated() {
		assertNull(ViewTable.class.getAnnotation(Deprecated.class));
		assertNull(RegionDef.class.getAnnotation(Deprecated.class));
		assertNull(RegionTable.class.getAnnotation(Deprecated.class));
		assertNoDeprecatedMethods(ViewTable.class, "of");
		assertNoDeprecatedMethods(RegionDef.class, "create");
		assertNoDeprecatedMethods(RegionTable.class, "of");
		assertNoDeprecatedMethods(RegionTable.class, "sidecar");
		assertNoDeprecatedMethods(RegionTable.class, "detailSidecar");
	}

	@Test void a02b_dualUseJavadocNamesEnvelopeAndMount() throws Exception {
		assertPageBodyClassification(ViewTable.class);
		assertTrue(sourceOf(ViewTable.class).contains("ViewSlot#envelope"),
			"ViewTable javadoc must name ViewSlot.envelope as the page-body factory");
		assertPageBodyClassification(RegionDef.class);
		assertPageBodyClassification(RegionTable.class);
	}

	@Test void b01_viewsCssHasTwoRowNavWithoutPillsOrSlds() throws Exception {
		var css = resource(ViewsMixin.VIEWS_CSS_RESOURCE);
		assertTrue(css.contains(".juneau-page-nav-sections"), css);
		assertTrue(css.contains(".juneau-page-nav-children"), css);
		assertFalse(css.contains(".juneau-page-nav-cloud"), css);
		assertFalse(css.contains("url("), () -> "page-nav cloud SVG was the last url() in views CSS:\n" + css);
		assertTrue(css.contains(".juneau-page-nav-section[aria-current=\"page\"]"), css);
		assertTrue(css.contains(".juneau-page-nav-child[aria-current=\"page\"]"), css);
		assertFalse(css.contains("juneau-page-nav-section-selected"), css);
		assertFalse(css.contains("juneau-page-nav-child-selected"), css);
		var childStart = css.indexOf(".juneau-page-nav-child {");
		assertTrue(childStart >= 0, css);
		var childBlock = css.substring(childStart, css.indexOf("}", childStart));
		assertTrue(childBlock.contains("border-radius: 0"),
			() -> "child links must be square text, not pills, block:\n" + childBlock);
		assertFalse(childBlock.contains("font-size"),
			() -> "shared section/child shape must not share a font-size, block:\n" + childBlock);
		var childFontStart = css.indexOf(".juneau-page-nav-child {", childStart + 1);
		assertTrue(childFontStart >= 0, css);
		var childFontBlock = css.substring(childFontStart, css.indexOf("}", childFontStart));
		assertTrue(childFontBlock.contains("font-size: var(--jc-page-nav-child-font-size, 12px)"),
			() -> "children spend --jc-page-nav-child-font-size, block:\n" + childFontBlock);
		assertTrue(css.contains(".juneau-page-nav-sections {"), css);
		var hairlineRule = css.indexOf(".juneau-page-nav-sections {");
		assertTrue(hairlineRule >= 0, css);
		var hairlineShape = css.substring(hairlineRule, css.indexOf("}", hairlineRule));
		assertTrue(hairlineShape.contains("border-bottom: var(--jc-page-nav-hairline, 2px) solid"),
			() -> "sections row must carry a 2px hairline under it:\n" + hairlineShape);
		var navRule = css.indexOf(".juneau-page-nav {");
		assertTrue(navRule >= 0, css);
		var navBlock = css.substring(navRule, css.indexOf("}", navRule));
		assertTrue(navBlock.contains("border-bottom: var(--jc-nav-indicator-width, 3px) solid"),
			() -> "the pair's floor spends --jc-nav-indicator-width, block:\n" + navBlock);
		assertFalse(navBlock.contains("#ffffff"),
			() -> "nav background is a chrome hue, not a views hex, block:\n" + navBlock);
		var sectionRule = css.indexOf(".juneau-page-nav-section {");
		assertTrue(sectionRule >= 0, css);
		var sectionBlock = css.substring(sectionRule, css.indexOf("}", sectionRule));
		assertTrue(sectionBlock.contains("border-top: var(--jc-nav-indicator-width, 3px) solid transparent"),
			() -> "selected-section accent sits on the top edge, block:\n" + sectionBlock);
		assertTrue(sectionBlock.contains("font-size: var(--jc-page-nav-section-font-size, 13px)"),
			() -> "sections spend --jc-page-nav-section-font-size, block:\n" + sectionBlock);
		assertFalse(sectionBlock.contains("border-bottom"),
			() -> "section tabs must not use a bottom underline, block:\n" + sectionBlock);
		assertFalse(css.contains("slds-"), css);
		assertFalse(css.toLowerCase().contains("salesforce sans"), css);
		assertFalse(css.contains("#1589EE"), css);
	}

	@Test void b02_regionsJsDoesNotIntroduceSlds() throws Exception {
		var js = RegionsHarness.regionsJs();
		assertFalse(js.contains("slds-"), js);
		assertFalse(js.toLowerCase().contains("salesforce sans"), js);
	}

	@Test void b03_viewsJsDoesNotDerivePageNavSelectionFromUrl() throws Exception {
		var js = resource(ViewsMixin.VIEWS_JS_RESOURCE);
		assertFalse(js.contains("pageNavPathsMatch"), js);
		assertFalse(js.contains("NS.pageNav"), js);
		assertFalse(js.contains("juneau-page-nav-section-selected"), js);
		assertFalse(js.contains("juneau-page-nav-child-selected"), js);
		assertTrue(js.contains("aria-current=\"page\""), js);
	}

	private static void assertNoDeprecatedMethods(Class<?> c, String name) {
		for (var m : c.getDeclaredMethods()) {
			if (m.getName().equals(name) && Modifier.isPublic(m.getModifiers()))
				assertNull(m.getAnnotation(Deprecated.class), m::toString);
		}
	}

	private static void assertPageBodyClassification(Class<?> c) throws Exception {
		var src = sourceOf(c);
		assertTrue(src.contains("JuneauViews.regions.mount"),
			() -> c.getName() + " javadoc must name mount as the page-slot factory:\n" + src);
		assertTrue(src.contains("Not {@code @Deprecated}"),
			() -> c.getName() + " javadoc must say the dual-use method is not @Deprecated:\n" + src);
	}

	private static String sourceOf(Class<?> c) throws Exception {
		var rel = "src/main/java/" + c.getName().replace('.', '/') + ".java";
		var basedir = System.getProperty("basedir");
		var candidates = new ArrayList<Path>();
		if (basedir != null) {
			candidates.add(Path.of(basedir, rel));
			candidates.add(Path.of(basedir, "../juneau-rest-server-widgets", rel));
			candidates.add(Path.of(basedir, "../juneau-rest-server-views", rel));
		}
		candidates.add(Path.of(rel));
		candidates.add(Path.of("juneau-rest/juneau-rest-server-views", rel));
		candidates.add(Path.of("juneau-rest/juneau-rest-server-widgets", rel));
		for (var p : candidates) {
			if (Files.isRegularFile(p))
				return Files.readString(p);
		}
		fail("source not found for " + c.getName() + "; tried " + candidates);
		return "";
	}

	private static String resource(String name) throws Exception {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
