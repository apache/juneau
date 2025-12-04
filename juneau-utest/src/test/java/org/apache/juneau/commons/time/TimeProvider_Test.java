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
package org.apache.juneau.commons.time;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.temporal.ChronoUnit;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link TimeProvider}.
 */
class TimeProvider_Test extends TestBase {

	//====================================================================================================
	// INSTANCE field tests
	//====================================================================================================

	@Test
	void a01_instanceNotNull() {
		assertNotNull(TimeProvider.INSTANCE);
	}

	@Test
	void a02_instanceIsTimeProvider() {
		assertTrue(TimeProvider.INSTANCE instanceof TimeProvider);
	}

	//====================================================================================================
	// getSystemDefaultZoneId() tests
	//====================================================================================================

	@Test
	void b01_getSystemDefaultZoneId() {
		ZoneId expected = ZoneId.systemDefault();
		ZoneId actual = TimeProvider.INSTANCE.getSystemDefaultZoneId();
		assertEquals(expected, actual);
	}

	@Test
	void b02_getSystemDefaultZoneId_notNull() {
		ZoneId zoneId = TimeProvider.INSTANCE.getSystemDefaultZoneId();
		assertNotNull(zoneId);
	}

	//====================================================================================================
	// now() tests
	//====================================================================================================

	@Test
	void c01_now() {
		ZonedDateTime now = TimeProvider.INSTANCE.now();
		assertNotNull(now);
		assertEquals(ZoneId.systemDefault(), now.getZone());
	}

	@Test
	void c02_now_isRecent() {
		ZonedDateTime before = ZonedDateTime.now();
		ZonedDateTime now = TimeProvider.INSTANCE.now();
		ZonedDateTime after = ZonedDateTime.now();

		// The time should be between before and after (with some tolerance)
		assertTrue(now.isAfter(before.minusSeconds(1)) || now.isEqual(before.minusSeconds(1)));
		assertTrue(now.isBefore(after.plusSeconds(1)) || now.isEqual(after.plusSeconds(1)));
	}

	@Test
	void c03_now_usesSystemDefaultZone() {
		ZonedDateTime now = TimeProvider.INSTANCE.now();
		assertEquals(ZoneId.systemDefault(), now.getZone());
	}

	//====================================================================================================
	// now(ZoneId) tests
	//====================================================================================================

	@Test
	void d01_nowWithZoneId() {
		ZoneId utc = ZoneId.of("UTC");
		ZonedDateTime now = TimeProvider.INSTANCE.now(utc);
		assertNotNull(now);
		assertEquals(utc, now.getZone());
	}

	@Test
	void d02_nowWithZoneId_usesSpecifiedZone() {
		ZoneId newYork = ZoneId.of("America/New_York");
		ZonedDateTime now = TimeProvider.INSTANCE.now(newYork);
		assertEquals(newYork, now.getZone());
	}

	@Test
	void d03_nowWithZoneId_isRecent() {
		ZoneId utc = ZoneId.of("UTC");
		ZonedDateTime before = ZonedDateTime.now(utc);
		ZonedDateTime now = TimeProvider.INSTANCE.now(utc);
		ZonedDateTime after = ZonedDateTime.now(utc);

		// The time should be between before and after (with some tolerance)
		assertTrue(now.isAfter(before.minusSeconds(1)) || now.isEqual(before.minusSeconds(1)));
		assertTrue(now.isBefore(after.plusSeconds(1)) || now.isEqual(after.plusSeconds(1)));
	}

	@Test
	void d04_nowWithZoneId_differentZones() {
		ZoneId utc = ZoneId.of("UTC");
		ZoneId newYork = ZoneId.of("America/New_York");

		ZonedDateTime utcTime = TimeProvider.INSTANCE.now(utc);
		ZonedDateTime nyTime = TimeProvider.INSTANCE.now(newYork);

		// Both should represent the same instant, just in different zones
		Instant utcInstant = utcTime.toInstant();
		Instant nyInstant = nyTime.toInstant();

		// They should be very close (within 1 second)
		long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(utcInstant, nyInstant));
		assertTrue(diffSeconds < 2, "Times should be within 1 second of each other");
	}

	@Test
	void d05_nowWithZoneId_nullThrowsException() {
		assertThrows(NullPointerException.class, () -> {
			TimeProvider.INSTANCE.now(null);
		});
	}

	//====================================================================================================
	// Custom instance tests
	//====================================================================================================

	@Test
	void e01_customInstance() {
		TimeProvider provider = new TimeProvider();
		assertNotNull(provider);
		assertNotNull(provider.getSystemDefaultZoneId());
		assertNotNull(provider.now());
	}

	@Test
	void e02_customInstance_behaviorMatchesInstance() {
		TimeProvider custom = new TimeProvider();
		ZoneId customZone = custom.getSystemDefaultZoneId();
		ZoneId instanceZone = TimeProvider.INSTANCE.getSystemDefaultZoneId();
		assertEquals(instanceZone, customZone);
	}
}

