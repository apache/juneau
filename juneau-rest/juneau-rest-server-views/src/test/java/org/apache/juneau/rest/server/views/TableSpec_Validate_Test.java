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

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** {@link TableSpec#validate()}: required data, order columns, dialogs, row actions, bulk, server-mode operators. */
class TableSpec_Validate_Test extends TestBase {

	@Test void a01_missingDataUrl_rejected() {
		var t = TableSpec.create("slo").columns(Column.create("pod"));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' requires dataUrl.", ex.getMessage());
	}

	@Test void a02_noColumns_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data");
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' requires at least one column.", ex.getMessage());
	}

	@Test void a03_defaultOrderColumnNotFound_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.defaultOrder("nope", TableSpec.Sort.ASC);
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' defaultOrder column 'nope' is not a column; columns: 'pod'.", ex.getMessage());
	}

	@Test void a04_ribbonDialogMissingFormAndEndpoint_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.ribbon(RibbonItem.dialog("add"));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' ribbon dialog 'add' requires form and endpoint.", ex.getMessage());
	}

	@Test void a05_ribbonDialogWithFormAndEndpoint_passes() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.ribbon(RibbonItem.dialog("add").form("/rest/slo/add-modal").endpoint("/rest/slo/new"));
		assertDoesNotThrow(t::validate);
	}

	@Test void a06_duplicateRowActionId_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.rowActions(RowAction.create("ack"), RowAction.create("ack"));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' declares row action 'ack' twice.", ex.getMessage());
	}

	@Test void a07_bulkWithoutSelection_rejected() {
		var permit = org.apache.juneau.rest.server.views.WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var bulk = BulkMutateDef.create(permit, selection).actions(RowAction.create("ack"));
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.bulk(bulk);
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' sets bulk without selection.", ex.getMessage());
	}

	@Test void a08_bulkWithSelection_passes() {
		var permit = org.apache.juneau.rest.server.views.WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var bulk = BulkMutateDef.create(permit, selection).actions(RowAction.create("ack").endpoint("/rest/slo/ack"));
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod"))
			.selection(selection)
			.bulk(bulk);
		assertDoesNotThrow(t::validate);
	}

	@Test void a09_serverModeAmbiguousOperators_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.dataMode(TableSpec.DataMode.SERVER)
			.columns(Column.create("pod").searchType(SearchType.TEXT));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' column 'pod' sets searchType but dataMode is 'server' and the table "
			+ "declares no operators.", ex.getMessage());
	}

	@Test void a10_serverModeWithTableOperators_passes() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.dataMode(TableSpec.DataMode.SERVER)
			.operators(SearchOperatorSet.standard())
			.columns(Column.create("pod").searchType(SearchType.TEXT));
		assertDoesNotThrow(t::validate);
	}

	@Test void a11_clientModeAmbiguousOperators_passes() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data")
			.columns(Column.create("pod").searchType(SearchType.TEXT));
		assertDoesNotThrow(t::validate);
	}

	@Test void a12_bulkWithNoActions_rejected() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.selection(selection).bulk(BulkMutateDef.create(permit, selection));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' sets bulk with no actions.", ex.getMessage());
	}

	@Test void a13_rowActionWithoutEndpoint_rejected() {
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.rowActions(RowAction.create("ack"));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' row action 'ack' requires an endpoint.", ex.getMessage());
	}

	@Test void a14_bulkActionWithoutEndpoint_rejected() {
		var permit = WritePermit.forCapability("slo:bulk");
		var selection = SelectionDef.create("id");
		var t = TableSpec.create("slo").dataUrl("/rest/slo/data").columns(Column.create("pod"))
			.selection(selection).bulk(BulkMutateDef.create(permit, selection).actions(RowAction.create("ack")));
		var ex = assertThrows(IllegalStateException.class, t::validate);
		assertEquals("TableSpec 'slo' bulk action 'ack' requires an endpoint.", ex.getMessage());
	}
}
