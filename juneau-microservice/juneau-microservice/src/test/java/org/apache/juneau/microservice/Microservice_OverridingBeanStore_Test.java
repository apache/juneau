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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.test.junit.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that {@link Microservice.Builder#overridingBeanStore(BeanStore)} installs the overlay
 * in the {@code overridingParent} slot of the microservice's bean store so test-time overrides
 * resolve before locally-registered beans during the microservice's startup-time injection
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class Microservice_OverridingBeanStore_Test extends TestBase {

	interface ExternalApi {
		String describe();
	}

	@Configuration
	static class ProductionConfig {
		@Bean ExternalApi externalApi() { return () -> "production"; }
	}

	@Test
	void a01_overlay_winsOverConfigurationBean() throws Exception {
		@SuppressWarnings({
			"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
		})
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "test-overlay");

		var ms = Microservice.create()
			.configurations(ProductionConfig.class)
			.overridingBeanStore(overlay)
			.build();
		try {
			@SuppressWarnings({
				"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
			})
			var resolved = ms.getBeanStore().getBean(ExternalApi.class).orElseThrow();
			assertEquals("test-overlay", resolved.describe(),
				"Overlay installed via overridingBeanStore must win over @Bean factory contributions");
		} finally {
			ms.stop();
		}
	}

	@Test
	void a02_overlay_fallsThroughForUnsupportedTypes() throws Exception {
		var overlay = new TestBeanStore(); // empty
		var ms = Microservice.create()
			.configurations(ProductionConfig.class)
			.overridingBeanStore(overlay)
			.build();
		try {
			@SuppressWarnings({
				"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
			})
			var resolved = ms.getBeanStore().getBean(ExternalApi.class).orElseThrow();
			assertEquals("production", resolved.describe(),
				"Empty overlay must fall through to the @Bean-supplied production value");
		} finally {
			ms.stop();
		}
	}
	@SuppressWarnings({
		"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
	})
	@Test
	void a03_nullOverlay_isAccepted() throws Exception {
		var ms = Microservice.create()
			.configurations(ProductionConfig.class)
			.overridingBeanStore(null)
			.build();
		try {
			assertEquals("production", ms.getBeanStore().getBean(ExternalApi.class).orElseThrow().describe());
		} finally {
			ms.stop();
		}
	}
	@SuppressWarnings({
		"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
	})
	@Test
	void a04_overlay_composesWithExternalBeanStore() throws Exception {
		WritableBeanStore external = new BasicBeanStore();
		external.addBean(ExternalApi.class, () -> "from-external");

		@SuppressWarnings({
			"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
		})
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "from-overlay");

		var ms = Microservice.create()
			.beanStore(external)
			.overridingBeanStore(overlay)
			.build();
		try {
			// Overlay wins over the external store.
			assertEquals("from-overlay", ms.getBeanStore().getBean(ExternalApi.class).orElseThrow().describe());
			// And the external store is the regular parent: clearing the overlay slot would fall through to it.
			assertNotSame(external, ms.getBeanStore(),
				"When both an external store and an overlay are supplied, the microservice wraps the external store");
		} finally {
			ms.stop();
		}
	}
}
