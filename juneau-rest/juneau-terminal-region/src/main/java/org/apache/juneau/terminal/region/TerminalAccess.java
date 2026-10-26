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

import java.util.*;

/**
 * The app's single answer to "who may watch" and "who may type".
 *
 * <p>
 * As a lambda it answers the first question only: typing stays denied unless {@link #inputSink(TerminalCaller, String)}
 * is overridden.
 */
@FunctionalInterface
public interface TerminalAccess {

	/**
	 * Whether the caller may watch the terminal.
	 *
	 * <p>
	 * A typist must also be able to watch.
	 *
	 * @param caller Who is asking.
	 * @param terminalId The terminal id.
	 * @return <jk>true</jk> if the caller may watch it.
	 */
	boolean canWatch(TerminalCaller caller, String terminalId);

	/**
	 * The sink the caller may type into.
	 *
	 * <p>
	 * Input also requires {@link #canWatch(TerminalCaller, String)}: a sink returned for a caller who may not watch is
	 * still denied.
	 *
	 * @param caller Who is asking.
	 * @param terminalId The terminal id.
	 * @return The sink, or empty when typing is denied.  Empty by default.
	 */
	default Optional<TerminalSink> inputSink(TerminalCaller caller, String terminalId) {
		return Optional.empty();
	}
}
