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
package org.apache.juneau.internal;

import java.text.*;

/**
 * Various utility methods for creating and working with throwables.
 */
public class ThrowableUtils {

	/**
	 * Throws an {@link IllegalArgumentException} if the specified object is <jk>null</jk>.
	 *
	 * @param o The object to check.
	 * @param msg The message of the IllegalArgumentException.
	 * @param args {@link MessageFormat}-style arguments in the message.
	 * @throws IllegalArgumentException
	 */
	public static void assertNotNull(Object o, String msg, Object...args) throws IllegalArgumentException {
		if (o == null)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified field is <jk>null</jk>.
	 *
	 * @param fieldValue The object to check.
	 * @param fieldName The name of the field.
	 * @throws IllegalArgumentException
	 */
	public static void assertFieldNotNull(Object fieldValue, String fieldName) throws IllegalArgumentException {
		if (fieldValue == null)
			throw new IllegalArgumentException("Field '" + fieldName + "' cannot be null.");
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified field is <code>&lt;=0</code>.
	 *
	 * @param fieldValue The object to check.
	 * @param fieldName The name of the field.
	 * @throws IllegalArgumentException
	 */
	public static void assertFieldPositive(int fieldValue, String fieldName) throws IllegalArgumentException {
		if (fieldValue <= 0)
			throw new IllegalArgumentException("Field '" + fieldName + "' must be a positive integer.");
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> IllegalArgumentException(MessageFormat.<jsm>format</jsm>(msg, args));</code>
	 *
	 * @param msg The message of the IllegalArgumentException.
	 * @param args {@link MessageFormat}-style arguments in the message.
	 * @throws IllegalArgumentException
	 */
	public static void illegalArg(String msg, Object...args) throws IllegalArgumentException {
		throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}

	/**
	 * Throws an exception if the specified thread ID is not the same as the current thread.
	 *
	 * @param threadId The ID of the thread to compare against.
	 * @param msg The message of the IllegalStateException.
	 * @param args {@link IllegalStateException}-style arguments in the message.
	 * @throws IllegalStateException
	 */
	public static void assertSameThread(long threadId, String msg, Object...args) throws IllegalStateException {
		if (Thread.currentThread().getId() != threadId)
			throw new IllegalArgumentException(MessageFormat.format(msg, args));
	}
}
