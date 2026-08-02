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
package org.apache.juneau.http.tracing;

/**
 * Neutral, tracer-agnostic carrier of W3C trace-context fields ({@code traceparent} /
 * {@code tracestate} / {@code baggage}) from a source that need not be an HTTP request.
 *
 * <p>
 * {@code juneau-rest-server} has exactly one built-in carrier source &mdash; HTTP request headers,
 * read directly by the OpenTelemetry bridge's {@code RestRequestTextMapGetter}. This interface exists
 * so a <i>second</i> carrier source &mdash; for example a JSON-RPC/MCP request's nested
 * {@code params._meta} object &mdash; can be read/written through the exact same shape a
 * {@code TracerHook} bridge already knows how to consume, without that bridge (or this module)
 * depending on the concept of MCP, JSON-RPC, or any specific wire format.
 *
 * <p>
 * A single carrier instance may also be a <i>composite</i> that consults more than one underlying
 * source with a defined precedence (for example: an explicit metadata key wins, an absent key falls
 * back to the equivalent HTTP header). This interface makes no assumption about how many underlying
 * sources a given implementation consults &mdash; only that {@link #get(String)} return the
 * effective value for a key after any such precedence is applied.
 *
 * <p>
 * Implementations are read/write: {@link #get(String)} and {@link #keys()} support inbound context
 * extraction (a {@code TracerHook} bridge reading a remote parent out of the carrier);
 * {@link #set(String, String)} supports outbound context injection (a bridge or
 * {@code TraceContexts#inject(TracerHook, TraceContextCarrier)} writing the current trace context
 * into the carrier for a caller to read back).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@code org.apache.juneau.rest.server.tracing.TraceContextExtractor}
 * 	<li class='jc'>{@code org.apache.juneau.rest.server.tracing.TraceContexts}
 * 	<li class='jc'>{@code org.apache.juneau.rest.server.tracing.TracerHook}
 * 	<li class='link'><a class="doclink" href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface TraceContextCarrier {

	/**
	 * Returns the effective value for the given key.
	 *
	 * @param key The carrier key (e.g. {@code "traceparent"}, {@code "tracestate"}, {@code "baggage"}).
	 * 	Never <jk>null</jk>.
	 * @return The value for <c>key</c>, or <jk>null</jk> if the key is absent from every underlying
	 * 	source the carrier consults.
	 */
	String get(String key);

	/**
	 * Returns every key this carrier can resolve through {@link #get(String)}.
	 *
	 * <p>
	 * For a composite carrier, this is the union of the keys available across every underlying
	 * source &mdash; not just the source that happens to win precedence for any single key.
	 *
	 * @return The carrier's keys. Never <jk>null</jk>; empty if the carrier currently has no keys.
	 */
	Iterable<String> keys();

	/**
	 * Writes a value for the given key.
	 *
	 * <p>
	 * Used for outbound context injection &mdash; a {@code TracerHook} bridge rendering the current
	 * trace context into the carrier so a caller can read it back (for example, an MCP adapter
	 * copying the rendered {@code traceparent} into a JSON-RPC result's {@code _meta}).
	 *
	 * @param key The carrier key (e.g. {@code "traceparent"}, {@code "tracestate"}, {@code "baggage"}).
	 * 	Never <jk>null</jk>.
	 * @param value The value to write. Never <jk>null</jk>.
	 */
	void set(String key, String value);
}
