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

/** Tests for {@link ExprSegment}. */
class ExprSegment_Test extends TestBase {

	/** Function returning fixed text containing a var marker. */
	public static class Marker implements VarFunction {
		@Override public String name() { return "marker"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "${x}"; }
	}

	/** Function returning a list of three elements. */
	public static class Three implements VarFunction {
		@Override public String name() { return "three"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return "3"; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return List.of(1L, 2L, 3L); }
	}

	/** Function returning null. */
	public static class Nothing implements VarFunction {
		@Override public String name() { return "nothing"; }
		@Override public int minArity() { return 0; }
		@Override public int maxArity() { return 0; }
		@Override public String invoke(VarResolverSession session, List<Object> args) { return null; }
		@Override public Object invokeValue(VarResolverSession session, List<Object> args) { return null; }
	}

	/** Stable var returning "[key]". */
	public static class StableVar extends SimpleVar {
		public StableVar() { super("ST"); }
		@Override public String resolve(VarResolverSession session, String key) { return "[" + key + "]"; }
		@Override protected boolean isStable() { return true; }
	}

	/** Stable var returning a number string. */
	public static class StableNum extends SimpleVar {
		public StableNum() { super("SN"); }
		@Override public String resolve(VarResolverSession session, String key) { return "41"; }
		@Override protected boolean isStable() { return true; }
	}

	/** Stable var that cannot resolve without beans. */
	public static class NoBeanVar extends SimpleVar {
		public NoBeanVar() { super("NB"); }
		@Override public String resolve(VarResolverSession session, String key) { return "nb"; }
		@Override protected boolean isStable() { return true; }
		@Override protected boolean canResolve(VarResolverSession session) { return session.getBean(String.class).isPresent(); }
	}

	/** Stable var that resolves to null. */
	public static class NullVar extends SimpleVar {
		public NullVar() { super("NU"); }
		@Override public String resolve(VarResolverSession session, String key) { return null; }
		@Override protected boolean isStable() { return true; }
	}

	/** Non-stable var. */
	public static class UnstableVar extends SimpleVar {
		public UnstableVar() { super("UN"); }
		@Override public String resolve(VarResolverSession session, String key) { return "un"; }
	}

	/** Stable streamed var. */
	public static class StableStreamed extends StreamedVar {
		public StableStreamed() { super("SS"); }
		@Override public void resolveTo(VarResolverSession s, java.io.Writer w, String arg) throws Exception { w.write("x"); }
		@Override protected boolean isStable() { return true; }
	}

	private static VarResolver.Builder base() {
		return VarResolver.create().defaultVars().defaultFunctions().functions(new Marker(), new Three(), new Nothing())
			.vars(new StableVar(), new StableNum(), new NoBeanVar(), new NullVar(), new UnstableVar(), new StableStreamed());
	}

	private static final VarResolver LENIENT = base().build();
	private static final VarResolver STRICT = base().strict(true).build();

	private static ExprSegment seg(VarResolver vr, String src) {
		return new ExprSegment(ExprParser.parse(src, vr.maxExprDepth), src);
	}

	private static String render(VarResolver vr, String src) {
		var sb = new StringBuilder();
		seg(vr, src).resolve(vr.createSession(), sb);
		return sb.toString();
	}

	@Test void a01_arithmetic() {
		assertEquals("3", render(LENIENT, "1+2"));
	}

	@Test void a02_stringConcat() {
		assertEquals("a1", render(LENIENT, "'a'+1"));
	}

	@Test void a03_outputNotReparsed() {
		assertEquals("${x}", render(LENIENT, "marker()"));
	}

	@Test void a04_nullIsEmpty() {
		assertEquals("", render(LENIENT, "nothing()"));
	}

	@Test void b01_lenientFallThrough() {
		assertEquals("#{nosuch(1)}", render(LENIENT, "nosuch(1)"));
	}

	@Test void b02_strictThrows() {
		assertThrows(ExprException.class, () -> render(STRICT, "nosuch(1)"));
	}

	@Test void c01_freshBudgetPerRender() {
		var vr = base().maxCollectionSize(3).strict(true).build();
		var s = seg(vr, "three().![this]");
		for (var i = 0; i < 2; i++) {
			var sb = new StringBuilder();
			s.resolve(vr.createSession(), sb);
			assertEquals("[1,2,3]", sb.toString());
		}
	}

	@Test void c02_exhaustedBudgetDoesNotPoisonLaterRender() {
		// Lenient: an over-limit projection truncates; a later render of a within-limit expression must be unaffected.
		var vr = base().maxCollectionSize(3).build();
		var over = seg(vr, "three().![three().![this]]");  // 3 outer + inner elements > 3 total
		var ok = seg(vr, "three().![this]");
		var sb = new StringBuilder();
		over.resolve(vr.createSession(), sb);
		var sb2 = new StringBuilder();
		ok.resolve(vr.createSession(), sb2);
		assertEquals("[1,2,3]", sb2.toString());
		// Same segment, same session, repeated: the budget is per render, not per segment/session.
		var session = vr.createSession();
		for (var i = 0; i < 3; i++) {
			var sb3 = new StringBuilder();
			ok.resolve(session, sb3);
			assertEquals("[1,2,3]", sb3.toString());
		}
	}

	@Test void d01_stableFolds() {
		var f = seg(LENIENT, "1+2").fold(LENIENT);
		assertNotNull(f);
		assertTrue(f.isLiteral());
		assertEquals("3", f.text);
		assertEquals("a1", seg(LENIENT, "'a'+1").fold(LENIENT).text);
		assertEquals("2", seg(LENIENT, "true ? 2 : 3").fold(LENIENT).text);
	}

	@Test void d02_identNotFolded() {
		assertNull(seg(LENIENT, "a+1").fold(LENIENT));
	}

	@Test void d03_callNotFolded() {
		assertNull(seg(LENIENT, "three()").fold(LENIENT));
	}

	@Test void d04_nonStableVarNotFolded() {
		assertNull(seg(LENIENT, "${foo}").fold(LENIENT));
	}

	@Test void d05_foldFailureSkips() {
		assertNull(seg(LENIENT, "1/0").fold(LENIENT));
	}

	@Test void e01_foldsStableVarRef() {
		assertEquals("[k]", seg(LENIENT, "${ST:k}").fold(LENIENT).text);
		assertEquals("411", seg(LENIENT, "${SN:x}+1").fold(LENIENT).text);
	}

	@Test void e02_canResolveFalseNotFolded() {
		assertNull(seg(LENIENT, "${NB:k}").fold(LENIENT));
	}

	@Test void e03_nullValueNotFolded() {
		assertNull(seg(LENIENT, "${NU:k}").fold(LENIENT));
		// Still renders correctly at runtime through the elvis.
		assertEquals("d", render(LENIENT, "${NU:k} ?: 'd'"));
	}

	@Test void e04_unstableVarNotFolded() {
		assertNull(seg(LENIENT, "${UN:k}").fold(LENIENT));
	}

	@Test void e05_dollarBraceShortcutUsesP() {
		// \${name} routes to the P var (not stable), so no fold.
		assertNull(seg(LENIENT, "${someprop}").fold(LENIENT));
	}

	@Test void f01_foldErrorResurfacesStrict() {
		var s = seg(STRICT, "1/0");
		assertNull(s.fold(STRICT));
		var session = STRICT.createSession();
		var sb = new StringBuilder();
		assertThrows(ExprException.class, () -> s.resolve(session, sb));
	}

	@Test void f02_foldFallThroughNotBakedIn() {
		// No operator functions registered, so evaluation falls through at fold time.
		var vr = VarResolver.create().defaultVars().build();
		var s = seg(vr, "1+2");
		assertNull(s.fold(vr));
		var sb = new StringBuilder();
		s.resolve(vr.createSession(), sb);
		assertEquals("#{1+2}", sb.toString());
	}

	@Test void g01_fallThroughIsVerbatim() {
		assertEquals("#{ nosuch( 1 ) }", render(LENIENT, " nosuch( 1 ) "));
		assertEquals("#{nosuch(${ST:a}, 'b')}", render(LENIENT, "nosuch(${ST:a}, 'b')"));
	}

	@Test void h01_isStable() {
		assertFalse(VarTemplateCompiler.isStable(null, false));
		assertFalse(VarTemplateCompiler.isStable(new UnstableVar(), false));
		assertFalse(VarTemplateCompiler.isStable(new StableVar(), true));
		assertFalse(VarTemplateCompiler.isStable(new StableStreamed(), false));
		assertTrue(VarTemplateCompiler.isStable(new StableVar(), false));
	}

	@Test void i01_identifiersResolveToNullToday() {
		// Pins current behaviour: no render-scope hook yet, so bare identifiers (other than 'this') are null.
		assertEquals("", render(LENIENT, "someIdent"));
		assertEquals("", render(STRICT, "someIdent"));
		assertEquals("x", render(LENIENT, "someIdent ?: 'x'"));
	}
}
