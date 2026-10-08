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

/**
 * Picks a {@link ReportReader} by report kind.
 *
 * <p>
 * The kinds are <c>surefire</c>, <c>junitxml</c>, <c>pytest</c>, <c>jest-json</c> and <c>playwright-json</c>.  The
 * parent kind <c>junitxml</c> yields the framework key <c>junit-xml</c>; a host that knows the file came from pytest
 * asks for <c>pytest</c>, which yields the key <c>pytest</c>.  Readers are stateless, so each kind shares one instance.
 */
public final class ReportReaders {

	private static final ReportReader SUREFIRE = new SurefireReportReader();
	private static final ReportReader JUNIT_XML = new JUnitXmlReportReader();
	private static final ReportReader PYTEST = new JUnitXmlReportReader("pytest");
	private static final ReportReader JEST = new JestJsonReportReader();
	private static final ReportReader PLAYWRIGHT = new PlaywrightJsonReportReader();

	private ReportReaders() {}

	/**
	 * Returns the reader for a report kind.
	 *
	 * @param kind The kind.  May be <jk>null</jk>.
	 * @return The reader, or <jk>null</jk> if the kind is unknown.
	 */
	public static ReportReader forKind(String kind) {
		if (kind == null)
			return null;
		return switch (kind) {
			case "surefire" -> SUREFIRE;
			case "junitxml" -> JUNIT_XML;
			case "pytest" -> PYTEST;
			case "jest-json" -> JEST;
			case "playwright-json" -> PLAYWRIGHT;
			default -> null;
		};
	}
}
