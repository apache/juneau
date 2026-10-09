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

/**
 * One tile in a table's quick-stats strip ({@code quickStats.items[]}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jv>table</jv>.quickStats(<js>"work-unavailable"</js>,
 * 		QuickStat.<jsm>of</jsm>(<js>"unavailable"</js>, <js>"Work data"</js>, <js>"unavailable"</js>).tone(QuickStat.Tone.<jsf>ERROR</jsf>));
 * </p>
 *
 * @since 10.0.0
 */
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

	private final String id;
	private final String label;
	private final Object value;
	private Tone tone;

	private QuickStat(String id, String label, Object value) {
		this.id = id;
		this.label = label;
		this.value = value;
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
		if (id == null || id.isBlank())
			throw iaex("QuickStat id must not be null or blank.");
		return new QuickStat(id, label, value);
	}

	/**
	 * Sets this tile's visual tone.
	 *
	 * @param t The tone.  Can be <jk>null</jk> to unset.
	 * @return This object.
	 */
	public QuickStat tone(Tone t) {
		tone = t;
		return this;
	}

	/**
	 * Builds this tile's {@code quickStats.items[]} entry.
	 *
	 * @return {@code {id, label, value, tone?}}.
	 */
	public JsonMap toMap() {
		var m = new JsonMap();
		m.put("id", id);
		m.put("label", label);
		m.put("value", value);
		if (tone != null)
			m.put("tone", tone.wire());
		return m;
	}
}
