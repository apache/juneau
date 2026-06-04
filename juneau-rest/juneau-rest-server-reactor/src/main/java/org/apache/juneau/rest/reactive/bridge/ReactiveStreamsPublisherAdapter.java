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
package org.apache.juneau.rest.reactive.bridge;

import org.apache.juneau.rest.reactive.*;
import org.reactivestreams.*;

/**
 * Catch-all {@link ReactiveStreamsAdapter} for any
 * <a class="doclink" href="https://www.reactive-streams.org/">Reactive-Streams</a>
 * {@link org.reactivestreams.Publisher org.reactivestreams.Publisher&lt;T&gt;} that is not already
 * handled by a more specific adapter.
 *
 * <p>
 * Converts to a JDK {@link java.util.concurrent.Flow.Publisher Flow.Publisher} via
 * {@link org.reactivestreams.FlowAdapters#toFlowPublisher(Publisher)} and renders it as a stream.
 * Registered <em>last</em> so library-specific single-value types (Reactor {@code Mono}, which also
 * implements {@code Publisher}) are matched by their own adapter first.
 *
 * @since 10.0.0
 */
public class ReactiveStreamsPublisherAdapter implements ReactiveStreamsAdapter {

	@Override /* Overridden from ReactiveStreamsAdapter */
	public boolean canAdapt(Object value) {
		return value instanceof Publisher;
	}

	@Override /* Overridden from ReactiveStreamsAdapter */
	public Adaptation adapt(Object value) {
		return Adaptation.stream(FlowAdapters.toFlowPublisher((Publisher<?>) value));
	}
}
