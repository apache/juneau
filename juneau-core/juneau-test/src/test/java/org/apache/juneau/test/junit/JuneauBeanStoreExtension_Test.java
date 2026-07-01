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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

/**
 * Tests for {@link JuneauBeanStoreExtension} discovery and per-test isolation.
 *
 * <p>
 * Phase 2 covers {@code @TestBean} field/method discovery, per-test overlay isolation, parameter resolution,
 * named-bean qualifiers, and explicit-type overrides.  Class-scope behavior is exercised by
 * {@code JuneauBeanStoreExtension_ClassScope_Test} (Phase 3).
 *
 * <p>
 * The host class itself is wired via {@code @ExtendWith(JuneauBeanStoreExtension.class)} so the lifecycle
 * callbacks (beforeAll/afterAll/beforeEach/afterEach/ParameterResolver) are exercised at runtime by every test.
 * Error-condition tests use the package-private {@code buildClassScopeStore} / {@code buildMethodScopeStore}
 * helpers to drive the discovery layer directly &mdash; that lets them assert on failure messages without
 * spawning a nested JUnit launcher.
 */
@ExtendWith(JuneauBeanStoreExtension.class)
@SuppressWarnings({
	"java:S5778",  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
	"resource" // Bean stores/overlays are Closeables whose lifecycle is owned by the extension/test; Eclipse JDT @Owning warning is by design.
})
class JuneauBeanStoreExtension_Test extends TestBase {

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
	// a — basic field/method discovery (drives full @ExtendWith lifecycle)
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean
	Svc fieldSvc = new Svc("from-field");

	@TestBean
	static OtherSvc staticFieldSvc = new OtherSvc("from-static-field");

	@Test
	void a01_instanceField_isDiscovered(TestBeanStore store) {
		assertSame(fieldSvc, store.getBean(Svc.class).orElseThrow());
	}

	@Test
	void a02_staticField_scopeMethod_isDiscovered(TestBeanStore store) {
		assertSame(staticFieldSvc, store.getBean(OtherSvc.class).orElseThrow());
	}

	@Test
	void a03_parameterResolver_returnsMethodScopeStore(TestBeanStore store) {
		assertNotNull(store);
		assertTrue(store.hasBean(Svc.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — per-test isolation
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_mutationsLeak_doNotCarryOver_partOne(TestBeanStore store) {
		var marker = new Svc("test-method-1");
		store.override(Svc.class, marker, "isolation-marker");
		assertSame(marker, store.getBean(Svc.class, "isolation-marker").orElseThrow());
	}

	@Test
	void b02_mutationsLeak_doNotCarryOver_partTwo(TestBeanStore store) {
		assertTrue(store.getBean(Svc.class, "isolation-marker").isEmpty(),
			"Method-scope overlay must be rebuilt per test; isolation-marker should not leak from previous test");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — named-bean qualifiers
	//-----------------------------------------------------------------------------------------------------------------

	@TestBean(name = "primary")
	Svc primarySvc = new Svc("primary-svc");

	@TestBean(name = "secondary")
	Svc secondarySvc = new Svc("secondary-svc");

	@Test
	void c01_namedBeans_resolveIndependently(TestBeanStore store) {
		assertSame(primarySvc, store.getBean(Svc.class, "primary").orElseThrow());
		assertSame(secondarySvc, store.getBean(Svc.class, "secondary").orElseThrow());
		assertSame(fieldSvc, store.getBean(Svc.class).orElseThrow(),
			"Unnamed lookup should still resolve to the unnamed @TestBean field");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — explicit type override
	//-----------------------------------------------------------------------------------------------------------------

	interface Greeter { String greet(); }
	static final class HelloGreeter implements Greeter { @Override public String greet() { return "hello"; } }

	@TestBean(type = Greeter.class)
	HelloGreeter typedGreeter = new HelloGreeter();

	@Test
	void d01_typeOverride_registersUnderSupertype(TestBeanStore store) {
		assertSame(typedGreeter, store.getBean(Greeter.class).orElseThrow());
		assertTrue(store.getBean(HelloGreeter.class).isEmpty(),
			"Field's declared subtype should NOT be registered when an explicit type override is supplied");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e — method (factory) discovery
	//-----------------------------------------------------------------------------------------------------------------

	private final Svc methodFactoryInstance = new Svc("from-method-factory");

	@TestBean(name = "instance-factory")
	Svc instanceMethodFactory() {
		return methodFactoryInstance;
	}

	@TestBean(name = "static-factory")
	static Svc staticMethodFactory() {
		return new Svc("from-static-factory");
	}

	@Test
	void e01_instanceMethodFactory_isDiscovered(TestBeanStore store) {
		assertSame(methodFactoryInstance, store.getBean(Svc.class, "instance-factory").orElseThrow());
	}

	@Test
	void e02_staticMethodFactory_isDiscovered(TestBeanStore store) {
		var svc = store.getBean(Svc.class, "static-factory").orElseThrow();
		assertEquals("from-static-factory", svc.tag);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — null-value tolerance and inheritance
	//-----------------------------------------------------------------------------------------------------------------

	static class F01_Parent {
		@TestBean(name = "parent-bean") Svc parentSvc = new Svc("parent");
	}

	static final class F01_Child extends F01_Parent {
		@TestBean(name = "child-bean") Svc childSvc = new Svc("child");
	}

	@Test
	void f01_classHierarchy_walkedTopDown_pickingUpBothParentAndChild() {
		var child = new F01_Child();
		var store = JuneauBeanStoreExtension.buildMethodScopeStore(child, null);
		assertSame(child.parentSvc, store.getBean(Svc.class, "parent-bean").orElseThrow());
		assertSame(child.childSvc, store.getBean(Svc.class, "child-bean").orElseThrow());
	}

	static final class F02_NullableField {
		@TestBean Svc nullable = null;
	}

	@Test
	void f02_nullValue_isAllowed() {
		var store = JuneauBeanStoreExtension.buildMethodScopeStore(new F02_NullableField(), null);
		// addBean tolerates null beans (stores a () -> null supplier); TestBeanStore inherits that.
		assertTrue(store.hasBean(Svc.class));
		assertNull(store.getBean(Svc.class).orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — error conditions (driven via direct discovery calls, no launcher dependency required)
	//-----------------------------------------------------------------------------------------------------------------

	static class G01_BadClassScopeOnInstance {
		@TestBean(scope = Scope.CLASS) Svc bad = new Svc("bad");
	}

	@Test
	void g01_classScopeOnInstanceField_throws() {
		var ex = assertThrows(ExtensionContextException.class,
			() -> JuneauBeanStoreExtension.buildClassScopeStore(G01_BadClassScopeOnInstance.class));
		assertTrue(ex.getMessage().contains("@TestBean(scope = CLASS) field must be static"));
		assertTrue(ex.getMessage().contains("bad"));
	}

	static class G02_BadClassScopeOnInstanceMethod {
		@TestBean(scope = Scope.CLASS) Svc factory() { return new Svc("bad"); }
	}

	@Test
	void g02_classScopeOnInstanceMethod_throws() {
		var ex = assertThrows(ExtensionContextException.class,
			() -> JuneauBeanStoreExtension.buildClassScopeStore(G02_BadClassScopeOnInstanceMethod.class));
		assertTrue(ex.getMessage().contains("@TestBean(scope = CLASS) method must be static"));
	}

	static class G03_BadFactoryWithParameters {
		@SuppressWarnings({
			"unused" // Param intentionally present to make this @TestBean factory non-parameterless for the negative test.
		})
		@TestBean Svc factoryWithArgs(String unused) { return new Svc("unreachable"); }
	}

	@Test
	void g03_factoryWithParameters_throws() {
		var ex = assertThrows(ExtensionContextException.class,
			() -> JuneauBeanStoreExtension.buildMethodScopeStore(new G03_BadFactoryWithParameters(), null));
		assertTrue(ex.getMessage().contains("@TestBean factory method must be parameterless"));
	}

	static class G04_TypeMismatch {
		@TestBean(type = OtherSvc.class) Svc mismatched = new Svc("mismatched");
	}

	@Test
	void g04_typeMismatch_throws() {
		var ex = assertThrows(ExtensionContextException.class,
			() -> JuneauBeanStoreExtension.buildMethodScopeStore(new G04_TypeMismatch(), null));
		assertTrue(ex.getMessage().contains("is not assignable to declared type"));
	}

	static class G05_FactoryThrowsException {
		@TestBean Svc factory() { throw new IllegalStateException("kaboom"); }
	}

	@Test
	void g05_factoryThrowsException_wrappedInExtensionContextException() {
		var ex = assertThrows(ExtensionContextException.class,
			() -> JuneauBeanStoreExtension.buildMethodScopeStore(new G05_FactoryThrowsException(), null));
		assertTrue(ex.getMessage().contains("Failed to invoke @TestBean factory method"));
		assertNotNull(ex.getCause());
		assertEquals("kaboom", ex.getCause().getMessage());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// h — getStore(Scope) accessor + getStore() throws when unbound
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void h01_getStore_scope_method_isPopulatedDuringTestMethod(TestBeanStore store) {
		// The injected store is the method-scope store, which must be populated with the host class's
		// @TestBean beans during the test method.
		assertNotNull(store, "Injected method-scope store should be populated during the test method.");
		assertTrue(store.hasBean(Svc.class), "Method-scope store should contain the @TestBean Svc bean during the test method.");

		// The extension is registered on this host class; calling create() yields a new, unbound extension.
		// Asking for either scope on the unbound extension must be empty.
		var unbound = JuneauBeanStoreExtension.create();
		assertTrue(unbound.getStore(Scope.METHOD).isEmpty());
		assertTrue(unbound.getStore(Scope.CLASS).isEmpty());
		assertThrows(IllegalStateException.class, unbound::getStore);
	}

	@Test
	void h02_getStore_scopeArg_null_throws() {
		var ext = JuneauBeanStoreExtension.create();
		assertThrows(NullPointerException.class, () -> ext.getStore(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// i — direct lifecycle drive (covers branches the runtime path doesn't exercise: supportsParameter false,
	//     resolveParameter fallback to class-scope, resolveParameter throw, getStore() with method-scope null
	//     but class-scope non-null).
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void i01_supportsParameter_returnsFalse_forNonTestBeanStoreType() {
		var ext = JuneauBeanStoreExtension.create();
		var pc = new org.apache.juneau.test.junit.testsupport.StubParameterContext(String.class);
		assertFalse(ext.supportsParameter(pc, null));
	}

	@Test
	void i02_supportsParameter_returnsTrue_forTestBeanStoreType() {
		var ext = JuneauBeanStoreExtension.create();
		var pc = new org.apache.juneau.test.junit.testsupport.StubParameterContext(TestBeanStore.class);
		assertTrue(ext.supportsParameter(pc, null));
	}

	@Test
	void i03_resolveParameter_throws_whenNoOverlayActive() {
		var ext = JuneauBeanStoreExtension.create();
		var ctx = org.apache.juneau.test.junit.testsupport.StubExtensionContext.of(Object.class, null);
		assertThrows(ParameterResolutionException.class, () -> ext.resolveParameter(null, ctx));
	}

	@Test
	void i04_resolveParameter_fallsBackToClassScope() {
		var ext = JuneauBeanStoreExtension.create();
		var classCtx = org.apache.juneau.test.junit.testsupport.StubExtensionContext.of(F02_NullableField.class, null);
		ext.beforeAll(classCtx);
		try {
			// No beforeEach -> no method-scope store; resolveParameter must fall back to class-scope.
			var resolved = ext.resolveParameter(null, classCtx);
			assertNotNull(resolved);
			assertInstanceOf(TestBeanStore.class, resolved);
		} finally {
			ext.afterAll(classCtx);
		}
	}

	@Test
	void i05_getStore_returnsClassScope_whenNoMethodScope() {
		var ext = JuneauBeanStoreExtension.create();
		var classCtx = org.apache.juneau.test.junit.testsupport.StubExtensionContext.of(F02_NullableField.class, null);
		ext.beforeAll(classCtx);
		try {
			var store = ext.getStore();
			assertNotNull(store, "getStore() must return the class-scope store when no method scope is active");
		} finally {
			ext.afterAll(classCtx);
		}
	}

	@Test
	void i06_readClassStore_returnsNull_whenNoParentChainHasIt() {
		// Build a method context whose parent chain does NOT contain a class-scope store.
		var ext = JuneauBeanStoreExtension.create();
		var standaloneCtx = org.apache.juneau.test.junit.testsupport.StubExtensionContext.of(F02_NullableField.class,
			new F02_NullableField());
		// Note: no beforeAll was invoked, so the context's store doesn't carry a class-scope entry.
		ext.beforeEach(standaloneCtx);
		try {
			// resolveParameter should succeed (method-scope is present) and not throw.
			var resolved = ext.resolveParameter(null, standaloneCtx);
			assertNotNull(resolved);
		} finally {
			ext.afterEach(standaloneCtx);
		}
	}
}
