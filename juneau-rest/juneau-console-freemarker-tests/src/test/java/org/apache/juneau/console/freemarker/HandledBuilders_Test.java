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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.apache.juneau.console.views.*;
import org.junit.jupiter.api.*;

/**
 * Tests the {@link Handled} mixin on the page element builders and the model keys of the page contract.
 */
class HandledBuilders_Test extends TestBase {

	private static String json(PageSpec ps, CardSpec... extra) throws Exception {
		var cap = new PageCapture();
		cap.title("T");
		ps.applyTo(cap, null);
		for (var c : extra)
			cap.addCard(c, null);
		return cap.toContractJson();
	}

	private static void valid(String j) {
		var errs = PageContractSchema.get().validate(j);
		assertTrue(errs.isEmpty(), () -> errs + " in " + j);
	}

	@Test void a01_cardModel() {
		var m = CardSpec.html("c1").action("a", "x", "orders.archive").on("rendered", "orders.wire").subscribe("selection:t", "orders.sel").toMap();
		assertTrue(m.get("model").toString().contains("orders.archive"), m.toString());
	}

	@Test void a02_cardBodyRejectsModel() {
		assertThrows(IllegalArgumentException.class, () -> CardSpec.html("c1").body("model", "x"));
	}

	@Test void a03_cardCopyKeepsModel() {
		assertNotNull(CardSpec.html("c1").on("loaded", "a.b").copy().toMap().get("model"));
	}

	@Test void a04_tableModel() {
		var t = TableSpec.create("t1").dataUrl("/d").columns(Column.create("id")).on("loaded", "a.b");
		assertTrue(t.toCardBody().toString().contains("\"model\""));
	}

	@Test void a05_rowDetailActionsAndOn() {
		var m = RowDetail.create("/d/{id}").action("go", "i", "a.go").on("rendered", "a.r").toMap().toString();
		assertTrue(m.contains("\"actions\":[{\"type\":\"handler\""), m);
		assertTrue(m.contains("\"model\":{\"on\""), m);
	}

	@Test void b01_scriptAliasDedupes() {
		var ps = PageSpec.create().init("/a.js").script("/a.js", "/b.js");
		assertEquals(java.util.List.of("/a.js", "/b.js"), ps.init);
	}

	@Test void b02_rootOnLoaded() throws Exception {
		var j = json(PageSpec.create().on("loaded", "app.boot"));
		assertTrue(j.contains("\"modelVersion\":\"1\""), j);
		assertTrue(j.contains("\"model\":{\"on\""), j);
		valid(j);
	}

	@Test void b03_rootOnOtherThrows() {
		assertThrows(IllegalArgumentException.class, () -> PageSpec.create().on("rendered", "a.b"));
	}

	@Test void b04_inlineNeedsOptIn() {
		var ps = PageSpec.create().on("loaded", HandlerRef.inline("return 1"));
		assertThrows(IllegalArgumentException.class, () -> json(ps));
	}

	@Test void b05_inlineWithOptIn() throws Exception {
		var j = json(PageSpec.create().allowInlineHandlers(true).on("loaded", HandlerRef.inline("return 1")));
		assertTrue(j.contains("\"allowInlineHandlers\":true"), j);
		valid(j);
	}

	@Test void b06_cardInlineNeedsOptIn() {
		var ps = PageSpec.create().html("c1", "<p>x</p>", c -> c.on("loaded", HandlerRef.inline("return 1")));
		assertThrows(IllegalArgumentException.class, () -> json(ps));
	}

	@Test void b07_noModelNoKeys() throws Exception {
		var j = json(PageSpec.create());
		assertFalse(j.contains("modelVersion") || j.contains("scripts") || j.contains("allowInline"), j);
	}

	@Test void b08_scriptsEmitted() throws Exception {
		var j = json(PageSpec.create().script("/a.js"));
		assertTrue(j.contains("\"scripts\":[\"/a.js\"]"), j);
		valid(j);
	}

	@Test void b09_cardModelValidates() throws Exception {
		var j = json(PageSpec.create(), CardSpec.html("c1").src("/x").action("a", "x", "o.a"));
		assertTrue(j.contains("\"modelVersion\":\"1\""), j);
		valid(j);
	}
}
