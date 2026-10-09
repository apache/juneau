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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.Op;
import org.junit.jupiter.api.*;

/** {@link TableSpec}: builder shape, eager checks, id grammar, getters. */
class TableSpec_Test extends TestBase {

	@Test void a01_create_minimal() {
		var t = TableSpec.create("slo");
		assertEquals("slo", t.id());
		assertTrue(t.columns().isEmpty());
		assertTrue(t.rowActions().isEmpty());
		assertTrue(t.detail().isEmpty());
	}

	@Test void a02_create_blankId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> TableSpec.create(" "));
		assertEquals("TableSpec id must not be null or blank.", ex.getMessage());
	}

	@Test void a03_create_malformedId_rejected() {
		var ex = assertThrows(IllegalArgumentException.class, () -> TableSpec.create("1bad"));
		assertEquals("TableSpec id '1bad' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.", ex.getMessage());
	}

	@Test void a04_columns_appendAndGetter() {
		var t = TableSpec.create("slo").columns(Column.create("pod"), Column.create("incidentKey"));
		assertEquals(List.of("pod", "incidentKey"), t.columns().stream().map(Column::name).toList());
	}

	@Test void a05_columns_duplicateAcrossCalls_rejected() {
		var t = TableSpec.create("slo").columns(Column.create("pod"));
		var ex = assertThrows(IllegalArgumentException.class, () -> t.columns(Column.create("pod")));
		assertEquals("TableSpec 'slo' declares column 'pod' twice.", ex.getMessage());
	}

	@Test void a06_rowClass_valueOp_wrongOverload_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.rowClass("state", Op.PRESENT, "x", "row-x"));
		assertEquals("TableSpec 'slo' rowClass on 'state': op 'present' takes no value.", ex.getMessage());
	}

	@Test void a07_rowClass_presenceOp_wrongOverload_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.rowClass("state", Op.EQ, "row-x"));
		assertEquals("TableSpec 'slo' rowClass on 'state': op 'eq' requires a value.", ex.getMessage());
	}

	@Test void a08_rowClass_blankField_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.rowClass(" ", Op.EQ, "x", "row-x"));
		assertEquals("TableSpec 'slo' rowClass field must not be null or blank.", ex.getMessage());
	}

	@Test void a09_rowActions_bothOverloads_append() {
		var t = TableSpec.create("slo")
			.rowActions(RowAction.create("a"))
			.rowActions(List.of(RowAction.create("b")));
		assertEquals(List.of("a", "b"), t.rowActions().stream().map(a -> a.id).toList());
	}

	@Test void a10_detail_setterAndGetter() {
		var d = RowDetail.create("/rest/slo/data/{id}");
		var t = TableSpec.create("slo").detail(d);
		assertSame(d, t.detail().orElseThrow());
	}

	@Test void a11_pollIntervalMs_nonPositive_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.pollIntervalMs(0));
		assertEquals("TableSpec 'slo' pollIntervalMs must be positive.", ex.getMessage());
	}

	@Test void a12_quickStats_blankId_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class,
			() -> t.quickStats(" ", QuickStat.of("x", "X", 1)));
		assertEquals("TableSpec quickStats id must not be null or blank.", ex.getMessage());
	}

	@Test void a13_quickStats_noItems_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.quickStats("strip"));
		assertEquals("TableSpec quickStats requires at least one item.", ex.getMessage());
	}

	@Test void a99_rowClass_collectionOp_rejected() {
		var t = TableSpec.create("slo");
		var ex = assertThrows(IllegalArgumentException.class, () -> t.rowClass("state", Op.IN, "x", "row-x"));
		assertEquals("TableSpec 'slo' rowClass on 'state': op 'in' is not supported; use eq, ne, present or absent.", ex.getMessage());
	}

	@Test void a98_cssClass_setterAndGetter() {
		assertNull(TableSpec.create("slo").cssClass());
		assertEquals("wide", TableSpec.create("slo").cssClass("wide").cssClass());
	}
}
