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

/** Tests for the render-scope hook ({@link VarResolverSession#scope(java.util.function.Function)}). */
class ExprSegment_Scope_Test extends TestBase {

	/** Record row. */
	public record Row(String id, int tries) {}

	/** Bean row. */
	public static class Bean {
		public String getId() { return "bean"; }
		public String getNullable() { return null; }
	}

	private static VarResolver.Builder base() {
		return VarResolver.create().defaultVars().defaultFunctions();
	}

	private static final VarResolver LENIENT = base().build();
	private static final VarResolver STRICT = base().strict(true).build();

	private static String render(VarResolver vr, String tpl, Map<String,Object> scope) {
		var session = vr.createSession().scope(scope::get);
		return vr.compile(tpl).resolve(session);
	}

	@Test void a01_bareIdentifierReadsScope() {
		assertEquals("Bob", render(LENIENT, "#{name}", Map.of("name", "Bob")));
	}

	@Test void a02_absentNameIsNullAndElvisFires() {
		assertEquals("anon", render(LENIENT, "#{name ?: 'anon'}", Map.of()));
	}

	@Test void a03_noScopeSetIsAbsent() {
		assertEquals("anon", LENIENT.compile("#{name ?: 'anon'}").resolve(LENIENT.createSession()));
	}

	@Test void a04_recordRowsInProjection() {
		var scope = Map.<String,Object>of("failed", List.of(new Row("a", 1), new Row("b", 2)));
		assertEquals("a:1,b:2", render(LENIENT, "#{join(',', failed.![id + ':' + tries])}", scope));
	}

	@Test void a05_mapRowsInProjection() {
		var scope = Map.<String,Object>of("rows", List.of(Map.of("k", "x"), Map.of("k", "y")));
		assertEquals("x,y", render(LENIENT, "#{join(',', rows.![k])}", scope));
	}

	@Test void a06_elementShadowsOuterScope() {
		var scope = Map.<String,Object>of("id", "outer", "rows", List.of(Map.of("id", "inner")));
		assertEquals("inner", render(LENIENT, "#{join(',', rows.![id])}", scope));
	}

	@Test void a07_elementFallsBackToOuterScopeForMissingName() {
		var scope = Map.<String,Object>of("prefix", "P-", "rows", List.of(Map.of("id", "1"), Map.of("id", "2")));
		assertEquals("P-1,P-2", render(LENIENT, "#{join(',', rows.![prefix + id])}", scope));
	}

	@Test void a08_classNameStaysBlocked() {
		var scope = Map.<String,Object>of("rows", List.of(new Bean()));
		assertThrows(ExprException.class, () -> render(STRICT, "#{rows.![class]}", scope));
	}

	@Test void a09_throwingScopeSurfacesInStrict() {
		var session = STRICT.createSession().scope(n -> { throw new IllegalStateException("boom"); });
		var t = STRICT.compile("#{name}");
		assertThrows(RuntimeException.class, () -> t.resolve(session));
	}

	@Test void a10_expressionWithIdentifierIsNotFolded() {
		var t = LENIENT.compile("#{name}");
		assertFalse(t.isLiteral());
		assertEquals("A", render(LENIENT, "#{name}", Map.of("name", "A")));
		assertEquals("B", t.resolve(LENIENT.createSession().scope(Map.<String,Object>of("name", "B")::get)));
	}

	/** Stand-in for a layer var bound to the <c>${name}</c> shortcut: reads the session scope. */
	public static class ScopeP extends SimpleVar {
		public ScopeP() { super("P"); }
		@Override public String resolve(VarResolverSession session, String key) { return String.valueOf(session.scope().apply(key)); }
	}

	@Test void a11_dollarBraceInsideProjectionReadsRenderScope() {
		var scope = Map.<String,Object>of("id", "outer", "rows", List.of(Map.of("id", "inner")));
		var vr = VarResolver.create().defaultFunctions().vars(new ScopeP()).build();
		// Pins the gotcha: ${id} resolves through the session, which has no notion of the element.
		assertEquals("inner/outer", render(vr, "#{join(',', rows.![id + '/' + ${id}])}", scope));
	}

	@Test void a12_nullScopeClears() {
		var session = LENIENT.createSession().scope(Map.<String,Object>of("n", "x")::get).scope(null);
		assertEquals("none", LENIENT.compile("#{n ?: 'none'}").resolve(session));
	}

	@Test void a13_presentNullMapValue() {
		var scope = new HashMap<String,Object>();
		scope.put("a", null);
		assertEquals("d", render(LENIENT, "#{a ?: 'd'}", scope));
	}

	@Test void a14_beanGetterNullDoesNotFallBack() {
		var scope = Map.<String,Object>of("nullable", "outer", "rows", List.of(new Bean()));
		assertEquals("[null]", render(LENIENT, "#{rows.![nullable]}", scope));
	}
}
