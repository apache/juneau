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

/** {@link NestedSlot}: the nested-table slot emitted for a table tab. */
@SuppressWarnings("unchecked")
class NestedSlot_Test extends TestBase {

	private static TableSpec orders(String id) {
		return TableSpec.create(id).dataUrl("/orders/rows").columns(Column.create("id"), Column.create("status"));
	}

	@Test void a01_shape() {
		var n = NestedSlot.of(orders("orders"), "petId");
		assertEquals("2", n.get("contractVersion"));
		assertEquals("petId", n.get("parentScopeParam"));
		var view = (Map<String,Object>) n.get("view");
		assertEquals("orders", view.get("id"));
		assertNotNull(view.get("columns"));
		var meta = (Map<String,Object>) n.get("meta");
		assertEquals(ViewsMixin.CONTRACT_VERSION, meta.get("contractVersion"));
		assertEquals("/orders/rows", meta.get("dataUrl"));
		var col = ((List<Map<String,Object>>) meta.get("columns")).get(0);
		assertEquals("id", col.get("data"));
		assertNull(col.get("key"));
	}

	@Test void a02_noRowsDataUrl_rejected() {
		assertThrows(IllegalArgumentException.class, () -> NestedSlot.of(TableSpec.create("t"), "petId"));
	}

	@Test void a03_depthOver2_rejected() {
		var inner = orders("inner");
		var mid = orders("mid").detail(RowDetail.inline().tab(DetailTab.table("Inner", inner)));
		assertThrows(IllegalArgumentException.class, () -> NestedSlot.of(mid, "petId"));
	}

	@Test void a04_duplicateViewId_rejected() {
		var d = RowDetail.inline()
			.tab(DetailTab.table("A", orders("same")).id("a"))
			.tab(DetailTab.table("B", orders("same")).id("b"));
		assertThrows(IllegalArgumentException.class, d::toMap);
	}

	@Test void a05_selectionAndDetailCarried() {
		var t = orders("o").selection(SelectionDef.create("id")).detail(RowDetail.inline());
		var n = NestedSlot.of(t, "petId");
		assertNotNull(n.get("selection"));
		assertNotNull(n.get("detail"));
	}

	@Test void a06_tableTab_emitsNestedAndRowsCount() {
		var m = DetailTab.table("Orders", orders("o")).countFromRows().toMap();
		assertEquals("rows", m.get("count"));
		assertNotNull(m.get("nested"));
	}
}
