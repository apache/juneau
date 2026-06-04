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
package org.apache.juneau.rest.tracing;

/**
 * Handle to one in-flight {@link TracerHook}-managed span.
 *
 * <p>
 * Returned by {@link TracerHook#startSpan(org.apache.juneau.rest.RestRequest)}. The framework drives
 * the {@code setStatusCode} / {@code setError} / {@code close} transitions inside a {@code finally}
 * block so the span always closes &mdash; including on exception paths.
 *
 * <p>
 * Bridge implementations typically wrap the downstream tracer's native span / scope. The OpenTelemetry
 * bridge in {@code juneau-rest-server-otel}, for example, wraps an
 * {@code io.opentelemetry.api.trace.Span} plus its {@code io.opentelemetry.context.Scope}: setters
 * translate to span attribute / status writes; {@code close()} ends the span and closes the OTel
 * scope.
 *
 * @since 10.0.0
 */
public interface Scope extends AutoCloseable {

	/**
	 * Records the HTTP response status code on the span.
	 *
	 * <p>
	 * Called by the framework after the {@code @RestOp} handler completes &mdash; before
	 * {@link #close()}, and before {@link #setError(Throwable)} if the handler also threw. Bridges
	 * typically translate the status into both a numeric attribute (e.g. OpenTelemetry's
	 * {@code http.response.status_code}) and an overall span status (HTTP {@code 5xx} maps to
	 * span-status {@code ERROR}; other codes map to {@code UNSET} per the OTel HTTP semantic
	 * conventions).
	 *
	 * @param statusCode The HTTP status code (e.g. {@code 200}, {@code 404}, {@code 500}).
	 */
	void setStatusCode(int statusCode);

	/**
	 * Records the exception (if any) the {@code @RestOp} handler threw on the span.
	 *
	 * <p>
	 * Called by the framework only when the handler threw &mdash; immediately before {@link #close()}.
	 * Bridges typically record the throwable as a span event with the stack trace, set the span status
	 * to {@code ERROR}, and add an {@code exception.type} attribute carrying the exception's
	 * simple-name.
	 *
	 * @param error The exception thrown by the handler. Never <jk>null</jk> when this method is called.
	 */
	void setError(Throwable error);

	/**
	 * Closes the span.
	 *
	 * <p>
	 * Always called by the framework &mdash; via the standard {@code finally}-block contract on
	 * {@link AutoCloseable} &mdash; whether the handler returned normally or threw. Implementations
	 * <b>must not</b> throw checked exceptions; the close path runs in a {@code finally} where masking
	 * the original handler exception is unsafe. Implementations that need to surface bridge-level
	 * errors should log + swallow instead.
	 */
	@Override
	void close();
}
