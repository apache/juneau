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

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.logging.Handler;
import java.util.regex.*;

import org.apache.juneau.commons.function.*;

/**
 * Captures log records for testing purposes.
 *
 * <p>
 * This class implements {@link LogRecordListener} to receive log records and stores them
 * in memory for inspection during tests. It implements {@link Closeable} to automatically
 * remove itself from the logger's listeners when closed.
 *
 * <h5 class='section'>Usage:</h5>
 * <p class='bjava'>
 * 	<jc>// Capture records using try-with-resources</jc>
 * 	<jk>try</jk> (LogRecordCapture <jv>capture</jv> = RichLogger.getLogger(MyClass.<jk>class</jk>).captureEvents()) {
 * 		<jv>logger</jv>.info(<js>"Test message"</js>);
 * 		<jv>logger</jv>.warning(<js>"Warning message"</js>);
 *
 * 		<jc>// Inspect captured records</jc>
 * 		List&lt;LogRecord&gt; <jv>records</jv> = <jv>capture</jv>.getRecords();
 * 		assertEquals(2, <jv>records</jv>.size());
 *
 * 		<jc>// Get formatted messages</jc>
 * 		List&lt;String&gt; <jv>messages</jv> = <jv>capture</jv>.getRecords(<js>"{level}: {msg}"</js>);
 * 		assertEquals(<js>"INFO: Test message"</js>, <jv>messages</jv>.get(0));
 * 	}
 * </p>
 *
 * <h5 class='section'>Format String:</h5>
 * <p>
 * The format string supports placeholders as documented in {@link LogRecord#formatted(String)}.
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RichLogger#captureEvents()}
 * 	<li class='jc'>{@link LogRecord#formatted(String)}
 * </ul>
 */
public class LogRecordCapture implements LogRecordListener, Closeable {

	/**
	 * Runs a block while capturing, and keeping off the console, everything logged to the named logger or any of its descendants.
	 * <p>
	 * Unlike {@link RichLogger#captureEvents()}, which only observes records, this also stops the logger's records from
	 * reaching the parent (console) handlers for the duration of the block.  The logger's previous parent-handler setting
	 * is restored in a <c>finally</c> block.
	 * <p class='bjava'>
	 * 	List&lt;LogRecord&gt; <jv>records</jv> = LogRecordCapture.<jsm>quietly</jsm>(MyClass.<jk>class</jk>, () -&gt; <jv>obj</jv>.doSomethingThatWarns());
	 * 	assertEquals(1, <jv>records</jv>.size());
	 * </p>
	 *
	 * @param loggerName The logger name.  Records from descendant loggers are captured too.
	 * @param body The code to run.  Checked exceptions are rethrown wrapped in a {@link RuntimeException}.
	 * @return The captured records, in the order logged.  Never <jk>null</jk>.
	 */
	public static List<java.util.logging.LogRecord> quietly(String loggerName, Snippet body) {
		var records = Collections.synchronizedList(new ArrayList<java.util.logging.LogRecord>());
		var handler = new Handler() {
			@Override public void publish(java.util.logging.LogRecord r) { records.add(r); }
			@Override public void flush() { /* No-op. */ }
			@Override public void close() { /* No-op. */ }
		};
		var log = java.util.logging.Logger.getLogger(loggerName);
		try (var q = quiet(loggerName)) {
			log.addHandler(handler);
			try {
				body.run();
			} catch (RuntimeException | Error e) {
				throw e;
			} catch (Throwable e) {
				throw new RuntimeException(e);
			} finally {
				log.removeHandler(handler);
			}
		}
		synchronized (records) {
			return List.copyOf(records);
		}
	}

	/**
	 * Keeps everything logged to the named logger, and its descendants, off the console until the returned handle is closed.
	 * <p>
	 * Use this when a whole test class (for example from <ja>@BeforeAll</ja> / <ja>@AfterAll</ja>) expects a warning it is not
	 * asserting on.  Calls nest and may overlap across threads: the logger's original parent-handler setting is restored when
	 * the last outstanding handle for that logger name is closed.
	 *
	 * @param loggerName The logger name.
	 * @return A handle that restores the logger when closed.  Never <jk>null</jk>.
	 */
	public static Quiet quiet(String loggerName) {
		synchronized (QUIET) {
			var state = QUIET.computeIfAbsent(loggerName, k -> new QuietState(java.util.logging.Logger.getLogger(k)));
			if (state.count++ == 0) {
				state.useParent = state.log.getUseParentHandlers();
				state.log.setUseParentHandlers(false);
			}
			return () -> release(loggerName);
		}
	}

	/**
	 * Same as {@link #quiet(String)}, keyed by the logger's class name.
	 *
	 * @param loggerClass The class whose logger is silenced.
	 * @return A handle that restores the logger when closed.  Never <jk>null</jk>.
	 */
	public static Quiet quiet(Class<?> loggerClass) {
		return quiet(loggerClass.getName());
	}

	/**
	 * Handle returned by {@link #quiet(String)}.
	 */
	@FunctionalInterface
	public interface Quiet extends AutoCloseable {
		@Override void close();
	}

	private static final Map<String,QuietState> QUIET = new HashMap<>();

	private static final class QuietState {
		final java.util.logging.Logger log;  // Strong reference: the LogManager only holds loggers weakly.
		int count;
		boolean useParent;
		QuietState(java.util.logging.Logger log) { this.log = log; }
	}

	private static void release(String loggerName) {
		synchronized (QUIET) {
			var state = QUIET.get(loggerName);
			if (--state.count == 0) {
				state.log.setUseParentHandlers(state.useParent);
				QUIET.remove(loggerName);
			}
		}
	}

	/**
	 * Same as {@link #quietly(String, Snippet)}, keyed by the logger's class name.
	 *
	 * @param loggerClass The class whose logger is captured.
	 * @param body The code to run.
	 * @return The captured records, in the order logged.  Never <jk>null</jk>.
	 */
	public static List<java.util.logging.LogRecord> quietly(Class<?> loggerClass, Snippet body) {
		return quietly(loggerClass.getName(), body);
	}

	private final RichLogger logger;
	private final List<RichLogger> pinnedLoggers = new ArrayList<>();
	private final List<java.util.logging.LogRecord> records = Collections.synchronizedList(new ArrayList<>());
	private final Predicate<java.util.logging.LogRecord> filter;

	/**
	 * Constructor.
	 *
	 * @param logger The logger to capture records from.
	 */
	LogRecordCapture(RichLogger logger) {
		this(logger, x -> true);
	}

	LogRecordCapture(RichLogger logger, Predicate<java.util.logging.LogRecord> filter) {
		this.logger = logger;
		this.filter = filter == null ? x -> true : filter;
		pinnedLoggers.add(logger);
		RichLogger.forEachLiveAncestor(logger.getName(), pinnedLoggers::add);
		logger.addLogRecordListener(this);
	}

	/**
	 * Called when a log record is logged.
	 *
	 * @param rec The log record that was logged.
	 */
	@Override
	public void onLogRecord(java.util.logging.LogRecord rec) {
		if (filter.test(rec))
			records.add(rec);
	}

	/**
	 * Returns an unmodifiable list of all captured log records.
	 *
	 * @return An unmodifiable list of captured LogRecords.
	 */
	public List<java.util.logging.LogRecord> getRecords() {
		synchronized (records) {
			return List.copyOf(records);
		}
	}

	/**
	 * Returns captured log records formatted as strings.
	 *
	 * <p>
	 * The format string supports placeholders as documented in {@link LogRecord#formatted(String)}.
	 *
	 * @param format The format string with placeholders.
	 * @return A list of formatted record strings.
	 */
	public List<String> getRecords(String format) {
		synchronized (records) {
			return records.stream()
				.map(x -> LogRecord.formatted(x, format))
				.toList();
		}
	}

	/**
	 * Clears all captured records.
	 */
	public void clear() {
		records.clear();
	}

	/**
	 * Returns the number of captured records.
	 *
	 * @return The number of captured records.
	 */
	public int size() {
		return records.size();
	}

	/**
	 * Returns <jk>true</jk> if any records have been captured.
	 *
	 * @return <jk>true</jk> if records have been captured.
	 */
	public boolean isEmpty() {
		return records.isEmpty();
	}

	public List<String> messages() {
		synchronized (records) {
			return records.stream().map(java.util.logging.LogRecord::getMessage).toList();
		}
	}

	public java.util.logging.LogRecord last() {
		synchronized (records) {
			return records.isEmpty() ? null : records.get(records.size() - 1);
		}
	}

	public List<java.util.logging.LogRecord> byLevel(java.util.logging.Level level) {
		synchronized (records) {
			return records.stream().filter(x -> x.getLevel().equals(level)).toList();
		}
	}

	public List<java.util.logging.LogRecord> matching(String regex) {
		var p = Pattern.compile(regex);
		synchronized (records) {
			return records.stream().filter(x -> p.matcher(x.getMessage()).find()).toList();
		}
	}

	/**
	 * Returns the value of a {@link LogContext} entry attached to the specified captured record.
	 *
	 * <p>
	 * Convenience over {@link LogRecordContext#of(java.util.logging.LogRecord)} so capture-based tests do not need to
	 * know about the side table directly.
	 *
	 * @param rec The captured record.
	 * @param key The context key.
	 * @return The attached value for the key, or <jk>null</jk> if not present.
	 */
	public Object contextValue(java.util.logging.LogRecord rec, String key) {
		return LogRecordContext.of(rec).get(key);
	}

	public String assertMessage() {
		var r = last();
		return r == null ? null : r.getMessage();
	}

	public java.util.logging.Level assertLevel() {
		var r = last();
		return r == null ? null : r.getLevel();
	}

	public Throwable assertThrown() {
		var r = last();
		return r == null ? null : r.getThrown();
	}

	/**
	 * Closes this capture and removes it from the logger's listeners.
	 */
	@Override
	public void close() {
		logger.removeLogRecordListener(this);
		pinnedLoggers.clear();
	}
}
