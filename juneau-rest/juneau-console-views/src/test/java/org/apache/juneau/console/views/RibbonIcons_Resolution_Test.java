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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Default-icon lookup + {@code resolveButtonIcon(...)} pure-function assertions for {@code juneau-ribbon.js}
 * (DataTables ribbon icons). Content-substring and
 * function-body-extraction coverage, mirroring {@code ViewsMixin_Serving_Test}'s established idiom.
 */
@SuppressWarnings({
	"resource" // Closeable test fixtures held in static fields; lifecycle managed by the test/framework, not a real leak.
})
class RibbonIcons_Resolution_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cWithMixin = MockRestClient.buildLax(WithMixin.class);

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	@Test void a01_ribbonJs_hasDefaultIconsMapWithAnEntryPerNamedGlyph() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("DEFAULT_ICONS"), body);
		assertTrue(body.contains("copy: \"content_copy\""), body);
		assertTrue(body.contains("csv: \"csv\""), body);
		assertTrue(body.contains("excel: \"table\""), body);
		assertTrue(body.contains("pdf: \"picture_as_pdf\""), body);
		assertTrue(body.contains("refresh: \"refresh\""), body);
		assertFalse(body.contains("columnSearchToggle"), body);
		assertTrue(body.contains("collapse: \"unfold_less\""), body);
		assertTrue(body.contains("dialog: \"new\""), body);
	}

	/**
	 * Naming the row-less dialog trigger's glyph is load-bearing for the same reason the pause control's is (see
	 * {@code TablePolling_Wiring_Test.e06}): an unnamed type falls through to {@code resolveButtonIcon}'s neutral
	 * "tune" default, which paints the settings gear the column chooser already owns - so an icon-only ribbon
	 * carrying both would show two identical gears, one opening a create dialog and one opening the column list.
	 */
	@Test void a03_defaultIcons_dialogDoesNotCollideWithTheColumnChooserGear() throws Exception {
		var ribbon = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var start = ribbon.indexOf("const DEFAULT_ICONS = {");
		var defaults = ribbon.substring(start, ribbon.indexOf("};", start));
		assertTrue(defaults.contains("dialog:"), defaults);
		assertFalse(defaults.contains("dialog: \"tune\""), defaults);
		assertFalse(defaults.contains("dialog: \"settings\""), defaults);
	}

	@ParameterizedTest
	@MethodSource("a02_functionBodyContainsProvider")
	void a02_functionBodyContainsExpectedSubstring(String functionSignature, String expectedSubstring) throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, functionSignature);
		assertTrue(fnBody.contains(expectedSubstring), fnBody);
	}

	/** The positional cycle and the id-hint map are what an unhinted or hinted option resolves from. */
	@Test void a06_optionIconCycleAndHintsAreConcrete() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("const OPTION_ICON_CYCLE = [\"circle_check\", \"hourglass\""), body);
		assertTrue(body.contains("available: \"circle_check\""), body);
		assertTrue(body.contains("mammals: \"paw_print\""), body);
		var fnBody = functionBody(body, "function resolveButtonIcon(");
		assertTrue(fnBody.contains("OPTION_ICON_HINTS[id]"), fnBody);
		assertTrue(fnBody.contains("OPTION_ICON_CYCLE[(index | 0) % OPTION_ICON_CYCLE.length]"), fnBody);
	}

	static Stream<Arguments> a02_functionBodyContainsProvider() {
		return Stream.of(
			// resolveButtonIcon: an explicit symbol wins over the default.
			Arguments.of("function resolveButtonIcon(", ".symbol"),
			// resolveButtonIcon: a custom option with no symbol takes an id hint or a positional cycle glyph.
			Arguments.of("function resolveButtonIcon(", "OPTION_ICON_CYCLE"),
			// ribbonStorageKey: the storage-key layout is unchanged.
			Arguments.of("function ribbonStorageKey(", "\"juneau.view.\" + viewId + \".ribbon.\" + optionId"));
	}

	/** Review fixes: action icon honoured, sibling icons distinct, hidden claims recorded, handles attached. */
	@Test void a07_ribbonJs_reviewFixes() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("resolveButtonIcon(a.icon != null ? { symbol: a.icon } : a, null)"), body);
		assertTrue(body.contains("function assignOptionIcons("), body);
		assertTrue(body.contains("publish: \"push\""), body);
		var build = functionBody(body, "function buildRibbon(");
		assertTrue(build.indexOf("_ribbonOwns[a.type] = true") < build.indexOf("declaredItems.filter(ribbonItemVisible)"), build);
		assertTrue(build.contains("place(og, null)"), build);
		assertFalse(build.contains("curItem = null;\n\t\t\tif (modelOn"), build);
	}

	@Test void a08_modelJs_readsDefaultIdsFromRibbon_noDialog() throws Exception {
		var model = cWithMixin.get(ViewsMixin.MODEL_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(model.contains("NS.ribbon.stockDefaultIds"), model);
		assertFalse(model.contains("RIBBON_DEFAULT_IDS"), model);
		var ribbon = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertFalse(ribbon.contains("\"export\", \"dialog\""), ribbon);
	}

	@Test void a09_iconsJs_registeredStemMissingFromLoadedLayersResolvesNull() throws Exception {
		var icons = cWithMixin.get("/juneau-icons.js").run().assertStatus(200).getContent().asString();
		assertTrue(icons.contains("!_mergedStems.includes(NAME_TO_STEM[key])"), icons);
	}

	@Test void a05_loadPersistedState_routesThroughSyncPersistenceSpi_sameExactKeys() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var load = functionBody(body, "function loadPersistedState(");
		assertTrue(load.contains("storageGet(ribbonStorageKey(viewDef.id, a.id))"), load);
		assertFalse(load.contains("store.getItem("), load);
		assertTrue(load.contains("a.default"), load);
		var persistFn = functionBody(body, "function persist(");
		assertTrue(persistFn.contains("storageSet(ribbonStorageKey(viewDef.id, id), String(value))"), persistFn);
		assertFalse(persistFn.contains("store.setItem("), persistFn);
		var getFn = functionBody(body, "function storageGet(");
		assertTrue(getFn.contains("NS.persistence.getItem(key)"), getFn);
		assertFalse(getFn.contains("Promise"), getFn);
		assertFalse(getFn.contains(".then("), getFn);
		var setFn = functionBody(body, "function storageSet(");
		assertTrue(setFn.contains("NS.persistence.setItem(key, value)"), setFn);
		assertFalse(setFn.contains("Promise"), setFn);
	}
}
