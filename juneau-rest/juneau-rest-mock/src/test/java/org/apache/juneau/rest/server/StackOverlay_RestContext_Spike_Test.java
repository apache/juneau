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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.junit.jupiter.api.*;

/**
 * Validates that wedging a {@link StackOverlay} into the
 * {@code overridingParent} slot of a {@link BasicBeanStore} preserves the existing
 * resolution semantics that {@code RestContext_Precedence_Test} exercises.
 *
 * <p>
 * The framework {@link RestContext} integration of {@link StackOverlay} happens in later
 * phases.  This spike exercises the composition shape directly: a {@link BasicBeanStore}
 * whose {@code overridingParent} is a {@link StackOverlay} should behave identically to
 * one whose {@code overridingParent} is a single {@link BasicBeanStore} (as in the existing
 * Spring bridge), while supporting push/pop layering on top.
 *
 * <p>
 * <b>Go/no-go gate:</b> if any of the assertions below fail, the composition path is
 * non-trivial and the design should fall back to OQ4 option (a) — replace the slot with
 * a stack — per the plan's Phase 0 fallback gate.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class StackOverlay_RestContext_Spike_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final class Svc {
		final String tag;
		Svc(String tag) { this.tag = tag; }
		@Override public String toString() { return "Svc[" + tag + "]"; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — empty StackOverlay in overridingParent slot behaves like null overriding parent
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_emptyOverlay_doesNotShadowLocalEntries() {
		var local = new Svc("local");
		var overlay = new StackOverlay();
		var store = new BasicBeanStore(null, overlay).addBean(Svc.class, local);

		// With an empty stack overlay, lookup falls through to local entries — the same behavior
		// as if overridingParent were null.
		assertSame(local, store.getBean(Svc.class).get());
	}

	@Test
	void a02_emptyOverlay_fallsThroughToParent() {
		var fromParent = new Svc("from-parent");
		var parent = new BasicBeanStore().addBean(Svc.class, fromParent);
		var overlay = new StackOverlay();
		var store = new BasicBeanStore(parent, overlay);

		// Empty stack overlay should fall through to local entries (none here) and then to parent.
		assertSame(fromParent, store.getBean(Svc.class).get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — pushed frame wins over local entries (the whole point of overridingParent precedence)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_pushedFrame_winsOverLocalEntry() {
		var localBean = new Svc("local");
		var overrideBean = new Svc("override");

		var overlay = new StackOverlay();
		var store = new BasicBeanStore(null, overlay).addBean(Svc.class, localBean);

		overlay.push(new BasicBeanStore().addBean(Svc.class, overrideBean));
		assertSame(overrideBean, store.getBean(Svc.class).get(),
			"Pushed override frame must win over the resource's local entry");

		overlay.pop();
		assertSame(localBean, store.getBean(Svc.class).get(),
			"After popping, local entry should be visible again");
	}

	@Test
	void b02_pushedFrame_winsOverParentToo() {
		var parentBean = new Svc("from-parent");
		var overrideBean = new Svc("override");

		var parent = new BasicBeanStore().addBean(Svc.class, parentBean);
		var overlay = new StackOverlay();
		var store = new BasicBeanStore(parent, overlay);

		overlay.push(new BasicBeanStore().addBean(Svc.class, overrideBean));
		assertSame(overrideBean, store.getBean(Svc.class).get());

		overlay.pop();
		assertSame(parentBean, store.getBean(Svc.class).get());
	}

	@Test
	void b03_pushedFrame_winsOverDefaultSupplier() {
		var defaultBean = new Svc("default");
		var overrideBean = new Svc("override");

		var overlay = new StackOverlay();
		var store = new BasicBeanStore(null, overlay).addDefaultSupplier(Svc.class, () -> defaultBean);

		overlay.push(new BasicBeanStore().addBean(Svc.class, overrideBean));
		assertSame(overrideBean, store.getBean(Svc.class).get());

		overlay.pop();
		assertSame(defaultBean, store.getBean(Svc.class).get(), "Default supplier resolves after popping");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — multi-frame stack
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_topFrame_winsOverBottomFrame_inOverridingParentSlot() {
		var bottom = new Svc("bottom");
		var top = new Svc("top");
		var local = new Svc("local");

		var overlay = new StackOverlay();
		var store = new BasicBeanStore(null, overlay).addBean(Svc.class, local);

		overlay.push(new BasicBeanStore().addBean(Svc.class, bottom));
		overlay.push(new BasicBeanStore().addBean(Svc.class, top));

		assertSame(top, store.getBean(Svc.class).get());

		overlay.pop();
		assertSame(bottom, store.getBean(Svc.class).get());

		overlay.pop();
		assertSame(local, store.getBean(Svc.class).get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — toString lifecycle / cycle safety on the production-shaped composition
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S125"  // Explanatory prose describing the fixture shape; not dead code.
	})
	@Test
	void d01_toString_doesNotStackOverflow_onProductionShapedComposition() {
		// Production shape: outer BasicBeanStore holds StackOverlay in its overridingParent slot;
		// pushed frames may themselves be BasicBeanStores whose parent is the outer store
		// (e.g. when the Mode OVERLAY test fixture chains a TestBeanStore onto the live SUT's store).
		var overlay = new StackOverlay();
		var outer = new BasicBeanStore(null, overlay);
		var frame = new BasicBeanStore(outer);
		overlay.push(frame);

		assertDoesNotThrow(outer::toString, "BasicBeanStore.toString must not stack-overflow on cyclic StackOverlay composition");
		assertDoesNotThrow(overlay::toString, "StackOverlay.toString must not stack-overflow on cyclic frame chain");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — getBeansOfType across all four resolution tiers
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_getBeansOfType_includesAllTiers_overlayWins() {
		var overlay = new StackOverlay();
		var local = new Svc("local");
		var fromOverlay = new Svc("from-overlay");
		var fromDefault = new Svc("from-default");

		var store = new BasicBeanStore(null, overlay)
			.addBean(Svc.class, local, "local-only")
			.addBean(Svc.class, new Svc("local-shadowed"), "shared")
			.addDefaultSupplier(Svc.class, () -> fromDefault, "default-only");
		overlay.push(new BasicBeanStore().addBean(Svc.class, fromOverlay, "shared"));

		var all = store.getBeansOfType(Svc.class);
		assertSame(local, all.get("local-only"));
		assertSame(fromOverlay, all.get("shared"), "Overlay frame should shadow local entry on shared name");
		assertSame(fromDefault, all.get("default-only"));
	}
}
