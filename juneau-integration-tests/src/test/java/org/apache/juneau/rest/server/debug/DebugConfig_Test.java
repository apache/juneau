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
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.debug.format.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Tests for {@link DebugConfig} and {@link DebugConfig.Builder}.
 *
 * <p>Exercises:
 * <ul>
 *   <li>Static factory and builder defaults.
 *   <li>Builder fluent setters (defaultFormat / defaultLevel / conditional / defaultCacheBodies / rule).
 *   <li>{@link DebugConfig#resolve(RestContext, HttpServletRequest)} branches:
 *       null context, request "Debug" attribute, conditional predicate match,
 *       no enablement signal at all.
 *   <li>{@link DebugConfig#resolve(RestOpContext, HttpServletRequest)} method-level branch.
 *   <li>{@code defaultCacheBodies} interaction with the resolved enablement.
 *   <li>{@code beanStore()} accessor and bean-store-based constructor.
 *   <li>BasicTextFormat resolution from the bean store at builder-construction time.
 * </ul>
 *
 * <p>{@link DebugConfig.Builder#rule(String, Consumer)} stores the
 * configured rule on the builder; the produced {@code DebugConfig} exposes it via
 * {@link DebugConfig#getRuleFor(String)}.
 */
class DebugConfig_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Test fixtures
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class R {
		@RestGet(path = "/x") public String x() { return "x"; }
	}

	private static DebugConfig.Builder newBuilder() {
		return DebugConfig.create(BasicBeanStore.INSTANCE);
	}

	private static RestContext newRestContext() throws Exception {
		var resource = new R();
		return new RestContext(new RestContext.Args(R.class, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	private static RestOpContext opCtx(RestContext rc, String methodName) throws Exception {
		var m = R.class.getMethod(methodName);
		return new RestOpContext(m, rc);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a. Static factory and builder defaults.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_create_returnsBuilder() {
		var b = DebugConfig.create(BasicBeanStore.INSTANCE);
		assertNotNull(b);
		assertInstanceOf(DebugConfig.Builder.class, b);
	}

	@Test void a02_builderDefaults_buildSucceedsWithSensibleDefaults() {
		var cfg = newBuilder().build();
		assertNotNull(cfg);
		// Default-conditional predicate looks at the "Debug" header.
		var req = MockServletRequest.create("GET", "/x");
		var r1 = cfg.resolve((RestContext) null, req);
		assertFalse(r1.enabled(), "no Debug header => disabled");
		assertNotNull(r1.format(), "default format must be installed");
		assertInstanceOf(BasicTextFormat.class, r1.format());
		assertEquals(Level.INFO, r1.level());
		assertFalse(r1.cacheBodies());
	}

	@Test void a03_default_format_resolvedFromBeanStore() {
		// When BasicTextFormat is registered as a bean, the builder should pick it up.
		var preset = new BasicTextFormat();
		var bs = new BasicBeanStore();
		bs.addBean(BasicTextFormat.class, preset);
		var cfg = DebugConfig.create(bs).build();
		assertSame(preset, cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).format());
	}

	@Test void a04_default_format_fallback_whenBeanAbsent() {
		// BasicBeanStore.INSTANCE has no BasicTextFormat preconfigured: builder
		// should orElseGet() a fresh BasicTextFormat instance.
		var cfg = DebugConfig.create(new BasicBeanStore()).build();
		assertInstanceOf(BasicTextFormat.class,
			cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).format());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b. Builder fluent setters.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_defaultFormat_setter() {
		DebugFormat custom = ctx -> "x";
		var b = newBuilder();
		assertSame(b, b.defaultFormat(custom));
		var cfg = b.build();
		assertSame(custom, cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).format());
	}

	@Test void b02_defaultLevel_setter() {
		var b = newBuilder();
		assertSame(b, b.defaultLevel(Level.WARNING));
		var cfg = b.build();
		assertEquals(Level.WARNING, cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).level());
	}

	@Test void b03_conditional_setter_customPredicate_true() {
		Predicate<HttpServletRequest> alwaysTrue = r -> true;
		var b = newBuilder();
		assertSame(b, b.conditional(alwaysTrue));
		var cfg = b.build();
		assertTrue(cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).enabled());
	}

	@Test void b04_conditional_setter_customPredicate_methodGated() {
		Predicate<HttpServletRequest> isPost = r -> "POST".equalsIgnoreCase(r.getMethod());
		var cfg = newBuilder().conditional(isPost).build();
		assertTrue(cfg.resolve((RestContext) null, MockServletRequest.create("POST", "/")).enabled());
		assertFalse(cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/")).enabled());
	}

	@Test void b05_defaultCacheBodies_setterTrue() {
		var b = newBuilder();
		assertSame(b, b.defaultCacheBodies(true));
		var cfg = b.conditional(r -> true).build();
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertTrue(res.enabled());
		assertTrue(res.cacheBodies(), "cacheBodies = defaultCacheBodies AND enabled");
	}

	@Test void b06_defaultCacheBodies_setterFalse() {
		var cfg = newBuilder().defaultCacheBodies(false).conditional(r -> true).build();
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertTrue(res.enabled());
		assertFalse(res.cacheBodies());
	}

	@Test void b07_defaultCacheBodies_falseWhenNotEnabled() {
		// cacheBodies AND-gates with enabled — set true on the builder but a false predicate must squash it.
		var cfg = newBuilder().defaultCacheBodies(true).conditional(r -> false).build();
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertFalse(res.enabled());
		assertFalse(res.cacheBodies());
	}

	@Test void b08_rule_returnsThisAndAcceptsConfigurer() {
		// rule(target, consumer) stores the built rule under the target key and is
		// retrievable via DebugConfig.getRuleFor(target).
		var consumed = new boolean[]{false};
		var b = newBuilder();
		var ret = b.rule("com.example.Foo", rb -> {
			consumed[0] = true;
			rb.always().level(Level.FINE);
		});
		assertSame(b, ret);
		assertTrue(consumed[0], "rule(target, consumer) must invoke the consumer");
		var cfg = b.build();
		var rule = cfg.getRuleFor("com.example.Foo");
		assertNotNull(rule, "rule must be retrievable via getRuleFor");
		assertEquals(Level.FINE, rule.getLevel());
		assertNull(cfg.getRuleFor("com.example.Bar"), "no rule should be returned for unconfigured target");
	}

	@Test void b09_chainedBuilder_allFluent() {
		DebugFormat custom = ctx -> "y";
		var cfg = newBuilder()
			.defaultFormat(custom)
			.defaultLevel(Level.FINE)
			.conditional(r -> true)
			.defaultCacheBodies(true)
			.build();
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertTrue(res.enabled());
		assertSame(custom, res.format());
		assertEquals(Level.FINE, res.level());
		assertTrue(res.cacheBodies());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c. resolve(RestContext, req) — class-level path.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_resolve_restContext_nullContext_isAllowed() {
		// resolve(RestContext) explicitly handles null context by passing a null resourceClass.
		var cfg = newBuilder().build();
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertNotNull(res);
		assertFalse(res.enabled());
	}

	@Test void c02_resolve_restContext_withLiveContext() throws Exception {
		var ctx = newRestContext();
		var cfg = newBuilder().build();
		var res = cfg.resolve(ctx, MockServletRequest.create("GET", "/x"));
		assertNotNull(res);
		assertFalse(res.enabled(), "no Debug header / no attribute => disabled");
	}

	@Test void c03_resolve_restContext_debugHeaderTrue_enables() throws Exception {
		var ctx = newRestContext();
		var cfg = newBuilder().build();
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "true");
		var res = cfg.resolve(ctx, req);
		assertTrue(res.enabled(), "default conditional matches 'Debug: true' header");
	}

	@Test void c04_resolve_restContext_debugAttributeTrue_enables() throws Exception {
		// The internal resolver checks req.getAttribute("Debug") before falling through
		// to the conditional predicate.
		var ctx = newRestContext();
		var cfg = newBuilder().conditional(r -> false).build();
		var req = MockServletRequest.create("GET", "/x");
		req.attribute("Debug", Boolean.TRUE);
		var res = cfg.resolve(ctx, req);
		assertTrue(res.enabled(), "Debug attribute=true short-circuits before predicate");
	}

	@Test void c05_resolve_restContext_debugAttributeFalse_fallsThroughToPredicate() throws Exception {
		var ctx = newRestContext();
		var cfg = newBuilder().conditional(r -> true).build();
		var req = MockServletRequest.create("GET", "/x");
		req.attribute("Debug", Boolean.FALSE);
		var res = cfg.resolve(ctx, req);
		// isTrue(false) is false → predicate runs → true.
		assertTrue(res.enabled());
	}

	@Test void c06_resolve_restContext_debugAttributeNull_fallsThroughToPredicate() throws Exception {
		var ctx = newRestContext();
		var cfg = newBuilder().conditional(r -> true).build();
		var res = cfg.resolve(ctx, MockServletRequest.create("GET", "/x"));
		assertTrue(res.enabled(), "absent attribute falls through to conditional predicate");
	}

	@Test void c07_resolve_restContext_nullRequest_resolvesDisabled() {
		// When req is null the resolver skips both the attribute check and predicate.
		var cfg = newBuilder().conditional(r -> true).defaultCacheBodies(true).build();
		var res = cfg.resolve((RestContext) null, null);
		assertNotNull(res);
		assertFalse(res.enabled(), "null request must not call the predicate");
		assertFalse(res.cacheBodies(), "no enablement => no body caching");
	}

	@Test void c08_resolve_restContext_predicateFalse_disabled() throws Exception {
		var ctx = newRestContext();
		var cfg = newBuilder().conditional(r -> false).build();
		assertFalse(cfg.resolve(ctx, MockServletRequest.create("GET", "/x")).enabled());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d. resolve(RestOpContext, req) — method-level path.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_resolve_restOpContext_basic() throws Exception {
		var rc = newRestContext();
		var op = opCtx(rc, "x");
		var cfg = newBuilder().build();
		var res = cfg.resolve(op, MockServletRequest.create("GET", "/x"));
		assertNotNull(res);
		assertFalse(res.enabled());
	}

	@Test void d02_resolve_restOpContext_debugHeaderEnables() throws Exception {
		var rc = newRestContext();
		var op = opCtx(rc, "x");
		var cfg = newBuilder().build();
		var req = MockServletRequest.create("GET", "/x");
		req.header("Debug", "true");
		assertTrue(cfg.resolve(op, req).enabled());
	}

	@Test void d03_resolve_restOpContext_debugAttribute_shortCircuits() throws Exception {
		var rc = newRestContext();
		var op = opCtx(rc, "x");
		var cfg = newBuilder().conditional(r -> false).build();
		var req = MockServletRequest.create("GET", "/x");
		req.attribute("Debug", Boolean.TRUE);
		assertTrue(cfg.resolve(op, req).enabled());
	}

	@Test void d04_resolve_restOpContext_cacheBodiesPropagates() throws Exception {
		var rc = newRestContext();
		var op = opCtx(rc, "x");
		var cfg = newBuilder().defaultCacheBodies(true).conditional(r -> true).build();
		var res = cfg.resolve(op, MockServletRequest.create("GET", "/x"));
		assertTrue(res.enabled());
		assertTrue(res.cacheBodies());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// e. Constructor variants and accessor.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_beanStoreConstructor_initializesViaBuilder() {
		// public DebugConfig(BeanStore) delegates to create(beanStore).
		var cfg = new DebugConfig(BasicBeanStore.INSTANCE);
		assertNotNull(cfg);
		var res = cfg.resolve((RestContext) null, MockServletRequest.create("GET", "/"));
		assertNotNull(res);
		assertEquals(Level.INFO, res.level());
		assertInstanceOf(BasicTextFormat.class, res.format());
	}

	@Test void e02_beanStore_accessor_returnsConstructorBeanStore() {
		// Subclass to expose the protected beanStore() method.
		class Exposed extends DebugConfig {
			Exposed(BeanStore bs) { super(bs); }
			BeanStore exposeBeanStore() { return beanStore(); }
		}
		var bs = new BasicBeanStore();
		var cfg = new Exposed(bs);
		assertSame(bs, cfg.exposeBeanStore());
	}

	@Test void e03_protectedBuilderConstructor_canBeSubclassed() {
		// Confirms the protected DebugConfig(Builder) constructor is reachable.
		class Sub extends DebugConfig {
			Sub(DebugConfig.Builder b) { super(b); }
		}
		var sub = new Sub(newBuilder());
		assertNotNull(sub);
		assertNotNull(sub.resolve((RestContext) null, MockServletRequest.create("GET", "/")));
	}
}
