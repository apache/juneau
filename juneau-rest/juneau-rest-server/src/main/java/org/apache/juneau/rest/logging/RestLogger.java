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

import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.logging.RestLoggingDetail.*;
import static org.apache.juneau.Enablement.*;
import static org.apache.juneau.SystemProperties.*;
import static java.util.logging.Level.*;

import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.utils.*;

/**
 * Interface class used for logging HTTP requests to the log file.
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
 * <p class='bcode w800'>
 * 		RestLogger
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
 * 		;
 * </p>
 *
 * <p>
 * The {@link RestLoggerBuilder#build(Class)} method has been provided for easy extension of this class.
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
 * 	<li class='jf'>{@link RestContext#REST_callLogger}
 * 	<li class='jf'>{@link RestContext#REST_callLoggerDefault}
 * 	<li class='jf'>{@link RestContext#REST_debug}
 * 	<li class='jf'>{@link RestContext#REST_debugOn}
 * 	<li class='ja'>{@link Rest#debug}
 * 	<li class='ja'>{@link RestOp#debug}
 * 	<li class='link'>{@doc RestLoggingAndDebugging}
 * </ul>
 */
public class RestLogger {

	/** Represents no logger */
	public static final class Null extends RestLogger {
		Null(RestLoggerBuilder builder) {
			super(create());
		}
	}

	private static final RestLoggerRule DEFAULT_RULE = RestLoggerRule.create().build();

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
	 * 	<li>{@link Enablement#CONDITIONAL "CONDITIONALLY"} - Logging is enabled if it passes the {@link RestLoggerBuilder#enabledTest(Predicate)} test.
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
	public static RestLoggerBuilder create() {
		return new RestLoggerBuilder();
	}

	/**
	 * Creates a new rule builder.
	 *
	 * @return A new rule builder.
	 */
	public static RestLoggerRuleBuilder createRule() {
		return new RestLoggerRuleBuilder();
	}

	private final Logger logger;
	private final StackTraceStore stackTraceStore;
	private final RestLoggerRule[] normalRules, debugRules;
	private final Enablement enabled;
	private final Predicate<HttpServletRequest> enabledTest;
	private final Level level;
	private final RestLoggingDetail requestDetail, responseDetail;

	/**
	 * Constructor.
	 *
	 * @param builder The builder object.
	 */
	public RestLogger(RestLoggerBuilder builder) {
		this.logger = firstNonNull(builder.logger, Logger.getLogger(getProperty(String.class, SP_logger, "global")));
		this.stackTraceStore = builder.stackTraceDatabase;
		this.normalRules = builder.normalRules.toArray(new RestLoggerRule[builder.normalRules.size()]);
		this.debugRules = builder.debugRules.toArray(new RestLoggerRule[builder.debugRules.size()]);
		this.enabled = firstNonNull(builder.enabled, getProperty(Enablement.class, SP_enabled, ALWAYS));
		this.enabledTest = firstNonNull(builder.enabledTest, x -> false);
		this.requestDetail = firstNonNull(builder.requestDetail, getProperty(RestLoggingDetail.class, SP_requestDetail, STATUS_LINE));
		this.responseDetail = firstNonNull(builder.responseDetail, getProperty(RestLoggingDetail.class, SP_responseDetail, STATUS_LINE));
		this.level = firstNonNull(builder.level, getProperty(Level.class, SP_level, OFF));
	}

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	public void log(HttpServletRequest req, HttpServletResponse res) {

		RestLoggerRule rule = getRule(req, res);

		if (! isEnabled(rule, req))
			return;

		Level level = firstNonNull(rule.getLevel(), this.level);

		if (level == Level.OFF)
			return;

		Throwable e = castOrNull(req.getAttribute("Exception"), Throwable.class);
		Long execTime = castOrNull(req.getAttribute("ExecTime"), Long.class);

		RestLoggingDetail reqd = firstNonNull(rule.getRequestDetail(), requestDetail);
		RestLoggingDetail resd = firstNonNull(rule.getResponseDetail(), responseDetail);

		String method = req.getMethod();
		int status = res.getStatus();
		String uri = req.getRequestURI();
		byte[] reqBody = getRequestBody(req);
		byte[] resBody = getResponseBody(req, res);

		StringBuilder sb = new StringBuilder();

		if (reqd != STATUS_LINE || resd != STATUS_LINE)
			sb.append("\n=== HTTP Call (incoming) ======================================================\n");

		StackTraceInfo sti = getStackTraceInfo(e);

		sb.append('[').append(status);

		if (sti != null) {
			int count = sti.getCount();
			sb.append(',').append(sti.getHash()).append('.').append(count);
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

			if (reqBody != null && reqd.isOneOf(HEADER ,ENTITY))
				sb.append("\n\tRequest length: ").append(reqBody.length).append(" bytes");

			if (resd.isOneOf(HEADER, ENTITY))
				sb.append("\n\tResponse code: ").append(status);

			if (resBody != null && resd.isOneOf(HEADER, ENTITY))
				sb.append("\n\tResponse length: ").append(resBody.length).append(" bytes");

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

			if (reqBody != null && reqBody.length > 0 && reqd == ENTITY) {
				try {
					sb.append("\n---Request Body UTF-8---");
					sb.append("\n").append(new String(reqBody, IOUtils.UTF8));
					sb.append("\n---Request Body Hex---");
					sb.append("\n").append(toSpacedHex(reqBody));
				} catch (Exception e1) {
					sb.append("\n").append(e1.getLocalizedMessage());
				}
			}

			if (resBody != null && resBody.length > 0 && resd == ENTITY) {
				try {
					sb.append("\n---Response Body UTF-8---");
					sb.append("\n").append(new String(resBody, IOUtils.UTF8));
					sb.append("\n---Response Body Hex---");
					sb.append("\n").append(toSpacedHex(resBody));
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
	protected RestLoggerRule getRule(HttpServletRequest req, HttpServletResponse res) {
		for (RestLoggerRule r : isDebug(req) ? debugRules : normalRules)
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
	 * @see RestContext#REST_debug
	 * @see RestContext#REST_debugOn
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
	protected boolean isEnabled(RestLoggerRule rule, HttpServletRequest req) {
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

	private byte[] getRequestBody(HttpServletRequest req) {
		if (req instanceof RestRequest)
			req = ((RestRequest)req).getInner();
		if (req instanceof CachingHttpServletRequest)
			return ((CachingHttpServletRequest)req).getBody();
		return castOrNull(req.getAttribute("RequestBody"), byte[].class);
	}

	private byte[] getResponseBody(HttpServletRequest req, HttpServletResponse res) {
		if (res instanceof RestResponse)
			res = ((RestResponse)res).getInner();
		if (res instanceof CachingHttpServletResponse)
			return ((CachingHttpServletResponse)res).getBody();
		return castOrNull(req.getAttribute("ResponseBody"), byte[].class);
	}

	private StackTraceInfo getStackTraceInfo(Throwable e) {
		if (e == null || stackTraceStore == null)
			return null;
		stackTraceStore.add(e);
		return stackTraceStore.getStackTraceInfo(e);
	}

	/**
	 * Returns the properties defined on this bean context as a simple map for debugging purposes.
	 *
	 * @return A new map containing the properties defined on this context.
	 */
	public OMap toMap() {
		return OMap.create()
			.a("logger", logger)
			.a("stackTraceStore", stackTraceStore)
			.a("enabled", enabled)
			.a("level", level)
			.a("requestDetail", requestDetail)
			.a("responseDetail", responseDetail)
			.a("normalRules", normalRules.length == 0 ? null : normalRules)
			.a("debugRules", debugRules.length == 0 ? null : debugRules)
		;
	}

	@Override /* Object */
	public String toString() {
		return SimpleJsonSerializer.DEFAULT_READABLE.toString(toMap());
	}
}
