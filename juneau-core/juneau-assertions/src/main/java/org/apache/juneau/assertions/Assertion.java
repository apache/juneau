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

import static org.apache.juneau.common.utils.StringUtils.*;
import static org.apache.juneau.common.utils.ThrowableUtils.*;
import static org.apache.juneau.common.utils.Utils.*;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.cp.*;

/**
 * Base class for all assertion objects.
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li>None
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li>None
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
 * <ul class='seealso'>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauEcosystemOverview">Juneau Ecosystem Overview</a>
 * </ul>
 */
public class Assertion {

	private static final Messages MESSAGES = Messages.of(Assertion.class, "Messages");

	// @formatter:off
	static final String
		MSG_parameterCannotBeNull = MESSAGES.getString("parameterCannotBeNull"),
		MSG_causedBy = MESSAGES.getString("causedBy");
	// @formatter:on

	/**
	 * Convenience method for getting the array class of the specified element type.
	 *
	 * @param <E> The element type.
	 * @param c The object to get the class name for.
	 * @return The class name for an object.
	 */
	@SuppressWarnings("unchecked")
	protected static <E> Class<E[]> arrayClass(Class<E> c) {
		return (Class<E[]>)Array.newInstance(c, 0).getClass();
	}

	private String msg;
	private Object[] msgArgs;

	private PrintStream out = System.err; // NOSONAR - Intentional.

	private Class<? extends RuntimeException> throwable;

	/**
	 * Constructor used when this assertion is being created from within another assertion.
	 *
	 * @param creator The creator of this assertion.
	 */
	protected Assertion(Assertion creator) {
		if (nn(creator)) {
			this.msg = creator.msg;
			this.msgArgs = creator.msgArgs;
			this.out = creator.out;
			this.throwable = creator.throwable;
		}
	}

	/**
	 * Allows you to override the assertion failure message.
	 *
	 * <p>
	 * String can contain <js>"{msg}"</js> to represent the original message.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Throws an assertion with a custom message instead of the default "Value was null."</jc>
	 * 	<jsm>assertString</jsm>(<jv>myString</jv>)
	 * 		.setMsg(<js>"My string was bad:  {msg}"</js>)
	 * 		.isNotNull();
	 * </p>
	 *
	 * @param msg The assertion failure message.
	 * @param args Optional message arguments.
	 * @return This object.
	 */
	public Assertion setMsg(String msg, Object...args) {
		this.msg = msg.replace("{msg}", "<<<MSG>>>");
		this.msgArgs = args;
		return this;
	}

	/**
	 * If an error occurs, send the error message to the specified stream instead of STDERR.
	 *
	 * @param value
	 * 	The output stream.
	 * 	Can be <jk>null</jk> to suppress output.
	 * @return This object.
	 */
	public Assertion setOut(PrintStream value) {
		this.out = value;
		return this;
	}

	/**
	 * Suppresses output to STDERR.
	 *
	 * <p>
	 * This is the equivalent to calling <c>out(<jk>null</jk>)</c>.
	 *
	 * @return This object.
	 */
	public Assertion setSilent() {
		return setOut(null);
	}

	/**
	 * If an error occurs, send the error message to STDOUT instead of STDERR.
	 *
	 * @return This object.
	 */
	public Assertion setStdOut() {
		return setOut(System.out); // NOSONAR - Intentional.
	}

	/**
	 * If an error occurs, throw this exception instead of the standard {@link AssertionError}.
	 *
	 * <p>
	 * The throwable class must have a public constructor that takes in any of the following parameters:
	 * <ul>
	 * 	<li>{@link Throwable} - The caused-by exception (if there is one).
	 * 	<li>{@link String} - The assertion failure message.
	 * </ul>
	 *
	 * <p>
	 * If the throwable cannot be instantiated, a {@link RuntimeException} is thrown instead.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
	 *
	 *	<jc>// Throws a BadRequest instead of an AssertionError if the string is null.</jc>
	 * 	<jsm>assertString</jsm>(<jv>myString</jv>)
	 * 		.setThrowable(BadRequest.<jk>class</jk>)
	 * 		.isNotNull();
	 * </p>
	 *
	 * @param value The new value for this setting.
	 * @return This object.
	 */
	public Assertion setThrowable(Class<? extends RuntimeException> value) {
		this.throwable = value;
		return this;
	}

	/**
	 * Creates a new {@link BasicAssertionError}.
	 *
	 * @param msg The message.
	 * @param args The message arguments.
	 * @return A new {@link BasicAssertionError}.
	 */
	protected BasicAssertionError error(String msg, Object...args) {
		return error(null, msg, args);
	}

	/**
	 * Creates a new {@link BasicAssertionError}.
	 *
	 * @param cause Optional caused-by throwable.
	 * @param msg The message.
	 * @param args The message arguments.
	 * @return A new {@link BasicAssertionError}.
	 */
	protected BasicAssertionError error(Throwable cause, String msg, Object...args) {
		msg = format(msg, args);
		if (nn(this.msg))
			msg = format(this.msg, this.msgArgs).replace("<<<MSG>>>", msg);
		if (nn(out))
			out.println(msg);
		if (nn(throwable)) {
			try {
				// @formatter:off
				throw BeanStore
					.create()
					.build()
					.addBean(Throwable.class, cause)
					.addBean(String.class, msg)
					.addBean(Object[].class,new Object[0])
					.createBean(throwable)
					.run();
				// @formatter:on
			} catch (@SuppressWarnings("unused") ExecutableException e) {
				// If we couldn't create requested exception, just throw a RuntimeException.
				throw rex(cause, msg);
			}
		}
		return new BasicAssertionError(cause, msg);
	}
}