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
package org.apache.juneau.rest.metrics;

import java.time.*;

/**
 * Default {@link MetricsRecorder} implementation that drops every event on the floor.
 *
 * <p>
 * Used whenever no consumer-provided {@code MetricsRecorder} bean is reachable from the
 * {@code RestContext}'s bean store. Ensures the {@link MetricsRecorder} contract is always satisfied
 * &mdash; the framework can call {@code record(...)} unconditionally without a null-check.
 *
 * <p>
 * The implementation is a single static-field constant ({@link #INSTANCE}); the {@code record(...)}
 * method does nothing, allocates nothing, and JIT-inlines to an empty method body, so the
 * off-by-default path has no observable runtime cost.
 *
 * @since 9.5.0
 */
public final class NoOpMetricsRecorder implements MetricsRecorder {

	/** Process-wide singleton instance. */
	public static final NoOpMetricsRecorder INSTANCE = new NoOpMetricsRecorder();

	private NoOpMetricsRecorder() {}

	@Override /* MetricsRecorder */
	public void record(String opName, String httpMethod, String uriTemplate, int statusCode, Duration elapsed, Throwable error, String metricName, String metricTags) {
		// Intentionally empty — the default off-by-default behaviour is to drop every event.
	}
}
