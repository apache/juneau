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
package org.apache.juneau.terminal.region;

/**
 * Receives a script's prompt lines from a {@link TerminalProcess} started with a prompt channel.
 *
 * <p>
 * Both methods are called on the process's prompt reader thread, never concurrently.  The answers go back through
 * {@link TerminalProcess#answer(String)}.
 */
public interface TerminalPrompts {

	/**
	 * One prompt-protocol JSON line from the script, without its newline.
	 *
	 * @param jsonLine The line.
	 */
	void onLine(String jsonLine);

	/** The runner's output ended (the job finished or the process was closed).  Called once. */
	void onEnd();
}
