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
package org.apache.juneau.rest.server.widgets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * A header strip of at-a-glance figures above a table &mdash; an ordered list of {@link StatItem}s
 * ({@link StatTile} / {@link StatBar} / {@link SegmentedBadge}).
 *
 * <p>
 * A pure data bean.  The views emitter paints the strip once, above the table's toolbar, from values the host has
 * already computed.
 *
 * <h5 class='section'>Display-only by construction:</h5>
 * <p>
 * There is no tile action, no endpoint, no refresh url, and no poll interval on this bean or on any {@link StatItem},
 * so nothing a runtime could bind a click or a timer to ever reaches the markup.  That is a design lock, not an
 * unimplemented feature: a figure that needs to change is a table column or a card, not a quick-stat.
 *
 * <p>
 * Tones are the closed {@link StatusTone} palette ({@code info} / {@code success} / {@code warning} / {@code error} /
 * {@code neutral}).  Any other token &mdash; including the {@link Tone} badge-palette names and the older
 * {@code ok}/{@code exceeds} pill vocabulary &mdash; fails {@link #validate()} closed.
 *
 * <p>
 * A {@code QuickStats} is <b>never</b> a {@code $FV} interpolation host: its strings are host-computed values, not
 * author-declared chrome templates, so there is no allowlist to extend and no per-response mutate/restore window.
 *
 * @since 10.0.0
 */
@BeanType(properties="contractVersion,id,items")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class QuickStats implements Widget {

	/** The frozen contract version for this widget.  Serialized as the JSON <b>string</b> {@code "1"}. */
	public static final String CONTRACT_VERSION = "1";

	/** The contract version; must equal {@link #CONTRACT_VERSION} at validation time. */
	public String contractVersion = CONTRACT_VERSION;

	/** The stable strip id, unique on the page.  Required, non-blank. */
	public String id;

	/** The stat items, in display order.  At least one is required; ids must be unique within the strip. */
	public List<StatItem> items;

	/**
	 * Creates an empty quick-stats strip with the given id.
	 *
	 * @param id The stable strip id.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link QuickStats}.
	 */
	public static QuickStats create(String id) {
		var q = new QuickStats();
		q.id = id;
		return q;
	}

	/**
	 * Sets the stat items, in display order.
	 *
	 * @param value The items.
	 * @return This object.
	 */
	public QuickStats items(StatItem...value) {
		items = l(value);
		return this;
	}

	@Override /* Widget */
	public void validate() {
		if (! CONTRACT_VERSION.equals(contractVersion))
			throw iaex("QuickStats contractVersion must be '%s': %s", CONTRACT_VERSION, contractVersion);
		if (id == null || id.isBlank())
			throw iaex("QuickStats id must not be null or blank.");
		if (items == null || items.isEmpty())
			throw iaex("QuickStats '%s' must declare at least one item.", id);
		var ids = new HashSet<String>();
		for (var i : items) {
			if (i == null)
				throw iaex("QuickStats '%s' item must not be null.", id);
			validateItem(i);
			if (! ids.add(i.id()))
				throw iaex("QuickStats '%s' duplicate item id '%s'.", id, i.id());
		}
	}

	/** Fail-closed per-item validation via total {@code instanceof} dispatch over the sealed {@link StatItem}. */
	private void validateItem(StatItem i) {
		requireId(i.id());
		if (i instanceof StatTile i2)
			validateStatTile(i2);
		else if (i instanceof StatBar i2)
			validateStatBar(i2);
		else if (i instanceof SegmentedBadge i2)
			validateSegmentedBadge(i2);
		else
			throw iaex("QuickStats '%s' unknown item type: %s", id, i.getClass().getName());
	}

	/** Validates a {@link StatTile}: label, non-null value, and tone. */
	private void validateStatTile(StatTile i2) {
		requireLabel(i2.id, i2.label);
		if (i2.value == null)
			throw iaex("QuickStats '%s' StatTile '%s' value must not be null.", id, i2.id);
		requireTone(i2.id, i2.tone);
	}

	/** Validates a {@link StatBar}: label, non-negative value, positive max, and tone. */
	private void validateStatBar(StatBar i2) {
		requireLabel(i2.id, i2.label);
		if (i2.value == null || i2.value < 0)
			throw iaex("QuickStats '%s' StatBar '%s' value must be >= 0.", id, i2.id);
		if (i2.max == null || i2.max <= 0)
			throw iaex("QuickStats '%s' StatBar '%s' max must be > 0.", id, i2.id);
		requireTone(i2.id, i2.tone);
	}

	/** Validates a {@link SegmentedBadge}: label, at least one segment, and each segment's shape. */
	private void validateSegmentedBadge(SegmentedBadge i2) {
		requireLabel(i2.id, i2.label);
		if (i2.segments == null || i2.segments.isEmpty())
			throw iaex("QuickStats '%s' SegmentedBadge '%s' must declare at least one segment.", id, i2.id);
		for (var s : i2.segments) {
			if (s == null)
				throw iaex("QuickStats '%s' SegmentedBadge '%s' segment must not be null.", id, i2.id);
			if (s.label == null || s.label.isBlank())
				throw iaex("QuickStats '%s' SegmentedBadge '%s' segment label must not be null or blank.", id, i2.id);
			if (s.count == null || s.count < 0)
				throw iaex("QuickStats '%s' SegmentedBadge '%s' segment count must be >= 0.", id, i2.id);
			requireTone(i2.id, s.tone);
		}
	}

	private void requireId(String itemId) {
		if (itemId == null || itemId.isBlank())
			throw iaex("QuickStats '%s' item id must not be null or blank.", id);
	}

	private void requireLabel(String itemId, String label) {
		if (label == null || label.isBlank())
			throw iaex("QuickStats '%s' item '%s' label must not be null or blank.", id, itemId);
	}

	private void requireTone(String itemId, String tone) {
		if (tone != null && ! StatusTone.isValid(tone))
			throw iaex("QuickStats '%s' item '%s' tone '%s' must be one of %s.", id, itemId, tone,
				String.join("|", StatusTone.WIRE_TOKENS));
	}
}
