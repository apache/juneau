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

import org.apache.juneau.commons.inject.*;

/**
 * The {@code ReactiveResponse.*} settings for {@link ReactiveResponseProcessor}, resolved as a single bound object.
 *
 * <p>
 * Values are resolved from the standard {@link org.apache.juneau.commons.settings.Settings Settings} source chain
 * (system properties, environment, registered property sources) as well as any
 * {@link org.apache.juneau.commons.settings.PropertySource PropertySource} beans registered in the resource's bean
 * store, so a resource's {@code @Rest(config=...)} file can set a {@code ReactiveResponse.*} key. Relaxed key matching
 * is enabled.
 *
 * <p>
 * The bound instance is retrievable from the resource bean store via
 * {@code beanStore().getBean(ReactiveResponseProperties.class)}; registering your own instance there overrides the
 * settings.
 *
 * <h2 class='topic'>Settings</h2>
 * <ul>
 * 	<li>{@code ReactiveResponse.maxBufferedElements} &mdash; the maximum number of elements collected for a
 * 		buffered (non-streaming) response. Default {@code 10000}. A value less than {@code 1} selects the default.
 * </ul>
 *
 * @since 10.0.0
 */
@ConfigProperties(prefix = "ReactiveResponse")
@SuppressWarnings({
	"java:S9438" // Fields are bound reflectively from config sources by @ConfigProperties; making them final breaks binding.
})
public class ReactiveResponseProperties {

	/** The default value of {@link #getMaxBufferedElements()}. */
	public static final int DEFAULT_MAX_BUFFERED_ELEMENTS = 10_000;

	private int maxBufferedElements = DEFAULT_MAX_BUFFERED_ELEMENTS;

	/**
	 * The maximum number of elements a buffered response may collect.
	 *
	 * <p>
	 * When a publisher emits more elements than this limit, its subscription is cancelled and the request fails with a
	 * {@code 500} response; the response is never silently truncated.
	 *
	 * @return The limit. Always at least {@code 1}.
	 */
	public int getMaxBufferedElements() {
		return maxBufferedElements < 1 ? DEFAULT_MAX_BUFFERED_ELEMENTS : maxBufferedElements;
	}
}
