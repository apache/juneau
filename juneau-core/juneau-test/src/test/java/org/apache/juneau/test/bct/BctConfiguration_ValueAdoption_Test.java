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
package org.apache.juneau.test.bct;

import static org.apache.juneau.test.bct.BctConfiguration.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Acceptance tests for {@code @Value}-driven defaults on {@link BctConfiguration.Defaults}.
 *
 * <p>
 * 3-test triad per migrated field per OQA #4 — system property set, unset (default), and {@code Settings.setGlobal}.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class BctConfiguration_ValueAdoption_Test {

	@AfterEach
	void cleanup() {
		var s = Settings.get();
		s.unsetGlobal(BCT_SORT_MAPS);
		s.unsetGlobal(BCT_SORT_COLLECTIONS);
		System.clearProperty(BCT_SORT_MAPS);
		System.clearProperty(BCT_SORT_COLLECTIONS);
	}

	// -------------------- sortMaps --------------------

	@Test
	void a01_sortMaps_set() {
		System.setProperty(BCT_SORT_MAPS, "true");
		assertTrue(BctConfiguration.defaults().isSortMaps());
	}

	@Test
	void a02_sortMaps_unset() {
		assertFalse(BctConfiguration.defaults().isSortMaps());
	}

	@Test
	void a03_sortMaps_setGlobal() {
		Settings.get().setGlobal(BCT_SORT_MAPS, "true");
		assertTrue(BctConfiguration.defaults().isSortMaps());
	}

	// -------------------- sortCollections --------------------

	@Test
	void b01_sortCollections_set() {
		System.setProperty(BCT_SORT_COLLECTIONS, "true");
		assertTrue(BctConfiguration.defaults().isSortCollections());
	}

	@Test
	void b02_sortCollections_unset() {
		assertFalse(BctConfiguration.defaults().isSortCollections());
	}

	@Test
	void b03_sortCollections_setGlobal() {
		Settings.get().setGlobal(BCT_SORT_COLLECTIONS, "true");
		assertTrue(BctConfiguration.defaults().isSortCollections());
	}
}
