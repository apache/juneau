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
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Acceptance tests for per-{@code RestContext} {@code @Value} resolution against
 * {@code @Rest(config=...)} {@link org.apache.juneau.marshall.config.Config}s.
 *
 * <p>
 * The test writes its config fixtures to the cwd in {@link BeforeAll @BeforeAll} (because the
 * default {@link org.apache.juneau.marshall.config.store.FileStore#DEFAULT FileStore.DEFAULT} resolves
 * names against the cwd; with Maven Surefire that is the module directory). The cwd-resident
 * files are removed in {@link AfterAll @AfterAll}. Names use a fixed {@code todo95-} prefix so
 * they are easy to spot and clean up by hand should a test crash partway through.
 *
 * <p>
 * Each resource class uses a unique name so {@code MockRestClient}'s static {@code RestContext}
 * cache does not return a stale {@code RestContext} from an unrelated test class.
 */
@SuppressWarnings({
	"serial", // Test resources extend BasicRestServlet which is serializable; not relevant.
	"resource" // MockRestClient instances are short-lived test fixtures; the mock framework manages lifecycle.
})
class RestContext_ValueAgainstConfig_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Fixture management — write cfg files to cwd before tests, delete them after.
	//------------------------------------------------------------------------------------------------------------------

	private static final String CFG_A = "todo95-rcvac-a.cfg";
	private static final String CFG_B = "todo95-rcvac-b.cfg";
	private static final String CFG_PARENT = "todo95-rcvac-parent.cfg";
	private static final String CFG_CHILD = "todo95-rcvac-child.cfg";
	private static final String CFG_OVERRIDE = "todo95-rcvac-override.cfg";
	private static final String CFG_ASYNC = "todo95-rcvac-async.cfg";

	@BeforeAll
	static void writeFixtures() throws IOException {
		write(CFG_A,
			"api.key = secret-A",
			"api.url = https://a.example.org/",
			"foo = A",
			"[section]",
			"nested = nested-A");
		write(CFG_B,
			"api.key = secret-B",
			"foo = B");
		write(CFG_PARENT,
			"parent.only = parent-value",
			"shared = parent-shared");
		write(CFG_CHILD,
			"child.only = child-value",
			"shared = child-shared");
		write(CFG_OVERRIDE,
			"override.key = from-config");
		write(CFG_ASYNC,
			"async.greeting = hello-from-config");
	}

	@AfterAll
	static void removeFixtures() throws IOException {
		for (var n : List.of(CFG_A, CFG_B, CFG_PARENT, CFG_CHILD, CFG_OVERRIDE, CFG_ASYNC))
			Files.deleteIfExists(Path.of(n));
	}

	private static void write(String name, String... lines) throws IOException {
		Files.writeString(Path.of(name), String.join(System.lineSeparator(), lines) + System.lineSeparator(),
			StandardCharsets.UTF_8);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #1 — Happy path. @Rest(config=...) + @Value("${cfg-key}") resolves to the Config value.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG_A)
	public static class HappyPathResource extends BasicRestServlet {
		@Value("${api.key}")
		String apiKey;

		@Value("${api.url}")
		String apiUrl;

		@Value("${section/nested}")
		String nested;
	}

	@Test void a01_happyPath_configKeyResolves() throws Exception {
		var rc = build(HappyPathResource.class);
		var bean = (HappyPathResource) rc.getResource();
		assertEquals("secret-A", bean.apiKey);
		assertEquals("https://a.example.org/", bean.apiUrl);
		assertEquals("nested-A", bean.nested);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #2 — Fall-through. Key absent from Config falls back to Settings/env.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG_A)
	public static class FallThroughResource extends BasicRestServlet {
		// "api.key" is in the Config; "todo95.absent.key" is not — should hit the default branch.
		@Value("${todo95.absent.key:default-fallback}")
		String absentDefault;

		// Use a Settings.setGlobal key during the test to prove Settings sources still resolve.
		@Value("${todo95.absent.from.settings}")
		String fromSettings;
	}

	@Test void a02_fallThrough_defaultBranch_andSettings() throws Exception {
		Settings.get().setGlobal("todo95.absent.from.settings", "from-settings");
		try {
			var rc = build(FallThroughResource.class);
			var bean = (FallThroughResource) rc.getResource();
			assertEquals("default-fallback", bean.absentDefault);
			assertEquals("from-settings", bean.fromSettings);
		} finally {
			Settings.get().unsetGlobal("todo95.absent.from.settings");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #3 — Resource isolation. Resource A and Resource B both have key "foo" but with
	// different values; each sees its own.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG_A)
	public static class IsolationResourceA extends BasicRestServlet {
		@Value("${foo}")
		String foo;
	}

	@Rest(config=CFG_B)
	public static class IsolationResourceB extends BasicRestServlet {
		@Value("${foo}")
		String foo;
	}

	@Test void a03_resourceIsolation_each_sees_own_config() throws Exception {
		var rcA = build(IsolationResourceA.class);
		var rcB = build(IsolationResourceB.class);
		assertEquals("A", ((IsolationResourceA) rcA.getResource()).foo);
		assertEquals("B", ((IsolationResourceB) rcB.getResource()).foo);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #4 — No-config resource. @Rest with no config attribute resolves via Settings only.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class NoConfigResource extends BasicRestServlet {
		@Value("${todo95.noconfig.key:noconfig-default}")
		String value;

		@Value("${todo95.noconfig.fromSettings}")
		String fromSettings;
	}

	@Test void a04_noConfig_falls_through_to_settings_only() throws Exception {
		Settings.get().setGlobal("todo95.noconfig.fromSettings", "settings-value");
		try {
			var rc = build(NoConfigResource.class);
			var bean = (NoConfigResource) rc.getResource();
			assertEquals("noconfig-default", bean.value);
			assertEquals("settings-value", bean.fromSettings);
		} finally {
			Settings.get().unsetGlobal("todo95.noconfig.fromSettings");
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #5 — Inheritance. Parent + child @Rest(config=...) annotations: child wins on
	// collision, parent fills the gaps.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG_PARENT)
	public static class InheritanceParent extends BasicRestServlet {
		@Value("${shared}")
		String shared;

		@Value("${parent.only}")
		String parentOnly;

		@Value("${child.only:absent-on-parent}")
		String childOnly;
	}

	@Rest(config=CFG_CHILD)
	public static class InheritanceChild extends InheritanceParent {}

	@Test void a05_inheritance_child_wins_parent_fills_gaps() throws Exception {
		var rc = build(InheritanceChild.class);
		var bean = (InheritanceChild) rc.getResource();
		// Child wins on collision: shared resolves to child's value.
		assertEquals("child-shared", bean.shared);
		// Parent fills the gap: parent.only is only in parent.cfg, child sees it.
		assertEquals("parent-value", bean.parentOnly);
		// Child has its own key:
		assertEquals("child-value", bean.childOnly);
	}

	@Test void a05b_parent_alone_resolves_parent_keys() throws Exception {
		var rc = build(InheritanceParent.class);
		var bean = (InheritanceParent) rc.getResource();
		assertEquals("parent-shared", bean.shared);
		assertEquals("parent-value", bean.parentOnly);
		// child.only is absent from parent.cfg → falls through to the default branch.
		assertEquals("absent-on-parent", bean.childOnly);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #6 — Settings.setLocal()/setGlobal() overrides still win over resource Config.
	//------------------------------------------------------------------------------------------------------------------

	// MockRestClient caches RestContext per class, so a @Value String field is captured ONCE at
	// resource-bean construction time. To exercise the "override wins later" path we use a
	// Supplier<String> @Value — it re-evaluates on each .get() call against a fresh
	// VarResolverSession that carries the resource's BeanStore as a session bean.
	@Rest(config=CFG_OVERRIDE)
	public static class OverrideResource extends BasicRestServlet {
		@Value("${override.key}")
		Supplier<String> value;
	}

	@Test void a06_setGlobal_overrides_resource_config() throws Exception {
		var rc = build(OverrideResource.class);
		var bean = (OverrideResource) rc.getResource();
		// No override active → resource Config value wins.
		assertEquals("from-config", bean.value.get());

		Settings.get().setGlobal("override.key", "from-settings-global");
		try {
			// Override active → setGlobal must beat resource Config (per OQA #3).
			assertEquals("from-settings-global", bean.value.get(),
				"Settings.setGlobal must win over resource @Rest(config) values.");
		} finally {
			Settings.get().unsetGlobal("override.key");
		}
		// Override gone → resource Config value visible again (Supplier re-evaluates).
		assertEquals("from-config", bean.value.get());
	}

	@Test void a06b_setLocal_overrides_resource_config() throws Exception {
		var rc = build(OverrideResource.class);
		var bean = (OverrideResource) rc.getResource();

		Settings.get().setLocal("override.key", "from-settings-local");
		try {
			assertEquals("from-settings-local", bean.value.get(),
				"Settings.setLocal must win over resource @Rest(config) values.");
		} finally {
			Settings.get().unsetLocal("override.key");
		}
		assertEquals("from-config", bean.value.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #7 — Async / virtual-thread composition. CompletableFuture<String> @RestOp method
	// body reads a @Value field; assertion holds whether the future resolves on the dispatch thread
	// or a worker thread.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(config=CFG_ASYNC)
	public static class AsyncResource extends BasicRestServlet {
		@Value("${async.greeting}")
		String greeting;

		@Value("${async.greeting}")
		Supplier<String> greetingSupplier;
	}

	@Test void a07_async_composition_value_visible() throws Exception {
		var rc = build(AsyncResource.class);
		var bean = (AsyncResource) rc.getResource();
		// The field value is captured at injection time — verify directly on the bean.
		assertEquals("hello-from-config", bean.greeting);
		// The Supplier<String> re-evaluates on every .get() call.
		assertEquals("hello-from-config", bean.greetingSupplier.get());

		// Re-evaluation from a worker thread (CompletableFuture) must see the same value because
		// the BeanStore reference captured by the Supplier participates regardless of which thread
		// calls .get() — i.e., the resolution is not pinned to the dispatch thread.
		var worker = CompletableFuture.supplyAsync(bean.greetingSupplier::get).get(5, TimeUnit.SECONDS);
		assertEquals("hello-from-config", worker);
	}

	@Rest(config=CFG_ASYNC, virtualThreads="true")
	public static class AsyncVirtualThreadsResource extends BasicRestServlet {
		@Value("${async.greeting}")
		String greeting;

		@Value("${async.greeting}")
		Supplier<String> greetingSupplier;
	}

	@Test void a07b_virtualThreads_value_visible() throws Exception {
		var rc = build(AsyncVirtualThreadsResource.class);
		var bean = (AsyncVirtualThreadsResource) rc.getResource();
		// Field injection captured at construction time (the BeanStore had the rest.config source
		// in place by then, so this verifies the construction-time path holds when the resource is
		// configured with virtualThreads="true").
		assertEquals("hello-from-config", bean.greeting);
		// Re-evaluation from a worker thread continues to see the value (BeanStore-by-reference
		// capture in ValueResolver's supplier path, not pinned to the dispatch thread).
		var worker = CompletableFuture.supplyAsync(bean.greetingSupplier::get).get(5, TimeUnit.SECONDS);
		assertEquals("hello-from-config", worker);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance #8 — Registration is under name "rest.config" in the resource BeanStore.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a08_propertySource_registered_under_rest_config_name() throws Exception {
		var rc = build(HappyPathResource.class);
		var src = rc.getBeanStore().getBean(PropertySource.class, "rest.config").orElse(null);
		assertNotNull(src, "Expected a PropertySource bean registered as \"rest.config\" in the resource BeanStore.");
		assertEquals("secret-A", src.get("api.key").value().orElse(null));
	}

	@Test void a08b_no_config_no_registration() throws Exception {
		// A @Rest resource with no config= attribute and no @Bean Config method should leave the
		// "rest.config" slot empty (rawConfig.get() returns an empty Config that the bridge skips).
		var rc = build(NoConfigResource.class);
		var src = rc.getBeanStore().getBean(PropertySource.class, "rest.config").orElse(null);
		// An empty Config still has the bridge registered (since rawConfig.get() is non-null), but
		// querying any unknown key yields missing. Either no bean registered OR a bean that returns
		// missing for an arbitrary key is acceptable; assert the latter.
		if (src != null)
			assertFalse(src.get("definitely-not-here").isPresent());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helper — build a RestContext and look it up in the global registry so the test can read
	// back the resource bean and the BeanStore directly.
	//------------------------------------------------------------------------------------------------------------------

	private static RestContext build(Class<?> resourceClass) throws Exception {
		// MockRestClient.build() instantiates a RestContext for the resource class (and caches it
		// statically); look the result up in the global registry to read the injected fields.
		MockRestClient.build(resourceClass);
		var rc = RestContext.getGlobalRegistry().get(resourceClass);
		assertNotNull(rc, "RestContext for " + resourceClass.getSimpleName() + " not in REGISTRY after build.");
		return rc;
	}
}
