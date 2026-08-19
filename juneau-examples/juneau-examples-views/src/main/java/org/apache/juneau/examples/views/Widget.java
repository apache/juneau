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
package org.apache.juneau.examples.views;

/**
 * A row bean for the "Active"/"Archived" sub-tab views.
 *
 * <p>
 * {@code notes} is deliberately NOT one of the table's visible {@code Column}s &mdash; it exists only to give the
 * row-details expander something worth showing that a visible column does not already contain.
 *
 * @since 10.0.0
 */
public class Widget {

	/** The widget's stable name (also the row's natural sort key). */
	public String name;

	/** One of {@code "active"}, {@code "error"}, or {@code "archived"} &mdash; rendered via the {@code tag} renderer. */
	public String status;

	/** The team/person responsible for this widget. */
	public String owner;

	/** An ISO-8601 timestamp; rendered via the {@code date} renderer. */
	public String updatedAt;

	/** Free-text detail only shown in the row-details expander, never in a table column. */
	public String notes;

	/**
	 * Creates a fully-populated row.
	 *
	 * @param name The widget name.
	 * @param status The status token.
	 * @param owner The owning team/person.
	 * @param updatedAt An ISO-8601 timestamp.
	 * @param notes Free-text detail (expander-only).
	 */
	public Widget(String name, String status, String owner, String updatedAt, String notes) {
		this.name = name;
		this.status = status;
		this.owner = owner;
		this.updatedAt = updatedAt;
		this.notes = notes;
	}
}
