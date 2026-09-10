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
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * First-landing deprecation + dual-hat pins for HTML-slot pages: page-only types carry {@code @Deprecated}
 * naming {@code JuneauViews.regions.mount}; dual-use methods do not; views CSS/JS introduce no {@code slds-*}
 * or Salesforce Sans.
 */
class HtmlSlotPages_FirstLanding_Test extends TestBase {

	@Test void a01_pageOnlyTypesAreDeprecated() {
		assertDeprecated(Card.class);
		assertDeprecated(CardGrid.class);
		assertDeprecated(CardBody.class);
		assertDeprecated(CardFieldList.class);
		assertDeprecated(CardField.class);
		assertDeprecated(CardContent.class);
		assertDeprecated(ViewCardBody.class);
		assertDeprecated(CardGridTable.class);
		assertDeprecated(PageDef.class);
		assertDeprecated(Tab.class);
		assertDeprecated(Subtab.class);
		assertDeprecated(PageTable.class);
	}

	@Test void a02_dualUseMethodsAreNotDeprecated() {
		assertNull(ViewTable.class.getAnnotation(Deprecated.class));
		assertNull(RegionDef.class.getAnnotation(Deprecated.class));
		assertNull(RegionTable.class.getAnnotation(Deprecated.class));
		assertNoDeprecatedMethods(ViewTable.class, "of");
		assertNoDeprecatedMethods(RegionDef.class, "create");
		assertNoDeprecatedMethods(RegionTable.class, "of");
	}

	@Test void a03_deprecatedJavadocNamesMountAndSweep() throws Exception {
		assertJavadocSweep(Card.class);
		assertJavadocSweep(CardField.class);
		assertJavadocSweep(PageDef.class);
		assertJavadocSweep(PageTable.class);
		assertJavadocSweep(CardGridTable.class);
	}

	@Test void a04_pagesJsIsDeprecatedForHashSwap() throws Exception {
		var body = resource(ViewsMixin.PAGES_JS_RESOURCE);
		assertTrue(body.contains("@deprecated"), body);
		assertTrue(body.contains("JuneauViews.regions.mount"), body);
		assertTrue(body.contains("grep removal sites"), body);
		assertTrue(ViewsMixin.class.getDeclaredField("PAGES_JS_PATH").isAnnotationPresent(Deprecated.class));
	}

	@Test void b01_viewsCssHasTwoRowNavWithoutPillsOrSlds() throws Exception {
		var css = resource(ViewsMixin.VIEWS_CSS_RESOURCE);
		assertTrue(css.contains(".juneau-page-nav-sections"), css);
		assertTrue(css.contains(".juneau-page-nav-children"), css);
		assertTrue(css.contains(".juneau-page-nav-cloud"), css);
		assertTrue(css.contains(".juneau-page-nav-section[aria-current=\"page\"]"), css);
		assertTrue(css.contains(".juneau-page-nav-child[aria-current=\"page\"]"), css);
		assertFalse(css.contains("juneau-page-nav-section-selected"), css);
		assertFalse(css.contains("juneau-page-nav-child-selected"), css);
		var childStart = css.indexOf(".juneau-page-nav-child {");
		assertTrue(childStart >= 0, css);
		var childBlock = css.substring(childStart, css.indexOf("}", childStart));
		assertTrue(childBlock.contains("border-radius: 0"),
			() -> "child links must be square text, not pills, block:\n" + childBlock);
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

	private static void assertDeprecated(Class<?> c) {
		assertNotNull(c.getAnnotation(Deprecated.class), () -> c.getName() + " must be @Deprecated");
	}

	private static void assertNoDeprecatedMethods(Class<?> c, String name) {
		for (var m : c.getDeclaredMethods()) {
			if (m.getName().equals(name) && Modifier.isPublic(m.getModifiers()))
				assertNull(m.getAnnotation(Deprecated.class), m::toString);
		}
	}

	private static void assertJavadocSweep(Class<?> c) throws Exception {
		var src = sourceOf(c);
		assertTrue(src.contains("@deprecated"), () -> c.getName() + " javadoc must contain @deprecated:\n" + src);
		assertTrue(src.contains("JuneauViews.regions.mount"), () -> c.getName() + " javadoc must name mount:\n" + src);
		assertTrue(src.contains("grep removal sites"), () -> c.getName() + " javadoc must name the sweep:\n" + src);
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
