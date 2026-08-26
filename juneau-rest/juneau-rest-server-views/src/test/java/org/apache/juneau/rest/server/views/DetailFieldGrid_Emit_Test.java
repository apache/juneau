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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on coverage for the detail field grid's author-facing surface ({@link FieldLayout}, {@link FieldSpan},
 * {@link DetailSection#count}) and for the stylesheet shape those classes are useless without.
 *
 * <p>
 * What a JVM test can prove here is that the right class or attribute is emitted and that the rule it needs
 * exists in the served stylesheet.  What it cannot prove is that the rule then produces the intended layout - a
 * container query and a cascade need a real engine - so the measurements live in
 * {@link RowDetail_FieldGrid_BrowserTest} and this class deliberately stops short of asserting geometry.
 */
class DetailFieldGrid_Emit_Test extends TestBase {

	private static String viewsCss() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_CSS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String html(DetailSection...sections) {
		return Html.of(ViewTable.of(ViewDef.create("v")
			.dataMode(ViewDef.DataMode.CLIENT)
			.dataUrl("/data")
			.columns(Column.of("id").title("Id"))
			.details(RowDetailDef.create().endpoint("/data/{id}").sections(sections))
			.build()));
	}

	private static DetailSection section(String id) {
		return DetailSection.create(id, id).fields(DetailField.of(id + "-f").title("F"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// FieldLayout / FieldSpan - closed vocabularies the framework translates to a class
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_inlineIsTheDefault_andStackedIsAnExplicitOptIn() {
		assertTrue(html(section("a")).contains("juneau-view-detail-fields-inline"));
		assertTrue(html(section("a").layout(FieldLayout.INLINE)).contains("juneau-view-detail-fields-inline"));
		var stacked = html(section("a").layout(FieldLayout.STACKED));
		assertTrue(stacked.contains("juneau-view-detail-fields-stacked"), stacked);
		assertFalse(stacked.contains("juneau-view-detail-fields-inline"), stacked);
	}

	@Test void a02_columnsIsACapAndIsClampedToTheLaddersWidestStep() {
		assertTrue(html(section("a").columns(1)).contains("juneau-view-detail-fields-cols-1"));
		assertTrue(html(section("a").columns(4)).contains("juneau-view-detail-fields-cols-4"));
		// Above the ladder's widest step the class would name a rule that does not exist, so it clamps.
		var over = html(section("a").columns(12));
		assertTrue(over.contains("juneau-view-detail-fields-cols-4"), over);
		assertFalse(over.contains("juneau-view-detail-fields-cols-12"), over);
	}

	@Test void a03_noInlineGridStyleSurvives() {
		// The whole point of the class: an inline style out-ranks the stylesheet, so a container query could
		// never step the grid down while one was being emitted.
		var out = html(section("a").columns(3));
		assertFalse(out.contains("grid-template-columns"), out);
		assertFalse(out.contains("style=\"grid"), out);
	}

	@Test void a04_spanIsOptIn_andMarkdownSpansWithoutAskingForIt() {
		var plain = html(DetailSection.create("s", "S").fields(DetailField.of("f").title("F")));
		assertFalse(plain.contains("juneau-view-detail-field-span-full"), plain);
		var spanned = html(DetailSection.create("s", "S").fields(DetailField.of("f").title("F").span(FieldSpan.FULL)));
		assertTrue(spanned.contains("juneau-view-detail-field-span-full"), spanned);
		var md = html(DetailSection.create("s", "S")
			.fields(DetailField.of("f").title("F").format(DetailField.Format.MARKDOWN)));
		assertTrue(md.contains("juneau-view-detail-field-span-full"),
			() -> "markdown must span through the SAME mechanism, not a parallel hardcoded rule: " + md);
	}

	//------------------------------------------------------------------------------------------------------------------
	// DetailSection.count
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_countIsOmittedWhenUnset_soASectionThatDeclaresNoneIsByteIdentical() {
		assertFalse(html(section("a")).contains("data-juneau-detail-count"));
	}

	@Test void b02_zeroRenders_becauseCheckedNoneIsInformation() {
		var out = html(section("a").count(0));
		assertTrue(out.contains("data-juneau-detail-count=\"0\""), out);
	}

	@Test void b03_nonZeroRendersItsOwnValue() {
		var out = html(section("a").count(12));
		assertTrue(out.contains("data-juneau-detail-count=\"12\""), out);
	}

	@Test void b04_theClientReadsTheCountAndPaintsItAsItsOwnElement() throws Exception {
		var js = viewsJs();
		assertTrue(js.contains("sec.dataset.juneauDetailCount"),
			() -> "buildDetailStripItem must read the server-stamped count");
		assertTrue(js.contains("function paintDetailStripCounts(tabs, items)"), js);
		assertTrue(js.contains("juneau-view-detail-tab-count"), js);
		// Not folded into the label: the generic builder paints label with textContent, which would put the
		// count inside the button's single text node where CSS cannot reach it.
		assertTrue(js.contains("el.textContent = item.count;"), js);
	}

	//------------------------------------------------------------------------------------------------------------------
	// The stylesheet those classes depend on
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_theQueryContainerIsThePanel_neverTheRoot() throws Exception {
		var css = viewsCss();
		assertTrue(css.contains("container-type: inline-size"), css);
		var rootStart = css.indexOf(":root {");
		assertTrue(rootStart >= 0, css);
		var rootBlock = css.substring(rootStart, css.indexOf("\n}", rootStart));
		assertFalse(rootBlock.contains("container-type"),
			() -> "container-type on :root makes every unnamed @container query resolve against the viewport, "
				+ "which is the signal this panel was chosen to avoid: " + rootBlock);
		var hostStart = css.indexOf(".juneau-view-detail-panel {");
		var hostBlock = css.substring(hostStart, css.indexOf("}", hostStart));
		assertTrue(hostBlock.contains("container-type: inline-size"), hostBlock);
	}

	@Test void c02_theLadderStepsUpwards_narrowestFirst() throws Exception {
		var css = viewsCss();
		var two = css.indexOf("@container (min-width: 560px)");
		var three = css.indexOf("@container (min-width: 800px)");
		var four = css.indexOf("@container (min-width: 1040px)");
		assertTrue(two >= 0 && three >= 0 && four >= 0, css);
		// A wide panel satisfies every step, so the last match wins.  Reversed, every section would pin to two
		// columns at every width - a silent failure that still renders a grid.
		assertTrue(two < three && three < four,
			() -> "container steps must appear narrowest-first or the widest step is overridden");
		assertTrue(css.contains("repeat(4, minmax(0, 1fr))"), css);
	}

	@Test void c03_theFieldBlockCarriesASeparatorWithNoInlineInset() throws Exception {
		var css = viewsCss();
		var start = css.indexOf(".juneau-view-detail-field {");
		assertTrue(start >= 0, css);
		var rule = css.substring(start, css.indexOf("}", start));
		assertTrue(rule.contains("border-bottom: 1px solid"), rule);
		// Inline padding would inset the bottom border with it, and the separator is required to span the full
		// column width including the label half.
		assertTrue(rule.contains("padding: var(--jc-space-1) 0"), rule);
	}

	@Test void c04_anEmptyValueGetsGeneratedContent_notMarkup() throws Exception {
		var css = viewsCss();
		assertTrue(css.contains(".juneau-view-detail-field-value:empty::after"), css);
		// The value div is a client-filled textContent sink, so anything placed INSIDE it is overwritten on the
		// first paint.  ::after survives that by construction.
		assertTrue(css.contains("content: \"\\2014\""), css);
	}

	@Test void c05_theLabelValuePairSpendsTheSharedScale_andIsInverted() throws Exception {
		var css = viewsCss();
		var titleAt = css.indexOf(".juneau-view-detail-field-title {");
		var title = css.substring(titleAt, css.indexOf("}", titleAt));
		assertTrue(title.contains("var(--jc-chrome-font-size-1)"), title);
		assertTrue(title.contains("font-weight: 400"), () -> "the label used to be the heavier of the pair: " + title);
		var valueAt = css.indexOf(".juneau-view-detail-field-value {");
		var value = css.substring(valueAt, css.indexOf("}", valueAt));
		assertTrue(value.contains("var(--jc-chrome-font-size-3)"), value);
		assertTrue(value.contains("overflow-wrap: anywhere"), value);
		assertTrue(title.contains("overflow-wrap: anywhere"), title);
	}

	@Test void c06_fontSize3IsNoLongerProvisional_butItsSiblingsStillAre() throws Exception {
		var css = viewsCss();
		var line = css.lines().filter(x -> x.contains("--jc-chrome-font-size-3:")).findFirst().orElseThrow();
		assertFalse(line.contains("PROVISIONAL"),
			() -> "the detail field value is -font-size-3's first consumer, which fixes it: " + line);
		// The other two markers belong to a different item and must not be swept up.
		assertTrue(css.contains("--jc-chrome-font-size-2: 0.8125rem;          /* PROVISIONAL"), css);
		assertTrue(css.contains("--jc-chrome-line-height: 1.2;                /* PROVISIONAL"), css);
	}

	@Test void c07_theDetailTabsPillOverrideIsGone_butTheBarSlotHostSurvives() throws Exception {
		var css = viewsCss();
		var pill = ".juneau-view-ribbon-group.juneau-view-detail-tabs[data-juneau-strip-mode=\"tab\"]";
		assertFalse(css.contains(pill + " {"), css);
		assertFalse(css.contains(pill + " .juneau-view-ribbon-btn"), css);
		assertTrue(css.contains(pill + "[data-juneau-strip-trailed]"), css);
		// The generic tab-mode shape the detail strip now inherits must still be there.
		assertTrue(css.contains(".juneau-view-ribbon-group[data-juneau-strip-mode=\"tab\"] {"), css);
	}

	@Test void c08_theCountSuffixIsGappedByMarginNotWhitespace() throws Exception {
		var css = viewsCss();
		var start = css.indexOf(".juneau-view-detail-tab-count {");
		assertTrue(start >= 0, css);
		var rule = css.substring(start, css.indexOf("}", start));
		// The tab button is inline-flex, so a leading space in the markup collapses and the tab would read
		// "Suspensions(0)".
		assertTrue(rule.contains("margin-left: var(--jc-space-1)"), rule);
	}
}
