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
package org.apache.juneau.assertions;

import static org.apache.juneau.internal.StringUtils.*;

import java.util.function.*;

/**
 * Wrapper around a {@link Predicate} that allows for an error message for when the predicate fails.
 *
 * @param <T> the type of input being tested.
 */
public class AssertionPredicate<T> implements Predicate<T> {

	final Predicate<T> inner;
	final String message;
	final ThreadLocal<String> failedMessage = new ThreadLocal<>();

	/**
	 * Constructor.
	 *
	 * @param inner The predicate test.
	 * @param message
	 * 	The error message if predicate fails.
	 * 	<br>Can contain <c>{VALUE}</c> variable.
	 * @param args Optional message arguments.
	 */
	public AssertionPredicate(Predicate<T> inner, String message, Object...args) {
		this.inner = inner;
		this.message = format(message, args);
	}

	AssertionPredicate() {
		this.inner = null;
		this.message = null;
	}

	@Override /* Predicate */
	public boolean test(T t) {
		failedMessage.remove();
		boolean b = inner.test(t);
		if (! b) {
			String m = message.replace("{VALUE}", stringify(t));
			if (inner instanceof AssertionPredicate)
				m += "\n\t" + ((AssertionPredicate<?>)inner).getFailureMessage();
			failedMessage.set(m);
		}
		return inner.test(t);
	}

	/**
	 * Returns the error message from the last call to this assertion.
	 *
	 * @return The error message, or <jk>null</jk> if there was no failure.
	 */
	public String getFailureMessage() {
		return failedMessage.get();
	}

	/**
	 * Encapsulates multiple predicates into a single AND operation.
	 *
	 * <p>
	 * Similar to <c><jsm>stream</jsm>(<jv>predicates<jv>).reduce(<jv>x</jv>-><jk>true</jk>, Predicate::and)</c> but
	 * provides for {@link #getFailureMessage()} to return a useful message.
	 *
	 * @param <T> the type of input being tested.
	 */
	public static class And<T> extends AssertionPredicate<T> {

		private final Predicate<T>[] inner;

		/**
		 * Constructor.
		 *
		 * @param inner The inner predicates to run.
		 */
		@SafeVarargs
		public And(Predicate<T>...inner) {
			this.inner = inner;
		}

		@Override /* Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			for (int i = 0; i < inner.length; i++) {
				Predicate<T> p = inner[i];
				if (p != null) {
					boolean b = p.test(t);
					if (! b) {
						String m = format("Predicate test #{0} failed.", i);
						if (p instanceof AssertionPredicate)
							m += "\n\t" + ((AssertionPredicate<?>)p).getFailureMessage();
						failedMessage.set(m);
						return false;
					}
				}
			}
			return true;
		}
	}

	/**
	 * Encapsulates multiple predicates into a single OR operation.
	 *
	 * <p>
	 * Similar to <c><jsm>stream</jsm>(<jv>predicates<jv>).reduce(<jv>x</jv>-><jk>true</jk>, Predicate::or)</c> but
	 * provides for {@link #getFailureMessage()} to return a useful message.
	 *
	 * @param <T> the type of input being tested.
	 */
	public static class Or<T> extends AssertionPredicate<T> {

		private final Predicate<T>[] inner;

		/**
		 * Constructor.
		 *
		 * @param inner The inner predicates to run.
		 */
		@SafeVarargs
		public Or(Predicate<T>...inner) {
			this.inner = inner;
		}

		@Override /* Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			for (Predicate<T> p : inner)
				if (p != null)
					if (p.test(t))
						return true;
			String m = format("No predicate tests passed.");
			failedMessage.set(m);
			return false;
		}
	}

	/**
	 * Negates an assertion.
	 *
	 * <p>
	 * Similar to <c><jv>predicate</jv>.negate()</c> but provides for {@link #getFailureMessage()} to return a useful message.
	 *
	 * @param <T> the type of input being tested.
	 */
	public static class Not<T> extends AssertionPredicate<T> {

		private final Predicate<T> inner;

		/**
		 * Constructor.
		 *
		 * @param inner The inner predicates to run.
		 */
		public Not(Predicate<T> inner) {
			this.inner = inner;
		}

		@Override /* Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			Predicate<T> p = inner;
			if (p != null) {
				boolean b = p.test(t);
				if (b) {
					String m = format("Predicate test unexpectedly passed.");
					if (p instanceof AssertionPredicate)
						m += "\n\t" + ((AssertionPredicate<?>)p).getFailureMessage();
					failedMessage.set(m);
					return false;
				}
			}
			return true;
		}
	}

}
