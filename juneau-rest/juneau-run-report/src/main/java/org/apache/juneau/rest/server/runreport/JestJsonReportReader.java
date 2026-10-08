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

import static org.apache.juneau.rest.server.runreport.ReportJson.*;
import static org.apache.juneau.rest.server.runreport.ReportText.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.rest.server.runreport.RunEvent.*;

/**
 * Reads Jest's <c>--json</c> output.
 *
 * <p>
 * The framework key is <c>jest</c>.  A test file that failed to run (no assertions, a non-empty message) becomes one
 * <c>error</c> test named <c>(suite failed to run)</c>, so a crashed file is a visible red block rather than silence.
 * ANSI colour codes are stripped from messages.  A truncated file yields no tests and a warning.
 */
public final class JestJsonReportReader implements ReportReader {

	private static final String CRASHED = "(suite failed to run)";

	@Override /* ReportReader */
	public String fw() {
		return "jest";
	}

	@Override /* ReportReader */
	public ReportResult read(Path path, ReportLimits lim) {
		var out = new ReportBuilder();
		var root = readRoot(path, lim, out);
		if (root == null)
			return out.build(fw());

		var files = new ArrayList<String>();
		var rows = new ArrayList<ReportTest>();
		var rowFiles = new ArrayList<String>();
		outer: for (var tr : list(root.get("testResults"))) {
			var file = map(tr);
			if (file == null)
				continue;
			var raw = str(file.get("name")) == null ? "(unknown file)" : str(file.get("name"));
			if (! files.contains(raw))
				files.add(raw);
			var assertions = list(file.get("assertionResults"));
			var fileMsg = str(file.get("message"));
			if (assertions.isEmpty() && "failed".equals(str(file.get("status"))) && fileMsg != null && ! fileMsg.isBlank()) {
				if (rows.size() >= lim.maxTests()) {
					out.truncated = true;
					out.warnings.add("report truncated at " + lim.maxTests() + " tests");
					break;
				}
				var body = stripAnsi(fileMsg).strip();
				rows.add(new ReportTest(null, CRASHED, TestStatus.ERROR, null, clip(firstLine(body), lim.maxMsgChars()), clip(body, lim.maxTraceChars())));
				rowFiles.add(raw);
				continue;
			}
			for (var a : assertions) {
				var am = map(a);
				if (am == null)
					continue;
				if (rows.size() >= lim.maxTests()) {
					out.truncated = true;
					out.warnings.add("report truncated at " + lim.maxTests() + " tests");
					break outer;
				}
				rows.add(assertion(am, lim));
				rowFiles.add(raw);
			}
		}
		var display = stripCommonDir(files);
		for (var i = 0; i < rows.size(); i++) {
			var r = rows.get(i);
			var suite = clip(display.get(files.indexOf(rowFiles.get(i))), lim.maxNameChars());
			out.tests.add(new ReportTest(suite, r.name(), r.status(), r.ms(), r.msg(), r.trace()));
		}
		return out.build(fw());
	}

	private static ReportTest assertion(Map<String,Object> a, ReportLimits lim) {
		var name = str(a.get("fullName"));
		if (name == null || name.isEmpty()) {
			var parts = new ArrayList<String>();
			for (var t : list(a.get("ancestorTitles")))
				if (t instanceof String s)
					parts.add(s);
			var title = str(a.get("title"));
			if (title != null)
				parts.add(title);
			name = String.join(" ", parts);
		}
		var status = switch (String.valueOf(a.get("status"))) {
			case "passed" -> TestStatus.PASS;
			case "failed" -> TestStatus.FAIL;
			default -> TestStatus.SKIP;
		};
		String msg = null, trace = null;
		var fm = new ArrayList<String>();
		for (var m : list(a.get("failureMessages")))
			if (m instanceof String s)
				fm.add(stripAnsi(s));
		if (! fm.isEmpty()) {
			var first = firstLine(fm.get(0).strip());
			msg = first.isEmpty() ? null : clip(first, lim.maxMsgChars());
			trace = clip(String.join("\n", fm), lim.maxTraceChars());
		}
		return new ReportTest(null, clip(name, lim.maxNameChars()), status, ms(a.get("duration")), msg, trace);
	}

	private static String firstLine(String s) {
		return RunViewChecks.firstLine(s);
	}
}
