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

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/** Tests for {@link VarResolver.Builder#allowFunctions(String...)}. */
class VarResolver_AllowFunctions_Test extends TestBase {

	/** Returns a list so select/project have something to iterate. */
	public static class Nums implements VarFunction {
		@Override public String name() { return "nums"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "nums"; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return List.of(1L, 2L, 3L); }
	}

	private static VarResolver.Builder base(boolean strict) {
		return VarResolver.create().defaultFunctions().functions(new Nums()).strict(strict);
	}

	private static String r(VarResolver vr, String t) { return vr.resolve(t); }

	private static String msg(VarResolver vr, String t) {
		return assertThrows(ExprException.class, () -> vr.resolve(t)).getMessage();
	}

	@Test void a01_unspecifiedAllowsAll() {
		var vr = base(true).build();
		assertEquals("3", r(vr, "#{1+2}"));
		assertEquals("A", r(vr, "#{upper('a')}"));
	}

	@Test void a02_onlyUpper() {
		var vr = base(true).allowFunctions("upper").build();
		assertEquals("A", r(vr, "#{upper('a')}"));
		assertTrue(msg(vr, "#{1+2}").contains("operator '$op.add' is not permitted"));
	}

	@Test void a03_upperAndOps() {
		var vr = base(true).allowFunctions("$op.*", "upper").build();
		assertEquals("4", r(vr, "#{1+3}"));
		assertEquals("A", r(vr, "#{upper('a')}"));
		assertTrue(msg(vr, "#{lower('A')}").contains("function 'lower' is not permitted"));
	}

	@Test void a04_emptyDeniesAll() {
		var vr = base(true).allowFunctions().build();
		assertTrue(msg(vr, "#{upper('a')}").contains("not permitted"));
		assertTrue(msg(vr, "#{1+2}").contains("not permitted"));
		assertTrue(msg(vr, "#{true ? 1 : 2}").contains("not permitted"));
	}

	@Test void b01_selectAndProject() {
		var ok = base(true).allowFunctions("$op.*", "nums").build();
		assertEquals("[2,3]", r(ok, "#{nums().?[this > 1]}"));
		var noSel = base(true).allowFunctions("nums", "$op.gt", "$op.project").build();
		assertTrue(msg(noSel, "#{nums().?[this > 1]}").contains("operator '$op.select' is not permitted"));
		var noProj = base(true).allowFunctions("nums", "$op.gt", "$op.add", "$op.select").build();
		assertTrue(msg(noProj, "#{nums().![this + 1]}").contains("operator '$op.project' is not permitted"));
	}

	@Test void c01_structuralOperatorsEachDenied() {
		for (var e : Map.of(
			"ternary", "#{true ? 1 : 2}",
			"elvis", "#{null ?: 2}",
			"and", "#{true and false}",
			"or", "#{true or false}",
			"not", "#{not true}").entrySet()) {
			var ok = base(true).allowFunctions("$op.*").build();
			assertDoesNotThrow(() -> r(ok, e.getValue()), e.getKey());
			var vr = base(true).allowFunctions("upper", "$op.add", "$op.neg", "$op.gt", "$op.eq").build();
			assertTrue(msg(vr, e.getValue()).contains("operator '$op." + e.getKey() + "' is not permitted"), e.getKey());
		}
	}

	@Test void d01_copyPreserves() {
		var vr = base(true).allowFunctions("upper").build();
		var c = vr.copy().build();
		assertEquals("A", r(c, "#{upper('a')}"));
		assertTrue(msg(c, "#{1+2}").contains("not permitted"));
		assertTrue(msg(c, "#{lower('A')}").contains("not permitted"));
	}

	@Test void e01_deniedDistinctFromUnknown() {
		var vr = base(true).allowFunctions("upper").build();
		assertTrue(msg(vr, "#{lower('A')}").contains("function 'lower' is not permitted"));
		var unk = msg(vr, "#{nosuch('A')}");
		assertTrue(unk.contains("unknown function 'nosuch'"), unk);
		assertFalse(unk.contains("not permitted"));
		assertNull(vr.createSession().findFunction("lower"));
		assertTrue(vr.createSession().isFunctionDenied("lower"));
		assertFalse(vr.createSession().isFunctionDenied("upper"));
	}

	@Test void f01_lenientLiteral() {
		var vr = base(false).allowFunctions("upper").build();
		assertEquals("#{1+2}", r(vr, "#{1+2}"));
		assertEquals("#{lower('A')}", r(vr, "#{lower('A')}"));
		assertEquals("A", r(vr, "#{upper('a')}"));
		assertEquals("x #{1+2} y", r(vr, "x #{1+2} y"));
	}

	@Test void g01_globPatterns() {
		var vr = base(true).allowFunctions("up*", "*wer", "u*r").build();
		assertEquals("A", r(vr, "#{upper('a')}"));
		assertEquals("a", r(vr, "#{lower('A')}"));
		assertTrue(msg(vr, "#{trim(' a ')}").contains("not permitted"));
	}

	@Test void g02_exactNotPrefix() {
		var vr = base(true).allowFunctions("uppe").build();
		assertTrue(msg(vr, "#{upper('a')}").contains("not permitted"));
	}

	private static final String[][] BINARY = {
		{"+","add"},{"-","sub"},{"*","mul"},{"/","div"},{"%","mod"},{"==","eq"},{"!=","ne"},{"<","lt"},{"<=","le"},{">","gt"},{">=","ge"},{"matches","matches"}};

	@Test void h01_eachBinaryOperatorDenied() {
		for (var b : BINARY) {
			var src = "#{4 " + b[0] + " 2}";
			var all = base(true).allowFunctions("$op.*").build();
			assertDoesNotThrow(() -> r(all, b[0].equals("matches") ? "#{'a' matches 'a'}" : src), b[0]);
			var vr = base(true).allowFunctions("$op.*").build().copy().build();
			assertNotNull(vr);
			// allow every operator except this one
			var pats = new ArrayList<String>();
			for (var o : BINARY) if (! o[1].equals(b[1])) pats.add("$op." + o[1]);
			var d = base(true).allowFunctions(pats.toArray(new String[0])).build();
			var m = msg(d, b[0].equals("matches") ? "#{'a' matches 'a'}" : src);
			assertTrue(m.contains("operator '$op." + b[1] + "' is not permitted"), b[0] + ": " + m);
		}
	}

	@Test void h02_eachUnaryOperatorDenied() {
		for (var u : new String[][]{{"-","neg","#{-4}"},{"+","pos","#{+4}"},{"!","not","#{!true}"}}) {
			var pats = new ArrayList<>(List.of("$op.neg", "$op.pos", "$op.not"));
			pats.remove("$op." + u[1]);
			var d = base(true).allowFunctions(pats.toArray(new String[0])).build();
			var m = msg(d, u[2]);
			assertTrue(m.contains("operator '$op." + u[1] + "' is not permitted"), u[0] + ": " + m);
			assertDoesNotThrow(() -> r(base(true).allowFunctions("$op.*").build(), u[2]));
		}
	}

	@Test void i01_compileTimeUntakenBranches() {
		var vr = base(true).allowFunctions("$op.*").build();
		assertTrue(msg(vr, "#{true ? 1 : lower('A')}").contains("function 'lower' is not permitted"));
		assertTrue(msg(vr, "#{false and lower('A')}").contains("function 'lower' is not permitted"));
		assertTrue(msg(vr, "#{true or lower('A')}").contains("function 'lower' is not permitted"));
	}

	@Test void i02_deniedInsidePredicateArgAndVarBody() {
		var vr = base(true).allowFunctions("$op.*", "nums", "upper").build();
		assertTrue(msg(vr, "#{nums().?[lower('A') == 'a']}").contains("function 'lower' is not permitted"));
		assertTrue(msg(vr, "#{nums().![lower('A')]}").contains("function 'lower' is not permitted"));
		assertTrue(msg(vr, "#{upper(lower('A'))}").contains("function 'lower' is not permitted"));
	}

	/** Counts invocations; returns a property name so a leak would be visible in the output. */
	public static class Spy implements VarFunction {
		static final java.util.concurrent.atomic.AtomicInteger N = new java.util.concurrent.atomic.AtomicInteger();
		@Override public String name() { return "spy"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 1; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return String.valueOf(invokeValue(session, args)); }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { N.incrementAndGet(); return "j0439.allow.hit"; }
	}

	/**
	 * A denied function inside a nested <c>${...}</c> body never executes.
	 *
	 * <p>
	 * Inside <c>#{}</c> the <c>${...}</c> body is a plain var key (it is not template-resolved), so
	 * <c>#{spy()}</c> there is looked up literally as a key, finds nothing, and yields empty/null.  At the top level
	 * (<c>${S:#{spy()}}</c>) the nested expression is compiled and the denied function is rejected.
	 */
	@Test void i03_deniedInNestedVarBodyNeverRuns() {
		System.setProperty("j0439.allow.hit", "LEAK");
		for (var strict : new boolean[]{true, false}) {
			var vr = VarResolver.create().defaultFunctions().vars(org.apache.juneau.commons.svl.vars.SystemPropertiesVar.class).functions(new Spy()).strict(strict).allowFunctions("$op.*", "upper").build();
			Spy.N.set(0);
			assertEquals("", r(vr, "#{${S:#{spy()}}}"), "strict=" + strict);
			assertEquals("", r(vr, "#{upper(${S:#{spy('x')}})}"), "strict=" + strict);
			assertEquals("nullx", r(vr, "#{${S:#{spy()}} + 'x'}"), "strict=" + strict);
			assertEquals(0, Spy.N.get(), "denied function must never execute, strict=" + strict);
			if (strict) {
				assertTrue(msg(vr, "${S:#{spy()}}").contains("function 'spy' is not permitted"));
				assertTrue(msg(vr, "#{spy()}").contains("function 'spy' is not permitted"));
			} else {
				assertEquals("", r(vr, "${S:#{spy()}}"));
				assertEquals("#{spy()}", r(vr, "#{spy()}"));
			}
			assertEquals(0, Spy.N.get(), "strict=" + strict);
		}
	}

	@Test void i04_varValueContainingExpressionIsNotReEvaluated() {
		System.setProperty("j0439.allow.k", "#{lower('A')}");
		for (var strict : new boolean[]{true, false}) {
			var vr = VarResolver.create().defaultFunctions().vars(org.apache.juneau.commons.svl.vars.SystemPropertiesVar.class).strict(strict).allowFunctions("$op.*", "upper").build();
			assertEquals("#{lower('A')}", r(vr, "#{${S:j0439.allow.k}}"), "strict=" + strict);
			assertEquals("#{LOWER('A')}", r(vr, "#{upper(${S:j0439.allow.k})}"), "strict=" + strict);
		}
	}

	@Test void j01_globMetacharactersLiteral() {
		var vr = base(true).allowFunctions("up.er", "$op.a+d").build();
		assertTrue(msg(vr, "#{upper('a')}").contains("not permitted"));
		assertTrue(msg(vr, "#{1+2}").contains("not permitted"));
		var exact = base(true).allowFunctions("up.er").functions(new VarFunction() {
			@Override public String name() { return "up.er"; }
			@Override public int minArity() { return 0; }
			@Override public int maxArity() { return 0; }
			@Override public String invoke(VarResolverSession s, List<Object> a) { return "ok"; }
			@Override public Object invokeValue(VarResolverSession s, List<Object> a) { return "ok"; }
		}).build();
		assertNotNull(exact.createSession().findFunction("up.er"));
		assertNull(exact.createSession().findFunction("upper"));
	}

	@Test void k01_loneStarEmptyAndAdditive() {
		var all = base(true).allowFunctions("*").build();
		assertEquals("3", r(all, "#{1+2}"));
		assertEquals("A", r(all, "#{upper('a')}"));
		var none = base(true).allowFunctions("").build();
		assertTrue(msg(none, "#{upper('a')}").contains("not permitted"));
		assertTrue(msg(none, "#{1+2}").contains("not permitted"));
		var add = base(true).allowFunctions("upper").allowFunctions("$op.*").build();
		assertEquals("4", r(add, "#{1+3}"));
		assertEquals("A", r(add, "#{upper('a')}"));
		var widened = base(true).allowFunctions("upper").build().copy().allowFunctions("$op.*").build();
		assertEquals("4", r(widened, "#{1+3}"));
	}

	@Test void k02_nullPatternsRejected() {
		var b = base(true);
		assertThrows(NullPointerException.class, () -> b.allowFunctions((String[])null));
		assertThrows(NullPointerException.class, () -> b.allowFunctions("a", null));
	}

	@Test void l01_lenientStructuralOperators() {
		var vr = base(false).allowFunctions("upper").build();
		for (var src : new String[]{"#{true ? 1 : 2}", "#{null ?: 2}", "#{true and false}", "#{true or false}", "#{not true}"})
			assertEquals(src, r(vr, src));
	}
}
