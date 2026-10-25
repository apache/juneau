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
package org.apache.juneau.commons.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Pins that the lazy config source is never cached, that a miss inside a static initializer is not frozen, and that
 * live {@link Config} changes reach the builders.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")  // addSource on the singleton, Config.setSystemDefault
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class Settings_LazyConfigShape_Test extends TestBase {

	private static final String LAZY = "org.apache.juneau.config.ConfigPropertySourceProvider$LazyConfigPropertySource";

	/** Set by f01 before it runs its probe. Public, so a probe defined by a throwaway class loader can read it. */
	public static final class Target {
		public static volatile Settings value;
	}

	/** f01's probe: reads {@code k} from {@link Target} inside its {@code <clinit>}. */
	public static final class Probe1 {
		public static final String SEEN = Target.value.get("k").orElse(null);
	}

	/** f02's probe: reads a serializer setting from the singleton inside its {@code <clinit>}. */
	public static final class Probe2 {
		public static final String SEEN = Settings.get().get("JsonSerializer.escapeSolidus").orElse(null);
	}

	/**
	 * Defines a fresh copy of {@code probe} in a throwaway class loader and runs its {@code <clinit>}, so the probe
	 * really runs again when a test is re-run in the same JVM. Everything else the probe touches comes from the parent.
	 */
	private static String runProbe(Class<?> probe) throws Exception {
		var name = probe.getName();
		byte[] bytes;
		try (var in = probe.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
			bytes = in.readAllBytes();
		}
		var loader = new ClassLoader(probe.getClassLoader()) {
			@Override
			protected Class<?> loadClass(String n, boolean resolve) throws ClassNotFoundException {
				if (! n.equals(name))
					return super.loadClass(n, resolve);
				synchronized (getClassLoadingLock(n)) {
					var c = findLoadedClass(n);
					return c != null ? c : defineClass(n, bytes, 0, bytes.length);
				}
			}
		};
		return (String)Class.forName(name, true, loader).getField("SEEN").get(null);
	}

	/**
	 * Resolves the singleton's own lazy config source against the real system default before any test swaps it.
	 * That source binds to whichever {@code Config} is the system default at its first lookup outside a static
	 * initializer, so without this a test's temporary {@code Config} would stay bound for the rest of the JVM.
	 */
	@BeforeAll static void resolveSingletonLazySource() {
		Settings.get().get("Settings_LazyConfigShape_Test.warm").orElse(null);
	}

	// Each test builds its Config on a private MemoryStore: memStore() shares MemoryStore.DEFAULT, so one test's keys leak into the next.
	private Config previous;

	@BeforeEach void saveDefault() {
		previous = Config.getSystemDefault();
	}

	@AfterEach void restoreDefault() {
		Config.setSystemDefault(previous);
	}

	@Test void e03_singleton_hasOneLazyConfigSource_andItIsNotWrapped() {
		var lazy = Settings.get().sources().stream().filter(s -> s.getClass().getName().equals(LAZY)).toList();
		assertEquals(1, lazy.size());
		for (var s : Settings.get().sources())
			if (s instanceof CachingPropertySource c)
				assertNotEquals(LAZY, c.delegate().getClass().getName(), "LazyConfigPropertySource must never be wrapped");
	}

	@Test void f01_missInsideClinit_isNotFrozen() throws Exception {
		var cfg = Config.create().store(MemoryStore.create().build()).build();
		cfg.set("k", "v");
		Config.setSystemDefault(cfg);
		Target.value = Settings.defaultBuilder(List.of(new ConfigPropertySourceProvider())).build();
		try {
			assertNull(runProbe(Probe1.class), "the <clinit> guard answers missing()");
			assertEquals("v", Target.value.get("k").orElse(null), "with a cache over the lazy source this second read would also be missing");
		} finally {
			Target.value = null;
		}
	}

	@Test void f02_missInsideClinit_doesNotPoisonBuilderDefaults() throws Exception {
		var unused = JsonSerializer.DEFAULT;   // make sure its <clinit> has already run
		// Config.setSystemDefault runs Settings lookups, so it goes before the lazy source is registered: a lazy source
		// that resolved before the swap would bind to the old Config.
		var cfg = Config.create().store(MemoryStore.create().build()).build();
		cfg.set("JsonSerializer.escapeSolidus", "true");
		Config.setSystemDefault(cfg);
		// Created directly: building a Settings runs lookups too, which would resolve the lazy source early.
		var lazy = new ConfigPropertySourceProvider().create();
		assertEquals(LAZY, lazy.getClass().getName());
		Settings.get().addSource(lazy);
		try {
			assertNull(runProbe(Probe2.class), "the <clinit> guard answers missing()");
			assertEquals("\"a\\/b\"", JsonSerializer.create().build().write("a/b"), "the miss inside the <clinit> was not frozen");
		} finally {
			Settings.get().removeSource(lazy);
		}
	}

	@Test void s5_configChange_isSeenByTheNextBuilder() throws Exception {
		var cfg = Config.create().store(MemoryStore.create().build()).build();
		var src = new ConfigPropertySource(cfg);
		Settings.get().addSource(src);
		try {
			// Warm every cache first.
			assertEquals("\"a/b\"", JsonSerializer.create().build().write("a/b"));
			assertEquals("\"a/b\"", JsonSerializer.create().build().write("a/b"));
			cfg.set("JsonSerializer.escapeSolidus", "true");
			assertEquals("\"a\\/b\"", JsonSerializer.create().build().write("a/b"));
			cfg.set("JsonSerializer.escapeSolidus", "false");
			assertEquals("\"a/b\"", JsonSerializer.create().build().write("a/b"));
		} finally {
			Settings.get().removeSource(src);
		}
	}
}
