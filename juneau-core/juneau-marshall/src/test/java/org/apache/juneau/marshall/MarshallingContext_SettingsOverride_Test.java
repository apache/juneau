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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Overrides must still take effect after the run-constant source caches are warm.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
@ResourceLock("org.apache.juneau.commons.settings.Settings.singleton")  // c02 setGlobal, c04 addSource, clearGlobal in setup
class MarshallingContext_SettingsOverride_Test extends TestBase {

	private static final String KEY = "MarshallingContext.ignoreUnknownBeanProperties";

	@BeforeEach void warm() {
		Settings.get().clearLocal();
		Settings.get().clearGlobal();
		for (var i = 0; i < 3; i++) {
			MarshallingContext.create().build();
			JsonSerializer.create().build();
		}
	}

	@AfterEach void clean() {
		Settings.get().clearLocal();
		Settings.get().clearGlobal();
	}

	private static boolean ignoreUnknown() {
		return MarshallingContext.create().build().isIgnoreUnknownBeanProperties();
	}

	@Test void c01_local_override_seenThenGone() {
		assertFalse(ignoreUnknown());
		Settings.get().setLocal(KEY, "true");
		assertTrue(ignoreUnknown());
		Settings.get().clearLocal();
		assertFalse(ignoreUnknown());
	}

	@Test void c02_global_override_seenByOtherThread_localIsNot() throws Exception {
		Settings.get().setGlobal(KEY, "true");
		var other = new boolean[1];
		var t = new Thread(() -> other[0] = ignoreUnknown());
		t.start();
		t.join(30_000);
		assertTrue(other[0], "a global override reaches other threads");
		Settings.get().unsetGlobal(KEY);
		assertFalse(ignoreUnknown());

		Settings.get().setLocal(KEY, "true");
		var other2 = new boolean[1];
		var t2 = new Thread(() -> other2[0] = ignoreUnknown());
		t2.start();
		t2.join(30_000);
		assertFalse(other2[0], "a thread-local override stays on its thread");
	}

	@Test void c03_liveSystemProperty_seenThenGone() throws Exception {
		var key = "Serializer.trimStrings";
		assertEquals("\" a \"", JsonSerializer.create().build().write(" a "));
		System.setProperty(key, "true");
		try {
			assertEquals("\"a\"", JsonSerializer.create().build().write(" a "));
		} finally {
			System.clearProperty(key);
		}
		assertEquals("\" a \"", JsonSerializer.create().build().write(" a "));
	}

	@Test void c04_runtimeSource_seen_changeSeen_removedGone() {
		var store = new MapStore();
		var settings = Settings.get();
		settings.addSource(store);
		try {
			assertFalse(ignoreUnknown(), "an empty mutable source is not cached as a miss");
			store.set(KEY, "true");
			assertTrue(ignoreUnknown());
			store.set(KEY, "false");
			assertFalse(ignoreUnknown());
			store.set(KEY, "true");
		} finally {
			settings.removeSource(store);
		}
		assertFalse(ignoreUnknown());
	}

	@ResourceLock(Resources.LOCALE)
	@Test void c05_localeDefault_isReadAtConstruction() {
		var saved = Locale.getDefault();
		try {
			Locale.setDefault(Locale.CANADA_FRENCH);
			assertEquals(Locale.CANADA_FRENCH, MarshallingContext.create().build().getLocale());
			Locale.setDefault(Locale.GERMANY);
			assertEquals(Locale.GERMANY, MarshallingContext.create().build().getLocale());
		} finally {
			Locale.setDefault(saved);
		}
	}

	@Test void c06_cachedEnvHit_stillShadowedByLocalOverride() {
		var settings = Settings.create()
			.addSource(CachingPropertySource.of(name -> name.equals("c06.k") ? PropertyLookupResult.present("env") : PropertyLookupResult.missing()))
			.build();
		assertEquals("env", settings.get("c06.k").orElse(null));
		assertEquals("env", settings.get("c06.k").orElse(null));
		settings.setLocal("c06.k", "local");
		try {
			assertEquals("local", settings.get("c06.k").orElse(null));
		} finally {
			settings.clearLocal();
		}
		assertEquals("env", settings.get("c06.k").orElse(null));
	}
}
