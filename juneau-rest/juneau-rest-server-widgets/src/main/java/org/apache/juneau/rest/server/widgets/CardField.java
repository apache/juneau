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

/**
 * One key/value entry in a {@link CardFieldList}: a fill key, a display label, and an initial scalar value.
 *
 * <p>
 * The {@link #value} is painted server-side via {@code textContent} so a static field-list shows its values with
 * JavaScript disabled; a refresh GET (when the enclosing {@link CardFieldList} carries a {@code refreshEndpoint})
 * overwrites the same {@code [data-juneau-card-field]} slot.
 *
 * @since 10.0.0
 */
public class CardField {

	/** The fill key &mdash; matches the refresh envelope's {@code fields[data]} entry.  Required, non-blank. */
	public String data;

	/** The display label, painted as {@code textContent}. */
	public String label;

	/**
	 * The initial scalar value, painted server-side via {@code textContent}.  Defaults to an empty string for a
	 * field that is only ever populated by refresh; a static field-list should set it.
	 */
	public String value = "";

	/**
	 * Creates a field with an empty initial value.
	 *
	 * @param data The fill key.  Must not be <jk>null</jk> or blank.
	 * @param label The display label.
	 * @return A new {@link CardField}.
	 */
	public static CardField of(String data, String label) {
		return of(data, label, "");
	}

	/**
	 * Creates a field with an initial value.
	 *
	 * @param data The fill key.  Must not be <jk>null</jk> or blank.
	 * @param label The display label.
	 * @param value The initial scalar value.  A <jk>null</jk> is normalized to an empty string.
	 * @return A new {@link CardField}.
	 */
	public static CardField of(String data, String label, String value) {
		var f = new CardField();
		f.data = data;
		f.label = label;
		f.value = value == null ? "" : value;
		return f;
	}
}
