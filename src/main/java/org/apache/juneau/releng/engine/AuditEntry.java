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

package org.apache.juneau.releng.engine;

/**
 * One row of the Audit page: a run event or a dropped release candidate, flattened to a common shape.
 *
 * <p>
 * The run-event log carries no timestamps, so {@link #at} is set only for a dropped RC; events are ordered by
 * {@link #seq}, their position in the run's event file.
 */
public class AuditEntry {

	/** The run version. */
	public String run;

	/** The 1-based position of the record within the run: the line in the event file, or the index in {@code rcHistory}. */
	public int seq;

	/** The record kind: {@code step}, {@code end}, {@code note}, {@code done} or {@code rc-dropped}. */
	public String kind;

	/** When it happened (ISO-8601), or null when the record carries no time. */
	public String at;

	/** The step attempt id for a step or end event, the release candidate for a dropped RC, else null. */
	public String subject;

	/** The status, level or outcome, or null. */
	public String status;

	/** The step title, note text or drop reason, or null. */
	public String detail;

	/**
	 * Constructor for the JSON serializer.
	 */
	public AuditEntry() {
		// No-arg constructor required so the row is a bean.
	}

	/**
	 * Constructs a row.
	 */
	public AuditEntry(String run, int seq, String kind, String at, String subject, String status, String detail) {
		this.run = run;
		this.seq = seq;
		this.kind = kind;
		this.at = at;
		this.subject = subject;
		this.status = status;
		this.detail = detail;
	}
}
