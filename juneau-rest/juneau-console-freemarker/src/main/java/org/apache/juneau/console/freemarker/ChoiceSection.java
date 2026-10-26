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

import java.util.*;

import org.apache.juneau.console.*;

/**
 * A rail section of checkboxes over the distinct values of one dictionary column of a data source.
 *
 * <p>
 * Publishes {@code {schemaVersion:1, viewId, columns:{<dim>: [selected...]}, options:{deselected:[...]}}}, omitting
 * {@code columns} and {@code options} when every value is selected.  The {@code deselected} list names the values the
 * user has unticked (capped at {@link #max(int) max}), so a reload can list them again and tick them back.  The array
 * value is a data-source extension: datatables do not apply it.
 * </p>
 *
 * <p>
 * Unticking every value publishes an empty list ({@code columns:{<dim>: []}}).  The data sources map an empty list to
 * no restriction on the dimension, so "nothing ticked" shows every row, the same as "everything ticked".  The section
 * shows a "Select all" button while a filter is in force.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	ChoiceSection.<jsm>fromDim</jsm>(<js>"orders"</js>, <js>"reason"</js>).label(<js>"Reason"</js>).max(50)
 * 		.publishes(Topics.<jsm>filter</jsm>(<js>"reasons"</js>));
 * </p>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S1192" // Repeated literals are contract wire keys; named constants would obscure the contract shape.
})
public final class ChoiceSection extends Section {

	private final String source;
	private final String dim;
	private int max = 50;

	private ChoiceSection(String source, String dim) {
		this.source = source;
		this.dim = dim;
	}

	/**
	 * Creates a choice section.
	 *
	 * @param source The data source id (its topic is {@code data:<source>}).
	 * @param dim The dictionary column whose values are listed.
	 * @return A new section.
	 */
	public static ChoiceSection fromDim(String source, String dim) {
		return new ChoiceSection(source, dim);
	}

	/**
	 * Caps the values shown before "Show all".
	 *
	 * @param value The cap, at least 1.  Defaults to 50.
	 * @return This object.
	 */
	public ChoiceSection max(int value) {
		max = value;
		return this;
	}

	@Override /* Section */
	public ChoiceSection publishes(String topic) {
		super.publishes(topic);
		return this;
	}

	@Override /* Section */
	public ChoiceSection label(String label) {
		super.label(label);
		return this;
	}

	@Override String kind() { return "choice"; }

	/** @return The source id. */
	String source() {
		return source;
	}

	@Override void check(String dash) {
		super.check(dash);
		if (! Topics.isKey(source))
			throw e77(dash, "choice section '" + id() + "' source '" + source + "' is not a valid topic key");
		if (! Topics.isKey(dim))
			throw e77(dash, "choice section '" + id() + "' dim '" + dim + "' is not a valid topic key");
		if (dim.equals("__proto__") || dim.equals("constructor") || dim.equals("prototype"))
			throw e77(dash, "choice section '" + id() + "' dim '" + dim + "' is a reserved name");
		if (max < 1)
			throw e77(dash, "choice section '" + id() + "' max must be at least 1");
	}

	@Override void body(Map<String,Object> m) {
		m.put("source", source);
		m.put("dim", dim);
		m.put("max", max);
	}
}
