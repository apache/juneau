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
import org.junit.jupiter.api.*;

/**
 * Always-on source-shape coverage for {@code juneau-config.js} (TODO-444, slice 2): the client-side async
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

	@Test void b02_decSegment_isStrict_rejectsNonCanonicalInput() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function decSegment(");
		// Lowercase hex, a truncated escape, and a raw unsafe char must all throw 'malformed' - never a lenient decode.
		assertTrue(fn.contains("[0-9A-F]{2}"), fn);
		assertTrue(fn.contains("malformedError("), fn);
	}

	@Test void b03_safeAlphabetIsExactly_A_Za_z0_9_dash_underscore() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function isSafeSegmentChar(");
		assertTrue(fn.contains("ch === \"_\""), fn);
		assertTrue(fn.contains("ch === \"-\""), fn);
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

	@Test void c01_pageIdAttr_matchesPageTableMarkerAttr() throws Exception {
		var body = configJs();
		assertTrue(body.contains("const PAGE_ID_ATTR = \"" + PageTable.MARKER_ATTR + "\";"), body);
	}

	@Test void c02_viewIdAttr_matchesViewTableMarkerAttr() throws Exception {
		var body = configJs();
		assertTrue(body.contains("const VIEW_ID_ATTR = \"" + ViewTable.MARKER_ATTR + "\";"), body);
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

	@Test void d03_localStorageProvider_handlesDanglingActiveAsDefaultPlusNotice() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function resolveActiveAgainstViews(");
		assertTrue(fn.contains("dangling: true"), fn);
		assertTrue(fn.contains("name: null"), fn);
	}

	@Test void d04_localStorageProvider_watchesStorageEventForCrossTabReconcile() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "watchExternalChanges: function (table, onChange) {");
		assertTrue(fn.contains("window.addEventListener(\"storage\""), fn);
		assertTrue(fn.contains("function unwatch()"), fn);
	}

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

	@Test void e02_writesUseCredentialsSameOrigin() throws Exception {
		var body = configJs();
		assertTrue(body.contains("credentials: \"same-origin\""), body);
	}

	@Test void e03_getRequests_arePlainCsrfFreeFetch() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function get(table, path, extraParams) {");
		assertTrue(fn.contains("{ method: \"GET\", credentials: \"same-origin\" }"), fn);
		assertFalse(fn.contains("CsrfToken"), fn);
	}

	@Test void e04_saveAndActivate_usesActivateQueryFlag_neverAFieldInsideTheBlob() throws Exception {
		var region = factoryBody(configJs(), "createServerProvider");
		assertTrue(region.contains("{ name: name, activate: 1 }"), region);
	}

	@Test void e05_setActive_alwaysSendsARealJsonBody_neverEmpty() throws Exception {
		var region = factoryBody(configJs(), "createServerProvider");
		assertTrue(region.contains("name == null ? {} : { name: name }"), region);
	}

	@Test void e06_delete_sendsNameAsQueryParam_neverAsAPathSegment() throws Exception {
		var region = factoryBody(configJs(), "createServerProvider");
		assertTrue(region.contains("write(table, \"DELETE\", \"/item\", { name: name }, {})"), region);
	}

	@Test void e07_baseUrl_resolvedFromClosestShellAttribute_failsClosedWhenAbsent() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function baseFor(table) {");
		assertTrue(fn.contains("resolveSavedViewsBase(table)"), fn);
		assertTrue(fn.contains("unavailableError("), fn);
	}

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

	@Test void g02_schemaVersionMismatch_rejectsAsMalformed_neverCrashes() throws Exception {
		var body = configJs();
		var fn = functionBody(body, "function assertSupportedSchema(");
		assertTrue(fn.contains("malformedError("), fn);
		assertTrue(fn.contains("CURRENT_SCHEMA_VERSION"), fn);
	}

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
