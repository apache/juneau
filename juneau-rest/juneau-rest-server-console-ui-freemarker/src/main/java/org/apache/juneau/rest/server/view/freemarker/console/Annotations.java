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

/**
 * The built-in {@code annotations} decorator: ECharts {@code markPoint} presets (max, min, average) on every series,
 * or explicit points on a carrier series {@code juneau-annotations-<n>}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Annotations.<jsm>max</jsm>().withMin().label(<js>"{name}"</js>);
 * </p>
 *
 * @since 10.0.0
 */
public final class Annotations implements Decorator {

	private final Set<String> presets = new LinkedHashSet<>();
	private List<Map<String,Object>> points;
	private String label;

	private Annotations() {}

	/** @return A new decorator marking each series' maximum. */
	public static Annotations max() { return new Annotations().withMax(); }

	/** @return A new decorator marking each series' minimum. */
	public static Annotations min() { return new Annotations().withMin(); }

	/** @return A new decorator marking each series' average. */
	public static Annotations average() { return new Annotations().withAverage(); }

	/** @param rows Explicit points {@code {x, y, label}}. @return A new decorator. */
	public static Annotations points(List<? extends Map<String,?>> rows) {
		var x = new Annotations();
		x.points = Markers.copyRows(rows);
		return x;
	}

	/** @return This object, also marking the maximum. */
	public Annotations withMax() { presets.add("max"); return this; }

	/** @return This object, also marking the minimum. */
	public Annotations withMin() { presets.add("min"); return this; }

	/** @return This object, also marking the average. */
	public Annotations withAverage() { presets.add("average"); return this; }

	/** @param v A label template ({@code template} fn tokens). @return This object. */
	public Annotations label(String v) { label = v; return this; }

	@Override
	public JsonMap toJson() {
		var m = JsonMap.of("use", "annotations");
		if (! presets.isEmpty()) m.put("presets", List.copyOf(presets));
		if (points != null) m.put("values", List.copyOf(points));
		if (label != null) m.put("label", label);
		return m;
	}
}
