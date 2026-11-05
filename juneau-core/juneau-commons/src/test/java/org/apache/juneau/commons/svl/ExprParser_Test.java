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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.Expr.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link ExprParser} Pratt parser ({@link ExprParser#parse(String, int)}).
 */
class ExprParser_Test extends TestBase {

	private static Expr parse(String s) {
		return ExprParser.parse(s, 32);
	}

	private static String show(String s) {
		return parse(s).show();
	}

	private static ExprException err(String s) {
		return err(s, 32);
	}

	private static ExprException err(String s, int maxDepth) {
		var e = assertThrows(ExprException.class, () -> ExprParser.parse(s, maxDepth));
		assertTrue(e.getMessage().contains("^"), e.getMessage());
		assertEquals(s, e.getSource());
		return e;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Literals and primaries
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_literals() {
		assertEquals("'it''s'", show("'it''s'"));
		assertEquals("'x'", show("\"x\""));
		assertEquals("42", show("42"));
		assertEquals("1.5", show("1.5"));
		assertEquals("true", show("true"));
		assertEquals("false", show("false"));
		assertEquals("null", show("null"));
		assertInstanceOf(Literal.class, parse("null"));
	}

	@Test void a02_primaries() {
		assertEquals("a", show("a"));
		assertInstanceOf(Ident.class, parse("a"));
		assertEquals("${C:k}", show("${C:k}"));
		assertInstanceOf(VarRef.class, parse("${C:k}"));
		assertEquals("(+ ${C:k} 1)", show("${C:k} + 1"));
		assertEquals("(f a (+ b 1))", show("f(a, b+1)"));
		assertEquals("(f)", show("f()"));
		assertEquals("(f (g x) 'y')", show("f(g(x), 'y')"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Precedence and associativity
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_arithmetic() {
		assertEquals("(+ 1 (* 2 3))", show("1+2*3"));
		assertEquals("(* (+ 1 2) 3)", show("(1+2)*3"));
		assertEquals("(- (- 1 2) 3)", show("1 - 2 - 3"));
		assertEquals("(% (/ a b) c)", show("a / b % c"));
		assertEquals("(+ (- x) (+ y))", show("-x + +y"));
		assertEquals("(- (. x y))", show("-x.y"));
	}

	@Test void b02_ternaryAndElvis() {
		assertEquals("(? a b (? c d e))", show("a?b:c?d:e"));
		assertEquals("(?: a (?: b c))", show("a?:b?:c"));
		assertEquals("(? (or a b) c d)", show("a or b ? c : d"));
		assertEquals("(? a (? b c d) e)", show("a ? b ? c : d : e"));
		assertEquals("(? (?: a b) c d)", show("a ?: b ? c : d"));
		assertEquals("(?: (+ a 1) b)", show("a + 1 ?: b"));
	}

	@Test void b03_logical() {
		assertEquals("(and (not a) b)", show("not a and b"));
		assertEquals("(and (not a) b)", show("!a && b"));
		assertEquals("(or a (and b c))", show("a or b and c"));
		assertEquals("(or a (and b c))", show("a || b && c"));
		assertEquals("(or (and a b) c)", show("a and b or c"));
		assertEquals("(not (not a))", show("not not a"));
	}

	@Test void b04_comparison() {
		assertEquals("(== (== a b) c)", show("a == b == c"));
		assertEquals("(matches a 'x.*')", show("a matches 'x.*'"));
		assertEquals("(and (> a 1) (<= b 2))", show("a > 1 and b <= 2"));
		// Equality and relational share one binding power (5), so they chain left-associatively.
		assertEquals("(>= (!= (< a b) c) d)", show("a < b != c >= d"));
		assertEquals("(matches (== a b) c)", show("a == b matches c"));
		assertEquals(16, err("a matches b + c matches d").getPos());
		assertEquals("(== (+ a 1) (* b 2))", show("a + 1 == b * 2"));
		assertEquals("(== (matches a b) c)", show("a matches b == c"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Postfix
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_paths() {
		assertEquals("(idx (idx (?. (. a b) c) 'k') 0)", show("a.b?.c['k'][0]"));
		assertEquals("(?idx a 0)", show("a?.[0]"));
		assertEquals("(idx a (+ i 1))", show("a[i + 1]"));
		assertEquals("(. (f x) y)", show("f(x).y"));
		assertEquals("(. ${C:k} y)", show("${C:k}.y"));
	}

	@Test void c02_selectProject() {
		assertEquals("(.! (.? list (> age 18)) name)", show("list.?[age > 18].![name]"));
		assertEquals("(.? items (and (> age 18) (matches name 'A.*')))", show("items.?[age > 18 and name matches 'A.*']"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Keyword rules
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_keywordCalls() {
		assertEquals("(not x)", show("not(x)"));
		assertInstanceOf(Call.class, parse("not(x)"));
		assertInstanceOf(Call.class, parse("not (x)"));
		assertEquals("(and a b)", show("and(a,b)"));
		assertInstanceOf(Call.class, parse("and(a,b)"));
		assertEquals("(or a b)", show("or(a, b)"));
		assertInstanceOf(Call.class, parse("or(a, b)"));
		// The symbol form is always the operator.
		assertInstanceOf(Unary.class, parse("!(x)"));
		// Keyword call inside an operator expression.
		assertEquals("(and (not x) y)", show("not(x) and y"));
	}

	@Test void d02_keywordPropertyNames() {
		assertEquals("(. obj not)", show("obj.not"));
		assertEquals("(?. a matches)", show("a?.matches"));
		assertEquals("(. (. a and) null)", show("a.and.null"));
		assertEquals("(. a true)", show("a.true"));
	}

	@Test void d03_keywordErrors() {
		assertEquals(0, err("and").getPos());
		assertEquals(0, err("matches x").getPos());
		assertEquals(2, err("a.&&b").getPos());
		assertEquals(2, err("a.${C:k}").getPos());
		assertEquals(2, err("a.'k'").getPos());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Positions
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_positions() {
		var b = (Binary)parse("ab + cd");
		assertEquals(3, b.pos());
		assertEquals(0, b.l().pos());
		assertEquals(5, b.r().pos());
		assertEquals(2, ((Path)parse("a .b")).pos());
		assertEquals(1, ((Path)parse("a?.b")).pos());
		assertEquals(1, ((Index)parse("a[0]")).pos());
		assertEquals(1, ((Index)parse("a?.[0]")).pos());
		assertEquals(1, ((Select)parse("a.?[x]")).pos());
		assertEquals(1, ((Project)parse("a.![x]")).pos());
		assertEquals(2, ((Call)parse("  f(x)")).pos());
		assertEquals(0, ((Unary)parse("-x")).pos());
		assertEquals(2, ((Ternary)parse("a ? b : c")).pos());
		assertEquals(2, ((Elvis)parse("a ?: b")).pos());
		assertEquals(1, ((Literal)parse("(42)")).pos());
		assertEquals(0, ((VarRef)parse("${C:k}")).pos());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Depth
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_depth() {
		var ok = "(".repeat(32) + "1" + ")".repeat(32);
		assertEquals("1", ExprParser.parse(ok, 32).show());
		var bad = "(".repeat(33) + "1" + ")".repeat(33);
		var e = err(bad, 32);
		assertEquals("expression nested too deeply", e.getBaseMessage());
		assertEquals(33, e.getPos());
		// Prefix chains and nested calls count too.
		assertEquals("expression nested too deeply", err("-".repeat(40) + "x", 32).getBaseMessage());
		assertEquals("expression nested too deeply", err("f(".repeat(40) + "x" + ")".repeat(40), 32).getBaseMessage());
		// Long left-associative chains do not nest.
		assertDoesNotThrow(() -> ExprParser.parse("1" + "+1".repeat(200), 32));
		assertDoesNotThrow(() -> ExprParser.parse("a" + ".b".repeat(200), 32));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Errors
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_trailingGarbage() {
		var e = err("1 2");
		assertEquals(2, e.getPos());
		assertTrue(e.getMessage().contains("1 2\n  ^"), e.getMessage());
		assertEquals(2, err("a b").getPos());
		assertEquals(3, err("(a))").getPos());
	}

	@Test void g02_empty() {
		assertEquals(0, err("").getPos());
		assertEquals(3, err("   ").getPos());
		assertEquals(1, err("()").getPos());
	}

	@Test void g03_unclosed() {
		assertEquals(4, err("(1+2").getPos());
		assertEquals(4, err("f(a,").getPos());
		assertEquals(4, err("f(a b)").getPos());
		assertEquals(3, err("a[0").getPos());
		assertEquals(6, err("a.?[x ").getPos());
		assertEquals(3, err("1 +").getPos());
		assertEquals(2, err("a.").getPos());
		assertEquals(2, err("a.(b)").getPos());
	}

	@Test void g04_ternaryMissingColon() {
		var e = err("a ? b");
		assertEquals(5, e.getPos());
		assertEquals(6, err("a ? b c").getPos());
	}

	@Test void g05_matchesNonAssociative() {
		var e = err("a matches b matches c");
		assertEquals(12, e.getPos());
	}

	@Test void g06_unexpectedToken() {
		assertEquals(0, err(")").getPos());
		assertEquals(0, err("* 2").getPos());
		assertEquals(2, err("a ] b").getPos());
		assertEquals(0, err(":").getPos());
	}

	@Test void g07_malformedPostfixAndCalls() {
		assertEquals(3, err("a.b(c)").getPos());
		assertEquals(4, err("f(a)(b)").getPos());
		assertEquals(2, err("f(,)").getPos());
		assertEquals(4, err("f(a,)").getPos());
		assertEquals(2, err("a[]").getPos());
		assertEquals(4, err("a.?[]").getPos());
		assertEquals(2, err("a.!").getPos());
		assertEquals(4, err("-x ** y").getPos());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Review follow-ups (pinning current behavior)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_prefixChains() {
		assertEquals("(- (- 1))", show("- - 1"));
		assertEquals("(not (not a))", show("!!a"));
		// Negative literals are never folded: Unary("-", Literal).
		var u = assertInstanceOf(Unary.class, parse("-1"));
		assertEquals("-", u.op());
		assertEquals(1L, assertInstanceOf(Literal.class, u.e()).v());
		// Prefix not binds tighter than comparison.
		assertEquals("(== (not a) b)", show("not a == b"));
		// Keyword-call rule: 'not (' is a call, then the postfix applies to the call.
		assertEquals("(. (not a) b)", show("not (a).b"));
		assertInstanceOf(Call.class, ((Path)parse("not (a).b")).target());
	}

	@Test void h02_conditionalMixes() {
		assertEquals("(? a (?: b c) d)", show("a ? b ?: c : d"));
		assertEquals("(?: (or a b) c)", show("a or b ?: c"));
	}

	@Test void h03_matchesMixes() {
		// Accepted by design: matches shares binding power 5 with the comparison operators.
		assertEquals("(matches (== a b) c)", show("a == b matches c"));
		// Only DIRECTLY adjacent matches operators are rejected; an intervening level-5 operator resets the check.
		assertEquals("(matches (== (matches a b) c) d)", show("a matches b == c matches d"));
	}

	@Test void h04_keywordPrefixedIdentifiers() {
		for (var s : new String[]{"android", "nullable", "order"}) {
			var id = assertInstanceOf(Ident.class, parse(s));
			assertEquals(s, id.name());
		}
	}
}
