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

import java.nio.file.*;


/**
 * Reads a single JUnit-XML report file, such as pytest's <c>--junitxml</c> output.
 *
 * <p>
 * The framework key defaults to <c>junit-xml</c>; pass <c>"pytest"</c> to the constructor when the producer knows the
 * file came from pytest.
 */
public final class JUnitXmlReportReader implements ReportReader {

	private final String fw;

	/** Constructor; the framework key is <c>junit-xml</c>. */
	public JUnitXmlReportReader() {
		this("junit-xml");
	}

	/**
	 * Constructor.
	 *
	 * @param fw The framework key, matching <c>^[a-z0-9][a-z0-9._-]{0,31}$</c>.
	 * @throws IllegalArgumentException If the key does not match.
	 */
	public JUnitXmlReportReader(String fw) {
		if (! RunViewChecks.isFw(fw))
			throw iaex("JUnitXmlReportReader fw must match ^[a-z0-9][a-z0-9._-]{0,31}$; got '%s'.", fw);
		this.fw = fw;
	}

	@Override /* ReportReader */
	public String fw() {
		return fw;
	}

	@Override /* ReportReader */
	public ReportResult read(Path path, ReportLimits limits) {
		var out = new ReportBuilder();
		JUnitXmlParser.parse(path, limits, out);
		return out.build(fw);
	}
}
