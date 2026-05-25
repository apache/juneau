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
package org.apache.juneau.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ConfigPropertySourceProvider} — the auto-registered SPI bridge from {@link Config}
 * to {@link Settings} introduced in Phase 3 of TODO-79.
 *
 * <p>
 * Covers three layers:
 * <ol>
 * 	<li>SPI plumbing: the provider returns {@code null} silently when no classpath default exists.
 * 	<li>SPI plumbing: the provider returns a {@link ConfigPropertySource} when a system default is set.
 * 	<li>End-to-end: a {@code @Value("${...}")} bean resolves through {@link Settings#addSource(PropertySource)
 * 		Settings.get().addSource(...)} the same way the microservice / RestContext hooks register their
 * 		per-resource {@code Config}.
 * </ol>
 */
@SuppressWarnings({
	"java:S2094" // Test fixture / data class, no methods required.
})
class ConfigPropertySourceProvider_Test extends TestBase {

	public static class ValueBean {
		@Value("${ConfigPropertySourceProvider_Test/k}")
		String value;
	}

	public static class ValueBeanWithDefault {
		@Value("${ConfigPropertySourceProvider_Test/k:not-set}")
		String value;
	}

	private BasicBeanStore beanStore;

	@BeforeEach
	void setUp() {
		beanStore = new BasicBeanStore(null);
	}

	//====================================================================================================
	// Provider semantics — direct unit tests.
	//====================================================================================================

	@Test
	void a01_provider_silent_whenNoSystemDefault() {
		// Provider always returns a non-null lazy source — eager resolution would create a
		// Settings/Config static-initialization cycle (the SPI fires from Settings.<clinit>).
		// "Silent when no config" means: every lookup on the source returns missing(), with no
		// log noise and no exception.
		var previous = Config.getSystemDefault();
		Config.setSystemDefault(null);
		try {
			var provider = new ConfigPropertySourceProvider();
			var src = provider.create();
			assertNotNull(src, "Provider returns a lazy wrapper unconditionally");
			var result = src.get("ConfigPropertySourceProvider_Test/k");
			assertFalse(result.isPresent(),
				"Lazy wrapper must report missing() when no system-default Config is set");
		} finally {
			Config.setSystemDefault(previous);
		}
	}

	@Test
	void a02_provider_returnsSource_whenSystemDefaultPresent() {
		var previous = Config.getSystemDefault();
		var cfg = Config.create().memStore().build();
		cfg.set("ConfigPropertySourceProvider_Test/k", "from-system-default");
		Config.setSystemDefault(cfg);
		try {
			var provider = new ConfigPropertySourceProvider();
			var src = provider.create();
			assertNotNull(src, "Provider must return a source when a system default exists");
			var result = src.get("ConfigPropertySourceProvider_Test/k");
			assertTrue(result.isPresent(), "Source must report present for set key");
			assertEquals("from-system-default", result.value().orElse(null));
		} finally {
			Config.setSystemDefault(previous);
		}
	}

	@Test
	void a03_provider_order_lowSoUserAdditionsWin() {
		// The provider deliberately sits at order 100 so later programmatic addSource() calls
		// (microservice / RestContext) register AFTER it and therefore win at lookup time.
		assertEquals(100, new ConfigPropertySourceProvider().order());
	}

	//====================================================================================================
	// End-to-end: pushing a Config-backed source onto Settings and reading via @Value.
	//====================================================================================================

	@Test
	void b01_endToEnd_addSource_shadowsDefault() {
		var cfg = Config.create().memStore().build();
		cfg.set("ConfigPropertySourceProvider_Test/k", "from-explicit-source");
		var src = new ConfigPropertySource(cfg);
		Settings.get().addSource(src);
		try {
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("from-explicit-source", bean.value);
		} finally {
			assertTrue(Settings.get().removeSource(src),
				"removeSource must report success when the source is present");
		}
	}

	@Test
	void b02_endToEnd_missing_usesDefault() {
		// No source pushed — verify the @Value default path fires when nothing wins.
		var bean = BeanInstantiator.of(ValueBeanWithDefault.class, beanStore).run();
		assertEquals("not-set", bean.value);
	}

	@Test
	void b03_endToEnd_microserviceOverride_winsOverFirstAdd() {
		// Simulates the microservice path: a "base" source is registered first, then a "micro"
		// source is registered later. The later source wins because Settings walks in reverse.
		var base = Config.create().memStore().build();
		base.set("ConfigPropertySourceProvider_Test/k", "base-val");
		var micro = Config.create().memStore().build();
		micro.set("ConfigPropertySourceProvider_Test/k", "micro-val");
		var baseSrc = new ConfigPropertySource(base);
		var microSrc = new ConfigPropertySource(micro);
		Settings.get().addSource(baseSrc);
		Settings.get().addSource(microSrc);
		try {
			var bean = BeanInstantiator.of(ValueBean.class, beanStore).run();
			assertEquals("micro-val", bean.value, "Later addSource() must shadow earlier addSource()");
		} finally {
			Settings.get().removeSource(microSrc);
			Settings.get().removeSource(baseSrc);
		}
	}

	@Test
	void b04_removeSource_returnsFalse_whenSourceUnknown() {
		var src = new ConfigPropertySource(Config.create().memStore().build());
		assertFalse(Settings.get().removeSource(src),
			"removeSource must return false for a never-added source");
	}

	//====================================================================================================
	// SPI plumbing — verify the META-INF/services file is on the classpath.
	//====================================================================================================

	@Test
	void c01_spiServiceFile_isOnClasspath() {
		// The Phase 3 META-INF/services entry must ship with juneau-config so Settings.useServiceLoader()
		// picks up the provider at startup.  This guards against accidental deletion of the resource.
		var url = ConfigPropertySourceProvider.class.getClassLoader()
			.getResource("META-INF/services/org.apache.juneau.commons.settings.PropertySourceProvider");
		assertNotNull(url, "META-INF/services entry for PropertySourceProvider must be discoverable");
	}
}
