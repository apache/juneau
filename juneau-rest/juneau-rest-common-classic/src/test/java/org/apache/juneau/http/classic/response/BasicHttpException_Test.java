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
 * Immutability-paradigm tests for the {@link BasicHttpException} hierarchy, using {@link NotFound} as a representative
 * leaf.
 *
 * <p>
 * Mirrors the Phase 2 pilot ({@code BasicHttpResponse_Test} b01–b07): reflective mutator guard, idempotency, content
 * equality, snapshot independence, and snapshot-construction regression.  This is the {@link Throwable} branch — the
 * root is non-generic (JLS &sect;8.1.2), the funnel returns the base type, and each leaf retains its covariant setter
 * overrides.
 */
class BasicHttpException_Test extends TestBase {

	@Test void b01_unmodifiable_returnsUnmodifiableLeafType() {
		var x = new NotFound();
		assertFalse(x.isUnmodifiable());
		var u = x.unmodifiable();
		assertTrue(u.isUnmodifiable());
		assertInstanceOf(NotFound.Unmodifiable.class, u);
		assertInstanceOf(NotFound.class, u, "Snapshot must preserve the concrete leaf type.");
	}

	@Test void b02_reflectiveGuard_everyPublicMutatorThrows() throws Exception {
		var u = new NotFound().setContent("body").setHeader2("X-Test", "v").unmodifiable();
		var mutators = 0;
		for (var m : NotFound.class.getMethods()) {
			var n = m.getName();
			// Only funnel-routed mutators declared in our hierarchy — skip JDK-declared set/add/remove such as
			// Throwable.setStackTrace(...) and Throwable.addSuppressed(...), which are not routed through modify().
			if (Modifier.isStatic(m.getModifiers()) || m.getDeclaringClass().getName().startsWith("java.") || ! (n.startsWith("set") || n.startsWith("add") || n.startsWith("remove")))
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
		var u = new NotFound().unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b04_contentEquality_equalContentComparesEqual() {
		// D3 content equality — satisfiable slices for Phase 3.
		// Two independently-frozen snapshots of equal content compare equal.
		var u1 = new NotFound().setHeader2("X-Test", "v").unmodifiable();
		var u2 = new NotFound().setHeader2("X-Test", "v").unmodifiable();
		assertEquals(u1, u2);
		assertEquals(u1.hashCode(), u2.hashCode());
		// Two modifiable beans of equal content compare equal.
		var m1 = new NotFound().setHeader2("X-Test", "v");
		var m2 = new NotFound().setHeader2("X-Test", "v");
		assertEquals(m1, m2);
		assertEquals(m1.hashCode(), m2.hashCode());
		// D3 cross-boundary equality (Phase 5): now that HeaderList equality is content-only, a modifiable bean
		// compares equal to its own frozen snapshot (over statusLine + headers + getMessage(), excluding the
		// value-less content wrapper).
		assertEquals(m1, m1.unmodifiable());
		assertEquals(m1.hashCode(), m1.unmodifiable().hashCode());
	}

	@Test void b05_snapshotIndependence_mutatingOriginalDoesNotAffectSnapshot() {
		var x = new NotFound().setHeader2("X-Test", "before");
		var u = x.unmodifiable();
		x.setHeader2("X-Test", "after").setStatusCode2(201);
		assertEquals("before", u.getFirstHeader("X-Test").getValue());
		assertEquals(404, u.getStatusLine().getStatusCode());
	}

	@Test void b06_snapshotConstruction_freezeDoesNotThrow() {
		// Regression: constructing the snapshot must not route the freeze through the throwing modify().
		var x = new NotFound().setContent("body").setHeader2("X-Test", "v").setReasonPhrase2("Custom");
		assertDoesNotThrow(x::unmodifiable);
	}

	@Test void b07_instanceConstant_isUnmodifiableLeaf() {
		assertTrue(NotFound.INSTANCE.isUnmodifiable());
		assertInstanceOf(NotFound.class, NotFound.INSTANCE);
	}
}
