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
package org.apache.juneau.internal;

import static java.util.logging.Level.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transforms.*;

/**
 * Wraps and extends the {@link java.util.logging.Logger} class to provide some additional convenience methods.
 */
public class JuneauLogger extends java.util.logging.Logger {

	private static final WriterSerializer serializer = JsonSerializer.create()
		.pojoSwaps(
			CalendarSwap.ISO8601DTZ.class,
			DateSwap.ISO8601DTZ.class,
			EnumerationSwap.class,
			IteratorSwap.class
		)
		.ssq()
		.build();

	private static final ConcurrentHashMap<Class<?>,String> rbMap = new ConcurrentHashMap<>();

	private final ResourceBundle rb;
	private final java.util.logging.Logger innerLogger;

	/**
	 * Get logger for specified class.
	 *
	 * @param forClass The class to create a logger for.
	 * @return A new <l>Logger</l>.
	 */
	public static JuneauLogger getLogger(Class<?> forClass) {
		return getLogger(forClass.getName());
	}

	/**
	 * Get logger for specified class.
	 *
	 * @param loggerName The logger name.
	 * @return A new <l>Logger</l>.
	 */
	public static JuneauLogger getLogger(String loggerName) {
		return new JuneauLogger(java.util.logging.Logger.getLogger(loggerName));
	}

	/**
	 * Get logger for specified class using the specified resource bundle name.
	 *
	 * @param forClass The class to create a logger for.
	 * @param resourceBundleName
	 * 	The name of the resource bundle.
	 * 	Can be any of the following formats:
	 * 	<ol>
	 * 		<li>An absolute path.  E.g. <js>"com/foo/nls/Messages"</js>.
	 * 		<li>A path relative to the package of the class.  E.g. <js>"nls/Messages"</js>.
	 * 	</ol>
	 * 	Both <js>'.'</js> and <js>'/'</js> can be used as path delimiters.
	 * @return A new <l>Logger</l>.
	 */
	public static JuneauLogger getLogger(Class<?> forClass, String resourceBundleName) {
		return new JuneauLogger(java.util.logging.Logger.getLogger(forClass.getName(), resolveResourceBundleName(forClass, resourceBundleName)));
	}

	/**
	 * Get logger with specified name using the specified resource bundle name.
	 *
	 * @param name The name of the logger to use.
	 * @param resourceBundleName
	 * 	The name of the resource bundle.
	 * 	Can be any of the following formats:
	 * 	<ol>
	 * 		<li>An absolute path.  E.g. <js>"com/foo/nls/Messages"</js>.
	 * 		<li>A path relative to the package of the class.  E.g. <js>"nls/Messages"</js>.
	 * 	</ol>
	 * 	Both <js>'.'</js> and <js>'/'</js> can be used as path delimiters.
	 * @return A new <l>Logger</l>.
	 */
	public static synchronized JuneauLogger getLogger(String name, String resourceBundleName) {
		return new JuneauLogger(java.util.logging.Logger.getLogger(name, resourceBundleName));
	}

	/**
	 * Constructor.
	 *
	 * @param innerLogger The wrapped logger.
	 */
	protected JuneauLogger(java.util.logging.Logger innerLogger) {
		super(innerLogger.getName(), innerLogger.getResourceBundleName());
		this.innerLogger = innerLogger;
		this.rb = getResourceBundle();
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#SEVERE} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void severe(String msg, Object...args) {
		if (isLoggable(SEVERE))
			log(SEVERE, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#WARNING} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void warning(String msg, Object...args) {
		if (isLoggable(WARNING))
			log(WARNING, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#INFO} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void info(String msg, Object...args) {
		if (isLoggable(INFO))
			log(INFO, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#CONFIG} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void config(String msg, Object...args) {
		if (isLoggable(CONFIG))
			log(CONFIG, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#FINE} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void fine(String msg, Object...args) {
		if (isLoggable(FINE))
			log(FINE, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#FINER} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void finer(String msg, Object...args) {
		if (isLoggable(FINER))
			log(FINER, msg, args);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#FINEST} level.
	 *
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void finest(String msg, Object...args) {
		if (isLoggable(FINEST))
			log(FINEST, msg, args);
	}

	/**
	 * Logs an exception as {@link Level#SEVERE} level.
	 *
	 * @param t The Throwable object to log.
	 */
	public void severe(Throwable t) {
		if (isLoggable(SEVERE))
			log(SEVERE, t.getLocalizedMessage(), t);
	}

	/**
	 * Logs an exception as {@link Level#WARNING} level.
	 *
	 * @param t The Throwable object to log.
	 */
	public void warning(Throwable t) {
		if (isLoggable(WARNING))
			log(WARNING, t.getLocalizedMessage(), t);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#SEVERE} level.
	 *
	 * @param t The Throwable object associated with the event that needs to be logged.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void severe(Throwable t, String msg, Object...args) {
		if (isLoggable(SEVERE))
			log(SEVERE, getMessage(msg, args), t);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#WARNING} level.
	 *
	 * @param t The Throwable object associated with the event that needs to be logged.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void warning(Throwable t, String msg, Object...args) {
		if (isLoggable(WARNING))
			log(WARNING, getMessage(msg, args), t);
	}

	/**
	 * Logs a message with the specified {@link MessageFormat}-style arguments at {@link Level#INFO} level.
	 *
	 * @param t The Throwable object associated with the event that needs to be logged.
	 * @param msg The message to log.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void info(Throwable t, String msg, Object...args) {
		if (isLoggable(INFO))
			log(INFO, getMessage(msg, args), t);
	}

	@Override /* Logger */
	public void log(LogRecord record) {
		innerLogger.log(record);
	}

	@Override /* Logger */
	public boolean isLoggable(Level level) {
		return innerLogger.isLoggable(level);
	}

	/**
	 * Similar to {@link #log(Level, String, Object[])}, except arguments are converted to objects
	 * that are serialized using the {@link JsonSerializer#toStringObject(Object)} method.
	 *
	 * <p>
	 * This allows arbitrary POJOs to be serialized as message parameters.
	 *
	 * @param level The level of the given message.
	 * @param msg The message to log.
	 * @param args The POJO arguments.
	 */
	public void logObjects(Level level, String msg, Object...args) {
		if (isLoggable(level)) {
			for (int i = 0; i < args.length; i++)
				args[i] = serializer.toStringObject(args[i]);
			log(level, msg, args);
		}
	}

	private String getMessage(String msg, Object...args) {
		if (args.length == 0)
			return msg;
		if (rb != null && rb.containsKey(msg))
			msg = rb.getString(msg);
		return format(msg, args);
	}

	private static String resolveResourceBundleName(Class<?> forClass, String path) {
		if (isEmpty(path))
			return null;
		String rb = rbMap.get(forClass);
		if (rb == null) {
			path = path.replace('/', '.');
			if (path.startsWith("."))
				path = path.substring(1);
			ClassLoader cl = forClass.getClassLoader();
			try {
				ResourceBundle.getBundle(path, Locale.getDefault(), cl);
				rbMap.putIfAbsent(forClass, path);
			} catch (MissingResourceException e) {
				try {
					path = forClass.getPackage().getName() + '.' + path;
					ResourceBundle.getBundle(path, Locale.getDefault(), cl);
					rbMap.putIfAbsent(forClass, path);
				} catch (MissingResourceException e2) {
					rbMap.putIfAbsent(forClass, "");
				}
			}
			rb = rbMap.get(forClass);
		}
		return ("".equals(rb) ? null : rb);
	}
}
