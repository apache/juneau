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
package org.apache.juneau.encoders;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.junit5.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link EncoderSet.Builder#overridingBeanStore(BeanStore)} threads the overlay
 * through to the builder's reflective bean lookups so test-time overrides win over the builder's
 * regular bean store entries.
 */
class EncoderSet_OverridingBeanStore_Test extends TestBase {

	interface Marker {
		String tag();
	}

	@Test
	void a01_overlay_winsOverLocalRegistration() {
		var defaultStore = new BasicBeanStore();
		defaultStore.addBean(Marker.class, () -> "production");

		var overlay = new TestBeanStore().override(Marker.class, () -> "test-overlay");

		var b = EncoderSet.create(defaultStore).overridingBeanStore(overlay);

		var resolved = b.beanStore().getBean(Marker.class).orElseThrow();
		assertEquals("test-overlay", resolved.tag(),
			"Overlay must be consulted before the builder's regular bean store");
	}

	@Test
	void a02_overlay_fallsThroughForUnsupportedTypes() {
		var defaultStore = new BasicBeanStore();
		defaultStore.addBean(Marker.class, () -> "production");

		var overlay = new TestBeanStore();
		var b = EncoderSet.create(defaultStore).overridingBeanStore(overlay);

		var resolved = b.beanStore().getBean(Marker.class).orElseThrow();
		assertEquals("production", resolved.tag());
	}

	@Test
	void a03_build_succeedsWithOverlayInstalled() {
		var overlay = new TestBeanStore();
		var set = EncoderSet.create()
			.overridingBeanStore(overlay)
			.add(GzipEncoder.class)
			.build();
		assertNotNull(set);
	}
}
