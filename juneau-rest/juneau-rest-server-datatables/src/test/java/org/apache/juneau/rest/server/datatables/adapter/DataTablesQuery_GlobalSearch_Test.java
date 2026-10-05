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
package org.apache.juneau.rest.server.datatables.adapter;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.DataTablesRequest.Column;
import org.apache.juneau.rest.server.datatables.DataTablesRequest.Search;
import org.junit.jupiter.api.*;

/**
 * Tests {@link DataTablesQuery}'s per-{@link SearchType} global-search leaf selection (design §9), on top of
 * {@code DataTablesQuery_Test}'s per-column search / sort / page / counts coverage.
 */
class DataTablesQuery_GlobalSearch_Test extends TestBase {

	private static List<Column> cols() {
		return new ArrayList<>(List.of(new Column().setData("name"), new Column().setData("age")));
	}

	@Test void a01_globalSearch_synthesizesOrOfContains() {
		var req = new DataTablesRequest().setColumns(cols()).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.TEXT));
		assertEquals("$or(name=$contains(\"bob\"),age=$contains(\"bob\"))", q.getSearch());
	}

	@Test void a02_globalSearch_andedWithPerColumnSearch() {
		var columns = cols();
		columns.get(0).setSearch(new Search().setValue("$eq(Alice)"));
		var req = new DataTablesRequest().setColumns(columns).setSearch(new Search().setValue("x"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.TEXT));
		assertEquals("name=$eq(Alice),$or(name=$contains(\"x\"),age=$contains(\"x\"))", q.getSearch());
	}

	@Test void a03_blankGlobalSearch_addsNothing() {
		var req = new DataTablesRequest().setColumns(cols()).setSearch(new Search().setValue("   "));
		assertNull(DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.TEXT)).getSearch());
	}

	@Test void a04_noSearchDescriptor_addsNothing() {
		var req = new DataTablesRequest().setColumns(cols());
		assertNull(DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.TEXT)).getSearch());
	}

	@Test void a05_nonSearchableColumnExcludedFromGlobalSearch() {
		var columns = cols();
		columns.get(0).setSearchable(false);
		var req = new DataTablesRequest().setColumns(columns).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.TEXT));
		assertEquals("age=$contains(\"bob\")", q.getSearch());  // one searchable column left -> unwrapped, no $or(...)
	}

	/** Builds a {@link BeanQueryContext} declaring the given {@code column, SearchType, column, SearchType, ...} pairs. */
	private static BeanQueryContext<?> ctx(Object...pairs) {
		var b = InMemoryBeanQueryContext.<Map<String,Object>>create();
		for (var i = 0; i < pairs.length; i += 2)
			b.column((String) pairs[i], (SearchType) pairs[i + 1]);
		return b.build();
	}

	@Test void b01_numeric_termParses_addsEq() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("age"))).setSearch(new Search().setValue("42"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("age", SearchType.NUMERIC));
		assertEquals("age=$eq(\"42\")", q.getSearch());
	}

	@Test void b02_numeric_termDoesNotParse_columnSkipped() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("age"))).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("age", SearchType.NUMERIC));
		assertNull(q.getSearch());
	}

	@Test void b03_boolean_termParses_addsEq() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("active"))).setSearch(new Search().setValue("true"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("active", SearchType.BOOLEAN));
		assertEquals("active=$eq(\"true\")", q.getSearch());
	}

	@Test void b04_boolean_termDoesNotParse_columnSkipped() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("active"))).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("active", SearchType.BOOLEAN));
		assertNull(q.getSearch());
	}

	@Test void b05_timestamp_termParses_addsEq() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("createdAt")))
			.setSearch(new Search().setValue("2026-09-30T12:00:00Z"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("createdAt", SearchType.TIMESTAMP));
		assertEquals("createdAt=$eq(\"2026-09-30T12:00:00Z\")", q.getSearch());
	}

	@Test void b06_timestamp_termDoesNotParse_columnSkipped() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("createdAt"))).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("createdAt", SearchType.TIMESTAMP));
		assertNull(q.getSearch());
	}

	@Test void b07_enum_alwaysEligible_addsEqic() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("status"))).setSearch(new Search().setValue("OPEN"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("status", SearchType.ENUM));
		assertEquals("status=$eqic(\"OPEN\")", q.getSearch());
	}

	@Test void b08_enum_noParseGate_evenAnOddTermAddsEqic() {
		// unlike NUMERIC/BOOLEAN/TIMESTAMP, ENUM has no parse gate (same bucket as TEXT/ID/VERSION in the spec's
		// per-type table) -- any non-blank term is eligible, exactly like $contains/$prefix.
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("status"))).setSearch(new Search().setValue("not open"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("status", SearchType.ENUM));
		assertEquals("status=$eqic(\"not open\")", q.getSearch());
	}

	@Test void b09_version_usesPrefix() {
		var req = new DataTablesRequest().setColumns(List.of(new Column().setData("ver"))).setSearch(new Search().setValue("10.0"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("ver", SearchType.VERSION));
		assertEquals("ver=$prefix(\"10.0\")", q.getSearch());
	}

	@Test void b10_mixedColumns_onlyParsingTypesContribute_unwrapsSingleSurvivor() {
		var columns = List.of(new Column().setData("name"), new Column().setData("age"), new Column().setData("active"));
		var req = new DataTablesRequest().setColumns(columns).setSearch(new Search().setValue("bob"));
		var q = DataTablesQuery.toBeanQuery(req, ctx("name", SearchType.TEXT, "age", SearchType.NUMERIC, "active", SearchType.BOOLEAN));
		// "bob" parses as neither NUMERIC nor BOOLEAN, so only the TEXT column survives -> unwrapped, no $or(...)
		assertEquals("name=$contains(\"bob\")", q.getSearch());
	}

	public static class Rec {
		private final String name;
		private final int age;

		Rec(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() { return name; }
		public int getAge() { return age; }

		@Override public String toString() { return name; }
	}

	private static final List<Rec> ROWS = List.of(
		new Rec("Alice", 30),
		new Rec("Bob", 25),
		new Rec("Carol", 40),
		new Rec("Dave", 17)
	);

	private static InMemoryBeanQueryContext<Rec> context() {
		return InMemoryBeanQueryContext.create(Rec.class).build();  // name: TEXT, age: NUMERIC (inferred).
	}

	private static List<String> names(DataTablesResults<Rec> r) {
		return r.getData().stream().map(Rec::getName).toList();
	}

	@Test
	void c01_globalSearchRunsThroughARealSession() {
		var columns = new ArrayList<>(List.of(new Column().setData("name"), new Column().setData("age")));
		var req = new DataTablesRequest().setLength(10).setColumns(columns).setSearch(new Search().setValue("car"));
		DataTablesResults<Rec> r;
		try (var s = context().getSession(ROWS)) {
			r = DataTablesQuery.run(req, s);
		}
		// "car" matches Carol by name ($contains) and nothing by age (not numeric) — the per-type global $or (gap
		// 5/D9) resolves to one eligible column here, but it still goes through FilterEvaluator end to end.
		assertList(names(r), "Carol");
		assertBean(r, "recordsTotal,recordsFiltered", "4,1");
	}
}
