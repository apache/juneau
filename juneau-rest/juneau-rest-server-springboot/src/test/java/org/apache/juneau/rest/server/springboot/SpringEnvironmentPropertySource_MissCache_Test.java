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
package org.apache.juneau.rest.server.springboot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jakarta.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.PropertyLookupResult;
import org.apache.juneau.commons.settings.Settings;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.core.env.*;
import org.springframework.mock.env.*;
import org.springframework.web.context.support.*;

/**
 * Tests for the miss cache in {@link SpringEnvironmentPropertySource} (J1038 Part 2).  Keys are mixed-case (invalid
 * Spring Boot names, so cacheable) unless stated.
 */
@org.apache.juneau.testing.SpringbootTest
@ResourceLock(Resources.SYSTEM_PROPERTIES)
@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")
class SpringEnvironmentPropertySource_MissCache_Test extends TestBase {

	private static final String K = "Juneau.Miss.Key";
	private static final String DISABLE = "juneau.spring.missCache.disable";

	/** A map source that counts containsProperty calls per name. */
	static final class CountingSource extends MapPropertySource {
		private final Map<String,Integer> calls = new ConcurrentHashMap<>();

		CountingSource(String name, Map<String,Object> map) {
			super(name, map);
		}

		@Override
		public boolean containsProperty(String name) {
			calls.merge(name, 1, Integer::sum);
			return super.containsProperty(name);
		}

		int calls(String name) {
			return calls.getOrDefault(name, 0);
		}
	}

	private static CountingSource counting() {
		return new CountingSource("c", new HashMap<>());
	}

	private static StandardEnvironment env(org.springframework.core.env.PropertySource<?> first) {
		var env = new StandardEnvironment();
		env.getPropertySources().addFirst(first);
		return env;
	}

	private static MapPropertySource holding(String name) {
		return new MapPropertySource(name, Map.<String,Object>of(K, "v"));
	}

	private static String value(PropertyLookupResult r) {
		return r.value().orElse(null);
	}

	private static void assertSeenAfter(Consumer<MutablePropertySources> edit) {
		var env = env(counting());
		var src = new SpringEnvironmentPropertySource(env);
		assertFalse(src.get(K).isPresent());
		edit.accept(env.getPropertySources());
		assertEquals("v", value(src.get(K)));
	}

	@Test void a01_repeatedMiss_walksOnce() {
		var c = counting();
		var src = new SpringEnvironmentPropertySource(env(c));
		for (var i = 0; i < 100; i++)
			assertFalse(src.get(K).isPresent());
		assertEquals(1, c.calls(K));
		assertEquals(1, src.missCount());
	}

	@Test void a01b_servletEnvironment_initParamNamesBuiltOnce() {
		var sc = mock(ServletContext.class);
		when(sc.getInitParameterNames()).thenAnswer(x -> Collections.enumeration(List.of("p1", "p2", "p3")));
		var env = new StandardServletEnvironment();
		env.initPropertySources(sc, null);
		var src = new SpringEnvironmentPropertySource(env);
		for (var i = 0; i < 100; i++)
			assertFalse(src.get(K).isPresent());
		verify(sc, times(1)).getInitParameterNames();
	}

	@Test void a02_addFirstAfterMiss_isSeen() {
		assertSeenAfter(x -> x.addFirst(holding("first")));
	}

	@Test void a02b_otherInsertsAfterMiss_areSeen() {
		assertSeenAfter(x -> x.addLast(holding("last")));
		assertSeenAfter(x -> x.addBefore("c", holding("before")));
		assertSeenAfter(x -> x.addAfter("c", holding("after")));
	}

	@Test void a03_replaceAfterMiss_isSeen() {
		assertSeenAfter(x -> x.replace("c", holding("c")));
	}

	@Test void a04_removeOfHit_isAbsent() {
		var env = env(holding("h"));
		var src = new SpringEnvironmentPropertySource(env);
		assertEquals("v", value(src.get(K)));
		env.getPropertySources().remove("h");
		assertFalse(src.get(K).isPresent());
		assertFalse(src.get(K).isPresent());
	}

	@Test void a05_systemPropertyAfterMiss_isSeen() {
		var k = "Juneau.A05.Key";
		var src = new SpringEnvironmentPropertySource(new StandardEnvironment());
		try {
			assertFalse(src.get(k).isPresent());
			assertEquals(1, src.missCount());
			System.setProperty(k, "v");
			assertEquals("v", value(src.get(k)));
			assertEquals(0, src.missCount());
		} finally {
			System.clearProperty(k);
		}
	}

	@Test void a06_inPlaceMutation_invalidName_staysMissingUntilInvalidate() {
		var map = new HashMap<String,Object>();
		var src = new SpringEnvironmentPropertySource(env(new MapPropertySource("m", map)));
		assertFalse(src.get(K).isPresent());
		map.put(K, "v");
		assertFalse(src.get(K).isPresent(), "M5: documented staleness for a cached invalid-name miss");
		src.invalidate();
		assertEquals("v", value(src.get(K)));
	}

	@Test void a06b_inPlaceMutation_validName_isSeen() {
		var k = "my.app.key";
		var map = new HashMap<String,Object>();
		var src = new SpringEnvironmentPropertySource(env(new MapPropertySource("m", map)));
		assertFalse(src.get(k).isPresent());
		assertEquals(0, src.missCount(), "Valid Boot names are never cached");
		map.put(k, "v");
		assertEquals("v", value(src.get(k)));
	}

	@Test void a07_cap_keepsFirstKeys_passesThroughWhenFull() {
		var c = new CountingSource("c", new HashMap<>(Map.of("Present.Key", "p")));
		var src = new SpringEnvironmentPropertySource(env(c));
		var hot = List.of("Hot.A", "Hot.B", "Hot.C");
		hot.forEach(k -> assertFalse(src.get(k).isPresent()));
		for (var i = 0; i < SpringEnvironmentPropertySource.MISS_CACHE_MAX_ENTRIES + 100; i++)
			assertFalse(src.get("Flood.Key" + i).isPresent());
		assertEquals(SpringEnvironmentPropertySource.MISS_CACHE_MAX_ENTRIES, src.missCount());
		for (var k : hot) {
			assertFalse(src.get(k).isPresent());
			assertEquals(1, c.calls(k), "Hot key " + k + " must still be cached after the flood");
		}
		assertEquals("p", value(src.get("Present.Key")));
		var beyond = "Beyond.Cap";
		assertFalse(src.get(beyond).isPresent());
		assertFalse(src.get(beyond).isPresent());
		assertEquals(2, c.calls(beyond));
	}

	@Test void a08_profilesActive_followsSetActiveProfiles() {
		var env = new StandardEnvironment();
		var src = new SpringEnvironmentPropertySource(env);
		env.setActiveProfiles("p1");
		assertEquals("p1", value(src.get("juneau.profiles.active")));
		env.setActiveProfiles("p2", "p3");
		assertEquals("p2,p3", value(src.get("juneau.profiles.active")));
		env.setActiveProfiles();
		assertFalse(src.get("juneau.profiles.active").isPresent());
		assertEquals(0, src.missCount());
	}

	@Test void a09_optOutInEnvironment_everyMissWalks() {
		var c = new CountingSource("c", new HashMap<>(Map.of(DISABLE, "true")));
		var src = new SpringEnvironmentPropertySource(env(c));
		for (var i = 0; i < 3; i++)
			assertFalse(src.get(K).isPresent());
		assertEquals(3, c.calls(K));
		assertEquals(0, src.missCount());
	}

	@Test void a09b_optOutAsSystemProperty_everyMissWalks() {
		var c = counting();
		System.setProperty(DISABLE, "true");
		try {
			var src = new SpringEnvironmentPropertySource(env(c));
			for (var i = 0; i < 3; i++)
				assertFalse(src.get(K).isPresent());
			assertEquals(3, c.calls(K));
		} finally {
			System.clearProperty(DISABLE);
		}
	}

	@Test void a09c_optOutAfterFirstLookup_isIgnored() {
		var map = new HashMap<String,Object>();
		var c = new CountingSource("c", map);
		var src = new SpringEnvironmentPropertySource(env(c));
		assertFalse(src.get(K).isPresent());
		map.put(DISABLE, "true");
		assertFalse(src.get(K).isPresent());
		assertFalse(src.get(K).isPresent());
		assertEquals(1, c.calls(K));
	}

	@Test void a09d_optOutIsNotReadThroughSettings() {
		var seen = ConcurrentHashMap.<String>newKeySet();
		org.apache.juneau.commons.settings.PropertySource spy = n -> {
			seen.add(n);
			return PropertyLookupResult.missing();
		};
		Settings.get().addSource(spy);
		try {
			new SpringEnvironmentPropertySource(new StandardEnvironment()).get(K);
		} finally {
			Settings.get().removeSource(spy);
		}
		assertFalse(seen.contains(DISABLE));
	}

	@Test void a10_nonConfigurableEnvironment_noCaching() {
		var env = mock(Environment.class);
		when(env.containsProperty(K)).thenReturn(false);
		var src = new SpringEnvironmentPropertySource(env);
		for (var i = 0; i < 3; i++)
			assertFalse(src.get(K).isPresent());
		verify(env, times(3)).containsProperty(K);
		assertEquals(0, src.missCount());
	}

	@Test void a11_concurrentAddFirst_isSeenByLookupsStartedAfter() throws Exception {
		var env = env(counting());
		var src = new SpringEnvironmentPropertySource(env);
		var keys = List.of(K, "Other.A", "Other.B", "my.app.key");
		var added = new AtomicBoolean();
		var failures = new ConcurrentLinkedQueue<String>();
		var start = new CountDownLatch(1);
		var running = new CountDownLatch(8);
		var pool = Executors.newFixedThreadPool(8);
		try {
			var futures = new ArrayList<Future<?>>();
			for (var t = 0; t < 8; t++)
				futures.add(pool.submit(() -> {
					start.await();
					for (var i = 0; i < 20_000; i++) {
						var after = added.get();
						var r = src.get(keys.get(i % keys.size()));
						if (after && i % keys.size() == 0 && ! r.isPresent())
							failures.add("lookup " + i + " started after addFirst returned but missed");
						if (i == 0)
							running.countDown();
					}
					return null;
				}));
			start.countDown();
			assertTrue(running.await(60, TimeUnit.SECONDS));
			env.getPropertySources().addFirst(holding("added"));
			added.set(true);
			for (var f : futures)
				f.get(60, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
		assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
		assertEquals("v", value(src.get(K)));
	}

	@Test void a12_mockEnvironmentSetProperty_staysMissingUntilInvalidate() {
		var env = new MockEnvironment();
		var src = new SpringEnvironmentPropertySource(env);
		assertFalse(src.get(K).isPresent());
		env.setProperty(K, "v");
		assertFalse(src.get(K).isPresent(), "Trap for test authors: MockEnvironment.setProperty mutates in place");
		src.invalidate();
		assertEquals("v", value(src.get(K)));
	}
}
