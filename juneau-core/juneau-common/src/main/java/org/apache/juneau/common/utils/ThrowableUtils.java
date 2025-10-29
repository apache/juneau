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

import java.io.*;
import java.util.*;

/**
 * Various utility methods for creating and working with throwables.
 */
public class ThrowableUtils {

	/**
	 * Interface used with {@link Utils#safeSupplier(SupplierWithThrowable)}.
	 *
	 * @param <T> The supplier type.
	 */
	@FunctionalInterface
	public interface SupplierWithThrowable<T> {

		/**
		 * Gets a result.
		 *
		 * @return a result
		 * @throws Throwable if supplier threw an exception.
		 */
		T get() throws Throwable;
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException toRuntimeException(Throwable cause) {
		return castException(RuntimeException.class, cause);
	}

	/**
	 * Casts or wraps the specified throwable to the specified type.
	 *
	 * @param <T> The class to cast to.
	 * @param type The class to cast to.
	 * @param t The throwable to cast.
	 * @return Either the same exception if it's already the specified type, or a wrapped exception.
	 */
	public static <T> T castException(Class<T> type, Throwable t) {
		try {
			return type.isInstance(t) ? type.cast(t) : type.getConstructor(Throwable.class).newInstance(t);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Same as {@link Throwable#getCause()} but searches the throwable chain for an exception of the specified type.
	 *
	 * @param c The throwable type to search for.
	 * @param <T> The throwable type to search for.
	 * @param t The throwable to search.
	 * @return The exception, or <jk>null</jk> if not found.
	 */
	public static <T extends Throwable> T getThrowableCause(Class<T> c, Throwable t) {
		while (nn(t)) {
			t = t.getCause();
			if (c.isInstance(t))
				return c.cast(t);
		}
		return null;
	}

	/**
	 * Convenience method for getting a stack trace as a string.
	 *
	 * @param t The throwable to get the stack trace from.
	 * @return The same content that would normally be rendered via <c>t.printStackTrace()</c>
	 */
	public static String getStackTrace(Throwable t) {
		var sw = new StringWriter();
		try (var pw = new PrintWriter(sw)) {
			t.printStackTrace(pw);
		}
		return sw.toString();
	}

	/**
	 * Calculates a 16-bit hash for the specified throwable based on it's stack trace.
	 *
	 * @param t The throwable to calculate the stack trace on.
	 * @param stopClass Optional stop class on which to stop calculation of a stack trace beyond when found.
	 * @return A calculated hash.
	 */
	public static int hash(Throwable t, String stopClass) {
		var i = 0;
		while (nn(t)) {
			for (var e : t.getStackTrace()) {
				if (e.getClassName().equals(stopClass))
					break;
				if (e.getClassName().indexOf('$') == -1)
					i ^= e.hashCode();
			}
			t = t.getCause();
		}
		return i;
	}

	/**
	 * Creates an {@link IllegalArgumentException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IllegalArgumentException with the formatted message.
	 */
	public static IllegalArgumentException illegalArg(String msg, Object...args) {
		return new IllegalArgumentException(args.length == 0 ? msg : f(msg, args));
	}

	/**
	 * Creates an {@link IllegalArgumentException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IllegalArgumentException with the formatted message and cause.
	 */
	public static IllegalArgumentException illegalArg(Throwable cause, String msg, Object...args) {
		return new IllegalArgumentException(args.length == 0 ? msg : f(msg, args), cause);
	}

	/**
	 * Creates a {@link RuntimeException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message.
	 */
	public static RuntimeException runtimeException(String msg, Object...args) {
		return new RuntimeException(args.length == 0 ? msg : f(msg, args));
	}

	/**
	 * Creates a {@link RuntimeException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message and cause.
	 */
	public static RuntimeException runtimeException(Throwable cause, String msg, Object...args) {
		return new RuntimeException(args.length == 0 ? msg : f(msg, args), cause);
	}

	/**
	 * Creates an {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new UnsupportedOperationException with the formatted message.
	 */
	public static UnsupportedOperationException unsupportedOp(String msg, Object...args) {
		return new UnsupportedOperationException(args.length == 0 ? msg : f(msg, args));
	}

	/**
	 * Creates an {@link UnsupportedOperationException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new UnsupportedOperationException with the formatted message and cause.
	 */
	public static UnsupportedOperationException unsupportedOp(Throwable cause, String msg, Object...args) {
		return new UnsupportedOperationException(args.length == 0 ? msg : f(msg, args), cause);
	}

	/**
	 * Creates an {@link java.io.IOException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IOException with the formatted message.
	 */
	public static java.io.IOException ioException(String msg, Object...args) {
		return new java.io.IOException(args.length == 0 ? msg : f(msg, args));
	}

	/**
	 * Creates an {@link java.io.IOException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IOException with the formatted message and cause.
	 */
	public static java.io.IOException ioException(Throwable cause, String msg, Object...args) {
		return new java.io.IOException(args.length == 0 ? msg : f(msg, args), cause);
	}

	/**
	 * Searches through the cause chain of an exception to find an exception of the specified type.
	 *
	 * @param <T> The cause type.
	 * @param e The exception to search.
	 * @param cause The cause type to search for.
	 * @return An {@link Optional} containing the cause if found, or empty if not found.
	 */
	public static <T extends Throwable> Optional<T> findCause(Throwable e, Class<T> cause) {
		while (nn(e)) {
			if (cause.isInstance(e))
				return Optional.of(cause.cast(e));
			e = e.getCause();
		}
		return Optional.empty();
	}
}