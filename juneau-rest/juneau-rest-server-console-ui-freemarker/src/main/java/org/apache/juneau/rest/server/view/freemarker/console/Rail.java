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

/**
 * The rail of a dashboard: a column of {@link Section}s beside the panel grid, with a splitter.
 *
 * <p>
 * The width is a default; the user's dragged width is saved per page.  Below 900 px the rail moves above the grid
 * as a collapsible "Filters" disclosure.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Rail.<jsm>left</jsm>().width(280).sections(SearchSection.<jsm>create</jsm>().publishes(Topics.<jsm>filter</jsm>(<js>"search"</js>)));
 * </p>
 *
 * @since 10.0.0
 */
public final class Rail {

	private final String side;
	private int width = 280;
	private final List<Section> sections = new ArrayList<>();

	private Rail(String side) {
		this.side = side;
	}

	/** @return A rail on the left of the grid. */
	public static Rail left() {
		return new Rail("left");
	}

	/** @return A rail on the right of the grid. */
	public static Rail right() {
		return new Rail("right");
	}

	/**
	 * Sets the default width.
	 *
	 * @param value The width in pixels, 200 to 480.  Defaults to 280.
	 * @return This object.
	 */
	public Rail width(int value) {
		width = value;
		return this;
	}

	/**
	 * Appends sections, top to bottom.
	 *
	 * @param values The sections.
	 * @return This object.
	 */
	public Rail sections(Section...values) {
		for (var v : values)
			sections.add(Objects.requireNonNull(v, "rail section must not be null"));
		return this;
	}

	/** @return The sections, in order. */
	List<Section> sections() {
		return sections;
	}

	/**
	 * Checks the rail and every section, reporting E-77 under {@code dash}.
	 *
	 * @param dash The dashboard id.
	 */
	void check(String dash) {
		if (width < 200 || width > 480)
			throw Section.e77(dash, "rail width " + width + " is outside 200-480");
		for (var s : sections)
			s.check(dash);
	}

	/** @return The rail JSON. */
	Map<String,Object> toMap() {
		var list = sections.stream().map(Section::toMap).toList();
		var m = new LinkedHashMap<String,Object>();
		m.put("side", side);
		m.put("width", width);
		m.put("sections", list);
		return m;
	}
}
