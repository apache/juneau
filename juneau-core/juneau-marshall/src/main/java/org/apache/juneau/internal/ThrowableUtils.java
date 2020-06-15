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

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;

/**
 * Various utility methods for creating and working with throwables.
 */
public class ThrowableUtils {

	/**
	 * Throws an {@link IllegalArgumentException} if the specified object is <jk>null</jk>.
	 *
	 * @param o The object to check.
	 * @param msg The message of the IllegalArgumentException.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static void assertNotNull(Object o, String msg, Object...args) throws IllegalArgumentException {
		if (o == null)
			throw new BasicIllegalArgumentException(msg, args);
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified field is <jk>null</jk>.
	 *
	 * @param fieldValue The object to check.
	 * @param fieldName The name of the field.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static void assertFieldNotNull(Object fieldValue, String fieldName) throws IllegalArgumentException {
		if (fieldValue == null)
			throw new IllegalArgumentException("Field '"+fieldName+"' cannot be null.");
	}

	/**
	 * Throws an {@link IllegalArgumentException} if the specified field is <c>&lt;=0</c>.
	 *
	 * @param fieldValue The object to check.
	 * @param fieldName The name of the field.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static void assertFieldPositive(int fieldValue, String fieldName) throws IllegalArgumentException {
		if (fieldValue <= 0)
			throw new IllegalArgumentException("Field '"+fieldName+"' must be a positive integer.");
	}

	/**
	 * Shortcut for calling <code><jk>new</jk> IllegalArgumentException(MessageFormat.<jsm>format</jsm>(msg, args));</code>
	 *
	 * @param msg The message of the IllegalArgumentException.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @throws IllegalArgumentException Constructed exception.
	 */
	public static void illegalArg(String msg, Object...args) throws IllegalArgumentException {
		throw new BasicIllegalArgumentException(msg, args);
	}

	/**
	 * Throws an exception if the specified thread ID is not the same as the current thread.
	 *
	 * @param threadId The ID of the thread to compare against.
	 * @param msg The message of the IllegalStateException.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @throws IllegalStateException Constructed exception.
	 */
	public static void assertSameThread(long threadId, String msg, Object...args) throws IllegalStateException {
		if (Thread.currentThread().getId() != threadId)
			throw new BasicIllegalArgumentException(msg, args);
	}

	/**
	 * Throws an assertion error if the specified throwable or throwable causes do not contain the specified message string.
	 *
	 * @param t The throwable to check.
	 * @param msg The message text to check for.
	 * @throws AssertionError Assertion failed.
	 */
	public static void assertExceptionContainsMessage(Throwable t, String msg) throws AssertionError {
		Throwable t2 = t;
		while (t2 != null) {
			if (t2.getMessage() != null && t2.getMessage().contains(msg))
				return;
			t2 = t2.getCause();
		}
		t.printStackTrace();
		throw new BasicAssertionError(t, "Throwable did not contain the expected message.  Message=[{0}]", msg);
	}

	/**
	 * Same as {@link Throwable#getCause()} but searches the throwable chain for an exception of the specified type.
	 *
	 * @param c The throwable type to search for.
	 * @param <T> The throwable type to search for.
	 * @param t The throwable to search.
	 * @return The exception, or <jk>null</jk> if not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> T getCause(Class<T> c, Throwable t) {
		while (t != null) {
			t = t.getCause();
			if (c.isInstance(t))
				return (T)t;
		}
		return null;
	}

	/**
	 * Given a list of available throwable types, attempts to create and throw the exception based on name.
	 *
	 * @param name The exception name.  Can be a simple name or fully-qualified.
	 * @param message The exception message passed to the exception constructor.
	 * @param throwables The list of available throwable classes to choose from.
	 * @throws Throwable The thrown exception if it was possible to create.
	 */
	public static void throwException(String name, String message, Class<?>...throwables) throws Throwable {
		if (name != null)
			for (Class<?> t : throwables)
				if (t.getName().endsWith(name))
					doThrow(t, message);
	}

	private static void doThrow(Class<?> t, String msg) throws Throwable {
		ConstructorInfo c = null;
		ClassInfo ci = ClassInfo.of(t);
		if (msg != null) {
			c = ci.getPublicConstructor(String.class);
			if (c != null)
				throw c.<Throwable>invoke(msg);
			c = ci.getPublicConstructor(Object.class);
			if (c != null)
				throw c.<Throwable>invoke(msg);
		}
		c = ci.getPublicConstructor();
		if (c != null)
			throw c.<Throwable>invoke();
	}
}
