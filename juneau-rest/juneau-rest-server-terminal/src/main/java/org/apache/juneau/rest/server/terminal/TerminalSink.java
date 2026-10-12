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

import java.io.*;

/**
 * Where a caller's keystrokes go: the input end of one running terminal job.
 */
public interface TerminalSink {

	/**
	 * Whether input is accepted right now.
	 *
	 * @return <jk>false</jk> once the process has ended or a write has failed.
	 */
	boolean acceptsInput();

	/**
	 * Hands the bytes to the process.
	 *
	 * <p>
	 * Must not block the caller (it runs on the WebSocket thread).  Never throws for a closed or broken pipe: that
	 * turns {@link #acceptsInput()} false instead.
	 *
	 * @param bytes The bytes to send.
	 * @return <jk>false</jk> when the bytes were not taken because the sink's pending-input buffer is full.
	 * @throws IOException On an unexpected I/O failure.
	 */
	boolean write(byte[] bytes) throws IOException;
}
