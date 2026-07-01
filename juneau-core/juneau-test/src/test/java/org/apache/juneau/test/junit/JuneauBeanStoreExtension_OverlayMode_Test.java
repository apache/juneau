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
package org.apache.juneau.test.junit;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.test.junit.testsupport.*;
import org.junit.jupiter.api.*;

/**
 * Validates the {@link Mode#OVERLAY Mode.OVERLAY} push/pop wiring on {@link JuneauBeanStoreExtension}.
 *
 * <p>
 * Drives the extension directly (no JUnit-managed instance) and asserts:
 * <ul>
 * 	<li>{@link JuneauBeanStoreExtension#attach attach}/{@link JuneauBeanStoreExtension#detach detach} and
 * 		{@link JuneauBeanStoreExtension#getAttachedStore getAttachedStore} round-trip correctly.
 * 	<li>{@link Mode#OVERLAY} with no attached store throws a clear {@link IllegalStateException}.
 * 	<li>Mixing {@link Mode#INJECT} and {@link Mode#OVERLAY} declarations in a single scope is rejected at
 * 		scope-build time.
 * 	<li>An all-{@code Mode.OVERLAY} method-scope overlay is pushed/popped against the attached store across the
 * 		{@code beforeEach}/{@code afterEach} lifecycle.
 * </ul>
 */
@SuppressWarnings({
	"resource" // Bean stores/overlays are Closeables whose lifecycle is owned by the extension/test; Eclipse JDT @Owning warning is by design.
})
class JuneauBeanStoreExtension_OverlayMode_Test extends TestBase {

	interface Svc {
		String describe();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — attach/detach round-trip
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_attach_then_getAttachedStore_returnsIt() {
		var ext = new JuneauBeanStoreExtension();
		WritableBeanStore store = new BasicBeanStore();
		ext.attach(store);
		assertSame(store, ext.getAttachedStore().orElseThrow());
	}

	@Test
	void a02_detach_clearsAttachedStore() {
		var ext = new JuneauBeanStoreExtension();
		ext.attach(new BasicBeanStore());
		ext.detach();
		assertTrue(ext.getAttachedStore().isEmpty());
	}

	@Test
	void a03_attach_null_throws() {
		var ext = new JuneauBeanStoreExtension();
		assertThrows(NullPointerException.class, () -> ext.attach(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — Mode.OVERLAY without attach is rejected at beforeEach
	//-----------------------------------------------------------------------------------------------------------------

	static class OverlayNoAttachHolder {
		@TestBean(mode = Mode.OVERLAY)
		Svc svc = () -> "overlay";
	}

	@Test
	void b01_overlayMode_withoutAttach_throwsAtBeforeEach() {
		var ext = new JuneauBeanStoreExtension();
		var ctx = StubExtensionContext.of(OverlayNoAttachHolder.class, new OverlayNoAttachHolder());
		ext.beforeAll(ctx);
		var ex = assertThrows(IllegalStateException.class, () -> ext.beforeEach(ctx));
		assertTrue(ex.getMessage().contains("Mode.OVERLAY"),
			"Error should mention Mode.OVERLAY requirement: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — mixed Mode.INJECT + Mode.OVERLAY in the same scope is rejected
	//-----------------------------------------------------------------------------------------------------------------

	static class MixedModesHolder {
		@TestBean(mode = Mode.INJECT) Svc a = () -> "a";
		@TestBean(mode = Mode.OVERLAY) Svc b = () -> "b";
	}

	@Test
	void c01_mixedModes_inMethodScope_throws() {
		var holder = new MixedModesHolder();
		var ex = assertThrows(IllegalStateException.class,
			() -> JuneauBeanStoreExtension.buildMethodScopeStoreWithMode(holder, null));
		assertTrue(ex.getMessage().contains("Mixed @TestBean modes"),
			"Error should mention mixed modes: " + ex.getMessage());
	}

	static class MixedModesClassHolder {
		@TestBean(scope = Scope.CLASS, mode = Mode.INJECT) static Svc a = () -> "a";
		@TestBean(scope = Scope.CLASS, mode = Mode.OVERLAY) static Svc b = () -> "b";
	}

	@Test
	void c02_mixedModes_inClassScope_throws() {
		var ex = assertThrows(IllegalStateException.class,
			() -> JuneauBeanStoreExtension.buildClassScopeStoreWithMode(MixedModesClassHolder.class));
		assertTrue(ex.getMessage().contains("Mixed @TestBean modes"),
			"Error should mention mixed modes: " + ex.getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — happy-path: attached + all-Mode.OVERLAY method-scope override is pushed/popped across
	//     beforeEach/afterEach
	//-----------------------------------------------------------------------------------------------------------------

	static class OverlayHolder {
		@TestBean(mode = Mode.OVERLAY)
		Svc svc = () -> "overlay";
	}

	static class InjectHolder {
		@TestBean
		Svc svc = () -> "overlay";
	}

	@Test
	void d01_overlayMode_pushedAt_beforeEach_poppedAt_afterEach() {
		var ext = new JuneauBeanStoreExtension();
		WritableBeanStore store = new BasicBeanStore();
		store.addBean(Svc.class, () -> "production");
		ext.attach(store);

		assertEquals("production", store.getBean(Svc.class).orElseThrow().describe(),
			"Before push, lookup should see the production bean");

		var ctx = StubExtensionContext.of(OverlayHolder.class, new OverlayHolder());
		ext.beforeAll(ctx);
		ext.beforeEach(ctx);
		assertEquals("overlay", store.getBean(Svc.class).orElseThrow().describe(),
			"During the test, lookup should see the overlay bean");

		ext.afterEach(ctx);
		assertEquals("production", store.getBean(Svc.class).orElseThrow().describe(),
			"After afterEach, lookup should see the production bean again");

		ext.afterAll(ctx);
	}

	@Test
	void d02_injectMode_withoutAttach_doesNotPush() {
		var ext = new JuneauBeanStoreExtension();

		var holder = new InjectHolder();
		var ctx = StubExtensionContext.of(InjectHolder.class, holder);
		// Mode.INJECT is the back-compat default; the full lifecycle must complete without throwing.
		assertDoesNotThrow(() -> {
			ext.beforeAll(ctx);
			ext.beforeEach(ctx);
			ext.afterEach(ctx);
			ext.afterAll(ctx);
		});
	}
}
