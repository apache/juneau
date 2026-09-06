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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Source-shape purity scan for {@code juneau-helpers.js} (WORK-J0522b, design §9.2 / test 29).
 *
 * <p>
 * Design §9.2's invariants for the helper library:
 * <ul>
 * <li>Pure data -&gt; DOM: no {@code fetch}, no message-bus reference, no page-global mutable state.
 * <li>The one amendment (SD-3): a helper MAY hold per-instance state confined to the node it returned (tabStrip's
 * fill-once bookkeeping), living in a closure captured by that node's own event listeners - never in a
 * module-level {@code Map}/{@code Set}/{@code WeakMap}/counter.
 * <li>No HTML-string sink, ever (R16), except {@code icon()}, which paints TRUSTED icon-registry sprite markup
 * only (checked structurally in {@link RawContentSink_SecurityScan_Test}, not re-checked here).
 * <li>Themable by class, not by inline style - the one exception being {@code fieldGrid}'s {@code columns} option,
 * which is a CSS custom property, not a {@code grid-template-columns} inline style.
 * </ul>
 *
 * <p>
 * This is a static source-text scan (like {@code RawContentSink_SecurityScan_Test}'s HTML-sink checks), not a
 * behavioral one - it is what lets an accidental module-level counter or a stray {@code WeakMap} fail this suite
 * even on a code path the behavioral harness in {@link Helpers_Test} happens not to reach.
 */
class Helpers_Purity_Test extends TestBase {

	private static String source() throws Exception {
		return HelpersHarness.helpersJs();
	}

	// =================================================================================================================
	// No page-global mutable state: no WeakMap/WeakSet/Map/Set at module scope, no assignment to a NS/window/
	// document/globalThis member other than the single `NS.helpers = {...}` export block.
	// =================================================================================================================

	@Test void a01_noWeakMapOrWeakSet() throws Exception {
		var src = source();
		// Checks for actual CONSTRUCTION, not the bare identifier: the file's own header comment names
		// WeakMap/WeakSet as the thing this invariant forbids (see the doc comment above this class), so a
		// bare `contains("WeakMap")` would fail on that comment text alone rather than on real usage.
		assertFalse(src.contains("new WeakMap("), "juneau-helpers.js must not use WeakMap for per-instance state - "
			+ "fill-once bookkeeping must live in a closure, not a module-level identity map: " + src);
		assertFalse(src.contains("new WeakSet("), "juneau-helpers.js must not use WeakSet: " + src);
	}

	/**
	 * No module-level counter/registry: the ONLY {@code new Map()}/{@code new Set()} construction allowed anywhere
	 * in the file is none at all - {@code tabStrip}'s uniqueness needs are met with a per-call random suffix
	 * (see {@code a03}), and every other helper's bookkeeping (the field-action bar, the filter-builder's
	 * predicate list) is a plain array or object living in that call's own closure, never shared across calls.
	 */
	@Test void a02_noModuleLevelMapOrSetConstruction() throws Exception {
		var src = source();
		assertFalse(src.contains("new Map("), "no module-level registry: " + src);
		assertFalse(src.contains("new Set("), "no module-level registry: " + src);
	}

	/** {@code tabStrip} mints its per-instance DOM id namespace from a per-call random suffix, not a module counter. */
	@Test void a03_tabStripInstanceId_isPerCallRandomNotAModuleCounter() throws Exception {
		var src = source();
		assertTrue(src.contains("Math.random().toString(36)"), src);
		// A module-level counter would show up as a `let`/`var` declaration OUTSIDE any function, incremented on
		// every call.  The file's only top-level mutable bindings are `NS` and `I` (both assigned once, from
		// juneau-views.js's own already-published globals) - neither is a counter or a registry.
		assertFalse(src.contains("Seq = 0") || src.contains("Seq=0"), "no module-level sequence counter: " + src);
	}

	/**
	 * The ONLY assignment to an {@code NS.*} member anywhere in the file is the single, one-time export block at
	 * the bottom ({@code NS.helpers = {...}}).  {@code fieldGrid}'s scoped-renderer-override mechanism calls the
	 * existing public {@code NS.registerRenderer}/{@code NS.resolveRenderer} FUNCTIONS (a call, not a member
	 * assignment) to swap and restore a registry entry within a single synchronous call - so it never appears as
	 * an {@code NS.<identifier> =} pattern.
	 */
	@Test void a04_onlyOneNsMemberAssignment_theHelpersExport() throws Exception {
		var src = source();
		var assignments = src.split("\\bNS\\.[A-Za-z_$][\\w$]*\\s*=(?!=)", -1).length - 1;
		assertEquals(1, assignments, "juneau-helpers.js must assign exactly one NS.* member (NS.helpers) - found "
			+ assignments + " occurrences of `NS.<member> =`:\n" + src);
		assertTrue(src.contains("NS.helpers = {"), src);
	}

	/** {@code fieldGrid}'s renderer-override swap goes through the EXISTING public functions, never a direct write. */
	@Test void a05_scopedRendererOverride_usesPublicFunctionsNotDirectRegistryWrite() throws Exception {
		var src = source();
		assertTrue(src.contains("NS.registerRenderer(renderId, override);"), src);
		assertTrue(src.contains("NS.registerRenderer(renderId, prior);"), src);
		assertFalse(src.contains("NS._renderers"), "must go through the public registerRenderer/resolveRenderer "
			+ "functions, never touch the private _renderers registry directly: " + src);
	}

	/**
	 * {@code window.*} appears in exactly two spots: the one-time namespace bootstrap
	 * ({@code var NS = window.JuneauViews = window.JuneauViews || {};}) and {@code icon()}'s read-only lookup of
	 * the icon registry ({@code window.JuneauViews && window.JuneauViews.icons}).  Neither is a page-global
	 * WRITE this file performs on its own account - the bootstrap assigns the same {@code JuneauViews} object
	 * juneau-views.js already created (a no-op if it exists), and the icon lookup only reads.  No OTHER
	 * {@code window.<member> =} assignment, and no {@code globalThis}/{@code document.<member> =} assignment,
	 * appears anywhere in the file.
	 */
	@Test void a06_noAssignmentToWindowGlobalThisOrDocumentMembers() throws Exception {
		var src = source();
		var windowAssignments = new java.util.regex.Matcher[]{
			java.util.regex.Pattern.compile("window\\.[A-Za-z_$][\\w$]*\\s*=(?!=)").matcher(src)};
		var count = 0;
		while (windowAssignments[0].find())
			count++;
		assertEquals(1, count, "the only window.<member> = assignment allowed is the namespace bootstrap "
			+ "(`window.JuneauViews = window.JuneauViews || {}`); found " + count + ":\n" + src);
		assertFalse(src.contains("globalThis"), src);
		assertFalse(src.matches("(?s).*\\bdocument\\.[A-Za-z_$][\\w$]*\\s*=(?!=).*"),
			"document.<member> = ASSIGNMENT is forbidden (document.createElement/createTextNode CALLS are fine and "
				+ "are not this pattern - it requires an `=` after the member name): " + src);
	}

	// =================================================================================================================
	// No fetch, no message-bus reference: this is a paint library, not a data layer.
	// =================================================================================================================

	@Test void b01_noFetchCall() throws Exception {
		var src = source();
		assertFalse(src.contains("fetch("), "juneau-helpers.js must never call fetch itself - dataPane's `load` "
			+ "is an author-supplied thunk: " + src);
	}

	@Test void b02_noMessageBusReference() throws Exception {
		var src = source();
		for (var busTerm : new String[]{"ctx.on(", "ctx.emit(", "ctx.refresh(", "postMessage", "BroadcastChannel"})
			assertFalse(src.contains(busTerm), "juneau-helpers.js must not reference the message bus (" + busTerm
				+ "): " + src);
	}

	// =================================================================================================================
	// Themable by class, not inline style - fieldGrid's `columns` is the one documented exception, and it is a
	// CSS custom property, never a `grid-template-columns` inline style.
	// =================================================================================================================

	@Test void c01_noStyleDisplayOrOtherInlineStyleAssignment() throws Exception {
		var src = source();
		assertFalse(src.contains(".style.display"), src);
		assertFalse(src.contains(".style.gridTemplateColumns"), "columns must be a CSS custom property, never an "
			+ "inline grid-template-columns: " + src);
	}

	@Test void c02_columnsIsACssCustomProperty() throws Exception {
		var src = source();
		assertTrue(src.contains("grid.style.setProperty(\"--juneau-view-detail-columns\""), src);
	}

	// =================================================================================================================
	// Reuse, not reimplementation: fieldGrid paints through the SAME juneau-views.js copiers paintDetailFieldSlot
	// uses, so an overridden or globally-registered renderer gets identical sanitize-and-copy discipline.
	// =================================================================================================================

	@Test void d01_fieldGrid_reusesExistingCopiers_doesNotReimplementThem() throws Exception {
		var src = source();
		assertTrue(src.contains("I.fillMarkdownSlot("), src);
		assertTrue(src.contains("I.fillSanitizedHtmlSlot("), src);
		assertTrue(src.contains("I.fillRenderSlot("), src);
		assertTrue(src.contains("I.scalarFieldValue("), src);
		assertTrue(src.contains("I.renderAsyncStatus("), src);
		assertTrue(src.contains("I.isSafeDetailUrl("), src);
	}

	// =================================================================================================================
	// Loud rather than silent: the file refuses to load ahead of juneau-views.js, rather than building a
	// blank/broken helpers object.
	// =================================================================================================================

	@Test void e01_refusesToLoadWithoutViewsJs() throws Exception {
		var src = source();
		assertTrue(src.contains("if (!NS.init)"), src);
		assertTrue(src.contains("throw new Error("), src);
	}
}
