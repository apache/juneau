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
package org.apache.juneau.rest.mock;

import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import org.apache.juneau.assertions.*;

/**
 * Simplified logger for intercepting and asserting logging messages.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Instantiate a mock logger.</jc>
 * 	MockLogger <jv>logger</jv> = <jk>new</jk> MockLogger();
 *
 * 	<jc>// Associate it with a MockRestClient.</jc>
 * 	MockRestClient
 * 		.<jsm>create</jsm>(MyRestResource.<jk>class</jk>)
 * 		.json5()
 * 		.logger(<jv>logger</jv>)
 * 		.logRequests(DetailLevel.<jsf>FULL</jsf>, Level.<jsf>SEVERE</jsf>)
 * 		.build()
 * 		.post(<js>"/bean"</js>, <jv>bean</jv>)
 * 		.complete();
 *
 * 	<jc>// Assert that logging occurred.</jc>
 * 	<jv>logger</jv>.assertLastLevel(Level.<jsf>SEVERE</jsf>);
 * 	<jv>logger</jv>.assertLastMessage().is(
 * 		<js>"=== HTTP Call (outgoing) ======================================================"</js>,
 * 		<js>"=== REQUEST ==="</js>,
 * 		<js>"POST http://localhost/bean"</js>,
 * 		<js>"---request headers---"</js>,
 * 		<js>"	Accept: application/json5"</js>,
 * 		<js>"---request entity---"</js>,
 * 		<js>"	Content-Type: application/json5"</js>,
 * 		<js>"---request content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== RESPONSE ==="</js>,
 * 		<js>"HTTP/1.1 200 "</js>,
 * 		<js>"---response headers---"</js>,
 * 		<js>"	Content-Type: application/json"</js>,
 * 		<js>"---response content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== END ======================================================================="</js>,
 * 		<js>""</js>
 * 	);
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-rest-mock">juneau-rest-mock</a>
 * </ul>
 */
public class MockLogger extends Logger {

	private static final String FORMAT_PROPERTY = "java.util.logging.SimpleFormatter.format";

	private final List<LogRecord> logRecords = list();
	private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private volatile Formatter formatter;
	private volatile String format = "%4$s: %5$s%6$s%n";

	/**
	 * Constructor.
	 */
	public MockLogger() {
		super("Mock", null);
	}

	/**
	 * Creator.
	 *
	 * @return A new {@link MockLogger} object.
	 */
	public static MockLogger create() {
		return new MockLogger();
	}

	@Override /* Logger */
	public synchronized void log(LogRecord record) {
		logRecords.add(record);
		try {
			baos.write(getFormatter().format(record).getBytes("UTF-8"));
		} catch (Exception e) {
			throw asRuntimeException(e);
		}
	}

	private Formatter getFormatter() {
		if (formatter == null) {
			synchronized(this) {
				String oldFormat = System.getProperty(FORMAT_PROPERTY);
				System.setProperty(FORMAT_PROPERTY, format);
				formatter = new SimpleFormatter();
				if (oldFormat == null)
					System.clearProperty(FORMAT_PROPERTY);
				else
					System.setProperty(FORMAT_PROPERTY, oldFormat);
			}
		}
		return formatter;
	}

	/**
	 * Sets the level for this logger.
	 *
	 * @param level The new level for this logger.
	 * @return This object.
	 */
	public synchronized MockLogger level(Level level) {
		super.setLevel(level);
		return this;
	}

	/**
	 * Specifies the format for messages sent to the log file.
	 *
	 * <p>
	 * See {@link SimpleFormatter#format(LogRecord)} for the syntax of this string.
	 *
	 * @param format The format string.
	 * @return This object.
	 */
	public synchronized MockLogger format(String format) {
		this.format = format;
		return this;
	}

	/**
	 * Overrides the formatter to use for formatting messages.
	 *
	 * <p>
	 * The default uses {@link SimpleFormatter}.
	 *
	 * @param formatter The log record formatter.
	 * @return This object.
	 */
	public synchronized MockLogger formatter(Formatter formatter) {
		this.formatter = formatter;
		return this;
	}

	/**
	 * Resets this logger.
	 *
	 * @return This object.
	 */
	public synchronized MockLogger reset() {
		logRecords.clear();
		baos.reset();
		return this;
	}

	/**
	 * Asserts that this logger was called.
	 *
	 * @return This object.
	 */
	public synchronized MockLogger assertLogged() {
		if (logRecords.isEmpty())
			throw new AssertionError("Message not logged");
		return this;
	}

	/**
	 * Asserts that the last message was logged at the specified level.
	 *
	 * @param level The level to match against.
	 * @return This object.
	 */
	public synchronized MockLogger assertLastLevel(Level level) {
		assertLogged();
		if (last().getLevel() != level)
			throw new AssertionError("Message logged at [" + last().getLevel() + "] instead of [" + level + "]");
		return this;
	}

	/**
	 * Asserts that the last message matched the specified message.
	 *
	 * @return This object.
	 */
	public synchronized FluentStringAssertion<MockLogger> assertLastMessage() {
		assertLogged();
		return new FluentStringAssertion<>(last().getMessage(), this);
	}

	/**
	 * Asserts that the specified number of messages have been logged.
	 *
	 * @return This object.
	 */
	public synchronized FluentIntegerAssertion<MockLogger> assertRecordCount() {
		return new FluentIntegerAssertion<>(logRecords.size(), this);
	}

	/**
	 * Allows you to perform fluent-style assertions on the contents of the log file.
	 *
	 * @return A new fluent-style assertion object.
	 */
	public synchronized FluentStringAssertion<MockLogger> assertContents() {
		return new FluentStringAssertion<>(baos.toString(), this);
	}

	private LogRecord last() {
		if (logRecords.isEmpty())
			throw new AssertionError("Message not logged");
		return logRecords.get(logRecords.size()-1);
	}

	/**
	 * Returns the contents of this log file as a string.
	 */
	@Override
	public String toString() {
		return baos.toString();
	}
}
