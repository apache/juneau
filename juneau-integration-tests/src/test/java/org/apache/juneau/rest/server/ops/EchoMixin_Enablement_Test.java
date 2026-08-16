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
package org.apache.juneau.rest.server.ops;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Proves {@link EchoMixin} reachability is decoupled from the JUL logger level (TODO-372a).
 *
 * <p>
 * Echo is <b>disabled by default</b> and is enabled only by an explicit, non-logging switch:
 * <ul>
 * 	<li>{@link EchoMixin.Builder#enabled(Boolean) builder enablement} (explicit {@code true}/{@code false}), or
 * 	<li>the {@code ${juneau.echo.enabled:false}} deployment fallback (system property / relaxed environment variable).
 * </ul>
 *
 * <p>
 * The gates {@code a01}/{@code a04} are deliberately <b>red on unmodified {@code main}</b>: on {@code main},
 * {@code EchoMixin.echo(...)} unhides the endpoint whenever {@code req.isDebug()} is {@code true} (i.e. the resolved op
 * logger is loggable at {@code FINE}), so raising the host logger to {@code FINE}/{@code FINEST} returns {@code 200}.
 * These tests assert the post-decoupling {@code 404}, which fails against that old coupled behavior.
 *
 * <p>
 * The resolved op logger for a mixin-served op is {@code <hostClass>.<method>} (a JUL child of the host resource's
 * logger), so raising {@code Logger.getLogger(<hostClass>)} controls {@code isDebug()} for the {@code /echo/*} op.
 *
 * @since 10.0.0
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class EchoMixin_Enablement_Test extends TestBase {

	private static final String KEY = "juneau.echo.enabled";

	@FunctionalInterface private interface ThrowingBody { void run() throws Exception; }

	/** Raises {@code hostClass}'s JUL logger to {@code level} for the duration of {@code action}, then restores it. */
	private static void withLoggerLevel(Class<?> hostClass, Level level, ThrowingBody action) throws Exception {
		var logger = Logger.getLogger(hostClass.getName());
		var prev = logger.getLevel();
		logger.setLevel(level);
		try {
			action.run();
		} finally {
			logger.setLevel(prev);
		}
	}

	/** Sets {@code juneau.echo.enabled=value} for the duration of {@code body}, restoring the prior value afterward. */
	private static void withEchoEnabledProperty(String value, ThrowingBody body) throws Exception {
		var prev = System.getProperty(KEY);
		System.setProperty(KEY, value);
		try {
			body.run();
		} finally {
			if (prev == null)
				System.clearProperty(KEY);
			else
				System.setProperty(KEY, prev);
		}
	}

	// -----------------------------------------------------------------------------------------
	// a01 / a04 — Decoupling gates (RED on current main): raising the logger must NOT unhide echo.
	// -----------------------------------------------------------------------------------------

	/** Mixes in echo with no explicit enablement and no {@code @Bean} factory (unset builder → defers to fallback). */
	@Rest(mixins=EchoMixin.class)
	public static class A_UnsetNoFallback extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a01_finestLoggerDoesNotUnhideEcho() throws Exception {
		// RED on main: FINEST → isDebug()==true → old code served 200.  Post-decoupling: 404 (no fallback, unset builder).
		withLoggerLevel(A_UnsetNoFallback.class, Level.FINEST, () -> {
			var body = MockRestClient.buildLax(A_UnsetNoFallback.class)
				.get("/echo/secret?probe=LEAKMARKER")
				.header("X-Probe", "LEAKMARKER")
				.run()
				.assertStatus(404)
				.getContent().asString();
			assertFalse(body.contains("LEAKMARKER"), "Disabled echo must not reflect any request payload; body: " + body);
			assertFalse(body.contains("pathRemainder"), "Disabled echo must not emit an echo payload; body: " + body);
		});
	}

	@Test void a04_fineLoggerDoesNotUnhideEcho() throws Exception {
		// RED on main: FINE → isDebug()==true → old code served 200.  Post-decoupling: 404.
		withLoggerLevel(A_UnsetNoFallback.class, Level.FINE, () -> {
			var body = MockRestClient.buildLax(A_UnsetNoFallback.class)
				.get("/echo/secret?probe=LEAKMARKER")
				.header("X-Probe", "LEAKMARKER")
				.run()
				.assertStatus(404)
				.getContent().asString();
			assertFalse(body.contains("LEAKMARKER"), "Disabled echo must not reflect any request payload; body: " + body);
			assertFalse(body.contains("pathRemainder"), "Disabled echo must not emit an echo payload; body: " + body);
		});
	}

	// -----------------------------------------------------------------------------------------
	// a02 — Explicit builder enablement serves at EVERY logger level (below-INFO, INFO, FINE, FINEST).
	// -----------------------------------------------------------------------------------------

	/** Explicitly enabled via {@code @Bean} factory. */
	@Rest(mixins=EchoMixin.class)
	public static class B_Enabled extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() {
			return EchoMixin.create().enabled().build();
		}
	}

	@Test void a02_builderEnabledServesAtAllLoggerLevels() throws Exception {
		// Includes below-INFO (WARNING) and INFO cells (isDebug()==false) so an "enabled && isDebug()" AND-gate cannot pass.
		var c = MockRestClient.buildLax(B_Enabled.class);
		for (var level : new Level[]{Level.WARNING, Level.INFO, Level.FINE, Level.FINEST}) {
			withLoggerLevel(B_Enabled.class, level, () ->
				c.get("/echo/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json"));
		}
	}

	// -----------------------------------------------------------------------------------------
	// a03 — Global fallback: system property enables an unset builder; relaxed env-var mapping.
	// -----------------------------------------------------------------------------------------

	/** Unset builder — relies on the {@code ${juneau.echo.enabled:false}} fallback. */
	@Rest(mixins=EchoMixin.class)
	public static class C_Unset extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a03a_systemPropertyEnablesUnsetBuilder() throws Exception {
		var c = MockRestClient.buildLax(C_Unset.class);
		// Default (property unset) → disabled → 404.
		c.get("/echo/ping").run().assertStatus(404);
		// Property true → enabled via fallback → 200 (logger left at default INFO: proves it's not logger-driven).
		withEchoEnabledProperty("true", () ->
			c.get("/echo/ping").run().assertStatus(200).assertHeader("Content-Type").isContains("application/json"));
		// Cleared again → back to 404.
		c.get("/echo/ping").run().assertStatus(404);
	}

	@Test void a03b_relaxedEnvVarMapsToLogicalKey() {
		// The harness cannot set a real environment variable, so assert the relaxed mapping used by the
		// default Settings singleton: the logical key resolves JUNEAU_ECHO_ENABLED as an env-var candidate.
		var candidates = RelaxedPropertySource.candidates(KEY);
		assertTrue(candidates.contains("JUNEAU_ECHO_ENABLED"),
			"Relaxed env mapping must probe JUNEAU_ECHO_ENABLED; candidates: " + candidates);
	}

	// -----------------------------------------------------------------------------------------
	// a05 — Explicit enabled(false) overrides a true global fallback, even with the logger raised.
	// -----------------------------------------------------------------------------------------

	/** Explicit {@code enabled(false)} — must win over a true global fallback. */
	@Rest(mixins=EchoMixin.class)
	public static class D_ExplicitFalse extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public EchoMixin echo() {
			return EchoMixin.create().enabled(Boolean.FALSE).build();
		}
	}

	@Test void a05_explicitFalseOverridesTrueFallbackAndLogger() throws Exception {
		var c = MockRestClient.buildLax(D_ExplicitFalse.class);
		withEchoEnabledProperty("true", () ->
			withLoggerLevel(D_ExplicitFalse.class, Level.FINE, () ->
				c.get("/echo/ping").run().assertStatus(404)));
	}

	// -----------------------------------------------------------------------------------------
	// a06 — A true global fallback enables independent hosts; a host guard still rejects.
	// -----------------------------------------------------------------------------------------

	/** Denies unless an {@code X-Admin: yes} header is present. */
	public static class DenyUnlessAdmin extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Admin").orElse(null));
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class E_Unset1 extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixins=EchoMixin.class)
	public static class F_Unset2 extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	/** Unset builder (relies on fallback) but host guards the mixed-in op. */
	@Rest(mixins=EchoMixin.class, guards=DenyUnlessAdmin.class)
	public static class G_Guarded extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void a06_fallbackEnablesIndependentHostsButGuardStillRejects() throws Exception {
		var ce = MockRestClient.buildLax(E_Unset1.class);
		var cf = MockRestClient.buildLax(F_Unset2.class);
		var cg = MockRestClient.buildLax(G_Guarded.class);
		withEchoEnabledProperty("true", () -> {
			// The single process-wide fallback enables echo on two independent hosts.
			ce.get("/echo/ping").run().assertStatus(200);
			cf.get("/echo/ping").run().assertStatus(200);
			// A host guard still authorizes the mixed-in echo op: denied without the header, allowed with it.
			cg.get("/echo/ping").run().assertStatus(403);
			cg.get("/echo/ping").header("X-Admin", "yes").run().assertStatus(200);
		});
	}
}
