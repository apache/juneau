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
package org.apache.juneau.microservice.tomcat;

import static java.util.logging.Level.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.util.logging.*;

import org.apache.juli.logging.*;

/**
 * Implementation of the embedded-Tomcat {@link Log} façade based on {@link java.util.logging.Logger}.
 *
 * <p>
 * Allows Tomcat to log to the Java Util logging framework (and thus to the main log file defined in the
 * <cc>[Logging]</cc> section), mirroring the role of
 * {@code org.apache.juneau.microservice.jetty.JettyLogger} in the Jetty module.
 *
 * <p>
 * Tomcat log levels map to {@link Level} values as follows: <c>trace</c>&rarr;{@link Level#FINEST FINEST},
 * <c>debug</c>&rarr;{@link Level#FINE FINE}, <c>info</c>&rarr;{@link Level#INFO INFO},
 * <c>warn</c>&rarr;{@link Level#WARNING WARNING}, and <c>error</c>/<c>fatal</c>&rarr;{@link Level#SEVERE SEVERE}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauMicroserviceTomcatBasics">juneau-microservice-tomcat Basics</a>
 * </ul>
 *
 * @since 10.0.0
 */
public class TomcatLogger implements Log {

	private final Logger logger;

	/**
	 * Default constructor.
	 *
	 * <p>
	 * Returns the logger with name <js>"org.apache.juneau.microservice.tomcat"</js>.
	 */
	public TomcatLogger() {
		this("org.apache.juneau.microservice.tomcat");
	}

	/**
	 * Normal constructor.
	 *
	 * @param name The logger name.
	 */
	public TomcatLogger(String name) {
		logger = Logger.getLogger(name);
	}

	/**
	 * Returns the name of the underlying Java Util logger.
	 *
	 * @return The logger name.
	 */
	public String getName() {
		return logger.getName();
	}

	@Override /* Overridden from Log */
	public boolean isTraceEnabled() {
		return logger.isLoggable(FINEST);
	}

	@Override /* Overridden from Log */
	public boolean isDebugEnabled() {
		return logger.isLoggable(FINE);
	}

	@Override /* Overridden from Log */
	public boolean isInfoEnabled() {
		return logger.isLoggable(INFO);
	}

	@Override /* Overridden from Log */
	public boolean isWarnEnabled() {
		return logger.isLoggable(WARNING);
	}

	@Override /* Overridden from Log */
	public boolean isErrorEnabled() {
		return logger.isLoggable(SEVERE);
	}

	@Override /* Overridden from Log */
	public boolean isFatalEnabled() {
		return logger.isLoggable(SEVERE);
	}

	@Override /* Overridden from Log */
	public void trace(Object message) {
		log(FINEST, message, null);
	}

	@Override /* Overridden from Log */
	public void trace(Object message, Throwable t) {
		log(FINEST, message, t);
	}

	@Override /* Overridden from Log */
	public void debug(Object message) {
		log(FINE, message, null);
	}

	@Override /* Overridden from Log */
	public void debug(Object message, Throwable t) {
		log(FINE, message, t);
	}

	@Override /* Overridden from Log */
	public void info(Object message) {
		log(INFO, message, null);
	}

	@Override /* Overridden from Log */
	public void info(Object message, Throwable t) {
		log(INFO, message, t);
	}

	@Override /* Overridden from Log */
	public void warn(Object message) {
		log(WARNING, message, null);
	}

	@Override /* Overridden from Log */
	public void warn(Object message, Throwable t) {
		log(WARNING, message, t);
	}

	@Override /* Overridden from Log */
	public void error(Object message) {
		log(SEVERE, message, null);
	}

	@Override /* Overridden from Log */
	public void error(Object message, Throwable t) {
		log(SEVERE, message, t);
	}

	@Override /* Overridden from Log */
	public void fatal(Object message) {
		log(SEVERE, message, null);
	}

	@Override /* Overridden from Log */
	public void fatal(Object message, Throwable t) {
		log(SEVERE, message, t);
	}

	/**
	 * Internal log method that creates a {@link LogRecord} and logs it through the underlying Java Util logger.
	 *
	 * @param level The log level.
	 * @param message The log message (its {@code toString()} is used; {@code null} is rendered as an empty string).
	 * @param thrown The throwable to log, or <jk>null</jk> if none.
	 */
	private void log(Level level, Object message, Throwable thrown) {
		if (! logger.isLoggable(level))
			return;
		var r = new LogRecord(level, message == null ? "" : message.toString());
		if (nn(thrown))
			r.setThrown(thrown);
		r.setLoggerName(logger.getName());
		logger.log(r);
	}
}
