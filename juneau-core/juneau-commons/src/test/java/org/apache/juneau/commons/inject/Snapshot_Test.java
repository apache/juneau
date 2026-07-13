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
package org.apache.juneau.commons.inject;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Snapshot}.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class Snapshot_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a — identity equality
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void a01_distinctPushes_produceDistinctSnapshots_evenForSameOverlay() {
		var store = new BasicBeanStore();
		var overlay = new BasicBeanStore();

		var s1 = store.pushOverlay(overlay);
		store.popOverlay(s1);
		var s2 = store.pushOverlay(overlay);

		assertNotSame(s1, s2, "Each push must produce a fresh Snapshot instance");
		assertNotEquals(s1, s2, "Snapshots use identity equality; distinct pushes are not equal");
	}

	@Test
	void a02_sameSnapshot_isEqualToItself() {
		var store = new BasicBeanStore();
		var s = store.pushOverlay(new BasicBeanStore());

		assertEquals(s, s);
		assertEquals(s.hashCode(), s.hashCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b — external construction is impossible
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void b01_snapshot_hasNoPublicConstructor() {
		var ctors = Snapshot.class.getDeclaredConstructors();
		for (var c : ctors)
			assertFalse(Modifier.isPublic(c.getModifiers()),
				"Snapshot should not expose a public constructor: " + c);
	}

	@Test
	void b02_snapshot_class_isFinal() {
		assertTrue(Modifier.isFinal(Snapshot.class.getModifiers()),
			"Snapshot should be final to prevent subclasses bypassing the contract");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c — toString is non-trivial (used in error messages)
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	void c01_toString_includesIdentityHashes() {
		var store = new BasicBeanStore();
		var s = store.pushOverlay(new BasicBeanStore());

		var str = s.toString();
		assertTrue(str.startsWith("Snapshot["), "Should start with Snapshot[: " + str);
		assertTrue(str.contains("owner="), "Should include owner identity: " + str);
		assertTrue(str.contains("frame="), "Should include frame identity: " + str);
	}
}
