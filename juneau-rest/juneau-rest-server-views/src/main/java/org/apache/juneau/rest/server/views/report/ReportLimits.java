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

import static org.apache.juneau.commons.utils.Shorts.*;

/**
 * Bounds applied while reading a report, so a hostile or runaway file cannot exhaust memory.
 *
 * @param maxFileBytes The largest file that is read; larger files are skipped with a warning.
 * @param maxTests The most tests kept from one file; reading stops there with a warning.
 * @param maxNameChars The longest suite or test name kept; longer names are clipped.
 * @param maxMsgChars The longest failure message kept.
 * @param maxTraceChars The longest stack trace kept.
 */
public record ReportLimits(long maxFileBytes, int maxTests, int maxNameChars, int maxMsgChars, int maxTraceChars) {

	/** The default limits: 32 MiB, 100,000 tests, 512-char names, 2,000-char messages, 8,000-char traces. */
	public static final ReportLimits DEFAULT = new ReportLimits(33_554_432L, 100_000, 512, 2000, 8000);

	/**
	 * Constructor.
	 *
	 * @throws IllegalArgumentException If any value is not positive.
	 */
	public ReportLimits {
		if (maxFileBytes < 1 || maxTests < 1 || maxNameChars < 1 || maxMsgChars < 1 || maxTraceChars < 1)
			throw iaex("ReportLimits values must all be positive; got %s, %s, %s, %s, %s.", maxFileBytes, maxTests, maxNameChars, maxMsgChars, maxTraceChars);
	}
}
