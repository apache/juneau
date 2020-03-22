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

import java.util.logging.*;

/**
 * Simplified logger for intercepting and asserting logging messages.
 */
public class MockLogger extends Logger {

	private volatile LogRecord logRecord;

	/**
	 * Constructor.
	 */
	public MockLogger() {
		super("Mock", null);
	}

	@Override /* Logger */
	public synchronized void log(LogRecord record) {
		this.logRecord = record;
	}

	/**
	 * Resets this logger.
	 *
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger reset() {
		this.logRecord = null;
		return this;
	}

	/**
	 * Asserts that this logger was called.
	 *
	 * @return This object (for method chaining).
	 */
	public synchronized MockLogger assertLogged() {
		if (logRecord == null)
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
		if (logRecord.getLevel() != level)
			throw new AssertionError("Message logged at [" + logRecord.getLevel() + "] instead of [" + level + "]");
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
		if (! logRecord.getMessage().equals(message))
			throw new AssertionError("Message was not [" + message + "].  Message=[" + logRecord.getMessage() + "]");
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
			if (! logRecord.getMessage().contains(m))
				throw new AssertionError("Message did not contain [" + m + "].  Message=[" + logRecord.getMessage() + "]");
		return this;
	}
}
