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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

/**
 * Tests for config <b>profiles</b> &mdash; {@code <name>-<profile>} overlays activated via
 * {@code Config.Builder.profiles(...)} (the standalone path; the Spring piggyback is covered in the springboot module).
 */
@SuppressWarnings({
	"resource" // MemoryStore/Config are test fixtures; lifecycle managed by the test, not a real leak.
})
class ConfigProfiles_Test extends TestBase {

	private static MemoryStore store(String... namesAndContents) {
		var s = MemoryStore.create().build();
		for (var i = 0; i < namesAndContents.length; i += 2)
			s.update(namesAndContents[i], namesAndContents[i + 1]);
		return s;
	}

	private static Config config(MemoryStore s, String name, String... profiles) {
		try {
			return Config.create().store(s).name(name).profiles(profiles).build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// =================================================================================
	// A. Basic overlay — profile wins over base; base-only keys survive.
	// =================================================================================

	@Test void a01_profileOverridesBase_baseOnlySurvives() {
		var s = store(
			"App.cfg", "[S]\na = base-a\nb = base-b\n",
			"App-stage.cfg", "[S]\na = stage-a\n");
		var c = config(s, "App.cfg", "stage");
		assertEquals("stage-a", c.get("S/a").orElse(null));  // profile wins
		assertEquals("base-b", c.get("S/b").orElse(null));   // base-only survives
	}

	@Test void a02_noProfilesIsBaseOnly() {
		var s = store("App.cfg", "[S]\na = base-a\n", "App-stage.cfg", "[S]\na = stage-a\n");
		var c = config(s, "App.cfg");
		assertEquals("base-a", c.get("S/a").orElse(null));
	}

	@Test void a03_newSectionFromProfile() {
		var s = store(
			"App.cfg", "[S]\na = base-a\n",
			"App-stage.cfg", "[T]\nx = stage-x\n");
		var c = config(s, "App.cfg", "stage");
		assertEquals("base-a", c.get("S/a").orElse(null));
		assertEquals("stage-x", c.get("T/x").orElse(null));  // section only in the profile
	}

	// =================================================================================
	// B. Multiple active profiles — last-active-profile wins.
	// =================================================================================

	@Test void b01_lastActiveProfileWins() {
		var s = store(
			"App.cfg", "[S]\na = base\n",
			"App-one.cfg", "[S]\na = one\n",
			"App-two.cfg", "[S]\na = two\n");
		var c = config(s, "App.cfg", "one", "two");
		assertEquals("two", c.get("S/a").orElse(null));  // two activated last
	}

	@Test void b02_earlierProfileStillContributesUnsharedKeys() {
		var s = store(
			"App.cfg", "[S]\na = base\n",
			"App-one.cfg", "[S]\nb = one-b\n",
			"App-two.cfg", "[S]\na = two-a\n");
		var c = config(s, "App.cfg", "one", "two");
		assertEquals("two-a", c.get("S/a").orElse(null));   // two wins for a
		assertEquals("one-b", c.get("S/b").orElse(null));   // one's unique key survives
	}

	// =================================================================================
	// C. Missing profile file is a no-op overlay (base unchanged).
	// =================================================================================

	@Test void c01_missingProfileFileNoOp() {
		var s = store("App.cfg", "[S]\na = base-a\n");  // no App-stage.cfg seeded
		var c = config(s, "App.cfg", "stage");
		assertEquals("base-a", c.get("S/a").orElse(null));
	}

	// =================================================================================
	// D. Live reload — a change to a profile overlay file re-fires config-change listeners.
	// =================================================================================

	@Test void d01_profileFileChangeTriggersReload() throws Exception {
		var s = store(
			"App.cfg", "[S]\na = base-a\n",
			"App-stage.cfg", "[S]\na = stage-a\n");
		var c = config(s, "App.cfg", "stage");
		assertEquals("stage-a", c.get("S/a").orElse(null));

		var latch = new CountDownLatch(1);
		var seen = new AtomicReference<String>();
		c.addListener((ConfigEventListener) events -> { seen.set("fired"); latch.countDown(); });

		// Change the PROFILE file underneath the store.
		s.update("App-stage.cfg", "[S]\na = stage-a2\n");

		assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected a config-change event from the profile-file update");
		assertEquals("stage-a2", c.get("S/a").orElse(null));  // re-merged value visible
	}

	@Test void d02_baseFileChangeTriggersReload() throws Exception {
		var s = store(
			"App.cfg", "[S]\na = base-a\nb = base-b\n",
			"App-stage.cfg", "[S]\na = stage-a\n");
		var c = config(s, "App.cfg", "stage");

		var latch = new CountDownLatch(1);
		c.addListener((ConfigEventListener) events -> latch.countDown());

		s.update("App.cfg", "[S]\na = base-a\nb = base-b2\n");

		assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected a config-change event from the base-file update");
		assertEquals("stage-a", c.get("S/a").orElse(null));   // profile still wins after base reload
		assertEquals("base-b2", c.get("S/b").orElse(null));   // base change visible
	}
}
