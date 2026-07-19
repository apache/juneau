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
package org.apache.juneau.rest.server.reactive.reactor;

import org.apache.juneau.rest.server.reactive.*;
import org.reactivestreams.*;

import reactor.core.publisher.*;

/**
 * {@link ReactiveStreamsAdapter} for <a class="doclink" href="https://projectreactor.io/">Project Reactor</a>
 * return types.
 *
 * <ul>
 * 	<li>{@link Mono Mono&lt;T&gt;} &rarr; single value (via {@link Mono#toFuture()}). An empty {@code Mono}
 * 		completes the response with a {@code null} body.
 * 	<li>{@link Flux Flux&lt;T&gt;} &rarr; stream (via {@link org.reactivestreams.FlowAdapters#toFlowPublisher(Publisher)}).
 * </ul>
 *
 * <p>
 * Registered ahead of {@link ReactiveStreamsPublisherAdapter} so a {@code Mono} (which also implements
 * {@code org.reactivestreams.Publisher}) is treated as a single value rather than a one-element stream.
 *
 * @since 10.0.0
 */
public class ReactorReactiveAdapter implements ReactiveStreamsAdapter {

	@Override /* Overridden from ReactiveStreamsAdapter */
	public boolean canAdapt(Object value) {
		return value instanceof Mono || value instanceof Flux;
	}

	@Override /* Overridden from ReactiveStreamsAdapter */
	public Adaptation adapt(Object value) {
		if (value instanceof Mono<?> value2)
			return Adaptation.single(value2.toFuture());
		var f = (Flux<?>) value;
		return Adaptation.stream(FlowAdapters.toFlowPublisher(f));
	}
}
