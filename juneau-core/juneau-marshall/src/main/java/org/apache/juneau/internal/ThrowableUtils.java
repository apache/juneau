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

import java.io.*;

import org.apache.juneau.assertions.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;

/**
 * Various utility methods for creating and working with throwables.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class ThrowableUtils {

	/**
	 * Creates a new builder for {@link RuntimeException} objects.
	 *
	 * @return A new builder for {@link RuntimeException} objects.
	 */
	public static ExceptionBuilder<RuntimeException> runtimeException() {
		return new ExceptionBuilder<>(RuntimeException.class);
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(String msg, Object...args) {
		return runtimeException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link RuntimeException}.
	 */
	public static RuntimeException runtimeException(Throwable cause, String msg, Object...args) {
		return runtimeException().message(msg, args).causedBy(cause).build();
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException runtimeException(Throwable cause) {
		return cause instanceof RuntimeException ? (RuntimeException)cause : runtimeException().causedBy(cause).build();
	}

	/**
	 * Creates a new builder for {@link IOException} objects.
	 *
	 * @return A new builder for {@link IOException} objects.
	 */
	public static ExceptionBuilder<IOException> ioException() {
		return new ExceptionBuilder<>(IOException.class);
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link IOException}.
	 */
	public static IOException ioException(String msg, Object...args) {
		return ioException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param cause The caused-by exception.
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link IOException}.
	 */
	public static IOException ioException(Throwable cause, String msg, Object...args) {
		return ioException().message(msg, args).causedBy(cause).build();
	}

	/**
	 * Creates a new {@link IOException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link IOException}, or the same exception if it's already of that type.
	 */
	public static IOException ioException(Throwable cause) {
		return cause instanceof IOException ? (IOException)cause : ioException().causedBy(cause).build();
	}

	/**
	 * Creates a new builder for {@link IOException} objects.
	 *
	 * @return A new builder for {@link IOException} objects.
	 */
	public static ExceptionBuilder<UnsupportedOperationException> unsupportedOperationException() {
		return new ExceptionBuilder<>(UnsupportedOperationException.class);
	}

	/**
	 * Creates a new {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link UnsupportedOperationException}.
	 */
	public static UnsupportedOperationException unsupportedOperationException(String msg, Object...args) {
		return unsupportedOperationException().message(msg, args).build();
	}

	/**
	 * Creates a new builder for {@link IllegalArgumentException} objects.
	 *
	 * @return A new builder for {@link IllegalArgumentException} objects.
	 */
	public static ExceptionBuilder<IllegalArgumentException> illegalArgumentException() {
		return new ExceptionBuilder<>(IllegalArgumentException.class);
	}

	/**
	 * Creates a new {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link UnsupportedOperationException}.
	 */
	public static IllegalArgumentException illegalArgumentException(String msg, Object...args) {
		return illegalArgumentException().message(msg, args).build();
	}

	/**
	 * Creates a new builder for {@link ParseException} objects.
	 *
	 * @return A new builder for {@link ParseException} objects.
	 */
	public static ExceptionBuilder<SerializeException> serializeException() {
		return new ExceptionBuilder<>(SerializeException.class);
	}

	/**
	 * Creates a new {@link ParseException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link ParseException}.
	 */
	public static SerializeException serializeException(String msg, Object...args) {
		return serializeException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link SerializeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link SerializeException}, or the same exception if it's already of that type.
	 */
	public static SerializeException serializeException(Throwable cause) {
		return cause instanceof SerializeException ? (SerializeException)cause : serializeException().causedBy(cause).build();
	}

	/**
	 * Creates a new builder for {@link ParseException} objects.
	 *
	 * @return A new builder for {@link ParseException} objects.
	 */
	public static ExceptionBuilder<ParseException> parseException() {
		return new ExceptionBuilder<>(ParseException.class);
	}

	/**
	 * Creates a new {@link ParseException}.
	 *
	 * @param msg The exception message.  Can be <jk>null</jk>.
	 * 	<br>If <jk>null</jk>, then the caused-by message is used if available.
	 * @param args The exception message arguments.
	 * @return A new {@link ParseException}.
	 */
	public static ParseException parseException(String msg, Object...args) {
		return parseException().message(msg, args).build();
	}

	/**
	 * Creates a new {@link ParseException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link ParseException}, or the same exception if it's already of that type.
	 */
	public static ParseException parseException(Throwable cause) {
		return cause instanceof ParseException ? (ParseException)cause : parseException().causedBy(cause).build();
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
			throw runtimeException(t);
		}
	}

	/**
	 * Interface used with {@link #safeSupplier(SupplierWithThrowable)}.
	 */
	@SuppressWarnings("javadoc")
	@FunctionalInterface
	public interface SupplierWithThrowable<T> {
		public T get() throws Throwable;
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
			throw runtimeException(t);
		}
	}

	/**
	 * Convenience method for getting a stack trace as a string.
	 *
	 * @param t The throwable to get the stack trace from.
	 * @return The same content that would normally be rendered via <c>t.printStackTrace()</c>
	 */
	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			t.printStackTrace(pw);
		}
		return sw.toString();
	}
}
