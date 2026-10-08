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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class CardSource_Test {

	@Test void hasJsonBody_sniffsFirstNonWhitespaceChar() {
		var jsonSrc = CardSource.create("kpi", "k").body("  \n {value:3}").build();
		assertTrue(jsonSrc.hasJsonBody());
		var markupSrc = CardSource.create("html", "h").body("<p>hi</p>").build();
		assertFalse(markupSrc.hasJsonBody());
		var emptySrc = CardSource.create("html", "h").build();
		assertFalse(emptySrc.hasJsonBody());
	}

	@Test void json_parsesJson5AndCaches() {
		var calls = new int[1];
		var src = CardSource.create("kpi", "k").body("{value:3, label:'N'}").build();
		var m1 = src.json();
		var m2 = src.json();
		assertSame(m1, m2); // cached, not re-parsed
		assertEquals(3, m1.get("value"));
		assertEquals("N", m1.get("label"));
	}

	@Test void json_onMalformedJson5_throwsE24() {
		var src = CardSource.create("kpi", "k").body("{value: [}").build();
		var ex = assertThrows(IllegalArgumentException.class, src::json);
		// E-24 keeps the C1 'Card JSON5 is invalid: ' substring pinned by PetstoreFailLoud_Test a04 (Auto-decision 8).
		assertTrue(ex.getMessage().startsWith("<@card id='k'> Card JSON5 is invalid: "), ex.getMessage());
	}

	@Test void json_onMarkupBody_throwsE25Shaped() {
		var src = CardSource.create("kpi", "k").body("<p>not json</p>").build();
		var ex = assertThrows(IllegalArgumentException.class, src::json);
		assertEquals("<@card id='k'> type='kpi' requires a JSON5 object body; got '<p>not json</p>'.", ex.getMessage());
	}

	@Test void bodyMap_isTreatedAsAlreadyParsedJson() {
		var src = CardSource.create("kpi", "k").bodyMap(java.util.Map.of("value", 3)).build();
		assertTrue(src.hasJsonBody());
		assertEquals(3, src.json().get("value"));
	}

	@Test void captureTemplate_callsSinkAndReturnsId() {
		var captured = new String[2];
		var src = CardSource.create("html", "h")
			.body("<p>hi</p>")
			.templateSink((id, markup) -> { captured[0] = id; captured[1] = markup; })
			.build();
		assertEquals("h", src.captureTemplate());
		assertEquals("h", captured[0]);
		assertEquals("<p>hi</p>", captured[1]);
	}

	@Test void captureTemplate_withNoSink_throws() {
		var src = CardSource.create("html", "h").body("<p>hi</p>").build();
		assertThrows(IllegalArgumentException.class, src::captureTemplate);
	}

	@Test void error_prefixesCardId() {
		var src = CardSource.create("kpi", "k").build();
		var ex = src.error("needs '%s'.", "value");
		assertEquals("<@card id='k'> needs 'value'.", ex.getMessage());
	}

	@Test void create_rejectsBlankTypeOrId() {
		assertThrows(IllegalArgumentException.class, () -> CardSource.create("", "k"));
		assertThrows(IllegalArgumentException.class, () -> CardSource.create("kpi", " "));
	}
}
