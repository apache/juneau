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
package org.apache.juneau.rest.server.tracing.otel;

import java.util.*;

import org.apache.juneau.rest.server.tracing.*;

import io.opentelemetry.context.propagation.*;

/**
 * {@link TextMapGetter} implementation that reads W3C trace-context fields from a neutral
 * {@link TraceContextCarrier} &mdash; for example an MCP request's {@code params._meta} object
 * &mdash; instead of HTTP request headers.
 *
 * <p>
 * {@link OtelTracerHook} chains an extraction pass through this getter after an ordinary HTTP-header
 * extraction (via {@link RestRequestTextMapGetter}) so an explicit carrier value wins while an
 * absent/invalid one falls back to whatever the HTTP-header extraction already produced. This relies
 * entirely on the standard {@link TextMapPropagator} contract &mdash; extraction that finds nothing
 * usable for a given concern returns the supplied {@link io.opentelemetry.context.Context} unmodified
 * &mdash; so no bespoke precedence-merging logic lives here or in {@link OtelTracerHook}.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S6548" // Singleton pattern is intentional; INSTANCE is a stateless, thread-safe TextMapGetter implementation.
})
public final class TraceContextCarrierTextMapGetter implements TextMapGetter<TraceContextCarrier> {

	/** Process-wide singleton instance. */
	public static final TraceContextCarrierTextMapGetter INSTANCE = new TraceContextCarrierTextMapGetter();

	private TraceContextCarrierTextMapGetter() {}

	@Override /* TextMapGetter */
	public Iterable<String> keys(TraceContextCarrier carrier) {
		if (carrier == null)
			return List.of();
		var keys = carrier.keys();
		return keys == null ? List.of() : keys;
	}

	@Override /* TextMapGetter */
	public String get(TraceContextCarrier carrier, String key) {
		if (carrier == null || key == null)
			return null;
		var value = carrier.get(key);
		return (value == null || value.isEmpty()) ? null : value;
	}
}
