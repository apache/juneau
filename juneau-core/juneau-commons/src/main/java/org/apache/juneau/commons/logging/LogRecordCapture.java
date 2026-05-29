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

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * 	<jk>try</jk> (LogRecordCapture <jv>capture</jv> = Logger.getLogger(MyClass.<jk>class</jk>).captureEvents()) {
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
 * 	<li class='jc'>{@link Logger#captureEvents()}
 * 	<li class='jc'>{@link LogRecord#formatted(String)}
 * </ul>
 */
public class LogRecordCapture implements LogRecordListener, Closeable {

	private final Logger logger;
	private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Constructor.
	 *
	 * @param logger The logger to capture records from.
	 */
	LogRecordCapture(Logger logger) {
		this.logger = logger;
		logger.addLogRecordListener(this);
	}

	/**
	 * Called when a log record is logged.
	 *
	 * @param rec The log record that was logged.
	 */
	@Override
	public void onLogRecord(LogRecord rec) {
		records.add(rec);
	}

	/**
	 * Returns an unmodifiable list of all captured log records.
	 *
	 * @return An unmodifiable list of captured LogRecords.
	 */
	public List<LogRecord> getRecords() {
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
				.map(LogRecord.class::cast)
				.map(x -> x.formatted(format))
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

	/**
	 * Closes this capture and removes it from the logger's listeners.
	 */
	@Override
	public void close() {
		logger.removeLogRecordListener(this);
	}
}
