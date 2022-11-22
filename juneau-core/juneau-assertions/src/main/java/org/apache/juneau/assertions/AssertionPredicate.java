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

import static org.apache.juneau.common.internal.StringUtils.*;

import java.text.*;
import java.util.function.*;

import org.apache.juneau.cp.*;

/**
 * Wrapper around a {@link Predicate} that allows for an error message for when the predicate fails.
 *
 * <p>
 * Typically used wherever predicates are allowed for testing of {@link Assertion} objects such as...
 * <ul>
 * 	<li>{@link FluentObjectAssertion#is(Predicate)}
 * 	<li>{@link FluentArrayAssertion#is(Predicate...)}
 * 	<li>{@link FluentPrimitiveArrayAssertion#is(Predicate...)}
 * 	<li>{@link FluentListAssertion#isEach(Predicate...)}
 * </ul>
 *
 * <p>
 * See {@link AssertionPredicates} for a set of predefined predicates for common use cases.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Asserts that a bean passes a custom test.</jc>
 * 	<jc>// AssertionError with specified message is thrown otherwise.</jc>
 * 	Predicate&lt;MyBean&gt; <jv>predicate</jv> = <jk>new</jk> AssertionPredicate&lt;MyBean&gt;(
 * 		<jv>x</jv> -&gt; <jv>x</jv>.getFoo().equals(<js>"bar"</js>),
 * 		<js>"Foo did not equal bar.  Bean was=''{0}''"</js>,
 * 		<jsf>VALUE</jsf>
 * 	);
 * 	<jsm>assertObject</jsm>(<jv>myBean</jv>).is(<jv>predicate</jv>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <T> the type of input being tested.
 */
public class AssertionPredicate<T> implements Predicate<T> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Argument placeholder for tested value.
	 */
	public static final Function<Object,String> VALUE = x -> stringifyDeep(x);

	private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");
	private static final String
		MSG_valueDidNotPassTest = MESSAGES.getString("valueDidNotPassTest"),
		MSG_valueDidNotPassTestWithValue = MESSAGES.getString("valueDidNotPassTestWithValue");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final Predicate<T> inner;
	private final String message;
	private final Object[] args;
	final ThreadLocal<String> failedMessage = new ThreadLocal<>();

	/**
	 * Constructor.
	 *
	 * @param inner The predicate test.
	 * @param message
	 * 	The error message if predicate fails.
	 * 	<br>Supports {@link MessageFormat}-style arguments.
	 * @param args
	 * 	Optional message arguments.
	 * 	<br>Can contain {@link #VALUE} to specify the value itself as an argument.
	 * 	<br>Can contain {@link Function functions} to apply to the tested value.
	 */
	public AssertionPredicate(Predicate<T> inner, String message, Object...args) {
		this.inner = inner;
		if (message != null) {
			this.message = message;
			this.args = args;
		} else if (inner instanceof AssertionPredicate) {
			this.message = MSG_valueDidNotPassTest;
			this.args = new Object[]{};
		} else {
			this.message = MSG_valueDidNotPassTestWithValue;
			this.args = new Object[]{VALUE};
		}
	}

	AssertionPredicate() {
		this.inner = null;
		this.message = null;
		this.args = null;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Predicate */
	@SuppressWarnings({"unchecked","rawtypes"})
	public boolean test(T t) {
		failedMessage.remove();
		boolean b = inner.test(t);
		if (! b) {
			String m = message;
			Object[] args = new Object[this.args.length];
			for (int i = 0; i < args.length; i++) {
				Object a = this.args[i];
				if (a instanceof Function)
					args[i] = ((Function)a).apply(t);
				else
					args[i] = a;
			}
			m = format(m, args);
			if (inner instanceof AssertionPredicate)
				m += "\n\t" + ((AssertionPredicate<?>)inner).getFailureMessage();
			failedMessage.set(m);
		}
		return inner.test(t);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the error message from the last call to this assertion.
	 *
	 * @return The error message, or <jk>null</jk> if there was no failure.
	 */
	protected String getFailureMessage() {
		return failedMessage.get();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Subclasses
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Encapsulates multiple predicates into a single AND operation.
	 *
	 * <p>
	 * Similar to <c><jsm>stream</jsm>(<jv>predicates</jv>).reduce(<jv>x</jv>-&gt;<jk>true</jk>, Predicate::and)</c> but
	 * provides for {@link #getFailureMessage()} to return a useful message.
	 *
	 * @param <T> the type of input being tested.
	 */
	public static class And<T> extends AssertionPredicate<T> {

		private final Predicate<T>[] inner;

		private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");
		private static final String
			MSG_predicateTestFailed = MESSAGES.getString("predicateTestFailed");

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
						String m = format(MSG_predicateTestFailed, i+1);
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
	 * Similar to <c><jsm>stream</jsm>(<jv>predicates</jv>).reduce(<jv>x</jv>-&gt;<jk>true</jk>, Predicate::or)</c> but
	 * provides for {@link #getFailureMessage()} to return a useful message.
	 *
	 * @param <T> the type of input being tested.
	 */
	public static class Or<T> extends AssertionPredicate<T> {

		private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");
		private static final String
			MSG_noPredicateTestsPassed = MESSAGES.getString("noPredicateTestsPassed");

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
			String m = format(MSG_noPredicateTestsPassed);
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

		private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");
		private static final String
			MSG_predicateTestsUnexpectedlyPassed = MESSAGES.getString("predicateTestsUnexpectedlyPassed");

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
					failedMessage.set(format(MSG_predicateTestsUnexpectedlyPassed));
					return false;
				}
			}
			return true;
		}
	}
}
