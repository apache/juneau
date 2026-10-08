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

import java.nio.file.*;

/**
 * Reads one test-report format.
 *
 * <p>
 * A reader <b>never throws for bad input</b>.  A missing, unreadable, oversize, malformed or truncated file yields a
 * result with the tests that could be recovered and a warning.  Readers are stateless and thread-safe.
 */
public interface ReportReader {

	/**
	 * Returns the default framework key for the tests this reader produces.
	 *
	 * @return The key, for example <c>surefire</c> or <c>jest</c>.
	 */
	String fw();

	/**
	 * Reads a report.
	 *
	 * @param path The report file (or, for Surefire, a directory of them).
	 * @param limits The bounds to apply.
	 * @return The result; never <jk>null</jk>.
	 */
	ReportResult read(Path path, ReportLimits limits);

	/**
	 * Reads a report with {@link ReportLimits#DEFAULT}.
	 *
	 * @param path The report file or directory.
	 * @return The result; never <jk>null</jk>.
	 */
	default ReportResult read(Path path) {
		return read(path, ReportLimits.DEFAULT);
	}
}
