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
package org.apache.juneau.rest.mixin;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.guard.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Feature tests for the {@link Rest#mixinDefs() @Rest(mixinDefs=@Mixin(...))} host-side override mechanism.
 *
 * <p>
 * Verifies the motivating capability &mdash; a host declaring {@code @Mixin(type=X, guards=AdminGuard)}
 * applies {@code AdminGuard} to {@code X}'s mixed-in endpoints without {@code X} declaring it &mdash; plus the
 * resolution rules: host-override + inherited host chain both apply (list-shaped append), the {@code @Mixin}'s
 * own {@code noInherit} cuts the inherited chain, replace-shaped slots win, host-chosen {@code path} re-mount,
 * transitive-mixin non-propagation, and bare-class &equiv; empty-override {@code @Mixin} equivalence.
 */
class MixinDefs_Overrides_Test extends TestBase {

	public static class AllowOnlyAdmin extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Admin").orElse(null));
		}
	}

	public static class AllowOnlyHost extends RestGuard {
		@Override public boolean isRequestAllowed(RestRequest req) {
			return "yes".equals(req.getHeaderParam("X-Host").orElse(null));
		}
	}

	// An unguarded, plain mixin that declares nothing special — overrides come from the host @Mixin.
	@Rest
	public static class PlainMixin {
		@RestGet(path="/plain") public String plain() { return "plain"; }
	}

	// =================================================================================
	// A. Host @Mixin(guards=...) applies a guard the mixin never declared.
	// =================================================================================

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, guards=AllowOnlyAdmin.class))
	public static class A extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void a01_hostMixinGuardAppliesToMixinEndpoint() throws Exception {
		var c = MockRestClient.buildLax(A.class);
		c.get("/plain").run().assertStatus(403);
		c.get("/plain").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	@Test void a02_hostEndpointUnaffectedByMixinOverride() throws Exception {
		// The override applies only to the mixin's endpoints, not the host's own.
		var c = MockRestClient.buildLax(A.class);
		c.get("/h").accept("text/plain").run().assertStatus(200).assertContent("h");
	}

	// =================================================================================
	// B. Override + inherited host guard chain BOTH apply (list-shaped append).
	// =================================================================================

	@Rest(guards=AllowOnlyHost.class, mixinDefs=@Mixin(type=PlainMixin.class, guards=AllowOnlyAdmin.class))
	public static class B extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void b01_bothHostChainAndOverrideApply() throws Exception {
		var c = MockRestClient.buildLax(B.class);
		c.get("/plain").run().assertStatus(403);
		c.get("/plain").header("X-Host", "yes").run().assertStatus(403);            // missing admin
		c.get("/plain").header("X-Admin", "yes").run().assertStatus(403);           // missing host
		c.get("/plain").header("X-Host", "yes").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// C. @Mixin(noInherit="guards") cuts the inherited host chain (override still applies).
	// =================================================================================

	@Rest(guards=AllowOnlyHost.class, mixinDefs=@Mixin(type=PlainMixin.class, guards=AllowOnlyAdmin.class, noInherit="guards"))
	public static class C extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void c01_noInheritCutsHostChainButKeepsOverride() throws Exception {
		var c = MockRestClient.buildLax(C.class);
		// Host guard cut -> only the @Mixin's own AllowOnlyAdmin applies.
		c.get("/plain").header("X-Host", "yes").run().assertStatus(403);            // host header no longer sufficient/required
		c.get("/plain").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// D. Replace-shaped slot — @Mixin(defaultRequestHeaders=...) seen by the mixin endpoint.
	// =================================================================================

	@Rest
	public static class HeaderEchoMixin {
		@RestGet(path="/echo") public String echo(RestRequest req) {
			return req.getHeaderParam("X-Default").orElse("<none>");
		}
	}

	@Rest(mixinDefs=@Mixin(type=HeaderEchoMixin.class, defaultRequestHeaders="X-Default: from-host-mixin"))
	public static class D extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void d01_defaultRequestHeaderOverrideAppliesToMixin() throws Exception {
		var c = MockRestClient.buildLax(D.class);
		c.get("/echo").accept("text/plain").run().assertStatus(200).assertContent("from-host-mixin");
	}

	// =================================================================================
	// E. Host-chosen path re-mount via @Mixin(path=...).
	// =================================================================================

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, path="/admin"))
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void e01_pathRemount() throws Exception {
		var c = MockRestClient.buildLax(E.class);
		c.get("/admin/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
		// Original (un-prefixed) path no longer mounted.
		c.get("/plain").run().assertStatus(404);
	}

	@Test void e02_pathRemountNormalizesInputForms() throws Exception {
		// Leading/trailing slashes + wildcard suffix all normalize to the same prefix.
		var c = MockRestClient.buildLax(E2.class);
		c.get("/mgmt/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, path="/mgmt/*"))
	public static class E2 extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	// =================================================================================
	// F. bare mixins=X  ===  mixinDefs=@Mixin(type=X) with no overrides.
	// =================================================================================

	@Rest(mixins=PlainMixin.class)
	public static class F_Bare extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class))
	public static class F_EmptyDef extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void f01_bareEqualsEmptyDef() throws Exception {
		var bare = MockRestClient.buildLax(F_Bare.class);
		var def = MockRestClient.buildLax(F_EmptyDef.class);
		bare.get("/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
		def.get("/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// G. Transitive non-propagation — override on the directly-named mixin does NOT reach a transitive mixin.
	// =================================================================================

	@Rest
	public static class InnerMixin {
		@RestGet(path="/inner") public String inner() { return "inner"; }
	}

	// OuterMixin pulls in InnerMixin transitively (flat-linked to the host).
	@Rest(mixins=InnerMixin.class)
	public static class OuterMixin {
		@RestGet(path="/outer") public String outer() { return "outer"; }
	}

	// Host overrides guards for OuterMixin only; InnerMixin must remain unguarded.
	@Rest(mixinDefs=@Mixin(type=OuterMixin.class, guards=AllowOnlyAdmin.class))
	public static class G extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void g01_overrideAppliesToNamedMixin() throws Exception {
		var c = MockRestClient.buildLax(G.class);
		c.get("/outer").run().assertStatus(403);
		c.get("/outer").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("outer");
	}

	@Test void g02_overrideDoesNotPropagateToTransitiveMixin() throws Exception {
		var c = MockRestClient.buildLax(G.class);
		// InnerMixin was not directly named in a @Mixin override, so it stays unguarded.
		c.get("/inner").accept("text/plain").run().assertStatus(200).assertContent("inner");
	}

	// =================================================================================
	// H. Multi-path re-mount — @Mixin(paths={...}) mounts the mixin under several prefixes.
	// =================================================================================

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, paths={"/p1", "/p2/"}))
	public static class H extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void h01_multiPathRemount() throws Exception {
		var c = MockRestClient.buildLax(H.class);
		c.get("/p1/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
		// Trailing-slash input form normalizes to the same clean prefix.
		c.get("/p2/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// I. bare + rich for the SAME class — the rich @Mixin upgrades the bare entry.
	// =================================================================================

	@Rest(mixins=PlainMixin.class, mixinDefs=@Mixin(type=PlainMixin.class, guards=AllowOnlyAdmin.class))
	public static class I extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void i01_richUpgradesBareEntry() throws Exception {
		var c = MockRestClient.buildLax(I.class);
		// The bare mixins=PlainMixin entry is upgraded by the rich @Mixin(guards=...) for the same class,
		// so the guard applies (and the mixin is mounted exactly once).
		c.get("/plain").run().assertStatus(403);
		c.get("/plain").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// J. Nested mixinDefs — a mixin class declares its own @Rest(mixinDefs=...) transitively.
	// =================================================================================

	@Rest(mixinDefs=@Mixin(type=InnerMixin.class))
	public static class OuterWithNestedDef {
		@RestGet(path="/outer2") public String outer2() { return "outer2"; }
	}

	@Rest(mixinDefs=@Mixin(type=OuterWithNestedDef.class))
	public static class J extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void j01_nestedMixinDefsDiscovered() throws Exception {
		var c = MockRestClient.buildLax(J.class);
		c.get("/outer2").accept("text/plain").run().assertStatus(200).assertContent("outer2");
		// InnerMixin pulled in transitively via the nested mixinDefs.
		c.get("/inner").accept("text/plain").run().assertStatus(200).assertContent("inner");
	}

	// =================================================================================
	// K. Path-normalization edge cases — blank paths[] entry skipped; slash-only path collapses to "no re-mount".
	// =================================================================================

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, paths={"/k1", ""}))
	public static class K extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void k01_blankPathsEntrySkipped() throws Exception {
		var c = MockRestClient.buildLax(K.class);
		// The non-blank "/k1" mounts; the blank entry is skipped (no spurious root mount).
		c.get("/k1/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, path="/"))
	public static class L extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void l01_slashOnlyPathIsNoRemount() throws Exception {
		var c = MockRestClient.buildLax(L.class);
		// path="/" normalizes to an empty token -> no re-mount; the mixin keeps its own absolute op path.
		c.get("/plain").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// =================================================================================
	// M. Discovery edge cases — rich-then-bare ordering (no downgrade) and a non-@Rest mixin class.
	// =================================================================================

	// mixinDefs (rich) listed BEFORE the bare mixins= entry for the same class: the rich entry must win
	// (a later bare occurrence must not downgrade it).  Exercises the "already discovered, rm is bare" path.
	@Rest(mixinDefs=@Mixin(type=PlainMixin.class, guards=AllowOnlyAdmin.class), mixins=PlainMixin.class)
	public static class M extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void m01_richBeforeBareNotDowngraded() throws Exception {
		var c = MockRestClient.buildLax(M.class);
		// Rich @Mixin(guards=...) declared first; the later bare entry for the same class does not strip the guard.
		c.get("/plain").run().assertStatus(403);
		c.get("/plain").header("X-Admin", "yes").accept("text/plain").run().assertStatus(200).assertContent("plain");
	}

	// A mixin class with NO class-level @Rest annotation (just @RestOp methods) — exercises the r==null arm of
	// the nested-discovery recursion.
	public static class NoRestMixin {
		@RestGet(path="/norest") public String norest() { return "norest"; }
	}

	@Rest(mixinDefs=@Mixin(type=NoRestMixin.class))
	public static class N extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@Test void n01_nonRestAnnotatedMixinClass() throws Exception {
		var c = MockRestClient.buildLax(N.class);
		c.get("/norest").accept("text/plain").run().assertStatus(200).assertContent("norest");
	}
}
