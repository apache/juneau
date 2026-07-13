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

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.settings.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;

/**
 * Tests for the {@code ${xxx}} shortcut in {@link VarResolverSession}'s tokenizer.
 *
 * <p>
 * The shortcut routes {@code ${xxx}} to {@code $P{xxx}} (i.e. through {@code PropertyVar}, which
 * reads from {@link Settings}). The first top-level {@code ':'} in the body is rewritten to
 * {@code ','} so Spring-idiom defaults {@code ${key:default}} resolve via {@code DefaultingVar}.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class DollarBraceShortcut_Test extends TestBase {

	private static final String K1 = "DollarBraceShortcut_Test.k1";
	private static final String K2 = "DollarBraceShortcut_Test.k2";
	private static final String K3 = "DollarBraceShortcut_Test.k3";
	private static final String K_ENV = "DollarBraceShortcut_Test.envName";

	@AfterEach
	void cleanup() {
		Settings.get().unsetGlobal(K1);
		Settings.get().unsetGlobal(K2);
		Settings.get().unsetGlobal(K3);
		Settings.get().unsetGlobal(K_ENV);
		Settings.get().unsetGlobal("DollarBraceShortcut_Test.staging.url");
		Settings.get().unsetGlobal("DollarBraceShortcut_Test.prod.url");
	}

	//====================================================================================================
	// Basic resolution.
	//====================================================================================================

	@Test
	void a01_dollarBrace_resolvesFromSettings() {
		Settings.get().setGlobal(K1, "from-settings");
		assertEquals("from-settings", VarResolver.DEFAULT.resolve("${" + K1 + "}"));
	}

	@Test
	void a02_dollarBrace_identicalToDollarP() {
		Settings.get().setGlobal(K1, "v");
		var dp = VarResolver.DEFAULT.resolve("$P{" + K1 + "}");
		var db = VarResolver.DEFAULT.resolve("${" + K1 + "}");
		assertEquals(dp, db, "${...} must resolve identically to $P{...}");
	}

	@Test
	void a03_dollarBrace_missing_returnsLiteral() {
		// PropertyVar.resolve returns null for missing key; with no default, DefaultingVar yields
		// null, which the var-resolution layer converts to "".
		assertEquals("", VarResolver.DEFAULT.resolve("${" + K1 + "}"));
	}

	//====================================================================================================
	// Default-value syntax: ${key:default} // NOSONAR
	//====================================================================================================

	@Test
	void b01_dollarBrace_default_colonSeparator() {
		assertEquals("bar", VarResolver.DEFAULT.resolve("${" + K1 + ":bar}"));
	}

	@Test
	void b02_dollarBrace_default_presentValueWins() {
		Settings.get().setGlobal(K1, "real");
		assertEquals("real", VarResolver.DEFAULT.resolve("${" + K1 + ":fallback}"));
	}

	@Test
	void b03_dollarBrace_default_multiColon_keepsTrailing() {
		// First ':' is the separator; remaining ':' are part of the default literal.
		assertEquals("postgres://localhost/db",
			VarResolver.DEFAULT.resolve("${" + K1 + ":postgres://localhost/db}"));
	}

	@Test
	void b04_dollarBrace_default_commaInDefaultStillWorks() {
		// Translation rewrites the first top-level ':' to ','; pre-existing ',' in the default text
		// is preserved by DefaultingVar (which splits on FIRST ',' only).
		assertEquals("a,b,c", VarResolver.DEFAULT.resolve("${" + K1 + ":a,b,c}"));
	}

	//====================================================================================================
	// Nested resolution: ${foo.${env}.url} // NOSONAR
	//====================================================================================================

	@Test
	void c01_dollarBrace_nestedKey() {
		Settings.get().setGlobal(K_ENV, "staging");
		Settings.get().setGlobal("DollarBraceShortcut_Test.staging.url", "https://staging.example/");
		assertEquals("https://staging.example/",
			VarResolver.DEFAULT.resolve("${DollarBraceShortcut_Test.${" + K_ENV + "}.url}"));
	}

	@Test
	void c02_dollarBrace_innerColonIsInnerDefault() {
		// Inner ${innerKey:inner-default} provides "inner-default" because innerKey is unset; // NOSONAR
		// outer ${k1:<inner>} sees k1 missing, falls back to "inner-default".
		assertEquals("inner-default",
			VarResolver.DEFAULT.resolve("${" + K1 + ":${" + K3 + ":inner-default}}"));
	}

	//====================================================================================================
	// Backward compatibility: existing $X{...} forms still work.
	//====================================================================================================

	@Test
	void d01_dollarP_stillWorks() {
		Settings.get().setGlobal(K2, "p-val");
		assertEquals("p-val", VarResolver.DEFAULT.resolve("$P{" + K2 + "}"));
	}

	@Test
	void d02_dollarP_commaDefaultUnchanged() {
		// $P{...} keeps the original DefaultingVar "," separator semantics; no ':' translation.
		assertEquals("default-val", VarResolver.DEFAULT.resolve("$P{" + K2 + ",default-val}"));
	}

	@Test
	void d03_dollarP_colonNotTranslated() {
		// For explicit $P{...}, ':' is NOT a separator; the whole body is treated as a single key.
		// Result is "" because key "k2:fallback" doesn't exist.
		Settings.get().unsetGlobal(K2);
		assertEquals("", VarResolver.DEFAULT.resolve("$P{" + K2 + ":fallback}"));
	}

	@Test
	void d04_dollarS_systemPropertyVar_unchanged() {
		System.setProperty("DollarBraceShortcut_Test.d04", "sys-val");
		try {
			assertEquals("sys-val", VarResolver.DEFAULT.resolve("$S{DollarBraceShortcut_Test.d04}"));
		} finally {
			System.clearProperty("DollarBraceShortcut_Test.d04");
		}
	}

	//====================================================================================================
	// Escape semantics.
	//====================================================================================================

	@Test
	void e01_escapedDollar_keepsLiteral() {
		assertEquals("${literal}", VarResolver.DEFAULT.resolve("\\${literal}"));
	}

	//====================================================================================================
	// Embedded inside surrounding text (streaming path).
	//====================================================================================================

	@Test
	void f01_embeddedInText() {
		Settings.get().setGlobal(K1, "World");
		assertEquals("Hello, World!", VarResolver.DEFAULT.resolve("Hello, ${" + K1 + "}!"));
	}

	@Test
	void f02_embeddedWithDefault() {
		assertEquals("Hello, stranger!", VarResolver.DEFAULT.resolve("Hello, ${" + K1 + ":stranger}!"));
	}

	@Test
	void f03_multipleVarsInOneString() {
		Settings.get().setGlobal(K1, "alpha");
		Settings.get().setGlobal(K2, "beta");
		assertEquals("alpha/beta",
			VarResolver.DEFAULT.resolve("${" + K1 + "}/${" + K2 + "}"));
	}
}
