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

import static org.apache.juneau.commons.svl.TemplateReference.Kind.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/** Tests for {@link VarTemplate#references()}. */
class VarTemplate_References_Test extends TestBase {

	private static final VarResolver VR = VarResolver.create().defaultFunctions().allowFunctions("upper", "join", "$op.*").build();

	private static List<TemplateReference> refs(String t) {
		return VR.compile(t).references();
	}

	private static final VarResolver WITH_P = VarResolver.create().vars(new DefaultingVar("P") {
		@Override public String resolve(VarResolverSession s, String key) { return key; }
	}).build();

	private static List<TemplateReference> refsP(String t) {
		return WITH_P.compile(t).references();
	}

	private static TemplateReference ref(TemplateReference.Kind k, String name, boolean bound, String list, int s, int e) {
		return new TemplateReference(k, name, bound, list, s, e);
	}

	@Test void a01_literalHasNone() {
		assertTrue(refs("plain").isEmpty());
		assertTrue(refs(null).isEmpty());
	}

	@Test void a02_varShortcutIsP() {
		assertEquals(List.of(ref(VAR, "P", true, null, 3, 10), ref(IDENT, "name", true, null, 5, 9)), refsP("hi ${name}"));
	}

	@Test void a03_unregisteredPrefixIsShortcutKey() {
		// "Q" is not a registered var, so the whole body is the shortcut key; with no P registered it is unbound.
		assertEquals(List.of(ref(VAR, "P", false, null, 0, 6)), refs("${Q:x}"));
		assertEquals(List.of(ref(VAR, "P", true, null, 0, 6), ref(IDENT, "Q", true, null, 2, 3)), refsP("${Q:x}"));
	}

	@Test void a04_functionAndIdentOffsets() {
		assertEquals(List.of(ref(FUNCTION, "upper", true, null, 2, 7), ref(IDENT, "name", true, null, 8, 12)), refs("#{upper(name)}"));
	}

	@Test void a05_deniedFunctionUnbound() {
		var r = refs("#{trim(x)}");
		assertEquals(ref(FUNCTION, "trim", false, null, 2, 6), r.get(0));
	}

	@Test void a06_rowFieldCarriesListName() {
		var r = refs("#{rows.![id]}");
		assertEquals(ref(IDENT, "rows", true, null, 2, 6), r.get(0));
		assertEquals(ref(IDENT, "id", true, "rows", 9, 11), r.get(1));
	}

	@Test void a07_selectBodyCarriesListName() {
		var r = refs("#{rows.?[ok]}");
		assertEquals(ref(IDENT, "ok", true, "rows", 9, 11), r.get(1));
	}

	@Test void a08_opaqueTargetOmitsBodyIdents() {
		var r = refs("#{list(1).![id]}");
		assertTrue(r.stream().noneMatch(x -> x.kind() == IDENT), r.toString());
	}

	@Test void a09_varInsideExpression() {
		var r = refs("#{${P:x} + 1}");
		assertEquals(VAR, r.get(0).kind());
		assertEquals(2, r.get(0).start());
	}

	@Test void a10_syntaxProblemReported() {
		var r = refs("a #{1 +} b");
		assertEquals(SYNTAX, r.get(0).kind());
		assertEquals(2, r.get(0).start());
	}

	@Test void a11_foldedExpressionAbsent() {
		assertTrue(refs("#{1 + 2}").isEmpty());
	}

	@Test void a12_sourceOrderAcrossMarkers() {
		var r = refs("${a} #{b} ${c}");
		assertEquals(List.of(0, 7, 10), r.stream().map(TemplateReference::start).toList());
	}

	@Test void a13_listIsUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> refs("${a}").add(null));
	}
}
