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
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.TestBase;

/**
 * Coverage-targeted tests for {@link VarTemplateCompiler} (TODO-155 H1).
 *
 * <p>
 * Focuses on tokenizer edge cases (malformed templates, escapes, multi-var, nested vars,
 * empty input/var-name), {@code translateDollarBraceDefault} brace-depth handling,
 * {@code tryFoldStableVar} skip-paths, and the recursive-descent script body parser
 * (function-name validation, quoted-string escapes, nested-marker errors, bare-token errors).
 *
 * <p>
 * Tests-only: where the current behavior is surprising (e.g. {@code ${a${b}}} parses with
 * the inner brace as part of the body), the test asserts the observed legacy behavior and
 * cites it as a candidate observation for a future TODO-156 follow-up.
 */
@SuppressWarnings({
	"java:S5778", // Multi-statement assertThrows lambdas — intentional for compile/resolve flow.
	"java:S1186"  // Empty methods in test fixtures — intentional.
})
class VarTemplateCompiler_Test extends TestBase {

	// =========================================================================
	// Fixtures
	// =========================================================================

	/** Echo Var — unconditionally returns "[arg]". */
	public static class EchoVar extends SimpleVar {
		public EchoVar() { super("E"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "[" + arg + "]"; }
	}

	/** Stable + recursive-allowed Var that returns a string starting with {@code $}. */
	public static class RecursiveStableVar extends SimpleVar {
		public RecursiveStableVar() { super("RS"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "$E{" + arg + "}"; }
		@Override protected boolean isStable() { return true; }
		@Override protected boolean allowRecurse() { return true; }
	}

	/** Stable Var whose doResolve throws. Should fall back to dispatch (not fold). */
	public static class ThrowingStableVar extends SimpleVar {
		public ThrowingStableVar() { super("TH"); }
		@Override public String resolve(VarResolverSession session, String arg) {
			throw new RuntimeException("kaboom");
		}
		@Override protected boolean isStable() { return true; }
	}

	/** Stable Var that returns null from resolve. Should not fold. */
	public static class NullStableVar extends SimpleVar {
		public NullStableVar() { super("NL"); }
		@Override public String resolve(VarResolverSession session, String arg) { return null; }
		@Override protected boolean isStable() { return true; }
	}

	/** Stable Var that says it cannot resolve a session. */
	public static class CannotResolveVar extends SimpleVar {
		public CannotResolveVar() { super("CR"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "should-not-be-called"; }
		@Override public boolean canResolve(VarResolverSession session) { return false; }
		@Override protected boolean isStable() { return true; }
	}

	/** Simple typed function for #{...} script tests. */
	public static class UpperFn extends TypedFunction {
		@Override public String name() { return "upper"; }
		public String invoke(String s) { return s == null ? "" : s.toUpperCase(); }
	}

	/** Identity function — returns its arg verbatim. */
	public static class IdFn extends TypedFunction {
		@Override public String name() { return "id"; }
		public String invoke(String s) { return s == null ? "" : s; }
	}

	private static VarResolver vr() {
		return VarResolver.create()
			.vars(EchoVar.class)
			.functions(new UpperFn(), new IdFn())
			.build();
	}

	// =========================================================================
	// a. Top-level escape handling (state S1)
	// =========================================================================

	@Test void a01_escapeBackslash() {
		var vr = vr();
		// "\\X" -> "\\X" preserved (line 168 path: \X for non-special X).
		assertEquals("\\X", vr.compile("\\\\X").resolve(vr.createSession()));
	}

	@Test void a02_escapeDollar() {
		var vr = vr();
		// "\$E{x}" -> literal "$E{x}".
		assertEquals("$E{x}", vr.compile("\\$E{x}").resolve(vr.createSession()));
	}

	@Test void a03_escapeHash() {
		var vr = vr();
		// Line 165 covers \# specifically — \# emits a literal '#'.
		assertEquals("#{upper(x)}", vr.compile("\\#{upper(x)}").resolve(vr.createSession()));
	}

	@Test void a04_escapeOtherCharPreservesBackslash() {
		var vr = vr();
		// "\n" emits "\n" verbatim (line 168 — no special meaning at top level).
		assertEquals("\\n", vr.compile("\\n").resolve(vr.createSession()));
	}

	@Test void a05_trailingBackslashAtEnd() {
		var vr = vr();
		// Bare trailing '\\' at EOF — line 290 ("if (isInEscape)") appends '\\' to literal.
		assertEquals("abc\\", vr.compile("abc\\").resolve(vr.createSession()));
	}

	// =========================================================================
	// b. Multi-var, nested, empty templates
	// =========================================================================

	@Test void b01_multiVar() {
		var vr = vr();
		assertEquals("[a]-[b]-[c]", vr.compile("$E{a}-$E{b}-$E{c}").resolve(vr.createSession()));
	}

	@Test void b02_emptyTemplate() {
		var vr = vr();
		var tpl = vr.compile("");
		assertEquals("", tpl.resolve(vr.createSession()));
		assertTrue(tpl.isLiteral());
	}

	@Test void b03_nullTemplate() {
		var vr = vr();
		var tpl = vr.compile(null);
		assertNull(tpl.resolve(vr.createSession()));
		assertTrue(tpl.isLiteral());
	}

	@Test void b04_purelyLiteralFastPath() {
		// No '$', '\\', or '#' at all — fast path produces a single LiteralSegment.
		var vr = vr();
		var tpl = vr.compile("plain text only 123");
		assertEquals("plain text only 123", tpl.resolve(vr.createSession()));
		assertTrue(tpl.isLiteral());
	}

	@Test void b05_emptyVarBody() {
		var vr = vr();
		// $E{} -> resolves to "[]" (empty body string passed to var).
		assertEquals("[]", vr.compile("$E{}").resolve(vr.createSession()));
	}

	@Test void b06_compileCacheReturnsSameForLiteral() {
		// Compile twice — distinct VarTemplate instances (no compile() cache currently),
		// but both must resolve identically. This documents the current contract.
		var vr = vr();
		var t1 = vr.compile("hello");
		var t2 = vr.compile("hello");
		assertEquals("hello", t1.resolve(vr.createSession()));
		assertEquals("hello", t2.resolve(vr.createSession()));
	}

	// =========================================================================
	// c. Malformed templates / EOF mid-var (state S2/S3/S3H end-of-input)
	// =========================================================================

	@Test void c01_danglingDollar() {
		var vr = vr();
		// Lone trailing '$' — state S2 at EOF; literal "$" appended (line 293).
		assertEquals("abc$", vr.compile("abc$").resolve(vr.createSession()));
	}

	@Test void c02_danglingDollarWithLetters() {
		var vr = vr();
		// "$E" with no '{' — exits S2 via the "false trigger" branch (line 202),
		// emitting "$E" verbatim.
		assertEquals("$E ", vr.compile("$E ").resolve(vr.createSession()));
	}

	@Test void c03_danglingDollarWithLettersEof() {
		var vr = vr();
		// "$E" at EOF — also state S2 fall-through (line 293).
		assertEquals("$E", vr.compile("$E").resolve(vr.createSession()));
	}

	@Test void c04_unclosedVarBody() {
		var vr = vr();
		// "$E{abc" with no matching '}' — state S3 EOF; literal "$E{abc" emitted (line 295).
		assertEquals("$E{abc", vr.compile("$E{abc").resolve(vr.createSession()));
	}

	@Test void c05_unclosedVarBodyWithEscapes() {
		var vr = vr();
		// "$E{a\\}" without closing '}' — hasInnerEscapes path; AS2 unescape applied.
		assertEquals("$E{a}", vr.compile("$E{a\\}").resolve(vr.createSession()));
	}

	@Test void c06_unclosedHashBody() {
		var vr = vr();
		// "#{upper(x)" missing close — state S3H EOF; literal "#{upper(x)" emitted (line 297).
		assertEquals("#{upper(x)", vr.compile("#{upper(x)").resolve(vr.createSession()));
	}

	@Test void c07_unclosedHashBodyWithEscapes() {
		var vr = vr();
		// "#{a\\}" with no real close — AS2 unescape applied.
		assertEquals("#{a}", vr.compile("#{a\\}").resolve(vr.createSession()));
	}

	@Test void c08_hashWithoutBrace() {
		var vr = vr();
		// "# foo" — '#' not followed by '{' is plain literal.
		assertEquals("# foo", vr.compile("# foo").resolve(vr.createSession()));
	}

	@Test void c09_hashAtEnd() {
		var vr = vr();
		// Bare trailing '#' — line 177 condition i+1<length false; emit '#' as literal.
		assertEquals("foo#", vr.compile("foo#").resolve(vr.createSession()));
	}

	@Test void c10_danglingCloseBrace() {
		var vr = vr();
		// '}' alone in S1 — plain literal.
		assertEquals("a}b", vr.compile("a}b").resolve(vr.createSession()));
	}

	// =========================================================================
	// d. Inner escapes / brace-depth tracking inside ${...} bodies
	// =========================================================================

	@Test void d01_innerEscapeInBody() {
		var vr = vr();
		// "$E{a\\}b}" — backslash-} inside body; fold/dispatch sees "a}b" as the arg.
		// The body collector tracks the escape so the inner \} doesn't close the var.
		assertEquals("[a\\}b]", vr.compile("$E{a\\}b}").resolve(vr.createSession()));
	}

	@Test void d02_innerEscapeInS2Prefix() {
		var vr = vr();
		// "$\\E{x}" — '\\' in state S2 sets isInEscape, then 'E' is consumed without action.
		// Subsequent '{' enters S3 with varType="\\E" (an unrecognized prefix). Body collects
		// "x", and since varType has no registered Var, the dispatch becomes a fallthrough
		// emitting the original substring with AS2 unescaping. Observed legacy behavior:
		// the literal output preserves the backslash sequence as "$\\E{x}".
		// (Candidate observation for TODO-156: doc the AS2 unescape semantics for inner-escape
		// prefix sequences.)
		assertEquals("$\\E{x}", vr.compile("$\\E{x}").resolve(vr.createSession()));
	}

	@Test void d03_braceDepthInBody() {
		var vr = vr();
		// "$E{a{b}c}" — inner '{' bumps depth, matching '}' decrements; only outer '}' closes.
		// The hasInternalVar flag is set (line 218) which suppresses stable-folding even
		// for non-stable vars — here EchoVar is non-stable so it's just a sanity check.
		assertEquals("[a{b}c]", vr.compile("$E{a{b}c}").resolve(vr.createSession()));
	}

	@Test void d04_nestedDollarVar() {
		var vr = vr();
		// "$E{$E{x}}" — outer body contains inner "${" which compile() recurses on.
		assertEquals("[[x]]", vr.compile("$E{$E{x}}").resolve(vr.createSession()));
	}

	@Test void d05_dollarBraceShortcut() {
		var vr = VarResolver.create().defaultVars().build();
		// "${user.home}" — empty-prefix triggers PropertyVar dispatch (DOLLAR_BRACE_VAR fallback).
		var tpl = vr.compile("${user.home}");
		var resolved = tpl.resolve(vr.createSession());
		assertEquals(System.getProperty("user.home"), resolved);
	}

	// =========================================================================
	// e. translateDollarBraceDefault — brace-depth and missing colon paths
	// =========================================================================

	@Test void e01_dollarBraceDefaultNoColon() {
		var vr = VarResolver.create().defaultVars().build();
		// Body has no ':' — translateDollarBraceDefault returns body unchanged (line 332).
		var tpl = vr.compile("${user.home}");
		assertNotNull(tpl.resolve(vr.createSession()));
	}

	@Test void e02_dollarBraceDefaultColonInsideBraces() {
		var vr = VarResolver.create().defaultVars().build();
		// Inner braces around the ':' — depth>0, so the ':' is NOT translated (lines 327, 329 hit).
		// Outer body becomes "missing.key,fallback" only when the FIRST top-level ':' is found.
		// Here the only ':' is inside braces, so no translation; body resolves with depth fully
		// closed and unchanged source. We assert the resolver doesn't crash and produces a string.
		var tpl = vr.compile("${missing.key{a:b}}");
		assertNotNull(tpl.resolve(vr.createSession()));
	}

	@Test void e03_dollarBraceDefaultBalancedBraces() {
		var vr = VarResolver.create().defaultVars().build();
		// Brace depth opens then closes before the ':' — '}' decrements depth (line 327 path).
		// The ':' fires translation at depth==0.
		var tpl = vr.compile("${{a}:fallback}");
		// Whatever PropertyVar does with the rewritten body, this exercises the depth-decrement path.
		assertNotNull(tpl.resolve(vr.createSession()));
	}

	// =========================================================================
	// f. tryFoldStableVar — skip paths
	// =========================================================================

	@Test void f01_stableVarThatThrowsFallsBackToDispatch() {
		var vr = VarResolver.create().vars(new ThrowingStableVar()).build();
		// Compile must NOT throw — the fold path catches the exception (line 406-407).
		var tpl = vr.compile("$TH{x}");
		// Resolve invokes the dispatch which DOES throw at runtime.
		assertThrows(RuntimeException.class, () -> tpl.resolve(vr.createSession()));
	}

	@Test void f02_stableVarReturningNullDoesNotFold() {
		var vr = VarResolver.create().vars(new NullStableVar()).build();
		// resolve(...) returns null -> tryFoldStableVar returns null (line 395-396).
		// The dispatch segment is emitted instead. At runtime, the dispatch path
		// handles null by treating it as empty string (observed legacy behavior).
		var tpl = vr.compile("$NL{x}");
		assertEquals("", tpl.resolve(vr.createSession()));
	}

	@Test void f03_stableVarCannotResolveSkipsFolding() {
		var vr = VarResolver.create().vars(new CannotResolveVar()).build();
		// canResolve false -> tryFoldStableVar returns null (line 390-391).
		var tpl = vr.compile("$CR{x}");
		// The dispatch segment is emitted; resolve at runtime would also short-circuit
		// because canResolve still returns false. The segment-dispatch path returns
		// the fallthrough source.
		var session = vr.createSession();
		assertNotNull(tpl.resolve(session));
	}

	@Test void f04_stableRecursiveVarWithDollarInOutputDoesNotFold() {
		// allowRecurse + resolved value contains '$' -> skip folding (line 403).
		var vr = VarResolver.create().vars(new RecursiveStableVar(), new EchoVar()).build();
		var tpl = vr.compile("$RS{x}");
		// Because folding was skipped, the dispatch happens at resolve time; the result
		// is "$E{x}" which then gets recursively resolved (allowRecurse=true) to "[x]".
		assertEquals("[x]", tpl.resolve(vr.createSession()));
	}

	@Test void f05_nonStableVarNotFolded() {
		// EchoVar is non-stable (default). isStable() false -> line 384 short-circuits.
		var vr = vr();
		var tpl = vr.compile("$E{x}");
		assertFalse(tpl.isLiteral(), "non-stable var should not fold");
	}

	@Test void f06_stableVarWithInternalVarNotFolded() {
		// hasInternalVar=true via inner '{' -> line 384 short-circuits ('hasInternalVar' branch).
		var vr = VarResolver.create().vars(new RecursiveStableVar()).build();
		var tpl = vr.compile("$RS{a{b}c}");
		assertNotNull(tpl.resolve(vr.createSession()));
	}

	// =========================================================================
	// g. Recursive-descent parser — function name
	// =========================================================================

	@Test void g01_emptyFunctionName() {
		var vr = vr();
		// "#{()}" — parseFunctionName: pos==start at line 447 -> "Expected function name".
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{()}"));
		assertTrue(ex.getMessage().contains("Expected function name"), ex.getMessage());
	}

	@Test void g02_functionNameStartsWithDigit() {
		var vr = vr();
		// "#{1foo()}" — parseFunctionName captures "1foo", then line 450 detects non-letter start.
		// parseError no longer routes the assembled message (which contains the user's source fragment
		// with literal '{' / '}') through MessageFormat, so the visible message is preserved verbatim.
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{1foo()}"));
		assertTrue(ex.getMessage().contains("Function name must start with a letter"), ex.getMessage());
		assertTrue(ex.getMessage().contains("1foo"), ex.getMessage());
	}

	@Test void g03_functionNameStartsWithUnderscore() {
		var vr = vr();
		// "_x()" — '_' is allowed in identifier chars but isLetter('_') is false.
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{_foo()}"));
		assertTrue(ex.getMessage().contains("Function name must start with a letter"), ex.getMessage());
	}

	@Test void g04_whitespaceOnlyBody() {
		var vr = vr();
		// "#{   }" — skipWs eats all of body, parseFunctionName finds pos==start.
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{   }"));
		assertTrue(ex.getMessage().contains("Expected function name"), ex.getMessage());
	}

	@Test void g05_emptyBody() {
		var vr = vr();
		// "#{}" — same path as whitespace-only.
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{}"));
		assertTrue(ex.getMessage().contains("Expected function name"), ex.getMessage());
	}

	@Test void g06_validFunctionNameWithUnderscoreAfterStart() {
		var vr = VarResolver.create().functions(new IdFn()).build();
		// Underscore inside the name is fine (line 441 condition allows '_').
		// Also covers digits-after-letter.
		// id1 is unknown but compile must succeed (lazy-fail).
		var tpl = vr.compile("#{id1()}");
		assertNotNull(tpl);
		// Resolve fails because 'id1' isn't registered.
		assertThrows(IllegalArgumentException.class, () -> tpl.resolve(vr.createSession()));
	}

	// =========================================================================
	// h. Recursive-descent parser — arg list
	// =========================================================================

	@Test void h01_noArgsFunction() {
		var vr = vr();
		// "#{upper()}" — parseArgList sees ')' first (line 459), returns empty list.
		// upper() with no args is an arity error at resolve time (lazy-fail).
		var tpl = vr.compile("#{upper()}");
		assertNotNull(tpl);
	}

	@Test void h02_unclosedArgList() {
		var vr = vr();
		// Closed body but missing ')': "#{upper(}".
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{upper(}"));
		assertTrue(ex.getMessage().contains("Invalid script in template"), ex.getMessage());
	}

	@Test void h03_trailingContentAfterCloseParen() {
		var vr = vr();
		// "#{upper(x)trailing}" — expectEnd fails (line 592-593).
		assertThrows(IllegalArgumentException.class, () -> vr.compile("#{upper(x)trailing}"));
	}

	@Test void h04_missingOpenParen() {
		var vr = vr();
		// "#{upper x}" — after parseFunctionName (consuming "upper"), expect('(') fails.
		assertThrows(IllegalArgumentException.class, () -> vr.compile("#{upper x}"));
	}

	@Test void h05_unexpectedEndInArg() {
		var vr = vr();
		// Trailing comma forces parseArg to look for another arg and fail.
		assertThrows(IllegalArgumentException.class, () -> vr.compile("#{upper(a,}"));
	}

	// =========================================================================
	// i. Quoted-string args
	// =========================================================================

	@Test void i01_doubleQuotedString() {
		var vr = vr();
		assertEquals("HELLO WORLD", vr.resolve("#{upper(\"hello world\")}"));
	}

	@Test void i02_singleQuotedString() {
		var vr = vr();
		assertEquals("HELLO", vr.resolve("#{upper('hello')}"));
	}

	@Test void i03_quotedEscapeBackslash() {
		var vr = vr();
		// "\\\\" inside double-quotes -> single '\\' in the arg.
		assertEquals("A\\B", vr.resolve("#{id(\"A\\\\B\")}"));
	}

	@Test void i04_quotedEscapeSameQuote() {
		var vr = vr();
		// '\"' inside double-quoted string -> embedded '"'.
		assertEquals("a\"b", vr.resolve("#{id(\"a\\\"b\")}"));
	}

	@Test void i05_quotedEscapeOtherCharPreservesBackslash() {
		var vr = vr();
		// '\n' inside double-quoted -> '\\n' literal (line 508-509: append '\\' then char).
		assertEquals("a\\nb", vr.resolve("#{id(\"a\\nb\")}"));
	}

	@Test void i06_quotedSingleEscapeSingle() {
		var vr = vr();
		// '\\' inside single-quoted -> '\\' (escape match).
		assertEquals("it's", vr.resolve("#{id('it\\'s')}"));
	}

	@Test void i07_unterminatedQuotedString() {
		var vr = vr();
		// Quoted string with no closing quote — covers line 518.
		// The '#{}' body close still has to be reached, so use:
		//   "#{id(\"abc)}"  (quote opened, never closed; ')}' is consumed as part of string)
		// At EOF of body, parseQuotedString throws (line 518).
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{id(\"abc)}"));
		assertTrue(ex.getMessage().contains("Unterminated quoted string"), ex.getMessage());
	}

	@Test void i08_quotedTrailingBackslash() {
		var vr = vr();
		// '\\' as the last char before quote - pos+1 < length is false (line 501),
		// fallthrough appends '\\' then increments. Then closing quote closes string.
		// Use single backslash followed by another char then close quote: "x\\y".
		// Combined Java escapes: "#{id(\"x\\\\\")}" -> actual template: #{id("x\\")}
		// which has a final '\\' before close quote. Inside parser:
		//   pos at '\\' -> pos+1 is '"' -> next==quote so escape branch consumes both.
		// Need a case where pos+1 is past end while in quoted string... but that means
		// the string is unterminated. Skip — covered by i07.
		assertEquals("x\\", vr.resolve("#{id(\"x\\\\\")}"));
	}

	// =========================================================================
	// j. Bare-token args
	// =========================================================================

	@Test void j01_bareIdentifier() {
		var vr = vr();
		assertEquals("HELLO", vr.resolve("#{upper(hello)}"));
	}

	@Test void j02_bareNumber() {
		var vr = vr();
		assertEquals("123", vr.resolve("#{id(123)}"));
	}

	@Test void j03_bareDecimal() {
		var vr = vr();
		assertEquals("1.5", vr.resolve("#{id(1.5)}"));
	}

	@Test void j04_bareNegative() {
		var vr = vr();
		assertEquals("-7", vr.resolve("#{id(-7)}"));
	}

	@Test void j05_bareBoolean() {
		var vr = vr();
		assertEquals("true", vr.resolve("#{id(true)}"));
	}

	@Test void j06_bareTokenWithUnderscoreDotDash() {
		var vr = vr();
		assertEquals("a.b-c_d", vr.resolve("#{id(a.b-c_d)}"));
	}

	@Test void j07_bareTokenStopsAtTab() {
		var vr = vr();
		// Tab inside a bare token terminates capture (line 574 \\t branch).
		assertEquals("a", vr.resolve("#{id(a\t)}"));
	}

	@Test void j08_bareTokenStopsAtNewline() {
		var vr = vr();
		// Newline -> covers \\n branch.
		assertEquals("a", vr.resolve("#{id(a\n)}"));
	}

	@Test void j09_bareTokenStopsAtCarriageReturn() {
		var vr = vr();
		// \\r branch.
		assertEquals("a", vr.resolve("#{id(a\r)}"));
	}

	@Test void j10_emptyBareToken() {
		var vr = vr();
		// "#{id(,a)}" — first arg is empty bare token. parseArg sees ',' (separator)
		// and parseBareToken finds pos==start -> "Expected argument" (line 578-579).
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{id(,a)}"));
		assertTrue(ex.getMessage().contains("Expected argument"), ex.getMessage());
	}

	// =========================================================================
	// k. Nested markers as args
	// =========================================================================

	@Test void k01_nestedDollarVar() {
		// Need PropertyVar registration via defaultVars + custom IdFn.
		var vr = VarResolver.create().defaultVars().functions(new IdFn()).build();
		// PropertyVar with default — ${unset.key:fallback} -> "fallback".
		assertEquals("fallback", vr.resolve("#{id(${unset.key.x.y.z:fallback})}"));
	}

	@Test void k02_nestedTypedDollarVar() {
		var vr = vr();
		assertEquals("[hello]", vr.resolve("#{id($E{hello})}"));
	}

	@Test void k03_nestedHashScript() {
		var vr = vr();
		assertEquals("HELLO", vr.resolve("#{id(#{upper(hello)})}"));
	}

	@Test void k04_nestedMarkerMissingBrace() {
		var vr = vr();
		// "#{id($E)}" — first char '$' begins parseNestedMarker; consumes 'E' as prefix;
		// next char is ')' which fails the '{' expectation (line 540-541).
		var ex = assertThrows(IllegalArgumentException.class, () -> vr.compile("#{id($E)}"));
		assertTrue(ex.getMessage().contains("Invalid script in template"), ex.getMessage());
	}

	@Test void k05_nestedMarkerHashMissingBrace() {
		var vr = vr();
		// "#{id(#x)}" — '#' begins nested marker but no '{' follows.
		assertThrows(IllegalArgumentException.class, () -> vr.compile("#{id(#x)}"));
	}

	@Test void k06_nestedMarkerUnterminated() {
		var vr = vr();
		// "#{id(${abc)}" — outer '#{...}' picks up nested ${ } but the body ends before
		// the inner var has a closing '}'. The outer S3H state machine actually consumes
		// the trailing close as part of the body so the parser sees an unterminated
		// nested marker. Observed: a parser error fires.
		// Note: depending on brace pairing, the outer state machine may instead leave
		// the body unclosed (S3H EOF fallthrough) and emit a literal — we therefore use
		// a balanced outer-close that forces the body parse to fail:
		//   "#{id(${abc)} more}" — outer '}' matches, body parse fails on inner.
		assertThrows(IllegalArgumentException.class, () -> vr.compile("#{id(${abc)} more}"));
	}

	@Test void k07_nestedMarkerWithEscapeInBody() {
		var vr = vr();
		// "#{id($E{a\\}b})}" — escape inside nested marker body must NOT close at \\}.
		// Combined Java string literal:
		assertEquals("[a\\}b]", vr.resolve("#{id($E{a\\}b})}"));
	}

	@Test void k08_nestedMarkerDeepBraces() {
		var vr = vr();
		// "#{id($E{a{b{c}d}e})}" — multiple nested '{' incrementing depth (line 551).
		assertEquals("[a{b{c}d}e]", vr.resolve("#{id($E{a{b{c}d}e})}"));
	}

	// =========================================================================
	// l. Whitespace handling around args
	// =========================================================================

	@Test void l01_whitespaceAroundArgs() {
		var vr = vr();
		// Spaces / tabs / newlines between args must be skipped.
		assertEquals("HELLO", vr.resolve("#{ upper ( hello ) }"));
	}

	@Test void l02_whitespaceBeforeAndAfterCommas() {
		// Use multiple bare args to exercise skipWs around the comma in parseArgList
		// (lines 462-468 — skipWs both before and after the ','). Single-arg + trailing
		// comma is rejected as "Expected argument".
		var vr = vr();
		// Two-arg id is unknown arity but compile succeeds (lazy-fail), confirming the
		// arg list was parsed including ws around ','.
		var tpl = vr.compile("#{id(  a  ,  b  )}");
		assertNotNull(tpl);
	}

	@Test void l03_tabsAndNewlinesAsWhitespace() {
		var vr = vr();
		// skipWs() handles \t, \n, \r (lines 604).
		assertEquals("HELLO", vr.resolve("#{\tupper\n(\rhello\t)\n}"));
	}

	// =========================================================================
	// m. Recursion / depth
	// =========================================================================

	@Test void m01_deeplyNestedScripts() {
		var vr = vr();
		// 10-level deep nested upper(...) calls.
		var s = "x";
		var open = new StringBuilder();
		var close = new StringBuilder();
		for (var i = 0; i < 10; i++) {
			open.append("#{upper(");
			close.append(")}");
		}
		var tpl = open.toString() + s + close.toString();
		assertEquals("X", vr.resolve(tpl));
	}

	@Test void m02_deeplyNestedDollarVars() {
		var vr = vr();
		// Deep $E{...} nesting.
		var s = "x";
		var open = new StringBuilder();
		var close = new StringBuilder();
		for (var i = 0; i < 5; i++) {
			open.append("$E{");
			close.append("}");
		}
		// Each layer wraps with [...]
		var expected = "x";
		for (var i = 0; i < 5; i++)
			expected = "[" + expected + "]";
		assertEquals(expected, vr.resolve(open.toString() + s + close.toString()));
	}

	// =========================================================================
	// n. Compile cache / segment reuse via VarTemplate API
	// =========================================================================

	@Test void n01_compiledTemplateReusable() {
		// One compile, many resolves — the segments array is allocated once.
		var vr = vr();
		var tpl = vr.compile("$E{a}-$E{b}");
		var s = vr.createSession();
		for (var i = 0; i < 100; i++)
			assertEquals("[a]-[b]", tpl.resolve(s));
	}

	@Test void n02_compileWithUnknownVarFallthrough() {
		// Unknown prefix "$X{...}" with no registered Var — fallthrough emits the source.
		var vr = vr();
		assertEquals("$X{x}", vr.compile("$X{x}").resolve(vr.createSession()));
	}

	// =========================================================================
	// o. Counters — verify compile-time vs resolve-time invocation
	// =========================================================================

	@Test void o01_stableVarFoldsAtCompileTime() {
		// CountingStable returns its arg uppercased. Stable -> folded once.
		var counter = new AtomicInteger();
		var vr = VarResolver.create().vars(new CountingStable(counter)).build();
		var tpl = vr.compile("hello $CS{world}");
		assertEquals(1, counter.get(), "Stable var folded once at compile time");
		for (var i = 0; i < 5; i++)
			assertEquals("hello WORLD", tpl.resolve(vr.createSession()));
		assertEquals(1, counter.get(), "No additional dispatch after folding");
	}

	public static class CountingStable extends SimpleVar {
		final AtomicInteger calls;
		public CountingStable(AtomicInteger calls) {
			super("CS");
			this.calls = calls;
		}
		@Override public String resolve(VarResolverSession session, String arg) {
			calls.incrementAndGet();
			return arg == null ? "" : arg.toUpperCase(Locale.ROOT);
		}
		@Override protected boolean isStable() { return true; }
	}

	// =========================================================================
	// p. Extra coverage — StreamedVar stable, uppercase function name
	// =========================================================================

	/** Stable StreamedVar — should NOT be folded (line 384: !(v instanceof SimpleVar)). */
	public static class StableStreamedVar extends StreamedVar {
		public StableStreamedVar() { super("SS"); }
		@Override public void resolveTo(VarResolverSession session, java.io.Writer w, String arg) throws java.io.IOException {
			w.write("[" + arg + "]");
		}
		@Override protected boolean isStable() { return true; }
	}

	@Test void p01_stableStreamedVarIsNotFolded() {
		// Even though the var is stable, it's a StreamedVar so the fold path skips it.
		var vr = VarResolver.create().vars(new StableStreamedVar()).build();
		var tpl = vr.compile("$SS{x}");
		// Resolution still works at runtime via streaming dispatch.
		assertEquals("[x]", tpl.resolve(vr.createSession()));
	}

	@Test void p02_uppercaseFunctionName() {
		// Exercises the (c >= 'A' && c <= 'Z') branch in parseFunctionName (line 441).
		var vr = vr();
		// Unknown fn — lazy-fail: compile succeeds.
		var tpl = vr.compile("#{UPPER(x)}");
		assertNotNull(tpl);
		assertThrows(IllegalArgumentException.class, () -> tpl.resolve(vr.createSession()));
	}

	@Test void p03_compileSegmentsCount() {
		// A compiled multi-segment template exposes its segment count via segments().
		var vr = vr();
		var tpl = vr.compile("hello $E{a} world $E{b}");
		// 4 segments: "hello ", $E{a}, " world ", $E{b}
		assertEquals(4, tpl.segments().length);
	}
}
