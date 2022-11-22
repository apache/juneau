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
package org.apache.juneau.rest.logger;

import static java.util.logging.Level.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.SystemEnv.*;
import static org.apache.juneau.rest.logger.CallLoggingDetail.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.util.*;

/**
 * Basic implementation of a {@link CallLogger} for logging HTTP requests.
 *
 * <p>
 * Provides the following capabilities:
 * <ul>
 * 	<li>Allows incoming HTTP requests to be logged at various {@link Enablement detail levels}.
 * 	<li>Allows rules to be defined to handle request logging differently depending on the resulting status code.
 * 	<li>Allows use of stack trace hashing to eliminate duplication of stack traces in log files.
 * 	<li>Allows customization of handling of where requests are logged to.
 * 	<li>Allows configuration via system properties or environment variables.
 * </ul>
 *
 * <p>
 * The following is an example of a logger that logs errors only when debugging is not enabled, and everything when
 * logging is enabled.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 		CallLogger <jv>logger</jv> = CallLogger
 * 			.<jsm>create</jsm>()
 * 			.logger(<js>"MyLogger"</js>)  <jc>// Use MyLogger Java logger.</jc>
 * 			.normalRules(  <jc>// Rules when debugging is not enabled.</jc>
 * 				<jsm>createRule</jsm>()  <jc>// Log 500+ errors with status-line and header information.</jc>
 * 					.statusFilter(x -&gt; x &gt;= 500)
 * 					.level(<jsf>SEVERE</jsf>)
 * 					.requestDetail(<jsf>HEADER</jsf>)
 * 					.responseDetail<jsf>(HEADER</jsf>)
 * 					.build(),
 * 				<jsm>createRule</jsm>()  <jc>// Log 400-500 errors with just status-line information.</jc>
 * 					.statusFilter(x -&gt; x &gt;= 400)
 * 					.level(<jsf>WARNING</jsf>)
 * 					.requestDetail(<jsf>STATUS_LINE</jsf>)
 * 					.responseDetail(<jsf>STATUS_LINE</jsf>)
 * 					.build()
 * 			)
 * 			.debugRules(  <jc>// Rules when debugging is enabled.</jc>
 * 				<jsm>createRule</jsm>()  <jc>// Log everything with full details.</jc>
 * 					.level(<jsf>SEVERE</jsf>)
 * 					.requestDetail(<jsf>ENTITY</jsf>)
 * 					.responseDetail(<jsf>ENTITY</jsf>)
 * 					.build()
 * 			)
 * 			.build()
 * 		;
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#callLogger()}
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#debugEnablement()}
 * 	<li class='ja'>{@link Rest#debug}
 * 	<li class='ja'>{@link RestOp#debug}
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class CallLogger {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final CallLoggerRule DEFAULT_RULE = CallLoggerRule.create(BeanStore.INSTANCE).build();

	/** Represents no logger */
	public abstract class Void extends CallLogger {
		Void(BeanStore beanStore) {
			super(beanStore);
		}
	}

	/**
	 * System property name for the default logger name to use for {@link CallLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LOGGER</c> environment variable.
	 * <p>
	 * If not specified, the default is <js>"global"</js>.
	 */
	public static final String SP_logger = "juneau.restLogger.logger";

	/**
	 * System property name for the default enablement setting for {@link CallLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_ENABLED</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link Enablement#ALWAYS "ALWAYS"} (default) - Logging is enabled.
	 * 	<li>{@link Enablement#NEVER "NEVER"} - Logging is disabled.
	 * 	<li>{@link Enablement#CONDITIONAL "CONDITIONALLY"} - Logging is enabled if it passes the {@link Builder#enabledTest(Predicate)} test.
	 * </ul>
	 */
	public static final String SP_enabled = "juneau.restLogger.enabled";

	/**
	 * System property name for the default request detail setting for {@link CallLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_REQUESTDETAIL</c> environment variable.
	 *
	 * <ul class='values'>
	 * 	<li>{@link CallLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link CallLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link CallLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and content if available.
	 * </ul>
	 */
	public static final String SP_requestDetail = "juneau.restLogger.requestDetail";

	/**
	 * System property name for the default response detail setting for {@link CallLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_RESPONSEDETAIL</c> environment variable.
	 *
	 * <ul class='values'>
	 * 	<li>{@link CallLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link CallLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link CallLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and content if available.
	 * </ul>
	 */
	public static final String SP_responseDetail = "juneau.restLogger.responseDetail";

	/**
	 * System property name for the logging level setting for {@link CallLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LEVEL</c> environment variable.
	 *
	 * <ul class='values'>
	 * 	<li>{@link Level#OFF "OFF"} (default)
	 * 	<li>{@link Level#SEVERE "SEVERE"}
	 * 	<li>{@link Level#WARNING "WARNING"}
	 * 	<li>{@link Level#INFO "INFO"}
	 * 	<li>{@link Level#CONFIG "CONFIG"}
	 * 	<li>{@link Level#FINE "FINE"}
	 * 	<li>{@link Level#FINER "FINER"}
	 * 	<li>{@link Level#FINEST "FINEST"}
	 * </ul>
	 */
	public static final String SP_level = "juneau.restLogger.level";

	/**
	 * Static creator.
	 *
	 * @param beanStore The bean store to use for creating beans.
	 * @return A new builder for this object.
	 */
	public static Builder create(BeanStore beanStore) {
		return new Builder(beanStore);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder {

		Logger logger;
		ThrownStore thrownStore;
		List<CallLoggerRule> normalRules = list(), debugRules = list();
		Enablement enabled;
		Predicate<HttpServletRequest> enabledTest;
		CallLoggingDetail requestDetail, responseDetail;
		Level level;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			logger = Logger.getLogger(env(SP_logger, "global"));
			enabled = env(SP_enabled, ALWAYS);
			enabledTest = x -> false;
			requestDetail = env(SP_requestDetail, STATUS_LINE);
			responseDetail = env(SP_responseDetail, STATUS_LINE);
			level = env(SP_level).map(Level::parse).orElse(OFF);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Specifies the logger to use for logging the request.
		 *
		 * <p>
		 * If not specified, the logger name is determined in the following order:
		 * <ol>
		 * 	<li><js>{@link CallLogger#SP_logger "juneau.restLogger.logger"} system property.
		 * 	<li><js>{@link CallLogger#SP_logger "JUNEAU_RESTLOGGER_LOGGER"} environment variable.
		 * 	<li><js>"global"</js>.
		 * </ol>
		 *
		 * <p>
		 * The {@link CallLogger#getLogger()} method can also be overridden to provide different logic.
		 *
		 * @param value
		 * 	The logger to use for logging the request.
		 * @return This object.
		 */
		public Builder logger(Logger value) {
			logger = value;
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
		 * 	<li><js>{@link CallLogger#SP_logger "juneau.restLogger.logger"} system property.
		 * 	<li><js>{@link CallLogger#SP_logger "JUNEAU_RESTLOGGER_LOGGER"} environment variable.
		 * 	<li><js>"global"</js>.
		 * </ol>
		 *
		 * <p>
		 * The {@link CallLogger#getLogger()} method can also be overridden to provide different logic.
		 *
		 * @param value
		 * 	The logger to use for logging the request.
		 * @return This object.
		 */
		public Builder logger(String value) {
			logger = value == null ? null :Logger.getLogger(value);
			return this;
		}

		/**
		 * Same as {@link #logger(Logger)} but only sets the value if it's currently <jk>null</jk>.
		 *
		 * @param value The logger to use for logging the request.
		 * @return This object.
		 */
		public Builder loggerOnce(Logger value) {
			if (logger == null)
				logger = value;
			return this;
		}

		/**
		 * Specifies the thrown exception store to use for getting stack trace information (hash IDs and occurrence counts).
		 *
		 * @param value
		 * 	The stack trace store.
		 * 	<br>If <jk>null</jk>, stack trace information will not be logged.
		 * @return This object.
		 */
		public Builder thrownStore(ThrownStore value) {
			thrownStore = value;
			return this;
		}

		/**
		 * Same as {@link #thrownStore(ThrownStore)} but only sets the value if it's currently <jk>null</jk>.
		 *
		 * @param value
		 * 	The stack trace store.
		 * 	<br>If <jk>null</jk>, stack trace information will not be logged.
		 * @return This object.
		 */
		public Builder thrownStoreOnce(ThrownStore value) {
			if (thrownStore == null)
				thrownStore = value;
			return this;
		}
		/**
		 * Specifies the default logging enablement setting.
		 *
		 * <p>
		 * This specifies the default logging enablement value if not set on the first matched rule or if no rules match.
		 *
		 * <p>
		 * If not specified, the setting is determined via the following:
		 * <ul>
		 * 	<li><js>{@link CallLogger#SP_enabled "juneau.restLogger.enabled"} system property.
		 * 	<li><js>{@link CallLogger#SP_enabled "JUNEAU_RESTLOGGER_ENABLED"} environment variable.
		 * 	<li><js>"ALWAYS"</js>.
		 * </ul>
		 *
		 * <ul class='values'>
		 * 	<li>{@link Enablement#ALWAYS ALWAYS} (default) - Logging is enabled.
		 * 	<li>{@link Enablement#NEVER NEVER} - Logging is disabled.
		 * 	<li>{@link Enablement#CONDITIONAL CONDITIONALLY} - Logging is enabled if it passes the {@link #enabledTest(Predicate)} test.
		 * </ul>
		 *
		 * <p>
		 * @param value
		 * 	The default enablement flag value.  Can be <jk>null</jk> to use the default.
		 * @return This object.
		 */
		public Builder enabled(Enablement value) {
			enabled = value;
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
		 * The default if not specified is <c><jv>x</jv> -&gt; <jk>false</jk></c> (never log).
		 *
		 * @param value
		 * 	The default enablement flag value.  Can be <jk>null</jk> to use the default.
		 * @return This object.
		 */
		public Builder enabledTest(Predicate<HttpServletRequest> value) {
			enabledTest = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>enabled(<jsf>NEVER</jsf>)</c>.
		 *
		 * @return This object.
		 */
		public Builder disabled() {
			return enabled(NEVER);
		}

		/**
		 * The default level of detail to log on a request.
		 *
		 * <p>
		 * This specifies the default level of request detail if not set on the first matched rule or if no rules match.
		 *
		 * <p>
		 * If not specified, the setting is determined via the following:
		 * <ul>
		 * 	<li><js>{@link CallLogger#SP_requestDetail "juneau.restLogger.requestDetail"} system property.
		 * 	<li><js>{@link CallLogger#SP_requestDetail "JUNEAU_RESTLOGGER_requestDetail"} environment variable.
		 * 	<li><js>"STATUS_LINE"</js>.
		 * </ul>
		 *
		 * <ul class='values'>
		 * 	<li>{@link CallLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
		 * 	<li>{@link CallLoggingDetail#HEADER HEADER} - Log the status line and headers.
		 * 	<li>{@link CallLoggingDetail#ENTITY ENTITY} - Log the status line and headers and content if available.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to use the default.
		 * @return This object.
		 */
		public Builder requestDetail(CallLoggingDetail value) {
			requestDetail = value;
			return this;
		}

		/**
		 * The default level of detail to log on a response.
		 *
		 * <p>
		 * This specifies the default level of response detail if not set on the first matched rule or if no rules match.
		 *
		 * <p>
		 * If not specified, the setting is determined via the following:
		 * <ul>
		 * 	<li><js>{@link CallLogger#SP_responseDetail "juneau.restLogger.responseDetail"} system property.
		 * 	<li><js>{@link CallLogger#SP_responseDetail "JUNEAU_RESTLOGGER_responseDetail"} environment variable.
		 * 	<li><js>"STATUS_LINE"</js>.
		 * </ul>
		 *
		 * <ul class='values'>
		 * 	<li>{@link CallLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
		 * 	<li>{@link CallLoggingDetail#HEADER HEADER} - Log the status line and headers.
		 * 	<li>{@link CallLoggingDetail#ENTITY ENTITY} - Log the status line and headers and content if available.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to use the default.
		 * @return This object.
		 */
		public Builder responseDetail(CallLoggingDetail value) {
			responseDetail = value;
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
		 * 	<li><js>{@link CallLogger#SP_level "juneau.restLogger.level"} system property.
		 * 	<li><js>{@link CallLogger#SP_level "JUNEAU_RESTLOGGER_level"} environment variable.
		 * 	<li><js>"OFF"</js>.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to use the default value.
		 * @return This object.
		 */
		public Builder level(Level value) {
			level = value;
			return this;
		}

		/**
		 * Adds logging rules to use when debug mode is not enabled.
		 *
		 * <p>
		 * Logging rules are matched in the order they are added.  The first to match wins.
		 *
		 * @param values The logging rules to add to the list of rules.
		 * @return This object.
		 */
		public Builder normalRules(CallLoggerRule...values) {
			for (CallLoggerRule rule : values)
				normalRules.add(rule);
			return this;
		}

		/**
		 * Adds logging rules to use when debug mode is enabled.
		 *
		 * <p>
		 * Logging rules are matched in the order they are added.  The first to match wins.
		 *
		 * @param values The logging rules to add to the list of rules.
		 * @return This object.
		 */
		public Builder debugRules(CallLoggerRule...values) {
			for (CallLoggerRule rule : values)
				debugRules.add(rule);
			return this;
		}

		/**
		 * Shortcut for adding the same rules as normal and debug rules.
		 *
		 * <p>
		 * Logging rules are matched in the order they are added.  The first to match wins.
		 *
		 * @param values The logging rules to add to the list of rules.
		 * @return This object.
		 */
		public Builder rules(CallLoggerRule...values) {
			return normalRules(values).debugRules(values);
		}

		/**
		 * Instantiates a new call logger based on the settings in this builder.
		 *
		 * @return A new call logger.
		 */
		public CallLogger build() {
			return new CallLogger(this);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Fluent setters
		//-----------------------------------------------------------------------------------------------------------------

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Logger logger;
	private final ThrownStore thrownStore;
	private final CallLoggerRule[] normalRules, debugRules;
	private final Enablement enabled;
	private final Predicate<HttpServletRequest> enabledTest;
	private final Level level;
	private final CallLoggingDetail requestDetail, responseDetail;

	/**
	 * Constructor.
	 * <p>
	 * Subclasses typically override the {@link #init(BeanStore)} method when using this constructor.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 */
	public CallLogger(BeanStore beanStore) {
		Builder builder = init(beanStore);
		this.logger = builder.logger;
		this.thrownStore = builder.thrownStore;
		this.normalRules = builder.normalRules.toArray(new CallLoggerRule[builder.normalRules.size()]);
		this.debugRules = builder.debugRules.toArray(new CallLoggerRule[builder.debugRules.size()]);
		this.enabled = builder.enabled;
		this.enabledTest = builder.enabledTest;
		this.requestDetail = builder.requestDetail;
		this.responseDetail = builder.responseDetail;
		this.level = builder.level;
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this logger.
	 */
	public CallLogger(Builder builder) {
		this.logger = builder.logger;
		this.thrownStore = builder.thrownStore;
		this.normalRules = builder.normalRules.toArray(new CallLoggerRule[builder.normalRules.size()]);
		this.debugRules = builder.debugRules.toArray(new CallLoggerRule[builder.debugRules.size()]);
		this.enabled = builder.enabled;
		this.enabledTest = builder.enabledTest;
		this.requestDetail = builder.requestDetail;
		this.responseDetail = builder.responseDetail;
		this.level = builder.level;
	}

	/**
	 * Initializer.
	 * <p>
	 * Subclasses should override this method to make modifications to the builder used to create this logger.
	 *
	 * @param beanStore The bean store containing injectable beans for this logger.
	 * @return A new builder object.
	 */
	protected Builder init(BeanStore beanStore) {
		return new Builder(beanStore)
			.logger(beanStore.getBean(Logger.class).orElse(null))
			.thrownStore(beanStore.getBean(ThrownStore.class).orElse(null));
	}

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	public void log(HttpServletRequest req, HttpServletResponse res) {

		CallLoggerRule rule = getRule(req, res);

		if (! isEnabled(rule, req))
			return;

		Level level = firstNonNull(rule.getLevel(), this.level);

		if (level == Level.OFF)
			return;

		Throwable e = castOrNull(req.getAttribute("Exception"), Throwable.class);
		Long execTime = castOrNull(req.getAttribute("ExecTime"), Long.class);

		CallLoggingDetail reqd = firstNonNull(rule.getRequestDetail(), requestDetail);
		CallLoggingDetail resd = firstNonNull(rule.getResponseDetail(), responseDetail);

		String method = req.getMethod();
		int status = res.getStatus();
		String uri = req.getRequestURI();
		byte[] reqContent = getRequestContent(req);
		byte[] resContent = getResponseContent(req, res);

		StringBuilder sb = new StringBuilder();

		if (reqd != STATUS_LINE || resd != STATUS_LINE)
			sb.append("\n=== HTTP Call (incoming) ======================================================\n");

		ThrownStats sti = getThrownStats(e);

		sb.append('[').append(status);

		if (sti != null) {
			int count = sti.getCount();
			sb.append(',').append(StringUtils.toHex8(sti.getHash())).append('.').append(count);
			if (count > 1)
				e = null;
		}

		sb.append("] ");

		sb.append("HTTP ").append(method).append(' ').append(uri);

		if (reqd != STATUS_LINE || resd != STATUS_LINE) {

			if (reqd.isOneOf(HEADER, ENTITY)) {
				String qs = req.getQueryString();
				if (qs != null)
					sb.append('?').append(qs);
			}

			if (reqContent != null && reqd.isOneOf(HEADER ,ENTITY))
				sb.append("\n\tRequest length: ").append(reqContent.length).append(" bytes");

			if (resd.isOneOf(HEADER, ENTITY))
				sb.append("\n\tResponse code: ").append(status);

			if (resContent != null && resd.isOneOf(HEADER, ENTITY))
				sb.append("\n\tResponse length: ").append(resContent.length).append(" bytes");

			if (execTime != null && resd.isOneOf(HEADER, ENTITY))
				sb.append("\n\tExec time: ").append(execTime).append("ms");

			if (reqd.isOneOf(HEADER, ENTITY)) {
				Enumeration<String> hh = req.getHeaderNames();
				if (hh.hasMoreElements()) {
					sb.append("\n---Request Headers---");
					while (hh.hasMoreElements()) {
						String h = hh.nextElement();
						sb.append("\n\t").append(h).append(": ").append(req.getHeader(h));
					}
				}
			}

			if (resd.isOneOf(HEADER, ENTITY)) {
				Collection<String> hh = res.getHeaderNames();
				if (hh.size() > 0) {
					sb.append("\n---Response Headers---");
					for (String h : hh) {
						sb.append("\n\t").append(h).append(": ").append(res.getHeader(h));
					}
				}
			}

			if (reqContent != null && reqContent.length > 0 && reqd == ENTITY) {
				try {
					sb.append("\n---Request Content UTF-8---");
					sb.append("\n").append(new String(reqContent, IOUtils.UTF8));
					sb.append("\n---Request Content Hex---");
					sb.append("\n").append(toSpacedHex(reqContent));
				} catch (Exception e1) {
					sb.append("\n").append(e1.getLocalizedMessage());
				}
			}

			if (resContent != null && resContent.length > 0 && resd == ENTITY) {
				try {
					sb.append("\n---Response Content UTF-8---");
					sb.append("\n").append(new String(resContent, IOUtils.UTF8));
					sb.append("\n---Response Content Hex---");
					sb.append("\n").append(toSpacedHex(resContent));
				} catch (Exception e1) {
					sb.append(e1.getLocalizedMessage());
				}
			}
			sb.append("\n=== END ======================================================================");
		}

		if (rule.isLogStackTrace() && e == null)
			e = new Throwable("Stacktrace");

		log(level, sb.toString(), e);

	}

	/**
	 * Given the specified servlet request/response, find the rule that applies to it.
	 *
	 * <p>
	 * This method can be overridden to provide specialized logic for finding rules.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @return The applicable logging rule, or the default rule if not found.  Never <jk>null</jk>.
	 */
	protected CallLoggerRule getRule(HttpServletRequest req, HttpServletResponse res) {
		for (CallLoggerRule r : isDebug(req) ? debugRules : normalRules)
			if (r.matches(req, res))
				return r;
		return DEFAULT_RULE;
	}

	/**
	 * Returns <jk>true</jk> if debug is enabled on this request.
	 *
	 * <p>
	 * Looks for the request attribute <js>"Debug"</js> to determine whether debug is enabled.
	 *
	 * <p>
	 * This method can be overridden to provide specialized logic for determining whether debug mode is enabled on a request.
	 *
	 * @param req The HTTP request being logged.
	 * @return <jk>true</jk> if debug is enabled on this request.
	 * @see org.apache.juneau.rest.RestContext.Builder#debugEnablement()
	 * @see Rest#debug()
	 * @see RestOp#debug()
	 */
	protected boolean isDebug(HttpServletRequest req) {
		return firstNonNull(castOrNull(req.getAttribute("Debug"), Boolean.class), false);
	}

	/**
	 * Returns <jk>true</jk> if logging is enabled for this request.
	 *
	 * <p>
	 * Uses the enabled and enabled-test settings on the matched rule and this logger to determine whether a REST
	 * call should be logged.
	 *
	 * <p>
	 * This method can be overridden to provide specialized logic for determining whether a REST call should be logged.
	 *
	 * @param rule The first matching rule.  Never <jk>null</jk>.
	 * @param req The HTTP request.
	 * @return <jk>true</jk> if logging is enabled for this request.
	 */
	protected boolean isEnabled(CallLoggerRule rule, HttpServletRequest req) {
		Enablement enabled = firstNonNull(rule.getEnabled(), this.enabled);
		Predicate<HttpServletRequest> enabledTest = firstNonNull(rule.getEnabledTest(), this.enabledTest);
		return enabled.isEnabled(enabledTest.test(req));
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the logger to use for logging REST calls.
	 *
	 * <p>
	 * Returns the logger specified in the builder, or {@link Logger#getGlobal()} if it wasn't specified.
	 *
	 * <p>
	 * This method can be overridden in subclasses to provide a different logger.
	 *
	 * @return The logger to use for logging REST calls.
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Logs the specified message to the logger.
	 *
	 * <p>
	 * Subclasses can override this method to capture messages being sent to the logger and handle it differently.
	 *
	 * @param level The log level.
	 * @param msg The log message.
	 * @param e The exception.
	 */
	protected void log(Level level, String msg, Throwable e) {
		getLogger().log(level, msg, e);
	}

	private byte[] getRequestContent(HttpServletRequest req) {
		if (req instanceof CachingHttpServletRequest)
			return ((CachingHttpServletRequest)req).getContent();
		return castOrNull(req.getAttribute("RequestContent"), byte[].class);
	}

	private byte[] getResponseContent(HttpServletRequest req, HttpServletResponse res) {
		if (res instanceof CachingHttpServletResponse)
			return ((CachingHttpServletResponse)res).getContent();
		return castOrNull(req.getAttribute("ResponseContent"), byte[].class);
	}

	private ThrownStats getThrownStats(Throwable e) {
		if (e == null || thrownStore == null)
			return null;
		return thrownStore.getStats(e).orElse(null);
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("logger", logger)
			.append("thrownStore", thrownStore)
			.append("enabled", enabled)
			.append("level", level)
			.append("requestDetail", requestDetail)
			.append("responseDetail", responseDetail)
			.append("normalRules", normalRules.length == 0 ? null : normalRules)
			.append("debugRules", debugRules.length == 0 ? null : debugRules)
			.asReadableString();
	}
}
