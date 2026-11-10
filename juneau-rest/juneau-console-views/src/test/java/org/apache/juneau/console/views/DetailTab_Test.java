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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** {@link DetailTab}: the four tab kinds, id grammar, per-kind validation and the count API. */
@SuppressWarnings("unchecked")
class DetailTab_Test extends TestBase {

	@Test void a01_slugId() {
		assertEquals("price-history", DetailTab.fields("Price history", "a").toMap().get("id"));
	}

	@Test void a02_fieldsTab_rejectsColumnsOutside1to4() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a").columns(5).toMap());
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a").columns(-1).toMap());
		assertEquals(4, DetailTab.fields("D", "a").columns(4).toMap().get("columns"));
	}

	@Test void a03_fieldsTab_duplicateFieldWithinTab() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a", "a").toMap());
	}

	@Test void a04_group_fieldOutsideTab_orInTwoGroups() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a").group("G", "b").toMap());
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a").group("G1", "a").group("G2", "a").toMap());
	}

	@Test void a05_clientRendererRegex() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.client("C", "bad name").toMap());
		assertEquals("client", DetailTab.client("C", "pet-labels").toMap().get("kind"));
	}

	@Test void a06_countApi_rowsAndFunction() {
		var rows = TableSpec.create("o").dataUrl("/o/rows").columns(Column.create("id"));
		assertEquals("rows", DetailTab.table("D", rows).countFromRows().toMap().get("count"));
		assertNull(DetailTab.fields("D","a").count(id -> 3).toMap().get("count"));   // server counters ride `counts`, not a flag
	}

	@Test void a07_tabIdGrammar() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D","a").id("Bad Id").toMap());
	}

	@Test void a08_fieldsTab_needsAtLeastOneField() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D").toMap());
	}

	@Test void a09_unsafeEndpoint_rejected() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.client("C", "r").endpoint("https://x/y").toMap());
		assertEquals("/a/{id}/labels", DetailTab.client("C", "r").endpoint("/a/{id}/labels").toMap().get("endpoint"));
	}

	@Test void a10_groups_serialise() {
		var m = DetailTab.fields("D", "a", "b", "c").group("G", "a", "b").toMap();
		assertEquals("[{title=G, fields=[a, b]}]", m.get("groups").toString());
	}

	@Test void a11_countFromRows_onlyOnTableTabs() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.client("C", "r").countFromRows().toMap());
	}

	@Test void a12_keyOrder() {
		var m = DetailTab.fields("Details", "a").id("details").icon("link").columns(3).emptyText("none").refreshOnPoll(true).toMap();
		assertEquals("[id, kind, title, icon, columns, fields, emptyText, refreshOnPoll]", m.keySet().toString());
	}

	@Test void h01_detailTab_onHandler_serialisesModelBlock() {
		var m = DetailTab.fields("D", "a").id("d").on("loaded", "pets.onLoaded").toMap();
		var model = (Map<String,Object>)m.get("model");
		var on = (Map<String,Object>)model.get("on");
		assertNotNull(on.get("loaded"));
		assertTrue(String.valueOf(on.get("loaded")).contains("pets.onLoaded"));
	}

	@Test void h02_detailTab_handlerAction_inActions() {
		var m = DetailTab.fields("D", "a").id("d").action("go", "refresh-cw", "pets.go").toMap();
		var acts = (List<Map<String,Object>>)((Map<String,Object>)m.get("model")).get("actions");
		assertEquals("go", acts.get(0).get("id"));
	}

	@Test void h03_detailTab_unknownEvent_rejected() {
		assertThrows(IllegalArgumentException.class, () -> DetailTab.fields("D", "a").on("expanded", "x.y"));
	}

	@Test void h04_detailTab_noModel_noKey() {
		assertNull(DetailTab.fields("D", "a").toMap().get("model"));
	}

	@Test void h05_tableTab_modelOnTab_notOnNestedSlot() {
		var rows = TableSpec.create("o").dataUrl("/o/rows").columns(Column.create("id"));
		var m = DetailTab.table("D", rows).on("activated", "pets.act").toMap();
		assertNotNull(m.get("model"));
		assertNull(((Map<String,Object>)m.get("nested")).get("model"));
	}
}
