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

import static java.util.Collections.*;
import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.list;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against throwables.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#isExists() isExists()}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Object) is(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#is(Predicate) is(Predicate)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNot(Object) isNot(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isAny(Object...) isAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...) isNotAny(Object...)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNull() isNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isNotNull() isNotNull()}
 * 		<li class='jm'>{@link FluentObjectAssertion#isString(String) isString(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isJson(String) isJson(String)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSame(Object) isSame(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object) isSameJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object) isSameSortedJsonAs(Object)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer) isSameSerializedAs(Object, WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isType(Class) isType(Class)}
 * 		<li class='jm'>{@link FluentObjectAssertion#isExactType(Class) isExactType(Class)}
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentThrowableAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentThrowableAssertion#asMessage() asMessage()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asMessages() asMessages()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asLocalizedMessage() asLocalizedMessage()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asLocalizedMessages() asLocalizedMessages()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asStackTrace() asStackTrace()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asCausedBy() asCausedBy()}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asCausedBy(Class) asCausedBy(Class)}
 * 		<li class='jm'>{@link FluentThrowableAssertion#asFind(Class) asFind(Class)}
 * 	</ul>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentObjectAssertion#asString() asString()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(WriterSerializer) asString(WriterSerializer)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asString(Function) asString(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJson() asJson()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asJsonSorted() asJsonSorted()}
 * 		<li class='jm'>{@link FluentObjectAssertion#asTransformed(Function) asApplied(Function)}
 * 		<li class='jm'>{@link FluentObjectAssertion#asAny() asAny()}
 *	</ul>
 * </ul>
 *
 * <h5 class='section'>Configuration Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link Assertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link Assertion#setMsg(String, Object...) setMsg(String, Object...)}
 * 		<li class='jm'>{@link Assertion#setOut(PrintStream) setOut(PrintStream)}
 * 		<li class='jm'>{@link Assertion#setSilent() setSilent()}
 * 		<li class='jm'>{@link Assertion#setStdOut() setStdOut()}
 * 		<li class='jm'>{@link Assertion#setThrowable(Class) setThrowable(Class)}
* 	</ul>
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <T> The throwable type.
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentThrowableAssertion<T,R>")
public class FluentThrowableAssertion<T extends Throwable,R> extends FluentObjectAssertion<T,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	private static final Messages MESSAGES = Messages.of(FluentThrowableAssertion.class, "Messages");
	private static final String
		MSG_exceptionWasNotExpectedType = MESSAGES.getString("exceptionWasNotExpectedType"),
		MSG_exceptionWasNotThrown = MESSAGES.getString("exceptionWasNotThrown"),
		MSG_causedByExceptionNotExpectedType = MESSAGES.getString("causedByExceptionNotExpectedType");

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
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
	public FluentThrowableAssertion<T,R> asTransformed(Function<T,T> function) {
		return new FluentThrowableAssertion<>(this, function.apply(orElse(null)), returns());
	}

	/**
	 * Returns an assertion against the throwable message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception
	 *	// with 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asMessage()
	 * 		.isPattern(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> asMessage() {
		return new FluentStringAssertion<>(this, map(Throwable::getMessage).orElse(null), returns());
	}

	/**
	 * Returns an assertion against the throwable message and all caused-by messages.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception with
	 * 	// 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asMessages()
	 * 		.isPattern(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentListAssertion<String,R> asMessages() {
		List<String> l = null;
		Throwable t = orElse(null);
		if (t != null) {
			if (t.getCause() == null)
				l = singletonList(t.getMessage());
			else {
				l = list();
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
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception with
	 * 	// 'foobar' somewhere in the localized messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asLocalizedMessage()
	 * 		.isPattern(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable localized message.  Never <jk>null</jk>.
	 */
	public FluentStringAssertion<R> asLocalizedMessage() {
		return new FluentStringAssertion<>(this, map(Throwable::getLocalizedMessage).orElse(null), returns());
	}

	/**
	 * Returns an assertion against the throwable message and all caused-by messages.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception with
	 * 	// 'foobar' somewhere in the messages. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asLocalizedMessages()
	 * 		.isPattern(<js>".*foobar.*"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable message.  Never <jk>null</jk>.
	 */
	public FluentListAssertion<String,R> asLocalizedMessages() {
		List<String> l = null;
		Throwable t = orElse(null);
		if (t != null) {
			if (t.getCause() == null)
				l = singletonList(t.getMessage());
			else {
				l = list();
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
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception with
	 * 	// 'foobar' somewhere in the stack trace. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asStackTrace()
	 * 		.isPattern(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return An assertion against the throwable stacktrace.  Never <jk>null</jk>.
	 */
	public FluentStringListAssertion<R> asStackTrace() {
		return new FluentStringListAssertion<>(this, valueIsNull() ? null : Arrays.asList(getStackTrace(value())), returns());
	}

	/**
	 * Returns an assertion against the caused-by throwable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception whose
	 * 	// caused-by message contains 'foobar'. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asCausedBy()
	 * 		.asMessage()
	 * 		.isPattern(<js>"foobar"</js>);
	 * </p>
	 *
	 * @return An assertion against the caused-by.  Never <jk>null</jk>.
	 */
	public FluentThrowableAssertion<Throwable,R> asCausedBy() {
		return asCausedBy(Throwable.class);
	}

	/**
	 * Returns an assertion against the caused-by throwable.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception whose
	 * 	// caused-by message contains 'foobar'. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.asCausedBy(RuntimeException.<jk>class</jk>)
	 * 		.asMessage()
	 * 		.isPattern(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param <X> The throwable type.
	 * @param type The expected exception type.
	 * @return An assertion against the caused-by.  Never <jk>null</jk>.
	 */
	public <X extends Throwable> FluentThrowableAssertion<X,R> asCausedBy(Class<X> type) {
		Throwable t = map(Throwable::getCause).orElse(null);
		if (t == null || type.isInstance(t))
			return new FluentThrowableAssertion<>(this, type.cast(t), returns());
		throw error(MSG_causedByExceptionNotExpectedType, type, t.getClass());
	}

	/**
	 * Returns an assertion against the throwable localized message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws an exception with a
	 * 	// caused-by RuntimeException containing 'foobar'</jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.findCausedBy(RuntimeException.<jk>class</jk>)
	 * 		.isExists()
	 * 		.asMessage()
	 * 		.isPattern(<js>"foobar"</js>);
	 * </p>
	 *
	 * @param <X> The throwable type.
	 * @param throwableClass The class type to search for in the caused-by chain.
	 * @return An assertion against the caused-by throwable.  Never <jk>null</jk>.
	 */
	public <X extends Throwable> FluentThrowableAssertion<X,R> asFind(Class<X> throwableClass) {
		Throwable t = orElse(null);
		while (t != null) {
			if (throwableClass.isInstance(t))
				return new FluentThrowableAssertion<>(this, throwableClass.cast(t), returns());
			t = t.getCause();
		}
		return new FluentThrowableAssertion<>(this, (X)null, returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Asserts that this throwable is of the specified type.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.isType(RuntimeException.<jk>class</jk>);
	 * </p>
	 *
	 * @param parent The type.
	 * @return The fluent return object.
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
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws a RuntimeException. </jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar())
	 * 		.isExactType(RuntimeException.<jk>class</jk>);
	 * </p>
	 *
	 * @param type The type.
	 * @return The fluent return object.
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
	 * <p class='bjava'>
	 * 	<jc>// Asserts that the specified method throws any exception.</jc>
	 * 	ThrowableAssertion.<jsm>assertThrown</jsm>(() -&gt; <jv>foo</jv>.getBar()).isExists();
	 * </p>
	 *
	 * @return The fluent return object.
	 */
	@Override
	public R isExists() {
		if (valueIsNull())
			throw error(MSG_exceptionWasNotThrown);
		return returns();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentThrowableAssertion<T,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
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
