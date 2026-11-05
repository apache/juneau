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
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Regression test: verifies that {@code #{name(args)}} expressions are compiled once (one {@link ExprSegment}
 * holding the parsed AST) and only <em>evaluated</em> on each subsequent resolve.
 *
 * <p>
 * Evidence used: a {@link VarResolver} subclass counts {@code findFunction} lookups.  Compile performs a fixed number
 * (the unknown-function check, one per call node); each render performs exactly one per call node (evaluation) and
 * no more, so the count grows by a constant per resolve and never re-checks the whole tree.  The parsed AST
 * instance held by the segment (read reflectively) is the same object across all resolves, which shows nothing is
 * re-parsed (the parser is static and cannot be counted directly).
 */
class ExprSegment_Precompiled_Test extends TestBase {

	/** Counter-backed function so we can detect each invocation. */
	public static class CountingFn extends TypedFunction {
		final AtomicInteger calls = new AtomicInteger();
		@Override public String name() { return "count"; }
		public String invoke(String s) {
			calls.incrementAndGet();
			return s == null ? "" : s.toUpperCase();
		}
	}

	/** Echo Var with call counter. */
	public static class EchoVar extends SimpleVar {
		final AtomicInteger calls = new AtomicInteger();
		public EchoVar() { super("E"); }
		@Override public String resolve(VarResolverSession session, String key) {
			calls.incrementAndGet();
			return "[" + key + "]";
		}
	}

	/** Resolver that counts function lookups. */
	public static class CountingResolver extends VarResolver {
		final Map<String,AtomicInteger> lookups = new HashMap<>();
		CountingResolver(VarResolver.Builder b) { super(b); }
		@Override VarFunction findFunction(String name) {
			lookups.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
			return super.findFunction(name);
		}
		int count(String name) { return lookups.containsKey(name) ? lookups.get(name).get() : 0; }
	}

	@SafeVarargs
	private static CountingResolver counting(VarFunction fn, Class<? extends Var>...vars) {
		return new CountingResolver(VarResolver.create().functions(fn).vars(vars));
	}

	private static Object astOf(TemplateSegment seg) throws Exception {
		var f = ExprSegment.class.getDeclaredField("ast");
		f.setAccessible(true);
		return f.get(seg);
	}

	@Test void a01_compileProducesSingleExprSegmentAndChecksFunctionOnce() {
		var fn = new CountingFn();
		var vr = counting(fn);
		var tpl = vr.compile("#{count('hello')}");
		var segs = tpl.segments();
		assertEquals(1, segs.length, "Single #{...} segment expected");
		assertInstanceOf(ExprSegment.class, segs[0]);
		assertEquals(1, vr.count("count"), "Compile looks the function up exactly once (unknown-function check)");
		assertEquals(0, fn.calls.get(), "Compile must not invoke the function for a non-stable call");
	}

	@Test void a02_nestedVarRefInsideCall() {
		var vr = counting(new CountingFn(), EchoVar.class);
		var tpl = vr.compile("#{count(${E:fred})}");
		assertEquals(1, tpl.segments().length);
		assertInstanceOf(ExprSegment.class, tpl.segments()[0]);
		assertEquals("[FRED]", tpl.resolve(vr.createSession()));
	}

	@Test void a03_repeatedResolveDoesNotRecheckOrReparse() throws Exception {
		var fn = new CountingFn();
		var vr = counting(fn);
		var tpl = vr.compile("#{count('hello')}");
		var compileLookups = vr.count("count");
		var seg = tpl.segments()[0];
		var ast = astOf(seg);
		var s = vr.createSession();

		for (var i = 0; i < 10; i++)
			assertEquals("HELLO", tpl.resolve(s));
		assertEquals(10, fn.calls.get(), "Each resolve dispatches the function once");
		assertEquals(compileLookups + 10, vr.count("count"), "Exactly one lookup per render, none extra");
		assertSame(seg, tpl.segments()[0], "Segment instance is stable across resolves");
		assertSame(ast, astOf(seg), "Parsed AST is the same instance (never re-parsed)");
	}

	@Test void a04_nestedArgsEvaluatedOncePerResolve() throws Exception {
		var fn = new CountingFn();
		var echo = new EchoVar();
		var vr = new CountingResolver(VarResolver.create().functions(fn).vars(echo));
		var tpl = vr.compile("#{count(${E:name})}");
		var ast = astOf(tpl.segments()[0]);
		var s = vr.createSession();

		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals("[NAME]", tpl.resolve(s));
		assertEquals(3, fn.calls.get());
		assertEquals(3, echo.calls.get(), "Nested var dispatches once per outer resolve");
		assertSame(ast, astOf(tpl.segments()[0]), "Expression tree (including the nested var reference) is stable");
	}

	@Test void a05_compileOnceResolveMany() throws Exception {
		var fn = new CountingFn();
		var vr = counting(fn);
		var tpl = vr.compile("#{count('world')}");
		var base = vr.count("count");
		var seg = tpl.segments()[0];
		var ast = astOf(seg);
		for (var i = 0; i < 50; i++)
			assertEquals("WORLD", tpl.resolve(vr.createSession()));
		assertSame(seg, tpl.segments()[0]);
		assertSame(ast, astOf(seg));
		assertEquals(base + 50, vr.count("count"));
		assertEquals(50, fn.calls.get());
	}
}
