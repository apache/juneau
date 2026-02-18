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
package org.apache.juneau.commons.logging;

import static org.apache.juneau.commons.utils.Utils.*;
import static java.util.logging.Level.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.apache.juneau.commons.collections.Cache;

/**
 * Extended logger that provides convenience methods for logging formatted messages.
 *
 * <p>
 * This class extends {@link java.util.logging.Logger} and uses delegation internally to wrap
 * an underlying {@link java.util.logging.Logger} instance. This design provides:
 * </p>
 * <ul>
 * 	<li>Type compatibility - extends Logger for use anywhere a Logger is expected
 * 	<li>Flexibility - can wrap existing logger instances
 * 	<li>Convenience methods for formatted logging using {@link org.apache.juneau.commons.utils.Utils#fs(String, Object...)}
 * </ul>
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 * 	<li>All standard {@link java.util.logging.Logger} functionality via delegation
 * 	<li>Convenience methods for formatted logging at all log levels
 * 	<li>Lazy evaluation of formatted messages using {@link Utils#fs(String, Object...)}
 * 	<li>Support for both MessageFormat-style and printf-style formatting
 * </ul>
 *
 * <h5 class='section'>Format Support:</h5>
 * <p>
 * The formatting methods support both MessageFormat-style and printf-style formatting:
 * </p>
 * <ul>
 * 	<li><b>Printf-style:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%.2f"</js>, etc.
 * 	<li><b>MessageFormat-style:</b> <js>"{0}"</js>, <js>"{1,number}"</js>, etc.
 * 	<li><b>Un-numbered MessageFormat:</b> <js>"{}"</js> - Sequential placeholders
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	Logger <jv>logger</jv> = Logger.getLogger(MyClass.<jk>class</jk>.getName());
 *
 * 	<jc>// Formatted logging at different levels</jc>
 * 	<jv>logger</jv>.severe(<js>"Error processing user {0}: {1}"</js>, userId, error);
 * 	<jv>logger</jv>.warning(<js>"Failed to connect to %s after %d attempts"</js>, hostname, attempts);
 * 	<jv>logger</jv>.info(<js>"Processing {} items"</js>, count);
 * 	<jv>logger</jv>.fine(<js>"Debug value: %s"</js>, debugValue);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.commons.utils.Utils#fs(String, Object...)}
 * 	<li class='jc'>{@link org.apache.juneau.commons.utils.Utils#f(String, Object...)}
 * 	<li class='jc'>{@link java.util.logging.Logger}
 * </ul>
 */
@SuppressWarnings({
	"java:S100",  // Method names match java.util.logging.Logger for API compatibility
	"java:S1192"  // String literals intentionally duplicated for clarity
})
public class Logger extends java.util.logging.Logger {

	/**
	 * Cache of logger instances by name.
	 *
	 * <p>
	 * Logger instances are cached and reused for the same name, ensuring that listeners
	 * attached to a logger persist across multiple calls to {@link #getLogger(String)}.
	 */
	private static final Cache<String, Logger> loggers = Cache.of(String.class, Logger.class).supplier(k -> new Logger(java.util.logging.Logger.getLogger(k))).build();

	/**
	 * The underlying logger instance that we delegate to.
	 */
	private final java.util.logging.Logger delegate;

	/**
	 * List of log record listeners.
	 */
	private final List<LogRecordListener> listeners = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Protected constructor - wraps an existing logger instance.
	 *
	 * @param delegate The underlying logger to delegate to.
	 */
	protected Logger(java.util.logging.Logger delegate) {
		super(delegate.getName(), null);
		this.delegate = delegate;
	}

	/**
	 * Creates a logger for the specified name.
	 *
	 * <p>
	 * This method returns the same Logger instance for a given name, ensuring that
	 * listeners attached to a logger persist across multiple calls to {@link #getLogger(String)}.
	 * Logger instances are cached and automatically created using the underlying
	 * {@link java.util.logging.Logger#getLogger(String)}.
	 *
	 * @param name The logger name.
	 * @return A logger instance (cached and reused for the same name).
	 */
	public static Logger getLogger(String name) {
		return loggers.get(name);
	}

	/**
	 * Creates a logger for the specified class.
	 *
	 * @param clazz The class.
	 * @return A logger instance.
	 */
	public static Logger getLogger(Class<?> clazz) {
		return getLogger(cn(clazz));
	}

	/**
	 * Creates a log record capture for testing.
	 *
	 * <p>
	 * This method creates a {@link LogRecordCapture} that will capture all log records
	 * from this logger. The capture should be closed when done (typically using try-with-resources).
	 *
	 * @return A LogRecordCapture instance.
	 */
	public LogRecordCapture captureEvents() {
		return new LogRecordCapture(this);
	}

	/**
	 * Adds a log record listener.
	 *
	 * @param listener The listener to add.
	 */
	void addLogRecordListener(LogRecordListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a log record listener.
	 *
	 * @param listener The listener to remove.
	 */
	void removeLogRecordListener(LogRecordListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Central logging method that all logging calls feed through.
	 *
	 * <p>
	 * This method creates a {@link LogRecord}, notifies all listeners, and then delegates
	 * to the underlying logger.
	 *
	 * <p>
	 * The method will proceed even if the log level is not loggable if there are listeners
	 * attached, allowing listeners to capture all log records regardless of level.
	 *
	 * @param level The log level.
	 * @param msg The log message or format pattern.
	 * @param args The format arguments, or <jk>null</jk> if none.
	 * @param thrown The throwable, or <jk>null</jk> if none.
	 */
	private void doLog(Level level, String msg, Object[] args, Throwable thrown) {
		if (!isLoggable(level) && listeners.isEmpty())
			return;

		// Create LogRecord with lazy formatting support
		var rec = new LogRecord(getName(), level, msg, args, thrown);

		// Notify all listeners
		listeners.forEach(x -> x.onLogRecord(rec));

		// Delegate to underlying logger (LogRecord extends java.util.logging.LogRecord)
		delegate.log(rec);
	}

	// Convenience methods with formatted strings

	// Standard Logger methods - feed through central doLog method
	@Override
	public void severe(String msg) {
		doLog(SEVERE, msg, null, null);
	}

	@Override
	public void warning(String msg) {
		doLog(WARNING, msg, null, null);
	}

	@Override
	public void info(String msg) {
		doLog(INFO, msg, null, null);
	}

	@Override
	public void config(String msg) {
		doLog(CONFIG, msg, null, null);
	}

	@Override
	public void fine(String msg) {
		doLog(FINE, msg, null, null);
	}

	@Override
	public void finer(String msg) {
		doLog(FINER, msg, null, null);
	}

	@Override
	public void finest(String msg) {
		doLog(FINEST, msg, null, null);
	}

	@Override
	public void log(Level level, String msg) {
		doLog(level, msg, null, null);
	}

	@Override
	public void log(Level level, String msg, Object param1) {
		doLog(level, msg, new Object[]{param1}, null);
	}

	@Override
	public void log(Level level, String msg, Object[] params) {
		doLog(level, msg, params, null);
	}

	@Override
	public void log(Level level, String msg, Throwable thrown) {
		doLog(level, msg, null, thrown);
	}

	@Override
	public boolean isLoggable(Level level) {
		return delegate.isLoggable(level);
	}

	@Override
	public Level getLevel() {
		return delegate.getLevel();
	}

	@Override
	public void setLevel(Level newLevel) {
		delegate.setLevel(newLevel);
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public void addHandler(Handler handler) {
		delegate.addHandler(handler);
	}

	@Override
	public void removeHandler(Handler handler) {
		delegate.removeHandler(handler);
	}

	@Override
	public Handler[] getHandlers() {
		return delegate.getHandlers();
	}

	// Convenience methods with formatted strings

	/**
	 * Logs a SEVERE level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void severe(String pattern, Object...args) {
		doLog(SEVERE, pattern, args, null);
	}

	/**
	 * Logs a SEVERE level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void severe(Throwable thrown, String pattern, Object...args) {
		doLog(SEVERE, pattern, args, thrown);
	}

	/**
	 * Logs a WARNING level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void warning(String pattern, Object...args) {
		doLog(WARNING, pattern, args, null);
	}

	/**
	 * Logs a WARNING level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void warning(Throwable thrown, String pattern, Object...args) {
		doLog(WARNING, pattern, args, thrown);
	}

	/**
	 * Logs an INFO level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void info(String pattern, Object...args) {
		doLog(INFO, pattern, args, null);
	}

	/**
	 * Logs an INFO level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void info(Throwable thrown, String pattern, Object...args) {
		doLog(INFO, pattern, args, thrown);
	}

	/**
	 * Logs a CONFIG level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void config(String pattern, Object...args) {
		doLog(CONFIG, pattern, args, null);
	}

	/**
	 * Logs a CONFIG level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void config(Throwable thrown, String pattern, Object...args) {
		doLog(CONFIG, pattern, args, thrown);
	}

	/**
	 * Logs a FINE level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void fine(String pattern, Object...args) {
		doLog(FINE, pattern, args, null);
	}

	/**
	 * Logs a FINE level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void fine(Throwable thrown, String pattern, Object...args) {
		doLog(FINE, pattern, args, thrown);
	}

	/**
	 * Logs a FINER level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void finer(String pattern, Object...args) {
		doLog(FINER, pattern, args, null);
	}

	/**
	 * Logs a FINER level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void finer(Throwable thrown, String pattern, Object...args) {
		doLog(FINER, pattern, args, thrown);
	}

	/**
	 * Logs a FINEST level message with formatted string.
	 *
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void finest(String pattern, Object...args) {
		doLog(FINEST, pattern, args, null);
	}

	/**
	 * Logs a FINEST level message with formatted string and throwable.
	 *
	 * @param thrown The throwable.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void finest(Throwable thrown, String pattern, Object...args) {
		doLog(FINEST, pattern, args, thrown);
	}

	/**
	 * Logs a message at the specified level with formatted string.
	 *
	 * @param level The log level.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void logf(Level level, String pattern, Object...args) {
		doLog(level, pattern, args, null);
	}

	/**
	 * Logs a message at the specified level with formatted string and throwable.
	 *
	 * @param level The log level.
	 * @param pattern The format pattern.
	 * @param thrown The throwable.
	 * @param args The format arguments.
	 */
	public void logf(Level level, String pattern, Throwable thrown, Object...args) {
		doLog(level, pattern, args, thrown);
	}
}
