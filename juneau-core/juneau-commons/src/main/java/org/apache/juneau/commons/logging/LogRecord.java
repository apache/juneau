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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.formatNamed;
import static org.apache.juneau.commons.utils.Utils.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.*;
import java.util.logging.Level;

import org.apache.juneau.commons.utils.*;

/**
 * LogRecord that supports lazy message formatting.
 *
 * <p>
 * This class extends {@link java.util.logging.LogRecord java.util.logging.LogRecord} and overrides {@link #getMessage()} to use
 * {@link org.apache.juneau.commons.utils.Utils#f(String, Object...)} for lazy formatting.
 * The message is only formatted when {@link #getMessage()} is actually called.
 *
 * <h5 class='section'>Usage:</h5>
 * <p>
 * This class is used internally by {@link Logger} when logging formatted messages.
 * The formatted message is only computed when the LogRecord's message is actually
 * accessed (e.g., by a Handler or Formatter).
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link Logger}
 * 	<li class='jc'>{@link org.apache.juneau.commons.utils.Utils#f(String, Object...)}
 * 	<li class='jc'>{@link java.util.logging.LogRecord}
 * </ul>
 */
@SuppressWarnings({
	"java:S115",  // Constants use UPPER_snakeCase convention
	"java:S100", // Class name intentionally matches java.util.logging.LogRecord
	"java:S1192" // Rename not desired; extends java.util.logging.LogRecord for compatibility
})
public class LogRecord extends java.util.logging.LogRecord {

	private static final long serialVersionUID = 1L;

	// Key constants for format placeholders
	private static final String KEY_date = "date";
	private static final String KEY_source = "source";
	private static final String KEY_logger = "logger";
	private static final String KEY_level = "level";
	private static final String KEY_msg = "msg";
	private static final String KEY_thrown = "thrown";
	private static final String KEY_timestamp = "timestamp";
	private static final String KEY_class = "class";
	private static final String KEY_method = "method";
	private static final String KEY_thread = "thread";
	private static final String KEY_threadid = "threadid";
	private static final String KEY_exception = "exception";

	private transient Supplier<Optional<StackTraceElement>> source = mem(()->findSource());

	/**
	 * Constructor.
	 *
	 * @param loggerName The logger name.
	 * @param level The log level.
	 * @param msg The message pattern (will be used as-is if args is null or empty).
	 * @param parameters The format arguments, or <jk>null</jk> if none.
	 * @param throwable The throwable, or <jk>null</jk> if none.
	 */
	public LogRecord(String loggerName, Level level, String msg, Object[] parameters, Throwable throwable) {
		super(level, msg);
		setLoggerName(loggerName);
		setParameters(isNotEmptyArray(parameters) ? parameters : null);
		setThrown(throwable);
	}

	/**
	 * Returns the log message, formatting it lazily if needed.
	 *
	 * <p>
	 * If parameters were set via {@link #setParameters(Object[])}, the message is formatted
	 * using {@link org.apache.juneau.commons.utils.Utils#f(String, Object...)} on first access.
	 *
	 * @return The log message, formatted if parameters are present.
	 */
	@Override
	public String getMessage() {
		var m = super.getMessage();
		var p = getParameters();
		return isEmptyArray(p) ? m : f(m, p);
	}

	/**
	 * Returns the source class name, calculating it lazily from the stack trace if not already set.
	 *
	 * @return The source class name, or <jk>null</jk> if not available.
	 */
	@Override
	public String getSourceClassName() {
		if (source == null)
			source = mem(()->findSource());
		return source.get().map(x -> x.getClassName()).orElse(null);
	}

	/**
	 * Returns the source method name, calculating it lazily from the stack trace if not already set.
	 *
	 * @return The source method name, or <jk>null</jk> if not available.
	 */
	@Override
	public String getSourceMethodName() {
		if (source == null)
			source = mem(()->findSource());
		return source.get().map(x -> x.getMethodName()).orElse(null);
	}

	@SuppressWarnings({
		"java:S3776" // Cognitive complexity acceptable for stack trace filtering
	})
	private static Optional<StackTraceElement> findSource() {
		for (var e : new Throwable().getStackTrace()) {
			var c = e.getClassName();
			var m = e.getMethodName();
			// Skip internal classes, logging classes, lambda methods/classes, and synthetic methods
			if (eq(c, cn(LogRecord.class)) || eq(c, cn(Logger.class)) || eq(c, cn(StringUtils.class)) || eq(c, cn(Utils.class))
				|| c.startsWith("java.util.logging.")
				|| (m != null && (m.contains("lambda$") || m.startsWith("access$")))
				|| c.contains("$$Lambda$") || (c.contains("/") && c.contains("$Lambda"))) {
				continue;
			}
			return opt(e);
		}
		return opte();
	}

	/**
	 * Formats this log record as a string using the specified format pattern.
	 *
	 * <p>
	 * Similar to how {@link java.util.logging.SimpleFormatter} formats log records, this method
	 * allows you to specify a custom format string with placeholders that are replaced with
	 * actual values from the log record.
	 *
	 * <p>
	 * The format string supports both named placeholders (e.g., <js>"{date}"</js>) and
	 * {@link java.util.Formatter Formatter}-style format specifiers (e.g., <js>"%1$tc"</js>).
	 * Named placeholders are replaced first, then the resulting string is formatted using
	 * {@link java.util.Formatter Formatter} with the following arguments:
	 * <ol>
	 * 	<li><c>date</c> - The date/time as a {@link Date} object
	 * 	<li><c>source</c> - The source class name and method name (e.g., "com.example.MyClass myMethod")
	 * 	<li><c>logger</c> - The logger name
	 * 	<li><c>level</c> - The log level
	 * 	<li><c>message</c> - The formatted log message
	 * 	<li><c>thrown</c> - The throwable object (if any)
	 * </ol>
	 *
	 * <h5 class='section'>Named Placeholders:</h5>
	 * <ul>
	 * 	<li><js>"{date}"</js> - The date/time formatted using {@link java.util.logging.SimpleFormatter SimpleFormatter}'s default date format
	 * 	<li><js>"{timestamp}"</js> - The date/time formatted as ISO-8601 (yyyy-MM-dd'T'HH:mm:ss.SSSZ)
	 * 	<li><js>"{class}"</js> - The source class name
	 * 	<li><js>"{method}"</js> - The source method name
	 * 	<li><js>"{source}"</js> - The source class name and method name (e.g., "com.example.MyClass myMethod")
	 * 	<li><js>"{logger}"</js> - The logger name
	 * 	<li><js>"{level}"</js> - The log level name
	 * 	<li><js>"{msg}"</js> - The log message (formatted if parameters are present)
	 * 	<li><js>"{thread}"</js> - The thread ID
	 * 	<li><js>"{threadid}"</js> - The thread ID (alias for {thread})
	 * 	<li><js>"{exception}"</js> - The exception message (if thrown)
	 * 	<li><js>"{thrown}"</js> - The throwable object (if thrown)
	 * </ul>
	 *
	 * <h5 class='section'>Formatter-Style Format Specifiers:</h5>
	 * <p>
	 * You can use {@link java.util.Formatter Formatter}-style format specifiers directly in the format string.
	 * The arguments are passed in the same order as {@link java.util.logging.SimpleFormatter SimpleFormatter}:
	 * </p>
	 * <ul>
	 * 	<li><js>"%1$s"</js> or <js>"%1$tc"</js> - The date/time (argument 1)
	 * 	<li><js>"%2$s"</js> - The source (argument 2)
	 * 	<li><js>"%3$s"</js> - The logger name (argument 3)
	 * 	<li><js>"%4$s"</js> - The log level (argument 4)
	 * 	<li><js>"%5$s"</js> - The log message (argument 5)
	 * 	<li><js>"%6$s"</js> - The throwable and its backtrace (argument 6)
	 * </ul>
	 *
	 * <p>
	 * For date/time formatting, you can use date/time conversion characters with argument 1:
	 * </p>
	 * <ul>
	 * 	<li><js>"%1$tc"</js> - Complete date and time (e.g., "Tue Mar 22 13:11:31 PDT 2011")
	 * 	<li><js>"%1$tb"</js> - Month abbreviation (e.g., "Mar")
	 * 	<li><js>"%1$td"</js> - Day of month (01-31)
	 * 	<li><js>"%1$tY"</js> - Year (4 digits)
	 * 	<li><js>"%1$tH"</js> - Hour (00-23)
	 * 	<li><js>"%1$tM"</js> - Minute (00-59)
	 * 	<li><js>"%1$tS"</js> - Second (00-59)
	 * 	<li><js>"%1$tL"</js> - Milliseconds (000-999)
	 * 	<li><js>"%1$tN"</js> - Nanoseconds (000000000-999999999, since JDK 9)
	 * 	<li><js>"%1$Tp"</js> - AM/PM marker (uppercase)
	 * 	<li><js>"%1$tp"</js> - AM/PM marker (lowercase)
	 * </ul>
	 *
	 * <p>
	 * The format string can also contain <js>"%n"</js> for newlines.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bjava'>
	 * 	LogRecord <jv>record</jv> = <jk>new</jk> LogRecord(Level.INFO, <js>"User {0} logged in"</js>, <js>"John"</js>);
	 *
	 * 	<jc>// Using named placeholders</jc>
	 * 	String <jv>formatted1</jv> = <jv>record</jv>.formatted(<js>"[{timestamp} {level}] {msg}"</js>);
	 * 	<jc>// Result: "[2025-01-22T07:42:45.123-0500 INFO] User John logged in"</jc>
	 *
	 * 	<jc>// Using Formatter-style specifiers</jc>
	 * 	String <jv>formatted2</jv> = <jv>record</jv>.formatted(<js>"%4$s: %5$s [%1$tc]%n"</js>);
	 * 	<jc>// Result: "INFO: User John logged in [Tue Jan 22 07:42:45 PST 2025]"</jc>
	 *
	 * 	<jc>// Mixed named placeholders and Formatter specifiers</jc>
	 * 	String <jv>formatted3</jv> = <jv>record</jv>.formatted(<js>"%1$tb %1$td, %1$tY {level}: {msg}%n"</js>);
	 * 	<jc>// Result: "Jan 22, 2025 INFO: User John logged in"</jc>
	 * </p>
	 *
	 * @param format The format string with placeholders and/or Formatter-style format specifiers.
	 * @return The formatted string.
	 * @see java.util.logging.SimpleFormatter
	 * @see java.util.Formatter
	 */
	@SuppressWarnings({
		"deprecation" // Date constructor is deprecated but needed for compatibility
	})
	public String formatted(String format) {
		var date = new Date(getMillis());
		Supplier<String> sourceName = () -> getSourceClassName() + ' ' + getSourceMethodName();

		Function<String, Object> resolver = key -> switch (key) {
			case KEY_date -> "%1$s";
			case KEY_source -> sourceName.get();  // Override default behavior since logging class doesn't handle classes outside of java.util.logging.
			case KEY_logger -> "%3$s";
			case KEY_level -> "%4$s";
			case KEY_msg -> "%5$s";
			case KEY_thrown -> "%6$s";
			case KEY_timestamp -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(date);
			case KEY_class -> getSourceClassName();
			case KEY_method -> getSourceMethodName();
			case KEY_thread, KEY_threadid -> s(getThreadID());
			case KEY_exception -> opt(getThrown()).map(x -> x.getMessage()).orElse("");
			default -> "";
		};

		return safeOptCatch(()->f(formatNamed(format, resolver), date, sourceName, getLoggerName(), getLevel(), getMessage(), getThrown()), x -> x.getLocalizedMessage()).orElse(null);
	}
}
