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
 * <p class='bjava'>
 * 	<jc>// Create a basic REST client with JSON support and download a bean.</jc>
 * 	MyPojo <jv>myPojo</jv> = ...;
 * 	MyTestedBean <jv>myTestedBean</jv> = ...;
 *
 * 	Assertion <jv>assertion</jv> = <jk>new</jk> FluentBeanAssertion&lt;MyPojo,MyTestedBean&gt;(<jv>myPojo</jv>, <jv>myTestedBean</jv>);
 * 	<jv>myPojo</jv> = <jv>assertion</jv>.test(<jv>x</jv> -&gt; <jv>x</jv>.getMyProperty().equals(<js>"foo"</js>));  <jc>// Returns myPojo after test.</jc>
 * </p>
 * <p>
 * For subclasses such as {@link IntegerAssertion}, the return object is simply itself so that multiple tests
 * can be performed using the same assertion.
 * <h5 class='figure'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>public class</jk> IntegerAssertion <jk>extends</jk> FluentIntegerAssertion&lt;IntegerAssertion&gt; {
 *  	...
 * 	}
 *
 * 	Assertion <jv>assertion</jv> = <jk>new</jk> IntegerAssertion(123);
 * 	<jv>assertion</jv>
 * 		.isNotNull()
 * 		.isGt(100)
 *  ;
 * </p>
 *
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
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
 * </ul>
 *
 * @param <R> The return type.
 */
@FluentSetters(returns="FluentAssertion<R>")
public abstract class FluentAssertion<R> extends Assertion {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	private final R returns;

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

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAssertion<R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAssertion<R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAssertion<R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAssertion<R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public FluentAssertion<R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
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
