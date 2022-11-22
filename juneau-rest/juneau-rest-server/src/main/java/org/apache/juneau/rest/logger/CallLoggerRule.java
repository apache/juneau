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

import static org.apache.juneau.collections.JsonMap.*;

import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * Represents a logging rule used by {@link CallLogger}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.LoggingAndDebugging">Logging / Debugging</a>
 * </ul>
 */
public class CallLoggerRule {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
	public static class Builder extends BeanBuilder<CallLoggerRule> {

		Predicate<Integer> statusFilter;
		Predicate<HttpServletRequest> requestFilter;
		Predicate<HttpServletResponse> responseFilter;
		Predicate<Throwable> exceptionFilter;
		Enablement enabled;
		Predicate<HttpServletRequest> enabledTest;
		Level level;
		CallLoggingDetail requestDetail, responseDetail;
		boolean logStackTrace;

		/**
		 * Constructor.
		 *
		 * @param beanStore The bean store to use for creating beans.
		 */
		protected Builder(BeanStore beanStore) {
			super(CallLoggerRule.class, beanStore);
		}

		@Override /* BeanBuilder */
		protected CallLoggerRule buildDefault() {
			return new CallLoggerRule(this);
		}

		//-------------------------------------------------------------------------------------------------------------
		// Properties
		//-------------------------------------------------------------------------------------------------------------

		/**
		 * Apply a status-based predicate check for this rule to match against.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule that only matches if the status code is greater than or equal to 500.</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.statusFilter(<jv>x</jv> -&gt; <jv>x</jv> &gt;= 500)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The predicate check, or <jk>null</jk> to not use any filtering based on status code.
		 * @return This object.
		 */
		public Builder statusFilter(Predicate<Integer> value) {
			this.statusFilter = value;
			return this;
		}

		/**
		 * Apply a throwable-based predicate check for this rule to match against.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule that only matches if a NotFound exception was thrown.</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.exceptionFilter(<jv>x</jv> -&gt; <jv>x</jv> <jk>instanceof</jk> NotFound)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * This check is only performed if an actual throwable was thrown.  Therefore it's not necessary to perform a
		 * null check in the predicate.
		 *
		 * @param value
		 * 	The predicate check, or <jk>null</jk> to not use any filtering based on exceptions.
		 * @return This object.
		 */
		public Builder exceptionFilter(Predicate<Throwable> value) {
			this.exceptionFilter = value;
			return this;
		}

		/**
		 * Apply a request-based predicate check for this rule to match against.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule that only matches if the servlet path contains "foobar".</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.requestFilter(<jv>x</jv> -&gt; <jv>x</jv>.getServletPath().contains(<js>"foobar"</js>))
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The predicate check, or <jk>null</jk> to not use any filtering based on the request.
		 * @return This object.
		 */
		public Builder requestFilter(Predicate<HttpServletRequest> value) {
			this.requestFilter = value;
			return this;
		}

		/**
		 * Apply a response-based predicate check for this rule to match against.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule that only matches if the servlet path contains "foobar".</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.responseFilter(<jv>x</jv> -&gt; <jv>x</jv>.getStatus() &gt;= 500)
		 * 		.build();
		 * </p>
		 *
		 * <p>
		 * Note that the {@link #statusFilter(Predicate)} and {@link #exceptionFilter(Predicate)} methods are simply
		 * convenience response filters.
		 *
		 * @param value
		 * 	The predicate check, or <jk>null</jk> to not use any filtering based on the response.
		 * @return This object.
		 */
		public Builder responseFilter(Predicate<HttpServletResponse> value) {
			this.responseFilter = value;
			return this;
		}

		/**
		 * Specifies whether logging is enabled when using this rule.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule where logging is only enabled if the query string contains "foobar".</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.enabled(<jsf>CONDITIONALLY</jsf>)
		 * 		.enabledPredicate(<jv>x</jv> -&gt; <jv>x</jv>.getQueryString().contains(<js>"foobar"</js>))
		 * 		.build();
		 * </p>
		 *
		 * <ul class='values'>
		 * 	<li>{@link Enablement#ALWAYS ALWAYS} - Logging is enabled.
		 * 	<li>{@link Enablement#NEVER NEVER} - Logging is disabled.
		 * 	<li>{@link Enablement#CONDITIONAL CONDITIONALLY} - Logging is enabled if it passes the {@link #enabledPredicate(Predicate)} test.
		 * </ul>
		 *
		 * @param value
		 * 	The enablement flag value, or <jk>null</jk> to inherit from the call logger whose default value is {@link Enablement#ALWAYS ALWAYS}
		 * 	unless overridden via a <js>"juneau.restCallLogger.enabled"</js> system property or <js>"JUNEAU_RESTCALLLOGGER_ENABLED"</js> environment variable.
		 * @return This object.
		 */
		public Builder enabled(Enablement value) {
			this.enabled = value;
			return this;
		}

		/**
		 * Specifies the predicate test to use when the enabled setting is set to {@link Enablement#CONDITIONAL CONDITIONALLY}.
		 *
		 * <p>
		 * This setting has no effect if the enablement value is not {@link Enablement#CONDITIONAL CONDITIONALLY}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a logger rule where logging is only enabled if the query string contains "foobar".</jc>
		 * 	RestLogger
		 * 		.<jsm>createRule</jsm>()
		 * 		.enabled(<jsf>CONDITIONALLY</jsf>)
		 * 		.enabledPredicate(<jv>x</jv> -&gt; <jv>x</jv>.getQueryString().contains(<js>"foobar"</js>))
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The enablement predicate test, or <jk>null</jk> to inherit from the call logger whose default value is <c><jv>x</jv> -&gt; <jk>false</jk></c>.
		 * @return This object.
		 */
		public Builder enabledPredicate(Predicate<HttpServletRequest> value) {
			this.enabledTest = value;
			return this;
		}

		/**
		 * Shortcut for calling <c>enabled(<jsf>NEVER</jsf>)</c>.
		 *
		 * @return This object.
		 */
		public Builder disabled() {
			return this.enabled(Enablement.NEVER);
		}

		/**
		 * The level of detail to log on a request.
		 *
		 * <ul class='values'>
		 * 	<li>{@link CallLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
		 * 	<li>{@link CallLoggingDetail#HEADER HEADER} - Log the status line and headers.
		 * 	<li>{@link CallLoggingDetail#ENTITY ENTITY} - Log the status line and headers and content if available.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to inherit from the call logger.
		 * @return This object.
		 */
		public Builder requestDetail(CallLoggingDetail value) {
			this.requestDetail = value;
			return this;
		}

		/**
		 * The level of detail to log on a response.
		 *
		 * <ul class='values'>
		 * 	<li>{@link CallLoggingDetail#STATUS_LINE STATUS_LINE} - Log only the status line.
		 * 	<li>{@link CallLoggingDetail#HEADER HEADER} - Log the status line and headers.
		 * 	<li>{@link CallLoggingDetail#ENTITY ENTITY} - Log the status line and headers and content if available.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to inherit from the call logger.
		 * @return This object.
		 */
		public Builder responseDetail(CallLoggingDetail value) {
			this.responseDetail = value;
			return this;
		}

		/**
		 * The logging level to use for logging the request/response.
		 *
		 * <p>
		 * The default value is {@link Level#INFO}.
		 *
		 * @param value
		 * 	The new value for this property, or <jk>null</jk> to inherit from the call logger.
		 * @return This object.
		 */
		public Builder level(Level value) {
			this.level = value;
			return this;
		}

		/**
		 * Log a stack trace as part of the log entry.
		 *
		 * <p>
		 * The default value is <jk>false</jk>.
		 *
		 * @return This object.
		 */
		public Builder logStackTrace() {
			this.logStackTrace = true;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder impl(Object value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.BeanBuilder */
		public Builder type(Class<?> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Predicate<Integer> statusFilter;
	private final Predicate<HttpServletRequest> requestFilter;
	private final Predicate<HttpServletResponse> responseFilter;
	private final Predicate<Throwable> exceptionFilter;
	private final Level level;
	private final Enablement enabled;
	private final Predicate<HttpServletRequest> enabledTest;
	private final CallLoggingDetail requestDetail, responseDetail;
	private final boolean logStackTrace;

	/**
	 * Constructor.
	 *
	 * @param b Builder
	 */
	CallLoggerRule(Builder b) {
		this.statusFilter = b.statusFilter;
		this.exceptionFilter = b.exceptionFilter;
		this.requestFilter = b.requestFilter;
		this.responseFilter = b.responseFilter;
		this.level = b.level;
		this.enabled = b.enabled;
		this.enabledTest = b.enabledTest;
		this.requestDetail = b.requestDetail;
		this.responseDetail = b.responseDetail;
		this.logStackTrace = b.logStackTrace;
	}

	/**
	 * Returns <jk>true</jk> if this rule matches the specified parameters.
	 *
	 * @param req The HTTP request being logged.  Never <jk>null</jk>.
	 * @param res The HTTP response being logged.  Never <jk>null</jk>.
	 * @return <jk>true</jk> if this rule matches the specified parameters.
	 */
	public boolean matches(HttpServletRequest req, HttpServletResponse res) {

		if ((requestFilter != null && ! requestFilter.test(req)) || (responseFilter != null && ! responseFilter.test(res)))
			return false;

		if (statusFilter != null && ! statusFilter.test(res.getStatus()))
			return false;

		Throwable e = (Throwable)req.getAttribute("Exception");
		if (e != null && exceptionFilter != null && ! exceptionFilter.test(e))
			return false;

		return true;
	}

	/**
	 * Returns the detail level for HTTP requests.
	 *
	 * @return the detail level for HTTP requests, or <jk>null</jk> if it's not set.
	 */
	public CallLoggingDetail getRequestDetail() {
		return requestDetail;
	}

	/**
	 * Returns the detail level for HTTP responses.
	 *
	 * @return the detail level for HTTP responses, or <jk>null</jk> if it's not set.
	 */
	public CallLoggingDetail getResponseDetail() {
		return responseDetail;
	}

	/**
	 * Returns the log level on this rule.
	 *
	 * @return The log level on this rule, or <jk>null</jk> if it's not set.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Returns the enablement flag value on this rule.
	 *
	 * @return The enablement flag value on this rule, or <jk>null</jk> if it's not set.
	 */
	public Enablement getEnabled() {
		return enabled;
	}

	/**
	 * Returns the enablement predicate test on this rule.
	 *
	 * @return The enablement predicate test on this rule, or <jk>null</jk> if it's not set.
	 */
	public Predicate<HttpServletRequest> getEnabledTest() {
		return enabledTest;
	}

	/**
	 * Returns <jk>true</jk> if a stack trace should be logged.
	 *
	 * @return <jk>true</jk> if a stack trace should be logged.
	 */
	public boolean isLogStackTrace() {
		return logStackTrace;
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.append("codeFilter", statusFilter)
			.append("exceptionFilter", exceptionFilter)
			.append("requestFilter", requestFilter)
			.append("responseFilter", responseFilter)
			.append("level", level)
			.append("requestDetail", requestDetail)
			.append("responseDetail", responseDetail)
			.append("enabled", enabled)
			.append("enabledTest", enabledTest)
			.asReadableString();
	}
}
