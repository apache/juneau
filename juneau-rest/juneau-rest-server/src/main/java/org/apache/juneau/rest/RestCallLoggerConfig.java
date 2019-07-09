// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Represents a set of logging rules for how to handle logging of HTTP requests/responses.
 */
public class RestCallLoggerConfig {

	private final RestCallLoggerRule[] rules;
	private final boolean disabled, debugAlways, debugPerRequest, noTraceAlways, noTracePerRequest, useStackTraceHashing;
	private final int stackTraceHashingTimeout;
	private final Level level;

	RestCallLoggerConfig(Builder b) {
		RestCallLoggerConfig p = b.parent;

		this.disabled = bool(b.disabled, p == null ? false : p.disabled);
		this.debugAlways = always(b.debug, p == null ? false : p.debugAlways);
		this.debugPerRequest = perRequest(b.debug, p == null ? false : p.debugPerRequest);
		this.noTraceAlways = always(b.noTrace, p == null ? false : p.noTraceAlways);
		this.noTracePerRequest = perRequest(b.noTrace, p == null ? false : p.noTracePerRequest);
		this.useStackTraceHashing = boolOrNum(b.stackTraceHashing, p == null ? false : p.useStackTraceHashing);
		this.stackTraceHashingTimeout = number(b.stackTraceHashing, p == null ? Integer.MAX_VALUE : p.stackTraceHashingTimeout);
		this.level = level(b.level, p == null ? Level.INFO : p.level);

		ArrayList<RestCallLoggerRule> rules = new ArrayList<>();
		if (p != null)
			rules.addAll(Arrays.asList(p.rules));
		rules.addAll(b.rules);
		this.rules = rules.toArray(new RestCallLoggerRule[rules.size()]);
	}

	private boolean always(String s, boolean def) {
		if (s == null)
			return def;
		return "always".equalsIgnoreCase(s);
	}

	private boolean perRequest(String s, boolean def) {
		if (s == null)
			return def;
		return "per-request".equalsIgnoreCase(s);
	}

	private boolean bool(String s, boolean def) {
		if (s == null)
			return def;
		return "true".equalsIgnoreCase(s);
	}

	private boolean boolOrNum(String s, boolean def) {
		if (s == null)
			return def;
		return "true".equalsIgnoreCase(s) || isNumeric(s);
	}

	private int number(String s, int def) {
		if (StringUtils.isNumeric(s)) {
			try {
				return (Integer)parseNumber(s, Integer.class);
			} catch (ParseException e) { /* Should never happen */ }
		}
		return def;
	}

	private Level level(String s, Level def) {
		if (s == null)
			return def;
		return Level.parse(s);
	}

	/**
	 * Creates a builder for this class.
	 *
	 * @return A new builder for this class.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for {@link RestCallLoggerConfig} objects.
	 */
	public static class Builder {
		List<RestCallLoggerRule> rules = new ArrayList<>();
		RestCallLoggerConfig parent;
		String stackTraceHashing, debug, noTrace, disabled, level;

		/**
		 * Sets the parent logging config.
		 *
		 * @param parent The parent logging config.
		 * @return This object (for method chaining).
		 */
		public Builder parent(RestCallLoggerConfig parent) {
			this.parent = parent;
			return this;
		}

		/**
		 * Adds a new logging rule to this config.
		 *
		 * @param rule The logging rule to add to this config.
		 * @return This object (for method chaining).
		 */
		public Builder rule(RestCallLoggerRule rule) {
			this.rules.add(rule);
			return this;
		}

		/**
		 * Adds new logging rules to this config.
		 *
		 * @param rules The logging rules to add to this config.
		 * @return This object (for method chaining).
		 */
		public Builder rules(RestCallLoggerRule...rules) {
			for (RestCallLoggerRule rule : rules)
				this.rules.add(rule);
			return this;
		}

		/**
		 * Enables debug mode on this config.
		 *
		 * <p>
		 * Debug mode causes the HTTP bodies to be cached in memory so that they can be logged.
		 *
		 * <p>
		 * Possible values (case-insensitive):
		 * <ul>
		 * 	<li><js>"always"</js> - Debug mode enabled for all requests.
		 * 	<li><js>"never"</js> - Debug mode disabled for all requests.
		 * 	<li><js>"per-request"</js> - Debug mode enabled for requests that have a <js>"X-Debug: true"</js> header.
		 * </ul>
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder debug(String value) {
			this.debug = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>debug(<js>"always"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debugAlways() {
			this.debug = "always";
			return this;
		}

		/**
		 * Shortcut for calling <c>debug(<js>"per-request"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder debugPerRequest() {
			this.debug = "per-request";
			return this;
		}

		/**
		 * Enables no-trace mode on this config.
		 *
		 * <p>
		 * No-trace mode prevents logging of messages to the log file.
		 *
		 * <p>
		 * Possible values (case-insensitive):
		 * <ul>
		 * 	<li><js>"always"</js> - No-trace mode enabled for all requests.
		 * 	<li><js>"never"</js> - No-trace mode disabled for all requests.
		 * 	<li><js>"per-request"</js> - No-trace mode enabled for requests that have a <js>"X-NoTrace: true"</js> header.
		 * </ul>
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder noTrace(String value) {
			this.noTrace = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>noTrace(<js>"always"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder noTraceAlways() {
			this.noTrace = "always";
			return this;
		}

		/**
		 * Shortcut for calling <c>noTrace(<js>"per-request"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder noTracePerRequest() {
			this.noTrace = "per-request";
			return this;
		}

		/**
		 * Enables the use of stacktrace hashing.
		 *
		 * <p>
		 * When enabled, stacktraces will be replaced with hashes in the log file.
		 *
		 * <p>
		 * Possible values (case-insensitive):
		 * <ul>
		 * 	<li><js>"true"</js> - Stacktrace-hash mode enabled for all requests.
		 * 	<li><js>"false"</js> - Stacktrace-hash mode disabled for all requests.
		 * 	<li>Numeric value - Same as <js>"true"</js> but identifies a time in milliseconds during which stack traces
		 * 		should be hashed before starting over.
		 * 		<br>Useful if you cycle your log files and want to make sure stack traces are logged at least one per day
		 * 		(for example).
		 * </ul>
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder stackTraceHashing(String value) {
			this.stackTraceHashing = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>stackTraceHashing(<js>"true"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder stackTraceHashing() {
			this.stackTraceHashing = "true";
			return this;
		}

		/**
		 * Shortcut for calling <c>stackTraceHashing(String.<jsm>valueOf</jsm>(timeout));</c>.
		 *
		 * @param timeout Time in milliseconds to hash stack traces for.
		 * @return This object (for method chaining).
		 */
		public Builder stackTraceHashingTimeout(int timeout) {
			this.stackTraceHashing = String.valueOf(timeout);
			return this;
		}

		/**
		 * Disable all logging.
		 *
		 * <p>
		 * This is equivalent to <c>noTrace(<js>"always"</js>)</c> and provided for convenience.
		 *
		 * <p>
		 * Possible values (case-insensitive):
		 * <ul>
		 * 	<li><js>"true"</js> - Stacktrace-hash mode enabled for all requests.
		 * 	<li><js>"false"</js> - Stacktrace-hash mode disabled for all requests.
		 * 	<li>Numeric value - Same as <js>"true"</js> but identifies a time in milliseconds during which stack traces
		 * 		should be hashed before starting over.
		 * 		<br>Useful if you cycle your log files and want to make sure stack traces are logged at least one per day
		 * 		(for example).
		 * </ul>
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder disabled(String value) {
			this.disabled = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>disabled(<js>"true"</js>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder disabled() {
			this.disabled = "true";
			return this;
		}

		/**
		 * The default logging level.
		 *
		 * <p>
		 * This defines the logging level for messages if they're not already defined on the matched rule.
		 *
		 * <p>
		 * If not specified, <js>"INFO"</js> is used.
		 *
		 * <p>
		 * See {@link Level} for possible values.
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder level(String value) {
			this.level = value;
			return this;
		}

		/**
		 * The default logging level.
		 *
		 * <p>
		 * This defines the logging level for messages if they're not already defined on the matched rule.
		 *
		 * <p>
		 * If not specified, <js>"INFO"</js> is used.
		 *
		 * <p>
		 * See {@link Level} for possible values.
		 *
		 * @param value The value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder level(Level value) {
			this.level = value == null ? null : value.getName();
			return this;
		}

		/**
		 * Creates the {@link RestCallLoggerConfig} object based on settings on this builder.
		 *
		 * @return A new {@link RestCallLoggerConfig} object.
		 */
		public RestCallLoggerConfig build() {
			return new RestCallLoggerConfig(this);
		}
	}

	/**
	 * Given the specified servlet request/response, find the rule that applies to it.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @return The applicable logging rule, or <jk>null<jk> if a match could not be found.
	 */
	public RestCallLoggerRule getRule(HttpServletRequest req, HttpServletResponse res) {
		if (isNoTrace(req))
			return null;

		int status = res.getStatus();
		Throwable e = (Throwable)req.getAttribute("Exception");
		boolean debug = isDebug(req);

		for (RestCallLoggerRule r : rules)
			if (r.matches(status, debug, e))
				return r;

		return null;
	}

	/**
	 * Returns <jk>true</jk> if the current request has debug enabled.
	 *
	 * @param req The request to check.
	 * @return <jk>true</jk> if the current request has debug enabled.
	 */
	public boolean isDebug(HttpServletRequest req) {
		if (debugAlways)
			return true;
		if (debugPerRequest) {
			if ("true".equalsIgnoreCase(req.getHeader("X-Debug")))
				return true;
			Boolean b = boolAttr(req, "Debug");
			if (b != null && b == true)
				return true;
		}
		return false;
	}

	private boolean isNoTrace(HttpServletRequest req) {
		if (disabled || noTraceAlways)
			return true;
		if (noTracePerRequest) {
			if ("true".equalsIgnoreCase(req.getHeader("X-NoTrace")))
				return true;
			Boolean b = boolAttr(req, "NoTrace");
			if (b != null && b == true)
				return true;
		}
		return false;
	}

	/**
	 * Returns the default logging level.
	 *
	 * @return The default logging level.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Returns <jk>true</jk> if stack traces should be hashed.
	 *
	 * @return <jk>true</jk> if stack traces should be hashed.
	 */
	public boolean useStackTraceHashing() {
		return useStackTraceHashing;
	}

	/**
	 * Returns the time in milliseconds that stacktrace hashes should be persisted.
	 *
	 * @return The time in milliseconds that stacktrace hashes should be persisted.
	 */
	public int getStackTraceHashingTimeout() {
		return stackTraceHashingTimeout;
	}

	private Boolean boolAttr(HttpServletRequest req, String name) {
		Object o = req.getAttribute(name);
		if (o == null || ! (o instanceof Boolean))
			return null;
		return (Boolean)o;
	}
}
