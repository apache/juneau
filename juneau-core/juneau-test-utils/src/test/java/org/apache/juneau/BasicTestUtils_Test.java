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
package org.apache.juneau;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

class BasicTestUtils_Test extends TestBase {

	@TempDir Path dir;

	//------------------------------------------------------------------------------------------------------------------
	// await(BooleanSupplier, Duration)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_await_trueImmediately() throws Exception {
		var calls = new AtomicInteger();
		var t0 = System.nanoTime();
		assertTrue(await(() -> calls.incrementAndGet() > 0, Duration.ofSeconds(10)));
		assertEquals(1, calls.get());
		assertTrue(Duration.ofNanos(System.nanoTime() - t0).toMillis() < 1000, "an already-true condition should not wait");
	}

	@Test void a02_await_trueAfterDelay() throws Exception {
		var calls = new AtomicInteger();
		assertTrue(await(() -> calls.incrementAndGet() >= 3, Duration.ofSeconds(10)));
		assertEquals(3, calls.get());
	}

	@Test void a03_await_falseOnTimeout() throws Exception {
		var t0 = System.nanoTime();
		assertFalse(await(() -> false, Duration.ofMillis(200)));
		var ms = Duration.ofNanos(System.nanoTime() - t0).toMillis();
		assertTrue(ms >= 200, "returned before the timeout: " + ms + " ms");
		assertTrue(ms < 5000, "overran the timeout: " + ms + " ms");
	}

	@Test void a04_await_zeroTimeoutChecksOnce() throws Exception {
		var calls = new AtomicInteger();
		assertFalse(await(() -> calls.incrementAndGet() < 0, Duration.ZERO));
		assertEquals(1, calls.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// awaitFile(Path, Duration)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_awaitFile_exists() throws Exception {
		var f = Files.writeString(dir.resolve("f"), "x");
		assertTrue(awaitFile(f, Duration.ofSeconds(10)));
	}

	@Test void b02_awaitFile_createdLater() throws Exception {
		var f = dir.resolve("f");
		var later = CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS);
		CompletableFuture.runAsync(() -> safe(() -> Files.writeString(f, "x")), later);
		assertTrue(awaitFile(f, Duration.ofSeconds(10)));
	}

	@Test void b03_awaitFile_missing() throws Exception {
		assertFalse(awaitFile(dir.resolve("missing"), Duration.ofMillis(100)));
	}
}
