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
package org.apache.juneau.config.store;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.format.*;
import org.apache.juneau.config.internal.*;
import org.junit.jupiter.api.*;

/**
 * Direct tests for {@link ProfileConfigStore} (the profile-overlay store decorator) and {@link ProfileMerge}
 * (the overlay engine), exercising read pass-through, delegation, name derivation, lifecycle, and the merge edges.
 */
@SuppressWarnings({
	"resource" // MemoryStore/ProfileConfigStore are test fixtures; lifecycle managed by the test, not a real leak.
})
class ProfileConfigStore_Test extends TestBase {

	private static MemoryStore store(String... kv) {
		var s = MemoryStore.create().build();
		for (var i = 0; i < kv.length; i += 2)
			s.update(kv[i], kv[i + 1]);
		return s;
	}

	private static ProfileConfigStore wrap(MemoryStore delegate, String baseName, String... profiles) {
		return ProfileConfigStore.create().delegate(delegate).baseName(baseName).profiles(Arrays.asList(profiles)).format(IniConfigFormat.INSTANCE).build();
	}

	// =================================================================================
	// A. read() — base name merges; other names pass through.
	// =================================================================================

	@Test void a01_readBaseNameMerges() throws Exception {
		var d = store("App.cfg", "[S]\na = base\n", "App-stage.cfg", "[S]\na = stage\n");
		var p = wrap(d, "App.cfg", "stage");
		assertTrue(p.read("App.cfg").contains("stage"));
	}

	@Test void a02_readOtherNamePassesThrough() throws Exception {
		var d = store("App.cfg", "[S]\na = base\n", "Other.cfg", "[S]\nx = y\n");
		var p = wrap(d, "App.cfg", "stage");
		assertTrue(p.read("Other.cfg").contains("x = y"));
	}

	@Test void a03_noProfilesReturnsBaseVerbatim() throws Exception {
		var d = store("App.cfg", "[S]\na = base\n");
		var p = wrap(d, "App.cfg");  // no profiles
		assertTrue(p.read("App.cfg").contains("base"));
	}

	// =================================================================================
	// B. Delegation — exists / write delegate to the wrapped store.
	// =================================================================================

	@Test void b01_existsDelegates() {
		var d = store("App.cfg", "x");
		var p = wrap(d, "App.cfg", "stage");
		assertTrue(p.exists("App.cfg"));
		assertFalse(p.exists("Nope.cfg"));
	}

	@Test void b02_writeDelegates() throws Exception {
		var d = store("App.cfg", "[S]\na = base\n");
		var p = wrap(d, "App.cfg", "stage");
		p.write("Other.cfg", null, "[S]\nx = written\n");
		assertTrue(d.read("Other.cfg").contains("written"));
	}

	// =================================================================================
	// C. profileName() — inserts -<profile> before the extension; appends when none.
	// =================================================================================

	@Test void c01_profileNameWithExtension() {
		var p = wrap(store("App.cfg", ""), "App.cfg", "stage");
		assertEquals("App-stage.cfg", p.profileName("stage"));
	}

	@Test void c02_profileNameNoExtension() {
		var p = wrap(store("App", ""), "App", "stage");
		assertEquals("App-stage", p.profileName("stage"));
	}

	// =================================================================================
	// D. Lifecycle — copy() round-trips; close() unregisters + closes delegate.
	// =================================================================================

	@Test void d01_copyRoundTrips() {
		var p = wrap(store("App.cfg", ""), "App.cfg", "stage");
		var p2 = p.copy().build();  // store.copy() -> Builder(ProfileConfigStore)
		assertEquals("App-stage.cfg", p2.profileName("stage"));
	}

	@Test void d01b_builderCopyRoundTrips() {
		var d = store("App.cfg", "");
		var b = ProfileConfigStore.create().delegate(d).baseName("App.cfg").profiles(List.of("stage")).format(IniConfigFormat.INSTANCE);
		var p = b.copy().build();  // Builder.copy() -> Builder(Builder)
		assertEquals("App-stage.cfg", p.profileName("stage"));
	}

	@Test void d02_closeIsClean() {
		var d = store("App.cfg", "[S]\na = base\n", "App-stage.cfg", "[S]\na = stage\n");
		var p = wrap(d, "App.cfg", "stage");
		assertDoesNotThrow(p::close);  // unregisters listeners + closes delegate
	}

	// =================================================================================
	// E. ProfileMerge edges — null format, null base, blank/empty profiles.
	// =================================================================================

	@Test void e01_mergeNullFormatDefaultsIni() throws Exception {
		var d = store();
		var merged = ProfileMerge.merge(d, "App.cfg", "[S]\na = base\n", List.of("[S]\na = over\n"), null);
		assertTrue(merged.contains("over"));
	}

	@Test void e02_mergeNullBaseContents() throws Exception {
		var d = store();
		var merged = ProfileMerge.merge(d, "App.cfg", null, List.of("[S]\na = over\n"), IniConfigFormat.INSTANCE);
		assertTrue(merged.contains("over"));
	}

	@Test void e03_mergeBlankAndNullProfilesSkipped() throws Exception {
		var d = store();
		var profiles = new ArrayList<String>();
		profiles.add("");      // blank — skipped
		profiles.add(null);    // null — skipped
		profiles.add("[S]\na = over\n");
		var merged = ProfileMerge.merge(d, "App.cfg", "[S]\na = base\n", profiles, IniConfigFormat.INSTANCE);
		assertTrue(merged.contains("over"));
	}

	@Test void e04_mergeNoProfilesReturnsBase() throws Exception {
		var d = store();
		var merged = ProfileMerge.merge(d, "App.cfg", "[S]\na = base\n", List.of(), IniConfigFormat.INSTANCE);
		assertTrue(merged.contains("base"));
	}
}
