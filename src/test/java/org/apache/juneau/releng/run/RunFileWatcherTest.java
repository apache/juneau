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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunFileWatcherTest {

	@Test
	void a01_publishesRunOnMtimeChangeOnly(@TempDir Path home) throws Exception {
		var bus = new EventBroadcaster();
		var got = new ArrayList<String>();
		bus.subscribe(got::add);
		var w = new RunFileWatcher(home, bus);
		w.poll();
		assertTrue(got.isEmpty());
		var f = Files.createDirectories(home.resolve("runs/9.2.1")).resolve("run.json");
		Files.writeString(f, "{}");
		w.poll();
		w.poll();
		assertEquals(1, got.size());
		assertTrue(got.get(0).contains("\"version\":\"9.2.1\""), got.get(0));
		Files.setLastModifiedTime(f, FileTime.fromMillis(Files.getLastModifiedTime(f).toMillis() + 5000));
		w.poll();
		assertEquals(2, got.size());
	}

	@Test
	void a02_pollFailureDoesNotCancelTheSchedule(@TempDir Path home) throws Exception {
		var attempts = new java.util.concurrent.atomic.AtomicInteger();
		var bus = new EventBroadcaster() {
			@Override
			public void publish(String type, java.util.Map<String, ?> fields) {
				attempts.incrementAndGet();
				throw new IllegalStateException("boom");
			}
		};
		var f = Files.createDirectories(home.resolve("runs/9.2.1")).resolve("run.json");
		Files.writeString(f, "{}");
		try (var w = new RunFileWatcher(home, bus)) {
			w.start();
			RunnerFixture.await(() -> attempts.get() >= 1, 10);
			// a changed mtime makes the next tick publish (and fail) again, so the schedule must still be alive
			Files.setLastModifiedTime(f, FileTime.fromMillis(Files.getLastModifiedTime(f).toMillis() + 5000));
			RunnerFixture.await(() -> attempts.get() >= 2, 10);
		}
	}
}
