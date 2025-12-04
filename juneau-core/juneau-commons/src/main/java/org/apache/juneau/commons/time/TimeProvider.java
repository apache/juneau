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

import java.time.*;

/**
 * Provides access to system time and timezone information.
 *
 * <p>
 * This class abstracts time-related operations to allow for easier testing and customization.
 * By default, it delegates to the system's time and timezone, but can be extended or replaced
 * for testing purposes (e.g., using a {@link org.apache.juneau.utest.utils.FakeTimeProvider}).
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Use the default instance</jc>
 * 	ZoneId <jv>zone</jv> = TimeProvider.<jsf>INSTANCE</jsf>.<jsm>getSystemDefaultZoneId</jsm>();
 * 	ZonedDateTime <jv>now</jv> = TimeProvider.<jsf>INSTANCE</jsf>.<jsm>now</jsm>();
 *
 * 	<jc>// Or create a custom implementation for testing</jc>
 * 	TimeProvider <jv>testProvider</jv> = <jk>new</jk> FakeTimeProvider();
 * 	ZonedDateTime <jv>fixedTime</jv> = <jv>testProvider</jv>.<jsm>now</jsm>();
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jm'>{@link org.apache.juneau.utest.utils.FakeTimeProvider}
 * 	<li class='jm'>{@link GranularZonedDateTime}
 * </ul>
 */
public class TimeProvider {

	/**
	 * The default instance that uses the system's time and timezone.
	 */
	public static final TimeProvider INSTANCE = new TimeProvider();

	/**
	 * Returns the system default timezone.
	 *
	 * @return The system default {@link ZoneId}.
	 */
	public ZoneId getSystemDefaultZoneId() {
		return ZoneId.systemDefault();
	}

	/**
	 * Returns the current date and time in the system default timezone.
	 *
	 * @return The current {@link ZonedDateTime} in the system default timezone.
	 */
	public ZonedDateTime now() {
		return ZonedDateTime.now();
	}

	/**
	 * Returns the current date and time in the specified timezone.
	 *
	 * @param zoneId The timezone to use. Must not be <jk>null</jk>.
	 * @return The current {@link ZonedDateTime} in the specified timezone.
	 */
	public ZonedDateTime now(ZoneId zoneId) {
		return ZonedDateTime.now(zoneId);
	}
}
