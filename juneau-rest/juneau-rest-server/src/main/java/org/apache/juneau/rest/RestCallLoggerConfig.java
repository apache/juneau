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

import static org.apache.juneau.rest.Enablement.*;

import java.util.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;

/**
 * Represents a set of logging rules for how to handle logging of HTTP requests/responses.
 */
public class RestCallLoggerConfig {

	/**
	 * Default empty logging config.
	 */
	public static final RestCallLoggerConfig DEFAULT = RestCallLoggerConfig.create().build();

	/**
	 * Default debug logging config.
	 */
	public static final RestCallLoggerConfig DEFAULT_DEBUG =
		RestCallLoggerConfig
			.create()
			.useStackTraceHashing(false)
			.level(Level.WARNING)
			.rules(
				RestCallLoggerRule
					.create()
					.codes("*")
					.verbose()
					.build()
			)
			.build();

	private final RestCallLoggerRule[] rules;
	private final boolean useStackTraceHashing;
	private final Enablement disabled;
	private final int stackTraceHashingTimeout;
	private final Level level;

	RestCallLoggerConfig(Builder b) {
		RestCallLoggerConfig p = b.parent;

		this.disabled = b.disabled != null ? b.disabled : p != null ? p.disabled : FALSE;
		this.useStackTraceHashing = b.useStackTraceHashing != null ? b.useStackTraceHashing : p != null ? p.useStackTraceHashing : false;
		this.stackTraceHashingTimeout = b.stackTraceHashingTimeout != null ? b.stackTraceHashingTimeout : p != null ? p.stackTraceHashingTimeout : Integer.MAX_VALUE;
		this.level = b.level != null ? b.level : p != null ? p.level : Level.INFO;

		ArrayList<RestCallLoggerRule> rules = new ArrayList<>();
		rules.addAll(b.rules);
		if (p != null)
			rules.addAll(Arrays.asList(p.rules));
		this.rules = rules.toArray(new RestCallLoggerRule[rules.size()]);
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
		Level level;
		Boolean useStackTraceHashing;
		Enablement disabled;
		Integer stackTraceHashingTimeout;

		/**
		 * Sets the parent logging config.
		 *
		 * @param parent
		 * 	The parent logging config.
		 * 	<br>Can be <jk>null</jk>.
		 * @return This object (for method chaining).
		 */
		public Builder parent(RestCallLoggerConfig parent) {
			this.parent = parent;
			return this;
		}

		/**
		 * Adds a new logging rule to this config.
		 *
		 * <p>
		 * 	The rule will be added to the END of list of current rules and thus checked last in the current list but
		 * 	before any parent rules.
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
		 * <p>
		 * 	The rules will be added in order to the END of list of current rules and thus checked last in the current list but
		 * 	before any parent rules.
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
		 * Enables no-trace mode on this config.
		 *
		 * <p>
		 * No-trace mode prevents logging of messages to the log file.
		 *
		 * <p>
		 * Possible values (case-insensitive):
		 * <ul>
		 * 	<li><{@link Enablement#TRUE TRUE} - No-trace mode enabled for all requests.
		 * 	<li><{@link Enablement#FALSE FALSE} - No-trace mode disabled for all requests.
		 * 	<li><{@link Enablement#PER_REQUEST PER_REQUEST} - No-trace mode enabled for requests that have a <js>"X-NoTrace: true"</js> header.
		 * </ul>
		 *
		 * @param value
		 * 	The value for this property.
		 * 	<br>Can be <jk>null</jk> (inherit from parent or default to {@link Enablement#FALSE NEVER}).
		 * @return This object (for method chaining).
		 */
		public Builder disabled(Enablement value) {
			this.disabled = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>disabled(<jsf>TRUE</jsf>)</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder disabled() {
			return disabled(TRUE);
		}

		/**
		 * Enables the use of stacktrace hashing.
		 *
		 * <p>
		 * When enabled, stacktraces will be replaced with hashes in the log file.
		 *
		 * @param value
		 * 	The value for this property.
		 * 	<br>Can be <jk>null</jk> (inherit from parent or default to <jk>false</jk>).
		 * @return This object (for method chaining).
		 */
		public Builder useStackTraceHashing(Boolean value) {
			this.useStackTraceHashing = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>useStackTraceHashing(<jk>true</jk>);</c>.
		 *
		 * @return This object (for method chaining).
		 */
		public Builder useStackTraceHashing() {
			this.useStackTraceHashing = true;
			return this;
		}

		/**
		 * Enables a timeout after which stack traces hashes are flushed.
		 *
		 * @param timeout
		 * 	Time in milliseconds to hash stack traces for.
		 * 	<br>Can be <jk>null</jk> (inherit from parent or default to {@link Integer#MAX_VALUE MAX_VALUE}).
		 * @return This object (for method chaining).
		 */
		public Builder stackTraceHashingTimeout(Integer timeout) {
			this.stackTraceHashingTimeout = timeout;
			return this;
		}

		/**
		 * The default logging level.
		 *
		 * <p>
		 * This defines the logging level for messages if they're not already defined on the matched rule.
		 *
		 * @param value
		 * 	The value for this property.
		 * 	<br>Can be <jk>null</jk> (inherit from parent or default to {@link Level#INFO INFO}).
		 * @return This object (for method chaining).
		 */
		public Builder level(Level value) {
			this.level = value;
			return this;
		}

		/**
		 * Applies the properties in the specified object map to this builder.
		 *
		 * @param m The map containing properties to apply.
		 * @return This object (for method chaining).
		 */
		public Builder apply(ObjectMap m) {
			for (String key : m.keySet()) {
				if ("useStackTraceHashing".equals(key))
					useStackTraceHashing(m.getBoolean("useStackTraceHashing"));
				else if ("stackTraceHashingTimeout".equals(key))
					stackTraceHashingTimeout(m.getInt("stackTraceHashingTimeout"));
				else if ("disabled".equals(key))
					disabled(m.get("disabled", Enablement.class));
				else if ("rules".equals(key))
					rules(m.get("rules", RestCallLoggerRule[].class));
				else if ("level".equals(key))
					level(m.get("level", Level.class));
			}
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

		int status = res.getStatus();
		Throwable e = (Throwable)req.getAttribute("Exception");
		boolean debug = isDebug(req);

		for (RestCallLoggerRule r : rules) {
			if (r.matches(status, debug, e)) {
				Enablement disabled = r.getDisabled();
				if (disabled == null)
					disabled = this.disabled;
				if (disabled == TRUE)
					return null;
				if (isNoTraceAttr(req))
					return null;
				if (disabled == FALSE)
					return r;
				if (isNoTraceHeader(req))
					return null;
				return r;
			}
		}

		return null;
	}

	private boolean isDebug(HttpServletRequest req) {
		Boolean b = boolAttr(req, "Debug");
		return (b != null && b == true);
	}

	private boolean isNoTraceAttr(HttpServletRequest req) {
		Boolean b = boolAttr(req, "NoTrace");
		return (b != null && b == true);
	}

	private boolean isNoTraceHeader(HttpServletRequest req) {
		return "true".equalsIgnoreCase(req.getHeader("X-NoTrace"));
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
	public boolean isUseStackTraceHashing() {
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

	/**
	 * Returns the rules defined in this config.
	 *
	 * @return Thew rules defined in this config.
	 */
	public List<RestCallLoggerRule> getRules() {
		return Collections.unmodifiableList(Arrays.asList(rules));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	private Boolean boolAttr(HttpServletRequest req, String name) {
		Object o = req.getAttribute(name);
		if (o == null || ! (o instanceof Boolean))
			return null;
		return (Boolean)o;
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	public ObjectMap toMap() {
		return new DefaultFilteringObjectMap()
			.append("useStackTraceHashing", useStackTraceHashing)
			.append("disabled", disabled == FALSE ? null : disabled)
			.append("stackTraceHashingTimeout", stackTraceHashingTimeout == Integer.MAX_VALUE ? null : stackTraceHashingTimeout)
			.append("level", level == Level.INFO ? null : level)
			.append("rules", rules.length == 0 ? null : rules)
		;
	}
}
