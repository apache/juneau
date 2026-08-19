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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * Regression barrier for the panel-visibility contract that decides whether a <b>sub-tabbed</b> tab renders at all.
 *
 * <p>
 * A tab declaring {@link Tab#subtabs} is emitted as two nested levels: an outer {@link PageTable#PANEL_CLASS} panel
 * (tab-scoped only) wrapping the sub-tab bar plus one {@link PageTable#SUBPANEL_CLASS} per sub-tab (tab- <i>and</i>
 * sub-tab-scoped).  Three independently-editable artifacts have to agree for that to be visible, and a mismatch
 * between any two of them renders the entire tab <b>blank</b> while leaving leaf tabs working perfectly &mdash; which
 * is precisely how such a mismatch escapes a leaf-tab-only test suite:
 * <ol class='spaced-list'>
 * 	<li><b>{@code juneau-views.css}</b> hides {@code .jc-panel}/{@code .jc-subpanel} until {@code .jc-active} is
 * 		added, so the outer panel <i>must</i> receive {@code .jc-active} or its descendants stay invisible no matter
 * 		what classes they carry.
 * 	<li><b>{@link PageTable}</b> deliberately emits the outer panel with {@link PageTable#PANEL_TAB_ATTR} and
 * 		<i>no</i> {@link PageTable#PANEL_SUBTAB_ATTR} (it must be visible for every one of its sub-tabs; a static
 * 		attribute could only name one).
 * 	<li><b>{@code juneau-pages.js}</b>'s {@code panelMatches} must therefore treat a missing
 * 		{@link PageTable#PANEL_SUBTAB_ATTR} as "any sub-tab" rather than demanding an exact match.
 * </ol>
 *
 * <h5 class='section'>Division of labour with the browser harness:</h5>
 * <p>
 * Points 1 and 2 are asserted here, directly against the served CSS and the emitted markup, and need nothing but a
 * JVM.  Point 3 is a claim about <i>runtime behavior</i>, so it is proven by executing the runtime in a real browser
 * in {@link PagePanelVisibility_BrowserTest} rather than by pattern-matching the script's source &mdash; a substring
 * assertion can tell you the code still <i>looks</i> right while the page renders blank.
 * <p>
 * What this class keeps for the runtime side is the one thing a behavioural test cannot cover: the attribute and
 * class <b>names</b> are shared across a Java/JavaScript boundary that no compiler checks, and the browser harness is
 * opt-in, so a rename that never reached {@code juneau-pages.js} would sail through a default build and blank every
 * panel at runtime.  Section {@code c} pins those spellings so that mismatch fails a build instead.
 *
 * <h5 class='section'>Which names are constants, and which are not:</h5>
 * <p>
 * Every name the <i>emitter</i> writes and the runtime reads is a {@link PageTable} constant, because Java is one of
 * the two parties and a constant is how one party states the name once.  The active-state classes
 * ({@code .jc-tab-active}, {@code .jc-subtab-active}, {@code .jc-active}) deliberately are <b>not</b>: the emitter
 * never writes them, so a public Java constant for them would publish API surface for a string this module's Java
 * never produces.  They are still a two-artifact contract &mdash; {@code juneau-pages.js} sets them and
 * {@code juneau-views.css} styles them &mdash; so they get the same protection a different way, in {@code c03}.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class PageTable_SubtabPanelContract_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends org.apache.juneau.rest.server.servlet.BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	public static class Release {
		public String name;
	}

	private static ViewDef view(String id) {
		return ViewDef.create(id)
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/" + id + "/data")
			.columns(Column.of("name").title("Name"))
			.build();
	}

	/** A page mixing a leaf tab with a sub-tabbed tab - the leaf tab is the case that never broke. */
	private static PageDef pageWithSubtabs() {
		return PageDef.create("admin")
			.tabs(
				Tab.create("releases", "Releases").view(view("releases")),
				Tab.create("catalog", "Catalog").subtabs(
					Subtab.create("packages", "Packages").view(view("packages")),
					Subtab.create("bundles", "Bundles").view(view("bundles"))))
			.build();
	}

	/** Every {@code <div ...>} start tag in the markup, so a panel's OWN attributes can be inspected in isolation. */
	private static List<String> divTags(String html) {
		var l = new ArrayList<String>();
		var i = 0;
		while ((i = html.indexOf("<div", i)) >= 0) {
			var end = html.indexOf('>', i);
			assertTrue(end >= 0, () -> "unterminated <div in:\n" + html);
			l.add(html.substring(i, end + 1));
			i = end + 1;
		}
		return l;
	}

	/** The single {@code <div>} start tag containing all of {@code required}; fails when not exactly one matches. */
	private static String theDivTag(String html, String...required) {
		var matches = divTags(html).stream().filter(x -> Arrays.stream(required).allMatch(x::contains)).toList();
		assertEquals(1, matches.size(),
			() -> "expected exactly one <div> tag containing " + Arrays.toString(required) + ", found " + matches + " in:\n" + html);
		return matches.get(0);
	}

	/** {@code data-panel-tab="<id>"} as it appears in the markup, spelled from the emitter's own constant. */
	private static String tabAttr(String id) {
		return PageTable.PANEL_TAB_ATTR + "=\"" + id + "\"";
	}

	/** {@code data-panel-subtab="<id>"}, likewise. */
	private static String subtabAttr(String id) {
		return PageTable.PANEL_SUBTAB_ATTR + "=\"" + id + "\"";
	}

	//------------------------------------------------------------------------------------------------------------------
	// a: the emitted markup side of the contract
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_subtabbedTabOuterPanelIsTabScopedOnly() {
		// The load-bearing emitter invariant: the outer panel wrapping a sub-tab bar must NOT be pinned to one
		// sub-tab, because it has to stay visible for all of them.
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		var tag = theDivTag(html, PageTable.PANEL_CLASS + "\"", tabAttr("catalog"));
		assertFalse(tag.contains(PageTable.PANEL_SUBTAB_ATTR),
			() -> "the outer .jc-panel of a sub-tabbed tab must stay sub-tab-agnostic (adding data-panel-subtab pins the whole tab to one sub-tab and blanks it for the others): " + tag);
	}

	@Test void a02_eachSubpanelIsTabAndSubtabScoped() {
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		for (var id : List.of("packages", "bundles")) {
			var tag = theDivTag(html, PageTable.SUBPANEL_CLASS, subtabAttr(id));
			assertTrue(tag.contains(tabAttr("catalog")),
				() -> "a .jc-subpanel must also carry its parent tab id, so it can never activate under another tab: " + tag);
		}
	}

	@Test void a03_leafTabPanelIsAlsoSubtabAgnostic() {
		// Leaf and sub-tabbed outer panels share one rule ("no data-panel-subtab means any sub-tab"), which is why a
		// leaf-tab-only suite exercises the rule but never the sub-tabbed case.
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		var tag = theDivTag(html, PageTable.PANEL_CLASS + "\"", tabAttr("releases"));
		assertFalse(tag.contains(PageTable.PANEL_SUBTAB_ATTR), tag);
	}

	@Test void a04_subpanelsAreNestedInsideTheirTabsOuterPanel() {
		// Nesting is what makes the outer panel's visibility a precondition for the sub-panels' visibility.
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		var outer = html.indexOf(theDivTag(html, PageTable.PANEL_CLASS + "\"", tabAttr("catalog")));
		var sub = html.indexOf(theDivTag(html, PageTable.SUBPANEL_CLASS, subtabAttr("packages")));
		assertTrue(outer >= 0 && sub > outer, () -> "expected .jc-subpanel nested after its outer .jc-panel:\n" + html);
	}

	@Test void a05_pageMetaGivesTheSubtabbedTabANonEmptySubtabList() {
		// This is what makes resolveInitial(...) hand showActive(...) a NON-NULL subtabId for this tab - the
		// precondition under which an exact-match panel rule would blank the outer panel.
		var html = Html.of(PageTable.of(pageWithSubtabs()));
		var open = html.indexOf("id=\"" + PageTable.SIDECAR_ID_PREFIX + "admin\"");
		assertTrue(open >= 0, () -> "PAGE_META sidecar not found:\n" + html);
		var start = html.indexOf('>', open) + 1;
		var meta = Json.to(html.substring(start, html.indexOf("</script>", start)), Map.class);
		var catalog = (Map<?,?>) ((List<?>) meta.get("tabs")).get(1);
		assertEquals("catalog", catalog.get("id"));
		assertEquals(2, ((List<?>) catalog.get("subtabs")).size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b: the CSS side of the contract
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_panelsAreHiddenUntilTheRuntimeAddsJcActive() throws Exception {
		// Why the outer panel's .jc-active matters: without it the whole subtree - sub-tab bar included - is
		// display:none, regardless of any class the runtime put on the sub-panel inside it.
		var css = c.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString().replaceAll("\\s+", " ");
		assertTrue(css.contains(".jc-panel, .jc-subpanel { display: none; }"), css);
		assertTrue(css.contains(".jc-panel.jc-active, .jc-subpanel.jc-active { display: block; }"), css);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c: the Java-to-JavaScript name correspondence  (the half no compiler and no behavioural test can check)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_theRuntimeReadsEveryAttributeNameTheEmitterWrites() throws Exception {
		// Built from the constants on purpose: rename any of these without mirroring the new spelling in
		// juneau-pages.js and this fails, instead of the runtime silently failing to recognize what it is handed -
		// which for a panel attribute means display:none forever, and for a tab-bar attribute means a tab that never
		// reads as selected.
		var js = pagesJs();
		for (var attr : List.of(PageTable.PANEL_TAB_ATTR, PageTable.PANEL_SUBTAB_ATTR, PageTable.TAB_ID_ATTR, PageTable.SUBTAB_ID_ATTR, PageTable.PARENT_TAB_ATTR))
			assertTrue(js.contains("getAttribute(\"" + attr + "\")"),
				() -> "juneau-pages.js does not read '" + attr + "' - the emitter and the runtime have drifted apart, and an attribute the runtime cannot recognize is one it silently ignores:\n" + js);
	}

	@Test void c02_theRuntimeSelectsTheSamePanelClassNamesTheEmitterWrites() throws Exception {
		// Same irreducible duplication, other half: the runtime's PANEL_SELECTOR names both panel levels literally.
		var js = pagesJs();
		assertTrue(js.contains("\"." + PageTable.PANEL_CLASS + ", ." + PageTable.SUBPANEL_CLASS + "\""),
			() -> "juneau-pages.js must select both emitted panel levels; dropping either one re-hides it:\n" + js);
	}

	@Test void c03_theStylesheetStylesTheExactActiveClassNamesTheRuntimeToggles() throws Exception {
		// The active-state class names are the one part of this contract with no Java side at all: the emitter never
		// writes them, the runtime adds them and the stylesheet reacts to them.  So there is no constant to build
		// these from - the pin is this test naming each one once and requiring both artifacts to agree, which is
		// exactly the drift a rename in either file would otherwise cause (a class nothing styles, or a style
		// nothing sets - both render as a page that quietly stops responding to clicks).
		var js = pagesJs();
		var css = viewsCss();
		for (var cls : List.of("jc-tab-active", "jc-subtab-active", "jc-active")) {
			assertTrue(js.contains("classList.toggle(\"" + cls + "\""),
				() -> "juneau-pages.js no longer toggles '" + cls + "', but juneau-views.css still styles it:\n" + js);
			assertTrue(css.contains("." + cls),
				() -> "juneau-views.css does not style '." + cls + "', so the class juneau-pages.js sets has no visible effect:\n" + css);
		}
	}

	private static String pagesJs() throws Exception {
		return c.get(ViewsMixin.PAGES_JS_PATH).run().assertStatus(200).getContent().asString();
	}

	private static String viewsCss() throws Exception {
		return c.get(ViewsMixin.VIEWS_CSS_PATH).run().assertStatus(200).getContent().asString();
	}
}
