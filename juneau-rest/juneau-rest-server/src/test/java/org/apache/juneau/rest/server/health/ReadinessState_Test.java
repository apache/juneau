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
package org.apache.juneau.rest.server.health;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

class ReadinessState_Test {

	@Test void a01_markOutOfService_runsRegisteredCallbacks() {
		var state = new ReadinessState();
		var calls = new AtomicInteger();
		state.onOutOfService("a", calls::incrementAndGet).onOutOfService("b", calls::incrementAndGet);
		assertEquals(0, calls.get());
		state.markOutOfService();
		assertFalse(state.isReady());
		assertEquals(2, calls.get());
	}

	@Test void a02_sameKey_replacesPreviousCallback() {
		var state = new ReadinessState();
		var first = new AtomicInteger();
		var second = new AtomicInteger();
		state.onOutOfService("k", first::incrementAndGet).onOutOfService("k", second::incrementAndGet);
		state.markOutOfService();
		assertEquals(0, first.get());
		assertEquals(1, second.get());
	}

	@Test void a03_throwingCallback_doesNotBlockOthersOrShutdown() {
		var state = new ReadinessState();
		var ran = new AtomicInteger();
		state.onOutOfService("bad", () -> { throw new IllegalStateException("boom"); }).onOutOfService("good", ran::incrementAndGet);
		assertDoesNotThrow(state::markOutOfService);
		assertFalse(state.isReady());
		assertEquals(1, ran.get());
	}
}
