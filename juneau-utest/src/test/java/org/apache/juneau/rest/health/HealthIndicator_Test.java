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
package org.apache.juneau.rest.health;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class HealthIndicator_Test {

	@Test void a01_builderUpContainsDetails() {
		var h = Health.up("db").detail("latencyMs", 12).build();
		assertEquals("db", h.getName());
		assertEquals(HealthStatus.UP, h.getStatus());
		assertEquals(12, h.getDetails().get("latencyMs"));
	}

	@Test void a02_builderDownCarriesError() {
		var ex = new IllegalStateException("boom");
		var h = Health.down("cache", ex).build();
		assertEquals(HealthStatus.DOWN, h.getStatus());
		assertSame(ex, h.getError());
	}

	@Test void a03_defaultProbesAreLiveAndReady() {
		HealthIndicator i = () -> Health.up("ok").build();
		assertTrue(i.probes().contains(HealthProbe.LIVE));
		assertTrue(i.probes().contains(HealthProbe.READY));
		assertFalse(i.probes().contains(HealthProbe.STARTUP));
	}
}
