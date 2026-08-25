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
 * The fill-sink half of the {@code pill} contract: {@code DetailField.render("pill")} is legal, but a sink pill is
 * unconditionally display-only.
 *
 * <p>
 * The two hosts deliberately disagree about one key and agree about the other.  {@code meta.action} is <b>optional</b>
 * on a {@link Column} and <b>forbidden</b> on a {@link DetailField}, because a fill sink has no {@code rowActions} in
 * scope for an action id to resolve against.  {@code meta.tone} is validated identically on both, so no tone can be
 * legal in a cell and illegal in a sink or vice versa.
 *
 * @see ViewDef_Pill_Validate_Test The cell-host counterpart.
 */
class RowDetailDef_SinkPill_Test extends TestBase {

	private static RowDetailDef sink(Render render) {
		return RowDetailDef.create()
			.endpoint("/data/{id}")
			.sections(DetailSection.create("info", "Info").fields(DetailField.of("state").title("State").render(render)));
	}

	// -----------------------------------------------------------------------------------------------------------
	// a) pill is a legal fill-sink renderer
	// -----------------------------------------------------------------------------------------------------------

	@Test void a01_bareSinkPill_validates() {
		sink(Render.pill()).validate(null);
		sink(Render.of("pill")).validate(null);
	}

	@Test void a02_sinkPill_withNoMetaAtAll_validates() {
		// validateSinkPill early-returns on a null meta, exactly as the cell path does - the string form of
		// DetailField.render(...) never populates one, and that must stay the cheapest legal spelling.
		var f = DetailField.of("state").render("pill");
		assertNull(f.render.meta);
		sink(f.render).validate(null);
	}

	@Test void a03_sinkPill_withDisplayOnlyMeta_validates() {
		sink(Render.pill().meta("field", "state").meta("dot", "off")).validate(null);
	}

	// -----------------------------------------------------------------------------------------------------------
	// b) meta.action is FORBIDDEN on the sink host (a fill sink has no rowActions)
	// -----------------------------------------------------------------------------------------------------------

	@Test void b01_sinkPill_withAction_failsClosed() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> sink(Render.pill().meta("action", "ack")).validate(null));
		assertTrue(e.getMessage().contains("meta.action"), e::getMessage);
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	/** The failure message must name the offending sink, not just the renderer, so an author can find it. */
	@Test void b02_sinkPill_actionFailure_namesTheHost() {
		var e = assertThrows(IllegalArgumentException.class,
			() -> sink(Render.pill().meta("action", "ack")).validate(null));
		assertTrue(e.getMessage().contains("info.state"), e::getMessage);
	}

	/**
	 * Declaring the action on the view does not rescue it.  A sink pill is not "an action pill whose id happens to be
	 * unresolvable" - it is display-only by construction, so the check is unconditional rather than a lookup.
	 */
	@Test void b03_sinkPill_withAction_failsEvenWhenTheActionIsDeclared() {
		var v = ViewDef.create("x").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
			.columns(Column.of("state"))
			.rowActions(RowAction.create("ack").endpoint("/x/{id}").method(RowAction.Method.POST))
			.details(sink(Render.pill().meta("action", "ack")))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v));
		assertTrue(e.getMessage().contains("meta.action"), e::getMessage);
	}

	/** A blank action is an absent action on both hosts - the cell path ignores it, so the sink path must too. */
	@Test void b04_sinkPill_withBlankAction_isTreatedAsAbsent() {
		sink(Render.pill().meta("action", "")).validate(null);
		sink(Render.pill().meta("action", "   ")).validate(null);
	}

	// -----------------------------------------------------------------------------------------------------------
	// c) the tone palette is identical on both hosts
	// -----------------------------------------------------------------------------------------------------------

	@Test void c01_sinkPill_validTones_pass() {
		for (var tone : new String[]{"info", "success", "warning", "error", "neutral"})
			sink(Render.pill(tone)).validate(null);
	}

	@Test void c02_sinkPill_offPaletteTone_failsClosed() {
		// The retired v1 palette (ok/warn/exceeds) plus the Badge overlay names, plus wrong-cased variants.
		for (var tone : new String[]{"ok", "warn", "exceeds", "accent", "danger", "INFO", "Success", "bogus"}) {
			var e = assertThrows(IllegalArgumentException.class,
				() -> sink(Render.pill().meta("tone", tone)).validate(null), tone);
			assertTrue(e.getMessage().contains(tone), e::getMessage);
			assertTrue(e.getMessage().contains("info|success|warning|error|neutral"), e::getMessage);
		}
	}

	/** The same tone rejected in a sink must be rejected in a cell, and the same tone accepted must be accepted. */
	@Test void c03_toneVerdictsAgreeAcrossBothHosts() {
		for (var tone : new String[]{"info", "success", "warning", "error", "neutral", "ok", "warn", "exceeds", "accent"}) {
			var cell = ViewDef.create("v").dataMode(ViewDef.DataMode.CLIENT).dataUrl("/u")
				.columns(Column.of("state").render(Render.pill().meta("tone", tone)))
				.build();
			var cellOk = tryValidate(cell::validate);
			var sinkOk = tryValidate(() -> sink(Render.pill().meta("tone", tone)).validate(null));
			assertEquals(cellOk, sinkOk, tone);
		}
	}

	private static boolean tryValidate(Runnable r) {
		try {
			r.run();
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	// -----------------------------------------------------------------------------------------------------------
	// d) a sink pill is not popover text
	// -----------------------------------------------------------------------------------------------------------

	/**
	 * {@code pill} is a fill sink but not a {@code POPOVER_TEXT_IDS} member: a chip is a chip, not a paragraph a
	 * popover can be filled from.  Guards against the natural-looking follow-on of adding it to both sets.
	 */
	@Test void d01_pillIsNotPopoverText() {
		assertTrue(SinkRenderAllowlist.BUILTIN_IDS.contains("pill"));
		assertFalse(SinkRenderAllowlist.POPOVER_TEXT_IDS.contains("pill"));
	}
}
