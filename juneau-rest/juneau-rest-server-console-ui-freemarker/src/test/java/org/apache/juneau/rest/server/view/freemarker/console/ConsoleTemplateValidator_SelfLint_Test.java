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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Lints the module's own templates: the shipped {@code base.ftlh} and every test fixture that is not intentionally
 * invalid. Keeps the fixtures honest and proves the validator accepts real console templates.
 *
 * @since 10.0.0
 */
class ConsoleTemplateValidator_SelfLint_Test extends TestBase {

	// Fixtures that exist to trigger an error. Each entry names the test that renders it and expects the failure.
	private static final Set<String> INTENTIONALLY_INVALID = Set.of(
		"admin/console-format.ftlh",      // ConsoleDirective_Test.c08_formatAttr_isRejected expects a format= rejection
		"admin/console-nav-bogus.ftlh",   // NavigationDirective_Test.c02_unknownNodeAttr_isRejected expects E-? unknown attribute
		"admin/console-theme-bogus.ftlh", // ThemeDirective_Test.d03_unknownThemeAttr_isRejected expects E-? unknown attribute
		"admin/page-cards-bogus.ftlh",    // CardDirective_Test.b03_unknownAttr_isRejected expects E-? unknown attribute
		"admin/page-cards-format.ftlh",   // CardDirective_Test.b02_formatAttr_isRejected expects a format= rejection
		"admin/page-theme-attr.ftlh",     // ThemeDirective_Test.d04_pageThemeAttr_isRejected expects E-? unknown attribute
		"admin/page-unknown.ftlh",        // PageDirective_Test.a04_unknownAttr_isRejected expects E-? unknown attribute
		"c1/bool-visible-tru.ftlh",       // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
		"c1/bool-visible-empty.ftlh",     // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
		"c1/bool-selected-tru.ftlh",      // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
		"c1/bool-selected-empty.ftlh",    // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
		"c1/bool-chrome-tru.ftlh",        // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
		"c1/bool-chrome-empty.ftlh"       // NodeDirective_StrictBoolean_Test.a04_rejected_E5 expects E-5 strict-boolean
	);

	private static Path basedir() {
		return Path.of(System.getProperty("basedir", "."));
	}

	@Test void a01_testFixtures() {
		var findings = ConsoleTemplateValidator.create()
			.templateRoot(basedir().resolve("src/test/resources/templates"))
			.validateAll()
			.stream()
			.filter(f -> ! f.template().startsWith("c1/bad-") && ! f.template().startsWith("c1/removed-") && ! f.template().startsWith("c1/dev-bad-"))
			.filter(f -> ! INTENTIONALLY_INVALID.contains(f.template()))
			.toList();
		assertTrue(findings.isEmpty(), () -> String.join("\n", findings.stream().map(Object::toString).toList()));
	}

	@Test void a02_shippedTemplates() {
		var f = ConsoleTemplateValidator.create()
			.templateRoot(basedir().resolve("src/main/resources"))
			.validateAll();
		assertTrue(f.isEmpty(), f::toString);
	}
}
