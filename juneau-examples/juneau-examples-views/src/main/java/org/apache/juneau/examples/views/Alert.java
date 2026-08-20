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
 * A row bean for the fake "Alerts" tab &mdash; the {@code RowDetailDef} / {@code ActionBar} dogfood
 * (two sections, two mutating actions, expand GET).
 *
 * @since 10.0.0
 */
public class Alert {

	/** Stable row id (also the expand-GET {@code {id}}). */
	public String id;

	/** One of {@code "critical"}, {@code "warning"}, or {@code "info"}. */
	public String severity;

	/** Short title shown in the table and the overview section. */
	public String title;

	/** One of {@code "open"}, {@code "acknowledged"}, or {@code "escalated"}. */
	public String status;

	/** Longer summary shown only in the expander. */
	public String summary;

	/** On-call assignee shown only in the expander. */
	public String assignee;

	/**
	 * Creates a fully-populated row.
	 *
	 * @param id The stable row id.
	 * @param severity The severity token.
	 * @param title The short title.
	 * @param status The workflow status.
	 * @param summary The expander-only summary.
	 * @param assignee The expander-only assignee.
	 */
	public Alert(String id, String severity, String title, String status, String summary, String assignee) {
		this.id = id;
		this.severity = severity;
		this.title = title;
		this.status = status;
		this.summary = summary;
		this.assignee = assignee;
	}
}
