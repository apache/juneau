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
package org.apache.juneau.rest.server.health;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage tests for {@link HealthProbeSettings} — factory, builder, defaults, and explicit values.
 *
 * @since 10.0.0
 */
class HealthProbeSettings_Test extends TestBase {

	// -----------------------------------------------------------------------------------------
	// a — create() / build() / defaults
	// -----------------------------------------------------------------------------------------

	@Test void a01_create_returnsNonNullBuilder() {
		assertNotNull(HealthProbeSettings.create(), "create() must return a non-null Builder");
	}

	@Test void a02_build_returnsNonNullSettings() {
		var settings = HealthProbeSettings.create().build();
		assertNotNull(settings, "build() must return a non-null HealthProbeSettings");
	}

	@Test void a03_defaultTimeout_isOneSecond() {
		var settings = HealthProbeSettings.create().build();
		assertEquals(Duration.ofSeconds(1), settings.getTimeout(),
			"Default timeout must be 1 second");
	}

	// -----------------------------------------------------------------------------------------
	// b — timeout(Duration) setter
	// -----------------------------------------------------------------------------------------

	@Test void b01_timeout_setsCustomValue() {
		var custom = Duration.ofMillis(500);
		var settings = HealthProbeSettings.create().timeout(custom).build();
		assertEquals(custom, settings.getTimeout(),
			"timeout(Duration) must persist the supplied value");
	}

	@Test void b02_timeout_largeValue() {
		var large = Duration.ofMinutes(5);
		var settings = HealthProbeSettings.create().timeout(large).build();
		assertEquals(large, settings.getTimeout());
	}

	@Test void b03_timeout_nullResetsToDefault() {
		// Passing null to timeout(Duration) must silently reset to the 1-second default
		var settings = HealthProbeSettings.create()
			.timeout(Duration.ofSeconds(30))  // set non-default first
			.timeout(null)                    // then reset via null
			.build();
		assertEquals(Duration.ofSeconds(1), settings.getTimeout(),
			"timeout(null) must reset to the default 1-second value");
	}

	@Test void b04_timeout_zeroDuration() {
		var zero = Duration.ZERO;
		var settings = HealthProbeSettings.create().timeout(zero).build();
		assertEquals(zero, settings.getTimeout(),
			"timeout(Duration.ZERO) must be stored as-is");
	}

	// -----------------------------------------------------------------------------------------
	// c — builder is reusable / each build() is independent
	// -----------------------------------------------------------------------------------------

	@Test void c01_builderIsReusable_eachBuildIndependent() {
		var builder = HealthProbeSettings.create().timeout(Duration.ofSeconds(2));
		var s1 = builder.build();
		var s2 = builder.build();
		assertNotSame(s1, s2, "Each build() call must produce a distinct HealthProbeSettings instance");
		assertEquals(s1.getTimeout(), s2.getTimeout());
	}

	@Test void c02_subsequentTimeoutOverrideWins() {
		var settings = HealthProbeSettings.create()
			.timeout(Duration.ofSeconds(3))
			.timeout(Duration.ofSeconds(7))
			.build();
		assertEquals(Duration.ofSeconds(7), settings.getTimeout(),
			"The last call to timeout() must win");
	}
}
