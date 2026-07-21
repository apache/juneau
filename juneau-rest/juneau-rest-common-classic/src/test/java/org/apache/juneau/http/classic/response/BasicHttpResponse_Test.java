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
package org.apache.juneau.http.classic.response;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Immutability-paradigm tests for the {@link BasicHttpResponse} hierarchy, using {@link Ok} as a representative leaf.
 *
 * <p>
 * Mirrors the Phase 1 pilot ({@code BasicStatusLine_Test} b01–b06): reflective mutator guard, idempotency, content
 * equality, snapshot independence, and snapshot-construction regression.
 */
class BasicHttpResponse_Test extends TestBase {

	@Test void b01_unmodifiable_returnsUnmodifiableLeafType() {
		var ok = new Ok();
		assertFalse(ok.isUnmodifiable());
		var u = ok.unmodifiable();
		assertTrue(u.isUnmodifiable());
		assertInstanceOf(Ok.Unmodifiable.class, u);
		assertInstanceOf(Ok.class, u, "Snapshot must preserve the concrete leaf type.");
	}

	@Test void b02_reflectiveGuard_everyPublicMutatorThrows() throws Exception {
		var u = new Ok().setContent("body").setHeader2("X-Test", "v").unmodifiable();
		var mutators = 0;
		for (var m : Ok.class.getMethods()) {
			var n = m.getName();
			if (Modifier.isStatic(m.getModifiers()) || ! (n.startsWith("set") || n.startsWith("add") || n.startsWith("remove")))
				continue;
			mutators++;
			var types = m.getParameterTypes();
			var args = new Object[types.length];
			for (var i = 0; i < types.length; i++)
				args[i] = types[i] == int.class ? Integer.valueOf(0) : null;
			var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(u, args), m::getName);
			assertInstanceOf(UnsupportedOperationException.class, ex.getCause(), m::getName);
		}
		assertTrue(mutators >= 10, "Expected at least 10 public mutators, found " + mutators);
	}

	@Test void b03_idempotency_alreadyUnmodifiableReturnsThis() {
		var u = new Ok().unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b04_contentEquality_equalContentComparesEqual() {
		// D3 content equality — satisfiable slices for Phase 2.
		// Two independently-frozen snapshots of equal content compare equal.
		var u1 = new Ok().setHeader2("X-Test", "v").unmodifiable();
		var u2 = new Ok().setHeader2("X-Test", "v").unmodifiable();
		assertEquals(u1, u2);
		assertEquals(u1.hashCode(), u2.hashCode());
		// Two modifiable beans of equal content compare equal.
		var m1 = new Ok().setHeader2("X-Test", "v");
		var m2 = new Ok().setHeader2("X-Test", "v");
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
		// D3 cross-boundary equality (Phase 5): now that HeaderList equality is content-only, a modifiable bean
		// compares equal to its own frozen snapshot.
		assertEquals(m1, m1.unmodifiable());
		assertEquals(m1.hashCode(), m1.unmodifiable().hashCode());
	}

	@Test void b05_snapshotIndependence_mutatingOriginalDoesNotAffectSnapshot() {
		var ok = new Ok().setHeader2("X-Test", "before");
		var u = ok.unmodifiable();
		ok.setHeader2("X-Test", "after").setStatusCode2(201);
		assertEquals("before", u.getFirstHeader("X-Test").getValue());
		assertEquals(200, u.getStatusLine().getStatusCode());
	}

	@Test void b06_snapshotConstruction_freezeDoesNotThrow() {
		// Regression: constructing the snapshot must not route the freeze through the throwing modify().
		var ok = new Ok().setContent("body").setHeader2("X-Test", "v").setReasonPhrase2("Custom");
		assertDoesNotThrow(ok::unmodifiable);
	}

	@Test void b07_instanceConstant_isUnmodifiableLeaf() {
		assertTrue(Ok.INSTANCE.isUnmodifiable());
		assertInstanceOf(Ok.class, Ok.INSTANCE);
	}
}
