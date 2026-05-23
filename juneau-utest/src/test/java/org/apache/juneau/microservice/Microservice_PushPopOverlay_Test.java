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
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.junit5.*;
import org.junit.jupiter.api.*;

/**
 * Validates Mode OVERLAY (existing-instance push/pop) end-to-end against a long-lived {@link Microservice}.
 *
 * <p>
 * Boots a single microservice for the whole test class, then exercises three consecutive tests against it to prove:
 * <ul>
 * 	<li>An overlay pushed via {@link WritableBeanStore#pushOverlay} shadows the {@code @Bean} factory contribution.
 * 	<li>Popping the overlay restores the original production bean &mdash; no microservice rebuild required.
 * 	<li>A different overlay pushed on a subsequent test only sees its own bean, not the previous test's overlay.
 * </ul>
 *
 * <p>
 * Together these assertions cover Mode OVERLAY's central promise: an existing SUT can host per-test overlays
 * without being torn down and re-stood-up between tests.  See TODO-35 Phase 6.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
class Microservice_PushPopOverlay_Test extends TestBase {

	interface ExternalApi {
		String describe();
	}

	@Configuration
	static class ProductionConfig {
		@Bean ExternalApi externalApi() { return () -> "production"; }
	}

	private Microservice microservice;

	@BeforeAll
	void bootMicroserviceOnce() throws Exception {
		microservice = Microservice.create()
			.configurations(ProductionConfig.class)
			.build();
	}

	@AfterAll
	void stopMicroservice() throws Exception {
		if (microservice != null)
			microservice.stop();
	}

	@Test
	void a01_pushedOverlay_shadowsProductionBean() {
		var beanStore = microservice.getBeanStore();
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "overlay-a01");

		var snapshot = beanStore.pushOverlay(overlay);
		try {
			assertEquals("overlay-a01", beanStore.getBean(ExternalApi.class).orElseThrow().describe(),
				"Pushed overlay must shadow the @Bean-supplied production value during the test");
		} finally {
			beanStore.popOverlay(snapshot);
		}
	}

	@Test
	void a02_afterPop_productionBeanIsRestored_noRebuild() {
		var beanStore = microservice.getBeanStore();
		assertEquals("production", beanStore.getBean(ExternalApi.class).orElseThrow().describe(),
			"After the previous test popped its overlay, the same microservice should resolve the production bean");
	}

	@Test
	void a03_freshOverlay_doesNotSeePreviousTestsOverlay() {
		var beanStore = microservice.getBeanStore();
		var overlay = new TestBeanStore().override(ExternalApi.class, () -> "overlay-a03");

		var snapshot = beanStore.pushOverlay(overlay);
		try {
			assertEquals("overlay-a03", beanStore.getBean(ExternalApi.class).orElseThrow().describe(),
				"A fresh overlay must shadow the production bean with its own value, not the previous test's value");
		} finally {
			beanStore.popOverlay(snapshot);
		}
	}
}
