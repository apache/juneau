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

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Serve-time {@code $L} resolution through the {@link ViewTable}/{@link PageTable} chrome-resolution path
 * (view-def string i18n, READY-J0483): the gate decoupled from {@code serverValues} (LD-1), the LD-2 non-recursive
 * scoping, {@link Column#titleKey} sugar (LD-3), and the request-free {@link Messages}-bean seam (LD-4).
 *
 * <p>
 * {@code LocalizationChromeResolutionHost.properties} (same package, {@code src/test/resources}) backs
 * {@code RestRequest.getMessages()} and
 * the request-free {@link Messages#of(Class)} lookups below: {@code col.name=Name}, {@code col.status=Status},
 * {@code col.a=$L{col.b}}, {@code col.b=Leaf}, {@code page.title=Releases Page}, {@code tab.main=Main}.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class LocalizationChromeResolution_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Shared definitions.  None declares serverValues: every $L resolution below exercises LD-1's decoupled gate.
	//------------------------------------------------------------------------------------------------------------------

	/** The motivating shape: {@link Column#titleKey} sugar alongside the raw {@code $L{...}} longhand. */
	static final ViewDef VIEW = ViewDef.create("releases")
		.columns(
			Column.of("name").titleKey("col.name"),
			Column.of("status").title("$L{col.status}"),
			Column.of("plain").title("Plain"),
			Column.of("chain").title("$L{col.a}"))    // LD-2: must NOT recursively resolve to "Leaf".
		.build();

	static final ViewDef DETAILED = ViewDef.create("detailed")
		.columns(Column.of("name").title("Plain"))
		.details(RowDetailDef.create()
			.endpoint("/detailed/{id}")
			.title("$L{col.name}")
			.sections(DetailSection.create("main", "$L{col.status}").fields(DetailField.of("k").title("$L{col.name}"))))
		.build();

	static final PageDef PAGE = PageDef.create("admin")
		.title("$L{page.title}")
		.tabs(Tab.create("main", "$L{tab.main}").view(VIEW))
		.build();

	//------------------------------------------------------------------------------------------------------------------
	// Host servlet.  No @Bean varResolver override: $L (LocalizationVar) is already part of the default REST var
	// set (unlike $FV/ServerValuesVar, which every ServerValues*_Test host must opt in explicitly).
	//------------------------------------------------------------------------------------------------------------------

	@Rest(mixins=ViewsMixin.class)
	public static class LocalizationChromeResolutionHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/view") public HttpResource view(RestRequest req) {
			return html(Html.of(ViewTable.of(req, VIEW)));
		}

		@RestGet(path="/detailed") public HttpResource detailed(RestRequest req) {
			return html(Html.of(ViewTable.of(req, DETAILED)));
		}

		@RestGet(path="/page") public HttpResource page(RestRequest req) {
			return html(Html.of(PageTable.of(req, PAGE)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(LocalizationChromeResolutionHost.class);

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

	//------------------------------------------------------------------------------------------------------------------
	// LD-1: the gate is decoupled from serverValues - none of VIEW/DETAILED/PAGE declares one, yet $L still resolves.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_titleKey_resolvesThroughRestRequestMessages() throws Exception {
		var html = body("/view");
		assertTrue(html.contains("<th>Name</th>"), html);
		assertFalse(html.contains("$L{col.name}"), html);
	}

	@Test void a02_rawLTemplate_resolvesIdenticallyToTitleKeySugar() throws Exception {
		var html = body("/view");
		assertTrue(html.contains("<th>Status</th>"), html);
		assertFalse(html.contains("$L{col.status}"), html);
	}

	@Test void a03_viewMeta_carriesTheSameResolvedChromeAsThePaintedTh() throws Exception {
		var sidecar = sidecarBody(body("/view"), "releases");
		assertTrue(sidecar.contains("\"title\":\"Name\""), sidecar);
		assertTrue(sidecar.contains("\"title\":\"Status\""), sidecar);
		// Neither of the two author templates that should have resolved is still present literally.  (The sidecar
		// legitimately DOES still contain a "$L{" substring - see b02 - from the "chain" column's LD-2 non-recursive
		// value, so this cannot assert a blanket absence of "$L{" the way a04 of ServerValuesResolution_Test does.)
		assertFalse(sidecar.contains("$L{col.name}"), sidecar);
		assertFalse(sidecar.contains("$L{col.status}"), sidecar);
	}

	@Test void a04_plainTitle_isUnaffected() throws Exception {
		var html = body("/view");
		assertTrue(html.contains("<th>Plain</th>"), html);
	}

	@Test void a05_rowDetailHost_resolvesItsOwnLTemplatesIndependently() throws Exception {
		// The row-detail host (ViewTable's OTHER gate site) - a second, independent decoupled gate.
		var html = body("/detailed");
		assertTrue(html.contains("Name"), html);
		assertTrue(html.contains("Status"), html);
		assertFalse(html.contains("$L{col.name}"), html);
		assertFalse(html.contains("$L{col.status}"), html);
	}

	@Test void a06_pageHost_resolvesItsOwnLTemplates() throws Exception {
		// PageTable's gate site: the painted tab label, again with no serverValues declared anywhere.  (PageDef#title
		// is on the same resolveChrome/pageChromeHasVar allowlist but is never painted into the shell or PAGE_META
		// by this emitter - see PageTable_Emit_Test's b03 "if rendered" caveat - so it has nothing externally
		// observable to assert here; its presence in PAGE still exercises that allowlist entry's resolve/restore.)
		var html = body("/page");
		assertTrue(html.contains("Main"), html);
		assertFalse(html.contains("$L{tab.main}"), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// LD-2 (security): $L resolved through the chrome path is non-recursive - a resolved value that itself looks
	// like a $L{...} template is emitted literally, never re-parsed as SVL.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_resolvedBundleValue_isNotRecursivelyReResolved() throws Exception {
		var html = body("/view");
		// col.a's bundle value is the literal string "$L{col.b}"; if recursion were allowed this would further
		// resolve to "Leaf" (col.b's own value).  It must not.
		assertTrue(html.contains("<th>$L{col.b}</th>"), html);
		assertFalse(html.contains("<th>Leaf</th>"), html);
	}

	@Test void b02_viewMeta_alsoStaysNonRecursive() throws Exception {
		var sidecar = sidecarBody(body("/view"), "releases");
		assertTrue(sidecar.contains("$L{col.b}"), sidecar);
		assertFalse(sidecar.contains("Leaf"), sidecar);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Byte stability: with no RestRequest and no bound Messages, $L{...} stays exactly literal - the decoupled gate
	// does not force a failing resolve attempt when there is no context to resolve against.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_noRequestNoMessages_staysLiteral() {
		var html = Html.of(ViewTable.of(VIEW));
		assertTrue(html.contains("$L{col.name}"), html);
		assertTrue(html.contains("$L{col.status}"), html);
	}

	@Test void c02_defWithNoDollar_isByteIdentical_withAndWithoutAMessagesBean() {
		var literalOnly = ViewDef.create("literal").columns(Column.of("name").title("Plain")).build();
		assertEquals(Html.of(ViewTable.of(literalOnly)), Html.of(ViewTable.of(messages(), literalOnly)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// LD-4: the request-free Messages-bean seam.  No RestRequest anywhere on this path.
	//------------------------------------------------------------------------------------------------------------------

	private static Messages messages() {
		return Messages.of(LocalizationChromeResolutionHost.class);
	}

	@Test void d01_messagesBean_resolvesLWithNoRequest() {
		var html = Html.of(ViewTable.of(messages(), VIEW));
		assertTrue(html.contains("<th>Name</th>"), html);
		assertTrue(html.contains("<th>Status</th>"), html);
		assertFalse(html.contains("$L{col.name}"), html);
	}

	@Test void d02_messagesBean_alsoNonRecursive() {
		var html = Html.of(ViewTable.of(messages(), VIEW));
		assertTrue(html.contains("<th>$L{col.b}</th>"), html);
		assertFalse(html.contains("<th>Leaf</th>"), html);
	}

	@Test void d03_nullMessages_isExactlyTheNoRequestOverload() {
		assertEquals(Html.of(ViewTable.of(VIEW)), Html.of(ViewTable.of((Messages) null, VIEW)));
	}

	@Test void d04_pageHost_messagesBean_resolvesWithNoRequest() {
		var html = Html.of(PageTable.of(messages(), PAGE));
		assertTrue(html.contains("Main"), html);
		assertFalse(html.contains("$L{tab.main}"), html);
		// The request-free page overload does not propagate into child views (see PageTable#of(Messages,PageDef)
		// javadoc): the tab's own hosted ViewTable renders exactly as request-free/messages-free as of(PageDef).
		assertTrue(html.contains("$L{col.name}"), html);
	}

	@Test void d05_pageHost_nullMessages_isExactlyTheNoRequestOverload() {
		assertEquals(Html.of(PageTable.of(PAGE)), Html.of(PageTable.of((Messages) null, PAGE)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// LD-3: Column.titleKey is thin sugar over title("$L{" + key + "}") - no new field, same resolution.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_titleKey_storesTheRawLTemplateIntoTitle() {
		var col = Column.of("x").titleKey("some.key");
		assertEquals("$L{some.key}", col.title);
	}

	@Test void e02_titleKey_isEquivalentToTheRawLonghand() {
		var viaSugar = Column.of("x").titleKey("col.name");
		var viaRaw = Column.of("x").title("$L{col.name}");
		assertEquals(viaRaw.title, viaSugar.title);
	}
}
