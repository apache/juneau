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
package org.apache.juneau.console;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class SwitcherSchema_Test extends TestBase {

	private static String fixture(String name) throws IOException {
		try (var in = SwitcherSchema_Test.class.getResourceAsStream("/contracts/" + name)) {
			assertNotNull(in, () -> "missing fixture " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_validSwitcher() throws Exception {
		var errors = PageContractSchema.get().validate(fixture("valid-switcher.json"));
		assertTrue(errors.isEmpty(), () -> "unexpected " + errors);
	}

	@ParameterizedTest
	@ValueSource(strings = {"noviews", "emptyviews", "both", "neither", "badid", "extra"})
	void a02_invalidFails(String kind) throws Exception {
		var errors = PageContractSchema.get().validate(fixture("invalid-switcher-" + kind + ".json"));
		assertFalse(errors.isEmpty(), () -> kind + " should fail");
	}

	@Test void a03_nestedSwitcherFailsOnTheNestingRule() throws Exception {
		var errors = PageContractSchema.get().validate(fixture("invalid-switcher-nested.json"));
		assertFalse(errors.isEmpty(), "nested switcher should fail");
		assertEquals(List.of("$.cards[0]: matches 0 of oneOf (expected exactly 1)"), errors);
	}
}
