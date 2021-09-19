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
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.rest.HttpRuntimeException.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.mstat.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Interface class used for logging HTTP requests.
 *
 * <p>
 * The {@link Builder#type(Class)} method has been provided for easy extension of this class.
 *
 * <p>
 * The following default implementations are also provided:
 * <ul class='javatree'>
 * 	<li class='jc'>{@link BasicRestLogger} - The default logger typically used.
 * 	<li class='jc'>{@link BasicDisabledRestLogger} - A no-op logger if you want to turn off logging entirely.
 * 	<li class='jc'>{@link BasicTestRestLogger} - A logger useful for testcases.
 * 	<li class='jc'>{@link BasicTestCaptureRestLogger} - Useful for capturing log messages for testing logging itself.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link RestContextBuilder#callLogger()}
 * 	<li class='jm'>{@link RestContextBuilder#debugEnablement()}
 * 	<li class='jm'>{@link RestContextBuilder#debugOn(String)}
 * 	<li class='ja'>{@link Rest#debug}
 * 	<li class='ja'>{@link RestOp#debug}
 * 	<li class='link'>{@doc RestLoggingAndDebugging}
 * </ul>
 */
public interface RestLogger {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Represents no logger */
	public abstract class Null implements RestLogger {}

	/**
	 * System property name for the default logger name to use for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LOGGER</c> environment variable.
	 * <p>
	 * If not specified, the default is <js>"global"</js>.
	 */
	public static final String SP_logger = "juneau.restLogger.logger";

	/**
	 * System property name for the default enablement setting for {@link RestLogger} objects.
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
	 * System property name for the default request detail setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_REQUESTDETAIL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and body if available.
	 * </ul>
	 */
	public static final String SP_requestDetail = "juneau.restLogger.requestDetail";

	/**
	 * System property name for the default response detail setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_RESPONSEDETAIL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
	 * 	<li>{@link RestLoggingDetail#STATUS_LINE "STATUS_LINE"} (default) - Log only the status line.
	 * 	<li>{@link RestLoggingDetail#HEADER "HEADER"} - Log the status line and headers.
	 * 	<li>{@link RestLoggingDetail#ENTITY "ENTITY"} - Log the status line and headers and body if available.
	 * </ul>
	 */
	public static final String SP_responseDetail = "juneau.restLogger.responseDetail";

	/**
	 * System property name for the logging level setting for {@link RestLogger} objects.
	 * <p>
	 * Can also use a <c>JUNEAU_RESTLOGGER_LEVEL</c> environment variable.
	 * <p>
	 * The possible values are:
	 * <ul>
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
	 * Creates a new builder for this object.
	 *
	 * @return A new builder for this object.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder {

		Logger logger;
		ThrownStore thrownStore;
		List<RestLoggerRule> normalRules = AList.create(), debugRules = AList.create();
		Enablement enabled;
		Predicate<HttpServletRequest> enabledTest;
		RestLoggingDetail requestDetail, responseDetail;
		Level level;
		BeanStore beanStore;
		Class<? extends RestLogger> type;
		RestLogger impl;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Copy constuctor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			logger = copyFrom.logger;
			thrownStore = copyFrom.thrownStore;
			normalRules = AList.<RestLoggerRule>create().append(copyFrom.normalRules);
			debugRules = AList.<RestLoggerRule>create().append(copyFrom.debugRules);
			enabled = copyFrom.enabled;
			enabledTest = copyFrom.enabledTest;
			requestDetail = copyFrom.requestDetail;
			responseDetail = copyFrom.responseDetail;
			level = copyFrom.level;
			beanStore = copyFrom.beanStore;
			type = copyFrom.type;
			impl = copyFrom.impl;
		}

		/**
		 * Creates a new {@link RestLogger} object from this builder.
		 *
		 * <p>
		 * Instantiates an instance of the {@link #type(Class) implementation class} or
		 * else {@link BasicRestLogger} if implementation class was not specified.
		 *
		 * @return A new {@link RestLogger} object.
		 */
		public RestLogger build() {
			try {
				if (impl != null)
					return impl;
				return BeanCreator.create(RestLogger.class).type(isConcrete(type) ? type : BasicRestLogger.class).store(beanStore).builder(this).findSingleton().run();
			} catch (Exception e) {
				throw toHttpException(e, InternalServerError.class);
			}
		}

		/**
		 * Specifies the bean store to use for instantiating the {@link RestLogger} object.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder beanStore(BeanStore value) {
			beanStore = value;
			return this;
		}

		/**
		 * Specifies a subclass of {@link RestLogger} to create when the {@link #build()} method is called.
		 *
		 * @param value The new value for this setting.
		 * @return  This object.
		 */
		public Builder type(Class<? extends RestLogger> value) {
			type = value;
			return this;
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
		 * The {@link BasicRestLogger#getLogger()} method can also be overridden to provide different logic.
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
		 * 	<li><js>{@link RestLogger#SP_logger "juneau.restLogger.logger"} system property.
		 * 	<li><js>{@link RestLogger#SP_logger "JUNEAU_RESTLOGGER_LOGGER"} environment variable.
		 * 	<li><js>"global"</js>.
		 * </ol>
		 *
		 * <p>
		 * The {@link BasicRestLogger#getLogger()} method can also be overridden to provide different logic.
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
		 * The default if not specified is <c><jv>x</jv> -> <jk>false</jk></c> (never log).
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
		 * @return This object.
		 */
		public Builder requestDetail(RestLoggingDetail value) {
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
		 * @return This object.
		 */
		public Builder responseDetail(RestLoggingDetail value) {
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
		 * 	<li><js>{@link RestLogger#SP_level "juneau.restLogger.level"} system property.
		 * 	<li><js>{@link RestLogger#SP_level "JUNEAU_RESTLOGGER_level"} environment variable.
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
		public Builder normalRules(RestLoggerRule...values) {
			for (RestLoggerRule rule : values)
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
		public Builder debugRules(RestLoggerRule...values) {
			for (RestLoggerRule rule : values)
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
		public Builder rules(RestLoggerRule...values) {
			return normalRules(values).debugRules(values);
		}

		/**
		 * Specifies an already-instantiated bean for the {@link #build()} method to return.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder impl(RestLogger value) {
			impl = value;
			return this;
		}

		/**
		 * Creates a copy of this builder.
		 *
		 * @return A copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	public void log(HttpServletRequest req, HttpServletResponse res);
}
