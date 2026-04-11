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

import java.util.function.*;

/**
 * Convenience {@link RestLogger} implementation that routes log entries to a {@link java.lang.System.Logger}.
 *
 * <p>
 * Uses the JDK 9+ {@link java.lang.System.Logger} facade, which automatically routes to whatever logging framework is on
 * the classpath (SLF4J, Log4j 2, Logback, JUL) with no additional dependency.
 *
 * <h5 class='section'>Example — shorthand:</h5>
 * <p class='bjava'>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.logger(BasicRestLogger.<jsm>of</jsm>(System.<jsm>getLogger</jsm>(<js>"myapp.http"</js>)))
 * 		.build();
 * </p>
 *
 * <h5 class='section'>Example — custom level resolver and template:</h5>
 * <p class='bjava'>
 * 	NgRestClient <jv>client</jv> = NgRestClient.<jsm>builder</jsm>()
 * 		.logger(BasicRestLogger.<jsm>create</jsm>()
 * 			.logger(System.<jsm>getLogger</jsm>(<js>"myapp.http"</js>))
 * 			.infoTemplate(<js>"{method} {uri} -> {status} ({elapsed})"</js>)
 * 			.errorTemplate(<js>"{method} {uri} -> {error} ({elapsed})"</js>)
 * 			.build())
 * 		.build();
 * </p>
 *
 * <p>
 * <b>Default behavior</b> (no filter, all requests logged, using {@link RestLogLevelResolver#DEFAULT}):
 * <ul>
 * 	<li>{@code INFO} → {@code "GET https://api.example.com/users -> 200 OK (42ms)"}
 * 	<li>{@code WARNING} → {@code "GET https://api.example.com/users -> 404 Not Found (8ms)"}
 * 	<li>{@code ERROR} → {@code "GET https://api.example.com/users -> connection refused (12ms)"} + exception
 * </ul>
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
public final class BasicRestLogger implements RestLogger {

	private static final String DEFAULT_INFO_TEMPLATE    = "{method} {uri} -> {status} {reason} ({elapsed})";
	private static final String DEFAULT_WARNING_TEMPLATE = "{method} {uri} -> {status} {reason} ({elapsed})";
	private static final String DEFAULT_ERROR_TEMPLATE   = "{method} {uri} -> {error} ({elapsed})";
	private static final String DEFAULT_DEBUG_TEMPLATE   =
		"--> {method} {uri}\n  (elapsed: {elapsed}, status: {status} {reason})";

	private final System.Logger logger;
	private final RestLogLevelResolver levelResolver;
	private final Predicate<RestLogEntry> filter;
	private final String infoTemplate;
	private final String warningTemplate;
	private final String errorTemplate;
	private final String debugTemplate;

	private BasicRestLogger(Builder builder) {
		this.logger = builder.logger;
		this.levelResolver = builder.levelResolver;
		this.filter = builder.filter;
		this.infoTemplate = builder.infoTemplate;
		this.warningTemplate = builder.warningTemplate;
		this.errorTemplate = builder.errorTemplate;
		this.debugTemplate = builder.debugTemplate;
	}

	/**
	 * Creates a {@link BasicRestLogger} using {@link RestLogLevelResolver#DEFAULT} and default templates.
	 *
	 * @param logger The target {@link java.lang.System.Logger}. Must not be <jk>null</jk>.
	 * @return A new instance. Never <jk>null</jk>.
	 */
	public static BasicRestLogger of(System.Logger logger) {
		return create().logger(logger).build();
	}

	/**
	 * Returns a new builder for full configuration.
	 *
	 * @return A new builder. Never <jk>null</jk>.
	 */
	public static Builder create() {
		return new Builder();
	}

	@Override /* RestLogger */
	public void log(RestLogEntry entry) {
		if (filter != null && !filter.test(entry))
			return;

		if (entry.isDebug()) {
			logger.log(System.Logger.Level.DEBUG, entry.format(debugTemplate));
			return;
		}

		// Use this logger's own resolver (may differ from the DEFAULT used to compute entry.getLevel())
		var level = levelResolver.resolve(entry);

		var template = switch (level) {
			case ERROR   -> errorTemplate;
			case WARNING -> warningTemplate;
			default      -> infoTemplate;
		};

		var message = entry.format(template);

		if (level == System.Logger.Level.ERROR && entry.getError() != null)
			logger.log(level, message, entry.getError());
		else
			logger.log(level, message);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Builder
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Fluent builder for {@link BasicRestLogger}.
	 *
	 * @since 9.2.1
	 */
	public static final class Builder {

		System.Logger logger;
		RestLogLevelResolver levelResolver = RestLogLevelResolver.DEFAULT;
		Predicate<RestLogEntry> filter;
		String infoTemplate = DEFAULT_INFO_TEMPLATE;
		String warningTemplate = DEFAULT_WARNING_TEMPLATE;
		String errorTemplate = DEFAULT_ERROR_TEMPLATE;
		String debugTemplate = DEFAULT_DEBUG_TEMPLATE;

		private Builder() {}

		/**
		 * Sets the target {@link java.lang.System.Logger} to emit log messages to.
		 *
		 * @param value The logger. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder logger(System.Logger value) {
			logger = value;
			return this;
		}

		/**
		 * Sets the level resolver used to compute the log level for each entry.
		 *
		 * <p>
		 * Defaults to {@link RestLogLevelResolver#DEFAULT}.
		 *
		 * @param value The resolver. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder levelResolver(RestLogLevelResolver value) {
			levelResolver = value;
			return this;
		}

		/**
		 * Sets a filter that controls which entries are logged.
		 *
		 * <p>
		 * Only entries for which the predicate returns {@code true} will be logged.
		 * By default, all entries are logged.
		 *
		 * @param value The filter predicate. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder filter(Predicate<RestLogEntry> value) {
			filter = value;
			return this;
		}

		/**
		 * Sets the message template used for {@link java.lang.System.Logger.Level#INFO} entries.
		 *
		 * @param value The template string. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder infoTemplate(String value) {
			infoTemplate = value;
			return this;
		}

		/**
		 * Sets the message template used for {@link java.lang.System.Logger.Level#WARNING} entries.
		 *
		 * @param value The template string. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder warningTemplate(String value) {
			warningTemplate = value;
			return this;
		}

		/**
		 * Sets the message template used for {@link java.lang.System.Logger.Level#ERROR} entries.
		 *
		 * @param value The template string. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder errorTemplate(String value) {
			errorTemplate = value;
			return this;
		}

		/**
		 * Sets the message template used for debug entries (when {@link RestLogEntry#isDebug()} is {@code true}).
		 *
		 * @param value The template string. Must not be <jk>null</jk>.
		 * @return This object.
		 */
		public Builder debugTemplate(String value) {
			debugTemplate = value;
			return this;
		}

		/**
		 * Builds and returns the {@link BasicRestLogger}.
		 *
		 * @return A new instance. Never <jk>null</jk>.
		 */
		public BasicRestLogger build() {
			return new BasicRestLogger(this);
		}
	}
}
