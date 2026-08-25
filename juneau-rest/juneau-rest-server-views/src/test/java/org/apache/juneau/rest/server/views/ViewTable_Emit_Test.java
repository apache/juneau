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
 * Markup + escaping tests for the {@link ViewTable} emitter (design doc §6.1).
 *
 * <p>
 * Proves the emitter produces the {@code data-juneau-view} table plus the {@code <script type="application/json">}
 * VIEW_META sidecar, and &mdash; the security-critical part &mdash; that a {@link ViewDef} whose serialized JSON
 * contains {@code <script>}/{@code </script>}-shaped strings is neutralized so it cannot break out of the
 * {@code <script>} element.
 */
class ViewTable_Emit_Test extends TestBase {

	/** Row bean for the client-mode {@code tbody} test. */
	public static class Release {
		public String name;
		public String status;
		public Release(String name, String status) { this.name = name; this.status = status; }
	}

	private static ViewDef view() {
		return ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(
				Column.of("name").title("Name"),
				Column.of("status").title("Status"))
			.build();
	}

	/** Extracts the raw text between the sidecar's opening and closing {@code <script>} tags. */
	private static String sidecarBody(String html) {
		var open = html.indexOf("id=\"juneau-view:releases\"");
		assertTrue(open >= 0, () -> "sidecar script tag not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Markup shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_emitsViewTableAndSidecar() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("id=\"releases\""), html);
		assertTrue(html.contains("data-juneau-view=\"releases\""), html);
		assertTrue(html.contains("<script type=\"application/json\" id=\"juneau-view:releases\">"), html);
	}

	@Test void a02_usesViewMarkerNotDataTablesMarker() {
		var html = Html.of(ViewTable.of(view()));
		// Distinct marker: must NOT collide with the plain-DataTables path (data-juneau-datatable).
		assertFalse(html.contains("data-juneau-datatable"), html);
		assertEquals("data-juneau-view", ViewTable.MARKER_ATTR);
	}

	@Test void a03_sidecarCarriesTheSerializedViewMeta() {
		var html = Html.of(ViewTable.of(view()));
		var body = sidecarBody(html);
		// The sidecar JSON must round-trip back to the same VIEW_META the golden fixture pins.
		var expected = Json.to(Json.of(view()), Map.class);
		var actual = Json.to(body, Map.class);
		assertEquals(expected, actual, body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Escaping contract (SECURITY-CRITICAL) - a </script> in the config must not break out of the <script> element.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_scriptBreakoutIsNeutralized() {
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(Column.of("name").title("x</script><!-- <script>alert(1)</script>"))
			.build();

		var html = Html.of(ViewTable.of(v));
		var body = sidecarBody(html);

		// No raw '<' may survive inside the sidecar (so neither </script> nor <!-- nor <script can break out).
		assertFalse(body.contains("<"), () -> "raw '<' leaked into the JSON sidecar:\n" + body);
		// The break-out char must be present as its JSON unicode escape.
		assertTrue(body.contains("\\u003c"), () -> "expected \\u003c escaping in sidecar:\n" + body);
	}

	@Test void b02_neutralizedPayloadStillRoundTripsAsData() {
		var payload = "x</script><!-- <script>alert(1)</script>";
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(Column.of("name").title(payload))
			.build();

		var html = Html.of(ViewTable.of(v));
		var body = sidecarBody(html);

		// The escaped JSON is still valid JSON and the value decodes back to the original string (data preserved).
		var parsed = Json.to(body, Map.class);
		var cols = (List<?>) parsed.get("columns");
		var col0 = (Map<?,?>) cols.get(0);
		assertEquals(payload, col0.get("title"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Optional rows (client mode) -> populated <tbody>
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_rowsOverloadPopulatesTbody() {
		var rows = List.of(new Release("1.0", "released"), new Release("2.0", "draft"));
		var html = Html.of(ViewTable.of(view(), rows));
		assertTrue(html.contains("<tbody>"), html);
		assertTrue(html.contains("released"), html);
		assertTrue(html.contains("draft"), html);
	}

	@Test void c02_noRowsOmitsTbody() {
		var html = Html.of(ViewTable.of(view()));
		// Server mode with no rows: rows arrive via ajax, so no <tbody> is emitted up front.
		assertFalse(html.contains("<tbody>"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Re-serializability (Task 1 regression) - the String-backed rawText sidecar is NOT a one-shot Reader, so the same
	// ViewTable bean survives repeated serialization (which is what the full HtmlDoc page path requires).
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_sameBeanReserializesByteIdenticalAndKeepsEscaping() {
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(Column.of("name").title("x</script><!-- <script>alert(1)</script>"))
			.build();
		var bean = ViewTable.of(v);

		var html1 = Html.of(bean);
		var html2 = Html.of(bean);

		// Re-serializable: before the rawText swap the sidecar was a one-shot StringReader (consumed on first read).
		assertFalse(html1.isEmpty(), () -> "first serialization was empty");
		assertEquals(html1, html2, () -> "same ViewTable bean must re-serialize byte-identically:\n1:" + html1 + "\n2:" + html2);

		// The B.3 escaping contract still holds on the (re)serialized output: no raw '<' survives, \u003c is present.
		var body = sidecarBody(html2);
		assertFalse(body.contains("<"), () -> "raw '<' leaked into the JSON sidecar after re-serialization:\n" + body);
		assertTrue(body.contains("\\u003c"), () -> "expected \\u003c escaping in re-serialized sidecar:\n" + body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Emitter branch coverage (thead title fallback + Map-backed rows with a missing key)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d02_titleFallsBackToDataKeyWhenNoTitleSet() {
		// A column with no .title(): the <thead> cell falls back to the data key.
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(Column.of("name"))   // no .title(...) -> header falls back to the data key
			.build();
		var html = Html.of(ViewTable.of(v));
		assertTrue(html.contains("<th>name</th>"), html);
	}

	@Test void d03_mapBackedRowsWithMissingKeyRenderEmptyCell() {
		// Map-backed rows exercise the direct-key-lookup path; an absent key yields an empty cell.
		var r1 = new LinkedHashMap<String,Object>();
		r1.put("name", "1.0");
		r1.put("status", "released");
		var r2 = new LinkedHashMap<String,Object>();
		r2.put("name", "2.0");   // no "status" key -> null value -> empty <td>
		var rows = List.of(r1, r2);

		var html = Html.of(ViewTable.of(view(), rows));
		assertTrue(html.contains("<tbody>"), html);
		assertTrue(html.contains("released"), html);
		assertTrue(html.contains("2.0"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// data-juneau-saved-views stamp (slice 6) — wrapper <div>, not the <table>
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_resolvedBase_stampsWrapperDivNotTable() {
		var html = Html.of(ViewTable.of(MarshallingContext.DEFAULT, view(), null, null, null, null,
			"/myapp/juneau-saved-views"));
		assertTrue(html.contains("data-juneau-saved-views="), html);
		assertTrue(html.contains("/myapp/juneau-saved-views"), html);
		assertEquals("data-juneau-saved-views", ViewTable.SAVED_VIEWS_ATTR);
		// The table itself must NOT carry the attribute (JS reads it via closest() from the wrapper/page shell).
		assertFalse(html.contains("<table id=\"releases\" data-juneau-saved-views"), html);
	}

	@Test void e02_absentBase_doesNotStamp() {
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains("data-juneau-saved-views"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// data-juneau-layout="wide" full-real-estate stamp (Goal 1 / N2 A) — the ONE stamp node is the wrapper
	// <div>, never the <table>, .jc-card, or .jc-main (ViewTable emits none of those classes).
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_layoutWide_stampsWrapperDiv() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-layout=\"wide\""), html);
		assertEquals("data-juneau-layout", ViewTable.LAYOUT_ATTR);
		assertEquals("wide", ViewTable.LAYOUT_WIDE);
		// The wrapper <div> opens the emitted markup; the stamp rides it, not the <table>.
		assertTrue(html.startsWith("<div"), html);
		assertFalse(html.contains("<table id=\"releases\" data-juneau-view=\"releases\" data-juneau-layout"), html);
	}

	@Test void f02_layoutWide_notStampedOnCardOrMain() {
		// ViewTable emits neither .jc-card nor .jc-main; those are consumer chrome. The stamp must not synthesize them.
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains("jc-card"), html);
		assertFalse(html.contains("jc-main"), html);
	}
}
