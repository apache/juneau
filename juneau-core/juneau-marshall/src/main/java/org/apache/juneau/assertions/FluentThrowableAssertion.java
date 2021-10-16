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

import static org.apache.juneau.assertions.Assertions.*;
import static java.util.Collections.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against throwables.
 * {@review}
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentObjectAssertion#exists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class)}
 * 	</ul>
 *
 * <h5 class='topic'>Transform Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link FluentThrowableAssertion#message()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#messages()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#localizedMessage()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#localizedMessages()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#stackTrace()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#causedBy()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#causedBy(Class)}
 * 		<li class='jm'>{@link FluentThrowableAssertion#find(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#apply(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny()}
 *	</ul>
 *
 * <h5 class='topic'>Configuration Methods</h5>
 * 	<ul>
 * 		<li class='jm'>{@link Assertion#msg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#out(PrintStream)}
 * 		<li class='jm'>{@link Assertion#silent()}
 * 		<li class='jm'>{@link Assertion#stdout()}
 * 		<li class='jm'>{@link Assertion#throwable(Class)}
 * 	</ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc FluentAssertions}
 * </ul>
 *
 * @param <T> The throwable type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentThrowableAssertion<T,R>")
public class FluentThrowableAssertion<T extends Throwable,R> extends FluentObjectAssertion<T,R> {

	private static final Messages MESSAGES = Messages.of(FluentThrowableAssertion.class, "Messages");
	private static final String
		MSG_exceptionWasNotExpectedType = MESSAGES.getString("exceptionWasNotExpectedType"),
		MSG_exceptionWasNotThrown = MESSAGES.getString("exceptionWasNotThrown"),
		MSG_causedByExceptionNotExpectedType = MESSAGES.getString("causedByExceptionNotExpectedType");

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentThrowableAssertion(T value, R returns) {
		this(null, value, returns);
	}

	/**
	 * Chained constructor.
	 *
	 * <p>
	 * Used when transforming one assertion into another so that the assertion config can be used by the new assertion.
	 *
	 * @param creator
	 * 	The assertion that created this assertion.
	 * 	<br>Should be <jk>null</jk> if this is the top-level assertion.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	public FluentThrowableAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* FluentObjectAssertion */
	public FluentThrowableAssertion<T,R> apply(Function<T,T> function) {
		return new FluentThrowableAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Returns an assertion against the throwable message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).message().matches(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> message() {
		return new FluentStringAssertion<>(this, map(Throwable::getMessage).orElse(null), returns());
	}

	/**
	 * Returns an assertion against the throwable message and all caused-by messages.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).messages().containsMatch(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentListAssertion<String,R> messages() {
		List<String> l = null;
		Throwable t = orElse(null);
		if (t != null) {
			if (t.getCause() == null)
				l = singletonList(t.getMessage());
			else {
				l = new ArrayList<>();
				while (t != null) {
					l.add(t.getMessage());
					t = t.getCause();
				}
			}
		}
		return new FluentListAssertion<>(this, l, returns());
	}

	/**
	 * Returns an assertion against the throwable localized message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with 'foobar' somewhere in the localized messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).localizedMessage().matches(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable localized message.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> localizedMessage() {
		return new FluentStringAssertion<>(this, map(Throwable::getLocalizedMessage).orElse(null), returns());
	}

	/**
	 * Returns an assertion against the throwable message and all caused-by messages.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).localizedMessages().contains(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentListAssertion<String,R> localizedMessages() {
		List<String> l = null;
		Throwable t = orElse(null);
		if (t != null) {
			if (t.getCause() == null)
				l = singletonList(t.getMessage());
			else {
				l = new ArrayList<>();
				while (t != null) {
					l.add(t.getLocalizedMessage());
					t = t.getCause();
				}
			}
		}
		return new FluentListAssertion<>(this, l, returns());
	}

	/**
	 * Returns an assertion against the throwable localized message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with 'foobar' somewhere in the stack trace. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).stackTrace().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable stacktrace.  Never <jk>null</jk>.
	 */
	public FluentStringListAssertion<R> stackTrace() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : Arrays.asList(getStackTrace(value())), returns());
	}

	/**
	 * Returns an assertion against the caused-by throwable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception whose caused-by message contains 'foobar'. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).causedBy().message().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return An assertion against the caused-by.  Never <jk>null</jk>.
	 */
	public FluentThrowableAssertion<Throwable,R> causedBy() {
		return causedBy(Throwable.class);
	}

	/**
	 * Returns an assertion against the caused-by throwable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception whose caused-by message contains 'foobar'. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).causedBy().message().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param type The expected exception type.
	 * @return An assertion against the caused-by.  Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <E extends Throwable> FluentThrowableAssertion<E,R> causedBy(Class<E> type) {
		Throwable t = map(Throwable::getCause).orElse(null);
		if (t == null || type.isInstance(t))
			return new FluentThrowableAssertion<>(this, (E)t, returns());
		throw error(MSG_causedByExceptionNotExpectedType, type, t.getClass());
	}

	/**
	 * Returns an assertion against the throwable localized message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws an exception with a caused-by RuntimeException containing 'foobar'</jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).causedBy(RuntimeException.<jk>class</jk>).exists().contains(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param throwableClass The class type to search for in the caused-by chain.
	 * @return An assertion against the caused-by throwable.  Never <jk>null</jk>.
	 */
	@SuppressWarnings("unchecked")
	public <E extends Throwable> FluentThrowableAssertion<E,R> find(Class<E> throwableClass) {
		Throwable t = orElse(null);
		while (t != null) {
			if (throwableClass.isInstance(t))
				return new FluentThrowableAssertion<>(this, (E)t, returns());
			t = t.getCause();
		}
		return new FluentThrowableAssertion<>(this, (E)null, returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that this throwable is of the specified type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();})
	 * 		.isType(RuntimeException.<jk>class</jk>);
	 * </p>
	 *
	 * @param parent The type.
	 * @return This object (for method chaining).
	 */
	@Override
	public R isType(Class<?> parent) {
		assertArgNotNull("parent", parent);
		if (! parent.isInstance(value()))
			throw error(MSG_exceptionWasNotExpectedType, className(parent), className(value()));
		return returns();
	}

	/**
	 * Asserts that this throwable is exactly the specified type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();})
	 * 		.isExactType(RuntimeException.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The type.
	 * @return This object (for method chaining).
	 */
	@Override
	public R isExactType(Class<?> type) {
		assertArgNotNull("type", type);
		if (type != value().getClass())
			throw error(MSG_exceptionWasNotExpectedType, className(type), className(value()));
		return returns();
	}

	/**
	 * Asserts that this throwable exists.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Asserts that the specified method throws any exception.</jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; {<jv>foo</jv>.getBar();}).exists();
	 * </p>
	 *
	 * @return This object (for method chaining).
	 */
	@Override
	public R exists() {
		if (valueIsNull())
			throw error(MSG_exceptionWasNotThrown);
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override
	protected boolean equals(Object o1, Object o2) {
		if (o1 instanceof Throwable && o2 instanceof Throwable)
			return ObjectUtils.eq((Throwable)o1, (Throwable)o2, (x,y)->ObjectUtils.eq(x.getClass(),y.getClass()) && ObjectUtils.eq(x.getMessage(),y.getMessage()));
		return super.equals(o1, o2);
	}
}
