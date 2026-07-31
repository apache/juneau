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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * End-to-end coverage that {@code RestContext.*} settings resolve through {@link RestContextProperties} in the real
 * {@link RestContext} wiring &mdash; via system-property / relaxed-key overrides and via a resource's
 * {@code @Rest(config=...)} file.
 *
 * <p>
 * Two distinct resolution layers are exercised:
 * <ul>
 * 	<li><b>The bound-bean seam.</b> Every {@code RestContext.*} key resolves into the {@link RestContextProperties}
 * 		instance (retrievable via {@link RestContext#getRestContextProperties()}), independent of any downstream
 * 		merge &mdash; including keys supplied by a per-resource {@code @Rest(config=...)} file through the
 * 		caller-scoped {@code PropertySource} path.
 * 	<li><b>The resolved getter.</b> {@code RestContextProperties} supplies the <i>env-level default</i> that seeds
 * 		{@code RestContext}'s public getter, ranked below the {@code @Rest} annotation chain (which includes the
 * 		framework {@code DefaultConfig} fallback). For settings whose {@code DefaultConfig} default is blank or
 * 		absent (e.g. {@code problemDetails}, {@code allowedMethodHeaders}, {@code uriRelativity}) the env default
 * 		reaches the getter; for settings {@code DefaultConfig} fixes to a non-blank value (e.g.
 * 		{@code clientVersionHeader}) that annotation value wins in the getter.
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S5976" // Each d0x/e0x/i0x test pins a distinct named scenario (blank/invalid/mis-cased/whitespace/SVL) as its own discoverable, individually-runnable test; collapsing them into one @ParameterizedTest would trade per-scenario failure clarity for a marginal LOC reduction.
})
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class RestContext_ConfigPropertiesOverride_Test extends TestBase {

	// Intentionally NOT a ".cfg" extension: Config.getCandidateSystemDefaultConfigNames() treats any "*.cfg" file in
	// the current directory as a system-default candidate, which would let this fixture permanently poison the
	// process-lifetime Config.SYSTEM_DEFAULT memoizer for every no-config RestContext built later in the same fork.
	private static final String CFG = "rcpo-restcontext.ini";

	@BeforeAll
	static void writeFixtures() throws IOException {
		Files.writeString(Path.of(CFG),
			String.join(System.lineSeparator(),
				"RestContext.allowedHeaderParams = X-From-Config",
				"RestContext.allowedMethodHeaders = X-From-Config-Method",
				"RestContext.problemDetails = true") + System.lineSeparator(),
			StandardCharsets.UTF_8);
	}

	@AfterAll
	static void removeFixtures() throws IOException {
		Files.deleteIfExists(Path.of(CFG));
	}

	private static RestContext build(Class<?> resourceClass, Object resource) throws Exception {
		return new RestContext(new RestContext.Args(resourceClass, null, null, () -> resource, "", null, null, null, RestContext.ContextKind.ROOT))
			.postInit().postInitChildFirst();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Identity — getRestContextProperties() is the same instance auto-registered in the bean store.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A01 {}

	@Test void a01_identity_getterMatchesBeanStore() throws Exception {
		var ctx = build(A01.class, new A01());
		var p = ctx.getRestContextProperties();
		assertNotNull(p);
		assertSame(p, ctx.getBeanStore().getBean(RestContextProperties.class).orElse(null));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// The bound-bean seam: a RestContext.* system property binds into RestContextProperties even for settings whose
	// resolved getter is fixed by the DefaultConfig annotation (clientVersionHeader here).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B01 {}

	@Test void b01_sysPropertyBindsIntoBeanEvenWhenGetterIsAnnotationFixed() throws Exception {
		System.setProperty("RestContext.clientVersionHeader", "X-Client-Version");
		try {
			var ctx = build(B01.class, new B01());
			// Resolves into the bound bean...
			assertEquals("X-Client-Version", ctx.getRestContextProperties().getClientVersionHeader());
			// ...but DefaultConfig fixes clientVersionHeader to a non-blank value that wins in the resolved getter.
			assertEquals("Client-Version", ctx.getClientVersionHeader());
		} finally {
			System.clearProperty("RestContext.clientVersionHeader");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Getter flow-through: for settings DefaultConfig leaves blank/absent, the RestContext.* env default reaches the
	// resolved public getter (string, boolean, and enum shapes).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B02 {}

	@Test void b02_sysPropertyStringFlowsIntoGetter() throws Exception {
		System.setProperty("RestContext.allowedMethodHeaders", "X-Method-Hdr");
		try {
			var ctx = build(B02.class, new B02());
			assertEquals("X-Method-Hdr", ctx.getRestContextProperties().getAllowedMethodHeaders());
			assertTrue(ctx.getAllowedMethodHeaders().contains("X-Method-Hdr"));
		} finally {
			System.clearProperty("RestContext.allowedMethodHeaders");
		}
	}

	@Rest
	public static class B03 {}

	@Test void b03_sysPropertyBooleanFlowsIntoGetter() throws Exception {
		System.setProperty("RestContext.problemDetails", "true");
		try {
			var ctx = build(B03.class, new B03());
			assertEquals("true", ctx.getRestContextProperties().getProblemDetailsRaw());
			assertTrue(ctx.isProblemDetails());
		} finally {
			System.clearProperty("RestContext.problemDetails");
		}
	}

	@Rest
	public static class B04 {}

	@Test void b04_sysPropertyEnumFlowsIntoGetter() throws Exception {
		System.setProperty("RestContext.uriRelativity", "PATH_INFO");
		try {
			var ctx = build(B04.class, new B04());
			assertEquals("PATH_INFO", ctx.getRestContextProperties().getUriRelativityRaw());
			assertEquals(UriRelativity.PATH_INFO, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Relaxed SCREAMING_SNAKE_CASE spelling of a RestContext.* key resolves the same as the dotted form.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B05 {}

	@Test void b05_relaxedEnvStyleKeyFlowsIntoGetter() throws Exception {
		// Relaxed spelling of "RestContext.problemDetails".
		System.setProperty("REST_CONTEXT_PROBLEM_DETAILS", "true");
		try {
			var ctx = build(B05.class, new B05());
			assertEquals("true", ctx.getRestContextProperties().getProblemDetailsRaw());
			assertTrue(ctx.isProblemDetails());
		} finally {
			System.clearProperty("REST_CONTEXT_PROBLEM_DETAILS");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// A resource's @Rest(config=...) file can set RestContext.* keys and have them resolve through the per-resource
	// PropertySource into RestContextProperties, and — for settings not fixed by DefaultConfig — flow through into the
	// resolved public getters.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG)
	public static class C01 {}

	@Test void c01_restConfigFileLandsInBeanAndFlowsThrough() throws Exception {
		var ctx = build(C01.class, new C01());
		// Scoped-source path: every config-file RestContext.* key resolves into the bound bean...
		assertEquals("X-From-Config", ctx.getRestContextProperties().getAllowedHeaderParams());
		assertEquals("X-From-Config-Method", ctx.getRestContextProperties().getAllowedMethodHeaders());
		assertEquals("true", ctx.getRestContextProperties().getProblemDetailsRaw());
		// ...and, for settings DefaultConfig leaves blank/absent, flows into the resolved public getters.
		assertTrue(ctx.getAllowedMethodHeaders().contains("X-From-Config-Method"));
		assertTrue(ctx.isProblemDetails());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Lenient enum resolution for uriRelativity / uriResolution: blank, invalid, mis-cased, whitespace-padded, and
	// SVL ($S{...}) values all fall back to the hard default rather than throwing, and a valid value (with SVL and
	// surrounding whitespace tolerated) resolves to its enum constant. The RestContext.* env value is applied through
	// the resource's var resolver in RestContext#getUriRelativity() / #getUriResolution().
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D01 {}

	@Test void d01_uriRelativity_validValue() throws Exception {
		System.setProperty("RestContext.uriRelativity", "PATH_INFO");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.PATH_INFO, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	@Test void d02_uriRelativity_blankFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriRelativity", "");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.RESOURCE, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	@Test void d03_uriRelativity_invalidFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriRelativity", "BOGUS_VALUE");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.RESOURCE, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	@Test void d04_uriRelativity_misCasedFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriRelativity", "path_info");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.RESOURCE, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	@Test void d05_uriRelativity_whitespacePaddedResolves() throws Exception {
		System.setProperty("RestContext.uriRelativity", " PATH_INFO ");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.PATH_INFO, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
		}
	}

	@Test void d06_uriRelativity_svlValueResolves() throws Exception {
		System.setProperty("rcpo.uriRelativity", "PATH_INFO");
		System.setProperty("RestContext.uriRelativity", "$S{rcpo.uriRelativity}");
		try {
			var ctx = build(D01.class, new D01());
			assertEquals(UriRelativity.PATH_INFO, ctx.getUriRelativity());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
			System.clearProperty("rcpo.uriRelativity");
		}
	}

	@Rest
	public static class E01 {}

	@Test void e01_uriResolution_validValue() throws Exception {
		System.setProperty("RestContext.uriResolution", "ABSOLUTE");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ABSOLUTE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
		}
	}

	@Test void e02_uriResolution_blankFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriResolution", "");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ROOT_RELATIVE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
		}
	}

	@Test void e03_uriResolution_invalidFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriResolution", "BOGUS_VALUE");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ROOT_RELATIVE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
		}
	}

	@Test void e04_uriResolution_misCasedFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.uriResolution", "absolute");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ROOT_RELATIVE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
		}
	}

	@Test void e05_uriResolution_whitespacePaddedResolves() throws Exception {
		System.setProperty("RestContext.uriResolution", " ABSOLUTE ");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ABSOLUTE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
		}
	}

	@Test void e06_uriResolution_svlValueResolves() throws Exception {
		System.setProperty("rcpo.uriResolution", "ABSOLUTE");
		System.setProperty("RestContext.uriResolution", "$S{rcpo.uriResolution}");
		try {
			var ctx = build(E01.class, new E01());
			assertEquals(UriResolution.ABSOLUTE, ctx.getUriResolution());
		} finally {
			System.clearProperty("RestContext.uriResolution");
			System.clearProperty("rcpo.uriResolution");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// The bound bean carries the RAW (unresolved) enum string verbatim (no trim, no SVL) — the resource var resolver
	// and lenient enum typing live in RestContext#getUriRelativity() / #getUriResolution(), exercised above.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F01 {}

	@Test void f01_beanCarriesRawStringVerbatim() throws Exception {
		System.setProperty("RestContext.uriRelativity", " path_info ");
		System.setProperty("RestContext.uriResolution", "$S{rcpo.absent}");
		try {
			var ctx = build(F01.class, new F01());
			var p = ctx.getRestContextProperties();
			// Raw strings are stored verbatim (no trim, no SVL, no enum conversion).
			assertEquals(" path_info ", p.getUriRelativityRaw());
			assertEquals("$S{rcpo.absent}", p.getUriResolutionRaw());
		} finally {
			System.clearProperty("RestContext.uriRelativity");
			System.clearProperty("RestContext.uriResolution");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Single materialization + identity: getRestContextProperties() is stable across calls and identical to the
	// bean-store entry, even under concurrent access (the memoizer supplier's store-registration side effect must not
	// produce two competing instances).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class G01 {}

	@Test void g01_identity_stableAndMatchesBeanStore() throws Exception {
		var ctx = build(G01.class, new G01());
		var p = ctx.getRestContextProperties();
		assertNotNull(p);
		assertSame(p, ctx.getRestContextProperties());
		assertSame(p, ctx.getBeanStore().getBean(RestContextProperties.class).orElse(null));
	}

	@Test void g02_identity_concurrentAccess() throws Exception {
		// The constructor materializes the bean exactly once, so by the time any caller sees the context a single
		// instance exists and it is the bean-store entry — concurrent readers all observe that same instance.
		var ctx = build(G01.class, new G01());
		var expected = ctx.getBeanStore().getBean(RestContextProperties.class).orElse(null);
		assertNotNull(expected);
		var pool = Executors.newFixedThreadPool(8);
		try {
			var tasks = new ArrayList<Callable<RestContextProperties>>();
			for (var i = 0; i < 32; i++)
				tasks.add(ctx::getRestContextProperties);
			for (var f : pool.invokeAll(tasks))
				assertSame(expected, f.get());
		} finally {
			pool.shutdownNow();
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Mixin sub-context isolation: a mixin's bean store resolves ITS OWN RestContextProperties (== its
	// getRestContextProperties()), distinct from the host's, rather than walking up to the parent-linked host's.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H01Mixin {
		@RestGet(path="/mx") public String mx() { return "mx"; }
	}

	@Rest(mixins={H01Mixin.class})
	public static class H01Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void h01_mixinResolvesOwnRestContextProperties() {
		MockRestClient.buildLax(H01Host.class);
		var hostCtx = RestContext.getGlobalRegistry().get(H01Host.class);
		var mixinCtx = hostCtx.getMixinContexts().get(H01Mixin.class);
		assertNotNull(mixinCtx);

		var hostProps = hostCtx.getRestContextProperties();
		var mixinProps = mixinCtx.getRestContextProperties();
		assertNotNull(hostProps);
		assertNotNull(mixinProps);
		// The mixin's own bean store resolves the mixin's instance, NOT the host's (parent-linked) instance.
		assertSame(mixinProps, mixinCtx.getBeanStore().getBean(RestContextProperties.class).orElse(null));
		assertSame(hostProps, hostCtx.getBeanStore().getBean(RestContextProperties.class).orElse(null));
		assertNotSame(hostProps, mixinProps);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Boolean settings resolve SVL ($S{...}) in the RestContext.* env default the same as the enum settings do: the
	// raw seed is resolved recursively through the resource var resolver before being parsed to boolean. Covers both
	// a merge-path boolean (virtualThreads / eagerInit) and a direct-read boolean (responseTraceparent), with literal,
	// blank, and absent inputs asserted alongside to pin the full behavior surface. These settings are intentionally
	// not among the keys written by this class's @Rest(config=...) fixture, so they read cleanly.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I01 {}

	@Test void i01_virtualThreads_literalTrue() throws Exception {
		System.setProperty("RestContext.virtualThreads", "true");
		try {
			assertTrue(build(I01.class, new I01()).isVirtualThreadsEnabled());
		} finally {
			System.clearProperty("RestContext.virtualThreads");
		}
	}

	@Test void i02_virtualThreads_literalFalse() throws Exception {
		System.setProperty("RestContext.virtualThreads", "false");
		try {
			assertFalse(build(I01.class, new I01()).isVirtualThreadsEnabled());
		} finally {
			System.clearProperty("RestContext.virtualThreads");
		}
	}

	@Test void i03_virtualThreads_svlValueResolves() throws Exception {
		System.setProperty("rcpo.virtualThreads", "true");
		System.setProperty("RestContext.virtualThreads", "$S{rcpo.virtualThreads}");
		try {
			assertTrue(build(I01.class, new I01()).isVirtualThreadsEnabled());
		} finally {
			System.clearProperty("RestContext.virtualThreads");
			System.clearProperty("rcpo.virtualThreads");
		}
	}

	@Test void i04_virtualThreads_blankFallsBackToDefault() throws Exception {
		System.setProperty("RestContext.virtualThreads", "");
		try {
			assertFalse(build(I01.class, new I01()).isVirtualThreadsEnabled());
		} finally {
			System.clearProperty("RestContext.virtualThreads");
		}
	}

	@Test void i05_virtualThreads_absentUsesDefault() throws Exception {
		assertFalse(build(I01.class, new I01()).isVirtualThreadsEnabled());
	}

	@Test void i06_eagerInit_svlValueResolves() throws Exception {
		System.setProperty("rcpo.eagerInit", "true");
		System.setProperty("RestContext.eagerInit", "$S{rcpo.eagerInit}");
		try {
			assertTrue(build(I01.class, new I01()).isEagerInit());
		} finally {
			System.clearProperty("RestContext.eagerInit");
			System.clearProperty("rcpo.eagerInit");
		}
	}

	@Test void i07_responseTraceparent_svlValueResolves() throws Exception {
		// Default is true; assert an SVL value resolves recursively (would parse false under a literal-only bug).
		System.setProperty("rcpo.responseTraceparent", "true");
		System.setProperty("RestContext.responseTraceparent", "$S{rcpo.responseTraceparent}");
		try {
			assertTrue(build(I01.class, new I01()).isResponseTraceparent());
		} finally {
			System.clearProperty("RestContext.responseTraceparent");
			System.clearProperty("rcpo.responseTraceparent");
		}
	}

	@Test void i08_responseTraceparent_absentDefaultsTrue() throws Exception {
		assertTrue(build(I01.class, new I01()).isResponseTraceparent());
	}

	@Test void i09_responseTraceparent_literalFalse() throws Exception {
		System.setProperty("RestContext.responseTraceparent", "false");
		try {
			assertFalse(build(I01.class, new I01()).isResponseTraceparent());
		} finally {
			System.clearProperty("RestContext.responseTraceparent");
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Child-of-mixin isolation: a child mounted on a mixin sub-context resolves ITS OWN RestContextProperties through
	// its own bean store, even though its parent chain reaches the host's full store (which carries the host's
	// instance at a lower tier).  Without the local pin, the parent walk would return the host's bean while
	// getRestContextProperties() returns the child's — a store/getter mismatch.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J01Child {
		@RestGet(path="/g") public String g() { return "g"; }
	}

	@Rest(children={J01Child.class})
	public static class J01Mixin {
		@RestGet(path="/mx") public String mx() { return "mx"; }
	}

	@Rest(mixins={J01Mixin.class})
	public static class J01Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/h") public String h() { return "h"; }
	}

	@Test void j01_childOfMixinResolvesOwnRestContextProperties() {
		MockRestClient.buildLax(J01Host.class);
		var hostCtx = RestContext.getGlobalRegistry().get(J01Host.class);
		var mixinCtx = hostCtx.getMixinContexts().get(J01Mixin.class);
		assertNotNull(mixinCtx);
		var children = mixinCtx.getRestChildren().asMap().values();
		assertFalse(children.isEmpty());
		var childCtx = children.iterator().next();

		var hostProps = hostCtx.getRestContextProperties();
		var childProps = childCtx.getRestContextProperties();
		assertNotNull(hostProps);
		assertNotNull(childProps);
		// The child's own bean store resolves the child's instance, NOT the host's (reached via the parent walk).
		assertSame(childProps, childCtx.getBeanStore().getBean(RestContextProperties.class).orElse(null));
		assertNotSame(hostProps, childProps);
	}
}
