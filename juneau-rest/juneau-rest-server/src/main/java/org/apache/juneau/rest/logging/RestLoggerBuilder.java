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
package org.apache.juneau.rest.logging;

import static org.apache.juneau.Enablement.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.utils.*;

/**
 * Builder class for {@link RestLogger} objects.
 */
public class RestLoggerBuilder {

	Logger logger;
	StackTraceStore stackTraceDatabase;
	List<RestLoggerRule> normalRules = AList.of(), debugRules = AList.of();
	Enablement enabled;
	Predicate<HttpServletRequest> enabledTest;
	RestLoggingDetail requestDetail, responseDetail;
	Level level;

	/**
	 * Create a new {@link RestLogger} using this builder.
	 *
	 * @return A new {@link RestLogger}
	 */
	public RestLogger build() {
		return new RestLogger(this);
	}

	/**
	 * Create a new subclass of {@link RestLogger} using this builder.
	 *
	 * <p>
	 * The subclass must have a public constructor that optionally takes in a {@link RestLoggerBuilder} (or subclass) object.
	 *
	 * @param c The subclass to instantiate.
	 * @param <T> The subclass to instantiate.
	 * @return A new subclass of {@link RestLogger}
	 * @throws ExecutableException If constructor invocation threw an exception.
	 */
	public <T extends RestLogger> T build(Class<T> c) throws ExecutableException {
		return ClassInfo
			.of(c)
			.getOptionalPublicConstructorFuzzy(this)
			.orElseThrow(()->new ExecutableException(c.getName() + "(RestCallLoggerBuilder)"))
			.invoke(this);
	}

	/**
	 * Specifies the logger to use for logging the request.
	 *
	 * <p>
	 * If not specified, the logger name is determined in the following order:
	 * <ol>
	 * 	<li><js>{@link RestLogger#SP_logger "juneau.restLogger.logger"} system property.
	 * 	<li><js>{@link RestLogger#SP_logger "JUNEAU_RESTLOGGER_LOGGER"} environment variable.
	 * 	<li><js>"global"</js>.
	 * </ol>
	 *
	 * <p>
	 * The {@link RestLogger#getLogger()} method can also be overridden to provide different logic.
	 *
	 * @param value
	 * 	The logger to use for logging the request.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder logger(Logger value) {
		this.logger = value;
		return this;
	}

	/**
	 * Specifies the logger to use for logging the request.
	 *
	 * <p>
	 * Shortcut for calling <c>logger(Logger.<jsm>getLogger</jsm>(value))</c>.
	 *
	 * <p>
	 * If not specified, the logger name is determined in the following order:
	 * <ol>
	 * 	<li><js>{@link RestLogger#SP_logger "juneau.restLogger.logger"} system property.
	 * 	<li><js>{@link RestLogger#SP_logger "JUNEAU_RESTLOGGER_LOGGER"} environment variable.
	 * 	<li><js>"global"</js>.
	 * </ol>
	 *
	 * <p>
	 * The {@link RestLogger#getLogger()} method can also be overridden to provide different logic.
	 *
	 * @param value
	 * 	The logger to use for logging the request.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder logger(String value) {
		this.logger = value == null ? null :Logger.getLogger(value);
		return this;
	}

	/**
	 * Specifies the stack trace store to use for getting stack trace information (hash IDs and occurrence counts).
	 *
	 * @param value
	 * 	The stack trace store.
	 * 	<br>If <jk>null</jk>, stack trace information will not be logged.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder stackTraceStore(StackTraceStore value) {
		this.stackTraceDatabase = value;
		return this;
	}

	/**
	 * Specifies the default logging enablement setting.
	 *
	 * <p>
	 * This specifies the default logging enablement value if not set on the first matched rule or if no rules match.
	 *
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link Enablement#ALWAYS ALWAYS} (default) - Logging is enabled.
	 * 	<li>{@link Enablement#NEVER NEVER} - Logging is disabled.
	 * 	<li>{@link Enablement#CONDITIONAL CONDITIONALLY} - Logging is enabled if it passes the {@link #enabledTest(Predicate)} test.
	 * </ul>
	 *
	 * <p>
	 * If not specified, the setting is determined via the following:
	 * <ul>
	 * 	<li><js>{@link RestLogger#SP_enabled "juneau.restLogger.enabled"} system property.
	 * 	<li><js>{@link RestLogger#SP_enabled "JUNEAU_RESTLOGGER_ENABLED"} environment variable.
	 * 	<li><js>"ALWAYS"</js>.
	 * </ul>
	 *
	 * <p>
	 * @param value
	 * 	The default enablement flag value.  Can be <jk>null</jk> to use the default.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder enabled(Enablement value) {
		this.enabled = value;
		return this;
	}

	/**
	 * Specifies the default logging enablement test predicate.
	 *
	 * <p>
	 * This specifies the default logging enablement test if not set on the first matched rule or if no rules match.
	 *
	 * <p>
	 * This setting has no effect if the enablement setting is not {@link Enablement#CONDITIONAL CONDITIONALLY}.
	 *
	 * <p>
	 * The default if not specified is <c><jv>x</jv> -> <jk>false</jk></c> (never log).
	 *
	 * @param value
	 * 	The default enablement flag value.  Can be <jk>null</jk> to use the default.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder enabledTest(Predicate<HttpServletRequest> value) {
		this.enabledTest = value;
		return this;
	}

	/**
	 * Shortcut for calling <c>enabled(<jsf>NEVER</jsf>)</c>.
	 *
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder disabled() {
		return this.enabled(NEVER);
	}

	/**
	 * The default level of detail to log on a request.
	 *
	 * <p>
	 * This specifies the default level of request detail if not set on the first matched rule or if no rules match.
	 *
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER HEADER} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY ENTITY} - Log the status line and headers and body if available.
	 * </ul>
	 *
	 * <p>
	 * If not specified, the setting is determined via the following:
	 * <ul>
	 * 	<li><js>{@link RestLogger#SP_requestDetail "juneau.restLogger.requestDetail"} system property.
	 * 	<li><js>{@link RestLogger#SP_requestDetail "JUNEAU_RESTLOGGER_requestDetail"} environment variable.
	 * 	<li><js>"STATUS_LINE"</js>.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property, or <jk>null</jk> to use the default.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder requestDetail(RestLoggingDetail value) {
		this.requestDetail = value;
		return this;
	}

	/**
	 * The default level of detail to log on a response.
	 *
	 * <p>
	 * This specifies the default level of response detail if not set on the first matched rule or if no rules match.
	 *
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER HEADER} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY ENTITY} - Log the status line and headers and body if available.
	 * </ul>
	 *
	 * <p>
	 * If not specified, the setting is determined via the following:
	 * <ul>
	 * 	<li><js>{@link RestLogger#SP_responseDetail "juneau.restLogger.responseDetail"} system property.
	 * 	<li><js>{@link RestLogger#SP_responseDetail "JUNEAU_RESTLOGGER_responseDetail"} environment variable.
	 * 	<li><js>"STATUS_LINE"</js>.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property, or <jk>null</jk> to use the default.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder responseDetail(RestLoggingDetail value) {
		this.responseDetail = value;
		return this;
	}

	/**
	 * The default logging level to use for logging the request/response.
	 *
	 * <p>
	 * This specifies the default logging level if not set on the first matched rule or if no rules match.
	 *
	 * <p>
	 * If not specified, the setting is determined via the following:
	 * <ul>
	 * 	<li><js>{@link RestLogger#SP_level "juneau.restLogger.level"} system property.
	 * 	<li><js>{@link RestLogger#SP_level "JUNEAU_RESTLOGGER_level"} environment variable.
	 * 	<li><js>"OFF"</js>.
	 * </ul>
	 *
	 * @param value
	 * 	The new value for this property, or <jk>null</jk> to use the default value.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder level(Level value) {
		this.level = value;
		return this;
	}

	/**
	 * Adds logging rules to use when debug mode is not enabled.
	 *
	 * <p>
	 * Logging rules are matched in the order they are added.  The first to match wins.
	 *
	 * @param rules The logging rules to add to the list of rules.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder normalRules(RestLoggerRule...rules) {
		for (RestLoggerRule rule : rules)
			this.normalRules.add(rule);
		return this;
	}

	/**
	 * Adds logging rules to use when debug mode is enabled.
	 *
	 * <p>
	 * Logging rules are matched in the order they are added.  The first to match wins.
	 *
	 * @param rules The logging rules to add to the list of rules.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder debugRules(RestLoggerRule...rules) {
		for (RestLoggerRule rule : rules)
			this.debugRules.add(rule);
		return this;
	}

	/**
	 * Shortcut for adding the same rules as normal and debug rules.
	 *
	 * <p>
	 * Logging rules are matched in the order they are added.  The first to match wins.
	 *
	 * @param rules The logging rules to add to the list of rules.
	 * @return This object (for method chaining).
	 */
	public RestLoggerBuilder rules(RestLoggerRule...rules) {
		return normalRules(rules).debugRules(rules);
	}
}
