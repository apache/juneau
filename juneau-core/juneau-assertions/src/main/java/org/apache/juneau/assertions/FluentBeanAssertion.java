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

import static java.util.Arrays.*;

import java.io.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.serializer.*;

/**
 * Used for fluent assertion calls against Java beans.
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
 * 	<li class='jc'>{@link FluentBeanAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentBeanAssertion#asPropertyMap(String...) asPropertyMap(String...)}
 * 		<li class='jm'>{@link FluentBeanAssertion#asProperty(String) asProperty(String)}
 * 		<li class='jm'>{@link FluentBeanAssertion#asProperties(String...) asProperties(String...)}
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
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauEcosystemOverview">Juneau Ecosystem Overview</a>
 * </ul>
 *
 * @param <T> The bean type.
 * @param <R> The return type.
 */
public class FluentBeanAssertion<T,R> extends FluentObjectAssertion<T,R> {

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

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
	public FluentBeanAssertion(Assertion creator, T value, R returns) {
		super(creator, value, returns);
	}

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
	public FluentBeanAssertion(T value, R returns) {
		this(null, value, returns);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Transform methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Extracts the specified property as an {@link FluentListAssertion}.
	 *
	 * @param names The names of the properties to extract.  Can also pass in comma-delimited lists.
	 * @return An assertion of the property values.
	 */
	public FluentListAssertion<Object,R> asProperties(String...names) {
		BeanMap<T> bm = toBeanMap();
		return new FluentListAssertion<>(this, stream(names).map(bm::get).toList(), returns());
	}

	/**
	 * Extracts the specified property as an {@link FluentAnyAssertion}.
	 *
	 * @param name The property to extract.  Can also pass in comma-delimited lists.
	 * @return An assertion of the property value.
	 */
	public FluentAnyAssertion<Object,R> asProperty(String name) {
		return new FluentAnyAssertion<>(this, toBeanMap().get(name), returns());
	}

	/**
	 * Extracts the specified fields of this bean into a simple map of key/value pairs and returns it as
	 * a new {@link MapAssertion}.
	 *
	 * @param names The fields to extract.  Can also pass in comma-delimited lists.
	 * @return This object.
	 */
	public FluentMapAssertion<String,Object,R> asPropertyMap(String...names) {
		return new FluentMapAssertion<>(this, toBeanMap().getProperties(Utils.splita(names, ',')), returns());
	}

	@Override /* Overridden from FluentObjectAssertion */
	public FluentBeanAssertion<T,R> asTransformed(Function<T,T> function) {  // NOSONAR - Intentional.
		return new FluentBeanAssertion<>(this, function.apply(orElse(null)), returns());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------
	@Override /* Overridden from Assertion */
	public FluentBeanAssertion<T,R> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* Overridden from Assertion */
	public FluentBeanAssertion<T,R> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* Overridden from Assertion */
	public FluentBeanAssertion<T,R> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* Overridden from Assertion */
	public FluentBeanAssertion<T,R> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* Overridden from Assertion */
	public FluentBeanAssertion<T,R> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}
	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	private BeanMap<T> toBeanMap() {
		return BeanMap.of(value());
	}
}