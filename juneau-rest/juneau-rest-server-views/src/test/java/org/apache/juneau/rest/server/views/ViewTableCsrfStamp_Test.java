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

import jakarta.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.MockServletRequest;  // Single-type import: the two mock packages both carry a MockRestClient.
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

/**
 * The value of the auto-embedded CSRF stamp ({@link ViewTable#CSRF_ATTR}), on both request flavors the
 * request-bearing {@code of(...)} overloads accept.
 *
 * <p>
 * The stamp must be the <b>bare token value</b> the boundary filter published under
 * {@link LoopbackBoundaryFilter#TOKEN_ATTRIBUTE} &mdash; byte-identical to what the {@code juneau-views.js} row-action
 * submit will put on the wire and what {@link LoopbackBoundary} compares against.  Anything else 403s every mutating
 * row action, which is precisely what a <b>presence</b>-only assertion cannot catch: a stamp carrying
 * {@code "<attribute-name>=<token>"} contains the token and is non-blank, so it satisfies both the runtime's
 * fail-closed check and any {@code contains(token)} test while still failing the server's comparison.  These tests
 * therefore assert equality, and assert the attribute <i>name</i> never appears in the markup at all.
 *
 * <p>
 * The two flavors matter independently because {@link RestRequest#getAttribute(String)} covariantly returns a
 * {@link org.apache.juneau.rest.server.httppart.RequestAttribute} wrapper rather than the raw value, so a
 * {@code HttpServletRequest}-declared read behaves differently depending on which one is actually passed.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixture held in a static field; lifecycle managed by the test/framework.
})
class ViewTableCsrfStamp_Test extends TestBase {

	/** A token of the shape {@link SynchronizerToken} mints: 64 hex characters, no separators. */
	private static final String TOKEN = "3f1c8a2b4d6e0f9a7c5b3d1e8f2a4c6b0d9e7f5a3c1b8d6e4f2a0c9b7d5e3f1c";

	private static ViewDef view() {
		return ViewDef.create("orders")
			.columns(Column.of("ref").title("Ref"))
			.build();
	}

	@Rest(mixins=ViewsMixin.class)
	public static class CsrfStampHost extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/table") public HttpResource table(RestRequest req) {
			req.setAttribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
			return html(Html.of(ViewTable.of(req, view())));
		}

		@RestGet(path="/tokenless") public HttpResource tokenless(RestRequest req) {
			return html(Html.of(ViewTable.of(req, view())));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(CsrfStampHost.class);

	private static String body(String path) throws Exception {
		return c.get(path).accept("text/html").run().assertStatus(200).getContent().asString();
	}

	/** The text of the CSRF stamp, or {@code null} when the markup carries no stamp at all. */
	private static String stamp(String html) {
		var marker = ViewTable.CSRF_ATTR + "=\"";
		var at = html.indexOf(marker);
		if (at < 0)
			return null;
		var from = at + marker.length();
		return html.substring(from, html.indexOf('"', from));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) The stamp is the token value itself
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_restRequestStampIsTheBareToken() throws Exception {
		var html = body("/table");
		assertEquals(TOKEN, stamp(html), () -> "the CSRF stamp must be the token itself:\n" + html);
	}

	@Test void a02_restRequestStampNeverCarriesTheAttributeName() throws Exception {
		// A "name=value" stamp would still contain the token, so the token-bearing assertion above is not enough
		// on its own: the attribute name is request plumbing and has no business in the rendered markup.
		var html = body("/table");
		assertFalse(html.contains(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE), html);
	}

	@Test void a03_plainServletRequestStampIsTheBareToken() {
		HttpServletRequest req = MockServletRequest.create().attribute(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE, TOKEN);
		var html = Html.of(ViewTable.of(req, view()));
		assertEquals(TOKEN, stamp(html), () -> "the CSRF stamp must be the token itself:\n" + html);
		assertFalse(html.contains(LoopbackBoundaryFilter.TOKEN_ATTRIBUTE), html);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) No token means no stamp, so the runtime can fail closed
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_restRequestWithNoTokenStampsNothing() throws Exception {
		// The runtime refuses a row action only when the attribute is absent/blank; a placeholder stamp would send
		// a token the boundary is guaranteed to reject instead of refusing visibly on the client.
		var html = body("/tokenless");
		assertNull(stamp(html), () -> "a request carrying no boundary token must stamp no CSRF attribute:\n" + html);
		assertFalse(html.contains(ViewTable.CSRF_ATTR), html);
	}

	@Test void b02_plainServletRequestWithNoTokenStampsNothing() {
		HttpServletRequest req = MockServletRequest.create();
		var html = Html.of(ViewTable.of(req, view()));
		assertFalse(html.contains(ViewTable.CSRF_ATTR), html);
	}
}
