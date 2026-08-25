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
 * A labelled scalar in a {@link QuickStats} strip &mdash; the "42 open" tile.
 *
 * <p>
 * Display-only: the value is server-painted once at emit time.  A tile carries no endpoint, no action, and no refresh
 * interval, so there is nothing here for a runtime to click or poll.
 *
 * @since 10.0.0
 */
@BeanType(properties="id,label,value,tone")
@SuppressWarnings({
	"java:S1845" // "id" field mirrors the StatItem#id() contract for JSON emit; tone setter mirrors the field name (Juneau DSL convention).
})
public final class StatTile implements StatItem {

	/** The stable item id, unique within its {@link QuickStats}.  Required, non-blank. */
	public String id;

	/** The human label painted as {@code textContent} (e.g. <js>"Open"</js>).  Required, non-blank. */
	public String label;

	/** The server-painted value, already formatted for display (e.g. <js>"42"</js>).  Required, non-<jk>null</jk>. */
	public String value;

	/** Optional {@link StatusTone#wire()} token; off-palette values fail {@link QuickStats#validate()}. */
	public String tone;

	/**
	 * Creates a tile with the given id, label, and server-painted value.
	 *
	 * @param id The stable item id.  Must not be <jk>null</jk> or blank.
	 * @param label The human label.  Must not be <jk>null</jk> or blank.
	 * @param value The already-formatted value.  Must not be <jk>null</jk>.
	 * @return A new {@link StatTile}.
	 */
	public static StatTile of(String id, String label, String value) {
		var t = new StatTile();
		t.id = id;
		t.label = label;
		t.value = value;
		return t;
	}

	/**
	 * Sets the status tone.
	 *
	 * @param value The tone.  Can be <jk>null</jk> for no tone.
	 * @return This object.
	 */
	public StatTile tone(StatusTone value) {
		tone = value == null ? null : value.wire();
		return this;
	}

	@Override /* StatItem */
	public String id() {
		return id;
	}
}
