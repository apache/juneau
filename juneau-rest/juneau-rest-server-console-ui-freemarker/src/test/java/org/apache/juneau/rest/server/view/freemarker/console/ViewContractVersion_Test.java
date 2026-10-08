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
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Path;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * {@code view.contractVersion} auto-injection: omitted &rarr; injected, matching &rarr; silent, mismatched
 * &rarr; WARNING naming the template and both versions, plus a loud failure in dev mode and a static validator finding.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class ViewContractVersion_Test extends TestBase {

	private static final String RUNTIME = ViewsMixin.CONTRACT_VERSION;

	/** Captures WARNING-and-above records emitted by {@link DatatablesCardType}. */
	private static final class Capture extends Handler {
		final List<String> messages = new ArrayList<>();
		@Override public void publish(LogRecord r) { messages.add(r.getLevel() + ": " + r.getMessage()); }
		@Override public void flush() { /* no-op */ }
		@Override public void close() { /* no-op */ }
	}

	private static List<String> warningsFrom(Runnable r) {
		var log = Logger.getLogger(DatatablesCardType.class.getName());
		var cap = new Capture();
		log.addHandler(cap);
		try {
			r.run();
		} finally {
			log.removeHandler(cap);
		}
		return cap.messages;
	}

	private static JsonMap parse(String json5) {
		try {
			return Json5Parser.DEFAULT.read(json5, JsonMap.class);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private static JsonMap catalog(String versionClause) {
		return parse("{contractVersion:'1', layout:'wide', view:{" + versionClause + "id:'t', dataUrl:'/d', columns:[]}}");
	}

	@Test void a01_omitted_isInjected() {
		var slot = catalog("");
		var warnings = warningsFrom(() -> DatatablesCardType.reconcileViewVersion("t", slot, "p.ftlh", true));
		assertEquals(RUNTIME, slot.getMap("view").getString("contractVersion"));
		assertTrue(warnings.isEmpty(), warnings::toString);
	}

	@Test void a02_omittedSlotVersion_isAlsoDefaulted() {
		var slot = parse("{view:{id:'t', dataUrl:'/d', columns:[]}}");
		DatatablesCardType.reconcileViewVersion("t", slot, "p.ftlh", false);
		assertBean(slot, "contractVersion,view{contractVersion}", "1,{" + RUNTIME + "}");
	}

	@Test void a03_matching_noWarning() {
		var slot = catalog("contractVersion:'" + RUNTIME + "',");
		var warnings = warningsFrom(() -> DatatablesCardType.reconcileViewVersion("t", slot, "p.ftlh", true));
		assertTrue(warnings.isEmpty(), warnings::toString);
		assertEquals(RUNTIME, slot.getMap("view").getString("contractVersion"));
	}

	@Test void a04_mismatched_warnsNamingTemplateAndBothVersions_notDev() {
		var slot = catalog("contractVersion:'0',");
		var warnings = warningsFrom(() -> DatatablesCardType.reconcileViewVersion("t", slot, "p.ftlh", false));
		assertEquals(1, warnings.size(), warnings::toString);
		assertTrue(warnings.get(0).startsWith("WARNING: "), warnings::toString);
		assertContainsAll(warnings.get(0), "'p.ftlh'", "card 't'", "is '0'", "is '" + RUNTIME + "'");
		assertEquals("0", slot.getMap("view").getString("contractVersion"), "an explicit pin is left alone");
	}

	@Test void a05_mismatched_failsLoudlyInDevMode() {
		var slot = catalog("contractVersion:'0',");
		var ex = assertThrows(IllegalArgumentException.class, () -> DatatablesCardType.reconcileViewVersion("t", slot, "p.ftlh", true));
		assertContainsAll(ex.getMessage(), "'p.ftlh'", "is '0'", "is '" + RUNTIME + "'");
	}

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

	@Test void b01_render_omitted_injectsRuntimeVersion() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class); var rsp = c.get("/t/cv-omit").run()) {
			rsp.assertStatus(200);
			assertTrue(rsp.getContent().asString().contains("\"contractVersion\":\"" + RUNTIME + "\""));
		}
	}

	@Test void b02_render_matching_ok() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class); var rsp = c.get("/t/cv-pinned-ok").run()) {
			rsp.assertStatus(200);
		}
	}

	@Test void b03_render_mismatched_failsInDevMode() throws Exception {
		try (var c = MockRestClient.buildLax(DevHost.class); var rsp = c.get("/t/cv-pinned-stale").run()) {
			rsp.assertStatus(500);
			assertContainsAll(rsp.getContent().asString(), "c1/cv-pinned-stale.ftlh", "is '4'", "is '" + RUNTIME + "'");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static validator rule
	//-----------------------------------------------------------------------------------------------------------------

	private static List<ConsoleTemplateValidator.Finding> lint(String source) {
		// Only the version rule is under test; the fixtures' pre-built view objects also trip the unrelated inline-slot-meta rule.
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
