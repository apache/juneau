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

/** Tests for {@link VarTemplate#resolve(VarResolverSession, UnaryOperator)}. */
class VarTemplate_ValueEncoder_Test extends TestBase {

	/** Stable var returning "[key]". */
	public static class StableVar extends SimpleVar {
		public StableVar() { super("ST"); }
		@Override public String resolve(VarResolverSession session, String key) { return "<" + key + ">"; }
		@Override protected boolean isStable() { return true; }
	}

	/** Plain var returning "<key>". */
	public static class AngleVar extends SimpleVar {
		public AngleVar() { super("A"); }
		@Override public String resolve(VarResolverSession session, String key) { return "<" + key + ">"; }
	}

	/** Reads the scope. */
	public static class ScopeP extends SimpleVar {
		public ScopeP() { super("P"); }
		@Override public String resolve(VarResolverSession session, String key) { return String.valueOf(session.scope().apply(key)); }
	}

	private static final VarResolver VR = VarResolver.create().defaultFunctions().vars(new StableVar(), new AngleVar(), new ScopeP()).build();

	private static final UnaryOperator<String> BRACKET = s -> "[" + s + "]";
	private static final UnaryOperator<String> HTML = s -> s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

	private static String render(String tpl, UnaryOperator<String> enc, Map<String,Object> scope) {
		return VR.compile(tpl).resolve(VR.createSession().scope(scope::get), enc);
	}

	@Test void a01_literalTextUnencoded() {
		assertEquals("plain <b> text", render("plain <b> text", BRACKET, Map.of()));
	}

	@Test void a02_varOutputEncodedOnce() {
		assertEquals("x [<v>] y", render("x ${A:v} y", BRACKET, Map.of()));
	}

	@Test void a03_encodedAfterFunction() {
		assertEquals("&lt;A&gt;", render("#{upper(name)}", HTML, Map.of("name", "<a>")));
	}

	@Test void a04_nestedVarEncodedOnce() {
		assertEquals("[<<v>>]", render("${A:${A:v}}", BRACKET, Map.of()));
	}

	@Test void a05_nestedVarInsideFunctionEncodedOnce() {
		assertEquals("[<V>]", render("#{upper(${A:v})}", BRACKET, Map.of()));
	}

	@Test void a06_structureBypassesEncoder() {
		var out = render("#{ul('a', 'b')}", BRACKET, Map.of());
		assertFalse(out.startsWith("["), out);
		assertTrue(out.contains("a") && out.contains("b"), out);
	}

	@Test void a07_foldedVarIsEncoded() {
		var t = VR.compile("a ${ST:v} b");
		assertEquals("a <v> b", t.resolve(VR.createSession()));
		assertEquals("a &lt;v&gt; b", t.resolve(VR.createSession(), HTML));
	}

	@Test void a08_foldedVarOnlyTemplateIsEncoded() {
		var t = VR.compile("${ST:v}");
		assertTrue(t.isLiteral());
		assertEquals("<v>", t.resolve(VR.createSession()));
		assertEquals("[<v>]", t.resolve(VR.createSession(), BRACKET));
	}

	@Test void a09_pureLiteralExpressionFoldIsNotEncoded() {
		var t = VR.compile("#{'<' + 'b>'}");
		assertEquals("<b>", t.resolve(VR.createSession(), BRACKET));
	}

	@Test void a10_expressionFoldOverStableVarIsEncoded() {
		var t = VR.compile("#{'x' + ${ST:v}}");
		assertEquals("[x<v>]", t.resolve(VR.createSession(), BRACKET));
	}

	@Test void a11_nullEncoderEqualsPlainResolve() {
		var t = VR.compile("a ${A:v} #{1+2}");
		assertEquals(t.resolve(VR.createSession()), t.resolve(VR.createSession(), null));
	}

	@Test void a12_literalOnlyTemplateFastPath() {
		var t = VR.compile("just text");
		assertTrue(t.isLiteral());
		assertEquals("just text", t.resolve(VR.createSession(), BRACKET));
	}

	@Test void a13_exprFallThroughStaysUnencoded() {
		assertEquals("#{nosuch(1)}", render("#{nosuch(1)}", BRACKET, Map.of()));
	}

	@Test void a14_scopeValuesNeverReparsed() {
		assertEquals("[${x}] [#{1+1}]", render("#{n} #{m}", BRACKET, Map.of("n", "${x}", "m", "#{1+1}")));
	}
}
