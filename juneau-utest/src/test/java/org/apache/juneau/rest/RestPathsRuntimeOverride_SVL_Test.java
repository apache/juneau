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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.junit5.*;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Validates SVL substitution applied per-element to {@code @Rest(paths=...)} annotation literals.
 *
 * <p>
 * Each {@code @Rest(paths=...)} element runs through SVL substitution and the post-SVL value is split on
 * {@code ,} (trim each piece, drop empties).  This covers the four variable backends that matter for
 * runtime path substitution:
 * <ul>
 * 	<li>{@code $C{key}} &mdash; Juneau {@link Config} (requires a Config bean registered in the bean store).
 * 	<li>{@code $E{NAME,default}} &mdash; environment variable with literal default.
 * 	<li>{@code $S{prop,default}} &mdash; system property with literal default.
 * 	<li>An unresolved SVL reference falls back to the literal element (no exception during construction).
 * </ul>
 *
 * @since 9.5.0
 */
class RestPathsRuntimeOverride_SVL_Test extends TestBase {

	private static Config inMemoryConfig(String name, String...kvLines) {
		var store = MemoryStore.create().build();
		var sb = new StringBuilder();
		for (var line : kvLines)
			sb.append(line).append('\n');
		store.write(name, null, sb.toString());
		return Config.create().store(store).name(name).build();
	}

	private static TestBeanStore overlayWith(Config c) {
		return new TestBeanStore().override(Config.class, c);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a — $C{key}: Juneau Config lookup applied per element
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"$C{health.paths}"})
	public static class A_ConfigKey {}

	@Test
	void a01_configKey_resolvesAndCommaSplits() throws Exception {
		// Single element wraps a Config lookup; the resolved value contains a comma so the post-SVL split
		// produces two mount paths.
		var cfg = inMemoryConfig("a01.cfg", "health.paths = /healthz, /readyz");
		var overlay = overlayWith(cfg);

		var args = new RestContext.Args(A_ConfigKey.class, null, null, A_ConfigKey::new, "", null, overlay, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/healthz", "/readyz"}, ctx.getPaths(),
			"$C{health.paths} should resolve via Config and the multi-value result should comma-split");
	}

	@Rest(paths={"/api", "$C{extra.paths}"})
	public static class B_LiteralMixedWithConfig {}

	@Test
	void b01_literalMixedWithConfigKey_bothMount() throws Exception {
		// First element is a literal path; second element pulls a multi-value Config string.  Total: 3 mounts.
		var cfg = inMemoryConfig("b01.cfg", "extra.paths = /probes/live, /probes/ready");
		var overlay = overlayWith(cfg);

		var args = new RestContext.Args(B_LiteralMixedWithConfig.class, null, null, B_LiteralMixedWithConfig::new, "", null, overlay, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/api", "/probes/live", "/probes/ready"}, ctx.getPaths(),
			"Mixing a literal element with a $C{...} element should produce all of: literal + each comma-split piece");
	}

	@Rest(paths={"$C{paths.missing,/from-default}"})
	public static class C_ConfigMissWithDefault {}

	@Test
	void c01_configMissWithDefault_usesDefault() throws Exception {
		// Config has no `paths.missing` key — the $C{key,default} form falls back to the literal default
		// after the comma.  This is the supported way to provide an in-annotation fallback.
		var cfg = inMemoryConfig("c01.cfg", "some.other.key = /not-this");
		var overlay = overlayWith(cfg);

		var args = new RestContext.Args(C_ConfigMissWithDefault.class, null, null, C_ConfigMissWithDefault::new, "", null, overlay, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/from-default"}, ctx.getPaths(),
			"$C{missing,/default} should fall back to the literal default after the comma");
	}

	@Rest(paths={"$C{paths.missing}"})
	public static class C2_ConfigMissNoDefault {}

	@Test
	void c02_configMissNoDefault_dropsToEmpty() throws Exception {
		// Config has no `paths.missing` key and no in-annotation default — SVL resolves to "" and the
		// comma-split drops the empty piece.  Final result: zero mounts.  This matches how every other
		// SVL-bearing @Rest member treats unresolved variables (empty substitution).
		var cfg = inMemoryConfig("c02.cfg", "some.other.key = /not-this");
		var overlay = overlayWith(cfg);

		var args = new RestContext.Args(C2_ConfigMissNoDefault.class, null, null, C2_ConfigMissNoDefault::new, "", null, overlay, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertNotNull(ctx.getPaths());
		assertEquals(0, ctx.getPaths().length,
			"Unresolved $C{key} with no default should produce zero mounts (the empty SVL substitution gets dropped by the comma-split)");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — $E{NAME,default}: env-var lookup with literal default per element
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"$E{TODO73_NEVER_SET_HEALTH_PATHS,/healthz}"})
	public static class D_EnvDefault {}

	@Test
	void d01_envVar_unset_fallsBackToLiteralDefault() throws Exception {
		// Env var should not be set on the test JVM; the $E{...,default} form resolves to "/healthz".
		var args = new RestContext.Args(D_EnvDefault.class, null, null, D_EnvDefault::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/healthz"}, ctx.getPaths(),
			"$E{NAME,/default} with NAME unset should resolve to /default");
	}

	@Rest(paths={"$E{TODO73_NEVER_SET_HEALTH_PATHS,/healthz,/readyz}"})
	public static class E_EnvDefaultMulti {}

	@Test
	void e01_envVar_unset_defaultWithCommas_splitsAfterResolve() throws Exception {
		// $E{NAME,a,b,c} returns "a,b,c" (the SVL var concatenates everything after the first comma as the
		// default).  Then the outer comma-split fires and produces 2 mount paths.
		var args = new RestContext.Args(E_EnvDefaultMulti.class, null, null, E_EnvDefaultMulti::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/healthz", "/readyz"}, ctx.getPaths(),
			"Env-var literal default with commas should expand to multiple mount paths after the post-SVL split");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// f — $S{prop,default}: system-property lookup with literal default per element
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"$S{juneau.todo73.svl.test.prop,/fallback}"})
	public static class F_SysProp {}

	@Test
	void f01_systemProperty_resolvesAndCommaSplits() throws Exception {
		var key = "juneau.todo73.svl.test.prop";
		System.setProperty(key, "/sys-a,/sys-b");
		try {
			var args = new RestContext.Args(F_SysProp.class, null, null, F_SysProp::new, "", null, null, null);
			var ctx = new RestContext(args).postInit().postInitChildFirst();

			assertArrayEquals(new String[]{"/sys-a", "/sys-b"}, ctx.getPaths(),
				"$S{key,/fallback} should resolve from the system property and comma-split when set");
		} finally {
			System.clearProperty(key);
		}
	}

	@Test
	void f02_systemProperty_unset_fallsBackToDefault() throws Exception {
		// Property is not set; $S{key,/fallback} returns "/fallback".
		var args = new RestContext.Args(F_SysProp.class, null, null, F_SysProp::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/fallback"}, ctx.getPaths(),
			"Unset system property should fall back to the literal default after the comma");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// g — pure literal annotation: SVL is a no-op, behavior unchanged from pre-rework
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(paths={"/static-1", "/static-2"})
	public static class G_PureLiteral {}

	@Test
	void g01_pureLiteralPaths_passThroughUnchanged() throws Exception {
		var args = new RestContext.Args(G_PureLiteral.class, null, null, G_PureLiteral::new, "", null, null, null);
		var ctx = new RestContext(args).postInit().postInitChildFirst();

		assertArrayEquals(new String[]{"/static-1", "/static-2"}, ctx.getPaths(),
			"Elements with no SVL syntax should pass through unchanged (comma-split is a no-op on single segments)");
	}
}
