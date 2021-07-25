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

import java.io.*;

import org.apache.juneau.internal.*;

/**
 * Parent class of all fluent assertion calls.
 *
 * <p>
 * Defines a {@link #returns()} method that returns an original object.
 * Assertion test methods that pass use this method to return to the origin of the call.
 *
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jc>// Create a basic REST client with JSON support and download a bean.</jc>
 * MyPojo myPojo = ...;
 * MyTestedBean myTestedBean = ...;
 *
 * Assertion assertion = new FluentBeanAssertion<MyPojo,MyTestedBean>(myPojo, myTestedBean);
 * myPojo = assertion.test(x -> x.getMyProperty().equals("foo"));  <jc>// Returns myPojo after test.</jc>
 * </p>
 *
 * For subclasses such as {@link IntegerAssertion}, the return object is simply itself so that multiple tests
 * can be performed using the same assertion.
 * <h5 class='figure'>Example:</h5>
 * <p class='bcode w800'>
 * 	<jk>public class</jk> IntegerAssertion <jk>extends</jk> FluentIntegerAssertion&lt;IntegerAssertion&gt; {
 *  	...
 *  }
 *
 * 	Assertion <jv>assertion</jv> = <jk>new</jk> IntegerAssertion(123);
 * 	<jv>assertion</jv>
 * 		.isNotNull()
 * 		.isGt(100)
 *  ;
 * </p>
 *
 *
 * <h5 class='topic'>Test Methods</h5>
 * 	<ul>
 * 		<li>None
 * 	</ul>
 *
 * <h5 class='topic'>Transform Methods</h5>
 * 	<ul>
 * 		<li>None
 * 	</ul>
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
 * 	<li class='link'>{@doc Assertions}
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentAssertion<R>")
public abstract class FluentAssertion<R> extends Assertion {

	private final R returns;

	//-----------------------------------------------------------------------------------------------------------------
	// Constructors
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param creator
	 * 	The assertion that created this assertion.
	 * 	<br>Should be <jk>null</jk> if this is the top-level assertion.
	 * @param returns
	 * 	The object to return after a test method is called.
	 * 	<br>If <jk>null</jk>, the test method returns this object allowing multiple test method calls to be
	 * used on the same assertion.
	 */
	protected FluentAssertion(Assertion creator, R returns) {
		super(creator);
		this.returns = returns;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - Assertion */
	public FluentAssertion<R> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the object that the fluent methods on this class should return.
	 *
	 * @return The response object.
	 */
	@SuppressWarnings("unchecked")
	protected R returns() {
		return returns != null ? returns : (R)this;
	}

}
