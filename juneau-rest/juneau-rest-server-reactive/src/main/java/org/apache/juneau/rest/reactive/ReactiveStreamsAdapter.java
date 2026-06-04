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

import java.util.concurrent.*;

/**
 * SPI that bridges a third-party reactive return type to one of the two JDK-native shapes that the
 * {@link ReactiveResponseProcessor} understands: a {@link CompletionStage} (single value) or a
 * {@link java.util.concurrent.Flow.Publisher Flow.Publisher} (a stream of values).
 *
 * <h5 class='topic'>Why this exists</h5>
 * <p>
 * The opt-in {@code juneau-rest-server-reactive} module handles {@link java.util.concurrent.Flow.Publisher Flow.Publisher}
 * (and, via the existing {@code AsyncResponseProcessor}, {@link CompletionStage}) using only JDK APIs &mdash;
 * no external dependency. Libraries such as Project Reactor ({@code Mono} / {@code Flux}), RxJava 3
 * ({@code Single} / {@code Maybe} / {@code Flowable} / {@code Observable}), and the
 * Reactive-Streams {@code org.reactivestreams.Publisher} are NOT on the core classpath. The opt-in
 * {@code juneau-rest-server-reactor} module ships {@code ReactiveStreamsAdapter} implementations that
 * convert those types into the JDK-native shapes, so the streaming / buffering / SSE / NDJSON plumbing
 * is implemented exactly once in the core.
 *
 * <h5 class='topic'>Registration</h5>
 * <p>
 * Adapters are discovered via the JDK {@link java.util.ServiceLoader} mechanism. A bridge module (or a
 * consumer who wants a custom adapter) ships a
 * {@code META-INF/services/org.apache.juneau.rest.reactive.ReactiveStreamsAdapter} file listing the
 * implementation class names. The {@link ReactiveResponseProcessor} loads the providers once (lazily,
 * at first use) and skips any provider whose backing library is absent from the runtime classpath
 * (a {@link NoClassDefFoundError} / {@link java.util.ServiceConfigurationError} on instantiation is swallowed).
 * This means a single {@code juneau-rest-server-reactor} jar can declare Reactor, RxJava, and
 * Reactive-Streams adapters while the consumer pulls only the {@code provided}-scope libraries they
 * actually want.
 *
 * @see ReactiveResponseProcessor
 * @see Adaptation
 * @since 10.0.0
 */
public interface ReactiveStreamsAdapter {

	/**
	 * Returns whether this adapter recognizes the supplied return value.
	 *
	 * <p>
	 * Implementations must return {@code false} (rather than throw) for values they do not handle, and
	 * must be cheap to call &mdash; this is invoked on the response hot path for every non-null handler
	 * return value once at least one adapter is registered.
	 *
	 * @param value The {@code @RestOp} handler return value. Never {@code null}.
	 * @return {@code true} if {@link #adapt(Object)} can convert this value.
	 */
	boolean canAdapt(Object value);

	/**
	 * Converts the supplied reactive value to a JDK-native {@link Adaptation}.
	 *
	 * <p>
	 * Only called when {@link #canAdapt(Object)} returned {@code true} for the same value.
	 *
	 * @param value The reactive value to adapt. Never {@code null}.
	 * @return The adapted single-value or streaming shape. Never {@code null}.
	 */
	Adaptation adapt(Object value);
}
