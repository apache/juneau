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
package org.apache.juneau.console.freemarker;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.marshall.collections.*;

/**
 * The built-in {@code thresholds} decorator: a horizontal mark line at a y value, on a carrier series
 * {@code juneau-thresholds-<n>}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Thresholds.<jsm>at</jsm>(500).label(<js>"SLO"</js>).tone(<js>"danger"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class Thresholds implements Decorator {

	private static final Set<String> TONES = Set.of("danger", "ok", "warning");

	private final Map<String,Object> value = new LinkedHashMap<>();

	private Thresholds() {}

	/** @param y The y value. @return A new decorator. */
	public static Thresholds at(Number y) {
		if (y == null)
			throw iaex("Thresholds y must not be null.");
		var x = new Thresholds();
		x.value.put("y", y);
		return x;
	}

	/** @param v The line label. @return This object. */
	public Thresholds label(String v) { value.put("label", v); return this; }

	/** @param v {@code danger}, {@code ok} or {@code warning}. @return This object. */
	public Thresholds tone(String v) {
		if (! TONES.contains(v))
			throw iaex("Thresholds tone '%s' must be 'danger', 'ok' or 'warning'.", v);
		value.put("tone", v);
		return this;
	}

	@Override
	public JsonMap toJson() { return JsonMap.of("use", "thresholds", "values", List.of(new LinkedHashMap<>(value))); }
}
