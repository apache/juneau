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
package org.apache.juneau.utils;

import java.text.*;

import org.apache.juneau.marshaller.*;

/**
 * A utility for logging formatted messages to the console.
 * Uses the {@link Json5} marshaller for serializing objects so any
 * POJOs can be used as format arguments.
 */
public class Console {

	/**
	 * Prints a message with arguments to {@link System#out}.
	 *
	 * <p>
	 * Arguments are automatically converted to strings using the {@link Json5} marshaller.
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
	 * @param args The arguments sent to the the formatter after running them through the {@link Json5} marshaller.
	 */
	public static final void out(String msg, Object...args) {
		System.out.println(format(msg, args));
	}

	/**
	 * Prints a message with arguments to {@link System#err}.
	 *
	 * <p>
	 * Arguments are automatically converted to strings using the {@link Json5} marshaller.
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
	 * @param args The arguments sent to the the formatter after running them through the {@link Json5} marshaller.
	 */
	public static final void err(String msg, Object...args) {
		System.err.println(format(msg, args));  // NOT DEBUG
	}

	/**
	 * Formats a message with arguments.
	 *
	 * <p>
	 * Arguments are automatically converted to strings using the {@link Json5} marshaller.
	 *
	 * <p>
	 * Useful for debug messages.
	 *
	 * <h5 class='figure'>Example:</h5>
	 * <p class='bjava'>
	 * 	String <jv>msg</jv> = Console.<jsm>format</jsm>(<js>"myPojo={0}"</js>, <jv>myPojo</jv>);
	 * </p>
	 *
	 * @param msg The {@link MessageFormat}-styled message.
	 * @param args The arguments sent to the the formatter after running them through the {@link Json5} marshaller.
	 * @return This object.
	 */
	public static final String format(String msg, Object...args) {
		for (int i = 0; i < args.length; i++)
			args[i] = Json5.of(args[i]);
		return MessageFormat.format(msg, args);
	}
}
