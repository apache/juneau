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
package org.apache.juneau.config.mod;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Mod_Test extends TestBase {

	//====================================================================================================
	// Mod.NO_OP
	//====================================================================================================

	@Test void a01_noOp_getId() {
		assertEquals(' ', Mod.NO_OP.getId());
	}

	@Test void a02_noOp_apply() {
		assertEquals("foo", Mod.NO_OP.apply("foo"));
	}

	@Test void a03_noOp_remove() {
		assertEquals("bar", Mod.NO_OP.remove("bar"));
	}

	@Test void a04_noOp_isApplied() {
		assertTrue(Mod.NO_OP.isApplied("anything"));
	}

	@Test void a05_noOp_doApply_alreadyApplied() {
		// NO_OP always reports isApplied=true so doApply returns value unchanged
		assertEquals("hello", Mod.NO_OP.doApply("hello"));
	}

	@Test void a06_noOp_doRemove_applied() {
		// NO_OP always reports isApplied=true so doRemove calls remove(value)
		assertEquals("hello", Mod.NO_OP.doRemove("hello"));
	}

	//====================================================================================================
	// Custom Mod
	//====================================================================================================

	@Test void b01_customMod_getId() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals('x', mod.getId());
	}

	@Test void b02_customMod_apply() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("[hello]", mod.apply("hello"));
	}

	@Test void b03_customMod_remove() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("hello", mod.remove("[hello]"));
	}

	@Test void b04_customMod_isApplied_true() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertTrue(mod.isApplied("[hello]"));
	}

	@Test void b05_customMod_isApplied_false() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertFalse(mod.isApplied("hello"));
	}

	@Test void b06_customMod_doApply_notApplied() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("[hello]", mod.doApply("hello"));
	}

	@Test void b07_customMod_doApply_alreadyApplied() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("[hello]", mod.doApply("[hello]"));
	}

	@Test void b08_customMod_doRemove_applied() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("hello", mod.doRemove("[hello]"));
	}

	@Test void b09_customMod_doRemove_notApplied() {
		var mod = new Mod('x', v -> "[" + v + "]", v -> v.substring(1, v.length() - 1), v -> v.startsWith("[") && v.endsWith("]"));
		assertEquals("hello", mod.doRemove("hello"));
	}

	//====================================================================================================
	// XorEncodeMod
	//====================================================================================================

	@Test void c01_xor_getId() {
		assertEquals('*', XorEncodeMod.INSTANCE.getId());
	}

	@Test void c02_xor_isApplied_encoded() {
		var encoded = XorEncodeMod.INSTANCE.apply("hello");
		assertTrue(XorEncodeMod.INSTANCE.isApplied(encoded));
	}

	@Test void c03_xor_isApplied_notEncoded() {
		assertFalse(XorEncodeMod.INSTANCE.isApplied("hello"));
	}

	@Test void c04_xor_isApplied_onlyOpenBrace() {
		assertFalse(XorEncodeMod.INSTANCE.isApplied("{hello"));
	}

	@Test void c05_xor_isApplied_onlyCloseBrace() {
		assertFalse(XorEncodeMod.INSTANCE.isApplied("hello}"));
	}

	@Test void c06_xor_roundTrip() {
		var original = "mySecret123";
		var encoded = XorEncodeMod.INSTANCE.apply(original);
		assertNotEquals(original, encoded);
		assertTrue(encoded.startsWith("{"));
		assertTrue(encoded.endsWith("}"));
		var decoded = XorEncodeMod.INSTANCE.remove(encoded);
		assertEquals(original, decoded);
	}

	@Test void c07_xor_doApply_notApplied() {
		var encoded = XorEncodeMod.INSTANCE.doApply("password");
		assertTrue(XorEncodeMod.INSTANCE.isApplied(encoded));
	}

	@Test void c08_xor_doApply_alreadyApplied() {
		var encoded = XorEncodeMod.INSTANCE.apply("password");
		// doApply should not double-encode
		assertEquals(encoded, XorEncodeMod.INSTANCE.doApply(encoded));
	}

	@Test void c09_xor_doRemove_applied() {
		var encoded = XorEncodeMod.INSTANCE.apply("secret");
		assertEquals("secret", XorEncodeMod.INSTANCE.doRemove(encoded));
	}

	@Test void c10_xor_doRemove_notApplied() {
		assertEquals("plain", XorEncodeMod.INSTANCE.doRemove("plain"));
	}

	@Test void c11_xor_emptyString() {
		var encoded = XorEncodeMod.INSTANCE.apply("");
		assertEquals("", XorEncodeMod.INSTANCE.remove(encoded));
	}
}
