/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.utils;

import java.text.*;

/**
 * Various utility methods for creating and working with throwables.
 *
 * @author James Bognar (jbognar@us.ibm.com)
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
