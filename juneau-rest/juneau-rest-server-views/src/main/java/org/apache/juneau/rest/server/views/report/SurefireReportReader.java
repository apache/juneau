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
package org.apache.juneau.rest.server.views.report;

import static org.apache.juneau.rest.server.views.report.ReportText.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Reads Maven Surefire (and Failsafe) <c>TEST-*.xml</c> reports.
 *
 * <p>
 * The path may be a report directory, in which case every <c>TEST-*.xml</c> in it is read in name order, or a single file.
 * The framework key is <c>surefire</c>.
 */
public final class SurefireReportReader implements ReportReader {

	@Override /* ReportReader */
	public String fw() {
		return "surefire";
	}

	@Override /* ReportReader */
	public ReportResult read(Path path, ReportLimits limits) {
		var out = new ReportBuilder();
		if (Files.isDirectory(path)) {
			var files = new ArrayList<Path>();
			try (var ds = Files.newDirectoryStream(path, "TEST-*.xml")) {
				for (var p : ds)
					files.add(p);
			} catch (IOException e) {
				out.warnings.add("report unreadable: cannot list " + fileName(path));
				return out.build(fw());
			}
			if (files.isEmpty()) {
				out.warnings.add("no TEST-*.xml files in " + fileName(path));
				return out.build(fw());
			}
			files.sort(Comparator.comparing(p -> p.getFileName().toString()));
			for (var p : files) {
				if (out.truncated && out.tests.size() >= limits.maxTests())
					break;
				JUnitXmlParser.parse(p, limits, out);
			}
		} else {
			JUnitXmlParser.parse(path, limits, out);
		}
		return out.build(fw());
	}
}
