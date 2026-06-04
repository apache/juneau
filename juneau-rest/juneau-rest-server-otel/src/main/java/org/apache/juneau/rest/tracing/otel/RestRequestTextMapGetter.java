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
package org.apache.juneau.rest.tracing.otel;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.rest.*;

import io.opentelemetry.context.propagation.*;

/**
 * {@link TextMapGetter} implementation that reads HTTP request headers from a {@link RestRequest}.
 *
 * <p>
 * Used by {@link OtelTracerHook} to extract W3C {@code traceparent} / {@code tracestate} headers (and
 * any other propagator-defined headers, e.g. baggage) from incoming requests so the server span can
 * be created as a child of the caller's trace context.
 *
 * <p>
 * Header lookups are case-insensitive at the underlying {@code RequestHeaderList}, matching the
 * RFC 9110 contract.
 *
 * @since 10.0.0
 */
@SuppressWarnings({
	"java:S6548" // Singleton pattern is intentional; INSTANCE is a stateless, thread-safe TextMapGetter implementation.
})
public final class RestRequestTextMapGetter implements TextMapGetter<RestRequest> {

	/** Process-wide singleton instance. */
	public static final RestRequestTextMapGetter INSTANCE = new RestRequestTextMapGetter();

	private RestRequestTextMapGetter() {}

	@Override /* TextMapGetter */
	public Iterable<String> keys(RestRequest carrier) {
		if (carrier == null)
			return List.of();
		return carrier.getHeaders().getNames();
	}

	@Override /* TextMapGetter */
	public String get(RestRequest carrier, String key) {
		if (carrier == null || key == null)
			return null;
		var header = carrier.getHeaderParam(key);
		if (! header.isPresent())
			return null;
		var value = header.orElse(null);
		return isEmpty(value) ? null : value;
	}
}
