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
import java.util.function.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;

/**
 * Used for assertion calls against generic POJOs.
 * {@review}
 *
 * <p>
 * Extends from {@link ObjectAssertion} allowing you to perform basic assertions, but adds several transform
 * methods to convert to more-specific assertion types.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>import static</jk> org.apache.juneau.assertions.Assertions.*;
 *
 * 	List&lt;MyBean&gt; <jv>listOfBeans</jv> = ...;
 * 	<jsm>assertList</jsm>(<jv>listOfBeans</jv>)
 * 		.item(1)  <jc>// Returns an AnyAssertion.</jc>
 * 		.asBean()  <jc>// Transforms to BeanAssertion.</jc>
 * 			.property(<js>"foo"</js>)  <jc>// Returns an AnyAssertion.</jc>
 * 			.asString()  <jc>// Transforms to StringAssertion.</jc>
 * 				.is(<js>"bar"</js>);  <jc>// Performs test.</jc>
 * </p>
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
 * 		<li class='jm'>{@link FluentAnyAssertion#asArray(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asIntArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLongArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asShortArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asFloatArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDoubleArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCharArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asByteArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBooleanArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBoolean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBytes()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asStringList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asComparable()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDate()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asInteger()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLong()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap(Class,Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBeanList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asZonedDateTime()}
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
 * 	<li class='link'>{@doc jm.FluentAssertions}
 * 	<li class='extlink'>{@source}
 * </ul>
 *
 * @param <T> The object type.
 */
@FluentSetters(returns="AnyAssertion<T>")
public class AnyAssertion<T> extends FluentAnyAssertion<T,AnyAssertion<T>> {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Static creator.
	 *
	 * @param <T> The value type.
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 * @return A new assertion object.
	 */
	public static <T> AnyAssertion<T> create(T value) {
		return new AnyAssertion<>(value);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param value
	 * 	The object being tested.
	 * 	<br>Can be <jk>null</jk>.
	 */
	public AnyAssertion(T value) {
		super(value, null);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//-----------------------------------------------------------------------------------------------------------------

	// <FluentSetters>

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> msg(String msg, Object...args) {
		super.msg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> out(PrintStream value) {
		super.out(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> silent() {
		super.silent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> stdout() {
		super.stdout();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> throwable(Class<? extends java.lang.RuntimeException> value) {
		super.throwable(value);
		return this;
	}

	// </FluentSetters>
}
