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

import static org.apache.juneau.rest.server.views.ConsoleBrowserFixture.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * {@code juneau-joblog.js} in a real browser: a real form submit and {@code fetch} POST, progress lines from a fake
 * {@code EventSource}, the success banner, the URL param written with {@code history.replaceState}, and reload
 * re-attach from stored prefs.
 */
@EnabledIfSystemProperty(named=ConsoleBrowserFixture.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -pl juneau-rest/juneau-rest-server-views -am test` from the repo root")
class JobLogCard_BrowserTest extends TestBase {

	private static final String JOBLOG = "/juneau-console/juneau-joblog.js";
	private static final String REF = "{\"jobId\":\"j1\",\"streamUrl\":\"/jobs/j1/stream\",\"cancelUrl\":\"/jobs/j1/cancel\"}";
	private static final String CARD = "{\"id\":\"rebuild\",\"type\":\"job-log\",\"title\":\"Rebuild\",\"urlParam\":\"job\","
		+ "\"submit\":{\"endpoint\":\"/jobs/rebuild\",\"submitLabel\":\"Rebuild\",\"fields\":[{\"name\":\"reason\",\"label\":\"Reason\",\"type\":\"text\",\"required\":true}]}}";
	private static final String CONTRACT = "{\"contractVersion\":\"1\",\"title\":\"T\",\"cards\":[" + CARD + "]}";

	/** Fake EventSource + a fetch spy; installed before the shell. */
	private static final String BEFORE = """
		<script>
		window.__probe = { posts: [], get search() { return location.search; },
		  get lines() { return Array.from(document.querySelectorAll('#rebuild .jc-joblog-line')).map(function (n) { return n.textContent; }); } };
		var realFetch = window.fetch;
		window.fetch = function (url, init) {
		  if (init && init.method === 'POST') window.__probe.posts.push({ url: String(url), body: JSON.parse(init.body) });
		  return realFetch.apply(this, arguments);
		};
		window.EventSource = function (url) {
		  var self = this, listeners = {};
		  window.__probe.stream = String(url);
		  this.readyState = 1;
		  this.addEventListener = function (t, fn) { (listeners[t] = listeners[t] || []).push(fn); };
		  this.close = function () { self.readyState = 2; };
		  function emit(t, data) { (listeners[t] || []).forEach(function (fn) { fn({ type: t, data: data }); }); }
		  setTimeout(function () {
		    emit('progress', 'step 1');
		    emit('progress', 'step 2');
		    emit('result', JSON.stringify({ outcome: 'success', message: 'Rebuilt.', contractVersion: '1' }));
		  }, 50);
		};
		</script>
		""";

	/** Fills the required field and clicks the real submit button once the card has rendered. */
	private static final String AUTO_SUBMIT = """
		<script>
		(function go() {
		  var i = document.querySelector('#rebuild .jc-joblog-input'), b = document.querySelector('#rebuild .jc-joblog-submit');
		  if (!i || !b) { setTimeout(go, 20); return; }
		  i.value = 'nightly';
		  b.click();
		})();
		</script>
		""";

	private static final String SEED = """
		<script>
		localStorage.setItem('juneau-card:rebuild:job', JSON.stringify(%s));
		</script>
		""".formatted(REF);

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void setup() throws Exception {
		var after = "<script src=\"" + JOBLOG + "\"></script>";
		report = ConsoleBrowserFixture.create("job-log-card")
			.asset(JOBLOG, resource(ConsoleChromeMixin.JOBLOG_JS_RESOURCE), "text/javascript")
			.asset("/jobs/rebuild", REF, "application/json")
			.page("submit", ORIGIN + "/jobs",
				contractPage(CONTRACT, "", BEFORE, after + AUTO_SUBMIT),
				"banner", "#rebuild .jc-joblog-banner")
			.waitFor("#rebuild .jc-joblog-ok")
			.page("reattach", ORIGIN + "/jobs?job=j1",
				contractPage(CONTRACT, "", SEED + BEFORE, after),
				"banner", "#rebuild .jc-joblog-banner")
			.waitFor("#rebuild .jc-joblog-ok")
			.run();
	}

	@Test void a01_submit_streamsToSuccess() {
		var r = report.get("submit");
		assertNoShellErrors(r);
		assertEquals("Done. Rebuilt.", queries(r).get("banner"));
		var p = probe(r);
		assertEquals(List.of("step 1", "step 2"), p.get("lines"));
		assertEquals("/jobs/j1/stream", p.get("stream"));
		assertEquals("?job=j1", p.get("search"));
		var posts = (List<?>)p.get("posts");
		assertEquals(1, posts.size());
		var post = (Map<?,?>)posts.get(0);
		assertEquals("/jobs/rebuild", post.get("url"));
		var body = (Map<?,?>)post.get("body");
		assertEquals("nightly", body.get("reason"));
		assertTrue(String.valueOf(body.get("idempotencyKey")).length() >= 8, () -> String.valueOf(body));
	}

	@Test void a02_reload_reattachesWithoutSubmitting() {
		var r = report.get("reattach");
		assertNoShellErrors(r);
		assertEquals("Done. Rebuilt.", queries(r).get("banner"));
		var p = probe(r);
		assertEquals("/jobs/j1/stream", p.get("stream"));
		assertEquals(List.of(), p.get("posts"));
		assertEquals(List.of("step 1", "step 2"), p.get("lines"));
	}
}
