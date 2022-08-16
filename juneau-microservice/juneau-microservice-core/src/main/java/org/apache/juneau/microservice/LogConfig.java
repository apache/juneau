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
package org.apache.juneau.microservice;

import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import org.apache.juneau.microservice.resources.*;

/**
 * Can be used for configuration of simple logging in the microservice.
 *
 * <p>
 * Instances of this class can be created using {@link #create()} and passing the result to
 * {@link Microservice.Builder#logConfig(LogConfig)}.
 *
 * <p>
 * These values override values specified in the <js>"Logging"</js> configuration section.
 */
public class LogConfig {

	String logFile, logDir;
	Boolean append;
	Integer limit, count;
	Level fileLevel, consoleLevel;
	Map<String,Level> levels = new LinkedHashMap<>();
	Formatter formatter;

	LogConfig() {}

	/**
	 * Copy constructor.
	 *
	 * @param copyFrom The log config to copy from.
	 */
	protected LogConfig(LogConfig copyFrom) {
		this.logFile = copyFrom.logFile;
		this.logDir = copyFrom.logDir;
		this.append = copyFrom.append;
		this.limit = copyFrom.limit;
		this.count = copyFrom.count;
		this.fileLevel = copyFrom.fileLevel;
		this.consoleLevel = copyFrom.consoleLevel;
		this.levels = new LinkedHashMap<>(copyFrom.levels);
		this.formatter = copyFrom.formatter;
	}

	/**
	 * Creates a copy of this log configuration.
	 *
	 * @return A new copy of this log configuration.
	 */
	public LogConfig copy() {
		return new LogConfig(this);
	}

	/**
	 * Creates a new instance of this config.
	 *
	 * @return A new instance of this config.
	 */
	public static LogConfig create() {
		return new LogConfig();
	}

	/**
	 * Returns the name of the log file on the file system to store the log file for this microservice.
	 *
	 * <p>
	 * This overrides the configuration value <js>"Logging/logFile"</js>.
	 * If not specified, no file system logging will be used.
	 *
	 * @param logFile The log file.
	 * @return This object (for method chaining).
	 */
	public LogConfig logFile(String logFile) {
		this.logFile = logFile;
		return this;
	}

	/**
	 * The location of the log directory to create the log file.
	 *
	 * <p>
	 * This overrides the configuration value <js>"Logging/logDir"</js>.
	 * If not specified, uses the JVM working directory.
	 *
	 * @param logDir The log directory location as a path relative to the working directory.
	 * @return This object (for method chaining).
	 */
	public LogConfig logDir(String logDir) {
		this.logDir = logDir;
		return this;
	}

	/**
	 * The log entry formatter.
	 *
	 * <p>
	 * If not specified, uses the following values pulled from the configuration to construct a {@link LogEntryFormatter}:
	 * <ul>
	 * 	<li><js><js>"Logging/format"</js> (default is <js>"[{date} {level}] {msg}%n"</js>)
	 * 	<li><js><js>"Logging/dateFormat"</js> (default is <js>"yyyy.MM.dd hh:mm:ss"</js>)
	 * 	<li><js><js>"Logging/useStackTraceHashes"</js> (default is <jk>false</jk>)
	 * </ul>
	 *
	 *
	 * @param formatter The log entry formatter.
	 * @return This object (for method chaining).
	 * @see LogEntryFormatter
	 */
	public LogConfig formatter(Formatter formatter) {
		this.formatter = formatter;
		return this;
	}

	/**
	 * Specified append mode for the log file.
	 *
	 * @return This object (for method chaining).
	 */
	public LogConfig append() {
		this.append = true;
		return this;
	}

	/**
	 * The maximum number of bytes to write to any one log file.
	 *
	 * @param limit The number of bytes.
	 * @return This object (for method chaining).
	 */
	public LogConfig limit(int limit) {
		this.limit = limit;
		return this;
	}

	/**
	 * The number of log files to use.
	 *
	 * @param count The number of log files.
	 * @return This object (for method chaining).
	 */
	public LogConfig count(int count) {
		this.count = count;
		return this;
	}

	/**
	 * The default logging level for the log file.
	 *
	 * @param fileLevel The logging level.
	 * @return This object (for method chaining).
	 */
	public LogConfig fileLevel(Level fileLevel) {
		this.fileLevel = fileLevel;
		return this;
	}

	/**
	 * The default logging level for the console.
	 *
	 * @param consoleLevel The logging level.
	 * @return This object (for method chaining).
	 */
	public LogConfig consoleLevel(Level consoleLevel) {
		this.consoleLevel = consoleLevel;
		return this;
	}

	/**
	 * Default logging levels for loggers.
	 *
	 * @param levels A map of logger names to logger levels.
	 * @return This object (for method chaining).
	 */
	public LogConfig levels(Map<String,Level> levels) {
		this.levels.putAll(levels);
		return this;
	}

	/**
	 * Default logging level for logger.
	 *
	 * @param logger Logger name.
	 * @param level Logger level.
	 * @return This object (for method chaining).
	 */
	public LogConfig level(String logger, Level level) {
		this.levels.put(logger, level);
		return this;
	}
}
