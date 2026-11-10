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
package org.apache.juneau.console.views;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.console.widgets.*;

/**
 * A named cell-renderer reference in the {@code VIEW_META} wire contract.
 *
 * <p>
 * Serializes to the canonical object form <c>{"id":"tag","meta":{"field":"status"}}</c>; the optional
 * {@link #meta} and {@link #popover} members are omitted from the wire when unset.  The client
 * {@code juneau-renders.js} registry looks up {@link #id} and invokes the matching renderer, passing the
 * {@link #meta} map through as per-column context (and painting {@link #popover}, when present, from row data
 * already on the client).  Typed factories exist for the renderers that take structured metadata:
 * {@link #progress(int)} and {@link #pill(String)}.  Timestamp renderers ({@code ts-zulu}, {@code datetime})
 * honor {@code meta.popup}: {@code off} disables the local/California hover popup; {@code ts-zulu} defaults
 * the popup on, {@code datetime} defaults it off.
 *
 * <p>
 * A compact string sugar is supported via {@link #parse(String)}: everything after the <b>first</b> colon becomes
 * <c>meta.field</c> &mdash; e.g. <c>"tag:status"</c> &rarr; <c>{id:"tag", meta:{field:"status"}}</c> and
 * <c>"date"</c> &rarr; <c>{id:"date"}</c>.
 *
 * <h2 class='section'>See Also:</h2>
 * <ul>
 * 	<li class='jc'>{@link ViewTable}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="id,meta,popover")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class Render {

	/** The renderer id (registry key in {@code juneau-renders.js}). */
	public String id;

	/** Optional per-column renderer metadata.  Omitted from the wire when absent. */
	public Map<String,Object> meta;

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
		if (ib(s))
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
	 * @param value The metadata value.  Serialized as given, so a {@link Number} or {@link Boolean} stays a JSON
	 * 	number or boolean (for example {@code meta("places", 2)}).
	 * @return This object.
	 */
	public Render meta(String key, Object value) {
		if (meta == null)
			meta = m();
		meta.put(key, value);
		return this;
	}

	/**
	 * Maps one cell value to a status tone on a {@code pill} renderer.
	 *
	 * <p>
	 * Emits {@code meta.tones[value] = tone.wire()}.  Value matching is exact and case-sensitive against the string
	 * the chip displays.  Re-adding a value replaces its tone and keeps its position.
	 *
	 * <h4 class='section'>Precedence (first hit wins):</h4>
	 * <ol>
	 * 	<li>An entry added here.
	 * 	<li>A slot pinned with {@link #category(String,int)}.
	 * 	<li>{@link #categorical()}: the value's hashed palette slot.
	 * 	<li>The column-wide tone from {@link #pill(StatusTone)}.
	 * 	<li>Otherwise no colour class; but in a column that uses {@code tone(...)} or {@code category(...)} an
	 * 		unmapped value renders as a neutral (grey) chip.
	 * </ol>
	 *
	 * <h4 class='section'>Example:</h4>
	 * <p class='bjava'>
	 * 	<jc>// Renders as {id:"pill",meta:{tones:{AVAILABLE:"success",SOLD:"neutral"}}}.</jc>
	 * 	Render <jv>r</jv> = Render.<jsm>pill</jsm>()
	 * 		.tone(<js>"AVAILABLE"</js>, StatusTone.<jsf>SUCCESS</jsf>)
	 * 		.tone(<js>"SOLD"</js>, StatusTone.<jsf>NEUTRAL</jsf>);
	 * </p>
	 *
	 * @param value The cell value, case-sensitive.  Must not be <jk>null</jk> or blank.
	 * @param tone The status tone.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalStateException If this is not a {@code pill} renderer.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank, or {@code tone} is <jk>null</jk>.
	 * @since 10.0.0
	 */
	public Render tone(String value, StatusTone tone) {
		requirePill("tone");
		if (! inb(value) || tone == null)
			throw new IllegalArgumentException("tone(value, tone) requires a non-blank value and a non-null tone");
		nested("tones").put(value, tone.wire());
		return this;
	}

	/**
	 * Colours every distinct value of a {@code pill} column from the categorical palette.
	 *
	 * <p>
	 * Emits {@code meta.categorical = true}.  A value without an explicit {@link #tone(String,StatusTone)} or
	 * {@link #category(String,int)} entry gets a stable colour from {@link PillPalette#slot(String)}: the same value
	 * has the same colour on every page.  Hashing is case-sensitive, and hashing {@code N} values into
	 * {@link PillPalette#SIZE} slots can collide; pin a closed set with {@link #category(String,int)}.
	 * See {@link #tone(String,StatusTone)} for the full precedence.
	 *
	 * @return This object.
	 * @throws IllegalStateException If this is not a {@code pill} renderer.
	 * @since 10.0.0
	 */
	public Render categorical() {
		requirePill("categorical");
		meta("categorical", true);
		return this;
	}

	/**
	 * Pins one cell value of a {@code pill} column to a categorical palette slot.
	 *
	 * <p>
	 * Emits {@code meta.categories[value] = slot}.  Use it for a closed value set so the values get distinct colours
	 * instead of a hash collision.  Value matching is exact and case-sensitive.  See
	 * {@link #tone(String,StatusTone)} for the full precedence.
	 *
	 * @param value The cell value, case-sensitive.  Must not be <jk>null</jk> or blank.
	 * @param slot The palette slot, 1 through {@link PillPalette#SIZE}.
	 * @return This object.
	 * @throws IllegalStateException If this is not a {@code pill} renderer.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank, or {@code slot} is out of range.
	 * @since 10.0.0
	 */
	public Render category(String value, int slot) {
		requirePill("category");
		if (! inb(value) || slot < 1 || slot > PillPalette.SIZE)
			throw new IllegalArgumentException("category(value, slot) requires a non-blank value and a slot from 1 to " + PillPalette.SIZE);
		nested("categories").put(value, slot);
		return this;
	}

	private void requirePill(String method) {
		if (! "pill".equals(id))
			throw new IllegalStateException(method + "(...) is only valid on a pill renderer, not '" + id + "'");
	}

	@SuppressWarnings("unchecked")
	private Map<String,Object> nested(String key) {
		if (meta == null)
			meta = m();
		return (Map<String,Object>)meta.computeIfAbsent(key, k -> new LinkedHashMap<String,Object>());
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
	 * {@link #meta(String,Object)} for {@code label} / {@code field}.
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
	 * ({@code currentColor}) until an explicit {@code tone} is chosen.  Chain {@link #meta(String,Object)} for
	 * {@code field}, {@code dot} ({@code on}|{@code off}) and {@code action} (a {@code rowActions} id).
	 *
	 * <p>
	 * {@code action} is optional and only meaningful on a table column: a pill named on a field-grid fill
	 * sink is unconditionally display-only and rejects an {@code action} outright.
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
	 * {@code tone} is one of the five status tones
	 * {@code info}|{@code success}|{@code warning}|{@code error}|{@code neutral} (see
	 * {@link StatusTone}).  A <jk>null</jk> or blank {@code tone} is omitted
	 * from {@code meta} (identical to {@link #pill()}); {@code neutral} is emitted verbatim and, like an absent tone,
	 * leaves the dot inheriting {@code currentColor}.  The tone is not validated server-side: the client
	 * (<c>NS._render.pillTones</c>) ignores an off-palette token, so the dot gets no tone class.  Prefer
	 * {@link #pill(StatusTone)} to stay on the palette.
	 *
	 * <p>
	 * Not to be confused with {@link #progress(int, Integer, Integer)}'s {@code warn}/{@code exceeds} meta, which are
	 * numeric thresholds rather than tone names and share no vocabulary with this palette.
	 *
	 * @param tone The explicit dot tone.  Can be <jk>null</jk> or blank to inherit.
	 * @return A new {@link Render}.
	 */
	public static Render pill(String tone) {
		var r = pill();
		if (inb(tone))
			r.meta("tone", tone);
		return r;
	}

	/**
	 * Creates a {@code pill} renderer with an explicit dot tone taken from the {@link StatusTone} palette.
	 *
	 * <p>
	 * Equivalent to <c>pill(tone == <jk>null</jk> ? <jk>null</jk> : tone.wire())</c>; a <jk>null</jk> tone is
	 * omitted from {@code meta}, identical to {@link #pill()}.
	 *
	 * <h4 class='section'>Example:</h4>
	 * <p class='bjava'>
	 * 	<jc>// Renders as {id:"pill",meta:{tone:"warning"}}.</jc>
	 * 	Render <jv>r</jv> = Render.<jsm>pill</jsm>(StatusTone.<jsf>WARNING</jsf>);
	 * </p>
	 *
	 * @param tone The explicit dot tone.  Can be <jk>null</jk> to inherit.
	 * @return A new {@link Render}.
	 */
	public static Render pill(StatusTone tone) {
		return pill(tone == null ? null : tone.wire());
	}
}
