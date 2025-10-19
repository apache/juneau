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
import static org.apache.juneau.common.utils.Utils.*;

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
		msg = String.valueOf(msg);
		if (args.length == 0)
			return msg;
		StringBuilder sb = new StringBuilder();
		int start = 0;
		for (Object arg : args) {
			int bi = msg.indexOf("{}", start);
			if (bi < 0) {
				sb.append(msg.substring(start)).append(" ").append(arg);
				start = msg.length();
			} else {
				sb.append(msg.substring(start, bi)).append(String.valueOf(arg));
				start = bi + 2;
			}
		}
		sb.append(msg.substring(start));
		return sb.toString();
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
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(Marker marker, String format, Object...arguments) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(String msg) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
	}

	@Override
	public void error(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(Marker marker, String format, Object...arguments) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(String msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(String format, Object arg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(String format, Object...arguments) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void error(String msg, Throwable t) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
	}

	@Override
	public void info(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void info(Marker marker, String format, Object...arguments) {
		// TODO Auto-generated method stub
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void info(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void info(String msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void info(String msg, Object...args) {
		if (isLoggable(INFO))
			log(INFO, format(msg, args), null);
	}

	@Override
	public void info(String format, Object arg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
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
	public boolean isDebugEnabled(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(Marker marker, String format, Object...argArray) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(String msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(String format, Object arg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(String format, Object...arguments) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void trace(String msg, Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void warn(Marker marker, String msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(Marker marker, String format, Object arg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(Marker marker, String format, Object...arguments) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(String msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(String msg, Object...args) {
		if (isLoggable(WARNING))
			log(WARNING, format(msg, args), null);
	}

	@Override
	public void warn(String format, Object arg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		// TODO Auto-generated method stub

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
		LogRecord r = new LogRecord(level, msg);
		if (thrown != null)
			r.setThrown(thrown);
		r.setLoggerName(logger.getName());
		if (SHOW_SOURCE) {
			StackTraceElement[] stack = new Throwable().getStackTrace();
			for (StackTraceElement e : stack) {
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