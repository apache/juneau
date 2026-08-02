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
package org.apache.juneau.rest.server.tracing;

import java.util.*;

import org.apache.juneau.http.tracing.TraceContextCarrier;
import org.apache.juneau.rest.server.*;

/**
 * SPI for recognizing a non-HTTP trace-context carrier inside a resolved {@code @RestOp} invocation.
 *
 * <p>
 * {@code RestOpInvoker} resolves every operation argument (path/query/body parameters, parsed request
 * beans, etc.) before it opens a {@code TracerHook} span. A registered {@code TraceContextExtractor}
 * bean gets a look at those resolved arguments &mdash; not just the raw {@link RestRequest} &mdash; so
 * it can recognize a domain-specific carrier (for example a parsed JSON-RPC/MCP request's nested
 * {@code params._meta} object) and hand back a {@link TraceContextCarrier} plus a {@link TraceOperation}
 * before the span is created. Neither this interface nor its default implementation depends on MCP,
 * JSON-RPC, or OpenTelemetry.
 *
 * <p>
 * Extraction only happens when a non-no-op {@code TracerHook} is active; a resource with no
 * registered {@code TracerHook} bean never resolves a {@code TraceContextExtractor} either, preserving
 * the existing zero-allocation no-op fast path.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link TraceContextCarrier}
 * 	<li class='jc'>{@link TraceOperation}
 * 	<li class='jc'>{@link TracerHook}
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestServerObservability">REST Server &mdash; Observability (Micrometer + OpenTelemetry)</a>
 * </ul>
 *
 * @since 10.0.0
 */
public interface TraceContextExtractor {

	/**
	 * Attempts to recognize a non-HTTP trace-context carrier among the resolved operation arguments.
	 *
	 * @param request The in-flight {@link RestRequest}. Never <jk>null</jk>.
	 * @param resolvedArguments The fully-resolved {@code @RestOp} handler arguments, in declaration
	 * 	order. Never <jk>null</jk>; may be empty.
	 * @return The recognized {@link TraceContextCarrier}, or {@link Optional#empty()} if this
	 * 	extractor found nothing to extract from the resolved arguments (the bridge then falls back to
	 * 	HTTP headers only). Never <jk>null</jk>.
	 */
	Optional<TraceContextCarrier> extract(RestRequest request, Object[] resolvedArguments);

	/**
	 * Derives the {@link TraceOperation} a bridge should name/attribute its span with.
	 *
	 * <p>
	 * The default implementation returns {@link TraceOperation#DEFAULT} &mdash; an extractor that
	 * overrides only {@link #extract} still satisfies {@code TracerHook.startSpan}'s three-argument
	 * overload, and the bridge keeps its own HTTP-derived span name/attributes.
	 *
	 * @param request The in-flight {@link RestRequest}. Never <jk>null</jk>.
	 * @param resolvedArguments The fully-resolved {@code @RestOp} handler arguments, in declaration
	 * 	order. Never <jk>null</jk>; may be empty.
	 * @return The derived {@link TraceOperation}. Never <jk>null</jk>.
	 */
	default TraceOperation operation(RestRequest request, Object[] resolvedArguments) {
		return TraceOperation.DEFAULT;
	}
}
