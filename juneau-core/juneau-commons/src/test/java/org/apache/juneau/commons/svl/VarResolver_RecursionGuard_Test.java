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
package org.apache.juneau.commons.svl;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

/**
 * Tests the recursion depth guard in {@link VarResolver}.
 */
class VarResolver_RecursionGuard_Test extends TestBase {

	public static class SelfVar extends SimpleVar {
		public SelfVar() {
			super("SELF");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return "${SELF:" + arg + "}";
		}
	}

	/** Test var that returns a system property and, like most vars, allows its output to be re-resolved. */
	public static class PropVar extends SimpleVar {
		public PropVar() {
			super("PV");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return System.getProperty(arg);
		}
	}

	@Test void a01_selfReferenceThrowsInsteadOfOverflow() {
		var r = VarResolver.create().vars(SelfVar.class).maxRecursionDepth(8).build();
		var e = assertThrows(VarResolverException.class, () -> r.resolve("${SELF:x}"));
		assertTrue(e.getMessage().contains("recursion depth"), e.getMessage());
		assertTrue(e.getMessage().startsWith("Maximum variable recursion depth"), e.getMessage());
		assertNull(e.getCause());
	}

	@Test void a02_normalNestingUnaffected() {
		var r = VarResolver.create().vars(PropVar.class).maxRecursionDepth(8).build();
		System.setProperty("svl.test.a02a", "${PV:svl.test.a02b}");
		System.setProperty("svl.test.a02b", "v");
		try {
			assertEquals("v", r.resolve("${PV:svl.test.a02a}"));
		} finally {
			System.clearProperty("svl.test.a02a");
			System.clearProperty("svl.test.a02b");
		}
	}

	@Test void a03_defaultIs32() {
		assertEquals(32, VarResolver.DEFAULT_MAX_RECURSION_DEPTH);
	}

	@Test void a04_builderRejectsNonPositive() {
		var b = VarResolver.create();
		assertThrows(IllegalArgumentException.class, () -> b.maxRecursionDepth(0));
		assertThrows(IllegalArgumentException.class, () -> b.maxRecursionDepth(-1));
	}

	@Test void a05_exactBoundary() {
		// a -> ${PV:b} -> ${PV:c} -> v : the output of a and b are each re-resolved, so depth 2.
		System.setProperty("svl.test.a05a", "${PV:svl.test.a05b}");
		System.setProperty("svl.test.a05b", "${PV:svl.test.a05c}");
		System.setProperty("svl.test.a05c", "v");
		try {
			var ok = VarResolver.create().vars(PropVar.class).maxRecursionDepth(2).build();
			assertEquals("v", ok.resolve("${PV:svl.test.a05a}"));
			var tooSmall = VarResolver.create().vars(PropVar.class).maxRecursionDepth(1).build();
			assertThrows(VarResolverException.class, () -> tooSmall.resolve("${PV:svl.test.a05a}"));
		} finally {
			System.clearProperty("svl.test.a05a");
			System.clearProperty("svl.test.a05b");
			System.clearProperty("svl.test.a05c");
		}
	}

	@Test void a06_sessionReusableAfterGuardThrows() {
		var r = VarResolver.create().vars(SelfVar.class).defaultVars().maxRecursionDepth(4).build();
		var session = r.createSession();
		assertThrows(VarResolverException.class, () -> session.resolve("${SELF:x}"));
		assertThrows(VarResolverException.class, () -> session.resolve("${SELF:x}"));
		assertEquals("plain", session.resolve("plain"));
	}

	@Test void a07_copyKeepsMaxRecursionDepth() {
		var r = VarResolver.create().defaultVars().maxRecursionDepth(8).build();
		assertEquals(8, r.copy().build().maxRecursionDepth);
	}
}
