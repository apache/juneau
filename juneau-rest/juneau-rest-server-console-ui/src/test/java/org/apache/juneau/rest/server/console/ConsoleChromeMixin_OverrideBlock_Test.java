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
package org.apache.juneau.rest.server.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link ConsoleChromeMixin#overrideBlock(Theme)}: the single emitter for a theme's override block. Leaves are
 * escaped and emitted first in declaration order, then aliases verbatim. An empty theme emits nothing.
 */
class ConsoleChromeMixin_OverrideBlock_Test extends TestBase {

	private static final Theme EMISSION = Theme.create("corporate")
		.token("--jc-accent", "#b45309")
		.token("--jc-link", "#0b6bcb")
		.alias("--jc-tab-bar-bg", "var(--jc-card-bg)")
		.alias("--jc-tab-selected-bg", "var(--jc-white)")
		.build();

	private static List<String> declaredNames(String block) {
		var names = new ArrayList<String>();
		var m = Pattern.compile("(--[a-zA-Z0-9-]++)\\s*:").matcher(block);
		while (m.find())
			names.add(m.group(1));
		return names;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a) Shape and order
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_block_isOneHtmlRootBlock() {
		var block = ConsoleChromeMixin.overrideBlock(EMISSION);
		assertTrue(block.startsWith("html:root{"), block);
		assertTrue(block.endsWith("}"), block);
		assertEquals(1, block.split("html:root\\{", -1).length - 1, block);
	}

	@Test void a02_block_carriesLeavesThenAliases_inDeclarationOrder() {
		assertList(declaredNames(ConsoleChromeMixin.overrideBlock(EMISSION)),
			"--jc-accent", "--jc-link", "--jc-tab-bar-bg", "--jc-tab-selected-bg");
	}

	/**
	 * The two var() paths must never be unified: a var() given to token() is resolved before the wire (a05); an
	 * alias survives as a reference. If either test has to change to make the other pass, the change is wrong.
	 */
	@Test void a03_alias_survivesAsAReference() {
		var block = ConsoleChromeMixin.overrideBlock(EMISSION);
		assertContainsAll(block, "--jc-tab-bar-bg:var(--jc-card-bg);", "--jc-tab-selected-bg:var(--jc-white);");
	}

	/** Every declaration is a --jc- name; the digit-admitting class makes sure names like --evil-1 are inspected. */
	@Test void a04_block_declaresOnlyJcNames() {
		var names = declaredNames(ConsoleChromeMixin.overrideBlock(EMISSION));
		assertEquals(4, names.size(), names::toString);
		for (var n : names)
			assertTrue(n.startsWith("--jc-"), n);
	}

	@Test void a05_leafVarReference_isResolvedToItsLiteral() {
		var t = Theme.create("derived").token("--jc-danger", "#c23934").token("--jc-tag-red-text", "var(--jc-danger)").build();
		var block = ConsoleChromeMixin.overrideBlock(t);
		assertTrue(block.contains("--jc-tag-red-text:#c23934;"), block);
		assertFalse(block.contains("--jc-tag-red-text:var("), block);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Escaping
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * The escaper-wiring gate. {@code 'My;Font'} is grammar-accepted, but a raw {@code ;} would end the
	 * declaration early. This is the one value shape where the escaper is not a no-op.
	 */
	@Test void b01_leafWithSemicolon_isEscaped() {
		var block = ConsoleChromeMixin.overrideBlock(Theme.create("semi").token("--jc-font", "'My;Font', sans-serif").build());
		assertFalse(block.contains("'My;Font'"), block);
		assertTrue(block.contains("\\3B "), block);
	}

	@Test void b02_valuePositives_surviveByteForByte() {
		var block = ConsoleChromeMixin.overrideBlock(Theme.create("positives")
			.token("--jc-accent", "#e91e63")
			.token("--jc-page-bg", "linear-gradient(180deg, #aabbcc 0%, #112233 100%)")
			.token("--jc-font", "'Helvetica Neue', Inter, sans-serif")
			.build());
		assertContainsAll(block, "--jc-accent:#e91e63;", "--jc-page-bg:linear-gradient(180deg, #aabbcc 0%, #112233 100%);",
			"--jc-font:'Helvetica Neue', Inter, sans-serif;");
	}

	@Test void b03_block_passesChromeCssScanner() {
		var block = ConsoleChromeMixin.overrideBlock(EMISSION);
		assertEquals(List.of(), ChromeCssScanner.scan(block), block);
	}

	@Test void b04_redTriad_isThemeable() {
		var block = ConsoleChromeMixin.overrideBlock(Theme.create("red-override")
			.token("--jc-tag-red-bg", "#ffe0e2")
			.token("--jc-tag-red-text", "#5a0f14")
			.token("--jc-tag-red-border", "#f0b3b7")
			.build());
		assertContainsAll(block, "--jc-tag-red-bg:#ffe0e2;", "--jc-tag-red-text:#5a0f14;", "--jc-tag-red-border:#f0b3b7;");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c) Empty suppression, on the honest "no leaves AND no aliases" condition
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_noLeavesAndNoAliases_emitsNothing() {
		assertEquals("", ConsoleChromeMixin.overrideBlock(Theme.create("empty").build()));
	}

	@Test void c02_aliasesButNoLeaves_stillEmits() {
		assertEquals("html:root{--jc-tab-bar-bg:var(--jc-card-bg);}",
			ConsoleChromeMixin.overrideBlock(Theme.create("alias-only").alias("--jc-tab-bar-bg", "var(--jc-card-bg)").build()));
	}

	@Test void c03_leavesButNoAliases_stillEmits() {
		assertEquals("html:root{--jc-accent:#b45309;}",
			ConsoleChromeMixin.overrideBlock(Theme.create("leaf-only").token("--jc-accent", "#b45309").build()));
	}

	/** The emitter applies no "open" name suppression; that decision belongs to the caller (buildBody). */
	@Test void c04_themeNamedOpen_isNotSuppressed() {
		assertEquals("html:root{--jc-accent:#b45309;}",
			ConsoleChromeMixin.overrideBlock(Theme.create("open").token("--jc-accent", "#b45309").build()));
	}
}
