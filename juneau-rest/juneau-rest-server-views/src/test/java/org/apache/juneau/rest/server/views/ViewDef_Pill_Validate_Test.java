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
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Serving-path fail-closed validation for {@code pill} columns in {@link ViewDef#validate()}: an action-bound pill
 * must name a declared {@link ViewDef#rowActions} id, an explicit tone must be one of the five status tones, and an
 * action-bound pill cannot also carry a {@link CellPopover}.  Display-only pills are never gated.
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
		view(Column.of("state").render(Render.pill("success").meta("field", "state"))).validate();
	}

	@Test void a02_actionPill_namingDeclaredRowAction_passes() {
		view(Column.of("state").render(Render.pill().meta("action", "ack")), ack()).validate();
	}

	@Test void a03_actionPill_namingMissingRowAction_throws() {
		var v = view(Column.of("state").render(Render.pill().meta("action", "nope")), ack());
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("nope"), e::getMessage);
	}

	/**
	 * Off-palette tones fail closed.  The list deliberately includes the entire retired v1 pill palette
	 * ({@code ok}/{@code warn}/{@code exceeds}) and the {@link org.apache.juneau.rest.server.widgets.Tone} overlay
	 * names ({@code accent}/{@code danger}/{@code warn}), which are a different palette on a different surface and
	 * are not status tones.  Case matters: the wire tokens are lowercase.
	 */
	@Test void a04_badTone_throws() {
		for (var tone : new String[]{"ok", "warn", "exceeds", "accent", "danger", "INFO", "Success", "bogus"}) {
			var v = view(Column.of("state").render(Render.of("pill").meta("tone", tone)));
			var e = assertThrows(IllegalArgumentException.class, v::validate, tone);
			assertTrue(e.getMessage().contains(tone), e::getMessage);
		}
	}

	@Test void a05_validTones_pass() {
		for (var tone : new String[]{"info", "success", "warning", "error", "neutral"})
			view(Column.of("state").render(Render.pill(tone).meta("field", "state"))).validate();
	}

	@Test void a06_actionPill_withPopover_throws() {
		var popover = CellPopover.of(PopoverField.of("detail"));
		var render = Render.pill().meta("action", "ack").popover(popover);
		var v = view(Column.of("state").render(render), ack());
		var e = assertThrows(IllegalArgumentException.class, v::validate);
		assertTrue(e.getMessage().contains("popover"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) An action is OPTIONAL on the cell path.  A display-only pill - with or without metadata - stays legal.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_cellPill_withNoActionMeta_stillValidates() {
		// No meta at all: validatePill early-returns.
		view(Column.of("state").render(Render.pill())).validate();
		// Meta present but no "action" key.
		view(Column.of("state").render(Render.pill().meta("field", "state").meta("dot", "off"))).validate();
		// A blank action is treated as absent, not as a dangling reference.
		view(Column.of("state").render(Render.pill().meta("action", "   "))).validate();
	}

	@Test void b02_cellPill_withNoActionMeta_rendersDisplayOnly() {
		// The emitted VIEW_META carries no action key, so the client renderer paints no role/tabindex affordance.
		var r = Render.pill("info").meta("field", "state");
		assertFalse(r.meta.containsKey("action"));
		var markup = Html.of(ViewTable.of(view(Column.of("state").render(r)), List.of(Map.of("state", "open"))));
		assertFalse(markup.contains("role=\"button\""), markup);
		assertFalse(markup.contains("data-juneau-action"), markup);
	}

	@Test void b03_cellPill_withNoActionButAPopover_isStillLegal() {
		// The action/popover mutual exclusion only bites when an action is actually declared.
		var render = Render.pill("neutral").popover(CellPopover.of(PopoverField.of("detail")));
		view(Column.of("state").render(render)).validate();
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) An unrecognized meta.select is IGNORED, not rejected - pills are not part of the selection protocol.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_cellPill_withSelectMeta_isIgnoredNotFailedClosed() {
		view(Column.of("state").render(Render.pill().meta("select", "true"))).validate();
		view(Column.of("state").render(Render.pill("info").meta("select", "row").meta("field", "state"))).validate();
		// An action-bound pill that also (pointlessly) declares select is still judged only on its action.
		view(Column.of("state").render(Render.pill().meta("action", "ack").meta("select", "true")), ack()).validate();
	}

	@Test void c02_selectNotice_isLoggedAtMostOncePerJvm() {
		var log = Logger.getLogger(ViewDef.class.getName());
		var records = new CopyOnWriteArrayList<LogRecord>();
		var handler = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no buffering to flush; records() is capture-only */ }
			@Override public void close() { /* nothing to release; the handler is removed in the finally below */ }
		};
		log.addHandler(handler);
		try {
			// Whether or not an earlier test already tripped the one-shot guard, validating repeatedly here must
			// never add a second record: the guard is a JVM-wide compareAndSet, not a per-call check.
			for (var i = 0; i < 5; i++)
				view(Column.of("state").render(Render.pill().meta("select", "true"))).validate();
			assertTrue(records.size() <= 1, () -> "meta.select notice logged " + records.size() + " times");
			for (var r : records)
				assertTrue(r.getMessage().contains("meta.select"), r::getMessage);
		} finally {
			log.removeHandler(handler);
		}
	}

	@Test void c03_noSelectKeyIsIntroducedIntoThePillVocabulary() {
		// Render has no select() factory/sugar; select is only ever an unrecognized author-supplied meta key.
		for (var m : Render.class.getMethods())
			assertNotEquals("select", m.getName(), "Render must not grow a select() pill affordance");
	}
}
