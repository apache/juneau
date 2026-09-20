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
 * Parser for the {@code <@card type="json">} envelope (and the {@code datatables} / {@code calendar}
 * sugars that desugar into it).
 *
 * <p>
 * The card body is authored as <b>JSON5</b> &mdash; comments, unquoted keys, trailing commas, and
 * single quotes are all first-class &mdash; and parsed with the Juneau {@link Json5Parser#DEFAULT}
 * (<b>not</b> a line-delimited multi-record JSON5 stream reader; a card body is exactly one object). The
 * emitted client sidecar, by contrast, is strict RFC JSON written with
 * {@code org.apache.juneau.marshall.marshaller.Json.DEFAULT.write(...)}.
 *
 * @since 10.0.0
 */
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
		Set.of("defaultOrder", "ribbon", "dataMode", "rowType", "pollIntervalMs");

	/**
	 * Lifts a {@code type="datatables"} author catalog into the frozen SLOT_META envelope that
	 * {@code JuneauViews.init.mountTableSlot} handshakes (F2).
	 *
	 * <p>
	 * The author writes IRS-portable catalog JSON5 (a bare {@code {dataUrl, columns:[{key,label}]}}),
	 * <b>not</b> hand-authored VIEW_META. This method wraps it in a {@link ViewSlot#CONTRACT_VERSION}
	 * slot carrying a {@link ViewDef#CONTRACT_VERSION} view: author {@code key}/{@code label} become
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
		view.put("contractVersion", ViewDef.CONTRACT_VERSION);
		view.put("id", cardId);
		view.put("dataUrl", dataUrl);
		var cols = new JsonList();
		for (var raw : columns) {
			if (! (raw instanceof Map<?, ?> c))
				throw new IllegalArgumentException("<@card type=\"datatables\"> each column must be an object.");
			var col = new JsonMap();
			col.put("data", firstNonNull(c.get("data"), c.get("key")));
			col.put("title", firstNonNull(c.get("title"), c.get("label")));
			cols.add(col);
		}
		view.put("columns", cols);
		for (var f : VIEW_META_PASSTHROUGH)
			if (catalog.containsKey(f))
				view.put(f, catalog.get(f));

		var slot = new JsonMap();
		slot.put("contractVersion", ViewSlot.CONTRACT_VERSION);
		slot.put("layout", "wide");
		slot.put("view", view);
		return slot;
	}

	private static Object firstNonNull(Object a, Object b) {
		return a != null ? a : b;
	}
}
