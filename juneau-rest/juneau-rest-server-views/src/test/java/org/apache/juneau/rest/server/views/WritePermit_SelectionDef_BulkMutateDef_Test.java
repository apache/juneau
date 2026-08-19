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

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * {@code TODO-428}: {@link WritePermit}, {@link SelectionDef}, and {@link BulkMutateDef} &mdash; the API-shape half
 * of the separability guarantee (design-doc HIGH-5).
 *
 * <p>
 * These are NOT prose assertions: {@link BulkMutateDef#create(WritePermit, SelectionDef)} is the ONLY way to obtain
 * a {@link BulkMutateDef} (a private constructor + reflection below prove there is no other, narrower factory), and
 * both parameters are non-null-checked at that single call site &mdash; so a bulk-mutate opt-in that omits a
 * {@link WritePermit} cannot compile against any DIFFERENT, permit-less overload (there isn't one) and cannot
 * construct one by passing {@code null} either (it throws).
 */
class WritePermit_SelectionDef_BulkMutateDef_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// WritePermit
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_forCapability_rejectsNullAndBlank() {
		assertThrows(IllegalArgumentException.class, () -> WritePermit.forCapability(null));
		assertThrows(IllegalArgumentException.class, () -> WritePermit.forCapability(""));
		assertThrows(IllegalArgumentException.class, () -> WritePermit.forCapability("   "));
	}

	@Test void a02_forCapability_roundTripsTheCapability() {
		var p = WritePermit.forCapability("incidents:ack");
		assertEquals("incidents:ack", p.capability());
		assertTrue(p.toString().contains("incidents:ack"), p.toString());
	}

	/** WritePermit has no public constructor - the factory is the only way in, mirroring WriteGuard's discipline. */
	@Test void a03_noPublicConstructor() {
		for (var c : WritePermit.class.getDeclaredConstructors())
			assertFalse(Modifier.isPublic(c.getModifiers()), () -> "WritePermit must not expose a public constructor: " + c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// SelectionDef
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_create_rejectsNullAndBlankRowIdField() {
		assertThrows(IllegalArgumentException.class, () -> SelectionDef.create(null));
		assertThrows(IllegalArgumentException.class, () -> SelectionDef.create(""));
		assertThrows(IllegalArgumentException.class, () -> SelectionDef.create("  "));
	}

	@Test void b02_selectAll_defaultsTrue_andIsToggleable() {
		var s = SelectionDef.create("id");
		assertEquals("id", s.rowIdField());
		assertTrue(s.selectAll());
		assertFalse(s.selectAll(false).selectAll());
		assertTrue(s.selectAll(true).selectAll());
	}

	@Test void b03_noPublicConstructor() {
		for (var c : SelectionDef.class.getDeclaredConstructors())
			assertFalse(Modifier.isPublic(c.getModifiers()), () -> "SelectionDef must not expose a public constructor: " + c);
	}

	//------------------------------------------------------------------------------------------------------------------
	// BulkMutateDef - the separability/compile-shape guarantee (HIGH-5)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_create_requiresBothPermitAndSelection() {
		var permit = WritePermit.forCapability("cap");
		var selection = SelectionDef.create("id");
		assertThrows(IllegalArgumentException.class, () -> BulkMutateDef.create(null, selection));
		assertThrows(IllegalArgumentException.class, () -> BulkMutateDef.create(permit, null));
		assertThrows(IllegalArgumentException.class, () -> BulkMutateDef.create(null, null));
	}

	@Test void c02_create_succeedsWithBoth_andExposesBothBack() {
		var permit = WritePermit.forCapability("cap");
		var selection = SelectionDef.create("id");
		var b = BulkMutateDef.create(permit, selection);
		assertSame(permit, b.permit());
		assertSame(selection, b.selection());
	}

	@Test void c03_actions_rejectsNullAndEmpty() {
		var b = BulkMutateDef.create(WritePermit.forCapability("cap"), SelectionDef.create("id"));
		assertThrows(IllegalArgumentException.class, () -> b.actions((RowAction[]) null));
		assertThrows(IllegalArgumentException.class, b::actions);
	}

	@Test void c04_actions_roundTrips() {
		var ack = RowAction.create("ack").label("Acknowledge").endpoint("servlet:/bulk/ack").method(RowAction.Method.POST);
		var b = BulkMutateDef.create(WritePermit.forCapability("cap"), SelectionDef.create("id")).actions(ack);
		assertEquals(1, b.actions.size());
		assertSame(ack, b.actions.get(0));
	}

	/**
	 * The API-SHAPE proof (HIGH-5), not a prose claim: {@code create(...)} is the ONLY public factory on
	 * {@link BulkMutateDef} and its signature is EXACTLY {@code (WritePermit, SelectionDef)} - there is no
	 * narrower overload a caller could reach for that skips the permit. Combined with c01 (a null permit throws)
	 * and c05/c06 below (no public constructor, no setter for the permit/selection fields), this proves a
	 * bulk-mutate opt-in cannot be constructed without supplying a real {@link WritePermit} - by shape, not by
	 * javadoc sentence.
	 */
	@Test void c05_onlyOneCreateOverload_andItsSignatureRequiresWritePermitFirst() {
		var createMethods = Arrays.stream(BulkMutateDef.class.getMethods())
			.filter(m -> m.getName().equals("create"))
			.toList();
		assertEquals(1, createMethods.size(), () -> "expected exactly one create(...) overload: " + createMethods);
		var m = createMethods.get(0);
		assertArrayEquals(new Class<?>[] { WritePermit.class, SelectionDef.class }, m.getParameterTypes(),
			() -> "BulkMutateDef.create(...) must require (WritePermit, SelectionDef), in that order: " + m);
	}

	@Test void c06_noPublicConstructor() {
		for (var c : BulkMutateDef.class.getDeclaredConstructors())
			assertFalse(Modifier.isPublic(c.getModifiers()), () -> "BulkMutateDef must not expose a public constructor: " + c);
	}

	/**
	 * The permit/selection fields backing separability are NOT public mutable fields either - a caller cannot
	 * bypass the constructor-time requirement by building a bare instance (impossible; no public ctor) and later
	 * poking a permit in via reflection-free code.
	 */
	@Test void c07_permitAndSelectionAreNotPublicSettableFields() {
		for (var f : BulkMutateDef.class.getDeclaredFields()) {
			if (f.getType() == WritePermit.class || f.getType() == SelectionDef.class)
				assertFalse(Modifier.isPublic(f.getModifiers()), () -> "must not be a public field: " + f);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Independent contract version + per-target result shape (no permit/selection leak onto the wire)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_ownContractVersion_independentOfViewAndActionResult() {
		// A third, independently-versioned contract, alongside ViewDef's and ActionResult's - see BulkMutateDef's
		// class javadoc; this test only pins that the constant exists and is its own string, not that it must
		// differ numerically (an incidental "3" vs "1" tells us nothing) - the independence is structural
		// (three separate constants, never aliased), which the ViewTable render tests further verify at the wire.
		assertEquals("1", BulkMutateDef.CONTRACT_VERSION);
	}

	@Test void d02_serializedForm_carriesOnlyContractVersionAndActions_neverThePermitOrSelection() {
		var ack = RowAction.create("ack").endpoint("servlet:/bulk/ack").method(RowAction.Method.POST);
		var b = BulkMutateDef.create(WritePermit.forCapability("cap"), SelectionDef.create("id")).actions(ack);
		var json = Json.of(b);
		assertFalse(json.contains("cap"), () -> "the WritePermit's capability must never reach the wire: " + json);
		assertFalse(json.toLowerCase().contains("rowidfield"), () -> "the SelectionDef must never reach the wire: " + json);
		var parsed = Json.to(json, Map.class);
		assertEquals(Set.of("contractVersion", "actions"), parsed.keySet(), json);
	}
}
