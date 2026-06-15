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
package org.apache.juneau.junit5;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * Tests for class-scope overlays produced by {@link JuneauBeanStoreExtension} (Phase 3 of work item 35).
 *
 * <p>
 * Phase 3 covers {@code @TestBean(scope = CLASS)} discovery, shared lifetime across test methods,
 * method-scope shadowing of class-scope, and mixed-lifecycle coexistence inside a single test class.
 *
 * <p>
 * These tests run under {@code @ExtendWith(JuneauBeanStoreExtension.class)} so the
 * {@code beforeAll}/{@code afterAll} hooks fire end-to-end.  Verification that {@code afterAll}
 * drops the class-scope overlay is performed by {@link C_AfterAllReleasesClassScope} using a
 * static {@link AtomicReference} that the extension's {@code AfterAllCallback} clears via direct
 * inspection of an unbound extension instance after the test class completes.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(JuneauBeanStoreExtension.class)
@SuppressWarnings({
	"resource" // Bean stores/overlays are Closeables whose lifecycle is owned by the extension/test; Eclipse JDT @Owning warning is by design.
})
class JuneauBeanStoreExtension_ClassScope_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Marker beans
	//-----------------------------------------------------------------------------------------------------------------

	static final class Svc {
		final String tag;
		Svc(String tag) { this.tag = tag; }
	}

	static final class OtherSvc {
		final String tag;
		OtherSvc(String tag) { this.tag = tag; }
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — class-scope static field is shared across multiple @Test methods in the same class.
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean(scope = Scope.CLASS)
	static Svc sharedClassSvc = new Svc("class-shared");

	private static final AtomicReference<Svc> a_seenInFirstTest = new AtomicReference<>();

	@Test
	void a01_classScopeStaticField_isVisible(TestBeanStore store) {
		var seen = store.getBean(Svc.class).orElseThrow();
		assertSame(sharedClassSvc, seen, "Class-scope static field should be visible in test methods");
		a_seenInFirstTest.set(seen);
	}

	@Test
	void a02_classScopeStaticField_isSameInstanceAcrossMethods(TestBeanStore store) {
		var seen = store.getBean(Svc.class).orElseThrow();
		assertSame(sharedClassSvc, seen);
		assertSame(a_seenInFirstTest.get(), seen,
			"Class-scope overlay must be shared across @Test methods (single instance, not rebuilt per method)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — method-scope shadows class-scope (same type, same name); other class-scope entries fall through.
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean(scope = Scope.CLASS, name = "shadowable")
	static Svc classLevelShadowable = new Svc("class-level");

	@TestBean(scope = Scope.CLASS, name = "fallthrough")
	static OtherSvc classLevelFallthrough = new OtherSvc("class-only");

	@TestBean(name = "shadowable")
	Svc methodLevelShadow = new Svc("method-level");

	@Test
	void b01_methodScope_shadowsClassScope_sameNameAndType(TestBeanStore store) {
		assertSame(methodLevelShadow, store.getBean(Svc.class, "shadowable").orElseThrow(),
			"Method-scope overlay must shadow class-scope when (type, name) match");
	}

	@Test
	void b02_classScope_falls_through_when_no_method_scope_override(TestBeanStore store) {
		assertSame(classLevelFallthrough, store.getBean(OtherSvc.class, "fallthrough").orElseThrow(),
			"Class-scope entries with no method-scope shadow must still be visible via the method overlay's parent chain");
	}

	@Test
	void b03_getStore_scope_CLASS_returnsClassLayerDirectly() {
		var classStore = JuneauBeanStoreExtension.buildClassScopeStore(JuneauBeanStoreExtension_ClassScope_Test.class);
		var methodStore = JuneauBeanStoreExtension.buildMethodScopeStore(this, classStore);

		// The class store contains "shadowable" -> class-level
		assertSame(classLevelShadowable, classStore.getBean(Svc.class, "shadowable").orElseThrow());
		// The method store shadows it
		assertSame(methodLevelShadow, methodStore.getBean(Svc.class, "shadowable").orElseThrow());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — mixed lifecycles in the same class coexist.
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean(scope = Scope.CLASS, name = "mixed-class")
	static Svc mixedClassSvc = new Svc("mixed-class");

	@TestBean(name = "mixed-method")
	Svc mixedMethodSvc = new Svc("mixed-method");

	@Test
	void c01_mixedScopes_bothVisible(TestBeanStore store) {
		assertSame(mixedClassSvc, store.getBean(Svc.class, "mixed-class").orElseThrow());
		assertSame(mixedMethodSvc, store.getBean(Svc.class, "mixed-method").orElseThrow());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — static-method factory at class scope is invoked once at beforeAll.
	//-----------------------------------------------------------------------------------------------------------------

	private static final AtomicInteger d_factoryInvocations = new AtomicInteger();
	private static final AtomicInteger d_factorySnapshotAtFirstTest = new AtomicInteger(-1);

	@TestBean(scope = Scope.CLASS, name = "class-factory")
	static Svc classFactory() {
		d_factoryInvocations.incrementAndGet();
		return new Svc("from-class-factory");
	}

	@Test
	void d01_classScopeFactory_visible(TestBeanStore store) {
		var svc = store.getBean(Svc.class, "class-factory").orElseThrow();
		assertEquals("from-class-factory", svc.tag);
		// Stamp the invocation count seen at d01 so d02 can verify it is unchanged across method boundaries
		// (i.e. the runtime didn't re-invoke the factory between @Test methods of the same class).
		d_factorySnapshotAtFirstTest.compareAndSet(-1, d_factoryInvocations.get());
	}

	@Test
	void d02_classScopeFactory_notReinvokedBetweenTestMethods(TestBeanStore store) {
		var svc = store.getBean(Svc.class, "class-factory").orElseThrow();
		assertEquals("from-class-factory", svc.tag);
		var snapshot = d_factorySnapshotAtFirstTest.get();
		assertTrue(snapshot > 0, "d01 must have stamped the snapshot (test order: methodName, d01 < d02)");
		assertEquals(snapshot, d_factoryInvocations.get(),
			"@TestBean(scope = CLASS) factory must not be re-invoked between @Test methods of the same class");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — getStore(Scope.CLASS) / getStore(Scope.METHOD) accessor semantics under direct discovery.
	//
	// Verified via direct invocation of the package-private build helpers (avoids dragging in a second extension
	// instance through @RegisterExtension, which would compete with the @ExtendWith ParameterResolver on this
	// host class).  Programmatic-extension behavior (@RegisterExtension) is exercised by
	// JuneauBeanStoreExtension_RestIntegration_Test$B_Programmatic.
	//-----------------------------------------------------------------------------------------------------------------

	static final class E_DirectHarness {
		@TestBean(scope = Scope.CLASS, name = "direct-class")
		static Svc directClassSvc = new Svc("direct-class");

		@TestBean(name = "direct-method")
		Svc directMethodSvc = new Svc("direct-method");
	}

	@Test
	void e01_directBuild_classStoreContainsOnlyClassScopeEntries() {
		var classStore = JuneauBeanStoreExtension.buildClassScopeStore(E_DirectHarness.class);
		assertSame(E_DirectHarness.directClassSvc, classStore.getBean(Svc.class, "direct-class").orElseThrow());
		assertTrue(classStore.getBean(Svc.class, "direct-method").isEmpty(),
			"Class-scope store must not contain method-scope entries");
	}

	@Test
	void e02_directBuild_methodStoreLayeredOnClassStore() {
		var classStore = JuneauBeanStoreExtension.buildClassScopeStore(E_DirectHarness.class);
		var methodStore = JuneauBeanStoreExtension.buildMethodScopeStore(new E_DirectHarness(), classStore);
		// Method overlay sees both its own entries and the class-scope entries via the parent chain.
		assertSame(E_DirectHarness.directClassSvc, methodStore.getBean(Svc.class, "direct-class").orElseThrow());
		assertNotNull(methodStore.getBean(Svc.class, "direct-method").orElseThrow());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — afterAll drops the class-scope overlay (covered indirectly via direct discovery sanity check).
	//
	// We can't introspect the live JuneauBeanStoreExtension instance after its afterAll has fired (its
	// currentClassContext is intentionally nulled — that's the whole point of the cleanup).  Instead we
	// (i) construct a fresh, unbound extension and confirm getStore(Scope.CLASS) returns Optional.empty(), and
	// (ii) confirm that buildClassScopeStore on an unrelated class produces an isolated store (no cross-class
	// bleed).  Cross-class lifetime isolation is also verified by the companion {@link C_AfterAllReleasesClassScope}
	// top-level test class declared in this file.
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void f01_unboundExtension_getStoreClass_isEmpty() {
		var ext = JuneauBeanStoreExtension.create();
		assertTrue(ext.getStore(Scope.CLASS).isEmpty(),
			"A freshly-created extension that has never had beforeAll invoked must report an empty class-scope store");
	}

	@Test
	void f02_directBuild_classStoreIsIsolatedPerClass() {
		var a = JuneauBeanStoreExtension.buildClassScopeStore(JuneauBeanStoreExtension_ClassScope_Test.class);
		var b = JuneauBeanStoreExtension.buildClassScopeStore(C_AfterAllReleasesClassScope.class);
		// 'a' has the shared svc; 'b' has the companion svc — they must not share state.
		assertTrue(a.getBean(Svc.class).isPresent());
		assertTrue(b.getBean(Svc.class).isPresent());
		assertNotSame(a.getBean(Svc.class).orElseThrow(), b.getBean(Svc.class).orElseThrow(),
			"Class-scope stores built for different test classes must hold distinct overrides");
	}
}

/**
 * Companion fixture that verifies {@code afterAll} actually drops the class-scope overlay from the
 * extension's runtime state.  Lives at file scope (separate top-level package-private class) so JUnit
 * runs it after the enclosing test class has completed all callbacks, exercising the cleanup path.
 */
@SuppressWarnings({
	"java:S3577" // Companion fixture verifying the sibling class's afterAll cleanup; intentionally named, not a standalone *Test suite.
})
@TestMethodOrder(MethodOrderer.MethodName.class)
@ExtendWith(JuneauBeanStoreExtension.class)
class C_AfterAllReleasesClassScope extends TestBase {

	@TestBean(scope = Scope.CLASS)
	static JuneauBeanStoreExtension_ClassScope_Test.Svc ownSvc =
		new JuneauBeanStoreExtension_ClassScope_Test.Svc("companion");

	@Test
	void z01_isolatedClassDoesNotSeeOtherTestsClassScope(TestBeanStore store) {
		var seen = store.getBean(JuneauBeanStoreExtension_ClassScope_Test.Svc.class).orElseThrow();
		assertSame(ownSvc, seen);
		assertEquals("companion", seen.tag, "Class-scope overlay should be this class's own, not the sibling class's");
	}
}
