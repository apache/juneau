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

import io.reactivex.rxjava3.core.*;

/**
 * {@link ReactiveStreamsAdapter} for <a class="doclink" href="https://github.com/ReactiveX/RxJava">RxJava 3</a>
 * return types.
 *
 * <ul>
 * 	<li>{@link Single Single&lt;T&gt;} &rarr; single value (via {@link Single#toCompletionStage()}).
 * 	<li>{@link Maybe Maybe&lt;T&gt;} &rarr; single value (via {@link Maybe#toCompletionStage(Object)} with a
 * 		{@code null} default, so an empty {@code Maybe} yields a {@code null} body).
 * 	<li>{@link Completable} &rarr; single {@code null} value (via {@link Completable#toCompletionStage(Object)}).
 * 	<li>{@link Flowable Flowable&lt;T&gt;} &rarr; stream (via {@link org.reactivestreams.FlowAdapters#toFlowPublisher(Publisher)}).
 * 	<li>{@link Observable Observable&lt;T&gt;} &rarr; stream (converted to a {@link Flowable} with
 * 		{@link BackpressureStrategy#BUFFER} first, since {@code Observable} has no native backpressure).
 * </ul>
 *
 * @since 10.0.0
 */
public class RxJavaReactiveAdapter implements ReactiveStreamsAdapter {

	@Override /* Overridden from ReactiveStreamsAdapter */
	public boolean canAdapt(Object value) {
		return value instanceof Single
			|| value instanceof Maybe
			|| value instanceof Completable
			|| value instanceof Flowable
			|| value instanceof Observable;
	}

	@Override /* Overridden from ReactiveStreamsAdapter */
	public Adaptation adapt(Object value) {
		if (value instanceof Single<?> s)
			return Adaptation.single(s.toCompletionStage());
		if (value instanceof Maybe<?> m)
			return Adaptation.single(m.toCompletionStage(null));
		if (value instanceof Completable c)
			return Adaptation.single(c.toCompletionStage(null));
		if (value instanceof Flowable<?> f)
			return Adaptation.stream(FlowAdapters.toFlowPublisher(f));
		var o = (Observable<?>) value;
		return Adaptation.stream(FlowAdapters.toFlowPublisher(o.toFlowable(BackpressureStrategy.BUFFER)));
	}
}
