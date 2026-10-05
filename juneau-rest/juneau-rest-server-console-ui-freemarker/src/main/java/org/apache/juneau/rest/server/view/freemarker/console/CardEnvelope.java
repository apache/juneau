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
package org.apache.juneau.rest.server.view.freemarker.console;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.server.views.*;

/**
 * Parser and lift for a {@code <@card type="datatables">} JSON5 catalog body.
 *
 * <p>
 * The card body is authored as <b>JSON5</b> &mdash; comments, unquoted keys, trailing commas, and
 * single quotes are all first-class &mdash; and parsed with the Juneau {@link Json5Parser#DEFAULT}
 * (<b>not</b> a line-delimited multi-record JSON5 stream reader; a card body is exactly one object).
 * {@link #liftTable} then lifts that catalog into the frozen SLOT_META envelope the views runtime
 * handshakes.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Duplicated literals read more clearly inline than as constants
})
final class CardEnvelope {

	private CardEnvelope() {}

	/**
	 * Parses a JSON5 card body into a {@link JsonMap}.
	 *
	 * @param body The JSON5 card body. Must not be {@code null}.
	 * @return The parsed envelope.
	 * @throws IllegalArgumentException if {@code body} is not a single valid JSON5 object.
	 */
	static JsonMap parse(String body) {
		try {
			return Json5Parser.DEFAULT.read(body, JsonMap.class);
		} catch (ParseException ex) {
			throw new IllegalArgumentException("Card JSON5 is invalid: " + ex.getMessage(), ex);
		}
	}

	/** Author-catalog VIEW_META fields copied verbatim onto the lifted {@code view} when present. */
	private static final Set<String> VIEW_META_PASSTHROUGH =
		Set.of(
			"defaultOrder", "ribbon", "dataMode", "rowType", "pollIntervalMs", "columnConfig",
			"cleanAddress", "primary"
		);

	/**
	 * Lifts a {@code type="datatables"} author catalog into the frozen SLOT_META envelope that
	 * {@code JuneauViews.init.mountTableSlot} handshakes (F2).
	 *
	 * <p>
	 * The author writes IRS-portable catalog JSON5 (a bare {@code {dataUrl, columns:[{key,label}]}}),
	 * <b>not</b> hand-authored VIEW_META. This method wraps it in a {@link ViewsMixin#SLOT_CONTRACT_VERSION}
	 * slot carrying a {@link ViewsMixin#CONTRACT_VERSION} view: author {@code key}/{@code label} become
	 * VIEW_META {@code data}/{@code title} ({@code data}/{@code title} are also accepted verbatim). An
	 * object that already carries both {@code contractVersion} and {@code view} is treated as a
	 * pre-built SLOT_META envelope and passed through unchanged (escape hatch).
	 *
	 * @param cardId The author card {@code id=}; becomes the view id.
	 * @param catalog The parsed author catalog.
	 * @return The lifted SLOT_META envelope.
	 * @throws IllegalArgumentException if the catalog is missing {@code dataUrl} or {@code columns}.
	 */
	static JsonMap liftTable(String cardId, JsonMap catalog) {
		if (catalog.containsKey("contractVersion") && catalog.containsKey("view"))
			return catalog;

		var dataUrl = catalog.getString("dataUrl");
		var columns = catalog.getList("columns");
		if (dataUrl == null || columns == null)
			throw new IllegalArgumentException(
				"<@card type=\"datatables\"> catalog requires 'dataUrl' and 'columns'.");

		var view = new JsonMap();
		view.put("contractVersion", ViewsMixin.CONTRACT_VERSION);
		view.put("id", cardId);
		view.put("dataUrl", dataUrl);
		var cols = new JsonList();
		for (var raw : columns) {
			if (! (raw instanceof Map<?, ?> c))
				throw new IllegalArgumentException("<@card type=\"datatables\"> each column must be an object.");
			var col = new JsonMap();
			var data = firstNonNull(c.get("data"), c.get("key"));
			col.put("data", data);
			col.put("title", firstNonNull(c.get("title"), c.get("label")));
			addSearchMeta(col, str(data), c);
			cols.add(col);
		}
		view.put("columns", cols);
		for (var f : VIEW_META_PASSTHROUGH)
			if (catalog.containsKey(f))
				view.put(f, catalog.get(f));

		var slot = new JsonMap();
		slot.put("contractVersion", ViewsMixin.SLOT_CONTRACT_VERSION);
		slot.put("layout", "wide");
		slot.put("view", view);
		return slot;
	}

	/**
	 * Emits the per-column {@code search} block (design §4.3&ndash;§4.5) when the author gave the column a
	 * {@code searchType}: its wire search-type token plus the gated effective operators, each carrying the help
	 * text the header popup renders. A column with no {@code searchType} is left non-searchable (no {@code search}
	 * key).
	 *
	 * <p>
	 * Author catalog input per column: a {@code searchType} wire token, an optional {@code searchOperators}
	 * allow-list array (design §4.3), and optional {@code customOperators:[{name,help}]} (design §4.4).  The
	 * operator-gating and the wire shape are owned by {@link Column#searchMeta(String, String, List, List)} in the
	 * views layer; this chrome-only module never touches the search engine directly.
	 *
	 * @param col The lifted VIEW_META column to add the {@code search} block to.
	 * @param columnName The row-data key this column reads (its {@link Column} name).
	 * @param c The author catalog column.
	 */
	private static void addSearchMeta(JsonMap col, String columnName, Map<?, ?> c) {
		var searchType = str(c.get("searchType"));
		if (searchType == null || searchType.isBlank())
			return;  // Non-searchable column: no search block (design §4.3).
		List<String> allow = null;
		if (c.get("searchOperators") instanceof List<?> a) {
			allow = new ArrayList<>();
			for (var n : a)
				allow.add(str(n));
		}
		var customs = new ArrayList<Map<String,String>>();
		if (c.get("customOperators") instanceof List<?> cs)
			for (var raw : cs) {
				if (! (raw instanceof Map<?, ?> cm))
					throw new IllegalArgumentException(
						"<@card type=\"datatables\"> each customOperator must be an object.");
				var nameHelp = new LinkedHashMap<String,String>();
				nameHelp.put("name", str(cm.get("name")));
				nameHelp.put("help", str(cm.get("help")));
				customs.add(nameHelp);
			}
		var search = Column.searchMeta(columnName, searchType, allow, customs);
		if (search != null)
			col.put("search", search);
	}

	private static Object firstNonNull(Object a, Object b) {
		return a != null ? a : b;
	}

	private static String str(Object o) {
		return o == null ? null : o.toString();
	}
}
