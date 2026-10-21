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
package org.apache.juneau.releng.run;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.junit.jupiter.api.Test;

class PromptBrokerTest {

	static final String HELLO = "{\"v\":1,\"type\":\"hello\",\"pid\":4242,\"script\":\"release.py\"}";

	static JsonMap json(String s) {
		try {
			return Json.to(s, JsonMap.class);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	static String prompt(String id, String kind, String key) {
		return "{\"v\":1,\"type\":\"prompt\",\"id\":\"" + id + "\",\"kind\":\"" + kind + "\",\"key\":\"" + key
			+ "\",\"message\":\"m\"}";
	}

	final List<String> written = new ArrayList<>();
	final List<String> events = new ArrayList<>();

	PromptBroker broker(Map<String, String> stored, Map<String, String> auto) {
		var bus = new EventBroadcaster();
		bus.subscribe(events::add);
		var b = new PromptBroker(k -> Optional.ofNullable(stored.get(k)), bus, auto);
		b.attach(written::add);
		return b;
	}

	@Test
	void a01_credentialKeyAutoAnswered() {
		var b = broker(Map.of("credential.gpg.passphrase", "pp"), Map.of());
		b.onLine(HELLO);
		b.onLine(prompt("p1", "secret", "credential.gpg.passphrase"));
		assertEquals(1, written.size());
		var a = json(written.get(0));
		assertEquals("p1", a.getString("id"));
		assertEquals("pp", a.getString("value"));
		assertEquals("stored", a.getString("source"));
		assertTrue(b.pending().isEmpty());
		assertTrue(events.isEmpty(), "no dialog");
		assertEquals(Long.valueOf(4242), b.pid());
	}

	@Test
	void a02_missingCredentialOpensDialog() {
		var b = broker(Map.of(), Map.of());
		b.onLine(prompt("p1", "secret", "credential.gpg.passphrase"));
		assertEquals("p1", b.pending().orElseThrow().getString("id"));
		assertTrue(events.get(0).contains("\"type\":\"prompt\""), events.get(0));
		b.answer("p1", json("{\"value\":\"typed\"}"));
		var a = json(written.get(0));
		assertEquals("typed", a.getString("value"));
		assertEquals("entered", a.getString("source"));
		assertTrue(b.pending().isEmpty());
		assertTrue(events.get(1).contains("prompt-resolved"), events.get(1));
	}

	@Test
	void a03_wrongIdRejected() {
		var b = broker(Map.of(), Map.of());
		b.onLine(prompt("p1", "text", "release.rc"));
		assertThrows(IllegalStateException.class, () -> b.answer("p9", json("{\"value\":\"1\"}")));
		assertTrue(written.isEmpty());
		assertTrue(b.pending().isPresent(), "the real prompt stays pending");
	}

	@Test
	void a04_secretNeverLogged() {
		var records = new ArrayList<LogRecord>();
		var h = new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() {}
			@Override public void close() {}
		};
		var root = Logger.getLogger("org.apache.juneau.releng");
		var old = root.getLevel();
		root.setLevel(Level.ALL);
		root.addHandler(h);
		try {
			var bus = new EventBroadcaster();
			bus.subscribe(events::add);
			// A writer that refuses (closed channel) forces the warning path too.
			var b = new PromptBroker(k -> Optional.of("s3cr3t-stored"), bus, Map.of());
			b.attach(line -> false);
			b.onLine(prompt("p1", "secret", "credential.gpg.passphrase"));
			b.onLine(prompt("p2", "secret", "release.other-secret"));
			b.answer("p2", json("{\"value\":\"s3cr3t-typed\"}"));
			b.onLine("not json s3cr3t-garbage");
			for (var r : records) {
				var text = r.getMessage() + " " + (r.getParameters() == null ? "" : List.of(r.getParameters()));
				assertFalse(text.contains("s3cr3t"), text);
			}
			for (var e : events)
				assertFalse(e.contains("s3cr3t"), e);
			assertFalse(b.toString().contains("s3cr3t"));
		} finally {
			root.removeHandler(h);
			root.setLevel(old);
		}
	}

	@Test
	void a05_processEndClearsPending() {
		var b = broker(Map.of(), Map.of());
		b.onLine(prompt("p1", "text", "release.rc"));
		b.onEnd();
		assertTrue(b.pending().isEmpty());
		assertTrue(events.get(events.size() - 1).contains("\"prompt-resolved\""));
		assertThrows(IllegalStateException.class, () -> b.answer("p1", json("{\"value\":\"1\"}")));
	}

	@Test
	void a06_autoAnswerForTypedRcConfirm() {
		var b = broker(Map.of(), Map.of("confirm.revert", "RC2"));
		b.onLine(prompt("p1", "confirm", "confirm.revert"));
		assertEquals("RC2", json(written.get(0)).getString("value"));
		assertTrue(b.pending().isEmpty());
	}

	@Test
	void a08_throwingStoredLookupFallsThroughToDialogWithoutLeaking() {
		var bus = new EventBroadcaster();
		bus.subscribe(events::add);
		var b = new PromptBroker(k -> { throw new IllegalStateException("s3cr3t-in-message"); }, bus, Map.of());
		b.attach(written::add);
		b.onLine(prompt("p1", "secret", "credential.gpg.passphrase"));
		assertTrue(written.isEmpty());
		assertEquals("p1", b.pending().orElseThrow().getString("id"));
		assertTrue(events.get(0).contains("\"type\":\"prompt\""), events.get(0));
	}

	@Test
	void a09_cancelWritesCancelLineWithoutValue() {
		var b = broker(Map.of(), Map.of());
		b.onLine(prompt("p1", "text", "release.rc"));
		b.answer("p1", json("{\"cancel\":true}"));
		var a = json(written.get(0));
		assertEquals(Boolean.TRUE, a.get("cancel"));
		assertFalse(a.containsKey("value"));
		assertTrue(b.pending().isEmpty());
	}

	@Test
	void a10_bodyWithoutValueOrCancelRejectedAndPromptStaysPending() {
		var b = broker(Map.of(), Map.of());
		b.onLine(prompt("p1", "text", "release.rc"));
		var ex = assertThrows(IllegalArgumentException.class, () -> b.answer("p1", json("{}")));
		assertEquals("value or cancel required", ex.getMessage());
		assertTrue(written.isEmpty());
		assertTrue(b.pending().isPresent());
	}

	@Test
	void a07_linesBeforeAttachAreQueued() {
		var bus = new EventBroadcaster();
		var b = new PromptBroker(k -> Optional.of("pp"), bus, Map.of());
		b.onLine(prompt("p1", "secret", "credential.gpg.passphrase"));
		b.attach(written::add);
		assertEquals(1, written.size());
	}
}
