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

import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.rest.server.views.*;

/**
 * Shared plumbing for the JSON report readers (Jest and Playwright): a size-checked tree parse and loose accessors.
 *
 * <p>
 * A tree is acceptable here because both tools write the file once at the end and the size is capped by
 * {@link ReportLimits#maxFileBytes()}.
 */
final class ReportJson {

	private ReportJson() {}

	/** Returns the parsed root object, or <jk>null</jk> after adding a warning to {@code out}. */
	@SuppressWarnings("unchecked")
	static Map<String,Object> readRoot(Path file, ReportLimits lim, ReportBuilder out) {
		var name = fileName(file);
		try {
			if (Files.size(file) > lim.maxFileBytes()) {
				out.skippedFiles++;
				out.warnings.add("report skipped: " + name + " is larger than " + lim.maxFileBytes() + " bytes");
				return null;
			}
		} catch (IOException e) {
			out.skippedFiles++;
			out.warnings.add("report unreadable: " + (e instanceof NoSuchFileException ? "file not found" : "cannot read") + " (" + name + ")");
			return null;
		}
		try (var in = Files.newBufferedReader(file)) {
			var m = Json.to(in, Map.class);
			if (m == null) {
				out.warnings.add("report unreadable: empty file (" + name + ")");
				return null;
			}
			return m;
		} catch (Exception e) {
			var msg = RunViewChecks.firstLine(e.getMessage());
			out.truncated = true;
			out.warnings.add("report unreadable: " + clip(msg.isEmpty() ? e.getClass().getSimpleName() : msg, 200) + " (" + name + ")");
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	static Map<String,Object> map(Object o) {
		return o instanceof Map ? (Map<String,Object>)o : null;
	}

	static List<?> list(Object o) {
		return o instanceof List ? (List<?>)o : List.of();
	}

	static String str(Object o) {
		return o instanceof String s ? s : null;
	}

	/** Returns a non-negative whole number of milliseconds, or <jk>null</jk>. */
	static Long ms(Object o) {
		if (o instanceof Number n) {
			var d = n.doubleValue();
			if (d >= 0 && d < 9.0e12)
				return Math.round(d);
		}
		return null;
	}
}
