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
package org.apache.juneau.http.classic.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Immutability-paradigm tests for the {@link BasicHttpEntity} hierarchy, using {@link ByteArrayEntity} as a
 * representative leaf.
 *
 * <p>
 * Mirrors the Phase 2 response tests ({@code BasicHttpResponse_Test} b01–b07): reflective mutator guard, idempotency,
 * content equality, snapshot independence, and snapshot-construction regression.  Because entities hold no list-backed
 * sub-bean whose {@code unmodifiable} flag folds into equality, the full D3 assertion {@code bean.equals(bean.unmodifiable())}
 * holds here and is asserted directly (unlike the response side, which defers it to Phase 5).
 */
class BasicHttpEntity_Test extends TestBase {

	private static final byte[] BYTES = "hello world".getBytes();

	@Test void b01_unmodifiable_returnsUnmodifiableLeafType() {
		var x = new ByteArrayEntity().setContent(BYTES);
		assertFalse(x.isUnmodifiable());
		var u = x.unmodifiable();
		assertTrue(u.isUnmodifiable());
		assertInstanceOf(ByteArrayEntity.Unmodifiable.class, u);
		assertInstanceOf(ByteArrayEntity.class, u, "Snapshot must preserve the concrete leaf type.");
	}

	@Test void b02_reflectiveGuard_everyPublicMutatorThrows() throws Exception {
		var u = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain").unmodifiable();
		var mutators = 0;
		for (var m : ByteArrayEntity.class.getMethods()) {
			var n = m.getName();
			if (Modifier.isStatic(m.getModifiers()) || ! (n.startsWith("set") || n.startsWith("add") || n.startsWith("remove")))
				continue;
			mutators++;
			var types = m.getParameterTypes();
			var args = new Object[types.length];
			for (var i = 0; i < types.length; i++) {
				var t = types[i];
				if (t == int.class)
					args[i] = Integer.valueOf(0);
				else if (t == long.class)
					args[i] = Long.valueOf(0);
				else if (t == boolean.class)
					args[i] = Boolean.FALSE;
				else
					args[i] = null;
			}
			var ex = assertThrows(InvocationTargetException.class, () -> m.invoke(u, args), m::getName);
			assertInstanceOf(UnsupportedOperationException.class, ex.getCause(), m::getName);
		}
		assertTrue(mutators >= 10, "Expected at least 10 public mutators, found " + mutators);
	}

	@Test void b03_idempotency_alreadyUnmodifiableReturnsThis() {
		var u = new ByteArrayEntity().setContent(BYTES).unmodifiable();
		assertSame(u, u.unmodifiable());
	}

	@Test void b04_contentEquality_beanEqualsSnapshot() {
		// D3 content equality — entities have no list-backed sub-bean whose 'unmodifiable' flag folds into equality,
		// so the full bean.equals(bean.unmodifiable()) assertion holds (design spec D3 / Phase 4).
		var x = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain");
		var u = x.unmodifiable();
		assertEquals(x, u);
		assertEquals(x.hashCode(), u.hashCode());
		// Two independently-frozen snapshots of equal content compare equal.
		var u1 = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain").unmodifiable();
		var u2 = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain").unmodifiable();
		assertEquals(u1, u2);
		assertEquals(u1.hashCode(), u2.hashCode());
	}

	@Test void b05_snapshotIndependence_mutatingOriginalDoesNotAffectSnapshot() throws Exception {
		var x = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain");
		var u = x.unmodifiable();
		x.setContent("changed".getBytes()).setContentType("text/html").setChunked(true);
		assertArrayEquals(BYTES, u.asBytes());
		assertEquals("text/plain", u.getContentType().getValue());
		assertFalse(u.isChunked());
	}

	@Test void b06_snapshotConstruction_freezeDoesNotThrow() {
		// Regression: constructing the snapshot must not route the freeze through the throwing modify().
		var x = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain").setChunked(true);
		assertDoesNotThrow(x::unmodifiable);
	}

	@Test void b07_emptyConstant_isUnmodifiable() {
		assertTrue(BasicHttpEntity.EMPTY.isUnmodifiable());
	}

	@Test void b08_crossLeafInequality_equalBaseContentDifferentLeaf() {
		// Two unrelated leaves with identical base content must NOT be equal (leaf-type gate), in both directions.
		var s = new StringEntity().setContent("x").setContentType("text/plain");
		var b = new ByteArrayEntity().setContent("x").setContentType("text/plain");
		assertNotEquals(s, b);
		assertNotEquals(b, s);
	}

	@Test void b09_crossLeafInequality_holdsAcrossSnapshots() {
		// The leaf-type gate must also hold for the frozen snapshots (Unmodifiable IS-A its leaf).
		var s = new StringEntity().setContent("x").unmodifiable();
		var b = new ByteArrayEntity().setContent("x").unmodifiable();
		assertNotEquals(s, b);
		assertNotEquals(b, s);
	}

	@Test void b10_d3_holdsBothDirections_representativeLeaves() {
		// D3: bean.equals(bean.unmodifiable()) AND the reverse, for a representative sampling of leaves.
		var se = new StringEntity().setContent("x").setContentType("text/plain");
		assertEquals(se, se.unmodifiable());
		assertEquals(se.unmodifiable(), se);
		assertEquals(se.hashCode(), se.unmodifiable().hashCode());

		var be = new ByteArrayEntity().setContent(BYTES).setContentType("text/plain");
		assertEquals(be, be.unmodifiable());
		assertEquals(be.unmodifiable(), be);
		assertEquals(be.hashCode(), be.unmodifiable().hashCode());
	}

	@Test void b11_sameLeafEquality_differentInstances() {
		// Same leaf type + equal base content compare equal (sanity that the gate does not over-restrict).
		var a = new StringEntity().setContent("x").setContentType("text/plain");
		var b = new StringEntity().setContent("x").setContentType("text/plain");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}
}
