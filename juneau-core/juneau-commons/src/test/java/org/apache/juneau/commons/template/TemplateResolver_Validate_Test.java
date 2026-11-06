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
import org.junit.jupiter.api.*;

/** Tests for {@link TemplateResolver#check(String, TemplateCatalog)} and {@link TemplateResolver#validate(String, TemplateCatalog)}. */
class TemplateResolver_Validate_Test extends TestBase {

	private static final TemplateResolver TR = TemplateResolver.DEFAULT;

	private static final TemplateCatalog CAT = TemplateCatalog.create()
		.fields("n", "title")
		.list("rows", FieldCatalog.of("id", "msg"))
		.list("anyrows")
		.build();

	private static TemplateProblem p(int s, int e, String m) {
		return new TemplateProblem(s, e, m);
	}

	@Test void a01_cleanTemplateHasNoProblems() {
		assertTrue(TR.check("${n} #{join(',', rows.![id + msg])}", CAT).isEmpty());
	}

	@Test void a02_unknownTopLevelIdentifier() {
		assertEquals(List.of(p(2, 7, "unknown name 'wrong'")), TR.check("#{wrong}", CAT));
	}

	@Test void a03_unknownRowField() {
		assertEquals(List.of(p(9, 14, "unknown field 'bogus' in list 'rows'")), TR.check("#{rows.![bogus]}", CAT));
	}

	@Test void a04_listWithoutCatalogIsPermissive() {
		assertTrue(TR.check("#{anyrows.![whatever]}", CAT).isEmpty());
	}

	@Test void a05_rowTemplateMayUseTopLevelNames() {
		assertTrue(TR.check("#{rows.![id + title]}", CAT).isEmpty());
	}

	@Test void a06_collectionCallOnNonList() {
		assertEquals(List.of(p(6, 8, "'n' is not a declared list")), TR.check("#{n.![id]}", CAT));
	}

	@Test void a07_unknownFunction() {
		assertEquals(List.of(p(2, 6, "unknown or denied function 'exec'")), TR.check("#{exec(n)}", CAT));
	}

	@Test void a08_unknownFunctionAllowedByCatalog() {
		var cat = TemplateCatalog.create().fields("n").allowUnknownFunctions(true).build();
		assertTrue(TR.check("#{exec(n)}", cat).isEmpty());
	}

	@Test void a09_dollarBraceKeyIsCatalogChecked() {
		assertEquals(List.of(p(2, 8, "unknown name 'secret'")), TR.check("${secret}", CAT));
		assertTrue(TR.check("${n}", CAT).isEmpty());
	}

	@Test void a09b_dollarBraceKeyWithDefault() {
		assertEquals(List.of(p(2, 9, "unknown name 'unknown'")), TR.check("${unknown:default}", CAT));
		assertTrue(TR.check("${n:fallback}", CAT).isEmpty());
	}

	@Test void a09c_dollarBraceInsideExpression() {
		assertEquals(List.of(p(4, 7, "unknown name 'bad'")), TR.check("#{${bad}}", CAT));
	}

	@Test void a09d_computedKeyNotAllowed() {
		// The key is read as written, so a computed key is an unknown name and is not evaluated at render time.
		assertFalse(TR.check("${${n}}", CAT).isEmpty());
		assertEquals("", TR.render(TR.parse("${${n}}"), Map.of("n", "secret", "secret", "LEAK")));
	}

	@Test void a10_severalProblemsInSourceOrder() {
		var out = TR.check("#{b} ${n} #{a}", CAT);
		assertEquals(List.of(p(2, 3, "unknown name 'b'"), p(12, 13, "unknown name 'a'")), out);
	}

	@Test void a11_syntaxErrorBecomesProblem() {
		var out = TR.check("#{1 +}", CAT);
		assertEquals(1, out.size());
		assertEquals(0, out.get(0).start());
	}

	@Test void a12_strictCompileErrorBecomesOneProblem() {
		var out = TemplateResolver.create().strict(true).build().check("#{exec(1)}", CAT);
		assertEquals(1, out.size());
	}

	@Test void a13_validateThrowsWithProblems() {
		var e = assertThrows(TemplateValidationException.class, () -> TR.validate("#{x}", CAT));
		assertEquals(1, e.problems().size());
		assertTrue(e.getMessage().contains("unknown name 'x'"));
	}

	@Test void a15_validateCompilesOnce() {
		var parsed = TR.validate("${n}", CAT);
		assertEquals("x", TR.render(parsed, Map.of("n", "x")));
	}

	@Test void a14_validateReturnsParsed() {
		assertEquals("t", TR.validate("t", CAT).getSource());
	}
}
