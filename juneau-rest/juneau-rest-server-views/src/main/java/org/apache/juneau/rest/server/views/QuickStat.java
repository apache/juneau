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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * One item in a table's quick-stats strip ({@code quickStats.items[]}): a plain tile, a meter (one value against a
 * maximum) or a segmented tile (a proportional bar of labelled counts).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>table</jv>.quickStats(<js>"work-unavailable"</js>,
 * 		QuickStat.<jsm>of</jsm>(<js>"unavailable"</js>, <js>"Work data"</js>, <js>"unavailable"</js>).tone(QuickStat.Tone.<jsf>ERROR</jsf>),
 * 		QuickStat.<jsm>meter</jsm>(<js>"inventory"</js>, <js>"Inventory"</js>, 12, 40).text(<js>"12 of 40"</js>).statusTone(StatusTone.<jsf>SUCCESS</jsf>),
 * 		QuickStat.<jsm>segmented</jsm>(<js>"orders"</js>, <js>"Orders"</js>).segment(<js>"Placed"</js>, 3).segment(<js>"Delivered"</js>, 9, StatusTone.<jsf>SUCCESS</jsf>));
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Repeated literals are contract wire keys; named constants would obscure the contract shape.
})
public final class QuickStat {

	/** The tile's visual tone. */
	public enum Tone {

		/** No particular emphasis. */
		NEUTRAL,

		/** A positive/healthy value. */
		SUCCESS,

		/** An informational value. */
		INFO,

		/** A value that deserves attention. */
		WARNING,

		/** A value that signals a problem. */
		ERROR;

		String wire() {
			return name().toLowerCase(Locale.ROOT);
		}
	}

	/** The most segments one segmented item may carry (one per chart palette colour). */
	public static final int MAX_SEGMENTS = 8;

	private enum Kind { TILE, METER, SEGMENTED }

	private record Segment(String label, long count, String tone) {}

	private final String id;
	private final String label;
	private final Object value;
	private final Kind kind;
	private Number max;
	private String text;
	private String tone;  // wire token, or null when unset
	private final List<Segment> segments = new ArrayList<>();

	private QuickStat(String id, String label, Object value, Kind kind) {
		this.id = id;
		this.label = label;
		this.value = value;
		this.kind = kind;
	}

	private static String checkId(String id) {
		if (ib(id))
			throw iaex("QuickStat id must not be null or blank.");
		return id;
	}

	private IllegalArgumentException fail(String reason) {
		return iaex("QuickStat '%s': %s", id, reason);
	}

	private void requireKind(Kind k, String method) {
		if (kind != k)
			throw fail(method + "(...) is only valid on a " + k.name().toLowerCase(Locale.ROOT) + " item.");
	}

	/**
	 * Creates a quick-stat tile.
	 *
	 * @param id This tile's own id, unique within its table.  Must not be <jk>null</jk> or blank.
	 * @param label The tile's label text.
	 * @param value The tile's value, serialized as-is.
	 * @return A new {@link QuickStat}.
	 * @throws IllegalArgumentException If {@code id} is <jk>null</jk> or blank.
	 */
	public static QuickStat of(String id, String label, Object value) {
		return new QuickStat(checkId(id), label, value, Kind.TILE);
	}

	/**
	 * Creates a meter item: one value drawn against a maximum.
	 *
	 * <p>
	 * A value above the maximum is allowed; it paints a full bar with an over-limit cue.
	 *
	 * @param id This item's own id, unique within its table.  Must not be <jk>null</jk> or blank.
	 * @param label The item's label text; it names the meter for assistive technology.  Must not be <jk>null</jk> or blank.
	 * @param value The current value.  Must be finite and {@code >= 0}.
	 * @param max The maximum.  Must be finite and {@code > 0}.
	 * @return A new {@link QuickStat}.
	 * @throws IllegalArgumentException If {@code id} or {@code label} is blank, or {@code value} or {@code max} is <jk>null</jk> or out of range.
	 */
	public static QuickStat meter(String id, String label, Number value, Number max) {
		var q = new QuickStat(checkId(id), label, value, Kind.METER);
		q.checkLabel();
		if (value == null || max == null)
			throw q.fail("meter value and max must not be null.");
		var v = value.doubleValue();
		var mx = max.doubleValue();
		if (! Double.isFinite(v) || v < 0)
			throw q.fail("meter value must be a finite number >= 0.");
		if (! Double.isFinite(mx) || mx <= 0)
			throw q.fail("meter max must be a finite number > 0.");
		q.max = max;
		return q;
	}

	/**
	 * Creates a segmented item: a proportional bar plus a legend of labelled counts.
	 *
	 * <p>
	 * Add the segments with {@link #segment(String, long)}; at least one is required by the time the item is built.
	 *
	 * @param id This item's own id, unique within its table.  Must not be <jk>null</jk> or blank.
	 * @param label The item's label text; it names the group for assistive technology.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link QuickStat}.
	 * @throws IllegalArgumentException If {@code id} or {@code label} is <jk>null</jk> or blank.
	 */
	public static QuickStat segmented(String id, String label) {
		var q = new QuickStat(checkId(id), label, null, Kind.SEGMENTED);
		q.checkLabel();
		return q;
	}

	private void checkLabel() {
		if (ib(label))
			throw fail("label must not be null or blank.");
	}

	/**
	 * Adds an untoned segment to a segmented item.
	 *
	 * @param label The segment label.  Must not be <jk>null</jk> or blank.
	 * @param count The segment count.  Must be {@code >= 0}.
	 * @return This object.
	 * @throws IllegalArgumentException If this is not a segmented item, the label is blank, the count is negative,
	 * 	or the item already has {@link #MAX_SEGMENTS} segments.
	 */
	public QuickStat segment(String label, long count) {
		return segment(label, count, null);
	}

	/**
	 * Adds a segment to a segmented item.
	 *
	 * @param label The segment label.  Must not be <jk>null</jk> or blank.
	 * @param count The segment count.  Must be {@code >= 0}.
	 * @param tone The segment tone.  Can be <jk>null</jk> for an untoned segment.
	 * @return This object.
	 * @throws IllegalArgumentException If this is not a segmented item, the label is blank, the count is negative,
	 * 	or the item already has {@link #MAX_SEGMENTS} segments.
	 */
	public QuickStat segment(String label, long count, StatusTone tone) {
		requireKind(Kind.SEGMENTED, "segment");
		if (ib(label))
			throw fail("segment label must not be null or blank.");
		if (count < 0)
			throw fail("segment count must be >= 0.");
		if (segments.size() == MAX_SEGMENTS)
			throw fail("a segmented item takes at most " + MAX_SEGMENTS + " segments.");
		segments.add(new Segment(label, count, tone == null ? null : tone.wire()));
		return this;
	}

	/**
	 * Sets a meter's display text, which replaces the default "value / max".
	 *
	 * @param text The text.  Must not be <jk>null</jk>; blank means the default text is used.
	 * @return This object.
	 * @throws IllegalArgumentException If this is not a meter item, or {@code text} is <jk>null</jk>.
	 */
	public QuickStat text(String text) {
		requireKind(Kind.METER, "text");
		if (text == null)
			throw fail("text must not be null.");
		this.text = text;
		return this;
	}

	/**
	 * Sets the tone of a tile or meter.
	 *
	 * <p>
	 * Writes the same tone as {@link #tone(Tone)}; the last call wins.
	 *
	 * @param t The tone.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 * @throws IllegalArgumentException If this is a segmented item (tone each segment instead).
	 */
	public QuickStat statusTone(StatusTone t) {
		if (kind == Kind.SEGMENTED)
			throw fail("statusTone(...) is not valid on a segmented item; tone each segment instead.");
		tone = t == null ? null : t.wire();
		return this;
	}

	/**
	 * Sets this tile's visual tone.
	 *
	 * @param t The tone.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 * @throws IllegalArgumentException If this is a segmented item (tone each segment instead).
	 */
	public QuickStat tone(Tone t) {
		if (kind == Kind.SEGMENTED)
			throw fail("tone(...) is not valid on a segmented item; tone each segment instead.");
		tone = t == null ? null : t.wire();
		return this;
	}

	/**
	 * Builds this item's {@code quickStats.items[]} entry.
	 *
	 * @return A tile {@code {id, label, value, tone?}} (no {@code type}); a meter
	 * 	{@code {id, type, label, value, max, text?, tone?}}; or a segmented item
	 * 	{@code {id, type, label, segments[{label, count, tone?}]}}.
	 * @throws IllegalArgumentException If this is a segmented item with no segments.
	 */
	public JsonMap toMap() {
		var m = new JsonMap();
		m.put("id", id);
		if (kind == Kind.TILE) {
			m.put("label", label);
			m.put("value", value);
			if (tone != null)
				m.put("tone", tone);
			return m;
		}
		m.put("type", kind == Kind.METER ? "meter" : "segmented");
		m.put("label", label);
		if (kind == Kind.METER) {
			m.put("value", value);
			m.put("max", max);
			if (inb(text))
				m.put("text", text);
			if (tone != null)
				m.put("tone", tone);
			return m;
		}
		if (segments.isEmpty())
			throw fail("a segmented item needs at least one segment.");
		var list = new ArrayList<JsonMap>(segments.size());
		for (var s : segments) {
			var sm = new JsonMap();
			sm.put("label", s.label());
			sm.put("count", s.count());
			if (s.tone() != null)
				sm.put("tone", s.tone());
			list.add(sm);
		}
		m.put("segments", list);
		return m;
	}
}
