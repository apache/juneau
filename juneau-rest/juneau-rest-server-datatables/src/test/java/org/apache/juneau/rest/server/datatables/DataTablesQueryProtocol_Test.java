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

import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.config.*;
import org.apache.juneau.rest.server.converter.*;
import org.junit.jupiter.api.*;

/**
 * Tests the DataTables server-side-processing protocol against the
 * <a class="doclink" href="https://datatables.net/manual/server-side">DataTables server-side contract</a>.
 */
class DataTablesQueryProtocol_Test extends TestBase {

	/** A resource wired to the DataTables protocol via a {@link QueryableSettings} bean. */
	@Rest(converters=ProtocolQueryable.class)
	public static class A implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> a() {
			return List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			);
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).build();
		}
	}

	/** Issues a GET with the given DataTables params and returns the (cached) response body. */
	private static String get(MockRestClient c, String...kv) throws Exception {
		return c.get("/?" + qs(kv)).run().cacheContent().assertStatus(200).getContent().asString();
	}

	/** Builds an encoded query string from key/value pairs (no leading {@code ?}). */
	private static String qs(String...kv) {
		var parts = new ArrayList<String>();
		for (var i = 0; i < kv.length; i += 2)
			parts.add(enc(kv[i]) + "=" + enc(kv[i+1]));
		return String.join("&", parts);
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	/** The standard two-column (name, age) descriptor block. */
	private static String[] cols() {
		return new String[] {
			"columns[0][data]", "name", "columns[0][searchable]", "true", "columns[0][orderable]", "true",
			"columns[1][data]", "age",  "columns[1][searchable]", "true", "columns[1][orderable]", "true"
		};
	}

	private static String[] params(String[] head, String...cols) {
		return Stream.concat(Stream.of(head), Stream.of(cols)).toArray(String[]::new);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Envelope shape
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_envelopeShape() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","start","0","length","10"}, cols()));
		assertTrue(body.contains("\"draw\":1"), body);
		assertTrue(body.contains("\"recordsTotal\":3"), body);
		assertTrue(body.contains("\"recordsFiltered\":3"), body);
		assertTrue(body.contains("\"data\":["), body);
		assertTrue(body.contains("Alice") && body.contains("Bob") && body.contains("Charlie"), body);
	}

	@Test void a02_drawEchoedBack() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","7"}, cols()));
		assertTrue(body.contains("\"draw\":7"), body);
	}

	@Test void a03_errorOmittedWhenNull() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1"}, cols()));
		// The null error field is omitted, so the JSON carries no "error" key.
		assertFalse(body.contains("\"error\""), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Paging
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_pagingLength() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		// order by name asc so the page window is deterministic.
		var body = get(c, params(new String[]{"draw","1","start","0","length","2","order[0][column]","0","order[0][dir]","asc"}, cols()));
		assertTrue(body.contains("Alice") && body.contains("Bob"), body);
		assertFalse(body.contains("Charlie"), body);
		assertTrue(body.contains("\"recordsTotal\":3") && body.contains("\"recordsFiltered\":3"), body);
	}

	@Test void b02_pagingStart() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","start","2","length","2","order[0][column]","0","order[0][dir]","asc"}, cols()));
		assertTrue(body.contains("Charlie"), body);
		assertFalse(body.contains("Alice") || body.contains("Bob"), body);
	}

	@Test void b03_lengthMinusOneReturnsAll() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","start","0","length","-1"}, cols()));
		assertTrue(body.contains("Alice") && body.contains("Bob") && body.contains("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Ordering
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_orderDescByName() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","order[0][column]","0","order[0][dir]","desc"}, cols()));
		// Charlie should appear before Alice in a descending name sort.
		assertTrue(body.indexOf("Charlie") < body.indexOf("Alice"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Global (cross-column OR) search
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_globalSearchMatchesOneColumn() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","Alice","search[regex]","false"}, cols()));
		assertTrue(body.contains("Alice"), body);
		assertTrue(body.contains("\"recordsTotal\":3") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Per-column (AND) search
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_columnSearch() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","columns[0][search][value]","Bob"}, cols()));
		assertTrue(body.contains("Bob") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Alice") || body.contains("Charlie"), body);
	}

	@Test void e02_columnSearchRegexFlagIgnoredWhenNotAllowed() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		// Resource A does not enable allowRegexSearch, so a regex=true flag is treated as a literal case-insensitive
		// substring search - "Bob" still matches the single row.
		var body = get(c, params(new String[]{"draw","1","columns[0][search][value]","Bob","columns[0][search][regex]","true"}, cols()));
		assertTrue(body.contains("Bob") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Alice") || body.contains("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Column-key resolution / degenerate column + order descriptors
	//------------------------------------------------------------------------------------------------------------------

	// data empty => key falls back to name; data numeric (array-index source) => key falls back to name.
	@Test void f01_columnKeyFallbacks() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","",  "columns[0][name]","name", "columns[0][searchable]","true",
			"columns[1][data]","0", "columns[1][name]","age",  "columns[1][searchable]","true",
			// Column-0 key resolved to "name" (from empty data) - search it to prove the fallback worked.
			"columns[0][search][value]","Alice");
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// Exercises the false arms of the per-column AND / global OR predicates:
	// a non-searchable column with a search value, and a column with an empty key.
	@Test void f02_nonSearchableAndEmptyKeyColumns() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"search[value]","Alice",
			"columns[0][data]","name", "columns[0][searchable]","true",  "columns[0][search][value]","",
			"columns[1][data]","age",  "columns[1][searchable]","false", "columns[1][search][value]","30",
			"columns[2][data]","",     "columns[2][name]","",            "columns[2][searchable]","true");
		// Global "Alice" matches only via the searchable, non-empty-key name column.
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// order referencing an out-of-range index, a negative index, and an empty-key column are all ignored;
	// the last valid order (name desc) wins.
	@Test void f03_orderEdgeCases() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","name", "columns[0][searchable]","true",
			"columns[1][data]","age",  "columns[1][searchable]","true",
			"columns[2][data]","",     "columns[2][name]","", "columns[2][searchable]","true",
			"order[0][column]","2", "order[0][dir]","asc",   // valid index but empty key -> skipped
			"order[1][column]","5", "order[1][dir]","asc",   // >= column count -> skipped
			"order[2][column]","-1","order[2][dir]","asc",   // negative index -> skipped
			"order[3][column]","0", "order[3][dir]","desc"); // name desc -> wins
		assertTrue(body.indexOf("Charlie") < body.indexOf("Alice"), body);
	}

	// Two searchable columns resolve to the same key; the de-dup guard scans it only once (the seen.add false arm),
	// so the global search still narrows to the single matching row.
	@Test void f04_globalSearchDeduplicatesRepeatedColumnKey() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"search[value]","Alice",
			"columns[0][data]","name", "columns[0][searchable]","true",
			"columns[1][data]","name", "columns[1][searchable]","true");
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// An order referencing a non-orderable column is skipped (the c.orderable false arm); the later valid order wins.
	@Test void f05_orderIgnoresNonOrderableColumn() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","name", "columns[0][searchable]","true", "columns[0][orderable]","true",
			"columns[1][data]","age",  "columns[1][searchable]","true", "columns[1][orderable]","false",
			"order[0][column]","1", "order[0][dir]","asc",   // age column is non-orderable -> skipped
			"order[1][column]","0", "order[1][dir]","desc"); // name desc -> wins
		assertTrue(body.indexOf("Charlie") < body.indexOf("Alice"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// recordsTotal/recordsFiltered fallback for a non-countable (non-collection) response
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=ProtocolQueryable.class)
	public static class B implements BasicUniversalConfig {
		@RestOp(path="/")
		public Map<String,Object> b() {
			return Map.of("name", "Solo", "age", 99);
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).build();
		}
	}

	@Test void g01_nonCountableResponseCountsFallBackToPageSize() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		var body = get(c, "draw", "3");
		assertTrue(body.contains("\"draw\":3"), body);
		// count() returns -1 for a non-collection response, so wrap() falls back to the emitted page size (1).
		assertTrue(body.contains("\"recordsTotal\":1") && body.contains("\"recordsFiltered\":1"), body);
		assertTrue(body.contains("Solo"), body);
	}

	// A global search over a non-collection response exercises the engine's "can't row-filter, return as-is" path
	// (searchAny -> asList returns null).
	@Test void g02_globalSearchOverNonCollectionResponse() throws Exception {
		var c = MockRestClient.buildJson(B.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","Solo"}, cols()));
		assertTrue(body.contains("Solo") && body.contains("\"recordsTotal\":1"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Array response - exercises the engine's array normalization (asList/count array branches)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(converters=ProtocolQueryable.class)
	public static class C implements BasicUniversalConfig {
		@RestOp(path="/")
		public Object[] c() {
			return new Object[] {
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			};
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).build();
		}
	}

	@Test void g03_arrayResponseGlobalSearch() throws Exception {
		var c = MockRestClient.buildJson(C.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","Alice"}, cols()));
		// recordsTotal counts the raw array (3); the OR search narrows the array-normalized rows to Alice (1).
		assertTrue(body.contains("Alice") && body.contains("\"recordsTotal\":3") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Direct unit coverage of the two internal helpers + shared normalized query types
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_patternFlavors() {
		// Non-regex => quoted contains (case-insensitive, whitespace-literal), embedded single-quotes escaped.
		assertEquals("'*Bill*'", DataTablesQueryProtocol.pattern("Bill", false, false));
		assertEquals("'*Bill Smith*'", DataTablesQueryProtocol.pattern("Bill Smith", false, false));
		assertEquals("'*O\\'Brien*'", DataTablesQueryProtocol.pattern("O'Brien", false, false));
		// Regex only when allowed; otherwise it falls back to the quoted literal form.
		assertEquals("/Bill/", DataTablesQueryProtocol.pattern("Bill", true, true));
		assertEquals("'*Bill*'", DataTablesQueryProtocol.pattern("Bill", true, false));
	}

	@Test void h02_toListNormalization() {
		assertTrue(DataTablesQueryProtocol.toList(null).isEmpty());
		assertEquals(2, DataTablesQueryProtocol.toList(List.of("a", "b")).size());
		assertEquals(2, DataTablesQueryProtocol.toList(new LinkedHashSet<>(List.of("a", "b"))).size());
		assertEquals(3, DataTablesQueryProtocol.toList(new int[]{1, 2, 3}).size());
		assertEquals(1, DataTablesQueryProtocol.toList("scalar").size());
	}

	@Test void h03_resultsErrorField() {
		var r = DataTablesResults.create().setError("boom");
		assertEquals("boom", r.getError());
	}

	// A null column or pattern is ignored, so the searchAny map stays absent.
	@Test void i01_queryArgsSearchAnyNullGuards() {
		var q = QueryArgs.create().searchAny(null, "*x*").searchAny("c", null).build();
		assertTrue(q.getSearchAny().isEmpty());
	}

	// With no protocol set, QueryableSettings resolves to the native protocol.
	@Test void i02_queryableSettingsDefaultsToNative() {
		assertSame(NativeQueryProtocol.INSTANCE, QueryableSettings.create().build().protocol());
		assertSame(NativeQueryProtocol.INSTANCE, QueryableSettings.DEFAULT.protocol());
	}

	// The native protocol advertises the historical s/v/o/p/l swagger descriptors via the swaggerParams() hook; the
	// SPI default (inherited by the DataTables protocol, which doesn't override it) is empty.
	@Test void i03_swaggerParamsHook() {
		assertEquals(Queryable.SWAGGER_PARAMS, NativeQueryProtocol.INSTANCE.swaggerParams());
		assertEquals("", new DataTablesQueryProtocol().swaggerParams());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Case-insensitive / literal search (item 1)
	//------------------------------------------------------------------------------------------------------------------

	// A lowercase term matches a capitalized value (quoted '*term*' flavor is case-insensitive).
	@Test void j01_globalSearchIsCaseInsensitive() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","alice"}, cols()));
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// A per-column lowercase term likewise matches case-insensitively.
	@Test void j02_columnSearchIsCaseInsensitive() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","columns[0][search][value]","bob"}, cols()));
		assertTrue(body.contains("Bob") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Alice") || body.contains("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Regex gating (item 2)
	//------------------------------------------------------------------------------------------------------------------

	/** Resource that opts into client regex search. */
	@Rest(converters=ProtocolQueryable.class)
	public static class R implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> r() {
			return List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			);
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).allowRegexSearch(true).build();
		}
	}

	// With allowRegexSearch on, a valid regex is honored as a full-match (/Alic./ matches "Alice").
	@Test void k01_regexHonoredWhenAllowed() throws Exception {
		var c = MockRestClient.buildJson(R.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","Alic.","search[regex]","true"}, cols()));
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// With allowRegexSearch on, a malformed regex is a 400 (not a 500).
	@Test void k02_malformedRegexIsBadRequest() throws Exception {
		var c = MockRestClient.create(R.class).json().ignoreErrors().build();
		c.get("/?" + qs(params(new String[]{"draw","1","search[value]","(a","search[regex]","true"}, cols()))).run().assertStatus(400);
	}

	// With allowRegexSearch off (resource A), a malformed regex term is treated literally - no match, no error.
	@Test void k03_malformedRegexLiteralWhenNotAllowed() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c, params(new String[]{"draw","1","search[value]","(a","search[regex]","true"}, cols()));
		assertTrue(body.contains("\"recordsFiltered\":0"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// AND-path text-in-numeric-column must not 500 (item 4)
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_textInNumericColumnIsBadRequestNot500() throws Exception {
		var c = MockRestClient.create(A.class).json().ignoreErrors().build();
		// "age" values are numeric; a text per-column term can't match a numeric column -> 400, not 500.
		c.get("/?" + qs(params(new String[]{"draw","1","columns[1][search][value]","abc"}, cols()))).run().assertStatus(400);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Column / order caps (item 3)
	//------------------------------------------------------------------------------------------------------------------

	/** Resource capped to a single parsed column. */
	@Rest(converters=ProtocolQueryable.class)
	public static class MC implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> mc() {
			return List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			);
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).maxColumns(1).build();
		}
	}

	// Column index 1 carries the per-column search, but the cap (1) stops parsing before it, so the search is
	// dropped and all three rows survive - proving the loop is bounded rather than parsing the extra column.
	@Test void m01_columnCapTruncatesParse() throws Exception {
		var c = MockRestClient.buildJson(MC.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","age",  "columns[0][searchable]","true",
			"columns[1][data]","name", "columns[1][searchable]","true", "columns[1][search][value]","Alice");
		assertTrue(body.contains("\"recordsFiltered\":3"), body);
		assertTrue(body.contains("Alice") && body.contains("Bob") && body.contains("Charlie"), body);
	}

	/** Resource capped to a single parsed order descriptor. */
	@Rest(converters=ProtocolQueryable.class)
	public static class MO implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Map<String,Object>> mo() {
			return List.of(
				Map.of("name", "Alice", "age", 30),
				Map.of("name", "Bob", "age", 25),
				Map.of("name", "Charlie", "age", 35)
			);
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol()).maxOrderColumns(1).build();
		}
	}

	// order[0] targets an empty-key column (skipped); order[1] (name desc) is past the cap (1) so it's never parsed,
	// leaving the rows unsorted in original order - proving the order loop is bounded.
	@Test void m02_orderCapTruncatesParse() throws Exception {
		var c = MockRestClient.buildJson(MO.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","name", "columns[0][searchable]","true",
			"columns[1][data]","",     "columns[1][name]","", "columns[1][searchable]","true",
			"order[0][column]","1", "order[0][dir]","asc",     // empty-key column -> skipped
			"order[1][column]","0", "order[1][dir]","desc");   // past the cap -> never parsed
		// No sort applied -> original order (Alice before Charlie).  With the 2nd order it would be Charlie before Alice.
		assertTrue(body.indexOf("Alice") < body.indexOf("Charlie"), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Positional array-index column fallback (item 5)
	//------------------------------------------------------------------------------------------------------------------

	public static class Prow {
		public String name;
		public int age;
		public Prow() {}
		public Prow(String name, int age) { this.name = name; this.age = age; }
	}

	/** Resource wiring a positional-aware protocol (row type supplied). */
	@Rest(converters=ProtocolQueryable.class)
	public static class P implements BasicUniversalConfig {
		@RestOp(path="/")
		public List<Prow> p() {
			return List.of(new Prow("Alice", 30), new Prow("Bob", 25), new Prow("Charlie", 35));
		}
		@Bean public QueryableSettings queryableSettings() {
			return QueryableSettings.create().protocol(new DataTablesQueryProtocol(Prow.class)).build();
		}
	}

	/** The bean-property index of the "name" column, as DataTables' array data source would reference it. */
	private static int nameIndex() {
		var keys = DataTablesColumns.of(Prow.class).stream().map(x -> (String) x.get("data")).toList();
		return keys.indexOf("name");
	}

	// Array data source: columns[i][data]=<index>, no name.  The numeric index maps positionally to the Nth bean
	// property, so a per-column search on the "name" column filters correctly instead of silently returning the full
	// table.
	@Test void n01_positionalArrayIndexFallback() throws Exception {
		var c = MockRestClient.buildJson(P.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]",String.valueOf(nameIndex()), "columns[0][searchable]","true", "columns[0][search][value]","Alice");
		assertTrue(body.contains("Alice") && body.contains("\"recordsFiltered\":1"), body);
		assertFalse(body.contains("Bob") || body.contains("Charlie"), body);
	}

	// Out-of-range positional index resolves to no key (skipped) - the search is ignored, all rows survive.
	@Test void n02_positionalIndexOutOfRangeIgnored() throws Exception {
		var c = MockRestClient.buildJson(P.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","9", "columns[0][searchable]","true", "columns[0][search][value]","Alice");
		assertTrue(body.contains("\"recordsFiltered\":3"), body);
	}

	// Without a row type (resource A), a numeric-data/no-name column has no resolvable key and is ignored (deterministic
	// full result), rather than erroring.
	@Test void n03_arrayIndexIgnoredWithoutRowType() throws Exception {
		var c = MockRestClient.buildJson(A.class);
		var body = get(c,
			"draw","1",
			"columns[0][data]","0", "columns[0][searchable]","true", "columns[0][search][value]","Alice");
		assertTrue(body.contains("\"recordsFiltered\":3"), body);
		assertTrue(body.contains("Alice") && body.contains("Bob") && body.contains("Charlie"), body);
	}
}
