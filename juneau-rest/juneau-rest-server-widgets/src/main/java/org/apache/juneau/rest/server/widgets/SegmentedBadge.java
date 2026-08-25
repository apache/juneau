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
 * A labelled breakdown of counts in a {@link QuickStats} strip &mdash; the "3 failed / 12 running / 40 done" badge.
 *
 * <p>
 * Display-only, like every {@link StatItem}: each segment is a tone plus a server-painted count, and no segment is
 * clickable or filterable.  A segmented badge is presentation of an already-computed breakdown, not a facet control.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,segments")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setter mirrors the field name (Juneau DSL convention); "id" field mirrors the StatItem#id() contract for JSON emit.
})
public final class SegmentedBadge implements StatItem {

	/**
	 * One tone-plus-count slice of a {@link SegmentedBadge}.
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="label,count,tone")
	public static final class Segment {

		/** The segment's human label painted as {@code textContent} (e.g. <js>"failed"</js>).  Required, non-blank. */
		public String label;

		/** The segment count.  Required, {@code >= 0}. */
		public Long count;

		/** Optional {@link StatusTone#wire()} token; off-palette values fail {@link QuickStats#validate()}. */
		public String tone;

		/**
		 * Creates a segment with the given label and count.
		 *
		 * @param label The segment label.  Must not be <jk>null</jk> or blank.
		 * @param count The segment count.  Must be {@code >= 0}.
		 * @return A new {@link Segment}.
		 */
		public static Segment of(String label, long count) {
			var s = new Segment();
			s.label = label;
			s.count = count;
			return s;
		}

		/**
		 * Sets the status tone.
		 *
		 * @param value The tone.  Can be <jk>null</jk> for no tone.
		 * @return This object.
		 */
		public Segment tone(StatusTone value) {
			tone = value == null ? null : value.wire();
			return this;
		}
	}

	/** The stable item id, unique within its {@link QuickStats}.  Required, non-blank. */
	public String id;

	/** The human label painted as {@code textContent} (e.g. <js>"Jobs"</js>).  Required, non-blank. */
	public String label;

	/** The segments, in display order.  At least one is required. */
	public List<Segment> segments;

	/**
	 * Creates a segmented badge with the given id and label.
	 *
	 * @param id The stable item id.  Must not be <jk>null</jk> or blank.
	 * @param label The human label.  Must not be <jk>null</jk> or blank.
	 * @return A new {@link SegmentedBadge}.
	 */
	public static SegmentedBadge of(String id, String label) {
		var b = new SegmentedBadge();
		b.id = id;
		b.label = label;
		return b;
	}

	/**
	 * Sets the segments, in display order.
	 *
	 * @param value The segments.
	 * @return This object.
	 */
	public SegmentedBadge segments(Segment...value) {
		segments = l(value);
		return this;
	}

	@Override /* StatItem */
	public String id() {
		return id;
	}
}
