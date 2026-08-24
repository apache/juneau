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
 * A row bean for the read-only "Related events" table nested inside an alert's expander &mdash; the
 * {@code NestedTableDef} dogfood.  Its rows are fetched by the nested table's own data GET, scoped to the
 * parent alert.
 *
 * @since 10.0.0
 */
public class AlertEvent {

	/** The parent alert id this event belongs to (the scope key the nested data GET filters on). */
	public String alertId;

	/** When the event fired. */
	public String timestamp;

	/** One of {@code "fired"}, {@code "notified"}, {@code "acknowledged"}, or {@code "note"}. */
	public String kind;

	/** Human-readable event detail. */
	public String detail;

	/**
	 * Creates a fully-populated row.
	 *
	 * @param alertId The parent alert id.
	 * @param timestamp When the event fired.
	 * @param kind The event kind token.
	 * @param detail The human-readable detail.
	 */
	public AlertEvent(String alertId, String timestamp, String kind, String detail) {
		this.alertId = alertId;
		this.timestamp = timestamp;
		this.kind = kind;
		this.detail = detail;
	}
}
