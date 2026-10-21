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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.juneau.releng.log.EventBroadcaster;

/**
 * Polls {@code <rm.home>/runs/*}{@code /run.json} mtimes once a second and publishes a {@code run} event for each
 * changed file, so the page reflects CLI runs too.
 */
public class RunFileWatcher implements AutoCloseable {

	private static final Logger LOG = Logger.getLogger(RunFileWatcher.class.getName());

	private final Path runs;
	private final EventBroadcaster events;
	private final Map<Path, Long> seen = new HashMap<>();
	private ScheduledExecutorService timer;

	/**
	 * Constructor.
	 *
	 * @param home {@code rm.home}.
	 * @param events The page's event bus.
	 */
	public RunFileWatcher(Path home, EventBroadcaster events) {
		this.runs = home.resolve("runs");
		this.events = events;
	}

	/** Starts the one-second poll. */
	public synchronized void start() {
		if (timer == null) {
			timer = Executors.newSingleThreadScheduledExecutor(r -> {
				var t = new Thread(r, "run-file-watcher");
				t.setDaemon(true);
				return t;
			});
			timer.scheduleWithFixedDelay(this::safePoll, 1, 1, TimeUnit.SECONDS);
		}
	}

	/** Runs one poll; a failure is logged so the scheduled task is never cancelled by it. */
	private void safePoll() {
		try {
			poll();
		} catch (RuntimeException e) {
			LOG.log(Level.WARNING, "run file poll failed; retrying next tick", e);
		}
	}

	synchronized void poll() {
		if (! Files.isDirectory(runs))
			return;
		try (Stream<Path> s = Files.list(runs)) {
			s.map(d -> d.resolve("run.json")).filter(Files::isRegularFile).forEach(f -> {
				try {
					var m = Files.getLastModifiedTime(f).toMillis();
					var prior = seen.put(f, m);
					if (prior == null || prior != m)
						events.publish("run", Map.of("version", f.getParent().getFileName().toString()));
				} catch (IOException e) {
					// vanished between list and stat: the next poll sees the new state
				}
			});
		} catch (IOException e) {
			// runs/ unreadable this tick: try again next second
		}
	}

	@Override
	public synchronized void close() {
		if (timer != null)
			timer.shutdownNow();
		timer = null;
	}
}
