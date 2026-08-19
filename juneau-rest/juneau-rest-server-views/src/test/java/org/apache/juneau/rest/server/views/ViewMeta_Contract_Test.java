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
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.views.RowClassRule.Op;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.apache.juneau.rest.server.views.ViewDef.Dir;
import org.junit.jupiter.api.*;

/**
 * Golden-fixture contract test for the {@code VIEW_META} wire format (design doc §6.10).
 *
 * <p>
 * This is the load-bearing seam of the toolkit: it pins the {@link ViewDef} JSON serialization field-by-field
 * against the frozen §6.10 example.  The serializer is the repo's canonical compact JSON marshaller
 * ({@link Json#of(Object)} &rarr; {@code JsonSerializer.DEFAULT}) &mdash; the same config that serializes the
 * {@code DataTablesResults} envelope in the server-side query protocol tests, so the emitted sidecar and this
 * fixture agree.
 */
class ViewMeta_Contract_Test extends TestBase {

	/** Row bean whose simple name drives the {@code rowType} diagnostic field. */
	public static class Release {
		public String name;
		public String status;
		public String date;
	}

	/**
	 * The frozen §6.10 golden fixture (pretty-printed; whitespace and key order are normalized away by the
	 * structural map comparison below, while the top-level key <b>order</b> is pinned separately).
	 */
	private static final String EXPECTED_VIEW_META = """
		{
		  "contractVersion": "3",
		  "id": "releases",
		  "rowType": "Release",
		  "dataMode": "server",
		  "dataUrl": "servlet:/releases/data",
		  "defaultOrder": [ { "data": "date", "dir": "desc" } ],
		  "columns": [
		    { "data": "name",   "title": "Name",   "orderable": true, "searchable": true,
		      "render": { "id": "linked" }, "href": "servlet:/releases/{id}" },
		    { "data": "status", "title": "Status", "orderable": true, "searchable": true,
		      "render": { "id": "tag", "meta": { "field": "status" } } },
		    { "data": "date",   "title": "Date",   "orderable": true, "searchable": true,
		      "render": { "id": "date" } }
		  ],
		  "ribbon": [
		    { "type": "export", "buttons": ["copy", "csv"], "optional": ["excel", "pdf"] },
		    { "type": "columnSearchToggle" },
		    { "type": "option", "id": "show-superseded", "title": "Show superseded",
		      "column": "status", "value": "superseded", "persist": true },
		    { "type": "refresh" }
		  ],
		  "rowClassRules": [
		    { "field": "deleted", "op": "eq", "value": true, "class": "row-deleted" },
		    { "field": "error",   "op": "present",             "class": "row-flagged" }
		  ]
		}
		""";

	/** Builds the representative "releases" view from the §5/§6.10 builder API. */
	private static ViewDef releasesView() {
		return ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.defaultOrder("date", Dir.DESC)
			.columns(
				Column.of("name").title("Name").render("linked").href("servlet:/releases/{id}"),
				Column.of("status").title("Status").render("tag:status"),
				Column.of("date").title("Date").render("date"))
			.ribbon(
				RibbonAction.export("copy", "csv").optional("excel", "pdf"),
				RibbonAction.columnSearchToggle(),
				RibbonAction.option("show-superseded").title("Show superseded").column("status").value("superseded").persist(true),
				RibbonAction.refresh())
			.rowClassRule("deleted", Op.EQ, true, "row-deleted")
			.rowClassRule("error", Op.PRESENT, "row-flagged")
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// The frozen VIEW_META contract
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_releasesView_serializesToFrozenContract() {
		var json = Json.of(releasesView());

		// Field-by-field structural compare (order- and whitespace-independent): every key/value in the emitted
		// JSON must match the golden fixture exactly, and no extra keys may leak.
		var expected = Json.to(EXPECTED_VIEW_META, Map.class);
		var actual = Json.to(json, Map.class);
		assertEquals(expected, actual, json);
	}

	@Test void a02_topLevelKeyOrderMatchesContract() {
		Map<?,?> actual = Json.to(Json.of(releasesView()), Map.class);
		assertEquals(
			List.of("contractVersion", "id", "rowType", "dataMode", "dataUrl", "defaultOrder", "columns", "ribbon", "rowClassRules"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void a03_reservedFieldsOmittedNotNull() {
		var json = Json.of(releasesView());
		// "details" and "rowActions" are implemented - see f01-f04 (details) and g01-g05 (rowActions) below for
		// their POSITIVE assertions. The remaining reserved C/D/E stubs must still be omitted entirely (not
		// serialized as null keys).
		for (var k : List.of("catalog", "format", "description", "pinned", "defaultVisible")) {
			var key = "\"" + k + "\"";
			assertFalse(json.contains(key), () -> "Reserved field leaked into VIEW_META: " + k + "\n" + json);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// rowClassRules grammar invariants (§6.3)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_rowClassRuleEqRequiresValue() {
		var view = ViewDef.create("x");
		var e = assertThrows(IllegalArgumentException.class,
			() -> view.rowClassRule("deleted", Op.EQ, null, "row-deleted"));
		assertTrue(e.getMessage().contains("value"), e::getMessage);
	}

	@Test void b02_rowClassRulePresentOmitsValue() {
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.rowClassRule("error", Op.PRESENT, "row-flagged")
			.build();
		var json = Json.of(v);
		// A present/absent rule tests only field presence, so the "value" key must be omitted for that rule.
		assertTrue(json.contains("\"op\":\"present\""), json);
		assertFalse(json.contains("\"value\""), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// B.2: render-id string sugar (§6.6): everything after the first colon of an "id:field"-shaped string becomes
	// the parsed renderer's meta.field.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_renderSugarTagField() {
		// Everything after the first colon becomes meta.field.
		var json = Json.of(Column.of("status").render("tag:status"));
		assertTrue(json.contains("\"render\":{\"id\":\"tag\",\"meta\":{\"field\":\"status\"}}"), json);
	}

	@Test void c02_renderSugarBareIdOmitsMeta() {
		// A bare id (no colon) yields a metadata-free renderer -> no "meta" key.
		var json = Json.of(Column.of("date").render("date"));
		assertTrue(json.contains("\"render\":{\"id\":\"date\"}"), json);
		assertFalse(json.contains("\"meta\""), json);
	}

	@Test void c03_renderSugarEqualsCanonicalForm() {
		// The string sugar must serialize identically to the hand-built canonical Render.
		var sugar = Json.of(Column.of("status").render("tag:status"));
		var canonical = Json.of(Column.of("status").render(Render.of("tag").meta("field", "status")));
		assertEquals(canonical, sugar);
	}

	@Test void c04_renderSugarFieldMayContainColons() {
		// Only the FIRST colon is the id/field delimiter; later colons stay in meta.field.
		var json = Json.of(Column.of("t").render("ts-zulu:created:at"));
		assertTrue(json.contains("\"render\":{\"id\":\"ts-zulu\",\"meta\":{\"field\":\"created:at\"}}"), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// B.2: column auto-seed from DataTablesColumns.of(rowType) when .columns(...) is omitted
	//------------------------------------------------------------------------------------------------------------------

	@Test void c10_columnsAutoSeedFromRowType() {
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.build();  // NOTE: no .columns(...) -> should auto-seed from DataTablesColumns.of(Release.class).

		var expected = DataTablesColumns.of(Release.class);
		assertNotNull(v.columns, "columns should be auto-seeded from the row type when omitted");
		assertEquals(expected.size(), v.columns.size(), () -> "auto-seeded column count mismatch: " + Json.of(v.columns));
		for (var i = 0; i < expected.size(); i++) {
			var e = expected.get(i);
			var c = v.columns.get(i);
			assertEquals(e.get("data"), c.data, "data[" + i + "]");
			assertEquals(e.get("title"), c.title, "title[" + i + "]");
			assertEquals(e.get("orderable"), c.orderable, "orderable[" + i + "]");
			assertEquals(e.get("searchable"), c.searchable, "searchable[" + i + "]");
		}
	}

	@Test void c11_autoSeedTitlesMatchHumanizedNames() {
		// Release{name,status,date} -> humanized titles, in Juneau bean-property order.  For a public-field bean with
		// no explicit ordering, Juneau emits properties alphabetically (Date, Name, Status) -- the auto-seed mirrors
		// DataTablesColumns.of() faithfully, so the order matches that source of truth rather than declaration order.
		var v = ViewDef.create("releases").rowType(Release.class).dataMode(DataMode.SERVER).dataUrl("u").build();
		var titles = v.columns.stream().map(c -> c.title).toList();
		assertEquals(List.of("Date", "Name", "Status"), titles);
	}

	@Test void c12_explicitColumnsAreNotOverwrittenByAutoSeed() {
		// When columns are declared explicitly, auto-seed must NOT clobber them.
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("u")
			.columns(Column.of("name").title("Name"))
			.build();
		assertEquals(1, v.columns.size());
		assertEquals("name", v.columns.get(0).data);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Gap-closing branch coverage: Render null-guards + repeated meta(), RowClassRule NE/ABSENT + misuse guards,
	// ViewDef.create()/build()/queryableSettings() validation branches, Column setter instruction gaps.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_renderParseNullThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> Render.parse(null));
		assertTrue(e.getMessage().contains("null"), e::getMessage);
	}

	@Test void d02_renderParseBlankThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> Render.parse("   "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
	}

	@Test void d03_renderMetaAppendsSecondEntryToExistingMap() {
		// First .meta() call creates the map (meta==null branch); a second call must append rather than replace it.
		var r = Render.of("tag").meta("field", "status").meta("extra", "x");
		assertEquals(2, r.meta.size());
		assertEquals("status", r.meta.get("field"));
		assertEquals("x", r.meta.get("extra"));
	}

	@Test void d04_rowClassRuleNeUsage() {
		var r = RowClassRule.of("status", Op.NE, "active", "row-inactive");
		assertEquals("ne", r.op);
		assertEquals("active", r.value);
	}

	@Test void d05_rowClassRuleAbsentUsage() {
		var r = RowClassRule.of("error", Op.ABSENT, "row-ok");
		assertEquals("absent", r.op);
		assertNull(r.value);
	}

	@Test void d06_rowClassRuleValueFormRejectsPresenceOp() {
		// Misusing the (field, op, value, class) form with a presence-based op must be rejected.
		var e = assertThrows(IllegalArgumentException.class,
			() -> RowClassRule.of("error", Op.PRESENT, "x", "row-flagged"));
		assertTrue(e.getMessage().contains("does not take a value"), e::getMessage);
	}

	@Test void d07_rowClassRulePresenceFormRejectsValueOp() {
		// Misusing the (field, op, class) form with a value-based op must be rejected.
		var e = assertThrows(IllegalArgumentException.class,
			() -> RowClassRule.of("status", Op.EQ, "row-x"));
		assertTrue(e.getMessage().contains("requires a value"), e::getMessage);
	}

	@Test void d08_viewDefCreateNullIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ViewDef.create(null));
		assertTrue(e.getMessage().contains("null"), e::getMessage);
	}

	@Test void d09_viewDefCreateBlankIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ViewDef.create("   "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
	}

	@Test void d10_buildWithoutRowTypeOrColumns_leavesColumnsNull() {
		// Neither .rowType(...) nor .columns(...) declared -> build() has nothing to auto-seed from.
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").build();
		assertNull(v.columns);
	}

	@Test void d11_queryableSettingsWithoutRowTypeThrows() {
		var view = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").build();
		var e = assertThrows(IllegalArgumentException.class, view::queryableSettings);
		assertTrue(e.getMessage().contains("rowType"), e::getMessage);
	}

	@Test void d12_columnNameSetter() {
		var c = Column.of("status").name("statusCol");
		assertEquals("statusCol", c.name);
	}

	@Test void d13_columnClassNameSetter() {
		var c = Column.of("status").className("text-right");
		assertEquals("text-right", c.className);
	}

	@Test void d14_ribbonActionDividerAndHintSetters() {
		var divider = RibbonAction.divider();
		assertEquals("divider", divider.type);

		var withHints = RibbonAction.option("o").symbol("*").color("red").deselectable();
		assertEquals("*", withHints.symbol);
		assertEquals("red", withHints.color);
		assertTrue(withHints.deselectable);
	}

	@Test void d15_ribbonActionOptHintSetters() {
		var opt = RibbonAction.Opt.of("a").title("A").color("blue").symbol("o");
		assertEquals("A", opt.title);
		assertEquals("blue", opt.color);
		assertEquals("o", opt.symbol);
	}

	@Test void d16_ribbonActionGroupSetter_andOmittedWhenUnset() {
		// The "group" affordance (visual-parity control-row layout: segmented ribbon clusters) - set + serialized.
		var grouped = RibbonAction.columnSearchToggle().group("filters");
		assertEquals("filters", grouped.group);
		assertTrue(Json.of(grouped).contains("\"group\":\"filters\""), Json.of(grouped));

		// Unset (the common case) -> omitted entirely, like every other optional RibbonAction field.
		var ungrouped = RibbonAction.columnSearchToggle();
		assertNull(ungrouped.group);
		assertFalse(Json.of(ungrouped).contains("\"group\""), Json.of(ungrouped));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e) Declarable poll interval: floor clamp + wire presence/omission
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_pollIntervalMs_omittedWhenUnset() {
		// The golden fixture view never calls .poll(...) - confirms the new field doesn't leak when absent, and
		// that a01/a02's frozen contract is untouched by this additive field.
		var json = Json.of(releasesView());
		assertFalse(json.contains("\"pollIntervalMs\""), json);
	}

	@Test void e02_pollAboveFloor_isHonoredAsDeclared() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a")).poll(30_000L).build();
		assertEquals(30_000L, v.pollIntervalMs);
		assertTrue(Json.of(v).contains("\"pollIntervalMs\":30000"), Json.of(v));
	}

	@Test void e03_pollBelowFloor_isClampedUpToFloor() {
		// By design: a declared interval below the floor is clamped to it, not rejected.
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a")).poll(1_000L).build();
		assertEquals(ViewDef.MIN_POLL_INTERVAL_MS, v.pollIntervalMs);
	}

	@Test void e04_pollAtExactlyTheFloor_isUnchanged() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.poll(ViewDef.MIN_POLL_INTERVAL_MS).build();
		assertEquals(ViewDef.MIN_POLL_INTERVAL_MS, v.pollIntervalMs);
	}

	@Test void e05_pollNonPositiveThrows() {
		var view = ViewDef.create("x");
		var e = assertThrows(IllegalArgumentException.class, () -> view.poll(0L));
		assertTrue(e.getMessage().contains("positive"), e::getMessage);
		var e2 = assertThrows(IllegalArgumentException.class, () -> view.poll(-500L));
		assertTrue(e2.getMessage().contains("positive"), e2::getMessage);
	}

	@Test void e06_pollIntervalMs_isTheLastWireKey_afterRowClassRules() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a")).poll(60_000L).build();
		Map<?,?> actual = Json.to(Json.of(v), Map.class);
		assertEquals(
			List.of("contractVersion", "id", "dataMode", "dataUrl", "columns", "pollIntervalMs"),
			new ArrayList<>(actual.keySet()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f) Row-details expander: the positive assertion for the "details" key (the same positive-assertion-plus-
	// remaining-negatives split used elsewhere for "rowActions", applied here - see a03 above for the remaining
	// negative list with "details" now removed from it).
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_details_omittedWhenUnset() {
		// The golden fixture view never calls .details(...) - confirms the new field doesn't leak when absent,
		// and that a01/a02's frozen contract is untouched by this additive field.
		var json = Json.of(releasesView());
		assertFalse(json.contains("\"details\""), json);
	}

	@Test void f02_details_emittedInPinnedOrder_withExpectedShape() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.details(ViewDef.DetailDef.of("owner").title("Owner"), ViewDef.DetailDef.of("createdAt").title("Created"))
			.build();
		var json = Json.of(v);
		assertTrue(json.contains(
			"\"details\":[{\"data\":\"owner\",\"title\":\"Owner\"},{\"data\":\"createdAt\",\"title\":\"Created\"}]"),
			json);
	}

	@Test void f03_details_isBeforePollIntervalMs_andAfterRowClassRules_inTopLevelKeyOrder() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.rowClassRule("error", Op.PRESENT, "row-flagged")
			.details(ViewDef.DetailDef.of("owner").title("Owner"))
			.poll(60_000L)
			.build();
		Map<?,?> actual = Json.to(Json.of(v), Map.class);
		assertEquals(
			List.of("contractVersion", "id", "dataMode", "dataUrl", "columns", "rowClassRules", "details", "pollIntervalMs"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void f04_detailDefTitleDefaultsToNullWhenUnset() {
		var d = ViewDef.DetailDef.of("owner");
		assertEquals("owner", d.data);
		assertNull(d.title);
	}

	//------------------------------------------------------------------------------------------------------------------
	// g) Row actions: the positive assertion for the "rowActions" key (moved out of a03's negative reserved list -
	// same positive-assertion-plus-remaining-negatives split used for "details" in section f).  This freezes the
	// COMPLETE RowAction wire schema (TODO-399 Decision 8) so a later wave (TODO-416) can rely on it verbatim.
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_rowActions_omittedWhenUnset() {
		// The golden fixture view never calls .rowActions(...) - confirms the new field doesn't leak when absent,
		// and that a01/a02's frozen contract is untouched by this additive field.
		var json = Json.of(releasesView());
		assertFalse(json.contains("\"rowActions\""), json);
	}

	@Test void g02_rowActions_emittedInPinnedOrder_withCompleteWireSchema() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").icon("check").endpoint("servlet:/x/{id}/ack")
					.method(RowAction.Method.POST).confirm("Acknowledge this?").form("servlet:/x/ack-form")
					.present(RowAction.Present.DIALOG).onSuccess(RowAction.OnSuccess.MERGE_ROW))
			.build();
		var json = Json.of(v);
		// Every RowAction wire field, in the pinned @BeanType order, with the exact tokens 416 will consume.
		assertTrue(json.contains(
			"\"rowActions\":[{\"id\":\"ack\",\"label\":\"Acknowledge\",\"icon\":\"check\","
			+ "\"endpoint\":\"servlet:/x/{id}/ack\",\"method\":\"POST\",\"confirm\":\"Acknowledge this?\","
			+ "\"form\":\"servlet:/x/ack-form\",\"present\":\"dialog\",\"onSuccess\":\"mergeRow\"}]"),
			json);
	}

	@Test void g03_rowActions_omitsUnsetOptionalFields() {
		// Only id is required; every other RowAction field is omitted (not null) when unset.
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.rowActions(RowAction.create("del").endpoint("servlet:/x/{id}").method(RowAction.Method.DELETE))
			.build();
		var json = Json.of(v);
		assertTrue(json.contains("\"rowActions\":[{\"id\":\"del\",\"endpoint\":\"servlet:/x/{id}\",\"method\":\"DELETE\"}]"), json);
		assertFalse(json.contains("\"confirm\""), json);
		assertFalse(json.contains("\"present\""), json);
		assertFalse(json.contains("\"onSuccess\""), json);
	}

	@Test void g04_rowActions_isAfterRowClassRules_andBeforeDetails_inTopLevelKeyOrder() {
		var v = ViewDef.create("x").dataMode(DataMode.SERVER).dataUrl("u").columns(Column.of("a"))
			.rowClassRule("error", Op.PRESENT, "row-flagged")
			.rowActions(RowAction.create("go").endpoint("u").method(RowAction.Method.POST))
			.details(ViewDef.DetailDef.of("owner").title("Owner"))
			.poll(60_000L)
			.build();
		Map<?,?> actual = Json.to(Json.of(v), Map.class);
		assertEquals(
			List.of("contractVersion", "id", "dataMode", "dataUrl", "columns", "rowClassRules", "rowActions", "details", "pollIntervalMs"),
			new ArrayList<>(actual.keySet()));
	}

	@Test void g05_rowActionMethodTokensAreTheNonSafeMethods() {
		// The Method enum makes a safe method (GET/HEAD/OPTIONS/TRACE) unexpressible - the build-time half of the
		// HIGH-7 refusal - and each token is the uppercase HTTP method the client submits with.
		assertEquals("POST", RowAction.Method.POST.wire());
		assertEquals("PUT", RowAction.Method.PUT.wire());
		assertEquals("PATCH", RowAction.Method.PATCH.wire());
		assertEquals("DELETE", RowAction.Method.DELETE.wire());
	}

	@Test void g06_rowActionCreateBlankIdThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> RowAction.create("  "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
	}

	@Test void g07_rowActionPresentAndOnSuccessTokens() {
		assertEquals("page", RowAction.Present.PAGE.wire());
		assertEquals("dialog", RowAction.Present.DIALOG.wire());
		assertEquals("inline", RowAction.Present.INLINE.wire());
		assertEquals("redraw", RowAction.OnSuccess.REDRAW.wire());
		assertEquals("mergeRow", RowAction.OnSuccess.MERGE_ROW.wire());
		assertEquals("navigate", RowAction.OnSuccess.NAVIGATE.wire());
	}
}
