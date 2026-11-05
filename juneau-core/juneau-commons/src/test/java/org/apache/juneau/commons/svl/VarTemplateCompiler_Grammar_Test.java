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
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.vars.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Tests the {@code ${Type:key[:default]}} / {@code #{expr}} grammar of {@link VarTemplateCompiler} (J0439 Task 3.2).
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class VarTemplateCompiler_Grammar_Test extends TestBase {

	private static final Map<String,String> DATA = Map.of("k1", "v1", "k2", "v2", "b:c", "bc", "x:y", "xy", "flag", "yes", "a,b", "comma", "'", "quote", "}", "brace", "\"", "dq");

	/** Map-backed defaulting var; an empty key resolves to {@code EMPTY}. */
	public static class MapVar extends DefaultingVar {
		public MapVar(String name) { super(name); }
		@Override public String resolve(VarResolverSession session, String key) { return key.isEmpty() ? "EMPTY" : DATA.get(key); }
	}

	/** Map-backed multipart-resolving var (first non-null key wins). */
	public static class MultiResolving extends MultipartResolvingVar {
		public MultiResolving(String name) { super(name); }
		@Override public String resolve(VarResolverSession session, String key) { return DATA.get(key); }
	}

	/** Multipart var that joins its parts with {@code |}. */
	public static class Multi extends MultipartVar {
		public Multi() { super("ML"); }
		@Override public String resolve(VarResolverSession session, String[] args) { return String.join("|", args); }
	}

	/** Plain var echoing its whole body in brackets. */
	public static class Echo extends SimpleVar {
		public Echo(String name) { super(name); }
		@Override public String resolve(VarResolverSession session, String arg) { return "[" + arg + "]"; }
	}

	/** Plain var returning HTML markup around its body. */
	public static class Html extends SimpleVar {
		public Html(String name) { super(name); }
		@Override public String resolve(VarResolverSession session, String arg) { return "<b>" + arg + "</b>"; }
	}

	/** Var whose typed value is not a String: a list holding markup, or a number. */
	public static class Typed extends DefaultingVar {
		public Typed(String name) { super(name); }
		@Override public String resolve(VarResolverSession session, String key) { return key.equals("n") ? "42" : "[<i>]"; }
		@Override protected Object resolveValue(VarResolverSession session, String key) { return key.equals("n") ? (Object)42 : List.of("<i>"); }
	}

	/** Defaulting var that never re-resolves its output. */
	public static class NoRecurse extends MapVar {
		public NoRecurse() { super("NR"); }
		@Override protected boolean allowRecurse() { return false; }
	}

	/** Stable var for folding. */
	public static class Stable extends SimpleVar {
		public Stable() { super("ST"); }
		@Override public String resolve(VarResolverSession session, String arg) { return "<" + arg + ">"; }
		@Override protected boolean isStable() { return true; }
	}

	private static VarResolver.Builder base() {
		return VarResolver.create().defaultFunctions().vars(
			new PropertyVar(), new SystemPropertiesVar(), new MapVar("A"), new MapVar("B"), new MapVar("C"),
			new MultiResolving("MR"), new Multi(), new Echo("O"), new Stable(),
			new Html("RQ"), new Html("W"), new Html("H"), new Typed("TV"), new NoRecurse());
	}

	private static final VarResolver LENIENT = base().build();
	private static final VarResolver STRICT = base().strict(true).build();

	private static String r(String s) { return LENIENT.resolve(s); }
	private static String rs(String s) { return STRICT.resolve(s); }

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal("VTCG.k");
		Settings.get().unsetGlobal("name");
	}

	//====================================================================================================
	// Item 1: registered prefix is a lookup, otherwise the whole body is the P shortcut.
	//====================================================================================================

	@Test void a01_registeredPrefixIsLookup() {
		assertEquals("v1", r("${A:k1}"));
		assertEquals("v1", rs("${A:k1}"));
	}

	@Test void a02_unregisteredPrefixIsPropertyShortcut() {
		assertEquals("k", r("${Zz:k}"));
		assertEquals("k", rs("${Zz:k}"));
	}

	@Test void a03_lowercaseOrNonLetterPrefixIsShortcut() {
		assertEquals("dflt", r("${a.b:dflt}"));
		assertEquals("dflt", r("${A1:dflt}"));
	}

	@Test void a04_prefixNamesArePreservedOnSegment() {
		var segs = LENIENT.compile("${A:k1}").segments();
		assertInstanceOf(VarRefSegment.class, segs[0]);
		assertInstanceOf(MapVar.class, ((VarRefSegment)segs[0]).cachedVar());
	}

	//====================================================================================================
	// Item 2: colon split per var class, translated to the internal comma form.
	//====================================================================================================

	@Test void b01_defaultingVar_firstColonSplitsKeyFromDefault() {
		assertEquals("v1", r("${A:k1:dflt}"));
		assertEquals("dflt", r("${A:nope:dflt}"));
		assertEquals("a:b", r("${A:nope:a:b}"));
		assertEquals("x,y", r("${A:nope:x,y}"));
	}

	@Test void b02_multipartResolvingVar_eachColonIsAKey() {
		assertEquals("v1", r("${MR:k1}"));
		assertEquals("v2", r("${MR:nope:k2:k1}"));
		assertEquals("", r("${MR:nope:nope2}"));
	}

	@Test void b03_multipartVar_eachColonIsAPart() {
		assertEquals("a|b|c", r("${ML:a:b:c}"));
		assertEquals("a", r("${ML:a}"));
	}

	@Test void b04_otherVar_bodyIsWhole() {
		assertEquals("[a:b,c]", r("${O:a:b,c}"));
	}

	@Test void b05_commaInKeyPart_strictThrowsLenientPassesThrough() {
		assertEquals("x ${A:a,b} y", r("x ${A:a,b} y"));
		assertEquals("${ML:a,b:c}", r("${ML:a,b:c}"));
		assertEquals("${MR:a:b,c}", r("${MR:a:b,c}"));
		var e = assertThrows(ExprException.class, () -> rs("x ${A:a,b} y"));
		assertEquals(2, e.getPos());
		assertTrue(e.getMessage().contains("','"), e.getMessage());
		assertThrows(ExprException.class, () -> rs("${ML:a,b:c}"));
	}

	@Test void b06_commaInsideNestedKeyIsNotAKeyComma() {
		assertEquals("v1", r("${A:${Zz:k1}}"));
	}

	//====================================================================================================
	// Escapes and quoting.
	//====================================================================================================

	@Test void c01_escapedColonIsLiteral() {
		assertEquals("bc", r("${A:b\\:c}"));
		assertEquals("a:b", r("${A:nope:a\\:b}"));
	}

	@Test void c02_quotedKeyIsLiteral() {
		assertEquals("xy", r("${A:'x:y'}"));
		assertEquals("xy", r("${A:\"x:y\"}"));
		assertEquals("xy", r("${MR:nope:'x:y'}"));
	}

	@Test void c03_escapedDollarStaysLiteral() {
		assertEquals("${A:k1}", r("\\${A:k1}"));
		assertEquals("#{1+2}", r("\\#{1+2}"));
	}

	//====================================================================================================
	// Escapes: a backslash before ' " , } : is removed before the var sees the key, part or default.
	//====================================================================================================

	@Test void c04_escapedChars_inDefault() {
		assertEquals("'", r("${A:nope:\\'}"));
		assertEquals("\"", r("${A:nope:\\\"}"));
		assertEquals("a,b", r("${A:nope:a\\,b}"));
		assertEquals("}", r("${A:nope:\\}}"));
		assertEquals("a:b", r("${A:nope:a\\:b}"));
		assertEquals("\\", r("${A:nope:\\\\}"));
		assertEquals("'", rs("${A:nope:\\'}"));
		assertEquals("a,b", rs("${A:nope:a\\,b}"));
	}

	@Test void c05_escapedChars_inKey() {
		assertEquals("quote", r("${A:\\'}"));
		assertEquals("dq", r("${A:\\\"}"));
		assertEquals("comma", r("${A:a\\,b}"));
		assertEquals("comma", rs("${A:a\\,b:dflt}"));
		assertEquals("brace", r("${A:\\}}"));
		assertEquals("bc", r("${A:b\\:c}"));
	}

	@Test void c06_escapedChars_inMultipartParts() {
		assertEquals("a,b|c", r("${ML:a\\,b:c}"));
		assertEquals("'|\"|}|:|,", r("${ML:\\':\\\":\\}:\\::\\,}"));
		assertEquals("a,b|c", rs("${ML:a\\,b:c}"));
		assertEquals("comma", r("${MR:nope:a\\,b}"));
		assertEquals("quote", r("${MR:nope:\\'}"));
		assertEquals("brace", rs("${MR:nope:\\}}"));
	}

	@Test void c07_escapedChars_inSimpleVar() {
		assertEquals("[a,b]", r("${O:a\\,b}"));
		assertEquals("[']", r("${O:\\'}"));
		assertEquals("[}]", r("${O:\\}}"));
		assertEquals("[\"]", rs("${O:\\\"}"));
		assertEquals("[a:b]", r("${O:a\\:b}"));
	}

	@Test void c08_escapedChars_insideExpression() {
		assertEquals("quote", r("#{${A:\\'}}"));
		assertEquals("comma", r("#{${A:a\\,b}}"));
		assertEquals("x,y", r("#{${A:nope:x\\,y}}"));
		assertEquals("brace", r("#{${A:\\}}}"));
	}

	@Test void c09_unescapedCommaInKeyStillRejected() {
		assertEquals("${A:a,b}", r("${A:a,b}"));
		assertThrows(ExprException.class, () -> rs("${A:a,b}"));
		assertEquals("x,y", r("${A:nope:x,y}"));
	}

	//====================================================================================================
	// Nesting.
	//====================================================================================================

	@Test void d01_nestedVarInBody() {
		Settings.get().setGlobal("VTCG.k", "found");
		assertEquals("found", r("${P:${Q:VTCG.k}}"));
		assertEquals("v1", r("${A:${B:nope:k1}}"));
	}

	@Test void d02_nestedVarInDefault() {
		assertEquals("v2", r("${A:nope:${B:k2}}"));
	}

	@Test void d03_bracesOfEmbeddedVarDoNotEndExpression() {
		assertEquals("v1", r("#{true ? ${A:k1} : ${B:k2}}"));
		assertEquals("v2", r("#{false ? ${A:k1} : ${B:k2}}"));
		assertEquals("v1", r("#{${A:flag} == 'yes' ? ${A:k1:x} : ${B:y}}"));
		assertEquals("[v1]", r("[#{${A:k1}}]"));
	}

	@Test void d04_quotedBraceInExpression() {
		assertEquals("}a", r("#{'}' + 'a'}"));
	}

	//====================================================================================================
	// #{...} expressions.
	//====================================================================================================

	@Test void e01_expressionSegment() {
		assertEquals("3", r("#{1+2}"));
		assertEquals("a3b", r("a#{1+2}b"));
	}

	@Test void e02_constantExpressionFolds() {
		var segs = LENIENT.compile("#{1+2}").segments();
		assertEquals(1, segs.length);
		assertInstanceOf(LiteralSegment.class, segs[0]);
		assertEquals("3", ((LiteralSegment)segs[0]).text);
	}

	@Test void e03_nonConstantExpressionIsExprSegment() {
		assertInstanceOf(ExprSegment.class, LENIENT.compile("#{${A:k1}}").segments()[0]);
	}

	@Test void e04_parseError_strictThrowsLenientLiteral() {
		assertEquals("x #{1 +} y", r("x #{1 +} y"));
		assertThrows(ExprException.class, () -> rs("x #{1 +} y"));
	}

	@Test void e05_maxExprDepthIsThreaded() {
		var shallow = base().maxExprDepth(2).strict(true).build();
		assertEquals("6", rs("#{1+(2+(3))}"));
		var e = assertThrows(ExprException.class, () -> shallow.resolve("#{((1+(2+(3))))}"));
		assertTrue(e.getMessage().contains("nested too deeply"), e.getMessage());
		var shallowLenient = base().maxExprDepth(2).build();
		assertEquals("#{((1+(2+(3))))}", shallowLenient.resolve("#{((1+(2+(3))))}"));
	}

	//====================================================================================================
	// Unterminated markers.
	//====================================================================================================

	@Test void f01_unterminatedDollarBrace() {
		assertEquals("a ${A:k1", r("a ${A:k1"));
		var e = assertThrows(ExprException.class, () -> rs("a ${A:k1"));
		assertEquals(2, e.getPos());
	}

	@Test void f02_unterminatedHashBrace() {
		assertEquals("a #{1+2", r("a #{1+2"));
		var e = assertThrows(ExprException.class, () -> rs("a #{1+2"));
		assertEquals(2, e.getPos());
	}

	//====================================================================================================
	// Empty forms (spec section 2.2).
	//====================================================================================================

	@Test void g01_emptyForms() {
		// Empty key: the var's own empty-arg behavior.  P (Settings) rejects an empty name.
		assertThrows(VarResolverException.class, () -> r("${}"));
		assertThrows(VarResolverException.class, () -> r("${:}"));
		assertThrows(VarResolverException.class, () -> r("${P:}"));
		assertEquals("EMPTY", r("${A:}"));
		assertEquals("", r("${A:nope:}"));
		assertEquals("v1", r("${A:k1:}"));
		// The real S var's own empty-key behavior: System.getProperty("") throws.
		assertThrows(VarResolverException.class, () -> r("${S:}"));
		assertEquals("", r("${S:VTCG.absent:}"));
	}

	@Test void g03_emptyFormErrorsUseNewSyntax() {
		var e = assertThrows(VarResolverException.class, () -> r("${}"));
		assertTrue(e.getMessage().contains("'${}'"), e.getMessage());
		assertFalse(e.getMessage().contains("$P{"), e.getMessage()); // svl-migrate:skip (deliberate legacy-syntax fixture)
		e = assertThrows(VarResolverException.class, () -> r("${S:}"));
		assertTrue(e.getMessage().contains("'${S:}'"), e.getMessage());
		assertFalse(e.getMessage().contains("$S{"), e.getMessage()); // svl-migrate:skip (deliberate legacy-syntax fixture)
	}

	@Test void g02_emptyExpression() {
		assertEquals("#{ }", r("#{ }"));
		assertEquals("#{}", r("#{}"));
		assertThrows(ExprException.class, () -> rs("#{ }"));
	}

	//====================================================================================================
	// Item 4: legacy $X{...}. // svl-migrate:skip (deliberate legacy-syntax fixture)
	//====================================================================================================

	@Test void h01_legacyLenientIsLiteral() {
		assertEquals("x $A{k1} y", r("x $A{k1} y")); // svl-migrate:skip (deliberate legacy-syntax fixture)
		assertEquals("$A{k1,dflt}", r("$A{k1,dflt}")); // svl-migrate:skip (deliberate legacy-syntax fixture)
		assertEquals("$A{$B{k1}}", r("$A{$B{k1}}")); // svl-migrate:skip (deliberate legacy-syntax fixture)
	}

	@Test void h02_legacyStrictThrows() {
		var e = assertThrows(VarResolverException.class, () -> rs("x $A{k1} y")); // svl-migrate:skip (deliberate legacy-syntax fixture)
		assertTrue(e.getMessage().contains("Legacy variable syntax $X{...} is no longer supported; use ${X:...}"), e.getMessage()); // svl-migrate:skip (deliberate legacy-syntax fixture)
		assertInstanceOf(ExprException.class, e);
		assertEquals(2, ((ExprException)e).getPos());
	}

	@Test void h03_legacyInsideNewBody() {
		assertThrows(VarResolverException.class, () -> rs("${A:$B{k1}}")); // svl-migrate:skip (deliberate legacy-syntax fixture)
	}

	@Test void h04_falseTriggersStayLiteral() {
		assertEquals("$A $ $1 cost", r("$A $ $1 cost"));
	}

	//====================================================================================================
	// Item 5: HTML-doc passthrough and script/style refusal keep working on prefix names.
	//====================================================================================================

	@Test void i01_scriptStyleRefusesRequestVars() {
		var s = LENIENT.createSession();
		assertEquals("${RQ:x}", s.resolveScriptStyle("${RQ:x}"));
		assertEquals("<b>x</b>", s.resolveScriptStyle("${W:x}"));
		assertEquals("<b>x</b>", s.resolve("${RQ:x}"));
	}

	@Test void i02_scriptStyleRefusesRequestVarsInExpressions() {
		var s = LENIENT.createSession();
		assertEquals("", s.resolveScriptStyle("#{${RQ:x}}"));
		assertThrows(ExprException.class, () -> STRICT.createSession().resolveScriptStyle("#{${RQ:x}}"));
	}

	@Test void i03_htmlDocPassthroughVars() {
		var s = LENIENT.createSession();
		assertEquals("<b>x</b>", s.resolveHtmlDoc("${W:x}"));
		assertEquals("&lt;b&gt;x&lt;/b&gt;", s.resolveHtmlDoc("${H:x}"));
	}

	@Test void i04_htmlDocEncodesExpressionOutput() {
		var s = LENIENT.createSession();
		assertEquals("&lt;b&gt;x&lt;/b&gt;", s.resolveHtmlDoc("#{${H:x}}"));
		assertEquals("<b>x</b>", s.resolve("#{${H:x}}"));
	}

	//====================================================================================================
	// Item 6: the P core shortcut.
	//====================================================================================================

	@Test void j01_propertyShortcut() {
		Settings.get().setGlobal("name", "val");
		assertEquals("val", r("${name}"));
		assertEquals("val", r("${name:def}"));
		Settings.get().unsetGlobal("name");
		assertEquals("", r("${name}"));
		assertEquals("def", r("${name:def}"));
		assertEquals("EMPTY", r("${C:}"));
		assertEquals("v1", r("${C:k1}"));
		assertEquals("y:z", r("${x:y:z}"));
		// Unique key: a shared name like "a" is set globally by other tests (CI saw "a1").
		assertEquals("b", r("${P:j01.unsetProp:b}"));
	}

	//====================================================================================================
	// Stable folding on the new VarRefSegment.
	//====================================================================================================

	@Test void k01_stableVarFolds() {
		var segs = LENIENT.compile("${ST:k}").segments();
		assertInstanceOf(LiteralSegment.class, segs[0]);
		assertEquals("<k>", ((LiteralSegment)segs[0]).text);
	}

	@Test void k02_nestedLiveBodyDoesNotFold() {
		assertInstanceOf(VarRefSegment.class, LENIENT.compile("${ST:${A:k1}}").segments()[0]);
		assertEquals("<v1>", r("${ST:${A:k1}}"));
	}

	//====================================================================================================
	// Scanner agreement (one rule set for the template compiler, VarSplit and the expression lexer).
	//====================================================================================================

	@Test void l01_apostropheInVarInsideExpression() {
		assertEquals("false", r("#{ ${S:it's} == 'x' }"));
		assertEquals("true", r("#{ ${A:nope:it's} == 'it''s' }"));
		assertEquals("true", rs("#{ ${A:nope:it's} == 'it''s' }"));
	}

	@Test void l02_midWordApostropheDoesNotQuote() {
		assertEquals("don't|b", r("${ML:don't:b}"));
		assertEquals("it's|b", r("${ML:it's:b}"));
		assertEquals("<b>don't:b</b>", r("${RQ:don't:b}"));
		assertEquals("it's", r("${A:nope:it's}"));
	}

	@Test void l03_backslashQuoteInExpressionString() {
		// SpEL strings have no backslash escape: 'a\' is a complete string, so the trailing ' runs to the end.
		assertEquals(-1, VarSplit.closeOfExpr("'a\\'b'}", 0));
		assertThrows(ExprException.class, () -> ExprParser.lex("'a\\'b'"));
		assertEquals("#{'a\\'b'}", r("#{'a\\'b'}"));
		var e = assertThrows(ExprException.class, () -> rs("#{'a\\'b'}"));
		assertTrue(e.getMessage().contains("Unterminated #{"), e.getMessage());
	}

	@Test void l04_plainTextApostrophesUnaffected() {
		assertEquals("don't v1 it's", r("don't ${A:k1} it's"));
		assertEquals("it's #{1+2}'", r("it's \\#{1+2}'"));
		assertEquals("'3'", r("'#{1+2}'"));
	}

	@Test void l05_partStartQuoteStillQuotes() {
		assertEquals("xy", r("${A:'x:y'}"));
		// A quoted default groups (its ':' and '}' are not separators) but is passed to the var verbatim.
		assertEquals("'a:b}'", r("${A:nope:'a:b}'}"));
		assertEquals("'}'", r("#{${A:nope:'}'}}"));
		assertEquals("brace", r("#{${MR:'}':k1}}"));
	}

	//====================================================================================================
	// HTML-doc chrome inside #{...}: leaf ${...} values are encoded once, as the old #{fn(...)} args were.
	//====================================================================================================

	@Test void m01_htmlDocLeafFromNormalVar() {
		var s = LENIENT.createSession();
		assertEquals("&lt;b&gt;x&lt;/b&gt;&lt;b&gt;y&lt;/b&gt;", s.resolveHtmlDoc("#{${H:x} + ${H:y}}"));
		assertEquals("&lt;b&gt;x&lt;/b&gt;", s.resolveHtmlDoc("#{${H:x}}"));
	}

	@Test void m02_htmlDocLeafFromPassthroughVar() {
		var s = LENIENT.createSession();
		assertEquals("<b>x</b>", s.resolveHtmlDoc("#{${W:x}}"));
		assertEquals("<b>x</b>&lt;b&gt;y&lt;/b&gt;", s.resolveHtmlDoc("#{${W:x} + ${H:y}}"));
	}

	@Test void m03_htmlDocLeafFromNonStringVar() {
		var s = LENIENT.createSession();
		assertEquals("[&lt;i&gt;]", s.resolveHtmlDoc("${TV:x}"));
		assertEquals("[&lt;i&gt;]", s.resolveHtmlDoc("#{${TV:x}}"));
		assertEquals("[\"<i>\"]", s.resolve("#{${TV:x}}"));
		// Numbers and booleans cannot carry markup and stay typed.
		assertEquals("43", s.resolveHtmlDoc("#{${TV:n} + 1}"));
	}

	@Test void m04_htmlDocExpressionLiteralsAreNotEncoded() {
		// As with the old #{fn(...)} output, only leaf ${...} values are encoded, not literals or results.
		assertEquals("<p>", LENIENT.createSession().resolveHtmlDoc("#{'<p>'}"));
	}

	//====================================================================================================
	// Script/style and HTML-doc guards on ${...} inside #{...} (ExprEvaluator.varRef -> resolveValue).
	//====================================================================================================

	@Test void n01_scriptStyleRefusesInsideExpression() {
		var s = LENIENT.createSession();
		assertEquals("d", s.resolveScriptStyle("#{${RQ:x} ?: 'd'}"));
		assertEquals("<b>x</b>", s.resolveScriptStyle("#{${W:x}}"));
		assertEquals("<b>x</b>", s.resolve("#{${RQ:x} ?: 'd'}"));
		var e = assertThrows(ExprException.class, () -> STRICT.createSession().resolveScriptStyle("#{${RQ:x} ?: 'd'}"));
		assertTrue(e.getMessage().contains("RQ"), e.getMessage());
	}

	@Test void n02_htmlDocEncodesOrBypassesInsideExpression() {
		var s = LENIENT.createSession();
		assertEquals("&lt;b&gt;x&lt;/b&gt;", s.resolveHtmlDoc("#{${RQ:x}}"));
		assertEquals("<b>x</b>", s.resolveHtmlDoc("#{${W:x}}"));
		assertEquals("<b>x</b>", s.resolve("#{${RQ:x}}"));
	}

	//====================================================================================================
	// Nesting depth guard on ${...} bodies.
	//====================================================================================================

	@Test void o01_deepNestingStrictIsPositionedError() {
		var t = "${S:y:".repeat(3000) + "z" + "}".repeat(3000);
		var e = assertThrows(ExprException.class, () -> rs(t));
		assertTrue(e.getMessage().contains("nested too deeply"), e.getMessage());
		assertEquals(6 * VarResolver.DEFAULT_MAX_EXPR_DEPTH, e.getPos());
	}

	@Test void o02_deepNestingLenientIsLiteral() {
		var d = VarResolver.DEFAULT_MAX_EXPR_DEPTH;
		var t = "${NR:nope:".repeat(3000) + "z" + "}".repeat(3000);
		var out = assertDoesNotThrow(() -> r(t));
		assertEquals("${NR:nope:".repeat(3000 - d) + "z" + "}".repeat(3000 - d), out);
	}

	@Test void o05_hugeLeftAssociativeChainNeverRawStackOverflow() {
		var t = "#{1" + "+1".repeat(20000) + "}";
		var e = assertThrows(ExprException.class, () -> rs(t));
		assertTrue(e.getMessage().contains("too complex") || e.getMessage().contains("nested too deeply"), e.getMessage());
		var out = assertDoesNotThrow(() -> r(t));
		assertEquals(t, out);
	}

	@Test void o06_chainJustOverCapStrictThrowsLenientLiteral() {
		var over = "#{1" + "+1".repeat(ExprParser.MAX_OPERATORS + 1) + "}";
		var e = assertThrows(ExprException.class, () -> rs(over));
		assertTrue(e.getMessage().contains("too complex"), e.getMessage());
		assertEquals(over, assertDoesNotThrow(() -> r(over)));
		var at = "#{1" + "+1".repeat(ExprParser.MAX_OPERATORS) + "}";
		assertEquals(String.valueOf(ExprParser.MAX_OPERATORS + 1), rs(at));
	}

	@Test void o07_precompiledHugeChainRendersLiteralLenient() {
		var t = "x#{1" + "+1".repeat(20000) + "}y";
		var tpl = assertDoesNotThrow(() -> LENIENT.compile(t));
		assertEquals(t, assertDoesNotThrow(() -> tpl.resolve(LENIENT.createSession())));
		assertEquals(t, assertDoesNotThrow(() -> tpl.resolve(LENIENT.createSession())));
	}

	@Test void o04_deepRuntimeNestingThroughExpressions() {
		// ${...} inside #{...} is compiled lazily at evaluation; the session caps that chain too.
		var t = "#{${A:nope:".repeat(3000) + "1" + "}}".repeat(3000);
		var e = assertThrows(VarResolverException.class, () -> rs(t));
		assertTrue(e.getMessage().contains("nested too deeply"), e.getMessage());
		assertDoesNotThrow(() -> r(t));
		var d = VarResolver.DEFAULT_MAX_EXPR_DEPTH;
		assertEquals("1", rs("#{${A:nope:".repeat(d) + "1" + "}}".repeat(d)));
	}

	@Test void o03_nestingAtLimitResolves() {
		var d = VarResolver.DEFAULT_MAX_EXPR_DEPTH;
		assertEquals("z", rs("${A:nope:".repeat(d) + "z" + "}".repeat(d)));
	}

	//====================================================================================================
	// Lenient fall-through text is unescaped the same way on every path.
	//====================================================================================================

	@Test void p01_fallthroughUnescapedConsistently() {
		// Unterminated, parse error, unknown-in-key and legacy paths all strip \$ \{ \} escapes like the old compiler.
		assertEquals("#{1 }", r("#{1 \\}"));
		assertEquals("#{1 + \\x}", r("#{1 + \\x}"));
		assertEquals("#{1 + }}", r("#{1 + \\}}"));
		assertEquals("$A{k}1}", r("$A{k\\}1}")); // svl-migrate:skip (deliberate legacy-syntax fixture)
	}
}
