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

import org.apache.juneau.commons.bean.*;

/**
 * A value-against-a-maximum meter in a {@link QuickStats} strip &mdash; the "180 of 500 seats" bar.
 *
 * <p>
 * Display-only: the fill percentage is computed and painted once, server-side.  There is deliberately no threshold
 * vocabulary here &mdash; a bar's colour comes from its explicit {@link #tone}, not from a client-side comparison, so
 * this is not a second copy of the {@code progress} cell renderer's threshold logic.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,value,max,tone")
public final class StatBar implements StatItem {

	/** The stable item id, unique within its {@link QuickStats}.  Required, non-blank. */
	public String id;

	/** The human label painted as {@code textContent} (e.g. <js>"Seats used"</js>).  Required, non-blank. */
	public String label;

	/** The current value.  Required, {@code >= 0}. */
	public Long value;

	/** The maximum the value is measured against.  Required, {@code > 0}. */
	public Long max;

	/** Optional {@link StatusTone#wire()} token; off-palette values fail {@link QuickStats#validate()}. */
	public String tone;

	/**
	 * Creates a bar with the given id, label, value, and maximum.
	 *
	 * @param id The stable item id.  Must not be <jk>null</jk> or blank.
	 * @param label The human label.  Must not be <jk>null</jk> or blank.
	 * @param value The current value.  Must be {@code >= 0}.
	 * @param max The maximum.  Must be {@code > 0}.
	 * @return A new {@link StatBar}.
	 */
	public static StatBar of(String id, String label, long value, long max) {
		var b = new StatBar();
		b.id = id;
		b.label = label;
		b.value = value;
		b.max = max;
		return b;
	}

	/**
	 * Sets the status tone.
	 *
	 * @param value The tone.  Can be <jk>null</jk> for no tone.
	 * @return This object.
	 */
	public StatBar tone(StatusTone value) {
		tone = value == null ? null : value.wire();
		return this;
	}

	/**
	 * Returns the server-computed fill percentage, clamped to {@code 0..100}.
	 *
	 * @return The fill percentage.
	 */
	public int percent() {
		var pct = Math.round((value * 100.0) / max);
		return (int) Math.max(0, Math.min(100, pct));
	}

	@Override /* StatItem */
	public String id() {
		return id;
	}
}
