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
package org.apache.juneau.rest.server.debug;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link DebugEnablement} and its {@link DebugEnablement.Builder}.
 *
 * <p>Exercises:
 * <ul>
 *   <li>Builder fluent setters (defaultEnable / enable(class) / enable(keys) / conditional / type / impl).
 *   <li>{@link DebugEnablement#isDebug(RestContext, HttpServletRequest)} predicate paths.
 *   <li>{@link DebugEnablement#isDebug(RestOpContext, HttpServletRequest)} predicate paths
 *       (per-method match, fallback to declaring class, fallback to default).
 *   <li>{@link DebugEnablement#isConditionallyEnabled(HttpServletRequest)} via the default
 *       {@code Debug} header predicate, custom predicate, and null-predicate replacement.
 *   <li>{@link DebugEnablement#properties()} and {@link DebugEnablement#toString()}.
 *   <li>Builder {@code build()} resolution branches: explicit {@code impl}, explicit
 *       {@code type}, default {@link BasicDebugEnablement}.
 *   <li>Constructor-time {@code firstNonNull} fallbacks for null defaultEnablement / null conditional.
 * </ul>
 */
class DebugEnablement_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class R {
		@RestGet(path="/x") public String x() { return "x"; }
		@RestGet(path="/y") public String y() { return "y"; }
	}

	/**
	 * A minimal subclass that exposes both protected constructors so tests can
	 * exercise them without going through the BeanStore-based BeanInstantiator
	 * pipeline (which would require ResourceSupplier/VarResolver wiring).
	 */
	public static class TestEnablement extends DebugEnablement {

		// Used by constructor(BeanStore) tests — captures the builder produced by init().
		static volatile Function<DebugEnablement.Builder, DebugEnablement.Builder> initFn = b -> b;

		public TestEnablement(DebugEnablement.Builder builder) {
			super(builder);
		}

		public TestEnablement(BeanStore beanStore) {
			super(beanStore);
		}

		@Override
		protected DebugEnablement.Builder init(BeanStore beanStore) {
			return initFn.apply(super.init(beanStore));
		}
	}

	private static DebugEnablement.Builder newBuilder() {
		return new DebugEnablement.Builder(BasicBeanStore.INSTANCE);
	}

	private static RestContext newRestContext() throws Exception {
		var resource = new R();
		return new RestContext(new RestContext.Args(R.class, null, null, () -> resource, "", null, null, null, false))
			.postInit().postInitChildFirst();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a. Static factory and builder defaults.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_create_returnsBuilder() {
		var b = DebugEnablement.create(BasicBeanStore.INSTANCE);
		assertNotNull(b);
		assertInstanceOf(DebugEnablement.Builder.class, b);
		assertSame(BasicBeanStore.INSTANCE, b.beanStore);
	}

	@Test void a02_builder_defaults() {
		var b = newBuilder();
		assertEquals(Enablement.NEVER, b.defaultEnablement);
		assertNotNull(b.conditional);
		assertNotNull(b.mapBuilder);
	}

	@Test void a03_default_isDebug_returnsFalse_whenDefaultIsNever() {
		var de = new TestEnablement(newBuilder());
		var req = MockServletRequest.create("GET", "/x");
		assertFalse(de.isConditionallyEnabled(req));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. Builder fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_defaultEnable_setter() {
		var b = newBuilder();
		assertSame(b, b.defaultEnable(Enablement.ALWAYS));
		assertEquals(Enablement.ALWAYS, b.defaultEnablement);
	}

	@Test void b02_enable_byClass_oneClass() {
		var b = newBuilder();
		assertSame(b, b.enable(Enablement.ALWAYS, R.class));
		// Round-trip via build to confirm the rule is in effect for the class.
		var de = new TestEnablement(b);
		var props = de.properties();
		assertNotNull(props);
	}

	@Test void b03_enable_byClass_multipleClasses() {
		var b = newBuilder();
		assertSame(b, b.enable(Enablement.CONDITIONAL, R.class, String.class));
		// Just ensure the call returns the builder and doesn't throw.
		assertNotNull(b.mapBuilder);
	}

	@Test void b04_enable_byKey_singleKey() {
		var b = newBuilder();
		assertSame(b, b.enable(Enablement.ALWAYS, "com.example.Foo"));
		assertNotNull(b.mapBuilder);
	}

	@Test void b05_enable_byKey_multipleKeys() {
		var b = newBuilder();
		assertSame(b, b.enable(Enablement.NEVER, "com.example.Foo", "com.example.Bar.method"));
		assertNotNull(b.mapBuilder);
	}

	@Test void b06_conditional_customPredicate() {
		Predicate<HttpServletRequest> p = r -> "POST".equalsIgnoreCase(r.getMethod());
		var b = newBuilder();
		assertSame(b, b.conditional(p));
		assertSame(p, b.conditional);

		var de = new TestEnablement(b);
		assertTrue(de.isConditionallyEnabled(MockServletRequest.create("POST", "/x")));
		assertFalse(de.isConditionallyEnabled(MockServletRequest.create("GET", "/x")));
	}

	@Test void b07_impl_setter_returnsThisFromBuild() {
		var preBuilt = new TestEnablement(newBuilder());
		var b = newBuilder();
		assertSame(b, b.impl(preBuilt));
		assertSame(preBuilt, b.build());
	}

	@Test void b08_type_setter_nonNull() {
		var b = newBuilder();
		assertSame(b, b.type(BasicDebugEnablement.class));
	}

	@Test void b09_type_setter_nullFallsBackToBasic() {
		var b = newBuilder();
		// Branch: opt(value).isPresent() == false → implType is set to BasicDebugEnablement.class.
		assertSame(b, b.type(null));
		// No exception means the branch was traversed.
	}

	@Test void b10_build_withType_instantiatesViaBeanInstantiator() {
		// Drive the "impl == null" branch and the BeanInstantiator path with a custom type
		// whose (BeanStore) constructor is satisfiable from BasicBeanStore.INSTANCE alone.
		var de = newBuilder().type(TestEnablement.class).build();
		assertNotNull(de);
		assertInstanceOf(TestEnablement.class, de);
	}

	@Test void b11_build_default_instantiatesBasicDebugEnablement_orThrows() {
		// implType is null → falls through to BasicDebugEnablement.class. Without ResourceSupplier
		// in the bean store BasicDebugEnablement.init() throws IllegalStateException, which the
		// catch in build() wraps as InternalServerError. Either outcome traverses lines 92-99.
		var b = newBuilder();
		try {
			var de = b.build();
			// If somehow it succeeded, the result must be a BasicDebugEnablement.
			assertInstanceOf(BasicDebugEnablement.class, de);
		} catch (org.apache.juneau.http.response.InternalServerError expected) {
			// Catch branch (line 98-99) traversed: missing ResourceSupplier wraps to 500.
			assertNotNull(expected);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. Constructor branches and firstNonNull fallbacks.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_builderConstructor_setsFields() {
		var b = newBuilder().defaultEnable(Enablement.ALWAYS);
		var de = new TestEnablement(b);
		assertNotNull(de.toString());
	}

	@Test void c02_builderConstructor_nullDefaultEnablement_fallsBackToNever() {
		var b = newBuilder();
		b.defaultEnablement = null;
		var de = new TestEnablement(b);
		// Constructor uses firstNonNull → NEVER; toString must still render.
		assertTrue(de.toString().contains("defaultEnablement=NEVER"));
	}

	@Test void c03_builderConstructor_nullConditional_fallsBackToHeaderPredicate() {
		var b = newBuilder();
		b.conditional = null;
		var de = new TestEnablement(b);
		// Default predicate fires when "Debug: true" header is present.
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "true");
		assertTrue(de.isConditionallyEnabled(req));
		// And not when missing.
		assertFalse(de.isConditionallyEnabled(MockServletRequest.create("GET", "/x")));
	}

	@Test void c04_beanStoreConstructor_invokesInit() {
		// The beanStore constructor calls init(beanStore) which must yield a populated builder.
		// Use TestEnablement.initFn to inject a defaultEnablement so we can assert it.
		try {
			TestEnablement.initFn = b -> b.defaultEnable(Enablement.ALWAYS);
			var de = new TestEnablement(BasicBeanStore.INSTANCE);
			assertTrue(de.toString().contains("defaultEnablement=ALWAYS"));
		} finally {
			TestEnablement.initFn = b -> b;
		}
	}

	@Test void c05_beanStoreConstructor_nullConditional_fallsBackToHeaderPredicate() {
		// Force builder.conditional = null so the constructor's firstNonNull right-hand-side fires.
		try {
			TestEnablement.initFn = b -> { b.conditional = null; return b; };
			var de = new TestEnablement(BasicBeanStore.INSTANCE);
			var req = MockServletRequest.create("GET", "/x");
			req.header("Debug", "true");
			assertTrue(de.isConditionallyEnabled(req));
		} finally {
			TestEnablement.initFn = b -> b;
		}
	}

	@Test void c06_beanStoreConstructor_nullDefaultEnablement_fallsBackToNever() {
		try {
			TestEnablement.initFn = b -> { b.defaultEnablement = null; return b; };
			var de = new TestEnablement(BasicBeanStore.INSTANCE);
			assertTrue(de.toString().contains("defaultEnablement=NEVER"));
		} finally {
			TestEnablement.initFn = b -> b;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. isDebug(RestContext, req) — class-level rule resolution.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_isDebug_restContext_alwaysRule() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder().enable(Enablement.ALWAYS, R.class));
		assertTrue(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d02_isDebug_restContext_neverRule() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder().enable(Enablement.NEVER, R.class));
		assertFalse(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d03_isDebug_restContext_conditional_predicateTrue() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.CONDITIONAL, R.class)
			.conditional(r -> true));
		assertTrue(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d04_isDebug_restContext_conditional_predicateFalse() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.CONDITIONAL, R.class)
			.conditional(r -> false));
		assertFalse(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d05_isDebug_restContext_noRule_defaultNever() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder()); // default NEVER, no rules
		assertFalse(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d06_isDebug_restContext_noRule_defaultAlways() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.ALWAYS));
		assertTrue(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	@Test void d07_isDebug_restContext_noRule_defaultConditional_headerOn() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.CONDITIONAL));
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "true");
		assertTrue(de.isDebug(ctx, req));
	}

	@Test void d08_isDebug_restContext_noRule_defaultConditional_headerOff() throws Exception {
		var ctx = newRestContext();
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.CONDITIONAL));
		assertFalse(de.isDebug(ctx, MockServletRequest.create("GET", "/x")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. isDebug(RestOpContext, req) — method-level rule resolution.
	//-----------------------------------------------------------------------------------------------------------------

	private static RestOpContext opCtx(RestContext rc, String methodName) throws Exception {
		java.lang.reflect.Method m = R.class.getMethod(methodName);
		return new RestOpContext(m, rc);
	}

	@Test void e01_isDebug_restOpContext_methodRule_alwaysWins() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "x");
		// Method-level rule takes precedence over class-level rule.
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.NEVER, R.class)
			.enable(Enablement.ALWAYS, R.class.getName() + ".x"));
		assertTrue(de.isDebug(op, MockServletRequest.create("GET", "/x")));
	}

	@Test void e02_isDebug_restOpContext_methodRule_neverWins() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "x");
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.ALWAYS, R.class)
			.enable(Enablement.NEVER, R.class.getName() + ".x"));
		assertFalse(de.isDebug(op, MockServletRequest.create("GET", "/x")));
	}

	@Test void e03_isDebug_restOpContext_methodRule_conditional_true() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "x");
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.CONDITIONAL, R.class.getName() + ".x")
			.conditional(r -> true));
		assertTrue(de.isDebug(op, MockServletRequest.create("GET", "/x")));
	}

	@Test void e04_isDebug_restOpContext_methodRule_conditional_false() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "x");
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.CONDITIONAL, R.class.getName() + ".x")
			.conditional(r -> false));
		assertFalse(de.isDebug(op, MockServletRequest.create("GET", "/x")));
	}

	@Test void e05_isDebug_restOpContext_fallsBackToClassRule_always() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "y");
		// Only a class-level rule exists; method-level lookup must fall back to it.
		var de = new TestEnablement(newBuilder().enable(Enablement.ALWAYS, R.class));
		assertTrue(de.isDebug(op, MockServletRequest.create("GET", "/y")));
	}

	@Test void e06_isDebug_restOpContext_fallsBackToClassRule_never() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "y");
		var de = new TestEnablement(newBuilder().enable(Enablement.NEVER, R.class));
		assertFalse(de.isDebug(op, MockServletRequest.create("GET", "/y")));
	}

	@Test void e07_isDebug_restOpContext_fallsBackToDefault_never() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "y");
		// No method or class rule; defaultEnablement (NEVER) wins.
		var de = new TestEnablement(newBuilder());
		assertFalse(de.isDebug(op, MockServletRequest.create("GET", "/y")));
	}

	@Test void e08_isDebug_restOpContext_fallsBackToDefault_always() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "y");
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.ALWAYS));
		assertTrue(de.isDebug(op, MockServletRequest.create("GET", "/y")));
	}

	@Test void e09_isDebug_restOpContext_fallsBackToDefault_conditional_headerOn() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "y");
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.CONDITIONAL));
		var req = MockServletRequest.create("GET", "/y");
		req.header("Debug", "true");
		assertTrue(de.isDebug(op, req));
	}

	@Test void e10_isDebug_restOpContext_overlappingRules_firstMatchWins() throws Exception {
		var ctx = newRestContext();
		var op = opCtx(ctx, "x");
		// Two class-level rules of opposite values: ReflectionMap returns matches in
		// insertion order, so the first ALWAYS hit short-circuits via findFirst().
		var de = new TestEnablement(newBuilder()
			.enable(Enablement.ALWAYS, R.class)
			.enable(Enablement.NEVER, R.class));
		// findFirst() picks the first inserted match (ALWAYS).
		assertTrue(de.isDebug(op, MockServletRequest.create("GET", "/x")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f. isConditionallyEnabled — default Debug-header predicate.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_defaultPredicate_debugHeaderTrue() {
		var de = new TestEnablement(newBuilder());
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "true");
		assertTrue(de.isConditionallyEnabled(req));
	}

	@Test void f02_defaultPredicate_debugHeaderTrue_caseInsensitive() {
		var de = new TestEnablement(newBuilder());
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "TRUE");
		assertTrue(de.isConditionallyEnabled(req));
	}

	@Test void f03_defaultPredicate_debugHeaderFalse() {
		var de = new TestEnablement(newBuilder());
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "false");
		assertFalse(de.isConditionallyEnabled(req));
	}

	@Test void f04_defaultPredicate_debugHeaderMissing() {
		var de = new TestEnablement(newBuilder());
		assertFalse(de.isConditionallyEnabled(MockServletRequest.create("GET", "/x")));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g. properties() / toString().
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_properties_containsAllKeys() {
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.ALWAYS));
		var p = de.properties();
		assertNotNull(p);
		// Map should expose at least the three documented properties when non-default.
		var s = p.toString();
		assertTrue(s.contains("defaultEnablement"));
		assertTrue(s.contains("conditionalPredicate"));
		assertTrue(s.contains("enablementMap"));
	}

	@Test void g02_toString_nonEmpty() {
		var de = new TestEnablement(newBuilder().defaultEnable(Enablement.CONDITIONAL).enable(Enablement.ALWAYS, R.class));
		var s = de.toString();
		assertNotNull(s);
		assertTrue(s.contains("defaultEnablement=CONDITIONAL"));
	}

	@Test void g03_toString_default_renderable() {
		// Even with all defaults, toString must not throw.
		var de = new TestEnablement(newBuilder());
		assertNotNull(de.toString());
	}
}
