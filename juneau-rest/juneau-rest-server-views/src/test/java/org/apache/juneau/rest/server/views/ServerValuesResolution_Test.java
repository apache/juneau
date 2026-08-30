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
import static org.apache.juneau.bean.html5.HtmlBuilder.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;

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
 * Serve-time {@code $FV} resolution through the {@link ViewTable} emit path (W1): resolved chrome in painted
 * {@code <th>} and the VIEW_META sidecar, XSS/escaping, per-response isolation, and expand-JSON data staying
 * unresolved.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class ServerValuesResolution_Test extends TestBase {

	private static final String XSS = "<script>alert(1)</script> a&b \"q\"";

	/** A shared view instance so the per-response restore (no permanent overwrite of author templates) is exercised. */
	static final ViewDef VIEW = ViewDef.create("releases")
		.columns(
			Column.of("count").title("Failures ($FV{failedCount})"),
			Column.of("bad").title("$FV{xss}"),
			Column.of("miss").title("$FV{missing,Fallback}"),
			Column.of("plain").title("Plain"))
		.serverValues(ServerValues.create()
			.value("failedCount", s -> s.getBean(RestRequest.class).map(r -> r.getQueryParam("n").orElse("0")).orElse("0"))
			.value("xss", s -> XSS))
		.build();

	static ViewDef fixedView(String id, int count) {
		return ViewDef.create(id)
			.columns(Column.of("count").title("Failures ($FV{failedCount})"))
			.serverValues(ServerValues.create().value("failedCount", s -> count))
			.build();
	}

	/**
	 * Counts how often the row-detail host asks for its popover titles.  A detail field's popover is reachable
	 * chrome but has no painted output in the emitted {@code <template>} today, so the provider call is the only
	 * externally observable evidence that the traversal descends into it (see {@code b03}).
	 */
	static final AtomicInteger DETAIL_POPOVER_READS = new AtomicInteger();

	/**
	 * The chrome fields reached beyond a column's own title: a row action's confirmation prompt, and the two title
	 * strings a cell popover owns, hung off both a {@link Column}'s and a {@link DetailField}'s {@code render}.
	 * The row-detail host declares its own {@link ServerValues} because it resolves against its own session.
	 */
	static final ViewDef WIDENED = ViewDef.create("widened")
		.columns(
			Column.of("id").title("Id"),
			Column.of("used").render(Render.of("progress").popover(
				CellPopover.of(PopoverField.of("actual").title("$FV{fieldTitle}")).title("$FV{popoverTitle}"))))
		.rowActions(RowAction.create("ack").label("Acknowledge").endpoint("/data/{id}/ack")
			.method(RowAction.Method.POST).confirm("$FV{confirmPrompt}"))
		.details(RowDetailDef.create()
			.endpoint("/data/{id}")
			.title("Detail")
			.serverValues(ServerValues.create()
				.value("detailPopoverTitle", s -> { DETAIL_POPOVER_READS.incrementAndGet(); return "Note detail"; })
				.value("detailFieldTitle", s -> { DETAIL_POPOVER_READS.incrementAndGet(); return "Note text"; }))
			.sections(DetailSection.create("main", "Main").fields(
				DetailField.of("actual").title("Actual").render(Render.of("bool").popover(
					CellPopover.of(PopoverField.of("note").title("$FV{detailFieldTitle}"))
						.title("$FV{detailPopoverTitle}"))))))
		.serverValues(ServerValues.create()
			.value("confirmPrompt", s -> "Acknowledge this incident?")
			.value("popoverTitle", s -> "Usage detail")
			.value("fieldTitle", s -> "Actual usage"))
		.build();

	@Rest(mixins=ViewsMixin.class)
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@Bean public VarResolver varResolver(VarResolver.Builder b) {
			return b.vars(ServerValuesVar.class).build();
		}

		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return html(Html.of(ViewTable.of(req, VIEW)));
		}

		@RestGet(path="/two") public HttpResource two(RestRequest req) {
			return html(Html.of(div(ViewTable.of(req, fixedView("a", 1)), ViewTable.of(req, fixedView("b", 2)))));
		}

		@RestGet(path="/rows") public HttpResource rows(RestRequest req) {
			var data = List.<Object>of(map("count", "$FV{failedCount}", "bad", "x", "miss", "y", "plain", "z"));
			return html(Html.of(ViewTable.of(req, VIEW, data)));
		}

		@RestGet(path="/widened") public HttpResource widened(RestRequest req) {
			return html(Html.of(ViewTable.of(req, WIDENED)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(Host.class);

	private static String body(String path) throws Exception {
		return c.get(path).run().assertStatus(200).getContent().asString();
	}

	/** Extracts the raw text between the named sidecar's opening and closing {@code <script>} tags. */
	private static String sidecarBody(String html, String viewId) {
		var open = html.indexOf("id=\"juneau-view:" + viewId + "\"");
		assertTrue(open >= 0, () -> "sidecar script tag not found:\n" + html);
		var contentStart = html.indexOf('>', open) + 1;
		var contentEnd = html.indexOf("</script>", contentStart);
		return html.substring(contentStart, contentEnd);
	}

	@Test void a01_resolvedScalar_inPaintedTh() throws Exception {
		var html = body("/view?n=7");
		assertTrue(html.contains("Failures (7)"), () -> html);
		assertFalse(html.contains("$FV{failedCount}"), () -> html);
	}

	@Test void a02_missingName_usesDefaultInChrome() throws Exception {
		var html = body("/view?n=7");
		assertTrue(html.contains("Fallback"), () -> html);
		assertFalse(html.contains("$FV{missing"), () -> html);
	}

	@Test void a03_xss_elementText_serializerEncoded() throws Exception {
		var html = body("/view?n=7");
		// No raw injection anywhere (painted th is entity-encoded; sidecar is escapeForScript-neutralized).
		assertFalse(html.contains("<script>alert(1)"), () -> html);
		// Entity-encoded element text, and NOT double-encoded.
		assertTrue(html.contains("&lt;script&gt;alert(1)"), () -> html);
		assertFalse(html.contains("&amp;lt;script"), () -> html);
	}

	@Test void a04_viewMeta_carriesResolvedChrome() throws Exception {
		var sidecar = sidecarBody(body("/view?n=7"), "releases");
		assertTrue(sidecar.contains("Failures (7)"), () -> sidecar);
		assertTrue(sidecar.contains("Fallback"), () -> sidecar);
		assertFalse(sidecar.contains("$FV{"), () -> sidecar);
	}

	@Test void a05_viewMeta_scriptBreakoutNeutralized() throws Exception {
		var sidecar = sidecarBody(body("/view?n=7"), "releases");
		// The resolved XSS title lands in the raw-text sidecar; escapeForScript must neutralize the </script> breakout.
		assertTrue(sidecar.contains("alert(1)"), () -> sidecar);
		assertFalse(sidecar.contains("</script>"), () -> sidecar);
	}

	@Test void a06_twoWidgets_oneResponse_noLeak() throws Exception {
		var html = body("/two");
		assertTrue(html.contains("Failures (1)"), () -> html);
		assertTrue(html.contains("Failures (2)"), () -> html);
	}

	@Test void a07_twoSequentialRequests_perResponseSnapshot() throws Exception {
		assertTrue(body("/view?n=3").contains("Failures (3)"));
		// The shared VIEW instance must have been restored after the first response; the second resolves freshly.
		var html = body("/view?n=9");
		assertTrue(html.contains("Failures (9)"), () -> html);
		assertFalse(html.contains("Failures (3)"), () -> html);
	}

	@Test void a08_expandData_notResolved() throws Exception {
		// A row cell that literally contains a $FV token is row data, never passed through VarResolver.
		var html = body("/rows?n=7");
		assertTrue(html.contains("$FV{failedCount}"), () -> html);
	}

	@Test void b01_rowActionConfirm_resolvedIntoViewMeta() throws Exception {
		var sidecar = sidecarBody(body("/widened"), "widened");
		assertTrue(sidecar.contains("Acknowledge this incident?"), () -> sidecar);
		assertFalse(sidecar.contains("$FV{confirmPrompt}"), () -> sidecar);
	}

	@Test void b02_columnPopoverTitles_resolvedIntoViewMeta() throws Exception {
		var sidecar = sidecarBody(body("/widened"), "widened");
		assertTrue(sidecar.contains("Usage detail"), () -> sidecar);
		assertTrue(sidecar.contains("Actual usage"), () -> sidecar);
		assertFalse(sidecar.contains("$FV{popoverTitle}"), () -> sidecar);
		assertFalse(sidecar.contains("$FV{fieldTitle}"), () -> sidecar);
	}

	@Test void b03_detailFieldPopover_isReachedByTheRowDetailHostsOwnWalk() throws Exception {
		// A detail field's popover has nothing painted into the emitted <template> today, so its resolution is
		// observable only through the provider being asked for both titles.
		DETAIL_POPOVER_READS.set(0);
		body("/widened");
		assertEquals(2, DETAIL_POPOVER_READS.get());
	}

	@Test void b04_widenedChrome_restoredAfterTheResponse() throws Exception {
		body("/widened");
		// Every shared def must be back on its author templates once the response is written.
		assertEquals("$FV{confirmPrompt}", WIDENED.rowActions.get(0).confirm);
		assertEquals("$FV{popoverTitle}", WIDENED.columns.get(1).render.popover.title);
		assertEquals("$FV{fieldTitle}", WIDENED.columns.get(1).render.popover.fields.get(0).title);
		var detailPopover = WIDENED.details.sections.get(0).fields.get(0).render.popover;
		assertEquals("$FV{detailPopoverTitle}", detailPopover.title);
		assertEquals("$FV{detailFieldTitle}", detailPopover.fields.get(0).title);
	}

	@Test void b05_popoverWithNoTemplate_isNeverMutated() {
		// A $-free popover title costs nothing: no session, no resolve, no restore, byte-identical markup.
		var literal = ViewDef.create("literal")
			.columns(Column.of("used").render(Render.of("progress").popover(
				CellPopover.of(PopoverField.of("actual").title("Actual")).title("Usage"))))
			.build();
		assertEquals(Html.of(ViewTable.of(literal)), Html.of(ViewTable.of(literal)));
		assertEquals("Usage", literal.columns.get(0).render.popover.title);
	}
}
