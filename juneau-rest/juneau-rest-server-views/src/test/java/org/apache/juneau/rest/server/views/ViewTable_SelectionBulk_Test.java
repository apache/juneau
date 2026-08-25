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
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * The row-selection contract: {@link ViewTable}'s row-selection and bulk-mutation rendering &mdash; the DOM-shape half of the
 * separability guarantee (design-doc HIGH-5) and the R2 client-only/non-wire guard.
 */
class ViewTable_SelectionBulk_Test extends TestBase {

	public static class Incident {
		public String incidentId;
		public String status;
		public Incident(String incidentId, String status) { this.incidentId = incidentId; this.status = status; }
	}

	private static ViewDef view() {
		return ViewDef.create("incidents")
			.rowType(Incident.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/incidents/data")
			.columns(Column.of("incidentId").title("Incident"), Column.of("status").title("Status"))
			.build();
	}

	private static String bulkSidecarBody(String html, String viewId) {
		var marker = "id=\"" + ViewTable.BULK_SIDECAR_ID_PREFIX + viewId + "\"";
		var open = html.indexOf(marker);
		assertTrue(open >= 0, () -> "bulk sidecar not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	private static String viewSidecarBody(String html, String viewId) {
		var marker = "id=\"juneau-view:" + viewId + "\"";
		var open = html.indexOf(marker);
		assertTrue(open >= 0, () -> "view sidecar not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) An ordinary table (no selection) - unaffected, no new attributes at all.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_ordinaryTable_hasNeitherSelectNorBulkAttributes() {
		var html = Html.of(ViewTable.of(view(), List.of(new Incident("INC-1", "open"))));
		assertFalse(html.contains(ViewTable.SELECT_ATTR), html);
		assertFalse(html.contains(ViewTable.BULK_ATTR), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Selection-only opt-in (e.g. selectable-for-export, PagerDuty-style: selection WITHOUT bulk mutation)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_selectionOnly_stampsSelectAttrsButNeverBulk() {
		var selection = SelectionDef.create("incidentId");
		var html = Html.of(ViewTable.of(view(), List.of(new Incident("INC-1", "open")), selection));
		assertTrue(html.contains(ViewTable.SELECT_ATTR + "=\"1\""), html);
		assertTrue(html.contains(ViewTable.ROW_ID_FIELD_ATTR + "=\"incidentId\""), html);
		assertTrue(html.contains(ViewTable.SELECT_ALL_ATTR + "=\"1\""), html);   // selectAll() default true
		assertTrue(html.contains("juneau-view-select-th"), html);
		// The load-bearing separability fact (HIGH-5): a selection-only table has NO bulk affordance anywhere -
		// not the marker attribute, not a bulk sidecar - there is no code path in ViewTable that could add one
		// from a bare SelectionDef.
		assertFalse(html.contains(ViewTable.BULK_ATTR), html);
		assertFalse(html.contains(ViewTable.BULK_SIDECAR_ID_PREFIX), html);
	}

	@Test void b02_selectAllFalse_stampsZero() {
		var selection = SelectionDef.create("incidentId").selectAll(false);
		var html = Html.of(ViewTable.of(view(), List.of(new Incident("INC-1", "open")), selection));
		assertTrue(html.contains(ViewTable.SELECT_ALL_ATTR + "=\"0\""), html);
	}

	@Test void b03_nullSelection_throws() {
		var v = view();
		var rows = List.of();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v, rows, (SelectionDef) null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) Bulk-mutate opt-in - requires (and renders) selection too, plus its OWN independently-versioned sidecar.
	//------------------------------------------------------------------------------------------------------------------

	private static BulkMutateDef bulkDef(SelectionDef selection) {
		var ack = RowAction.create("ack").label("Acknowledge").endpoint("servlet:/incidents/bulk-ack").method(RowAction.Method.POST);
		return BulkMutateDef.create(WritePermit.forCapability("incidents:ack"), selection).actions(ack);
	}

	@Test void c01_bulkMutate_stampsSelectAndBulkAttrs_plusBulkSidecar() {
		var selection = SelectionDef.create("incidentId");
		var bulk = bulkDef(selection);
		var html = Html.of(ViewTable.of(view(), List.of(new Incident("INC-1", "open")), bulk));

		assertTrue(html.contains(ViewTable.SELECT_ATTR + "=\"1\""), html);
		assertTrue(html.contains(ViewTable.BULK_ATTR + "=\"1\""), html);
		assertTrue(html.contains("id=\"" + ViewTable.BULK_SIDECAR_ID_PREFIX + "incidents\""), html);

		var body = bulkSidecarBody(html, "incidents");
		var expected = Json.to(Json.of(bulk), Map.class);
		var actual = Json.to(body, Map.class);
		assertEquals(expected, actual, body);
		assertEquals(Set.of("contractVersion", "actions"), actual.keySet(), body);
	}

	@Test void c02_nullBulkMutate_throws() {
		var v = view();
		var rows = List.of();
		assertThrows(IllegalArgumentException.class, () -> ViewTable.of(v, rows, (BulkMutateDef) null));
	}

	/**
	 * The compile/DOM-shape guarantee from the OTHER direction: {@code ViewTable.of(..., BulkMutateDef)} has no
	 * parameter through which a caller could supply a DIFFERENT {@link SelectionDef} than the one the
	 * {@link BulkMutateDef} was built against - the overload only ever reads {@code bulkMutate.selection()}. The
	 * lower-level core method enforces this explicitly when a caller mixes the two by hand.
	 */
	@Test void c03_core_rejectsASelectionThatIsNotTheBulkDefsOwnSelection() {
		var selectionA = SelectionDef.create("incidentId");
		var selectionB = SelectionDef.create("incidentId");   // a different instance, same field name
		var bulk = bulkDef(selectionA);
		var v = view();
		var rows = List.of();
		assertThrows(IllegalArgumentException.class,
			() -> ViewTable.of(MarshallingContext.DEFAULT, v, rows, null, selectionB, bulk));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) R2 guard: selection/bulk are 100% client-only/non-wire - VIEW_META is byte-identical with or without them.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_viewMetaSidecarIsUnaffectedByEitherOptIn() {
		var v = view();
		var plainHtml = Html.of(ViewTable.of(v, List.of(new Incident("INC-1", "open"))));
		var selectionHtml = Html.of(ViewTable.of(v, List.of(new Incident("INC-1", "open")), SelectionDef.create("incidentId")));
		var bulkHtml = Html.of(ViewTable.of(v, List.of(new Incident("INC-1", "open")), bulkDef(SelectionDef.create("incidentId"))));

		var plainMeta = viewSidecarBody(plainHtml, "incidents");
		var selectionMeta = viewSidecarBody(selectionHtml, "incidents");
		var bulkMeta = viewSidecarBody(bulkHtml, "incidents");

		// Byte-identical VIEW_META regardless of the opt-in: selection/bulk never touch ViewDef's serialized form.
		assertEquals(plainMeta, selectionMeta, "SelectionDef must not alter VIEW_META");
		assertEquals(plainMeta, bulkMeta, "BulkMutateDef must not alter VIEW_META");
		assertFalse(plainMeta.toLowerCase().contains("select"), plainMeta);
		assertFalse(plainMeta.toLowerCase().contains("bulk"), plainMeta);
	}

	@Test void d02_viewDefContractVersionUnchanged() {
		// Sanity: the selection/bulk-mutation opt-ins themselves never bump VIEW_META's contract version - pin
		// whatever the current value is so a future bump elsewhere doesn't silently drift this fixture's assumption.
		assertEquals("4", ViewDef.CONTRACT_VERSION);
	}
}
