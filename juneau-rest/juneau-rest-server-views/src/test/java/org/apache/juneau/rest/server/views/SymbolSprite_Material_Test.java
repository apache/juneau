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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the opt-in Material Symbols Outlined sprite: stem ids, Material viewBox, and the recovered
 * git-history path data. Default pack remains {@code juneau-symbols.svg} (Juneau-original).
 */
class SymbolSprite_Material_Test extends TestBase {

	private static final Set<String> EXPECTED_IDS = Set.of(
		"juneau-sym-copy", "juneau-sym-csv", "juneau-sym-spreadsheet", "juneau-sym-pdf",
		"juneau-sym-refresh", "juneau-sym-toggle_column_search", "juneau-sym-collapse_all",
		"juneau-sym-settings", "juneau-sym-first_page", "juneau-sym-chevron_left",
		"juneau-sym-chevronright", "juneau-sym-last_page", "juneau-sym-filter",
		"juneau-sym-chevrondown"
	);

	private static final Pattern SYMBOL_ID_PATTERN = Pattern.compile("<symbol\\s+id=\"([^\"]+)\"");

	private static String svg() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.SYMBOLS_MATERIAL_SVG_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.SYMBOLS_MATERIAL_SVG_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_stemIdSetIsPinned() throws Exception {
		var ids = new LinkedHashSet<String>();
		var m = SYMBOL_ID_PATTERN.matcher(svg());
		while (m.find())
			ids.add(m.group(1));
		assertEquals(EXPECTED_IDS, ids);
	}

	@Test void a02_everySymbolUsesMaterialViewBox() throws Exception {
		var body = svg();
		assertTrue(body.contains("viewBox=\"0 -960 960 960\""), body);
		assertFalse(body.contains("viewBox=\"0 0 24 24\""), body);
	}

	@Test void a03_recoveredMaterialPathData_notBlank() throws Exception {
		var body = svg();
		assertTrue(body.contains("q-33 0-56.5-23.5T280-320"), body);
	}

	@Test void a04_notIrsOrSldsArt() throws Exception {
		var body = svg();
		assertFalse(body.contains("slds-"), body);
		assertFalse(body.contains("irs-icon-overrides"), body);
		assertFalse(body.contains("irs-symbols"), body);
	}
}
