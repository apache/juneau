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
package org.apache.juneau.rest;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Regression test guarding the BeanStore-routed {@code @Rest(config=...)} bridge against
 * re-introducing the per-{@code RestContext} {@link Settings} source leak that caused the
 * 6.7&times; full-suite slowdown during an earlier prototype.
 *
 * <p>
 * In FINISHED-79, an earlier draft of @Rest(config=...) bridging registered a
 * {@code ConfigPropertySource} per resolved RestContext via {@code Settings.get().addSource(...)}.
 * MockRestClient statically caches RestContext instances and never invokes {@code destroy()},
 * so the global {@code Settings} source list grew unbounded across the test suite — a 6.7x
 * wall-clock regression as {@code Settings.get(name)}'s reverse-walk got O(N).
 *
 * <p>
 * This test asserts that:
 * <ol>
 * 	<li>Building a single @Rest(config=...) MockRestClient adds zero new sources to
 * 		{@code Settings.get()}.
 * 	<li>Building hundreds of mixed @Rest / @Rest(config=...) MockRestClients leaves the source
 * 		count unchanged.
 * </ol>
 *
 * <p>
 * If a future change accidentally routes @Rest(config=...) Configs through
 * {@code Settings.get().addSource(...)} (or any other process-wide register), this test fails
 * immediately, well before the performance regression shows up in wall-clock test time.
 */
@SuppressWarnings({
	"serial" // BasicRestServlet is Serializable; not relevant in test fixtures.
})
class Settings_NoLeakedSources_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Distinct @Rest resources — mixed config=/no-config — to exercise both code paths.
	//------------------------------------------------------------------------------------------------------------------

	@Rest public static class NoConfig01 extends BasicRestServlet {}
	@Rest public static class NoConfig02 extends BasicRestServlet {}
	@Rest public static class NoConfig03 extends BasicRestServlet {}
	@Rest public static class NoConfig04 extends BasicRestServlet {}
	@Rest public static class NoConfig05 extends BasicRestServlet {}
	@Rest public static class NoConfig06 extends BasicRestServlet {}
	@Rest public static class NoConfig07 extends BasicRestServlet {}
	@Rest public static class NoConfig08 extends BasicRestServlet {}
	@Rest public static class NoConfig09 extends BasicRestServlet {}
	@Rest public static class NoConfig10 extends BasicRestServlet {}

	// We deliberately use a config name that points at a non-existent file. The
	// FileStore.DEFAULT-backed Config still builds (the in-memory ConfigMap is empty) and the
	// bridge still registers a "rest.config" PropertySource in the RestContext's BeanStore —
	// exactly the path we want to exercise without polluting the test cwd with fixture files.
	@Rest(config="settings_noleakedsources_test_a.cfg") public static class WithConfig01 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_b.cfg") public static class WithConfig02 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_c.cfg") public static class WithConfig03 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_d.cfg") public static class WithConfig04 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_e.cfg") public static class WithConfig05 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_f.cfg") public static class WithConfig06 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_g.cfg") public static class WithConfig07 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_h.cfg") public static class WithConfig08 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_i.cfg") public static class WithConfig09 extends BasicRestServlet {}
	@Rest(config="settings_noleakedsources_test_j.cfg") public static class WithConfig10 extends BasicRestServlet {}

	private static final Class<?>[] NO_CONFIG_CLASSES = {
		NoConfig01.class, NoConfig02.class, NoConfig03.class, NoConfig04.class, NoConfig05.class,
		NoConfig06.class, NoConfig07.class, NoConfig08.class, NoConfig09.class, NoConfig10.class,
	};

	private static final Class<?>[] WITH_CONFIG_CLASSES = {
		WithConfig01.class, WithConfig02.class, WithConfig03.class, WithConfig04.class, WithConfig05.class,
		WithConfig06.class, WithConfig07.class, WithConfig08.class, WithConfig09.class, WithConfig10.class,
	};

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance — single instantiation adds zero new sources.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_singleInstantiation_zeroNewSources() {
		var before = Settings.get().sourceCount();
		MockRestClient.build(WithConfig01.class);
		var after = Settings.get().sourceCount();
		assertEquals(before, after,
			"@Rest(config=...) must not leak ConfigPropertySource instances into Settings.get().");
	}

	@Test void a02_singleInstantiation_noConfig_zeroNewSources() {
		var before = Settings.get().sourceCount();
		MockRestClient.build(NoConfig01.class);
		var after = Settings.get().sourceCount();
		assertEquals(before, after,
			"@Rest (no config=) must not add any new Settings sources.");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Acceptance — 200 instantiations across mixed classes leave the source count unchanged.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_twoHundredInstantiations_zeroNewSources() {
		var before = Settings.get().sourceCount();
		// 200 builds = 10 base classes × 10 builds × 2 mixes (config + no-config) = matches the
		// FINISHED-79 cache-saturation threshold that produced the 6.7x regression.
		for (var i = 0; i < 10; i++) {
			for (var c : NO_CONFIG_CLASSES)
				MockRestClient.build(c);
			for (var c : WITH_CONFIG_CLASSES)
				MockRestClient.build(c);
		}
		var after = Settings.get().sourceCount();
		assertEquals(before, after,
			"After 200 @Rest builds, Settings.get() source list must not grow (FINISHED-79 regression guard).");
	}
}
