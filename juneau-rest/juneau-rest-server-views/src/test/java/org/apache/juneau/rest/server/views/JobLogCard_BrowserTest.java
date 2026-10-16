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
 * {@code EventSource}, the success banner, the URL param written with {@code history.replaceState}, reload
 * re-attach from stored prefs, and a real {@code EventSource} that drops and resumes with {@code Last-Event-ID}.
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
		  if (init && init.method === 'POST') window.__probe.posts.push({ url: String(url), body: init.body ? JSON.parse(init.body) : null });
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

	/** Like {@link #BEFORE}, but the job never finishes on its own: it streams one line and waits to be cancelled. */
	private static final String BEFORE_HANGING = BEFORE.replace(
		"    emit('result', JSON.stringify({ outcome: 'success', message: 'Rebuilt.', contractVersion: '1' }));\n", "");

	/** Submits, then clicks the real Cancel button as soon as the job is running. */
	private static final String AUTO_CANCEL = """
		<script>
		(function go() {
		  var b = document.querySelector('#rebuild .jc-joblog[data-phase=running] .jc-joblog-submit');
		  if (!b || b.disabled || !document.querySelector('#rebuild .jc-joblog-line')) { setTimeout(go, 20); return; }
		  b.click();
		})();
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

	private static final String CANCEL_DONE = "{\"outcome\":\"cancelled\",\"contractVersion\":\"1\"}";

	private static final String SEED = """
		<script>
		localStorage.setItem('juneau-card:rebuild:job', JSON.stringify(%s));
		</script>
		""".formatted(REF);

	/** Probe only (no EventSource override), so Chromium's real EventSource talks to the sequenced stream. */
	private static final String PROBE_ONLY = """
		<script>
		window.__probe = { get lines() { return Array.from(document.querySelectorAll('#rebuild .jc-joblog-line')).map(function (n) { return n.textContent; }); } };
		</script>
		""";

	/** The first response ends after id 2 (a drop); the browser reconnects 50ms later with Last-Event-ID: 2. */
	private static final String STREAM_1 = "retry: 50\n\nid: 1\nevent: progress\ndata: p1\n\nid: 2\nevent: progress\ndata: p2\n\n";

	/** The resumed response: id 3, then the result, which closes the stream before this body ends. */
	private static final String STREAM_2 = "id: 3\nevent: progress\ndata: p3\n\n"
		+ "event: result\ndata: {\"outcome\":\"success\",\"message\":\"Rebuilt.\",\"contractVersion\":\"1\"}\n\n";

	private static Map<String,Map<String,Object>> report;

	@BeforeAll
	static void setup() throws Exception {
		var after = "<script src=\"" + JOBLOG + "\"></script>";
		report = ConsoleBrowserFixture.create("job-log-card")
			.asset(JOBLOG, resource(ConsoleChromeMixin.JOBLOG_JS_RESOURCE), "text/javascript")
			.asset("/jobs/rebuild", REF, "application/json")
			.sequence("/jobs/j1/stream", "text/event-stream", STREAM_1, STREAM_2)
			.page("submit", ORIGIN + "/jobs",
				contractPage(CONTRACT, "", BEFORE, after + AUTO_SUBMIT),
				"banner", "#rebuild .jc-joblog-banner")
			.waitFor("#rebuild .jc-joblog-ok")
			.page("reattach", ORIGIN + "/jobs?job=j1",
				contractPage(CONTRACT, "", SEED + BEFORE, after),
				"banner", "#rebuild .jc-joblog-banner")
			.waitFor("#rebuild .jc-joblog-ok")
			.page("reconnect", ORIGIN + "/jobs?job=j1",
				contractPage(CONTRACT, "", SEED + PROBE_ONLY, after),
				"banner", "#rebuild .jc-joblog-banner")
			.waitFor("#rebuild .jc-joblog-ok")
			.asset("/jobs/j1/cancel", CANCEL_DONE, "application/json")
			.page("cancel", ORIGIN + "/jobs",
				contractPage(CONTRACT, "", BEFORE_HANGING, after + AUTO_SUBMIT + AUTO_CANCEL),
				"banner", "#rebuild .jc-joblog-banner", "phase", "#rebuild .jc-joblog")
			.waitFor("#rebuild .jc-joblog[data-phase=done]")
			.run();
	}

	@Test void a01_submit_streamsToSuccess() {
		var r = report.get("submit");
		assertNoShellErrors(r);
		assertEquals("Done. Rebuilt.", queries(r).get("banner"));
		var p = probe(r);
		assertEquals(List.of("step 1", "step 2"), p.get("lines"));
		assertEquals("/jobs/j1/stream?lastEventId=0", p.get("stream"));
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
		assertEquals("/jobs/j1/stream?lastEventId=0", p.get("stream"));
		assertEquals(List.of(), p.get("posts"));
		assertEquals(List.of("step 1", "step 2"), p.get("lines"));
		assertEquals("?job=j1", p.get("search"));
	}

	@Test void a03_realEventSource_dropAndResume() {
		var r = report.get("reconnect");
		assertNoShellErrors(r);
		assertEquals("Done. Rebuilt.", queries(r).get("banner"));
		assertEquals(List.of("p1", "p2", "p3"), probe(r).get("lines"));
		// At least the first connect and the reconnect; a harmless extra retry is tolerated.
		var requests = (List<?>)r.get("requests");
		assertTrue(requests.size() >= 2, () -> String.valueOf(requests));
		var first = (Map<?,?>)requests.get(0);
		assertEquals("/jobs/j1/stream", first.get("path"), () -> String.valueOf(requests));
		assertEquals("?lastEventId=0", first.get("search"), () -> String.valueOf(requests));
		var firstId = first.get("lastEventId");
		assertTrue(firstId == null || "".equals(firstId), () -> String.valueOf(requests));
		// The proof: the browser's reconnect carried Last-Event-ID: 2 (the last id it received).
		assertEquals(Map.of("path", "/jobs/j1/stream", "search", "?lastEventId=0", "lastEventId", "2"), requests.get(1));
	}

	@Test void a04_cancel_postsToTheCancelUrlAndEndsCancelled() {
		assertNotEquals(BEFORE, BEFORE_HANGING, "the hanging variant must differ from the finishing one");
		var r = report.get("cancel");
		assertNoShellErrors(r);
		assertEquals("Cancelled.", queries(r).get("banner"));
		var p = probe(r);
		assertEquals("step 1", ((List<?>)p.get("lines")).get(0));
		var posts = (List<?>)p.get("posts");
		assertEquals(2, posts.size(), () -> String.valueOf(posts));
		assertEquals("/jobs/j1/cancel", ((Map<?,?>)posts.get(1)).get("url"));
	}
}
