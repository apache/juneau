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
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Always-on source-shape coverage for {@code juneau-config.js} (slice 2): the client-side async
 * persistence SPI, the strict localStorage key codec, and the two first-party persistence providers.
 *
 * <p>
 * Mirrors {@link ViewsJs_RowActions_Test}'s served-script substring style, but reads the asset straight off the
 * classpath ({@link #CONFIG_JS_RESOURCE}) so the persistence SPI can be pinned without going through a mixin route.
 * {@link ViewsMixin} now serves the same bytes at {@link ViewsMixin#CONFIG_JS_PATH} (slice 6).  Behavioral
 * (real-DOM/localStorage/fetch) proof of this contract lives in the opt-in {@code ConfigPersistence_BrowserTest}
 * canary.
 */
class ViewsJs_ConfigPersistence_Test extends TestBase {

	/** Classpath location of the shipped slice-2 persistence layer (mirrors {@code ViewsMixin}'s *_JS_RESOURCE constants). */
	static final String CONFIG_JS_RESOURCE = "/org/apache/juneau/views/juneau-config.js";

	private static String configJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(CONFIG_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + CONFIG_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/**
	 * Extracts a named function/method's body via BALANCED brace matching (from the first {@code {} after
	 * {@code signature} to its true matching close), skipping braces inside string/template literals and
	 * comments.  A plain {@code indexOf("\n\t}", start)} (the sibling {@code ViewsJs_RowActions_Test}/
	 * {@code ViewsMixin_Serving_Test} helper) only works for functions indented exactly one tab; several of this
	 * file's functions/methods are nested two or three tabs deep inside a provider factory, where that shortcut
	 * would overshoot into unrelated trailing code.
	 */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var i = body.indexOf('{', start);
		assertTrue(i >= 0, () -> "'" + signature + "' has no opening brace:\n" + body);
		var depth = 0;
		var j = i;
		for (; j < body.length(); j++) {
			var c = body.charAt(j);
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
				if (depth == 0) { j++; break; }
			} else if (c == '"' || c == '\'' || c == '`') {
				var quote = c;
				j++;
				while (j < body.length() && body.charAt(j) != quote) { if (body.charAt(j) == '\\') j++; j++; }
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '/') {
				while (j < body.length() && body.charAt(j) != '\n') j++;
			} else if (c == '/' && j + 1 < body.length() && body.charAt(j + 1) == '*') {
				j = body.indexOf("*/", j) + 1;
			}
		}
		return body.substring(start, j);
	}

	/**
	 * Extracts an ENTIRE top-level factory function's body (from `function <name>(` to the next `// ====` section
	 * divider) - needed because both provider factories define same-named object-literal methods
	 * (e.g. both have a {@code saveAndActivate:} method), so a plain {@link String#indexOf} for a nested method
	 * signature would silently match the FIRST provider rather than the intended one.
	 */
	private static String factoryBody(String body, String factoryName) {
		var start = body.indexOf("function " + factoryName + "(");
		assertTrue(start >= 0, () -> "'" + factoryName + "' not found:\n" + body);
		var end = body.indexOf("\n\t// ====", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Namespace attachment - extends window.JuneauViews, never replaces it
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_extendsExistingJuneauViewsNamespace_neverReplacesIt() throws Exception {
		var body = configJs();
		assertTrue(body.contains("window.JuneauViews = window.JuneauViews || {}"), body);
		assertTrue(body.contains("NS.config = NS.config || {}"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Key grammar + enc/dec codec (§3.1)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_encSegment_usesUppercasePercentEncoding_notEncodeURIComponent() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function encSegment(");
		assertTrue(fn.contains("hex2Upper(b)"), fn);
		assertFalse(fn.contains("encodeURIComponent"), fn);
	}

	/**
	 * Six independent single-function pinning checks, each extracting one named function's body and asserting it
	 * contains two expected substrings: strict decSegment decoding (lowercase hex / truncated escape / unsafe char
	 * all throw 'malformed'), the exact safe-alphabet chars, the dangling-active default+notice shape, the
	 * cross-tab storage-event watch/unwatch pair, the shell-attribute base-URL resolution with its fail-closed
	 * error, and the schema-version-mismatch fail-closed path.
	 */
	@ParameterizedTest
	@MethodSource("b02_functionBodyContainsTwoSubstringsProvider")
	void b02_functionBodyContainsTwoSubstrings(String functionSignature, String expected1, String expected2) throws Exception {
		var body = configJs();
		var fn = functionBody(body, functionSignature);
		assertTrue(fn.contains(expected1), fn);
		assertTrue(fn.contains(expected2), fn);
	}

	static Stream<Arguments> b02_functionBodyContainsTwoSubstringsProvider() {
		return Stream.of(
			Arguments.of("function decSegment(", "[0-9A-F]{2}", "malformedError("),
			Arguments.of("function isSafeSegmentChar(", "ch === \"_\"", "ch === \"-\""),
			Arguments.of("function resolveActiveAgainstViews(", "dangling: true", "name: null"),
			Arguments.of("watchExternalChanges: function (table, onChange) {",
				"window.addEventListener(\"storage\"", "function unwatch()"),
			Arguments.of("function baseFor(table) {", "resolveSavedViewsBase(table)", "unavailableError("),
			Arguments.of("function assertSupportedSchema(", "malformedError(", "CURRENT_SCHEMA_VERSION"));
	}

	@Test void b04_scopeKey_isPageQualified_soTwoPagesSharingAViewIdCannotCollide() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function scopeKey(");
		assertTrue(fn.contains("\"~\""), fn);
		assertTrue(fn.contains("encSegment(pageId)"), fn);
		assertTrue(fn.contains("encSegment(viewId)"), fn);
	}

	@Test void b05_keyRootAndViewsActiveKeyShape() throws Exception {
		var body = configJs();
		assertTrue(body.contains("const KEY_ROOT = \"juneau.view.\";"), body);
		assertTrue(body.contains(".columns.views."), body);
		assertTrue(body.contains(".columns.active"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) pageId/viewId/base-URL resolution (§3.1/§3.3) - cross-checked against the server-side marker constants
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Three independent single-substring pins that each read the whole served asset directly (no function-body
	 * extraction needed): the pageId/viewId marker attributes cross-checked against their server-side constants,
	 * and the server-provider transport's same-origin credentials mode.
	 */
	@ParameterizedTest
	@MethodSource("c01_bodyContainsSingleSubstringProvider")
	void c01_bodyContainsExpectedSubstring(String expected) throws Exception {
		var body = configJs();
		assertTrue(body.contains(expected), body);
	}

	static Stream<String> c01_bodyContainsSingleSubstringProvider() {
		return Stream.of(
			"const PAGE_ID_ATTR = \"" + PageTable.MARKER_ATTR + "\";",
			"const VIEW_ID_ATTR = \"" + ViewTable.MARKER_ATTR + "\";",
			"credentials: \"same-origin\"");
	}

	@Test void c03_resolvePageId_usesClosestOnThePageIdAttr() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function resolvePageId(");
		assertTrue(fn.contains("table.closest(\"[\" + PAGE_ID_ATTR + \"]\")"), fn);
	}

	@Test void c04_resolveSavedViewsBase_failsClosedWhenShellAbsent() throws Exception {
		var body = configJs();
		assertTrue(body.contains("const SAVED_VIEWS_BASE_ATTR = \"data-juneau-saved-views\";"), body);
		var fn = functionBody(body, "function resolveSavedViewsBase(");
		assertTrue(fn.contains("table.closest(\"[\" + SAVED_VIEWS_BASE_ATTR + \"]\")"), fn);
		assertTrue(fn.contains("isBlank(v) ? null : v"), fn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) localStorage bounds - a SEPARATE, independently-named copy of the server defaults
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_localStorageBounds_areSeparatelyNamedFromAnyServerConstant() throws Exception {
		var body = configJs();
		assertTrue(body.contains("const LOCALSTORAGE_MAX_VIEWS_PER_SCOPE = 50;"), body);
		assertTrue(body.contains("const LOCALSTORAGE_MAX_BLOB_BYTES = 64 * 1024;"), body);
		assertTrue(body.contains("const LOCALSTORAGE_MAX_VIEWS_PER_USER = 500;"), body);
		assertTrue(body.contains("const MAX_NAME_LEN = 128;"), body);
		assertTrue(body.contains("const MAX_ENCODED_SEGMENT_LEN = 512;"), body);
	}

	@Test void d02_localStorageProvider_enforcesPerScopeAndAggregateCaps() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function enforceBounds(");
		assertTrue(fn.contains("quotaError("), fn);
		assertTrue(fn.contains("LOCALSTORAGE_MAX_VIEWS_PER_SCOPE"), fn);
		assertTrue(fn.contains("countAllViewsForThisUser()"), fn);
		assertTrue(fn.contains("LOCALSTORAGE_MAX_VIEWS_PER_USER"), fn);
	}

	// d03/d04 (dangling-active default+notice; cross-tab storage watch/unwatch) are covered by b02 above,
	// alongside four other structurally-identical single-function two-substring pins.

	//------------------------------------------------------------------------------------------------------------------
	// e) Server-persisted provider transport envelope (§3.3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_writeRequest_usesJsonContentTypeAndTheSharedCsrfHelpers_failsClosedOnBlankToken() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function writeRequest(");
		assertTrue(fn.contains("\"Content-Type\": \"application/json\""), fn);
		assertTrue(fn.contains("init.resolveCsrfToken(table)"), fn);
		assertTrue(fn.contains("init.resolveCsrfHeaderName(table)"), fn);
		assertTrue(fn.contains("init.isBlankToken(token)"), fn);
		assertTrue(fn.contains("refuse: true"), fn);
	}

	// e02 (same-origin credentials) is covered by c01 above, alongside two other single-substring pins.

	@Test void e03_getRequests_arePlainCsrfFreeFetch() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function get(table, path, extraParams) {");
		assertTrue(fn.contains("{ method: \"GET\", credentials: \"same-origin\" }"), fn);
		assertFalse(fn.contains("CsrfToken"), fn);
	}

	/**
	 * Three independent server-provider-region pins: saveAndActivate uses the {@code activate} query flag (never a
	 * blob field), setActive always sends a real JSON body (never empty), and delete sends the name as a query
	 * param (never a path segment).
	 */
	@ParameterizedTest
	@MethodSource("e04_serverProviderRegionContainsSubstringProvider")
	void e04_serverProviderRegionContainsExpectedSubstring(String expected) throws Exception {
		var region = factoryBody(configJs(), "createServerProvider");
		assertTrue(region.contains(expected), region);
	}

	static Stream<String> e04_serverProviderRegionContainsSubstringProvider() {
		return Stream.of(
			"{ name: name, activate: 1 }",
			"name == null ? {} : { name: name }",
			"write(table, \"DELETE\", \"/item\", { name: name }, {})");
	}

	// e07 (shell-attribute base-URL resolution, fail-closed) is covered by b02 above.

	@Test void e08_serverProvider_neverEmitsHttpForCrossTabReconcile() throws Exception {
		// The server provider's own object literal must not DEFINE watchExternalChanges (that method-def token,
		// not a mere substring match, since the region's own comment explaining the omission legitimately
		// contains the plain word "watchExternalChanges").
		var region = factoryBody(configJs(), "createServerProvider");
		assertFalse(region.contains("watchExternalChanges: function"), region);
		assertTrue(region.contains("storage` event never fires for an HTTP write"), region);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) The seven-method async SPI + provider-selection seam (§3.2/§5)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_persistenceFacadeExposesTheSevenMethodContract() throws Exception {
		var body = configJs();
		assertTrue(body.contains("NS.persistence = {"), body);
		for (var method : new String[]{"list:", "load:", "save:", "saveAndActivate:", "setActive:", "\"delete\":", "getActive:"})
			assertTrue(body.contains(method), () -> "missing SPI method '" + method + "':\n" + body);
	}

	@Test void f02_bothProviderFactoriesImplementAllSevenMethods() throws Exception {
		var body = configJs();
		for (var factory : new String[]{"createLocalStorageProvider", "createServerProvider"}) {
			var region = factoryBody(body, factory);
			for (var method : new String[]{"list:", "load:", "save:", "saveAndActivate:", "setActive:", "\"delete\":"})
				assertTrue(region.contains(method), () -> factory + " missing '" + method + "':\n" + region);
		}
	}

	@Test void f03_providerSelectionSeam_defaultsToLocalStorage_swappableViaSetPersistenceProvider() throws Exception {
		var body = configJs();
		assertTrue(body.contains("NS.setPersistenceProvider = function (provider) {"), body);
		assertTrue(body.contains("NS.persistenceProviders = {"), body);
		assertTrue(body.contains("localStorage: createLocalStorageProvider"), body);
		assertTrue(body.contains("server: createServerProvider"), body);
		var fn = functionBody(body, "function activeProvider() {");
		assertTrue(fn.contains("createLocalStorageProvider()"), fn);
	}

	@Test void f04_getActive_isDerivedUniformlyFromListRatherThanDuplicatedPerProvider() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "getActive: function (table) {");
		assertTrue(fn.contains("NS.persistence.list(table)"), fn);
	}

	@Test void f05_syncGetItemSetItem_areSynchronous_notPromises() throws Exception {
		var body = configJs();
		assertTrue(body.contains("NS.persistence.getItem = function (key)"), body);
		assertTrue(body.contains("NS.persistence.setItem = function (key, value)"), body);
		var getFn = functionBody(body, "NS.persistence.getItem = function (key) {");
		assertTrue(getFn.contains("window.localStorage.getItem(key)"), getFn);
		assertFalse(getFn.contains("Promise"), getFn);
		assertFalse(getFn.contains(".then("), getFn);
		var setFn = functionBody(body, "NS.persistence.setItem = function (key, value) {");
		assertTrue(setFn.contains("window.localStorage.setItem(key, String(value))"), setFn);
		assertFalse(setFn.contains("Promise"), setFn);
		assertFalse(setFn.contains(".then("), setFn);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Typed error shapes (§3.2) - 'conflict' was dropped in round-2; must not resurface here
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_typedErrorCodes_areExactlyTheFourFrozenCodes_conflictNeverResurfaces() throws Exception {
		var body = configJs();
		for (var code : new String[]{"\"malformed\"", "\"quota\"", "\"unavailable\"", "\"network\""})
			assertTrue(body.contains(code), () -> "missing typed error code " + code + ":\n" + body);
		assertFalse(body.contains("\"conflict\""), body);
	}

	// g02 (schema-version-mismatch fail-closed) is covered by b02 above.

	//------------------------------------------------------------------------------------------------------------------
	// h) Reserved-name rule (shared by both providers, so slice 3's mixin must enforce the identical rule)
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_defaultIsReservedCaseInsensitively() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function isReservedName(");
		assertTrue(fn.contains(".toLowerCase() === \"default\""), fn);
	}

	@Test void h02_validateNameBasic_isSharedByBothProviders_notJustLocalStorage() throws Exception {
		// The server provider's load/save/saveAndActivate/setActive/delete all call validateNameBasic (not the
		// localStorage-only validateNameForLocalStorage) - proves the two providers share ONE name-validity rule.
		var region = factoryBody(configJs(), "createServerProvider");
		var occurrences = region.split("validateNameBasic\\(name\\)", -1).length - 1;
		assertTrue(occurrences >= 4, () -> "expected validateNameBasic(name) called at least 4x in the server provider, found " + occurrences + ":\n" + region);
		assertFalse(region.contains("validateNameForLocalStorage"), region);
	}
}
