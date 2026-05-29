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
 * Project Reactor / RxJava 3 / Reactive-Streams {@code Publisher} adapters for the
 * {@code juneau-rest-server} {@link org.apache.juneau.rest.reactive.ReactiveStreamsAdapter} SPI.
 *
 * <p>
 * This module is opt-in. It ships three {@link org.apache.juneau.rest.reactive.ReactiveStreamsAdapter}
 * implementations &mdash; {@link org.apache.juneau.rest.reactive.bridge.ReactorReactiveAdapter},
 * {@link org.apache.juneau.rest.reactive.bridge.RxJavaReactiveAdapter}, and
 * {@link org.apache.juneau.rest.reactive.bridge.ReactiveStreamsPublisherAdapter} &mdash; registered via a
 * {@code META-INF/services} file so merely placing this jar (plus the desired reactive library) on the
 * classpath enables {@code Mono} / {@code Flux} / {@code Single} / {@code Maybe} / {@code Flowable} /
 * {@code Observable} / {@code Publisher} return types from {@code @RestOp} handlers.
 *
 * <h5 class='topic'>Containment</h5>
 * <p>
 * The {@code reactor-core}, {@code rxjava}, and {@code reactive-streams} dependencies are declared in
 * {@code provided} scope on this module's POM. Consumers add only the reactive library and version they
 * actually use; adapters whose backing library is absent are skipped at {@link java.util.ServiceLoader}
 * load time. The core {@code juneau-rest-server} jar stays dependency-free.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link org.apache.juneau.rest.reactive.ReactiveResponseProcessor}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerReactive">REST Server &mdash; Reactive Streams</a>
 * </ul>
 *
 * @since 9.5.0
 */
package org.apache.juneau.rest.reactive.bridge;
