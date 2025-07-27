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
package org.apache.juneau.common.internal;

import java.io.*;

import org.apache.juneau.common.utils.*;

/**
 * Various utility methods for creating and working with throwables.
 */
public class ThrowableUtils {

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException asRuntimeException(Throwable cause) {
		return cast(RuntimeException.class, cause);
	}

	/**
	 * Casts or wraps the specified throwable to the specified type.
	 *
	 * @param <T> The class to cast to.
	 * @param type The class to cast to.
	 * @param t The throwable to cast.
	 * @return Either the same exception if it's already the specified type, or a wrapped exception.
	 */
	public static <T> T cast(Class<T> type, Throwable t) {
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
	public static <T extends Throwable> T getCause(Class<T> c, Throwable t) {
		while (t != null) {
			t = t.getCause();
			if (c.isInstance(t))
				return c.cast(t);
		}
		return null;
	}

	/**
	 * Allows you to wrap a supplier that throws an exception so that it can be used in a fluent interface.
	 *
	 * @param <T> The supplier type.
	 * @param supplier The supplier throwing an exception.
	 * @return The supplied result.
	 * @throws RuntimeException if supplier threw an exception.
	 */
	public static <T> T safeSupplier(SupplierWithThrowable<T> supplier) {
		try {
			return supplier.get();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw asRuntimeException(t);
		}
	}

	/**
	 * Interface used with {@link #safeSupplier(SupplierWithThrowable)}.
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
	 * Runs a snippet of code and encapsulates any throwable inside a {@link RuntimeException}.
	 *
	 * @param snippet The snippet of code to run.
	 */
	public static void safeRun(Snippet snippet) {
		try {
			snippet.run();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw asRuntimeException(t);
		}
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
		while (t != null) {
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
}
