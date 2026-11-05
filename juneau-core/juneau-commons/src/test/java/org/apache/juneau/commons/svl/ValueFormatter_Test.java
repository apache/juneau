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

import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the value formatter applied to {@code #{...}} results.
 */
class ValueFormatter_Test extends TestBase {

	/** Function returning a fixed typed value. */
	static VarFunction fn(String name, Supplier<Object> s) {
		return new VarFunction() {
			@Override public String name() { return name; }
			@Override public int minArity() { return 0; }
			@Override public int maxArity() { return 0; }
			@Override public String invoke(VarResolverSession session, List<Object> args) { return "legacy"; }
			@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return s.get(); }
		};
	}

	static VarResolver.Builder base() {
		return VarResolver.create().defaultFunctions()
			.functions(fn("names", () -> List.of("a", "b")))
			.functions(fn("nums", () -> List.of(1, 2.5, 3.0, true)))
			.functions(fn("themap", () -> new LinkedHashMap<>(Map.of("k", "v"))))
			.functions(fn("nested", () -> List.of(List.of("x"), Map.of("m", Arrays.asList(1, null)))))
			.functions(fn("arr", () -> new int[]{1, 2}))
			.functions(fn("esc", () -> List.of("q\"b\\s\nt\u0001é")))
			.functions(fn("nul", () -> null))
			.functions(fn("dbl", () -> 4.0))
			.functions(fn("struct", () -> Structure.ul(List.of("p", "q"))))
			.functions(fn("cyc", () -> { var l = new ArrayList<Object>(); l.add(l); return l; }))
			.functions(fn("big", () -> new ArrayList<>(Collections.nCopies(10, "z"))));
	}

	@Test void a01_list() { assertEquals("[\"a\",\"b\"]", base().build().resolve("#{names()}")); }
	@Test void a02_stringBare() { assertEquals("s", base().build().resolve("#{'s'}")); }
	@Test void a03_null() { assertEquals("", base().build().resolve("#{null}")); assertEquals("", base().build().resolve("#{nul()}")); }
	@Test void a04_integralDouble() { assertEquals("4", base().build().resolve("#{dbl()}")); }
	@Test void a05_numbersAndBooleansBareInContainers() { assertEquals("[1,2.5,3,true]", base().build().resolve("#{nums()}")); }
	@Test void a06_map() { assertEquals("{\"k\":\"v\"}", base().build().resolve("#{themap()}")); }
	@Test void a07_nested() { assertEquals("[[\"x\"],{\"m\":[1,null]}]", base().build().resolve("#{nested()}")); }
	@Test void a08_array() { assertEquals("[1,2]", base().build().resolve("#{arr()}")); }
	@Test void a09_escaping() { assertEquals("[\"q\\\"b\\\\s\\nt\\u0001é\"]", base().build().resolve("#{esc()}")); }

	@Test void b01_builderFormatter() {
		var vr = base().valueFormatter(o -> "<" + o + ">").build();
		assertEquals("<[a, b]>", vr.resolve("#{names()}"));
	}
	@Test void b02_sessionOverrideWins() {
		var vr = base().valueFormatter(o -> "B").build();
		var s = vr.createSession().valueFormatter(o -> "S:" + o);
		assertEquals("S:4.0", s.resolve("#{dbl()}"));
	}
	@Test void b03_sessionFormatsStructure() {
		var vr = base().build();
		assertEquals("- p\n- q", vr.resolve("#{struct()}"));
		var s = vr.createSession().valueFormatter(o -> o instanceof Structure st ? "K=" + st.kind() : String.valueOf(o));
		assertEquals("K=UL", s.resolve("#{struct()}"));
	}
	@Test void b04_builderFormatterBypassedForStructure() {
		var vr = base().valueFormatter(o -> "X").build();
		assertEquals("- p\n- q", vr.resolve("#{struct()}"));
	}
	@Test void b05_copyPreserves() {
		var vr = base().valueFormatter(o -> "X").build().copy().build();
		assertEquals("X", vr.resolve("#{names()}"));
	}
	@Test void b06_copyPreservesDefault() {
		assertEquals("[\"a\",\"b\"]", base().build().copy().build().resolve("#{names()}"));
	}
	@Test void b07_formatterReturningNull() {
		var vr = base().valueFormatter(o -> null).build();
		assertEquals("[]", vr.resolve("[#{names()}]"));
	}
	@Test void b08_sessionFormatterReturningNull() {
		var s = base().build().createSession().valueFormatter(o -> null);
		assertEquals("[]", s.resolve("[#{names()}]"));
	}
	@Test void b09_formatValueDirect() {
		var s = base().build().createSession();
		assertEquals("", s.formatValue(null));
		assertEquals("[1]", s.formatValue(List.of(1)));
	}
	@Test void b10_sessionChaining() {
		var s = base().build().createSession();
		assertSame(s, s.valueFormatter(String::valueOf));
	}

	@Test void c01_cycleLenientTruncates() {
		assertEquals("[\"...\"]", base().build().resolve("#{cyc()}"));
	}
	@Test void c02_cycleStrictThrows() {
		var vr = base().strict(true).build();
		assertThrows(RuntimeException.class, () -> vr.resolve("#{cyc()}"));
	}
	@Test void c03_collectionLimitLenientTruncates() {
		var r = base().maxCollectionSize(3).build().resolve("#{big()}");
		assertEquals("[\"z\",\"z\",\"z\",\"...\"]", r);
	}
	@Test void c04_collectionLimitStrictThrows() {
		var vr = base().maxCollectionSize(3).strict(true).build();
		assertThrows(RuntimeException.class, () -> vr.resolve("#{big()}"));
	}
	@Test void c05_beanFallsBackToQuotedToString() {
		var vr = base().functions(fn("bean", () -> List.of(new StringBuilder("sb")))).build();
		assertEquals("[\"sb\"]", vr.resolve("#{bean()}"));
	}

	// Budget / cycles / DAG

	static Object fanOut() {
		var a = new ArrayList<Object>();
		a.add(a);
		a.add(a);
		return a;
	}

	static Object dag() {
		List<Object> n = List.of("x");
		for (int i = 0; i < 25; i++)
			n = List.of(n, n);
		return n;
	}

	@Test void d01_fanOutLenient() {
		var vr = base().functions(fn("fan", ProxyHolder::fan)).build();
		var r = vr.resolve("#{fan()}");
		assertTrue(r.length() < 1_000_000, "len=" + r.length());
		assertTrue(r.contains("..."), r);
	}
	@Test void d02_fanOutStrict() {
		var vr = base().strict(true).functions(fn("fan", ProxyHolder::fan)).build();
		assertThrows(RuntimeException.class, () -> vr.resolve("#{fan()}"));
	}
	@Test void d03_bigSelfReferencingLenient() {
		var vr = base().functions(fn("selfbig", () -> { var l = new ArrayList<Object>(); for (int i = 0; i < 1000; i++) l.add(l); return l; })).build();
		var r = vr.resolve("#{selfbig()}");
		assertTrue(r.length() < 1_000_000, "len=" + r.length());
	}
	@Test void d04_bigSelfReferencingStrict() {
		var vr = base().strict(true).functions(fn("selfbig", () -> { var l = new ArrayList<Object>(); for (int i = 0; i < 1000; i++) l.add(l); return l; })).build();
		var ex = assertThrows(RuntimeException.class, () -> vr.resolve("#{selfbig()}"));
		assertTrue(ex.getMessage().contains("cyclic"), ex.getMessage());
	}
	@Test void d05_sharedDagBounded() {
		var vr = base().functions(fn("dag", ProxyHolder::dag)).build();
		var r = vr.resolve("#{dag()}");
		assertTrue(r.length() < 2_000_000, "len=" + r.length());
		assertTrue(r.contains("..."));
	}
	@Test void d06_smallSharedAcyclicNotACycle() {
		var shared = List.of("s");
		var vr = base().strict(true).functions(fn("sh", () -> List.of(shared, shared))).build();
		assertEquals("[[\"s\"],[\"s\"]]", vr.resolve("#{sh()}"));
	}

	// Escaping and numbers

	@Test void e01_extraEscapes() {
		var vr = base().functions(fn("e", () -> List.of("\u007f\u2028\u2029"))).build();
		assertEquals("[\"\\u007f\\u2028\\u2029\"]", vr.resolve("#{e()}"));
	}
	@Test void e02_loneSurrogateKept() {
		var vr = base().functions(fn("e", () -> List.of("a\ud800b"))).build();
		assertEquals("[\"a\ud800b\"]", vr.resolve("#{e()}"));
	}
	@Test void e03_floatShortest() {
		var vr = base().functions(fn("f", () -> List.of(0.1f, 2.0f))).build();
		assertEquals("[0.1,2]", vr.resolve("#{f()}"));
	}
	@Test void e04_nonFinite() {
		var vr = base().functions(fn("f", () -> List.of(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.0, Float.NaN))).build();
		assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\",0,\"NaN\"]", vr.resolve("#{f()}"));
	}

	// Throwing formatter

	@Test void f01_throwingFormatterWrapped() {
		var vr = base().valueFormatter(o -> { throw new IllegalStateException("boom"); }).build();
		var ex = assertThrows(ExprException.class, () -> vr.resolve("#{names()}"));
		assertTrue(ex.getMessage().contains("boom"), ex.getMessage());
	}
	@Test void f02_throwingSessionFormatterWrapped() {
		var s = base().build().createSession().valueFormatter(o -> { throw new IllegalStateException("bang"); });
		assertThrows(ExprException.class, () -> s.resolve("#{names()}"));
	}
	@Test void f03_foldTimeFailureFallsThroughToRender() {
		var vr = VarResolver.create().defaultFunctions().valueFormatter(o -> { throw new IllegalStateException("boom"); }).build();
		var tpl = vr.compile("#{1+1}");   // compile must not throw
		assertThrows(ExprException.class, () -> tpl.resolve(vr.createSession()));
	}

	// Folding vs per-render override

	@Test void g01_stableCollectionNotFoldedSoOverrideApplies() {
		var vr = base().build();
		// Collections are never folded (only stable literal/var expressions fold, and those yield scalars here), so a compiled template honours the override.
		var tpl = vr.compile("#{names()}");
		assertEquals("[\"a\",\"b\"]", tpl.resolve(vr.createSession()));
		assertEquals("L2", tpl.resolve(vr.createSession().valueFormatter(o -> "L" + ((Collection<?>)o).size())));
	}
	/** Stable var whose typed value is a List. */
	public static class StableList extends DefaultingVar {
		public StableList() { super("SL"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "text"; }
		@Override protected Object resolveValue(VarResolverSession session, String arg) { return List.of("p", "q"); }
		@Override protected boolean isStable() { return true; }
	}

	@Test void g03_stableCollectionExpressionHonoursOverride() {
		var vr = base().vars(new StableList()).build();
		var tpl = vr.compile("#{${SL:x}}");
		assertEquals("[\"p\",\"q\"]", tpl.resolve(vr.createSession()));
		assertEquals("L2", tpl.resolve(vr.createSession().valueFormatter(o -> "L" + ((Collection<?>)o).size())));
	}

	@Test void g02_stableScalarFoldedOverrideIgnored() {
		var vr = base().build();
		var tpl = vr.compile("#{1+1}");
		assertEquals("2", tpl.resolve(vr.createSession().valueFormatter(o -> "S" + o)));
	}

	/** Holder to keep lambdas short. */
	static class ProxyHolder {
		static Object fan() { return fanOut(); }
		static Object dag() { return ValueFormatter_Test.dag(); }
	}
}
