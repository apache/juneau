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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class PageContractSchema_Test extends TestBase {

	private static String fixture(String name) throws IOException {
		try (var in = PageContractSchema_Test.class.getResourceAsStream("/contracts/" + name)) {
			assertNotNull(in, () -> "missing fixture " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"valid-example.json", "valid-minimal.json", "valid-deep.json"})
	void a01_validCorpusPasses(String name) throws Exception {
		var errors = PageContractSchema.get().validate(fixture(name));
		assertTrue(errors.isEmpty(), () -> name + ": " + errors);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"invalid-version.json|$.contractVersion: must equal 1",
		"invalid-legacy-version.json|missing required 'contractVersion'",
		"invalid-legacy-version.json|$.version: not allowed",
		"invalid-both-versions.json|$.version: not allowed",
		"invalid-extra-top.json|$.x: not allowed",
		"invalid-missing-cards.json|missing required 'cards'",
		"invalid-navlayout.json|$.navLayout: must be one of",
		"invalid-id-pattern.json|does not match",
		"invalid-nav-leaf.json|matches none of anyOf",
		"invalid-nav-empty-children.json|matches none of anyOf",
		"invalid-slot-name.json|{Brand}",
		"invalid-header-slot-enum.json|must be one of",
		"invalid-footer-both.json|forbidden shape (not)",
		"invalid-initials.json|longer than 3",
		"invalid-html-neither.json|oneOf",
		"invalid-html-extra.json|oneOf",
		"invalid-datatables-notable.json|oneOf",
		"invalid-card-type.json|does not match",
		"invalid-r1.json|R-1: duplicate nav id 'a'",
		"invalid-r2.json|R-2: duplicate card id 'c'",
		"invalid-r3.json|failed at 'b'",
	})
	void a02_invalidCorpusFails(String name, String expected) throws Exception {
		var errors = PageContractSchema.get().validate(fixture(name));
		assertTrue(errors.stream().anyMatch(e -> e.contains(expected)), () -> name + " expected '" + expected + "' in " + errors);
	}

	@Test void a03_r4_danglingTemplate() throws Exception {
		var c = fixture("valid-example.json");
		assertEquals(List.of(), PageContractSchema.get().validate(c, Set.of("header.banner", "jc-seg-1")));
		var errors = PageContractSchema.get().validate(c, Set.of("header.banner"));
		assertEquals(List.of("R-4: template 'jc-seg-1' is referenced but not present"), errors);
	}

	@Test void a04_unparseable() {
		var errors = PageContractSchema.get().validate("{nope");
		assertList(errors, (Predicate<String>) e -> e.startsWith("$: unparseable JSON"));
	}
}
