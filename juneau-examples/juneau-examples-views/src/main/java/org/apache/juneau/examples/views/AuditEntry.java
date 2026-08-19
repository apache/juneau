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
 * A row bean for the sibling plain "Audit Log" tab &mdash; deliberately a different row type than {@link Widget},
 * so the example page composes two distinct {@code ViewDef} row shapes instead of reusing one type everywhere.
 *
 * @since 10.0.0
 */
public class AuditEntry {

	/** An ISO-8601 timestamp; rendered via the {@code date} renderer. */
	public String timestamp;

	/** Who performed the action. */
	public String actor;

	/** What happened. */
	public String action;

	/**
	 * Creates a fully-populated row.
	 *
	 * @param timestamp An ISO-8601 timestamp.
	 * @param actor Who performed the action.
	 * @param action What happened.
	 */
	public AuditEntry(String timestamp, String actor, String action) {
		this.timestamp = timestamp;
		this.actor = actor;
		this.action = action;
	}
}
