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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Java layer of the ribbon parity corpus (C5 §6.4): for every case in {@code ribbon-corpus.json}, the DataTables
 * request whose {@code columns[i].search.value} carries the merged searches yields {@code expect.search} and, run over
 * the corpus rows, {@code expect.rowIds}.  The JS layer is {@link RibbonCorpus_Parity_Test}.
 */
class RibbonCorpus_Test extends TestBase {

	private static RibbonCorpus corpus;

	static RibbonCorpus corpus() throws Exception {
		if (corpus == null) {
			try (var in = RibbonCorpus_Test.class.getResourceAsStream("ribbon-corpus.json")) {
				assertNotNull(in, "ribbon-corpus.json is not on the test classpath");
				corpus = Json.to(new String(in.readAllBytes(), UTF_8), RibbonCorpus.class);
			}
		}
		return corpus;
	}

	static Stream<Arguments> cases() throws Exception {
		return corpus().cases.stream().map(c -> Arguments.of(c.name, c));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cases")
	void a01_javaLayer(String name, RibbonCorpus.Case c) throws Exception {
		var cols = c.columns != null ? c.columns : corpus().columns;
		var request = new DataTablesRequest().setLength(-1).setColumns(requestColumns(cols, merged(c)));

		assertEquals(c.expect.search, searchOf(request), name + ": BeanQuery search string");

		if (c.expect.rowIds == null)
			return;
		try (var session = context().getSession(corpus().rows)) {
			var ids = DataTablesQuery.run(request, session).getData().stream().map(CorpusRow::getId).toList();
			assertEquals(c.expect.rowIds, ids, name + ": matching row ids");
		}
	}

	@org.junit.jupiter.api.Test
	void a02_corpusHasTheRequiredStarterCases() throws Exception {
		var names = corpus().cases.stream().map(x -> x.name).collect(Collectors.toSet());
		for (var required : List.of("single-option-on", "single-option-off", "option-group-member", "option-group-none",
				"two-options-different-columns", "two-options-same-column", "two-options-same-param",
				"combine-user-and-ribbon", "user-and-ribbon-same-column", "user-only", "column-hidden-dtindex",
				"quoted-comma-value", "param-option", "jrm-dropped-only",
				"foundry-daemon-stream", "foundry-review-phase", "foundry-work-flavor", "foundry-review-detail-isnew",
				"option-default-applied", "group-default-applied", "default-overridden-by-persisted"))
			assertTrue(names.contains(required), "ribbon-corpus.json is missing case '" + required + "'");
	}

	private static InMemoryBeanQueryContext<CorpusRow> context() {
		return InMemoryBeanQueryContext.create(CorpusRow.class).build();
	}

	/** expect.merged, else the plain union of userSearch and columnSearches (an overlap must give merged explicitly). */
	private static Map<String,String> merged(RibbonCorpus.Case c) {
		if (c.expect.merged != null)
			return c.expect.merged;
		var out = new LinkedHashMap<String,String>();
		if (c.userSearch != null)
			out.putAll(c.userSearch);
		if (c.expect.columnSearches != null) {
			c.expect.columnSearches.forEach((k, v) -> {
				assertFalse(out.containsKey(k), c.name + ": user and ribbon both search column " + k + "; give expect.merged explicitly");
				out.put(k, v);
			});
		}
		return out;
	}

	private static List<DataTablesRequest.Column> requestColumns(List<RibbonCorpus.CorpusColumn> cols, Map<String,String> merged) {
		var out = new ArrayList<DataTablesRequest.Column>();
		for (var i = 0; i < cols.size(); i++) {
			var cc = cols.get(i);
			var col = new DataTablesRequest.Column().setData(cc.data).setName(cc.name).setSearchable(cc.searchable).setOrderable(false);
			var v = merged.get(String.valueOf(i));
			if (v != null)
				col.setSearch(new DataTablesRequest.Search().setValue(v));
			out.add(col);
		}
		return out;
	}

	private static String searchOf(DataTablesRequest request) {
		return DataTablesQuery.toBeanQuery(request, context()).getSearch();
	}
}
