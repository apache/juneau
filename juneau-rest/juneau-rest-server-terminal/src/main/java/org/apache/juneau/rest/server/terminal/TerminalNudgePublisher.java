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
package org.apache.juneau.rest.server.terminal;

/**
 * Receives a notice that a terminal's bytes have grown.
 *
 * <p>
 * Sources such as {@link MemoryTerminalSource} and {@link TerminalProcess} call this after bytes are appended and once
 * more when no further bytes will arrive.  It is a hint only: a reader still fetches the bytes through
 * {@link TerminalSource#read(long, int)}.
 *
 * <p>
 * Implementations must not block and must not throw; callers still guard against a throwing implementation.
 */
@FunctionalInterface
public interface TerminalNudgePublisher {

	/**
	 * Reports that a terminal's bytes now end at an offset.
	 *
	 * @param terminalId The terminal id.
	 * @param end The offset one past the last byte appended so far.
	 * @param done <jk>true</jk> if no more bytes will be appended.
	 */
	void nudge(String terminalId, long end, boolean done);
}
