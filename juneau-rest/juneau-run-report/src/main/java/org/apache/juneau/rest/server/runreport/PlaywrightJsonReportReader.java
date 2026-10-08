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
 * Reads Playwright's <c>--reporter=json</c> output.
 *
 * <p>
 * The framework key is <c>playwright</c>.  Suites and specs are walked recursively; a suite whose title equals its file
 * is the file-level suite and does not contribute to the test name.  A <c>flaky</c> test (failed, then passed on retry)
 * is a pass.  A truncated file yields no tests and a warning.
 */
public final class PlaywrightJsonReportReader implements ReportReader {

	private static final int MAX_DEPTH = 100;
	private static final String SEP = " › ";

	@Override /* ReportReader */
	public String fw() {
		return "playwright";
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
		for (var s : list(root.get("suites"))) {
			var sm = map(s);
			if (sm != null && ! walk(sm, List.of(), null, 0, lim, out, files, rows, rowFiles))
				break;
		}
		var display = stripCommonDir(files);
		for (var i = 0; i < rows.size(); i++) {
			var r = rows.get(i);
			var suite = clip(display.get(files.indexOf(rowFiles.get(i))), lim.maxNameChars());
			out.tests.add(new ReportTest(suite, r.name(), r.status(), r.ms(), r.msg(), r.trace()));
		}
		return out.build(fw());
	}

	/** Returns <jk>false</jk> when the test limit stopped the walk. */
	private boolean walk(Map<String,Object> suite, List<String> titles, String parentFile, int depth, ReportLimits lim, ReportBuilder out,
			List<String> files, List<ReportTest> rows, List<String> rowFiles) {
		if (depth > MAX_DEPTH)
			return true;
		var file = str(suite.get("file")) != null ? str(suite.get("file")) : parentFile;
		var title = str(suite.get("title"));
		var path = titles;
		if (title != null && ! title.isEmpty() && ! title.equals(str(suite.get("file")))) {
			path = new ArrayList<>(titles);
			path.add(title);
		}
		for (var sp : list(suite.get("specs"))) {
			var spec = map(sp);
			if (spec == null)
				continue;
			var specFile = str(spec.get("file")) != null ? str(spec.get("file")) : file;
			var key = specFile == null ? "(unknown file)" : specFile;
			if (! files.contains(key))
				files.add(key);
			var names = new ArrayList<>(path);
			if (str(spec.get("title")) != null)
				names.add(str(spec.get("title")));
			for (var t : list(spec.get("tests"))) {
				var tm = map(t);
				if (tm == null)
					continue;
				if (rows.size() >= lim.maxTests()) {
					out.truncated = true;
					out.warnings.add("report truncated at " + lim.maxTests() + " tests");
					return false;
				}
				rows.add(test(names, tm, lim));
				rowFiles.add(key);
			}
		}
		for (var child : list(suite.get("suites"))) {
			var cm = map(child);
			if (cm != null && ! walk(cm, path, file, depth + 1, lim, out, files, rows, rowFiles))
				return false;
		}
		return true;
	}

	private static ReportTest test(List<String> names, Map<String,Object> t, ReportLimits lim) {
		var name = String.join(SEP, names);
		var project = str(t.get("projectName"));
		if (project != null && ! project.isEmpty())
			name += " [" + project + "]";
		var results = list(t.get("results"));
		Map<String,Object> last = null;
		long total = 0;
		var hasMs = false;
		Map<String,Object> failing = null;
		for (var r : results) {
			var rm = map(r);
			if (rm == null)
				continue;
			last = rm;
			var d = ms(rm.get("duration"));
			if (d != null) {
				total += d;
				hasMs = true;
			}
			if (failing == null && isFailed(str(rm.get("status"))))
				failing = rm;
		}
		var status = status(str(t.get("status")), last == null ? null : str(last.get("status")));
		String msg = null, trace = null;
		if (status == TestStatus.FAIL && failing != null) {
			var text = errorText(failing);
			if (text != null) {
				var body = stripAnsi(text).strip();
				var first = RunViewChecks.firstLine(body);
				msg = first.isEmpty() ? null : clip(first, lim.maxMsgChars());
				trace = clip(body, lim.maxTraceChars());
			}
		}
		return new ReportTest(null, clip(name, lim.maxNameChars()), status, hasMs ? Long.valueOf(total) : null, msg, trace);
	}

	private static boolean isFailed(String s) {
		return "failed".equals(s) || "timedOut".equals(s) || "interrupted".equals(s);
	}

	private static TestStatus status(String testStatus, String lastResult) {
		if (testStatus != null) {
			switch (testStatus) {
				case "expected", "flaky": return TestStatus.PASS;
				case "unexpected": return TestStatus.FAIL;
				case "skipped": return TestStatus.SKIP;
				default: break;
			}
		}
		if (lastResult == null)
			return TestStatus.SKIP;
		if ("passed".equals(lastResult))
			return TestStatus.PASS;
		return isFailed(lastResult) ? TestStatus.FAIL : TestStatus.SKIP;
	}

	private static String errorText(Map<String,Object> result) {
		var e = map(result.get("error"));
		if (e != null && str(e.get("message")) != null)
			return str(e.get("message"));
		for (var x : list(result.get("errors"))) {
			var xm = map(x);
			if (xm != null && str(xm.get("message")) != null)
				return str(xm.get("message"));
		}
		return null;
	}
}
