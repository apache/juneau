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

import java.util.*;

/**
 * Test-only bean for {@code ribbon-corpus.json}: the shared ribbon parity contract read by {@link RibbonCorpus_Test}
 * (Java layer) and {@code ribbon-corpus.cjs} (JS layer).  Not public API.
 */
public class RibbonCorpus {

	/** Corpus schema version. */
	public int version;

	/** Rows shared by every case. */
	public List<CorpusRow> rows;

	/** Default DataTables column array (a case may override it). */
	public List<CorpusColumn> columns;

	/** The cases. */
	public List<Case> cases;

	/** One DataTables column as the corpus declares it. */
	public static class CorpusColumn {
		/** Row property, or null for a synthetic column. */
		public String data;
		/** Column name. */
		public String name;
		/** Whether the column takes a search. */
		public boolean searchable = true;
	}

	/** One corpus case. */
	public static class Case {
		/** Stable case name. */
		public String name;
		/** Optional per-case column override. */
		public List<CorpusColumn> columns;
		/** viewDef.ribbon as the sidecar carries it (untyped; only JS interprets it). */
		public List<Map<String,Object>> ribbon;
		/** Active state (absent for the default cases). */
		public Map<String,Object> state;
		/** Stored state for the default cases. */
		public Map<String,String> persisted;
		/** dtIndex to user-typed $-expression. */
		public Map<String,String> userSearch;
		/** Expected outputs. */
		public Expect expect;

		@Override /* Object */
		public String toString() { return name; }
	}

	/** Expected outputs of one case. */
	public static class Expect {
		/** JS: derived state (default cases only). */
		public Map<String,Object> state;
		/** JS: ribbonColumnSearches output. */
		public Map<String,String> columnSearches;
		/** JS: ribbonQueryParams output. */
		public Map<String,String> queryParams;
		/** JS and Java: per-column values after the merge (optional). */
		public Map<String,String> merged;
		/** Java: BeanQuery.getSearch(); null means no search. */
		public String search;
		/** Java and JS: matching row ids in input order; null skips the rows layer. */
		public List<Integer> rowIds;
		/** JS: substring a console.error must contain (optional). */
		public String consoleError;
	}
}
