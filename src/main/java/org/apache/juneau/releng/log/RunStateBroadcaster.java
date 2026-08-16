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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Per-run in-memory pub/sub of run/step-status snapshots, mirroring {@link LogBroadcaster}'s shape.
 * Subscribers are SSE connections to the run-state channel ({@code /events/{version}/state}); each
 * published payload is one JSON-serialized {@code RunStateSnapshot}. Publish is thread-safe since steps
 * run on a background thread while SSE connections read/write on request-handling threads.
 */
public class RunStateBroadcaster implements Broadcaster {

	private final List<Consumer<String>> subscribers = new CopyOnWriteArrayList<>();

	@Override
	public AutoCloseable subscribe(Consumer<String> sink) {
		subscribers.add(sink);
		return () -> subscribers.remove(sink);
	}

	public void publish(String snapshotJson) {
		for (var s : subscribers) {
			try {
				s.accept(snapshotJson);
			} catch (RuntimeException ignore) {
				/* a dead client must not break others */ }
		}
	}

	public int subscriberCount() {
		return subscribers.size();
	}
}
