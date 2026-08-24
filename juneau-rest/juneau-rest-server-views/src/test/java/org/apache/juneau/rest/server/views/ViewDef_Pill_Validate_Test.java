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
import org.junit.jupiter.api.*;

/**
 * Serving-path fail-closed validation for {@code pill} columns in {@link ViewDef#validate()}: an action-bound pill
 * must name a declared {@link ViewDef#rowActions} id, an explicit tone must be one of the four tokens (never
 * {@code info}), and an action-bound pill cannot also carry a {@link CellPopover}.  Display-only pills are never gated.
 */
class ViewDef_Pill_Validate_Test extends TestBase {

	private static RowAction ack() {
		return RowAction.create("ack").endpoint("servlet:/ack").method(RowAction.Method.POST);
	}

	private static ViewDef view(Column col, RowAction...actions) {
		return ViewDef.create("v").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u").columns(col).rowActions(actions).build();
	}

	@Test void a01_displayOnlyPill_needsNoRowAction() {
		// A pill with no action is pure presentation and must validate even with an empty action catalog.
		view(Column.of("state").render(Render.pill("ok").meta("field", "state"))).validate();
	}

	@Test void a02_actionPill_namingDeclaredRowAction_passes() {
		view(Column.of("state").render(Render.pill().meta("action", "ack")), ack()).validate();
	}

	@Test void a03_actionPill_namingMissingRowAction_throws() {
		var v = view(Column.of("state").render(Render.pill().meta("action", "nope")), ack());
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("nope"), e::getMessage);
	}

	@Test void a04_badTone_throws_includingInfo() {
		for (var tone : new String[]{"info", "danger", "OK"}) {
			var v = view(Column.of("state").render(Render.of("pill").meta("tone", tone)));
			var e = assertThrows(IllegalArgumentException.class, v::validate, tone);
			assertTrue(e.getMessage().contains(tone), e::getMessage);
		}
	}

	@Test void a05_validTones_pass() {
		for (var tone : new String[]{"ok", "warn", "exceeds", "neutral"})
			view(Column.of("state").render(Render.pill(tone).meta("field", "state"))).validate();
	}

	@Test void a06_actionPill_withPopover_throws() {
		var popover = CellPopover.of(PopoverField.of("detail"));
		var render = Render.pill().meta("action", "ack").popover(popover);
		var v = view(Column.of("state").render(render), ack());
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("popover"), e::getMessage);
	}
}
