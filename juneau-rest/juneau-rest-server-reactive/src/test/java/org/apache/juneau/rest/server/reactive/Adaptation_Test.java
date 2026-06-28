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
package org.apache.juneau.rest.server.reactive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link Adaptation} — both factory methods and {@code isStream()} branches.
 *
 * @since 10.0.0
 */
class Adaptation_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// A: single() factory — isStream() false, single() returns the stage, stream() returns null
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_single_isStream_false() {
		var cs = CompletableFuture.completedStage("x");
		var a = Adaptation.single(cs);
		assertFalse(a.isStream());
		assertSame(cs, a.single());
		assertNull(a.stream());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// B: stream() factory — isStream() true, stream() returns the publisher, single() returns null
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_stream_isStream_true() {
		Flow.Publisher<String> pub = sub -> sub.onSubscribe(new Flow.Subscription() {
			@Override public void request(long n) { sub.onComplete(); }
			@Override public void cancel() { /* publisher completes immediately; cancellation is never needed */ }
		});
		var a = Adaptation.stream(pub);
		assertTrue(a.isStream());
		assertSame(pub, a.stream());
		assertNull(a.single());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// C: null arguments rejected
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_single_null_throws() {
		assertThrows(Exception.class, () -> Adaptation.single(null));
	}

	@Test void c02_stream_null_throws() {
		assertThrows(Exception.class, () -> Adaptation.stream(null));
	}
}
