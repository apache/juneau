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

/**
 * Interface for listening to log records.
 *
 * <p>
 * Implementations of this interface can be registered with a {@link Logger} to receive
 * notifications when log records are logged.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link LogRecordCapture}
 * 	<li class='jc'>{@link Logger}
 * </ul>
 */
public interface LogRecordListener {

	/**
	 * Called when a log record is logged.
	 *
	 * @param record The log record that was logged.
	 */
	void onLogRecord(LogRecord record);
}
