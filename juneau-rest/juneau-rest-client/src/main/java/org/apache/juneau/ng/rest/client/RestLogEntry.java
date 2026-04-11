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
package org.apache.juneau.ng.rest.client;

import java.time.*;

/**
 * Structured log entry produced after every {@link NgRestRequest#run()} invocation.
 *
 * <p>
 * Passed to a {@link RestLogger} in the {@code finally} block of {@link NgRestRequest#run()}.
 * When the transport fails before a response is received, {@link #getResponse()} is {@code null}
 * and {@link #getError()} is non-{@code null}.
 *
 * <p>
 * <b>Beta — API subject to change:</b> This type is part of the next-generation REST client and HTTP stack
 * ({@code org.apache.juneau.ng.*}).
 * It is not API-frozen: binary- and source-incompatible changes may appear in the <b>next major</b> Juneau release
 * (and possibly earlier).
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/juneau-ng-rest-client">juneau-ng REST client</a>
 * </ul>
 *
 * @since 9.2.1
 */
public final class RestLogEntry {

	private final NgRestRequest request;

	@SuppressWarnings({
		"resource" // Eclipse resource analysis: response is borrowed for logging snapshot only
	})
	private final NgRestResponse response;
	private final Throwable error;
	private final Duration elapsed;
	private final boolean debug;
	private final System.Logger.Level level;

	RestLogEntry(NgRestRequest request, NgRestResponse response, Throwable error, Duration elapsed, boolean debug, System.Logger.Level level) {
		this.request = request;
		this.response = response;
		this.error = error;
		this.elapsed = elapsed;
		this.debug = debug;
		this.level = level;
	}

	/**
	 * Returns the request that triggered this log entry.
	 *
	 * @return The request. Never <jk>null</jk>.
	 */
	public NgRestRequest getRequest() {
		return request;
	}

	/**
	 * Returns the response received, or {@code null} if the transport failed before a response was received.
	 *
	 * @return The response, or <jk>null</jk>.
	 */
	public NgRestResponse getResponse() {
		return response;
	}

	/**
	 * Returns the error that occurred, or {@code null} on success.
	 *
	 * @return The error, or <jk>null</jk>.
	 */
	public Throwable getError() {
		return error;
	}

	/**
	 * Returns the elapsed time for the request (from before {@code onInit} to after the response is received).
	 *
	 * @return The elapsed time. Never <jk>null</jk>.
	 */
	public Duration getElapsed() {
		return elapsed;
	}

	/**
	 * Returns {@code true} if {@link NgRestRequest#debug()} was called on the originating request.
	 *
	 * @return {@code true} if this is a debug request.
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * Returns the log level computed by the configured {@link RestLogLevelResolver}.
	 *
	 * @return The log level. Never <jk>null</jk>.
	 */
	public System.Logger.Level getLevel() {
		return level;
	}

	/**
	 * Returns {@code true} if the call resulted in an error (transport error or HTTP status ≥ 400).
	 *
	 * @return {@code true} if this entry represents an error condition.
	 */
	public boolean isError() {
		return error != null || getStatusCode() >= 400;
	}

	/**
	 * Returns the HTTP status code, or {@code 0} if no response was received.
	 *
	 * @return The status code.
	 */
	public int getStatusCode() {
		return response != null ? response.getStatusCode() : 0;
	}

	/**
	 * Returns {@code true} if the response contains a header with the given name (case-insensitive).
	 *
	 * @param name The header name. Must not be <jk>null</jk>.
	 * @return {@code true} if the header is present.
	 */
	public boolean hasResponseHeader(String name) {
		return response != null && response.getFirstHeader(name) != null;
	}

	/**
	 * Formats the entry using the default template: {@code "METHOD URI -> STATUS REASON (ELAPSEDms)"}.
	 *
	 * @return A formatted string. Never <jk>null</jk>.
	 */
	public String format() {
		return format("{method} {uri} -> {status} {reason} ({elapsed})");
	}

	/**
	 * Formats the entry using a named-placeholder template.
	 *
	 * <p>
	 * Supported placeholders:
	 * <ul>
	 * 	<li>{@code {method}} — HTTP method (e.g. {@code GET})
	 * 	<li>{@code {uri}} — Full request URI
	 * 	<li>{@code {status}} — Response status code, or {@code 0} if no response
	 * 	<li>{@code {reason}} — Response reason phrase, or empty string
	 * 	<li>{@code {elapsed}} — Elapsed time (e.g. {@code 42ms})
	 * 	<li>{@code {error}} — Error message, or empty string if no error
	 * </ul>
	 *
	 * @param template The template string. Must not be <jk>null</jk>.
	 * @return A formatted string. Never <jk>null</jk>.
	 */
	public String format(String template) {
		var method = request.getMethod();
		var uri = request.getUri() != null ? request.getUri().toString() : ""; // HTT: uri is always set when format() is called from logger
		var status = getStatusCode() > 0 ? String.valueOf(getStatusCode()) : "0";
		var reason = response != null && response.getReasonPhrase() != null ? response.getReasonPhrase() : "";
		var elapsedMs = elapsed.toMillis() + "ms";
		var errorMsg = error != null ? error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName() : "";

		return template
			.replace("{method}", method)
			.replace("{uri}", uri)
			.replace("{status}", status)
			.replace("{reason}", reason)
			.replace("{elapsed}", elapsedMs)
			.replace("{error}", errorMsg);
	}

	@Override /* Object */
	public String toString() {
		return format();
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@link RestLogEntry}.
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		NgRestRequest request;

		@SuppressWarnings({
			"resource" // Eclipse resource analysis: builder holds reference only; entry does not own response lifecycle
		})
		NgRestResponse response;
		Throwable error;
		Duration elapsed = Duration.ZERO;
		boolean debug;
		RestLogLevelResolver levelResolver = RestLogLevelResolver.DEFAULT;

		private Builder() {}

		/**
		 * Sets the request.
		 *
		 * @param value The request. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder request(NgRestRequest value) {
			request = value;
			return this;
		}

		/**
		 * Sets the response.
		 *
		 * @param value The response. May be <jk>null</jk> if transport failed before receiving a response.
		 * @return This object.
		 */
		public Builder response(NgRestResponse value) {
			response = value;
			return this;
		}

		/**
		 * Sets the error.
		 *
		 * @param value The error. May be <jk>null</jk> on success.
		 * @return This object.
		 */
		public Builder error(Throwable value) {
			error = value;
			return this;
		}

		/**
		 * Sets the elapsed time.
		 *
		 * @param value The elapsed duration. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder elapsed(Duration value) {
			elapsed = value;
			return this;
		}

		/**
		 * Sets whether this was a debug request.
		 *
		 * @param value {@code true} if {@link NgRestRequest#debug()} was called.
		 * @return This object.
		 */
		public Builder debug(boolean value) {
			debug = value;
			return this;
		}

		/**
		 * Sets the level resolver used to compute {@link RestLogEntry#getLevel()}.
		 *
		 * @param value The resolver. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder levelResolver(RestLogLevelResolver value) {
			levelResolver = value;
			return this;
		}

		/**
		 * Builds the {@link RestLogEntry}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public RestLogEntry build() {
			var entry = new RestLogEntry(request, response, error, elapsed, debug, null);
			return new RestLogEntry(request, response, error, elapsed, debug, levelResolver.resolve(entry));
		}
	}
}
