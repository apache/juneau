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
package org.apache.juneau.commons.template;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link TemplateResolver}. */
class TemplateResolver_Render_Test extends TestBase {

	private static String r(TemplateResolver tr, String t, Map<String,?> scope) {
		return tr.render(tr.parse(t), scope);
	}

	private static Map<String,Object> row(String id, String msg) {
		var m = new LinkedHashMap<String,Object>();
		m.put("id", id);
		m.put("msg", msg);
		return m;
	}

	@Test void a01_scopeVarReadsScope() {
		assertEquals("Hi Bob", r(TemplateResolver.DEFAULT, "Hi ${name}", Map.of("name", "Bob")));
	}

	@Test void a02_absentNameIsEmpty() {
		assertEquals("Hi ", r(TemplateResolver.DEFAULT, "Hi ${name}", Map.of()));
	}

	@Test void a03_noSystemVarsByDefault() {
		// "S" is not a registered var, so this is the scope key "S" with default "user.home"; no system property is read.
		assertEquals("user.home", r(TemplateResolver.DEFAULT, "${S:user.home}", Map.of()));
		assertNotEquals(System.getProperty("user.home"), r(TemplateResolver.DEFAULT, "${S:user.home}", Map.of()));
	}

	@Test void a04_pCanBeRebound() {
		var tr = TemplateResolver.create().vars(new DefaultingVar("P") {
			@Override public String resolve(VarResolverSession s, String key) { return "rebound-" + key; }
		}).build();
		assertEquals("rebound-x", r(tr, "${x}", Map.of("x", "scope")));
	}

	@Test void a05_projectionReadsRowFields() {
		var rows = List.of(row("1", "a"), row("2", "b"));
		assertEquals("1:a,2:b", r(TemplateResolver.DEFAULT, "#{join(',', rows.![id + ':' + msg])}", Map.of("rows", rows)));
	}

	@Test void a06_presentNullFieldDoesNotFallBackToRoot() {
		var m = row("1", null);
		assertEquals("[x]", r(TemplateResolver.DEFAULT, "#{rows.![coalesce(msg, 'x')]}".replace("rows.![coalesce(msg, 'x')]", "'['+rows.![msg ?: 'x'][0]+']'"), Map.of("rows", List.of(m), "msg", "ROOT")));
	}

	@Test void a07_denyListedFunctionLenientFallsThrough() {
		assertEquals("#{exec('x')}", r(TemplateResolver.DEFAULT, "#{exec('x')}", Map.of()));
	}

	@Test void a08_strictUnknownFunctionThrows() {
		var tr = TemplateResolver.create().strict(true).build();
		assertThrows(ExprException.class, () -> r(tr, "#{exec('x')}", Map.of()));
	}

	@Test void a09_injectionInDataNotReparsed() {
		assertEquals("${S:user.home} #{1+1}", r(TemplateResolver.DEFAULT, "${a}", Map.of("a", "${S:user.home} #{1+1}")));
	}

	@Test void a10_classAccessBlocked() {
		assertThrows(ExprException.class, () -> r(TemplateResolver.DEFAULT, "#{a.class}", Map.of("a", "x")));
	}

	@Test void a11_maxCollectionSizeLenientTruncates() {
		var tr = TemplateResolver.create().maxCollectionSize(2).build();
		var out = r(tr, "#{list(1,2,3,4)}", Map.of());
		assertFalse(out.contains("4"), out);
	}

	@Test void a12_maxCollectionSizeStrictThrows() {
		var tr = TemplateResolver.create().maxCollectionSize(2).strict(true).build();
		assertThrows(RuntimeException.class, () -> r(tr, "#{list(1,2,3,4)}", Map.of()));
	}

	@Test void a13_explicitEncoderOverridesDestination() {
		var tr = TemplateResolver.create().destination(Destinations.HTML).build();
		var t = tr.parse("${a}");
		assertEquals("<b>", tr.render(t, Map.of("a", "<b>"), s -> s));
		assertEquals("&lt;b&gt;", tr.render(t, Map.of("a", "<b>")));
	}

	@Test void a14_nullScopeAndTemplateLiteral() {
		assertEquals("plain", TemplateResolver.DEFAULT.render(TemplateResolver.DEFAULT.parse("plain"), null));
	}

	@Test void a15_foreignParsedTemplateRejected() {
		var other = TemplateResolver.create().build();
		assertThrows(IllegalArgumentException.class, () -> TemplateResolver.DEFAULT.render(other.parse("x"), Map.of()));
	}

	@Test void a16_allowFunctionsIsAdditive() {
		var tr = TemplateResolver.create().allowFunctions("repeat").build();
		assertEquals("ababX", r(tr, "#{repeat('ab', 2)}#{upper('x')}", Map.of()));
	}

	@Test void a17_allowOnlyFunctionsReplaces() {
		var tr = TemplateResolver.create().allowOnlyFunctions("upper", "$op.*").build();
		assertEquals("X #{join(',', list('a'))}", r(tr, "#{upper('x')} #{join(',', list('a'))}", Map.of()));
	}

	@Test void a18_wildcardRejected() {
		assertThrows(IllegalArgumentException.class, () -> TemplateResolver.create().allowFunctions("*"));
		assertThrows(IllegalArgumentException.class, () -> TemplateResolver.create().allowOnlyFunctions("*"));
	}

	@Test void a19_mapRowSizeShadowsScope() {
		// Pins current behavior: a map row's synthetic size does not fall back to a scope entry named size.
		var rows = List.of(row("1", "a"));
		assertEquals("2", r(TemplateResolver.DEFAULT, "#{join(',', rows.![size])}", Map.of("rows", rows, "size", "OUTER")));
	}

	@Test void a20_noDestinationDoesNotEncode() {
		assertEquals("<b>", r(TemplateResolver.DEFAULT, "${a}", Map.of("a", "<b>")));
	}

	@Test void a21_ulLenientTruncates() {
		var tr = TemplateResolver.create().maxCollectionSize(2).destination(Destinations.PLAIN_TEXT).build();
		assertEquals("- 1\n- 2", r(tr, "#{ul(xs)}", Map.of("xs", List.of(1, 2, 3, 4))));
	}

	@Test void a22_ulStrictThrows() {
		var tr = TemplateResolver.create().maxCollectionSize(2).strict(true).build();
		assertThrows(RuntimeException.class, () -> r(tr, "#{ul(xs)}", Map.of("xs", List.of(1, 2, 3))));
	}

	@Test void a23_olTableTakeJoinCapped() {
		var tr = TemplateResolver.create().maxCollectionSize(2).destination(Destinations.PLAIN_TEXT).build();
		var xs = Map.<String,Object>of("xs", List.of(1, 2, 3, 4));
		assertEquals("1. 1\n2. 2".replace("\\n", "\n"), r(tr, "#{ol(xs)}", xs));
		assertEquals("1,2", r(tr, "#{join(',', xs)}", xs));
		assertEquals("[1,2]", r(TemplateResolver.create().maxCollectionSize(2).build(), "#{take(xs, 3)}", xs));
		var rows = Map.<String,Object>of("rows", List.of(List.of("a"), List.of("b"), List.of("c")));
		assertEquals(2, r(tr, "#{table(list('H'), rows)}", rows).split("\n").length - 2);
	}

	@Test void a24_joinAndTakeStrictThrow() {
		var tr = TemplateResolver.create().maxCollectionSize(2).strict(true).build();
		var xs = Map.<String,Object>of("xs", List.of(1, 2, 3));
		assertThrows(RuntimeException.class, () -> r(tr, "#{join(',', xs)}", xs));
		assertThrows(RuntimeException.class, () -> r(tr, "#{take(xs, 1)}", xs));
	}
}
