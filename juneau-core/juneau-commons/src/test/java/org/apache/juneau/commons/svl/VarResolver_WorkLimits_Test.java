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
import org.apache.juneau.commons.svl.functions.*;
import org.junit.jupiter.api.*;

/**
 * Tests the work limits, regex deadline and non-recursive external variables.
 */
class VarResolver_WorkLimits_Test extends TestBase {

	private static final VarResolver R = VarResolver.create().defaultVars().defaultFunctions().build();

	@Test void a01_externalValuesAreNotReResolved() {
		System.setProperty("svl.test.wl.a01", "${S:user.dir}");
		try {
			assertEquals("${S:user.dir}", R.resolve("${S:svl.test.wl.a01}"));
		} finally {
			System.clearProperty("svl.test.wl.a01");
		}
	}

	/** Non-stable var returning a system property, so templates are not folded at compile time. */
	public static class PropVar extends SimpleVar {
		public PropVar() {
			super("PV");
		}

		@Override
		public String resolve(VarResolverSession s, String arg) {
			return System.getProperty(arg);
		}
	}

	private static final VarResolver RP = VarResolver.create().vars(PropVar.class).defaultFunctions().build();

	@Test void a02_substitutionCountIsCapped() {
		var sb = new StringBuilder();
		for (var i = 0; i < 10_001; i++)
			sb.append("${PV:user.dir}");
		var input = sb.toString();
		assertThrows(VarResolverException.class, () -> RP.resolve(input));
	}

	@Test void a03_outputSizeIsCapped() {
		System.setProperty("svl.test.wl.a03", "x".repeat(600_000));
		try {
			assertThrows(VarResolverException.class, () -> RP.resolve("${PV:svl.test.wl.a03}${PV:svl.test.wl.a03}"));
		} finally {
			System.clearProperty("svl.test.wl.a03");
		}
	}

	@Test void a04_budgetResetsBetweenResolves() {
		for (var i = 0; i < 3; i++)
			assertEquals("a", R.resolve("${S:svl.test.wl.none:a}"));
	}

	@Test void b01_repeatIsCapped() {
		assertThrows(RuntimeException.class, () -> R.resolve("#{repeat('abcdefghij', 1000000)}"));
	}

	@Test void b02_randStringIsCapped() {
		assertThrows(RuntimeException.class, () -> R.resolve("#{randString(100000000)}"));
	}

	@Test void c01_runawayRegexIsAborted() {
		var evil = "a".repeat(30);
		var match = new RegexFunctions.Match();
		var t = System.nanoTime();
		assertThrows(RuntimeException.class, () -> match.invoke(evil, "(a+)+\\1b"));
		assertTrue((System.nanoTime() - t) < 30_000_000_000L);
	}
}
