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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Fails when {@code juneau-page.schema.json} uses a JSON Schema keyword outside the subset that the in-house
 * {@link SchemaSubsetValidator} implements (C1-D2).
 */
class PageContractSchema_KeywordSubset_Test extends TestBase {

	@Test void a01_schemaUsesOnlySupportedKeywords() throws Exception {
		var schema = JsonMap.ofString(PageContractSchema.get().schemaJson());
		var unsupported = new TreeSet<String>();
		walk(schema, unsupported, false);
		assertTrue(unsupported.isEmpty(), () -> "unsupported keywords: " + unsupported);
	}

	@Test void a02_subsetIsTheDocumentedOne() {
		assertEquals(
			SchemaSubsetValidator.KEYWORDS,
			Set.of("$schema", "$id", "$defs", "$ref", "title", "description", "default", "type", "const", "enum",
				"required", "properties", "additionalProperties", "propertyNames", "items", "pattern", "minLength",
				"maxLength", "minItems", "allOf", "anyOf", "oneOf", "not"));
	}

	// Walks schema objects. Inside "properties" and "$defs" the keys are names, not keywords.
	private static void walk(Object node, Set<String> unsupported, boolean namesLevel) {
		if (node instanceof Map<?,?> m) {
			for (var e : m.entrySet()) {
				var k = (String)e.getKey();
				if (! namesLevel && ! SchemaSubsetValidator.KEYWORDS.contains(k))
					unsupported.add(k);
				var childIsNames = ! namesLevel && (eqa(k, "properties", "$defs"));
				walk(e.getValue(), unsupported, childIsNames);
			}
		} else if (node instanceof List<?> l) {
			for (var x : l)
				walk(x, unsupported, false);
		}
	}
}
