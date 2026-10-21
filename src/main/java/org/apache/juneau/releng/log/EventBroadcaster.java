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

package org.apache.juneau.releng.log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * The Release page's event bus: {@code process}, {@code prompt}, {@code prompt-resolved} and {@code run} events, each
 * a JSON object whose first field is {@code type}. Prompt payloads never hold a secret (the protocol forbids it).
 */
public class EventBroadcaster implements Broadcaster {

	private final Set<Consumer<String>> sinks = new CopyOnWriteArraySet<>();

	@Override /* Broadcaster */
	public AutoCloseable subscribe(Consumer<String> sink) {
		sinks.add(sink);
		return () -> sinks.remove(sink);
	}

	/**
	 * Publishes one event to every subscriber.
	 *
	 * @param type The event type.
	 * @param fields The payload; its own {@code type} entry, if any, is dropped.
	 */
	public void publish(String type, Map<String, ?> fields) {
		var m = new LinkedHashMap<String, Object>();
		m.put("type", type);
		fields.forEach((k, v) -> {
			if (! "type".equals(k))
				m.put(k, v);
		});
		var json = Json.of(m);
		for (var s : sinks) {
			try {
				s.accept(json);
			} catch (RuntimeException e) {
				// One bad subscriber must not starve the others.
			}
		}
	}

	int subscribers() {
		return sinks.size();
	}
}
