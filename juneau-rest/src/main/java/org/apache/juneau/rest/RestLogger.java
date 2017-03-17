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

import static javax.servlet.http.HttpServletResponse.*;

import java.text.*;
import java.util.logging.*;

import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Logging utility class.
 * <p>
 * Subclasses can override these methods to tailor logging of HTTP requests.
 * Subclasses MUST implement a no-arg public constructor.
 * <p>
 * RestLoggers are associated with servlets/resources in one of the following ways:
 * <ul>
 * 	<li>The {@link RestResource#logger @RestResource.logger()} annotation.
 * 	<li>The {@link RestConfig#setLogger(Class)}/{@link RestConfig#setLogger(RestLogger)} methods.
 * </ul>
 */
public abstract class RestLogger {

	/**
	 * Returns the Java logger used for logging.
	 * <p>
	 * Subclasses can provide their own logger.
	 * The default implementation returns the logger created using <code>Logger.getLogger(getClass())</code>.
	 *
	 * @return The logger used for logging.
	 */
	protected abstract Logger getLogger();

	/**
	 * Log a message to the logger.
	 * <p>
	 * Subclasses can override this method if they wish to log messages using a library other than
	 * 	Java Logging (e.g. Apache Commons Logging).
	 *
	 * @param level The log level.
	 * @param cause The cause.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected abstract void log(Level level, Throwable cause, String msg, Object...args);

	/**
	 * Log a message.
	 * <p>
	 * Equivalent to calling <code>log(level, <jk>null</jk>, msg, args);</code>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void log(Level level, String msg, Object...args) {
		log(level, null, msg, args);
	}

	/**
	 * Same as {@link #log(Level, String, Object...)} excepts runs the arguments through {@link JsonSerializer#DEFAULT_LAX_READABLE}.
	 * <p>
	 * Serialization of arguments do not occur if message is not logged, so it's safe to use this method from within debug log statements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	logObjects(<jsf>DEBUG</jsf>, <js>"Pojo contents:\n{0}"</js>, myPojo);
	 * </p>
	 *
	 * @param level The log level.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void logObjects(Level level, String msg, Object...args) {
		for (int i = 0; i < args.length; i++)
			args[i] = JsonSerializer.DEFAULT_LAX_READABLE.toStringObject(args[i]);
		log(level, null, msg, args);
	}

	/**
	 * Callback method for logging errors during HTTP requests.
	 * <p>
	 * Typically, subclasses will override this method and log errors themselves.
	 * <p>
	 * The default implementation simply logs errors to the <code>RestServlet</code> logger.
	 * <p>
	 * Here's a typical implementation showing how stack trace hashing can be used to reduce log file sizes...
	 * </p>
	 * <p class='bcode'>
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
	protected void onError(HttpServletRequest req, HttpServletResponse res, RestException e) {
		if (shouldLog(req, res, e)) {
			String qs = req.getQueryString();
			String msg = "HTTP " + req.getMethod() + " " + e.getStatus() + " " + req.getRequestURI() + (qs == null ? "" : "?" + qs);
			int c = e.getOccurrence();
			if (shouldLogStackTrace(req, res, e)) {
				msg = '[' + Integer.toHexString(e.hashCode()) + '.' + e.getStatus() + '.' + c + "] " + msg;
				log(Level.WARNING, e, msg);
			} else {
				msg = '[' + Integer.toHexString(e.hashCode()) + '.' + e.getStatus() + '.' + c + "] " + msg + ", " + e.getLocalizedMessage();
				log(Level.WARNING, msg);
			}
		}
	}

	/**
	 * Returns <jk>true</jk> if the specified exception should be logged.
	 * <p>
	 * Subclasses can override this method to provide their own logic for determining when exceptions are logged.
	 * <p>
	 * The default implementation will return <jk>false</jk> if <js>"noTrace=true"</js> is passed in the query string.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param e The exception.
	 * @return <jk>true</jk> if exception should be logged.
	 */
	protected boolean shouldLog(HttpServletRequest req, HttpServletResponse res, RestException e) {
		String q = req.getQueryString();
		return (q == null ? true : q.indexOf("noTrace=true") == -1);
	}

	/**
	 * Returns <jk>true</jk> if a stack trace should be logged for this exception.
	 * <p>
	 * Subclasses can override this method to provide their own logic for determining when stack traces are logged.
	 * <p>
	 * The default implementation will only log a stack trace if {@link RestException#getOccurrence()} returns <code>1</code>
	 * 	and the exception is not one of the following:
	 * </p>
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
		if (e.getOccurrence() == 1) {
			switch (e.getStatus()) {
				case SC_UNAUTHORIZED:
				case SC_FORBIDDEN:
				case SC_NOT_FOUND:  return false;
				default:            return true;
			}
		}
		return false;
	}

	/**
	 * NO-OP logger.
	 * <p>
	 * Disables all logging.
	 *
	 * @author James Bognar (james.bognar@salesforce.com)
	 */
	public static class NoOp extends RestLogger {

		@Override /* RestLogger */
		protected Logger getLogger() {
			return null;
		}

		@Override /* RestLogger */
		protected void log(Level level, Throwable cause, String msg, Object...args) {}
	}

	/**
	 * Default logger.
	 * <p>
	 * Logs all messages to the logger returned by <code>Logger.<jsm>getLogger</jsm>(getClass().getName())</code>
	 */
	public static class Normal extends RestLogger {

		private final JuneauLogger logger = JuneauLogger.getLogger(getClass());

		@Override /* RestLogger */
		protected Logger getLogger() {
			return logger;
		}

		@Override /* RestLogger */
		protected void log(Level level, Throwable cause, String msg, Object...args) {
			if (args.length > 0)
				msg = MessageFormat.format(msg, args);
			getLogger().log(level, msg, cause);
		}
	}
}
