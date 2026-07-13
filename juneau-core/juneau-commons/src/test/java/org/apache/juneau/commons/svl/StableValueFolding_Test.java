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

import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.vars.*;
import org.junit.jupiter.api.*;

/**
 * Tests compile-time stable-value folding for opt-in source {@link Var}s.
 *
 * <p>
 * Verifies:
 * <ul>
 * 	<li>Stable {@link Var}s ({@link SystemPropertiesVar} / {@link EnvVariablesVar} /
 * 		{@link ManifestFileVar} / {@link ArgsVar}) fold their resolved value into a
 * 		{@link LiteralSegment} at compile time.
 * 	<li>Non-stable {@link Var}s do not fold; their dispatch happens at resolve time.
 * 	<li>Folding is skipped when the body contains nested live markers (so the resolver still
 * 		sees the dispatch).
 * 	<li>Folded templates flip {@link VarTemplate#isLiteral()} to {@code true}.
 * </ul>
 */
class StableValueFolding_Test extends TestBase {

	/** Counter-backed Var so we can detect dispatch vs fold. */
	public static class CountingVar extends SimpleVar {
		final AtomicInteger calls;
		final boolean stable;

		public CountingVar() { this(new AtomicInteger(), false); }

		public CountingVar(AtomicInteger calls, boolean stable) {
			super("CV");
			this.calls = calls;
			this.stable = stable;
		}

		@Override
		public String resolve(VarResolverSession session, String key) {
			calls.incrementAndGet();
			return "[" + key + "]";
		}

		@Override
		protected boolean isStable() { return stable; }
	}

	@Test void a01_stableVarFoldsAtCompileTime() {
		var counter = new AtomicInteger();
		var vr = VarResolver.create().vars(new CountingVar(counter, true)).build();

		var tpl = vr.compile("hello $CV{name}");
		assertEquals(1, counter.get(), "Stable var resolved exactly once at compile time");

		// Resolving N more times should not increment the counter (template is folded).
		for (var i = 0; i < 5; i++)
			assertEquals("hello [name]", tpl.resolve(vr.createSession()));
		assertEquals(1, counter.get(), "Folded literal does not redispatch on resolve");

		// Folded-only template flips isLiteral().
		assertTrue(tpl.isLiteral());
	}

	@Test void a02_unstableVarDoesNotFold() {
		var counter = new AtomicInteger();
		var vr = VarResolver.create().vars(new CountingVar(counter, false)).build();

		var tpl = vr.compile("hello $CV{name}");
		assertEquals(0, counter.get(), "Compile must not invoke unstable var");

		assertEquals("hello [name]", tpl.resolve(vr.createSession()));
		assertEquals(1, counter.get());
		assertEquals("hello [name]", tpl.resolve(vr.createSession()));
		assertEquals(2, counter.get(), "Each resolve dispatches separately");

		assertFalse(tpl.isLiteral());
	}

	@Test void a03_systemPropertyFoldsAtCompileTime() {
		System.setProperty("StableValueFolding_Test.a03", "fred");
		try {
			var vr = VarResolver.create().vars(SystemPropertiesVar.class).build();
			var tpl = vr.compile("$S{StableValueFolding_Test.a03}");

			// Mutate the property after compile — folded value should not change.
			System.setProperty("StableValueFolding_Test.a03", "barney");
			assertEquals("fred", tpl.resolve(vr.createSession()), "Folded value frozen at compile time");
			assertTrue(tpl.isLiteral());
		} finally {
			System.clearProperty("StableValueFolding_Test.a03");
		}
	}

	@Test void a04_nestedLiveMarkersInBodyDisableFolding() {
		var counter = new AtomicInteger();
		var vr = VarResolver.create()
			.vars(new CountingVar(counter, true))
			.vars(SystemPropertiesVar.class)
			.build();

		// Body contains a nested ${...} — fold must NOT apply; the inner ref needs runtime.
		System.setProperty("StableValueFolding_Test.a04", "x");
		try {
			var tpl = vr.compile("$CV{$S{StableValueFolding_Test.a04}}");
			assertEquals(0, counter.get(), "Outer var must not be folded when body has live nested marker");
			assertEquals("[x]", tpl.resolve(vr.createSession()));
		} finally {
			System.clearProperty("StableValueFolding_Test.a04");
		}
	}

	@Test void a05_envVarOpsInForFold() {
		// EnvVariablesVar.isStable returns true; finding a guaranteed-set env var to fold-test
		// would be brittle on different OSes, so this sticks to the SPI assertion.
		assertTrue(new EnvVariablesVarOpener().isStableForTest());
	}

	@Test void a06_systemPropertyVarOpsInForFold() {
		assertTrue(new SystemPropertiesVarOpener().isStableForTest());
	}

	@Test void a07_argsVarDefaultIsStable() {
		assertTrue(new ArgsVarOpener().isStableForTest());
	}

	@Test void a08_propertyVarStaysUnstable() {
		assertFalse(new PropertyVarOpener().isStableForTest());
	}

	private static final class EnvVariablesVarOpener extends EnvVariablesVar {
		boolean isStableForTest() { return isStable(); }
	}

	private static final class SystemPropertiesVarOpener extends SystemPropertiesVar {
		boolean isStableForTest() { return isStable(); }
	}

	private static final class ArgsVarOpener extends ArgsVar {
		boolean isStableForTest() { return isStable(); }
	}

	private static final class PropertyVarOpener extends PropertyVar {
		boolean isStableForTest() { return isStable(); }
	}
}
