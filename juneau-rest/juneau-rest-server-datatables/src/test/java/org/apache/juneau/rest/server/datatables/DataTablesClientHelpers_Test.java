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
import org.apache.juneau.commons.Schema;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.junit.jupiter.api.*;

/**
 * Tests the client-side DataTables browser helpers: {@link DataTablesColumns}, {@link DataTablesTable}, and
 * {@link DataTablesMixin}.
 */
@SuppressWarnings({
	"resource" // MockRestClient.close() is a no-op (no real OS resource); test-fixture AutoCloseables are managed by the test lifecycle (mixed-module resource analysis on test code).
})
class DataTablesClientHelpers_Test extends TestBase {

	/**
	 * Row bean exercising every column-title source:
	 * <ul>
	 * 	<li>{@code order} - public field, no {@code @Schema} - humanized name.
	 * 	<li>{@code code} - public field, {@code @Schema(title)} - schema title (field path).
	 * 	<li>{@code name} - getter, {@code @Schema(title)} - schema title (getter path).
	 * 	<li>{@code notes} - getter, {@code @Schema(title="")} - empty schema title falls back to humanized name.
	 * 	<li>{@code releaseDate} - getter, no {@code @Schema} - humanized (camel-case split); value left null.
	 * 	<li>{@code secret} - write-only (setter only) - skipped (not readable).
	 * </ul>
	 */
	public static class Row {
		public String order = "o";
		@Schema(title="Ship Code") public String code = "c";
		private String releaseDate;
		@Schema(title="Full Name") public String getName() { return "Alice"; }
		@Schema(title="") public String getNotes() { return "n"; }
		public String getReleaseDate() { return releaseDate; }
		public void setSecret(@SuppressWarnings("unused") String value) { /* write-only */ }
	}

	private static Map<String,String> byData(List<Map<String,Object>> cols) {
		var m = new LinkedHashMap<String,String>();
		cols.forEach(c -> m.put((String)c.get("data"), (String)c.get("title")));
		return m;
	}

	//------------------------------------------------------------------------------------------------------------------
	// DataTablesColumns
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_columnTitleSources() {
		var cols = DataTablesColumns.of(Row.class);
		var titles = byData(cols);
		assertEquals("Order", titles.get("order"), titles::toString);          // humanized public field
		assertEquals("Ship Code", titles.get("code"), titles::toString);       // @Schema on field
		assertEquals("Full Name", titles.get("name"), titles::toString);       // @Schema on getter
		assertEquals("Notes", titles.get("notes"), titles::toString);          // empty @Schema title -> humanized
		assertEquals("Release Date", titles.get("releaseDate"), titles::toString); // camel-case humanization
		assertFalse(titles.containsKey("secret"), titles::toString);           // write-only skipped
	}

	@Test void a02_columnFlagsDefaultTrue() {
		var cols = DataTablesColumns.of(Row.class);
		var name = cols.stream().filter(c -> "name".equals(c.get("data"))).findFirst().orElseThrow();
		assertEquals(Boolean.TRUE, name.get("orderable"));
		assertEquals(Boolean.TRUE, name.get("searchable"));
	}

	/** Pure getter/setter bean (no public fields) with a write-only property to exercise the read-only filter. */
	public static class WriteOnlyRow {
		public String getVisible() { return "v"; }
		public void setHidden(@SuppressWarnings("unused") String value) { /* write-only */ }
	}

	@Test void a03_notABeanThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> DataTablesColumns.of(String.class));
		assertTrue(e.getMessage().contains("not a bean"), e::getMessage);
	}

	@Test void a04_humanizeEdgeCases() {
		assertEquals("Name", DataTablesColumns.humanize("name"));
		assertEquals("First Name", DataTablesColumns.humanize("firstName"));
		assertEquals("", DataTablesColumns.humanize(""));
		// Consecutive capitals: only a lower->upper boundary inserts a space, so an upper char preceded by a
		// non-lowercase char (the second 'B'/'C' below) must NOT split.
		assertEquals("A BC", DataTablesColumns.humanize("aBC"));
	}

	@Test void a05_writeOnlyPropertySkipped() {
		var titles = byData(DataTablesColumns.of(WriteOnlyRow.class));
		assertTrue(titles.containsKey("visible"), titles::toString);
		assertFalse(titles.containsKey("hidden"), titles::toString);
	}

	@Test void a06_marshallingContextOverload() {
		var cols = byData(DataTablesColumns.of(MarshallingContext.DEFAULT, Row.class));
		assertEquals("Ship Code", cols.get("code"), cols::toString);
	}

	//------------------------------------------------------------------------------------------------------------------
	// DataTablesTable
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_beanRowsRender() {
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of("releases", List.of(new Row()), Row.class));
		assertTrue(html.contains("id='releases'") || html.contains("id=\"releases\""), html);
		assertTrue(html.contains(DataTablesTable.MARKER_ATTR), html);
		assertTrue(html.contains("Ship Code") && html.contains("Full Name"), html);  // thead titles
		assertTrue(html.contains("Alice"), html);                                     // tbody cell (bean read)
	}

	@Test void b02_mapRowsWithExplicitColumnsAndNullCell() {
		var columns = List.of(
			col("name", "Name"),
			col("age", "Age")
		);
		var rows = new ArrayList<Map<String,Object>>();
		rows.add(new LinkedHashMap<>(Map.of("name", "Bob", "age", 40)));
		var missing = new LinkedHashMap<String,Object>();
		missing.put("name", "Carol");   // "age" intentionally absent -> null cell
		rows.add(missing);
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of("people", rows, columns));
		assertTrue(html.contains("Bob") && html.contains("Carol"), html);
		assertTrue(html.contains(">40<") || html.contains("40"), html);
	}

	@Test void b03_emptyRows() {
		var html = HtmlSerializer.DEFAULT.toString(DataTablesTable.of("empty", List.of(), Row.class));
		assertTrue(html.contains("id='empty'") || html.contains("id=\"empty\""), html);
	}

	// MarshallingContext overloads (of(ctx,id,rows,rowType) and of(ctx,id,rows,columns)) render the same content.
	@Test void b04_marshallingContextOverloads() {
		var fromType = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "releases", List.of(new Row()), Row.class));
		assertTrue(fromType.contains("Alice") && fromType.contains("Ship Code"), fromType);

		var rows = new ArrayList<Map<String,Object>>();
		rows.add(new LinkedHashMap<>(Map.of("name", "Bob")));
		var fromCols = HtmlSerializer.DEFAULT.toString(DataTablesTable.of(MarshallingContext.DEFAULT, "people", rows, List.of(col("name", "Name"))));
		assertTrue(fromCols.contains("Bob"), fromCols);
	}

	private static Map<String,Object> col(String data, String title) {
		var m = new LinkedHashMap<String,Object>();
		m.put("data", data);
		m.put("title", title);
		return m;
	}

	//------------------------------------------------------------------------------------------------------------------
	// DataTablesMixin
	//------------------------------------------------------------------------------------------------------------------

	@Rest(mixins=DataTablesMixin.class)
	public static class GlueHost implements BasicUniversalConfig {}

	@Test void c01_glueScriptServed() throws Exception {
		var c = MockRestClient.build(GlueHost.class);
		var resp = c.get("/juneau-datatables.js").run().assertStatus(200);
		var body = resp.getContent().asString();
		assertTrue(body.contains("DataTable"), body);
		assertTrue(body.contains("data-juneau-datatable"), body);
		assertTrue(resp.getHeader("Content-Type").asString().orElse("").contains("javascript"),
			resp.getHeader("Content-Type").asString().orElse("(none)"));
		// Second request exercises the memoized (already-cached) fast path of the double-checked-locking glue-script reader.
		var body2 = c.get("/juneau-datatables.js").run().assertStatus(200).getContent().asString();
		assertEquals(body, body2, body2);
	}
}
