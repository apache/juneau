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

import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.resource.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;
import org.junit.jupiter.api.*;

/**
 * {@link CalendarTable#of(RestRequest, CalendarDef)} / {@link CalendarTable#of(RestRequest, CalendarDef, Clock)}
 * localize the month/year title against {@link RestRequest#getLocale()} (TODO-J0485).
 *
 * <p>
 * The request-free {@link CalendarTable#of(CalendarDef, Clock)} path is covered (and pinned to
 * {@code Locale.ENGLISH}, byte-stable) by {@code CalendarDef_Serving_Test}; this class exercises only the new
 * request-aware overloads, which resolve the title's locale from the request's {@code Accept-Language} header
 * instead.
 */
@SuppressWarnings({
	"resource"  // Closeable test fixtures held in static fields; lifecycle managed by the test/framework.
})
class CalendarTableRequestAware_Test extends TestBase {

	/** A fixed August-2026 clock so the title is deterministic. */
	private static final Clock AUG_2026 = Clock.fixed(Instant.parse("2026-08-15T12:00:00Z"), ZoneOffset.UTC);

	private static CalendarDef good() {
		return CalendarDef.create()
			.id("cal1")
			.categories(EventCategory.create().id("team").label("Team").color(CategoryColor.BLUE))
			.initial(2026, 8)
			.events(CalendarEvent.create().id("e1").title("Team offsite").start("2026-08-14").categoryId("team"));
	}

	@Rest
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/cal") public HttpResource cal(RestRequest req) {
			return html(Html.of(CalendarTable.of(req, good(), AUG_2026)));
		}

		private static HttpResource html(String markup) {
			return HttpResourceBean.of(
				ByteArrayBody.of(markup.getBytes(UTF_8), "text/html;charset=utf-8"),
				list(ContentType.of("text/html;charset=utf-8")));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(Host.class);

	@Test void a01_frenchAcceptLanguage_localizesTheMonthTitle() throws Exception {
		var html = c.get("/cal").header("Accept-Language", "fr").accept("text/html").run()
			.assertStatus(200).getContent().asString();
		assertTrue(html.contains("août 2026"), html);
		assertFalse(html.contains("August 2026"), html);
	}

	@Test void a02_noAcceptLanguageHeader_stillRenders() throws Exception {
		// No assertion on which locale wins here (that is the servlet container's own default-locale policy) -
		// only that the request-aware overload does not blow up when the request carries no explicit preference.
		var html = c.get("/cal").accept("text/html").run().assertStatus(200).getContent().asString();
		assertTrue(html.contains("data-juneau-calendar-title"), html);
	}

	@Test void b01_englishAcceptLanguage_rendersTheSameTitleAsTheRequestFreePath() throws Exception {
		var html = c.get("/cal").header("Accept-Language", "en").accept("text/html").run()
			.assertStatus(200).getContent().asString();
		assertTrue(html.contains("August 2026"), html);
	}
}
