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

/**
 * A rail section with one text input.
 *
 * <p>
 * Publishes {@code {schemaVersion:1, viewId, search}} on Enter and after a 250 ms pause in typing.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	SearchSection.<jsm>create</jsm>().placeholder(<js>"status:open"</js>).publishes(Topics.<jsm>filter</jsm>(<js>"search"</js>));
 * </p>
 *
 * @since 10.0.0
 */
public final class SearchSection extends Section {

	private String placeholder;

	private SearchSection() {}

	/** @return A new section. */
	public static SearchSection create() {
		return new SearchSection();
	}

	/**
	 * Sets the input's placeholder text.
	 *
	 * @param value The placeholder.
	 * @return This object.
	 */
	public SearchSection placeholder(String value) {
		placeholder = value;
		return this;
	}

	@Override /* Section */
	public SearchSection publishes(String topic) {
		super.publishes(topic);
		return this;
	}

	@Override /* Section */
	public SearchSection label(String label) {
		super.label(label);
		return this;
	}

	@Override String kind() { return "search"; }

	@Override void body(Map<String,Object> m) {
		if (placeholder != null)
			m.put("placeholder", placeholder);
	}
}
