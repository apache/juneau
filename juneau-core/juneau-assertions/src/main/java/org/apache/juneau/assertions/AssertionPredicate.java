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
package org.apache.juneau.assertions;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.text.*;
import java.util.function.*;

import org.apache.juneau.common.utils.*;
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauEcosystemOverview">Juneau Ecosystem Overview</a>
 * </ul>
 *
 * @param <T> the type of input being tested.
 */
public class AssertionPredicate<T> implements Predicate<T> {

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

		private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");

		private static final String MSG_predicateTestFailed = MESSAGES.getString("predicateTestFailed");
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

		@Override /* Overridden from Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			for (var i = 0; i < inner.length; i++) {
				var p = inner[i];
				if (nn(p)) {
					var b = p.test(t);
					if (! b) {
						var m = mformat(MSG_predicateTestFailed, i + 1);
						if (p instanceof AssertionPredicate p2) // NOSONAR - Intentional.
							m += "\n\t" + p2.getFailureMessage();
						failedMessage.set(m);
						return false;
					}
				}
			}
			return true;
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
		private static final String MSG_predicateTestsUnexpectedlyPassed = MESSAGES.getString("predicateTestsUnexpectedlyPassed");

		private final Predicate<T> inner;

		/**
		 * Constructor.
		 *
		 * @param inner The inner predicates to run.
		 */
		public Not(Predicate<T> inner) {
			this.inner = inner;
		}

		@Override /* Overridden from Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			var p = inner;
			if (nn(p)) {
				var b = p.test(t);
				if (b) {
					failedMessage.set(mformat(MSG_predicateTestsUnexpectedlyPassed));
					return false;
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
		private static final String MSG_noPredicateTestsPassed = MESSAGES.getString("noPredicateTestsPassed");

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

		@Override /* Overridden from Predicate */
		public boolean test(T t) {
			failedMessage.remove();
			for (var p : inner)
				if (nn(p) && p.test(t))
					return true;
			var m = mformat(MSG_noPredicateTestsPassed);
			failedMessage.set(m);
			return false;
		}
	}

	/**
	 * Argument placeholder for tested value.
	 */
	public static final Function<Object,String> VALUE = StringUtils::stringifyDeep;
	private static final Messages MESSAGES = Messages.of(AssertionPredicate.class, "Messages");
	// @formatter:off
	private static final String
		MSG_valueDidNotPassTest = MESSAGES.getString("valueDidNotPassTest"),
		MSG_valueDidNotPassTestWithValue = MESSAGES.getString("valueDidNotPassTestWithValue");
	// @formatter:on
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
		if (nn(message)) {
			this.message = message;
			this.args = args;
		} else if (inner instanceof AssertionPredicate) {
			this.message = MSG_valueDidNotPassTest;
			this.args = a();
		} else {
			this.message = MSG_valueDidNotPassTestWithValue;
			this.args = a(VALUE);
		}
	}

	AssertionPredicate() {
		this.inner = null;
		this.message = null;
		this.args = null;
	}

	@Override /* Overridden from Predicate */
	@SuppressWarnings("unchecked")
	public boolean test(T t) {
		failedMessage.remove();
		var b = inner.test(t);
		if (! b) {
			var m = message;
			var oargs = new Object[this.args.length];
			for (var i = 0; i < oargs.length; i++) {
				var a = this.args[i];
			if (a instanceof Function a2) // NOSONAR - Intentional.
				oargs[i] = a2.apply(t);
				else
					oargs[i] = a;
			}
			m = mformat(m, oargs);
			if (inner instanceof AssertionPredicate inner2) // NOSONAR - Intentional.
				m += "\n\t" + inner2.getFailureMessage();
			failedMessage.set(m);
		}
		return inner.test(t);
	}

	/**
	 * Returns the error message from the last call to this assertion.
	 *
	 * @return The error message, or <jk>null</jk> if there was no failure.
	 */
	protected String getFailureMessage() { return failedMessage.get(); }
}