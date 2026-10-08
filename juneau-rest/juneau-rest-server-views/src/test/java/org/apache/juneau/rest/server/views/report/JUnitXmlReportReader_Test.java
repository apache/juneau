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

import static org.apache.juneau.rest.server.views.report.ReportTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class JUnitXmlReportReader_Test extends TestBase {

	@Test void c01_pytestFw() {
		assertEquals("pytest", new JUnitXmlReportReader("pytest").fw());
	}

	@Test void c02_pytestMapping() {
		var r = new JUnitXmlReportReader("pytest").read(resource("pytest.xml"));
		assertEquals(List.of("pass", "fail"), statuses(r));
		assertEquals("tests.test_a", r.tests().get(0).suite());
		assertEquals(500L, r.tests().get(0).ms());
		assertEquals("assert 1 == 2", r.tests().get(1).msg());
		assertEquals("pytest", r.fw());
	}

	@Test void c03_defaultFwIsJunitXml() {
		assertEquals("junit-xml", new JUnitXmlReportReader().fw());
	}

	@Test void c04_badFwRejected() {
		assertThrows(IllegalArgumentException.class, () -> new JUnitXmlReportReader("Bad Fw"));
	}
}
