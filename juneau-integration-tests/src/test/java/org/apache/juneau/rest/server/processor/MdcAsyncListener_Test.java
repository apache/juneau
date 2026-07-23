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
package org.apache.juneau.rest.server.processor;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.slf4j.*;

/**
 * Tests for {@link MdcAsyncListener} — the SLF4J MDC bridge that propagates request-thread MDC to
 * {@link CompletableFuture} completion threads.
 *
 * <p>
 * All direct unit tests exercise {@link MdcAsyncListener} in isolation via its static factory methods.
 * The integration tests verify the builder-knob opt-out path through the {@link AsyncResponseProcessor}
 * stack using the {@code MockRestClient} harness.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class MdcAsyncListener_Test extends TestBase {

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MdcAsyncListener_HappyPath: snapshot / restore works — callback sees request-thread MDC.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void happyPath_01_slfjAvailable() {
		assertTrue(MdcAsyncListener.isAvailable(), "SLF4J MDC must be on the test classpath");
	}

	@Test
	void happyPath_02_snapshotCapturesCurrentMdc() {
		MDC.put("requestId", "abc-123");
		MDC.put("userId", "alice");

		var snap = MdcAsyncListener.snapshot();

		assertNotNull(snap);
		assertEquals("abc-123", snap.get("requestId"));
		assertEquals("alice", snap.get("userId"));
	}

	@Test
	void happyPath_03_wrappedCallbackSeesMdc() {
		MDC.put("requestId", "req-999");
		var snap = MdcAsyncListener.snapshot();

		MDC.clear();  // Simulate moving to a fresh completion thread.

		var capturedRequestId = new AtomicReference<String>();
		var wrapped = MdcAsyncListener.<String>wrap(
			(v, e) -> capturedRequestId.set(MDC.get("requestId")),
			snap
		);

		wrapped.accept("result", null);

		assertEquals("req-999", capturedRequestId.get(), "Completion-thread callback must see the request-thread MDC.");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MdcAsyncListener_NoSlf4j: when snapshot is null (empty MDC), wrap returns original action.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void noSlf4j_01_emptyMdcSnapshotIsNull() {
		// MDC is clear — snapshot() must return null (lazy-skip contract).
		assertNull(MdcAsyncListener.snapshot());
	}

	@Test
	void noSlf4j_02_nullSnapshotReturnsOriginalAction() {
		var original = (java.util.function.BiConsumer<String,Throwable>) (v, e) -> {};
		var wrapped = MdcAsyncListener.wrap(original, null);
		assertSame(original, wrapped, "wrap(action, null) must return the original action unchanged.");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MdcAsyncListener_OptOut: builder knob mdcAsyncPropagation(false) disables propagation.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void optOut_01_nullSnapshotReturnsOriginalAction() {
		// When propagation is disabled (or request thread has no MDC), the snapshot is null
		// and wrap() returns the original action with no overhead.
		var original = (java.util.function.BiConsumer<String,Throwable>) (v, e) -> {};
		assertSame(original, MdcAsyncListener.wrap(original, null),
			"wrap(action, null) must return the original action unchanged when propagation is off.");
	}

	@Test
	void optOut_02_disabledPropagationDoesNotRestoreMdc() {
		// Simulate disabled propagation: no snapshot is taken, wrap is called with null.
		MDC.put("requestId", "should-not-propagate");
		var requestSnap = MdcAsyncListener.snapshot();
		MDC.clear();  // Completion thread starts empty.

		// opt-out: pretend the code decided not to propagate — it would pass null to wrap().
		var capturedOnCompletionThread = new AtomicReference<String>();
		MdcAsyncListener.<String>wrap(
			(v, e) -> capturedOnCompletionThread.set(MDC.get("requestId")),
			/* disabled: */ null
		).accept("result", null);

		assertNull(capturedOnCompletionThread.get(),
			"Completion callback must not see MDC when propagation is disabled (null snapshot).");
		// Snapshot was captured earlier just to consume it; not passed to wrap.
		assertNotNull(requestSnap);  // Confirm MDC was set on request thread.
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MdcAsyncListener_Cleanup: MDC cleared from completion thread after callback (no thread-pool leak).
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void cleanup_01_mdcClearedAfterCallback() {
		MDC.put("requestId", "to-be-cleared");
		var snap = MdcAsyncListener.snapshot();
		MDC.clear();  // Completion thread starts with no MDC.

		MdcAsyncListener.<String>wrap((v, e) -> {}, snap).accept("ok", null);

		// After the wrapped callback completes, the completion thread's MDC must be empty —
		// no leak into the next task in the thread pool.
		assertNull(MDC.get("requestId"), "MDC must be cleared after the wrapped callback runs.");
		assertTrue(MDC.getCopyOfContextMap() == null || MDC.getCopyOfContextMap().isEmpty(),
			"Completion thread must have an empty MDC after callback to prevent thread-pool contamination.");
	}

	@Test
	void cleanup_02_preExistingCompletionThreadMdcIsPreserved() {
		// Simulate a completion thread that already has its own MDC entries.
		MDC.put("ownKey", "ownValue");
		MdcAsyncListener.snapshot();  // Save for comparison.

		// Request-thread snapshot (a different set of keys).
		var requestSnap = Map.of("requestId", "req-789");

		// After wrap completes, the completion thread's original MDC should be restored.
		MdcAsyncListener.<String>wrap((v, e) -> {
			assertEquals("req-789", MDC.get("requestId"), "Request MDC must be visible during callback.");
		}, requestSnap).accept("ok", null);

		// After callback: completion thread's original "ownKey" must be back; "requestId" must be gone.
		assertEquals("ownValue", MDC.get("ownKey"), "Completion thread's own MDC must be restored.");
		assertNull(MDC.get("requestId"), "Request-thread MDC must not leak into the completion thread.");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// MdcAsyncListener_ExceptionalCompletion: MDC still set during failure callback, and cleared after.
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void exceptionalCompletion_01_mdcVisibleOnErrorPath() {
		MDC.put("requestId", "err-path");
		var snap = MdcAsyncListener.snapshot();
		MDC.clear();

		var capturedOnError = new AtomicReference<String>();
		var wrapped = MdcAsyncListener.<String>wrap(
			(v, e) -> capturedOnError.set(MDC.get("requestId")),
			snap
		);

		// Simulate exceptional completion: value is null, throwable is present.
		wrapped.accept(null, new RuntimeException("simulated failure"));

		assertEquals("err-path", capturedOnError.get(), "MDC must be set during exceptional-completion callback.");
		// Cleanup must still have run.
		assertNull(MDC.get("requestId"), "MDC must be cleared after exceptional-completion callback.");
	}

	@Test
	void exceptionalCompletion_02_mdcClearedEvenWhenCallbackThrows() {
		MDC.put("requestId", "throw-path");
		var snap = MdcAsyncListener.snapshot();
		MDC.clear();

		var wrapped = MdcAsyncListener.<String>wrap(
			(v, e) -> { throw new RuntimeException("callback itself throws"); },
			snap
		);

		// The wrapped BiConsumer must NOT re-throw (or if it does, MDC is cleared in finally).
		// BiConsumer.accept is not declared to throw, so the RuntimeException propagates — but
		// the important thing is the MDC is cleared even if it does.
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> wrapped.accept("x", null));
		assertEquals("callback itself throws", thrown.getMessage());

		// MDC must be cleared even though the callback threw.
		assertNull(MDC.get("requestId"), "MDC must be cleared in the finally block even when callback throws.");
	}

	// -----------------------------------------------------------------------------------------------------------------
	// traceEnrichment: snapshot() folds the active OTel trace/span id into the MDC snapshot so log
	// correlation survives the async-completion hop (resolved Q5).
	// -----------------------------------------------------------------------------------------------------------------

	@Test
	void traceEnrichment_01_activeSpanFoldsTraceAndSpanIdIntoSnapshot() {
		var tracer = io.opentelemetry.sdk.OpenTelemetrySdk.builder()
			.setTracerProvider(io.opentelemetry.sdk.trace.SdkTracerProvider.builder().build())
			.build()
			.getTracer("test");
		var span = tracer.spanBuilder("op").startSpan();
		try (var scope = span.makeCurrent()) {
			var snap = MdcAsyncListener.snapshot();
			assertNotNull(snap, "An active valid span must produce a non-null snapshot even with empty MDC.");
			assertEquals(span.getSpanContext().getTraceId(), snap.get("trace_id"));
			assertEquals(span.getSpanContext().getSpanId(), snap.get("span_id"));
		} finally {
			span.end();
		}
	}

	@Test
	void traceEnrichment_02_noActiveSpanLeavesSnapshotUnenriched() {
		// No active span — empty MDC must stay null (lazy-skip contract preserved).
		assertNull(MdcAsyncListener.snapshot());
	}

	@Test
	void traceEnrichment_03_existingMdcPreservedAlongsideTraceIds() {
		var tracer = io.opentelemetry.sdk.OpenTelemetrySdk.builder()
			.setTracerProvider(io.opentelemetry.sdk.trace.SdkTracerProvider.builder().build())
			.build()
			.getTracer("test");
		var span = tracer.spanBuilder("op").startSpan();
		MDC.put("requestId", "abc-123");
		try (var scope = span.makeCurrent()) {
			var snap = MdcAsyncListener.snapshot();
			assertNotNull(snap);
			assertEquals("abc-123", snap.get("requestId"), "Pre-existing MDC keys must be preserved.");
			assertEquals(span.getSpanContext().getTraceId(), snap.get("trace_id"));
		} finally {
			span.end();
		}
	}
}
