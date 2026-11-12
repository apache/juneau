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

import java.math.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.functions.*;
import org.apache.juneau.commons.svl.hidden.*;
import org.junit.jupiter.api.*;

/** Tests for {@link ExprEvaluator}. */
class ExprEvaluator_Test extends TestBase {

	/** Counting function proving laziness. */
	public static class Cnt implements VarFunction {
		static final AtomicInteger N = new AtomicInteger();
		@Override public String name() { return "cnt"; }
		@Override public int minArity() { return 1; }
		@Override public int maxArity() { return 1; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return String.valueOf(invokeValue(session, args)); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { N.incrementAndGet(); return args.get(0); }
	}

	/** A record. */
	public record Pt(int x, String name) {}

	/** A plain bean. */
	public static class Bean {
		public String pub = "P";
		public String getFoo() { return "foo"; }
		public boolean isOk() { return true; }
		public Pt getPt() { return new Pt(7, "seven"); }
	}

	/** A user enum, one constant with a body. */
	public enum Color {
		RED("r") { @Override public String toString() { return "red"; } },
		GREEN("g");
		private final String label;
		Color(String label) { this.label = label; }
		public String getLabel() { return label; }
	}

	/** Bean exposing reflective types. */
	public static class Leaky {
		public Class<?> getKlass() { return String.class; }
		public Thread getT() { return Thread.currentThread(); }
		public Object getBoom() { throw new IllegalStateException("boom"); }
		public String getOk() { return "ok"; }
	}

	/** Function that signals fall-through from inside invokeValue. */
	public static class Fall implements VarFunction {
		@Override public String name() { return "fall"; }
		@Override public int minArity() { return 1; }
		@Override public int maxArity() { return 1; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { throw new ExprEvaluator.FallThrough("nested"); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { throw new ExprEvaluator.FallThrough("nested"); }
	}

	/** Bean with Error-throwing getters and an Object-typed getter holding a Class. */
	public static class ErrBean {
		public Object getOom() { throw new OutOfMemoryError("oom"); }
		public Object getSoe() { throw new StackOverflowError("soe"); }
		public Object getObj() { return String.class; }
	}

	/** Var that always fails. */
	public static class BoomVar extends SimpleVar {
		public BoomVar() { super("BOOM"); }
		@Override public String resolve(VarResolverSession session, String key) { throw new IllegalStateException("kaboom"); }
	}

	private static final VarResolver LENIENT = VarResolver.create().defaultVars().defaultFunctions().functions(new Cnt(), new Fall()).vars(new Var_ResolveValue_Test.Multi(), new BoomVar()).build();
	private static final VarResolver STRICT = VarResolver.create().defaultVars().defaultFunctions().functions(new Cnt(), new Fall()).vars(new Var_ResolveValue_Test.Multi(), new BoomVar()).strict(true).build();

	private static Map<String,Object> root() {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", 5L);
		m.put("s", "hi");
		m.put("nul", null);
		m.put("map", new LinkedHashMap<>(Map.of("k", "v")));
		m.put("pt", new Pt(3, "three"));
		m.put("bean", new Bean());
		m.put("list", List.of(1L, 2L, 3L, 4L));
		m.put("arr", new int[]{10, 20});
		m.put("people", List.of(new Pt(1, "a"), new Pt(20, "b"), new Pt(30, "c")));
		m.put("thread", Thread.currentThread());
		m.put("leaky", new Leaky());
		m.put("x", 99L);
		m.put("cls", String.class);
		m.put("classes", List.of(String.class));
		m.put("clsMap", new LinkedHashMap<>(Map.of("k", String.class)));
		m.put("clsArr", new Object[]{String.class});
		m.put("errb", new ErrBean());
		m.put("errbs", List.of(new ErrBean()));
		m.put("hidList", List.of(HiddenFixture.create()));
		m.put("hid2", HiddenFixture.createViaInterface());
		m.put("leakies", List.of(new Leaky()));
		m.put("boom", "outer");
		m.put("en", Color.RED);
		m.put("tz", TimeUnit.SECONDS);
		m.put("immut", Map.of("a", 1));
		m.put("tree", new TreeMap<>(Map.of("a", 1)));
		m.put("bd", new BigDecimal("1.5"));
		m.put("hid", HiddenFixture.create());
		var big = new ArrayList<Long>();
		for (var i = 0L; i < 1000; i++) big.add(i);
		m.put("big", big);
		var self = new LinkedHashMap<String,Object>();
		self.put("b", self);
		m.put("selfm", self);
		return m;
	}

	private static Object ev(VarResolver vr, String src) {
		var root = root();
		var s = vr.createSession();
		return ExprEvaluator.eval(ExprParser.parse(src, 32), new ExprContext(s, root::get, null, src));
	}

	private static Object ev(String src) { return ev(LENIENT, src); }
	private static Object evs(String src) { return ev(STRICT, src); }

	@Test void a01_literalsAndIdent() {
		assertEquals(3L, ev("3"));
		assertEquals("x", ev("'x'"));
		assertEquals(5L, ev("a"));
		assertEquals("hi", ev("s"));
		assertNull(ev("nosuch"));
		assertNull(evs("nosuch"));
		assertEquals(-4L, ev("-4"));
	}

	@Test void a02_operators() {
		assertEquals(7L, ev("a + 2"));
		assertEquals(3L, ev("a - 2"));
		assertEquals(10L, ev("a * 2"));
		assertEquals(2L, ev("a % 3"));
		assertEquals(true, ev("a == 5"));
		assertEquals(true, ev("a != 4"));
		assertEquals(true, ev("a > 4 and a < 6"));
		assertEquals(true, ev("a >= 5 or false"));
		assertEquals(true, ev("!(a == 4)"));
		assertEquals("true", ev("not (a == 4)"));  // word-form not followed by ( is the public function call, which returns a String
		assertEquals(true, ev("s matches 'h.'"));
		assertEquals(-5L, ev("-a"));
		assertEquals(5L, ev("+a"));
		assertEquals("hi!", ev("s + '!'"));
	}

	@Test void a03_operatorErrorHasPosition() {
		var e = assertThrows(ExprException.class, () -> ev("1 + (2 / 0)"));
		assertTrue(e.getMessage().contains("^"), e.getMessage());
		assertTrue(e.getMessage().contains("1 + (2 / 0)"), e.getMessage());
		assertNotNull(e.getCause());
		assertNotEquals("null", e.getBaseMessage());
	}

	@Test void b01_pathMapRecordBean() {
		assertEquals("v", ev("map.k"));
		assertEquals(3, ev("pt.x"));
		assertEquals("three", ev("pt.name"));
		assertEquals("foo", ev("bean.foo"));
		assertEquals(true, ev("bean.ok"));
		assertEquals("P", ev("bean.pub"));
		assertEquals("seven", ev("bean.pt.name"));
	}

	@Test void b02_pathNullAndMissing() {
		assertNull(ev("nul.x"));
		assertNull(ev("nul?.x"));
		assertNull(evs("nul?.x"));
		assertThrows(ExprException.class, () -> evs("nul.x"));
		assertNull(ev("pt.nosuch"));
		assertThrows(ExprException.class, () -> evs("pt.nosuch"));
		assertNull(ev("map.nosuch"));
		assertThrows(ExprException.class, () -> evs("map.nosuch"));
	}

	@Test void b03_pathBlocked() {
		assertThrows(ExprException.class, () -> ev("pt.class"));
		assertThrows(ExprException.class, () -> ev("pt.getClass"));
		assertThrows(ExprException.class, () -> ev("bean.class"));
		assertNull(ev("thread.name"));   // a Thread read from the root scope is treated as absent (lenient)
		assertThrows(ExprException.class, () -> evs("thread.name"));
		assertThrows(ExprException.class, () -> ev("map.class"));
	}

	@Test void b04_pathOnSafeJavaTypes() {
		assertEquals(false, ev("list.empty"));
		assertEquals(4, ev("list.size"));
		assertEquals(2, ev("s.length"));
	}

	@Test void c01_index() {
		assertEquals("v", ev("map['k']"));
		assertEquals(1L, ev("list[0]"));
		assertEquals(4L, ev("list[3]"));
		assertEquals(20, ev("arr[1]"));
		assertNull(ev("list[9]"));
		assertThrows(ExprException.class, () -> evs("list[9]"));
		assertNull(ev("nul[0]"));
		assertNull(evs("nul?.[0]"));
		assertThrows(ExprException.class, () -> evs("nul[0]"));
		assertNull(ev("map['zz']"));
		assertThrows(ExprException.class, () -> evs("map['zz']"));
	}

	@Test void d01_ternaryIsLazy() {
		Cnt.N.set(0);
		assertEquals("A", ev("true ? 'A' : cnt('B')"));
		assertEquals(0, Cnt.N.get());
		assertEquals("B", ev("false ? cnt('A') : cnt('B')"));
		assertEquals(1, Cnt.N.get());
		assertEquals("B", ev("'false' ? 1 : 'B'"));
		assertThrows(ExprException.class, () -> ev("3 ? 1 : 2"));
	}

	@Test void d02_andOrShortCircuit() {
		Cnt.N.set(0);
		assertEquals(false, ev("false and cnt(true)"));
		assertEquals(true, ev("true or cnt(true)"));
		assertEquals(0, Cnt.N.get());
		assertEquals(true, ev("true and cnt(true)"));
		assertEquals(true, ev("false or cnt(true)"));
		assertEquals(2, Cnt.N.get());
		assertEquals(true, ev("not false"));
		assertThrows(ExprException.class, () -> ev("3 and true"));
	}

	@Test void d02b_callFormAndOrNotArePublicFunctions() throws Exception {
		var s = LENIENT.createSession();
		var and = s.findFunction("and").invokeValue(s, Arrays.<Object>asList(1L, "yes"));
		assertEquals(and, ev("and(1, 'yes')"));
		assertEquals(s.findFunction("or").invokeValue(s, Arrays.<Object>asList(0L, "on")), ev("or(0, 'on')"));
		assertEquals(s.findFunction("not").invokeValue(s, Arrays.<Object>asList("no")), ev("not('no')"));
		// eager: no short-circuit
		Cnt.N.set(0);
		ev("and(false, cnt(true))");
		assertEquals(1, Cnt.N.get());
	}

	@Test void d04_callLooksUpBeforeEvaluatingArgs() {
		Cnt.N.set(0);
		var e = assertThrows(ExprException.class, () -> evs("nosuchfn(cnt(1))"));
		assertTrue(e.getMessage().contains("unknown function"), e.getMessage());
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("nosuchfn(cnt(1))"));
		assertThrows(ExprException.class, () -> ev("cnt(cnt(1), 2)"));
		assertEquals(0, Cnt.N.get());
	}

	@Test void d03_elvisLazyNullOnly() {
		Cnt.N.set(0);
		assertEquals(5L, ev("a ?: cnt(1)"));
		assertEquals(false, ev("false ?: cnt(1)"));
		assertEquals(0L, ev("0 ?: cnt(1)"));
		assertEquals("", ev("'' ?: cnt(1)"));
		assertEquals(0, Cnt.N.get());
		assertEquals(9L, ev("nul ?: cnt(9)"));
		assertEquals(1, Cnt.N.get());
	}

	@Test void e01_select() {
		assertEquals(List.of(2L, 3L, 4L), ev("list.?[this > 1]"));
		assertEquals(List.of(), ev("list.?[this > 100]"));
		assertEquals(List.of(new Pt(20, "b"), new Pt(30, "c")), ev("people.?[x > 10]"));
	}

	@Test void e02_project() {
		assertEquals(List.of(2L, 4L, 6L, 8L), ev("list.![this * 2]"));
		assertEquals(List.of("a", "b", "c"), ev("people.![name]"));
		assertEquals(List.of(10, 20), ev("arr.![this]"));
		// outer scope visible
		assertEquals(List.of(6L, 7L, 8L, 9L), ev("list.![this + a]"));
		// nested element scope
		var four = List.of(2L, 3L, 4L, 5L);
		assertEquals(List.of(four, four, four), ev("people.![list.![this + 1]]"));
	}

	@Test void e03_mapIterationYieldsEntries() {
		assertEquals(List.of("k=v"), ev("map.![key + '=' + value]"));
	}

	@Test void e04_nullTarget() {
		assertNull(ev("nul.?[true]"));
		assertNull(ev("nul.![1]"));
		assertThrows(ExprException.class, () -> evs("nul.?[true]"));
		assertThrows(ExprException.class, () -> evs("nul.![1]"));
	}

	@Test void e05_limits() {
		var small = VarResolver.create().defaultFunctions().maxCollectionSize(2).build();
		var smallStrict = VarResolver.create().defaultFunctions().maxCollectionSize(2).strict(true).build();
		assertEquals(List.of(1L, 2L), ev(small, "list.![this]"));
		assertThrows(ExprException.class, () -> ev(smallStrict, "list.![this]"));
		var shallow = VarResolver.create().defaultFunctions().maxExprDepth(2).build();
		var e = assertThrows(ExprException.class, () -> ev(shallow, "people.![list.![list.![this]]]"));
		assertTrue(e.getMessage().contains("nested too deeply"), e.getMessage());
	}

	@Test void e06_selectProjectDeniedBeforeIteration() {
		// Only $op.project is registered, so $op.select is denied.
		Cnt.N.set(0);
		var noSelect = VarResolver.create().functions(new Cnt(), new OpFunctions.Project()).strict(true).build();
		var noSelectLenient = VarResolver.create().functions(new Cnt(), new OpFunctions.Project()).build();
		var e = assertThrows(ExprException.class, () -> ev(noSelect, "list.?[cnt(true)]"));
		assertTrue(e.getMessage().contains("$op.select"), e.getMessage());
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(noSelectLenient, "list.?[cnt(true)]"));
		assertEquals(0, Cnt.N.get());
		assertEquals(List.of(1L, 1L, 1L, 1L), ev(noSelectLenient, "list.![cnt(1)]"));
		assertEquals(4, Cnt.N.get());
		// Only select registered: project denied.
		var noProject = VarResolver.create().functions(new Cnt(), new OpFunctions.Select()).build();
		Cnt.N.set(0);
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(noProject, "list.![cnt(1)]"));
		assertEquals(0, Cnt.N.get());
	}

	@Test void e07_predicateNullOrNonBoolean() {
		assertEquals(List.of(), ev("list.?[nul]"));
		assertEquals(List.of(), ev("list.?[this]"));
		assertThrows(ExprException.class, () -> evs("list.?[nul]"));
		assertThrows(ExprException.class, () -> evs("list.?[this]"));
		assertEquals(List.of(1L, 2L, 3L, 4L), ev("list.?['TRUE']"));
	}

	@Test void e08_sharedCollectionBudget() {
		// A single projection of exactly maxCollectionSize elements works.
		assertEquals(1000, ((List<?>)ev("big.![this]")).size());
		assertEquals(1000, ((List<?>)evs("big.![this]")).size());
		// Nested iteration shares ONE budget per evaluation.
		assertThrows(ExprException.class, () -> evs("big.![big.![this]]"));
		var r = (List<?>)ev("big.![big.![this]]");
		// The first outer element spends 1 of the 1000, its inner projection gets the other 999, then the outer loop stops.
		assertEquals(1, r.size());
		assertEquals(999, ((List<?>)r.get(0)).size());
		// Budget is per evaluation, not per context: a fresh evaluation starts over.
		assertEquals(1000, ((List<?>)ev("big.![this]")).size());
	}

	@Test void f01_callAndUnknown() {
		assertEquals(4L, ev("cnt(4)"));
		var e = assertThrows(ExprException.class, () -> evs("nosuchfn(1)"));
		assertTrue(e.getMessage().contains("unknown function"), e.getMessage());
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("nosuchfn(1)"));
		assertThrows(ExprException.class, () -> ev("cnt(1, 2)"));
	}

	@Test void f02_deniedOperator() {
		var bare = VarResolver.create().build();
		var bareStrict = VarResolver.create().strict(true).build();
		var e = assertThrows(ExprException.class, () -> ev(bareStrict, "true and false"));
		assertTrue(e.getMessage().contains("operator '$op.and' is not available"), e.getMessage());
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(bare, "true and false"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(bare, "true ? 1 : 2"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(bare, "1 + 2"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev(bare, "nul ?: 2"));
	}

	@Test void g01_varRefs() {
		System.setProperty("j0439.k", "v");
		System.setProperty("j0439.name", "j0439.k");
		try {
			assertEquals("v", ev("${S:j0439.k}"));
			assertEquals("v", ev("${j0439.k}"));
			assertEquals("v", ev("${S:${S:j0439.name}}"));
			assertEquals("${S:j0439.k}", ev("'${S:j0439.k}'"));
			assertEquals("vx", ev("${S:j0439.k} + 'x'"));
			assertNull(ev("${S:j0439.absent}"));
			assertEquals("d", ev("${S:j0439.absent:d}"));
			assertEquals("dflt", ev("${j0439.absent:dflt}"));
			assertEquals(5, ev("${MP:a:b}"));
		} finally {
			System.clearProperty("j0439.k");
			System.clearProperty("j0439.name");
		}
	}

	@Test void g02_unknownPrefixFallsToPropertyShortcut() {
		assertEquals("fallback", ev("${myprop:fallback}"));
		assertEquals("fallback", evs("${myprop:fallback}"));
		assertEquals("k", ev("${Zz:k}"));
		assertNull(ev("${nosuchprop.zz}"));
	}

	@Test void g03_varFailureCarriesPosition() {
		var e = assertThrows(ExprException.class, () -> evs("1 + ${BOOM:x}"));
		assertTrue(e.getMessage().contains("kaboom"), e.getMessage());
		assertTrue(e.getMessage().contains("^"), e.getMessage());
		assertEquals(4, e.getPos());
	}

	@Test void h01_thisAtTopLevel() {
		var s = LENIENT.createSession();
		var ctx = new ExprContext(s, root()::get, "TOP", "this");
		assertEquals("TOP", ExprEvaluator.eval(ExprParser.parse("this", 32), ctx));
	}

	@Test void i01_blockedProperties() {
		for (var src : new String[]{"bean.Class", "bean.CLASS", "bean.getClass", "bean.GetClass", "pt.declaringClass", "pt.getDeclaringClass", "en.declaringClass", "en.class", "people.![class]", "people.![Class]", "people.![declaringClass]"})
			assertThrows(ExprException.class, () -> ev(src), src);
		assertEquals("r", ev("en.label"));
	}

	@Test void i02_reflectiveResultTypesRefused() {
		assertThrows(ExprException.class, () -> ev("leaky.klass"));
		assertThrows(ExprException.class, () -> ev("leaky.t"));
		assertThrows(ExprException.class, () -> ev("leakies.![klass]"));
		assertEquals("ok", ev("leaky.ok"));
	}

	@Test void i03_userEnumGetter() {
		assertEquals("r", ev("en.label"));
		assertThrows(ExprException.class, () -> ev("tz.name"));
	}

	@Test void j01_runtimeExceptionsBecomeExprException() {
		var e = assertThrows(ExprException.class, () -> ev("immut[nul]"));
		assertNotNull(e.getCause());
		assertTrue(e.getCause() instanceof NullPointerException);
		assertNotEquals("null", e.getBaseMessage(), e.getBaseMessage());
		assertTrue(e.getMessage().contains("^"), e.getMessage());
		var e2 = assertThrows(ExprException.class, () -> ev("tree[1]"));
		assertTrue(e2.getCause() instanceof ClassCastException);
		assertNotEquals("null", e2.getBaseMessage());
	}

	@Test void j02_deepChainsNeverRawStackOverflow() throws Exception {
		var sb = new StringBuilder("1");
		for (var i = 0; i < 5000; i++) sb.append("+1");
		var sum = ExprParser.parse(sb.toString(), 32, Integer.MAX_VALUE);
		var sb2 = new StringBuilder("selfm");
		for (var i = 0; i < 5000; i++) sb2.append(".b");
		var path = ExprParser.parse(sb2.toString(), 32, Integer.MAX_VALUE);
		for (var tree : new Expr[]{sum, path}) {
			var result = new AtomicReference<Throwable>();
			var t = new Thread(null, () -> {
				try {
					var s = LENIENT.createSession();
					var root = root();
					ExprEvaluator.eval(tree, new ExprContext(s, root::get, null, "chain"));
				} catch (Throwable x) {
					result.set(x);
				}
			}, "small-stack", 64 * 1024);
			t.start();
			t.join();
			var x = result.get();
			assertTrue(x instanceof ExprException, String.valueOf(x));
			assertTrue(x.getMessage().contains("too complex"), x.getMessage());
		}
	}

	@Test void k01_throwingGetter() {
		// Lenient: null, and must NOT fall through to the outer scope's 'boom'.
		assertEquals(Arrays.asList((Object)null), ev("leakies.![boom]"));
		assertThrows(ExprException.class, () -> evs("leakies.![boom]"));
		assertNull(ev("leaky.boom"));
		assertThrows(ExprException.class, () -> evs("leaky.boom"));
	}

	@Test void k02_genuinelyAbsentFallsBackToOuterScope() {
		assertEquals(List.of(5L, 5L, 5L), ev("people.![a]"));
	}

	@Test void k03_inaccessibleClass() {
		assertNull(ev("hid.x"));
		assertThrows(ExprException.class, () -> evs("hid.x"));
	}

	@Test void l01_deniedOperatorsAllFallThrough() {
		var bare = VarResolver.create().build();
		var bareStrict = VarResolver.create().strict(true).build();
		for (var src : new String[]{"1 + 2", "1 - 2", "1 * 2", "1 / 2", "1 % 2", "1 == 2", "1 != 2", "1 < 2", "1 <= 2", "1 > 2", "1 >= 2",
				"'a' matches 'a'", "-1", "-a", "+a", "not true", "true and false", "true or false", "true ? 1 : 2", "nul ?: 2", "list.?[true]", "list.![1]"}) {
			assertThrows(ExprEvaluator.FallThrough.class, () -> ev(bare, src), src);
			assertThrows(ExprException.class, () -> ev(bareStrict, src), src);
		}
	}

	@Test void m01_bigDecimalAndNegativeIndex() {
		assertEquals(0, new BigDecimal("2.5").compareTo((BigDecimal)ev("bd + 1")));
		assertEquals(true, ev("bd == 1.5"));
		assertNull(ev("list[-1]"));
		assertThrows(ExprException.class, () -> evs("list[-1]"));
	}

	@Test void m02_numericMapKeysLoose() {
		var m = new HashMap<Object,Object>();
		m.put(1L, "one");
		var s = LENIENT.createSession();
		var ctx = new ExprContext(s, Map.<String,Object>of("m", m)::get, null, "m[1]");
		assertEquals("one", ExprEvaluator.eval(ExprParser.parse("m[1]", 32), ctx));
	}

	@Test void n01_errorsFromGettersPropagateUnwrapped() {
		for (var vr : new VarResolver[]{LENIENT, STRICT}) {
			assertThrows(OutOfMemoryError.class, () -> ev(vr, "errb.oom"));
			assertThrows(OutOfMemoryError.class, () -> ev(vr, "errbs.![oom]"));
			var e = assertThrows(ExprException.class, () -> ev(vr, "errb.soe"));
			assertTrue(e.getMessage().contains("too complex"), e.getMessage());
			assertThrows(ExprException.class, () -> ev(vr, "errbs.![soe]"));
		}
	}

	@Test void n02_forbiddenTypesEverywhere() {
		assertNull(ev("cls"));
		assertThrows(ExprException.class, () -> evs("cls"));
		assertEquals(Arrays.asList((Object)null), ev("classes.![this]"));
		assertThrows(ExprException.class, () -> evs("classes.![this]"));
		assertEquals(List.of(), ev("classes.?[true]"));
		assertThrows(ExprException.class, () -> evs("classes.?[true]"));
		for (var vr : new VarResolver[]{LENIENT, STRICT}) {
			assertThrows(ExprException.class, () -> ev(vr, "errb.obj"), "object-typed getter");
			assertThrows(ExprException.class, () -> ev(vr, "clsMap.k"), "map value via path");
			assertThrows(ExprException.class, () -> ev(vr, "clsMap['k']"), "map value via index");
			assertThrows(ExprException.class, () -> ev(vr, "clsArr[0]"), "array element");
			assertThrows(ExprException.class, () -> ev(vr, "clsMap.![value]"), "map entry value in element scope");
		}
	}

	@Test void n03_fallThroughPropagatesFromFunctions() {
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("fall(1)"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("cnt(fall(1))"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("1 + fall(1)"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("list.![fall(this)]"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("list.?[fall(this)]"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("people.![list.![fall(this)]]"));
		assertThrows(ExprEvaluator.FallThrough.class, () -> ev("cnt(list.![nosuchfn(this)])"));
	}

	@Test void n04_inaccessibleGetterDoesNotFallBackToOuterScope() {
		assertEquals(Arrays.asList((Object)null), ev("hidList.![x]"));
		assertThrows(ExprException.class, () -> evs("hidList.![x]"));
		assertEquals(2, ev("hid2.y"));
		assertEquals(List.of(2, 2, 2, 2), ev("list.![hid2.y]"));
	}

	@Test void n05_mapKeyNarrowingIsRangeChecked() {
		var m = new HashMap<Object,Object>();
		m.put(1, "int-one");
		var s = LENIENT.createSession();
		var ctx = new ExprContext(s, Map.<String,Object>of("m", m)::get, null, "m[4294967297]");
		assertNull(ExprEvaluator.eval(ExprParser.parse("m[4294967297]", 32), ctx));
		assertEquals("int-one", ExprEvaluator.eval(ExprParser.parse("m[1]", 32), ctx));
	}
}
