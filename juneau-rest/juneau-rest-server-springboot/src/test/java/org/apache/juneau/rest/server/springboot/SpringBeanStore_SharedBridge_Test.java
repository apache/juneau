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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.Settings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.context.*;
import org.springframework.context.support.*;
import org.springframework.core.env.*;
import org.springframework.mock.env.*;

/**
 * Tests for the one-bridge-per-context registry ({@link SpringEnvironmentBridges}) as seen through
 * {@link SpringBeanStore}.
 */
@org.apache.juneau.testing.SpringbootTest
@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")
@SuppressWarnings({
	"java:S2093", // Stores are released with clear() (bridge deregistration), not close(); try-with-resources would call the wrong method.
	"resource" // Stores and contexts are test-local; each is cleared or closed explicitly where the test needs it.
})
class SpringBeanStore_SharedBridge_Test extends TestBase {

	private static final String K = "Juneau.B12.Key";

	private static int sources() {
		return Settings.get().sourceCount();
	}

	private static boolean registered(ApplicationContext ctx) {
		return SpringEnvironmentBridges.isRegistered(ctx);
	}

	private static GenericApplicationContext refreshed() {
		var ctx = new GenericApplicationContext();
		ctx.refresh();
		return ctx;
	}

	private static long closeListeners(AbstractApplicationContext ctx) {
		return ctx.getApplicationListeners().stream().filter(SpringEnvironmentBridges.CloseListener.class::isInstance).count();
	}

	@Test void b01_twoStoresOneContext_oneSource() {
		var ctx = mock(ApplicationContext.class);
		var base = sources();
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		try {
			assertEquals(base + 1, sources());
			assertTrue(registered(ctx));
			assertSame(s1.environmentSource(), s2.environmentSource());
		} finally {
			s1.clear();
			s2.clear();
		}
		assertEquals(base, sources());
		assertFalse(registered(ctx));
	}

	@Test void b02_lastClearRemoves_doubleClearReleasesOnce() {
		var ctx = mock(ApplicationContext.class);
		var base = sources();
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		s1.clear();
		assertEquals(base + 1, sources());
		s1.clear();
		assertEquals(base + 1, sources(), "A second clear() on the same store must not release again");
		assertTrue(registered(ctx));
		s2.clear();
		assertEquals(base, sources());
		assertFalse(registered(ctx));
	}

	@Test void b03_twoContexts_twoBridges() {
		var c1 = mock(ApplicationContext.class);
		var c2 = mock(ApplicationContext.class);
		var base = sources();
		var s1 = new SpringBeanStore(c1, null);
		var s2 = new SpringBeanStore(c2, null);
		try {
			assertEquals(base + 2, sources());
			assertNotSame(s1.environmentSource(), s2.environmentSource());
		} finally {
			s1.clear();
			s2.clear();
		}
		assertEquals(base, sources());
	}

	@Test void b04_contextClose_removesBridge_laterClearIsNoOp() {
		var base = sources();
		var ctx = refreshed();
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		assertEquals(base + 1, sources());
		assertEquals(1, closeListeners(ctx));
		ctx.close();
		assertFalse(registered(ctx));
		assertEquals(base, sources());
		assertDoesNotThrow(() -> { s1.clear(); s2.clear(); });
		assertEquals(base, sources());
	}

	@Test void b05_forceRemoveThenReacquire_oldClearDoesNotRemoveNew() {
		var base = sources();
		var ctx1 = refreshed();
		var old1 = new SpringBeanStore(ctx1, null);
		var old2 = new SpringBeanStore(ctx1, null);
		ctx1.close();
		assertEquals(base, sources());

		// A new context registers fresh; an old store's clear() leaves it alone.
		var ctx2 = refreshed();
		try {
			var fresh = new SpringBeanStore(ctx2, null);
			assertNotSame(old1.environmentSource(), fresh.environmentSource());
			old1.clear();
			assertTrue(registered(ctx2));
			assertEquals(base + 1, sources());
		} finally {
			ctx2.close();
		}
		assertEquals(base, sources());

		// The same, already-closed context: re-acquire registers fresh with no listener, and an old store's
		// clear() does not remove the new registration (identity check in release).
		var again = new SpringBeanStore(ctx1, null);
		assertTrue(registered(ctx1));
		assertEquals(0, closeListeners(ctx1));
		old2.clear();
		assertTrue(registered(ctx1));
		again.clear();
		assertFalse(registered(ctx1));
		assertEquals(base, sources());
	}

	@Test void b06_storeClose_doesNotDropSharedBridge() throws Exception {
		var ctx = mock(ApplicationContext.class);
		var base = sources();
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		try {
			s1.close();
			assertTrue(registered(ctx));
			assertEquals(base + 1, sources());
		} finally {
			// A closed store rejects clear() (BasicBeanStore asserts it is open), but still releases its reference.
			assertThrows(RuntimeException.class, s1::clear);
			s2.clear();
		}
		assertEquals(base, sources());
	}

	@Test void b07_alreadyClosedContext_noListener_clearRemoves() {
		var base = sources();
		var ctx = refreshed();
		ctx.close();
		var s = new SpringBeanStore(ctx, null);
		assertTrue(registered(ctx));
		assertEquals(0, closeListeners(ctx));
		s.clear();
		assertFalse(registered(ctx));
		assertEquals(base, sources());
	}

	@Test void b08_plainContext_noInteractions_staysUntilClear() {
		var ctx = mock(ApplicationContext.class);
		var s = new SpringBeanStore(ctx, null);
		try {
			verifyNoInteractions(ctx);
			assertTrue(registered(ctx));
		} finally {
			s.clear();
		}
		assertFalse(registered(ctx));
	}

	@Test void b09_parentChild_childCloseRemovesOnlyChild() {
		var base = sources();
		var parent = refreshed();
		var child = new GenericApplicationContext(parent);
		child.refresh();
		var sp = new SpringBeanStore(parent, null);
		var sc = new SpringBeanStore(child, null);
		try {
			assertEquals(base + 2, sources());
			child.close();
			assertFalse(registered(child));
			assertTrue(registered(parent), "The parent's listener must ignore the child's republished close event");
			assertEquals(base + 1, sources());
			parent.close();
			assertFalse(registered(parent));
			assertEquals(base, sources());
		} finally {
			sp.clear();
			sc.clear();
			if (parent.isActive())
				parent.close();
		}
	}

	@Test void b10_lastClear_removesCloseListener() {
		var base = sources();
		var ctx = refreshed();
		try {
			var s = new SpringBeanStore(ctx, null);
			assertEquals(1, closeListeners(ctx));
			s.clear();
			assertEquals(0, closeListeners(ctx));
			assertFalse(registered(ctx));
		} finally {
			ctx.close();
		}
		assertFalse(registered(ctx));
		assertEquals(base, sources());
	}

	@Test void b11_invalidate_isPerContext() {
		var k = "Juneau.B11.Key";
		var map = new HashMap<String,Object>();
		var env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("m", map));
		var ctx = mock(ApplicationContext.class);
		when(ctx.getEnvironment()).thenReturn(env);
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		try {
			assertFalse(s1.environmentSource().get(k).isPresent());
			map.put(k, "v");
			assertFalse(s2.environmentSource().get(k).isPresent(), "The miss cache is shared by every store on the context");
			s1.environmentSource().invalidate();
			assertEquals("v", s2.environmentSource().get(k).value().orElse(null));
		} finally {
			s1.clear();
			s2.clear();
		}
	}

	@Test void b12_q4a_sourceAddedBetweenStores_winsOverSharedBridge() {
		var env = new MockEnvironment().withProperty(K, "from-spring");
		var ctx = mock(ApplicationContext.class);
		when(ctx.getEnvironment()).thenReturn(env);
		org.apache.juneau.commons.settings.PropertySource between = n -> K.equals(n)
			? org.apache.juneau.commons.settings.PropertyLookupResult.present("in-between")
			: org.apache.juneau.commons.settings.PropertyLookupResult.missing();
		var a = new SpringBeanStore(ctx, null);
		Settings.get().addSource(between);
		var b = new SpringBeanStore(ctx, null);
		try {
			assertEquals("in-between", Settings.get().get(K, "none"), "Q4-A: the shared bridge keeps its first position");
		} finally {
			a.clear();
			b.clear();
			Settings.get().removeSource(between);
		}
		assertEquals("none", Settings.get().get(K, "none"));
	}

	@Test void b13_invalidateEnvironmentCache_byContext() {
		var k = "Juneau.B13.Key";
		var map = new HashMap<String,Object>();
		var env = new StandardEnvironment();
		env.getPropertySources().addFirst(new MapPropertySource("m", map));
		var ctx = mock(ApplicationContext.class);
		when(ctx.getEnvironment()).thenReturn(env);
		var s = new SpringBeanStore(ctx, null);
		try {
			assertFalse(s.environmentSource().get(k).isPresent());
			map.put(k, "v");
			assertTrue(SpringBeanStore.invalidateEnvironmentCache(ctx));
			assertEquals("v", s.environmentSource().get(k).value().orElse(null));
		} finally {
			s.clear();
		}
		assertFalse(SpringBeanStore.invalidateEnvironmentCache(ctx));
		assertFalse(SpringBeanStore.invalidateEnvironmentCache(null));
	}

	@Test void b14_clearThrowsAfterClose_stillReleasesBridge() {
		var ctx = mock(ApplicationContext.class);
		var base = sources();
		var s1 = new SpringBeanStore(ctx, null);
		var s2 = new SpringBeanStore(ctx, null);
		s1.close();
		assertThrows(RuntimeException.class, s1::clear);
		assertTrue(registered(ctx), "s2 still holds a reference");
		assertEquals(base + 1, sources());
		assertThrows(RuntimeException.class, s1::clear);
		assertTrue(registered(ctx), "A second throwing clear() must not release again");
		s2.clear();
		assertFalse(registered(ctx));
		assertEquals(base, sources());
	}

	@Test void b15_closedSoleStore_clearReleasesBridgeAndThrows() {
		var ctx = mock(ApplicationContext.class);
		var base = sources();
		var s = new SpringBeanStore(ctx, null);
		s.close();
		assertTrue(registered(ctx));
		assertThrows(RuntimeException.class, s::clear);
		assertFalse(registered(ctx));
		assertEquals(base, sources());
	}
}
