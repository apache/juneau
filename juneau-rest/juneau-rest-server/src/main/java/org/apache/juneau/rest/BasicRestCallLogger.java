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
import static org.apache.juneau.internal.ObjectUtils.*;
import static org.apache.juneau.rest.RestCallLoggingDetail.*;

import java.util.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.utils.*;

/**
 * Default implementation of the {@link RestCallLogger} interface.
 *
 * <p>
 * Subclasses can override these methods to tailor logging of HTTP requests.
 * <br>Subclasses MUST implement a no-arg public constructor or constructor that takes in a {@link RestContext} arg.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndDebugging}
 * </ul>
 */
public class BasicRestCallLogger implements RestCallLogger {

	private final String loggerName;
	private final Logger logger;
	private final RestContext context;
	private final StackTraceDatabase stackTraceDb;

	/**
	 * Constructor.
	 *
	 * @param context The context of the resource object.
	 */
	public BasicRestCallLogger(RestContext context) {
		this.context = context;
		this.loggerName = context == null ? getClass().getName() : context.getResource().getClass().getName();
		this.logger = Logger.getLogger(getLoggerName());
		this.stackTraceDb = context == null ? null : context.getStackTraceDb();
	}

	/**
	 * Constructor.
	 *
	 * @param logger The logger to use for logging.
	 * @param stackTraceDb The stack trace database for maintaining stack traces.
	 */
	protected BasicRestCallLogger(Logger logger, StackTraceDatabase stackTraceDb) {
		this.context = null;
		this.loggerName = getClass().getName();
		this.logger = logger;
		this.stackTraceDb = stackTraceDb;
	}

	/**
	 * Returns the logger name.
	 *
	 * <p>
	 * By default returns the class name of the servlet class passed in to the context.
	 *
	 * <p>
	 * Subclasses can override this to provide their own customized logger names.
	 *
	 * @return The logger name.
	 */
	protected String getLoggerName() {
		return loggerName;
	}

	/**
	 * Returns the Java logger used for logging.
	 *
	 * <p>
	 * Subclasses can provide their own logger.
	 * The default implementation returns the logger created using <c>Logger.getLogger(getClass())</c>.
	 *
	 * @return The logger used for logging.
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Clears out the stack trace database.
	 *
	 * @return This object (for method chaining).
	 */
	public BasicRestCallLogger resetStackTraces() {
		if (stackTraceDb != null)
			stackTraceDb.reset();
		return this;
	}

	@Override /* RestCallLogger */
	public void log(RestCallLoggerConfig config, HttpServletRequest req, HttpServletResponse res) {

		if (config.isDisabled(req))
			return;

		RestCallLoggerRule rule = config.getRule(req, res);
		if (rule == null)
			return;

		Level level = rule.getLevel();
		if (level == null)
			level = config.getLevel();

		if (level == Level.OFF)
			return;

		Throwable e = castOrNull(req.getAttribute("Exception"), Throwable.class);
		Long execTime = castOrNull(req.getAttribute("ExecTime"), Long.class);

		RestCallLoggingDetail reqd = rule.getReqDetail(), resd = rule.getResDetail();

		String method = req.getMethod();
		int status = res.getStatus();
		String uri = req.getRequestURI();
		byte[] reqBody = getRequestBody(req);
		byte[] resBody = getResponseBody(req, res);

		StringBuilder sb = new StringBuilder();

		if (reqd != SHORT || resd != SHORT)
			sb.append("\n=== HTTP Call (incoming) ======================================================\n");

		StackTraceInfo sti = getStackTraceInfo(config, e);

		sb.append('[').append(status);

		if (sti != null) {
			int count = sti.getCount();
			sb.append(',').append(sti.getHash()).append('.').append(count);
			if (count > 1)
				e = null;
		}

		sb.append("] ");

		sb.append("HTTP ").append(method).append(' ').append(uri);

		if (reqd != SHORT || resd != SHORT) {

			if (reqd.isOneOf(MEDIUM, LONG)) {
				String qs = req.getQueryString();
				if (qs != null)
					sb.append('?').append(qs);
			}

			if (reqBody != null && reqd.isOneOf(MEDIUM ,LONG))
				sb.append("\n\tRequest length: ").append(reqBody.length).append(" bytes");

			if (resd.isOneOf(MEDIUM, LONG))
				sb.append("\n\tResponse code: ").append(status);

			if (resBody != null && resd.isOneOf(MEDIUM, LONG))
				sb.append("\n\tResponse length: ").append(resBody.length).append(" bytes");

			if (execTime != null && resd.isOneOf(MEDIUM, LONG))
				sb.append("\n\tExec time: ").append(execTime).append("ms");

			if (reqd.isOneOf(MEDIUM, LONG)) {
				Enumeration<String> hh = req.getHeaderNames();
				if (hh.hasMoreElements()) {
					sb.append("\n---Request Headers---");
					while (hh.hasMoreElements()) {
						String h = hh.nextElement();
						sb.append("\n\t").append(h).append(": ").append(req.getHeader(h));
					}
				}
			}

			if (context != null && reqd.isOneOf(MEDIUM, LONG)) {
				Map<String,Object> hh = context.getReqHeaders();
				if (! hh.isEmpty()) {
					sb.append("\n---Default Servlet Headers---");
					for (Map.Entry<String,Object> h : hh.entrySet()) {
						sb.append("\n\t").append(h.getKey()).append(": ").append(h.getValue());
					}
				}
			}

			if (resd.isOneOf(MEDIUM, LONG)) {
				Collection<String> hh = res.getHeaderNames();
				if (hh.size() > 0) {
					sb.append("\n---Response Headers---");
					for (String h : hh) {
						sb.append("\n\t").append(h).append(": ").append(res.getHeader(h));
					}
				}
			}

			if (reqBody != null && reqBody.length > 0 && reqd == LONG) {
				try {
					sb.append("\n---Request Body UTF-8---");
					sb.append("\n").append(new String(reqBody, IOUtils.UTF8));
					sb.append("\n---Request Body Hex---");
					sb.append("\n").append(toSpacedHex(reqBody));
				} catch (Exception e1) {
					sb.append("\n").append(e1.getLocalizedMessage());
				}
			}

			if (resBody != null && resBody.length > 0 && resd == LONG) {
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

		log(level, sb.toString(), e);
	}

	/**
	 * Logs the specified message to the logger.
	 *
	 * <p>
	 * Subclasses can override this method to capture messages being sent to the logger.
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

	private StackTraceInfo getStackTraceInfo(RestCallLoggerConfig config, Throwable e) {
		if (e == null || stackTraceDb == null || ! config.isUseStackTraceHashing())
			return null;
		stackTraceDb.add(e);
		return stackTraceDb.getStackTraceInfo(e);
	}
}
