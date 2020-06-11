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
package org.apache.juneau.rest.mock2;

import java.util.*;
import java.util.logging.*;

/**
 * Simplified logger for intercepting and asserting logging messages.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Instantiate a mock logger.</jc>
 * 	MockLogger ml = <jk>new</jk> MockLogger();
 *
 * 	<jc>// Associate it with a MockRestClient.</jc>
 * 	MockRestClient
 * 		.<jsm>create</jsm>(MyRestResource.<jk>class</jk>)
 * 		.simpleJson()
 * 		.logger(ml)
 * 		.logRequests(DetailLevel.<jsf>FULL</jsf>, Level.<jsf>SEVERE</jsf>)
 * 		.build()
 * 		.post(<js>"/bean"</js>, bean)
 * 		.complete();
 *
 * 	<jc>// Assert that logging occurred.</jc>
 * 	ml.assertLevel(Level.<jsf>SEVERE</jsf>);
 * 	ml.assertMessageContains(
 * 		<js>"=== HTTP Call (outgoing) ======================================================"</js>,
 * 		<js>"=== REQUEST ==="</js>,
 * 		<js>"POST http://localhost/bean"</js>,
 * 		<js>"---request headers---"</js>,
 * 		<js>"	Accept: application/json+simple"</js>,
 * 		<js>"---request entity---"</js>,
 * 		<js>"application/json+simple"</js>,
 * 		<js>"---request content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== RESPONSE ==="</js>,
 * 		<js>"HTTP/1.1 200 "</js>,
 * 		<js>"---response headers---"</js>,
 * 		<js>"	Content-Type: application/json"</js>,
 * 		<js>"---response content---"</js>,
 * 		<js>"{f:1}"</js>,
 * 		<js>"=== END ======================================================================="</js>
 * 	);
 * </p>
 */
public class MockLogger extends Logger {

	private volatile List<LogRecord> logRecords = new ArrayList<>();

	/**
	 * Constructor.
	 */
	public MockLogger() {
		super("Mock", null);
	}

	@Override /* Logger */
	public synchronized void log(LogRecord record) {
		logRecords.add(record);
	}

	/**
	 * Resets this logger.
	 *
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger reset() {
		logRecords.clear();
		return this;
	}

	/**
	 * Asserts that this logger was called.
	 *
	 * @return This object (for method chaining).
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
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertLevel(Level level) {
		assertLogged();
		if (last().getLevel() != level)
			throw new AssertionError("Message logged at [" + last().getLevel() + "] instead of [" + level + "]");
		return this;
	}

	/**
	 * Asserts that the last message matched the specified message.
	 *
	 * @param message The message to search for.
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertMessage(String message) {
		assertLogged();
		if (! last().getMessage().equals(message))
			throw new AssertionError("Message was not [" + message + "].  Message=[" + last().getMessage() + "]");
		return this;
	}

	/**
	 * Asserts that the last message contained the specified text.
	 *
	 * @param messages The messages to search for.
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertMessageContains(String...messages) {
		assertLogged();
		for (String m : messages)
			if (! last().getMessage().contains(m))
				throw new AssertionError("Message did not contain [" + m + "].  Message=[" + last().getMessage() + "]");
		return this;
	}

	/**
	 * Asserts that the last message doesn't contained the specified text.
	 *
	 * @param messages The messages to search for.
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertMessageNotContains(String...messages) {
		assertLogged();
		for (String m : messages)
			if (last().getMessage().contains(m))
				throw new AssertionError("Message contained [" + m + "].  Message=[" + last().getMessage() + "]");
		return this;
	}

	/**
	 * Asserts that the specified number of messages have been logged.
	 *
	 * @param count Expected number of messages logged.
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertCount(int count) {
		assertLogged();
		if (logRecords.size() != count)
			throw new AssertionError("Wrong number of messages.  Expected=["+count+"], Actual=["+logRecords.size()+"]");
		return this;
	}

	private LogRecord last() {
		if (logRecords.isEmpty())
			throw new AssertionError("Message not logged");
		return logRecords.get(logRecords.size()-1);
	}
}
