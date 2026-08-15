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

import static java.util.logging.Level.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.lang.ref.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

/**
 * Extended logger that provides convenience methods for logging formatted messages.
 *
 * <p>
 * This class extends {@link java.util.logging.Logger} and uses delegation internally to wrap
 * an underlying {@link java.util.logging.Logger} instance. This design provides:
 * </p>
 * <ul>
 * 	<li>Type compatibility - extends RichLogger for use anywhere a RichLogger is expected
 * 	<li>Flexibility - can wrap existing logger instances
 * 	<li>Convenience methods for formatted logging using {@link org.apache.juneau.commons.utils.Shorts#fs(String, Object...)}
 * </ul>
 *
 * <h5 class='section'>Features:</h5>
 * <ul>
 * 	<li>All standard {@link java.util.logging.Logger} functionality via delegation
 * 	<li>Convenience methods for formatted logging at all log levels
 * 	<li>Lazy evaluation of formatted messages using {@link org.apache.juneau.commons.utils.Shorts#fs(String, Object...)}
 * 	<li>Printf-style formatting (rendered via {@link org.apache.juneau.commons.utils.StringUtils#format(String, Object...)})
 * </ul>
 *
 * <h5 class='section'>Format Support:</h5>
 * <p>
 * The formatting methods use printf-style ({@link java.util.Formatter}) placeholders. MessageFormat-style
 * (<js>"{0}"</js>) patterns are <b>not</b> expanded here — use {@link org.apache.juneau.commons.utils.Shorts#mf(String, Object...)} /
 * {@link org.apache.juneau.commons.utils.StringUtils#mformat(String, Object...)} for those:
 * </p>
 * <ul>
 * 	<li><b>Printf-style:</b> <js>"%s"</js>, <js>"%d"</js>, <js>"%1$s"</js>, etc.
 * </ul>
 *
 * <p>
 * Juneau {@link RichLogger} patterns are printf-style. Because {@link LogRecord#getMessage()} returns the
 * already-substituted message, the {@link java.text.MessageFormat} parameter contract of the underlying
 * {@link java.util.logging.LogRecord} only surfaces if a record is serialized and consumed as a plain
 * {@link java.util.logging.LogRecord} by a non-Juneau handler.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	RichLogger <jv>logger</jv> = RichLogger.getLogger(MyClass.<jk>class</jk>.getName());
 *
 * 	<jc>// Formatted logging at different levels (printf-style placeholders)</jc>
 * 	<jv>logger</jv>.severe(<js>"Error processing user %s: %s"</js>, userId, error);
 * 	<jv>logger</jv>.warning(<js>"Failed to connect to %s after %d attempts"</js>, hostname, attempts);
 * 	<jv>logger</jv>.info(<js>"Processing %s items"</js>, count);
 * 	<jv>logger</jv>.fine(<js>"Debug value: %s"</js>, debugValue);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link org.apache.juneau.commons.utils.Shorts#fs(String, Object...)}
 * 	<li class='jc'>{@link org.apache.juneau.commons.utils.StringUtils#format(String, Object...)}
 * 	<li class='jc'>{@link java.util.logging.Logger}
 * </ul>
 */
@SuppressWarnings({
	"java:S100",  // Method names match java.util.logging.Logger for API compatibility
	"java:S1192", // String literals intentionally duplicated for clarity
	"java:S2176"  // Class name intentionally matches java.util.logging.Logger; extends it to provide Juneau-specific logging enhancements
})
public class RichLogger extends java.util.logging.Logger {

	/**
	 * Permanent registry of logger instances by name.
	 *
	 * <p>
	 * Uses an unbounded {@link ConcurrentHashMap} so every name maps to exactly one
	 * {@link RichLogger} instance for the JVM lifetime.  An evictable cache (e.g. one with a
	 * {@code maxSize} that clears the whole map) would cause a class-loaded
	 * {@code static final RichLogger} field to refer to an instance that is no longer in the
	 * registry; a subsequent call to {@link #getLogger(String)} would then return a new,
	 * different instance, breaking {@link LogRecordCapture} and any other code that
	 * assumes one name == one identity.
	 */
	private static final ConcurrentHashMap<String,LoggerRef> loggers = new ConcurrentHashMap<>();
	private static final ReferenceQueue<RichLogger> loggerQueue = new ReferenceQueue<>();

	private static final class LoggerRef extends WeakReference<RichLogger> {
		final String name;

		LoggerRef(String name, RichLogger referent, ReferenceQueue<RichLogger> q) {
			super(referent, q);
			this.name = name;
		}
	}

	/**
	 * The underlying logger instance that we delegate to.
	 */
	private final java.util.logging.Logger delegate;

	private final MessageGenerator generator;
	private final RichLogger canonical;

	/**
	 * List of log record listeners.
	 */
	private final List<LogRecordListener> listeners;
	private volatile boolean useParentListeners = true;

	/**
	 * Protected constructor - wraps an existing logger instance.
	 *
	 * @param delegate The underlying logger to delegate to.  Must not be <jk>null</jk>.
	 */
	protected RichLogger(java.util.logging.Logger delegate) {
		this(delegate, MessageGenerator.PRINTF, null);
	}

	private RichLogger(java.util.logging.Logger delegate, MessageGenerator generator, RichLogger canonical) {
		super(delegate.getName(), null);
		this.delegate = delegate;
		this.generator = generator == null ? MessageGenerator.PRINTF : generator;
		this.canonical = canonical == null ? this : canonical;
		this.listeners = canonical == null ? Collections.synchronizedList(new ArrayList<>()) : null;
	}

	/**
	 * Creates a logger for the specified name.
	 *
	 * <p>
	 * This method returns the same RichLogger instance for a given name, ensuring that
	 * listeners attached to a logger persist across multiple calls to {@link #getLogger(String)}.
	 * RichLogger instances are cached and automatically created using the underlying
	 * {@link java.util.logging.Logger#getLogger(String)}.
	 *
	 * @param name The logger name.  Must not be <jk>null</jk>.
	 * @return A logger instance (cached and reused for the same name).
	 */
	public static RichLogger getLogger(String name) {
		drainCollectedLoggers();
		while (true) {
			var currentRef = loggers.get(name);
			var current = currentRef == null ? null : currentRef.get();
			if (current != null)
				return current;
			var created = new RichLogger(java.util.logging.Logger.getLogger(name));
			var createdRef = new LoggerRef(name, created, loggerQueue);
			if (currentRef == null) {
				if (loggers.putIfAbsent(name, createdRef) == null)
					return created;
			} else if (loggers.replace(name, currentRef, createdRef))
				return created;
			drainCollectedLoggers();
		}
	}

	/**
	 * Creates a logger for the specified class.
	 *
	 * @param clazz The class.  Must not be <jk>null</jk>.
	 * @return A logger instance.
	 */
	public static RichLogger getLogger(Class<?> clazz) {
		return getLogger(cn(clazz));
	}

	static RichLogger findLogger(String name) {
		drainCollectedLoggers();
		var ref = loggers.get(name);
		if (ref == null)
			return null;
		var logger = ref.get();
		if (logger == null)
			loggers.remove(name, ref);
		return logger;
	}

	static void forEachLiveAncestor(String name, Consumer<RichLogger> action) {
		var ancestorName = parentName(name);
		while (ancestorName != null) {
			var ancestor = findLogger(ancestorName);
			if (ancestor != null)
				action.accept(ancestor);
			ancestorName = parentName(ancestorName);
		}
	}

	private static String parentName(String name) {
		if (name == null || name.isEmpty())
			return null;
		var i = name.lastIndexOf('.');
		return i == -1 ? "" : name.substring(0, i);
	}

	private static void drainCollectedLoggers() {
		LoggerRef ref;
		while ((ref = (LoggerRef)loggerQueue.poll()) != null)
			loggers.remove(ref.name, ref);
	}

	/**
	 * Creates a builder for the specified logger name.
	 *
	 * @param name The logger name.
	 * @return A new builder.
	 */
	public static Builder builder(String name) {
		return new Builder(name);
	}

	/**
	 * Creates a builder for the specified class logger name.
	 *
	 * @param clazz The class.
	 * @return A new builder.
	 */
	public static Builder builder(Class<?> clazz) {
		return builder(cn(clazz));
	}

	/**
	 * Builder for creating configured {@link RichLogger} views.
	 */
	public static class Builder {
		private final String name;
		private MessageGenerator generator = MessageGenerator.PRINTF;

		Builder(String name) {
			this.name = name;
		}

		public Builder printf() {
			generator = MessageGenerator.PRINTF;
			return this;
		}

		public Builder messageFormat() {
			generator = MessageGenerator.MESSAGE_FORMAT;
			return this;
		}

		public Builder generator(MessageGenerator value) {
			generator = value == null ? MessageGenerator.PRINTF : value;
			return this;
		}

		public RichLogger build() {
			var canonical = getLogger(name);
			return new RichLogger(canonical.delegate, generator, canonical);
		}
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
	@SuppressWarnings({
		"resource" // Caller takes ownership of the returned LogRecordCapture
	})
	public LogRecordCapture captureEvents() {
		return new LogRecordCapture(canonical);
	}

	@SuppressWarnings({
		"resource" // Caller takes ownership of the returned LogRecordCapture
	})
	public LogRecordCapture captureEvents(Level min) {
		return new LogRecordCapture(canonical, x -> x.getLevel().intValue() >= min.intValue());
	}

	@SuppressWarnings({
		"resource" // Caller takes ownership of the returned LogRecordCapture
	})
	public LogRecordCapture captureEvents(Predicate<java.util.logging.LogRecord> filter) {
		return new LogRecordCapture(canonical, filter);
	}

	/**
	 * Adds a log record listener.
	 *
	 * @param listener The listener to add.
	 */
	void addLogRecordListener(LogRecordListener listener) {
		canonical.listeners.add(listener);
	}

	/**
	 * Removes a log record listener.
	 *
	 * @param listener The listener to remove.
	 */
	void removeLogRecordListener(LogRecordListener listener) {
		canonical.listeners.remove(listener);
	}

	public void setUseParentListeners(boolean useParentListeners) {
		canonical.useParentListeners = useParentListeners;
	}

	public boolean isUseParentListeners() {
		return canonical.useParentListeners;
	}

	@Override
	public void log(java.util.logging.LogRecord record) {
		canonical.listeners.forEach(x -> x.onLogRecord(record));
		if (canonical.useParentListeners) {
			var ancestors = new ArrayList<RichLogger>();
			forEachLiveAncestor(canonical.getName(), ancestors::add);
			for (var ancestor : ancestors) {
				ancestor.listeners.forEach(x -> x.onLogRecord(record));
				if (!ancestor.useParentListeners)
					break;
			}
		}
		delegate.log(record);
	}

	private static boolean hasAnyListenerInChain(RichLogger logger) {
		if (!logger.listeners.isEmpty())
			return true;
		if (!logger.useParentListeners)
			return false;
		var ancestors = new ArrayList<RichLogger>();
		forEachLiveAncestor(logger.getName(), ancestors::add);
		for (var ancestor : ancestors) {
			if (!ancestor.listeners.isEmpty())
				return true;
			if (!ancestor.useParentListeners)
				break;
		}
		return false;
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
		if (!isLoggable(level) && !hasAnyListenerInChain(canonical))
			return;

		// Create LogRecord with lazy formatting support
		var rec = new LogRecord(getName(), level, msg, args, thrown, generator);
		this.log(rec);
	}

	private void logSupplier(Level level, Supplier<String> msgSupplier, Throwable thrown) {
		if (!isLoggable(level) && !hasAnyListenerInChain(canonical))
			return;
		var msg = msgSupplier == null ? null : msgSupplier.get();
		var rec = new LogRecord(getName(), level, msg, null, thrown, generator);
		this.log(rec);
	}

	// Convenience methods with formatted strings

	// Standard RichLogger methods - feed through central doLog method
	@Override
	public void severe(String msg) {
		doLog(SEVERE, msg, null, null);
	}

	@Override
	public void severe(Supplier<String> msgSupplier) {
		logSupplier(SEVERE, msgSupplier, null);
	}

	@Override
	public void warning(String msg) {
		doLog(WARNING, msg, null, null);
	}

	@Override
	public void warning(Supplier<String> msgSupplier) {
		logSupplier(WARNING, msgSupplier, null);
	}

	@Override
	public void info(String msg) {
		doLog(INFO, msg, null, null);
	}

	@Override
	public void info(Supplier<String> msgSupplier) {
		logSupplier(INFO, msgSupplier, null);
	}

	@Override
	public void config(String msg) {
		doLog(CONFIG, msg, null, null);
	}

	@Override
	public void config(Supplier<String> msgSupplier) {
		logSupplier(CONFIG, msgSupplier, null);
	}

	@Override
	public void fine(String msg) {
		doLog(FINE, msg, null, null);
	}

	@Override
	public void fine(Supplier<String> msgSupplier) {
		logSupplier(FINE, msgSupplier, null);
	}

	@Override
	public void finer(String msg) {
		doLog(FINER, msg, null, null);
	}

	@Override
	public void finer(Supplier<String> msgSupplier) {
		logSupplier(FINER, msgSupplier, null);
	}

	@Override
	public void finest(String msg) {
		doLog(FINEST, msg, null, null);
	}

	@Override
	public void finest(Supplier<String> msgSupplier) {
		logSupplier(FINEST, msgSupplier, null);
	}

	@Override
	public void log(Level level, String msg) {
		doLog(level, msg, null, null);
	}

	@Override
	public void log(Level level, Supplier<String> msgSupplier) {
		logSupplier(level, msgSupplier, null);
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
	 * @param level The log level.  Must not be <jk>null</jk>.
	 * @param pattern The format pattern.
	 * @param args The format arguments.
	 */
	public void logf(Level level, String pattern, Object...args) {
		doLog(level, pattern, args, null);
	}

	/**
	 * Logs a message at the specified level with formatted string and throwable.
	 *
	 * @param level The log level.  Must not be <jk>null</jk>.
	 * @param pattern The format pattern.
	 * @param thrown The throwable.
	 * @param args The format arguments.
	 */
	public void logf(Level level, String pattern, Throwable thrown, Object...args) {
		doLog(level, pattern, args, thrown);
	}
}
