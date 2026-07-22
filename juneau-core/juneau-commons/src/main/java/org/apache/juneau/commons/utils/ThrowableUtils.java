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


import static org.apache.juneau.commons.utils.Exceptions.*;
import static org.apache.juneau.commons.utils.ObjectUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.commons.settings.*;

/**
 * Various utility methods for working with throwables.
 *
 * <p>
 * Exception <i>factory</i> methods (terse {@code rex}/{@code brex}/{@code iaex}/{@code isex}/{@code uoex}/
 * {@code uoroex}/{@code ioex}/{@code exex} constructors) live on {@link Shorts}
 * and construct their exceptions directly with no side-effects.  This class retains only throwable
 * <i>inspection</i>/<i>handling</i> helpers (stack-trace rendering, cause walking, safe-run wrappers, casting).
 *
 * <p>
 * The opt-in {@link #log(Throwable)} helper checks the {@code juneau.enableVerboseExceptions} setting and, when
 * enabled, prints the throwable's stack trace to the logger.  It is <b>not</b> invoked automatically by any
 * factory — callers that want verbose logging at a specific site must call it explicitly.
 *
 * <h5 class='section'>Example — enabling for a single test thread:</h5>
 * <p class='bjava'>
 * 	Settings.<jsm>get</jsm>().setLocal(<js>"juneau.enableVerboseExceptions"</js>, <js>"true"</js>);
 * 	<jk>try</jk> {
 * 		<jc>// ... code under test ...</jc>
 * 	} <jk>finally</jk> {
 * 		Settings.<jsm>get</jsm>().clearLocal();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauCommonsUtils">juneau-commons - Utils Package</a>
 * </ul>
 */
public class ThrowableUtils {

	private static final Logger LOG = Logger.getLogger(ThrowableUtils.class);

	static final Setting<Boolean> VERBOSE = Settings.get().get("juneau.enableVerboseExceptions").asBoolean();

	/**
	 * Interface used with {@link #safeSupplier(SupplierWithThrowable)}.
	 *
	 * @param <T> The supplier type.
	 */
	@FunctionalInterface
	@SuppressWarnings({
		"java:S112" // throws Throwable intentional - supplier may throw any exception
	})
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
	 * Casts or wraps the specified throwable to the specified type.
	 *
	 * @param <T> The class to cast to.
	 * @param type The class to cast to.  Must not be <jk>null</jk>.
	 * @param t The throwable to cast.  Can be <jk>null</jk> (produces a wrapper with a <jk>null</jk> cause).
	 * @return Either the same exception if it's already the specified type, or a wrapped exception.
	 */
	public static <T> T castException(Class<T> type, Throwable t) {
		return safeSupplier(() -> type.isInstance(t) ? type.cast(t) : type.getConstructor(Throwable.class).newInstance(t));
	}

	/**
	 * Searches through the cause chain of an exception to find an exception of the specified type.
	 *
	 * @param <T> The cause type.
	 * @param e The exception to search.  Can be <jk>null</jk> (returns an empty {@link Optional}).
	 * @param cause The cause type to search for.  Must not be <jk>null</jk>.
	 * @return An {@link Optional} containing the cause if found, or empty if not found.
	 */
	public static <T extends Throwable> Optional<T> findCause(Throwable e, Class<T> cause) {
		while (isNotNull(e)) {
			if (cause.isInstance(e))
				return optional(cause.cast(e));
			e = e.getCause();
		}
		return emptyOptional();
	}

	/**
	 * Convenience method for getting a stack trace as a string.
	 *
	 * @param t The throwable to get the stack trace from.  Must not be <jk>null</jk>.
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
	 * @param t The throwable to print the stack trace for.  Must not be <jk>null</jk>.
	 * @param pw The print writer to write to.  Must not be <jk>null</jk>.
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
	 * This method is useful for {@link StackOverflowError} situations where printing the full stack trace
	 * can cause additional errors due to stack exhaustion. The stack trace will be limited to the specified
	 * maximum number of elements.
	 *
	 * <p>
	 * Same as {@link #printStackTrace(Throwable, PrintWriter, Integer)} but writes to {@link System#err}.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> {
	 * 		<jc>// Some code that might cause StackOverflowError</jc>
	 * 	} <jk>catch</jk> (StackOverflowError <jv>e</jv>) {
	 * 		<jc>// Print only first 100 stack trace elements to stderr</jc>
	 * 		ThrowableUtils.<jsm>printStackTrace</jsm>(<jv>e</jv>, 100);
	 * 	}
	 * </p>
	 *
	 * @param t The throwable to print the stack trace for.
	 * @param maxDepth The maximum number of stack trace elements to print. If <jk>null</jk> or negative, prints all elements.
	 */
	public static void printStackTrace(Throwable t, Integer maxDepth) {
		var sw = new StringWriter();
		try (var pw = new PrintWriter(sw)) {
			printStackTrace(t, pw, maxDepth);
		}
		LOG.warning(t, sw.toString());
	}

	/**
	 * Same as {@link Throwable#getCause()} but searches the throwable chain for an exception of the specified type.
	 *
	 * @param c The throwable type to search for.  Must not be <jk>null</jk>.
	 * @param <T> The throwable type to search for.
	 * @param t The throwable to search.  Can be <jk>null</jk> (returns <jk>null</jk>).
	 * @return The exception, or <jk>null</jk> if not found.
	 */
	public static <T extends Throwable> T getThrowableCause(Class<T> c, Throwable t) {
		while (isNotNull(t)) {
			t = t.getCause();
			if (c.isInstance(t))
				return c.cast(t);
		}
		return null;
	}

	/**
	 * Calculates a 16-bit hash for the specified throwable based on it's stack trace.
	 *
	 * @param t The throwable to calculate the stack trace on.  Can be <jk>null</jk> (returns <c>0</c>).
	 * @param stopClass Optional stop class on which to stop calculation of a stack trace beyond when found.  Can be <jk>null</jk> (the entire stack trace, including causes, is used).
	 * @return A calculated hash.
	 */
	public static int hash(Throwable t, String stopClass) {
		var i = 0;
		while (isNotNull(t)) {
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
	 * Opt-in verbose logger for a throwable.
	 *
	 * <p>
	 * When the {@code juneau.enableVerboseExceptions} setting is {@code true}, logs the throwable's stack trace
	 * as a warning; otherwise does nothing.  Returns the same throwable to allow inline use (e.g.
	 * {@code throw log(iaex("..."))}).  This is never invoked automatically — call it explicitly at sites where
	 * verbose logging is desired.
	 *
	 * @param <T> The throwable type.
	 * @param exception The throwable to (optionally) log.  Must not be <jk>null</jk> when verbose exceptions are enabled; otherwise returned unchanged (may be <jk>null</jk>).
	 * @return The same throwable.
	 */
	public static <T extends Throwable> T log(T exception) {
		if (isTrue(VERBOSE.orElse(false))) LOG.warning(exception, getStackTrace(exception));
		return exception;
	}

	/**
	 * Convenience method for calling {@link Throwable#getLocalizedMessage()}.
	 *
	 * <p>
	 * Long messages (>2000 characters) are truncated to prevent stack overflow issues
	 * when exception messages contain circular references. The format is:
	 * "first-1000-chars&lt;truncated-#-chars&gt;last-1000-chars"
	 *
	 * @param t The throwable.  Must not be <jk>null</jk>.
	 * @return The localized message of the throwable, truncated if necessary, or <jk>null</jk> if the throwable has no localized message.
	 */
	public static String localizedMessage(Throwable t) {
		String msg = t.getLocalizedMessage();
		if (msg == null)
			return null;
		if (msg.length() <= 2000)
			return msg;
		int truncated = msg.length() - 2000;
		return msg.substring(0, 1000) + "<truncated-" + truncated + "-chars>" + msg.substring(msg.length() - 1000);
	}

	/** Wraps or casts to RuntimeException.  <br>A <jk>null</jk> argument produces a {@link RuntimeException} with a <jk>null</jk> cause. */
	public static RuntimeException toRuntimeException(Throwable cause) {
		return castException(RuntimeException.class, cause);
	}

	/**
	 * Creates a new {@link RuntimeException} from the specified cause (or returns it if already one).
	 *
	 * @param cause The caused-by exception.  Can be <jk>null</jk> (produces a {@link RuntimeException} with a <jk>null</jk> cause).
	 * @return A new {@link RuntimeException}, or the same exception if it's already of that type.
	 */
	public static RuntimeException toRex(Throwable cause) {
		return toRuntimeException(cause);
	}

	/** Runs a snippet; wraps any checked throwable in {@link RuntimeException}. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static void safe(Snippet snippet) {
		try {
			snippet.run();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw toRuntimeException(t);
		}
	}

	/** Runs a snippet; maps any checked throwable via the provided function. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static void safe(Snippet snippet, Function<Throwable, RuntimeException> exceptionMapper) {
		try {
			snippet.run();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw exceptionMapper.apply(t);
		}
	}

	/** Executes a supplier; wraps any checked exception in {@link RuntimeException}. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static <T> T safe(ThrowingSupplier<T> s) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception | Error e) {
			throw rex(e);
		}
	}

	/** Executes a supplier; maps any checked exception via the provided function. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static <T> T safe(ThrowingSupplier<T> s, Function<Throwable, RuntimeException> exceptionMapper) {
		try {
			return s.get();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception | Error e) {
			throw exceptionMapper.apply(e);
		}
	}

	/** Runs a snippet, silently ignoring any exception. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static void runQuietly(Snippet snippet) {
		try {
			snippet.run();
		} catch (@SuppressWarnings("unused") Throwable t) { /* Ignore */ }
	}

	/** Executes a supplier; returns the result or a fallback value produced by the exception function. */
	public static <T> T safeCatch(ThrowingSupplier<T> s, Function<Throwable,T> exceptionFunction) {
		try {
			return s.get();
		} catch (Exception e) {
			return exceptionFunction.apply(e);
		}
	}

	/** Executes a supplier; returns an {@link Optional} of the result, or empty if an exception is thrown. */
	public static <T> Optional<T> safeOpt(ThrowingSupplier<T> s) {
		try {
			return Optional.ofNullable(s.get());
		} catch (@SuppressWarnings("unused") Exception e) {
			return Optional.empty();
		}
	}

	/** Executes a supplier; returns an {@link Optional} of the result or the exception function's value. */
	public static <T> Optional<T> safeOptCatch(ThrowingSupplier<T> s, Function<Throwable,T> exceptionFunction) {
		try {
			return Optional.ofNullable(s.get());
		} catch (Exception e) {
			return Optional.ofNullable(exceptionFunction.apply(e));
		}
	}

	/** Executes a supplier; returns the result or {@code null} if an exception is thrown. */
	public static <T> T safeOrNull(ThrowingSupplier<T> s) {
		return safeOpt(s).orElse(null);
	}

	/** Executes a {@link SupplierWithThrowable}; wraps any checked throwable in {@link RuntimeException}. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static <T> T safeSupplier(SupplierWithThrowable<T> supplier) {
		try {
			return supplier.get();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw toRuntimeException(t);
		}
	}

	/** Executes a {@link SupplierWithThrowable}; maps any checked throwable via the provided function. */
	@SuppressWarnings({
		"java:S1181" // Need to catch Throwable to handle all exception types including Error
	})
	public static <T> T safeSupplier(SupplierWithThrowable<T> supplier, Function<Throwable, RuntimeException> exceptionMapper) {
		try {
			return supplier.get();
		} catch (RuntimeException t) {
			throw t;
		} catch (Throwable t) {
			throw exceptionMapper.apply(t);
		}
	}
}