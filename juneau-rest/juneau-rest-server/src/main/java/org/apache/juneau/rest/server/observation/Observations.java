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
package org.apache.juneau.rest.server.observation;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

/**
 * Static facade over an explicitly-installed default {@link Observer} for ergonomic custom
 * (non-request) observations.
 *
 * <p>
 * {@code juneau-rest-server} has no method-interception / AOP substrate, so custom observations are
 * <b>explicit</b>: wrap the block of work in a try-with-resources over {@link #observe(String, String)}.
 * This mirrors how the request path is instrumented (the framework opens / closes the observation around
 * the {@code @RestOp} handler) but lets application code instrument arbitrary work.
 *
 * <h5 class='topic'>Installing a backend</h5>
 *
 * <p>
 * The default observer is {@link Observer#NOOP} until a backend is installed via {@link #install(Observer)}
 * &mdash; typically once at startup, after the application's {@code MetricsRecorder} / {@code TracerHook}
 * beans are known. Until then (and in any process that never installs one) {@link #observe(String, String)}
 * hands back {@link Observation#NOOP} and costs nothing.
 *
 * <p class='bjava'>
 * 	<jc>// At startup, once the observability beans are built:</jc>
 * 	Observations.<jsm>install</jsm>(<jk>new</jk> Observer(<jv>metricsRecorder</jv>, <jv>tracerHook</jv>));
 *
 * 	<jc>// Anywhere in application code:</jc>
 * 	<jk>try</jk> (Observation <jv>o</jv> = Observations.<jsm>observe</jsm>(<js>"order.load"</js>, <js>"team=payments"</js>)) {
 * 		<jk>return</jk> loadOrder(<jv>id</jv>);
 * 	} <jk>catch</jk> (RuntimeException <jv>e</jv>) {
 * 		<jv>o</jv>.setError(<jv>e</jv>);
 * 		<jk>throw</jk> <jv>e</jv>;
 * 	}
 * </p>
 *
 * <p>
 * Code that prefers explicit dependency injection over a global can hold an {@link Observer} directly
 * (resolved from the {@code RestContext} bean store) and call {@link Observer#start(String, String)} &mdash;
 * this facade is a convenience, not the only entry point.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Observer}
 * 	<li class='jc'>{@link Observation}
 * </ul>
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"resource" // observe(...) returns the Observation handle for the caller's try-with-resources; closing it here is wrong.
})
public class Observations {

	private static final String ARG_observer = "observer";

	private static volatile Observer defaultObserver = Observer.NOOP;

	private Observations() {}

	/**
	 * Installs the process-wide default {@link Observer} used by {@link #observe(String, String)}.
	 *
	 * <p>
	 * Intended to be called once at application startup. Passing {@link Observer#NOOP} (or calling
	 * {@link #reset()}) restores the no-op default.
	 *
	 * @param observer The observer to install. Must not be <jk>null</jk>.
	 */
	public static void install(Observer observer) {
		defaultObserver = assertArgNotNull(ARG_observer, observer);
	}

	/**
	 * Resets the process-wide default observer back to {@link Observer#NOOP}.
	 */
	public static void reset() {
		defaultObserver = Observer.NOOP;
	}

	/**
	 * Returns the currently-installed default {@link Observer}.
	 *
	 * @return The default observer; {@link Observer#NOOP} if none was installed. Never <jk>null</jk>.
	 */
	public static Observer observer() {
		return defaultObserver;
	}

	/**
	 * Starts a custom observation against the installed default {@link Observer}.
	 *
	 * @param name The observation name &mdash; used as both the span name and the metric timer name.
	 * 	Must not be <jk>null</jk> or blank.
	 * @param tags Additional metric tags as comma-separated {@code key=value} pairs, or <jk>null</jk> /
	 * 	empty for none.
	 * @return The started {@link Observation} (a try-with-resources handle). Never <jk>null</jk>.
	 */
	public static Observation observe(String name, String tags) {
		return defaultObserver.start(name, tags);
	}

	/**
	 * Starts a custom observation with no additional tags.
	 *
	 * @param name The observation name. Must not be <jk>null</jk> or blank.
	 * @return The started {@link Observation}. Never <jk>null</jk>.
	 */
	public static Observation observe(String name) {
		return defaultObserver.start(name, "");
	}
}
