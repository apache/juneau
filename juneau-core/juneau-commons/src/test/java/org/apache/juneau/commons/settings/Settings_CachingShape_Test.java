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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Pins which sources are wrapped in a {@link CachingPropertySource} and which are not.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
@ResourceLock(value = "org.apache.juneau.commons.settings.Settings.singleton", mode = ResourceAccessMode.READ)  // reads the singleton chain
class Settings_CachingShape_Test extends TestBase {

	private static final Set<Class<?>> RUN_CONSTANT = Set.of(
		SystemEnvPropertySource.class, DotenvPropertySource.class, ManifestFilePropertySource.class, ArgsPropertySource.class);

	private static PropertySource unwrap(PropertySource s) {
		return s instanceof CachingPropertySource c ? c.delegate() : s;
	}

	@Test void e01a_relaxedEnvConstant_isCachingOverRelaxed() {
		assertInstanceOf(CachingPropertySource.class, Settings.RELAXED_SYSTEM_ENV_SOURCE);
		assertInstanceOf(RelaxedPropertySource.class, ((CachingPropertySource)Settings.RELAXED_SYSTEM_ENV_SOURCE).delegate());
	}

	@Test void e01b_singleton_runConstantSources_areCaching() {
		var seen = new HashSet<Class<?>>();
		for (var s : Settings.get().sources()) {
			var cls = unwrap(s).getClass();
			if (RUN_CONSTANT.contains(cls)) {
				assertInstanceOf(CachingPropertySource.class, s, cls.getSimpleName() + " must be wrapped");
				seen.add(cls);
			}
		}
		// All four providers are in META-INF/services and their create() never returns null, so all four are always there.
		assertEquals(RUN_CONSTANT, seen, "every run-constant source must be on the singleton chain, wrapped");
	}

	@Test void e02_singleton_noSystemPropertySourceIsCaching() {
		for (var s : Settings.get().sources()) {
			if (s instanceof CachingPropertySource c) {
				assertNotSame(Settings.SYSTEM_PROPERTY_SOURCE, c.delegate());
				assertFalse(c.delegate() instanceof SystemPropertyPropertySource);
			}
		}
	}

	@Test void e04_providers_wrapExactlyTheRunConstantOnes() {
		assertWrapped(new SystemEnvPropertySourceProvider().create(), SystemEnvPropertySource.class);
		assertWrapped(new DotenvPropertySourceProvider().create(), DotenvPropertySource.class);
		assertWrapped(new ManifestFilePropertySourceProvider().create(), ManifestFilePropertySource.class);
		assertWrapped(new ArgsPropertySourceProvider().create(), ArgsPropertySource.class);
		// The system-property provider is never wrapped: system properties are mutable.
		assertEquals(SystemPropertyPropertySource.class, new SystemPropertyPropertySourceProvider().create().getClass());
	}

	private static void assertWrapped(PropertySource s, Class<?> expectedDelegate) {
		assertInstanceOf(CachingPropertySource.class, s);
		assertEquals(expectedDelegate, ((CachingPropertySource)s).delegate().getClass());
	}

	@Test void e05_wrapped_source_stillShadowedByLocalOverride() {
		var settings = Settings.create()
			.addSource(CachingPropertySource.of(name -> name.equals("e05.k") ? PropertyLookupResult.present("env") : PropertyLookupResult.missing()))
			.build();
		assertEquals("env", settings.get("e05.k").orElse(null));
		settings.setLocal("e05.k", "local");
		try {
			assertEquals("local", settings.get("e05.k").orElse(null));
		} finally {
			settings.clearLocal();
		}
		assertEquals("env", settings.get("e05.k").orElse(null));
	}
}
