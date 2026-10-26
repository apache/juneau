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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Tests that the singleton's source chain drops the duplicate trailing {@code SYSTEM_PROPERTY_SOURCE} only when SPI
 * registered a system-property source, and that no lookup changes.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class Settings_DefaultChain_Test extends TestBase {

	// ------------------------------------------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------------------------------------------

	private static PropertySourceProvider provider(int order, PropertySource src) {
		return new PropertySourceProvider() {
			@Override public PropertySource create() { return src; }
			@Override public int order() { return order; }
		};
	}

	private static List<PropertySourceProvider> realProviders() {
		return List.of(
			new ManifestFilePropertySourceProvider(), new DotenvPropertySourceProvider(), new SystemEnvPropertySourceProvider(),
			new SystemPropertyPropertySourceProvider(), new ArgsPropertySourceProvider());
	}

	/** Today's composition, literally. */
	private static Settings legacy(Collection<PropertySourceProvider> providers) {
		return new Settings.Builder().setSources(Settings.RELAXED_SYSTEM_ENV_SOURCE, Settings.SYSTEM_PROPERTY_SOURCE).addProviders(providers).build();
	}

	private static Settings current(Collection<PropertySourceProvider> providers) {
		return Settings.defaultBuilder(providers).build();
	}

	private static PropertySource unwrap(PropertySource s) {
		return s instanceof CachingPropertySource c ? c.delegate() : s;
	}

	/** Position-wise shape: identity for the two constants, the unwrapped class for everything else. */
	private static List<Object> shape(Settings s) {
		return s.sources().stream()
			.map(x -> x == Settings.RELAXED_SYSTEM_ENV_SOURCE || x == Settings.SYSTEM_PROPERTY_SOURCE ? (Object)x : (Object)unwrap(x).getClass())
			.toList();
	}

	/** Walks the sources newest first exactly like Settings does, but keeps present-with-null distinct from missing. */
	private static String walk(Settings s, String key) {
		var src = s.sources();
		for (var i = src.size() - 1; i >= 0; i--) {
			var r = src.get(i).get(key);
			if (r.isPresent())
				return "P:" + r.value().orElse(null);
		}
		return "M";
	}

	private static PropertySource mapSource(Map<String,String> m) {
		return name -> m.containsKey(name) ? PropertyLookupResult.present(o(m.get(name))) : PropertyLookupResult.missing();
	}

	private static String camel(String envKey) {
		var parts = envKey.toLowerCase(Locale.ROOT).split("_", -1);
		var sb = new StringBuilder(parts[0]);
		for (var i = 1; i < parts.length; i++)
			if (! parts[i].isEmpty())
				sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
		return sb.toString();
	}

	// ------------------------------------------------------------------------------------------------------------
	// Shape (these detect the D5 change)
	// ------------------------------------------------------------------------------------------------------------

	@Test void d01_shape_case1_realProviders_dropsTheDuplicate() {
		var p = realProviders();
		var legacyShape = shape(legacy(p));
		var currentShape = shape(current(p));
		assertEquals(legacyShape.size() - 1, currentShape.size());
		assertEquals(legacyShape.stream().filter(x -> x != Settings.SYSTEM_PROPERTY_SOURCE).toList(), currentShape);
		assertEquals(1, current(p).sources().stream().filter(x -> unwrap(x) instanceof SystemPropertyPropertySource).count());
		assertTrue(current(p).sources().stream().noneMatch(x -> x == Settings.SYSTEM_PROPERTY_SOURCE));
	}

	@Test void d02_shape_case2_noProviders_keepsFallbackAtIndex1() {
		var c = current(List.of());
		assertEquals(List.of(Settings.RELAXED_SYSTEM_ENV_SOURCE, Settings.SYSTEM_PROPERTY_SOURCE), shape(c));
		assertEquals(shape(legacy(List.of())), shape(c));
	}

	@Test void d03_shape_case2_partial_onlyFakeOrder100() {
		PropertySource fake = name -> PropertyLookupResult.missing();
		var c = current(List.of(provider(100, fake)));
		var s = c.sources();
		assertEquals(3, s.size());
		assertSame(Settings.RELAXED_SYSTEM_ENV_SOURCE, s.get(0));
		assertSame(Settings.SYSTEM_PROPERTY_SOURCE, s.get(1));
		assertSame(fake, s.get(2));
	}

	@ResourceLock(value = "org.apache.juneau.commons.settings.Settings.singleton", mode = ResourceAccessMode.READ)
	@Test void d07_singleton_hasExactlyOneSystemPropertySource_andItIsNotTheConstant() {
		var matches = Settings.get().sources().stream()
			.filter(x -> x == Settings.SYSTEM_PROPERTY_SOURCE || unwrap(x).getClass().getName().equals(SystemPropertyPropertySource.class.getName()))
			.toList();
		assertEquals(1, matches.size(), "today's chain has 2; D5 leaves 1");
		assertNotSame(Settings.SYSTEM_PROPERTY_SOURCE, matches.get(0));
	}

	@Test void d08_detectionFailsSafe_subclassAndDecorator_keepFallback() {
		class SubSysProp extends SystemPropertyPropertySource {}
		var sub = current(List.of(provider(40, new SubSysProp())));
		assertSame(Settings.SYSTEM_PROPERTY_SOURCE, sub.sources().get(1), "a subclass is not detected, so the fallback is kept");

		var inner = new SystemPropertyPropertySource();
		PropertySource decorator = new PropertySource() {
			@Override public PropertyLookupResult get(String name) { return inner.get(name); }
		};
		var dec = current(List.of(provider(40, decorator)));
		assertSame(Settings.SYSTEM_PROPERTY_SOURCE, dec.sources().get(1), "a decorator is not detected, so the fallback is kept");
	}

	// ------------------------------------------------------------------------------------------------------------
	// Precedence parity (these pass before and after D5)
	// ------------------------------------------------------------------------------------------------------------

	private Set<String> parityKeys(List<String> matrix) {
		var keys = new LinkedHashSet<>(matrix);
		keys.addAll(System.getProperties().stringPropertyNames());
		for (var e : System.getenv().keySet()) {
			keys.add(e);
			keys.add(e.toLowerCase(Locale.ROOT).replace('_', '.'));
			keys.add(camel(e));
		}
		return keys;
	}

	private void assertParity(List<PropertySourceProvider> providers, List<String> matrix) {
		var l = legacy(providers);
		var c = current(providers);
		for (var k : parityKeys(matrix)) {
			assertEquals(walk(l, k), walk(c, k), "walk parity for key: " + k);
			assertEquals(l.get(k).orElse(null), c.get(k).orElse(null), "get parity for key: " + k);
			assertEquals(l.get(k).isPresent(), c.get(k).isPresent(), "isPresent parity for key: " + k);
		}
	}

	private static final List<String> MATRIX = List.of(
		"d04.sp", "d04.sp.man", "d04.sp.env", "d04.sp.args", "d04.sp.cfg", "d04.sp.nullval", "d04.man", "d04.env", "d04.args", "d04.none");

	private List<PropertySourceProvider> parityProviders(boolean withSysPropProvider) {
		var man = new HashMap<String,String>();
		man.put("d04.sp.man", "man");
		man.put("d04.man", "man");
		var env = new HashMap<String,String>();
		env.put("d04.sp.env", "dotenv");
		env.put("d04.env", "dotenv");
		var args = new HashMap<String,String>();
		args.put("d04.sp.args", "args");
		args.put("d04.args", "args");
		var cfg = new HashMap<String,String>();
		cfg.put("d04.sp.cfg", "cfg");
		cfg.put("d04.sp.nullval", null);
		var l = new ArrayList<PropertySourceProvider>();
		l.add(provider(10, mapSource(man)));
		l.add(provider(20, mapSource(env)));
		l.add(new SystemEnvPropertySourceProvider());
		if (withSysPropProvider)
			l.add(new SystemPropertyPropertySourceProvider());
		l.add(provider(50, mapSource(args)));
		l.add(provider(100, mapSource(cfg)));
		return l;
	}

	private void withParitySysProps(Runnable r) {
		var names = List.of("d04.sp", "d04.sp.man", "d04.sp.env", "d04.sp.args", "d04.sp.cfg", "d04.sp.nullval");
		names.forEach(n -> System.setProperty(n, "sys"));
		try {
			r.run();
		} finally {
			names.forEach(System::clearProperty);
		}
	}

	@Test void d04_precedenceParity_case1() {
		withParitySysProps(() -> assertParity(parityProviders(true), MATRIX));
		// The explicit null in the config slot (order 100, newest) shadows the "sys" system property on both chains.
		assertEquals("P:null", walk(current(parityProviders(true)), "d04.sp.nullval"));
	}

	@Test void d05_precedenceParity_case2_noSysPropProvider() {
		withParitySysProps(() -> assertParity(parityProviders(false), MATRIX));
	}

	@Test void d06_liveSystemProperties_afterChange() {
		var c = current(realProviders());
		assertNull(c.get("d06.k").orElse(null));
		System.setProperty("d06.k", "v");
		try {
			assertEquals("v", c.get("d06.k").orElse(null));
		} finally {
			System.clearProperty("d06.k");
		}
		assertNull(c.get("d06.k").orElse(null));
	}

	@Test void d10_camel_underscoreOnlyEnvKeys_doNotThrow() {
		assertEquals("", camel("_"));
		assertEquals("", camel("__"));
		assertEquals("fooBar", camel("FOO__BAR"));
	}

	@Test void d09_commonsProviderOrders_arePinned() {
		assertEquals(10, new ManifestFilePropertySourceProvider().order());
		assertEquals(20, new DotenvPropertySourceProvider().order());
		assertEquals(30, new SystemEnvPropertySourceProvider().order());
		assertEquals(40, new SystemPropertyPropertySourceProvider().order());
		assertEquals(50, new ArgsPropertySourceProvider().order());
	}
}
