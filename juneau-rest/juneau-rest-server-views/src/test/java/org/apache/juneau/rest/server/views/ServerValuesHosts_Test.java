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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * Serve-time {@code $FV} resolution for the {@link RowDetailDef} host, alongside the shipped {@link ViewDef} host:
 * the closed chrome allowlists, per-host isolation, LIFO restore fidelity on the shared definitions, fail-soft /
 * fail-closed, and the expand envelope staying chrome-free.
 *
 * <p>
 * Every definition below is a <b>shared static instance</b>, which is the realistic application pattern and the one
 * that exercises the mutate-and-restore window rather than a fresh per-request object graph.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class ServerValuesHosts_Test extends TestBase {

	/** Reads the {@code env} query param so each response resolves to its own value. */
	private static ServerValues envValues(String suffix) {
		return ServerValues.create()
			.value("env", s -> s.getBean(RestRequest.class).map(r -> r.getQueryParam("env").orElse("?")).orElse("?")
				+ suffix);
	}

	/** The row-detail host: {@code title} is on the allowlist. */
	static final RowDetailDef DETAIL = RowDetailDef.create()
		.endpoint("/alerts/{id}/detail")
		.title("D-title:$FV{env}")
		.icon("D-icon:$FV{env}")           // NOT on the allowlist - must stay literal
		.region(RegionDef.create("d").allowPopulators("p").populate("p"))
		.serverValues(envValues("/DETAIL"));

	/** The view host (the shipped v1 host), which must resolve independently of the detail host. */
	static final ViewDef ALERTS = ViewDef.create("alerts")
		.columns(Column.of("name").title("V-col:$FV{env}"))
		.details(DETAIL)
		.serverValues(envValues("/VIEW"))
		.build();

	static final RowDetailDef THROWING_DETAIL = RowDetailDef.create()
		.endpoint("/boom/{id}")
		.title("T:$FV{bad}")
		.region(RegionDef.create("d").allowPopulators("p").populate("p"))
		.serverValues(ServerValues.create().value("bad", s -> {
			throw new IllegalStateException("provider blew up");
		}));

	static final ViewDef THROWING_DETAIL_VIEW = ViewDef.create("boom-detail")
		.columns(Column.of("name").title("Name"))
		.details(THROWING_DETAIL)
		.build();

	@Rest(mixins=ViewsMixin.class)
	public static class ServerValuesHostsHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return html(Html.of(ViewTable.of(req, ALERTS)));
		}

		/** The app-owned expand GET: data only, no chrome (umbrella decision 9). */
		@RestGet(path="/alerts/{id}/detail") public Map<String,Object> detail(@Path("id") String id) {
			var out = new LinkedHashMap<String,Object>();
			out.put("contractVersion", RowDetailDef.CONTRACT_VERSION);
			out.put("fields", Map.of("k", "row-value-" + id));
			return out;
		}

		@RestGet(path="/detail-throws") public HttpResource detailThrows(RestRequest req) {
			return html(Html.of(ViewTable.of(req, THROWING_DETAIL_VIEW)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(ServerValuesHostsHost.class);

	private static String body(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	private static List<String> authorTemplates() {
		return list(DETAIL.title, DETAIL.icon, ALERTS.columns.get(0).title);
	}

	/** A declaration whose provider map holds a null entry - only reachable through {@code values(Map)}. */
	private static ServerValues brokenValues() {
		var m = new LinkedHashMap<String,ServerValuesValue>();
		m.put("x", null);
		return ServerValues.create().values(m);
	}

	@Test void a02_rowDetailDefValidate_cascadesIntoServerValuesValidate() {
		var d = RowDetailDef.create()
			.endpoint("/x/{id}")
			.region(RegionDef.create("d").allowPopulators("p").populate("p"))
			.serverValues(brokenValues());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ServerValues"), e.getMessage());
	}

	@Test void a03_wellFormedHostsValidateCleanly() {
		assertDoesNotThrow(() -> DETAIL.validate(null, "alerts"));
	}

	@Test void d01_detailTitleResolves() throws Exception {
		var html = body("/view?env=A");
		assertTrue(html.contains("D-title:A/DETAIL"), html);
	}

	@Test void d02_detailIcon_isNotOnTheAllowlist_staysLiteral() throws Exception {
		var html = body("/view?env=A");
		assertTrue(html.contains("D-icon:$FV{env}"), "RowDetailDef.icon must not be interpolated");
	}

	@Test void d03_detailHostAlsoResolvesForAStandaloneViewTable() throws Exception {
		var html = body("/view?env=Z");
		assertTrue(html.contains("D-title:Z/DETAIL"), html);
	}

	@Test void d04_detailTitleTemplateAttributeCarriesTheResolvedString() throws Exception {
		var html = body("/view?env=A");
		assertTrue(html.contains(ViewTable.DETAIL_TITLE_TEMPLATE_ATTR + "=\"D-title:A/DETAIL\""), html);
	}

	@Test void e01_sameNameAcrossHostsResolvesIndependently() throws Exception {
		var html = body("/view?env=A");
		assertTrue(html.contains("D-title:A/DETAIL"), "row-detail host");
		assertTrue(html.contains("V-col:A/VIEW"), "view host");
	}

	@Test void f01_everyAuthorTemplateIsByteIdenticalAfterAFullRender() throws Exception {
		var before = authorTemplates();
		body("/view?env=A");
		assertEquals(before, authorTemplates(), "author $FV templates must be restored byte-identically");
		assertTrue(before.contains("D-title:$FV{env}"), before::toString);
	}

	@Test void f02_twoSequentialRequestsResolveFreshly_noCarryOver() throws Exception {
		var first = body("/view?env=A");
		var second = body("/view?env=B");
		assertTrue(first.contains("D-title:A/DETAIL"), first);
		assertTrue(second.contains("D-title:B/DETAIL"), second);
		assertFalse(second.contains("D-title:A/DETAIL"), "the first response's resolved chrome must not survive");
	}

	@Test void g01_unknownName_isFailSoft_onTheRowDetailHost() {
		var d = RowDetailDef.create()
			.endpoint("/x/{id}")
			.title("$FV{nope}")
			.region(RegionDef.create("d").allowPopulators("p").populate("p"))
			.serverValues(ServerValues.create().value("known", s -> "k"));
		assertDoesNotThrow(() -> d.validate(null));
	}

	@Test void g04_throwingProvider_onTheRowDetailHost_failsClosed_andStillRestores() throws Exception {
		c.get("/detail-throws").run().assertStatus(500);
		assertEquals("T:$FV{bad}", THROWING_DETAIL.title, "the restore must run even when a provider throws");
	}

	@Test void h01_expandEnvelopeCarriesNoFetchedValueTemplate() throws Exception {
		var json = c.get("/alerts/a1/detail").accept("application/json").run().assertStatus(200)
			.getContent().asString();
		assertFalse(json.contains("$FV{"), () -> json);
	}

	@Test void h02_expandEnvelopeCarriesNoDetailChromeAtAll() throws Exception {
		var json = c.get("/alerts/a1/detail").accept("application/json").run().assertStatus(200)
			.getContent().asString();
		assertFalse(json.contains("D-title"), () -> json);
		assertTrue(json.contains("row-value-a1"), () -> json);
	}

	@Test void i02_rowDetailContractUnchangedByThisSlice() {
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void i03_serverValuesStayOffTheWire() throws Exception {
		var html = body("/view?env=A");
		assertFalse(html.contains("serverValues"), "ServerValues is Java-only and must never marshal");
	}
}
