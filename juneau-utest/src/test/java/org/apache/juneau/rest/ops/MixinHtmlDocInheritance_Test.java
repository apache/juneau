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
package org.apache.juneau.rest.ops;

import org.apache.juneau.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates that a mixin op inherits the host resource's class-level {@link HtmlDocConfig @HtmlDocConfig}
 * page decoration (TODO-148).
 *
 * <p>
 * A mixin's {@code @RestOp} methods bind to a per-mixin {@link RestContext} sub-context whose
 * {@code getResourceClass()} is the mixin class, not the host. Without inheritance the mixin's HTML page
 * renders with default (empty) decoration even when the host declares class-level {@code @HtmlDocConfig}.
 * These tests cover host-only config, mixin-only config (still wins), and host+mixin-both precedence /
 * list-merge semantics, plus the {@link StatsMixin#getStats} per-method regression guard.
 *
 * @since 9.5.0
 */
@SuppressWarnings({"serial"})
class MixinHtmlDocInheritance_Test extends TestBase {

	// A page-less mixin op: relies entirely on the host's class-level @HtmlDocConfig.
	@Rest
	public static class A01_HostOnlyMixin extends RestMixin {
		@RestGet(path="/m")
		public Object m() { return "OK"; }
	}

	// Host declares class-level decoration; mixin op declares none.
	@Rest(mixins=A01_HostOnlyMixin.class)
	@HtmlDocConfig(navlinks={"home: servlet:/"}, aside={"hostAside"})
	public static class A01_HostOnly extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a01_hostOnlyConfigInheritedByMixinOp() throws Exception {
		var c = MockRestClient.build(A01_HostOnly.class);
		var r = c.get("/m").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(r, "<aside>hostAside</aside>");
		assertContains(r, "home");
	}

	// Mixin op carries its own per-method config; host declares none.
	@Rest
	public static class A02_MixinOnlyMixin extends RestMixin {
		@RestGet(path="/m")
		@HtmlDocConfig(aside={"mixinAside"})
		public Object m() { return "OK"; }
	}

	@Rest(mixins=A02_MixinOnlyMixin.class)
	public static class A02_MixinOnly extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a02_mixinOnlyConfigStillWins() throws Exception {
		var c = MockRestClient.build(A02_MixinOnly.class);
		var r = c.get("/m").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(r, "<aside>mixinAside</aside>");
	}

	// Both host and mixin-op declare config: covers method-level INHERIT merge and method-level override.
	@Rest
	public static class A03_BothMixin extends RestMixin {
		// Method INHERIT pulls in the host's class-level aside, then appends the mixin's own value.
		@RestGet(path="/merge")
		@HtmlDocConfig(aside={"INHERIT","mixinAside"})
		public Object merge() { return "OK"; }

		// Method declares aside without INHERIT: overrides the host's class-level aside entirely.
		@RestGet(path="/override")
		@HtmlDocConfig(aside={"overrideAside"})
		public Object override() { return "OK"; }
	}

	@Rest(mixins=A03_BothMixin.class)
	@HtmlDocConfig(aside={"hostAside"})
	public static class A03_Both extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a03_hostAndMixinMergeSemantics() throws Exception {
		var c = MockRestClient.build(A03_Both.class);
		var merge = c.get("/merge").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(merge, "<aside>hostAside mixinAside</aside>");
		var override = c.get("/override").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(override, "<aside>overrideAside</aside>");
	}

	// Mixin-class-level config overrides host-class config (precedence: mixin-class > host-class).
	@Rest
	@HtmlDocConfig(aside={"mixinClassAside"})
	public static class A04_MixinClassMixin extends RestMixin {
		@RestGet(path="/m")
		public Object m() { return "OK"; }
	}

	@Rest(mixins=A04_MixinClassMixin.class)
	@HtmlDocConfig(aside={"hostAside"})
	public static class A04_MixinClassWins extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a04_mixinClassOverridesHostClass() throws Exception {
		var c = MockRestClient.build(A04_MixinClassWins.class);
		var r = c.get("/m").accept("text/html").run().assertStatus(200).getContent().asString();
		assertContains(r, "<aside>mixinClassAside</aside>");
	}

	// Opt-out: @Rest(noInherit) naming the host config annotation type suppresses inheritance.
	@Rest(noInherit={"HtmlDocConfig"})
	public static class A05_NoInheritMixin extends RestMixin {
		@RestGet(path="/m")
		public Object m() { return "OK"; }
	}

	@Rest(mixins=A05_NoInheritMixin.class)
	@HtmlDocConfig(aside={"hostAside"})
	public static class A05_NoInherit extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a05_noInheritSuppressesHostConfig() throws Exception {
		var c = MockRestClient.build(A05_NoInherit.class);
		var r = c.get("/m").accept("text/html").run().assertStatus(200).getContent().asString();
		assertNotContains(r, "hostAside");
	}

	// Regression guard: StatsMixin's per-method @HtmlDocConfig (navlinks back/json, aside=NONE) still renders,
	// and is not clobbered by the host's class-level decoration.
	@Rest(mixins=StatsMixin.class)
	@HtmlDocConfig(navlinks={"home: servlet:/"}, aside={"hostAside"})
	public static class A06_StatsHost extends BasicRestServlet implements BasicJsonHtmlConfig {}

	@Test void a06_statsMixinPerMethodConfigStillRenders() throws Exception {
		var c = MockRestClient.build(A06_StatsHost.class);
		var r = c.get("/stats").accept("text/html").run().assertStatus(200).getContent().asString();
		// StatsMixin.getStats declares its own navlinks (back/json) at rank=10 and aside=NONE, so the host's
		// aside content must not appear on the /stats page.
		assertContains(r, "back");
		assertNotContains(r, "hostAside");
	}

	private static void assertContains(String s, String needle) {
		if (!s.contains(needle))
			throw new AssertionError("Expected to contain '" + needle + "' but did not. Body: " + s);
	}

	private static void assertNotContains(String s, String needle) {
		if (s.contains(needle))
			throw new AssertionError("Expected NOT to contain '" + needle + "' but did. Body excerpt: "
				+ s.substring(Math.max(0, s.indexOf(needle) - 50), Math.min(s.length(), s.indexOf(needle) + 100)));
	}
}
