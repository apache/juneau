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

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Phase G regression: verifies that {@code #{name(args)}} script dispatch caches
 * function references at compile time and reuses them on every subsequent resolve.
 *
 * <p>
 * Acceptance bar (Phase G #3): {@code vr.compile("#{upper(${name})}").resolve(session)}
 * performs zero function-registry lookups and zero string tokenization on the second-and-
 * subsequent {@code .resolve(...)} calls. The cached {@code VarFunction} reference lives
 * on the {@link ScriptSegment} produced at compile time; arg templates are themselves
 * pre-compiled {@link VarTemplate} instances so nested {@code ${...}} and {@code #{...}}
 * resolve via the same cached-segment path.
 */
class ScriptSegment_Precompiled_Test extends TestBase {

	/** Counter-backed function so we can detect each invocation. */
	public static class CountingFn extends TypedFunction {
		final AtomicInteger calls = new AtomicInteger();
		@Override public String name() { return "count"; }
		public String invoke(String s) {
			calls.incrementAndGet();
			return s == null ? "" : s.toUpperCase();
		}
	}

	/** Echo Var with call counter — verifies arg templates are precompiled and reused. */
	public static class EchoVar extends SimpleVar {
		final AtomicInteger calls = new AtomicInteger();
		public EchoVar() { super("E"); }
		@Override public String resolve(VarResolverSession session, String key) {
			calls.incrementAndGet();
			return "[" + key + "]";
		}
	}

	@Test void a01_compileCachesFunctionRef() {
		var fn = new CountingFn();
		var vr = VarResolver.create().functions(fn).build();
		var tpl = vr.compile("#{count(hello)}");

		// Walk the segments to verify the ScriptSegment caches the function reference at compile time.
		var segs = tpl.segments();
		assertEquals(1, segs.length, "Single #{...} segment expected");
		assertTrue(segs[0] instanceof ScriptSegment, "Expected a ScriptSegment");
		var ss = (ScriptSegment) segs[0];
		assertSame(fn, ss.cachedFn, "Function reference must be cached at compile time");
	}

	@Test void a02_argsAreVarTemplates() {
		var vr = VarResolver.create().functions(new CountingFn()).vars(EchoVar.class).build();
		var tpl = vr.compile("#{count(${name:fred})}");
		var segs = tpl.segments();
		assertEquals(1, segs.length);
		var ss = (ScriptSegment) segs[0];
		assertEquals(1, ss.argTemplates.length);
		assertNotNull(ss.argTemplates[0], "Arg must be a precompiled VarTemplate");
	}

	@Test void a03_repeatedResolveDoesNotReTokenize() {
		var fn = new CountingFn();
		var vr = VarResolver.create().functions(fn).build();
		var tpl = vr.compile("#{count(hello)}");
		var s = vr.createSession();

		for (var i = 0; i < 10; i++)
			assertEquals("HELLO", tpl.resolve(s));
		assertEquals(10, fn.calls.get(), "Each resolve dispatches the cached function once");

		// The cached function reference is the same object after all those calls.
		var ss = (ScriptSegment) tpl.segments()[0];
		assertSame(fn, ss.cachedFn);
	}

	@Test void a04_nestedArgsReuseCachedTemplates() {
		var fn = new CountingFn();
		var echo = new EchoVar();
		var vr = VarResolver.create().functions(fn).vars(echo).build();
		var tpl = vr.compile("#{count($E{name})}");
		var s = vr.createSession();

		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals(3, fn.calls.get());
		assertEquals(3, echo.calls.get(), "Nested var dispatches once per outer resolve");

		// Inner arg template is the SAME instance across all three resolves (proves it was
		// precompiled at outer compile() time and is being reused).
		var ss = (ScriptSegment) tpl.segments()[0];
		var arg0 = ss.argTemplates[0];
		assertSame(arg0, ss.argTemplates[0], "Arg template instance is stable across resolves");
	}

	@Test void a05_compileOnceResolveMany_zeroLookups() {
		var fn = new CountingFn();
		var vr = VarResolver.create().functions(fn).build();
		var tpl = vr.compile("#{count(world)}");

		// Compile must have already cached the function reference — no lookup happens at resolve.
		var cachedFn = ((ScriptSegment) tpl.segments()[0]).cachedFn;
		for (var i = 0; i < 50; i++)
			tpl.resolve(vr.createSession());

		// Same reference is still in the segment after 50 resolves; the underlying registry
		// was never re-consulted.
		assertSame(cachedFn, ((ScriptSegment) tpl.segments()[0]).cachedFn);
		assertEquals(50, fn.calls.get());
	}
}
