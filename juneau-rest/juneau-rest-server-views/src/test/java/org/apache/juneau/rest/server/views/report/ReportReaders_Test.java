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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

class ReportReaders_Test extends TestBase {

	@Test void a01_kinds() {
		assertEquals("surefire", ReportReaders.forKind("surefire").fw());
		assertEquals("junit-xml", ReportReaders.forKind("junitxml").fw());
		assertEquals("pytest", ReportReaders.forKind("pytest").fw());
		assertEquals("jest", ReportReaders.forKind("jest-json").fw());
		assertEquals("playwright", ReportReaders.forKind("playwright-json").fw());
		assertNull(ReportReaders.forKind("bogus"));
		assertNull(ReportReaders.forKind(null));
	}

	@Test void b01_resultFeedsAnEventLog() {
		var log = RunViewLog.create();
		var r = ReportReaders.forKind("surefire").read(resource("surefire-mixed"));
		r.toEvents("tests").forEach(log::append);
		var page = log.page(null, 100);
		assertEquals("replace", page.events().get(0).toContractMap().get("ev"));
		assertEquals(6, page.events().size());
		page.validate();
	}
}
