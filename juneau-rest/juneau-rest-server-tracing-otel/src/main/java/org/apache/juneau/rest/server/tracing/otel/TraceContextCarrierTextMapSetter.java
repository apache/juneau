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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.rest.server.tracing.*;

import io.opentelemetry.context.propagation.*;

/**
 * {@link TextMapSetter} implementation that writes W3C trace-context fields into a neutral
 * {@link TraceContextCarrier} instead of an HTTP response.
 *
 * <p>
 * The adapter substrate for {@link OtelTracerHook#inject(TraceContextCarrier)}: the configured
 * {@link TextMapPropagator} renders the caller's currently-active trace context (and baggage, when a
 * baggage propagator is configured) by calling {@link #set(TraceContextCarrier, String, String)} once
 * per key, which this class forwards to {@link TraceContextCarrier#set(String, String)}. A propagator
 * with nothing valid to render (no active span context, empty baggage, etc.) simply never calls
 * {@link #set(TraceContextCarrier, String, String)}, so the carrier is left untouched &mdash; the
 * substrate for {@link TracerHook}'s documented default no-op {@code inject(...)} contract.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S6548" // Singleton pattern is intentional; INSTANCE is a stateless, thread-safe TextMapSetter implementation.
})
public final class TraceContextCarrierTextMapSetter implements TextMapSetter<TraceContextCarrier> {

	/** Process-wide singleton instance. */
	public static final TraceContextCarrierTextMapSetter INSTANCE = new TraceContextCarrierTextMapSetter();

	private TraceContextCarrierTextMapSetter() {}

	@Override /* TextMapSetter */
	public void set(TraceContextCarrier carrier, String key, String value) {
		if (nn(carrier) && nn(key) && nn(value))
			carrier.set(key, value);
	}
}
