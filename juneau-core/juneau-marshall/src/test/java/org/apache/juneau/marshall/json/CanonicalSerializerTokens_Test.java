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
package org.apache.juneau.marshall.json;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.jcs.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the {@code serializeTokens(...)} overrides on the canonical-JSON serializers
 * (JCS, JSON-Schema), whose token writer emits raw JSON with the POJO {@code object(...)} bridge
 * disabled (canonical output can't be expressed through the generic walker).
 */
@SuppressWarnings({
	"resource" // Token writers wrap in-memory StringBuilders; nothing to clean up.
})
class CanonicalSerializerTokens_Test extends TestBase {

	@Test void a01_jcsSerializeTokensEmitsRawJson() throws Exception {
		var sb = new StringBuilder();
		try (var w = JcsSerializer.DEFAULT.serializeTokens(sb)) {
			w.startObject();
			w.fieldName("a"); w.number(1L);
			w.endObject();
		}
		assertEquals("{\"a\":1}", sb.toString());
	}

	@Test void a02_jcsSerializeTokensDisablesObjectBridge() throws Exception {
		try (var w = JcsSerializer.DEFAULT.serializeTokens(new StringBuilder())) {
			assertThrows(UnsupportedOperationException.class, () -> w.object(1));
		}
	}

	@Test void a03_jsonSchemaSerializeTokensEmitsRawJson() throws Exception {
		var sb = new StringBuilder();
		try (var w = JsonSchemaSerializer.DEFAULT.serializeTokens(sb)) {
			w.startArray();
			w.number(1L);
			w.number(2L);
			w.endArray();
		}
		assertEquals("[1,2]", sb.toString());
	}

	@Test void a04_jsonSchemaSerializeTokensDisablesObjectBridge() throws Exception {
		try (var w = JsonSchemaSerializer.DEFAULT.serializeTokens(new StringBuilder())) {
			assertThrows(UnsupportedOperationException.class, () -> w.object(1));
		}
	}
}
