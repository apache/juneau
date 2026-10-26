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
package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.http.response.Conflict;
import org.apache.juneau.http.response.InternalServerError;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.apache.juneau.releng.run.PromptBroker;
import org.apache.juneau.releng.run.ReleaseProcessService;
import org.apache.juneau.releng.run.RunFiles;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.terminal.region.TerminalProcess;
import org.apache.juneau.console.views.IdempotencyKey;
import org.apache.juneau.rest.server.view.freemarker.FreemarkerView;
import org.apache.juneau.commons.inject.StackOverlay;
import org.apache.juneau.commons.utils.IoUtils;
import org.apache.juneau.rest.mock.MockRestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.servlet.http.HttpServletRequest;

class ReleaseRunRestTest {

	static void runJson(Path home, String phase, int rc, String attemptDir, String status) throws Exception {
		var d = Files.createDirectories(home.resolve("runs/9.2.1"));
		Files.writeString(d.resolve("run.json"), "{\"schema\":1,\"details\":{\"version\":\"9.2.1\",\"rc\":" + rc + "},\"phase\":\""
			+ phase + "\",\"attempts\":[{\"n\":1,\"dir\":\"" + attemptDir + "\",\"command\":\"start\",\"origin\":\"cli\","
			+ "\"status\":\"" + status + "\",\"started\":\"20261020-100000\",\"pid\":1}]}");
	}

	ReleaseRunRest rest(Path home, ReleaseProcessService svc) {
		return new ReleaseRunRest(new RunFiles(home, svc::isLiveDir), svc);
	}

	static final String HELLO = "{\"v\":1,\"type\":\"hello\",\"pid\":4242}";
	static final String PROMPT = "{\"v\":1,\"type\":\"prompt\",\"id\":\"p1\",\"kind\":\"text\",\"key\":\"release.branch\",\"message\":\"m\"}";

	@SuppressWarnings("resource") // Mocked process and source hold no real resource; ownership passes to the returned Live.
	static ReleaseProcessService.Live live(String attemptId, boolean hello, boolean prompt) {
		var broker = new PromptBroker(k -> Optional.empty(), new EventBroadcaster(), java.util.Map.of());
		broker.attach(line -> true);
		if (hello)
			broker.onLine(HELLO);
		if (prompt)
			broker.onLine(PROMPT);
		var proc = mock(TerminalProcess.class);
		var source = mock(org.apache.juneau.terminal.region.FileTerminalSource.class);
		when(proc.terminal()).thenReturn(source);
		return new ReleaseProcessService.Live(attemptId, java.nio.file.Path.of("x"), "start", true, proc, broker);
	}

	@SuppressWarnings("resource") // Mocked process and source hold no real resource.
	@Test
	void a01_terminalSourceLiveThenFile(@TempDir Path home) throws Exception {
		var dir = Files.createDirectories(home.resolve("attempts/20261020-100000-001"));
		Files.writeString(dir.resolve("terminal.log"), "hello\n");
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var r = rest(home, svc);
		assertTrue(r.terminalSource("attempt-20261020-100000-001", null).isPresent(), "replay from terminal.log");
		assertTrue(r.terminalSource("attempt-nope", null).isEmpty());
		assertTrue(r.terminalSource("attempt-..", null).isEmpty(), "no traversal");
		Files.delete(dir.resolve("terminal.log"));
		Files.writeString(dir.resolve("full.log"), "older attempt\n");
		assertTrue(r.terminalSource("attempt-20261020-100000-001", null).isPresent(), "falls back to full.log");

		var l = live("attempt-20261020-100000-009", false, false);
		when(svc.live()).thenReturn(Optional.of(l));
		assertSame(l.process().terminal(), r.terminalSource("attempt-20261020-100000-009", null).orElseThrow(), "live branch");
		assertTrue(r.terminalSource("attempt-20261020-100000-008", null).isEmpty(), "another attempt is not the live one");
	}

	@Test
	void a02_dropRcRequiresTypedRc(@TempDir Path home) throws Exception {
		runJson(home, "vote", 2, home.resolve("attempts/a").toString(), "ok");
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var r = rest(home, svc);
		var rc1 = body("RC1", false);
		assertThrows(BadRequest.class, () -> r.dropRc(rc1));
		verify(svc, never()).dropRc(anyString(), anyBoolean());
		when(svc.dropRc("RC2", false)).thenReturn(null);
		var rc2 = body("RC2", false);
		assertThrows(NullPointerException.class, () -> r.dropRc(rc2), "reached the service");
		verify(svc).dropRc("RC2", false);
	}

	@Test
	void a03_dropRcRefusedAtPromote(@TempDir Path home) throws Exception {
		runJson(home, "promote", 2, home.resolve("attempts/a").toString(), "ok");
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var r = rest(home, svc);
		var req = body("RC2", false);
		assertThrows(Conflict.class, () -> r.dropRc(req));
	}

	@Test
	void a04_startRefusedWhileRunActiveOrLive(@TempDir Path home) throws Exception {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		when(svc.start(anyBoolean())).thenThrow(new ReleaseProcessService.BusyException("x"));
		var r = rest(home, svc);
		var req = start(false, null);
		var e = assertThrows(Conflict.class, () -> r.start(req));
		assertTrue(e.getMessage().contains("already running"));
		runJson(home, "rc", 1, home.resolve("attempts/a").toString(), "failed");
		var svc2 = mock(ReleaseProcessService.class);
		when(svc2.live()).thenReturn(Optional.empty());
		var r2 = rest(home, svc2);
		var req2 = start(false, null);
		assertThrows(Conflict.class, () -> r2.start(req2), "an active run must be resumed");
		verify(svc2, never()).start(anyBoolean());
	}

	@Test
	void a05_resumeNeedsActiveRun(@TempDir Path home) {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var r = rest(home, svc);
		var req = new ReleaseRunRest.DryRunRequest();
		assertThrows(Conflict.class, () -> r.resume(req));
	}

	@Test
	void a06_answerIsNeverLogged() {
		assertEquals(Level.OFF, Logger.getLogger(ReleaseRunRest.class.getName() + ".answer").getLevel());
	}

	@Test
	void a07_pageCarriesStartKey(@TempDir Path home) {
		var svc = mock(ReleaseProcessService.class);
		var view = (FreemarkerView) rest(home, svc).page(mock(HttpServletRequest.class));
		assertEquals("release", view.getTemplateName());
		assertNotNull(view.getAttributes().get("startKey"));
		assertEquals("release", view.getAttributes().get("navTab"));
	}

	@Test
	void a08_otherLaunchFailuresAre500NotConflict(@TempDir Path home) throws Exception {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		when(svc.start(anyBoolean())).thenThrow(new IllegalStateException("python3 not found"));
		var r = rest(home, svc);
		var req = start(false, null);
		var e = assertThrows(InternalServerError.class, () -> r.start(req));
		assertTrue(e.getMessage().contains("python3 not found"));
		when(svc.resume(anyBoolean())).thenThrow(new IllegalStateException("dir already holds a run"));
		runJson(home, "rc", 1, home.resolve("attempts/a").toString(), "failed");
		var r2 = rest(home, svc);
		var resumeReq = new ReleaseRunRest.DryRunRequest();
		assertThrows(InternalServerError.class, () -> r2.resume(resumeReq));
	}

	@Test
	void a09_startKeyReplayReturnsTheLiveAttempt(@TempDir Path home) {
		var svc = mock(ReleaseProcessService.class);
		var l = live("attempt-20261020-100000-001", true, false);
		when(svc.start(false)).thenReturn(l);
		when(svc.live()).thenReturn(Optional.of(l));
		var r = rest(home, svc);
		var key = IdempotencyKey.mintSelfTargeted(ReleaseRunRest.START_ACTION).value();
		assertEquals("attempt-20261020-100000-001", r.start(start(false, key)).get("attemptId"));
		assertEquals("attempt-20261020-100000-001", r.start(start(false, key)).get("attemptId"));
		verify(svc, times(1)).start(false);
	}

	@Test
	void a10_currentShape(@TempDir Path home) throws Exception {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var idle = rest(home, svc).current();
		assertEquals(java.util.Set.of("history"), idle.keySet());

		runJson(home, "rc", 1, home.resolve("attempts/a").toString(), "failed");
		var withPrompt = live("attempt-a", true, true);
		when(svc.live()).thenReturn(Optional.of(withPrompt));
		var m = rest(home, svc).current();
		assertEquals(java.util.Set.of("active", "live", "pending", "history"), m.keySet());
		var activeJson = Json.of(m.get("active"));
		assertTrue(activeJson.contains("\"phase\":\"rc\""), activeJson);
		assertFalse(activeJson.contains("\"path\""), activeJson);
		@SuppressWarnings("unchecked")
		var lv = (java.util.Map<String, Object>)m.get("live");
		assertEquals("attempt-a", lv.get("attemptId"));
		assertEquals("start", lv.get("command"));
		assertEquals(true, lv.get("dryRun"));
		assertEquals(4242L, lv.get("pid"));

		var quiet = live("attempt-a", false, false);
		when(svc.live()).thenReturn(Optional.of(quiet));
		assertFalse(rest(home, svc).current().containsKey("pending"));
		assertDoesNotThrow(() -> rest(home, svc).current(), "a live process that has not said hello has no pid yet");
	}

	@Test
	void a11_cancelDelegates(@TempDir Path home) {
		var svc = mock(ReleaseProcessService.class);
		assertTrue(rest(home, svc).cancel().isEmpty());
		verify(svc).cancel();
	}

	@Test
	void a12_dropRcWhileBusyIs409(@TempDir Path home) throws Exception {
		runJson(home, "rc", 2, home.resolve("attempts/a").toString(), "failed");
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		when(svc.dropRc("RC2", false)).thenThrow(new ReleaseProcessService.BusyException("x"));
		var r = rest(home, svc);
		var req = body("RC2", false);
		assertThrows(Conflict.class, () -> r.dropRc(req));
	}

	@Test
	void a13_answerMapsErrors(@TempDir Path home) {
		var svc = mock(ReleaseProcessService.class);
		var r = rest(home, svc);
		var value = new JsonMap().append("value", "x");
		when(svc.live()).thenReturn(Optional.empty());
		assertThrows(Conflict.class, () -> r.answer("p1", value), "no live process");

		var withPrompt = live("attempt-a", true, true);
		when(svc.live()).thenReturn(Optional.of(withPrompt));
		assertThrows(Conflict.class, () -> r.answer("nope", value), "not the pending prompt");
		assertThrows(BadRequest.class, () -> r.answer("p1", new JsonMap()), "neither value nor cancel");
		assertThrows(BadRequest.class, () -> r.answer("p1", null), "no body");
		assertTrue(r.answer("p1", value).isEmpty());
	}

	@Test
	void a14_nullBodiesAndMissingRc(@TempDir Path home) throws Exception {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		var r = rest(home, svc);
		assertThrows(BadRequest.class, () -> r.start(null));
		assertThrows(BadRequest.class, () -> r.dropRc(null));
		runJson(home, "rc", 0, home.resolve("attempts/a").toString(), "failed");
		var r2 = rest(home, svc);
		var noRc = body(null, false);
		var e = assertThrows(BadRequest.class, () -> r2.dropRc(noRc));
		assertFalse(e.getMessage().contains("RCnull"), e.getMessage());
		verify(svc, never()).dropRc(any(), anyBoolean());
	}

	@Test
	void a15_pageRendersThroughRestWithoutTemplateErrors(@TempDir Path home) throws Exception {
		var svc = mock(ReleaseProcessService.class);
		when(svc.live()).thenReturn(Optional.empty());
		try (var client = MockRestClient.builder(rest(home, svc)).overridingBeanStore(new StackOverlay()).build();
			var resp = client.request("GET", "/").run()) {
			assertEquals(200, resp.getStatusCode());
			var body = resp.getBodyAsString();
			assertTrue(body.contains("<h1>Release</h1>"), body);
			assertTrue(body.contains("class=\"jc-page-header\""), body);
			assertTrue(body.contains("id=\"rl-terminal\""), body);
			assertTrue(body.matches("(?s).*data-start-key=\"[^\"]+\".*"), body);
			assertTrue(body.matches("(?s).*/js/release\\.js\\?v=[0-9a-f]{8}.*"), body);
			assertTrue(body.matches("(?s).*/css/release\\.css\\?v=[0-9a-f]{8}.*"), body);
			assertFalse(body.contains("slds-"), body);
			assertFalse(body.contains("ssc-"), body);
			assertFalse(body.contains("FreeMarker template error"), body);
		}
	}

	@Test
	void a16_releasePageAssetsConsumeJuneauWellWithoutPageCanvas() throws Exception {
		for (var path : java.util.List.of("/templates/release.ftlh", "/static/css/release.css", "/static/js/release.js")) {
			try (var in = ReleaseRunRestTest.class.getResourceAsStream(path)) {
				assertNotNull(in, path);
				var text = new String(IoUtils.readBytes(in), java.nio.charset.StandardCharsets.UTF_8);
				assertFalse(text.contains("rm-page-canvas"), path);
				assertFalse(text.contains("slds-"), path);
				assertFalse(text.contains("ssc-"), path);
				if (path.endsWith(".ftlh")) {
					assertTrue(text.contains("class=\"jc-page-header\""), text);
					assertTrue(text.contains("<h1>Release</h1>"), text);
					assertTrue(text.contains("jc-page-sub"), text);
					assertTrue(text.contains("toolkit=\"terminal\""), text);
				}
			}
		}
	}

	static ReleaseRunRest.DropRcRequest body(String confirm, boolean dryRun) {
		var b = new ReleaseRunRest.DropRcRequest();
		b.confirm = confirm;
		b.dryRun = dryRun;
		return b;
	}

	static ReleaseRunRest.StartRequest start(boolean dryRun, String key) {
		var b = new ReleaseRunRest.StartRequest();
		b.dryRun = dryRun;
		b.idempotencyKey = key;
		return b;
	}
}
