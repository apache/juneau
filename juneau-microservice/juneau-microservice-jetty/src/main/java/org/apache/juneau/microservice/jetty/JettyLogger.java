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
package org.apache.juneau.microservice.jetty;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static org.apache.juneau.internal.SystemEnv.env;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-jetty">juneau-microservice-jetty</a>
 * </ul>
 */
public class JettyLogger implements org.eclipse.jetty.util.log.Logger {
	private static final boolean SHOW_SOURCE = env("org.eclipse.jetty.util.log.SOURCE", env("org.eclipse.jetty.util.log.javautil.SOURCE", true));

	private Level configuredLevel;
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
    public org.eclipse.jetty.util.log.Logger getLogger(String name) {
        return new JettyLogger(name);
    }

	@Override
	public String getName() {
		return logger.getName();
	}

	@Override
	public void warn(String msg, Object... args) {
		if (isLoggable(WARNING))
			log(WARNING, format(msg, args), null);
	}

	@Override
	public void warn(Throwable thrown) {
		if (isLoggable(WARNING))
			log(WARNING, "", thrown);
	}

	@Override
	public void warn(String msg, Throwable thrown) {
		if (isLoggable(WARNING))
			log(WARNING, msg, thrown);
	}

	@Override
	public void info(String msg, Object... args) {
		if (isLoggable(INFO))
			log(INFO, format(msg, args), null);
	}

	@Override
	public void info(Throwable thrown) {
		if (isLoggable(INFO))
			log(INFO, "", thrown);
	}

	@Override
	public void info(String msg, Throwable thrown) {
		if (isLoggable(INFO))
			log(INFO, msg, thrown);
	}

	@Override
	public boolean isDebugEnabled() {
		return isLoggable(FINE);
	}

	@Override
	public void setDebugEnabled(boolean enabled) {
		if (enabled) {
			configuredLevel = logger.getLevel();
			logger.setLevel(FINE);
		} else {
			logger.setLevel(configuredLevel);
		}
	}

	@Override
	public void debug(String msg, Object... args) {
		if (isLoggable(FINE))
			log(FINE, format(msg, args), null);
	}

	@Override
	public void debug(String msg, long arg) {
		if (isLoggable(FINE))
			log(FINE, format(msg, arg), null);
	}

	@Override
	public void debug(Throwable thrown) {
		if (isLoggable(FINE))
			log(FINE, "", thrown);
	}

	@Override
	public void debug(String msg, Throwable thrown) {
		if (isLoggable(FINE))
			log(FINE, msg, thrown);
	}

	@Override
	public void ignore(Throwable ignored) {
		if (isLoggable(FINEST))
			log(FINEST, "IGNORED EXCEPTION ", ignored);
	}

	private static String format(String msg, Object... args) {
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

	private void log(Level level, String msg, Throwable thrown) {
		LogRecord r = new LogRecord(level, msg);
		if (thrown != null)
			r.setThrown(thrown);
		r.setLoggerName(logger.getName());
		if (SHOW_SOURCE) {
			StackTraceElement[] stack = new Throwable().getStackTrace();
			for (int i = 0; i < stack.length; i++) {
				StackTraceElement e = stack[i];
				if (!e.getClassName().equals(getClass().getName())) {
					r.setSourceClassName(e.getClassName());
					r.setSourceMethodName(e.getMethodName());
					break;
				}
			}
		}
		logger.log(r);
	}

	private boolean isLoggable(Level level) {
		return logger.isLoggable(level);
	}

}