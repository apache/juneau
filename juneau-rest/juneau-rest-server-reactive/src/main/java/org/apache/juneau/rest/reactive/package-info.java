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

/**
 * Reactive-Streams return-type support for {@code @RestOp} handlers (opt-in, dependency-free).
 *
 * <p>
 * This package is the JDK-native spine that lets {@code @RestOp} handlers return reactive values. It
 * ships in the opt-in {@code juneau-rest-server-reactive} module &mdash; a bare {@code juneau-rest-server}
 * has <b>no</b> reactive behavior and {@code DefaultConfig} does not wire any reactive processor. Merely
 * placing this module on the classpath auto-registers {@link org.apache.juneau.rest.reactive.ReactiveResponseProcessor}
 * via a {@code META-INF/services/org.apache.juneau.rest.processor.ResponseProcessor} provider file, which
 * {@code RestContext} discovers through {@link java.util.ServiceLoader} and front-loads ahead of
 * {@code AsyncResponseProcessor}.
 *
 * <p>
 * The processor handles {@link java.util.concurrent.Flow.Publisher Flow.Publisher&lt;T&gt;} directly (no
 * external dependency) and exposes the {@link org.apache.juneau.rest.reactive.ReactiveStreamsAdapter} SPI
 * so the further opt-in {@code juneau-rest-server-reactor} module can plug in Project Reactor, RxJava 3,
 * and Reactive-Streams {@code Publisher} support without duplicating any of the streaming / buffering /
 * SSE / NDJSON plumbing.
 *
 * <h5 class='topic'>Key types</h5>
 * <ul>
 * 	<li>{@link org.apache.juneau.rest.reactive.ReactiveResponseProcessor} &mdash; the response processor
 * 		(auto-registered via {@code ServiceLoader}, front-loaded ahead of {@code AsyncResponseProcessor}).
 * 	<li>{@link org.apache.juneau.rest.reactive.ReactiveStreamsAdapter} &mdash; the {@link java.util.ServiceLoader}
 * 		SPI bridge modules implement.
 * 	<li>{@link org.apache.juneau.rest.reactive.Adaptation} &mdash; the single-value / streaming result of an adapter.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerReactive">REST Server &mdash; Reactive Streams</a>
 * 	<li class='link'><a class="doclink" href="https://www.reactive-streams.org/">Reactive Streams</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.reactive;
