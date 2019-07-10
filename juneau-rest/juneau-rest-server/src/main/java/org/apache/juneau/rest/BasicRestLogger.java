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

import java.text.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;

/**
 * Logging utility class.
 *
 * <p>
 * Subclasses can override these methods to tailor logging of HTTP requests.
 * <br>Subclasses MUST implement a no-arg public constructor.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'>{@doc juneau-rest-server.LoggingAndErrorHandling}
 * </ul>
 *
 * @deprecated Use {@link BasicRestCallLogger}
 */
@Deprecated
public class BasicRestLogger implements RestLogger {

	private final JuneauLogger logger;
	private final RestContext context;


	/**
	 * Constructor.
	 *
	 * @param context The context of the resource object.
	 */
	public BasicRestLogger(RestContext context) {
		this.context = context;
		this.logger = JuneauLogger.getLogger(getLoggerName());
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
		return context == null ? getClass().getName() : context.getResource().getClass().getName();
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

	@Override /* RestLogger */
	public void setLevel(Level level) {
		getLogger().setLevel(level);
	}

	/**
	 * Log a message to the logger.
	 *
	 * <p>
	 * Subclasses can override this method if they wish to log messages using a library other than Java Logging
	 * (e.g. Apache Commons Logging).
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	@Override /* RestLogger */
	public void log(Level level, Throwable cause, String msg, Object...args) {
		msg = format(msg, args);
		getLogger().log(level, msg, cause);
	}

	/**
	 * Log a message.
	 *
	 * <p>
	 * Equivalent to calling <code>log(level, <jk>null</jk>, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	@Override /* RestLogger */
	public void log(Level level, String msg, Object...args) {
		log(level, null, msg, args);
	}

	/**
	 * Same as {@link #log(Level, String, Object...)} excepts runs the arguments through {@link JsonSerializer#DEFAULT_READABLE}.
	 *
	 * <p>
	 * Serialization of arguments do not occur if message is not logged, so it's safe to use this method from within
	 * debug log statements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	logObjects(<jsf>DEBUG</jsf>, <js>"Pojo contents:\n{0}"</js>, myPojo);
	 * </p>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	@Override /* RestLogger */
	public void logObjects(Level level, String msg, Object...args) {
		for (int i = 0; i < args.length; i++)
			args[i] = SimpleJsonSerializer.DEFAULT_READABLE.toStringObject(args[i]);
		log(level, null, msg, args);
	}

	/**
	 * Callback method for logging errors during HTTP requests.
	 *
	 * <p>
	 * Typically, subclasses will override this method and log errors themselves.
	 *
	 * <p>
	 * The default implementation simply logs errors to the <c>RestServlet</c> logger.
	 *
	 * <p>
	 * Here's a typical implementation showing how stack trace hashing can be used to reduce log file sizes...
	 * <p class='bcode w800'>
	 * 	<jk>protected void</jk> onError(HttpServletRequest req, HttpServletResponse res, RestException e, <jk>boolean</jk> noTrace) {
	 * 		String qs = req.getQueryString();
	 * 		String msg = <js>"HTTP "</js> + req.getMethod() + <js>" "</js> + e.getStatus() + <js>" "</js> + req.getRequestURI() + (qs == <jk>null</jk> ? <js>""</js> : <js>"?"</js> + qs);
	 * 		<jk>int</jk> c = e.getOccurrence();
	 *
	 * 		<jc>// REST_useStackTraceHashes is disabled, so we have to log the exception every time.</jc>
	 * 		<jk>if</jk> (c == 0)
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%s] %s"</js>, e.getStatus(), msg), e);
	 *
	 * 		<jc>// This is the first time we've countered this error, so log a stack trace
	 * 		// unless ?noTrace was passed in as a URL parameter.</jc>
	 * 		<jk>else if</jk> (c == 1 &amp;&amp; ! noTrace)
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%h.%s.%s] %s"</js>, e.hashCode(), e.getStatus(), c, msg), e);
	 *
	 * 		<jc>// This error occurred before.
	 * 		// Only log the message, not the stack trace.</jc>
	 * 		<jk>else</jk>
	 * 			myLogger.log(Level.<jsf>WARNING</jsf>, <jsm>format</jsm>(<js>"[%h.%s.%s] %s, %s"</js>, e.hashCode(), e.getStatus(), c, msg, e.getLocalizedMessage()));
	 * 	}
	 * </p>
	 *
	 * @param req The servlet request object.
	 * @param res The servlet response object.
	 * @param e Exception indicating what error occurred.
	 */
	@Override /* RestLogger */
	public void onError(HttpServletRequest req, HttpServletResponse res, RestException e) {
		// No-op
	}

	/**
	 * Returns <jk>true</jk> if the specified exception should be logged.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own logic for determining when exceptions are logged.
	 *
	 * <p>
	 * The default implementation will return <jk>false</jk> if <js>"noTrace=true"</js> is passed in the query string
	 * or <c>No-Trace: true</c> is specified in the header.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param e The exception.
	 * @return <jk>true</jk> if exception should be logged.
	 */
	protected boolean shouldLog(HttpServletRequest req, HttpServletResponse res, RestException e) {
		return false;
	}

	/**
	 * Returns <jk>true</jk> if a stack trace should be logged for this exception.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own logic for determining when stack traces are logged.
	 *
	 * <p>
	 * The default implementation will only log a stack trace if {@link RestException#getOccurrence()} returns
	 * <c>1</c> and the exception is not one of the following:
	 * <ul>
	 * 	<li>{@link HttpServletResponse#SC_UNAUTHORIZED}
	 * 	<li>{@link HttpServletResponse#SC_FORBIDDEN}
	 * 	<li>{@link HttpServletResponse#SC_NOT_FOUND}
	 * </ul>
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param e The exception.
	 * @return <jk>true</jk> if stack trace should be logged.
	 */
	protected boolean shouldLogStackTrace(HttpServletRequest req, HttpServletResponse res, RestException e) {
		return false;
	}
}
