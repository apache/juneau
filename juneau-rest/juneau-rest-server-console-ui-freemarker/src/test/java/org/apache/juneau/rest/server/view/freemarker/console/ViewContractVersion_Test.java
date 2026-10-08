// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * A pre-built SLOT_META body fails at render time with E-28; the static {@code view-contract-version} validator rule
 * still flags a stale pinned version.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ViewContractVersion_Test extends TestBase {

	private static final String RUNTIME = ViewsMixin.CONTRACT_VERSION;

	//-----------------------------------------------------------------------------------------------------------------
	// End to end through <@card type="datatables">
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(mixins=FreemarkerMixin.class, renderResponseStackTraces="true")
	public static class DevHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public FreemarkerMixin freemarker() {
			return ConsoleFreemarkerMixin.create().devMode(true).basePath("/templates/").chromeTemplate("c1/chrome.ftlh").build();
		}
		@RestGet(path="/t/{name}")
		public View t(@Path("name") String name) {
			return FreemarkerView.of("c1/" + name + ".ftlh");
		}
	}

	@Test void b03_render_prebuiltSlotMeta_failsWithE28() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class); var rsp = c.get("/t/cv-pinned-stale").run()) {
			rsp.assertStatus(500);
			assertContainsAll(rsp.getContent().asString(), "c1/cv-pinned-stale.ftlh", "body is a pre-built SLOT_META envelope");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static validator rule
	//-----------------------------------------------------------------------------------------------------------------

	private static List<ConsoleTemplateValidator.Finding> lint(String source) {
		// Only the version rule is under test.
		return ConsoleTemplateValidator.create().validateSource("t.ftlh", source).stream().filter(x -> "view-contract-version".equals(x.rule())).toList();
	}

	@Test void c01_validator_flagsStalePin() {
		var f = lint("<@page>\n<@card type=\"datatables\" id=\"t\">\n{ contractVersion:'1', quickStats:{contractVersion:'1'}, view:{ contractVersion:'4', id:'t' } }\n</@card>\n</@page>");
		assertEquals(1, f.size(), f::toString);
		assertEquals("view-contract-version", f.get(0).rule());
		assertEquals(3, f.get(0).line(), "anchored on the stale pin inside the card body");
	}

	@Test void c02_validator_acceptsOmittedAndMatchingAndNestedVersions() {
		assertTrue(lint("<@page>\n<@card type=\"datatables\" id=\"t\">\n{ contractVersion:'1', view:{ id:'t', detail:{contractVersion:'1'} } }\n</@card>\n</@page>").isEmpty());
		assertTrue(lint("<@page>\n<@card type=\"datatables\" id=\"t\">\n{ contractVersion:'1', view:{ contractVersion:'" + RUNTIME + "', id:'t' } }\n</@card>\n</@page>").isEmpty());
	}
}
