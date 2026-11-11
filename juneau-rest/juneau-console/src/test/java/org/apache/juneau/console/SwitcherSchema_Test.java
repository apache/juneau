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

	@Test void style_a01_enumInSchema() throws Exception {
		var base = fixture("valid-switcher.json");
		var slider = base.replace("\"initialView\":\"a\",", "\"initialView\":\"a\",\"style\":\"slider\",");
		assertNotEquals(base, slider);
		assertTrue(PageContractSchema.get().validate(slider).isEmpty());
		assertFalse(PageContractSchema.get().validate(slider.replace("slider", "pills")).isEmpty());
	}

	@Test void dataTab_a01_booleanInSchema() throws Exception {
		var base = fixture("valid-chart-src.json");
		var on = base.replace("\"legend\":\"bottom\"", "\"legend\":\"bottom\",\"dataTab\":true");
		assertNotEquals(base, on);
		assertTrue(PageContractSchema.get().validate(on).isEmpty());
		assertFalse(PageContractSchema.get().validate(on.replace("\"dataTab\":true", "\"dataTab\":\"yes\"")).isEmpty());
	}

	@Test void symbol_a05_schemaAllowsSymbol() throws Exception {
		var base = fixture("valid-example.json");
		var logo = "\"logo\": { \"src\": \"/img/sandbox-console-logo.svg\", \"href\": \"/\", \"alt\": \"Support Enablement Console\" }";
		assertTrue(base.contains(logo));
		assertTrue(PageContractSchema.get().validate(base.replace(logo, "\"logo\": { \"symbol\": \"paw_print\" }")).isEmpty());
		assertFalse(PageContractSchema.get().validate(base.replace(logo, "\"logo\": { \"symbol\": \"Bad\" }")).isEmpty());
		assertFalse(PageContractSchema.get().validate(base.replace(logo, "\"logo\": { \"symbol\": \"paw_print\", \"src\": \"/a.svg\" }")).isEmpty());
	}

	@Test void cards_a01_schema() throws Exception {
		var base = fixture("valid-switcher.json");
		var stacked = base.replace("\"card\":{\"id\":\"ca\",\"type\":\"html\",\"template\":\"t\"}",
			"\"cards\":[{\"id\":\"ca\",\"type\":\"html\",\"template\":\"t\"},{\"id\":\"ca2\",\"type\":\"html\",\"template\":\"t\"}]");
		assertNotEquals(base, stacked);
		assertEquals(List.of(), PageContractSchema.get().validate(stacked));
		var both = stacked.replace("\"cards\":[", "\"card\":{\"id\":\"cx\",\"type\":\"html\",\"template\":\"t\"},\"cards\":[");
		assertFalse(PageContractSchema.get().validate(both).isEmpty());
		var one = stacked.replace(",{\"id\":\"ca2\",\"type\":\"html\",\"template\":\"t\"}", "");
		assertNotEquals(stacked, one);
		assertFalse(PageContractSchema.get().validate(one).isEmpty());
	}
}
