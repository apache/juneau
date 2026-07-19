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
package org.apache.juneau.marshall.marshaller;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Drift/presence + round-trip guard for the Feature-B variant marshaller classes
 * ({@link Json5R}, {@link IniR}, {@link HjsonC}).
 *
 * <p>
 * Asserts, for each variant class:
 * <ul>
 * 	<li>It is a subclass of its format facade, and its {@link #DEFAULT} instance's serializer/parser
 * 		identities match the mapped {@code *Serializer.DEFAULT_*}/{@code <Format>Parser.DEFAULT} constants.
 * 	<li>It <b>redeclares the full static shortcut surface</b> of its base format (so none is silently
 * 		inherited and mis-bound to the base {@code DEFAULT}).
 * 	<li>Round-trip / wiring: {@code <Variant>.of(x)} equals the variant serializer's own output for a
 * 		representative bean, {@code <Variant>.to(<Variant>.of(bean), …)} round-trips, and the output is in
 * 		the expected variant form (readable/multi-line vs. compact/single-line).
 * </ul>
 */
class MarshallerVariantClass_Test extends TestBase {

	/** The full set of static shortcut names a facade may declare. */
	private static final Set<String> SHORTCUT_NAMES = Set.of(
		"of", "to",
		"toTokens", "ofTokens", "toRecords", "ofRecords", "toArrayRecords", "ofArrayRecords");

	static Stream<Arguments> variants() {
		return Stream.of(
			arguments(Json5R.class, Json5.class, Json5Serializer.DEFAULT_READABLE, Json5Parser.DEFAULT),
			arguments(IniR.class, Ini.class, IniSerializer.DEFAULT_READABLE, IniParser.DEFAULT),
			arguments(HjsonC.class, Hjson.class, HjsonSerializer.DEFAULT_COMPACT, HjsonParser.DEFAULT));
	}

	@ParameterizedTest
	@MethodSource("variants")
	void a01_subclassAndWiringIdentity(Class<? extends Marshaller> variant, Class<? extends Marshaller> base, Serializer s, Parser p) throws Exception {
		assertTrue(base.isAssignableFrom(variant), () -> variant.getSimpleName() + " must be a subclass of " + base.getSimpleName());
		var def = (Marshaller) variant.getField("DEFAULT").get(null);
		assertEquals(variant, def.getClass(), () -> variant.getSimpleName() + ".DEFAULT must be an instance of " + variant.getSimpleName());
		assertSame(s, def.getSerializer(), () -> variant.getSimpleName() + ".DEFAULT must be wired to the variant serializer");
		assertSame(p, def.getParser(), () -> variant.getSimpleName() + ".DEFAULT must be wired to the plain format parser");
	}

	@ParameterizedTest
	@MethodSource("variants")
	void a02_fullStaticSurfaceRedeclared(Class<? extends Marshaller> variant, Class<? extends Marshaller> base, @SuppressWarnings("unused") Serializer s, @SuppressWarnings("unused") Parser p) {
		for (var m : base.getDeclaredMethods()) {
			if (!Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers()))
				continue;
			if (!SHORTCUT_NAMES.contains(m.getName()))
				continue;
			assertDoesNotThrow(() -> variant.getDeclaredMethod(m.getName(), m.getParameterTypes()),
				() -> variant.getSimpleName() + " must redeclare the full static surface of " + base.getSimpleName() + " (missing " + m + ")");
		}
	}

	@Test void b01_json5rReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = Json5R.of(bean);
		assertEquals(Json5Serializer.DEFAULT_READABLE.writeToString(bean), out);
		assertTrue(out.contains("\n"), () -> "Json5R output should be multi-line readable but was: " + out);
		var m = Json5R.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b02_inirReadableRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = IniR.of(bean);
		assertEquals(IniSerializer.DEFAULT_READABLE.writeToString(bean), out);
		var m = IniR.to(out, Map.class);
		assertBean(m, "a", "1");
	}

	@Test void b03_hjsoncCompactRoundTrip() throws Exception {
		var bean = Map.of("a", 1);
		var out = HjsonC.of(bean);
		assertEquals(HjsonSerializer.DEFAULT_COMPACT.writeToString(bean), out);
		assertFalse(out.trim().contains("\n"), () -> "HjsonC output should be single-line compact but was: " + out);
		var m = HjsonC.to(out, Map.class);
		assertBean(m, "a", "1");
	}
}
