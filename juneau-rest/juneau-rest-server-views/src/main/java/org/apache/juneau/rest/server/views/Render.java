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

import org.apache.juneau.commons.bean.*;

/**
 * A named cell-renderer reference in the {@code VIEW_META} wire contract (design doc §6.6).
 *
 * <p>
 * Serializes to the canonical object form <c>{"id":"tag","meta":{"field":"status"}}</c>.  The client
 * {@code juneau-renders.js} registry looks up {@link #id} and invokes the matching renderer, passing the
 * {@link #meta} map through as per-column context.  Timestamp renderers ({@code ts-zulu}, {@code datetime})
 * honor {@code meta.popup}: {@code off} disables the local/California hover popup; {@code ts-zulu} defaults
 * the popup on, {@code datetime} defaults it off.
 *
 * <p>
 * A compact string sugar is supported via {@link #parse(String)}: everything after the <b>first</b> colon becomes
 * <c>meta.field</c> &mdash; e.g. <c>"tag:status"</c> &rarr; <c>{id:"tag", meta:{field:"status"}}</c> and
 * <c>"date"</c> &rarr; <c>{id:"date"}</c>.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link Column}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,meta,popover")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class Render {

	/** The renderer id (registry key in {@code juneau-renders.js}). */
	public String id;

	/** Optional per-column renderer metadata.  Omitted from the wire when absent. */
	public Map<String,String> meta;

	/**
	 * Creates a renderer reference with the specified id and no metadata.
	 *
	 * @param id The renderer id.  Must not be <jk>null</jk>.
	 * @return A new {@link Render}.
	 */
	public static Render of(String id) {
		var r = new Render();
		r.id = id;
		return r;
	}

	/**
	 * Parses a render-id string into a {@link Render}, applying the <c>"id:field"</c> string sugar.
	 *
	 * <p>
	 * Everything after the <b>first</b> colon becomes <c>meta.field</c>; a bare id (no colon) yields a
	 * metadata-free renderer.
	 *
	 * @param s The render-id string.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link Render}.
	 */
	public static Render parse(String s) {
		if (s == null || s.isBlank())
			throw iaex("Render id must not be null or blank.");
		var i = s.indexOf(':');
		if (i < 0)
			return of(s);
		return of(s.substring(0, i)).meta("field", s.substring(i + 1));
	}

	/**
	 * Adds a metadata key/value pair to this renderer reference.
	 *
	 * @param key The metadata key.  Must not be <jk>null</jk>.
	 * @param value The metadata value.
	 * @return This object.
	 */
	public Render meta(String key, String value) {
		if (meta == null)
			meta = m();
		meta.put(key, value);
		return this;
	}

	/**
	 * Optional per-cell popover, omitted from the wire when unset.
	 *
	 * <p>
	 * The runtime appends a sibling trigger next to the rendered cell and paints the popover from row data
	 * already on the client.  Never assigned {@code innerHTML}.
	 */
	public CellPopover popover;

	/**
	 * Sets the optional per-cell popover.
	 *
	 * @param value The popover definition.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public Render popover(CellPopover value) {
		popover = value;
		return this;
	}

	/**
	 * Creates a {@code progress} renderer with the given maximum (denominator).
	 *
	 * <p>
	 * Equivalent to {@code Render.of("progress").meta("max", String.valueOf(max))}.  Chain
	 * {@link #meta(String,String)} for {@code label} / {@code field}.
	 *
	 * @param max The ratio denominator.  Serialized as the string {@code meta.max}.
	 * @return A new {@link Render}.
	 */
	public static Render progress(int max) {
		return of("progress").meta("max", String.valueOf(max));
	}

	/**
	 * Creates a {@code progress} renderer with maximum and optional warn/exceeds thresholds.
	 *
	 * <p>
	 * {@code warn} and {@code exceeds} are omitted from {@code meta} when <jk>null</jk>.  A boxed {@code 0} is a
	 * real threshold ({@code "0"}), not omitted.
	 *
	 * @param max The ratio denominator.
	 * @param warn Warn threshold in the same units as the cell value.  Can be <jk>null</jk>.
	 * @param exceeds Exceeds threshold in the same units as the cell value.  Can be <jk>null</jk>.
	 * @return A new {@link Render}.
	 */
	public static Render progress(int max, Integer warn, Integer exceeds) {
		var r = progress(max);
		if (warn != null)
			r.meta("warn", String.valueOf(warn));
		if (exceeds != null)
			r.meta("exceeds", String.valueOf(exceeds));
		return r;
	}

	/**
	 * Creates a {@code pill} renderer with the leading dot on and the tone inherited from the chip theme.
	 *
	 * <p>
	 * Equivalent to {@code Render.of("pill")}.  The dot color follows the {@code .tag.<field>.<value>} theme
	 * ({@code currentColor}) until an explicit {@code tone} is chosen.  Chain {@link #meta(String,String)} for
	 * {@code field}, {@code dot} ({@code on}|{@code off}) and {@code action} (a {@code rowActions} id).
	 *
	 * @return A new {@link Render}.
	 */
	public static Render pill() {
		return of("pill");
	}

	/**
	 * Creates a {@code pill} renderer with an explicit dot tone.
	 *
	 * <p>
	 * {@code tone} is one of {@code ok}|{@code warn}|{@code exceeds}|{@code neutral}.  A <jk>null</jk> or blank
	 * {@code tone} is omitted from {@code meta} (identical to {@link #pill()}); {@code neutral} is emitted verbatim
	 * and, like an absent tone, leaves the dot inheriting {@code currentColor}.  {@code info} is not a valid tone.
	 * The value is validated against {@code this.rowActions} and the tone enum on the serving path by
	 * {@code ViewDef.validate()}.
	 *
	 * @param tone The explicit dot tone.  Can be <jk>null</jk> or blank to inherit.
	 * @return A new {@link Render}.
	 */
	public static Render pill(String tone) {
		var r = pill();
		if (tone != null && ! tone.isBlank())
			r.meta("tone", tone);
		return r;
	}
}
