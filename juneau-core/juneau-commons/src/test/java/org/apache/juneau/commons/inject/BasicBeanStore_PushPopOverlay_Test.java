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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Tests for {@link BasicBeanStore#pushOverlay(BeanStore)} / {@link BasicBeanStore#popOverlay(Snapshot)}.
 *
 * <p>
 * Validates the Phase 6 (Mode OVERLAY) push/pop overlay primitive on {@link BasicBeanStore} (work item 35):
 * overlay precedence over local entries and the construction-time {@code overridingParent} slot, LIFO
 * discipline, foreign-snapshot rejection, null-overlay rejection, empty-stack pop rejection, and the
 * happy-path lookup-shadow-then-restore semantics that the {@code JuneauBeanStoreExtension} Mode OVERLAY
 * machinery depends on.
 */
@SuppressWarnings({
	"java:S5778", // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"resource"    // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class BasicBeanStore_PushPopOverlay_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final class Svc {
		final String tag;
		Svc(String tag) { this.tag = tag; }
		@Override public String toString() { return "Svc[" + tag + "]"; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — happy path: push shadows local entry, pop restores it
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_push_shadowsLocalEntry_pop_restoresIt() {
		var production = new Svc("production");
		var override = new Svc("override");

		var store = new BasicBeanStore().addBean(Svc.class, production);
		assertSame(production, store.getBean(Svc.class).orElseThrow(), "Pre-push lookup should see local entry");

		var overlay = new BasicBeanStore().addBean(Svc.class, override);
		var snap = store.pushOverlay(overlay);

		assertSame(override, store.getBean(Svc.class).orElseThrow(),
			"After push, lookup should resolve to overlay's bean");

		store.popOverlay(snap);

		assertSame(production, store.getBean(Svc.class).orElseThrow(),
			"After pop, lookup should resolve back to the local entry");
	}

	@Test
	void a02_pushReturns_freshSnapshot() {
		var store = new BasicBeanStore();
		var snap = store.pushOverlay(new BasicBeanStore());
		assertNotNull(snap);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — nested push/push/pop/pop respects LIFO
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_nestedPush_topFrameWins() {
		var store = new BasicBeanStore();
		var lower = new Svc("lower");
		var upper = new Svc("upper");

		var s1 = store.pushOverlay(new BasicBeanStore().addBean(Svc.class, lower));
		assertSame(lower, store.getBean(Svc.class).orElseThrow());

		var s2 = store.pushOverlay(new BasicBeanStore().addBean(Svc.class, upper));
		assertSame(upper, store.getBean(Svc.class).orElseThrow(),
			"Top frame must win even when an underlying frame defines the same type");

		store.popOverlay(s2);
		assertSame(lower, store.getBean(Svc.class).orElseThrow(),
			"After popping the top frame, the underlying frame should be visible again");

		store.popOverlay(s1);
		assertTrue(store.getBean(Svc.class).isEmpty(),
			"After popping all frames, the overlay stack should fall through to local entries (which are empty here)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — out-of-order pop throws
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_outOfOrderPop_throws() {
		var store = new BasicBeanStore();
		var s1 = store.pushOverlay(new BasicBeanStore().addBean(Svc.class, new Svc("a")));
		store.pushOverlay(new BasicBeanStore().addBean(Svc.class, new Svc("b")));

		var ex = assertThrows(IllegalStateException.class, () -> store.popOverlay(s1));
		assertTrue(ex.getMessage().contains("Out-of-order"),
			"Error message should mention LIFO violation: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — pop-when-empty throws
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_popWhenNoStackPushedYet_throws() {
		var store = new BasicBeanStore();
		// Build a snapshot via push/pop on a different store so we can simulate a stale snapshot on a never-pushed
		// stack.
		var sibling = new BasicBeanStore();
		var foreignSnap = sibling.pushOverlay(new BasicBeanStore());
		sibling.popOverlay(foreignSnap);

		var ex = assertThrows(IllegalStateException.class, () -> store.popOverlay(foreignSnap));
		// Could be either "foreign-snapshot" or "empty overlay stack"; foreign-snapshot is checked first.
		assertTrue(ex.getMessage().contains("foreign-snapshot") || ex.getMessage().contains("empty"),
			"Message should mention foreign-snapshot or empty stack: " + ex.getMessage());
	}

	@Test
	void d02_pop_afterAllFramesPopped_throws() {
		var store = new BasicBeanStore();
		var s = store.pushOverlay(new BasicBeanStore());
		store.popOverlay(s);

		var ex = assertThrows(IllegalStateException.class, () -> store.popOverlay(s));
		assertTrue(ex.getMessage().contains("empty"),
			"Error message should mention empty stack: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — null guards
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void e01_push_null_throws() {
		var store = new BasicBeanStore();
		assertThrows(NullPointerException.class, () -> store.pushOverlay(null));
	}

	@Test
	void e02_pop_null_throws() {
		var store = new BasicBeanStore();
		assertThrows(NullPointerException.class, () -> store.popOverlay(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — foreign-snapshot pop rejected
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_snapshotFromForeignStore_rejected() {
		var storeA = new BasicBeanStore();
		var storeB = new BasicBeanStore();

		var snapFromA = storeA.pushOverlay(new BasicBeanStore());

		var ex = assertThrows(IllegalStateException.class, () -> storeB.popOverlay(snapFromA));
		assertTrue(ex.getMessage().contains("foreign-snapshot"),
			"Error message should mention foreign-snapshot: " + ex.getMessage());

		// storeA's stack should be untouched — verify by popping cleanly.
		storeA.popOverlay(snapFromA);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — precedence: overlay > local entries > overridingParent > parent chain > defaults
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void g01_overlay_beats_overridingParent_beats_localEntry() {
		var local = new Svc("local");
		var fromOverriding = new Svc("overriding");
		var fromOverlay = new Svc("overlay");

		var overriding = new BasicBeanStore().addBean(Svc.class, fromOverriding);
		var store = new BasicBeanStore(null, overriding).addBean(Svc.class, local);

		// Without overlay, overridingParent already wins over local.
		assertSame(fromOverriding, store.getBean(Svc.class).orElseThrow());

		// Push an overlay — overlay wins.
		var snap = store.pushOverlay(new BasicBeanStore().addBean(Svc.class, fromOverlay));
		assertSame(fromOverlay, store.getBean(Svc.class).orElseThrow(),
			"Pushed overlay should win over both overridingParent and local entries");

		store.popOverlay(snap);
		assertSame(fromOverriding, store.getBean(Svc.class).orElseThrow(),
			"After popping overlay, overridingParent should reassert");
	}

	@Test
	void g02_overlay_fallthrough_to_localEntry() {
		var local = new Svc("local");
		var store = new BasicBeanStore().addBean(Svc.class, local);

		// Overlay is empty for this type, so lookup falls through to local entries.
		var snap = store.pushOverlay(new BasicBeanStore());
		assertSame(local, store.getBean(Svc.class).orElseThrow(),
			"Empty overlay should not block fall-through to local entries");

		store.popOverlay(snap);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h — getBeansOfType merges overlay frames over local entries
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_getBeansOfType_overlayShadowsLocal_byName() {
		var localA = new Svc("local-a");
		var localB = new Svc("local-b");
		var overlayA = new Svc("overlay-a");

		var store = new BasicBeanStore()
			.addBean(Svc.class, localA, "a")
			.addBean(Svc.class, localB, "b");

		var snap = store.pushOverlay(new BasicBeanStore().addBean(Svc.class, overlayA, "a"));

		var all = store.getBeansOfType(Svc.class);
		assertEquals(2, all.size(), "Map should have two entries (one shadowed, one local-only)");
		assertSame(overlayA, all.get("a"), "Overlay's 'a' should shadow local 'a'");
		assertSame(localB, all.get("b"), "Local 'b' should still be visible (no overlay entry shadows it)");

		store.popOverlay(snap);

		var afterPop = store.getBeansOfType(Svc.class);
		assertSame(localA, afterPop.get("a"), "After pop, local 'a' is back");
		assertSame(localB, afterPop.get("b"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i — closed store rejects push and pop
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_closed_push_throws() throws Exception {
		var store = new BasicBeanStore();
		store.close();
		assertThrows(IllegalStateException.class, () -> store.pushOverlay(new BasicBeanStore()));
	}

	@Test
	void i02_closed_pop_throws() throws Exception {
		var store = new BasicBeanStore();
		var snap = store.pushOverlay(new BasicBeanStore());
		store.close();
		assertThrows(IllegalStateException.class, () -> store.popOverlay(snap));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// j — toString includes overlayStack when frames are present
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void j01_toString_includesOverlayStack_whenPushed() {
		var store = new BasicBeanStore();
		assertFalse(store.toString().contains("overlayStack"),
			"Empty stack should not appear in toString to keep output lean");

		var snap = store.pushOverlay(new BasicBeanStore());
		assertTrue(store.toString().contains("overlayStack"),
			"Once a frame is pushed, toString should surface the overlayStack property");
		assertTrue(store.toString().contains("depth=1"),
			"toString should expose StackOverlay's depth=1: " + store);

		store.popOverlay(snap);
	}
}
