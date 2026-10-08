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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/** Source-shape and Node behavioral checks for the ribbon "export loud" contract (disabled button, one logged E-JS-69). */
@SuppressWarnings({
	"resource" // Closeable test fixture held in a static field; lifecycle managed by the test/framework, not a real leak.
})
class ViewsJs_ExportDetect_Test extends TestBase {

	@Rest(mixins=ViewsMixin.class)
	public static class WithMixin extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient c = MockRestClient.buildLax(WithMixin.class);

	private static Map<?,?> report() {
		var r = NodeHarness.report("export-detect.cjs", ViewsMixin.RIBBON_JS_RESOURCE);
		assumeTrue(r != null, "node (or export-detect.cjs) not available - behavioral layer skipped");
		return r;
	}

	@Test void a01_sourceDeclaresTheDependencyTableAndTheCode() throws Exception {
		var body = c.get(ViewsMixin.RIBBON_JS_PATH).run().assertStatus(200).getContent().asString();
		assertTrue(body.contains("EXPORT_BUTTON_DEPS"), "dependency table");
		assertTrue(body.contains("E-JS-69"), "code");
		assertTrue(body.contains("return { ok: ok, missing: missing }"), "result shape");
	}

	@Test void b01_okAndMissingAreSplitByDependency() {
		var r = report();
		assertEquals(Map.of("ok", List.of(), "missing", List.of()), r.get("noButtonsExtension"));
		assertEquals(Map.of("ok", List.of("copy", "csv"), "missing", List.of()), r.get("plainButtons"));
		assertEquals(Map.of("ok", List.of("copy"), "missing", List.of(Map.of("id", "excel", "needs", "jszip"))), r.get("hardExcelNoJszip"));
		assertEquals(Map.of("ok", List.of("pdf"), "missing", List.of()), r.get("hardPdfWithPdfmake"));
	}

	@Test void b02_optionalEntriesStayASilentOptIn() {
		var r = report();
		assertEquals(Map.of("ok", List.of(), "missing", List.of()), r.get("optionalExcelNoJszip"));
		assertEquals(Map.of("ok", List.of("excel"), "missing", List.of()), r.get("optionalExcelWithJszip"));
	}

	@Test void c01_missingButtonRendersDisabledWithATipNamingTheLibrary() {
		var r = report();
		assertEquals(2, ((Number)r.get("disabledButtonCount")).intValue());
		assertTrue(((String)r.get("disabledButtonTip")).contains("JSZip"), () -> String.valueOf(r.get("disabledButtonTip")));
		assertNull(r.get("disabledTitleAttr"));
	}

	@Test void c02_e_js_69_isLoggedOncePerTable() {
		var r = report();
		assertEquals(1, ((Number)r.get("errorCountAcrossBothActions")).intValue());
		assertEquals(true, r.get("ctxFlagSet"));
		assertTrue(((String)r.get("errorText")).contains("ribbon export 'excel' needs 'JSZip'"), () -> String.valueOf(r.get("errorText")));
	}
}
