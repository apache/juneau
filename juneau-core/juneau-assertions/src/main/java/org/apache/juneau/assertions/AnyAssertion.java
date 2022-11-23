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
 * 		.asItem(1)  <jc>// Returns an AnyAssertion.</jc>
 * 		.asBean()  <jc>// Transforms to BeanAssertion.</jc>
 * 			.asProperty(<js>"foo"</js>)  <jc>// Returns an AnyAssertion.</jc>
 * 			.asString()  <jc>// Transforms to StringAssertion.</jc>
 * 				.is(<js>"bar"</js>);  <jc>// Performs test.</jc>
 * </p>
 *
 *
 * <h5 class='section'>Test Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentObjectAssertion}
 * 		<ul class='javatreec'>
 * 			<li class='jm'>{@link FluentObjectAssertion#isExists() isExists()}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#is(Object) is(Object)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#is(Predicate) is(Predicate)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isNot(Object) isNot(Object)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isAny(Object...) isAny(Object...)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isNotAny(Object...) isNotAny(Object...)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isNull() isNull()}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isNotNull() isNotNull()}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isString(String) isString(String)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isJson(String) isJson(String)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isSame(Object) isSame(Object)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isSameJsonAs(Object) isSameJsonAs(Object)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isSameSortedJsonAs(Object) isSameSortedJsonAs(Object)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isSameSerializedAs(Object, WriterSerializer) isSameSerializedAs(Object, WriterSerializer)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isType(Class) isType(Class)}</li>
 * 			<li class='jm'>{@link FluentObjectAssertion#isExactType(Class) isExactType(Class)}</li>
 * 		</ul>
 * 	</li>
 * </ul>
 *
 * <h5 class='section'>Transform Methods:</h5>
 * <p>
 * <ul class='javatree'>
 * 	<li class='jc'>{@link FluentAnyAssertion}
 * 	<ul class='javatreec'>
 * 		<li class='jm'>{@link FluentAnyAssertion#asArray(Class) asArray(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asIntArray() asIntArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLongArray() asLongArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asShortArray() asShortArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asFloatArray() asFloatArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDoubleArray() asDoubleArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCharArray() asCharArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asByteArray() asByteArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBooleanArray() asBooleanArray()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBoolean() asBoolean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBytes() asBytes()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection() asCollection()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asCollection(Class) asCollection(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asStringList() asStringList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asComparable() asComparable()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asDate() asDate()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asInteger() asInteger()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asLong() asLong()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList() asList()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asList(Class) asList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap() asMap()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asMap(Class,Class) asMap(Class,Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean() asBean()}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBean(Class) asBean(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asBeanList(Class) asBeanList(Class)}
 * 		<li class='jm'>{@link FluentAnyAssertion#asZonedDateTime() asZonedDateTime()}
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#ja.Overview">Overview &gt; juneau-assertions &gt; Overview</a>
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
	public AnyAssertion<T> setMsg(String msg, Object...args) {
		super.setMsg(msg, args);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> setOut(PrintStream value) {
		super.setOut(value);
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> setSilent() {
		super.setSilent();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> setStdOut() {
		super.setStdOut();
		return this;
	}

	@Override /* GENERATED - org.apache.juneau.assertions.Assertion */
	public AnyAssertion<T> setThrowable(Class<? extends java.lang.RuntimeException> value) {
		super.setThrowable(value);
		return this;
	}

	// </FluentSetters>
}
