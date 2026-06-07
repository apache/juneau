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
package org.apache.juneau.rest.server;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * Validates SVL substitution applied to op-level path declarations.
 *
 * <p>
 * Closes the asymmetry where class-level {@link Rest#path() @Rest(path)} /
 * {@link Rest#paths() @Rest(paths)} were SVL-resolved but op-level
 * {@link RestGet#path() @RestGet(path)} / {@link RestPost#path() @RestPost(path)} /
 * {@link RestOp#path() @RestOp(path)} / {@link RestOp#value() @RestOp(value)} were not.
 *
 * <p>
 * Each test uses a <i>distinct</i> resource class because {@link MockRestClient} caches
 * {@link RestContext} by resource class &mdash; SVL substitution happens at context-construction
 * time, so changing a system property and re-building MockRestClient with the same class hits the
 * cached context (with the original resolution) and would silently mask the override.
 *
 * <p>
 * The {@code ${name:default}} shortcut form is used throughout: the dollar-brace shortcut is
 * registered as {@link org.apache.juneau.commons.svl.vars.PropertyVar PropertyVar}, which reads
 * from {@link org.apache.juneau.commons.settings.Settings Settings.get()} (which falls back to
 * system properties), and the first top-level {@code :} is rewritten to {@code ,} so it matches
 * {@link org.apache.juneau.commons.svl.DefaultingVar DefaultingVar}'s {@code key,default}
 * separator.
 *
 * @since 10.0.0
 */
class RestOpContext_SvlInOpPath_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a01 — @RestGet(path) with a single ${name:default} element, default branch (no sysprop set)
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A01_DefaultPath {
		@RestGet(path="/${a01.test.path:a01-default}/*")
		public String hello() { return "a01-hit"; }
	}

	@Test void a01_singleSvlPath_defaultBranch_mountsAtDefault() throws Exception {
		// No system property is set; the ${...:default} form falls back to the literal default after
		// the rewritten ',' separator. Mount path resolves to "/a01-default/*".
		var c = MockRestClient.buildLax(A01_DefaultPath.class);

		c.get("/a01-default/anything")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("a01-hit");

		// And the default-branch path is the only mount — the unresolved SVL placeholder is NOT a
		// fallback path. Hitting "/a01-override/foo" with no sysprop set should miss.
		c.get("/a01-override/anything")
			.run()
			.assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a02 — same op shape, but a different class so the per-class RestContext cache doesn't bleed; the
	//        system property is set BEFORE MockRest build so SVL substitution captures the override.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A02_OverridePath {
		@RestGet(path="/${a02.test.path:a02-default}/*")
		public String hello() { return "a02-hit"; }
	}

	@Test void a02_singleSvlPath_systemPropertyOverride_mountsAtOverride() throws Exception {
		var key = "a02.test.path";
		System.setProperty(key, "a02-override");
		try {
			var c = MockRestClient.buildLax(A02_OverridePath.class);

			c.get("/a02-override/anything")
				.run()
				.assertStatus(200)
				.assertContent().asString().isContains("a02-hit");

			// And the default-branch mount no longer exists.
			c.get("/a02-default/anything")
				.run()
				.assertStatus(404);
		} finally {
			System.clearProperty(key);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — multi-path array: one literal + one SVL element
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B_MultiPath {
		@RestGet(path={"/a", "/${a03.alt:b}"})
		public String hello() { return "b-multi-hit"; }
	}

	@Test void b01_multiPathArray_literalAndSvl_bothMount() throws Exception {
		var c = MockRestClient.buildLax(B_MultiPath.class);

		c.get("/a")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("b-multi-hit");

		// Second array element — SVL with default falls through to "/b"
		c.get("/b")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("b-multi-hit");

		// Anything else 404s
		c.get("/c")
			.run()
			.assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — verb-on-value form: @RestOp(value="GET /${a04.path:legacy}")
	//      Note the space between the method token and the path token — that is what triggers the
	//      "[METHOD] [path]" parse rule on @RestOp.value(). SVL is then applied to the trailing path
	//      token (after the space) only, never to the method token.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C_VerbOnValue {
		@RestOp(value="GET /${a04.path:legacy}")
		public String hello() { return "c-verb-on-value-hit"; }
	}

	@Test void c01_verbOnValue_svlResolves() throws Exception {
		var c = MockRestClient.buildLax(C_VerbOnValue.class);

		c.get("/legacy")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("c-verb-on-value-hit");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d — unresolved variable with no default: PropertyVar returns null → DefaultingVar returns null
	//      → resolveInto rewrites null replacement to "". The path "/${a05.missing}/foo" therefore
	//      compiles to "//foo" (or similar) at build time. The resource still builds; runtime
	//      requests miss. We assert "no startup crash + 404 on a sensible request".
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D_UnresolvedNoDefault {
		@RestGet(path="/${a05.missing}/foo")
		public String hello() { return "d-unresolved-hit"; }
	}

	@Test void d01_unresolvedVarNoDefault_doesNotCrashAtBuild() throws Exception {
		// The resource MUST build cleanly even when an SVL var has no value and no default; the
		// framework intentionally substitutes empty for unresolved vars rather than throwing during
		// construction (matches class-level path SVL handling — see RestContext#applySvl).
		var c = MockRestClient.buildLax(D_UnresolvedNoDefault.class);

		// Exact mount path is implementation-defined when SVL collapses to an empty segment; the
		// contract we assert is "the system stays functional and an obviously-wrong request is
		// rejected predictably (4xx)". 5xx would also be acceptable per the task spec.
		c.get("/some-other-path")
			.run()
			.assertStatus(404);
	}
}
