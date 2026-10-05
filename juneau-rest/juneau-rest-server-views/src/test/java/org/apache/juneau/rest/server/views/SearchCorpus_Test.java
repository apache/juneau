/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.*;

/**
 * Runs the shared {@code search-corpus.json} parity corpus through the server-side in-memory engine
 * ({@link InMemoryBeanQueryContext}, {@link BeanQuery}, {@link InMemoryMatch} indirectly via
 * {@code BeanQuerySession.find}). {@code ColumnSearch_Parity_Test} runs the SAME corpus through
 * {@code juneau-search.js} inside a Node sandbox; a divergence between the two names the exact case that drifted.
 */
@SuppressWarnings({
	"unchecked" // Corpus cases are untyped parsed JSON; tests cast the matches list to List<Number>
})
class SearchCorpus_Test extends TestBase {

	private static final SearchOperator STARTS_CI =
		SearchOperator.create("$startsCI", "Case-insensitive starts-with. Example: $startsCI(HE)")
			.types(SearchType.TEXT).minArgs(1).maxArgs(1)
			.predicate((cell, args) -> cell != null
				&& String.valueOf(cell).toLowerCase(Locale.ROOT).startsWith(args.get(0).toLowerCase(Locale.ROOT)))
			.build();

	@Test void everyCorpusCaseMatchesExpectedBehavior() {
		var cases = loadCorpus();
		assertAll(cases.stream().map(kase -> (Executable) () -> runCase(kase)));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Per-case execution.
	// -----------------------------------------------------------------------------------------------------------------

	private static void runCase(Map<?,?> kase) {
		var id = (String) kase.get("id");
		var ctx = contextFor(kase);
		var query = new BeanQuery().setSearch((String) kase.get("search"));
		var error = (Map<?,?>) kase.get("error");
		if (error != null) {
			var ex = assertThrows(BeanQuerySyntaxException.class, () -> ctx.resolve(query), id);
			assertEquals(BeanQuerySyntaxException.Code.valueOf((String) error.get("code")), ex.code(), id);
			return;
		}
		// "note" is informational only - mirrors column-search.cjs, which also does not special-case it.  A malformed
		// typed ARGUMENT is a BAD_VALUE error case (see the "error" branch above); only unparseable CELLS quietly
		// fail to match.
		ResolvedQuery resolved;
		try {
			resolved = ctx.resolve(query);
		} catch (BeanQuerySyntaxException e) {
			throw new AssertionError(id + ": unexpected " + e.code() + ": " + e.getMessage(), e);
		}
		if (kase.get("tree") != null)
			assertEquals(kase.get("tree"), CorpusTrees.of(resolved.filter()), id);
		if (kase.get("rows") != null) {
			var rows = rowsOf(kase);
			var indices = (List<Number>) kase.get("matches");
			var expected = new ArrayList<Map<String,Object>>();
			for (var i : indices)
				expected.add(rows.get(i.intValue()));
			try (var session = ctx.getSession(rows)) {
				assertEquals(expected, session.find(query).rows(), id);
			}
		}
	}

	private static InMemoryBeanQueryContext<Map<String,Object>> contextFor(Map<?,?> kase) {
		var types = (Map<?,?>) kase.get("types");
		var builder = InMemoryBeanQueryContext.<Map<String,Object>>create().allowRegex(true);
		for (var e : types.entrySet())
			builder.column((String) e.getKey(), SearchType.valueOf((String) e.getValue()));
		if (hasCustom(kase, "$startsCI"))
			builder.operators(SearchOperatorSet.standard().with(STARTS_CI));
		return builder.build();
	}

	private static boolean hasCustom(Map<?,?> kase, String opName) {
		if (!(kase.get("operators") instanceof Map<?,?> m))
			return false;
		var custom = (List<?>) m.get("custom");
		return custom != null && custom.contains(opName);
	}

	/** Builds row maps: multi-column cases already carry object rows; single-column cases wrap scalars as {@code {v: value}}. */
	private static List<Map<String,Object>> rowsOf(Map<?,?> kase) {
		var raw = (List<?>) kase.get("rows");
		var out = new ArrayList<Map<String,Object>>(raw.size());
		for (var r : raw) {
			if (r instanceof Map<?,?> m) {
				out.add((Map<String,Object>) m);
			} else {
				var row = new LinkedHashMap<String,Object>();
				row.put("v", r);
				out.add(row);
			}
		}
		return out;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Corpus loading.
	// -----------------------------------------------------------------------------------------------------------------

	private static List<Map<?,?>> loadCorpus() {
		try (var in = SearchCorpus_Test.class.getResourceAsStream("search-corpus.json")) {
			assertNotNull(in, "search-corpus.json");
			var json = new String(in.readAllBytes(), UTF_8);
			return (List<Map<?,?>>) (List<?>) Json.to(json, List.class);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
