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
import java.util.logging.*;

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

	private static final Logger LOG = Logger.getLogger(CardEnvelope.class.getName());

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

	/**
	 * Author-catalog VIEW_META fields copied verbatim onto the lifted {@code view} when present.
	 *
	 * <p>
	 * Every view-level option {@code juneau-views.js} / {@code juneau-urlstate.js} reads off the view def must be listed
	 * here, otherwise it is silently dropped on FTL-built pages: {@code defaultOrder}, {@code ribbon}, {@code dataMode},
	 * {@code rowType}, {@code pollIntervalMs}, {@code columnConfig}, {@code cleanAddress}, {@code primary},
	 * {@code copyLink} (set {@code false} to suppress the View Settings Copy-link button), {@code rowActions} (row-action
	 * catalog), {@code rowClassRules}, and {@code pausePollingWhileEditing}.
	 */
	private static final Set<String> VIEW_META_PASSTHROUGH =
		Set.of(
			"defaultOrder", "ribbon", "dataMode", "rowType", "pollIntervalMs", "columnConfig",
			"cleanAddress", "primary", "copyLink", "rowActions", "rowClassRules", "pausePollingWhileEditing"
		);

	/**
	 * Author-catalog SLOT_META (envelope-level, sibling of {@code view}) fields copied verbatim onto the lifted slot when
	 * present: {@code selection} ({@code {rowIdField, selectAll}}), {@code bulk} (the bulk-actions contract object),
	 * {@code detail} (the row-detail contract object, carrying its own {@code contractVersion}), {@code quickStats}
	 * (the QuickStats strip, carrying its own {@code contractVersion}) and {@code savedViewsBase} (the saved-views
	 * endpoint base URL).  {@code rows} is lifted separately by {@link #liftTable}, because it is validated against
	 * {@code dataUrl}.  Nested {@code contractVersion}s are copied verbatim, never injected: the browser runtime
	 * withholds a mismatching {@code detail}/{@code quickStats} and logs it.
	 */
	private static final Set<String> SLOT_META_PASSTHROUGH =
		Set.of("selection", "bulk", "detail", "quickStats", "savedViewsBase");

	/** Inline {@code rows} count above which a {@code WARNING} steers the author to {@code dataUrl}. */
	private static final int ROWS_WARN_THRESHOLD = 1_000;

	/**
	 * Lifts a {@code type="datatables"} author catalog into the frozen SLOT_META envelope that
	 * {@code JuneauViews.init.mountTableSlot} handshakes (F2).
	 *
	 * <p>
	 * The author writes IRS-portable catalog JSON5 (a bare {@code {dataUrl, columns:[{key,label}]}}),
	 * <b>not</b> hand-authored VIEW_META. This method wraps it in a {@link ViewsMixin#SLOT_CONTRACT_VERSION}
	 * slot carrying a {@link ViewsMixin#CONTRACT_VERSION} view: author {@code key}/{@code label} become
	 * VIEW_META {@code data}/{@code title} ({@code data}/{@code title} are also accepted verbatim). The view-level
	 * options in {@link #VIEW_META_PASSTHROUGH} land on {@code view}; {@code selection} and {@code bulk} land on the slot
	 * itself ({@link #SLOT_META_PASSTHROUGH}).
	 *
	 * <p>
	 * The table's data comes from <b>exactly one</b> of {@code dataUrl} (fetched by the browser) or {@code rows} (an
	 * inline array of row objects; a static table with no fetch, intended for small, fixed data sets &mdash; steer
	 * large or changing data to {@code dataUrl}).  {@code rows: []} is a valid empty table, {@code rows: null} and a
	 * blank {@code dataUrl} count as absent.  An inline-{@code rows} table is client-side only: {@code dataMode:'server'}
	 * and {@code pollIntervalMs} are rejected, since there is nothing to fetch or poll.  When emitting {@code rows} from
	 * an FTL template use {@code ?json_string}/{@code ?c} rather than a raw {@code ${}}, and note the browser renders
	 * the cells as escaped text.  The validation runs only on this lift path; a pre-built envelope is
	 * not re-validated. An
	 * object that already carries both {@code contractVersion} and {@code view} is treated as a
	 * pre-built SLOT_META envelope and passed through unchanged (escape hatch).
	 *
	 * @param cardId The author card {@code id=}; becomes the view id.
	 * @param catalog The parsed author catalog.
	 * @return The lifted SLOT_META envelope.
	 * @throws IllegalArgumentException if the catalog is missing {@code columns}, does not carry exactly one of
	 * 	{@code dataUrl} or {@code rows}, has a {@code rows} value that is not an array of objects, or combines
	 * 	{@code rows} with {@code dataMode:'server'} or {@code pollIntervalMs}.
	 */
	static JsonMap liftTable(String cardId, JsonMap catalog) {
		return liftTable(cardId, catalog, null, false);
	}

	/**
	 * Same as {@link #liftTable(String, JsonMap)}, additionally reconciling the {@code view.contractVersion} of a
	 * pre-built SLOT_META envelope against {@link ViewsMixin#CONTRACT_VERSION}.
	 *
	 * <p>
	 * An omitted {@code view.contractVersion} (and slot-level {@code contractVersion}) is injected. A stated version
	 * that differs from the runtime's logs a {@code WARNING} naming the template, card and both versions, and in
	 * {@code devMode} throws so the mismatch fails the render instead of surfacing as an opaque browser-side refusal.
	 *
	 * @param cardId The author card {@code id=}.
	 * @param catalog The parsed author catalog.
	 * @param template The authoring template name, for diagnostics; may be {@code null}.
	 * @param devMode Whether a version mismatch fails the render.
	 * @return The lifted SLOT_META envelope.
	 * @throws IllegalArgumentException if the catalog is malformed (see {@link #liftTable(String, JsonMap)}), or on a
	 * 	version mismatch in dev mode.
	 */
	static JsonMap liftTable(String cardId, JsonMap catalog, String template, boolean devMode) {
		if (catalog.get("view") instanceof Map<?,?> prebuilt) {
			reconcileViewVersion(cardId, catalog, prebuilt, template, devMode);
			return catalog;
		}

		var dataUrl = catalog.getString("dataUrl");
		if (dataUrl != null && dataUrl.isBlank())
			dataUrl = null;
		var columns = catalog.getList("columns");
		if (columns == null)
			throw new IllegalArgumentException(
				"<@card type=\"datatables\"> catalog requires 'columns'.");
		var rows = catalog.get("rows");
		if ((dataUrl == null) == (rows == null))
			throw new IllegalArgumentException(
				"<@card type=\"datatables\"> catalog requires exactly one of 'dataUrl' or 'rows'.");
		if (rows != null)
			validateRows(rows, catalog);

		var view = new JsonMap();
		view.put("contractVersion", ViewsMixin.CONTRACT_VERSION);
		view.put("id", cardId);
		if (dataUrl != null)
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
		for (var f : SLOT_META_PASSTHROUGH)
			if (catalog.containsKey(f))
				slot.put(f, catalog.get(f));
		if (rows != null)
			slot.put("rows", rows);
		return slot;
	}

	private static void validateRows(Object rows, JsonMap catalog) {
		if (! (rows instanceof List<?> list))
			throw new IllegalArgumentException("<@card type=\"datatables\"> 'rows' must be an array of objects.");
		for (var r : list)
			if (! (r instanceof Map<?, ?>))
				throw new IllegalArgumentException("<@card type=\"datatables\"> each 'rows' element must be an object.");
		// Q:  Use Shorts here and elsewhere in this module.
		if ("server".equals(catalog.getString("dataMode")))
			throw new IllegalArgumentException(
				"<@card type=\"datatables\"> inline 'rows' cannot be combined with dataMode:'server'; use 'dataUrl'.");
		if (catalog.get("pollIntervalMs") != null)
			throw new IllegalArgumentException(
				"<@card type=\"datatables\"> inline 'rows' cannot be combined with 'pollIntervalMs' (nothing to poll); use 'dataUrl'.");
		if (list.size() > ROWS_WARN_THRESHOLD)
			LOG.log(Level.WARNING, "<@card type=\"datatables\"> inline 'rows' has {0} entries; prefer 'dataUrl' for large data sets.", list.size());
	}

	@SuppressWarnings({
		"unchecked" // The view of a pre-built envelope is a parsed JsonMap; it is mutated in place to inject the version.
	})
	private static void reconcileViewVersion(String cardId, JsonMap slot, Map<?,?> view, String template, boolean devMode) {
		if (! slot.containsKey("contractVersion"))
			slot.put("contractVersion", ViewsMixin.SLOT_CONTRACT_VERSION);
		var stated = view.get("contractVersion");
		if (stated == null) {
			((Map<String,Object>)view).put("contractVersion", ViewsMixin.CONTRACT_VERSION);
			return;
		}
		if (ViewsMixin.CONTRACT_VERSION.equals(String.valueOf(stated)))
			return;
		var msg = String.format(
			"Template '%s' card '%s': view.contractVersion is '%s' but the runtime ViewsMixin.CONTRACT_VERSION is '%s'. "
			+ "Omit view.contractVersion to track the runtime automatically.",
			template, cardId, stated, ViewsMixin.CONTRACT_VERSION);
		LOG.log(Level.WARNING, msg);
		if (devMode)
			throw new IllegalArgumentException(msg);
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
