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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link StackOverlay}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class StackOverlay_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	private static final class Svc {
		final String tag;
		Svc(String tag) { this.tag = tag; }
		@Override public String toString() { return "Svc[" + tag + "]"; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — push / pop happy path
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_pushPop_resolutionOrder() {
		var overlay = new StackOverlay();
		var bottom = new Svc("bottom");
		var top = new Svc("top");

		overlay.push(new BasicBeanStore().addBean(Svc.class, bottom));
		overlay.push(new BasicBeanStore().addBean(Svc.class, top));

		assertEquals(2, overlay.depth());
		assertSame(top, overlay.getBean(Svc.class).get(), "Top frame should win");
		assertTrue(overlay.hasBean(Svc.class));

		overlay.pop();

		assertEquals(1, overlay.depth());
		assertSame(bottom, overlay.getBean(Svc.class).get(), "After popping, bottom frame should win");

		overlay.pop();

		assertEquals(0, overlay.depth());
		assertTrue(overlay.getBean(Svc.class).isEmpty(), "After popping all frames, lookup is empty");
	}

	@Test
	void a02_pushReturnsSelf_forFluentChaining() {
		var overlay = new StackOverlay();
		var ret = overlay.push(new BasicBeanStore().addBean(Svc.class, new Svc("a")));
		assertSame(overlay, ret);
	}

	@Test
	void a03_peek_returnsTopFrameWithoutRemoving() {
		var overlay = new StackOverlay();
		assertNull(overlay.peek(), "Empty stack peek returns null");

		var frameA = new BasicBeanStore();
		var frameB = new BasicBeanStore();
		overlay.push(frameA);
		overlay.push(frameB);

		assertSame(frameB, overlay.peek(), "Peek returns the top (last-pushed) frame");
		assertEquals(2, overlay.depth(), "Peek does not remove the frame");
	}

	@Test
	void a04_popReturnsRemovedFrame() {
		var overlay = new StackOverlay();
		var frameA = new BasicBeanStore();
		var frameB = new BasicBeanStore();
		overlay.push(frameA);
		overlay.push(frameB);

		assertSame(frameB, overlay.pop());
		assertSame(frameA, overlay.pop());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — error / edge cases
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_pop_emptyStack_throwsIllegalStateException() {
		var overlay = new StackOverlay();
		var ex = assertThrows(IllegalStateException.class, overlay::pop);
		assertTrue(ex.getMessage().contains("empty"), "Message should mention empty stack: " + ex.getMessage());
	}

	@Test
	void b02_pop_afterAllFramesPopped_throwsIllegalStateException() {
		var overlay = new StackOverlay();
		overlay.push(new BasicBeanStore());
		overlay.pop();
		assertThrows(IllegalStateException.class, overlay::pop);
	}

	@Test
	void b03_push_null_throwsIllegalArgumentException() {
		var overlay = new StackOverlay();
		assertThrows(IllegalArgumentException.class, () -> overlay.push(null));
	}

	@Test
	void b04_emptyStack_fallThrough_returnsEmpty() {
		var overlay = new StackOverlay();
		assertTrue(overlay.getBean(Svc.class).isEmpty());
		assertTrue(overlay.getBean(Svc.class, "named").isEmpty());
		assertTrue(overlay.getBeanSupplier(Svc.class).isEmpty());
		assertTrue(overlay.getBeanSupplier(Svc.class, "named").isEmpty());
		assertFalse(overlay.hasBean(Svc.class));
		assertFalse(overlay.hasBean(Svc.class, "named"));
		assertTrue(overlay.getBeansOfType(Svc.class).isEmpty());
		assertTrue(overlay.getBeanType(Svc.class).isEmpty());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — toString / no recursion
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_toString_emptyStack() {
		var s = new StackOverlay().toString();
		assertTrue(s.contains("depth=0"), "Should contain depth=0: " + s);
		assertTrue(s.contains("StackOverlay"));
	}

	@Test
	void c02_toString_doesNotRecurseIntoFrames_thatBackReferenceTheOuterChain() {
		// Construct the same shape that BasicBeanStore + StackOverlay form in production:
		// the outer BasicBeanStore holds the StackOverlay in its overridingParent slot, and the
		// frame's parent is the same outer BasicBeanStore — i.e. the frame back-references the
		// chain that owns the overlay.  toString must not stack-overflow on this shape.
		var overlay = new StackOverlay();
		var outer = new BasicBeanStore(null, overlay);
		var frame = new BasicBeanStore(outer);
		overlay.push(frame);

		// Should not throw / overflow.
		var s = overlay.toString();
		assertTrue(s.contains("depth=1"), "Expected depth=1: " + s);
		// Outer's toString also must not overflow despite the cycle (because StackOverlay.toString
		// is flat — it does not recurse into frame.toString()).
		assertDoesNotThrow(outer::toString);
	}

	@Test
	void c03_toString_includesPerFrameIdentity() {
		var overlay = new StackOverlay();
		overlay.push(new BasicBeanStore());
		overlay.push(new BasicBeanStore());
		var s = overlay.toString();
		assertTrue(s.contains("BasicBeanStore@"), "Expected BasicBeanStore@<id>: " + s);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — multi-frame getBeansOfType merge semantics
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void d01_getBeansOfType_topFrame_shadowsBottomFrame_byName() {
		var overlay = new StackOverlay();
		var bottomA = new Svc("bottom-a");
		var bottomB = new Svc("bottom-b");
		var topA = new Svc("top-a");

		overlay.push(new BasicBeanStore()
			.addBean(Svc.class, bottomA, "a")
			.addBean(Svc.class, bottomB, "b"));
		overlay.push(new BasicBeanStore()
			.addBean(Svc.class, topA, "a"));

		var all = overlay.getBeansOfType(Svc.class);
		assertEquals(2, all.size(), "Should have entries for both names");
		assertSame(topA, all.get("a"), "Top frame should shadow bottom for name 'a'");
		assertSame(bottomB, all.get("b"), "Bottom frame's 'b' should still be visible");
	}

	@Test
	void d02_namedBean_resolutionOrder() {
		var overlay = new StackOverlay();
		var top = new Svc("top");
		var bottom = new Svc("bottom");

		overlay.push(new BasicBeanStore().addBean(Svc.class, bottom, "primary"));
		overlay.push(new BasicBeanStore().addBean(Svc.class, top, "primary"));

		assertSame(top, overlay.getBean(Svc.class, "primary").get());
		overlay.pop();
		assertSame(bottom, overlay.getBean(Svc.class, "primary").get());
	}

	@Test
	void d03_supplier_resolutionOrder() {
		var overlay = new StackOverlay();
		var bottom = new Svc("bottom");
		var top = new Svc("top");

		overlay.push(new BasicBeanStore().addSupplier(Svc.class, () -> bottom));
		overlay.push(new BasicBeanStore().addSupplier(Svc.class, () -> top));

		var sup = overlay.getBeanSupplier(Svc.class);
		assertTrue(sup.isPresent());
		assertSame(top, sup.get().get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — concurrency smoke test
	//-----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"java:S2093" // ExecutorService teardown uses explicit shutdownNow() in finally; try-with-resources close() would instead block awaiting termination, changing this concurrency smoke test's teardown semantics.
	})
	@Test
	void e01_concurrentReads_whilePushPopInFlight_doNotThrow() throws Exception {
		var overlay = new StackOverlay();
		var base = new Svc("base");
		overlay.push(new BasicBeanStore().addBean(Svc.class, base));

		var stop = new AtomicBoolean(false);
		var errors = new AtomicReference<Throwable>(null);
		var readerCount = 8;
		var pool = Executors.newFixedThreadPool(readerCount + 1);
		var iterations = 200;

		try {
			for (var i = 0; i < readerCount; i++) {
				pool.submit(() -> {
					try {
						while (!stop.get()) {
							var b = overlay.getBean(Svc.class);
							b.ifPresent(s -> {
								if (s.tag == null)
									throw new AssertionError("null tag");
							});
						}
					} catch (Throwable t) {
						errors.set(t);
					}
				});
			}
			pool.submit(() -> {
				try {
					for (var i = 0; i < iterations; i++) {
						var pushed = new BasicBeanStore().addBean(Svc.class, new Svc("frame-" + i));
						overlay.push(pushed);
						Thread.yield();
						overlay.pop();
					}
				} catch (Throwable t) {
					errors.set(t);
				} finally {
					stop.set(true);
				}
			});

			pool.shutdown();
			assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "Pool did not terminate in time");
		} finally {
			pool.shutdownNow();
		}

		assertNull(errors.get(), () -> "Concurrent operation surfaced error: " + errors.get());
		assertEquals(1, overlay.depth(), "Stack should be back to its original depth");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — getBeanType pass-through
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_getBeanType_walksFrames() {
		var overlay = new StackOverlay();
		var bottom = new BasicBeanStore();
		bottom.addBeanType(Svc.class, Svc.class);
		overlay.push(bottom);

		assertTrue(overlay.getBeanType(Svc.class).isPresent());
		assertEquals(Svc.class, overlay.getBeanType(Svc.class).get());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — multi-frame fall-through (top frame does not have the bean, bottom frame does)
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * When the top frame does not have the requested bean, lookup must fall through to the lower frame.
	 * Exercises the "continue loop past empty frame" branch on getBean, hasBean, getBeanSupplier, getBeanType.
	 */
	@Test
	void g01_fallThrough_topFrameMissing_bottomFrameHas() {
		var overlay = new StackOverlay();
		var found = new Svc("from-bottom");

		// Bottom frame has the bean; top frame is intentionally empty (no Svc registered).
		overlay.push(new BasicBeanStore().addBean(Svc.class, found));
		overlay.push(new BasicBeanStore());

		assertSame(found, overlay.getBean(Svc.class).get(),
			"Lookup should fall through the empty top frame to the bottom frame");
		assertTrue(overlay.hasBean(Svc.class),
			"hasBean should fall through the empty top frame to the bottom frame");
		assertTrue(overlay.getBeanSupplier(Svc.class).isPresent(),
			"getBeanSupplier should fall through the empty top frame to the bottom frame");
	}

	@Test
	void g02_fallThrough_getBeanType_topFrameMissing_bottomFrameHas() {
		var overlay = new StackOverlay();
		var bottom = new BasicBeanStore();
		bottom.addBeanType(Svc.class, Svc.class);

		overlay.push(bottom);
		overlay.push(new BasicBeanStore());

		assertTrue(overlay.getBeanType(Svc.class).isPresent(),
			"getBeanType should fall through the empty top frame to the bottom frame");
		assertEquals(Svc.class, overlay.getBeanType(Svc.class).get());
	}
}
