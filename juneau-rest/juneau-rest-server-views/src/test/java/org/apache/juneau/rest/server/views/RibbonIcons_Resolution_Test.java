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
 * (DataTables ribbon visual-parity design doc §4.A, plan Task 2). Option-A (content-substring +
 * function-body-extraction) coverage, mirroring {@code ViewsMixin_Serving_Test}'s established idiom.
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

	@Test void a01_ribbonJs_hasDefaultIconsMapWithAllSevenEntries() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("DEFAULT_ICONS"), body);
		assertTrue(body.contains("copy: \"content_copy\""), body);
		assertTrue(body.contains("csv: \"csv\""), body);
		assertTrue(body.contains("excel: \"table\""), body);
		assertTrue(body.contains("pdf: \"picture_as_pdf\""), body);
		assertTrue(body.contains("refresh: \"refresh\""), body);
		assertTrue(body.contains("columnSearchToggle: \"manage_search\""), body);
		assertTrue(body.contains("collapse: \"unfold_less\""), body);
	}

	@ParameterizedTest
	@MethodSource("a02_functionBodyContainsProvider")
	void a02_functionBodyContainsExpectedSubstring(String functionSignature, String expectedSubstring) throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var fnBody = functionBody(body, functionSignature);
		assertTrue(fnBody.contains(expectedSubstring), fnBody);
	}

	static Stream<Arguments> a02_functionBodyContainsProvider() {
		return Stream.of(
			// resolveButtonIcon: an explicit symbol wins over the default.
			Arguments.of("function resolveButtonIcon(", ".symbol"),
			// resolveButtonIcon: a custom option with no symbol falls back to "tune".
			Arguments.of("function resolveButtonIcon(", "\"tune\""),
			// ribbonStorageKey: the storage-key layout is unchanged.
			Arguments.of("function ribbonStorageKey(", "\"juneau.view.\" + viewId + \".ribbon.\" + optionId"));
	}

	@Test void a05_loadPersistedState_routesThroughSyncPersistenceSpi_sameExactKeys() throws Exception {
		var body = cWithMixin.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		var load = functionBody(body, "function loadPersistedState(");
		assertTrue(load.contains("storageGet(ribbonStorageKey(viewDef.id, a.id))"), load);
		assertFalse(load.contains("store.getItem("), load);
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
