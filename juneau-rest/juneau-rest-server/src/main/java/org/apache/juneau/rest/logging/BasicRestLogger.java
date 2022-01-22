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

import static org.apache.juneau.collections.OMap.*;
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.logging.RestLoggingDetail.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.stats.*;
import org.apache.juneau.rest.util.*;

/**
 * Basic implementation of a {@link RestLogger} for logging HTTP requests.
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
 * 		RestLogger <jv>logger</jv> = RestLogger
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
 * <ul class='seealso'>
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#callLogger()}
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#debugEnablement()}
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestContext.Builder#debugOn(String)}
 * 	<li class='ja'>{@link Rest#debug}
 * 	<li class='ja'>{@link RestOp#debug}
 * 	<li class='link'>{@doc jrs.LoggingAndDebugging}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class BasicRestLogger implements RestLogger {

	private static final RestLoggerRule DEFAULT_RULE = RestLoggerRule.create(BeanStore.INSTANCE).build();

	private final Logger logger;
	private final ThrownStore thrownStore;
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
	public BasicRestLogger(RestLogger.Builder builder) {
		this.logger = builder.logger;
		this.thrownStore = builder.thrownStore;
		this.normalRules = builder.normalRules.toArray(new RestLoggerRule[builder.normalRules.size()]);
		this.debugRules = builder.debugRules.toArray(new RestLoggerRule[builder.debugRules.size()]);
		this.enabled = builder.enabled;
		this.enabledTest = builder.enabledTest;
		this.requestDetail = builder.requestDetail;
		this.responseDetail = builder.responseDetail;
		this.level = builder.level;
	}

	/**
	 * Called at the end of a servlet request to log the request.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 */
	@Override /* RestLogger */
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
	 * @see org.apache.juneau.rest.RestContext.Builder#debugEnablement()
	 * @see org.apache.juneau.rest.RestContext.Builder#debugOn(String)
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
		if (req instanceof CachingHttpServletRequest)
			return ((CachingHttpServletRequest)req).getBody();
		return castOrNull(req.getAttribute("RequestBody"), byte[].class);
	}

	private byte[] getResponseBody(HttpServletRequest req, HttpServletResponse res) {
		if (res instanceof CachingHttpServletResponse)
			return ((CachingHttpServletResponse)res).getBody();
		return castOrNull(req.getAttribute("ResponseBody"), byte[].class);
	}

	private ThrownStats getThrownStats(Throwable e) {
		if (e == null || thrownStore == null)
			return null;
		return thrownStore.getStats(e).orElse(null);
	}

	@Override /* Object */
	public String toString() {
		return filteredMap()
			.a("logger", logger)
			.a("thrownStore", thrownStore)
			.a("enabled", enabled)
			.a("level", level)
			.a("requestDetail", requestDetail)
			.a("responseDetail", responseDetail)
			.a("normalRules", normalRules.length == 0 ? null : normalRules)
			.a("debugRules", debugRules.length == 0 ? null : debugRules)
			.asReadableString();
	}
}
