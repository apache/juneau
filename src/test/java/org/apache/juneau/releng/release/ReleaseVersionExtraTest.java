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

class ReleaseVersionExtraTest {

	@Test
	void a01_partsAndAccessors() {
		var v = ReleaseVersion.of("9.2.1-RC3");
		assertEquals("9.2.1", v.version());
		assertEquals("RC3", v.rc());
		assertTrue(v.isPrerelease());
		assertEquals(9, v.major());
		assertEquals(2, v.minor());
		assertEquals(1, v.maintenance());
		assertEquals(0, v.part(7));
	}

	@Test
	void a02_betaIsPrereleaseWithoutRc() {
		var v = ReleaseVersion.of("9.0-B1");
		assertTrue(v.isPrerelease());
		assertNull(v.rc());
		assertEquals(0, v.maintenance());
	}

	@Test
	void a03_ofTagStripsPrefixOnlyWhenPresent() {
		assertEquals("9.1.0", ReleaseVersion.ofTag("juneau-9.1.0").version());
		assertEquals("9.1.0", ReleaseVersion.ofTag("9.1.0").version());
	}

	@Test
	void b01_equalsAndHashCodeTreatTrailingZerosAsEqual() {
		var a = ReleaseVersion.of("9.2");
		var b = ReleaseVersion.of("9.2.0");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a, a);
		assertNotEquals(a, ReleaseVersion.of("9.2.1"));
		assertNotEquals(a, "9.2");
		assertNotEquals(a, null);
		assertTrue(ReleaseVersion.of("9.3").compareTo(a) > 0);
		assertTrue(ReleaseVersion.of("9.2.0.1").compareTo(a) > 0);
		assertTrue(a.compareTo(ReleaseVersion.of("9.2.0.1")) < 0);
	}

	@Test
	void b02_allZeroVersionsHashEqually() {
		assertEquals(ReleaseVersion.of("0").hashCode(), ReleaseVersion.of("0.0.0").hashCode());
	}

	@Test
	void c01_highestReleasedBelowSkipsPrereleasesAndCeiling() {
		var tags = List.of("juneau-9.0.0", "juneau-9.1.0", "juneau-9.1.0-RC1", "juneau-9.2.0", "juneau-8.9.0");
		assertEquals("9.1.0", ReleaseVersion.highestReleasedBelow(tags, "9.2.0").version());
		assertEquals("9.2.0", ReleaseVersion.highestReleasedBelow(tags, "10.0.0").version());
		assertNull(ReleaseVersion.highestReleasedBelow(tags, "8.0.0"));
		assertNull(ReleaseVersion.highestReleasedBelow(List.of(), "1.0"));
	}
}
