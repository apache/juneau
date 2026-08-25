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
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * {@link CellPopover} / {@link PopoverField} validation, serialization, and serving-path wiring.
 */
class CellPopover_Test extends TestBase {

	@Test void a01_validate_emptyFieldsRejected() {
		var p = CellPopover.of();
		var e = assertThrows(IllegalArgumentException.class, p::validate);
		assertTrue(e.getMessage().contains("at least one field"), e::getMessage);
	}

	@Test void a02_validate_blankDataRejected() {
		assertThrows(IllegalArgumentException.class, () -> PopoverField.of(""));
		assertThrows(IllegalArgumentException.class, () -> PopoverField.of(null));
	}

	@Test void a03_validate_duplicateDataRejected() {
		var p = CellPopover.of(PopoverField.of("a"), PopoverField.of("a"));
		var e = assertThrows(IllegalArgumentException.class, p::validate);
		assertTrue(e.getMessage().contains("duplicate"), e::getMessage);
	}

	@Test void a04_validate_illegalRenderIdsRejected() {
		for (var id : new String[]{ "tag", "linked", "progress", "json", "truncate", "custom" }) {
			var p = CellPopover.of(PopoverField.of("a").render(id));
			var e = assertThrows(IllegalArgumentException.class, p::validate);
			assertTrue(e.getMessage().contains(id), e::getMessage);
		}
	}

	@Test void a05_validate_textShapedRenderAccepted() {
		for (var id : new String[]{ "date", "bool", "decimal", "datetime", "ts-zulu" })
			CellPopover.of(PopoverField.of("a").render(id)).validate();
	}

	@Test void a06_servingPath_viewTableOfThrows() {
		var v = ViewDef.create("x").dataMode(DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("used").render(Render.of("progress").popover(CellPopover.of())))
			.build();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
	}

	@Test void a07_jsonRoundTrip_fieldsAndRender() {
		var json = Json.of(CellPopover.of(
			PopoverField.of("actual").title("Actual"),
			PopoverField.of("created").title("Created").render("date")));
		assertTrue(json.contains("\"data\":\"actual\""), json);
		assertTrue(json.contains("\"title\":\"Actual\""), json);
		assertTrue(json.contains("\"id\":\"date\""), json);
	}

	@Test void a08_contractVersionUnchanged() {
		assertEquals("4", ViewDef.CONTRACT_VERSION);
	}
}
