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

import java.net.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.converter.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * Server-side wiring + ribbon-toggle&rarr;query-arg mapping tests (design doc §6, Task B.5).
 *
 * <p>
 * Two seams are proven here:
 * <ul class='spaced-list'>
 * 	<li>The <b>pure</b> {@link RibbonAction#toQueryParams(ViewDef)} mapping: a column-scoped {@code option} contributes
 * 		the DataTables {@code columns[N][search][value]} request param (resolving the column key to its index), and a
 * 		custom-param {@code option} contributes its own {@code param=value} (which the endpoint, not
 * 		{@code DataTablesQueryProtocol}, must parse).
 * 	<li>The <b>end-to-end</b> server wiring: a view's {@link ViewDef#queryableSettings()} helper registers the
 * 		{@code DataTablesQueryProtocol(rowType)} so a {@link ProtocolQueryable}-converted op returns a
 * 		{@code DataTablesResults} envelope with the ribbon toggle's filter applied &mdash; the same
 * 		{@code columns[1][search][value]} key the pure mapping emits.
 * </ul>
 */
class ViewServerWiring_Test extends TestBase {

	/** Row bean: {@code name} (col 0), {@code status} (col 1), {@code date} (col 2). */
	public static class Release {
		public String name;
		public String status;
		public String date;
		public Release() {}
		public Release(String name, String status, String date) { this.name = name; this.status = status; this.date = date; }
	}

	/** The representative "releases" view with a column-scoped status quick-filter option ({@code status} = index 1). */
	private static ViewDef releasesView() {
		return ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(
				Column.of("name").title("Name"),
				Column.of("status").title("Status"),
				Column.of("date").title("Date"))
			.ribbon(
				RibbonAction.columnSearchToggle(),
				RibbonAction.option("show-superseded").title("Show superseded").column("status").value("superseded").persist(true),
				RibbonAction.refresh())
			.build();
	}

	//------------------------------------------------------------------------------------------------------------------
	// Step 1: pure column-scoped mapping -> columns[N][search][value]
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_columnScopedOption_mapsToColumnSearchValueByIndex() {
		var params = RibbonAction.toQueryParams(releasesView());
		// status is column index 1, so the "show-superseded" toggle contributes columns[1][search][value]=superseded.
		assertEquals("superseded", params.get("columns[1][search][value]"));
	}

	@Test void a02_nonOptionActions_contributeNothing() {
		// columnSearchToggle/refresh are not query-contributing options -> the only mapped key is the option's.
		var params = RibbonAction.toQueryParams(releasesView());
		assertEquals(1, params.size(), () -> "unexpected params: " + params);
	}

	@Test void a03_unknownColumn_isRejected() {
		var bad = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.ribbon(RibbonAction.option("o").column("nope").value("v"))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> RibbonAction.toQueryParams(bad));
		assertTrue(e.getMessage().contains("nope"), e::getMessage);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Step 2: non-column-scoped custom param -> param=value (endpoint's responsibility to parse)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_customParamOption_mapsToOwnParam() {
		var v = ViewDef.create("releases")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/releases/data")
			.columns(Column.of("name").title("Name"))
			.ribbon(RibbonAction.option("include-deleted").param("includeDeleted").value("true"))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertEquals("true", params.get("includeDeleted"));
		// It must NOT be shaped as a DataTables column-search param.
		assertFalse(params.keySet().stream().anyMatch(k -> k.startsWith("columns[")), () -> "params: " + params);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Step 3/4: end-to-end server wiring via ViewDef.queryableSettings() -> DataTablesResults envelope
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=ProtocolQueryable.class)
	public static class ReleasesHost implements BasicUniversalConfig {
		static final ViewDef VIEW = releasesView();
		@RestOp(path="/data")
		public List<Release> data() {
			return List.of(
				new Release("1.0", "released", "2024-01-01"),
				new Release("2.0", "superseded", "2023-01-01"),
				new Release("3.0", "superseded", "2022-01-01"),
				new Release("4.0", "draft", "2025-01-01"));
		}
		/** Server-side wiring helper: keeps the protocol's rowType in sync with the ViewDef. */
		@Bean public QueryableSettings queryableSettings() {
			return VIEW.queryableSettings();
		}
	}

	/** The full DataTables column-descriptor block for the (name, status, date) view. */
	private static String[] cols() {
		return new String[] {
			"columns[0][data]", "name",   "columns[0][searchable]", "true", "columns[0][orderable]", "true",
			"columns[1][data]", "status", "columns[1][searchable]", "true", "columns[1][orderable]", "true",
			"columns[2][data]", "date",   "columns[2][searchable]", "true", "columns[2][orderable]", "true"
		};
	}

	private static String qs(String...kv) {
		var parts = new ArrayList<String>();
		for (var i = 0; i < kv.length; i += 2)
			parts.add(enc(kv[i]) + "=" + enc(kv[i+1]));
		return String.join("&", parts);
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	@Test void c01_ribbonToggle_filtersServerSide_returningEnvelope() throws Exception {
		var c = MockRestClient.buildJson(ReleasesHost.class);

		// Derive the exact search key/value the ribbon toggle contributes (proves the pure mapping drives the wire).
		var toggle = RibbonAction.toQueryParams(ReleasesHost.VIEW);
		var key = "columns[1][search][value]";
		assertEquals("superseded", toggle.get(key));

		var kv = new ArrayList<>(List.of("draw", "1", "start", "0", "length", "10"));
		kv.addAll(List.of(cols()));
		kv.addAll(List.of(key, toggle.get(key)));

		var body = c.get("/data?" + qs(kv.toArray(String[]::new))).run().cacheContent().assertStatus(200).getContent().asString();

		// Envelope shape: {draw, recordsTotal, recordsFiltered, data}.
		assertTrue(body.contains("\"draw\":1"), body);
		assertTrue(body.contains("\"recordsTotal\":4"), body);
		assertTrue(body.contains("\"recordsFiltered\":2"), body);
		assertTrue(body.contains("\"data\":["), body);
		// Only the two superseded releases survive the column filter.
		assertTrue(body.contains("2.0") && body.contains("3.0"), body);
		assertFalse(body.contains("1.0") || body.contains("4.0"), body);
	}

	@Test void c02_queryableSettings_carriesDataTablesProtocol() {
		// The helper must yield a settings bean whose protocol is the DataTables adapter (not the native default).
		var s = releasesView().queryableSettings();
		assertNotNull(s);
		assertNotSame(QueryableSettings.DEFAULT.protocol(), s.protocol());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Step 4: gap-closing branch coverage for toQueryParams() edge cases (design doc §6.5)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_nullRibbon_contributesNothing() {
		// No .ribbon(...) declared -> ribbon stays null; toQueryParams must short-circuit, not NPE.
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertTrue(params.isEmpty(), () -> "params: " + params);
	}

	@Test void d02_optionGroupWithMembers_mapsColumnAndParamOptions() {
		// optionGroup member options are mapped uniformly with top-level options (both column- and param-scoped).
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"), Column.of("status").title("Status"))
			.ribbon(RibbonAction.optionGroup("g")
				.options(
					RibbonAction.Opt.of("a").column("status").value("released"),
					RibbonAction.Opt.of("b").param("custom").value("v2")))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertEquals("released", params.get("columns[1][search][value]"));
		assertEquals("v2", params.get("custom"));
		assertEquals(2, params.size(), () -> "params: " + params);
	}

	@Test void d03_optionGroupWithNoOptions_contributesNothing() {
		// No .options(...) declared on the optionGroup -> options stays null; must be skipped, not NPE.
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.ribbon(RibbonAction.optionGroup("g"))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertTrue(params.isEmpty(), () -> "params: " + params);
	}

	@Test void d04_optionWithNoValue_contributesNothing() {
		// A column-scoped option with no .value(...) set is a no-op (valueless options contribute nothing).
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.ribbon(RibbonAction.option("o").column("name"))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertTrue(params.isEmpty(), () -> "params: " + params);
	}

	@Test void d05_optionWithValueButNoColumnOrParam_contributesNothing() {
		// A value-bearing option with neither .column(...) nor .param(...) set has nowhere to map -> no-op.
		var v = ViewDef.create("x")
			.rowType(Release.class)
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.columns(Column.of("name").title("Name"))
			.ribbon(RibbonAction.option("o").value("v"))
			.build();
		var params = RibbonAction.toQueryParams(v);
		assertTrue(params.isEmpty(), () -> "params: " + params);
	}

	@Test void d06_columnScopedOption_withNullColumns_throws() {
		// No rowType and no .columns(...) declared -> viewDef.columns stays null; resolving a column-scoped option
		// must still fail loud (not NPE) with the same "unknown column" message as the populated-columns case.
		var v = ViewDef.create("x")
			.dataMode(DataMode.SERVER)
			.dataUrl("servlet:/x/data")
			.ribbon(RibbonAction.option("o").column("foo").value("v"))
			.build();
		var e = assertThrows(IllegalArgumentException.class, () -> RibbonAction.toQueryParams(v));
		assertTrue(e.getMessage().contains("foo"), e::getMessage);
	}
}
