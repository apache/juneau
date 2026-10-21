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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.apache.juneau.rest.server.terminal.TerminalPrompts;

/**
 * Brokers one release.py process's prompts (protocol v1).
 *
 * <p>A {@code credential.*} prompt with a stored value is answered at once ({@code source: "stored"}). Any other
 * prompt becomes the single pending prompt and is pushed to the page. Answer values are never logged and are held
 * only until written.
 */
public class PromptBroker implements TerminalPrompts {

	/** Stored credential values by prompt key (for example {@link CredentialAnswers#lookup}). */
	@FunctionalInterface
	public interface Stored extends Function<String, Optional<String>> {}

	private static final Logger LOG = Logger.getLogger(PromptBroker.class.getName());

	private final Stored stored;
	private final EventBroadcaster events;
	private final Map<String, String> autoAnswers;
	private final Object lock = new Object();
	private final List<String> queued = new ArrayList<>();
	private Predicate<String> writer;
	private JsonMap pending;
	private volatile Long pid;

	/**
	 * Constructor.
	 *
	 * @param stored Stored credential values by prompt key ({@link CredentialAnswers#lookup}).
	 * @param events Where {@code prompt} and {@code prompt-resolved} go.
	 * @param autoAnswers Values already typed on the page, by prompt key.
	 */
	public PromptBroker(Stored stored, EventBroadcaster events, Map<String, String> autoAnswers) {
		this.stored = stored;
		this.events = events;
		this.autoAnswers = Map.copyOf(autoAnswers);
	}

	/**
	 * Attaches the answer writer ({@code TerminalProcess::answer}) and flushes answers queued before it existed.
	 *
	 * @param value The writer; returns {@code false} when the line could not be written.
	 */
	public void attach(Predicate<String> value) {
		synchronized (lock) {
			writer = value;
			queued.forEach(this::send);
			queued.clear();
		}
	}

	/** @return The script's pid from its {@code hello}, or {@code null}. */
	public Long pid() {
		return pid;
	}

	/** @return The pending prompt (it never holds a secret). */
	public Optional<JsonMap> pending() {
		synchronized (lock) {
			return Optional.ofNullable(pending);
		}
	}

	@Override /* TerminalPrompts */
	public void onLine(String jsonLine) {
		JsonMap msg;
		try {
			msg = Json.to(jsonLine, JsonMap.class);
		} catch (Exception e) {
			LOG.warning(() -> "prompt channel: dropped a line that is not JSON (" + jsonLine.length() + " characters)");
			return;
		}
		if (msg == null)
			return;
		switch (msg.getString("type", "")) {
			case "hello" -> pid = msg.get("pid") instanceof Number n ? Long.valueOf(n.longValue()) : null;
			case "prompt" -> onPrompt(msg);
			default -> { /* unknown types are ignored (protocol v1) */ }
		}
	}

	private void onPrompt(JsonMap p) {
		var id = p.getString("id");
		var key = p.getString("key", "");
		var auto = autoAnswers.get(key);
		if (auto != null) {
			write(answerLine(id, auto, "entered"));
			return;
		}
		if (key.startsWith("credential.")) {
			Optional<String> v = Optional.empty();
			try {
				v = stored.apply(key);
			} catch (RuntimeException e) {
				// Never log the exception message: it could hold a secret.
				var k = key;
				var cls = e.getClass().getName();
				LOG.warning(() -> "prompt channel: stored lookup failed for " + k + " (" + cls + "); asking the operator");
			}
			if (v.isPresent()) {
				write(answerLine(id, v.get(), "stored"));
				return;
			}
		}
		synchronized (lock) {
			pending = p;
		}
		events.publish("prompt", p);
	}

	/**
	 * Answers the pending prompt.
	 *
	 * @param id The prompt id; must be the pending one.
	 * @param body {@code {"value": …}} or {@code {"cancel": true}}.
	 * @throws IllegalStateException If {@code id} is not the pending prompt (the REST layer maps it to 409).
	 * @throws IllegalArgumentException If {@code body} has neither {@code cancel: true} nor a {@code value}.
	 */
	public void answer(String id, JsonMap body) {
		String line;
		synchronized (lock) {
			if (pending == null || ! pending.getString("id", "").equals(id))
				throw new IllegalStateException("No pending prompt " + id);
			var cancel = Boolean.TRUE.equals(body.get("cancel"));
			if (! cancel && body.get("value") == null)
				throw new IllegalArgumentException("value or cancel required");
			line = cancel ? cancelLine(id) : answerLine(id, body.get("value"), "entered");
			pending = null;
		}
		write(line);
		events.publish("prompt-resolved", Map.of("id", id));
	}

	@Override /* TerminalPrompts */
	public void onEnd() {
		String id;
		synchronized (lock) {
			id = pending == null ? null : pending.getString("id");
			pending = null;
		}
		if (id != null)
			events.publish("prompt-resolved", Map.of("id", id));
	}

	private void write(String line) {
		synchronized (lock) {
			if (writer == null)
				queued.add(line);
			else
				send(line);
		}
	}

	private void send(String line) {
		if (! writer.test(line))
			LOG.warning("prompt channel: an answer could not be written (the process has ended or closed its channel)");
	}

	private static String answerLine(String id, Object value, String source) {
		var m = new LinkedHashMap<String, Object>();
		m.put("v", 1);
		m.put("type", "answer");
		m.put("id", id);
		m.put("value", value);
		m.put("source", source);
		return Json.of(m);
	}

	private static String cancelLine(String id) {
		var m = new LinkedHashMap<String, Object>();
		m.put("v", 1);
		m.put("type", "answer");
		m.put("id", id);
		m.put("cancel", Boolean.TRUE);
		return Json.of(m);
	}

	@Override
	public String toString() {
		return "PromptBroker[pid=" + pid + ", pending=" + pending().map(p -> p.getString("id")).orElse(null) + "]";
	}
}
