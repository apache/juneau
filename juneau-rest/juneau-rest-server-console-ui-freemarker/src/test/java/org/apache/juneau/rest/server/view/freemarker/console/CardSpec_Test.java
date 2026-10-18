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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CardSpec_Test extends TestBase {

	@Test void a01_htmlToMap_keyOrder() {
		var c = CardSpec.html("intro").title("Welcome").template("intro");
		assertEquals("{id=intro, type=html, title=Welcome, template=intro}", c.toMap().toString());
	}

	@Test void a02_bareOnlyWhenTrue() {
		assertEquals("{id=s, type=html, template=s, bare=true}", CardSpec.html("s").template("s").bare(true).toMap().toString());
		assertFalse(CardSpec.html("s").template("s").toMap().containsKey("bare"));
	}

	@Test void a02b_bareRefusedWhereSchemaHasNoBare() {
		for (var type : List.of("run-view", "terminal")) {
			var target = CardSpec.of(type, "x");
			var ex = assertThrows(IllegalArgumentException.class, () -> target.bare(true));
			assertTrue(ex.getMessage().contains("card 'x': a " + type + " card does not support bare."), ex.getMessage());
			assertFalse(CardSpec.of(type, "x").bare(false).toMap().containsKey("bare"));
		}
		for (var type : List.of("html", "datatables", "chart", "job-log", "meter"))
			assertEquals(true, CardSpec.of(type, "x").bare(true).toMap().get("bare"));
	}

	@Test void a03_bodyFieldsAfterCommonFields() {
		var c = CardSpec.of("datatables", "releases").body("table", Map.of("dataUrl", "/x")).title("Releases");
		assertEquals("{id=releases, type=datatables, title=Releases, table={dataUrl=/x}}", c.toMap().toString());
		assertBean(c, "type,id", "datatables,releases");
	}

	@Test void a04_sanitizeOnlyWhenTrue() {
		var c = CardSpec.html("frag").src("/rest/frag").sanitize(true);
		assertEquals("{id=frag, type=html, src=/rest/frag, sanitize=true}", c.toMap().toString());
		assertEquals("{id=frag, type=html, src=/rest/frag, sanitize=true}", c.copy().toMap().toString());
		assertFalse(CardSpec.html("frag").src("/rest/frag").toMap().containsKey("sanitize"));
		assertFalse(CardSpec.html("frag").src("/rest/frag").sanitize(true).sanitize(false).toMap().containsKey("sanitize"));
	}

	@Test void a05_sanitizeNeedsAnHtmlCardWithSrc() {
		var noSrc = CardSpec.html("frag").template("intro").sanitize(true);
		assertThrows(IllegalArgumentException.class, noSrc::toMap);
		var notHtml = CardSpec.of("datatables", "t").src("/rest/data").sanitize(true);
		assertThrows(IllegalArgumentException.class, notHtml::toMap);
		assertEquals("{id=frag, type=html, src=/rest/frag, sanitize=true}", CardSpec.html("frag").sanitize(true).src("/rest/frag").toMap().toString());
		assertEquals("{id=frag, type=html, template=intro}", CardSpec.html("frag").template("intro").toMap().toString());
	}
}
