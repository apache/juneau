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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins the keyword list that the client-side port of the subset validator ({@code juneau-model.js SCHEMA_KEYWORDS},
 * compared against the same file by {@code model.cjs}) must implement to {@link SchemaSubsetValidator#KEYWORDS}.
 */
class SchemaSubsetKeywords_Parity_Test extends TestBase {

	private static final Path LIST = Path.of("../juneau-console-views/src/test/resources/schema-subset-keywords.json");

	@Test void javaKeywordsMatchTheSharedListTheJsPortIsCheckedAgainst() throws Exception {
		assertTrue(Files.exists(LIST), () -> "missing " + LIST.toAbsolutePath());
		var m = Pattern.compile("\"([^\"]+)\"").matcher(Files.readString(LIST));
		var listed = new TreeSet<String>();
		while (m.find())
			listed.add(m.group(1));
		assertEquals(new TreeSet<>(SchemaSubsetValidator.KEYWORDS), listed,
			"schema-subset-keywords.json must equal SchemaSubsetValidator.KEYWORDS; update both the file and juneau-model.js SCHEMA_KEYWORDS");
	}
}
