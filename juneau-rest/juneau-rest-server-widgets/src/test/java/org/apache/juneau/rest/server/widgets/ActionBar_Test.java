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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link ActionBar} / {@link ActionRef} / {@link SafeAction} bean contract.  The module-isolation rule these beans
 * depend on is asserted in {@link Widgets_ModuleBoundary_Test}.
 */
class ActionBar_Test extends TestBase {

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", ActionBar.CONTRACT_VERSION);
	}

	@Test void a02_items_roundTrip() {
		var bar = ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE);
		assertSize(2, bar.items);
		assertEquals("ack", ((ActionRef) bar.items.get(0)).id);
		assertEquals(SafeAction.COLLAPSE, bar.items.get(1));
		bar.validate();
	}

	@Test void a03_blankActionRef_rejectedAtFactory() {
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of(null));
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of("  "));
	}

	@Test void a04_validate_rejectsBlankActionRefId() {
		var bar = ActionBar.create();
		var blank = new ActionRef();
		blank.id = "  ";
		bar.items = java.util.List.of(blank);
		var e = assertThrows(IllegalArgumentException.class, bar::validate);
		assertTrue(e.getMessage().contains("ActionRef"), e::getMessage);
	}

	@Test void a05_safeAction_collapseWireAndLabel() {
		assertEquals("collapse", SafeAction.COLLAPSE.wire());
		assertEquals("Collapse", SafeAction.COLLAPSE.label());
	}

	@Test void a06_actionRef_emphasisDefaultsToSecondary() {
		assertEquals(ActionRef.Emphasis.SECONDARY, ActionRef.of("ack").emphasis);
	}

	@Test void a07_actionRef_emphasisFluentSetter() {
		var ar = ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY);
		assertEquals(ActionRef.Emphasis.PRIMARY, ar.emphasis);
	}

	@Test void a08_validate_allowsOnePrimary() {
		var bar = ActionBar.create().items(ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY), ActionRef.of("esc"));
		bar.validate();
	}

	@Test void a09_validate_rejectsMoreThanOnePrimary() {
		var bar = ActionBar.create().items(
			ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY),
			ActionRef.of("esc").emphasis(ActionRef.Emphasis.PRIMARY));
		var e = assertThrows(IllegalArgumentException.class, bar::validate);
		assertTrue(e.getMessage().contains("PRIMARY"), e::getMessage);
	}

	@Test void a10_validate_safeActionNeverCountsAsPrimary() {
		// SafeAction carries no emphasis field at all, so a bar of nothing but SafeAction items can never trip
		// the at-most-one-PRIMARY rule - the type system rules it out rather than the validator.
		var bar = ActionBar.create().items(ActionRef.of("ack").emphasis(ActionRef.Emphasis.PRIMARY), SafeAction.COLLAPSE);
		bar.validate();
	}

	@Test void a11_enabledWhen_valueForm_recordsFieldOpValueAndReason() {
		var ar = ActionRef.of("close").enabledWhen("state", Op.EQ, "open", "This record is not open.");
		assertSize(1, ar.enabledWhen);
		var r = ar.enabledWhen.get(0);
		assertEquals("state", r.field);
		assertEquals(Op.EQ, r.op);
		assertEquals("open", r.value);
		assertEquals("This record is not open.", r.reason);
		ActionBar.create().items(ar).validate();
	}

	@Test void a12_enabledWhen_isAdditive_andKeepsDeclarationOrder() {
		// Declaration order is the priority mechanism (the first failing rule's reason is the one shown), so the
		// list must preserve it rather than dedupe or sort.
		var ar = ActionRef.of("close")
			.enabledWhen("state", Op.EQ, "open", "declared first")
			.enabledWhen("owner", Op.PRESENT, "declared second");
		assertSize(2, ar.enabledWhen);
		assertEquals("declared first", ar.enabledWhen.get(0).reason);
		assertEquals("declared second", ar.enabledWhen.get(1).reason);
		assertEquals(Op.PRESENT, ar.enabledWhen.get(1).op);
		assertNull(ar.enabledWhen.get(1).value, "a presence-based rule carries no value");
		ActionBar.create().items(ar).validate();
	}

	@Test void a13_enabledWhen_blankReason_rejectedAtTheSetter() {
		// The reason is a parameter of the rule rather than a chained optional setter, so there is no shape of the
		// fluent API that produces a rule without one - and a blank string is not a way around that.
		var ar = ActionRef.of("close");
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.EQ, "open", null));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.EQ, "open", "  "));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.PRESENT, " "));
		assertNull(ar.enabledWhen, "a rejected rule must not be half-added");
	}

	@Test void a14_enabledWhen_blankField_rejectedAtTheSetter() {
		var ar = ActionRef.of("close");
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("  ", Op.EQ, "open", "why"));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen(null, Op.PRESENT, "why"));
	}

	@Test void a15_enabledWhen_operatorValueShapeMismatch_rejectedAtTheSetter() {
		var ar = ActionRef.of("close");
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.PRESENT, "open", "why"));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.ABSENT, "open", "why"));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.EQ, null, "why"));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", Op.EQ, "why"));
		assertThrows(IllegalArgumentException.class, () -> ar.enabledWhen("state", null, "open", "why"));
	}

	@Test void a16_validate_rejectsAHandBuiltRuleWithNoReason() {
		// The fluent setter cannot produce this, so a field-by-field bean is the only route in - the same reason
		// a04 hand-builds a blank id rather than going through ActionRef.of.
		var ar = ActionRef.of("close");
		var r = new ActionRef.EnabledRule();
		r.field = "state";
		r.op = Op.EQ;
		r.value = "open";
		ar.enabledWhen = java.util.List.of(r);
		var bar = ActionBar.create().items(ar);
		var e = assertThrows(IllegalArgumentException.class, bar::validate);
		assertTrue(e.getMessage().contains("reason"), e::getMessage);
	}

	@Test void a17_validate_rejectsAHandBuiltRuleWithTheWrongValueShape() {
		var ar = ActionRef.of("close");
		var r = new ActionRef.EnabledRule();
		r.field = "state";
		r.op = Op.PRESENT;
		r.value = "open";
		r.reason = "why";
		ar.enabledWhen = java.util.List.of(r);
		var bar = ActionBar.create().items(ar);
		var e = assertThrows(IllegalArgumentException.class, bar::validate);
		assertTrue(e.getMessage().contains("does not take a value"), e::getMessage);
	}

	@Test void a18_safeAction_isNotGateable() {
		// COLLAPSE must work while the expand GET is in flight and after it fails, so it is never gated at all.
		// That is a type-level guarantee rather than a validator rule: the setter is simply not offered here.
		for (var m : SafeAction.class.getMethods())
			assertNotEquals("enabledWhen", m.getName(), m::toString);
	}

	@Test void a19_op_wireTokensAndValueRequirement() {
		assertEquals("eq", Op.EQ.wire());
		assertEquals("ne", Op.NE.wire());
		assertEquals("present", Op.PRESENT.wire());
		assertEquals("absent", Op.ABSENT.wire());
		assertTrue(Op.EQ.requiresValue());
		assertTrue(Op.NE.requiresValue());
		assertFalse(Op.PRESENT.requiresValue());
		assertFalse(Op.ABSENT.requiresValue());
	}
}
