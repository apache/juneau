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
package org.apache.juneau.commons.utils;

import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.commons.reflect.*;

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
	 * Shortcut for creating a {@link BeanRuntimeException} with a message and associated class.
	 *
	 * @param c The class associated with the exception.
	 * @param msg The message.
	 * @param args Optional {@link String#format(String, Object...)} arguments.
	 * @return A new {@link BeanRuntimeException}.
	 */
	public static BeanRuntimeException bex(Class<?> c, String msg, Object...args) {
		return new BeanRuntimeException(c, msg, args);
	}

	/**
	 * Creates a {@link RuntimeException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message.
	 */
	public static BeanRuntimeException bex(String msg, Object...args) {
		return new BeanRuntimeException(msg, args);
	}

	/**
	 * Creates a {@link RuntimeException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new RuntimeException wrapping the given cause.
	 */
	public static BeanRuntimeException bex(Throwable cause) {
		return new BeanRuntimeException(cause);
	}

	/**
	 * Shortcut for creating a {@link BeanRuntimeException} with a cause, message, and associated class.
	 *
	 * @param e The cause of the exception.
	 * @param c The class associated with the exception.
	 * @param msg The message.
	 * @param args Optional {@link String#format(String, Object...)} arguments.
	 * @return A new {@link BeanRuntimeException}.
	 */
	public static BeanRuntimeException bex(Throwable e, Class<?> c, String msg, Object...args) {
		return new BeanRuntimeException(e, c, msg, args);
	}

	/**
	 * Creates a {@link RuntimeException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message and cause.
	 */
	public static BeanRuntimeException bex(Throwable cause, String msg, Object...args) {
		return new BeanRuntimeException(f(msg, args), cause);
	}

	/**
	 * Shortcut for creating an {@link ExecutableException} with a message.
	 *
	 * @param msg The message.
	 * @param args Optional {@link String#format(String, Object...)} arguments.
	 * @return A new {@link ExecutableException}.
	 */
	public static ExecutableException exex(String msg, Object...args) {
		return new ExecutableException(msg, args);
	}

	/**
	 * Creates an {@link ExecutableException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new ExecutableException wrapping the given cause.
	 */
	public static ExecutableException exex(Throwable cause) {
		return new ExecutableException(cause);
	}

	/**
	 * Creates an {@link ExecutableException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new ExecutableException with the formatted message and cause.
	 */
	public static ExecutableException exex(Throwable cause, String msg, Object...args) {
		return new ExecutableException(cause, msg, args);
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
		return safeSupplier(() -> {
			return type.isInstance(t) ? type.cast(t) : type.getConstructor(Throwable.class).newInstance(t);
		});
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
				return opt(cause.cast(e));
			e = e.getCause();
		}
		return opte();
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
	 * Prints a stack trace with a maximum depth limit.
	 *
	 * <p>
	 * This method is useful for {@link StackOverflowError} situations where printing the full stack trace
	 * can cause additional errors due to stack exhaustion. The stack trace will be limited to the specified
	 * maximum number of elements.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> {
	 * 		<jc>// Some code that might cause StackOverflowError</jc>
	 * 	} <jk>catch</jk> (StackOverflowError <jv>e</jv>) {
	 * 		<jc>// Print only first 100 stack trace elements</jc>
	 * 		ThrowableUtils.<jsm>printStackTrace</jsm>(<jv>e</jv>, System.err, 100);
	 * 	}
	 * </p>
	 *
	 * @param t The throwable to print the stack trace for.
	 * @param pw The print writer to write to.
	 * @param maxDepth The maximum number of stack trace elements to print. If <jk>null</jk> or negative, prints all elements.
	 */
	public static void printStackTrace(Throwable t, PrintWriter pw, Integer maxDepth) {
		try {
			pw.println(t);
			var stackTrace = t.getStackTrace();
			var depth = maxDepth != null && maxDepth > 0 ? Math.min(maxDepth, stackTrace.length) : stackTrace.length;
			for (var i = 0; i < depth; i++) {
				pw.println("\tat " + stackTrace[i]);
			}
			if (maxDepth != null && maxDepth > 0 && stackTrace.length > maxDepth) {
				pw.println("\t... (" + (stackTrace.length - maxDepth) + " more)");
			}
			var cause = t.getCause();
			if (cause != null && cause != t) {
				pw.print("Caused by: ");
				printStackTrace(cause, pw, maxDepth);
			}
		} catch (Exception e) {
			pw.println("Error printing stack trace: " + e.getMessage());
		}
	}

	/**
	 * Prints a stack trace with a maximum depth limit to standard error.
	 *
	 * <p>
	 * Same as {@link #printStackTrace(Throwable, PrintWriter, Integer)} but writes to {@link System#err}.
	 *
	 * @param t The throwable to print the stack trace for.
	 * @param maxDepth The maximum number of stack trace elements to print. If <jk>null</jk> or negative, prints all elements.
	 */
	public static void printStackTrace(Throwable t, Integer maxDepth) {
		printStackTrace(t, new PrintWriter(System.err, true), maxDepth);
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
		return new IllegalArgumentException(f(msg, args));
	}

	/**
	 * Creates an {@link IllegalArgumentException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new IllegalArgumentException wrapping the given cause.
	 */
	public static IllegalArgumentException illegalArg(Throwable cause) {
		return new IllegalArgumentException(cause);
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
		return new IllegalArgumentException(f(msg, args), cause);
	}

	/**
	 * Creates an {@link java.io.IOException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IOException with the formatted message.
	 */
	public static java.io.IOException ioex(String msg, Object...args) {
		return new java.io.IOException(f(msg, args));
	}

	/**
	 * Creates an {@link java.io.IOException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new IOException wrapping the given cause.
	 */
	public static java.io.IOException ioex(Throwable cause) {
		return new java.io.IOException(cause);
	}

	/**
	 * Creates an {@link java.io.IOException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new IOException with the formatted message and cause.
	 */
	public static java.io.IOException ioex(Throwable cause, String msg, Object...args) {
		return new java.io.IOException(f(msg, args), cause);
	}

	/**
	 * Convenience method for calling {@link Throwable#getLocalizedMessage()}.
	 *
	 * @param t The throwable.
	 * @return The localized message of the throwable.
	 */
	public static String lm(Throwable t) {
		return t.getLocalizedMessage();
	}

	/**
	 * Creates a {@link RuntimeException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message.
	 */
	public static RuntimeException rex(String msg, Object...args) {
		return new RuntimeException(f(msg, args));
	}

	/**
	 * Creates a {@link RuntimeException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new RuntimeException wrapping the given cause.
	 */
	public static RuntimeException rex(Throwable cause) {
		return new RuntimeException(cause);
	}

	/**
	 * Creates a {@link RuntimeException} with a cause.
	 *
	 * @param cause The cause of the exception.
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new RuntimeException with the formatted message and cause.
	 */
	public static RuntimeException rex(Throwable cause, String msg, Object...args) {
		return new RuntimeException(f(msg, args), cause);
	}

	/**
	 * Creates a new {@link RuntimeException}.
	 *
	 * @param cause The caused-by exception.
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException toRex(Throwable cause) {
		return castException(RuntimeException.class, cause);
	}

	/**
	 * Creates an {@link UnsupportedOperationException} with a default message.
	 *
	 * @return A new UnsupportedOperationException with the message "Not supported."
	 */
	public static UnsupportedOperationException unsupportedOp() {
		return new UnsupportedOperationException("Not supported.");
	}

	/**
	 * Creates an {@link UnsupportedOperationException}.
	 *
	 * @param msg The exception message.
	 * @param args The arguments to substitute into the message.
	 * @return A new UnsupportedOperationException with the formatted message.
	 */
	public static UnsupportedOperationException unsupportedOp(String msg, Object...args) {
		return new UnsupportedOperationException(f(msg, args));
	}

	/**
	 * Creates an {@link UnsupportedOperationException} wrapping the given throwable.
	 *
	 * @param cause The cause of the exception.
	 * @return A new UnsupportedOperationException wrapping the given cause.
	 */
	public static UnsupportedOperationException unsupportedOp(Throwable cause) {
		return new UnsupportedOperationException(cause);
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
		return new UnsupportedOperationException(f(msg, args), cause);
	}

	/**
	 * Creates an {@link UnsupportedOperationException} for read-only objects.
	 *
	 * @return A new UnsupportedOperationException with the message "Object is read only."
	 */
	public static UnsupportedOperationException unsupportedOpReadOnly() {
		return new UnsupportedOperationException("Object is read only.");
	}
}