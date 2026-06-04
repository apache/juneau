/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.microservice.jetty;

import static java.util.logging.Level.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.logging.*;
import java.util.logging.Logger;

import org.slf4j.*;
import org.slf4j.spi.*;

/**
 * Implementation of Jetty {@link Logger} based on {@link java.util.logging.Logger}.
 *
 * <p>
 * Allows Jetty to log to the Java Util logging framework (and thus to the main log file defined in the
 * <cc>[Logging]</cc> section).
 *
 * <p>
 * Can be used by setting the following system property in the microservice config file.
 *
 * <p class='bini'>
 * 	<cs>[SystemProperties]</cs>
 *
 * 	<cc># Configure Jetty to log using java-util logging</cc>
 * 	<ck>org.eclipse.jetty.util.log.class</ck> = org.apache.juneau.microservice.jetty.JettyLogger
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceJettyBasics">juneau-microservice-jetty Basics</a>
 * </ul>
 */
public class JettyLogger implements LocationAwareLogger {
	private static final boolean SHOW_SOURCE = env("org.eclipse.jetty.util.log.SOURCE", env("org.eclipse.jetty.util.log.javautil.SOURCE", true));

	/**
	 * Formats a log message by replacing "{}" placeholders with the provided arguments.
	 *
	 * @param msg The message template with "{}" placeholders.
	 * @param args The arguments to substitute into the message.
	 * @return The formatted message string.
	 */
	private static String format(String msg, Object...args) {
		return f(msg, args);
	}

	/**
	 * Maps an SLF4J integer log level to the equivalent {@link java.util.logging.Level}.
	 *
	 * @param level The SLF4J level constant (e.g. {@link LocationAwareLogger#DEBUG_INT}).
	 * @return The equivalent Java Util Logging level.
	 */
	private static Level toLevel(int level) {
		return switch (level) {
			case LocationAwareLogger.TRACE_INT -> FINEST;
			case LocationAwareLogger.DEBUG_INT -> FINE;
			case LocationAwareLogger.INFO_INT -> INFO;
			case LocationAwareLogger.WARN_INT -> WARNING;
			default -> SEVERE;  // ERROR_INT and any out-of-range value map to the highest severity.
		};
	}

	/**
	 * The originally configured log level, used to restore the level when debug mode is disabled.
	 */
	private Level configuredLevel;

	/**
	 * The underlying Java Util Logger instance.
	 */
	private Logger logger;

	/**
	 * Default constructor.
	 *
	 * <p>
	 * Returns the logger with name <js>"org.eclipse.jetty.util.log.javautil"</js>.
	 */
	public JettyLogger() {
		this("org.eclipse.jetty.util.log.javautil");
	}

	/**
	 * Normal constructor.
	 *
	 * @param name The logger name.
	 */
	public JettyLogger(String name) {
		logger = Logger.getLogger(name);
		configuredLevel = logger.getLevel();
	}

	@Override
	public void debug(Marker marker, String msg) {
		debug(msg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		debug(format, arg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void debug(Marker marker, String format, Object...arguments) {
		debug(format, arguments);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		debug(format, arg1, arg2);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		debug(msg, t);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void debug(String msg) {
		if (isLoggable(FINE))
			log(FINE, msg, null);
	}

	/**
	 * Logs a debug message with a long argument.
	 *
	 * @param msg The message template with "{}" placeholder.
	 * @param arg The long argument to substitute into the message.
	 */
	public void debug(String msg, long arg) {
		if (isLoggable(FINE))
			log(FINE, format(msg, arg), null);
	}

	@Override
	public void debug(String msg, Object...args) {
		if (isLoggable(FINE))
			log(FINE, format(msg, args), null);
	}

	@Override
	public void debug(String format, Object arg) {
		if (isLoggable(FINE))
			log(FINE, format(format, arg), null);
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (isLoggable(FINE))
			log(FINE, format(format, arg1, arg2), null);
	}

	@Override
	public void debug(String msg, Throwable thrown) {
		if (isLoggable(FINE))
			log(FINE, msg, thrown);
	}

	/**
	 * Logs a debug message with only a throwable.
	 *
	 * @param thrown The throwable to log.
	 */
	public void debug(Throwable thrown) {
		if (isLoggable(FINE))
			log(FINE, "", thrown);
	}

	@Override
	public void error(Marker marker, String msg) {
		error(msg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		error(format, arg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void error(Marker marker, String format, Object...arguments) {
		error(format, arguments);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		error(format, arg1, arg2);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		error(msg, t);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void error(String msg) {
		if (isLoggable(SEVERE))
			log(SEVERE, msg, null);
	}

	@Override
	public void error(String format, Object arg) {
		if (isLoggable(SEVERE))
			log(SEVERE, format(format, arg), null);
	}

	@Override
	public void error(String format, Object...arguments) {
		if (isLoggable(SEVERE))
			log(SEVERE, format(format, arguments), null);
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (isLoggable(SEVERE))
			log(SEVERE, format(format, arg1, arg2), null);
	}

	@Override
	public void error(String msg, Throwable t) {
		if (isLoggable(SEVERE))
			log(SEVERE, msg, t);
	}

	/**
	 * Creates a child logger with the specified name.
	 *
	 * @param name The logger name.
	 * @return A new JettyLogger instance for the specified name.
	 */
	public JettyLogger getLogger(String name) {
		return new JettyLogger(name);
	}

	@Override
	public String getName() { return logger.getName(); }

	/**
	 * Logs an ignored exception at the FINEST level.
	 *
	 * @param ignored The exception to log as ignored.
	 */
	public void ignore(Throwable ignored) {
		if (isLoggable(FINEST))
			log(FINEST, "IGNORED EXCEPTION ", ignored);
	}

	@Override
	public void info(Marker marker, String msg) {
		info(msg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		info(format, arg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void info(Marker marker, String format, Object...arguments) {
		info(format, arguments);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		info(format, arg1, arg2);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		info(msg, t);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void info(String msg) {
		if (isLoggable(INFO))
			log(INFO, msg, null);
	}

	@Override
	public void info(String msg, Object...args) {
		if (isLoggable(INFO))
			log(INFO, format(msg, args), null);
	}

	@Override
	public void info(String format, Object arg) {
		if (isLoggable(INFO))
			log(INFO, format(format, arg), null);
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (isLoggable(INFO))
			log(INFO, format(format, arg1, arg2), null);
	}

	@Override
	public void info(String msg, Throwable thrown) {
		if (isLoggable(INFO))
			log(INFO, msg, thrown);
	}

	/**
	 * Logs an info message with only a throwable.
	 *
	 * @param thrown The throwable to log.
	 */
	public void info(Throwable thrown) {
		if (isLoggable(INFO))
			log(INFO, "", thrown);
	}

	@Override
	public boolean isDebugEnabled() { return isLoggable(FINE); }

	@Override
	public boolean isDebugEnabled(Marker marker) { return isLoggable(FINE); }

	@Override
	public boolean isErrorEnabled() { return isLoggable(SEVERE); }

	@Override
	public boolean isErrorEnabled(Marker marker) { return isLoggable(SEVERE); }

	@Override
	public boolean isInfoEnabled() { return isLoggable(INFO); }

	@Override
	public boolean isInfoEnabled(Marker marker) { return isLoggable(INFO); }

	@Override
	public boolean isTraceEnabled() { return isLoggable(FINEST); }

	@Override
	public boolean isTraceEnabled(Marker marker) { return isLoggable(FINEST); }

	@Override
	public boolean isWarnEnabled() { return isLoggable(WARNING); }

	@Override
	public boolean isWarnEnabled(Marker marker) { return isLoggable(WARNING); }

	@Override
	public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
		var l = toLevel(level);
		if (isLoggable(l))
			log(l, argArray == null ? message : format(message, argArray), t);
	}

	/**
	 * Enables or disables debug logging.
	 *
	 * <p>
	 * When enabled, sets the log level to FINE. When disabled, restores the previously configured log level.
	 *
	 * @param enabled <jk>true</jk> to enable debug logging, <jk>false</jk> to disable.
	 */
	public void setDebugEnabled(boolean enabled) {
		if (enabled) {
			configuredLevel = logger.getLevel();
			logger.setLevel(FINE);
		} else {
			logger.setLevel(configuredLevel);
		}
	}

	@Override
	public void trace(Marker marker, String msg) {
		trace(msg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		trace(format, arg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void trace(Marker marker, String format, Object...argArray) {
		trace(format, argArray);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		trace(format, arg1, arg2);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		trace(msg, t);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void trace(String msg) {
		if (isLoggable(FINEST))
			log(FINEST, msg, null);
	}

	@Override
	public void trace(String format, Object arg) {
		if (isLoggable(FINEST))
			log(FINEST, format(format, arg), null);
	}

	@Override
	public void trace(String format, Object...arguments) {
		if (isLoggable(FINEST))
			log(FINEST, format(format, arguments), null);
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (isLoggable(FINEST))
			log(FINEST, format(format, arg1, arg2), null);
	}

	@Override
	public void trace(String msg, Throwable t) {
		if (isLoggable(FINEST))
			log(FINEST, msg, t);
	}

	@Override
	public void warn(Marker marker, String msg) {
		warn(msg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		warn(format, arg);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void warn(Marker marker, String format, Object...arguments) {
		warn(format, arguments);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		warn(format, arg1, arg2);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		warn(msg, t);  // Markers are not used by this JUL-backed implementation.
	}

	@Override
	public void warn(String msg) {
		if (isLoggable(WARNING))
			log(WARNING, msg, null);
	}

	@Override
	public void warn(String msg, Object...args) {
		if (isLoggable(WARNING))
			log(WARNING, format(msg, args), null);
	}

	@Override
	public void warn(String format, Object arg) {
		if (isLoggable(WARNING))
			log(WARNING, format(format, arg), null);
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (isLoggable(WARNING))
			log(WARNING, format(format, arg1, arg2), null);
	}

	@Override
	public void warn(String msg, Throwable thrown) {
		if (isLoggable(WARNING))
			log(WARNING, msg, thrown);
	}

	/**
	 * Logs a warning message with only a throwable.
	 *
	 * @param thrown The throwable to log.
	 */
	public void warn(Throwable thrown) {
		if (isLoggable(WARNING))
			log(WARNING, "", thrown);
	}

	/**
	 * Checks if a message at the specified level would be logged.
	 *
	 * @param level The log level to check.
	 * @return <jk>true</jk> if the level is loggable, <jk>false</jk> otherwise.
	 */
	private boolean isLoggable(Level level) {
		return logger.isLoggable(level);
	}

	/**
	 * Internal log method that creates a LogRecord and logs it.
	 *
	 * <p>
	 * Optionally includes source class and method name in the log record if SHOW_SOURCE is enabled.
	 *
	 * @param level The log level.
	 * @param msg The log message.
	 * @param thrown The throwable to log, or <jk>null</jk> if none.
	 */
	private void log(Level level, String msg, Throwable thrown) {
		var r = new LogRecord(level, msg);
		if (nn(thrown))
			r.setThrown(thrown);
		r.setLoggerName(logger.getName());
		if (SHOW_SOURCE) { // HTT - SHOW_SOURCE is a static final set at class load time from system property; false branch requires property set before JVM start
			StackTraceElement[] stack = new Throwable().getStackTrace();
			for (var e : stack) {
				if (! e.getClassName().equals(getClass().getName())) {
					r.setSourceClassName(e.getClassName());
					r.setSourceMethodName(e.getMethodName());
					break;
				}
			}
		}
		logger.log(r);
	}
}