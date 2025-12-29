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
package org.apache.juneau.utest.utils;

import java.time.*;

import org.apache.juneau.commons.time.TimeProvider;

/**
 * A fake time provider for testing that always returns UTC timezone and a fixed time.
 */
public class FakeTimeProvider extends TimeProvider {

	private static final ZoneId UTC = ZoneId.of("UTC");
	private static final ZonedDateTime FIXED_TIME = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, UTC);

	@Override
	public ZoneId getSystemDefaultZoneId() {
		return UTC;
	}

	@Override
	public ZonedDateTime now() {
		return FIXED_TIME;
	}

	@Override
	public ZonedDateTime now(ZoneId zoneId) {
		return FIXED_TIME.withZoneSameInstant(zoneId);
	}
}

