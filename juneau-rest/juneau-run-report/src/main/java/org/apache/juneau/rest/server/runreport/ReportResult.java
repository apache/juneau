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
package org.apache.juneau.rest.server.runreport;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.rest.server.runreport.RunEvent.*;

/**
 * What a {@link ReportReader} recovered from a report.
 */
public final class ReportResult {

	private static final String UNNAMED = "(unnamed)";

	private final String fw;
	private final List<ReportTest> tests;
	private final List<String> warnings;
	private final boolean truncated;
	private final int skippedFiles;

	/**
	 * Constructor.
	 *
	 * @param fw The framework key.
	 * @param tests The recovered tests.
	 * @param warnings Human-readable warnings, for example <c>report truncated: unexpected end of file after 4120 tests</c>.
	 * @param truncated Whether the file ended early or the test limit was hit.
	 * @param skippedFiles The number of files that were not read (over the size limit, unreadable).
	 */
	public ReportResult(String fw, List<ReportTest> tests, List<String> warnings, boolean truncated, int skippedFiles) {
		if (! RunViewChecks.isFw(fw))
			throw iaex("ReportResult fw must match ^[a-z0-9][a-z0-9._-]{0,31}$; got '%s'.", fw);
		this.fw = fw;
		this.tests = List.copyOf(tests);
		this.warnings = List.copyOf(warnings);
		this.truncated = truncated;
		this.skippedFiles = skippedFiles;
	}

	/**
	 * Returns the framework key.
	 *
	 * @return The key.
	 */
	public String fw() {
		return fw;
	}

	/**
	 * Returns the recovered tests.
	 *
	 * @return An unmodifiable list, in report order.
	 */
	public List<ReportTest> tests() {
		return tests;
	}

	/**
	 * Returns the warnings.
	 *
	 * @return An unmodifiable list.
	 */
	public List<String> warnings() {
		return warnings;
	}

	/**
	 * Returns whether the file ended early or the test limit was hit.
	 *
	 * @return <jk>true</jk> if so.
	 */
	public boolean truncated() {
		return truncated;
	}

	/**
	 * Returns the number of files that were not read.
	 *
	 * @return The count.
	 */
	public int skippedFiles() {
		return skippedFiles;
	}

	/**
	 * Returns the events a producer appends at the end of a step.
	 *
	 * <p>
	 * The sequence is a <c>replace</c> for the step, one <c>test</c> per recovered test, then one <c>warn</c> note per
	 * warning.  When no test was recovered and there are warnings, only the notes are returned, so the step keeps its
	 * live <c>suite</c> placeholders.  A valid but empty report yields just the <c>replace</c>.
	 *
	 * @param step The id of the step the results belong to.
	 * @return The events, not yet numbered.
	 */
	public List<RunEvent> toEvents(String step) {
		var out = new ArrayList<RunEvent>();
		if (! (tests.isEmpty() && ! warnings.isEmpty()))
			out.add(RunEvent.replace(step));
		for (var t : tests)
			out.add(toEvent(step, t));
		for (var w : warnings)
			out.add(RunEvent.note(Level.WARN, clip(w, RunEvent.MAX_NOTE)).withStep(step));
		return out;
	}

	private RunEvent toEvent(String step, ReportTest t) {
		var e = RunEvent.test(step, fw, name(t.suite()), name(t.name()), t.status());
		if (t.ms() != null && t.ms() >= 0 && t.ms() <= RunEvent.MAX_SAFE_INT)
			e = e.withMs(t.ms());
		var withMsg = t.msg() == null || t.msg().isEmpty() ? e : e.withMsg(clip(t.msg(), RunEvent.MAX_MSG));
		var full = t.trace() == null || t.trace().isEmpty() ? withMsg : withMsg.withTrace(clip(t.trace(), RunEvent.MAX_TRACE));
		// Control characters expand six-fold when serialized, so an extreme test can exceed the event size cap; shed the
		// bulkiest members rather than lose the test.
		for (var candidate : List.of(full, withMsg, e)) {
			try {
				return candidate.validate();
			} catch (IllegalArgumentException ex) {
				// try the next, smaller candidate
			}
		}
		return e;
	}

	private static String name(String s) {
		return s == null || s.isEmpty() ? UNNAMED : clip(s, RunEvent.MAX_NAME);
	}

	private static String clip(String s, int max) {
		return ReportText.clip(s, max);
	}
}
