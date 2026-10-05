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

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.apache.juneau.commons.utils.Shorts.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Operator gating on {@link Column} (design §4.3): a column's offerable operators are modeled by a single
 * {@link SearchOperatorSet} &mdash; absent means the full type-applicable {@link SearchOperatorSet#standard()
 * standard} universe (or the inherited table-level set), present entirely <b>replaces</b> the table-level set for
 * that column; plus the {@link ViewDef} column container and its own table-level set.
 */
class ColumnSearchGating_Test extends TestBase {

	private static List<String> opNames(Column c) {
		return c.effectiveOperators().stream().map(SearchOperator::name).toList();
	}

	private static List<String> opNames(List<SearchOperator> ops) {
		return ops.stream().map(SearchOperator::name).toList();
	}

	@Nested class A_setAbsent {

		@Test void a01_textDefaultsToAllApplicableBuiltins() {
			var c = Column.create("name").searchType(TEXT);
			assertEquals(List.of("$eq", "$eqic", "$ne", "$in", "$and", "$or", "$not", "$contains", "$prefix", "$regex", "$blank"), opNames(c));
		}

		@Test void a02_numericDefaults() {
			var c = Column.create("count").searchType(NUMERIC);
			assertEquals(List.of("$eq", "$ne", "$in", "$and", "$or", "$not", "$gt", "$gte", "$lt", "$lte", "$between"), opNames(c));
		}

		@Test void a03_booleanDefaults() {
			var c = Column.create("active").searchType(BOOLEAN);
			assertEquals(List.of("$eq", "$ne", "$and", "$or", "$not"), opNames(c));
		}

		@Test void a04_offersApplicableRejectsInapplicable() {
			var c = Column.create("name").searchType(TEXT);
			assertMapped(c, Column::offers, "$contains,$gt,$madeup", "true,false,false");  // $gt: ordered op not applicable to text → reject
			assertFalse(c.offers(null));
		}
	}

	@Nested class B_setPresent {

		@Test void b01_exactlyNamed() {
			var c = Column.create("name").searchType(TEXT)
				.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), SearchOperatorSet.standard().get("$contains")));
			assertEquals(List.of("$eq", "$contains"), opNames(c));
			assertMapped(c, Column::offers, "$eq,$ne", "true,false");  // $ne: built-in omitted from the set → off
		}

		@Test void b02_namedButInapplicableBuiltinDroppedByForType() {
			// $gt is in the set but not applicable to TEXT, so forType() drops it.
			var c = Column.create("name").searchType(TEXT)
				.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), SearchOperatorSet.standard().get("$gt")));
			assertEquals(List.of("$eq"), opNames(c));
			assertFalse(c.offers("$gt"));
		}

		@Test void b03_customOperatorOffered() {
			var near = SearchOperator.create("$near", "Fuzzy. Example: $near(x)").types(TEXT).build();
			var c = Column.create("name").searchType(TEXT).operators(SearchOperatorSet.of(near, SearchOperatorSet.standard().get("$eq")));
			assertEquals(List.of("$near", "$eq"), opNames(c));  // an explicit set preserves the author's order
			assertTrue(c.offers("$near"));
		}

		@Test void b04_ownSetReplacesRatherThanMergesWithStandard() {
			// A column's own set entirely replaces the built-in universe: an omitted built-in is off, even though
			// it would otherwise be on under the default.
			var c = Column.create("name").searchType(TEXT).operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq")));
			assertFalse(c.offers("$contains"));
			assertEquals(List.of("$eq"), opNames(c));
		}

		@Test void b05_explicitEmptySetOffersNothing() {
			var c = Column.create("name").searchType(TEXT).operators(SearchOperatorSet.of());
			assertEquals(List.of(), opNames(c));
			assertFalse(c.offers("$eq"));
			assertNotNull(c.operators());  // present, but empty
			assertTrue(c.operators().operators().isEmpty());
		}

		@Test void b06_nullClearsBackToAbsent() {
			var c = Column.create("name").searchType(TEXT)
				.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq")))
				.operators(null);
			assertNull(c.operators());
			assertTrue(c.offers("$contains"));  // absent → default all
		}

		@Test void b07_standardWithCustomAppendsToFullUniverse() {
			var near = SearchOperator.create("$near", "Fuzzy. Example: $near(x)").types(TEXT).build();
			var c = Column.create("name").searchType(TEXT).operators(SearchOperatorSet.standard().with(near));
			assertEquals(List.of("$eq", "$eqic", "$ne", "$in", "$and", "$or", "$not", "$contains", "$prefix", "$regex", "$blank", "$near"), opNames(c));
			assertTrue(c.offers("$near"));
			var op = c.effectiveOperators().stream().filter(o -> eq(o.name(), "$near")).findFirst().orElseThrow();
			assertBean(op, "custom,help", "true,Fuzzy. Example: $near(x)");
		}
	}

	@Nested class C_notSearchable {

		@Test void c01_noSearchTypeMeansEmpty() {
			var c = Column.create("name");
			assertBean(c, "searchable,searchMeta", "false,<null>");
			assertEmpty(c.effectiveOperators());
			assertFalse(c.offers("$eq"));
		}

		@Test void c02_searchableAfterType() {
			var c = Column.create("name").searchType(ID);
			assertTrue(c.searchable());
		}
	}

	@Nested class D_validation {

		@Test void d01_columnNameRequired() {
			assertThrows(IllegalArgumentException.class, () -> Column.create(null));
			assertThrows(IllegalArgumentException.class, () -> Column.create("  "));
		}

		@Test void d02_labelAccessor() {
			var c = Column.create("name").label("Full Name");
			assertBean(c, "label,name", "Full Name,name");
			assertNull(Column.create("x").label());
		}
	}

	@Nested class E_viewDef {

		@Test void e01_columnsInOrderAndLookup() {
			var v = ViewDef.create("releases")
				.column(Column.create("name").searchType(TEXT))
				.column(Column.create("count").searchType(NUMERIC));
			// Not BCT-converted: ViewDef has a private field named "columns" (a Map<String,Column>) that shadows
			// the columns() getter in BCT's property-extractor lookup order (declared-field match runs before the
			// exact-name zero-arg method fallback), so "columns{#{name}}" resolves to the raw Map, not the List.
			assertEquals("releases", v.id());
			assertEquals(List.of("name", "count"), v.columns().stream().map(Column::name).toList());
			assertEquals(TEXT, v.findColumn("name").searchType());
			assertNull(v.findColumn("missing"));
			assertNull(v.findColumn(null));
		}

		@Test void e02_validation() {
			assertThrows(IllegalArgumentException.class, () -> ViewDef.create(null));
			assertThrows(IllegalArgumentException.class, () -> ViewDef.create(" "));
			var v = ViewDef.create("v");
			assertThrows(IllegalArgumentException.class, () -> v.column(null));
		}

		@Test void e03_tableOperatorsAccessor() {
			var set = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
			var v = ViewDef.create("releases").operators(set);
			assertSame(set, v.operators());
			assertNull(ViewDef.create("x").operators());
		}
	}

	@Nested class F_tableInheritance {

		@Test void f01_columnWithNoOwnSetInheritsTable() {
			var tableSet = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), SearchOperatorSet.standard().get("$contains"));
			var c = Column.create("name").searchType(TEXT);
			assertEquals(List.of("$eq", "$contains"), opNames(c.effectiveOperators(tableSet)));
		}

		@Test void f02_columnWithOwnSetReplacesTable() {
			var tableSet = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), SearchOperatorSet.standard().get("$contains"));
			var c = Column.create("name").searchType(TEXT).operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$prefix")));
			assertEquals(List.of("$prefix"), opNames(c.effectiveOperators(tableSet)));
		}

		@Test void f03_nullTableDefaultFallsBackToStandard() {
			var c = Column.create("name").searchType(TEXT);
			assertEquals(List.of("$eq", "$eqic", "$ne", "$in", "$and", "$or", "$not", "$contains", "$prefix", "$regex", "$blank"),
				opNames(c.effectiveOperators(null)));
		}

		@Test void f04_notSearchableIgnoresTableDefault() {
			var tableSet = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
			var c = Column.create("name");
			assertEquals(List.of(), c.effectiveOperators(tableSet));
		}

		@Test void f05_searchMetaInheritsTable() {
			var tableSet = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
			var search = Column.create("name").searchType(TEXT).searchMeta(tableSet);
			assertBean(search, "operators{length,0{name}}", "{1,{$eq}}");
		}

		@Test void f06_searchMetaOwnSetReplacesTable() {
			var tableSet = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
			var search = Column.create("name").searchType(TEXT)
				.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$contains")))
				.searchMeta(tableSet);
			assertBean(search, "operators{length,0{name}}", "{1,{$contains}}");
		}
	}

	@Nested class G_searchMeta {

		/** The operator {@code name}s of a {@code search.operators} block, in wire order. */
		private static List<String> opNames(JsonMap search) {
			var ops = search.getList("operators");
			var l = new ArrayList<String>();
			for (var i = 0; i < ops.size(); i++)
				l.add(ops.getMap(i).getString("name"));
			return l;
		}

		@Test void g01_instance_nonSearchableColumnHasNoBlock() {
			assertNull(Column.create("name").searchMeta());
		}

		@Test void g02_instance_blockCarriesTypeAndOperatorFields() {
			var search = Column.create("name").searchType(TEXT).searchMeta();
			assertEquals("text", search.getString("type"));
			// $eq is a non-combinator built-in; $and is a combinator; each entry carries the full field set.
			var eq = search.getList("operators").getMap(0);
			assertBean(eq, "name,minArgs,combinator,custom", "$eq,1,false,false");
			assertTrue(eq.getString("help").startsWith("Exact, case-sensitive match."), eq::toString);
			assertTrue(opNames(search).contains("$and"));
			var and = search.getList("operators").stream()
				.map(o -> (JsonMap) o).filter(o -> eq(o.getString("name"), "$and")).findFirst().orElseThrow();
			assertTrue(and.getBoolean("combinator"));
		}

		@Test void g03_static_unknownWireIsNonSearchable() {
			assertNull(Column.searchMeta("name", "bogus", null, null));
			assertNull(Column.searchMeta("name", null, null, null));
		}

		@Test void g04_static_listAbsentIsFullUniverse() {
			var search = Column.searchMeta("name", "text", null, null);
			assertBean(search, "type,operators{#{name}}",
				"text,{[{$eq},{$eqic},{$ne},{$in},{$and},{$or},{$not},{$contains},{$prefix},{$regex},{$blank}]}");
		}

		@Test void g05_static_explicitAllowListIntersectsInAuthorOrder() {
			var search = Column.searchMeta("count", "numeric", List.of("$eq", "$gt", "$between"), null);
			assertEquals(List.of("$eq", "$gt", "$between"), opNames(search));
		}

		@Test void g06_static_customOperatorAppendedAndFlagged() {
			var customs = List.of(Map.of("name", "$near", "help", "Fuzzy match. Example: $near(jhon)"));
			var search = Column.searchMeta("status", "text", null, customs);
			var ops = search.getList("operators");
			var near = ops.getMap(ops.size() - 1);
			assertBean(near, "name,custom,help", "$near,true,Fuzzy match. Example: $near(jhon)");
		}

		@Test void g07_static_emptyCustomListIsHarmless() {
			var search = Column.searchMeta("name", "text", null, List.of());
			assertEquals("text", search.getString("type"));
		}
	}
}
