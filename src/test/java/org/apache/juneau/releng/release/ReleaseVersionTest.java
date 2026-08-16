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

package org.apache.juneau.releng.release;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseVersionTest {

	@Test
	void parsesPlainVersionFromTag() {
		var v = ReleaseVersion.ofTag("juneau-9.2.0");
		assertEquals("9.2.0", v.version());
		assertFalse(v.isPrerelease());
		assertNull(v.rc());
	}

	@Test
	void parsesRcFromTag() {
		var v = ReleaseVersion.ofTag("juneau-9.2.0-RC3");
		assertEquals("9.2.0", v.version());
		assertTrue(v.isPrerelease());
		assertEquals("RC3", v.rc());
	}

	@Test
	void parsesBetaAsPrerelease() {
		var v = ReleaseVersion.ofTag("juneau-9.0-B1");
		assertEquals("9.0", v.version());
		assertTrue(v.isPrerelease());
	}

	@Test
	void ordersBySemver() {
		assertTrue(ReleaseVersion.ofTag("juneau-9.2.0").compareTo(ReleaseVersion.ofTag("juneau-9.1.0")) > 0);
		assertTrue(ReleaseVersion.ofTag("juneau-10.0.0").compareTo(ReleaseVersion.ofTag("juneau-9.2.0")) > 0);
		assertTrue(ReleaseVersion.ofTag("juneau-9.0.1").compareTo(ReleaseVersion.ofTag("juneau-9.0.0")) > 0);
	}

	@Test
	void highestReleasedExcludesPrereleases() {
		var tags = List.of("juneau-9.2.0", "juneau-9.2.0-RC3", "juneau-9.1.0", "juneau-9.0.1", "juneau-8.2.0-RC1");
		assertEquals("9.2.0", ReleaseVersion.highestReleasedBelow(tags, "10.0.0").version());
	}

	@Test
	void highestReleasedBelowCeiling() {
		var tags = List.of("juneau-9.2.0", "juneau-9.1.0", "juneau-9.0.1");
		assertEquals("9.1.0", ReleaseVersion.highestReleasedBelow(tags, "9.2.0").version());
	}

	@Test
	void exposesNumericParts() {
		var v = ReleaseVersion.of("9.2.1");
		assertEquals(9, v.major());
		assertEquals(2, v.minor());
		assertEquals(1, v.maintenance());
	}

	@Test
	void maintenanceDefaultsToZeroWhenAbsent() {
		var v = ReleaseVersion.of("10.0");
		assertEquals(10, v.major());
		assertEquals(0, v.minor());
		assertEquals(0, v.maintenance());
	}
}
