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
package org.apache.juneau.commons.svl.vars;

import static org.junit.jupiter.api.Assertions.*;

import java.util.jar.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

class PropertyVars_Test extends TestBase {

	//====================================================================================================
	// ArgsVar
	//====================================================================================================

	@Test
	void a01_argsVar_create_resolvesPositional() {
		var vr = VarResolver.create().vars(ArgsVar.create(() -> new Args("hello world"))).build();
		assertEquals("hello", vr.resolve("$A{0}"));
		assertEquals("world", vr.resolve("$A{1}"));
	}

	@Test
	void a02_argsVar_create_resolvesNamed() {
		var vr = VarResolver.create().vars(ArgsVar.create(() -> new Args("-port 9999"))).build();
		assertEquals("9999", vr.resolve("$A{port}"));
	}

	@Test
	void a03_argsVar_create_missingKey_returnsDefault() {
		var vr = VarResolver.create().vars(ArgsVar.create(() -> new Args("-port 9999"))).build();
		assertEquals("defaultVal", vr.resolve("$A{host,defaultVal}"));
	}

	@Test
	void a04_argsVar_create_withSupplier() {
		var src = ArgsVar.create(() -> new Args("-env staging"));
		var vr = VarResolver.create().vars(src).build();
		assertEquals("staging", vr.resolve("$A{env}"));
	}

	@Test
	void a05_argsVar_create_missingKey_returnsDefault() {
		var src = ArgsVar.create(() -> new Args("-env staging"));
		var vr = VarResolver.create().vars(src).build();
		assertEquals("prod", vr.resolve("$A{region,prod}"));
	}

	//====================================================================================================
	// ManifestFileVar
	//====================================================================================================

	@Test
	void b01_manifestFileVar_create_resolvesKey() {
		var manifest = new Manifest();
		manifest.getMainAttributes().putValue("My-Attr", "from-manifest");
		var vr = VarResolver.create().vars(ManifestFileVar.create(() -> new ManifestFile(manifest))).build();
		assertEquals("from-manifest", vr.resolve("$MF{My-Attr}"));
	}

	@Test
	void b02_manifestFileVar_create_missingKey_returnsEmptyString() {
		// ManifestFileVar.resolve() returns "" (not null) for missing keys,
		// so DefaultingVar does not apply the default — empty string is returned.
		var manifest = new Manifest();
		manifest.getMainAttributes().putValue("My-Attr", "x");
		var vr = VarResolver.create().vars(ManifestFileVar.create(() -> new ManifestFile(manifest))).build();
		assertEquals("", vr.resolve("$MF{Missing-Attr}"));
		assertEquals("", vr.resolve("$MF{Missing-Attr,ignored-default}"));
	}

	@Test
	void b03_manifestFileVar_create_withSupplier() {
		var manifest = new Manifest();
		manifest.getMainAttributes().putValue("Version", "1.2.3");
		var mf = new ManifestFile(manifest);
		var src = ManifestFileVar.create(() -> mf);
		var vr = VarResolver.create().vars(src).build();
		assertEquals("1.2.3", vr.resolve("$MF{Version}"));
	}

	@Test
	void b04_manifestFileVar_nullManifest_returnsEmptyString() {
		var src = ManifestFileVar.create(() -> null);
		var vr = VarResolver.create().vars(src).build();
		assertEquals("", vr.resolve("$MF{Main-Class}"));
	}

	//====================================================================================================
	// PropertyVar
	//====================================================================================================

	@Test
	void c01_propertyVar_resolvesFromSystemProperty() {
		System.setProperty("PropertyVars_Test.c01", "sysval");
		try {
			var vr = VarResolver.create().vars(PropertyVar.class).build();
			assertEquals("sysval", vr.resolve("$P{PropertyVars_Test.c01}"));
		} finally {
			System.clearProperty("PropertyVars_Test.c01");
		}
	}

	@Test
	void c02_propertyVar_missingKey_returnsDefault() {
		System.clearProperty("PropertyVars_Test.c02.missing");
		var vr = VarResolver.create().vars(PropertyVar.class).build();
		assertEquals("defval", vr.resolve("$P{PropertyVars_Test.c02.missing,defval}"));
	}

	@Test
	void c03_propertyVar_fromSettings() {
		Settings.get().setGlobal("PropertyVars_Test.c03", "overrideVal");
		try {
			var vr = VarResolver.create().vars(PropertyVar.class).build();
			assertEquals("overrideVal", vr.resolve("$P{PropertyVars_Test.c03}"));
		} finally {
			Settings.get().unsetGlobal("PropertyVars_Test.c03");
		}
	}
}
