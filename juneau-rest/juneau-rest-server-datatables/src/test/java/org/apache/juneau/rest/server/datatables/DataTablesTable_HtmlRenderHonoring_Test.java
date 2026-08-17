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
package org.apache.juneau.rest.server.datatables;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Ticket 361 Phase 6: {@code DataTablesTable.of(ctx, id, rows, rowType)} additively honors a property's
 * {@code @Html(render=...)} the same way the ordinary {@code HtmlSerializer}/{@code HtmlDocSerializer} path already
 * does &mdash; closing r3 should-fixes S4 (Map-row branch preserved) and S5 ({@code DataTablesTable} does not
 * actually resolve {@code BeanMeta} today; it delegates to the raw-value {@code DataTablesColumns}-overload path).
 *
 * <p>
 * Per the plan's Phase-6 test-fixture-discipline note (S4), this local {@link LocalFixtureRender} is deliberately
 * NOT {@code TagHtmlRender}/{@code console-ui} &mdash; this module gains no test-scope dependency on {@code console-ui}.
 * The real {@code .tag}-markup-in-a-real-datatable proof is Phase 7's job, via the actual {@code <@datatable>} macro.
 */
class DataTablesTable_HtmlRenderHonoring_Test extends TestBase {

	/** A trivial in-module {@link HtmlRender} fixture: uppercases the string form of the value. */
	public static class LocalFixtureRender extends HtmlRender<Object> {
		@Override
		public Object getContent(SerializerSession session, Object value) {
			return String.valueOf(value).toUpperCase(Locale.ROOT);
		}
	}

	/** One {@code @Html(render=...)}-annotated property, one plain property (back-compat gate needs both). */
	public static class Row {
		@Html(render=LocalFixtureRender.class) public String status = "released";
		public String name = "widget";
	}

	/** A {@code null}-valued annotated property must not NPE and must not invoke {@code getContent(...)}. */
	public static class NullRow {
		@Html(render=LocalFixtureRender.class) public String status;
	}

	//------------------------------------------------------------------------------------------------------------------
	// RED (pre-Phase-6): @Html(render) is NOT honored by DataTablesTable.of(ctx, id, rows, rowType) today, because
	// it delegates to the raw-value columns-overload path (DataTablesTable.java:94-96 as of pre-Phase-6 source).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_htmlRenderHonored_annotatedPropertyIsTransformed() {
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "t", List.of(new Row()), Row.class));
		assertTrue(html.contains("RELEASED"), () -> "expected the LocalFixtureRender-transformed (uppercased) value in the <td>, got:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Back-compat: an un-annotated property on the SAME row bean still emits the raw value, unchanged.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a02_unannotatedPropertyOnSameRow_stillRaw() {
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "t", List.of(new Row()), Row.class));
		assertTrue(html.contains("widget"), () -> "expected the plain property's raw (lowercase, unmodified) value, got:\n" + html);
		assertFalse(html.contains("WIDGET"), () -> "un-annotated property must NOT be transformed, got:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// DataTablesColumns is untouched: output for the same bean is byte-for-byte identical, no new key.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a03_dataTablesColumnsOutput_unchanged() {
		var cols = DataTablesColumns.of(MarshallingContext.DEFAULT, Row.class);
		for (var col : cols) {
			assertEquals(Set.of("data", "title", "orderable", "searchable"), col.keySet(), col::toString);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Null-cell gate: a null value on an @Html(render)-annotated property must not NPE, and getContent must not run
	// (a render invoked on null would NPE inside a render that dereferences the value, as LocalFixtureRender's
	// String.valueOf(value) would happily print "NULL" for -- assert the SHORT-CIRCUIT, not just "no exception").
	//------------------------------------------------------------------------------------------------------------------

	@Test void a04_nullValueOnAnnotatedProperty_noNpe_rendersEmpty() {
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "t", List.of(new NullRow()), NullRow.class));
		assertFalse(html.contains("NULL"), () -> "getContent(...) must not be invoked on a null value, got:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Map-row gate (S4): bean rowType (for columns) + Map rows (for values) is a supported path today via
	// value()'s "row instanceof Map" branch. It must keep rendering RAW values, with NO render resolution attempt,
	// both before and after this change (a Map row has no BeanPropertyMeta / @Html(render)).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a05_mapRowWithBeanRowType_stillRawNoRenderAttempted() {
		var row = new LinkedHashMap<String,Object>();
		row.put("status", "released");
		row.put("name", "widget");
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "t", List.of(row), Row.class));
		assertTrue(html.contains("released"), () -> "Map-row value must render raw (lowercase), got:\n" + html);
		assertFalse(html.contains("RELEASED"), () -> "Map row has no BeanPropertyMeta -- no render must be attempted, got:\n" + html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Honesty-note gate: documented BEHAVIOR CHANGE, not purely additive. A bean already using @Html(render) for the
	// existing HtmlDoc/serializer path now ALSO renders through that same render when passed through
	// DataTablesTable.of(..., rowType) -- named explicitly so it shows up in the diff/PR description.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a06_behaviorChangeNotPurelyAdditive_existingHtmlRenderNowAppliesInDataTablesTableToo() {
		// The serializer path (unrelated to DataTablesTable) already honors @Html(render) -- this is pre-existing.
		var plainSerializerHtml = HtmlSerializer.DEFAULT.toString(new Row());
		assertTrue(plainSerializerHtml.contains("RELEASED"), () -> "sanity: plain HtmlSerializer already honors @Html(render), got:\n" + plainSerializerHtml);

		// BEHAVIOR CHANGE: DataTablesTable.of(..., rowType) now ALSO honors it (did not, pre-Phase-6).
		var tableHtml = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "t", List.of(new Row()), Row.class));
		assertTrue(tableHtml.contains("RELEASED"), () -> "DataTablesTable must now ALSO honor @Html(render) -- not purely additive, got:\n" + tableHtml);
	}
}
