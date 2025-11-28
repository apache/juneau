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
package org.apache.juneau.common.utils;

import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;

/**
 * A utility for logging formatted messages to the console.
 *
 * <p>
 * Uses {@link java.text.MessageFormat} for formatting messages with arguments.
 */
public class Console {

	/**
	 * Prints a message with arguments to {@link System#err}.
	 *
	 * <p>
	 * Arguments are formatted using {@link java.text.MessageFormat}.
	 *
	 * <p>
	 * Useful for debug messages.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	Console.<jsm>err</jsm>(<js>"myPojo={0}"</js>, <jv>myPojo</jv>);
	 * </p>
	 *
	 * @param msg The {@link MessageFormat}-styled message.
	 * @param args The arguments sent to the formatter.
	 */
	public static final void err(String msg, Object...args) {
		System.err.println(f(msg, args));  // NOT DEBUG
	}

	/**
	 * Prints a message with arguments to {@link System#out}.
	 *
	 * <p>
	 * Arguments are formatted using {@link java.text.MessageFormat}.
	 *
	 * <p>
	 * Useful for debug messages.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	Console.<jsm>out</jsm>(<js>"myPojo={0}"</js>, <jv>myPojo</jv>);
	 * </p>
	 *
	 * @param msg The {@link MessageFormat}-styled message.
	 * @param args The arguments sent to the formatter.
	 */
	public static final void out(String msg, Object...args) {
		System.out.println(f(msg, args));
	}
}