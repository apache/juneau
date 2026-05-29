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
package org.apache.juneau.rest.reactive;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.concurrent.*;

/**
 * The result of a {@link ReactiveStreamsAdapter#adapt(Object) ReactiveStreamsAdapter.adapt(...)} call:
 * either a single-value {@link CompletionStage} or a multi-value
 * {@link java.util.concurrent.Flow.Publisher Flow.Publisher}.
 *
 * <p>
 * Exactly one of the two shapes is present. Single-value adaptations (e.g. Reactor {@code Mono},
 * RxJava {@code Single} / {@code Maybe}) collapse onto the existing {@code CompletableFuture} async
 * response path. Streaming adaptations (e.g. Reactor {@code Flux}, RxJava {@code Flowable} /
 * {@code Observable}, a Reactive-Streams {@code Publisher}) are subscribed to and rendered as a
 * buffered list, an SSE stream, or an NDJSON stream depending on the negotiated response media type.
 *
 * @see ReactiveStreamsAdapter
 * @see ReactiveResponseProcessor
 * @since 9.5.0
 */
public final class Adaptation {

	private final CompletionStage<?> single;
	private final java.util.concurrent.Flow.Publisher<?> stream;

	private Adaptation(CompletionStage<?> single, java.util.concurrent.Flow.Publisher<?> stream) {
		this.single = single;
		this.stream = stream;
	}

	/**
	 * Creates a single-value adaptation backed by a {@link CompletionStage}.
	 *
	 * @param value The completion stage. Must not be {@code null}.
	 * @return A new adaptation.
	 */
	public static Adaptation single(CompletionStage<?> value) {
		return new Adaptation(assertArgNotNull("value", value), null);
	}

	/**
	 * Creates a streaming adaptation backed by a {@link java.util.concurrent.Flow.Publisher Flow.Publisher}.
	 *
	 * @param value The publisher. Must not be {@code null}.
	 * @return A new adaptation.
	 */
	public static Adaptation stream(java.util.concurrent.Flow.Publisher<?> value) {
		return new Adaptation(null, assertArgNotNull("value", value));
	}

	/**
	 * Returns whether this adaptation is a multi-value stream (vs. a single value).
	 *
	 * @return {@code true} if this is a {@link java.util.concurrent.Flow.Publisher Flow.Publisher} adaptation.
	 */
	public boolean isStream() {
		return stream != null;
	}

	/**
	 * Returns the single-value completion stage, or {@code null} if this is a streaming adaptation.
	 *
	 * @return The completion stage, or {@code null}.
	 */
	public CompletionStage<?> single() {
		return single;
	}

	/**
	 * Returns the streaming publisher, or {@code null} if this is a single-value adaptation.
	 *
	 * @return The publisher, or {@code null}.
	 */
	public java.util.concurrent.Flow.Publisher<?> stream() {
		return stream;
	}
}
